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
import androidx.health.connect.client.deleteRecords
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant

@Sampled
suspend fun DeleteByUniqueIdentifier(
    healthConnectClient: HealthConnectClient,
    uid1: String,
    uid2: String
) {
    healthConnectClient.deleteRecords<StepsRecord>(
        recordIdsList = listOf(uid1, uid2),
        clientRecordIdsList = emptyList()
    )
}

@Sampled
suspend fun DeleteByTimeRange(
    healthConnectClient: HealthConnectClient,
    startTime: Instant,
    endTime: Instant
) {
    healthConnectClient.deleteRecords<StepsRecord>(
        timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
    )
}
