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

package androidx.compose.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.util.lerp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The receiver scope of the trackpad input injection lambda from [performTrackpadInput].
 *
 * The functions in [TrackpadInjectionScope] can roughly be divided into two groups: full gestures
 * and individual trackpad events. The individual trackpad events are: [press], [moveTo] and
 * friends, [release], [cancel], [scroll] and [advanceEventTime]. Full gestures are all the other
 * functions, like [TrackpadInjectionScope.click], [TrackpadInjectionScope.doubleClick],
 * [TrackpadInjectionScope.animateMoveTo], etc. These are built on top of the individual events and
 * serve as a good example on how you can build your own full gesture functions.
 *
 * A trackpad move event can be sent with [moveTo] and [moveBy]. The trackpad position can be
 * updated with [updatePointerTo] and [updatePointerBy], which will not send an event and only
 * update the position internally. This can be useful if you want to send an event that is not a
 * move event with a location other then the current location, but without sending a preceding move
 * event. Use [press] and [release] to send button pressed and button released events. This will
 * also send all other necessary events that keep the stream of trackpad events consistent with
 * actual trackpad input, such as a hover exit event. A [cancel] event can be sent at any time when
 * at least one button is pressed. Use [scroll] to send a trackpad scroll event.
 *
 * The entire event injection state is shared between all `perform.*Input` methods, meaning you can
 * continue an unfinished trackpad gesture in a subsequent invocation of [performTrackpadInput] or
 * [performMultiModalInput]. Note however that while the trackpad's position is retained across
 * invocation of `perform.*Input` methods, it is always manipulated in the current node's local
 * coordinate system. That means that two subsequent invocations of [performTrackpadInput] on
 * different nodes will report a different [currentPosition], even though it is actually the same
 * position on the screen.
 *
 * All events sent by these methods are batched together and sent as a whole after
 * [performTrackpadInput] has executed its code block.
 *
 * Example of performing a trackpad click:
 *
 * @sample androidx.compose.ui.test.samples.trackpadInputClick
 * @see InjectionScope
 */
