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

package androidx.ui.framework.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Direction
import androidx.ui.core.DrawModifier
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.DoubleTapGestureDetector
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.LongPressGestureDetector
import androidx.ui.core.gesture.PressIndicatorGestureDetector
import androidx.ui.core.gesture.PressReleasedGestureDetector
import androidx.ui.core.gesture.TouchSlopDragGestureDetector
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.DrawBackground
import androidx.ui.foundation.DrawBorder
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutWidth
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.toRect

/**
 * Demonstration for how multiple DragGestureDetectors interact.
 */
@Composable
fun NestedScrollingDemo() {
    // Outer composable that scrollsAll mea
    Draggable {
        RepeatingList(repetitions = 3) {
            Box(LayoutHeight(398.dp), padding = 72.dp) {
                // Inner composable that scrolls
                Draggable {
                    RepeatingList(repetitions = 5) {
                        // Composable that indicates it is being pressed
                        Pressable(
                            height = 72.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * A very simple ScrollView like implementation that allows for vertical scrolling.
 */
@Composable
private fun Draggable(children: @Composable() () -> Unit) {
    val offset = state { 0.px }
    val maxOffset = state { 0.px }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            val resultingOffset = offset.value + dragDistance.y
            val dyToConsume =
                if (resultingOffset > 0.px) {
                    0.px - offset.value
                } else if (resultingOffset < maxOffset.value) {
                    maxOffset.value - offset.value
                } else {
                    dragDistance.y
                }
            offset.value = offset.value + dyToConsume
            return PxPosition(0.px, dyToConsume)
        }
    }

    val canDrag = { direction: Direction ->
        when (direction) {
            Direction.UP -> true
            Direction.DOWN -> true
            else -> false
        }
    }

    TouchSlopDragGestureDetector(dragObserver, canDrag) {
        Layout(
            children = children,
            modifier = ClipModifier,
            measureBlock = { measurables, constraints, _ ->
                val placeable =
                    measurables.first()
                        .measure(constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity))

                maxOffset.value = constraints.maxHeight.value.px - placeable.height

                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.place(0.ipx, offset.value.round())
                }
            })
    }
}

val ClipModifier = object : DrawModifier {
    override fun draw(density: Density, drawContent: () -> Unit, canvas: Canvas, size: PxSize) {
        canvas.save()
        canvas.clipRect(size.toRect())
        drawContent()
        canvas.restore()
    }
}

/**
 * A very simple Button like implementation that visually indicates when it is being pressed.
 */
@Composable
private fun Pressable(
    height: Dp
) {

    val pressedColor = PressedColor
    val defaultColor = DefaultBackgroundColor

    val color = state { defaultColor }
    val showPressed = state { false }

    val onPress: (PxPosition) -> Unit = {
        showPressed.value = true
    }

    val onRelease = {
        showPressed.value = false
    }

    val onTap = {
        color.value = color.value.next()
    }

    val onDoubleTap: (PxPosition) -> Unit = {
        color.value = color.value.prev().prev()
    }

    val onLongPress = { _: PxPosition ->
        color.value = defaultColor
        showPressed.value = false
    }

    PressIndicatorGestureDetector(onPress, onRelease, onRelease) {
        PressReleasedGestureDetector(onTap, false) {
            DoubleTapGestureDetector(onDoubleTap) {
                LongPressGestureDetector(onLongPress) {
                    val pressOverlay =
                        if (showPressed.value) DrawBackground(pressedColor) else Modifier.None
                    Box(
                        LayoutWidth.Fill + LayoutHeight(height) +
                                DrawBackground(color = color.value) + pressOverlay
                    )
                }
            }
        }
    }
}

/**
 * A simple composable that repeats its children as a vertical list of divided items [repetitions]
 * times.
 */
@Composable
private fun RepeatingList(repetitions: Int, row: @Composable() () -> Unit) {
    Column(DrawBorder(border = Border(2.dp, BorderColor))) {
        for (i in 1..repetitions) {
            row()
            if (i != repetitions) {
                Box(
                    LayoutWidth.Fill + LayoutHeight(1.dp),
                    backgroundColor = Color(0f, 0f, 0f, .12f)
                )
            }
        }
    }
}