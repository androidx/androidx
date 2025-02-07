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

package androidx.health.connect.client.impl.platform.aggregate

import android.annotation.TargetApi
import android.content.Context
import android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.impl.platform.toLocalTimeWithDefaultZoneFallback
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.grams
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
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class NutritionAggregationExtensionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            HealthPermission.getWritePermission(NutritionRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class)
        )

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(NutritionRecord::class, TimeRangeFilter.none())
    }

    @Test
    fun aggregateNutritionTransFatTotal_noData() = runTest {
        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(emptySet(), TimeRangeFilter.none(), emptySet())
            )

        assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateNutritionTransFatTotal_noFilters() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.3),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 2.minutes,
                    endTime = START_TIME + 3.minutes,
                    transFat = null,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 4.minutes,
                    endTime = START_TIME + 5.minutes,
                    transFat = Mass.grams(0.4),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 6.minutes,
                    endTime = START_TIME + 7.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 8.minutes,
                    endTime = START_TIME + 9.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(emptySet(), TimeRangeFilter.none(), emptySet())
            )

        assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL]).isEqualTo(Mass.grams(1.7))
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateNutritionTransFatTotal_groupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = .3.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 1.days + 2.minutes,
                    endTime = START_TIME + 1.days + 3.minutes,
                    transFat = null,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 3.days + 4.minutes,
                    endTime = START_TIME + 3.days + 5.minutes,
                    transFat = .4.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 3.days + 6.minutes,
                    endTime = START_TIME + 3.days + 7.minutes,
                    transFat = .5.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(NutritionRecord.TRANS_FAT_TOTAL),
                    timeRangeFilter =
                        TimeRangeFilter.after(
                            START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                        ),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

        assertThat(aggregationResult).hasSize(2)

        with(aggregationResult[0]) {
            assertThat(startTime)
                .isEqualTo(START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC))
            assertThat(endTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(1)
                )
            assertThat(result[NutritionRecord.TRANS_FAT_TOTAL]).isEqualTo(.3.grams)
            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))
        }

        with(aggregationResult[1]) {
            assertThat(startTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(3)
                )
            assertThat(endTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(4)
                )
            assertThat(result[NutritionRecord.TRANS_FAT_TOTAL]).isEqualTo(.9.grams)
            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))
        }
    }

    @Test
    fun aggregateNutritionTransFatTotal_groupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    transFat = .3.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                ),
                NutritionRecord(
                    startTime = START_TIME + 1.hours + 2.minutes,
                    endTime = START_TIME + 1.hours + 3.minutes,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    transFat = null,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                ),
                NutritionRecord(
                    startTime = START_TIME + 3.hours + 4.minutes,
                    endTime = START_TIME + 3.hours + 5.minutes,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    transFat = .4.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                ),
                NutritionRecord(
                    startTime = START_TIME + 3.hours + 6.minutes,
                    endTime = START_TIME + 3.hours + 7.minutes,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    transFat = .5.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByDurationRequest(
                    metrics = setOf(NutritionRecord.TRANS_FAT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.after(START_TIME),
                    timeRangeSlicer = 1.hours
                )
            )

        assertThat(aggregationResult)
            .containsExactly(
                AggregationResultGroupedByDuration(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.hours,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues = mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to .3),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 3.hours,
                    endTime = START_TIME + 4.hours,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues = mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to .9),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                )
            )
    }

    @Test
    fun aggregateNutritionTransFatTotal_instantTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.3),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 2.minutes,
                    endTime = START_TIME + 3.minutes,
                    transFat = null,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 4.minutes,
                    endTime = START_TIME + 5.minutes,
                    transFat = Mass.grams(0.4),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 6.minutes,
                    endTime = START_TIME + 7.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 8.minutes,
                    endTime = START_TIME + 9.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.between(
                        START_TIME + 30.seconds,
                        START_TIME + 6.minutes + 45.seconds
                    ),
                    emptySet()
                )
            )

        assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL])
            .isEqualTo(Mass.grams(0.15 + 0.4 + 0.375))
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateNutritionTransFatTotal_instantTimeRangeFilter_filterStartTimeRecordEndTime() =
        runTest {
            healthConnectClient.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = START_TIME,
                        endTime = START_TIME + 1.minutes,
                        transFat = Mass.grams(0.3),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    ),
                    NutritionRecord(
                        startTime = START_TIME + 2.minutes,
                        endTime = START_TIME + 3.minutes,
                        transFat = Mass.grams(0.4),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    )
                )
            )

            val aggregationResult =
                healthConnectClient.aggregateNutritionTransFatTotal(
                    AggregateRequest(
                        emptySet(),
                        TimeRangeFilter.between(START_TIME + 1.minutes, START_TIME + 2.minutes),
                        emptySet()
                    )
                )

            assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
            assertThat(aggregationResult.dataOrigins).isEmpty()
        }

    @Test
    fun aggregateNutritionTransFatTotal_instantTimeRangeFilter_filterStartTimeRecordStartTime() =
        runTest {
            healthConnectClient.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = START_TIME,
                        endTime = START_TIME + 1.minutes,
                        transFat = Mass.grams(0.3),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    ),
                    NutritionRecord(
                        startTime = START_TIME + 2.minutes,
                        endTime = START_TIME + 3.minutes,
                        transFat = Mass.grams(0.4),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    )
                )
            )

            val aggregationResult =
                healthConnectClient.aggregateNutritionTransFatTotal(
                    AggregateRequest(
                        emptySet(),
                        TimeRangeFilter.between(START_TIME, START_TIME + 2.minutes),
                        emptySet()
                    )
                )

            assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL])
                .isEqualTo(Mass.grams(0.3))
            assertThat(aggregationResult.dataOrigins)
                .containsExactly(DataOrigin(context.packageName))
        }

    @Test
    fun aggregateNutritionTransFatTotal_instantTimeRangeFilter_recordRangeLargerThanQuery() =
        runTest {
            healthConnectClient.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = START_TIME,
                        endTime = START_TIME + 1.minutes,
                        transFat = Mass.grams(0.5),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    ),
                )
            )

            val aggregationResult =
                healthConnectClient.aggregateNutritionTransFatTotal(
                    AggregateRequest(
                        emptySet(),
                        TimeRangeFilter.between(START_TIME + 15.seconds, START_TIME + 45.seconds),
                        emptySet()
                    )
                )

            assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL])
                .isEqualTo(Mass.grams(0.25))
            assertThat(aggregationResult.dataOrigins)
                .containsExactly(DataOrigin(context.packageName))
        }

    @Test
    fun aggregateNutritionTransFatTotal_localTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.3),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 2.minutes,
                    endTime = START_TIME + 3.minutes,
                    transFat = null,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME - 2.hours + 4.minutes,
                    endTime = START_TIME + 5.minutes,
                    transFat = Mass.grams(0.4),
                    startZoneOffset = ZoneOffset.ofHours(2),
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME + 3.hours + 6.minutes,
                    endTime = START_TIME + 3.hours + 7.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.ofHours(-3),
                    endZoneOffset = ZoneOffset.ofHours(-3),
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
                NutritionRecord(
                    startTime = START_TIME - 4.hours + 8.minutes,
                    endTime = START_TIME - 4.hours + 9.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.ofHours(4),
                    endZoneOffset = ZoneOffset.ofHours(4),
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME + 30.seconds, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(START_TIME + 6.minutes + 45.seconds, ZoneOffset.UTC)
                    ),
                    emptySet()
                )
            )

        assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL])
            .isEqualTo(Mass.grams(0.15 + 0.4 + 0.375))
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateNutritionTransFatTotal_localTimeRangeFilter_recordRangeLargerThanQuery() =
        runTest {
            healthConnectClient.insertRecords(
                listOf(
                    NutritionRecord(
                        startTime = START_TIME,
                        endTime = START_TIME + 1.minutes,
                        transFat = Mass.grams(0.5),
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    ),
                )
            )

            val aggregationResult =
                healthConnectClient.aggregateNutritionTransFatTotal(
                    AggregateRequest(
                        emptySet(),
                        TimeRangeFilter.between(
                            LocalDateTime.ofInstant(
                                START_TIME - 2.hours + 15.seconds,
                                ZoneOffset.ofHours(2)
                            ),
                            LocalDateTime.ofInstant(
                                START_TIME - 2.hours + 45.seconds,
                                ZoneOffset.ofHours(2)
                            )
                        ),
                        emptySet()
                    )
                )

            assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL])
                .isEqualTo(Mass.grams(0.25))
            assertThat(aggregationResult.dataOrigins)
                .containsExactly(DataOrigin(context.packageName))
        }

    // TODO(b/337195270): Test with data origins from multiple apps
    @Test
    fun aggregateNutritionTransFatTotal_insertedDataOriginFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.none(),
                    setOf(DataOrigin(context.packageName))
                )
            )

        assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL]).isEqualTo(Mass.grams(0.5))
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateNutritionTransFatTotal_timeRangeFilterOutOfBounds() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.after(START_TIME + 2.minutes),
                    emptySet()
                )
            )

        assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateNutritionTransFatTotal_recordStartTimeWithNegativeZoneOffset() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 60.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.ofHours(-2),
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(START_TIME, ZoneOffset.UTC),
                        LocalDateTime.ofInstant(START_TIME + 60.minutes, ZoneOffset.UTC)
                    ),
                    emptySet()
                )
            )

        assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateNutritionTransFatTotal_nonExistingDataOriginFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.5),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateNutritionTransFatTotal(
                AggregateRequest(
                    emptySet(),
                    TimeRangeFilter.none(),
                    setOf(DataOrigin("some random package name"))
                )
            )

        assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    private val Int.days: Duration
        get() = Duration.ofDays(this.toLong())

    private val Int.seconds: Duration
        get() = Duration.ofSeconds(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())

    private val Int.hours: Duration
        get() = Duration.ofHours(this.toLong())
}
