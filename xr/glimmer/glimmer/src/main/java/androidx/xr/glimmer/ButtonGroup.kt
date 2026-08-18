/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.scrollableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusTargetModifierNode
import androidx.compose.ui.focus.Focusability
import androidx.compose.ui.focus.requestFocusForChildInRootBounds
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import androidx.xr.glimmer.list.AutoFocusScrollConverter
import androidx.xr.glimmer.list.AwaitFirstLayoutModifier
import kotlinx.coroutines.launch

/**
 * A ButtonGroup is a horizontal container for multiple [Button]s, allowing them to scroll if they
 * exceed the size of the viewport. It is recommended that this container is used for groups of
 * approximately 3-7 related buttons.
 *
 * @sample androidx.xr.glimmer.samples.ButtonGroupSample
 *
 * As the user scrolls, the [state] will keep track of the "current item" via
 * [ButtonGroupState.currentItemIndex]. If this container is currently focused, then it will
 * continuously attempt to gain focus on the current item.
 *
 * @sample androidx.xr.glimmer.samples.ButtonGroupControlCurrentItemSample
 * @param modifier The modifier to be applied to the ButtonGroup
 * @param state The [ButtonGroupState] of this ButtonGroup
 * @param horizontalArrangement The horizontal arrangement of the ButtonGroup's children
 * @param verticalAlignment The vertical alignment of the ButtonGroup's children
 * @param contentPadding The spacing values to apply internally between the container and content
 * @param content The content of the ButtonGroup
 */
@Composable
public fun ButtonGroup(
    modifier: Modifier = Modifier,
    state: ButtonGroupState = rememberButtonGroupState(),
    horizontalArrangement: Arrangement.Horizontal = ButtonGroupDefaults.HorizontalArrangement,
    verticalAlignment: Alignment.Vertical = ButtonGroupDefaults.VerticalAlignment,
    contentPadding: PaddingValues = ButtonGroupDefaults.ContentPadding,
    content: @Composable () -> Unit,
) {
    require(state is ButtonGroupStateImpl) { "Unexpected state of type ${state::class}: $state" }
    Row(
        modifier
            .then(state.awaitFirstLayoutModifier)
            .edgeScrim { contentPadding }
            .scrollableArea(
                state = state,
                orientation = Orientation.Horizontal,
                flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(state)),
                bringIntoViewSpec =
                    // TODO(b/524694593): This only works properly for indirect touch mode
                    object : BringIntoViewSpec {
                        // The scroll drives the focus in this component, not the other way around,
                        // so we want a no-op BringIntoViewSpec
                        override fun calculateScrollDistance(
                            offset: Float,
                            size: Float,
                            containerSize: Float,
                        ): Float = 0f
                    },
            )
            .padding(contentPadding)
            .then(ButtonGroupElement(state, horizontalArrangement)),
        // Wrap the provided arrangement so we can update our state with the child layout positions
        horizontalArrangement =
            remember(horizontalArrangement, state) {
                ArrangementListeningWrapper(
                    wrapped = horizontalArrangement,
                    onChildLayoutsChange = state::updateBounds,
                )
            },
        verticalAlignment = verticalAlignment,
        content = { content() },
    )
}

/** A state object that allows querying and controlling the state of a [ButtonGroup] component. */
@Stable
public sealed interface ButtonGroupState : ScrollableState {
    /**
     * Index of the current child.
     *
     * The index updates continuously as the user scrolls. To get updates only when the index is
     * "settled", use the current item index when [isScrollInProgress] is `false`.
     *
     * Returns -1 if the [ButtonGroup] is empty, or if the layout has not been initialized yet.
     */
    public val currentItemIndex: Int

    /** The number of items currently within the ButtonGroup. */
    public val itemCount: Int

    /**
     * Jump instantly to the item at [index].
     *
     * If the [ButtonGroup] has focus, focus will be requested for the child at this index.
     *
     * @throws IllegalArgumentException if [index] is not in `[0, itemCount)`
     */
    public suspend fun scrollToItem(index: Int)

