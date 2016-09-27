package com.yunxinlink.notes;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatDelegate;
import android.text.TextUtils;

import com.jiongbull.jlog.JLog;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.socks.library.KLog;
import com.yunxinlink.notes.api.impl.DeviceApiImpl;
import com.yunxinlink.notes.listener.SimpleOnLoadCompletedListener;
import com.yunxinlink.notes.model.ActionResult;
import com.yunxinlink.notes.model.DeviceInfo;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.receiver.SystemReceiver;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SettingsUtil;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.log.FilePathGenerator;
import com.yunxinlink.notes.util.log.Log;
import com.yunxinlink.notes.util.log.LogFilter;

import java.io.IOException;

/**
 * @author huanghui1
 * @update 2016/2/24 19:28
 * @version: 0.0.1
 */
public class NoteApplication extends Application {
    private static final String TAG = "NoteApplication";
    
    private static NoteApplication mInstance;

    /**
     * 当前登录的账号，如果没有登录，则为空
     */
    private User mCurrentUser;

    /**
     * 默认的文件夹sid
     */
    private String mDefaultFolderSid;
    
    /**
     * 是否显示“所有文件夹”这一项
     */
    private boolean mShowFolderAll = true;

    //主题切换的广播
    private SystemReceiver mSystemReceiver;
    
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        registerSystemReceiver();

        int nightMode = SettingsUtil.getDefaultNightMode(this);
        if (nightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        initLog();
        init(this);

        //初始化设备信息
        initDeviceInfo(this);

        //app 启动完毕
        onBootCompleted(this);
    }

    /**
     * 初始化设备信息
     * @param context
     */
    public void initDeviceInfo(final Context context) {
        SystemUtil.getThreadPool().execute(new NoteTask() {
            @Override
            public void run() {
                boolean shouldActive = NoteUtil.shouldActive(context);
                if (shouldActive) {    //需要激活
                    KLog.d(TAG, "device will active ...");
                    DeviceInfo deviceInfo = SystemUtil.getDeviceInfo(context);
                    DeviceApiImpl.activeDeviceInfo(deviceInfo, new SimpleOnLoadCompletedListener<ActionResult<Void>>() {
                        @Override
                        public void onLoadSuccess(ActionResult<Void> result) {
                            //成功，保存
                            NoteUtil.saveDeviceActive(context, true);
                        }
                    });
                } else {
                    KLog.d(TAG, "device already active ");
                }
            }
        });
    }

    /**
     * app 启动完成
     * @param context
     */
    private void onBootCompleted(final Context context) {
        //延迟500毫秒执行
        mHandler.postDelayed(new NoteTask() {
            @Override
            public void run() {
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
                Intent intent = new Intent(SystemReceiver.ACTION_AUTHORITY_VERIFY);
                localBroadcastManager.sendBroadcast(intent);
            }
        }, 500);
    }

    /**
     * 注册主题变换的广播
     */
    private void registerSystemReceiver() {
        if (mSystemReceiver == null) {
            mSystemReceiver = new SystemReceiver(this);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(SystemReceiver.ACTION_THEME_CHANGE);
        filter.addAction(SystemReceiver.ACTION_AUTHORITY_VERIFY);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(mSystemReceiver, filter);
    }

    /**
     * 注销主题的广播
     */
    private void unregisterSystemReceiver() {
        if (mSystemReceiver != null) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(mSystemReceiver);
        }
    }

    @Override
    public void onTerminate() {
        unregisterSystemReceiver();
        super.onTerminate();
    }

    /**
     * 获得全局的application
     * @return 全局的application
     */
    public static NoteApplication getInstance() {
        return mInstance;
    }

    /**
     * 初始化日志的配置
     * @author huanghui1
     * @update 2016/3/3 10:23
     * @version: 1.0.0
     */
    private void initLog() {
        Log.setEnabled(BuildConfig.LOG_ENABLE);
        Log.setLog2ConsoleEnabled(BuildConfig.LOG_TO_CONSOLE_ENABLE);
        Log.setLog2FileEnabled(BuildConfig.LOG_TO_FILE_ENABLE);
        Log.addLogFilter(new LogFilter.LevelFilter(Log.LEVEL.valueOf(BuildConfig.LOG_TO_FILE_LEVEL)));
        Log.setGlobalTag(Constants.APP_ROOT_NAME);
        try {
            Log.setFilePathGenerator(new FilePathGenerator.DateFilePathGenerator(SystemUtil.getLogPath(), Constants.APP_LOG_DIR, Constants.LOG_SUBFFIX));
        } catch (IOException e) {
            Log.e(TAG, "---initLog---error---" + e.getMessage());
            e.printStackTrace();
        }

        initKLog();
    }
    
    private void initKLog() {
        KLog.init(BuildConfig.LOG_ENABLE, Constants.APP_ROOT_NAME);
    }
    
