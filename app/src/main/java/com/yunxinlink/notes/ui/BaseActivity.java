package com.yunxinlink.notes.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.ILockerActivityDelegate;
import com.yunxinlink.notes.lock.LockerDelegate;
import com.yunxinlink.notes.model.Folder;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.SystemUtil;

import me.imid.swipebacklayout.app.SwipeBackActivity;
import me.imid.swipebacklayout.app.SwipeBackActivityHelper;

/**
 * @author huanghui1
 * @update 2016/2/24 17:30
 * @version: 0.0.1
 */
public abstract class BaseActivity extends SwipeBackActivity {
    protected static String TAG = null;
    
    protected Context mContext;
    
    protected Toolbar mToolBar;
    
    /**
     * 是否显示返回箭头
     */
    private boolean mShowHomeUp;

    //手势滑动返回的工具类
    private SwipeBackActivityHelper mSwipeBackHelper;
    
    //密码锁的工具类
    private ILockerActivityDelegate mLockerActivityDelegate;

    public BaseActivity() {
        TAG = this.getClass().getSimpleName();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        
        mLockerActivityDelegate = LockerDelegate.getInstance(getApplicationContext());

        if (isInterrupt()) {
            KLog.d(TAG, "onCreate isInterrupt true");
            return;
        }
        
        if (hasLockedController()) {
            if (savedInstanceState == null) {
                savedInstanceState = new Bundle();
            }
            savedInstanceState.putBoolean(ILockerActivityDelegate.EXTRA_BOOLEAN_SHOULD_START_LOCK_DELAY, true);
            mLockerActivityDelegate.onCreate(this, savedInstanceState);
        }

        mShowHomeUp = showHomeUp();

        setContentView(getContentView());

        initToolBar();
        
        initView();

        initData();
        
        if (mToolBar != null) {
            updateToolBar(mToolBar);
        }
    }

