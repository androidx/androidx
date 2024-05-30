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
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.metadata.DataOrigin
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
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Before
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

    @Before
    fun setUp() = runTest {
        // SDK ext 10 and above don't process any fallback metrics
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)
    }

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
    fun aggregateCyclingSpeed() = runTest {
        healthConnectClient.insertRecords(
            listOf(
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                    samples = listOf()
                ),
                StepsCadenceRecord(
                    startTime = START_TIME + 16.minutes,
                    endTime = START_TIME + 20.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
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
                healthConnectClient.aggregateSeriesRecord(
                    recordType = StepsCadenceRecord::class,
                    aggregateMetrics =
                        setOf(
                            SpeedRecord.SPEED_AVG,
                            StepsCadenceRecord.RATE_MAX,
                            StepsCadenceRecord.RATE_MIN
                        ),
                    timeRangeFilter = TimeRangeFilter.none(),
                    dataOriginFilter = emptySet()
                ) {
                    samples.map { SampleInfo(time = it.time, value = it.rate) }
                }
            }
        }
    }

    @Test
    fun aggregateSeriesRecord_invalidSeriesRecord_throws() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                healthConnectClient.aggregateSeriesRecord(
                    recordType = HeartRateRecord::class,
                    aggregateMetrics =
                        setOf(
                            HeartRateRecord.BPM_AVG,
                            HeartRateRecord.BPM_MAX,
                            HeartRateRecord.BPM_MIN
                        ),
                    timeRangeFilter = TimeRangeFilter.none(),
                    dataOriginFilter = emptySet()
                ) {
                    samples.map { SampleInfo(time = it.time, value = it.beatsPerMinute.toDouble()) }
                }
            }
        }
    }

    @Test
    fun sampleInfoIsWithin_noneTimeRangeFilter_returnsTrue() {
        val sampleInfo = SampleInfo(time = START_TIME, value = 0.0)
        val timeRangeFilter = TimeRangeFilter.none()

        assertThat(sampleInfo.isWithin(timeRangeFilter = timeRangeFilter, zoneOffset = null))
            .isTrue()
    }

    @Test
    fun sampleInfoIsWithin_instantTimeRangeFilter_between() {
        val sampleInfo = SampleInfo(time = START_TIME, value = 0.0)
        val zoneOffset = ZoneOffset.ofHours(2)

        var timeRangeFilter =
            TimeRangeFilter.between(START_TIME - 2.minutes, START_TIME + 2.minutes)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()

        timeRangeFilter = TimeRangeFilter.between(START_TIME - 2.minutes, START_TIME)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()

        timeRangeFilter = TimeRangeFilter.between(START_TIME, START_TIME + 2.minutes)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()

        timeRangeFilter = TimeRangeFilter.between(START_TIME + 1.minutes, START_TIME + 2.minutes)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()
    }

    @Test
    fun sampleInfoIsWithin_instantTimeRangeFilter_openEnded() {
        val sampleInfo = SampleInfo(time = START_TIME, value = 0.0)
        val zoneOffset = ZoneOffset.ofHours(2)

        var timeRangeFilter = TimeRangeFilter.after(START_TIME)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()

        timeRangeFilter = TimeRangeFilter.after(START_TIME + 1.minutes)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()

        timeRangeFilter = TimeRangeFilter.before(START_TIME)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()

        timeRangeFilter = TimeRangeFilter.before(START_TIME + 1.minutes)
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()
    }

    @Test
    fun sampleInfoIsWithin_localTimeRangeFilter_between() {
        val sampleInfo = SampleInfo(time = START_TIME, value = 0.0)
        val zoneOffset = ZoneOffset.ofHours(2)

        var timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.ofInstant(START_TIME - 2.minutes, ZoneOffset.UTC),
                LocalDateTime.ofInstant(START_TIME + 2.minutes, ZoneOffset.UTC)
            )
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()

        timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.ofInstant(START_TIME - 2.minutes, zoneOffset),
                LocalDateTime.ofInstant(START_TIME + 2.minutes, zoneOffset)
            )
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()

        timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.ofInstant(START_TIME - 2.minutes, zoneOffset),
                LocalDateTime.ofInstant(START_TIME, zoneOffset)
            )
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()

        timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.ofInstant(START_TIME, zoneOffset),
                LocalDateTime.ofInstant(START_TIME + 2.minutes, zoneOffset)
            )
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isTrue()

        timeRangeFilter =
            TimeRangeFilter.between(
                LocalDateTime.ofInstant(START_TIME + 1.minutes, zoneOffset),
                LocalDateTime.ofInstant(START_TIME + 2.minutes, zoneOffset)
            )
        assertThat(sampleInfo.isWithin(timeRangeFilter, zoneOffset)).isFalse()
    }

    private val Int.hours: Duration
        get() = Duration.ofHours(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
