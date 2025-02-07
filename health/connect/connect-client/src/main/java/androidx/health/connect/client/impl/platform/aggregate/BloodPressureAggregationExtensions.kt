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

@file:RequiresApi(api = 34)

package androidx.health.connect.client.impl.platform.aggregate

import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.units.Pressure
import kotlin.math.max
import kotlin.math.min

private val BLOOD_PRESSURE_METRICS =
    setOf(
        BloodPressureRecord.DIASTOLIC_AVG,
        BloodPressureRecord.DIASTOLIC_MAX,
        BloodPressureRecord.DIASTOLIC_MIN,
        BloodPressureRecord.SYSTOLIC_AVG,
        BloodPressureRecord.SYSTOLIC_MAX,
        BloodPressureRecord.SYSTOLIC_MIN,
    )

internal suspend fun HealthConnectClient.aggregateBloodPressure(
    aggregateRequest: AggregateGroupByDurationRequest
): List<AggregationResultGroupedByDurationWithMinTime> {
    return aggregate(
        ReadRecordsRequest(
            BloodPressureRecord::class,
            aggregateRequest.timeRangeFilter,
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByDurationAggregator(
            createTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            BloodPressureAggregationProcessor(aggregateRequest.metrics)
        }
    )
}

internal suspend fun HealthConnectClient.aggregateBloodPressure(
    aggregateRequest: AggregateGroupByPeriodRequest
): List<AggregationResultGroupedByPeriod> {
    return aggregate(
        ReadRecordsRequest(
            BloodPressureRecord::class,
            aggregateRequest.timeRangeFilter,
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByPeriodAggregator(
            createLocalTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            BloodPressureAggregationProcessor(aggregateRequest.metrics)
        }
    )
}

internal suspend fun HealthConnectClient.aggregateBloodPressure(
    aggregateRequest: AggregateRequest
): AggregationResult {
    return aggregate(
        ReadRecordsRequest(
            BloodPressureRecord::class,
            aggregateRequest.timeRangeFilter,
            aggregateRequest.dataOriginFilter
        ),
        ResultAggregator(
            createTimeRange(aggregateRequest.timeRangeFilter),
            BloodPressureAggregationProcessor(aggregateRequest.metrics)
        )
    )
}

internal class BloodPressureAggregationProcessor(val metrics: Set<AggregateMetric<*>>) :
    AggregationProcessor<BloodPressureRecord> {
    private val avgDataMap = mutableMapOf<AggregateMetric<Pressure>, AvgData>()
    private val minMaxMap = mutableMapOf<AggregateMetric<Pressure>, Double?>()
    private val dataOrigins = mutableSetOf<DataOrigin>()

    init {
        check(BLOOD_PRESSURE_METRICS.containsAll(metrics)) {
            "Invalid set of blood pressure fallback aggregation metrics ${metrics.map { it.metricKey }}"
        }

        for (metric in metrics) {
            when (metric) {
                BloodPressureRecord.DIASTOLIC_AVG,
                BloodPressureRecord.SYSTOLIC_AVG -> avgDataMap[metric] = AvgData()
                BloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_MAX,
                BloodPressureRecord.SYSTOLIC_MIN -> minMaxMap[metric] = null
                else ->
                    error("Invalid blood pressure fallback aggregation metric ${metric.metricKey}")
            }
        }
    }

    override fun processRecord(record: BloodPressureRecord) {
        val diastolic = record.diastolic.inMillimetersOfMercury
        val systolic = record.systolic.inMillimetersOfMercury

        for (metric in metrics) {
            when (metric) {
                BloodPressureRecord.DIASTOLIC_AVG -> avgDataMap[metric]!! += diastolic
                BloodPressureRecord.DIASTOLIC_MAX ->
                    minMaxMap[metric] = max(minMaxMap[metric] ?: diastolic, diastolic)
                BloodPressureRecord.DIASTOLIC_MIN ->
                    minMaxMap[metric] = min(minMaxMap[metric] ?: diastolic, diastolic)
                BloodPressureRecord.SYSTOLIC_AVG -> avgDataMap[metric]!! += systolic
                BloodPressureRecord.SYSTOLIC_MAX ->
                    minMaxMap[metric] = max(minMaxMap[metric] ?: systolic, systolic)
                BloodPressureRecord.SYSTOLIC_MIN ->
                    minMaxMap[metric] = min(minMaxMap[metric] ?: systolic, systolic)
            }

            dataOrigins += record.metadata.dataOrigin
        }
    }

    override fun getProcessedAggregationResult(): AggregationResult {
        val doubleValues =
            if (dataOrigins.isEmpty()) emptyMap()
            else
                metrics.associateBy({ it.metricKey }) { metric ->
                    when (metric) {
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_AVG -> avgDataMap[metric]!!.average()
                        BloodPressureRecord.DIASTOLIC_MAX,
                        BloodPressureRecord.DIASTOLIC_MIN,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN -> minMaxMap[metric]!!
                        else ->
                            error(
                                "Invalid blood pressure fallback aggregation type ${metric.metricKey}"
                            )
                    }
                }

        return AggregationResult(emptyMap(), doubleValues, dataOrigins)
    }
}
