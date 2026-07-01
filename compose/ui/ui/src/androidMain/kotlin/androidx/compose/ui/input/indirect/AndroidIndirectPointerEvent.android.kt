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

import android.view.InputDevice.SOURCE_TOUCH_NAVIGATION
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.HistoricalChange
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.util.fastIsFinite
import org.jetbrains.annotations.TestOnly

internal class AndroidIndirectPointerEvent(
    override val changes: List<IndirectPointerInputChange>,
    override val type: IndirectPointerEventType,
    override val primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    internal val nativeEvent: MotionEvent,
) : PlatformIndirectPointerEvent {
    init {
        require(changes.isNotEmpty()) { "changes cannot be empty" }
    }
}

/** Returns the underlying [MotionEvent] for additional information and cross module testing. */
val IndirectPointerEvent.nativeEvent: MotionEvent
    get() = (this as AndroidIndirectPointerEvent).nativeEvent

/**
 * Create a [IndirectPointerEvent] for test use cases. In most cases, you should receive an
 * [IndirectPointerEvent] from the system through [IndirectPointerInputModifierNode].
 *
 * If you need to test indirect pointer events, use
 * [SemanticsNodeInteractionsProvider.sendIndirectPointerInput()] where you do not need to manually
 * create IndirectPointerEvents (instead calling higher-level functions).
 *
 * @param changes A list of [IndirectPointerInputChange] associated with the event
 * @param type Indicates the reason that the [IndirectPointerEvent] was sent.
 * @param primaryDirectionalMotionAxis Primary directional motion axis for testing.
 * @param motionEvent The [MotionEvent] to convert to an [IndirectPointerEvent].
 */
@TestOnly
fun IndirectPointerEvent(
    changes: List<IndirectPointerInputChange>,
    type: IndirectPointerEventType,
    primaryDirectionalMotionAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    motionEvent: MotionEvent,
): IndirectPointerEvent {
    return AndroidIndirectPointerEvent(
        changes = changes,
        type = type,
        primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
        nativeEvent = motionEvent,
    )
}

internal fun convertActionToIndirectPointerEventType(actionMasked: Int): IndirectPointerEventType {
    return when (actionMasked) {
        ACTION_UP,
        ACTION_POINTER_UP -> IndirectPointerEventType.Release
        ACTION_DOWN,
        ACTION_POINTER_DOWN -> IndirectPointerEventType.Press
        ACTION_MOVE -> IndirectPointerEventType.Move
        else -> IndirectPointerEventType.Unknown
    }
}

internal fun createIndirectPointerInputChangesFromMotionEvents(
    motionEvent: MotionEvent,
    previousMotionEvent: MotionEvent?,
): List<IndirectPointerInputChange> {
    val action = motionEvent.actionMasked
    val upIndex =
        when (action) {
            ACTION_UP -> 0
            ACTION_POINTER_UP -> motionEvent.actionIndex
            else -> -1
        }

    val previousAction = previousMotionEvent?.actionMasked
    val previousMotionEventWasPressed =
        previousAction?.let { isMotionEventPressed(previousAction) } ?: false

    val uptimeMillis = motionEvent.eventTime
    return List(motionEvent.pointerCount) { index ->
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
            motionEvent = motionEvent,
            motionEventIndex = index,
        )
    }
}

