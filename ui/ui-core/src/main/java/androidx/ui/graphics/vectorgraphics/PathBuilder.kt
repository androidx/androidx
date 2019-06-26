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

package androidx.ui.graphics.vectorgraphics

class PathBuilder {

    private val nodes = mutableListOf<PathNode>()

    fun getNodes(): Array<PathNode> = nodes.toTypedArray()

    fun close(): PathBuilder =
        addNode(PathCommand.Close)

    fun moveTo(x: Float, y: Float) =
        addNode(PathCommand.MoveTo, x, y)

    fun moveToRelative(x: Float, y: Float) =
        addNode(PathCommand.RelativeMoveTo, x, y)

    fun lineTo(x: Float, y: Float) =
        addNode(PathCommand.LineTo, x, y)

    fun lineToRelative(x: Float, y: Float) =
        addNode(PathCommand.RelativeLineTo, x, y)

    fun horizontalLineTo(x: Float) =
        addNode(PathCommand.HorizontalLineTo, x)

    fun horizontalLineToRelative(x: Float) =
        addNode(PathCommand.RelativeHorizontalTo, x)

    fun verticalLineTo(y: Float) =
        addNode(PathCommand.VerticalLineTo, y)

    fun verticalLineToRelative(y: Float) =
        addNode(PathCommand.RelativeVerticalTo, y)

    fun curveTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ) = addNode(PathCommand.CurveTo, x1, y1, x2, y2, x3, y3)

    fun curveToRelative(
        dx1: Float,
        dy1: Float,
        dx2: Float,
        dy2: Float,
        dx3: Float,
        dy3: Float
    ) = addNode(PathCommand.RelativeCurveTo, dx1, dy1, dx2, dy2, dx3, dy3)

    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.ReflectiveCurveTo, x1, y1, x2, y2)

    fun reflectiveCurveToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.RelativeReflectiveCurveTo, x1, y1, x2, y2)

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.QuadTo, x1, y1, x2, y2)

    fun quadToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.RelativeQuadTo, x1, y1, x2, y2)

    fun reflectiveQuadTo(x1: Float, y1: Float) =
        addNode(PathCommand.ReflectiveQuadTo, x1, y1)

    fun reflectiveQuadToRelative(x1: Float, y1: Float) =
        addNode(PathCommand.RelativeReflectiveQuadTo, x1, y1)

    fun arcTo(
        horizontalEllipseRadius: Float,
        verticalEllipseRadius: Float,
        theta: Float,
        largeArcFlag: Float,
        sweepFlag: Float,
        x1: Float,
        y1: Float
    ) = addNode(
        PathCommand.ArcTo, horizontalEllipseRadius, verticalEllipseRadius, theta,
                largeArcFlag, sweepFlag, x1, y1)

    fun arcToRelative(
        a: Float,
        b: Float,
        theta: Float,
        largeArcFlag: Float,
        sweepFlag: Float,
        x1: Float,
        y1: Float
    ) = addNode(PathCommand.RelativeArcTo, a, b, theta, largeArcFlag, sweepFlag, x1, y1)

    private fun addNode(cmd: PathCommand, vararg args: Float): PathBuilder {
        nodes.add(PathNode(cmd, args))
        return this
    }
}