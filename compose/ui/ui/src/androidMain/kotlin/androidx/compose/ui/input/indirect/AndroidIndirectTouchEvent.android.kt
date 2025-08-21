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

package androidx.compose.ui.input.indirect

import android.view.InputDevice
import android.view.InputDevice.SOURCE_TOUCH_NAVIGATION
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.geometry.Offset

internal class AndroidIndirectTouchEvent
@OptIn(ExperimentalIndirectTouchTypeApi::class)
constructor(
    override val position: Offset,
    override val uptimeMillis: Long,
    override val type: IndirectTouchEventType,
    override val primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis,
    internal val nativeEvent: MotionEvent,
) : PlatformIndirectTouchEvent

@ExperimentalIndirectTouchTypeApi
val IndirectTouchEvent.nativeEvent: MotionEvent
    get() = (this as AndroidIndirectTouchEvent).nativeEvent

/**
 * Allows creation of a [IndirectTouchEvent] from a [MotionEvent] for cross module testing.
 * IMPORTANT NOTE: Primary axis is determined by properties of the [InputDevice] contained within
 * the [MotionEvent]. However, when manually creating a [MotionEvent], there is no way to set the
 * [InputDevice]. Therefore, this function allows you to manually set the primary axis for testing.
 * If you have a system created [MotionEvent], you can call indirectScrollAxis() on your
 * [MotionEvent] to get the primary axis.
 */
@ExperimentalIndirectTouchTypeApi
fun IndirectTouchEvent(
    motionEvent: MotionEvent,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.None,
): IndirectTouchEvent =
    AndroidIndirectTouchEvent(
        position = Offset(motionEvent.x, motionEvent.y),
        uptimeMillis = motionEvent.eventTime,
        type = convertActionToIndirectTouchEventType(motionEvent.actionMasked),
        primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
        nativeEvent = motionEvent,
    )

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun convertActionToIndirectTouchEventType(actionMasked: Int): IndirectTouchEventType {
    return when (actionMasked) {
        ACTION_UP -> IndirectTouchEventType.Release
        ACTION_DOWN -> IndirectTouchEventType.Press
        ACTION_MOVE -> IndirectTouchEventType.Move
        else -> IndirectTouchEventType.Unknown
    }
}

@OptIn(ExperimentalIndirectTouchTypeApi::class)
internal fun indirectPrimaryDirectionalScrollAxis(
    motionEvent: MotionEvent
): IndirectTouchEventPrimaryDirectionalMotionAxis {
    require(motionEvent.isFromSource(SOURCE_TOUCH_NAVIGATION)) {
        "MotionEvent must be a touch navigation source"
    }

    motionEvent.device?.let { inputDevice ->
        val xMotionRange = inputDevice.getMotionRange(MotionEvent.AXIS_X)
        val yMotionRange = inputDevice.getMotionRange(MotionEvent.AXIS_Y)

        if (xMotionRange != null && yMotionRange == null) {
            return IndirectTouchEventPrimaryDirectionalMotionAxis.X
        } else if (yMotionRange != null && xMotionRange == null) {
            return IndirectTouchEventPrimaryDirectionalMotionAxis.Y
        } else if (xMotionRange != null && yMotionRange != null) {
            val xRange = xMotionRange.range
            val yRange = yMotionRange.range

            if ((xRange > yRange) && ((yRange == 0f) || (xRange / yRange >= RATIO_CUTOFF))) {
                return IndirectTouchEventPrimaryDirectionalMotionAxis.X
            } else if ((yRange > xRange) && ((xRange == 0f) || (yRange / xRange >= RATIO_CUTOFF))) {
                return IndirectTouchEventPrimaryDirectionalMotionAxis.Y
            }
        }
    }
    return IndirectTouchEventPrimaryDirectionalMotionAxis.None
}

// TODO: Remove once platform supports device specifying preferred axis for scrolling.
private const val RATIO_CUTOFF = 5f
