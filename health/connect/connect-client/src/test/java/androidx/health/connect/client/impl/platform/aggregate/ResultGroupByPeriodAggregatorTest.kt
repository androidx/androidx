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

import android.annotation.SuppressLint
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.impl.platform.toInstantWithDefaultZoneFallback
import androidx.health.connect.client.impl.platform.toLocalTimeWithDefaultZoneFallback
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.Metadata.Companion.RECORDING_METHOD_MANUAL_ENTRY
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilograms
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("NewApi")
@RunWith(AndroidJUnit4::class)
class ResultGroupByPeriodAggregatorTest {

    @Test
    fun getResult_filterShorterThanPeriod_singleBucket() {
        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                    Instant.ofEpochMilli(1000).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                ),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100),
                endTime = Instant.ofEpochMilli(1000),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 5.grams
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    result = aggregationResult(5.grams, "some.package"),
                    startTime =
                        Instant.ofEpochMilli(100)
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                    endTime =
                        Instant.ofEpochMilli(1000)
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                )
            )
    }

    @Test
    fun getResult_filterEndTimeMatchingBucketEnd_bucketBoundariesMatchFilterBoundaries() {
        val filterStartTime =
            Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
        val filterEndTime = filterStartTime + Period.ofDays(2)

        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(filterStartTime, filterEndTime),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.minusDays(1).toInstant(ZoneOffset.UTC),
                endTime = filterEndTime.plusDays(1).toInstant(ZoneOffset.UTC),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 10.grams
            )
        )

        // Slice factor is 0.25 for each bucket, multiplied by 10 grams, remaining 0.5 is outside of
        // time range filter
        val bucketResult = aggregationResult(2.5.grams, "some.package")

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    bucketResult,
                    startTime = filterStartTime,
                    endTime = filterStartTime.plusDays(1)
                ),
                AggregationResultGroupedByPeriod(
                    bucketResult,
                    startTime = filterStartTime.plusDays(1),
                    filterEndTime
                )
            )
    }

    @Test
    fun getResult_recordsAcrossMultipleBuckets_onlyReturnsBucketsWithRecords() {
        val filterStartTime = Instant.ofEpochMilli(100)
        val filterEndTime = filterStartTime + Period.ofDays(5)

        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(
                    filterStartTime.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                    filterEndTime.toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                ),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        // Out of bounds records
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1),
                endTime = Instant.ofEpochMilli(99),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("out.package")
                    ),
                transFat = 10.grams
            )
        )

        // Record in bucket 1 (skip bucket 0)
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.plus(Duration.ofDays(1)).plusMillis(1),
                endTime = filterStartTime.plus(Duration.ofDays(1)).plusMillis(100),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("bucket_1.package")
                    ),
                transFat = 10.grams
            )
        )

        // Record in bucket 2
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.plus(Duration.ofDays(2)),
                endTime = filterStartTime.plus(Duration.ofDays(2)).plusMillis(100),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("bucket_2.package")
                    ),
                transFat = 100.grams
            )
        )

        // Record split in half between buckets 1 and 2
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.plus(Duration.ofDays(1)).plus(Duration.ofHours(12)),
                endTime = filterStartTime.plus(Duration.ofDays(2)).plus(Duration.ofHours(12)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("buckets_1_2.package")
                    ),
                transFat = 1.kilograms
            )
        )

        // Record 0.25 slice factor in bucket 4, remaining is beyond end of time range filter
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.plus(Duration.ofDays(4)).plus(Duration.ofHours(18)),
                endTime = filterStartTime.plus(Duration.ofDays(5)).plus(Duration.ofHours(18)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("bucket_4.package")
                    ),
                transFat = 10.kilograms
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    aggregationResult(510.grams, "bucket_1.package", "buckets_1_2.package"),
                    startTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(1),
                    endTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(2)
                ),
                AggregationResultGroupedByPeriod(
                    aggregationResult(600.grams, "bucket_2.package", "buckets_1_2.package"),
                    startTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(2),
                    endTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(3)
                ),
                AggregationResultGroupedByPeriod(
                    aggregationResult(2.5.kilograms, "bucket_4.package"),
                    startTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(4),
                    endTime =
                        filterStartTime
                            .toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                            .plusDays(5)
                ),
            )
    }

    @Test
    fun getResult_multipleZoneOffsets_resultCalculatedBasedOnLocalTime() {
        val filterStartTime = LocalDateTime.parse("2024-12-13T00:00:00")
        val filterEndTime = filterStartTime.plusDays(2)

        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(filterStartTime, filterEndTime),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        // Record only in first bucket
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.toInstantWithDefaultZoneFallback(ZoneOffset.ofHours(2)),
                endTime =
                    filterStartTime
                        .toInstantWithDefaultZoneFallback(ZoneOffset.ofHours(3))
                        .plus(Duration.ofDays(1))
                        .minusMillis(100),
                startZoneOffset = ZoneOffset.ofHours(2),
                endZoneOffset = ZoneOffset.ofHours(3),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("first.package")
                    ),
                transFat = 10.grams
            )
        )

        // Record split in half between first and second bucket
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    filterStartTime
                        .toInstantWithDefaultZoneFallback(ZoneOffset.UTC)
                        .plus(Duration.ofHours(12)),
                endTime =
                    filterStartTime
                        .toInstantWithDefaultZoneFallback(ZoneOffset.UTC)
                        .plus(Duration.ofDays(1))
                        .plus(Duration.ofHours(12)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("first_second.package")
                    ),
                transFat = 100.grams
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    aggregationResult(60.grams, "first.package", "first_second.package"),
                    startTime = filterStartTime,
                    endTime = filterStartTime.plusDays(1)
                ),
                AggregationResultGroupedByPeriod(
                    aggregationResult(50.grams, "first_second.package"),
                    startTime = filterStartTime.plusDays(1),
                    endTime = filterEndTime
                )
            )
    }

    @Test
    fun getResult_monthlyBucket_bucketsSplitByMonth() {
        val filterStartTime = LocalDateTime.parse("2024-01-27T00:00:00")
        val filterEndTime = LocalDateTime.parse("2024-03-31T13:00:00")

        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(filterStartTime, filterEndTime),
                bucketPeriod = Period.ofMonths(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        // January (out of range)
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.parse("2024-01-04T10:12:30.00Z"),
                endTime = Instant.parse("2024-01-15T10:11:30.00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("january_out.package")
                    ),
                transFat = 100.kilograms
            )
        )

        // January (within range)
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.parse("2024-01-28T10:15:30.00Z"),
                endTime = Instant.parse("2024-01-28T10:17:30.00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("january.package")
                    ),
                transFat = 10.grams
            )
        )

        // February
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.parse("2024-02-28T10:15:30.00Z"),
                endTime = Instant.parse("2024-02-29T10:17:30.00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("february.package")
                    ),
                transFat = 100.grams
            )
        )

        // March
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.parse("2024-03-27T10:15:30.00Z"),
                endTime = Instant.parse("2024-03-27T10:17:30.00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("march.package")
                    ),
                transFat = 1.kilograms
            )
        )

        // March (half within_range)
        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.parse("2024-03-31T12:00:00.00Z"),
                endTime = Instant.parse("2024-03-31T14:00:00.00Z"),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("march_half.package")
                    ),
                transFat = 10.kilograms
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByPeriod(
                    aggregationResult(10.grams, "january.package"),
                    filterStartTime,
                    endTime = LocalDateTime.parse("2024-02-27T00:00:00")
                ),
                AggregationResultGroupedByPeriod(
                    aggregationResult(100.grams, "february.package"),
                    startTime = LocalDateTime.parse("2024-02-27T00:00:00"),
                    endTime = LocalDateTime.parse("2024-03-27T00:00:00")
                ),
                AggregationResultGroupedByPeriod(
                    aggregationResult(6.kilograms, "march.package", "march_half.package"),
                    startTime = LocalDateTime.parse("2024-03-27T00:00:00"),
                    filterEndTime
                ),
            )
    }

    @Test
    fun getResult_recordContributingToAggregation_returnsListWithResult() {
        val aggregator =
            ResultGroupedByPeriodAggregator(
                createLocalTimeRange(
                    TimeRangeFilter.after(
                        Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                    )
                ),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100),
                endTime = Instant.ofEpochMilli(1000),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 5.grams
            )
        )

        assertThat(aggregator.getResult()).isNotEmpty()
    }

    @Test
    fun getResult_recordNotContributingToAggregation_returnsEmptyList() {
        val aggregator =
            ResultGroupedByPeriodAggregator(
                createLocalTimeRange(
                    TimeRangeFilter.after(
                        Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                    )
                ),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100),
                endTime = Instant.ofEpochMilli(1000),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
            )
        )

        assertThat(aggregator.getResult()).isEmpty()
    }

    @Test
    fun getResult_recordOutOfBounds_returnsEmptyList() {
        val aggregator =
            ResultGroupedByPeriodAggregator(
                LocalTimeRange(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                    Instant.ofEpochMilli(200).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                ),
                bucketPeriod = Period.ofDays(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100).plus(Duration.ofDays(10)),
                endTime = Instant.ofEpochMilli(1000).plus(Duration.ofDays(10)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 5.grams
            )
        )

        assertThat(aggregator.getResult()).isEmpty()
    }

    private companion object {
        private fun aggregationResult(transFatTotalMass: Mass, vararg packageNames: String) =
            AggregationResult(
                longValues = emptyMap(),
                doubleValues =
                    mapOf(NutritionRecord.TRANS_FAT_TOTAL.metricKey to transFatTotalMass.inGrams),
                dataOrigins = packageNames.map { DataOrigin(it) }.toSet()
            )
    }
}
