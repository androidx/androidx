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

package androidx.compose.ui.input.pointer

import android.os.Build
import android.util.SparseBooleanArray
import android.util.SparseLongArray
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_SCROLL
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_ERASER
import android.view.MotionEvent.TOOL_TYPE_FINGER
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import android.view.MotionEvent.TOOL_TYPE_STYLUS
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.collection.LongSparseArray
import androidx.collection.set
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.indirect.AndroidIndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.input.indirect.IndirectPointerInputChange
import androidx.compose.ui.input.indirect.convertActionToIndirectPointerEventType
import androidx.compose.ui.input.indirect.indirectPrimaryDirectionalScrollAxis
import androidx.compose.ui.util.fastIsFinite

/** Converts Android framework [MotionEvent]s into Compose [PointerInputEvent]s. */
internal class MotionEventAdapter {

    private var nextId = 0L

    /**
     * Whenever a new MotionEvent pointer is added, we create a new PointerId that is associated
     * with it. This holds that association.
     */
    @VisibleForTesting internal val motionEventToComposePointerIdMap = SparseLongArray()

    private val activeHoverIds = SparseBooleanArray()

    private val pointers = mutableListOf<PointerInputEventData>()

    private val previousIndirectPointerEventData: LongSparseArray<IndirectPointerEventData> =
        LongSparseArray()

    @JvmInline
    private value class IndirectPointerEventData(val packedValue: Long) {
        constructor(
            uptime: Long,
            position: Offset,
            down: Boolean,
        ) : this(
            (if (down) 1L else 0L) or // Bit 0 for 'down'
                ((uptime and 0x7FFF_FFFFL) shl 1) or // Bits 1-31 for 'uptime'
                (packShorts(position.x.toInt().toShort(), position.y.toInt().toShort()).toLong() shl
                    32) // Bits 32-63 for 'position'
        )

        val down: Boolean
            get() = (packedValue and 1L) != 0L

        val uptime: Long
            get() = (packedValue shr 1) and 0x7FFF_FFFFL

        val position: Offset
            get() {
                val packedShorts = (packedValue ushr 32).toInt()
                return Offset(
                    unpackShort1(packedShorts).toFloat(),
                    unpackShort2(packedShorts).toFloat(),
                )
            }

        companion object {
            // Helper functions to pack/unpack two Shorts into/from an Int.
            private fun packShorts(val1: Short, val2: Short): Int {
                return (val1.toInt() shl 16) or (val2.toInt() and 0xFFFF)
            }

            private fun unpackShort1(value: Int): Short {
                return (value ushr 16).toShort()
            }

            private fun unpackShort2(value: Int): Short {
                return (value and 0xFFFF).toShort()
            }
        }
    }

    /**
     * The previous event's tool type. This is used in combination with [previousSource] to
     * determine when a different device was used to send events.
     */
    private var previousToolType = -1

    /**
     * The previous event's source. This is used in combination with [previousToolType] to determine
     * when a different device was used to send events.
     */
    private var previousSource = -1

    /**
     * A piece of tracking data to infer whether we are currently in the middle of a trackpad fake
     * finger gesture.
     */
    private var isInFakeFingerGesture: Boolean = false

    /**
     * A piece of tracking data for whether we are currently reinterpreting the fake finger gesture
     * as a mouse event.
     */
    private var isReinterpretingFakeFingerGesture: Boolean = false

    /**
     * A piece of tracking data to infer the cursor location reinterpreting a fake finger gesture
     * that is coming from the trackpad. When a two finger swipe is ongoing, the fake finger will
     * start from this offset, and then wander like a finger on a touchscreen would to achieve the
     * equivalent swipe distance. The fake finger motion event doesn't currently publicly expose the
     * raw cursor position, so we need to keep track of it ourselves and reset it appropriately.
     */
    private var inferredCursorRawOffset: Offset? = null

    /** Resets the fake finger gesture tracking data, in preparation for a new gesture. */
    private fun resetFakeFingerGesture() {
        isInFakeFingerGesture = false
        isReinterpretingFakeFingerGesture = false
        inferredCursorRawOffset = null
    }

