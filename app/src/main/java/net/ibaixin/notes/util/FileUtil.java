package net.ibaixin.notes.util;

import android.webkit.MimeTypeMap;

import net.ibaixin.notes.util.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author huanghui1
 * @update 2016/7/13 18:21
 * @version: 0.0.1
 */
public class FileUtil {
    private static final java.lang.String TAG = "FileUtil";
    
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
}
