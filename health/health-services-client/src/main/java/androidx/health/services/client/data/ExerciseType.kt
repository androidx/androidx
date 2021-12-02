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

import androidx.health.services.client.proto.DataProto

// TODO(b/185276729): Keep track of values separately to maintain alphabetical order
// once values are locked in
/** Exercise type used to configure sensors and algorithms. */
public enum class ExerciseType(
    /** Returns a unique identifier of for the [ExerciseType], as an `int`. */
    public val id: Int
) {
    /** The current exercise type of the user is unknown or not set. */
    UNKNOWN(0),
    BACK_EXTENSION(1),
    BADMINTON(2),
    BARBELL_SHOULDER_PRESS(3),
    BASEBALL(4),
    BASKETBALL(5),
    BENCH_PRESS(6),
    BENCH_SIT_UP(7),
    BIKING(8),
    BIKING_STATIONARY(9),
    BOOT_CAMP(10),
    BOXING(11),
    BURPEE(12),

    /** (E.g., push ups, sit ups, pull-ups, jumping jacks). */
    CALISTHENICS(13),
    CRICKET(14),
    CRUNCH(15),
    DANCING(16),
    DEADLIFT(17),
    DUMBBELL_CURL_RIGHT_ARM(18),
    DUMBBELL_CURL_LEFT_ARM(19),
    DUMBBELL_FRONT_RAISE(20),
    DUMBBELL_LATERAL_RAISE(21),
    DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM(22),
    DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM(23),
    DUMBBELL_TRICEPS_EXTENSION_TWO_ARM(24),
    ELLIPTICAL(25),
    EXERCISE_CLASS(26),
    FENCING(27),
    FRISBEE_DISC(28),
    FOOTBALL_AMERICAN(29),
    FOOTBALL_AUSTRALIAN(30),
    FORWARD_TWIST(31),
    GOLF(32),
    GUIDED_BREATHING(33),
    GYMNASTICS(34),
    HANDBALL(35),
    HIGH_INTENSITY_INTERVAL_TRAINING(36),
    HIKING(37),
    ICE_HOCKEY(38),
    ICE_SKATING(39),
    JUMP_ROPE(40),
    JUMPING_JACK(41),
    LAT_PULL_DOWN(42),
    LUNGE(43),
    MARTIAL_ARTS(44),
    MEDITATION(45),
    PADDLING(46),
    PARA_GLIDING(47),
    PILATES(48),
    PLANK(49),
    RACQUETBALL(50),
    ROCK_CLIMBING(51),
    ROLLER_HOCKEY(52),
    ROWING(53),
    ROWING_MACHINE(54),
    RUNNING(55),
    RUNNING_TREADMILL(56),
    RUGBY(57),
    SAILING(58),
    SCUBA_DIVING(59),
    SKATING(60),
    SKIING(61),
    SNOWBOARDING(62),
    SNOWSHOEING(63),
    SOCCER(64),
    SOFTBALL(65),
    SQUASH(66),
    SQUAT(67),
    STAIR_CLIMBING(68),
    STAIR_CLIMBING_MACHINE(69),
    STRENGTH_TRAINING(70),
    STRETCHING(71),
    SURFING(72),
    SWIMMING_OPEN_WATER(73),
    SWIMMING_POOL(74),
    TABLE_TENNIS(75),
    TENNIS(76),
    UPPER_TWIST(77),
    VOLLEYBALL(78),
    WALKING(79),
    WATER_POLO(80),
    WEIGHTLIFTING(81),
    WORKOUT(82),
    YOGA(83);

    /** @hide */
    public fun toProto(): DataProto.ExerciseType =
        DataProto.ExerciseType.forNumber(id) ?: DataProto.ExerciseType.EXERCISE_TYPE_UNKNOWN

    public companion object {
        private val IDS = initialize()
        private fun initialize(): Map<Int, ExerciseType> {
            val map = mutableMapOf<Int, ExerciseType>()
            for (exerciseType in values()) {
                map.put(exerciseType.id, exerciseType)
            }
            return map
        }

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
        public fun fromProto(proto: DataProto.ExerciseType): ExerciseType = fromId(proto.number)
    }
}
