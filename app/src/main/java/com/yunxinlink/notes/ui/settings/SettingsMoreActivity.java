package com.yunxinlink.notes.ui.settings;

import android.os.Bundle;

import com.yunxinlink.notes.R;

/**
 * 安全设置的界面，主要设置查看笔记的密码
 * @author huanghui1
 * @update 2016/8/25 19:28
 * @version: 1.0.0
 */
public class SettingsMoreActivity extends AppCompatPreferenceActivity implements SettingsMoreFragment.OnThemeFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_more);

        setupActionBar(R.id.toolbar);

        setListDividerHeight();
    }

}
