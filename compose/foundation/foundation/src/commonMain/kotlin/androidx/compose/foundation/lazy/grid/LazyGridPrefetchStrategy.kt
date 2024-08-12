/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.offsetOnMainAxis
import androidx.compose.foundation.gestures.snapping.sizeOnMainAxis
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collection.mutableVectorOf

/**
 * Implementations of this interface control which indices of a LazyGrid should be prefetched
 * (precomposed and premeasured during idle time) as the user interacts with it.
 *
 * Implementations should invoke [LazyGridPrefetchScope.scheduleLinePrefetch] to schedule prefetches
 * from the [onScroll] and [onVisibleItemsUpdated] callbacks. If any of the returned PrefetchHandles
 * no longer need to be prefetched, use [LazyLayoutPrefetchState.PrefetchHandle.cancel] to cancel
 * the request.
 */
@ExperimentalFoundationApi
interface LazyGridPrefetchStrategy {

    /**
     * A [PrefetchScheduler] implementation which will be used to execute prefetch requests for this
     * strategy implementation. If null, the default [PrefetchScheduler] for the platform will be
     * used.
     */
    val prefetchScheduler: PrefetchScheduler?
        get() = null

    /**
     * onScroll is invoked when the LazyGrid scrolls, whether or not the visible items have changed.
     * If the visible items have also changed, then this will be invoked in the same frame *after*
     * [onVisibleItemsUpdated].
     *
     * @param delta the change in scroll direction. Delta < 0 indicates scrolling down while delta >
     *   0 indicates scrolling up.
     * @param layoutInfo the current [LazyGridLayoutInfo]
     */
    fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo)

    /**
     * onVisibleItemsUpdated is invoked when the LazyGrid scrolls if the visible items have changed.
     *
     * @param layoutInfo the current [LazyGridLayoutInfo]. Info about the updated visible items can
     *   be found in [LazyGridLayoutInfo.visibleItemsInfo].
     */
    fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo)

    /**
     * onNestedPrefetch is invoked when a parent LazyLayout has prefetched content which contains
     * this LazyGrid. It gives this LazyGrid a chance to request prefetch for some of its own
     * children before coming onto screen.
     *
     * Implementations can use [NestedPrefetchScope.schedulePrefetch] to schedule child prefetches.
     * For example, this is useful if this LazyGrid is a LazyRow that is a child of a LazyColumn: in
     * that case, [onNestedPrefetch] can schedule the children it expects to be visible when it
     * comes onto screen, giving the LazyLayout infra a chance to compose these children ahead of
     * time and reduce jank.
     *
     * Generally speaking, [onNestedPrefetch] should only request prefetch for children that it
     * expects to actually be visible when this grid is scrolled into view.
     *
     * @param firstVisibleItemIndex the index of the first visible item. It should be used to start
     *   prefetching from the correct index in case the grid has been created at a non-zero offset.
     */
    fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int)
}

/** Scope for callbacks in [LazyGridPrefetchStrategy] which allows prefetches to be requested. */
@ExperimentalFoundationApi
interface LazyGridPrefetchScope {

    /**
     * Schedules a prefetch for the given line index. Requests are executed in the order they're
     * requested. If a requested prefetch is no longer necessary (for example, due to changing
     * scroll direction), the request should be canceled via
     * [LazyLayoutPrefetchState.PrefetchHandle.cancel].
     *
     * See [PrefetchScheduler].
     *
     * @param lineIndex index of the row or column to prefetch
     */
    fun scheduleLinePrefetch(lineIndex: Int): List<LazyLayoutPrefetchState.PrefetchHandle>
}

/**
 * Creates an instance of the default [LazyGridPrefetchStrategy], allowing for customization of the
 * nested prefetch count.
 *
 * @param nestedPrefetchItemCount specifies how many inner items should be prefetched when this
 *   LazyGrid is nested inside another LazyLayout. For example, if this is the state for a
 *   horizontal LazyGrid nested in a vertical LazyGrid, you might want to set this to the number of
 *   items that will be visible when this grid is scrolled into view.
 */
@ExperimentalFoundationApi
fun LazyGridPrefetchStrategy(nestedPrefetchItemCount: Int = 2): LazyGridPrefetchStrategy =
    DefaultLazyGridPrefetchStrategy(nestedPrefetchItemCount)

/**
 * The default prefetching strategy for LazyGrids - this will be used automatically if no other
 * strategy is provided.
 */
