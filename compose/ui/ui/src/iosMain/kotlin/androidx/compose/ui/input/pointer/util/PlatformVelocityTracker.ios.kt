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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastForEach

internal actual fun PlatformVelocityTracker(): PlatformVelocityTracker = UIKitVelocityTracker()

private const val AssumePointerMoveStoppedMilliseconds: Int = 40
private const val MinimumGestureDurationMilliseconds: Int = 50

private class UIKitVelocityTracker: PlatformVelocityTracker {
    @OptIn(ExperimentalVelocityTrackerApi::class)
    private val strategy =
        VelocityTracker1D.Strategy.Lsq2 // non-differential, Lsq2 1D velocity tracker
    private val yVelocityTracker = VelocityTracker1D(strategy = strategy)
    private val xVelocityTracker = VelocityTracker1D(strategy = strategy)
    private var lastMoveEventTimeStamp = 0L
    private var lastPointerStopEventTimeStamp = 0L

    override fun addPointerInputChange(event: PointerInputChange, offset: Offset) {
        // If this is ACTION_DOWN: Reset the tracking.
        if (event.changedToDownIgnoreConsumed()) {
            resetTracking()
        }

        // If this is not ACTION_UP event: Add events to the tracker as per the platform implementation.
        // In the platform implementation the historical events array is used, they store the current
        // event data in the position HistoricalArray.Size. Our historical array doesn't have access
        // to the final position, but we can get that information from the original event data X and Y
        // coordinates.
        if (!event.changedToUpIgnoreConsumed()) {
            event.historical.fastForEach {
                addPosition(it.uptimeMillis, it.position + offset)
            }
            addPosition(event.uptimeMillis, event.position + offset)
        }

        if (event.uptimeMillis - lastMoveEventTimeStamp > AssumePointerMoveStoppedMilliseconds) {
            lastPointerStopEventTimeStamp = event.uptimeMillis
        }

        if (event.changedToUpIgnoreConsumed() &&
            event.uptimeMillis - lastPointerStopEventTimeStamp < MinimumGestureDurationMilliseconds
        ) {
            resetTracking()
        }
        lastMoveEventTimeStamp = event.uptimeMillis
    }

    override fun calculateVelocity(maximumVelocity: Velocity): Velocity {
        val velocityX = xVelocityTracker.calculateVelocity(maximumVelocity.x)
        val velocityY = yVelocityTracker.calculateVelocity(maximumVelocity.y)
        return Velocity(velocityX, velocityY)
    }

    override fun resetTracking() {
        xVelocityTracker.resetTracking()
        yVelocityTracker.resetTracking()
        lastMoveEventTimeStamp = 0L
    }

    override fun addPosition(timeMillis: Long, position: Offset) {
        xVelocityTracker.addDataPoint(timeMillis, position.x)
        yVelocityTracker.addDataPoint(timeMillis, position.y)
    }
}
