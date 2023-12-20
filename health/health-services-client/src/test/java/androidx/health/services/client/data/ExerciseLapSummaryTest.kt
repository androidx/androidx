/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client.data

import androidx.health.services.client.data.DataType.Companion.CALORIES_TOTAL
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM_STATS
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseLapSummaryTest {

    @Test
    fun protoRoundTrip() {
        val startTime = Instant.parse("2021-03-02T14:00:00.00Z")
        val endTime = Instant.parse("2021-03-02T14:12:00.00Z").truncatedTo(ChronoUnit.SECONDS)
        val proto = ExerciseLapSummary(
            lapCount = 1,
            startTime = startTime,
            endTime = endTime,
            activeDuration = Duration.ofMinutes(10),
            lapMetrics = DataPointContainer(
                listOf(
                    DataPoints.caloriesTotal(
                        kilocalories = 1000.0,
                        startTime = startTime,
                        endTime = endTime
                    ),
                    DataPoints.heartRateStats(
                        minBpm = 50.0,
                        maxBpm = 150.0,
                        averageBpm = 100.0,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
            )
        ).proto

        val summary = ExerciseLapSummary(proto)

        assertThat(summary.lapCount).isEqualTo(1)
        assertThat(summary.startTime).isEqualTo(startTime)
        assertThat(summary.endTime).isEqualTo(endTime)
        assertThat(summary.activeDuration).isEqualTo(Duration.ofMinutes(10))
        val calories = summary.lapMetrics.getData(CALORIES_TOTAL)!!
        assertThat(calories.dataType).isEqualTo(CALORIES_TOTAL)
        assertThat(calories.total).isEqualTo(1000.0)
        assertThat(calories.start).isEqualTo(startTime)
        assertThat(calories.end).isEqualTo(endTime)
        val hrStats = summary.lapMetrics.getData(HEART_RATE_BPM_STATS)!!
        assertThat(hrStats.dataType).isEqualTo(HEART_RATE_BPM_STATS)
        assertThat(hrStats.min).isEqualTo(50.0)
        assertThat(hrStats.max).isEqualTo(150.0)
        assertThat(hrStats.average).isEqualTo(100.0)
        assertThat(hrStats.start).isEqualTo(startTime)
        assertThat(hrStats.end).isEqualTo(endTime)
    }
}
