/*
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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.animation.core

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

@kotlin.jvm.JvmInline
internal value class Motion(val packedValue: Long) {
    inline val value: Float
        get() = unpackFloat1(packedValue)

    inline val velocity: Float
        get() = unpackFloat2(packedValue)
}

internal inline fun Motion(value: Float, velocity: Float) = Motion(packFloats(value, velocity))

/**
 * Spring Simulation simulates spring physics, and allows you to query the motion (i.e. value and
 * velocity) at certain time in the future based on the starting velocity and value.
 *
 * By configuring the stiffness and damping ratio, callers can create a spring with the look and
 * feel suits their use case. Stiffness corresponds to the spring constant. The stiffer the spring
 * is, the harder it is to stretch it, the faster it undergoes dampening.
 *
 * Spring damping ratio describes how oscillations in a system decay after a disturbance. When
 * damping ratio > 1* (i.e. over-damped), the object will quickly return to the rest position
 * without overshooting. If damping ratio equals to 1 (i.e. critically damped), the object will
 * return to equilibrium within the shortest amount of time. When damping ratio is less than 1 (i.e.
 * under-damped), the mass tends to overshoot, and return, and overshoot again. Without any damping
 * (i.e. damping ratio = 0), the mass will oscillate forever.
 */
internal class SpringSimulation(var finalPosition: Float) {
    // Natural frequency
    private var naturalFreq = sqrt(Spring.StiffnessVeryLow.toDouble())

    /** Stiffness of the spring. */
    var stiffness: Float
        set(value) {
            if (stiffness <= 0) {
                throwIllegalArgumentException("Spring stiffness constant must be positive.")
            }
            naturalFreq = sqrt(value.toDouble())
        }
        get() {
            return (naturalFreq * naturalFreq).toFloat()
        }

    /**
     * Returns the damping ratio of the spring.
     *
     * @return damping ratio of the spring
     */
    var dampingRatio: Float = Spring.DampingRatioNoBouncy
        set(value) {
            if (value < 0) {
                throwIllegalArgumentException("Damping ratio must be non-negative")
            }
            field = value
        }

    /** ********************* Below are private APIs */
    fun getAcceleration(lastDisplacement: Float, lastVelocity: Float): Float {
        val adjustedDisplacement = lastDisplacement - finalPosition

        val k = naturalFreq * naturalFreq
        val c = 2.0 * naturalFreq * dampingRatio

        return (-k * adjustedDisplacement - c * lastVelocity).toFloat()
    }

    /**
     * Internal only call for Spring to calculate the spring position/velocity using an analytical
     * approach.
     */
    internal fun updateValues(
        lastDisplacement: Float,
        lastVelocity: Float,
        timeElapsed: Long
    ): Motion {
        val adjustedDisplacement = lastDisplacement - finalPosition
        val deltaT = timeElapsed / 1000.0 // unit: seconds
        val dampingRatioSquared = dampingRatio * dampingRatio.toDouble()
        val r = -dampingRatio * naturalFreq

        val displacement: Double
        val currentVelocity: Double

        if (dampingRatio > 1) {
            // Over damping
            val s = naturalFreq * sqrt(dampingRatioSquared - 1)
            val gammaPlus = r + s
            val gammaMinus = r - s

            // Overdamped
            val coeffB =
                (gammaMinus * adjustedDisplacement - lastVelocity) / (gammaMinus - gammaPlus)
            val coeffA = adjustedDisplacement - coeffB
            displacement = (coeffA * exp(gammaMinus * deltaT) + coeffB * exp(gammaPlus * deltaT))
            currentVelocity =
                (coeffA * gammaMinus * exp(gammaMinus * deltaT) +
                    coeffB * gammaPlus * exp(gammaPlus * deltaT))
        } else if (dampingRatio == 1.0f) {
            // Critically damped
            val coeffA = adjustedDisplacement
            val coeffB = lastVelocity + naturalFreq * adjustedDisplacement
            val nFdT = -naturalFreq * deltaT
            displacement = (coeffA + coeffB * deltaT) * exp(nFdT)
            currentVelocity =
                (((coeffA + coeffB * deltaT) * exp(nFdT) * (-naturalFreq)) + coeffB * exp(nFdT))
        } else {
            val dampedFreq = naturalFreq * sqrt(1 - dampingRatioSquared)
            // Underdamped
            val cosCoeff = adjustedDisplacement
            val sinCoeff = ((1 / dampedFreq) * (((-r * adjustedDisplacement) + lastVelocity)))
            val dFdT = dampedFreq * deltaT
            displacement = (exp(r * deltaT) * ((cosCoeff * cos(dFdT) + sinCoeff * sin(dFdT))))
            currentVelocity =
                (displacement * r +
                    (exp(r * deltaT) *
                        ((-dampedFreq * cosCoeff * sin(dFdT) + dampedFreq * sinCoeff * cos(dFdT)))))
        }

        val newValue = (displacement + finalPosition).toFloat()
        val newVelocity = currentVelocity.toFloat()

        return Motion(newValue, newVelocity)
    }
}
