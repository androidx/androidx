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

package androidx.fragment.app;

import android.os.Bundle;

import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

/**
 * Listener for handling fragment results.
 *
 * This object should be passed to
 * {@link FragmentManager#setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)}
 * and it will listen for results with the same key that are passed into
 * {@link FragmentManager#setFragmentResult(String, Bundle)}.
 *
 * @see FragmentResultOwner#setFragmentResultListener
 */
public interface FragmentResultListener {
    /**
     * Callback used to handle results passed between fragments.
     *
     * @param requestKey key used to store the result
     * @param result result passed to the callback
     */
    void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result);
}
