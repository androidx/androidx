/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.task;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Provides a mechanism for the app to listen to argument updates from Assistant.
 *
 * @param <T>
 */
public interface ValueListener<T> {
    /**
     * Invoked when Assistant reports that an argument value has changed. This method should be
     * idempotent, as it may be called multiple times with the same input value, not only on the
     * initial value change.
     *
     * <p>This method should:
     *
     * <ul>
     *   <li>1. validate the given argument value(s).
     *   <li>2. If the given values are valid, update app UI state if applicable.
     * </ul>
     *
     * <p>Returns a ListenableFuture that resolves to the ValidationResult.
     */
    @NonNull
    ListenableFuture<ValidationResult> onReceived(T value);
}
