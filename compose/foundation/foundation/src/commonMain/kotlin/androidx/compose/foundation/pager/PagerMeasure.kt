/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.compose.foundation.fastFilter
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutMeasureScope.measurePager(
    pageCount: Int,
    pagerItemProvider: PagerLazyLayoutItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    spaceBetweenPages: Int,
    firstVisiblePage: Int,
    firstVisiblePageOffset: Int,
    scrollToBeConsumed: Float,
    constraints: Constraints,
    orientation: Orientation,
    verticalAlignment: Alignment.Vertical?,
    horizontalAlignment: Alignment.Horizontal?,
    reverseLayout: Boolean,
    visualPageOffset: IntOffset,
    pageAvailableSize: Int,
    beyondBoundsPageCount: Int,
    pinnedPages: List<Int>,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult
): PagerMeasureResult {
    require(beforeContentPadding >= 0)
    require(afterContentPadding >= 0)
    val pageSizeWithSpacing = (pageAvailableSize + spaceBetweenPages).coerceAtLeast(0)
    debugLog { "Remeasuring..." }
    return if (pageCount <= 0) {
        PagerMeasureResult(
            visiblePagesInfo = emptyList(),
            pagesCount = 0,
            pageSize = pageAvailableSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            orientation = orientation,
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            measureResult = layout(constraints.minWidth, constraints.minHeight) {},
            consumedScroll = 0f,
            closestPageToSnapPosition = null,
            firstVisiblePage = null,
            firstVisiblePageOffset = 0,
            reverseLayout = false,
            canScrollForward = false
        )
    } else {

        val childConstraints = Constraints(
            maxWidth = if (orientation == Orientation.Vertical) {
                constraints.maxWidth
            } else {
                pageAvailableSize
            },
            maxHeight = if (orientation != Orientation.Vertical) {
                constraints.maxHeight
            } else {
                pageAvailableSize
            }
        )

        var currentFirstPage = firstVisiblePage
        var currentFirstPageScrollOffset = firstVisiblePageOffset
        if (currentFirstPage >= pageCount) {
            // the data set has been updated and now we have less pages that we were
            // scrolled to before
            currentFirstPage = pageCount - 1
            currentFirstPageScrollOffset = 0
        }

        // represents the real amount of scroll we applied as a result of this measure pass.
        var scrollDelta = scrollToBeConsumed.roundToInt()

        // applying the whole requested scroll offset. we will figure out if we can't consume
        // all of it later
        currentFirstPageScrollOffset -= scrollDelta

        // if the current scroll offset is less than minimally possible
        if (currentFirstPage == 0 && currentFirstPageScrollOffset < 0) {
            scrollDelta += currentFirstPageScrollOffset
            currentFirstPageScrollOffset = 0
        }

        // this will contain all the measured pages representing the visible pages
        val visiblePages = mutableListOf<MeasuredPage>()

        // define min and max offsets
        val minOffset = -beforeContentPadding + if (spaceBetweenPages < 0) spaceBetweenPages else 0
        val maxOffset = mainAxisAvailableSize

        // include the start padding so we compose pages in the padding area and neutralise page
        // spacing (if the spacing is negative this will make sure the previous page is composed)
        // before starting scrolling forward we will remove it back
        currentFirstPageScrollOffset += minOffset

        // max of cross axis sizes of all visible pages
        var maxCrossAxis = 0

        // we had scrolled backward or we compose pages in the start padding area, which means
        // pages before current firstPageScrollOffset should be visible. compose them and update
        // firstPageScrollOffset
        while (currentFirstPageScrollOffset < 0 && currentFirstPage > 0) {
            val previous = currentFirstPage - 1
            val measuredPage = getAndMeasure(
                index = previous,
                childConstraints = childConstraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                afterContentPadding = afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                pageAvailableSize = pageAvailableSize
            )
            visiblePages.add(0, measuredPage)
            maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
            currentFirstPageScrollOffset += pageSizeWithSpacing
            currentFirstPage = previous
        }

        // if we were scrolled backward, but there were not enough pages before. this means
        // not the whole scroll was consumed
        if (currentFirstPageScrollOffset < minOffset) {
            scrollDelta += currentFirstPageScrollOffset
            currentFirstPageScrollOffset = minOffset
        }

        // neutralize previously added padding as we stopped filling the before content padding
        currentFirstPageScrollOffset -= minOffset

        var index = currentFirstPage
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstPageScrollOffset

        // first we need to skip pages we already composed while composing backward
        visiblePages.fastForEach {
            index++
            currentMainAxisOffset += pageSizeWithSpacing
        }

        // then composing visible pages forward until we fill the whole viewport.
        // we want to have at least one page in visiblePages even if in fact all the pages are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (index < pageCount &&
            (currentMainAxisOffset < maxMainAxis ||
                currentMainAxisOffset <= 0 || // filling beforeContentPadding area
                visiblePages.isEmpty())
        ) {
            val measuredPage = getAndMeasure(
                index = index,
                childConstraints = childConstraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                afterContentPadding = afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                pageAvailableSize = pageAvailableSize
            )
            currentMainAxisOffset += pageSizeWithSpacing

            if (currentMainAxisOffset <= minOffset && index != pageCount - 1) {
                // this page is offscreen and will not be placed. advance firstVisiblePage
                currentFirstPage = index + 1
                currentFirstPageScrollOffset -= pageSizeWithSpacing
            } else {
                maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
                visiblePages.add(measuredPage)
            }

            index++
        }

        // we didn't fill the whole viewport with pages starting from firstVisiblePage.
        // lets try to scroll back if we have enough pages before firstVisiblePage.
        if (currentMainAxisOffset < maxOffset) {
            val toScrollBack = maxOffset - currentMainAxisOffset
            currentFirstPageScrollOffset -= toScrollBack
            currentMainAxisOffset += toScrollBack
            while (currentFirstPageScrollOffset < beforeContentPadding &&
                currentFirstPage > 0
            ) {
                val previousIndex = currentFirstPage - 1
                val measuredPage = getAndMeasure(
                    index = previousIndex,
                    childConstraints = childConstraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    afterContentPadding = afterContentPadding,
                    beforeContentPadding = beforeContentPadding,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    pageAvailableSize = pageAvailableSize
                )
                visiblePages.add(0, measuredPage)
                maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
                currentFirstPageScrollOffset += pageSizeWithSpacing
                currentFirstPage = previousIndex
            }
            scrollDelta += toScrollBack
            if (currentFirstPageScrollOffset < 0) {
                scrollDelta += currentFirstPageScrollOffset
                currentMainAxisOffset += currentFirstPageScrollOffset
                currentFirstPageScrollOffset = 0
            }
        }

        // report the amount of pixels we consumed. scrollDelta can be smaller than
        // scrollToBeConsumed if there were not enough pages to fill the offered space or it
        // can be larger if pages were resized, or if, for example, we were previously
        // displaying the page 15, but now we have only 10 pages in total in the data set.
        val consumedScroll = if (scrollToBeConsumed.roundToInt().sign == scrollDelta.sign &&
            abs(scrollToBeConsumed.roundToInt()) >= abs(scrollDelta)
        ) {
            scrollDelta.toFloat()
        } else {
            scrollToBeConsumed
        }

        // the initial offset for pages from visiblePages list
        require(currentFirstPageScrollOffset >= 0)
        val visiblePagesScrollOffset = -currentFirstPageScrollOffset
        var firstPage = visiblePages.first()

        // even if we compose pages to fill before content padding we should ignore pages fully
        // located there for the state's scroll position calculation (first page + first offset)
        if (beforeContentPadding > 0 || spaceBetweenPages < 0) {
            for (i in visiblePages.indices) {
                val size = pageSizeWithSpacing
                if (currentFirstPageScrollOffset != 0 && size <= currentFirstPageScrollOffset &&
                    i != visiblePages.lastIndex
                ) {
                    currentFirstPageScrollOffset -= size
                    firstPage = visiblePages[i + 1]
                } else {
                    break
                }
            }
        }

        // Compose extra pages before
        val extraPagesBefore = createPagesBeforeList(
            currentFirstPage = currentFirstPage,
            beyondBoundsPageCount = beyondBoundsPageCount,
            pinnedPages = pinnedPages
        ) {
            getAndMeasure(
                index = it,
                childConstraints = childConstraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                afterContentPadding = afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                pageAvailableSize = pageAvailableSize
            )
        }

        // Update maxCrossAxis with extra pages
        extraPagesBefore.fastForEach {
            maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize)
        }

        // Compose pages after last page
        val extraPagesAfter = createPagesAfterList(
            currentLastPage = visiblePages.last().index,
            pagesCount = pageCount,
            beyondBoundsPageCount = beyondBoundsPageCount,
            pinnedPages = pinnedPages
        ) {
            getAndMeasure(
                index = it,
                childConstraints = childConstraints,
                pagerItemProvider = pagerItemProvider,
                visualPageOffset = visualPageOffset,
                orientation = orientation,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                afterContentPadding = afterContentPadding,
                beforeContentPadding = beforeContentPadding,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                pageAvailableSize = pageAvailableSize
            )
        }

        // Update maxCrossAxis with extra pages
        extraPagesAfter.fastForEach {
            maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize)
        }

        val noExtraPages = firstPage == visiblePages.first() &&
            extraPagesBefore.isEmpty() &&
            extraPagesAfter.isEmpty()

        val layoutWidth = constraints
            .constrainWidth(
                if (orientation == Orientation.Vertical)
                    maxCrossAxis
                else
                    currentMainAxisOffset
            )
        val layoutHeight = constraints
            .constrainHeight(
                if (orientation == Orientation.Vertical)
                    currentMainAxisOffset
                else
                    maxCrossAxis
            )

        val positionedPages = calculatePagesOffsets(
            pages = visiblePages,
            extraPagesBefore = extraPagesBefore,
            extraPagesAfter = extraPagesAfter,
            layoutWidth = layoutWidth,
            layoutHeight = layoutHeight,
            finalMainAxisOffset = currentMainAxisOffset,
            maxOffset = maxOffset,
            pagesScrollOffset = visiblePagesScrollOffset,
            orientation = orientation,
            reverseLayout = reverseLayout,
            density = this,
            pageAvailableSize = pageAvailableSize,
            spaceBetweenPages = spaceBetweenPages
        )

        val visiblePagesInfo = if (noExtraPages) positionedPages else positionedPages.fastFilter {
            (it.index >= visiblePages.first().index && it.index <= visiblePages.last().index)
        }
        val viewPortSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth

        val closestPageToSnapPosition = visiblePagesInfo.fastMaxBy {
            -abs(
                calculateDistanceToDesiredSnapPosition(
                    viewPortSize,
                    beforeContentPadding,
                    afterContentPadding,
                    pageAvailableSize,
                    it,
                    SnapAlignmentStartToStart
                )
            )
        }

        return PagerMeasureResult(
            firstVisiblePage = firstPage,
            firstVisiblePageOffset = currentFirstPageScrollOffset,
            closestPageToSnapPosition = closestPageToSnapPosition,
            consumedScroll = consumedScroll,
            measureResult = layout(layoutWidth, layoutHeight) {
                positionedPages.fastForEach {
                    it.place(this)
                }
            },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = maxOffset + afterContentPadding,
            visiblePagesInfo = visiblePagesInfo,
            pagesCount = pageCount,
            reverseLayout = reverseLayout,
            orientation = orientation,
            pageSize = pageAvailableSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            canScrollForward = index < pageCount || currentMainAxisOffset > maxOffset
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Density.calculateDistanceToDesiredSnapPosition(
    axisViewPortSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    pageSize: Int,
    page: PageInfo,
    positionInLayout: Density.(layoutSize: Float, itemSize: Float) -> Float
): Float {
    val containerSize = axisViewPortSize - beforeContentPadding - afterContentPadding

    val desiredDistance =
        positionInLayout(containerSize.toFloat(), pageSize.toFloat())

    val itemCurrentPosition = page.offset
    return itemCurrentPosition - desiredDistance
}

private fun createPagesAfterList(
    currentLastPage: Int,
    pagesCount: Int,
    beyondBoundsPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null

    val end = minOf(currentLastPage + beyondBoundsPageCount, pagesCount - 1)

    fun addPage(index: Int) {
        if (list == null) list = mutableListOf()
        requireNotNull(list).add(getAndMeasure(index))
    }

    for (i in currentLastPage + 1..end) {
        addPage(i)
    }

    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex in (end + 1) until pagesCount) {
            addPage(pageIndex)
        }
    }

    return list ?: emptyList()
}

