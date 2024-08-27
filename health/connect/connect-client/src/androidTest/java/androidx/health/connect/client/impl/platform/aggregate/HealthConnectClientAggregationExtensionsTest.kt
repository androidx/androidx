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
import android.content.pm.PackageManager
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.impl.converters.datatype.RECORDS_CLASS_NAME_MAP
import androidx.health.connect.client.permission.HealthPermission.Companion.PERMISSION_PREFIX
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.metersPerSecond
import androidx.health.connect.client.units.millimetersOfMercury
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
class HealthConnectClientAggregationExtensionsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    private val allHealthPermissions =
        context.packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
            .requestedPermissions!!
            .filter { it.startsWith(PERMISSION_PREFIX) }
            .toTypedArray()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(*allHealthPermissions)

    @After
    fun tearDown() = runTest {
        for (recordType in RECORDS_CLASS_NAME_MAP.keys) {
            healthConnectClient.deleteRecords(recordType, TimeRangeFilter.none())
        }
    }

    @Test
    fun aggregateFallback_sdkExt10AndAbove() = runTest {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    diastolic = 70.millimetersOfMercury,
                    systolic = 110.millimetersOfMercury
                ),
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
                ),
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = 0.3.grams,
                    calcium = 0.1.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                ),
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            SpeedRecord.Sample(
                                time = START_TIME + 5.minutes,
                                speed = Velocity.metersPerSecond(2.8)
                            ),
                            SpeedRecord.Sample(
                                time = START_TIME + 10.minutes,
                                speed = Velocity.metersPerSecond(2.7)
                            )
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0)
                        )
                )
            )
        )

        // Adding calcium total (which has always been supported) to make sure it's filtered out of
        // the calculation.
        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics = AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10 + NutritionRecord.CALCIUM_TOTAL,
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        for (metric in AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10) {
            assertThat(metric in aggregationResult).isFalse()
        }
        assertThat(NutritionRecord.CALCIUM_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateFallback_belowSdkExt10() = runTest {
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                BloodPressureRecord(
                    time = START_TIME,
                    zoneOffset = ZoneOffset.UTC,
                    diastolic = 70.millimetersOfMercury,
                    systolic = 110.millimetersOfMercury
                ),
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
                ),
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = 0.3.grams,
                    calcium = 0.1.grams,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                ),
                SpeedRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 15.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            SpeedRecord.Sample(
                                time = START_TIME + 5.minutes,
                                speed = Velocity.metersPerSecond(2.8)
                            ),
                            SpeedRecord.Sample(
                                time = START_TIME + 10.minutes,
                                speed = Velocity.metersPerSecond(2.7)
                            )
                        )
                ),
                StepsCadenceRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 10.minutes,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples =
                        listOf(
                            StepsCadenceRecord.Sample(time = START_TIME + 3.minutes, rate = 170.0)
                        )
                )
            )
        )

        // Adding calcium total (which has always been supported) to make sure it's filtered out of
        // the calculation.
        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics = AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10 + NutritionRecord.CALCIUM_TOTAL,
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        assertEquals(
            aggregationResult[BloodPressureRecord.DIASTOLIC_AVG] to 70.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.DIASTOLIC_MAX] to 70.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.DIASTOLIC_MIN] to 70.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_AVG] to 110.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MAX] to 110.millimetersOfMercury,
            aggregationResult[BloodPressureRecord.SYSTOLIC_MIN] to 110.millimetersOfMercury,
            aggregationResult[CyclingPedalingCadenceRecord.RPM_AVG] to 85.0,
            aggregationResult[CyclingPedalingCadenceRecord.RPM_MAX] to 90.0,
            aggregationResult[CyclingPedalingCadenceRecord.RPM_MIN] to 80.0,
            aggregationResult[NutritionRecord.TRANS_FAT_TOTAL] to 0.3.grams,
            aggregationResult[SpeedRecord.SPEED_AVG] to 2.75.metersPerSecond,
            aggregationResult[SpeedRecord.SPEED_MAX] to 2.8.metersPerSecond,
            aggregationResult[SpeedRecord.SPEED_MIN] to 2.7.metersPerSecond,
            aggregationResult[StepsCadenceRecord.RATE_AVG] to 170.0,
            aggregationResult[StepsCadenceRecord.RATE_MAX] to 170.0,
            aggregationResult[StepsCadenceRecord.RATE_MIN] to 170.0,
        )

        assertThat(NutritionRecord.CALCIUM_TOTAL in aggregationResult).isFalse()
        assertThat(aggregationResult.dataOrigins).containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun aggregateFallback_belowSdkExt10NoData() = runTest {
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        val aggregationResult =
            healthConnectClient.aggregateFallback(
                AggregateRequest(
                    metrics = AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10,
                    timeRangeFilter = TimeRangeFilter.none()
                )
            )

        for (metric in AGGREGATE_METRICS_ADDED_IN_SDK_EXT_10) {
            assertThat(metric in aggregationResult).isFalse()
        }

        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun readRecordsFlow_noFilters_readsAllInsertedRecords() = runTest {
        insertManyStepsRecords()

        val count =
            healthConnectClient
                .readRecordsFlow(StepsRecord::class, TimeRangeFilter.none(), emptySet())
                .fold(0) { currentCount, records -> currentCount + records.size }

        assertThat(count).isEqualTo(10_000L)
    }

    @Test
    fun readRecordsFlow_timeRangeFilter_readsFilteredRecords() = runTest {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)
        insertManyStepsRecords()

        val count =
            healthConnectClient
                .readRecordsFlow(
                    StepsRecord::class,
                    TimeRangeFilter.between(
                        START_TIME + 10_000.seconds,
                        START_TIME + 90_000.seconds
                    ),
                    emptySet()
                )
                .fold(0) { currentCount, records -> currentCount + records.size }

        assertThat(count).isEqualTo(8_000L)
    }

    // TODO(b/337195270): Test with data origins from multiple apps
    @Test
    fun readRecordsFlow_insertedDataOriginFilter_readsAllInsertedRecords() = runTest {
        insertManyStepsRecords()

        val count =
            healthConnectClient
                .readRecordsFlow(
                    StepsRecord::class,
                    TimeRangeFilter.none(),
                    setOf(DataOrigin(context.packageName))
                )
                .fold(0) { currentCount, records -> currentCount + records.size }

        assertThat(count).isEqualTo(10_000L)
    }

    @Test
    fun readRecordsFlow_nonExistingDataOriginFilter_doesNotReadAnyRecord() = runTest {
        insertManyStepsRecords()

        val count =
            healthConnectClient
                .readRecordsFlow(
                    StepsRecord::class,
                    TimeRangeFilter.none(),
                    setOf(DataOrigin("some random package name"))
                )
                .fold(0) { currentCount, records -> currentCount + records.size }

        assertThat(count).isEqualTo(0L)
    }

    private suspend fun insertManyStepsRecords() {
        // Insert a large number of step records, bigger than the default page size
        for (i in 0..9) {
            healthConnectClient.insertRecords(
                List(1000) {
                    val startTime = START_TIME + (i * 10_000 + it * 10).seconds
                    StepsRecord(
                        startTime = startTime,
                        endTime = startTime + 5.seconds,
                        count = 10L,
                        startZoneOffset = ZoneOffset.UTC,
                        endZoneOffset = ZoneOffset.UTC
                    )
                }
            )
        }
    }

    private fun <A, E> assertEquals(vararg assertions: Pair<A, E>) {
        assertions.forEach { (actual, expected) -> assertThat(actual).isEqualTo(expected) }
    }

    private val Int.seconds: Duration
        get() = Duration.ofSeconds(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
