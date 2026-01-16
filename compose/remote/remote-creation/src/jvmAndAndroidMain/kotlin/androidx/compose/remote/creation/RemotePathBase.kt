/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression

private typealias RemotePathBaseCore = androidx.compose.remote.core.RemotePathBase

/** RemotePath implementation that manages the path buffer. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemotePathBase(protected val wrappedRemotePath: RemotePathBaseCore) {
    public companion object {
        public const val MOVE: Int = 10
        public const val LINE: Int = 11
        public const val QUADRATIC: Int = 12
        public const val CONIC: Int = 13
        public const val CUBIC: Int = 14
        public const val CLOSE: Int = 15
        public const val DONE: Int = 16
        public val MOVE_NAN: Float = Utils.asNan(MOVE)
        public val LINE_NAN: Float = Utils.asNan(LINE)
        public val QUADRATIC_NAN: Float = Utils.asNan(QUADRATIC)
        public val CONIC_NAN: Float = Utils.asNan(CONIC)
        public val CUBIC_NAN: Float = Utils.asNan(CUBIC)
        public val CLOSE_NAN: Float = Utils.asNan(CLOSE)
        public val DONE_NAN: Float = Utils.asNan(DONE)

        /** This is useful to create an approximate circle using remote float */
        @Suppress("FloatingPointLiteralPrecision")
        public fun createCirclePath(
            rc: RemoteComposeWriter,
            x: Float,
            y: Float,
            rad: Float,
        ): RemotePath {
            val k = 0.5522847498f
            val clockwise = true
            val c = rc.floatExpression(rad, k, AnimatedFloatExpression.MUL)
            val path = RemotePath()
            val xc = rc.floatExpression(x, c, AnimatedFloatExpression.ADD)
            val yc = rc.floatExpression(y, c, AnimatedFloatExpression.ADD)
            val xr = rc.floatExpression(x, rad, AnimatedFloatExpression.ADD)
            val yr = rc.floatExpression(y, rad, AnimatedFloatExpression.ADD)

            val x_c = rc.floatExpression(x, c, AnimatedFloatExpression.SUB)
            val y_c = rc.floatExpression(y, c, AnimatedFloatExpression.SUB)
            val x_r = rc.floatExpression(x, rad, AnimatedFloatExpression.SUB)
            val y_r = rc.floatExpression(y, rad, AnimatedFloatExpression.SUB)
            path.moveTo(xr, y)
            if (clockwise) {
                path.cubicTo(xr, yc, xc, yr, x, yr)
                path.cubicTo(x_c, yr, x_r, yc, x_r, y)
                path.cubicTo(x_r, y_c, x_c, y_r, x, y_r)
                path.cubicTo(xc, y_r, xr, y_c, xr, y)
            } else {
                path.moveTo(xr, y)
                path.cubicTo(xr, y_c, xc, y_r, x, y_r)
                path.cubicTo(x_c, y_r, x_r, y_c, x_r, y)
                path.cubicTo(x_r, yc, x_c, yr, x, yr)
                path.cubicTo(xc, yr, xr, yc, xr, y)
            }

            path.close()
            return path
        }
    }

    public val pathArray: FloatArray
        get() = wrappedRemotePath.path

    public val currentX: Float
        get() = wrappedRemotePath.currentX

    public val currentY: Float
        get() = wrappedRemotePath.currentY

    public val size: Int
        get() = wrappedRemotePath.size

    public constructor() : this(RemotePathBaseCore())

    public constructor(bufferSize: Int) : this(RemotePathBaseCore(bufferSize))

    public constructor(pathData: String) : this(RemotePathBaseCore(pathData))

    /** Reset the path */
    public open fun reset() {
        wrappedRemotePath.reset()
    }

    /** reserve space TODO: Do we need this function? */
    public open fun incReserve(extraPtCount: Int) {
        wrappedRemotePath.incReserve(extraPtCount)
    }

    private fun add(type: Int) {
        wrappedRemotePath.add(type)
    }

    protected fun addMove(type: Int, a1: Float, a2: Float) {
        wrappedRemotePath.add(type, a1, a2)
    }

    protected fun add(type: Int, a1: Float, a2: Float) {
        wrappedRemotePath.add(type, a1, a2)
    }

    protected fun add(type: Int, a1: Float, a2: Float, a3: Float, a4: Float) {
        wrappedRemotePath.add(type, a1, a2, a3, a4)
    }

    protected fun add(type: Int, a1: Float, a2: Float, a3: Float, a4: Float, a5: Float) {
        wrappedRemotePath.add(type, a1, a2, a3, a4, a5)
    }

    protected fun add(type: Int, a1: Float, a2: Float, a3: Float, a4: Float, a5: Float, a6: Float) {
        wrappedRemotePath.add(type, a1, a2, a3, a4, a5, a6)
    }

    /**
     * Set the beginning of the next contour to the point (x,y).\n
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
    public open fun moveTo(x: Float, y: Float) {
        wrappedRemotePath.moveTo(x, y)
    }

    /**
     * Set the beginning of the next contour relative to the last point on the previous contour. If
     * there is no previous contour, this is treated the same as moveTo().\n
     *
     * @param dx The amount to add to the x-coordinate of the end of the previous contour, to
     *   specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the previous contour, to
     *   specify the start of a new contour
     */
    public open fun rMoveTo(dx: Float, dy: Float) {
        wrappedRemotePath.rMoveTo(dx, dy)
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point (x1,y1), and ending at
     * (x2,y2). If no moveTo() call has been made for this contour, the first point is automatically
     * set to (0,0).\n
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public open fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        wrappedRemotePath.quadTo(x1, y1, x2, y2)
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.\n
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on this contour, for the
     *   control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on this contour, for the
     *   control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on this contour, for the
     *   end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on this contour, for the
     *   end point of a quadratic curve
     */
    public open fun rQuadTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        wrappedRemotePath.rQuadTo(dx1, dy1, dx2, dy2)
    }

    /**
     * Add a quadratic bezier from the last point, approaching control point (x1,y1), and ending at
     * (x2,y2), weighted by <code>weight</code>. If no moveTo() call has been made for this contour,
     * the first point is automatically set to (0,0).\n
     *
     * <p>A weight of 1 is equivalent to calling {@link #quadTo(float, float, float, float)}. A
     * weight of 0 is equivalent to calling {@link #lineTo(float, float)} to <code>(x1, y1)</code>
     * followed by {@link #lineTo(float, float)} to <code>(x2, y2)</code>.
     *
     * @param x1 The x-coordinate of the control point on a conic curve
     * @param y1 The y-coordinate of the control point on a conic curve
     * @param x2 The x-coordinate of the end point on a conic curve
     * @param y2 The y-coordinate of the end point on a conic curve
     * @param weight The weight of the conic applied to the curve. A value of 1 is equivalent to a
     *   quadratic with the given control and anchor points and a value of 0 is equivalent to a line
     *   to the first and another line to the second point.
     */
    public open fun conicTo(x1: Float, y1: Float, x2: Float, y2: Float, weight: Float) {
        wrappedRemotePath.conicTo(x1, y1, x2, y2, weight)
    }

    /**
     * Same as conicTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.\n
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on this contour, for the
     *   control point of a conic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on this contour, for the
     *   control point of a conic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on this contour, for the
     *   end point of a conic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on this contour, for the
     *   end point of a conic curve
     * @param weight The weight of the conic applied to the curve. A value of 1 is equivalent to a
     *   quadratic with the given control and anchor points and a value of 0 is equivalent to a line
     *   to the first and another line to the second point.
     */
    public open fun rConicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, weight: Float) {
        wrappedRemotePath.rConicTo(dx1, dy1, dx2, dy2, weight)
    }

    /**
     * Add a line from the last point to the specified point (x,y). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).\n
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
    public open fun lineTo(x: Float, y: Float) {
        wrappedRemotePath.lineTo(x, y)
    }

    /**
     * Same as lineTo, but the coordinates are considered relative to the last point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.\n
     *
     * @param dx The amount to add to the x-coordinate of the previous point on this contour, to
     *   specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on this contour, to
     *   specify a line
     */
    public open fun rLineTo(dx: Float, dy: Float) {
        wrappedRemotePath.rLineTo(dx, dy)
    }

    /**
     * Add a cubic bezier from the last point, approaching control points (x1,y1) and (x2,y2), and
     * ending at (x3,y3). If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).\n
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public open fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        wrappedRemotePath.cubicTo(x1, y1, x2, y2, x3, y3)
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the current point on this
     * contour. If there is no previous point, then a moveTo(0,0) is inserted automatically.\n
     */
    public open fun rCubicTo(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float,
    ) {
        wrappedRemotePath.rCubicTo(dx1, dy1, dx2, dy2, dx3, dy3)
    }

    /**
     * Close the current contour. If the current point is not equal to the first point of the
     * contour, a line segment is automatically added.
     */
    public open fun close() {
        wrappedRemotePath.close()
    }

    /**
     * Rewinds the path: clears any lines and curves from the path but keeps the internal data
     * structure for faster reuse.
     */
    public open fun rewind() {
        wrappedRemotePath.rewind()
    }

    /**
     * Returns true if the path is empty (contains no lines or curves)\n
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public open fun isEmpty(): Boolean = wrappedRemotePath.isEmpty()

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path\'s current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     */
    public open fun addArc(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
    ) {
        wrappedRemotePath.addArc(left, top, right, bottom, startAngle, sweepAngle, false)
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of the path is different
     * from the path\'s current last point, then an automatic lineTo() is added to connect the
     * current contour to the start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc. \n
     *
     * @param left left most bounds of the oval
     * @param top top most bounds of the oval
     * @param right right most bounds of the oval
     * @param bottom lowest bound of the oval
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public open fun addArc( // Made open
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float,
        forceMoveTo: Boolean,
    ) {
        wrappedRemotePath.addArc(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo)
    }

    override fun toString(): String = wrappedRemotePath.toString()

    /**
     * creates a float Array of the same size as the pathArray
     *
     * @return the array
     */
    public open fun createFloatArray(): FloatArray = wrappedRemotePath.createFloatArray()
}
