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

@file:Suppress("Deprecation")

package androidx.compose.material

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.SwipeableDefaults.AnimationSpec
import androidx.compose.material.SwipeableDefaults.StandardResistanceFactor
import androidx.compose.material.SwipeableDefaults.VelocityThreshold
import androidx.compose.material.SwipeableDefaults.resistanceConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastMinByOrNull
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * State of the [swipeable] modifier.
 *
 * This contains necessary information about any ongoing swipe or animation and provides methods
 * to change the state either immediately or by starting an animation. To create and remember a
 * [SwipeableState] with the default animation clock, use [rememberSwipeableState].
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
open class SwipeableState<T>(
    initialValue: T,
    internal val animationSpec: AnimationSpec<Float> = AnimationSpec,
    internal val confirmStateChange: (newValue: T) -> Boolean = { true }
) {
    /**
     * The current value of the state.
     *
     * If no swipe or animation is in progress, this corresponds to the anchor at which the
     * [swipeable] is currently settled. If a swipe or animation is in progress, this corresponds
     * the last anchor at which the [swipeable] was settled before the swipe or animation started.
     */
    var currentValue: T by mutableStateOf(initialValue)
        private set

    /**
     * Whether the state is currently animating.
     */
    var isAnimationRunning: Boolean by mutableStateOf(false)
        private set

    /**
     * The current position (in pixels) of the [swipeable].
     *
     * You should use this state to offset your content accordingly. The recommended way is to
     * use `Modifier.offsetPx`. This includes the resistance by default, if resistance is enabled.
     */
    val offset: State<Float> get() = offsetState

    /**
     * The amount by which the [swipeable] has been swiped past its bounds.
     */
    val overflow: State<Float> get() = overflowState

    // Use `Float.NaN` as a placeholder while the state is uninitialised.
    private val offsetState = mutableFloatStateOf(0f)
    private val overflowState = mutableFloatStateOf(0f)

    // the source of truth for the "real"(non ui) position
    // basically position in bounds + overflow
    private val absoluteOffset = mutableFloatStateOf(0f)

    // current animation target, if animating, otherwise null
    private val animationTarget = mutableStateOf<Float?>(null)

    internal var anchors by mutableStateOf(emptyMap<Float, T>())

    private val latestNonEmptyAnchorsFlow: Flow<Map<Float, T>> =
        snapshotFlow { anchors }
            .filter { it.isNotEmpty() }
            .take(1)

    internal var minBound = Float.NEGATIVE_INFINITY
    internal var maxBound = Float.POSITIVE_INFINITY

    internal fun ensureInit(newAnchors: Map<Float, T>) {
        if (anchors.isEmpty()) {
            // need to do initial synchronization synchronously :(
            val initialOffset = newAnchors.getOffset(currentValue)
            requireNotNull(initialOffset) {
                "The initial value must have an associated anchor."
            }
            offsetState.floatValue = initialOffset
            absoluteOffset.floatValue = initialOffset
        }
    }

    internal suspend fun processNewAnchors(
        oldAnchors: Map<Float, T>,
        newAnchors: Map<Float, T>
    ) {
        if (oldAnchors.isEmpty()) {
            // If this is the first time that we receive anchors, then we need to initialise
            // the state so we snap to the offset associated to the initial value.
            minBound = newAnchors.keys.minOrNull()!!
            maxBound = newAnchors.keys.maxOrNull()!!
            val initialOffset = newAnchors.getOffset(currentValue)
            requireNotNull(initialOffset) {
                "The initial value must have an associated anchor."
            }
            snapInternalToOffset(initialOffset)
        } else if (newAnchors != oldAnchors) {
            // If we have received new anchors, then the offset of the current value might
            // have changed, so we need to animate to the new offset. If the current value
            // has been removed from the anchors then we animate to the closest anchor
            // instead. Note that this stops any ongoing animation.
            minBound = Float.NEGATIVE_INFINITY
            maxBound = Float.POSITIVE_INFINITY
            val animationTargetValue = animationTarget.value
            // if we're in the animation already, let's find it a new home
            val targetOffset = if (animationTargetValue != null) {
                // first, try to map old state to the new state
                val oldState = oldAnchors[animationTargetValue]
                val newState = newAnchors.getOffset(oldState)
                // return new state if exists, or find the closes one among new anchors
                newState ?: newAnchors.keys.minByOrNull { abs(it - animationTargetValue) }!!
            } else {
                // we're not animating, proceed by finding the new anchors for an old value
                val actualOldValue = oldAnchors[offset.value]
                val value = if (actualOldValue == currentValue) currentValue else actualOldValue
                newAnchors.getOffset(value) ?: newAnchors
                    .keys.minByOrNull { abs(it - offset.value) }!!
            }
            try {
                animateInternalToOffset(targetOffset, animationSpec)
            } catch (c: CancellationException) {
                // If the animation was interrupted for any reason, snap as a last resort.
                snapInternalToOffset(targetOffset)
            } finally {
                currentValue = newAnchors.getValue(targetOffset)
                minBound = newAnchors.keys.minOrNull()!!
                maxBound = newAnchors.keys.maxOrNull()!!
            }
        }
    }

    internal var thresholds: (Float, Float) -> Float by mutableStateOf({ _, _ -> 0f })

    internal var velocityThreshold by mutableFloatStateOf(0f)

    internal var resistance: ResistanceConfig? by mutableStateOf(null)

    internal val draggableState = DraggableState {
        val newAbsolute = absoluteOffset.floatValue + it
        val clamped = newAbsolute.coerceIn(minBound, maxBound)
        val overflow = newAbsolute - clamped
        val resistanceDelta = resistance?.computeResistance(overflow) ?: 0f
        offsetState.floatValue = clamped + resistanceDelta
        overflowState.floatValue = overflow
        absoluteOffset.floatValue = newAbsolute
    }

    private suspend fun snapInternalToOffset(target: Float) {
        draggableState.drag {
            dragBy(target - absoluteOffset.floatValue)
        }
    }

    private suspend fun animateInternalToOffset(target: Float, spec: AnimationSpec<Float>) {
        draggableState.drag {
            var prevValue = absoluteOffset.floatValue
            animationTarget.value = target
            isAnimationRunning = true
            try {
                Animatable(prevValue).animateTo(target, spec) {
                    dragBy(this.value - prevValue)
                    prevValue = this.value
                }
            } finally {
                animationTarget.value = null
                isAnimationRunning = false
            }
        }
    }

    /**
     * The target value of the state.
     *
     * If a swipe is in progress, this is the value that the [swipeable] would animate to if the
     * swipe finished. If an animation is running, this is the target value of that animation.
     * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
     */
    @ExperimentalMaterialApi
    val targetValue: T
        get() {
            // TODO(calintat): Track current velocity (b/149549482) and use that here.
            val target = animationTarget.value ?: computeTarget(
                offset = offset.value,
                lastValue = anchors.getOffset(currentValue) ?: offset.value,
                anchors = anchors.keys,
                thresholds = thresholds,
                velocity = 0f,
                velocityThreshold = Float.POSITIVE_INFINITY
            )
            return anchors[target] ?: currentValue
        }

    /**
     * Information about the ongoing swipe or animation, if any. See [SwipeProgress] for details.
     *
     * If no swipe or animation is in progress, this returns `SwipeProgress(value, value, 1f)`.
     */
    @ExperimentalMaterialApi
    val progress: SwipeProgress<T>
        get() {
            val bounds = findBounds(offset.value, anchors.keys)
            val from: T
            val to: T
            val fraction: Float
            when (bounds.size) {
                0 -> {
                    from = currentValue
                    to = currentValue
                    fraction = 1f
                }
                1 -> {
                    from = anchors.getValue(bounds[0])
                    to = anchors.getValue(bounds[0])
                    fraction = 1f
                }
                else -> {
                    val (a, b) =
                        if (direction > 0f) {
                            bounds[0] to bounds[1]
                        } else {
                            bounds[1] to bounds[0]
                        }
                    from = anchors.getValue(a)
                    to = anchors.getValue(b)
                    fraction = (offset.value - a) / (b - a)
                }
            }
            return SwipeProgress(from, to, fraction)
        }

    /**
     * The direction in which the [swipeable] is moving, relative to the current [currentValue].
     *
     * This will be either 1f if it is is moving from left to right or top to bottom, -1f if it is
     * moving from right to left or bottom to top, or 0f if no swipe or animation is in progress.
     */
    @ExperimentalMaterialApi
    val direction: Float
        get() = anchors.getOffset(currentValue)?.let { sign(offset.value - it) } ?: 0f

    /**
     * Set the state without any animation and suspend until it's set
     *
     * @param targetValue The new target value to set [currentValue] to.
     */
    @ExperimentalMaterialApi
    suspend fun snapTo(targetValue: T) {
        latestNonEmptyAnchorsFlow.collect { anchors ->
            val targetOffset = anchors.getOffset(targetValue)
            requireNotNull(targetOffset) {
                "The target value must have an associated anchor."
            }
            snapInternalToOffset(targetOffset)
            currentValue = targetValue
        }
    }

    /**
     * Set the state to the target value by starting an animation.
     *
     * @param targetValue The new value to animate to.
     * @param anim The animation that will be used to animate to the new value.
     */
    @ExperimentalMaterialApi
    suspend fun animateTo(targetValue: T, anim: AnimationSpec<Float> = animationSpec) {
        latestNonEmptyAnchorsFlow.collect { anchors ->
            try {
                val targetOffset = anchors.getOffset(targetValue)
                requireNotNull(targetOffset) {
                    "The target value must have an associated anchor."
                }
                animateInternalToOffset(targetOffset, anim)
            } finally {
                val endOffset = absoluteOffset.floatValue
                val endValue = anchors
                    // fighting rounding error once again, anchor should be as close as 0.5 pixels
                    .filterKeys { anchorOffset -> abs(anchorOffset - endOffset) < 0.5f }
                    .values.firstOrNull() ?: currentValue
                currentValue = endValue
            }
        }
    }

    /**
     * Perform fling with settling to one of the anchors which is determined by the given
     * [velocity]. Fling with settling [swipeable] will always consume all the velocity provided
     * since it will settle at the anchor.
     *
     * In general cases, [swipeable] flings by itself when being swiped. This method is to be
     * used for nested scroll logic that wraps the [swipeable]. In nested scroll developer may
     * want to trigger settling fling when the child scroll container reaches the bound.
     *
     * @param velocity velocity to fling and settle with
     *
     * @return the reason fling ended
     */
    suspend fun performFling(velocity: Float) {
        latestNonEmptyAnchorsFlow.collect { anchors ->
            val lastAnchor = anchors.getOffset(currentValue)!!
            val targetValue = computeTarget(
                offset = offset.value,
                lastValue = lastAnchor,
                anchors = anchors.keys,
                thresholds = thresholds,
                velocity = velocity,
                velocityThreshold = velocityThreshold
            )
            val targetState = anchors[targetValue]
            if (targetState != null && confirmStateChange(targetState)) animateTo(targetState)
            // If the user vetoed the state change, rollback to the previous state.
            else animateInternalToOffset(lastAnchor, animationSpec)
        }
    }

    /**
     * Force [swipeable] to consume drag delta provided from outside of the regular [swipeable]
     * gesture flow.
     *
     * Note: This method performs generic drag and it won't settle to any particular anchor, *
     * leaving swipeable in between anchors. When done dragging, [performFling] must be
     * called as well to ensure swipeable will settle at the anchor.
     *
     * In general cases, [swipeable] drags by itself when being swiped. This method is to be
     * used for nested scroll logic that wraps the [swipeable]. In nested scroll developer may
     * want to force drag when the child scroll container reaches the bound.
     *
     * @param delta delta in pixels to drag by
     *
     * @return the amount of [delta] consumed
     */
    fun performDrag(delta: Float): Float {
        val potentiallyConsumed = absoluteOffset.floatValue + delta
        val clamped = potentiallyConsumed.coerceIn(minBound, maxBound)
        val deltaToConsume = clamped - absoluteOffset.floatValue
        if (abs(deltaToConsume) > 0) {
            draggableState.dispatchRawDelta(deltaToConsume)
        }
        return deltaToConsume
    }

    companion object {
        /**
         * The default [Saver] implementation for [SwipeableState].
         */
        fun <T : Any> Saver(
            animationSpec: AnimationSpec<Float>,
            confirmStateChange: (T) -> Boolean
        ) = Saver<SwipeableState<T>, T>(
            save = { it.currentValue },
            restore = { SwipeableState(it, animationSpec, confirmStateChange) }
        )
    }
}

