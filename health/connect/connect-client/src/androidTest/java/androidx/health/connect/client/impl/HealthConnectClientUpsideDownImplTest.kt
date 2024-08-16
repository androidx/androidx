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
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.impl.platform.aggregate.AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.health.connect.client.readRecord
import androidx.health.connect.client.records.BloodPressureRecord
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
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFeatureAvailabilityApi::class)
@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class HealthConnectClientUpsideDownImplTest {

    private companion object {
        private const val TOLERANCE = 1.0e-9

        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
        private val ZONE_OFFSET = ZoneOffset.UTC
        private val ZONE_ID = ZoneId.of(ZONE_OFFSET.id)
    }

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

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(*allHealthPermissions)

    private lateinit var healthConnectClient: HealthConnectClient

    @Before
    fun setUp() {
        healthConnectClient = HealthConnectClientUpsideDownImpl(context)
    }

    @After
    fun tearDown() = runTest {
        for (recordType in RECORDS_CLASS_NAME_MAP.keys) {
            healthConnectClient.deleteRecords(recordType, TimeRangeFilter.none())
        }
    }

    @Test
    fun allFeatures_belowUExt13_noneSupported() {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < 13)

        val features =
            listOf(
                HealthConnectFeatures.FEATURE_HEALTH_DATA_BACKGROUND_READ,
                HealthConnectFeatures.FEATURE_HEALTH_DATA_HISTORIC_READ,
                HealthConnectFeatures.FEATURE_SKIN_TEMPERATURE,
                HealthConnectFeatures.FEATURE_PLANNED_EXERCISE
            )

        for (feature in features) {
            assertThat(healthConnectClient.features.getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
        }
    }

    @Test
    fun insertRecords() = runTest {
        val response =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = START_TIME,
                        startZoneOffset = null,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = null
                    )
                )
            )
        assertThat(response.recordIdsList).hasSize(1)
    }

    @Test
    fun deleteRecords_byId() = runTest {
        val recordIds =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = null,
                            endTime = START_TIME + 1.minutes,
                            endZoneOffset = null
                        ),
                        StepsRecord(
                            count = 15,
                            startTime = START_TIME + 2.minutes,
                            startZoneOffset = null,
                            endTime = START_TIME + 3.minutes,
                            endZoneOffset = null
                        ),
                        StepsRecord(
                            count = 20,
                            startTime = START_TIME + 4.minutes,
                            startZoneOffset = null,
                            endTime = START_TIME + 5.minutes,
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

    @Test
    fun deleteRecords_byTimeRange() = runTest {
        healthConnectClient
            .insertRecords(
                listOf(
                    StepsRecord(
                        count = 100,
                        startTime = START_TIME,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = ZoneOffset.UTC
                    ),
                    StepsRecord(
                        count = 150,
                        startTime = START_TIME + 2.minutes,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 3.minutes,
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
            TimeRangeFilter.before(START_TIME + 1.minutes + 30.seconds)
        )

        assertThat(
                healthConnectClient
                    .readRecords(ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.none()))
                    .records
            )
            .containsExactly(initialRecords[1])
    }

    @Test
    fun updateRecords() = runTest {
        val id =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = null,
                            endTime = START_TIME + 30.seconds,
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
                    startTime = START_TIME,
                    startZoneOffset = null,
                    endTime = START_TIME + 30.seconds,
                    endZoneOffset = null,
                    metadata = Metadata(id, insertedRecord.metadata.dataOrigin)
                )
            )
        )

        val updatedRecord = healthConnectClient.readRecord(StepsRecord::class, id).record

        assertThat(updatedRecord.count).isEqualTo(5L)
    }

    @Test
    fun readRecord_withId() = runTest {
        val insertResponse =
            healthConnectClient.insertRecords(
                listOf(
                    StepsRecord(
                        count = 10,
                        startTime = START_TIME,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME + 1.minutes,
                        endZoneOffset = ZoneOffset.UTC
                    )
                )
            )

        val readResponse =
            healthConnectClient.readRecord(StepsRecord::class, insertResponse.recordIdsList[0])

        with(readResponse.record) {
            assertThat(count).isEqualTo(10)
            assertThat(startTime).isEqualTo(START_TIME.truncatedTo(ChronoUnit.MILLIS))
            assertThat(startZoneOffset).isEqualTo(ZoneOffset.UTC)
            assertThat(endTime).isEqualTo((START_TIME + 1.minutes).truncatedTo(ChronoUnit.MILLIS))
            assertThat(endZoneOffset).isEqualTo(ZoneOffset.UTC)
        }
    }

    @Test
    fun readRecords_withFilters() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 10,
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = START_TIME + 2.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 3.minutes,
                    endZoneOffset = ZoneOffset.UTC
                ),
            )
        )

        val readResponse =
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    StepsRecord::class,
                    TimeRangeFilter.after(START_TIME + 1.minutes + 30.seconds)
                )
            )

        assertThat(readResponse.records[0].count).isEqualTo(5)
    }

    @Ignore("b/314092270")
    @Test
    fun aggregateRecords() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 10,
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = START_TIME + 1.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC
                ),
                HeartRateRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            HeartRateRecord.Sample(START_TIME, 57L),
                            HeartRateRecord.Sample(START_TIME + 15.seconds, 120L)
                        )
                ),
                HeartRateRecord(
                    startTime = START_TIME + 1.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes + 30.seconds,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            HeartRateRecord.Sample(START_TIME + 1.minutes, 47L),
                            HeartRateRecord.Sample(START_TIME + 1.minutes + 15.seconds, 48L)
                        )
                ),
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    energy = Energy.kilocalories(200.0)
                ),
                WeightRecord(
                    time = START_TIME,
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
            assertThat(this[NutritionRecord.CAFFEINE_TOTAL]!!.inGrams).isWithin(TOLERANCE).of(0.0)
            assertThat(this[WeightRecord.WEIGHT_MAX]).isEqualTo(Mass.kilograms(100.0))

            assertThat(contains(WheelchairPushesRecord.COUNT_TOTAL)).isFalse()
        }
    }

    // TODO(b/361297592): Remove once the aggregation bug is fixed
    @Test
    fun aggregateRecords_unsupportedMetrics_throwsUOE() = runTest {
        for (metric in AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10) {
            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregate(
                        AggregateRequest(setOf(metric), TimeRangeFilter.none())
                    )
                }
            }

            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregateGroupByDuration(
                        AggregateGroupByDurationRequest(
                            setOf(metric),
                            TimeRangeFilter.none(),
                            Duration.ofDays(1)
                        )
                    )
                }
            }

            assertThrows(UnsupportedOperationException::class.java) {
                runBlocking {
                    healthConnectClient.aggregateGroupByPeriod(
                        AggregateGroupByPeriodRequest(
                            setOf(metric),
                            TimeRangeFilter.none(),
                            Period.ofDays(1)
                        )
                    )
                }
            }
        }
    }

    @Ignore("b/326414908")
    @Test
    fun aggregateRecords_belowSdkExt10() = runTest {
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    transFat = 0.5.grams
                ),
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.DIASTOLIC_MAX,
                        BloodPressureRecord.DIASTOLIC_MIN,
                        BloodPressureRecord.SYSTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN,
                        NutritionRecord.TRANS_FAT_TOTAL
                    ),
                    TimeRangeFilter.none()
                )
            )

        assertEquals(
            aggregateResponse[NutritionRecord.TRANS_FAT_TOTAL] to 0.5.grams,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_AVG] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_MAX] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.SYSTOLIC_MIN] to 120.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_AVG] to 80.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_MAX] to 80.millimetersOfMercury,
            aggregateResponse[BloodPressureRecord.DIASTOLIC_MIN] to 80.millimetersOfMercury,
        )
    }

    @Ignore("b/314092270")
    @Test
    fun aggregateRecordsGroupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 1,
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 10.seconds,
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 2,
                    startTime = START_TIME + 15.seconds,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 25.seconds,
                    endZoneOffset = ZoneOffset.UTC
                ),
                StepsRecord(
                    count = 5,
                    startTime = START_TIME + 40.seconds,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(START_TIME, START_TIME + 1.minutes),
                    Duration.ofSeconds(30),
                    setOf()
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isEqualTo(3)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isEqualTo(5)
        }
    }

    @Ignore("b/314092270")
    @Test
    fun aggregateRecordsGroupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 100,
                    startTime = START_TIME,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 5.minutes,
                    endZoneOffset = ZONE_OFFSET
                ),
                StepsRecord(
                    count = 200,
                    startTime = START_TIME + 10.minutes,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 30.minutes,
                    endZoneOffset = ZONE_OFFSET
                ),
                StepsRecord(
                    count = 50,
                    startTime = START_TIME + 1.days,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 1.days + 10.minutes,
                    endZoneOffset = ZONE_OFFSET
                )
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME, ZONE_ID),
                        LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID),
                    ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isEqualTo(300)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isEqualTo(50)
        }
    }

    @Ignore("b/314092270")
    @Test
    fun aggregateRecordsGroupByPeriod_monthly() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsRecord(
                    count = 100,
                    startTime = START_TIME - 40.days,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME - 40.days + 5.minutes,
                    endZoneOffset = ZONE_OFFSET
                ),
                StepsRecord(
                    count = 200,
                    startTime = START_TIME - 40.days + 10.minutes,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME - 40.days + 30.minutes,
                    endZoneOffset = ZONE_OFFSET
                ),
                StepsRecord(
                    count = 50,
                    startTime = START_TIME,
                    startZoneOffset = ZONE_OFFSET,
                    endTime = START_TIME + 10.minutes,
                    endZoneOffset = ZONE_OFFSET
                )
            )
        )

        val queryStartTime = LocalDateTime.ofInstant(START_TIME - 40.days, ZONE_ID)
        val queryEndTime = LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID)

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        queryStartTime,
                        queryEndTime,
                    ),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)

            assertThat(this[0].startTime).isEqualTo(queryStartTime)
            assertThat(this[0].endTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isEqualTo(300)

            assertThat(this[1].startTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[1].endTime).isEqualTo(queryEndTime)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isEqualTo(50)
        }
    }

    @Ignore("b/314092270")
    @Test
    fun aggregateRecordsGroupByPeriod_monthly_noData() = runTest {
        val queryStartTime = LocalDateTime.ofInstant(START_TIME - 40.days, ZONE_ID)
        val queryEndTime = LocalDateTime.ofInstant(START_TIME + 2.days, ZONE_ID)

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(StepsRecord.COUNT_TOTAL),
                    TimeRangeFilter.between(
                        queryStartTime,
                        queryEndTime,
                    ),
                    timeRangeSlicer = Period.ofMonths(1)
                )
            )

        with(aggregateResponse) {
            assertThat(this).hasSize(2)

            assertThat(this[0].startTime).isEqualTo(queryStartTime)
            assertThat(this[0].endTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[0].result[StepsRecord.COUNT_TOTAL]).isNull()

            assertThat(this[1].startTime).isEqualTo(queryStartTime.plus(Period.ofMonths(1)))
            assertThat(this[1].endTime).isEqualTo(queryEndTime)
            assertThat(this[1].result[StepsRecord.COUNT_TOTAL]).isNull()
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
    fun getChanges() = runTest {
        var token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )

        val insertedRecordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        StepsRecord(
                            count = 10,
                            startTime = START_TIME,
                            startZoneOffset = ZoneOffset.UTC,
                            endTime = START_TIME + 5.minutes,
                            endZoneOffset = ZoneOffset.UTC
                        )
                    )
                )
                .recordIdsList[0]

        val record = healthConnectClient.readRecord(StepsRecord::class, insertedRecordId).record

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(UpsertionChange(record))

        token =
            healthConnectClient.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class), setOf())
            )

        healthConnectClient.deleteRecords(StepsRecord::class, listOf(insertedRecordId), emptyList())

        assertThat(healthConnectClient.getChanges(token).changes)
            .containsExactly(DeletionChange(insertedRecordId))
    }

    @Test
    fun nutritionRecord_roundTrip_valuesEqual() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                            calcium = Mass.grams(15.0),
                            monounsaturatedFat = Mass.grams(50.0),
                            energy = Energy.calories(300.0)
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isEqualTo(Mass.grams(15.0))
            assertThat(monounsaturatedFat).isEqualTo(Mass.grams(50.0))
            assertThat(energy).isEqualTo(Energy.calories(300.0))
        }
    }

    @Test
    fun nutritionRecord_roundTrip_zeroValues() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                            calcium = Mass.grams(0.0),
                            monounsaturatedFat = Mass.grams(0.0),
                            energy = Energy.calories(0.0)
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isEqualTo(Mass.grams(0.0))
            assertThat(monounsaturatedFat).isEqualTo(Mass.grams(0.0))
            assertThat(energy).isEqualTo(Energy.calories(0.0))
        }
    }

    @Test
    fun nutritionRecord_roundTrip_nullValues() = runTest {
        val recordId =
            healthConnectClient
                .insertRecords(
                    listOf(
                        NutritionRecord(
                            startTime = START_TIME,
                            startZoneOffset = ZONE_OFFSET,
                            endTime = START_TIME + 10.minutes,
                            endZoneOffset = ZONE_OFFSET,
                        )
                    )
                )
                .recordIdsList[0]

        val nutritionRecord = healthConnectClient.readRecord<NutritionRecord>(recordId).record

        with(nutritionRecord) {
            assertThat(calcium).isNull()
            assertThat(monounsaturatedFat).isNull()
            assertThat(energy).isNull()
        }
    }

    @Test
    fun getGrantedPermissions() = runTest {
        assertThat(healthConnectClient.permissionController.getGrantedPermissions())
            .containsExactlyElementsIn(allHealthPermissions)
    }

    private fun <A, E> assertEquals(vararg assertions: Pair<A, E>) {
        assertions.forEach { (actual, expected) -> assertThat(actual).isEqualTo(expected) }
    }

    private val Int.seconds: Duration
        get() = Duration.ofSeconds(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())

    private val Int.days: Duration
        get() = Duration.ofDays(this.toLong())
}
