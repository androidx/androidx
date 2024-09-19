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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.graphics

import androidx.annotation.RestrictTo
import androidx.collection.FloatFloatPair
import androidx.compose.ui.util.fastCbrt
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastMaxOf
import androidx.compose.ui.util.fastMinOf
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

private const val Tau = PI * 2.0
private const val Epsilon = 1e-7
// We use a fairly high epsilon here because it's post double->float conversion
// and because we use a fast approximation of cbrt(). The epsilon we use here is
// slightly larger than the max error of fastCbrt() in the -1f..1f range
// (8.3446500e-7f) but smaller than 1.0f.ulp * 10.
private const val FloatEpsilon = 1e-6f

/**
 * Evaluate the specified [segment] at position [t] and returns the X coordinate of the segment's
 * curve at that position.
 */
private fun evaluateX(segment: PathSegment, t: Float): Float {
    val points = segment.points

    return when (segment.type) {
        PathSegment.Type.Move -> points[0]
        PathSegment.Type.Line -> {
            evaluateLine(points[0], points[2], t)
        }
        PathSegment.Type.Quadratic -> {
            evaluateQuadratic(points[0], points[2], points[4], t)
        }
        PathSegment.Type.Cubic -> {
            evaluateCubic(points[0], points[2], points[4], points[6], t)
        }
        // Conic (converted to Cubic), Close, Done
        else -> Float.NaN
    }
}

/**
 * Evaluate the specified [segment] at position [t] and returns the Y coordinate of the segment's
 * curve at that position.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun evaluateY(segment: PathSegment, t: Float): Float {
    val points = segment.points

    return when (segment.type) {
        PathSegment.Type.Move -> points[1]
        PathSegment.Type.Line -> {
            evaluateLine(points[1], points[3], t)
        }
        PathSegment.Type.Quadratic -> {
            evaluateQuadratic(points[1], points[3], points[5], t)
        }
        PathSegment.Type.Cubic -> {
            evaluateCubic(points[1], points[3], points[5], points[7], t)
        }
        // Conic (converted to Cubic), Close, Done
        else -> Float.NaN
    }
}

private fun evaluateLine(p0y: Float, p1y: Float, t: Float) = (p1y - p0y) * t + p0y

private fun evaluateQuadratic(p0: Float, p1: Float, p2: Float, t: Float): Float {
    val by = 2.0f * (p1 - p0)
    val ay = p2 - 2.0f * p1 + p0
    return (ay * t + by) * t + p0
}

private fun evaluateCubic(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val a = p3 + 3.0f * (p1 - p2) - p0
    val b = 3.0f * (p2 - 2.0f * p1 + p0)
    val c = 3.0f * (p1 - p0)
    return ((a * t + b) * t + c) * t + p0
}

/**
 * Evaluates a cubic Bézier curve at position [t] along the curve. The curve is defined by the start
 * point (0, 0), the end point (0, 0) and two control points of respective coordinates [p1] and
 * [p2].
 */
@Suppress("UnnecessaryVariable")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun evaluateCubic(p1: Float, p2: Float, t: Float): Float {
    val a = 1.0f / 3.0f + (p1 - p2)
    val b = (p2 - 2.0f * p1)
    val c = p1
    return 3.0f * ((a * t + b) * t + c) * t
}

/**
 * Finds the first real root of the specified [segment]. If no root can be found, this method
 * returns [Float.NaN].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun findFirstRoot(segment: PathSegment, fraction: Float): Float {
    val points = segment.points
    return when (segment.type) {
        PathSegment.Type.Move -> Float.NaN
        PathSegment.Type.Line -> {
            findFirstLineRoot(
                points[0] - fraction,
                points[2] - fraction,
            )
        }
        PathSegment.Type.Quadratic ->
            findFirstQuadraticRoot(points[0] - fraction, points[2] - fraction, points[4] - fraction)

        // We convert all conics to cubics, won't happen
        PathSegment.Type.Conic -> Float.NaN
        PathSegment.Type.Cubic ->
            findFirstCubicRoot(
                points[0] - fraction,
                points[2] - fraction,
                points[4] - fraction,
                points[6] - fraction
            )
        PathSegment.Type.Close -> Float.NaN
        PathSegment.Type.Done -> Float.NaN
    }
}

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
private fun findFirstQuadraticRoot(p0: Float, p1: Float, p2: Float): Float {
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun findFirstCubicRoot(p0: Float, p1: Float, p2: Float, p3: Float): Float {
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
 * Finds the real root of a line defined by the X coordinates of its start ([p0]) and end ([p1])
 * points. The root, if any, is written in the [roots] array at [index]. Returns 1 if a root was
 * found, 0 otherwise.
 */
