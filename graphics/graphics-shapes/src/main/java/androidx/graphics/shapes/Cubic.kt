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

import android.graphics.Matrix
import android.graphics.PointF
import kotlin.math.sqrt

/**
 * This class holds the anchor and control point data for a single cubic Bézier curve,
 * with anchor points ([anchor0X], [anchor0Y]) and ([anchor1X], [anchor1Y]) at either end
 * and control points ([control0X], [control0Y]) and ([control1X], [control1Y]) determining
 * the slope of the curve between the anchor points.
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
class Cubic(
    anchor0X: Float,
    anchor0Y: Float,
    control0X: Float,
    control0Y: Float,
    control1X: Float,
    control1Y: Float,
    anchor1X: Float,
    anchor1Y: Float
) {

    /**
     * The first anchor point x coordinate
     */
    var anchor0X: Float = anchor0X
        private set

    /**
     * The first anchor point y coordinate
     */
    var anchor0Y: Float = anchor0Y
        private set

    /**
     * The first control point x coordinate
     */
    var control0X: Float = control0X
        private set

    /**
     * The first control point y coordinate
     */
    var control0Y: Float = control0Y
        private set

    /**
     * The second control point x coordinate
     */
    var control1X: Float = control1X
        private set

    /**
     * The second control point y coordinate
     */
    var control1Y: Float = control1Y
        private set

    /**
     * The second anchor point x coordinate
     */
    var anchor1X: Float = anchor1X
        private set

    /**
     * The second anchor point y coordinate
     */
    var anchor1Y: Float = anchor1Y
        private set

    internal constructor(p0: PointF, p1: PointF, p2: PointF, p3: PointF) :
        this(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

    /**
     * Copy constructor which creates a copy of the given object.
     */
    constructor(cubic: Cubic) : this(
        cubic.anchor0X, cubic.anchor0Y, cubic.control0X, cubic.control0Y,
        cubic.control1X, cubic.control1Y, cubic.anchor1X, cubic.anchor1Y,
    )

    override fun toString(): String {
        return "p0: ($anchor0X, $anchor0Y) p1: ($control0X, $control0Y), " +
            "p2: ($control1X, $control1Y), p3: ($anchor1X, $anchor1Y)"
    }

    /**
     * Returns a point on the curve for parameter t, representing the proportional distance
     * along the curve between its starting ([anchor0X], [anchor0Y]) and ending
     * ([anchor1X], [anchor1Y]) anchor points.
     *
     * @param t The distance along the curve between the anchor points, where 0 is at
     * ([anchor0X], [anchor0Y]) and 1 is at ([control0X], [control0Y])
     * @param result Optional object to hold the result, can be passed in to avoid allocating a
     * new PointF object.
     */
    @JvmOverloads
    fun pointOnCurve(t: Float, result: PointF = PointF()): PointF {
        val u = 1 - t
        result.x = anchor0X * (u * u * u) + control0X * (3 * t * u * u) +
            control1X * (3 * t * t * u) + anchor1X * (t * t * t)
        result.y = anchor0Y * (u * u * u) + control0Y * (3 * t * u * u) +
            control1Y * (3 * t * t * u) + anchor1Y * (t * t * t)
        return result
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
    operator fun plus(o: Cubic) = Cubic(
        anchor0X + o.anchor0X, anchor0Y + o.anchor0Y,
        control0X + o.control0X, control0Y + o.control0Y,
        control1X + o.control1X, control1Y + o.control1Y,
        anchor1X + o.anchor1X, anchor1Y + o.anchor1Y
    )

    /**
     * Operator overload to enable multiplying Cubics by a scalar value x, like "c0 * x"
     */
    operator fun times(x: Float) = Cubic(
        anchor0X * x, anchor0Y * x,
        control0X * x, control0Y * x,
        control1X * x, control1Y * x,
        anchor1X * x, anchor1Y * x
    )

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

    /**
     * This function transforms this curve (its anchor and control points) with the given
     * Matrix.
     *
     * @param matrix The matrix used to transform the curve
     * @param points Optional array of Floats used internally. Supplying this array of floats saves
     * allocating the array internally when not provided. Must have size equal to or larger than 8.
     * @throws IllegalArgumentException if [points] is provided but is not large enough to
     * hold 8 values.
     */
    @JvmOverloads
    fun transform(matrix: Matrix, points: FloatArray = FloatArray(8)) {
        if (points.size < 8) {
            throw IllegalArgumentException("points array must be of size >= 8")
        }
        points[0] = anchor0X
        points[1] = anchor0Y
        points[2] = control0X
        points[3] = control0Y
        points[4] = control1X
        points[5] = control1Y
        points[6] = anchor1X
        points[7] = anchor1Y
        matrix.mapPoints(points)
        anchor0X = points[0]
        anchor0Y = points[1]
        control0X = points[2]
        control0Y = points[3]
        control1X = points[4]
        control1Y = points[5]
        anchor1X = points[6]
        anchor1Y = points[7]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cubic

        if (anchor0X != other.anchor0X) return false
        if (anchor0Y != other.anchor0Y) return false
        if (control0X != other.control0X) return false
        if (control0Y != other.control0Y) return false
        if (control1X != other.control1X) return false
        if (control1Y != other.control1Y) return false
        if (anchor1X != other.anchor1X) return false
        if (anchor1Y != other.anchor1Y) return false

        return true
    }

    override fun hashCode(): Int {
        var result = anchor0X.hashCode()
        result = 31 * result + anchor0Y.hashCode()
        result = 31 * result + control0X.hashCode()
        result = 31 * result + control0Y.hashCode()
        result = 31 * result + control1X.hashCode()
        result = 31 * result + control1Y.hashCode()
        result = 31 * result + anchor1X.hashCode()
        result = 31 * result + anchor1Y.hashCode()
        return result
    }

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

        /**
         * Creates and returns a new Cubic which is a linear interpolation between
         * [start] AND [end]. This can be used, for example, in animations to smoothly animate a
         * curve from one location and size to another.
         */
        @JvmStatic
        fun interpolate(start: Cubic, end: Cubic, t: Float): Cubic {
            return (Cubic(
                interpolate(start.anchor0X, end.anchor0X, t),
                interpolate(start.anchor0Y, end.anchor0Y, t),
                interpolate(start.control0X, end.control0X, t),
                interpolate(start.control0Y, end.control0Y, t),
                interpolate(start.control1X, end.control1X, t),
                interpolate(start.control1Y, end.control1Y, t),
                interpolate(start.anchor1X, end.anchor1X, t),
                interpolate(start.anchor1Y, end.anchor1Y, t),
            ))
        }
    }
}
