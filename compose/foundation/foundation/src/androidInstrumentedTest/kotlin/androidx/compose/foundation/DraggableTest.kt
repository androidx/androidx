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

package androidx.compose.foundation

import android.os.SystemClock
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitAllPointersUp
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalPointerSlopOrCancellation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertModifierIsPure
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.absoluteValue
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalIndirectTouchTypeApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class DraggableTest {

    @get:Rule val rule = createComposeRule()

    private val draggableBoxTag = "dragTag"

    private val focusRequester = FocusRequester()

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun draggable_horizontalDrag() {
        var total = 0f
        setDraggableContent { Modifier.draggable(Orientation.Horizontal) { total += it } }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        val lastTotal =
            rule.runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_verticalDrag() {
        var total = 0f
        setDraggableContent { Modifier.draggable(Orientation.Vertical) { total += it } }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }
        val lastTotal =
            rule.runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_indirectTouchDrag_worksOnBothOrientations_primaryXAxis() {
        var total = 0f
        var orientation by mutableStateOf(Orientation.Horizontal)
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(orientation) { total += it }
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadStart, 0f),
                Offset(TouchPadEnd, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadEnd, 0f),
                Offset(TouchPadStart, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }

        rule.runOnIdle {
            orientation = Orientation.Vertical
            total = 0f
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadStart, 0f),
                Offset(TouchPadEnd, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadEnd, 0f),
                Offset(TouchPadStart, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_indirectTouchDrag_worksOnBothOrientations_primaryYAxis() {
        var total = 0f
        var orientation by mutableStateOf(Orientation.Horizontal)
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(orientation) { total += it }
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadStart),
                Offset(0f, TouchPadEnd),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.Y,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadEnd),
                Offset(0f, TouchPadStart),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.Y,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }

        rule.runOnIdle {
            orientation = Orientation.Vertical
            total = 0f
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadStart),
                Offset(0f, TouchPadEnd),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.Y,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadEnd),
                Offset(0f, TouchPadStart),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.Y,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_indirectTouchDrag_worksOnBothOrientations_noPrimaryAxis() {
        var total = 0f
        var orientation by mutableStateOf(Orientation.Horizontal)
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(orientation) { total += it }
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadStart, 0f),
                Offset(TouchPadEnd, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.None,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(TouchPadEnd, 0f),
                Offset(TouchPadStart, 0f),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.None,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }

        rule.runOnIdle {
            orientation = Orientation.Vertical
            total = 0f
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadStart),
                Offset(0f, TouchPadEnd),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.None,
            )
        rule.runOnIdle { assertThat(total).isGreaterThan(0) }
        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectSwipeEvent(
                Offset(0f, TouchPadEnd),
                Offset(0f, TouchPadStart),
                primaryDirectionalMotionAxis = IndirectTouchEventPrimaryDirectionalMotionAxis.None,
            )
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_indirectTouchDrag_notFocused_shouldNotReceiveEvents() {
        var total = 0f
        var orientation by mutableStateOf(Orientation.Horizontal)
        setDraggableContent(enableInitialFocus = false) {
            Modifier.draggable(orientation) { total += it }
        }

        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()
        rule.runOnIdle { assertThat(total).isEqualTo(0.0f) }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeBackward()
        rule.runOnIdle { assertThat(total).isEqualTo(0.0f) }
    }

    @Test
    fun draggable_verticalDrag_newState() {
        var total = 0f
        setDraggableContent { Modifier.draggable(Orientation.Vertical) { total += it } }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100,
            )
        }
        val lastTotal =
            rule.runOnIdle {
                assertThat(total).isGreaterThan(0)
                total
            }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isEqualTo(lastTotal) }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isLessThan(0.01f) }
    }

    @Test
    fun draggable_startStop() {
        var startTrigger = 0f
        var stopTrigger = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStarted = { startTrigger += 1 },
                onDragStopped = { stopTrigger += 1 },
            ) {}
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable_indirectTouchDrag_startStop() {
        var startTrigger = 0
        var stopTrigger = 0
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStarted = { startTrigger += 1 },
                onDragStopped = { stopTrigger += 1 },
            ) {}
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeBackward()

        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    fun draggable_disabledWontCallLambda() {
        var total = 0f
        val enabled = mutableStateOf(true)
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, enabled = enabled.value) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        val prevTotal =
            rule.runOnIdle {
                assertThat(total).isGreaterThan(0f)
                enabled.value = false
                total
            }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    fun draggable_indirectTouchEvent_disabledWontCallLambda() {
        var total = 0f
        val enabled = mutableStateOf(true)
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(Orientation.Horizontal, enabled = enabled.value) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()

        val prevTotal =
            rule.runOnIdle {
                assertThat(total).isGreaterThan(0f)
                enabled.value = false
                total
            }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()
        rule.runOnIdle { assertThat(total).isEqualTo(prevTotal) }
    }

    @Test
    fun draggable_velocityProxy() {
        var velocityTriggered = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { velocityTriggered = it },
            ) {}
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                durationMillis = 100,
            )
        }
        rule.runOnIdle { assertThat(velocityTriggered - 112f).isLessThan(0.1f) }
    }

    @Test
    fun draggable_indirectTouchEvent_velocityProxy() {
        var velocityTriggered = 0f
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { velocityTriggered = it },
            ) {}
        }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()
        rule.runOnIdle { assertThat(velocityTriggered).isGreaterThan(0.0f) }
    }

    @Test
    fun draggable_startWithoutSlop_ifAnimating() {
        var total = 0f
        setDraggableContent {
            Modifier.draggable(Orientation.Horizontal, startDragImmediately = true) { total += it }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100,
            )
        }
        rule.runOnIdle {
            // should be exactly 100 as there's no slop
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun draggable_cancel_callsDragStop() {
        var total = 0f
        var dragStopped = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                startDragImmediately = true,
            ) {
                total += it
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            cancel()
        }
        rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    fun draggable_indirectTouchCancel_callsDragStop() {
        var total = 0f
        var dragStopped = 0
        setDraggableContent(enableInitialFocus = true) {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                startDragImmediately = true,
            ) {
                total += it
            }
        }
        rule.onNodeWithTag(draggableBoxTag).sendIndirectTouchCancelEvent()

        rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1)
        }
    }

    // regression test for b/176971558
    @Test
    fun draggable_immediateStart_callsStopWithoutSlop() {
        var total = 0f
        var dragStopped = 0f
        var dragStarted = 0f
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                onDragStarted = { dragStarted += 1 },
                startDragImmediately = true,
            ) {
                total += it
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performMouseInput { this.press() }
        rule.runOnIdle { assertThat(dragStarted).isEqualTo(1f) }
        rule.onNodeWithTag(draggableBoxTag).performMouseInput { this.release() }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable_callsDragStop_whenNewState() {
        var dragStopped = 0f
        val state = mutableStateOf(DraggableState {})
        setDraggableContent {
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                state = state.value,
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            state.value = DraggableState { /* do nothing */ }
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable_callsDragStop_whenNewOrientation() {
        var dragStopped = 0f
        var orientation by mutableStateOf(Orientation.Horizontal)
        setDraggableContent {
            Modifier.draggable(
                orientation = orientation,
                onDragStopped = { dragStopped += 1 },
                onDrag = {},
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            orientation = Orientation.Vertical
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable_callsDragStop_whenDisabled() {
        var dragStopped = 0f
        var enabled by mutableStateOf(true)
        setDraggableContent {
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                enabled = enabled,
                onDrag = {},
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            enabled = false
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable_callsDragStop_whenNewReverseDirection() {
        var dragStopped = 0f
        var reverseDirection by mutableStateOf(false)
        setDraggableContent {
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDragStopped = { dragStopped += 1 },
                onDrag = {},
                reverseDirection = reverseDirection,
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(dragStopped).isEqualTo(0f)
            reverseDirection = true
        }
        rule.runOnIdle { assertThat(dragStopped).isEqualTo(1f) }
    }

    @Test
    fun draggable_updates_startDragImmediately() {
        var total = 0f
        var startDragImmediately by mutableStateOf(false)
        var touchSlop: Float? = null
        setDraggableContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDrag = { total += it },
                startDragImmediately = startDragImmediately,
            )
        }
        val delta = touchSlop!! / 2f
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(delta, 0f))
            up()
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(0f)
            startDragImmediately = true
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(delta, 0f))
            up()
        }
        rule.runOnIdle { assertThat(total).isEqualTo(delta) }
    }

    @Test
    fun draggable_updates_onDragStarted() {
        var total = 0f
        var onDragStarted1Calls = 0
        var onDragStarted2Calls = 0
        var onDragStarted: (Offset) -> Unit by mutableStateOf({ onDragStarted1Calls += 1 })
        setDraggableContent {
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDrag = { total += it },
                onDragStarted = onDragStarted,
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(onDragStarted1Calls).isEqualTo(1)
            assertThat(onDragStarted2Calls).isEqualTo(0)
            onDragStarted = { onDragStarted2Calls += 1 }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle {
            assertThat(onDragStarted1Calls).isEqualTo(1)
            assertThat(onDragStarted2Calls).isEqualTo(1)
        }
    }

    @Test
    fun draggable_updates_onDragStopped() {
        var total = 0f
        var onDragStopped1Calls = 0
        var onDragStopped2Calls = 0
        var onDragStopped: (Float) -> Unit by mutableStateOf({ onDragStopped1Calls += 1 })
        setDraggableContent {
            Modifier.draggable(
                orientation = Orientation.Horizontal,
                onDrag = { total += it },
                onDragStopped = onDragStopped,
            )
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        rule.runOnIdle {
            assertThat(onDragStopped1Calls).isEqualTo(0)
            assertThat(onDragStopped2Calls).isEqualTo(0)
            onDragStopped = { onDragStopped2Calls += 1 }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput { up() }
        rule.runOnIdle {
            // We changed the lambda before we ever stopped dragging, so only the new one should be
            // called
            assertThat(onDragStopped1Calls).isEqualTo(0)
            assertThat(onDragStopped2Calls).isEqualTo(1)
        }
    }

    @Test
    fun draggable_resumesNormally_whenInterruptedWithHigherPriority() = runBlocking {
        var total = 0f
        var dragStopped = 0f
        val state = DraggableState { total += it }
        setDraggableContent {
            if (total < 20f) {
                Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    onDragStopped = { dragStopped += 1 },
                    state = state,
                )
            } else Modifier
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }
        val prevTotal =
            rule.runOnIdle {
                assertThat(dragStopped).isEqualTo(0f)
                assertThat(total).isGreaterThan(0f)
                total
            }
        state.drag(MutatePriority.PreventUserInput) { dragBy(123f) }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123f)
            assertThat(dragStopped).isEqualTo(1f)
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            up()
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }
        rule.runOnIdle { assertThat(total).isGreaterThan(prevTotal + 123f) }
    }

    @Test
    fun draggable_resumesNormally_whenInterruptedWithHigherPriority_indirectTouch() = runBlocking {
        var total = 0f
        var dragStopped = 0f
        val state = DraggableState { total += it }

        setDraggableContent(enableInitialFocus = true) {
            if (total < 20f) {
                Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    onDragStopped = { dragStopped += 1 },
                    state = state,
                )
            } else Modifier
        }

        val stepSize = Offset((TouchPadEnd - TouchPadStart) / 10, 0f)
        var currentTime = SystemClock.uptimeMillis()
        var currentValue = Offset(TouchPadStart, 0f)

        rule.onNodeWithTag(draggableBoxTag).sendIndirectTouchPressEvent(currentTime, currentValue)
        currentTime += 16L
        currentValue += stepSize

        val (newCurrentTime, newCurrentValue) =
            rule
                .onNodeWithTag(draggableBoxTag)
                .sendIndirectTouchMoveEvents(
                    5,
                    currentTime,
                    currentValue,
                    16L,
                    stepSize,
                    IndirectTouchEventPrimaryDirectionalMotionAxis.X,
                )

        val prevTotal =
            rule.runOnIdle {
                assertThat(dragStopped).isEqualTo(0f)
                assertThat(total).isGreaterThan(0f)
                total
            }
        state.drag(MutatePriority.PreventUserInput) { dragBy(123f) }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123f)
            assertThat(dragStopped).isEqualTo(1f)
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectTouchReleaseEvent(newCurrentTime, newCurrentValue)
        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()
        rule.runOnIdle { assertThat(total).isGreaterThan(prevTotal + 123f) }
    }

    @Test
    fun draggable_noNestedDrag() {
        var innerDrag = 0f
        var outerDrag = 0f
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier.testTag(draggableBoxTag).size(300.dp).draggable(
                            Orientation.Horizontal
                        ) {
                            outerDrag += it
                        },
                ) {
                    Box(
                        modifier =
                            Modifier.size(300.dp).draggable(Orientation.Horizontal) { delta ->
                                innerDrag += delta / 2
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
            )
        }
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            // draggable doesn't participate in nested scrolling, so outer should receive 0 events
            assertThat(outerDrag).isEqualTo(0f)
        }
    }

    @Test
    fun draggable_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setDraggableContent {
            scope = rememberCoroutineScope()
            Modifier.draggable(Orientation.Horizontal, interactionSource = interactionSource) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput { up() }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_interactionSource_withIndirectTouches() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        setDraggableContent(enableInitialFocus = true) {
            scope = rememberCoroutineScope()
            Modifier.draggable(Orientation.Horizontal, interactionSource = interactionSource) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        val stepSize = Offset((TouchPadEnd - TouchPadStart) / 10, 0f)
        var currentTime = SystemClock.uptimeMillis()
        var currentValue = Offset(TouchPadStart, 0f)

        rule.onNodeWithTag(draggableBoxTag).sendIndirectTouchPressEvent(currentTime, currentValue)
        currentTime += 16L
        currentValue += stepSize

        val (newCurrentTime, newCurrentValue) =
            rule
                .onNodeWithTag(draggableBoxTag)
                .sendIndirectTouchMoveEvents(
                    5,
                    currentTime,
                    currentValue,
                    16L,
                    stepSize,
                    IndirectTouchEventPrimaryDirectionalMotionAxis.X,
                )

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule
            .onNodeWithTag(draggableBoxTag)
            .sendIndirectTouchReleaseEvent(newCurrentTime, newCurrentValue)

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitDraggableBox by mutableStateOf(true)

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitDraggableBox) {
                    Box(
                        modifier =
                            Modifier.testTag(draggableBoxTag).size(100.dp).draggable(
                                orientation = Orientation.Horizontal,
                                interactionSource = interactionSource,
                            ) {}
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose draggable
        rule.runOnIdle { emitDraggableBox = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_interactionSource_resetWhenEnabledChanged() {
        val interactionSource = MutableInteractionSource()
        val enabledState = mutableStateOf(true)

        var scope: CoroutineScope? = null

        setDraggableContent {
            scope = rememberCoroutineScope()
            Modifier.draggable(
                Orientation.Horizontal,
                enabled = enabledState.value,
                interactionSource = interactionSource,
            ) {}
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch { interactionSource.interactions.collect { interactions.add(it) } }

        rule.runOnIdle { assertThat(interactions).isEmpty() }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.runOnIdle { enabledState.value = false }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start).isEqualTo(interactions[0])
        }
    }

    @Test
    fun draggable_velocityIsLimitedByViewConfiguration() {
        var latestVelocity = 0f
        val maxVelocity = 1000f

        rule.setContent {
            val viewConfig = LocalViewConfiguration.current
            val newConfig =
                object : ViewConfiguration by viewConfig {
                    override val maximumFlingVelocity: Float
                        get() = maxVelocity
                }
            CompositionLocalProvider(LocalViewConfiguration provides newConfig) {
                Box {
                    Box(
                        modifier =
                            Modifier.testTag(draggableBoxTag)
                                .size(100.dp)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = { latestVelocity = it },
                                    onDrag = {},
                                )
                    )
                }
            }
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.centerLeft,
                end = this.centerRight,
                endVelocity = 2000f,
            )
        }
        rule.runOnIdle { assertThat(latestVelocity).isEqualTo(maxVelocity) }
    }

    @Test
    fun draggable_indirectTouch_velocityIsLimitedByViewConfiguration() {
        var latestVelocity = 0f
        val maxVelocity = 10f

        rule.setContent {
            val viewConfig = LocalViewConfiguration.current
            val newConfig =
                object : ViewConfiguration by viewConfig {
                    override val maximumFlingVelocity: Float
                        get() = maxVelocity
                }
            CompositionLocalProvider(LocalViewConfiguration provides newConfig) {
                Box {
                    Box(
                        modifier =
                            Modifier.testTag(draggableBoxTag)
                                .size(100.dp)
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = { latestVelocity = it },
                                    onDrag = {},
                                )
                                .focusRequester(focusRequester)
                                .focusTarget()
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }

        rule.onNodeWithTag(draggableBoxTag).sendIndirectSwipeForward()
        rule.runOnIdle { assertThat(latestVelocity).isEqualTo(maxVelocity) }
    }

    @Test
    fun draggable_interactionSource_resetWhenInteractionSourceChanged() {
        val interactionSource1 = MutableInteractionSource()
        val interactionSource2 = MutableInteractionSource()
        val interactionSourceState = mutableStateOf(interactionSource1)

        var scope: CoroutineScope? = null

        setDraggableContent {
            scope = rememberCoroutineScope()
            Modifier.draggable(
                Orientation.Horizontal,
                interactionSource = interactionSourceState.value,
            ) {}
        }

        val interactions1 = mutableListOf<Interaction>()
        val interactions2 = mutableListOf<Interaction>()

        scope!!.launch { interactionSource1.interactions.collect { interactions1.add(it) } }

        rule.runOnIdle {
            assertThat(interactions1).isEmpty()
            assertThat(interactions2).isEmpty()
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
            moveBy(Offset(visibleSize.width / 2f, 0f))
        }

        rule.runOnIdle {
            assertThat(interactions1).hasSize(1)
            assertThat(interactions1.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions2).isEmpty()
        }

        rule.runOnIdle { interactionSourceState.value = interactionSource2 }

        rule.runOnIdle {
            assertThat(interactions1).hasSize(2)
            assertThat(interactions1.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions1[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions1[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions1[0])
            // Currently we don't emit drag start for an in progress drag, but this might change
            // in the future.
            assertThat(interactions2).isEmpty()
        }
    }

    @Test
    fun draggable_cancelMidDown_shouldContinueWithNextDown() {
        var total = 0f

        setDraggableContent { Modifier.draggable(Orientation.Horizontal) { total += it } }

        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            cancel()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performMouseInput {
            enter()
            exit()
        }

        assertThat(total).isEqualTo(0f)
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
        }

        assertThat(total).isGreaterThan(0f)
    }

    @Test
    fun draggable_noMomentumDragging_onDragStopped_shouldGenerateZeroVelocity() {
        val delta = -10f
        var flingVelocity = Float.NaN
        setDraggableContent {
            Modifier.draggable(
                state = rememberDraggableState {},
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> flingVelocity = velocity },
            )
        }

        // Drag, stop and release. The resulting velocity should be zero because we lost the
        // gesture momentum.
        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            // generate various move events
            repeat(30) { moveBy(Offset(0f, delta), delayMillis = 16L) }
            // stop for a moment
            advanceEventTime(3000L)
            up()
        }
        rule.runOnIdle { Assert.assertEquals(0f, flingVelocity) }
    }

    @Test
    fun onDragStopped_inputChanged_shouldNotCancelScope() {
        val enabled = mutableStateOf(true)
        lateinit var runningJob: Job
        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .draggable(
                            enabled = enabled.value,
                            state = rememberDraggableState {},
                            orientation = Orientation.Vertical,
                            onDragStopped = { _ ->
                                runningJob = launch { delay(10_000L) } // long running operation
                            },
                        )
            )
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle {
            enabled.value = false // cancels pointer input scope
        }

        rule.runOnIdle {
            assertTrue { runningJob.isActive } // check if scope is still active
        }
    }

    @Test
    fun onDragStarted_startDragImmediately_offsetShouldBePositionOfDownEvent() {
        var onDragStartedOffset = Offset.Unspecified
        var downEventPosition = Offset.Unspecified
        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .draggable(
                            enabled = true,
                            state = rememberDraggableState {},
                            orientation = Orientation.Vertical,
                            onDragStarted = { offset -> onDragStartedOffset = offset },
                            startDragImmediately = true,
                        )
            )
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            downEventPosition = center
            down(center)
            moveBy(Offset(100f, 100f))
            up()
        }

        rule.runOnIdle { assertEquals(downEventPosition, onDragStartedOffset) }
    }

    @Test
    fun onDragStarted_startDragImmediatelyFalse_offsetShouldBePostSlopPosition_vertical() {
        var onDragStartedOffset = Offset.Unspecified
        var downEventPosition = Offset.Unspecified
        var touchSlop = 0f
        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop

            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .draggable(
                            enabled = true,
                            state = rememberDraggableState {},
                            orientation = Orientation.Vertical,
                            onDragStarted = { offset -> onDragStartedOffset = offset },
                            startDragImmediately = false,
                        )
            )
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            downEventPosition = center
            down(center)
            moveBy(Offset(0f, 100f))
            up()
        }

        rule.runOnIdle { assertEquals(downEventPosition.y + touchSlop, onDragStartedOffset.y) }
    }

    @Test
    fun onDragStarted_startDragImmediatelyFalse_offsetShouldBePostSlopPosition_horizontal() {
        var onDragStartedOffset = Offset.Unspecified
        var downEventPosition = Offset.Unspecified
        var touchSlop = 0f
        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop

            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .draggable(
                            enabled = true,
                            state = rememberDraggableState {},
                            orientation = Orientation.Horizontal,
                            onDragStarted = { offset -> onDragStartedOffset = offset },
                            startDragImmediately = false,
                        )
            )
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            downEventPosition = center
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }

        rule.runOnIdle { assertEquals(downEventPosition.x + touchSlop, onDragStartedOffset.x) }
    }

    @Test
    fun testInspectableValue() {
        rule.setContent {
            val modifier =
                Modifier.draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState {},
                ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("draggable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable())
                .containsExactly(
                    "orientation",
                    "enabled",
                    "reverseDirection",
                    "interactionSource",
                    "startDragImmediately",
                    "onDragStarted",
                    "onDragStopped",
                    "state",
                )
        }
    }

    @Test // b/355160589
    fun onDragStopped_withSameTimeStamps_shouldNotPropagateNanVelocity() {
        setDraggableContent {
            Modifier.draggable(
                Orientation.Horizontal,
                onDragStopped = { assertThat(it).isNotNaN() },
            ) {}
        }

        rule.onNodeWithTag(draggableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f), 0L)
            moveBy(Offset(100f, 0f), 0L)
            moveBy(Offset(100f, 0f), 0L)
            up()
        }

        rule.waitForIdle()
    }

    @Test
    fun nestedDraggable_childStopsConsuming_shouldAllowParentToConsume() {
        var parentDeltas by mutableFloatStateOf(0.0f)
        var childDeltas by mutableFloatStateOf(0.0f)

        val parentDraggableController = DraggableState { parentDeltas += it }
        val childDraggableController = DraggableState { childDeltas += it }

        rule.setContent {
            Box(
                Modifier.size(400.dp)
                    .draggable(parentDraggableController, orientation = Orientation.Vertical)
            ) {
                if (childDeltas.absoluteValue < 80.0f) {
                    Box(
                        Modifier.testTag("childDraggable")
                            .size(400.dp)
                            .draggable(childDraggableController, orientation = Orientation.Vertical)
                    )
                }
            }
        }

        rule.onRoot().performTouchInput { swipeUp() }

        rule.onNodeWithTag("childDraggable").assertDoesNotExist()

        rule.runOnIdle {
            assertThat(parentDeltas).isNonZero()
            assertThat(childDeltas).isNonZero()
        }
    }

    // b/380242617
    @Test
    fun nestedDraggable_childStopsConsumingMidway_shouldAllowParentToConsume() {
        var parentDeltas = 0.0f
        val parentDraggableController = DraggableState { parentDeltas += it }
        var keepChild by mutableStateOf(true)
        var childDeltas = 0f
        val dragDistance = with(rule.density) { 400.dp.toPx() }

        var eventDropHappened = false
        var extraUnconsumed = 0f
        var parentStartOffset = 0f

        rule.setContent {
            Box(
                Modifier.size(400.dp)
                    .draggable(
                        parentDraggableController,
                        onDragStarted = { parentStartOffset = it.y },
                        orientation = Orientation.Vertical,
                    )
            ) {
                if (keepChild) {
                    Box(
                        Modifier.testTag("childDraggable").size(400.dp).pointerInput(keepChild) {
                            awaitPointerEventScope {
                                val down = awaitFirstDown()
                                awaitVerticalPointerSlopOrCancellation(
                                    pointerId = down.id,
                                    pointerType = down.type,
                                ) { change, overSlop ->
                                    change.consume()
                                    childDeltas += overSlop
                                }
                                while (keepChild) {
                                    val drag = awaitDragOrCancellation(down.id) ?: break
                                    if (
                                        childDeltas.absoluteValue < dragDistance * 0.2 ||
                                            eventDropHappened
                                    ) {
                                        childDeltas += drag.positionChange().y
                                        drag.consume()
                                    } else {
                                        extraUnconsumed += drag.positionChange().y
                                        eventDropHappened = true
                                    }
                                    if (childDeltas.absoluteValue > dragDistance * 0.4) {
                                        keepChild = false
                                    }
                                }
                                awaitAllPointersUp()
                            }
                        }
                    )
                }
            }
        }

        var swipeStartOffset = 0f
        rule.onRoot().performTouchInput {
            swipeUp()
            swipeStartOffset = bottom
        }

        rule.onNodeWithTag("childDraggable").assertDoesNotExist()

        // the total drag distance should be reflected on the parent's drag callbacks.
        rule.runOnIdle {
            assertThat(dragDistance)
                .isWithin(2f)
                .of(
                    (parentStartOffset - swipeStartOffset).absoluteValue +
                        parentDeltas.absoluteValue
                )
        }
    }

    @Test
    fun onGesturePickUp_doesNotNeedToWaitForCompleteTouchSlop() {
        var dragStarted = false
        var composeNestedScrollable by mutableStateOf(true)
        var touchSlop = 0f
        rule.setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                Modifier.fillMaxSize()
                    .draggable(
                        orientation = Orientation.Vertical,
                        onDragStarted = { dragStarted = true },
                        state = rememberDraggableState {},
                    )
                    .then(
                        if (composeNestedScrollable) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
            )
        }

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(x = 0f, y = touchSlop + 10f))
        }

        assertThat(dragStarted).isFalse()

        composeNestedScrollable = false

        // one event for entering pick up.
        rule.onRoot().performTouchInput { moveBy(Offset(x = 0f, y = touchSlop / 2)) }

        // one event for triggering touch slop
        rule.onRoot().performTouchInput { moveBy(Offset(x = 0f, y = touchSlop / 2)) }
        assertThat(dragStarted).isTrue()
    }

    // On tests this wouldn't fail due to usage of a UnconfinedTestDispatcher. Using a
    // standard test dispatcher will reveal this issue.
    @Test
    fun assertDraggableCallbackOrder() {
        var onStartCalled = false
        val draggableController = DraggableState { assertTrue { onStartCalled } }

        rule.setContent {
            Box(
                Modifier.size(400.dp)
                    .draggable(
                        draggableController,
                        orientation = Orientation.Vertical,
                        onDragStarted = { onStartCalled = true },
                        onDragStopped = { assertTrue { onStartCalled } },
                    )
            )
        }

        rule.onRoot().performTouchInput { swipeUp() }
        rule.waitForIdle()
    }

    @Test
    fun assertDraggableCallbackOrder_usingIndirectTouch() {
        var onStartCalled = false
        val draggableController = DraggableState { assertTrue { onStartCalled } }

        rule.setContent {
            Box(
                Modifier.size(400.dp)
                    .draggable(
                        draggableController,
                        orientation = Orientation.Vertical,
                        onDragStarted = { onStartCalled = true },
                        onDragStopped = { assertTrue { onStartCalled } },
                    )
                    .focusRequester(focusRequester)
                    .focusTarget()
            )
        }
        rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }
        rule.onRoot().sendIndirectSwipeForward()
        rule.waitForIdle()
    }

    @Test
    fun equalInputs_shouldResolveToEquals() {
        val state = DraggableState {}

        assertModifierIsPure { toggleInput ->
            if (toggleInput) {
                Modifier.draggable(state, Orientation.Horizontal)
            } else {
                Modifier.draggable(state, Orientation.Vertical)
            }
        }
    }

    @Test
    fun gesturePickUp_doesNotStealFromOngoingGesture() {
        var innerDeltas = 0f
        rule.setContent {
            Box(
                Modifier.size(400.dp)
                    .draggable(
                        rememberDraggableState {},
                        onDragStarted = {
                            throw AssertionError(
                                "Outer Draggable onDragStarted shouldn't be called"
                            )
                        },
                        orientation = Orientation.Horizontal,
                    )
            ) {
                Box(
                    Modifier.size(400.dp)
                        .draggable(
                            rememberDraggableState { innerDeltas += it },
                            orientation = Orientation.Vertical,
                        )
                )
            }
        }

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(0f, 100f)) // start moving inner draggable
        }

        rule.runOnIdle { assertThat(innerDeltas).isNonZero() }
        val previousInnerDeltas = innerDeltas

        rule.onRoot().performTouchInput {
            moveBy(Offset(100f, 0f)) // moving completely on the cross axis
            moveBy(Offset(100f, 0f))
        }

        rule.runOnIdle { assertThat(innerDeltas).isEqualTo(previousInnerDeltas) }

        rule.onRoot().performTouchInput {
            moveBy(Offset(0f, 100f)) // moving again on the correct axis
            up()
        }

        rule.runOnIdle { assertThat(innerDeltas).isGreaterThan(previousInnerDeltas) }
    }

    @Test
    fun parentConsumedDuringTheMainPass_shouldGiveItUp() {
        var deltas = 0f
        var consumedDuringFinalPass = 0f
        val state = DraggableState { deltas += it }

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Main)
                                    val change = event.changes.first()
                                    // this is a movement
                                    if (
                                        !change.changedToUpIgnoreConsumed() &&
                                            !change.changedToDownIgnoreConsumed()
                                    ) {
                                        consumedDuringFinalPass += change.positionChange().y
                                        change.consume()
                                    }
                                }
                            }
                        }
                        .draggable(state = state, orientation = Orientation.Vertical)
            )
        }

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(0f, 10f))
            moveBy(Offset(0f, 60f))
            up()
        }

        // draggable shouldn't get deltas because the pointer input used them
        rule.runOnIdle { assertThat(consumedDuringFinalPass.absoluteValue).isNotEqualTo(0f) }
        rule.runOnIdle { assertThat(deltas.absoluteValue).isEqualTo(0f) }
    }

    @Test
    fun parentConsumedDuringTheFinalPass_shouldGiveItUp() {
        var deltas = 0f
        var consumedDuringFinalPass = 0f
        val state = DraggableState { deltas += it }

        rule.setContent {
            Box(
                modifier =
                    Modifier.testTag(draggableBoxTag)
                        .size(100.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    val change = event.changes.first()
                                    // this is a movement
                                    if (
                                        !change.changedToUpIgnoreConsumed() &&
                                            !change.changedToDownIgnoreConsumed()
                                    ) {
                                        consumedDuringFinalPass += change.positionChange().y
                                        change.consume()
                                    }
                                }
                            }
                        }
                        .draggable(state = state, orientation = Orientation.Vertical)
            )
        }

        rule.onRoot().performTouchInput {
            down(center)
            moveBy(Offset(0f, 10f))
            moveBy(Offset(0f, 60f))
            up()
        }

        // draggable shouldn't get deltas because the pointer input used them
        rule.runOnIdle { assertThat(consumedDuringFinalPass.absoluteValue).isNotEqualTo(0f) }
        rule.runOnIdle { assertThat(deltas.absoluteValue).isEqualTo(0f) }
    }

    private fun setDraggableContent(
        enableInitialFocus: Boolean = false,
        draggableFactory: @Composable () -> Modifier,
    ) {
        val initialFocus =
            if (enableInitialFocus) {
                Modifier.focusRequester(focusRequester).focusTarget()
            } else {
                Modifier
            }
        rule.setContent {
            Box {
                val draggable = draggableFactory()
                Box(
                    modifier =
                        Modifier.testTag(draggableBoxTag)
                            .size(100.dp)
                            .then(draggable)
                            .then(initialFocus)
                )
            }
        }

        if (enableInitialFocus)
            rule.runOnIdle { assertThat(focusRequester.requestFocus()).isTrue() }
    }

    private fun Modifier.draggable(
        orientation: Orientation,
        enabled: Boolean = true,
        reverseDirection: Boolean = false,
        interactionSource: MutableInteractionSource? = null,
        startDragImmediately: Boolean = false,
        onDragStarted: (startedPosition: Offset) -> Unit = {},
        onDragStopped: (velocity: Float) -> Unit = {},
        onDrag: (Float) -> Unit,
    ): Modifier = composed {
        val state = rememberDraggableState(onDrag)
        draggable(
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            startDragImmediately = startDragImmediately,
            onDragStarted = { onDragStarted(it) },
            onDragStopped = { onDragStopped(it) },
            state = state,
        )
    }
}
