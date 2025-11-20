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

package androidx.compose.material3

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.tokens.DragHandleTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastRoundToInt

/**
 * [Material Design drag
 * handle](https://m3.material.io/foundations/layout/understanding-layout/parts-of-layout#314a4c32-be52-414c-8da7-31f059f1776d)
 *
 * A drag handle is a capsule-like shape that can be used by users to change component size and/or
 * position by dragging. A typical usage of it will be pane expansion - when you split your screen
 * into multiple panes, a drag handle is suggested to be used so users can drag it to change the
 * proportion of how the screen is being split. Note that a vertically oriented drag handle is meant
 * to convey horizontal drag motions.
 *
 * @sample androidx.compose.material3.samples.VerticalDragHandleSample
 * @param modifier the [Modifier] to be applied to this drag handle.
 * @param sizes sizes of this drag handle; see [VerticalDragHandleDefaults.sizes] for the default
 *   values.
 * @param colors colors of this drag handle; see [VerticalDragHandleDefaults.colors] for the default
 *   values.
 * @param shapes shapes of this drag handle; see [VerticalDragHandleDefaults.colors] for the default
 *   values.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this drag handle. You can use this to change the drag handle's
 *   appearance or preview the drag handle in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun VerticalDragHandle(
    modifier: Modifier = Modifier,
    sizes: DragHandleSizes = VerticalDragHandleDefaults.sizes(),
    colors: DragHandleColors = VerticalDragHandleDefaults.colors(),
    shapes: DragHandleShapes = VerticalDragHandleDefaults.shapes(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .hoverable(interactionSource)
                .pressable(interactionSource, { isPressed = true }, { isPressed = false })
                .graphicsLayer {
                    shape =
                        when {
                            isDragged -> shapes.draggedShape
                            isPressed -> shapes.pressedShape
                            else -> shapes.shape
                        }
                    clip = true
                }
                .layout { measurable, _ ->
                    val dragHandleSize =
                        when {
                            isDragged -> sizes.draggedSize
                            isPressed -> sizes.pressedSize
                            else -> sizes.size
                        }.toSize()
                    // set constraints here to be the size needed
                    val placeable =
                        measurable.measure(
                            Constraints.fixed(
                                dragHandleSize.width.fastRoundToInt(),
                                dragHandleSize.height.fastRoundToInt(),
                            )
                        )
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .drawBehind {
                    drawRect(
                        when {
                            isDragged -> colors.draggedColor
                            isPressed -> colors.pressedColor
                            else -> colors.color
                        }
                    )
                }
                .indication(interactionSource, ripple())
    )
}

/**
 * Specifies the colors that will be used in a drag handle in different states.
 *
 * @param color the default color of the drag handle when it's not being pressed.
 * @param pressedColor the color of the drag handle when it's being pressed but not dragged, by
 *   default it will be the same as [draggedColor].
 * @param draggedColor the color of the drag handle when it's being dragged.
 */
@Immutable
class DragHandleColors(val color: Color, val pressedColor: Color, val draggedColor: Color) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleColors) return false
        if (color != other.color) return false
        if (pressedColor != other.pressedColor) return false
        if (draggedColor != other.draggedColor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + pressedColor.hashCode()
        result = 31 * result + draggedColor.hashCode()
        return result
    }
}

/**
 * Specifies the shapes that will be used in a drag handle in different states.
 *
 * @param shape the default shape of the drag handle when it's not being pressed.
 * @param pressedShape the shape of the drag handle when it's being pressed but not dragged, by
 *   default it will be the same as [draggedShape].
 * @param draggedShape the shape of the drag handle when it's being dragged.
 */
@Immutable
class DragHandleShapes(val shape: Shape, val pressedShape: Shape, val draggedShape: Shape) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleShapes) return false
        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false
        if (draggedShape != other.draggedShape) return false
        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + draggedShape.hashCode()
        return result
    }
}

/**
 * Specifies the sizes that will be used in a drag handle in different states.
 *
 * @param size the default size of the drag handle when it's not being pressed.
 * @param pressedSize the size of the drag handle when it's being pressed but not dragged, by
 *   default it will be the same as [draggedSize].
 * @param draggedSize the size of the drag handle when it's being dragged.
 */
@Immutable
class DragHandleSizes(val size: DpSize, val pressedSize: DpSize, val draggedSize: DpSize) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleSizes) return false
        if (size != other.size) return false
        if (pressedSize != other.pressedSize) return false
        if (draggedSize != other.draggedSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = size.hashCode()
        result = 31 * result + pressedSize.hashCode()
        result = 31 * result + draggedSize.hashCode()
        return result
    }
}

/** Contains the baseline values used by a [VerticalDragHandle]. */
object VerticalDragHandleDefaults {
    /**
     * Creates a [DragHandleColors] that represents the default, pressed, and dragged colors used in
     * a [VerticalDragHandle].
     */
    @Composable fun colors(): DragHandleColors = MaterialTheme.colorScheme.colors

