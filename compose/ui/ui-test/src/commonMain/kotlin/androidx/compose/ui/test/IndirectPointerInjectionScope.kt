/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.annotation.FloatRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.input.indirect.IndirectPointerEvent
import androidx.compose.ui.input.indirect.IndirectPointerEventPrimaryDirectionalMotionAxis
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.test.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * The receiver scope of the indirect pointer input injection lambda from
 * [performIndirectPointerInput].
 *
 * Indirect pointer input events are dispatched through the focused tree, and components will only
 * receive these events if they are focused, or an ancestor of a focused item. Therefore, this API
 * requires an active focus state.
 *
 * An indirect pointer gesture (just like a regular touch gesture) starts with a [down] event,
 * followed by a sequence of [move] events and finally an [up] event, optionally combined with more
 * sets of [down] and [up] events for multitouch gestures.
 *
 * Most methods accept a pointerId to specify which pointer (finger) the event applies to. Movement
 * can be expressed absolutely with [moveTo] and [updatePointerTo], or relative to the current
 * pointer position with [moveBy] and [updatePointerBy]. The `moveTo/By` methods enqueue an event
 * immediately, while the `updatePointerTo/By` methods don't. This allows you to update the position
 * of multiple pointers in a single [move] event for multitouch gestures. Indirect pointer input
 * gestures can be canceled with [cancel]. All events, regardless the method used, will always
 * contain the current position of _all_ pointers.
 *
 * The entire event injection state is shared between all `perform.*Input` methods, meaning you can
 * continue an unfinished Indirect pointer input gesture in a subsequent invocation of
 * [performIndirectPointerInput] or [performMultiModalInput].
 *
 * All events sent by these methods are batched together and sent as a whole after
 * [performIndirectPointerInput] has executed its code block. Because gestures don't have to be
 * defined all in the same [performIndirectPointerInput] block, keep in mind that while the gesture
 * is not complete, all code you execute in between these blocks will be executed while imaginary
 * fingers are actively touching the indirect pointer input device. Remember, indirect pointer
 * events do NOT correlate to the screen, so those finger locations won't map to a screen x and y.
 * The x and y coordinates are instead mapped to the specific input device being used during the
 * interaction.
 *
 * The events sent as part of the same batch will not be interrupted by recomposition. However, if a
 * gesture spans multiple [performIndirectPointerInput] blocks it is important to remember that
 * recomposition, layout and drawing could take place during the gesture, which may lead to events
 * being injected into a moving target.
 *
 * This scope also provides general capabilities such as advancing event time and accessing the
 * system [ViewConfiguration]. It also implements [Density] to facilitate conversion between pixels
 * and density-independent pixels.
 *
 * For injection methods that require specific node information (e.g., [performTouchInput],
 * [performMouseInput]), use [InjectionScope].
 *
 * Example of performing an indirect pointer click:
 *
 * @sample androidx.compose.ui.test.samples.indirectPointerInputClick
 * @sample androidx.compose.ui.test.samples.indirectPointerInputAssertDuringClick
 *
 * Example of performing an indirect pointer swipe:
 *
 * @sample androidx.compose.ui.test.samples.indirectPointerInputSwipeRight
 */
@JvmDefaultWithCompatibility
interface IndirectPointerInjectionScope : Density {
    /** The default time between two successive events. */
    val eventPeriodMillis
        get() = InputDispatcher.eventPeriodMillis

    /**
     * Adds the given [durationMillis] to the current event time, delaying the next event by that
     * time.
     */
    fun advanceEventTime(durationMillis: Long = eventPeriodMillis)

    /**
     * The dimensions of the external indirect pointer input device that provide the boundaries for
     * indirect input. If you go outside these dimensions, the tests will throw an exception. Note:
     * This is not related to the screen coordinates.
     */
    val inputDeviceSize: IntSize

    /**
     * The primary axis for motion from an [IndirectPointerEvent]. Indirect input devices (such as
     * touchpads) that do not move a cursor on screen may define a primary axis for motion (such as
     * scrolling). This facilitates the translation of a 2D input gesture into a 1D scroll on the
     * screen. For example, an input device might be wide horizontally but narrow vertically. In
     * such a case, it would designate X as its primary axis of motion. This means horizontal
     * scrolling on the input device would cause a horizontal list to scroll horizontally, and a
     * vertical list to scroll vertically - even though the direction of motion on the input device
     * is horizontal in both cases.
     */
    val indirectPointerEventPrimaryDirectionalMotionAxis:
        IndirectPointerEventPrimaryDirectionalMotionAxis

    /**
     * The [ViewConfiguration] in use by the
     * [SemanticsNode][androidx.compose.ui.semantics.SemanticsNode] from the
     * [SemanticsNodeInteraction] on which the input injection method is called.
     */
    val viewConfiguration: ViewConfiguration

    /**
     * Returns the current position of the given [pointerId]. The default [pointerId] is 0. The
     * position is returned in the coordinate system of the device sending the input (see
     * [inputDeviceSize]). It is NOT related to the screen location.
     */
    fun currentPosition(pointerId: Int = 0): Offset?

