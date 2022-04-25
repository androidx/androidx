/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro

import androidx.benchmark.BenchmarkResult

/**
 * Metric results from a single macrobenchmark iteration.
 */
internal data class IterationResult(
    /**
     * Results for metrics that are measured once per iteration.
     */
    val singleMetrics: Map<String, Double>,

    /**
     * Results for metrics that are sampled multiple times per iteration, with all samples pooled.
     */
    val sampledMetrics: Map<String, List<Double>>,

    /**
     * Start of iteration relevant content, if easily provided, in trace-native nano timestamps.
     *
     * The union of all timelineRanges for a given iteration, if any are present, will determine
     * default zoom for that iteration's trace in Studio / Perfetto UI.
     */
    val timelineRangeNs: LongRange? = null
) {
    operator fun plus(element: IterationResult) = IterationResult(
        singleMetrics = singleMetrics + element.singleMetrics,
        sampledMetrics = sampledMetrics + element.sampledMetrics,
        timelineRangeNs = listOf(
            element.timelineRangeNs,
            this.timelineRangeNs
        ).mergeTimelineRangeNs()
    )

    private fun List<LongRange?>.mergeTimelineRangeNs(): LongRange? {
        filterNotNull().run {
            return if (isNotEmpty()) {
                (minOf { it.first })..(maxOf { it.last })
            } else {
                null
            }
        }
    }

    companion object {
        val EMPTY = IterationResult(
            singleMetrics = emptyMap(),
            sampledMetrics = emptyMap(),
            timelineRangeNs = null
        )
    }
}

internal fun List<IterationResult>.mergeIterationMeasurements() = BenchmarkResult.Measurements(
    singleMetrics = this.map { it.singleMetrics }.mergeToSingleMetricResults(),
    sampledMetrics = this.map { it.sampledMetrics }.mergeToSampledMetricResults()
)
