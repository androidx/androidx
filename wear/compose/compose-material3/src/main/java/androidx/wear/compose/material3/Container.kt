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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastIsFinite
import androidx.compose.ui.util.fastRoundToInt
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.LocalTransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.scrollTransform

/**
 * Applies a container style to the current composable and Material 3 Motion if in the scope of a
 * TransformingLazyColumn
 *
 * This modifier provides a background using the given [Painter] and clips it to the given [Shape].
 * If a border is provided, it will be applied around the shape.
 *
 * For items within a TransformingLazyColumn, it applies a scrolling transformation to the container
 * based on its position within the lazy column. For other composables, it simply applies the
 * background and border.
 *
 * @param painter The painter used to draw the background of the container.
 * @param shape The shape of the container. Defaults to [RectangleShape].
 * @param border The border stroke to apply to the container. If null, no border is drawn.
 * @return A modifier that applies the container style.
 */
@Composable
internal fun Modifier.container(
    painter: Painter,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null
): Modifier {
    val borderModifier = if (border != null) border(border = border, shape = shape) else this
    val itemScope =
        if (LocalReduceMotion.current) {
            null
        } else {
            LocalTransformingLazyColumnItemScope.current
        }
    return itemScope?.let { tlcScope -> scrollTransform(tlcScope, shape, painter, border) }
        ?: borderModifier
            .clip(shape = shape)
            .paintBackground(painter = painter, contentScale = ContentScale.Crop)
}

/**
 * Applies a container style to the current composable and Material 3 Motion if in the scope of a
 * TransformingLazyColumn
 *
 * This modifier provides a background using the given [Color] and clips it to the given [Shape]. If
 * a border is provided, it will be applied around the shape.
 *
 * For items within a TransformingLazyColumn, it applies a scrolling transformation to the container
 * based on its position within the lazy column. For other composables, it simply applies the
 * background and border.
 *
 * @param color The color used to draw the background of the container.
 * @param shape The shape of the container. Defaults to [RectangleShape].
 * @param border The border stroke to apply to the container. If null, no border is drawn.
 * @return A modifier that applies the container style.
 */
@Composable
internal fun Modifier.container(
    color: Color,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null
): Modifier {
    val borderModifier = if (border != null) border(border = border, shape = shape) else this
    val itemScope =
        if (LocalReduceMotion.current) {
            null
        } else {
            LocalTransformingLazyColumnItemScope.current
        }
    return itemScope?.let { tlcScope ->
        scrollTransform(tlcScope, shape, ColorPainter(color), border)
    } ?: borderModifier.clip(shape = shape).background(color = color)
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
                layoutDirection
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
