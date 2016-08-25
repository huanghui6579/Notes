package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.util.NoteUtil;

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
                    return false;
                }
            }
        }
        return true;
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
                        switch (which) {
                            case 0: //数字密码
                                break;
                            case 1: //图案密码
                                break;
                        }
                    }
                })
                .setPositiveButton(android.R.string.cancel, null)
                .show();
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
