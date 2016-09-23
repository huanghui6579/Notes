package com.yunxinlink.notes.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.socks.library.KLog;
import com.yunxinlink.notes.api.impl.UserApiImpl;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 主题切换的广播
 */
public class SystemReceiver extends BroadcastReceiver {
    private static final String TAG = "SystemReceiver";

    /**
     * 主题变化的广播
     */
    public static final String ACTION_THEME_CHANGE = "com.yunxinlink.notes.receiver.ACTION_THEME_CHANGE";

    /**
     * 后台登录，校验权限
     */
    public static final String ACTION_AUTHORITY_VERIFY = "com.yunxinlink.notes.ACTION_AUTHORITY_VERIFY";

    private Context mContext;

    public SystemReceiver(Context context) {
        this.mContext = context;
    }

    public SystemReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_THEME_CHANGE:   //主题变化的广播
                    changeTheme(mContext);
                    break;
                case ACTION_AUTHORITY_VERIFY:   //app启动后的广播
                    doAuthorityVerify(context);
                    break;
            }
        }
    }

    /**
     * 切换主题
     * @param context
     */
    private void changeTheme(Context context) {
        KLog.d(TAG, "on receive a theme change receiver");
        if (context != null && context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.recreate();
        }
    }

    /**
     * 后台进行权限校验
     * @param context
     */
    private void doAuthorityVerify(final Context context) {
        SystemUtil.getThreadPool().execute(new NoteTask() {
            @Override
            public void run() {
                KLog.d(TAG, "doAuthorityVerify invoke");
                final UserDto userDto = NoteUtil.buildLoginParams(context, null, null);
                if (userDto == null) {
                    KLog.d(TAG, "doAuthorityVerify userDto is null");
                    return;
                }
                UserApiImpl.login(context, userDto, null);
            }
        });
    }
}
