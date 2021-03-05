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

import androidx.benchmark.MetricResult

/**
 * Merge the Map<String, Long> results from each iteration into one List<MetricResult>
 */
internal fun List<Map<String, Long>>.mergeToMetricResults(
    tracePaths: List<String>
): List<MetricResult> {
    val setOfAllKeys = flatMap { it.keys }.toSet()

    // validate each key shows up in each iteration
    val iterationErrorStrings = mapIndexedNotNull { iteration, iterationResults ->
        if (iterationResults.keys != setOfAllKeys) {
            "Iteration $iteration missing keys " + (setOfAllKeys - iterationResults.keys)
        } else null
    }
    if (iterationErrorStrings.isNotEmpty()) {
        throw IllegalStateException(
            "Error, different metrics observed in different iterations.\n\n" +
                iterationErrorStrings.joinToString("\n") +
                "Please report a bug, and include a logcat capture, and all traces captured by " +
                "this test run:\n" + tracePaths.joinToString("\n") + "\n" +
                DeviceInfo.deviceSummaryString
        )
    }

    // build Map<String, List<Long>>
    val listResults: Map<String, List<Long>> = setOfAllKeys.map { key ->
        key to map {
            it[key] ?: error("Metric $key not observed in iteration")
        }
    }.toMap()

    // transform to List<MetricResult>, sorted by metric name
    return listResults.map { (metricName, values) ->
        MetricResult(metricName, values.toLongArray())
    }.sortedBy { it.stats.name }
}