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

package androidx.compose.ui.platform

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameMillis
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.scene.BaseComposeScene
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runInternalSkikoComposeUiTest
import androidx.compose.ui.touch
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.launch

/**
 * Tests the ordering of the phases of the [BaseComposeScene.render] loop.
 */
@OptIn(ExperimentalTestApi::class, InternalTestApi::class)
class RenderPhasesTest {
    /**
     * Test the order of the basic phases:
     * 1. withFrameMillis/Nanos
     * 2. Composition
     * 3. Layout
     * 4. Draw
     * 5. Effects launched in the composition.
     */
    @Test
    fun testBasicOrder() = runInternalSkikoComposeUiTest {
        mainClock.autoAdvance = false

        val phases = mutableListOf<String>()
        var logPhases by mutableStateOf(false)

        setContent {
            LaunchedEffect(Unit) {
                var stop = false
                while (!stop) {
                    withFrameNanos {
                        if (logPhases) {
                            phases.add("frame")
                            stop = true
                        }
                    }
                }
            }

            if (logPhases) {
                phases.add("composition")
            }

            Layout(
                modifier = Modifier.size(100.dp),
                measurePolicy = { _, _ ->
                    if (logPhases) {
                        phases.add("layout")
                    }

                    layout(100, 100) { }
                }
            )

            Canvas(Modifier.size(100.dp)) {
                if (logPhases) {
                    phases.add("draw")
                }
            }

            LaunchedEffect(logPhases) {
                if (logPhases) {
                    phases.add("effects")
                }
            }
        }

        assertContentEquals(emptyList(), phases)
        logPhases = true
        mainClock.advanceTimeByFrame()
        waitForIdle()  // Make the effects run
        assertContentEquals(
            expected = listOf("frame", "composition", "layout", "draw", "effects"),
            actual = phases
        )
    }

