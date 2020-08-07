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
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.material.SwipeableConstants.DefaultVelocityThreshold
import androidx.compose.material.SwipeableConstants.StandardResistanceFactor
import androidx.compose.material.SwipeableConstants.StiffResistanceFactor
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.AnimationClockAmbient
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.annotation.FloatRange
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.sign
import kotlin.math.sin

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
    private val _overflow: MutableState<Float> = mutableStateOf(0f)
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

    /**
     * The current amount by which the [swipeable] has been swiped past the bounds.
     */
    val overflow: State<Float> get() = _overflow

    internal val animatedFloat: AnimatedFloat = NotificationBasedAnimatedFloat(0f, clockProxy) {
        val clamped = it.coerceIn(minBound, maxBound)
        val overflow = it - clamped
        val resistance = computeResistance(
            basis = resistanceBasis,
            factor = if (overflow < 0f) resistanceAtMinBound else resistanceAtMaxBound,
            overflow = overflow
        )
        _offset.value = clamped + resistance
        _overflow.value = overflow
    }

    internal var anchors: Map<Float, T>
        get() = _anchors.value
        set(anchors) {
            if (anchors != this._anchors.value) {
                this._anchors.value = anchors
                anchors.getOffset(value)?.let {
                    animatedFloat.snapTo(it)
                }
                minBound = anchors.keys.minOrNull()!!
                maxBound = anchors.keys.maxOrNull()!!
            }
        }
    internal var thresholds: (Float, Float) -> Float by mutableStateOf(value = { _, _ -> 0f })

    private var minBound: Float = Float.NEGATIVE_INFINITY
    private var maxBound: Float = Float.POSITIVE_INFINITY

    internal var resistanceBasis: Float = 0f
    internal var resistanceAtMinBound: Float = 0f
    internal var resistanceAtMaxBound: Float = 0f

    /**
     * If a swipe is in progress, this is the state the [swipeable] would animate to
     * if the swipe finished. If no swipe is in progress, this is the same as [value].
     */
    @ExperimentalMaterialApi
    val swipeTarget: T
        get() {
            // TODO(calintat): Track current velocity (b/149549482) and use that here.
            val target = adjustTarget(
                anchors = anchors.keys,
                thresholds = thresholds,
                target = offset.value,
                lastAnchor = anchors.getOffset(value) ?: offset.value,
                velocity = 0f,
                velocityThreshold = Float.POSITIVE_INFINITY
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
        get() = anchors.getOffset(value)?.let { sign(offset.value - it) } ?: 0f

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
            // TODO(calintat): What to do if anchors[endOffset] is null?
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
    val forceAnimationCheck = remember { mutableStateOf(false) }
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
 * Swiping is constrained between the min and max anchors. If the user attempts to swipe past
 * those bounds, a resistance effect will be applied. The amount of resistance at each edge can
 * be customised using the two resistance factor parameters, and you can disable resistance by
 * setting them to zero. Two default resistance factors are provided in [SwipeableConstants]:
 * - [StiffResistanceFactor] which conveys to the user that swiping is not available right now, and
 * - [StandardResistanceFactor] which conveys to the user that they have run out of things to see.
 *
 * For an example of a swipeable with three states, see:
 *
 * @sample androidx.compose.material.samples.SwipeableSample
 *
 * @param T The type of the state.
 * @param state The state of the [swipeable].
 * @param anchors Pairs of anchors and states, used to map anchors to states and vice versa.
 * @param thresholds Specifies where the thresholds between the states are. The thresholds will be
 * used to determine which state to animate to when swiping stops. This is represented as a lambda
 * that takes two states and returns the threshold between them in the form of a [ThresholdConfig].
 * Note that the order of the states corresponds to the swipe direction.
 * @param orientation The orientation in which the [swipeable] can be swiped.
 * @param enabled Whether this [swipeable] is enabled and should react to the user's input.
 * @param reverseDirection Whether to reverse the direction of the swipe, so a top to bottom
 * swipe will behave like bottom to top, and a left to right swipe will behave like right to left.
 * @param interactionState Optional [InteractionState] that will passed on to [Modifier.draggable].
 * @param velocityThreshold The threshold (in dp per second) that the end velocity has to exceed
 * in order to animate to the next state, even if the positional [thresholds] have not been reached.
 * @param resistanceFactorAtMin The resistance factor applied when swiping past the min bound.
 * @param resistanceFactorAtMax The resistance factor applied when swiping past the max bound.
 */
@ExperimentalMaterialApi
fun <T> Modifier.swipeable(
    state: SwipeableState<T>,
    anchors: Map<Float, T>,
    thresholds: (from: T, to: T) -> ThresholdConfig,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionState: InteractionState? = null,
    velocityThreshold: Dp = DefaultVelocityThreshold,
    resistanceFactorAtMin: Float = StandardResistanceFactor,
    resistanceFactorAtMax: Float = StandardResistanceFactor
) = composed {
    require(anchors.isNotEmpty()) {
        "You must have at least one anchor."
    }
    require(anchors.values.distinct().count() == anchors.size) {
        "You cannot have two anchors mapped to the same state."
    }
    val density = DensityAmbient.current
    state.anchors = anchors
    state.thresholds = { a, b ->
        val from = anchors.getValue(a)
        val to = anchors.getValue(b)
        with(thresholds(from, to)) { density.computeThreshold(a, b) }
    }
    state.resistanceAtMinBound = resistanceFactorAtMin
    state.resistanceAtMaxBound = resistanceFactorAtMax

    val draggable = Modifier.draggable(
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        interactionState = interactionState,
        startDragImmediately = state.isAnimationRunning,
        onDragStopped = { velocity ->
            val lastAnchor = anchors.getOffset(state.value)!!
            val targetValue = adjustTarget(
                anchors = anchors.keys,
                thresholds = state.thresholds,
                target = state.offset.value,
                lastAnchor = lastAnchor,
                velocity = velocity,
                velocityThreshold = with(density) { velocityThreshold.toPx() }
            )
            val targetState = anchors[targetValue]
            if (targetState != null && state.confirmStateChange(targetState)) {
                state.animateTo(targetState)
            } else {
                // If the user vetoed the state change, rollback to the previous state.
                state.animatedFloat.animateTo(lastAnchor, state.animationSpec)
            }
        }
    ) { delta ->
        state.animatedFloat.snapTo(state.animatedFloat.value + delta)
    }
    draggable.onPositioned {
        state.resistanceBasis = when (orientation) {
            Orientation.Vertical -> it.size.height.toFloat()
            Orientation.Horizontal -> it.size.width.toFloat()
        }
    }
}

/**
 * Interface to compute a threshold between two anchors/states in a [swipeable].
 *
 * To define a [ThresholdConfig], consider using [FixedThreshold] and [FractionalThreshold].
 */
@Stable
@ExperimentalMaterialApi
interface ThresholdConfig {
    /**
     * Compute the value of the threshold (in pixels), once the values of the anchors are known.
     */
    fun Density.computeThreshold(fromValue: Float, toValue: Float): Float
}

/**
 * A fixed threshold will be at an [offset] away from the first anchor.
 *
 * @param offset The offset (in dp) that the threshold will be at.
 */
@Immutable
@ExperimentalMaterialApi
data class FixedThreshold(private val offset: Dp) : ThresholdConfig {
    override fun Density.computeThreshold(fromValue: Float, toValue: Float): Float {
        return fromValue + offset.toPx() * sign(toValue - fromValue)
    }
}

/**
 * A fractional threshold will be at a [fraction] of the way between the two anchors.
 *
 * @param fraction The fraction (between 0 and 1) that the threshold will be at.
 */
@Immutable
@ExperimentalMaterialApi
data class FractionalThreshold(
    @FloatRange(from = 0.0, to = 1.0) private val fraction: Float
) : ThresholdConfig {
    override fun Density.computeThreshold(fromValue: Float, toValue: Float): Float {
        return lerp(fromValue, toValue, fraction)
    }
}

private class NotificationBasedAnimatedFloat(
    initialValue: Float,
    clock: AnimationClockObservable,
    val onNewValue: (Float) -> Unit
) : AnimatedFloat(clock) {
    override var value = initialValue
        set(value) {
            field = value
            onNewValue(value)
        }
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
    thresholds: (Float, Float) -> Float,
    velocity: Float,
    velocityThreshold: Float
): Float {
    val bounds = findBounds(target, anchors)
    return when (bounds.size) {
        0 -> lastAnchor
        1 -> bounds[0]
        else -> {
            val lower = bounds[0]
            val upper = bounds[1]
            if (lastAnchor <= target) {
                // Swiping from lower to upper (positive).
                if (velocity >= velocityThreshold) {
                    return upper
                } else {
                    val threshold = thresholds(lower, upper)
                    if (target < threshold) lower else upper
                }
            } else {
                // Swiping from upper to lower (negative).
                if (velocity <= -velocityThreshold) {
                    return lower
                } else {
                    val threshold = thresholds(upper, lower)
                    if (target > threshold) upper else lower
                }
            }
        }
    }
}

internal fun computeResistance(
    basis: Float,
    factor: Float,
    overflow: Float
): Float {
    val progress = (overflow / basis).coerceIn(-1f, 1f)
    val resistance = basis / factor * sin(progress * PI.toFloat() / 2)
    return if (resistance.isFinite()) resistance else 0f
}

private fun <T> Map<Float, T>.getOffset(state: T): Float? {
    return entries.firstOrNull { it.value == state }?.key
}

/**
 * Contains the default values used by [swipeable].
 */
object SwipeableConstants {
    /**
     * The default velocity threshold used by [swipeable].
     */
    val DefaultVelocityThreshold = 125.dp // 1/8 dp per millisecond

    /**
     * A stiff resistance factor which indicates that swiping isn't available right now.
     */
    const val StiffResistanceFactor = 20f

    /**
     * A standard resistance factor which indicates that the user has run out of things to see.
     */
    const val StandardResistanceFactor = 10f
}