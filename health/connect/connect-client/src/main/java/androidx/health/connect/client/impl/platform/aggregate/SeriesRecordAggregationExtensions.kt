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
import androidx.health.connect.client.impl.platform.toInstantWithDefaultZoneFallback
import androidx.health.connect.client.impl.platform.useLocalTime
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.SeriesRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneOffset
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

internal suspend fun HealthConnectClient.aggregateCyclingPedalingCadence(
    metrics: Set<AggregateMetric<*>>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
) =
    aggregateSeriesRecord(
        recordType = CyclingPedalingCadenceRecord::class,
        aggregateMetrics = metrics,
        timeRangeFilter = timeRangeFilter,
        dataOriginFilter = dataOriginFilter
    ) {
        samples.map { SampleInfo(time = it.time, value = it.revolutionsPerMinute) }
    }

internal suspend fun HealthConnectClient.aggregateSpeed(
    metrics: Set<AggregateMetric<*>>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
) =
    aggregateSeriesRecord(
        recordType = SpeedRecord::class,
        aggregateMetrics = metrics,
        timeRangeFilter = timeRangeFilter,
        dataOriginFilter = dataOriginFilter
    ) {
        samples.map { SampleInfo(time = it.time, value = it.speed.inMetersPerSecond) }
    }

internal suspend fun HealthConnectClient.aggregateStepsCadence(
    metrics: Set<AggregateMetric<*>>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
) =
    aggregateSeriesRecord(
        recordType = StepsCadenceRecord::class,
        aggregateMetrics = metrics,
        timeRangeFilter = timeRangeFilter,
        dataOriginFilter = dataOriginFilter
    ) {
        samples.map { SampleInfo(time = it.time, value = it.rate) }
    }

@VisibleForTesting
internal suspend fun <T : SeriesRecord<*>> HealthConnectClient.aggregateSeriesRecord(
    recordType: KClass<T>,
    aggregateMetrics: Set<AggregateMetric<*>>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>,
    getSampleInfo: T.() -> List<SampleInfo>
) =
    aggregateSeriesRecord(
        recordType,
        timeRangeFilter,
        dataOriginFilter,
        SeriesAggregator(recordType, aggregateMetrics),
        getSampleInfo
    )

private suspend fun <T : SeriesRecord<*>> HealthConnectClient.aggregateSeriesRecord(
    recordType: KClass<T>,
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>,
    aggregator: Aggregator<RecordInfo>,
    getSampleInfo: T.() -> List<SampleInfo>
): AggregationResult {
    val readRecordsFlow =
        readRecordsFlow(recordType, timeRangeFilter.withBufferedStart(), dataOriginFilter)

    readRecordsFlow.collect { records ->
        records
            .asSequence()
            .map {
                RecordInfo(
                    dataOrigin = it.metadata.dataOrigin,
                    samples =
                        it.getSampleInfo().filter { sample ->
                            sample.isWithin(
                                timeRangeFilter = timeRangeFilter,
                                zoneOffset = it.startZoneOffset
                            )
                        }
                )
            }
            .filter { it.samples.isNotEmpty() }
            .forEach { recordInfo -> aggregator += recordInfo }
    }

    return aggregator.getResult()
}

private class SeriesAggregator<T : SeriesRecord<*>>(
    recordType: KClass<T>,
    val aggregateMetrics: Set<AggregateMetric<*>>
) : Aggregator<RecordInfo> {

    val avgData = AvgData()
    var min: Double? = null
    var max: Double? = null

    override val dataOrigins = mutableSetOf<DataOrigin>()

    override val doubleValues: Map<String, Double>
        get() = buildMap {
            for (metric in aggregateMetrics) {
                val result =
                    when (metric) {
                        aggregateInfo.averageMetric -> avgData.average()
                        aggregateInfo.maxMetric -> max!!
                        aggregateInfo.minMetric -> min!!
                        else -> error("Invalid fallback aggregation metric ${metric.metricKey}")
                    }
                put(metric.metricKey, result)
            }
        }

    val aggregateInfo =
        RECORDS_TO_AGGREGATE_METRICS_INFO_MAP[recordType]
            ?: throw IllegalArgumentException("Non supported fallback series record $recordType")

    init {
        check(
            setOf(aggregateInfo.averageMetric, aggregateInfo.minMetric, aggregateInfo.maxMetric)
                .containsAll(aggregateMetrics)
        ) {
            "Invalid set of metrics ${aggregateMetrics.map { it.metricKey }}"
        }
    }

    override fun plusAssign(value: RecordInfo) {
        value.samples.forEach {
            avgData += it.value
            min = min(min ?: it.value, it.value)
            max = max(max ?: it.value, it.value)
        }
        dataOrigins += value.dataOrigin
    }
}

@VisibleForTesting
internal data class AggregateMetricsInfo<T : Any>(
    val averageMetric: AggregateMetric<T>,
    val minMetric: AggregateMetric<T>,
    val maxMetric: AggregateMetric<T>
)

@VisibleForTesting
internal data class RecordInfo(val dataOrigin: DataOrigin, val samples: List<SampleInfo>)

@VisibleForTesting
internal data class SampleInfo(val time: Instant, val value: Double) {
    fun isWithin(timeRangeFilter: TimeRangeFilter, zoneOffset: ZoneOffset?): Boolean {
        if (timeRangeFilter.useLocalTime()) {
            if (
                timeRangeFilter.localStartTime != null &&
                    time.isBefore(
                        timeRangeFilter.localStartTime.toInstantWithDefaultZoneFallback(zoneOffset)
                    )
            ) {
                return false
            }
            if (
                timeRangeFilter.localEndTime != null &&
                    !time.isBefore(
                        timeRangeFilter.localEndTime.toInstantWithDefaultZoneFallback(zoneOffset)
                    )
            ) {
                return false
            }
            return true
        }
        if (timeRangeFilter.startTime != null && time.isBefore(timeRangeFilter.startTime)) {
            return false
        }
        if (timeRangeFilter.endTime != null && !time.isBefore(timeRangeFilter.endTime)) {
            return false
        }
        return true
    }
}
