package com.yunxinlink.notes.ui.settings;


import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.State;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.AuthorityActivity;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

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

    @Override
    public void onBindEmail(int resId) {
        mHandler.post(new NoteTask(resId) {
            @Override
            public void run() {
                int id = (int) params[0];
                if (id != 0) {
                    SystemUtil.makeShortToast(id);
                }
            }
        });
    }

    /**
     * 注销登录
     */
    private void doLogout() {
        NoteUtil.finishAll(this);
        ((NoteApplication) getApplication()).clearCache();
        //跳转到登录界面
        mHandler.postDelayed(new NoteTask() {
            @Override
            public void run() {
                Intent intent = new Intent(SettingsAccountActivity.this, AuthorityActivity.class);
                intent.putExtra(AuthorityActivity.ARG_RELOGIN, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 10);
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        KLog.d(TAG, "onPreferenceStartScreen settings account activity");
        return false;
    }

    /**
     * 退出登录的后台任务
     */
    class LogoutTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            User user = getCurrentUser();
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
