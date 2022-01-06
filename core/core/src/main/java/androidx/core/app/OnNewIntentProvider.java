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

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

/**
 * Interface for components that can dispatch calls from
 * {@link android.app.Activity#onNewIntent(Intent)}.
 */
public interface OnNewIntentProvider {
    /**
     * Add a new listener that will get a callback associated with
     * {@link android.app.Activity#onNewIntent(Intent)} with the
     * new {@link Intent}.
     *
     * @param listener The listener that should be called whenever
     * {@link android.app.Activity#onNewIntent(Intent)} was called.
     */
    void addOnNewIntentListener(@NonNull Consumer<Intent> listener);

    /**
     * Remove a previously added listener. It will not receive any future callbacks.
     *
     * @param listener The listener previously added with
     * {@link #addOnNewIntentListener(Consumer)} that should be removed.
     */
    void removeOnNewIntentListener(@NonNull Consumer<Intent> listener);
}