@Suppress("NotCloseable")
interface TrackpadInjectionScope : InjectionScope {
    /**
     * Returns the current position of the cursor. The position is returned in the local coordinate
     * system of the node with which we're interacting. (0, 0) is the top left corner of the node.
     * If none of the move or updatePointer methods have been used yet, the trackpad's position will
     * be (0, 0) in the Compose host's coordinate system, which will be `-[topLeft]` in the node's
     * local coordinate system.
     */
    val currentPosition: Offset

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with the
     * position of the trackpad updated to [position]. The [position] is in the node's local
     * coordinate system, where (0, 0) is the top left corner of the node.
     *
     * If no mouse buttons are pressed, a hover event will be sent instead of a move event. If the
     * trackpad wasn't hovering yet, a hover enter event is sent as well.
     *
     * @param position The new position of the trackpad, in the node's local coordinate system
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveTo(position: Offset, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a move event [delayMillis] after the last sent event on the associated node, with the
     * position of the trackpad moved by the given [delta].
     *
     * If no mouse buttons are pressed, a hover event will be sent instead of a move event. If the
     * trackpad wasn't hovering yet, a hover enter event is sent as well.
     *
     * @param delta The position for this move event, relative to the current position of the
     *   trackpad. For example, `delta = Offset(10.px, -10.px) will add 10.px to the trackpad's
     *   x-position, and subtract 10.px from the trackpad's y-position.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun moveBy(delta: Offset, delayMillis: Long = eventPeriodMillis) {
        moveTo(currentPosition + delta, delayMillis)
    }

    /**
     * Updates the position of the trackpad to the given [position], but does not send a move or
     * hover event. This can be useful to adjust the trackpad position before sending for example a
     * [press] event. The [position] is in the node's local coordinate system, where (0.px, 0.px) is
     * the top left corner of the node.
     *
     * @param position The new position of the trackpad, in the node's local coordinate system
     */
    fun updatePointerTo(position: Offset)

    /**
     * Updates the position of the trackpad by the given [delta], but does not send a move or hover
     * event. This can be useful to adjust the trackpad position before sending for example a
     * [press] event.
     *
     * @param delta The position for this move event, relative to the current position of the
     *   trackpad. For example, `delta = Offset(10.px, -10.px) will add 10.px to the trackpad's
     *   x-position, and subtract 10.px from the trackpad's y-position.
     */
    fun updatePointerBy(delta: Offset) {
        updatePointerTo(currentPosition + delta)
    }

    /**
     * Sends a down and button pressed event for the given [button] on the associated node. When no
     * buttons were down yet, this will exit hovering mode before the button is pressed. All events
     * will be sent at the current event time. Trackpads behave similarly to mice, with platform
     * interpreted gestures that send mouse button events, so this API takes the [MouseButton] to
     * press.
     *
     * Throws an [IllegalStateException] if the [button] is already pressed.
     *
     * @param button The mouse button that is pressed. By default the primary mouse button.
     */
    fun press(button: MouseButton = MouseButton.Primary)

    /**
     * Sends a button released and up event for the given [button] on the associated node. If this
     * was the last button to be released, the trackpad will enter hovering mode and send an
     * accompanying trackpad move event after the button has been released. All events will be sent
     * at the current event time. Trackpads behave similarly to mice, with platform interpreted
     * gestures that send mouse button events, so this API takes the [MouseButton] to release.
     *
     * Throws an [IllegalStateException] if the [button] is not pressed.
     *
     * @param button The mouse button that is released. By default the primary mouse button.
     */
    fun release(button: MouseButton = MouseButton.Primary)

    /**
     * Sends a cancel event [delayMillis] after the last sent event to cancel a stream of trackpad
     * events with pressed mouse buttons. All buttons will be released as a result. A trackpad
     * cancel event can only be sent when mouse buttons are pressed.
     *
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun cancel(delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a hover enter event at the given [position], [delayMillis] after the last sent event,
     * without sending a hover move event.
     *
     * An [IllegalStateException] will be thrown when mouse buttons are down, or if the trackpad is
     * already hovering.
     *
     * The [position] is in the node's local coordinate system, where (0, 0) is the top left corner
     * of the node.
     *
     * __Note__: enter and exit events are already sent as a side effect of [movement][moveTo] when
     * necessary. Whether or not this is part of the contract of trackpad events is platform
     * dependent, so it is highly discouraged to manually send enter or exit events. Only use this
     * method for tests that need to make assertions about a component's state _in between_ the
     * enter/exit and move event.
     *
     * @param position The new position of the trackpad, in the node's local coordinate system.
     *   [currentPosition] by default.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun enter(position: Offset = currentPosition, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a hover exit event at the given [position], [delayMillis] after the last sent event,
     * without sending a hover move event.
     *
     * An [IllegalStateException] will be thrown if the trackpad was not hovering.
     *
     * The [position] is in the node's local coordinate system, where (0, 0) is the top left corner
     * of the node.
     *
     * __Note__: enter and exit events are already sent as a side effect of [movement][moveTo] when
     * necessary. Whether or not this is part of the contract of trackpad events is platform
     * dependent, so it is highly discouraged to manually send enter or exit events. Only use this
     * method for tests that need to make assertions about a component's state _in between_ the
     * enter/exit and move event.
     *
     * @param position The new position of the trackpad, in the node's local coordinate system
     *   [currentPosition] by default.
     * @param delayMillis The time between the last sent event and this event. [eventPeriodMillis]
     *   by default.
     */
    fun exit(position: Offset = currentPosition, delayMillis: Long = eventPeriodMillis)

    /**
     * Sends a scroll event with the given [offset]. The event will be sent at the current event
     * time.
     *
     * @sample androidx.compose.ui.test.samples.trackpadInputScroll
     * @param offset The amount of scroll
     */
    fun scroll(offset: Offset)

    /**
     * Sends a pinch event with the given [scaleFactor]. The event will be sent at the current event
     * time.
     *
     * The [scaleFactor] is a multiplicative zoom factor. A [scaleFactor] of 1 represents no pinch
     * movement. A [scaleFactor] less than 1 represents a pinch where the 2 fingers become closer
     * together (often interpreted as a "zoom out" gesture), and a [scaleFactor] of more than 1
     * represents a pinch where the 2 fingers become farther apart (often interpreted as a "zoom in"
     * gesture).
     *
     * @sample androidx.compose.ui.test.samples.trackpadInputPinch
     * @param scaleFactor The amount of pinch.
     */
    fun pinch(scaleFactor: Float)
}

internal class TrackpadInjectionScopeImpl(private val baseScope: MultiModalInjectionScopeImpl) :
    TrackpadInjectionScope, InjectionScope by baseScope {
    private val inputDispatcher
        get() = baseScope.inputDispatcher

    private fun localToRoot(position: Offset) = baseScope.localToRoot(position)

    override val currentPosition: Offset
        get() = baseScope.rootToLocal(inputDispatcher.currentCursorPosition)

    override fun moveTo(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueTrackpadMove(positionInRoot)
    }

    override fun updatePointerTo(position: Offset) {
        val positionInRoot = localToRoot(position)
        inputDispatcher.updateTrackpadPosition(positionInRoot)
    }

    override fun press(button: MouseButton) {
        inputDispatcher.enqueueTrackpadPress(button.buttonId)
    }

    override fun release(button: MouseButton) {
        inputDispatcher.enqueueTrackpadRelease(button.buttonId)
    }

    override fun enter(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueTrackpadEnter(positionInRoot)
    }

    override fun exit(position: Offset, delayMillis: Long) {
        advanceEventTime(delayMillis)
        val positionInRoot = localToRoot(position)
        inputDispatcher.enqueueTrackpadExit(positionInRoot)
    }

    override fun cancel(delayMillis: Long) {
        advanceEventTime(delayMillis)
        inputDispatcher.enqueueTrackpadCancel()
    }

    override fun scroll(offset: Offset) {
        inputDispatcher.enqueueTrackpadScroll(offset)
    }

    override fun pinch(scaleFactor: Float) {
        inputDispatcher.enqueueTrackpadPinch(scaleFactor)
    }
}

/**
 * Use [button] to click on [position], or on the current cursor position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system, where
 * (0, 0) is the top left corner of the node. The default [button] is the
 * [primary][MouseButton.Primary] button.
 *
 * @param position The position where to click, in the node's local coordinate system. If omitted,
 *   the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks on the
 *   current trackpad position.
 * @param button The button to click with. Uses the [primary][MouseButton.Primary] by default.
 */
fun TrackpadInjectionScope.click(
    position: Offset = center,
    button: MouseButton = MouseButton.Primary,
) {
    if (position.isSpecified) {
        updatePointerTo(position)
    }
    press(button)
    release(button)
}

/**
 * Secondary-click on [position], or on the current cursor position if [position] is
 * [unspecified][Offset.Unspecified]. While the secondary mouse button is not necessarily the right
 * mouse button (e.g. on left-handed mice), this method is still called `rightClick` for it's
 * widespread use. The [position] is in the node's local coordinate system, where (0, 0) is the top
 * left corner of the node.
 *
 * @param position The position where to click, in the node's local coordinate system. If omitted,
 *   the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks on the
 *   current trackpad position.
 */
fun TrackpadInjectionScope.rightClick(position: Offset = center) =
    click(position, MouseButton.Secondary)

// The average of min and max is a safe default
private val ViewConfiguration.defaultDoubleTapDelayMillis: Long
    get() = (doubleTapMinTimeMillis + doubleTapTimeoutMillis) / 2

/**
 * Use [button] to double-click on [position], or on the current trackpad position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system, where
 * (0, 0) is the top left corner of the node. The default [button] is the
 * [primary][MouseButton.Primary] button.
 *
 * @param position The position where to click, in the node's local coordinate system. If omitted,
 *   the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks on the
 *   current trackpad position.
 * @param button The button to click with. Uses the [primary][MouseButton.Primary] by default.
 */
fun TrackpadInjectionScope.doubleClick(
    position: Offset = center,
    button: MouseButton = MouseButton.Primary,
) {
    click(position, button)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position, button)
}

