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
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

/**
 * Represents a single command in a vector graphics path. Each node corresponds to a command in a
 * standard path data specification.
 *
 * @property isCurve `true` if this command is a cubic Bézier curve, `false` otherwise.
 * @property isQuad `true` if this command is a quadratic Bézier curve, `false` otherwise.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public sealed class RemotePathNode(
    public val isCurve: Boolean = false,
    public val isQuad: Boolean = false,
) {

    /**
     * Closes the current subpath by drawing a straight line from the current point to the initial
     * point of the subpath. RelativeClose and Close are considered the same internally, so we
     * represent both with Close for simplicity and to make equals comparisons robust.
     *
     * Corresponds to the `Z` or `z` path data commands.
     */
    @Immutable public object Close : RemotePathNode()

    /**
     * Starts a new subpath at a point defined by a relative offset from the current point.
     * Corresponds to the `m` path data command.
     *
     * @param dx The relative change in the x-coordinate.
     * @param dy The relative change in the y-coordinate.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeMoveTo(val dx: RemoteFloat, val dy: RemoteFloat) : RemotePathNode()

    /**
     * Starts a new subpath at the given absolute (x,y) coordinate. Corresponds to the `M` path data
     * command.
     *
     * @param x The absolute x-coordinate to move to.
     * @param y The absolute y-coordinate to move to.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class MoveTo(val x: RemoteFloat, val y: RemoteFloat) : RemotePathNode()

    /**
     * Draws a line from the current point to a new point, defined by a relative offset. Corresponds
     * to the `l` path data command.
     *
     * @param dx The relative change in the x-coordinate.
     * @param dy The relative change in the y-coordinate.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeLineTo(val dx: RemoteFloat, val dy: RemoteFloat) : RemotePathNode()

    /**
     * Draws a line from the current point to the specified absolute (x,y) coordinate. Corresponds
     * to the `L` path data command.
     *
     * @param x The absolute x-coordinate of the line's end point.
     * @param y The absolute y-coordinate of the line's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class LineTo(val x: RemoteFloat, val y: RemoteFloat) : RemotePathNode()

    /**
     * Draws a horizontal line from the current point, offset by a relative distance `dx`.
     * Corresponds to the `h` path data command.
     *
     * @param dx The relative change in the x-coordinate.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeHorizontalTo(val dx: RemoteFloat) : RemotePathNode()

    /**
     * Draws a horizontal line from the current point to the specified absolute x-coordinate.
     * Corresponds to the `H` path data command.
     *
     * @param x The absolute x-coordinate of the line's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class HorizontalTo(val x: RemoteFloat) : RemotePathNode()

    /**
     * Draws a vertical line from the current point, offset by a relative distance `dy`. Corresponds
     * to the `v` path data command.
     *
     * @param dy The relative change in the y-coordinate.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeVerticalTo(val dy: RemoteFloat) : RemotePathNode()

    /**
     * Draws a vertical line from the current point to the specified absolute y-coordinate.
     * Corresponds to the `V` path data command.
     *
     * @param y The absolute y-coordinate of the line's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class VerticalTo(val y: RemoteFloat) : RemotePathNode()

    /**
     * Draws a cubic Bézier curve from the current point to a new point using relative coordinates.
     * Corresponds to the `c` path data command.
     *
     * @param dx1 The relative x-offset of the first control point.
     * @param dy1 The relative y-offset of the first control point.
     * @param dx2 The relative x-offset of the second control point.
     * @param dy2 The relative y-offset of the second control point.
     * @param dx3 The relative x-offset of the curve's end point.
     * @param dy3 The relative y-offset of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeCurveTo(
        val dx1: RemoteFloat,
        val dy1: RemoteFloat,
        val dx2: RemoteFloat,
        val dy2: RemoteFloat,
        val dx3: RemoteFloat,
        val dy3: RemoteFloat,
    ) : RemotePathNode(isCurve = true)

    /**
     * Draws a cubic Bézier curve from the current point to a new point using absolute coordinates.
     * Corresponds to the `C` path data command.
     *
     * @param x1 The absolute x-coordinate of the first control point.
     * @param y1 The absolute y-coordinate of the first control point.
     * @param x2 The absolute x-coordinate of the second control point.
     * @param y2 The absolute y-coordinate of the second control point.
     * @param x3 The absolute x-coordinate of the curve's end point.
     * @param y3 The absolute y-coordinate of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class CurveTo(
        val x1: RemoteFloat,
        val y1: RemoteFloat,
        val x2: RemoteFloat,
        val y2: RemoteFloat,
        val x3: RemoteFloat,
        val y3: RemoteFloat,
    ) : RemotePathNode(isCurve = true)

    /**
     * Draws a smooth cubic Bézier curve using relative coordinates. This command ensures a seamless
     * connection to a previous curve by inferring its first control point as a reflection of the
     * last control point of the preceding command. Corresponds to the `s` path data command.
     *
     * @param dx1 The relative x-offset of the second control point.
     * @param dy1 The relative y-offset of the second control point.
     * @param dx2 The relative x-offset of the curve's end point.
     * @param dy2 The relative y-offset of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeReflectiveCurveTo(
        val dx1: RemoteFloat,
        val dy1: RemoteFloat,
        val dx2: RemoteFloat,
        val dy2: RemoteFloat,
    ) : RemotePathNode(isCurve = true)

    /**
     * Draws a smooth cubic Bézier curve using absolute coordinates. This command ensures a seamless
     * connection to a previous curve by inferring its first control point as a reflection of the
     * last control point of the preceding command. Corresponds to the `S` path data command.
     *
     * @param x1 The absolute x-coordinate of the second control point.
     * @param y1 The absolute y-coordinate of the second control point.
     * @param x2 The absolute x-coordinate of the curve's end point.
     * @param y2 The absolute y-coordinate of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class ReflectiveCurveTo(
        val x1: RemoteFloat,
        val y1: RemoteFloat,
        val x2: RemoteFloat,
        val y2: RemoteFloat,
    ) : RemotePathNode(isCurve = true)

    /**
     * Draws a quadratic Bézier curve from the current point to a new point using relative
     * coordinates. Corresponds to the `q` path data command.
     *
     * @param dx1 The relative x-offset of the control point.
     * @param dy1 The relative y-offset of the control point.
     * @param dx2 The relative x-offset of the curve's end point.
     * @param dy2 The relative y-offset of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeQuadTo(
        val dx1: RemoteFloat,
        val dy1: RemoteFloat,
        val dx2: RemoteFloat,
        val dy2: RemoteFloat,
    ) : RemotePathNode(isQuad = true)

    /**
     * Draws a quadratic Bézier curve from the current point to a new point using absolute
     * coordinates. Corresponds to the `Q` path data command.
     *
     * @param x1 The absolute x-coordinate of the control point.
     * @param y1 The absolute y-coordinate of the control point.
     * @param x2 The absolute x-coordinate of the curve's end point.
     * @param y2 The absolute y-coordinate of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class QuadTo(
        val x1: RemoteFloat,
        val y1: RemoteFloat,
        val x2: RemoteFloat,
        val y2: RemoteFloat,
    ) : RemotePathNode(isQuad = true)

    /**
     * Draws a smooth quadratic Bézier curve using relative coordinates. This command ensures a
     * seamless connection by inferring its control point as a reflection of the control point of
     * the preceding command. Corresponds to the `t` path data command.
     *
     * @param dx The relative x-offset of the curve's end point.
     * @param dy The relative y-offset of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeReflectiveQuadTo(val dx: RemoteFloat, val dy: RemoteFloat) :
        RemotePathNode(isQuad = true)

    /**
     * Draws a smooth quadratic Bézier curve using absolute coordinates. This command ensures a
     * seamless connection by inferring its control point as a reflection of the control point of
     * the preceding command. Corresponds to the `T` path data command.
     *
     * @param x The absolute x-coordinate of the curve's end point.
     * @param y The absolute y-coordinate of the curve's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class ReflectiveQuadTo(val x: RemoteFloat, val y: RemoteFloat) :
        RemotePathNode(isQuad = true)

    /**
     * Draws an elliptical arc from the current point to a new point using relative coordinates.
     * Corresponds to the `a` path data command.
     *
     * @param horizontalEllipseRadius The radius of the ellipse on the x-axis.
     * @param verticalEllipseRadius The radius of the ellipse on the y-axis.
     * @param theta The rotation angle of the ellipse in degrees.
     * @param isMoreThanHalf If `true`, the larger of the two possible arcs is chosen.
     * @param isPositiveArc If `true`, the arc is drawn in a "positive-angle" direction.
     * @param arcStartDx The relative x-offset of the arc's end point.
     * @param arcStartDy The relative y-offset of the arc's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class RelativeArcTo(
        val horizontalEllipseRadius: RemoteFloat,
        val verticalEllipseRadius: RemoteFloat,
        val theta: RemoteFloat,
        val isMoreThanHalf: RemoteBoolean,
        val isPositiveArc: RemoteBoolean,
        val arcStartDx: RemoteFloat,
        val arcStartDy: RemoteFloat,
    ) : RemotePathNode()

    /**
     * Draws an elliptical arc from the current point to a new point using absolute coordinates.
     * Corresponds to the `A` path data command.
     *
     * @param horizontalEllipseRadius The radius of the ellipse on the x-axis.
     * @param verticalEllipseRadius The radius of the ellipse on the y-axis.
     * @param theta The rotation angle of the ellipse in degrees.
     * @param isMoreThanHalf If `true`, the larger of the two possible arcs is chosen.
     * @param isPositiveArc If `true`, the arc is drawn in a "positive-angle" direction.
     * @param arcStartX The absolute x-coordinate of the arc's end point.
     * @param arcStartY The absolute y-coordinate of the arc's end point.
     */
    @Immutable
    @Suppress("DataClassDefinition")
    public data class ArcTo(
        val horizontalEllipseRadius: RemoteFloat,
        val verticalEllipseRadius: RemoteFloat,
        val theta: RemoteFloat,
        val isMoreThanHalf: RemoteBoolean,
        val isPositiveArc: RemoteBoolean,
        val arcStartX: RemoteFloat,
        val arcStartY: RemoteFloat,
    ) : RemotePathNode()
}

