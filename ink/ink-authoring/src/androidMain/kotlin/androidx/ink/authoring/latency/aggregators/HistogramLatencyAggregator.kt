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
import androidx.ink.authoring.latency.aggregators.internal.runEvery
import java.util.concurrent.Executor
import kotlin.collections.ArrayDeque
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A [LatencyAggregator] that reports counts of latencies that fall into user-configured buckets in
 * consecutive, non-overlapping time windows.
 *
 * N+1 buckets are configured by specifying N finite boundary values between them; each boundary is
 * an inclusive lower bound for one bucket. The first bucket is for underflow values: it goes from
 * -infinity to the first boundary value (exclusive). Then there are N-1 finite buckets, each of
 * which is inclusive of its lower bound and exclusive of its upper bound. The final bucket goes
 * from the last boundary value (inclusive) to +infinity.
 *
 * N=0 (no boundaries specified) is an allowed input; it produces a one-bucket "histogram" that
 * simply counts all latencies passed to the aggregator.
 *
 * Usage example in a `DocumentActivity`:
 * ```
 * val aggregator = HistogramLatencyAggregator.create(
 *   window = 60.seconds,
 *   // Three buckets: (-inf, 1ms), [1ms, 4ms), [4ms, +inf).
 *   inclusiveLowerBoundsNanos = listOf(1_000_000L, 4_000_000L),
 *   scope = lifecycleScope + backgroundCpuCoroutineContext,
 * ) {
 *  // This "reporting lambda" runs in backgroundCpuCoroutineContext. Here we just print out the
 *  // results, but a real caller could send them to a database, update a debug pane, etc.
 *  bucketCounts: IntArray -> Log.i("${bucketCounts.joinToString()}")
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
public class HistogramLatencyAggregator
private constructor(private val implementationHelper: ImplementationHelper) : LatencyAggregator {

    public fun interface Callback {
        /**
         * Callback invoked at most once per [ImplementationHelper.window] in the [CoroutineScope]
         * (or, for Java clients, on the [Executor]) passed to [create].
         *
         * @param bucketCounts An array of counts for each bucket (one more than the size of
         *   [ImplementationHelper.inclusiveLowerBoundsNanos]), giving the number of latency samples
         *   that fell in each bucket in the last reporting window. Do not hold a reference to this
         *   array after returning from the callback; it will be recycled (overwritten in place)
         *   immediately for use in a future callback.
         */
        public suspend fun onLatencyBuckets(bucketCounts: IntArray): Unit
    }

    @UiThread
    public override fun aggregate(startNanos: Long, endNanos: Long): Unit =
        implementationHelper.aggregate(startNanos, endNanos)

    @UiThread
    public override fun reportSynchronously(): Unit = implementationHelper.reportSynchronously()

    public override fun job(): Job = implementationHelper.supervisorJob

    @VisibleForTesting
    internal fun numLateHistogramAllocations() = implementationHelper.numLateHistogramAllocations

    public companion object {
        /**
         * Returns a new [HistogramLatencyAggregator]. For use by Kotlin clients. [callback] will be
         * called in the given [scope], using its default [kotlin.coroutines.CoroutineContext].
         *
         * @param window The length of the consecutive time windows in which to compute and report a
         *   histogram.
         * @param inclusiveLowerBoundsNanos The boundaries between buckets to report in the
         *   callback. N boundary values produce N+1 buckets with inclusive lower bounds and
         *   exclusive upper bounds. For example, `ListOf(1_000_000L, 5_000_000L)` means bucket
         *   boundaries of 1ms and 5ms, giving three buckets: (-inf, 1ms), [1ms, 5ms), and [5ms,
         *   +inf). An empty boundary list is valid and produces just 1 bucket. Otherwise, boundary
         *   values must be finite and strictly increasing.
         * @param scope The scope in which to aggregate and to call the callback. The scope's
         *   associated context should be distinct from the UI thread.
         * @param callback The [Callback] with which to report the histogram.
         */
        @JvmStatic
        public fun create(
            window: Duration,
            inclusiveLowerBoundsNanos: List<Long>,
            scope: CoroutineScope,
            callback: Callback,
        ): HistogramLatencyAggregator {
            validateConstructorParams(window, inclusiveLowerBoundsNanos)
            return HistogramLatencyAggregator(
                ImplementationHelper(window, inclusiveLowerBoundsNanos, scope, callback)
            )
        }

        /**
         * Returns a new [HistogramLatencyAggregator]. For use by Java clients. [callback] will be
         * called on the given [executor].
         *
         * @param windowMilliseconds The length, in milliseconds, of the consecutive time windows in
         *   which to compute and report a histogram.
         * @param inclusiveLowerBoundsNanos The boundaries between buckets to report in the
         *   callback. N boundary values produce N+1 buckets with inclusive lower bounds and
         *   exclusive upper bounds. For example, `ListOf(1_000_000L, 5_000_000L)` means bucket
         *   boundaries of 1ms and 5ms, giving three buckets: (-inf, 1ms), [1ms, 5ms), and [5ms,
         *   +inf). An empty boundary list is valid and produces just 1 bucket. Otherwise, boundary
         *   values must be finite and strictly increasing.
         * @param executor An [Executor] on which to compute and report histograms. Should be
         *   distinct from the UI thread. More efficient if the [Executor] is an instance of
         *   [java.util.concurrent.ScheduledExecutorService].
         * @param callback The [Callback] with which to report the histogram.
         */
        @JvmStatic
        public fun create(
            windowMilliseconds: Int,
            inclusiveLowerBoundsNanos: List<Long>,
            executor: Executor,
            callback: Callback,
        ): HistogramLatencyAggregator {
            return create(
                windowMilliseconds.milliseconds,
                inclusiveLowerBoundsNanos,
                CoroutineScope(executor.asCoroutineDispatcher()),
                callback,
            )
        }

        private fun validateConstructorParams(
            window: Duration,
            inclusiveLowerBoundsNanos: List<Long>,
        ) {
            check(window > 0.seconds) { "window must be be a positive duration" }
            for (i in 1..<inclusiveLowerBoundsNanos.size) {
                check(inclusiveLowerBoundsNanos[i] > inclusiveLowerBoundsNanos[i - 1]) {
                    "inclusiveLowerBoundsNanos must be an increasing sequence. " +
                        "#${i-1} is ${inclusiveLowerBoundsNanos[i-1]}; #${i} is ${inclusiveLowerBoundsNanos[i]}"
                }
            }
        }
    }

    private class ImplementationHelper(
        val window: Duration,
        val inclusiveLowerBoundsNanos: List<Long>,
        val scope: CoroutineScope,
        val callback: Callback,
    ) {
        private val numBuckets = inclusiveLowerBoundsNanos.size + 1

        /**
         * Channel for receiving nanosecond latency values. [aggregate] sends values, and
         * [continuallyIncrementBucketCounts] receives them.
         */
        // TODO: b/322501538 - Channel boxes its contents, which probably leads to allocations.
        private val latencyNanosChannel = Channel<Long>(Channel.UNLIMITED)

        /**
         * Counts of latencies in the different histogram buckets, in the current reporting window.
         */
        private var currentHistogram = IntArray(numBuckets)

        /**
         * A mutex to protect [currentHistogram]. [continuallyIncrementBucketCounts] wants to write
         * to it constantly, but [reportAndResetHistogram] swaps it out for a new one from time to
         * time.
         */
        private var currentHistogramMutex = Mutex()

        /**
         * Pool of recycled arrays for storing and reporting latency histograms. After initial
         * preallocation, this will only be accessed from [scope].
         *
         * Invariant: the length of every IntArray in the pool is `numBuckets`.
         */
        private val histogramPool = ArrayDeque<IntArray>(HISTOGRAM_POOL_INITIAL_CAPACITY)

        /** Number of times [obtainHistogram] had to allocate a new [IntArray]. */
        var numLateHistogramAllocations = 0

        /** The overall job for all concurrent work done in this class. */
        val supervisorJob = SupervisorJob(parent = scope.coroutineContext[Job])

        init {
            // Preallocate histogram arrays for later use.
            repeat(HISTOGRAM_POOL_INITIAL_CAPACITY) { histogramPool.add(IntArray(numBuckets)) }
            // The pool invariant holds at this point: all arrays in the pool are of the correct
            // length.

            (scope + supervisorJob).launch { continuallyIncrementBucketCounts() }
            (scope + supervisorJob).runEvery(window, ::reportAndResetHistogram)
        }

        @UiThread
        fun aggregate(startNanos: Long, endNanos: Long) {
            // Push each latency into the channel without blocking. Compute the latency outside the
            // launch
            // lambda so that startNanos and endNanos can be recycled as soon as this function
            // returns,
            // even if `send` gets delayed.
            val latencyNanos = endNanos - startNanos
            // TODO: b/322501538 - This may cause two allocations on a high-frequency path; one for
            // the
            // new Job made by `launch`, and one for the lambda.
            (scope + supervisorJob).launch { latencyNanosChannel.send(latencyNanos) }
        }

        @UiThread
        fun reportSynchronously() {
            runBlocking { reportAndResetHistogram() }
        }

        private suspend fun continuallyIncrementBucketCounts() {
            // As latencies come in on the channel, increment the appropriate bucket for each one.
            while (true) {
                var bucketIndex = getBucketIndex(latencyNanosChannel.receive())
                currentHistogramMutex.withLock { currentHistogram[bucketIndex] += 1 }
            }
        }

        /**
         * Report the current histogram, and simultaneously swap it out for a new, empty one to be
         * updated in the upcoming reporting window.
         */
        private suspend fun reportAndResetHistogram() {
            val histogram = currentHistogram
            // Swap out the old histogram with a new one, so that `continuallyIncrementBucketCounts`
            // can
            // start updating the new one.
            currentHistogramMutex.withLock { currentHistogram = obtainHistogram() }
            callback.onLatencyBuckets(histogram)
            recycleHistogram(histogram)
        }

        private fun obtainHistogram(): IntArray {
            // If the pool is non-empty, the pool invariant guarantees that the array we get has
            // length
            // `numBuckets`. If it's empty, then the code constructs an array of that length. So
            // either
            // way, recycling this array into the pool later will maintain the invariant.
            return histogramPool.removeFirstOrNull()
                ?: IntArray(numBuckets).also {
                    numLateHistogramAllocations += 1
                    Log.w(
                        this::class.simpleName,
                        "Histogram pool is empty; allocating a new array. Report callbacks taking too long.",
                    )
                }
        }

        private fun recycleHistogram(histogram: IntArray) {
            if (histogram.size == numBuckets) {
                for (i in histogram.indices) {
                    histogram[i] = 0
                }
                histogramPool.add(histogram)
            } else {
                // This should never happen, since we control all calls to this method. The
                // `histogram`
                // either came from the pool initially (in which case the invariant guarantees it's
                // of the
                // correct size) or it was newly allocated in `obtainHistogram`, which guarantees
                // its output
                // is of the correct size.
                Log.e(
                    this::class.simpleName,
                    "Tried to recycle a histogram of size ${histogram.size} but expected $numBuckets. " +
                        "This is a significant logic error in this class.",
                )
            }
        }

        private fun getBucketIndex(latencyNanos: Long): Int {
            // Bucket 0 is (-inf, inclusiveLowerBoundsNanos[0]).
            // Bucket 1 is [inclusiveLowerBoundsNanos[0], inclusiveLowerBoundsNanos[1]).
            // In general, for bucket i, its _exclusive_ upper bound is
            // inclusiveLowerBoundsNanos[i].
            for ((index, boundaryNanos) in inclusiveLowerBoundsNanos.withIndex()) {
                if (latencyNanos < boundaryNanos) {
                    return index
                }
            }
            // Note that numBuckets = inclusiveLowerBoundsNanos.size + 1. So the index of the final
            // bucket
            // (for overflow) is numBuckets - 1, or equivalently, inclusiveLowerBoundsNanos.size.
            return inclusiveLowerBoundsNanos.size
        }

        companion object {
            // The histogram pool only runs down if the client's reporting callback doesn't return
            // before
            // the next one is invoked. Normal uses of this aggregator should set very long
            // reporting
            // windows, so the pool should never run down, so the pool capacity can be small.
            private const val HISTOGRAM_POOL_INITIAL_CAPACITY: Int = 5
        }
    }
}
