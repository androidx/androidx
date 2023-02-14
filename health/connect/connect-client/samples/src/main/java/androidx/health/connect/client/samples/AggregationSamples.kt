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

@file:Suppress("UNUSED_VARIABLE")

package androidx.health.connect.client.samples

import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.Period

@Sampled
suspend fun AggregateDistance(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    // The result may be null if no data is available in the time range.
    val distanceTotalInMeters = response[DistanceRecord.DISTANCE_TOTAL]?.inMeters
}

@Sampled
suspend fun AggregateHeartRate(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.aggregate(
            AggregateRequest(
                setOf(HeartRateRecord.BPM_MAX, HeartRateRecord.BPM_MIN),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    // The result may be null if no data is available in the time range.
    val minimumHeartRate = response[HeartRateRecord.BPM_MIN]
    val maximumHeartRate = response[HeartRateRecord.BPM_MAX]
}

@Sampled
suspend fun AggregateIntoMinutes(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                timeRangeSlicer = Duration.ofMinutes(1)
            )
        )
    for (monthlyResult in response) {
        // The result may be null if no data is available in the time range.
        val totalSteps = monthlyResult.result[StepsRecord.COUNT_TOTAL]
    }
}

@Sampled
suspend fun AggregateIntoMonths(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                timeRangeSlicer = Period.ofMonths(1)
            )
        )
    for (monthlyResult in response) {
        // The result may be null if no data is available in the time range.
        val totalSteps = monthlyResult.result[StepsRecord.COUNT_TOTAL]
    }
}
