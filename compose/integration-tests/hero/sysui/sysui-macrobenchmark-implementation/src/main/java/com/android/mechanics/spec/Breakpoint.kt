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

package com.android.mechanics.spec

import androidx.compose.ui.util.fastIsFinite
import com.android.mechanics.haptics.BreakpointHaptics
import com.android.mechanics.spring.SpringParameters

/**
 * Key to identify a breakpoint in a [DirectionalMotionSpec].
 *
 * @param debugLabel name of the breakpoint, for tooling and debugging.
 * @param identity is used to check the equality of two key instances.
 */
class BreakpointKey(val debugLabel: String? = null, val identity: Any = Any()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BreakpointKey) return false

        return identity == other.identity
    }

    override fun hashCode(): Int {
        return identity.hashCode()
    }

    override fun toString(): String {
        return "BreakpointKey(${debugLabel ?: ""}" +
            "@${identity.hashCode().toString(16).padStart(8,'0')})"
    }

    internal companion object {
        val MinLimit = BreakpointKey("built-in::min")
        val MaxLimit = BreakpointKey("built-in::max")
    }
}

/**
 * Specification of a breakpoint, in the context of a [DirectionalMotionSpec].
 *
 * The [spring] and [guarantee] define the physics animation for the discontinuity at this
 * breakpoint.They are applied in the direction of the containing [DirectionalMotionSpec].
 *
 * This [Breakpoint]'s animation definition is valid while the input is within the next segment. If
 * the animation is still in progress when the input value reaches the next breakpoint, the
 * remaining animation will be blended with the animation starting at the next breakpoint.
 *
 * @param key Identity of the [Breakpoint], unique within a [DirectionalMotionSpec].
 * @param position The position of the [Breakpoint], in the domain of the `MotionValue`'s input.
 * @param spring Parameters of the spring used to animate the breakpoints discontinuity.
 * @param guarantee Optional constraints to accelerate the completion of the spring motion, based on
 *   `MotionValue`'s input or other non-time signals.
 * @param breakpointHaptics A description of haptics when the input crosses this breakpoint.
 */
data class Breakpoint(
    val key: BreakpointKey,
    val position: Float,
    val spring: SpringParameters,
    val guarantee: Guarantee,
    val breakpointHaptics: BreakpointHaptics = BreakpointHaptics.None,
) : Comparable<Breakpoint> {

    init {
        when (key) {
            BreakpointKey.MinLimit -> require(position == Float.NEGATIVE_INFINITY)
            BreakpointKey.MaxLimit -> require(position == Float.POSITIVE_INFINITY)
            else -> require(position.fastIsFinite())
        }
    }

    companion object {
        /** First breakpoint of each spec. */
        val minLimit =
            Breakpoint(
                BreakpointKey.MinLimit,
                Float.NEGATIVE_INFINITY,
                SpringParameters.Snap,
                Guarantee.None,
                BreakpointHaptics.None,
            )

        /** Last breakpoint of each spec. */
        val maxLimit =
            Breakpoint(
                BreakpointKey.MaxLimit,
                Float.POSITIVE_INFINITY,
                SpringParameters.Snap,
                Guarantee.None,
                BreakpointHaptics.None,
            )

        internal fun create(
            breakpointKey: BreakpointKey,
            breakpointPosition: Float,
            springSpec: SpringParameters,
            guarantee: Guarantee,
            breakpointHaptics: BreakpointHaptics,
        ): Breakpoint {
            return when (breakpointKey) {
                BreakpointKey.MinLimit -> minLimit
                BreakpointKey.MaxLimit -> maxLimit
                else ->
                    Breakpoint(
                        breakpointKey,
                        breakpointPosition,
                        springSpec,
                        guarantee,
                        breakpointHaptics,
                    )
            }
        }
    }

    override fun compareTo(other: Breakpoint): Int {
        return position.compareTo(other.position)
    }
}
