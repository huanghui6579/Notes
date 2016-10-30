package com.yunxinlink.notes.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.nostra13.universalimageloader.core.download.ImageDownloader;
import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.Attach;
import com.yunxinlink.notes.model.DeviceInfo;
import com.yunxinlink.notes.richtext.AttachSpec;
import com.yunxinlink.notes.richtext.AttachText;
import com.yunxinlink.notes.util.log.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Headers;

/**
 * @author huanghui1
 * @update 2016/2/24 18:04
 * @version: 0.0.1
 */
public class SystemUtil {
    
    private static final String TAG = SystemUtil.class.getSimpleName();
    
    private static final String PREFIX_NOTE = "N";
    private static final String PREFIX_FOLDER = "F";
    private static final String PREFIX_ATTACH = "A";
    private static final String PREFIX_PAINT = "P";
    private static final String PREFIX_DETAIL = "D";
    
    //颜色进度度的阀值
    private static final double COLOR_THRESHOLD = 180.0;

    private static ExecutorService cachedThreadPool = null;//可缓存的线程池

    //email的正则表达式
    public static Pattern VALID_EMAIL_ADDRESS_REGEX = null;

    /**
     * 附件的正则表达式
     */
    public static final String mAttachRegEx = "\\[" + Constants.ATTACH_PREFIX + "=([a-zA-Z0-9_]+)\\]";

    private static Pattern mPattern;

