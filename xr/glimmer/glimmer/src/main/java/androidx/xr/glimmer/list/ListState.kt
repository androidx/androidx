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

package androidx.xr.glimmer.list

import androidx.annotation.IntRange
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastCoerceIn
import androidx.xr.glimmer.list.ListState.Companion.Saver
import kotlin.math.abs

/**
 * Creates a [ListState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param initialFirstVisibleItemIndex the initial value for [ListState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset the initial value for
 *   [ListState.firstVisibleItemScrollOffset]
 */
@Composable
public fun rememberListState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): ListState =
    rememberSaveable(saver = ListState.Saver) {
        ListState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }

/**
 * A state object that can be hoisted to control and observe scrolling.
 *
 * In most cases, this will be created via [rememberListState].
 *
 * @param firstVisibleItemIndex the initial value for [ListState.firstVisibleItemIndex]
 * @param firstVisibleItemScrollOffset the initial value for
 *   [ListState.firstVisibleItemScrollOffset]
 */
public class ListState(firstVisibleItemIndex: Int = 0, firstVisibleItemScrollOffset: Int = 0) :
    ScrollableState {

    private val backingState = ScrollableState { -onScroll(-it) }

    // TODO: b/414961654 - Consider making this abstraction around "anchor item".
    /** The holder class for the current scroll position. */
    private val scrollPosition =
        GlimmerListScrollPosition(firstVisibleItemIndex, firstVisibleItemScrollOffset)

    /** Backing state for [layoutInfo] */
    private val layoutInfoState = mutableStateOf(EmptyLazyListMeasureResult, neverEqualPolicy())

    private val density: Density
        get() = layoutInfoState.value.density

    /**
     * This field is used to save information about the number of "beyond bounds items" that we want
     * to compose. These items are not within the visible bounds of the lazy layout, but we compose
     * them because they are explicitly requested through the
     * [beyond bounds layout API][androidx.compose.ui.layout.BeyondBoundsLayout].
     */
    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    /** Includes information for requesting focus for children as the list scrolls. */
    internal val autoFocusBehaviour = GlimmerListAutoFocusBehaviour()

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()

    internal val internalInteractionSource: MutableInteractionSource = MutableInteractionSource()

    /**
     * The amount of scroll to be consumed in the next layout pass. Scrolling forward is negative
     * - that is, it is the amount that the items are offset in y
     */
    internal var scrollToBeConsumed = 0f
        private set

    /**
     * The scroll value saved from the previous pass to be added to the dispatched scroll value in
     * the next pass.
     */
    private var accumulatedScroll: Float = 0f

    /**
     * This value is updated after the measure pass inside [applyMeasureResult] and defines how much
     * of the [scrollToBeConsumed] was actually used.
     */
    private var consumedScroll: Float = 0f

    internal val nearestRange: kotlin.ranges.IntRange by
        LazyLayoutNearestRangeState(0, NearestItemsSlidingWindowSize, NearestItemsExtraItemCount)

    /**
     * The [Remeasurement] object associated with our layout. It allows us to remeasure
     * synchronously during scroll.
     */
    internal var remeasurement: Remeasurement? = null
        private set

    /** The modifier which provides [remeasurement]. */
    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@ListState.remeasurement = remeasurement
            }
        }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    /**
     * The index of the first item that is visible within the scrollable viewport area not including
     * items in the content padding region. For the first visible item that includes items in the
     * content padding please use [ListLayoutInfo.visibleItemsInfo].
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every change causing potential performance issues.
     */
    public val firstVisibleItemIndex: Int
        @FrequentlyChangingValue get() = scrollPosition.index

    /**
     * The scroll offset of the first visible item. Scrolling forward is positive - i.e., the amount
     * that the item is offset backwards.
     *
     * Note that this property is observable and if you use it in the composable function it will be
     * recomposed on every scroll causing potential performance issues.
     */
    public val firstVisibleItemScrollOffset: Int
        @FrequentlyChangingValue get() = scrollPosition.scrollOffset

    /**
     * The object of [ListLayoutInfo] calculated during the last layout pass. For example, you can
     * use it to calculate what items are currently visible.
     *
     * Note that this property is observable and is updated after every scroll or remeasure. If you
     * use it in the composable function it will be recomposed on every change causing potential
     * performance issues including infinity recomposition loop. Therefore, avoid using it in the
     * composition.
     *
     * If you want to run some side effects like sending an analytics event or updating a state
     * based on this value consider using "snapshotFlow":
     */
    public val layoutInfo: ListLayoutInfo
        @FrequentlyChangingValue get() = layoutInfoState.value

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    public val interactionSource: InteractionSource
        get() = internalInteractionSource

    /** Snaps to the requested scroll position. */
    internal fun snapToItemIndexInternal(index: Int, scrollOffset: Int) {
        scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)
        remeasurement?.forceRemeasure()
    }

    /**
     * When the user provided custom keys for the items we can try to detect when there were items
     * added or removed before our current first visible item and keep this item as the first
     * visible one even given that its index has been changed.
     */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: GlimmerListItemProvider,
        firstItemIndex: Int,
    ): Int = scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

    /**
     * * It's called during the measurement pass once the dispatched [scrollToBeConsumed] has been
     *   handled and the new item positions are known.
     *
     * @param result glimmer lazy list measuring results.
     * @param consumedScroll defines how much scroll was consumed during the measure pass.
     * @param accumulatedScroll tracks the amount of scrolling that the internal logic has reported
     *   as consumed, but wants to save for later measurement. Also if the list consumes more scroll
     *   than it was given (for example, due to rounding errors), this value can be set to a
     *   negative amount to balance it out in the next pass. This amount will be added to the next
     *   dispatched scroll distance, and they will be passed together as [scrollToBeConsumed]. This
     *   value should never be larger than [consumedScroll].
     */
    internal fun applyMeasureResult(
        result: GlimmerListMeasureResult,
        consumedScroll: Float,
        accumulatedScroll: Float,
    ) {
        this.consumedScroll = consumedScroll
        this.accumulatedScroll = accumulatedScroll

        canScrollBackward = result.canScrollBackward
        canScrollForward = result.canScrollForward
        layoutInfoState.value = result

        scrollPosition.updateFromMeasureResult(result)
    }

    internal fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }

        // Skip measure pass.
        if (abs(distance + accumulatedScroll) <= 0.5f) {
            // Inside measuring we do `scrollToBeConsumed.roundToInt()` so there will be no scroll
            // if we have less than 0.5 pixels. So just accumulate it for the next pass.
            accumulatedScroll += distance
            return distance
        }

        scrollToBeConsumed = distance + accumulatedScroll
        // The `forceRemeasure()` invocation triggers the measure pass where `scrollToBeConsumed`
        // will be used to update `consumedScroll` and `accumulatedScroll` values.
        remeasurement?.forceRemeasure()

        // Calculate consumed value excluding the accumulated part's effect, since it's internal.
        val consumedDistance =
            if (consumedScroll == scrollToBeConsumed) {
                // This branch is needed, because `consumedScroll = distance + accumulatedScroll`.
                // There is no need to report `accumulatedScroll` as consumed.
                distance
            } else {
                // If, due to accumulated value, we consumed more than what was provided.
                if (distance >= 0f) {
                    consumedScroll.fastCoerceIn(0f, distance)
                } else {
                    consumedScroll.fastCoerceIn(distance, 0f)
                }
            }

        // It's important to reset this value because there are measure passes
        // triggered from outside scrolling. They read this value as well.
        // So, after we used it, we need to reset it to zero.
        scrollToBeConsumed = 0f

        return consumedDistance
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit,
    ) {
        awaitLayoutModifier.waitForFirstLayout()
        backingState.scroll(scrollPriority, block)
    }

    override fun dispatchRawDelta(delta: Float): Float = backingState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = backingState.isScrollInProgress

    @get:Suppress("GetterSetterNames")
    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    @get:Suppress("GetterSetterNames")
    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    /**
     * Instantly brings the item at [index] to the top of the viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    public suspend fun scrollToItem(@IntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll { snapToItemIndexInternal(index, scrollOffset) }
    }

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    public suspend fun animateScrollToItem(@IntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        scroll {
            GlimmerListScrollScope(this@ListState, this)
                .animateScrollToItem(index, scrollOffset, NumberOfItemsToTeleport, density)
        }
    }

    public companion object {
        /** The default [Saver] implementation for [ListState]. */
        public val Saver: Saver<ListState, Any> =
            listSaver(
                save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
                restore = {
                    ListState(firstVisibleItemIndex = it[0], firstVisibleItemScrollOffset = it[1])
                },
            )
    }
}

private val EmptyLazyListMeasureResult =
    GlimmerListMeasureResult(
        firstVisibleItem = null,
        firstVisibleItemScrollOffset = 0,
        canScrollForward = false,
        consumedScroll = 0f,
        measureResult =
            object : MeasureResult {
                override val width: Int = 0
                override val height: Int = 0

                @Suppress("PrimitiveInCollection")
                override val alignmentLines: Map<AlignmentLine, Int> = emptyMap()

                override fun placeChildren() {}
            },
        visibleItemsInfo = emptyList(),
        viewportStartOffset = 0,
        viewportEndOffset = 0,
        totalItemsCount = 0,
        reverseLayout = false,
        orientation = Orientation.Vertical,
        afterContentPadding = 0,
        mainAxisItemSpacing = 0,
        remeasureNeeded = false,
        density = Density(1f),
        childConstraints = Constraints(),
    )
