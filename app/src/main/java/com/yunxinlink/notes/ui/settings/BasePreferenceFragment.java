package com.yunxinlink.notes.ui.settings;

import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.yunxinlink.notes.NoteApplication;

/**
 * @author huanghui1
 * @update 2016/8/24 17:50
 * @version: 0.0.1
 */
public abstract class BasePreferenceFragment extends PreferenceFragment {
    protected static String TAG = null;

    BasePreferenceFragment() {
        TAG = getClass().getSimpleName();
    }

//    /**
//     * A preference value change listener that updates the preference's summary
//     * to reflect its new value.
//     */
//    protected Preference.OnPreferenceChangeListener sBindPreferenceToValueListener = new Preference.OnPreferenceChangeListener() {
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object value) {
//            String stringValue = value.toString();
//
//            if (preference instanceof ListPreference) {
//                // For list preferences, look up the correct display value in
//                // the preference's 'entries' list.
//                ListPreference listPreference = (ListPreference) preference;
//                int index = listPreference.findIndexOfValue(stringValue);
//
//                // Set the summary to reflect the new value.
//                preference.setSummary(
//                        index >= 0
//                                ? listPreference.getEntries()[index]
//                                : null);
//
//            } else if (preference instanceof RingtonePreference) {
//                // For ringtone preferences, look up the correct display value
//                // using RingtoneManager.
//                if (TextUtils.isEmpty(stringValue)) {
//                    // Empty values correspond to 'silent' (no ringtone).
//                    preference.setSummary(R.string.pref_ringtone_silent);
//
//                } else {
//                    Ringtone ringtone = RingtoneManager.getRingtone(
//                            preference.getContext(), Uri.parse(stringValue));
//
//                    if (ringtone == null) {
//                        // Clear the summary if there was a lookup error.
//                        preference.setSummary(null);
//                    } else {
//                        // Set the summary to reflect the new ringtone display
//                        // name.
//                        String name = ringtone.getTitle(preference.getContext());
//                        preference.setSummary(name);
//                    }
//                }
//
//            } else if (preference instanceof CheckBoxPreference) {
//                KLog.d("---preference--check--changed---" + stringValue);
//            } else if (preference instanceof SwitchPreference) {
//                KLog.d("---preference--switch--changed---" + stringValue);
//            } else {
//                // For all other preferences, set the summary to the value's
//                // simple string representation.
//                preference.setSummary(stringValue);
//            }
//            return true;
//        }
//    };
//
//    /**
//     * Binds a preference's summary to its value. More specifically, when the
//     * preference's value is changed, its summary (line of text below the
//     * preference title) is updated to reflect the value. The summary is also
//     * immediately updated upon calling this method. The exact display format is
//     * dependent on the type of preference.
//     *
//     * @see #sBindPreferenceToValueListener
//     */
//    protected void bindPreferenceSummaryToValue(Preference preference) {
//        // Set the listener to watch for value changes.
//        preference.setOnPreferenceChangeListener(sBindPreferenceToValueListener);
//
//        // Trigger the listener immediately with the preference's
//        // current value.
//        sBindPreferenceToValueListener.onPreferenceChange(preference,
//                PreferenceManager
//                        .getDefaultSharedPreferences(preference.getContext())
//                        .getString(preference.getKey(), ""));
//    }
//
    /**
     * 设置值变化的监听
     * @param preference
     */
    protected void bindPreferenceChangeListener(Preference preference, Preference.OnPreferenceChangeListener preferenceChangeListener) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(preferenceChangeListener);
    }

    /**
     * 获取app
     * @return
     */
    protected NoteApplication getApp() {
        return (NoteApplication) getActivity().getApplication();
    }
}
