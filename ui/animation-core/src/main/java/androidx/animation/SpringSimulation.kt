/**
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

/**
 * Spring Simulation simulates spring physics, and allows you to query the motion (i.e. value and
 * velocity) at certain time in the future based on the starting velocity and value.
 *
 * By configuring the stiffness and damping ratio, callers can create a spring with the look and
 * feel suits their use case. Stiffness corresponds to the spring constant. The stiffer the spring
 * is, the harder it is to stretch it, the faster it undergoes dampening.
 *
 *
 * Spring damping ratio describes how oscillations in a system decay after a disturbance.
 * When damping ratio > 1* (i.e. over-damped), the object will quickly return to the rest position
 * without overshooting. If damping ratio equals to 1 (i.e. critically damped), the object will
 * return to equilibrium within the shortest amount of time. When damping ratio is less than 1
 * (i.e. under-damped), the mass tends to overshoot, and return, and overshoot again. Without any
 * damping (i.e. damping ratio = 0), the mass will oscillate forever.
 */

// TODO(tianliu): make this inline when inline classes are supported
internal data class Motion(val value: Float, val velocity: Float)

// This multiplier is used to calculate the velocity threshold given a certain value threshold.
// The idea is that if it takes >= 1 frame to move the value threshold amount, then the velocity
// is a reasonable threshold.
private const val VelocityThresholdMultiplier = 1000.0 / 16.0

// Value to indicate an unset state.
internal val UNSET = Float.MAX_VALUE

internal class SpringSimulation(var finalPosition: Float) {

    // Natural frequency
    private var naturalFreq = Math.sqrt(Physics.StiffnessVeryLow.toDouble())

    // Indicates whether the spring has been initialized
    private var initialized = false

    // Threshold for velocity and value to determine when it's reasonable to assume that the spring
    // is approximately at rest.
    private var valueThreshold: Double = 0.005
    private var velocityThreshold: Double = 0.3125

    // Intermediate values to simplify the spring function calculation per frame.
    private var gammaPlus: Double = 0.0
    private var gammaMinus: Double = 0.0
    private var dampedFreq: Double = 0.0

    /**
     * Stiffness of the spring.
     */
    var stiffness: Float
        set(value) {
            if (stiffness <= 0) {
                throw IllegalArgumentException("Spring stiffness constant must be positive.")
            }
            naturalFreq = Math.sqrt(value.toDouble())
            // All the intermediate values need to be recalculated.
            initialized = false
        }
        get() {
            return (naturalFreq * naturalFreq).toFloat()
        }

