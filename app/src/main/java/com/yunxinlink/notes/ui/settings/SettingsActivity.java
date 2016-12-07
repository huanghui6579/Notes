package com.yunxinlink.notes.ui.settings;


import android.net.Uri;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.ui.BaseActivity;

/**
 * 设置主界面
 * @author huanghui1
 * @update 2016/8/24 17:09
 * @version: 1.0.0
 */
public class SettingsActivity extends BaseActivity implements SettingsFragment.OnSettingsFragmentInteractionListener {

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
}
