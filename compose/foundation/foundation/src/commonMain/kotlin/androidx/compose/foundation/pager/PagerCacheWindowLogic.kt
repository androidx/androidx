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
) : CacheWindowLogic(cacheWindow) {
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

    /**
     * For Pager, the "visible" area may be extended using beyondBoundsPageCount, but we still
     * consider extra space outside of the viewport as space that occupies the cache window area.
     */
    override val mainAxisExtraSpaceStart: Int
        get() {
            val firstVisibleItem = layoutInfo.visiblePagesInfo.first()
            // how much of the first item is peeking out of view at the start of the layout.
            val firstItemOverflowOffset =
                (firstVisibleItem.offset + layoutInfo.beforeContentPadding).coerceAtMost(0)
            // extra space is always positive in this context
            return firstItemOverflowOffset.absoluteValue
        }

    override val mainAxisExtraSpaceEnd: Int
        get() {

            val lastVisibleItem = layoutInfo.visiblePagesInfo.last()
            // how much of the last item is peeking out of view at the end of the layout
            val lastItemOverflowOffset =
                lastVisibleItem.offset + layoutInfo.pageSize + layoutInfo.pageSpacing

            // extra space is always positive in this context
            return (lastItemOverflowOffset - layoutInfo.viewportEndOffset).absoluteValue
        }

    override val firstVisibleLineIndex: Int
        get() =
            (layoutInfo.visiblePagesInfo.first().index - layoutInfo.beyondViewportPageCount)
                .coerceAtLeast(0)

    override val lastVisibleLineIndex: Int
        get() =
            (layoutInfo.visiblePagesInfo.last().index + layoutInfo.beyondViewportPageCount)
                .coerceAtMost(totalItemsCount - 1)

    override val mainAxisViewportSize: Int
        get() = layoutInfo.mainAxisViewportSize

    override val density: Density?
        get() = layoutInfo.density

    override fun schedulePrefetch(
        lineIndex: Int,
        onItemPrefetched: (Int, Int) -> Unit,
    ): List<PrefetchHandle> {
        val childConstraints = layoutInfo.childConstraints

        return listOf(
            state.schedulePrecompositionAndPremeasure(lineIndex, childConstraints, true) {
                onItemPrefetched.invoke(index, layoutInfo.pageSize)
            }
        )
    }

    override val visibleLineCount: Int
        get() =
            layoutInfo.extraPagesBefore.size +
                layoutInfo.visiblePagesInfo.size +
                layoutInfo.extraPagesAfter.size

    override fun getVisibleItemSize(indexInVisibleLines: Int): Int = layoutInfo.pageSize

    override fun getVisibleItemLine(indexInVisibleLines: Int): Int {
        val extraPagesBeforeCount = layoutInfo.extraPagesBefore.size

        val visiblePagesCount = layoutInfo.visiblePagesInfo.size

        if (indexInVisibleLines < extraPagesBeforeCount) {
            return layoutInfo.extraPagesBefore[indexInVisibleLines].index
        }

        if (
            indexInVisibleLines >= extraPagesBeforeCount &&
                indexInVisibleLines < extraPagesBeforeCount + visiblePagesCount
        ) {
            return layoutInfo.visiblePagesInfo[indexInVisibleLines - extraPagesBeforeCount].index
        }

        if (indexInVisibleLines >= extraPagesBeforeCount + visiblePagesCount) {
            return layoutInfo.extraPagesAfter[
                    indexInVisibleLines - extraPagesBeforeCount - visiblePagesCount]
                .index
        }
        return InvalidIndex
    }

    override fun getLastIndexInLine(lineIndex: Int): Int = lineIndex

    override fun getLastLineIndex(): Int {
        if (totalItemsCount == 0) return InvalidIndex
        return totalItemsCount - 1
    }
}
