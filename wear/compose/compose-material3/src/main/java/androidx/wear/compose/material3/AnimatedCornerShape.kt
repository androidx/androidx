/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3

import androidx.compose.foundation.shape.AbsoluteCutCornerShape
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.LayoutDirection.Ltr
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

/**
 * An implementation similar to RoundedCornerShape, but based on linear interpolation between a
 * start and stop CornerSize, and an observable progress between 0.0 and 1.0.
 *
 * @param start the corner sizes when progress is 0.0
 * @param stop the corner sizes when progress is 1.0
 * @param progress returns the current progress from start to stop.
 */
@Stable
internal class AnimatedRoundedCornerShape(
    start: RoundedCornerShape,
    stop: RoundedCornerShape,
    progress: () -> Float
) : Shape {
    private val topStart = AnimatedCornerSize(start.topStart, stop.topStart, progress)
    private val topEnd = AnimatedCornerSize(start.topEnd, stop.topEnd, progress)
    private val bottomEnd = AnimatedCornerSize(start.bottomEnd, stop.bottomEnd, progress)
    private val bottomStart = AnimatedCornerSize(start.bottomStart, stop.bottomStart, progress)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline =
        Outline.Rounded(
            RoundRect(
                rect = size.toRect(),
                topLeft =
                    CornerRadius(
                        (if (layoutDirection == Ltr) topStart else topEnd).toPx(size, density)
                    ),
                topRight =
                    CornerRadius(
                        (if (layoutDirection == Ltr) topEnd else topStart).toPx(size, density)
                    ),
                bottomRight =
                    CornerRadius(
                        (if (layoutDirection == Ltr) bottomEnd else bottomStart).toPx(size, density)
                    ),
                bottomLeft =
                    CornerRadius(
                        (if (layoutDirection == Ltr) bottomStart else bottomEnd).toPx(size, density)
                    )
            )
        )
}

@Stable
internal class AnimatedCornerSize(
    val start: CornerSize,
    val stop: CornerSize,
    val progress: () -> Float
) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float =
        lerp(start.toPx(shapeSize, density), stop.toPx(shapeSize, density), progress())
}

@Composable
internal fun rememberAnimatedRoundedCornerShape(
    shape: RoundedCornerShape,
    pressedShape: RoundedCornerShape,
    progress: State<Float>
): Shape {
    return remember(shape, pressedShape, progress) {
        AnimatedRoundedCornerShape(shape, pressedShape) { progress.value }
    }
}

/**
 * An implementation of Shape, animating a Morph based on an observable progress between 0.0 and
 * 1.0.
 */
@Stable
internal class AnimatedMorphShape(
    private val shape: CornerBasedShape,
    private val pressedShape: CornerBasedShape,
    private val progress: () -> Float
) : Shape {

    @Suppress("PrimitiveInCollection") // No way to get underlying Long of Size
    private val morphState = mutableStateMapOf<Size, Morph?>()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val morph =
            morphState.computeIfAbsent(size) {
                val polygon =
                    shape.toRoundedPolygonOrNull(size, density, layoutDirection)
                        ?: return@computeIfAbsent null
                val pressedPolygon =
                    pressedShape.toRoundedPolygonOrNull(size, density, layoutDirection)
                        ?: return@computeIfAbsent null

                Morph(polygon, pressedPolygon)
            }

        if (morph == null) {
            return shape.createOutline(size, layoutDirection, density)
        }

        val path =
            morph.toPath(progress()).asComposePath().apply {
                transform(Matrix().apply { scale(size.width, size.height) })
            }

        return Outline.Generic(path)
    }
}

/**
 * Convert a CornerBasedShape (Compose Shape) to a RoundedPolygon (Graphics Shape), such that it can
 * be used in a Morph between two shapes.
 *
 * Returns null if the CornerBasedShape is not one of RoundedCornerShape, CutCornerShape,
 * AbsoluteRoundedCornerShape, or AbsoluteCutCornerShape.
 *
 * Size and density must be known at this point since Corners may be specified in either percentage
 * or dp, and cannot be correctly scaled as either a RoundedPolygon or a Morph.
 *
 * @param size The size of the final Composable such as a Button.
 * @param density The density of the composition.
 */
