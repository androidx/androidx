/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.window

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.WindowScope
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

/**
 * WindowDraggableArea is a component that allows you to drag the window using the mouse.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param content The content lambda.
 */
@Composable
fun WindowScope.WindowDraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val handler = remember { DragHandler(window) }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown()
                handler.onDragStarted()
            }
        }
    ) {
        content()
    }
}

/**
 * Converts AWT [Point] to compose [IntOffset]
 */
private fun Point.toComposeOffset() = IntOffset(x = x, y = y)

/**
 * Returns the position of the mouse pointer, in screen coordinates.
 */
private fun currentPointerLocation(): IntOffset? {
    return MouseInfo.getPointerInfo()?.location?.toComposeOffset()
}

private class DragHandler(
    private val window: Window
) {
    private var windowLocationAtDragStart: IntOffset? = null
    private var dragStartPoint: IntOffset? = null

    private val dragListener = object : MouseMotionAdapter() {
        override fun mouseDragged(event: MouseEvent) = onDrag()
    }
    private val removeListener = object : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) {
            window.removeMouseMotionListener(dragListener)
            window.removeMouseListener(this)
        }
    }

    fun onDragStarted() {
        dragStartPoint = currentPointerLocation() ?: return
        windowLocationAtDragStart = window.location.toComposeOffset()

        window.addMouseListener(removeListener)
        window.addMouseMotionListener(dragListener)
    }

    private fun onDrag() {
        val windowLocationAtDragStart = this.windowLocationAtDragStart ?: return
        val dragStartPoint = this.dragStartPoint ?: return
        val point = currentPointerLocation() ?: return
        val newLocation = windowLocationAtDragStart + (point - dragStartPoint)
        window.setLocation(newLocation.x, newLocation.y)
    }


}
