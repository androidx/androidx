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

@file:Suppress("PrimitiveInCollection")

package androidx.compose.material3.internal

/**
 * This is a copy of androidx.compose.foundation.gestures.AnchoredDraggable until that API is
 * promoted to stable in foundation. Any changes there should be replicated here.
 */
import androidx.annotation.FloatRange
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Structure that represents the anchors of a [AnchoredDraggableState].
 *
 * See the DraggableAnchors factory method to construct drag anchors using a default implementation.
 */
internal interface DraggableAnchors<T> {

    /**
     * Get the anchor position for an associated [value]
     *
     * @return The position of the anchor, or [Float.NaN] if the anchor does not exist
     */
    fun positionOf(value: T): Float

    /**
     * Whether there is an anchor position associated with the [value]
     *
     * @param value The value to look up
     * @return true if there is an anchor for this value, false if there is no anchor for this value
     */
    fun hasAnchorFor(value: T): Boolean

    /**
     * Find the closest anchor to the [position].
     *
     * @param position The position to start searching from
     * @return The closest anchor or null if the anchors are empty
     */
    fun closestAnchor(position: Float): T?

    /**
     * Find the closest anchor to the [position], in the specified direction.
     *
     * @param position The position to start searching from
     * @param searchUpwards Whether to search upwards from the current position or downwards
     * @return The closest anchor or null if the anchors are empty
     */
    fun closestAnchor(position: Float, searchUpwards: Boolean): T?

    /** The smallest anchor, or [Float.NEGATIVE_INFINITY] if the anchors are empty. */
    fun minAnchor(): Float

    /** The biggest anchor, or [Float.POSITIVE_INFINITY] if the anchors are empty. */
    fun maxAnchor(): Float

    /** The amount of anchors */
    val size: Int
}

/**
 * [DraggableAnchorsConfig] stores a mutable configuration anchors, comprised of values of [T] and
 * corresponding [Float] positions. This [DraggableAnchorsConfig] is used to construct an immutable
 * [DraggableAnchors] instance later on.
 */
internal class DraggableAnchorsConfig<T> {

    internal val anchors = mutableMapOf<T, Float>()

    /**
     * Set the anchor position for [this] anchor.
     *
     * @param position The anchor position.
     */
    @Suppress("BuilderSetStyle")
    infix fun T.at(position: Float) {
        anchors[this] = position
    }
}

/**
 * Create a new [DraggableAnchors] instance using a builder function.
 *
 * @param builder A function with a [DraggableAnchorsConfig] that offers APIs to configure anchors
 * @return A new [DraggableAnchors] instance with the anchor positions set by the `builder`
 *   function.
 */
internal fun <T : Any> DraggableAnchors(
    builder: DraggableAnchorsConfig<T>.() -> Unit
): DraggableAnchors<T> = MapDraggableAnchors(DraggableAnchorsConfig<T>().apply(builder).anchors)

/**
 * Enable drag gestures between a set of predefined values.
 *
 * When a drag is detected, the offset of the [AnchoredDraggableState] will be updated with the drag
 * delta. You should use this offset to move your content accordingly (see [Modifier.offset]). When
 * the drag ends, the offset will be animated to one of the anchors and when that anchor is reached,
 * the value of the [AnchoredDraggableState] will also be updated to the value corresponding to the
 * new anchor.
 *
 * Dragging is constrained between the minimum and maximum anchors.
 *
 * @param state The associated [AnchoredDraggableState].
 * @param orientation The orientation in which the [anchoredDraggable] can be dragged.
 * @param enabled Whether this [anchoredDraggable] is enabled and should react to the user's input.
 * @param reverseDirection Whether to reverse the direction of the drag, so a top to bottom drag
 *   will behave like bottom to top, and a left to right drag will behave like right to left.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to the internal
 *   [Modifier.draggable].
 */
internal fun <T> Modifier.anchoredDraggable(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    interactionSource: MutableInteractionSource? = null
) =
    draggable(
        state = state.draggableState,
        orientation = orientation,
        enabled = enabled,
        interactionSource = interactionSource,
        reverseDirection = reverseDirection,
        startDragImmediately = state.isAnimationRunning,
        onDragStopped = { velocity -> launch { state.settle(velocity) } }
    )

/**
 * Scope used for suspending anchored drag blocks. Allows to set [AnchoredDraggableState.offset] to
 * a new value.
 *
 * @see [AnchoredDraggableState.anchoredDrag] to learn how to start the anchored drag and get the
 *   access to this scope.
 */