    /**
     * Creates a [DragHandleColors] that represents the default, pressed, and dragged colors used in
     * a [VerticalDragHandle].
     *
     * @param color provides a different color to override the default color of the drag handle when
     *   it's not being pressed.
     * @param pressedColor provides a different color to override the color of the drag handle when
     *   it's being pressed but not dragged.
     * @param draggedColor provides a different color to override the color of the drag handle when
     *   it's being dragged.
     */
    @Composable
    fun colors(
        color: Color = Color.Unspecified,
        pressedColor: Color = Color.Unspecified,
        draggedColor: Color = Color.Unspecified,
    ): DragHandleColors =
        with(MaterialTheme.colorScheme.colors) {
            DragHandleColors(
                color.takeOrElse { this.color },
                pressedColor.takeOrElse { this.pressedColor },
                draggedColor.takeOrElse { this.draggedColor },
            )
        }

    /**
     * Creates a [DragHandleShapes] that represents the default, pressed, and dragged shapes used in
     * a [VerticalDragHandle].
     */
    @Composable fun shapes(): DragHandleShapes = MaterialTheme.shapes.shapes

    /**
     * Creates a [DragHandleShapes] that represents the default, pressed, and dragged shapes used in
     * a [VerticalDragHandle].
     *
     * @param shape provides a different shape to override the default shape of the drag handle when
     *   it's not being pressed.
     * @param pressedShape provides a different shape to override the shape of the drag handle when
     *   it's being pressed but not dragged.
     * @param draggedShape provides a different shape to override the shape of the drag handle when
     *   it's being dragged.
     */
    @Composable
    fun shapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        draggedShape: Shape? = null,
    ): DragHandleShapes =
        with(MaterialTheme.shapes.shapes) {
            DragHandleShapes(
                shape ?: this.shape,
                pressedShape ?: this.pressedShape,
                draggedShape ?: this.draggedShape,
            )
        }

    /**
     * Creates a [DragHandleSizes] that represents the default, pressed, and dragged sizes used in a
     * [VerticalDragHandle].
     */
    fun sizes(): DragHandleSizes = sizes

    /**
     * Creates a [DragHandleSizes] that represents the default, pressed, and dragged sizes used in a
     * [VerticalDragHandle].
     *
     * @param size provides a different size to override the default size of the drag handle when
     *   it's not being pressed.
     * @param pressedSize provides a different size to override the size of the drag handle when
     *   it's being pressed but not dragged.
     * @param draggedSize provides a different size to override the size of the drag handle when
     *   it's being dragged.
     */
    fun sizes(
        size: DpSize = DpSize.Unspecified,
        pressedSize: DpSize = DpSize.Unspecified,
        draggedSize: DpSize = DpSize.Unspecified,
    ): DragHandleSizes =
        with(sizes) {
            DragHandleSizes(
                if (size.isSpecified) size else this.size,
                if (pressedSize.isSpecified) pressedSize else this.pressedSize,
                if (draggedSize.isSpecified) draggedSize else this.draggedSize,
            )
        }

    private val ColorScheme.colors: DragHandleColors
        get() {
            return defaultVerticalDragHandleColorsCached
                ?: DragHandleColors(
                        color = fromToken(DragHandleTokens.Color),
                        pressedColor = fromToken(DragHandleTokens.PressedColor),
                        draggedColor = fromToken(DragHandleTokens.DraggedColor),
                    )
                    .also { defaultVerticalDragHandleColorsCached = it }
        }

    private val Shapes.shapes: DragHandleShapes
        get() {
            return defaultVerticalDragHandleShapesCached
                ?: DragHandleShapes(
                        shape = fromToken(DragHandleTokens.Shape),
                        pressedShape = fromToken(DragHandleTokens.PressedShape),
                        draggedShape = fromToken(DragHandleTokens.DraggedShape),
                    )
                    .also { defaultVerticalDragHandleShapesCached = it }
        }

    private val sizes =
        DragHandleSizes(
            size = DpSize(DragHandleTokens.Width, DragHandleTokens.Height),
            pressedSize = DpSize(DragHandleTokens.PressedWidth, DragHandleTokens.PressedHeight),
            draggedSize = DpSize(DragHandleTokens.DraggedWidth, DragHandleTokens.DraggedHeight),
        )
}

private fun Modifier.pressable(
    interactionSource: MutableInteractionSource,
    onPressed: () -> Unit,
    onReleasedOrCancelled: () -> Unit,
): Modifier =
    pointerInput(interactionSource) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            onPressed()
            waitForUpOrCancellation(pass = PointerEventPass.Initial)
            onReleasedOrCancelled()
        }
    }
