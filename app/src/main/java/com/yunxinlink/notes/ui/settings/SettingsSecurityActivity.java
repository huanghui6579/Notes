package com.yunxinlink.notes.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.LockInfo;
import com.yunxinlink.notes.lock.LockType;
import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;
import com.yunxinlink.notes.ui.BaseActivity;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 安全设置的界面，主要设置查看笔记的密码
 * @author huanghui1
 * @update 2016/8/25 19:28
 * @version: 1.0.0
 */
public class SettingsSecurityActivity extends BaseActivity implements SettingsSecurityFragment.OnSecurityFragmentInteractionListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    
    public static final int REQ_CREATE_DIGITAL = 10;
    public static final int REQ_COMPARE_DIGITAL = 11;
    public static final int REQ_CREATE_PATTERN = 12;
    public static final int REQ_COMPARE_PATTERN = 13;
    public static final int REQ_MODIFY_PATTERN = 14;
    public static final int REQ_MODIFY_DIGITAL = 15;

    @Override
    protected int getContentView() {
        return R.layout.activity_settings_security;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        KLog.d(TAG, "activity onActivityResult");
        
        LockType lockType = null;
        boolean hasLock = false;
        int tryCount = 0;
        int maxTryCount = 0;
        Intent intent = null;
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
//                    lockType = LockType.PATTERN;
//                    hasLock = true;
                    saveLockState(false);
                    intent = LockPatternActivity.IntentBuilder
                            .newPatternCreator(this)
                            .build();
                    intent.putExtra(LockPatternActivity.EXTRA_HAS_LOCK_CONTROLLER, true);
                    intent.putExtra(LockPatternActivity.EXTRA_TEXT_INFO, R.string.settings_pwd_modify_pattern_new_title);
                    startActivityForResult(intent, SettingsSecurityActivity.REQ_CREATE_PATTERN);
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
//                    lockType = LockType.DIGITAL;
//                    hasLock = true;
                    saveLockState(false);
                    intent = LockDigitalActivity.IntentBuilder
                            .newPatternCreator(this)
                            .build();
                    intent.putExtra(LockDigitalActivity.EXTRA_HAS_LOCK_CONTROLLER, true);
                    intent.putExtra(LockDigitalActivity.EXTRA_TEXT_INFO, R.string.settings_pwd_modify_digital_new_title);
                    startActivityForResult(intent, SettingsSecurityActivity.REQ_CREATE_DIGITAL);
                } else {    //面输入次数超过5次，稍后再试
                    if (maxTryCount != 0 && tryCount >= maxTryCount) {
                        SystemUtil.makeShortToast(R.string.pwd_error);
                    }
                }
                break;
        }
        KLog.d(TAG, "lockType:" + lockType + ", hasLock" + hasLock);
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
        //TODO 获取fragment
        SettingsSecurityFragment securityFragment = (SettingsSecurityFragment) getSupportFragmentManager().findFragmentByTag("SettingsFragment");
        if (securityFragment != null) {
            securityFragment.saveSecurityPreference(hasLock);
        }
    }

    /**
     * 保存目前app的解锁状态
     * @param isLocking 是否锁定，true：锁定了
     */
    public void saveLockState(boolean isLocking) {
        updateLockState(isLocking);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        KLog.d(TAG, "onPreferenceStartScreen settings security activity");
        SettingsSecurityFragment fragment = SettingsSecurityFragment.newInstance(pref.getKey());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.main_frame, fragment, pref.getKey());
        transaction.addToBackStack(pref.getKey());
        transaction.commit();
        return true;
    }
}
