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

internal class AndroidInputDispatcher(
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

    override fun getState(): InputDispatcherState {
        return InputDispatcherState(nextDownTime, partialGesture)
    }

    override fun restoreState(state: InputDispatcherState) {
        if (state.partialGesture != null) {
            nextDownTime = state.nextDownTime
            partialGesture = state.partialGesture
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

    override fun getCurrentPosition(pointerId: Int): PxPosition? {
        return partialGesture?.lastPositions?.get(pointerId)
    }

    override fun sendDown(pointerId: Int, position: PxPosition) {
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

        // Send the ACTION_DOWN or ACTION_POINTER_DOWN
        val positions = gesture.lastPositions
        gesture.sendMotionEvent(
            if (positions.size() == 1) ACTION_DOWN else ACTION_POINTER_DOWN,
            positions.indexOfKey(pointerId)
        )
    }

    // Move 1 pointer and don't send a move event
    override fun movePointer(pointerId: Int, position: PxPosition) {
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

    // Move 0 pointers and send a move event
    override fun sendMove() {
        sendMove(eventPeriod)
    }

    /**
     * Sends a move event, [deltaTime] milliseconds after the last event
     */
    // TODO(b/157717418): make this public API
    private fun sendMove(deltaTime: Long) {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send MOVE event, no gesture is in progress"
        }

        gesture.increaseEventTime(deltaTime)
        gesture.sendMotionEvent(ACTION_MOVE, 0)
        gesture.hasPointerUpdates = false
    }

    override fun sendUp(pointerId: Int) {
        val gesture = partialGesture

        // Check if this pointer is in the gesture
        check(gesture != null) {
            "Cannot send UP event, no gesture is in progress"
        }
        require(gesture.lastPositions.containsKey(pointerId)) {
            "Cannot send UP event for pointer $pointerId, it is not active in the current gesture"
        }

        gesture.flushPointerUpdates()

        // First send the ACTION_UP or ACTION_POINTER_UP
        val positions = gesture.lastPositions
        gesture.sendMotionEvent(
            if (positions.size() == 1) ACTION_UP else ACTION_POINTER_UP,
            positions.indexOfKey(pointerId)
        )

        // Then remove the pointer, and end the gesture if no pointers are left
        positions.remove(pointerId)
        if (positions.isEmpty) {
            partialGesture = null
        }
    }

    override fun sendCancel() {
        val gesture = checkNotNull(partialGesture) {
            "Cannot send CANCEL event, no gesture is in progress"
        }

        gesture.increaseEventTime()
        gesture.sendMotionEvent(ACTION_CANCEL, 0)
        partialGesture = null
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
     * Sends a MOVE event with all pointer locations, if any of the pointers has been moved by
     * [movePointer] since the last MOVE event.
     */
    private fun PartialGesture.flushPointerUpdates() {
        if (hasPointerUpdates) {
            sendMove()
        }
    }

    /**
     * Sends a MotionEvent with the given [action] and [actionIndex], adding all pointers that
     * are currently in the gesture.
     *
     * @see MotionEvent.getAction
     * @see MotionEvent.getActionIndex
     */
    private fun PartialGesture.sendMotionEvent(action: Int, actionIndex: Int) {
        sendMotionEvent(
            downTime,
            lastEventTime,
            action,
            actionIndex,
            List(lastPositions.size()) { lastPositions.valueAt(it) },
            List(lastPositions.size()) { lastPositions.keyAt(it) }
        )
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
        fs: List<(Long) -> PxPosition>,
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
            val t = lerp(t0, tN, progress)
            fs.forEachIndexed { i, f ->
                movePointer(i, f(t))
            }
            sendMove(t - tPrev)
            tPrev = t
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
        coordinates: List<PxPosition>,
        pointerIds: List<Int>
    ) {
        sleepUntil(eventTime)
        sendAndRecycleEvent(
            MotionEvent.obtain(
                /* downTime = */ downTime,
                /* eventTime = */ eventTime,
                /* action = */ action + (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                /* pointerCount = */ coordinates.size,
                /* pointerProperties = */ Array(coordinates.size) {
                    MotionEvent.PointerProperties().apply { id = pointerIds[it] }
                },
                /* pointerCoords = */ Array(coordinates.size) {
                    MotionEvent.PointerCoords().apply {
                        x = coordinates[it].x
                        y = coordinates[it].y
                    }
                },
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ 0,
                /* flags = */ 0
            )
        )
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