private inline fun findLineRoot(p0: Float, p1: Float, roots: FloatArray, index: Int = 0) =
    writeValidRootInUnitRange(-p0 / (p1 - p0), roots, index)

/**
 * Finds the real roots of a quadratic Bézier curve. To find the roots, only the X coordinates of
 * the four points are required:
 * - [p0]: x coordinate of the start point
 * - [p1]: x coordinate of the control point
 * - [p2]: x coordinate of the end point
 *
 * Any root found is written in the [roots] array, starting at [index]. The function returns the
 * number of roots found and written to the array.
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

        rootCount += writeValidRootInUnitRange((-(v1 + v2) / d).toFloat(), roots, index)
        rootCount += writeValidRootInUnitRange(((v1 - v2) / d).toFloat(), roots, index + rootCount)

        // Returns the roots sorted
        if (rootCount > 1) {
            val s = roots[index]
            val t = roots[index + 1]
            if (s > t) {
                roots[index] = t
                roots[index + 1] = s
            } else if (s == t) {
                // Don't report identical roots
                rootCount--
            }
        }
    } else if (b != c) {
        rootCount +=
            writeValidRootInUnitRange(((2.0 * b - c) / (2.0 * b - 2.0 * c)).toFloat(), roots, index)
    }

    return rootCount
}

/**
 * Finds the roots of the derivative of the curve described by [segment]. The roots, if any, are
 * written in the [roots] array starting at [index]. The function returns the number of roots founds
 * and written into the array. The [roots] array must be able to hold at least 5 floats starting at
 * [index].
 */
private fun findDerivativeRoots(
    segment: PathSegment,
    horizontal: Boolean,
    roots: FloatArray,
    index: Int
): Int {
    val offset = if (horizontal) 0 else 1
    val points = segment.points
    return when (segment.type) {
        PathSegment.Type.Quadratic -> {
            // Line derivative of a quadratic function
            // We do the computation inline to avoid using arrays of other data
            // structures to return the result
            val d0 = 2 * (points[offset + 2] - points[offset + 0])
            val d1 = 2 * (points[offset + 4] - points[offset + 2])
            findLineRoot(d0, d1, roots, index)
        }
        PathSegment.Type.Cubic -> {
            // Quadratic derivative of a cubic function
            // We do the computation inline to avoid using arrays of other data
            // structures to return the result
            val d0 = 3.0f * (points[offset + 2] - points[offset + 0])
            val d1 = 3.0f * (points[offset + 4] - points[offset + 2])
            val d2 = 3.0f * (points[offset + 6] - points[offset + 4])
            val count = findQuadraticRoots(d0, d1, d2, roots, index)

            // Compute the second derivative as a line
            val dd0 = 2.0f * (d1 - d0)
            val dd1 = 2.0f * (d2 - d1)
            // Return the sum of the roots count
            count + findLineRoot(dd0, dd1, roots, index + count)
        }
        else -> 0
    }
}

/**
 * Computes the horizontal bounds of the specified [segment] and returns a pair of floats containing
 * the lowest bound as the first value, and the highest bound as the second value.
 *
 * The [roots] array is used as a scratch array and must be able to hold at least 5 floats.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun computeHorizontalBounds(
    segment: PathSegment,
    roots: FloatArray,
    index: Int = 0
): FloatFloatPair {
    val count = findDerivativeRoots(segment, true, roots, index)
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

/**
 * Computes the vertical bounds of the specified [segment] and returns a pair of floats containing
 * the lowest bound as the first value, and the highest bound as the second value.
 *
 * The [roots] array is used as a scratch array and must be able to hold at least 5 floats.
 */