internal interface AnchoredDragScope {
    /**
     * Assign a new value for an offset value for [AnchoredDraggableState].
     *
     * @param newOffset new value for [AnchoredDraggableState.offset].
     * @param lastKnownVelocity last known velocity (if known)
     */
    fun dragTo(newOffset: Float, lastKnownVelocity: Float = 0f)
}

/**
 * State of the [anchoredDraggable] modifier. Use the constructor overload with anchors if the
 * anchors are defined in composition, or update the anchors using [updateAnchors].
 *
 * This contains necessary information about any ongoing drag or animation and provides methods to
 * change the state either immediately or by starting an animation.
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
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Stable
internal class AnchoredDraggableState<T>(
    initialValue: T,
    internal val positionalThreshold: (totalDistance: Float) -> Float,
    internal val velocityThreshold: () -> Float,
    val animationSpec: () -> AnimationSpec<Float>,
    internal val confirmValueChange: (newValue: T) -> Boolean = { true }
) {

    /**
     * Construct an [AnchoredDraggableState] instance with anchors.
     *
     * @param initialValue The initial value of the state.
     * @param anchors The anchors of the state. Use [updateAnchors] to update the anchors later.
     * @param animationSpec The default animation that will be used to animate to a new state.
     * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
     *   change.
     * @param positionalThreshold The positional threshold, in px, to be used when calculating the
     *   target state while a drag is in progress and when settling after the drag ends. This is the
     *   distance from the start of a transition. It will be, depending on the direction of the
     *   interaction, added or subtracted from/to the origin offset. It should always be a positive
     *   value.
     * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has
     *   to exceed in order to animate to the next state, even if the [positionalThreshold] has not
     *   been reached.
     */
    constructor(
        initialValue: T,
        anchors: DraggableAnchors<T>,
        positionalThreshold: (totalDistance: Float) -> Float,
        velocityThreshold: () -> Float,
        animationSpec: () -> AnimationSpec<Float>,
        confirmValueChange: (newValue: T) -> Boolean = { true }
    ) : this(
        initialValue,
        positionalThreshold,
        velocityThreshold,
        animationSpec,
        confirmValueChange
    ) {
        this.anchors = anchors
        trySnapTo(initialValue)
    }

    private val dragMutex = InternalMutatorMutex()

    internal val draggableState =
        object : DraggableState {

            private val dragScope =
                object : DragScope {
                    override fun dragBy(pixels: Float) {
                        with(anchoredDragScope) { dragTo(newOffsetForDelta(pixels)) }
                    }
                }

            override suspend fun drag(
                dragPriority: MutatePriority,
                block: suspend DragScope.() -> Unit
            ) {
                this@AnchoredDraggableState.anchoredDrag(dragPriority) {
                    with(dragScope) { block() }
                }
            }

            override fun dispatchRawDelta(delta: Float) {
                this@AnchoredDraggableState.dispatchRawDelta(delta)
            }
        }

    /** The current value of the [AnchoredDraggableState]. */
    var currentValue: T by mutableStateOf(initialValue)
        private set

    /**
     * The target value. This is the closest value to the current offset, taking into account
     * positional thresholds. If no interactions like animations or drags are in progress, this will
     * be the current value.
     */
    val targetValue: T by derivedStateOf {
        dragTarget
            ?: run {
                val currentOffset = offset
                if (!currentOffset.isNaN()) {
                    computeTarget(currentOffset, currentValue, velocity = 0f)
                } else currentValue
            }
    }

    /**
     * The closest value in the swipe direction from the current offset, not considering thresholds.
     * If an [anchoredDrag] is in progress, this will be the target of that anchoredDrag (if
     * specified).
     */
    internal val closestValue: T by derivedStateOf {
        dragTarget
            ?: run {
                val currentOffset = offset
                if (!currentOffset.isNaN()) {
                    computeTargetWithoutThresholds(currentOffset, currentValue)
                } else currentValue
            }
    }

    /**
     * The current offset, or [Float.NaN] if it has not been initialized yet.
     *
     * The offset will be initialized when the anchors are first set through [updateAnchors].
     *
     * Strongly consider using [requireOffset] which will throw if the offset is read before it is
     * initialized. This helps catch issues early in your workflow.
     */
    var offset: Float by mutableFloatStateOf(Float.NaN)
        private set

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     * @see offset
     */
    fun requireOffset(): Float {
        check(!offset.isNaN()) {
            "The offset was read before being initialized. Did you access the offset in a phase " +
                "before layout, like effects or composition?"
        }
        return offset
    }

    /** Whether an animation is currently in progress. */
    val isAnimationRunning: Boolean
        get() = dragTarget != null

    /**
     * The fraction of the progress going from [currentValue] to [closestValue], within [0f..1f]
     * bounds, or 1f if the [AnchoredDraggableState] is in a settled state.
     */
    @get:FloatRange(from = 0.0, to = 1.0)
    val progress: Float by
        derivedStateOf(structuralEqualityPolicy()) {
            val a = anchors.positionOf(currentValue)
            val b = anchors.positionOf(closestValue)
            val distance = abs(b - a)
            if (!distance.isNaN() && distance > 1e-6f) {
                val progress = (this.requireOffset() - a) / (b - a)
                // If we are very close to 0f or 1f, we round to the closest
                if (progress < 1e-6f) 0f else if (progress > 1 - 1e-6f) 1f else progress
            } else 1f
        }

    /**
     * The velocity of the last known animation. Gets reset to 0f when an animation completes
     * successfully, but does not get reset when an animation gets interrupted. You can use this
     * value to provide smooth reconciliation behavior when re-targeting an animation.
     */
    var lastVelocity: Float by mutableFloatStateOf(0f)
        private set

    private var dragTarget: T? by mutableStateOf(null)

    var anchors: DraggableAnchors<T> by mutableStateOf(emptyDraggableAnchors())
        private set

    /**
     * Update the anchors. If there is no ongoing [anchoredDrag] operation, snap to the [newTarget],
     * otherwise restart the ongoing [anchoredDrag] operation (e.g. an animation) with the new
     * anchors.
     *
     * <b>If your anchors depend on the size of the layout, updateAnchors should be called in the
     * layout (placement) phase, e.g. through Modifier.onSizeChanged.</b> This ensures that the
     * state is set up within the same frame. For static anchors, or anchors with different data
     * dependencies, [updateAnchors] is safe to be called from side effects or layout.
     *
     * @param newAnchors The new anchors.
     * @param newTarget The new target, by default the closest anchor or the current target if there
     *   are no anchors.
     */
    fun updateAnchors(
        newAnchors: DraggableAnchors<T>,
        newTarget: T =
            if (!offset.isNaN()) {
                newAnchors.closestAnchor(offset) ?: targetValue
            } else targetValue
    ) {
        if (anchors != newAnchors) {
            anchors = newAnchors
            // Attempt to snap. If nobody is holding the lock, we can immediately update the offset.
            // If anybody is holding the lock, we send a signal to restart the ongoing work with the
            // updated anchors.
            val snapSuccessful = trySnapTo(newTarget)
            if (!snapSuccessful) {
                dragTarget = newTarget
            }
        }
    }

    /**
     * Find the closest anchor, taking into account the [velocityThreshold] and
     * [positionalThreshold], and settle at it with an animation.
     *
     * If the [velocity] is lower than the [velocityThreshold], the closest anchor by distance and
     * [positionalThreshold] will be the target. If the [velocity] is higher than the
     * [velocityThreshold], the [positionalThreshold] will <b>not</b> be considered and the next
     * anchor in the direction indicated by the sign of the [velocity] will be the target.
     */
    suspend fun settle(velocity: Float) {
        val previousValue = this.currentValue
        val targetValue =
            computeTarget(
                offset = requireOffset(),
                currentValue = previousValue,
                velocity = velocity
            )
        if (confirmValueChange(targetValue)) {
            animateTo(targetValue, velocity)
        } else {
            // If the user vetoed the state change, rollback to the previous state.
            animateTo(previousValue, velocity)
        }
    }

    private fun computeTarget(offset: Float, currentValue: T, velocity: Float): T {
        val currentAnchors = anchors
        val currentAnchorPosition = currentAnchors.positionOf(currentValue)
        val velocityThresholdPx = velocityThreshold()
        return if (currentAnchorPosition == offset || currentAnchorPosition.isNaN()) {
            currentValue
        } else if (currentAnchorPosition < offset) {
            // Swiping from lower to upper (positive).
            if (velocity >= velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, true)!!
            } else {
                val upper = currentAnchors.closestAnchor(offset, true)!!
                val distance = abs(currentAnchors.positionOf(upper) - currentAnchorPosition)
                val relativeThreshold = abs(positionalThreshold(distance))
                val absoluteThreshold = abs(currentAnchorPosition + relativeThreshold)
                if (offset < absoluteThreshold) currentValue else upper
            }
        } else {
            // Swiping from upper to lower (negative).
            if (velocity <= -velocityThresholdPx) {
                currentAnchors.closestAnchor(offset, false)!!
            } else {
                val lower = currentAnchors.closestAnchor(offset, false)!!
                val distance = abs(currentAnchorPosition - currentAnchors.positionOf(lower))
                val relativeThreshold = abs(positionalThreshold(distance))
                val absoluteThreshold = abs(currentAnchorPosition - relativeThreshold)
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

    private fun computeTargetWithoutThresholds(
        offset: Float,
        currentValue: T,
    ): T {
        val currentAnchors = anchors
        val currentAnchorPosition = currentAnchors.positionOf(currentValue)
        return if (currentAnchorPosition == offset || currentAnchorPosition.isNaN()) {
            currentValue
        } else if (currentAnchorPosition < offset) {
            currentAnchors.closestAnchor(offset, true) ?: currentValue
        } else {
            currentAnchors.closestAnchor(offset, false) ?: currentValue
        }
    }

    private val anchoredDragScope: AnchoredDragScope =
        object : AnchoredDragScope {
            override fun dragTo(newOffset: Float, lastKnownVelocity: Float) {
                offset = newOffset
                lastVelocity = lastKnownVelocity
            }
        }

    /**
     * Call this function to take control of drag logic and perform anchored drag with the latest
     * anchors.
     *
     * All actions that change the [offset] of this [AnchoredDraggableState] must be performed
     * within an [anchoredDrag] block (even if they don't call any other methods on this object) in
     * order to guarantee that mutual exclusion is enforced.
     *
     * If [anchoredDrag] is called from elsewhere with the [dragPriority] higher or equal to ongoing
     * drag, the ongoing drag will be cancelled.
     *
     * <b>If the [anchors] change while the [block] is being executed, it will be cancelled and
     * re-executed with the latest anchors and target.</b> This allows you to target the correct
     * state.
     *
     * @param dragPriority of the drag operation
     * @param block perform anchored drag given the current anchor provided
     */
    suspend fun anchoredDrag(
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend AnchoredDragScope.(anchors: DraggableAnchors<T>) -> Unit
    ) {
        try {
            dragMutex.mutate(dragPriority) {
                restartable(inputs = { anchors }) { latestAnchors ->
                    anchoredDragScope.block(latestAnchors)
                }
            }
        } finally {
            val closest = anchors.closestAnchor(offset)
            if (
                closest != null &&
                    abs(offset - anchors.positionOf(closest)) <= 0.5f &&
                    confirmValueChange.invoke(closest)
            ) {
                currentValue = closest
            }
        }
    }

    /**
     * Call this function to take control of drag logic and perform anchored drag with the latest
     * anchors and target.
     *
     * All actions that change the [offset] of this [AnchoredDraggableState] must be performed
     * within an [anchoredDrag] block (even if they don't call any other methods on this object) in
     * order to guarantee that mutual exclusion is enforced.
     *
     * This overload allows the caller to hint the target value that this [anchoredDrag] is intended
     * to arrive to. This will set [AnchoredDraggableState.targetValue] to provided value so
     * consumers can reflect it in their UIs.
     *
     * <b>If the [anchors] or [AnchoredDraggableState.targetValue] change while the [block] is being
     * executed, it will be cancelled and re-executed with the latest anchors and target.</b> This
     * allows you to target the correct state.
     *
     * If [anchoredDrag] is called from elsewhere with the [dragPriority] higher or equal to ongoing
     * drag, the ongoing drag will be cancelled.
     *
     * @param targetValue hint the target value that this [anchoredDrag] is intended to arrive to
     * @param dragPriority of the drag operation
     * @param block perform anchored drag given the current anchor provided
     */
    suspend fun anchoredDrag(
        targetValue: T,
        dragPriority: MutatePriority = MutatePriority.Default,
        block: suspend AnchoredDragScope.(anchors: DraggableAnchors<T>, targetValue: T) -> Unit
    ) {
        if (anchors.hasAnchorFor(targetValue)) {
            try {
                dragMutex.mutate(dragPriority) {
                    dragTarget = if (confirmValueChange(targetValue)) targetValue else currentValue
                    restartable(inputs = { anchors to this@AnchoredDraggableState.targetValue }) {
                        (latestAnchors, latestTarget) ->
                        anchoredDragScope.block(latestAnchors, latestTarget)
                    }
                }
            } finally {
                dragTarget = null
                val closest = anchors.closestAnchor(offset)
                if (
                    closest != null &&
                        abs(offset - anchors.positionOf(closest)) <= 0.5f &&
                        confirmValueChange.invoke(closest)
                ) {
                    currentValue = closest
                }
            }
        } else {
            // Todo: b/283467401, revisit this behavior
            currentValue = targetValue
        }
    }

    internal fun newOffsetForDelta(delta: Float) =
        ((if (offset.isNaN()) 0f else offset) + delta).coerceIn(
            anchors.minAnchor(),
            anchors.maxAnchor()
        )

    /**
     * Drag by the [delta], coerce it in the bounds and dispatch it to the [AnchoredDraggableState].
     *
     * @return The delta the consumed by the [AnchoredDraggableState]
     */
    fun dispatchRawDelta(delta: Float): Float {
        val newOffset = newOffsetForDelta(delta)
        val oldOffset = if (offset.isNaN()) 0f else offset
        offset = newOffset
        return newOffset - oldOffset
    }

    /**
     * Attempt to snap synchronously. Snapping can happen synchronously when there is no other drag
     * transaction like a drag or an animation is progress. If there is another interaction in
     * progress, the suspending [snapTo] overload needs to be used.
     *
     * @return true if the synchronous snap was successful, or false if we couldn't snap synchronous
     */
    private fun trySnapTo(targetValue: T): Boolean =
        dragMutex.tryMutate {
            with(anchoredDragScope) {
                val targetOffset = anchors.positionOf(targetValue)
                if (!targetOffset.isNaN()) {
                    dragTo(targetOffset)
                    dragTarget = null
                }
                currentValue = targetValue
            }
        }

    companion object {
        /** The default [Saver] implementation for [AnchoredDraggableState]. */
        fun <T : Any> Saver(
            animationSpec: () -> AnimationSpec<Float>,
            confirmValueChange: (T) -> Boolean,
            positionalThreshold: (distance: Float) -> Float,
            velocityThreshold: () -> Float,
        ) =
            Saver<AnchoredDraggableState<T>, T>(
                save = { it.currentValue },
                restore = {
                    AnchoredDraggableState(
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
 * Snap to a [targetValue] without any animation. If the [targetValue] is not in the set of anchors,
 * the [AnchoredDraggableState.currentValue] will be updated to the [targetValue] without updating
 * the offset.
 *
 * @param targetValue The target value of the animation
 * @throws CancellationException if the interaction interrupted by another interaction like a
 *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
 */
internal suspend fun <T> AnchoredDraggableState<T>.snapTo(targetValue: T) {
    anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
        val targetOffset = anchors.positionOf(latestTarget)
        if (!targetOffset.isNaN()) dragTo(targetOffset)
    }
}

/**
 * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
 * [AnchoredDraggableState.currentValue] will be updated to the [targetValue] without updating the
 * offset.
 *
 * @param targetValue The target value of the animation
 * @param velocity The velocity the animation should start with
 * @throws CancellationException if the interaction interrupted by another interaction like a
 *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
 */
internal suspend fun <T> AnchoredDraggableState<T>.animateTo(
    targetValue: T,
    velocity: Float = this.lastVelocity,
) {
    anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
        val targetOffset = anchors.positionOf(latestTarget)
        if (!targetOffset.isNaN()) {
            var prev = if (offset.isNaN()) 0f else offset
            animate(prev, targetOffset, velocity, animationSpec.invoke()) { value, velocity ->
                // Our onDrag coerces the value within the bounds, but an animation may
                // overshoot, for example a spring animation or an overshooting interpolator
                // We respect the user's intention and allow the overshoot, but still use
                // DraggableState's drag for its mutex.
                dragTo(value, velocity)
                prev = value
            }
        }
    }
}

/** Contains useful defaults for [anchoredDraggable] and [AnchoredDraggableState]. */
@Stable
internal object AnchoredDraggableDefaults {
    /** The default animation used by [AnchoredDraggableState]. */
    val AnimationSpec = SpringSpec<Float>()
}

internal expect class AnchoredDragFinishedSignal() : CancellationException

private suspend fun <I> restartable(inputs: () -> I, block: suspend (I) -> Unit) {
    try {
        coroutineScope {
            var previousDrag: Job? = null
            snapshotFlow(inputs).collect { latestInputs ->
                previousDrag?.apply {
                    cancel(AnchoredDragFinishedSignal())
                    join()
                }
                previousDrag =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        block(latestInputs)
                        this@coroutineScope.cancel(AnchoredDragFinishedSignal())
                    }
            }
        }
    } catch (anchoredDragFinished: AnchoredDragFinishedSignal) {
        // Ignored
    }
}

private fun <T> emptyDraggableAnchors() = MapDraggableAnchors<T>(emptyMap())

private class MapDraggableAnchors<T>(private val anchors: Map<T, Float>) : DraggableAnchors<T> {

    override fun positionOf(value: T): Float = anchors[value] ?: Float.NaN

    override fun hasAnchorFor(value: T) = anchors.containsKey(value)

    override fun closestAnchor(position: Float): T? =
        anchors.minByOrNull { abs(position - it.value) }?.key

    override fun closestAnchor(position: Float, searchUpwards: Boolean): T? {
        return anchors
            .minByOrNull { (_, anchor) ->
                val delta = if (searchUpwards) anchor - position else position - anchor
                if (delta < 0) Float.POSITIVE_INFINITY else delta
            }
            ?.key
    }

    override fun minAnchor() = anchors.values.minOrNull() ?: Float.NaN

    override fun maxAnchor() = anchors.values.maxOrNull() ?: Float.NaN

    override val size: Int
        get() = anchors.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MapDraggableAnchors<*>) return false

        return anchors == other.anchors
    }

    override fun hashCode() = 31 * anchors.hashCode()

    override fun toString() = "MapDraggableAnchors($anchors)"
}

/**
 * This Modifier allows configuring an [AnchoredDraggableState]'s anchors based on this layout
 * node's size and offsetting it. It considers lookahead and reports the appropriate size and
 * measurement for the appropriate phase.
 *
 * @param state The state the anchors should be attached to
 * @param orientation The orientation the component should be offset in
 * @param anchors Lambda to calculate the anchors based on this layout's size and the incoming
 *   constraints. These can be useful to avoid subcomposition.
 */
internal fun <T> Modifier.draggableAnchors(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
) = this then DraggableAnchorsElement(state, anchors, orientation)

private class DraggableAnchorsElement<T>(
    private val state: AnchoredDraggableState<T>,
    private val anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    private val orientation: Orientation
) : ModifierNodeElement<DraggableAnchorsNode<T>>() {

    override fun create() = DraggableAnchorsNode(state, anchors, orientation)

    override fun update(node: DraggableAnchorsNode<T>) {
        node.state = state
        node.anchors = anchors
        node.orientation = orientation
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is DraggableAnchorsElement<*>) return false

        if (state != other.state) return false
        if (anchors !== other.anchors) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + anchors.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        debugInspectorInfo {
            properties["state"] = state
            properties["anchors"] = anchors
            properties["orientation"] = orientation
        }
    }
}

