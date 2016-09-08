package com.yunxinlink.notes.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.LockInfo;
import com.yunxinlink.notes.lock.LockType;
import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 安全设置的界面，主要设置查看笔记的密码
 * @author huanghui1
 * @update 2016/8/25 19:28
 * @version: 1.0.0
 */
public class SettingsSecurityActivity extends AppCompatPreferenceActivity implements SettingsSecurityFragment.OnSecurityFragmentInteractionListener {
    
    public static final int REQ_CREATE_DIGITAL = 10;
    public static final int REQ_COMPARE_DIGITAL = 11;
    public static final int REQ_CREATE_PATTERN = 12;
    public static final int REQ_COMPARE_PATTERN = 13;
    public static final int REQ_MODIFY_PATTERN = 14;
    public static final int REQ_MODIFY_DIGITAL = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_security);

        setupActionBar(R.id.toolbar);

        setListDividerHeight();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        KLog.d(TAG, "activity onActivityResult");
        
        LockType lockType = null;
        boolean hasLock = false;
        int tryCount = 0;
        int maxTryCount = 0;
        switch (requestCode) {
            case REQ_CREATE_DIGITAL:    //创建数字密码
                if (resultCode == RESULT_OK) {  //成功
                    lockType = LockType.DIGITAL;
                    hasLock = true;
                }
                break;
            case REQ_CREATE_PATTERN:    //创建图案密码
                if (resultCode == RESULT_OK) {  //成功
                    final char[] pattern = data.getCharArrayExtra(LockPatternActivity.EXTRA_PATTERN);
                    if (pattern != null && pattern.length > 0) {
                        lockType = LockType.PATTERN;
                        hasLock = true;
                        String text = new String(pattern);
                        KLog.d("onActivityResult create text success:" + text);
                    }
                }
                break;
            case REQ_COMPARE_PATTERN:   //校验图案密码，用于取消密码的校验
                if (data != null) {
                    tryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0);
                    maxTryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_MAX_COUNT, 0);
                }
                KLog.d("onActivityResult compare pattern success try count:" + tryCount);
                if (resultCode == RESULT_OK) {  //成功
                    lockType = LockType.PATTERN;
                    hasLock = false;
                } else {    //面输入次数超过5次，稍后再试
                    if (maxTryCount != 0 && tryCount >= maxTryCount) {
                        SystemUtil.makeShortToast(R.string.pwd_error);
                    }
                }
                break;
            case REQ_COMPARE_DIGITAL:   //校验数字密码，用于取消密码的校验
                if (data != null) {
                    tryCount = data.getIntExtra(LockDigitalActivity.EXTRA_RETRY_COUNT, 0);
                    maxTryCount = data.getIntExtra(LockDigitalActivity.EXTRA_RETRY_MAX_COUNT, 0);
                }
                KLog.d("onActivityResult compare digital success try count:" + tryCount);
                if (resultCode == RESULT_OK) {  //成功
                    lockType = LockType.DIGITAL;
                    hasLock = false;
                } else {    //面输入次数超过5次，稍后再试
                    if (maxTryCount != 0 && tryCount >= maxTryCount) {
                        SystemUtil.makeShortToast(R.string.pwd_error);
                    }
                }
                break;
            case REQ_MODIFY_PATTERN:    //修改图案密码时的校验
                if (data != null) {
                    tryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_COUNT, 0);
                    maxTryCount = data.getIntExtra(LockPatternActivity.EXTRA_RETRY_MAX_COUNT, 0);
                }
                KLog.d("onActivityResult compare modify pattern success try count:" + tryCount);
                if (resultCode == RESULT_OK) {  //成功
                    lockType = LockType.PATTERN;
                    hasLock = false;
                    LockPatternActivity.IntentBuilder
                            .newPatternCreator(this)
                            .startForResult(this, SettingsSecurityActivity.REQ_CREATE_PATTERN);
                } else {    //面输入次数超过5次，稍后再试
                    if (maxTryCount != 0 && tryCount >= maxTryCount) {
                        SystemUtil.makeShortToast(R.string.pwd_error);
                    }
                }
                break;
            case REQ_MODIFY_DIGITAL:    //修改数字密码时的校验
                if (data != null) {
                    tryCount = data.getIntExtra(LockDigitalActivity.EXTRA_RETRY_COUNT, 0);
                    maxTryCount = data.getIntExtra(LockDigitalActivity.EXTRA_RETRY_MAX_COUNT, 0);
                }
                KLog.d("onActivityResult compare modify digital success try count:" + tryCount);
                if (resultCode == RESULT_OK) {  //成功
                    lockType = LockType.DIGITAL;
                    hasLock = false;
                    LockDigitalActivity.IntentBuilder
                            .newPatternCreator(this)
                            .startForResult(this, SettingsSecurityActivity.REQ_CREATE_DIGITAL);
                } else {    //面输入次数超过5次，稍后再试
                    if (maxTryCount != 0 && tryCount >= maxTryCount) {
                        SystemUtil.makeShortToast(R.string.pwd_error);
                    }
                }
                break;
        }
        saveSecurityType(lockType, hasLock);
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 保存密码锁的类型
     * @param lockType 密码锁的类型,{@link LockType#DIGITAL}和{@link LockType#PATTERN}
     * @param hasLock 密码锁是否打开，true，打开                
     */
    public void saveSecurityType(LockType lockType, boolean hasLock) {
        if (lockType == null) {
            return;
        }
        SharedPreferences preferences = SystemUtil.getDefaultPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.settings_key_security_type), lockType.getType());
        editor.putBoolean(getString(R.string.settings_key_security_password), hasLock);
        editor.apply();

        LockInfo lockInfo = new LockInfo();
        lockInfo.setLockType(lockType);
        lockInfo.setHasLock(hasLock);
        
        updateLockInfo(lockInfo);
        
        KLog.d(TAG, "update security type:" + lockInfo);
        
        SettingsSecurityFragment securityFragment = (SettingsSecurityFragment) getFragmentManager().findFragmentByTag("SettingsFragment");
        if (securityFragment != null) {
            securityFragment.saveSecurityPreference(hasLock);
        }
    }
}
