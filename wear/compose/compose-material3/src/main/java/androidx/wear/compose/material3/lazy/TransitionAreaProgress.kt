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

package androidx.wear.compose.material3.lazy

import androidx.compose.animation.core.Easing
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.foundation.lazy.inverseLerp

/**
 * Class that represents where in the transition areas a given item is. This can be either in the
 * top transition area, the bottom transition area, or neither.
 */
@JvmInline
internal value class TransitionAreaProgress(private val encodedProgress: Float) {
    // encodedProgress is, going from top to bottom:
    // -1 to 0 for top transition
    // 0 in the center of the screen, no transition
    // 0 to 1 for bottom transition.

    /** Are we in the top transition area */
    val isInTopTransitionArea: Boolean
        get() = encodedProgress < 0

    /**
     * How far into the transition area we are. 0 = item is entering the screen, 1 = item is
     * exiting/outside the transition area.
     */
    val progress: Float
        get() = if (encodedProgress < 0) encodedProgress + 1 else 1 - encodedProgress

    /**
     * Compute the value the given variable will have, given the current progress on the
     * transformation zone and the easing to apply.
     */
    fun compute(variable: TransformationVariableSpec, easing: Easing): Float {
        val edgeValue = if (isInTopTransitionArea) variable.topValue else variable.bottomValue
        val transformationZoneProgress =
            inverseLerp(
                variable.transformationZoneEnterFraction,
                variable.transformationZoneExitFraction,
                progress,
            )
        return lerp(edgeValue, 1f, easing.transform(transformationZoneProgress))
    }

    companion object {
        /** We are not in a transition area */
        val None = TransitionAreaProgress(0f)

        /**
         * We are in the top transition area, progress is 0 for an item entering the screen, up to 1
         * for an item exiting this transition area.
         */
        fun Top(progress: Float) = TransitionAreaProgress((progress - 1f).coerceAtMost(0f))

        /**
         * We are in the botom transition area, progress is 0 for an item entering the screen, up to
         * 1 for an item exiting this transition area.
         */
        fun Bottom(progress: Float) = TransitionAreaProgress((1f - progress).coerceAtLeast(0f))
    }
}

// TODO: Decide what we want to compute & store vs compute when needed.
@JvmInline
internal value class TransformationState internal constructor(private val packedValue: Long) {
    val scale: Float
        get() = unpackFloat1(packedValue)

    val containerAlpha: Float
        get() = unpackFloat2(packedValue)

    constructor(scale: Float, containerAlpha: Float) : this(packFloats(scale, containerAlpha))

    companion object {
        /**
         * Represents an unspecified [TransformingLazyColumnItemScrollProgress] value, usually a
         * replacement for `null` when a primitive value is desired.
         */
        val Unspecified: TransformationState = TransformationState(UnspecifiedPackedFloats)
    }
}

internal const val UnspecifiedPackedFloats = 0x7fc00000_7fc00000L // NaN_NaN