    /**
     * Sends a down event for the pointer with the given [pointerId] at [position] on the external
     * indirect pointer input device. The [position] is NOT in the node's local coordinate system
     * (see [inputDeviceSize]).
     *
     * If no pointers are down yet, this will start a new Indirect pointer input gesture. If a
     * gesture is already in progress (that is, there are other pointer ids that are down), this
     * event is sent at the same timestamp as the last event. You cannot call down with a pointer id
     * that is already down.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The position of the down event, in the input device's coordinate system.
     * @throws IllegalArgumentException if the given pointer id is already down.
     */
    fun down(pointerId: Int, position: Offset)

    /**
     * Sends a down event for the default pointer at [position] on the indirect pointer input device
     * sending the input. The [position] is NOT in the node's local coordinate system (see
     * [inputDeviceSize]).
     *
     * If no pointers are down yet, this will start a new Indirect pointer input gesture. If a
     * gesture is already in progress, this event is sent at the same timestamp as the last event.
     * If the given pointer is already down, @throws [IllegalArgumentException].
     *
     * @param position The position of the down event, in the input device's coordinate system.
     */
    fun down(position: Offset) {
        down(0, position)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on nodes in the focus path, with
     * the position of the pointer with the given [pointerId] updated to [position]. The [position]
     * is NOT in the node's local coordinate system (see [inputDeviceSize]).
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param position The new position of the pointer, in the indirect pointer input device's
     *   coordinate system
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveTo(pointerId: Int, position: Offset, delayMillis: Long = eventPeriodMillis) {
        updatePointerTo(pointerId, position)
        move(delayMillis)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on nodes in the focus path, with
     * the position of the default pointer updated to [position]. The [position] is NOT in the
     * node's local coordinate system (see [inputDeviceSize]).
     *
     * If the default pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param position The new position of the pointer, in the indirect pointer input device's
     *   coordinate system
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveTo(position: Offset, delayMillis: Long = eventPeriodMillis) {
        moveTo(0, position, delayMillis)
    }

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position] within
     * the indirect pointer input device's bounds, but does not send a move event. The move event
     * can be sent with [move]. The [position] is NOT in the node's local coordinate system (see
     * [inputDeviceSize]).
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param position The new position of the pointer, in the indirect pointer input device's
     *   coordinate system
     */
    fun updatePointerTo(pointerId: Int, position: Offset)

    /**
     * Updates the position of the default pointer (`pointerId = 0`) to the given [position] within
     * the indirect pointer input device's bounds, but does not send a move event. The move event
     * can be sent with [move]. The [position] is NOT in the node's local coordinate system (see
     * [inputDeviceSize]).
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param position The new position of the pointer, in the indirect pointer input device's
     *   coordinate system
     */
    fun updatePointerTo(position: Offset) {
        updatePointerTo(0, position)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on nodes in the focus path, with
     * the position of the pointer with the given [pointerId] moved by the given [delta].
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param delta The position for this move event, relative to the current position of the
     *   pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     *   x-position, and subtract 10.px from the pointer's y-position.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveBy(pointerId: Int, delta: Offset, delayMillis: Long = eventPeriodMillis) {
        updatePointerBy(pointerId, delta)
        move(delayMillis)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event on nodes in the focus path, with
     * the position of the default pointer moved by the given [delta]. The default pointer has
     * `pointerId = 0`.
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param delta The position for this move event, relative to the current position of the
     *   pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     *   x-position, and subtract 10.px from the pointer's y-position.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveBy(delta: Offset, delayMillis: Long = eventPeriodMillis) {
        moveBy(0, delta, delayMillis)
    }

    /**
     * Updates the position of the pointer with the given [pointerId] by the given [delta], but does
     * not send a move event. The move event can be sent with [move].
     *
     * If the pointer is not yet down, @throws [IllegalArgumentException].
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param delta The position for this move event, relative to the last sent position of the
     *   pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     *   x-position, and subtract 10.px from the pointer's y-position.
     */
    fun updatePointerBy(pointerId: Int, delta: Offset) {
        // Ignore currentPosition of null here, let updatePointerTo generate the error
        val currentPosition = currentPosition(pointerId) ?: Offset.Zero

        val position =
            if (currentPosition.isValid() && delta.isValid()) {
                currentPosition + delta
            } else {
                // Allows invalid position to still pass back through Compose (for testing)
                Offset.Unspecified
            }

        updatePointerTo(pointerId, position)
    }

