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

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.ink.authoring.ExperimentalLatencyDataApi
import androidx.ink.authoring.latency.aggregators.internal.ConcurrentIntervalQueue
import androidx.ink.authoring.latency.aggregators.internal.runEvery
import java.util.concurrent.Executor
import kotlin.collections.ArrayDeque
import kotlin.collections.ArrayList
import kotlin.collections.MutableList
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking

/**
 * A [LatencyAggregator] that reports latency percentiles in consecutive, non-overlapping time
 * windows.
 *
 * Usage example in a `DocumentActivity`:
 * ```
 * val aggregator = PercentileLatencyAggregator.create(
 *   window = 15.seconds,
 *   percentiles = listOf(50f, 90f),
 *   expectedSamplesPerSecond = 250,
 *   scope = lifecycleScope + backgroundCpuCoroutineContext,
 * ) {
 *  // This "reporting lambda" runs in backgroundCpuCoroutineContext. Here we just print out the
 *  // results, but a real caller could send them to a database, update a debug pane, etc.
 *  latencyPercentileNanos: List<Long>, sampleCount: Int ->
 *  Log.i("$latencyPercentileNanos; N=$sampleCount")
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
public class PercentileLatencyAggregator
private constructor(private val implementationHelper: ImplementationHelper) : LatencyAggregator {

    public fun interface Callback {
        /**
         * Callback invoked at most once per [ImplementationHelper.window] in the [CoroutineScope]
         * (or, for Java clients, the [Executor]) passed to [create].
         *
         * @param latencyPercentileNanos Nanosecond latency durations for each of the
         *   [ImplementationHelper.percentilesToReport], computed over the last aggregation window.
         *   Do not hold a reference to this [List] after returning from the callback; it will be
         *   recycled (overwritten in place) immediately for use in a future callback.
         * @param sampleCount Count of samples in this window. Will always be positive; if there are
         *   no samples, the callback does not get called.
         */
        public suspend fun onLatencyPercentiles(
            latencyPercentileNanos: List<Long>,
            sampleCount: Int,
        ): Unit
    }

    @UiThread
    public override fun aggregate(startNanos: Long, endNanos: Long): Unit =
        implementationHelper.aggregate(startNanos, endNanos)

    @UiThread
    public override fun reportSynchronously(): Unit = implementationHelper.reportSynchronously()

    public override fun job(): Job = implementationHelper.job

    @VisibleForTesting
    internal fun numLateIntervalAllocations() = implementationHelper.numLateIntervalAllocations()

    @VisibleForTesting
    internal fun numLatePercentileListAllocations() =
        implementationHelper.numLatePercentileListAllocations

    public companion object {
        /**
         * Returns a new [PercentileLatencyAggregator]. For use by Kotlin clients. [callback] will
         * be called in the given [scope], using its default [kotlin.coroutines.CoroutineContext].
         *
         * @param window The length of the consecutive time windows in which to compute and report
         *   percentiles.
         * @param percentiles Percentiles to report in the callback. For example, `ListOf(50f,
         *   99.9f)` will report the median and 99.9-th percentile latency values.
         * @param expectedSamplesPerSecond The expected maximum number of calls to [aggregate] per
         *   second, for preallocation purposes. `expectedSamplesPerSecond * (window + 0.5.seconds)`
         *   latency samples will be preallocated, so please do not pass extremely high values for
         *   this parameter.
         * @param scope The scope in which to aggregate and to call the callback. The scope's
         *   associated context should be distinct from the UI thread.
         * @param callback The [Callback] with which to report latency percentiles.
         */
        @JvmStatic
        public fun create(
            window: Duration,
            percentiles: List<Float>,
            expectedSamplesPerSecond: Int,
            scope: CoroutineScope,
            callback: Callback,
        ): PercentileLatencyAggregator {
            validateConstructorParams(window, percentiles, expectedSamplesPerSecond)
            return PercentileLatencyAggregator(
                ImplementationHelper(window, percentiles, expectedSamplesPerSecond, scope, callback)
            )
        }

        /**
         * Returns a new [PercentileLatencyAggregator]. For use by Java clients. [callback] will be
         * called on the given [executor].
         *
         * @param windowMilliseconds The length, in milliseconds, of the consecutive time windows in
         *   which to compute and report percentiles.
         * @param percentiles Percentiles to report in the callback. For example, `ListOf(50f,
         *   99.9f)` will report the median and 99.9-th percentile latency values.
         * @param expectedSamplesPerSecond The expected maximum number of calls to [aggregate] per
         *   second, for preallocation purposes. `expectedSamplesPerSecond * (window + 0.5.seconds)`
         *   latency samples will be preallocated, so please do not pass extremely high values for
         *   this parameter.
         * @param executor An [Executor] on which to compute and report latency percentiles. Should
         *   be distinct from the UI thread. More efficient if the [Executor] is an instance of
         *   [java.util.concurrent.ScheduledExecutorService].
         * @param callback The [Callback] with which to report latency percentiles.
         */
        @JvmStatic
        public fun create(
            windowMilliseconds: Int,
            percentiles: List<Float>,
            expectedSamplesPerSecond: Int,
            executor: Executor,
            callback: Callback,
        ): PercentileLatencyAggregator {
            return create(
                windowMilliseconds.milliseconds,
                percentiles,
                expectedSamplesPerSecond,
                CoroutineScope(executor.asCoroutineDispatcher()),
                callback,
            )
        }

        private fun validateConstructorParams(
            window: Duration,
            percentiles: List<Float>,
            expectedSamplesPerSecond: Int,
        ) {
            check(window > 0.seconds) { "window must be be a positive duration" }
            check(!percentiles.isEmpty()) { "percentiles must not be empty" }
            check(percentiles.all { it >= 0f && it <= 100f }) {
                "percentiles must be in the range [0, 100]"
            }
            check(expectedSamplesPerSecond > 0) { "expectedSamplesPerSecond must be positive" }
        }
    }

    private class ImplementationHelper(
        val window: Duration,
        val percentilesToReport: List<Float>,
        val maxSamplesPerSecond: Int,
        val scope: CoroutineScope,
        val callback: Callback,
    ) {
        private val numPreallocatedInstances =
            (maxSamplesPerSecond *
                    (window.toDouble(SECONDS) + ESTIMATED_MAX_REPORT_DURATION_SECONDS))
                .toInt()

        private val intervalQueue = ConcurrentIntervalQueue(numPreallocatedInstances)

        /**
         * Pool of recycled lists for storing and reporting latency percentiles. After initial
         * preallocation, this will only be accessed from [scope].
         *
         * Invariant: every List in the pool has length exactly `percentilesToReport.size`.
         */
        private val percentilePool = ArrayDeque<MutableList<Long>>(PERCENTILE_POOL_INITIAL_CAPACITY)

        private val scratchDurationsNanos = ArrayList<Long>(numPreallocatedInstances)

        /** Number of times [obtainPercentileReportList] had to allocate a new [MutableList]. */
        @VisibleForTesting
        var numLatePercentileListAllocations = 0
            private set

        val job = scope.runEvery(window, ::calculateAndReportPercentiles)

        init {
            // Preallocate percentile lists for later use.
            repeat(PERCENTILE_POOL_INITIAL_CAPACITY) {
                percentilePool.add(ArrayList<Long>(List(percentilesToReport.size) { 0L }))
            }
            // The pool invariant holds at this point: all lists in the pool are of the correct
            // size.
        }

        /**
         * Number of times [intervalQueue] had to allocate a new [ConcurrentIntervalQueue.Interval].
         */
        @VisibleForTesting fun numLateIntervalAllocations() = intervalQueue.numLateAllocations

        @UiThread
        fun aggregate(startNanos: Long, endNanos: Long) {
            intervalQueue.record(startNanos, endNanos)
        }

        @UiThread
        fun reportSynchronously() {
            runBlocking { calculateAndReportPercentiles() }
        }

        suspend fun calculateAndReportPercentiles() {
            // Copy all active samples into the scratch list.
            scratchDurationsNanos.clear()
            while (!intervalQueue.isEmpty()) {
                intervalQueue.applyToOldestAndRecycle { interval ->
                    scratchDurationsNanos.add(interval.endNanos - interval.startNanos)
                }
            }
            if (scratchDurationsNanos.isEmpty()) {
                return
            }
            scratchDurationsNanos.sort()

            var report = obtainPercentileReportList()
            percentilesToReport.forEachIndexed { i, percentile ->
                report[i] = scratchDurationsNanos.getPercentile(percentile)
            }

            callback.onLatencyPercentiles(report, scratchDurationsNanos.size)
            recyclePercentileReportList(report)
        }

        fun obtainPercentileReportList(): MutableList<Long> {
            // If the pool is non-empty, the pool invariant guarantees that the list we get has
            // length
            // `percentilesToReport.size`. If it's empty, then the code constructs a list of that
            // length.
            // So either way, recycling this list into the pool later will maintain the invariant.
            return percentilePool.removeFirstOrNull()
                ?: ArrayList<Long>(List(percentilesToReport.size) { 0L }).also {
                    numLatePercentileListAllocations += 1
                    Log.w(
                        this::class.simpleName,
                        "Percentile pool is empty; allocating a new List. " +
                            "Report callbacks are taking too long.",
                    )
                }
        }

        private fun recyclePercentileReportList(report: MutableList<Long>) {
            if (report.size == percentilesToReport.size) {
                percentilePool.add(report)
                // The two lines above maintain the pool invariant: all lists in the pool are of the
                // correct
                // size (namely, `percentilesToReport.size`).
            } else {
                // This should never happen, since we control all calls to this method. The `report`
                // either
                // came from the pool initially (in which case the invariant guarantees it's of the
                // correct
                // size) or it was newly allocated in `obtainPercentileReportList`, which guarantees
                // its
                // output is of the correct size.
                Log.e(
                    this::class.simpleName,
                    "Tried to recycle a report list of size ${report.size} but expected " +
                        " ${percentilesToReport.size}. This is a significant logic error in this class.",
                )
            }
        }

        companion object {
            const val ESTIMATED_MAX_REPORT_DURATION_SECONDS: Float = 0.5f
            const val PERCENTILE_POOL_INITIAL_CAPACITY: Int = 5

            /**
             * Gets a single percentile value for a sorted list of [Long]s. The receiver list must
             * be sorted. This method does not check input preconditions.
             *
             * @param percentile Between 0.0f and 100.0f inclusive. For example, 50f means the
             *   median, 90f means 90th percentile, etc.
             */
            fun List<Long>.getPercentile(percentile: Float): Long {
                // Short-circuit the 100% case for safety. If this check and the `fraction < 1e-6`
                // logic
                // below were both missing, then the `this[index+1]` at the end would be an
                // out-of-bounds
                // access for percentile=1.0.
                if (percentile == 100f) {
                    return this[this.size - 1]
                }
                val fractionalIndex: Double = (percentile.toDouble() / 100.0) * (this.size - 1)
                val index = fractionalIndex.toInt()
                val fraction = fractionalIndex - index
                if (fraction < 1e-6) {
                    return this[index]
                } else if ((1.0 - fraction) < 1e-6) {
                    return this[index + 1]
                }
                // Standard form is (1-a)*x + a*y. Rewritten: x + a*(y-x). (y-x) is expected to be
                // much
                // smaller than x or y in the [PercentileLatencyAggregator] use case, so this form
                // reduces
                // issues with precision and rounding.
                return this[index] + (fraction * (this[index + 1] - this[index])).roundToLong()
            }
        }
    }
}
