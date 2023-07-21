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
import androidx.health.services.client.data.DataType.Companion.HEART_RATE_BPM
import androidx.health.services.client.data.DataType.Companion.LOCATION
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseConfigTest {
    @Test
    fun protoRoundTrip() {
        val proto = ExerciseConfig(
            ExerciseType.RUNNING,
            setOf(LOCATION, DISTANCE_TOTAL, HEART_RATE_BPM),
            isAutoPauseAndResumeEnabled = true,
            isGpsEnabled = true,
            exerciseGoals = listOf(
                ExerciseGoal.createOneTimeGoal(
                    DataTypeCondition(DISTANCE_TOTAL, 50.0, GREATER_THAN)
                ),
                ExerciseGoal.createOneTimeGoal(
                    DataTypeCondition(DISTANCE_TOTAL, 150.0, GREATER_THAN)
                ),
            ),
            exerciseTypeConfig = GolfExerciseTypeConfig(
                GolfExerciseTypeConfig
                    .GolfShotTrackingPlaceInfo.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY
            ),
            batchingModeOverrides = setOf(BatchingMode.HEART_RATE_5_SECONDS),
            exerciseEventTypes = setOf(ExerciseEventType.GOLF_SHOT_EVENT),
        ).toProto()

        val config = ExerciseConfig(proto)

        assertThat(config.exerciseType).isEqualTo(ExerciseType.RUNNING)
        assertThat(config.dataTypes).containsExactly(LOCATION, HEART_RATE_BPM, DISTANCE_TOTAL)
        assertThat(config.isAutoPauseAndResumeEnabled).isEqualTo(true)
        assertThat(config.isGpsEnabled).isEqualTo(true)
        assertThat(config.exerciseGoals[0].dataTypeCondition.dataType).isEqualTo(DISTANCE_TOTAL)
        assertThat(config.exerciseGoals[0].dataTypeCondition.threshold).isEqualTo(50.0)
        assertThat(config.exerciseGoals[0].dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(config.exerciseGoals[1].dataTypeCondition.dataType).isEqualTo(DISTANCE_TOTAL)
        assertThat(config.exerciseGoals[1].dataTypeCondition.threshold).isEqualTo(150.0)
        assertThat(config.exerciseGoals[1].dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(config.exerciseTypeConfig!!).isInstanceOf(GolfExerciseTypeConfig::class.java)
        assertThat(
            (config.exerciseTypeConfig!! as GolfExerciseTypeConfig)
            .golfShotTrackingPlaceInfo
        ).isEqualTo(GolfExerciseTypeConfig
            .GolfShotTrackingPlaceInfo.GOLF_SHOT_TRACKING_PLACE_INFO_FAIRWAY)
        assertThat(config.batchingModeOverrides).containsExactly(BatchingMode.HEART_RATE_5_SECONDS)
        assertThat(config.exerciseEventTypes).containsExactly(ExerciseEventType.GOLF_SHOT_EVENT)
    }

    @Test
    fun protoRoundTrip_emptyExerciseTypeConfigAndBatchingModes() {
        val proto = ExerciseConfig(
            ExerciseType.RUNNING,
            setOf(LOCATION, DISTANCE_TOTAL, HEART_RATE_BPM),
            isAutoPauseAndResumeEnabled = true,
            isGpsEnabled = true,
            exerciseGoals = listOf(
                ExerciseGoal.createOneTimeGoal(
                    DataTypeCondition(DISTANCE_TOTAL, 50.0, GREATER_THAN)
                ),
                ExerciseGoal.createOneTimeGoal(
                    DataTypeCondition(DISTANCE_TOTAL, 150.0, GREATER_THAN)
                ),
            )
        ).toProto()

        val config = ExerciseConfig(proto)

        assertThat(config.exerciseType).isEqualTo(ExerciseType.RUNNING)
        assertThat(config.dataTypes).containsExactly(LOCATION, HEART_RATE_BPM, DISTANCE_TOTAL)
        assertThat(config.isAutoPauseAndResumeEnabled).isEqualTo(true)
        assertThat(config.isGpsEnabled).isEqualTo(true)
        assertThat(config.exerciseGoals[0].dataTypeCondition.dataType).isEqualTo(DISTANCE_TOTAL)
        assertThat(config.exerciseGoals[0].dataTypeCondition.threshold).isEqualTo(50.0)
        assertThat(config.exerciseGoals[0].dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat(config.exerciseGoals[1].dataTypeCondition.dataType).isEqualTo(DISTANCE_TOTAL)
        assertThat(config.exerciseGoals[1].dataTypeCondition.threshold).isEqualTo(150.0)
        assertThat(config.exerciseGoals[1].dataTypeCondition.comparisonType).isEqualTo(GREATER_THAN)
        assertThat((config.exerciseTypeConfig)).isNull()
    }

    @Test
    fun builder_exerciseTypeConfigNull() {
        val exerciseTypeConfigNotSetExerciseConfig =
            ExerciseConfig.builder(ExerciseType.UNKNOWN).build()
        val setNullExerciseTypeConfigExerciseConfig =
            ExerciseConfig.builder(ExerciseType.UNKNOWN).setExerciseTypeConfig(null).build()

        assertThat(exerciseTypeConfigNotSetExerciseConfig.exerciseTypeConfig).isNull()
        assertThat(setNullExerciseTypeConfigExerciseConfig.exerciseTypeConfig).isNull()
    }

    @Test
    fun throwsWhenLocationIsRequestedButGpsNotEnabled() {
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseConfig(
                ExerciseType.RUNNING,
                setOf(LOCATION),
                isAutoPauseAndResumeEnabled = true,
                isGpsEnabled = false,
            )
        }
    }
}
