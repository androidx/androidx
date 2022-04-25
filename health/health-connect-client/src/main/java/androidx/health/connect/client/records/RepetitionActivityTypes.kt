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

/** Activity types supported by repetitions. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public object RepetitionActivityTypes {
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
            RepetitionActivityTypes.ARM_CURL,
            RepetitionActivityTypes.BACK_EXTENSION,
            RepetitionActivityTypes.BALL_SLAM,
            RepetitionActivityTypes.BENCH_PRESS,
            RepetitionActivityTypes.BURPEE,
            RepetitionActivityTypes.CRUNCH,
            RepetitionActivityTypes.DEADLIFT,
            RepetitionActivityTypes.DOUBLE_ARM_TRICEPS_EXTENSION,
            RepetitionActivityTypes.DUMBBELL_ROW,
            RepetitionActivityTypes.FRONT_RAISE,
            RepetitionActivityTypes.HIP_THRUST,
            RepetitionActivityTypes.HULA_HOOP,
            RepetitionActivityTypes.JUMPING_JACK,
            RepetitionActivityTypes.JUMP_ROPE,
            RepetitionActivityTypes.KETTLEBELL_SWING,
            RepetitionActivityTypes.LATERAL_RAISE,
            RepetitionActivityTypes.LAT_PULL_DOWN,
            RepetitionActivityTypes.LEG_CURL,
            RepetitionActivityTypes.LEG_EXTENSION,
            RepetitionActivityTypes.LEG_PRESS,
            RepetitionActivityTypes.LEG_RAISE,
            RepetitionActivityTypes.LUNGE,
            RepetitionActivityTypes.MOUNTAIN_CLIMBER,
            RepetitionActivityTypes.PLANK,
            RepetitionActivityTypes.PULL_UP,
            RepetitionActivityTypes.PUNCH,
            RepetitionActivityTypes.SHOULDER_PRESS,
            RepetitionActivityTypes.SINGLE_ARM_TRICEPS_EXTENSION,
            RepetitionActivityTypes.SIT_UP,
            RepetitionActivityTypes.SQUAT
        ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY)
annotation class RepetitionActivityType
