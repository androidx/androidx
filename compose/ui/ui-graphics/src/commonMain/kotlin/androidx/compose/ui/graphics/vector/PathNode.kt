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

import androidx.compose.runtime.Immutable

/**
 * Class representing a singular path command in a vector.
 *
 * @property isCurve whether this command is a curve command
 * @property isQuad whether this command is a quad command
 */
@Immutable
sealed class PathNode(val isCurve: Boolean = false, val isQuad: Boolean = false) {
    // RelativeClose and Close are considered the same internally, so we represent both with Close
    // for simplicity and to make equals comparisons robust.
    @Immutable
    object Close : PathNode()

    @Immutable
    data class RelativeMoveTo(val dx: Float, val dy: Float) : PathNode()

    @Immutable
    data class MoveTo(val x: Float, val y: Float) : PathNode()

    @Immutable
    data class RelativeLineTo(val dx: Float, val dy: Float) : PathNode()

    @Immutable
    data class LineTo(val x: Float, val y: Float) : PathNode()

    @Immutable
    data class RelativeHorizontalTo(val dx: Float) : PathNode()

    @Immutable
    data class HorizontalTo(val x: Float) : PathNode()

    @Immutable
    data class RelativeVerticalTo(val dy: Float) : PathNode()

    @Immutable
    data class VerticalTo(val y: Float) : PathNode()

    @Immutable
    data class RelativeCurveTo(
        val dx1: Float,
        val dy1: Float,
        val dx2: Float,
        val dy2: Float,
        val dx3: Float,
        val dy3: Float
    ) : PathNode(isCurve = true)

    @Immutable
    data class CurveTo(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val x3: Float,
        val y3: Float
    ) : PathNode(isCurve = true)

    @Immutable
    data class RelativeReflectiveCurveTo(
        val dx1: Float,
        val dy1: Float,
        val dx2: Float,
        val dy2: Float
    ) : PathNode(isCurve = true)

    @Immutable
    data class ReflectiveCurveTo(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    ) : PathNode(isCurve = true)

    @Immutable
    data class RelativeQuadTo(
        val dx1: Float,
        val dy1: Float,
        val dx2: Float,
        val dy2: Float
    ) : PathNode(isQuad = true)

    @Immutable
    data class QuadTo(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    ) : PathNode(isQuad = true)

    @Immutable
    data class RelativeReflectiveQuadTo(
        val dx: Float,
        val dy: Float
    ) : PathNode(isQuad = true)

    @Immutable
    data class ReflectiveQuadTo(
        val x: Float,
        val y: Float
    ) : PathNode(isQuad = true)

    @Immutable
    data class RelativeArcTo(
        val horizontalEllipseRadius: Float,
        val verticalEllipseRadius: Float,
        val theta: Float,
        val isMoreThanHalf: Boolean,
        val isPositiveArc: Boolean,
        val arcStartDx: Float,
        val arcStartDy: Float
    ) : PathNode()

    @Immutable
    data class ArcTo(
        val horizontalEllipseRadius: Float,
        val verticalEllipseRadius: Float,
        val theta: Float,
        val isMoreThanHalf: Boolean,
        val isPositiveArc: Boolean,
        val arcStartX: Float,
        val arcStartY: Float
    ) : PathNode()
}

/**
 * Adds the corresponding [PathNode] for the given character key, if it exists, to [nodes].
 * If the key is unknown then [IllegalArgumentException] is thrown
 * @throws IllegalArgumentException
 */
