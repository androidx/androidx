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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcPlatformServices.LogCategory
import androidx.compose.remote.creation.compose.capture.headless.UpdateRateLimiter
import kotlin.time.Duration.Companion.milliseconds
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
    internal fun newLimiter(
        timeSource: TimeSource = TimeSource.Monotonic,
        platform: RcPlatformServices = RcPlatformServices.None,
    ): UpdateRateLimiter? {
        if (intervalMillis == 0L) return null
        val interval = intervalMillis.milliseconds
        return UpdateRateLimiter(burst, interval, timeSource) { throttled ->
            platform.log(
                LogCategory.WARN,
                "captureRemoteDocument content is updating faster than $burst documents, then " +
                    "one every $interval; throttled $throttled update(s). Remote documents should " +
                    "change rarely; animate with remote expressions instead of recomposition.",
            )
        }
    }

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
