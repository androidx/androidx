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

import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.lerp

internal interface InputDispatcher {
    /**
     * Sends a click event at [position]. There will be 10ms in between the down and the up
     * event. This method blocks until all input events have been dispatched.
     *
     * @param position The coordinate of the click
     */
    fun sendClick(position: PxPosition)

    /**
     * Sends a swipe gesture from [start] to [end] with the given [duration]. This method blocks
     * until all input events have been dispatched.
     *
     * @param start The start position of the gesture
     * @param end The end position of the gesture
     * @param duration The duration of the gesture
     */
    fun sendSwipe(start: PxPosition, end: PxPosition, duration: Duration) {
        val durationFloat = duration.inMilliseconds().toFloat()
        sendSwipe(
            curve = { lerp(start, end, it / durationFloat) },
            duration = duration
        )
    }

    /**
     * Sends a swipe gesture from `curve(0)` to `curve([duration])`, following the route defined
     * by [curve]. Will force sampling of an event at all times defined in [keyTimes]. The number
     * of events sampled between the key times is implementation dependent. This method blocks
     * until all input events have been dispatched.
     *
     * @param curve The function that defines the position of the gesture over time
     * @param duration The duration of the gesture
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun sendSwipe(
        curve: (Long) -> PxPosition,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    ) {
        sendSwipes(listOf(curve), duration, keyTimes)
    }

    /**
     * Sends swipe gestures from `curve(0)` to `curve([duration])`, following the route defined
     * by [curves]. Will force sampling of an event at all times defined in [keyTimes]. The number
     * of events sampled between the key times is implementation dependent. This method blocks
     * until all input events have been dispatched.
     *
     * @param curves The functions that define the position of the gesture over time
     * @param duration The duration of the gestures
     * @param keyTimes An optional list of timestamps in milliseconds at which a move event must
     * be sampled
     */
    fun sendSwipes(
        curves: List<(Long) -> PxPosition>,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    /**
     * Blocks for the given [duration] in order to delay the next gesture. Guarantees that the
     * first event time of the next gesture will be exactly [duration] later then if that gesture
     * would be injected without this delay.
     *
     * Note: this does not affect the time of the next event for the _current_ partial gesture,
     * using [sendMove], [sendUp] and [sendCancel], but it will affect the time of the _next_
     * gesture (including partial gestures started with [sendDown]).
     *
     * @param duration The duration of the delay. Must be positive
     */
    fun delay(duration: Duration)

    /**
     * Sends a down event at [position] and returns a [token][GestureToken] to send subsequent
     * touch events to continue this gesture. This method blocks until the input event has been
     * dispatched.
     *
     * A full gesture starts with a down event at some position (this method) that indicates a
     * finger has started touching the screen, followed by zero or more [move][sendMove] events
     * that indicate the finger has moved around along those positions, and is finished by an
     * [up][sendUp] or a [cancel][sendCancel] event that indicate the finger was lifted up from
     * the screen. As long as the gesture is incomplete, keep in mind that an imaginary finger is
     * actively touching the screen.
     *
     * In the context of testing, it is not necessary to complete a gesture with an up or cancel
     * event, if the test ends before it expects the finger to be lifted from the screen.
     *
     * @param position The coordinate of the down event
     * @return A [token][GestureToken] that must be passed to all subsequent events that are part
     * of the gesture started by this method.
     *
     * @see sendMove
     * @see sendUp
     * @see sendCancel
     */
    fun sendDown(position: PxPosition): GestureToken

    /**
     * Sends a move event at [position], 10 milliseconds after the previous injected event of
     * this gesture. This method blocks until the input event has been dispatched. See [sendDown]
     * for more information on how to make complete gestures from partial gestures.
     *
     * @param token The token returned from the corresponding [down event][sendDown] that started
     * this gesture.
     * @param position The coordinate of the move event
     *
     * @see sendDown
     * @see sendUp
     * @see sendCancel
     */
    fun sendMove(token: GestureToken, position: PxPosition)

    /**
     * Sends an up event at [position], 10 milliseconds after the previous injected event of this
     * gesture. This method blocks until the input event has been dispatched. See [sendDown] for
     * more information on how to make complete gestures from partial gestures.
     *
     * @param token The token returned from the corresponding [down event][sendDown] that started
     * this gesture.
     * @param position The coordinate of the up event
     *
     * @see sendDown
     * @see sendMove
     * @see sendCancel
     */
    fun sendUp(token: GestureToken, position: PxPosition)

    /**
     * Sends a cancel event at [position], 10 milliseconds after the previous injected event of
     * this gesture. This method blocks until the input event has been dispatched. See [sendDown]
     * for more information on how to make complete gestures from partial gestures.
     *
     * @param token The token returned from the corresponding [down event][sendDown] that started
     * this gesture.
     * @param position The coordinate of the cancel event
     *
     * @see sendDown
     * @see sendMove
     * @see sendUp
     */
    fun sendCancel(token: GestureToken, position: PxPosition)

    // TODO(b/145593752): how to solve multi-touch?
}
