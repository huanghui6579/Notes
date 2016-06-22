package net.ibaixin.notes.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.ibaixin.notes.R;
import net.ibaixin.notes.db.Provider;
import net.ibaixin.notes.db.observer.ContentObserver;
import net.ibaixin.notes.db.observer.Observable;
import net.ibaixin.notes.listener.OnItemClickListener;
import net.ibaixin.notes.model.DeleteState;
import net.ibaixin.notes.model.Folder;
import net.ibaixin.notes.model.NoteInfo;
import net.ibaixin.notes.persistent.FolderManager;
import net.ibaixin.notes.persistent.NoteManager;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.TimeUtil;
import net.ibaixin.notes.util.log.Log;
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

    /**
     * 默认选中的文件夹id
     */
    private int mSelectedFolderId;

    /**
     * 主界面的空的控件
     */
    private View mMainEmptyView;
    
    private NoteContentObserver mNoteObserver;
    
    private NoteManager mNoteManager;
    
    //是否有删除笔记的操作，第一次操作则有删除提示，后面则没有了
    private boolean mHasDeleteOpt;
    
    private List<Folder> mFolders = new ArrayList<>();
    
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
                        int currentItem = target.mNavAdapter.getSelectedItem();
                        if (currentItem != target.mSelectedFolderId) {  //更新默认选中的项
                            target.mNavAdapter.setSelectedItem(target.mSelectedFolderId);
                        }
                        target.mNavAdapter.notifyDataSetChanged();
                        break;
                    case Constants.MSG_SUCCESS2:    //笔记内容加载成功
                        target.mRefresher.setRefreshing(false);
                        List<NoteInfo> list = (List<NoteInfo>) msg.obj;
                        if (!SystemUtil.isEmpty(list)) {  //有数据
                            if (!target.mNotes.isEmpty()) {
                                target.mNotes.clear();
                            }
                            target.mNotes.addAll(list);
                            target.setShowContentStyle(target.mIsGridStyle, false);
                            
                            target.clearEmptyView();
                        } else {    //没有数据
                            if (!target.mNotes.isEmpty()) { //原来有数据
                                target.mNotes.clear();
                                target.setShowContentStyle(target.mIsGridStyle, false);
                            }
                            //隐藏recycleView
                            if (target.mRefresher.getVisibility() == View.VISIBLE) {
                                target.mRefresher.setVisibility(View.GONE);
                            }
                            target.loadEmptyView();
                        }
                        
                        break;
                }
            }
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        //初始化主界面右下角编辑按钮
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, NoteEditActivity.class);
                    startActivity(intent);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                }
            });
        }

        //初始化顶部栏
        if (mToolBar != null) {
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, drawer, mToolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                drawer.addDrawerListener(toggle);
                toggle.syncState();
            }
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

        mNoteManager = NoteManager.getInstance();

        initFolder();

        if (navigationView != null) {
            navigationView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
            mNavAdapter = new NavViewAdapter(this, mFolders);
            mNavAdapter.setItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    view.setSelected(true);
                }
            });
            navigationView.setAdapter(mNavAdapter);
        }

        mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                SystemUtil.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() { 
                        NoteManager noteManager = NoteManager.getInstance();

                        Bundle args = new Bundle();
                        args.putInt("folderId", mSelectedFolderId);
                        List<NoteInfo> list = noteManager.getAllNotes(getCurrentUser(), args);
                        Message msg = mHandler.obtainMessage();
                        msg.what = Constants.MSG_SUCCESS2;
                        msg.obj = list;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        };

        mRefresher.setColorSchemeResources(R.color.colorPrimary);
        mRefresher.setOnRefreshListener(mOnRefreshListener);

        //注册观察者
        registContentObserver();
    }

    @Override
    protected void initData() {
        //初始化配置文件
        initProperties();

        //加载文件夹
        loadFolder();

        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });
    }
    
    /**
     * 初始化配置文件
     * @author huanghui1
     * @update 2016/6/22 17:32
     * @version: 1.0.0
     */
    private void initProperties() {
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                mHasDeleteOpt = sharedPreferences.getBoolean(Constants.PREF_HAS_DELETE_OPT, false);
            }
        });
    }

    /**
     * 加载文件夹
     */
    private void loadFolder() {
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<Folder> list = FolderManager.getInstance().getAllFolders(getCurrentUser(), null);
                if (list != null) {
                    mFolders.addAll(list);
                }
            }
        });
    }
    
    /**
     * 初始化文件夹
     * @author huanghui1
     * @update 2016/3/8 17:38
     * @version: 1.0.0
     */
    private void initFolder() {
        mSelectedFolderId = SystemUtil.getSelectedFolder(mContext);
        Folder archive = new Folder();
        archive.setName(getString(R.string.default_archive));
        mFolders.add(archive);
        
        SystemUtil.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                List<Folder> list = FolderManager.getInstance().getAllFolders(getCurrentUser(), null);
                if (!SystemUtil.isEmpty(list)) {
                    Message msg = mHandler.obtainMessage();
                    msg.obj = list;
                    mHandler.sendMessage(msg);
                }
            }
        });
    }
    
    /**
     * 加载空的提示view
     * @author huanghui1
     * @update 2016/3/9 14:44
     * @version: 1.0.0
     */
    private View loadEmptyView() {
        if (mMainEmptyView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mMainEmptyView = inflater.inflate(R.layout.main_empty_view, null);

            CoordinatorLayout viewGroup = (CoordinatorLayout) findViewById(R.id.content_main);
            CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, CoordinatorLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity = Gravity.CENTER;
            mMainEmptyView.setLayoutParams(layoutParams);

            if (viewGroup != null) {
                viewGroup.addView(mMainEmptyView);
            }
        } else {
            if (mMainEmptyView.getVisibility() != View.VISIBLE) {
                mMainEmptyView.setVisibility(View.VISIBLE);
            }
        }
        return mMainEmptyView;
    }
    
    /**
     * 清除空的提示控件
     * @author huanghui1
     * @update 2016/3/9 15:07
     * @version: 1.0.0
     */
    private void clearEmptyView() {
        if (mMainEmptyView != null) {
            if (mMainEmptyView.getVisibility() == View.VISIBLE) {
                mMainEmptyView.setVisibility(View.GONE);
            }
            CoordinatorLayout viewGroup = (CoordinatorLayout) findViewById(R.id.content_main);
            if (viewGroup != null) {
                viewGroup.removeView(mMainEmptyView);
                mMainEmptyView = null;
            }
        }
    }
    
    /**
     * 注册观察者的监听
     * @author huanghui1
     * @update 2016/3/9 18:10
     * @version: 1.0.0
     */
    private void registContentObserver() {
        mNoteObserver = new NoteContentObserver(mHandler);
        NoteManager.getInstance().addObserver(mNoteObserver);
    }
    
    /**
     * 注销观察者
     * @author huanghui1
     * @update 2016/3/9 18:11
     * @version: 1.0.0
     */
    private void unRegistContentObserver() {
        if (mNoteObserver != null) {
            NoteManager.getInstance().removeObserver(mNoteObserver);
        }
    }

    @Override
    protected boolean showHomeUp() {
        return false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
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

    @Override
    protected void onDestroy() {
        //注销观察者
        unRegistContentObserver();
        super.onDestroy();
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
     * 添加笔记
     * @author huanghui1
     * @update 2016/3/9 17:29
     * @version: 1.0.0
     */
    private void addNote(NoteInfo note) {
        mNotes.add(0, note);
        updateUI(mNotes);
    }
    
    /**
     * 修改笔记
     * @author huanghui1
     * @update 2016/3/9 17:33
     * @version: 1.0.0
     */
    private void updateNote(NoteInfo note) {
        int index = mNotes.indexOf(note);
        if (index != -1) {  //列表中存在
            NoteInfo info = mNotes.get(index);
            
            info.setHash(note.getHash());
            info.setOldContent(info.getContent());
            info.setModifyTime(note.getModifyTime());
            info.setContent(note.getContent());
            info.setFolderId(note.getFolderId());
            info.setHasAttach(note.hasAttach());
            info.setKind(note.getKind());
            info.setRemindId(note.getRemindId());
            info.setSyncState(note.getSyncState());

            updateUI(mNotes);
        }
    }
    
    /**
     * 刷新ui界面
     * @author huanghui1
     * @update 2016/3/10 9:10
     * @version: 1.0.0
     */
    private void updateUI(List<NoteInfo> list) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            setShowContentStyle(mIsGridStyle, false);
            //显示recycleView
            if (mRefresher.getVisibility() != View.VISIBLE) {
                mRefresher.setVisibility(View.VISIBLE);
            }
            clearEmptyView();
        } else {    //没有数据
            setShowContentStyle(mIsGridStyle, false);
            //隐藏recycleView
            if (mRefresher.getVisibility() == View.VISIBLE) {
                mRefresher.setVisibility(View.GONE);
            }
            loadEmptyView();
        }
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
        private List<Folder> mList;
        private OnItemClickListener mItemClickListener;
        
        private int mSelectedItem = 0;

        public NavViewAdapter(Context context, List<Folder> items) {
            mList = items;
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        public int getSelectedItem() {
            return mSelectedItem;
        }

        public void setSelectedItem(int selectedItem) {
            this.mSelectedItem = selectedItem;
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
            Folder folder = mList.get(position);
            final int folderId = folder.getId();
            holder.itemView.setSelected(mSelectedItem == folderId);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        notifyItemChanged(mSelectedItem);
                        mSelectedItem = folderId;
                        mItemClickListener.onItemClick(v, position);
                    }
                }
            });
            holder.mTextView.setText(folder.getName());
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
            NoteInfo note = mList.get(position);
            if (note != null) {
                holder.mIvOverflow.setOnClickListener(new GridItemClickListener(note));
                holder.mTvTitle.setText(note.getContent());
                holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getModifyTime()));
                holder.mTvSumary.setText(note.getContent());
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 网格的每一项点击事件
         * @author huanghui1
         * @update 2016/6/21 11:40
         * @version: 1.0.0
         */
        class GridItemClickListener implements View.OnClickListener {
            private NoteInfo note;

            public GridItemClickListener(NoteInfo note) {
                this.note = note;
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.iv_overflow:
                        PopupMenu itemMenu = createPopuMenu(v, R.menu.grid_item_opt, false, new ItemMenuClickListener(note));
                        itemMenu.show();
                        break;
                }
            }
        }

        /**
         * 菜单的每一项点击监听器
         * @author huanghui1
         * @update 2016/6/21 14:45
         * @version: 1.0.0
         */
        class ItemMenuClickListener implements PopupMenu.OnMenuItemClickListener {
            private NoteInfo note;

            public ItemMenuClickListener(NoteInfo note) {
                this.note = note;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:    //删除
                        if (!mHasDeleteOpt) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setTitle(R.string.prompt)
                                    .setMessage(R.string.confirm_to_trash)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            note.setDeleteState(DeleteState.DELETE_TRASH);
                                            mNoteManager.deleteNote(note);
                                            SystemUtil.getThreadPool().execute(new Runnable() {
                                                @Override
                                                public void run() {
                                                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    editor.putBoolean(Constants.PREF_HAS_DELETE_OPT, true);
                                                    editor.apply();
                                                    mHasDeleteOpt = true;
                                                }
                                            });
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show();
                        } else {
                            note.setDeleteState(DeleteState.DELETE_TRASH);
                            mNoteManager.deleteNote(note);
                        }
                        break;
                    case R.id.action_info:  //详情
                        String info = note.getNoteInfo(mContext);
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setTitle(note.getContent())
                                .setMessage(info)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        break;
                }
                return false;
            }
        }

    }
    
    /**
     * 笔记的观察者
     * @author huanghui1
     * @update 2016/3/9 17:04
     * @version: 1.0.0
     */
    class NoteContentObserver extends ContentObserver {

        public NoteContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            NoteInfo noteInfo = null;
            switch (notifyFlag) {
                case Provider.NoteColumns.NOTIFY_FLAG:  //笔记的通知
                    if (data != null) {
                        noteInfo = (NoteInfo) data;
                    }
                    switch (notifyType) {
                        case ADD:   //添加
                            Log.d(TAG, "------addNote----" + noteInfo);
                            if (noteInfo != null) {
                                addNote(noteInfo);
                            }
                            break;
                        case UPDATE:    //修改笔记
                            Log.d(TAG, "------updateNote----" + noteInfo);
                            if (noteInfo != null) {
                                updateNote(noteInfo);
                            }
                            break;
                        case DELETE:    //删除、移到回收站
                            Log.d(TAG, "------deleteNote----" + noteInfo);
                            if (noteInfo != null) {
                                mNotes.remove(noteInfo);
                                updateUI(mNotes);
                            }
                            break;
                    }
                    break;
            }
        }
    }
}
