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
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.platform.aggregate.AggregatorUtils.time
import androidx.health.connect.client.impl.platform.aggregate.AggregatorUtils.value
import androidx.health.connect.client.impl.platform.isWithin
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.SeriesRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

private val RECORDS_TO_AGGREGATE_METRICS_INFO_MAP =
    mapOf(
        CyclingPedalingCadenceRecord::class to
            AggregateMetricsInfo(
                averageMetric = CyclingPedalingCadenceRecord.RPM_AVG,
                maxMetric = CyclingPedalingCadenceRecord.RPM_MAX,
                minMetric = CyclingPedalingCadenceRecord.RPM_MIN
            ),
        SpeedRecord::class to
            AggregateMetricsInfo(
                averageMetric = SpeedRecord.SPEED_AVG,
                maxMetric = SpeedRecord.SPEED_MAX,
                minMetric = SpeedRecord.SPEED_MIN
            ),
        StepsCadenceRecord::class to
            AggregateMetricsInfo(
                averageMetric = StepsCadenceRecord.RATE_AVG,
                maxMetric = StepsCadenceRecord.RATE_MAX,
                minMetric = StepsCadenceRecord.RATE_MIN
            )
    )

internal suspend inline fun <reified T : SeriesRecord<*>> HealthConnectClient.aggregateSeries(
    aggregateRequest: AggregateRequest
): AggregationResult {
    val timeRange = createTimeRange(aggregateRequest.timeRangeFilter)
    return aggregate(
        ReadRecordsRequest(
            T::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultAggregator(
            timeRange,
            SeriesAggregationProcessor(T::class, aggregateRequest.metrics, timeRange)
        )
    )
}

internal suspend inline fun <reified T : SeriesRecord<*>> HealthConnectClient.aggregateSeries(
    aggregateRequest: AggregateGroupByPeriodRequest
): List<AggregationResultGroupedByPeriod> {
    return aggregate(
        ReadRecordsRequest(
            T::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByPeriodAggregator(
            createLocalTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            SeriesAggregationProcessor(T::class, aggregateRequest.metrics, it)
        }
    )
}

internal suspend inline fun <reified T : SeriesRecord<*>> HealthConnectClient.aggregateSeries(
    aggregateRequest: AggregateGroupByDurationRequest
): List<AggregationResultGroupedByDurationWithMinTime> {
    return aggregate(
        ReadRecordsRequest(
            T::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByDurationAggregator(
            createTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            SeriesAggregationProcessor(T::class, aggregateRequest.metrics, it)
        }
    )
}

@VisibleForTesting
internal class SeriesAggregationProcessor<T : SeriesRecord<*>>(
    recordType: KClass<T>,
    val metrics: Set<AggregateMetric<*>>,
    val timeRange: TimeRange<*>,
) : AggregationProcessor<T> {
    val avgData = AvgData()
    var min: Double? = null
    var max: Double? = null

    private val dataOrigins = mutableSetOf<DataOrigin>()

    private val aggregateInfo =
        RECORDS_TO_AGGREGATE_METRICS_INFO_MAP[recordType]
            ?: throw IllegalArgumentException("Non supported fallback series record $recordType")

    init {
        check(
            setOf(aggregateInfo.averageMetric, aggregateInfo.minMetric, aggregateInfo.maxMetric)
                .containsAll(metrics)
        ) {
            "Invalid set of metrics ${metrics.map { it.metricKey }}"
        }
    }

    override fun processRecord(record: T) {
        record.samples
            .filter { it.time.isWithin(timeRange, record.startZoneOffset) }
            .forEach {
                avgData += it.value
                min = min(min ?: it.value, it.value)
                max = max(max ?: it.value, it.value)
            }
        dataOrigins += record.metadata.dataOrigin
    }

    override fun getProcessedAggregationResult(): AggregationResult {
        val doubleValues =
            if (dataOrigins.isEmpty()) emptyMap()
            else
                metrics.associateBy({ it.metricKey }) { metric ->
                    when (metric) {
                        aggregateInfo.averageMetric -> avgData.average()
                        aggregateInfo.maxMetric -> max!!
                        aggregateInfo.minMetric -> min!!
                        else -> error("Invalid fallback aggregation metric ${metric.metricKey}")
                    }
                }
        return AggregationResult(emptyMap(), doubleValues, dataOrigins)
    }
}

@VisibleForTesting
internal data class AggregateMetricsInfo<T : Any>(
    val averageMetric: AggregateMetric<T>,
    val minMetric: AggregateMetric<T>,
    val maxMetric: AggregateMetric<T>
)
