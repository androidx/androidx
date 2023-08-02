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
 * with anchor points ([anchorX0], [anchorY0]) and ([anchorX1], [anchorY1]) at either end
 * and control points ([controlX0], [controlY0]) and ([controlX1], [controlY1]) determining
 * the slope of the curve between the anchor points.
 *
 * @param anchorX0 the first anchor point x coordinate
 * @param anchorY0 the first anchor point y coordinate
 * @param controlX0 the first control point x coordinate
 * @param controlY0 the first control point y coordinate
 * @param controlX1 the second control point x coordinate
 * @param controlY1 the second control point y coordinate
 * @param anchorX1 the second anchor point x coordinate
 * @param anchorY1 the second anchor point y coordinate
 */
class Cubic(
    anchorX0: Float,
    anchorY0: Float,
    controlX0: Float,
    controlY0: Float,
    controlX1: Float,
    controlY1: Float,
    anchorX1: Float,
    anchorY1: Float
) {

    /**
     * The first anchor point x coordinate
     */
    var anchorX0: Float = anchorX0
        private set

    /**
     * The first anchor point y coordinate
     */
    var anchorY0: Float = anchorY0
        private set

    /**
     * The first control point x coordinate
     */
    var controlX0: Float = controlX0
        private set

    /**
     * The first control point y coordinate
     */
    var controlY0: Float = controlY0
        private set

    /**
     * The second control point x coordinate
     */
    var controlX1: Float = controlX1
        private set

    /**
     * The second control point y coordinate
     */
    var controlY1: Float = controlY1
        private set

    /**
     * The second anchor point x coordinate
     */
    var anchorX1: Float = anchorX1
        private set

    /**
     * The second anchor point y coordinate
     */
    var anchorY1: Float = anchorY1
        private set

    internal constructor(p0: PointF, p1: PointF, p2: PointF, p3: PointF) :
        this(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)

    /**
     * Copy constructor which creates a copy of the given object.
     */
    constructor(cubic: Cubic) : this(
        cubic.anchorX0, cubic.anchorY0, cubic.controlX0, cubic.controlY0,
        cubic.controlX1, cubic.controlY1, cubic.anchorX1, cubic.anchorY1,
    )

    override fun toString(): String {
        return "p0: ($anchorX0, $anchorY0) p1: ($controlX0, $controlY0), " +
            "p2: ($controlX1, $controlY1), p3: ($anchorX1, $anchorY1)"
    }

    /**
     * Returns a point on the curve for parameter t, representing the proportional distance
     * along the curve between its starting ([anchorX0], [anchorY0]) and ending
     * ([anchorX1], [anchorY1]) anchor points.
     *
     * @param t The distance along the curve between the anchor points, where 0 is at
     * ([anchorX0], [anchorY0]) and 1 is at ([controlX0], [controlY0])
     * @param result Optional object to hold the result, can be passed in to avoid allocating a
     * new PointF object.
     */
    @JvmOverloads
    fun pointOnCurve(t: Float, result: PointF = PointF()): PointF {
        val u = 1 - t
        result.x = anchorX0 * (u * u * u) + controlX0 * (3 * t * u * u) +
            controlX1 * (3 * t * t * u) + anchorX1 * (t * t * t)
        result.y = anchorY0 * (u * u * u) + controlY0 * (3 * t * u * u) +
            controlY1 * (3 * t * t * u) + anchorY1 * (t * t * t)
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
            anchorX0, anchorY0,
            anchorX0 * u + controlX0 * t, anchorY0 * u + controlY0 * t,
            anchorX0 * (u * u) + controlX0 * (2 * u * t) + controlX1 * (t * t),
            anchorY0 * (u * u) + controlY0 * (2 * u * t) + controlY1 * (t * t),
            pointOnCurve.x, pointOnCurve.y
        ) to Cubic(
            // TODO: should calculate once and share the result
            pointOnCurve.x, pointOnCurve.y,
            controlX0 * (u * u) + controlX1 * (2 * u * t) + anchorX1 * (t * t),
            controlY0 * (u * u) + controlY1 * (2 * u * t) + anchorY1 * (t * t),
            controlX1 * u + anchorX1 * t, controlY1 * u + anchorY1 * t,
            anchorX1, anchorY1
        )
    }

    /**
     * Utility function to reverse the control/anchor points for this curve.
     */
    fun reverse() = Cubic(anchorX1, anchorY1, controlX1, controlY1, controlX0, controlY0,
        anchorX0, anchorY0)

    /**
     * Operator overload to enable adding Cubic objects together, like "c0 + c1"
     */
    operator fun plus(o: Cubic) = Cubic(
        anchorX0 + o.anchorX0, anchorY0 + o.anchorY0,
        controlX0 + o.controlX0, controlY0 + o.controlY0,
        controlX1 + o.controlX1, controlY1 + o.controlY1,
        anchorX1 + o.anchorX1, anchorY1 + o.anchorY1
    )

    /**
     * Operator overload to enable multiplying Cubics by a scalar value x, like "c0 * x"
     */
    operator fun times(x: Float) = Cubic(
        anchorX0 * x, anchorY0 * x,
        controlX0 * x, controlY0 * x,
        controlX1 * x, controlY1 * x,
        anchorX1 * x, anchorY1 * x
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
        points[0] = anchorX0
        points[1] = anchorY0
        points[2] = controlX0
        points[3] = controlY0
        points[4] = controlX1
        points[5] = controlY1
        points[6] = anchorX1
        points[7] = anchorY1
        matrix.mapPoints(points)
        anchorX0 = points[0]
        anchorY0 = points[1]
        controlX0 = points[2]
        controlY0 = points[3]
        controlX1 = points[4]
        controlY1 = points[5]
        anchorX1 = points[6]
        anchorY1 = points[7]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Cubic

        if (anchorX0 != other.anchorX0) return false
        if (anchorY0 != other.anchorY0) return false
        if (controlX0 != other.controlX0) return false
        if (controlY0 != other.controlY0) return false
        if (controlX1 != other.controlX1) return false
        if (controlY1 != other.controlY1) return false
        if (anchorX1 != other.anchorX1) return false
        if (anchorY1 != other.anchorY1) return false

        return true
    }

    override fun hashCode(): Int {
        var result = anchorX0.hashCode()
        result = 31 * result + anchorY0.hashCode()
        result = 31 * result + controlX0.hashCode()
        result = 31 * result + controlY0.hashCode()
        result = 31 * result + controlX1.hashCode()
        result = 31 * result + controlY1.hashCode()
        result = 31 * result + anchorX1.hashCode()
        result = 31 * result + anchorY1.hashCode()
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
                interpolate(start.anchorX0, end.anchorX0, t),
                interpolate(start.anchorY0, end.anchorY0, t),
                interpolate(start.controlX0, end.controlX0, t),
                interpolate(start.controlY0, end.controlY0, t),
                interpolate(start.controlX1, end.controlX1, t),
                interpolate(start.controlY1, end.controlY1, t),
                interpolate(start.anchorX1, end.anchorX1, t),
                interpolate(start.anchorY1, end.anchorY1, t),
            ))
        }
    }
}
