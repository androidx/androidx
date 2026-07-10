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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.CacheWindowScope
import androidx.compose.foundation.lazy.layout.CachedItem
import androidx.compose.foundation.lazy.layout.InvalidIndex
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.ui.unit.Density
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
internal class PagerCacheWindowLogic(
    val cacheWindow: LazyLayoutCacheWindow,
    val state: LazyLayoutPrefetchState,
    val itemCount: () -> Int,
) : CacheWindowLogic by CacheWindowLogic(cacheWindow, enableInitialPrefetch = false) {
    private val cacheWindowScope = PagerCacheWindowScope(itemCount)

    fun onScroll(delta: Float, layoutInfo: PagerMeasureResult) {
        /** Flip scroll sign */
        applyWindowScope(layoutInfo) { onScroll(-delta) }
    }

    fun onVisibleItemsChanged(layoutInfo: PagerMeasureResult) {
        applyWindowScope(layoutInfo) { onVisibleItemsUpdated() }
    }

    private inline fun applyWindowScope(
        layoutInfo: PagerMeasureResult,
        block: CacheWindowScope.() -> Unit,
    ) {
        cacheWindowScope.layoutInfo = layoutInfo
        cacheWindowScope.state = state
        block(cacheWindowScope)
    }
}

private class PagerCacheWindowScope(val itemCount: () -> Int) : CacheWindowScope {
    lateinit var layoutInfo: PagerMeasureResult
    lateinit var state: LazyLayoutPrefetchState

    override val totalItemsCount: Int
        get() = itemCount.invoke()

    override val hasVisibleItems: Boolean
        get() = layoutInfo.visiblePagesInfo.isNotEmpty()

    override val lastVisibleItemIndex: Int
        get() {
            if (layoutInfo.visiblePagesInfo.isEmpty()) return InvalidIndex
            val itemIndex =
                (layoutInfo.visiblePagesInfo.last().index.toLong() +
                    layoutInfo.beyondViewportPageCount.toLong())
            return itemIndex.coerceAtMost(totalItemsCount - 1L).toInt()
        }

    override val mainAxisViewportSize: Int
        get() = layoutInfo.mainAxisViewportSize

    override val density: Density
        get() = layoutInfo.density

    override val firstVisibleItemIndex: Int
        get() {
            if (layoutInfo.visiblePagesInfo.isEmpty()) return InvalidIndex
            val itemIndex =
                layoutInfo.visiblePagesInfo.first().index.toLong() -
                    layoutInfo.beyondViewportPageCount.toLong()
            return itemIndex.coerceAtLeast(0L).toInt()
        }

    override val visibleLineCount: Int
        get() =
            layoutInfo.extraPagesBefore.size +
                layoutInfo.visiblePagesInfo.size +
                layoutInfo.extraPagesAfter.size

    /**
     * For Pager, the "visible" area may be extended using beyondBoundsPageCount, but we still
     * consider extra space outside of the viewport as space that occupies the cache window area.
     */
    override fun updatePerLaneMainAxisExtraStartSpace(perLaneMainAxisExtraStartSpace: IntArray) {
        if (layoutInfo.visiblePagesInfo.isEmpty()) {
            perLaneMainAxisExtraStartSpace[0] = 0
            return
        }
        val firstVisibleItem = layoutInfo.visiblePagesInfo.first()
        // how much of the first item is peeking out of view at the start of the layout.
        val firstItemOverflowOffset =
            (firstVisibleItem.offset + layoutInfo.beforeContentPadding).coerceAtMost(0)
        // extra space is always positive in this context
        perLaneMainAxisExtraStartSpace[0] = firstItemOverflowOffset.absoluteValue
    }

    override fun updatePerLaneMainAxisExtraEndSpace(perLaneMainAxisExtraEndSpace: IntArray) {
        if (layoutInfo.visiblePagesInfo.isEmpty()) {
            perLaneMainAxisExtraEndSpace[0] = 0
            return
        }
        val lastVisibleItem = layoutInfo.visiblePagesInfo.last()
        // how much of the last item is peeking out of view at the end of the layout
        val lastItemOverflowOffset =
            lastVisibleItem.offset + layoutInfo.pageSize + layoutInfo.pageSpacing

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
        val childConstraints = layoutInfo.childConstraints

        return listOf(
            state.schedulePrecompositionAndPremeasure(itemIndex, childConstraints, true) {
                onItemPrefetched.invoke(layoutInfo.pageSize)
            }
        )
    }

    override fun getVisibleItemSize(indexInVisibleItems: Int): Int = layoutInfo.pageSize

    override fun getVisibleItemIndex(indexInVisibleItems: Int): Int {
        val extraPagesBeforeCount = layoutInfo.extraPagesBefore.size

        val visiblePagesCount = layoutInfo.visiblePagesInfo.size

        if (indexInVisibleItems < extraPagesBeforeCount) {
            return layoutInfo.extraPagesBefore[indexInVisibleItems].index
        }

        if (
            indexInVisibleItems >= extraPagesBeforeCount &&
                indexInVisibleItems < extraPagesBeforeCount + visiblePagesCount
        ) {
            return layoutInfo.visiblePagesInfo[indexInVisibleItems - extraPagesBeforeCount].index
        }

        if (indexInVisibleItems >= extraPagesBeforeCount + visiblePagesCount) {
            return layoutInfo.extraPagesAfter[
                    indexInVisibleItems - extraPagesBeforeCount - visiblePagesCount]
                .index
        }
        return InvalidIndex
    }

    override fun getVisibleItemKey(indexInVisibleItems: Int): Any {
        val extraPagesBeforeCount = layoutInfo.extraPagesBefore.size

        val visiblePagesCount = layoutInfo.visiblePagesInfo.size

        if (indexInVisibleItems < extraPagesBeforeCount) {
            return layoutInfo.extraPagesBefore[indexInVisibleItems].key
        }

        if (
            indexInVisibleItems >= extraPagesBeforeCount &&
                indexInVisibleItems < extraPagesBeforeCount + visiblePagesCount
        ) {
            return layoutInfo.visiblePagesInfo[indexInVisibleItems - extraPagesBeforeCount].key
        }

        if (indexInVisibleItems >= extraPagesBeforeCount + visiblePagesCount) {
            return layoutInfo.extraPagesAfter[
                    indexInVisibleItems - extraPagesBeforeCount - visiblePagesCount]
                .key
        }
        return CachedItem.NoKey
    }

    override fun getVisibleItemLane(indexInVisibleItems: Int) = 0

    override fun lastItemIndexInLine(currentItemIndex: Int): Int = currentItemIndex

    override fun getLastItemIndex(): Int {
        if (layoutInfo.visiblePagesInfo.isEmpty()) return InvalidIndex
        return totalItemsCount - 1
    }
}
