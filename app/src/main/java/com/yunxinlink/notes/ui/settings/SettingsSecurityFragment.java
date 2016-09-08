package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.lock.LockType;
import com.yunxinlink.notes.lock.ui.LockDigitalActivity;
import com.yunxinlink.notes.lock.ui.LockPatternActivity;
import com.yunxinlink.notes.lockpattern.utils.AlpSettings;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SettingsUtil;

/**
 * 安全密码设置界面
 * @author huanghui1
 * @update 2016/8/25 19:34
 * @version: 1.0.0
 */
public class SettingsSecurityFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener {
    // TODO: Rename parameter arguments, choose names that match

    private OnSecurityFragmentInteractionListener mListener;

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
    public static SettingsSecurityFragment newInstance() {
        SettingsSecurityFragment fragment = new SettingsSecurityFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSecurityFragmentInteractionListener) {
            mListener = (OnSecurityFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
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
                    //显示选择密码类型的提示框
                    chosePwdType();
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
     * 校验密码
     * @param isModify 是否是修改密码事的校验密码
     */
    private boolean compareSecurity(boolean isModify) {
        LockType lockType = SettingsUtil.getLockType(getActivity());
        if (lockType != null) { //有密码
            int requestCode = 0;
            switch (lockType) {
                case DIGITAL:
                    if (isModify) {
                        requestCode = SettingsSecurityActivity.REQ_MODIFY_DIGITAL;
                    } else {
                        requestCode = SettingsSecurityActivity.REQ_COMPARE_DIGITAL;
                    }
                    LockDigitalActivity.IntentBuilder
                            .newPatternComparator(getActivity())
                            .startForResult(getActivity(), requestCode);
                    break;
                case PATTERN:
                    if (isModify) {
                        requestCode = SettingsSecurityActivity.REQ_MODIFY_PATTERN;
                    } else {
                        requestCode = SettingsSecurityActivity.REQ_COMPARE_PATTERN;
                    }
                    LockPatternActivity.IntentBuilder
                            .newPatternComparator(getActivity())
                            .startForResult(getActivity(), requestCode);
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
                        switch (which) {
                            case 0: //数字密码
                                LockDigitalActivity.IntentBuilder
                                        .newPatternCreator(getActivity())
                                        .startForResult(getActivity(), SettingsSecurityActivity.REQ_CREATE_DIGITAL);
                                break;
                            case 1: //图案密码
                                LockPatternActivity.IntentBuilder
                                        .newPatternCreator(getActivity())
                                        .startForResult(getActivity(), SettingsSecurityActivity.REQ_CREATE_PATTERN);
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
