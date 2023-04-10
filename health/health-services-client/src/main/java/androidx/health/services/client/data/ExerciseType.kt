/*
 * Copyright (C) 2021 The Android Open Source Project
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

import androidx.annotation.RestrictTo
import androidx.health.services.client.proto.DataProto

/** Exercise type used to configure sensors and algorithms. */
public class ExerciseType @RestrictTo(RestrictTo.Scope.LIBRARY) public constructor(
    /** Returns a unique identifier of for the [ExerciseType], as an `int`. */
    public val id: Int,

    /** Returns a human readable name to represent this [ExerciseType]. */
    public val name: String
) {

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseType) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public fun toProto(): DataProto.ExerciseType =
        DataProto.ExerciseType.forNumber(id) ?: DataProto.ExerciseType.EXERCISE_TYPE_UNKNOWN

    public companion object {
        // Next ID: 93
        /** The current exercise type of the user is unknown or not set. */
        @JvmField public val UNKNOWN: ExerciseType = ExerciseType(0, "UNKNOWN")
        @JvmField public val ALPINE_SKIING: ExerciseType = ExerciseType(92, "ALPINE_SKIING")
        @JvmField public val BACKPACKING: ExerciseType = ExerciseType(84, "BACKPACKING")
        @JvmField public val BACK_EXTENSION: ExerciseType = ExerciseType(1, "BACK_EXTENSION")
        @JvmField public val BADMINTON: ExerciseType = ExerciseType(2, "BADMINTON")
        @JvmField
        public val BARBELL_SHOULDER_PRESS: ExerciseType = ExerciseType(3, "BARBELL_SHOULDER_PRESS")
        @JvmField public val BASEBALL: ExerciseType = ExerciseType(4, "BASEBALL")
        @JvmField public val BASKETBALL: ExerciseType = ExerciseType(5, "BASKETBALL")
        @JvmField public val BENCH_PRESS: ExerciseType = ExerciseType(6, "BENCH_PRESS")
        @JvmField internal val BENCH_SIT_UP: ExerciseType = ExerciseType(7, "BENCH_SIT_UP")
        @JvmField public val BIKING: ExerciseType = ExerciseType(8, "BIKING")
        @JvmField public val BIKING_STATIONARY: ExerciseType = ExerciseType(9, "BIKING_STATIONARY")
        @JvmField public val BOOT_CAMP: ExerciseType = ExerciseType(10, "BOOT_CAMP")
        @JvmField public val BOXING: ExerciseType = ExerciseType(11, "BOXING")
        @JvmField public val BURPEE: ExerciseType = ExerciseType(12, "BURPEE")

        /** Calisthenics (E.g., push ups, sit ups, pull-ups, jumping jacks). */
        @JvmField public val CALISTHENICS: ExerciseType = ExerciseType(13, "CALISTHENICS")
        @JvmField public val CRICKET: ExerciseType = ExerciseType(14, "CRICKET")
        @JvmField
        public val CROSS_COUNTRY_SKIING: ExerciseType = ExerciseType(91, "CROSS_COUNTRY_SKIING")
        @JvmField public val CRUNCH: ExerciseType = ExerciseType(15, "CRUNCH")
        @JvmField public val DANCING: ExerciseType = ExerciseType(16, "DANCING")
        @JvmField public val DEADLIFT: ExerciseType = ExerciseType(17, "DEADLIFT")
        @JvmField
        internal val DUMBBELL_CURL_RIGHT_ARM: ExerciseType =
            ExerciseType(18, "DUMBBELL_CURL_RIGHT_ARM")
        @JvmField
        internal val DUMBBELL_CURL_LEFT_ARM: ExerciseType =
            ExerciseType(19, "DUMBBELL_CURL_LEFT_ARM")
        @JvmField
        internal val DUMBBELL_FRONT_RAISE: ExerciseType = ExerciseType(20, "DUMBBELL_FRONT_RAISE")
        @JvmField
        internal val DUMBBELL_LATERAL_RAISE: ExerciseType =
            ExerciseType(21, "DUMBBELL_LATERAL_RAISE")
        @JvmField
        internal val DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM: ExerciseType =
            ExerciseType(22, "DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM")
        @JvmField
        internal val DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM: ExerciseType =
            ExerciseType(23, "DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM")
        @JvmField
        internal val DUMBBELL_TRICEPS_EXTENSION_TWO_ARM: ExerciseType =
            ExerciseType(24, "DUMBBELL_TRICEPS_EXTENSION_TWO_ARM")
        @JvmField public val ELLIPTICAL: ExerciseType = ExerciseType(25, "ELLIPTICAL")
        @JvmField public val EXERCISE_CLASS: ExerciseType = ExerciseType(26, "EXERCISE_CLASS")
        @JvmField public val FENCING: ExerciseType = ExerciseType(27, "FENCING")
        @JvmField public val FRISBEE_DISC: ExerciseType = ExerciseType(28, "FRISBEE_DISC")
        @JvmField public val FOOTBALL_AMERICAN: ExerciseType = ExerciseType(29, "FOOTBALL_AMERICAN")
        @JvmField
        public val FOOTBALL_AUSTRALIAN: ExerciseType = ExerciseType(30, "FOOTBALL_AUSTRALIAN")
        @JvmField public val FORWARD_TWIST: ExerciseType = ExerciseType(31, "FORWARD_TWIST")
        @JvmField public val GOLF: ExerciseType = ExerciseType(32, "GOLF")
        @JvmField public val GUIDED_BREATHING: ExerciseType = ExerciseType(33, "GUIDED_BREATHING")
        @JvmField public val HORSE_RIDING: ExerciseType = ExerciseType(88, "HORSE_RIDING")
        @JvmField public val GYMNASTICS: ExerciseType = ExerciseType(34, "GYMNASTICS")
        @JvmField public val HANDBALL: ExerciseType = ExerciseType(35, "HANDBALL")
        @JvmField
        public val HIGH_INTENSITY_INTERVAL_TRAINING: ExerciseType =
            ExerciseType(36, "HIGH_INTENSITY_INTERVAL_TRAINING")
        @JvmField public val HIKING: ExerciseType = ExerciseType(37, "HIKING")
        @JvmField public val ICE_HOCKEY: ExerciseType = ExerciseType(38, "ICE_HOCKEY")
        @JvmField public val ICE_SKATING: ExerciseType = ExerciseType(39, "ICE_SKATING")
        @JvmField public val INLINE_SKATING: ExerciseType = ExerciseType(87, "INLINE_SKATING")
        @JvmField public val JUMP_ROPE: ExerciseType = ExerciseType(40, "JUMP_ROPE")
        @JvmField public val JUMPING_JACK: ExerciseType = ExerciseType(41, "JUMPING_JACK")
        @JvmField public val LAT_PULL_DOWN: ExerciseType = ExerciseType(42, "LAT_PULL_DOWN")
        @JvmField public val LUNGE: ExerciseType = ExerciseType(43, "LUNGE")
        @JvmField public val MARTIAL_ARTS: ExerciseType = ExerciseType(44, "MARTIAL_ARTS")
        @JvmField public val MEDITATION: ExerciseType = ExerciseType(45, "MEDITATION")
        @JvmField public val MOUNTAIN_BIKING: ExerciseType = ExerciseType(85, "MOUNTAIN_BIKING")
        @JvmField public val ORIENTEERING: ExerciseType = ExerciseType(86, "ORIENTEERING")
        @JvmField public val PADDLING: ExerciseType = ExerciseType(46, "PADDLING")
        @JvmField public val PARA_GLIDING: ExerciseType = ExerciseType(47, "PARA_GLIDING")
        @JvmField public val PILATES: ExerciseType = ExerciseType(48, "PILATES")
        @JvmField public val PLANK: ExerciseType = ExerciseType(49, "PLANK")
        @JvmField public val RACQUETBALL: ExerciseType = ExerciseType(50, "RACQUETBALL")
        @JvmField public val ROCK_CLIMBING: ExerciseType = ExerciseType(51, "ROCK_CLIMBING")
        @JvmField public val ROLLER_HOCKEY: ExerciseType = ExerciseType(52, "ROLLER_HOCKEY")
        @JvmField public val ROLLER_SKATING: ExerciseType = ExerciseType(89, "ROLLER_SKATING")
        @JvmField public val ROWING: ExerciseType = ExerciseType(53, "ROWING")
        @JvmField public val ROWING_MACHINE: ExerciseType = ExerciseType(54, "ROWING_MACHINE")
        @JvmField public val RUNNING: ExerciseType = ExerciseType(55, "RUNNING")
        @JvmField public val RUNNING_TREADMILL: ExerciseType = ExerciseType(56, "RUNNING_TREADMILL")
        @JvmField public val RUGBY: ExerciseType = ExerciseType(57, "RUGBY")
        @JvmField public val SAILING: ExerciseType = ExerciseType(58, "SAILING")
        @JvmField public val SCUBA_DIVING: ExerciseType = ExerciseType(59, "SCUBA_DIVING")
        @JvmField public val SKATING: ExerciseType = ExerciseType(60, "SKATING")
        @JvmField public val SKIING: ExerciseType = ExerciseType(61, "SKIING")
        @JvmField public val SNOWBOARDING: ExerciseType = ExerciseType(62, "SNOWBOARDING")
        @JvmField public val SNOWSHOEING: ExerciseType = ExerciseType(63, "SNOWSHOEING")
        @JvmField public val SOCCER: ExerciseType = ExerciseType(64, "SOCCER")
        @JvmField public val SOFTBALL: ExerciseType = ExerciseType(65, "SOFTBALL")
        @JvmField public val SQUASH: ExerciseType = ExerciseType(66, "SQUASH")
        @JvmField public val SQUAT: ExerciseType = ExerciseType(67, "SQUAT")
        @JvmField public val STAIR_CLIMBING: ExerciseType = ExerciseType(68, "STAIR_CLIMBING")
        @JvmField
        public val STAIR_CLIMBING_MACHINE: ExerciseType = ExerciseType(69, "STAIR_CLIMBING_MACHINE")
        @JvmField public val STRENGTH_TRAINING: ExerciseType = ExerciseType(70, "STRENGTH_TRAINING")
        @JvmField public val STRETCHING: ExerciseType = ExerciseType(71, "STRETCHING")
        @JvmField public val SURFING: ExerciseType = ExerciseType(72, "SURFING")
        @JvmField
        public val SWIMMING_OPEN_WATER: ExerciseType = ExerciseType(73, "SWIMMING_OPEN_WATER")
        @JvmField public val SWIMMING_POOL: ExerciseType = ExerciseType(74, "SWIMMING_POOL")
        @JvmField public val TABLE_TENNIS: ExerciseType = ExerciseType(75, "TABLE_TENNIS")
        @JvmField public val TENNIS: ExerciseType = ExerciseType(76, "TENNIS")
        @JvmField public val UPPER_TWIST: ExerciseType = ExerciseType(77, "UPPER_TWIST")
        @JvmField public val VOLLEYBALL: ExerciseType = ExerciseType(78, "VOLLEYBALL")
        @JvmField public val WALKING: ExerciseType = ExerciseType(79, "WALKING")
        @JvmField public val WATER_POLO: ExerciseType = ExerciseType(80, "WATER_POLO")
        @JvmField public val WEIGHTLIFTING: ExerciseType = ExerciseType(81, "WEIGHTLIFTING")
        @JvmField public val WORKOUT: ExerciseType = ExerciseType(82, "WORKOUT")
        @JvmField public val YACHTING: ExerciseType = ExerciseType(90, "YACHTING")
        @JvmField public val YOGA: ExerciseType = ExerciseType(83, "YOGA")

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        public val VALUES: List<ExerciseType> =
            listOf(
                UNKNOWN,
                ALPINE_SKIING,
                BACKPACKING,
                BACK_EXTENSION,
                BADMINTON,
                BARBELL_SHOULDER_PRESS,
                BASEBALL,
                BASKETBALL,
                BENCH_PRESS,
                BENCH_SIT_UP,
                BIKING,
                BIKING_STATIONARY,
                BOOT_CAMP,
                BOXING,
                BURPEE,
                CALISTHENICS,
                CRICKET,
                CROSS_COUNTRY_SKIING,
                CRUNCH,
                DANCING,
                DEADLIFT,
                DUMBBELL_CURL_RIGHT_ARM,
                DUMBBELL_CURL_LEFT_ARM,
                DUMBBELL_FRONT_RAISE,
                DUMBBELL_LATERAL_RAISE,
                DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM,
                DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM,
                DUMBBELL_TRICEPS_EXTENSION_TWO_ARM,
                ELLIPTICAL,
                EXERCISE_CLASS,
                FENCING,
                FRISBEE_DISC,
                FOOTBALL_AMERICAN,
                FOOTBALL_AUSTRALIAN,
                FORWARD_TWIST,
                GOLF,
                GUIDED_BREATHING,
                HORSE_RIDING,
                GYMNASTICS,
                HANDBALL,
                HIGH_INTENSITY_INTERVAL_TRAINING,
                HIKING,
                ICE_HOCKEY,
                ICE_SKATING,
                INLINE_SKATING,
                JUMP_ROPE,
                JUMPING_JACK,
                LAT_PULL_DOWN,
                LUNGE,
                MARTIAL_ARTS,
                MEDITATION,
                MOUNTAIN_BIKING,
                ORIENTEERING,
                PADDLING,
                PARA_GLIDING,
                PILATES,
                PLANK,
                RACQUETBALL,
                ROCK_CLIMBING,
                ROLLER_HOCKEY,
                ROLLER_SKATING,
                ROWING,
                ROWING_MACHINE,
                RUNNING,
                RUNNING_TREADMILL,
                RUGBY,
                SAILING,
                SCUBA_DIVING,
                SKATING,
                SKIING,
                SNOWBOARDING,
                SNOWSHOEING,
                SOCCER,
                SOFTBALL,
                SQUASH,
                SQUAT,
                STAIR_CLIMBING,
                STAIR_CLIMBING_MACHINE,
                STRENGTH_TRAINING,
                STRETCHING,
                SURFING,
                SWIMMING_OPEN_WATER,
                SWIMMING_POOL,
                TABLE_TENNIS,
                TENNIS,
                UPPER_TWIST,
                VOLLEYBALL,
                WALKING,
                WATER_POLO,
                WEIGHTLIFTING,
                WORKOUT,
                YACHTING,
                YOGA,
            )

        private val IDS = VALUES.map { it.id to it }.toMap()

        /**
         * Returns the [ExerciseType] based on its unique `id`.
         *
         * If the `id` doesn't map to an particular [ExerciseType], then [ExerciseType.UNKNOWN] is
         * returned by default.
         */
        @JvmStatic
        public fun fromId(id: Int): ExerciseType {
            val exerciseType = IDS[id]
            return exerciseType ?: UNKNOWN
        }

        /** @hide */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public fun fromProto(proto: DataProto.ExerciseType): ExerciseType = fromId(proto.number)
    }
}
