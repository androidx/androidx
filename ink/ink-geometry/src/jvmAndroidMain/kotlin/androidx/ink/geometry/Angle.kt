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

package androidx.ink.geometry

import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader

/**
 * A utility for working with a signed angle. A positive value represents rotation from the positive
 * x-axis to the positive y-axis. Angle functions manage the conversion of angle values in degrees
 * and radians. Most of Strokes API requires angle values in radians.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object Angle {

    init {
        NativeLoader.load()
    }

    @JvmStatic
    @AngleRadiansFloat
    public fun degreesToRadians(degrees: Float): Float = degrees * RADIANS_PER_DEGREE

    @JvmStatic
    @AngleRadiansFloat
    public fun degreesToRadians(degrees: Int): Float = degrees.toFloat() * RADIANS_PER_DEGREE

    @JvmStatic
    @AngleRadiansFloat
    public fun degreesToRadians(degrees: Double): Float = degrees.toFloat() * RADIANS_PER_DEGREE

    @JvmStatic
    public fun radiansToDegrees(@AngleRadiansFloat radians: Float): Float =
        radians * DEGREES_PER_RADIAN

    @JvmStatic
    public fun normalized(@AngleRadiansFloat radians: Float): Float = nativeNormalized(radians)

    @JvmStatic
    public fun normalizedAboutZero(@AngleRadiansFloat radians: Float): Float =
        nativeNormalizedAboutZero(radians)

    private const val PI = Math.PI.toFloat()
    private const val HALF_PI = (Math.PI / 2.0).toFloat()
    private const val TWO_PI = (2 * Math.PI).toFloat()
    private const val DEGREES_PER_RADIAN = 180.0f / Math.PI.toFloat()
    private const val RADIANS_PER_DEGREE = Math.PI.toFloat() / 180.0f

    /** Angle of zero radians. */
    @JvmField @AngleRadiansFloat public val ZERO: Float = 0.0f
    /** Angle of PI radians. */
    @JvmField @AngleRadiansFloat public val PI_RADIANS: Float = PI
    /** Angle of PI/2 radians. */
    @JvmField @AngleRadiansFloat public val HALF_PI_RADIANS: Float = HALF_PI
    /** Angle of 2*PI radians. */
    @JvmField @AngleRadiansFloat public val TWO_PI_RADIANS: Float = TWO_PI

    private external fun nativeNormalized(radians: Float): Float

    private external fun nativeNormalizedAboutZero(radians: Float): Float
}
