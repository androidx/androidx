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
    GOLF(31),
    GUIDED_BREATHING(32),
    GYNMASTICS(33),
    HANDBALL(34),
    HIGH_INTENSITY_INTERVAL_TRAINING(35),
    HIKING(36),
    ICE_HOCKEY(37),
    ICE_SKATING(38),
    JUMP_ROPE(39),
    JUMPING_JACK(40),
    LAT_PULL_DOWN(41),
    LUNGE(42),
    MARTIAL_ARTS(43),
    MEDITATION(44),
    PADDLING(45),
    PARA_GLIDING(46),
    PILATES(47),
    PLANK(48),
    RACQUETBALL(49),
    ROCK_CLIMBING(50),
    ROLLER_HOCKEY(51),
    ROWING(52),
    ROWING_MACHINE(53),
    RUNNING(54),
    RUNNING_TREADMILL(55),
    RUGBY(56),
    SAILING(57),
    SCUBA_DIVING(58),
    SKATING(59),
    SKIING(60),
    SNOWBOARDING(61),
    SNOWSHOEING(62),
    SOCCER(63),
    SOFTBALL(64),
    SQUASH(65),
    SQUAT(66),
    STAIR_CLIMBING(67),
    STAIR_CLIMBING_MACHINE(68),
    STRENGTH_TRAINING(69),
    STRETCHING(70),
    SURFING(71),
    SWIMMING_OPEN_WATER(72),
    SWIMMING_POOL(73),
    TABLE_TENNIS(74),
    TENNIS(75),
    VOLLEYBALL(76),
    WALKING(77),
    WATER_POLO(78),
    WEIGHTLIFTING(79),
    WORKOUT_INDOOR(80),
    WORKOUT_OUTDOOR(81),
    YOGA(82);

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
    }
}
