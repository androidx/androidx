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
package androidx.ui.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.node.Owner
import androidx.compose.ui.unit.Duration
import androidx.compose.ui.unit.inMilliseconds
import androidx.compose.ui.unit.milliseconds

/**
 * Interface for dispatching full and partial gestures.
 *
 * Full gestures:
 * * [enqueueClick]
 * * [enqueueSwipe]
 * * [enqueueSwipes]
 *
 * Partial gestures:
 * * [enqueueDown]
 * * [enqueueMove]
 * * [enqueueUp]
 * * [enqueueCancel]
 * * [movePointer]
 * * [getCurrentPosition]
 *
 * Chaining methods:
 * * [enqueueDelay]
 */
internal interface InputDispatcher {
    companion object {
        /**
         * Whether or not injection of events should be suspended in between events until [now]
         * is at least the `eventTime` of the next event to inject. If `true`, will suspend until
         * the next `eventTime`, if `false`, will send the event immediately without suspending.
         */
        internal var dispatchInRealTime: Boolean = true

        /**
         * The minimum time between two successive injected MotionEvents, 10 milliseconds.
         * Ideally, the value should reflect a realistic pointer input sample rate, but that
         * depends on too many factors. Instead, the value is chosen comfortably below the
         * targeted frame rate (60 fps, equating to a 16ms period).
         */
        var eventPeriod = 10.milliseconds.inMilliseconds()
            internal set
    }

    /**
     * The current time, in the time scale used by gesture events.
     */
    val now: Long

    /**
     * Sends all enqueued events and blocks while they are dispatched. Will suspend before
     * dispatching an event until [now] is at least that event's timestamp. If an exception is
     * thrown during the process, all events that haven't yet been dispatched will be dropped.
     */
    fun sendAllSynchronous()

    fun saveState(owner: Owner?)
    /**
     * Called when this [InputDispatcher] is about to be discarded, from [GestureScope.dispose].
     */
    fun dispose()

    /**
     * Generates a click event at [position]. There will be 10ms in between the down and the up
     * event. The generated events are enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param position The coordinate of the click
     */
    fun enqueueClick(position: Offset)

    /**
     * Generates a swipe gesture from [start] to [end] with the given [duration]. The generated
     * events are enqueued in this [InputDispatcher] and will be sent when [sendAllSynchronous]
     * is called at the end of [performGesture].
     *
     * @param start The start position of the gesture
     * @param end The end position of the gesture
     * @param duration The duration of the gesture
     */
    fun enqueueSwipe(start: Offset, end: Offset, duration: Duration)

    /**
     * Generates a swipe gesture from [curve]&#40;0) to [curve]&#40;[duration]), following the
     * route defined by [curve]. Will force sampling of an event at all times defined in
     * [keyTimes]. The number of events sampled between the key times is implementation
     * dependent. The generated events are enqueued in this [InputDispatcher] and will be sent
     * when [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param curve The function that defines the position of the gesture over time
     * @param duration The duration of the gesture
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun enqueueSwipe(
        curve: (Long) -> Offset,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    /**
     * Generates [curves].size simultaneous swipe gestures, each swipe going from
     * [curves]&#91;i&#93;(0) to [curves]&#91;i&#93;([duration]), following the route defined by
     * [curves]&#91;i&#93;. Will force sampling of an event at all times defined in [keyTimes].
     * The number of events sampled between the key times is implementation dependent. The
     * generated events are enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture].
     *
     * @param curves The functions that define the position of the gesture over time
     * @param duration The duration of the gestures
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun enqueueSwipes(
        curves: List<(Long) -> Offset>,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    /**
     * Adds a delay between the end of the last full or current partial gesture of the given
     * [duration]. Guarantees that the first event time of the next gesture will be exactly
     * [duration] later then if that gesture would be injected without this delay, provided that
     * the next gesture is started using the same [InputDispatcher] instance as the one used to
     * end the last gesture.
     *
     * Note: this does not affect the time of the next event for the _current_ partial gesture,
     * using [enqueueMove], [enqueueUp] and [enqueueCancel], but it will affect the time of the
     * _next_ gesture (including partial gestures started with [enqueueDown]).
     *
     * @param duration The duration of the delay. Must be positive
     */
    fun enqueueDelay(duration: Duration)

