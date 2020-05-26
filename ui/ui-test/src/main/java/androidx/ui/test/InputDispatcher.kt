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

import androidx.ui.core.AndroidOwner
import androidx.ui.core.Owner
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.android.AndroidOwnerRegistry
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.lerp
import java.util.WeakHashMap

/**
 * Interface for dispatching full and partial gestures.
 *
 * Full gestures:
 * * [sendClick]
 * * [sendSwipe]
 * * [sendSwipes]
 *
 * Partial gestures:
 * * [sendDown]
 * * [sendMove]
 * * [sendUp]
 * * [sendCancel]
 * * [currentPosition]
 *
 * Chaining methods:
 * * [delay]
 */
internal interface InputDispatcher {
    companion object : AndroidOwnerRegistry.OnRegistrationChangedListener {
        private val states = WeakHashMap<Owner, InputDispatcherState>()

        init {
            AndroidOwnerRegistry.addOnRegistrationChangedListener(this)
        }

        internal fun createInstance(owner: Owner): InputDispatcher {
            require(owner is AndroidOwner) {
                "InputDispatcher currently only supports dispatching to AndroidOwner, not to " +
                        owner::class.java.simpleName
            }
            val view = owner.view
            return AndroidInputDispatcher { view.dispatchTouchEvent(it) }.apply {
                states[owner]?.also {
                    restoreInstanceState(it)
                    states.remove(owner)
                }
            }
        }

        internal fun saveInstanceState(owner: Owner?, state: InputDispatcherState) {
            // Owner may have been removed already
            if (owner != null && AndroidOwnerRegistry.getUnfilteredOwners().contains(owner)) {
                states[owner] = state
            }
        }

        override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
            if (!registered) {
                states.remove(owner)
            }
        }
    }

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
     * Sends a swipe gesture from [curve]&#40;0) to [curve]&#40;[duration]), following the route
     * defined by [curve]. Will force sampling of an event at all times defined in [keyTimes].
     * The number of events sampled between the key times is implementation dependent. This
     * method blocks until all input events have been dispatched.
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
     * Sends [curves].size simultaneous swipe gestures, each swipe going from
     * [curves]&#91;i&#93;(0) to [curves]&#91;i&#93;([duration]), following the route defined by
     * [curves]&#91;i&#93;. Will force sampling of an event at all times defined in [keyTimes].
     * The number of events sampled between the key times is implementation dependent. This
     * method blocks until all input events have been dispatched.
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
     * would be injected without this delay, provided that the next gesture is started using the
     * same [InputDispatcher] instance as the one used to end the last gesture.
     *
     * Note: this does not affect the time of the next event for the _current_ partial gesture,
     * using [sendMove], [sendUp] and [sendCancel], but it will affect the time of the _next_
     * gesture (including partial gestures started with [sendDown]).
     *
     * @param duration The duration of the delay. Must be positive
     */
    fun delay(duration: Duration)

    /**
     * During a partial gesture, returns the position of the last touch event. Returns `null` if
     * no partial gesture is in progress.
     */
    val currentPosition: PxPosition?

    /**
     * Sends a down event at [position], starting a new partial gesture. A partial gesture can
     * only be started if none was currently ongoing. This method blocks until the input event
     * has been dispatched.
     *
     * A full gesture starts with a down event at some position (with this method) that indicates
     * a finger has started touching the screen, followed by zero or more [move][sendMove] events
     * that indicate the finger has moved around along those positions, and is finished by an
     * [up][sendUp] or a [cancel][sendCancel] event that indicate the finger was lifted up from
     * the screen.
     *
     * Partial gestures don't have to be defined all in the same [doPartialGesture] block, but
     * keep in mind that while the gesture is not complete, all code you execute in between
     * blocks that progress the gesture, will be executed while an imaginary finger is actively
     * touching the screen.
     *
     * In the context of testing, it is not necessary to complete a gesture with an up or cancel
     * event, if the test ends before it expects the finger to be lifted from the screen.
     *
     * @param position The coordinate of the down event
     *
     * @see sendMove
     * @see sendUp
     * @see sendCancel
     */
    fun sendDown(position: PxPosition)

    /**
     * Sends a move event at [position], 10 milliseconds after the previous injected event of
     * this gesture. This method blocks until the input event has been dispatched. See [sendDown]
     * for more information on how to make complete gestures from partial gestures.
     *
     * @param position The coordinate of the move event
     *
     * @see sendDown
     * @see sendUp
     * @see sendCancel
     */
    fun sendMove(position: PxPosition)

    /**
     * Sends an up event at [position], 10 milliseconds after the previous injected event of this
     * gesture. This method blocks until the input event has been dispatched. See [sendDown] for
     * more information on how to make complete gestures from partial gestures.
     *
     * @param position The coordinate of the up event
     *
     * @see sendDown
     * @see sendMove
     * @see sendCancel
     */
    fun sendUp(position: PxPosition?)

    /**
     * Sends a cancel event at [position], 10 milliseconds after the previous injected event of
     * this gesture. This method blocks until the input event has been dispatched. See [sendDown]
     * for more information on how to make complete gestures from partial gestures.
     *
     * @param position The coordinate of the cancel event
     *
     * @see sendDown
     * @see sendMove
     * @see sendUp
     */
    fun sendCancel(position: PxPosition?)

    /**
     * Returns the state of this input dispatcher, in case a partial gesture is in progress.
     */
    fun saveInstanceState(): InputDispatcherState

    /**
     * Restores the state of this input dispatcher, in case a partial gesture was in progress. If
     * a partial gesture was not in progress, no state is restored.
     */
    fun restoreInstanceState(state: InputDispatcherState)

    // TODO(b/145593752): how to solve multi-touch?
}

/**
 * The state of an [InputDispatcher], saved when the [BaseGestureScope] is disposed and restored
 * when the [BaseGestureScope] is recreated.
 *
 * @param nextDownTime The downTime of the start of the next gesture, when chaining gestures.
 * This property will only be restored if an incomplete gesture was in progress when the state of
 * the [InputDispatcher] was saved.
 * @param partialGestureState The state of an incomplete gesture. If no gesture was in progress
 * when the state of the [InputDispatcher] was saved, this will be `null`.
 */
internal data class InputDispatcherState(
    val nextDownTime: Long,
    val partialGestureState: PartialGesture.SavedState?
)

/**
 * The state of a partial gesture.
 */
internal class PartialGesture internal constructor(
    internal val downTime: Long,
    internal var lastPosition: PxPosition
) {
    internal var lastEventTime: Long = downTime

    constructor(state: SavedState) : this(state.downTime, state.lastPosition) {
        lastEventTime = state.lastEventTime
    }

    /**
     * Immutable representation of [PartialGesture] to save its state between instances of
     * [InputDispatcher].
     */
    class SavedState(partialGesture: PartialGesture) {
        val downTime = partialGesture.downTime
        val lastPosition = partialGesture.lastPosition
        val lastEventTime = partialGesture.lastEventTime
    }
}
