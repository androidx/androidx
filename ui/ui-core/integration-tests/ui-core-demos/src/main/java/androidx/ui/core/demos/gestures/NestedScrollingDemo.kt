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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Direction
import androidx.ui.core.DrawModifier
import androidx.ui.core.ContentDrawScope
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.longPressGestureFilter
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.core.gesture.dragGestureFilter
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.drawBorder
import androidx.ui.graphics.Color
import androidx.ui.graphics.drawscope.clipRect
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxWidth
import androidx.ui.layout.preferredHeight
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import kotlin.math.roundToInt

/**
 * Demonstration for how multiple DragGestureDetectors interact.
 */
@Composable
fun NestedScrollingDemo() {
    Column {
        Text("Demonstrates nested scrolling.")
        Text("There are 3 fake vertical scrollers inside another vertical scroller.  Try " +
                "scrolling with 1 or many fingers.")
        Draggable {
            RepeatingList(repetitions = 3) {
                Box(Modifier.preferredHeight(398.dp), padding = 72.dp) {
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
}

/**
 * A very simple ScrollView like implementation that allows for vertical scrolling.
 */
@Composable
private fun Draggable(children: @Composable () -> Unit) {
    val offset = state { 0f }
    val maxOffset = state { 0f }

    val dragObserver = object : DragObserver {
        override fun onDrag(dragDistance: PxPosition): PxPosition {
            val resultingOffset = offset.value + dragDistance.y
            val dyToConsume =
                if (resultingOffset > 0f) {
                    0f - offset.value
                } else if (resultingOffset < maxOffset.value) {
                    maxOffset.value - offset.value
                } else {
                    dragDistance.y
                }
            offset.value = offset.value + dyToConsume
            return PxPosition(0f, dyToConsume)
        }
    }

    val canDrag = { direction: Direction ->
        when (direction) {
            Direction.UP -> true
            Direction.DOWN -> true
            else -> false
        }
    }

    Layout(
        children = children,
        modifier = Modifier.dragGestureFilter(dragObserver, canDrag) + ClipModifier,
        measureBlock = { measurables, constraints, _ ->
            val placeable =
                measurables.first()
                    .measure(constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity))

            maxOffset.value = (constraints.maxHeight.value - placeable.height.value).toFloat()

            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(0.ipx, offset.value.roundToInt().ipx)
            }
        })
}

val ClipModifier = object : DrawModifier {
    override fun ContentDrawScope.draw() {
        clipRect {
            this@draw.drawContent()
        }
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

    val onTap: (PxPosition) -> Unit = {
        color.value = color.value.next()
    }

    val onDoubleTap: (PxPosition) -> Unit = {
        color.value = color.value.prev().prev()
    }

    val onLongPress = { _: PxPosition ->
        color.value = defaultColor
        showPressed.value = false
    }

    val gestureDetectors =
        Modifier
            .pressIndicatorGestureFilter(onPress, onRelease, onRelease)
            .tapGestureFilter(onTap)
            .doubleTapGestureFilter(onDoubleTap)
            .longPressGestureFilter(onLongPress)

    val layout = Modifier.fillMaxWidth().preferredHeight(height)

    val pressOverlay =
        if (showPressed.value) Modifier.drawBackground(pressedColor) else Modifier
    Box(gestureDetectors.plus(layout).drawBackground(color.value).plus(pressOverlay))
}

/**
 * A simple composable that repeats its children as a vertical list of divided items [repetitions]
 * times.
 */
@Composable
private fun RepeatingList(repetitions: Int, row: @Composable () -> Unit) {
    Column(Modifier.drawBorder(border = Border(2.dp, BorderColor))) {
        for (i in 1..repetitions) {
            row()
            if (i != repetitions) {
                Box(
                    Modifier.fillMaxWidth().preferredHeight(1.dp),
                    backgroundColor = Color(0f, 0f, 0f, .12f)
                )
            }
        }
    }
}