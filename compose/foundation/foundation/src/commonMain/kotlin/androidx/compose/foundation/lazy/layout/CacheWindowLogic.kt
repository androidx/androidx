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

package androidx.compose.foundation.lazy.layout

import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.compose.foundation.ComposeFoundationFlags.isMultiLaneCacheWindowEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.traceValue
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

/** Implements the logic for [LazyLayoutCacheWindow] prefetching and item preservation. */
@OptIn(ExperimentalFoundationApi::class)
internal interface CacheWindowLogic {
    fun CacheWindowScope.onScroll(delta: Float)

    fun CacheWindowScope.onVisibleItemsUpdated()

    fun resetStrategy()

    val perLaneCacheWindowStartIndex: IntArray
    val perLaneCacheWindowEndItemIndex: IntArray

    fun hasValidBounds(): Boolean
}

@OptIn(ExperimentalFoundationApi::class)
internal fun CacheWindowLogic(
    cacheWindow: LazyLayoutCacheWindow,
    enableInitialPrefetch: Boolean = true,
    laneCount: () -> Int = { 1 },
): CacheWindowLogic =
    if (isMultiLaneCacheWindowEnabled) {
        MultiLaneCacheWindow(cacheWindow, enableInitialPrefetch, laneCount)
    } else {
        LegacyCacheWindowLogic(cacheWindow, enableInitialPrefetch)
    }

