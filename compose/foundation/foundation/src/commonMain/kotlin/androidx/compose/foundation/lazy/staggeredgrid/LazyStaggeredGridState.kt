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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.annotation.IntRange as AndroidXIntRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.lazy.layout.AwaitFirstLayoutModifier
import androidx.compose.foundation.lazy.layout.LazyLayoutBeyondBoundsInfo
import androidx.compose.foundation.lazy.layout.LazyLayoutItemAnimator
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnedItemList
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.foundation.lazy.layout.animateScrollToItem
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.FullSpan
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridLaneInfo.Companion.Unset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.unit.Constraints
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.ranges.IntRange
import kotlinx.coroutines.launch

/**
 * Creates a [LazyStaggeredGridState] that is remembered across composition.
 *
 * Calling this function with different parameters on recomposition WILL NOT recreate or change the
 * state. Use [LazyStaggeredGridState.scrollToItem] or [LazyStaggeredGridState.animateScrollToItem]
 * to adjust position instead.
 *
 * @param initialFirstVisibleItemIndex initial position for
 *   [LazyStaggeredGridState.firstVisibleItemIndex]
 * @param initialFirstVisibleItemScrollOffset initial value for
 *   [LazyStaggeredGridState.firstVisibleItemScrollOffset]
 * @return created and memoized [LazyStaggeredGridState] with given parameters.
 */
@Composable
fun rememberLazyStaggeredGridState(
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0
): LazyStaggeredGridState =
    rememberSaveable(saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState(initialFirstVisibleItemIndex, initialFirstVisibleItemScrollOffset)
    }

