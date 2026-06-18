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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerEventType
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.node.RootForTest
import androidx.core.view.InputDeviceCompat.SOURCE_TOUCH_NAVIGATION

/** Send the indirect swipe consisting of three events: DOWN, MOVE, and UP. */
@MainThread
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
internal fun RootForTest.sendIndirectSwipeEvents(
    from: Offset,
    to: Offset,
    axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    delayTimeMillis: Long = 16L,
) {
    var currentTime = SystemClock.uptimeMillis()
    val downTime = currentTime

    // Down event
    val downChange =
        IndirectPointerInputChange(
            id = PointerId(0L),
            uptimeMillis = currentTime,
            position = from,
            pressed = true,
            pressure = 1.0f,
            previousUptimeMillis = currentTime,
            previousPosition = from,
            previousPressed = false,
        )
    val downEvent =
        IndirectPointerEvent(
            changes = listOf(downChange),
            type = IndirectPointerEventType.Press,
            primaryDirectionalMotionAxis = axis,
            motionEvent =
                obtainIndirectMotionEvent(downTime, currentTime, MotionEvent.ACTION_DOWN, from),
        )
    sendIndirectPointerEvent(downEvent)

    currentTime += delayTimeMillis

    // Move event
    val moveChange =
        IndirectPointerInputChange(
            id = PointerId(0L),
            uptimeMillis = currentTime,
            position = to,
            pressed = true,
            pressure = 1.0f,
            previousUptimeMillis = currentTime - delayTimeMillis,
            previousPosition = from,
            previousPressed = true,
        )
    val moveEvent =
        IndirectPointerEvent(
            changes = listOf(moveChange),
            type = IndirectPointerEventType.Move,
            primaryDirectionalMotionAxis = axis,
            motionEvent =
                obtainIndirectMotionEvent(downTime, currentTime, MotionEvent.ACTION_MOVE, to),
        )
    sendIndirectPointerEvent(moveEvent)

    currentTime += delayTimeMillis

    // Up event
    val upChange =
        IndirectPointerInputChange(
            id = PointerId(0L),
            uptimeMillis = currentTime,
            position = to,
            pressed = false,
            pressure = 1.0f,
            previousUptimeMillis = currentTime - delayTimeMillis,
            previousPosition = to,
            previousPressed = true,
        )
    val upEvent =
        IndirectPointerEvent(
            changes = listOf(upChange),
            type = IndirectPointerEventType.Release,
            primaryDirectionalMotionAxis = axis,
            motionEvent =
                obtainIndirectMotionEvent(downTime, currentTime, MotionEvent.ACTION_UP, to),
        )
    sendIndirectPointerEvent(upEvent)
}

private fun obtainIndirectMotionEvent(
    downTime: Long,
    eventTime: Long,
    action: Int,
    coordinates: Offset,
): MotionEvent {
    return MotionEvent.obtain(
            /* downTime = */ downTime,
            /* eventTime = */ eventTime,
            /* action = */ action,
            /* x = */ coordinates.x,
            /* y = */ coordinates.y,
            /* metaState = */ 0,
        )
        .apply { source = SOURCE_TOUCH_NAVIGATION }
}