    /**
     * Updates the position of the default pointer by the given [delta], but does not send a move
     * event. The move event can be sent with [move]. The default pointer is `pointerId = 0`.
     *
     * If the pointer is not yet down, an [IllegalArgumentException] will be thrown.
     *
     * @param delta The position for this move event, relative to the last sent position of the
     *   pointer. For example, `delta = Offset(10.px, -10.px) will add 10.px to the pointer's
     *   x-position, and subtract 10.px from the pointer's y-position.
     */
    fun updatePointerBy(delta: Offset) {
        updatePointerBy(0, delta)
    }

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions. This can be useful when batching movement of multiple pointers together,
     * which can be done with [updatePointerTo] and [updatePointerBy].
     *
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun move(delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions, while adding the [historicalCoordinates] at the [relativeHistoricalTimes]
     * to the move event. This corresponds to the scenario where an external touchpad generates
     * Indirect pointer input events quicker than can be dispatched and batches them together.
     *
     * @param relativeHistoricalTimes Time of each historical event, as a millisecond relative to
     *   the time the actual event is sent. For example, -10L means 10ms earlier.
     * @param historicalCoordinates Coordinates of each historical event, in the same coordinate
     *   space as [moveTo]. The outer list must have the same size as the number of pointers in the
     *   event, and each inner list must have the same size as [relativeHistoricalTimes]. The `i`th
     *   pointer is assigned the `i`th history, with the pointers sorted on ascending pointerId.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    @Suppress("PrimitiveInCollection")
    fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long = eventPeriodMillis,
    )

    /**
     * Sends a move event [delayMillis] after the last sent event without updating any of the
     * pointer positions, while adding the [historicalCoordinates] at the [relativeHistoricalTimes]
     * to the move event. This corresponds to the scenario where the external device generates
     * Indirect pointer input events quicker than can be dispatched and batches them together.
     *
     * This overload is a convenience method for the common case where the gesture only has one
     * pointer.
     *
     * @param relativeHistoricalTimes Time of each historical event, as a millisecond relative to
     *   the time the actual event is sent. For example, -10L means 10ms earlier.
     * @param historicalCoordinates Coordinates of each historical event, in the same coordinate
     *   space as [moveTo]. The list must have the same size as [relativeHistoricalTimes].
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    @Suppress("PrimitiveInCollection")
    fun moveWithHistory(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<Offset>,
        delayMillis: Long = eventPeriodMillis,
    ) =
        moveWithHistoryMultiPointer(
            relativeHistoricalTimes,
            listOf(historicalCoordinates),
            delayMillis,
        )

    /**
     * Sends an up event for the pointer with the given [pointerId], or the default pointer if
     * [pointerId] is omitted, on nodes in the focus path.
     *
     * @param pointerId The id of the pointer to liftup, as supplied in [down]
     */
    fun up(pointerId: Int = 0)

