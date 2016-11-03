package com.yunxinlink.notes.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.cache.FolderCache;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.helper.AdapterRefreshHelper;
import com.yunxinlink.notes.listener.OnItemClickListener;
import com.yunxinlink.notes.model.DetailNoteInfo;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.NoteInfo;
import com.yunxinlink.notes.persistent.FolderManager;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.settings.SettingsActivity;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SettingsUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.sharesdk.framework.ShareSDK;

/**
 * 主界面
 * @author huanghui1
 * @update 2016/2/24 19:25
 * @version: 1.0.0
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, MainFragment.OnMainFragmentInteractionListener {
    
    public static final String ACTION_MAIN = "com.yunxinlink.notes.MAIN_ACTION";
    
    public static final String ARG_EXIT = "arg_exit";
    
    private static final int MSG_SELECT_NAV = 3;

    private static final int MSG_MOVE_FAILED = 4;
    private static final int MSG_MOVE_SUCCESS = 5;

    private NavViewAdapter mNavAdapter;

    /**
     * 默认选中的文件夹id
     */
    private String mSelectedFolderId;
    private NoteContentObserver mNoteObserver;
    
    //是否有删除笔记的操作，第一次操作则有删除提示，后面则没有了
    private boolean mHasDeleteOpt;
    
    private View mNavArchiveView;
    private View mNavTrashView;
    private View mNavSettingsView;

    private DrawerLayout mNavDrawer;
    
    private List<Folder> mFolders = new ArrayList<>();
    
    private final Handler mHandler = new MyHandler(this);

    /**
     * 新建按钮
     */
    private FloatingActionButton mFab;
    
    //内容区域的根布局
    private View mContentMain;
    
    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        //初始化主界面右下角编辑按钮
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(this);
        }

        mContentMain = findViewById(R.id.content_main);

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

        //初始化左侧导航菜单
        RecyclerView navigationView = (RecyclerView) findViewById(R.id.nav_view);

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

        //注册观察者
        registerContentObserver();
    }

    @Override
    protected boolean isInterrupt() {
        if (exit()) {
            return true;
        } else {
            return super.isInterrupt();
        }
    }

    /**
     * 填充主界面
     * @param folderId 笔记本的id
     */
    private void attachMainFrame(String folderId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = MainFragment.newInstance(folderId);
        transaction.replace(R.id.content_container, fragment, fragment.getClass().getSimpleName()).commit();
    }

    /**
     * 刷新数据
     */
    private void doOnRefresh() {
        //初始化文件夹
        initFolder();

        //初始化笔记的一些设置项
//        initNoteSettings();

        doInbackground(new Runnable() {
            @Override
            public void run() {
                if (isFolderAllDisable()) { //不能加载所有文件夹里的笔记，则加载第一个文件夹
                    //加载文件夹
                    List<Folder> folders = loadFolder(false);

                    if (folders != null && folders.size() > 0) {
                        reLoadFirstFolder(folders.get(0));
                    }

                } else {
                    //加载文件夹
                    loadFolder(isShowFolderAll());
                }
            }
        });
        
    }

    @Override
    public boolean isSwipeBackEnabled() {
        return false;
    }

    @Override
    protected void initData() {

        //初始化分享sdk
        ShareSDK.initSDK(this);
        
        //初始化配置文件
        initProperties();

        //加载笔记本等数据
        doOnRefresh();

        attachMainFrame(mSelectedFolderId);

        requestPermission();

    }
    
    @Override
    protected boolean reLock() {
        if (SettingsUtil.hasLock(this)) {
            return true;
        }
        return false;
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
        
        String folderId = folder.getSid();
        //笔记本是否相同
        boolean isSame = false;
        if (mSelectedFolderId != null) {
            isSame = mSelectedFolderId.equals(folderId);
        } else if (folderId == null) {
            isSame = true;
        }
        
        if (isSame) {
            KLog.d(TAG, "--reLoadFirstFolder----folder---sid--is---same--");
            return;
        }
        mSelectedFolderId = folder.getSid();
        SystemUtil.setSelectedFolder(mContext, mSelectedFolderId);
        MainFragment mainFragment = getMainFragment();
        if (mainFragment != null) {
            mainFragment.selectFolder(mSelectedFolderId);
            //加载笔记
            mainFragment.loadNotes(mSelectedFolderId);
        } else {
            KLog.d(TAG, "---reLoadFirstFolder--mainFragment--is---null--");
        }
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
     * 初始化文件夹
     * @author huanghui1
     * @update 2016/3/8 17:38
     * @version: 1.0.0
     */
    private void initFolder() {
        mSelectedFolderId = SystemUtil.getSelectedFolder(mContext);
        
        Folder folder = new Folder();
        folder.setSid(mSelectedFolderId);
        
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
        List<Folder> list = FolderManager.getInstance().getAllFolders(getCurrentUser(true), null);
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
     * 注册观察者的监听
     * @author huanghui1
     * @update 2016/3/9 18:10
     * @version: 1.0.0
     */
    private void registerContentObserver() {
        mNoteObserver = new NoteContentObserver(mHandler);
        NoteManager.getInstance().addObserver(mNoteObserver);
        FolderManager.getInstance().addObserver(mNoteObserver);
        UserManager.getInstance().addObserver(mNoteObserver);
    }
    
    /**
     * 注销观察者
     * @author huanghui1
     * @update 2016/3/9 18:11
     * @version: 1.0.0
     */
    private void unRegisterContentObserver() {
        if (mNoteObserver != null) {
            NoteManager.getInstance().removeObserver(mNoteObserver);
            FolderManager.getInstance().removeObserver(mNoteObserver);
            UserManager.getInstance().removeObserver(mNoteObserver);
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
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU: //点击了菜单
                MainFragment mainFragment = getMainFragment();
                if (mainFragment == null) {
                    KLog.d(TAG, "----onKeyUp--menu---mainFragment--is--null----");
                    return false;
                }
                return mainFragment.onMenuKeyUp();
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        //注销分享的sdk
        ShareSDK.stopSDK(this);
        //注销观察者
        unRegisterContentObserver();
        super.onDestroy();
    }

    /**
     * 退出
     * @return
     */
    private boolean exit() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean exit = intent.getBooleanExtra(ARG_EXIT, false);
            if (exit) {
                KLog.d(TAG, "main activity will exit");
                finish();
                return true;
            }
        }
        return false;
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
//                mSelectedFolderId = firstFolder.getSid();
                mNavAdapter.setSelectedItem(firstFolder.getSid());
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
            boolean result = updateRefresh(folder);
            if (!result && folder.isNormal()) { //且是普通的笔记本
                addFolder(folder);
            }
        }
        
        //更新子标题
        updateSubTitle(getSubTitle(mSelectedFolderId));
    }

    /**
     * 刷新一条记录
     * @param folder
     * @return 在列表中有没有找到要刷新的项
     */
    private boolean updateRefresh(Folder folder) {
        int index = mFolders.indexOf(folder);

        if (index != -1) {  //有找到
            Folder tFolder = mFolders.get(index);
            setUpNewFolder(tFolder, folder);

            AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
            refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
            refreshHelper.position = index;

            refreshNavUI(refreshHelper);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 批量添加或者更新笔记本
     * @param folderList
     */
    private void batchUpdate(List<Folder> folderList) {
        if (SystemUtil.isEmpty(folderList)) {
            KLog.d(TAG, "main activity batch update but list is empty");
            return;
        }
        for (Folder folder : folderList) {
            boolean result = updateRefresh(folder);
            if (!result && folder.isNormal()) { //且是普通的笔记本
                addFolder(folder);
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
        oldFolder.setLock(newFolder.isLock());
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
            
            String deleteId = folder.getSid();

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
                    mSelectedFolderId = folder.getSid();
                    doInbackground(new Runnable() {
                        @Override
                        public void run() {
                            reLoadFirstFolder(firstFolder);
                        }
                    });

                    //选择第一项
                    mNavAdapter.setSelectedItem(firstFolder.getSid());

                    AdapterRefreshHelper refreshHelper = new AdapterRefreshHelper();
                    refreshHelper.type = AdapterRefreshHelper.TYPE_UPDATE;
                    refreshHelper.position = 0;
                    refreshNavUI(refreshHelper);

                    //更新子标题
                    updateSubTitle(getSubTitle(mSelectedFolderId));
                } else if (TextUtils.isEmpty(mSelectedFolderId)) {  //之前选中的是第一项“所有文件夹”，移除所删除文件夹中的笔记
                    MainFragment mainFragment = getMainFragment();
                    if (mainFragment != null) {
                        mainFragment.removeNotes(deleteId);
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
     * 事件发起方是fragment，初始化actionMode
     */
    private void onInitActionMode() {
        
        if (mFab.isShown()) {
            mFab.hide();
        }
        
        //禁止侧滑
        if (mNavDrawer != null) {
            mNavDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }
    
    /**
     * 事件发起方是fragment，退出多选模式
     */
    private void onOutActionMode() {
        if (!mFab.isShown()) {
            mFab.show();
        }
        //允许侧滑
        if (mNavDrawer != null) {
            mNavDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    /**
     * 退出多选模式,事件发起方是activity
     * @param refresh 是否需要刷新界面
     */
    private void outActionMode(boolean refresh) {
        MainFragment mainFragment = getMainFragment();
        if (mainFragment != null) {
            mainFragment.outActionMode(refresh);
        }
    }
    
    @Override
    public void setActionMode(boolean isOn) {
        if (isOn) {
            onInitActionMode();
        } else {
            onOutActionMode();
        }
    }

    @Override
    public View getContentMainView() {
        return mContentMain;
    }

    /**
     * 获取该activity的fragment
     * @return
     */
    public MainFragment getMainFragment() {
        return (MainFragment) getSupportFragmentManager().findFragmentByTag(MainFragment.class.getSimpleName());
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
            case R.id.fab:  //新建按钮
                /*Platform weibo = ShareSDK.getPlatform(SinaWeibo.NAME);
                    weibo.setPlatformActionListener(new SimplePlatformActionListener());

                    weibo.removeAccount();*/
                    
                    /*boolean isAuthValid = weibo.isAuthValid();
                    if (isAuthValid) {//移除授权
                        weibo.removeAccount(true);
                    } else {
                        weibo.authorize();
                    }*/

                intent = new Intent(mContext, NoteEditActivity.class);
//                intent = new Intent(mContext, TestActivity.class);
//                intent = new Intent(mContext, TestNetActivity.class);
//                intent = new Intent(mContext, AuthorityActivity.class);
//                intent = new Intent(mContext, LockDigitalActivity.class);
//                intent = new Intent(mContext, ShortCreateAppWidgetConfigure.class);
                intent.putExtra(NoteEditActivity.ARG_FOLDER_ID, mSelectedFolderId);
                intent.putExtra(NoteEditActivity.ARG_OPT_DELETE, mHasDeleteOpt);
                startActivity(intent);

                //退出选择模式
                outActionMode(true);

                /*Snackbar.make(mContentMain, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                break;
            case R.id.nav_trash:    //加载垃圾桶的笔记
                intent = new Intent(mContext, TrashActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_settings: //设置
                intent = new Intent(mContext, SettingsActivity.class);
                startActivity(intent);
                break;
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
                        pFolder.setSid(mSelectedItem);
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
            final String folderId = folder.getSid();
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
            MainFragment mainFragment = null;
            switch (notifyFlag) {
                case Provider.NoteColumns.NOTIFY_FLAG:  //笔记的通知
                    DetailNoteInfo detailNote = null;
                    mainFragment = getMainFragment();
                    if (mainFragment == null) {
                        KLog.d(TAG, "--update--observer--mainFragment-is---null--");
                        return;
                    }
                    List<DetailNoteInfo> detailNoteList = null;
                    if (data != null) {
                        if (data instanceof NoteInfo) {
                            NoteInfo noteInfo = (NoteInfo) data;
                            detailNote = new DetailNoteInfo();
                            detailNote.setNoteInfo(noteInfo);
                        } else if (data instanceof DetailNoteInfo) {
                            detailNote = ((DetailNoteInfo) data);
                        } else if (data instanceof List) {  //多条数据
                            detailNoteList = (List<DetailNoteInfo>) data;
                        }
                    }
                    switch (notifyType) {
                        case ADD:   //添加
                            KLog.d(TAG, "------addNote----" + detailNote);
                            if (detailNote != null) {
                                mainFragment.addNote(detailNote);
                            } else if (detailNoteList != null) {  //多条记录
                                mainFragment.addNotes(detailNoteList);
                            }
                            break;
                        case UPDATE:    //修改笔记
                            KLog.d(TAG, "------updateNote----" + detailNote);
                            if (detailNote != null) {
                                mainFragment.updateNote(detailNote);
                            }
                            break;
                        case DELETE:    //删除、移到回收站
                            KLog.d(TAG, "------deleteNote----" + detailNote);
                            if (detailNote != null) {
                                mainFragment.deleteNote(detailNote, false);
                            } else if (detailNoteList != null) {  //删除了多个笔记
                                mainFragment.deleteNotes(detailNoteList, false);
                            } else {    //清除了所有
                                mainFragment.clearNotes();
                            }
                            mainFragment.saveDeleteOpt();
                            break;
                        case MOVE:    //移动到其他文件夹
                            KLog.d(TAG, "------moveNote----" + detailNote);
                            boolean isFolderAll = TextUtils.isEmpty(mSelectedFolderId);
                            if (detailNote != null) {
                                mainFragment.deleteNote(detailNote, isFolderAll);
                            } else if (detailNoteList != null) {  //移动了多个笔记
                                mainFragment.deleteNotes(detailNoteList, isFolderAll);
                            }
                            if (isFolderAll) {  //在所有文件中，则给个操作结果的提示
                                mHandler.sendEmptyMessage(MSG_MOVE_SUCCESS);
                            }
                            break;
                        case BATCH_UPDATE:  //同时更新了多条记录
                            if (!SystemUtil.isEmpty(detailNoteList)) {
                                KLog.d(TAG, "------update note list size:" + detailNoteList.size());
                                mainFragment.updateNotes(detailNoteList);
                            }
                            break;
                        case MERGE: //合并笔记
                            if (detailNote != null) {   //单个
                                KLog.d(TAG, "------merge note single------");
                                mainFragment.mergeNote(detailNote);
                            } else if (!SystemUtil.isEmpty(detailNoteList)) {
                                KLog.d(TAG, "------merge note list size:" + detailNoteList.size());
                                mainFragment.mergeNotes(detailNoteList);
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
                        case BATCH_UPDATE:  //批量更新或者添加，若数据不存在，则添加
                            if (data instanceof List) {
                                List<Folder> folderList = (List<Folder>) data;
                                batchUpdate(folderList);
                            }
                            break;
                    }
                    break;
                /*case Provider.UserColumns.NOTIFY_FLAG:  //用户的通知
                    mainFragment = getMainFragment();
                    if (mainFragment == null) {
                        KLog.d(TAG, "--update--observer--mainFragment-is---null--");
                        return;
                    }
                    switch (notifyType) {
                        case ADD:   //用户添加
                            //加载该用户的笔记,优先加载本地的
                            KLog.d(TAG, "main activity user added will reload data");
                            mainFragment.reLoadData(true);
                            break;
                    }
                    break;*/
                case Provider.NOTIFY_FLAG:  //通用的刷新，一般重新加载数据，然后刷新界面
                    mainFragment = getMainFragment();
                    if (mainFragment == null) {
                        KLog.d(TAG, "--update--observer--mainFragment-is---null--");
                        return;
                    }
                    mainFragment.reLoadData(false);
                    break;
            }
        }
    }
}
