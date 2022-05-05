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

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import kotlin.math.abs

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

        var lastValue = 0f
        val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
        val isAFling = abs(initialVelocity) > 1f && visibleItemsInfo.size > 1
        val finalTarget = if (isAFling) {
            // Target we will land on given initialVelocity & decay
            val decayTarget = decay.calculateTargetValue(0f, initialVelocity)

            animationState.animateDecay(decay) {
                val delta = value - lastValue
                scrollBy(delta)
                lastValue = value

                // When we are "slow" enough, switch from decay to spring
                if (abs(velocity) < CASUAL_SPRING_THRESHOLD) cancelAnimation()
            }

            // Now that scrolling slowed down, adjust the animation to land in the item closest to
            // the original target. Note that the target may be off-screen, in that case we will
            // land on the last visible item in that direction.
            (state.layoutInfo.visibleItemsInfo
                .map { animationState.value + it.unadjustedOffset + snapOffset }
                .minByOrNull { abs(it - decayTarget) } ?: decayTarget)
        } else {
            // Not a fling, just snap to the current item.
            (snapOffset - state.centerItemScrollOffset).toFloat()
        }

        animationState.animateTo(
            targetValue = finalTarget,
            sequentialAnimation = isAFling,
            animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
        ) {
            scrollBy(value - lastValue)
            lastValue = value
        }

        return animationState.velocity
    }

    // Speed, in pixels per second, to switch between decay and spring.
    private val CASUAL_SPRING_THRESHOLD = 1000
}