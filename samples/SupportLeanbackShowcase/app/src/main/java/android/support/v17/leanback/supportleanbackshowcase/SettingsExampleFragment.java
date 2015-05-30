package android.support.v17.leanback.supportleanbackshowcase;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v17.preference.LeanbackPreferenceDialogFragment;
import android.support.v17.preference.LeanbackPreferenceFragment;
import android.support.v17.preference.LeanbackSettingsFragment;
import android.support.v7.preference.DialogPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import java.util.Stack;

public class SettingsExampleFragment extends LeanbackSettingsFragment implements DialogPreference.TargetFragment {

    public static final String TAG = "SettingsExampleFragment";
    private final Stack<Fragment> fragments = new Stack<Fragment>();

    @Override
    public void onPreferenceStartInitialScreen() {
        startPreferenceFragment(buildPreferenceFragment(R.xml.prefs, null));
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment preferenceFragment,
                                             Preference preference) {
        return false;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment preferenceFragment,
                                           PreferenceScreen preferenceScreen) {
        PreferenceFragment frag = buildPreferenceFragment(R.xml.prefs, preferenceScreen.getKey());
        if ("prefs_wifi_screen_key".equals(preferenceScreen.getKey())) {
            ListPreference pref = (ListPreference)preferenceScreen.findPreference("prefs_wifi_key");
            pref.setEntries(new String[] {"Wi-Fi Network 01"});
            pref.setEntryValues(new String[] {"01"});
            if (Constants.LOCAL_LOGD) Log.d(TAG, "pref: " + pref);
        }
        startPreferenceFragment(frag);
        return true;
    }

    @Override
    public Preference findPreference(CharSequence prefKey) {
        return ((PreferenceFragment) fragments.peek()).findPreference(prefKey);
    }

    private PreferenceFragment buildPreferenceFragment(int preferenceResId, String root) {
        PreferenceFragment fragment = new PrefFragment();
        Bundle args = new Bundle();
        args.putInt("preferenceResource", preferenceResId);
        args.putString("root", root);
        fragment.setArguments(args);
        return fragment;
    }

    private class PrefFragment extends LeanbackPreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            String root = getArguments().getString("root", null);
            int prefResId = getArguments().getInt("preferenceResource");
            if (root == null) {
                addPreferencesFromResource(prefResId);
            } else {
                setPreferencesFromResource(prefResId, root);
            }
        }

        @Override
        public void onAttach(Context context) {
            fragments.push(this);
            super.onAttach(context);
        }

        @Override
        public void onDetach() {
            fragments.pop();
            super.onDetach();
        }
    }
}
