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

@file:OptIn(ExperimentalIndirectPointerApi::class)

package androidx.compose.foundation

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Synthetically range the x movements from 1000 to 0 */
internal fun SemanticsNodeInteraction.sendIndirectSwipeEvent(
    rule: ComposeTestRule,
    from: Offset = Offset(TouchPadStart, 0f),
    to: Offset = Offset(TouchPadEnd, 0f),
    stepCount: Int = 10,
    delayTimeMills: Long = 16L,
    primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis =
        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
    sendMoveEvents: Boolean = true,
    sendReleaseEvent: Boolean = true,
) {
    require(stepCount > 0) { "Step count should be at least 1" }
    val stepSize = (to - from) / stepCount.toFloat()

    var currentTime = SystemClock.uptimeMillis()
    var currentValue = from

    val downEvent =
        sendIndirectPointerPressEvent(rule, currentTime, currentValue, primaryDirectionalMotionAxis)
    currentTime += delayTimeMills
    currentValue += stepSize

    val (newCurrentTime, newCurrentValue, lastMove) =
        if (sendMoveEvents) {
            sendIndirectPointerMoveEvents(
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
        sendIndirectPointerReleaseEvent(
            rule,
            newCurrentTime,
            newCurrentValue,
            primaryDirectionalMotionAxis,
            lastMove,
        )
    }
}

internal fun SemanticsNodeInteraction.sendIndirectPointerMoveEvents(
    rule: ComposeTestRule,
    stepCount: Int,
    currentTime: Long,
    currentValue: Offset,
    delayTimeMills: Long,
    stepSize: Offset,
    primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
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
        performIndirectPointerEvent(
            rule,
            IndirectPointerEvent(move, primaryDirectionalMotionAxis, prevEvent),
        )
        prevEvent = move
    }
    return Triple(currentTime1, currentValue1, prevEvent)
}

internal fun SemanticsNodeInteraction.sendIndirectPointerReleaseEvent(
    rule: ComposeTestRule,
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryAxis: IndirectPointerEventPrimaryDirectionalMotionAxis =
        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
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
    performIndirectPointerEvent(rule, IndirectPointerEvent(up, primaryAxis, previousEvent))
}

internal fun SemanticsNodeInteraction.sendIndirectPointerPressEvent(
    rule: ComposeTestRule,
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis =
        IndirectPointerEventPrimaryDirectionalMotionAxis.X,
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
    performIndirectPointerEvent(rule, IndirectPointerEvent(down, primaryDirectionalMotionAxis))
    return down
}

/** Swiping away from the start of the touchpad. */
internal fun SemanticsNodeInteraction.sendIndirectSwipeBackward(rule: ComposeTestRule) {
    sendIndirectSwipeEvent(rule, Offset(TouchPadEnd, 0f), Offset(TouchPadStart, 0f))
}

/** Swiping towards the start of the touchpad. */
internal fun SemanticsNodeInteraction.sendIndirectSwipeForward(rule: ComposeTestRule) {
    sendIndirectSwipeEvent(rule, Offset(TouchPadStart, 0f), Offset(TouchPadEnd, 0f))
}

internal fun SemanticsNodeInteraction.sendIndirectPointerCancelEvent(
    rule: ComposeTestRule,
    sendMoveEvents: Boolean = true,
) {
    val stepSize = Offset((TouchPadEnd - TouchPadStart) / 5, 0f)
    var currentTime = SystemClock.uptimeMillis()
    var currentValue = Offset(TouchPadStart, 0f)

    val downEvent = sendIndirectPointerPressEvent(rule, currentTime, currentValue)
    currentTime += 16L
    currentValue += stepSize

    val prevEvent =
        if (sendMoveEvents) {
            sendIndirectPointerMoveEvents(
                    rule,
                    5,
                    currentTime,
                    currentValue,
                    16L,
                    stepSize,
                    IndirectPointerEventPrimaryDirectionalMotionAxis.X,
                    downEvent,
                )
                .third
        } else {
            downEvent
        }

    val cancel =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, Offset.Zero.y, Offset.Zero.y, 0)
    performIndirectPointerEvent(rule, IndirectPointerEvent(cancel, previousMotionEvent = prevEvent))
}

internal fun SemanticsNodeInteraction.sendIndirectPressReleaseEvent(
    rule: ComposeTestRule,
    time: Long = SystemClock.uptimeMillis(),
) {
    val currentValue = Offset((TouchPadEnd - TouchPadStart) / 2, 0f)
    val downEvent = sendIndirectPointerPressEvent(rule, time, currentValue)
    val (newCurrentTime, newCurrentValue, lastMove) =
        sendIndirectPointerMoveEvents(
            rule,
            1,
            time,
            currentValue,
            16L,
            Offset.Zero,
            IndirectPointerEventPrimaryDirectionalMotionAxis.X,
            downEvent,
        )
    sendIndirectPointerReleaseEvent(rule, newCurrentTime, newCurrentValue, previousEvent = lastMove)
}

/**
 * Send the specified [IndirectPointerEvent] to the focused component.
 *
 * @return true if the event was consumed. False otherwise.
 */
internal fun SemanticsNodeInteraction.performIndirectPointerEvent(
    rule: ComposeTestRule,
    indirectPointerEvent: IndirectPointerEvent,
): Boolean {
    val semanticsNode =
        fetchSemanticsNode("Failed to send indirect pointer event ($indirectPointerEvent)")
    val root = semanticsNode.root
    requireNotNull(root) { "Failed to find owner" }
    return rule.runOnUiThread { root.sendIndirectPointerEvent(indirectPointerEvent) }
}

internal const val TouchPadEnd = 1000f
internal const val TouchPadStart = 0f
