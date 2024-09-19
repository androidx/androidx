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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.lazy.layout.LazyLayoutScrollScope
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.max

/**
 * An implementation of [LazyLayoutScrollScope] that can be used with LazyGrids.
 *
 * @param state The [LazyGridState] associated with the layout where this custom scroll should be
 *   performed.
 * @param scrollScope The base [ScrollScope] where the scroll session was created.
 * @return An implementation of [LazyLayoutScrollScope] that works with [LazyHorizontalGrid] and
 *   [LazyVerticalGrid].
 * @sample androidx.compose.foundation.samples.LazyGridCustomScrollUsingLazyLayoutScrollScopeSample
 */
fun LazyLayoutScrollScope(state: LazyGridState, scrollScope: ScrollScope): LazyLayoutScrollScope {
    return object : LazyLayoutScrollScope, ScrollScope by scrollScope {
        override val firstVisibleItemIndex: Int
            get() = state.firstVisibleItemIndex

        override val firstVisibleItemScrollOffset: Int
            get() = state.firstVisibleItemScrollOffset

        override val lastVisibleItemIndex: Int
            get() = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

        override val itemCount: Int
            get() = state.layoutInfo.totalItemsCount

        override fun snapToItem(index: Int, offset: Int) {
            state.snapToItemIndexInternal(index, offset, forceRemeasure = true)
        }

        override fun calculateDistanceTo(targetIndex: Int, targetOffset: Int): Int {
            val layoutInfo = state.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return 0
            return if (targetIndex !in firstVisibleItemIndex..lastVisibleItemIndex) {
                val slotsPerLine = state.slotsPerLine
                val averageLineMainAxisSize = calculateLineAverageMainAxisSize(layoutInfo)
                val before = targetIndex < firstVisibleItemIndex
                val linesDiff =
                    (targetIndex - firstVisibleItemIndex +
                        (slotsPerLine - 1) * if (before) -1 else 1) / slotsPerLine
                (averageLineMainAxisSize * linesDiff) - firstVisibleItemScrollOffset
            } else {
                val visibleItem =
                    layoutInfo.visibleItemsInfo.fastFirstOrNull { it.index == targetIndex }
                if (layoutInfo.orientation == Orientation.Vertical) {
                    visibleItem?.offset?.y
                } else {
                    visibleItem?.offset?.x
                } ?: 0
            } + targetOffset
        }

        private fun calculateLineAverageMainAxisSize(layoutInfo: LazyGridLayoutInfo): Int {
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
                    lineMainAxisSize =
                        max(
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
    }
}
