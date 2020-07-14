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
import androidx.ui.unit.IntOffset
import androidx.ui.unit.NanosecondsPerMillisecond
import androidx.ui.unit.milliseconds

// TODO(shepshapard): Refactor in order to remove the need for current.uptime!!
/**
 * Converts to a [MotionEvent], runs [block] with it, and recycles the [MotionEvent].
 *
 * @param regionToGlobalOffset The global offset of the region where this [MotionEvent] is being
 * used.  This will be added to each [PointerInputChange]'s position in order set raw coordinates
 * correctly, and will be subtracted via [MotionEvent.offsetLocation] so the [MotionEvent]
 * is still offset to be relative to the region.
 * @param block The block to be executed with the created [MotionEvent].
 */
internal fun List<PointerInputChange>.toMotionEventScope(
    regionToGlobalOffset: IntOffset,
    block: (MotionEvent) -> Unit
) {
    toMotionEventScope(regionToGlobalOffset, block, false)
}

/**
 * Converts to an [MotionEvent.ACTION_CANCEL] [MotionEvent], runs [block] with it, and recycles the
 * [MotionEvent].
 *
 * @param regionToGlobalOffset The global offset of the region where this [MotionEvent] is being
 * used.  This will be added to each [PointerInputChange]'s position in order set raw coordinates
 * correctly, and will be subtracted via [MotionEvent.offsetLocation] so the [MotionEvent]
 * is still offset to be relative to the region.
 * @param block The block to be executed with the created [MotionEvent].
 */
// TODO(shepshapard): Refactor in order to remove the need for current.uptime!!
internal fun List<PointerInputChange>.toCancelMotionEventScope(
    regionToGlobalOffset: IntOffset,
    block: (MotionEvent) -> Unit
) {
    toMotionEventScope(regionToGlobalOffset, block, true)
}

internal fun emptyCancelMotionEventScope(
    now: Duration = SystemClock.uptimeMillis().milliseconds,
    block: (MotionEvent) -> Unit
) {
    // Does what ViewGroup does when it needs to send a minimal ACTION_CANCEL event.
    val nowMillis = now.nanoseconds / NanosecondsPerMillisecond
    val motionEvent =
        MotionEvent.obtain(nowMillis, nowMillis, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0)
    motionEvent.source = InputDevice.SOURCE_TOUCHSCREEN
    block(motionEvent)
    motionEvent.recycle()
}

private fun List<PointerInputChange>.toMotionEventScope(
    regionToGlobalOffset: IntOffset,
    block: (MotionEvent) -> Unit,
    cancel: Boolean
) {
    // We need to make sure this is not empty
    check(isNotEmpty())

    // We derive the values of each aspect of MotionEvent...

    val eventTime =
        first().current.uptime!!.nanoseconds / NanosecondsPerMillisecond

    val action = if (cancel) {
        MotionEvent.ACTION_CANCEL
    } else {
        if (all { it.changedToDownIgnoreConsumed() }) {
            createAction(MotionEvent.ACTION_DOWN, 0)
        } else if (all { it.changedToUpIgnoreConsumed() }) {
            createAction(MotionEvent.ACTION_UP, 0)
        } else {
            val downIndex = indexOfFirst { it.changedToDownIgnoreConsumed() }
            if (downIndex != -1) {
                createAction(MotionEvent.ACTION_POINTER_DOWN, downIndex)
            } else {
                val upIndex = indexOfFirst { it.changedToUpIgnoreConsumed() }
                if (upIndex != -1) {
                    createAction(MotionEvent.ACTION_POINTER_UP, upIndex)
                } else {
                    createAction(MotionEvent.ACTION_MOVE, 0)
                }
            }
        }
    }

    val numPointers = size

    // TODO(b/154136736): "(it.id.value % 32).toInt()" is very fishy, but android never expects
    //  the id to be larger than 31.
    val pointerProperties =
        map {
            MotionEvent.PointerProperties().apply {
                id = (it.id.value % 32).toInt()
                toolType = MotionEvent.TOOL_TYPE_UNKNOWN
            }
        }.toTypedArray()

    val pointerCoords =
        map {
            val offsetX =
                if (it.changedToUpIgnoreConsumed()) {
                    it.previous.position!!.x
                } else {
                    it.current.position!!.x
                }
            val offsetY =
                if (it.changedToUpIgnoreConsumed()) {
                    it.previous.position!!.y
                } else {
                    it.current.position!!.y
                }
            // We add the regionToGlobalOffset so that the raw coordinates are correct.
            val rawX = offsetX + regionToGlobalOffset.x
            val rawY = offsetY + regionToGlobalOffset.y

            MotionEvent.PointerCoords().apply {
                this.x = rawX
                this.y = rawY
            }
        }.toTypedArray()

    // ... Then we create the MotionEvent, dispatch it to block, and recycle it.

    // TODO(b/154136736): Downtime as 0 isn't right.  Not sure it matters.
    MotionEvent.obtain(
        0,
        eventTime,
        action,
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
    ).apply {
        // We subtract the regionToGlobalOffset so the local coordinates are correct.
        offsetLocation(
            -regionToGlobalOffset.x.toFloat(),
            -regionToGlobalOffset.y.toFloat()
        )
        block(this)
        recycle()
    }
}

private fun createAction(actionType: Int, actionIndex: Int) =
    actionType + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
