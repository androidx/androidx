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
import java.time.Period
import java.time.ZoneOffset
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

private val METRICS = setOf(HeartRateRecord.MEASUREMENTS_COUNT)

private val PERIOD_SLICER = Period.ofDays(1)

private val NOW = Instant.ofEpochMilli(1234512345L)
private val ONE_HOUR_AGO = NOW.minus(Duration.ofHours(1))

@RunWith(AndroidJUnit4::class)
class AggregateGroupByPeriodRequestTest {

    @Test
    fun localTimeRange_success() {
        AggregateGroupByPeriodRequest(
            metrics = METRICS,
            timeRangeFilter =
                TimeRangeFilter.before(LocalDateTime.ofInstant(ONE_HOUR_AGO, ZoneOffset.UTC)),
            timeRangeSlicer = PERIOD_SLICER
        )
        AggregateGroupByPeriodRequest(
            metrics = METRICS,
            timeRangeFilter =
                TimeRangeFilter.between(
                    LocalDateTime.ofInstant(ONE_HOUR_AGO, ZoneOffset.UTC),
                    LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)
                ),
            timeRangeSlicer = PERIOD_SLICER
        )
        AggregateGroupByPeriodRequest(
            metrics = METRICS,
            timeRangeFilter = TimeRangeFilter.after(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)),
            timeRangeSlicer = PERIOD_SLICER
        )
    }

    @Test
    fun instantTimeRange_throws() {
        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByPeriodRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.before(ONE_HOUR_AGO),
                timeRangeSlicer = PERIOD_SLICER
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByPeriodRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.between(ONE_HOUR_AGO, NOW),
                timeRangeSlicer = PERIOD_SLICER
            )
        }
        assertFailsWith<IllegalArgumentException> {
            AggregateGroupByPeriodRequest(
                metrics = METRICS,
                timeRangeFilter = TimeRangeFilter.after(NOW),
                timeRangeSlicer = PERIOD_SLICER
            )
        }
    }
}
