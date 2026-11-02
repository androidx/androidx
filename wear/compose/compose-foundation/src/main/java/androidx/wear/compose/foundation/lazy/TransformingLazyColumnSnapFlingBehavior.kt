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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

internal class TransformingLazyColumnSnapFlingBehavior(
    val state: TransformingLazyColumnState,
    /** The provider of snap points that the fling snaps to. */
    val snapPoints: TransformingLazyColumnSnapPointsProvider,
    /** Speed in px/s above which a fling moves to a snap point in its direction. */
    val minFlingVelocity: Float,
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
        val snapTarget =
            findSnapFlingTarget(animationState, initialVelocity) ?: return animationState.velocity

        val distance = snapTarget.finalTarget - animationState.value
        // If the distance to fling is zero, nothing to do (and must avoid divide-by-zero below).
        if (distance == 0.0f) {
            return animationState.velocity
        }

        // At a hard scroll bound the list has stopped and is reversing off content padding.
        val initialSpeed = if (snapTarget.endOfListReached) 0f else animationState.velocity
        animateFinalSnap(
            animationState = animationState,
            targetKey = snapTarget.targetKey,
            finalTarget = snapTarget.finalTarget,
            distance = distance,
            initialSpeed = initialSpeed,
        )
        settleRemainingOffset(snapTarget.targetKey)

        // We consider all velocity consumed
        return 0.0f
    }

    /**
     * Decays [animationState] when [initialVelocity] starts a fling and chooses the snap target to
     * finish on. Returns null when the decay already stopped on an edge snap point, leaving any
     * unconsumed velocity in [animationState] for overscroll and nested scrolling.
     */
    private suspend fun ScrollScope.findSnapFlingTarget(
        animationState: AnimationState<Float, AnimationVector1D>,
        initialVelocity: Float,
    ): SnapFlingTarget? {
        // A single visible item can still be flung past: it may have several snap points, or other
        // items beyond it in the direction of the fling, as an item kept whole that is taller
        // than the viewport.
        val points = snapPoints.current()
        val isAFling =
            abs(initialVelocity) > 1f &&
                (points.points.size > 1 || points.hasItemsBeyond(initialVelocity.sign.toInt()))
        if (!isAFling) {
            // Not a fling, just snap to the nearest snap point.
            val current = snapPoints.current()
            val finalTarget = current.nearest?.let { current.distanceTo(it) } ?: 0f
            return SnapFlingTarget(current.nearest?.key, finalTarget, endOfListReached = false)
        }

        // Target we will land on given initialVelocity & decay
        val decayTarget = decay.calculateTargetValue(0f, initialVelocity)
        val endOfListReached = decayTowardsTarget(animationState)
        if (endOfListReached) {
            val current = snapPoints.current()
            val nearest = current.nearest
            val distanceToNearest = nearest?.let { current.distanceTo(it) } ?: 0f
            if (abs(distanceToNearest) <= MAX_FINAL_CORRECTION) {
                // Already on the edge snap point: finish and keep the remaining velocity
                // for overscroll and nested scrolling.
                scrollBy(distanceToNearest)
                return null
            }
            // Stopped on content padding past the edge snap point: animate back onto it.
            return SnapFlingTarget(
                targetKey = nearest?.key,
                finalTarget = animationState.value + distanceToNearest,
                endOfListReached = true,
            )
        }

        // The snap point closest to where the fling would stop, ahead in its direction if it is
        // fast enough.
        val current = snapPoints.current()
        val target =
            current.flingTarget(
                decayDistance = decayTarget - animationState.value,
                direction =
                    if (abs(initialVelocity) >= minFlingVelocity) {
                        initialVelocity.sign.toInt()
                    } else {
                        0
                    },
            )
        return SnapFlingTarget(
            targetKey = target?.key,
            finalTarget =
                target?.let { animationState.value + current.distanceTo(it) } ?: decayTarget,
            endOfListReached = false,
        )
    }

    /** Runs the decay phase of a fling and returns whether it hit the end of the scroll range. */
    private suspend fun ScrollScope.decayTowardsTarget(
        animationState: AnimationState<Float, AnimationVector1D>
    ): Boolean {
        var lastValue = 0f
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
        return endOfListReached
    }

    /**
     * Animates from the end of decay to [finalTarget] along a cubic bezier matching [initialSpeed],
     * following [targetKey] as items transform.
     */
    private suspend fun ScrollScope.animateFinalSnap(
        animationState: AnimationState<Float, AnimationVector1D>,
        targetKey: TransformingLazyColumnSnapPointKey?,
        finalTarget: Float,
        distance: Float,
        initialSpeed: Float,
    ) {
        // Construct a cubic bezier with the given initial velocity, and ending at 0 speed, unless
        // that will mean that we need to accelerate and decelerate. We can also control the inertia
        // of these speeds, i.e. how much it will accelerate/decelerate at the beginning and end.
        val initialInertia = 0.5f

        // Compute how much time we want to spend on the final snap, depending on the speed
        val finalSnapDuration =
            lerp(
                FINAL_SNAP_DURATION_MIN,
                FINAL_SNAP_DURATION_MAX,
                (abs(initialSpeed) / SNAP_SPEED_THRESHOLD).coerceAtMost(1f),
            )

        // Initial control point. Has slope (velocity) adjustedSpeed and magnitude (inertia)
        // initialInertia
        val adjustedSpeed = initialSpeed * finalSnapDuration / distance
        val easingX0 = initialInertia / sqrt(1f + adjustedSpeed * adjustedSpeed)
        val easingY0 = easingX0 * adjustedSpeed

        // Final control point. Has slope 0, unless that will make us accelerate then decelerate, in
        // that case we set the slope to 1.
        val easingX1 = 0.8f
        val easingY1 = if (easingX0 > easingY0) 0.8f else 1f

        val start = animationState.value
        var lastValue = start
        // Where the target snap point was last laid out, if it stops being laid out.
        var targetPosition = finalTarget
        var previousProgress = 0f
        animationState.animateTo(
            finalTarget,
            tween(
                (finalSnapDuration * 1000).fastRoundToInt(),
                easing = CubicBezierEasing(easingX0, easingY0, easingX1, easingY1),
            ),
        ) {
            // Items grow or shrink as they move, which moves the target snap point: spread the
            // scroll left to where it is now over the rest of the animation, so that the list
            // lands on it without turning back when it moves. The same curve if it does not.
            val left = targetKey?.let { distanceTo(it) } ?: (targetPosition - lastValue)
            targetPosition = lastValue + left
            val progress = (value - start) / distance
            lastValue += scrollBy(left.spreadOver(previousProgress, progress))
            previousProgress = progress
        }
    }

    /**
     * Corrects any offset left by item transformation on the final frame of [animateFinalSnap]. At
     * the ends of the list, a collapsed target falls back to the edge snap point so that it still
     * finishes on the edge.
     */
    private suspend fun ScrollScope.settleRemainingOffset(
        targetKey: TransformingLazyColumnSnapPointKey?
    ) {
        val remaining = targetKey?.let { distanceTo(it) } ?: 0f
        if (abs(remaining) <= MAX_FINAL_CORRECTION) {
            scrollBy(remaining)
        } else if (targetKey != null) {
            var previousProgress = 0f
            AnimationState(0f).animateTo(
                1f,
                tween(FINAL_CORRECTION_DURATION_MS, easing = FastOutSlowInEasing),
            ) {
                // Re-aimed on every frame, as the target keeps moving while items transform.
                val left = distanceTo(targetKey)
                if (left == null) {
                    cancelAnimation()
                } else {
                    scrollBy(left.spreadOver(previousProgress, value))
                    previousProgress = value
                }
            }
        }
    }

    private class SnapFlingTarget(
        val targetKey: TransformingLazyColumnSnapPointKey?,
        val finalTarget: Float,
        val endOfListReached: Boolean,
    )

    /**
     * Scroll needed to bring the snap point for [key] onto the snap line, falling back to the first
     * or last snap point at the ends of the list (see [TransformingLazyColumnSnapPoints.pointFor]),
     * or null.
     */
    private fun distanceTo(key: TransformingLazyColumnSnapPointKey): Float? =
        snapPoints.current().distanceTo(key)

    /**
     * The part of this scroll, left to do at [fromProgress] of an animation, to do by [toProgress].
     */
    private fun Float.spreadOver(fromProgress: Float, toProgress: Float): Float =
        if (fromProgress < 1f) this * (toProgress - fromProgress) / (1f - fromProgress) else this

    internal companion object {
        /** Same as the minimum fling velocity of the Compose Foundation snapping. */
        val MinFlingVelocity = 400.dp

        // Largest correction after the final snap, in pixels, applied in a single frame.
        private const val MAX_FINAL_CORRECTION = 1f

        // Duration of a larger correction after the final snap.
        private const val FINAL_CORRECTION_DURATION_MS = 100
    }
}

/**
 * Legacy item-based snap [FlingBehavior] used when
 * [androidx.wear.compose.foundation.WearComposeFoundationFlags.isTransformingLazyColumnSnapSegmentsEnabled]
 * is `false`.
 */
internal class LegacyTransformingLazyColumnSnapFlingBehavior(
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
}

// Speed, in pixels per second, to switch between decay and final snap.
private const val SNAP_SPEED_THRESHOLD = 1200f

// Minimum duration for the final snap after the fling, in seconds, used when the initial
// speed is 0
private const val FINAL_SNAP_DURATION_MIN = .1f

// Maximum duration for the final snap after the fling, in seconds, used when the speed is
// SNAP_SPEED_THRESHOLD
private const val FINAL_SNAP_DURATION_MAX = .35f
