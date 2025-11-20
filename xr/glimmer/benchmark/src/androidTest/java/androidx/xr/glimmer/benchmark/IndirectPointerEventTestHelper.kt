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

package androidx.xr.glimmer.benchmark

import android.os.SystemClock
import android.view.MotionEvent
import androidx.annotation.MainThread
import androidx.compose.ui.ExperimentalIndirectPointerApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.node.RootForTest
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Send the indirect swipe consisting of three events: DOWN, MOVE, and UP. */
@MainThread
@OptIn(ExperimentalIndirectPointerApi::class)
internal fun RootForTest.sendIndirectSwipe(
    distance: Float,
    primaryAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
) {
    val dstOffset =
        when (primaryAxis) {
            IndirectPointerEventPrimaryDirectionalMotionAxis.X -> Offset(x = distance, y = 0f)
            IndirectPointerEventPrimaryDirectionalMotionAxis.Y -> Offset(x = 0f, y = distance)
            else -> Offset(x = distance, y = distance)
        }
    sendIndirectSwipeEvents(from = Offset.Zero, to = dstOffset, axis = primaryAxis)
}

@MainThread
@OptIn(ExperimentalIndirectPointerApi::class)
internal fun RootForTest.sendIndirectSwipeEvents(
    from: Offset,
    to: Offset,
    axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    delayTimeMills: Long = 16L,
) {
    var currentTime = SystemClock.uptimeMillis()
    val downEvent = sendIndirectPointerPressEvent(time = currentTime, value = from, axis = axis)
    currentTime += delayTimeMills
    val move =
        sendIndirectPointerMoveEvents(
            time = currentTime,
            value = to,
            axis = axis,
            previousEvent = downEvent,
        )
    currentTime += delayTimeMills
    sendIndirectPointerReleaseEvent(
        time = currentTime,
        value = to,
        axis = axis,
        previousEvent = move,
    )
}

@MainThread
@OptIn(ExperimentalIndirectPointerApi::class)
internal fun RootForTest.sendIndirectPointerMoveEvents(
    time: Long,
    value: Offset,
    axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    previousEvent: MotionEvent?,
): MotionEvent {
    val move = obtainIndirectMotionEvent(time, MotionEvent.ACTION_MOVE, value)
    sendIndirectPointerEvent(IndirectPointerEvent(move, axis, previousEvent))
    return move
}

@MainThread
@OptIn(ExperimentalIndirectPointerApi::class)
internal fun RootForTest.sendIndirectPointerReleaseEvent(
    time: Long,
    value: Offset,
    axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    previousEvent: MotionEvent? = null,
) {
    val up = obtainIndirectMotionEvent(time, MotionEvent.ACTION_UP, value)
    sendIndirectPointerEvent(IndirectPointerEvent(up, axis, previousEvent))
}

@MainThread
@OptIn(ExperimentalIndirectPointerApi::class)
internal fun RootForTest.sendIndirectPointerPressEvent(
    time: Long,
    value: Offset,
    axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
): MotionEvent {
    val down = obtainIndirectMotionEvent(time, MotionEvent.ACTION_DOWN, value)
    sendIndirectPointerEvent(IndirectPointerEvent(down, axis))
    return down
}

private fun obtainIndirectMotionEvent(time: Long, action: Int, coordinates: Offset): MotionEvent {
    return MotionEvent.obtain(
            /* downTime = */ time,
            /* eventTime = */ time,
            /* action = */ action,
            /* x = */ coordinates.x,
            /* y = */ coordinates.y,
            /* metaState = */ 0,
        )
        .apply { source = SOURCE_TOUCH_NAVIGATION }
}
