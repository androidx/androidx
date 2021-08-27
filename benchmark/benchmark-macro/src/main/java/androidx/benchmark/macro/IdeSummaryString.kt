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
import androidx.benchmark.Outputs
import java.util.Collections

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
    metricResults: List<MetricResult>,
    absoluteTracePaths: List<String>
): Pair<String, String> {
    require(metricResults.isNotEmpty()) { "Require non-empty list of metric results." }

    val maxLabelLength = Collections.max(metricResults.map { it.name.length })

    fun Double.toDisplayString() = "%,.1f".format(this)

    // max string length of any printed min/median/max is the largest max value seen. used to pad.
    val maxValueLength = metricResults
        .maxOf { it.max }
        .toDisplayString().length

    fun ideSummaryString(
        transform: (
            name: String,
            min: String,
            median: String,
            max: String,
            metricResult: MetricResult
        ) -> String
    ): String {
        return warningLines + benchmarkName + "\n" + metricResults.joinToString("\n") {
            transform(
                it.name.padStart(maxLabelLength),
                it.min.toDisplayString().padStart(maxValueLength),
                it.median.toDisplayString().padStart(maxValueLength),
                it.max.toDisplayString().padStart(maxValueLength),
                it
            )
        } + "\n"
    }
    val relativeTracePaths = absoluteTracePaths.map { absolutePath ->
        Outputs.relativePathFor(absolutePath)
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
    return Pair(
        first = ideSummaryString { name, min, median, max, _ ->
            "  $name   min $min,   median $median,   max $max"
        },
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