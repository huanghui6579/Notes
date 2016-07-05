package net.ibaixin.notes.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.R;

import java.io.File;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
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
    public static ExecutorService getThreadPool() {
        if (cachedThreadPool == null) {
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
        return PREFIX_NOTE + generateSid();
    }
    
    /**
     * 生成文件夹的sid
     * @author huanghui1
     * @update 2016/6/18 15:50
     * @version: 1.0.0
     */
    public static String generateFolderSid() {
        return PREFIX_FOLDER + generateSid();
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
    public static SharedPreferences getDefaultPreferences(Context context) {
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
    public static String getSelectedFolder(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(Constants.SELECTED_FOLDER_ID, null);
    }
    
    /**
     * 默认选中的文件夹
     * @author huanghui1
     * @update 2016/6/27 21:00
     * @version: 1.0.0
     */
    public static void setSelectedFolder(Context context, String folderId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Constants.SELECTED_FOLDER_ID, folderId);
        editor.apply();
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

    /**
     * 复制文字
     * @param context 上下文
     * @param text 要复制的文字
     * @param showTip 是否显示复制成功后的提示语            
     */
    public static void copyText(Context context, CharSequence text, boolean showTip) {
        if (text == null) {
            makeShortToast(R.string.tip_no_text);
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(Constants.CLIP_TEXT_LABLE, text);
        clipboard.setPrimaryClip(clipData);
        
        if (showTip) {  //显示提示语
            makeShortToast(R.string.copy_success);
        }
    }

    /**
     * 显示软键盘
     * @param context 上下文
     * @param view 键盘焦点的控件
     */
    public static void showSoftInput(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    /**
     * 隐藏软键盘
     * @param context 上下文
     * @param view 键盘焦点的控件
     */
    public static void hideSoftInput(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 隐藏/显示软键盘
     * @param context 上下文
     */
    public static void toggleSoftInput(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, 0);
    }

    /**
     * 判断键盘是否显示
     * @param window 窗口
     * @return 是否显示
     */
    public static boolean isSoftInputShow(Context context, Window window, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        return imm.isActive(view) || window.getAttributes().softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
    }

    /**
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes
     * @author tiger
     * @update 2015年3月13日 上午12:00:33
     * @return
     */
    public static File getDefaultAppFile() {
        File root = new File(Environment.getExternalStorageDirectory(), Constants.DEAULT_APP_FOLDER_NAME);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    /**
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes
     * @return
     */
    public static String getDefaultAppPath() {
        return getDefaultAppFile().getAbsolutePath();
    }

    /**
     * 根据note id生成文件保存文件的路径，如:/mnt/sdcard/YunXinNotes/attach/N454212545
     * @param noteId
     * @return
     */
    public static String generateNoteAttachPath(String noteId) {
        String root = getDefaultAppPath();
        StringBuilder sb = new StringBuilder(root);
        sb.append(File.separator)
                .append(Constants.DATA_MSG_ATT_FOLDER_NAME)
                .append(File.separator)
                .append(noteId);
        String path = sb.toString();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    /**
     * 根据note id生成文件保存文件缩略图的路径，如:/mnt/sdcard/YunXinNotes/attach/N454212545/thumb
     * @param noteId
     * @return
     */
    public static String generateNoteThumbAttachPath(String noteId) {
        String path = generateNoteAttachPath(noteId) + File.separator + "thumb";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }

    /**
     * 获取笔记缩略图的文件全路径
     * @param noteId 笔记id
     * @param filename 文件名
     * @return
     */
    public static String generateNoteThumbAttachFilePath(String noteId, String filename) {
        String path = generateNoteThumbAttachPath(noteId);
        StringBuilder sb = new StringBuilder(path);
        sb.append(File.separator)
                .append(filename);
        return sb.toString();
    }
}
