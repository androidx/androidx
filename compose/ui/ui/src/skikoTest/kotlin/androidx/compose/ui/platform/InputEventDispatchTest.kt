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

package androidx.compose.ui.platform

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.InternalKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.v2.runInternalSkikoComposeUiTest
import androidx.compose.ui.touch
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.launch


@OptIn(ExperimentalTestApi::class, InternalTestApi::class)
class InputEventDispatchTest {
    @Test
    fun dragPointerEventProcessedSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertEquals(0, scrollState.value)

        scene.sendPointerEvent(
            eventType = PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true)
            )
        )
        scene.sendPointerEvent(
            eventType = PointerEventType.Move,
            pointers = listOf(
                touch(50f, 10f, pressed = true)
            )
        )

        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun scrollPointerEventProcessedSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertEquals(0, scrollState.value)

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(50f, 50f),
            scrollDelta = Offset(0f, 40f)
        )

        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun panPointerEventProcessedSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertEquals(0, scrollState.value)

        scene.sendPointerEvent(
            eventType = PointerEventType.PanStart,
            position = Offset(50f, 50f),
        )
        scene.sendPointerEvent(
            eventType = PointerEventType.PanMove,
            position = Offset(50f, 50f),
            panGestureOffset = Offset(0f, 40f)
        )
        scene.sendPointerEvent(
            eventType = PointerEventType.PanEnd,
            position = Offset(50f, 50f),
        )

        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun scalePointerEventProcessedSynchronously() = runInternalSkikoComposeUiTest {
        var scale = 1f
        setContent {
            Box(modifier = Modifier.size(100.dp).onPointerEvent(PointerEventType.ScaleChange) {
                it.changes.forEach { change ->
                    scale *= change.scaleFactor
                }
            }) {
                Box(Modifier.size(200.dp))
            }
        }

        assertEquals(1f, scale)

        scene.sendPointerEvent(
            eventType = PointerEventType.ScaleChange,
            position = Offset(50f, 50f),
            scaleGestureFactor = 2.0f
        )

        assertNotEquals(1f, scale)
    }

    @Test
    fun pointerPressEventRunsScheduledCoroutinesSynchronously() = runInternalSkikoComposeUiTest {
        var pointerEventHandledInCoroutine by mutableStateOf(false)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitPointerEvent()
                            coroutineScope.launch {
                                pointerEventHandledInCoroutine = true
                            }
                        }
                    }
            )
        }

        assertFalse(pointerEventHandledInCoroutine)

        scene.sendPointerEvent(
            eventType = PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true)
            )
        )

        assertTrue(pointerEventHandledInCoroutine)
    }

    @Test
    fun pointerScrollEventRunsScheduledCoroutinesSynchronously() = runInternalSkikoComposeUiTest {
        var pointerHandledAfterDelay by mutableStateOf(false)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            awaitPointerEvent()
                            coroutineScope.launch {
                                pointerHandledAfterDelay = true
                            }
                        }
                    }
            )
        }

        assertFalse(pointerHandledAfterDelay)

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(50f, 50f),
            scrollDelta = Offset(0f, 40f)
        )

        assertTrue(pointerHandledAfterDelay)
    }

    @Test
    fun keyEventRunsScheduledCoroutinesSynchronously() = runInternalSkikoComposeUiTest {
        var keyHandledAfterDelay by mutableStateOf(false)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val focusRequester = remember { FocusRequester() }
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
                    .onKeyEvent {
                        coroutineScope.launch {
                            keyHandledAfterDelay = true
                        }
                        true
                    }
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertFalse(keyHandledAfterDelay)

        scene.sendKeyEvent(
            KeyEvent(
                nativeKeyEvent = InternalKeyEvent(
                    key = Key.A,
                    type = KeyEventType.KeyDown,
                    codePoint = 0,
                    modifiers = PointerKeyboardModifiers(),
                    nativeEvent = null
                )
            )
        )

        assertTrue(keyHandledAfterDelay)
    }

    @Test
    fun rotaryEventRunsScheduledCoroutinesSynchronously() = runInternalSkikoComposeUiTest {
        var eventHandledAfterDelay by mutableStateOf(false)
        setContent {
            val coroutineScope = rememberCoroutineScope()
            val focusRequester = remember { FocusRequester() }
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            eventHandledAfterDelay = true
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable(interactionSource = interactionSource)
            )
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        assertFalse(eventHandledAfterDelay)

        scene.sendRotaryScrollEvent(1f, 1f)

        assertTrue(eventHandledAfterDelay)
    }
}