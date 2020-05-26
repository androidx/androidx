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

import androidx.collection.SparseArrayCompat
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
 * * [getCurrentPosition]
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
                states.remove(owner)?.also {
                    restoreState(it)
                }
            }
        }

        internal fun saveState(owner: Owner?, inputDispatcher: InputDispatcher) {
            // Owner may have been removed already
            if (owner != null && AndroidOwnerRegistry.getUnfilteredOwners().contains(owner)) {
                states[owner] = inputDispatcher.getState()
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
    fun sendClick(position: PxPosition) {
        sendDown(0, position)
        sendMove()
        sendUp(0)
    }

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
     * During a partial gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no partial gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     * pointer is not currently in use
     */
    fun getCurrentPosition(pointerId: Int): PxPosition?

    /**
     * Sends a down event at [position] for the pointer with the given [pointerId], starting a
     * new partial gesture. A partial gesture can only be started if none was currently ongoing
     * for that pointer. Pointer ids may be reused during the same gesture. This method blocks
     * until the input event has been dispatched.
     *
     * It is possible to mix partial gestures with full gestures (e.g. send a [click][sendClick]
     * during a partial gesture), as long as you make sure that the default pointer id (id=0) is
     * free to be used by the full gesture.
     *
     * A full gesture starts with a down event at some position (with this method) that indicates
     * a finger has started touching the screen, followed by zero or more [down][sendDown],
     * [move][sendMove] and [up][sendUp] events that respectively indicate that another finger
     * started touching the screen, a finger moved around or a finger was lifted up from the
     * screen. A gesture is finished when [up][sendUp] lifts the last remaining finger from the
     * screen, or when a single [cancel][sendCancel] event is sent.
     *
     * Partial gestures don't have to be defined all in the same [doPartialGesture] block, but
     * keep in mind that while the gesture is not complete, all code you execute in between
     * blocks that progress the gesture, will be executed while imaginary fingers are actively
     * touching the screen.
     *
     * In the context of testing, it is not necessary to complete a gesture with an up or cancel
     * event, if the test ends before it expects the finger to be lifted from the screen.
     *
     * @param pointerId The id of the pointer, can be any number not yet in use by another pointer
     * @param position The coordinate of the down event
     *
     * @see movePointer
     * @see sendMove
     * @see sendUp
     * @see sendCancel
     */
    fun sendDown(pointerId: Int, position: PxPosition)

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position],
     * but does not send a move event. Use this to move multiple pointers simultaneously. To send
     * the next move event, which will contain the current position of _all_ pointers (not just
     * the moved ones), call [sendMove] without arguments. If you move one or more pointers and
     * then call [sendDown] or [sendUp], without calling [sendMove] first, a move event will be
     * sent right before that down or up event. See [sendDown] for more information on how to make
     * complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to move, as supplied in [sendDown]
     * @param position The position to move the pointer to
     *
     * @see sendDown
     * @see sendMove
     * @see sendUp
     * @see sendCancel
     */
    fun movePointer(pointerId: Int, position: PxPosition)

    /**
     * Sends a move event 10 milliseconds after the previous injected event of this gesture,
     * without moving any of the pointers. Use this to commit all changes in pointer location
     * made with [movePointer]. The sent event will contain the current position of all pointers.
     * See [sendDown] for more information on how to make complete gestures from partial gestures.
     */
    fun sendMove()

    /**
     * Sends an up event for the given [pointerId] at the current position of that pointer, 10
     * milliseconds after the previous injected event of this gesture. This method blocks until
     * the input event has been dispatched. See [sendDown] for more information on how to make
     * complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [sendDown]
     *
     * @see sendDown
     * @see movePointer
     * @see sendMove
     * @see sendCancel
     */
    fun sendUp(pointerId: Int)

    /**
     * Sends a cancel event 10 milliseconds after the previous injected event of this gesture.
     * This method blocks until the input event has been dispatched. See [sendDown] for more
     * information on how to make complete gestures from partial gestures.
     *
     * @see sendDown
     * @see movePointer
     * @see sendMove
     * @see sendUp
     */
    fun sendCancel()

    /**
     * Returns the state of this input dispatcher, in case a partial gesture is in progress.
     */
    fun getState(): InputDispatcherState

    /**
     * Restores the state of this input dispatcher, in case a partial gesture was in progress. If
     * a partial gesture was not in progress, no state is restored.
     *
     * @param state The state to restore
     */
    fun restoreState(state: InputDispatcherState)
    // TODO(b/157653315): Move restore state to constructor
}

/**
 * The state of an [InputDispatcher], saved when the [BaseGestureScope] is disposed and restored
 * when the [BaseGestureScope] is recreated.
 *
 * @param nextDownTime The downTime of the start of the next gesture, when chaining gestures.
 * This property will only be restored if an incomplete gesture was in progress when the state of
 * the [InputDispatcher] was saved.
 * @param partialGesture The state of an incomplete gesture. If no gesture was in progress
 * when the state of the [InputDispatcher] was saved, this will be `null`.
 */
internal data class InputDispatcherState(
    val nextDownTime: Long,
    val partialGesture: PartialGesture?
)

internal class PartialGesture(val downTime: Long, startPosition: PxPosition, pointerId: Int) {
    var lastEventTime: Long = downTime
    val lastPositions = SparseArrayCompat<PxPosition>().apply { put(pointerId, startPosition) }
    var hasPointerUpdates: Boolean = false
}
