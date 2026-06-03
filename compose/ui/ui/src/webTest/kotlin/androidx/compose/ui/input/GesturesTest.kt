/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.w3c.dom.pointerevents.PointerEvent as WebPointerEvent
import org.w3c.dom.pointerevents.PointerEventInit

class GesturesTest : OnCanvasTests {

    @Test
    fun pan() = runTest {
        var currentDensity = Density(1f)

        val pans = mutableListOf<Offset>()

        createComposeWindow {
            currentDensity = LocalDensity.current
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { _, pan, _, _ -> pans.add(pan) }
            })
        }

        dispatchEvents(
            WebPointerEvent("pointerdown", touch(0, 0, 0)),
            // first move to exceed the touch slop
            WebPointerEvent("pointermove", touch(0, 10, 10)),
            WebPointerEvent("pointermove", touch(0, 10, 20)),
            WebPointerEvent("pointermove", touch(0, 20, 20))
        )

        val actualPan = 10f * currentDensity.density
        assertEquals(2, pans.size)
        assertEquals(Offset(0f, actualPan), pans[0])
        assertEquals(Offset(actualPan, 0f), pans[1])
    }

    @Test
    fun zoomGestureTest() = runTest {
        val zooms = mutableListOf<Float>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            zooms.add(zoom)
                        }
                    }
            )
        }

        dispatchEvents(
            // Simulate two touch points starting fairly close together
            WebPointerEvent("pointerdown", touch(0, 50, 50)),
            WebPointerEvent("pointerdown", touch(1, 60, 50)),
            // first move to exceed the touch slop
            WebPointerEvent("pointermove", touch(0, 45, 60)),
            WebPointerEvent("pointermove", touch(1, 65, 50)),
            // Zoom in, zoom > 1
            WebPointerEvent("pointermove", touch(0, 40, 50)),
            WebPointerEvent("pointermove", touch(1, 70, 50)),
            WebPointerEvent("pointermove", touch(0, 30, 50)),
            WebPointerEvent("pointermove", touch(1, 80, 50)),
            // and now zoom out, zoom < 1
            WebPointerEvent("pointermove", touch(0, 35, 50)),
            WebPointerEvent("pointermove", touch(1, 75, 50)),
            WebPointerEvent("pointermove", touch(0, 37, 50)),
            WebPointerEvent("pointermove", touch(1, 73, 50)),
        )

        // Verify that at least one zoom value greater than 1.0 was recorded.
        assertEquals(7, zooms.size)
        println(zooms.joinToString(","))
        assertTrue(zooms[0] > 1 && zooms[0] < zooms[2]) // according to the Offset change
        assertTrue(zooms[3] < 1 && zooms[3] < zooms[5]) // according to the Offset change
    }

    @Test
    // test that multiple pointer events are handled
    fun canReceiveTouchEvents() = runApplicationTest {
        var lastPointerEvent: PointerEvent? = null

        createComposeWindow {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (coroutineContext.isActive) {
                        lastPointerEvent = awaitPointerEvent()
                    }
                }
            })
        }

        assertNull(lastPointerEvent)

        dispatchEvents(
            WebPointerEvent("pointerdown", touch(0, 50, 50)),
            WebPointerEvent("pointermove", touch(0, 60, 60))
        )

        awaitIdle()

        assertNotNull(lastPointerEvent)
        assertEquals(1, lastPointerEvent.changes.size)
        assertEquals(PointerEventType.Move, lastPointerEvent.type)

        dispatchEvents(
            WebPointerEvent("pointerup", touch(0, 60, 60))
        )
        lastPointerEvent = null

        dispatchEvents(
            WebPointerEvent("pointerdown", touch(1, 10, 10)),
            WebPointerEvent("pointermove", touch(1, 20, 20))
        )

        awaitIdle()

        assertNotNull(lastPointerEvent)
        assertEquals(1, lastPointerEvent!!.changes.size)
        assertEquals(PointerEventType.Move, lastPointerEvent!!.type)
    }

    @Test
    fun threeTouchesWithTouchEnd() = runApplicationTest {
        val pointerEvents = mutableListOf<PointerEvent>()

        createComposeWindow {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (coroutineContext.isActive) {
                        pointerEvents.add(awaitPointerEvent())
                    }
                }
            })
        }

        assertTrue(pointerEvents.isEmpty())

        dispatchEvents(
            // +1
            WebPointerEvent("pointerdown", touch(1, 10, 10)),
            // +2
            WebPointerEvent("pointerdown", touch(2, 20, 20)),
            // +3
            WebPointerEvent("pointerdown", touch(3, 30, 30)),
            // -3
            WebPointerEvent("pointerup", touch(3, 30, 30)),
            // -2
            WebPointerEvent("pointerup", touch(2, 20, 20)),
            // -1
            WebPointerEvent("pointerup", touch(1, 10, 10))
        )

        awaitIdle()

        val expected = """
            + 1
            + 1; + 2
            + 1; + 2; + 3
            + 1; + 2; - 3
            + 1; - 2
            - 1
        """.trimIndent()

        val actual = pointerEvents.joinToString("\n") { event ->
            event.changes.sortedBy { it.id.value }.joinToString("; ") {
                if (it.pressed) {
                    "+ ${it.id.value}"
                } else {
                    "- ${it.id.value}"
                }
            }
        }
        assertEquals(expected, actual)
    }

    @Test // Regression: https://youtrack.jetbrains.com/issue/CMP-10249
    fun pointerCancelDoesNotTriggerClick() = runApplicationTest {
        var clicksCount = 0

        createComposeWindow {
            Box(modifier = Modifier.fillMaxSize().clickable { clicksCount++ })
        }

        // Normal click should fire
        dispatchEvents(
            WebPointerEvent("pointerdown", touch(0, 50, 50)),
            WebPointerEvent("pointerup", touch(0, 50, 50))
        )
        awaitIdle()
        assertEquals(1, clicksCount)

        // pointercancel should NOT fire click (browser took over the gesture)
        dispatchEvents(
            WebPointerEvent("pointerdown", touch(1, 50, 50)),
            WebPointerEvent("pointercancel", touch(1, 50, 50))
        )
        awaitIdle()
        assertEquals(1, clicksCount) // still 1 — click was cancelled
    }

    private fun touch(id: Int, x: Int, y: Int) = PointerEventInit(
        pointerId = id,
        clientX = x,
        clientY = y,
        pointerType = "touch"
    )
}
