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
import androidx.ui.geometry.Offset
import androidx.ui.geometry.lerp
import androidx.ui.test.android.AndroidInputDispatcher
import androidx.ui.test.android.AndroidOwnerRegistry
import androidx.ui.unit.Duration
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.roundToInt

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
internal abstract class InputDispatcher {
    companion object : AndroidOwnerRegistry.OnRegistrationChangedListener {
        /**
         * Whether or not events with an eventTime in the future should be dispatched at that
         * exact eventTime. If `true`, will sleep until the eventTime, if `false`, will send the
         * event immediately without blocking.
         */
        private var dispatchInRealTime: Boolean = true

        /**
         * The minimum time between two successive injected MotionEvents, 10 milliseconds.
         * Ideally, the value should reflect a realistic pointer input sample rate, but that
         * depends on too many factors. Instead, the value is chosen comfortably below the
         * targeted frame rate (60 fps, equating to a 16ms period).
         */
        var eventPeriod = 10.milliseconds.inMilliseconds()
            private set

        /**
         * Indicates that [nextDownTime] is not set
         */
        private const val DownTimeNotSet = -1L

        /**
         * Stores the [InputDispatcherState] of each [Owner]. The state will be restored in an
         * [InputDispatcher] when it is created for an owner that has a state stored.
         */
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
                    // TODO(b/157653315): Move restore state to constructor
                    if (it.partialGesture != null) {
                        nextDownTime = it.nextDownTime
                        partialGesture = it.partialGesture
                    }
                }
            }
        }

        override fun onRegistrationChanged(owner: AndroidOwner, registered: Boolean) {
            if (!registered) {
                states.remove(owner)
            }
        }
    }

    internal fun saveState(owner: Owner?) {
        if (owner != null && AndroidOwnerRegistry.getUnfilteredOwners().contains(owner)) {
            states[owner] = InputDispatcherState(nextDownTime, partialGesture)
        }
    }

    protected var nextDownTime = DownTimeNotSet
    protected var partialGesture: PartialGesture? = null

    /**
     * Indicates if a gesture is in progress or not. A gesture is in progress if at least one
     * finger is (still) touching the screen.
     */
    val isGestureInProgress: Boolean
        get() = partialGesture != null

    /**
     * The current time, in the time scale used by gesture events.
     */
    protected abstract val now: Long

    /**
     * Generates the downTime of the next gesture with the given [duration]. The gesture's
     * [duration] is necessary to facilitate chaining of gestures: if another gesture is made
     * after the next one, it will start exactly [duration] after the start of the next gesture.
     * Always use this method to determine the downTime of the [down event][down] of a gesture.
     *
     * If the duration is unknown when calling this method, use a duration of zero and update
     * with [moveNextDownTime] when the duration is known, or use [moveNextDownTime]
     * incrementally if the gesture unfolds gradually.
     */
    private fun generateDownTime(duration: Duration): Long {
        val downTime = if (nextDownTime == DownTimeNotSet) {
            now
        } else {
            nextDownTime
        }
        nextDownTime = downTime + duration.inMilliseconds()
        return downTime
    }

    /**
     * Moves the start time of the next gesture ahead by the given [duration]. Does not affect
     * any event time from the current gesture. Use this when the expected duration passed to
     * [generateDownTime] has changed.
     */
    private fun moveNextDownTime(duration: Duration) {
        generateDownTime(duration)
    }

    /**
     * Increases the eventTime with the given [time]. Also pushes the downTime for the next
     * chained gesture by the same amount to facilitate chaining.
     */
    private fun PartialGesture.increaseEventTime(time: Long = eventPeriod) {
        moveNextDownTime(time.milliseconds)
        lastEventTime += time
    }

    /**
     * Delays the next gesture by the given [duration], but does not block. Guarantees that the
     * first event time of the next gesture will be exactly [duration] later then if that gesture
     * would be injected without this delay, provided that the next gesture is started using the
     * same [InputDispatcher] instance as the one used to end the last gesture.
     *
     * Note: this does not affect the time of the next event for the _current_ partial gesture,
     * using [move], [up] and [cancel], but it will affect the time of the _next_
     * gesture (including partial gestures started with [down]).
     *
     * @param duration The duration of the delay. Must be positive
     */
    fun delay(duration: Duration) {
        require(duration >= Duration.Zero) {
            "duration of a delay can only be positive, not $duration"
        }
        moveNextDownTime(duration)
    }

    /**
     * Blocks until uptime of [time], if [dispatchInRealTime] is `true`.
     */
    protected fun sleepUntil(time: Long) {
        if (dispatchInRealTime) {
            val currTime = now
            if (currTime < time) {
                Thread.sleep(time - currTime)
            }
        }
    }

    /**
     * Sends a click event at [position]. There will be 10ms in between the down and the up
     * event. This method blocks until all input events have been dispatched.
     *
     * @param position The coordinate of the click
     */
    fun sendClick(position: Offset) {
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
    fun sendSwipe(start: Offset, end: Offset, duration: Duration) {
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
        curve: (Long) -> Offset,
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
        curves: List<(Long) -> Offset>,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    ) {
        val startTime = 0L
        val endTime = duration.inMilliseconds()

        // Validate input
        require(duration >= 1.milliseconds) {
            "duration must be at least 1 millisecond, not $duration"
        }
        val validRange = startTime..endTime
        require(keyTimes.all { it in validRange }) {
            "keyTimes contains timestamps out of range [$startTime..$endTime]: $keyTimes"
        }
        require(keyTimes.asSequence().zipWithNext { a, b -> a <= b }.all { it }) {
            "keyTimes must be sorted: $keyTimes"
        }

        // Send down events
        curves.forEachIndexed { i, curve ->
            sendDown(i, curve(startTime))
        }

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
            sendPartialSwipes(curves, currTime, tNext)
            currTime = tNext
        }

        // And end with up events
        repeat(curves.size) {
            sendUp(it)
        }
    }

    /**
     * Sends move events between `f([t0])` and `f([tN])` during the time window `(downTime + t0,
     * downTime + tN]`, using [fs] to sample the coordinate of each event. The number of events
     * sent (#numEvents) is such that the time between each event is as close to [eventPeriod] as
     * possible, but at least 1. The first event is sent at time `downTime + (tN - t0) /
     * #numEvents`, the last event is sent at time tN.
     *
     * @param fs The functions that define the coordinates of the respective gestures over time
     * @param t0 The start time of this segment of the swipe, in milliseconds relative to downTime
     * @param tN The end time of this segment of the swipe, in milliseconds relative to downTime
     */
    private fun sendPartialSwipes(
        fs: List<(Long) -> Offset>,
        t0: Long,
        tN: Long
    ) {
        var step = 0
        // How many steps will we take between t0 and tN? At least 1, and a number that will
        // bring as as close to eventPeriod as possible
        val steps = max(1, ((tN - t0) / eventPeriod.toFloat()).roundToInt())

        var tPrev = t0
        while (step++ < steps) {
            val progress = step / steps.toFloat()
            val t = androidx.ui.util.lerp(t0, tN, progress)
            fs.forEachIndexed { i, f ->
                movePointer(i, f(t))
            }
            sendMove(t - tPrev)
            tPrev = t
        }
    }

    /**
     * During a partial gesture, returns the position of the last touch event of the given
     * [pointerId]. Returns `null` if no partial gesture is in progress for that [pointerId].
     *
     * @param pointerId The id of the pointer for which to return the current position
     * @return The current position of the pointer with the given [pointerId], or `null` if the
     * pointer is not currently in use
     */
    fun getCurrentPosition(pointerId: Int): Offset? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    /**
     * Sends a down event at [position] for the pointer with the given [pointerId], starting a
     * new partial gesture. A partial gesture can only be started if none was currently ongoing
     * for that pointer. Pointer ids may be reused during the same gesture. This method blocks
     * until the input event has been dispatched.
     *
     * It is possible to mix partial gestures with full gestures (e.g. send a [click][click]
     * during a partial gesture), as long as you make sure that the default pointer id (id=0) is
     * free to be used by the full gesture.
     *
     * A full gesture starts with a down event at some position (with this method) that indicates
     * a finger has started touching the screen, followed by zero or more [down][down],
     * [move][move] and [up][up] events that respectively indicate that another finger
     * started touching the screen, a finger moved around or a finger was lifted up from the
     * screen. A gesture is finished when [up][up] lifts the last remaining finger from the
     * screen, or when a single [cancel][cancel] event is sent.
     *
     * Partial gestures don't have to be defined all in the same [performPartialGesture] block, but
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
     * @see move
     * @see up
     * @see cancel
     */
    fun sendDown(pointerId: Int, position: Offset) {
        var gesture = partialGesture

        // Check if this pointer is not already down
        require(gesture == null || !gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send DOWN event, a gesture is already in progress for pointer $pointerId"
        }

        gesture?.flushPointerUpdates()

        // Start a new gesture, or add the pointerId to the existing gesture
        if (gesture == null) {
            gesture = PartialGesture(generateDownTime(0.milliseconds), position, pointerId)
            partialGesture = gesture
        } else {
            gesture.lastPositions.put(pointerId, position)
        }

        // Send the DOWN event
        gesture.sendDown(pointerId)
    }

    /**
     * Updates the position of the pointer with the given [pointerId] to the given [position],
     * but does not send a move event. Use this to move multiple pointers simultaneously. To send
     * the next move event, which will contain the current position of _all_ pointers (not just
     * the moved ones), call [move] without arguments. If you move one or more pointers and
     * then call [down] or [up], without calling [move] first, a move event will be
     * sent right before that down or up event. See [down] for more information on how to make
     * complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to move, as supplied in [down]
     * @param position The position to move the pointer to
     *
     * @see down
     * @see move
     * @see up
     * @see cancel
     */
    fun movePointer(pointerId: Int, position: Offset) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot move pointers, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot move pointer $pointerId, it is not active in the current gesture"
        }

        gesture.lastPositions.put(pointerId, position)
        gesture.hasPointerUpdates = true
    }

    /**
     * Sends a move event [delay] milliseconds after the previous injected event of this gesture,
     * without moving any of the pointers. The default [delay] is [10][eventPeriod] milliseconds.
     * Use this to commit all changes in pointer location made with [movePointer]. The sent event
     * will contain the current position of all pointers. See [down] for more information on
     * how to make complete gestures from partial gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. [10][eventPeriod] milliseconds by default.
     */
    fun sendMove(delay: Long = eventPeriod) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }
        require(delay >= 0) {
            "Cannot send MOVE event with a delay of $delay ms"
        }

        gesture.increaseEventTime(delay)
        gesture.sendMove()
        gesture.hasPointerUpdates = false
    }

    /**
     * Sends an up event for the given [pointerId] at the current position of that pointer,
     * [delay] milliseconds after the previous injected event of this gesture. The default
     * [delay] is 0 milliseconds. This method blocks until the input event has been dispatched.
     * See [down] for more information on how to make complete gestures from partial gestures.
     *
     * @param pointerId The id of the pointer to lift up, as supplied in [down]
     * @param delay The time in milliseconds between the previously injected event and the move
     * event. 0 milliseconds by default.
     *
     * @see down
     * @see movePointer
     * @see move
     * @see cancel
     */
    fun sendUp(pointerId: Int, delay: Long = 0) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot send UP event, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }
        require(delay >= 0) {
            "Cannot send UP event with a delay of $delay ms"
        }

        gesture.flushPointerUpdates()
        gesture.increaseEventTime(delay)

        // First send the UP event
        gesture.sendUp(pointerId)

        // Then remove the pointer, and end the gesture if no pointers are left
        gesture.lastPositions.remove(pointerId)
        if (gesture.lastPositions.isEmpty) {
            partialGesture = null
        }
    }

    /**
     * Sends a cancel event [delay] milliseconds after the previous injected event of this
     * gesture. The default [delay] is [10][eventPeriod] milliseconds. This method blocks until
     * the input event has been dispatched. See [down] for more information on how to make
     * complete gestures from partial gestures.
     *
     * @param delay The time in milliseconds between the previously injected event and the cancel
     * event. [10][eventPeriod] milliseconds by default.
     *
     * @see down
     * @see movePointer
     * @see move
     * @see up
     */
    fun sendCancel(delay: Long = eventPeriod) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }
        require(delay >= 0) {
            "Cannot send CANCEL event with a delay of $delay ms"
        }

        gesture.increaseEventTime(delay)
        gesture.sendCancel()
        partialGesture = null
    }

    /**
     * Sends a MOVE event with all pointer locations, if any of the pointers has been moved by
     * [movePointer] since the last MOVE event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            sendMove(eventPeriod)
        }
    }

    protected abstract fun PartialGesture.sendDown(pointerId: Int)

    protected abstract fun PartialGesture.sendMove()

    protected abstract fun PartialGesture.sendUp(pointerId: Int)

    protected abstract fun PartialGesture.sendCancel()

    /**
     * A test rule that modifies [InputDispatcher]s behavior. Can be used to disable
     * dispatching of MotionEvents in real time (skips the sleep before injection of an event) or
     * to change the time between consecutive injected events.
     *
     * @param disableDispatchInRealTime If set, controls whether or not events with an eventTime
     * in the future will be dispatched as soon as possible or at that exact eventTime. If
     * `false` or not set, will sleep until the eventTime, if `true`, will send the event
     * immediately without blocking. See also [dispatchInRealTime].
     * @param eventPeriodOverride If set, specifies a different period in milliseconds between
     * two consecutive injected motion events injected by this [InputDispatcher]. If not
     * set, the event period of 10 milliseconds is unchanged.
     *
     * @see InputDispatcher.eventPeriod
     */
    internal class InputDispatcherTestRule(
        private val disableDispatchInRealTime: Boolean = false,
        private val eventPeriodOverride: Long? = null
    ) : TestRule {

        override fun apply(base: Statement, description: Description?): Statement {
            return ModifyingStatement(base)
        }

        inner class ModifyingStatement(private val base: Statement) : Statement() {
            override fun evaluate() {
                if (disableDispatchInRealTime) {
                    dispatchInRealTime = false
                }
                if (eventPeriodOverride != null) {
                    InputDispatcher.eventPeriod = eventPeriodOverride
                }
                try {
                    base.evaluate()
                } finally {
                    if (disableDispatchInRealTime) {
                        dispatchInRealTime = true
                    }
                    if (eventPeriodOverride != null) {
                        InputDispatcher.eventPeriod = 10L
                    }
                }
            }
        }
    }
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

internal class PartialGesture(val downTime: Long, startPosition: Offset, pointerId: Int) {
    var lastEventTime: Long = downTime
    val lastPositions = SparseArrayCompat<Offset>().apply { put(pointerId, startPosition) }
    var hasPointerUpdates: Boolean = false
}
