/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.benchmark.Insight
import androidx.benchmark.Markdown
import androidx.benchmark.Outputs
import androidx.benchmark.StartupInsightsConfig
import java.net.URLEncoder
import perfetto.protos.AndroidStartupMetric.SlowStartReason
import perfetto.protos.AndroidStartupMetric.ThresholdValue.ThresholdUnit

/**
 * Aggregates raw SlowStartReason results into a list of [Insight]s - in a format easier to display
 * in the IDE as a summary.
 *
 * TODO(353692849): add unit tests
 */
internal fun createInsightsIdeSummary(
    rawInsights: List<List<SlowStartReason>>,
    startupInsightsConfig: StartupInsightsConfig?,
    tracePaths: List<String>
): List<Insight> {
    fun createInsightString(
        criterion: SlowStartReason,
        observed: List<IndexedValue<SlowStartReason>>
    ): Insight {
        observed.forEach {
            require(it.value.reason_id == criterion.reason_id)
            require(it.value.expected_value == criterion.expected_value)
        }

        val expectedValue = requireNotNull(criterion.expected_value)
        val thresholdUnit = requireNotNull(expectedValue.unit)
        require(thresholdUnit != ThresholdUnit.THRESHOLD_UNIT_UNSPECIFIED)
        val unitSuffix =
            when (thresholdUnit) {
                ThresholdUnit.NS -> "ns"
                ThresholdUnit.PERCENTAGE -> "%"
                ThresholdUnit.COUNT -> " count"
                ThresholdUnit.TRUE_OR_FALSE -> ""
                else -> " ${thresholdUnit.toString().lowercase()}"
            }

        val criterionString = buildString {
            val reasonHelpUrlBase = startupInsightsConfig?.reasonHelpUrlBase
            if (reasonHelpUrlBase != null) {
                append("[")
                append(requireNotNull(criterion.reason).replace("]", "\\]"))
                append("]")
                append("(")
                append(reasonHelpUrlBase.replace(")", "\\)")) // base url
                val reasonId = requireNotNull(criterion.reason_id).name
                append(URLEncoder.encode(reasonId, Charsets.UTF_8.name())) // reason id as a suffix
                append(")")
            } else {
                append(requireNotNull(criterion.reason))
            }

            val thresholdValue = requireNotNull(expectedValue.value_)
            append(" (expected: ")
            if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                require(thresholdValue in 0L..1L)
                if (thresholdValue == 0L) append("false")
                if (thresholdValue == 1L) append("true")
            } else {
                if (expectedValue.higher_expected == true) append("> ")
                if (expectedValue.higher_expected == false) append("< ")
                append(thresholdValue)
                append(unitSuffix)
            }
            append(")")
        }

        val observedString =
            observed.joinToString(" ", "seen in iterations: ") {
                val actualValue = requireNotNull(it.value.actual_value?.value_)
                val actualString: String =
                    if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                        require(actualValue in 0L..1L)
                        if (actualValue == 0L) "false" else "true"
                    } else {
                        "$actualValue$unitSuffix"
                    }

                // TODO(364590575): implement zoom-in on relevant parts of the trace and then make
                //  the 'actualString' also part of the link.
                val relativePath =
                    tracePaths.getOrNull(it.index)?.let { Outputs.relativePathFor(it) }
                val traceLink =
                    when (relativePath) {
                        null -> "${it.index}"
                        else -> Markdown.createFileLink("${it.index}", relativePath)
                    }

                "$traceLink($actualString)"
            }

        return Insight(criterionString, observedString)
    }

    // Pivot from List<iteration_id -> insight_list> to List<insight -> iteration_list>
    // and convert to a format expected in Studio text output.
    return rawInsights
        .flatMapIndexed { iterationId, insights -> insights.map { IndexedValue(iterationId, it) } }
        .groupBy { it.value.reason_id }
        .values
        .map { createInsightString(it.first().value, it) }
}
