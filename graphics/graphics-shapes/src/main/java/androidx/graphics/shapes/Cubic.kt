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
import androidx.core.graphics.minus
import androidx.core.graphics.plus
import androidx.core.graphics.times
import kotlin.math.sqrt

/**
 * This class holds the anchor and control point data for a single cubic Bézier curve.
 *
 * @param p0 the first anchor point
 * @param p1 the first control point
 * @param p2 the second control point
 * @param p3 the second anchor point
 */
class Cubic(p0: PointF, p1: PointF, p2: PointF, p3: PointF) {

    // TODO: cubic points should not be mutable. Consider switching to Floats instead of Points,
    /**
     * The first anchor point
     */
    val p0: PointF

    /**
     * The first control point
     */
    val p1: PointF

    /**
     * The second control point
     */
    val p2: PointF

    /**
     * The second anchor point
     */
    val p3: PointF

    // Defensive copy to new PointF objects
    init {
        this.p0 = PointF(p0.x, p0.y)
        this.p1 = PointF(p1.x, p1.y)
        this.p2 = PointF(p2.x, p2.y)
        this.p3 = PointF(p3.x, p3.y)
    }

    /**
     * Copy constructor which creates a copy of the given object.
     */
    constructor(cubic: Cubic) : this(cubic.p0, cubic.p1, cubic.p2, cubic.p3)

    override fun toString(): String {
        return "p0: $p0, p1: $p1, p2: $p2, p3: $p3"
    }

    /**
     * Returns a point on the curve for parameter t, representing the proportional distance
     * along the curve between its starting ([p0]) and ending ([p3]) anchor points.
     *
     * @param t The distance along the curve between the anchor points, where 0 is at [p0] and
     * 1 is at [p1]
     * @param result Optional object to hold the result, can be passed in to avoid allocating a
     * new PointF object.
     */
    @JvmOverloads
    fun pointOnCurve(t: Float, result: PointF = PointF()): PointF {
        val u = 1 - t
        result.x = p0.x * (u * u * u) + p1.x * (3 * t * u * u) +
            p2.x * (3 * t * t * u) + p3.x * (t * t * t)
        result.y = p0.y * (u * u * u) + p1.y * (3 * t * u * u) +
            p2.y * (3 * t * t * u) + p3.y * (t * t * t)
        return result
    }

    /**
     * Returns two Cubics, created by splitting this curve at the given
     * distance of [t] between the original starting and ending anchor points.
     */
    // TODO: cartesian optimization?
    fun split(t: Float): Pair<Cubic, Cubic> {
        val u = 1 - t
        return Cubic(
            p0,
            p0 * u + p1 * t,
            p0 * (u * u) + p1 * (2 * u * t) + p2 * (t * t),
            pointOnCurve(t)
        ) to Cubic(
            // TODO: should calculate once and share the result
            pointOnCurve(t),
            p1 * (u * u) + p2 * (2 * u * t) + p3 * (t * t),
            p2 * u + p3 * t,
            p3
        )
    }

    /**
     * Utility function to reverse the control/anchor points for this curve.
     */
    fun reverse() = Cubic(p3, p2, p1, p0)

    /**
     * Operator overload to enable adding Cubic objects together, like "c0 + c1"
     */
    operator fun plus(o: Cubic) = Cubic(p0 + o.p0, p1 + o.p1, p2 + o.p2, p3 + o.p3)

    /**
     * Operator overload to enable multiplying Cubics by a scalar value x, like "c0 * x"
     */
    operator fun times(x: Float) = Cubic(p0 * x, p1 * x, p2 * x, p3 * x)

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
        points[0] = p0.x
        points[1] = p0.y
        points[2] = p1.x
        points[3] = p1.y
        points[4] = p2.x
        points[5] = p2.y
        points[6] = p3.x
        points[7] = p3.y
        matrix.mapPoints(points)
        p0.x = points[0]
        p0.y = points[1]
        p1.x = points[2]
        p1.y = points[3]
        p2.x = points[4]
        p2.y = points[5]
        p3.x = points[6]
        p3.y = points[7]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cubic

        if (p0 != other.p0) return false
        if (p1 != other.p1) return false
        if (p2 != other.p2) return false
        if (p3 != other.p3) return false

        return true
    }

    override fun hashCode(): Int {
        var result = p0.hashCode()
        result = 31 * result + p1.hashCode()
        result = 31 * result + p2.hashCode()
        result = 31 * result + p3.hashCode()
        return result
    }

    companion object {
        /**
         * Generates a bezier curve that is a straight line between the given anchor points.
         * The control points lie 1/3 of the distance from their respective anchor points.
         */
        @JvmStatic
        fun straightLine(p0: PointF, p1: PointF): Cubic {
            return Cubic(
                p0,
                interpolate(p0, p1, 1f / 3f),
                interpolate(p0, p1, 2f / 3f),
                p1
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
        fun circularArc(center: PointF, p0: PointF, p1: PointF): Cubic {
            val p0d = (p0 - center).getDirection()
            val p1d = (p1 - center).getDirection()
            p0d.rotate90()
            val clockwise = p0d.rotate90().dotProduct(p1 - center) >= 0
            val cosa = p0d.dotProduct(p1d)
            if (cosa > 0.999f) /* p0 ~= p1 */ return straightLine(p0, p1)
            val k = (p0 - center).getDistance() * 4f / 3f *
                    (sqrt(2 * (1 - cosa)) - sqrt(1 - cosa * cosa)) / (1 - cosa) *
                    if (clockwise) 1f else -1f
            return Cubic(p0, p0 + p0d.rotate90() * k, p1 - p1d.rotate90() * k, p1)
        }

        /**
         * Creates and returns a new Cubic which is a linear interpolation between
         * [start] AND [end]. This can be used, for example, in animations to smoothly animate a
         * curve from one location and size to another.
         */
        @JvmStatic
        fun interpolate(start: Cubic, end: Cubic, t: Float): Cubic {
            return (Cubic(
                interpolate(start.p0, end.p0, t),
                interpolate(start.p1, end.p1, t),
                interpolate(start.p2, end.p2, t),
                interpolate(start.p3, end.p3, t),
            ))
        }
    }
}