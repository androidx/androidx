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

package androidx.ui.graphics.vector

class PathBuilder {

    private val nodes = mutableListOf<PathNode>()

    fun getNodes(): List<PathNode> = nodes

    fun close(): PathBuilder = addNode(PathNode.Close)

    fun moveTo(x: Float, y: Float) = addNode(PathNode.MoveTo(x, y))

    fun moveToRelative(x: Float, y: Float) = addNode(PathNode.RelativeMoveTo(x, y))

    fun lineTo(x: Float, y: Float) = addNode(PathNode.LineTo(x, y))

    fun lineToRelative(x: Float, y: Float) = addNode(PathNode.RelativeLineTo(x, y))

    fun horizontalLineTo(x: Float) = addNode(PathNode.HorizontalTo(x))

    fun horizontalLineToRelative(x: Float) = addNode(PathNode.RelativeHorizontalTo(x))

    fun verticalLineTo(y: Float) = addNode(PathNode.VerticalTo(y))

    fun verticalLineToRelative(y: Float) = addNode(PathNode.RelativeVerticalTo(y))

    fun curveTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ) = addNode(PathNode.CurveTo(x1, y1, x2, y2, x3, y3))

    fun curveToRelative(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) = addNode(PathNode.RelativeCurveTo(dx1, dy1, dx2, dy2, dx3, dy3))

    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathNode.ReflectiveCurveTo(x1, y1, x2, y2))

    fun reflectiveCurveToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathNode.RelativeReflectiveCurveTo(x1, y1, x2, y2))

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathNode.QuadTo(x1, y1, x2, y2))

    fun quadToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathNode.RelativeQuadTo(x1, y1, x2, y2))

    fun reflectiveQuadTo(x1: Float, y1: Float) =
        addNode(PathNode.ReflectiveQuadTo(x1, y1))

    fun reflectiveQuadToRelative(x1: Float, y1: Float) =
        addNode(PathNode.RelativeReflectiveQuadTo(x1, y1))

    fun arcTo(
        horizontalEllipseRadius: Float,
        verticalEllipseRadius: Float,
        theta: Float,
        isMoreThanHalf: Boolean,
        isPositiveArc: Boolean,
        x1: Float,
        y1: Float
    ) = addNode(
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

    fun arcToRelative(
        a: Float,
        b: Float,
        theta: Float,
        isMoreThanHalf: Boolean,
        isPositiveArc: Boolean,
        x1: Float,
        y1: Float
    ) = addNode(PathNode.RelativeArcTo(a, b, theta, isMoreThanHalf, isPositiveArc, x1, y1))

    private fun addNode(node: PathNode): PathBuilder {
        nodes.add(node)
        return this
    }
}