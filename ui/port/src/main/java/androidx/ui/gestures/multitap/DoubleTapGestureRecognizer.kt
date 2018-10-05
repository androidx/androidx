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

package androidx.ui.gestures.multitap

import androidx.ui.async.Timer
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.kDoubleTapSlop
import androidx.ui.gestures.kDoubleTapTimeout
import androidx.ui.gestures.kDoubleTapTouchSlop
import androidx.ui.gestures.recognizer.GestureRecognizer

/**
 * Signature for callback when the user has tapped the screen at the same
 * location twice in quick succession.
 */
typealias GestureDoubleTapCallback = () -> Unit

/**
 * Recognizes when the user has tapped the screen at the same location twice in
 * quick succession.
 */
class DoubleTapGestureRecognizer(debugOwner: Any? = null) :
    GestureRecognizer(debugOwner = debugOwner) {

    // Implementation notes:
    // The double tap recognizer can be in one of four states. There's no
    // explicit enum for the states, because they are already captured by
    // the state of existing fields. Specifically:
    // Waiting on first tap: In this state, the trackers list is empty, and
    // firstTap is null.
    // First tap in progress: In this state, the trackers list contains all
    // the states for taps that have begun but not completed. This list can
    // have more than one entry if two pointers begin to tap.
    // Waiting on second tap: In this state, one of the in-progress taps has
    // completed successfully. The trackers list is again empty, and
    // firstTap records the successful tap.
    // Second tap in progress: Much like the "first tap in progress" state, but
    // firstTap is non-null. If a tap completes successfully while in this
    // state, the callback is called and the state is reset.
    // There are various other scenarios that cause the state to reset:
    // - All in-progress taps are rejected (by time, distance, pointercancel, etc)
    // - The long timer between taps expires
    // - The gesture arena decides we have been rejected wholesale

    /**
     * Called when the user has tapped the screen at the same location twice in
     * quick succession.
     */
    var onDoubleTap: GestureDoubleTapCallback? = null

    private var doubleTapTimer: Timer? = null
    private var firstTap: TapTracker? = null
    private val trackers: MutableMap<Int, TapTracker> = hashMapOf()

    override fun addPointer(event: PointerDownEvent) {
        // Ignore out-of-bounds second taps.
        firstTap?.let {
            if (!it.isWithinTolerance(event, kDoubleTapSlop)) {
                return
            }
        }
        stopDoubleTapTimer()
        val tracker = TapTracker(
            event = event as PointerDownEvent,
            entry = GestureBinding.instance!!.gestureArena.add(event.pointer, this)
        )
        trackers[event.pointer] = tracker
        tracker.startTrackingPointer(::handleEvent)
    }

    private fun handleEvent(event: PointerEvent) {
        val tracker: TapTracker = trackers[event.pointer]!!
        if (event is PointerUpEvent) {
            if (firstTap == null) {
                registerFirstTap(tracker)
            } else {
                registerSecondTap(tracker)
            }
        } else {
            if (event is PointerMoveEvent) {
                if (!tracker.isWithinTolerance(event, kDoubleTapTouchSlop)) {
                    reject(tracker)
                }
            } else {
                if (event is PointerCancelEvent) {
                    reject(tracker)
                }
            }
        }
    }

    override fun acceptGesture(pointer: Int) {}

    override fun rejectGesture(pointer: Int) {
        // If tracker isn't in the list, check if this is the first tap tracker
        var tracker: TapTracker? = trackers[pointer]
        firstTap?.let {
            if (tracker == null && it.pointer == pointer) {
                tracker = firstTap
            }
        }
        // If tracker is still null, we rejected ourselves already
        tracker?.let {
            reject(it)
        }
    }

    private fun reject(tracker: TapTracker) {
        trackers.remove(tracker.pointer)
        tracker.entry.resolve(GestureDisposition.rejected)
        freezeTracker(tracker)
        // If the first tap is in progress, and we've run out of taps to track,
        // reset won't have any work to do. But if we're in the second tap, we need
        // to clear intermediate state.
        if (firstTap != null && (trackers.isEmpty() || tracker == firstTap)) {
            reset()
        }
    }

    override fun dispose() {
        reset()
        super.dispose()
    }

    private fun reset() {
        stopDoubleTapTimer()
        firstTap?.let {
            // Note, order is important below in order for the resolve -> reject logic to work
            // properly.
            val tracker: TapTracker = it
            firstTap = null
            reject(tracker)
            GestureBinding.instance!!.gestureArena.release(tracker.pointer)
        }
        clearTrackers()
    }

    private fun registerFirstTap(tracker: TapTracker) {
        startDoubleTapTimer()
        GestureBinding.instance!!.gestureArena.hold(tracker.pointer)
        // Note, order is important below in order for the clear -> reject logic to
        // work properly.
        freezeTracker(tracker)
        trackers.remove(tracker.pointer)
        clearTrackers()
        firstTap = tracker
    }

    private fun registerSecondTap(tracker: TapTracker) {
        firstTap!!.entry.resolve(GestureDisposition.accepted)
        tracker.entry.resolve(GestureDisposition.accepted)
        freezeTracker(tracker)
        trackers.remove(tracker.pointer)
        onDoubleTap?.let {
            invokeCallback("onDoubleTap", it)
        }
        reset()
    }

    private fun clearTrackers() {
        trackers.values.toList().forEach(::reject)
        assert(trackers.isEmpty())
    }

    private fun freezeTracker(tracker: TapTracker) {
        tracker.stopTrackingPointer(::handleEvent)
    }

    private fun startDoubleTapTimer() {
        doubleTapTimer = doubleTapTimer ?: Timer.create(kDoubleTapTimeout, ::reset)
    }

    private fun stopDoubleTapTimer() {
        doubleTapTimer?.cancel()
        doubleTapTimer = null
    }

    override val debugDescription = "double tap"
}