internal fun Char.addPathNodes(nodes: MutableList<PathNode>, args: FloatArray, count: Int) {
    when (this) {
        RelativeCloseKey, CloseKey -> nodes.add(PathNode.Close)

        RelativeMoveToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_MOVE_TO_ARGS
        ) { array, start ->
            PathNode.RelativeMoveTo(dx = array[start], dy = array[start + 1])
        }

        MoveToKey -> pathNodesFromArgs(nodes, args, count, NUM_MOVE_TO_ARGS) { array, start ->
            PathNode.MoveTo(x = array[start], y = array[start + 1])
        }

        RelativeLineToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_LINE_TO_ARGS
        ) { array, start ->
            PathNode.RelativeLineTo(dx = array[start], dy = array[start + 1])
        }

        LineToKey -> pathNodesFromArgs(nodes, args, count, NUM_LINE_TO_ARGS) { array, start ->
            PathNode.LineTo(x = array[start], y = array[start + 1])
        }

        RelativeHorizontalToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_HORIZONTAL_TO_ARGS
        ) { array, start ->
            PathNode.RelativeHorizontalTo(dx = array[start])
        }

        HorizontalToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_HORIZONTAL_TO_ARGS
        ) { array, start ->
            PathNode.HorizontalTo(x = array[start])
        }

        RelativeVerticalToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_VERTICAL_TO_ARGS
        ) { array, start ->
            PathNode.RelativeVerticalTo(dy = array[start])
        }

        VerticalToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_VERTICAL_TO_ARGS
        ) { array, start ->
            PathNode.VerticalTo(y = array[start])
        }

        RelativeCurveToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_CURVE_TO_ARGS
        ) { array, start ->
            PathNode.RelativeCurveTo(
                dx1 = array[start],
                dy1 = array[start + 1],
                dx2 = array[start + 2],
                dy2 = array[start + 3],
                dx3 = array[start + 4],
                dy3 = array[start + 5]
            )
        }

        CurveToKey -> pathNodesFromArgs(nodes, args, count, NUM_CURVE_TO_ARGS) { array, start ->
            PathNode.CurveTo(
                x1 = array[start],
                y1 = array[start + 1],
                x2 = array[start + 2],
                y2 = array[start + 3],
                x3 = array[start + 4],
                y3 = array[start + 5]
            )
        }

        RelativeReflectiveCurveToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_REFLECTIVE_CURVE_TO_ARGS
        ) { array, start ->
            PathNode.RelativeReflectiveCurveTo(
                dx1 = array[start],
                dy1 = array[start + 1],
                dx2 = array[start + 2],
                dy2 = array[start + 3]
            )
        }

        ReflectiveCurveToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_REFLECTIVE_CURVE_TO_ARGS
        ) { array, start ->
            PathNode.ReflectiveCurveTo(
                x1 = array[start],
                y1 = array[start + 1],
                x2 = array[start + 2],
                y2 = array[start + 3]
            )
        }

        RelativeQuadToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_QUAD_TO_ARGS
        ) { array, start ->
            PathNode.RelativeQuadTo(
                dx1 = array[start],
                dy1 = array[start + 1],
                dx2 = array[start + 2],
                dy2 = array[start + 3]
            )
        }

        QuadToKey -> pathNodesFromArgs(nodes, args, count, NUM_QUAD_TO_ARGS) { array, start ->
            PathNode.QuadTo(
                x1 = array[start],
                y1 = array[start + 1],
                x2 = array[start + 2],
                y2 = array[start + 3]
            )
        }

        RelativeReflectiveQuadToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_REFLECTIVE_QUAD_TO_ARGS
        ) { array, start ->
            PathNode.RelativeReflectiveQuadTo(dx = array[start], dy = array[start + 1])
        }

        ReflectiveQuadToKey -> pathNodesFromArgs(
            nodes,
            args,
            count,
            NUM_REFLECTIVE_QUAD_TO_ARGS
        ) { array, start ->
            PathNode.ReflectiveQuadTo(x = array[start], y = array[start + 1])
        }

        RelativeArcToKey -> pathNodesFromArgs(nodes, args, count, NUM_ARC_TO_ARGS) { array, start ->
            PathNode.RelativeArcTo(
                horizontalEllipseRadius = array[start],
                verticalEllipseRadius = array[start + 1],
                theta = array[start + 2],
                isMoreThanHalf = array[start + 3].compareTo(0.0f) != 0,
                isPositiveArc = array[start + 4].compareTo(0.0f) != 0,
                arcStartDx = array[start + 5],
                arcStartDy = array[start + 6]
            )
        }

        ArcToKey -> pathNodesFromArgs(nodes, args, count, NUM_ARC_TO_ARGS) { array, start ->
            PathNode.ArcTo(
                horizontalEllipseRadius = array[start],
                verticalEllipseRadius = array[start + 1],
                theta = array[start + 2],
                isMoreThanHalf = array[start + 3].compareTo(0.0f) != 0,
                isPositiveArc = array[start + 4].compareTo(0.0f) != 0,
                arcStartX = array[start + 5],
                arcStartY = array[start + 6]
            )
        }

        else -> throw IllegalArgumentException("Unknown command for: $this")
    }
}

private inline fun pathNodesFromArgs(
    nodes: MutableList<PathNode>,
    args: FloatArray,
    count: Int,
    numArgs: Int,
    crossinline nodeFor: (subArray: FloatArray, start: Int) -> PathNode
) {
    val end = count - numArgs
    var index = 0
    while (index <= end) {
        val node = nodeFor(args, index)
        nodes.add(when {
            // According to the spec, if a MoveTo is followed by multiple pairs of coordinates,
            // the subsequent pairs are treated as implicit corresponding LineTo commands.
            node is PathNode.MoveTo && index > 0 -> PathNode.LineTo(args[index], args[index + 1])
            node is PathNode.RelativeMoveTo && index > 0 ->
                PathNode.RelativeLineTo(args[index], args[index + 1])
            else -> node
        })
        index += numArgs
    }
}

/**
 * Constants used by [Char.addPathNodes] for creating [PathNode]s from parsed paths.
 */
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