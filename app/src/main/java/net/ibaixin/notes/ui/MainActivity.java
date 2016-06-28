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
import net.ibaixin.notes.helper.AdapterRefreshHelper;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 主界面
 * @author huanghui1
 * @update 2016/2/24 19:25
 * @version: 1.0.0
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {
    
    private static final int MSG_SELECT_NAV = 3;

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
     * 默认选中的文件夹
     */
    private Folder mSelectedFolder;

    /**
     * 主界面的空的控件
     */
    private View mMainEmptyView;
    
    private NoteContentObserver mNoteObserver;
    
    private NoteManager mNoteManager;
    
    //是否有删除笔记的操作，第一次操作则有删除提示，后面则没有了
    private boolean mHasDeleteOpt;
    
    private View mNavArchiveView;
    private View mNavTrashView;
    private View mNavSettingsView;
    
    private DrawerLayout mNavDrawer;
    
    private List<Folder> mFolders = new ArrayList<>();
    
    private final Handler mHandler = new MyHandler(this);

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
                    intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, mSelectedFolderId);
                    startActivity(intent);
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                }
            });
        }

        //初始化顶部栏
        if (mToolBar != null) {
            mNavDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (mNavDrawer != null) {
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, mNavDrawer, mToolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                mNavDrawer.addDrawerListener(toggle);
                toggle.syncState();
            }
        }

        mNavArchiveView = findViewById(R.id.nav_archive);
        mNavTrashView = findViewById(R.id.nav_trash);
        mNavSettingsView = findViewById(R.id.nav_settings);

        mNavArchiveView.setOnClickListener(this);
        mNavTrashView.setOnClickListener(this);
        mNavSettingsView.setOnClickListener(this);

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

        if (navigationView != null) {
            navigationView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
            mNavAdapter = new NavViewAdapter(this, mFolders);
            mNavAdapter.setItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    view.setSelected(true);
                    
                    Folder folder = mFolders.get(position);
                    selectFolder(folder);
                    
                }
            });
            navigationView.setAdapter(mNavAdapter);
        }

        mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        //初始化文件夹
                        initFolder();

                        if (isFolderAllDisable()) { //不能加载所有文件夹里的笔记，则加载第一个文件夹
                            //加载文件夹
                            loadFolder(false);

                            reLoadFisrtFolder(mFolders.get(0));
                        } else {
                            //加载笔记
                            loadNotes(mSelectedFolderId);

                            doInbackground(new Runnable() {
                                @Override
                                public void run() {
                                    //加载文件夹
                                    loadFolder(isShowFolderAll());
                                }
                            });
                        }
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
    public boolean isSwipeBackEnabled() {
        return false;
    }

    @Override
    protected void initData() {
        //初始化配置文件
        initProperties();

        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });
    }

    /**
     * 选择文件夹
     * @param folder
     */
    private void selectFolder(final Folder folder) {
        doInbackground(new Runnable() {
            @Override
            public void run() {

                reLoadFisrtFolder(folder);
                
                mHandler.sendEmptyMessage(MSG_SELECT_NAV);
            }
        });
    }
    
    /**
     * 拷贝文件夹
     * @author huanghui1
     * @update 2016/6/28 21:37
     * @version: 1.0.0
     */
    private Folder cloneFolder(Folder folder) {
        Folder newFolder = null;
        try {
            newFolder = (Folder) folder.clone();
        } catch (CloneNotSupportedException e) {
            Log.e(TAG, "--cloneFolder--error---" + e.getMessage());
        }
        if (newFolder == null) {
            newFolder = new Folder();
            newFolder.setId(folder.getId());
            newFolder.setSId(folder.getSId());
            newFolder.setName(folder.getName());
        }
        return newFolder;
    }

    /**
     * 重新加载第一个文件夹的文件
     */
    private void reLoadFisrtFolder(Folder folder) {
        if (folder == null) {
            folder = new Folder();
        }
        mSelectedFolderId = folder.getId();
        SystemUtil.setSelectedFolder(mContext, mSelectedFolderId);

        mSelectedFolder = cloneFolder(folder);
        
        //加载笔记
        loadNotes(mSelectedFolderId);
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
     * 加载笔记
     */
    private void loadNotes(final int folderId) {
        NoteManager noteManager = NoteManager.getInstance();
        Bundle args = new Bundle();
        args.putInt("folderId", folderId);
        List<NoteInfo> list = noteManager.getAllNotes(getCurrentUser(), args);
        Message msg = mHandler.obtainMessage();
        msg.what = Constants.MSG_SUCCESS2;
        msg.obj = list;
        mHandler.sendMessage(msg);
    }
    
    /**
     * 初始化文件夹
     * @author huanghui1
     * @update 2016/3/8 17:38
     * @version: 1.0.0
     */
    private void initFolder() {
        mSelectedFolderId = SystemUtil.getSelectedFolder(mContext);
        
        Folder folder = new Folder();
        folder.setId(mSelectedFolderId);
        mSelectedFolder = cloneFolder(folder);
        
        Log.d(TAG, "------initFolder----SelectedFolderId-----" + mSelectedFolderId);
        
        if (!isShowFolderAll()) {  //“所有文件夹”没有显示
            Log.d(TAG, "---initFolder-----folder all is hide---");
            return;
        }

        final Folder archive = getFolderAll();
        
        mFolders.add(archive);
        
    }
    
    /**
     * 所有文件夹里的笔记是否不能被加载
     * @author huanghui1
     * @update 2016/6/28 9:57
     * @version: 1.0.0
     */
    private boolean isFolderAllDisable() {
        return mSelectedFolderId == 0 && !isShowFolderAll();
    }
    
    /**
     * 从数据库加载文件夹
     * @author huanghui1
     * @update 2016/6/23 16:25
     * @version: 1.0.0
     */
    private void loadFolder(boolean hasFolderAll) {
        List<Folder> list = FolderManager.getInstance().getAllFolders(getCurrentUser(), null);
        if (!SystemUtil.isEmpty(list)) {
            if (hasFolderAll) {    //显示所有文件夹
                Folder archive = getFolderAll();
                list.add(0, archive);
            }
            Message msg = mHandler.obtainMessage();
            msg.obj = list;
            msg.what = Constants.MSG_SUCCESS;
            mHandler.sendMessage(msg);
        }
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
        FolderManager.getInstance().addObserver(mNoteObserver);
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
            FolderManager.getInstance().removeObserver(mNoteObserver);
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
     * @param refreshHelper 更新的类型，0：全部刷新，1：添加，2：更新单个，3：删除                    
     * @author huanghui1
     * @update 2016/3/1 11:45
     * @version: 1.0.0
     */
    private void setShowContentStyle(boolean isGridStyle, boolean resetAdapter, AdapterRefreshHelper refreshHelper) {
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
                if (refreshHelper == null || refreshHelper.type == AdapterRefreshHelper.TYPE_NONE) {
                    mNoteGridAdapter.notifyDataSetChanged();
                } else {
                    refreshHelper.refresh(mNoteGridAdapter);
                }
            }
            int padding = mContext.getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
            mRecyclerView.setPadding(padding, 0, padding, 0);
        } else {    //列表样式
            mRecyclerView.addItemDecoration(getItemDecoration(mContext));
            if (mNoteListAdapter == null) {
                mNoteListAdapter = new NoteListAdapter(mContext, mNotes);
                resetAdapter = true;
            }
            if (resetAdapter) {
                mRecyclerView.setAdapter(mNoteListAdapter);
            } else {
                if (refreshHelper == null || refreshHelper.type == AdapterRefreshHelper.TYPE_NONE) {
                    mNoteListAdapter.notifyDataSetChanged();
                } else {
                    refreshHelper.refresh(mNoteListAdapter);
                }
            }
            mRecyclerView.setPadding(0, 0, 0, 0);
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
     * 获取第一个文件夹
     * @param list
     * @return
     */
    private Folder getFirstFolder(List<Folder> list) {
        Folder folder = null;
        if (list == null || list.size() == 0) {
            folder = new Folder();
            folder.setId(0);
        } else {
            folder = list.get(0);
        }
        return folder;
    }
    
    /**
     * 添加笔记
     * @author huanghui1
     * @update 2016/3/9 17:29
     * @version: 1.0.0
     */
    private void addNote(NoteInfo note) {
        mNotes.add(0, note);
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        refreshHelper.position = 0;
        refreshUI(mNotes, refreshHelper);
    }

    /**
     * 删除笔记
     * @param note
     */
    private void deleteNote(NoteInfo note) {
        int index = mNotes.indexOf(note);
        if (index != -1) {  //列表中存在
            mNotes.remove(index);
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
            refreshHelper.position = index;
            refreshUI(mNotes, refreshHelper);
        }
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

            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
            refreshHelper.position = index;
            refreshUI(mNotes, refreshHelper);
        }
    }
    
    /**
     * 添加文件夹
     * @author huanghui1
     * @update 2016/6/27 21:27
     * @version: 1.0.0
     */
    private void updateFolder(Folder folder) {
        
        if (folder.isEmpty()) { //更新的是所有文件夹，则查看其隐藏和显示的状态
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            
            boolean reloadNotes = false;
            boolean updateFirst = false;
            
            if (folder.isShow()) {  //改为显示
                Folder folderAll = getFolderAll();
                mFolders.add(0, folderAll);
                refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
                //没有其他文件夹，则重新加载所有笔记
                reloadNotes = mFolders.size() == 1 || mSelectedFolderId == 0;
                
            } else {    //改为隐藏
                mFolders.remove(0);
                refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
                //没有其他文件夹，则重新加载所有笔记
                reloadNotes = mFolders.size() == 1 || mSelectedFolderId == 0;
                updateFirst = reloadNotes;
            }
            
            refreshHelper.position = 0;
            if (reloadNotes && mSelectedFolderId == 0) {
                mNavAdapter.setSelectedItem(0);
            }
            refreshNavUI(refreshHelper);
            
            if (updateFirst) {
                refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                refreshHelper.position = 0;
                Folder firstFolder = getFirstFolder(mFolders);
                mNavAdapter.setSelectedItem(firstFolder.getId());
                refreshNavUI(refreshHelper);
            }
            
            if (reloadNotes) {  //重新加载笔记
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        //选中第一个文件夹
                        reLoadFisrtFolder(mFolders.get(0));
                    }
                });
            }
        } else {
            int index = mFolders.indexOf(folder);

            if (index != -1) {
                Folder tFolder = mFolders.get(index);
                setUpNewFolder(tFolder, folder);

                AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                refreshHelper.position = index;
                
                refreshNavUI(refreshHelper);
            }
        }
    }
    
    /**
     * 更新文件夹的数据
     * @author huanghui1
     * @update 2016/6/28 21:04
     * @version: 1.0.0
     */
    private void setUpNewFolder(Folder oldFolder, Folder newFolder) {
        oldFolder.setCount(newFolder.getCount());
        oldFolder.setSyncState(newFolder.getSyncState());
        oldFolder.setDeleteState(newFolder.getDeleteState());
        oldFolder.setSort(newFolder.getSort());
        oldFolder.setName(newFolder.getName());
        oldFolder.setIsLock(newFolder.isLock());
        oldFolder.setModifyTime(newFolder.getModifyTime());
    }
    
    /**
     * 文件夹排序
     * @author huanghui1
     * @update 2016/6/28 21:01
     * @version: 1.0.0
     */
    private void swapFolder(Folder fromFolder, Folder toFolder) {
        int fromIndex = mFolders.indexOf(fromFolder);
        int toIndex = mFolders.indexOf(toFolder);
        if (fromIndex != -1 && toIndex != -1) {
            Folder fromOldFolder = mFolders.get(fromIndex);
            setUpNewFolder(fromOldFolder, fromFolder);
            Folder toOldFolder = mFolders.get(toIndex);
            setUpNewFolder(toOldFolder, toFolder);

            Collections.swap(mFolders, fromIndex, toIndex);
            
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_SWAP;
            refreshHelper.fromPosition = fromIndex;
            refreshHelper.toPosition = toIndex;
            
            refreshNavUI(refreshHelper);
        }
    }

    /**
     * 添加文件夹
     * @param folder
     */
    private void addFolder(Folder folder) {
        mFolders.add(folder);

        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        refreshHelper.position = mFolders.size() - 1;
        refreshNavUI(refreshHelper);
    }

    /**
     * 删除文件夹
     * @param folder
     */
    private void deleteFolder(Folder folder) {
        int index = mFolders.indexOf(folder);
        if (index != -1) {  //列表中存在
            
            int deleteId = folder.getId();

            mFolders.remove(index);
            
            if (mFolders.size() == 0) { //没有文件夹了，则显示“所有文件夹”
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        FolderManager.getInstance().updateShowState(mContext, true);
                    }
                });
                
            } else {
                if (deleteId == mSelectedFolderId) {    //删除的是当前选中的项，则删除后，选择第一项
                    doInbackground(new Runnable() {
                        @Override
                        public void run() {
                            reLoadFisrtFolder(null);
                        }
                    });

                    //选择第一项
                    mNavAdapter.setSelectedItem(0);

                    AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                    refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                    refreshHelper.position = 0;
                    refreshNavUI(refreshHelper);
                }
            }
            
            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
            refreshHelper.position = index;
            refreshNavUI(refreshHelper);
        }
    }
    
    /**
     * 刷新ui界面
     * @author huanghui1
     * @update 2016/3/10 9:10
     * @version: 1.0.0
     */
    private void refreshUI(List<NoteInfo> list, AdapterRefreshHelper refreshHelper) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            setShowContentStyle(mIsGridStyle, false, refreshHelper);
            //显示recycleView
            if (mRefresher.getVisibility() != View.VISIBLE) {
                mRefresher.setVisibility(View.VISIBLE);
            }
            clearEmptyView();
        } else {    //没有数据
            setShowContentStyle(mIsGridStyle, false, null);
            //隐藏recycleView
            if (mRefresher.getVisibility() == View.VISIBLE) {
                mRefresher.setVisibility(View.GONE);
            }
            loadEmptyView();
        }
    }
    
    /**
     * 刷新菜单
     * @author huanghui1
     * @update 2016/6/27 21:31
     * @version: 1.0.0
     */
    private void refreshNavUI(AdapterRefreshHelper refreshHelper) {
        if (mNavAdapter != null) {
            refreshHelper.refresh(mNavAdapter);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.nav_archive:  //文件夹
                intent = new Intent(mContext, FolderListActivity.class);
                startActivity(intent);
                break;
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
                                int padding = mContext.getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
                                mRecyclerView.setPadding(padding, 0, padding, 0);
                                mRecyclerView.setAdapter(mNoteGridAdapter);
                                item.setTitle(R.string.action_show_list);
                                item.setIcon(R.drawable.ic_action_view_list);
                            } else {    //列表样式
                                mRecyclerView.setPadding(0, 0, 0, 0);
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
        public void onBindViewHolder(final NavTextViewHolder holder, final int position) {
            Folder folder = mList.get(position);
            final int folderId = folder.getId();
            holder.itemView.setSelected(mSelectedItem == folderId);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        int tPos = holder.getAdapterPosition();
                        Folder pForder = new Folder();
                        pForder.setId(mSelectedItem);
                        int preSelectIndex = mFolders.indexOf(pForder);
                        mSelectedItem = folderId;
                        if (preSelectIndex != -1) {
                            notifyItemChanged(preSelectIndex);
                        }
                        mItemClickListener.onItemClick(v, tPos);
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
                holder.mTvTitle.setText(note.getTitle());
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
                        builder.setTitle(note.getTitle())
                                .setMessage(info)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        break;
                }
                return false;
            }
        }

    }

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
                    case Constants.MSG_SUCCESS: //文件夹加载完毕
                        List<Folder> folderList = (List<Folder>) msg.obj;
                        target.mFolders.clear();
                        if (folderList != null && folderList.size() > 0) {
                            target.mFolders.addAll(folderList);
                        }
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
                            target.refreshUI(target.mNotes, null);
                        } else {    //没有数据
                            if (!target.mNotes.isEmpty()) { //原来有数据
                                target.mNotes.clear();
                            }
                            target.refreshUI(target.mNotes, null);
                        }

                        break;
                    case MSG_SELECT_NAV:    //选择左菜单，菜单消失
                        if (target.mNavDrawer != null) {
                            target.mNavDrawer.closeDrawers();
                        }
                        break;
                }
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
            switch (notifyFlag) {
                case Provider.NoteColumns.NOTIFY_FLAG:  //笔记的通知
                    NoteInfo noteInfo = null;
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
                                deleteNote(noteInfo);
                            }
                            break;
                    }
                    break;
                case Provider.FolderColumns.NOTIFY_FLAG:    //文件夹的通知
                    Folder folder = null;
                    if (data == null) {
                        return;
                    }
                    
                    switch (notifyType) {
                        case ADD:   //添加文件夹
                            folder = (Folder) data;
                            Log.d(TAG, "------addFolder----" + folder);
                            addFolder(folder);
                            break;
                        case UPDATE:   //更新文件夹
                            if (data instanceof Folder) {
                                folder = (Folder) data;
                                Log.d(TAG, "------updateFolder----" + folder);
                                updateFolder(folder);
                            } else if (data instanceof Map) {   //文件夹排序
                                Map<String, Object> map = (Map) data;
                                Folder fromFolder = (Folder) map.get(Constants.ARG_CORE_OBJ);
                                Folder toFolder = (Folder) map.get(Constants.ARG_SUB_OBJ);
                                swapFolder(fromFolder, toFolder);
                            }
                            break;
                        case DELETE:   //删除文件夹
                            folder = (Folder) data;
                            Log.d(TAG, "------deleteFolder----" + folder);
                            deleteFolder(folder);
                            break;
                    }
                    break;
            }
        }
    }
}