/**
 * Collects information about the ongoing swipe or animation in [swipeable].
 *
 * To access this information, use [SwipeableState.progress].
 *
 * @param from The state corresponding to the anchor we are moving away from.
 * @param to The state corresponding to the anchor we are moving towards.
 * @param fraction The fraction that the current position represents between [from] and [to].
 * Must be between `0` and `1`.
 */
@Immutable
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
class SwipeProgress<T>(
    val from: T,
    val to: T,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    val fraction: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SwipeProgress<*>) return false

        if (from != other.from) return false
        if (to != other.to) return false
        if (fraction != other.fraction) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from?.hashCode() ?: 0
        result = 31 * result + (to?.hashCode() ?: 0)
        result = 31 * result + fraction.hashCode()
        return result
    }

    override fun toString(): String {
        return "SwipeProgress(from=$from, to=$to, fraction=$fraction)"
    }
}

/**
 * Create and [remember] a [SwipeableState] with the default animation clock.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
fun <T : Any> rememberSwipeableState(
    initialValue: T,
    animationSpec: AnimationSpec<Float> = AnimationSpec,
    confirmStateChange: (newValue: T) -> Boolean = { true }
): SwipeableState<T> {
    return rememberSaveable(
        saver = SwipeableState.Saver(
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    ) {
        SwipeableState(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmStateChange = confirmStateChange
        )
    }
}

/**
 * Create and [remember] a [SwipeableState] which is kept in sync with another state, i.e.:
 *  1. Whenever the [value] changes, the [SwipeableState] will be animated to that new value.
 *  2. Whenever the value of the [SwipeableState] changes (e.g. after a swipe), the owner of the
 *  [value] will be notified to update their state to the new value of the [SwipeableState] by
 *  invoking [onValueChange]. If the owner does not update their state to the provided value for
 *  some reason, then the [SwipeableState] will perform a rollback to the previous, correct value.
 */
