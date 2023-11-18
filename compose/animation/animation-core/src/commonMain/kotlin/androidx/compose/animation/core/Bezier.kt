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

package androidx.compose.animation.core

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val Tau = Math.PI * 2.0
private const val Epsilon = 1e-7
private const val FloatEpsilon = 1e-7f

/**
 * Evaluates a cubic Bézier curve at position [t] along the curve and returns
 * the Y coordinate at that position. The curve is defined by the start
 * point (0, 0), the end point (0, 0) and two control points of respective
 * Y coordinates [p1y] and [p2y].
 */
@Suppress("UnnecessaryVariable")
internal fun evaluateCubic(
    p1y: Float,
    p2y: Float,
    t: Float
): Float {
    val a = 1.0 / 3.0 + (p1y - p2y)
    val b = (p2y - 2.0 * p1y)
    val c = p1y
    return 3.0f * (((a * t + b) * t + c) * t).toFloat()
}

/**
 * Finds the first real root of a cubic Bézier curve. To find the roots, only the X
 * coordinates of the four points are required:
 * - [p0]: x coordinate of the start point
 * - [p1]: x coordinate of the first control point
 * - [p2]: x coordinate of the second control point
 * - [p3]: x coordinate of the end point
 *
 * If no root can be found, this method returns [Float.NaN].
 */
internal fun findFirstCubicRoot(
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float
): Float {
    // This function implements Cardano's algorithm as described in "A Primer on Bézier Curves":
    // https://pomax.github.io/bezierinfo/#yforx
    //
    // The math used to find the roots is explained in "Solving the Cubic Equation":
    // http://www.trans4mind.com/personal_development/mathematics/polynomials/cubicAlgebra.htm

    var a = 3.0 * p0 - 6.0 * p1 + 3.0 * p2
    var b = -3.0 * p0 + 3.0 * p1
    var c = p0.toDouble()
    val d = -p0 + 3.0 * p1 - 3.0 * p2 + p3

    // Not a cubic
    if (d.closeTo(0.0)) {
        // Not a quadratic
        if (a.closeTo(0.0)) {
            // No solutions
            if (b.closeTo(0.0)) {
                return Float.NaN
            }
            return clampValidRootInUnitRange((-c / b).toFloat())
        } else {
            val q = sqrt(b * b - 4.0 * a * c)
            val a2 = 2.0 * a

            val root = clampValidRootInUnitRange(((q - b) / a2).toFloat())
            if (!root.isNaN()) return root

            return clampValidRootInUnitRange(((-b - q) / a2).toFloat())
        }
    }

    a /= d
    b /= d
    c /= d

    val o = (3.0 * b - a * a) / 3.0
    val o3 = o / 3.0
    val q = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 27.0
    val q2 = q / 2.0
    val discriminant = q2 * q2 + o3 * o3 * o3

    if (discriminant < 0.0) {
        val mp3 = -o / 3.0
        val mp33 = mp3 * mp3 * mp3
        val r = sqrt(mp33)
        val t = -q / (2.0 * r)
        val cosPhi = min(1.0, max(-1.0, t))
        val phi = acos(cosPhi)
        val t1 = 2.0 * cbrt(r)

        var root = clampValidRootInUnitRange((t1 * cos(phi / 3.0) - a / 3.0).toFloat())
        if (!root.isNaN()) return root

        root = clampValidRootInUnitRange((t1 * cos((phi + Tau) / 3.0) - a / 3.0).toFloat())
        if (!root.isNaN()) return root

        return clampValidRootInUnitRange((t1 * cos((phi + 2.0 * Tau) / 3.0) - a / 3.0).toFloat())
    } else if (discriminant == 0.0) { // TODO: closeTo(0.0)?
        val u1 = if (q2 < 0.0) cbrt(-q2) else -cbrt(q2)

        val root = clampValidRootInUnitRange((2.0 * u1 - a / 3.0).toFloat())
        if (!root.isNaN()) return root

        return clampValidRootInUnitRange((-u1 - a / 3.0).toFloat())
    }

    val sd = sqrt(discriminant)
    val u1 = cbrt(-q2 + sd)
    val v1 = cbrt(q2 + sd)

    return clampValidRootInUnitRange((u1 - v1 - a / 3.0).toFloat())
}

/**
 * Finds the real roots of a cubic Bézier curve. To find the roots, only the X
 * coordinates of the four points are required:
 * - [p0]: x coordinate of the start point
 * - [p1]: x coordinate of the first control point
 * - [p2]: x coordinate of the second control point
 * - [p3]: x coordinate of the end point
 *
 * This function returns the number of roots written in the [roots] array
 * starting at [index]. The number of roots can be 0, 1, 2, or 3. If the
 * function returns 0, no root was found and the cubic curve does not have
 * a numerical solution and should be considered invalid.
 */
