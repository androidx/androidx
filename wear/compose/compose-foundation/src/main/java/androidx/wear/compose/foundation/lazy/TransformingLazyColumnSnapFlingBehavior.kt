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

package androidx.wear.compose.foundation.lazy

import android.util.Log
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.sqrt

internal class TransformingLazyColumnSnapFlingBehavior(
    val state: TransformingLazyColumnState,
    val snapOffset: Int = 0,
    val decay: DecayAnimationSpec<Float> = exponentialDecay(),
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (initialVelocity.isNaN()) {
            Log.w(
                "WearCompose",
                "TransformingLazyColumnSnapFlingBehavior: ScrollScope.performFling called with initialVelocity NaN. Please use a valid value.",
            )
            return Float.NaN
        }

        val animationState = AnimationState(initialValue = 0f, initialVelocity = initialVelocity)
        var lastValue = 0f

        val visibleItemsInfo = state.layoutInfo.visibleItems
        val isAFling = abs(initialVelocity) > 1f && visibleItemsInfo.size > 1

        val finalTarget =
            if (isAFling) {
                // Target we will land on given initialVelocity & decay
                val decayTarget = decay.calculateTargetValue(0f, initialVelocity)
                var endOfListReached = false

                animationState.animateDecay(decay) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value

                    // When we are "slow" enough, switch from decay to the final snap.
                    if (abs(velocity) < SNAP_SPEED_THRESHOLD) cancelAnimation()

                    // If we can't consume the scroll, also stop.
                    if (abs(delta - consumed) > 0.1f) {
                        endOfListReached = true
                        cancelAnimation()
                    }
                }

                if (endOfListReached) {
                    // We couldn't scroll as much as we wanted, likely we reached the end of the
                    // list, Snap to the current item and finish.
                    scrollBy((snapOffset - state.anchorItemScrollOffset).toFloat())
                    return animationState.velocity
                } else {
                    calculateFlingTarget(
                        decayTarget = decayTarget,
                        currentScrollOffset = animationState.value,
                    )
                }
            } else {
                // Not a fling, just snap to the current item.
                (snapOffset - state.anchorItemScrollOffset).toFloat()
            }

        // We have a velocity (animationState.velocity), and a target (finalTarget),
        // Construct a cubic bezier with the given initial velocity, and ending at 0 speed,
        // unless that will mean that we need to accelerate and decelerate.
        // We can also control the inertia of these speeds, i.e. how much it will accelerate/
        // decelerate at the beginning and end.
        val distance = finalTarget - animationState.value

        // If the distance to fling is zero, nothing to do (and must avoid divide-by-zero below).
        if (distance != 0.0f) {
            val initialSpeed = animationState.velocity

            // Inertia of the initial speed.
            val initialInertia = 0.5f

            // Compute how much time we want to spend on the final snap, depending on the speed
            val finalSnapDuration =
                lerp(
                    FINAL_SNAP_DURATION_MIN,
                    FINAL_SNAP_DURATION_MAX,
                    abs(initialSpeed) / SNAP_SPEED_THRESHOLD,
                )

            // Initial control point. Has slope (velocity) adjustedSpeed and magnitude (inertia)
            // initialInertia
            val adjustedSpeed = initialSpeed * finalSnapDuration / distance
            val easingX0 = initialInertia / sqrt(1f + adjustedSpeed * adjustedSpeed)
            val easingY0 = easingX0 * adjustedSpeed

            // Final control point. Has slope 0, unless that will make us accelerate then
            // decelerate, in that case we set the slope to 1.
            val easingX1 = 0.8f
            val easingY1 = if (easingX0 > easingY0) 0.8f else 1f

            animationState.animateTo(
                finalTarget,
                tween(
                    (finalSnapDuration * 1000).fastRoundToInt(),
                    easing = CubicBezierEasing(easingX0, easingY0, easingX1, easingY1),
                ),
            ) {
                scrollBy(value - lastValue)
                lastValue = value
            }

            // We consider all velocity consumed
            return 0.0f
        }

        return animationState.velocity
    }

    /**
     * Calculates the best snapping target by reconstructing the linear layout structure relative to
     * the anchor item. This accounts for the fact that scrollBy() moves the anchor linearly, even
     * though other items transformed.
     */
    private fun calculateFlingTarget(decayTarget: Float, currentScrollOffset: Float): Float {
        val layoutInfo = state.layoutInfoState.value
        val visibleItems = layoutInfo.visibleItems
        // Find the index of the anchor within the visible items list
        val anchorVisualIndex = visibleItems.indexOfFirst { it.index == state.anchorItemIndex }
        if (anchorVisualIndex == -1 || visibleItems.isEmpty()) {
            return decayTarget
        }

        val anchorItem = visibleItems[anchorVisualIndex]
        val viewportCenter = layoutInfo.viewportSize.height / 2
        val itemSpacing = layoutInfo.itemSpacing

        var bestTarget = decayTarget
        var minDiff = Float.MAX_VALUE
        // Helper to check if a calculated target is the best one so far
        fun checkCandidate(untransformedOffset: Int, untransformedHeight: Int) {
            val distToCenter = untransformedOffset + (untransformedHeight / 2) - viewportCenter
            val target = currentScrollOffset + distToCenter + snapOffset
            val diff = abs(target - decayTarget)
            if (diff < minDiff) {
                minDiff = diff
                bestTarget = target
            }
        }
        // Check Anchor and items after it (Forward pass)
        var currentUntransformedOffset = anchorItem.offset
        for (i in anchorVisualIndex until visibleItems.size) {
            val item = visibleItems[i]
            if (i > anchorVisualIndex) {
                val prevItem = visibleItems[i - 1]
                currentUntransformedOffset += prevItem.measuredHeight + itemSpacing
            }
            checkCandidate(currentUntransformedOffset, item.measuredHeight)
        }
        // Check items before Anchor (Backward pass)
        currentUntransformedOffset = anchorItem.offset
        for (i in anchorVisualIndex - 1 downTo 0) {
            val item = visibleItems[i]
            currentUntransformedOffset -= (item.measuredHeight + itemSpacing)
            checkCandidate(currentUntransformedOffset, item.measuredHeight)
        }
        return bestTarget
    }

    private companion object {
        // Speed, in pixels per second, to switch between decay and final snap.
        private const val SNAP_SPEED_THRESHOLD = 1200f

        // Minimum duration for the final snap after the fling, in seconds, used when the initial
        // speed is 0
        private const val FINAL_SNAP_DURATION_MIN = .1f

        // Maximum duration for the final snap after the fling, in seconds, used when the speed is
        // SNAP_SPEED_THRESHOLD
        private const val FINAL_SNAP_DURATION_MAX = .35f
    }
}
