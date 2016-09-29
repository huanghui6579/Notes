package com.yunxinlink.notes.util;

import com.socks.library.KLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author huanghui1
 * @update 2016/6/18 15:39
 * @version: 0.0.1
 */
public class DigestUtil {
    private static final String TAG = "DigestUtil";
    
    /** MD5算法名称 */
    private static final String ALGORIGTHM_MD5 = "MD5";
    /** SHA-1算法名称 */
    private static final String ALGORIGTHM_SHA1 ="SHA-1";
    /** 字节数组缓存大小 */
    private static final int CACHE_SIZE = 2048;

    /**
     * 字节数组转换为16进制字符串
     * @param data 字节数组
     * @return String
     */
    private static String toHexString(byte[] data){
        StringBuilder digestStr = new StringBuilder();
        String stmp = "";
        for(int i = 0;i < data.length; i++){
            stmp = Integer.toHexString(data[i] & 0XFF);
            if(stmp.length() == 1) {
                digestStr.append("0").append(stmp);
            }else {
                digestStr.append(stmp);
            }
        }
        return digestStr.toString();
    }

    /**
     * 获取MD5实例
     * @return MessageDigest
     * @throws NoSuchAlgorithmException 异常
     */
    private static MessageDigest getMD5() throws NoSuchAlgorithmException{
        return MessageDigest.getInstance(ALGORIGTHM_MD5);
    }

    private static MessageDigest getSHA1() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(ALGORIGTHM_SHA1);
    }

    /**
     * 对字符串md5加密
     * @param str
     * @return
     */
    public static String md5Hex(String str) {
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            // 计算md5函数
            md.update(str.getBytes());
            // digest()最后确定返回md5 hash值，返回值为8为字符串。因为md5 hash值是16位的hex值，实际上就是8位的字符
            // BigInteger函数则将8位的字符串转换成16位hex值，用字符串来表示；得到字符串形式的hash值
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            KLog.d(TAG, "md5 hex error:" + e.getMessage());
        }
        return null;
    }

    /**
     * 获取文件的MD5值
     * @param file
     * @return
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月1日 下午4:38:41
     */
    public static String md5FileHex(File file) {
        String result = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = getMD5();
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            result = bi.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 获取文件的MD5值
     * @param filePath
     * @return
     * @author tiger
     * @version 1.0.0
     * @update 2015年5月1日 下午4:38:41
     */
    public static String md5FileHex(String filePath) {
        File file = new File(filePath);
        return md5FileHex(file);
    }
}
