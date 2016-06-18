package net.ibaixin.notes.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.R;

import java.io.File;
import java.text.DateFormat;
import java.util.Collection;
import java.util.UUID;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author huanghui1
 * @update 2016/2/24 18:04
 * @version: 0.0.1
 */
public class SystemUtil {
    
    private static final String TAG = SystemUtil.class.getSimpleName();
    
    private static final String PREFIX_NOTE = "N";
    private static final String PREFIX_FOLDER = "F";

    private static ExecutorService cachedThreadPool = null;//可缓存的线程池
    
    private SystemUtil() {}

    /**
     * 获得可缓存的线程池
     * @return
     */
    public static ExecutorService getThreadPool(){
        if(cachedThreadPool == null) {
            synchronized (SystemUtil.class) {
                if(cachedThreadPool == null) {
                    cachedThreadPool = Executors.newCachedThreadPool();
                }
            }
        }
        return cachedThreadPool;
    }
    
    /**
     * 生成笔记的sid
     * @author huanghui1
     * @update 2016/6/18 15:49
     * @version: 1.0.0
     */
    public static String generateNoteSid() {
        return PREFIX_NOTE + generateNoteSid();
    }
    
    /**
     * 生成文件夹的sid
     * @author huanghui1
     * @update 2016/6/18 15:50
     * @version: 1.0.0
     */
    public static String generateFolderSid() {
        return PREFIX_FOLDER + generateNoteSid();
    }
    
    /**
     * 生成sid
     * @author huanghui1
     * @update 2016/6/18 15:49
     * @version: 1.0.0
     */
    public static String generateSid() {
        int hashCodeV = UUID.randomUUID().toString().hashCode();
        if (hashCodeV < 0) {// 有可能是负数
            hashCodeV = -hashCodeV;
        }
        // 0 代表前面补充0
        // 15 代表长度为15
        // d 代表参数为正数型
        return /*machineId + */String.format("%018d", hashCodeV);
    }

    /**
     * 显示短时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:49:18
     * @param text
     */
    public static void makeShortToast(CharSequence text) {
        Toast toast = Toast.makeText(NoteApplication.getInstance(), text, Toast.LENGTH_SHORT);
        toast = setToastStyle(toast);
        toast.show();
    }

    /**
     * 显示短时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:49:18
     * @param resId
     */
    public static void makeShortToast(int resId) {
        Toast toast = Toast.makeText(NoteApplication.getInstance(), resId, Toast.LENGTH_SHORT);
        toast = setToastStyle(toast);
        toast.show();
    }

    /**
     * 显示长时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:50:02
     * @param text
     */
    public static void makeLongToast(CharSequence text) {
        Toast toast = Toast.makeText(NoteApplication.getInstance(), text, Toast.LENGTH_LONG);
        toast = setToastStyle(toast);
        toast.show();
    }

    /**
     * 显示长时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:50:02
     * @param resId
     */
    public static void makeLongToast(int resId) {
        Toast toast = Toast.makeText(NoteApplication.getInstance(), resId, Toast.LENGTH_LONG);
        toast = setToastStyle(toast);
        toast.show();
    }

    /**
     * 设置Toast的样式
     * @update 2014年11月12日 下午4:22:41
     * @param toast
     * @return
     */
    private static Toast setToastStyle(Toast toast) {
        View view = toast.getView();
        view.setBackgroundResource(R.drawable.toast_frame);
        TextView textView = (TextView) view.findViewById(android.R.id.message);
        textView.setTextColor(Color.WHITE);
        return toast;
    }

    /**
     * 当前Android系统版本是否在Android5.0或者之上
     * @author tiger
     * @update 2016/2/27 9:21
     * @version 1.0.0
     */
    public static boolean hasSdkV21() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
    
    /**
     * 当前Android系统版本是否在Android6.0或者之上
     * @author tiger
     * @update 2016/2/27 15:59
     * @version 1.0.0
     */
    public static boolean hasSdkV23() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 当前Android系统版本是否在Android4.1或者之上
     * @author tiger
     * @update 2016/3/5 8:06
     * @version 1.0.0
     */
    public static boolean hasSdkV16() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }


    /**
     * 获取默认名称的SharedPreferences
     * @author tiger
     * @update 2016/2/28 12:16
     * @version 1.0.0
     */
    public static  SharedPreferences getDefaultPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     * 获取资源中的颜色
     * @author huanghui1
     * @update 2016/3/2 16:11
     * @version: 1.0.0
     */
    public static int getColor(Context context, int colorResId) {
        if (hasSdkV23()) {
            return context.getColor(colorResId);
        } else {
            return context.getResources().getColor(colorResId);
        }
    }

    /**
     * 设置背景图片
     * @author tiger
     * @update 2016/3/5 8:05
     * @version 1.0.0
     */
    public static void setBrackground(View view, Drawable drawable) {
        if (hasSdkV16()) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }
    }

    /**
     * 判断sd卡是否可用
     * @author huanghui1
     * @update 2016/3/3 10:38
     * @version: 1.0.0
     */
    public static boolean isSDCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }
    
    /**
     * 判断sd卡是否可写
     * @author huanghui1
     * @update 2016/3/3 10:47
     * @version: 1.0.0
     */
    public static boolean isSDCardWriteable() {
        boolean isWriteable = false;
        if (isSDCardAvailable()) {
            if (!Environment.MEDIA_MOUNTED_READ_ONLY.equals(Environment.getExternalStorageState())) {    //可写
                isWriteable = true;
            }
        }
        return isWriteable;
    }
    
    /**
     * 获取应用程序的根目录
     * @author huanghui1
     * @update 2016/3/3 10:33
     * @version: 1.0.0
     */
    public static File getAppRootDir() {
        File root = null;
        if (isSDCardAvailable()) {  //内存卡可用
            File dir = Environment.getExternalStorageDirectory();
            if (isSDCardWriteable()) {
                root = new File(dir, Constants.APP_ROOT_NAME);
                if (!root.exists()) {
                    root.mkdirs();
                }
            }
        }
        return root;
    }
    
    /**
     * 获取应用程序的根目录
     * @author huanghui1
     * @update 2016/3/3 10:54
     * @version: 1.0.0
     */
    public static String getAppRootPath() {
        File file = getAppRootDir();
        if (file != null) {
            return file.getAbsolutePath();
        } else {
            return null;
        }
    }
    
    /**
     * 获取日志的路径,默认为/sdcard/IbaixinNotes/log/
     * @author huanghui1
     * @update 2016/3/3 10:58
     * @version: 1.0.0
     */
    public static String getLogPath() {
        String rootPath = getAppRootPath();
        if (rootPath != null) {
            return rootPath + File.separator + Constants.LOG_DIR;
        } else {
            return null;
        }
    }
    
    /**
     * 获取默认选中的文件夹
     * @author huanghui1
     * @update 2016/3/8 18:05
     * @version: 1.0.0
     */
    public static int getSelectedFolder(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(Constants.SELECTED_FOLDER_ID, 0);
    }
    
    /**
     * 判断该集合是否为空
     * @param collection 集合
     * @author huanghui1
     * @update 2016/3/9 9:17
     * @version: 1.0.0
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }

    /**
     * 获取当前的格式化日期
     * @author tiger
     * @update 2016/3/13 10:20
     * @version 1.0.0
     */
    public static final String getFormatTime() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        return dateFormat.format(new Date());
    }
    
}
