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
package androidx.compose.ui.test

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.PlatformRootForTest
import androidx.compose.ui.scene.ComposeScenePointer

@OptIn(InternalComposeUiApi::class)
internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher {
    return SkikoInputDispatcher(testContext, root as PlatformRootForTest)
}

private class TestInputEvent(
    val eventTime: Long,
    val action: () -> Unit
)

@OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
internal class SkikoInputDispatcher(
    private val testContext: TestContext,
    private val root: PlatformRootForTest
) : InputDispatcher(
    testContext,
    root,
    exitHoverOnPress = false,
    moveOnScroll = false,
) {

    private var currentClockTime = currentTime
    private var batchedEvents = mutableListOf<TestInputEvent>()
    private var modifiers = 0

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        val timeMillis = currentTime
        val pointers = lastPositions.map {
            ComposeScenePointer(
                id = PointerId(it.key.toLong()),
                position = it.value,
                pressed = true,
                type = PointerType.Touch
            )
        }
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Press,
                pointers = pointers,
                timeMillis = timeMillis
            )
        }
    }
    override fun PartialGesture.enqueueMove() {
        val timeMillis = currentTime
        val pointers = lastPositions.map {
            ComposeScenePointer(
                id = PointerId(it.key.toLong()),
                position = it.value,
                pressed = true,
                type = PointerType.Touch
            )
        }
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Move,
                pointers = pointers,
                timeMillis = timeMillis
            )
        }
    }

    override fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    ) {
        // TODO: add support for historical events
        enqueueMove()
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        val timeMillis = currentTime
        val pointers = lastPositions.map {
            ComposeScenePointer(
                id = PointerId(it.key.toLong()),
                position = it.value,
                pressed = pointerId != it.key,
                type = PointerType.Touch
            )
        }
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Release,
                pointers = pointers,
                timeMillis = timeMillis
            )
        }
    }

    override fun PartialGesture.enqueueCancel() {
        // desktop don't have cancel events as Android does
    }

    override fun MouseInputState.enqueuePress(buttonId: Int) {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Press,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis,
                button = PointerButton(buttonId)
            )
        }
    }

    override fun MouseInputState.enqueueMove() {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Move,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis
            )
        }
    }

    override fun MouseInputState.enqueueRelease(buttonId: Int) {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Release,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis,
                button = PointerButton(buttonId)
            )
        }
    }

    override fun MouseInputState.enqueueEnter() {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Enter,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis
            )
        }
    }

    override fun MouseInputState.enqueueExit() {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Exit,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis
            )
        }
    }

    override fun MouseInputState.enqueueCancel() {
        // desktop don't have cancel events as Android does
    }

    @OptIn(ExperimentalTestApi::class)
    override fun MouseInputState.enqueueScroll(delta: Float, scrollWheel: ScrollWheel) {
        val position = lastPosition
        val timeMillis = currentTime
        enqueue(timeMillis) {
            root.sendPointerEvent(
                PointerEventType.Scroll,
                position = position,
                type = PointerType.Mouse,
                timeMillis = timeMillis,
                scrollDelta = if (scrollWheel == ScrollWheel.Vertical) Offset(0f, delta) else Offset(delta, 0f)
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun updateModifiers(key: Key, down: Boolean) {
        modifiers = modifiers.updatedKeyboardModifiers(key, down)
    }

    override fun KeyInputState.enqueueDown(key: Key) {
        enqueue(currentTime) {
            updateModifiers(key = key, down = true)
            root.sendKeyEvent(keyEvent(key, KeyEventType.KeyDown, modifiers))
        }
    }

    override fun KeyInputState.enqueueUp(key: Key) {
        enqueue(currentTime) {
            updateModifiers(key = key, down = false)
            root.sendKeyEvent(keyEvent(key, KeyEventType.KeyUp, modifiers))
        }
    }

    override fun RotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        // desktop don't have rotary events as Android Wear does
    }

    override fun RotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        // desktop don't have rotary events as Android Wear does
    }

    private fun enqueue(timeMillis: Long, action: () -> Unit) {
        batchedEvents.add(TestInputEvent(timeMillis, action))
    }

    @OptIn(InternalTestApi::class)
    private fun advanceClockTime(millis: Long) {
        // Don't bother advancing the clock if there's nothing to advance
        if (millis > 0) {
            testContext.testOwner.mainClock.advanceTimeBy(millis, ignoreFrameDuration = true)
        }
    }

    override fun flush() {
        val copy = batchedEvents.toList()
        batchedEvents.clear()
        for (event in copy) {
            advanceClockTime(event.eventTime - currentClockTime)
            currentClockTime = event.eventTime
            event.action()
        }
    }

    override fun onDispose() {
        batchedEvents.clear()
    }
}

/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal expect fun keyEvent(
    key: Key, keyEventType: KeyEventType, modifiers: Int = 0
): KeyEvent

// The receiver (Int) is the current keyboard modifiers state
internal expect fun Int.updatedKeyboardModifiers(key: Key, down: Boolean): Int
