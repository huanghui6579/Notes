package com.yunxinlink.notes.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.yunxinlink.notes.util.SettingsUtil;

/**
 * @author huanghui1
 * @update 2016/9/7 16:03
 * @version: 0.0.1
 */
public class LockerManager implements ILockerManager {

    private static ILockerManager mLockerManager = null;
    
    private Context mContext;

    /**
     * 是否有锁
     */
    private boolean mHasLock;

    /**
     * 是否被锁中
     */
    private boolean mIsLocking;
    
    private LockBroadcastReceiver mLockBroadcastReceiver;
    
    public LockerManager(Context context) {
        this.mContext = context;
        mLockBroadcastReceiver = new LockBroadcastReceiver();
        mLockBroadcastReceiver.register(context);
        init(context);
    }
    
    public static synchronized ILockerManager getInstance(Context context) {
        if (mLockerManager == null) {
            mLockerManager = new LockerManager(context);
        }

        return mLockerManager;
    }

    /**
     * 初始化
     */
    private void init(Context context) {
        mHasLock = SettingsUtil.hasLock(context);
        mIsLocking = false;
    }

    /**
     * 注销广播
     */
    public void destory() {
        if (mLockBroadcastReceiver != null) {
            mLockBroadcastReceiver.unregister(mContext);
            mLockBroadcastReceiver = null;
        }
    }
    
    @Override
    public boolean hasLock() {
        return mHasLock;
    }

    @Override
    public boolean isBeingLocked() {
        return mIsLocking;
    }

    @Override
    public void unlock() {
        mIsLocking = false;
    }

    @Override
    public void lock() {
        mHasLock = true;
        mIsLocking = true;
    }
    
    private void makeLock() {
        mIsLocking = true;
    }
    
    private void unMakeLock() {
        mIsLocking = false;
    }

    @Override
    public String acquireLockerActivityAction() {
        LockType lockType = SettingsUtil.getLockType(mContext);
        if (lockType != null) {
            switch (lockType) {
                case PATTERN:   //图案密码
                    return LockerActivityAction.LOCKER_ACTIVITY_PATTERN.getAction();
                case DIGITAL:   //数字密码
                    return LockerActivityAction.LOCKER_ACTIVITY_DIGITAL.getAction();
            }
        }
        return LockerActivityAction.LOCKER_ACTIVITY_PATTERN.getAction();
    }
    
    class LockBroadcastReceiver extends BroadcastReceiver {

        private static final String ACTION_LOCK_SETTINGS_CHANGED = "com.yunxinlink.notes.LOCK_CHANGED_ACTION";

        /**
         * 注册广播
         * @param context
         */
        public void register(Context context) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(ACTION_LOCK_SETTINGS_CHANGED);
            context.registerReceiver(this, filter);
        }
        
        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action)) {  //灭屏广播
                    makeLock();
                } else if (ACTION_LOCK_SETTINGS_CHANGED.equals(action)) {   //密码的策略改变了
                    init(context);
                }
            }
        }
    }
}
