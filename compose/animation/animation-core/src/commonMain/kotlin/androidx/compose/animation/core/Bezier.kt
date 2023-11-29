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

import androidx.collection.FloatFloatPair
import androidx.compose.ui.graphics.PathSegment
import androidx.compose.ui.util.fastCbrt
import androidx.compose.ui.util.fastCoerceIn
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val Tau = Math.PI * 2.0
private const val Epsilon = 1e-7
private const val FloatEpsilon = 1e-7f

/**
 * Evaluate the specified [segment] at position [t] and returns the X
 * coordinate of the segment's curve at that position.
 */
private fun evaluateX(
    segment: PathSegment,
    t: Float
): Float {
    val points = segment.points

    return when (segment.type) {
        PathSegment.Type.Move -> points[0]

        PathSegment.Type.Line -> {
            evaluateLine(
                points[0],
                points[2],
                t
            )
        }

        PathSegment.Type.Quadratic -> {
            evaluateQuadratic(
                points[0],
                points[2],
                points[4],
                t
            )
        }

        // We convert all conics to cubics, won't happen
        PathSegment.Type.Conic -> Float.NaN

        PathSegment.Type.Cubic -> {
            evaluateCubic(
                points[0],
                points[2],
                points[4],
                points[6],
                t
            )
        }

        PathSegment.Type.Close -> Float.NaN
        PathSegment.Type.Done -> Float.NaN
    }
}

/**
 * Evaluate the specified [segment] at position [t] and returns the Y
 * coordinate of the segment's curve at that position.
 */
internal fun evaluateY(
    segment: PathSegment,
    t: Float
): Float {
    val points = segment.points

    return when (segment.type) {
        PathSegment.Type.Move -> points[1]

        PathSegment.Type.Line -> {
            evaluateLine(
                points[1],
                points[3],
                t
            )
        }

        PathSegment.Type.Quadratic -> {
            evaluateQuadratic(
                points[1],
                points[3],
                points[5],
                t
            )
        }

        // We convert all conics to cubics, won't happen
        PathSegment.Type.Conic -> Float.NaN

        PathSegment.Type.Cubic -> {
            evaluateCubic(
                points[1],
                points[3],
                points[5],
                points[7],
                t
            )
        }

        PathSegment.Type.Close -> Float.NaN
        PathSegment.Type.Done -> Float.NaN
    }
}

private fun evaluateLine(
    p0y: Float,
    p1y: Float,
    t: Float
) = (p1y - p0y) * t + p0y

private fun evaluateQuadratic(
    p0: Float,
    p1: Float,
    p2: Float,
    t: Float
): Float {
    val by = 2.0f * (p1 - p0)
    val ay = p2 - 2.0f * p1 + p0
    return (ay * t + by) * t + p0
}

private fun evaluateCubic(
    p0: Float,
    p1: Float,
    p2: Float,
    p3: Float,
    t: Float
): Float {
    val a = p3 + 3.0f * (p1 - p2) - p0
    val b = 3.0f * (p2 - 2.0f * p1 + p0)
    val c = 3.0f * (p1 - p0)
    return ((a * t + b) * t + c) * t + p0
}

/**
 * Evaluates a cubic Bézier curve at position [t] along the curve. The curve is
 * defined by the start point (0, 0), the end point (0, 0) and two control points
 * of respective coordinates [p1] and [p2].
 */
@Suppress("UnnecessaryVariable")
internal fun evaluateCubic(
    p1: Float,
    p2: Float,
    t: Float
): Float {
    val a = 1.0f / 3.0f + (p1 - p2)
    val b = (p2 - 2.0f * p1)
    val c = p1
    return 3.0f * ((a * t + b) * t + c) * t
}

/**
 * Finds the first real root of the specified [segment].
 * If no root can be found, this method returns [Float.NaN].
 */
