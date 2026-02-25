/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.authoring.latency.aggregators

import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.latency.aggregators.internal.ConcurrentIntervalQueue
import androidx.ink.authoring.latency.aggregators.internal.runEvery
import java.util.concurrent.Executor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * A [LatencyAggregator] that reports at most one latency sample per fixed time period.
 *
 * Usage example in a `DocumentActivity`:
 * ```
 * // Report at most one latency interval per five seconds.
 * val aggregator = RateLimitedLatencyAggregator.create(
 *   period = 5.seconds,
 *   scope = lifecycleScope + backgroundCpuCoroutineContext,
 * ) {
 *  // This "reporting lambda" runs in backgroundCpuCoroutineContext. Here we just print out the
 *  // results, but a real caller could send them to a database, update a debug pane, etc.
 *  startNanos: Long, endNanos: Long ->
 *  Log.i("$startNanos -- $endNanos")
 * }
 *
 * // Send a selected latency interval to the aggregator in the latencyDataCallback.
 * inProgressStrokesView.latencyDataCallback = { latency: LatencyData ->
 *   aggregator.aggregate(latency.timePointA, latency.timePointB)
 * }
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalLatencyDataApi
public class RateLimitedLatencyAggregator
private constructor(private val implementationHelper: ImplementationHelper) : LatencyAggregator {

    public fun interface Callback {
        /**
         * Callback invoked at most once per [period] to report the latest start and end values
         * passed to [aggregate]. This callback runs in the [CoroutineScope] (or, for Java clients,
         * the [Executor]) passed to [create].
         */
        public suspend fun onLatencySample(startNanos: Long, endNanos: Long): Unit
    }

    @UiThread
    public override fun aggregate(startNanos: Long, endNanos: Long): Unit =
        implementationHelper.aggregate(startNanos, endNanos)

    /**
     * Reports the latest sample without disrupting the regular cadence of asynchronous reports. If
     * any more samples come in before the next report, one of those will be reported at that time.
     */
    @UiThread
    public override fun reportSynchronously(): Unit = implementationHelper.reportSynchronously()

    public override fun job(): Job = implementationHelper.job

    public companion object {
        /**
         * Returns a new [RateLimitedLatencyAggregator]. For use by Kotlin clients. [callback] will
         * be called in the given [scope], using its default [CoroutineContext].
         *
         * @param period The period of time over which at most one sample will be reported.
         * @param scope The scope in which to aggregate and to call the callback. The scope's
         *   associated context should be distinct from the UI thread.
         * @param callback The callback with which to report samples.
         */
        @JvmStatic
        public fun create(
            period: Duration,
            scope: CoroutineScope,
            callback: Callback,
        ): RateLimitedLatencyAggregator {
            check(period > 0.seconds) { "period must be be a positive duration" }
            return RateLimitedLatencyAggregator(ImplementationHelper(period, scope, callback))
        }

        /**
         * Returns a new [RateLimitedLatencyAggregator]. For use by Java clients.
         *
         * @param periodMilliseconds The period of time, in milliseconds, over which at most one
         *   sample will be reported.
         * @param executor An [Executor] on which which to perform reporting. Should be distinct
         *   from the UI thread. More efficient if the [Executor] is an instance of
         *   [java.util.concurrent.ScheduledExecutorService].
         * @param callback The callback with which to report samples.
         */
        @JvmStatic
        public fun create(
            periodMilliseconds: Int,
            executor: Executor,
            callback: Callback,
        ): RateLimitedLatencyAggregator =
            create(
                periodMilliseconds.milliseconds,
                CoroutineScope(executor.asCoroutineDispatcher()),
                callback,
            )
    }

    private class ImplementationHelper(
        val period: Duration,
        val scope: CoroutineScope,
        val callback: Callback,
    ) {
        // Though we just need the latest sample in each window, we still use the
        // ConcurrentIntervalQueue for samples, since [aggregate] and [reportLatestSample] are
        // concurrent.
        private val sampleQueue = ConcurrentIntervalQueue(numPreallocatedIntervals = 10)

        val job = scope.runEvery(period, ::reportLatestSample)

        @UiThread
        fun aggregate(startNanos: Long, endNanos: Long) {
            sampleQueue.record(startNanos, endNanos)
            // Drop all but the newest 5 samples, to avoid using up all our preallocated instances.
            while (sampleQueue.size() > 5) {
                sampleQueue.recycleOldest()
            }
        }

        @UiThread
        fun reportSynchronously() {
            runBlocking { reportLatestSample() }
        }

        suspend fun reportLatestSample() {
            // Empty out all but the most recent sample, which is the one we'll report.
            while (sampleQueue.size() > 1) {
                sampleQueue.recycleOldest()
            }
            // Report the sample that remains. (Others may have been added after it in the meantime,
            // but
            // they'll be processed later.)
            sampleQueue.applyToOldestAndRecycle { interval ->
                callback.onLatencySample(interval.startNanos, interval.endNanos)
            }
        }
    }
}