    /**
     * Converts a single [MotionEvent] from an Android event stream into a [PointerInputEvent], or
     * null if the [MotionEvent.getActionMasked] is [ACTION_CANCEL].
     *
     * All MotionEvents should be passed to this method so that it can correctly maintain it's
     * internal state.
     *
     * @param motionEvent The MotionEvent to process.
     * @return The PointerInputEvent or null if the event action was ACTION_CANCEL.
     */
    internal fun convertToPointerInputEvent(
        motionEvent: MotionEvent,
        positionCalculator: PositionCalculator,
    ): PointerInputEvent? {
        val action = motionEvent.actionMasked
        if (action == ACTION_CANCEL || action == ACTION_OUTSIDE) {
            motionEventToComposePointerIdMap.clear()
            activeHoverIds.clear()
            resetFakeFingerGesture()
            return null
        }
        clearOnDeviceChange(motionEvent)

        addFreshIds(motionEvent)

        val isHover =
            action == ACTION_HOVER_ENTER ||
                action == ACTION_HOVER_MOVE ||
                action == ACTION_HOVER_EXIT

        val isScroll = action == ACTION_SCROLL

        if (isHover) {
            val hoverId = motionEvent.getPointerId(motionEvent.actionIndex)
            activeHoverIds.put(hoverId, true)
        }

        val upIndex =
            when (action) {
                ACTION_UP -> 0
                ACTION_POINTER_UP -> motionEvent.actionIndex
                else -> -1
            }

        pointers.clear()

        // For record keeping, determine if we are in a fake finger gesture
        @OptIn(ExperimentalComposeUiApi::class)
        if (
            ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                motionEvent.actionMasked == ACTION_DOWN
        ) {
            val isFakeFingerGestureByClassification =
                Build.VERSION.SDK_INT >= 34 &&
                    (motionEvent.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE ||
                        motionEvent.classification == MotionEvent.CLASSIFICATION_PINCH)

            val isFakeFingerGestureByNoButtonStateAndSource =
                motionEvent.buttonState == 0 &&
                    (motionEvent.isFromSource(InputDevice.SOURCE_MOUSE) ||
                        motionEvent.isFromSource(InputDevice.SOURCE_TOUCHPAD))

            if (
                isFakeFingerGestureByClassification || isFakeFingerGestureByNoButtonStateAndSource
            ) {
                isInFakeFingerGesture = true
            }
        }

        // Re-interpret applicable trackpad events to mouse events, if possible, avoiding passing
        // through the fake fingers that would otherwise be added
        // TODO: Should we also re-interpret CLASSIFICATION_PINCH?
        @OptIn(ExperimentalComposeUiApi::class)
        if (
            ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                Build.VERSION.SDK_INT >= 34 &&
                motionEvent.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
        ) {
            isReinterpretingFakeFingerGesture = true
            // If this is the fake finger action down, store the location of the fake finger
            // as a proxy for the cursor position
            if (motionEvent.actionMasked == ACTION_DOWN) {
                inferredCursorRawOffset = Offset(motionEvent.getRawX(0), motionEvent.getRawY(0))
            }

            pointers.add(
                createPointerInputEventData(
                    positionCalculator = positionCalculator,
                    motionEvent = motionEvent,
                    rawPositionOverride = inferredCursorRawOffset,
                    index = 0,
                    pressed = false,
                )
            )
        } else {
            isReinterpretingFakeFingerGesture = false

            // The default case:
            // This converts the MotionEvent into a list of PointerInputEventData, and updates
            // internal record keeping.
            for (i in 0 until motionEvent.pointerCount) {
                pointers.add(
                    createPointerInputEventData(
                        positionCalculator = positionCalculator,
                        motionEvent = motionEvent,
                        rawPositionOverride = null,
                        index = i,
                        // "pressed" means:
                        // 1. we're not hovered
                        // 2. we didn't get UP event for a pointer
                        // 3. button on the mouse is pressed BUT it's not a "scroll" simulated
                        // button
                        pressed =
                            !isHover && i != upIndex && (!isScroll || motionEvent.buttonState != 0),
                    )
                )
            }
        }

        if (motionEvent.actionMasked == ACTION_UP) {
            resetFakeFingerGesture()
        }
        removeStaleIds(motionEvent)

        return PointerInputEvent(motionEvent.eventTime, pointers, motionEvent)
    }

