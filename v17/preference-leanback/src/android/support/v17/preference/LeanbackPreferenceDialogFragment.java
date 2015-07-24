/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v17.preference;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.DialogPreference;

public class LeanbackPreferenceDialogFragment extends Fragment {

    public static final String ARG_KEY = "key";

    private DialogPreference mPreference;

    public LeanbackPreferenceDialogFragment() {
        if (Build.VERSION.SDK_INT >= 21) {
            LeanbackPreferenceFragmentTransitionHelperApi21.addTransitions(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            throw new IllegalStateException("Target fragment must implement TargetFragment" +
                    " interface");
        }

        final DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) rawFragment;

        final String key = getArguments().getString(LeanbackListPreferenceDialogFragment.ARG_KEY);
        mPreference = (DialogPreference) fragment.findPreference(key);
    }

    public DialogPreference getPreference() {
        return mPreference;
    }
}
