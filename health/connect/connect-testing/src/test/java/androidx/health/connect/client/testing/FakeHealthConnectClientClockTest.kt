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

package androidx.health.connect.client.testing

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Length
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Tests for the [Clock] parameter in [FakeHealthConnectClient] which is used for open-ended time
 * ranges. It unit tests the [isWithin] and [sanitize] functions.
 */
class FakeHealthConnectClientClockTest {
    private val fixedInstant = Instant.parse("2000-01-01T10:00:00Z")

    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val zoneOffset = clock.zone.rules.getOffset(fixedInstant)

    private val record1 =
        ExerciseSessionRecord(
            startTime = fixedInstant.minusSeconds(60),
            startZoneOffset = zoneOffset,
            endTime = fixedInstant.minusSeconds(30),
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Record1",
            exerciseRoute = null,
            metadata = Metadata(clientRecordId = "FakeHealthConnectData1")
        )

    private val record2 =
        ExerciseSessionRecord(
            startTime = fixedInstant.minusSeconds(29),
            startZoneOffset = zoneOffset,
            endTime = fixedInstant.minusSeconds(1),
            endZoneOffset = zoneOffset,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            title = "Record2",
            exerciseRoute = null,
            metadata = Metadata(clientRecordId = "FakeHealthConnectData2")
        )

    private val fake =
        FakeHealthConnectClient(clock = clock).apply {
            runBlocking { insertRecords(listOf(record1, record2)) }
        }

    @Test
    fun timeRangeFilter_openEnded_endTime() = runTest {
        // Read records that start when record 2 starts
        val records =
            fake.readRecords(
                ReadRecordsRequest(
                    record1::class,
                    // No endTime, defaults to clock
                    timeRangeFilter = TimeRangeFilter(startTime = record2.startTime),
                )
            )

        // Only record2 should be returned
        assertThat(records.records).hasSize(1)
        assertThat(records.records.first().title).isEqualTo(record2.title)
    }

    @Test
    fun timeRangeFilter_openEndedEndTime_clockMiddle() = runTest {
        // Record 2 is in the future for this clock
        val clock = Clock.fixed(record1.endTime.plusMillis(1), ZoneOffset.UTC)
        val fake =
            FakeHealthConnectClient(clock = clock).apply {
                runBlocking { insertRecords(listOf(record1, record2)) }
            }
        val records =
            fake.readRecords(
                ReadRecordsRequest(
                    record1::class,
                    // No endTime, defaults to clock
                    timeRangeFilter = TimeRangeFilter(startTime = record1.startTime.minusMillis(1)),
                )
            )

        // Only record1 should be returned
        assertThat(records.records).hasSize(1)
        assertThat(records.records.first().title).isEqualTo(record1.title)
    }

    @Test
    fun timeRangeFilter_openEndedStartTimeEpoch_endTimeIsEnd() = runTest {

        // Read records that end when record 2 ends
        val records =
            fake.readRecords(
                ReadRecordsRequest(
                    record1::class,
                    // No startTime defaults to EPOCH
                    timeRangeFilter = TimeRangeFilter(endTime = record2.endTime.plusMillis(1)),
                )
            )

        // Both records should be returned
        assertThat(records.records).hasSize(2)
    }

    @Test
    fun timeRangeFilter_openEndedStartTimeEpoch_endTimeIsMiddle() = runTest {

        // Read records that end when record 1 ends
        val records =
            fake.readRecords(
                ReadRecordsRequest(
                    record1::class,
                    // No startTime defaults to EPOCH
                    timeRangeFilter = TimeRangeFilter(endTime = record1.endTime.plusMillis(1)),
                )
            )

        // Only record1 should be returned
        assertThat(records.records).hasSize(1)
        assertThat(records.records.first().title).isEqualTo(record1.title)
    }

