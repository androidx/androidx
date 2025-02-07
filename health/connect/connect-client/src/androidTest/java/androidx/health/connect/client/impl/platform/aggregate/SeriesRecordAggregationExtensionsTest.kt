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
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.metersPerSecond
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
class SeriesRecordAggregationExtensionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
        private val SERIES_AGGREGATION_FALLBACK_RECORD_TYPES =
            setOf(
                CyclingPedalingCadenceRecord::class,
                SpeedRecord::class,
                StepsCadenceRecord::class
            )
    }

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *(SERIES_AGGREGATION_FALLBACK_RECORD_TYPES.flatMap {
                    listOf(
                        HealthPermission.getWritePermission(it),
                        HealthPermission.getReadPermission(it)
                    )
                }
                .toTypedArray())
        )

    @After
    fun tearDown() = runTest {
        for (recordType in SERIES_AGGREGATION_FALLBACK_RECORD_TYPES) {
            healthConnectClient.deleteRecords(recordType, TimeRangeFilter.none())
        }
    }

    @Test
    fun aggregateCyclingPedalingCadence() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 30.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 5.minutes,
                                revolutionsPerMinute = 80.0
                            ),
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 15.minutes,
                                revolutionsPerMinute = 90.0
                            )
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            CyclingPedalingCadenceRecord.RPM_AVG,
                            CyclingPedalingCadenceRecord.RPM_MAX,
                            CyclingPedalingCadenceRecord.RPM_MIN
                        ),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(aggregationResult[CyclingPedalingCadenceRecord.RPM_AVG]).isEqualTo(85.0)
        assertThat(aggregationResult[CyclingPedalingCadenceRecord.RPM_MAX]).isEqualTo(90.0)
        assertThat(aggregationResult[CyclingPedalingCadenceRecord.RPM_MIN]).isEqualTo(80.0)
    }

    @Test
    fun aggregateSeries_groupByPeriod() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 30.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 5.minutes,
                                revolutionsPerMinute = 80.0
                            ),
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 15.minutes,
                                revolutionsPerMinute = 90.0
                            )
                        )
                ),
                CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 2.days + 30.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 10.minutes,
                                revolutionsPerMinute = 220.0
                            ),
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 2.days + 15.minutes,
                                revolutionsPerMinute = 100.0
                            )
                        )
                ),
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            SpeedRecord.Sample(
                                time = START_TIME + 5.minutes,
                                speed = 2.8.metersPerSecond
                            ),
                            SpeedRecord.Sample(
                                time = START_TIME + 10.minutes,
                                speed = 2.7.metersPerSecond
                            )
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByPeriodRequest(
                    metrics =
                        setOf(
                            CyclingPedalingCadenceRecord.RPM_AVG,
                            CyclingPedalingCadenceRecord.RPM_MAX,
                            CyclingPedalingCadenceRecord.RPM_MIN,
                            SpeedRecord.SPEED_AVG,
                        ),
                    timeRangeSlicer = Period.ofDays(1),
                    timeRangeFilter =
                        TimeRangeFilter.after(
                            START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                        )
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
            assertThat(result[CyclingPedalingCadenceRecord.RPM_AVG]).isEqualTo(130.0)
            assertThat(result[CyclingPedalingCadenceRecord.RPM_MAX]).isEqualTo(220.0)
            assertThat(result[CyclingPedalingCadenceRecord.RPM_MIN]).isEqualTo(80.0)

            assertThat(result[SpeedRecord.SPEED_AVG]).isEqualTo(2.75.metersPerSecond)

            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))
        }

        with(aggregationResult[1]) {
            assertThat(startTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(2)
                )
            assertThat(endTime)
                .isEqualTo(
                    START_TIME.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC).plusDays(3)
                )
            assertThat(result[CyclingPedalingCadenceRecord.RPM_AVG]).isEqualTo(100.0)
            assertThat(result[CyclingPedalingCadenceRecord.RPM_MAX]).isEqualTo(100.0)
            assertThat(result[CyclingPedalingCadenceRecord.RPM_MIN]).isEqualTo(100.0)

            assertThat(SpeedRecord.SPEED_AVG in result).isFalse()

            assertThat(result.dataOrigins).containsExactly(DataOrigin(context.packageName))
        }
    }

    @Test
    fun aggregateSeries_groupByDuration() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 30.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 5.minutes,
                                revolutionsPerMinute = 80.0
                            ),
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 15.minutes,
                                revolutionsPerMinute = 90.0
                            )
                        )
                ),
                CyclingPedalingCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 2.hours + 30.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 10.minutes,
                                revolutionsPerMinute = 220.0
                            ),
                            CyclingPedalingCadenceRecord.Sample(
                                time = START_TIME + 2.hours + 15.minutes,
                                revolutionsPerMinute = 100.0
                            )
                        )
                ),
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            SpeedRecord.Sample(
                                time = START_TIME + 5.minutes,
                                speed = 2.8.metersPerSecond
                            ),
                            SpeedRecord.Sample(
                                time = START_TIME + 10.minutes,
                                speed = 2.7.metersPerSecond
                            )
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateGroupByDurationRequest(
                    metrics =
                        setOf(
                            CyclingPedalingCadenceRecord.RPM_AVG,
                            CyclingPedalingCadenceRecord.RPM_MAX,
                            CyclingPedalingCadenceRecord.RPM_MIN,
                            SpeedRecord.SPEED_AVG,
                        ),
                    timeRangeSlicer = 1.hours,
                    timeRangeFilter = TimeRangeFilter.after(START_TIME)
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
                                    CyclingPedalingCadenceRecord.RPM_AVG.metricKey to 130.0,
                                    CyclingPedalingCadenceRecord.RPM_MAX.metricKey to 220.0,
                                    CyclingPedalingCadenceRecord.RPM_MIN.metricKey to 80.0,
                                    SpeedRecord.SPEED_AVG.metricKey to 2.75,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                ),
                AggregationResultGroupedByDuration(
                    startTime = START_TIME + 2.hours,
                    endTime = START_TIME + 3.hours,
                    zoneOffset = ZoneOffset.UTC,
                    result =
                        AggregationResult(
                            longValues = emptyMap(),
                            doubleValues =
                                mapOf(
                                    CyclingPedalingCadenceRecord.RPM_AVG.metricKey to 100.0,
                                    CyclingPedalingCadenceRecord.RPM_MAX.metricKey to 100.0,
                                    CyclingPedalingCadenceRecord.RPM_MIN.metricKey to 100.0,
                                ),
                            dataOrigins = setOf(DataOrigin(context.packageName))
                        )
                )
            )
    }

    @Test
    fun aggregateCyclingSpeed() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            SpeedRecord.Sample(
                                time = START_TIME + 5.minutes,
                                speed = 2.8.metersPerSecond
                            ),
                            SpeedRecord.Sample(
                                time = START_TIME + 10.minutes,
                                speed = 2.7.metersPerSecond
                            )
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(SpeedRecord.SPEED_AVG, SpeedRecord.SPEED_MAX, SpeedRecord.SPEED_MIN),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(aggregationResult[SpeedRecord.SPEED_AVG]).isEqualTo(2.75.metersPerSecond)
        assertThat(aggregationResult[SpeedRecord.SPEED_MAX]).isEqualTo(2.8.metersPerSecond)
        assertThat(aggregationResult[SpeedRecord.SPEED_MIN]).isEqualTo(2.7.metersPerSecond)
    }

    @Test
    fun aggregateStepsCadence() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(aggregationResult[StepsCadenceRecord.RATE_AVG]).isEqualTo(175.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MAX]).isEqualTo(180.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MIN]).isEqualTo(170.0)
    }

    @Test
    fun aggregateSeriesRecord_noData() = runTest {
        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(StepsCadenceRecord.RATE_AVG in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MAX in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MIN in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(aggregationResult[StepsCadenceRecord.RATE_AVG]).isEqualTo(177.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MAX]).isEqualTo(181.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MIN]).isEqualTo(170.0)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords_oneMetric() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_MAX,
                        ),
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertThat(aggregationResult[StepsCadenceRecord.RATE_MAX]).isEqualTo(181.0)
        assertThat(StepsCadenceRecord.RATE_AVG in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MIN in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords_instantTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 18.minutes, rate = 182.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter =
                        TimeRangeFilter.between(START_TIME + 7.minutes, START_TIME + 18.minutes)
                )
            )

        assertThat(aggregationResult[StepsCadenceRecord.RATE_AVG]).isEqualTo(180.5)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MAX]).isEqualTo(181.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MIN]).isEqualTo(180.0)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords_instantTimeRangeFilterOutOfBounds() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 18.minutes, rate = 182.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter =
                        TimeRangeFilter.after(
                            START_TIME + 19.minutes,
                        )
                )
            )

        assertThat(StepsCadenceRecord.RATE_AVG in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MAX in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MIN in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords_localTimeRangeFilter() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 18.minutes, rate = 182.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            LocalDateTime.ofInstant(
                                START_TIME + 7.minutes + 2.hours,
                                ZoneOffset.ofHours(-2)
                            ),
                            LocalDateTime.ofInstant(
                                START_TIME + 18.minutes + 2.hours,
                                ZoneOffset.ofHours(-2)
                            )
                        )
                )
            )

        assertThat(aggregationResult[StepsCadenceRecord.RATE_AVG]).isEqualTo(180.5)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MAX]).isEqualTo(181.0)
        assertThat(aggregationResult[StepsCadenceRecord.RATE_MIN]).isEqualTo(180.0)
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateSeriesRecord_multipleRecords_localTimeRangeFilterOutOfBounds() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 7.minutes, rate = 180.0)
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 11.minutes,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 17.minutes, rate = 181.0),
                            StepsCadenceRecord.Sample(time = START_TIME + 18.minutes, rate = 182.0)
                        )
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics =
                        setOf(
                            StepsCadenceRecord.RATE_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN,
                        ),
                    timeRangeFilter =
                        TimeRangeFilter.before(
                            LocalDateTime.ofInstant(
                                START_TIME + 2.minutes + 2.hours,
                                ZoneOffset.ofHours(-2)
                            )
                        )
                )
            )

        assertThat(StepsCadenceRecord.RATE_AVG in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MAX in aggregationResult).isFalse()
        assertThat(StepsCadenceRecord.RATE_MIN in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateSeriesRecord_invalidMetrics_throws() = runTest {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                healthConnectClient.aggregateSeries<StepsCadenceRecord>(
                    AggregateRequest(
                        setOf(
                            SpeedRecord.SPEED_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN
                        ),
                        TimeRangeFilter.none(),
                        emptySet()
                    )
                )
            }
        }
    }

    @Test
    fun aggregateSeriesRecord_invalidSeriesRecord_throws() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                healthConnectClient.aggregateSeries<HeartRateRecord>(
                    AggregateRequest(
                        setOf(
                            HeartRateRecord.BPM_AVG,
                            HeartRateRecord.BPM_MAX,
                            HeartRateRecord.BPM_MIN
                        ),
                        TimeRangeFilter.none(),
                        emptySet()
                    )
                )
            }
        }
    }

    private val Int.days: Duration
        get() = Duration.ofDays(this.toLong())

    private val Int.hours: Duration
        get() = Duration.ofHours(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
