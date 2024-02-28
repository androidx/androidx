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

package androidx.compose.ui.awt

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.runApplicationTest
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SwingPanelTest {
    /**
     * Test the positioning of a [SwingPanel] with offset.
     * See https://github.com/JetBrains/compose-multiplatform/issues/4005
     */
    @Test
    fun swingPanelWithOffset() = runApplicationTest {
        val panel = JPanel()
        launchTestApplication {
            Window(onCloseRequest = {}) {
                SwingPanel(
                    modifier = Modifier.size(100.dp).offset(50.dp, 50.dp),
                    factory = { panel }
                )
            }
        }
        awaitIdle()

        val locationInRootPane =
            SwingUtilities.convertPoint(panel, Point(0, 0), SwingUtilities.getRootPane(panel))
        assertEquals(expected = Point(50, 50), locationInRootPane)
    }

    @Test
    fun swingPanelMouseInput() = runApplicationTest {
        val events = mutableListOf<MouseEvent>()
        val listener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) { events.add(e) }
            override fun mouseReleased(e: MouseEvent) { events.add(e) }
            override fun mouseWheelMoved(e: MouseWheelEvent) { events.add(e) }
            override fun mouseMoved(e: MouseEvent) { events.add(e) }
        }
        val panel = JPanel().also {
            it.addMouseListener(listener)
            it.addMouseMotionListener(listener)
            it.addMouseWheelListener(listener)
        }

        val window = ComposeWindow()
        try {
            window.size = Dimension(300, 400)
            window.setContent {
                SwingPanel(
                    modifier = Modifier.size(100.dp).offset(50.dp, 50.dp),
                    factory = { panel }
                )
            }
            window.isVisible = true
            awaitIdle()


            window.sendMouseEvent(MouseEvent.MOUSE_MOVED, 100, 100)
            awaitIdle()
            window.sendMouseWheelEvent(100, 100, wheelRotation = 10.2)
            awaitIdle()
            window.sendMousePress(MouseEvent.BUTTON1, 100, 100)
            awaitIdle()
            window.sendMouseRelease(MouseEvent.BUTTON1, 100, 100)
            awaitIdle()

            assertEquals(4, events.size)

            assertEquals(MouseEvent.MOUSE_MOVED, events[0].id)
            assertEquals(panel, events[0].component)
            assertEquals(50, events[0].x)
            assertEquals(50, events[0].y)

            assertEquals(MouseEvent.MOUSE_WHEEL, events[1].id)
            assertEquals(panel, events[1].component)
            assertIs<MouseWheelEvent>(events[1])
            assertEquals(10, (events[1] as MouseWheelEvent).wheelRotation)
            assertEquals(10.2, (events[1] as MouseWheelEvent).preciseWheelRotation)

            assertEquals(MouseEvent.MOUSE_PRESSED, events[2].id)
            assertEquals(panel, events[2].component)
            assertEquals(MouseEvent.BUTTON1, events[2].button)
            assertEquals(50, events[2].x)
            assertEquals(50, events[2].y)

            assertEquals(MouseEvent.MOUSE_RELEASED, events[3].id)
            assertEquals(panel, events[3].component)
            assertEquals(MouseEvent.BUTTON1, events[3].button)
            assertEquals(50, events[3].x)
            assertEquals(50, events[3].y)
        } finally {
            window.dispose()
        }
    }
}