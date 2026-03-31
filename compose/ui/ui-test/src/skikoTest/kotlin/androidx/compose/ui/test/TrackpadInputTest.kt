/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the trackpad-event sending functionality of the test framework.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)
class TrackpadInputTest {

    @Test
    fun testPerformClick() = runComposeUiTest {
        var clicked = false
        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .clickable {
                        clicked = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                click()
            }
            assertTrue(clicked, "Click event not received")
        }
    }

    @Test
    fun testMouseClick() = runComposeUiTest {
        var clicked = false
        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .clickable {
                        clicked = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                click()
            }
            assertTrue(clicked, "Mouse click event not received")
        }
    }

    @Test
    fun testMousePressDragAndRelease() = runComposeUiTest {
        var pressDetected = false
        var dragDetected = false
        var releaseDetected = false

        fun assertState(expectedPress: Boolean, expectedDrag: Boolean, expectedRelease: Boolean) {
            assertEquals(expectedPress, pressDetected, "Press detection mismatch")
            assertEquals(expectedDrag, dragDetected, "Drag detection mismatch")
            assertEquals(expectedRelease, releaseDetected, "Release detection mismatch")
        }

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown().also { it.consume() }
                            pressDetected = true

                            awaitDragOrCancellation(down.id) ?: return@awaitEachGesture
                            dragDetected = true

                            waitForUpOrCancellation() ?: return@awaitEachGesture
                            releaseDetected = true
                        }
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                press()
            }
            assertState(expectedPress = true, expectedDrag = false, expectedRelease = false)

            performTrackpadInput {
                moveBy(Offset(x = 50f, y = 50f))
            }
            assertState(expectedPress = true, expectedDrag = true, expectedRelease = false)

            performTrackpadInput {
                release()
            }
            assertState(expectedPress = true, expectedDrag = true, expectedRelease = true)
        }
    }

    private fun Modifier.onPointerEvent(
        eventType: PointerEventType,
        onEvent: AwaitPointerEventScope.(event: PointerEvent) -> Unit
    ) = pointerInput(eventType, onEvent) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == eventType) {
                    onEvent(event)
                }
            }
        }
    }

    @Test
    fun testMouseEnterExit() = runComposeUiTest {
        var mouseEnterDetected = false
        var mouseExitDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onPointerEvent(PointerEventType.Enter) {
                        mouseEnterDetected = true
                    }
                    .onPointerEvent(PointerEventType.Exit) {
                        mouseExitDetected = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                enter()
            }
            assertTrue(mouseEnterDetected, "Mouse entered event not detected")

            performTrackpadInput {
                exit()
            }
            assertTrue(mouseExitDetected, "Mouse exited event not detected")
        }
    }

    @Test
    fun updatePointerToDoesNotSendMoveEvent() = runComposeUiTest {
        var mouseMoveDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onPointerEvent(PointerEventType.Move) {
                        mouseMoveDetected = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                updatePointerTo(Offset(10f, 10f))
                press()
                release()
            }
            assertFalse(mouseMoveDetected, "Mouse move detected")
        }
    }

    @Test
    fun testPan() = runComposeUiTest {
        var panDelta = Offset.Unspecified

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.PanMove) {
                                panDelta = event.changes.first().panOffset
                            }
                        }
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                pan(Offset(0f, 50f))
            }
            assertEquals(Offset(0f, 50f), panDelta, "Wrong vertical scroll delta detected")

            performTrackpadInput {
                pan(Offset(30f, 0f))
            }
            assertEquals(Offset(30f, 0f), panDelta, "Wrong horizontal scroll delta detected")
        }
    }

    @Test
    fun testPinch() = runComposeUiTest {
        var scale: Float? = null

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.ScaleChange) {
                                scale = event.changes.first().scaleFactor
                            }
                        }
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                scale(0.5f)
            }
            assertEquals(0.5f, scale, "Wrong vertical scroll delta detected")

            performTrackpadInput {
                scale(2.0f)
            }
            assertEquals(2.0f, scale, "Wrong horizontal scroll delta detected")
        }
    }

    @Test
    fun testClick() = runComposeUiTest {
        var clickDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                        clickDetected = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                click()
            }
            assertTrue(clickDetected, "Mouse click not detected")
        }
    }

    @Test
    fun testRightClick() = runComposeUiTest {
        var rightClickDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                        rightClickDetected = true
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                rightClick()
            }
            assertTrue(rightClickDetected, "Mouse right-click not detected")
        }
    }

    @Test
    fun testDoubleClick() = runComposeUiTest {
        var doubleClickDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onDoubleClick = {
                            doubleClickDetected = true
                        },
                        onClick = {}
                    )
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                doubleClick()
            }
            assertTrue(doubleClickDetected, "Mouse double-click not detected")
        }
    }

    @Test
    fun testTripleClick() = runComposeUiTest {
        var clickCount = 0

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onClick = {
                            clickCount += 1
                        }
                    )
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                tripleClick()
            }
            assertEquals(3, clickCount, "Mouse triple-click not detected")
        }
    }

    @Test
    fun testLongClick() = runComposeUiTest {
        var longClickDetected = false

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .onClick(
                        matcher = PointerMatcher.mouse(PointerButton.Primary),
                        onLongClick = {
                            longClickDetected = true
                        },
                        onClick = {}
                    )
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                longClick()
            }
            assertTrue(longClickDetected, "Mouse long-click not detected")
        }
    }

    @Test
    fun testDragAndDrop() = runComposeUiTest {
        var dragOffset = Offset.Zero

        setContent {
            Box(
                Modifier
                    .testTag("tag")
                    .size(100.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, dragAmount ->
                            dragOffset += dragAmount
                        }
                    }
            )
        }

        onNodeWithTag("tag").apply {
            performTrackpadInput {
                dragAndDrop(
                    start = Offset(10f, 10f),
                    end = Offset(20f, 30f),
                )
            }
            assertOffsetEquals(
                expected = Offset(10f, 20f),
                actual = dragOffset,
                message = "Wrong drag-and-drop offset detected"
            )
        }
    }

    private fun assertOffsetEquals(
        expected: Offset,
        actual: Offset,
        toleratedDistance: Float = 0.5f,
        message: String? = null
    ) {
        assertTrue(
            actual = (expected - actual).getDistance() < toleratedDistance,
            message = (if (message == null) "" else "$message; ") +
                "expected=$expected, actual=$actual, toleratedDistance=$toleratedDistance"
        )
    }
}