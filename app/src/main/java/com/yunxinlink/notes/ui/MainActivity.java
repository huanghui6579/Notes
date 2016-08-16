package com.yunxinlink.notes.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.graphics.Palette;
import android.support.v7.view.ActionMode;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.listener.OnCheckedChangeListener;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.listener.OnItemLongClickListener;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.ImageUtil;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;
import com.yunxinlink.notes.widget.DividerItemDecoration;
import com.yunxinlink.notes.widget.LayoutManagerFactory;
import com.yunxinlink.notes.widget.NoteItemViewAware;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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

    private static final int MSG_MOVE_FAILED = 4;
    private static final int MSG_MOVE_SUCCESS = 5;
    private static final int MSG_PALETTE_COLOR = 6;

    private NavViewAdapter mNavAdapter;
    
    private SwipeRefreshLayout mRefresher;
    
    private RecyclerView mRecyclerView;
    
    private NoteListAdapter mNoteListAdapter;
    private NoteGridAdapter mNoteGridAdapter;

    private List<DetailNoteInfo> mNotes;
    
    private SwipeRefreshLayout.OnRefreshListener mOnRefreshListener;

    /**
     * 主界面右上角菜单
     */
    private PopupMenu mMainPopuMenu;

    /**
     * 显示的是否是网格风格
     */
    private boolean mIsGridStyle = true;
    /**
     * 笔记的排序方式
     */
    private int mNoteSort;

    private LayoutManagerFactory mLayoutManagerFactory;

    private RecyclerView.ItemDecoration mItemDecoration;
    
    /*
    主界面更多菜单
     */
    private MenuItem mActionOverflow;

    /**
     * 默认选中的文件夹id
     */
    private String mSelectedFolderId;

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

    /**
     * 是否是多选模式
     */
    private boolean mIsChooseMode;
    
    private ActionMode mActionMode;
    
    //选择的笔记集合
    private List<DetailNoteInfo> mSelectedList;

    /**
     * 新建按钮
     */
    private FloatingActionButton mFab;
    
    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        //初始化主界面右下角编辑按钮
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, NoteEditActivity.class);
//                    Intent intent = new Intent(mContext, TestActivity.class);
                    intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, mSelectedFolderId);
                    intent.putExtra(NoteEditActivity.ARG_OPT_DELETE, mHasDeleteOpt);
                    startActivity(intent);

                    //退出选择模式
                    outActionMode(true);
                    
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

        //初始化左侧导航菜单
        RecyclerView navigationView = (RecyclerView) findViewById(R.id.nav_view);

        mNoteManager = NoteManager.getInstance();

        if (navigationView != null) {
            navigationView.setLayoutManager(new LinearLayoutManager(this));//这里用线性显示 类似于listview
            mNavAdapter = new NavViewAdapter(this, mFolders);
            mNavAdapter.setItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view) {
                    Integer pos = (Integer) view.getTag();
                    if (pos == null) {
                        return;
                    }
                    view.setSelected(true);
                    Folder folder = mFolders.get(pos);
                    selectFolder(folder);
                    
                    //退出选择模式
                    outActionMode(false);
                    
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
                        doOnRefresh();
                    }
                });
                
            }
        };

        mRefresher.setColorSchemeResources(R.color.colorPrimary);
        mRefresher.setOnRefreshListener(mOnRefreshListener);

        //注册观察者
        registContentObserver();
    }

    /**
     * 刷新数据
     */
    private void doOnRefresh() {
        //初始化文件夹
        initFolder();

        //初始化笔记的一些设置项
        initNoteSettings();

        if (isFolderAllDisable()) { //不能加载所有文件夹里的笔记，则加载第一个文件夹
            //加载文件夹
            List<Folder> folders = loadFolder(false);

            if (folders != null && folders.size() > 0) {
                reLoadFirstFolder(folders.get(0));
            } else {
                //加载笔记
                loadNotes(mSelectedFolderId, mNoteSort);
            }

        } else {
            //加载笔记
            loadNotes(mSelectedFolderId, mNoteSort);

            doInbackground(new Runnable() {
                @Override
                public void run() {
                    //加载文件夹
                    loadFolder(isShowFolderAll());
                }
            });
        }
    }
    
    /**
     * 初始化主界面的显示方式，默认网格
     * @author huanghui1
     * @update 2016/6/30 20:42
     * @version: 1.0.0
     */
    private void initNoteSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mIsGridStyle = sharedPreferences.getBoolean(Constants.PREF_IS_GRID_STYLE, true);
        mNoteSort = sharedPreferences.getInt(Constants.PREF_NOTE_SORT, 0);
    }
    
    /**
     * 更新主界面的显示方式
     * @param isGridStyle 是否是网格显示
     * @author huanghui1
     * @update 2016/6/30 20:45
     * @version: 1.0.0
     */
    private void updateShowStyle(boolean isGridStyle) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Constants.PREF_IS_GRID_STYLE, isGridStyle);
        editor.apply();
    }

    /**
     * 修改笔记的排序方式
     * @param sort
     */
    private void updateNoteSort(int sort) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(Constants.PREF_NOTE_SORT, sort);
        editor.apply();
    }

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    @Override
    protected void initData() {
        //初始化配置文件
        initProperties();

        //下拉刷新
        pullRefresh();

        requestPermission();
    }

    /**
     * 手动下拉刷新
     */
    private void pullRefresh() {
        mRefresher.post(new Runnable() {
            @Override
            public void run() {
                mRefresher.setRefreshing(true);
                mOnRefreshListener.onRefresh();
            }
        });
    }

    /**
     * 请求权限
     */
    private void requestPermission() {
        final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this, permissions, new PermissionsResultAction() {

            @Override
            public void onGranted() {
                KLog.d(TAG, "---requestPermission---onGranted----permission--" + permissions[0]);
//                Toast.makeText(mContext,"onGranted",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDenied(String permission) {
                KLog.d(TAG, "-----onDenied---permission----" + permission);
                if (permissions[0].equals(permission)) {
                    NoteUtil.onPermissionDenied(MainActivity.this, permission, R.string.tip_mkfile_error, R.string.tip_grant_write_storage_failed);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        KLog.i(TAG, "Activity-onRequestPermissionsResult() PermissionsManager.notifyPermissionsChange()");
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults);
    }

    /**
     * 选择文件夹
     * @param folder
     */
    private void selectFolder(final Folder folder) {
        //关闭左侧菜单
        closeNavDrawer();
        
        String subTitle = folder.isEmpty() ? null : folder.getName();
        
        updateSubTitle(subTitle);
        
        doInbackground(new Runnable() {
            @Override
            public void run() {

                reLoadFirstFolder(folder);
            }
        });
    }
    
    /**
     * 关闭左侧菜单
     * @author huanghui1
     * @update 2016/6/29 19:47
     * @version: 1.0.0
     */
    private void closeNavDrawer() {
        if (mNavDrawer != null) {
            mNavDrawer.closeDrawers();
        }
    }
    
    /**
     * 更新子标题
     * @author huanghui1
     * @update 2016/6/29 19:51
     * @version: 1.0.0
     */
    private void updateSubTitle(String subTitle) {
        if (mToolBar != null) {
            mToolBar.setSubtitle(subTitle);
        }
    }

    /**
     * 重新加载第一个文件夹的文件
     */
    private void reLoadFirstFolder(Folder folder) {
        if (folder == null) {
            folder = new Folder();
        }
        mSelectedFolderId = folder.getSId();
        SystemUtil.setSelectedFolder(mContext, mSelectedFolderId);
        
        //加载笔记
        loadNotes(mSelectedFolderId, mNoteSort);
    }
    
    /**
     * 初始化配置文件
     * @author huanghui1
     * @update 2016/6/22 17:32
     * @version: 1.0.0
     */
    private void initProperties() {
        doInbackground(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                mHasDeleteOpt = sharedPreferences.getBoolean(Constants.PREF_HAS_DELETE_OPT, false);
            }
        });
    }

    /**
     * 加载笔记
     * @param folderId 所属笔记本
     * @param sort 排序方式                
     */
    private void loadNotes(final String folderId, int sort) {
        NoteManager noteManager = NoteManager.getInstance();
        Bundle args = new Bundle();
        if (!TextUtils.isEmpty(folderId)) {
            args.putString("folderId", folderId);
        }
        args.putInt("sort", sort);
        List<DetailNoteInfo> list = noteManager.getAllDetailNotes(getCurrentUser(), args);
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
        folder.setSId(mSelectedFolderId);
        
        String subTitle = getSubTitle(mSelectedFolderId);

        //更新子标题
        updateSubTitle(subTitle);
        
        KLog.d(TAG, "------initFolder----SelectedFolderId-----" + mSelectedFolderId);
        
        if (!isShowFolderAll()) {  //“所有文件夹”没有显示
            KLog.d(TAG, "---initFolder-----folder all is hide---");
            return;
        }

        final Folder archive = getFolderAll();
        mFolders.clear();
        mFolders.add(archive);
        
    }
    
    /**
     * 获取所选择文件夹的名称
     * @author huanghui1
     * @update 2016/6/29 20:10
     * @version: 1.0.0
     */
    private String getSubTitle(String folderSid) {
        String subTitle = null;

        if (!TextUtils.isEmpty(folderSid)) {
            Folder tFolder = FolderCache.getInstance().getCacheFolder(folderSid);
            if (tFolder != null) {
                subTitle = tFolder.getName();
            }
        }
        return subTitle;
    }
    
    /**
     * 所有文件夹里的笔记是否不能被加载
     * @author huanghui1
     * @update 2016/6/28 9:57
     * @version: 1.0.0
     */
    private boolean isFolderAllDisable() {
        return TextUtils.isEmpty(mSelectedFolderId) && !isShowFolderAll();
    }
    
    /**
     * 从数据库加载文件夹
     * @author huanghui1
     * @update 2016/6/23 16:25
     * @version: 1.0.0
     */
    private List<Folder> loadFolder(boolean hasFolderAll) {
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
        return list;
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

                createPopMenu(view);
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
                    createPopMenu(view);
                }
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        //注销观察者
        unRegistContentObserver();
        mIsChooseMode = false;
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
     * @param author 菜单要在哪个view上弹出
     * @author tiger
     * @update 2016/2/27 17:46
     * @version 1.0.0
     */
    private PopupMenu createPopMenu(View author) {
        if (author != null) {
            if (mMainPopuMenu == null) {
                mMainPopuMenu = createPopMenu(author, R.menu.main_overflow, true, new OnPopuMenuItemClickListener());
                if (!mIsGridStyle) {    //开始就显示列表，则菜单为网格
                    Menu menu = mMainPopuMenu.getMenu();
                    MenuItem menuItem = menu.findItem(R.id.nav_show_style);
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
            /*if (mItemDecoration != null) {
                mRecyclerView.removeItemDecoration(mItemDecoration);
            }*/
            if (mNoteGridAdapter == null) {
                initNoteAdapter(true);
                mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(this, true));
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
            if (mRecyclerView.getPaddingLeft() == 0) {
                int padding = mContext.getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
                mRecyclerView.setPadding(padding, 0, padding, 0);
            }
        } else {    //列表样式
//            mRecyclerView.addItemDecoration(getItemDecoration(mContext));
            if (mNoteListAdapter == null) {
                initNoteAdapter(false);
                mRecyclerView.setLayoutManager(mLayoutManagerFactory.getLayoutManager(this, false));
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
            if (mRecyclerView.getPaddingLeft() != 0) {
                mRecyclerView.setPadding(0, 0, 0, 0);
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
     * 获取第一个文件夹
     * @param list 文件夹列表
     * @return 返回
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
    private void addNote(DetailNoteInfo note) {
        mNotes.add(0, note);
        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
        refreshHelper.type = AdapterRefreshHelper.TYPE_ADD;
        refreshHelper.position = 0;
        refreshUI(mNotes, refreshHelper);
    }

    /**
     * 删除笔记
     * @param detailNote 笔记
     * @param isMove 是指只是移动笔记到其他文件文件，如果是指移动，那么在"所有文件夹"中就不需要删除了
     */
    private void deleteNote(DetailNoteInfo detailNote, boolean isMove) {
        int index = mNotes.indexOf(detailNote);
        if (index != -1) {  //列表中存在
            if (isMove) {
                NoteInfo note = detailNote.getNoteInfo();
                setupUpdateNote(mNotes.get(index).getNoteInfo(), note);
            } else {
                mNotes.remove(index);
                AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
                refreshHelper.position = index;
                refreshUI(mNotes, refreshHelper);
            }
        }
    }

    /**
     * 移除多个笔记，并刷新界面
     * @param list 笔记列表
     * @param isMove 是指只是移动笔记到其他文件文件，如果是指移动，那么在"所有文件夹"中就不需要删除了
     */
    private void deleteNotes(List<DetailNoteInfo> list, boolean isMove) {
        if (list != null && list.size() > 0) {
            if (isMove) {   //移动到其他文件夹
                for (DetailNoteInfo detailNote : list) {
                    int index = mNotes.indexOf(detailNote);
                    if (index != -1) {
                        NoteInfo note = detailNote.getNoteInfo();
                        setupUpdateNote(mNotes.get(index).getNoteInfo(), note);
                    }
                }
            } else {
                mNotes.removeAll(list);
                refreshUI(mNotes, null);
            }
        }
    }
    
    /**
     * 删除单条笔记
     * @param detailNote 笔记
     * @author huanghui1
     * @update 2016/6/29 19:37
     * @version: 1.0.0
     */
    private void handleDeleteNote(final DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);
        NoteUtil.handleDeleteNote(mContext, list, mHasDeleteOpt);
    }

    /**
     * 保存删除操作的记录
     */
    public void saveDeleteOpt() {
        if (!mHasDeleteOpt) {   //之前是否有删除操作，如果没有，则需保存  
            doInbackground(new Runnable() {
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
    }

    /**
     * 显示编辑笔记
     * @param note 笔记
     */
    private void showNoteInfo(NoteInfo note) {
        Intent intent = new Intent(mContext, NoteEditActivity.class);
        intent.putExtra(NoteEditActivity.ARG_NOTE_ID, note.getId());
        intent.putExtra(NoteEditActivity.ARG_NOTE_SID, note.getSId());
        intent.putExtra(NoteEditActivity.ARG_IS_NOTE_TEXT, !note.isDetailNote());
        intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, note.getFolderId());
        intent.putExtra(NoteEditActivity.ARG_OPT_DELETE, mHasDeleteOpt);
        startActivity(intent);
    }
    
    /**
     * 修改笔记
     * @author huanghui1
     * @update 2016/3/9 17:33
     * @version: 1.0.0
     */
    private void updateNote(DetailNoteInfo detailNote) {
        int index = mNotes.indexOf(detailNote);
        NoteInfo note = detailNote.getNoteInfo();
        if (index != -1) {  //列表中存在
            DetailNoteInfo oldDetail = mNotes.get(index);
            NoteInfo info = oldDetail.getNoteInfo();
            setupUpdateNote(info, note);
            oldDetail.setLastAttach(detailNote.getLastAttach());
            oldDetail.setDetailList(detailNote.getDetailList());
            
            //先删除原来位置的
            mNotes.remove(index);
            //再添加到0位
            mNotes.add(0, oldDetail);

            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_SWAP;
            refreshHelper.fromPosition = index;
            refreshHelper.toPosition = 0;
            refreshHelper.position = index;
            refreshUI(mNotes, refreshHelper);
            mRecyclerView.scrollToPosition(0);
        }
    }

    /**
     * 更新笔记的内容
     * @param oldNote 原来的笔记
     * @param newNote 新的笔记
     */
    private void setupUpdateNote(NoteInfo oldNote, NoteInfo newNote) {
        oldNote.setHash(newNote.getHash());
        oldNote.setOldContent(oldNote.getContent());
        oldNote.setModifyTime(newNote.getModifyTime());
        oldNote.setContent(newNote.getContent());
        oldNote.setShowContent(newNote.getShowContent());
        oldNote.setHasAttach(newNote.hasAttach());
        oldNote.setKind(newNote.getKind());
        oldNote.setRemindId(newNote.getRemindId());
        oldNote.setSyncState(newNote.getSyncState());
        oldNote.setFolderId(newNote.getFolderId());
        oldNote.setTitle(newNote.getTitle());
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
                reloadNotes = mFolders.size() == 1 || TextUtils.isEmpty(mSelectedFolderId);
                
            } else {    //改为隐藏
                mFolders.remove(0);
                refreshHelper.type = AdapterRefreshHelper.TYPE_DELETE;
                //没有其他文件夹，则重新加载所有笔记
                reloadNotes = mFolders.size() == 1 || TextUtils.isEmpty(mSelectedFolderId);
                updateFirst = reloadNotes;
            }
            
            refreshHelper.position = 0;
            if (reloadNotes && TextUtils.isEmpty(mSelectedFolderId)) {
                mNavAdapter.setSelectedItem(null);
            }
            refreshNavUI(refreshHelper);
            
            if (updateFirst) {
                refreshHelper = new AdapterRefreshHelper();
                refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                refreshHelper.position = 0;
                Folder firstFolder = getFirstFolder(mFolders);
                mSelectedFolderId = firstFolder.getSId();
                mNavAdapter.setSelectedItem(firstFolder.getSId());
                refreshNavUI(refreshHelper);
            }
            
            if (reloadNotes) {  //重新加载笔记
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        //选中第一个文件夹
                        reLoadFirstFolder(mFolders.get(0));
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
        
        //更新子标题
        updateSubTitle(getSubTitle(mSelectedFolderId));
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
            
            String deleteId = folder.getSId();

            mFolders.remove(index);
            
            if (mFolders.size() == 0) { //没有文件夹了，则显示“所有文件夹”
                //更新子标题
                updateSubTitle(null);
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        FolderManager.getInstance().updateShowState(mContext, true);
                    }
                });
                
            } else {
                if (deleteId.equals(mSelectedFolderId)) {    //删除的是当前选中的项，则删除后，选择第一项
                    final Folder firstFolder = mFolders.get(0);
                    mSelectedFolderId = folder.getSId();
                    doInbackground(new Runnable() {
                        @Override
                        public void run() {
                            reLoadFirstFolder(firstFolder);
                        }
                    });

                    //选择第一项
                    mNavAdapter.setSelectedItem(firstFolder.getSId());

                    AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                    refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                    refreshHelper.position = 0;
                    refreshNavUI(refreshHelper);

                    //更新子标题
                    updateSubTitle(getSubTitle(mSelectedFolderId));
                } else if (TextUtils.isEmpty(mSelectedFolderId)) {  //之前选中的是第一项“所有文件夹”，移除所删除文件夹中的笔记
                    List<DetailNoteInfo> deleteList = new ArrayList<>();
                    for (DetailNoteInfo detailNote : mNotes) {
                        NoteInfo note = detailNote.getNoteInfo();
                        if (deleteId.equals(note.getFolderId())) {
                            deleteList.add(detailNote);
                        }
                    }
                    if (deleteList.size() > 0) {
                        mNotes.removeAll(deleteList);
                        refreshUI(mNotes, null);
                    }
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
    private void refreshUI(List<DetailNoteInfo> list, AdapterRefreshHelper refreshHelper) {
        if (!SystemUtil.isEmpty(list)) {  //有数据
            setShowContentStyle(mIsGridStyle, false, refreshHelper);
            //显示recycleView
            SystemUtil.setViewVisibility(mRefresher, View.VISIBLE);
            clearEmptyView();
        } else {    //没有数据
            setShowContentStyle(mIsGridStyle, false, null);
            //隐藏recycleView
            SystemUtil.setViewVisibility(mRefresher, View.GONE);
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

    /**
     * 移动笔记到其他文件夹
     * @param detailNote
     */
    private void moveNote(final DetailNoteInfo detailNote) {
        List<DetailNoteInfo> list = new ArrayList<>(1);
        list.add(detailNote);

        moveNotes(list);
    }

    /**
     * 移动笔记到其他文件夹
     * @param noteList
     */
    private void moveNotes(List<DetailNoteInfo> noteList) {
        if (noteList == null || noteList.size() == 0) {
            return;
        }
        final List<Folder> list = FolderCache.getInstance().getSortFolders();
        String currentFolderId = null;
        if (noteList.size() == 1) { //只有一个笔记
            currentFolderId = noteList.get(0).getNoteInfo().getFolderId();
        } else {    //多个笔记
            currentFolderId = mSelectedFolderId;
        }
        if (list != null && list.size() > 0) {
            AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
            int checkedItem = -1;
            int size = list.size();
            Folder selectedFolder = null;
            String[] items = new String[size];
            for (int i = 0; i < size; i++) {
                Folder folder = list.get(i);
                items[i] = folder.getName();
                if (folder.getSId().equals(currentFolderId)) {
                    checkedItem = i;
                    selectedFolder = folder;
                }
            }
            final List<DetailNoteInfo> selects = new ArrayList<>(noteList);
            final int defaultItem = checkedItem;
            final Folder oldFolder = selectedFolder;
            final AlertDialog dialog = builder.setTitle(R.string.move_to)
                    .setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            if (defaultItem != which) { //有改变
                                doInbackground(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean success = false;
                                        if (selects.size() == 1) { //只有一条记录
                                            success = mNoteManager.move2Folder(selects, oldFolder, list.get(which));
                                        } else {    //多条记录
                                            Folder newFolder = list.get(which);
                                            List<DetailNoteInfo> actualList = new ArrayList<>();
                                            for (DetailNoteInfo detailNote : selects) {
                                                NoteInfo note = detailNote.getNoteInfo();
                                                if (!newFolder.getSId().equals(note.getFolderId())) {
                                                    actualList.add(detailNote);
                                                }
                                            }
                                            if (actualList.size() == 0) {
                                                success = true;
                                                mHandler.sendEmptyMessage(MSG_MOVE_SUCCESS);
                                                KLog.d(TAG, "--moveNotes--actualList---size---0---success--");
                                            } else {
                                                success = mNoteManager.move2Folder(actualList, oldFolder, newFolder);
                                            }
                                        }
                                        if (!success) {
                                            mHandler.sendEmptyMessage(MSG_MOVE_FAILED);
                                        }
                                    }
                                });
                                dialog.dismiss();
                            }
                        }
                    }).create();
            dialog.show();
        } else {
            SystemUtil.makeShortToast(R.string.folder_move_no_more);
        }
    }

    /**
     * 选择模式下每一项的选择事件
     * @param buttonView
     * @param isChecked
     */
    private void handleItemCheck(CompoundButton buttonView, boolean isChecked) {
        Integer pos = (Integer) buttonView.getTag();
        if (pos == null) {
            return;
        }
        if (isChecked) {
            addSelectedItem(pos);
        } else {
            removeSelectedItem(pos);
        }
    }
    
//    private void 
    
    /**
     * 初始化note的适配器
     * @author huanghui1
     * @update 2016/6/30 21:22
     * @version: 1.0.0
     */
    private void initNoteAdapter(boolean isGridStyle) {
        if (isGridStyle) {
            mNoteGridAdapter = new NoteGridAdapter(mContext, mNotes);
            mNoteGridAdapter.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleItemCheck(buttonView, isChecked);
                }
            });
            mNoteGridAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view) {
                    Integer pos = (Integer) view.getTag(R.integer.item_tag_data);

                    if (pos == null) {
                        return false;
                    }

                    NoteGridViewHolder holder = (NoteGridViewHolder) view.getTag();
                    //初始化actionMode
                    initActionMode();

                    addSelectedItem(pos);

                    if (holder.mCbCheck.getVisibility() != View.VISIBLE) {
                        AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                        refreshHelper.refresh(mNoteGridAdapter);
//                        mNoteGridAdapter.notifyDataSetChanged();
                    }
                    return true;
                }
            });
            mNoteGridAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view) {
                    if (mIsChooseMode) {    //选择模式
                        NoteGridViewHolder holder = (NoteGridViewHolder) view.getTag();
                        holder.mCbCheck.toggle();
                    } else {
                        Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                        if (pos == null) {
                            return;
                        }
                        showNoteInfo(mNotes.get(pos).getNoteInfo());
                    }
                }
            });
        } else {
            mNoteListAdapter = new NoteListAdapter(mContext, mNotes);
            mNoteListAdapter.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    handleItemCheck(buttonView, isChecked);
                }
            });
            mNoteListAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(View view) {

                    Integer pos = (Integer) view.getTag(R.integer.item_tag_data);

                    if (pos == null) {
                        return false;
                    }

                    NoteListViewHolder holder = (NoteListViewHolder) view.getTag();
                    //初始化actionMode
                    initActionMode();

                    addSelectedItem(pos);

                    if (holder.mCbCheck.getVisibility() != View.VISIBLE) {
                        mNoteListAdapter.notifyDataSetChanged();
                    }
                    return true;
                }
            });
            mNoteListAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(View view) {
                    if (mIsChooseMode) {    //选择模式
                        NoteListViewHolder holder = (NoteListViewHolder) view.getTag();
                        holder.mCbCheck.toggle();
                    } else {
                        Integer pos = (Integer) view.getTag(R.integer.item_tag_data);
                        if (pos == null) {
                            return;
                        }
                        showNoteInfo(mNotes.get(pos).getNoteInfo());
                    }
                }
            });
        }
    }

    /**
     * 添加选择项
     * @param position
     */
    private void addSelectedItem(int position) {
        if (mActionMode != null) {
            if (mSelectedList == null) {
                mSelectedList = new LinkedList<>();
            }
            DetailNoteInfo note = mNotes.get(position);
            if (mSelectedList.contains(note)) {
                return;
            }
            mSelectedList.add(note);
            mActionMode.setTitle(getSelectedTitle(mSelectedList.size(), mNotes.size()));
            
            if (mSelectedList.size() == 1) {
                updateActionModeMenuState(mActionMode, true, true);
            } else {
                updateActionModeMenuState(mActionMode, true, false);
            }
        }
    }

    /**
     * 移除选择项
     * @param position
     */
    private void removeSelectedItem(int position) {
        if (mActionMode != null) {
            if (mSelectedList == null) {
                mSelectedList = new LinkedList<>();
            }
            DetailNoteInfo note = mNotes.get(position);
            mSelectedList.remove(note);
            mActionMode.setTitle(getSelectedTitle(mSelectedList.size(), mNotes.size()));
            
            if (mSelectedList.isEmpty()) {  //没有选择项，则将菜单置为不可点
                updateActionModeMenuState(mActionMode, false, false);
            } else if (mSelectedList.size() == 1) {
                updateActionModeMenuState(mActionMode, true, true);
            } else {
                updateActionModeMenuState(mActionMode, true, false);
            }
        }
    }

    /**
     * 更新选择模式各菜单的状态
     * @param actionMode
     * @param enable
     * @param isSingle 是否只选择了一项
     */
    private void updateActionModeMenuState(ActionMode actionMode, boolean enable, boolean isSingle) {
        Menu menu = actionMode.getMenu();
        if (menu != null) {
            int size = menu.size();
            for (int i = 0; i < size; i++) {
                MenuItem menuItem = menu.getItem(i);
                if (enable) {
                    if (isSingle) { //只选择了单个
                        if (!menuItem.isEnabled()) {
                            menuItem.setEnabled(true);
                        }
                    } else {    //选择了多个
                        int menuId = menuItem.getItemId();
                        if (menuId == R.id.action_share || menuId == R.id.action_info) {
                            menuItem.setEnabled(false);
                        } else {
                            if (!menuItem.isEnabled()) {
                                menuItem.setEnabled(true);
                            }
                        }
                    }
                    
                } else {
                    if (menuItem.isEnabled()) {
                        menuItem.setEnabled(false);
                    }
                }
                
            }
        }
    }

    /**
     * 组装选择的标题
     * @param selectedSize
     * @param totalSize
     * @return
     */
    private String getSelectedTitle(int selectedSize, int totalSize) {
        return selectedSize + "/" + totalSize;
    }

    /**
     * 清除选择的项
     */
    private void clearSelectedItem() {
        mIsChooseMode = false;
        if (mSelectedList != null) {
            mSelectedList.clear();
        }
        if (!mFab.isShown()) {
            mFab.show();
        }
        //允许侧滑
        if (mNavDrawer != null) {
            mNavDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    /**
     * 初始化actionMode
     */
    private void initActionMode() {
        if (!mIsChooseMode) {
            mIsChooseMode = true;
            mActionMode = startSupportActionMode(new ActionModeCallbackImpl());
            
            if (mFab.isShown()) {
                mFab.hide();
            }
            
            //禁止侧滑
            if (mNavDrawer != null) {
                mNavDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        }
    }

    /**
     * 退出多选模式
     */
    private void outActionMode(boolean refresh) {
        if (!mIsChooseMode) {
            return;
        }

        //清除选择的项
        clearSelectedItem();
        
        //销毁actionMode
        finishActionMode(mActionMode);
        
        if (refresh) {
            //刷新界面
            refreshUI(mNotes, null);
        }
    }

    /**
     * 隐藏ActionMode
     * @param actionMode
     */
    private void finishActionMode(ActionMode actionMode) {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    /**
     * 根据位置获取对应的viewholder
     * @param position
     * @return
     */
    public RecyclerView.ViewHolder getViewHolder(int position) {
        return mRecyclerView.findViewHolderForAdapterPosition(position);
    }
    
    /**
     * 菜单的显示回调
     * @author huanghui1
     * @update 2016/7/1 14:55
     * @version: 1.0.0
     */
    class ActionModeCallbackImpl implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode mode, final Menu menu) {
            MenuInflater menuInflater = getMenuInflater();
            menuInflater.inflate(R.menu.grid_item_opt, menu);
            
            if (!FolderCache.getInstance().hasMoreFolder()) {   //没有更多的文件夹，除了“所有文件夹”
                //移除“移动”菜单
                menu.removeItem(R.id.action_move);
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_move:  //移动
                    moveNotes(mSelectedList);
                    break;
                case R.id.action_delete:    //删除
                    NoteUtil.handleDeleteNote(mContext, mSelectedList, mHasDeleteOpt);
                    break;
                case R.id.action_share:    //分享
                    break;
                case R.id.action_info:    //详情
                    NoteUtil.showInfo(mContext, mSelectedList.get(0).getNoteInfo());
                    break;
            }
            outActionMode(true);
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            outActionMode(true);
        }
    }
    
    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.nav_archive:  //文件夹
                intent = new Intent(mContext, FolderListActivity.class);
                startActivity(intent);
                //退出选择模式
                outActionMode(true);
                break;
        }
    }

    /**
     * 切换显示模式
     */
    private void changeShowMode(final MenuItem item) {
        mHandler.removeMessages(MSG_PALETTE_COLOR);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                outActionMode(false);
                mIsGridStyle = !mIsGridStyle;
                doInbackground(new Runnable() {
                    @Override
                    public void run() {
                        updateShowStyle(mIsGridStyle);
                    }
                });
                RecyclerView.LayoutManager layoutManager = mLayoutManagerFactory.getLayoutManager(mContext, mIsGridStyle);
                mRecyclerView.setLayoutManager(layoutManager);
                if (mIsGridStyle) { //显示成网格样式
                                /*if (mItemDecoration != null) {
                                    mRecyclerView.removeItemDecoration(mItemDecoration);
                                }*/
                    if (mNoteGridAdapter == null) {
                        initNoteAdapter(true);
                    }
                    int padding = mContext.getResources().getDimensionPixelSize(R.dimen.grid_item_padding);
                    mRecyclerView.setPadding(padding, 0, padding, 0);
                    mRecyclerView.setAdapter(mNoteGridAdapter);
                    item.setTitle(R.string.action_show_list);
                    item.setIcon(R.drawable.ic_action_view_list);
                } else {    //列表样式
                    mRecyclerView.setPadding(0, 0, 0, 0);
//                                mRecyclerView.addItemDecoration(getItemDecoration(mContext));
                    if (mNoteListAdapter == null) {
                        initNoteAdapter(false);
                    }
                    mRecyclerView.setAdapter(mNoteListAdapter);
                    item.setTitle(R.string.action_show_grid);
                    item.setIcon(R.drawable.ic_action_grid);
                }

            }
        });
    }

    /**
     * 是否是修改时间排序
     * @return
     */
    private boolean isModifyTimeSort() {
        return mNoteSort != NoteInfo.SORT_CREATE_TIME;
    }

    /**
     * 显示排序方式的对话框
     * @param currentSort 当前的排序，默认勾选项
     */
    private void showSortStyle(final int currentSort) {
        AlertDialog.Builder builder = NoteUtil.buildDialog(mContext);
        builder.setTitle(R.string.sort_style)
                .setSingleChoiceItems(R.array.sort_menu_items, currentSort, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        dialog.dismiss();
                        if (which != currentSort) {
                            mNoteSort = which;
                            //下拉刷新
                            pullRefresh();

                            //修改笔记的排序方式
                            doInbackground(new NoteTask() {
                                @Override
                                public void run() {
                                    updateNoteSort(which);
                                }
                            });
                        }
                    }
                })
                .show();
    }

    /**
     * popuMenu每一项点击的事件
     * @author huanghui1
     * @update 2016/3/2 15:05
     * @version: 1.0.0
     */
    class OnPopuMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_show_style:   //列表显示方式，有列表方式个网格方式
                    changeShowMode(item);

                    break;
                case R.id.nav_upload:   //同步
                    break;
                case R.id.nav_sort: //排序
                    showSortStyle(mNoteSort);
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
        
        private String mSelectedItem;

        public NavViewAdapter(Context context, List<Folder> items) {
            mList = items;
            mContext = context;
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        public String getSelectedItem() {
            return mSelectedItem;
        }

        public void setSelectedItem(String selectedItem) {
            this.mSelectedItem = selectedItem;
        }

        public void setItemClickListener(OnItemClickListener itemClickListener) {
            this.mItemClickListener = itemClickListener;
        }

        @Override
        public NavTextViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.nav_list_item, parent, false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        String folderId = (String) v.getTag(R.integer.item_tag_data);
                        Folder pFolder = new Folder();
                        pFolder.setSId(mSelectedItem);
                        int preSelectIndex = mFolders.indexOf(pFolder);
                        mSelectedItem = folderId;
                        if (preSelectIndex != -1) {
                            notifyItemChanged(preSelectIndex);
                        }
                        mItemClickListener.onItemClick(v);
                    }
                }
            });
            return new NavTextViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final NavTextViewHolder holder, final int position) {
            Folder folder = mList.get(position);
            final String folderId = folder.getSId();
            holder.itemView.setTag(holder.getAdapterPosition());
            holder.itemView.setTag(R.integer.item_tag_data, folderId);
            boolean selected = false;
            if (mSelectedItem != null && mSelectedItem.equals(folderId)) {
                selected = true;
            } else if (TextUtils.isEmpty(mSelectedItem) && TextUtils.isEmpty(folderId)) {
                selected = true;
            }
            holder.itemView.setTag(holder.getAdapterPosition());
            holder.itemView.setSelected(selected);
            
            holder.mTextView.setText(folder.getName());
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }
    }
    
    class NoteListViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvIcon;
        TextView mTvContent;
        TextView mTvTime;
        CheckBox mCbCheck;

        public NoteListViewHolder(View itemView) {
            super(itemView);

            mIvIcon = (ImageView) itemView.findViewById(R.id.iv_icon);
            mTvContent = (TextView) itemView.findViewById(R.id.tv_content);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
            mCbCheck = (CheckBox) itemView.findViewById(R.id.cb_check);
        }
    }
    
    class NoteListAdapter extends RecyclerView.Adapter<NoteListViewHolder> {

        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<DetailNoteInfo> mList;

        private OnItemLongClickListener mOnItemLongClickListener;
        
        private OnItemClickListener mOnItemClickListener;
        
        private OnCheckedChangeListener mOnCheckedChangeListener;
        
        //图片加载失败的图片
        private ItemAttachIcon mItemAttachIcon;
        //着色的颜色
        private int mTintColor;

        public NoteListAdapter(Context context, List<DetailNoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);
            
            mTintColor = initTintColor();

            mItemAttachIcon = new ItemAttachIcon();
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.mOnItemLongClickListener = onItemLongClickListener;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = onCheckedChangeListener;
        }

        @Override
        public NoteListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_main_list, parent, false);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemLongClickListener != null) {
                        return mOnItemLongClickListener.onItemLongClick(v);
                    } else {
                        return false;
                    }
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            NoteListViewHolder holder = new NoteListViewHolder(view);
            view.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(final NoteListViewHolder holder, int position) {
            DetailNoteInfo detailNote = mList.get(position);
            NoteInfo note = detailNote.getNoteInfo();
            holder.itemView.setTag(R.integer.item_tag_data, holder.getAdapterPosition());
            holder.mCbCheck.setTag(holder.getAdapterPosition());
            if (note != null) {
                holder.mCbCheck.setOnCheckedChangeListener(null);
                if (mIsChooseMode) {    //选择模式
                    boolean checked = mSelectedList != null && mSelectedList.size() > 0 &&  mSelectedList.contains(detailNote);
                    showCheckbox(holder.mCbCheck);
                    holder.mCbCheck.setChecked(checked);
                } else {
                    hideCheckbox(holder.mCbCheck);
                    if (holder.mCbCheck.isChecked()) {
                        holder.mCbCheck.setChecked(false);
                    }
                }

                //重置附件图标的各种状态
                resetAttachView(holder);

                holder.mTvContent.setText(note.getStyleContent(true, detailNote.getDetailList()));
                holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(isModifyTimeSort())));

                holder.mCbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mOnCheckedChangeListener != null) {
                            mOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                        }
                    }
                });
                
                if (note.hasAttach() && detailNote.getLastAttach() != null) {
                    SystemUtil.setViewVisibility(holder.mIvIcon, View.VISIBLE);
                    showAttachIcon(detailNote, holder);
                }
                
            }
        }

        /**
         * 重置附件图标的各种状态
         * @param holder
         */
        private void resetAttachView(RecyclerView.ViewHolder holder) {
            NoteListViewHolder listHolder = (NoteListViewHolder) holder;
            listHolder.mIvIcon.setImageResource(0);
            SystemUtil.setViewVisibility(listHolder.mIvIcon, View.GONE);
        }

        /**
         * 初始化着色的颜色
         * @return
         */
        private int initTintColor() {
            Resources resources = getResources();
            return ResourcesCompat.getColor(resources, R.color.text_time_color, getTheme());
        }

        /**
         * 初始化图片加载时候的图片
         * @return
         */
        private Drawable initFailedImage(int color) {
            Resources resources = getResources();
            Drawable drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_broken_image, getTheme());
            drawable = getTintDrawable(drawable, color);
            return drawable;
        }

        /**
         * 初始化图片加载时候的图片
         * @return
         */
        private Drawable initAttachIcon(int attachType, int color) {
            Drawable drawable = null;
            int resId = 0;
            Resources resources = getResources();
            switch (attachType) {
                case Attach.VOICE:
                    if (mItemAttachIcon.mMusicDrawable == null) {
                        resId = R.drawable.ic_library_music;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mMusicDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mMusicDrawable;
                    break;
                case Attach.VIDEO:
                    if (mItemAttachIcon.mVideoDrawable == null) {
                        resId = R.drawable.ic_library_music;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mVideoDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mVideoDrawable;
                    break;
                case Attach.ARCHIVE:
                    if (mItemAttachIcon.mArchiveDrawable == null) {
                        resId = R.drawable.ic_library_archive;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mArchiveDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mArchiveDrawable;
                    break;
                case Attach.FILE:
                    if (mItemAttachIcon.mFileDrawable == null) {
                        resId = R.drawable.ic_library_file;
                        drawable = ResourcesCompat.getDrawable(resources, resId, getTheme());
                        mItemAttachIcon.mFileDrawable = getTintDrawable(drawable, color);
                    }
                    drawable = mItemAttachIcon.mFileDrawable;
                    break;
            }
            return drawable;
        }

        /**
         * 显示附件的图标
         * @param detailNote
         * @param holder
         */
        private void showAttachIcon(DetailNoteInfo detailNote, NoteListViewHolder holder) {
            Attach lastAttach = detailNote.getLastAttach();
            if (lastAttach.isImage()) { //图片文件
                ImageUtil.displayImage(lastAttach.getLocalPath(), new ImageViewAware(holder.mIvIcon, false), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (loadedImage == null) {
                            loadImageFailed((ImageView) view);
                        }
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        loadImageFailed((ImageView) view);
                    }
                });
            } else {
                Drawable drawable = initAttachIcon(lastAttach.getType(), mTintColor);
                holder.mIvIcon.setImageDrawable(drawable);
            }
        }

        /**
         * 图片加载失败
         * @param imageView
         */
        private void loadImageFailed(ImageView imageView) {
            if (mItemAttachIcon.mFailedDrawable == null) {
                mItemAttachIcon.mFailedDrawable = initFailedImage(mTintColor);
            }
            imageView.setImageDrawable(mItemAttachIcon.mFailedDrawable);
        }

        /**
         * 显示复选框
         * @param checkBox
         */
        public void showCheckbox(CheckBox checkBox) {
            if (checkBox.getVisibility() != View.VISIBLE) {
                checkBox.setVisibility(View.VISIBLE);
            }
        }

        /**
         * 隐藏复选框
         * @param checkBox
         */
        public void hideCheckbox(CheckBox checkBox) {
            if (checkBox.getVisibility() == View.VISIBLE) {
                checkBox.setVisibility(View.GONE);
            }
        }
        
        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 附件的类型的图标
         */
        class ItemAttachIcon {
            //图片加载失败的图片
            Drawable mFailedDrawable;
            Drawable mMusicDrawable;
            //压缩文件
            Drawable mArchiveDrawable;
            //视频文件
            Drawable mVideoDrawable;
            Drawable mFileDrawable;

        }
    }

    class NoteGridViewHolder extends RecyclerView.ViewHolder {
        ImageView mIvOverflow;
        TextView mTvTitle;
        TextView mTvSummary;
        TextView mTvTime;
        CheckBox mCbCheck;
        View mItemContainer;

        public NoteGridViewHolder(final View itemView) {
            super(itemView);

            mIvOverflow = (ImageView) itemView.findViewById(R.id.iv_overflow);
            mTvTitle = (TextView) itemView.findViewById(R.id.tv_title);
            mTvSummary = (TextView) itemView.findViewById(R.id.tv_summary);
            mTvTime = (TextView) itemView.findViewById(R.id.tv_time);
            mCbCheck = (CheckBox) itemView.findViewById(R.id.cb_check);
            mItemContainer = itemView.findViewById(R.id.item_container);
        }
    }

    class NoteGridAdapter extends RecyclerView.Adapter<NoteGridViewHolder> {
        private final LayoutInflater mLayoutInflater;
        private final Context mContext;
        private List<DetailNoteInfo> mList;

        private OnItemLongClickListener mOnItemLongClickListener;
        
        private OnItemClickListener mOnItemClickListener;

        private OnCheckedChangeListener mOnCheckedChangeListener;
        
        private ItemColor mItemColor;

        public NoteGridAdapter(Context context, List<DetailNoteInfo> list) {
            this.mContext = context;
            this.mList = list;
            mLayoutInflater = LayoutInflater.from(context);
            
            mItemColor = initItemColor();
        }

        public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
            this.mOnItemLongClickListener = onItemLongClickListener;
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mOnItemClickListener = onItemClickListener;
        }

        public void setOnCheckedChangeListener(OnCheckedChangeListener onCheckedChangeListener) {
            this.mOnCheckedChangeListener = onCheckedChangeListener;
        }

        @Override
        public NoteGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mLayoutInflater.inflate(R.layout.item_main_grid, parent, false);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mOnItemLongClickListener != null) {
                        return mOnItemLongClickListener.onItemLongClick(v);
                    }
                    return false;
                }
            });

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemClick(v);
                    }
                }
            });
            NoteGridViewHolder holder = new NoteGridViewHolder(view);
            view.setTag(holder);
            return holder;
        }

        @Override
        public void onBindViewHolder(final NoteGridViewHolder holder, int position) {
            DetailNoteInfo detailNote = mList.get(position);
            
            if (detailNote == null) {
                return;
            }
            
            holder.itemView.setTag(R.integer.item_tag_data, holder.getAdapterPosition());
            holder.mCbCheck.setTag(holder.getAdapterPosition());

            //重置文字的颜色
            resetTextColor(holder, mItemColor);
                
            if (mIsChooseMode) {
                SystemUtil.hideView(holder.mIvOverflow);
                SystemUtil.showView(holder.mCbCheck);

                holder.mCbCheck.setOnCheckedChangeListener(null);

                boolean checked = mSelectedList != null && mSelectedList.size() > 0 &&  mSelectedList.contains(detailNote);
                holder.mCbCheck.setChecked(checked);

                holder.mCbCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (mOnCheckedChangeListener != null) {
                            mOnCheckedChangeListener.onCheckedChanged(buttonView, isChecked);
                        }
                    }
                });
            } else {
                SystemUtil.showView(holder.mIvOverflow);
                SystemUtil.hideView(holder.mCbCheck);
                holder.mIvOverflow.setOnClickListener(new GridItemClickListener(detailNote));
            }
            
            NoteInfo note = detailNote.getNoteInfo();
            
            CharSequence title = note.getNoteTitle(false);
            if (note.isDetailNote() && TextUtils.isEmpty(title)) {
                title = getResources().getString(R.string.no_title);
            }
            
            holder.mTvTitle.setText(title);
            holder.mTvTime.setText(TimeUtil.formatNoteTime(note.getShowTime(isModifyTimeSort())));
            holder.mTvSummary.setText(note.getStyleContent(detailNote.getDetailList()));

            if (note.hasAttach() && detailNote.getLastAttach() != null) {
                Attach attach = detailNote.getLastAttach();
                ImageUtil.displayImage(attach.getLocalPath(), new NoteItemViewAware(holder.mItemContainer), new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        if (loadedImage != null) {
                            doInbackground(new PaletteItemColorTask(holder.getAdapterPosition(), loadedImage));
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        /**
         * 初始化各项的文字颜色
         * @return
         */
        public ItemColor initItemColor() {
            Resources resources = getResources();
            int titleColor = ResourcesCompat.getColor(resources, R.color.text_title_color, getTheme());
            int contentColor = ResourcesCompat.getColor(resources, R.color.text_content_color, getTheme());
            int timeColor = ResourcesCompat.getColor(resources, R.color.text_time_color, getTheme());

            ItemColor itemColor = new ItemColor();
            itemColor.titleColor = titleColor;
            itemColor.contentColor = contentColor;
            itemColor.timeColor = timeColor;
            
            return itemColor;
        }

        /**
         * 重置文字的颜色
         * @param holder
         * @param itemColor 每项的颜色
         */
        public void resetTextColor(RecyclerView.ViewHolder holder, ItemColor itemColor) {
            NoteGridViewHolder gridHolder = (NoteGridViewHolder) holder;
            gridHolder.mTvTitle.setTextColor(itemColor.titleColor);
            gridHolder.mTvSummary.setTextColor(itemColor.contentColor);
            gridHolder.mTvTime.setTextColor(itemColor.timeColor);

            gridHolder.mIvOverflow.setImageResource(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                gridHolder.mItemContainer.setBackground(null);
            } else {
                gridHolder.mItemContainer.setBackgroundDrawable(null);
            }
        }
        
        class ItemColor {
            int titleColor;
            int contentColor;
            int timeColor;
        }
        
        /**
         * 给每一项文本着色的任务
         * @author huanghui1
         * @update 2016/8/15 16:33
         * @version: 1.0.0
         */
        class PaletteItemColorTask implements Runnable {
            private int position;
            
            private Bitmap bitmap;

            public PaletteItemColorTask(int position, Bitmap bitmap) {
                this.position = position;
                this.bitmap = bitmap;
            }

            @Override
            public void run() {
                if (bitmap == null || bitmap.isRecycled() || !mIsGridStyle) {
                    return;
                }
                
                Palette.Builder builder = new Palette.Builder(bitmap);
                Palette palette = builder.generate();
                List<Palette.Swatch> swatches = palette.getSwatches();
                if (swatches.size() == 0) {
                    return;
                }
                Palette.Swatch swatch = swatches.get(swatches.size() - 1);
                if (swatch == null) {
                    return;
                }
                int titleColor = swatch.getTitleTextColor();
                int bodyColor = swatch.getBodyTextColor();
                Message msg = mHandler.obtainMessage();
                msg.arg1 = titleColor;
                msg.arg2 = bodyColor;
                msg.obj = position;
                msg.what = MSG_PALETTE_COLOR;
                mHandler.sendMessage(msg);
            }
        }

        /**
         * 网格的每一项点击事件
         * @author huanghui1
         * @update 2016/6/21 11:40
         * @version: 1.0.0
         */
        class GridItemClickListener implements View.OnClickListener {
            private DetailNoteInfo detailNote;

            public GridItemClickListener(DetailNoteInfo detailNote) {
                this.detailNote = detailNote;
            }

            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.iv_overflow:
                        PopupMenu itemMenu = createPopMenu(v, R.menu.grid_item_opt, false, new ItemMenuClickListener(detailNote));
                        boolean hasMoreFolder = FolderCache.getInstance().hasMoreFolder();
                        if (!hasMoreFolder) {   //删除“移动”菜单项
                            Menu menu = itemMenu.getMenu();
                            if (menu != null) {
                                menu.removeItem(R.id.action_move);
                            }
                        }
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
            private DetailNoteInfo detailNote;

            public ItemMenuClickListener(DetailNoteInfo detailNote) {
                this.detailNote = detailNote;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_delete:    //删除
                        handleDeleteNote(detailNote);
                        break;
                    case R.id.action_info:  //详情
                        NoteUtil.showInfo(mContext, detailNote.getNoteInfo());
                        break;
                    case R.id.action_move:  //移动
                        moveNote(detailNote);
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
                        String currentItem = target.mNavAdapter.getSelectedItem();
                        if (!TextUtils.isEmpty(currentItem) && 
                                !currentItem.equals(target.mSelectedFolderId)) {  //更新默认选中的项
                            target.mNavAdapter.setSelectedItem(target.mSelectedFolderId);
                        } else if (TextUtils.isEmpty(currentItem) && !TextUtils.isEmpty(target.mSelectedFolderId)) {
                            target.mNavAdapter.setSelectedItem(target.mSelectedFolderId);
                        }
                        target.mNavAdapter.notifyDataSetChanged();

                        String subTitle = target.getSubTitle(target.mSelectedFolderId);

                        //更新子标题
                        target.updateSubTitle(subTitle);
                        
                        break;
                    case Constants.MSG_SUCCESS2:    //笔记内容加载成功
                        target.mRefresher.setRefreshing(false);
                        List<DetailNoteInfo> list = (List<DetailNoteInfo>) msg.obj;
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
                    case MSG_MOVE_FAILED:   //移动文件夹失败
                        SystemUtil.makeShortToast(R.string.move_result_error);
                        break;
                    case MSG_MOVE_SUCCESS:   //移动文件夹成功
                        SystemUtil.makeShortToast(R.string.result_success);
                        break;
                    case MSG_PALETTE_COLOR: //给笔记的item着色，针对有背景图片的
                        int titleColor = msg.arg1;
                        int bodyColor = msg.arg2;
                        Integer position = (Integer) msg.obj;
                        if (position < 0) {
                            return;
                        }
                        RecyclerView.ViewHolder holder = target.getViewHolder(position);
                        if (holder != null && holder instanceof NoteGridViewHolder) {
                            NoteGridViewHolder gridHolder = (NoteGridViewHolder) holder;
                            
                            Integer tagPos = (Integer) gridHolder.itemView.getTag(R.integer.item_tag_data);
                            if (tagPos != null && tagPos.intValue() == position.intValue()) { //同一项，防止错位
                                gridHolder.mTvTitle.setTextColor(titleColor);
                                gridHolder.mTvSummary.setTextColor(bodyColor);
                                gridHolder.mTvTime.setTextColor(bodyColor);

                                Drawable drawable = gridHolder.mIvOverflow.getDrawable();
                                drawable = target.getTintDrawable(drawable, titleColor);
                                gridHolder.mIvOverflow.setImageDrawable(drawable);
                            }
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
                    DetailNoteInfo detailNote = null;
                    if (data != null) {
                        if (data instanceof NoteInfo) {
                            NoteInfo noteInfo = (NoteInfo) data;
                            detailNote = new DetailNoteInfo();
                            detailNote.setNoteInfo(noteInfo);
                        } else if (data instanceof DetailNoteInfo) {
                            detailNote = ((DetailNoteInfo) data);
                        }
                    }
                    switch (notifyType) {
                        case ADD:   //添加
                            KLog.d(TAG, "------addNote----" + detailNote);
                            if (detailNote != null) {
                                addNote(detailNote);
                            }
                            break;
                        case UPDATE:    //修改笔记
                            KLog.d(TAG, "------updateNote----" + detailNote);
                            if (detailNote != null) {
                                updateNote(detailNote);
                            }
                            break;
                        case DELETE:    //删除、移到回收站
                            KLog.d(TAG, "------deleteNote----" + detailNote);
                            if (detailNote != null) {
                                deleteNote(detailNote, false);
                            } else if (data instanceof List) {  //删除了多个笔记
                                List<DetailNoteInfo> detailNoteList = (List<DetailNoteInfo>) data;
                                deleteNotes(detailNoteList, false);
                            }
                            saveDeleteOpt();
                            break;
                        case MOVE:    //移动到其他文件夹
                            KLog.d(TAG, "------moveNote----" + detailNote);
                            boolean isFolderAll = TextUtils.isEmpty(mSelectedFolderId);
                            if (detailNote != null) {
                                deleteNote(detailNote, isFolderAll);
                            } else if (data instanceof List) {  //移动了多个笔记
                                List<DetailNoteInfo> detailNoteList = (List<DetailNoteInfo>) data;
                                deleteNotes(detailNoteList, isFolderAll);
                            }
                            if (isFolderAll) {  //在所有文件中，则给个操作结果的提示
                                mHandler.sendEmptyMessage(MSG_MOVE_SUCCESS);
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
                            KLog.d(TAG, "------addFolder----" + folder);
                            addFolder(folder);
                            break;
                        case UPDATE:   //更新文件夹
                            if (data instanceof Folder) {
                                folder = (Folder) data;
                                KLog.d(TAG, "------updateFolder----" + folder);
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
                            KLog.d(TAG, "------deleteFolder----" + folder);
                            deleteFolder(folder);
                            break;
                    }
                    break;
            }
        }
    }
}
