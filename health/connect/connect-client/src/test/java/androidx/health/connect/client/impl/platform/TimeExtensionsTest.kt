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

package androidx.health.connect.client.impl.platform

import androidx.health.connect.client.impl.platform.aggregate.InstantTimeRange
import androidx.health.connect.client.impl.platform.aggregate.LocalTimeRange
import androidx.health.connect.client.impl.platform.aggregate.createInstantTimeRange
import androidx.health.connect.client.impl.platform.aggregate.createLocalTimeRange
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.Metadata.Companion.RECORDING_METHOD_MANUAL_ENTRY
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimeExtensionsTest {

    @Test
    fun div() {
        val dividend = Duration.ofHours(1)
        val divisor = Duration.ofHours(4)
        assertThat(dividend / divisor).isEqualTo(0.25)
    }

    @Test
    fun minus() {
        val a = Instant.now()
        val b = a.plusSeconds(5)
        assertThat(b - a).isEqualTo(Duration.ofSeconds(5))
    }

    @Test
    fun toInstantWithDefaultZoneFallback() {
        val instant = Instant.now()
        val localDateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

        assertThat(localDateTime.toInstantWithDefaultZoneFallback(ZoneOffset.UTC))
            .isEqualTo(instant)
        assertThat(localDateTime.toInstantWithDefaultZoneFallback(ZoneOffset.ofHours(2)))
            .isEqualTo(instant - Duration.ofHours(2))
    }

    @Test
    fun isWithin_instantTimeRange_outOfBounds_returnsFalse() {
        var timeRange = createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(100)))
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange)).isFalse()

        timeRange = createInstantTimeRange(TimeRangeFilter.before(Instant.ofEpochMilli(100)))
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange)).isFalse()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange)).isFalse()

        timeRange = InstantTimeRange(Instant.ofEpochMilli(100), Instant.ofEpochMilli(200))
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange)).isFalse()
        assertThat(Instant.ofEpochMilli(200).isWithin(timeRange)).isFalse()
        assertThat(Instant.ofEpochMilli(201).isWithin(timeRange)).isFalse()
    }

    @Test
    fun isWithin_localTimeRange_outOfBounds_returnsFalse() {
        var timeRange =
            createLocalTimeRange(
                TimeRangeFilter.after(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                )
            )
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange, ZoneOffset.UTC)).isFalse()

        timeRange =
            createLocalTimeRange(
                TimeRangeFilter.before(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                )
            )
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange, ZoneOffset.UTC)).isFalse()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange, ZoneOffset.UTC)).isFalse()

        timeRange =
            LocalTimeRange(
                Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                Instant.ofEpochMilli(200).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
            )
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange, ZoneOffset.UTC)).isFalse()
        assertThat(Instant.ofEpochMilli(200).isWithin(timeRange, ZoneOffset.UTC)).isFalse()
        assertThat(Instant.ofEpochMilli(201).isWithin(timeRange, ZoneOffset.UTC)).isFalse()
    }

    @Test
    fun isWithin_instantTimeRange_withinBounds_returnsTrue() {
        var timeRange = createInstantTimeRange(TimeRangeFilter.after(Instant.ofEpochMilli(100)))
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange)).isTrue()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange)).isTrue()

        timeRange = createInstantTimeRange(TimeRangeFilter.before(Instant.ofEpochMilli(100)))
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange)).isTrue()

        timeRange = InstantTimeRange(Instant.ofEpochMilli(100), Instant.ofEpochMilli(200))
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange)).isTrue()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange)).isTrue()
        assertThat(Instant.ofEpochMilli(199).isWithin(timeRange)).isTrue()
    }

    @Test
    fun isWithin_localTimeRange_withinBounds_returnsTrue() {
        var timeRange =
            createLocalTimeRange(
                TimeRangeFilter.after(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                )
            )
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange, ZoneOffset.UTC)).isTrue()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange, ZoneOffset.UTC)).isTrue()

        timeRange =
            createLocalTimeRange(
                TimeRangeFilter.before(
                    Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
                )
            )
        assertThat(Instant.ofEpochMilli(99).isWithin(timeRange, ZoneOffset.UTC)).isTrue()

        timeRange =
            LocalTimeRange(
                Instant.ofEpochMilli(100).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC),
                Instant.ofEpochMilli(200).toLocalTimeWithDefaultZoneFallback(ZoneOffset.UTC)
            )
        assertThat(Instant.ofEpochMilli(100).isWithin(timeRange, ZoneOffset.UTC)).isTrue()
        assertThat(Instant.ofEpochMilli(101).isWithin(timeRange, ZoneOffset.UTC)).isTrue()
        assertThat(Instant.ofEpochMilli(199).isWithin(timeRange, ZoneOffset.UTC)).isTrue()
    }

    @Test
    fun intervalRecord_duration() {
        val startTime = Instant.now()
        val nutritionRecord =
            NutritionRecord(
                startTime = startTime,
                endTime = startTime.plusSeconds(10),
                startZoneOffset = null,
                endZoneOffset = null,
                metadata = Metadata(recordingMethod = RECORDING_METHOD_MANUAL_ENTRY),
            )
        assertThat(nutritionRecord.duration).isEqualTo(Duration.ofSeconds(10))
    }
}
