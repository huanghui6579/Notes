package com.yunxinlink.notes.ui.settings;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.State;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.AuthorityActivity;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;

/**
 * 用户账号的设置界面
 * @author huanghui1
 * @update 2016/9/29 16:30
 * @version: 1.0.0
 */
public class SettingsAccountActivity extends AppCompatPreferenceActivity implements SettingsAccountFragment.OnAccountFragmentInteractionListener {
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_account);

        setupActionBar(R.id.toolbar);

        setListDividerHeight();
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || SettingsAccountFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void logout() {
        new LogoutTask().execute();
    }
    
    private void doLogout() {
        NoteUtil.finishAll(this);
        ((NoteApplication) getApplication()).clearCache();
        //跳转到登录界面
        mHandler.postDelayed(new NoteTask() {
            @Override
            public void run() {
                Intent intent = new Intent(SettingsAccountActivity.this, AuthorityActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 10);
    }

    /**
     * 退出登录的后台任务
     */
    class LogoutTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            User user = ((NoteApplication) getApplication()).getCurrentUser();
            if (user != null) {
                KLog.d(TAG, "settings account logout task set user state to offline");
                User u = new User();
                u.setId(user.getId());
                u.setState(State.OFFLINE);
                UserManager.getInstance().update(u);
            } else {
                KLog.d(TAG, "settings account logout task user is null");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            doLogout();
        }
    }
}
