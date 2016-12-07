package com.yunxinlink.notes;

import android.content.Context;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.SystemUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Random;

/**
 * @author tiger
 * @version 1.0.0
 * @update 2016/12/3 11:14
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static CrashHandler instance; // 单例模式

    private Context mContext; // 程序Context对象
    private Thread.UncaughtExceptionHandler mDefaultHandler; // 系统默认的UncaughtException处理类

    /**
     * 获取CrashHandler实例
     *
     * @return CrashHandler
     */
    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }

        return instance;
    }

    private CrashHandler() {

    }

    /**
     * 异常处理初始化
     *
     * @param context
     */
    public void init(Context context) {
        this.mContext = context;
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        KLog.e("app crash thread name:" + t.getName() + ", error:" + e);
        // 自定义错误处理
        boolean res = handleException(t, e);
        if (!res && mDefaultHandler != null) {
            // 如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(t, e);
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                KLog.e("uncaught exception thread sleep error : ", e1);
            }
            KLog.e("app crash and will exit");
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param e
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Thread t, final Throwable e) {
        if (e == null) {
            return false;
        }
        try {
            File dir = SystemUtil.getLogDir();
            if (dir != null) {
                File file = new File(dir, getFileName());
                FileOutputStream fos = new FileOutputStream(file);
                PrintStream printStream = new PrintStream(fos);
                e.printStackTrace(printStream);
                KLog.d("app crash save log file >>> " + file.getAbsolutePath());
            } else {
                KLog.d("app crash but get log dir failed dir is null");
            }
            return true;
        } catch (Exception e1) {
            KLog.e("app crash save log file error:" + e.getMessage());
            e1.printStackTrace();
        }
        return false;
    }

    private String getFileName() {
        Random random = new Random();
        return "Log_" + Long.toString(System.currentTimeMillis() + random.nextInt(10000)).substring(4) + Constants.LOG_SUBFFIX;
    }
}