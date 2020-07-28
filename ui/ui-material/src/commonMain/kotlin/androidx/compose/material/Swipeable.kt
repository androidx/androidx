/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.animation.asDisposableClock
import androidx.compose.animation.core.AnimatedFloat
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.ExponentialDecay
import androidx.compose.animation.core.OnAnimationEnd
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TargetAnimation
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.fling
import androidx.compose.foundation.gestures.draggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.state
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.annotation.FloatRange
import androidx.compose.ui.util.lerp
import kotlin.math.sign

/**
 * State of the [swipeable] composable.
 *
 * @param initialValue The initial value of the state.
 * @param clock The animation clock that will be used to drive the animation.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 * @param animationSpec The default animation that will be used to animate to a new state.
 */
@Stable
@ExperimentalMaterialApi
open class SwipeableState<T>(
    initialValue: T,
    clock: AnimationClockObservable,
    internal val confirmStateChange: (T) -> Boolean = { true },
    internal val animationSpec: AnimationSpec<Float> = SpringSpec()
) {
    /**
     * The current value of this state.
     *
     * If no swipe or animation is in progress, this corresponds to the anchor at which the
     * composable is currently settled. If a swipe or animation is in progress, this corresponds
     * the last anchor at which the composable was settled before the swipe or animation started.
     */
    var value: T by mutableStateOf(initialValue)
        internal set

    /**
     * Whether the [swipeable] is currently animating or flinging.
     */
    var isAnimationRunning by mutableStateOf(false)
        internal set

    private val _offset: MutableState<Float> = mutableStateOf(0f)
    private val _anchors: MutableState<Map<Float, T>> = mutableStateOf(emptyMap())

    private val clockProxy: AnimationClockObservable = object : AnimationClockObservable {
        override fun subscribe(observer: AnimationClockObserver) {
            isAnimationRunning = true
            clock.subscribe(observer)
        }

        override fun unsubscribe(observer: AnimationClockObserver) {
            isAnimationRunning = false
            clock.unsubscribe(observer)
        }
    }

    /**
     * The current offset of the [swipeable], intended to be used with Modifier.offsetPx.
     */
    val offset: State<Float> get() = _offset

    internal val animatedFloat: AnimatedFloat = AnimatedFloatByState(_offset, clockProxy)

    internal var anchors: Map<Float, T>
        get() = _anchors.value
        set(anchors) {
            if (anchors != this._anchors.value) {
                this._anchors.value = anchors
                anchors.getOffset(value)?.let {
                    animatedFloat.snapTo(it)
                }
            }
        }
    internal var thresholds: (Float, Float) -> Float by mutableStateOf(value = { _, _ -> 0f })

    /**
     * If a swipe is in progress, this is the state the [swipeable] would animate to
     * if the swipe finished. If no swipe is in progress, this is the same as [value].
     */
    @ExperimentalMaterialApi
    val swipeTarget: T
        get() {
            val target = adjustTarget(
                anchors = anchors.keys,
                thresholds = thresholds,
                target = animatedFloat.value,
                lastAnchor = anchors.getOffset(value) ?: animatedFloat.value
            )
            return anchors[target] ?: value
        }

    /**
     * Information about the current swipe progress. See [SwipeProgress] for details.
     *
     * If no swipe is in progress, this returns SwipeProgress(value, value, 1f).
     */
    @ExperimentalMaterialApi
    val swipeProgress: SwipeProgress<T>
        get() {
            val bounds = findBounds(offset.value, anchors.keys)
            val from: T
            val to: T
            val progress: Float
            when (bounds.size) {
                0 -> {
                    from = value
                    to = value
                    progress = 1f
                }
                1 -> {
                    from = anchors.getValue(bounds[0])
                    to = anchors.getValue(bounds[0])
                    progress = 1f
                }
                else -> {
                    val (a, b) =
                        if (swipeDirection > 0f) {
                            bounds[0] to bounds[1]
                        } else {
                            bounds[1] to bounds[0]
                        }
                    from = anchors.getValue(a)
                    to = anchors.getValue(b)
                    progress = (offset.value - a) / (b - a)
                }
            }
            return SwipeProgress(from, to, progress)
        }

    /**
     * The direction of the swipe, relative to the last anchor (i.e. [value]).
     *
     * This will be either 1f if it is a left to right or top to bottom swipe, -1f if it is
     * a right to left or bottom to top swipe, or 0f if no swipe is currently in progress.
     */
    @ExperimentalMaterialApi
    val swipeDirection: Float
        get() = anchors.getOffset(value)?.let { sign(animatedFloat.value - it) } ?: 0f

    /**
     * Set the state to the target value immediately, without any animation.
     *
     * @param targetValue The new target value to set [value] to.
     */
    @ExperimentalMaterialApi
    fun snapTo(targetValue: T) {
        val targetOffset = anchors.getOffset(targetValue)
        requireNotNull(targetOffset) {
            "The target value must have an associated anchor."
        }
        value = targetValue
        animatedFloat.snapTo(targetOffset)
    }

    /**
     * Set the state to the target value by starting an animation.
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     * @param onEnd Optional callback that will be invoked when the animation ended for any reason.
     */
    @ExperimentalMaterialApi
    fun animateTo(
        targetValue: T,
        anim: AnimationSpec<Float> = animationSpec,
        onEnd: ((AnimationEndReason, T) -> Unit)? = null
    ) {
        val targetOffset = anchors.getOffset(targetValue)
        requireNotNull(targetOffset) {
            "The target value must have an associated anchor."
        }
        animatedFloat.animateTo(targetOffset, anim) { endReason, endOffset ->
            anchors[endOffset]?.let {
                value = it
                onEnd?.invoke(endReason, it)
            }
        }
    }
}

