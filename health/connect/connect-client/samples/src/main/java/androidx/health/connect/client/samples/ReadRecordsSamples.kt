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

import androidx.activity.result.ActivityResultCaller
import androidx.annotation.Sampled
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.contracts.ExerciseRouteRequestContract
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

@Sampled
suspend fun ReadStepsRange(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    for (stepRecord in response.records) {
        // Process each step record
    }
}

@Sampled
suspend fun ReadExerciseSessions(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.readRecords(
            ReadRecordsRequest(
                ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    for (exerciseRecord in response.records) {
        // Process each exercise record
        // Optionally pull in with other data sources of the same time range.
        val heartRateRecords =
            healthConnectClient
                .readRecords(
                    ReadRecordsRequest(
                        HeartRateRecord::class,
                        timeRangeFilter =
                            TimeRangeFilter.between(
                                exerciseRecord.startTime,
                                exerciseRecord.endTime
                            )
                    )
                )
                .records
    }
}

@Sampled
suspend fun ReadExerciseRoute(
    activityResultCaller: ActivityResultCaller,
    healthConnectClient: HealthConnectClient,
    displayExerciseRoute: (ExerciseRoute.Data) -> Unit,
    recordId: String
) {
    // See https://developer.android.com/training/basics/intents/result#launch for appropriately
    // handling ActivityResultContract.
    val requestExerciseRoute =
        activityResultCaller.registerForActivityResult(ExerciseRouteRequestContract()) {
            exerciseRoute: ExerciseRoute.Data? ->
            if (exerciseRoute != null) {
                displayExerciseRoute(exerciseRoute)
            } else {
                // Consent was denied
            }
        }

    // Show exercise route, based on user action
    val exerciseSessionRecord =
        healthConnectClient.readRecord(ExerciseSessionRecord::class, recordId).record

    when (val exerciseRoute = exerciseSessionRecord.exerciseRoute) {
        is ExerciseRoute.Data -> displayExerciseRoute(exerciseRoute)
        is ExerciseRoute.ConsentRequired -> requestExerciseRoute.launch(recordId)
        is ExerciseRoute.NoData -> Unit // No exercise route to show
        else -> Unit
    }
}

@Sampled
suspend fun ReadSleepSessions(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.readRecords(
            ReadRecordsRequest(
                SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    for (sleepRecord in response.records) {
        // Process each exercise record
        // Optionally pull in sleep stages of the same time range
        val sleepStageRecords =
            healthConnectClient
                .readRecords(
                    ReadRecordsRequest(
                        SleepStageRecord::class,
                        timeRangeFilter =
                            TimeRangeFilter.between(sleepRecord.startTime, sleepRecord.endTime)
                    )
                )
                .records
    }
}
