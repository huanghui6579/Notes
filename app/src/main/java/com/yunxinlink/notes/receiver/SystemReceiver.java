package com.yunxinlink.notes.receiver;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.api.impl.DeviceApi;
import com.yunxinlink.notes.api.impl.UserApi;
import com.yunxinlink.notes.api.model.UserDto;
import com.yunxinlink.notes.api.model.VersionInfo;
import com.yunxinlink.notes.model.DeviceInfo;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.service.CoreService;
import com.yunxinlink.notes.sync.download.DownloadTask;
import com.yunxinlink.notes.sync.download.SimpleDownloadListener;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;

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
                    bugReport();    //上报日志文件
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

                //检查版本更新
                VersionInfo versionInfo = checkVersion(app);

                //是否可以自动下载软件更新包
                boolean isOnLine = NoteUtil.isAccountOnline(context);
                if (isOnLine) { //用户有登录过，需要后台登录
                    User user = app.getCurrentUser();
                    final UserDto userDto = NoteUtil.buildLoginParams(context, user, null);
                    if (userDto != null) {
                        boolean success = UserApi.login(context, userDto, null);
                        if (success) {
                            KLog.d(TAG, "app init completed user login success and will start sync down notes");
                            startSyncDownNote(context);
                        }
                    } else {
                        KLog.d(TAG, "doAuthorityVerify userDto is null");
                    }
                } else {
                    KLog.d(TAG, "doAuthorityVerify user is offline");
                }
                
                if (versionInfo != null && versionInfo.checkContent()) {   //可以自动下载软件更新包
                    //下载软件版本
                    downloadAppInbackground(app, versionInfo);
                }
            }
        });
    }

    /**
     * 上传日志信息，主要是上传日志文件
     */
    private void bugReport() {
        SystemUtil.doInbackground(new NoteTask() {
            @Override
            public void run() {
                NoteApplication app = NoteApplication.getInstance();
                try {
                    KLog.d(TAG, "report bug invoke begin");
                    DeviceInfo deviceInfo = SystemUtil.getDeviceInfo(app);
                    String dir = SystemUtil.getLogPath();
                    if (TextUtils.isEmpty(dir)) {   //文件夹名称为空
                        KLog.d(TAG, "report bug log dir is empty");
                        return;
                    }
                    File filePath = new File(dir);
                    if (filePath.canWrite() && filePath.isDirectory() && filePath.exists()) {
                        //文件夹可用，扫描文件夹里的文件
                        File[] files = scanLogFiles(filePath);
                        if (files != null && files.length > 0) {
                            for (File file : files) {
                                DeviceApi.reportBug(deviceInfo, file);
                            }
                        } else {
                            KLog.d(TAG, "report bug log dir has no log");
                        }
                    }
                    KLog.d(TAG, "report bug invoke end");
                } catch (Exception e) {
                    KLog.e(TAG, "report bug error:" + e);
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 扫描过滤日志文件
     * @param dir
     * @return
     */
    private File[] scanLogFiles(File dir) {
        //文件夹可用，扫描文件夹里的文件
        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.exists() && pathname.canRead()) {
                    long fileLength = pathname.length();
                    String filename = pathname.getName();
                    Matcher matcher = NoteUtil.logFilePattern.matcher(filename);
                    boolean find = matcher.find();
                    if (find && fileLength <= Constants.MAX_LOG_FILE_SIZE) {  //文件找到且文件的最大只有5M
                        return true;
                    } else {
                        KLog.d(TAG, "scan log file can not match or file is too large:" + pathname + ", length:" + fileLength);
                    }
                }
                return false;
            }
        });
        return files;
    }

    /**
     * 检查版本更新
     * @param app
     * @return
     */
    private VersionInfo checkVersion(NoteApplication app) {
        boolean isWifi = SystemUtil.isWifiConnected(app);
        VersionInfo versionInfo = null;
        if (isWifi) {   //只有WiFi下才自动检查版本更新
            versionInfo = DeviceApi.checkVersion(app);
        } else {
            KLog.d(TAG, "is not wifi so can not auto check app version");
        }
        app.setVersionInfo(versionInfo);
        KLog.d(TAG, "app init check new version:" + versionInfo);
        return versionInfo;
    }

    /**
     * 下载软件版本
     * @param app
     * @param versionInfo
     */
    private void downloadAppInbackground(NoteApplication app, VersionInfo versionInfo) {
        KLog.d(TAG, "app init will auto download new app package");
        //如果本地已经有了新版本的信息，则下载新版本
        boolean hasNewVersion = app.hasNewVersion();
        if (hasNewVersion) {
            //检测是否否读写SD卡的权限
            boolean hasPermission = SystemUtil.hasPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (!hasPermission) {   //没有sd卡的读取权限
                KLog.d(TAG, "app init has new version but not write sd card permission");
                app.setVersionInfo(null);
                return;
            }
            KLog.d(TAG, "app init has new version and wifi connected and will download new app");
            DeviceApi.downloadApp(versionInfo, new AppDownloadListener(app));
        }
    }

    /**
     * APP下载的监听器
     */
    private class AppDownloadListener extends SimpleDownloadListener {

        private Context mContext;

        public AppDownloadListener(Context context) {
            this.mContext = context;
        }

        @Override
        public void onStart(DownloadTask downloadTask) {
            super.onStart(downloadTask);
            KLog.d(TAG, "download app sync service onStart task:" + downloadTask);
        }

        @Override
        public void onCompleted(DownloadTask downloadTask) {
            KLog.d(TAG, "download app sync service onStart task:" + downloadTask);
            //在界面上弹出安装的提示框
            VersionInfo versionInfo = ((NoteApplication) mContext.getApplicationContext()).getVersionInfo();
            if (versionInfo != null && versionInfo.checkContent()) {
                versionInfo.setFilePath(downloadTask.getSavePath());
                Intent service = new Intent(mContext, CoreService.class);
                service.putExtra(Constants.ARG_CORE_OPT, Constants.OPT_INSTALL_APP);
                mContext.startService(service);
            }
        }

        @Override
        public void onError(DownloadTask downloadTask) {
            super.onError(downloadTask);
            KLog.d(TAG, "download app sync service onError task:" + downloadTask);
        }

        @Override
        public void onProgress(long bytesRead, long contentLength, boolean done) {
        }

        @Override
        public void onCanceled(DownloadTask downloadTask) {
            super.onCanceled(downloadTask);
            KLog.d(TAG, "download app sync service onCanceled task:" + downloadTask);
        }
    }
}
