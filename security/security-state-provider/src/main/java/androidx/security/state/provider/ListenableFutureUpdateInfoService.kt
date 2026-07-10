/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.security.state.provider

import androidx.concurrent.futures.await
import androidx.security.state.UpdateInfo
import com.google.common.util.concurrent.ListenableFuture

/**
 * [ListenableFuture]-based compatibility wrapper around [UpdateInfoService]'s suspending APIs.
 *
 * This class is designed for Java consumers who need to implement an [UpdateInfoService] but prefer
 * using Guava's [ListenableFuture] over Kotlin Coroutines. By extending this class, host
 * applications can provide the actual network logic by implementing the [fetchUpdatesAsync] method.
 *
 * ### Host Implementation
 * Host applications (such as the System Updater, Google Play Store, or OEM-specific updaters)
 * written in Java must extend this class and implement the [fetchUpdatesAsync] method to provide
 * the actual update check logic.
 *
 * Kotlin consumers should generally prefer extending [UpdateInfoService] directly to leverage
 * native suspending functions.
 */
public abstract class ListenableFutureUpdateInfoService : UpdateInfoService() {

    /**
     * Performs the actual network request to fetch fresh updates from the backend asynchronously.
     *
     * **Template Method:** This method is implemented by the host application (e.g., the System
     * Updater) and invoked by the base class on a background thread. The base class handles
     * concurrency, caching, and rate-limiting.
     *
     * **Preconditions:** This method is only called if [shouldFetchUpdates] returns `true` and
     * [shouldThrottle] returns `false` (the rate limiter allows the request).
     *
     * @return A [ListenableFuture] containing a list of [UpdateInfo] objects currently available
     *   for the device.
     */
    protected abstract fun fetchUpdatesAsync():
        ListenableFuture<@JvmSuppressWildcards List<UpdateInfo>>

    /**
     * Satisfies the base class Coroutine requirement by delegating to the Future-based
     * implementation.
     */
    final override suspend fun fetchUpdates(): List<UpdateInfo> {
        return fetchUpdatesAsync().await()
    }
}