internal fun indirectPrimaryDirectionalScrollAxis(
    motionEvent: MotionEvent
): IndirectPointerEventPrimaryDirectionalMotionAxis {
    require(motionEvent.isFromSource(SOURCE_TOUCH_NAVIGATION)) {
        "MotionEvent must be a touch navigation source"
    }

    motionEvent.device?.let { inputDevice ->
        val xMotionRange = inputDevice.getMotionRange(MotionEvent.AXIS_X)
        val yMotionRange = inputDevice.getMotionRange(MotionEvent.AXIS_Y)

        if (xMotionRange != null && yMotionRange == null) {
            return IndirectPointerEventPrimaryDirectionalMotionAxis.X
        } else if (yMotionRange != null && xMotionRange == null) {
            return IndirectPointerEventPrimaryDirectionalMotionAxis.Y
        } else if (xMotionRange != null && yMotionRange != null) {
            val xRange = xMotionRange.range
            val yRange = yMotionRange.range

            if ((xRange > yRange) && ((yRange == 0f) || (xRange / yRange >= RATIO_CUTOFF))) {
                return IndirectPointerEventPrimaryDirectionalMotionAxis.X
            } else if ((yRange > xRange) && ((xRange == 0f) || (yRange / xRange >= RATIO_CUTOFF))) {
                return IndirectPointerEventPrimaryDirectionalMotionAxis.Y
            }
        }
    }
    return IndirectPointerEventPrimaryDirectionalMotionAxis.None
}

// Keep in sync with the [AndroidInputDispatcher.android.kt] version.
internal fun isMotionEventPressed(action: Int): Boolean =
    when (action) {
        ACTION_DOWN,
        ACTION_POINTER_DOWN,
        // Pointer up means only one of multiple pointers was lifted but another is still down,
        // so it is still pressed.
        ACTION_POINTER_UP,
        ACTION_MOVE -> true
        else -> false
    }

// TODO: Remove once platform supports device specifying preferred axis for scrolling.
private const val RATIO_CUTOFF = 5f

/**
 * Platform-specific constructor helper for Android [MotionEvent] sources that extracts
 * [HistoricalChange] events lazily.
 */
internal fun IndirectPointerInputChange(
    id: PointerId,
    uptimeMillis: Long,
    position: Offset,
    pressed: Boolean,
    pressure: Float,
    previousUptimeMillis: Long,
    previousPosition: Offset,
    previousPressed: Boolean,
    // Required for providing historical information on-demand
    motionEvent: MotionEvent,
    motionEventIndex: Int,
): IndirectPointerInputChange {
    if (motionEvent.historySize > 0) {
        return IndirectPointerInputChange(
            id = id,
            uptimeMillis = uptimeMillis,
            position = position,
            pressed = pressed,
            pressure = pressure,
            previousUptimeMillis = previousUptimeMillis,
            previousPosition = previousPosition,
            previousPressed = previousPressed,
            historical = LazyHistoricalChangeList(motionEvent, motionEventIndex),
        )
    }

    return IndirectPointerInputChange(
        id = id,
        uptimeMillis = uptimeMillis,
        position = position,
        pressed = pressed,
        pressure = pressure,
        previousUptimeMillis = previousUptimeMillis,
        previousPosition = previousPosition,
        previousPressed = previousPressed,
    )
}

/**
 * A lazy [List] implementation that computes the list of [HistoricalChange]s on-demand and clears
 * its [MotionEvent] reference after first evaluation to release resources.
 */
private class LazyHistoricalChangeList(
    private var motionEvent: MotionEvent?,
    private val index: Int,
) : AbstractList<HistoricalChange>() {
    private var delegate: List<HistoricalChange>? = null

    private fun getDelegate(): List<HistoricalChange> {
        var result = delegate
        if (result == null) {
            val event = motionEvent!!
            val historySize = event.historySize
            val list = ArrayList<HistoricalChange>(historySize)
            repeat(historySize) { pos ->
                val x = event.getHistoricalX(index, pos)
                val y = event.getHistoricalY(index, pos)
                if (x.fastIsFinite() && y.fastIsFinite()) {
                    list.add(
                        HistoricalChange(
                            uptimeMillis = event.getHistoricalEventTime(pos),
                            position = Offset(x, y),
                        )
                    )
                }
            }
            result = list
            delegate = result
            motionEvent = null // Release native/MotionEvent reference to prevent leaks
        }
        return result
    }

    override val size: Int
        get() = getDelegate().size

    override fun get(index: Int): HistoricalChange = getDelegate()[index]
}
