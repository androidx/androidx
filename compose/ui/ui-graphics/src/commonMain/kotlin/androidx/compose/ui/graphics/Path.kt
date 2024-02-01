/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.internal.JvmDefaultWithCompatibility

expect fun Path(): Path

/**
 * Create a new path, copying the contents from the src path.
 */
fun Path.copy(): Path = Path().apply { addPath(this@copy) }

@JvmDefaultWithCompatibility
/* expect class */ interface Path {
    /**
     * Specifies how closed shapes (e.g. rectangles, ovals) are wound (oriented)
     * when they are added to a path.
     */
    enum class Direction {
        /**
         * The shape is wound in counter-clockwise order.
         */
        CounterClockwise,
        /**
         * The shape is wound in clockwise order.
         */
        Clockwise
    }

    /**
     * Determines how the interior of this path is calculated.
     *
     * Defaults to the non-zero winding rule, [PathFillType.NonZero].
     */
    var fillType: PathFillType

    /**
     * Returns the path's convexity, as defined by the content of the path.
     *
     * A path is convex if it has a single contour, and only ever curves in a
     * single direction.
     *
     * This function will calculate the convexity of the path from its control
     * points, and cache the result.
     */
    val isConvex: Boolean

    /**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    val isEmpty: Boolean

    /**
     * Starts a new subpath at the given coordinate
     */
    fun moveTo(x: Float, y: Float)

    /**
     * Starts a new subpath at the given offset from the current point
     */
    fun relativeMoveTo(dx: Float, dy: Float)

    /**
     * Adds a straight line segment from the current point to the given point
     */
    fun lineTo(x: Float, y: Float)

    /**
     * Adds a straight line segment from the current point to the point
     * at the given offset from the current point.
     */
    fun relativeLineTo(dx: Float, dy: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current
     * point to the given point ([x2], [y2]), using the control point
     * ([x1], [y1]).
     */
    @Deprecated(
        "Use quadraticTo() for consistency with cubicTo()",
        replaceWith = ReplaceWith("quadraticTo(x1, y1, x2, y2)"),
        level = DeprecationLevel.WARNING
    )
    fun quadraticBezierTo(x1: Float, y1: Float, x2: Float, y2: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current
     * point to the given point ([x2], [y2]), using the control point
     * ([x1], [y1]).
     */
    fun quadraticTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        @Suppress("DEPRECATION")
        quadraticBezierTo(x1, y1, x2, y2)
    }

    /**
     * Adds a quadratic bezier segment that curves from the current
     * point to the point at the offset ([dx2], [dy2]) from the current point,
     * using the control point at the offset ([dx1], [dy1]) from the current
     * point.
     */
    @Deprecated(
        "Use relativeQuadraticTo() for consistency with relativeCubicTo()",
        replaceWith = ReplaceWith("relativeQuadraticTo(dx1, dy1, dx2, dy2)"),
        level = DeprecationLevel.WARNING
    )
    fun relativeQuadraticBezierTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float)

    /**
     * Adds a quadratic bezier segment that curves from the current
     * point to the point at the offset ([dx2], [dy2]) from the current point,
     * using the control point at the offset ([dx1], [dy1]) from the current
     * point.
     */
    fun relativeQuadraticTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float) {
        @Suppress("DEPRECATION")
        relativeQuadraticBezierTo(dx1, dy1, dx2, dy2)
    }

    /**
     * Adds a cubic bezier segment that curves from the current point
     * to the given point ([x3], [y3]), using the control points ([x1], [y1]) and
     * ([x2], [y2]).
     */
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float)

    /**
     * Adds a cubic bezier segment that curves from the current point
     * to the point at the offset ([dx3], [dy3]) from the current point, using
     * the control points at the offsets ([dx1], [dy1]) and ([dx2], [dy2]) from the
     * current point.
     */
    fun relativeCubicTo(dx1: Float, dy1: Float, dx2: Float, dy2: Float, dx3: Float, dy3: Float)

    /**
     * If the [forceMoveTo] argument is false, adds a straight line
     * segment and an arc segment.
     *
     * If the [forceMoveTo] argument is true, starts a new subpath
     * consisting of an arc segment.
     *
     * In either case, the arc segment consists of the arc that follows
     * the edge of the oval bounded by the given rectangle, from
     * startAngle radians around the oval up to startAngle + sweepAngle
     * radians around the oval, with zero radians being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval.
     *
     * The line segment added if `forceMoveTo` is false starts at the
     * current point and ends at the start of the arc.
     */
    fun arcToRad(
        rect: Rect,
        startAngleRadians: Float,
        sweepAngleRadians: Float,
        forceMoveTo: Boolean
    ) {
        arcTo(rect, degrees(startAngleRadians), degrees(sweepAngleRadians), forceMoveTo)
    }

    /**
     * If the [forceMoveTo] argument is false, adds a straight line
     * segment and an arc segment.
     *
     * If the [forceMoveTo] argument is true, starts a new subpath
     * consisting of an arc segment.
     *
     * In either case, the arc segment consists of the arc that follows
     * the edge of the oval bounded by the given rectangle, from
     * startAngle degrees around the oval up to startAngle + sweepAngle
     * degrees around the oval, with zero degrees being the point on
     * the right hand side of the oval that crosses the horizontal line
     * that intersects the center of the rectangle and with positive
     * angles going clockwise around the oval.
     *
     * The line segment added if `forceMoveTo` is false starts at the
     * current point and ends at the start of the arc.
     */
    fun arcTo(
        rect: Rect,
        startAngleDegrees: Float,
        sweepAngleDegrees: Float,
        forceMoveTo: Boolean
    )

    /**
     * Adds a new subpath that consists of four lines that outline the
     * given rectangle. The rectangle is wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addRect() with a winding direction",
        replaceWith = ReplaceWith("addRect(rect)"),
        level = DeprecationLevel.HIDDEN
    )
    fun addRect(rect: Rect)

    /**
     * Adds a new subpath that consists of four lines that outline the
     * given rectangle. The direction to wind the rectangle's contour
     * is specified by [direction].
     */
    fun addRect(rect: Rect, direction: Direction = Direction.CounterClockwise)

    /**
     * Adds a new subpath that consists of a curve that forms the
     * ellipse that fills the given rectangle.
     *
     * To add a circle, pass an appropriate rectangle as `oval`. [Rect]
     * can be used to easily describe the circle's center [Offset] and radius.
     *
     * The oval is wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addOval() with a winding direction",
        replaceWith = ReplaceWith("addOval(oval)"),
        level = DeprecationLevel.HIDDEN
    )
    fun addOval(oval: Rect)

    /**
     * Adds a new subpath that consists of a curve that forms the
     * ellipse that fills the given rectangle.
     *
     * To add a circle, pass an appropriate rectangle as `oval`. [Rect]
     * can be used to easily describe the circle's center [Offset] and radius.
     *
     * The direction to wind the rectangle's contour is specified by [direction].
     */
    fun addOval(oval: Rect, direction: Direction = Direction.CounterClockwise)

    /**
     * Add a round rectangle shape to the path from the given [RoundRect].
     * The round rectangle is wound counter-clockwise.
     */
    @Deprecated(
        "Prefer usage of addRoundRect() with a winding direction",
        replaceWith = ReplaceWith("addRoundRect(roundRect)"),
        level = DeprecationLevel.HIDDEN
    )
    fun addRoundRect(roundRect: RoundRect)

    /**
     * Add a round rectangle shape to the path from the given [RoundRect].
     * The direction to wind the rectangle's contour is specified by [direction].
     */
    fun addRoundRect(roundRect: RoundRect, direction: Direction = Direction.CounterClockwise)

    /**
     * Adds a new subpath with one arc segment that consists of the arc
     * that follows the edge of the oval bounded by the given
     * rectangle, from startAngle radians around the oval up to
     * startAngle + sweepAngle radians around the oval, with zero
     * radians being the point on the right hand side of the oval that
     * crosses the horizontal line that intersects the center of the
     * rectangle and with positive angles going clockwise around the
     * oval.
     */
    fun addArcRad(oval: Rect, startAngleRadians: Float, sweepAngleRadians: Float)

    /**
     * Adds a new subpath with one arc segment that consists of the arc
     * that follows the edge of the oval bounded by the given
     * rectangle, from startAngle degrees around the oval up to
     * startAngle + sweepAngle degrees around the oval, with zero
     * degrees being the point on the right hand side of the oval that
     * crosses the horizontal line that intersects the center of the
     * rectangle and with positive angles going clockwise around the
     * oval.
     */
    fun addArc(oval: Rect, startAngleDegrees: Float, sweepAngleDegrees: Float)

    /**
     * Adds a new subpath that consists of the given `path` offset by the given
     * `offset`.
     */
    fun addPath(path: Path, offset: Offset = Offset.Zero)

    /**
     * Closes the last subpath, as if a straight line had been drawn
     * from the current point to the first point of the subpath.
     */
    fun close()

    /**
     * Clears the [Path] object of all subpaths, returning it to the
     * same state it had when it was created. The _current point_ is
     * reset to the origin. This does NOT change the fill-type setting.
     */
    fun reset()

    /**
     * Rewinds the path: clears any lines and curves from the path but keeps the internal data
     * structure for faster reuse.
     */
    fun rewind() {
        // Call reset to avoid AbstractMethodAdded lint API errors. Implementations are already
        // calling into the respective platform Path#rewind equivalent.
        reset()
    }

    /**
     * Translates all the segments of every subpath by the given offset.
     */
    fun translate(offset: Offset)

    /**
     * Transform the points in this path by the provided matrix
     */
    fun transform(matrix: Matrix) {
        // NO-OP to ensure runtime + compile time compatibility
    }

    /**
     * Compute the bounds of the control points of the path, and write the
     * answer into bounds. If the path contains 0 or 1 points, the bounds is
     * set to (0,0,0,0)
     */
    fun getBounds(): Rect

    /**
     * Creates a new [PathIterator] for this [Path] that evaluates conics as quadratics.
     * To preserve conics, use the [Path.iterator] function that takes a
     * [PathIterator.ConicEvaluation] parameter.
     */
    operator fun iterator() = PathIterator(this)

    /**
     * Creates a new [PathIterator] for this [Path]. To preserve conics as conics (not
     * convert them to quadratics), set [conicEvaluation] to [PathIterator.ConicEvaluation.AsConic].
     *
     * @param conicEvaluation Indicates how to evaluate conic segments
     * @param tolerance When [conicEvaluation] is set to [PathIterator.ConicEvaluation.AsQuadratics]
     *        defines the maximum distance between the original conic curve and its quadratic
     *        approximations
     */
    fun iterator(
        conicEvaluation: PathIterator.ConicEvaluation,
        tolerance: Float = 0.25f
    ) = PathIterator(this, conicEvaluation, tolerance)

    /**
     * Set this path to the result of applying the Op to the two specified paths.
     * The resulting path will be constructed from non-overlapping contours.
     * The curve order is reduced where possible so that cubics may be turned
     * into quadratics, and quadratics maybe turned into lines.
     *
     * @param path1 The first operand (for difference, the minuend)
     * @param path2 The second operand (for difference, the subtrahend)
     *
     * @return True if operation succeeded, false otherwise and this path remains unmodified.
     */
    fun op(
        path1: Path,
        path2: Path,
        operation: PathOperation
    ): Boolean

    /**
     * Returns the union of two paths as a new [Path].
     */
    operator fun plus(path: Path) = Path().apply {
        op(this@Path, path, PathOperation.Union)
    }

    /**
     * Returns the difference of two paths as a new [Path].
     */
    operator fun minus(path: Path) = Path().apply {
        op(this@Path, path, PathOperation.Difference)
    }

    /**
     * Returns the union of two paths as a new [Path].
     */
    infix fun or(path: Path): Path = this + path

    /**
     * Returns the intersection of two paths as a new [Path].
     * If the paths do not intersect, returns an empty path.
     */
    infix fun and(path: Path) = Path().apply {
        op(this@Path, path, PathOperation.Intersect)
    }

    /**
     * Returns the union minus the intersection of two paths as a new [Path].
     */
    infix fun xor(path: Path) = Path().apply {
        op(this@Path, path, PathOperation.Xor)
    }

    companion object {
        /**
         * Combines the two paths according to the manner specified by the given
         * `operation`.
         *
         * The resulting path will be constructed from non-overlapping contours. The
         * curve order is reduced where possible so that cubics may be turned into
         * quadratics, and quadratics maybe turned into lines.
         *
         * Throws [IllegalArgumentException] if the combining operation fails.
         */
        fun combine(
            operation: PathOperation,
            path1: Path,
            path2: Path
        ): Path {
            val path = Path()
            if (path.op(path1, path2, operation)) {
                return path
            }

            throw IllegalArgumentException(
                "Path.combine() failed.  This may be due an invalid " +
                    "path; in particular, check for NaN values."
            )
        }
    }
}
