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
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.RemoteException
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
// Comment the SDK suppress to run on emulators lower than U.
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class HealthConnectClientUpsideDownImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val allHealthPermissions =
        context.packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
            .requestedPermissions
            .filter { it.startsWith(PERMISSION_PREFIX) }
            .toTypedArray()

    // Grant every permission as deletion by id checks for every permission
    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(*allHealthPermissions)

    private lateinit var healthConnectClient: HealthConnectClient

    @Before
    fun setUp() {
        healthConnectClient = HealthConnectClientUpsideDownImpl(context)
    }

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(StepsRecord::class, TimeRangeFilter.none())
        healthConnectClient.deleteRecords(HeartRateRecord::class, TimeRangeFilter.none())
        healthConnectClient.deleteRecords(NutritionRecord::class, TimeRangeFilter.none())
    }

    @Test
    fun insertRecords() = runTest {
        val response =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = null,
                        endTime = Instant.ofEpochMilli(5678L),
                        endZoneOffset = null
                    )
                )
            )
        assertThat(response.recordIdsList).hasSize(1)
    }

    @Test
    @Ignore("b/270954533")
    fun deleteRecords_byId() = runTest {
        val recordIds =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = Instant.ofEpochMilli(1234L),
                            startZoneOffset = null,
                            endTime = Instant.ofEpochMilli(5678L),
                            endZoneOffset = null
                        ),
                        StepsRecord(
                            count = 15,
                            startTime = Instant.ofEpochMilli(12340L),
                            startZoneOffset = null,
                            endTime = Instant.ofEpochMilli(56780L),
                            endZoneOffset = null
                        ),
                        StepsRecord(
                            count = 20,
                            startTime = Instant.ofEpochMilli(123400L),
                            startZoneOffset = null,
                            endTime = Instant.ofEpochMilli(567800L),
                            endZoneOffset = null,
                            metadata = Metadata(clientRecordId = "clientId")
                        ),
                    )
                )
                .recordIdsList

        val initialRecords =
            healthConnectClient
                .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                .records

        healthConnectClient.deleteRecords(
            StepsRecord::class,
            listOf(recordIds[1]),
            listOf("clientId")
        )

        assertThat(
                healthConnectClient
                    .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                    .records
            )
            .containsExactly(initialRecords[0])
    }

    // TODO(b/264253708): remove @Ignore from this test case once bug is resolved
    @Test
    @Ignore("Blocked while investigating b/264253708")
    fun deleteRecords_byTimeRange() = runTest {
        healthConnectClient
            .insertRecords(
                listOf(
                    StepsRecord(
                        count = 100,
                        startTime = Instant.ofEpochMilli(1_234L),
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = Instant.ofEpochMilli(5_678L),
                        endZoneOffset = ZoneOffset.UTC
                    ),
                    StepsRecord(
                        count = 150,
                        startTime = Instant.ofEpochMilli(12_340L),
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = Instant.ofEpochMilli(56_780L),
                        endZoneOffset = ZoneOffset.UTC
                    ),
                )
            )
            .recordIdsList

        val initialRecords =
            healthConnectClient
                .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                .records

        healthConnectClient.deleteRecords(
            StepsRecord::class,
            TimeRangeFilter.before(Instant.ofEpochMilli(10_000L))
        )

        assertThat(
                healthConnectClient
                    .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                    .records
            )
            .containsExactly(initialRecords[1])
    }

    @Test
    @Ignore("b/270954533")
    fun updateRecords() = runTest {
        val id =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = Instant.ofEpochMilli(1234L),
                            startZoneOffset = null,
                            endTime = Instant.ofEpochMilli(5678L),
                            endZoneOffset = null
                        )
                    )
                )
                .recordIdsList[0]

        val insertedRecord = healthConnectClient.readRecord(StepsRecord::class, id).record

        healthConnectClient.updateRecords(
            listOf(
                StepsRecord(
                    count = 5,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = null,
                    metadata = Metadata(id, insertedRecord.metadata.dataOrigin)
                )
            )
        )

        val updatedRecord = healthConnectClient.readRecord(StepsRecord::class, id).record

        assertThat(updatedRecord.count).isEqualTo(5L)
    }

    @Test
    @Ignore("b/270954533")
    fun readRecord_withId() = runTest {
        val insertResponse =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = Instant.ofEpochMilli(1234L),
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = Instant.ofEpochMilli(5678L),
                        endZoneOffset = ZoneOffset.UTC
                    )
                )
            )

        val readResponse =
            healthConnectClient.readRecord(StepsRecord::class, insertResponse.recordIdsList[0])

        with(readResponse.record) {
            assertThat(count).isEqualTo(10)
            assertThat(startTime).isEqualTo(Instant.ofEpochMilli(1234L))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo(Instant.ofEpochMilli(5678L))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
        }
    }

    @Test
    @Ignore("b/270954533")
    fun readRecords_withFilters() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 10,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = Instant.ofEpochMilli(12340L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(56780L),
                    endZoneOffset = ZoneOffset.UTC
                ),
            )
        )

        val readResponse =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.after(Instant.ofEpochMilli(10_000L))
                )
            )

        assertThat(readResponse.records[0].count).isEqualTo(5)
    }

    @Test
    @Ignore("b/270954533")
    fun readRecord_noRecords_throwRemoteException() = runTest {
        assertFailsWith<RemoteException> { healthConnectClient.readRecord(StepsRecord::class, "1") }
    }

    @Test
    @Ignore("b/270954533")
    fun aggregateRecords() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 10,
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = Instant.ofEpochMilli(12340L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(56780L),
                    endZoneOffset = ZoneOffset.UTC
                ),
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            HeartRateRecord.Sample(Instant.ofEpochMilli(1234L), 57L),
                            HeartRateRecord.Sample(Instant.ofEpochMilli(1235L), 120L)
                        )
                ),
                HeartRateRecord(
                    startTime = Instant.ofEpochMilli(12340L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(56780L),
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            HeartRateRecord.Sample(Instant.ofEpochMilli(12340L), 47L),
                            HeartRateRecord.Sample(Instant.ofEpochMilli(12350L), 48L)
                        )
                ),
                NutritionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(5678L),
                    endZoneOffset = ZoneOffset.UTC,
                    energy = Energy.kilocalories(200.0)
                ),
                WeightRecord(
                    time = Instant.ofEpochMilli(1234L),
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(100.0)
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(
                        StepsRecord.COUNT_TOTAL,
                        HeartRateRecord.BPM_MIN,
                        HeartRateRecord.BPM_MAX,
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.CAFFEINE_TOTAL,
                        WeightRecord.WEIGHT_MAX,
                        WheelchairPushesRecord.COUNT_TOTAL,
                    ),
                    TimeRangeFilter.none()
                )
            )

        with(aggregateResponse) {
            assertThat(this[StepsRecord.COUNT_TOTAL]).isEqualTo(15L)
            assertThat(this[HeartRateRecord.BPM_MIN]).isEqualTo(47L)
            assertThat(this[HeartRateRecord.BPM_MAX]).isEqualTo(120L)
            assertThat(this[NutritionRecord.ENERGY_TOTAL]).isEqualTo(Energy.kilocalories(200.0))
            assertThat(this[NutritionRecord.CAFFEINE_TOTAL]).isEqualTo(Mass.grams(0.0))
            assertThat(this[WeightRecord.WEIGHT_MAX]).isEqualTo(Mass.kilograms(100.0))

            assertThat(contains(WheelchairPushesRecord.COUNT_TOTAL)).isFalse()
        }
    }

    @Test
    @Ignore("b/270954533")
    fun aggregateRecordsGroupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 1,
                    startTime = Instant.ofEpochMilli(1200L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(1240L),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 2,
                    startTime = Instant.ofEpochMilli(1300L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(1500L),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = Instant.ofEpochMilli(2400L),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = Instant.ofEpochMilli(3500L),
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        Instant.ofEpochMilli(1000L),
                        Instant.ofEpochMilli(3000L)
                    ),
                    Duration.ofMillis(1000),
                    setOf()
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isEqualTo(3)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isEqualTo(5)
        }
    }

    @Test
    @Ignore("Blocked as period response from platform has a bug with inverted start/end timestamps")
    fun aggregateRecordsGroupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 100,
                    startTime = LocalDateTime.of(2018, 10, 11, 7, 10).toInstant(ZoneOffset.UTC),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = LocalDateTime.of(2018, 10, 11, 7, 15).toInstant(ZoneOffset.UTC),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 200,
                    startTime = LocalDateTime.of(2018, 10, 11, 10, 10).toInstant(ZoneOffset.UTC),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = LocalDateTime.of(2018, 10, 11, 11, 0).toInstant(ZoneOffset.UTC),
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 50,
                    startTime = LocalDateTime.of(2018, 10, 13, 7, 10).toInstant(ZoneOffset.UTC),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = LocalDateTime.of(2018, 10, 13, 8, 10).toInstant(ZoneOffset.UTC),
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        LocalDateTime.of(2018, 10, 11, 6, 10).toInstant(ZoneOffset.UTC),
                        LocalDateTime.of(2018, 10, 12, 7, 15).toInstant(ZoneOffset.UTC),
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isEqualTo(300)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isEqualTo(0)
        }
    }

    @Test
    fun getChangesToken() = runTest {
        val token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )
        assertThat(token).isNotEmpty()
    }

    @Test
    @Ignore("b/270954533")
    fun getChanges() = runTest {
        val token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )

        val insertedRecordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = Instant.ofEpochMilli(1234L),
                            startZoneOffset = ZoneOffset.UTC,
                            endTime = Instant.ofEpochMilli(5678L),
                            endZoneOffset = ZoneOffset.UTC
                        )
                    )
                )
                .recordIdsList[0]

        val record = healthConnectClient.readRecord(StepsRecord::class, insertedRecordId).record

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(UpsertionChange(record))

        healthConnectClient.deleteRecords(StepsRecord::class, TimeRangeFilter.none())

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(DeletionChange(insertedRecordId))
    }

    @Test
    fun getGrantedPermissions() = runTest {
        assertThat(healthConnectClient.permissionController.getGrantedPermissions())
            .containsExactlyElementsIn(allHealthPermissions)
    }
}
