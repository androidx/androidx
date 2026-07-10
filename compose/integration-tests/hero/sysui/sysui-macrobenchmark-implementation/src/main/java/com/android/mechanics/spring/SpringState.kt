/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.mechanics.spring

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.jvm.JvmInline
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Describes the motion state of a spring.
 *
 * @see calculateUpdatedState to simulate the springs movement
 * @see SpringState function to create this value.
 */
@JvmInline
value class SpringState(val packedValue: Long) {
    val displacement: Float
        get() = unpackFloat1(packedValue)

    val velocity: Float
        get() = unpackFloat2(packedValue)

    /**
     * Whether the state is considered stable.
     *
     * The amplitude of the remaining movement, for a spring with [parameters] is less than
     * [stableThreshold]
     */
    fun isStable(parameters: SpringParameters, stableThreshold: Float): Boolean {
        if (this == AtRest) return true
        val currentEnergy = parameters.stiffness * displacement * displacement + velocity * velocity
        val maxStableEnergy = parameters.stiffness * stableThreshold * stableThreshold
        return currentEnergy <= maxStableEnergy
    }

    /** Adds the specified [displacementDelta] and [velocityDelta] to the returned state. */
    fun nudge(displacementDelta: Float = 0f, velocityDelta: Float = 0f): SpringState {
        return SpringState(displacement + displacementDelta, velocity + velocityDelta)
    }

    override fun toString(): String {
        return "MechanicsSpringState(displacement=$displacement, velocity=$velocity)"
    }

    companion object {
        /** Spring at rest. */
        val AtRest = SpringState(displacement = 0f, velocity = 0f)
    }
}

/** Creates a [SpringState] given [displacement] and [velocity] */
fun SpringState(displacement: Float, velocity: Float = 0f) =
    SpringState(packFloats(displacement, velocity))

/**
 * Computes the updated [SpringState], after letting the spring with the specified [parameters]
 * settle for [elapsedNanos].
 *
 * This implementation is based on Compose's [SpringSimulation].
 */
fun SpringState.calculateUpdatedState(
    elapsedNanos: Long,
    parameters: SpringParameters,
): SpringState {
    if (parameters.isSnapSpring || this == SpringState.AtRest) {
        return SpringState.AtRest
    }

    val stiffness = parameters.stiffness.toDouble()
    val naturalFreq = sqrt(stiffness)

    val dampingRatio = parameters.dampingRatio
    val displacement = displacement
    val velocity = velocity
    val deltaT = elapsedNanos / 1_000_000_000.0 // unit: seconds
    val dampingRatioSquared = dampingRatio * dampingRatio.toDouble()
    val r = -dampingRatio * naturalFreq

    val currentDisplacement: Double
    val currentVelocity: Double

    if (dampingRatio > 1) {
        // Over damping
        val s = naturalFreq * sqrt(dampingRatioSquared - 1)
        val gammaPlus = r + s
        val gammaMinus = r - s

        val coeffB = (gammaMinus * displacement - velocity) / (gammaMinus - gammaPlus)
        val coeffA = displacement - coeffB
        currentDisplacement = (coeffA * exp(gammaMinus * deltaT) + coeffB * exp(gammaPlus * deltaT))
        currentVelocity =
            (coeffA * gammaMinus * exp(gammaMinus * deltaT) +
                coeffB * gammaPlus * exp(gammaPlus * deltaT))
    } else if (dampingRatio == 1.0f) {
        // Critically damped
        val coeffA = displacement
        val coeffB = velocity + naturalFreq * displacement
        val nFdT = -naturalFreq * deltaT
        currentDisplacement = (coeffA + coeffB * deltaT) * exp(nFdT)
        currentVelocity =
            (((coeffA + coeffB * deltaT) * exp(nFdT) * (-naturalFreq)) + coeffB * exp(nFdT))
    } else {
        // Underdamped
        val dampedFreq = naturalFreq * sqrt(1 - dampingRatioSquared)
        val cosCoeff = displacement
        val sinCoeff = ((1 / dampedFreq) * (((-r * displacement) + velocity)))
        val dFdT = dampedFreq * deltaT
        currentDisplacement = (exp(r * deltaT) * ((cosCoeff * cos(dFdT) + sinCoeff * sin(dFdT))))
        currentVelocity =
            (currentDisplacement * r +
                (exp(r * deltaT) *
                    ((-dampedFreq * cosCoeff * sin(dFdT) + dampedFreq * sinCoeff * cos(dFdT)))))
    }

    return SpringState(currentDisplacement.toFloat(), currentVelocity.toFloat())
}
