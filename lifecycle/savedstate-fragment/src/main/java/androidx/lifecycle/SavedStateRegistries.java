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

package androidx.lifecycle;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Provides simple accessor for {@link BundleSavedStateRegistry} of Activity and Fragments
 */
public class SavedStateRegistries {

    private SavedStateRegistries() {
    }

    /**
     * Returns {@link BundleSavedStateRegistry} for the given fragment.
     * @param fragment a fragment whose {@link BundleSavedStateRegistry} is requested
     * @return a {@code SavedStateStore}
     */
    public static BundleSavedStateRegistry of(Fragment fragment) {
        return SavedStateHolderFragmentController.savedStateRegistry(fragment);
    }

    /**
     * Returns {@link BundleSavedStateRegistry} for the given activity.
     * @param activity a activity whose {@link BundleSavedStateRegistry} is requested
     * @return a {@code SavedStateStore}
     */
    public static BundleSavedStateRegistry of(FragmentActivity activity) {
        return SavedStateHolderFragmentController.savedStateRegistry(activity);
    }
}
