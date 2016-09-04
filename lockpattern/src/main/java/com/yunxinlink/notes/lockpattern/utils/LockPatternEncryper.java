package com.yunxinlink.notes.lockpattern.utils;

import android.content.Context;

import com.socks.library.KLog;
import com.yunxinlink.notes.lockpattern.widget.LockPatternUtils;
import com.yunxinlink.notes.lockpattern.widget.LockPatternView;

import java.util.List;

import haibison.android.underdogs.NonNull;

/**
 * 图案解锁的加解密
 * @author tiger
 * @version 1.0.0
 * @update 2016/9/3 13:50
 */
public class LockPatternEncryper implements Encrypter {
    /**
     * 图案密码锁的加解密密钥
     * @hide
     */
    private static final String LOCK_PATTERN_KEY = "lock43hj8y3y4y48";
    @Override
    public char[] encrypt(@NonNull Context context, @NonNull List<LockPatternView.Cell> pattern) {
        //将cell转换成string
        String patternStr = LockPatternUtils.patternToString(pattern);
        String result = EncryptionUtil.AESEncrypt(LOCK_PATTERN_KEY, patternStr);
        if (result == null) {
            return null;
        }
        KLog.d("LockPattern Encryper encrypt result:" + result);
        return result.toCharArray();
    }

    @Override
    public List<LockPatternView.Cell> decrypt(@NonNull Context context, @NonNull char[] encryptedPattern) {
        if (encryptedPattern == null || encryptedPattern.length == 0) {
            return null;
        }
        String content = new String(encryptedPattern);
        String result = EncryptionUtil.AESDecrypt(LOCK_PATTERN_KEY, content);
        if (result == null) {
            return null;
        }
        KLog.d("LockPattern Encryper decrypt result:" + result);
        return LockPatternUtils.stringToPattern(result);
    }

    @Override
    public String encrypt(Context context, String digital) {
        if (digital == null) {
            return null;
        }
        String result = EncryptionUtil.AESEncrypt(LOCK_PATTERN_KEY, digital);
        KLog.d("LockDigitalPattern Encryper encrypt result:" + result);
        return result;
    }

    @Override
    public String decrypt(Context context, String encryptedDigital) {
        if (encryptedDigital == null) {
            return null;
        }
        String result = EncryptionUtil.AESDecrypt(LOCK_PATTERN_KEY, encryptedDigital);
        KLog.d("LockDigitalPattern Encryper decrypt result:" + result);
        return result;
    }
}
