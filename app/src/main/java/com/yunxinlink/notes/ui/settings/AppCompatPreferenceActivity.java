package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.socks.library.KLog;
import com.yunxinlink.notes.lock.ILockerActivityDelegate;
import com.yunxinlink.notes.lock.LockInfo;
import com.yunxinlink.notes.lock.LockerDelegate;
import com.yunxinlink.notes.receiver.SystemReceiver;

import me.imid.swipebacklayout.app.SwipeBackPreferenceActivity;

/**
 * A {@link android.preference.PreferenceActivity} which implements and proxies the necessary calls
 * to be used with AppCompat.
 */
public abstract class AppCompatPreferenceActivity extends SwipeBackPreferenceActivity {
    
    protected static String TAG;

    private AppCompatDelegate mDelegate;
    
    public AppCompatPreferenceActivity() {
        TAG = getClass().getSimpleName();
    }

    //密码锁的工具类
    private ILockerActivityDelegate mLockerActivityDelegate;

    //主题切换的广播
    private SystemReceiver mThemeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        mLockerActivityDelegate = LockerDelegate.getInstance(getApplicationContext());

        if (isInterrupt()) {
            KLog.d(TAG, "onCreate isInterrupt true");
//            return;
        }

        registerThemeReceiver();
    }

    /**
     * 注册主题变换的广播
     */
    private void registerThemeReceiver() {
        if (mThemeReceiver == null) {
            mThemeReceiver = new SystemReceiver(this);
        }
        IntentFilter filter = new IntentFilter(SystemReceiver.ACTION_THEME_CHANGE);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(mThemeReceiver, filter);
    }

    /**
     * 注销主题的广播
     */
    private void unregisterThemeReceiver() {
        if (mThemeReceiver != null) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(mThemeReceiver);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (outState != null) {
            outState.putBoolean(ILockerActivityDelegate.EXTRA_FLAG_IS_ACTIVITY_RECREATE, true);
            KLog.d(TAG, "onSaveInstanceState call outState is recreate:" + outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            savedInstanceState.remove(ILockerActivityDelegate.EXTRA_FLAG_IS_ACTIVITY_RECREATE);
            KLog.d(TAG, "onRestoreInstanceState call savedInstanceState:" + savedInstanceState);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    /**
     * 是否中断
     * @return
     */
    protected boolean isInterrupt() {
        return false;
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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getDelegate().onPostCreate(savedInstanceState);
    }

    public ActionBar getSupportActionBar() {
        return getDelegate().getSupportActionBar();
    }

    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        getDelegate().setSupportActionBar(toolbar);
    }

    @Override
    public MenuInflater getMenuInflater() {
        return getDelegate().getMenuInflater();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        getDelegate().setContentView(layoutResID);
    }

    @Override
    public void setContentView(View view) {
        getDelegate().setContentView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().setContentView(view, params);
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getDelegate().addContentView(view, params);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getDelegate().onPostResume();
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        getDelegate().setTitle(title);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getDelegate().onConfigurationChanged(newConfig);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();

        unregisterThemeReceiver();

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

    public void invalidateOptionsMenu() {
        getDelegate().invalidateOptionsMenu();
    }

    private AppCompatDelegate getDelegate() {
        if (mDelegate == null) {
            mDelegate = AppCompatDelegate.create(this, null);
        }
        return mDelegate;
    }

    /**
     * 设置actionbar
     * @param toolBarId
     */
    protected void setupActionBar(int toolBarId) {
        Toolbar toolbar = (Toolbar) findViewById(toolBarId);
        if (toolbar == null) {
            return;
        }

        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * 设置界列表的分隔线高度，1px
     */
    protected void setListDividerHeight() {
        getListView().setDividerHeight(1);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    protected boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * 更新密码锁的信息
     * @param lockInfo
     */
    protected void updateLockInfo(LockInfo lockInfo) {
        if (mLockerActivityDelegate != null) {
            mLockerActivityDelegate.updateLockInfo(lockInfo);
        }
    }

    /**
     * 更新目前app的解锁状态
     * @param isLocking 是否锁定，true：已锁定
     */
    protected void updateLockState(boolean isLocking) {
        if (mLockerActivityDelegate != null) {
            mLockerActivityDelegate.setLockState(isLocking);
        }
    }
}