/**
 * Use [button] to triple-click on [position], or on the current trackpad position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system, where
 * (0, 0) is the top left corner of the node. The default [button] is the
 * [primary][MouseButton.Primary] button.
 *
 * @param position The position where to click, in the node's local coordinate system. If omitted,
 *   the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks on the
 *   current trackpad position.
 * @param button The button to click with. Uses the [primary][MouseButton.Primary] by default.
 */
fun TrackpadInjectionScope.tripleClick(
    position: Offset = center,
    button: MouseButton = MouseButton.Primary,
) {
    click(position, button)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position, button)
    advanceEventTime(viewConfiguration.defaultDoubleTapDelayMillis)
    click(position, button)
}

/**
 * Use [button] to long-click on [position], or on the current trackpad position if [position] is
 * [unspecified][Offset.Unspecified]. The [position] is in the node's local coordinate system, where
 * (0, 0) is the top left corner of the node. The default [button] is the
 * [primary][MouseButton.Primary] button.
 *
 * @param position The position where to click, in the node's local coordinate system. If omitted,
 *   the [center] of the node will be used. If [unspecified][Offset.Unspecified], clicks on the
 *   current trackpad position.
 * @param button The button to click with. Uses the [primary][MouseButton.Primary] by default.
 */
fun TrackpadInjectionScope.longClick(
    position: Offset = center,
    button: MouseButton = MouseButton.Primary,
) {
    if (position.isSpecified) {
        updatePointerTo(position)
    }
    press(button)
    advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100L)
    release(button)
}

