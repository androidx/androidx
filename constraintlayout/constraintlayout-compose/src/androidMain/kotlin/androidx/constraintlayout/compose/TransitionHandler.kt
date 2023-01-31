/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.coroutines.coroutineContext

/**
 * Helper class that handles the interactions between Compose and
 * [androidx.constraintlayout.core.state.Transition].
 */
@OptIn(ExperimentalComposeApi::class)
@PublishedApi
@ExperimentalMotionApi
internal class TransitionHandler(
    private val motionMeasurer: MotionMeasurer,
    private val motionProgress: MotionProgress
) {
    private val transition: androidx.constraintlayout.core.state.Transition
        get() = motionMeasurer.transition

    /**
     * The [motionProgress] is updated based on the [Offset] from a single drag event.
     */
    fun updateProgressOnDrag(dragAmount: Offset) {
        val progressDelta = transition.dragToProgress(
            motionProgress.currentProgress,
            motionMeasurer.layoutCurrentWidth,
            motionMeasurer.layoutCurrentHeight,
            dragAmount.x,
            dragAmount.y
        )
        var newProgress = motionProgress.currentProgress + progressDelta
        newProgress = newProgress.coerceIn(0f, 1f)
        motionProgress.updateProgress(newProgress)
    }

    /**
     * Called when a swipe event ends, sets up the underlying Transition with the [velocity] of the
     * swipe at the next frame..
     */
    suspend fun onTouchUp(velocity: Velocity) {
        coroutineContext.monotonicFrameClock.withFrameNanos { timeNanos ->
            transition.setTouchUp(motionProgress.currentProgress, timeNanos, velocity.x, velocity.y)
        }
    }

    /**
     * Call to update the [motionProgress] after a swipe has ended and as long as there are no other
     * touch gestures.
     */
    suspend fun updateProgressWhileTouchUp() {
        val newProgress = coroutineContext.monotonicFrameClock.withFrameNanos { timeNanos ->
            transition.getTouchUpProgress(timeNanos)
        }
        motionProgress.updateProgress(newProgress)
    }

    /**
     * Returns true if the progress is still expected to be updated by [updateProgressWhileTouchUp].
     */
    fun pendingProgressWhileTouchUp(): Boolean {
        return transition.isTouchNotDone(motionProgress.currentProgress)
    }
}