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

package androidx.ui.gestures.scale

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerCancelEvent
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.kMaxFlingVelocity
import androidx.ui.gestures.kMinFlingVelocity
import androidx.ui.gestures.kPanSlop
import androidx.ui.gestures.kScaleSlop
import androidx.ui.gestures.recognizer.OneSequenceGestureRecognizer
import androidx.ui.gestures.velocity_tracker.Velocity
import androidx.ui.gestures.velocity_tracker.VelocityTracker
import kotlin.math.absoluteValue

/** The possible states of a [ScaleGestureRecognizer]. */
private enum class ScaleState {
    /** The recognizer is ready to start recognizing a gesture. */
    READY,

    /**
     * The sequence of pointer events seen thus far is consistent with a scale
     * gesture but the gesture has not been accepted definitively.
     */
    POSSIBLE,

    /**
     * The sequence of pointer events seen thus far has been accepted
     * definitively as a scale gesture.
     */
    ACCEPTED,

    /**
     * The sequence of pointer events seen thus far has been accepted
     * definitively as a scale gesture and the pointers established a focal point
     * and initial scale.
     */
    STARTED
}

/** Details for [GestureScaleStartCallback]. */
class ScaleStartDetails(
    /**
     * The initial focal point of the pointers in contact with the screen.
     * Reported in global coordinates.
     */
    val focalPoint: Offset = Offset.zero
) {
    override fun toString() = "ScaleStartDetails(focalPoint: $focalPoint)"
}

/** Details for [GestureScaleUpdateCallback]. */
class ScaleUpdateDetails(
    /**
     * The focal point of the pointers in contact with the screen. Reported in
     * global coordinates.
     */
    val focalPoint: Offset = Offset.zero,
    /**
     * The scale implied by the pointers in contact with the screen. A value
     * greater than or equal to zero.
     */
    val scale: Double = 1.0
) {
    override fun toString() = "ScaleUpdateDetails(focalPoint: $focalPoint, scale: $scale)"
}

/** Details for [GestureScaleEndCallback]. */
class ScaleEndDetails(
    /** The velocity of the last pointer to be lifted off of the screen. */
    val velocity: Velocity = Velocity.zero
) {
    override fun toString() = "ScaleEndDetails(velocity: $velocity)"
}

/**
 * Signature for when the pointers in contact with the screen have established
 * a focal point and initial scale of 1.0.
 */
typealias GestureScaleStartCallback = (ScaleStartDetails) -> Unit

/**
 * Signature for when the pointers in contact with the screen have indicated a
 * new focal point and/or scale.
 */
typealias GestureScaleUpdateCallback = (ScaleUpdateDetails) -> Unit

/** Signature for when the pointers are no longer in contact with the screen. */
typealias GestureScaleEndCallback = (ScaleEndDetails) -> Unit

internal fun isFlingGesture(velocity: Velocity): Boolean {
    val speedSquared: Double = velocity.pixelsPerSecond.getDistanceSquared()
    return (speedSquared > kMinFlingVelocity * kMinFlingVelocity)
}

/**
 * Recognizes a scale gesture.
 *
 * [ScaleGestureRecognizer] tracks the pointers in contact with the screen and
 * calculates their focal point and indicated scale. When a focal pointer is
 * established, the recognizer calls [onStart]. As the focal point and scale
 * change, the recognizer calls [onUpdate]. When the pointers are no longer in
 * contact with the screen, the recognizer calls [onEnd].
 */
class ScaleGestureRecognizer(debugOwner: Any? = null) : OneSequenceGestureRecognizer(debugOwner) {

    /**
     * The pointers in contact with the screen have established a focal point and
     * initial scale of 1.0.
     */
    var onStart: GestureScaleStartCallback? = null

    /**
     * The pointers in contact with the screen have indicated a new focal point
     * and/or scale.
     */
    var onUpdate: GestureScaleUpdateCallback? = null

    /** The pointers are no longer in contact with the screen. */
    var onEnd: GestureScaleEndCallback? = null

    private var state: ScaleState = ScaleState.READY

    private var initialFocalPoint: Offset? = null
    private var currentFocalPoint: Offset? = null
    private var initialSpan: Double = 0.0
    private var currentSpan: Double = 0.0
    private var pointerLocations: MutableMap<Int, Offset>? = null
    private val velocityTrackers: MutableMap<Int, VelocityTracker> = mutableMapOf()

    private fun scaleFactor() = if (initialSpan > 0.0) currentSpan / initialSpan else 1.0

    override fun addPointer(event: PointerDownEvent) {
        startTrackingPointer(event.pointer)
        velocityTrackers[event.pointer] = VelocityTracker()
        if (state == ScaleState.READY) {
            state = ScaleState.POSSIBLE
            initialSpan = 0.0
            currentSpan = 0.0
            pointerLocations = mutableMapOf()
        }
    }

