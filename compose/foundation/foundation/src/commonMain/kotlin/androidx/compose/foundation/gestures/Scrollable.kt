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

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ComposeFoundationFlags.isClearNestedScrollCoroutineScopeFixEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewResponderNode
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberPlatformOverscrollEffect
import androidx.compose.foundation.scrollableArea
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.getFocusedRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.UserInput
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.dispatchOnScrollChanged
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * `scrollable` is a low level modifier that handles low level scrolling input gestures, without
 * other behaviors commonly used for scrollable containers. For building scrollable containers, see
 * [androidx.compose.foundation.scrollableArea]. `scrollableArea` clips its content to its bounds,
 * renders overscroll, and adjusts the direction of scroll gestures to ensure that the content moves
 * with the user's gestures. See also [androidx.compose.foundation.verticalScroll] and
 * [androidx.compose.foundation.horizontalScroll] for high level scrollable containers that handle
 * layout and move the content as the user scrolls.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit drag events when
 *   this scrollable is being dragged.
 */
@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier =
    scrollable(
        state = state,
        orientation = orientation,
        enabled = enabled,
        reverseDirection = reverseDirection,
        flingBehavior = flingBehavior,
        interactionSource = interactionSource,
        overscrollEffect = null,
    )

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * `scrollable` is a low level modifier that handles low level scrolling input gestures, without
 * other behaviors commonly used for scrollable containers. For building scrollable containers, see
 * [androidx.compose.foundation.scrollableArea]. `scrollableArea` clips its content to its bounds,
 * renders overscroll, and adjusts the direction of scroll gestures to ensure that the content moves
 * with the user's gestures. See also [androidx.compose.foundation.verticalScroll] and
 * [androidx.compose.foundation.horizontalScroll] for high level scrollable containers that handle
 * layout and move the content as the user scrolls.
 *
 * If you don't need to have fling or nested scroll support, but want to make component simply
 * draggable, consider using [draggable].
 *
 * This overload provides the access to [OverscrollEffect] that defines the behaviour of the over
 * scrolling logic. Use [androidx.compose.foundation.rememberOverscrollEffect] to create an instance
 * of the current provided overscroll implementation. Note: compared to other APIs that accept
 * [overscrollEffect] such as [scrollableArea] and [verticalScroll], `scrollable` does not render
 * the overscroll, it only provides events. Manually add [androidx.compose.foundation.overscroll] to
 * render the overscroll or use other APIs.
 *
 * @sample androidx.compose.foundation.samples.ScrollableSample
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 *   interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param overscrollEffect effect to which the deltas will be fed when the scrollable have some
 *   scrolling delta left. Pass `null` for no overscroll. If you pass an effect you should also
 *   apply [androidx.compose.foundation.overscroll] modifier.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will behave
 *   like bottom to top and left to right will behave like right to left.
 * @param flingBehavior logic describing fling behavior when drag has finished with velocity. If
 *   `null`, default from [ScrollableDefaults.flingBehavior] will be used.
 * @param interactionSource [MutableInteractionSource] that will be used to emit drag events when
 *   this scrollable is being dragged.
 * @param bringIntoViewSpec The configuration that this scrollable should use to perform scrolling
 *   when scroll requests are received from the focus system. If null is provided the system will
 *   use the behavior provided by [LocalBringIntoViewSpec] which by default has a platform dependent
 *   implementation.
 */
@Stable
fun Modifier.scrollable(
    state: ScrollableState,
    orientation: Orientation,
    overscrollEffect: OverscrollEffect?,
    enabled: Boolean = true,
    reverseDirection: Boolean = false,
    flingBehavior: FlingBehavior? = null,
    interactionSource: MutableInteractionSource? = null,
    bringIntoViewSpec: BringIntoViewSpec? = null,
) =
    this then
        ScrollableElement(
            state,
            orientation,
            overscrollEffect,
            enabled,
            reverseDirection,
            flingBehavior,
            interactionSource,
            bringIntoViewSpec,
        )

