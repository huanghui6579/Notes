package net.ibaixin.notes.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.listener.OnItemClickListener;
import net.ibaixin.notes.model.Archive;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.widget.DividerItemDecoration;
import net.ibaixin.notes.widget.LayoutManagerFactory;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 主界面
 * @author huanghui1
 * @update 2016/2/24 19:25
 * @version: 1.0.0
 */
public class MainActivity extends BaseActivity {

    private NavViewAdapter mNavAdapter;
    
    private SwipeRefreshLayout mRefresher;
    
    private RecyclerView mRecyclerView;
    
    private NoteListAdapter mNoteListAdapter;
    private NoteGridAdapter mNoteGridAdapter;

    List<Archive> mArchives;
    
    private List<NoteInfo> mNotes;
    
    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;

    /**
     * 主界面右上角菜单
     */
    private PopupMenu mMainPopuMenu;

    private SharedPreferences mSharedPreferences;

    /**
     * 显示的是否是网格风格
     */
    private boolean mIsGridStyle = true;

    private LayoutManagerFactory mLayoutManagerFactory;

    private RecyclerView.ItemDecoration mItemDecoration;
    
    /*
    主界面更多菜单
     */
    private MenuItem mActionOverflow;
    
    private final Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mTarget;

        public MyHandler(MainActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity target = mTarget.get();
            if (target != null) {
                switch (msg.what) {
                    case Constants.MSG_SUCCESS:
                        target.mNavAdapter.notifyDataSetChanged();
                        break;
                    case Constants.MSG_SUCCESS2:    //笔记内容加载成功
                        target.mRefresher.setRefreshing(false);
                        target.setShowContentStyle(target.mIsGridStyle, false);
                        
                        break;
                }
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        //初始化主界面右下角编辑按钮
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, NoteEditActivity.class);
                startActivity(intent);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

        //初始化顶部栏
        if (mToolBar != null) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, mToolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
        }

        //初始化下拉刷新界面
        mRefresher = (SwipeRefreshLayout) findViewById(R.id.refresher);