    private void initJLog() {
        JLog.init(this).setDebug(BuildConfig.DEBUG);;
//        KLog.init(BuildConfig.LOG_ENABLE, Constants.APP_ROOT_NAME);
    }

    public String getDefaultFolderSid() {
        return mDefaultFolderSid;
    }

    public void setDefaultFolderSid(String defaultFolderSid) {
        this.mDefaultFolderSid = defaultFolderSid;

        updateWidgetFolder();
    }

    public boolean isShowFolderAll() {
        return mShowFolderAll;
    }

    public void setShowFolderAll(boolean showFolderAll) {
        this.mShowFolderAll = showFolderAll;
    }

    /**
     * 初始化基本数据
     * @author huanghui1
     * @update 2016/6/24 15:40
     * @version: 1.0.0
     */
    private void init(final Context context) {
        SystemUtil.getThreadPool().execute(new NoteTask() {
            @Override
            public void run() {
                Log.d(TAG, "--app----init---");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                mDefaultFolderSid = sharedPreferences.getString(Constants.PREF_DEFAULT_FOLDER, "");
                mShowFolderAll = sharedPreferences.getBoolean(Constants.PREF_SHOW_FOLDER_ALL, true);
                //是否有通知栏的快捷方式
                boolean hasCreateShortcut = sharedPreferences.getBoolean(getString(R.string.settings_key_more_shortcut), false);
                if (hasCreateShortcut) {
                    NoteUtil.createNotificationShortcut(getApplicationContext(), Constants.ID_NOTIFY_CREATE_SHORTCUT);
                }
                //初始化本地用户
                initLocalUser(context);

                //初始化图片加载器
                initImageLoaderConfig();
            }
        });
    }

    /**
     * 初始化本地的用户，获取本地用户的数据
     * @param context
     * @return
     */
    public User initLocalUser(Context context) {
        //获取本地用户的id
        User user = null;
        int userId = NoteUtil.getAccountId(context);
        if (userId <= 0) {  //如果本地用户id不存在，则看看是否使用的第三方账号登录的
            KLog.d(TAG, "init local user local user id is <= 0");
            int accountType = NoteUtil.getAccountType(context);
            String openUserId = null;
            if (accountType >= 0) { //非本地账号
                openUserId = NoteUtil.getOpenUserId(context, accountType);
            }
            if (!TextUtils.isEmpty(openUserId)) {   //第三方账号存在
                KLog.d(TAG, "init local user open by open user id:" + openUserId);
                user = UserManager.getInstance().getAccountInfo(openUserId);
            } else {
                KLog.d(TAG, "init local user open user id is null");
            }
        } else {
            KLog.d(TAG, "init local user open by user id:" + userId);
            user = UserManager.getInstance().getAccountInfo(userId);
        }
        KLog.d(TAG, "init local user result:" + user);
        setCurrentUser(user);
        return user;
    }

    /**
     * 更新桌面小部件的默认文件夹
     */
    public void updateWidgetFolder() {
        int[] widgetIds = NoteUtil.getShortCreateAppWidgetId(this);
        if (widgetIds != null && widgetIds.length > 0) {    //widget id 有效
            KLog.d(TAG, "updateWidgetFolder send broadcast ");
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        } else {
            KLog.d(TAG, "updateWidgetFolder widgetIds is null or size is 0 ");
        }
    }


    public User getCurrentUser() {
        return mCurrentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.mCurrentUser = currentUser;
    }

    /**
     * 初始化图片加载器
     */
    private void initImageLoaderConfig() {
        int width = 480;
        int height = 800;
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCacheExtraOptions(width, height)
                .diskCacheExtraOptions(width, height, null)
                .denyCacheImageMultipleSizesInMemory()	//同一个imageUri只允许在内存中有一个缓存的bitmap
                .memoryCache(new LruMemoryCache(2 * 1024 * 1024))
                .memoryCacheSize(2 * 1024 * 1024)
                .diskCacheSize(50 * 1024 * 1024)
                .defaultDisplayImageOptions(getDefaultDisplayOptions())
                .writeDebugLogs()
                .build();
        ImageLoader.getInstance().init(config);
    }

    /**
     * 初始化默认的图片显示选项
     * @return
     */
    private DisplayImageOptions getDefaultDisplayOptions() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
//                .showImageOnLoading(R.drawable.ic_stub)
//                .showImageForEmptyUri(R.drawable.ic_empty)
//                .showImageOnFail(R.drawable.ic_error)
                .cacheInMemory(true)
                .cacheOnDisk(false)
                .imageScaleType(ImageScaleType.IN_SAMPLE_INT)
                .bitmapConfig(Bitmap.Config.RGB_565)	//防止内存溢出
                //.displayer(new FadeInBitmapDisplayer(200))
                .build();
        return options;
    }
}
