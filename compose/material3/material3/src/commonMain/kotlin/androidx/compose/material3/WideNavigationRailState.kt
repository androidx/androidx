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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.material3.internal.AnchoredDraggableState
import androidx.compose.material3.internal.snapTo
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@ExperimentalMaterial3ExpressiveApi
/** Possible values of [ModalExpandedNavigationRailState]. */
enum class ModalExpandedNavigationRailValue {
    /** The state of the modal expanded navigation rail when it is closed. */
    Closed,

    /** The state of the modal expanded navigation rail when it is open. */
    Open,
}

/**
 * State of a modal expanded navigation rail, such as [ModalExpandedNavigationRail].
 *
 * Contains states relating to its swipe position as well as animations between state values.
 *
 * @param initialValue The initial value of the state
 * @param density The density that this state can use to convert values to and from dp
 * @param animationSpec The animation spec that will be used to animate to a new state
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change
 */
@Suppress("NotCloseable")
@ExperimentalMaterial3ExpressiveApi
class ModalExpandedNavigationRailState(
    var initialValue: ModalExpandedNavigationRailValue,
    density: Density,
    val animationSpec: AnimationSpec<Float>,
    var confirmValueChange: (ModalExpandedNavigationRailValue) -> Boolean = { true },
) {
    internal val anchoredDraggableState =
        AnchoredDraggableState(
            initialValue = initialValue,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 400.dp.toPx() } },
            animationSpec = { animationSpec },
            confirmValueChange = confirmValueChange
        )

    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the value the modal expanded
     * navigation rail is currently in. If a swipe or an animation is in progress, this corresponds
     * to the value the rail was in before the swipe or animation started.
     */
    val currentValue: ModalExpandedNavigationRailValue
        get() = anchoredDraggableState.currentValue

    /**
     * The target value of the modal expanded navigation rail state.
     *
     * If a swipe is in progress, this is the value that the modal rail will animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: ModalExpandedNavigationRailValue
        get() = anchoredDraggableState.targetValue

    /** Whether the modal expanded navigation rail is open. */
    val isOpen: Boolean
        get() = currentValue != ModalExpandedNavigationRailValue.Closed

    /** Whether the state is currently animating. */
    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * Open the modal expanded navigation rail with animation and suspend until it if fully open or
     * the animation has been cancelled. This method will throw CancellationException if the
     * animation is interrupted.
     *
     * @return the reason the expand animation ended
     */
    suspend fun open() = animateTo(ModalExpandedNavigationRailValue.Open)

    /**
     * Close the modal expanded navigation rail with animation and suspend until it is fully closed
     * or the animation has been cancelled. This method will throw CancellationException if the
     * animation interrupted.
     *
     * @return the reason the collapse animation ended
     */
    suspend fun close() = animateTo(ModalExpandedNavigationRailValue.Closed)

    /**
     * Set the state without any animation and suspend until it's set.
     *
     * @param targetValue The new target value
     */
    suspend fun snapTo(targetValue: ModalExpandedNavigationRailValue) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    internal suspend fun settle(velocity: Float) {
        anchoredDraggableState.settle(velocity)
    }

    /**
     * The current position (in pixels) of the rail, or Float.NaN before the offset is initialized.
     *
     * @see [AnchoredDraggableState.offset] for more information.
     */
    val currentOffset: Float
        get() = anchoredDraggableState.offset

    private suspend fun animateTo(
        targetValue: ModalExpandedNavigationRailValue,
        animationSpec: AnimationSpec<Float> = this.animationSpec,
        velocity: Float = anchoredDraggableState.lastVelocity
    ) {
        anchoredDraggableState.anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
            val targetOffset = anchors.positionOf(latestTarget)
            if (!targetOffset.isNaN()) {
                var prev = if (currentOffset.isNaN()) 0f else currentOffset
                animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                    // Our onDrag coerces the value within the bounds, but an animation may
                    // overshoot, for example a spring animation or an overshooting interpolator.
                    // We respect the user's intention and allow the overshoot, but still use
                    // DraggableState's drag for its mutex.
                    dragTo(value, velocity)
                    prev = value
                }
            }
        }
    }

    companion object {
        /** The default [Saver] implementation for [ModalExpandedNavigationRailState]. */
        fun Saver(
            density: Density,
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (ModalExpandedNavigationRailValue) -> Boolean
        ) =
            Saver<ModalExpandedNavigationRailState, ModalExpandedNavigationRailValue>(
                save = { it.currentValue },
                restore = {
                    ModalExpandedNavigationRailState(it, density, animationSpec, confirmStateChange)
                }
            )
    }
}

/**
 * Create and [remember] a [ModalExpandedNavigationRailState].
 *
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberModalExpandedNavigationRailState(
    confirmValueChange: (ModalExpandedNavigationRailValue) -> Boolean = { true }
): ModalExpandedNavigationRailState {
    val density = LocalDensity.current
    // TODO: Load the motionScheme tokens from the component tokens file.
    val animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value<Float>()
    return rememberSaveable(
        saver = ModalExpandedNavigationRailState.Saver(density, animationSpec, confirmValueChange)
    ) {
        ModalExpandedNavigationRailState(
            initialValue = ModalExpandedNavigationRailValue.Closed,
            density = density,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange
        )
    }
}

@Stable
internal class RailPredictiveBackState {
    var swipeEdgeMatchesRail by mutableStateOf(true)

    fun update(
        isSwipeEdgeLeft: Boolean,
        isRtl: Boolean,
    ) {
        swipeEdgeMatchesRail = (isSwipeEdgeLeft && !isRtl) || (!isSwipeEdgeLeft && isRtl)
    }
}
