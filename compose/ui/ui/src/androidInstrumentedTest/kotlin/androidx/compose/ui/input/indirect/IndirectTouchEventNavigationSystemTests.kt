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

package androidx.compose.ui.input.indirect

import android.os.SystemClock
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION
import androidx.test.core.view.MotionEventBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith

/*
 * Verifies SOURCE_TOUCH_NAVIGATION [MotionEvent]s passing through the system properly trigger
 * Indirect Events (and their navigation movements) in Compose.
 *
 * Important Note: While we set every other value in the MotionEvent for these tests, we manually
 * set the IndirectTouchEventPrimaryAxis in Compose vs. deriving it from the [MotionEvent],
 * because setting the [MotionEvent] fields required for deriving the IndirectTouchEventPrimaryAxis
 * (InputDevice and InputDevice.MotionRange) do not have setters, and we can't mock() or spy() on
 * MotionEvent to make the tests work (see details below):
 *   - spy() - While spy "wraps" the original MotionEvent, it actually makes a copy of the
 * mNativePtr (so both the original MotionEvent and the spy have a non-zero, matching number).
 * During garbage collection, both the spy object and the original MotionEvent have their
 * `finalize()` methods triggered and you aren't allowed to override that method in spy. If the
 * mNativePtr is a non-zero value, the MotionEvent pointer associated with that id is deleted in
 * android_view_MotionEvent_nativeDispose(). Since they both have a non-zero value (the same value)
 * and both are triggered separately, one will delete the backing MotionEvent and the other will
 * cause a crash since it no longer exists.
 *   - mock() - While you can mock most everything you would need for [MotionEvent], you can not
 * mock native fields/functions that are required for the [android.view.GestureDetector] to work (it
 * does a native copy and crashes if it isn't a real [MotionEvent]). ([AndroidComposeView] uses
 * [android.view.GestureDetector] to detect indirect touch event gestures.)
 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
@RunWith(AndroidJUnit4::class)
class IndirectTouchEventNavigationSystemTests {
    @get:Rule val rule = createComposeRule()

    // Used to dispatch motion events
    private lateinit var rootView: AndroidComposeView
    private var receivedEvent: IndirectTouchEvent? = null

    private val timeBetweenEvents = 20L
    private val flingTriggeringDistanceBetweenEvents = 50
    private val nonFlingTriggeringDistanceBetweenEvents = 5

    // ----- Primary Directional Motion Axis X tests -----
    @Test
    fun swipeViaNavigationMotionEvent_swipeRightWithPrimaryAxisX_movesFocusableBoxToNext() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertFalse(box2Focused)
            assertTrue(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeLeftWithPrimaryAxisX_movesFocusableBoxToPrevious() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertTrue(box1Focused)
            assertFalse(box2Focused)
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeRightAndSmallDownWithPrimaryAxisX_movesFocusableBoxToNext() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertFalse(box2Focused)
            assertTrue(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeLeftAndSmallUpWithPrimaryAxisX_movesFocusableBoxToPrevious() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertTrue(box1Focused)
            assertFalse(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // A movement along the primary axis that is over the threshold for a swipe should not trigger
    // a navigation if the non-primary axis motion is larger (the larger motion always wins).
    @Test
    fun swipeViaNavigationMotionEvent_swipeRightAndDoubleSwipeDownWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += (flingTriggeringDistanceBetweenEvents * 2)

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += (flingTriggeringDistanceBetweenEvents * 2)

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += (flingTriggeringDistanceBetweenEvents * 2)

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // A movement along the primary axis that is over the threshold for a swipe should not trigger
    // a navigation if the non-primary axis motion is larger (the larger motion always wins).
    @Test
    fun swipeViaNavigationMotionEvent_swipeLeftAndDoubleSwipeUpWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (6 * flingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (6 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= (2 * flingTriggeringDistanceBetweenEvents)

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= (2 * flingTriggeringDistanceBetweenEvents)

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= (2 * flingTriggeringDistanceBetweenEvents)

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveRightWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveLeftWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * nonFlingTriggeringDistanceBetweenEvents)
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeDownWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeUpWithPrimaryAxisX_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.X
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // ----- Primary Directional Motion Axis Y tests -----
    @Test
    fun swipeViaNavigationMotionEvent_swipeDownWithPrimaryAxisY_movesFocusableBoxToNext() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertFalse(box2Focused)
            assertTrue(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeUpWithPrimaryAxisY_movesFocusableBoxToPrevious() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertTrue(box1Focused)
            assertFalse(box2Focused)
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeDownAndSmallRightWithPrimaryAxisY_movesFocusableBoxToNext() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertFalse(box2Focused)
            assertTrue(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeUpAndSmallLeftWithPrimaryAxisY_movesFocusableBoxToPrevious() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertTrue(box1Focused)
            assertFalse(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // A movement along the primary axis that is over the threshold for a swipe should not trigger
    // a navigation if the non-primary axis motion is larger (the larger motion always wins).
    @Test
    fun swipeViaNavigationMotionEvent_swipeDownAndDoubleSwipeRightWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += (flingTriggeringDistanceBetweenEvents * 2)
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += (flingTriggeringDistanceBetweenEvents * 2)
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += (flingTriggeringDistanceBetweenEvents * 2)
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // A movement along the primary axis that is over the threshold for a swipe should not trigger
    // a navigation if the non-primary axis motion is larger (the larger motion always wins).
    @Test
    fun swipeViaNavigationMotionEvent_swipeUpAndDoubleSwipeLeftWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (6 * flingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (6 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= (2 * flingTriggeringDistanceBetweenEvents)
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= (2 * flingTriggeringDistanceBetweenEvents)
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= (2 * flingTriggeringDistanceBetweenEvents)
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveDownWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveUpWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f + (3 * nonFlingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeRightWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeLeftWithPrimaryAxisY_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // ----- Primary Directional Motion Axis X AND Y tests -----
    // There is no behavior for Primary Directional Motion Axis X AND Y because it is translated
    // by the system into key up, down, left, and right.
    @Test
    fun swipeViaNavigationMotionEvent_swipeDownAndRightWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeUpAndLeftWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveDownAndRightWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += nonFlingTriggeringDistanceBetweenEvents
        indirectY += nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    // Checks a minimal move does NOT qualify as a swipe/fling and does not trigger a behavior.
    @Test
    fun nonSwipeViaNavigationMotionEvent_minimalMoveUpAndLeftWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * nonFlingTriggeringDistanceBetweenEvents)
        var indirectY = 100f + (3 * nonFlingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= nonFlingTriggeringDistanceBetweenEvents
        indirectY -= nonFlingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeRightWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeLeftWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        var indirectX = 100f + (3 * flingTriggeringDistanceBetweenEvents)
        val indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectX -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeDownWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY += flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Test
    fun swipeViaNavigationMotionEvent_swipeUpWithPrimaryAxisNone_noBehavior() {
        var focusManager: FocusManager? = null

        val testTagBox1 = "testTagBox1"
        var box1Focused = false

        val testTagBox2 = "testTagBox2"
        var box2Focused = false

        val testTagBox3 = "testTagBox3"
        var box3Focused = false

        val contentBoxSize = 100.dp
        val boxPadding = 10.dp

        val focusRequesterBox1 = FocusRequester()
        val focusRequesterBox2 = FocusRequester()
        val focusRequesterBox3 = FocusRequester()

        rule.setContent {
            rootView = LocalView.current as AndroidComposeView
            focusManager = LocalFocusManager.current

            ThreeStackedBoxes(
                boxSize = contentBoxSize,
                boxPadding = boxPadding,
                testTagBox1 = testTagBox1,
                onBox1FocusChanged = { box1Focused = it },
                focusRequesterBox1 = focusRequesterBox1,
                testTagBox2 = testTagBox2,
                onBox2FocusChanged = { box2Focused = it },
                focusRequesterBox2 = focusRequesterBox2,
                testTagBox3 = testTagBox3,
                onBox3FocusChanged = { box3Focused = it },
                focusRequesterBox3 = focusRequesterBox3,
                onIndirectTouchEventForAllBoxes = {
                    receivedEvent = it
                    // Never consume event so it triggers the navigation behavior we are testing
                    false
                },
            )
        }

        // --- Test assertions and actions ---

        // Clear focus to start
        rule.runOnIdle { focusManager!!.clearFocus(true) }

        // Request initial focus for center box
        rule.runOnIdle { focusRequesterBox2.requestFocus() }
        rule.waitForIdle()

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused)
            assertFalse(box3Focused)
        }

        val downTime = SystemClock.uptimeMillis()
        var eventTime = downTime
        val indirectX = 100f
        var indirectY = 100f + (3 * flingTriggeringDistanceBetweenEvents)

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_DOWN)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle {
            rootView.primaryDirectionalMotionAxisOverride =
                IndirectTouchEventPrimaryDirectionalMotionAxis.None
            rootView.dispatchGenericMotionEvent(downEvent)
        }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Press) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent1 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent1) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val moveEvent2 =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_MOVE)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(moveEvent2) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Move) }

        eventTime += timeBetweenEvents
        indirectY -= flingTriggeringDistanceBetweenEvents

        val upEvent =
            MotionEventBuilder.newBuilder()
                .setDownTime(downTime)
                .setEventTime(eventTime)
                .setAction(ACTION_UP)
                .setSource(SOURCE_TOUCH_NAVIGATION)
                .setPointer(indirectX, indirectY)
                .build()

        rule.runOnIdle { rootView.dispatchGenericMotionEvent(upEvent) }
        rule.runOnIdle { assertThat(receivedEvent?.type).isEqualTo(IndirectTouchEventType.Release) }

        rule.runOnIdle {
            assertFalse(box1Focused)
            assertTrue(box2Focused) // No behavior, stays on box 2
            assertFalse(box3Focused)
        }
    }

    @Composable
    private fun ThreeStackedBoxes(
        boxSize: Dp,
        boxPadding: Dp,
        testTagBox1: String,
        onBox1FocusChanged: (Boolean) -> Unit,
        focusRequesterBox1: FocusRequester,
        testTagBox2: String,
        onBox2FocusChanged: (Boolean) -> Unit,
        focusRequesterBox2: FocusRequester,
        testTagBox3: String,
        onBox3FocusChanged: (Boolean) -> Unit,
        focusRequesterBox3: FocusRequester,
        onIndirectTouchEventForAllBoxes: (IndirectTouchEvent) -> Boolean,
    ) {
        Layout(
            modifier = Modifier.onIndirectTouchEvent { onIndirectTouchEventForAllBoxes(it) },
            content = {
                // Box 1
                CustomBox(
                    modifier =
                        Modifier.testTag(testTagBox1)
                            .onFocusEvent { focusState: FocusState ->
                                onBox1FocusChanged(focusState.isFocused)
                            }
                            .focusRequester(focusRequesterBox1)
                            .focusable(),
                    contentSize = boxSize,
                    color = Color.Red,
                    padding = boxPadding,
                )

                // Box 2
                CustomBox(
                    modifier =
                        Modifier.testTag(testTagBox2)
                            .onFocusEvent { focusState: FocusState ->
                                onBox2FocusChanged(focusState.isFocused)
                            }
                            .focusRequester(focusRequesterBox2)
                            .focusable(),
                    contentSize = boxSize,
                    color = Color.Green,
                    padding = boxPadding,
                )

                // Box 3
                CustomBox(
                    modifier =
                        Modifier.testTag(testTagBox3)
                            .onFocusEvent { focusState: FocusState ->
                                onBox3FocusChanged(focusState.isFocused)
                            }
                            .focusRequester(focusRequesterBox3)
                            .focusable(),
                    contentSize = boxSize,
                    color = Color.Blue,
                    padding = boxPadding,
                )
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { measurable -> measurable.measure(constraints) }

            var yPosition = 0
            val layoutWidth = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
            val layoutHeight = placeables.sumOf { it.height }

            layout(layoutWidth, layoutHeight) {
                placeables.forEach { placeable ->
                    placeable.placeRelative(x = 0, y = yPosition)
                    yPosition += placeable.height
                }
            }
        }
    }

    // Don't have access to Foundation Composables, so these fill in for the tests.
    @Composable
    private fun CustomBox(
        modifier: Modifier = Modifier,
        contentSize: Dp,
        padding: Dp = 0.dp,
        color: Color,
        content: @Composable () -> Unit = {},
    ) {
        Layout(
            content = content,
            modifier =
                modifier.drawBehind {
                    val paddingPx = padding.toPx()
                    val left = paddingPx
                    val top = paddingPx
                    val right = size.width - paddingPx
                    val bottom = size.height - paddingPx

                    drawRect(
                        SolidColor(color),
                        topLeft = Offset(left, top),
                        size = Size(right - left, bottom - top),
                    )
                },
        ) { measurables, incomingConstraints ->
            val totalWidthPx = (contentSize + 2 * padding).roundToPx()
            val totalHeightPx = (contentSize + 2 * padding).roundToPx()

            val boxConstraints =
                incomingConstraints.copy(
                    minWidth = totalWidthPx,
                    maxWidth = totalWidthPx,
                    minHeight = totalHeightPx,
                    maxHeight = totalHeightPx,
                )

            val contentConstraints =
                Constraints(
                    minWidth = (contentSize - 2 * padding).roundToPx().coerceAtLeast(0),
                    maxWidth = (contentSize - 2 * padding).roundToPx().coerceAtLeast(0),
                    minHeight = (contentSize - 2 * padding).roundToPx().coerceAtLeast(0),
                    maxHeight = (contentSize - 2 * padding).roundToPx().coerceAtLeast(0),
                )
            val placeables = measurables.map { it.measure(contentConstraints) }

            layout(totalWidthPx, totalHeightPx) {
                val paddingPx = padding.roundToPx()
                placeables.forEach { placeable -> placeable.placeRelative(paddingPx, paddingPx) }
            }
        }
    }
}