private fun createPagesBeforeList(
    currentFirstPage: Int,
    beyondBoundsPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null

    val start = maxOf(0, currentFirstPage - beyondBoundsPageCount)

    fun addPage(index: Int) {
        if (list == null) list = mutableListOf()
        requireNotNull(list).add(
            getAndMeasure(index)
        )
    }

    for (i in currentFirstPage - 1 downTo start) {
        addPage(i)
    }

    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex < start) {
            addPage(pageIndex)
        }
    }

    return list ?: emptyList()
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.getAndMeasure(
    index: Int,
    childConstraints: Constraints,
    pagerItemProvider: PagerLazyLayoutItemProvider,
    visualPageOffset: IntOffset,
    orientation: Orientation,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    afterContentPadding: Int,
    beforeContentPadding: Int,
    layoutDirection: LayoutDirection,
    reverseLayout: Boolean,
    pageAvailableSize: Int
): MeasuredPage {
    val key = pagerItemProvider.getKey(index)
    val placeable = measure(index, childConstraints)

    return MeasuredPage(
        index = index,
        placeables = placeable,
        visualOffset = visualPageOffset,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        afterContentPadding = afterContentPadding,
        beforeContentPadding = beforeContentPadding,
        layoutDirection = layoutDirection,
        reverseLayout = reverseLayout,
        size = pageAvailableSize,
        orientation = orientation,
        key = key
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyLayoutMeasureScope.calculatePagesOffsets(
    pages: List<MeasuredPage>,
    extraPagesBefore: List<MeasuredPage>,
    extraPagesAfter: List<MeasuredPage>,
    layoutWidth: Int,
    layoutHeight: Int,
    finalMainAxisOffset: Int,
    maxOffset: Int,
    pagesScrollOffset: Int,
    orientation: Orientation,
    reverseLayout: Boolean,
    density: Density,
    spaceBetweenPages: Int,
    pageAvailableSize: Int
): MutableList<PositionedPage> {
    val pageSizeWithSpacing = (pageAvailableSize + spaceBetweenPages)
    val mainAxisLayoutSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < minOf(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        check(pagesScrollOffset == 0)
    }
    val positionedPages =
        ArrayList<PositionedPage>(pages.size + extraPagesBefore.size + extraPagesAfter.size)

    if (hasSpareSpace) {
        require(extraPagesBefore.isEmpty() && extraPagesAfter.isEmpty())

        val pagesCount = pages.size
        fun Int.reverseAware() =
            if (!reverseLayout) this else pagesCount - this - 1

        val sizes = IntArray(pagesCount) { pageAvailableSize }
        val offsets = IntArray(pagesCount) { 0 }

        val arrangement = spacedBy(pageAvailableSize.toDp())
        if (orientation == Orientation.Vertical) {
            with(arrangement) { density.arrange(mainAxisLayoutSize, sizes, offsets) }
        } else {
            with(arrangement) {
                // Enforces Ltr layout direction as it is mirrored with placeRelative later.
                density.arrange(mainAxisLayoutSize, sizes, LayoutDirection.Ltr, offsets)
            }
        }

        val reverseAwareOffsetIndices =
            if (!reverseLayout) offsets.indices else offsets.indices.reversed()
        for (index in reverseAwareOffsetIndices) {
            val absoluteOffset = offsets[index]
            // when reverseLayout == true, offsets are stored in the reversed order to pages
            val page = pages[index.reverseAware()]
            val relativeOffset = if (reverseLayout) {
                // inverse offset to align with scroll direction for positioning
                mainAxisLayoutSize - absoluteOffset - page.size
            } else {
                absoluteOffset
            }
            positionedPages.add(page.position(relativeOffset, layoutWidth, layoutHeight))
        }
    } else {
        var currentMainAxis = pagesScrollOffset
        extraPagesBefore.fastForEach {
            currentMainAxis -= pageSizeWithSpacing
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
        }

        currentMainAxis = pagesScrollOffset
        pages.fastForEach {
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += pageSizeWithSpacing
        }

        extraPagesAfter.fastForEach {
            positionedPages.add(it.position(currentMainAxis, layoutWidth, layoutHeight))
            currentMainAxis += pageSizeWithSpacing
        }
    }
    return positionedPages
}

private const val DEBUG = false
private inline fun debugLog(generateMsg: () -> String) {
    if (DEBUG) {
        println("PagerMeasure: ${generateMsg()}")
    }
}