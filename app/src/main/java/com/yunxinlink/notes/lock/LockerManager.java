package com.yunxinlink.notes.lock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.SettingsUtil;

/**
 * @author huanghui1
 * @update 2016/9/7 16:03
 * @version: 0.0.1
 */
public class LockerManager implements ILockerManager {

    private static final String TAG = "LockerManager";
    
    private static ILockerManager mLockerManager = null;
    
    private Context mContext;

    //密码锁定的相关属性
    private LockInfo mLockInfo;
    
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
        mLockInfo = new LockInfo();
        boolean hasLock = SettingsUtil.hasLock(context);
        LockType lockType = SettingsUtil.getLockType(context);
        mLockInfo.setLockType(lockType);
        mLockInfo.setHasLock(hasLock);
        if (hasLock && lockType != null) {  //设置的密码锁，且密码锁的类型可用
            mLockInfo.setLocking(true);
        } else {
            mLockInfo.setLocking(false);
        }
    }
    
    @Override
    public void setLockInfo(LockInfo lockInfo) {
        if (mLockInfo == null) {
            mLockInfo = new LockInfo();
        }
        mLockInfo.setLocking(lockInfo.isLocking());
        mLockInfo.setHasLock(lockInfo.hasLock());
        mLockInfo.setLockType(lockInfo.getLockType());
        
        SettingsUtil.setHasLock(lockInfo.hasLock());
        SettingsUtil.setLockType(lockInfo.getLockType());
    }

    /**
     * 注销广播
     */
    public void destory() {
        mLockInfo = null;
        if (mLockBroadcastReceiver != null) {
            mLockBroadcastReceiver.unregister(mContext);
            mLockBroadcastReceiver = null;
        }
    }
    
    @Override
    public boolean hasLock() {
        return mLockInfo != null && mLockInfo.hasLock();
    }

    @Override
    public boolean isBeingLocked() {
        return hasLock() && mLockInfo.isLocking();
    }

    @Override
    public void unlock() {
        if (mLockInfo != null) {
            mLockInfo.setLocking(false);
        }
    }

    @Override
    public void lock() {
        if (mLockInfo != null) {
            mLockInfo.setHasLock(true);
            mLockInfo.setLocking(true);
        }
    }
    
    private void makeLock() {
        if (mLockInfo != null) {
            if (mLockInfo.isLockAvailable()) {
                mLockInfo.setLocking(true);
            } else {
                mLockInfo.setLocking(false);
            }
        }
    }
    
    private void unMakeLock() {
        if (mLockInfo != null) {
            mLockInfo.setLocking(false);
        }
    }

    @Override
    public LockAction acquireLockerActivityAction() {
        LockType lockType = SettingsUtil.getLockType(mContext);
        if (lockType != null) {
            switch (lockType) {
                case PATTERN:   //图案密码
                    return LockerActivityAction.LOCKER_ACTIVITY_PATTERN.getLockAction();
                case DIGITAL:   //数字密码
                    return LockerActivityAction.LOCKER_ACTIVITY_DIGITAL.getLockAction();
            }
        }
        return LockerActivityAction.LOCKER_ACTIVITY_PATTERN.getLockAction();
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
                    KLog.d(TAG, "onReceive a action screen off");
                    makeLock();
                } else if (ACTION_LOCK_SETTINGS_CHANGED.equals(action)) {   //密码的策略改变了
                    KLog.d(TAG, "onReceive a action lock settings changed");
                    init(context);
                }
            }
        }
    }
}
