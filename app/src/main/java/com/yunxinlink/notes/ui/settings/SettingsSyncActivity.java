package com.yunxinlink.notes.ui.settings;


import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.yunxinlink.notes.R;

/**
 * 同步选项设置界面
 * @author huanghui1
 * @update 2016/8/24 17:09
 * @version: 1.0.0
 */
public class SettingsSyncActivity extends AppCompatPreferenceActivity implements SettingsSyncFragment.OnSettingsSyncFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_settings_sync);

        setupActionBar(R.id.toolbar);

        setListDividerHeight();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsSyncFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        
    }
}
