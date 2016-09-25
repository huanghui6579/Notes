package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;

import com.socks.library.KLog;
import com.yunxinlink.notes.R;
import com.yunxinlink.notes.receiver.SystemReceiver;
import com.yunxinlink.notes.util.Constants;
import com.yunxinlink.notes.util.NoteUtil;
import com.yunxinlink.notes.util.SettingsUtil;

/**
 * 安全密码设置界面
 * @author huanghui1
 * @update 2016/8/25 19:34
 * @version: 1.0.0
 */
public class SettingsMoreFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener {
    // TODO: Rename parameter arguments, choose names that match

    private OnThemeFragmentInteractionListener mListener;

    public static final int REQ_ADD_NOTE = 10;
    public static final int REQ_SHOW_MAIN = 11;

    public SettingsMoreFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsSecurityFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsMoreFragment newInstance() {
        SettingsMoreFragment fragment = new SettingsMoreFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_more);
        //设置监听
        bindPreferenceChangeListener(findPreference(getString(R.string.settings_key_more_shortcut)), this);

        findPreference(getString(R.string.settings_key_more_theme)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                changeTheme();
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnThemeFragmentInteractionListener) {
            mListener = (OnThemeFragmentInteractionListener) context;
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
        if (getString(R.string.settings_key_more_shortcut).equals(key)) {   //是否在状态栏中打开快捷方式
            if (newValue instanceof Boolean) {
                boolean enablePwd = (boolean) newValue;
                if (enablePwd) {
                    NoteUtil.createNotificationShortcut(getActivity(), Constants.ID_NOTIFY_CREATE_SHORTCUT);
                } else {
                    NoteUtil.cancelNotificationShortcut(getActivity(), Constants.ID_NOTIFY_CREATE_SHORTCUT);
                }
            }
        }
        return true;
    }

    /**
     * 切换主题
     */
    private void changeTheme() {
        AlertDialog.Builder builder = NoteUtil.buildDialog(getActivity());
        final int oldMode = SettingsUtil.getDefaultNightMode(getActivity());
        int index = 0;
        if (oldMode == AppCompatDelegate.MODE_NIGHT_YES) {  //夜间模式
            index = 1;
        }
        final int defaultIndex = index;
        builder.setTitle(R.string.settings_more_theme_title)
                .setSingleChoiceItems(R.array.theme_type, index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == defaultIndex) {
                            return;
                        }
                        int mode = -1;
                        switch (which) {
                            case 0: //默认主题
                                mode = AppCompatDelegate.MODE_NIGHT_NO;
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                                break;
                            case 1: //夜间主题
                                mode = AppCompatDelegate.MODE_NIGHT_YES;
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                                break;
                        }
                        SettingsUtil.setDefaultNightMode(getActivity(), mode);
                        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
                        localBroadcastManager.sendBroadcast(new Intent(SystemReceiver.ACTION_THEME_CHANGE));
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
    public interface OnThemeFragmentInteractionListener {
    }
}