internal fun computeVerticalBounds(
    segment: PathSegment,
    roots: FloatArray,
    index: Int = 0
): FloatFloatPair {
    val count = findDerivativeRoots(segment, false, roots, index)
    var minY = min(segment.startY, segment.endY)
    var maxY = max(segment.startY, segment.endY)

    for (i in 0 until count) {
        val t = roots[i]
        val x = evaluateY(segment, t)
        minY = min(minY, x)
        maxY = max(maxY, x)
    }

    return FloatFloatPair(minY, maxY)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun computeCubicVerticalBounds(
    p0y: Float,
    p1y: Float,
    p2y: Float,
    p3y: Float,
    roots: FloatArray,
    index: Int = 0
): FloatFloatPair {
    // Quadratic derivative of a cubic function
    // We do the computation inline to avoid using arrays of other data
    // structures to return the result
    val d0 = 3.0f * (p1y - p0y)
    val d1 = 3.0f * (p2y - p1y)
    val d2 = 3.0f * (p3y - p2y)
    var count = findQuadraticRoots(d0, d1, d2, roots, index)

    // Compute the second derivative as a line
    val dd0 = 2.0f * (d1 - d0)
    val dd1 = 2.0f * (d2 - d1)
    count += findLineRoot(dd0, dd1, roots, index + count)

    var minY = min(p0y, p3y)
    var maxY = max(p0y, p3y)

    for (i in 0 until count) {
        val t = roots[i]
        val y = evaluateCubic(p0y, p1y, p2y, p3y, t)
        minY = min(minY, y)
        maxY = max(maxY, y)
    }

    return FloatFloatPair(minY, maxY)
}

internal inline fun Double.closeTo(b: Double) = abs(this - b) < Epsilon

internal inline fun Float.closeTo(b: Float) = abs(this - b) < FloatEpsilon

/**
 * Returns [r] if it's in the [0..1] range, and [Float.NaN] otherwise. To account for numerical
 * imprecision in computations, values in the [-FloatEpsilon..1+FloatEpsilon] range are considered
 * to be in the [0..1] range and clamped appropriately.
 */
private inline fun clampValidRootInUnitRange(r: Float): Float {
    // The code below is a branchless version of:
    // if (r < 0.0f) {
    //     if (r >= -FloatEpsilon) 0.0f else Float.NaN
    // } else if (r > 1.0f) {
    //     if (r <= 1.0f + FloatEpsilon) 1.0f else Float.NaN
    // } else {
    //     r
    // }
    val s = r.fastCoerceIn(0f, 1f)
    return if (abs(s - r) > FloatEpsilon) Float.NaN else s
}

/**
 * Writes [r] in the [roots] array at [index], if it's in the [0..1] range. To account for numerical
 * imprecision in computations, values in the [-FloatEpsilon..1+FloatEpsilon] range are considered
 * to be in the [0..1] range and clamped appropriately. Returns 0 if no value was written, 1
 * otherwise.
 */
private fun writeValidRootInUnitRange(r: Float, roots: FloatArray, index: Int): Int {
    val v = clampValidRootInUnitRange(r)
    roots[index] = v
    return if (v.isNaN()) 0 else 1
}

/**
 * Computes the winding value for a position [x]/[y] and the line defined by the [points] array. The
 * array must contain at least 4 floats defining the start and end points of the line as pairs of
 * x/y coordinates.
 */
internal fun lineWinding(points: FloatArray, x: Float, y: Float): Int {
    if (points.size < 4) return 0

    val x0 = points[0]
    var y0 = points[1]
    val yo = y0
    val x1 = points[2]
    var y1 = points[3]

    // Compute dy before we swap
    val dy = y1 - y0
    var direction = 1

    if (y0 > y1) {
        y0 = y1
        y1 = yo
        direction = -1
    }

    // We exclude the end point
    if (y < y0 || y >= y1) {
        return 0
    }

    // TODO: check if on the curve

    val crossProduct = (x1 - x0) * (y - yo) - dy * (x - x0)
    // The point is on the line
    if (crossProduct == 0.0f) {
        // TODO: check if we are on the line but exclude x1 and y1
        direction = 0
    } else if (crossProduct.sign.toInt() == direction) {
        direction = 0
    }

    return direction
}

/**
 * Returns whether the quadratic Bézier curve defined the start, control, and points [y0], [y1], and
 * [y2] is monotonic on the Y axis.
 */
private fun isQuadraticMonotonic(y0: Float, y1: Float, y2: Float): Boolean =
    (y0 - y1).sign + (y1 - y2).sign != 0.0f

/**
 * Computes the winding value for a position [x]/[y] and the quadratic Bézier curve defined by the
 * [points] array. The array must contain at least 6 floats defining the start, control, and end
 * points of the curve as pairs of x/y coordinates.
 *
 * The [tmpQuadratics] array is a scratch array used to hold temporary values and must contain at
 * least 10 floats. Its content can be ignored after calling this function.
 *
 * The [tmpRoots] array is a scratch array that must contain at least 2 values. It is used to hold
 * temporary values and its content can be ignored after calling this function.
 */
internal fun quadraticWinding(
    points: FloatArray,
    x: Float,
    y: Float,
    tmpQuadratics: FloatArray,
    tmpRoots: FloatArray
): Int {
    val y0 = points[1]
    val y1 = points[3]
    val y2 = points[5]

    if (isQuadraticMonotonic(y0, y1, y2)) {
        return monotonicQuadraticWinding(points, 0, x, y, tmpRoots)
    }

    val rootCount = quadraticToMonotonicQuadratics(points, tmpQuadratics)

    var winding = monotonicQuadraticWinding(tmpQuadratics, 0, x, y, tmpRoots)
    if (rootCount > 0) {
        winding += monotonicQuadraticWinding(tmpQuadratics, 4, x, y, tmpRoots)
    }
    return winding
}

/**
 * Computes the winding value of a _monotonic_ quadratic Bézier curve for the given [x] and [y]
 * coordinates. The curve is defined as 6 floats in the [points] array corresponding to the start,
 * control, and end points. The floats are stored at position [offset] in the array, meaning the
 * array must hold at least [offset] + 6 values.
 *
 * The [tmpRoots] array is a scratch array that must contain at least 2 values. It is used to hold
 * temporary values and its content can be ignored after calling this function.
 */
private fun monotonicQuadraticWinding(
    points: FloatArray,
    offset: Int,
    x: Float,
    y: Float,
    tmpRoots: FloatArray
): Int {
    var y0 = points[offset + 1]
    var y2 = points[offset + 5]

    var direction = 1
    if (y0 > y2) {
        val swap = y2
        y2 = y0
        y0 = swap
        direction = -1
    }

    // Exclude the end point
    if (y < y0 || y >= y2) return 0

    // TODO: check if on the curve

    y0 = points[offset + 1]
    val y1 = points[offset + 3]
    y2 = points[offset + 5]

    val rootCount = findQuadraticRoots(y0 - 2.0f * y1 + y2, 2.0f * (y1 - y0), y0 - y, tmpRoots)

    val xt =
        if (rootCount == 0) {
            points[(1 - direction) * 2]
        } else {
            evaluateQuadratic(points[0], points[2], points[4], tmpRoots[0])
        }

    if (xt.closeTo(x)) {
        if (x != points[4] || y != y2) {
            // TODO: on the curve
            return 0
        }
    }

    return if (xt < x) direction else 0
}

/**
 * Splits the specified [quadratic] Bézier curve into 1 or 2 monotonic quadratic Bézier curves. The
 * results are stored in the [dst] array. Both the input [quadratic] and the output [dst] arrays
 * store the curves as 3 pairs of floats defined by the start, control, and end points. In the [dst]
 * array, successive curves share a point: the end point of the first curve is the start point of
 * the second curve. As a result this function will output at most 10 values in the [dst] array (6
 * floats per curve, minus 2 for a shared point).
 *
 * The function returns the number of splits: if 0 is returned, the [dst] array contains a single
 * quadratic curve, if 1 is returned, the array contains 2 curves with a shared point.
 */
private fun quadraticToMonotonicQuadratics(quadratic: FloatArray, dst: FloatArray): Int {
    if (quadratic.size < 6) return 0
    if (dst.size < 6) return 0

    val y0 = quadratic[1]
    var y1 = quadratic[3]
    val y2 = quadratic[5]

    if (!isQuadraticMonotonic(y0, y1, y2)) {
        val t = unitDivide(y0 - y1, y0 - y1 - y1 + y2)
        if (!t.isNaN()) {
            splitQuadraticAt(quadratic, dst, t)
            return 1
        }
        // force the curve to be monotonic since the division above failed
        y1 = if (abs(y0 - y1) < abs(y1 - y2)) y0 else y2
    }

    quadratic.copyInto(dst, 0, 0, 6)
    dst[3] = y1

    return 0
}

/**
 * Splits the specified [src] quadratic Bézier curve into two quadratic Bézier curves at position
 * [t] (in the range 0..1), and stores the results in [dst]. The [dst] array must hold at least 10
 * floats. See [quadraticToMonotonicQuadratics] for more details.
 */
private fun splitQuadraticAt(src: FloatArray, dst: FloatArray, t: Float) {
    if (src.size < 6) return
    if (dst.size < 10) return

    val p0x = src[0]
    val p0y = src[1]
    val p1x = src[2]
    val p1y = src[3]
    val p2x = src[4]
    val p2y = src[5]

    val abx = lerp(p0x, p1x, t)
    val aby = lerp(p0y, p1y, t)

    dst[0] = p0x
    dst[1] = p0y
    dst[2] = abx
    dst[3] = aby

    val bcx = lerp(p1x, p2x, t)
    val bcy = lerp(p1y, p2y, t)

    val abcx = lerp(abx, bcx, t)
    val abcy = lerp(aby, bcy, t)

    dst[4] = abcx
    dst[5] = abcy
    dst[6] = bcx
    dst[7] = bcy
    dst[8] = p2x
    dst[9] = p2y
}

/**
 * Performs the division [x]/[y] and returns the result. If the division is invalid, for instance if
 * it would leads to [Float.POSITIVE_INFINITY] or if it underflows, this function returns
 * [Float.NaN].
 */
private fun unitDivide(x: Float, y: Float): Float {
    var n = x
    var d = y

    if (n < 0) {
        n = -n
        d = -d
    }

    if (d == 0.0f || n == 0.0f || n >= d) {
        return Float.NaN
    }

    val r = n / d
    if (r == 0.0f) {
        return Float.NaN
    }

    return r
}

/**
 * Computes the winding value for a position [x]/[y] and the cubic Bézier curve defined by the
 * [points] array. The array must contain at least 8 floats defining the start, 2 control, and end
 * points of the curve as pairs of x/y coordinates.
 *
 * The [tmpCubics] array is a scratch array used to hold temporary values and must contain at least
 * 20 floats. Its content can be ignored after calling this function.
 *
 * The [tmpRoots] array is a scratch array that must contain at least 2 values. It is used to hold
 * temporary values and its content can be ignored after calling this function.
 */
internal fun cubicWinding(
    points: FloatArray,
    x: Float,
    y: Float,
    tmpCubics: FloatArray,
    tmpRoots: FloatArray
): Int {
    val splits = cubicToMonotonicCubics(points, tmpCubics, tmpRoots)

    var winding = 0
    for (i in 0..splits) {
        winding += monotonicCubicWinding(tmpCubics, i * 3 * 2, x, y)
    }
    return winding
}

/**
 * Computes the winding value for a position [x]/[y] and the cubic Bézier curve defined by the
 * [points] array, starting at the specified [offset]. The array must contain at least 10 floats
 * after [offset] defining the start, control, and end points of the curve as pairs of x/y
 * coordinates.
 */
private fun monotonicCubicWinding(points: FloatArray, offset: Int, x: Float, y: Float): Int {
    var y0 = points[offset + 1]
    var y3 = points[offset + 7]

    var direction = 1
    if (y0 > y3) {
        val swap = y3
        y3 = y0
        y0 = swap
        direction = -1
    }

    // Exclude the end point
    if (y < y0 || y >= y3) return 0

    // TODO: check if on the curve

    val x0 = points[offset + 0]
    val x1 = points[offset + 2]
    val x2 = points[offset + 4]
    val x3 = points[offset + 6]

    // Reject if outside of the bounds
    val min = fastMinOf(x0, x1, x2, x3)
    if (x < min) return 0

    val max = fastMaxOf(x0, x1, x2, x3)
    if (x > max) return direction

    // Re-fetch y0 and y3 since we may have swapped them
    y0 = points[offset + 1]
    val y1 = points[offset + 3]
    val y2 = points[offset + 5]
    y3 = points[offset + 7]

    val root =
        findFirstCubicRoot(
            y0 - y,
            y1 - y,
            y2 - y,
            y3 - y,
        )
    if (root.isNaN()) return 0

    val xt = evaluateCubic(x0, x1, x2, x3, root)
    if (xt.closeTo(x)) {
        if (x != x3 || y != y3) {
            // TODO: on the curve
            return 0
        }
    }

    return if (xt < x) direction else 0
}

/**
 * Splits the specified [cubic] Bézier curve into 1, 2, or 3 monotonic cubic Bézier curves. The
 * results are stored in the [dst] array. Both the input [cubic] and the output [dst] arrays store
 * the curves as 4 pairs of floats defined by the start, 2 control, and end points. In the [dst]
 * array, successive curves share a point: the end point of the first curve is the start point of
 * the second curve. As a result this function will output at most 20 values in the [dst] array (8
 * floats per curve, minus 2 for each shared point).
 *
 * The function returns the number of splits: if 0 is returned, the [dst] array contains a single
 * cubic curve, if 1 is returned, the array contains 2 curves with a shared point, and if 2 is
 * returned, the array contains 3 curves with 2 shared points.
 *
 * The [tmpRoot] array is a scratch array that must contain at least 2 values. It is used to hold
 * temporary values and its content can be ignored after calling this function.
 */
private fun cubicToMonotonicCubics(cubic: FloatArray, dst: FloatArray, tmpRoot: FloatArray): Int {
    val rootCount = findCubicExtremaY(cubic, tmpRoot)

    // Split the curve at the extrema
    if (rootCount == 0) {
        if (dst.size < 8) return 0
        // The cubic segment is already monotonic, copy it as-is
        cubic.copyInto(dst, 0, 0, 8)
    } else {
        var lastT = 0.0f
        var dstOffset = 0
        var src = cubic

        for (i in 0 until rootCount) {
            var t = tmpRoot[i]
            t = ((t - lastT) / (1.0f - lastT)).fastCoerceIn(0.0f, 1.0f)
            lastT = t
            splitCubicAt(src, dstOffset, dst, dstOffset, t)
            src = dst
            dstOffset += 6
        }
    }

    // NOTE: Should we flatten the extrema?

    return rootCount
}

/**
 * Finds the roots of the cubic function which coincide with the specified [cubic] Bézier curve's
 * extrema on the Y axis. The roots are written in the specified [dstRoots] array which must hold at
 * least 2 floats. This function returns the number of roots found: 0, 1, or 2.
 */
private fun findCubicExtremaY(cubic: FloatArray, dstRoots: FloatArray): Int {
    val a = cubic[1]
    val b = cubic[3]
    val c = cubic[5]
    val d = cubic[7]

    val A = d - a + 3.0f * (b - c)
    val B = 2.0f * (a - b - b - c)
    val C = b - a

    return findQuadraticRoots(A, B, C, dstRoots, 0)
}

/**
 * Splits the cubic Bézier curve, specified by 4 pairs of floats (8 values) in the [src] array
 * starting at the index [srcOffset], at position [t] (in the 0..1 range). The results are written
 * in the [dst] array starting at index [dstOffset]. This function always outputs 2 curves sharing a
 * point in the [dst] array, for a total of 14 float values: 8 for the first curve, 7 for the second
 * curve (the end point of the first curve is shared as the start point of the second curve).
 */
private fun splitCubicAt(
    src: FloatArray,
    srcOffset: Int,
    dst: FloatArray,
    dstOffset: Int,
    t: Float
) {
    if (src.size < srcOffset + 8) return
    if (dst.size < dstOffset + 14) return

    if (t >= 1.0f) {
        src.copyInto(dst, dstOffset, srcOffset, 8)
        val x = src[srcOffset + 6]
        val y = src[srcOffset + 7]
        dst[dstOffset + 8] = x
        dst[dstOffset + 9] = y
        dst[dstOffset + 10] = x
        dst[dstOffset + 11] = y
        dst[dstOffset + 12] = x
        dst[dstOffset + 13] = y
        return
    }

    val p0x = src[srcOffset + 0]
    val p0y = src[srcOffset + 1]

    dst[dstOffset + 0] = p0x
    dst[dstOffset + 1] = p0y

    val p1x = src[srcOffset + 2]
    val p1y = src[srcOffset + 3]

    val abx = lerp(p0x, p1x, t)
    val aby = lerp(p0y, p1y, t)

    dst[dstOffset + 2] = abx
    dst[dstOffset + 3] = aby

    val p2x = src[srcOffset + 4]
    val p2y = src[srcOffset + 5]

    val bcx = lerp(p1x, p2x, t)
    val bcy = lerp(p1y, p2y, t)
    val abcx = lerp(abx, bcx, t)
    val abcy = lerp(aby, bcy, t)

    dst[dstOffset + 4] = abcx
    dst[dstOffset + 5] = abcy

    val p3x = src[srcOffset + 6]
    val p3y = src[srcOffset + 7]

    val cdx = lerp(p2x, p3x, t)
    val cdy = lerp(p2y, p3y, t)
    val bcdx = lerp(bcx, cdx, t)
    val bcdy = lerp(bcy, cdy, t)
    val abcdx = lerp(abcx, bcdx, t)
    val abcdy = lerp(abcy, bcdy, t)

    dst[dstOffset + 6] = abcdx
    dst[dstOffset + 7] = abcdy

    dst[dstOffset + 8] = bcdx
    dst[dstOffset + 9] = bcdy

    dst[dstOffset + 10] = cdx
    dst[dstOffset + 11] = cdy

    dst[dstOffset + 12] = p3x
    dst[dstOffset + 13] = p3y
}

/**
 * Returns the signed area of the specified cubic Bézier curve.
 *
 * @param x0 The x coordinate of the curve's start point
 * @param y0 The y coordinate of the curve's start point
 * @param x1 The x coordinate of the curve's first control point
 * @param y1 The y coordinate of the curve's first control point
 * @param x2 The x coordinate of the curve's second control point
 * @param y2 The y coordinate of the curve's second control point
 * @param x3 The x coordinate of the curve's end point
 * @param y3 The y coordinate of the curve's end point
 */
internal fun cubicArea(
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    x3: Float,
    y3: Float
): Float {
    // See "Computing the area and winding number for a Bézier curve", Jackowski 2012
    // https://tug.org/TUGboat/tb33-1/tb103jackowski.pdf
    return ((y3 - y0) * (x1 + x2) - (x3 - x0) * (y1 + y2) + y1 * (x0 - x2) - x1 * (y0 - y2) +
        y3 * (x2 + x0 / 3.0f) - x3 * (y2 + y0 / 3.0f)) * 3.0f / 20.0f
}

private inline val PathSegment.startX: Float
    get() = points[0]

private val PathSegment.endX: Float
    get() =
        points[
            when (type) {
                PathSegment.Type.Line -> 2
                PathSegment.Type.Quadratic -> 4
                PathSegment.Type.Conic -> 4
                PathSegment.Type.Cubic -> 6
                else -> 0
            }]

private inline val PathSegment.startY: Float
    get() = points[1]

private val PathSegment.endY: Float
    get() =
        points[
            when (type) {
                PathSegment.Type.Line -> 3
                PathSegment.Type.Quadratic -> 5
                PathSegment.Type.Conic -> 5
                PathSegment.Type.Cubic -> 7
                else -> 0
            }]
