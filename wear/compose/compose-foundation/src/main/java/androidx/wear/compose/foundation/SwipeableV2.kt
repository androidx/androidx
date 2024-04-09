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

package androidx.wear.compose.foundation

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// This is copied from the SwipeableV2.kt in Material Compose (will be moved to Compose Foundation).
// TODO (b/261409021): Delete this once the API is not experimental.

/**
 * Enable swipe gestures between a set of predefined values.
 *
 * When a swipe is detected, the offset of the [SwipeableV2State] will be updated with the swipe
 * delta. You should use this offset to move your content accordingly (see [Modifier.offset]).
 * When the swipe ends, the offset will be animated to one of the anchors and when that anchor is
 * reached, the value of the [SwipeableV2State] will also be updated to the value corresponding to
 * the new anchor.
 *
 * Swiping is constrained between the minimum and maximum anchors.
 *
 * @param state The associated [SwipeableV2State].
 * @param orientation The orientation in which the swipeable can be swiped.
 * @param enabled Whether this [swipeableV2] is enabled and should react to the user's input.
 * @param reverseDirection Whether to reverse the direction of the swipe, so a top to bottom
 * swipe will behave like bottom to top, and a left to right swipe will behave like right to left.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to
 * the internal [Modifier.draggable].
 */
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public fun <T> Modifier.swipeableV2(
    state: SwipeableV2State<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null
): Modifier {
    // Swipeables publish scroll range semantics so they look like they can scroll between values
    // of 0 and 1, inclusive, so that AndroidComposeView can report a value from its canScroll
    // methods that correctly tells the system's ScrollDismissLayout whether it should intercept
    // touch values (see b/199908428). This logic is *not* duplicated in the non-Wear swipeable
    // because it's a bit of a hack to fix navigation in WearOS. Once swipeable implements proper
    // nested scrolling, and the two swipeable implementations are merged, this fake scrolling stuff
    // should be gone anyway. Also note that the regular Android swipe-to-go-back gesture works very
    // differently than the wear gesture so we don't need this workaround to support it.
    // TODO(b/201009199): Modifier.swipeableV2 should coordinate with the nested scrolling system.
    val semantics = if (!enabled) Modifier else Modifier.semantics {
        // Set a fake scroll range axis so that the AndroidComposeView can correctly report whether
        // scrolling is supported via canScroll{Horizontally,Vertically}.
        val range = ScrollAxisRange(
            value = {
                // Avoid dividing by 0.
                if (state.minOffset == state.maxOffset) {
                    0f
                } else {
                    val clampedOffset = (state.offset ?: 0f)
                        .coerceIn(state.minOffset, state.maxOffset)
                    // [0f, 1f] representing the fraction between the swipe bounds.
                    // Return the remaining fraction available to swipe.
                    (state.maxOffset - clampedOffset) / (state.maxOffset - state.minOffset)
                }
            },
            maxValue = { 1f },
            reverseScrolling = reverseDirection
        )
        when (orientation) {
            Orientation.Horizontal -> horizontalScrollAxisRange = range
            Orientation.Vertical -> verticalScrollAxisRange = range
        }
    }

    // Update the orientation in the swipeable state
    state.orientation = orientation

    return this.then(semantics).draggable(
        state = state.swipeDraggableState,
        orientation = orientation,
        enabled = enabled,
        interactionSource = interactionSource,
        reverseDirection = reverseDirection,
        startDragImmediately = state.isAnimationRunning,
        onDragStopped = { velocity -> launch { state.settle(velocity) } }
    )
}

