package net.ibaixin.notes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.socks.library.KLog;

import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.FilePathGenerator;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.util.log.LogFilter;

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

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        initLog();

        init(this);
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
    }
    
    private void initKLog() {
        KLog.init(BuildConfig.LOG_ENABLE, Constants.APP_ROOT_NAME);
    }

    public String getDefaultFolderSid() {
        return mDefaultFolderSid;
    }

    public void setDefaultFolderSid(String defaultFolderSid) {
        this.mDefaultFolderSid = defaultFolderSid;
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "--app----init---");
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                mDefaultFolderSid = sharedPreferences.getString(Constants.PREF_DEFAULT_FOLDER, "");
                mShowFolderAll = sharedPreferences.getBoolean(Constants.PREF_SHOW_FOLDER_ALL, true);

                //初始化图片加载器
                initImageLoaderConfig();
            }
        }, "init").start();
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
