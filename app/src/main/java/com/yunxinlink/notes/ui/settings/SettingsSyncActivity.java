package com.yunxinlink.notes.ui.settings;


import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.NoteManager;
import com.yunxinlink.notes.util.Constants;

import java.lang.ref.WeakReference;

/**
 * 同步选项设置界面
 * @author huanghui1
 * @update 2016/8/24 17:09
 * @version: 1.0.0
 */
public class SettingsSyncActivity extends AppCompatPreferenceActivity implements SettingsSyncFragment.OnSettingsSyncFragmentInteractionListener {
    
    //观察者
    private ContentObserver mContentObserver;
    
    private Handler mHandler = new MyHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_settings_sync);

        setupActionBar(R.id.toolbar);

        setListDividerHeight();

        registerObserver();
    }

    /**
     * 注册观察者
     */
    private void registerObserver() {
        if (mContentObserver == null) {
            mContentObserver = new NoteContentObserver(mHandler);
        }
        NoteManager.getInstance().addObserver(mContentObserver);
    }

    /**
     * 注销观察者
     */
    private void unregisterObserver() {
        if (mContentObserver != null) {
            NoteManager.getInstance().removeObserver(mContentObserver);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterObserver();
        super.onDestroy();
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

    /**
     * 获取当前的fragment
     * @return
     */
    private SettingsSyncFragment getFragment() {
        return (SettingsSyncFragment) getFragmentManager().findFragmentByTag("SettingsSyncFragment");
    }

    /**
     * 刷新界面
     */
    private void refresh() {
        mHandler.sendEmptyMessage(Constants.MSG_SUCCESS);
    }

    /**
     * 执行刷新界面的操作
     */
    private void doRefresh() {
        SettingsSyncFragment fragment = getFragment();
        User user = getCurrentUser();
        if (fragment != null) {
            fragment.refresh(user);
        } else {
            KLog.d(TAG, "setting sync activity get refresh get fragment null or user is null or user not available");
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<SettingsSyncActivity> mTarget;

        public MyHandler(SettingsSyncActivity target) {
            mTarget = new WeakReference<>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            SettingsSyncActivity target = mTarget.get();
            if (target != null) {
                target.doRefresh();
            }
        }
    }

    /**
     * 笔记的观察者
     */
    class NoteContentObserver extends ContentObserver {

        NoteContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.NOTIFY_FLAG:  //全局的通知
                    switch (notifyType) {
                        case DONE:  //同步完成
                            refresh();
                            break;
                    }
                    break;
            }
        }
    }
}
