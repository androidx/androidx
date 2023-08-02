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
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.records.metadata.Metadata
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures any exercise a user does. This can be common fitness exercise like running or different
 * sports.
 *
 * Each record needs a start time and end time. Records don't need to be back-to-back or directly
 * after each other, there can be gaps in between.
 *
 * Example code demonstrate how to read exercise session:
 * @sample androidx.health.connect.client.samples.ReadExerciseSessions
 */
public class ExerciseSessionRecord(
    /**
     * Type of exercise (e.g. walking, swimming). Required field. Allowed values: [ExerciseType].
     *
     * @see ExerciseType
     */
    @property:ExerciseTypes public val exerciseType: String,
    /** Title of the session. Optional field. */
    public val title: String? = null,
    /** Additional notes for the session. Optional field. */
    public val notes: String? = null,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseSessionRecord) return false

        if (exerciseType != other.exerciseType) return false
        if (title != other.title) return false
        if (notes != other.notes) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + exerciseType.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        /**
         * Metric identifier to retrieve the total active time from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val ACTIVE_TIME_TOTAL: AggregateMetric<Duration> =
            AggregateMetric.durationMetric(
                dataTypeName = "ActiveTime",
                aggregationType = AggregateMetric.AggregationType.TOTAL,
                fieldName = "time",
            )
        // Active time requires computing total time from ExerciseEvent/Session and is not a
        // straightforward Duration aggregation.
    }

    /** List of supported exercise type on Health Platform. */
    public object ExerciseType {
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
        const val WHEELCHAIR = "wheelchair"
        const val WORKOUT = "workout"
        const val YOGA = "yoga"
    }

    /**
     * List of supported activities on Health Platform.
     * @suppress
     */
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @StringDef(
        value =
            [
                ExerciseType.BACK_EXTENSION,
                ExerciseType.BADMINTON,
                ExerciseType.BARBELL_SHOULDER_PRESS,
                ExerciseType.BASEBALL,
                ExerciseType.BASKETBALL,
                ExerciseType.BENCH_PRESS,
                ExerciseType.BENCH_SIT_UP,
                ExerciseType.BIKING,
                ExerciseType.BIKING_STATIONARY,
                ExerciseType.BOOT_CAMP,
                ExerciseType.BOXING,
                ExerciseType.BURPEE,
                ExerciseType.CALISTHENICS,
                ExerciseType.CRICKET,
                ExerciseType.CRUNCH,
                ExerciseType.DANCING,
                ExerciseType.DEADLIFT,
                ExerciseType.DUMBBELL_CURL_LEFT_ARM,
                ExerciseType.DUMBBELL_CURL_RIGHT_ARM,
                ExerciseType.DUMBBELL_FRONT_RAISE,
                ExerciseType.DUMBBELL_LATERAL_RAISE,
                ExerciseType.DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                ExerciseType.DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                ExerciseType.DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                ExerciseType.ELLIPTICAL,
                ExerciseType.EXERCISE_CLASS,
                ExerciseType.FENCING,
                ExerciseType.FOOTBALL_AMERICAN,
                ExerciseType.FOOTBALL_AUSTRALIAN,
                ExerciseType.FORWARD_TWIST,
                ExerciseType.FRISBEE_DISC,
                ExerciseType.GOLF,
                ExerciseType.GUIDED_BREATHING,
                ExerciseType.GYMNASTICS,
                ExerciseType.HANDBALL,
                ExerciseType.HIGH_INTENSITY_INTERVAL_TRAINING,
                ExerciseType.HIKING,
                ExerciseType.ICE_HOCKEY,
                ExerciseType.ICE_SKATING,
                ExerciseType.JUMPING_JACK,
                ExerciseType.JUMP_ROPE,
                ExerciseType.LAT_PULL_DOWN,
                ExerciseType.LUNGE,
                ExerciseType.MARTIAL_ARTS,
                ExerciseType.MEDITATION,
                ExerciseType.PADDLING,
                ExerciseType.PARA_GLIDING,
                ExerciseType.PILATES,
                ExerciseType.PLANK,
                ExerciseType.RACQUETBALL,
                ExerciseType.ROCK_CLIMBING,
                ExerciseType.ROLLER_HOCKEY,
                ExerciseType.ROWING,
                ExerciseType.ROWING_MACHINE,
                ExerciseType.RUGBY,
                ExerciseType.RUNNING,
                ExerciseType.RUNNING_TREADMILL,
                ExerciseType.SAILING,
                ExerciseType.SCUBA_DIVING,
                ExerciseType.SKATING,
                ExerciseType.SKIING,
                ExerciseType.SNOWBOARDING,
                ExerciseType.SNOWSHOEING,
                ExerciseType.SOCCER,
                ExerciseType.SOFTBALL,
                ExerciseType.SQUASH,
                ExerciseType.SQUAT,
                ExerciseType.STAIR_CLIMBING,
                ExerciseType.STAIR_CLIMBING_MACHINE,
                ExerciseType.STRENGTH_TRAINING,
                ExerciseType.STRETCHING,
                ExerciseType.SURFING,
                ExerciseType.SWIMMING_OPEN_WATER,
                ExerciseType.SWIMMING_POOL,
                ExerciseType.TABLE_TENNIS,
                ExerciseType.TENNIS,
                ExerciseType.UPPER_TWIST,
                ExerciseType.VOLLEYBALL,
                ExerciseType.WALKING,
                ExerciseType.WATER_POLO,
                ExerciseType.WEIGHTLIFTING,
                ExerciseType.WHEELCHAIR,
                ExerciseType.WORKOUT,
                ExerciseType.YOGA,
            ]
    )
    annotation class ExerciseTypes
}
