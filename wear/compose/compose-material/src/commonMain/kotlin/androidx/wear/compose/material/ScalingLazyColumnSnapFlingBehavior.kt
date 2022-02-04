/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.animation.core.AnimationScope
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import kotlin.math.abs
import kotlin.math.roundToInt

internal class ScalingLazyColumnSnapFlingBehavior(
    val state: ScalingLazyListState,
    val snapOffset: Int = 0,
    val decay: DecayAnimationSpec<Float> = exponentialDecay()
) : FlingBehavior {

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val animationState = AnimationState(
            initialValue = 0f,
            initialVelocity = initialVelocity,
        )

        // Is it actually a fling?
        val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
        if (abs(initialVelocity) > 1f && visibleItemsInfo.size > 1) {
            // Target we will land on given initialVelocity & decay
            val unmodifiedTarget = decay.calculateTargetValue(0f, initialVelocity)
            val viewPortHeight = state.viewportHeightPx.value!!

            // Estimate the item closest to the target, and adjust our aim.
            val totalSize = visibleItemsInfo.last().unadjustedOffset -
                visibleItemsInfo.first().unadjustedOffset
            val estimatedItemDistance = totalSize.toFloat() / (visibleItemsInfo.size - 1)
            val centerOffset = state.centerItemScrollOffset
            val itemsToTarget = (unmodifiedTarget + centerOffset) / estimatedItemDistance

            val estimatedTarget = itemsToTarget.roundToInt() * estimatedItemDistance -
                centerOffset + snapOffset

            animationState.animateDecayTo(estimatedTarget, decay) { delta ->
                val consumed = scrollBy(delta)

                // Check if the target entered the screen
                if (abs(value - estimatedTarget) < viewPortHeight / 2) {
                    this.cancelAnimation()
                }
                consumed
            }

            // Now that the target position is visible, adjust the animation to land on the
            // closest item.
            val finalTarget = (state.layoutInfo.visibleItemsInfo
                .map { animationState.value + it.unadjustedOffset + snapOffset }
                .minByOrNull { abs(it - estimatedTarget) } ?: estimatedTarget)

            animationState.animateDecayTo(
                finalTarget,
                decay,
                sequentialAnimation = true
            ) { delta -> scrollBy(delta) }
        } else {
            // The fling was too slow (or not even a fling), just animate a snap to the item
            // already in the center.
            var lastValue = 0f
            animationState.animateTo(
                targetValue = -(state.centerItemScrollOffset - snapOffset).toFloat(),
                sequentialAnimation = true
            ) {
                scrollBy(value - lastValue)
                lastValue = value
            }
        }
        return animationState.velocity
    }

    private suspend fun <V : AnimationVector> AnimationState<Float, V>.animateDecayTo(
        targetValue: Float,
        decay: DecayAnimationSpec<Float>,
        // Indicates whether the animation should start from last frame
        sequentialAnimation: Boolean = false,
        block: AnimationScope<Float, V>.(delta: Float) -> Float
    ) {
        var lastValue = value
        val target = decay.calculateTargetValue(initialValue = value, initialVelocity = velocity)
        val velocityAdjustment = (targetValue - value) / (target - value)
        animateDecay(decay, sequentialAnimation = sequentialAnimation) {
            val delta = (value - lastValue) * velocityAdjustment
            val consumed = block(delta)
            lastValue = value

            // avoid rounding errors and stop if anything is unconsumed
            if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
        }
    }
}