    /*
     * Converts a single [MotionEvent] from an Android event stream into an
     * [AndroidIndirectPointerEvent].
     * @param motionEvent The MotionEvent to process.
     * @param primaryDirectionalMotionAxisOverride The primary directional motion axis override. The
     * primaryDirectionalMotionAxisOverride is used for testing, because there is no way to
     * override the primary directional motion axis from the MotionEvent's device properly with
     * mockito (MotionEvent and Mockito don't work together). The override allows tests to inject a
     * custom value (null means use the actual MotionEvent device's value).
     * @return The AndroidIndirectPointerEvent or null if the event action was ACTION_CANCEL.
     */
    internal fun convertToIndirectPointerEvent(
        motionEvent: MotionEvent,
        primaryDirectionalMotionAxisOverride: IndirectPointerEventPrimaryDirectionalMotionAxis? =
            null,
    ): AndroidIndirectPointerEvent? {
        val action = motionEvent.actionMasked

        clearOnDeviceChange(motionEvent)

        if (action == ACTION_CANCEL) {
            motionEventToComposePointerIdMap.clear()
            activeHoverIds.clear()
            return null
        }

        addFreshIds(motionEvent)

        val upIndex =
            when (action) {
                ACTION_UP -> 0
                ACTION_POINTER_UP -> motionEvent.actionIndex
                else -> -1
            }

        val downOrMove =
            when (action) {
                ACTION_DOWN,
                ACTION_POINTER_DOWN,
                ACTION_MOVE -> true
                else -> false
            }

        val changes =
            List(motionEvent.pointerCount) { index ->
                val motionEventPointerId = motionEvent.getPointerId(index)
                val pointerId = getComposePointerId(motionEventPointerId)
                val currentLocation =
                    Offset(x = motionEvent.getX(index), y = motionEvent.getY(index))
                val isPressed = index != upIndex

                val previousData = previousIndirectPointerEventData[pointerId.value]

                if (index == upIndex) {
                    previousIndirectPointerEventData.remove(pointerId.value)
                } else if (downOrMove) {
                    previousIndirectPointerEventData[pointerId.value] =
                        IndirectPointerEventData(
                            uptime = motionEvent.eventTime,
                            position = currentLocation,
                            down = true,
                        )
                }

                IndirectPointerInputChange(
                    id = pointerId,
                    uptimeMillis = motionEvent.eventTime,
                    position = currentLocation,
                    pressed = isPressed,
                    pressure = motionEvent.getPressure(index),
                    previousUptimeMillis = previousData?.uptime ?: motionEvent.eventTime,
                    previousPosition = previousData?.position ?: currentLocation,
                    previousPressed = previousData?.down ?: false,
                )
            }

        removeStaleIds(motionEvent)

        val primaryDirectionalMotionAxis =
            primaryDirectionalMotionAxisOverride
                ?: indirectPrimaryDirectionalScrollAxis(motionEvent)

        return AndroidIndirectPointerEvent(
            changes = changes,
            type = convertActionToIndirectPointerEventType(action),
            primaryDirectionalMotionAxis = primaryDirectionalMotionAxis,
            nativeEvent = motionEvent,
        )
    }

    /**
     * An ACTION_DOWN or ACTION_POINTER_DOWN was received, but not handled, so the stream should be
     * considered ended.
     */
    fun endStream(pointerId: Int) {
        activeHoverIds.delete(pointerId)
        motionEventToComposePointerIdMap.delete(pointerId)
    }

    /** Add any new pointer IDs. */
    private fun addFreshIds(motionEvent: MotionEvent) {
        when (motionEvent.actionMasked) {
            ACTION_HOVER_ENTER -> {
                val pointerId = motionEvent.getPointerId(0)
                if (motionEventToComposePointerIdMap.indexOfKey(pointerId) < 0) {
                    motionEventToComposePointerIdMap.put(pointerId, nextId++)
                }
            }
            ACTION_DOWN,
            ACTION_POINTER_DOWN -> {
                val actionIndex = motionEvent.actionIndex
                val pointerId = motionEvent.getPointerId(actionIndex)
                if (motionEventToComposePointerIdMap.indexOfKey(pointerId) < 0) {
                    motionEventToComposePointerIdMap.put(pointerId, nextId++)
                    if (motionEvent.getToolType(actionIndex) == TOOL_TYPE_MOUSE) {
                        activeHoverIds.put(pointerId, true)
                    }
                }
            }
        }
    }

    /**
     * Remove any raised pointers if they didn't previously hover. Anything that hovers will stay
     * until a different event causes it to be removed.
     */
    private fun removeStaleIds(motionEvent: MotionEvent) {
        when (motionEvent.actionMasked) {
            ACTION_POINTER_UP,
            ACTION_UP -> {
                val actionIndex = motionEvent.actionIndex
                val pointerId = motionEvent.getPointerId(actionIndex)
                if (!activeHoverIds.get(pointerId, false)) {
                    motionEventToComposePointerIdMap.delete(pointerId)
                    activeHoverIds.delete(pointerId)
                }
            }
        }

        // Remove any IDs that don't currently exist in the MotionEvent.
        // This can happen, for example, when a mouse cursor disappears and the next
        // event is a touch event.
        if (motionEventToComposePointerIdMap.size() > motionEvent.pointerCount) {
            for (i in motionEventToComposePointerIdMap.size() - 1 downTo 0) {
                val pointerId = motionEventToComposePointerIdMap.keyAt(i)
                if (!motionEvent.hasPointerId(pointerId)) {
                    motionEventToComposePointerIdMap.removeAt(i)
                    activeHoverIds.delete(pointerId)
                }
            }
        }
    }

