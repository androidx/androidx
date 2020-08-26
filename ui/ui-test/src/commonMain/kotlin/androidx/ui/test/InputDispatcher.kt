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

internal abstract class InputDispatcher {
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
    protected abstract val now: Long

    /**
     * Sends all enqueued events and blocks while they are dispatched. Will suspend before
     * dispatching an event until [now] is at least that event's timestamp. If an exception is
     * thrown during the process, all events that haven't yet been dispatched will be dropped.
     */
    internal abstract fun sendAllSynchronous()

    internal abstract fun saveState(owner: Owner?)

    /**
     * Called when this [InputDispatcher] is about to be discarded, from [GestureScope.dispose].
     */
    internal abstract fun dispose()

    abstract fun enqueueClick(position: Offset)

    abstract fun enqueueSwipe(start: Offset, end: Offset, duration: Duration)

    abstract fun enqueueSwipe(
        curve: (Long) -> Offset,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    abstract fun enqueueSwipes(
        curves: List<(Long) -> Offset>,
        duration: Duration,
        keyTimes: List<Long> = emptyList()
    )

    abstract fun enqueueDelay(duration: Duration)

    abstract fun enqueueDown(pointerId: Int, position: Offset)

    abstract fun enqueueUp(pointerId: Int, delay: Long = 0)

    abstract fun enqueueCancel(delay: Long = eventPeriod)

    abstract fun movePointer(pointerId: Int, position: Offset)

    abstract fun enqueueMove(delay: Long = eventPeriod)

    abstract fun getCurrentPosition(pointerId: Int): Offset?
}

/**
 * The state of an [InputDispatcher], saved when the [GestureScope] is disposed and restored when
 * the [GestureScope] is recreated.
 *
 * @param nextDownTime The downTime of the start of the next gesture, when chaining gestures.
 * This property will only be restored if an incomplete gesture was in progress when the state of
 * the [InputDispatcher] was saved.
 * @param gestureLateness The time difference in milliseconds between enqueuing the first event
 * of the gesture and dispatching it. Depending on the implementation of [InputDispatcher], this
 * may or may not be used.
 * @param partialGesture The state of an incomplete gesture. If no gesture was in progress
 * when the state of the [InputDispatcher] was saved, this will be `null`.
 */
internal data class InputDispatcherState(
    val nextDownTime: Long,
    var gestureLateness: Long?,
    val partialGesture: PartialGesture?
)

internal expect class PartialGesture(downTime: Long, startPosition: Offset, pointerId: Int)

internal expect fun InputDispatcher(owner: Owner): InputDispatcher