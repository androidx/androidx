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

package androidx.graphics.shapes

import kotlin.math.sqrt

/**
 * This class holds the anchor and control point data for a single cubic Bézier curve,
 * with anchor points ([anchor0X], [anchor0Y]) and ([anchor1X], [anchor1Y]) at either end
 * and control points ([control0X], [control0Y]) and ([control1X], [control1Y]) determining
 * the slope of the curve between the anchor points.
 */
open class Cubic internal constructor(internal val points: FloatArray = FloatArray(8)) {
    init { require(points.size == 8) }

    /**
     * The first anchor point x coordinate
     */
    val anchor0X get() = points[0]

    /**
     * The first anchor point y coordinate
     */
    val anchor0Y get() = points[1]

    /**
     * The first control point x coordinate
     */
    val control0X get() = points[2]

    /**
     * The first control point y coordinate
     */
    val control0Y get() = points[3]

    /**
     * The second control point x coordinate
     */
    val control1X get() = points[4]

    /**
     * The second control point y coordinate
     */
    val control1Y get() = points[5]

    /**
     * The second anchor point x coordinate
     */
    val anchor1X get() = points[6]

    /**
     * The second anchor point y coordinate
     */
    val anchor1Y get() = points[7]

    internal constructor(anchor0: Point, control0: Point, control1: Point, anchor1: Point) :
        this(floatArrayOf(anchor0.x, anchor0.y, control0.x, control0.y,
            control1.x, control1.y, anchor1.x, anchor1.y))

    /**
     * Returns a point on the curve for parameter t, representing the proportional distance
     * along the curve between its starting point at anchor0 and ending point at anchor1.
     *
     * @param t The distance along the curve between the anchor points, where 0 is at anchor0 and
     * 1 is at anchor1
     */
    internal fun pointOnCurve(t: Float): Point {
        val u = 1 - t
        return Point(anchor0X * (u * u * u) + control0X * (3 * t * u * u) +
            control1X * (3 * t * t * u) + anchor1X * (t * t * t),
            anchor0Y * (u * u * u) + control0Y * (3 * t * u * u) +
                control1Y * (3 * t * t * u) + anchor1Y * (t * t * t)
        )
    }

    /**
     * Returns two Cubics, created by splitting this curve at the given
     * distance of [t] between the original starting and ending anchor points.
     */
    // TODO: cartesian optimization?
    fun split(t: Float): Pair<Cubic, Cubic> {
        val u = 1 - t
        val pointOnCurve = pointOnCurve(t)
        return Cubic(
            anchor0X, anchor0Y,
            anchor0X * u + control0X * t, anchor0Y * u + control0Y * t,
            anchor0X * (u * u) + control0X * (2 * u * t) + control1X * (t * t),
            anchor0Y * (u * u) + control0Y * (2 * u * t) + control1Y * (t * t),
            pointOnCurve.x, pointOnCurve.y
        ) to Cubic(
            // TODO: should calculate once and share the result
            pointOnCurve.x, pointOnCurve.y,
            control0X * (u * u) + control1X * (2 * u * t) + anchor1X * (t * t),
            control0Y * (u * u) + control1Y * (2 * u * t) + anchor1Y * (t * t),
            control1X * u + anchor1X * t, control1Y * u + anchor1Y * t,
            anchor1X, anchor1Y
        )
    }

    /**
     * Utility function to reverse the control/anchor points for this curve.
     */
    fun reverse() = Cubic(anchor1X, anchor1Y, control1X, control1Y, control0X, control0Y,
        anchor0X, anchor0Y)

    /**
     * Operator overload to enable adding Cubic objects together, like "c0 + c1"
     */
    operator fun plus(o: Cubic) = Cubic(FloatArray(8) { points[it] + o.points[it] })

    /**
     * Operator overload to enable multiplying Cubics by a scalar value x, like "c0 * x"
     */
    operator fun times(x: Float) = Cubic(FloatArray(8) { points[it] * x })

    /**
     * Operator overload to enable multiplying Cubics by an Int scalar value x, like "c0 * x"
     */
    operator fun times(x: Int) = times(x.toFloat())

    /**
     * Operator overload to enable dividing Cubics by a scalar value x, like "c0 / x"
     */
    operator fun div(x: Float) = times(1f / x)

    /**
     * Operator overload to enable dividing Cubics by a scalar value x, like "c0 / x"
     */
    operator fun div(x: Int) = div(x.toFloat())

    override fun toString(): String {
        return "anchor0: ($anchor0X, $anchor0Y) control0: ($control0X, $control0Y), " +
            "control1: ($control1X, $control1Y), anchor1: ($anchor1X, $anchor1Y)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cubic

        return points.contentEquals(other.points)
    }

    /**
     * Transforms the points in this [Cubic] with the given [PointTransformer] and returns a new
     * [Cubic]
     *
     * @param f The [PointTransformer] used to transform this [Cubic]
     */
    fun transformed(f: PointTransformer): Cubic {
        val newCubic = MutableCubic()
        points.copyInto(newCubic.points)
        newCubic.transform(f)
        return newCubic
    }

    override fun hashCode() = points.contentHashCode()

