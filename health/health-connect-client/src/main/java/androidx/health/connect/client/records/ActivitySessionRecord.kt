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
 * Captures any activity a user does. This can be common fitness activities like running or
 * different sports, as well as activities like meditation, gardening, and sleep.
 *
 * If the user was doing more than one activity during that time period, create a session for the
 * main activity type, and multiple segments for the different activity types. For example, if they
 * did a little bit of kick boxing and boxing during a mixed martial arts class, create a session
 * for mixed martial arts. You can then breakdown the different activity types into segments.
 *
 * Each record needs a start time and end time. Data points don't need to be back-to-back or
 * directly after each other, there can be gaps in between.
 */
public class ActivitySessionRecord(
    /**
     * Type of activity (e.g. walking, swimming). Required field. Allowed values: [ActivityType].
     */
    @property:ActivityTypes public val activityType: String,
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
        if (other !is ActivitySessionRecord) return false

        if (activityType != other.activityType) return false
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
        result = 31 * result + activityType.hashCode()
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
        // Active time requires computing total time from ActivityEvent/Session and is not a
        // straightforward Duration aggregation.
    }

    /** List of supported activities on Health Platform. */
    public object ActivityType {
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
                ActivityType.BACK_EXTENSION,
                ActivityType.BADMINTON,
                ActivityType.BARBELL_SHOULDER_PRESS,
                ActivityType.BASEBALL,
                ActivityType.BASKETBALL,
                ActivityType.BENCH_PRESS,
                ActivityType.BENCH_SIT_UP,
                ActivityType.BIKING,
                ActivityType.BIKING_STATIONARY,
                ActivityType.BOOT_CAMP,
                ActivityType.BOXING,
                ActivityType.BURPEE,
                ActivityType.CALISTHENICS,
                ActivityType.CRICKET,
                ActivityType.CRUNCH,
                ActivityType.DANCING,
                ActivityType.DEADLIFT,
                ActivityType.DUMBBELL_CURL_LEFT_ARM,
                ActivityType.DUMBBELL_CURL_RIGHT_ARM,
                ActivityType.DUMBBELL_FRONT_RAISE,
                ActivityType.DUMBBELL_LATERAL_RAISE,
                ActivityType.DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                ActivityType.DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                ActivityType.DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                ActivityType.ELLIPTICAL,
                ActivityType.EXERCISE_CLASS,
                ActivityType.FENCING,
                ActivityType.FOOTBALL_AMERICAN,
                ActivityType.FOOTBALL_AUSTRALIAN,
                ActivityType.FORWARD_TWIST,
                ActivityType.FRISBEE_DISC,
                ActivityType.GOLF,
                ActivityType.GUIDED_BREATHING,
                ActivityType.GYMNASTICS,
                ActivityType.HANDBALL,
                ActivityType.HIGH_INTENSITY_INTERVAL_TRAINING,
                ActivityType.HIKING,
                ActivityType.ICE_HOCKEY,
                ActivityType.ICE_SKATING,
                ActivityType.JUMPING_JACK,
                ActivityType.JUMP_ROPE,
                ActivityType.LAT_PULL_DOWN,
                ActivityType.LUNGE,
                ActivityType.MARTIAL_ARTS,
                ActivityType.MEDITATION,
                ActivityType.PADDLING,
                ActivityType.PARA_GLIDING,
                ActivityType.PILATES,
                ActivityType.PLANK,
                ActivityType.RACQUETBALL,
                ActivityType.ROCK_CLIMBING,
                ActivityType.ROLLER_HOCKEY,
                ActivityType.ROWING,
                ActivityType.ROWING_MACHINE,
                ActivityType.RUGBY,
                ActivityType.RUNNING,
                ActivityType.RUNNING_TREADMILL,
                ActivityType.SAILING,
                ActivityType.SCUBA_DIVING,
                ActivityType.SKATING,
                ActivityType.SKIING,
                ActivityType.SNOWBOARDING,
                ActivityType.SNOWSHOEING,
                ActivityType.SOCCER,
                ActivityType.SOFTBALL,
                ActivityType.SQUASH,
                ActivityType.SQUAT,
                ActivityType.STAIR_CLIMBING,
                ActivityType.STAIR_CLIMBING_MACHINE,
                ActivityType.STRENGTH_TRAINING,
                ActivityType.STRETCHING,
                ActivityType.SURFING,
                ActivityType.SWIMMING_OPEN_WATER,
                ActivityType.SWIMMING_POOL,
                ActivityType.TABLE_TENNIS,
                ActivityType.TENNIS,
                ActivityType.UPPER_TWIST,
                ActivityType.VOLLEYBALL,
                ActivityType.WALKING,
                ActivityType.WATER_POLO,
                ActivityType.WEIGHTLIFTING,
                ActivityType.WHEELCHAIR,
                ActivityType.WORKOUT,
                ActivityType.YOGA,
            ]
    )
    annotation class ActivityTypes
}