    private fun MotionEvent.hasPointerId(pointerId: Int): Boolean {
        for (i in 0 until pointerCount) {
            if (getPointerId(i) == pointerId) {
                return true
            }
        }
        return false
    }

    private fun getComposePointerId(motionEventPointerId: Int): PointerId {
        val pointerIndex = motionEventToComposePointerIdMap.indexOfKey(motionEventPointerId)
        val id =
            if (pointerIndex >= 0) {
                motionEventToComposePointerIdMap.valueAt(pointerIndex)
            } else {
                // An unexpected pointer was added or we may have previously removed it
                val newId = nextId++
                motionEventToComposePointerIdMap.put(motionEventPointerId, newId)
                newId
            }
        return PointerId(id)
    }

    /**
     * When the device has changed (noted by source and tool type), we don't need to track any of
     * the previous pointers.
     */
    private fun clearOnDeviceChange(motionEvent: MotionEvent) {
        if (motionEvent.pointerCount != 1) {
            return
        }
        val toolType = motionEvent.getToolType(0)
        val source = motionEvent.source

        if (toolType != previousToolType || source != previousSource) {
            previousToolType = toolType
            previousSource = source
            activeHoverIds.clear()
            motionEventToComposePointerIdMap.clear()
        }
    }

    /**
     * Creates a new PointerInputEventData.
     *
     * @param rawPositionOverride if specified, the offset to use for the raw position of the
     *   pointer input event data, overriding whatever is in the [motionEvent]. This will result in
     *   both [PointerInputEventData.position] and [PointerInputEventData.positionOnScreen] being
     *   overridden, but _not_ [PointerInputEventData.originalEventPosition]. This is supported on
     *   Android Q and above for all indices and for the first index on Android P and below. For
     *   non-zero indices on Android P and below for, the derivation of the position works
     *   differently, where the raw position is derived from the normal position, so therefore the
     *   [rawPositionOverride] has no effect. Currently we have no usages of a non-null
     *   `rawPositionOverride` on Android P and below, so that case can't be reached.
     */
    private fun createPointerInputEventData(
        positionCalculator: PositionCalculator,
        motionEvent: MotionEvent,
        rawPositionOverride: Offset?,
        index: Int,
        pressed: Boolean,
    ): PointerInputEventData {

        val motionEventPointerId = motionEvent.getPointerId(index)

        val pointerId = getComposePointerId(motionEventPointerId)

        val pressure = motionEvent.getPressure(index)

        val rawPosition: Offset
        val position: Offset
        val originalPositionEventPosition = Offset(motionEvent.getX(index), motionEvent.getY(index))
        if (index == 0) {
            rawPosition = rawPositionOverride ?: Offset(motionEvent.rawX, motionEvent.rawY)
            position = positionCalculator.screenToLocal(rawPosition)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rawPosition = rawPositionOverride ?: MotionEventHelper.toRawOffset(motionEvent, index)
            position = positionCalculator.screenToLocal(rawPosition)
        } else {
            position = originalPositionEventPosition
            rawPosition = positionCalculator.localToScreen(position)
        }

        val toolType =
            when (motionEvent.getToolType(index)) {
                TOOL_TYPE_UNKNOWN -> PointerType.Unknown
                TOOL_TYPE_FINGER -> {
                    // Convert trackpad events to mouse events when it is safe to do so.
                    @OptIn(ExperimentalComposeUiApi::class)
                    if (ComposeUiFlags.isTrackpadGestureHandlingEnabled) {
                        if (
                            (motionEvent.isFromSource(InputDevice.SOURCE_MOUSE) ||
                                motionEvent.isFromSource(InputDevice.SOURCE_TOUCHPAD)) &&
                                (!isInFakeFingerGesture || isReinterpretingFakeFingerGesture)
                        ) {
                            PointerType.Mouse
                        } else {
                            PointerType.Touch
                        }
                    } else {
                        PointerType.Touch
                    }
                }
                TOOL_TYPE_STYLUS -> PointerType.Stylus
                TOOL_TYPE_MOUSE -> PointerType.Mouse
                TOOL_TYPE_ERASER -> PointerType.Eraser
                else -> PointerType.Unknown
            }

        val historical = ArrayList<HistoricalChange>(motionEvent.historySize)
        with(motionEvent) {
            repeat(historySize) { pos ->
                val x = getHistoricalX(index, pos)
                val y = getHistoricalY(index, pos)
                if (x.fastIsFinite() && y.fastIsFinite()) {
                    val originalEventPosition = Offset(x, y) // hit path will convert to local
                    val historicalChange =
                        HistoricalChange(
                            uptimeMillis = getHistoricalEventTime(pos),
                            position = originalEventPosition,
                            scaleGestureFactor =
                                getHistoricalAxisValue(
                                        MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR,
                                        index,
                                        pos,
                                    )
                                    .takeIf { it > 0 } ?: 1f,
                            panGestureOffset =
                                if (
                                    Build.VERSION.SDK_INT >= 29 &&
                                        motionEvent.classification ==
                                            MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
                                ) {
                                    Offset(
                                        motionEvent.getHistoricalAxisValue(
                                            MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE,
                                            index,
                                            pos,
                                        ),
                                        motionEvent.getHistoricalAxisValue(
                                            MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE,
                                            index,
                                            pos,
                                        ),
                                    )
                                } else {
                                    Offset.Zero
                                },
                            originalEventPosition = originalEventPosition,
                        )
                    historical.add(historicalChange)
                }
            }
        }
        val scrollDelta =
            if (motionEvent.actionMasked == ACTION_SCROLL) {
                val x = motionEvent.getAxisValue(MotionEvent.AXIS_HSCROLL)
                val y = motionEvent.getAxisValue(MotionEvent.AXIS_VSCROLL)
                // NOTE: we invert the y scroll offset because android is special compared to other
                // platforms and uses the opposite sign for vertical mouse wheel scrolls. In order
                // to
                // support better x-platform mouse scroll, we invert the y-offset to be in line with
                // desktop and web.
                //
                // This looks more natural, because when we scroll mouse wheel up,
                // we move the wheel point (that touches the finger) up. And if we work in the usual
                // coordinate system, it means we move that point by "-1".
                //
                // Web also behaves this way. See deltaY:
                // https://developer.mozilla.org/en-US/docs/Web/API/Element/wheel_event
                // https://jsfiddle.net/27zwteog
                // (wheelDelta on the other hand is deprecated and inverted)
                //
                // We then add 0f to prevent injecting -0.0f into the pipeline, which can be
                // problematic when doing comparisons.
                Offset(x, -y + 0f)
            } else {
                Offset.Zero
            }

        /**
         * The gesture scale factor. Note that because this is a multiplicative factor, `1` is the
         * identity value that indicates no change - so we need to manually handle the case of
         * seeing a `0` indicating that the axis is missing.
         */
        @OptIn(ExperimentalComposeUiApi::class)
        val gestureScaleFactor =
            if (
                ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                    Build.VERSION.SDK_INT >= 29 &&
                    motionEvent.classification == MotionEvent.CLASSIFICATION_PINCH
            ) {
                motionEvent
                    .getAxisValue(MotionEvent.AXIS_GESTURE_PINCH_SCALE_FACTOR, index)
                    .takeIf { it > 0 } ?: 1f
            } else {
                1f
            }

        /** The offset for scrolling, expressed as a delta in pixel coordinates. */
        @OptIn(ExperimentalComposeUiApi::class)
        val gesturePanOffset =
            if (
                ComposeUiFlags.isTrackpadGestureHandlingEnabled &&
                    Build.VERSION.SDK_INT >= 29 &&
                    motionEvent.classification == MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE
            ) {
                Offset(
                    motionEvent.getAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_X_DISTANCE, index),
                    motionEvent.getAxisValue(MotionEvent.AXIS_GESTURE_SCROLL_Y_DISTANCE, index),
                )
            } else {
                Offset.Zero
            }

        val activeHover = activeHoverIds.get(motionEvent.getPointerId(index), false)
        return PointerInputEventData(
            id = pointerId,
            uptime = motionEvent.eventTime,
            positionOnScreen = rawPosition,
            position = position,
            down = pressed,
            pressure = pressure,
            type = toolType,
            activeHover = activeHover,
            historical = historical,
            scrollDelta = scrollDelta,
            scaleGestureFactor = gestureScaleFactor,
            panGestureOffset = gesturePanOffset,
            originalEventPosition = originalPositionEventPosition,
        )
    }
}

/**
 * This class is here to ensure that the classes that use this API will get verified and can be AOT
 * compiled. It is expected that this class will soft-fail verification, but the classes which use
 * this method will pass.
 */
@RequiresApi(Build.VERSION_CODES.Q)
private object MotionEventHelper {
    fun toRawOffset(motionEvent: MotionEvent, index: Int): Offset {
        return Offset(motionEvent.getRawX(index), motionEvent.getRawY(index))
    }
}
