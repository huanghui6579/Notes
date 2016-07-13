package net.ibaixin.notes.util;

import net.ibaixin.notes.util.log.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author huanghui1
 * @update 2016/7/13 18:21
 * @version: 0.0.1
 */
public class FileUtil {
    private static final java.lang.String TAG = "FileUtil";

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
}
