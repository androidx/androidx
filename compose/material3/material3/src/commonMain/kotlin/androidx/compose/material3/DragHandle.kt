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

import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DragHandleDefaults.dragHandleColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt

@Composable
internal fun VerticalDragHandle(
    modifier: Modifier = Modifier,
    sizes: DragHandleSizes = DragHandleDefaults.DefaultDragHandleSizes,
    colors: DragHandleColors = MaterialTheme.colorScheme.dragHandleColors(),
    shapes: DragHandleShapes = DragHandleDefaults.DefaultDragHandleShapes,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isDragged by interactionSource.collectIsDraggedAsState()
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .hoverable(interactionSource)
                .pressable(interactionSource) { _ ->
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
                .graphicsLayer {
                    shape = if (isDragged || isPressed) shapes.pressedShape else shapes.defaultShape
                    clip = true
                }
                .layout { measurable, _ ->
                    val dragHandleSize =
                        if (isDragged || isPressed) {
                                sizes.pressedSize
                            } else {
                                sizes.defaultSize
                            }
                            .toSize()
                    // set constraints here to be the size needed
                    val placeable =
                        measurable.measure(
                            Constraints.fixed(
                                dragHandleSize.width.fastRoundToInt(),
                                dragHandleSize.height.fastRoundToInt()
                            )
                        )
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }
                .drawBehind {
                    drawRect(
                        if (isDragged || isPressed) colors.pressedColor else colors.defaultColor
                    )
                }
                .indication(interactionSource, ripple())
    )
}

@Immutable
internal class DragHandleColors(val defaultColor: Color, val pressedColor: Color) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleColors) return false
        if (defaultColor != other.defaultColor) return false
        if (pressedColor != other.pressedColor) return false
        return true
    }

    override fun hashCode(): Int {
        var result = defaultColor.hashCode()
        result = 31 * result + pressedColor.hashCode()
        return result
    }
}

@Immutable
internal class DragHandleShapes(val defaultShape: Shape, val pressedShape: Shape) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleShapes) return false
        if (defaultShape != other.defaultShape) return false
        if (pressedShape != other.pressedShape) return false
        return true
    }

    override fun hashCode(): Int {
        var result = defaultShape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        return result
    }
}

@Immutable
internal class DragHandleSizes(val defaultSize: DpSize, val pressedSize: DpSize) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DragHandleSizes) return false
        if (defaultSize != other.defaultSize) return false
        if (pressedSize != other.pressedSize) return false
        return true
    }

    override fun hashCode(): Int {
        var result = defaultSize.hashCode()
        result = 31 * result + pressedSize.hashCode()
        return result
    }
}

// TODO(b/343194663): Introduce tokens and theming
internal object DragHandleDefaults {
    @Composable
    fun dragHandleColors(
        defaultColor: Color = Color.Unspecified,
        pressedColor: Color = Color.Unspecified
    ): DragHandleColors = MaterialTheme.colorScheme.dragHandleColors(defaultColor, pressedColor)

    fun dragHandleShapes(
        defaultShape: Shape = CircleShape,
        pressedShape: Shape = RoundedCornerShape(12.dp)
    ): DragHandleShapes = DragHandleShapes(defaultShape, pressedShape)

    fun dragHandleSizes(
        defaultSize: DpSize = DpSize(4.dp, 48.dp),
        pressedSize: DpSize = DpSize(12.dp, 52.dp)
    ): DragHandleSizes = DragHandleSizes(defaultSize, pressedSize)

    internal fun ColorScheme.dragHandleColors(
        defaultColor: Color = Color.Unspecified,
        pressedColor: Color = Color.Unspecified
    ): DragHandleColors =
        DragHandleColors(
            if (defaultColor.isSpecified) defaultColor else outline,
            if (pressedColor.isSpecified) pressedColor else onSurface
        )

    internal val DefaultDragHandleShapes = dragHandleShapes()

    internal val DefaultDragHandleSizes = dragHandleSizes()
}

private fun Modifier.pressable(
    interactionSource: MutableInteractionSource,
    onPress: suspend PressGestureScope.(Offset) -> Unit,
): Modifier = pointerInput(interactionSource) { detectTapGestures(onPress = onPress) }
