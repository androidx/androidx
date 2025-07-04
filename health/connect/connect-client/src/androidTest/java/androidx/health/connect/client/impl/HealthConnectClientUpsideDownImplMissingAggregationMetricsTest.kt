/*
 * Copyright 2025 The Android Open Source Project
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

import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilocalories
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// See b/326414908 for more context on this test.
// Some tests assume empty data origins in buckets with results, that's because platform doesn't
// support data origins in bucket < sdk ext 10. See b/284163741.
@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(
    minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
    maxSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
)
class HealthConnectClientUpsideDownImplMissingAggregationMetricsTest {

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)

        private val TEST_RECORD_TYPES =
            setOf(BloodPressureRecord::class, HeartRateRecord::class, NutritionRecord::class)

        private val TEST_RECORD_PERMISSIONS =
            TEST_RECORD_TYPES.flatMap {
                    listOf(
                        HealthPermission.getReadPermission(it),
                        HealthPermission.getWritePermission(it),
                    )
                }
                .toTypedArray()
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*TEST_RECORD_PERMISSIONS)

    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    @Before
    fun setUp() {
        Assume.assumeTrue(
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) < 10
        )
    }

    @After
    fun tearDown() = runTest {
        for (recordType in TEST_RECORD_TYPES) {
            healthConnectClient.deleteRecords(recordType, TimeRangeFilter.after(Instant.EPOCH))
        }
    }

    @Test
    fun aggregate_onlyMissingMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 0.5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                ),
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury,
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(BloodPressureRecord.DIASTOLIC_AVG, NutritionRecord.TRANS_FAT_TOTAL),
                    TimeRangeFilter.Companion.after(Instant.EPOCH),
                )
            )

        assertThat(aggregateResponse)
            .isEqualTo(
                AggregationResult(
                    longValues = emptyMap(),
                    doubleValues =
                        mapOf(
                            NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.5,
                            BloodPressureRecord.DIASTOLIC_AVG.metricKey to 80.0,
                        ),
                    dataOrigins = setOf(DataOrigin(context.packageName)),
                )
            )
    }

    @Test
    fun aggregate_onlySupportedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = .5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 5.minutes, 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 10.minutes, 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(HeartRateRecord.BPM_AVG, NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.after(START_TIME),
                )
            )

        assertThat(aggregateResponse)
            .isEqualTo(
                AggregationResult(
                    longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 75),
                    doubleValues = mapOf(NutritionRecord.ENERGY_TOTAL.metricKey to 600.0),
                    dataOrigins = setOf(DataOrigin(context.packageName)),
                )
            )
    }

    @Test
    fun aggregate_mixedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = .5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 5.minutes, 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 10.minutes, 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(
                        HeartRateRecord.BPM_AVG,
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.TRANS_FAT_TOTAL,
                    ),
                    TimeRangeFilter.after(START_TIME),
                )
            )

        assertThat(aggregateResponse)
            .isEqualTo(
                AggregationResult(
                    longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 75),
                    doubleValues =
                        mapOf(
                            NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.5,
                            NutritionRecord.ENERGY_TOTAL.metricKey to 600.0,
                        ),
                    dataOrigins = setOf(DataOrigin(context.packageName)),
                )
            )
    }

    @Test
    fun aggregateGroupByDuration_onlyMissingMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 0.5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                ),
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury,
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(
                        BloodPressureRecord.Companion.DIASTOLIC_AVG,
                        NutritionRecord.Companion.TRANS_FAT_TOTAL,
                    ),
                    TimeRangeFilter.Companion.after(Instant.EPOCH),
                    5.minutes,
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByDuration(
                    startTime = START_TIME,
                    endTime = START_TIME + 5.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf<String, Double>(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to 0.5,
                                    BloodPressureRecord.DIASTOLIC_AVG.metricKey to 80.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf<String, Double>(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.0
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
            )
    }

    @Test
    fun aggregateGroupByDuration_onlySupportedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 0.5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 5.minutes, 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 10.minutes, 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(HeartRateRecord.BPM_AVG, NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(START_TIME, START_TIME + 15.minutes),
                    5.minutes,
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByDuration(
                    startTime = START_TIME,
                    endTime = START_TIME + 5.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues = mapOf(NutritionRecord.ENERGY_TOTAL.metricKey to 100.0),
                            dataOrigins = emptySet(),
                        ),
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 100),
                            doubleValues = mapOf(NutritionRecord.ENERGY_TOTAL.metricKey to 500.0),
                            dataOrigins = emptySet(),
                        ),
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 50),
                            doubleValues = emptyMap(),
                            dataOrigins = emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun aggregateGroupByDuration_mixedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = .5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 6.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 5.minutes, 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + 10.minutes, 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    setOf(
                        HeartRateRecord.BPM_AVG,
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.TRANS_FAT_TOTAL,
                    ),
                    TimeRangeFilter.between(START_TIME, START_TIME + 15.minutes),
                    5.minutes,
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByDuration(
                    startTime = START_TIME,
                    endTime = START_TIME + 5.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to .5,
                                    NutritionRecord.ENERGY_TOTAL.metricKey to 100.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 5.minutes,
                    endTime = START_TIME + 10.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 100),
                            doubleValues =
                                mapOf(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.0,
                                    NutritionRecord.ENERGY_TOTAL.metricKey to 500.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 10.minutes,
                    endTime = START_TIME + 15.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 50),
                            doubleValues = emptyMap(),
                            dataOrigins = emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun aggregateGroupByPeriod_onlyMissingMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 0.5.grams,
                ),
                NutritionRecord(
                    startTime = START_TIME + Duration.ofDays(1),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + Duration.ofDays(1) + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                ),
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury,
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(
                        BloodPressureRecord.Companion.DIASTOLIC_AVG,
                        NutritionRecord.Companion.TRANS_FAT_TOTAL,
                    ),
                    TimeRangeFilter.after(LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC)),
                    Period.ofDays(1),
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    startTime = LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to 0.5,
                                    BloodPressureRecord.DIASTOLIC_AVG.metricKey to 80.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByPeriod(
                    startTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(2), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues = mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.0),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
            )
    }

    @Test
    fun aggregateGroupByPeriod_onlySupportedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = .5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + Duration.ofDays(1),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + Duration.ofDays(1) + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + Duration.ofDays(1),
                    endTime = START_TIME + Duration.ofDays(1) + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + Duration.ofDays(1), 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + Duration.ofDays(2),
                    endTime = START_TIME + Duration.ofDays(2) + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + Duration.ofDays(2), 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(HeartRateRecord.BPM_AVG, NutritionRecord.ENERGY_TOTAL),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(3), ZoneOffset.UTC),
                    ),
                    Period.ofDays(1),
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    startTime = LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues = mapOf(NutritionRecord.ENERGY_TOTAL.metricKey to 100.0),
                            dataOrigins = emptySet(),
                        ),
                ),
                AggregationResultGroupedByPeriod(
                    startTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(2), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 100),
                            doubleValues = mapOf(NutritionRecord.ENERGY_TOTAL.metricKey to 500.0),
                            dataOrigins = emptySet(),
                        ),
                ),
                AggregationResultGroupedByPeriod(
                    startTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(2), ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(3), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 50),
                            doubleValues = emptyMap(),
                            dataOrigins = emptySet(),
                        ),
                ),
            )
    }

    @Test
    fun aggregateGroupByPeriod_mixedMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = .5.grams,
                    energy = 100.kilocalories,
                ),
                NutritionRecord(
                    startTime = START_TIME + Duration.ofDays(1),
                    startZoneOffset = ZoneOffset.UTC,
                    endTime = START_TIME + Duration.ofDays(1) + 1.minutes,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    transFat = 5.grams,
                    energy = 500.kilocalories,
                ),
                HeartRateRecord(
                    startTime = START_TIME + Duration.ofDays(1),
                    endTime = START_TIME + Duration.ofDays(1) + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + Duration.ofDays(1), 100)),
                ),
                HeartRateRecord(
                    startTime = START_TIME + Duration.ofDays(2),
                    endTime = START_TIME + Duration.ofDays(2) + 5.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata.manualEntry(),
                    samples = listOf(HeartRateRecord.Sample(START_TIME + Duration.ofDays(2), 50)),
                ),
            )
        )

        val aggregateResponse =
            healthConnectClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    setOf(
                        HeartRateRecord.BPM_AVG,
                        NutritionRecord.ENERGY_TOTAL,
                        NutritionRecord.TRANS_FAT_TOTAL,
                    ),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(3), ZoneOffset.UTC),
                    ),
                    Period.ofDays(1),
                )
            )

        assertThat(aggregateResponse)
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    startTime = LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to .5,
                                    NutritionRecord.ENERGY_TOTAL.metricKey to 100.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByPeriod(
                    startTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(1), ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(2), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 100),
                            doubleValues =
                                mapOf(
                                    NutritionRecord.TRANS_FAT_TOTAL.metricKey to 5.0,
                                    NutritionRecord.ENERGY_TOTAL.metricKey to 500.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName)),
                        ),
                ),
                AggregationResultGroupedByPeriod(
                    startTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(2), ZoneOffset.UTC),
                    endTime =
                        LocalDateTime.ofInstant(START_TIME + Duration.ofDays(3), ZoneOffset.UTC),
                    result =
                        AggregationResult(
                            longValues = mapOf(HeartRateRecord.BPM_AVG.metricKey to 50),
                            doubleValues = emptyMap(),
                            dataOrigins = emptySet(),
                        ),
                ),
            )
    }

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
