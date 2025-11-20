/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastRoundToInt

/**
 * Applies a surface style to the current composable and Material 3 Motion if [transformation] is
 * provided.
 *
 * This modifier draws a background using the given [Painter] and clips it to the given [Shape]. If
 * a border is provided, it will be applied around the shape. If [transformation] is provided, it
 * will change the visual presentation of the surface.
 *
 * @param transformation The transformation to be applied to the composable. If null, will draw the
 *   container background using the given [Painter] and clip it to the given [Shape].
 * @param painter The painter used to draw the background of the container.
 * @param shape The shape of the container. Defaults to [RectangleShape].
 * @param border The border stroke to apply to the container. If null, no border is drawn.
 * @return A modifier that applies the transformation.
 */
@Composable
internal fun Modifier.surface(
    transformation: SurfaceTransformation?,
    painter: Painter,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
): Modifier =
    if (transformation != null && transformation != NoOpSurfaceTransformation) {
        val backgroundPainter =
            remember(transformation, painter, shape, border) {
                transformation.createContainerPainter(painter, shape, border)
            }

        // We first apply the container transformation, then the Modifier `surface` is applied to,
        // then the painter and finally the content transformation.
        Modifier.graphicsLayer { with(transformation) { applyContainerTransformation() } }
            .then(this)
            .paintBackground(painter = backgroundPainter)
            .graphicsLayer {
                this.shape = shape
                with(transformation) { applyContentTransformation() }
                clip = true
            }
    } else {
        val borderModifier = if (border != null) border(border = border, shape = shape) else this
        borderModifier
            .clip(shape = shape)
            .paintBackground(painter = painter, contentScale = ContentScale.Crop)
    }

/**
 * Paint the background using [Painter].
 *
 * This modifier simply paints the background, without modifying the size.
 *
 * @param painter [Painter] to be drawn by this [Modifier]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 */
internal fun Modifier.paintBackground(
    painter: Painter,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
) = this then PainterElement(painter = painter, alignment = alignment, contentScale = contentScale)

private data class PainterElement(
    val painter: Painter,
    val contentScale: ContentScale,
    var alignment: Alignment = Alignment.Center,
) : ModifierNodeElement<PainterNode>() {
    override fun create(): PainterNode {
        return PainterNode(painter = painter, alignment = alignment, contentScale = contentScale)
    }

    override fun update(node: PainterNode) {
        node.painter = painter
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "paint"
        properties["painter"] = painter
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
    }
}

private class PainterNode(
    var painter: Painter,
    var alignment: Alignment = Alignment.Center,
    val contentScale: ContentScale,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth =
            if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                intrinsicSize.width
            } else {
                size.width
            }

        val srcHeight =
            if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                intrinsicSize.height
            } else {
                size.height
            }

        val srcSize = Size(srcWidth, srcHeight)
        val scaledSize =
            if (size.width != 0f && size.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, size)
            } else {
                Size.Zero
            }

        val alignedPosition =
            alignment.align(
                IntSize(scaledSize.width.fastRoundToInt(), scaledSize.height.fastRoundToInt()),
                IntSize(size.width.fastRoundToInt(), size.height.fastRoundToInt()),
                layoutDirection,
            )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        translate(dx, dy) { with(painter) { draw(size = scaledSize) } }

        // Maintain the same pattern as Modifier.drawBehind to allow chaining of DrawModifiers
        drawContent()
    }

    private fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.fastIsFinite()

    private fun Size.hasSpecifiedAndFiniteHeight() =
        this != Size.Unspecified && height.fastIsFinite()

    override fun toString(): String =
        "PainterModifier(" +
            "painter=$painter, " +
            "alignment=$alignment, " +
            "contentScale=$contentScale)"
}
