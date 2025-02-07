/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalInsightApi::class)

package androidx.benchmark.traceprocessor

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import perfetto.protos.AndroidStartupMetric.SlowStartReason
import perfetto.protos.AndroidStartupMetric.ThresholdValue.ThresholdUnit
import perfetto.protos.TraceMetrics

/**
 * Convert the SlowStartReason concept from Perfetto's android_startup metric to the Macrobenchmark
 * generic Insight format
 */
private fun SlowStartReason.toInsight(
    packageName: String,
    helpUrlBase: String?,
    traceLinkTitle: String,
    traceLinkPath: String,
): Insight {
    val thresholdUnit = expected_value!!.unit!!
    val unitSuffix =
        when (thresholdUnit) {
            ThresholdUnit.NS -> "ns"
            ThresholdUnit.PERCENTAGE -> "%"
            ThresholdUnit.COUNT -> " count"
            ThresholdUnit.TRUE_OR_FALSE -> ""
            else -> " ${thresholdUnit.toString().lowercase()}"
        }

    val thresholdValue = expected_value.value_!!

    val thresholdString =
        StringBuilder()
            .apply {
                append(" (expected: ")
                if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                    when (thresholdValue) {
                        0L -> append("false")
                        1L -> append("true")
                        else ->
                            throw IllegalArgumentException(
                                "Unexpected boolean value $thresholdValue"
                            )
                    }
                } else {
                    if (expected_value.higher_expected == true) append("> ")
                    if (expected_value.higher_expected == false) append("< ")
                    append(thresholdValue)
                    append(unitSuffix)
                }
                append(")")
            }
            .toString()

    val category =
        Insight.Category(
            titleUrl = helpUrlBase?.plus(reason_id!!.name),
            title = reason!!,
            postTitleLabel = thresholdString
        )

    val observedValue = requireNotNull(actual_value?.value_)
    return Insight(
        observedLabel =
            if (thresholdUnit == ThresholdUnit.TRUE_OR_FALSE) {
                require(observedValue in 0L..1L)
                if (observedValue == 0L) "false" else "true"
            } else {
                "$observedValue$unitSuffix"
            },
        traceLink =
            PerfettoTrace.Link(
                path = traceLinkPath,
                title = traceLinkTitle,
                urlParamMap =
                    mapOf(
                        "AndroidStartup:packageName" to packageName,
                        "AndroidStartup:slowStartReasonId" to reason_id!!.name
                    ),
            ),
        category = category,
    )
}

@ExperimentalInsightApi
public class StartupInsights
@RestrictTo(LIBRARY_GROUP)
constructor(private val helpUrlBase: String?) : Insight.Provider {
    public constructor() : this(null)

    override fun queryInsights(
        session: TraceProcessor.Session,
        packageName: String,
        traceLinkTitle: String,
        traceLinkPath: String,
    ): List<Insight> {
        return TraceMetrics.ADAPTER.decode(
                session.queryMetricsProtoBinary(listOf("android_startup"))
            )
            .android_startup
            ?.startup
            ?.filter { it.package_name == packageName } // TODO: fuzzy match?
            ?.flatMap { it.slow_start_reason_with_details }
            ?.map {
                it.toInsight(
                    packageName = packageName,
                    helpUrlBase = helpUrlBase,
                    traceLinkTitle = traceLinkTitle,
                    traceLinkPath = traceLinkPath
                )
            } ?: emptyList()
    }
}
