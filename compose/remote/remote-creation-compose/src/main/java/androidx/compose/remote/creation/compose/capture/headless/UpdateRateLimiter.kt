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

package androidx.compose.remote.creation.compose.capture.headless

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * Token bucket that limits how often a headless host emits rendered outputs. Holds up to [burst]
 * tokens, each emission uses one, and one token is earned back every [interval].
 *
 * Not thread safe; it is only used from the collecting coroutine.
 *
 * @param onThrottled called when an emission had to wait, at most once per [reportInterval], with
 *   the number of throttled emissions since the previous call, e.g. to log a warning.
 */
internal class UpdateRateLimiter(
    private val burst: Int,
    /** Time to earn back one emission once the burst is used up. */
    val interval: Duration,
    private val timeSource: TimeSource,
    private val reportInterval: Duration = DEFAULT_REPORT_INTERVAL,
    private val onThrottled: (throttledCount: Int) -> Unit = {},
) {
    private var tokens = burst
    private var refilledAt: TimeMark = timeSource.markNow()
    private var throttledSinceReport = 0
    private var reportedAt: TimeMark? = null

    /**
     * Returns immediately if an output may be emitted now, otherwise reports (rate limited) and
     * suspends until one may.
     */
    suspend fun awaitPermit() {
        refill()
        if (tokens > 0) return
        throttledSinceReport++
        reportThrottled()
        delay(interval - refilledAt.elapsedNow())
        // Grant the token directly instead of calling refill() again: the dispatcher running
        // delay() may use a different clock (e.g. virtual time) from timeSource, which then has not
        // advanced.
        tokens = 1
        refilledAt = timeSource.markNow()
    }

    /** `true` if an output may be emitted now, i.e. [awaitPermit] would return immediately. */
    fun hasPermit(): Boolean {
        refill()
        return tokens > 0
    }

    /** Records that an output was emitted. */
    fun onEmitted() {
        if (tokens > 0) tokens--
    }

    private fun refill() {
        if (tokens >= burst) {
            refilledAt = timeSource.markNow()
            return
        }
        val earned = refilledAt.elapsedNow().inWholeNanoseconds / interval.inWholeNanoseconds
        if (earned <= 0) return
        if (tokens + earned >= burst) {
            tokens = burst
            refilledAt = timeSource.markNow()
        } else {
            tokens += earned.toInt()
            refilledAt += interval * earned.toDouble()
        }
    }

    private fun reportThrottled() {
        val last = reportedAt
        if (last != null && last.elapsedNow() < reportInterval) return
        onThrottled(throttledSinceReport)
        throttledSinceReport = 0
        reportedAt = timeSource.markNow()
    }

    private companion object {
        /** Minimum time between [onThrottled] calls, so throttled content does not flood a log. */
        val DEFAULT_REPORT_INTERVAL: Duration = 60.seconds
    }
}
