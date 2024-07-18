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

package androidx.appactions.interaction.capabilities.core

import androidx.annotation.RestrictTo
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.concurrent.futures.await
import com.google.common.util.concurrent.ListenableFuture

/** Provides a mechanism for the app to listen to argument updates from Assistant. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ValueListener<T> {
    /**
     * Invoked when Assistant reports that an argument value has changed. This method should be
     * idempotent, as it may be called multiple times with the same input value, not only on the
     * initial value change.
     *
     * <p>This method should:
     * <ul>
     * <li>1. validate the given argument value(s).
     * <li>2. If the given values are valid, update app UI state if applicable.
     * </ul>
     *
     * @return the [ValidationResult].
     */
    suspend fun onReceived(value: T): ValidationResult = onReceivedAsync(value).await()

    /**
     * Invoked when Assistant reports that an argument value has changed. This method should be
     * idempotent, as it may be called multiple times with the same input value, not only on the
     * initial value change.
     *
     * <p>This method should:
     * <ul>
     * <li>1. validate the given argument value(s).
     * <li>2. If the given values are valid, update app UI state if applicable.
     * </ul>
     *
     * @return a [ListenableFuture] containing the [ValidationResult].
     */
    fun onReceivedAsync(value: T): ListenableFuture<ValidationResult> =
        Futures.immediateFuture(ValidationResult.newAccepted())
}
