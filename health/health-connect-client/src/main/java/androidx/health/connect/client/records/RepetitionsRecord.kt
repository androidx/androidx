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

import androidx.annotation.RestrictTo
import androidx.annotation.StringDef
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures the number of repetitions in an exercise set. */
public class RepetitionsRecord(
    /** Count. Required field. Valid range: 1-1000000. */
    public val count: Long,
    /** Type of activity being repeated. Optional field. Allowed values: [ActivityType]. */
    @property:ActivityTypes public val type: String,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    init {
        requireNonNegative(value = count, name = "count")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RepetitionsRecord) return false

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
        var result = 0
        result = 31 * result + count.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    /** Activity types supported by [RepetitionsRecord]. */
    public object ActivityType {
        const val ARM_CURL = "arm_curl"
        const val BACK_EXTENSION = "back_extension"
        const val BALL_SLAM = "ball_slam"
        const val BENCH_PRESS = "bench_press"
        const val BURPEE = "burpee"
        const val CRUNCH = "crunch"
        const val DEADLIFT = "deadlift"
        const val DOUBLE_ARM_TRICEPS_EXTENSION = "double_arm_triceps_extension"
        const val DUMBBELL_ROW = "dumbbell_row"
        const val FRONT_RAISE = "front_raise"
        const val HIP_THRUST = "hip_thrust"
        const val HULA_HOOP = "hula_hoop"
        const val JUMPING_JACK = "jumping_jack"
        const val JUMP_ROPE = "jump_rope"
        const val KETTLEBELL_SWING = "kettlebell_swing"
        const val LATERAL_RAISE = "lateral_raise"
        const val LAT_PULL_DOWN = "lat_pull_down"
        const val LEG_CURL = "leg_curl"
        const val LEG_EXTENSION = "leg_extension"
        const val LEG_PRESS = "leg_press"
        const val LEG_RAISE = "leg_raise"
        const val LUNGE = "lunge"
        const val MOUNTAIN_CLIMBER = "mountain_climber"
        const val PLANK = "plank"
        const val PULL_UP = "pull_up"
        const val PUNCH = "punch"
        const val SHOULDER_PRESS = "shoulder_press"
        const val SINGLE_ARM_TRICEPS_EXTENSION = "single_arm_triceps_extension"
        const val SIT_UP = "sit_up"
        const val SQUAT = "squat"
    }

    /**
     * Activity types supported by repetitions.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(
        value =
            [
                ActivityType.ARM_CURL,
                ActivityType.BACK_EXTENSION,
                ActivityType.BALL_SLAM,
                ActivityType.BENCH_PRESS,
                ActivityType.BURPEE,
                ActivityType.CRUNCH,
                ActivityType.DEADLIFT,
                ActivityType.DOUBLE_ARM_TRICEPS_EXTENSION,
                ActivityType.DUMBBELL_ROW,
                ActivityType.FRONT_RAISE,
                ActivityType.HIP_THRUST,
                ActivityType.HULA_HOOP,
                ActivityType.JUMPING_JACK,
                ActivityType.JUMP_ROPE,
                ActivityType.KETTLEBELL_SWING,
                ActivityType.LATERAL_RAISE,
                ActivityType.LAT_PULL_DOWN,
                ActivityType.LEG_CURL,
                ActivityType.LEG_EXTENSION,
                ActivityType.LEG_PRESS,
                ActivityType.LEG_RAISE,
                ActivityType.LUNGE,
                ActivityType.MOUNTAIN_CLIMBER,
                ActivityType.PLANK,
                ActivityType.PULL_UP,
                ActivityType.PUNCH,
                ActivityType.SHOULDER_PRESS,
                ActivityType.SINGLE_ARM_TRICEPS_EXTENSION,
                ActivityType.SIT_UP,
                ActivityType.SQUAT
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class ActivityTypes
}