/**
 * Define anchor points for a given [SwipeableV2State] based on this node's layout size and update
 * the state with them.
 *
 * @param state The associated [SwipeableV2State]
 * @param possibleValues All possible values the [SwipeableV2State] could be in.
 * @param anchorChangeHandler A callback to be invoked when the anchors have changed,
 * `null` by default. Components with custom reconciliation logic should implement this callback,
 * i.e. to re-target an in-progress animation.
 * @param calculateAnchor This method will be invoked to calculate the position of all
 * [possibleValues], given this node's layout size. Return the anchor's offset from the initial
 * anchor, or `null` to indicate that a value does not have an anchor.
 */
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public fun <T> Modifier.swipeAnchors(
    state: SwipeableV2State<T>,
    possibleValues: Set<T>,
    anchorChangeHandler: AnchorChangeHandler<T>? = null,
    calculateAnchor: (value: T, layoutSize: IntSize) -> Float?,
) = this.then(SwipeAnchorsModifier(
    onDensityChanged = { state.density = it },
    onSizeChanged = { layoutSize ->
        val previousAnchors = state.anchors
        val newAnchors = mutableMapOf<T, Float>()
        possibleValues.forEach {
            val anchorValue = calculateAnchor(it, layoutSize)
            if (anchorValue != null) {
                newAnchors[it] = anchorValue
            }
        }
        if (previousAnchors != newAnchors) {
            val previousTarget = state.targetValue
            val stateRequiresCleanup = state.updateAnchors(newAnchors)
            if (stateRequiresCleanup) {
                anchorChangeHandler?.onAnchorsChanged(
                    previousTarget,
                    previousAnchors,
                    newAnchors
                )
            }
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "swipeAnchors"
        properties["state"] = state
        properties["possibleValues"] = possibleValues
        properties["anchorChangeHandler"] = anchorChangeHandler
        properties["calculateAnchor"] = calculateAnchor
    }
))

/**
 * State of the [swipeableV2] modifier.
 *
 * This contains necessary information about any ongoing swipe or animation and provides methods
 * to change the state either immediately or by starting an animation. To create and remember a
 * [SwipeableV2State] use [rememberSwipeableV2State].
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold to be used when calculating the target state
 * while a swipe is in progress and when settling after the swipe ends. This is the distance from
 * the start of a transition. It will be, depending on the direction of the interaction, added or
 * subtracted from/to the origin offset. It should always be a positive value. See the
 * [fractionalPositionalThreshold] and [fixedPositionalThreshold] methods.
 * @param velocityThreshold The velocity threshold (in dp per second) that the end velocity has to
 * exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 * reached.
 */
@Stable
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public class SwipeableV2State<T>(
    initialValue: T,
    internal val animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    internal val confirmValueChange: (newValue: T) -> Boolean = { true },
    internal val positionalThreshold: Density.(totalDistance: Float) -> Float =
        SwipeableV2Defaults.PositionalThreshold,
    internal val velocityThreshold: Dp = SwipeableV2Defaults.VelocityThreshold,
    private val nestedScrollDispatcher: NestedScrollDispatcher? = null
) {

    private val swipeMutex = InternalMutatorMutex()

    internal val swipeDraggableState = object : DraggableState {
        private val dragScope = object : DragScope {
            override fun dragBy(pixels: Float) {
                this@SwipeableV2State.dispatchRawDelta(pixels)
            }
        }

        override suspend fun drag(
            dragPriority: MutatePriority,
            block: suspend DragScope.() -> Unit
        ) {
            swipe(dragPriority) { dragScope.block() }
        }

        override fun dispatchRawDelta(delta: Float) {
            this@SwipeableV2State.dispatchRawDelta(delta)
        }
    }

    /**
     * The orientation in which the swipeable can be swiped.
     */
    internal var orientation = Orientation.Horizontal

    /**
     * The current value of the [SwipeableV2State].
     */
    var currentValue: T by mutableStateOf(initialValue)
        private set

    /**
     * The target value. This is the closest value to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current value.
     */
    val targetValue: T by derivedStateOf {
        animationTarget ?: run {
            val currentOffset = offset
            if (currentOffset != null) {
                computeTarget(currentOffset, currentValue, velocity = 0f)
            } else currentValue
        }
    }

    /**
     * The current offset, or null if it has not been initialized yet.
     *
     * The offset will be initialized during the first measurement phase of the node that the
     * [swipeableV2] modifier is attached to. These are the phases:
     * Composition { -> Effects } -> Layout { Measurement -> Placement } -> Drawing
     * During the first composition, the offset will be null. In subsequent compositions, the offset
     * will be derived from the anchors of the previous pass.
     * Always prefer accessing the offset from a LaunchedEffect as it will be scheduled to be
     * executed the next frame, after layout.
     *
     * To guarantee stricter semantics, consider using [requireOffset].
     */
    @get:Suppress("AutoBoxing")
    var offset: Float? by mutableStateOf(null)
        private set

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float = checkNotNull(offset) {
        "The offset was read before being initialized. Did you access the offset in a phase " +
            "before layout, like effects or composition?"
    }

    /**
     * Whether an animation is currently in progress.
     */
    val isAnimationRunning: Boolean get() = animationTarget != null

    /**
     * The fraction of the progress going from [currentValue] to [targetValue], within [0f..1f]
     * bounds.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val progress: Float by derivedStateOf {
        val a = anchors[currentValue] ?: 0f
        val b = anchors[targetValue] ?: 0f
        val distance = abs(b - a)
        if (distance > 1e-6f) {
            val progress = (this.requireOffset() - a) / (b - a)
            // If we are very close to 0f or 1f, we round to the closest
            if (progress < 1e-6f) 0f else if (progress > 1 - 1e-6f) 1f else progress
        } else 1f
    }

    /**
     * The velocity of the last known animation. Gets reset to 0f when an animation completes
     * successfully, but does not get reset when an animation gets interrupted.
     * You can use this value to provide smooth reconciliation behavior when re-targeting an
     * animation.
     */
    var lastVelocity: Float by mutableFloatStateOf(0f)
        private set

    /**
     * The minimum offset this state can reach. This will be the smallest anchor, or
     * [Float.NEGATIVE_INFINITY] if the anchors are not initialized yet.
     */
    val minOffset by derivedStateOf { anchors.minOrNull() ?: Float.NEGATIVE_INFINITY }

    /**
     * The maximum offset this state can reach. This will be the biggest anchor, or
     * [Float.POSITIVE_INFINITY] if the anchors are not initialized yet.
     */
    val maxOffset by derivedStateOf { anchors.maxOrNull() ?: Float.POSITIVE_INFINITY }

    private var animationTarget: T? by mutableStateOf(null)

    internal var anchors by mutableStateOf(emptyMap<T, Float>())

    internal var density: Density? = null

    /**
     * Update the anchors.
     * If the previous set of anchors was empty, attempt to update the offset to match the initial
     * value's anchor.
     *
     * @return true if the state needs to be adjusted after updating the anchors, e.g. if the
     * initial value is not found in the initial set of anchors. false if no further updates are
     * needed.
     */
    internal fun updateAnchors(newAnchors: Map<T, Float>): Boolean {
        val previousAnchorsEmpty = anchors.isEmpty()
        anchors = newAnchors
        val initialValueHasAnchor = if (previousAnchorsEmpty) {
            val initialValue = currentValue
            val initialValueAnchor = anchors[initialValue]
            val initialValueHasAnchor = initialValueAnchor != null
            if (initialValueHasAnchor) trySnapTo(initialValue)
            initialValueHasAnchor
        } else true
        return !initialValueHasAnchor || !previousAnchorsEmpty
    }

    /**
     * Whether the [value] has an anchor associated with it.
     */
    fun hasAnchorForValue(value: T): Boolean = anchors.containsKey(value)

    /**
     * Snap to a [targetValue] without any animation.
     * If the [targetValue] is not in the set of anchors, the [currentValue] will be updated to the
     * [targetValue] without updating the offset.
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetValue The target value of the animation
     */
    suspend fun snapTo(targetValue: T) {
        swipe { snap(targetValue) }
    }

    /**
     * Animate to a [targetValue].
     * If the [targetValue] is not in the set of anchors, the [currentValue] will be updated to the
     * [targetValue] without updating the offset.
     *
     * @throws CancellationException if the interaction interrupted by another interaction like a
     * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
     *
     * @param targetValue The target value of the animation
     * @param velocity The velocity the animation should start with, [lastVelocity] by default
     */
    suspend fun animateTo(
        targetValue: T,
        velocity: Float = lastVelocity,
    ) {
        val targetOffset = anchors[targetValue]
        if (targetOffset != null) {
            try {
                swipe {
                    animationTarget = targetValue
                    var prev = offset ?: 0f
                    animate(prev, targetOffset, velocity, animationSpec) { value, velocity ->
                        // Our onDrag coerces the value within the bounds, but an animation may
                        // overshoot, for example a spring animation or an overshooting interpolator
                        // We respect the user's intention and allow the overshoot, but still use
                        // DraggableState's drag for its mutex.
                        offset = value
                        prev = value
                        lastVelocity = velocity
                    }
                    lastVelocity = 0f
                }
            } finally {
                animationTarget = null
                val endOffset = requireOffset()
                val endState = anchors
                    .entries
                    .firstOrNull { (_, anchorOffset) -> abs(anchorOffset - endOffset) < 0.5f }
                    ?.key
                this.currentValue = endState ?: currentValue
            }
        } else {
            currentValue = targetValue
        }
    }

    /**
     * Find the closest anchor taking into account the velocity and settle at it with an animation.
     */
    suspend fun settle(velocity: Float) {
        var availableVelocity = velocity
        // Dispatch the velocity to parent nodes for consuming
        nestedScrollDispatcher?.let {
            val consumedVelocity = nestedScrollDispatcher.dispatchPreFling(
                if (orientation == Orientation.Horizontal) {
                    Velocity(x = velocity, y = 0f)
                } else {
                    Velocity(x = 0f, y = velocity)
                }
            )
            availableVelocity -= (consumedVelocity.x + consumedVelocity.y)
        }
        val previousValue = this.currentValue
        val targetValue = computeTarget(
            offset = requireOffset(),
            currentValue = previousValue,
            velocity = availableVelocity
        )
        if (confirmValueChange(targetValue)) {
            animateTo(targetValue, availableVelocity)
        } else {
            // If the user vetoed the state change, rollback to the previous state.
            animateTo(previousValue, availableVelocity)
        }
    }

    /**
     * Swipe by the [delta], coerce it in the bounds and dispatch it to the [SwipeableV2State].
     *
     * @return The delta the consumed by the [SwipeableV2State]
     */
    @Suppress("DEPRECATION") // b/327155912
    fun dispatchRawDelta(delta: Float): Float {
        var remainingDelta = delta

        // Dispatch the delta as a scroll event to parent node for consuming it
        nestedScrollDispatcher?.let {
            val consumedByParent = nestedScrollDispatcher.dispatchPreScroll(
                available = offsetWithOrientation(remainingDelta),
                source = NestedScrollSource.Drag
            )
            remainingDelta -= (consumedByParent.x + consumedByParent.y)
        }
        val currentDragPosition = offset ?: 0f
        val potentiallyConsumed = currentDragPosition + remainingDelta
        val clamped = potentiallyConsumed.coerceIn(minOffset, maxOffset)
        val deltaToConsume = clamped - currentDragPosition
        if (abs(deltaToConsume) >= 0) {
            offset = ((offset ?: 0f) + deltaToConsume).coerceIn(minOffset, maxOffset)
        }

        nestedScrollDispatcher?.let {
            val consumedDelta = nestedScrollDispatcher.dispatchPostScroll(
                consumed = offsetWithOrientation(deltaToConsume),
                available = offsetWithOrientation(delta - deltaToConsume),
                source = NestedScrollSource.Drag
            )
            remainingDelta -= (deltaToConsume + consumedDelta.x + consumedDelta.y)
        }
        return remainingDelta
    }

    private fun offsetWithOrientation(delta: Float): Offset {
        return if (orientation == Orientation.Horizontal) {
            Offset(x = delta, y = 0f)
        } else {
            Offset(x = 0f, y = delta)
        }
    }

    private fun computeTarget(
        offset: Float,
        currentValue: T,
        velocity: Float
    ): T {
        val currentAnchors = anchors
        val currentAnchor = currentAnchors[currentValue]
        val currentDensity = requireDensity()
        val velocityThresholdPx = with(currentDensity) { velocityThreshold.toPx() }
        return if (currentAnchor == offset || currentAnchor == null) {
            currentValue
        } else if (currentAnchor < offset) {
            // Swiping from lower to upper (positive).
            if (velocity >= velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, true)
            } else {
                val upper = currentAnchors.closestAnchor(offset, true)
                val distance = abs(currentAnchors.getValue(upper) - currentAnchor)
                val relativeThreshold = abs(positionalThreshold(currentDensity, distance))
                val absoluteThreshold = abs(currentAnchor + relativeThreshold)
                if (offset < 0) {
                    if (abs(offset) > absoluteThreshold) currentValue else upper
                } else {
                    if (offset < absoluteThreshold) currentValue else upper
                }
            }
        } else {
            // Swiping from upper to lower (negative).
            if (velocity <= -velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, false)
            } else {
                val lower = currentAnchors.closestAnchor(offset, false)
                val distance = abs(currentAnchor - currentAnchors.getValue(lower))
                val relativeThreshold = abs(positionalThreshold(currentDensity, distance))
                val absoluteThreshold = abs(currentAnchor - relativeThreshold)
                if (offset < 0) {
                    // For negative offsets, larger absolute thresholds are closer to lower anchors
                    // than smaller ones.
                    if (abs(offset) < absoluteThreshold) currentValue else lower
                } else {
                    if (offset > absoluteThreshold) currentValue else lower
                }
            }
        }
    }

    private fun requireDensity() = requireNotNull(density) {
        "SwipeableState did not have a density attached. Are you using Modifier.swipeable with " +
            "this=$this SwipeableState?"
    }

    private suspend fun swipe(
        swipePriority: MutatePriority = MutatePriority.Default,
        action: suspend () -> Unit
    ): Unit = coroutineScope { swipeMutex.mutate(swipePriority, action) }

    /**
     * Attempt to snap synchronously. Snapping can happen synchronously when there is no other swipe
     * transaction like a drag or an animation is progress. If there is another interaction in
     * progress, the suspending [snapTo] overload needs to be used.
     *
     * @return true if the synchronous snap was successful, or false if we couldn't snap synchronous
     */
    internal fun trySnapTo(targetValue: T): Boolean = swipeMutex.tryMutate { snap(targetValue) }

    private fun snap(targetValue: T) {
        val targetOffset = anchors[targetValue]
        if (targetOffset != null) {
            dispatchRawDelta(targetOffset - (offset ?: 0f))
            currentValue = targetValue
            animationTarget = null
        } else {
            currentValue = targetValue
        }
    }

    companion object {
        /**
         * The default [Saver] implementation for [SwipeableV2State].
         */
        @ExperimentalWearFoundationApi
        fun <T : Any> Saver(
            animationSpec: AnimationSpec<Float>,
            confirmValueChange: (T) -> Boolean,
            positionalThreshold: Density.(distance: Float) -> Float,
            velocityThreshold: Dp
        ) = Saver<SwipeableV2State<T>, T>(
            save = { it.currentValue },
            restore = {
                SwipeableV2State(
                    initialValue = it,
                    animationSpec = animationSpec,
                    confirmValueChange = confirmValueChange,
                    positionalThreshold = positionalThreshold,
                    velocityThreshold = velocityThreshold
                )
            }
        )
    }
}

