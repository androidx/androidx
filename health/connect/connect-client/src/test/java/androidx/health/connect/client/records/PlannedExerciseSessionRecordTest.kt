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

package androidx.health.connect.client.records

import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Power
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannedExerciseSessionRecordTest {

    @Test
    fun identicalRecords_bothAreEqual() {
        assertThat(
                PlannedExerciseSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(180.0),
                                                        maxPower = Power.watts(220.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
            .isEqualTo(
                PlannedExerciseSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(180.0),
                                                        maxPower = Power.watts(220.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
    }

    @Test
    fun differentRecords_notEqual() {
        assertThat(
                PlannedExerciseSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                2,
                                description = "Main set",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                            PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(3000.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(200.0),
                                                        maxPower = Power.watts(240.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
            .isNotEqualTo(
                PlannedExerciseSessionRecord(
                    startTime = Instant.ofEpochMilli(1234L),
                    startZoneOffset = null,
                    endTime = Instant.ofEpochMilli(1236L),
                    endZoneOffset = null,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets = listOf()
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
    }

    @Test
    fun invalidTimes_startAfterEnd_throws() {
        assertFailsWith<IllegalArgumentException> {
            PlannedExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(100L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(50L),
                endZoneOffset = null,
                blocks =
                    listOf(
                        PlannedExerciseBlock(
                            3,
                            description = "Warmup",
                            steps =
                                listOf(
                                    PlannedExerciseStep(
                                        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                        PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                        completionGoal =
                                            ExerciseCompletionGoal.DistanceGoal(
                                                Length.meters(200.0)
                                            ),
                                        performanceTargets =
                                            listOf(
                                                ExercisePerformanceTarget.PowerTarget(
                                                    minPower = Power.watts(180.0),
                                                    maxPower = Power.watts(220.0)
                                                )
                                            )
                                    )
                                )
                        )
                    ),
                title = "Total Body Conditioning Workout",
                notes = "A tough workout that mixes both cardio and strength!",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            )
        }
    }

    @Test
    fun identicalRecords_localDateConstructor_bothAreEqual() {
        val startDate = LocalDate.of(2022, 12, 31)
        val duration = Duration.ofHours(1)

        assertThat(
                PlannedExerciseSessionRecord(
                    startDate = startDate,
                    duration = duration,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(180.0),
                                                        maxPower = Power.watts(220.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
            .isEqualTo(
                PlannedExerciseSessionRecord(
                    startDate = startDate,
                    duration = duration,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(180.0),
                                                        maxPower = Power.watts(220.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
    }

    @Test
    fun differentRecords_localDateConstructor_notEqual() {
        val startDate = LocalDate.of(2022, 12, 31)
        val duration = Duration.ofHours(1)

        assertThat(
                PlannedExerciseSessionRecord(
                    startDate = startDate,
                    duration = duration,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                2,
                                description = "Main set",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
                                            PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(3000.0)
                                                ),
                                            performanceTargets =
                                                listOf(
                                                    ExercisePerformanceTarget.PowerTarget(
                                                        minPower = Power.watts(200.0),
                                                        maxPower = Power.watts(240.0)
                                                    )
                                                )
                                        )
                                    )
                            )
                        ),
                    title = "",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
            .isNotEqualTo(
                PlannedExerciseSessionRecord(
                    startDate = startDate,
                    duration = duration,
                    blocks =
                        listOf(
                            PlannedExerciseBlock(
                                3,
                                description = "Warmup",
                                steps =
                                    listOf(
                                        PlannedExerciseStep(
                                            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                                            PlannedExerciseStep.EXERCISE_PHASE_WARMUP,
                                            completionGoal =
                                                ExerciseCompletionGoal.DistanceGoal(
                                                    Length.meters(200.0)
                                                ),
                                            performanceTargets = listOf()
                                        )
                                    )
                            )
                        ),
                    title = "Total Body Conditioning Workout",
                    notes = "A tough workout that mixes both cardio and strength!",
                    exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                )
            )
    }

    @Test
    fun completedExerciseSessionId_setsCorrectly() {
        // Note: this can only be set via the internal constructor.
        val record =
            PlannedExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                hasExplicitTime = true,
                blocks = listOf(),
                title = "My Planned Session",
                notes = "Notes",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
                completedExerciseSessionId = "some-uuid",
                metadata = Metadata("record_id", DataOrigin("com.some.app"))
            )
        assertThat(record.completedExerciseSessionId).isEqualTo("some-uuid")
    }

    @Test
    fun completedExerciseSessionId_defaultsToNull() {
        val record =
            PlannedExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(1234L),
                startZoneOffset = null,
                endTime = Instant.ofEpochMilli(1236L),
                endZoneOffset = null,
                blocks = listOf(),
                title = "My Planned Session",
                notes = "Notes",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS
            )
        assertThat(record.completedExerciseSessionId).isNull()
    }

    @Test
    fun localDateConstructor_implicitlySetsStartAndEndTime() {
        val startDate = LocalDate.of(2023, 10, 26)
        val record =
            PlannedExerciseSessionRecord(
                startDate = startDate,
                duration = Duration.ofHours(1),
                blocks = listOf(),
                title = "My Planned Session",
                notes = "Notes",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            )

        assertThat(record.startTime)
            .isEqualTo(startDate.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant())
        assertThat(record.endTime)
            .isEqualTo(
                startDate
                    .atTime(LocalTime.NOON)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .plus(Duration.ofHours(1))
            )
    }

    @Test
    fun localDateConstructor_hasExplicitTimeIsFalse() {
        val record =
            PlannedExerciseSessionRecord(
                startDate = LocalDate.now(),
                duration = Duration.ofMinutes(30),
                blocks = listOf(),
                title = "My Planned Session",
                notes = "Notes",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            )
        assertThat(record.hasExplicitTime).isFalse()
    }

    @Test
    fun instantConstructor_hasExplicitTimeIsTrue() {
        val record =
            PlannedExerciseSessionRecord(
                startTime = Instant.now(),
                startZoneOffset = null,
                endTime = Instant.now().plusSeconds(1800),
                endZoneOffset = null,
                blocks = listOf(),
                title = "My Planned Session",
                notes = "Notes",
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            )
        assertThat(record.hasExplicitTime).isTrue()
    }
}