internal fun findFirstRoot(
    segment: PathSegment,
    fraction: Float
): Float {
    val points = segment.points
    return when (segment.type) {
        PathSegment.Type.Move -> Float.NaN

        PathSegment.Type.Line -> {
            findFirstLineRoot(
                points[0] - fraction,
                points[2] - fraction,
            )
        }

        PathSegment.Type.Quadratic -> findFirstQuadraticRoot(
            points[0] - fraction,
            points[2] - fraction,
            points[4] - fraction
        )

        // We convert all conics to cubics, won't happen
        PathSegment.Type.Conic -> Float.NaN

        PathSegment.Type.Cubic -> findFirstCubicRoot(
            points[0] - fraction,
            points[2] - fraction,
            points[4] - fraction,
            points[6] - fraction
        )

        PathSegment.Type.Close -> Float.NaN
        PathSegment.Type.Done -> Float.NaN
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun findFirstLineRoot(p0: Float, p1: Float) =
    clampValidRootInUnitRange(-p0 / (p1 - p0))

/**
 * Finds the first real root of a quadratic Bézier curve:
 * - [p0]: coordinate of the start point
 * - [p1]: coordinate of the control point
 * - [p2]: coordinate of the end point
 *
 * If no root can be found, this method returns [Float.NaN].
 */
private fun findFirstQuadraticRoot(
    p0: Float,
    p1: Float,
    p2: Float
): Float {
    val a = p0.toDouble()
    val b = p1.toDouble()
    val c = p2.toDouble()
    val d = a - 2.0 * b + c

    if (d != 0.0) {
        val v1 = -sqrt(b * b - a * c)
        val v2 = -a + b

        val root = clampValidRootInUnitRange((-(v1 + v2) / d).toFloat())
        if (!root.isNaN()) return root

        return clampValidRootInUnitRange(((v1 - v2) / d).toFloat())
    } else if (b != c) {
        return clampValidRootInUnitRange(((2.0 * b - c) / (2.0 * b - 2.0 * c)).toFloat())
    }

    return Float.NaN
}

/**
 * Finds the first real root of a cubic Bézier curve:
 * - [p0]: coordinate of the start point
 * - [p1]: coordinate of the first control point
 * - [p2]: coordinate of the second control point
 * - [p3]: coordinate of the end point
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

    var a = 3.0 * (p0 - 2.0 * p1 + p2)
    var b = 3.0 * (p1 - p0)
    var c = p0.toDouble()
    val d = -p0 + 3.0 * (p1 - p2) + p3

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

    val o3 = (3.0 * b - a * a) / 9.0
    val q2 = (2.0 * a * a * a - 9.0 * a * b + 27.0 * c) / 54.0
    val discriminant = q2 * q2 + o3 * o3 * o3
    val a3 = a / 3.0

    if (discriminant < 0.0) {
        val mp33 = -(o3 * o3 * o3)
        val r = sqrt(mp33)
        val t = -q2 / r
        val cosPhi = t.fastCoerceIn(-1.0, 1.0)
        val phi = acos(cosPhi)
        val t1 = 2.0f * fastCbrt(r.toFloat())

        var root = clampValidRootInUnitRange((t1 * cos(phi / 3.0) - a3).toFloat())
        if (!root.isNaN()) return root

        root = clampValidRootInUnitRange((t1 * cos((phi + Tau) / 3.0) - a3).toFloat())
        if (!root.isNaN()) return root

        return clampValidRootInUnitRange((t1 * cos((phi + 2.0 * Tau) / 3.0) - a3).toFloat())
    } else if (discriminant == 0.0) { // TODO: closeTo(0.0)?
        val u1 = -fastCbrt(q2.toFloat())

        val root = clampValidRootInUnitRange(2.0f * u1 - a3.toFloat())
        if (!root.isNaN()) return root

        return clampValidRootInUnitRange(-u1 - a3.toFloat())
    }

    val sd = sqrt(discriminant)
    val u1 = fastCbrt((-q2 + sd).toFloat())
    val v1 = fastCbrt((q2 + sd).toFloat())

    return clampValidRootInUnitRange((u1 - v1 - a3).toFloat())
}

/**
 * Finds the real root of a line defined by the X coordinates of its start ([p0])
 * and end ([p1]) points. The root, if any, is written in the [roots] array at
 * [index]. Returns 1 if a root was found, 0 otherwise.
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun findLineRoot(p0: Float, p1: Float, roots: FloatArray, index: Int = 0) =
    writeValidRootInUnitRange(-p0 / (p1 - p0), roots, index)

/**
 * Finds the real roots of a quadratic Bézier curve. To find the roots, only the X
 * coordinates of the four points are required:
 * - [p0]: x coordinate of the start point
 * - [p1]: x coordinate of the control point
 * - [p2]: x coordinate of the end point
 *
 * Any root found is written in the [roots] array, starting at [index]. The
 * function returns the number of roots found and written to the array.
 */
private fun findQuadraticRoots(
    p0: Float,
    p1: Float,
    p2: Float,
    roots: FloatArray,
    index: Int = 0
): Int {
    val a = p0.toDouble()
    val b = p1.toDouble()
    val c = p2.toDouble()
    val d = a - 2.0 * b + c

    var rootCount = 0

    if (d != 0.0) {
        val v1 = -sqrt(b * b - a * c)
        val v2 = -a + b

        rootCount += writeValidRootInUnitRange(
            (-(v1 + v2) / d).toFloat(), roots, index
        )
        rootCount += writeValidRootInUnitRange(
            ((v1 - v2) / d).toFloat(), roots, index + rootCount
        )
    } else if (b != c) {
        rootCount += writeValidRootInUnitRange(
            ((2.0 * b - c) / (2.0 * b - 2.0 * c)).toFloat(), roots, index
        )
    }

    return rootCount
}

/**
 * Finds the roots of the derivative of the curve described by [segment].
 * The roots, if any, are written in the [roots] array starting at [index].
 * The function returns the number of roots founds and written into the array.
 * The [roots] array must be able to hold at least 5 floats starting at [index].
 */
private fun findDerivativeRoots(
    segment: PathSegment,
    roots: FloatArray,
    index: Int = 0,
): Int {
    val points = segment.points
    return when (segment.type) {
        PathSegment.Type.Move -> 0

        PathSegment.Type.Line -> 0

        PathSegment.Type.Quadratic -> {
            // Line derivative of a quadratic function
            // We do the computation inline to avoid using arrays of other data
            // structures to return the result
            val d0 = 2 * (points[2] - points[0])
            val d1 = 2 * (points[4] - points[2])
            findLineRoot(d0, d1, roots, index)
        }

        // We convert all conics to cubics, won't happen
        PathSegment.Type.Conic -> 0

        PathSegment.Type.Cubic -> {
            // Quadratic derivative of a cubic function
            // We do the computation inline to avoid using arrays of other data
            // structures to return the result
            val d0 = 3.0f * (points[2] - points[0])
            val d1 = 3.0f * (points[4] - points[2])
            val d2 = 3.0f * (points[6] - points[4])
            val count = findQuadraticRoots(d0, d1, d2, roots, index)

            // Compute the second derivative as a line
            val dd0 = 2.0f * (d1 - d0)
            val dd1 = 2.0f * (d2 - d1)
            count + findLineRoot(dd0, dd1, roots, index + count)
        }

        PathSegment.Type.Close -> 0
        PathSegment.Type.Done -> 0
    }
}

/**
 * Computes the horizontal bounds of the specified [segment] and returns
 * a pair of floats containing the lowest bound as the first value, and
 * the highest bound as the second value.
 *
 * The [roots] array is used as a scratch array and must be able to hold
 * at least 5 floats.
 */
internal fun computeHorizontalBounds(
    segment: PathSegment,
    roots: FloatArray,
    index: Int = 0
): FloatFloatPair {
    val count = findDerivativeRoots(segment, roots, index)
    var minX = min(segment.startX, segment.endX)
    var maxX = max(segment.startX, segment.endX)

    for (i in 0 until count) {
        val t = roots[i]
        val x = evaluateX(segment, t)
        minX = min(minX, x)
        maxX = max(maxX, x)
    }

    return FloatFloatPair(minX, maxX)
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Double.closeTo(b: Double, epsilon: Double = Epsilon) = abs(this - b) < epsilon

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

/**
 * Writes [r] in the [roots] array at [index], if it's in the [0..1] range. To account
 * for numerical imprecision in computations, values in the [-FloatEpsilon..1+FloatEpsilon]
 * range are considered to be in the [0..1] range and clamped appropriately. Returns 0 if
 * no value was written, 1 otherwise.
 */
private fun writeValidRootInUnitRange(r: Float, roots: FloatArray, index: Int): Int {
    val v = clampValidRootInUnitRange(r)
    roots[index] = v
    return if (v.isNaN()) 0 else 1
}

private inline val PathSegment.startX: Float
    get() = points[0]

private val PathSegment.endX: Float
    get() = points[when (type) {
        PathSegment.Type.Move -> 0
        PathSegment.Type.Line -> 2
        PathSegment.Type.Quadratic -> 4
        PathSegment.Type.Conic -> 4
        PathSegment.Type.Cubic -> 6
        PathSegment.Type.Close -> 0
        PathSegment.Type.Done -> 0
    }]
