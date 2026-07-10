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

import android.os.Build
import android.view.MotionEvent
import android.view.VelocityTracker
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.unit.Velocity

/** Create an instance of the platform-specific velocity tracker. */
@OptIn(ExperimentalComposeUiApi::class)
internal actual fun PlatformVelocityTracker(): PlatformVelocityTracker =
    if (AndroidComposeUiFlags.isFrameworkVelocityTrackerEnabled) FrameworkVelocityTracker()
    else Lsq2VelocityTracker()

/** Computes a pointer's velocity using the Android Framework's VelocityTracker. */
internal class FrameworkVelocityTracker : PlatformVelocityTracker {
    private lateinit var velocityTracker: VelocityTracker

    private var lastEventTimeMillis = 0L

    override fun addPointerInputChange(event: PointerInputChange, offset: Offset) {
        if (event.changedToDownIgnoreConsumed()) {
            addMovement(
                event.uptimeMillis,
                MotionEvent.ACTION_DOWN,
                event.originalEventPosition + offset,
            )
        } else if (!event.changedToUpIgnoreConsumed()) {
            // TODO(b/359962905): Ignore resampled events.
            if (event.historical.isNotEmpty()) {
                // Create a MotionEvent containing the historical events.
                val oldestEvent = event.historical.first()
                val motionEvent =
                    obtainMotionEvent(
                        oldestEvent.uptimeMillis,
                        MotionEvent.ACTION_MOVE,
                        oldestEvent.originalEventPosition + offset,
                    )
                for (i in 1..event.historical.lastIndex) {
                    val position = event.historical[i].originalEventPosition + offset
                    motionEvent.addBatch(
                        event.historical[i].uptimeMillis,
                        position.x,
                        position.y,
                        0f, /* pressure */
                        0f, /* size */
                        0, /* metaState */
                    )
                }

                // Add the current event to the MotionEvent.
                val position = event.originalEventPosition + offset
                motionEvent.addBatch(
                    event.uptimeMillis,
                    event.originalEventPosition.x + offset.x,
                    event.originalEventPosition.y + offset.y,
                    0f, /* pressure */
                    0f, /* size */
                    0, /* metaState */
                )
                consumeMotionEvent(motionEvent)
            } else {
                addMovement(
                    event.uptimeMillis,
                    MotionEvent.ACTION_MOVE,
                    event.originalEventPosition + offset,
                )
            }
        } else if (event.changedToUpIgnoreConsumed()) {
            addMovement(
                event.uptimeMillis,
                MotionEvent.ACTION_UP,
                event.originalEventPosition + offset,
            )
        }
    }

    override fun addPosition(timeMillis: Long, position: Offset) =
        addMovement(timeMillis, MotionEvent.ACTION_MOVE, position)

    internal fun addMovement(timeMillis: Long, action: Int, position: Offset) =
        consumeMotionEvent(obtainMotionEvent(timeMillis, action, position))

    internal fun obtainMotionEvent(timeMillis: Long, action: Int, position: Offset) =
        MotionEvent.obtain(
            0 /* downTime */,
            timeMillis /* eventTime */,
            action,
            position.x,
            position.y,
            0, /* metaState */
        )

    internal fun consumeMotionEvent(motionEvent: MotionEvent) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            // On older versions of Android, this test exists in VelocityTracker, but is not
            // implemented consistently.
            val intervalMillis = motionEvent.eventTime - lastEventTimeMillis
            if (intervalMillis > 40L) { // ASSUME_POINTER_STOPPED_TIME from VelocityTracker.cpp
                resetTracking()
            }
            lastEventTimeMillis = motionEvent.eventTime
        }
        if (!this::velocityTracker.isInitialized) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker.addMovement(motionEvent)
        motionEvent.recycle()
    }

    override fun calculateVelocity(maximumVelocity: Velocity): Velocity {
        checkPrecondition(maximumVelocity.x > 0f && maximumVelocity.y > 0) {
            "maximumVelocity should be a positive value. You specified=$maximumVelocity"
        }
        if (!this::velocityTracker.isInitialized) {
            return Velocity.Zero
        }
        velocityTracker.computeCurrentVelocity(1000) // Units per second.
        return Velocity(
            Math.clamp(velocityTracker.xVelocity, -maximumVelocity.x, maximumVelocity.x),
            Math.clamp(velocityTracker.yVelocity, -maximumVelocity.y, maximumVelocity.y),
        )
    }

    override fun resetTracking() {
        if (this::velocityTracker.isInitialized) {
            velocityTracker.clear()
        }
    }
}
