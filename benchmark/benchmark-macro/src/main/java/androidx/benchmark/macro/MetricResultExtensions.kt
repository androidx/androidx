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

import android.util.Log
import androidx.benchmark.Measurements
import androidx.benchmark.MetricResult
import kotlin.math.abs

/**
 * Asserts that the two lists of Measurements are equal with a threshold for data, ignoring list
 * order.
 *
 * @throws AssertionError
 */
@ExperimentalMetricApi
fun assertEqualMeasurements(
    expected: List<Metric.Measurement>,
    observed: List<Metric.Measurement>,
    threshold: Double
) {
    val expectedSorted = expected.sortedBy { it.name }
    val observedSorted = observed.sortedBy { it.name }
    val expectedNames = listOf(expectedSorted.map { it.name })
    val observedNames = listOf(observedSorted.map { it.name })
    if (expectedNames != observedNames) {
        throw AssertionError(
            "expected same measurement names, " +
                "expected = $expectedNames, observed = $observedNames"
        )
    }

    var errorString = ""
    expectedSorted.zip(observedSorted) { expectedMeasurement, observedMeasurement ->
        val name = expectedMeasurement.name
        if (expectedMeasurement.requireSingleValue != observedMeasurement.requireSingleValue) {
            errorString +=
                "expected value of requireSingleValue " +
                    "(${expectedMeasurement.requireSingleValue}) does not match observed " +
                    "value ${observedMeasurement.requireSingleValue}\n"
        }

        val expectedSamples = expectedMeasurement.data
        val observedSamples = observedMeasurement.data
        if (expectedSamples.size != observedSamples.size) {
            errorString +=
                "$name expected ${expectedSamples.size} samples," +
                    " observed ${observedSamples.size}\n"
        } else {
            expectedSamples.zip(observedSamples).forEachIndexed { index, pair ->
                if (abs(pair.first - pair.second) > threshold) {
                    errorString +=
                        "$name sample $index observed ${pair.second}, which is" +
                            " more than $threshold from expected ${pair.first}\n"
                }
            }
        }
    }

    if (errorString.isNotBlank()) {
        throw AssertionError(errorString)
    }
}

internal fun List<Metric.Measurement>.merge(
    other: List<Metric.Measurement>
): List<Metric.Measurement> {
    val nameSet = this.map { it.name }.toSet()
    val otherNameSet = other.map { it.name }.toSet()
    val intersectingNames = nameSet.intersect(otherNameSet)
    if (intersectingNames.isNotEmpty()) {
        throw IllegalStateException(
            "Multiple metrics produced " + "measurements with overlapping names: $intersectingNames"
        )
    }
    return this + other
}

/**
 * Takes a `List<List<Measurement>>`, one for each iteration, and transposes the data to be
 * organized by Measurement name, with data merged into a `MetricResult`.
 *
 * For requireSingleValue Measurements, this becomes a MetricResult used to extract min/med/max.
 *
 * For !requireSingleValue SubResults, this becomes a MetricResult used to extract P50/P90/P95/P99
 * from a flattened list of all samples, pooled together.
 */
internal fun List<List<Metric.Measurement>>.mergeMultiIterResults() =
    Measurements(
        singleMetrics =
            this.map {
                    it.filter { measurement -> measurement.requireSingleValue }
                        .associate { singleResult ->
                            singleResult.name to singleResult.data.first()
                        }
                }
                .mergeToSingleMetricResults(),
        sampledMetrics =
            this.map {
                    it.filter { measurement -> !measurement.requireSingleValue }
                        .associate { singleResult -> singleResult.name to singleResult.data }
                }
                .mergeToSampledMetricResults()
    )

/** Merge the Map<String, Long> results from each iteration into one List<MetricResult> */
internal fun List<Map<String, Double>>.mergeToSingleMetricResults(): List<MetricResult> {
    val setOfAllKeys = flatMap { it.keys }.toSet()

    // build Map<String, List<Long>>
    val listResults: Map<String, List<Double>> =
        setOfAllKeys.associateWith { key ->
            mapIndexedNotNull { iteration, resultMap ->
                if (resultMap.keys != setOfAllKeys) {
                    // TODO: assert that metrics are always captured (b/193827052)
                    Log.d(
                        TAG,
                        "Skipping results from iter $iteration, it didn't capture all metrics"
                    )
                    null
                } else {
                    resultMap[key] ?: error("Metric $key not observed in iteration")
                }
            }
        }

    // transform to List<MetricResult>, sorted by metric name
    return listResults
        .map { (metricName, values) -> MetricResult(name = metricName, data = values) }
        .sortedBy { it.name }
}

/** Merge the Map<String, List<Long>> results from each iteration into one List<MetricResult> */
internal fun List<Map<String, List<Double>>>.mergeToSampledMetricResults(): List<MetricResult> {
    val setOfAllKeys = flatMap { it.keys }.toSet()

    // build Map<String, List<List<Long>>>
    val listResults =
        setOfAllKeys.associateWith { key ->
            mapIndexed { index: Int, iterationSamples: Map<String, List<Double>> ->
                iterationSamples[key]
                    ?: throw IllegalStateException("Iteration $index didn't capture metric $key")
            }
        }

    // transform to List<MetricResult>, sorted by metric name
    return listResults
        .map { (metricName, values) ->
            MetricResult(name = metricName, data = values.flatten(), iterationData = values)
        }
        .sortedBy { it.name }
}
