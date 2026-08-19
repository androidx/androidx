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
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import kotlin.collections.ArrayList

/** Provides a DSL to describe a vector path. */
// NotCloseable - because close here is a path operation, not resource closing
@Suppress("NotCloseable")
public class RemotePathScope constructor() {

    // 88% of Material icons use 32 or fewer path nodes
    private val _nodes = ArrayList<RemotePathNode>(32)

    /** Returns the list of [RemotePathNode] currently held in this builder. */
    internal val nodes: List<RemotePathNode>
        get() = _nodes

    /** Closes the current contour. */
    public fun close() {
        _nodes.add(RemotePathNode.Close)
    }

    /**
     * Starts a new contour at position ([x], [y]).
     *
     * @param x The x coordinate of the start of the new contour
     * @param y The y coordinate of the start of the new contour
     */
    public fun moveTo(x: RemoteFloat, y: RemoteFloat) {
        _nodes.add(RemotePathNode.MoveTo(x, y))
    }

    /**
     * Starts a new contour at [point].
     *
     * @param point The start position of the new contour
     */
    public fun moveTo(point: RemoteOffset) {
        moveTo(point.x, point.y)
    }

    /**
     * Starts a new contour at the offset ([dx], [dy]) relative to the last path position.
     *
     * @param dx The x offset of the start of the new contour, relative to the last path position
     * @param dy The y offset of the start of the new contour, relative to the last path position
     */
    public fun moveToRelative(dx: RemoteFloat, dy: RemoteFloat) {
        _nodes.add(RemotePathNode.RelativeMoveTo(dx, dy))
    }

    /**
     * Starts a new contour at [offset] relative to the last path position.
     *
     * @param offset The offset of the start of the new contour, relative to the last path position
     */
    public fun moveToRelative(offset: RemoteOffset) {
        moveToRelative(offset.x, offset.y)
    }

    /**
     * Adds a line from the last point to the position ([x], [y]). If no contour has been created by
     * calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     * @param y The y coordinate of the end of the line
     */
    public fun lineTo(x: RemoteFloat, y: RemoteFloat) {
        _nodes.add(RemotePathNode.LineTo(x, y))
    }

    /**
     * Adds a line from the last point to [point]. If no contour has been created by calling
     * [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param point The end position of the line
     */
    public fun lineTo(point: RemoteOffset) {
        lineTo(point.x, point.y)
    }

    /**
     * Adds a line from the last point to the offset ([dx], [dy]) relative to the last point. If no
     * contour has been created by calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    public fun lineToRelative(dx: RemoteFloat, dy: RemoteFloat) {
        _nodes.add(RemotePathNode.RelativeLineTo(dx, dy))
    }

    /**
     * Adds a line from the last point to [offset] relative to the last point. If no contour has
     * been created by calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param offset The offset of the end of the line, relative to the last path position
     */
    public fun lineToRelative(offset: RemoteOffset) {
        lineToRelative(offset.x, offset.y)
    }

    /**
     * Adds a horizontal line from the last point to the position ([x], `oy`), where `oy` is the y
     * coordinate of the last point. If no contour has been created by calling [moveTo] first, the
     * origin of the line is set to (0, 0).
     *
     * @param x The x coordinate of the end of the line
     */
    public fun horizontalLineTo(x: RemoteFloat) {
        _nodes.add(RemotePathNode.HorizontalTo(x))
    }

    /**
     * Adds a horizontal line from the last point to the position ([dx] `+ ox`, `oy`), where `ox`
     * and `oy` are the x and y coordinates of the last point. If no contour has been created by
     * calling [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param dx The x offset of the end of the line, relative to the last path position
     */
    public fun horizontalLineToRelative(dx: RemoteFloat) {
        _nodes.add(RemotePathNode.RelativeHorizontalTo(dx))
    }

    /**
     * Adds a vertical line from the last point to the position (`ox`, [y]), where `ox` is the x
     * coordinate of the last point. If no contour has been created by calling [moveTo] first, the
     * origin of the line is set to (0, 0).
     *
     * @param y The y coordinate of the end of the line
     */
    public fun verticalLineTo(y: RemoteFloat) {
        _nodes.add(RemotePathNode.VerticalTo(y))
    }

