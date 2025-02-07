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

package androidx.compose.foundation.lazy

import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.singleAxisViewportSize
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.foundation.lazy.layout.PrefetchScheduler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastForEach
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * This is a transition class based on [LazyListPrefetchStrategy] where we will perform a window
 * based prefetching for items in the direction of the scroll movement (ahead).
 */
@ExperimentalFoundationApi
internal class CacheWindowListPrefetchStrategy(
    private val prefetchCacheWindow: LazyLayoutCacheWindow,
    private val density: Density,
    override val prefetchScheduler: PrefetchScheduler? = null
) : LazyListPrefetchStrategy {

    /** Handles for prefetched items in the current forward window. */
    private val prefetchWindowHandles = mutableIntObjectMapOf<PrefetchHandle>()

    private val indicesToRemove = mutableIntSetOf()
    /**
     * Cache for items sizes in the current window. Holds sizes for both visible and non-visible
     * items
     */
    private val windowCache = mutableIntIntMapOf()
    private var previousPassDelta = 0f

    /**
     * Indices for the start and end of the cache window. The items between
     * [prefetchWindowStartIndex] and [prefetchWindowEndIndex] can be:
     * 1) Visible.
     * 2) Cached.
     * 3) Scheduled for prefetching.
     * 4) Not scheduled yet.
     */
    internal var prefetchWindowStartIndex = Int.MAX_VALUE
        private set

    internal var prefetchWindowEndIndex = Int.MIN_VALUE
        private set

    /**
     * Keeps track of the "extra" space used. Extra space starts by being the amount of space
     * occupied by the first and last visible items outside of the viewport, that is, how much
     * they're "peeking" out of view. These values will be updated as we fill the cache window.
     */
    private var prefetchWindowStartExtraSpace = 0
    private var prefetchWindowEndExtraSpace = 0

    /** Signals that we should run the window refilling loop from start. */
    private var shouldRefillWindow = false

    /** Keep the latest item count where it can be used more easily. */
    private var itemsCount = 0

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        updateCacheWindow(delta, layoutInfo)
        previousPassDelta = delta
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        itemsCount = layoutInfo.totalItemsCount
        // If visible items changed, update cached information. Any items that were visible
        // and became out of bounds will either count for the cache window or be cancelled/removed
        // by [cancelOutOfBounds]. If any items changed sizes we re-trigger the window filling
        // update.
        if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
            layoutInfo.visibleItemsInfo.fastForEach { cacheVisibleItemsInfo(it.index, it.size) }
            if (shouldRefillWindow) {
                updateCacheWindow(0.0f, layoutInfo)
                shouldRefillWindow = false
            }
        } else {
            // if no visible items, it means the dataset is empty and we should reset the window.
            // Next time visible items update we we re-start the window strategy.
            resetStrategy()
        }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        // no-op for now
    }

    fun hasValidBounds() =
        prefetchWindowStartIndex != Int.MAX_VALUE && prefetchWindowEndIndex != Int.MIN_VALUE

    private fun LazyListPrefetchScope.updateCacheWindow(
        delta: Float,
        layoutInfo: LazyListLayoutInfo
    ) {
        with(layoutInfo) {
            if (visibleItemsInfo.isNotEmpty()) {
                val firstVisibleItem = visibleItemsInfo.first()
                val lastVisibleItem = visibleItemsInfo.last()

                val viewport = singleAxisViewportSize
                // how much of the first item is peeking out of view at the start of the layout.
                val firstItemOverflowOffset =
                    (firstVisibleItem.offset + beforeContentPadding).coerceAtMost(0)

                // how much of the last item is peeking out of view at the end of the layout
                val lastItemOverflowOffset =
                    lastVisibleItem.offset + lastVisibleItem.size + mainAxisItemSpacing

                // extra space is always positive in this context
                val mainAxisExtraSpaceStart = firstItemOverflowOffset.absoluteValue

                // extra space is always positive in this context
                val mainAxisExtraSpaceEnd =
                    (lastItemOverflowOffset - viewportEndOffset).absoluteValue
                val prefetchForwardWindow =
                    with(prefetchCacheWindow) { density.calculateAheadWindow(viewport) }
                val keepAroundWindow =
                    with(prefetchCacheWindow) { density.calculateBehindWindow(viewport) }

                // save latest item count
                itemsCount = totalItemsCount

                onKeepAround(
                    visibleWindowStart = firstVisibleItem.index,
                    visibleWindowEnd = lastVisibleItem.index,
                    keepAroundWindow = keepAroundWindow,
                    scrollDelta = delta,
                    itemsCount = totalItemsCount,
                    mainAxisExtraSpaceStart = mainAxisExtraSpaceStart,
                    mainAxisExtraSpaceEnd = mainAxisExtraSpaceEnd,
                )

                onPrefetchForward(
                    visibleWindowStart = firstVisibleItem.index,
                    visibleWindowEnd = lastVisibleItem.index,
                    prefetchForwardWindow = prefetchForwardWindow,
                    scrollDelta = delta,
                    mainAxisExtraSpaceStart = mainAxisExtraSpaceStart,
                    mainAxisExtraSpaceEnd = mainAxisExtraSpaceEnd
                )
            }
        }
    }

    private fun resetStrategy() {
        prefetchWindowStartIndex = Int.MAX_VALUE
        prefetchWindowEndIndex = Int.MIN_VALUE
        prefetchWindowStartExtraSpace = 0
        prefetchWindowEndExtraSpace = 0

        windowCache.clear()
        prefetchWindowHandles.removeIf { _, value ->
            value.cancel()
            true
        }
    }

    /**
     * Prefetch Forward Logic: Fill in the forward window with prefetched items from the previous
     * measure pass. If the item is not prefetched yet, schedule a prefetching for it. Once a
     * prefetch returns, we check if the window is filled and if not we schedule the next
     * prefetching.
     */
    private fun LazyListPrefetchScope.onPrefetchForward(
        visibleWindowStart: Int,
        visibleWindowEnd: Int,
        prefetchForwardWindow: Int,
        mainAxisExtraSpaceEnd: Int,
        mainAxisExtraSpaceStart: Int,
        scrollDelta: Float
    ) {
        val changedScrollDirection = scrollDelta.sign != previousPassDelta.sign

        if (scrollDelta < 0.0f) { // scrolling forward, starting on last visible
            if (changedScrollDirection || shouldRefillWindow) {
                prefetchWindowEndExtraSpace = (prefetchForwardWindow - mainAxisExtraSpaceEnd)
                prefetchWindowEndIndex = visibleWindowEnd
            } else {
                prefetchWindowEndExtraSpace += scrollDelta.absoluteValue.roundToInt()
            }

            while (prefetchWindowEndExtraSpace > 0 && prefetchWindowEndIndex < itemsCount) {
                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    if (prefetchWindowEndIndex + 1 == visibleWindowEnd + 1) {
                        scrollDelta.absoluteValue >= mainAxisExtraSpaceEnd
                    } else {
                        false
                    }

                // no more items available to fill prefetch window if this is null, break
                val itemSize =
                    getItemSizeOrPrefetch(index = prefetchWindowEndIndex + 1, isUrgent = isUrgent)

                if (itemSize == InvalidItemSize) break

                prefetchWindowEndIndex++
                prefetchWindowEndExtraSpace -= itemSize
            }
            remoteOutOfBoundsItems(0, prefetchWindowStartIndex - 1)
        } else { // scrolling backwards, starting on first visible

            if (changedScrollDirection || shouldRefillWindow) {
                prefetchWindowStartExtraSpace = (prefetchForwardWindow - mainAxisExtraSpaceStart)
                prefetchWindowStartIndex = visibleWindowStart
            } else {
                prefetchWindowStartExtraSpace += scrollDelta.absoluteValue.roundToInt()
            }

            while (prefetchWindowStartExtraSpace > 0 && prefetchWindowStartIndex > 0) {
                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    if (prefetchWindowStartIndex - 1 == visibleWindowStart - 1) {
                        scrollDelta.absoluteValue >= mainAxisExtraSpaceStart
                    } else {
                        false
                    }
                // no more items available to fill prefetch window if this is null, break
                val itemSize =
                    getItemSizeOrPrefetch(index = prefetchWindowStartIndex - 1, isUrgent = isUrgent)
                if (itemSize == InvalidItemSize) break
                prefetchWindowStartIndex--
                prefetchWindowStartExtraSpace -= itemSize
            }
            remoteOutOfBoundsItems(prefetchWindowEndIndex + 1, itemsCount - 1)
        }
    }

    /**
     * Keep Around Logic: Keep around items were visible in the previous measure pass. This means
     * that they will be present in [windowCache] along their size information. We loop through
     * items starting in the last visible one and update [prefetchWindowStartExtraSpace] or
     * [prefetchWindowEndExtraSpace] and also [prefetchWindowStartIndex] or
     * [prefetchWindowEndIndex]. We never schedule a prefetch call for keep around items.
     */
    private fun onKeepAround(
        visibleWindowStart: Int,
        visibleWindowEnd: Int,
        mainAxisExtraSpaceEnd: Int,
        mainAxisExtraSpaceStart: Int,
        keepAroundWindow: Int,
        scrollDelta: Float,
        itemsCount: Int
    ) {

        if (scrollDelta < 0.0f) { // scrolling forward, keep around from firstVisible
            prefetchWindowStartExtraSpace = (keepAroundWindow - mainAxisExtraSpaceStart)
            prefetchWindowStartIndex = visibleWindowStart
            while (prefetchWindowStartExtraSpace > 0 && prefetchWindowStartIndex > 0) {
                val item =
                    if (windowCache.containsKey(prefetchWindowStartIndex - 1)) {
                        windowCache[prefetchWindowStartIndex - 1]
                    } else {
                        break
                    }

                prefetchWindowStartIndex--
                prefetchWindowStartExtraSpace -= item
            }
            remoteOutOfBoundsItems(0, prefetchWindowStartIndex - 1)
        } else { // scrolling backwards, keep around from last visible
            prefetchWindowEndExtraSpace = (keepAroundWindow - mainAxisExtraSpaceEnd)
            prefetchWindowEndIndex = visibleWindowEnd
            while (prefetchWindowEndExtraSpace > 0 && prefetchWindowEndIndex < itemsCount) {
                val item =
                    if (windowCache.containsKey(prefetchWindowStartIndex - 1)) {
                        windowCache[prefetchWindowEndIndex + 1]
                    } else {
                        break
                    }
                prefetchWindowEndIndex++
                prefetchWindowEndExtraSpace -= item
            }
            remoteOutOfBoundsItems(prefetchWindowEndIndex + 1, itemsCount - 1)
        }
    }

    private fun LazyListPrefetchScope.getItemSizeOrPrefetch(index: Int, isUrgent: Boolean): Int {
        return if (windowCache.containsKey(index)) {
            windowCache[index]
        } else if (prefetchWindowHandles.containsKey(index)) {
            // item is scheduled but didn't finish yet
            if (isUrgent) prefetchWindowHandles[index]?.markAsUrgent()
            InvalidItemSize
        } else {
            // item is not scheduled
            prefetchWindowHandles[index] = schedulePrefetch(index) { onItemPrefetched(index, it) }
            if (isUrgent) prefetchWindowHandles[index]?.markAsUrgent()
            InvalidItemSize
        }
    }

    /** Grows the window with measured items and prefetched items. */
    private fun cachePrefetchedItem(index: Int, size: Int) {
        windowCache[index] = size
        if (index > prefetchWindowEndIndex) {
            prefetchWindowEndIndex = index
            prefetchWindowEndExtraSpace -= size
        } else if (index < prefetchWindowStartIndex) {
            prefetchWindowStartIndex = index
            prefetchWindowStartExtraSpace -= size
        }
    }

    /**
     * When caching visible items we need to check if the existing item changed sizes. If so, we
     * will set [shouldRefillWindow] which will trigger a complete window filling and cancel any out
     * of bounds requests.
     */
    private fun cacheVisibleItemsInfo(index: Int, size: Int) {
        if (windowCache.containsKey(index) && windowCache[index] != size) {
            shouldRefillWindow = true
        }

        windowCache[index] = size
        // We're caching a visible item, remove its handle since we won't need it anymore.
        prefetchWindowStartIndex = minOf(prefetchWindowStartIndex, index)
        prefetchWindowEndIndex = maxOf(prefetchWindowEndIndex, index)
        prefetchWindowHandles.remove(index)?.cancel()
    }

    /** Takes care of removing caches and canceling handles for items that we won't use anymore. */
    private fun remoteOutOfBoundsItems(start: Int, end: Int) {
        indicesToRemove.clear()
        prefetchWindowHandles.forEachKey { if (it in start..end) indicesToRemove.add(it) }

        windowCache.forEachKey { if (it in start..end) indicesToRemove.add(it) }

        indicesToRemove.forEach {
            prefetchWindowHandles.remove(it)?.cancel()
            windowCache.remove(it)
        }
    }

    /**
     * Item prefetching finished, we can cache its information and schedule the next prefetching if
     * needed.
     */
    private fun LazyListPrefetchScope.onItemPrefetched(index: Int, itemSize: Int) {
        cachePrefetchedItem(index, itemSize)
        scheduleNextItemIfNeeded()
    }

    private fun LazyListPrefetchScope.scheduleNextItemIfNeeded() {
        var nextPrefetchableIndex: Int = -1
        // if was scrolling forward
        if (previousPassDelta.sign <= 0) {
            if (prefetchWindowEndExtraSpace > 0) nextPrefetchableIndex = prefetchWindowEndIndex + 1
        } else if (previousPassDelta.sign > 0) {
            if (prefetchWindowStartExtraSpace > 0)
                nextPrefetchableIndex = prefetchWindowStartIndex - 1
        }

        if (nextPrefetchableIndex in 0..itemsCount) {
            prefetchWindowHandles[nextPrefetchableIndex] =
                schedulePrefetch(nextPrefetchableIndex) {
                    onItemPrefetched(nextPrefetchableIndex, it)
                }
        }
    }
}

private const val InvalidItemSize = -1
