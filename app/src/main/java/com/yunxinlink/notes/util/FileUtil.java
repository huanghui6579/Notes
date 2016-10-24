package com.yunxinlink.notes.util;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import com.socks.library.KLog;
import com.yunxinlink.notes.util.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;

/**
 * @author huanghui1
 * @update 2016/7/13 18:21
 * @version: 0.0.1
 */
public class FileUtil {
    private static final java.lang.String TAG = "FileUtil";

    /**
     * 文本的mime类型：text/plain
     */
    public static final String MIME_TYPE_TEXT = "text/plain";
    
    //压缩文件的格式集合
    public static List<String> suffixList = null;
    
    static {
        suffixList = new ArrayList<>();
        suffixList.add("zip");
        suffixList.add("gz");
        suffixList.add("nar");
        suffixList.add("rar");
        suffixList.add("gtar");
        suffixList.add("tar");
        suffixList.add("taz");
        suffixList.add("tgz");
    }

    private FileUtil() {}

    /**
     * 复制文件
     * @param source 原始文件
     * @param dest 复制到的目的文件
     */
    public static void copyFile(File source, File dest) {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } catch (Exception e) {
            Log.e(TAG, "---copyFile----error---" + e.getMessage());
        } finally {
            try {
                if (inputChannel != null) {
                    inputChannel.close();
                }
                if (outputChannel != null) {
                    outputChannel.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "---copyFile---close---error---" + e.getMessage());
            }
        }
    }

    /**
     * 复制文件
     * @param source 原始文件
     * @param dest 复制到的目的文件
     */
    public static void copyFile(String source, String dest) {
        File src = new File(source);
        File des = new File(dest);
        copyFile(src, des);
    }

    /**
     * 删除文件
     * @param filePath 文件的全路径
     */
    public static void deleteFile(String filePath) {
        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 删除多个文件
     * @param list 文件路径的集合
     */
    public static void deleteFiles(List<String> list) {
        if (list != null && list.size() > 0) {
            for (String path : list) {
                deleteFile(path);
            }
        }
    }

    /**
     * 获取文件的后缀名，不包含"."
     * @param file
     * @return
     */
    public static String getSuffix(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return null;
        }
        String fileName = file.getName();
        if (fileName.equals("") || fileName.endsWith(".")) {
            return null;
        }
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(index + 1).toLowerCase(Locale.getDefault());
        } else {
            return null;
        }
    }

    /**
     * 获取文件的后缀名，不包含"."
     * @param filePath
     * @return
     */
    public static String getSuffix(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        return getSuffix(new File(filePath));
    }

    /**
     * 获取文件的mime类型
     * @param file
     * @return
     */
    public static String getMimeType(File file) {
        String suffix = getSuffix(file);
        if (suffix == null) {
            return "file/*";
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null && !type.isEmpty()) {
            return type;
        }
        return "file/*";
    }

    /**
     * 获取文件的mime类型 ，针对web类型的,若不存在，则返回null
     * @param file
     * @return
     */
    public static String getWebMime(File file) {
        String suffix = getSuffix(file);
        if (suffix == null) {
            return null;
        }
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
        if (type != null && !type.isEmpty()) {
            return type;
        }
        return null;
    }

    /**
     * 判断附件是否存在
     * @param localPath 本地附件的全路径
     * @return
     */
    public static boolean isFileExists(String localPath) {
        if (TextUtils.isEmpty(localPath)) {
            return false;
        } else {
            File file = new File(localPath);
            boolean exists = false;
            try {
                exists = file.exists();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return exists;
        }
    }

    /**
     * 是否是压缩包
     * @param suffix 文件的后缀，如rar
     * @return
     */
    public static boolean isArchive(String suffix) {
        return suffixList.contains(suffix);
    }

    /**
     * 将流写入磁盘
     * @param body
     * @return
     */
    public static boolean writeResponseBodyToDisk(ResponseBody body, File saveFile) {
        try {
            // todo change the file location/name according to your needs
//            File futureStudioIconFile = new File(getExternalFilesDir(null) + File.separator + "Future Studio Icon.png");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(saveFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    KLog.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
