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

package androidx.health.connect.client.testing.testdata

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.testing.FakeHealthConnectClient
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import java.time.Duration
import java.time.Period
import java.time.ZonedDateTime

// Arbitrary start time in the past
private val startTime: ZonedDateTime = ZonedDateTime.now().minusHours(23)

val runRecord1 =
    ExerciseSessionRecord(
        startTime = startTime.plusMinutes(1).toInstant(),
        startZoneOffset = startTime.offset,
        endTime = startTime.plusMinutes(2).toInstant(),
        endZoneOffset = startTime.offset,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        title = "My Run #1",
        exerciseRoute = null,
        metadata =
            Metadata(
                clientRecordId = "FakeHealthConnectData1",
                id = "Id1",
                dataOrigin = DataOrigin(FakeHealthConnectClient.DEFAULT_PACKAGE_NAME)
            )
    )

val hydrationRecord1 =
    HydrationRecord(
        startTime = startTime.plusMinutes(3).toInstant(),
        startZoneOffset = startTime.offset,
        endTime = startTime.plusMinutes(4).toInstant(),
        endZoneOffset = startTime.offset,
        volume = Volume.liters(1.0)
    )

/** Same as [runRecord1] but updated with a new end time. */
val runRecord1Updated =
    ExerciseSessionRecord(
        startTime = startTime.plusMinutes(1).toInstant(),
        startZoneOffset = startTime.offset,
        endTime = startTime.plusMinutes(3).toInstant(),
        endZoneOffset = startTime.offset,
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        title = "My Run #1 - Updated",
        exerciseRoute = null,
        metadata =
            Metadata(
                clientRecordId = "FakeHealthConnectData1",
                id = "Id1",
                dataOrigin = DataOrigin(FakeHealthConnectClient.DEFAULT_PACKAGE_NAME)
            )
    )

/**
 * Generates a list of [ExerciseSessionRecord]s of type
 * [ExerciseSessionRecord.EXERCISE_TYPE_RUNNING], one per day ending now, 1h long.
 */
@JvmOverloads
fun generateRunningRecords(
    amount: Int,
    startTime: ZonedDateTime = ZonedDateTime.now().minusDays(amount.toLong()),
    exerciseType: Int = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
    defaultPackageName: String = FakeHealthConnectClient.DEFAULT_PACKAGE_NAME
): List<ExerciseSessionRecord> {
    return List(amount) { index ->
        val day = startTime.plusDays(index.toLong())
        ExerciseSessionRecord(
            startTime = day.minusMinutes(60).toInstant(),
            startZoneOffset = startTime.offset,
            endTime = day.toInstant(),
            endZoneOffset = startTime.offset,
            exerciseType = exerciseType,
            title = "My Run #$index",
            exerciseRoute = null,
            metadata =
                Metadata(
                    clientRecordId = "FakeHealthConnectDataRunning$index",
                    dataOrigin = DataOrigin(defaultPackageName)
                )
        )
    }
}

/* Test dummies */

val dummyAggregateRequest =
    AggregateRequest(
        metrics = emptySet(),
        timeRangeFilter =
            TimeRangeFilter(startTime = runRecord1.startTime, endTime = runRecord1.endTime)
    )

val dummyReadRecordsRequest =
    ReadRecordsRequest(
        timeRangeFilter =
            TimeRangeFilter(startTime = runRecord1.startTime, endTime = runRecord1.endTime),
        recordType = runRecord1::class
    )

val dummyAggregateGbpRequest =
    AggregateGroupByPeriodRequest(
        metrics = emptySet(),
        timeRangeFilter = TimeRangeFilter(),
        timeRangeSlicer = Period.ofDays(1)
    )

val dummyAggregateGbdRequest =
    AggregateGroupByDurationRequest(
        metrics = emptySet(),
        timeRangeFilter =
            TimeRangeFilter(startTime = runRecord1.startTime, endTime = runRecord1.endTime),
        timeRangeSlicer = Duration.ofMillis(98765),
    )
