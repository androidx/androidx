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

package androidx.compose.remote.creation.compose.capture

import android.util.Log
import androidx.annotation.RestrictTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

/**
 * Limits how often [captureRemoteDocument] emits documents.
 *
 * Remote documents should change rarely. After a state change, a document may update a few times in
 * quick succession while it settles, but it should rarely update more often than every few seconds
 * over a sustained period. Animation belongs in remote expressions that the player evaluates.
 * Content that recomposes the document faster than that is almost certainly animating through
 * recomposition, which costs CPU on the host and bandwidth to every player. It is therefore
 * throttled, and a warning is logged.
 *
 * Throttling suspends between emissions, so updates that arrive meanwhile are conflated and the
 * latest state is always emitted eventually.
 *
 * TODO(b/567847315): Make this public and configurable, and consider an adaptive strategy, e.g.
 *   backing off further while content keeps updating, or reacting to how fast the collector
 *   consumes documents.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CaptureUpdateThrottle
private constructor(private val burst: Int, private val intervalMillis: Long) {

    /** Creates the per-collection limiter, or `null` if emissions are never throttled. */
    internal fun newLimiter(timeSource: TimeSource = TimeSource.Monotonic): UpdateRateLimiter? =
        if (intervalMillis == 0L) null
        else UpdateRateLimiter(burst, intervalMillis.milliseconds, timeSource)

    public companion object {
        /**
         * Emits every changed document as soon as it is rendered.
         *
         * Throttling suspends with [delay], which never resumes on hosts whose clock only advances
         * explicitly, such as a paused Robolectric main looper. Tests on such hosts that expect
         * many documents in quick succession should use this.
         */
        @JvmField public val None: CaptureUpdateThrottle = CaptureUpdateThrottle(0, 0L)

        /**
         * Emits up to [burst] documents immediately, then at most one document per
         * [intervalMillis], earning back one document of burst for every [intervalMillis] without
         * emissions.
         */
        @JvmStatic
        public fun rateLimited(burst: Int, intervalMillis: Long): CaptureUpdateThrottle {
            require(burst >= 1) { "burst must be at least 1, was $burst" }
            require(intervalMillis > 0) { "intervalMillis must be positive, was $intervalMillis" }
            return CaptureUpdateThrottle(burst, intervalMillis)
        }

        /**
         * Allows a document to settle through a few quick updates, then limits sustained updates to
         * one per second.
         */
        internal val Default: CaptureUpdateThrottle =
            rateLimited(burst = DEFAULT_BURST, intervalMillis = DEFAULT_INTERVAL_MILLIS)
    }
}

private const val DEFAULT_BURST = 5
private const val DEFAULT_INTERVAL_MILLIS = 1_000L

/** Minimum time between throttling warnings, so throttled content does not flood the log. */
private val THROTTLE_LOG_INTERVAL: Duration = 60.seconds

private const val TAG = "CaptureRemoteDocument"

/**
 * Token bucket that limits emissions of a single [captureRemoteDocument] collection. Holds up to
 * [burst] tokens, each emission uses one, and one token is earned back every [interval].
 *
 * Not thread safe; it is only used from the collecting coroutine.
 */
internal class UpdateRateLimiter(
    private val burst: Int,
    private val interval: Duration,
    private val timeSource: TimeSource,
) {
    private var tokens = burst
    private var refilledAt: TimeMark = timeSource.markNow()
    private var throttledSinceLog = 0
    private var loggedAt: TimeMark? = null

    /**
     * Returns immediately if a document may be emitted now, otherwise logs (rate limited) and
     * suspends until one may.
     */
    suspend fun awaitPermit() {
        refill()
        if (tokens > 0) return
        throttledSinceLog++
        logThrottled()
        delay(interval - refilledAt.elapsedNow())
        // Grant the token directly instead of calling refill() again: the dispatcher running
        // delay() may use a different clock (e.g. virtual time) from timeSource, which then has not
        // advanced.
        tokens = 1
        refilledAt = timeSource.markNow()
    }

    /** Records that a document was emitted. */
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

    private fun logThrottled() {
        val last = loggedAt
        if (last != null && last.elapsedNow() < THROTTLE_LOG_INTERVAL) return
        Log.w(
            TAG,
            "captureRemoteDocument content is updating faster than $burst documents, then one " +
                "every $interval; throttled $throttledSinceLog update(s). Remote documents " +
                "should change rarely; animate with remote expressions instead of recomposition.",
        )
        throttledSinceLog = 0
        loggedAt = timeSource.markNow()
    }
}
