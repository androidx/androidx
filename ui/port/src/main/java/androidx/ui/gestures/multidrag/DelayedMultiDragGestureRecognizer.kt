/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.multidrag

import androidx.ui.async.Timer
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.kLongPressTimeout
import androidx.ui.gestures.kTouchSlop

/**
 * Recognizes movement both horizontally and vertically on a per-pointer basis
 * after a delay.
 *
 * In contrast to [ImmediateMultiDragGestureRecognizer],
 * [DelayedMultiDragGestureRecognizer] waits for a [delay] before recognizing
 * the drag. If the pointer moves more than [kTouchSlop] before the delay
 * expires, the gesture is not recognized.
 *
 * In contrast to [PanGestureRecognizer], [DelayedMultiDragGestureRecognizer]
 * watches each pointer separately, which means multiple drags can be
 * recognized concurrently if multiple pointers are in contact with the screen.
 *
 * In order for a drag to be recognized by this recognizer, the pointer must
 * remain in the same place for [delay] (up to [kTouchSlop]). The [delay]
 * defaults to [kLongPressTimeout] to match [LongPressGestureRecognizer] but
 * can be changed for specific behaviors.
 *
 * See also:
 *
 *  * [ImmediateMultiDragGestureRecognizer], a similar recognizer but without
 *    the delay.
 *  * [PanGestureRecognizer], which recognizes only one drag gesture at a time,
 *    regardless of how many fingers are involved.
 */
class DelayedMultiDragGestureRecognizer(
    /**
     * The amount of time the pointer must remain in the same place for the drag
     * to be recognized.
     */
    val delay: Duration = kLongPressTimeout,
    debugOwner: Any? = null
) : MultiDragGestureRecognizer<MultiDragPointerState>(debugOwner = debugOwner) {

    override fun createNewPointerState(event: PointerDownEvent): MultiDragPointerState {
        return DelayedPointerState(event.position, delay)
    }

    override val debugDescription = "long multidrag"
}

private class DelayedPointerState(initialPosition: Offset, delay: Duration) :
    MultiDragPointerState(initialPosition) {

    private var timer: Timer? = Timer.create(delay, ::delayPassed)
    private var starter: GestureMultiDragStartCallback? = null

    private fun delayPassed() {
        assert(timer != null)
        assert(pendingDelta != null)
        assert(pendingDelta!!.getDistance() <= kTouchSlop)
        timer = null
        val starter = starter
        if (starter != null) {
            starter(initialPosition)
            this.starter = null
        } else {
            resolve(GestureDisposition.accepted)
        }
        assert(this.starter == null)
    }

    private fun ensureTimerStopped() {
        timer?.cancel()
        timer = null
    }

    override fun accepted(starter: GestureMultiDragStartCallback) {
        assert(this.starter == null)
        if (timer == null)
            starter(initialPosition)
        else
            this.starter = starter
    }

    override fun checkForResolutionAfterMove() {
        if (timer == null) {
            // If we've been accepted by the gesture arena but the pointer moves too
            // much before the timer fires, we end up a state where the timer is
            // stopped but we keep getting calls to this function because we never
            // actually started the drag. In this case, starter will be non-null
            // because we're essentially waiting forever to start the drag.
            assert(starter != null)
            return
        }
        assert(pendingDelta != null)
        if (pendingDelta!!.getDistance() > kTouchSlop) {
            resolve(GestureDisposition.rejected)
            ensureTimerStopped()
        }
    }

    override fun dispose() {
        ensureTimerStopped()
        super.dispose()
    }
}