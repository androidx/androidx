/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.app;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

/**
 * Interface for components that can dispatch calls from
 * {@link Activity#onMultiWindowModeChanged}.
 */
public interface OnMultiWindowModeChangedProvider {
    /**
     * Add a new listener that will get a callback associated with
     * {@link Activity#onMultiWindowModeChanged} with the
     * new {@link MultiWindowModeChangedInfo}.
     *
     * @param listener The listener that should be called whenever
     * {@link Activity#onMultiWindowModeChanged} was called.
     */
    void addOnMultiWindowModeChangedListener(
            @NonNull Consumer<MultiWindowModeChangedInfo> listener);

    /**
     * Remove a previously added listener. It will not receive any future callbacks.
     *
     * @param listener The listener previously added with
     * {@link #addOnMultiWindowModeChangedListener(Consumer)} that should be removed.
     */
    void removeOnMultiWindowModeChangedListener(
            @NonNull Consumer<MultiWindowModeChangedInfo> listener);
}
