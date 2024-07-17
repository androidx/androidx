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

package androidx.health.connect.client.request

import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

private val METRICS = setOf(HeartRateRecord.MEASUREMENTS_COUNT)

private val MINUTE_BASED_DURATION = Duration.ofMinutes(1)
private val SECOND_BASED_DURATION = Duration.ofSeconds(70)

private val NOW = Instant.ofEpochMilli(1234512345L)
private val ONE_HOUR_AGO = NOW.minus(Duration.ofHours(1))

@RunWith(AndroidJUnit4::class)
class AggregateGroupByDurationRequestTest {

    @Test
    fun instantTimeRange_success() {
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.before(ONE_HOUR_AGO),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.between(ONE_HOUR_AGO, NOW),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.after(NOW),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )

        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.before(ONE_HOUR_AGO),
            timeRangeSlicer = SECOND_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.between(ONE_HOUR_AGO, NOW),
            timeRangeSlicer = SECOND_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.after(NOW),
            timeRangeSlicer = SECOND_BASED_DURATION
        )
    }

    @Test
    fun localTimeRange_minuteBasedDuration_success() {
        val oneHourAgo = LocalDateTime.ofInstant(ONE_HOUR_AGO, ZoneOffset.UTC)
        val now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)

        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.before(oneHourAgo),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.between(oneHourAgo, now),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )
        AggregateGroupByDurationRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.after(now),
            timeRangeSlicer = MINUTE_BASED_DURATION
        )
    }

    @Test
    fun localTimeRange_secondBasedDuration_throws() {
        val oneHourAgo = LocalDateTime.ofInstant(ONE_HOUR_AGO, ZoneOffset.UTC)
        val now = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)

        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByDurationRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.before(oneHourAgo),
                timeRangeSlicer = SECOND_BASED_DURATION
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByDurationRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.between(oneHourAgo, now),
                timeRangeSlicer = SECOND_BASED_DURATION
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByDurationRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.after(now),
                timeRangeSlicer = SECOND_BASED_DURATION
            )
        }
    }
}
