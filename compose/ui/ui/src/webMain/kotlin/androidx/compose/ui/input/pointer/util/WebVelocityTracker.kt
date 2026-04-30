/*
 * Copyright 2026 The Android Open Source Project
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
 * This is a copy of the DefaultVelocityTracker from the framework.
 * The only difference is that we use the WebVelocityTracker1D instead of the Android one.
 * It is required for better fling gesture handling because Browsers send touch events
 * not so often than other targets.
 */
@OptIn(ExperimentalVelocityTrackerApi::class)
internal class WebVelocityTracker : PlatformVelocityTracker {
    private val xVelocityTracker = PointerVelocityTracker1D()
    private val yVelocityTracker = PointerVelocityTracker1D()

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

    override fun resetTracking() {
        xVelocityTracker.resetTracking()
        yVelocityTracker.resetTracking()
        lastMoveEventTimeStamp = 0L
    }
}
