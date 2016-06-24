package net.ibaixin.notes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.ibaixin.notes.model.User;
import net.ibaixin.notes.util.Constants;
import net.ibaixin.notes.util.SystemUtil;
import net.ibaixin.notes.util.log.FilePathGenerator;
import net.ibaixin.notes.util.log.Log;
import net.ibaixin.notes.util.log.LogFilter;

/**
 * @author huanghui1
 * @update 2016/2/24 19:28
 * @version: 0.0.1
 */
public class NoteApplication extends Application {
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
        Log.setFilePathGenerator(new FilePathGenerator.DateFilePathGenerator(SystemUtil.getLogPath(), Constants.LOG_DIR, Constants.LOG_SUBFFIX));
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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                mDefaultFolderSid = sharedPreferences.getString(Constants.PREF_DEFAULT_FOLDER, "");
                mShowFolderAll = sharedPreferences.getBoolean(Constants.PREF_SHOW_FOLDER_ALL, true);
            }
        }, "init").start();
    }

    public User getCurrentUser() {
        return mCurrentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.mCurrentUser = currentUser;
    }
}