private class ScrollableElement(
    val state: ScrollableState,
    val orientation: Orientation,
    val overscrollEffect: OverscrollEffect?,
    val enabled: Boolean,
    val reverseDirection: Boolean,
    val flingBehavior: FlingBehavior?,
    val interactionSource: MutableInteractionSource?,
    val bringIntoViewSpec: BringIntoViewSpec?,
) : ModifierNodeElement<ScrollableNode>() {
    override fun create(): ScrollableNode {
        return ScrollableNode(
            state,
            overscrollEffect,
            flingBehavior,
            orientation,
            enabled,
            reverseDirection,
            interactionSource,
            bringIntoViewSpec,
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
            bringIntoViewSpec,
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
        properties["bringIntoViewSpec"] = bringIntoViewSpec
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class ScrollableNode(
    state: ScrollableState,
    overscrollEffect: OverscrollEffect?,
    flingBehavior: FlingBehavior?,
    orientation: Orientation,
    enabled: Boolean,
    reverseDirection: Boolean,
    interactionSource: MutableInteractionSource?,
    bringIntoViewSpec: BringIntoViewSpec?,
) :
    AbstractScrollableNode(
        overscrollEffect = overscrollEffect,
        flingBehavior = flingBehavior,
        enabled = enabled,
        interactionSource = interactionSource,
        orientation = orientation,
    ),
    KeyInputModifierNode,
    OnScrollChangedDispatcher {

    // Placeholder fling behavior, we'll initialize it when the density is available.
    override val defaultFlingBehavior = platformScrollableDefaultFlingBehavior()

    override val scrollLogic =
        ScrollingLogic(
            scrollableState = state,
            orientation = orientation,
            overscrollEffect = overscrollEffect,
            reverseDirection = reverseDirection,
            flingBehavior = flingBehavior ?: defaultFlingBehavior,
            nestedScrollDispatcher = nestedScrollDispatcher,
            onScrollChangedDispatcher = this,
            isScrollableNodeAttached = { isAttached },
        )

    override val nestedScrollConnection =
        ScrollableNestedScrollConnection(enabled = enabled, scrollingLogic = scrollLogic)

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))

    private val contentInViewNode =
        delegate(
            ContentInViewNode(
                orientation = orientation,
                scrollingLogic = scrollLogic,
                reverseDirection = reverseDirection,
                bringIntoViewSpec = bringIntoViewSpec,
                getFocusedRect = { focusTargetModifierNode.getFocusedRect() },
            )
        )

    override fun createMouseWheelScrollingLogic() =
        MouseWheel1DScrollingLogic(
            scrollingLogic = scrollLogic,
            scrollConfig = platformScrollConfig(),
            onScrollStopped = ::onMouseWheelScrollStopped,
            density = requireDensity(),
        )

    override fun createTrackpadScrollingLogic() =
        Trackpad1DScrollingLogic(
            scrollingLogic = scrollLogic,
            onScrollStopped = ::onTrackpadScrollStopped,
            density = requireDensity(),
        )

    init {
        // Must be called here because in AbstractScrollableNode.init nestedScrollConnection hasn't
        // been created yet
        initializeNestedScrollingDelegation()

        /** Focus scrolling */
        delegate(BringIntoViewResponderNode(contentInViewNode))
    }

    override fun dispatchScrollDeltaInfo(delta: Offset) {
        if (!isAttached) return
        dispatchOnScrollChanged(delta)
    }

    override suspend fun drag(
        forEachDelta: suspend ((dragDelta: DragEvent.DragDelta) -> Unit) -> Unit
    ) {
        with(scrollLogic) {
            scroll(scrollPriority = MutatePriority.UserInput) {
                forEachDelta {
                    // Indirect pointer Events should be reverted to account for the reverse we
                    // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
                    // that shouldn't happen for indirect pointer events, so we cancel the reverse
                    // here.
                    val invertIndirectPointer = if (it.isIndirectPointerEvent) -1f else 1f
                    scrollByWithOverscroll(
                        it.delta.singleAxisOffset() * invertIndirectPointer,
                        source = UserInput,
                    )
                }
            }
        }
    }

    override fun onDragStopped(event: DragEvent.DragStopped) {
        if (isClearNestedScrollCoroutineScopeFixEnabled && !isAttached) return
        nestedScrollDispatcher.coroutineScope.launch {
            // Indirect pointer Events should be reverted to account for the reverse we
            // do in Scrollable. Regular touchscreen events are inverted in scrollable, but
            // that shouldn't happen for indirect pointer events, so we cancel the reverse
            // here.
            val invertIndirectPointer = if (event.isIndirectPointerEvent) -1f else 1f
            scrollLogic.onScrollStopped(
                event.velocity * invertIndirectPointer,
                isMouseWheel = false,
            )
        }
    }

    private fun onMouseWheelScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollLogic.onScrollStopped(velocity, isMouseWheel = true)
        }
    }

    private fun onTrackpadScrollStopped(velocity: Velocity) {
        nestedScrollDispatcher.coroutineScope.launch {
            scrollLogic.onScrollStopped(velocity, isMouseWheel = false)
        }
    }

    fun update(
        state: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        enabled: Boolean,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior?,
        interactionSource: MutableInteractionSource?,
        bringIntoViewSpec: BringIntoViewSpec?,
    ) {
        update(
            enabled = enabled,
            overscrollEffect = overscrollEffect,
            flingBehavior = flingBehavior,
        )

        // a new fling behavior was set, change the resolved one.
        val resolvedFlingBehavior = flingBehavior ?: defaultFlingBehavior
        val resetPointerInputHandling =
            scrollLogic.update(
                scrollableState = state,
                orientation = orientation,
                overscrollEffect = overscrollEffect,
                reverseDirection = reverseDirection,
                flingBehavior = resolvedFlingBehavior,
                nestedScrollDispatcher = nestedScrollDispatcher,
            )
        contentInViewNode.update(orientation, reverseDirection, bringIntoViewSpec)

        // update DragGestureNode
        update(
            canDrag = CanDragCalculation,
            enabled = enabled,
            interactionSource = interactionSource,
            orientation = if (scrollLogic.isVertical()) Vertical else Horizontal,
            shouldResetPointerInputHandling = resetPointerInputHandling,
        )
    }

    // Key handler for Page up/down scrolling behavior.
    override fun onKeyEvent(event: KeyEvent): Boolean {
        return if (
            enabled &&
                (event.key == Key.PageDown || event.key == Key.PageUp) &&
                (event.type == KeyEventType.KeyDown) &&
                (!event.isCtrlPressed)
        ) {

            val scrollAmount: Offset =
                if (scrollLogic.isVertical()) {
                    val viewportHeight = contentInViewNode.viewportSizeOrZero.height

                    val yAmount =
                        if (event.key == Key.PageUp) {
                            viewportHeight.toFloat()
                        } else {
                            -viewportHeight.toFloat()
                        }

                    Offset(0f, yAmount)
                } else {
                    val viewportWidth = contentInViewNode.viewportSizeOrZero.width

                    val xAmount =
                        if (event.key == Key.PageUp) {
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
                scrollLogic.scroll(scrollPriority = MutatePriority.UserInput) {
                    scrollBy(offset = scrollAmount, source = UserInput)
                }
            }
            true
        } else {
            false
        }
    }

    override fun onPreKeyEvent(event: KeyEvent) = false

    override suspend fun semanticsScrollBy(offset: Offset): Offset {
        return scrollLogic.semanticsScrollBy(offset)
    }
}

