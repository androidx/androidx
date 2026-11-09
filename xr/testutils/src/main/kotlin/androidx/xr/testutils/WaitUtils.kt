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

@file:JvmName("WaitUtils")

package androidx.xr.testutils

import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Default timeout applied by [waitUntil] and [XrSessionRule.captureToBitmap]. */
internal val DEFAULT_WAIT_TIMEOUT = 5.seconds

/** Default polling interval used by [waitUntil] on the instrumentation thread. */
private val DEFAULT_POLL_INTERVAL = 50.milliseconds

/**
 * Polls [condition] serially on the instrumentation thread, syncing with the main thread between
 * attempts, until [condition] returns `true` or [timeout] elapses.
 *
 * Must be called from the instrumentation thread, not the Android main thread.
 *
 * @param description human-readable condition description included in timeout errors
 * @param timeout maximum duration to poll before throwing [AssertionError]
 * @param pollInterval delay between polling attempts
 * @param condition predicate evaluated on the instrumentation thread
 * @throws IllegalArgumentException if [timeout] or [pollInterval] is negative
 * @throws IllegalStateException if called from the main thread
 * @throws AssertionError if [condition] does not return `true` within [timeout]
 */
public fun waitUntil(
    description: String = "condition",
    timeout: Duration = DEFAULT_WAIT_TIMEOUT,
    pollInterval: Duration = DEFAULT_POLL_INTERVAL,
    condition: () -> Boolean,
) {
    require(!timeout.isNegative()) { "timeout must be non-negative, was $timeout" }
    require(!pollInterval.isNegative()) { "pollInterval must be non-negative, was $pollInterval" }
    check(Looper.myLooper() != Looper.getMainLooper()) {
        "waitUntil must be called from the instrumentation thread, not the main thread."
    }
    val start = TimeSource.Monotonic.markNow()
    while (true) {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        if (condition()) return
        if (start.elapsedNow() >= timeout) {
            throw AssertionError("Timed out after $timeout waiting for $description.")
        }
        CountDownLatch(1).await(pollInterval.inWholeNanoseconds, TimeUnit.NANOSECONDS)
    }
}