internal fun findCubicRoots(
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float,
    roots: FloatArray,
    index: Int = 0
): Int {
    // This function implements Cardano's algorithm as described in "A Primer on Bézier Curves":
    // https://pomax.github.io/bezierinfo/#yforx
    //
    // The math used to find the roots is explained in "Solving the Cubic Equation":
    // http://www.trans4mind.com/personal_development/mathematics/polynomials/cubicAlgebra.htm

    var a = 3.0 * p0 - 6.0 * p1 + 3.0 * p2
    var b = -3.0 * p0 + 3.0 * p1
    var c = p0.toDouble()
    val d = -p0 + 3.0 * p1 - 3.0 * p2 + p3

    var rootCount = 0

    // Not a cubic
    if (d.closeTo(0.0)) {
        // Not a quadratic
        if (a.closeTo(0.0)) {
            // No solutions
            if (b.closeTo(0.0)) {
                return 0
            }
            return writeValidRootInUnitRange((-c / b).toFloat(), roots, index)
        } else {
            val q = sqrt(b * b - 4.0 * a * c)
            val a2 = 2.0 * a

            rootCount += writeValidRootInUnitRange(
                ((q - b) / a2).toFloat(), roots, index
            )
            rootCount += writeValidRootInUnitRange(
                ((-b - q) / a2).toFloat(), roots, index + rootCount
            )
            return rootCount
        }
    }

    a /= d
    b /= d
    c /= d

    val o = (3.0 * b - a * a) / 3.0
    val o3 = o / 3.0
    val q = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 27.0
    val q2 = q / 2.0
    val discriminant = q2 * q2 + o3 * o3 * o3

    if (discriminant < 0.0) {
        val mp3 = -o / 3.0
        val mp33 = mp3 * mp3 * mp3
        val r = sqrt(mp33)
        val t = -q / (2.0 * r)
        val cosPhi = min(1.0, max(-1.0, t))
        val phi = acos(cosPhi)
        val t1 = 2.0 * cbrt(r)

        rootCount += writeValidRootInUnitRange(
            (t1 * cos(phi / 3.0) - a / 3.0).toFloat(), roots, index
        )
        rootCount += writeValidRootInUnitRange(
            (t1 * cos((phi + Tau) / 3.0) - a / 3.0).toFloat(),
            roots,
            index + rootCount
        )
        rootCount += writeValidRootInUnitRange(
            (t1 * cos((phi + 2.0 * Tau) / 3.0) - a / 3.0).toFloat(),
            roots,
            index + rootCount
        )
        return rootCount
    } else if (discriminant == 0.0) { // TODO: closeTo(0.0)?
        val u1 = if (q2 < 0.0) cbrt(-q2) else -cbrt(q2)

        rootCount += writeValidRootInUnitRange(
            (2.0 * u1 - a / 3.0).toFloat(),
            roots,
            index
        )
        rootCount += writeValidRootInUnitRange(
            (-u1 - a / 3.0).toFloat(),
            roots,
            index + rootCount
        )
        return rootCount
    }

    val sd = sqrt(discriminant)
    val u1 = cbrt(-q2 + sd)
    val v1 = cbrt(q2 + sd)

    return writeValidRootInUnitRange((u1 - v1 - a / 3.0).toFloat(), roots, index)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Double.closeTo(b: Double, epsilon: Double = Epsilon) = abs(this - b) < epsilon

/**
 * Writes the root [r] in the [roots] array at the specified [index]. If [r]
 * is outside the [0..1] range, [Float.NaN] is written instead. To account for
 * numerical imprecision in computations, values in the [-FloatEpsilon..1+FloatEpsilon]
 * range are considered to be in the [0..1] range and clamped appropriately.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun writeValidRootInUnitRange(r: Float, roots: FloatArray, index: Int): Int {
    val v = if (r < 0.0f) {
        if (r >= -FloatEpsilon) 0.0f else Float.NaN
    } else if (r > 1.0f) {
        if (r <= 1.0f + FloatEpsilon) 1.0f else Float.NaN
    } else {
        r
    }
    roots[index] = v
    return if (v.isNaN()) 0 else 1
}

/**
 * Returns [r] if it's in the [0..1] range, and [Float.NaN] otherwise. To account
 * for numerical imprecision in computations, values in the [-FloatEpsilon..1+FloatEpsilon]
 * range are considered to be in the [0..1] range and clamped appropriately.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun clampValidRootInUnitRange(r: Float): Float = if (r < 0.0f) {
    if (r >= -FloatEpsilon) 0.0f else Float.NaN
} else if (r > 1.0f) {
    if (r <= 1.0f + FloatEpsilon) 1.0f else Float.NaN
} else {
    r
}
