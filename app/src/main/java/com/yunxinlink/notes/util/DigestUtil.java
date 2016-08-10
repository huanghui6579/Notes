package com.yunxinlink.notes.util;

import com.yunxinlink.notes.util.log.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author huanghui1
 * @update 2016/6/18 15:39
 * @version: 0.0.1
 */
public class DigestUtil {
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
     * 计算字符串的MD5值
     * @param input 字符串
     * @return String
     */
    public static String md5Digest(String input) {

        byte[] data = input.getBytes();
        try {
            MessageDigest messageDigest = getMD5();
            messageDigest.update(data);
            return toHexString(messageDigest.digest());
        } catch (Exception e) {
            Log.e("-----md5Digest--error---" + e.getMessage());
        }
        return null;
    }
}