/**
 * Create and remember a [SwipeableV2State].
 *
 * @param initialValue The initial value.
 * @param animationSpec The default animation that will be used to animate to a new value.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending value change.
 */
@Composable
@ExperimentalWearFoundationApi
internal fun <T : Any> rememberSwipeableV2State(
    initialValue: T,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (newValue: T) -> Boolean = { true }
): SwipeableV2State<T> {
    return rememberSaveable(
        initialValue, animationSpec, confirmValueChange,
        saver = SwipeableV2State.Saver(
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = SwipeableV2Defaults.PositionalThreshold,
            velocityThreshold = SwipeableV2Defaults.VelocityThreshold
        ),
    ) {
        SwipeableV2State(
            initialValue = initialValue,
            animationSpec = animationSpec,
            confirmValueChange = confirmValueChange,
            positionalThreshold = SwipeableV2Defaults.PositionalThreshold,
            velocityThreshold = SwipeableV2Defaults.VelocityThreshold
        )
    }
}

/**
 * Expresses a fixed positional threshold of [threshold] dp. This will be the distance from an
 * anchor that needs to be reached for [SwipeableV2State] to settle to the next closest anchor.
 *
 * @see [fractionalPositionalThreshold] for a fractional positional threshold
 */
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public fun fixedPositionalThreshold(threshold: Dp): Density.(distance: Float) -> Float = {
    threshold.toPx()
}

