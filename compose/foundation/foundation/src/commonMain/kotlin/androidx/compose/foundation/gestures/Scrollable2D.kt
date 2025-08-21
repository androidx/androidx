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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

/**
 * Configure touch scrolling and flinging for the UI element in both XY orientations.
 *
 * Users should update their state themselves using default [Scrollable2DState] and its
 * `consumeScrollDelta` callback or by implementing [Scrollable2DState] interface manually and
 * reflect their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable2D]. If you're only interested in a single direction scroll,
 * consider using [scrollable].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the over
 * scrolling logic. Use [androidx.compose.foundation.rememberOverscrollEffect] to create an instance
 * of the current provided overscroll implementation.
 *
 * @sample androidx.compose.foundation.samples.Scrollable2DSample
 * @param state [Scrollable2DState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param enabled whether or not scrolling is enabled
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have some
 *   scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should also
 *   apply [androidx.compose.foundation.overscroll] modifier.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit drag events when
 *   this scrollable is being dragged.
 */
@Stable
fun Modifier.scrollable2D(
    state: Scrollable2DState,
    enabled: Boolean = true,
    overscrollEffect: OverscrollEffect? = null,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
) =
    this then
        Scrollable2DElement(state, overscrollEffect, enabled, flingBehavior, interactionSource)

private class Scrollable2DElement(
    val state: Scrollable2DState,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
) : ModifierNodeElement<Scrollable2DNode>() {
    override fun create(): Scrollable2DNode {
        return Scrollable2DNode(state, overscrollEffect, flingBehavior, enabled, interactionSource)
    }

    override fun update(node: Scrollable2DNode) {
        node.update(state, overscrollEffect, enabled, flingBehavior, interactionSource)
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is Scrollable2DElement) return false

        if (state != other.state) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable2D"
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
    }
}