/**
 * Adds the corresponding [RemotePathNode] for the given character key, if it exists, to [nodes]. If
 * the key is unknown then [IllegalArgumentException] is thrown
 *
 * @throws IllegalArgumentException
 */
internal fun Char.addPathNodes(
    nodes: ArrayList<RemotePathNode>,
    args: List<RemoteFloat>,
    count: Int,
) {
    when (this) {
        RelativeCloseKey,
        CloseKey -> nodes.add(RemotePathNode.Close)
        RelativeMoveToKey -> pathRelativeMoveNodeFromArgs(nodes, args, count)
        MoveToKey -> pathMoveNodeFromArgs(nodes, args, count)
        RelativeLineToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_LINE_TO_ARGS) { array, start ->
                RemotePathNode.RelativeLineTo(dx = array[start], dy = array[start + 1])
            }
        LineToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_LINE_TO_ARGS) { array, start ->
                RemotePathNode.LineTo(x = array[start], y = array[start + 1])
            }
        RelativeHorizontalToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_HORIZONTAL_TO_ARGS) { array, start ->
                RemotePathNode.RelativeHorizontalTo(dx = array[start])
            }
        HorizontalToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_HORIZONTAL_TO_ARGS) { array, start ->
                RemotePathNode.HorizontalTo(x = array[start])
            }
        RelativeVerticalToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_VERTICAL_TO_ARGS) { array, start ->
                RemotePathNode.RelativeVerticalTo(dy = array[start])
            }
        VerticalToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_VERTICAL_TO_ARGS) { array, start ->
                RemotePathNode.VerticalTo(y = array[start])
            }
        RelativeCurveToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_CURVE_TO_ARGS) { array, start ->
                RemotePathNode.RelativeCurveTo(
                    dx1 = array[start],
                    dy1 = array[start + 1],
                    dx2 = array[start + 2],
                    dy2 = array[start + 3],
                    dx3 = array[start + 4],
                    dy3 = array[start + 5],
                )
            }
        CurveToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_CURVE_TO_ARGS) { array, start ->
                RemotePathNode.CurveTo(
                    x1 = array[start],
                    y1 = array[start + 1],
                    x2 = array[start + 2],
                    y2 = array[start + 3],
                    x3 = array[start + 4],
                    y3 = array[start + 5],
                )
            }
        RelativeReflectiveCurveToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_REFLECTIVE_CURVE_TO_ARGS) { array, start ->
                RemotePathNode.RelativeReflectiveCurveTo(
                    dx1 = array[start],
                    dy1 = array[start + 1],
                    dx2 = array[start + 2],
                    dy2 = array[start + 3],
                )
            }
        ReflectiveCurveToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_REFLECTIVE_CURVE_TO_ARGS) { array, start ->
                RemotePathNode.ReflectiveCurveTo(
                    x1 = array[start],
                    y1 = array[start + 1],
                    x2 = array[start + 2],
                    y2 = array[start + 3],
                )
            }
        RelativeQuadToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_QUAD_TO_ARGS) { array, start ->
                RemotePathNode.RelativeQuadTo(
                    dx1 = array[start],
                    dy1 = array[start + 1],
                    dx2 = array[start + 2],
                    dy2 = array[start + 3],
                )
            }
        QuadToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_QUAD_TO_ARGS) { array, start ->
                RemotePathNode.QuadTo(
                    x1 = array[start],
                    y1 = array[start + 1],
                    x2 = array[start + 2],
                    y2 = array[start + 3],
                )
            }
        RelativeReflectiveQuadToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_REFLECTIVE_QUAD_TO_ARGS) { array, start ->
                RemotePathNode.RelativeReflectiveQuadTo(dx = array[start], dy = array[start + 1])
            }
        ReflectiveQuadToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_REFLECTIVE_QUAD_TO_ARGS) { array, start ->
                RemotePathNode.ReflectiveQuadTo(x = array[start], y = array[start + 1])
            }
        RelativeArcToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_ARC_TO_ARGS) { array, start ->
                RemotePathNode.RelativeArcTo(
                    horizontalEllipseRadius = array[start],
                    verticalEllipseRadius = array[start + 1],
                    theta = array[start + 2],
                    isMoreThanHalf = array[start + 3].ne(0.0f.rf),
                    isPositiveArc = array[start + 4].ne(0.0f.rf),
                    arcStartDx = array[start + 5],
                    arcStartDy = array[start + 6],
                )
            }
        ArcToKey ->
            pathNodesFromArgs(nodes, args, count, NUM_ARC_TO_ARGS) { array, start ->
                RemotePathNode.ArcTo(
                    horizontalEllipseRadius = array[start],
                    verticalEllipseRadius = array[start + 1],
                    theta = array[start + 2],
                    isMoreThanHalf = array[start + 3].ne(0.0f.rf),
                    isPositiveArc = array[start + 4].ne(0.0f.rf),
                    arcStartX = array[start + 5],
                    arcStartY = array[start + 6],
                )
            }
        else -> throw IllegalArgumentException("Unknown command for: $this")
    }
}

