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
import androidx.annotation.VisibleForTesting
import androidx.ink.authoring.ExperimentalLatencyDataApi
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * A [LatencyAggregator] that reports latency samples with a fixed probability.
 *
 * Usage example in a `DocumentActivity`:
 * ```
 * // Report about one out of every thousand latency intervals by picking each one randomly with
 * // 0.1% probability.
 * val aggregator = FixedProbabilityLatencyAggregator.create(
 *   sampleProbability = 1f / 1000f,
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
 *
 * The correct choice of `sampleProbability` depends on the rate at which you expect to call
 * [aggregate] and the rate at which you want reports to be issued. For example, a typical stylus
 * may report move events between 60 and 500 times per second; in typical usage, each such event
 * results in a call to [aggregate]. Say your stylus reports 250 times per second and you want to
 * report an average of 1 value per 5 seconds. Then `sampleProbability` should be `1f / (250 * 5)`.
 *
 * However, every sample is reported with this same probability independently, so if one sample gets
 * reported, you have a `1/(250*5)` chance of the very next sample also getting reported. Reporting
 * multiple samples in quick succession may, depending on your logging framework and device
 * characteristics, have a temporary performance impact on your app.
 *
 * Say we define a "double-up report" as a situation in which two samples are reported within 10ms
 * of each other. Continuing the above example, if a user uses your app for 10 minutes, they have a
 * 16% chance of a double-up report at some point in that session. And if 100 users each have
 * 10-minute sessions, then there's a 99.9999985% chance that at least one user will have a
 * double-up report in at least one session.
 *
 * In general, if you get `r` samples per second, you sample with probability `p`, you have `N`
 * users with equal session lengths of `L` seconds each, and your definition of a double-up is two
 * reports within `t` seconds, then the probability of at least one user having at least one session
 * with at least one double-up report is approximately:
 * ```
 * 1 - (t * r * p * (1-p)^(t * r - 1) + (1-p)^(t * r))^(N * (L-t) * r)
 * ```
 *
 * Note that this formula is a continuous approximation of a discrete probability distribution, and
 * so the result is valid only for values of `t` that are multiples of `1/r` seconds. In particular,
 * the result is negative if `t < 1/r`, but the actual probability is 0.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalLatencyDataApi
public class FixedProbabilityLatencyAggregator
private constructor(
    private val sampleProbability: Float,
    private val scope: CoroutineScope,
    private val randomSource: Random,
    private val callback: Callback,
) : LatencyAggregator {

    private val supervisorJob = SupervisorJob(parent = scope.coroutineContext[Job])

    public fun interface Callback {
        /**
         * Callback invoked to report selected start and end values passed to [aggregate]. This
         * callback runs in the [CoroutineScope] (or, for Java clients, the
         * [ScheduledExecutorService]) passed to [create].
         */
        public suspend fun onLatencySample(startNanos: Long, endNanos: Long): Unit
    }

    @UiThread
    public override fun aggregate(startNanos: Long, endNanos: Long) {
        if (randomSource.nextFloat() < sampleProbability) {
            (scope + supervisorJob).launch { callback.onLatencySample(startNanos, endNanos) }
        }
    }

    /** This method is a no-op for this aggregator. */
    @UiThread public override fun reportSynchronously() {}

    public override fun job(): Job = supervisorJob

    public companion object {
        /**
         * Returns a new [FixedProbabilityLatencyAggregator]. For use by Kotlin clients. [callback]
         * will be called in the given [scope], using its default [CoroutineContext].
         *
         * [callback] runs immediately when a sample is selected for reporting. See the
         * documentation on the [FixedProbabilityLatencyAggregator] class itself for cautions about
         * potential performance impact and guidance on picking the correct [sampleProbability].
         *
         * @param sampleProbability The probability of reporting each aggregated sample to
         *   [callback].
         * @param scope The scope in which to call the callback. The scope's associated context
         *   should be distinct from the UI thread.
         * @param callback The [Callback] with which to report selected samples.
         */
        @JvmStatic
        public fun create(
            sampleProbability: Float,
            scope: CoroutineScope,
            callback: Callback,
        ): FixedProbabilityLatencyAggregator {
            check(sampleProbability >= 0f && sampleProbability <= 1f) {
                "sampleProbability must be in [0, 1]"
            }
            return create(sampleProbability, scope, Random.Default, callback)
        }

        /**
         * Returns a new [FixedProbabilityLatencyAggregator]. For use by Java clients. [callback]
         * will be called on the given [executor].
         *
         * [callback] runs immediately when a sample is selected for reporting. See the
         * documentation on the [FixedProbabilityLatencyAggregator] class itself for cautions about
         * potential performance impact and guidance on picking the correct [sampleProbability].
         *
         * @param sampleProbability The probability of reporting each aggregated sample to
         *   [callback].
         * @param executor An [Executor] on which to call the callback. Should be distinct from the
         *   UI thread. More efficient if the [Executor] is an instance of
         *   [ScheduledExecutorService].
         * @param callback The [Callback] with which to report selected samples.
         */
        @JvmStatic
        public fun create(
            sampleProbability: Float,
            executor: Executor,
            callback: Callback,
        ): FixedProbabilityLatencyAggregator {
            return create(
                sampleProbability,
                CoroutineScope(executor.asCoroutineDispatcher()),
                Random.Default,
                callback,
            )
        }

        /**
         * Same as the other [create] that takes a [CoroutineScope], but with the ability to specify
         * a [Random] implementation.
         */
        @VisibleForTesting
        internal fun create(
            sampleProbability: Float,
            scope: CoroutineScope,
            randomSource: Random,
            callback: Callback,
        ): FixedProbabilityLatencyAggregator {
            check(sampleProbability >= 0f && sampleProbability <= 1f) {
                "sampleProbability must be in [0, 1]"
            }
            return FixedProbabilityLatencyAggregator(
                sampleProbability,
                scope,
                randomSource,
                callback,
            )
        }

        /**
         * Same as the other [create] that takes an [Executor], but with the ability to specify a
         * [Random] implementation.
         */
        @VisibleForTesting
        internal fun create(
            sampleProbability: Float,
            executor: ScheduledExecutorService,
            randomSource: Random,
            callback: Callback,
        ): FixedProbabilityLatencyAggregator {
            return create(
                sampleProbability,
                CoroutineScope(executor.asCoroutineDispatcher()),
                randomSource,
                callback,
            )
        }
    }
}
