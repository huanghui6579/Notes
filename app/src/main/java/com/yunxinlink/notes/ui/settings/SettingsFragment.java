package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.NoteApplication;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.AuthorityActivity;
import com.yunxinlink.notes.ui.BaseHandler;
import com.yunxinlink.notes.util.Constants;

/**
 * 设置界面
 * @author huanghui1
 * @update 2016/8/24 11:14
 * @version: 1.0.0
 */
public class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    private OnSettingsFragmentInteractionListener mListener;

    //用户信息的观察者
    private UserObserver mUserObserver;

    private Handler mHandler = new MyHandler(this);

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //注册观察者
        registerObserver();
        
        addPreferencesFromResource(R.xml.pref_main);

        Preference accountPreference = findPreference(getString(R.string.settings_key_account_in));
        accountPreference.setOnPreferenceClickListener(this);

        User user = getApp().getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getAccount())) {
            String account = user.getAccount();
            showAccount(account);
        } else {
            KLog.d(TAG, "settings main fragment user is null or account is empty:" + user);
        }
        
        
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSettingsFragmentInteractionListener) {
            mListener = (OnSettingsFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRegisterFragmentInteractionListener");
        }
    }

    /**
     * 注册观察者
     */
    private void registerObserver() {
        if (mUserObserver == null) {
            mUserObserver = new UserObserver(mHandler);
        }
        UserManager.getInstance().addObserver(mUserObserver);
    }

    /**
     * 注销观察者
     */
    private void unregisterObserver() {
        if (mUserObserver != null) {
            UserManager.getInstance().removeObserver(mUserObserver);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        //注销观察者
        unregisterObserver();
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (getString(R.string.settings_key_account_in).equals(key)) {  //账号的入口
            //获取账号信息，如果没有，则需要登录
            NoteApplication app = getApp();
            User user = app.getCurrentUser();
            Intent intent = null;
            if (user != null && user.checkId()) {   //用户可用
                intent = new Intent(getActivity(), SettingsAccountActivity.class);
                startActivity(intent);
            } else {
                intent = new Intent(getActivity(), AuthorityActivity.class);
                startActivity(intent);
            }
        }
        return false;
    }

    /**
     * 处理显示账号信息
     * @param account
     */
    private void handleShowAccount(String account) {
        Message msg = mHandler.obtainMessage();
        msg.what = Constants.MSG_SUCCESS;
        msg.obj = account;
        mHandler.sendMessage(msg);
    }
    
    /**
     * 显示用户账号信息
     * @param account
     */
    private void showAccount(String account) {
        if (mListener == null) {
            KLog.d(TAG, "settings fragment not attach to the activity");
            return;
        }
        if (TextUtils.isEmpty(account)) {   //没有账号
            account = getString(R.string.settings_account_summary);
        }
        findPreference(getString(R.string.settings_key_account_in)).setSummary(account);
    }

    /**
     * 账号的观察者
     */
    class UserObserver extends ContentObserver {

        public UserObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.UserColumns.NOTIFY_FLAG:  //用户信息变化了
                    switch (notifyType) {
                        case ADD:   //用户登录了
                        case UPDATE:    //用户更新了
                        case REFRESH:   //用户数据加载完毕了，则刷新界面
                            User user = (User) data;
                            if (user == null) {
                                user = getApp().getCurrentUser();
                            }
                            if (user == null) {
                                KLog.d(TAG, "settings main fragment user observer update but user is null");
                                return;
                            }
                            String account = user.getAccount();
                            if (TextUtils.isEmpty(account)) {
                                KLog.d(TAG, "settings main fragment user observer update but account is null:" + user);
                                return;
                            }
                            handleShowAccount(account);
                            break;
                        case REMOVE:    //用户注销了
                            handleShowAccount(null);
                            break;
                    }
                    break;
            }
        }
    }

    static class MyHandler extends BaseHandler<SettingsFragment> {

        public MyHandler(SettingsFragment target) {
            super(target);
        }

        @Override
        public void handleMessage(Message msg) {
            SettingsFragment target = mTarget.get();
            if (target != null) {
                switch (msg.what) {
                    case Constants.MSG_SUCCESS:
                        target.showAccount((String) msg.obj);
                        break;
                }
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSettingsFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
