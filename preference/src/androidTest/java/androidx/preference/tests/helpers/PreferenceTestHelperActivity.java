/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License.
 */

package androidx.preference.tests.helpers;

import android.os.Bundle;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * Helper activity that inflates a preference hierarchy defined in a given XML resource with a
 * {@link PreferenceFragmentCompat} to aid testing.
 */
public class PreferenceTestHelperActivity extends AppCompatActivity {

    /**
     * Inflates the given XML resource and returns the root PreferenceScreen from the hierarchy.
     *
     * @param preferenceLayoutId The XML resource ID to inflate
     * @return An inflated PreferenceScreen to be used in tests
     */
    public PreferenceScreen setupPreferenceHierarchy(@LayoutRes int preferenceLayoutId) {
        TestFragment fragment = new TestFragment(preferenceLayoutId);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                fragment).commitNow();
        return fragment.getPreferenceScreen();
    }

    public static class TestFragment extends PreferenceFragmentCompat {
        private final int mPreferenceLayoutId;

        TestFragment(int preferenceLayoutId) {
            mPreferenceLayoutId = preferenceLayoutId;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(mPreferenceLayoutId, rootKey);
        }
    }
}
