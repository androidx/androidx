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

import androidx.health.connect.client.units.kilometers
import androidx.health.connect.client.units.meters
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannedExerciseBlockTest {

    @Test
    fun identicalBlocks_bothAreEqual() {
        assertThat(
                PlannedExerciseBlock(
                    repetitions = 2,
                    description = "Main set",
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                completionGoal = ExerciseCompletionGoal.DistanceGoal(3.kilometers),
                                performanceTargets = listOf()
                            )
                        )
                )
            )
            .isEqualTo(
                PlannedExerciseBlock(
                    repetitions = 2,
                    description = "Main set",
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                completionGoal = ExerciseCompletionGoal.DistanceGoal(3.kilometers),
                                performanceTargets = listOf()
                            )
                        )
                )
            )
    }

    @Test
    fun differentBlocks_notEqual() {
        assertThat(
                PlannedExerciseBlock(
                    repetitions = 2,
                    description = "Main set",
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                completionGoal = ExerciseCompletionGoal.DistanceGoal(1.kilometers),
                                performanceTargets = listOf()
                            )
                        )
                )
            )
            .isNotEqualTo(
                PlannedExerciseBlock(
                    repetitions = 3,
                    description = "Warmup",
                    steps =
                        listOf(
                            PlannedExerciseStep(
                                ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                completionGoal = ExerciseCompletionGoal.DistanceGoal(200.meters),
                                performanceTargets = listOf()
                            )
                        )
                )
            )
    }
}