@Composable
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
internal fun <T : Any> rememberSwipeableStateFor(
    value: T,
    onValueChange: (T) -> Unit,
    animationSpec: AnimationSpec<Float> = AnimationSpec
): SwipeableState<T> {
    val swipeableState = remember {
        SwipeableState(
            initialValue = value,
            animationSpec = animationSpec,
            confirmStateChange = { true }
        )
    }
    val forceAnimationCheck = remember { mutableStateOf(false) }
    LaunchedEffect(value, forceAnimationCheck.value) {
        if (value != swipeableState.currentValue) {
            swipeableState.animateTo(value)
        }
    }
    DisposableEffect(swipeableState.currentValue) {
        if (value != swipeableState.currentValue) {
            onValueChange(swipeableState.currentValue)
            forceAnimationCheck.value = !forceAnimationCheck.value
        }
        onDispose { }
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
 * delta. You should use this offset to move your content accordingly (see `Modifier.offsetPx`).
 * When the swipe ends, the offset will be animated to one of the anchors and when that anchor is
 * reached, the value of the [SwipeableState] will also be updated to the state corresponding to
 * the new anchor. The target anchor is calculated based on the provided positional [thresholds].
 *
 * Swiping is constrained between the minimum and maximum anchors. If the user attempts to swipe
 * past these bounds, a resistance effect will be applied by default. The amount of resistance at
 * each edge is specified by the [resistance] config. To disable all resistance, set it to `null`.
 *
 * For an example of a [swipeable] with three states, see:
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
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to
 * the internal [Modifier.draggable].
 * @param resistance Controls how much resistance will be applied when swiping past the bounds.
 * @param velocityThreshold The threshold (in dp per second) that the end velocity has to exceed
 * in order to animate to the next state, even if the positional [thresholds] have not been reached.
 */
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
fun <T> Modifier.swipeable(
    state: SwipeableState<T>,
    anchors: Map<Float, T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
    thresholds: (from: T, to: T) -> ThresholdConfig = { _, _ -> FixedThreshold(56.dp) },
    resistance: ResistanceConfig? = resistanceConfig(anchors.keys),
    velocityThreshold: Dp = VelocityThreshold
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "swipeable"
        properties["state"] = state
        properties["anchors"] = anchors
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["thresholds"] = thresholds
        properties["resistance"] = resistance
        properties["velocityThreshold"] = velocityThreshold
    }
) {
    require(anchors.isNotEmpty()) {
        "You must have at least one anchor."
    }
    require(anchors.values.distinct().count() == anchors.size) {
        "You cannot have two anchors mapped to the same state."
    }
    val density = LocalDensity.current
    state.ensureInit(anchors)
    LaunchedEffect(anchors, state) {
        val oldAnchors = state.anchors
        state.anchors = anchors
        state.resistance = resistance
        state.thresholds = { a, b ->
            val from = anchors.getValue(a)
            val to = anchors.getValue(b)
            with(thresholds(from, to)) { density.computeThreshold(a, b) }
        }
        with(density) {
            state.velocityThreshold = velocityThreshold.toPx()
        }
        state.processNewAnchors(oldAnchors, anchors)
    }

    Modifier.draggable(
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        interactionSource = interactionSource,
        startDragImmediately = state.isAnimationRunning,
        onDragStopped = { velocity -> launch { state.performFling(velocity) } },
        state = state.draggableState
    )
}

/**
 * Interface to compute a threshold between two anchors/states in a [swipeable].
 *
 * To define a [ThresholdConfig], consider using [FixedThreshold] and [FractionalThreshold].
 */
@Stable
@ExperimentalMaterialApi
@Deprecated(SwipeableDeprecation)
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
@Deprecated(SwipeableDeprecation)
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
@Deprecated(SwipeableDeprecation)
data class FractionalThreshold(
    /*@FloatRange(from = 0.0, to = 1.0)*/
    private val fraction: Float
) : ThresholdConfig {
    override fun Density.computeThreshold(fromValue: Float, toValue: Float): Float {
        return lerp(fromValue, toValue, fraction)
    }
}

/**
 * Specifies how resistance is calculated in [swipeable].
 *
 * There are two things needed to calculate resistance: the resistance basis determines how much
 * overflow will be consumed to achieve maximum resistance, and the resistance factor determines
 * the amount of resistance (the larger the resistance factor, the stronger the resistance).
 *
 * The resistance basis is usually either the size of the component which [swipeable] is applied
 * to, or the distance between the minimum and maximum anchors. For a constructor in which the
 * resistance basis defaults to the latter, consider using [resistanceConfig].
 *
 * You may specify different resistance factors for each bound. Consider using one of the default
 * resistance factors in [SwipeableDefaults]: `StandardResistanceFactor` to convey that the user
 * has run out of things to see, and `StiffResistanceFactor` to convey that the user cannot swipe
 * this right now. Also, you can set either factor to 0 to disable resistance at that bound.
 *
 * @param basis Specifies the maximum amount of overflow that will be consumed. Must be positive.
 * @param factorAtMin The factor by which to scale the resistance at the minimum bound.
 * Must not be negative.
 * @param factorAtMax The factor by which to scale the resistance at the maximum bound.
 * Must not be negative.
 */
@Immutable
@Deprecated(SwipeableDeprecation)
class ResistanceConfig(
    /*@FloatRange(from = 0.0, fromInclusive = false)*/
    val basis: Float,
    /*@FloatRange(from = 0.0)*/
    val factorAtMin: Float = StandardResistanceFactor,
    /*@FloatRange(from = 0.0)*/
    val factorAtMax: Float = StandardResistanceFactor
) {
    fun computeResistance(overflow: Float): Float {
        val factor = if (overflow < 0) factorAtMin else factorAtMax
        if (factor == 0f) return 0f
        val progress = (overflow / basis).fastCoerceIn(-1f, 1f)
        return basis / factor * sin(progress * PI.toFloat() / 2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResistanceConfig) return false

        if (basis != other.basis) return false
        if (factorAtMin != other.factorAtMin) return false
        if (factorAtMax != other.factorAtMax) return false

        return true
    }

    override fun hashCode(): Int {
        var result = basis.hashCode()
        result = 31 * result + factorAtMin.hashCode()
        result = 31 * result + factorAtMax.hashCode()
        return result
    }

    override fun toString(): String {
        return "ResistanceConfig(basis=$basis, factorAtMin=$factorAtMin, factorAtMax=$factorAtMax)"
    }
}

/**
 *  Given an offset x and a set of anchors, return a list of anchors:
 *   1. [ ] if the set of anchors is empty,
 *   2. [ x' ] if x is equal to one of the anchors, accounting for a small rounding error, where x'
 *      is x rounded to the exact value of the matching anchor,
 *   3. [ min ] if min is the minimum anchor and x < min,
 *   4. [ max ] if max is the maximum anchor and x > max, or
 *   5. [ a , b ] if a and b are anchors such that a < x < b and b - a is minimal.
 */
private fun findBounds(
    offset: Float,
    anchors: Set<Float>
): List<Float> {
    // Find the anchors the target lies between with a little bit of rounding error.
    val a = anchors.filter { it <= offset + 0.001 }.fastMaxBy { it }
    val b = anchors.filter { it >= offset - 0.001 }.fastMinByOrNull { it }

    return when {
        a == null ->
            // case 1 or 3
            listOfNotNull(b)
        b == null ->
            // case 4
            listOf(a)
        a == b ->
            // case 2
            // Can't return offset itself here since it might not be exactly equal
            // to the anchor, despite being considered an exact match.
            listOf(a)
        else ->
            // case 5
            listOf(a, b)
    }
}

private fun computeTarget(
    offset: Float,
    lastValue: Float,
    anchors: Set<Float>,
    thresholds: (Float, Float) -> Float,
    velocity: Float,
    velocityThreshold: Float
): Float {
    val bounds = findBounds(offset, anchors)
    return when (bounds.size) {
        0 -> lastValue
        1 -> bounds[0]
        else -> {
            val lower = bounds[0]
            val upper = bounds[1]
            if (lastValue <= offset) {
                // Swiping from lower to upper (positive).
                if (velocity >= velocityThreshold) {
                    return upper
                } else {
                    val threshold = thresholds(lower, upper)
                    if (offset < threshold) lower else upper
                }
            } else {
                // Swiping from upper to lower (negative).
                if (velocity <= -velocityThreshold) {
                    return lower
                } else {
                    val threshold = thresholds(upper, lower)
                    if (offset > threshold) upper else lower
                }
            }
        }
    }
}

private fun <T> Map<Float, T>.getOffset(state: T): Float? {
    return entries.firstOrNull { it.value == state }?.key
}

/**
 * Contains useful defaults for [swipeable] and [SwipeableState].
 */
@Deprecated(SwipeableDeprecation)
object SwipeableDefaults {
    /**
     * The default animation used by [SwipeableState].
     */
    val AnimationSpec = SpringSpec<Float>()

    /**
     * The default velocity threshold (1.8 dp per millisecond) used by [swipeable].
     */
    val VelocityThreshold = 125.dp

    /**
     * A stiff resistance factor which indicates that swiping isn't available right now.
     */
    const val StiffResistanceFactor = 20f

    /**
     * A standard resistance factor which indicates that the user has run out of things to see.
     */
    const val StandardResistanceFactor = 10f

    /**
     * The default resistance config used by [swipeable].
     *
     * This returns `null` if there is one anchor. If there are at least two anchors, it returns
     * a [ResistanceConfig] with the resistance basis equal to the distance between the two bounds.
     */
    fun resistanceConfig(
        anchors: Set<Float>,
        factorAtMin: Float = StandardResistanceFactor,
        factorAtMax: Float = StandardResistanceFactor
    ): ResistanceConfig? {
        return if (anchors.size <= 1) {
            null
        } else {
            val basis = anchors.maxOrNull()!! - anchors.minOrNull()!!
            ResistanceConfig(basis, factorAtMin, factorAtMax)
        }
    }
}

// temp default nested scroll connection for swipeables which desire as an opt in
// revisit in b/174756744 as all types will have their own specific connection probably
@ExperimentalMaterialApi
internal val <T> SwipeableState<T>.PreUpPostDownNestedScrollConnection: NestedScrollConnection
    get() = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.UserInput) {
                performDrag(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            return if (source == NestedScrollSource.UserInput) {
                performDrag(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = Offset(available.x, available.y).toFloat()
            return if (toFling < 0 && offset.value > minBound) {
                performFling(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            performFling(velocity = Offset(available.x, available.y).toFloat())
            return available
        }

        private fun Float.toOffset(): Offset = Offset(0f, this)

        private fun Offset.toFloat(): Float = this.y
    }

private const val SwipeableDeprecation = "Material's Swipeable has been replaced by Foundation's " +
    "AnchoredDraggable APIs. Please see developer.android.com for an overview of the changes and " +
    "a migration guide."