/**
 * Hoisted state object controlling [LazyVerticalStaggeredGrid] or [LazyHorizontalStaggeredGrid]. In
 * most cases, it should be created via [rememberLazyStaggeredGridState].
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class LazyStaggeredGridState
internal constructor(
    initialFirstVisibleItems: IntArray,
    initialFirstVisibleOffsets: IntArray,
    prefetchScheduler: PrefetchScheduler?
) : ScrollableState {
    /**
     * @param initialFirstVisibleItemIndex initial value for [firstVisibleItemIndex]
     * @param initialFirstVisibleItemOffset initial value for [firstVisibleItemScrollOffset]
     */
    constructor(
        initialFirstVisibleItemIndex: Int = 0,
        initialFirstVisibleItemOffset: Int = 0
    ) : this(
        intArrayOf(initialFirstVisibleItemIndex),
        intArrayOf(initialFirstVisibleItemOffset),
        null
    )

    /**
     * Index of the first visible item across all staggered grid lanes. This does not include items
     * in the content padding region. For the first visible item that includes items in the content
     * padding please use [LazyStaggeredGridLayoutInfo.visibleItemsInfo].
     *
     * This property is observable and when use it in composable function it will be recomposed on
     * each scroll, potentially causing performance issues.
     */
    val firstVisibleItemIndex: Int
        get() = scrollPosition.index

    /**
     * Current offset of the item with [firstVisibleItemIndex] relative to the container start.
     *
     * This property is observable and when use it in composable function it will be recomposed on
     * each scroll, potentially causing performance issues.
     */
    val firstVisibleItemScrollOffset: Int
        get() = scrollPosition.scrollOffset

    /** holder for current scroll position */
    internal val scrollPosition =
        LazyStaggeredGridScrollPosition(
            initialFirstVisibleItems,
            initialFirstVisibleOffsets,
            ::fillNearestIndices
        )

    /**
     * Layout information calculated during last layout pass, with information about currently
     * visible items and container parameters.
     *
     * This property is observable and when use it in composable function it will be recomposed on
     * each scroll, potentially causing performance issues.
     */
    val layoutInfo: LazyStaggeredGridLayoutInfo
        get() = layoutInfoState.value

    /** backing state for [layoutInfo] */
    private val layoutInfoState =
        mutableStateOf(EmptyLazyStaggeredGridLayoutInfo, neverEqualPolicy())

    /** storage for lane assignments for each item for consistent scrolling in both directions */
    internal val laneInfo = LazyStaggeredGridLaneInfo()

    override var canScrollForward: Boolean by mutableStateOf(false)
        private set

    override var canScrollBackward: Boolean by mutableStateOf(false)
        private set

    @get:Suppress("GetterSetterNames")
    override val lastScrolledForward: Boolean
        get() = scrollableState.lastScrolledForward

    @get:Suppress("GetterSetterNames")
    override val lastScrolledBackward: Boolean
        get() = scrollableState.lastScrolledBackward

    internal var remeasurement: Remeasurement? = null
        private set

    internal val remeasurementModifier =
        object : RemeasurementModifier {
            override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                this@LazyStaggeredGridState.remeasurement = remeasurement
            }
        }

    /**
     * Provides a modifier which allows to delay some interactions (e.g. scroll) until layout is
     * ready.
     */
    internal val awaitLayoutModifier = AwaitFirstLayoutModifier()

    internal val beyondBoundsInfo = LazyLayoutBeyondBoundsInfo()

    /** Only used for testing to disable prefetching when needed to test the main logic. */
    /*@VisibleForTesting*/
    internal var prefetchingEnabled: Boolean = true

    /** prefetch state used for precomputing items in the direction of scroll */
    internal val prefetchState: LazyLayoutPrefetchState = LazyLayoutPrefetchState(prefetchScheduler)

    /** state controlling the scroll */
    private val scrollableState = ScrollableState { -onScroll(-it) }

    /** scroll to be consumed during next/current layout pass */
    internal var scrollToBeConsumed = 0f
        private set

    /* @VisibleForTesting */
    internal var measurePassCount = 0

    /** prefetch state */
    private var prefetchBaseIndex: Int = -1
    private val currentItemPrefetchHandles = mutableMapOf<Int, PrefetchHandle>()

    internal val laneCount
        get() = layoutInfoState.value.slots.sizes.size

    /**
     * [InteractionSource] that will be used to dispatch drag events when this list is being
     * dragged. If you want to know whether the fling (or animated scroll) is in progress, use
     * [isScrollInProgress].
     */
    val interactionSource
        get(): InteractionSource = mutableInteractionSource

    /** backing field mutable field for [interactionSource] */
    internal val mutableInteractionSource = MutableInteractionSource()

    /** Stores currently pinned items which are always composed. */
    internal val pinnedItems = LazyLayoutPinnedItemList()

    internal val itemAnimator = LazyLayoutItemAnimator<LazyStaggeredGridMeasuredItem>()

    internal val nearestRange: IntRange by scrollPosition.nearestRangeState

    internal val placementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Call this function to take control of scrolling and gain the ability to send scroll events
     * via [ScrollScope.scrollBy]. All actions that change the logical scroll position must be
     * performed within a [scroll] block (even if they don't call any other methods on this object)
     * in order to guarantee that mutual exclusion is enforced.
     *
     * If [scroll] is called from elsewhere, this will be canceled.
     */
    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) {
        awaitLayoutModifier.waitForFirstLayout()
        scrollableState.scroll(scrollPriority, block)
    }

    /**
     * Whether this [scrollableState] is currently scrolling by gesture, fling or programmatically
     * or not.
     */
    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress

    /** Main scroll callback which adjusts scroll delta and remeasures layout */
    private fun onScroll(distance: Float): Float {
        if (distance < 0 && !canScrollForward || distance > 0 && !canScrollBackward) {
            return 0f
        }
        checkPrecondition(abs(scrollToBeConsumed) <= 0.5f) {
            "entered drag with non-zero pending scroll"
        }
        scrollToBeConsumed += distance

        // scrollToBeConsumed will be consumed synchronously during the forceRemeasure invocation
        // inside measuring we do scrollToBeConsumed.roundToInt() so there will be no scroll if
        // we have less than 0.5 pixels
        if (abs(scrollToBeConsumed) > 0.5f) {
            val preScrollToBeConsumed = scrollToBeConsumed
            val intDelta = scrollToBeConsumed.roundToInt()
            val scrolledLayoutInfo =
                layoutInfoState.value.copyWithScrollDeltaWithoutRemeasure(delta = intDelta)
            if (scrolledLayoutInfo != null) {
                applyMeasureResult(result = scrolledLayoutInfo, visibleItemsStayedTheSame = true)
                // we don't need to remeasure, so we only trigger re-placement:
                placementScopeInvalidator.invalidateScope()

                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed, scrolledLayoutInfo)
            } else {
                remeasurement?.forceRemeasure()

                notifyPrefetch(preScrollToBeConsumed - scrollToBeConsumed)
            }
        }

        // here scrollToBeConsumed is already consumed during the forceRemeasure invocation
        if (abs(scrollToBeConsumed) <= 0.5f) {
            // We consumed all of it - we'll hold onto the fractional scroll for later, so report
            // that we consumed the whole thing
            return distance
        } else {
            val scrollConsumed = distance - scrollToBeConsumed
            // We did not consume all of it - return the rest to be consumed elsewhere (e.g.,
            // nested scrolling)
            scrollToBeConsumed = 0f // We're not consuming the rest, give it back
            return scrollConsumed
        }
    }

    /**
     * Instantly brings the item at [index] to the top of layout viewport, offset by [scrollOffset]
     * pixels.
     *
     * @param index the index to which to scroll. MUST NOT be negative.
     * @param scrollOffset the offset where the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a reversed list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun scrollToItem(
        /* @IntRange(from = 0) */
        index: Int,
        scrollOffset: Int = 0
    ) {
        scroll { snapToItemInternal(index, scrollOffset, forceRemeasure = true) }
    }

    /**
     * Animate (smooth scroll) to the given item.
     *
     * @param index the index to which to scroll. MUST NOT be negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    suspend fun animateScrollToItem(
        /* @IntRange(from = 0) */
        index: Int,
        scrollOffset: Int = 0
    ) {
        val layoutInfo = layoutInfoState.value
        val numOfItemsToTeleport = 100 * layoutInfo.slots.sizes.size
        scroll {
            LazyLayoutScrollScope(this@LazyStaggeredGridState, this)
                .animateScrollToItem(
                    index,
                    scrollOffset,
                    numOfItemsToTeleport,
                    layoutInfo.density,
                    this
                )
        }
    }

    internal val measurementScopeInvalidator = ObservableScopeInvalidator()

    /**
     * Requests the item at [index] to be at the start of the viewport during the next remeasure,
     * offset by [scrollOffset], and schedules a remeasure.
     *
     * The scroll position will be updated to the requested position rather than maintain the index
     * based on the first visible item key (when a data set change will also be applied during the
     * next remeasure), but *only* for the next remeasure.
     *
     * Any scroll in progress will be cancelled.
     *
     * @param index the index to which to scroll. Must be non-negative.
     * @param scrollOffset the offset that the item should end up after the scroll. Note that
     *   positive offset refers to forward scroll, so in a top-to-bottom list, positive offset will
     *   scroll the item further upward (taking it partly offscreen).
     */
    fun requestScrollToItem(@AndroidXIntRange(from = 0) index: Int, scrollOffset: Int = 0) {
        // Cancel any scroll in progress.
        if (isScrollInProgress) {
            layoutInfoState.value.coroutineScope.launch { stopScroll() }
        }

        snapToItemInternal(index, scrollOffset, forceRemeasure = false)
    }

    internal fun snapToItemInternal(index: Int, scrollOffset: Int, forceRemeasure: Boolean) {
        val positionChanged =
            scrollPosition.index != index || scrollPosition.scrollOffset != scrollOffset
        // sometimes this method is called not to scroll, but to stay on the same index when
        // the data changes, as by default we maintain the scroll position by key, not index.
        // when this happens we don't need to reset the animations as from the user perspective
        // we didn't scroll anywhere and if there is an offset change for an item, this change
        // should be animated.
        // however, when the request is to really scroll to a different position, we have to
        // reset previously known item positions as we don't want offset changes to be animated.
        // this offset should be considered as a scroll, not the placement change.
        if (positionChanged) {
            itemAnimator.reset()
        }
        val layoutInfo = layoutInfoState.value
        val visibleItem = layoutInfo.findVisibleItem(index)
        if (visibleItem != null && positionChanged) {
            val currentOffset =
                if (layoutInfo.orientation == Orientation.Vertical) {
                    visibleItem.offset.y
                } else {
                    visibleItem.offset.x
                }
            val delta = currentOffset + scrollOffset
            val offsets =
                IntArray(layoutInfo.firstVisibleItemScrollOffsets.size) {
                    layoutInfo.firstVisibleItemScrollOffsets[it] + delta
                }
            scrollPosition.updateScrollOffset(offsets)
        } else {
            scrollPosition.requestPositionAndForgetLastKnownKey(index, scrollOffset)
        }
        if (forceRemeasure) {
            remeasurement?.forceRemeasure()
        } else {
            measurementScopeInvalidator.invalidateScope()
        }
    }

    /** Maintain scroll position for item based on custom key if its index has changed. */
    internal fun updateScrollPositionIfTheFirstItemWasMoved(
        itemProvider: LazyLayoutItemProvider,
        firstItemIndex: IntArray
    ): IntArray =
        scrollPosition.updateScrollPositionIfTheFirstItemWasMoved(itemProvider, firstItemIndex)

    override fun dispatchRawDelta(delta: Float): Float = scrollableState.dispatchRawDelta(delta)

    /** Start prefetch of the items based on provided delta */
    private fun notifyPrefetch(
        delta: Float,
        info: LazyStaggeredGridMeasureResult = layoutInfoState.value
    ) {
        if (prefetchingEnabled && info.visibleItemsInfo.isNotEmpty()) {
            val scrollingForward = delta < 0

            val prefetchIndex =
                if (scrollingForward) {
                    info.visibleItemsInfo.last().index
                } else {
                    info.visibleItemsInfo.first().index
                }

            if (prefetchIndex == prefetchBaseIndex) {
                // Already prefetched based on this index
                return
            }
            prefetchBaseIndex = prefetchIndex

            val prefetchHandlesUsed = mutableSetOf<Int>()
            var targetIndex = prefetchIndex
            val slots = info.slots
            val laneCount = slots.sizes.size
            for (lane in 0 until laneCount) {
                val previousIndex = targetIndex

                // find the next item for each line and prefetch if it is valid
                targetIndex =
                    if (scrollingForward) {
                        laneInfo.findNextItemIndex(previousIndex, lane)
                    } else {
                        laneInfo.findPreviousItemIndex(previousIndex, lane)
                    }
                if (
                    targetIndex !in (0 until info.totalItemsCount) ||
                        targetIndex in prefetchHandlesUsed
                ) {
                    break
                }

                prefetchHandlesUsed += targetIndex
                if (targetIndex in currentItemPrefetchHandles) {
                    continue
                }

                val isFullSpan = info.spanProvider.isFullSpan(targetIndex)
                val slot = if (isFullSpan) 0 else lane
                val span = if (isFullSpan) laneCount else 1

                val crossAxisSize =
                    when {
                        span == 1 -> slots.sizes[slot]
                        else -> {
                            val start = slots.positions[slot]
                            val endSlot = slot + span - 1
                            val end = slots.positions[endSlot] + slots.sizes[endSlot]
                            end - start
                        }
                    }

                val constraints =
                    if (info.orientation == Orientation.Vertical) {
                        Constraints.fixedWidth(crossAxisSize)
                    } else {
                        Constraints.fixedHeight(crossAxisSize)
                    }

                currentItemPrefetchHandles[targetIndex] =
                    prefetchState.schedulePrefetch(index = targetIndex, constraints = constraints)
            }

            clearLeftoverPrefetchHandles(prefetchHandlesUsed)
        }
    }

    private fun clearLeftoverPrefetchHandles(prefetchHandlesUsed: Set<Int>) {
        val iterator = currentItemPrefetchHandles.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in prefetchHandlesUsed) {
                entry.value.cancel()
                iterator.remove()
            }
        }
    }

    private fun cancelPrefetchIfVisibleItemsChanged(info: LazyStaggeredGridLayoutInfo) {
        val items = info.visibleItemsInfo
        if (prefetchBaseIndex != -1 && items.isNotEmpty()) {
            if (prefetchBaseIndex !in items.first().index..items.last().index) {
                prefetchBaseIndex = -1
                currentItemPrefetchHandles.values.forEach { it.cancel() }
                currentItemPrefetchHandles.clear()
            }
        }
    }

    /** updates state after measure pass */
    internal fun applyMeasureResult(
        result: LazyStaggeredGridMeasureResult,
        visibleItemsStayedTheSame: Boolean = false
    ) {
        scrollToBeConsumed -= result.consumedScroll
        layoutInfoState.value = result

        if (visibleItemsStayedTheSame) {
            scrollPosition.updateScrollOffset(result.firstVisibleItemScrollOffsets)
        } else {
            scrollPosition.updateFromMeasureResult(result)
            cancelPrefetchIfVisibleItemsChanged(result)
        }
        canScrollBackward = result.canScrollBackward
        canScrollForward = result.canScrollForward

        measurePassCount++
    }

    private fun fillNearestIndices(itemIndex: Int, laneCount: Int): IntArray {
        val indices = IntArray(laneCount)
        if (layoutInfoState.value.spanProvider.isFullSpan(itemIndex)) {
            indices.fill(itemIndex)
            return indices
        }

        // reposition spans if needed to ensure valid indices
        laneInfo.ensureValidIndex(itemIndex + laneCount)
        val targetLaneIndex =
            when (val previousLane = laneInfo.getLane(itemIndex)) {
                // lane was never set or contains obsolete full span (the check for full span above)
                Unset,
                FullSpan -> 0
                // lane was previously set, keep item to the same lane
                else -> {
                    requirePrecondition(previousLane >= 0) {
                        "Expected positive lane number, got $previousLane instead."
                    }
                    minOf(previousLane, laneCount)
                }
            }

        // fill lanes before starting index
        var currentItemIndex = itemIndex
        for (lane in (targetLaneIndex - 1) downTo 0) {
            indices[lane] = laneInfo.findPreviousItemIndex(currentItemIndex, lane)
            if (indices[lane] == Unset) {
                indices.fill(-1, toIndex = lane)
                break
            }
            currentItemIndex = indices[lane]
        }

        indices[targetLaneIndex] = itemIndex

        // fill lanes after starting index
        currentItemIndex = itemIndex
        for (lane in (targetLaneIndex + 1) until laneCount) {
            indices[lane] = laneInfo.findNextItemIndex(currentItemIndex, lane)
            currentItemIndex = indices[lane]
        }

        return indices
    }

    companion object {
        /** The default implementation of [Saver] for [LazyStaggeredGridState] */
        val Saver =
            listSaver<LazyStaggeredGridState, IntArray>(
                save = { state ->
                    listOf(state.scrollPosition.indices, state.scrollPosition.scrollOffsets)
                },
                restore = { LazyStaggeredGridState(it[0], it[1], null) }
            )
    }
}