    /**
     * Sends a cancel event [delayMillis] after the last sent event to cancel the current gesture.
     * The cancel event contains the current position of all active pointers.
     *
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun cancel(delayMillis: Long = eventPeriodMillis)
}

/**
 * The width of the external indirect pointer input device that provide the width boundary for
 * indirect input. If you go outside this width, the tests will throw an exception. Note: This is
 * not related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceWidth: Int
    get() = inputDeviceSize.width

/**
 * The height of the external indirect pointer input device that provide the height boundary for
 * indirect input. If you go outside this height, the tests will throw an exception. Note: This is
 * not related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceHeight: Int
    get() = inputDeviceSize.height

/**
 * The x-coordinate for the left edge of the indirect pointer input device we're interacting with in
 * px, where (0, 0) is the top left corner of the indirect pointer input device. Note: This is not
 * related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceLeft: Float
    get() = 0f

/**
 * The y-coordinate for the top of the indirect pointer input device we're interacting with in px,
 * where (0, 0) is the top left corner of the indirect pointer input device. Note: This is not
 * related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceTop: Float
    get() = 0f

/**
 * The x-coordinate for the center of the indirect pointer input device we're interacting with in
 * px, where (0, 0) is the top left corner of the indirect pointer input device. Note: This is not
 * related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceCenterX: Float
    get() = inputDeviceWidth / 2f

/**
 * The y-coordinate for the center of the indirect pointer input device we're interacting with in
 * px, where (0, 0) is the top left corner of the indirect pointer input device. Note: This is not
 * related to the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceCenterY: Float
    get() = inputDeviceHeight / 2f

/**
 * The x-coordinate for the right edge of the indirect pointer input device we're interacting with
 * in px, where (0, 0) is the top left corner of the indirect pointer input device. Note: This is
 * not related to the screen coordinates.
 *
 * Note that, unless `inputDeviceWidth == 0`, `inputDeviceRight != inputDeviceWidth`. In particular,
 * `inputDeviceRight == inputDeviceWidth - 1f`, because pixels are 0-based. If `inputDeviceWidth ==
 * 0`, `inputDeviceRight == 0` too.
 */
val IndirectPointerInjectionScope.inputDeviceRight: Float
    get() = inputDeviceWidth.let { if (it == 0) 0f else it - 1f }

/**
 * The y-coordinate for the bottom of the indirect pointer input device we're interacting with in
 * px, where (0, 0) is the top left corner of the indirect pointer input device. Note: This is not
 * related to the screen coordinates.
 *
 * Note that, unless `inputDeviceHeight == 0`, `inputDeviceBottom != inputDeviceHeight`. In
 * particular, `inputDeviceBottom == inputDeviceHeight - 1f`, because pixels are 0-based. If
 * `inputDeviceHeight == 0`, `inputDeviceBottom == 0` too.
 */
val IndirectPointerInjectionScope.inputDeviceBottom: Float
    get() = inputDeviceHeight.let { if (it == 0) 0f else it - 1f }

/**
 * The top left corner of the indirect pointer input device we're interacting with, where (0, 0) is
 * the top left corner of the indirect pointer input device. Note: This is not related to the screen
 * coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceTopLeft: Offset
    get() = Offset(inputDeviceLeft, inputDeviceTop)

/**
 * The center of the top edge of the indirect pointer input device we're interacting with, where
 * (0, 0) is the top left corner of the indirect pointer input device. Note: This is not related to
 * the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceTopCenter: Offset
    get() = Offset(inputDeviceCenterX, inputDeviceTop)

/**
 * The top right corner of the indirect pointer input device we're interacting with, where (0, 0) is
 * the top left corner of the indirect pointer input device. Note: This is not related to the screen
 * coordinates.
 *
 * Note that `inputDeviceTopRight.x != inputDeviceWidth`, see [inputDeviceRight].
 */
val IndirectPointerInjectionScope.inputDeviceTopRight: Offset
    get() = Offset(inputDeviceRight, inputDeviceTop)

/**
 * The center of the left edge of the indirect pointer input device we're interacting with, where
 * (0, 0) is the top left corner of the indirect pointer input device. Note: This is not related to
 * the screen coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceCenterLeft: Offset
    get() = Offset(inputDeviceLeft, inputDeviceCenterY)

/**
 * The center of the indirect pointer input device we're interacting with, where (0, 0) is the top
 * left corner of the indirect pointer input device. Note: This is not related to the screen
 * coordinates.
 */
val IndirectPointerInjectionScope.inputDeviceCenter: Offset
    get() = Offset(inputDeviceCenterX, inputDeviceCenterY)

/**
 * The center of the right edge of the indirect pointer input device we're interacting with, where
 * (0, 0) is the top left corner of the indirect pointer input device. Note: This is not related to
 * the screen coordinates.
 *
 * Note that `inputDeviceCenterRight.x != inputDeviceWidth`, see [inputDeviceRight].
 */
val IndirectPointerInjectionScope.inputDeviceCenterRight: Offset
    get() = Offset(inputDeviceRight, inputDeviceCenterY)

/**
 * The bottom left corner of the indirect pointer input device we're interacting with, where (0, 0)
 * is the top left corner of the indirect pointer input device. Note: This is not related to the
 * screen coordinates.
 *
 * Note that `inputDeviceBottomLeft.y != inputDeviceHeight`, see [inputDeviceBottom].
 */
val IndirectPointerInjectionScope.inputDeviceBottomLeft: Offset
    get() = Offset(inputDeviceLeft, inputDeviceBottom)

/**
 * The center of the bottom edge of the indirect pointer input device we're interacting with, where
 * (0, 0) is the top left corner of the indirect pointer input device. Note: This is not related to
 * the screen coordinates.
 *
 * Note that `inputDeviceBottomCenter.y != inputDeviceHeight`, see [inputDeviceBottom].
 */
val IndirectPointerInjectionScope.inputDeviceBottomCenter: Offset
    get() = Offset(inputDeviceCenterX, inputDeviceBottom)

/**
 * The bottom right corner of the indirect pointer input device we're interacting with, where (0, 0)
 * is the top left corner of the indirect pointer input device. Note: This is not related to the
 * screen coordinates.
 *
 * Note that `inputDeviceBottomRight.x != inputDeviceWidth` and `inputDeviceBottomRight.y !=
 * inputDeviceHeight`, see [inputDeviceRight] and [inputDeviceBottom].
 */
val IndirectPointerInjectionScope.inputDeviceBottomRight: Offset
    get() = Offset(inputDeviceRight, inputDeviceBottom)

/**
 * Performs a click gesture (aka a tap) on nodes in the focus path.
 *
 * The click is done at the given [position] within the indirect pointer input device's bounds, or
 * the [inputDeviceCenter] if the [position] is omitted. The [position] is NOT in the node's local
 * coordinate system (see [IndirectPointerInjectionScope.inputDeviceSize]).
 *
 * @param position The position where to click, in the indirect pointer input device's coordinate
 *   system ([IndirectPointerInjectionScope.inputDeviceSize]). If omitted, the defaultStartLocation
 *   will be used.
 */
fun IndirectPointerInjectionScope.click(position: Offset = inputDeviceCenter) {
    down(position)
    move()
    up()
}

/**
 * Performs a long click gesture (aka a long press) on nodes in the focus path.
 *
 * The long click is done at the given [position] within the indirect pointer input device's bounds,
 * or [inputDeviceCenter] if the [position] is omitted. By default, the [durationMillis] of the
 * press is 100ms longer than the minimum required duration for a long press. The [position] is NOT
 * in the node's local coordinate system (see [IndirectPointerInjectionScope.inputDeviceSize]).
 *
 * @param position The position where to click, in the indirect pointer input device's coordinate
 *   system (see [IndirectPointerInjectionScope.inputDeviceSize]). If omitted, the
 *   defaultStartLocation will be used.
 * @param durationMillis The time between the down and the up event.
 */
fun IndirectPointerInjectionScope.longClick(
    position: Offset = inputDeviceCenter,
    durationMillis: Long = viewConfiguration.longPressTimeoutMillis + 100,
) {
    require(durationMillis >= viewConfiguration.longPressTimeoutMillis) {
        "Long click must have a duration of at least ${viewConfiguration.longPressTimeoutMillis}ms"
    }
    swipe(start = position, end = position, durationMillis = durationMillis)
}

// The average of min and max is a safe default
private val ViewConfiguration.defaultDoubleTapDelayMillis: Long
    get() = (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2

/**
 * Performs a double click gesture (aka a double tap) on nodes in the focus path.
 *
 * The double click is done at the given [position] within the indirect pointer input device's
 * bounds or [inputDeviceCenter] if the [position] is omitted. By default, the [delayMillis] between
 * the first and the second click is halfway in between the minimum and maximum required delay for a
 * double click. The [position] is NOT in the node's local coordinate system (see
 * [IndirectPointerInjectionScope.inputDeviceSize]).
 *
 * @param position The position where to click, in the indirect pointer input device's coordinate
 *   system ([IndirectPointerInjectionScope.inputDeviceSize]). If omitted, the defaultStartLocation
 *   will be used.
 * @param delayMillis The time between the up event of the first click and the down event of the
 *   second click
 */
fun IndirectPointerInjectionScope.doubleClick(
    position: Offset = inputDeviceCenter,
    delayMillis: Long = viewConfiguration.defaultDoubleTapDelayMillis,
) {
    require(delayMillis >= viewConfiguration.doubleTapMinTimeMillis) {
        "Time between clicks in double click must be at least " +
            "${viewConfiguration.doubleTapMinTimeMillis}ms"
    }
    require(delayMillis < viewConfiguration.doubleTapTimeoutMillis) {
        "Time between clicks in double click must be smaller than " +
            "${viewConfiguration.doubleTapTimeoutMillis}ms"
    }
    click(position)
    advanceEventTime(delayMillis)
    click(position)
}

/**
 * Performs a swipe gesture on nodes in the focus path.
 *
 * The motion events are linearly interpolated between [start] and [end].
 *
 * The coordinates are NOT in the node's local coordinate system (see
 * [IndirectPointerInjectionScope.inputDeviceSize]) and is usually used for focused movement (that
 * is, a focused node would move to the next/previous focusable node in the hierarchy with a swipe).
 *
 * @param start The position of the pointer starting the swipe gesture, in the indirect pointer
 *   input device's coordinate system ([IndirectPointerInjectionScope.inputDeviceSize]).
 * @param end The position of the pointer ending the swipe gesture, in the indirect pointer input
 *   device's coordinate system ([IndirectPointerInjectionScope.inputDeviceSize]).
 * @param durationMillis The duration of the swipe gesture (default duration is 200 milliseconds)
 */
fun IndirectPointerInjectionScope.swipe(start: Offset, end: Offset, durationMillis: Long = 200) {
    val durationFloat = durationMillis.toFloat()
    swipe(curve = { lerp(start, end, it / durationFloat) }, durationMillis = durationMillis)
}

/**
 * Performs a swipe gesture on nodes in the focus path.
 *
 * The swipe follows the [curve] from 0 till [durationMillis]. Will force sampling of an event at
 * all times defined in [keyTimes]. The time between events is kept as close to
 * [eventPeriodMillis][InjectionScope.eventPeriodMillis] as possible, given the constraints.
 *
 * The coordinates are NOT in the node's local coordinate system (see
 * [IndirectPointerInjectionScope.inputDeviceSize]) and is usually used for focused movement (that
 * is, a focused node would move to the next/previous focusable node in the hierarchy with a swipe).
 *
 * @param curve The function that describes the gesture. The argument passed to the function is the
 *   time in milliseconds since the start of the swipe, and the return value is the location of the
 *   pointer at that point in time.
 * @param durationMillis The duration of the gesture (default duration is 200 milliseconds)
 * @param keyTimes An optional list of timestamps in milliseconds at which a move event must be
 *   sampled
 */
@Suppress("PrimitiveInCollection")
fun IndirectPointerInjectionScope.swipe(
    curve: (timeMillis: Long) -> Offset,
    durationMillis: Long = 200,
    keyTimes: List<Long> = emptyList(),
) {
    multiTouchSwipe(curves = listOf(curve), durationMillis = durationMillis, keyTimes = keyTimes)
}

/**
 * Performs a multitouch swipe gesture on nodes in the focus path.
 *
 * Each pointer follows [curves] from 0 till [durationMillis]. Sampling of an event is forced at all
 * times defined in [keyTimes]. The time between events is kept as close to
 * [eventPeriodMillis][InjectionScope.eventPeriodMillis] as possible, given the constraints.
 *
 * The coordinates are NOT in the node's local coordinate system (see
 * [IndirectPointerInjectionScope.inputDeviceSize]) and is usually used for focused movement (that
 * is, a focused node would move to the next/previous focusable node in the hierarchy with a swipe).
 *
 * @param curves The functions that describe the gesture. Function _i_ defines the position over
 *   time for pointer id _i_. The argument passed to each function is the time in milliseconds since
 *   the start of the swipe, and the return value is the location of that pointer at that point in
 *   time.
 * @param durationMillis The duration of the gesture (default duration is 200 milliseconds)
 * @param keyTimes An optional list of timestamps in milliseconds at which a move event must be
 *   sampled
 */
@Suppress("PrimitiveInCollection")
fun IndirectPointerInjectionScope.multiTouchSwipe(
    curves: List<(timeMillis: Long) -> Offset>,
    durationMillis: Long = 200,
    keyTimes: List<Long> = emptyList(),
) {
    val startTime = 0L
    val endTime = durationMillis

    // Validate input
    require(durationMillis >= 1) { "duration must be at least 1 millisecond, not $durationMillis" }
    val validRange = startTime..endTime
    require(keyTimes.all { it in validRange }) {
        "keyTimes contains timestamps out of range [$startTime..$endTime]: $keyTimes"
    }
    require(keyTimes.asSequence().zipWithNext { a, b -> a <= b }.all { it }) {
        "keyTimes must be sorted: $keyTimes"
    }

    // Send down events
    curves.forEachIndexed { i, curve -> down(pointerId = i, position = curve(startTime)) }

    // Send move events between each consecutive pair in [t0, ..keyTimes, tN]
    var currTime = startTime
    var key = 0
    while (currTime < endTime) {
        // advance key
        while (key < keyTimes.size && keyTimes[key] <= currTime) {
            key++
        }
        // send events between t and next keyTime
        val tNext = if (key < keyTimes.size) keyTimes[key] else endTime
        sendMultiTouchSwipeSegment(curves, currTime, tNext)
        currTime = tNext
    }

    // And end with up events
    repeat(curves.size) { up(it) }
}

/**
 * Performs a pinch gesture on nodes in the focus path.
 *
 * For each pair of start and end [Offset]s, the motion events are linearly interpolated.
 *
 * The coordinates are NOT in the node's local coordinate system (see
 * [IndirectPointerInjectionScope.inputDeviceSize]).
 *
 * The default duration is 400 milliseconds.
 *
 * @param start0 The start position of the first pointer in the indirect pointer input device's
 *   coordinate system ([IndirectPointerInjectionScope.inputDeviceSize]).
 * @param end0 The end position of the first pointer in the indirect pointer input device's
 *   coordinate system ([IndirectPointerInjectionScope.inputDeviceSize]).
 * @param start1 The start position of the second pointer in the indirect pointer input device's
 *   coordinate system.
 * @param end1 The end position of the second pointer in the indirect pointer input device's
 *   coordinate system ([IndirectPointerInjectionScope.inputDeviceSize]).
 * @param durationMillis the duration of the pinch gesture
 */
fun IndirectPointerInjectionScope.pinch(
    start0: Offset,
    end0: Offset,
    start1: Offset,
    end1: Offset,
    durationMillis: Long = 400,
) {
    val durationFloat = durationMillis.toFloat()
    multiTouchSwipe(
        curves =
            listOf(
                { lerp(start0, end0, it / durationFloat) },
                { lerp(start1, end1, it / durationFloat) },
            ),
        durationMillis = durationMillis,
    )
}

/**
 * Performs a swipe gesture on nodes in the focus path such that it ends with the given
 * [endVelocity].
 *
 * The swipe will go through [start] at t=0 and through [end] at t=[durationMillis]. In between, the
 * swipe will go monotonically from [start] and [end], but not strictly. Due to imprecision, no
 * guarantees can be made for the actual velocity at the end of the gesture, but generally it is
 * within 0.1 of the desired velocity.
 *
 * When a swipe cannot be created that results in the desired velocity (because the input is too
 * restrictive), an exception will be thrown with suggestions to fix the input.
 *
 * The coordinates are NOT in the node's local coordinate system and is usually used for focused
 * movement (that is, a focused node would move to the next focusable node in the hierarchy with a
 * swipe).
 *
 * The default duration is calculated such that a feasible swipe can be created that ends in the
 * given velocity.
 *
 * @param start The position of the pointer starting the swipe gesture, in the indirect pointer
 *   input device's coordinate system.
 * @param end The position of the pointer ending the swipe gesture, in the indirect pointer input
 *   device's coordinate system.
 * @param endVelocity The velocity of the swipe gesture at the moment it ends in px/second. Must be
 *   positive.
 * @param durationMillis The duration of the swipe gesture in milliseconds. Must be long enough that
 *   at least 3 input events are generated, which happens with a duration of 40ms or more. If
 *   omitted, a duration is calculated such that a valid swipe with velocity can be created.
 * @throws IllegalArgumentException When no swipe can be generated that will result in the desired
 *   velocity. The error message will suggest changes to the input parameters such that a swipe will
 *   become feasible.
 */
fun IndirectPointerInjectionScope.swipeWithVelocity(
    start: Offset,
    end: Offset,
    @FloatRange(from = 0.0) endVelocity: Float,
    durationMillis: Long = VelocityPathFinder.calculateDefaultDuration(start, end, endVelocity),
) {
    require(endVelocity >= 0f) { "Velocity cannot be $endVelocity, it must be positive" }
    require(eventPeriodMillis < 40) {
        "InputDispatcher.eventPeriod must be smaller than 40ms in order to generate velocities"
    }
    val minimumDuration = ceil(2.5f * eventPeriodMillis).roundToLong()
    require(durationMillis >= minimumDuration) {
        "Duration must be at least ${minimumDuration}ms because " +
            "velocity requires at least 3 input events"
    }

    val pathFinder = VelocityPathFinder(start, end, endVelocity, durationMillis)
    val swipeFunction: (Long) -> Offset = { pathFinder.calculateOffsetForTime(it) }
    swipe(curve = swipeFunction, durationMillis = durationMillis)
}

/**
 * Performs a swipe up gesture on nodes in the focus path along `x =
 * [IndirectPointerInjectionScope.inputDeviceCenter].x`, from [startY] till [endY], taking
 * [durationMillis] milliseconds.
 *
 * @param startY The y-coordinate of the start of the swipe. Must be greater than or equal to the
 *   [endY].
 * @param endY The y-coordinate of the end of the swipe. Must be less than or equal to the [startY].
 * @param durationMillis The duration of the swipe. By default, 200 milliseconds.
 */
fun IndirectPointerInjectionScope.swipeUp(startY: Float, endY: Float, durationMillis: Long = 200) {
    require(startY >= endY) { "startY=$startY needs to be greater than or equal to endY=$endY" }
    val start = Offset(inputDeviceCenter.x, startY)
    val end = Offset(inputDeviceCenter.x, endY)
    swipe(start = start, end = end, durationMillis = durationMillis)
}

/**
 * Performs a swipe down gesture on nodes in the focus path along `x =
 * [IndirectPointerInjectionScope.inputDeviceCenter].x`, from [startY] till [endY], taking
 * [durationMillis] milliseconds.
 *
 * @param startY The y-coordinate of the start of the swipe. Must be less than or equal to the
 *   [endY].
 * @param endY The y-coordinate of the end of the swipe. Must be greater than or equal to the
 *   [startY].
 * @param durationMillis The duration of the swipe. By default, 200 milliseconds.
 */
fun IndirectPointerInjectionScope.swipeDown(
    startY: Float,
    endY: Float,
    durationMillis: Long = 200,
) {
    require(startY <= endY) { "startY=$startY needs to be less than or equal to endY=$endY" }
    val start = Offset(inputDeviceCenter.x, startY)
    val end = Offset(inputDeviceCenter.x, endY)
    swipe(start = start, end = end, durationMillis = durationMillis)
}

/**
 * Performs a swipe left gesture on nodes in the focus path along `y =
 * [IndirectPointerInjectionScope.inputDeviceCenter].y`, from [startX] till [endX], taking
 * [durationMillis] milliseconds.
 *
 * @param startX The x-coordinate of the start of the swipe. Must be greater than or equal to the
 *   [endX].
 * @param endX The x-coordinate of the end of the swipe. Must be less than or equal to the [startX].
 * @param durationMillis The duration of the swipe. By default, 200 milliseconds.
 */
fun IndirectPointerInjectionScope.swipeLeft(
    startX: Float,
    endX: Float,
    durationMillis: Long = 200,
) {
    require(startX >= endX) { "startX=$startX needs to be greater than or equal to endX=$endX" }
    val start = Offset(startX, inputDeviceCenter.y)
    val end = Offset(endX, inputDeviceCenter.y)
    swipe(start = start, end = end, durationMillis = durationMillis)
}

/**
 * Performs a swipe right gesture on nodes in the focus path along `y =
 * [IndirectPointerInjectionScope.inputDeviceCenter].y`, from [startX] till [endX], taking
 * [durationMillis] milliseconds.
 *
 * @param startX The x-coordinate of the start of the swipe. Must be less than or equal to the
 *   [endX].
 * @param endX The x-coordinate of the end of the swipe. Must be greater than or equal to the
 *   [startX].
 * @param durationMillis The duration of the swipe. By default, 200 milliseconds.
 */
fun IndirectPointerInjectionScope.swipeRight(
    startX: Float,
    endX: Float,
    durationMillis: Long = 200,
) {
    require(startX <= endX) { "startX=$startX needs to be less than or equal to endX=$endX" }
    val start = Offset(startX, inputDeviceCenter.y)
    val end = Offset(endX, inputDeviceCenter.y)
    swipe(start = start, end = end, durationMillis = durationMillis)
}

internal class IndirectPointerInjectionScopeImpl(
    private val baseScope: MultiModalInjectionScopeImpl,
    initialInputDeviceSize: IntSize,
    initialAxis: IndirectPointerEventPrimaryDirectionalMotionAxis,
) : IndirectPointerInjectionScope {

    override val density: Float
        get() = baseScope.density

    override val fontScale: Float
        get() = baseScope.fontScale

    override val eventPeriodMillis: Long
        get() = baseScope.eventPeriodMillis

    override val viewConfiguration: ViewConfiguration
        get() = baseScope.viewConfiguration

    override fun advanceEventTime(durationMillis: Long) = baseScope.advanceEventTime(durationMillis)

    override var inputDeviceSize: IntSize = initialInputDeviceSize
        internal set

    override var indirectPointerEventPrimaryDirectionalMotionAxis:
        IndirectPointerEventPrimaryDirectionalMotionAxis =
        initialAxis
        internal set

    private val inputDispatcher
        get() = baseScope.inputDispatcher

    private fun validatePosition(position: Offset) {
        require(
            position.x in 0f..inputDeviceSize.width.toFloat() &&
                position.y in 0f..inputDeviceSize.height.toFloat()
        ) {
            "Position $position is outside of the indirect pointer input device bounds $inputDeviceSize"
        }
    }

    internal fun updateConfiguration(
        size: IntSize,
        axis: IndirectPointerEventPrimaryDirectionalMotionAxis,
    ) {
        inputDeviceSize = size
        indirectPointerEventPrimaryDirectionalMotionAxis = axis
    }

    override fun currentPosition(pointerId: Int): Offset? {
        return inputDispatcher.getCurrentIndirectPointerPosition(pointerId)
    }

    override fun down(pointerId: Int, position: Offset) {
        validatePosition(position)
        inputDispatcher.enqueueIndirectPointerDown(
            pointerId,
            position,
            indirectPointerEventPrimaryDirectionalMotionAxis =
                indirectPointerEventPrimaryDirectionalMotionAxis,
        )
    }

    override fun updatePointerTo(pointerId: Int, position: Offset) {
        validatePosition(position)
        inputDispatcher.updateIndirectPointer(pointerId, position)
    }

    override fun move(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueIndirectPointerMove()
    }

    @ExperimentalTestApi
    @Suppress("PrimitiveInCollection")
    override fun moveWithHistoryMultiPointer(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>,
        delayMillis: Long,
    ) {
        repeat(relativeHistoricalTimes.size) {
            check(relativeHistoricalTimes[it] < 0) {
                "Relative historical times should be negative, in order to be in the past" +
                    "(offset $it was: ${relativeHistoricalTimes[it]})"
            }
            check(relativeHistoricalTimes[it] >= -delayMillis) {
                "Relative historical times should not be earlier than the previous event " +
                    "(offset $it was: ${relativeHistoricalTimes[it]}, ${-delayMillis})"
            }
        }

        historicalCoordinates.forEach { listOfPositions ->
            listOfPositions.forEach { position -> validatePosition(position) }
        }

        advanceEventTime(delayMillis)
        inputDispatcher.enqueueIndirectPointerMoves(relativeHistoricalTimes, historicalCoordinates)
    }

    override fun up(pointerId: Int) {
        inputDispatcher.enqueueIndirectPointerUp(pointerId)
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueIndirectPointerCancel()
    }
}

/**
 * Generates move events between `f([t0])` and `f([tN])` during the time window `(t0, tN]`, for each
 * `f` in [fs], following the curves defined by each `f`. The number of events sent (#numEvents) is
 * such that the time between each event is as close to
 * [eventPeriodMillis][InputDispatcher.eventPeriodMillis] as possible, but at least 1. The first
 * event is sent at time `downTime + (tN - t0) / #numEvents`, the last event is sent at time tN.
 *
 * @param fs The functions that define the coordinates of the respective gestures over time
 * @param t0 The start time of this segment of the swipe, in milliseconds relative to downTime
 * @param tN The end time of this segment of the swipe, in milliseconds relative to downTime
 */
private fun IndirectPointerInjectionScope.sendMultiTouchSwipeSegment(
    fs: List<(Long) -> Offset>,
    t0: Long,
    tN: Long,
) {
    var step = 0
    // How many steps will we take between t0 and tN? At least 1, and a number that will
    // bring it as close to eventPeriod as possible
    val steps = max(1, ((tN - t0) / eventPeriodMillis.toFloat()).roundToInt())

    var tPrev = t0
    while (step++ < steps) {
        val progress = step / steps.toFloat()
        val t = lerp(t0, tN, progress)
        fs.forEachIndexed { i, f -> updatePointerTo(i, f(t)) }
        move(t - tPrev)
        tPrev = t
    }
}
