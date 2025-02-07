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

@file:RequiresApi(
    api = 31
) // To use Duration.dividedBy. In any case this file is only expected to be used in U+

package androidx.health.connect.client.impl.platform.aggregate

import androidx.annotation.RequiresApi
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.impl.platform.aggregate.AggregatorUtils.time
import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SeriesRecord
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Implementation of [Aggregator] that aggregates into [AggregationResultGroupedByDuration] buckets.
 *
 * @param initProcessor initialization function for an [AggregationProcessor] that does the actual
 *   computation per bucket.
 */
internal class ResultGroupedByDurationAggregator<T : Record>(
    private val timeRange: TimeRange<*>,
    private val bucketDuration: Duration,
    private val initProcessor: (InstantTimeRange) -> AggregationProcessor<T>
) : Aggregator<T, List<AggregationResultGroupedByDurationWithMinTime>> {

    private val instantTimeRange: InstantTimeRange =
        when (timeRange) {
            is InstantTimeRange -> timeRange
            is LocalTimeRange ->
                InstantTimeRange(
                    startTime = timeRange.startTime.toInstant(ZoneOffset.MAX),
                    endTime = timeRange.endTime.toInstant(ZoneOffset.MIN)
                )
        }

    private val bucketProcessors = mutableMapOf<Instant, AggregationProcessorWithZoneOffset<T>>()

    override fun filterAndAggregate(record: T) {
        if (!AggregatorUtils.contributesToAggregation(record, timeRange)) {
            return
        }

        var bucketStartTime =
            maxOf(
                instantTimeRange.startTime,
                when (record) {
                    is InstantaneousRecord -> getBucketStartTime(record.time)
                    is IntervalRecord -> getBucketStartTime(record.startTime)
                    else -> error("Unsupported value for aggregation: $record")
                }
            )

        val lastBucketStartTime =
            when (record) {
                is InstantaneousRecord -> bucketStartTime
                is IntervalRecord -> getBucketStartTime(record.endTime)
                else -> error("Unsupported value for aggregation: $record")
            }

        while (
            bucketStartTime <= lastBucketStartTime && bucketStartTime < instantTimeRange.endTime
        ) {
            val bucketTimeRange = getBucketTimeRange(bucketStartTime)
            if (AggregatorUtils.contributesToAggregation(record, bucketTimeRange)) {
                bucketProcessors
                    .getOrPut(bucketStartTime) {
                        AggregationProcessorWithZoneOffset(
                            initProcessor(bucketTimeRange),
                            bucketStartTime
                        )
                    }
                    .processRecord(record)
            }
            bucketStartTime += bucketDuration
        }
    }

    override fun getResult(): List<AggregationResultGroupedByDurationWithMinTime> {
        return bucketProcessors.values.map {
            val bucketTimeRange = getBucketTimeRange(it.bucketStartTime)

            val zoneOffset =
                it.zoneOffset ?: ZoneId.systemDefault().rules.getOffset(it.minTime ?: Instant.now())

            AggregationResultGroupedByDurationWithMinTime(
                aggregationResultGroupedByDuration =
                    AggregationResultGroupedByDuration(
                        result = it.getProcessedAggregationResult(),
                        startTime = bucketTimeRange.startTime,
                        endTime = bucketTimeRange.endTime,
                        zoneOffset = zoneOffset
                    ),
                minTime = it.minTime ?: Instant.MAX
            )
        }
    }

    private fun getBucketStartTime(time: Instant): Instant {
        return instantTimeRange.startTime +
            bucketDuration.multipliedBy(
                Duration.between(instantTimeRange.startTime, time).dividedBy(bucketDuration)
            )
    }

    private fun getBucketTimeRange(bucketStartTime: Instant): InstantTimeRange {
        val bucketEndTime = minOf(bucketStartTime + bucketDuration, instantTimeRange.endTime)
        return InstantTimeRange(bucketStartTime, bucketEndTime)
    }
}

private class AggregationProcessorWithZoneOffset<T : Record>(
    private val delegate: AggregationProcessor<T>,
    val bucketStartTime: Instant
) : AggregationProcessor<T> by delegate {

    var zoneOffset: ZoneOffset? = null
    var minTime: Instant? = null

    override fun processRecord(record: T) {
        val recordTime =
            when (record) {
                is InstantaneousRecord -> record.time
                is SeriesRecord<*> ->
                    // For series record there will be at least one sample that satisfies the
                    // condition below.
                    // By this point only records that have samples within the time range are being
                    // processed.
                    record.samples.filter { it.time >= bucketStartTime }.minOf { it.time }
                is IntervalRecord -> record.startTime
                else -> error("Unsupported record $record")
            }
        val recordZoneOffset =
            when (record) {
                is InstantaneousRecord -> record.zoneOffset
                is IntervalRecord -> record.startZoneOffset
                else -> error("Unsupported record $record")
            }
        if (minTime == null || recordTime < minTime) {
            minTime = recordTime
            zoneOffset = recordZoneOffset
        }
        delegate.processRecord(record)
    }
}

internal data class AggregationResultGroupedByDurationWithMinTime(
    val aggregationResultGroupedByDuration: AggregationResultGroupedByDuration,
    val minTime: Instant
)