    /**
     * Scroll to the item at [index] with the given [animationSpec].
     *
     * If the [ButtonGroup] has focus, focus will be requested for the child at this index.
     *
     * @throws IllegalArgumentException if [index] is not in `[0, itemCount)`
     */
    public suspend fun animateScrollToItem(
        index: Int,
        animationSpec: FiniteAnimationSpec<Float> =
            ButtonGroupDefaults.AnimateScrollToItemAnimationSpec,
    )

    public companion object {
        /** The default [Saver] implementation for [ButtonGroupState]. */
        public val Saver: Saver<ButtonGroupState, *> =
            Saver(
                save = { (it as ButtonGroupStateImpl).currentItemIndex },
                restore = { ButtonGroupStateImpl(it.fastCoerceAtLeast(0)) },
            )
    }
}

/**
 * Creates and remembers a [ButtonGroupState].
 *
 * @param initialItemIndex item index to be scrolled to when [ButtonGroup] is initialized
 */
@Composable
public fun rememberButtonGroupState(initialItemIndex: Int = 0): ButtonGroupState =
    rememberSaveable(saver = ButtonGroupState.Saver) { ButtonGroupState(initialItemIndex) }

/**
 * Creates a [ButtonGroupState].
 *
 * @param initialItemIndex item index to be scrolled to when [ButtonGroup] is initialized
 * @see rememberButtonGroupState
 * @see ButtonGroupState.Saver
 */
@RememberInComposition
public fun ButtonGroupState(initialItemIndex: Int = 0): ButtonGroupState {
    require(initialItemIndex >= 0) {
        "initialItemIndex must be non-negative but was $initialItemIndex"
    }
    return ButtonGroupStateImpl(initialItemIndex)
}

/** Default values used with [ButtonGroup]. */
public object ButtonGroupDefaults {
    /** The default arrangement used to horizontally space ButtonGroup children. */
    public val HorizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)

    /** The default alignment used to vertically align ButtonGroup children. */
    public val VerticalAlignment: Alignment.Vertical = Alignment.CenterVertically

    // TODO(b/535205202): Read this ContentPadding value from the spacing subsystem
    /** Internal padding that is applied around the ButtonGroup's children. */
    public val ContentPadding: PaddingValues = PaddingValues(horizontal = 44.dp)

    /** The default animation spec used by [ButtonGroupState.animateScrollToItem]. */
    public val AnimateScrollToItemAnimationSpec: FiniteAnimationSpec<Float> =
        SpringSpec(stiffness = Spring.StiffnessMediumLow)
}

/**
 * An implementation of [ButtonGroupState].
 *
 * @property initialItemIndex the child index that will become the
 *   [ButtonGroupState.currentItemIndex]; this can only happen once a layout is completed, as we
 *   need to know that item's scroll position. If this index is out-of-bounds, it is clamped down to
 *   the last item in the group.
 */
@Stable
internal class ButtonGroupStateImpl(private val initialItemIndex: Int) : ButtonGroupState {
    /**
     * The current accumulated scroll from the user. The viewport should take some of this scroll;
     * the amount is determined by the [ButtonGroup] Composable. The remaining amount of this scroll
     * not used by the viewport should be used for the focus-scroll.
     *
     * If this is set to [Float.NaN], it means that a layout hasn't occurred yet; once
     * [updateBounds] is called, this value will be updated to the center of the [initialItemIndex]
     * (or clamped to the first/last item if that index is out of bounds).
     */
    var userScroll: Float by mutableFloatStateOf(Float.NaN)
        @FrequentlyChangingValue get
        private set

    /**
     * The bounds of each child layout within this ButtonGroup.
     *
     * Should be set via [updateBounds] so we can also adjust scroll if needed.
     */
    var bounds: ButtonGroupChildrenBounds? = null
        private set

    /** The maximum scroll value allowed by this container. */
    var maxScroll by mutableIntStateOf(0)
        private set

    override var itemCount: Int by mutableIntStateOf(0)
        private set

