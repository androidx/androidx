/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Velocity

/** An ongoing target that should be met during a [PlannedExerciseStep]. */
abstract class ExercisePerformanceTarget internal constructor() {
    /**
     * An [ExercisePerformanceTarget] that requires a target power range to be met during the
     * associated [PlannedExerciseStep].
     */
    class PowerTarget(
        val minPower: Power,
        val maxPower: Power,
    ) : ExercisePerformanceTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PowerTarget) return false

            if (minPower != other.minPower) return false
            if (maxPower != other.maxPower) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minPower.hashCode()
            result = 31 * result + maxPower.hashCode()
            return result
        }

        override fun toString(): String {
            return "PowerTarget(minPower=$minPower, maxPower=$maxPower)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires a target speed range to be met during the
     * associated [PlannedExerciseStep].
     */
    class SpeedTarget(
        val minSpeed: Velocity,
        val maxSpeed: Velocity,
    ) : ExercisePerformanceTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SpeedTarget) return false

            if (minSpeed != other.minSpeed) return false
            if (maxSpeed != other.maxSpeed) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minSpeed.hashCode()
            result = 31 * result + maxSpeed.hashCode()
            return result
        }

        override fun toString(): String {
            return "SpeedTarget(minSpeed=$minSpeed, maxSpeed=$maxSpeed)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires a target cadence range to be met during the
     * associated [PlannedExerciseStep].The value may be interpreted as RPM for e.g. cycling
     * activities, or as steps per minute for e.g. walking/running activities.
     */
    class CadenceTarget(
        val minCadence: Double,
        val maxCadence: Double,
    ) : ExercisePerformanceTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CadenceTarget) return false

            if (minCadence != other.minCadence) return false
            if (maxCadence != other.maxCadence) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minCadence.hashCode()
            result = 31 * result + maxCadence.hashCode()
            return result
        }

        override fun toString(): String {
            return "CadenceTarget(minCadence=$minCadence, maxCadence=$maxCadence)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires a target heart rate range, in BPM, to be met
     * during the associated {@link PlannedExerciseStep}.
     */
    class HeartRateTarget(
        val minHeartRate: Double,
        val maxHeartRate: Double,
    ) : ExercisePerformanceTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HeartRateTarget) return false

            if (minHeartRate != other.minHeartRate) return false
            if (maxHeartRate != other.maxHeartRate) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minHeartRate.hashCode()
            result = 31 * result + maxHeartRate.hashCode()
            return result
        }

        override fun toString(): String {
            return "HeartRateTarget(minHeartRate=$minHeartRate, maxHeartRate=$maxHeartRate)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires a target weight to be lifted during the
     * associated [PlannedExerciseStep].
     */
    class WeightTarget(
        val mass: Mass,
    ) : ExercisePerformanceTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is WeightTarget) return false

            return mass == other.mass
        }

        override fun hashCode(): Int {
            return mass.hashCode()
        }

        override fun toString(): String {
            return "WeightTarget(mass=$mass)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires a target RPE (rate of perceived exertion) to be
     * met during the associated {@link PlannedExerciseStep}.
     *
     * <p>Values correspond to the Borg CR10 RPE scale and must be in the range 0 to 10 inclusive.
     * 0: No exertion (at rest) 1: Very light 2-3: Light 4-5: Moderate 6-7: Hard 8-9: Very hard 10:
     * Maximum effort
     */
    class RateOfPerceivedExertionTarget(
        val rpe: Int,
    ) : ExercisePerformanceTarget() {
        init {
            require(rpe in 0..10) { "RPE value must be between 0 and 10, inclusive." }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RateOfPerceivedExertionTarget) return false

            return rpe == other.rpe
        }

        override fun hashCode(): Int {
            return rpe.hashCode()
        }

        override fun toString(): String {
            return "RateOfPerceivedExertionTarget(rpe=$rpe)"
        }
    }

    /**
     * An [ExercisePerformanceTarget] that requires completing as many repetitions as possible.
     * AMRAP (as many reps as possible) sets are often used in conjunction with a duration based
     * completion goal.
     */
    object AmrapTarget : ExercisePerformanceTarget() {
        override fun toString(): String {
            return "AmrapTarget()"
        }
    }

    /** An [ExercisePerformanceTarget] that is unknown. */
    object UnknownTarget : ExercisePerformanceTarget() {
        override fun toString(): String {
            return "UnknownTarget()"
        }
    }
}