    companion object {
        /**
         * Generates a bezier curve that is a straight line between the given anchor points.
         * The control points lie 1/3 of the distance from their respective anchor points.
         */
        @JvmStatic
        fun straightLine(x0: Float, y0: Float, x1: Float, y1: Float): Cubic {
            return Cubic(
                x0, y0,
                interpolate(x0, x1, 1f / 3f),
                interpolate(y0, y1, 1f / 3f),
                interpolate(x0, x1, 2f / 3f),
                interpolate(y0, y1, 2f / 3f),
                x1, y1
            )
        }

        // TODO: consider a more general function (maybe in addition to this) that allows
        // caller to get a list of curves surpassing 180 degrees
        /**
         * Generates a bezier curve that approximates a circular arc, with p0 and p1 as
         * the starting and ending anchor points. The curve generated is the smallest of
         * the two possible arcs around the entire 360-degree circle. Arcs of greater than 180
         * degrees should use more than one arc together. Note that p0 and p1 should be
         * equidistant from the center.
         */
        @JvmStatic
        fun circularArc(
            centerX: Float,
            centerY: Float,
            x0: Float,
            y0: Float,
            x1: Float,
            y1: Float
        ): Cubic {
            val p0d = directionVector(x0 - centerX, y0 - centerY)
            val p1d = directionVector(x1 - centerX, y1 - centerY)
            val rotatedP0 = p0d.rotate90()
            val rotatedP1 = p1d.rotate90()
            val clockwise = rotatedP0.dotProduct(x1 - centerX, y1 - centerY) >= 0
            val cosa = p0d.dotProduct(p1d)
            if (cosa > 0.999f) /* p0 ~= p1 */ return straightLine(x0, y0, x1, y1)
            val k = distance(x0 - centerX, y0 - centerY) * 4f / 3f *
                (sqrt(2 * (1 - cosa)) - sqrt(1 - cosa * cosa)) / (1 - cosa) *
                if (clockwise) 1f else -1f
            return Cubic(
                x0, y0, x0 + rotatedP0.x * k, y0 + rotatedP0.y * k,
                x1 - rotatedP1.x * k, y1 - rotatedP1.y * k, x1, y1
            )
        }
    }
}

/**
 * Create a Cubic that holds the anchor and control point data for a single Bézier curve,
 * with anchor points ([anchor0X], [anchor0Y]) and ([anchor1X], [anchor1Y]) at either end
 * and control points ([control0X], [control0Y]) and ([control1X], [control1Y]) determining
 * the slope of the curve between the anchor points.
 *
 * The returned instance is immutable.
 *
 * @param anchor0X the first anchor point x coordinate
 * @param anchor0Y the first anchor point y coordinate
 * @param control0X the first control point x coordinate
 * @param control0Y the first control point y coordinate
 * @param control1X the second control point x coordinate
 * @param control1Y the second control point y coordinate
 * @param anchor1X the second anchor point x coordinate
 * @param anchor1Y the second anchor point y coordinate
 */
fun Cubic(
    anchor0X: Float,
    anchor0Y: Float,
    control0X: Float,
    control0Y: Float,
    control1X: Float,
    control1Y: Float,
    anchor1X: Float,
    anchor1Y: Float
) = Cubic(floatArrayOf(anchor0X, anchor0Y, control0X, control0Y,
    control1X, control1Y, anchor1X, anchor1Y))

/**
 * This interface is used refer to Points that can be modified, as a scope to
 * [PointTransformer]
 */
interface MutablePoint {
    /**
     * The x coordinate of the Point
     */
    var x: Float

    /**
     * The y coordinate of the Point
     */
    var y: Float
}

/**
 * Interface for a function that can transform (rotate/scale/translate/etc.) points
 */
fun interface PointTransformer {
    /**
     * Transform the given [MutablePoint] in place.
     */
    fun MutablePoint.transform()
}

/**
 * This is a Mutable version of [Cubic], used mostly for performance critical paths so we can
 * avoid creating new [Cubic]s
 *
 * This is used in Morph.asMutableCubics, reusing a [MutableCubic] instance to avoid creating
 * new [Cubic]s.
 */
class MutableCubic internal constructor() : Cubic() {
    internal val anchor0 = ArrayMutablePoint(points, 0)
    internal val control0 = ArrayMutablePoint(points, 2)
    internal val control1 = ArrayMutablePoint(points, 4)
    internal val anchor1 = ArrayMutablePoint(points, 6)

    fun transform(f: PointTransformer) {
        with(f) {
            anchor0.transform()
            control0.transform()
            control1.transform()
            anchor1.transform()
        }
    }
}

/**
 * Implementation of [MutablePoint] backed by a [FloatArray], at a given position.
 * Note that the same [FloatArray] can be used to back many [ArrayMutablePoint],
 * see [MutableCubic]
 */
internal class ArrayMutablePoint(internal val arr: FloatArray, internal val ix: Int) :
    MutablePoint {
    init { require(arr.size >= ix + 2) }

    override var x: Float
        get() = arr[ix]
        set(v) {
            arr[ix] = v
        }
    override var y: Float
        get() = arr[ix + 1]
        set(v) {
            arr[ix + 1] = v
        }
}