private inline fun pathNodesFromArgs(
    nodes: MutableList<RemotePathNode>,
    args: List<RemoteFloat>,
    count: Int,
    numArgs: Int,
    crossinline nodeFor: (subArray: List<RemoteFloat>, start: Int) -> RemotePathNode,
) {
    val end = count - numArgs
    var index = 0
    while (index <= end) {
        nodes.add(nodeFor(args, index))
        index += numArgs
    }
}

// According to the spec, if a MoveTo is followed by multiple pairs of coordinates,
// the subsequent pairs are treated as implicit corresponding LineTo commands.
private fun pathMoveNodeFromArgs(
    nodes: MutableList<RemotePathNode>,
    args: List<RemoteFloat>,
    count: Int,
) {
    val end = count - NUM_MOVE_TO_ARGS
    if (end >= 0) {
        nodes.add(RemotePathNode.MoveTo(args[0], args[1]))
        var index = NUM_MOVE_TO_ARGS
        while (index <= end) {
            nodes.add(RemotePathNode.LineTo(args[index], args[index + 1]))
            index += NUM_MOVE_TO_ARGS
        }
    }
}

// According to the spec, if a RelativeMoveTo is followed by multiple pairs of coordinates,
// the subsequent pairs are treated as implicit corresponding RelativeLineTo commands.
private fun pathRelativeMoveNodeFromArgs(
    nodes: MutableList<RemotePathNode>,
    args: List<RemoteFloat>,
    count: Int,
) {
    val end = count - NUM_MOVE_TO_ARGS
    if (end >= 0) {
        nodes.add(RemotePathNode.RelativeMoveTo(args[0], args[1]))
        var index = NUM_MOVE_TO_ARGS
        while (index <= end) {
            nodes.add(RemotePathNode.RelativeLineTo(args[index], args[index + 1]))
            index += NUM_MOVE_TO_ARGS
        }
    }
}

