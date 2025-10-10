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

package androidx.compose.foundation

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectTouchEvent
import androidx.compose.ui.input.indirect.IndirectTouchEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Synthetically range the x movements from 1000 to 0 */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectSwipeEvent(
    rule: ComposeTestRule,
    from: Offset = Offset(TouchPadStart, 0f),
    to: Offset = Offset(TouchPadEnd, 0f),
    stepCount: Int = 10,
    delayTimeMills: Long = 16L,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
    sendMoveEvents: Boolean = true,
    sendReleaseEvent: Boolean = true,
) {
    require(stepCount > 0) { "Step count should be at least 1" }
    val stepSize = (to - from) / stepCount.toFloat()

    var currentTime = SystemClock.uptimeMillis()
    var currentValue = from

    val downEvent =
        sendIndirectTouchPressEvent(rule, currentTime, currentValue, primaryDirectionalMotionAxis)
    currentTime += delayTimeMills
    currentValue += stepSize

    val (newCurrentTime, newCurrentValue, lastMove) =
        if (sendMoveEvents) {
            sendIndirectTouchMoveEvents(
                rule,
                stepCount,
                currentTime,
                currentValue,
                delayTimeMills,
                stepSize,
                primaryDirectionalMotionAxis,
                downEvent,
            )
        } else {
            Triple(currentTime, currentValue, downEvent)
        }

    if (sendReleaseEvent) {
        sendIndirectTouchReleaseEvent(
            rule,
            newCurrentTime,
            newCurrentValue,
            primaryDirectionalMotionAxis,
            lastMove,
        )
    }
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectTouchMoveEvents(
    rule: ComposeTestRule,
    stepCount: Int,
    currentTime: Long,
    currentValue: Offset,
    delayTimeMills: Long,
    stepSize: Offset,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis,
    previousEvent: MotionEvent? = null,
): Triple<Long, Offset, MotionEvent?> {
    var currentTime1 = currentTime
    var currentValue1 = currentValue
    var prevEvent: MotionEvent? = previousEvent
    repeat(stepCount) {
        val move =
            MotionEvent.obtain(
                currentTime1,
                currentTime1,
                MotionEvent.ACTION_MOVE,
                currentValue1.x,
                currentValue1.y,
                0,
            )
        move.source = SOURCE_TOUCH_NAVIGATION
        if (it != stepCount - 1) {
            currentTime1 += delayTimeMills
            currentValue1 += stepSize
        }
        performIndirectTouchEvent(
            rule,
            IndirectTouchEvent(move, primaryDirectionalMotionAxis, prevEvent),
        )
        prevEvent = move
    }
    return Triple(currentTime1, currentValue1, prevEvent)
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectTouchReleaseEvent(
    rule: ComposeTestRule,
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
    previousEvent: MotionEvent? = null,
) {
    val up =
        MotionEvent.obtain(
            currentTime,
            currentTime,
            MotionEvent.ACTION_UP,
            currentValue.x,
            currentValue.y,
            0,
        )
    up.source = SOURCE_TOUCH_NAVIGATION
    performIndirectTouchEvent(rule, IndirectTouchEvent(up, primaryAxis, previousEvent))
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectTouchPressEvent(
    rule: ComposeTestRule,
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
): MotionEvent {
    val down =
        MotionEvent.obtain(
            currentTime, // downTime,
            currentTime, // eventTime,
            MotionEvent.ACTION_DOWN,
            currentValue.x,
            currentValue.y,
            0,
        )
    down.source = SOURCE_TOUCH_NAVIGATION
    performIndirectTouchEvent(rule, IndirectTouchEvent(down, primaryDirectionalMotionAxis))
    return down
}

/** Swiping away from the start of the touchpad. */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectSwipeBackward(rule: ComposeTestRule) {
    sendIndirectSwipeEvent(rule, Offset(TouchPadEnd, 0f), Offset(TouchPadStart, 0f))
}

/** Swiping towards the start of the touchpad. */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectSwipeForward(rule: ComposeTestRule) {
    sendIndirectSwipeEvent(rule, Offset(TouchPadStart, 0f), Offset(TouchPadEnd, 0f))
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectTouchCancelEvent(
    rule: ComposeTestRule,
    sendMoveEvents: Boolean = true,
) {
    val stepSize = Offset((TouchPadEnd - TouchPadStart) / 5, 0f)
    var currentTime = SystemClock.uptimeMillis()
    var currentValue = Offset(TouchPadStart, 0f)

    val downEvent = sendIndirectTouchPressEvent(rule, currentTime, currentValue)
    currentTime += 16L
    currentValue += stepSize

    val prevEvent =
        if (sendMoveEvents) {
            sendIndirectTouchMoveEvents(
                    rule,
                    5,
                    currentTime,
                    currentValue,
                    16L,
                    stepSize,
                    IndirectTouchEventPrimaryDirectionalMotionAxis.X,
                    downEvent,
                )
                .third
        } else {
            downEvent
        }

    val cancel =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, Offset.Zero.y, Offset.Zero.y, 0)
    performIndirectTouchEvent(rule, IndirectTouchEvent(cancel, previousMotionEvent = prevEvent))
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectPressReleaseEvent(rule: ComposeTestRule) {
    val currentTime = SystemClock.uptimeMillis()
    val currentValue = Offset((TouchPadEnd - TouchPadStart) / 2, 0f)
    val downEvent = sendIndirectTouchPressEvent(rule, currentTime, currentValue)
    val (newCurrentTime, newCurrentValue, lastMove) =
        sendIndirectTouchMoveEvents(
            rule,
            1,
            currentTime,
            currentValue,
            16L,
            Offset.Zero,
            IndirectTouchEventPrimaryDirectionalMotionAxis.X,
            downEvent,
        )
    sendIndirectTouchReleaseEvent(rule, newCurrentTime, newCurrentValue, previousEvent = lastMove)
}

/**
 * Send the specified [IndirectTouchEvent] to the focused component.
 *
 * @return true if the event was consumed. False otherwise.
 */
@ExperimentalIndirectTouchTypeApi
internal fun SemanticsNodeInteraction.performIndirectTouchEvent(
    rule: ComposeTestRule,
    indirectTouchEvent: IndirectTouchEvent,
): Boolean {
    val semanticsNode =
        fetchSemanticsNode("Failed to send indirect touch event ($indirectTouchEvent)")
    val root = semanticsNode.root
    requireNotNull(root) { "Failed to find owner" }
    return rule.runOnUiThread { root.sendIndirectTouchEvent(indirectTouchEvent) }
}

internal const val TouchPadEnd = 1000f
internal const val TouchPadStart = 0f