/** Contains the default values used by [scrollable] */
object ScrollableDefaults {

    /** Create and remember default [FlingBehavior] that will represent natural fling curve. */
    @Composable fun flingBehavior(): FlingBehavior = rememberPlatformDefaultFlingBehavior()

    /**
     * Returns a remembered [OverscrollEffect] created from the current value of
     * [LocalOverscrollFactory].
     *
     * This API has been deprecated, and replaced with [rememberOverscrollEffect]
     */
    @Deprecated(
        "This API has been replaced with rememberOverscrollEffect, which queries theme provided OverscrollFactory values instead of the 'platform default' without customization.",
        replaceWith =
            ReplaceWith(
                "rememberOverscrollEffect()",
                "androidx.compose.foundation.rememberOverscrollEffect",
            ),
    )
    @Composable
    fun overscrollEffect(): OverscrollEffect {
        return rememberPlatformOverscrollEffect() ?: NoOpOverscrollEffect
    }

    private object NoOpOverscrollEffect : OverscrollEffect {
        override fun applyToScroll(
            delta: Offset,
            source: NestedScrollSource,
            performScroll: (Offset) -> Offset,
        ): Offset = performScroll(delta)

        override suspend fun applyToFling(
            velocity: Velocity,
            performFling: suspend (Velocity) -> Velocity,
        ) {
            performFling(velocity)
        }

