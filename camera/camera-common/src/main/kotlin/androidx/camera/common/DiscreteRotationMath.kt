/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.common

import android.view.Surface

/** Utility functions for working with discrete rotations values [0, 90, 180, 270]. */
public object DiscreteRotationMath {
    /**
     * Throws an [IllegalArgumentException] if the given [degrees] is not one of [0, 90, 180, 270].
     */
    @JvmStatic
    public fun requireDiscreteRotation(degrees: Int) {
        require(degrees == 0 || degrees == 90 || degrees == 180 || degrees == 270) {
            "Unexpected rotation: $degrees. Value must be one of 0, 90, 180, 270"
        }
    }

    /**
     * Round [degrees] to the nearest discrete rotation (0, 90, 180, 270). Negative values are
     * rounded to the nearest positive discrete rotation value.
     *
     * Example(s):
     * - `40 => 0°`
     * - `50 => 90°`
     * - `-40 => 0°` (Equivalent to -40 + 360 => round(320) => 0)
     * - `-50 => 270°` (Equivalent to -50 + 360 => round(310) => 270°)
     */
    @JvmStatic
    public fun round(degrees: Int): Int {
        // 1. Given an integer (positive or negative) constrain it to -359...359 using % 360
        // 2. Offset the value to a positive range by adding 360.
        // 3. When rounding to a multiple of 90 we use integer division to discard the remainder,
        //    which effectively rounds down for positive integers. Adding 45 causes this to round to
        //    the nearest discrete value.
        // 4. Multiply by 90 to convert the value back to degrees.
        // 5. % 360, giving a one of 0, 90, 180, 270
        return ((degrees % 360 + (360 + 45)) / 90) * 90 % 360
    }

    /**
     * Round [degrees] to the nearest discrete rotation (0, 90, 180, 270). Negative values are
     * rounded to the nearest positive discrete rotation value.
     *
     * Example(s):
     * - `40.000f => 0°`
     * - `44.990f => 0°`
     * - `45.001f => 90°`
     * - `-40.00f° => 0°` (Equivalent to -40.000f + 360 => round(320) => 0)
     * - `-50.00f° => 270°` (Equivalent to -50.000f + 360 => round(310) => 270)
     */
    @JvmStatic
    public fun round(degrees: Float): Int {
        // 1. Constrain to -360 < d < 360 using % 360
        // 2. Divide d by 90 and round giving an integer value between -4 < d < 4
        // 3. Multiply by 90, and add 360 to convert to a positive integer degree range.
        // 4. % 360, giving a one of 0, 90, 180, 270
        return (Math.round(degrees % 360 / 90) * 90 + 360) % 360
    }

    /** Get a [DiscreteRotation] from [Surface] rotation values. */
    @JvmStatic
    public fun fromSurfaceRotation(surfaceRotation: Int): Int =
        when (surfaceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalArgumentException("Unexpected Surface rotation $surfaceRotation!")
        }
}
