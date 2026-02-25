/*
 * Copyright 2025 The Android Open Source Project
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

import android.content.Context
import android.content.Intent
import androidx.security.state.IUpdateInfoService
import androidx.security.state.UpdateCheckResult
import androidx.security.state.UpdateInfo
import java.io.IOException

/**
 * Concrete implementation of the abstract [UpdateInfoService] for testing purposes.
 *
 * This class acts as a **Test Double** (Stub/Spy) that enables unit tests to:
 * 1. **Control Business Logic:** Deterministically toggle freshness checks and rate limiting via
 *    flags like [testShouldFetch] and [testIsThrottled].
 * 2. **Verify Concurrency:** Control the execution flow of network requests via the
 *    [onFetchUpdates] hook to reliably test Double-Checked Locking without race conditions.
 * 3. **Simulate Behavior:** Mock network returns ([updatesToReturn]) and failures
 *    ([shouldThrowError]).
 * 4. **Test Default Logic:** Verify the real base class behavior by setting
 *    [useRealFreshnessLogic].
 */
class TestUpdateInfoService : UpdateInfoService() {
    /** Tracks the number of times [fetchUpdates] has been called. */
    var fetchCount = 0

    /** Tracks if [onFetchFailed] was invoked (e.g. due to a network error). */
    var wasOnFetchFailedCalled: Boolean = false

    /**
     * Controls the return value of [shouldFetchUpdates]. Set to `true` to simulate a stale cache
     * (triggering a fetch attempt), or `false` to simulate a fresh cache.
     *
     * Ignored if [useRealFreshnessLogic] is true.
     */
    var testShouldFetch: Boolean = false

    /**
     * If set to `true`, [shouldFetchUpdates] delegates to the real base class implementation
     * (checking the 1-hour expiration) instead of returning [testShouldFetch].
     */
    var useRealFreshnessLogic: Boolean = false

    /**
     * Controls the return value of [shouldThrottle]. Set to `true` to simulate an active rate limit
     * (blocking the fetch).
     */
    var testIsThrottled: Boolean = false

    /** If `true`, [fetchUpdates] will throw an [IOException] to simulate a network failure. */
    var shouldThrowError: Boolean = false

    /** The list of updates to return from a successful [fetchUpdates] call. */
    var updatesToReturn: List<UpdateInfo> = emptyList()

    /**
     * Optional callback to control the execution of [fetchUpdates].
     *
     * Tests can set this to suspend execution (e.g., using `CompletableDeferred`), holding the
     * service lock to verify that concurrent requests are correctly blocked.
     */
    var onFetchUpdates: (suspend () -> Unit)? = null

    /**
     * Exposes [attachBaseContext] publicly so tests can inject a standard or mock Context (e.g.,
     * from Robolectric) without using reflection.
     */
    fun attach(context: Context) {
        super.attachBaseContext(context)
    }

    /**
     * Helper to call the protected AIDL method directly from a test context. This simulates an
     * incoming IPC call from a client by binding via [onBind].
     *
     * @throws IllegalStateException if the service refuses to bind.
     */
    fun callListAvailableUpdates(): UpdateCheckResult {
        val intent = Intent("androidx.security.state.provider.UPDATE_INFO_SERVICE")
        val binder =
            onBind(intent) as? IUpdateInfoService
                ?: throw IllegalStateException("Service refused to bind (check Intent action)")
        return binder.listAvailableUpdates()
    }

    /**
     * Exposes the protected [shouldFetchUpdates] method publicly. This allows tests to verify the
     * default time-based logic when [useRealFreshnessLogic] is true.
     */
    fun callShouldFetchUpdates(): Boolean {
        return shouldFetchUpdates()
    }

    /**
     * Simulates a network fetch operation.
     *
     * This implementation:
     * 1. Increments [fetchCount].
     * 2. Throws exception if [shouldThrowError] is set.
     * 3. Executes [onFetchUpdates] hook to allow test synchronization.
     * 4. Flips [testShouldFetch] to `false` to simulate a successful state update.
     */
    override suspend fun fetchUpdates(): List<UpdateInfo> {
        fetchCount++

        // Simulate Network Failure
        if (shouldThrowError) {
            throw IOException("Simulated Network Error")
        }

        // Test Hook: Allow the test to pause execution here to verify locking.
        onFetchUpdates?.invoke()

        // Simulate Side-Effect:
        // In a real app, a successful fetch updates the last check time, making the cache fresh.
        // We must manually flip this flag here so that subsequent threads waiting on the
        // lock will see the "fresh" state during their double-check.
        testShouldFetch = false

        return updatesToReturn
    }

    override fun shouldFetchUpdates(): Boolean {
        if (useRealFreshnessLogic) {
            return super.shouldFetchUpdates()
        }
        return testShouldFetch
    }

    override fun shouldThrottle(): Boolean = testIsThrottled

    override fun onFetchFailed(e: Exception) {
        wasOnFetchFailedCalled = true
        super.onFetchFailed(e)
    }
}