        override val isInProgress: Boolean
            get() = false

        override val node: DelegatableNode
            get() = object : Modifier.Node() {}
    }

    /**
     * Calculates the final `reverseDirection` value for a scrollable component.
     *
     * This is a helper function used by [androidx.compose.foundation.scrollableArea] to determine
     * whether to reverse the direction of scroll input. The goal is to provide a "natural"
     * scrolling experience where content moves with the user's gesture, while also accounting for
     * the [layoutDirection].
     *
     * The logic is as follows:
     * 1. To achieve "natural" scrolling (content moves with the gesture), scroll deltas are
     *    inverted. This function returns `true` by default when `reverseScrolling` is `false`.
     * 2. In a Right-to-Left (`Rtl`) context with a `Horizontal` orientation, the direction is
     *    flipped an additional time to maintain the natural feel, as the content is laid out from
     *    right to left.
     *
     * @param layoutDirection current layout direction (e.g. from [LocalLayoutDirection])
     * @param orientation orientation of scroll
     * @param reverseScrolling whether scrolling direction should be reversed
     * @return `true` if scroll direction should be reversed, `false` otherwise.
     */
    fun reverseDirection(
        layoutDirection: LayoutDirection,
        orientation: Orientation,
        reverseScrolling: Boolean,
    ): Boolean {
        // A finger moves with the content, not with the viewport. Therefore,
        // always reverse once to have "natural" gesture that goes reversed to layout
        var reverseDirection = !reverseScrolling
        // But if rtl and horizontal, things move the other way around
        val isRtl = layoutDirection == LayoutDirection.Rtl
        if (isRtl && orientation != Vertical) {
            reverseDirection = !reverseDirection
        }
        return reverseDirection
    }
}

internal interface ScrollConfig {

    /** Enables animated transition of scroll on mouse wheel events. */
    val isSmoothScrollingEnabled: Boolean
        get() = true

    fun isPreciseWheelScroll(event: PointerEvent): Boolean = false

    fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset
}

internal expect fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig

/**
 * Holds all scrolling related logic: controls nested scrolling, flinging, overscroll and delta
 * dispatching.
 */
