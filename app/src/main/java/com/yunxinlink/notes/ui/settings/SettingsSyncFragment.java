package com.yunxinlink.notes.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.yunxinlink.notes.R;
import com.yunxinlink.notes.model.User;
import com.yunxinlink.notes.util.SystemUtil;
import com.yunxinlink.notes.util.TimeUtil;

/**
 * 同步的设置界面
 * @author huanghui1
 * @update 2016/8/24 17:11
 * @version: 1.0.0
 */
public class SettingsSyncFragment extends BasePreferenceFragment {

    private OnSettingsSyncFragmentInteractionListener mListener;

    public SettingsSyncFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsSyncFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsSyncFragment newInstance(String rootKey) {
        SettingsSyncFragment fragment = new SettingsSyncFragment();
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
        addPreferencesFromResource(R.xml.pref_data_sync);

        refresh(getApp().getCurrentUser());

//        bindPreferenceSummaryToValue(findPreference("sync_note_state"));

//        bindPreferenceChangeListener(findPreference("sync_note_auto"));
//        bindPreferenceChangeListener(findPreference("sync_note_traffic"));
    }

    /**
     * 刷新界面
     * @param user 当前登录的用户
     */
    public void refresh(User user) {
        Preference preference = findPreference(getString(R.string.settings_key_sync_note_state));
        if (preference == null) {
            return;
        }
        long time = user == null ? 0 : user.getLastSyncTime();
        if (user != null && user.isAvailable() && time > 0) {
            preference.setSummary(getString(R.string.settings_sync_state_summary_time, TimeUtil.formatNoteTime(time)));
        } else {
            preference.setSummary(R.string.settings_sync_state_summary);
        }
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
        if (context instanceof OnSettingsSyncFragmentInteractionListener) {
            mListener = (OnSettingsSyncFragmentInteractionListener) context;
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
    public interface OnSettingsSyncFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
