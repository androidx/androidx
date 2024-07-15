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

package androidx.health.connect.client.records

import androidx.health.connect.client.records.ExerciseCompletionGoal.ActiveCaloriesBurnedGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.DistanceAndDurationGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.DistanceGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.DurationGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.ManualCompletion
import androidx.health.connect.client.records.ExerciseCompletionGoal.RepetitionsGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.StepsGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.TotalCaloriesBurnedGoal
import androidx.health.connect.client.records.ExerciseCompletionGoal.UnknownGoal
import androidx.health.connect.client.units.calories
import androidx.health.connect.client.units.meters
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseCompletionGoalTest {
    @Test
    fun distanceGoal_hashCodeAndEquals() {
        val distanceGoal1 = DistanceGoal(20.0.meters)
        val distanceGoal1Duplicate = DistanceGoal(20.0.meters)
        val distanceGoal2 = DistanceGoal(30.0.meters)

        assertThat(distanceGoal1.hashCode()).isNotEqualTo(distanceGoal2.hashCode())
        assertThat(distanceGoal1).isNotEqualTo(distanceGoal2)
        assertThat(distanceGoal1.hashCode()).isEqualTo(distanceGoal1Duplicate.hashCode())
        assertThat(distanceGoal1).isEqualTo(distanceGoal1Duplicate)
    }

    @Test
    fun distanceWithVariableRestGoal_hashCodeAndEquals() {
        val distanceAndDurationGoal1 = DistanceAndDurationGoal(20.0.meters, Duration.ofMinutes(2))
        val distanceAndDurationGoal1Duplicate =
            DistanceAndDurationGoal(20.0.meters, Duration.ofMinutes(2))
        val distanceAndDurationGoal2 = DistanceAndDurationGoal(20.0.meters, Duration.ofMinutes(4))

        assertThat(distanceAndDurationGoal1.hashCode())
            .isNotEqualTo(distanceAndDurationGoal2.hashCode())
        assertThat(distanceAndDurationGoal1).isNotEqualTo(distanceAndDurationGoal2)
        assertThat(distanceAndDurationGoal1.hashCode())
            .isEqualTo(distanceAndDurationGoal1Duplicate.hashCode())
        assertThat(distanceAndDurationGoal1).isEqualTo(distanceAndDurationGoal1Duplicate)
    }

    @Test
    fun stepsGoal_hashCodeAndEquals() {
        val stepsGoal1 = StepsGoal(1000)
        val stepsGoal1Duplicate = StepsGoal(1000)
        val stepsGoal2 = StepsGoal(2000)

        assertThat(stepsGoal1.hashCode()).isNotEqualTo(stepsGoal2.hashCode())
        assertThat(stepsGoal1).isNotEqualTo(stepsGoal2)
        assertThat(stepsGoal1.hashCode()).isEqualTo(stepsGoal1Duplicate.hashCode())
        assertThat(stepsGoal1).isEqualTo(stepsGoal1Duplicate)
    }

    @Test
    fun durationGoal_hashCodeAndEquals() {
        val durationGoal1 = DurationGoal(Duration.ofMinutes(30))
        val durationGoal1Duplicate = DurationGoal(Duration.ofMinutes(30))
        val durationGoal2 = DurationGoal(Duration.ofMinutes(60))

        assertThat(durationGoal1.hashCode()).isNotEqualTo(durationGoal2.hashCode())
        assertThat(durationGoal1).isNotEqualTo(durationGoal2)
        assertThat(durationGoal1.hashCode()).isEqualTo(durationGoal1Duplicate.hashCode())
        assertThat(durationGoal1).isEqualTo(durationGoal1Duplicate)
    }

    @Test
    fun repetitionsGoal_hashCodeAndEquals() {
        val repetitionsGoal1 = RepetitionsGoal(Duration.ofSeconds(10))
        val repetitionsGoal1Duplicate = RepetitionsGoal(Duration.ofSeconds(10))
        val repetitionsGoal2 = RepetitionsGoal(Duration.ofSeconds(20))

        assertThat(repetitionsGoal1.hashCode()).isNotEqualTo(repetitionsGoal2.hashCode())
        assertThat(repetitionsGoal1).isNotEqualTo(repetitionsGoal2)
        assertThat(repetitionsGoal1.hashCode()).isEqualTo(repetitionsGoal1Duplicate.hashCode())
        assertThat(repetitionsGoal1).isEqualTo(repetitionsGoal1Duplicate)
    }

    @Test
    fun totalCaloriesBurnedGoal_hashCodeAndEquals() {
        val totalCaloriesBurnedGoal1 = TotalCaloriesBurnedGoal(100.calories)
        val totalCaloriesBurnedGoal1Duplicate = TotalCaloriesBurnedGoal(100.calories)
        val totalCaloriesBurnedGoal2 = TotalCaloriesBurnedGoal(200.calories)

        assertThat(totalCaloriesBurnedGoal1.hashCode())
            .isNotEqualTo(totalCaloriesBurnedGoal2.hashCode())
        assertThat(totalCaloriesBurnedGoal1).isNotEqualTo(totalCaloriesBurnedGoal2)
        assertThat(totalCaloriesBurnedGoal1.hashCode())
            .isEqualTo(totalCaloriesBurnedGoal1Duplicate.hashCode())
        assertThat(totalCaloriesBurnedGoal1).isEqualTo(totalCaloriesBurnedGoal1Duplicate)
    }

    @Test
    fun activeCaloriesBurnedGoal_hashCodeAndEquals() {
        val activeCaloriesBurnedGoal1 = ActiveCaloriesBurnedGoal(100.calories)
        val activeCaloriesBurnedGoal1Duplicate = ActiveCaloriesBurnedGoal(100.calories)
        val activeCaloriesBurnedGoal2 = ActiveCaloriesBurnedGoal(200.calories)

        assertThat(activeCaloriesBurnedGoal1.hashCode())
            .isNotEqualTo(activeCaloriesBurnedGoal2.hashCode())
        assertThat(activeCaloriesBurnedGoal1).isNotEqualTo(activeCaloriesBurnedGoal2)
        assertThat(activeCaloriesBurnedGoal1.hashCode())
            .isEqualTo(activeCaloriesBurnedGoal1Duplicate.hashCode())
        assertThat(activeCaloriesBurnedGoal1).isEqualTo(activeCaloriesBurnedGoal1Duplicate)
    }

    @Test
    fun unknownGoal_hashCodeAndEquals() {
        assertThat(UnknownGoal.hashCode()).isEqualTo(UnknownGoal.hashCode())
        assertThat(UnknownGoal).isEqualTo(UnknownGoal)
    }

    @Test
    fun unspecifiedGoal_hashCodeAndEquals() {
        assertThat(ManualCompletion).isEqualTo(ManualCompletion)
        assertThat(ManualCompletion).isEqualTo(ManualCompletion)
    }
}
