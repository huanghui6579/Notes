package com.yunxinlink.notes.ui.settings;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.account.AccountBinder;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.AccountEditActivity;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 账号设置界面
 * @author huanghui1
 * @update 2016/9/29 16:48
 * @version: 1.0.0
 */
public class SettingsAccountFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener, AccountBinder.OnBindPostedListener {

    private OnAccountFragmentInteractionListener mListener;
    
    private UserObserver mUserObserver;
    
    private Preference mAccountNamePreference;
    
    private Preference mAccountEmailPreference;
    
    private ProgressDialog mProgressDialog;

    public SettingsAccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsAccountFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsAccountFragment newInstance(String rootKey) {
        SettingsAccountFragment fragment = new SettingsAccountFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, rootKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.pref_account);

        mAccountNamePreference = findPreference(getString(R.string.settings_key_account_name));
        mAccountNamePreference.setOnPreferenceClickListener(this);

        mAccountEmailPreference = findPreference(getString(R.string.settings_key_account_email));
        mAccountEmailPreference.setOnPreferenceClickListener(this);

        Preference logoutPreference = findPreference(getString(R.string.settings_key_account_logout));
        logoutPreference.setOnPreferenceClickListener(this);

        User user = getApp().getCurrentUser();

        //显示用户信息
        showAccountInfo(user);
    }

    /**
     * 显示用户信息
     * @param user
     */
    private void showAccountInfo(User user) {
        boolean canModifyPwd = false;
        if (user != null) {
            canModifyPwd = user.hasBindAccount();
            String nickname = user.getNickname();
            if (!TextUtils.isEmpty(nickname)) {
                mAccountNamePreference.setTitle(nickname);
            }
            String account = user.getAccount();
            if (!TextUtils.isEmpty(account)) {
                mAccountNamePreference.setSummary(account);
            }
            String email = user.getEmail();
            if (!TextUtils.isEmpty(email)) {
                mAccountEmailPreference.setSummary(email);
            }
        }
        Preference preference = findPreference(getString(R.string.settings_key_account_modify_password));
        if (preference != null) {
            if (!canModifyPwd) {
                if (preference.isVisible()) {
                    preference.setVisible(false);
                }
            } else {    //可修改密码
                if (!preference.isVisible()) {
                    preference.setVisible(true);
                }
            }
        }
        
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        attachCompat(context);
    }

    @Override
    protected void attachCompat(Context context) {
        if (context instanceof OnAccountFragmentInteractionListener) {
            mListener = (OnAccountFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAccountFragmentInteractionListener");
        }
        //注册观察者
        registerObserver();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //注销观察者
        unregisterObserver();
        mListener = null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        Intent intent = null;
        if (getString(R.string.settings_key_account_name).equals(key)) {  //账号的名称，则进入账号信息修改界面
            intent = new Intent(getActivity(), AccountEditActivity.class);
            startActivity(intent);
        } else if (getString(R.string.settings_key_account_logout).equals(key)) {   //退出登录
            showLogoutDialog();
        } else if (getString(R.string.settings_key_account_email).equals(key)) {    //绑定的邮箱
            bindEmail();
        }
        return false;
    }

    /**
     * 绑定邮箱
     */
    private void bindEmail() {
        AccountBinder accountBinder = new AccountBinder();
        accountBinder.bindAccount(getActivity(), false, this);
    }

    /**
     * 执行绑定邮箱的操作，需另开线程
     * @param email 电子邮箱
     * @param password 登录的密码             
     */
    private void doBindEmail(String email, String password) {
        if (!NoteUtil.checkEmail(email)) {
            return;
        }
        if (TextUtils.isEmpty(password)) {
            SystemUtil.makeShortToast(R.string.tip_password_is_empty);
            return;
        }
        mProgressDialog = ProgressDialog.show(getActivity(), null, getString(R.string.account_email_bind_ing), true, true);
        AccountBinder accountBinder = new AccountBinder();
        User param = new User();
        param.setEmail(email);
        param.setPassword(password);
        accountBinder.postData(getActivity(), param, this);
    }

    /**
     * 注册观察者
     */
    private void registerObserver() {
        if (mUserObserver == null) {
            mUserObserver = new UserObserver(new Handler());
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

    /**
     * 显示退出登录的对话框
     */
    private void showLogoutDialog() {
        AlertDialog.Builder builder = NoteUtil.buildDialog(getActivity());
        builder.setTitle(R.string.account_logout)
                .setMessage(R.string.account_logout_msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doLogout();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /**
     * 退出登录
     */
    private void doLogout() {
        if (mListener != null) {
            mListener.logout();
        }
    }

    @Override
    public void onBindPosted(String account, String password) {
        doBindEmail(account, password);
    }

    @Override
    public void onBindEnd(boolean success, int resId) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mListener != null) {
            mListener.onBindEmail(resId);
        }
    }

    /**
     * 用户信息的观察者
     */
    private class UserObserver extends ContentObserver {

        public UserObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void update(Observable<?> observable, int notifyFlag, NotifyType notifyType, Object data) {
            switch (notifyFlag) {
                case Provider.UserColumns.NOTIFY_FLAG:  //用户信息改变了
                    switch (notifyType) {
                        case ADD:   //重新设置显示的数据
                        case UPDATE:
                        case REFRESH:
                            if (data != null && data instanceof User) {
                                User user = (User) data;
                                showAccountInfo(user);
                            }
                            break;
                    }
                    break;
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnAccountFragmentInteractionListener {
        /**
         * 退出登录
         */
        void logout();

        /**
         * 绑定邮箱
         * @param resId 结果的字符串
         */
        void onBindEmail(int resId);
    }
}
