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

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseCapabilitiesTest {
    @Test
    fun return_supportedDataTypesForSpecifiedExercise() {
        assertThat(
            EXERCISE_CAPABILITIES.getExerciseTypeCapabilities(
                ExerciseType.WALKING
            ).supportedDataTypes
        ).containsExactly(
            DataType.STEPS
        )
    }

    @Test
    fun supportedGoalsForExercise() {
        assertThat(
            EXERCISE_CAPABILITIES.getExerciseTypeCapabilities(
                ExerciseType.RUNNING
            ).supportedGoals
        ).isEqualTo(
            EXERCISE_CAPABILITIES.typeToCapabilities.get(ExerciseType.RUNNING)!!.supportedGoals
        )
    }

    @Test
    fun supportedMilestonesForExercise() {
        assertThat(
            EXERCISE_CAPABILITIES.getExerciseTypeCapabilities(
                ExerciseType.RUNNING).supportedMilestones
        ).isEqualTo(
            EXERCISE_CAPABILITIES.typeToCapabilities.get(ExerciseType.RUNNING)!!.supportedMilestones
        )
    }

    @Test
    fun supportedExerciseEventForGolfExercise() {
        assertThat(
            EXERCISE_CAPABILITIES.getExerciseTypeCapabilities(
                ExerciseType.GOLF).supportedExerciseEvents
        ).isEqualTo(setOf(ExerciseEventType.GOLF_SHOT_EVENT))
        assertThat(
            EXERCISE_CAPABILITIES.getExerciseTypeCapabilities(
                ExerciseType.GOLF).supportedExerciseEvents
        ).isEqualTo(
            EXERCISE_CAPABILITIES.typeToCapabilities.get(ExerciseType.GOLF)?.supportedExerciseEvents
        )
        assertThat(EXERCISE_CAPABILITIES.typeToCapabilities[ExerciseType.GOLF]
                        ?.getExerciseEventCapabilityDetails(ExerciseEventType.GOLF_SHOT_EVENT)
                        ?.isSwingTypeClassificationSupported).isTrue()
    }

    @Test
    fun exercisesSupportingAutoResumeAndPause_returnCorrectSet() {
        val supportsAutoPauseAndResume = ExerciseTypeCapabilities(
            supportedDataTypes = ImmutableSet.of(),
            supportedGoals = ImmutableMap.of(),
            supportedMilestones = ImmutableMap.of(),
            supportsAutoPauseAndResume = true,
        )
        val doesNotSupportAutoPauseAndResume = ExerciseTypeCapabilities(
            supportedDataTypes = ImmutableSet.of(),
            supportedGoals = ImmutableMap.of(),
            supportedMilestones = ImmutableMap.of(),
            supportsAutoPauseAndResume = false,
        )
        val exerciseCapabilities = ExerciseCapabilities(
            ImmutableMap.of(
                ExerciseType.WALKING, supportsAutoPauseAndResume,
                ExerciseType.RUNNING, doesNotSupportAutoPauseAndResume
            )
        )

        assertThat(exerciseCapabilities.autoPauseAndResumeEnabledExercises)
            .containsExactly(ExerciseType.WALKING)
    }

    @Test
    fun parcelable_roundTrip_returnsOriginalCapabilities() {
        val proto = EXERCISE_CAPABILITIES.proto
        val capabilities = ExerciseCapabilities(proto)

        assertThat(capabilities.supportedExerciseTypes).containsExactlyElementsIn(
            EXERCISE_CAPABILITIES.supportedExerciseTypes
        )
        assertThat(capabilities.autoPauseAndResumeEnabledExercises).containsExactlyElementsIn(
            EXERCISE_CAPABILITIES.autoPauseAndResumeEnabledExercises
        )
        assertThat(capabilities.supportedBatchingModeOverrides).containsExactlyElementsIn(
            EXERCISE_CAPABILITIES.supportedBatchingModeOverrides
        )
    }

    @Test
    fun parcelable_roundTrip_returnsEmptyOriginalCapabilities() {
        val emptyCapabilities = ExerciseCapabilities(ImmutableMap.of())
        val roundTripEmptyCapabilities = ExerciseCapabilities(emptyCapabilities.proto)

        assertThat(emptyCapabilities.supportedExerciseTypes).containsExactlyElementsIn(
            roundTripEmptyCapabilities.supportedExerciseTypes
        )
        assertThat(emptyCapabilities.autoPauseAndResumeEnabledExercises).containsExactlyElementsIn(
            roundTripEmptyCapabilities.autoPauseAndResumeEnabledExercises
        )
        assertThat(emptyCapabilities.supportedBatchingModeOverrides).containsExactlyElementsIn(
            roundTripEmptyCapabilities.supportedBatchingModeOverrides
        )
    }

    companion object {
        private val WALKING_CAPABILITIES = ExerciseTypeCapabilities(
            supportedDataTypes = ImmutableSet.of(DataType.STEPS),
            supportedGoals = ImmutableMap.of(
                DataType.STEPS_TOTAL,
                ImmutableSet.of(ComparisonType.GREATER_THAN)
            ),
            supportedMilestones = ImmutableMap.of(
                DataType.STEPS_TOTAL,
                ImmutableSet.of(ComparisonType.LESS_THAN, ComparisonType.GREATER_THAN)
            ),
            supportsAutoPauseAndResume = false,
        )
        private val RUNNING_CAPABILITIES = ExerciseTypeCapabilities(
            supportedDataTypes = ImmutableSet.of(DataType.HEART_RATE_BPM, DataType.SPEED),
            supportedGoals = ImmutableMap.of(
                DataType.HEART_RATE_BPM_STATS,
                ImmutableSet.of(ComparisonType.GREATER_THAN, ComparisonType.LESS_THAN),
                DataType.SPEED_STATS,
                ImmutableSet.of(ComparisonType.LESS_THAN)
            ),
            supportedMilestones = ImmutableMap.of(
                DataType.HEART_RATE_BPM_STATS,
                ImmutableSet.of(ComparisonType.GREATER_THAN_OR_EQUAL),
                DataType.SPEED_STATS,
                ImmutableSet.of(ComparisonType.LESS_THAN, ComparisonType.GREATER_THAN)
            ),
            supportsAutoPauseAndResume = true,
        )

        private val SWIMMING_CAPABILITIES = ExerciseTypeCapabilities(
            supportedDataTypes = emptySet(),
            supportedGoals = emptyMap(),
            supportedMilestones = emptyMap(),
            supportsAutoPauseAndResume = true,
        )

        private val GOLF_SHOT_EVENT_CAPABILITIES: GolfShotEventCapabilities =
            GolfShotEventCapabilities(isSupported = true, isSwingTypeClassificationSupported = true)

        private val GOLF_CAPABILITIES = ExerciseTypeCapabilities(
            supportedDataTypes = emptySet(),
            supportedGoals = emptyMap(),
            supportedMilestones = emptyMap(),
            supportsAutoPauseAndResume = true,
            exerciseEventCapabilities =
            ImmutableMap.of(ExerciseEventType.GOLF_SHOT_EVENT, GOLF_SHOT_EVENT_CAPABILITIES),
        )

        private val EXERCISE_TYPE_TO_EXERCISE_CAPABILITIES_MAPPING =
            ImmutableMap.of(
                ExerciseType.WALKING, WALKING_CAPABILITIES,
                ExerciseType.RUNNING, RUNNING_CAPABILITIES,
                ExerciseType.SWIMMING_POOL, SWIMMING_CAPABILITIES,
                ExerciseType.GOLF, GOLF_CAPABILITIES,
            )

        private val EXERCISE_CAPABILITIES: ExerciseCapabilities =
            ExerciseCapabilities(EXERCISE_TYPE_TO_EXERCISE_CAPABILITIES_MAPPING,
                ImmutableSet.of(BatchingMode.HEART_RATE_5_SECONDS))
    }
}
