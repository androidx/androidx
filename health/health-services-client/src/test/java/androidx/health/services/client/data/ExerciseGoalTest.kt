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

import androidx.health.services.client.data.ComparisonType.Companion.GREATER_THAN
import androidx.health.services.client.data.DataType.Companion.DISTANCE_TOTAL
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM_STATS
import androidx.health.services.client.data.ExerciseGoalType.Companion.MILESTONE
import androidx.health.services.client.data.ExerciseGoalType.Companion.ONE_TIME_GOAL
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExerciseGoalTest {

    @Test
    fun protoRoundTrip_oneTime() {
        val proto = ExerciseGoal.createOneTimeGoal(
            DataTypeCondition(HEART_RATE_BPM_STATS, 145.0, GREATER_THAN)
        ).proto

        val goal = ExerciseGoal.fromProto(proto)

        assertThat(goal.exerciseGoalType).isEqualTo(ONE_TIME_GOAL)
        assertThat(goal.dataTypeCondition.dataType).isEqualTo(HEART_RATE_BPM_STATS)
        assertThat(goal.dataTypeCondition.threshold).isEqualTo(145.0)
        assertThat(goal.dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(goal.period).isEqualTo(null)
    }

    @Test
    fun protoRoundTrip_milestone() {
        val proto = ExerciseGoal.createMilestone(
            DataTypeCondition(DISTANCE_TOTAL, 500.0, GREATER_THAN),
            period = 1000.0
        ).proto

        val goal = ExerciseGoal.fromProto(proto)

        assertThat(goal.exerciseGoalType).isEqualTo(MILESTONE)
        assertThat(goal.dataTypeCondition.dataType).isEqualTo(DISTANCE_TOTAL)
        assertThat(goal.dataTypeCondition.threshold).isEqualTo(500.0)
        assertThat(goal.dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(goal.period).isEqualTo(1000.0)
    }
}
