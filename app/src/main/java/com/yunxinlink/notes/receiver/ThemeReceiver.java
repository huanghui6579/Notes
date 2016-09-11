package com.yunxinlink.notes.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.socks.library.KLog;

/**
 * 主题切换的广播
 */
public class ThemeReceiver extends BroadcastReceiver {

    public static final String ACTION_THEME_CHANGE = "com.yunxinlink.notes.receiver.THEME_CHANGE_ACTION";

    private static final String TAG = "ThemeReceiver";

    private Context mContext;

    public ThemeReceiver(Context context) {
        this.mContext = context;
    }

    public ThemeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        KLog.d(TAG, "on receive a theme change receiver");
        if (mContext != null && mContext instanceof Activity) {
            Activity activity = (Activity) mContext;
            activity.recreate();
        }
    }
}
