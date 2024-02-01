/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutAnimateScrollScope
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridAnimateScrollScope(
    private val state: LazyGridState
) : LazyLayoutAnimateScrollScope {

    override val firstVisibleItemIndex: Int get() = state.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int get() = state.firstVisibleItemScrollOffset

    override val lastVisibleItemIndex: Int
        get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    override val itemCount: Int get() = state.layoutInfo.totalItemsCount

    override val visibleItemsAverageSize: Int
        get() = calculateLineAverageMainAxisSize(state.layoutInfo)

    override fun getVisibleItemScrollOffset(index: Int): Int {
        val layoutInfo = state.layoutInfo
        return layoutInfo.visibleItemsInfo
            .fastFirstOrNull {
                it.index == index
            }?.let { item ->
                if (layoutInfo.orientation == Orientation.Vertical) {
                    item.offset.y
                } else {
                    item.offset.x
                }
            } ?: 0
    }

    override fun ScrollScope.snapToItem(index: Int, scrollOffset: Int) {
        state.snapToItemIndexInternal(index, scrollOffset)
    }

    override fun calculateDistanceTo(targetIndex: Int, targetItemOffset: Int): Float {
        val slotsPerLine = state.slotsPerLine
        val averageLineMainAxisSize = visibleItemsAverageSize
        val before = targetIndex < firstVisibleItemIndex
        val linesDiff =
            (targetIndex - firstVisibleItemIndex + (slotsPerLine - 1) * if (before) -1 else 1) /
                slotsPerLine

        var coercedOffset = minOf(abs(targetItemOffset), averageLineMainAxisSize)
        if (targetItemOffset < 0) coercedOffset *= -1
        return (averageLineMainAxisSize * linesDiff).toFloat() +
            coercedOffset - firstVisibleItemScrollOffset
    }

    private fun calculateLineAverageMainAxisSize(
        layoutInfo: LazyGridLayoutInfo
    ): Int {
        val isVertical = layoutInfo.orientation == Orientation.Vertical
        val visibleItems = layoutInfo.visibleItemsInfo
        val lineOf: (Int) -> Int = {
            if (isVertical) visibleItems[it].row else visibleItems[it].column
        }

        var totalLinesMainAxisSize = 0
        var linesCount = 0

        var lineStartIndex = 0
        while (lineStartIndex < visibleItems.size) {
            val currentLine = lineOf(lineStartIndex)
            if (currentLine == -1) {
                // Filter out exiting items.
                ++lineStartIndex
                continue
            }

            var lineMainAxisSize = 0
            var lineEndIndex = lineStartIndex
            while (lineEndIndex < visibleItems.size && lineOf(lineEndIndex) == currentLine) {
                lineMainAxisSize = max(
                    lineMainAxisSize,
                    if (isVertical) {
                        visibleItems[lineEndIndex].size.height
                    } else {
                        visibleItems[lineEndIndex].size.width
                    }
                )
                ++lineEndIndex
            }

            totalLinesMainAxisSize += lineMainAxisSize
            ++linesCount

            lineStartIndex = lineEndIndex
        }

        return totalLinesMainAxisSize / linesCount + layoutInfo.mainAxisItemSpacing
    }

    override suspend fun scroll(block: suspend ScrollScope.() -> Unit) {
        state.scroll(block = block)
    }
}
