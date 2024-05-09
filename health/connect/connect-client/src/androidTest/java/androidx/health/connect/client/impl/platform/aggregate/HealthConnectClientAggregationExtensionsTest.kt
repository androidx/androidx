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
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
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

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(NutritionRecord::class, TimeRangeFilter.none())
        healthConnectClient.deleteRecords(StepsRecord::class, TimeRangeFilter.none())
    }

    @Test
    fun aggregateFallback_sdkExt10AndAbove() = runTest {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.3),
                    calcium = Mass.grams(0.1),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(AggregateRequest(
                metrics = setOf(NutritionRecord.TRANS_FAT_TOTAL, NutritionRecord.CALCIUM_TOTAL),
                timeRangeFilter = TimeRangeFilter.none()
            ))

        assertThat(NutritionRecord.TRANS_FAT_TOTAL in aggregationResult).isFalse()
        assertThat(NutritionRecord.CALCIUM_TOTAL in aggregationResult).isFalse()

        assertThat(aggregationResult.dataOrigins).isEmpty()
    }

    @Test
    fun aggregateFallback_belowSdkExt10() = runTest {
        assumeFalse(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)

        healthConnectClient.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = START_TIME,
                    endTime = START_TIME + 1.minutes,
                    transFat = Mass.grams(0.3),
                    calcium = Mass.grams(0.1),
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            )
        )

        val aggregationResult =
            healthConnectClient.aggregateFallback(AggregateRequest(
                metrics = setOf(NutritionRecord.TRANS_FAT_TOTAL, NutritionRecord.CALCIUM_TOTAL),
                timeRangeFilter = TimeRangeFilter.none()
            ))

        assertThat(aggregationResult[NutritionRecord.TRANS_FAT_TOTAL]).isEqualTo(Mass.grams(0.3))
        assertThat(NutritionRecord.CALCIUM_TOTAL in aggregationResult).isFalse()

        assertThat(aggregationResult.dataOrigins)
            .containsExactly(DataOrigin(context.packageName))
    }

    @Test
    fun readRecordsFlow_noFilters_readsAllInsertedRecords() = runTest {
        insertManyStepsRecords()

        val count = healthConnectClient.readRecordsFlow(
            StepsRecord::class,
            TimeRangeFilter.none(),
            emptySet()
        ).fold(0) { currentCount, records ->
            currentCount + records.size
        }

        assertThat(count).isEqualTo(10_000L)
    }

    @Test
    fun readRecordsFlow_timeRangeFilter_readsFilteredRecords() = runTest {
        assumeTrue(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 10)
        insertManyStepsRecords()

        val count = healthConnectClient.readRecordsFlow(
            StepsRecord::class,
            TimeRangeFilter.between(START_TIME + 10_000.seconds, START_TIME + 90_000.seconds),
            emptySet()
        ).fold(0) { currentCount, records ->
            currentCount + records.size
        }

        assertThat(count).isEqualTo(8_000L)
    }

    // TODO(b/337195270): Test with data origins from multiple apps
    @Test
    fun readRecordsFlow_insertedDataOriginFilter_readsAllInsertedRecords() = runTest {
        insertManyStepsRecords()

        val count = healthConnectClient.readRecordsFlow(
            StepsRecord::class,
            TimeRangeFilter.none(),
            setOf(DataOrigin(context.packageName))
        ).fold(0) { currentCount, records ->
            currentCount + records.size
        }

        assertThat(count).isEqualTo(10_000L)
    }

    @Test
    fun readRecordsFlow_nonExistingDataOriginFilter_doesNotReadAnyRecord() = runTest {
        insertManyStepsRecords()

        val count = healthConnectClient.readRecordsFlow(
            StepsRecord::class,
            TimeRangeFilter.none(),
            setOf(DataOrigin("some random package name"))
        ).fold(0) { currentCount, records ->
            currentCount + records.size
        }

        assertThat(count).isEqualTo(0L)
    }

    private suspend fun insertManyStepsRecords() {
        // Insert a large number of step records, bigger than the default page size
        for (i in 0..9) {
            healthConnectClient.insertRecords(List(1000) {
                val startTime = START_TIME + (i * 10_000 + it * 10).seconds
                StepsRecord(
                    startTime = startTime,
                    endTime = startTime + 5.seconds,
                    count = 10L,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            })
        }
    }

    private val Int.seconds: Duration
        get() = Duration.ofSeconds(this.toLong())

    private val Int.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
