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
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest

internal suspend fun HealthConnectClient.aggregateNutritionTransFatTotal(
    aggregateRequest: AggregateRequest
): AggregationResult {
    val timeRange = createTimeRange(aggregateRequest.timeRangeFilter)
    return aggregate(
        ReadRecordsRequest(
            NutritionRecord::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultAggregator(timeRange, TransFatTotalAggregationProcessor(timeRange))
    )
}

internal suspend fun HealthConnectClient.aggregateNutritionTransFatTotal(
    aggregateRequest: AggregateGroupByPeriodRequest
): List<AggregationResultGroupedByPeriod> {
    return aggregate(
        ReadRecordsRequest(
            NutritionRecord::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByPeriodAggregator(
            createLocalTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            TransFatTotalAggregationProcessor(it)
        }
    )
}

internal suspend fun HealthConnectClient.aggregateNutritionTransFatTotal(
    aggregateRequest: AggregateGroupByDurationRequest
): List<AggregationResultGroupedByDurationWithMinTime> {
    return aggregate(
        ReadRecordsRequest(
            NutritionRecord::class,
            aggregateRequest.timeRangeFilter.withBufferedStart(),
            aggregateRequest.dataOriginFilter
        ),
        ResultGroupedByDurationAggregator(
            createTimeRange(aggregateRequest.timeRangeFilter),
            aggregateRequest.timeRangeSlicer
        ) {
            TransFatTotalAggregationProcessor(it)
        }
    )
}

@VisibleForTesting
internal class TransFatTotalAggregationProcessor(private val timeRange: TimeRange<*>) :
    AggregationProcessor<NutritionRecord> {

    private var total = 0.0
    private val dataOrigins = mutableSetOf<DataOrigin>()

    override fun processRecord(record: NutritionRecord) {
        total += record.transFat!!.inGrams * AggregatorUtils.sliceFactor(record, timeRange)
        dataOrigins += record.metadata.dataOrigin
    }

    override fun getProcessedAggregationResult(): AggregationResult {
        val doubleValues =
            if (dataOrigins.isEmpty()) emptyMap()
            else mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to total)
        return AggregationResult(emptyMap(), doubleValues, dataOrigins)
    }
}