/** Implements the logic for [LazyLayoutCacheWindow] prefetching and item preservation. */
@OptIn(ExperimentalFoundationApi::class)
private class MultiLaneCacheWindow(
    private val cacheWindow: LazyLayoutCacheWindow,
    private val enableInitialPrefetch: Boolean = true,
    private val laneCount: () -> Int = { 1 },
) : CacheWindowLogic {
    /** Handles for prefetched items in the current forward window. */
    private val prefetchWindowHandles = mutableIntObjectMapOf<List<PrefetchHandle>>()

    private val indicesToRemove = mutableIntSetOf()

    /**
     * Cache for items sizes in the current window. Holds sizes for both visible and non-visible
     * items
     */
    private val windowCache = mutableIntIntMapOf()
    private val windowCacheWithItems = mutableIntObjectMapOf<CachedItem>()

    private var previousPassDelta = 0f
    private var previousPassItemCount = UnsetItemCount
    private var hasUpdatedVisibleItemsOnce = false

    /**
     * Indices for the start and end of the cache window for each lane. The items between
     * [perLaneCacheWindowStartIndex] and [perLaneCacheWindowEndItemIndex] can be:
     * 1) Visible.
     * 2) Cached.
     * 3) Scheduled for prefetching.
     * 4) Not scheduled yet.
     */
    private var previousLaneCount = maxOf(1, laneCount())

    override var perLaneCacheWindowStartIndex = IntArray(previousLaneCount) { Int.MAX_VALUE }
        private set

    override var perLaneCacheWindowEndItemIndex = IntArray(previousLaneCount) { Int.MIN_VALUE }
        private set

    /**
     * Keeps track of the "extra" space used for each lane. Extra space starts by being the amount
     * of space occupied by the first and last visible items outside of the viewport, that is, how
     * much they're "peeking" out of view. These values will be updated as we fill the cache window.
     */
    private var perLaneCacheWindowStartSpace = IntArray(previousLaneCount)
    private var perLaneCacheWindowEndSpace = IntArray(previousLaneCount)

    /** First visible item index in each lane. */
    private var perLaneFirstVisibleItemIndex = IntArray(previousLaneCount)

    /** Last visible item index in each lane. */
    private var perLaneLastVisibleItemIndex = IntArray(previousLaneCount)

    /** Start-side main-axis overflow space (extra space outside the viewport) per lane. */
    private var perLaneMainAxisExtraStartSpace = IntArray(previousLaneCount)

    /** End-side main-axis overflow space (extra space outside the viewport) per lane. */
    private var perLaneMainAxisExtraEndSpace = IntArray(previousLaneCount)

    /** First non-visible item index adjacent to the viewport in the scroll direction per lane. */
    private var perLaneFirstNonVisibleItemIndex = IntArray(previousLaneCount)

    private fun handleLaneResize() {
        val newLaneCount = maxOf(1, laneCount())
        if (previousLaneCount != newLaneCount) {
            resetStrategy()
        }
    }

    private val currentLaneCount
        get() = perLaneCacheWindowStartIndex.size

    /**
     * Signals that we should run the window refilling loop from start. This might re-trigger a
     * prefetch in case the window is not filled with item information. There are 3 conditions in
     * which window refilling will happen:
     * 1) After the first layout pass
     * 2) If any of the visible items were resized since the last measure pass.
     * 3) If the total number of items changed since the last measure pass.
     */
    private var shouldRefillWindow = false

    /** Keep the latest item count where it can be used more easily. */
    private var itemsCount = 0

    override fun CacheWindowScope.onScroll(delta: Float) {
        handleLaneResize()
        debugLog { "delta=$delta" }
        traceWindowInfo()
        fillCacheWindowBackward(delta)
        fillCacheWindowForward(delta)
        previousPassDelta = delta
        traceWindowInfo()
        debugLog {
            "perLaneCacheWindowStartSpace=${perLaneCacheWindowStartSpace.contentToString()}\n" +
                "perLaneCacheWindowEndSpace=${perLaneCacheWindowEndSpace.contentToString()}\n" +
                "perLaneCacheWindowStartIndex=${perLaneCacheWindowStartIndex.contentToString()}\n" +
                "perLaneCacheWindowEndItemIndex=${perLaneCacheWindowEndItemIndex.contentToString()}"
        }
    }

    private fun traceWindowInfo() {
        repeat(currentLaneCount) { lane ->
            traceValue(
                "perLaneCacheWindowStartSpace lane=$lane",
                perLaneCacheWindowStartSpace[lane].toLong(),
            )
            traceValue(
                "perLaneCacheWindowEndSpace lane=$lane",
                perLaneCacheWindowEndSpace[lane].toLong(),
            )
            traceValue(
                "perLaneCacheWindowStartIndex lane=$lane",
                perLaneCacheWindowStartIndex[lane].toLong(),
            )
            traceValue(
                "perLaneCacheWindowEndItemIndex lane=$lane",
                perLaneCacheWindowEndItemIndex[lane].toLong(),
            )
        }
    }

    override fun CacheWindowScope.onVisibleItemsUpdated() {
        handleLaneResize()
        debugLog { "hasUpdatedVisibleItemsOnce=$hasUpdatedVisibleItemsOnce" }
        if (!hasUpdatedVisibleItemsOnce && enableInitialPrefetch) {
            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(mainAxisViewportSize) ?: 0 }
            // we won't fill the window if we don't have a prefetch window
            if (prefetchForwardWindow != 0) shouldRefillWindow = true
            hasUpdatedVisibleItemsOnce = true
        }

        /**
         * We already have information about the number of items from before and it actually
         * changed.
         */
        if (previousPassItemCount != UnsetItemCount && previousPassItemCount != totalItemsCount) {
            onDatasetChanged()
        }

        itemsCount = totalItemsCount
        // If visible items changed, update cached information. Any items that were visible
        // and became out of bounds will either count for the cache window or be cancelled/removed
        // by [cancelOutOfBounds]. If any items changed sizes we re-trigger the window filling
        // update.
        if (hasVisibleItems) {
            forEachVisibleItem { index, key, mainAxisSize, lane ->
                if (index != InvalidIndex) cacheVisibleItemsInfo(index, key, mainAxisSize, lane)
            }
            if (shouldRefillWindow) {
                // refill window in accordance with last pass delta
                debugLog { "Refill Window Forward=${previousPassDelta <= 0.0f}" }
                refillWindow(previousPassDelta <= 0.0f)
                shouldRefillWindow = false
            }
        } else {
            // if no visible items, it means the dataset is empty and we should reset the window.
            // Next time visible items update we we re-start the window strategy.
            resetStrategy()
        }

        previousPassItemCount = totalItemsCount
    }

    private fun CacheWindowScope.onDatasetChanged() {
        debugLog { "Total Items Changed" }
        shouldRefillWindow = true
        if (hasVisibleItems) {
            val lastLineIndex = getLastItemIndex()

            repeat(currentLaneCount) { lane ->
                perLaneCacheWindowStartIndex[lane] =
                    perLaneCacheWindowStartIndex[lane].coerceAtLeast(0)
                if (lastLineIndex != InvalidIndex) {
                    perLaneCacheWindowEndItemIndex[lane] =
                        perLaneCacheWindowEndItemIndex[lane].coerceAtMost(lastLineIndex)
                }
            }

            /**
             * Resets the window state. We will refill the window on the direction of the last
             * scroll.
             */
            if (previousPassDelta <= 0f) {
                removeOutOfBoundsItems(lastVisibleItemIndex + 1, itemsCount - 1)
            } else {
                removeOutOfBoundsItems(0, firstVisibleItemIndex - 1)
            }
        }
    }

    override fun hasValidBounds(): Boolean {
        handleLaneResize()
        for (i in 0 until currentLaneCount) {
            if (!laneHasValidBounds(i)) return false
        }
        return true
    }

    private fun laneHasValidBounds(lane: Int) =
        perLaneCacheWindowStartIndex[lane] != Int.MAX_VALUE &&
            perLaneCacheWindowEndItemIndex[lane] != Int.MIN_VALUE

    private fun CacheWindowScope.fillCacheWindowBackward(delta: Float) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val keepAroundWindow =
                with(cacheWindow) { density?.calculateBehindWindow(viewport) ?: 0 }

            // save latest item count
            itemsCount = totalItemsCount

            debugLog {
                "fillCacheWindowBackward perLaneFirstVisibleItemIndex=${perLaneFirstVisibleItemIndex.contentToString()} \n" +
                    "perLaneLastVisibleItemIndex=${perLaneLastVisibleItemIndex.contentToString()} \n" +
                    "keepAroundWindow=$keepAroundWindow \n" +
                    "perLaneMainAxisExtraStartSpace=${perLaneMainAxisExtraStartSpace.contentToString()} \n" +
                    "perLaneMainAxisExtraEndSpace=${perLaneMainAxisExtraEndSpace.contentToString()} \n"
            }

            onKeepAround(
                keepAroundWindow = keepAroundWindow,
                scrollDelta = delta,
                itemsCount = totalItemsCount,
            )
        }
    }

    private fun CacheWindowScope.fillCacheWindowForward(delta: Float) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(viewport) ?: 0 }

            debugLog {
                "fillCacheWindowForward perLaneFirstVisibleItemIndex=${perLaneFirstVisibleItemIndex.contentToString()} \n" +
                    "perLaneLastVisibleItemIndex=${perLaneLastVisibleItemIndex.contentToString()} \n" +
                    "prefetchForwardWindow=$prefetchForwardWindow \n" +
                    "perLaneMainAxisExtraStartSpace=${perLaneMainAxisExtraStartSpace.contentToString()} \n" +
                    "perLaneMainAxisExtraEndSpace=${perLaneMainAxisExtraEndSpace.contentToString()} \n"
            }

            onPrefetchForward(
                prefetchForwardWindow = prefetchForwardWindow,
                scrollDelta = delta,
                applyForwardPrefetch = delta <= 0.0f,
            )
        }
    }

    private fun CacheWindowScope.refillWindow(refillForward: Boolean) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(viewport) ?: 0 }

            onPrefetchForward(
                prefetchForwardWindow = prefetchForwardWindow,
                scrollDelta = 0.0f,
                applyForwardPrefetch = refillForward,
            )
        }
    }

    override fun resetStrategy() {
        val currentLaneCount = maxOf(1, laneCount())
        previousLaneCount = currentLaneCount
        if (perLaneCacheWindowStartIndex.size != currentLaneCount) {
            resizeCache(currentLaneCount)
        } else {
            perLaneCacheWindowStartIndex.fill(Int.MAX_VALUE)
            perLaneCacheWindowEndItemIndex.fill(Int.MIN_VALUE)
            perLaneCacheWindowStartSpace.fill(0)
            perLaneCacheWindowEndSpace.fill(0)
        }
        shouldRefillWindow = false

        windowCache.clear()
        windowCacheWithItems.clear()
        prefetchWindowHandles.removeIf { _, value ->
            value.fastForEach { it.cancel() }
            true
        }
    }

    private fun resizeCache(newLaneCount: Int) {
        perLaneCacheWindowStartIndex = IntArray(newLaneCount) { Int.MAX_VALUE }
        perLaneCacheWindowEndItemIndex = IntArray(newLaneCount) { Int.MIN_VALUE }
        perLaneCacheWindowStartSpace = IntArray(newLaneCount)
        perLaneCacheWindowEndSpace = IntArray(newLaneCount)
        perLaneFirstVisibleItemIndex = IntArray(newLaneCount)
        perLaneLastVisibleItemIndex = IntArray(newLaneCount)
        perLaneMainAxisExtraStartSpace = IntArray(newLaneCount)
        perLaneMainAxisExtraEndSpace = IntArray(newLaneCount)
        perLaneFirstNonVisibleItemIndex = IntArray(newLaneCount)
    }

    /**
     * Prefetch Forward Logic: Fill in the forward window with prefetched items from the previous
     * measure pass. If the item is not prefetched yet, schedule a prefetching for it. Once a
     * prefetch returns, we check if the window is filled and if not we schedule the next
     * prefetching.
     */
    private fun CacheWindowScope.onPrefetchForward(
        prefetchForwardWindow: Int,
        scrollDelta: Float,
        applyForwardPrefetch: Boolean,
    ) {
        val changedScrollDirection = scrollDelta.sign != previousPassDelta.sign

        if (applyForwardPrefetch) { // scrolling forward, starting on last visible
            updatePerLaneVisibleItemIndexes(perLaneLastVisibleItemIndex)
            perLaneLastVisibleItemIndex.forEachIndexed { lane, value ->
                perLaneFirstNonVisibleItemIndex[lane] = getNextEndItemIndexInLane(lane, value)
            }
            updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace)

            for (lane in 0 until currentLaneCount) {
                val remainingLaneSpace = prefetchForwardWindow - perLaneMainAxisExtraEndSpace[lane]
                if (changedScrollDirection || shouldRefillWindow) {
                    perLaneCacheWindowEndItemIndex[lane] = perLaneLastVisibleItemIndex[lane]
                    perLaneCacheWindowEndSpace[lane] = remainingLaneSpace
                } else {
                    perLaneCacheWindowEndSpace[lane] =
                        (perLaneCacheWindowEndSpace[lane] + scrollDelta.absoluteValue.roundToInt())
                            .coerceAtMost(remainingLaneSpace)
                }
            }

            var lane = InvalidIndex
            while (
                perLaneCacheWindowEndSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowEndSpace[lane] > 0
            ) {
                val finalIndexInLine = lastItemIndexInLine(perLaneCacheWindowEndItemIndex[lane])
                if (finalIndexInLine == InvalidIndex || finalIndexInLine >= itemsCount - 1) {
                    perLaneCacheWindowEndSpace[lane] = 0
                    continue
                }
                val itemIndexToPrefetch =
                    getNextEndItemIndexInLane(lane, perLaneCacheWindowEndItemIndex[lane])

                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    itemIndexToPrefetch == perLaneFirstNonVisibleItemIndex[lane] &&
                        scrollDelta != 0.0f &&
                        scrollDelta.absoluteValue >= perLaneMainAxisExtraEndSpace[lane]

                debugLog { "getItemSizeOrPrefetch item=$itemIndexToPrefetch isUrgent=$isUrgent" }
                // no more items available to fill prefetch window if this is null, break
                val itemSize =
                    getItemSizeOrPrefetch(
                        lane = lane,
                        isUrgent = isUrgent,
                        itemIndex = itemIndexToPrefetch,
                    )

                if (itemSize == InvalidItemSize) break

                updateEndCacheWindowsState(lane, itemIndexToPrefetch, itemSize)
            }
        } else { // scrolling backwards, starting on first visible
            updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex)
            perLaneFirstVisibleItemIndex.forEachIndexed { lane, itemIndex ->
                perLaneFirstNonVisibleItemIndex[lane] = getNextStartItemIndexInLane(lane, itemIndex)
            }
            updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace)

            for (lane in 0 until currentLaneCount) {
                val remainingLaneSpace =
                    prefetchForwardWindow - perLaneMainAxisExtraStartSpace[lane]
                if (changedScrollDirection || shouldRefillWindow) {
                    perLaneCacheWindowStartSpace[lane] = remainingLaneSpace
                    perLaneCacheWindowStartIndex[lane] = perLaneFirstVisibleItemIndex[lane]
                } else {
                    perLaneCacheWindowStartSpace[lane] =
                        (perLaneCacheWindowStartSpace[lane] +
                                scrollDelta.absoluteValue.roundToInt())
                            .coerceAtMost(remainingLaneSpace)
                }
            }

            var lane = InvalidIndex
            while (
                perLaneCacheWindowStartSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowStartSpace[lane] > 0
            ) {
                if (perLaneCacheWindowStartIndex[lane] <= 0) {
                    perLaneCacheWindowStartSpace[lane] = 0
                    continue
                }
                val itemIndexToPrefetch =
                    getNextStartItemIndexInLane(lane, perLaneCacheWindowStartIndex[lane])
                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    itemIndexToPrefetch == perLaneFirstNonVisibleItemIndex[lane] &&
                        scrollDelta != 0.0f &&
                        scrollDelta.absoluteValue >= perLaneMainAxisExtraStartSpace[lane]

                debugLog { "getItemSizeOrPrefetch item=$itemIndexToPrefetch isUrgent=$isUrgent" }

                // no more items available to fill prefetch window if this is null, break
                val laneSize =
                    getItemSizeOrPrefetch(
                        lane = lane,
                        isUrgent = isUrgent,
                        itemIndex = itemIndexToPrefetch,
                    )
                if (laneSize == InvalidItemSize) break

                updateStartCacheWindowsState(lane, itemIndexToPrefetch, laneSize)
            }
        }
    }

    /**
     * Keep Around Logic: Keep around items were visible in the previous measure pass. This means
     * that they will be present in [windowCache] along their size information. We loop through
     * items starting in the last visible one and update [perLaneCacheWindowStartSpace] or
     * [perLaneCacheWindowEndSpace] and also [perLaneCacheWindowStartIndex] or
     * [perLaneCacheWindowEndItemIndex]. We never schedule a prefetch call for keep around items.
     */
    private fun CacheWindowScope.onKeepAround(
        keepAroundWindow: Int,
        scrollDelta: Float,
        itemsCount: Int,
    ) {
        if (scrollDelta <= 0.0f) { // scrolling forward, keep around from firstVisible
            updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex)
            updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace)
            for (lane in 0 until currentLaneCount) {
                perLaneCacheWindowStartSpace[lane] =
                    (keepAroundWindow - perLaneMainAxisExtraStartSpace[lane])
                perLaneCacheWindowStartIndex[lane] = perLaneFirstVisibleItemIndex[lane]
            }

            var lane = InvalidIndex
            while (
                perLaneCacheWindowStartSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowStartSpace[lane] > 0
            ) {
                val nextStartCacheWindowIndex =
                    getNextStartItemIndexInLane(lane, perLaneCacheWindowStartIndex[lane])
                if (nextStartCacheWindowIndex == InvalidIndex) {
                    perLaneCacheWindowStartSpace[lane] = 0
                    continue
                }
                val itemSize =
                    if (windowCacheWithItems.containsKey(nextStartCacheWindowIndex)) {
                        windowCacheWithItems[nextStartCacheWindowIndex]!!.mainAxisSize
                    } else {
                        perLaneCacheWindowStartSpace[lane] = 0
                        continue
                    }

                updateStartCacheWindowsState(lane, nextStartCacheWindowIndex, itemSize)
            }
            removeOutOfBoundsItems(0, perLaneCacheWindowStartIndex.min() - 1)
        } else { // scrolling backwards, keep around from last visible
            updatePerLaneVisibleItemIndexes(perLaneLastVisibleItemIndex)
            updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace)
            for (lane in 0 until currentLaneCount) {
                perLaneCacheWindowEndSpace[lane] =
                    (keepAroundWindow - perLaneMainAxisExtraEndSpace[lane])
                perLaneCacheWindowEndItemIndex[lane] = perLaneLastVisibleItemIndex[lane]
            }

            var lane = InvalidIndex
            while (
                perLaneCacheWindowEndSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowEndSpace[lane] > 0
            ) {
                // If the current lane end index is the last index of the layout lane, we continue
                // onto the other lanes by zero-ing the lane's remaining space
                val nextEndCacheWindowItemIndex =
                    getNextEndItemIndexInLane(lane, perLaneCacheWindowEndItemIndex[lane])
                if (
                    nextEndCacheWindowItemIndex == InvalidIndex ||
                        lastItemIndexInLine(perLaneCacheWindowEndItemIndex[lane]) >= itemsCount - 1
                ) {
                    perLaneCacheWindowEndSpace[lane] = 0
                    continue
                }
                val itemSize =
                    if (windowCacheWithItems.containsKey(nextEndCacheWindowItemIndex)) {
                        windowCacheWithItems[nextEndCacheWindowItemIndex]!!.mainAxisSize
                    } else {
                        perLaneCacheWindowEndSpace[lane] = 0
                        continue
                    }
                updateEndCacheWindowsState(lane, nextEndCacheWindowItemIndex, itemSize)
            }
            removeOutOfBoundsItems(perLaneCacheWindowEndItemIndex.max() + 1, itemsCount - 1)
        }
    }

    private fun CacheWindowScope.getItemSizeOrPrefetch(
        lane: Int,
        isUrgent: Boolean,
        itemIndex: Int,
    ): Int {
        return if (windowCacheWithItems.containsKey(itemIndex)) {
            debugLog { "Item $itemIndex is Cached!" }
            windowCacheWithItems[itemIndex]!!.mainAxisSize
        } else if (prefetchWindowHandles.containsKey(itemIndex)) {
            // item is scheduled but didn't finish yet
            debugLog { "Item=$itemIndex is already scheduled. isUrgent=$isUrgent" }
            if (isUrgent) prefetchWindowHandles[itemIndex]?.fastForEach { it.markAsUrgent() }
            InvalidItemSize
        } else {
            // item is not scheduled
            debugLog { "Scheduling Prefetching for Item=$itemIndex. isUrgent=$isUrgent lane=$lane" }
            prefetchWindowHandles[itemIndex] =
                schedulePrefetch(lane, itemIndex) { itemSize ->
                    onItemPrefetched(lane, itemIndex, itemSize)
                }
            if (isUrgent) prefetchWindowHandles[itemIndex]?.fastForEach { it.markAsUrgent() }
            InvalidItemSize
        }
    }

    /** Grows the window with measured items and prefetched items. */
    private fun CacheWindowScope.cachePrefetchedItem(lane: Int, itemIndex: Int, size: Int) {
        windowCacheWithItems[itemIndex] =
            updateOrCreateCachedItem(itemIndex, size, CachedItem.NoKey)
        if (itemIndex > perLaneCacheWindowEndItemIndex[lane]) {
            updateEndCacheWindowsState(lane, itemIndex, size)
        } else if (itemIndex < perLaneCacheWindowStartIndex[lane]) {
            updateStartCacheWindowsState(lane, itemIndex, size)
        }
    }

    private fun updateOrCreateCachedItem(itemIndex: Int, itemSize: Int, key: Any): CachedItem {
        val cachedItem = windowCacheWithItems[itemIndex]
        return if (cachedItem != null) {
            cachedItem.mainAxisSize = itemSize
            cachedItem.key = key
            cachedItem
        } else {
            CachedItem(key, itemSize)
        }
    }

    /**
     * When caching visible items we need to check if the existing item changed sizes. If so, we
     * will set [shouldRefillWindow] which will trigger a complete window filling and cancel any out
     * of bounds requests. The same is valid if items are replaced (have the same size by key
     * changed).
     */
    private fun cacheVisibleItemsInfo(itemIndex: Int, key: Any, itemSize: Int, lane: Int) {
        debugLog { "cacheVisibleItemsInfo item=$itemIndex size=$itemSize key=$key" }
        if (windowCacheWithItems.containsKey(itemIndex)) {
            val cachedSize = windowCacheWithItems[itemIndex]!!.mainAxisSize
            val cachedKey = windowCacheWithItems[itemIndex]!!.key
            if (cachedSize != itemSize || cachedKey != key) {
                shouldRefillWindow = true
            }
        }

        windowCacheWithItems[itemIndex] = updateOrCreateCachedItem(itemIndex, itemSize, key)
        // We're caching a visible item, remove its handle since we won't need it anymore.
        perLaneCacheWindowStartIndex[lane] = minOf(perLaneCacheWindowStartIndex[lane], itemIndex)
        perLaneCacheWindowEndItemIndex[lane] =
            maxOf(perLaneCacheWindowEndItemIndex[lane], itemIndex)
        prefetchWindowHandles.remove(itemIndex)?.fastForEach { it.cancel() }
    }

    /** Takes care of removing caches and canceling handles for items that we won't use anymore. */
    private fun removeOutOfBoundsItems(startItemIndex: Int, endItemIndex: Int) {
        indicesToRemove.clear()
        prefetchWindowHandles.forEachKey {
            if (it in startItemIndex..endItemIndex) indicesToRemove.add(it)
        }

        windowCache.forEachKey { if (it in startItemIndex..endItemIndex) indicesToRemove.add(it) }
        windowCacheWithItems.forEachKey {
            if (it in startItemIndex..endItemIndex) indicesToRemove.add(it)
        }

        debugLog { "Indices to remove=$indicesToRemove" }

        indicesToRemove.forEach {
            prefetchWindowHandles.remove(it)?.fastForEach { handle -> handle.cancel() }
            windowCache.remove(it)
            windowCacheWithItems.remove(it)
        }
    }

    /**
     * Item prefetching finished, we can cache its information and schedule the next prefetching if
     * needed.
     */
    private fun CacheWindowScope.onItemPrefetched(lane: Int, itemIndex: Int, itemSize: Int) {
        debugLog { "onItemPrefetched lane=$lane item=$itemIndex size=$itemSize" }
        cachePrefetchedItem(lane, itemIndex, itemSize)
        scheduleNextItemIfNeeded()
        traceWindowInfo()
    }

    private fun CacheWindowScope.scheduleNextItemIfNeeded() {
        var nextPrefetchableItemIndex = InvalidIndex
        var lane = InvalidIndex
        // if was scrolling forward
        if (previousPassDelta.sign <= 0) {
            while (
                perLaneCacheWindowEndSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowEndSpace[lane] > 0
            ) {
                val nextIndex =
                    getNextEndItemIndexInLane(lane, perLaneCacheWindowEndItemIndex[lane])
                val finalIndexInLine = lastItemIndexInLine(nextIndex)
                if (finalIndexInLine == InvalidIndex || finalIndexInLine >= itemsCount) {
                    perLaneCacheWindowEndSpace[lane] = 0
                    continue
                }
                nextPrefetchableItemIndex = nextIndex
                break
            }
        } else {
            while (
                perLaneCacheWindowStartSpace.indexOfMaxValue().also { lane = it } != InvalidIndex &&
                    perLaneCacheWindowStartSpace[lane] > 0
            ) {
                if (perLaneCacheWindowStartIndex[lane] <= 0) {
                    perLaneCacheWindowStartSpace[lane] = 0
                    continue
                }
                val nextIndex =
                    getNextStartItemIndexInLane(lane, perLaneCacheWindowStartIndex[lane])
                val finalIndexInLine = lastItemIndexInLine(nextIndex)
                if (finalIndexInLine == InvalidIndex) {
                    perLaneCacheWindowStartSpace[lane] = 0
                    continue
                }
                nextPrefetchableItemIndex = nextIndex
                break
            }
        }

        debugLog { "nextPrefetchableItemIndex=$nextPrefetchableItemIndex" }

        if (nextPrefetchableItemIndex >= 0) {
            val nextPrefetchableItemIndex = nextPrefetchableItemIndex
            prefetchWindowHandles[nextPrefetchableItemIndex] =
                schedulePrefetch(lane, nextPrefetchableItemIndex) { mainAxisSize ->
                    onItemPrefetched(lane, nextPrefetchableItemIndex, mainAxisSize)
                }
        }
    }

    private fun CacheWindowScope.updateEndCacheWindowsState(
        lane: Int,
        itemIndex: Int,
        itemSize: Int,
    ) {
        if (currentLaneCount > 1 && isSpanLine(itemIndex)) {
            val minExtraSpace = perLaneCacheWindowEndSpace.minOrNull() ?: 0
            val newExtraSpace = minExtraSpace - itemSize

            for (lane in 0 until currentLaneCount) {
                perLaneCacheWindowEndItemIndex[lane] = itemIndex
                perLaneCacheWindowEndSpace[lane] = newExtraSpace
            }
        } else {
            perLaneCacheWindowEndItemIndex[lane] = itemIndex
            perLaneCacheWindowEndSpace[lane] -= itemSize
        }
    }

    private fun CacheWindowScope.updateStartCacheWindowsState(
        lane: Int,
        itemIndex: Int,
        itemSize: Int,
    ) {
        if (currentLaneCount > 1 && isSpanLine(itemIndex)) {
            val minExtraSpace = perLaneCacheWindowStartSpace.minOrNull() ?: 0
            val newExtraSpace = minExtraSpace - itemSize

            for (lane in 0 until currentLaneCount) {
                perLaneCacheWindowStartIndex[lane] = itemIndex
                perLaneCacheWindowStartSpace[lane] = newExtraSpace
            }
        } else {
            perLaneCacheWindowStartIndex[lane] = itemIndex
            perLaneCacheWindowStartSpace[lane] -= itemSize
        }
    }
}

