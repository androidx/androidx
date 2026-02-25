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
import kotlinx.coroutines.Job

/**
 * An aggregator for nanosecond-resolution latency measurements. Implementations of this class
 * perform different kinds of aggregation, for example rate-limiting or computation of windowed
 * summary statistics. An aggregator asynchronously reports "aggregates" — representative values or
 * summary statistics — to the client via a callback whose signature depends on the aggregator.
 *
 * Objects of this type are expected to be long-lived, typically for the entire lifetime of the
 * client app. It's expected that the client will call [aggregate] frequently, and will receive
 * reporting callbacks regularly for the entire lifetime of the aggregator.
 *
 * To disable or destroy an aggregator before the app shuts down, either cancel the concurrency
 * scope passed into the factory function for the chosen concrete subclass, or call
 * `job().cancelAndJoin()`.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalLatencyDataApi
public interface LatencyAggregator {
    /**
     * Inserts a latency measurement between [startNanos] and [endNanos] into the pool of values to
     * be aggregated. These values are expected to be offsets from some consistent time base; e.g.,
     * device boot time or the Unix epoch.
     */
    @UiThread public fun aggregate(startNanos: Long, endNanos: Long): Unit

    /**
     * For periodic aggregators, runs the reporting callback immediately and synchronously. For
     * other aggregators this function is a no-op.
     *
     * A periodic aggregator is one that reports at a regular cadence (except, perhaps, for cases
     * when there's nothing to report); for example, one might call the reporting callback every 15
     * seconds. In contrast, other aggregators report irregularly or on every input.
     *
     * This call does not modify the periodic reporting cadence. However, inputs are incorporated
     * into at most one report, so the next report will not include any contribution from inputs
     * received prior to this call.
     */
    @UiThread public fun reportSynchronously(): Unit

    /** Returns the long-running [Job] for aggregation and reporting. */
    public fun job(): Job
}
