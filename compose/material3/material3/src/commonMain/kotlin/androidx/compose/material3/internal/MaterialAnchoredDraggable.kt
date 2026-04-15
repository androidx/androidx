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

package androidx.compose.material3.internal

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDragScope
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * A thin wrapper around [AnchoredDraggableState] with Material3-specific opinions.
 *
 * @param initialValue The initial value of the state.
 * @param positionalThreshold The positional threshold, in px, to be used when calculating the
 *   target state while a drag is in progress and when settling after the drag ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has to
 *   exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 *   reached.
 * @param snapAnimationSpec The default animation spec used to animate to a new state.
 * @param decayAnimationSpec The default animation spec used to animate to a new state when a fling
 *   is detected.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
internal class MaterialAnchoredDraggableState<T>(
    initialValue: T,
    internal val positionalThreshold: ((totalDistance: Float) -> Float)? = null,
    internal val velocityThreshold: (() -> Float)? = null,
    internal val snapAnimationSpec: AnimationSpec<Float> = spring(),
    internal val decayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay(),
    val confirmValueChange: (newValue: T) -> Boolean = { true },
) {

    /** The underlying [AnchoredDraggableState]. */
    @Suppress("Deprecation")
    val anchoredDraggableState =
        if (positionalThreshold != null && velocityThreshold != null) {
            androidx.compose.foundation.gestures.AnchoredDraggableState(
                initialValue = initialValue,
                positionalThreshold = positionalThreshold,
                velocityThreshold = velocityThreshold,
                snapAnimationSpec = snapAnimationSpec,
                decayAnimationSpec = decayAnimationSpec,
                confirmValueChange = confirmValueChange,
            )
        } else {
            AnchoredDraggableState(
                initialValue = initialValue,
                confirmValueChange = confirmValueChange,
            )
        }

    /**
     * The current value of the state.
     *
     * In Material3, this maps to the [AnchoredDraggableState#settledValue] of the underlying
     * foundation component. This preserves the "settled" behavior where the value only updates when
     * the component has finished settling at an anchor.
     */
    val currentValue: T
        get() = anchoredDraggableState.settledValue

    /** The touch target is currently closest to this value. */
    val closestValue: T
        get() = anchoredDraggableState.currentValue

    // TODO(b/477969920): Remove forked targetValue logic when foundation dependencies are updated.
    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the component would animate to if the swipe
     * finishes. If an animation is running, this is the target value of that animation. Finally, if
     * no swipe or animation is in progress, this is the same as the [currentValue].
     */
    val targetValue: T by derivedStateOf {
        // AnchoredDraggableState does not expose the dragTarget, but isAnimationRunning returns
        // whether AnchoredDraggableState.dragTarget is null. If it's not, we can use the
        // targetValue; otherwise we apply the calculation fix.
        if (isAnimationRunning) {
            anchoredDraggableState.targetValue
        } else {
            calculateTargetValueWithFix(offset)
        }
    }

    private fun calculateTargetValueWithFix(currentOffset: Float): T {
        return if (!currentOffset.isNaN()) {
            // DraggableAnchors allows multiple anchors with the same offsets. If the offset is
            // already equal to the currentValue's offset, this anchor gets priority.
            val currentValueOffset = anchors.positionOf(currentValue)
            if (currentValueOffset.isNaN() || currentOffset == currentValueOffset) {
                currentValue
            } else {
                anchors.closestAnchor(currentOffset) ?: currentValue
            }
        } else currentValue
    }

    /**
     * The current offset, or [Float.NaN] if it has not been initialized yet.
     *
     * The offset will be initialized when the anchors are first set through [updateAnchors].
     *
     * Strongly consider using [requireOffset] which will throw if the offset is read before it is
     * initialized. This helps catch issues early in your workflow.
     */
    val offset: Float
        get() = anchoredDraggableState.offset

    /** The current anchors. */
    val anchors: DraggableAnchors<T>
        get() = anchoredDraggableState.anchors

    /**
     * The fraction of the progress going from [currentValue] to [targetValue], within [0f..1f]
     * bounds, or 1f if the state is in a settled state.
     */
    @Suppress("Deprecation")
    val progress: Float
        get() = anchoredDraggableState.progress

    /**
     * The velocity of the last known animation. Gets reset to 0f when an animation completes
     * successfully, but does not get reset when an animation gets interrupted. You can use this
     * value to provide smooth reconciliation behavior when re-targeting an animation.
     */
    val lastVelocity: Float
        get() = anchoredDraggableState.lastVelocity

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     * @see offset
     */
    fun requireOffset(): Float = anchoredDraggableState.requireOffset()

    /** Whether an animation is currently in progress. */
    val isAnimationRunning: Boolean
        get() = anchoredDraggableState.isAnimationRunning

    /**
     * Snap to a [targetValue] without any animation. If the [targetValue] is not in the set of
     * anchors, the [currentValue] will be updated to the [targetValue] without updating the offset.
     *
     * @param targetValue The target value of the animation
     */
    suspend fun snapTo(targetValue: T) {
        anchoredDraggableState.snapTo(targetValue)
    }

    /**
     * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
     * [currentValue] will be updated to the [targetValue] without updating the offset.
     *
     * @param targetValue The target value of the animation
     */
    suspend fun animateTo(targetValue: T) {
        anchoredDraggableState.animateTo(targetValue)
    }

    /**
     * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
     * [currentValue] will be updated to the [targetValue] without updating the offset.
     *
     * @param targetValue The target value of the animation
     * @param animationSpec The animation spec to use.
     */
    suspend fun animateTo(targetValue: T, animationSpec: AnimationSpec<Float>) {
        anchoredDraggableState.animateTo(targetValue, animationSpec)
    }

    /**
     * Find the target anchor based on [velocity], [velocityThreshold] and [positionalThreshold] and
     * animate to it.
     *
     * @param velocity The velocity of the drag
     * @return The remaining velocity
     */
    @Suppress("Deprecation")
    suspend fun settle(velocity: Float): Float = anchoredDraggableState.settle(velocity)

    /**
     * Drag by the [delta], coerce it in the bounds and dispatch it to the [AnchoredDraggableState].
     *
     * @return The delta the consumed by the [AnchoredDraggableState]
     */
    fun dispatchRawDelta(delta: Float): Float = anchoredDraggableState.dispatchRawDelta(delta)

    /**
     * Call this function to take control of drag logic and perform anchored drag with the latest
     * anchors.
     *
     * All actions that change the [offset] of this state must be performed within an [anchoredDrag]
     * block (even if they don't call any other methods on this object) in order to guarantee that
     * mutual exclusion is enforced.
     *
     * If [anchoredDrag] is called from elsewhere with the [dragPriority] higher or equal to ongoing
     * drag, the ongoing drag will be cancelled.
     *
     * @param dragPriority of the drag operation
     * @param block perform anchored drag given the current anchor provided
     */
    suspend fun anchoredDrag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend AnchoredDragScope.(anchors: DraggableAnchors<T>) -> Unit,
    ) {
        anchoredDraggableState.anchoredDrag(dragPriority, block)
    }

    /**
     * Call this function to take control of drag logic and perform anchored drag with the latest
     * anchors and target.
     *
     * All actions that change the [offset] of this state must be performed within an [anchoredDrag]
     * block (even if they don't call any other methods on this object) in order to guarantee that
     * mutual exclusion is enforced.
     *
     * This overload allows the caller to hint the target value that this [anchoredDrag] is intended
     * to arrive to. This will set [targetValue] to provided value so consumers can reflect it in
     * their UIs.
     *
     * @param targetValue hint the target value that this [anchoredDrag] is intended to arrive to
     * @param dragPriority of the drag operation
     * @param block perform anchored drag given the current anchor provided
     */
    suspend fun anchoredDrag(
        targetValue: T,
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend AnchoredDragScope.(anchors: DraggableAnchors<T>, targetValue: T) -> Unit,
    ) {
        anchoredDraggableState.anchoredDrag(targetValue, dragPriority, block)
    }

    /**
     * Update the anchors. If there is no ongoing [anchoredDrag] operation, snap to the [newTarget],
     * otherwise restart the ongoing [anchoredDrag] operation (e.g. an animation) with the new
     * anchors.
     *
     * @param newAnchors The new anchors.
     * @param newTarget The new target, by default the current [targetValue].
     */
    fun updateAnchors(newAnchors: DraggableAnchors<T>, newTarget: T = targetValue) {
        anchoredDraggableState.updateAnchors(newAnchors, newTarget)
    }

    /**
     * Calculate the new offset for a [delta] to ensure it is coerced in the bounds
     *
     * @param delta The delta to be added to the [offset]
     * @return The coerced offset
     */
    fun newOffsetForDelta(delta: Float) =
        ((if (offset.isNaN()) 0f else offset) + delta).coerceIn(
            anchoredDraggableState.anchors.minPosition(),
            anchoredDraggableState.anchors.maxPosition(),
        )

    internal suspend fun anchoredDrag(flingBehavior: FlingBehavior, initialVelocity: Float): Float {
        var consumedVelocity = 0f
        anchoredDraggableState.anchoredDrag {
            val scrollScope =
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        val newOffset = newOffsetForDelta(pixels)
                        val consumed = newOffset - offset
                        dragTo(newOffset)
                        return consumed
                    }
                }
            consumedVelocity = with(flingBehavior) { scrollScope.performFling(initialVelocity) }
        }
        return consumedVelocity
    }
}

internal object MaterialAnchoredDraggableDefaults {

    /** This Modifier allows configuring a [MaterialAnchoredDraggableState] to a layout node. */
    @Composable
    internal fun <T> Modifier.materialAnchoredDraggable(
        state: MaterialAnchoredDraggableState<T>,
        orientation: Orientation,
        enabled: Boolean = true,
        reverseDirection: Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl,
        flingBehavior: FlingBehavior? = null,
        interactionSource: MutableInteractionSource? = null,
    ): Modifier {
        return this.anchoredDraggable(
            state = state.anchoredDraggableState,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            flingBehavior = flingBehavior,
            interactionSource = interactionSource,
        )
    }

    @Composable
    internal fun <T> flingBehavior(
        state: MaterialAnchoredDraggableState<T>,
        positionalThreshold: (totalDistance: Float) -> Float,
        animationSpec: AnimationSpec<Float>,
    ): TargetedFlingBehavior {
        return AnchoredDraggableDefaults.flingBehavior(
            state = state.anchoredDraggableState,
            positionalThreshold = positionalThreshold,
            animationSpec = animationSpec,
        )
    }
}
