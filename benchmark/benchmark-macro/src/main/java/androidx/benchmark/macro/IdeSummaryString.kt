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
import androidx.benchmark.MetricResult
import androidx.benchmark.Outputs

/**
 * Returns a pair of ideSummaryStrings - v1 (pre Arctic-fox) and v2 (Arctic-fox+)
 *
 * These strings are to be displayed in Studio, depending on the version.
 *
 * The V2 string embeds links to trace files relative to the output path sent to the IDE via
 * `[link](file://<relative/path/to/trace>)`
 *
 * @see androidx.benchmark.InstrumentationResultScope#ideSummaryRecord
 */
internal fun ideSummaryStrings(
    warningLines: String,
    benchmarkName: String,
    measurements: BenchmarkResult.Measurements,
    absoluteTracePaths: List<String>
): Pair<String, String> {
    require(measurements.isNotEmpty()) { "Require non-empty list of metric results." }
    val allMetrics = measurements.singleMetrics + measurements.sampledMetrics

    val maxLabelLength = allMetrics.maxOf { it.name.length }

    fun Double.toDisplayString() = "%,.1f".format(this)

    // max string length of any printed min/median/max is the largest max value seen. used to pad.
    val maxValueLength = allMetrics
        .maxOf { it.max }
        .toDisplayString().length

    fun ideSummaryString(
        singleTransform: (
            name: String,
            min: String,
            median: String,
            max: String,
            metricResult: MetricResult
        ) -> String
    ) = (
        listOf(warningLines + benchmarkName) +
            measurements.singleMetrics.map {
                singleTransform(
                    it.name.padStart(maxLabelLength),
                    it.min.toDisplayString().padStart(maxValueLength),
                    it.median.toDisplayString().padStart(maxValueLength),
                    it.max.toDisplayString().padStart(maxValueLength),
                    it
                )
            } +
            measurements.sampledMetrics.map {
                val name = it.name.padStart(maxLabelLength)
                val p50 = it.p50.toDisplayString()
                val p90 = it.p90.toDisplayString()
                val p95 = it.p95.toDisplayString()
                val p99 = it.p99.toDisplayString()
                // we don't try and link percentiles, since they're grouped across multiple iters
                "  $name   P50  $p50,   P90  $p90,   P95  $p95,   P99  $p99"
            }
        ).joinToString("\n") + "\n"

    val relativeTracePaths = absoluteTracePaths.map { absolutePath ->
        Outputs.relativePathFor(absolutePath)
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
    return Pair(
        first = ideSummaryString(
            singleTransform = { name, min, median, max, _ ->
                "  $name   min $min,   median $median,   max $max"
            }
        ),
        second = ideSummaryString { name, min, median, max, metricResult ->
            "  $name" +
                "   [min $min](file://${relativeTracePaths[metricResult.minIndex]})," +
                "   [median $median](file://${relativeTracePaths[metricResult.medianIndex]})," +
                "   [max $max](file://${relativeTracePaths[metricResult.maxIndex]})"
        } + "    Traces: Iteration " + relativeTracePaths.mapIndexed { index, path ->
            "[$index](file://$path)"
        }.joinToString(separator = " ") + "\n"
    )
}