        mNotes = new ArrayList<>();
        mLayoutManagerFactory = new LayoutManagerFactory();
        mRecyclerView = (RecyclerView) findViewById(R.id.lv_data);
        mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(this, mIsGridStyle));
        if (!mIsGridStyle) {    //开始显示列表样式
            mItemDecoration = getItemDecoration(this);
            mRecyclerView.addItemDecoration(mItemDecoration);

            mNoteListAdapter = new NoteListAdapter(mContext, mNotes);
            mRecyclerView.setAdapter(mNoteListAdapter);
        } else {
            mNoteGridAdapter = new NoteGridAdapter(mContext, mNotes);
            mRecyclerView.setAdapter(mNoteGridAdapter);
        }

        //初始化左侧导航菜单
        RecyclerView navigationView = (RecyclerView) findViewById(R.id.nav_view);

        mArchives = new ArrayList<>();
        Archive archive = new Archive();
        archive.setName(getString(R.string.default_archive));
        mArchives.add(archive);

        navigationView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
        mNavAdapter = new NavViewAdapter(this, mArchives);
        mNavAdapter.setItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                view.setSelected(true);
            }
        });
        navigationView.setAdapter(mNavAdapter);

        mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SystemUtil.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (!mNotes.isEmpty()) {
                            mNotes.clear();
                        }
                        for (int i = 0; i < 26; i++) {
                            NoteInfo note = new NoteInfo();
                            note.setContent("测试文本内容" + i);
                            mNotes.add(note);
                        }

                        mHandler.sendEmptyMessage(Constants.MSG_SUCCESS2);
                    }
                });
            }
        };

        mRefresher.setColorSchemeResources(R.color.colorPrimary);
        mRefresher.setOnRefreshListener(mOnRefreshListener);

    }

    @Override
    protected void initData() {
        
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    Archive archive = new Archive();
                    archive.setName("测试分类" + i);
                    mArchives.add(archive);
                    
                    mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
                }
            }
        });

        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });
    }

    @Override
    protected boolean showHomeUp() {
        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_more);
        setMenuOverFlowTint(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_more:  //更多

                View view = getToolBarMenuView(R.id.action_more);

                createPopuMenu(view);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU: //点击了菜单
                if (mMainPopuMenu != null) {
                    togglePopuMenu(mMainPopuMenu);
                } else {
                    View view = getToolBarMenuView(R.id.action_more);
                    createPopuMenu(view);
                }
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * 创建或者获取列表的分割线
     * @author tiger
     * @update 2016/2/28 14:12
     * @version 1.0.0
     */
    private RecyclerView.ItemDecoration getItemDecoration(Context context) {
        if (mItemDecoration == null) {
            mItemDecoration = new DividerItemDecoration(context, R.drawable.divider_horizontal, DividerItemDecoration.VERTICAL_LIST);
        }
        return mItemDecoration;
    }

    /**
     * popuMenu菜单的显示与消失之间的切换
     * @author tiger
     * @update 2016/2/27 17:43
     * @version 1.0.0
     */
    private void togglePopuMenu(PopupMenu popupMenu) {
        if (popupMenu != null) {
            if (isPopuMenuShowing(popupMenu)) { //已经显示了，则隐藏
                popupMenu.dismiss();
            } else {
                popupMenu.show();
            }
        }
    }

    /**
     * 创建popuMenu
     * @param auchor 菜单要在哪个view上弹出
     * @author tiger
     * @update 2016/2/27 17:46
     * @version 1.0.0
     */
    private PopupMenu createPopuMenu(View auchor) {
        if (auchor != null) {
            if (mMainPopuMenu == null) {
                mMainPopuMenu = createPopuMenu(auchor, R.menu.main_overflow, true, new OnPopuMenuItemClickListener());
                if (!mIsGridStyle) {    //开始就显示列表，则菜单为网格
                    Menu menu = mMainPopuMenu.getMenu();
                    MenuItem menuItem = menu.getItem(R.id.nav_show_style);
                    if (menuItem != null) {
                        menuItem.setTitle(R.string.action_show_grid);
                        menuItem.setIcon(R.drawable.ic_action_grid);
                    }
                }
            }
            mMainPopuMenu.show();
            return mMainPopuMenu;
        } else {
            return null;
        }
    }
    
    /**
     * 根据不同的显示方式来显示不同的样式
     * @param isGridStyle 是否是网格显示样式
     * @param resetAdapter 是否重新设置adapter，如果不重新设置，则只是adapter的更新                   
     * @author huanghui1
     * @update 2016/3/1 11:45
     * @version: 1.0.0
     */
    private void setShowContentStyle(boolean isGridStyle, boolean resetAdapter) {
        if (isGridStyle) { //显示成网格样式
            if (mItemDecoration != null) {
                mRecyclerView.removeItemDecoration(mItemDecoration);
            }
            if (mNoteGridAdapter == null) {
                mNoteGridAdapter = new NoteGridAdapter(mContext, mNotes);
                resetAdapter = true;
            }
            if (resetAdapter) {
                mRecyclerView.setAdapter(mNoteGridAdapter);
            } else {
                mNoteGridAdapter.notifyDataSetChanged();
            }
        } else {    //列表样式
            mRecyclerView.addItemDecoration(getItemDecoration(mContext));
            if (mNoteListAdapter == null) {
                mNoteListAdapter = new NoteListAdapter(mContext, mNotes);
                resetAdapter = true;
            }
            if (resetAdapter) {
                mRecyclerView.setAdapter(mNoteListAdapter);
            } else {
                mNoteListAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * 判断popuMenu是否显示
     * @author tiger
     * @update 2016/2/27 17:38
     * @version 1.0.0
     */
    private boolean isPopuMenuShowing(PopupMenu popupMenu) {
        boolean isShowing = false;
        if (popupMenu != null) {
            try {
                Field field = popupMenu.getClass().getDeclaredField("mPopup");
                if (field != null) {
                    field.setAccessible(true);
                    Object obj = field.get(popupMenu);
                    if (obj instanceof MenuPopupHelper) {
                        MenuPopupHelper popupHelper = (MenuPopupHelper) obj;
                        isShowing = popupHelper.isShowing();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isShowing;
    }

    /**
     * popuMenu每一项点击的事件
     * @author huanghui1
     * @update 2016/3/2 15:05
     * @version: 1.0.0
     */
    class OnPopuMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_show_style:   //列表显示方式，有列表方式个网格方式
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mIsGridStyle = !mIsGridStyle;
                            RecyclerView.LayoutManager layoutManager = mLayoutManagerFactory.getLayoutManager(mContext, mIsGridStyle);
                            mRecyclerView.setLayoutManager(layoutManager);
                            if (mIsGridStyle) { //显示成网格样式
                                if (mItemDecoration != null) {
                                    mRecyclerView.removeItemDecoration(mItemDecoration);
                                }
                                if (mNoteGridAdapter == null) {
                                    mNoteGridAdapter = new NoteGridAdapter(mContext, mNotes);
                                }
                                mRecyclerView.setAdapter(mNoteGridAdapter);
                                item.setTitle(R.string.action_show_list);
                                item.setIcon(R.drawable.ic_action_view_list);
                            } else {    //列表样式
                                mRecyclerView.addItemDecoration(getItemDecoration(mContext));
                                if (mNoteListAdapter == null) {
                                    mNoteListAdapter = new NoteListAdapter(mContext, mNotes);
                                }
                                mRecyclerView.setAdapter(mNoteListAdapter);
                                item.setTitle(R.string.action_show_grid);
                                item.setIcon(R.drawable.ic_action_grid);
                            }

                        }
                    });

                    break;
                case R.id.nav_upload:   //同步
                    break;
                case R.id.nav_sort: //排序
                    break;
            }
            return false;
        }
    }
    
    class NavTextViewHolder extends RecyclerView.ViewHolder {

        TextView mTextView;

        public NavTextViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    class NavViewAdapter extends RecyclerView.Adapter<NavTextViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<Archive> mList;
        private OnItemClickListener mItemClickListener;
        
        private int mSelectedItem = 0;

        public NavViewAdapter(Context context, List<Archive> items) {
            mList = items;
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }

        public void setItemClickListener(OnItemClickListener itemClickListener) {
            this.mItemClickListener = itemClickListener;
        }

        @Override
        public NavTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.nav_list_item, parent, false);
            return new NavTextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NavTextViewHolder holder, final int position) {
            holder.itemView.setSelected(mSelectedItem == position);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        notifyItemChanged(mSelectedItem);
                        mSelectedItem = position;
                        mItemClickListener.onItemClick(v, position);
                    }
                }
            });
            holder.mTextView.setText(mList.get(position).getName());
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }
    
    class NoteListViewHolder extends RecyclerView.ViewHolder {

        public NoteListViewHolder(View itemView) {
            super(itemView);
        }
    }
    
    class NoteListAdapter extends RecyclerView.Adapter<NavTextViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<NoteInfo> mList;

        public NoteListAdapter(Context context, List<NoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        @Override
        public NavTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.nav_list_item, parent, false);
            return new NavTextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NavTextViewHolder holder, int position) {
            holder.mTextView.setText(mList.get(position).getContent());
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }

    class NoteGridViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvOverflow;
        TextView mTvTitle;
        TextView mTvSumary;
        TextView mTvTime;

        public NoteGridViewHolder(View itemView) {
            super(itemView);

            mIvOverflow = (ImageView) itemView.findViewById(R.id.iv_overflow);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mTvSumary = (TextView) itemView.findViewById(R.id.tv_summary);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
        }
    }

    class NoteGridAdapter extends RecyclerView.Adapter<NoteGridViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<NoteInfo> mList;

        public NoteGridAdapter(Context context, List<NoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);
        }

        @Override
        public NoteGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_main_grid, parent, false);
            return new NoteGridViewHolder(view);
        }

        @Override
        public void onBindViewHolder(NoteGridViewHolder holder, int position) {
            holder.mTvTitle.setText(mList.get(position).getContent());
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }
}