private class DraggableAnchorsNode<T>(
    var state: AnchoredDraggableState<T>,
    var anchors: (size: IntSize, constraints: Constraints) -> Pair<DraggableAnchors<T>, T>,
    var orientation: Orientation
) : Modifier.Node(), LayoutModifierNode {
    private var didLookahead: Boolean = false

    override fun onDetach() {
        didLookahead = false
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        // If we are in a lookahead pass, we only want to update the anchors here and not in
        // post-lookahead. If there is no lookahead happening (!isLookingAhead && !didLookahead),
        // update the anchors in the main pass.
        if (!isLookingAhead || !didLookahead) {
            val size = IntSize(placeable.width, placeable.height)
            val newAnchorResult = anchors(size, constraints)
            state.updateAnchors(newAnchorResult.first, newAnchorResult.second)
        }
        didLookahead = isLookingAhead || didLookahead
        return layout(placeable.width, placeable.height) {
            // In a lookahead pass, we use the position of the current target as this is where any
            // ongoing animations would move. If the component is in a settled state, lookahead
            // and post-lookahead will converge.
            val offset =
                if (isLookingAhead) {
                    state.anchors.positionOf(state.targetValue)
                } else state.requireOffset()
            val xOffset = if (orientation == Orientation.Horizontal) offset else 0f
            val yOffset = if (orientation == Orientation.Vertical) offset else 0f
            placeable.place(xOffset.roundToInt(), yOffset.roundToInt())
        }
    }
}
