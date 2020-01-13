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
import androidx.ui.unit.NanosecondsPerMillisecond
import androidx.ui.unit.PxPosition
import androidx.ui.unit.Uptime
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px

/**
 * Converts an Android framework [MotionEvent] into a [PointerInputEvent].
 *
 * The resulting [PointerInputEvent] has coordinates that are relative to the screen (the
 * [MotionEvent]s raw coordinates are used as provided by [MotionEvent.getRawX] and
 * [MotionEvent.getRawY].
 */
internal fun MotionEvent.toPointerInputEvent(): PointerInputEvent {
    val upIndex = when (this.actionMasked) {
        ACTION_POINTER_UP -> this.actionIndex
        ACTION_UP -> 0
        else -> null
    }

    val pointers: MutableList<PointerInputEventData> = mutableListOf()
    offsetLocation(getRawX() - getX(), getRawY() - getY())
    for (i in 0 until this.pointerCount) {
        pointers.add(
            PointerInputEventData(this, i, upIndex)
        )
    }

    return PointerInputEvent(Uptime.Boot + eventTime.milliseconds, pointers)
}

/**
 * Creates a new [PointerInputEventData] with coordinates relative to the screen.
 */
private fun PointerInputEventData(
    motionEvent: MotionEvent,
    index: Int,
    upIndex: Int?
): PointerInputEventData {
    return PointerInputEventData(
        motionEvent.getPointerId(index),
        PointerInputData(
            Uptime(motionEvent.eventTime * NanosecondsPerMillisecond),
            motionEvent,
            index,
            upIndex
        )
    )
}

/**
 * Creates a new [PointerInputData] with coordinates that are relative to the screen.
 */
private fun PointerInputData(
    uptime: Uptime,
    motionEvent: MotionEvent,
    index: Int,
    upIndex: Int?
): PointerInputData {
    val offset = PxPosition(motionEvent.getX(index).px, motionEvent.getY(index).px)

    return PointerInputData(
        uptime,
        offset,
        index != upIndex
    )
}