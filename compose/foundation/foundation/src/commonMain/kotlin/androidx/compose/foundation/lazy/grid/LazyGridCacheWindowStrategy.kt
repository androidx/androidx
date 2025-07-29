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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.offsetOnMainAxis
import androidx.compose.foundation.gestures.snapping.sizeOnMainAxis
import androidx.compose.foundation.lazy.layout.CacheWindowLogic
import androidx.compose.foundation.lazy.layout.CacheWindowScope
import androidx.compose.foundation.lazy.layout.InvalidIndex
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState.PrefetchHandle
import androidx.compose.foundation.lazy.layout.NestedPrefetchScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import kotlin.math.absoluteValue

@ExperimentalFoundationApi
internal class LazyGridCacheWindowPrefetchStrategy(cacheWindow: LazyLayoutCacheWindow) :
    CacheWindowLogic(cacheWindow), LazyGridPrefetchStrategy {
    private val cacheWindowScope = LazyGridCacheWindowScope()

    override fun LazyGridPrefetchScope.onScroll(delta: Float, layoutInfo: LazyGridLayoutInfo) {
        applyWindowScope(layoutInfo) { onScroll(delta) }
    }

    override fun LazyGridPrefetchScope.onVisibleItemsUpdated(layoutInfo: LazyGridLayoutInfo) {
        applyWindowScope(layoutInfo) { onVisibleItemsUpdated() }
    }

    override fun NestedPrefetchScope.onNestedPrefetch(firstVisibleItemIndex: Int) {
        repeat(nestedPrefetchItemCount) { schedulePrecomposition(firstVisibleItemIndex + it) }
    }

    /** Adapts the LazyGridPrefetchScope and LazyGridLayoutInfo to a single scope. */
    private inline fun LazyGridPrefetchScope.applyWindowScope(
        layoutInfo: LazyGridLayoutInfo,
        block: CacheWindowScope.() -> Unit,
    ) {
        cacheWindowScope.layoutInfo = layoutInfo
        cacheWindowScope.prefetchScope = this
        block(cacheWindowScope)
    }
}

@ExperimentalFoundationApi
private class LazyGridCacheWindowScope() : CacheWindowScope {
    lateinit var layoutInfo: LazyGridLayoutInfo
    lateinit var prefetchScope: LazyGridPrefetchScope

    override val totalItemsCount: Int
        get() = layoutInfo.totalItemsCount

    override val hasVisibleItems: Boolean
        get() = layoutInfo.visibleItemsInfo.isNotEmpty()

    override val mainAxisExtraSpaceStart: Int
        get() {
            val firstVisibleItem = layoutInfo.visibleItemsInfo.first()
            // how much of the first item is peeking out of view at the start of the layout.
            val firstItemOverflowOffset =
                (firstVisibleItem.offsetOnMainAxis(layoutInfo.orientation) +
                        layoutInfo.beforeContentPadding)
                    .coerceAtMost(0)
            // extra space is always positive in this context
            return firstItemOverflowOffset.absoluteValue
        }

    override val mainAxisExtraSpaceEnd: Int
        get() {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.last()
            // how much of the last item is peeking out of view at the end of the layout
            val lastItemOverflowOffset =
                lastVisibleItem.offsetOnMainAxis(layoutInfo.orientation) +
                    lastVisibleItem.sizeOnMainAxis(orientation = layoutInfo.orientation) +
                    layoutInfo.mainAxisItemSpacing

            // extra space is always positive in this context
            return (lastItemOverflowOffset - layoutInfo.viewportEndOffset).absoluteValue
        }

    override val firstVisibleLineIndex: Int
        get() {
            return layoutInfo.visibleItemsInfo.first().lineIndex
        }

    override val lastVisibleLineIndex: Int
        get() = layoutInfo.visibleItemsInfo.last().lineIndex

    override val mainAxisViewportSize: Int
        get() = layoutInfo.singleAxisViewportSize

    override val density: Density?
        get() = (layoutInfo as? LazyGridMeasureResult)?.density

    override fun schedulePrefetch(
        lineIndex: Int,
        onItemPrefetched: (Int, Int) -> Unit,
    ): List<PrefetchHandle> {
        return prefetchScope.scheduleLinePrefetch(lineIndex) {
            var tallestElement = Int.MIN_VALUE
            repeat(lineItemCount) { tallestElement = maxOf(getMainAxisSize(it)) }
            if (tallestElement != Int.MIN_VALUE) {
                onItemPrefetched(lineIndex, tallestElement)
            }
        }
    }

    override val visibleLineCount: Int
        get() = lastVisibleLineIndex - firstVisibleLineIndex + 1

    override fun getVisibleItemSize(indexInVisibleLines: Int): Int {
        val laneIndex = indexInVisibleLines + firstVisibleLineIndex
        var tallestItemSize = 0
        layoutInfo.visibleItemsInfo
            .fastFilter { it.lineIndex == laneIndex }
            .fastForEach {
                tallestItemSize =
                    maxOf(it.sizeOnMainAxis(orientation = layoutInfo.orientation), tallestItemSize)
            }

        return tallestItemSize
    }

    override fun getVisibleItemLine(indexInVisibleLines: Int): Int =
        firstVisibleLineIndex + indexInVisibleLines

    val LazyGridItemInfo.lineIndex: Int
        get() = lineIndex(layoutInfo.orientation)

    override fun getLastIndexInLine(lineIndex: Int): Int {
        val measureResult = layoutInfo as? LazyGridMeasureResult ?: return InvalidIndex
        val itemsInLine = measureResult.prefetchInfoRetriever.invoke(lineIndex)
        return if (itemsInLine.isEmpty()) {
            InvalidIndex
        } else {
            // the first index in this line plus the number of items in this line
            // gives me the last index in this line
            itemsInLine.first().first + itemsInLine.size - 1
        }
    }

    override fun getLastLineIndex(): Int {
        val measureResult = layoutInfo as? LazyGridMeasureResult ?: return InvalidIndex
        if (totalItemsCount == 0) return InvalidIndex
        return measureResult.lineIndexProvider.invoke(totalItemsCount - 1)
    }
}