/**
 * Expresses a relative positional threshold of the [fraction] of the distance to the closest anchor
 * in the current direction. This will be the distance from an anchor that needs to be reached for
 * [SwipeableV2State] to settle to the next closest anchor.
 *
 * @see [fixedPositionalThreshold] for a fixed positional threshold
 */
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public fun fractionalPositionalThreshold(
    fraction: Float
): Density.(distance: Float) -> Float = { distance -> distance * fraction }

/**
 * Contains useful defaults for [swipeableV2] and [SwipeableV2State].
 */
@Stable
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public object SwipeableV2Defaults {
    /**
     * The default animation used by [SwipeableV2State].
     */
    val AnimationSpec = SpringSpec<Float>()

    /**
     * The default velocity threshold (1.8 dp per millisecond) used by [rememberSwipeableV2State].
     */
    val VelocityThreshold: Dp = 125.dp

    /**
     * The default positional threshold (56 dp) used by [rememberSwipeableV2State]
     */
    val PositionalThreshold: Density.(totalDistance: Float) -> Float =
        fixedPositionalThreshold(56.dp)

    /**
     * A [AnchorChangeHandler] implementation that attempts to reconcile an in-progress animation
     * by re-targeting it if necessary or finding the closest new anchor.
     * If the previous anchor is not in the new set of anchors, this implementation will snap to the
     * closest anchor.
     *
     * Consider implementing a custom handler for more complex components like sheets.
     * The [animate] and [snap] lambdas hoist the animation and snap logic. Usually these will just
     * delegate to [SwipeableV2State].
     *
     * @param state The [SwipeableV2State] the change handler will read from
     * @param animate A lambda that gets invoked to start an animation to a new target
     * @param snap A lambda that gets invoked to snap to a new target
     */
    @ExperimentalWearFoundationApi
    internal fun <T> ReconcileAnimationOnAnchorChangeHandler(
        state: SwipeableV2State<T>,
        animate: (target: T, velocity: Float) -> Unit,
        snap: (target: T) -> Unit
    ) = AnchorChangeHandler { previousTarget, previousAnchors, newAnchors ->
        val previousTargetOffset = previousAnchors[previousTarget]
        val newTargetOffset = newAnchors[previousTarget]
        if (previousTargetOffset != newTargetOffset) {
            if (newTargetOffset != null) {
                animate(previousTarget, state.lastVelocity)
            } else {
                snap(newAnchors.closestAnchor(offset = state.requireOffset()))
            }
        }
    }
}

