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

import androidx.annotation.FloatRange
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * A utility for working with a signed angle. A positive value represents rotation from the positive
 * x-axis to the positive y-axis. Angle functions manage the conversion of angle values in degrees
 * and radians. Most of Strokes API requires angle values in radians.
 */
public object Angle {
    @JvmStatic
    @AngleRadiansFloat
    public fun degreesToRadians(@AngleDegreesFloat degrees: Float): Float =
        degrees * RADIANS_PER_DEGREE

    @JvmStatic
    @AngleDegreesFloat
    public fun radiansToDegrees(@AngleRadiansFloat radians: Float): Float =
        radians * DEGREES_PER_RADIAN

    /** Returns the equivalent angle in radians in the range [0, 2π). */
    @JvmStatic
    @AngleRadiansFloat
    @FloatRange(from = 0.0, to = 2 * Math.PI, toInclusive = false)
    public fun normalizedRadians(@AngleRadiansFloat radians: Float): Float =
        AngleNative.normalizedRadians(radians)

    /** Returns the equivalent angle in radians in the range (-π, π]. */
    @JvmStatic
    @AngleRadiansFloat
    @FloatRange(from = -Math.PI, to = Math.PI, fromInclusive = false)
    public fun normalizedAboutZeroRadians(@AngleRadiansFloat radians: Float): Float =
        AngleNative.normalizedAboutZeroRadians(radians)

    /** Returns the equivalent angle in degrees in the range [0, 360). */
    @JvmStatic
    @AngleDegreesFloat
    @FloatRange(from = 0.0, to = 360.0, toInclusive = false)
    public fun normalizedDegrees(@AngleDegreesFloat degrees: Float): Float =
        AngleNative.normalizedDegrees(degrees)

    /** Returns the equivalent angle in degrees in the range (-180, 180]. */
    @JvmStatic
    @AngleDegreesFloat
    @FloatRange(from = -180.0, to = 180.0, fromInclusive = false)
    public fun normalizedAboutZeroDegrees(@AngleDegreesFloat degrees: Float): Float =
        AngleNative.normalizedAboutZeroDegrees(degrees)

    private const val DEGREES_PER_RADIAN = 180.0f / Math.PI.toFloat()
    private const val RADIANS_PER_DEGREE = Math.PI.toFloat() / 180.0f

    /** Angle of zero (also 0 degrees, but this is annotated as radians). */
    @JvmField @AngleRadiansFloat public val ZERO_RADIANS: Float = 0.0f

    /** Angle of PI radians (180 degrees). */
    @JvmField @AngleRadiansFloat public val HALF_TURN_RADIANS: Float = Math.PI.toFloat()

    /** Angle of PI/2 radians (90 degrees). */
    @JvmField @AngleRadiansFloat public val QUARTER_TURN_RADIANS: Float = (Math.PI / 2).toFloat()

    /** Angle of 2*PI radians (360 degrees) */
    @JvmField @AngleRadiansFloat public val FULL_TURN_RADIANS: Float = (Math.PI * 2).toFloat()

    /** Angle of zero (also 0 radians, but this is annotated as degrees). */
    @JvmField @AngleDegreesFloat public val ZERO_DEGREES: Float = 0.0f

    /** Angle 180 degrees (PI radians). */
    @JvmField @AngleDegreesFloat public val HALF_TURN_DEGREES: Float = 180.0f

    /** Angle of 90 degrees (PI/2 radians). */
    @JvmField @AngleDegreesFloat public val QUARTER_TURN_DEGREES: Float = 90.0f

    /** Angle of 360 degrees (2*PI radians). */
    @JvmField @AngleDegreesFloat public val FULL_TURN_DEGREES: Float = 360.0f
}

@UsedByNative
internal object AngleNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative external fun normalizedDegrees(degrees: Float): Float

    @UsedByNative external fun normalizedAboutZeroDegrees(degrees: Float): Float

    @UsedByNative external fun normalizedRadians(radians: Float): Float

    @UsedByNative external fun normalizedAboutZeroRadians(radians: Float): Float
}
