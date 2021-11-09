/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark

import androidx.annotation.RestrictTo

/**
 * Data capture from a single benchmark test method run.
 *
 * Each field directly corresponds to JSON output, though not every JSON object may be
 * represented directly here.
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BenchmarkResult(
    val className: String,
    val testName: String,
    val totalRunTimeNs: Long,
    val metrics: Measurements,
    val repeatIterations: Int,
    val thermalThrottleSleepSeconds: Long,
    val warmupIterations: Int
) {
    /**
     * Simplified constructor, without sampled metrics, for micro benchmarks.
     */
    constructor(
        className: String,
        testName: String,
        totalRunTimeNs: Long,
        metrics: List<MetricResult>,
        repeatIterations: Int,
        thermalThrottleSleepSeconds: Long,
        warmupIterations: Int
    ) : this(
        className = className,
        testName = testName,
        totalRunTimeNs = totalRunTimeNs,
        metrics = Measurements(
            singleMetrics = metrics,
            sampledMetrics = emptyList()
        ),
        repeatIterations = repeatIterations,
        thermalThrottleSleepSeconds = thermalThrottleSleepSeconds,
        warmupIterations = warmupIterations
    )
    public fun getMetricResult(which: String): MetricResult {
        return metrics.singleMetrics.first { it.name == which }
    }

    /**
     * Final metric results from a full benchmark test, merged across multiple iterations.
     */
    data class Measurements(
        val singleMetrics: List<MetricResult>,
        val sampledMetrics: List<MetricResult>
    ) {
        fun isNotEmpty() = singleMetrics.isNotEmpty() || sampledMetrics.isNotEmpty()
    }
}
