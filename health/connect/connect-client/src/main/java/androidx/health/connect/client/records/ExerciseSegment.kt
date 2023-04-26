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

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import java.time.Instant

/**
 * Represents particular exercise within an exercise session.
 *
 * <p>Each segment contains start and end time of the exercise, exercise type and optional number of
 * repetitions.
 *
 * @see ExerciseSessionRecord
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ExerciseSegment(
    public val startTime: Instant,
    public val endTime: Instant,
    @property:ExerciseSegmentTypes public val segmentType: Int,
    public val repetitions: Int = 0,
) {
    init {
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
        require(repetitions >= 0) { "repetitions can not be negative." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseSegment) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (segmentType != other.segmentType) return false
        if (repetitions != other.repetitions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + segmentType.hashCode()
        result = 31 * result + repetitions.hashCode()
        return result
    }

    internal fun isCompatibleWith(sessionType: Int): Boolean {
        if (UNIVERSAL_SESSION_TYPES.contains(sessionType)) {
            return true
        }
        if (UNIVERSAL_SEGMENTS.contains(segmentType)) {
            return true
        }
        if (!SESSION_TO_SEGMENTS_MAPPING.contains(sessionType)) {
            return false
        }
        return SESSION_TO_SEGMENTS_MAPPING[sessionType]!!.contains(segmentType)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    companion object {
        /** Next Id: 68. */
        const val EXERCISE_SEGMENT_TYPE_UNKNOWN = 0
        const val EXERCISE_SEGMENT_TYPE_ARM_CURL = 1
        const val EXERCISE_SEGMENT_TYPE_BACK_EXTENSION = 2
        const val EXERCISE_SEGMENT_TYPE_BALL_SLAM = 3
        const val EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS = 4
        const val EXERCISE_SEGMENT_TYPE_BENCH_PRESS = 5
        const val EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP = 6
        const val EXERCISE_SEGMENT_TYPE_BIKING = 7
        const val EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY = 8
        const val EXERCISE_SEGMENT_TYPE_BURPEE = 9
        const val EXERCISE_SEGMENT_TYPE_CRUNCH = 10
        const val EXERCISE_SEGMENT_TYPE_DEADLIFT = 11
        const val EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION = 12
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM = 13
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM = 14
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE = 15
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE = 16
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW = 17
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM = 18
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM = 19
        const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM = 20
        const val EXERCISE_SEGMENT_TYPE_ELLIPTICAL = 21
        const val EXERCISE_SEGMENT_TYPE_FORWARD_TWIST = 22
        const val EXERCISE_SEGMENT_TYPE_FRONT_RAISE = 23
        const val EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING = 24
        const val EXERCISE_SEGMENT_TYPE_HIP_THRUST = 25
        const val EXERCISE_SEGMENT_TYPE_HULA_HOOP = 26
        const val EXERCISE_SEGMENT_TYPE_JUMPING_JACK = 27
        const val EXERCISE_SEGMENT_TYPE_JUMP_ROPE = 28
        const val EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING = 29
        const val EXERCISE_SEGMENT_TYPE_LATERAL_RAISE = 30
        const val EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN = 31
        const val EXERCISE_SEGMENT_TYPE_LEG_CURL = 32
        const val EXERCISE_SEGMENT_TYPE_LEG_EXTENSION = 33
        const val EXERCISE_SEGMENT_TYPE_LEG_PRESS = 34
        const val EXERCISE_SEGMENT_TYPE_LEG_RAISE = 35
        const val EXERCISE_SEGMENT_TYPE_LUNGE = 36
        const val EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER = 37
        const val EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT = 38
        const val EXERCISE_SEGMENT_TYPE_PAUSE = 39
        const val EXERCISE_SEGMENT_TYPE_PILATES = 40
        const val EXERCISE_SEGMENT_TYPE_PLANK = 41
        const val EXERCISE_SEGMENT_TYPE_PULL_UP = 42
        const val EXERCISE_SEGMENT_TYPE_PUNCH = 43
        const val EXERCISE_SEGMENT_TYPE_REST = 44
        const val EXERCISE_SEGMENT_TYPE_ROWING_MACHINE = 45
        const val EXERCISE_SEGMENT_TYPE_RUNNING = 46
        const val EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL = 47
        const val EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS = 48
        const val EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION = 49
        const val EXERCISE_SEGMENT_TYPE_SIT_UP = 50
        const val EXERCISE_SEGMENT_TYPE_SQUAT = 51
        const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING = 52
        const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE = 53
        const val EXERCISE_SEGMENT_TYPE_STRETCHING = 54
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE = 55
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE = 56
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY = 57
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE = 58
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED = 59
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER = 60
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER = 61
        const val EXERCISE_SEGMENT_TYPE_SWIMMING_POOL = 62
        const val EXERCISE_SEGMENT_TYPE_UPPER_TWIST = 63
        const val EXERCISE_SEGMENT_TYPE_WALKING = 64
        const val EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING = 65
        const val EXERCISE_SEGMENT_TYPE_WHEELCHAIR = 66
        const val EXERCISE_SEGMENT_TYPE_YOGA = 67

        internal val UNIVERSAL_SESSION_TYPES = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
            ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
            ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
        )

        internal val UNIVERSAL_SEGMENTS = setOf(
            EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
            EXERCISE_SEGMENT_TYPE_PAUSE,
            EXERCISE_SEGMENT_TYPE_REST,
            EXERCISE_SEGMENT_TYPE_STRETCHING,
            EXERCISE_SEGMENT_TYPE_UNKNOWN,
        )

        internal val EXERCISE_SEGMENTS = setOf(
            EXERCISE_SEGMENT_TYPE_ARM_CURL,
            EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
            EXERCISE_SEGMENT_TYPE_BALL_SLAM,
            EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
            EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
            EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
            EXERCISE_SEGMENT_TYPE_BURPEE,
            EXERCISE_SEGMENT_TYPE_CRUNCH,
            EXERCISE_SEGMENT_TYPE_DEADLIFT,
            EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
            EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
            EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
            EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
            EXERCISE_SEGMENT_TYPE_HIP_THRUST,
            EXERCISE_SEGMENT_TYPE_HULA_HOOP,
            EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
            EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
            EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
            EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
            EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
            EXERCISE_SEGMENT_TYPE_LEG_CURL,
            EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
            EXERCISE_SEGMENT_TYPE_LEG_PRESS,
            EXERCISE_SEGMENT_TYPE_LEG_RAISE,
            EXERCISE_SEGMENT_TYPE_LUNGE,
            EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
            EXERCISE_SEGMENT_TYPE_PLANK,
            EXERCISE_SEGMENT_TYPE_PULL_UP,
            EXERCISE_SEGMENT_TYPE_PUNCH,
            EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
            EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
            EXERCISE_SEGMENT_TYPE_SIT_UP,
            EXERCISE_SEGMENT_TYPE_SQUAT,
            EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
            EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING
        )
        internal val SWIMMING_SEGMENTS = setOf(
            EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
            EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
            EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
            EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
            EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
            EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER
        )

        private val SESSION_TO_SEGMENTS_MAPPING = mapOf(
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING to setOf(EXERCISE_SEGMENT_TYPE_BIKING),
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to setOf(
                EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to EXERCISE_SEGMENTS,
            ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL
                to setOf(EXERCISE_SEGMENT_TYPE_ELLIPTICAL),
            ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to setOf(
                EXERCISE_SEGMENT_TYPE_YOGA,
                EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                EXERCISE_SEGMENT_TYPE_PILATES,
                EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to EXERCISE_SEGMENTS,
            ExerciseSessionRecord.EXERCISE_TYPE_HIKING to setOf(
                EXERCISE_SEGMENT_TYPE_WALKING,
                EXERCISE_SEGMENT_TYPE_WHEELCHAIR
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_PILATES to setOf(EXERCISE_SEGMENT_TYPE_PILATES),
            ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to setOf(
                EXERCISE_SEGMENT_TYPE_ROWING_MACHINE
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to setOf(
                EXERCISE_SEGMENT_TYPE_RUNNING,
                EXERCISE_SEGMENT_TYPE_WALKING
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to setOf(
                EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to EXERCISE_SEGMENTS,
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to setOf(
                EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to setOf(
                EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE
            ),
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to buildSet {
                add(EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER)
                addAll(SWIMMING_SEGMENTS)
            },
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to buildSet {
                add(EXERCISE_SEGMENT_TYPE_SWIMMING_POOL)
                addAll(SWIMMING_SEGMENTS)
            },
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING to setOf(EXERCISE_SEGMENT_TYPE_WALKING),
            ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
                to setOf(EXERCISE_SEGMENT_TYPE_WHEELCHAIR),
            ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to EXERCISE_SEGMENTS,
            ExerciseSessionRecord.EXERCISE_TYPE_YOGA to setOf(EXERCISE_SEGMENT_TYPE_YOGA),
        )

        /**
         * List of supported segment types on Health Platform.
         *
         * @suppress
         */
        @Retention(AnnotationRetention.SOURCE)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @IntDef(
            value =
            [
                EXERCISE_SEGMENT_TYPE_UNKNOWN,
                EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS,
                EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP,
                EXERCISE_SEGMENT_TYPE_BIKING,
                EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                EXERCISE_SEGMENT_TYPE_FORWARD_TWIST,
                EXERCISE_SEGMENT_TYPE_ELLIPTICAL,
                EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                EXERCISE_SEGMENT_TYPE_PILATES,
                EXERCISE_SEGMENT_TYPE_ROWING_MACHINE,
                EXERCISE_SEGMENT_TYPE_RUNNING,
                EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL,
                EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING,
                EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE,
                EXERCISE_SEGMENT_TYPE_STRETCHING,
                EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER,
                EXERCISE_SEGMENT_TYPE_SWIMMING_POOL,
                EXERCISE_SEGMENT_TYPE_UPPER_TWIST,
                EXERCISE_SEGMENT_TYPE_WALKING,
                EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                EXERCISE_SEGMENT_TYPE_WHEELCHAIR,
                EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                EXERCISE_SEGMENT_TYPE_YOGA,
                EXERCISE_SEGMENT_TYPE_ARM_CURL,
                EXERCISE_SEGMENT_TYPE_BACK_EXTENSION,
                EXERCISE_SEGMENT_TYPE_BALL_SLAM,
                EXERCISE_SEGMENT_TYPE_BENCH_PRESS,
                EXERCISE_SEGMENT_TYPE_BURPEE,
                EXERCISE_SEGMENT_TYPE_CRUNCH,
                EXERCISE_SEGMENT_TYPE_DEADLIFT,
                EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW,
                EXERCISE_SEGMENT_TYPE_FRONT_RAISE,
                EXERCISE_SEGMENT_TYPE_HIP_THRUST,
                EXERCISE_SEGMENT_TYPE_HULA_HOOP,
                EXERCISE_SEGMENT_TYPE_JUMPING_JACK,
                EXERCISE_SEGMENT_TYPE_JUMP_ROPE,
                EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING,
                EXERCISE_SEGMENT_TYPE_LATERAL_RAISE,
                EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN,
                EXERCISE_SEGMENT_TYPE_LEG_CURL,
                EXERCISE_SEGMENT_TYPE_LEG_EXTENSION,
                EXERCISE_SEGMENT_TYPE_LEG_PRESS,
                EXERCISE_SEGMENT_TYPE_LEG_RAISE,
                EXERCISE_SEGMENT_TYPE_LUNGE,
                EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER,
                EXERCISE_SEGMENT_TYPE_PLANK,
                EXERCISE_SEGMENT_TYPE_PULL_UP,
                EXERCISE_SEGMENT_TYPE_PUNCH,
                EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS,
                EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                EXERCISE_SEGMENT_TYPE_SIT_UP,
                EXERCISE_SEGMENT_TYPE_SQUAT,
                EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
                EXERCISE_SEGMENT_TYPE_REST,
                EXERCISE_SEGMENT_TYPE_PAUSE,
            ]
        )
        annotation class ExerciseSegmentTypes
    }
}