/**
 * Defines a callback that is invoked when the anchors have changed.
 *
 * Components with custom reconciliation logic should implement this callback, for example to
 * re-target an in-progress animation when the anchors change.
 *
 * @see SwipeableV2Defaults.ReconcileAnimationOnAnchorChangeHandler for a default implementation
 */
@ExperimentalWearFoundationApi
@RestrictTo(LIBRARY_GROUP)
public fun interface AnchorChangeHandler<T> {

    /**
     * Callback that is invoked when the anchors have changed, after the [SwipeableV2State] has been
     * updated with them. Use this hook to re-launch animations or interrupt them if needed.
     *
     * @param previousTargetValue The target value before the anchors were updated
     * @param previousAnchors The previously set anchors
     * @param newAnchors The newly set anchors
     */
    fun onAnchorsChanged(
        previousTargetValue: T,
        previousAnchors: Map<T, Float>,
        newAnchors: Map<T, Float>
    )
}

@Stable
private class SwipeAnchorsModifier(
    private val onDensityChanged: (density: Density) -> Unit,
    private val onSizeChanged: (layoutSize: IntSize) -> Unit,
    inspectorInfo: InspectorInfo.() -> Unit,
) : LayoutModifier, OnRemeasuredModifier, InspectorValueInfo(inspectorInfo) {

    private var lastDensity: Float = -1f
    private var lastFontScale: Float = -1f

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        if (density != lastDensity || fontScale != lastFontScale) {
            onDensityChanged(Density(density, fontScale))
            lastDensity = density
            lastFontScale = fontScale
        }
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun onRemeasured(size: IntSize) {
        onSizeChanged(size)
    }

    override fun toString() = "SwipeAnchorsModifierImpl(updateDensity=$onDensityChanged, " +
        "onSizeChanged=$onSizeChanged)"
}

private fun <T> Map<T, Float>.closestAnchor(
    offset: Float = 0f,
    searchUpwards: Boolean = false
): T {
    require(isNotEmpty()) { "The anchors were empty when trying to find the closest anchor" }
    return minBy { (_, anchor) ->
        val delta = if (searchUpwards) anchor - offset else offset - anchor
        if (delta < 0) Float.POSITIVE_INFINITY else delta
    }.key
}

private fun <T> Map<T, Float>.minOrNull() = minOfOrNull { (_, offset) -> offset }
private fun <T> Map<T, Float>.maxOrNull() = maxOfOrNull { (_, offset) -> offset }
