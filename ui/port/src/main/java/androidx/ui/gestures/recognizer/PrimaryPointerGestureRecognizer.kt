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

package androidx.ui.gestures.recognizer

import androidx.ui.async.Timer
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.EnumProperty
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.kTouchSlop

// / A base class for gesture recognizers that track a single primary pointer.
// /
// / Gestures based on this class will reject the gesture if the primary pointer
// / travels beyond [kTouchSlop] pixels from the original contact point.
abstract class PrimaryPointerGestureRecognizer(
    // / If non-null, the recognizer will call [didExceedDeadline] after this
    // / amount of time has elapsed since starting to track the primary pointer.
    val deadline: Duration? = null,
    debugOwner: Any?
) : OneSequenceGestureRecognizer(debugOwner) {

    // / The current state of the recognizer.
    // /
    // / See [GestureRecognizerState] for a description of the states.
    var state: GestureRecognizerState = GestureRecognizerState.ready

    // / The ID of the primary pointer this recognizer is tracking.
    var primaryPointer: Int = 0

    // / The global location at which the primary pointer contacted the screen.
    var initialPosition: Offset? = null

    var _timer: Timer? = null

    override fun addPointer(event: PointerDownEvent) {
        startTrackingPointer(event.pointer)
        if (state == GestureRecognizerState.ready) {
            state = GestureRecognizerState.possible
            primaryPointer = event.pointer
            initialPosition = event.position
            if (deadline != null)
                _timer = Timer.create(deadline, ::didExceedDeadline)
        }
    }

    override fun handleEvent(event: PointerEvent) {
        assert(state != GestureRecognizerState.ready)
        if (state == GestureRecognizerState.possible && event.pointer == primaryPointer) {
            // TODO(abarth): Maybe factor the slop handling out into a separate class?
            if (event is PointerMoveEvent && _getDistance(event) > kTouchSlop) {
                resolve(GestureDisposition.rejected)
                stopTrackingPointer(primaryPointer)
            } else {
                handlePrimaryPointer(event)
            }
        }
        stopTrackingIfPointerNoLongerDown(event)
    }

    // / Override to provide behavior for the primary pointer when the gesture is still possible.
    protected abstract fun handlePrimaryPointer(event: PointerEvent)

    // / Override to be notified when [deadline] is exceeded.
    // /
    // / You must override this method if you supply a [deadline].
    protected open fun didExceedDeadline() {
        assert(deadline == null)
    }

    override fun rejectGesture(pointer: Int) {
        if (pointer == primaryPointer && state == GestureRecognizerState.possible) {
            _stopTimer()
            state = GestureRecognizerState.defunct
        }
    }

    override fun didStopTrackingLastPointer(pointer: Int) {
        assert(state != GestureRecognizerState.ready)
        _stopTimer()
        state = GestureRecognizerState.ready
    }

    override fun dispose() {
        _stopTimer()
        super.dispose()
    }

    private fun _stopTimer() {
        val timer = _timer
        if (timer != null) {
            timer.cancel()
            _timer = null
        }
    }

    private fun _getDistance(event: PointerEvent): Double {
        val offset: Offset = event.position - initialPosition!!
        return offset.getDistance()
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(EnumProperty("state", state))
    }
}