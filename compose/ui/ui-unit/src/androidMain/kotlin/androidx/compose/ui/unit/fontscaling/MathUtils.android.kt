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
package androidx.compose.ui.unit.fontscaling

import androidx.annotation.RestrictTo

/**
 * A utility class providing functions useful for common mathematical operations.
 */
// TODO(b/294384826): move these into core:core when the FontScaleConverter APIs are available.
//  These are temporary shims until core and platform are in a stable state.
@RestrictTo(RestrictTo.Scope.LIBRARY)
object MathUtils {
    /**
     * Linearly interpolates the fraction [amount] between [start] and [stop]
     *
     * @param start starting value
     * @param stop ending value
     * @param amount normalized between 0 - 1
     */
    fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }

    /**
     * Inverse of [.lerp]. More precisely, returns the interpolation
     * scalar (s) that satisfies the equation:
     * `value = `[ ][.lerp]`(a, b, s)`
     *
     *
     * If `a == b`, then this function will return 0.
     */
    fun lerpInv(a: Float, b: Float, value: Float): Float {
        return if (a != b) (value - a) / (b - a) else 0.0f
    }

    /**
     * Calculates a value in [rangeMin, rangeMax] that maps value in [valueMin, valueMax] to
     * returnVal in [rangeMin, rangeMax].
     *
     *
     * Always returns a constrained value in the range [rangeMin, rangeMax], even if value is
     * outside [valueMin, valueMax].
     *
     *
     * Eg:
     * constrainedMap(0f, 100f, 0f, 1f, 0.5f) = 50f
     * constrainedMap(20f, 200f, 10f, 20f, 20f) = 200f
     * constrainedMap(20f, 200f, 10f, 20f, 50f) = 200f
     * constrainedMap(10f, 50f, 10f, 20f, 5f) = 10f
     *
     * @param rangeMin minimum of the range that should be returned.
     * @param rangeMax maximum of the range that should be returned.
     * @param valueMin minimum of range to map `value` to.
     * @param valueMax maximum of range to map `value` to.
     * @param value to map to the range [`valueMin`, `valueMax`]. Note, can be outside
     * this range, resulting in a clamped value.
     * @return the mapped value, constrained to [`rangeMin`, `rangeMax`.
     */
    fun constrainedMap(
        rangeMin: Float,
        rangeMax: Float,
        valueMin: Float,
        valueMax: Float,
        value: Float
    ): Float {
        return lerp(
            rangeMin, rangeMax,
            Math.max(0f, Math.min(1f, lerpInv(valueMin, valueMax, value)))
        )
    }
}