    /**
     * 相片名称的格式化
     */
    private static SimpleDateFormat mCameraNameFormat;
    
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
     * 停止任务
     */
    public static void shutDownExecutor() {
        if (cachedThreadPool != null && !cachedThreadPool.isShutdown()) {
            try {
                cachedThreadPool.shutdown();
            } catch (Exception e) {
                KLog.e(TAG, "shut down executor error:" + e.getMessage());
            }
        }
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
     * 生成清单的sid
     * @author huanghui1
     * @update 2016/8/3 14:27
     * @version: 1.0.0
     */
    public static String generateDetailSid() {
        return PREFIX_DETAIL + generateSid();
    }
    
    /**
     * 生成附件的sid
     * @author huanghui1
     * @update 2016/7/6 14:58
     * @version: 1.0.0
     */
    public static String generateAttachSid() {
        return generateAttachSid(0);
    }

    /**
     * 生成附件的sid
     * @param attachType 附件的类型
     * @return
     */
    public static String generateAttachSid(int attachType) {
        String prefix = null;
        switch (attachType) {
            case Attach.PAINT:
                prefix = PREFIX_PAINT;
                break;
            default:
                prefix = PREFIX_ATTACH;
                break;
        }
        return prefix + generateSid();
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
        return formatSid(hashCodeV);
    }

    /**
     * 生成同步的sid,该sid是固定的
     * @return
     */
    public static String generateSyncSid() {
        int hashCodeV = 0;
        return formatSid(hashCodeV);
    }

    /**
     * 格式化sid
     * @param hashCode
     * @return
     */
    private static String formatSid(int hashCode) {
        if (hashCode < 0) {// 有可能是负数
            hashCode = -hashCode;
        }
        // 0 代表前面补充0
        // 15 代表长度为15
        // d 代表参数为正数型
        return /*machineId + */String.format("%018d", hashCode);
    }

    /**
     * 显示短时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:49:18
     * @param text
     */
    public static void makeShortToast(CharSequence text) {
        Toast.makeText(NoteApplication.getInstance(), text, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示短时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:49:18
     * @param resId
     */
    public static void makeShortToast(int resId) {
        Toast.makeText(NoteApplication.getInstance(), resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示长时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:50:02
     * @param text
     */
    public static void makeLongToast(CharSequence text) {
        Toast.makeText(NoteApplication.getInstance(), text, Toast.LENGTH_LONG).show();
    }

    /**
     * 显示长时间的toast
     * @author Administrator
     * @update 2014年10月7日 上午9:50:02
     * @param resId
     */
    @Deprecated
    public static void makeLongToast(int resId) {
        Toast.makeText(NoteApplication.getInstance(), resId, Toast.LENGTH_LONG).show();
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
     * 当前Android系统版本是否在Android4.4或者之上
     * @return
     */
    public static boolean hasSdkV19() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
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
     * 当前Android系统版本是否在Android4.1或者之上
     * @author tiger
     * @update 2016/3/5 8:06
     * @version 1.0.0
     */
    public static boolean hasSdkV17() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
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
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes
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
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes
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
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes/notes
     * @return
     */
    public static File getNoteDir() {
        File file = getAppRootDir();
        File path = null;
        if (file != null) {
            path = new File(file, Constants.APP_NOTES_FOLDER_NAME);
            if (!path.exists()) {
                path.mkdirs();
            }
        }
        return path;
    }

    /**
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes/notes
     * @return
     */
    public static String getNotePath() {
        File file = getNoteDir();
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * 获取用户头像在sd卡中的文件夹名称，默认路径为：/mnt/sdcard/YunXinNotes/icon
     * @return
     */
    public static String getNoteAvatarPath() {
        String rootPath = getAppRootPath();
        String iconPath = null;
        if (rootPath != null) {
            iconPath = rootPath + File.separator + Constants.APP_AVATAR_FOLDER_NAME;
            File file = new File(iconPath);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return iconPath;
    }

    /**
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes/notes/#{sid}
     * @param sid 笔记的sid
     * @return
     */
    @Deprecated
    public static String getNotePath(String sid) {
        String dir = getNotePath();
        if (dir != null) {
            return dir + File.separator + sid;
        }
        return null;
    }
    
    /**
     * 获取日志的路径,默认为/sdcard/YunXinNotes/log/
     * @author huanghui1
     * @update 2016/3/3 10:58
     * @version: 1.0.0
     */
    public static String getLogPath() throws IOException {
        String rootPath = getAppRootPath();
        if (rootPath != null) {
            return rootPath + File.separator + Constants.APP_LOG_DIR;
        } else {
            return null;
        }
    }

    /**
     * 根据附件类型获取对应类型的存储路径,默认为/sdcard/YunXinNotes/notes/#{noteid}/#{type}
     * @param sid
     * @param attachType
     * @return
     * @throws IOException
     */
    public static String getAttachPath(String sid, int attachType) throws IOException {
        return getAttachPath(sid, attachType, true);
    }

    /**
     * 根据附件类型获取对应类型的存储路径,默认为/sdcard/YunXinNotes/notes/#{noteid}/#{type}
     * @param sid
     * @param attachType
     * @param createDir 当文件夹不存在时，是否创建文件夹
     * @return
     * @throws IOException
     */
    public static String getAttachPath(String sid, int attachType, boolean createDir) throws IOException {
        String notePath = getNotePath();
        if (notePath != null) {
            String typeName = null;
            switch (attachType) {
                case Attach.IMAGE:  //图片
                    typeName = Constants.APP_CAMERA_FOLDER_NAME;
                    break;
                case Attach.VOICE:  //语音
                    typeName = Constants.APP_VOICE_FOLDER_NAME;
                    break;
                case Attach.PAINT:  //涂鸦
                    typeName = Constants.APP_PAINT_FOLDER_NAME;
                    break;
            }
            String path = notePath + File.separator + sid;
            if (typeName != null) {
                path += File.separator + typeName;
            }
            if (createDir) {
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdirs();
                }
            }
            return path;
        } else {
            return null;
        }
    }

    /**
     * 获取相机图片的全路径，包含文件名
     * @param sid 笔记的sid
     * @return
     * @throws IOException
     */
    public static File getCameraFile(String sid) throws IOException {
        return getAttachFile(sid, Attach.IMAGE);
    }

    /**
     * 生成相机的照片，格式为IMG_20160621_120205.jpg
     */
    public static String generateCameraFilename() {
        if (mCameraNameFormat == null) {
            mCameraNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        }
        return "IMG_" + mCameraNameFormat.format(new Date()) + ".jpg";
    }

    /**
     * 生成录音的文件，格式为V20160621_120205.amr
     * @return
     */
    public static String generateVoiceFilename() {
        if (mCameraNameFormat == null) {
            mCameraNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        }
        return "V" + mCameraNameFormat.format(new Date()) + ".amr";
    }

    /**
     * 生成涂鸦的文件，格式为P20160621_120205.png
     * @return
     */
    public static String generateHandWriteFilename() {
        if (mCameraNameFormat == null) {
            mCameraNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        }
        return "P" + mCameraNameFormat.format(new Date()) + ".png";
    }

    /**
     * 获取附件的全路径
     * @param sid 笔记的sid
     * @param attachType 文件的类型
     * @return
     */
    public static String getAttachFilePath(String sid, int attachType) throws IOException {
        String filename = null;
        switch (attachType) {
            case Attach.IMAGE:
                filename = generateCameraFilename();
                break;
            case Attach.VOICE:  //语音
                filename = generateVoiceFilename();
                break;
            case Attach.PAINT:  //涂鸦文件
                filename = generateHandWriteFilename();
                break;
        }
        String attachDir = SystemUtil.getAttachPath(sid, attachType);
        if (attachDir == null || filename == null) {
            Log.d(TAG, "--getAttachFilePath-----error---null--");
            return null;
        }
        return attachDir + File.separator + filename;
    }

    /**
     * 获取附件
     * @param sid 笔记的sid
     * @param attachType 文件的类型
     * @return
     * @throws IOException
     */
    public static File getAttachFile(String sid, int attachType) throws IOException {
        String filePath = getAttachFilePath(sid, attachType);
        if (filePath != null) {
            return new File(filePath);
        }
        return null;
    }

    /**
     * 生成用户头像的文件名称,格式为：456457544542.png
     * @param sid 用户的sid
     * @return
     * @throws IOException
     */
    public static File getAvatarFile(String sid) throws IOException {
        if (TextUtils.isEmpty(sid)) {
            return null;
        }
        String iconPath = getNoteAvatarPath();
        if (iconPath != null) {
            String iconName = sid + ".png";
            return new File(iconPath, iconName);
        }
        return null;
    }

    /**
     * 生成用户头像的文件名称,格式为：456457544542.png
     * @param filename 文件名
     * @return
     * @throws IOException
     */
    public static File getAvatarFileByName(String filename) throws IOException {
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        String iconPath = getNoteAvatarPath();
        if (iconPath != null) {
            return new File(iconPath, filename);
        }
        return null;
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
     * 判断map是否为空
     * @param map
     * @return
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.size() == 0;
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
        ClipData clipData = ClipData.newPlainText(Constants.CLIP_TEXT_LABEL, text);
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
     * 根据note id生成文件保存文件的路径，如:/mnt/sdcard/YunXinNotes/attach/N454212545
     * @param noteId
     * @return
     */
    public static String generateNoteAttachPath(String noteId) {
        String root = getAppRootPath();
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

    /**
     * 将从uri地址中获取实际的文件地址
     * @param uriStr uri,content://
     * @param context context
     * @return 返回文件的本地地址
     */
    public static String getFilePathFromContentUri(String uriStr, Context context) {
        String filePath = null;
        switch (ImageDownloader.Scheme.ofUri(uriStr)) {
            case FILE:
                filePath = ImageDownloader.Scheme.FILE.crop(uriStr);
                break;
            case CONTENT:
                // DocumentProvider
                Uri uri = Uri.parse(uriStr);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (DocumentsContract.isDocumentUri(context, uri)) {
                        // ExternalStorageProvider
                        if (isExternalStorageDocument(uri)) {
                            final String docId = DocumentsContract.getDocumentId(uri);
                            final String[] split = docId.split(":");
                            final String type = split[0];
    
                            if ("primary".equalsIgnoreCase(type)) {
                                filePath = Environment.getExternalStorageDirectory() + File.separator + split[1];
                            }
    
                            // TODO handle non-primary volumes
                        }
                        // DownloadsProvider
                        else if (isDownloadsDocument(uri)) {
    
                            final String id = DocumentsContract.getDocumentId(uri);
                            final Uri contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                            filePath = getDataColumn(context, contentUri, null, null);
                        }
                        // MediaProvider
                        else if (isMediaDocument(uri)) {
                            final String docId = DocumentsContract.getDocumentId(uri);
                            final String[] split = docId.split(":");
                            final String type = split[0];
    
                            Uri contentUri = null;
                            if ("image".equals(type)) {
                                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            } else if ("video".equals(type)) {
                                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            } else if ("audio".equals(type)) {
                                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            }
    
                            final String selection = "_id=?";
                            final String[] selectionArgs = new String[] {
                                    split[1]
                            };

                            filePath = getDataColumn(context, contentUri, selection, selectionArgs);
                        }
                    } else {
                        // Return the remote address
                        if (isGooglePhotosUri(uri)) {

                            filePath = uri.getLastPathSegment();
                        } else {
                            filePath = getDataColumn(context, uri, null, null);
                        }

                    }
                } else {
                    // Return the remote address
                    if (isGooglePhotosUri(uri)) {
                        filePath = uri.getLastPathSegment();
                    } else {
                        filePath = getDataColumn(context, uri, null, null);
                    }
                }
                break;
        }
        
        return filePath;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * 从文本中获取附件的信息,[0]：是匹配的文本内容[attach=fdfdf],[1]是附件的sid：fdfdf
     * @param text
     * @return
     */
    public static List<AttachSpec> getAttachText(CharSequence text) {
        if (mPattern == null) {
            mPattern = Pattern.compile(mAttachRegEx);
        }
        Matcher matcher = mPattern.matcher(text);
        List<AttachSpec> list = new ArrayList<>();
        while (matcher.find()) {
            AttachSpec spec = new AttachSpec();
            String s = matcher.group();
            String sid = matcher.group(1);
            int start = matcher.start();
            int end = matcher.end();
            spec.text = s;
            spec.sid = sid;
            spec.start = start;
            spec.end = end;
            list.add(spec);
        }
        return list;
    }

    /**
     * 从文本中获取附件的sid列表
     * @param text
     * @return
     */
    public static AttachText getAttachSids(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        if (mPattern == null) {
            mPattern = Pattern.compile(mAttachRegEx);
        }
        Matcher matcher = mPattern.matcher(text);
        List<String> list = new ArrayList<>();
        boolean find = false;
        while (matcher.find()) {
            find = true;
            String sid = matcher.group(1);
            list.add(sid);
        }
        String s = null;
        if (find) {
            s = matcher.replaceAll("");
        }
        AttachText attachText = new AttachText();
        attachText.setAttachSids(list);
        attachText.setText(s);
        KLog.d(TAG, "-----AttachText---s:" + s);
        return attachText;
    }

    /**
     * 从文本中获取附件的sid
     * @param text
     * @return
     */
    public static String getAttachSid(CharSequence text) {
        if (mPattern == null) {
            mPattern = Pattern.compile(mAttachRegEx);
        }
        Matcher matcher = mPattern.matcher(text);
        String sid = null;
        if (matcher.find()) {
            sid = matcher.group(1);
        }
        return sid;
    }

    /**
     * 从文件的全路径中获取文件名
     * @param filePath 文件的全部路径
     * @return 返回文件名
     */
    public static String getFilename(String filePath) {
        if (filePath == null) {
            return null;
        }
        String filename = null;
        int index = filePath.lastIndexOf(File.separator);
        if (index != -1) {
            filename = filePath.substring(index + 1);
        }
        return filename;
    }

    /**
     * 打开文件
     * @param context
     * @param filePath
     * @param attachType 文件的类型，参考Attach#Image
     * @param mimeType 文件的mime类型                  
     */
    public static void openFile(Context context, String filePath, int attachType, String mimeType) {
        Uri uri = Uri.fromFile(new File(filePath));
        Intent intent = new Intent(Intent.ACTION_VIEW);

        String type = null;
        switch (attachType) {
            case Attach.IMAGE:  //图片
                type = "image/*";
                break;
            case Attach.VOICE:  //音频文件
                type = "audio/*";
                break;
            default:
                if (TextUtils.isEmpty(mimeType)) {
                    type = "*/*";
                } else {
                    type = mimeType;
                }
                break;
        }

        intent.setDataAndType(uri, type);
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 分享文件
     * @param context
     * @param filePath
     * @param attachType 文件的类型如图片：image/*
     * @param mimeType 文件的mime类型                  
     */
    public static void shareFile(Context context, String filePath, int attachType, String mimeType) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        String type = null;
        switch (attachType) {
            case Attach.IMAGE:  //图片
                type = "image/*";
                break;
            case Attach.VOICE:  //音频文件
                type = "audio/*";
                break;
            default:
                if (TextUtils.isEmpty(mimeType)) {
                    type = "*/*";
                } else {
                    type = mimeType;
                }
                break;
        }
        sendIntent.setType(type);
        if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(sendIntent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 分享文本内容
     * @param context
     * @param text 文本内容
     */
    public static void shareText(Context context, String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        String type = FileUtil.MIME_TYPE_TEXT;
        sendIntent.setType(type);
        if (sendIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(sendIntent);
        } else {
            SystemUtil.makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 隐藏控件
     * @param view
     */
    public static void hideView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * 显示控件
     * @param view
     */
    public static void showView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 选择图片
     */
    public static void choseImage(Activity activity, int requestCode) {
        choseFile(activity, "image/*", requestCode);
    }

    /**
     * 选择文件，不限格式
     */
    public static void choseFile(Activity activity, String type, int requestCode) {
        Intent intent = new Intent();
        if (type == null) { //所有格式的文件
            type = "*/*";
        }
        intent.setType(type);
        if (SystemUtil.hasSdkV19()) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
//            intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
            intent.setAction(Intent.ACTION_PICK);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 将图片添加到相册中
     * @param image 图片文件
     */
    public static void galleryAddPic(Context context, File image) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(image);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * 打开相机拍照
     */
    public static void openCamera(Activity activity, String sid, int requestCode) throws IOException {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//create a intent to take picture 
        File file = getCameraFile(sid);
        if (file == null) {
            Log.d(TAG, "----openCamera---getCameraFile--error---file----is--null----");
            return;
        }
        //create a intent to take picture  
        Uri uri = Uri.fromFile(file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri); // set the image file name 
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            makeShortToast(R.string.tip_no_app_handle);
        }
    }

    /**
     * 格式化文件大小的显示
     * @param size 文件的大小
     * @return
     */
    public static String formatFileSize(double size) {
        String[] units = new String[]{"B","KB","MB","GB","TB","PB"};
        double mod = 1024.0;
        int i = 0;
        for (i = 0; size >= mod; i++) {
            size /= mod;
        }
        return String.format(Locale.getDefault(), "%.1f", size) + units[i];
    }

    /**
     * 设置颜色值的alpha
     * @param color
     * @param factor alpha值
     * @return
     */
    public static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * 计算控件爱你的宽度
     * @param itemView
     * @param maxWidth
     * @return [0]:width,[1]:height
     */
    public static int[] measureContentSize(View itemView, int maxWidth) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        itemView.measure(widthMeasureSpec, heightMeasureSpec);
        int itemWidth = itemView.getMeasuredWidth();
        int itemHeight = itemView.getMeasuredHeight();
        if (itemWidth >= maxWidth) {
            itemWidth = maxWidth;
        }
        return new int[] {itemWidth, itemHeight};
    }

    /**
     * 根据提供的属性获得该属性值对应的资源id
     * @update 2015年1月22日 下午6:48:30
     * @param attr 属性
     * @return
     */
    public static int getResourceId(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_INT && typedValue.type <= TypedValue.TYPE_LAST_INT) {
                return typedValue.data;
            } else if (typedValue.type == TypedValue.TYPE_STRING) {
                return typedValue.resourceId;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /**
     * 根据alpha合成颜色
     * @param alpha Alpha component [0..255] of the color
     * @param srcColor
     * @return
     */
    public static int calcColor(int alpha, int srcColor) {
        int red = Color.red(srcColor);
        int green = Color.green(srcColor);
        int blue = Color.blue(srcColor);
        return Color.argb(alpha, red, green, blue);
    }

    /**
     * 获取屏幕的宽度
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取文件的类型，{@link Attach#type}
     * @param file
     * @return
     */
    public static int guessFileType(File file) {
        String mimeType = FileUtil.getMimeType(file);
        return guessFileType(file.getAbsolutePath(), mimeType);
    }

    /**
     * 获取文件的类型，{@link Attach#type}
     * @param filePath 文件名或者文件的路径
     * @param mimeType 文件的mime类型                
     * @return
     */
    public static int guessFileType(String filePath, String mimeType) {
        int type = Attach.FILE;
        String suffix = FileUtil.getSuffix(filePath);
        if (FileUtil.isArchive(suffix)) {
            type = Attach.ARCHIVE;
        } else {
            if (mimeType.startsWith("image/")) {    //图片
                type = Attach.IMAGE;
            } else if (mimeType.startsWith("audio/")) { //音频文件
                type = Attach.VOICE;
            } else if (mimeType.startsWith("video/")) { //视频频文件
                type = Attach.VIDEO;
            }
        }
        return type;
    }

    /**
     * 设置控件的可视状态
     * @param view
     * @param visibility
     */
    public static void setViewVisibility(View view, int visibility) {
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    /**
     * 设置控件是否可用
     * @param view
     * @param enable
     */
    public static void setViewEnable(View view, boolean enable) {
        if (view.isEnabled() != enable) {
            view.setEnabled(enable);
        }
    }

    /**
     * 是否是相近的颜色
     * @param baseColor 基准颜色，如Color.BLACK
     * @param color 获取到的实际颜色
     * @return 是否与基准颜色相近
     */
    public static boolean isColorSimilar(int baseColor, int color) {
        int simpleBaseColor = baseColor | 0xff000000;
        int simpleColor = color | 0xff000000;
        
        int baseRed = Color.red(simpleBaseColor) - Color.red(simpleColor);
        int baseGreen = Color.green(simpleBaseColor) - Color.green(simpleColor);
        int baseBlue = Color.blue(simpleBaseColor) - Color.blue(simpleColor);
        
        double value = Math.sqrt(baseRed * baseRed + baseGreen * baseGreen + baseBlue * baseBlue);
        if (value < COLOR_THRESHOLD) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 颜色是否近似黑色
     * @param color
     * @return
     */
    public static boolean isColorSimilarBlack(int color) {
        return isColorSimilar(Color.BLACK, color);
    }

    /**
     * 颜色是否近似白色
     * @param color
     * @return
     */
    public static boolean isColorSimilarWhite(int color) {
        return isColorSimilar(Color.WHITE, color);
    }

    /**
     * 强制显示popupMenu的图标
     * @author huanghui1
     * @update 2016/3/2 11:31
     * @version: 1.0.0
     */
    public static void showPopMenuIcon(PopupMenu popupMenu) {
        try {
            Field field = popupMenu.getClass().getDeclaredField("mPopup");
            if (field != null) {
                field.setAccessible(true);
                Object obj = field.get(popupMenu);
                if (obj instanceof MenuPopupHelper) {
                    MenuPopupHelper popupHelper = (MenuPopupHelper) obj;
                    popupHelper.setForceShowIcon(true);
                }
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断popuMenu是否显示
     * @author tiger
     * @update 2016/2/27 17:38
     * @version 1.0.0
     */
    public static boolean isPopuMenuShowing(PopupMenu popupMenu) {
        boolean isShowing = false;
        if (popupMenu != null) {
            try {
                Field field = popupMenu.getClass().getDeclaredField("mPopup");
                if (field != null) {
                    field.setAccessible(true);
                    Object obj = field.get(popupMenu);
                    if (obj instanceof MenuPopupHelper) {
                        MenuPopupHelper popupHelper = (MenuPopupHelper) obj;
                        isShowing = popupHelper.isShowing();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return isShowing;
    }

    /**
     * 获取app的名称
     * @return
     */
    public static String getAppName(Context context) {
        int resId = context.getApplicationInfo().labelRes;
        return context.getString(resId);
    }

    /**
     * 设置菜单图标的颜色
     * @author tiger
     * @update 2016/2/27 16:00
     * @version 1.0.0
     */
    public static void setMenuOverFlowTint(Context context, MenuItem... menuItems) {
        if (menuItems != null) {
            TypedArray a = context.obtainStyledAttributes(android.support.v7.appcompat.R.style.Widget_AppCompat_ActionButton_Overflow, new int[]{android.R.attr.src});
            Drawable drawable = a.getDrawable(0);
//            a = obtainStyledAttributes(R.style.AppTheme_PopupOverlay, new int[] {R.attr.colorButtonNormal});
            int tint = SystemUtil.getColor(context, R.color.colorButtonControl);
            if (drawable != null) {
                DrawableCompat.setTint(drawable, tint);
                for (MenuItem item : menuItems) {
                    item.setIcon(drawable);
                }
            }
            a.recycle();
        }
    }

    /**
     * 返回着色后的图标
     * @param srcIcon 原始图标
     * @param color
     * @return
     */
    public static Drawable getTintDrawable(Context context, Drawable srcIcon, int color) {
        if (srcIcon != null) {
            int tint = color;
            if (tint == 0) {
                tint = getPrimaryColor(context);
            }
            srcIcon = DrawableCompat.wrap(srcIcon);
            DrawableCompat.setTint(srcIcon, tint);
        }
        return srcIcon;
    }

    /**
     * 获取应用的主题色
     * @author huanghui1
     * @update 2016/3/2 16:07
     * @version: 1.0.0
     */
    public static int getPrimaryColor(Context context) {
        TypedArray a = context.obtainStyledAttributes(R.style.AppTheme, new int[] {R.attr.colorPrimary});
        int defaultColor = SystemUtil.getColor(context, R.color.colorPrimary);
        int color = a.getColor(0, defaultColor);
        a.recycle();
        return color;
    }

    /**
     * 获取手机Android 版本（4.4、5.0、5.1 ...）
     *
     * @return
     */
    public static String getBuildVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机Android 版本号（22、23 ...）
     *
     * @return
     */
    public static int getBuildSDKInt() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String getPhoneModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机品牌
     *
     * @return
     */
    public static String getPhoneBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取设备的唯一标识，deviceId
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        if (deviceId == null) {
            return "";
        } else {
            return deviceId;
        }
    }

    /**
     * 获取设备的编译信息
     * @return
     */
    public static String getBuildNumber() {
        String osBuildNumber = Build.FINGERPRINT;  //"360/QK1505/QK1505:6.0.1/MMB29M/6.0.026.P0.160831.QK1505:userdebug/test-keys"
        final String forwardSlash = "/";
        String osReleaseVersion = Build.VERSION.RELEASE + forwardSlash;
        try {
            osBuildNumber = osBuildNumber.substring(osBuildNumber.indexOf(osReleaseVersion));  //"6.0.1/MMB29M/6.0.026.P0.160831.QK1505:userdebug/test-keys”
            osBuildNumber = osBuildNumber.replace(osReleaseVersion, "");  //"MMB29M/6.0.026.P0.160831.QK1505:userdebug/test-keys”
            osBuildNumber = osBuildNumber.substring(0, osBuildNumber.indexOf(forwardSlash)); //"MMB29M"
        } catch (Exception e) {
            osBuildNumber = "";
            Log.e(TAG, "getBuildNumber Exception while parsing - " + e.getMessage());
        }
        return osBuildNumber;
    }

    /**
     * 获取设备信息
     * @param context
     * @return
     */
    public static DeviceInfo getDeviceInfo(Context context) {
        DeviceInfo deviceInfo = new DeviceInfo();
        String imei = getDeviceId(context);
        String os = "Android";
        String osVersion = getBuildVersion();
        String phoneModel = getPhoneModel();
        String brand = getPhoneBrand();

        deviceInfo.setImei(imei);
        deviceInfo.setOs(os);
        deviceInfo.setOsVersion(osVersion);
        deviceInfo.setPhoneModel(phoneModel);
        deviceInfo.setBrand(brand);
        return deviceInfo;
    }

    /**
     * 是否是email
     * @param text
     * @return
     */
    public static boolean isEmail(CharSequence text) {
        if (VALID_EMAIL_ADDRESS_REGEX == null) {
            VALID_EMAIL_ADDRESS_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        }
        Matcher matcher = VALID_EMAIL_ADDRESS_REGEX .matcher(text);
        return matcher.find();
    }

    /**
     * 是否是电话号码
     * @param number
     * @return
     */
    public static boolean isPhoneNumber(String number) {
        Class<PhoneNumberUtil> clazz = PhoneNumberUtil.class;
        boolean isPhone = true;
        try {
            Method method = clazz.getDeclaredMethod("isViablePhoneNumber", String.class);
            if (method != null) {
                method.setAccessible(true);
                isPhone = (boolean) method.invoke(clazz, number);
            }
        } catch (Exception e) {
            KLog.d(TAG, "isPhoneNumber number:" + number + ", error:" + e.getMessage());
            e.printStackTrace();
        }
        return isPhone;
    }

    /**
     * 检测当的网络（WLAN、3G/2G）状态
     * @param context Context
     * @return true 表示网络可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断两个字符串是否相等，
     * 若都为null，则相等
     * @param srcStr
     * @param targetStr
     * @return
     */
    public static boolean equalsStr(String srcStr, String targetStr) {
        boolean result = false;
        if (srcStr == null && targetStr == null) {
            result = true;
        } else if (srcStr != null) {
            result = srcStr.equals(targetStr);
        }
        return result;
    }

    /**
     * 从headers中获取文件的名称
     * @param headers
     * @return
     */
    public static String getFilename(Headers headers) {
        String filename = null;
        if (headers != null) {
            try {
                String contentDesc = headers.get("Content-Disposition");
                KLog.d(TAG, "get filename from  headers content desc:" + contentDesc);
                if (!TextUtils.isEmpty(contentDesc)) {
                    String regex = "(filename=\\S+);";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(contentDesc);
                    if (matcher.find()) {
                        String content = matcher.group(1);
                        //filename=47447545778754.png
                        String[] array = content.split("=");
                        filename = array[1];
                        KLog.d(TAG, "get filename from headers :" + filename);
                    }
                }
            } catch (Exception e) {
                KLog.e(TAG, "get filename from headers error:" + e.getMessage());
            }
        }
        return filename;
    }
}
