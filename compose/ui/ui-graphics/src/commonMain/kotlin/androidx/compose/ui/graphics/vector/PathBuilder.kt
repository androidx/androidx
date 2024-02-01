/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.graphics.vector

/**
 * [PathBuilder] provides a fluent API to creates a list of [PathNode], used to
 * describe a path.
 */
class PathBuilder {
    // 88% of Material icons use 32 or fewer path nodes
    private val _nodes = ArrayList<PathNode>(32)

    /**
     * Returns the list of [PathNode] currently held in this builder.
     */
    val nodes: List<PathNode>
        get() = _nodes

    /**
     * Closes the current contour by adding a [PathNode.Close] to [nodes].
     */
    fun close(): PathBuilder {
        _nodes.add(PathNode.Close)
        return this
    }

    /**
     * Start a new contour at position ([x], [y]) by adding a
     * [PathNode.MoveTo] to [nodes].
     *
     * @param x The x coordinate of the start of the new contour
     * @param y The y coordinate of the start of the new contour
     */
    fun moveTo(x: Float, y: Float): PathBuilder {
        _nodes.add(PathNode.MoveTo(x, y))
        return this
    }

    /**
     * Start a new contour at the offset ([dx], [dy]) relative to the
     * last path position by adding a [PathNode.RelativeMoveTo] to [nodes].
     *
     * @param dx The x offset of the start of the new contour, relative to the last path position
     * @param dy The y offset of the start of the new contour, relative to the last path position
     */
    fun moveToRelative(dx: Float, dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeMoveTo(dx, dy))
        return this
    }

    /**
     * Add a line from the last point to the position ([x], [y]) by adding
     * a [PathNode.LineTo] to [nodes]. If no contour has been created by calling
     * [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     * @param y The y coordinate of the end of the line
     */
    fun lineTo(x: Float, y: Float): PathBuilder {
        _nodes.add(PathNode.LineTo(x, y))
        return this
    }

    /**
     * Add a line from the last point to the offset ([dx], [dy]) relative to the
     * last point by adding a [PathNode.RelativeLineTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the line is set
     * to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    fun lineToRelative(dx: Float, dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeLineTo(dx, dy))
        return this
    }

    /**
     * Add a line from the last point to the position ([x], `oy`), where `oy` is
     * the y coordinate of the last point, by adding a [PathNode.HorizontalTo] to
     * [nodes]. If no contour has been created by calling [moveTo] first, the
     * origin of the line is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     */
    fun horizontalLineTo(x: Float): PathBuilder {
        _nodes.add(PathNode.HorizontalTo(x))
        return this
    }

    /**
     * Add a line from the last point to the position ([dx] `+ ox`, `oy`),
     * where `ox` and `oy` are the x and y coordinates of the last point,
     * by adding a [PathNode.RelativeHorizontalTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the line is
     * set to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     */
    fun horizontalLineToRelative(dx: Float): PathBuilder {
        _nodes.add(PathNode.RelativeHorizontalTo(dx))
        return this
    }

    /**
     * Add a line from the last point to the position (`ox`, [y]), where `ox` is
     * the x coordinate of the last point, by adding a [PathNode.VerticalTo] to
     * [nodes]. If no contour has been created by calling [moveTo] first, the
     * origin of the line is set to (0, 0).
     *
     * @param y The y coordinate of the end of the line
     */
    fun verticalLineTo(y: Float): PathBuilder {
        _nodes.add(PathNode.VerticalTo(y))
        return this
    }

    /**
     * Add a line from the last point to the position (`ox`, [dy] `+ oy`),
     * where `ox` and `oy` are the x and y coordinates of the last point,
     * by adding a [PathNode.RelativeVerticalTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the line is
     * set to (0, 0).
     *
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    fun verticalLineToRelative(dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeVerticalTo(dy))
        return this
    }

    /**
     * Add a cubic Bézier from the last point to the position ([x3], [y3]),
     * approaching the control points ([x1], [y1]) and ([x2], [y2]), by adding a
     * [PathNode.CurveTo] to [nodes]. If no contour has been created by calling
     * [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the first control point of the cubic curve
     * @param y1 The y coordinate of the first control point of the cubic curve
     * @param x2 The x coordinate of the second control point of the cubic curve
     * @param y2 The y coordinate of the second control point of the cubic curve
     * @param x3 The x coordinate of the end point of the cubic curve
     * @param y3 The y coordinate of the end point of the cubic curve
     */
    fun curveTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ): PathBuilder {
        _nodes.add(PathNode.CurveTo(x1, y1, x2, y2, x3, y3))
        return this
    }

    /**
     * Add a cubic Bézier by adding a [PathNode.CurveTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the curve is set to
     * (0, 0). The cubic Bézier control and end points are defined by offsets relative
     * to the last point.
     *
     * @param dx1 The x offset of the first control point of the cubic curve, relative
     *            to the last path position
     * @param dy1 The y offset of the first control point of the cubic curve, relative
     *            to the last path position
     * @param dx2 The x offset of the second control point of the cubic curve, relative
     *            to the last path position
     * @param dy2 The y offset of the second control point of the cubic curve, relative
     *            to the last path position
     * @param dx3 The x offset of the end point of the cubic curve, relative to the
     *            last path position
     * @param dy3 The y offset of the end point of the cubic curve, relative to the
     *            last path position
     */
    fun curveToRelative(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ): PathBuilder {
        _nodes.add(PathNode.RelativeCurveTo(dx1, dy1, dx2, dy2, dx3, dy3))
        return this
    }

    /**
     * Add a cubic Bézier from the last point to the position ([x2], [y2]). The first
     * control point is the reflection of the second control point of the previous
     * command. If there is no previous command or the previous command is not a cubic
     * Bézier, the first control point is set to the last path position. The second
     * control point is defined by ([x1], [y1]). Calling this method adds a
     * [PathNode.ReflectiveCurveTo] to [nodes]. If no contour has been created by calling
     * [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the second control point of the cubic curve
     * @param y1 The y coordinate of the second control point of the cubic curve
     * @param x2 The x coordinate of the end point of the cubic curve
     * @param y2 The y coordinate of the end point of the cubic curve
     */
    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float): PathBuilder {
        _nodes.add(PathNode.ReflectiveCurveTo(x1, y1, x2, y2))
        return this
    }

    /**
     * Add a cubic Bézier by adding a [PathNode.RelativeReflectiveCurveTo] to [nodes].
     * If no contour has been created by calling [moveTo] first, the origin of the
     * curve is set to (0, 0). The cubic Bézier second control point and end points
     * are defined by offsets relative to the last point. The reflective nature of
     * the curve is described in [reflectiveCurveTo].
     *
     * @param dx1 The x offset of the second control point of the cubic curve, relative
     *            to the last path position
     * @param dy1 The y offset of the second control point of the cubic curve, relative
     *            to the last path position
     * @param dx2 The x offset of the end point of the cubic curve, relative to the
     *            last path position
     * @param dy2 The y offset of the end point of the cubic curve, relative to the
     *            last path position
     */
    fun reflectiveCurveToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float): PathBuilder {
        _nodes.add(PathNode.RelativeReflectiveCurveTo(dx1, dy1, dx2, dy2))
        return this
    }

    /**
     * Add a quadratic Bézier from the last point to the position ([x2], [y2]),
     * approaching the control point ([x1], [y1]), by adding a [PathNode.QuadTo]
     * to [nodes]. If no contour has been created by calling [moveTo] first, the
     * origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the control point of the quadratic curve
     * @param y1 The y coordinate of the control point of the quadratic curve
     * @param x2 The x coordinate of the end point of the quadratic curve
     * @param y2 The y coordinate of the end point of the quadratic curve
     */
    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float): PathBuilder {
        _nodes.add(PathNode.QuadTo(x1, y1, x2, y2))
        return this
    }

    /**
     * Add a quadratic Bézier by adding a [PathNode.RelativeQuadTo] to [nodes].
     * If no contour has been created by calling [moveTo] first, the origin of
     * the curve is set to (0, 0). The control point and end point of the curve
     * are defined by offsets relative to the last point.
     *
     * @param dx1 The x offset of the control point of the quadratic curve, relative
     *            to the last path position
     * @param dy1 The y offset of the control point of the quadratic curve, relative
     *            to the last path position
     * @param dx2 The x offset of the end point of the quadratic curve, relative
     *            to the last path position
     * @param dy2 The y offset of the end point of the quadratic curve, relative
     *            to the last path position
     */
    fun quadToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float): PathBuilder {
        _nodes.add(PathNode.RelativeQuadTo(dx1, dy1, dx2, dy2))
        return this
    }

    /**
     * Add a quadratic Bézier from the last point to the position ([x1], [y1]). The
     * control point is the reflection of the control point of the previous command.
     * If there is no previous command or the previous command is not a quadratic
     * Bézier, the control point is set to the last path position. Calling this method
     * adds a [PathNode.ReflectiveQuadTo] to [nodes]. If no contour has been created
     * by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the end point of the quadratic curve
     * @param y1 The y coordinate of the end point of the quadratic curve
     */
    fun reflectiveQuadTo(x1: Float, y1: Float): PathBuilder {
        _nodes.add(PathNode.ReflectiveQuadTo(x1, y1))
        return this
    }

    /**
     * Add a quadratic Bézier by adding a [PathNode.RelativeReflectiveQuadTo] to [nodes].
     * If no contour has been created by calling [moveTo] first, the origin of the
     * curve is set to (0, 0). The quadratic Bézier end point is defined by an offset
     * relative to the last point. The reflective nature of the curve is described in
     * [reflectiveQuadTo].
     *
     * @param dx1 The x offset of the end point of the quadratic curve, relative to the
     *            last path position
     * @param dy1 The y offset of the end point of the quadratic curve, relative to the
     *            last path position
     */
    fun reflectiveQuadToRelative(dx1: Float, dy1: Float): PathBuilder {
        _nodes.add(PathNode.RelativeReflectiveQuadTo(dx1, dy1))
        return this
    }

    /**
     * Add an elliptical arc from the last point to the position ([x1], [y1]) by adding
     * [PathNode.ArcTo] to [nodes]. If no contour has been created by calling [moveTo]
     * first, the origin of the arc is set to (0, 0).
     *
     * The ellipse is defined by 3 parameters:
     * - [horizontalEllipseRadius] and [verticalEllipseRadius] to define the size of the
     *   ellipse
     * - [theta] to define the orientation (as an X-axis rotation) of the ellipse
     *
     * In most situations, there are four arc candidates that can be drawn from the origin
     * to ([x1], [y1]). Which of the arcs is used is influenced by [isMoreThanHalf] and
     * [isPositiveArc].
     *
     * When [isMoreThanHalf] is set to `true`, the added arc will be chosen amongst the
     * two candidates that represent an arc sweep greater than or equal to 180 degrees.
     *
     * When [isPositiveArc] is set to `true`, the added arc will be chosen amongst the
     * two candidates with a positive-angle direction (counter-clockwise)
     *
     * @param horizontalEllipseRadius The horizontal radius of the ellipse
     * @param verticalEllipseRadius The vertical radius of the ellipse
     * @param theta The rotation of the ellipse around the X-axis, in degrees
     * @param isMoreThanHalf Defines whether to use an arc candidate with a sweep greater
     *        than or equal to 180 degrees
     * @param isPositiveArc Defines whether to use an arc candidate that's
     *        counter-clockwise or not
     * @param x1 The x coordinate of the end point of the arc
     * @param y1 The y coordinate of the end point of the arc
     */
    fun arcTo(
        horizontalEllipseRadius: Float,
        verticalEllipseRadius: Float,
        theta: Float,
        isMoreThanHalf: Boolean,
        isPositiveArc: Boolean,
        x1: Float,
        y1: Float
    ): PathBuilder {
        _nodes.add(
            PathNode.ArcTo(
                horizontalEllipseRadius,
                verticalEllipseRadius,
                theta,
                isMoreThanHalf,
                isPositiveArc,
                x1,
                y1
            )
        )
        return this
    }

    /**
     * Add an elliptical arc by adding [PathNode.RelativeArcTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the arc is set to (0, 0).
     * The arc Bézier end point is defined by an offset relative to the last point.
     *
     * The ellipse is defined by 3 parameters:
     * - [a] and [b] to define the size of the
     *   ellipse
     * - [theta] to define the orientation (as an X-axis rotation) of the ellipse
     *
     * In most situations, there are four arc candidates that can be drawn from the origin
     * to the end point. Which of the arcs is used is influenced by [isMoreThanHalf] and
     * [isPositiveArc].
     *
     * When [isMoreThanHalf] is set to `true`, the added arc will be chosen amongst the
     * two candidates that represent an arc sweep greater than or equal to 180 degrees.
     *
     * When [isPositiveArc] is set to `true`, the added arc will be chosen amongst the
     * two candidates with a positive-angle direction (counter-clockwise)
     *
     * @param a The horizontal radius of the ellipse
     * @param b The vertical radius of the ellipse
     * @param theta The rotation of the ellipse around the X-axis, in degrees
     * @param isMoreThanHalf Defines whether to use an arc candidate with a sweep
     *        greater than or equal to 180 degrees
     * @param isPositiveArc Defines whether to use an arc candidate that's
     *        counter-clockwise or not
     * @param dx1 The x offset of the end point of the arc, relative to the last path position
     * @param dy1 The y offset of the end point of the arc, relative to the last path position
     */
    fun arcToRelative(
        a: Float,
        b: Float,
        theta: Float,
        isMoreThanHalf: Boolean,
        isPositiveArc: Boolean,
        dx1: Float,
        dy1: Float
    ): PathBuilder {
        _nodes.add(PathNode.RelativeArcTo(a, b, theta, isMoreThanHalf, isPositiveArc, dx1, dy1))
        return this
    }
}
