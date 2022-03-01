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
package androidx.health.data.client.records

import androidx.annotation.StringDef

/** List of supported activities on Health Platform. */
public object ActivityTypes {
    const val BACK_EXTENSION = "back_extension"
    const val BADMINTON = "badminton"
    const val BARBELL_SHOULDER_PRESS = "barbell_shoulder_press"
    const val BASEBALL = "baseball"
    const val BASKETBALL = "basketball"
    const val BENCH_PRESS = "bench_press"
    const val BENCH_SIT_UP = "bench_sit_up"
    const val BIKING = "biking"
    const val BIKING_STATIONARY = "biking_stationary"
    const val BOOT_CAMP = "boot_camp"
    const val BOXING = "boxing"
    const val BURPEE = "burpee"
    const val CALISTHENICS = "calisthenics"
    const val CRICKET = "cricket"
    const val CRUNCH = "crunch"
    const val DANCING = "dancing"
    const val DEADLIFT = "deadlift"
    const val DUMBBELL_CURL_LEFT_ARM = "dumbbell_curl_left_arm"
    const val DUMBBELL_CURL_RIGHT_ARM = "dumbbell_curl_right_arm"
    const val DUMBBELL_FRONT_RAISE = "dumbbell_front_raise"
    const val DUMBBELL_LATERAL_RAISE = "dumbbell_lateral_raise"
    const val DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM = "dumbbell_triceps_extension_left_arm"
    const val DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM = "dumbbell_triceps_extension_right_arm"
    const val DUMBBELL_TRICEPS_EXTENSION_TWO_ARM = "dumbbell_triceps_extension_two_arm"
    const val ELLIPTICAL = "elliptical"
    const val EXERCISE_CLASS = "exercise_class"
    const val FENCING = "fencing"
    const val FOOTBALL_AMERICAN = "football_american"
    const val FOOTBALL_AUSTRALIAN = "football_australian"
    const val FORWARD_TWIST = "forward_twist"
    const val FRISBEE_DISC = "frisbee_disc"
    const val GOLF = "golf"
    const val GUIDED_BREATHING = "guided_breathing"
    const val GYMNASTICS = "gymnastics"
    const val HANDBALL = "handball"
    const val HIGH_INTENSITY_INTERVAL_TRAINING = "high_intensity_interval_training"
    const val HIKING = "hiking"
    const val ICE_HOCKEY = "ice_hockey"
    const val ICE_SKATING = "ice_skating"
    const val JUMPING_JACK = "jumping_jack"
    const val JUMP_ROPE = "jump_rope"
    const val LAT_PULL_DOWN = "lat_pull_down"
    const val LUNGE = "lunge"
    const val MARTIAL_ARTS = "martial_arts"
    const val MEDITATION = "meditation"
    const val PADDLING = "paddling"
    const val PARA_GLIDING = "para_gliding"
    const val PILATES = "pilates"
    const val PLANK = "plank"
    const val RACQUETBALL = "racquetball"
    const val ROCK_CLIMBING = "rock_climbing"
    const val ROLLER_HOCKEY = "roller_hockey"
    const val ROWING = "rowing"
    const val ROWING_MACHINE = "rowing_machine"
    const val RUGBY = "rugby"
    const val RUNNING = "running"
    const val RUNNING_TREADMILL = "running_treadmill"
    const val SAILING = "sailing"
    const val SCUBA_DIVING = "scuba_diving"
    const val SKATING = "skating"
    const val SKIING = "skiing"
    const val SNOWBOARDING = "snowboarding"
    const val SNOWSHOEING = "snowshoeing"
    const val SOCCER = "soccer"
    const val SOFTBALL = "softball"
    const val SQUASH = "squash"
    const val SQUAT = "squat"
    const val STAIR_CLIMBING = "stair_climbing"
    const val STAIR_CLIMBING_MACHINE = "stair_climbing_machine"
    const val STRENGTH_TRAINING = "strength_training"
    const val STRETCHING = "stretching"
    const val SURFING = "surfing"
    const val SWIMMING_OPEN_WATER = "swimming_open_water"
    const val SWIMMING_POOL = "swimming_pool"
    const val TABLE_TENNIS = "table_tennis"
    const val TENNIS = "tennis"
    const val UPPER_TWIST = "upper_twist"
    const val VOLLEYBALL = "volleyball"
    const val WALKING = "walking"
    const val WATER_POLO = "water_polo"
    const val WEIGHTLIFTING = "weightlifting"
    const val WORKOUT = "workout"
    const val YOGA = "yoga"
}

