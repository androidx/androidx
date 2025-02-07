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

package androidx.health.connect.client.impl.platform.aggregate

import android.annotation.SuppressLint
import android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.grams
import androidx.health.connect.client.units.kilograms
import androidx.health.connect.client.units.kilometersPerHour
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Test
import org.junit.runner.RunWith

@SuppressLint("NewApi")
@RunWith(AndroidJUnit4::class)
class ResultGroupByDurationAggregatorTest {

    @Test
    fun getResult_localTimeRange_filterRecordsBasedOnLocalTime() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                LocalTimeRange(
                    startTime = LocalDateTime.parse("2025-02-03T00:00:00"),
                    endTime = LocalDateTime.parse("2025-02-03T02:00:00")
                ),
                bucketDuration = Duration.ofHours(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    LocalDateTime.parse("2025-02-03T00:10:00").toInstant(ZoneOffset.ofHours(10)),
                endTime =
                    LocalDateTime.parse("2025-02-03T00:15:00").toInstant(ZoneOffset.ofHours(10)),
                startZoneOffset = ZoneOffset.ofHours(10),
                endZoneOffset = ZoneOffset.ofHours(10),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("within.range")
                    ),
                transFat = 5.grams
            )
        )

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    LocalDateTime.parse("2025-02-03T00:20:00").toInstant(ZoneOffset.ofHours(10)),
                endTime =
                    LocalDateTime.parse("2025-02-03T00:25:00").toInstant(ZoneOffset.ofHours(10)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("out.of.range")
                    ),
                transFat = 15.grams
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = aggregationResult(5.grams, "within.range"),
                            startTime =
                                LocalDateTime.parse("2025-02-03T00:00:00")
                                    .toInstant(ZoneOffset.ofHours(10)),
                            endTime =
                                LocalDateTime.parse("2025-02-03T01:00:00")
                                    .toInstant(ZoneOffset.ofHours(10)),
                            zoneOffset = ZoneOffset.ofHours(10)
                        ),
                    minTime =
                        LocalDateTime.parse("2025-02-03T00:10:00").toInstant(ZoneOffset.ofHours(10))
                )
            )
    }

    @Test
    fun getResult_localTimeRange_bucketsAreCalculatedUsingInstantTime() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                LocalTimeRange(
                    startTime = LocalDateTime.parse("2025-02-03T00:00:00"),
                    endTime = LocalDateTime.parse("2025-02-03T02:00:00")
                ),
                bucketDuration = Duration.ofHours(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    LocalDateTime.parse("2025-02-03T00:30:00").toInstant(ZoneOffset.ofHours(1)),
                endTime =
                    LocalDateTime.parse("2025-02-03T00:35:00").toInstant(ZoneOffset.ofHours(1)),
                startZoneOffset = ZoneOffset.ofHours(1),
                endZoneOffset = ZoneOffset.ofHours(1),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("first.hour.offset1")
                    ),
                transFat = 5.grams
            )
        )

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    LocalDateTime.parse("2025-02-03T01:20:00").toInstant(ZoneOffset.ofHours(2)),
                endTime =
                    LocalDateTime.parse("2025-02-03T01:25:00").toInstant(ZoneOffset.ofHours(2)),
                startZoneOffset = ZoneOffset.ofHours(2),
                endZoneOffset = ZoneOffset.ofHours(2),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("second.hour.offset2")
                    ),
                transFat = 50.grams
            )
        )

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime =
                    LocalDateTime.parse("2025-02-03T01:10:00").toInstant(ZoneOffset.ofHours(3)),
                endTime =
                    LocalDateTime.parse("2025-02-03T01:15:00").toInstant(ZoneOffset.ofHours(3)),
                startZoneOffset = ZoneOffset.ofHours(3),
                endZoneOffset = ZoneOffset.ofHours(3),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("second.hour.offset3")
                    ),
                transFat = 500.grams
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = aggregationResult(500.grams, "second.hour.offset3"),
                            startTime =
                                LocalDateTime.parse("2025-02-03T01:00:00")
                                    .toInstant(ZoneOffset.ofHours(3)),
                            endTime =
                                LocalDateTime.parse("2025-02-03T02:00:00")
                                    .toInstant(ZoneOffset.ofHours(3)),
                            zoneOffset = ZoneOffset.ofHours(3)
                        ),
                    minTime =
                        LocalDateTime.parse("2025-02-03T01:10:00").toInstant(ZoneOffset.ofHours(3))
                ),
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result =
                                aggregationResult(
                                    55.grams,
                                    "first.hour.offset1",
                                    "second.hour.offset2"
                                ),
                            startTime =
                                LocalDateTime.parse("2025-02-03T01:00:00")
                                    .toInstant(ZoneOffset.ofHours(2)),
                            endTime =
                                LocalDateTime.parse("2025-02-03T02:00:00")
                                    .toInstant(ZoneOffset.ofHours(2)),
                            zoneOffset = ZoneOffset.ofHours(2)
                        ),
                    minTime =
                        LocalDateTime.parse("2025-02-03T01:20:00").toInstant(ZoneOffset.ofHours(2))
                )
            )
    }

    @Test
    fun getResult_filterShorterThanDuration_singleBucket() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                InstantTimeRange(Instant.ofEpochMilli(100), Instant.ofEpochMilli(1000)),
                bucketDuration = Duration.ofHours(1)
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
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = aggregationResult(5.grams, "some.package"),
                            startTime = Instant.ofEpochMilli(100),
                            endTime = Instant.ofEpochMilli(1000),
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = Instant.ofEpochMilli(100)
                )
            )
    }

    @Test
    fun getResult_filterEndTimeMatchingBucketEnd_bucketBoundariesMatchFilterBoundaries() {
        val filterStartTime = Instant.ofEpochMilli(100)
        val filterEndTime = filterStartTime + Duration.ofHours(2)

        val aggregator =
            ResultGroupedByDurationAggregator(
                InstantTimeRange(filterStartTime, filterEndTime),
                bucketDuration = Duration.ofHours(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = filterStartTime.minus(Duration.ofHours(1)),
                endTime = filterEndTime.plus(Duration.ofHours(1)),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 20.grams
            )
        )

        // Slice factor is 0.25 for each bucket, multiplied by 20 grams, remaining 0.5 is outside of
        // time range filter
        val bucketResult = aggregationResult(5.grams, "some.package")

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = bucketResult,
                            startTime = filterStartTime,
                            endTime = filterStartTime.plus(Duration.ofHours(1)),
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = filterStartTime.minus(Duration.ofHours(1))
                ),
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = bucketResult,
                            startTime = filterStartTime.plus(Duration.ofHours(1)),
                            endTime = filterEndTime,
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = filterStartTime.minus(Duration.ofHours(1))
                )
            )
    }

    @Test
    fun getResult_recordsAcrossMultipleBuckets_onlyReturnsBucketsWithRecords() {
        val filterStartTime = Instant.ofEpochMilli(100)
        val filterEndTime = filterStartTime + Duration.ofMinutes(5)

        val aggregator =
            ResultGroupedByDurationAggregator(
                InstantTimeRange(filterStartTime, filterEndTime),
                bucketDuration = Duration.ofMinutes(1)
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
                startTime = filterStartTime.plus(Duration.ofMinutes(1)).plusMillis(1),
                endTime = filterStartTime.plus(Duration.ofMinutes(1)).plusMillis(100),
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
                startTime = filterStartTime.plus(Duration.ofMinutes(2)),
                endTime = filterStartTime.plus(Duration.ofMinutes(2)).plusMillis(100),
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
                startTime = filterStartTime.plus(Duration.ofMinutes(1)).plusSeconds(30),
                endTime = filterStartTime.plus(Duration.ofMinutes(2)).plusSeconds(30),
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
                startTime = filterStartTime.plus(Duration.ofMinutes(4)).plusSeconds(45),
                endTime = filterStartTime.plus(Duration.ofMinutes(5)).plusSeconds(45),
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
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result =
                                aggregationResult(
                                    510.grams,
                                    "bucket_1.package",
                                    "buckets_1_2.package"
                                ),
                            startTime = filterStartTime.plus(Duration.ofMinutes(1)),
                            endTime = filterStartTime.plus(Duration.ofMinutes(2)),
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = filterStartTime.plus(Duration.ofMinutes(1)).plusMillis(1)
                ),
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result =
                                aggregationResult(
                                    600.grams,
                                    "bucket_2.package",
                                    "buckets_1_2.package"
                                ),
                            startTime = filterStartTime.plus(Duration.ofMinutes(2)),
                            endTime = filterStartTime.plus(Duration.ofMinutes(3)),
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = filterStartTime.plus(Duration.ofMinutes(1)).plusSeconds(30)
                ),
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            result = aggregationResult(2.5.kilograms, "bucket_4.package"),
                            startTime = filterStartTime.plus(Duration.ofMinutes(4)),
                            endTime = filterStartTime.plus(Duration.ofMinutes(5)),
                            zoneOffset = ZoneOffset.UTC
                        ),
                    minTime = filterStartTime.plus(Duration.ofMinutes(4)).plusSeconds(45)
                ),
            )
    }

    @Test
    fun getResult_multipleZoneOffsets_zoneOffsetBasedOnEarliestRecord() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(1))),
                bucketDuration = Duration.ofHours(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(1000),
                endTime = Instant.ofEpochMilli(2000),
                startZoneOffset = ZoneOffset.ofHours(2),
                endZoneOffset = ZoneOffset.ofHours(3),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                transFat = 10.grams
            )
        )

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100),
                endTime = Instant.ofEpochMilli(3000),
                startZoneOffset = ZoneOffset.ofHours(1),
                endZoneOffset = ZoneOffset.ofHours(6),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.other.package")
                    ),
                transFat = 20.grams
            )
        )

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(150),
                endTime = Instant.ofEpochMilli(2001),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("yet.some.other.package")
                    ),
                transFat = 30.grams
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            startTime = Instant.ofEpochMilli(1),
                            endTime = Instant.ofEpochMilli(1).plus(Duration.ofHours(1)),
                            result =
                                aggregationResult(
                                    60.grams,
                                    "some.package",
                                    "some.other.package",
                                    "yet.some.other.package"
                                ),
                            zoneOffset = ZoneOffset.ofHours(1)
                        ),
                    minTime = Instant.ofEpochMilli(100)
                )
            )
    }

    @Test
    fun getResultForSeriesRecord_multipleZoneOffsets_zoneOffsetBasedOnEarliestSample() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(110))),
                bucketDuration = Duration.ofHours(1)
            ) {
                SeriesAggregationProcessor(SpeedRecord::class, setOf(SpeedRecord.SPEED_MAX), it)
            }

        aggregator.filterAndAggregate(
            SpeedRecord(
                startTime = Instant.ofEpochMilli(1000),
                endTime = Instant.ofEpochMilli(2000),
                startZoneOffset = ZoneOffset.ofHours(2),
                endZoneOffset = ZoneOffset.ofHours(3),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.package")
                    ),
                samples = emptyList()
            )
        )

        aggregator.filterAndAggregate(
            SpeedRecord(
                startTime = Instant.ofEpochMilli(100),
                endTime = Instant.ofEpochMilli(3000),
                startZoneOffset = ZoneOffset.ofHours(1),
                endZoneOffset = ZoneOffset.ofHours(6),
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("some.other.package")
                    ),
                samples =
                    listOf(
                        SpeedRecord.Sample(Instant.ofEpochMilli(100), 20.kilometersPerHour),
                        SpeedRecord.Sample(Instant.ofEpochMilli(120), 15.kilometersPerHour)
                    )
            )
        )

        aggregator.filterAndAggregate(
            SpeedRecord(
                startTime = Instant.ofEpochMilli(150),
                endTime = Instant.ofEpochMilli(2001),
                startZoneOffset = ZoneOffset.UTC,
                endZoneOffset = ZoneOffset.UTC,
                metadata =
                    Metadata(
                        recordingMethod = RECORDING_METHOD_MANUAL_ENTRY,
                        dataOrigin = DataOrigin("yet.some.other.package")
                    ),
                samples =
                    listOf(SpeedRecord.Sample(Instant.ofEpochMilli(156), 34.kilometersPerHour))
            )
        )

        assertThat(aggregator.getResult())
            .containsExactly(
                AggregationResultGroupedByDurationWithMinTime(
                    aggregationResultGroupedByDuration =
                        AggregationResultGroupedByDuration(
                            startTime = Instant.ofEpochMilli(110),
                            endTime = Instant.ofEpochMilli(110).plus(Duration.ofHours(1)),
                            result =
                                AggregationResult(
                                    longValues = emptyMap(),
                                    doubleValues =
                                        mapOf(
                                            SpeedRecord.SPEED_MAX.metricKey to
                                                34.kilometersPerHour.inMetersPerSecond
                                        ),
                                    dataOrigins =
                                        setOf(
                                            DataOrigin("some.other.package"),
                                            DataOrigin("yet.some.other.package")
                                        )
                                ),
                            zoneOffset = ZoneOffset.ofHours(1)
                        ),
                    minTime = Instant.ofEpochMilli(120)
                )
            )
    }

    @Test
    fun getResult_recordContributingToAggregation_returnsListWithResult() {
        val aggregator =
            ResultGroupedByDurationAggregator(
                createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(100))),
                bucketDuration = Duration.ofMinutes(1)
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
            ResultGroupedByDurationAggregator(
                createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(100))),
                bucketDuration = Duration.ofMinutes(1)
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
            ResultGroupedByDurationAggregator(
                InstantTimeRange(Instant.ofEpochMilli(100), Instant.ofEpochMilli(200)),
                bucketDuration = Duration.ofMinutes(1)
            ) {
                TransFatTotalAggregationProcessor(it)
            }

        aggregator.filterAndAggregate(
            NutritionRecord(
                startTime = Instant.ofEpochMilli(100).plus(Duration.ofMinutes(10)),
                endTime = Instant.ofEpochMilli(1000).plus(Duration.ofMinutes(10)),
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
