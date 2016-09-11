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

    //当前设置的默认主题，主要是默认和夜间两种
    private static Integer mDefaultNightMode;

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
    
    public static void setHasLock(boolean hasLock) {
        mHasLock = hasLock;
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
    
    public static void setLockType(LockType lockType) {
        mLockType = lockType;
    }

    /**
     * 获取保存的主题,one of {@link android.support.v7.app.AppCompatDelegate#MODE_NIGHT_YES}, {@link android.support.v7.app.AppCompatDelegate#MODE_NIGHT_NO}
     * @param context
     * @return
     */
    public static int getDefaultNightMode(Context context) {
        if (mDefaultNightMode == null || mDefaultNightMode == -1) {
            SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
            mDefaultNightMode = preferences.getInt(context.getString(R.string.settings_key_theme_mode), -1);
        }
        return mDefaultNightMode;
    }

    /**
     * 保存主题
     * @param context
     * @param mode 主题类型，one of {@link android.support.v7.app.AppCompatDelegate#MODE_NIGHT_YES}, {@link android.support.v7.app.AppCompatDelegate#MODE_NIGHT_NO}
     */
    public static void setDefaultNightMode(Context context, int mode) {
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(context.getString(R.string.settings_key_theme_mode), mode);
        editor.apply();

        mDefaultNightMode = mode;
    }

}