    override var currentItemIndex: Int by mutableIntStateOf(-1)
        private set

    private val backingState = ScrollableState { delta ->
        val bounds = bounds
        val userScroll = this@ButtonGroupStateImpl.userScroll
        val unclampedScroll = userScroll + delta
        val clampedScroll =
            if (bounds == null || bounds.size == 0) {
                unclampedScroll
            } else {
                unclampedScroll.fastCoerceIn(bounds[0].start.toFloat(), bounds.last().end.toFloat())
            }
        this.userScroll = clampedScroll
        this.currentItemIndex = bounds?.getIndexOfClosestChildTo(clampedScroll) ?: -1
        clampedScroll - userScroll
    }

    val awaitFirstLayoutModifier = AwaitFirstLayoutModifier()

    fun updateBounds(newBounds: ButtonGroupChildrenBounds) {
        if (this.bounds == newBounds) return // Didn't actually change, no need to update

        this.bounds = newBounds
        this.itemCount = newBounds.size
        this.maxScroll = newBounds.lastOrNull()?.end ?: 0

        // Since we have changed the bounds of the children, we may need to snap the user's scroll
        // to a new center
        this.currentItemIndex =
            if (newBounds.size == 0) {
                -1
            } else if (userScroll.isNaN()) {
                // This is the first time we've called updateBounds, so use the initialItemIndex
                initialItemIndex.fastCoerceAtMost(newBounds.lastIndex)
            } else {
                newBounds.getIndexOfClosestChildTo(userScroll)
            }
        this.userScroll = newBounds.getOrNull(currentItemIndex)?.center ?: 0f
    }

    override suspend fun scrollToItem(index: Int) = scrollToItemImpl(index, null)

    override suspend fun animateScrollToItem(
        index: Int,
        animationSpec: FiniteAnimationSpec<Float>,
    ) = scrollToItemImpl(index, animationSpec)

    private suspend fun scrollToItemImpl(index: Int, animationSpec: FiniteAnimationSpec<Float>?) {
        if (bounds == null) awaitFirstLayoutModifier.waitForFirstLayout()

        val child =
            bounds!!.getOrNull(index)
                ?: throw IllegalArgumentException("Index $index is out of bounds [0, $itemCount)")

        val delta = child.center - userScroll
        if (animationSpec == null) {
            backingState.scrollBy(delta)
        } else {
            backingState.animateScrollBy(delta, animationSpec)
        }
    }

    override val isScrollInProgress: Boolean
        get() = backingState.isScrollInProgress

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        backingState.scroll(scrollPriority, block)
    }

    override val canScrollBackward: Boolean
        get() = backingState.canScrollBackward

    override val canScrollForward: Boolean
        get() = backingState.canScrollForward

    override val lastScrolledBackward: Boolean
        get() = backingState.lastScrolledBackward

    override val lastScrolledForward: Boolean
        get() = backingState.lastScrolledForward

    override fun dispatchRawDelta(delta: Float): Float = backingState.dispatchRawDelta(delta)
}

/**
 * A [SnapLayoutInfoProvider] that snaps to the [ButtonGroupStateImpl.bounds] closest to the current
 * scroll position.
 */
private fun SnapLayoutInfoProvider(state: ButtonGroupStateImpl): SnapLayoutInfoProvider =
    object : SnapLayoutInfoProvider {
        // do not approach, our snapping positions are discrete.
        override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0.0f

        override fun calculateSnapOffset(velocity: Float): Float {
            val bounds = state.bounds
            // No items, nowhere to snap to
            if (bounds == null || bounds.size == 0) return 0f

            val userScroll = state.userScroll

            val snapToChildIndex = bounds.getIndexOfClosestChildTo(position = userScroll)
            if (snapToChildIndex == -1) {
                return 0f
            }
            val snapToPosition = bounds[snapToChildIndex].center
            return snapToPosition - userScroll
        }
    }

