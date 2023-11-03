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
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_WRITE_EXERCISE_ROUTE
import androidx.health.connect.client.permission.HealthPermission.Companion.getWritePermission
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

private val START_TIME = Instant.ofEpochMilli(1234L)
private val END_TIME = Instant.ofEpochMilli(5678L)
private val START_ZONE_OFFSET = ZoneOffset.UTC
private val END_ZONE_OFFSET = ZoneOffset.UTC

@Sampled
suspend fun InsertSteps(healthConnectClient: HealthConnectClient) {
    val stepsRecord =
        StepsRecord(
            count = 120,
            startTime = START_TIME,
            endTime = END_TIME,
            startZoneOffset = START_ZONE_OFFSET,
            endZoneOffset = END_ZONE_OFFSET,
        )
    healthConnectClient.insertRecords(listOf(stepsRecord))
}

@Sampled
suspend fun InsertNutrition(healthConnectClient: HealthConnectClient) {
    val banana =
        NutritionRecord(
            name = "banana",
            energy = 105.0.kilocalories,
            dietaryFiber = 3.1.grams,
            potassium = 0.422.grams,
            totalCarbohydrate = 27.0.grams,
            totalFat = 0.4.grams,
            saturatedFat = 0.1.grams,
            sodium = 0.001.grams,
            sugar = 14.0.grams,
            vitaminB6 = 0.0005.grams,
            vitaminC = 0.0103.grams,
            startTime = START_TIME,
            endTime = END_TIME,
            startZoneOffset = START_ZONE_OFFSET,
            endZoneOffset = END_ZONE_OFFSET,
        )
    healthConnectClient.insertRecords(listOf(banana))
}

@Sampled
suspend fun InsertHeartRateSeries(healthConnectClient: HealthConnectClient) {
    val heartRateRecord =
        HeartRateRecord(
            startTime = START_TIME,
            startZoneOffset = START_ZONE_OFFSET,
            endTime = END_TIME,
            endZoneOffset = END_ZONE_OFFSET,
            // records 10 arbitrary data, to replace with actual data
            samples =
                List(10) { index ->
                    HeartRateRecord.Sample(
                        time = START_TIME + Duration.ofSeconds(index.toLong()),
                        beatsPerMinute = 100 + index.toLong(),
                    )
                },
        )
    healthConnectClient.insertRecords(listOf(heartRateRecord))
}

@Sampled
suspend fun InsertExerciseRoute(healthConnectClient: HealthConnectClient) {
    val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

    if (!grantedPermissions.contains(getWritePermission(ExerciseSessionRecord::class))) {
        return
    }

    val sessionStartTime = Instant.parse("2023-07-11T10:00:00.00Z")
    val sessionDuration = Duration.ofMinutes(10)

    val startLatitude = 51.511831
    val endLatitude = 51.506007
    val startLongitude = -0.165785
    val endLongitude = -0.164888
    val latitudeDeltaPerSecond = (endLatitude - startLatitude) / sessionDuration.seconds
    val longitudeDeltaPerSecond = (endLongitude - startLongitude) / sessionDuration.seconds

    val exerciseRoute =
        if (grantedPermissions.contains(PERMISSION_WRITE_EXERCISE_ROUTE)) {
            ExerciseRoute(
                List(sessionDuration.seconds.toInt()) { timeSeconds ->
                    ExerciseRoute.Location(
                        time = sessionStartTime.plusSeconds(timeSeconds.toLong()),
                        latitude = startLatitude + latitudeDeltaPerSecond * timeSeconds,
                        longitude = startLongitude + longitudeDeltaPerSecond * timeSeconds,
                        horizontalAccuracy = Length.meters(2.0),
                        verticalAccuracy = Length.meters(2.0),
                        altitude = Length.meters(19.0)
                    )
                }
            )
        } else {
            null
        }

    val exerciseSessionRecord =
        ExerciseSessionRecord(
            startTime = sessionStartTime,
            startZoneOffset = ZoneOffset.UTC,
            endTime = sessionStartTime.plus(sessionDuration),
            endZoneOffset = ZoneOffset.UTC,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Morning Run",
            notes = "A nice run in a park",
            exerciseRoute = exerciseRoute
        )

    healthConnectClient.insertRecords(listOf(exerciseSessionRecord))
}
