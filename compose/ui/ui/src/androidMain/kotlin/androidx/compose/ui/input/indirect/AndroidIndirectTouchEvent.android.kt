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
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.ExperimentalIndirectTouchTypeApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId

internal class AndroidIndirectTouchEvent(
    override val changes: List<IndirectPointerInputChange>,
    override val type: IndirectTouchEventType,
    override val primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis,
    internal val nativeEvent: MotionEvent,
) : PlatformIndirectTouchEvent {
    init {
        require(changes.isNotEmpty()) { "changes cannot be empty" }
    }
}

/** Returns the underlying [MotionEvent] for additional information and cross module testing. */
val IndirectTouchEvent.nativeEvent: MotionEvent
    get() = (this as AndroidIndirectTouchEvent).nativeEvent

/**
 * Allows creation of a [IndirectTouchEvent] from a [MotionEvent] for cross module testing.
 * IMPORTANT NOTE 1: Primary axis is determined by properties of the [InputDevice] contained within
 * the [MotionEvent]. However, when manually creating a [MotionEvent], there is no way to set the
 * [InputDevice]. Therefore, this function allows you to manually set the primary axis for testing.
 * If you have a system created [MotionEvent], you can call indirectPrimaryDirectionalScrollAxis()
 * on your [MotionEvent] to get the primary axis. IMPORTANT NOTE 2: Since this is just a test
 * function that doesn't maintain state for previous [MotionEvent]s (like the Android Compose system
 * does), you will need to pass a separate [MotionEvent] to populate IndirectPointerInputChange's
 * "previous" parameters (time, position, and pressed).
 *
 * @param motionEvent The [MotionEvent] to convert to an [IndirectTouchEvent].
 * @param primaryDirectionalMotionAxis Primary directional motion axis for testing.
 * @param previousMotionEvent The [MotionEvent] for previous values (time, position, and pressed).
 */
@ExperimentalIndirectTouchTypeApi
fun IndirectTouchEvent(
    motionEvent: MotionEvent,
    primaryDirectionalMotionAxis: IndirectTouchEventPrimaryDirectionalMotionAxis =
        IndirectTouchEventPrimaryDirectionalMotionAxis.None,
    previousMotionEvent: MotionEvent? = null,
): IndirectTouchEvent {
    val action = motionEvent.actionMasked
    val upIndex =
        when (action) {
            ACTION_UP -> 0
            ACTION_POINTER_UP -> motionEvent.actionIndex
            else -> -1
        }

    val previousAction = previousMotionEvent?.actionMasked
    val previousMotionEventWasPressed =
        when (previousAction) {
            ACTION_DOWN,
            ACTION_POINTER_DOWN,
            ACTION_MOVE -> true
            else -> false
        }

    val uptimeMillis = motionEvent.eventTime
    val changes =
        List(motionEvent.pointerCount) { index ->
            // For tests, we directly use the motion event's pointer ID vs. the production approach
            // of translate MotionEvent ids to separate Compose PointerIds.
            val motionEventPointerId = motionEvent.getPointerId(index)
            val pointerId = PointerId(motionEventPointerId.toLong())
            val position = Offset(motionEvent.getX(index), motionEvent.getY(index))

            val pressed = index != upIndex

            val matchedPointerIdInPreviousMotionEventIndex =
                previousMotionEvent?.findPointerIndex(motionEventPointerId) ?: -1

            val previousUptimeMillis: Long
            val previousPosition: Offset
            val previousPressed: Boolean

            if (matchedPointerIdInPreviousMotionEventIndex >= 0) {
                // Found existing id in previous event
                previousUptimeMillis = previousMotionEvent!!.eventTime
                previousPosition =
                    Offset(
                        previousMotionEvent.getX(matchedPointerIdInPreviousMotionEventIndex),
                        previousMotionEvent.getY(matchedPointerIdInPreviousMotionEventIndex),
                    )
                previousPressed = previousMotionEventWasPressed
            } else {
                // Existing id NOT in previous event, so we match the current event values minus
                // pressed, that should always be false.
                previousUptimeMillis = uptimeMillis
                previousPosition = position
                previousPressed = false
            }

            IndirectPointerInputChange(
                id = pointerId,
                uptimeMillis = uptimeMillis,
                position = position,
                pressed = pressed,
                pressure = motionEvent.getPressure(index),
                previousUptimeMillis = previousUptimeMillis,
                previousPosition = previousPosition,
                previousPressed = previousPressed,
            )
        }

    return AndroidIndirectTouchEvent(
        changes = changes,
        type = convertActionToIndirectTouchEventType(action),
        primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
        nativeEvent = motionEvent,
    )
}

internal fun convertActionToIndirectTouchEventType(actionMasked: Int): IndirectTouchEventType {
    return when (actionMasked) {
        ACTION_UP,
        ACTION_POINTER_UP -> IndirectTouchEventType.Release
        ACTION_DOWN,
        ACTION_POINTER_DOWN -> IndirectTouchEventType.Press
        ACTION_MOVE -> IndirectTouchEventType.Move
        else -> IndirectTouchEventType.Unknown
    }
}

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
