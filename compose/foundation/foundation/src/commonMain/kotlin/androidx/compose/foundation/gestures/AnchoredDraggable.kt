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

package androidx.compose.foundation.gestures

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.DragEvent.DragDelta
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.PlatformOptimizedCancellationException
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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
 * @param reverseDirection Whether to reverse the direction of the drag, so a top to bottom drag
 *   will behave like bottom to top, and a left to right drag will behave like right to left. If not
 *   specified, this will be determined based on [orientation] and [LocalLayoutDirection] through
 *   the other [anchoredDraggable] overload.
 * @param orientation The orientation in which the [anchoredDraggable] can be dragged.
 * @param enabled Whether this [anchoredDraggable] is enabled and should react to the user's input.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to the internal
 *   [Modifier.draggable].
 * @param overscrollEffect optional effect to dispatch any excess delta or velocity to. The excess
 *   delta or velocity are a result of dragging/flinging and reaching the bounds. If you provide an
 *   [overscrollEffect], make sure to apply [androidx.compose.foundation.overscroll] to render the
 *   effect as well.
 * @param flingBehavior Optionally configure how the anchored draggable performs the fling. By
 *   default (if passing in null), this will snap to the closest anchor considering the velocity
 *   thresholds and positional thresholds. See [AnchoredDraggableDefaults.flingBehavior].
 */
fun <T> Modifier.anchoredDraggable(
    state: AnchoredDraggableState<T>,
    reverseDirection: Boolean,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    overscrollEffect: OverscrollEffect? = null,
    flingBehavior: FlingBehavior? = null,
): Modifier =
    this then
        AnchoredDraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            flingBehavior = flingBehavior,
        )

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
 * @param reverseDirection Whether to reverse the direction of the drag, so a top to bottom drag
 *   will behave like bottom to top, and a left to right drag will behave like right to left. If not
 *   specified, this will be determined based on [orientation] and [LocalLayoutDirection] through
 *   the other [anchoredDraggable] overload.
 * @param orientation The orientation in which the [anchoredDraggable] can be dragged.
 * @param enabled Whether this [anchoredDraggable] is enabled and should react to the user's input.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to the internal
 *   [Modifier.draggable].
 * @param overscrollEffect optional effect to dispatch any excess delta or velocity to. The excess
 *   delta or velocity are a result of dragging/flinging and reaching the bounds. If you provide an
 *   [overscrollEffect], make sure to apply [androidx.compose.foundation.overscroll] to render the
 *   effect as well.
 * @param startDragImmediately when set to false, [draggable] will start dragging only when the
 *   gesture crosses the touchSlop. This is useful to prevent users from "catching" an animating
 *   widget when pressing on it. See [draggable] to learn more about startDragImmediately.
 * @param flingBehavior Optionally configure how the anchored draggable performs the fling. By
 *   default (if passing in null), this will snap to the closest anchor considering the velocity
 *   thresholds and positional thresholds. See [AnchoredDraggableDefaults.flingBehavior].
 */
@Deprecated(StartDragImmediatelyDeprecated)
fun <T> Modifier.anchoredDraggable(
    state: AnchoredDraggableState<T>,
    reverseDirection: Boolean,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    overscrollEffect: OverscrollEffect? = null,
    startDragImmediately: Boolean = state.isAnimationRunning,
    flingBehavior: FlingBehavior? = null,
): Modifier =
    this then
        AnchoredDraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            startDragImmediately = startDragImmediately,
            flingBehavior = flingBehavior,
        )

/**
 * Enable drag gestures between a set of predefined values.
 *
 * When a drag is detected, the offset of the [AnchoredDraggableState] will be updated with the drag
 * delta. If the [orientation] is set to [Orientation.Horizontal] and [LocalLayoutDirection]'s value
 * is [LayoutDirection.Rtl], the drag deltas will be reversed. You should use this offset to move
 * your content accordingly (see [Modifier.offset]). When the drag ends, the offset will be animated
 * to one of the anchors and when that anchor is reached, the value of the [AnchoredDraggableState]
 * will also be updated to the value corresponding to the new anchor.
 *
 * Dragging is constrained between the minimum and maximum anchors.
 *
 * @param state The associated [AnchoredDraggableState].
 * @param orientation The orientation in which the [anchoredDraggable] can be dragged.
 * @param enabled Whether this [anchoredDraggable] is enabled and should react to the user's input.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to the internal
 *   [Modifier.draggable].
 * @param overscrollEffect optional effect to dispatch any excess delta or velocity to. The excess
 *   delta or velocity are a result of dragging/flinging and reaching the bounds. If you provide an
 *   [overscrollEffect], make sure to apply [androidx.compose.foundation.overscroll] to render the
 *   effect as well.
 * @param flingBehavior Optionally configure how the anchored draggable performs the fling. By
 *   default (if passing in null), this will snap to the closest anchor considering the velocity
 *   thresholds and positional thresholds. See [AnchoredDraggableDefaults.flingBehavior].
 */
fun <T> Modifier.anchoredDraggable(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    overscrollEffect: OverscrollEffect? = null,
    flingBehavior: FlingBehavior? = null,
): Modifier =
    this then
        AnchoredDraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = null,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            flingBehavior = flingBehavior,
        )

/**
 * Enable drag gestures between a set of predefined values.
 *
 * When a drag is detected, the offset of the [AnchoredDraggableState] will be updated with the drag
 * delta. If the [orientation] is set to [Orientation.Horizontal] and [LocalLayoutDirection]'s value
 * is [LayoutDirection.Rtl], the drag deltas will be reversed. You should use this offset to move
 * your content accordingly (see [Modifier.offset]). When the drag ends, the offset will be animated
 * to one of the anchors and when that anchor is reached, the value of the [AnchoredDraggableState]
 * will also be updated to the value corresponding to the new anchor.
 *
 * Dragging is constrained between the minimum and maximum anchors.
 *
 * @param state The associated [AnchoredDraggableState].
 * @param orientation The orientation in which the [anchoredDraggable] can be dragged.
 * @param enabled Whether this [anchoredDraggable] is enabled and should react to the user's input.
 * @param interactionSource Optional [MutableInteractionSource] that will passed on to the internal
 *   [Modifier.draggable].
 * @param overscrollEffect optional effect to dispatch any excess delta or velocity to. The excess
 *   delta or velocity are a result of dragging/flinging and reaching the bounds. If you provide an
 *   [overscrollEffect], make sure to apply [androidx.compose.foundation.overscroll] to render the
 *   effect as well.
 * @param startDragImmediately when set to false, [draggable] will start dragging only when the
 *   gesture crosses the touchSlop. This is useful to prevent users from "catching" an animating
 *   widget when pressing on it. See [draggable] to learn more about startDragImmediately.
 * @param flingBehavior Optionally configure how the anchored draggable performs the fling. By
 *   default (if passing in null), this will snap to the closest anchor considering the velocity
 *   thresholds and positional thresholds. See [AnchoredDraggableDefaults.flingBehavior].
 */
