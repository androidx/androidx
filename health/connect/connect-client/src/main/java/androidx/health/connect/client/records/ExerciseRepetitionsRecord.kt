/*
 * Copyright (C) 2022 The Android Open Source Project
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
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the number of repetitions in an exercise set. */
public class ExerciseRepetitionsRecord(
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    /** Count. Required field. Valid range: 1-1000000. */
    public val count: Long,
    /** Type of exercise being repeated. Required field. */
    @property:RepetitionTypes public val type: Int,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    init {
        requireNonNegative(value = count, name = "count")
        count.requireNotMore(other = 1000_000, name = "count")
        require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseRepetitionsRecord) return false

        if (count != other.count) return false
        if (type != other.type) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = count.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        const val REPETITION_TYPE_UNKNOWN = 0
        const val REPETITION_TYPE_ARM_CURL = 1
        const val REPETITION_TYPE_BACK_EXTENSION = 2
        const val REPETITION_TYPE_BALL_SLAM = 3
        const val REPETITION_TYPE_BENCH_PRESS = 4
        const val REPETITION_TYPE_BURPEE = 5
        const val REPETITION_TYPE_CRUNCH = 6
        const val REPETITION_TYPE_DEADLIFT = 7
        const val REPETITION_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION = 8
        const val REPETITION_TYPE_DUMBBELL_ROW = 9
        const val REPETITION_TYPE_FRONT_RAISE = 10
        const val REPETITION_TYPE_HIP_THRUST = 11
        const val REPETITION_TYPE_HULA_HOOP = 12
        const val REPETITION_TYPE_JUMPING_JACK = 13
        const val REPETITION_TYPE_JUMP_ROPE = 14
        const val REPETITION_TYPE_KETTLEBELL_SWING = 15
        const val REPETITION_TYPE_LATERAL_RAISE = 16
        const val REPETITION_TYPE_LAT_PULL_DOWN = 17
        const val REPETITION_TYPE_LEG_CURL = 18
        const val REPETITION_TYPE_LEG_EXTENSION = 19
        const val REPETITION_TYPE_LEG_PRESS = 20
        const val REPETITION_TYPE_LEG_RAISE = 21
        const val REPETITION_TYPE_LUNGE = 22
        const val REPETITION_TYPE_MOUNTAIN_CLIMBER = 23
        const val REPETITION_TYPE_PLANK = 24
        const val REPETITION_TYPE_PULL_UP = 25
        const val REPETITION_TYPE_PUNCH = 26
        const val REPETITION_TYPE_SHOULDER_PRESS = 27
        const val REPETITION_TYPE_SINGLE_ARM_TRICEPS_EXTENSION = 28
        const val REPETITION_TYPE_SIT_UP = 29
        const val REPETITION_TYPE_SQUAT = 30

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val REPETITION_TYPE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                "arm_curl" to REPETITION_TYPE_ARM_CURL,
                "back_extension" to REPETITION_TYPE_BACK_EXTENSION,
                "ball_slam" to REPETITION_TYPE_BALL_SLAM,
                "bench_press" to REPETITION_TYPE_BENCH_PRESS,
                "burpee" to REPETITION_TYPE_BURPEE,
                "crunch" to REPETITION_TYPE_CRUNCH,
                "deadlift" to REPETITION_TYPE_DEADLIFT,
                "double_arm_triceps_extension" to REPETITION_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                "dumbbell_row" to REPETITION_TYPE_DUMBBELL_ROW,
                "front_raise" to REPETITION_TYPE_FRONT_RAISE,
                "hip_thrust" to REPETITION_TYPE_HIP_THRUST,
                "hula_hoop" to REPETITION_TYPE_HULA_HOOP,
                "jumping_jack" to REPETITION_TYPE_JUMPING_JACK,
                "jump_rope" to REPETITION_TYPE_JUMP_ROPE,
                "kettlebell_swing" to REPETITION_TYPE_KETTLEBELL_SWING,
                "lateral_raise" to REPETITION_TYPE_LATERAL_RAISE,
                "lat_pull_down" to REPETITION_TYPE_LAT_PULL_DOWN,
                "leg_curl" to REPETITION_TYPE_LEG_CURL,
                "leg_extension" to REPETITION_TYPE_LEG_EXTENSION,
                "leg_press" to REPETITION_TYPE_LEG_PRESS,
                "leg_raise" to REPETITION_TYPE_LEG_RAISE,
                "lunge" to REPETITION_TYPE_LUNGE,
                "mountain_climber" to REPETITION_TYPE_MOUNTAIN_CLIMBER,
                "plank" to REPETITION_TYPE_PLANK,
                "pull_up" to REPETITION_TYPE_PULL_UP,
                "punch" to REPETITION_TYPE_PUNCH,
                "shoulder_press" to REPETITION_TYPE_SHOULDER_PRESS,
                "single_arm_triceps_extension" to REPETITION_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                "sit_up" to REPETITION_TYPE_SIT_UP,
                "squat" to REPETITION_TYPE_SQUAT,
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val REPETITION_TYPE_INT_TO_STRING_MAP =
            REPETITION_TYPE_STRING_TO_INT_MAP.entries.associateBy({ it.value }, { it.key })
    }

    /**
     * Exercise types supported by repetitions.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                REPETITION_TYPE_ARM_CURL,
                REPETITION_TYPE_BACK_EXTENSION,
                REPETITION_TYPE_BALL_SLAM,
                REPETITION_TYPE_BENCH_PRESS,
                REPETITION_TYPE_BURPEE,
                REPETITION_TYPE_CRUNCH,
                REPETITION_TYPE_DEADLIFT,
                REPETITION_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION,
                REPETITION_TYPE_DUMBBELL_ROW,
                REPETITION_TYPE_FRONT_RAISE,
                REPETITION_TYPE_HIP_THRUST,
                REPETITION_TYPE_HULA_HOOP,
                REPETITION_TYPE_JUMPING_JACK,
                REPETITION_TYPE_JUMP_ROPE,
                REPETITION_TYPE_KETTLEBELL_SWING,
                REPETITION_TYPE_LATERAL_RAISE,
                REPETITION_TYPE_LAT_PULL_DOWN,
                REPETITION_TYPE_LEG_CURL,
                REPETITION_TYPE_LEG_EXTENSION,
                REPETITION_TYPE_LEG_PRESS,
                REPETITION_TYPE_LEG_RAISE,
                REPETITION_TYPE_LUNGE,
                REPETITION_TYPE_MOUNTAIN_CLIMBER,
                REPETITION_TYPE_PLANK,
                REPETITION_TYPE_PULL_UP,
                REPETITION_TYPE_PUNCH,
                REPETITION_TYPE_SHOULDER_PRESS,
                REPETITION_TYPE_SINGLE_ARM_TRICEPS_EXTENSION,
                REPETITION_TYPE_SIT_UP,
                REPETITION_TYPE_SQUAT,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class RepetitionTypes
}
