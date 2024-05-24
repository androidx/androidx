/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle.skia

import androidx.benchmark.darwin.gradle.xcode.ActionTestSummary
import androidx.benchmark.darwin.gradle.xcode.ActionsInvocationRecord
import com.google.gson.annotations.SerializedName
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics

/** https://skia.googlesource.com/buildbot/+/refs/heads/main/perf/FORMAT.md */
data class Stat(val value: String, val measurement: Double)

data class Measurements(@SerializedName("stat") val stats: List<Stat>)

data class Metric(val key: Map<String, String>, val measurements: Measurements)

data class Metrics(
    val key: Map<String, String>,
    val results: List<Metric>,
    val version: Long = 1L,
    @SerializedName("git_hash") val referenceSha: String? = null
) {
    companion object {
        fun buildMetrics(
            record: ActionsInvocationRecord,
            summaries: List<ActionTestSummary>,
            referenceSha: String?,
        ): Metrics {
            require(record.actions.actionRecords.isNotEmpty())
            val runDestination = record.actions.actionRecords.first().runDestination
            val metricsKeys =
                mapOf(
                    "destination" to runDestination.displayName.value,
                    "arch" to runDestination.targetArchitecture.value,
                    "targetSdk" to runDestination.targetSDKRecord.identifier.value,
                    "identifier" to runDestination.localComputerRecord.identifier.value,
                    "modelName" to runDestination.localComputerRecord.modelName.value,
                    "modelCode" to runDestination.localComputerRecord.modelCode.value
                )
            val results = summaries.flatMap { it.toMetrics() }
            return Metrics(metricsKeys, results, referenceSha = referenceSha)
        }

        private fun ActionTestSummary.toMetrics(): List<Metric> {
            return performanceMetrics.values.map { metricSummary ->
                val key =
                    mutableMapOf(
                        "testDescription" to (title() ?: "No description"),
                        "metricName" to metricSummary.displayName.value,
                        "metricIdentifier" to metricSummary.identifier.value,
                        "polarity" to metricSummary.polarity.value,
                        "units" to metricSummary.unitOfMeasurement.value,
                    )
                val statistics = DescriptiveStatistics()
                metricSummary.measurements.values.forEach { statistics.addValue(it.value) }
                val min = Stat("min", statistics.min)
                // The 50th percentile is the median
                val median = Stat("median", statistics.getPercentile(50.0))
                val max = Stat("max", statistics.max)
                val deviation = Stat("stddev", statistics.standardDeviation)
                Metric(key, Measurements(listOf(min, median, max, deviation)))
            }
        }
    }
}