    /**
     * Test that synthetic events are delivered after effects have run.
     */
    @Test
    fun syntheticEventsDispatchedAfterEffects() = runInternalSkikoComposeUiTest {
        val phases = mutableListOf<String>()
        var showNewBox by mutableStateOf(false)

        setContent {
            Box(Modifier.size(100.dp).testTag("box")) {
                if (showNewBox) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .onPointerEvent(eventType = PointerEventType.Enter) {
                                phases.add("syntheticEvent")
                            }
                    )
                    LaunchedEffect(Unit) {
                        phases.add("effects")
                    }
                }
            }
        }

        assertContentEquals(emptyList(), phases)
        onNodeWithTag("box").performMouseInput {
            moveTo(Offset(50f, 50f))
        }
        showNewBox = true
        waitForIdle()
        assertContentEquals(
            expected = listOf("effects", "syntheticEvent"),
            actual = phases
        )
    }

    /**
     * Test that coroutines launched in a composition's [rememberCoroutineScope] execute in the
     * correct order, and only after the draw phase.
     */
    @Test
    fun coroutinesDispatchedAfterDraw() = runInternalSkikoComposeUiTest {
        mainClock.autoAdvance = false

        val phases = mutableListOf<String>()
        var startTest by mutableStateOf(false)
        setContent {
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                while (true) {
                    withFrameMillis {
                        if (startTest) {
                            coroutineScope.launch {
                                phases.add("launchedWithFrame")
                            }
                        }
                    }
                }
            }

            if (startTest) {
                coroutineScope.launch {
                    phases.add("launchedFromComposition")
                }
            }

            LaunchedEffect(startTest) {
                if (startTest) {
                    coroutineScope.launch {
                        phases.add("launchedFromEffect")
                    }
                }
            }

            Canvas(Modifier.size(100.dp)) {
                if (startTest) {
                    phases.add("draw")
                }
            }
        }

        assertContentEquals(emptyList(), phases)
        startTest = true
        mainClock.advanceTimeByFrame()
        assertContentEquals(
            expected = listOf(
                "draw",
                "launchedWithFrame",
                "launchedFromComposition",
                "launchedFromEffect"
            ),
            actual = phases
        )
    }

    @Test
    fun layoutScopeInvalidationDoesNotCauseRemeasure() = runInternalSkikoComposeUiTest {
        var measureCount = 0
        var layoutCount = 0

        var layoutScopeInvalidation by mutableStateOf(Unit, neverEqualPolicy())

        setContent {
            Layout(
                measurePolicy = { _, _ ->
                    measureCount += 1
                    layout(100, 100) {
                        @Suppress("UNUSED_EXPRESSION")
                        layoutScopeInvalidation
                        layoutCount += 1
                    }
                }
            )
        }

        assertEquals(measureCount, layoutCount)

        measureCount = 0
        layoutCount = 0
        layoutScopeInvalidation = Unit
        waitForIdle()

        assertEquals(0, measureCount)
        assertTrue(layoutCount > 0)
    }

    @Test
    fun readingStateInLayoutModifiedByMeasureDoesNotCauseInfiniteRemeasureAndLayout() {
        // https://github.com/JetBrains/compose-multiplatform/issues/4760
        runInternalSkikoComposeUiTest {
            mainClock.autoAdvance = false
            val state = mutableStateOf(0)
            // Don't read the state initially so that the test fails rather than getting stuck
            var readStateInLayout by mutableStateOf(false)
            var layoutCount = 0
            setContent {
                Layout(
                    measurePolicy = { _, _ ->
                        val prevValue = Snapshot.withoutReadObservation {
                            state.value
                        }
                        state.value = prevValue + 1
                        layout(100, 100) {
                            if (readStateInLayout) {
                                state.value  // Read the state value!
                            }
                            layoutCount++
                        }
                    },
                )
            }

            mainClock.advanceTimeByFrame()
            assertEquals(1, state.value)

            readStateInLayout = true
            layoutCount = 0
            mainClock.advanceTimeByFrame()
            assertEquals(1, layoutCount)

            // Check that no more layout happens
            mainClock.advanceTimeByFrame()
            assertEquals(1, layoutCount)
        }
    }

    @Test
    fun measureAndLayoutRunsAgainBeforeDraw() = runInternalSkikoComposeUiTest {
        // Android runs measureAndLayout again right before drawing; validate this behavior.
        val state = mutableStateOf(0)
        val events = mutableListOf<String>()
        setContent {
            Layout(
                modifier = Modifier.drawBehind {
                    events.add("draw.${state.value}")
                },
                measurePolicy = { _, _ ->
                    events.add("measure.${state.value}")
                    layout(100, 100) {
                        events.add("layout.${state.value}")
                        if (state.value == 0) {
                            // Invalidate the scope exactly once
                            state.value += 1
                        }
                    }
                },
            )
        }

        assertContentEquals(
            expected = listOf("measure.0", "layout.0", "measure.1", "layout.1", "draw.1"),
            actual = events
        )
    }

    @Test
    fun dragPointerEventHandlesScrollUpdatesSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertFalse(scene.hasInvalidations())
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
        assertTrue(scene.hasInvalidations())
        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun scrollPointerEventHandlesScrollUpdatesSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertFalse(scene.hasInvalidations())
        assertEquals(0, scrollState.value)

        scene.sendPointerEvent(
            eventType = PointerEventType.Scroll,
            position = Offset(50f, 50f),
            scrollDelta = Offset(0f, 40f)
        )

        assertTrue(scene.hasInvalidations())
        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun panPointerEventHandlesScrollUpdatesSynchronously() = runInternalSkikoComposeUiTest {
        val scrollState = ScrollState(0)
        setContent {
            Box(modifier = Modifier.size(100.dp).verticalScroll(scrollState)) {
                Box(Modifier.size(200.dp))
            }
        }

        assertFalse(scene.hasInvalidations())
        assertEquals(0, scrollState.value)

        scene.sendPointerEvent(
            eventType = PointerEventType.PanMove,
            position = Offset(50f, 50f),
            panGestureOffset = Offset(0f, 40f)
        )

        assertTrue(scene.hasInvalidations())
        assertNotEquals(0, scrollState.value)
    }

    @Test
    fun scalePointerEventHandlesScrollUpdatesSynchronously() = runInternalSkikoComposeUiTest {
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

        assertFalse(scene.hasInvalidations())
        assertEquals(1f, scale)

        scene.sendPointerEvent(
            eventType = PointerEventType.ScaleChange,
            position = Offset(50f, 50f),
            scaleGestureFactor = 2.0f
        )

        assertTrue(scene.hasInvalidations())
        assertNotEquals(1f, scale)
    }

    @Test
    fun pointerPressEventProcessesScheduledCoroutines() = runInternalSkikoComposeUiTest {
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
            eventType = PointerEventType.Press,
            pointers = listOf(
                touch(50f, 50f, pressed = true)
            )
        )

        assertTrue(pointerHandledAfterDelay)
    }

    @Test
    fun pointerScrollEventProcessesScheduledCoroutines() = runInternalSkikoComposeUiTest {
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
    fun keyEventsProcessesScheduledCoroutines() = runInternalSkikoComposeUiTest {
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
    fun rotaryEventsProcessesScheduledCoroutines() = runInternalSkikoComposeUiTest {
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