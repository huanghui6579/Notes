package com.yunxinlink.notes.ui.settings;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.BaseActivity;

/**
 * 安全设置的界面，主要设置查看笔记的密码
 * @author huanghui1
 * @update 2016/8/25 19:28
 * @version: 1.0.0
 */
public class SettingsMoreActivity extends BaseActivity implements SettingsMoreFragment.OnThemeFragmentInteractionListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    @Override
    protected int getContentView() {
        return R.layout.activity_settings_more;
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initView() {

    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        KLog.d(TAG, "onPreferenceStartScreen settings more activity");
        SettingsMoreFragment fragment = SettingsMoreFragment.newInstance(pref.getKey());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.main_frame, fragment, pref.getKey());
        transaction.addToBackStack(pref.getKey());
        transaction.commit();
        return true;
    }
}