/**
 * Legacy implementation of the logic for [LazyLayoutCacheWindow] prefetching and item preservation.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LegacyCacheWindowLogic(
    private val cacheWindow: LazyLayoutCacheWindow,
    private val enableInitialPrefetch: Boolean = true,
) : CacheWindowLogic {
    /** Temporary buffer to avoid array allocations. */
    private val extraSpaceBuffer = IntArray(1)
    /** Handles for prefetched items in the current forward window. */
    private val prefetchWindowHandles = mutableIntObjectMapOf<List<PrefetchHandle>>()

    private val indicesToRemove = mutableIntSetOf()

    /**
     * Cache for items sizes in the current window. Holds sizes for both visible and non-visible
     * items
     */
    private val windowCache = mutableIntIntMapOf()
    private val windowCacheWithItems = mutableIntObjectMapOf<CachedItem>()

    private var previousPassDelta = 0f
    private var previousPassItemCount = UnsetItemCount
    private var hasUpdatedVisibleItemsOnce = false

    /**
     * Indices for the start and end of the cache window. The items between
     * [prefetchWindowStartLine] and [prefetchWindowEndLine] can be:
     * 1) Visible.
     * 2) Cached.
     * 3) Scheduled for prefetching.
     * 4) Not scheduled yet.
     */
    internal var prefetchWindowStartLine = Int.MAX_VALUE
        private set

    internal var prefetchWindowEndLine = Int.MIN_VALUE
        private set

    /**
     * Keeps track of the "extra" space used. Extra space starts by being the amount of space
     * occupied by the first and last visible items outside of the viewport, that is, how much
     * they're "peeking" out of view. These values will be updated as we fill the cache window.
     */
    private var prefetchWindowStartExtraSpace = 0
    private var prefetchWindowEndExtraSpace = 0

    /**
     * Signals that we should run the window refilling loop from start. This might re-trigger a
     * prefetch in case the window is not filled with item information. There are 3 conditions in
     * which window refilling will happen:
     * 1) After the first layout pass
     * 2) If any of the visible items were resized since the last measure pass.
     * 3) If the total number of items changed since the last measure pass.
     */
    private var shouldRefillWindow = false

    /** Keep the latest item count where it can be used more easily. */
    private var itemsCount = 0

    private val startIndexArray = IntArray(1)
    private val endIndexArray = IntArray(1)

    override val perLaneCacheWindowStartIndex: IntArray
        get() {
            startIndexArray[0] = prefetchWindowStartLine
            return startIndexArray
        }

    override val perLaneCacheWindowEndItemIndex: IntArray
        get() {
            endIndexArray[0] = prefetchWindowEndLine
            return endIndexArray
        }

    override fun hasValidBounds(): Boolean =
        prefetchWindowStartLine != Int.MAX_VALUE && prefetchWindowEndLine != Int.MIN_VALUE

    override fun CacheWindowScope.onScroll(delta: Float) {
        debugLog { "delta=$delta" }
        traceWindowInfo()
        fillCacheWindowBackward(delta)
        fillCacheWindowForward(delta)
        previousPassDelta = delta
        traceWindowInfo()
        debugLog {
            "prefetchWindowStartExtraSpace=$prefetchWindowStartExtraSpace\n" +
                "prefetchWindowEndExtraSpace=$prefetchWindowEndExtraSpace\n" +
                "prefetchWindowStartIndex=$prefetchWindowStartLine\n" +
                "prefetchWindowEndIndex=$prefetchWindowEndLine"
        }
    }

    private fun traceWindowInfo() {
        traceValue("prefetchWindowStartExtraSpace", prefetchWindowStartExtraSpace.toLong())
        traceValue("prefetchWindowEndExtraSpace", prefetchWindowEndExtraSpace.toLong())
        traceValue("prefetchWindowStartIndex", prefetchWindowStartLine.toLong())
        traceValue("prefetchWindowEndIndex", prefetchWindowEndLine.toLong())
    }

    override fun CacheWindowScope.onVisibleItemsUpdated() {
        debugLog { "hasUpdatedVisibleItemsOnce=$hasUpdatedVisibleItemsOnce" }
        if (!hasUpdatedVisibleItemsOnce && enableInitialPrefetch) {
            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(mainAxisViewportSize) ?: 0 }
            // we won't fill the window if we don't have a prefetch window
            if (prefetchForwardWindow != 0) shouldRefillWindow = true
            hasUpdatedVisibleItemsOnce = true
        }

        /**
         * We already have information about the number of items from before and it actually
         * changed.
         */
        if (previousPassItemCount != UnsetItemCount && previousPassItemCount != totalItemsCount) {
            onDatasetChanged()
        }

        itemsCount = totalItemsCount
        // If visible items changed, update cached information. Any items that were visible
        // and became out of bounds will either count for the cache window or be cancelled/removed
        // by [cancelOutOfBounds]. If any items changed sizes we re-trigger the window filling
        // update.
        if (hasVisibleItems) {
            forEachVisibleItem { index, key, mainAxisSize, _ ->
                if (index != InvalidIndex) cacheVisibleItemsInfo(index, key, mainAxisSize)
            }
            if (shouldRefillWindow) {
                // refill window in accordance with last pass delta
                debugLog { "Refill Window Forward=${previousPassDelta <= 0.0f}" }
                refillWindow(previousPassDelta <= 0.0f)
                shouldRefillWindow = false
            }
        } else {
            // if no visible items, it means the dataset is empty and we should reset the window.
            // Next time visible items update we we re-start the window strategy.
            resetStrategy()
        }

        previousPassItemCount = totalItemsCount
    }

    private fun CacheWindowScope.onDatasetChanged() {
        debugLog { "Total Items Changed" }
        shouldRefillWindow = true
        if (hasVisibleItems) {
            prefetchWindowStartLine = prefetchWindowStartLine.coerceAtLeast(0)
            val lastLineIndex = getLastItemIndex()
            if (lastLineIndex != InvalidIndex) {
                prefetchWindowEndLine = prefetchWindowEndLine.coerceAtMost(lastLineIndex)
            }

            /**
             * Resets the window state. We will refill the window on the direction of the last
             * scroll.
             */
            if (previousPassDelta <= 0f) {
                removeOutOfBoundsItems(lastVisibleItemIndex, itemsCount - 1)
            } else {
                removeOutOfBoundsItems(0, firstVisibleItemIndex)
            }
        }
    }

    private fun CacheWindowScope.fillCacheWindowBackward(delta: Float) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val keepAroundWindow =
                with(cacheWindow) { density?.calculateBehindWindow(viewport) ?: 0 }

            // save latest item count
            itemsCount = totalItemsCount

            val startSpace = getMainAxisExtraSpaceStart()
            val endSpace = getMainAxisExtraSpaceEnd()

            debugLog {
                "fillCacheWindowBackward visibleWindowStart=$firstVisibleItemIndex \n" +
                    "visibleWindowEnd=$lastVisibleItemIndex \n" +
                    "keepAroundWindow=$keepAroundWindow \n" +
                    "mainAxisExtraSpaceStart=$startSpace \n" +
                    "mainAxisExtraSpaceEnd=$endSpace \n"
            }

            onKeepAround(
                visibleWindowStart = firstVisibleItemIndex,
                visibleWindowEnd = lastVisibleItemIndex,
                keepAroundWindow = keepAroundWindow,
                scrollDelta = delta,
                itemsCount = totalItemsCount,
                mainAxisExtraSpaceStart = startSpace,
                mainAxisExtraSpaceEnd = endSpace,
            )
        }
    }

    private fun CacheWindowScope.fillCacheWindowForward(delta: Float) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(viewport) ?: 0 }

            val startSpace = getMainAxisExtraSpaceStart()
            val endSpace = getMainAxisExtraSpaceEnd()

            debugLog {
                "fillCacheWindowForward visibleWindowStart=$firstVisibleItemIndex \n" +
                    "visibleWindowEnd=$lastVisibleItemIndex \n" +
                    "prefetchForwardWindow=$prefetchForwardWindow \n" +
                    "mainAxisExtraSpaceStart=$startSpace \n" +
                    "mainAxisExtraSpaceEnd=$endSpace \n"
            }

            onPrefetchForward(
                visibleWindowStart = firstVisibleItemIndex,
                visibleWindowEnd = lastVisibleItemIndex,
                prefetchForwardWindow = prefetchForwardWindow,
                scrollDelta = delta,
                mainAxisExtraSpaceStart = startSpace,
                mainAxisExtraSpaceEnd = endSpace,
                applyForwardPrefetch = delta <= 0.0f,
            )
        }
    }

    private fun CacheWindowScope.refillWindow(refillForward: Boolean) {
        if (hasVisibleItems) {
            val viewport = mainAxisViewportSize

            val prefetchForwardWindow =
                with(cacheWindow) { density?.calculateAheadWindow(viewport) ?: 0 }

            val startSpace = getMainAxisExtraSpaceStart()
            val endSpace = getMainAxisExtraSpaceEnd()

            onPrefetchForward(
                visibleWindowStart = firstVisibleItemIndex,
                visibleWindowEnd = lastVisibleItemIndex,
                prefetchForwardWindow = prefetchForwardWindow,
                scrollDelta = 0.0f,
                mainAxisExtraSpaceStart = startSpace,
                mainAxisExtraSpaceEnd = endSpace,
                applyForwardPrefetch = refillForward,
            )
        }
    }

    override fun resetStrategy() {
        prefetchWindowStartLine = Int.MAX_VALUE
        prefetchWindowEndLine = Int.MIN_VALUE
        prefetchWindowStartExtraSpace = 0
        prefetchWindowEndExtraSpace = 0
        shouldRefillWindow = false

        windowCache.clear()
        windowCacheWithItems.clear()
        prefetchWindowHandles.removeIf { _, value ->
            value.fastForEach { it.cancel() }
            true
        }
    }

    /**
     * Prefetch Forward Logic: Fill in the forward window with prefetched items from the previous
     * measure pass. If the item is not prefetched yet, schedule a prefetching for it. Once a
     * prefetch returns, we check if the window is filled and if not we schedule the next
     * prefetching.
     */
    private fun CacheWindowScope.onPrefetchForward(
        visibleWindowStart: Int,
        visibleWindowEnd: Int,
        prefetchForwardWindow: Int,
        mainAxisExtraSpaceEnd: Int,
        mainAxisExtraSpaceStart: Int,
        scrollDelta: Float,
        applyForwardPrefetch: Boolean,
    ) {
        val changedScrollDirection = scrollDelta.sign != previousPassDelta.sign

        if (applyForwardPrefetch) { // scrolling forward, starting on last visible
            if (changedScrollDirection || shouldRefillWindow) {
                prefetchWindowEndExtraSpace = (prefetchForwardWindow - mainAxisExtraSpaceEnd)
                prefetchWindowEndLine = visibleWindowEnd
            } else {
                prefetchWindowEndExtraSpace =
                    (prefetchWindowEndExtraSpace + scrollDelta.absoluteValue.roundToInt())
                        .coerceAtMost(prefetchForwardWindow - mainAxisExtraSpaceEnd)
            }

            while (
                prefetchWindowEndExtraSpace > 0 &&
                    lastItemIndexInLine(prefetchWindowEndLine) != InvalidIndex &&
                    lastItemIndexInLine(prefetchWindowEndLine) < itemsCount - 1
            ) {
                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    if (prefetchWindowEndLine + 1 == visibleWindowEnd + 1 && scrollDelta != 0.0f) {
                        scrollDelta.absoluteValue >= mainAxisExtraSpaceEnd
                    } else {
                        false
                    }

                debugLog {
                    "getItemSizeOrPrefetch item=${prefetchWindowEndLine + 1} isUrgent=$isUrgent"
                }
                // no more items available to fill prefetch window if this is null, break
                val itemSize =
                    getItemSizeOrPrefetch(index = prefetchWindowEndLine + 1, isUrgent = isUrgent)

                if (itemSize == InvalidItemSize) break

                prefetchWindowEndLine++
                prefetchWindowEndExtraSpace -= itemSize
            }
        } else { // scrolling backwards, starting on first visible
            if (changedScrollDirection || shouldRefillWindow) {
                prefetchWindowStartExtraSpace = (prefetchForwardWindow - mainAxisExtraSpaceStart)
                prefetchWindowStartLine = visibleWindowStart
            } else {
                prefetchWindowStartExtraSpace =
                    (prefetchWindowStartExtraSpace + scrollDelta.absoluteValue.roundToInt())
                        .coerceAtMost(prefetchForwardWindow - mainAxisExtraSpaceStart)
            }

            while (prefetchWindowStartExtraSpace > 0 && prefetchWindowStartLine > 0) {
                // If we get the same delta in the next frame, would we cover the extra space needed
                // to actually need this item? If so, mark it as urgent
                val isUrgent: Boolean =
                    if (
                        prefetchWindowStartLine - 1 == visibleWindowStart - 1 && scrollDelta != 0.0f
                    ) {
                        scrollDelta.absoluteValue >= mainAxisExtraSpaceStart
                    } else {
                        false
                    }

                debugLog {
                    "getItemSizeOrPrefetch item=${prefetchWindowEndLine + 1} isUrgent=$isUrgent"
                }

                // no more items available to fill prefetch window if this is null, break
                val itemSize =
                    getItemSizeOrPrefetch(index = prefetchWindowStartLine - 1, isUrgent = isUrgent)
                if (itemSize == InvalidItemSize) break
                prefetchWindowStartLine--
                prefetchWindowStartExtraSpace -= itemSize
            }
        }
    }

    /**
     * Keep Around Logic: Keep around items were visible in the previous measure pass. This means
     * that they will be present in [windowCache] along their size information. We loop through
     * items starting in the last visible one and update [prefetchWindowStartExtraSpace] or
     * [prefetchWindowEndExtraSpace] and also [prefetchWindowStartLine] or [prefetchWindowEndLine].
     * We never schedule a prefetch call for keep around items.
     */
    private fun onKeepAround(
        visibleWindowStart: Int,
        visibleWindowEnd: Int,
        mainAxisExtraSpaceEnd: Int,
        mainAxisExtraSpaceStart: Int,
        keepAroundWindow: Int,
        scrollDelta: Float,
        itemsCount: Int,
    ) {
        if (scrollDelta <= 0.0f) { // scrolling forward, keep around from firstVisible
            prefetchWindowStartExtraSpace = (keepAroundWindow - mainAxisExtraSpaceStart)
            prefetchWindowStartLine = visibleWindowStart
            while (prefetchWindowStartExtraSpace > 0 && prefetchWindowStartLine > 0) {
                val item =
                    if (windowCacheWithItems.containsKey(prefetchWindowStartLine - 1)) {
                        windowCacheWithItems[prefetchWindowStartLine - 1]!!.mainAxisSize
                    } else {
                        break
                    }

                prefetchWindowStartLine--
                prefetchWindowStartExtraSpace -= item
            }
            removeOutOfBoundsItems(0, prefetchWindowStartLine - 1)
        } else { // scrolling backwards, keep around from last visible
            prefetchWindowEndExtraSpace = (keepAroundWindow - mainAxisExtraSpaceEnd)
            prefetchWindowEndLine = visibleWindowEnd
            while (prefetchWindowEndExtraSpace > 0 && prefetchWindowEndLine < itemsCount - 1) {
                val item =
                    if (windowCacheWithItems.containsKey(prefetchWindowEndLine + 1)) {
                        windowCacheWithItems[prefetchWindowEndLine + 1]!!.mainAxisSize
                    } else {
                        break
                    }
                prefetchWindowEndLine++
                prefetchWindowEndExtraSpace -= item
            }
            removeOutOfBoundsItems(prefetchWindowEndLine + 1, itemsCount - 1)
        }
    }

    private fun CacheWindowScope.getItemSizeOrPrefetch(index: Int, isUrgent: Boolean): Int {
        return if (windowCacheWithItems.containsKey(index)) {
            debugLog { "Item $index is Cached!" }
            windowCacheWithItems[index]!!.mainAxisSize
        } else if (prefetchWindowHandles.containsKey(index)) {
            // item is scheduled but didn't finish yet
            debugLog { "Item=$index is already scheduled. isUrgent=$isUrgent" }
            if (isUrgent) prefetchWindowHandles[index]?.fastForEach { it.markAsUrgent() }
            InvalidItemSize
        } else {
            // item is not scheduled
            debugLog { "Scheduling Prefetching for Item=$index. isUrgent=$isUrgent" }
            prefetchWindowHandles[index] =
                schedulePrefetch(0, index) { size -> onItemPrefetched(index, size) }
            if (isUrgent) prefetchWindowHandles[index]?.fastForEach { it.markAsUrgent() }
            InvalidItemSize
        }
    }

    /** Grows the window with measured items and prefetched items. */
    private fun cachePrefetchedItem(index: Int, size: Int) {
        windowCacheWithItems[index] = updateOrCreateCachedItem(index, size, CachedItem.NoKey)
        if (index > prefetchWindowEndLine) {
            prefetchWindowEndLine = index
            prefetchWindowEndExtraSpace -= size
        } else if (index < prefetchWindowStartLine) {
            prefetchWindowStartLine = index
            prefetchWindowStartExtraSpace -= size
        }
    }

    private fun updateOrCreateCachedItem(index: Int, size: Int, key: Any): CachedItem {
        val cachedItem = windowCacheWithItems[index]
        return if (cachedItem != null) {
            cachedItem.mainAxisSize = size
            cachedItem.key = key
            cachedItem
        } else {
            CachedItem(key, size)
        }
    }

    /**
     * When caching visible items we need to check if the existing item changed sizes. If so, we
     * will set [shouldRefillWindow] which will trigger a complete window filling and cancel any out
     * of bounds requests. The same is valid if items are replaced (have the same size by key
     * changed).
     */
    private fun cacheVisibleItemsInfo(index: Int, key: Any, size: Int) {
        debugLog { "cacheVisibleItemsInfo item=$index size=$size key=$key" }
        if (windowCacheWithItems.containsKey(index)) {
            val cachedSize = windowCacheWithItems[index]!!.mainAxisSize
            val cachedKey = windowCacheWithItems[index]!!.key
            if (cachedSize != size || cachedKey != key) {
                shouldRefillWindow = true
            }
        }

        windowCacheWithItems[index] = updateOrCreateCachedItem(index, size, key)
        // We're caching a visible item, remove its handle since we won't need it anymore.
        prefetchWindowStartLine = minOf(prefetchWindowStartLine, index)
        prefetchWindowEndLine = maxOf(prefetchWindowEndLine, index)
        prefetchWindowHandles.remove(index)?.fastForEach { it.cancel() }
    }

    /** Takes care of removing caches and canceling handles for items that we won't use anymore. */
    private fun removeOutOfBoundsItems(startLine: Int, endLine: Int) {
        indicesToRemove.clear()
        prefetchWindowHandles.forEachKey { if (it in startLine..endLine) indicesToRemove.add(it) }

        windowCache.forEachKey { if (it in startLine..endLine) indicesToRemove.add(it) }
        windowCacheWithItems.forEachKey { if (it in startLine..endLine) indicesToRemove.add(it) }

        debugLog { "Indices to remove=$indicesToRemove" }

        indicesToRemove.forEach {
            prefetchWindowHandles.remove(it)?.fastForEach { it.cancel() }
            windowCache.remove(it)
            windowCacheWithItems.remove(it)
        }
    }

    /**
     * Item prefetching finished, we can cache its information and schedule the next prefetching if
     * needed.
     */
    private fun CacheWindowScope.onItemPrefetched(index: Int, itemSize: Int) {
        debugLog { "onItemPrefetched item=$index size=$itemSize" }
        cachePrefetchedItem(index, itemSize)
        scheduleNextItemIfNeeded()
        traceWindowInfo()
    }

    private fun CacheWindowScope.scheduleNextItemIfNeeded() {
        var nextPrefetchableLineIndex: Int = -1
        // if was scrolling forward
        if (previousPassDelta.sign <= 0) {
            if (prefetchWindowEndExtraSpace > 0)
                nextPrefetchableLineIndex = prefetchWindowEndLine + 1
        } else if (previousPassDelta.sign > 0) {
            if (prefetchWindowStartExtraSpace > 0)
                nextPrefetchableLineIndex = prefetchWindowStartLine - 1
        }

        debugLog { "nextPrefetchableLineIndex=$nextPrefetchableLineIndex" }

        if (
            nextPrefetchableLineIndex > 0 &&
                lastItemIndexInLine(nextPrefetchableLineIndex) != InvalidIndex &&
                lastItemIndexInLine(nextPrefetchableLineIndex) < itemsCount
        ) {
            prefetchWindowHandles[nextPrefetchableLineIndex] =
                schedulePrefetch(0, nextPrefetchableLineIndex) { mainAxisSize ->
                    onItemPrefetched(nextPrefetchableLineIndex, mainAxisSize)
                }
        }
    }

    private fun CacheWindowScope.getMainAxisExtraSpaceStart(): Int {
        updatePerLaneMainAxisExtraStartSpace(extraSpaceBuffer)
        return extraSpaceBuffer[0]
    }

    private fun CacheWindowScope.getMainAxisExtraSpaceEnd(): Int {
        updatePerLaneMainAxisExtraEndSpace(extraSpaceBuffer)
        return extraSpaceBuffer[0]
    }
}

private const val InvalidItemSize = -1
internal const val InvalidIndex = -1
private const val UnsetItemCount = -1

private const val DebugEnabled = false

private inline fun debugLog(generateMsg: () -> String) {
    if (DebugEnabled) {
        println("CacheWindowLogic: ${generateMsg()}")
    }
}

internal class CachedItem(var key: Any, var mainAxisSize: Int) {

    override fun toString(): String {
        return "CachedItem(key=$key, mainAxisSize=$mainAxisSize)"
    }

    companion object NoKey
}

private fun IntArray.indexOfMaxValue(): Int {
    var maxIndex = InvalidIndex
    var maxValue = Int.MIN_VALUE
    for (i in indices) {
        if (this[i] > maxValue) {
            maxValue = this[i]
            maxIndex = i
        }
    }
    return maxIndex
}
