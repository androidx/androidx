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

import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.health.connect.client.impl.platform.records.toPlatformExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord.ExerciseTypes
import androidx.health.connect.client.units.Mass
import java.time.Instant

/**
 * Represents particular exercise within an exercise session.
 *
 * <p>Each segment contains start and end time of the exercise, exercise type and optional metrics
 * such as number of repetitions, weight used, set index, and rate of perceived exertion.
 *
 * @see ExerciseSessionRecord
 */
public class ExerciseSegment
@JvmOverloads
constructor(
    public val startTime: Instant,
    public val endTime: Instant,
    /** Type of segment (e.g. biking, plank). */
    @property:ExerciseSegmentTypes public val segmentType: Int,
    /** Number of repetitions in the segment. Must be non-negative. */
    @IntRange(from = 0) @get:IntRange(from = 0) public val repetitions: Int = 0,
    /** Weight used in the segment. Must be non-negative. */
    public val weight: Mass? = null,
    @Suppress("AutoBoxing")
    @get:Suppress("AutoBoxing")
    /**
     * Index of the set in the session.
     *
     * A set is a group of consecutive repetitions (reps) of a specific exercise performed without a
     * break, e.g. 10 push-ups in a row without stopping.
     *
     * A set index represents the position of this set relative to other sets in the session. For
     * instance, if an exercise has three sets, they will have setIndex values of 0, 1, and 2
     * respectively.
     *
     * Multiple segments may be part of a single set, for example if a collection of activities are
     * considered to be a single set, in which case those segments would have the same set index.
     *
     * The set index may also go back to zero in a single [ExerciseSessionRecord]. For example, if
     * three sets of one activity are completed followed by three sets of another, setIndex values
     * of 0, 1, 2, 0, 1, 2 would be expected for those segments.
     *
     * Must be non-negative.
     */
    @IntRange(from = 0)
    @get:IntRange(from = 0)
    public val setIndex: Int? = null,
    @Suppress("AutoBoxing")
    @get:Suppress("AutoBoxing")
    /**
     * Rate of perceived exertion (RPE) for the segment.
     *
     * Values correspond to the Borg CR10 RPE scale and must be in the range 0 to 10 inclusive.
     * - 0: No exertion (at rest)
     * - 1: Very light
     * - 2-3: Light
     * - 4-5: Moderate
     * - 6-7: Hard
     * - 8-9: Very hard
     * - 10: Maximum effort
     */
    @FloatRange(from = 0.0, to = 10.0)
    @get:FloatRange(from = 0.0, to = 10.0)
    public val rateOfPerceivedExertion: Float? = null,
) {

    init {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                isAtLeastSdkExtension21()
        ) {
            @Suppress("UNUSED_VARIABLE") val unused = this.toPlatformExerciseSegment()
        } else {
            require(startTime.isBefore(endTime)) { "startTime must be before endTime." }
            require(repetitions >= 0) { "repetitions can not be negative." }
            weight?.let { require(it.inGrams >= 0) { "weight must be non-negative." } }
            setIndex?.let { require(it >= 0) { "setIndex must be non-negative." } }
            rateOfPerceivedExertion?.let {
                require(it in 0.0..10.0) { "rateOfPerceivedExertion must be in range [0.0, 10.0]." }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExerciseSegment) return false

        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (segmentType != other.segmentType) return false
        if (repetitions != other.repetitions) return false
        if (weight != other.weight) return false
        if (setIndex != other.setIndex) return false
        if (rateOfPerceivedExertion != other.rateOfPerceivedExertion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + segmentType.hashCode()
        result = 31 * result + repetitions.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + setIndex.hashCode()
        result = 31 * result + rateOfPerceivedExertion.hashCode()
        return result
    }

    override fun toString(): String {
        return "ExerciseSegment(startTime=$startTime, endTime=$endTime, segmentType=$segmentType, repetitions=$repetitions, weight=$weight, setIndex=$setIndex, rateOfPerceivedExertion=$rateOfPerceivedExertion)"
    }

    public companion object {
        /**
         * Is a segment type compatible with a session type.
         *
         * <p>For example, a swimming session can contain [EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE]
         * segments, but can't contain [EXERCISE_SEGMENT_TYPE_YOGA] segments.
         *
         * @param segmentType the segment type to be contained within [sessionType].
         * @param sessionType the session type that should contain [segmentType].
         * @return True, if [sessionType] can contain the provided segment, otherwise false.
         */
        @JvmStatic
        public fun isSegmentTypeCompatibleWithSessionType(
            @ExerciseSegmentTypes segmentType: Int,
            @ExerciseTypes sessionType: Int,
        ): Boolean {
            if (UNIVERSAL_SESSION_TYPES.contains(sessionType)) {
                return true
            }
            if (UNIVERSAL_SEGMENTS.contains(segmentType)) {
                return true
            }
            return SESSION_TO_SEGMENTS_MAPPING[sessionType]?.contains(segmentType) ?: false
        }

        /** Next Id: 68. */

        /** Use this type if the type of the exercise segment is not known. */
        public const val EXERCISE_SEGMENT_TYPE_UNKNOWN: Int = 0

        /** Use this type for arm curls. */
        public const val EXERCISE_SEGMENT_TYPE_ARM_CURL: Int = 1

        /** Use this type for back extensions. */
        public const val EXERCISE_SEGMENT_TYPE_BACK_EXTENSION: Int = 2

        /** Use this type for ball slams. */
        public const val EXERCISE_SEGMENT_TYPE_BALL_SLAM: Int = 3

        /** Use this type for barbel shoulder press. */
        public const val EXERCISE_SEGMENT_TYPE_BARBELL_SHOULDER_PRESS: Int = 4

        /** Use this type for bench presses. */
        public const val EXERCISE_SEGMENT_TYPE_BENCH_PRESS: Int = 5

        /** Use this type for bench sit up. */
        public const val EXERCISE_SEGMENT_TYPE_BENCH_SIT_UP: Int = 6

        /** Use this type for biking. */
        public const val EXERCISE_SEGMENT_TYPE_BIKING: Int = 7

        /** Use this type for stationary biking. */
        public const val EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY: Int = 8

        /** Use this type for burpees. */
        public const val EXERCISE_SEGMENT_TYPE_BURPEE: Int = 9

        /** Use this type for crunches. */
        public const val EXERCISE_SEGMENT_TYPE_CRUNCH: Int = 10

        /** Use this type for deadlifts. */
        public const val EXERCISE_SEGMENT_TYPE_DEADLIFT: Int = 11

        /** Use this type for double arms triceps extensions. */
        public const val EXERCISE_SEGMENT_TYPE_DOUBLE_ARM_TRICEPS_EXTENSION: Int = 12

        /** Use this type for left arm dumbbell curl. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_LEFT_ARM: Int = 13

        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_CURL_RIGHT_ARM: Int = 14

        /** Use this type for right arm dumbbell curl. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_FRONT_RAISE: Int = 15

        /** Use this type for dumbbell lateral raises. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_LATERAL_RAISE: Int = 16

        /** Use this type for dumbbells rows. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_ROW: Int = 17

        /** Use this type for left arm triceps extensions. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_LEFT_ARM: Int = 18

        /** Use this type for right arm triceps extensions. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_RIGHT_ARM: Int = 19

        /** Use this type for two arms triceps extensions. */
        public const val EXERCISE_SEGMENT_TYPE_DUMBBELL_TRICEPS_EXTENSION_TWO_ARM: Int = 20

        /** Use this type for elliptical workout. */
        public const val EXERCISE_SEGMENT_TYPE_ELLIPTICAL: Int = 21

        /** Use this type for forward twists. */
        public const val EXERCISE_SEGMENT_TYPE_FORWARD_TWIST: Int = 22

        /** Use this type for front raises. */
        public const val EXERCISE_SEGMENT_TYPE_FRONT_RAISE: Int = 23

        /** Use this type for high intensity training. */
        public const val EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING: Int = 24

        /** Use this type for hip thrusts. */
        public const val EXERCISE_SEGMENT_TYPE_HIP_THRUST: Int = 25

        /** Use this type for hula-hoops. */
        public const val EXERCISE_SEGMENT_TYPE_HULA_HOOP: Int = 26

        /** Use this type for jumping jacks. */
        public const val EXERCISE_SEGMENT_TYPE_JUMPING_JACK: Int = 27

        /** Use this type for jump rope. */
        public const val EXERCISE_SEGMENT_TYPE_JUMP_ROPE: Int = 28

        /** Use this type for kettlebell swings. */
        public const val EXERCISE_SEGMENT_TYPE_KETTLEBELL_SWING: Int = 29

        /** Use this type for lateral raises. */
        public const val EXERCISE_SEGMENT_TYPE_LATERAL_RAISE: Int = 30

        /** Use this type for lat pull-downs. */
        public const val EXERCISE_SEGMENT_TYPE_LAT_PULL_DOWN: Int = 31

        /** Use this type for leg curls. */
        public const val EXERCISE_SEGMENT_TYPE_LEG_CURL: Int = 32

        /** Use this type for leg extensions. */
        public const val EXERCISE_SEGMENT_TYPE_LEG_EXTENSION: Int = 33

        /** Use this type for leg presses. */
        public const val EXERCISE_SEGMENT_TYPE_LEG_PRESS: Int = 34

        /** Use this type for leg raises. */
        public const val EXERCISE_SEGMENT_TYPE_LEG_RAISE: Int = 35

        /** Use this type for lunges. */
        public const val EXERCISE_SEGMENT_TYPE_LUNGE: Int = 36

        /** Use this type for mountain climber. */
        public const val EXERCISE_SEGMENT_TYPE_MOUNTAIN_CLIMBER: Int = 37

        /** Use this type for other workout. */
        public const val EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT: Int = 38

        /** Use this type for the pause. */
        public const val EXERCISE_SEGMENT_TYPE_PAUSE: Int = 39

        /** Use this type for pilates. */
        public const val EXERCISE_SEGMENT_TYPE_PILATES: Int = 40

        /** Use this type for plank. */
        public const val EXERCISE_SEGMENT_TYPE_PLANK: Int = 41

        /** Use this type for pull-ups. */
        public const val EXERCISE_SEGMENT_TYPE_PULL_UP: Int = 42

        /** Use this type for punches. */
        public const val EXERCISE_SEGMENT_TYPE_PUNCH: Int = 43

        /** Use this type for the rest. */
        public const val EXERCISE_SEGMENT_TYPE_REST: Int = 44

        /** Use this type for rowing machine workout. */
        public const val EXERCISE_SEGMENT_TYPE_ROWING_MACHINE: Int = 45

        /** Use this type for running. */
        public const val EXERCISE_SEGMENT_TYPE_RUNNING: Int = 46

        /** Use this type for treadmill running. */
        public const val EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL: Int = 47

        public const val EXERCISE_SEGMENT_TYPE_SHOULDER_PRESS: Int = 48

        /** Use this type for shoulder press. */
        public const val EXERCISE_SEGMENT_TYPE_SINGLE_ARM_TRICEPS_EXTENSION: Int = 49

        /** Use this type for sit-ups. */
        public const val EXERCISE_SEGMENT_TYPE_SIT_UP: Int = 50

        /** Use this type for squats. */
        public const val EXERCISE_SEGMENT_TYPE_SQUAT: Int = 51

        /** Use this type for stair climbing. */
        public const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING: Int = 52

        /** Use this type for stair climbing machine. */
        public const val EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE: Int = 53

        /** Use this type for stretching. */
        public const val EXERCISE_SEGMENT_TYPE_STRETCHING: Int = 54

        /** Use this type for backstroke swimming. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE: Int = 55

        /** Use this type for breaststroke swimming. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE: Int = 56

        /** Use this type for butterfly swimming. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY: Int = 57

        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE: Int = 58

        /** Use this type for mixed swimming. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED: Int = 59

        /** Use this type for swimming in open water. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER: Int = 60

        /** Use this type if other swimming styles are not suitable. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER: Int = 61

        /** Use this type for swimming in the pool. */
        public const val EXERCISE_SEGMENT_TYPE_SWIMMING_POOL: Int = 62

        /** Use this type for upper twists. */
        public const val EXERCISE_SEGMENT_TYPE_UPPER_TWIST: Int = 63

        /** Use this type for walking. */
        public const val EXERCISE_SEGMENT_TYPE_WALKING: Int = 64

        /** Use this type for weightlifting. */
        public const val EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING: Int = 65

        /** Use this type for wheelchair. */
        public const val EXERCISE_SEGMENT_TYPE_WHEELCHAIR: Int = 66

        /** Use this type for yoga. */
        public const val EXERCISE_SEGMENT_TYPE_YOGA: Int = 67

        internal val UNIVERSAL_SESSION_TYPES =
            setOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BOOT_CAMP,
                ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            )

        internal val UNIVERSAL_SEGMENTS =
            setOf(
                EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                EXERCISE_SEGMENT_TYPE_PAUSE,
                EXERCISE_SEGMENT_TYPE_REST,
                EXERCISE_SEGMENT_TYPE_STRETCHING,
                EXERCISE_SEGMENT_TYPE_UNKNOWN,
            )

        internal val EXERCISE_SEGMENTS =
            setOf(
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
                EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
            )
        internal val SWIMMING_SEGMENTS =
            setOf(
                EXERCISE_SEGMENT_TYPE_SWIMMING_BACKSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE,
                EXERCISE_SEGMENT_TYPE_SWIMMING_BUTTERFLY,
                EXERCISE_SEGMENT_TYPE_SWIMMING_MIXED,
                EXERCISE_SEGMENT_TYPE_SWIMMING_OTHER,
            )

        private val SESSION_TO_SEGMENTS_MAPPING =
            mapOf(
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING to setOf(EXERCISE_SEGMENT_TYPE_BIKING),
                ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to
                    setOf(EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY),
                ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to
                    setOf(EXERCISE_SEGMENT_TYPE_ELLIPTICAL),
                ExerciseSessionRecord.EXERCISE_TYPE_EXERCISE_CLASS to
                    setOf(
                        EXERCISE_SEGMENT_TYPE_YOGA,
                        EXERCISE_SEGMENT_TYPE_BIKING_STATIONARY,
                        EXERCISE_SEGMENT_TYPE_PILATES,
                        EXERCISE_SEGMENT_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING,
                    ),
                ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_HIKING to
                    setOf(EXERCISE_SEGMENT_TYPE_WALKING, EXERCISE_SEGMENT_TYPE_WHEELCHAIR),
                ExerciseSessionRecord.EXERCISE_TYPE_PILATES to setOf(EXERCISE_SEGMENT_TYPE_PILATES),
                ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to
                    setOf(EXERCISE_SEGMENT_TYPE_ROWING_MACHINE),
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to
                    setOf(EXERCISE_SEGMENT_TYPE_RUNNING, EXERCISE_SEGMENT_TYPE_WALKING),
                ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to
                    setOf(EXERCISE_SEGMENT_TYPE_RUNNING_TREADMILL),
                ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to
                    setOf(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING),
                ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to
                    setOf(EXERCISE_SEGMENT_TYPE_STAIR_CLIMBING_MACHINE),
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to
                    buildSet {
                        add(EXERCISE_SEGMENT_TYPE_SWIMMING_OPEN_WATER)
                        addAll(SWIMMING_SEGMENTS)
                    },
                ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to
                    buildSet {
                        add(EXERCISE_SEGMENT_TYPE_SWIMMING_POOL)
                        addAll(SWIMMING_SEGMENTS)
                    },
                ExerciseSessionRecord.EXERCISE_TYPE_WALKING to setOf(EXERCISE_SEGMENT_TYPE_WALKING),
                ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR to
                    setOf(EXERCISE_SEGMENT_TYPE_WHEELCHAIR),
                ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING to EXERCISE_SEGMENTS,
                ExerciseSessionRecord.EXERCISE_TYPE_YOGA to setOf(EXERCISE_SEGMENT_TYPE_YOGA),
            )

        /** List of supported segment types on Health Platform. */
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
        public annotation class ExerciseSegmentTypes
    }
}
