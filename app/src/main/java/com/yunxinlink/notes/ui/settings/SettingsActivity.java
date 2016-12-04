package com.yunxinlink.notes.ui.settings;


import android.net.Uri;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.BaseActivity;

/**
 * 设置主界面
 * @author huanghui1
 * @update 2016/8/24 17:09
 * @version: 1.0.0
 */
public class SettingsActivity extends BaseActivity implements SettingsFragment.OnSettingsFragmentInteractionListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    @Override
    protected int getContentView() {
        return R.layout.activity_settings;
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void initView() {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        KLog.d(TAG, "onPreferenceStartScreen settings activity");
        SettingsFragment fragment = SettingsFragment.newInstance(pref.getKey());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.main_frame, fragment, pref.getKey());
        transaction.addToBackStack(pref.getKey());
        transaction.commit();
        return true;
    }
}
