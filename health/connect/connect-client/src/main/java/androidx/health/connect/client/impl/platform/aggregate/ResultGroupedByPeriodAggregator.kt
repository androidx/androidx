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

package androidx.health.connect.client.impl.platform.aggregate

import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.platform.toLocalTimeWithDefaultZoneFallback
import androidx.health.connect.client.records.InstantaneousRecord
import androidx.health.connect.client.records.IntervalRecord
import androidx.health.connect.client.records.Record
import java.time.LocalDateTime
import java.time.Period

/**
 * Implementation of [ResultGroupedByPeriodAggregator] that aggregates into
 * [AggregationResultGroupedByPeriod] buckets.
 *
 * @param initProcessor initialization function for an [AggregationProcessor] that does the actual
 *   computation per bucket.
 */
internal class ResultGroupedByPeriodAggregator<T : Record>(
    private val timeRange: LocalTimeRange,
    private val bucketPeriod: Period,
    private val initProcessor: (LocalTimeRange) -> AggregationProcessor<T>
) : Aggregator<T, List<AggregationResultGroupedByPeriod>> {

    private val bucketProcessors = mutableMapOf<LocalDateTime, AggregationProcessor<T>>()

    override fun filterAndAggregate(record: T) {
        if (!AggregatorUtils.contributesToAggregation(record, timeRange)) {
            return
        }

        var bucketStartTime =
            maxOf(
                timeRange.startTime,
                when (record) {
                    is InstantaneousRecord ->
                        getBucketStartTime(
                            record.time.toLocalTimeWithDefaultZoneFallback(record.zoneOffset)
                        )
                    is IntervalRecord ->
                        getBucketStartTime(
                            record.startTime.toLocalTimeWithDefaultZoneFallback(
                                record.startZoneOffset
                            )
                        )
                    else -> error("Unsupported value for aggregation: $record")
                }
            )

        val lastBucketStartTime =
            when (record) {
                is InstantaneousRecord -> bucketStartTime
                is IntervalRecord ->
                    getBucketStartTime(
                        record.endTime.toLocalTimeWithDefaultZoneFallback(record.endZoneOffset)
                    )
                else -> error("Unsupported value for aggregation: $record")
            }

        while (bucketStartTime <= lastBucketStartTime && bucketStartTime < timeRange.endTime) {
            val bucketTimeRange = getBucketTimeRange(bucketStartTime)
            if (AggregatorUtils.contributesToAggregation(record, bucketTimeRange)) {
                bucketProcessors
                    .getOrPut(bucketStartTime) { initProcessor(bucketTimeRange) }
                    .processRecord(record)
            }
            bucketStartTime += bucketPeriod
        }
    }

    override fun getResult(): List<AggregationResultGroupedByPeriod> {
        return bucketProcessors.map { (startTime, processor) ->
            AggregationResultGroupedByPeriod(
                result = processor.getProcessedAggregationResult(),
                startTime = startTime,
                endTime = getBucketTimeRange(startTime).endTime
            )
        }
    }

    private fun getBucketStartTime(time: LocalDateTime): LocalDateTime {
        var bucketEndTime = timeRange.startTime
        while (time >= bucketEndTime) {
            bucketEndTime += bucketPeriod
        }
        return bucketEndTime - bucketPeriod
    }

    private fun getBucketTimeRange(bucketStartTime: LocalDateTime): LocalTimeRange {
        val bucketEndTime = minOf(bucketStartTime + bucketPeriod, timeRange.endTime)
        return LocalTimeRange(bucketStartTime, bucketEndTime)
    }
}
