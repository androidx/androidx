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
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDragScope
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.animateToWithDecay
import androidx.compose.foundation.gestures.forEach
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs

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
 * @param animationSpec The default animation spec used to animate to a new state.
 * @param decayAnimationSpec The default animation spec used to animate to a new state when a fling
 *   is detected.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
internal class MaterialAnchoredDraggableState<T>(
    initialValue: T,
    val positionalThreshold: (totalDistance: Float) -> Float,
    val velocityThreshold: () -> Float,
    val animationSpec: AnimationSpec<Float>,
    val decayAnimationSpec: DecayAnimationSpec<Float>,
    val confirmValueChange: (newValue: T) -> Boolean = { true },
) {
    /** A convenient constructor for [MaterialAnchoredDraggableState] with reasonable defaults. */
    constructor(
        initialValue: T,
        confirmValueChange: (newValue: T) -> Boolean = { true },
    ) : this(
        initialValue = initialValue,
        positionalThreshold = AnchoredDraggableDefaults.PositionalThreshold,
        velocityThreshold = { 0f },
        animationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
        decayAnimationSpec = AnchoredDraggableDefaults.DecayAnimationSpec,
        confirmValueChange = confirmValueChange,
    )

    /** The underlying [AnchoredDraggableState]. */
    @Suppress("Deprecation")
    val anchoredDraggableState =
        AnchoredDraggableState(initialValue = initialValue, confirmValueChange = confirmValueChange)

    /**
     * The current value of the state.
     *
     * In Material3, this maps to the [AnchoredDraggableState#settledValue] of the underlying
     * foundation component. This preserves the "settled" behavior where the value only updates when
     * the component has finished settling at an anchor.
     */
    val currentValue: T
        get() = anchoredDraggableState.settledValue

    /** The value the touch target is currently closest to. */
    val closestValue: T
        get() = anchoredDraggableState.currentValue

    // TODO(b/477969920): Remove forked targetValue logic when foundation dependencies are updated.
    /**
     * The target value of the state.
     *
     * If an animation is running, it returns the target value of that animation. Otherwise, it
     * calculates the target value based on the [positionalThreshold].
     *
     * In Material3, this differs from foundation's [AnchoredDraggableState#targetValue] by
     * accounting for the [positionalThreshold] during active drags, even before a fling. This
     * ensures that UI elements (like background colors in SwipeToDismiss) update as soon as the
     * user crosses the threshold.
     */
    val targetValue: T by derivedStateOf {
        if (anchoredDraggableState.isAnimationRunning) {
            anchoredDraggableState.targetValue
        } else {
            calculateTargetValue(offset, currentValue, anchors)
        }
    }

    /**
     * The value the state is currently settled at.
     *
     * When progressing through multiple anchors, e.g. A -> B -> C, [settledValue] will stay the
     * same until settled at an anchor, while foundation's currentValue would update to the closest
     * anchor.
     */
    val settledValue: T
        get() = anchoredDraggableState.settledValue

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
     * The fraction of the progress going from [settledValue] to [targetValue], within [0f..1f]
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
    suspend fun settle(velocity: Float): Float {
        val target =
            if (abs(velocity) < abs(velocityThreshold())) {
                targetValue
            } else {
                anchors.closestAnchor(offset, velocity > 0) ?: targetValue
            }

        return anchoredDraggableState.animateToWithDecay(
            targetValue = target,
            velocity = velocity,
            snapAnimationSpec = animationSpec,
            decayAnimationSpec = decayAnimationSpec,
        )
    }

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

    private fun calculateTargetValue(
        currentOffset: Float,
        currentValue: T,
        anchors: DraggableAnchors<T>,
    ): T {
        if (currentOffset.isNaN()) return currentValue

        val currentValueOffset = anchors.positionOf(currentValue)
        // M3 logic: Priority fix. If we are already at the current value's offset, stay there.
        // This handles clashes where multiple anchors share the same offset and ensures target
        // stability. Analogous to foundation's calculateTargetValueWithFix.
        if (currentValueOffset.isNaN() || currentOffset == currentValueOffset) {
            return currentValue
        }

        val forward = currentOffset > currentValueOffset
        val candidates = mutableListOf<Pair<T, Float>>()
        anchors.forEach { value, position ->
            if (forward && position > currentValueOffset) {
                candidates.add(value to position)
            } else if (!forward && position < currentValueOffset) {
                candidates.add(value to position)
            }
        }
        if (forward) {
            candidates.sortBy { it.second }
        } else {
            candidates.sortByDescending { it.second }
        }

        var bestAllowedValue = currentValue
        var lastReachedOffset = currentValueOffset
        for (i in candidates.indices) {
            val (value, position) = candidates[i]
            val distance = abs(position - lastReachedOffset)
            // M3 logic: Use the positionalThreshold during active drags to determine targeting.
            // Foundation's targetValue implementation currently uses a fixed 50% bias
            // (closestAnchor)
            // until a fling occurs. By using the threshold here, M3 components can react (e.g.,
            // color changes) as soon as the user crosses the designated threshold.
            val threshold = positionalThreshold(distance)
            if (abs(currentOffset - lastReachedOffset) >= threshold) {
                // M3 logic: Respect confirmValueChange during target calculation.
                // Foundation's targetValue calculation doesn't factor in vetoes, which can lead to
                // the UI showing a target state that the component will never actually settle at.
                if (confirmValueChange(value)) {
                    bestAllowedValue = value
                }
                lastReachedOffset = position
            } else {
                break
            }
        }

        return bestAllowedValue
    }
}

/** This Modifier allows configuring a [MaterialAnchoredDraggableState] to a layout node. */
@Composable
internal fun <T> Modifier.anchoredDraggable(
    state: MaterialAnchoredDraggableState<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val finalFlingBehavior = flingBehavior ?: state.flingBehavior()
    return this.anchoredDraggable(
        state = state.anchoredDraggableState,
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        flingBehavior = finalFlingBehavior,
        interactionSource = interactionSource,
    )
}

/** Create and remember a [FlingBehavior] for use with [Modifier.anchoredDraggable]. */
@Composable
internal fun <T> MaterialAnchoredDraggableState<T>.flingBehavior(): FlingBehavior =
    AnchoredDraggableDefaults.flingBehavior(
        state = anchoredDraggableState,
        positionalThreshold = positionalThreshold,
        animationSpec = animationSpec,
    )
