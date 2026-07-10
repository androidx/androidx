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

@file:JvmName("MathHelper")
@file:Suppress("NOTHING_TO_INLINE")

package androidx.xr.runtime.math

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val DEGREES_PER_RADIAN = 180.0f / PI.toFloat()
private const val RADIANS_PER_DEGREE = PI.toFloat() / 180.0f

/** Calculates the reciprocal square root 1/sqrt(x) with a maximum error up to threshold. */
internal inline fun rsqrt(x: Float): Float {
    return 1 / sqrt(x)
}

/**
 * Clamps a value.
 *
 * @param x the value to clamp
 * @param min the minimum value
 * @param max the maximum value
 */
public fun clamp(x: Float, min: Float, max: Float): Float {
    val result = min(max, max(min, x))
    return result
}

/**
 * Linearly interpolates between two values.
 *
 * @param a the start value
 * @param b the end value
 * @param t the ratio between the two floats
 * @return the interpolated value between [a] and [b]
 */
public fun lerp(a: Float, b: Float, t: Float): Float {
    return a * (1.0f - t) + b * t
}

/**
 * Converts [angleInRadians] from radians to degrees.
 *
 * @param angleInRadians the angle in radians
 */
public fun toDegrees(angleInRadians: Float): Float = angleInRadians * DEGREES_PER_RADIAN

/**
 * Converts [angleInDegrees] from degrees to radians.
 *
 * @param angleInDegrees the angle in degrees
 */
public fun toRadians(angleInDegrees: Float): Float = angleInDegrees * RADIANS_PER_DEGREE