/**
 * Collects information about the current swipe progress of a [swipeable].
 *
 * To access this information, use [SwipeableState.swipeProgress].
 *
 * @param from The state corresponding to the anchor we are swiping from.
 * @param to The state corresponding to the anchor we are swiping to.
 * @param progress The fraction that the current offsets represents between [from] and [to].
 */
@Immutable
data class SwipeProgress<T>(
    val from: T,
    val to: T,
    @FloatRange(from = 0.0, to = 1.0) val progress: Float
)

/**
 * Creates a [SwipeableState] which is kept in sync with another state. This means that:
 *  1. Whenever [state] changes, the [SwipeableState] will be animated to the new value.
 *  2. Whenever the value of [SwipeableState] changes (e.g. by dragging), the owner of [state]
 *  will be notified to update their state to the new value of [SwipeableState] by invoking
 *  [onStateChange]. If the owner does not update their state to the provided value for some
 *  reason, then the [SwipeableState] will perform a rollback to the previous, correct state.
 */
@Composable
@ExperimentalMaterialApi
internal fun <T> swipeableStateFor(
    state: T,
    onStateChange: (T) -> Unit,
    animationSpec: AnimationSpec<Float> = SpringSpec()
): SwipeableState<T> {
    val forceAnimationCheck = state { false }
    val clock = AnimationClockAmbient.current.asDisposableClock()
    val swipeableState = remember(clock) {
        SwipeableState(state, clock, animationSpec = animationSpec)
    }
    onCommit(state, forceAnimationCheck.value) {
        if (state != swipeableState.value) {
            swipeableState.animateTo(state)
        }
    }
    onCommit(swipeableState.value) {
        if (state != swipeableState.value) {
            onStateChange(swipeableState.value)
            forceAnimationCheck.value = !forceAnimationCheck.value
        }
    }
    return swipeableState
}

/**
 * Enable swipe gestures between a set of predefined states.
 *
 * To use this, you must provide a map of anchors (in pixels) to states (of type [T]).
 * Note that this map cannot be empty and cannot have two anchors mapped to the same state.
 *
 * When a swipe is detected, the offset of the [SwipeableState] will be updated with the swipe
 * delta. You should use this offset to move your content accordingly (see Modifier.offsetPx).
 * When the swipe ends, the offset will be animated to one of the anchors and when that anchor is
 * reached, the value of the [SwipeableState] will also be updated to the state corresponding to
 * the new anchor. The target anchor is calculated based on the provided positional [thresholds].
 *
 * For an example of a swipeable with three states, see:
 *
 * @sample androidx.compose.material.samples.SwipeableSample
 *
 * @param T The type of the state.
 * @param state The state of the [swipeable].
 * @param anchors Pairs of anchors and states, used to map anchors to states and vice versa.
 * @param thresholds The thresholds between anchors that determine which anchor to animate to
 * when the user stops swiping, represented as a lambda that takes a pair of anchors and returns
 * a value between them. Note the order of the anchors matters as it indicates the swipe direction.
 * An easy way to define these thresholds is using [fixedThresholds] or [fractionalThresholds].
 * @param orientation The orientation in which the [swipeable] can be swiped.
 * @param enabled Whether this [swipeable] is enabled and should react to the user's input.
 * @param reverseDirection Whether to reverse the direction of the swipe, so a top to bottom
 * swipe will behave like bottom to top, and a left to right swipe will behave like right to left.
 * @param minValue The lower bound in pixels of the range that swipe gestures will be constrained
 * to.
 * @param maxValue The upper bound in pixels that the swipe gestures will be constrained to.
 * @param interactionState Optional [InteractionState] that will passed on to [Modifier.draggable].
 */
