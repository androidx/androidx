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
 * A class that manages passing data between fragments.
 */
public interface FragmentResultOwner {

    /**
     * Sets the given result for the requestKey. This result will be delivered to a
     * {@link FragmentResultListener} that is called given to
     * {@link #setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)} with
     * the same requestKey. If no {@link FragmentResultListener} with the same key is set or the
     * Lifecycle associated with the listener is not at least
     * {@link androidx.lifecycle.Lifecycle.State#STARTED}, the result is stored until one becomes
     * available, or {@link #clearFragmentResult(String)} is called with the same requestKey.
     *
     * @param requestKey key used to identify the result
     * @param result the result to be passed to another fragment
     */
    void setFragmentResult(@NonNull String requestKey, @NonNull Bundle result);

    /**
     * Clears the stored result for the given requestKey.
     *
     * This clears any result that was previously set via
     * {@link #setFragmentResult(String, Bundle)} that hasn't yet been delivered to a
     * {@link FragmentResultListener}.
     *
     * @param requestKey key used to identify the result
     */
    void clearFragmentResult(@NonNull String requestKey);

    /**
     * Sets the {@link FragmentResultListener} for a given requestKey. Once the given
     * {@link LifecycleOwner} is at least in the {@link androidx.lifecycle.Lifecycle.State#STARTED}
     * state, any results set by {@link #setFragmentResult(String, Bundle)} using the same
     * requestKey will be delivered to the
     * {@link FragmentResultListener#onFragmentResult(String, Bundle) callback}. The callback will
     * remain active until the LifecycleOwner reaches the
     * {@link androidx.lifecycle.Lifecycle.State#DESTROYED} state or
     * {@link #clearFragmentResultListener(String)} is called with the same requestKey.
     *
     * @param requestKey requestKey used to identify the result
     * @param lifecycleOwner lifecycleOwner for handling the result
     * @param listener listener for result changes
     */
    void setFragmentResultListener(@NonNull String requestKey,
            @NonNull LifecycleOwner lifecycleOwner, @NonNull FragmentResultListener listener);

    /**
     * Clears the stored {@link FragmentResultListener} for the given requestKey.
     *
     * This clears any {@link FragmentResultListener} that was previously set via
     * {@link #setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)}.
     *
     * @param requestKey key used to identify the result
     */
    void clearFragmentResultListener(@NonNull String requestKey);
}
