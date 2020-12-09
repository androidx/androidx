/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.biometric.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * A wrapper class for {@link FragmentActivity} and {@link Fragment} with the FragmentActivity or
 * Fragment that hosts the AuthPrompt
 */
public class AuthPromptHost {
    @Nullable private Fragment mFragment;
    @Nullable private FragmentActivity mActivity;

    public AuthPromptHost(@NonNull Fragment fragment) {
        mFragment = fragment;
    }

    public AuthPromptHost(@NonNull FragmentActivity activity) {
        mActivity = activity;
    }

    @Nullable
    public Fragment getFragment() {
        return mFragment;
    }

    @Nullable
    public FragmentActivity getActivity() {
        return mActivity;
    }
}