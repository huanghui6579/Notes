package net.ibaixin.notes.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.download.ImageDownloader;

import net.ibaixin.notes.NoteApplication;
import net.ibaixin.notes.R;
import net.ibaixin.notes.model.Attach;
import net.ibaixin.notes.richtext.AttachSpec;
import net.ibaixin.notes.util.log.Log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static ExecutorService cachedThreadPool = null;//可缓存的线程池

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
     * 生成笔记的sid
     * @author huanghui1
     * @update 2016/6/18 15:49
     * @version: 1.0.0
     */
    public static String generateNoteSid() {
        return PREFIX_NOTE + generateSid();
    }
    
    /**
     * 生成附件的sid
     * @author huanghui1
     * @update 2016/7/6 14:58
     * @version: 1.0.0
     */
    public static String generateAttachSid() {
        return PREFIX_ATTACH + generateSid();
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
     * 获取该应用程序默认存储在sd卡中的文件夹名称，默认路径为/mnt/sdcard/YunXinNotes/notes/#{sid}
     * @param sid 笔记的sid
     * @return
     */
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
            }
            String path = notePath + File.separator + sid;
            if (typeName != null) {
                path += File.separator + typeName;
            }
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
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
    public static List<String> getAttachSids(CharSequence text) {
        if (mPattern == null) {
            mPattern = Pattern.compile(mAttachRegEx);
        }
        Matcher matcher = mPattern.matcher(text);
        List<String> list = new ArrayList<>();
        while (matcher.find()) {
            String sid = matcher.group(1);
            list.add(sid);
        }
        return list;
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
     */
    public static void openFile(Context context, String filePath, int attachType) {
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
                type = "*/*";
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
     */
    public static void shareFile(Context context, String filePath, int attachType) {
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
                type = "*/*";
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
     * 显示控件
     * @param view
     */
    public static void showView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
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
     * 选择图片
     */
    public static void choseImage(Activity activity, int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
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
}