    override fun handleEvent(event: PointerEvent) {
        assert(state != ScaleState.READY)
        var didChangeConfiguration = false
        var shouldStartIfAccepted = false
        if (event is PointerMoveEvent) {
            val tracker: VelocityTracker? = velocityTrackers.get(event.pointer)
            assert(tracker != null)
            if (!event.synthesized) {
                tracker!!.addPosition(event.timeStamp, event.position)
            }
            pointerLocations!![event.pointer] = event.position
            shouldStartIfAccepted = true
        } else if (event is PointerDownEvent) {
            pointerLocations!![event.pointer] = event.position
            didChangeConfiguration = true
            shouldStartIfAccepted = true
        } else if (event is PointerUpEvent || event is PointerCancelEvent) {
            pointerLocations!!.remove(event.pointer)
            didChangeConfiguration = true
        }

        update()
        if (!didChangeConfiguration || reconfigure(event.pointer)) {
            advanceStateMachine(shouldStartIfAccepted)
        }
        stopTrackingIfPointerNoLongerDown(event)
    }

    private fun update() {
        val count: Int = pointerLocations!!.keys.size

        // Compute the focal point
        var focalPoint: Offset = Offset.zero
        pointerLocations!!.values.forEach {
            focalPoint += it
        }

        currentFocalPoint = if (count > 0) focalPoint / count.toDouble() else Offset.zero

        // Span is the average deviation from focal point
        var totalDeviation = 0.0
        pointerLocations!!.values.forEach {
            totalDeviation += (currentFocalPoint!! - it).getDistance()
        }
        currentSpan = if (count > 0) totalDeviation / count else 0.0
    }

    private fun reconfigure(pointer: Int): Boolean {
        initialFocalPoint = currentFocalPoint
        initialSpan = currentSpan
        if (state == ScaleState.STARTED) {
            onEnd?.let {
                val tracker: VelocityTracker? = velocityTrackers[pointer]
                assert(tracker != null)

                var velocity: Velocity = tracker!!.getVelocity()
                if (isFlingGesture(velocity)) {
                    val pixelsPerSecond: Offset = velocity.pixelsPerSecond
                    if (
                        pixelsPerSecond.getDistanceSquared() > kMaxFlingVelocity * kMaxFlingVelocity
                    ) {
                        velocity = Velocity(
                            pixelsPerSecond =
                            pixelsPerSecond / pixelsPerSecond.getDistance() * kMaxFlingVelocity
                        )
                    }
                    invokeCallback("onEnd", {
                        it(ScaleEndDetails(velocity = velocity))
                    })
                } else {
                    invokeCallback("onEnd", {
                        it(ScaleEndDetails(velocity = Velocity.zero))
                    })
                }
            }
            state = ScaleState.ACCEPTED
            return false
        }
        return true
    }

    private fun advanceStateMachine(shouldStartIfAccepted: Boolean) {
        if (state == ScaleState.READY) {
            state = ScaleState.POSSIBLE
        }

        if (state == ScaleState.POSSIBLE) {
            val spanDelta = (currentSpan - initialSpan).absoluteValue
            val focalPointDelta = (currentFocalPoint!! - initialFocalPoint!!).getDistance()
            if (spanDelta > kScaleSlop || focalPointDelta > kPanSlop) {
                resolve(GestureDisposition.accepted)
            }
        } else if (state.ordinal >= ScaleState.ACCEPTED.ordinal) {
            resolve(GestureDisposition.accepted)
        }

        if (state == ScaleState.ACCEPTED && shouldStartIfAccepted) {
            state = ScaleState.STARTED
            dispatchOnStartCallbackIfNeeded()
        }

        if (state == ScaleState.STARTED) {
            onUpdate?.let {
                invokeCallback("onUpdate", {
                    it(
                        ScaleUpdateDetails(
                            scale = scaleFactor(),
                            focalPoint = currentFocalPoint!!
                        )
                    )
                })
            }
        }
    }

    private fun dispatchOnStartCallbackIfNeeded() {
        assert(state == ScaleState.STARTED)
        onStart?.let {
            invokeCallback("onStart", {
                it(ScaleStartDetails(focalPoint = currentFocalPoint!!))
            })
        }
    }

    override fun acceptGesture(pointer: Int) {
        if (state == ScaleState.POSSIBLE) {
            state = ScaleState.STARTED
            dispatchOnStartCallbackIfNeeded()
        }
    }

    override fun rejectGesture(pointer: Int) {
        stopTrackingPointer(pointer)
    }

    override fun didStopTrackingLastPointer(pointer: Int) {
        when (state) {
            ScaleState.POSSIBLE -> resolve(GestureDisposition.rejected)
            ScaleState.READY -> assert(false)
            ScaleState.STARTED -> assert(false)
            ScaleState.ACCEPTED -> {
            }
        }
        state = ScaleState.READY
    }

    override fun dispose() {
        velocityTrackers.clear()
        super.dispose()
    }

    override val debugDescription = "scale"
}