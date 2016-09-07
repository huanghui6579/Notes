package com.yunxinlink.notes.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.LockType;

/**
 * 设置的工具
 * @author huanghui1
 * @update 2016/9/7 16:25
 * @version: 0.0.1
 */
public class SettingsUtil {
    //是否打开了安全密码
    private static Boolean mHasLock;
    //密码锁的类型
    private static LockType mLockType;

    /**
     * 是否有安全锁
     * @return
     */
    public static boolean hasLock(Context context) {
        if (mHasLock == null) {
            SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
            mHasLock = preferences.getBoolean(context.getString(R.string.settings_key_security_password), false);
        }
        return mHasLock;
    }

    /**
     * 获取密码锁的类型，1：图案密码，2：数字密码, see {@link LockType#DIGITAL} and {@link LockType#PATTERN}
     * @return
     */
    public static LockType getLockType(Context context) {
        if (mLockType == null) {
            SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
            int type = preferences.getInt(context.getString(R.string.settings_key_security_type), 0);
            mLockType = LockType.valueOf(type);
        }
        return mLockType;
    }
}