@OptIn(ExperimentalFoundationApi::class)
@Stable
private class DefaultLazyGridPrefetchStrategy(private val nestedPrefetchItemCount: Int = 2) :
    LazyGridPrefetchStrategy {

    /**
     * The index scheduled to be prefetched (or the last prefetched index if the prefetch is done).
     */
    private var lineToPrefetch = -1

    /** The list of handles associated with the items from the [lineToPrefetch] line. */
    private val currentLinePrefetchHandles =
        mutableVectorOf<LazyLayoutPrefetchState.PrefetchHandle>()

    /**
     * Keeps the scrolling direction during the previous calculation in order to be able to detect
     * the scrolling direction change.
     */
    private var wasScrollingForward = false

    override fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
        if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val scrollingForward = delta < 0
            val lineToPrefetch: Int
            val closestNextItemToPrefetch: Int
            if (scrollingForward) {
                lineToPrefetch =
                    1 +
                        layoutInfo.visibleItemsInfo.last().let {
                            if (layoutInfo.orientation == Orientation.Vertical) it.row
                            else it.column
                        }
                closestNextItemToPrefetch = layoutInfo.visibleItemsInfo.last().index + 1
            } else {
                lineToPrefetch =
                    -1 +
                        layoutInfo.visibleItemsInfo.first().let {
                            if (layoutInfo.orientation == Orientation.Vertical) it.row
                            else it.column
                        }
                closestNextItemToPrefetch = layoutInfo.visibleItemsInfo.first().index - 1
            }
            if (closestNextItemToPrefetch in 0 until layoutInfo.totalItemsCount) {
                if (
                    lineToPrefetch != this@DefaultLazyGridPrefetchStrategy.lineToPrefetch &&
                        lineToPrefetch >= 0
                ) {
                    if (wasScrollingForward != scrollingForward) {
                        // the scrolling direction has been changed which means the last prefetched
                        // is not going to be reached anytime soon so it is safer to dispose it.
                        // if this line is already visible it is safe to call the method anyway
                        // as it will be no-op
                        currentLinePrefetchHandles.forEach { it.cancel() }
                    }
                    this@DefaultLazyGridPrefetchStrategy.wasScrollingForward = scrollingForward
                    this@DefaultLazyGridPrefetchStrategy.lineToPrefetch = lineToPrefetch
                    currentLinePrefetchHandles.clear()
                    currentLinePrefetchHandles.addAll(scheduleLinePrefetch(lineToPrefetch))
                }
                if (scrollingForward) {
                    val lastItem = layoutInfo.visibleItemsInfo.last()
                    val itemSize = lastItem.sizeOnMainAxis(layoutInfo.orientation)
                    val itemSpacing = layoutInfo.mainAxisItemSpacing
                    val distanceToPrefetchItem =
                        lastItem.offsetOnMainAxis(layoutInfo.orientation) + itemSize + itemSpacing -
                            layoutInfo.viewportEndOffset
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < -delta) {
                        currentLinePrefetchHandles.forEach { it.markAsUrgent() }
                    }
                } else {
                    val firstItem = layoutInfo.visibleItemsInfo.first()
                    val distanceToPrefetchItem =
                        layoutInfo.viewportStartOffset -
                            firstItem.offsetOnMainAxis(layoutInfo.orientation)
                    // if in the next frame we will get the same delta will we reach the item?
                    if (distanceToPrefetchItem < delta) {
                        currentLinePrefetchHandles.forEach { it.markAsUrgent() }
                    }
                }
            }
        }
    }

    override fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo) {
        if (lineToPrefetch != -1 && layoutInfo.visibleItemsInfo.isNotEmpty()) {
            val expectedLineToPrefetch =
                if (wasScrollingForward) {
                    layoutInfo.visibleItemsInfo.last().let {
                        if (layoutInfo.orientation == Orientation.Vertical) it.row else it.column
                    } + 1
                } else {
                    layoutInfo.visibleItemsInfo.first().let {
                        if (layoutInfo.orientation == Orientation.Vertical) it.row else it.column
                    } - 1
                }
            if (lineToPrefetch != expectedLineToPrefetch) {
                lineToPrefetch = -1
                currentLinePrefetchHandles.forEach { it.cancel() }
                currentLinePrefetchHandles.clear()
            }
        }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        repeat(nestedPrefetchItemCount) { i -> schedulePrefetch(firstVisibleItemIndex + i) }
    }
}