    /**
     * 是否中断
     * @return
     */
    protected boolean isInterrupt() {
        return false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        
        if (hasLockedController()) {
            mLockerActivityDelegate.onRestart(this, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasLockedController()) {
            mLockerActivityDelegate.onResume(this, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        if (hasLockedController() && reLock()) {
            mLockerActivityDelegate.onDestroy(this, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (hasLockedController()) {
            mLockerActivityDelegate.onActivityResult(this, requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    protected final ILockerActivityDelegate getLockerActivityDelegate() {
        if (mLockerActivityDelegate == null) {
            KLog.d(TAG, "getLockerActivityDelegate but mLockerActivityDelegate is null, getAppLockActivityDelegate method must be called after onCreate called! ");
        }
        return mLockerActivityDelegate;
    }

    /**
     * 是否需要加锁
     * @return
     */
    protected boolean hasLockedController() {
        return true;
    }

    /**
     * 重新锁定应用，一般用于主界面的退出
     * @return
     */
    protected boolean reLock() {
        return false;
    }

    /**
     * 获取app的名称
     * @return
     */
    protected String getAppName() {
        return SystemUtil.getAppName(this);
    }
    
    /**
     * 判断当前用户是否登录
     * @author huanghui1
     * @update 2016/3/8 17:27
     * @version: 1.0.0
     */
    protected boolean isLogin() {
        return ((NoteApplication) getApplication()).getCurrentUser() != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                beforeBack();
                onBack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * 执行界面的返回操作
     * @author huanghui1
     * @update 2016/3/2 10:53
     * @version: 1.0.0
     */
    protected void onBack() {
        finish();
    }
    
    /**
     * 界面返回前的操作
     * @author tiger
     * @update 2016/3/5 15:09
     * @version 1.0.0
     */
    protected void beforeBack() {
        
    }
    
    /**
     * 创建popuMenu
     * @param showIcon 是否强制否显示图标
     * @author huanghui1
     * @update 2016/3/2 11:37
     * @version: 1.0.0
     */
    protected PopupMenu createPopMenu(View author, int menuResId, boolean showIcon, PopupMenu.OnMenuItemClickListener itemClickListener) {
        if (author != null) {
            PopupMenu popupMenu = new PopupMenu(this, author);
            popupMenu.inflate(menuResId);
            popupMenu.setOnMenuItemClickListener(itemClickListener);
            if (showIcon) {
                showPopMenuIcon(popupMenu);
            }
            return popupMenu;
        } else {
            return null;
        }
    }

    /**
     * 获取toolbar上各菜单项的view
     * @author huanghui1
     * @update 2016/3/2 14:22
     * @version: 1.0.0
     */
    protected View getToolBarMenuView(int menuItemId) {
        if (mToolBar != null) {
            View child = mToolBar.findViewById(menuItemId);
            return getToolBarMenuView(child);
        } else {
            return null;
        }
    }

    /**
     * 获取toolbar上各菜单项的view
     * @author huanghui1
     * @update 2016/7/21 16:57
     * @version: 1.0.0
     */
    protected View getToolBarMenuView(View child) {
        View parent = null;
        if (child != null) {
            ViewParent viewParent = child.getParent();
            if (viewParent instanceof ViewGroup) {
                parent = (ViewGroup) viewParent;
            } else {
                parent = child;
            }
        }
        return parent;
    }

    /**
     * 获取菜单
     * @return
     */
    protected Menu getOptionsMenu() {
        if (mToolBar != null) {
            return mToolBar.getMenu();
        }
        return null;
    }
    
    /**
     * 强制显示popupMenu的图标
     * @author huanghui1
     * @update 2016/3/2 11:31
     * @version: 1.0.0
     */
    protected void showPopMenuIcon(PopupMenu popupMenu) {
        SystemUtil.showPopMenuIcon(popupMenu);
    }

    /**
     * 设置菜单图标的颜色
     * @author tiger
     * @update 2016/2/27 16:00
     * @version 1.0.0
     */
    protected void setMenuOverFlowTint(MenuItem... menuItems) {
        SystemUtil.setMenuOverFlowTint(this, menuItems);
    }

    /**
     * 为菜单的图标着色
     * @author huanghui1
     * @update 2016/3/2 16:05
     * @version: 1.0.0
     */
    protected void setMenuTint(MenuItem item, int color) {
        if (item != null) {
            Drawable icon = item.getIcon().mutate();
            getTintDrawable(icon, color);
            item.setIcon(icon);
        }
    }

    /**
     * 返回着色后的图标
     * @param srcIcon 原始图标
     * @param color
     * @return
     */
    protected Drawable getTintDrawable(Drawable srcIcon, int color) {
        if (srcIcon != null) {
            int tint = color;
            if (tint == 0) {
                tint = getPrimaryColor();
            }
            srcIcon = DrawableCompat.wrap(srcIcon);
            DrawableCompat.setTint(srcIcon, tint);
        }
        return srcIcon;
    }

    /**
     * 创建叠层样式的图标
     * @param drawable
     * @return
     */
    protected LayerDrawable layerDrawable(Drawable... drawable) {
        int length = drawable.length;
        //合成的选中后的图标
        Drawable[] drawables = new Drawable[length];
        for (int i = 0; i < length; i++) {
            drawables[i] = drawable[i];
        }
        return new LayerDrawable(drawables);
    }

    /**
     * 获取包装状态的图标
     * @param drawable 原始的图标
     * @param colors 不同状态的颜色数组
     * @return
     */
    protected Drawable getStateListDrawable(Drawable drawable, int[] colors) {
        int[][] states = new int[2][];

        states[0] = new int[] { android.R.attr.state_selected};

        states[1] = new int[] {};

        return getStateListDrawable(drawable, states, colors);
    }

    /**
     * 获取包装状态的图标
     * @param drawable 原始的图标
     * @param colors 不同状态的颜色数组
     * @return
     */
    protected Drawable getStateListDrawable(Drawable drawable, int[][] stateSet, int[] colors) {

        ColorStateList colorList = new ColorStateList(stateSet, colors);

        StateListDrawable stateListDrawable = new StateListDrawable();

        int length = stateSet.length;
        for (int i = 0; i < length; i++) {
            stateListDrawable.addState(stateSet[i], drawable);//注意顺序
        }

        Drawable.ConstantState state = stateListDrawable.getConstantState();

        Drawable srcDrawable = DrawableCompat.wrap(state == null ? stateListDrawable : state.newDrawable()).mutate();

        DrawableCompat.setTintList(srcDrawable, colorList);
        return srcDrawable;
    }
    
    /**
     * 获取应用的主题色
     * @author huanghui1
     * @update 2016/3/2 16:07
     * @version: 1.0.0
     */
    protected int getPrimaryColor() {
        TypedArray a = obtainStyledAttributes(R.style.AppTheme, new int[] {R.attr.colorPrimary});
        int defaultColor = SystemUtil.getColor(this, R.color.colorPrimary);
        int color = a.getColor(0, defaultColor);
        a.recycle();
        return color;
    }
    
    protected abstract int getContentView();
    
    /**
     * 是否显示向上的返回箭头，默认显示
     * @author huanghui1
     * @update 2016/3/2 10:46
     * @version: 1.0.0
     */
    protected boolean showHomeUp() {
        return true;
    }
    
    /**
     * 初始化toolbar
     * @author huanghui1
     * @update 2016/2/24 17:39
     * @version: 1.0.0
     */
    protected void initToolBar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        if (mToolBar != null) {
            setSupportActionBar(mToolBar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null && mShowHomeUp) {
                actionBar.setDisplayHomeAsUpEnabled(mShowHomeUp);
            }
        }
    }

    /**
     * 更新toolbar的各种信息
     */
    protected void updateToolBar(Toolbar toolbar) {
        //TODO 子类酌情实现
    }
    
    /**
     * 获取当前登录的用户
     * @author huanghui1
     * @update 2016/3/8 18:14
     * @version: 1.0.0
     */
    protected User getCurrentUser() {
        NoteApplication app = (NoteApplication) getApplication();
        return app.getCurrentUser();
    }
    
    /**
     * 获取当前用户的本地数据库id
     * @author huanghui1
     * @update 2016/6/18 15:53
     * @version: 1.0.0
     */
    protected int getCurrentUserId() {
        User user = getCurrentUser();
        if (user != null) {
            return user.getId();
        }
        return 0;
    }
    
    /**
     * 获取默认的笔记保存文件夹的sid
     * @author huanghui1
     * @update 2016/6/25 10:51
     * @version: 1.0.0
     */
    protected String getDefaultFolderSid() {
        NoteApplication noteApp = (NoteApplication) getApplication();
        return noteApp.getDefaultFolderSid();
    }
    
    /**
     * 是否显示“所有文件夹”
     * @author huanghui1
     * @update 2016/6/25 16:02
     * @version: 1.0.0
     */
    protected boolean isShowFolderAll() {
        NoteApplication noteApp = (NoteApplication) getApplication();
        return noteApp.isShowFolderAll();
    }
    
    /**
     * 在后台执行任务
     * @author huanghui1
     * @update 2016/6/27 21:02
     * @version: 1.0.0
     */
    protected void doInbackground(Runnable runnable) {
        SystemUtil.getThreadPool().execute(runnable);
    }

    /**
     * 获取“所有文件夹”这一项
     * @return
     */
    protected Folder getFolderAll() {
        final Folder archive = new Folder();
        archive.setName(getString(R.string.default_archive));
        return archive;
    }
    
    /**
     * 初始化数据
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initData();
    
    /**
     * 初始化控件
     * @author huanghui1
     * @update 2016/2/24 17:35
     * @version: 1.0.0
     */
    protected abstract void initView();
}
