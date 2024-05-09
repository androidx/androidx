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
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.fold

internal suspend fun HealthConnectClient.aggregateNutritionTransFatTotal(
    timeRangeFilter: TimeRangeFilter,
    dataOriginFilter: Set<DataOrigin>
): AggregationResult {
    val readRecordsFlow = readRecordsFlow(
        NutritionRecord::class,
        timeRangeFilter.withBufferedStart(),
        dataOriginFilter
    )

    val aggregatedData = readRecordsFlow
        .fold(AggregatedData(0.0)) { currentAggregatedData, records ->
            val filteredRecords = records.filter {
                it.overlaps(timeRangeFilter) && it.transFat != null &&
                    sliceFactor(it, timeRangeFilter) > 0
            }

            filteredRecords.forEach {
                currentAggregatedData.value +=
                    it.transFat!!.inGrams * sliceFactor(it, timeRangeFilter)
            }

            filteredRecords.mapTo(currentAggregatedData.dataOrigins) { it.metadata.dataOrigin }
            currentAggregatedData
        }

    if (aggregatedData.dataOrigins.isEmpty()) {
        return emptyAggregationResult()
    }

    return AggregationResult(
        longValues = mapOf(),
        doubleValues = mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to aggregatedData.value),
        dataOrigins = aggregatedData.dataOrigins
    )
}