internal class ScrollingLogic(
    var scrollableState: ScrollableState,
    private var overscrollEffect: OverscrollEffect?,
    private var flingBehavior: FlingBehavior,
    var orientation: Orientation,
    var reverseDirection: Boolean,
    private var nestedScrollDispatcher: NestedScrollDispatcher,
    private var onScrollChangedDispatcher: OnScrollChangedDispatcher,
    private val isScrollableNodeAttached: () -> Boolean,
) : ScrollLogic {
    // specifies if this scrollable node is currently flinging
    override var isFlinging = false
        private set

    fun Float.toOffset(): Offset =
        when {
            this == 0f -> Offset.Zero
            orientation == Horizontal -> Offset(this, 0f)
            else -> Offset(0f, this)
        }

    fun Offset.singleAxisOffset(): Offset =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    fun Offset.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    private fun Velocity.toFloat(): Float = if (orientation == Horizontal) this.x else this.y

    private fun Velocity.singleAxisVelocity(): Velocity =
        if (orientation == Horizontal) copy(y = 0f) else copy(x = 0f)

    private fun Velocity.update(newValue: Float): Velocity =
        if (orientation == Horizontal) copy(x = newValue) else copy(y = newValue)

    fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun Offset.reverseIfNeeded(): Offset = if (reverseDirection) this * -1f else this

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
                return if (overscroll != null && shouldDispatchOverscroll) {
                    overscroll.applyToScroll(offset, latestScrollSource, performScrollForOverscroll)
                } else {
                    with(outerStateScope) { performScroll(offset, source) }
                }
            }
        }

    private val performScrollForOverscroll: (Offset) -> Offset = { delta ->
        with(outerStateScope) { performScroll(delta, latestScrollSource) }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun ScrollScope.performScroll(delta: Offset, source: NestedScrollSource): Offset {
        val consumedByPreScroll = nestedScrollDispatcher.dispatchPreScroll(delta, source)

        val scrollAvailableAfterPreScroll = delta - consumedByPreScroll

        val singleAxisDeltaForSelfScroll =
            scrollAvailableAfterPreScroll.singleAxisOffset().reverseIfNeeded().toFloat()

        // Consume on a single axis.
        val consumedBySelfScroll =
            scrollBy(singleAxisDeltaForSelfScroll).toOffset().reverseIfNeeded()

        // Trigger on scroll changed callback
        onScrollChangedDispatcher.dispatchScrollDeltaInfo(consumedBySelfScroll)

        val deltaAvailableAfterScroll = scrollAvailableAfterPreScroll - consumedBySelfScroll
        val consumedByPostScroll =
            nestedScrollDispatcher.dispatchPostScroll(
                consumedBySelfScroll,
                deltaAvailableAfterScroll,
                source,
            )
        return consumedByPreScroll + consumedBySelfScroll + consumedByPostScroll
    }

    private val shouldDispatchOverscroll
        get() =
            scrollableState.canScrollForward ||
                scrollableState.canScrollBackward ||
                overscrollEffect?.isInProgress == true

    override fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            dispatchRawDelta(scroll)
        }
    }

    private fun dispatchRawDelta(scroll: Offset): Offset {
        return scrollableState
            .dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
            .reverseIfNeeded()
            .toOffset()
    }

    suspend fun onScrollStopped(initialVelocity: Velocity, isMouseWheel: Boolean) {
        if (isMouseWheel && !flingBehavior.shouldBeTriggeredByMouseWheel) {
            return
        }
        val availableVelocity = initialVelocity.singleAxisVelocity()

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
        if (overscroll != null && shouldDispatchOverscroll) {
            overscroll.applyToFling(availableVelocity, performFling)
        } else {
            performFling(availableVelocity)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun doFlingAnimation(available: Velocity): Velocity {
        var result: Velocity = available
        isFlinging = true
        try {
            scroll(scrollPriority = MutatePriority.Default) {
                val nestedScrollScope = this
                val reverseScope =
                    object : ScrollScope {
                        override fun scrollBy(pixels: Float): Float {
                            // Fling has hit the bounds or node left composition,
                            // cancel it to allow continuation. This will conclude this node's
                            // fling,
                            // allowing the onPostFling signal to be called
                            // with the leftover velocity from the fling animation. Any nested
                            // scroll
                            // node above will be able to pick up the left over velocity and
                            // continue
                            // the fling.
                            if (
                                pixels.absoluteValue != 0.0f && !isScrollableNodeAttached.invoke()
                            ) {
                                throw FlingCancellationException()
                            }

                            return nestedScrollScope
                                .scrollByWithOverscroll(
                                    offset = pixels.toOffset().reverseIfNeeded(),
                                    source = SideEffect,
                                )
                                .toFloat()
                                .reverseIfNeeded()
                        }
                    }
                with(reverseScope) {
                    with(flingBehavior) {
                        result =
                            result.update(
                                performFling(available.toFloat().reverseIfNeeded())
                                    .reverseIfNeeded()
                            )
                    }
                }
            }
        } finally {
            isFlinging = false
        }

        return result
    }

    override fun shouldScrollImmediately(): Boolean {
        return scrollableState.isScrollInProgress || overscrollEffect?.isInProgress ?: false
    }

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend NestedScrollScope.() -> Unit,
    ) {
        scrollableState.scroll(scrollPriority) {
            outerStateScope = this
            block.invoke(nestedScrollScope)
        }
    }

    /** @return true if the pointer input should be reset */
    fun update(
        scrollableState: ScrollableState,
        orientation: Orientation,
        overscrollEffect: OverscrollEffect?,
        reverseDirection: Boolean,
        flingBehavior: FlingBehavior,
        nestedScrollDispatcher: NestedScrollDispatcher,
    ): Boolean {
        var resetPointerInputHandling = false
        if (this.scrollableState != scrollableState) {
            this.scrollableState = scrollableState
            resetPointerInputHandling = true
        }
        this.overscrollEffect = overscrollEffect
        if (this.orientation != orientation) {
            this.orientation = orientation
            resetPointerInputHandling = true
        }
        if (this.reverseDirection != reverseDirection) {
            this.reverseDirection = reverseDirection
            resetPointerInputHandling = true
        }
        this.flingBehavior = flingBehavior
        this.nestedScrollDispatcher = nestedScrollDispatcher
        return resetPointerInputHandling
    }

    fun isVertical(): Boolean = orientation == Vertical
}