    @Test
    fun timeRangeFilterlocalTime_noEndTime() = runTest {

        // Read records that end when record 1 ends
        val records =
            fake.readRecords(
                ReadRecordsRequest(
                    record1::class,
                    // No endTime, defaults to clock
                    timeRangeFilter =
                        TimeRangeFilter(
                            localStartTime = LocalDateTime.of(2000, 1, 1, 9, 59, 30, 1)
                        ),
                )
            )

        // Only record1 should be returned
        assertThat(records.records).hasSize(1)
        assertThat(records.records.first().title).isEqualTo(record2.title)
    }

    @Test
    fun timeRangeFilter_noEndTimeInstant() = runTest {
        // Given a record with a fixed time, before the clock.
        val heightRecord =
            HeightRecord(
                time = fixedInstant.minusSeconds(29),
                metadata = Metadata(clientRecordId = "HeightRecord#1"),
                height = Length.meters(1.8),
                zoneOffset = zoneOffset
            )
        val fake =
            FakeHealthConnectClient(clock = clock).apply {
                runBlocking { insertRecords(listOf(heightRecord)) }
            }
        // Records that start before the record.
        val recordsIncluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No endTime, defaults to clock
                    timeRangeFilter = TimeRangeFilter(startTime = fixedInstant.minusSeconds(30)),
                )
            )
        // Records that start after the record.
        val recordsExcluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No endTime, defaults to clock
                    timeRangeFilter = TimeRangeFilter(startTime = fixedInstant.minusSeconds(1)),
                )
            )

        // Only record1 should be returned
        assertThat(recordsIncluding.records).hasSize(1)
        assertThat(recordsExcluding.records).hasSize(0)
    }

    @Test
    fun timeRangeFilterlocalTime_noEndTimeInstant() = runTest {
        // Given a record with a fixed time, before the clock.
        val heightRecord =
            HeightRecord(
                time = fixedInstant.minusSeconds(29),
                metadata = Metadata(clientRecordId = "HeightRecord#1"),
                height = Length.meters(1.8),
                zoneOffset = zoneOffset
            )
        val fake =
            FakeHealthConnectClient(clock = clock).apply {
                runBlocking { insertRecords(listOf(heightRecord)) }
            }
        // Records that start before the record.
        val recordsIncluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No endTime, defaults to clock
                    timeRangeFilter =
                        TimeRangeFilter(
                            localStartTime = LocalDateTime.of(2000, 1, 1, 9, 59, 30, 1)
                        ),
                )
            )
        // Records that start after the record.
        val recordsExcluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No endTime, defaults to clock
                    timeRangeFilter =
                        TimeRangeFilter(
                            localStartTime = LocalDateTime.of(2000, 1, 1, 9, 59, 31, 1)
                        ),
                )
            )

        // Only record1 should be returned
        assertThat(recordsIncluding.records).hasSize(1)
        assertThat(recordsExcluding.records).hasSize(0)
    }

    @Test
    fun timeRangeFilterlocalTime_noStartTimeInstant() = runTest {
        // Given a record with a fixed time, before the clock.
        val heightRecord =
            HeightRecord(
                time = fixedInstant.minusSeconds(29),
                metadata = Metadata(clientRecordId = "HeightRecord#1"),
                height = Length.meters(1.8),
                zoneOffset = zoneOffset
            )
        val fake =
            FakeHealthConnectClient(clock = clock).apply {
                runBlocking { insertRecords(listOf(heightRecord)) }
            }
        // Records that end before the record.
        val recordsIncluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No startTime, defaults to EPOCH
                    timeRangeFilter =
                        TimeRangeFilter(localEndTime = LocalDateTime.of(2000, 1, 1, 9, 59, 30)),
                )
            )
        // Records that end after the record.
        val recordsExcluding =
            fake.readRecords(
                ReadRecordsRequest(
                    heightRecord::class,
                    // No endTime, defaults to clock
                    timeRangeFilter =
                        TimeRangeFilter(localEndTime = LocalDateTime.of(2000, 1, 1, 9, 59, 59)),
                )
            )

        // Only record2 should be returned
        assertThat(recordsIncluding.records).hasSize(0)
        assertThat(recordsExcluding.records).hasSize(1)
    }
}
