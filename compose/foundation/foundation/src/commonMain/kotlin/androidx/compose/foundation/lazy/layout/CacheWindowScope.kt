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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.unit.Density

@OptIn(ExperimentalFoundationApi::class)
/**
 * Provides layout state and prefetching APIs to [CacheWindowLogic].
 *
 * Implemented by concrete lazy layouts to bridge layout-specific state with the shared prefetching
 * logic.
 */
internal interface CacheWindowScope {
    /** Returns the total number of items in the layout. */
    val totalItemsCount: Int

    /** Returns the number of currently visible lines. */
    val visibleLineCount: Int

    /** Returns `true` if the layout contains visible items. */
    val hasVisibleItems: Boolean

    /** Returns the index of the first visible item. */
    val firstVisibleItemIndex: Int

    /** Returns the layout density, or `null` if unavailable. */
    val density: Density?

    /** Returns the index of the last visible item. */
    val lastVisibleItemIndex: Int

    /** Returns the viewport size along the main axis, in pixels. */
    val mainAxisViewportSize: Int

    /**
     * Populates [perLaneMainAxisExtraStartSpace] with start-side overflow space per lane.
     *
     * Overflow space represents how much the first visible item in each lane extends beyond the
     * start of the viewport, in pixels.
     *
     * @param perLaneMainAxisExtraStartSpace array to populate with overflow space values
     */
    fun updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace: IntArray)

    /**
     * Populates [perLaneMainAxisExtraEndSpace] with end-side overflow space per lane.
     *
     * Overflow space represents how much the last visible item in each lane extends beyond the end
     * of the viewport, in pixels.
     *
     * @param perLaneMainAxisExtraEndSpace array to populate with overflow space values
     */
    fun updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace: IntArray)

    /**
     * Populates [perLaneFirstVisibleItemIndex] with the first visible item index per lane.
     *
     * @param perLaneFirstVisibleItemIndex array to populate with item indexes
     */
    fun updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex: IntArray)

    /**
     * Populates [perLaneVisibleItemIndexes] with the last visible item index per lane.
     *
     * @param perLaneVisibleItemIndexes array to populate with item indexes
     */
    fun updatePerLaneVisibleItemIndexes(perLaneVisibleItemIndexes: IntArray)

    /**
     * Schedules a prefetch for the specified [itemIndex] and [lane].
     *
     * @param lane layout lane index
     * @param itemIndex item index to prefetch
     * @param onItemPrefetched callback invoked with the item's main-axis size in pixels when
     *   completed
     * @return list of [LazyLayoutPrefetchState.PrefetchHandle]s for the scheduled prefetch requests
     */
    fun schedulePrefetch(
        lane: Int,
        itemIndex: Int,
        onItemPrefetched: (itemSize: Int) -> Unit,
    ): List<LazyLayoutPrefetchState.PrefetchHandle>

    /**
     * Returns the main-axis size of the visible item, in pixels.
     *
     * @param indexInVisibleItems 0-based index within currently visible items
     */
    fun getVisibleItemSize(indexInVisibleItems: Int): Int

    /**
     * Returns the data index of the visible item.
     *
     * @param indexInVisibleItems 0-based index within currently visible items
     */
    fun getVisibleItemIndex(indexInVisibleItems: Int): Int

    /**
     * Returns the unique key of the visible item.
     *
     * @param indexInVisibleItems 0-based index within currently visible items
     */
    fun getVisibleItemKey(indexInVisibleItems: Int): Any

    /**
     * Returns the lane index of the visible item.
     *
     * @param indexInVisibleItems 0-based index within currently visible items
     */
    fun getVisibleItemLane(indexInVisibleItems: Int): Int

    /**
     * Returns the last item index in the current line.
     *
     * @param currentItemIndex current item index in line
     */
    fun lastItemIndexInLine(currentItemIndex: Int): Int

    /** Returns the index of the last item in the layout. */
    fun getLastItemIndex(): Int

    /**
     * Returns the next item index scrolling forward.
     *
     * @param lane layout lane index
     * @param currentItemIndex starting item index
     */
    fun getNextEndItemIndexInLane(lane: Int, currentItemIndex: Int): Int = currentItemIndex + 1

    /**
     * Returns the next item index scrolling backward.
     *
     * @param lane layout lane index
     * @param currentItemIndex starting item index
     */
    fun getNextStartItemIndexInLane(lane: Int, currentItemIndex: Int) = currentItemIndex - 1

    /**
     * Returns `true` if the item spans across all lanes.
     *
     * @param itemIndex item index
     */
    fun isSpanLine(itemIndex: Int) = false
}

internal inline fun CacheWindowScope.forEachVisibleItem(
    action: (itemIndex: Int, itemKey: Any, mainAxisSize: Int, lane: Int) -> Unit
) {
    repeat(visibleLineCount) {
        action(
            getVisibleItemIndex(it),
            getVisibleItemKey(it),
            getVisibleItemSize(it),
            getVisibleItemLane(it),
        )
    }
}
