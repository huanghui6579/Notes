package com.yunxinlink.notes.ui.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.yunxinlink.notes.R;

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
    public static SettingsSyncFragment newInstance() {
        SettingsSyncFragment fragment = new SettingsSyncFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.pref_data_sync);
        
//        bindPreferenceSummaryToValue(findPreference("sync_note_state"));
        
//        bindPreferenceChangeListener(findPreference("sync_note_auto"));
//        bindPreferenceChangeListener(findPreference("sync_note_traffic"));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSettingsSyncFragmentInteractionListener) {
            mListener = (OnSettingsSyncFragmentInteractionListener) context;
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
