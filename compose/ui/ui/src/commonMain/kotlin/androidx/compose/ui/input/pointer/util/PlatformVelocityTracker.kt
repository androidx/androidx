/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach

/**
 * This is an interface for calculating the velocity of a pointer based on tracked pointer events or
 * positions and timestamps. The implementation of this interface is platform-specific, so the
 * output result may vary depending on the platform.
 */
internal interface PlatformVelocityTracker {
    /**
     * Track the positions and timestamps inside this event change.
     *
     * @param event Pointer change to track.
     * @param offset An offset that should be applied to the position of the [event] before adding
     *   it to the tracker.
     */
    fun addPointerInputChange(event: PointerInputChange, offset: Offset)

    /** Adds a position at the given time to the tracker. */
    fun addPosition(timeMillis: Long, position: Offset)

    /**
     * Computes the estimated velocity of the pointer at the time of the last provided data point.
     *
     * @param maximumVelocity the absolute values of the X and Y maximum velocities to be returned
     *   in units/second. `units` is the units of the positions provided to this VelocityTracker.
     */
    fun calculateVelocity(maximumVelocity: Velocity): Velocity

    /** Clears the tracked positions added by [addPosition] and [addPointerInputChange]. */
    fun resetTracking()
}

/** Create an instance of the platform-specific velocity tracker. */
internal expect fun PlatformVelocityTracker(): PlatformVelocityTracker

/**
 * Computes a pointer's velocity. Provides default implementation of the [PlatformVelocityTracker].
 */
@OptIn(ExperimentalVelocityTrackerApi::class)
internal class DefaultVelocityTracker : PlatformVelocityTracker {
    private val strategy =
        if (VelocityTrackerStrategyUseImpulse) {
            VelocityTracker1D.Strategy.Impulse
        } else {
            VelocityTracker1D.Strategy.Lsq2 // non-differential, Lsq2 1D velocity tracker
        }
    private val xVelocityTracker = VelocityTracker1D(strategy = strategy)
    private val yVelocityTracker = VelocityTracker1D(strategy = strategy)

    internal var currentPointerPositionAccumulator = Offset.Zero
    internal var lastMoveEventTimeStamp = 0L

    override fun addPosition(timeMillis: Long, position: Offset) {
        xVelocityTracker.addDataPoint(timeMillis, position.x)
        yVelocityTracker.addDataPoint(timeMillis, position.y)
    }

    override fun calculateVelocity(maximumVelocity: Velocity): Velocity {
        checkPrecondition(maximumVelocity.x > 0f && maximumVelocity.y > 0) {
            "maximumVelocity should be a positive value. You specified=$maximumVelocity"
        }
        val velocityX = xVelocityTracker.calculateVelocity(maximumVelocity.x)
        val velocityY = yVelocityTracker.calculateVelocity(maximumVelocity.y)
        return Velocity(velocityX, velocityY)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun addPointerInputChange(event: PointerInputChange, offset: Offset) {
        if (VelocityTrackerAddPointsFix) {
            addPointerInputChangeWithFix(event, offset)
        } else {
            addPointerInputChangeLegacy(event, offset)
        }
    }

    override fun resetTracking() {
        xVelocityTracker.resetTracking()
        yVelocityTracker.resetTracking()
        lastMoveEventTimeStamp = 0L
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun addPointerInputChangeLegacy(event: PointerInputChange, offset: Offset) {

        // Register down event as the starting point for the accumulator
        if (event.changedToDownIgnoreConsumed()) {
            currentPointerPositionAccumulator = event.position
            resetTracking()
        }

        // To calculate delta, for each step we want to  do currentPosition - previousPosition.
        // Initially the previous position is the previous position of the current event
        var previousPointerPosition = event.previousPosition
        @OptIn(ExperimentalComposeUiApi::class)
        event.historical.fastForEach {
            // Historical data happens within event.position and event.previousPosition
            // That means, event.previousPosition < historical data < event.position
            // Initially, the first delta will happen between the previousPosition and
            // the first position in historical delta. For subsequent historical data, the
            // deltas happen between themselves. That's why we need to update
            // previousPointerPosition
            // everytime.
            val historicalDelta = it.position - previousPointerPosition
            previousPointerPosition = it.position

            // Update the current position with the historical delta and add it to the tracker
            currentPointerPositionAccumulator += historicalDelta
            addPosition(it.uptimeMillis, currentPointerPositionAccumulator + offset)
        }

        // For the last position in the event
        // If there's historical data, the delta is event.position - lastHistoricalPoint
        // If there's no historical data, the delta is event.position - event.previousPosition
        val delta = event.position - previousPointerPosition
        currentPointerPositionAccumulator += delta
        addPosition(event.uptimeMillis, currentPointerPositionAccumulator + offset)
    }

    private fun addPointerInputChangeWithFix(event: PointerInputChange, offset: Offset) {
        // If this is ACTION_DOWN: Reset the tracking.
        if (event.changedToDownIgnoreConsumed()) {
            resetTracking()
        }

        // If this is not ACTION_UP event: Add events to the tracker as per the platform
        // implementation.
        // In the platform implementation the historical events array is used, they store the
        // current
        // event data in the position HistoricalArray.Size. Our historical array doesn't have access
        // to the final position, but we can get that information from the original event data X and
        // Y coordinates.
        if (!event.changedToUpIgnoreConsumed()) {
            event.historical.fastForEach {
                addPosition(it.uptimeMillis, it.originalEventPosition + offset)
            }
            addPosition(event.uptimeMillis, event.originalEventPosition + offset)
        }

        // If this is ACTION_UP. Fix for b/238654963. If there's been enough time after the last
        // MOVE event, reset the tracker.
        if (
            event.changedToUpIgnoreConsumed() && (event.uptimeMillis - lastMoveEventTimeStamp) > 40L
        ) {
            resetTracking()
        }
        lastMoveEventTimeStamp = event.uptimeMillis
    }
}