/** Constants used by [Char.addPathNodes] for creating [RemotePathNode]s from parsed paths. */
private const val RelativeCloseKey = 'z'
private const val CloseKey = 'Z'
private const val RelativeMoveToKey = 'm'
private const val MoveToKey = 'M'
private const val RelativeLineToKey = 'l'
private const val LineToKey = 'L'
private const val RelativeHorizontalToKey = 'h'
private const val HorizontalToKey = 'H'
private const val RelativeVerticalToKey = 'v'
private const val VerticalToKey = 'V'
private const val RelativeCurveToKey = 'c'
private const val CurveToKey = 'C'
private const val RelativeReflectiveCurveToKey = 's'
private const val ReflectiveCurveToKey = 'S'
private const val RelativeQuadToKey = 'q'
private const val QuadToKey = 'Q'
private const val RelativeReflectiveQuadToKey = 't'
private const val ReflectiveQuadToKey = 'T'
private const val RelativeArcToKey = 'a'
private const val ArcToKey = 'A'

/**
 * Constants for the number of expected arguments for a given node. If the number of received
 * arguments is a multiple of these, the excess will be converted into additional path nodes.
 */
private const val NUM_MOVE_TO_ARGS = 2
private const val NUM_LINE_TO_ARGS = 2
private const val NUM_HORIZONTAL_TO_ARGS = 1
private const val NUM_VERTICAL_TO_ARGS = 1
private const val NUM_CURVE_TO_ARGS = 6
private const val NUM_REFLECTIVE_CURVE_TO_ARGS = 4
private const val NUM_QUAD_TO_ARGS = 4
private const val NUM_REFLECTIVE_QUAD_TO_ARGS = 2
private const val NUM_ARC_TO_ARGS = 7
