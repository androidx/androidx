/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit;

import androidx.annotation.Nullable;

/**
 * Callback interface for prefetch operations.
 *
 * @param <T> The type of the result returned on successful operation.
 */
@Profile.ExperimentalUrlPrefetch
@SuppressWarnings("GenericCallbacks")
public interface PrefetchOperationCallback<T> {

    /**
     * Called when the prefetch operation completes successfully.
     *
     * @param result The result of the prefetch operation.
     */
    void onSuccess(T result);

    /**
     * Called when the prefetch operation fails.
     *
     * @param exception The exception indicating the cause of the failure.
     */
    void onError(@Nullable Throwable exception);
}