    /**
     * Adds a vertical line from the last point to the position (`ox`, [dy] `+ oy`), where `ox` and
     * `oy` are the x and y coordinates of the last point. If no contour has been created by calling
     * [moveTo] first, the origin of the line is set to (0, 0).
     *
     * @param dy The y offset of the end of the line, relative to the last path position
     */
    public fun verticalLineToRelative(dy: RemoteFloat) {
        _nodes.add(RemotePathNode.RelativeVerticalTo(dy))
    }

    /**
     * Adds a cubic Bézier from the last point to the position ([x3], [y3]), approaching the control
     * points ([x1], [y1]) and ([x2], [y2]). If no contour has been created by calling [moveTo]
     * first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the first control point of the cubic curve
     * @param y1 The y coordinate of the first control point of the cubic curve
     * @param x2 The x coordinate of the second control point of the cubic curve
     * @param y2 The y coordinate of the second control point of the cubic curve
     * @param x3 The x coordinate of the end point of the cubic curve
     * @param y3 The y coordinate of the end point of the cubic curve
     */
    /**
     * Adds a cubic Bézier from the last point to the position ([x3], [y3]), approaching the control
     * points ([x1], [y1]) and ([x2], [y2]). If no contour has been created by calling [moveTo]
     * first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the first control point of the cubic curve
     * @param y1 The y coordinate of the first control point of the cubic curve
     * @param x2 The x coordinate of the second control point of the cubic curve
     * @param y2 The y coordinate of the second control point of the cubic curve
     * @param x3 The x coordinate of the end point of the cubic curve
     * @param y3 The y coordinate of the end point of the cubic curve
     */
    public fun curveTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
        x3: RemoteFloat,
        y3: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.CurveTo(x1, y1, x2, y2, x3, y3))
    }

    /**
     * Adds a cubic Bézier from the last point to [end], approaching the control points [control1]
     * and [control2]. If no contour has been created by calling [moveTo] first, the origin of the
     * curve is set to (0, 0).
     *
     * @param control1 The first control point of the cubic curve
     * @param control2 The second control point of the cubic curve
     * @param end The end point of the cubic curve
     */
    public fun curveTo(control1: RemoteOffset, control2: RemoteOffset, end: RemoteOffset) {
        curveTo(control1.x, control1.y, control2.x, control2.y, end.x, end.y)
    }

    /**
     * Adds a cubic Bézier where the control and end points are defined by offsets relative to the
     * last point. If no contour has been created by calling [moveTo] first, the origin of the curve
     * is set to (0, 0).
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
    public fun curveToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
        dx3: RemoteFloat,
        dy3: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.RelativeCurveTo(dx1, dy1, dx2, dy2, dx3, dy3))
    }

    /**
     * Adds a cubic Bézier from the last point to [endOffset] relative to the last point,
     * approaching control points [control1Offset] and [control2Offset] relative to the last point.
     *
     * @param control1Offset The offset of the first control point, relative to the last path
     *   position
     * @param control2Offset The offset of the second control point, relative to the last path
     *   position
     * @param endOffset The offset of the end point, relative to the last path position
     */
    public fun curveToRelative(
        control1Offset: RemoteOffset,
        control2Offset: RemoteOffset,
        endOffset: RemoteOffset,
    ) {
        curveToRelative(
            control1Offset.x,
            control1Offset.y,
            control2Offset.x,
            control2Offset.y,
            endOffset.x,
            endOffset.y,
        )
    }

    /**
     * Adds a cubic Bézier from the last point to the position ([x2], [y2]). The first control point
     * is the reflection of the second control point of the previous command. If there is no
     * previous command or the previous command is not a cubic Bézier, the first control point is
     * set to the last path position. The second control point is defined by ([x1], [y1]). If no
     * contour has been created by calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the second control point of the cubic curve
     * @param y1 The y coordinate of the second control point of the cubic curve
     * @param x2 The x coordinate of the end point of the cubic curve
     * @param y2 The y coordinate of the end point of the cubic curve
     */
    public fun reflectiveCurveTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.ReflectiveCurveTo(x1, y1, x2, y2))
    }

    /**
     * Adds a cubic Bézier where the second control point and end points are defined by offsets
     * relative to the last point. If no contour has been created by calling [moveTo] first, the
     * origin of the curve is set to (0, 0). The reflective nature of the curve is described in
     * [reflectiveCurveTo].
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
    public fun reflectiveCurveToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.RelativeReflectiveCurveTo(dx1, dy1, dx2, dy2))
    }

    /**
     * Adds a quadratic Bézier from the last point to the position ([x2], [y2]), approaching the
     * control point ([x1], [y1]). If no contour has been created by calling [moveTo] first, the
     * origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the control point of the quadratic curve
     * @param y1 The y coordinate of the control point of the quadratic curve
     * @param x2 The x coordinate of the end point of the quadratic curve
     * @param y2 The y coordinate of the end point of the quadratic curve
     */
    public fun quadTo(x1: RemoteFloat, y1: RemoteFloat, x2: RemoteFloat, y2: RemoteFloat) {
        _nodes.add(RemotePathNode.QuadTo(x1, y1, x2, y2))
    }

    /**
     * Adds a quadratic Bézier from the last point to [end], approaching control point [control]. If
     * no contour has been created by calling [moveTo] first, the origin of the curve is set to (0,
     * 0).
     *
     * @param control The control point of the quadratic curve
     * @param end The end point of the quadratic curve
     */
    public fun quadTo(control: RemoteOffset, end: RemoteOffset) {
        quadTo(control.x, control.y, end.x, end.y)
    }

    /**
     * Adds a quadratic Bézier where the control point and end point of the curve are defined by
     * offsets relative to the last point. If no contour has been created by calling [moveTo] first,
     * the origin of the curve is set to (0, 0).
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
    public fun quadToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.RelativeQuadTo(dx1, dy1, dx2, dy2))
    }

    /**
     * Adds a quadratic Bézier from the last point to [endOffset] relative to the last point,
     * approaching control point [controlOffset] relative to the last point.
     *
     * @param controlOffset The offset of the control point, relative to the last path position
     * @param endOffset The offset of the end point, relative to the last path position
     */
    public fun quadToRelative(controlOffset: RemoteOffset, endOffset: RemoteOffset) {
        quadToRelative(controlOffset.x, controlOffset.y, endOffset.x, endOffset.y)
    }

    /**
     * Adds a quadratic Bézier from the last point to the position ([x1], [y1]). The control point
     * is the reflection of the control point of the previous command. If there is no previous
     * command or the previous command is not a quadratic Bézier, the control point is set to the
     * last path position. If no contour has been created by calling [moveTo] first, the origin of
     * the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the end point of the quadratic curve
     * @param y1 The y coordinate of the end point of the quadratic curve
     */
    public fun reflectiveQuadTo(x1: RemoteFloat, y1: RemoteFloat) {
        _nodes.add(RemotePathNode.ReflectiveQuadTo(x1, y1))
    }

    /**
     * Adds a quadratic Bézier where the end point is defined by an offset relative to the last
     * point. If no contour has been created by calling [moveTo] first, the origin of the curve is
     * set to (0, 0). The reflective nature of the curve is described in [reflectiveQuadTo].
     *
     * @param dx1 The x offset of the end point of the quadratic curve, relative to the last path
     *   position
     * @param dy1 The y offset of the end point of the quadratic curve, relative to the last path
     *   position
     */
    public fun reflectiveQuadToRelative(dx1: RemoteFloat, dy1: RemoteFloat) {
        _nodes.add(RemotePathNode.RelativeReflectiveQuadTo(dx1, dy1))
    }

    /**
     * Adds a conic Bézier from the last point to the position ([x2], [y2]), approaching the control
     * point ([x1], [y1]), with the given [weight]. If no contour has been created by calling
     * [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param x1 The x coordinate of the control point of the conic curve
     * @param y1 The y coordinate of the control point of the conic curve
     * @param x2 The x coordinate of the end point of the conic curve
     * @param y2 The y coordinate of the end point of the conic curve
     * @param weight The weight of the conic curve
     */
    public fun conicTo(
        x1: RemoteFloat,
        y1: RemoteFloat,
        x2: RemoteFloat,
        y2: RemoteFloat,
        weight: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.ConicTo(x1, y1, x2, y2, weight))
    }

    /**
     * Adds a conic Bézier from the last point to [end], approaching control point [control] with
     * the given [weight]. If no contour has been created by calling [moveTo] first, the origin of
     * the curve is set to (0, 0).
     *
     * @param control The control point of the conic curve
     * @param end The end point of the conic curve
     * @param weight The weight of the control point
     */
    public fun conicTo(control: RemoteOffset, end: RemoteOffset, weight: RemoteFloat) {
        conicTo(control.x, control.y, end.x, end.y, weight)
    }

    /**
     * Adds a conic Bézier with the given [weight] where the control point and end point of the
     * curve are defined by offsets relative to the last point. If no contour has been created by
     * calling [moveTo] first, the origin of the curve is set to (0, 0).
     *
     * @param dx1 The x offset of the control point of the conic curve, relative to the last path
     *   position
     * @param dy1 The y offset of the control point of the conic curve, relative to the last path
     *   position
     * @param dx2 The x offset of the end point of the conic curve, relative to the last path
     *   position
     * @param dy2 The y offset of the end point of the conic curve, relative to the last path
     *   position
     * @param weight The weight of the conic curve
     */
    public fun conicToRelative(
        dx1: RemoteFloat,
        dy1: RemoteFloat,
        dx2: RemoteFloat,
        dy2: RemoteFloat,
        weight: RemoteFloat,
    ) {
        _nodes.add(RemotePathNode.RelativeConicTo(dx1, dy1, dx2, dy2, weight))
    }

    /**
     * Adds a conic Bézier from the last point to [endOffset] relative to the last point,
     * approaching control point [controlOffset] relative to the last point with the given [weight].
     *
     * @param controlOffset The offset of the control point, relative to the last path position
     * @param endOffset The offset of the end point, relative to the last path position
     * @param weight The weight of the control point
     */
    public fun conicToRelative(
        controlOffset: RemoteOffset,
        endOffset: RemoteOffset,
        weight: RemoteFloat,
    ) {
        conicToRelative(controlOffset.x, controlOffset.y, endOffset.x, endOffset.y, weight)
    }

    /**
     * Adds an elliptical arc from the last point to the position ([x1], [y1]). If no contour has
     * been created by calling [moveTo] first, the origin of the arc is set to (0, 0).
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
    public fun arcTo(
        horizontalEllipseRadius: RemoteFloat,
        verticalEllipseRadius: RemoteFloat,
        theta: RemoteFloat,
        isMoreThanHalf: RemoteBoolean,
        isPositiveArc: RemoteBoolean,
        x1: RemoteFloat,
        y1: RemoteFloat,
    ) {
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
     * Adds an elliptical arc where the end point is defined by an offset relative to the last
     * point. If no contour has been created by calling [moveTo] first, the origin of the arc is set
     * to (0, 0).
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
    public fun arcToRelative(
        a: RemoteFloat,
        b: RemoteFloat,
        theta: RemoteFloat,
        isMoreThanHalf: RemoteBoolean,
        isPositiveArc: RemoteBoolean,
        dx1: RemoteFloat,
        dy1: RemoteFloat,
    ) {
        _nodes.add(
            RemotePathNode.RelativeArcTo(a, b, theta, isMoreThanHalf, isPositiveArc, dx1, dy1)
        )
    }

    /**
     * Adds an elliptical arc to the path.
     *
     * @param left The left bound of the oval defining the shape of the arc
     * @param top The top bound of the oval defining the shape of the arc
     * @param right The right bound of the oval defining the shape of the arc
     * @param bottom The bottom bound of the oval defining the shape of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public fun arcTo(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        forceMoveTo: Boolean = false,
    ) {
        _nodes.add(
            RemotePathNode.AddArc(
                left,
                top,
                right,
                bottom,
                startAngle,
                sweepAngle,
                forceMoveTo = forceMoveTo,
            )
        )
    }

    /**
     * Adds a new subpath with an arc that occupies the given rectangle bounds.
     *
     * @param left The left bound of the oval defining the shape of the arc
     * @param top The top bound of the oval defining the shape of the arc
     * @param right The right bound of the oval defining the shape of the arc
     * @param bottom The bottom bound of the oval defining the shape of the arc
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public fun addArc(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
    ) {
        arcTo(left, top, right, bottom, startAngle, sweepAngle, forceMoveTo = true)
    }

    /**
     * Adds an elliptical arc to the path.
     *
     * @param topLeft The top-left corner of the bounding oval
     * @param size The size of the bounding oval
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public fun arcTo(
        topLeft: RemoteOffset,
        size: RemoteSize,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
        forceMoveTo: Boolean = false,
    ) {
        arcTo(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            startAngle,
            sweepAngle,
            forceMoveTo = forceMoveTo,
        )
    }

    /**
     * Adds a new subpath with an arc that occupies the rectangle defined by [topLeft] and [size].
     *
     * @param topLeft The top-left corner of the bounding rectangle
     * @param size The size of the bounding rectangle
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public fun addArc(
        topLeft: RemoteOffset,
        size: RemoteSize,
        startAngle: RemoteFloat,
        sweepAngle: RemoteFloat,
    ) {
        arcTo(topLeft, size, startAngle, sweepAngle, forceMoveTo = true)
    }

    /**
     * Adds a new subpath with an arc that occupies the rectangle defined by [size] starting at
     * [RemoteOffset.Zero].
     *
     * @param size The size of the bounding rectangle
     * @param startAngle Starting angle (in degrees) where the arc begins
     * @param sweepAngle Sweep angle (in degrees) measured clockwise
     */
    public fun addArc(size: RemoteSize, startAngle: RemoteFloat, sweepAngle: RemoteFloat) {
        addArc(RemoteOffset.Zero, size, startAngle, sweepAngle)
    }

    /**
     * Adds a new subpath that consists of four lines that outline the given rectangle.
     *
     * @param left The left bound of the rectangle
     * @param top The top bound of the rectangle
     * @param right The right bound of the rectangle
     * @param bottom The bottom bound of the rectangle
     */
    public fun addRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
    ) {
        moveTo(left, top)
        lineTo(right, top)
        lineTo(right, bottom)
        lineTo(left, bottom)
        close()
    }

    /**
     * Adds a new subpath that consists of four lines that outline the rectangle defined by
     * [topLeft] and [size].
     *
     * @param topLeft The top-left corner of the rectangle
     * @param size The size of the rectangle
     */
    public fun addRect(topLeft: RemoteOffset, size: RemoteSize) {
        addRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
    }

    /**
     * Adds a new subpath that consists of four lines that outline the rectangle defined by [size]
     * starting at [RemoteOffset.Zero].
     *
     * @param size The size of the rectangle
     */
    public fun addRect(size: RemoteSize) {
        addRect(RemoteOffset.Zero, size)
    }

    /**
     * Adds a new subpath that consists of the ellipse that fills the given rectangle bounds.
     *
     * @param left The left bound of the oval
     * @param top The top bound of the oval
     * @param right The right bound of the oval
     * @param bottom The bottom bound of the oval
     */
    public fun addOval(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
    ) {
        arcTo(left, top, right, bottom, 0f.rf, 360f.rf, forceMoveTo = true)
        close()
    }

    /**
     * Adds a new subpath that consists of the ellipse that fills the rectangle defined by [topLeft]
     * and [size].
     *
     * @param topLeft The top-left corner of the bounding rectangle
     * @param size The size of the bounding rectangle
     */
    public fun addOval(topLeft: RemoteOffset, size: RemoteSize) {
        addOval(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height)
    }

    /**
     * Adds a new subpath that consists of the ellipse that fills the rectangle defined by [size]
     * starting at [RemoteOffset.Zero].
     *
     * @param size The size of the bounding rectangle
     */
    public fun addOval(size: RemoteSize) {
        addOval(RemoteOffset.Zero, size)
    }

    /**
     * Adds a new circular subpath centered at [center] with the given [radius].
     *
     * @param center The center of the circle
     * @param radius The radius of the circle
     */
    public fun addCircle(center: RemoteOffset, radius: RemoteFloat) {
        addOval(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
    }

    /**
     * Adds a new circular subpath centered at ([centerX], [centerY]) with the given [radius].
     *
     * @param centerX The x coordinate of the center of the circle
     * @param centerY The y coordinate of the center of the circle
     * @param radius The radius of the circle
     */
    public fun addCircle(centerX: RemoteFloat, centerY: RemoteFloat, radius: RemoteFloat) {
        addOval(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
    }

    /**
     * Adds a new rounded rectangle subpath to the path.
     *
     * @param left The left bound of the rounded rectangle
     * @param top The top bound of the rounded rectangle
     * @param right The right bound of the rounded rectangle
     * @param bottom The bottom bound of the rounded rectangle
     * @param radiusX The horizontal radius of the rounded corners
     * @param radiusY The vertical radius of the rounded corners
     */
    public fun addRoundRect(
        left: RemoteFloat,
        top: RemoteFloat,
        right: RemoteFloat,
        bottom: RemoteFloat,
        radiusX: RemoteFloat,
        radiusY: RemoteFloat = radiusX,
    ) {
        val rx2 = radiusX * 2f
        val ry2 = radiusY * 2f
        moveTo(left + radiusX, top)
        lineTo(right - radiusX, top)
        arcTo(right - rx2, top, right, top + ry2, 270f.rf, 90f.rf, forceMoveTo = false)
        lineTo(right, bottom - radiusY)
        arcTo(right - rx2, bottom - ry2, right, bottom, 0f.rf, 90f.rf, forceMoveTo = false)
        lineTo(left + radiusX, bottom)
        arcTo(left, bottom - ry2, left + rx2, bottom, 90f.rf, 90f.rf, forceMoveTo = false)
        lineTo(left, top + radiusY)
        arcTo(left, top, left + rx2, top + ry2, 180f.rf, 90f.rf, forceMoveTo = false)
        close()
    }

    /**
     * Adds a new rounded rectangle subpath to the path defined by [topLeft] and [size].
     *
     * @param topLeft The top-left corner of the rounded rectangle
     * @param size The size of the rounded rectangle
     * @param cornerRadius The corner radii as a [RemoteOffset] (x = horizontal radius, y = vertical
     *   radius)
     */
    public fun addRoundRect(topLeft: RemoteOffset, size: RemoteSize, cornerRadius: RemoteOffset) {
        addRoundRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            cornerRadius.x,
            cornerRadius.y,
        )
    }

    /**
     * Adds a new rounded rectangle subpath to the path defined by [size] starting at
     * [RemoteOffset.Zero].
     *
     * @param size The size of the rounded rectangle
     * @param cornerRadius The corner radii as a [RemoteOffset] (x = horizontal radius, y = vertical
     *   radius)
     */
    public fun addRoundRect(size: RemoteSize, cornerRadius: RemoteOffset) {
        addRoundRect(RemoteOffset.Zero, size, cornerRadius)
    }

    /**
     * Adds a new rounded rectangle subpath to the path defined by [topLeft] and [size].
     *
     * @param topLeft The top-left corner of the rounded rectangle
     * @param size The size of the rounded rectangle
     * @param radiusX The horizontal radius of the rounded corners
     * @param radiusY The vertical radius of the rounded corners
     */
    public fun addRoundRect(
        topLeft: RemoteOffset,
        size: RemoteSize,
        radiusX: RemoteFloat,
        radiusY: RemoteFloat = radiusX,
    ) {
        addRoundRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + size.width,
            topLeft.y + size.height,
            radiusX,
            radiusY,
        )
    }

    /**
     * Adds a new rounded rectangle subpath to the path defined by [size] starting at
     * [RemoteOffset.Zero].
     *
     * @param size The size of the rounded rectangle
     * @param radiusX The horizontal radius of the rounded corners
     * @param radiusY The vertical radius of the rounded corners
     */
    public fun addRoundRect(
        size: RemoteSize,
        radiusX: RemoteFloat,
        radiusY: RemoteFloat = radiusX,
    ) {
        addRoundRect(RemoteOffset.Zero, size, radiusX, radiusY)
    }

    /**
     * Adds the nodes from another [RemotePathScope] to this path.
     *
     * @param path The [RemotePathScope] whose nodes should be added
     */
    public fun addPath(path: RemotePathScope) {
        _nodes.addAll(path.nodes)
    }

    /**
     * Adds the [nodes] to this path.
     *
     * @param nodes The list of [RemotePathNode]s to add
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun addPath(nodes: List<RemotePathNode>) {
        _nodes.addAll(nodes)
    }
}
