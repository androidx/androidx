/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.material.demos

import android.graphics.SweepGradient
import androidx.animation.FloatPropKey
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.WithConstraints
import androidx.ui.core.drawOpacity
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.foundation.currentTextStyle
import androidx.ui.foundation.shape.GenericShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.geometry.Offset
import androidx.ui.geometry.RRect
import androidx.ui.geometry.Radius
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Shader
import androidx.ui.graphics.SolidColor
import androidx.ui.graphics.isSet
import androidx.ui.graphics.toArgb
import androidx.ui.graphics.toPixelMap
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.Spacer
import androidx.ui.layout.Stack
import androidx.ui.layout.aspectRatio
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.offset
import androidx.ui.layout.padding
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.material.Surface
import androidx.ui.material.TopAppBar
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import java.util.Locale

/**
 * Demo that shows picking a color from a color wheel, which then dynamically updates
 * the color of a [TopAppBar]. This pattern could also be used to update the value of a
 * ColorPalette, updating the overall theme for an application.
 */
@Composable
fun ColorPickerDemo() {
    var primary by state { Color(0xFF6200EE) }
    Surface(color = Color(0xFF121212)) {
        Column {
            TopAppBar(title = { Text("Color Picker") }, backgroundColor = primary)
            ColorPicker(onColorChange = { primary = it })
        }
    }
}

@Composable
private fun ColorPicker(onColorChange: (Color) -> Unit) {
    WithConstraints(
        Modifier.padding(50.dp)
            .fillMaxSize()
            .aspectRatio(1f)
    ) {
        val diameter = constraints.maxWidth.value
        var position by state { PxPosition.Origin }
        val colorWheel = remember(diameter) { ColorWheel(diameter) }

        var isDragging by state { false }
        val inputModifier = SimplePointerInput(
            position = position,
            onPositionChange = { newPosition ->
                // Work out if the new position is inside the circle we are drawing, and has a
                // valid color associated to it. If not, keep the current position
                val newColor = colorWheel.colorForPosition(newPosition)
                if (newColor.isSet) {
                    position = newPosition
                    onColorChange(newColor)
                }
            },
            onDragStateChange = { isDragging = it }
        )

        Stack(Modifier.fillMaxSize()) {
            Image(modifier = inputModifier, asset = colorWheel.image)
            val color = colorWheel.colorForPosition(position)
            if (color.isSet) {
                Magnifier(visible = isDragging, position = position, color = color)
            }
        }
    }
}

// TODO: b/152046065 dragging has the wrong semantics here, and it's possible to continue dragging
// outside the bounds of the layout. Use a higher level, simple input wrapper when it's available
// to just get the current position of the pointer, without needing to care about drag behavior /
// relative positions.
/**
 * [dragGestureFilter] that only cares about raw positions, where [position] is the position of
 * the current / last input event, [onPositionChange] is called with the new position when the
 * pointer moves, and [onDragStateChange] is called when dragging starts / stops.
 */
@Composable
private fun SimplePointerInput(
    position: PxPosition,
    onPositionChange: (PxPosition) -> Unit,
    onDragStateChange: (Boolean) -> Unit
): Modifier {
    val observer = object : DragObserver {
        override fun onStart(downPosition: PxPosition) {
            onDragStateChange(true)
            onPositionChange(downPosition)
        }

        override fun onDrag(dragDistance: PxPosition): PxPosition {
            onPositionChange(position + dragDistance)
            return dragDistance
        }

        override fun onCancel() {
            onDragStateChange(false)
        }

        override fun onStop(velocity: PxPosition) {
            onDragStateChange(false)
        }
    }

    return Modifier.dragGestureFilter(observer, startDragImmediately = true)
}

/**
 * Magnifier displayed on top of [position] with the currently selected [color].
 */
@Composable
private fun Magnifier(visible: Boolean, position: PxPosition, color: Color) {
    val offset = with(DensityAmbient.current) {
        Modifier.offset(
            position.x.toDp() - MagnifierWidth / 2,
            // Align with the center of the selection circle
            position.y.toDp() - (MagnifierHeight - (SelectionCircleDiameter / 2))
        )
    }
    MagnifierTransition(
        visible,
        MagnifierWidth,
        SelectionCircleDiameter
    ) { labelWidth: Dp, selectionDiameter: Dp,
        opacity: Float ->
        Column(
            offset.preferredSize(width = MagnifierWidth, height = MagnifierHeight)
                .drawOpacity(opacity)
        ) {
            Box(Modifier.fillMaxWidth(), gravity = ContentGravity.Center) {
                MagnifierLabel(Modifier.preferredSize(labelWidth, MagnifierLabelHeight), color)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.fillMaxWidth().preferredHeight(SelectionCircleDiameter),
                gravity = ContentGravity.Center
            ) {
                MagnifierSelectionCircle(Modifier.preferredSize(selectionDiameter), color)
            }
        }
    }
}

