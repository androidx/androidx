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
 * A wrapper class for the component that will be used to host an auth prompt.
 */
public class AuthPromptHost {
    @Nullable private FragmentActivity mActivity;
    @Nullable private Fragment mFragment;

    /**
     * Constructs an {@link AuthPromptHost} wrapper for the given activity.
     *
     * @param activity The activity that will host the prompt.
     */
    public AuthPromptHost(@NonNull FragmentActivity activity) {
        mActivity = activity;
    }

    /**
     * Constructs an {@link AuthPromptHost} wrapper for the given fragment.
     *
     * @param fragment The fragment that will host the prompt.
     */
    public AuthPromptHost(@NonNull Fragment fragment) {
        mFragment = fragment;
    }

    /**
     * Gets the activity that will host the prompt, if set.
     *
     * @return The activity that will host the prompt, or {@code null} if the prompt will be hosted
     * by a different type of component.
     */
    @Nullable
    public FragmentActivity getActivity() {
        return mActivity;
    }

    /**
     * Gets the fragment that will host the prompt, if set.
     *
     * @return The fragment that will host the prompt, or {@code null} if the prompt will be hosted
     * by a different type of component.
     */
    @Nullable
    public Fragment getFragment() {
        return mFragment;
    }
}