/**
 * Move the trackpad from the [current position][TrackpadInjectionScope.currentPosition] to the
 * given [position], sending a stream of move events to get an animated path of [durationMillis]
 * milliseconds. [Move][moveTo] the trackpad to the desired start position if you want to start from
 * a different position. The [position] is in the node's local coordinate system, where (0, 0) is
 * the top left corner of the node.
 *
 * Example of moving the trackpad along a line:
 *
 * @sample androidx.compose.ui.test.samples.trackpadInputAnimateMoveTo
 * @param position The position where to move the trackpad to, in the node's local coordinate system
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
fun TrackpadInjectionScope.animateMoveTo(
    position: Offset,
    durationMillis: Long = DefaultTrackpadGestureDurationMillis,
) {
    val durationFloat = durationMillis.toFloat()
    val start = currentPosition
    animateMoveAlong(
        curve = { lerp(start, position, it / durationFloat) },
        durationMillis = durationMillis,
    )
}

/**
 * Move the trackpad from the [current position][TrackpadInjectionScope.currentPosition] by the
 * given [delta], sending a stream of move events to get an animated path of [durationMillis]
 * milliseconds.
 *
 * @param delta The position where to move the trackpad to, relative to the current position of the
 *   trackpad. For example, `delta = Offset(100.px, -100.px) will move the trackpad 100 pixels to
 *   the right and 100 pixels upwards.
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
fun TrackpadInjectionScope.animateMoveBy(
    delta: Offset,
    durationMillis: Long = DefaultTrackpadGestureDurationMillis,
) {
    animateMoveTo(currentPosition + delta, durationMillis)
}

/**
 * Move the trackpad along the given [curve], sending a stream of move events to get an animated
 * path of [durationMillis] milliseconds. The trackpad will initially be moved to the start of the
 * path, `curve(0)`, if it is not already there. The positions defined by the [curve] are in the
 * node's local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * Example of moving the trackpad along a curve:
 *
 * @sample androidx.compose.ui.test.samples.trackpadInputAnimateMoveAlong
 * @param curve The function that defines the position of the trackpad over time for this gesture,
 *   in the node's local coordinate system. The argument passed to the function is the time in
 *   milliseconds since the start of the animated move, and the return value is the location of the
 *   trackpad at that point in time
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
fun TrackpadInjectionScope.animateMoveAlong(
    curve: (timeMillis: Long) -> Offset,
    durationMillis: Long = DefaultTrackpadGestureDurationMillis,
) {
    require(durationMillis > 0) { "Duration is 0" }
    val start = curve(0)
    if (start != currentPosition) {
        // Instantly move to the start position to maintain the total durationMillis
        moveTo(curve(0), delayMillis = 0)
    }

    var step = 0
    // How many steps will we take in durationMillis?
    // At least 1, and a number that will bring as as close to eventPeriod as possible
    val steps = max(1, (durationMillis / eventPeriodMillis.toFloat()).roundToInt())

    var tPrev = 0L
    while (step++ < steps) {
        val progress = step / steps.toFloat()
        val t = lerp(0, durationMillis, progress)
        moveTo(curve(t), delayMillis = t - tPrev)
        tPrev = t
    }
}

/**
 * Use [button] to drag and drop something from [start] to [end] in [durationMillis] milliseconds.
 * The trackpad position is [updated][TrackpadInjectionScope.updatePointerTo] to the start position
 * before starting the gesture. The positions defined by the [start] and [end] are in the node's
 * local coordinate system, where (0, 0) is the top left corner of the node.
 *
 * @param start The position where to press the primary mouse button and initiate the drag, in the
 *   node's local coordinate system.
 * @param end The position where to release the primary mouse button and end the drag, in the node's
 *   local coordinate system.
 * @param button The button to drag with. Uses the [primary][MouseButton.Primary] by default.
 * @param durationMillis The duration of the gesture. By default 300 milliseconds.
 */
fun TrackpadInjectionScope.dragAndDrop(
    start: Offset,
    end: Offset,
    button: MouseButton = MouseButton.Primary,
    durationMillis: Long = DefaultTrackpadGestureDurationMillis,
) {
    updatePointerTo(start)
    press(button)
    animateMoveTo(end, durationMillis)
    release(button)
}

/** The default duration of trackpad gestures with configurable time (e.g. [animateMoveTo]). */
private const val DefaultTrackpadGestureDurationMillis: Long = 300L
