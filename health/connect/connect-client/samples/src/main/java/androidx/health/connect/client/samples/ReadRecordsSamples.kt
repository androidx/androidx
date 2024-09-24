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
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.contracts.ExerciseRouteRequestContract
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND
import androidx.health.connect.client.readRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

@OptIn(ExperimentalFeatureAvailabilityApi::class)
@Sampled
suspend fun ReadSkinTemperatureRecord(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    if (
        healthConnectClient.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    ) {
        if (
            healthConnectClient.permissionController
                .getGrantedPermissions()
                .contains(HealthPermission.getReadPermission(SkinTemperatureRecord::class))
        ) {
            val response =
                healthConnectClient.readRecords(
                    ReadRecordsRequest<SkinTemperatureRecord>(
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                    )
                )
            for (skinTemperatureRecord in response.records) {
                // Process each skin temperature record
            }
        } else {
            // Permission hasn't been granted. Request permission to read skin temperature.
        }
    } else {
        // Feature is not available. It is not possible to read skin temperature.
    }
}

@Sampled
suspend fun ReadStepsRange(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    val response =
        healthConnectClient.readRecords(
            ReadRecordsRequest<StepsRecord>(
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
            ReadRecordsRequest<ExerciseSessionRecord>(
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    for (exerciseRecord in response.records) {
        // Process each exercise record
        // Optionally pull in with other data sources of the same time range.
        val heartRateRecords =
            healthConnectClient
                .readRecords(
                    ReadRecordsRequest<HeartRateRecord>(
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
    displayExerciseRoute: (ExerciseRoute) -> Unit,
    recordId: String
) {
    // See https://developer.android.com/training/basics/intents/result#launch for appropriately
    // handling ActivityResultContract.
    val requestExerciseRoute =
        activityResultCaller.registerForActivityResult(ExerciseRouteRequestContract()) {
            exerciseRoute: ExerciseRoute? ->
            if (exerciseRoute != null) {
                displayExerciseRoute(exerciseRoute)
            } else {
                // Consent was denied
            }
        }

    // Show exercise route, based on user action
    val exerciseSessionRecord =
        healthConnectClient.readRecord<ExerciseSessionRecord>(recordId).record

    when (val exerciseRouteResult = exerciseSessionRecord.exerciseRouteResult) {
        is ExerciseRouteResult.Data -> displayExerciseRoute(exerciseRouteResult.exerciseRoute)
        is ExerciseRouteResult.ConsentRequired -> requestExerciseRoute.launch(recordId)
        is ExerciseRouteResult.NoData -> Unit // No exercise route to show
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
            ReadRecordsRequest<SleepSessionRecord>(
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
        )
    for (sleepRecord in response.records) {
        // Process each sleep record
    }
}

@Sampled
suspend fun ReadRecordsInBackground(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant,
) {
    val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

    // The permission should be requested and granted beforehand when the app is in foreground
    if (PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND !in grantedPermissions) {
        return
    }

    val response =
        healthConnectClient.readRecords(
            ReadRecordsRequest<StepsRecord>(
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            )
        )

    for (stepsRecord in response.records) {
        // Process each record
    }
}
