/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.runtime.Stable

/**
 * Interface to specify fling behavior with additional information about its animation target.
 */
@Stable
interface TargetedFlingBehavior : FlingBehavior {

    /**
     * Perform settling via fling animation with given velocity and suspend until fling has
     * finished. Use [onRemainingDistanceUpdated] to report the status of the ongoing fling
     * animation and the remaining amount of scroll offset.
     *
     * This functions is called with [ScrollScope] to drive the state change of the
     * [androidx.compose.foundation.gestures.ScrollableState] via [ScrollScope.scrollBy].
     *
     * This function must return the correct velocity left after it is finished flinging in order
     * to guarantee proper nested scroll support.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     * @param onRemainingDistanceUpdated a lambda that will be called anytime the
     * distance to the settling offset is updated. The settling offset is the final offset where
     * this fling will stop and may change depending on the snapping animation progression.
     *
     * @return remaining velocity after fling operation has ended
     */
    suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit
    ): Float

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
        performFling(initialVelocity, NoOnReport)
}

private val NoOnReport: (Float) -> Unit = { }
