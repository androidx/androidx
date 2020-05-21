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

package androidx.ui.node

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.ui.core.PointerInputChange
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.unit.Duration
import androidx.ui.unit.NanosecondsPerMillisecond
import androidx.ui.unit.milliseconds

// TODO(shepshapard): Refactor in order to remove the need for current.uptime!!
internal fun List<PointerInputChange>.toMotionEventScope(block: (MotionEvent) -> Unit) {
    check(isNotEmpty())

    val eventTime =
        (first().current.uptime!!.nanoseconds / NanosecondsPerMillisecond)
    val numPointers = size

    val (action, actionIndex) =
        if (all { it.changedToDownIgnoreConsumed() }) {
            MotionEvent.ACTION_DOWN to 0
        } else if (all { it.changedToUpIgnoreConsumed() }) {
            MotionEvent.ACTION_UP to 0
        } else {
            val downIndex = indexOfFirst { it.changedToDownIgnoreConsumed() }
            if (downIndex != -1) {
                MotionEvent.ACTION_POINTER_DOWN to downIndex
            } else {
                val upIndex = indexOfFirst { it.changedToUpIgnoreConsumed() }
                if (upIndex != -1) {
                    MotionEvent.ACTION_POINTER_UP to upIndex
                } else {
                    MotionEvent.ACTION_MOVE to 0
                }
            }
        }

    // TODO(b/154136736): "(it.id.value % 32).toInt()" is very fishy.
    val pointerProperties =
        map { PointerProperties((it.id.value % 32).toInt()) }
            .toTypedArray()
    val pointerCoords =
        map {
            if (it.changedToUpIgnoreConsumed()) {
                PointerCoords(it.previous.position!!.x, it.previous.position.y)
            } else {
                PointerCoords(it.current.position!!.x, it.current.position.y)
            }
        }.toTypedArray()

    // TODO(b/154136736): Downtime as 0 isn't right.  Not sure it matters.
    val motionEvent =
        MotionEvent(
            eventTime,
            action,
            numPointers,
            actionIndex,
            pointerProperties,
            pointerCoords
        )
    block(motionEvent)
    motionEvent.recycle()
}

// TODO(shepshapard): Refactor in order to remove the need for current.uptime!!
internal fun List<PointerInputChange>.toCancelMotionEventScope(block: (MotionEvent) -> Unit) {
    check(isNotEmpty())

    val eventTime =
        (first().current.uptime!!.nanoseconds / NanosecondsPerMillisecond)
    val numPointers = size

    // TODO(shepshapard): toInt() is clearly not right.
    val pointerProperties =
        map { PointerProperties(it.id.value.toInt()) }
            .toTypedArray()
    val pointerCoords =
        map {
            if (it.changedToUpIgnoreConsumed()) {
                PointerCoords(it.previous.position!!.x, it.previous.position.y)
            } else {
                PointerCoords(it.current.position!!.x, it.current.position.y)
            }
        }.toTypedArray()

    // TODO(b/154136736): Downtime as 0 isn't right.  Not sure it matters.
    val motionEvent =
        MotionEvent(
            eventTime,
            MotionEvent.ACTION_CANCEL,
            numPointers,
            0,
            pointerProperties,
            pointerCoords
        )
    block(motionEvent)
    motionEvent.recycle()
}

internal fun emptyCancelMotionEventScope(
    now: Duration = SystemClock.uptimeMillis().milliseconds,
    block: (MotionEvent) -> Unit
) {
    // Mimics what ViewGroup does when it needs to send a minimal ACTION_CANCEL event.
    val nowMillis = now.nanoseconds / NanosecondsPerMillisecond
    val motionEvent = MotionEvent.obtain(nowMillis, nowMillis,
        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0)
    motionEvent.source = InputDevice.SOURCE_TOUCHSCREEN
    block(motionEvent)
    motionEvent.recycle()
}

private fun PointerProperties(id: Int = 0, toolType: Int = MotionEvent.TOOL_TYPE_UNKNOWN) =
    MotionEvent.PointerProperties().apply {
        this.id = id
        this.toolType = toolType
    }

private fun PointerCoords(x: Float = 0f, y: Float = 0f) =
    MotionEvent.PointerCoords().apply {
        this.x = x
        this.y = y
    }

private fun MotionEvent(
    eventTime: Long,
    action: Int,
    numPointers: Int,
    actionIndex: Int,
    pointerProperties: Array<MotionEvent.PointerProperties>,
    pointerCoords: Array<MotionEvent.PointerCoords>
) = MotionEvent.obtain(
    0,
    eventTime,
    action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
    numPointers,
    pointerProperties,
    pointerCoords,
    0,
    0,
    0f,
    0f,
    0,
    0,
    0,
    0
)