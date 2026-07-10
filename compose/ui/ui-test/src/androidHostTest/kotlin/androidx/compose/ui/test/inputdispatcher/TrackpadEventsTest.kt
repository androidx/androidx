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

package androidx.compose.ui.test.inputdispatcher

import android.os.Build
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_BUTTON_PRESS
import android.view.MotionEvent.ACTION_BUTTON_RELEASE
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.BUTTON_PRIMARY
import android.view.MotionEvent.BUTTON_SECONDARY
import android.view.MotionEvent.BUTTON_TERTIARY
import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.AndroidInputDispatcher
import androidx.compose.ui.test.InputDispatcher.Companion.eventPeriodMillis
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.compose.ui.test.TrackpadButton
import androidx.compose.ui.test.util.assertHasValidEventTimes
import androidx.compose.ui.test.util.verifyTouchEvent
import androidx.compose.ui.test.util.verifyTouchPointer
import androidx.compose.ui.test.util.verifyTrackpadEvent
import androidx.compose.ui.test.util.verifyTrackpadFakePointerEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.collections.removeFirst as removeFirstKt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests if [AndroidInputDispatcher.enqueueTrackpadPress] and friends work. */
@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class TrackpadEventsTest : InputDispatcherTest() {
    companion object {
        // Positions
        private val position1 = Offset(1f, 1f)
        private val position2 = Offset(2f, 2f)
        private val position3 = Offset(3f, 3f)
        private val position4 = Offset(4f, 4f)
        private val positionMin1 = Offset(-1f, -1f)
        private val positionMin2 = Offset(-2f, -2f)
    }

    @Test
    fun oneButton_primary() {
        oneButton(TrackpadButton.Primary, BUTTON_PRIMARY)
    }

    @Test
    fun oneButton_secondary() {
        oneButton(TrackpadButton.Secondary, BUTTON_SECONDARY)
    }

    @Test
    fun oneButton_tertiary() {
        oneButton(TrackpadButton.Tertiary, BUTTON_TERTIARY)
    }

    private fun oneButton(TrackpadButton: TrackpadButton, expectedButtonState: Int) {
        // Scenario:
        // move trackpad
        // press button
        // move trackpad
        // release button
        // move trackpad

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadMove(position1)
        subject.verifyTrackpadPosition(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position2)
        subject.verifyTrackpadPosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadRelease(TrackpadButton.buttonId)
        expectedEvents += 4 // release + up + enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position3)
        subject.verifyTrackpadPosition(position3)
        expectedEvents += 1 // hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position1, 0)
        }

        // exit + down + press
        t = 0L // down resets downTime
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, expectedButtonState)
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, position1, expectedButtonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, position1, expectedButtonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position2, expectedButtonState)
        }

        // release + up + enter + hover
        t += eventPeriodMillis
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyTrackpadEvent(ACTION_BUTTON_RELEASE, t, position2, 0)
            upEvent.verifyTrackpadEvent(ACTION_UP, t, position2, 0)
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position3, 0)
        }
    }

    @Test
    fun oneButton_cancel() {
        // Scenario:
        // press primary button
        // cancel trackpad gesture

        var expectedEvents = 0
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadCancel()
        expectedEvents += 1 // cancel
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // cancel
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTrackpadEvent(ACTION_CANCEL, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun hoverOutOfRootBounds() {
        // Scenario:
        // move trackpad within bounds
        // move trackpad out of bounds
        // move trackpad out of bounds again
        // move trackpad into bounds

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadMove(position1)
        subject.verifyTrackpadPosition(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(positionMin1)
        subject.verifyTrackpadPosition(positionMin1)
        expectedEvents += 1 // exit (suppressed move)
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(positionMin2)
        subject.verifyTrackpadPosition(positionMin2)
        expectedEvents += 0 // nothing (suppressed move)
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position2)
        subject.verifyTrackpadPosition(position2)
        expectedEvents += 2 // enter + hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position1, 0)
        }

        // exit (suppressed move)
        t += eventPeriodMillis
        events.removeFirst(1).let { (exitEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, positionMin1, 0)
        }

        // nothing (suppressed move)
        t += eventPeriodMillis

        // enter + hover
        t += eventPeriodMillis
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }
    }

    @Test
    fun moveOutOfRootBounds() {
        // Scenario:
        // press primary button within bounds
        // move trackpad out of bounds
        // press secondary button
        // release secondary button
        // release primary button

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(positionMin1)
        subject.verifyTrackpadPosition(positionMin1)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Secondary.buttonId)
        expectedEvents += 1 // move (suppressed press)
        subject.advanceEventTime()
        subject.enqueueTrackpadRelease(TrackpadButton.Secondary.buttonId)
        expectedEvents += 1 // move (suppressed release)
        subject.advanceEventTime()
        subject.enqueueTrackpadRelease(TrackpadButton.Primary.buttonId)
        expectedEvents += 1 // up (suppressed release)
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // move (suppressed press)
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // move (suppressed release)
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, positionMin1, buttonState)
        }

        // up (suppressed release)
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_UP, t, positionMin1, 0)
        }
    }

    @Test
    fun twoButtons() {
        // Scenario:
        // press primary button
        // move trackpad
        // press secondary button
        // move trackpad
        // release primary button
        // move trackpad
        // release secondary button
        // move trackpad
        // press tertiary button

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position1)
        subject.verifyTrackpadPosition(position1)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Secondary.buttonId)
        expectedEvents += 2 // move + press
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position2)
        subject.verifyTrackpadPosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadRelease(TrackpadButton.Primary.buttonId)
        expectedEvents += 2 // release + move
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position3)
        subject.verifyTrackpadPosition(position3)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadRelease(TrackpadButton.Secondary.buttonId)
        expectedEvents += 4 // release + up + enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position4)
        subject.verifyTrackpadPosition(position4)
        expectedEvents += 1 // hover
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Tertiary.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position1, buttonState)
        }

        // move + press
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(2).let { (moveEvent, pressEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position1, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, position1, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position2, buttonState)
        }

        // release + move
        t += eventPeriodMillis
        buttonState = BUTTON_SECONDARY
        events.removeFirst(2).let { (releaseEvent, moveEvent) ->
            releaseEvent.verifyTrackpadEvent(ACTION_BUTTON_RELEASE, t, position2, buttonState)
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position2, buttonState)
        }

        // move
        t += eventPeriodMillis
        events.removeFirst(1).let { (moveEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, position3, buttonState)
        }

        // release + up + enter + hover
        t += eventPeriodMillis
        events.removeFirst(4).let { (releaseEvent, upEvent, enterEvent, hoverEvent) ->
            releaseEvent.verifyTrackpadEvent(ACTION_BUTTON_RELEASE, t, position3, 0)
            upEvent.verifyTrackpadEvent(ACTION_UP, t, position3, 0)
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position3, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position3, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position4, 0)
        }

        // exit + down + press
        t = 0L // down resets downTime
        buttonState = BUTTON_TERTIARY
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position4, buttonState)
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, position4, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, position4, buttonState)
        }
    }

    @Test
    fun manualEnterExit() {
        // Scenario:
        // send hover enter
        // move trackpad
        // send hover exit

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadEnter(position1)
        subject.verifyTrackpadPosition(position1)
        expectedEvents += 1 // enter
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position2)
        subject.verifyTrackpadPosition(position2)
        expectedEvents += 1 // move
        subject.advanceEventTime()
        subject.enqueueTrackpadExit(position3)
        subject.verifyTrackpadPosition(position3)
        expectedEvents += 1 // exit
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter
        var t = 0L
        events.removeFirst(1).let { (enterEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, 0)
        }

        // hover
        t += eventPeriodMillis
        events.removeFirst(1).let { (hoverEvent) ->
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }

        // exit
        t += eventPeriodMillis
        events.removeFirst(1).let { (exitEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position3, 0)
        }
    }

    @Test
    fun pan() {
        // Scenario:
        // move trackpad
        // pan vertically by 10f
        // press primary button
        // pan horizontally by 10f

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadMove(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadPanStart()
        subject.advanceEventTime()
        subject.enqueueTrackpadPanMove(Offset(0f, 10f))
        subject.advanceEventTime()
        subject.enqueueTrackpadPanEnd()
        expectedEvents += 5 // exit + down + move + up + enter
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 3 // exit + down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadPanStart()
        subject.advanceEventTime()
        subject.enqueueTrackpadPanMove(Offset(10f, 0f))
        subject.advanceEventTime()
        subject.enqueueTrackpadPanEnd()
        expectedEvents += 8 // release + up + enter + exit + down + move + up + enter
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        var buttonState = 0
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
        }

        // exit + down + move + up + enter
        t += eventPeriodMillis
        events.removeFirst(5).let { (exitEvent, downEvent, moveEvent, upEvent, enterEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, buttonState)

            t = 0L // down resets downTime

            downEvent.verifyTrackpadEvent(
                ACTION_DOWN,
                t,
                position1,
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to 0f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to 0f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )

            t += eventPeriodMillis

            moveEvent.verifyTrackpadEvent(
                ACTION_MOVE,
                t,
                position1 + Offset(0f, 10f),
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to 0f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to -10f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )

            t += eventPeriodMillis

            upEvent.verifyTrackpadEvent(
                ACTION_UP,
                t,
                position1 + Offset(0f, 10f),
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to 0f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to 0f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
        }

        // exit + down + press
        t = 0L // down resets downTime
        buttonState = BUTTON_PRIMARY
        events.removeFirst(3).let { (exitEvent, downEvent, pressEvent) ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, buttonState)
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, position1, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, position1, buttonState)
        }

        // release + up + enter + exit + down + move + up + enter
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(8).let {
            (
                releaseEvent,
                upEvent1,
                enterEvent1,
                exitEvent,
                downEvent,
                moveEvent,
                upEvent2,
                enterEvent2) ->
            releaseEvent.verifyTrackpadEvent(ACTION_BUTTON_RELEASE, t, position1, buttonState)
            upEvent1.verifyTrackpadEvent(ACTION_UP, t, position1, buttonState)
            enterEvent1.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, buttonState)

            t = 0L // down resets downTime

            downEvent.verifyTrackpadEvent(
                ACTION_DOWN,
                t,
                position1,
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to 0f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to 0f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )

            t += eventPeriodMillis

            moveEvent.verifyTrackpadEvent(
                ACTION_MOVE,
                t,
                position1 + Offset(10f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to -10f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to 0f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )

            t += eventPeriodMillis

            upEvent2.verifyTrackpadEvent(
                ACTION_UP,
                t,
                position1 + Offset(10f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE to 0f,
                MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE to 0f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            enterEvent2.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
        }
    }

    @Test
    fun pinch() {
        // Scenario:
        // move trackpad
        // pinch together
        // pinch apart

        var expectedEvents = 0
        subject.verifyTrackpadPosition(Offset.Zero)
        subject.enqueueTrackpadMove(position1)
        expectedEvents += 2 // enter + hover
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleStart()
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleChange(0.9f)
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleEnd()
        expectedEvents += 7 // exit + down + pointerDown + move + pointerUp + up + enter
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleStart()
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleChange(1.1f)
        subject.advanceEventTime()
        subject.enqueueTrackpadScaleEnd()
        expectedEvents += 7 // exit + down + pointerDown + move + pointerUp + up + enter
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // enter + hover
        var t = 0L
        var buttonState = 0
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position1, buttonState)
        }

        // exit + down + pointerDown + move + pointerUp + up + enter
        t += eventPeriodMillis
        events.removeFirst(7).let {
            (exitEvent, downEvent, pointerDownEvent, moveEvent, pointerUpEvent, upEvent, enterEvent)
            ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, buttonState)

            t = 0L // down resets downTime

            downEvent.verifyTrackpadEvent(
                ACTION_DOWN,
                t,
                position1 + Offset(-100f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 1f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            pointerDownEvent.verifyTrackpadEvent(
                ACTION_POINTER_DOWN,
                t,
                position1 + Offset(-100f, 0f),
                buttonState,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
                expectedActionIndex = 1,
            )
            pointerDownEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-100f, 0f))
            pointerDownEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(100f, 0f))

            t += eventPeriodMillis

            moveEvent.verifyTrackpadEvent(
                ACTION_MOVE,
                t,
                position1 + Offset(-90f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 0.9f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
            )
            moveEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-90f, 0f))
            moveEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(90f, 0f))

            t += eventPeriodMillis

            pointerUpEvent.verifyTrackpadEvent(
                ACTION_POINTER_UP,
                t,
                position1 + Offset(-90f, 0f),
                buttonState,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
                expectedActionIndex = 1,
            )
            pointerUpEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-90f, 0f))
            pointerUpEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(90f, 0f))
            upEvent.verifyTrackpadEvent(
                ACTION_UP,
                t,
                position1 + Offset(-90f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 1f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
        }

        // exit + down + pointerDown + move + pointerUp + up + enter
        t += eventPeriodMillis
        events.removeFirst(7).let {
            (exitEvent, downEvent, pointerDownEvent, moveEvent, pointerUpEvent, upEvent, enterEvent)
            ->
            exitEvent.verifyTrackpadEvent(ACTION_HOVER_EXIT, t, position1, buttonState)

            t = 0L // down resets downTime

            downEvent.verifyTrackpadEvent(
                ACTION_DOWN,
                t,
                position1 + Offset(-100f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 1f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            pointerDownEvent.verifyTrackpadEvent(
                ACTION_POINTER_DOWN,
                t,
                position1 + Offset(-100f, 0f),
                buttonState,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
                expectedActionIndex = 1,
            )
            pointerDownEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-100f, 0f))
            pointerDownEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(100f, 0f))

            t += eventPeriodMillis

            moveEvent.verifyTrackpadEvent(
                ACTION_MOVE,
                t,
                position1 + Offset(-110f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 1.1f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
            )
            moveEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-110f, 0f))
            moveEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(110f, 0f))

            t += eventPeriodMillis

            pointerUpEvent.verifyTrackpadEvent(
                ACTION_POINTER_UP,
                t,
                position1 + Offset(-110f, 0f),
                buttonState,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 2,
                expectedActionIndex = 1,
            )
            pointerUpEvent.verifyTrackpadFakePointerEvent(0, position1 + Offset(-110f, 0f))
            pointerUpEvent.verifyTrackpadFakePointerEvent(1, position1 + Offset(110f, 0f))
            upEvent.verifyTrackpadEvent(
                ACTION_UP,
                t,
                position1 + Offset(-110f, 0f),
                buttonState,
                MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR to 1f,
                expectedClassification =
                    if (Build.VERSION.SDK_INT >= 34) MotionEvent.CLASSIFICATION_PINCH
                    else MotionEvent.CLASSIFICATION_NONE,
                expectedPointerCount = 1,
            )
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position1, buttonState)
        }
    }

    @Test
    fun twoButtons_cancel() {
        // Scenario:
        // press primary button
        // press secondary button
        // cancel

        var expectedEvents = 0
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 2 // down + press
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Secondary.buttonId)
        expectedEvents += 2 // move + press
        subject.advanceEventTime()
        subject.enqueueTrackpadCancel()
        expectedEvents += 1 // cancel
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down + press
        var t = 0L
        var buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // move + press
        t += eventPeriodMillis
        buttonState = BUTTON_PRIMARY or BUTTON_SECONDARY
        events.removeFirst(2).let { (moveEvent, pressEvent) ->
            moveEvent.verifyTrackpadEvent(ACTION_MOVE, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }

        // cancel
        t += eventPeriodMillis
        buttonState = 0
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTrackpadEvent(ACTION_CANCEL, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun enqueueTrackpadPress_interruptsTouch() {
        // Scenario:
        // finger 1 down
        // press primary button

        var expectedEvents = 0
        subject.enqueueTouchDown(1, position1)
        expectedEvents += 1 // down
        subject.advanceEventTime()
        subject.enqueueTrackpadPress(TrackpadButton.Primary.buttonId)
        expectedEvents += 3 // cancel + down + press
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down
        var t = 0L
        events.removeFirst(1).let { (downEvent) ->
            downEvent.verifyTouchEvent(1, ACTION_DOWN, 0, t)
            downEvent.verifyTouchPointer(1, position1)
        }

        // cancel
        t += eventPeriodMillis
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            cancelEvent.verifyTouchPointer(1, position1)
        }

        // down + press
        t = 0L // down resets downTime
        val buttonState = BUTTON_PRIMARY
        events.removeFirst(2).let { (downEvent, pressEvent) ->
            downEvent.verifyTrackpadEvent(ACTION_DOWN, t, Offset.Zero, buttonState)
            pressEvent.verifyTrackpadEvent(ACTION_BUTTON_PRESS, t, Offset.Zero, buttonState)
        }
    }

    @Test
    fun enqueueTrackpadMove_interruptsTouch() {
        // Scenario:
        // finger 1 down
        // move trackpad

        var expectedEvents = 0
        subject.enqueueTouchDown(1, position1)
        expectedEvents += 1 // down
        subject.advanceEventTime()
        subject.enqueueTrackpadMove(position2)
        expectedEvents += 3 // cancel + enter + hover
        subject.flush()

        recorder.assertHasValidEventTimes()
        assertThat(recorder.events).hasSize(expectedEvents)
        val events = recorder.events.toMutableList()

        // down
        var t = 0L
        events.removeFirst(1).let { (downEvent) ->
            downEvent.verifyTouchEvent(1, ACTION_DOWN, 0, t)
            downEvent.verifyTouchPointer(1, position1)
        }

        // cancel
        t += eventPeriodMillis
        events.removeFirst(1).let { (cancelEvent) ->
            cancelEvent.verifyTouchEvent(1, ACTION_CANCEL, 0, t)
            cancelEvent.verifyTouchPointer(1, position1)
        }

        // enter + hover
        events.removeFirst(2).let { (enterEvent, hoverEvent) ->
            enterEvent.verifyTrackpadEvent(ACTION_HOVER_ENTER, t, position2, 0)
            hoverEvent.verifyTrackpadEvent(ACTION_HOVER_MOVE, t, position2, 0)
        }
    }

    @Test
    fun enqueueTrackpadDown_alreadyDown() {
        subject.enqueueTrackpadPress(1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad button down event, button 1 is already pressed"
        ) {
            subject.enqueueTrackpadPress(1)
        }
    }

    @Test
    fun enqueueTrackpadDown_outOfBounds() {
        subject.updateTrackpadPosition(positionMin1)
        expectError<IllegalStateException>(
            expectedMessage =
                "Cannot start a trackpad gesture outside the Compose root bounds, " +
                    "trackpad position is .* and bounds are .*"
        ) {
            subject.enqueueTrackpadPress(1)
        }
    }

    @Test
    fun enqueueTrackpadUp_withoutDown() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad button up event, button 1 is not pressed"
        ) {
            subject.enqueueTrackpadRelease(1)
        }
    }

    @Test
    fun enqueueTrackpadEnter_alreadyEntered() {
        subject.enqueueTrackpadEnter(position1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad hover enter event, trackpad is already hovering"
        ) {
            subject.enqueueTrackpadEnter(position1)
        }
    }

    @Test
    fun enqueueTrackpadEnter_buttonsDown() {
        subject.enqueueTrackpadPress(1)
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad hover enter event, trackpad buttons are down"
        ) {
            subject.enqueueTrackpadEnter(position1)
        }
    }

    @Test
    fun enqueueTrackpadEnter_outOfBounds() {
        expectError<IllegalStateException>(
            expectedMessage =
                "Cannot send trackpad hover enter event, " +
                    "Offset\\(-1\\.0, -1\\.0\\) is out of bounds"
        ) {
            subject.enqueueTrackpadEnter(positionMin1)
        }
    }

    @Test
    fun enqueueTrackpadExit_notEntered() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad hover exit event, trackpad is not hovering"
        ) {
            subject.enqueueTrackpadExit(position1)
        }
    }

    @Test
    fun enqueueTrackpadCancel_withoutDown() {
        expectError<IllegalStateException>(
            expectedMessage = "Cannot send trackpad cancel event, no trackpad buttons are pressed"
        ) {
            subject.enqueueTrackpadCancel()
        }
    }

    private fun AndroidInputDispatcher.verifyTrackpadPosition(expectedPosition: Offset) {
        assertWithMessage("currentTrackpadPosition")
            .that(currentCursorPosition)
            .isEqualTo(expectedPosition)
    }

    private fun <E> MutableList<E>.removeFirst(n: Int): List<E> {
        return mutableListOf<E>().also { result -> repeat(n) { result.add(removeFirstKt()) } }
    }
}

private operator fun <E> List<E>.component6() = get(5)

private operator fun <E> List<E>.component7() = get(6)

private operator fun <E> List<E>.component8() = get(7)
