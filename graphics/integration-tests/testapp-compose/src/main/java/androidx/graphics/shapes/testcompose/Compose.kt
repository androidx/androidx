/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.shapes.testcompose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.TransformResult
import kotlin.math.max
import kotlin.math.min

/**
 * Utility functions providing more idiomatic ways of transforming RoundedPolygons and transforming
 * shapes into a compose Path, for drawing them.
 *
 * This should in the future move into the compose library, maybe with additional API that makes it
 * easier to create, draw, and animate from Compose apps.
 *
 * This code is just here for now prior to integration into compose
 */

/** Scales a shape (given as a List), creating a new List. */
fun List<Cubic>.scaled(scale: Float) = map {
    it.transformed { x, y -> TransformResult(x * scale, y * scale) }
}

/**
 * Gets a [Path] representation for a [RoundedPolygon] shape, which can be used to draw the polygon.
 *
 * @param path an optional [Path] object which, if supplied, will avoid the function having to
 *   create a new [Path] object
 */
@JvmOverloads
fun RoundedPolygon.toPath(path: Path = Path()): Path {
    pathFromCubics(path, cubics)
    return path
}

/**
 * Gets a [Path] representation for a [Morph] shape. This [Path] can be used to draw the morph.
 *
 * @param progress a value from 0 to 1 that determines the morph's current shape, between the start
 *   and end shapes provided at construction time. A value of 0 results in the start shape, a value
 *   of 1 results in the end shape, and any value in between results in a shape which is a linear
 *   interpolation between those two shapes. The range is generally [0..1] and values outside could
 *   result in undefined shapes, but values close to (but outside) the range can be used to get an
 *   exaggerated effect (e.g., for a bounce or overshoot animation).
 * @param path an optional [Path] object which, if supplied, will avoid the function having to
 *   create a new [Path] object
 */
fun Morph.toPath(progress: Float, path: Path = Path()): Path {
    pathFromCubics(path, asCubics(progress))
    return path
}

/**
 * Returns the geometry of the given [cubics] in the given [path] object. This is used internally by
 * the toPath functions, but we could consider exposing it as public API in case anyone was dealing
 * directly with the cubics we create for our shapes.
 */
private fun pathFromCubics(path: Path, cubics: List<Cubic>) {
    var first = true
    path.rewind()
    for (i in 0 until cubics.size) {
        val cubic = cubics[i]
        if (first) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            first = false
        }
        path.cubicTo(
            cubic.control0X,
            cubic.control0Y,
            cubic.control1X,
            cubic.control1Y,
            cubic.anchor1X,
            cubic.anchor1Y
        )
    }
    path.close()
}

/** Transforms a [RoundedPolygon] with the given [Matrix] */
fun RoundedPolygon.transformed(matrix: Matrix): RoundedPolygon = transformed { x, y ->
    val transformedPoint = matrix.map(Offset(x, y))
    TransformResult(transformedPoint.x, transformedPoint.y)
}

/** Calculates and returns the bounds of this [RoundedPolygon] as a [Rect] */
fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

/** Calculates and returns the bounds of this [Morph] as a [Rect] */
fun Morph.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

/**
 * This class can be used to create a [Shape] object from a [RoundedPolygon]
 *
 * @param polygon The [RoundedPolygon] to be used for this [Shape]
 * @param matrix An optional transformation matrix. If none is supplied, or null is passed as the
 *   value, a transformation matrix will be calculated internally, based on the bounds of [polygon].
 *   The result will be that [polygon] will be scaled and translated to fit within the size of the
 *   [Shape].
 */
class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private var matrix: Matrix = Matrix()
) : Shape {
    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        polygon.toPath(path)
        fitToViewport(path, polygon.getBounds(), size, matrix)
        return Outline.Generic(path)
    }
}

/**
 * This class can be used to create a [Shape] object from a [RoundedPolygon]
 *
 * @param morph The [Morph] to be used for this [Shape]
 * @param progress a value from 0 to 1 that determines the morph's current shape, between the start
 *   and end shapes provided at construction time. A value of 0 results in the start shape, a value
 *   of 1 results in the end shape, and any value in between results in a shape which is a linear
 *   interpolation between those two shapes. The range is generally [0..1] and values outside could
 *   result in undefined shapes, but values close to (but outside) the range can be used to get an
 *   exaggerated effect (e.g., for a bounce or overshoot animation).
 * @param matrix An optional transformation matrix. If none is supplied, or null is passed as the
 *   value, a transformation matrix will be calculated internally, based on the bounds of [morph].
 *   The result will be that [morph] will be scaled and translated to fit within the size of the
 *   [Shape].
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private var matrix: Matrix = Matrix()
) : Shape {
    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        morph.toPath(progress, path)
        fitToViewport(path, morph.getBounds(), size, matrix)
        return Outline.Generic(path)
    }
}

/**
 * Scales and translates the given [path] to fit within the given [viewport], using the max
 * dimension of [bounds] and min dimension of [viewport] to ensure that the path fits completely
 * within the viewport.
 *
 * @param path the path to be transformed
 * @param bounds the bounds of the shape represented by [path]
 * @param viewport the area within which [path] will be transformed to fit
 * @param matrix optional [Matrix] item which can be supplied to avoid creating a new matrix every
 *   time the function is called.
 */
fun fitToViewport(path: Path, bounds: Rect, viewport: Size, matrix: Matrix = Matrix()) {
    matrix.reset()
    val maxDimension = max(bounds.width, bounds.height)
    if (maxDimension > 0f) {
        val viewportMin = min(viewport.width, viewport.height)
        val scaleFactor = viewportMin / maxDimension
        val pathCenterX = bounds.left + bounds.width / 2
        val pathCenterY = bounds.top + bounds.height / 2
        matrix.translate(viewportMin / 2, viewportMin / 2)
        matrix.scale(scaleFactor, scaleFactor)
        matrix.translate(-pathCenterX, -pathCenterY)
        path.transform(matrix)
    }
}

fun radialToCartesian(radius: Float, angleRadians: Float, center: Offset = Offset.Zero) =
    directionVector(angleRadians) * radius + center