@Deprecated(StartDragImmediatelyDeprecated)
fun <T> Modifier.anchoredDraggable(
    state: AnchoredDraggableState<T>,
    orientation: Orientation,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    overscrollEffect: OverscrollEffect? = null,
    startDragImmediately: Boolean = state.isAnimationRunning,
    flingBehavior: FlingBehavior? = null,
): Modifier =
    this then
        AnchoredDraggableElement(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = null,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            startDragImmediately = startDragImmediately,
            flingBehavior = flingBehavior,
        )

private class AnchoredDraggableElement<T>(
    private val state: AnchoredDraggableState<T>,
    private val orientation: Orientation,
    private val enabled: Boolean,
    private val reverseDirection: Boolean?,
    private val interactionSource: MutableInteractionSource?,
    private val startDragImmediately: Boolean? = null,
    private val overscrollEffect: OverscrollEffect?,
    private val flingBehavior: FlingBehavior? = null,
) : ModifierNodeElement<AnchoredDraggableNode<T>>() {
    override fun create() =
        AnchoredDraggableNode(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            startDragImmediately = startDragImmediately,
            flingBehavior = flingBehavior,
        )

    override fun update(node: AnchoredDraggableNode<T>) {
        node.update(
            state = state,
            orientation = orientation,
            enabled = enabled,
            reverseDirection = reverseDirection,
            interactionSource = interactionSource,
            overscrollEffect = overscrollEffect,
            startDragImmediately = startDragImmediately,
            flingBehavior = flingBehavior,
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + startDragImmediately.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is AnchoredDraggableElement<*>) return false

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (interactionSource != other.interactionSource) return false
        if (startDragImmediately != other.startDragImmediately) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (flingBehavior != other.flingBehavior) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "anchoredDraggable"
        properties["state"] = state
        properties["orientation"] = orientation
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["interactionSource"] = interactionSource
        properties["startDragImmediately"] = startDragImmediately
        properties["overscrollEffect"] = overscrollEffect
        properties["flingBehavior"] = flingBehavior
    }
}

private class AnchoredDraggableNode<T>(
    private var state: AnchoredDraggableState<T>,
    private var orientation: Orientation,
    enabled: Boolean,
    private var reverseDirection: Boolean?,
    interactionSource: MutableInteractionSource?,
    private var overscrollEffect: OverscrollEffect?,
    private var startDragImmediately: Boolean?,
    private var flingBehavior: FlingBehavior?,
) :
    DragGestureNode(
        canDrag = AlwaysDrag,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = orientation,
    ) {

    lateinit var resolvedFlingBehavior: FlingBehavior
    private var density: Density? = null

    private val isReverseDirection: Boolean
        get() =
            when (reverseDirection) {
                null ->
                    requireLayoutDirection() == LayoutDirection.Rtl &&
                        orientation == Orientation.Horizontal
                else -> reverseDirection!!
            }

    override fun onAttach() {
        updateFlingBehavior(flingBehavior)
    }

    override fun onDensityChange() {
        onCancelPointerInput()
        if (isAttached) updateDensity()
    }

    private fun updateDensity() {
        val newDensity = requireDensity()
        if (density == null || density != newDensity) {
            density = newDensity
            updateFlingBehavior(flingBehavior)
        }
    }

    private fun updateFlingBehavior(newFlingBehavior: FlingBehavior?) {
        // Fall back to default fling behavior if the new fling behavior is null
        this.resolvedFlingBehavior =
            newFlingBehavior
                ?: anchoredDraggableFlingBehavior(
                    snapAnimationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
                    positionalThreshold = AnchoredDraggableDefaults.PositionalThreshold,
                    density = requireDensity().also { density = it },
                    state = state,
                )
    }

    override suspend fun drag(forEachDelta: suspend ((dragDelta: DragDelta) -> Unit) -> Unit) {
        state.anchoredDrag {
            forEachDelta { dragDelta ->
                val oneDirectionalDelta = dragDelta.delta.reverseIfNeeded().toFloat()
                if (overscrollEffect == null) {
                    dragTo(state.newOffsetForDelta(oneDirectionalDelta))
                } else {
                    overscrollEffect!!.applyToScroll(
                        delta = oneDirectionalDelta.toOffset(),
                        source = NestedScrollSource.UserInput,
                    ) { deltaForDrag ->
                        val dragOffset = state.newOffsetForDelta(deltaForDrag.toFloat())
                        val consumedDelta = (dragOffset - state.requireOffset()).toOffset()
                        dragTo(dragOffset)
                        consumedDelta
                    }
                }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(event: DragEvent.DragStopped) {
        if (!isAttached) return
        coroutineScope.launch {
            val oneDirectionalVelocity = event.velocity.reverseIfNeeded().toFloat()
            if (overscrollEffect == null) {
                fling(oneDirectionalVelocity)
            } else {
                overscrollEffect!!.applyToFling(velocity = oneDirectionalVelocity.toVelocity()) {
                    availableVelocity ->
                    val consumed = fling(availableVelocity.toFloat())
                    val currentOffset = state.requireOffset()
                    val minAnchor = state.anchors.minPosition()
                    val maxAnchor = state.anchors.maxPosition()
                    // return consumed velocity only if we are reaching the min/max anchors
                    if (currentOffset >= maxAnchor || currentOffset <= minAnchor) {
                        consumed.toVelocity()
                    } else {
                        availableVelocity
                    }
                }
            }
        }
    }

    private suspend fun fling(velocity: Float): Float =
        if (state.usePreModifierChangeBehavior) {
            @Suppress("DEPRECATION") state.settle(velocity)
        } else {
            var leftoverVelocity = velocity
            state.anchoredDrag {
                val scrollScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {
                            val newOffset = state.newOffsetForDelta(pixels)
                            val consumed = newOffset - state.offset
                            dragTo(newOffset)
                            return consumed
                        }
                    }
                with(resolvedFlingBehavior) {
                    leftoverVelocity = scrollScope.performFling(velocity)
                }
            }
            leftoverVelocity
        }

    override fun startDragImmediately(): Boolean = startDragImmediately ?: state.isAnimationRunning

    fun update(
        state: AnchoredDraggableState<T>,
        orientation: Orientation,
        enabled: Boolean,
        reverseDirection: Boolean?,
        interactionSource: MutableInteractionSource?,
        overscrollEffect: OverscrollEffect?,
        startDragImmediately: Boolean?,
        flingBehavior: FlingBehavior?,
    ) {
        this.flingBehavior = flingBehavior

        var resetPointerInputHandling = false
        if (this.state != state) {
            this.state = state
            updateFlingBehavior(flingBehavior)
            resetPointerInputHandling = true
        }
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }

        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }

        this.startDragImmediately = startDragImmediately
        this.overscrollEffect = overscrollEffect

        update(
            enabled = enabled,
            interactionSource = interactionSource,
            shouldResetPointerInputHandling = resetPointerInputHandling,
            orientationLock = orientation,
        )
    }

    private fun Float.toOffset() =
        Offset(
            x = if (orientation == Orientation.Horizontal) this else 0f,
            y = if (orientation == Orientation.Vertical) this else 0f,
        )

    private fun Float.toVelocity() =
        Velocity(
            x = if (orientation == Orientation.Horizontal) this else 0f,
            y = if (orientation == Orientation.Vertical) this else 0f,
        )

    private fun Velocity.toFloat() = if (orientation == Orientation.Vertical) this.y else this.x

    private fun Offset.toFloat() = if (orientation == Orientation.Vertical) this.y else this.x

    private fun Velocity.reverseIfNeeded() = if (isReverseDirection) this * -1f else this * 1f

    private fun Offset.reverseIfNeeded() = if (isReverseDirection) this * -1f else this * 1f
}

