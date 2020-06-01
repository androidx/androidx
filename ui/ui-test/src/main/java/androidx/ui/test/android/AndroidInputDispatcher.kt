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

package androidx.ui.test.android

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import androidx.ui.test.InputDispatcher
import androidx.ui.test.InputDispatcherState
import androidx.ui.test.PartialGesture
import androidx.ui.unit.Duration
import androidx.ui.unit.PxPosition
import androidx.ui.unit.inMilliseconds
import androidx.ui.unit.milliseconds
import androidx.ui.util.lerp
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

internal class AndroidInputDispatcher constructor(
    private val sendEvent: (MotionEvent) -> Unit
) : InputDispatcher {
    companion object {
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
        private var eventPeriod = 10.milliseconds.inMilliseconds()

        /**
         * Indicates that [nextDownTime] is not set
         */
        private const val DownTimeNotSet = -1L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var nextDownTime = DownTimeNotSet
    private var partialGesture: PartialGesture? = null

    override fun saveInstanceState(): InputDispatcherState {
        return InputDispatcherState(
            nextDownTime,
            partialGesture?.let { PartialGesture.SavedState(it) }
        )
    }

    override fun restoreInstanceState(state: InputDispatcherState) {
        if (state.partialGestureState != null) {
            nextDownTime = state.nextDownTime
            partialGesture = PartialGesture(state.partialGestureState)
        }
    }

    /**
     * Generates the downTime of the next gesture with the given [duration]. The gesture's
     * [duration] is necessary to facilitate chaining of gestures: if another gesture is made
     * after the next one, it will start exactly [duration] after the start of the next gesture.
     * Always use this method to determine the downTime of the [ACTION_DOWN] event of a gesture.
     *
     * If the duration is unknown when calling this method, use a duration of zero and update
     * with [moveNextDownTime] when the duration is known, or use [moveNextDownTime]
     * incrementally if the gesture unfolds gradually.
     */
    private fun generateDownTime(duration: Duration): Long {
        val downTime = if (nextDownTime == DownTimeNotSet) {
            SystemClock.uptimeMillis()
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

    override fun delay(duration: Duration) {
        require(duration >= Duration.Zero) {
            "duration of a delay can only be positive, not $duration"
        }
        moveNextDownTime(duration)
        sleepUntil(nextDownTime)
    }

    override fun sendClick(position: PxPosition) {
        val downTime = generateDownTime(eventPeriod.milliseconds)
        sendMotionEvent(downTime, downTime, ACTION_DOWN, position)
        sendMotionEvent(downTime, downTime + eventPeriod, ACTION_UP, position)
    }

    override val currentPosition: PxPosition?
        get() = partialGesture?.lastPosition

    override fun sendDown(position: PxPosition) {
        check(partialGesture == null) {
            "Cannot send DOWN event, a gesture is already in progress"
        }
        val downTime = generateDownTime(0.milliseconds)
        sendMotionEvent(downTime, downTime, ACTION_DOWN, position)
        partialGesture = PartialGesture(downTime, position)
    }

    override fun sendMove(position: PxPosition) {
        checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }.let {
            sendNextMotionEvent(it, ACTION_MOVE, position)
        }
    }

    override fun sendUp(position: PxPosition?) {
        checkNotNull(partialGesture) {
            "Cannot send UP event, no gesture is in progress"
        }.let {
            sendNextMotionEvent(it, ACTION_UP, position ?: it.lastPosition)
        }
        partialGesture = null
    }

    override fun sendCancel(position: PxPosition?) {
        checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }.let {
            sendNextMotionEvent(it, ACTION_CANCEL, position ?: it.lastPosition)
        }
        partialGesture = null
    }

    private fun sendNextMotionEvent(gesture: PartialGesture, action: Int, position: PxPosition) {
        moveNextDownTime(eventPeriod.milliseconds)
        gesture.lastEventTime += eventPeriod
        gesture.lastPosition = position
        sendMotionEvent(gesture.downTime, gesture.lastEventTime, action, position)
    }

    override fun sendSwipes(
        curves: List<(Long) -> PxPosition>,
        duration: Duration,
        keyTimes: List<Long>
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

        // Determine time window for the events
        val downTime = generateDownTime(duration)
        val upTime = downTime + duration.inMilliseconds()

        val initialPositions = mutableListOf<PxPosition>()

        // Send down events
        for ((index, curve) in curves.withIndex()) {
            val action = if (index == 0) ACTION_DOWN else ACTION_POINTER_DOWN
            initialPositions.add(curve(startTime))
            sendMotionEvent(downTime, downTime, action, index, initialPositions)
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
            sendPartialSwipes(downTime, curves, currTime, tNext)
            currTime = tNext
        }

        // And end with up events
        val finalPositions = curves.map { it(endTime) }.toMutableList()
        for (index in curves.indices.reversed()) {
            val action = if (index > 0) ACTION_POINTER_UP else ACTION_UP
            sendMotionEvent(downTime, upTime, action, index, finalPositions)
            finalPositions.removeAt(index)
        }
    }

    /**
     * Sends move events between `f([t0])` and `f([tN])` during the time window `(downTime + t0,
     * downTime + tN]`, using [fs] to sample the coordinate of each event. The number of events
     * sent (#numEvents) is such that the time between each event is as close to [eventPeriod] as
     * possible, but at least 1. The first event is sent at time `downTime + (tN - t0) /
     * #numEvents`, the last event is sent at time tN.
     *
     * @param downTime The event time of the down event that started this gesture
     * @param fs The functions that define the coordinates of the respective gestures over time
     * @param t0 The start time of this segment of the swipe, in milliseconds relative to downTime
     * @param tN The end time of this segment of the swipe, in milliseconds relative to downTime
     */
    private fun sendPartialSwipes(
        downTime: Long,
        fs: List<(Long) -> PxPosition>,
        t0: Long,
        tN: Long
    ) {
        var step = 0
        // How many steps will we take between t0 and tN? At least 1, and a number that will
        // bring as as close to eventPeriod as possible
        val steps = max(1, ((tN - t0) / eventPeriod.toFloat()).roundToInt())

        while (step++ < steps) {
            val progress = step / steps.toFloat()
            val t = lerp(t0, tN, progress)
            sendMotionEvent(downTime, downTime + t, ACTION_MOVE, 0, fs.map { it(t) })
        }
    }

    /**
     * Sends an event with the given parameters. Method blocks if [dispatchInRealTime] is `true`.
     */
    private fun sendMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        actionIndex: Int,
        coordinates: List<PxPosition>
    ) {
        sleepUntil(eventTime)
        sendAndRecycleEvent(
            MotionEvent.obtain(
                downTime,
                eventTime,
                action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                coordinates.size,
                Array(coordinates.size) {
                    MotionEvent.PointerProperties().apply { id = it }
                },
                Array(coordinates.size) {
                    MotionEvent.PointerCoords().apply {
                        x = coordinates[it].x
                        y = coordinates[it].y
                    }
                },
                0,
                0,
                0f,
                0f,
                0,
                0,
                0,
                0
            )
        )
    }

    /**
     * Sends an event with the given parameters. Method blocks if [dispatchInRealTime] is `true`.
     */
    private fun sendMotionEvent(
        downTime: Long,
        eventTime: Long,
        action: Int,
        position: PxPosition
    ) {
        sendMotionEvent(downTime, eventTime, action, 0, listOf(position))
    }

    private fun sleepUntil(time: Long) {
        if (dispatchInRealTime) {
            val currTime = SystemClock.uptimeMillis()
            if (currTime < time) {
                SystemClock.sleep(time - currTime)
            }
        }
    }

    /**
     * Sends the [event] to the MotionEvent dispatcher and [recycles][MotionEvent.recycle] it
     * regardless of the result. This method blocks until the event is sent.
     */
    private fun sendAndRecycleEvent(event: MotionEvent) {
        val latch = CountDownLatch(1)
        handler.post {
            try {
                sendEvent(event)
            } finally {
                event.recycle()
                latch.countDown()
            }
        }
        check(latch.await(5, TimeUnit.SECONDS)) {
            "Event $event was not dispatched in 5 seconds"
        }
    }

    /**
     * A test rule that modifies [AndroidInputDispatcher]s behavior. Can be used to disable
     * dispatching of MotionEvents in real time (skips the sleep before injection of an event) or
     * to change the time between consecutive injected events.
     *
     * @param disableDispatchInRealTime If set, controls whether or not events with an eventTime
     * in the future will be dispatched as soon as possible or at that exact eventTime. If
     * `false` or not set, will sleep until the eventTime, if `true`, will send the event
     * immediately without blocking. See also [dispatchInRealTime].
     * @param eventPeriodOverride If set, specifies a different period in milliseconds between
     * two consecutive injected motion events injected by this [AndroidInputDispatcher]. If not
     * set, the event period of 10 milliseconds is unchanged.
     *
     * @see AndroidInputDispatcher.eventPeriod
     */
    internal class TestRule(
        private val disableDispatchInRealTime: Boolean = false,
        private val eventPeriodOverride: Long? = null
    ) : org.junit.rules.TestRule {

        val eventPeriod get() = AndroidInputDispatcher.eventPeriod

        override fun apply(base: Statement, description: Description?): Statement {
            return ModifyingStatement(base)
        }

        inner class ModifyingStatement(private val base: Statement) : Statement() {
            override fun evaluate() {
                if (disableDispatchInRealTime) {
                    dispatchInRealTime = false
                }
                if (eventPeriodOverride != null) {
                    AndroidInputDispatcher.eventPeriod = eventPeriodOverride
                }
                try {
                    base.evaluate()
                } finally {
                    if (disableDispatchInRealTime) {
                        dispatchInRealTime = true
                    }
                    if (eventPeriodOverride != null) {
                        AndroidInputDispatcher.eventPeriod = 10L
                    }
                }
            }
        }
    }
}
