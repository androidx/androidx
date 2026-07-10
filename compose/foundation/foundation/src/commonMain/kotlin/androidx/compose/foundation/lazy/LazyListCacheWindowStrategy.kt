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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.singleAxisViewportSize
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.CacheWindowScope
import androidx.compose.foundation.lazy.layout.InvalidIndex
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.ui.unit.Density
import kotlin.math.absoluteValue

/**
 * This is a transition class based on [androidx.compose.foundation.lazy.LazyListPrefetchStrategy]
 * where we will perform a window based prefetching for items in the direction of the scroll
 * movement (ahead).
 */
@OptIn(ExperimentalFoundationApi::class)
internal class LazyListCacheWindowStrategy(cacheWindow: LazyLayoutCacheWindow) :
    LazyListPrefetchStrategy, CacheWindowLogic by CacheWindowLogic(cacheWindow) {
    private val cacheWindowScope = LazyListCacheWindowScope()

    override fun LazyListPrefetchScope.onScroll(delta: Float, layoutInfo: LazyListLayoutInfo) {
        applyWindowScope(layoutInfo) { onScroll(delta) }
    }

    override fun LazyListPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyListLayoutInfo) {
        applyWindowScope(layoutInfo) { onVisibleItemsUpdated() }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        val resolvedNestedPrefetchItemCount =
            if (nestedPrefetchItemCount == -1) {
                DefaultNestedPrefetchCount
            } else {
                nestedPrefetchItemCount
            }
        repeat(resolvedNestedPrefetchItemCount) {
            schedulePrecomposition(firstVisibleItemIndex + it)
        }
    }

    /** Adapts the LazyListPrefetchScope and LazyListLayoutInfo to a single scope. */
    private inline fun LazyListPrefetchScope.applyWindowScope(
        layoutInfo: LazyListLayoutInfo,
        block: CacheWindowScope.() -> Unit,
    ) {
        cacheWindowScope.layoutInfo = layoutInfo
        cacheWindowScope.prefetchScope = this
        block(cacheWindowScope)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyListCacheWindowScope : CacheWindowScope {
    lateinit var layoutInfo: LazyListLayoutInfo
    lateinit var prefetchScope: LazyListPrefetchScope

    override val totalItemsCount: Int
        get() = layoutInfo.totalItemsCount

    override val hasVisibleItems: Boolean
        get() = layoutInfo.visibleItemsInfo.isNotEmpty()

    override val firstVisibleItemIndex: Int
        get() = layoutInfo.visibleItemsInfo.first().index

    override val lastVisibleItemIndex: Int
        get() = layoutInfo.visibleItemsInfo.last().index

    override val mainAxisViewportSize: Int
        get() = layoutInfo.singleAxisViewportSize

    override val density: Density?
        get() = (layoutInfo as? LazyListMeasureResult)?.density

    override val visibleLineCount: Int
        get() = layoutInfo.visibleItemsInfo.size

    override fun updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace: IntArray) {
        val firstVisibleItem = layoutInfo.visibleItemsInfo.first()
        // how much of the first item is peeking out of view at the start of the layout.
        val firstItemOverflowOffset =
            (firstVisibleItem.offset + layoutInfo.beforeContentPadding).coerceAtMost(0)
        // extra space is always positive in this context
        perLaneMainAxisExtraStartSpace[0] = firstItemOverflowOffset.absoluteValue
    }

    override fun updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace: IntArray) {
        val lastVisibleItem = layoutInfo.visibleItemsInfo.last()
        // how much of the last item is peeking out of view at the end of the layout
        val lastItemOverflowOffset =
            lastVisibleItem.offset + lastVisibleItem.size + layoutInfo.mainAxisItemSpacing

        // extra space is always positive in this context
        perLaneMainAxisExtraEndSpace[0] =
            (lastItemOverflowOffset - layoutInfo.viewportEndOffset).absoluteValue
    }

    override fun updatePerLaneFirstVisibleItemIndex(perLaneFirstVisibleItemIndex: IntArray) {
        perLaneFirstVisibleItemIndex[0] = firstVisibleItemIndex
    }

    override fun updatePerLaneVisibleItemIndexes(perLaneVisibleItemIndexes: IntArray) {
        perLaneVisibleItemIndexes[0] = lastVisibleItemIndex
    }

    override fun schedulePrefetch(
        lane: Int,
        itemIndex: Int,
        onItemPrefetched: (itemSize: Int) -> Unit,
    ): List<PrefetchHandle> {
        return listOf(prefetchScope.schedulePrefetch(itemIndex) { onItemPrefetched(mainAxisSize) })
    }

    override fun getVisibleItemSize(indexInVisibleItems: Int): Int =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].size

    override fun getVisibleItemIndex(indexInVisibleItems: Int): Int =
        layoutInfo.visibleItemsInfo[indexInVisibleItems].index

    override fun lastItemIndexInLine(currentItemIndex: Int): Int = currentItemIndex

    override fun getVisibleItemKey(indexInVisibleItems: Int): Any {
        return layoutInfo.visibleItemsInfo[indexInVisibleItems].key
    }

    override fun getVisibleItemLane(indexInVisibleItems: Int): Int = 0

    override fun getLastItemIndex(): Int {
        if (totalItemsCount == 0) return InvalidIndex
        return totalItemsCount - 1
    }
}

// we use 2 here because nested list have usually > 1 visible elements, so 2 is the minimum
// logical value we could use.
private const val DefaultNestedPrefetchCount = 2
