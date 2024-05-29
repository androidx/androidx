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

import androidx.health.connect.client.records.NutritionRecord
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
    fun dibByZero_returnsZero() {
        val dividend = Duration.ofHours(1)
        val divisor = Duration.ofSeconds(0)
        assertThat(dividend / divisor).isEqualTo(0.0)
    }

    @Test
    fun minus() {
        val a = Instant.now()
        val b = a.plusSeconds(5)
        assertThat(b - a).isEqualTo(Duration.ofSeconds(5))
    }

    @Test
    fun useLocalTime() {
        assertThat(TimeRangeFilter.none().useLocalTime()).isFalse()
        assertThat(
                TimeRangeFilter.between(Instant.now(), Instant.now().plusSeconds(2)).useLocalTime()
            )
            .isFalse()
        assertThat(TimeRangeFilter.after(Instant.now()).useLocalTime()).isFalse()
        assertThat(TimeRangeFilter.before(Instant.now()).useLocalTime()).isFalse()

        assertThat(
                TimeRangeFilter.between(LocalDateTime.now(), LocalDateTime.now().plusSeconds(2))
                    .useLocalTime()
            )
            .isTrue()
        assertThat(TimeRangeFilter.after(LocalDateTime.now()).useLocalTime()).isTrue()
        assertThat(TimeRangeFilter.before(LocalDateTime.now()).useLocalTime()).isTrue()
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
    fun intervalRecord_duration() {
        val startTime = Instant.now()
        val nutritionRecord =
            NutritionRecord(
                startTime = startTime,
                endTime = startTime.plusSeconds(10),
                startZoneOffset = null,
                endZoneOffset = null
            )
        assertThat(nutritionRecord.duration).isEqualTo(Duration.ofSeconds(10))
    }
}