private val NoOpScrollScope: ScrollScope =
    object : ScrollScope {
        override fun scrollBy(pixels: Float): Float = pixels
    }

/** Interface to allow re-use across Scrollable and Scrollable2D. */
internal interface ScrollLogic {
    val isFlinging: Boolean

    fun performRawScroll(scroll: Offset): Offset

    suspend fun doFlingAnimation(available: Velocity): Velocity

    fun shouldScrollImmediately(): Boolean

    /** Opens a scrolling session with nested scrolling and overscroll support. */
    suspend fun scroll(
        scrollPriority: MutatePriority = MutatePriority.Default,
        block: suspend NestedScrollScope.() -> Unit,
    )
}

/**
 * This method returns [ScrollableDefaultFlingBehavior] whose density will be managed by the
 * [ScrollableElement] because it's not created inside [Composable] context. This is different from
 * [rememberPlatformDefaultFlingBehavior] which creates [FlingBehavior] whose density depends on
 * [androidx.compose.ui.platform.LocalDensity] and is automatically resolved.
 */
internal expect fun platformScrollableDefaultFlingBehavior(): ScrollableDefaultFlingBehavior

/**
 * Create and remember default [FlingBehavior] that will represent natural platform fling decay
 * behavior.
 */
@Composable internal expect fun rememberPlatformDefaultFlingBehavior(): FlingBehavior

/**
 * Scroll deltas originating from the semantics system. Should be dispatched as an animation driven
 * event.
 */
private suspend fun ScrollingLogic.semanticsScrollBy(offset: Offset): Offset {
    var previousValue = 0f
    scroll(scrollPriority = MutatePriority.Default) {
        animate(0f, offset.toFloat()) { currentValue, _ ->
            val delta = currentValue - previousValue
            val consumed =
                scrollBy(offset = delta.reverseIfNeeded().toOffset(), source = UserInput)
                    .toFloat()
                    .reverseIfNeeded()
            previousValue += consumed
        }
    }
    return previousValue.toOffset()
}

internal interface OnScrollChangedDispatcher {
    fun dispatchScrollDeltaInfo(delta: Offset)
}
