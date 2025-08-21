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
import androidx.compose.ui.test.performIndirectTouchEvent
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Synthetically range the x movements from 1000 to 0 */
@ExperimentalIndirectTouchTypeApi
internal fun SemanticsNodeInteraction.sendIndirectSwipeEvent(
    from: Offset = Offset(TouchPadStart, 0f),
    to: Offset = Offset(TouchPadEnd, 0f),
    stepCount: Int = 10,
    delayTimeMills: Long = 16L,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
) {
    require(stepCount > 0) { "Step count should be at least 1" }
    val stepSize = (to - from) / stepCount.toFloat()

    var currentTime = SystemClock.uptimeMillis()
    var currentValue = from

    sendIndirectTouchPressEvent(currentTime, currentValue, primaryDirectionalMotionAxis)
    currentTime += delayTimeMills
    currentValue += stepSize

    val (newCurrentTime, newCurrentValue) =
        sendIndirectTouchMoveEvents(
            stepCount,
            currentTime,
            currentValue,
            delayTimeMills,
            stepSize,
            primaryDirectionalMotionAxis,
        )

    sendIndirectTouchReleaseEvent(newCurrentTime, newCurrentValue, primaryDirectionalMotionAxis)
}

@ExperimentalIndirectTouchTypeApi
internal fun SemanticsNodeInteraction.sendIndirectTouchMoveEvents(
    stepCount: Int,
    currentTime: Long,
    currentValue: Offset,
    delayTimeMills: Long,
    stepSize: Offset,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis,
): Pair<Long, Offset> {
    var currentTime1 = currentTime
    var currentValue1 = currentValue
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
        performIndirectTouchEvent(IndirectTouchEvent(move, primaryDirectionalMotionAxis))
    }
    return Pair(currentTime1, currentValue1)
}

@ExperimentalIndirectTouchTypeApi
internal fun SemanticsNodeInteraction.sendIndirectTouchReleaseEvent(
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
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
    performIndirectTouchEvent(IndirectTouchEvent(up, primaryAxis))
}

@ExperimentalIndirectTouchTypeApi
internal fun SemanticsNodeInteraction.sendIndirectTouchPressEvent(
    currentTime: Long = SystemClock.uptimeMillis(),
    currentValue: Offset = Offset((TouchPadEnd - TouchPadStart) / 2f, 0f),
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.X,
) {
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
    performIndirectTouchEvent(IndirectTouchEvent(down, primaryDirectionalMotionAxis))
}

/** Swiping away from the start of the touchpad. */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectSwipeBackward() {
    sendIndirectSwipeEvent(Offset(TouchPadEnd, 0f), Offset(TouchPadStart, 0f))
}

/** Swiping towards the start of the touchpad. */
@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectSwipeForward() {
    sendIndirectSwipeEvent(Offset(TouchPadStart, 0f), Offset(TouchPadEnd, 0f))
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectTouchCancelEvent(sendMoveEvents: Boolean = true) {
    val stepSize = Offset((TouchPadEnd - TouchPadStart) / 5, 0f)
    var currentTime = SystemClock.uptimeMillis()
    var currentValue = Offset(TouchPadStart, 0f)

    sendIndirectTouchPressEvent(currentTime, currentValue)
    currentTime += 16L
    currentValue += stepSize

    if (sendMoveEvents) {
        sendIndirectTouchMoveEvents(
            5,
            currentTime,
            currentValue,
            16L,
            stepSize,
            IndirectTouchEventPrimaryDirectionalMotionAxis.X,
        )
    }

    val cancel =
        MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_CANCEL, Offset.Zero.y, Offset.Zero.y, 0)
    performIndirectTouchEvent(IndirectTouchEvent(cancel))
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun SemanticsNodeInteraction.sendIndirectPressReleaseEvent() {
    val currentTime = SystemClock.uptimeMillis()
    val currentValue = Offset((TouchPadEnd - TouchPadStart) / 2, 0f)
    sendIndirectTouchPressEvent(currentTime, currentValue)
    val (newCurrentTime, newCurrentValue) =
        sendIndirectTouchMoveEvents(
            1,
            currentTime,
            currentValue,
            16L,
            Offset.Zero,
            IndirectTouchEventPrimaryDirectionalMotionAxis.X,
        )
    sendIndirectTouchReleaseEvent(newCurrentTime, newCurrentValue)
}

internal const val TouchPadEnd = 1000f
internal const val TouchPadStart = 0f
