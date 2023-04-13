/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("Utils")

package androidx.graphics.shapes

import android.graphics.PointF
import android.util.Log
import androidx.core.graphics.div
import androidx.core.graphics.plus
import androidx.core.graphics.times
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * This class has all internal methods, used by Polygon, Morph, etc.
 */

internal fun interpolate(start: Float, stop: Float, fraction: Float) =
    (start * (1 - fraction) + stop * fraction)

internal fun interpolate(start: PointF, stop: PointF, fraction: Float): PointF {
    return PointF(
        interpolate(start.x, stop.x, fraction),
        interpolate(start.y, stop.y, fraction)
    )
}

/**
 * Non-allocating version of interpolate; values are stored in [result]
 */
internal fun interpolate(start: PointF, stop: PointF, result: PointF, fraction: Float): PointF {
    result.x = interpolate(start.x, stop.x, fraction)
    result.y = interpolate(start.y, stop.y, fraction)
    return result
}

internal fun PointF.getDistance() = sqrt(x * x + y * y)

internal fun PointF.dotProduct(other: PointF) = x * other.x + y * other.y

/**
 * Compute the Z coordinate of the cross product of two vectors, to check if the second vector is
 * going clockwise ( > 0 ) or counterclockwise (< 0) compared with the first one.
 * It could also be 0, if the vectors are co-linear.
 */
internal fun PointF.clockwise(other: PointF) = x * other.y - y * other.x > 0

/**
 * Returns unit vector representing the direction to this point from (0, 0)
 */
internal fun PointF.getDirection() = run {
    val d = this.getDistance()
    require(d > 0f)
    this / d
}

/**
 * These epsilon values are used internally to determine when two points are the same, within
 * some reasonable roundoff error. The distance epsilon is smaller, with the intention that the
 * roundoff should not be larger than a pixel on any reasonable sized display.
 */
internal const val DistanceEpsilon = 1e-4f
internal const val AngleEpsilon = 1e-6f

internal fun PointF.rotate90() = PointF(-y, x)

internal val Zero = PointF(0f, 0f)

internal val FloatPi = Math.PI.toFloat()

internal val TwoPi: Float = 2 * Math.PI.toFloat()

internal fun directionVector(angleRadians: Float) = PointF(cos(angleRadians), sin(angleRadians))

internal fun square(x: Float) = x * x

internal fun PointF.copy(x: Float = Float.NaN, y: Float = Float.NaN) =
    PointF(if (x.isNaN()) this.x else x, if (y.isNaN()) this.y else y)

internal fun PointF.angle() = ((atan2(y, x) + TwoPi) % TwoPi)

internal fun radialToCartesian(radius: Float, angleRadians: Float, center: PointF = Zero) =
    directionVector(angleRadians) * radius + center

internal fun positiveModule(num: Float, mod: Float) = (num % mod + mod) % mod

/*
 * Does a ternary search in [v0..v1] to find the parameter that minimizes the given function.
 * Stops when the search space size is reduced below the given tolerance.
 *
 * NTS: Does it make sense to split the function f in 2, one to generate a candidate, of a custom
 * type T (i.e. (Float) -> T), and one to evaluate it ( (T) -> Float )?
 */
internal fun findMinimum(
    v0: Float,
    v1: Float,
    tolerance: Float = 1e-3f,
    f: (Float) -> Float
): Float {
    var a = v0
    var b = v1
    while (b - a > tolerance) {
        val c1 = (2 * a + b) / 3
        val c2 = (2 * b + a) / 3
        if (f(c1) < f(c2)) {
            b = c2
        } else {
            a = c1
        }
    }
    return (a + b) / 2
}

// Used to enable debug logging in the library
internal val DEBUG = true

internal inline fun debugLog(tag: String, messageFactory: () -> String) {
    if (DEBUG) messageFactory().split("\n").forEach { Log.d(tag, it) }
}