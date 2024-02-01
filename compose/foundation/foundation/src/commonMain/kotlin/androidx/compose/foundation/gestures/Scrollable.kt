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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.FocusedBoundsObserverNode
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.BringIntoViewSpec.Companion.DefaultBringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewResponderNode
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusProperties
import androidx.compose.ui.focus.FocusPropertiesModifierNode
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Drag
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Fling
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.Wheel
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 */
@Stable
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null
): Modifier = scrollable(
    state = state,
    orientation = orientation,
    enabled = enabled,
    reverseDirection = reverseDirection,
    flingBehavior = flingBehavior,
    interactionSource = interactionSource,
    overscrollEffect = null
)

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the
 * over scrolling logic. Consider using [ScrollableDefaults.overscrollEffect] for the platform
 * look-and-feel.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have
 * some scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should
 * also apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 * `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * drag events when this scrollable is being dragged.
 * @param bringIntoViewSpec The configuration that this scrollable should use to perform
 * scrolling when scroll requests are received from the focus system.
 *
 * Note: This API is experimental as it brings support for some experimental features:
 * [overscrollEffect] and [bringIntoViewScroller].
 */
@Stable
@ExperimentalFoundationApi
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec = ScrollableDefaults.bringIntoViewSpec()
) = this then ScrollableElement(
    state,
    orientation,
    overscrollEffect,
    enabled,
    reverseDirection,
    flingBehavior,
    interactionSource,
    bringIntoViewSpec
)

@OptIn(ExperimentalFoundationApi::class)
private class ScrollableElement(
    val state: ScrollableState,
    val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec
) : ModifierNodeElement<ScrollableNode>() {
    override fun create(): ScrollableNode {
        return ScrollableNode(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec
        )
    }

    override fun update(node: ScrollableNode) {
        node.update(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec
        )
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + overscrollEffect.hashCode()
        result = 31 * result + enabled.hashCode()
        result = 31 * result + reverseDirection.hashCode()
        result = 31 * result + flingBehavior.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + bringIntoViewSpec.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is ScrollableElement) return false

        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (overscrollEffect != other.overscrollEffect) return false
        if (enabled != other.enabled) return false
        if (reverseDirection != other.reverseDirection) return false
        if (flingBehavior != other.flingBehavior) return false
        if (interactionSource != other.interactionSource) return false
        if (bringIntoViewSpec != other.bringIntoViewSpec) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "scrollable"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["overscrollEffect"] = overscrollEffect
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["flingBehavior"] = flingBehavior
        properties["interactionSource"] = interactionSource
        properties["scrollableBringIntoViewConfig"] = bringIntoViewSpec
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class ScrollableNode(
    private var state: ScrollableState,
    private var orientation: Orientation,
    private var overscrollEffect: OverscrollEffect?,
    private var enabled: Boolean,
    private var reverseDirection: Boolean,
    private var flingBehavior: FlingBehavior?,
    private var interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec
) : DelegatingNode(), ObserverModifierNode, CompositionLocalConsumerModifierNode,
    FocusPropertiesModifierNode, KeyInputModifierNode {

    val nestedScrollDispatcher = NestedScrollDispatcher()

    // Place holder fling behavior, we'll initialize it when the density is available.
    val defaultFlingBehavior = DefaultFlingBehavior(splineBasedDecay(UnityDensity))

    val scrollingLogic = ScrollingLogic(
        scrollableState = state,
        orientation = orientation,
        overscrollEffect = overscrollEffect,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior ?: defaultFlingBehavior,
        nestedScrollDispatcher = nestedScrollDispatcher,
    )

    val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollingLogic)

    val contentInViewNode =
        delegate(
            ContentInViewNode(
                orientation,
                state,
                reverseDirection,
                bringIntoViewSpec
            )
        )
    val scrollableContainer = delegate(ModifierLocalScrollableContainerProvider(enabled))

    init {
        /**
         * Nested scrolling
         */
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))

        /**
         * Focus scrolling
         */
        delegate(FocusTargetModifierNode())
        delegate(BringIntoViewResponderNode(contentInViewNode))
        delegate(FocusedBoundsObserverNode { contentInViewNode.onFocusBoundsChanged(it) })
    }

    /**
     * Pointer gesture handling
     */
    val scrollableGesturesNode = delegate(
        ScrollableGesturesNode(
            interactionSource = interactionSource,
            orientation = orientation,
            enabled = enabled,
            nestedScrollDispatcher = nestedScrollDispatcher,
            scrollLogic = scrollingLogic
        )
    )

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec
    ) {

        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            scrollableContainer.enabled = enabled
        }
        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior

        scrollingLogic.update(
            scrollableState = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = resolvedFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher
        )

        scrollableGesturesNode.update(
            interactionSource = interactionSource,
            orientation = orientation,
            enabled = enabled
        )

        contentInViewNode.update(
            orientation,
            state,
            reverseDirection,
            bringIntoViewSpec
        )

        this.state = state
        this.orientation = orientation
        this.overscrollEffect = overscrollEffect
        this.enabled = enabled
        this.reverseDirection = reverseDirection
        this.flingBehavior = flingBehavior
        this.interactionSource = interactionSource
    }

    @Suppress("SuspiciousCompositionLocalModifierRead")
    override fun onAttach() {
        updateDefaultFlingBehavior()
        observeReads { currentValueOf(LocalDensity) } // monitor change in Density
    }

    override fun onObservedReadsChanged() {
        // if density changes, update the default fling behavior.
        updateDefaultFlingBehavior()
    }

    private fun updateDefaultFlingBehavior() {
        val density = currentValueOf(LocalDensity)
        defaultFlingBehavior.flingDecay = splineBasedDecay(density)
    }

    override fun applyFocusProperties(focusProperties: FocusProperties) {
        focusProperties.canFocus = false
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return if (enabled &&
            (event.key == Key.PageDown || event.key == Key.PageUp) &&
            (event.type == KeyEventType.KeyDown) &&
            (!event.isCtrlPressed)
            ) {
            with(scrollingLogic) {
                val scrollAmount: Offset = if (orientation == Orientation.Vertical) {
                    val viewportHeight = contentInViewNode.viewportSize.height

                    val yAmount = if (event.key == Key.PageUp) {
                        viewportHeight.toFloat()
                    } else {
                        -viewportHeight.toFloat()
                    }

                    Offset(0f, yAmount)
                } else {
                    val viewportWidth = contentInViewNode.viewportSize.width

                    val xAmount = if (event.key == Key.PageUp) {
                        viewportWidth.toFloat()
                    } else {
                        -viewportWidth.toFloat()
                    }

                    Offset(xAmount, 0f)
                }

                // A coroutine is launched for every individual scroll event in the
                // larger scroll gesture. If we see degradation in the future (that is,
                // a fast scroll gesture on a slow device causes UI jank [not seen up to
                // this point), we can switch to a more efficient solution where we
                // lazily launch one coroutine (with the first event) and use a Channel
                // to communicate the scroll amount to the UI thread.
                coroutineScope.launch {
                    scrollableState.scroll(MutatePriority.UserInput) {
                        dispatchScroll(scrollAmount, Wheel)
                    }
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false
}

/**
 * The configuration of how a scrollable reacts to bring into view requests.
 *
 * Note: API shape and naming are still being refined, therefore API is marked as experimental.
 */
@ExperimentalFoundationApi
@Stable
interface BringIntoViewSpec {

    /**
     * A retargetable Animation Spec to be used as the animation to run to fulfill the
     * BringIntoView requests.
     */
    val scrollAnimationSpec: AnimationSpec<Float> get() = DefaultScrollAnimationSpec

    /**
     * Calculate the offset needed to bring one of the scrollable container's child into view.
     *
     * @param offset from the side closest to the origin (For the x-axis this is 'left',
     * for the y-axis this is 'top').
     * @param size is the child size.
     * @param containerSize Is the main axis size of the scrollable container.
     *
     * All distances above are represented in pixels.
     *
     * @return The necessary amount to scroll to satisfy the bring into view request.
     * Returning zero from here means that the request was satisfied and the scrolling animation
     * should stop.
     *
     * This will be called for every frame of the scrolling animation. This means that, as the
     * animation progresses, the offset will naturally change to fulfill the scroll request.
     */
    fun calculateScrollDistance(
        offset: Float,
        size: Float,
        containerSize: Float
    ): Float

    companion object {

        /**
         * The default animation spec used by [Modifier.scrollable] to run Bring Into View requests.
         */
        val DefaultScrollAnimationSpec: AnimationSpec<Float> = spring()

        internal val DefaultBringIntoViewSpec = object : BringIntoViewSpec {

            override val scrollAnimationSpec: AnimationSpec<Float> = DefaultScrollAnimationSpec

            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                val trailingEdge = offset + size
                val leadingEdge = offset
                return when {

                    // If the item is already visible, no need to scroll.
                    leadingEdge >= 0 && trailingEdge <= containerSize -> 0f

                    // If the item is visible but larger than the parent, we don't scroll.
                    leadingEdge < 0 && trailingEdge > containerSize -> 0f

                    // Find the minimum scroll needed to make one of the edges coincide with the parent's
                    // edge.
                    abs(leadingEdge) < abs(trailingEdge - containerSize) -> leadingEdge
                    else -> trailingEdge - containerSize
                }
            }
        }
    }
}

/**
 * Contains the default values used by [scrollable]
 */
object ScrollableDefaults {

    /**
     * Create and remember default [FlingBehavior] that will represent natural fling curve.
     */
    @Composable
    fun flingBehavior(): FlingBehavior {
        val flingSpec = rememberSplineBasedDecay<Float>()
        return remember(flingSpec) {
            DefaultFlingBehavior(flingSpec)
        }
    }

    /**
     * Create and remember default [OverscrollEffect] that will be used for showing over scroll
     * effects.
     */
    @Composable
    @ExperimentalFoundationApi
    fun overscrollEffect(): OverscrollEffect {
        return rememberOverscrollEffect()
    }

    /**
     * Used to determine the value of `reverseDirection` parameter of [Modifier.scrollable]
     * in scrollable layouts.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     *
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Orientation.Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }

    /**
     * A default implementation for [BringIntoViewSpec] that brings a child into view
     * using the least amount of effort.
     */
    @ExperimentalFoundationApi
    fun bringIntoViewSpec(): BringIntoViewSpec = DefaultBringIntoViewSpec
}

internal interface ScrollConfig {
    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal expect fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig

/**
 * A node that detects and processes all scrollable gestures.
 */
private class ScrollableGesturesNode(
    val scrollLogic: ScrollingLogic,
    val orientation: Orientation,
    val enabled: Boolean,
    val nestedScrollDispatcher: NestedScrollDispatcher,
    val interactionSource: MutableInteractionSource?
) : DelegatingNode() {
    init { delegate(MouseWheelScrollNode(scrollLogic)) }

    val draggableState = ScrollDraggableState(scrollLogic)
    private val startDragImmediately = { scrollLogic.shouldScrollImmediately() }
    private val onDragStopped: suspend CoroutineScope.(velocity: Float) -> Unit = { velocity ->
        nestedScrollDispatcher.coroutineScope.launch {
            scrollLogic.onDragStopped(velocity)
        }
    }

    val draggableGesturesNode = delegate(
        DraggableNode(
            draggableState,
            orientation = orientation,
            enabled = enabled,
            interactionSource = interactionSource,
            reverseDirection = false,
            startDragImmediately = startDragImmediately,
            onDragStopped = onDragStopped,
            canDrag = CanDragCalculation,
            onDragStarted = NoOpOnDragStarted
        )
    )

    fun update(
        orientation: Orientation,
        enabled: Boolean,
        interactionSource: MutableInteractionSource?,
    ) {

        // update draggable node
        draggableGesturesNode.update(
            draggableState,
            orientation = orientation,
            enabled = enabled,
            interactionSource = interactionSource,
            reverseDirection = false,
            startDragImmediately = startDragImmediately,
            onDragStarted = NoOpOnDragStarted,
            onDragStopped = onDragStopped,
            canDrag = CanDragCalculation
        )
    }
}

private val CanDragCalculation: (PointerInputChange) -> Boolean =
    { down -> down.type != PointerType.Mouse }

private val NoOpOnDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {}

private class MouseWheelScrollNode(
    private val scrollingLogic: ScrollingLogic
) : DelegatingNode(), CompositionLocalConsumerModifierNode {
    // Need to wait until onAttach to read the scroll config. Currently this is static, so we
    // don't need to worry about observation / updating this over time.
    var scrollConfig: ScrollConfig? = null

    override fun onAttach() {
        scrollConfig = platformScrollConfig()
    }

    init {
        delegate(SuspendingPointerInputModifierNode {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitScrollEvent()
                    if (event.changes.fastAll { !it.isConsumed }) {
                        with(scrollConfig!!) {
                            val scrollAmount = calculateMouseWheelScroll(event, size)

                            with(scrollingLogic) {
                                // A coroutine is launched for every individual scroll event in the
                                // larger scroll gesture. If we see degradation in the future (that is,
                                // a fast scroll gesture on a slow device causes UI jank [not seen up to
                                // this point), we can switch to a more efficient solution where we
                                // lazily launch one coroutine (with the first event) and use a Channel
                                // to communicate the scroll amount to the UI thread.
                                coroutineScope.launch {
                                    scrollableState.scroll(MutatePriority.UserInput) {
                                        dispatchScroll(scrollAmount, Wheel)
                                    }
                                }
                                event.changes.fastForEach { it.consume() }
                            }
                        }
                    }
                }
            }
        })
    }
}

private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (event.type != PointerEventType.Scroll)
    return event
}

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
@OptIn(ExperimentalFoundationApi::class)
private class ScrollingLogic(
    var scrollableState: ScrollableState,
    private var orientation: Orientation,
    private var overscrollEffect: OverscrollEffect?,
    private var reverseDirection: Boolean,
    private var flingBehavior: FlingBehavior,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
) {
    private val isNestedFlinging = mutableStateOf(false)
    fun Float.toOffset(): Offset = when {
        this == 0f -> Offset.Zero
        orientation == Horizontal -> Offset(this, 0f)
        else -> Offset(0f, this)
    }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y

    fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

    fun Float.toVelocity() = Velocity(
        x = if (orientation == Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f,
    )

    /**
     * @return the amount of scroll that was consumed
     */
    fun ScrollScope.dispatchScroll(
        initialAvailableDelta: Offset,
        source: NestedScrollSource
    ): Offset {
        val performScroll: (Offset) -> Offset = { delta ->
            val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

            val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

            val singleAxisDeltaForSelfScroll =
                scrollAvailableAfterPreScroll.singleAxisOffset().reverseIfNeeded().toFloat()

            // Consume on a single axis
            val consumedBySelfScroll =
                scrollBy(singleAxisDeltaForSelfScroll).toOffset().reverseIfNeeded()

            val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
            val consumedByPostScroll = nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source
            )
            consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
        }

        val overscroll = overscrollEffect

        return if (source == Wheel) {
            performScroll(initialAvailableDelta)
        } else if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToScroll(initialAvailableDelta, source, performScroll)
        } else {
            performScroll(initialAvailableDelta)
        }
    }

    private val shouldDispatchOverscroll
        get() = scrollableState.canScrollForward || scrollableState.canScrollBackward

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }

    suspend fun onDragStopped(initialVelocity: Float) {
        // Self started flinging, set
        registerNestedFling(true)

        val availableVelocity = initialVelocity.toVelocity()

        val performFling: suspend (Velocity) -> Velocity = { velocity ->
            val preConsumedByParent = nestedScrollDispatcher
                .dispatchPreFling(velocity)
            val available = velocity - preConsumedByParent

            val velocityLeft = doFlingAnimation(available)

            val consumedPost =
                nestedScrollDispatcher.dispatchPostFling(
                    (available - velocityLeft),
                    velocityLeft
                )
            val totalLeft = velocityLeft - consumedPost
            velocity - totalLeft
        }

        val overscroll = overscrollEffect
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }

        // Self stopped flinging, reset
        registerNestedFling(false)
    }

    suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        scrollableState.scroll {
            val outerScopeScroll: (Offset) -> Offset = { delta ->
                dispatchScroll(delta.reverseIfNeeded(), Fling).reverseIfNeeded()
            }
            val scope = object : ScrollScope {
                override fun scrollBy(pixels: Float): Float {
                    return outerScopeScroll.invoke(pixels.toOffset()).toFloat()
                }
            }

            with(scope) {
                with(flingBehavior) {
                    result = result.update(
                        performFling(available.toFloat().reverseIfNeeded()).reverseIfNeeded()
                    )
                }
            }
        }
        return result
    }

    fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress || isNestedFlinging.value ||
            overscrollEffect?.isInProgress ?: false
    }

    fun registerNestedFling(isFlinging: Boolean) {
        isNestedFlinging.value = isFlinging
    }

    fun update(
        scrollableState: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ) {
        this.scrollableState = scrollableState
        this.orientation = orientation
        this.overscrollEffect = overscrollEffect
        this.reverseDirection = reverseDirection
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
    }
}

private class ScrollDraggableState(
    var scrollLogic: ScrollingLogic
) : DraggableState, DragScope {
    var latestScrollScope: ScrollScope = NoOpScrollScope

    override fun dragBy(pixels: Float) {
        with(scrollLogic) {
            with(latestScrollScope) {
                dispatchScroll(pixels.toOffset(), Drag)
            }
        }
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
        scrollLogic.scrollableState.scroll(dragPriority) {
            latestScrollScope = this
            block()
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        with(scrollLogic) { performRawScroll(delta.toOffset()) }
    }
}

private val NoOpScrollScope: ScrollScope = object : ScrollScope {
    override fun scrollBy(pixels: Float): Float = pixels
}

private class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollingLogic,
    var enabled: Boolean
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // child will fling, set
        if (source == Fling) {
            scrollingLogic.registerNestedFling(true)
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (enabled) {
        scrollingLogic.performRawScroll(available)
    } else {
        Offset.Zero
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity
    ): Velocity {
        return if (enabled) {
            val velocityLeft = scrollingLogic.doFlingAnimation(available)
            available - velocityLeft
        } else {
            Velocity.Zero
        }.also {
            // Flinging child finished flinging, reset
            scrollingLogic.registerNestedFling(false)
        }
    }
}

internal class DefaultFlingBehavior(
    var flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale
) : FlingBehavior {

    // For Testing
    var lastAnimationCycleCount = 0

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        lastAnimationCycleCount = 0
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                val animationState = AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                )
                try {
                    animationState.animateDecay(flingDecay) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity
                        // avoid rounding errors and stop if anything is unconsumed
                        if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                        lastAnimationCycleCount++
                    }
                } catch (exception: CancellationException) {
                    velocityLeft = animationState.velocity
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }
}

// TODO: b/203141462 - make this public and move it to ui
/**
 * Whether this modifier is inside a scrollable container, provided by [Modifier.scrollable].
 * Defaults to false.
 */
internal val ModifierLocalScrollableContainer = modifierLocalOf { false }

internal val NoOpFlingBehavior = object : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float = 0f
}

private const val DefaultScrollMotionDurationScaleFactor = 1f
internal val DefaultScrollMotionDurationScale = object : MotionDurationScale {
    override val scaleFactor: Float
        get() = DefaultScrollMotionDurationScaleFactor
}

private class ModifierLocalScrollableContainerProvider(var enabled: Boolean) :
    ModifierLocalModifierNode,
    Modifier.Node() {
    private val modifierLocalMap =
        modifierLocalMapOf(entry = ModifierLocalScrollableContainer to true)
    override val providedValues: ModifierLocalMap
        get() = if (enabled) {
            modifierLocalMap
        } else {
            modifierLocalMapOf()
        }
}

private val UnityDensity = object : Density {
    override val density: Float
        get() = 1f
    override val fontScale: Float
        get() = 1f
}
