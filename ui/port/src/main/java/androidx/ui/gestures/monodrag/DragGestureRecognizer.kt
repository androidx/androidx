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

package androidx.ui.gestures.monodrag

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.drag_details.DragDownDetails
import androidx.ui.gestures.drag_details.DragEndDetails
import androidx.ui.gestures.drag_details.DragStartDetails
import androidx.ui.gestures.drag_details.DragUpdateDetails
import androidx.ui.gestures.drag_details.GestureDragDownCallback
import androidx.ui.gestures.drag_details.GestureDragStartCallback
import androidx.ui.gestures.drag_details.GestureDragUpdateCallback
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.kMaxFlingVelocity
import androidx.ui.gestures.kMinFlingVelocity
import androidx.ui.gestures.recognizer.OneSequenceGestureRecognizer
import androidx.ui.gestures.velocity_tracker.Velocity
import androidx.ui.gestures.velocity_tracker.VelocityEstimate
import androidx.ui.gestures.velocity_tracker.VelocityTracker

// / Recognizes movement.
// /
// / In contrast to [MultiDragGestureRecognizer], [DragGestureRecognizer]
// / recognizes a single gesture sequence for all the pointers it watches, which
// / means that the recognizer has at most one drag sequence active at any given
// / time regardless of how many pointers are in contact with the screen.
// /
// / [DragGestureRecognizer] is not intended to be used directly. Instead,
// / consider using one of its subclasses to recognize specific types for drag
// / gestures.
// /
// / See also:
// /
// /  * [HorizontalDragGestureRecognizer], for left and right drags.
// /  * [VerticalDragGestureRecognizer], for up and down drags.
// /  * [PanGestureRecognizer], for drags that are not locked to a single axis.
// TODO(Migration/shepshapard): Needs tests, which rely on some Mixin stuff.
abstract class DragGestureRecognizer(
    debugOwner: Any? = null
) : OneSequenceGestureRecognizer(
    debugOwner
) {

    // / A pointer has contacted the screen and might begin to move.
    // /
    // / The position of the pointer is provided in the callback's `details`
    // / argument, which is a [DragDownDetails] object.
    var onDown: GestureDragDownCallback? = null

    // / A pointer has contacted the screen and has begun to move.
    // /
    // / The position of the pointer is provided in the callback's `details`
    // / argument, which is a [DragStartDetails] object.
    var onStart: GestureDragStartCallback? = null

    // / A pointer that is in contact with the screen and moving has moved again.
    // /
    // / The distance travelled by the pointer since the last update is provided in
    // / the callback's `details` argument, which is a [DragUpdateDetails] object.
    var onUpdate: GestureDragUpdateCallback? = null

    // / A pointer that was previously in contact with the screen and moving is no
    // / longer in contact with the screen and was moving at a specific velocity
    // / when it stopped contacting the screen.
    // /
    // / The velocity is provided in the callback's `details` argument, which is a
    // / [DragEndDetails] object.
    var onEnd: GestureDragEndCallback? = null

    // / The pointer that previously triggered [onDown] did not complete.
    var onCancel: GestureDragCancelCallback? = null

    // / The minimum distance an input pointer drag must have moved to
    // / to be considered a fling gesture.
    // /
    // / This value is typically compared with the distance traveled along the
    // / scrolling axis. If null then [kTouchSlop] is used.
    var minFlingDistance: Double? = null

    // / The minimum velocity for an input pointer drag to be considered fling.
    // /
    // / This value is typically compared with the magnitude of fling gesture's
    // / velocity along the scrolling axis. If null then [kMinFlingVelocity]
    // / is used.
    var minFlingVelocity: Double? = null

    // / Fling velocity magnitudes will be clamped to this value.
    // /
    // / If null then [kMaxFlingVelocity] is used.
    var maxFlingVelocity: Double? = null

    private var _state: _DragState = _DragState.ready
    private var _initialPosition: Offset? = null
    internal var _pendingDragOffset: Offset? = null
    private var _lastPendingEventTimestamp: Duration? = null

    internal abstract fun _isFlingGesture(estimate: VelocityEstimate): Boolean

    internal abstract fun _getDeltaForDetails(delta: Offset): Offset

    internal abstract fun _getPrimaryValueFromOffset(value: Offset): Double?

    internal abstract fun _hasSufficientPendingDragDeltaToAccept(): Boolean

    private val _velocityTrackers: MutableMap<Int, VelocityTracker> = mutableMapOf()

    override fun addPointer(event: PointerDownEvent) {
        startTrackingPointer(event.pointer)
        _velocityTrackers[event.pointer] = VelocityTracker()
        if (_state == _DragState.ready) {
            val position = event.position
            _state = _DragState.possible
            _initialPosition = position
            _pendingDragOffset = Offset.zero
            _lastPendingEventTimestamp = event.timeStamp
            onDown?.let {
                invokeCallback("onDown", {
                    it(DragDownDetails(globalPosition = position))
                })
            }
        } else {
            if (_state == _DragState.accepted) {
                resolve(GestureDisposition.accepted)
            }
        }
    }

    override fun handleEvent(event: PointerEvent) {
        assert(_state != _DragState.ready)
        if ((!event.synthesized && (event is PointerDownEvent || event is PointerMoveEvent))) {
            val tracker: VelocityTracker? = _velocityTrackers.get(event.pointer)
            assert(tracker != null)
            tracker!!.addPosition(event.timeStamp, event.position)
        }
        if (event is PointerMoveEvent) {
            val delta: Offset = event.delta
            if ((_state == _DragState.accepted)) {
                onUpdate?.let {
                    invokeCallback("onUpdate", {
                        it(
                            DragUpdateDetails(
                                sourceTimeStamp = event.timeStamp,
                                delta = _getDeltaForDetails(delta),
                                primaryDelta = _getPrimaryValueFromOffset(delta),
                                globalPosition = event.position
                            )
                        )
                    })
                }
            } else {
                _pendingDragOffset = _pendingDragOffset!! + delta
                _lastPendingEventTimestamp = event.timeStamp
                if (_hasSufficientPendingDragDeltaToAccept()) {
                    resolve(GestureDisposition.accepted)
                }
            }
        }
        stopTrackingIfPointerNoLongerDown(event)
    }

    override fun acceptGesture(pointer: Int) {
        if (_state != _DragState.accepted) {
            _state = _DragState.accepted
            val delta: Offset = _pendingDragOffset!!
            val timestamp: Duration = _lastPendingEventTimestamp!!
            _pendingDragOffset = Offset.zero
            _lastPendingEventTimestamp = null
            onStart?.let {
                invokeCallback("onStart", {
                    it(
                        DragStartDetails(
                            sourceTimeStamp = timestamp,
                            globalPosition = _initialPosition!!
                        )
                    )
                })
            }
            if (delta != Offset.zero) {
                onUpdate?.let {
                    invokeCallback("onUpdate", {
                        it(
                            DragUpdateDetails(
                                sourceTimeStamp = timestamp,
                                delta = _getDeltaForDetails(delta),
                                primaryDelta = _getPrimaryValueFromOffset(delta),
                                globalPosition = _initialPosition!!
                            )
                        )
                    })
                }
            }
        }
    }

    override fun rejectGesture(pointer: Int) {
        stopTrackingPointer(pointer)
    }

    override fun didStopTrackingLastPointer(pointer: Int) {
        if (_state == _DragState.possible) {
            resolve(GestureDisposition.rejected)
            _state = _DragState.ready
            onCancel?.let {
                invokeCallback("onCancel", it)
            }
            return
        }
        val wasAccepted = _state == _DragState.accepted
        _state = _DragState.ready
        val onEnd = onEnd
        if (wasAccepted && onEnd != null) {
            val tracker: VelocityTracker? = _velocityTrackers.get(pointer)
            assert(tracker != null)

            val estimate: VelocityEstimate? = tracker!!.getVelocityEstimate()
            if (estimate != null && _isFlingGesture(estimate)) {
                val velocity: Velocity =
                    Velocity(pixelsPerSecond = estimate.pixelsPerSecond)
                        .clampMagnitude(
                            minFlingVelocity ?: kMinFlingVelocity,
                            maxFlingVelocity ?: kMaxFlingVelocity
                        )
                invokeCallback("onEnd", {
                    onEnd(
                        DragEndDetails(
                            velocity = velocity,
                            primaryVelocity = _getPrimaryValueFromOffset(velocity.pixelsPerSecond)
                        )
                    )
                }, debugReport = {
                    "$estimate; fling at $velocity."
                })
            } else {
                invokeCallback("onEnd", {
                    onEnd(
                        DragEndDetails(
                            velocity = Velocity.zero,
                            primaryVelocity = 0.0
                        )
                    )
                }, debugReport = {
                    if (estimate == null) {
                        "Could not estimate velocity."
                    } else {
                        "$estimate; judged to not be a fling."
                    }
                })
            }
        }
        _velocityTrackers.clear()
    }

    override fun dispose() {
        _velocityTrackers.clear()
        super.dispose()
    }
}