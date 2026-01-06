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

import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import kotlin.jvm.JvmInline
import kotlin.math.pow

/**
 * Describes the parameters of a spring.
 *
 * Note: This is conceptually compatible with the Compose [SpringSpec]. In contrast to the compose
 * implementation, these [SpringParameters] are intended to be continuously updated.
 *
 * @see SpringParameters function to create this value.
 */
@JvmInline
value class SpringParameters(val packedValue: Long) {
    val stiffness: Float
        get() = unpackFloat1(packedValue)

    val dampingRatio: Float
        get() = unpackFloat2(packedValue)

    /** Whether the spring is expected to immediately end movement. */
    val isSnapSpring: Boolean
        get() = stiffness >= snapStiffness && dampingRatio == snapDamping

    override fun toString(): String {
        return "MechanicsSpringSpec(stiffness=$stiffness, dampingRatio=$dampingRatio)"
    }

    companion object {
        private val snapStiffness = 100_000f
        private val snapDamping = 1f

        /** A spring so stiff it completes the motion almost immediately. */
        val Snap = SpringParameters(snapStiffness, snapDamping)
    }
}

/** Creates a [SpringParameters] with the given [stiffness] and [dampingRatio]. */
fun SpringParameters(stiffness: Float, dampingRatio: Float): SpringParameters {
    require(stiffness > 0) { "Spring stiffness constant must be positive." }
    require(dampingRatio >= 0) { "Spring damping constant must be positive." }
    return SpringParameters(packFloats(stiffness, dampingRatio))
}

/**
 * Return interpolated [SpringParameters], based on the [fraction] between [start] and [stop].
 *
 * The [SpringParameters.dampingRatio] is interpolated linearly, the [SpringParameters.stiffness] is
 * interpolated logarithmically.
 *
 * The [fraction] is clamped to a `0..1` range.
 */
fun lerp(start: SpringParameters, stop: SpringParameters, fraction: Float): SpringParameters {
    val f = fraction.fastCoerceIn(0f, 1f)
    val stiffness = start.stiffness.pow(1 - f) * stop.stiffness.pow(f)
    val dampingRatio = lerp(start.dampingRatio, stop.dampingRatio, f)
    return SpringParameters(packFloats(stiffness, dampingRatio))
}
