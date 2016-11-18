package com.yunxinlink.notes.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.DeviceApi;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.sync.download.DownloadTask;
import com.yunxinlink.notes.sync.download.SimpleDownloadListener;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import static com.yunxinlink.notes.util.NoteUtil.startSyncDownNote;

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
                NoteApplication app = (NoteApplication) context.getApplicationContext();
                boolean isWifi = SystemUtil.isWifiConnected(context);
                isWifi = true;
                VersionInfo versionInfo = null;
                if (isWifi) {
                    versionInfo = DeviceApi.checkVersion(context);
                } else {
                    KLog.d(TAG, "is not wifi so can not auto check app version");
                }
                app.setVersionInfo(versionInfo);
                User user = app.getCurrentUser();
                KLog.d(TAG, "app init check new version:" + versionInfo);
                //是否可以自动下载软件更新包
                boolean canAutoDown = false;
                
                final UserDto userDto = NoteUtil.buildLoginParams(context, user, null);
                if (userDto != null) {
                    boolean success = UserApi.login(context, userDto, null);
                    if (success) {
                        KLog.d(TAG, "app init completed user login success and will start sync down notes");
                        startSyncDownNote(context);
                    } else {
                        canAutoDown = true;
                    }
                } else {
                    canAutoDown = true;
                    KLog.d(TAG, "doAuthorityVerify userDto is null");
                }
                if (canAutoDown && versionInfo != null && versionInfo.checkContent()) {   //可以自动下载软件更新包
                    KLog.d(TAG, "app init will auto download new app package");
                    //如果本地已经有了新版本的信息，则下载新版本
                    boolean hasNewVersion = app.hasNewVersion();
                    if (hasNewVersion) {
                        KLog.d(TAG, "app init has new version and wifi connected and will download new app");
                        VersionInfo cloneInfo = versionInfo.clone();
                        app.setVersionInfo(null);
                        if (cloneInfo == null) {
                            KLog.d(TAG, "app init has new version clone version info error");
                            return;
                        }
                        DeviceApi.downloadApp(app, versionInfo, new AppDownloadListener());
                    }
                }
            }
        });
    }

    /**
     * APP下载的监听器
     */
    class AppDownloadListener extends SimpleDownloadListener {

        @Override
        public void onStart(DownloadTask downloadTask) {
            super.onStart(downloadTask);
            KLog.d(TAG, "download app onStart task:" + downloadTask);
        }

        @Override
        public void onCompleted(DownloadTask downloadTask) {
            super.onCompleted(downloadTask);
            KLog.d(TAG, "download app onStart task:" + downloadTask);
        }

        @Override
        public void onError(DownloadTask downloadTask) {
            super.onError(downloadTask);
            KLog.d(TAG, "download app onError task:" + downloadTask);
        }

        @Override
        public void onProgress(long bytesRead, long contentLength, boolean done) {
            super.onProgress(bytesRead, contentLength, done);
            KLog.d(TAG, "download app onProgress bytesRead:" + bytesRead + ", contentLength:" + contentLength + ", done:" + done);
        }

        @Override
        public void onCanceled(DownloadTask downloadTask) {
            super.onCanceled(downloadTask);
            KLog.d(TAG, "download app onCanceled task:" + downloadTask);
        }
    }
}