internal class Scrollable2DNode(
    state: Scrollable2DState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
) :
    DragGestureNode(
        canDrag = CanDragCalculation,
        enabled = enabled,
        interactionSource = interactionSource,
        orientationLock = null,
    ),
    SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private val nestedScrollDispatcher = NestedScrollDispatcher()

    private val scrollableContainerNode = delegate(ScrollableContainerNode(enabled))

    // Place holder fling behavior, we'll initialize it when the density is available.
    private val defaultFlingBehavior = DefaultFlingBehavior(splineBasedDecay(UnityDensity))

    private val scrollingLogic =
        ScrollingLogic2D(
            scrollableState = state,
            overscrollEffect = overscrollEffect,
            flingBehavior = flingBehavior ?: defaultFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher,
            isScrollableNodeAttached = { isAttached },
        )

    private val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    init {
        /** Nested scrolling */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit
    ) {
        with(scrollingLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta { scrollByWithOverscroll(it.delta, source = UserInput) }
            }
        }
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun onDragStopped(event: DragEvent.DragStopped) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollingLogic.onScrollStopped(event.velocity)
        }
    }

    override fun startDragImmediately(): Boolean {
        return scrollingLogic.shouldScrollImmediately()
    }

    fun update(
        state: Scrollable2DState,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
    ) {
        var shouldInvalidateSemantics = false
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainerNode.update(enabled)
            shouldInvalidateSemantics = true
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        val resetPointerInputHandling =
            scrollingLogic.update(
                scrollableState = state,
                overscrollEffect = overscrollEffect,
                flingBehavior = resolvedFlingBehavior,
                nestedScrollDispatcher = nestedScrollDispatcher,
            )
        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )

        if (shouldInvalidateSemantics) {
            clearScrollSemanticsActions()
            invalidateSemantics()
        }
    }

    override fun onAttach() {
        updateDefaultFlingBehavior()
    }

    private fun updateDefaultFlingBehavior() {
        if (!isAttached) return
        val density = requireDensity()
        defaultFlingBehavior.updateDensity(density)
    }

    override fun onDensityChange() {
        onCancelPointerInput()
        updateDefaultFlingBehavior()
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pointerEvent.changes.fastAny { canDrag.invoke(it) }) {
            super.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let { scrollBy(action = it) }

        scrollByOffsetAction?.let { scrollByOffset(action = it) }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch { scrollingLogic.semanticsScrollBy(Offset(x, y)) }
            true
        }

        scrollByOffsetAction = { offset -> scrollingLogic.semanticsScrollBy(offset) }
    }

    private fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }
}

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
private class ScrollingLogic2D(
    var scrollableState: Scrollable2DState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
    private val isScrollableNodeAttached: () -> Boolean,
) : ScrollLogic {
    // specifies if this scrollable node is currently flinging
    override var isFlinging = false
        private set

    private var latestScrollSource = UserInput
    private var outerStateScope = NoOpScrollScope

    private val nestedScrollScope =
        object : NestedScrollScope {
            override fun scrollBy(offset: Offset, source: NestedScrollSource): Offset {
                return with(outerStateScope) { performScroll(offset, source) }
            }

            override fun scrollByWithOverscroll(
                offset: Offset,
                source: NestedScrollSource,
            ): Offset {
                latestScrollSource = source
                val overscroll = overscrollEffect
                return if (overscroll != null && shouldDispatchOverscroll(offset)) {
                    overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
                } else {
                    with(outerStateScope) { performScroll(offset, source) }
                }
            }
        }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) { performScroll(delta, latestScrollSource) }
    }

    private fun Scroll2DScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val consumedBySelfScroll = scrollBy(scrollAvailableAfterPreScroll)

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll =
            nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source,
            )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    fun shouldDispatchOverscroll(offset: Offset) = scrollableState.canScroll(offset)

    fun shouldDispatchOverscroll(velocity: Velocity) =
        scrollableState.canScroll(Offset(velocity.x, velocity.y))

    override fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    private fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollableState.dispatchRawDelta(scroll)
    }

    suspend fun onScrollStopped(initialVelocity: Velocity) {
        val availableVelocity = initialVelocity

        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher.dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling((available - velocityLeft), velocityLeft)
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll(initialVelocity)) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available

        /** Converts an XY offset to its magnitude float value. */
        fun Offset.toMagnitudeFloat() = sqrt(x.pow(2) + y.pow(2))

        /**
         * Decomposes a pixel offset to its XY components based on initial velocity angle and
         * magnitude
         */
        fun Float.toDecomposedOffset() =
            if (available.angle.isNaN()) {
                Offset(0f, this)
            } else {
                Offset(
                    x = abs(cos(available.angle) * this) * sign(available.x),
                    y = abs(sin(available.angle) * this) * sign(available.y),
                )
            }

        /**
         * Decomposes a pixel velocity to its XY components based on initial velocity angle and
         * magnitude
         */
        fun Float.toDecomposedVelocity() =
            if (available.angle.isNaN()) {
                Velocity(0f, this)
            } else {
                Velocity(
                    x = abs(cos(available.angle) * this) * sign(available.x),
                    y = abs(sin(available.angle) * this) * sign(available.y),
                )
            }

        isFlinging = true
        try {
            scroll(scrollPriority = MutatePriority.Default) {
                val nestedScrollScope = this
                val flingScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {
                            val pixelsOffset = pixels.toDecomposedOffset()

                            if (pixelsOffset != Offset.Zero && !isScrollableNodeAttached.invoke()) {
                                throw FlingCancellationException()
                            }

                            val consumedOffset =
                                nestedScrollScope.scrollByWithOverscroll(
                                    offset = pixelsOffset,
                                    source = SideEffect,
                                )

                            return consumedOffset.toMagnitudeFloat()
                        }
                    }
                with(flingScope) {
                    with(flingBehavior) {
                        val resultVelocity = performFling(available.magnitude)
                        result = resultVelocity.toDecomposedVelocity()
                    }
                }
            }
        } finally {
            isFlinging = false
        }

        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress || overscrollEffect?.isInProgress ?: false
    }

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /** @return true if the pointer input should be reset */
    fun update(
        scrollableState: Scrollable2DState,
        overscrollEffect: OverscrollEffect?,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }
}

private val NoOpScrollScope: Scroll2DScope =
    object : Scroll2DScope {
        override fun scrollBy(delta: Offset): Offset = delta
    }

private suspend fun ScrollingLogic2D.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = Offset.Zero
    scroll(scrollPriority = MutatePriority.Default) {
        animate(Offset.VectorConverter, Offset.Zero, offset) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed = scrollBy(offset = delta, source = UserInput)
            previousValue += consumed
        }
    }
    return previousValue
}

private val Velocity.magnitude
    get() = sqrt(x.pow(2) + y.pow(2))
private val Velocity.angle
    get() = atan2(x = x, y = y)
