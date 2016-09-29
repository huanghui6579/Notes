package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.db.Provider;
import com.yunxinlink.notes.db.observer.ContentObserver;
import com.yunxinlink.notes.db.observer.Observable;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.persistent.UserManager;
import com.yunxinlink.notes.ui.AccountEditActivity;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.TimeUtil;

/**
 * 账号设置界面
 * @author huanghui1
 * @update 2016/9/29 16:48
 * @version: 1.0.0
 */
public class SettingsAccountFragment extends BasePreferenceFragment implements Preference.OnPreferenceClickListener {

    private OnAccountFragmentInteractionListener mListener;
    
    private UserObserver mUserObserver;
    
    private Preference mAccountNamePreference;
    private Preference mLastSyncPreference;

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
    public static SettingsAccountFragment newInstance() {
        SettingsAccountFragment fragment = new SettingsAccountFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_account);

        mAccountNamePreference = findPreference(getString(R.string.settings_key_account_name));
        mAccountNamePreference.setOnPreferenceClickListener(this);

        mLastSyncPreference = findPreference(getString(R.string.settings_key_account_last_sync_time));

        Preference logoutPreference = findPreference(getString(R.string.settings_key_account_logout));
        logoutPreference.setOnPreferenceClickListener(this);

        //注册观察者
        registerObserver();

        User user = getApp().getCurrentUser();

        //显示用户信息
        showAccountInfo(user);
    }

    /**
     * 显示用户信息
     * @param user
     */
    private void showAccountInfo(User user) {
        if (user != null) {
            String nickname = user.getNickname();
            if (!TextUtils.isEmpty(nickname)) {
                mAccountNamePreference.setTitle(nickname);
            }
            String account = user.getAccount();
            if (!TextUtils.isEmpty(account)) {
                mAccountNamePreference.setSummary(account);
            }
            Long lastSyncTime = user.getLastSyncTime();
            if (lastSyncTime != null && lastSyncTime > 0) {
                String time = TimeUtil.formatNoteTime(lastSyncTime);
                mLastSyncPreference.setTitle(time);
            }
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnAccountFragmentInteractionListener) {
            mListener = (OnAccountFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAccountFragmentInteractionListener");
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
        Intent intent = null;
        if (getString(R.string.settings_key_account_name).equals(key)) {  //账号的名称，则进入账号信息修改界面
            intent = new Intent(getActivity(), AccountEditActivity.class);
            startActivity(intent);
        } else if (getString(R.string.settings_key_account_logout).equals(key)) {   //退出登录
            showLogoutDialog();
        }
        return false;
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
    }
}