private val MagnifierWidth = 110.dp
private val MagnifierHeight = 100.dp
private val MagnifierLabelHeight = 50.dp
private val SelectionCircleDiameter = 30.dp

/**
 * [Transition] that animates between [visible] states of the magnifier by animating the width of
 * the label, diameter of the selection circle, and opacity of the overall magnifier
 */
@Composable
private fun MagnifierTransition(
    visible: Boolean,
    maxWidth: Dp,
    maxDiameter: Dp,
    children: @Composable (labelWidth: Dp, selectionDiameter: Dp, opacity: Float) -> Unit
) {
    val transitionDefinition = remember {
        transitionDefinition {
            state(false) {
                this[LabelWidthPropKey] = 0.dp
                this[MagnifierDiameterPropKey] = 0.dp
                this[OpacityPropKey] = 0f
            }
            state(true) {
                this[LabelWidthPropKey] = maxWidth
                this[MagnifierDiameterPropKey] = maxDiameter
                this[OpacityPropKey] = 1f
            }
            transition(false to true) {
                LabelWidthPropKey using tween {}
                MagnifierDiameterPropKey using tween {}
                OpacityPropKey using tween {}
            }
            transition(true to false) {
                LabelWidthPropKey using tween {}
                MagnifierDiameterPropKey using tween {}
                OpacityPropKey using tween {
                    delay = 100
                    duration = 200
                }
            }
        }
    }
    Transition(transitionDefinition, visible) { state ->
        children(state[LabelWidthPropKey], state[MagnifierDiameterPropKey], state[OpacityPropKey])
    }
}

private val LabelWidthPropKey = DpPropKey()
private val MagnifierDiameterPropKey = DpPropKey()
private val OpacityPropKey = FloatPropKey()

/**
 * Label representing the currently selected [color], with [Text] representing the hex code and a
 * square at the start showing the [color].
 */
@Composable
private fun MagnifierLabel(modifier: Modifier, color: Color) {
    Surface(shape = MagnifierPopupShape, elevation = 4.dp) {
        Row(modifier) {
            Box(Modifier.weight(0.25f).fillMaxHeight(), backgroundColor = color)
            // Add `#` and drop alpha characters
            val text = "#" + Integer.toHexString(color.toArgb()).toUpperCase(Locale.ROOT).drop(2)
            val textStyle = currentTextStyle().copy(textAlign = TextAlign.Center)
            Text(
                text = text,
                modifier = Modifier.weight(0.75f).padding(top = 10.dp, bottom = 20.dp),
                style = textStyle,
                maxLines = 1
            )
        }
    }
}

/**
 * Selection circle drawn over the currently selected pixel of the color wheel.
 */
@Composable
private fun MagnifierSelectionCircle(modifier: Modifier, color: Color) {
    Surface(
        modifier,
        shape = CircleShape,
        elevation = 4.dp,
        color = color,
        border = Border(2.dp, SolidColor(Color.Black.copy(alpha = 0.75f))),
        content = emptyContent()
    )
}

/**
 * A [GenericShape] that draws a box with a triangle at the bottom center to indicate a popup.
 */
private val MagnifierPopupShape = GenericShape { size ->
    val width = size.width
    val height = size.height

    val arrowY = height * 0.8f
    val arrowXOffset = width * 0.4f

    addRRect(RRect(0f, 0f, width, arrowY, radius = Radius(20f, 20f)))

    moveTo(arrowXOffset, arrowY)
    lineTo(width / 2f, height)
    lineTo(width - arrowXOffset, arrowY)
    close()
}

/**
 * A color wheel with an [ImageAsset] that draws a circular color wheel of the specified diameter.
 */
private class ColorWheel(diameter: Int) {
    private val radius = diameter / 2f

    // TODO: b/152063545 - replace with Compose SweepGradient when it is available
    private val sweepShader = SweepGradient(
        radius,
        radius,
        intArrayOf(
            android.graphics.Color.RED,
            android.graphics.Color.MAGENTA,
            android.graphics.Color.BLUE,
            android.graphics.Color.CYAN,
            android.graphics.Color.GREEN,
            android.graphics.Color.YELLOW,
            android.graphics.Color.RED
        ),
        null
    )

    val image = ImageAsset(diameter, diameter).also { asset ->
        val canvas = Canvas(asset)
        val center = Offset(radius, radius)
        val paint = Paint().apply { shader = Shader(sweepShader) }
        canvas.drawCircle(center, radius, paint)
    }
}

/**
 * @return the matching color for [position] inside [ColorWheel], or `null` if there is no color
 * or the color is partially transparent.
 */
private fun ColorWheel.colorForPosition(position: PxPosition): Color {
    val x = position.x.toInt().coerceAtLeast(0)
    val y = position.y.toInt().coerceAtLeast(0)
    with(image.toPixelMap()) {
        if (x >= width || y >= height) return Color.Unset
        return this[x, y].takeIf { it.alpha == 1f } ?: Color.Unset
    }
}