private val AlwaysDrag: (PointerInputChange) -> Boolean = { true }

/**
 * Structure that represents the anchors of a [AnchoredDraggableState].
 *
 * See the DraggableAnchors factory method to construct drag anchors using a default implementation.
 * This structure does not make any guarantees about ordering of the anchors.
 */
interface DraggableAnchors<T> {

    /** The number of anchors */
    val size: Int

    /**
     * Get the anchor position for an associated [anchor]
     *
     * @param anchor The value to look up
     * @return The position of the anchor, or [Float.NaN] if the anchor does not exist
     */
    fun positionOf(anchor: T): Float

    /**
     * Whether there is an anchor position associated with the [anchor]
     *
     * @param anchor The value to look up
     * @return true if there is an anchor for this value, false if there is no anchor for this value
     */
    fun hasPositionFor(anchor: T): Boolean

    /**
     * Find the closest anchor value to the [position].
     *
     * @param position The position to start searching from
     * @return The closest anchor or null if the anchors are empty
     */
    fun closestAnchor(position: Float): T?

    /**
     * Find the closest anchor value to the [position], in the specified direction.
     *
     * @param position The position to start searching from
     * @param searchUpwards Whether to search upwards from the current position or downwards
     * @return The closest anchor or null if the anchors are empty
     */
    fun closestAnchor(position: Float, searchUpwards: Boolean): T?

    /** The smallest anchor position, or [Float.NaN] if the anchors are empty. */
    fun minPosition(): Float

    /** The biggest anchor position, or [Float.NaN] if the anchors are empty. */
    fun maxPosition(): Float

    /** Get the anchor key at the specified index, or null if the index is out of bounds. */
    fun anchorAt(index: Int): T?

    /**
     * Get the anchor position at the specified index, or [Float.NaN] if the index is out of bounds.
     */
    fun positionAt(index: Int): Float
}

/**
 * Iterate over all the anchors.
 *
 * @param block The action to invoke with the key and position
 */
inline fun <T> DraggableAnchors<T>.forEach(block: (key: T, position: Float) -> Unit) {
    for (i in 0 until size) {
        val key =
            requireNotNull(anchorAt(i)) { "There was no key at index $i. Please report a bug." }
        block(key, positionAt(i))
    }
}

/**
 * [DraggableAnchorsConfig] stores a mutable configuration anchors, comprised of values of [T] and
 * corresponding [Float] positions. This [DraggableAnchorsConfig] is used to construct an immutable
 * [DraggableAnchors] instance later on.
 */
class DraggableAnchorsConfig<T> {

    internal val keys = mutableListOf<T>()
    internal var positions = FloatArray(size = 5) { Float.NaN }

    /**
     * Set the anchor position for [this] anchor.
     *
     * @param position The anchor position.
     */
    @Suppress("BuilderSetStyle")
    infix fun T.at(position: Float) {
        keys.add(this)
        if (positions.size < keys.size) {
            expandPositions()
        }
        positions[keys.size - 1] = position
    }

    internal fun buildPositions(): FloatArray {
        // We might have expanded more than we actually need, so trim the array
        return positions.copyOfRange(
            fromIndex = 0,
            // toIndex is exclusive, so we need to take the entire keys.size, not just - 1
            toIndex = keys.size,
        )
    }

    internal fun buildKeys(): List<T> = keys

    private fun expandPositions() {
        positions = positions.copyOf(keys.size + 2)
    }
}

/**
 * Create a new [DraggableAnchors] instance using a builder function.
 *
 * @param builder A function with a [DraggableAnchorsConfig] that offers APIs to configure anchors
 * @return A new [DraggableAnchors] instance with the anchor positions set by the `builder`
 *   function.
 */
fun <T : Any> DraggableAnchors(builder: DraggableAnchorsConfig<T>.() -> Unit): DraggableAnchors<T> {
    val config = DraggableAnchorsConfig<T>().apply(builder)
    return DefaultDraggableAnchors(keys = config.buildKeys(), anchors = config.buildPositions())
}

/**
 * Scope used for suspending anchored drag blocks. Allows to set [AnchoredDraggableState.offset] to
 * a new value.
 *
 * @see [AnchoredDraggableState.anchoredDrag] to learn how to start the anchored drag and get the
 *   access to this scope.
 */
