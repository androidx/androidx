/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.health.connect.client.records.ExerciseSegment.Companion.EXERCISE_SEGMENTS
import androidx.health.connect.client.records.ExerciseSegment.Companion.SWIMMING_SEGMENTS
import androidx.health.connect.client.records.ExerciseSegment.Companion.UNIVERSAL_SEGMENTS
import androidx.health.connect.client.records.ExerciseSegment.Companion.UNIVERSAL_SESSION_TYPES
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseSegmentTest {
    private val allSegmentTypes =
        ExerciseSegment.Companion::class
            .members
            .asSequence()
            .filter { it.name.startsWith("EXERCISE_SEGMENT_TYPE_") }
            .filter { it.returnType == typeOf<Int>() }
            .map { it.call(ExerciseSegment.Companion) as Int }
            .toSet()

    private val allSessionTypes =
        ExerciseSessionRecord.Companion::class
            .members
            .asSequence()
            .filter { it.name.startsWith("EXERCISE_TYPE_") }
            .filter { it.returnType == typeOf<Int>() }
            .map { it.call(ExerciseSegment.Companion) as Int }
            .toSet()

    private val exerciseSessionTypes = setOf(
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS,
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
    )

    private val swimmingSessionTypes = setOf(
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
    )

    private val hikingSegments = setOf(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR
    )

    private val runningSegments = setOf(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING
    )

    private val exerciseClassSegments = setOf(
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES,
        ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA,
    )

    private val sameSessionAndSegmentTypePairs = mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_STRETCHING,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
            to ExerciseSegment.EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA to ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA,
    )

    @Test
    fun validSegment_equals() {
        assertThat(
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = 10,
            )
        ).isEqualTo(
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = 10,
            )
        )
    }

    @Test
    fun invalidTimes_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(1234L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(5678L),
                endTime = Instant.ofEpochMilli(1234L),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
            )
        }
    }

    @Test
    fun invalidReps_throws() {
        assertFailsWith<IllegalArgumentException> {
            ExerciseSegment(
                startTime = Instant.ofEpochMilli(1234L),
                endTime = Instant.ofEpochMilli(5678),
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                repetitions = -1,
            )
        }
    }

    @Test
    fun isCompatible_universalSession_acceptsEverything() {
        UNIVERSAL_SESSION_TYPES.forEach { sessionType ->
            allSegmentTypes.forEach { segmentType ->
                assertCompatibility(sessionType, segmentType)
            }
        }
    }

    @Test
    fun isCompatible_universalSegment_fitsInEverything() {
        allSessionTypes.forEach { sessionType ->
            UNIVERSAL_SEGMENTS.forEach { segmentType ->
                assertCompatibility(sessionType, segmentType)
            }
        }
    }

    @Test
    fun isCompatible_sameSessionAndSegmentType_returnsTrue() {
        sameSessionAndSegmentTypePairs.forEach { (sessionType, segmentType) ->
            assertCompatibility(sessionType, segmentType)
        }
    }

    @Test
    fun isCompatible_genericExerciseSessions_acceptGenericExerciseSegments() {
        exerciseSessionTypes.forEach { sessionType ->
            EXERCISE_SEGMENTS.forEach { segmentType ->
                assertCompatibility(sessionType, segmentType)
            }
        }
    }

    @Test
    fun isCompatible_swimmingSessions_acceptSwimmingSegments() {
        swimmingSessionTypes.forEach { sessionType ->
            SWIMMING_SEGMENTS.forEach { segmentType ->
                assertCompatibility(sessionType, segmentType)
            }
        }
    }

    @Test
    fun isCompatible_exerciseClassSession_acceptClassSegments() {
        assertCompatibility(
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_YOGA
        )
        assertCompatibility(
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
        )
        assertCompatibility(
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_PILATES
        )
        assertCompatibility(
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS,
            ExerciseSegment.EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
        )
    }

    @Test
    fun isCompatible_hikingSession_acceptWalkingAndWheelchair() {
        hikingSegments.forEach { segmentType ->
            assertCompatibility(ExerciseSessionRecord.EXERCISE_TYPE_HIKING, segmentType)
        }
    }

    @Test
    fun isCompatible_runningSession_acceptRunningAndWalking() {
        runningSegments.forEach { segmentType ->
            assertCompatibility(ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, segmentType)
        }
    }

    @Test
    fun isCompatible_allOtherCombinations_returnsFalse() {
        allSessionTypes.filter { !UNIVERSAL_SESSION_TYPES.contains(it) }.forEach { sessionType ->
            allSegmentTypes.asSequence().filter { !UNIVERSAL_SEGMENTS.contains(it) }
                .filter { !(sameSessionAndSegmentTypePairs[sessionType]?.equals(it) ?: false) }
                .filter {
                    !(exerciseSessionTypes.contains(sessionType) && EXERCISE_SEGMENTS.contains(it))
                }
                .filter {
                    !(swimmingSessionTypes.contains(sessionType) && SWIMMING_SEGMENTS.contains(it))
                }
                .filter {
                    !(sessionType == ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS &&
                        exerciseClassSegments.contains(it))
                }
                .filter {
                    !(sessionType == ExerciseSessionRecord.EXERCISE_TYPE_HIKING &&
                        hikingSegments.contains(it))
                }
                .filter {
                    !(sessionType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING &&
                        runningSegments.contains(it))
                }.toList()
                .forEach { segmentType -> assertCompatibility(sessionType, segmentType, false) }
        }
    }

    private fun assertCompatibility(
        sessionType: Int,
        segmentType: Int,
        isCompatible: Boolean = true
    ) {
        assertEquals(
            expected = isCompatible,
            actual = ExerciseSegment(
                startTime = Instant.ofEpochMilli(1),
                endTime = Instant.ofEpochMilli(2),
                segmentType = segmentType
            ).isCompatibleWith(sessionType),
            message = "$sessionType and $segmentType is not compatible"
        )
    }
}