internal fun CornerBasedShape.toRoundedPolygonOrNull(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
): RoundedPolygon? {
    return when (this) {
        is RoundedCornerShape -> toRoundedPolygon(size, density, layoutDirection).normalized()
        is CutCornerShape -> toRoundedPolygon(size, density, layoutDirection).normalized()
        is AbsoluteRoundedCornerShape -> toRoundedPolygon(size, density).normalized()
        is AbsoluteCutCornerShape -> toRoundedPolygon(size, density).normalized()
        else -> null
    }
}

private fun RoundedCornerShape.toRoundedPolygon(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
) =
    RoundedPolygon.rectangle(
        size.width,
        size.height,
        perVertexRounding =
            listOf(
                CornerRounding(
                    (if (layoutDirection == Ltr) bottomEnd else bottomStart).toPx(size, density)
                ),
                CornerRounding(
                    (if (layoutDirection == Ltr) bottomStart else bottomEnd).toPx(size, density)
                ),
                CornerRounding(
                    (if (layoutDirection == Ltr) topStart else topEnd).toPx(size, density)
                ),
                CornerRounding(
                    (if (layoutDirection == Ltr) topEnd else topStart).toPx(size, density)
                ),
            )
    )

private fun CutCornerShape.toRoundedPolygon(
    size: Size,
    density: Density,
    layoutDirection: LayoutDirection
): RoundedPolygon {
    val topRightPx = (if (layoutDirection == Ltr) topEnd else topStart).toPx(size, density)
    val bottomRightPx = (if (layoutDirection == Ltr) bottomEnd else bottomStart).toPx(size, density)
    val bottomLeftPx = (if (layoutDirection == Ltr) bottomStart else bottomEnd).toPx(size, density)
    val topLeftPx = (if (layoutDirection == Ltr) topStart else topEnd).toPx(size, density)

    val width = size.width
    val height = size.height

    return RoundedPolygon(
        floatArrayOf(
            width - bottomRightPx,
            height,
            bottomLeftPx,
            height,
            0f,
            height - bottomLeftPx,
            0f,
            topLeftPx,
            topLeftPx,
            0f,
            width - topRightPx,
            0f,
            width,
            topRightPx,
            width,
            height - bottomRightPx,
        ),
    )
}

private fun AbsoluteRoundedCornerShape.toRoundedPolygon(size: Size, density: Density) =
    RoundedPolygon.rectangle(
        size.width,
        size.height,
        perVertexRounding =
            listOf(
                CornerRounding(bottomEnd.toPx(size, density)),
                CornerRounding(bottomStart.toPx(size, density)),
                CornerRounding(topStart.toPx(size, density)),
                CornerRounding(topEnd.toPx(size, density)),
            )
    )

private fun AbsoluteCutCornerShape.toRoundedPolygon(size: Size, density: Density): RoundedPolygon {
    val topEndPx = topEnd.toPx(size, density)
    val bottomEndPx = bottomEnd.toPx(size, density)
    val bottomStartPx = bottomStart.toPx(size, density)
    val topStartPx = topStart.toPx(size, density)

    val width = size.width
    val height = size.height

    return RoundedPolygon(
        floatArrayOf(
            width - bottomEndPx,
            height,
            bottomStartPx,
            height,
            0f,
            height - bottomStartPx,
            0f,
            topStartPx,
            topStartPx,
            0f,
            width - topEndPx,
            0f,
            width,
            topEndPx,
            width,
            height - bottomEndPx,
        ),
    )
}

/**
 * Returns an implementation of Shape, animating a Morph based on an observable progress between 0.0
 * and 1.0.
 *
 * The Morph supports animated between different CornerBasedShapes such as a CutCorner to
 * RoundedCorner. Returns a simple non animated shape if `toRoundedPolygonOrNull` does not support
 * the shape.
 *
 * When animating between two RoundedCornerShape, `rememberAnimatedRoundedCornerShape` should be
 * used.
 *
 * Size and density must be known at this point since Corners may be specified in either percentage
 * or dp, and cannot be correctly scaled as either a RoundedPolygon or a Morph.
 */
@Composable
internal fun rememberAnimatedCornerBasedShape(
    shape: CornerBasedShape,
    pressedShape: CornerBasedShape,
    progress: State<Float>
): Shape {
    return remember(shape, pressedShape, progress) {
        AnimatedMorphShape(shape, pressedShape) { progress.value }
    }
}
