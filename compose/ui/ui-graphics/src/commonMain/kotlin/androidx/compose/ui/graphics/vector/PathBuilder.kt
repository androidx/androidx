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

class PathBuilder {
    // 88% of Material icons use 32 or fewer path nodes
    private val _nodes = ArrayList<PathNode>(32)

    val nodes: List<PathNode>
        get() = _nodes

    fun close(): PathBuilder {
        _nodes.add(PathNode.Close)
        return this
    }

    fun moveTo(x: Float, y: Float): PathBuilder {
        _nodes.add(PathNode.MoveTo(x, y))
        return this
    }

    fun moveToRelative(dx: Float, dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeMoveTo(dx, dy))
        return this
    }

    fun lineTo(x: Float, y: Float): PathBuilder {
        _nodes.add(PathNode.LineTo(x, y))
        return this
    }

    fun lineToRelative(dx: Float, dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeLineTo(dx, dy))
        return this
    }

    fun horizontalLineTo(x: Float): PathBuilder {
        _nodes.add(PathNode.HorizontalTo(x))
        return this
    }

    fun horizontalLineToRelative(dx: Float): PathBuilder {
        _nodes.add(PathNode.RelativeHorizontalTo(dx))
        return this
    }

    fun verticalLineTo(y: Float): PathBuilder {
        _nodes.add(PathNode.VerticalTo(y))
        return this
    }

    fun verticalLineToRelative(dy: Float): PathBuilder {
        _nodes.add(PathNode.RelativeVerticalTo(dy))
        return this
    }

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

    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float): PathBuilder {
        _nodes.add(PathNode.ReflectiveCurveTo(x1, y1, x2, y2))
        return this
    }

    fun reflectiveCurveToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float): PathBuilder {
        _nodes.add(PathNode.RelativeReflectiveCurveTo(dx1, dy1, dx2, dy2))
        return this
    }

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float): PathBuilder {
        _nodes.add(PathNode.QuadTo(x1, y1, x2, y2))
        return this
    }

    fun quadToRelative(dx1: Float, dy1: Float, dx2: Float, dy2: Float): PathBuilder {
        _nodes.add(PathNode.RelativeQuadTo(dx1, dy1, dx2, dy2))
        return this
    }

    fun reflectiveQuadTo(x1: Float, y1: Float): PathBuilder {
        _nodes.add(PathNode.ReflectiveQuadTo(x1, y1))
        return this
    }

    fun reflectiveQuadToRelative(dx1: Float, dy1: Float): PathBuilder {
        _nodes.add(PathNode.RelativeReflectiveQuadTo(dx1, dy1))
        return this
    }

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