@ExperimentalMaterialApi
fun <T> Modifier.swipeable(
    state: SwipeableState<T>,
    anchors: Map<Float, T>,
    thresholds: SwipeableThresholds,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    minValue: Float = anchors.keys.minOrNull() ?: Float.NEGATIVE_INFINITY,
    maxValue: Float = anchors.keys.maxOrNull() ?: Float.POSITIVE_INFINITY,
    interactionState: InteractionState? = null
) = composed {
    require(anchors.isNotEmpty()) {
        "You must have at least one anchor."
    }
    require(anchors.values.distinct().count() == anchors.size) {
        "You cannot have two anchors mapped to the same state."
    }
    val density = DensityAmbient.current
    state.anchors = anchors
    state.thresholds = { a, b -> density.thresholds(a, b) }
    state.animatedFloat.setBounds(minValue, maxValue)

    val lastAnchor = anchors.getOffset(state.value)!!
    val onAnimationEnd: OnAnimationEnd = { endReason, endValue, _ ->
        if (endReason != AnimationEndReason.Interrupted) {
            val newState = anchors[endValue]
            if (newState != null && state.confirmStateChange(newState)) {
                state.value = newState
            } else {
                state.animatedFloat.animateTo(lastAnchor, state.animationSpec)
            }
        }
    }
    val flingConfig = FlingConfig(
        decayAnimation = ExponentialDecay(),
        adjustTarget = { target ->
            val adjusted = adjustTarget(
                anchors = anchors.keys,
                thresholds = { a, b -> density.thresholds(a, b) },
                target = target,
                lastAnchor = lastAnchor
            )
            TargetAnimation(adjusted, state.animationSpec)
        }
    )

    Modifier.draggable(
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        interactionState = interactionState,
        startDragImmediately = state.isAnimationRunning,
        onDragStopped = {
            state.animatedFloat.fling(it, flingConfig, onAnimationEnd)
        }
    ) { delta ->
        state.animatedFloat.snapTo(state.animatedFloat.value + delta)
    }
}

/**
 * Type alias for the lambda that will be invoked to compute the thresholds between anchors in
 * [Modifier.swipeable]. This takes two anchors, whose the order indicates the swipe direction.
 */
typealias SwipeableThresholds = Density.(fromAnchor: Float, toAnchor: Float) -> Float

/**
 * Constructor for fixed thresholds between anchors.
 *
 * @param offset Each threshold will be at this offset (in dp) away from the first anchor.
 */
@ExperimentalMaterialApi
fun fixedThresholds(offset: Dp): SwipeableThresholds =
    { fromAnchor, toAnchor -> fromAnchor + offset.toPx() * sign(toAnchor - fromAnchor) }

/**
 * Constructor for fractional thresholds between anchors.
 *
 * @param fraction Each threshold will be at this fraction of the way between the two anchors.
 */
@ExperimentalMaterialApi
fun fractionalThresholds(
    @FloatRange(from = 0.0, to = 1.0) fraction: Float
): SwipeableThresholds = { fromAnchor, toAnchor -> lerp(fromAnchor, toAnchor, fraction) }

@Stable
private class AnimatedFloatByState(
    mutableState: MutableState<Float>,
    clock: AnimationClockObservable
) : AnimatedFloat(clock) {
    override var value: Float by mutableState
}

/**
 *  Given a target x and a set of anchors, return a list of anchors:
 *   1. [ ] if the set of anchors is empty,
 *   2. [ x ] if x is equal to one of the anchors,
 *   3. [ min ] if min is the minimum anchor and x < min,
 *   4. [ max ] if max is the maximum anchor and x > max, or
 *   5. [ a , b ] if a and b are anchors such that a < x < b and b - a is minimal.
 */
private fun findBounds(
    target: Float,
    anchors: Set<Float>
): List<Float> {
    // Find the anchors the target lies between.
    val a = anchors.filter { it <= target }.maxOrNull()
    val b = anchors.filter { it >= target }.minOrNull()

    return when {
        a == null ->
            // case 1 or 3
            listOfNotNull(b)
        b == null ->
            // case 4
            listOf(a)
        a == b ->
            // case 2
            listOf(target)
        else ->
            // case 5
            listOf(a, b)
    }
}

private fun adjustTarget(
    target: Float,
    lastAnchor: Float,
    anchors: Set<Float>,
    thresholds: (Float, Float) -> Float
): Float {
    val bounds = findBounds(target, anchors)
    return when (bounds.size) {
        0 -> lastAnchor
        1 -> bounds[0]
        else -> {
            val lower = bounds[0]
            val upper = bounds[1]
            val threshold =
                if (lastAnchor <= target) {
                    thresholds(lower, upper)
                } else {
                    thresholds(upper, lower)
                }
            if (target < threshold) lower else upper
        }
    }
}

private fun <T> Map<Float, T>.getOffset(state: T): Float? {
    return entries.firstOrNull { it.value == state }?.key
}