    /**
     * Generates a down event at [position] for the pointer with the given [pointerId], starting
     * a new partial gesture. A partial gesture can only be started if none was currently ongoing
     * for that pointer. Pointer ids may be reused during the same gesture. The generated event
     * is enqueued in this [InputDispatcher] and will be sent when [sendAllSynchronous] is called
     * at the end of [performGesture].
     *
     * It is possible to mix partial gestures with full gestures (e.g. generate a [click]
     * [enqueueClick] during a partial gesture), as long as you make sure that the default
     * pointer id (id=0) is free to be used by the full gesture.
     *
     * A full gesture starts with a down event at some position (with this method) that indicates
     * a finger has started touching the screen, followed by zero or more [down][enqueueDown],
     * [move][enqueueMove] and [up][enqueueUp] events that respectively indicate that another
     * finger started touching the screen, a finger moved around or a finger was lifted up from
     * the screen. A gesture is finished when [up][enqueueUp] lifts the last remaining finger
     * from the screen, or when a single [cancel][enqueueCancel] event is generated.
     *
     * Partial gestures don't have to be defined all in the same [performGesture] block, but
     * keep in mind that while the gesture is not complete, all code you execute in between
     * blocks that progress the gesture, will be executed while imaginary fingers are actively
     * touching the screen. All events generated during a single [performGesture] block are sent
     * together at the end of that block.
     *
     * In the context of testing, it is not necessary to complete a gesture with an up or cancel
     * event, if the test ends before it expects the finger to be lifted from the screen.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     *
     * @see movePointer
     * @see enqueueMove
     * @see enqueueUp
     * @see enqueueCancel
     */
    fun enqueueDown(pointerId: Int, position: Offset)

    /**
     * Generates an up event for the given [pointerId] at the current position of that pointer,
     * [delay] milliseconds after the previous injected event of this gesture. The default
     * [delay] is 0 milliseconds. The generated event is enqueued in this [InputDispatcher] and
     * will be sent when [sendAllSynchronous] is called at the end of [performGesture]. See
     * [enqueueDown] for more information on how to make complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [enqueueDown]
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. 0 milliseconds by default.
     *
     * @see enqueueDown
     * @see movePointer
     * @see enqueueMove
     * @see enqueueCancel
     */
    fun enqueueUp(pointerId: Int, delay: Long = 0)

    /**
     * Generates a cancel event [delay] milliseconds after the previous injected event of this
     * gesture. The default [delay] is [10 milliseconds][InputDispatcher.eventPeriod]. The
     * generated event is enqueued in this [InputDispatcher] and will be sent when
     * [sendAllSynchronous] is called at the end of [performGesture]. See [enqueueDown] for more
     * information on how to make complete gestures from partial gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the cancel
     * event. [10 milliseconds][InputDispatcher.eventPeriod] by default.
     *
     * @see enqueueDown
     * @see movePointer
     * @see enqueueMove
     * @see enqueueUp
     */
    fun enqueueCancel(delay: Long = eventPeriod)

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position],
     * but does not generate a move event. Use this to move multiple pointers simultaneously. To
     * generate the next move event, which will contain the current position of _all_ pointers
     * (not just the moved ones), call [enqueueMove] without arguments. If you move one or more
     * pointers and then call [enqueueDown] or [enqueueUp], without calling [enqueueMove] first,
     * a move event will be generated right before that down or up event. See [enqueueDown] for
     * more information on how to make complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to move, as supplied in [enqueueDown]
     * @param position The position to move the pointer to
     *
     * @see enqueueDown
     * @see enqueueMove
     * @see enqueueUp
     * @see enqueueCancel
     */
    fun movePointer(pointerId: Int, position: Offset)

    /**
     * Generates a move event [delay] milliseconds after the previous injected event of this
     * gesture, without moving any of the pointers. The default [delay] is [10 milliseconds]
     * [eventPeriod]. Use this to commit all changes in pointer location made
     * with [movePointer]. The generated event will contain the current position of all pointers.
     * It is enqueued in this [InputDispatcher] and will be sent when [sendAllSynchronous] is
     * called at the end of [performGesture]. See [enqueueDown] for more information on how to
     * make complete gestures from partial gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. [10 milliseconds][eventPeriod] by default.
     */
    fun enqueueMove(delay: Long = eventPeriod)

    /**
     * During a partial gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no partial gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     * pointer is not currently in use
     */
    fun getCurrentPosition(pointerId: Int): Offset?
}

internal expect fun InputDispatcher(owner: Owner): InputDispatcher
