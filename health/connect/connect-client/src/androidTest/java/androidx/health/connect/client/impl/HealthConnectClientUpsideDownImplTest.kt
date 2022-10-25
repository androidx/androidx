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

package androidx.health.connect.client.impl

import android.annotation.TargetApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.lang.UnsupportedOperationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators thats lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU, codeName = "UpsideDownCake")
class HealthConnectClientUpsideDownImplTest {

    private lateinit var healthConnectClient: HealthConnectClient

    @Before
    fun setUp() {
        healthConnectClient =
            HealthConnectClientUpsideDownImpl(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun filterGrantedPermissions_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.permissionController.filterGrantedPermissions(
                setOf(HealthPermission.READ_ACTIVE_CALORIES_BURNED)
            )
        }
    }

    @Test
    fun getGrantedPermission_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.permissionController.getGrantedPermissions(
                setOf(
                    HealthPermission.createReadPermission(
                        StepsRecord::class,
                    )
                )
            )
        }
    }

    @Test
    @Ignore("For unclear reason this is throwing exception from the system")
    fun insertRecords() = runTest {
        val response = healthConnectClient.insertRecords(listOf(
            StepsRecord(
                count = 100,
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(5678L),
                endZoneOffset = null
            )
        ))
        assertThat(response.recordIdsList).hasSize(1)
    }

    @Test
    fun updateRecords_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.updateRecords(listOf())
        }
    }

    @Test
    fun readRecord_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.readRecord(StepsRecord::class, "id")
        }
    }

    @Test
    fun readRecords_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.between(
                        Instant.ofEpochMilli(1234L),
                        Instant.ofEpochMilli(1235L)
                    )
                )
            )
        }
    }

    @Test
    fun aggregateRecords_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        Instant.ofEpochMilli(1234L),
                        Instant.ofEpochMilli(1235L)
                    )
                )
            )
        }
    }

    @Test
    fun aggregateRecordsGroupByDuration_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        Instant.ofEpochMilli(1234L),
                        Instant.ofEpochMilli(1235L)
                    ),
                    timeRangeSlicer = Duration.ofMillis(1)
                )
            )
        }
    }

    @Test
    fun aggregateRecordsGroupByPeriod_throwUOE() = runTest {
        assertFailsWith<UnsupportedOperationException> {
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        LocalDateTime.of(2018, 10, 11, 7, 10),
                        LocalDateTime.of(2018, 10, 13, 7, 10),
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
        }
    }
}
