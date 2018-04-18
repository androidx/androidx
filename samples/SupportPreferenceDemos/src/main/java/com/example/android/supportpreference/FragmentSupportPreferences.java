/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.example.android.supportpreference;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceScreen;

/**
 * Demonstration of PreferenceFragment, showing a single fragment in an
 * activity.
 */
public class FragmentSupportPreferences extends Activity
        implements PreferenceFragment.OnPreferenceStartScreenCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new PrefsFragment()).commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragment caller, PreferenceScreen pref) {
        final Fragment f = new PrefsFragment();
        final Bundle args = new Bundle(1);
        args.putString(PreferenceFragment.ARG_PREFERENCE_ROOT, pref.getKey());
        f.setArguments(args);
        getFragmentManager().beginTransaction().replace(android.R.id.content, f)
                .addToBackStack(null)
                .commit();
        return true;
    }

    //BEGIN_INCLUDE(support_fragment)
    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
//END_INCLUDE(support_fragment)
}
