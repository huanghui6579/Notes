package com.yunxinlink.notes.lockpattern.utils;

import com.socks.library.KLog;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加解密的工具
 * @author tiger
 * @version 1.0.0
 * @update 2016/9/3 0:56
 */
public class EncryptionUtil {
    //加密的向量
    /**
     * AES VECTOR
     * @hide
     */
    private static final String VECTOR = "12457afg24354487";
    /**
     * AES加密
     * @param key 加密的密钥
     * @param content 要加密的内容
     * @return
     */
    public static String AESEncrypt(String key, String content) {
        if (key == null) {
            KLog.d("AESEncrypt key is null");
            return null;
        }
        // 判断Key是否为16位
        if (key.length() != 16) {
            KLog.d("AESEncrypt key length is not 16");
            return null;
        }

        try {
            byte[] raw = key.getBytes();
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");//"算法/模式/补码方式"
            IvParameterSpec iv = new IvParameterSpec(VECTOR.getBytes());//使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(content.getBytes());

            return new String(BASE64Encoder.encode(encrypted));//此处使用BASE64做转码功能，同时能起到2次加密的作用。
        } catch (Exception e) {
            KLog.d("AESEncrypt error:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * AES解密
     * @param key 解密的密钥
     * @param content 加密后的内容
     * @return
     */
    public static String AESDecrypt(String key, String content) {
        try {
            // 判断Key是否正确
            if (key == null) {
                KLog.d("AESDecrypt key is null");
                return null;
            }
            // 判断Key是否为16位
            if (key.length() != 16) {
                KLog.d("AESDecrypt key length is not 16");
                return null;
            }
            byte[] raw = key.getBytes("ASCII");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(VECTOR.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] encrypted = BASE64Encoder.decode(content);//先用base64解密
            byte[] original = cipher.doFinal(encrypted);
            return new String(original);
        } catch (Exception e) {
            KLog.d("AESDecrypt error:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