/** Offsets the content drawn in this [ButtonGroup] using the provided [state]. */
@Suppress("ModifierNodeInspectableProperties")
private class ButtonGroupElement(
    private val state: ButtonGroupStateImpl,
    private val arrangement: Arrangement.Horizontal,
) : ModifierNodeElement<ButtonGroupNode>() {
    override fun create(): ButtonGroupNode = ButtonGroupNode(state, arrangement)

    override fun update(node: ButtonGroupNode) {
        node.update(state, arrangement)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonGroupElement) return false

        if (state != other.state) return false
        if (arrangement != other.arrangement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + arrangement.hashCode()
        return result
    }
}

private class ButtonGroupNode(
    private var state: ButtonGroupStateImpl,
    private var arrangement: Arrangement.Horizontal,
) : DelegatingNode(), LayoutModifierNode, SemanticsModifierNode {

    private val focusTargetModifierNode =
        delegate(FocusTargetModifierNode(focusability = Focusability.Never))

    fun update(state: ButtonGroupStateImpl, arrangement: Arrangement.Horizontal) {
        this.state = state
        this.arrangement = arrangement
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // The content does not need to respect the ButtonGroup's max width constraints, as it will
        // scroll horizontally within the container's viewport.
        val rowPlaceable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
        val contentWidth = rowPlaceable.width

        // The layout itself does need to respect the width constraints though
        val viewportWidth = constraints.constrainWidth(contentWidth)
        return layout(width = viewportWidth, height = rowPlaceable.height) {
            val totalScroll = state.userScroll
            val bounds = state.bounds ?: ButtonGroupChildrenBounds.Empty
            val x =
                contentOffset(
                    totalScroll = totalScroll,
                    bounds = bounds,
                    viewportWidth = viewportWidth,
                    contentWidth = contentWidth,
                )
            rowPlaceable.place(x, 0)
            // Request focus at the scroll position
            if (focusTargetModifierNode.focusState.hasFocus) {
                node.requestFocusForChildInLocalBounds(
                    left = totalScroll + x,
                    top = 0f,
                    right = totalScroll + x,
                    bottom = rowPlaceable.height.toFloat(),
                )
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        isTraversalGroup = true
        horizontalScrollAxisRange =
            ScrollAxisRange(
                value = { if (state.userScroll.isNaN()) 0f else state.userScroll },
                maxValue = { state.maxScroll.toFloat() },
            )
        scrollToIndex { index ->
            if (index in 0 until state.itemCount) {
                coroutineScope.launch { state.animateScrollToItem(index) }
                true
            } else {
                false
            }
        }
    }
}

/** Calculates the amount that the content should be translated in the x-axis in the viewport. */
private fun contentOffset(
    totalScroll: Float,
    bounds: ButtonGroupChildrenBounds,
    viewportWidth: Int,
    contentWidth: Int,
): Int {
    // The scrolling algorithm will cut off the start and end of the first and last child
    // respectively, as our SnapFlingBehavior will not let the user actually scroll beyond these
    // points. For that reason, we will make the following adjustments:
    // 1) `userScroll` is considered to be 0 at the center of the first item.
    // 2) `viewportWidth` and `contentWidth` are both shortened by the "dead space" (the first half
    //    of the first item and the last half of the last item) to accurately reflect how those
    //    parts of those children are considered more like overscroll regions.
    val deadSpaceStart = bounds.getOrNull(0)?.center ?: 0f
    val deadSpaceEnd = bounds.lastOrNull()?.let { contentWidth - it.center } ?: 0f

    val scroll =
        convertUserScrollToContentScroll(
            userScroll = totalScroll - deadSpaceStart,
            viewportWidth = viewportWidth - deadSpaceStart - deadSpaceEnd,
            contentWidth = contentWidth - deadSpaceStart - deadSpaceEnd,
        )

    return -scroll.fastRoundToInt()
}

private fun convertUserScrollToContentScroll(
    userScroll: Float,
    viewportWidth: Float,
    contentWidth: Float,
): Float {
    return AutoFocusScrollConverter.convertUserScrollToContentScroll(
            userScroll = userScroll.toDouble(),
            scrollThreshold = viewportWidth * ScrollableFocusLineProportionalThresholdFactor,
            viewportSize = viewportWidth.toDouble(),
            contentLength = contentWidth.toDouble(),
        )
        .toFloat()
}

/** The main-axis bounds of a single child inside of a `Row` or `Column`. */
@JvmInline
@Immutable
internal value class ButtonGroupChildBounds(private val packed: Long) {
    val start: Int
        get() = unpackInt1(packed)

    val end: Int
        get() = unpackInt2(packed)

    override fun toString(): String = "ButtonGroupChildBounds($start-$end)"
}

private val ButtonGroupChildBounds.center: Float
    get() = (start + end) / 2f

/**
 * A wrapper around an array of [ButtonGroupChildBounds] representing a whole row. The array is not
 * exposed to avoid boxing the [ButtonGroupChildBounds.packed] long while still maintaining type
 * safety.
 *
 * These bounds can be automatically returned by wrapping your Row's incoming [Arrangement] in an
 * [ArrangementListeningWrapper].
 */
@Immutable
internal class ButtonGroupChildrenBounds(private val bounds: LongArray) {
    val size: Int
        get() = bounds.size

    /**
     * An [IntervalList] where each interval points to the index of an item in [bounds].
     *
     * The item being pointed to is the item that we should snap to if the user is at the given
     * scroll position (in pixels) within this ButtonGroup.
     *
     * Unlike [bounds], which may have gaps between items, the full range from 0 to
     * `bounds.last().end` is covered here, as we need an answer for "which index should we snap
     * to?" for all possible scroll values.
     */
    private val snapRanges: IntervalList<Int> =
        with(GappedMutableIntervalListBuilder<Int>()) {
            forEachIndexed { index, child ->
                addGappedInterval(start = child.start, end = child.end, value = index)
            }
            build()
        }

    /**
     * If [position] overlaps a child, returns that child's index.
     *
     * Otherwise (if [position] is in the gap between two children), of those two children, returns
     * the index of the one with the closest edge (either start or end) to [position].
     *
     * If there are no children or the ButtonGroup has not been laid out yet, returns -1.
     */
    fun getIndexOfClosestChildTo(position: Float): Int {
        if (bounds.isEmpty() || position.isNaN()) {
            return -1
        }
        val clampedPosition = position.fastRoundToInt().fastCoerceIn(0, snapRanges.size - 1)
        return snapRanges[clampedPosition].value
    }

    operator fun get(index: Int): ButtonGroupChildBounds = ButtonGroupChildBounds(bounds[index])

    inline fun forEachIndexed(action: (index: Int, bounds: ButtonGroupChildBounds) -> Unit) {
        bounds.forEachIndexed { index, packed -> action(index, ButtonGroupChildBounds(packed)) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ButtonGroupChildrenBounds) return false

        if (!bounds.contentEquals(other.bounds)) return false

        return true
    }

    override fun hashCode(): Int {
        return bounds.contentHashCode()
    }

    override fun toString(): String =
        "ButtonGroupChildrenBounds(" +
            "bounds=[${bounds.joinToString { "${unpackInt1(it)}-${unpackInt2(it)}"}}]" +
            ")"

    companion object {
        val Empty = ButtonGroupChildrenBounds(longArrayOf())
    }
}

private fun ButtonGroupChildrenBounds.getOrNull(index: Int): ButtonGroupChildBounds? =
    if (index in 0..<size) get(index) else null

private val ButtonGroupChildrenBounds.lastIndex: Int
    get() = size - 1

private fun ButtonGroupChildrenBounds.last(): ButtonGroupChildBounds = get(lastIndex)

private fun ButtonGroupChildrenBounds.lastOrNull(): ButtonGroupChildBounds? =
    if (size == 0) null else last()

/**
 * Provides access to the [ButtonGroupChildrenBounds] of the [wrapped] arrangement via the
 * [onChildLayoutsChange] callback, which is invoked whenever the children are arranged.
 */
private class ArrangementListeningWrapper(
    private val wrapped: Arrangement.Horizontal,
    private val onChildLayoutsChange: (layouts: ButtonGroupChildrenBounds) -> Unit,
) : Arrangement.Horizontal {
    override val spacing: Dp
        get() = wrapped.spacing

    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        with(wrapped) { arrange(totalSize, sizes, layoutDirection, outPositions) }
        val bounds =
            LongArray(size = outPositions.size) { index ->
                val start = outPositions[index]
                val end = start + sizes[index]
                packInts(start, end)
            }
        onChildLayoutsChange(ButtonGroupChildrenBounds(bounds))
    }
}

/**
 * A [MutableIntervalList] builder that allows for gaps between items. If [addGappedInterval] is
 * called with gaps between the last invocation's `end` and the current invocation's `start`, half
 * of that gap is given to both the previous and current items.
 */
private class GappedMutableIntervalListBuilder<T> {
    private val delegate = MutableIntervalList<T>()

    private var lastEnd: Int = 0
    private var lastValue: T? = null

    /** Adds an interval from [start] (inclusive) to [end] (exclusive). */
    fun addGappedInterval(start: Int, end: Int, value: T) {
        // Zero-size interval, we can skip this
        if (start == end) return

        require(start < end) { "Start ($start) must be less than end ($end)" }
        require(start >= lastEnd) {
            "Gapped intervals must be in ascending order. Last end was $lastEnd, but we are trying to add an interval: [$start, $end)"
        }

        val lastValue = lastValue
        if (lastValue == null) {
            // First interval can be added directly. Note that the bounds of this interval may not
            // start at 0, but we still need to assign that space before any items to the first
            // interval.
            addInterval(size = end, value = value)
        } else {
            val gap = start - lastEnd
            if (gap == 0) {
                // No gap, just add this item normally.
                addInterval(size = end - start, value = value)
            } else {
                // There is a gap. Split the value between the last and current values
                val gapHalfForPreviousItem = gap / 2
                addInterval(gapHalfForPreviousItem, lastValue)

                val gapHalfForCurrentItem = gap - gapHalfForPreviousItem
                addInterval(size = end - start + gapHalfForCurrentItem, value = value)
            }
        }
        this.lastEnd = end
        this.lastValue = value
    }

    private fun addInterval(size: Int, value: T) {
        if (size > 0) {
            delegate.addInterval(size, value)
        }
    }

    fun build(): IntervalList<T> = delegate
}

/**
 * The scroll distance required to put the focus indicator in the center is calculated as a
 * proportion of the [ButtonGroup]'s viewport size, using
 * [ScrollableFocusLineProportionalThresholdFactor].
 *
 * For example, if the visible ButtonGroup width is 500dp and
 * [ScrollableFocusLineProportionalThresholdFactor] is 0.6f, the focus will reach the center after
 * scrolling 300dp of the content — that is, 60% of the viewport width.
 *
 * Note that this behavior only applies to ButtonGroups with enough content to scroll. If the group
 * is too short to scroll, the focus line moves using different rules.
 */
private const val ScrollableFocusLineProportionalThresholdFactor = 0.6

/**
 * Attempts to request focus for the most suitable focusable child node that overlaps with the given
 * rect area ([left], [top], [right], [bottom]).
 *
 * The rectangle is interpreted in the coordinate space **relative to the local node's root**.
 *
 * @see requestFocusForChildInRootBounds to request focus for a rect relative to the Compose root
 */
private fun DelegatableNode.requestFocusForChildInLocalBounds(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
) {
    val rootOrigin = requireLayoutCoordinates().positionInRoot()
    val x = rootOrigin.x
    val y = rootOrigin.y
    requestFocusForChildInRootBounds(
        left = (x + left).fastRoundToInt(),
        top = (y + top).fastRoundToInt(),
        right = (x + right).fastRoundToInt(),
        bottom = (y + bottom).fastRoundToInt(),
    )
}
