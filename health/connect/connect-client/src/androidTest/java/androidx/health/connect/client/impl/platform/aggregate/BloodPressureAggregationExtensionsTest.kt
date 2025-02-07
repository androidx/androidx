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
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class BloodPressureAggregationExtensionsTest {

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
            HealthPermission.getWritePermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class)
        )

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(BloodPressureRecord::class, TimeRangeFilter.none())
    }

    @Test
    fun aggregateBloodPressure_invalidMetrics() = runTest {
        val metrics =
            setOf(
                BloodPressureRecord.DIASTOLIC_AVG,
                BloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_AVG,
                BloodPressureRecord.SYSTOLIC_MAX,
                BloodPressureRecord.SYSTOLIC_MIN,
                NutritionRecord.TRANS_FAT_TOTAL
            )

        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                healthConnectClient.aggregateBloodPressure(
                    AggregateRequest(metrics, TimeRangeFilter.none(), emptySet())
                )
            }
        }
    }

    @Test
    fun aggregateBloodPressure_noFiltersNoData() = runTest {
        val metrics =
            setOf(
                BloodPressureRecord.DIASTOLIC_AVG,
                BloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_AVG,
                BloodPressureRecord.SYSTOLIC_MAX,
                BloodPressureRecord.SYSTOLIC_MIN,
            )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(metrics, TimeRangeFilter.none(), emptySet())
            )

        metrics.forEach { assertThat(it in aggregationResult).isFalse() }
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateBloodPressure_noFilters() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 105.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 2.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 90.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 6.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.SYSTOLIC_MAX),
                    TimeRangeFilter.none(),
                    emptySet()
                )
            )

        assertThat(aggregationResult[BloodPressureRecord.SYSTOLIC_MAX])
            .isEqualTo(120.millimetersOfMercury)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateBloodPressure_noFilters_allMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.DIASTOLIC_MAX,
                        BloodPressureRecord.DIASTOLIC_MIN,
                        BloodPressureRecord.SYSTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN,
                    ),
                    TimeRangeFilter.none(),
                    emptySet()
                )
            )

        assertEquals(
            aggregationResult[BloodPressureRecord.DIASTOLIC_AVG] to 70.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.DIASTOLIC_MAX] to 80.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.DIASTOLIC_MIN] to 60.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_AVG] to 110.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MAX] to 120.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MIN] to 100.millimetersOfMercury,
        )
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateBloodPressure_noFilters_someMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN,
                    ),
                    TimeRangeFilter.none(),
                    emptySet()
                )
            )

        assertEquals(
            aggregationResult[BloodPressureRecord.DIASTOLIC_AVG] to 70.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MAX] to 120.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MIN] to 100.millimetersOfMercury,
        )
        setOf(
                BloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_AVG
            )
            .forEach { assertThat(it in aggregationResult).isFalse() }
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateBloodPressure_noFilters_noMetrics() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 105.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 2.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 90.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 6.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(emptySet(), TimeRangeFilter.none(), emptySet())
            )

        setOf(
                BloodPressureRecord.DIASTOLIC_AVG,
                BloodPressureRecord.DIASTOLIC_MAX,
                BloodPressureRecord.DIASTOLIC_MIN,
                BloodPressureRecord.SYSTOLIC_AVG,
                BloodPressureRecord.SYSTOLIC_MAX,
                BloodPressureRecord.SYSTOLIC_MIN,
            )
            .forEach { assertThat(it in aggregationResult).isFalse() }
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateBloodPressure_instantTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 105.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 2.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 90.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 6.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.DIASTOLIC_MIN),
                    TimeRangeFilter.between(
                        START_TIME + 30.seconds,
                        START_TIME + 6.minutes + 45.seconds
                    ),
                    emptySet()
                )
            )

        assertThat(aggregationResult[BloodPressureRecord.DIASTOLIC_MIN])
            .isEqualTo(65.millimetersOfMercury)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateBloodPressure_instantTimeRangeFilter_filterEndTime() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 2.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.DIASTOLIC_AVG, BloodPressureRecord.SYSTOLIC_AVG),
                    TimeRangeFilter.between(
                        START_TIME + 1.minutes,
                        START_TIME + 1.minutes + 59.seconds
                    ),
                    emptySet()
                )
            )

        assertThat(BloodPressureRecord.DIASTOLIC_AVG in aggregationResult).isFalse()
        assertThat(BloodPressureRecord.SYSTOLIC_AVG in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateBloodPressure_localTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 105.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 2.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 6.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.DIASTOLIC_MIN, BloodPressureRecord.SYSTOLIC_AVG),
                    TimeRangeFilter.between(
                        LocalDateTime.ofInstant(
                            START_TIME + 2.hours + 30.seconds,
                            ZoneOffset.ofHours(-2)
                        ),
                        LocalDateTime.ofInstant(
                            START_TIME + 2.hours + 6.minutes + 45.seconds,
                            ZoneOffset.ofHours(-2)
                        )
                    ),
                    emptySet()
                )
            )

        assertThat(aggregationResult[BloodPressureRecord.DIASTOLIC_MIN])
            .isEqualTo(65.millimetersOfMercury)
        assertThat(aggregationResult[BloodPressureRecord.SYSTOLIC_AVG])
            .isEqualTo(110.millimetersOfMercury)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    // TODO(b/337195270): Test with data origins from multiple apps
    @Test
    fun aggregateBloodPressure_insertedDataOriginFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.SYSTOLIC_AVG),
                    TimeRangeFilter.none(),
                    setOf(DataOrigin(context.packageName))
                )
            )

        assertThat(aggregationResult[BloodPressureRecord.SYSTOLIC_AVG])
            .isEqualTo(110.millimetersOfMercury)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateBloodPressure_timeRangeFilterOutOfBounds() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.SYSTOLIC_AVG),
                    TimeRangeFilter.after(START_TIME + 2.minutes),
                    emptySet()
                )
            )

        assertThat(BloodPressureRecord.SYSTOLIC_AVG in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateBloodPressure_nonExistingDataOriginFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 110.millimetersOfMercury,
                    diastolic = 65.millimetersOfMercury
                ),
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateBloodPressure(
                AggregateRequest(
                    setOf(BloodPressureRecord.SYSTOLIC_AVG),
                    TimeRangeFilter.none(),
                    setOf(DataOrigin("some random package name"))
                )
            )

        assertThat(BloodPressureRecord.SYSTOLIC_AVG in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateBloodPressure_groupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 1.days + 10.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByPeriodRequest(
                    setOf(
                        BloodPressureRecord.DIASTOLIC_AVG,
                        BloodPressureRecord.DIASTOLIC_MAX,
                        BloodPressureRecord.DIASTOLIC_MIN,
                        BloodPressureRecord.SYSTOLIC_AVG,
                        BloodPressureRecord.SYSTOLIC_MAX,
                        BloodPressureRecord.SYSTOLIC_MIN,
                    ),
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
            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))

            assertEquals(
                result[BloodPressureRecord.DIASTOLIC_AVG] to 70.millimetersOfMercury,
                result[BloodPressureRecord.DIASTOLIC_MAX] to 80.millimetersOfMercury,
                result[BloodPressureRecord.DIASTOLIC_MIN] to 60.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_AVG] to 110.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_MAX] to 120.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_MIN] to 100.millimetersOfMercury,
            )
        }

        with(aggregationResult[1]) {
            assertThat(startTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(1)
                )
            assertThat(endTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(2)
                )
            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))

            assertEquals(
                result[BloodPressureRecord.DIASTOLIC_AVG] to 70.millimetersOfMercury,
                result[BloodPressureRecord.DIASTOLIC_MAX] to 70.millimetersOfMercury,
                result[BloodPressureRecord.DIASTOLIC_MIN] to 70.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_AVG] to 100.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_MAX] to 100.millimetersOfMercury,
                result[BloodPressureRecord.SYSTOLIC_MIN] to 100.millimetersOfMercury,
            )
        }
    }

    @Test
    fun aggregateBloodPressure_groupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME + 4.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 120.millimetersOfMercury,
                    diastolic = 80.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 8.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 60.millimetersOfMercury
                ),
                BloodPressureRecord(
                    time = START_TIME + 1.hours + 10.minutes,
                    zoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    systolic = 100.millimetersOfMercury,
                    diastolic = 70.millimetersOfMercury
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByDurationRequest(
                    metrics =
                        setOf(
                            BloodPressureRecord.DIASTOLIC_AVG,
                            BloodPressureRecord.DIASTOLIC_MAX,
                            BloodPressureRecord.DIASTOLIC_MIN,
                            BloodPressureRecord.SYSTOLIC_AVG,
                            BloodPressureRecord.SYSTOLIC_MAX,
                            BloodPressureRecord.SYSTOLIC_MIN,
                        ),
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
                            doubleValues =
                                mapOf(
                                    BloodPressureRecord.DIASTOLIC_AVG.metricKey to 70.0,
                                    BloodPressureRecord.DIASTOLIC_MAX.metricKey to 80.0,
                                    BloodPressureRecord.DIASTOLIC_MIN.metricKey to 60.0,
                                    BloodPressureRecord.SYSTOLIC_AVG.metricKey to 110.0,
                                    BloodPressureRecord.SYSTOLIC_MAX.metricKey to 120.0,
                                    BloodPressureRecord.SYSTOLIC_MIN.metricKey to 100.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 1.hours,
                    endTime = START_TIME + 2.hours,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf(
                                    BloodPressureRecord.DIASTOLIC_AVG.metricKey to 70.0,
                                    BloodPressureRecord.DIASTOLIC_MAX.metricKey to 70.0,
                                    BloodPressureRecord.DIASTOLIC_MIN.metricKey to 70.0,
                                    BloodPressureRecord.SYSTOLIC_AVG.metricKey to 100.0,
                                    BloodPressureRecord.SYSTOLIC_MAX.metricKey to 100.0,
                                    BloodPressureRecord.SYSTOLIC_MIN.metricKey to 100.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                )
            )
    }

    private fun <A, E> assertEquals(vararg assertions: Pair<A, E>) {
        assertions.forEach { (actual, expected) -> assertThat(actual).isEqualTo(expected) }
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
