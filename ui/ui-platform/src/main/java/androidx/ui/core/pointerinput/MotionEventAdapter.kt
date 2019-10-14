/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.pointerinput

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.ui.core.PointerInputData
import androidx.ui.core.PxPosition
import androidx.ui.core.Timestamp
import androidx.ui.core.millisecondsToTimestamp
import androidx.ui.core.px

/**
 * Converts Android framework [MotionEvent]s into [PointerInputEvent]s.
 */
internal fun MotionEvent.toPointerInputEvent(): PointerInputEvent {

    val upIndex = when (this.actionMasked) {
        ACTION_POINTER_UP -> this.actionIndex
        ACTION_UP -> 0
        else -> null
    }

    val pointers: MutableList<PointerInputEventData> = mutableListOf()
    for (i in 0 until this.pointerCount) {
        pointers.add(
            PointerInputEventData(this, i, upIndex)
        )
    }

    return PointerInputEvent(eventTime.millisecondsToTimestamp(), pointers)
}

/**
 * Creates a new Pointer.
 */
private fun PointerInputEventData(
    motionEvent: MotionEvent,
    index: Int,
    upIndex: Int?
): PointerInputEventData {
    return PointerInputEventData(
        motionEvent.getPointerId(index),
        PointerInputData(
            motionEvent.eventTime.millisecondsToTimestamp(),
            motionEvent,
            index,
            upIndex
        )
    )
}

/**
 * Creates a new PointerData.
 */
private fun PointerInputData(
    timestamp: Timestamp,
    motionEvent: MotionEvent,
    index: Int,
    upIndex: Int?
): PointerInputData {
    val pointerCoords = MotionEvent.PointerCoords()
    motionEvent.getPointerCoords(index, pointerCoords)
    val offset = PxPosition(pointerCoords.x.px, pointerCoords.y.px)

    return PointerInputData(
        timestamp,
        offset,
        index != upIndex
    )
}