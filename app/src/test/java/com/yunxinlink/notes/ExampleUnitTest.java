package com.yunxinlink.notes;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void aesEncod() {
    	/*
        //需要加密的内容
        String content = "的安全组件，其中的安全加密功能提供了开发者密钥的安全管理与加密算法实现，保证密钥的安全性，实现安全的加解密操作。同时从应用层、运行层、native层\n" +
                "提供多层次全方位立体防护，还有安全沙箱、白盒加密、底层加固\n" +
                "共同保证客户端数据安全。攻防是不断变化演进的，它能实时更新客户端模块\n" +
                "保证攻防对抗强度\n" +
                "\n" +
                "作者：justbeit\n" +
                "链接：https://www.zhihu.com/question/35136485/answer/84491440\n" +
                "来源：知乎\n" +
                "著作权归作者所有，转载请联系作者获得授权。";

        //密钥
        String password = "YunxinNotesAES/@android";

        System.out.println("加密之前：" + content);

        System.out.println("\r\n");

        // 加密
        byte[] encrypt = encrypt(content, password);
        System.out.println("加密后的内容：" + new String(encrypt));

        System.out.println("\r\n");

        // 解密
        byte[] decrypt = decrypt(encrypt, password);
        System.out.println("解密后的内容：" + new String(decrypt));
        */
    }

    public static byte[] encrypt(String content, String password) {
        /*
        try {
            //需要加密的内容

            //密钥
            KeyGenerator kgen = KeyGenerator.getInstance("AES");// 创建AES的Key生产者
            kgen.init(128, new SecureRandom(password.getBytes()));// 利用用户密码作为随机数初始化出

            //加密没关系，SecureRandom是生成安全随机数序列，password.getBytes()是种子，只要种子相同，序列就一样，所以解密只要有password就行
            SecretKey secretKey = kgen.generateKey();// 根据用户密码，生成一个密钥
            byte[] enCodeFormat = secretKey.getEncoded();// 返回基本编码格式的密钥，如果此密钥不支持编码，则返回// null。

            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");// 转换为AES专用密钥

            Cipher cipher = Cipher.getInstance("AES");// 创建密码器
            byte[] byteContent = content.getBytes("utf-8");
            cipher.init(Cipher.ENCRYPT_MODE, key);// 初始化为加密模式的密码器

            return cipher.doFinal(byteContent);// 加密
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }*/
        return null;
    }

    public static byte[] decrypt(byte[] content, String password) {
        /*
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");// 创建AES的Key生产者
            kgen.init(128, new SecureRandom(password.getBytes()));
            SecretKey secretKey = kgen.generateKey();// 根据用户密码，生成一个密钥
            byte[] enCodeFormat = secretKey.getEncoded();// 返回基本编码格式的密钥
            SecretKeySpec key = new SecretKeySpec(enCodeFormat, "AES");// 转换为AES专用密钥
            Cipher cipher = Cipher.getInstance("AES");// 创建密码器
            cipher.init(Cipher.DECRYPT_MODE, key);// 初始化为解密模式的密码器
            return cipher.doFinal(content); // 明文
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }*/
        return null;
    }
}