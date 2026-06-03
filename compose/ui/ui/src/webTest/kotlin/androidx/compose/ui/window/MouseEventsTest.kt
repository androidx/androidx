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

package androidx.compose.ui.window

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.OnCanvasTests
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.test.runTest
import org.w3c.dom.pointerevents.PointerEvent
import org.w3c.dom.pointerevents.PointerEventInit
import androidx.compose.ui.input.pointer.PointerEvent as ComposePointerEvent
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import org.w3c.dom.events.WheelEvent
import org.w3c.dom.events.WheelEventInit


class MouseEventsTest : OnCanvasTests {

    @Test
    fun testPointerEvents() = runTest {
        val pointerEvents = mutableListOf<ComposePointerEvent>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                pointerEvents.add(awaitPointerEvent())
                            }
                        }
                    }
            ) {}
        }

        dispatchEvents(
            PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")),
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1, pointerType = "mouse")),
            PointerEvent("pointerup", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 0, pointerType = "mouse"))
        )

        assertEquals(3, pointerEvents.size)
        assertEquals(PointerEventType.Enter, pointerEvents[0].type)

        // Check for primary button
        assertEquals(PointerEventType.Press, pointerEvents[1].type)
        assertEquals(PointerButton.Primary, pointerEvents[1].button)
        assertEquals(PointerEventType.Release, pointerEvents[2].type)
        assertEquals(PointerButton.Primary, pointerEvents[2].button)

        dispatchEvents(
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 2, buttons = 2, pointerType = "mouse")),
            PointerEvent("pointerup", PointerEventInit(clientX = 100, clientY = 100, button = 2, buttons = 0, pointerType = "mouse"))
        )

        assertEquals(5, pointerEvents.size)

        // Check for secondary button
        assertEquals(PointerEventType.Press, pointerEvents[3].type)
        assertEquals(PointerButton.Secondary, pointerEvents[3].button)
        assertEquals(PointerEventType.Release, pointerEvents[4].type)
        assertEquals(PointerButton.Secondary, pointerEvents[4].button)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testOnClickWithPointerMatchers() = runTest {
        var primaryClickedCounter = 0
        var secondaryClickedCounter = 0

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onClick(matcher = PointerMatcher.Primary) { primaryClickedCounter++ }
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { secondaryClickedCounter++ }
            ) {}
        }

        dispatchEvents(
            PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")),
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1, pointerType = "mouse")),
            PointerEvent("pointerup", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 0, pointerType = "mouse"))
        )

        assertEquals(1, primaryClickedCounter)
        assertEquals(0, secondaryClickedCounter)

        dispatchEvents(
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 2, buttons = 2, pointerType = "mouse")),
            PointerEvent("pointerup", PointerEventInit(clientX = 100, clientY = 100, button = 2, buttons = 0, pointerType = "mouse"))
        )

        assertEquals(1, primaryClickedCounter)
        assertEquals(1, secondaryClickedCounter)
    }

    @Test
    fun testPointerButtonIsNullForNoClickEvents() = runTest {
        var event: ComposePointerEvent? = null

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                event = awaitPointerEvent()
                            }
                        }
                    }
            ) {}
        }

        assertEquals(null, event)

        dispatchEvents(PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")))
        assertEquals(PointerEventType.Enter, event!!.type)
        assertEquals(null, event.button)

        dispatchEvents(PointerEvent("pointermove", PointerEventInit(clientX = 101, clientY = 101, pointerType = "mouse")))
        assertEquals(PointerEventType.Move, event.type)
        assertEquals(null, event.button)

        dispatchEvents(PointerEvent("pointerleave", PointerEventInit(clientX = 0, clientY = 0, pointerType = "mouse")))
        assertEquals(PointerEventType.Exit, event.type)
        assertEquals(null, event.button)
    }

    @Test
    fun testWheelEventButtonsResolvedOnPointerDown() = runTest {
        // CMP-9900 [web] Wheel event resolves buttons state incorrectly in Safari and Firefox
        val pointerEvents = mutableListOf<ComposePointerEvent>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                pointerEvents.add(awaitPointerEvent())
                            }
                        }
                    }
            ) {}
        }

        dispatchEvents(
            PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")),
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1, pointerType = "mouse")),
        )

        dispatchEvents(WheelEvent("wheel", WheelEventInit(clientX = 100, clientY = 100, buttons = 0)))

        assertEquals(3, pointerEvents.size)
        assertEquals(PointerEventType.Enter, pointerEvents[0].type)
        assertEquals(PointerEventType.Press, pointerEvents[1].type)
        assertEquals(PointerEventType.Scroll, pointerEvents[2].type)
        assertEquals(true, pointerEvents[2].buttons.isPrimaryPressed)

        dispatchEvents(
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 2, buttons = 3, pointerType = "mouse")),
        )
        dispatchEvents(WheelEvent("wheel", WheelEventInit(clientX = 100, clientY = 100, buttons = 0)))

        assertEquals(5, pointerEvents.size)
        assertEquals(PointerEventType.Press, pointerEvents[3].type)
        assertEquals(PointerEventType.Scroll, pointerEvents[4].type)
        assertEquals(true, pointerEvents[4].buttons.isPrimaryPressed)
        assertEquals(true, pointerEvents[4].buttons.isSecondaryPressed)
    }

    @Test
    fun testPointerReleaseIsNotCorruptedByMouseWheel() = runTest {
        // CMP-9891 [Web] Mouse. Incorrect click detectionCMP-9891 [Web] Mouse. Incorrect click detection
        val pointerEvents = mutableListOf<ComposePointerEvent>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                pointerEvents.add(awaitPointerEvent())
                            }
                        }
                    }
            ) {}
        }

        dispatchEvents(
            PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")),
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1, pointerType = "mouse")),
        )

        dispatchEvents(WheelEvent("wheel", WheelEventInit(clientX = 100, clientY = 100, buttons = 0)))

        dispatchEvents(
            PointerEvent("pointermove", PointerEventInit(clientX = 101, clientY = 101, buttons = 1, pointerType = "mouse")),
            PointerEvent("pointerup", PointerEventInit(clientX = 101, clientY = 101, button = 0, buttons = 0, pointerType = "mouse"))
        )

        assertEquals(5, pointerEvents.size)
        assertEquals(PointerEventType.Enter, pointerEvents[0].type)
        assertEquals(PointerEventType.Press, pointerEvents[1].type)
        assertEquals(PointerEventType.Scroll, pointerEvents[2].type)
        assertEquals(PointerEventType.Move, pointerEvents[3].type)
        assertEquals(PointerEventType.Release, pointerEvents[4].type)
    }

    // https://youtrack.jetbrains.com/issue/CMP-10185/Send-missing-input-events-in-Web-Target
    @Test
    fun testMouseMoveUnpressingButtonsSendsReleaseEvent() = runTest {
        val pointerEvents = mutableListOf<ComposePointerEvent>()

        createComposeWindow {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (isActive) {
                                pointerEvents.add(awaitPointerEvent())
                            }
                        }
                    }
            ) {}
        }

        dispatchEvents(
            PointerEvent("pointerenter", PointerEventInit(clientX = 100, clientY = 100, pointerType = "mouse")),
            PointerEvent("pointerdown", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 1, pointerType = "mouse")),
            PointerEvent("pointermove", PointerEventInit(clientX = 100, clientY = 100, buttons = 1, pointerType = "mouse")),
        )

        val countBefore = pointerEvents.count { it.type == PointerEventType.Release }
        assertEquals(0, countBefore, "Release event sent too early")

        // Move while the button is no longer pressed -> a synthetic Release should be emitted.
        dispatchEvents(
            PointerEvent("pointermove", PointerEventInit(clientX = 100, clientY = 100, buttons = 0, pointerType = "mouse")),
        )

        assertEquals(1, pointerEvents.count { it.type == PointerEventType.Release }, "Release event not sent")

        // Make sure we don't send an extra release event afterward.
        dispatchEvents(
            PointerEvent("pointerup", PointerEventInit(clientX = 100, clientY = 100, button = 0, buttons = 0, pointerType = "mouse")),
        )

        assertEquals(1, pointerEvents.count { it.type == PointerEventType.Release }, "Extra release event sent")
    }
}