    /**
     * Returns the damping ratio of the spring.
     *
     * @return damping ratio of the spring
     */
    var dampingRatio: Float = Physics.DampingRatioNoBouncy
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Damping ratio must be non-negative")
            }
            field = value
            // All the intermediate values need to be recalculated.
            initialized = false
        }

    /*********************** Below are private APIs  */

    fun getAcceleration(lastDisplacement: Float, lastVelocity: Float): Float {
        val adjustedDisplacement = lastDisplacement - finalPosition

        val k = naturalFreq * naturalFreq
        val c = 2.0 * naturalFreq * dampingRatio

        return (-k * adjustedDisplacement - c * lastVelocity).toFloat()
    }

    fun isAtEquilibrium(value: Float, velocity: Float, timeElapsed: Long = 0): Boolean {
        if (timeElapsed > 0) {
            val (endValue, endVelocity) = updateValues(value, velocity, timeElapsed)
            return isAtEquilibrium(endValue, endVelocity)
        } else {
            return isAtEquilibrium(value, velocity)
        }
    }

    private fun isAtEquilibrium(value: Float, velocity: Float): Boolean {

        if ((Math.abs(velocity) < velocityThreshold &&
                    Math.abs(value - finalPosition) < valueThreshold)) {
            return true
        }
        return false
    }

    /**
     * Initialize the string by doing the necessary pre-calculation as well as some sanity check
     * on the setup.
     *
     * @throws IllegalStateException if the final position is not yet set by the time the spring
     * animation has started
     */
    private fun init() {
        if (initialized) {
            return
        }

        if (finalPosition == UNSET) {
            throw IllegalStateException(
                "Error: Final position of the spring must be set before the animation starts")
        }

        val dampingRatioSquared = dampingRatio * dampingRatio.toDouble()
        if (dampingRatio > 1) {
            // Over damping
            gammaPlus =
                (-dampingRatio * naturalFreq + naturalFreq * Math.sqrt(dampingRatioSquared - 1))
            gammaMinus =
                (-dampingRatio * naturalFreq - naturalFreq * Math.sqrt(dampingRatioSquared - 1))
        } else if (dampingRatio >= 0 && dampingRatio < 1) {
            // Under damping
            dampedFreq = naturalFreq * Math.sqrt(1 - dampingRatioSquared)
        }

        initialized = true
    }

    /**
     * Internal only call for Spring to calculate the spring position/velocity using
     * an analytical approach.
     */
    internal fun updateValues(lastDisplacement: Float, lastVelocity: Float, timeElapsed: Long):
            Motion {
        init()

        val adjustedDisplacement = lastDisplacement - finalPosition
        val deltaT = timeElapsed / 1000.0 // unit: seconds
        val displacement: Double
        val currentVelocity: Double
        if (dampingRatio > 1) {
            // Overdamped
            val coeffA =
                (adjustedDisplacement - ((gammaMinus * adjustedDisplacement - lastVelocity) /
                    (gammaMinus - gammaPlus)))
            val coeffB = ((gammaMinus * adjustedDisplacement - lastVelocity) /
                    (gammaMinus - gammaPlus))
            displacement = (coeffA * Math.pow(Math.E, gammaMinus * deltaT) +
                    coeffB * Math.pow(Math.E, gammaPlus * deltaT))
            currentVelocity = (coeffA * gammaMinus * Math.pow(Math.E, gammaMinus * deltaT) +
                    coeffB * gammaPlus * Math.pow(Math.E, gammaPlus * deltaT))
        } else if (dampingRatio == 1.0f) {
            // Critically damped
            val coeffA = adjustedDisplacement
            val coeffB = lastVelocity + naturalFreq * adjustedDisplacement
            displacement = (coeffA + coeffB * deltaT) * Math.pow(Math.E, -naturalFreq * deltaT)
            currentVelocity =
                    (((coeffA + coeffB * deltaT) * Math.pow(Math.E, -naturalFreq * deltaT) *
                    (-naturalFreq)) + coeffB * Math.pow(Math.E, -naturalFreq * deltaT))
        } else {
            // Underdamped
            val cosCoeff = adjustedDisplacement
            val sinCoeff =
                ((1 / dampedFreq) * (((dampingRatio * naturalFreq * adjustedDisplacement) +
                    lastVelocity)))
            displacement = (Math.pow(Math.E, -dampingRatio * naturalFreq * deltaT) *
                    ((cosCoeff * Math.cos(dampedFreq * deltaT) +
                            sinCoeff * Math.sin(dampedFreq * deltaT))))
            currentVelocity = (displacement * (-naturalFreq) * dampingRatio + (Math.pow(Math.E,
                    - dampingRatio * naturalFreq * deltaT) * ((-dampedFreq * cosCoeff *
                    Math.sin(dampedFreq * deltaT) + dampedFreq * sinCoeff *
                    Math.cos(dampedFreq * deltaT)))))
        }

        var newValue = (displacement + finalPosition).toFloat()
        var newVelocity = currentVelocity.toFloat()
        if (isAtEquilibrium(newValue, newVelocity)) {
            newValue = finalPosition
            newVelocity = 0f
        }

        return Motion(newValue, newVelocity)
    }

    /**
     * This threshold defines how close the animation value needs to be before the animation can
     * finish. This default value is based on the property being animated, e.g. animations on alpha,
     * scale, translation or rotation would have different thresholds. This value should be small
     * enough to avoid visual glitch of "jumping to the end". But it shouldn't be so small that
     * animations take seconds to finish.
     *
     * @param threshold the difference between the animation value and final spring position that
     * is allowed to end the animation when velocity is very low
     */
    internal fun setValueThreshold(threshold: Double) {
        valueThreshold = Math.abs(threshold)
        velocityThreshold = valueThreshold * VelocityThresholdMultiplier
    }
}
