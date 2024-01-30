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
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MilestoneMarkerSummaryTest {
    fun Int.instant() = Instant.ofEpochMilli(toLong())
    fun Int.duration() = Duration.ofSeconds(toLong())

    @Test
    fun protoRoundTrip() {
        val goal = ExerciseGoal.createOneTimeGoal(
            DataTypeCondition(CALORIES_TOTAL, 125.0, ComparisonType.GREATER_THAN_OR_EQUAL)
        )
        val proto = MilestoneMarkerSummary(
            startTime = 15.instant(),
            endTime = 40.instant(),
            activeDuration = 20.duration(),
            achievedGoal = goal,
            summaryMetrics = DataPointContainer(
                listOf(DataPoints.caloriesTotal(130.0, 15.instant(), 35.instant()))
            )
        ).proto

        val summary = MilestoneMarkerSummary(proto)

        assertThat(summary.startTime).isEqualTo(15.instant())
        assertThat(summary.endTime).isEqualTo(40.instant())
        assertThat(summary.activeDuration).isEqualTo(20.duration())
        assertThat(summary.achievedGoal.dataTypeCondition.dataType).isEqualTo(CALORIES_TOTAL)
        assertThat(summary.summaryMetrics.getData(CALORIES_TOTAL)!!.total).isEqualTo(130.0)
    }
}
