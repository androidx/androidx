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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutBoundsHolder
import androidx.compose.ui.layout.layoutBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.sendMouseEvent
import androidx.compose.ui.sendMousePress
import androidx.compose.ui.sendMouseRelease
import androidx.compose.ui.sendMouseWheelEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.roundToDimension
import androidx.compose.ui.window.runApplicationTest
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun swingPanelIsSizedToContentPreferredSize() = runApplicationTest(useDelay = true) {
        val panel = JPanel()

        panel.preferredSize = Dimension(100, 100)

        val layoutBounds = LayoutBoundsHolder()
        lateinit var density: Density
        launchTestApplication {
            Window(onCloseRequest = {}) {
                SwingPanel(
                    modifier = Modifier.layoutBounds(layoutBounds),
                    factory = { panel }
                )
                density = LocalDensity.current
            }
        }

        fun assertPanelSizeIsItsPreferredSize() {
            assertEquals((panel.preferredSize.width * density.density).roundToInt(), layoutBounds.bounds?.width)
            assertEquals((panel.preferredSize.height * density.density).roundToInt(), layoutBounds.bounds?.height)
        }

        awaitIdle()
        assertPanelSizeIsItsPreferredSize()

        // Also check that when the preferred size is updated, the SwingPanel is resized
        panel.preferredSize = Dimension(200, 200)
        panel.invalidate()
        awaitIdle()
        assertPanelSizeIsItsPreferredSize()
    }

    @Test
    fun swingPanelPropagatesMouseWheelEvents() = runApplicationTest {
        val scrollState = ScrollState(0)
        launchTestWindowApplication(
            state = WindowState(size = DpSize(500.dp, 500.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(Modifier.fillMaxWidth().height(2000.dp)) {
                    SwingPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        factory = {
                            JPanel(BorderLayout()).also {
                                it.add(JLabel("Swing content"))
                                it.background = java.awt.Color.RED
                            }
                        },
                    )
                }
            }
        }
        awaitIdle()

        assertEquals(scrollState.value, 0)
        window.sendMouseWheelEvent(x = 200, y = 200, wheelRotation = 50.0)
        awaitIdle()
        assertTrue(scrollState.value > 0, "Compose did not scroll; SwingPanel blocked the event")
    }

    @Test
    fun swingPanelRespondsToDensityChange() = runApplicationTest {
        val swingComponent = object: JComponent() {
            override fun paint(g: Graphics) {
                g.color = java.awt.Color.RED
                g.fillRect(0, 0, width, height)
            }
        }
        val swingElementSize = DpSize(200.dp, 200.dp)

        var densityScale by mutableFloatStateOf(1f)
        launchTestWindowApplication(
            state = WindowState(size = DpSize(500.dp, 500.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(densityScale * density)) {
                    SwingPanel(
                        modifier = Modifier.size(swingElementSize),
                        factory = { swingComponent }
                    )
                }
            }
        }

        awaitIdle()
        assertEquals(
            expected = swingElementSize.roundToDimension(),
            actual = swingComponent.size
        )

        densityScale = 2f
        awaitIdle()
        assertEquals(
            expected = (swingElementSize * densityScale).roundToDimension(),
            actual = swingComponent.size
        )
    }
}