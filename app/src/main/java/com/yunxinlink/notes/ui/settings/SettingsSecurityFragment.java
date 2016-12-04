package com.yunxinlink.notes.ui.settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.account.AccountBinder;
import com.yunxinlink.notes.lock.LockType;
import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;
import com.yunxinlink.notes.lockpattern.utils.AlpSettings;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.NoteTask;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SettingsUtil;
import com.yunxinlink.notes.util.SystemUtil;

/**
 * 安全密码设置界面
 * @author huanghui1
 * @update 2016/8/25 19:34
 * @version: 1.0.0
 */
public class SettingsSecurityFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener, AccountBinder.OnBindPostedListener {
    // TODO: Rename parameter arguments, choose names that match

    private OnSecurityFragmentInteractionListener mListener;
    
    private ProgressDialog mProgressDialog;

    private Handler mHandler = new Handler();

    public SettingsSecurityFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsSecurityFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsSecurityFragment newInstance(String rootKey) {
        SettingsSecurityFragment fragment = new SettingsSecurityFragment();
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
        addPreferencesFromResource(R.xml.pref_security);
        //设置监听
        bindPreferenceChangeListener(findPreference(getString(R.string.settings_key_security_password)), this);
        //设置监听
        bindPreferenceChangeListener(findPreference(getString(R.string.settings_key_security_show_widget)), this);

        findPreference(getString(R.string.settings_key_security_modify_password)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                compareSecurity(true);
                return true;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!SystemUtil.hasSdkV23()) {
            attachCompat(activity);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (SystemUtil.hasSdkV23()) {
            attachCompat(context);
        }
    }

    @Override
    protected void attachCompat(Context context) {
        if (context instanceof OnSecurityFragmentInteractionListener) {
            mListener = (OnSecurityFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnRegisterFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        KLog.d("----preference----" + preference + "--newValue----" + newValue + "--class---" + newValue.getClass());
        String key = preference.getKey();
        if (getString(R.string.settings_key_security_password).equals(key)) {   //是否设置了安全密码
            if (newValue instanceof Boolean) {
                boolean enablePwd = (boolean) newValue;
                if (enablePwd) {
                    //先检查是否有绑定手机号或者邮箱，主要用于密码重置
                    if (checkBind()) {
                        //显示选择密码类型的提示框
                        chosePwdType();
                    } else {
                        bindEmail();
                        KLog.d(TAG, "user has not bind account and will bind first");
                    }
                } else {    //取消密码锁
                    //1、先校验密码
                    if (!compareSecurity(false)) {   //没有密码
                        //显示选择密码类型的提示框
                        chosePwdType();
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 绑定邮箱
     */
    private void bindEmail() {
        AccountBinder accountBinder = new AccountBinder();
        accountBinder.bindAccount(getActivity(), true, this);
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
     * 检查是否有绑定手机号或者邮箱
     * @return
     */
    private boolean checkBind() {
        User user = getApp().getCurrentUser();
        if (user == null) { //当前用户没有登录，则直接根据输入的账号和密码校验
            return false;
        } else {
            return user.hasBindAccount();
        }
    }

    /**
     * 校验密码
     * @param isModify 是否是修改密码事的校验密码
     */
    private boolean compareSecurity(boolean isModify) {
        LockType lockType = SettingsUtil.getLockType(getActivity());
        if (lockType != null) { //有密码
            int requestCode = 0;
            Intent intent = null;
            switch (lockType) {
                case DIGITAL:
                    intent = LockDigitalActivity.IntentBuilder
                            .newPatternComparator(getActivity())
                            .build();
                    if (isModify) {
                        requestCode = SettingsSecurityActivity.REQ_MODIFY_DIGITAL;
                        intent.putExtra(LockDigitalActivity.EXTRA_TEXT_INFO, R.string.settings_pwd_modify_digital_title);
                    } else {
                        requestCode = SettingsSecurityActivity.REQ_COMPARE_DIGITAL;
                    }
                    intent.putExtra(LockDigitalActivity.EXTRA_IS_MODIFY, true);
                    getActivity().startActivityForResult(intent, requestCode);
                    break;
                case PATTERN:
                    intent = LockPatternActivity.IntentBuilder
                            .newPatternComparator(getActivity())
                            .build();
                    if (isModify) {
                        requestCode = SettingsSecurityActivity.REQ_MODIFY_PATTERN;
                        intent.putExtra(LockPatternActivity.EXTRA_TEXT_INFO, R.string.settings_pwd_modify_pattern_title);
                    } else {
                        requestCode = SettingsSecurityActivity.REQ_COMPARE_PATTERN;
                    }
                    intent.putExtra(LockPatternActivity.EXTRA_IS_MODIFY, true);
                    getActivity().startActivityForResult(intent, requestCode);
                    break;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 显示选择密码类型的提示框
     */
    private void chosePwdType() {
        AlertDialog.Builder builder = NoteUtil.buildDialog(getActivity());
        builder.setTitle(R.string.settings_pwd_type_title)
                .setSingleChoiceItems(R.array.pwd_type, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        AlpSettings.Security.setAutoSavePattern(getActivity(), true);
                        Intent intent = null;
                        switch (which) {
                            case 0: //数字密码
                                intent = LockDigitalActivity.IntentBuilder
                                        .newPatternCreator(getActivity())
                                        .build();
                                intent.putExtra(LockDigitalActivity.EXTRA_HAS_LOCK_CONTROLLER, true);
                                getActivity().startActivityForResult(intent, SettingsSecurityActivity.REQ_CREATE_DIGITAL);
                                break;
                            case 1: //图案密码
                                intent = LockPatternActivity.IntentBuilder
                                        .newPatternCreator(getActivity())
                                        .build();
                                intent.putExtra(LockPatternActivity.EXTRA_HAS_LOCK_CONTROLLER, true);
                                getActivity().startActivityForResult(intent, SettingsSecurityActivity.REQ_CREATE_PATTERN);
                                break;
                        }
                    }
                })
                .setPositiveButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 保存密码锁的开关
     * @param isEnabled 密码锁是否打开
     */
    public void saveSecurityPreference(boolean isEnabled) {
        SwitchPreference switchPreference = (SwitchPreference) findPreference(getString(R.string.settings_key_security_password));
        switchPreference.setChecked(isEnabled);
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
            mHandler.post(new NoteTask(success, resId) {
                @Override
                public void run() {
                    boolean result = (boolean) params[0];
                    if (result) {
                        //显示选择密码类型的提示框
                        chosePwdType();
                    }
                    int id = (int) params[1];
                    if (id != 0) {
                        SystemUtil.makeShortToast(id);
                    }
                }
            });
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
    public interface OnSecurityFragmentInteractionListener {
    }
}