/**
 * List of supported activities on Health Platform.
 * @suppress
 */
@Retention(AnnotationRetention.SOURCE)
@StringDef(
    value =
        [
            ActivityTypes.BACK_EXTENSION,
            ActivityTypes.BADMINTON,
            ActivityTypes.BARBELL_SHOULDER_PRESS,
            ActivityTypes.BASEBALL,
            ActivityTypes.BASKETBALL,
            ActivityTypes.BENCH_PRESS,
            ActivityTypes.BENCH_SIT_UP,
            ActivityTypes.BIKING,
            ActivityTypes.BIKING_STATIONARY,
            ActivityTypes.BOOT_CAMP,
            ActivityTypes.BOXING,
            ActivityTypes.BURPEE,
            ActivityTypes.CALISTHENICS,
            ActivityTypes.CRICKET,
            ActivityTypes.CRUNCH,
            ActivityTypes.DANCING,
            ActivityTypes.DEADLIFT,
            ActivityTypes.DUMBBELL_CURL_LEFT_ARM,
            ActivityTypes.DUMBBELL_CURL_RIGHT_ARM,
            ActivityTypes.DUMBBELL_FRONT_RAISE,
            ActivityTypes.DUMBBELL_LATERAL_RAISE,
            ActivityTypes.DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
            ActivityTypes.DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
            ActivityTypes.DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
            ActivityTypes.ELLIPTICAL,
            ActivityTypes.EXERCISE_CLASS,
            ActivityTypes.FENCING,
            ActivityTypes.FOOTBALL_AMERICAN,
            ActivityTypes.FOOTBALL_AUSTRALIAN,
            ActivityTypes.FORWARD_TWIST,
            ActivityTypes.FRISBEE_DISC,
            ActivityTypes.GOLF,
            ActivityTypes.GUIDED_BREATHING,
            ActivityTypes.GYMNASTICS,
            ActivityTypes.HANDBALL,
            ActivityTypes.HIGH_INTENSITY_INTERVAL_TRAINING,
            ActivityTypes.HIKING,
            ActivityTypes.ICE_HOCKEY,
            ActivityTypes.ICE_SKATING,
            ActivityTypes.JUMPING_JACK,
            ActivityTypes.JUMP_ROPE,
            ActivityTypes.LAT_PULL_DOWN,
            ActivityTypes.LUNGE,
            ActivityTypes.MARTIAL_ARTS,
            ActivityTypes.MEDITATION,
            ActivityTypes.PADDLING,
            ActivityTypes.PARA_GLIDING,
            ActivityTypes.PILATES,
            ActivityTypes.PLANK,
            ActivityTypes.RACQUETBALL,
            ActivityTypes.ROCK_CLIMBING,
            ActivityTypes.ROLLER_HOCKEY,
            ActivityTypes.ROWING,
            ActivityTypes.ROWING_MACHINE,
            ActivityTypes.RUGBY,
            ActivityTypes.RUNNING,
            ActivityTypes.RUNNING_TREADMILL,
            ActivityTypes.SAILING,
            ActivityTypes.SCUBA_DIVING,
            ActivityTypes.SKATING,
            ActivityTypes.SKIING,
            ActivityTypes.SNOWBOARDING,
            ActivityTypes.SNOWSHOEING,
            ActivityTypes.SOCCER,
            ActivityTypes.SOFTBALL,
            ActivityTypes.SQUASH,
            ActivityTypes.SQUAT,
            ActivityTypes.STAIR_CLIMBING,
            ActivityTypes.STAIR_CLIMBING_MACHINE,
            ActivityTypes.STRENGTH_TRAINING,
            ActivityTypes.STRETCHING,
            ActivityTypes.SURFING,
            ActivityTypes.SWIMMING_OPEN_WATER,
            ActivityTypes.SWIMMING_POOL,
            ActivityTypes.TABLE_TENNIS,
            ActivityTypes.TENNIS,
            ActivityTypes.UPPER_TWIST,
            ActivityTypes.VOLLEYBALL,
            ActivityTypes.WALKING,
            ActivityTypes.WATER_POLO,
            ActivityTypes.WEIGHTLIFTING,
            ActivityTypes.WORKOUT,
            ActivityTypes.YOGA,
        ]
)
annotation class ActivityType