interface AnchoredDragScope {
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
 * anchors are defined in composition, or update the anchors using
 * [AnchoredDraggableState.updateAnchors].
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
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Deprecated(ConfigurationMovedToModifier, level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION") // confirmValueChange is deprecated
fun <T> AnchoredDraggableState(
    initialValue: T,
    positionalThreshold: (totalDistance: Float) -> Float,
    velocityThreshold: () -> Float,
    snapAnimationSpec: AnimationSpec<Float>,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    confirmValueChange: (newValue: T) -> Boolean = { true },
): AnchoredDraggableState<T> =
    AnchoredDraggableState(initialValue, confirmValueChange).apply {
        this.positionalThreshold = positionalThreshold
        this.velocityThreshold = velocityThreshold
        @Suppress("DEPRECATION")
        this.snapAnimationSpec = snapAnimationSpec
        @Suppress("DEPRECATION")
        this.decayAnimationSpec = decayAnimationSpec
    }

/**
 * Construct an [AnchoredDraggableState] instance with anchors.
 *
 * @param initialValue The initial value of the state.
 * @param anchors The anchors of the state. Use [AnchoredDraggableState.updateAnchors] to update the
 *   anchors later.
 * @param snapAnimationSpec The default animation spec that will be used to animate to a new state.
 * @param decayAnimationSpec The animation spec that will be used when flinging with a large enough
 *   velocity to reach or cross the target state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param positionalThreshold The positional threshold, in px, to be used when calculating the
 *   target state while a drag is in progress and when settling after the drag ends. This is the
 *   distance from the start of a transition. It will be, depending on the direction of the
 *   interaction, added or subtracted from/to the origin offset. It should always be a positive
 *   value.
 * @param velocityThreshold The velocity threshold (in px per second) that the end velocity has to
 *   exceed in order to animate to the next state, even if the [positionalThreshold] has not been
 *   reached.
 */
@Deprecated(ConfigurationMovedToModifier, level = DeprecationLevel.WARNING)
@Suppress("DEPRECATION") // confirmValueChange is deprecated
fun <T> AnchoredDraggableState(
    initialValue: T,
    anchors: DraggableAnchors<T>,
    positionalThreshold: (totalDistance: Float) -> Float,
    velocityThreshold: () -> Float,
    snapAnimationSpec: AnimationSpec<Float>,
    decayAnimationSpec: DecayAnimationSpec<Float>,
    confirmValueChange: (newValue: T) -> Boolean = { true },
): AnchoredDraggableState<T> =
    AnchoredDraggableState(
            initialValue = initialValue,
            anchors = anchors,
            confirmValueChange = confirmValueChange,
        )
        .apply {
            this.positionalThreshold = positionalThreshold
            this.velocityThreshold = velocityThreshold
            @Suppress("DEPRECATION")
            this.snapAnimationSpec = snapAnimationSpec
            @Suppress("DEPRECATION")
            this.decayAnimationSpec = decayAnimationSpec
        }

/**
 * State of the [anchoredDraggable] modifier. Use the constructor overload with anchors if the
 * anchors are defined in composition, or update the anchors using [updateAnchors].
 *
 * This contains necessary information about any ongoing drag or animation and provides methods to
 * change the state either immediately or by starting an animation.
 *
 * @param initialValue The initial value of the state.
 */
@Stable
class AnchoredDraggableState<T>(initialValue: T) {
    /**
     * Construct an [AnchoredDraggableState] instance with anchors.
     *
     * @param initialValue The initial value of the state.
     * @param anchors The anchors of the state. Use [updateAnchors] to update the anchors later.
     */
    constructor(initialValue: T, anchors: DraggableAnchors<T>) : this(initialValue) {
        this.anchors = anchors
        trySnapTo(initialValue)
    }

    /**
     * Construct an [AnchoredDraggableState] instance with anchors.
     *
     * @param initialValue The initial value of the state.
     * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
     *   change.
     * @sample androidx.compose.foundation.samples.AnchoredDraggableDynamicAnchorsSample For an
     *   example of using dynamic anchors to replace confirmValueChange.
     */
    @Deprecated(ConfirmValueChangeDeprecated, level = DeprecationLevel.WARNING)
    constructor(
        initialValue: T,
        confirmValueChange: (newValue: T) -> Boolean,
    ) : this(initialValue) {
        this.confirmValueChange = confirmValueChange
    }

    /**
     * Construct an [AnchoredDraggableState] instance with anchors.
     *
     * @param initialValue The initial value of the state.
     * @param anchors The anchors of the state. Use [updateAnchors] to update the anchors later.
     * @param confirmValueChange Optional callback invoked to confirm or veto a pending state
     *   change.
     * @sample androidx.compose.foundation.samples.AnchoredDraggableDynamicAnchorsSample For an
     *   example of using dynamic anchors to replace confirmValueChange.
     */
    @Deprecated(ConfirmValueChangeDeprecated, level = DeprecationLevel.WARNING)
    @Suppress("DEPRECATION")
    constructor(
        initialValue: T,
        anchors: DraggableAnchors<T>,
        confirmValueChange: (newValue: T) -> Boolean = { true },
    ) : this(initialValue, confirmValueChange) {
        this.anchors = anchors
        trySnapTo(initialValue)
    }

    internal var confirmValueChange: (newValue: T) -> Boolean = { true }
    internal lateinit var positionalThreshold: (totalDistance: Float) -> Float
    internal lateinit var velocityThreshold: () -> Float
    @Deprecated(ConfigurationMovedToModifier, level = DeprecationLevel.WARNING)
    lateinit var snapAnimationSpec: AnimationSpec<Float>
        internal set

    @Deprecated(ConfigurationMovedToModifier, level = DeprecationLevel.WARNING)
    lateinit var decayAnimationSpec: DecayAnimationSpec<Float>
        internal set

    @Suppress("DEPRECATION")
    internal val usePreModifierChangeBehavior: Boolean
        get() =
            ::positionalThreshold.isInitialized &&
                ::velocityThreshold.isInitialized &&
                ::snapAnimationSpec.isInitialized &&
                ::decayAnimationSpec.isInitialized

    private val dragMutex = MutatorMutex()

    /**
     * The current value of the [AnchoredDraggableState].
     *
     * That is the closest anchor point that the state has passed through.
     */
    var currentValue: T by mutableStateOf(initialValue)
        private set

    /**
     * The value the [AnchoredDraggableState] is currently settled at.
     *
     * When progressing through multiple anchors, e.g. `A -> B -> C`, [settledValue] will stay the
     * same until settled at an anchor, while [currentValue] will update to the closest anchor.
     */
    var settledValue: T by mutableStateOf(initialValue)
        private set

    /**
     * The target value. This is the closest value to the current offset. If no interactions like
     * animations or drags are in progress, this will be the current value.
     */
    val targetValue: T by derivedStateOf {
        dragTarget
            ?: run {
                val currentOffset = offset
                if (!currentOffset.isNaN()) {
                    anchors.closestAnchor(offset) ?: currentValue
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
    @get:FrequentlyChangingValue
    var offset: Float by mutableFloatStateOf(Float.NaN)
        private set

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     * @see offset
     */
    @FrequentlyChangingValue
    fun requireOffset(): Float {
        checkPrecondition(!offset.isNaN()) {
            "The offset was read before being initialized. Did you access the offset in a phase " +
                "before layout, like effects or composition?"
        }
        return offset
    }

    /** Whether an animation is currently in progress. */
    val isAnimationRunning: Boolean
        get() = dragTarget != null

    /**
     * The fraction of the offset between [from] and [to], as a fraction between [0f..1f], or 1f if
     * [from] is equal to [to].
     *
     * @param from The starting value used to calculate the distance
     * @param to The end value used to calculate the distance
     */
    @FrequentlyChangingValue
    @FloatRange(from = 0.0, to = 1.0)
    fun progress(from: T, to: T): Float {
        val fromOffset = anchors.positionOf(from)
        val toOffset = anchors.positionOf(to)
        val currentOffset =
            offset.coerceIn(
                min(fromOffset, toOffset), // fromOffset might be > toOffset
                max(fromOffset, toOffset),
            )
        val fraction = (currentOffset - fromOffset) / (toOffset - fromOffset)
        return if (!fraction.isNaN()) {
            // If we are very close to 0f or 1f, we round to the closest
            if (fraction < 1e-6f) 0f else if (fraction > 1 - 1e-6f) 1f else abs(fraction)
        } else 1f
    }

    /**
     * The fraction of the progress going from [settledValue] to [targetValue], within [0f..1f]
     * bounds, or 1f if the [AnchoredDraggableState] is in a settled state.
     */
    @Deprecated(
        message =
            "Use the progress function to query the progress between two specified " + "anchors.",
        replaceWith = ReplaceWith("progress(state.settledValue, state.targetValue)"),
    )
    @get:FrequentlyChangingValue
    @get:FloatRange(from = 0.0, to = 1.0)
    val progress: Float by
        derivedStateOf(structuralEqualityPolicy()) {
            val a = anchors.positionOf(settledValue)
            val b = anchors.positionOf(targetValue)
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
            } else targetValue,
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
     * Find the closest anchor and settle at it with the given [animationSpec].
     *
     * @param animationSpec The animation spec that will be used to animate to the closest anchor.
     */
    suspend fun settle(animationSpec: AnimationSpec<Float>) {
        val previousValue = this.currentValue
        val targetValue = anchors.closestAnchor(requireOffset())
        if (targetValue != null && confirmValueChange(targetValue)) {
            animateTo(targetValue, animationSpec)
        } else {
            // If the user vetoed the state change, rollback to the previous state.
            animateTo(previousValue, animationSpec)
        }
    }

    /**
     * Find the closest anchor, taking into account the velocityThreshold and positionalThreshold,
     * and settle at it with an animation.
     *
     * If the [velocity] is lower than the velocityThreshold, the closest anchor by distance and
     * positionalThreshold will be the target. If the [velocity] is higher than the
     * velocityThreshold, the positionalThreshold will <b>not</b> be considered and the next anchor
     * in the direction indicated by the sign of the [velocity] will be the target.
     *
     * Based on the [velocity], either snapAnimationSpec or decayAnimationSpec will be used to
     * animate towards the target.
     *
     * @return The velocity consumed in the animation
     */
    @Deprecated(SettleWithVelocityDeprecated, level = DeprecationLevel.WARNING)
    suspend fun settle(velocity: Float): Float {
        requirePrecondition(usePreModifierChangeBehavior) {
            "AnchoredDraggableState was configured through " +
                "a constructor without providing positional and velocity threshold. This " +
                "overload of settle has been deprecated. Please refer to " +
                "AnchoredDraggableState#settle(animationSpec) for more information."
        }
        val previousValue = this.currentValue
        val targetValue =
            anchors.computeTarget(
                currentOffset = requireOffset(),
                velocity = velocity,
                positionalThreshold,
                velocityThreshold,
            )
        return if (confirmValueChange(targetValue)) {
            animateToWithDecay(targetValue, velocity)
        } else {
            // If the user vetoed the state change, rollback to the previous state.
            animateToWithDecay(previousValue, velocity)
        }
    }

    private val anchoredDragScope =
        object : AnchoredDragScope {
            var leftBound: T? = null
            var rightBound: T? = null
            var distance = Float.NaN

            override fun dragTo(newOffset: Float, lastKnownVelocity: Float) {
                val previousOffset = offset
                offset = newOffset
                lastVelocity = lastKnownVelocity
                if (previousOffset.isNaN()) return
                val isMovingForward = newOffset >= previousOffset
                updateIfNeeded(isMovingForward)
            }

            fun updateIfNeeded(isMovingForward: Boolean) {
                updateBounds(isMovingForward)
                val distanceToCurrentAnchor = abs(offset - anchors.positionOf(currentValue))
                val crossedThreshold = distanceToCurrentAnchor >= distance / 2f
                if (crossedThreshold) {
                    val closestAnchor =
                        (if (isMovingForward) rightBound else leftBound) ?: currentValue
                    if (confirmValueChange(closestAnchor)) {
                        currentValue = closestAnchor
                    }
                }
            }

            fun updateBounds(isMovingForward: Boolean) {
                val currentAnchorPosition = anchors.positionOf(currentValue)
                if (offset == currentAnchorPosition) {
                    val searchStartPosition = offset + (if (isMovingForward) 1f else -1f)
                    val closestExcludingCurrent =
                        anchors.closestAnchor(searchStartPosition, isMovingForward) ?: currentValue
                    if (isMovingForward) {
                        leftBound = currentValue
                        rightBound = closestExcludingCurrent
                    } else {
                        leftBound = closestExcludingCurrent
                        rightBound = currentValue
                    }
                } else {
                    val closestLeft = anchors.closestAnchor(offset, false) ?: currentValue
                    val closestRight = anchors.closestAnchor(offset, true) ?: currentValue
                    leftBound = closestLeft
                    rightBound = closestRight
                }
                distance = abs(anchors.positionOf(leftBound!!) - anchors.positionOf(rightBound!!))
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
        block: suspend AnchoredDragScope.(anchors: DraggableAnchors<T>) -> Unit,
    ) {
        dragMutex.mutate(dragPriority) {
            restartable(inputs = { anchors }) { latestAnchors ->
                anchoredDragScope.block(latestAnchors)
            }
            val closest = anchors.closestAnchor(offset)
            if (closest != null) {
                val closestAnchorOffset = anchors.positionOf(closest)
                val isAtClosestAnchor = abs(offset - closestAnchorOffset) < 0.5f
                if (isAtClosestAnchor && confirmValueChange.invoke(closest)) {
                    settledValue = closest
                    currentValue = closest
                }
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
        block: suspend AnchoredDragScope.(anchor: DraggableAnchors<T>, targetValue: T) -> Unit,
    ) {
        if (anchors.hasPositionFor(targetValue)) {
            try {
                dragMutex.mutate(dragPriority) {
                    dragTarget = targetValue
                    restartable(inputs = { anchors to this@AnchoredDraggableState.targetValue }) {
                        (anchors, latestTarget) ->
                        anchoredDragScope.block(anchors, latestTarget)
                    }
                    if (confirmValueChange(targetValue)) {
                        val latestTargetOffset = anchors.positionOf(targetValue)
                        anchoredDragScope.dragTo(latestTargetOffset, lastVelocity)
                        settledValue = targetValue
                        currentValue = targetValue
                    }
                }
            } finally {
                dragTarget = null
            }
        } else {
            if (confirmValueChange(targetValue)) {
                settledValue = targetValue
                currentValue = targetValue
            }
        }
    }

    /**
     * Calculate the new offset for a [delta] to ensure it is coerced in the bounds
     *
     * @param delta The delta to be added to the [offset]
     * @return The coerced offset
     */
    internal fun newOffsetForDelta(delta: Float) =
        ((if (offset.isNaN()) 0f else offset) + delta).coerceIn(
            anchors.minPosition(),
            anchors.maxPosition(),
        )

    /**
     * Drag by the [delta], coerce it in the bounds and dispatch it to the [AnchoredDraggableState].
     *
     * @return The delta the consumed by the [AnchoredDraggableState]
     */
    fun dispatchRawDelta(delta: Float): Float {
        val newOffset = newOffsetForDelta(delta)
        val consumedDelta = (newOffset - requireOffset())
        anchoredDragScope.dragTo(newOffset)
        return consumedDelta
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
                settledValue = targetValue
            }
        }

    companion object {
        /** The default [Saver] implementation for [AnchoredDraggableState]. */
        fun <T : Any> Saver() =
            Saver<AnchoredDraggableState<T>, T>(
                save = { it.currentValue },
                restore = { AnchoredDraggableState(initialValue = it) },
            )

        /** The default [Saver] implementation for [AnchoredDraggableState]. */
        @Deprecated(ConfirmValueChangeDeprecated, level = DeprecationLevel.WARNING)
        @Suppress("DEPRECATION")
        fun <T : Any> Saver(confirmValueChange: (T) -> Boolean = { true }) =
            Saver<AnchoredDraggableState<T>, T>(
                save = { it.currentValue },
                restore = {
                    AnchoredDraggableState(
                        initialValue = it,
                        confirmValueChange = confirmValueChange,
                    )
                },
            )

        /** The default [Saver] implementation for [AnchoredDraggableState]. */
        @Deprecated(ConfigurationMovedToModifier, level = DeprecationLevel.WARNING)
        @Suppress("DEPRECATION")
        fun <T : Any> Saver(
            snapAnimationSpec: AnimationSpec<Float>,
            decayAnimationSpec: DecayAnimationSpec<Float>,
            positionalThreshold: (distance: Float) -> Float,
            velocityThreshold: () -> Float,
            confirmValueChange: (T) -> Boolean = { true },
        ): Saver<AnchoredDraggableState<T>, T> =
            Saver(
                save = { it.currentValue },
                restore = {
                    AnchoredDraggableState(
                        initialValue = it,
                        confirmValueChange = confirmValueChange,
                        positionalThreshold = positionalThreshold,
                        velocityThreshold = velocityThreshold,
                        snapAnimationSpec = snapAnimationSpec,
                        decayAnimationSpec = decayAnimationSpec,
                    )
                },
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
suspend fun <T> AnchoredDraggableState<T>.snapTo(targetValue: T) {
    anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
        val targetOffset = anchors.positionOf(latestTarget)
        if (!targetOffset.isNaN()) dragTo(targetOffset)
    }
}

private suspend fun <T> AnchoredDraggableState<T>.animateTo(
    velocity: Float,
    anchoredDragScope: AnchoredDragScope,
    anchors: DraggableAnchors<T>,
    latestTarget: T,
    snapAnimationSpec: AnimationSpec<Float>,
) {
    with(anchoredDragScope) {
        val targetOffset = anchors.positionOf(latestTarget)
        var prev = if (offset.isNaN()) 0f else offset
        if (!targetOffset.isNaN() && prev != targetOffset) {
            debugLog { "Target animation is used" }
            animate(prev, targetOffset, velocity, snapAnimationSpec) { value, velocity ->
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

/**
 * Animate to a [targetValue]. If the [targetValue] is not in the set of anchors, the
 * [AnchoredDraggableState.currentValue] will be updated to the [targetValue] without updating the
 * offset.
 *
 * @param targetValue The target value of the animation
 * @param animationSpec The animation spec used to perform the animation
 * @throws CancellationException if the interaction interrupted by another interaction like a
 *   gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
 */
suspend fun <T> AnchoredDraggableState<T>.animateTo(
    targetValue: T,
    animationSpec: AnimationSpec<Float> =
        if (usePreModifierChangeBehavior) {
            @Suppress("DEPRECATION") this.snapAnimationSpec
        } else AnchoredDraggableDefaults.SnapAnimationSpec,
) {
    anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
        animateTo(lastVelocity, this, anchors, latestTarget, animationSpec)
    }
}

/**
 * Attempt to animate using decay Animation to a [targetValue]. If the [velocity] is high enough to
 * get to the target offset, we'll use [decayAnimationSpec] to get to that offset and return the
 * consumed velocity. If the [velocity] is not high enough, we'll use [snapAnimationSpec] to reach
 * the target offset.
 *
 * If the [targetValue] is not in the set of anchors, [AnchoredDraggableState.currentValue] will be
 * updated ro the [targetValue] without updating the offset.
 *
 * @param targetValue The target value of the animation
 * @param velocity The velocity the animation should start with, in px/s
 * @param snapAnimationSpec The animation spec used if the velocity is not high enough to perform a
 *   decay to the [targetValue] using the [decayAnimationSpec]
 * @param decayAnimationSpec The animation spec used if the velocity is high enough to perform a
 *   decay to the [targetValue]
 * @return The velocity consumed in the animation
 * @throws CancellationException if the interaction interrupted bt another interaction like a
 *   gesture interaction or another programmatic interaction like [animateTo] or [snapTo] call.
 */
suspend fun <T> AnchoredDraggableState<T>.animateToWithDecay(
    targetValue: T,
    velocity: Float,
    snapAnimationSpec: AnimationSpec<Float> =
        if (usePreModifierChangeBehavior) {
            @Suppress("DEPRECATION") this.snapAnimationSpec
        } else AnchoredDraggableDefaults.SnapAnimationSpec,
    decayAnimationSpec: DecayAnimationSpec<Float> =
        if (usePreModifierChangeBehavior) {
            @Suppress("DEPRECATION") this.decayAnimationSpec
        } else AnchoredDraggableDefaults.DecayAnimationSpec,
): Float {
    var remainingVelocity = velocity
    anchoredDrag(targetValue = targetValue) { anchors, latestTarget ->
        val targetOffset = anchors.positionOf(latestTarget)
        if (!targetOffset.isNaN()) {
            var prev = if (offset.isNaN()) 0f else offset
            if (prev != targetOffset) {
                // If targetOffset is not in the same direction as the direction of the drag (sign
                // of the velocity) we fall back to using target animation.
                // If the component is at the target offset already, we use decay animation that
                // will
                // not consume any velocity.
                if (velocity * (targetOffset - prev) < 0f || velocity == 0f) {
                    animateTo(velocity, this, anchors, latestTarget, snapAnimationSpec)
                    remainingVelocity = 0f
                } else {
                    val projectedDecayOffset =
                        decayAnimationSpec.calculateTargetValue(prev, velocity)
                    debugLog {
                        "offset = $prev\tvelocity = $velocity\t" +
                            "targetOffset = $targetOffset\tprojectedOffset = $projectedDecayOffset"
                    }

                    val canDecayToTarget =
                        if (velocity > 0) {
                            projectedDecayOffset >= targetOffset
                        } else {
                            projectedDecayOffset <= targetOffset
                        }
                    if (canDecayToTarget) {
                        debugLog { "Decay animation is used" }
                        AnimationState(prev, velocity).animateDecay(decayAnimationSpec) {
                            // This covers a few different cases:
                            // 1) currentOffset < targetOffset
                            //    a) prev < targetOffset -> continue animation
                            //    b) prev > targetOffset -> cancel as target reached
                            // 2) currentOffset > targetOffset
                            //    a) prev > targetOffset -> continue animation
                            //    b) prev < targetOffset -> cancel as target reached
                            if (
                                (value < targetOffset && prev > targetOffset) ||
                                    (value > targetOffset && prev < targetOffset)
                            ) {
                                val finalValue = value.coerceToTarget(targetOffset)
                                dragTo(finalValue, this.velocity)
                                remainingVelocity = if (this.velocity.isNaN()) 0f else this.velocity
                                prev = finalValue
                                cancelAnimation()
                            } else {
                                dragTo(value, this.velocity)
                                remainingVelocity = this.velocity
                                prev = value
                            }
                        }
                    } else {
                        animateTo(velocity, this, anchors, latestTarget, snapAnimationSpec)
                        remainingVelocity = 0f
                    }
                }
            }
        }
    }
    return velocity - remainingVelocity
}

/**
 * Compute the target anchor based on the [currentOffset], [velocity] and [positionalThreshold] and
 * [velocityThreshold].
 *
 * @return The suggested target anchor
 */
private fun <T> DraggableAnchors<T>.computeTarget(
    currentOffset: Float,
    velocity: Float,
    positionalThreshold: (totalDistance: Float) -> Float,
    velocityThreshold: () -> Float,
): T {
    val currentAnchors = this
    require(!currentOffset.isNaN()) { "The offset provided to computeTarget must not be NaN." }
    val isMoving = abs(velocity) > 0.0f
    val isMovingForward = isMoving && velocity > 0f
    // When we're not moving, pick the closest anchor and don't consider directionality
    return if (!isMoving) {
        currentAnchors.closestAnchor(currentOffset)!!
    } else if (abs(velocity) >= abs(velocityThreshold())) {
        currentAnchors.closestAnchor(currentOffset, searchUpwards = isMovingForward)!!
    } else {
        val left = currentAnchors.closestAnchor(currentOffset, false)!!
        val leftAnchorPosition = currentAnchors.positionOf(left)
        val right = currentAnchors.closestAnchor(currentOffset, true)!!
        val rightAnchorPosition = currentAnchors.positionOf(right)
        val distance = abs(leftAnchorPosition - rightAnchorPosition)
        val relativeThreshold = abs(positionalThreshold(distance))
        val closestAnchorFromStart =
            if (isMovingForward) leftAnchorPosition else rightAnchorPosition
        val relativePosition = abs(closestAnchorFromStart - currentOffset)
        when (relativePosition >= relativeThreshold) {
            true -> if (isMovingForward) right else left
            false -> if (isMovingForward) left else right
        }
    }
}

/**
 * Contains useful defaults for use with [AnchoredDraggableState] and [Modifier.anchoredDraggable]
 */
object AnchoredDraggableDefaults {

    /** The default spec for snapping, a tween spec */
    val SnapAnimationSpec: AnimationSpec<Float> = tween()

    /** The default positional threshold, 50% of the distance */
    val PositionalThreshold: (Float) -> Float = { distance -> distance / 2f }

    /** The default spec for decaying, an exponential decay */
    val DecayAnimationSpec: DecayAnimationSpec<Float> = exponentialDecay()

    /**
     * Create and remember a [TargetedFlingBehavior] for use with [Modifier.anchoredDraggable] that
     * will find the target based on the velocity and [positionalThreshold] when performing a fling,
     * and settle to that target.
     *
     * There are two paths: a) If the velocity is bigger than [AnchoredDraggableMinFlingVelocity]
     * (125 dp/s), the fling behavior will move to the next closest anchor in the fling direction,
     * determined by the sign of the fling velocity. b) If the velocity is smaller than
     * [AnchoredDraggableMinFlingVelocity] (125 dp/s), the fling behavior will consider the
     * positional thresholds, by default [AnchoredDraggableDefaults.PositionalThreshold]. If the
     * offset has crossed the threshold when performing the fling, the fling behavior will move to
     * the next anchor in the fling direction. Otherwise, it will move back to the previous anchor.
     *
     * @param state The state the fling will be performed on
     * @param positionalThreshold The positional threshold, in px, to be used when calculating the
     *   target state while a drag is in progress and when settling after the drag ends. This is the
     *   distance from the start of a transition. It will be, depending on the direction of the
     *   interaction, added or subtracted from/to the origin offset. It should always be a positive
     *   value.
     * @param animationSpec The animation spec used to perform the settling
     */
    @Composable
    fun <T> flingBehavior(
        state: AnchoredDraggableState<T>,
        positionalThreshold: (totalDistance: Float) -> Float = PositionalThreshold,
        animationSpec: AnimationSpec<Float> = SnapAnimationSpec,
    ): TargetedFlingBehavior {
        val density = LocalDensity.current
        return remember(density, state, positionalThreshold, animationSpec) {
            anchoredDraggableFlingBehavior(
                state = state,
                density = density,
                positionalThreshold = positionalThreshold,
                snapAnimationSpec = animationSpec,
            )
        }
    }
}

private fun Float.coerceToTarget(target: Float): Float {
    if (target == 0f) return 0f
    return if (target > 0) coerceAtMost(target) else coerceAtLeast(target)
}

internal class AnchoredDragFinishedSignal :
    PlatformOptimizedCancellationException("Anchored drag finished")

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

private fun <T> emptyDraggableAnchors() = DefaultDraggableAnchors<T>(emptyList(), FloatArray(0))

private val GetOrNan: (Int) -> Float = { Float.NaN }

private class DefaultDraggableAnchors<T>(
    private val keys: List<T>,
    private val anchors: FloatArray,
) : DraggableAnchors<T> {

    init {
        requirePrecondition(keys.size == anchors.size) {
            "DraggableAnchors were constructed with " +
                "inconsistent key-value sizes. Keys: $keys | Anchors: ${anchors.toList()}"
        }
    }

    override fun positionOf(anchor: T): Float {
        val index = keys.indexOf(anchor)
        return anchors.getOrElse(index, GetOrNan)
    }

    override fun hasPositionFor(anchor: T) = keys.indexOf(anchor) != -1

    override fun closestAnchor(position: Float): T? {
        var minAnchorIndex = -1
        var minDistance = Float.POSITIVE_INFINITY
        anchors.forEachIndexed { index, anchorPosition ->
            val distance = abs(position - anchorPosition)
            if (distance <= minDistance) {
                minAnchorIndex = index
                minDistance = distance
            }
        }
        if (minAnchorIndex == -1) return null
        return keys[minAnchorIndex]
    }

    override fun closestAnchor(position: Float, searchUpwards: Boolean): T? {
        var minAnchorIndex = -1
        var minDistance = Float.POSITIVE_INFINITY
        anchors.forEachIndexed { index, anchorPosition ->
            val delta = if (searchUpwards) anchorPosition - position else position - anchorPosition
            val distance = if (delta < 0) Float.POSITIVE_INFINITY else delta
            if (distance <= minDistance) {
                minAnchorIndex = index
                minDistance = distance
            }
        }
        if (minAnchorIndex == -1) return null
        return keys[minAnchorIndex]
    }

    override fun minPosition() = anchors.minOrNaN()

    override fun maxPosition() = anchors.maxOrNaN()

    override val size = anchors.size

    override fun anchorAt(index: Int) = keys.getOrNull(index)

    override fun positionAt(index: Int) = anchors.getOrElse(index, GetOrNan)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is DefaultDraggableAnchors<*>) return false

        if (keys != other.keys) return false
        if (!anchors.contentEquals(other.anchors)) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keys.hashCode()
        result = 31 * result + anchors.contentHashCode()
        result = 31 * result + size
        return result
    }

    override fun toString() = buildString {
        append("DraggableAnchors(anchors={")
        for (i in 0 until size) {
            append("${anchorAt(i)}=${positionAt(i)}")
            if (i < size - 1) {
                append(", ")
            }
        }
        append("})")
    }

    // Kotlin stdlib's FloatArray#min/max implementations throw an exception when the array is empty
    //  This would add more overhead than needed for us, so we use our own.
    private fun FloatArray.minOrNaN(): Float {
        if (isEmpty()) return Float.NaN
        var min = this[0]
        for (i in 1..lastIndex) {
            val e = this[i]
            min = minOf(min, e)
        }
        return min
    }

    // Kotlin stdlib's FloatArray#min/max implementations throw an exception when the array is empty
    //  This would add more overhead than needed for us, so we use our own.
    private fun FloatArray.maxOrNaN(): Float {
        if (isEmpty()) return Float.NaN
        var min = this[0]
        for (i in 1..lastIndex) {
            val e = this[i]
            min = maxOf(min, e)
        }
        return min
    }
}

internal val AnchoredDraggableMinFlingVelocity = 125.dp

private const val ConfigurationMovedToModifier =
    "This constructor of " +
        "AnchoredDraggableState has been deprecated. Please pass thresholds and animation specs" +
        " to AnchoredDraggableDefaults.flingBehavior(..) instead, which can be passed to" +
        " Modifier.anchoredDraggable."
private const val SettleWithVelocityDeprecated =
    "settle does not accept a velocity anymore." +
        " Please use FlingBehavior#performFling instead. See AnchoredDraggableSample.kt for" +
        " example usages."
private const val StartDragImmediatelyDeprecated =
    "startDragImmediately has been removed " +
        "without replacement. Modifier.anchoredDraggable sets startDragImmediately to true by " +
        "default when animations are running."
private const val ConfirmValueChangeDeprecated =
    "confirmValueChange is deprecated without replacement. Rather than relying on a callback to " +
        "veto state changes, the anchor set should not include disallowed anchors. See " +
        "androidx.compose.foundation.samples.AnchoredDraggableDynamicAnchorsSample for an " +
        "example of using dynamic anchors over confirmValueChange."

/**
 * Construct a [FlingBehavior] for use with [Modifier.anchoredDraggable].
 *
 * @param state The [AnchoredDraggableState] that will be used for the fling animation
 * @param positionalThreshold A positional threshold that needs to be crossed in order to reach the
 *   next anchor when flinging, in pixels. This can be a derived from the distance that the lambda
 *   is invoked with.
 * @param snapAnimationSpec The animation spec that will be used to snap to a new state.
 */
internal fun <T> anchoredDraggableFlingBehavior(
    state: AnchoredDraggableState<T>,
    density: Density,
    positionalThreshold: (totalDistance: Float) -> Float,
    snapAnimationSpec: AnimationSpec<Float>,
): TargetedFlingBehavior =
    snapFlingBehavior(
        decayAnimationSpec = NoOpDecayAnimationSpec,
        snapAnimationSpec = snapAnimationSpec,
        snapLayoutInfoProvider =
            AnchoredDraggableLayoutInfoProvider(
                state = state,
                positionalThreshold = positionalThreshold,
                velocityThreshold = { with(density) { 125.dp.toPx() } },
            ),
    )

private fun <T> AnchoredDraggableLayoutInfoProvider(
    state: AnchoredDraggableState<T>,
    positionalThreshold: (totalDistance: Float) -> Float,
    velocityThreshold: () -> Float,
): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {

        // We never decay in AnchoredDraggable's fling
        override fun calculateApproachOffset(velocity: Float, decayOffset: Float) = 0f

        override fun calculateSnapOffset(velocity: Float): Float {
            val currentOffset = state.requireOffset()
            val proposedTargetValue =
                state.anchors.computeTarget(
                    currentOffset = currentOffset,
                    velocity = velocity,
                    positionalThreshold = positionalThreshold,
                    velocityThreshold = velocityThreshold,
                )
            val targetValue =
                if (state.confirmValueChange(proposedTargetValue)) {
                    proposedTargetValue
                } else {
                    state.settledValue
                }
            return state.anchors.positionOf(targetValue) - currentOffset
        }
    }

private val NoOpDecayAnimationSpec: DecayAnimationSpec<Float> =
    object : FloatDecayAnimationSpec {
            override val absVelocityThreshold = 0f

            override fun getValueFromNanos(
                playTimeNanos: Long,
                initialValue: Float,
                initialVelocity: Float,
            ) = 0f

            override fun getDurationNanos(initialValue: Float, initialVelocity: Float) = 0L

            override fun getVelocityFromNanos(
                playTimeNanos: Long,
                initialValue: Float,
                initialVelocity: Float,
            ) = 0f

            override fun getTargetValue(initialValue: Float, initialVelocity: Float) = 0f
        }
        .generateDecayAnimationSpec()

private const val DEBUG = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("AnchoredDraggable: ${generateMsg()}")
    }
}
