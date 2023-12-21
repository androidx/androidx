/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class LazyLayoutAnimateItemModifierNode(
    var placementAnimationSpec: FiniteAnimationSpec<IntOffset>
) : Modifier.Node() {

    /**
     * Returns true when the placement animation is currently in progress so the parent
     * should continue composing this item.
     */
    var isAnimationInProgress by mutableStateOf(false)
        private set

    /**
     * This property is managed by the animation manager and is not directly used by this class.
     * It represents the last known offset of this item in the lazy layout coordinate space.
     * It will be updated on every scroll and is allowing the manager to track when the item
     * position changes not because of the scroll event in order to start the animation.
     * When there is an active animation it represents the final/target offset.
     */
    var rawOffset: IntOffset = NotInitialized

    private val placementDeltaAnimation = Animatable(IntOffset.Zero, IntOffset.VectorConverter)

    /**
     * Current delta to apply for a placement offset. Updates every animation frame.
     * The settled value is [IntOffset.Zero] so the animation is always targeting this value.
     */
    var placementDelta by mutableStateOf(IntOffset.Zero)
        private set

    /**
     * Cancels the ongoing animation if there is one.
     */
    fun cancelAnimation() {
        if (isAnimationInProgress) {
            coroutineScope.launch {
                placementDeltaAnimation.snapTo(IntOffset.Zero)
                placementDelta = IntOffset.Zero
                isAnimationInProgress = false
            }
        }
    }

    /**
     * Tracks the offset of the item in the lookahead pass. When set, this is the animation target
     * that placementDelta should be applied to.
     */
    var lookaheadOffset: IntOffset = NotInitialized

    /**
     * Animate the placement by the given [delta] offset.
     */
    fun animatePlacementDelta(delta: IntOffset) {
        val totalDelta = placementDelta - delta
        placementDelta = totalDelta
        isAnimationInProgress = true
        coroutineScope.launch {
            try {
                val spec = if (placementDeltaAnimation.isRunning) {
                    // when interrupted, use the default spring, unless the spec is a spring.
                    if (placementAnimationSpec is SpringSpec<IntOffset>) {
                        placementAnimationSpec
                    } else {
                        InterruptionSpec
                    }
                } else {
                    placementAnimationSpec
                }
                if (!placementDeltaAnimation.isRunning) {
                    // if not running we can snap to the initial value and animate to zero
                    placementDeltaAnimation.snapTo(totalDelta)
                }
                // if animation is not currently running the target will be zero, otherwise
                // we have to continue the animation from the current value, but keep the needed
                // total delta for the new animation.
                val animationTarget = placementDeltaAnimation.value - totalDelta
                placementDeltaAnimation.animateTo(animationTarget, spec) {
                    // placementDelta is calculated as if we always animate to target equal to zero
                    placementDelta = value - animationTarget
                }

                isAnimationInProgress = false
            } catch (_: CancellationException) {
                // we don't reset inProgress in case of cancellation as it means
                // there is a new animation started which would reset it later
            }
        }
    }

    override fun onDetach() {
        placementDelta = IntOffset.Zero
        isAnimationInProgress = false
        rawOffset = NotInitialized
        // placementDeltaAnimation will be canceled because coroutineScope will be canceled.
    }

    companion object {
        val NotInitialized = IntOffset(Int.MAX_VALUE, Int.MAX_VALUE)
    }
}

/**
 * We switch to this spec when a duration based animation is being interrupted.
 */
private val InterruptionSpec = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.VisibilityThreshold
)
