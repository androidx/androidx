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

package androidx.compose.remote.creation.compose.vector

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.toRemotePath
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import kotlin.collections.ArrayList

/**
 * [RemotePathBuilder] provides a fluent API to creates a list of [RemotePathNode], used to describe
 * a path.
 */
@Suppress("NotCloseable", "TopLevelBuilder")
public class RemotePathBuilder internal constructor() {

    // 88% of Material icons use 32 or fewer path nodes
    private val _nodes = ArrayList<RemotePathNode>(32)

    /** Returns the list of [RemotePathNode] currently held in this builder. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val nodes: List<RemotePathNode>
        get() = _nodes

    /** Closes the current contour by adding a [RemotePathNode.Close] to [nodes]. */
    @Suppress("BuilderSetStyle")
    public fun close(): RemotePathBuilder = apply { _nodes.add(RemotePathNode.Close) }

    /**
     * Start a new contour at position ([x], [y]) by adding a [RemotePathNode.MoveTo] to [nodes].
     *
     * @param x The x coordinate of the start of the new contour
     * @param y The y coordinate of the start of the new contour
     */
    @Suppress("BuilderSetStyle")
    public fun moveTo(x: RemoteFloat, y: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.MoveTo(x, y))
    }

    /**
     * Start a new contour at the offset ([dx], [dy]) relative to the last path position by adding a
     * [RemotePathNode.RelativeMoveTo] to [nodes].
     *
     * @param dx The x offset of the start of the new contour, relative to the last path position
     * @param dy The y offset of the start of the new contour, relative to the last path position
     */
    @Suppress("BuilderSetStyle")
    public fun moveToRelative(dx: RemoteFloat, dy: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeMoveTo(dx, dy))
    }

    /**
     * Add a line from the last point to the position ([x], [y]) by adding a [RemotePathNode.LineTo]
     * to [nodes]. If no contour has been created by calling [moveTo] first, the origin of the line
     * is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     * @param y The y coordinate of the end of the line
     */
    @Suppress("BuilderSetStyle")
    public fun lineTo(x: RemoteFloat, y: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.LineTo(x, y))
    }

    /**
     * Add a line from the last point to the offset ([dx], [dy]) relative to the last point by
     * adding a [RemotePathNode.RelativeLineTo] to [nodes]. If no contour has been created by
     * calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    @Suppress("BuilderSetStyle")
    public fun lineToRelative(dx: RemoteFloat, dy: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeLineTo(dx, dy))
    }

    /**
     * Add a line from the last point to the position ([x], `oy`), where `oy` is the y coordinate of
     * the last point, by adding a [RemotePathNode.HorizontalTo] to [nodes]. If no contour has been
     * created by calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     */
    @Suppress("BuilderSetStyle")
    public fun horizontalLineTo(x: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.HorizontalTo(x))
    }

    /**
     * Add a line from the last point to the position ([dx] `+ ox`, `oy`), where `ox` and `oy` are
     * the x and y coordinates of the last point, by adding a [RemotePathNode.RelativeHorizontalTo]
     * to [nodes]. If no contour has been created by calling [moveTo] first, the origin of the line
     * is set to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     */
    @Suppress("BuilderSetStyle")
    public fun horizontalLineToRelative(dx: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeHorizontalTo(dx))
    }

    /**
     * Add a line from the last point to the position (`ox`, [y]), where `ox` is the x coordinate of
     * the last point, by adding a [RemotePathNode.VerticalTo] to [nodes]. If no contour has been
     * created by calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param y The y coordinate of the end of the line
     */
    @Suppress("BuilderSetStyle")
    public fun verticalLineTo(y: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.VerticalTo(y))
    }

    /**
     * Add a line from the last point to the position (`ox`, [dy] `+ oy`), where `ox` and `oy` are
     * the x and y coordinates of the last point, by adding a [RemotePathNode.RelativeVerticalTo] to
     * [nodes]. If no contour has been created by calling [moveTo] first, the origin of the line is
     * set to (0, 0).
     *
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    @Suppress("BuilderSetStyle")
    public fun verticalLineToRelative(dy: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeVerticalTo(dy))
    }

    /**
     * Add a cubic Bézier from the last point to the position ([x3], [y3]), approaching the control
     * points ([x1], [y1]) and ([x2], [y2]), by adding a [RemotePathNode.CurveTo] to [nodes]. If no
     * contour has been created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the first control point of the cubic curve
     * @param y1 The y coordinate of the first control point of the cubic curve
     * @param x2 The x coordinate of the second control point of the cubic curve
     * @param y2 The y coordinate of the second control point of the cubic curve
     * @param x3 The x coordinate of the end point of the cubic curve
     * @param y3 The y coordinate of the end point of the cubic curve
     */
    @Suppress("BuilderSetStyle")
    public fun curveTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
        x3: RemoteFloat,
        y3: RemoteFloat,
    ): RemotePathBuilder = withScope { _nodes.add(RemotePathNode.CurveTo(x1, y1, x2, y2, x3, y3)) }

    /**
     * Add a cubic Bézier by adding a [RemotePathNode.CurveTo] to [nodes]. If no contour has been
     * created by calling [moveTo] first, the origin of the curve is set to (0, 0). The cubic Bézier
     * control and end points are defined by offsets relative to the last point.
     *
     * @param dx1 The x offset of the first control point of the cubic curve, relative to the last
     *   path position
     * @param dy1 The y offset of the first control point of the cubic curve, relative to the last
     *   path position
     * @param dx2 The x offset of the second control point of the cubic curve, relative to the last
     *   path position
     * @param dy2 The y offset of the second control point of the cubic curve, relative to the last
     *   path position
     * @param dx3 The x offset of the end point of the cubic curve, relative to the last path
     *   position
     * @param dy3 The y offset of the end point of the cubic curve, relative to the last path
     *   position
     */
    @Suppress("BuilderSetStyle")
    public fun curveToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
        dx3: RemoteFloat,
        dy3: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeCurveTo(dx1, dy1, dx2, dy2, dx3, dy3))
    }

    /**
     * Add a cubic Bézier from the last point to the position ([x2], [y2]). The first control point
     * is the reflection of the second control point of the previous command. If there is no
     * previous command or the previous command is not a cubic Bézier, the first control point is
     * set to the last path position. The second control point is defined by ([x1], [y1]). Calling
     * this method adds a [RemotePathNode.ReflectiveCurveTo] to [nodes]. If no contour has been
     * created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the second control point of the cubic curve
     * @param y1 The y coordinate of the second control point of the cubic curve
     * @param x2 The x coordinate of the end point of the cubic curve
     * @param y2 The y coordinate of the end point of the cubic curve
     */
    @Suppress("BuilderSetStyle")
    public fun reflectiveCurveTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.ReflectiveCurveTo(x1, y1, x2, y2))
    }

    /**
     * Add a cubic Bézier by adding a [RemotePathNode.RelativeReflectiveCurveTo] to [nodes]. If no
     * contour has been created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     * The cubic Bézier second control point and end points are defined by offsets relative to the
     * last point. The reflective nature of the curve is described in [reflectiveCurveTo].
     *
     * @param dx1 The x offset of the second control point of the cubic curve, relative to the last
     *   path position
     * @param dy1 The y offset of the second control point of the cubic curve, relative to the last
     *   path position
     * @param dx2 The x offset of the end point of the cubic curve, relative to the last path
     *   position
     * @param dy2 The y offset of the end point of the cubic curve, relative to the last path
     *   position
     */
    @Suppress("BuilderSetStyle")
    public fun reflectiveCurveToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeReflectiveCurveTo(dx1, dy1, dx2, dy2))
    }

    /**
     * Add a quadratic Bézier from the last point to the position ([x2], [y2]), approaching the
     * control point ([x1], [y1]), by adding a [RemotePathNode.QuadTo] to [nodes]. If no contour has
     * been created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the control point of the quadratic curve
     * @param y1 The y coordinate of the control point of the quadratic curve
     * @param x2 The x coordinate of the end point of the quadratic curve
     * @param y2 The y coordinate of the end point of the quadratic curve
     */
    @Suppress("BuilderSetStyle")
    public fun quadTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
    ): RemotePathBuilder = withScope { _nodes.add(RemotePathNode.QuadTo(x1, y1, x2, y2)) }

    /**
     * Add a quadratic Bézier by adding a [RemotePathNode.RelativeQuadTo] to [nodes]. If no contour
     * has been created by calling [moveTo] first, the origin of the curve is set to (0, 0). The
     * control point and end point of the curve are defined by offsets relative to the last point.
     *
     * @param dx1 The x offset of the control point of the quadratic curve, relative to the last
     *   path position
     * @param dy1 The y offset of the control point of the quadratic curve, relative to the last
     *   path position
     * @param dx2 The x offset of the end point of the quadratic curve, relative to the last path
     *   position
     * @param dy2 The y offset of the end point of the quadratic curve, relative to the last path
     *   position
     */
    @Suppress("BuilderSetStyle")
    public fun quadToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.RelativeQuadTo(dx1, dy1, dx2, dy2))
    }

    /**
     * Add a quadratic Bézier from the last point to the position ([x1], [y1]). The control point is
     * the reflection of the control point of the previous command. If there is no previous command
     * or the previous command is not a quadratic Bézier, the control point is set to the last path
     * position. Calling this method adds a [RemotePathNode.ReflectiveQuadTo] to [nodes]. If no
     * contour has been created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the end point of the quadratic curve
     * @param y1 The y coordinate of the end point of the quadratic curve
     */
    @Suppress("BuilderSetStyle")
    public fun reflectiveQuadTo(x1: RemoteFloat, y1: RemoteFloat): RemotePathBuilder = withScope {
        _nodes.add(RemotePathNode.ReflectiveQuadTo(x1, y1))
    }

    /**
     * Add a quadratic Bézier by adding a [RemotePathNode.RelativeReflectiveQuadTo] to [nodes]. If
     * no contour has been created by calling [moveTo] first, the origin of the curve is set to (0,
     * 0). The quadratic Bézier end point is defined by an offset relative to the last point. The
     * reflective nature of the curve is described in [reflectiveQuadTo].
     *
     * @param dx1 The x offset of the end point of the quadratic curve, relative to the last path
     *   position
     * @param dy1 The y offset of the end point of the quadratic curve, relative to the last path
     *   position
     */
    @Suppress("BuilderSetStyle")
    public fun reflectiveQuadToRelative(dx1: RemoteFloat, dy1: RemoteFloat): RemotePathBuilder =
        withScope {
            _nodes.add(RemotePathNode.RelativeReflectiveQuadTo(dx1, dy1))
        }

    /**
     * Add an elliptical arc from the last point to the position ([x1], [y1]) by adding
     * [RemotePathNode.ArcTo] to [nodes]. If no contour has been created by calling [moveTo] first,
     * the origin of the arc is set to (0, 0).
     *
     * The ellipse is defined by 3 parameters:
     * - [horizontalEllipseRadius] and [verticalEllipseRadius] to define the size of the ellipse
     * - [theta] to define the orientation (as an X-axis rotation) of the ellipse
     *
     * In most situations, there are four arc candidates that can be drawn from the origin to ([x1],
     * [y1]). Which of the arcs is used is influenced by [isMoreThanHalf] and [isPositiveArc].
     *
     * When [isMoreThanHalf] is set to `true`, the added arc will be chosen amongst the two
     * candidates that represent an arc sweep greater than or equal to 180 degrees.
     *
     * When [isPositiveArc] is set to `true`, the added arc will be chosen amongst the two
     * candidates with a positive-angle direction (counter-clockwise)
     *
     * @param horizontalEllipseRadius The horizontal radius of the ellipse
     * @param verticalEllipseRadius The vertical radius of the ellipse
     * @param theta The rotation of the ellipse around the X-axis, in degrees
     * @param isMoreThanHalf Defines whether to use an arc candidate with a sweep greater than or
     *   equal to 180 degrees
     * @param isPositiveArc Defines whether to use an arc candidate that's counter-clockwise or not
     * @param x1 The x coordinate of the end point of the arc
     * @param y1 The y coordinate of the end point of the arc
     */
    @Suppress("BuilderSetStyle")
    public fun arcTo(
        horizontalEllipseRadius: RemoteFloat,
        verticalEllipseRadius: RemoteFloat,
        theta: RemoteFloat,
        isMoreThanHalf: RemoteBoolean,
        isPositiveArc: RemoteBoolean,
        x1: RemoteFloat,
        y1: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(
            RemotePathNode.ArcTo(
                horizontalEllipseRadius,
                verticalEllipseRadius,
                theta,
                isMoreThanHalf,
                isPositiveArc,
                x1,
                y1,
            )
        )
    }

    /**
     * Add an elliptical arc by adding [RemotePathNode.RelativeArcTo] to [nodes]. If no contour has
     * been created by calling [moveTo] first, the origin of the arc is set to (0, 0). The arc
     * Bézier end point is defined by an offset relative to the last point.
     *
     * The ellipse is defined by 3 parameters:
     * - [a] and [b] to define the size of the ellipse
     * - [theta] to define the orientation (as an X-axis rotation) of the ellipse
     *
     * In most situations, there are four arc candidates that can be drawn from the origin to the
     * end point. Which of the arcs is used is influenced by [isMoreThanHalf] and [isPositiveArc].
     *
     * When [isMoreThanHalf] is set to `true`, the added arc will be chosen amongst the two
     * candidates that represent an arc sweep greater than or equal to 180 degrees.
     *
     * When [isPositiveArc] is set to `true`, the added arc will be chosen amongst the two
     * candidates with a positive-angle direction (counter-clockwise)
     *
     * @param a The horizontal radius of the ellipse
     * @param b The vertical radius of the ellipse
     * @param theta The rotation of the ellipse around the X-axis, in degrees
     * @param isMoreThanHalf Defines whether to use an arc candidate with a sweep greater than or
     *   equal to 180 degrees
     * @param isPositiveArc Defines whether to use an arc candidate that's counter-clockwise or not
     * @param dx1 The x offset of the end point of the arc, relative to the last path position
     * @param dy1 The y offset of the end point of the arc, relative to the last path position
     */
    @Suppress("BuilderSetStyle")
    public fun arcToRelative(
        a: RemoteFloat,
        b: RemoteFloat,
        theta: RemoteFloat,
        isMoreThanHalf: RemoteBoolean,
        isPositiveArc: RemoteBoolean,
        dx1: RemoteFloat,
        dy1: RemoteFloat,
    ): RemotePathBuilder = withScope {
        _nodes.add(
            RemotePathNode.RelativeArcTo(a, b, theta, isMoreThanHalf, isPositiveArc, dx1, dy1)
        )
    }

    internal fun withScope(function: () -> Boolean): RemotePathBuilder = apply { function() }

    /**
     * Build the RemotePath, encoding using the given creationState.
     *
     * @param creationState the recording creation state.
     */
    public fun build(creationState: RemoteStateScope): RemotePath {
        return nodes.toRemotePath(creationState = creationState)
    }
}
