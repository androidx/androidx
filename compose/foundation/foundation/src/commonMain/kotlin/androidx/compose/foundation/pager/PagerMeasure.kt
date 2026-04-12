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

import androidx.collection.MutableIntObjectMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.calculateDistanceToDesiredSnapPosition
import androidx.compose.foundation.internal.checkPrecondition
import androidx.compose.foundation.internal.requirePrecondition
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.ObservableScopeInvalidator
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMaxBy
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyLayoutMeasureScope.measurePager(
    pageCount: Int,
    pagerItemProvider: PagerLazyLayoutItemProvider,
    mainAxisAvailableSize: Int,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    spaceBetweenPages: Int,
    currentPage: Int,
    currentPageOffset: Int,
    constraints: Constraints,
    orientation: Orientation,
    verticalAlignment: Alignment.Vertical?,
    horizontalAlignment: Alignment.Horizontal?,
    reverseLayout: Boolean,
    visualPageOffset: IntOffset,
    pageAvailableSize: Int,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    snapPosition: SnapPosition,
    placementScopeInvalidator: ObservableScopeInvalidator,
    coroutineScope: CoroutineScope,
    density: Density,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
    placeablesCache: MutableIntObjectMap<List<Placeable>>,
): PagerMeasureResult {
    requirePrecondition(beforeContentPadding >= 0) { "negative beforeContentPadding" }
    requirePrecondition(afterContentPadding >= 0) { "negative afterContentPadding" }
    val pageSizeWithSpacing = (pageAvailableSize + spaceBetweenPages).coerceAtLeast(0)
    // Limit beyondViewportPageCount to pageCount to prevent overflow if it's very large
    val coercedBeyondViewportPageCount = beyondViewportPageCount.coerceAtMost(pageCount)

    debugLog {
        "Starting Measure Pass..." +
            "\n CurrentPage = $currentPage" +
            "\n CurrentPageOffset = $currentPageOffset" +
            "\n SnapPosition = $snapPosition"
    }

    val childConstraints =
        Constraints(
            maxWidth =
                if (orientation == Orientation.Vertical) {
                    constraints.maxWidth
                } else {
                    pageAvailableSize
                },
            maxHeight =
                if (orientation != Orientation.Vertical) {
                    constraints.maxHeight
                } else {
                    pageAvailableSize
                },
        )

    return if (pageCount <= 0) {
        PagerMeasureResult(
            visiblePagesInfo = emptyList(),
            pageSize = pageAvailableSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            orientation = orientation,
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = mainAxisAvailableSize + afterContentPadding,
            measureResult = layout(constraints.minWidth, constraints.minHeight) {},
            firstVisiblePage = null,
            firstVisiblePageScrollOffset = 0,
            reverseLayout = false,
            beyondViewportPageCount = coercedBeyondViewportPageCount,
            canScrollForward = false,
            currentPage = null,
            currentPageOffsetFraction = 0.0f,
            snapPosition = snapPosition,
            remeasureNeeded = false,
            coroutineScope = coroutineScope,
            density = density,
            childConstraints = childConstraints,
        )
    } else {
        var firstVisiblePage = currentPage
        var firstVisiblePageOffset = currentPageOffset

        // figure out the first visible page and the firstVisiblePageOffset based on current page
        // The offset by the scroll event has already been applied to currentPageOffset
        while (firstVisiblePage > 0 && firstVisiblePageOffset > 0) {
            firstVisiblePage--
            firstVisiblePageOffset -= pageSizeWithSpacing
        }

        //  the scroll offset is opposite sign to the actual offset
        val firstVisiblePageScrollOffset = firstVisiblePageOffset * -1

        var currentFirstPage = firstVisiblePage
        var currentFirstPageScrollOffset = firstVisiblePageScrollOffset
        if (currentFirstPage >= pageCount) {
            // the data set has been updated and now we have less pages that we were
            // scrolled to before
            currentFirstPage = pageCount - 1
            currentFirstPageScrollOffset = 0
        }

        debugLog {
            "Calculated Info:" +
                "\n FirstVisiblePage=$firstVisiblePage" +
                "\n firstVisiblePageScrollOffset=$firstVisiblePageScrollOffset"
        }

        // this will contain all the measured pages representing the visible pages
        val visiblePages = ArrayDeque<MeasuredPage>()

        // define min and max offsets
        val minOffset = -beforeContentPadding + if (spaceBetweenPages < 0) spaceBetweenPages else 0
        val maxOffset = mainAxisAvailableSize

        // include the start padding so we compose pages in the padding area and neutralise page
        // spacing (if the spacing is negative this will make sure the previous page is composed)
        // before starting scrolling forward we will remove it back
        currentFirstPageScrollOffset += minOffset

        // max of cross axis sizes of all visible pages
        var maxCrossAxis = 0

        debugLog { "Composing Backwards" }

        // we had scrolled backward or we compose pages in the start padding area, which means
        // pages before current firstPageScrollOffset should be visible. compose them and update
        // firstPageScrollOffset
        while (currentFirstPageScrollOffset < 0 && currentFirstPage > 0) {
            val previous = currentFirstPage - 1
            val measuredPage =
                getAndMeasure(
                    index = previous,
                    childConstraints = childConstraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    pageAvailableSize = pageAvailableSize,
                    placeablesCache = placeablesCache,
                )

            debugLog { "Composed Page=$previous" }

            visiblePages.add(0, measuredPage)
            maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
            currentFirstPageScrollOffset += pageSizeWithSpacing
            currentFirstPage = previous
        }

        if (currentFirstPageScrollOffset < minOffset) {
            currentFirstPageScrollOffset = minOffset
        }

        // neutralize previously added padding as we stopped filling the before content padding
        currentFirstPageScrollOffset -= minOffset

        var index = currentFirstPage
        val maxMainAxis = (maxOffset + afterContentPadding).coerceAtLeast(0)
        var currentMainAxisOffset = -currentFirstPageScrollOffset

        // will be set to true if we composed some items only to know their size and apply scroll,
        // while in the end this item will not end up in the visible viewport. we will need an
        // extra remeasure in order to dispose such items.
        var remeasureNeeded = false

        // first we need to skip pages we already composed while composing backward
        var indexInVisibleItems = 0

        while (indexInVisibleItems < visiblePages.size) {
            if (currentMainAxisOffset >= maxMainAxis) {
                // this item is out of the bounds and will not be visible.
                visiblePages.removeAt(indexInVisibleItems)
                remeasureNeeded = true
            } else {
                index++
                currentMainAxisOffset += pageSizeWithSpacing
                indexInVisibleItems++
            }
        }

        debugLog { "Composing Forward Starting at Index=$index" }
        // then composing visible pages forward until we fill the whole viewport.
        // we want to have at least one page in visiblePages even if in fact all the pages are
        // offscreen, this can happen if the content padding is larger than the available size.
        while (
            index < pageCount &&
                (currentMainAxisOffset < maxMainAxis ||
                    currentMainAxisOffset <= 0 || // filling beforeContentPadding area
                    visiblePages.isEmpty())
        ) {
            val measuredPage =
                getAndMeasure(
                    index = index,
                    childConstraints = childConstraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    pageAvailableSize = pageAvailableSize,
                    placeablesCache = placeablesCache,
                )

            debugLog { "Composed Page=$index at $currentFirstPageScrollOffset" }

            // do not add space to the last page
            currentMainAxisOffset +=
                if (index == pageCount - 1) {
                    pageAvailableSize
                } else {
                    pageSizeWithSpacing
                }

            if (currentMainAxisOffset <= minOffset && index != pageCount - 1) {
                // this page is offscreen and will not be visible. advance currentFirstPage
                currentFirstPage = index + 1
                currentFirstPageScrollOffset -= pageSizeWithSpacing
                remeasureNeeded = true
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
            while (currentFirstPageScrollOffset < beforeContentPadding && currentFirstPage > 0) {
                val previousIndex = currentFirstPage - 1
                val measuredPage =
                    getAndMeasure(
                        index = previousIndex,
                        childConstraints = childConstraints,
                        pagerItemProvider = pagerItemProvider,
                        visualPageOffset = visualPageOffset,
                        orientation = orientation,
                        horizontalAlignment = horizontalAlignment,
                        verticalAlignment = verticalAlignment,
                        layoutDirection = layoutDirection,
                        reverseLayout = reverseLayout,
                        pageAvailableSize = pageAvailableSize,
                        placeablesCache = placeablesCache,
                    )
                visiblePages.add(0, measuredPage)
                maxCrossAxis = maxOf(maxCrossAxis, measuredPage.crossAxisSize)
                currentFirstPageScrollOffset += pageSizeWithSpacing
                currentFirstPage = previousIndex
            }

            if (currentFirstPageScrollOffset < 0) {
                currentMainAxisOffset += currentFirstPageScrollOffset
                currentFirstPageScrollOffset = 0
            }
        }

        // the initial offset for pages from visiblePages list
        requirePrecondition(currentFirstPageScrollOffset >= 0) {
            "invalid currentFirstPageScrollOffset"
        }
        val visiblePagesScrollOffset = -currentFirstPageScrollOffset

        var firstPage = visiblePages.first()

        // even if we compose pages to fill before content padding we should ignore pages fully
        // located there for the state's scroll position calculation (first page + first offset)
        if (beforeContentPadding > 0 || spaceBetweenPages < 0) {
            for (i in visiblePages.indices) {
                val size = pageSizeWithSpacing
                if (
                    currentFirstPageScrollOffset != 0 &&
                        size <= currentFirstPageScrollOffset &&
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
        val extraPagesBefore =
            createPagesBeforeList(
                currentFirstPage = currentFirstPage,
                beyondViewportPageCount = coercedBeyondViewportPageCount,
                pinnedPages = pinnedPages,
            ) {
                getAndMeasure(
                    index = it,
                    childConstraints = childConstraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    pageAvailableSize = pageAvailableSize,
                    placeablesCache = placeablesCache,
                )
            }

        // Update maxCrossAxis with extra pages
        extraPagesBefore.fastForEach { maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize) }

        // Compose pages after last page
        val extraPagesAfter =
            createPagesAfterList(
                currentLastPage = visiblePages.last().index,
                pagesCount = pageCount,
                beyondViewportPageCount = coercedBeyondViewportPageCount,
                pinnedPages = pinnedPages,
            ) {
                getAndMeasure(
                    index = it,
                    childConstraints = childConstraints,
                    pagerItemProvider = pagerItemProvider,
                    visualPageOffset = visualPageOffset,
                    orientation = orientation,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    pageAvailableSize = pageAvailableSize,
                    placeablesCache = placeablesCache,
                )
            }

        // Update maxCrossAxis with extra pages
        extraPagesAfter.fastForEach { maxCrossAxis = maxOf(maxCrossAxis, it.crossAxisSize) }

        val noExtraPages =
            firstPage == visiblePages.first() &&
                extraPagesBefore.isEmpty() &&
                extraPagesAfter.isEmpty()

        val layoutWidth =
            constraints.constrainWidth(
                if (orientation == Orientation.Vertical) maxCrossAxis else currentMainAxisOffset
            )

        val layoutHeight =
            constraints.constrainHeight(
                if (orientation == Orientation.Vertical) currentMainAxisOffset else maxCrossAxis
            )

        val positionedPages =
            calculatePagesOffsets(
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
                spaceBetweenPages = spaceBetweenPages,
            )

        val visiblePagesInfo =
            if (noExtraPages) positionedPages
            else
                positionedPages.fastFilter {
                    (it.index >= visiblePages.first().index &&
                        it.index <= visiblePages.last().index)
                }

        val positionedPagesBefore =
            if (extraPagesBefore.isEmpty()) emptyList()
            else positionedPages.fastFilter { it.index < visiblePages.first().index }

        val positionedPagesAfter =
            if (extraPagesAfter.isEmpty()) emptyList()
            else positionedPages.fastFilter { it.index > visiblePages.last().index }

        val layoutSize = mainAxisAvailableSize + beforeContentPadding + afterContentPadding

        val newCurrentPage =
            calculateNewCurrentPage(
                layoutSize,
                visiblePagesInfo,
                beforeContentPadding,
                afterContentPadding,
                pageAvailableSize,
                snapPosition,
                pageCount,
            )

        val snapOffset =
            snapPosition.position(
                layoutSize,
                pageAvailableSize,
                beforeContentPadding,
                afterContentPadding,
                newCurrentPage?.index ?: 0,
                pageCount,
            )

        val currentPagePositionOffset = (newCurrentPage?.offset ?: 0)

        val currentPageOffsetFraction =
            if (pageSizeWithSpacing == 0) {
                0.0f
            } else {
                ((snapOffset - currentPagePositionOffset) / (pageSizeWithSpacing.toFloat()))
                    .coerceIn(MinPageOffset, MaxPageOffset)
            }

        debugLog {
            "Finished Measure Pass" +
                "\n Final currentPage=${newCurrentPage?.index} " +
                "\n Final currentPageScrollOffset=$currentPagePositionOffset" +
                "\n Final currentPageScrollOffsetFraction=$currentPageOffsetFraction"
        }

        return PagerMeasureResult(
            firstVisiblePage = firstPage,
            firstVisiblePageScrollOffset = currentFirstPageScrollOffset,
            measureResult =
                layout(layoutWidth, layoutHeight) {
                    // Tagging as motion frame of reference placement, meaning the placement
                    // contains scrolling. This allows the consumer of this placement offset to
                    // differentiate this offset vs. offsets from structural changes. Generally
                    // speaking, this signals a preference to directly apply changes rather than
                    // animating, to avoid a chasing effect to scrolling.
                    withMotionFrameOfReferencePlacement {
                        positionedPages.fastForEach { it.place(this) }
                    }
                    // we attach it during the placement so PagerState can trigger re-placement
                    placementScopeInvalidator.attachToScope()
                },
            viewportStartOffset = -beforeContentPadding,
            viewportEndOffset = maxOffset + afterContentPadding,
            visiblePagesInfo = visiblePagesInfo,
            reverseLayout = reverseLayout,
            orientation = orientation,
            pageSize = pageAvailableSize,
            pageSpacing = spaceBetweenPages,
            afterContentPadding = afterContentPadding,
            beyondViewportPageCount = coercedBeyondViewportPageCount,
            canScrollForward = index < pageCount || currentMainAxisOffset > maxOffset,
            currentPage = newCurrentPage,
            currentPageOffsetFraction = currentPageOffsetFraction,
            snapPosition = snapPosition,
            remeasureNeeded = remeasureNeeded,
            extraPagesBefore = positionedPagesBefore,
            extraPagesAfter = positionedPagesAfter,
            coroutineScope = coroutineScope,
            density = density,
            childConstraints = childConstraints,
        )
    }
}

private fun createPagesAfterList(
    currentLastPage: Int,
    pagesCount: Int,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage,
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null

    val end = minOf(beyondViewportPageCount, (pagesCount - currentLastPage) - 1) + currentLastPage

    for (i in currentLastPage + 1..end) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }

    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex in (end + 1) until pagesCount) {
            if (list == null) list = mutableListOf()
            list?.add(getAndMeasure(pageIndex))
        }
    }

    return list ?: emptyList()
}

private fun createPagesBeforeList(
    currentFirstPage: Int,
    beyondViewportPageCount: Int,
    pinnedPages: List<Int>,
    getAndMeasure: (Int) -> MeasuredPage,
): List<MeasuredPage> {
    var list: MutableList<MeasuredPage>? = null

    val start = maxOf(0, currentFirstPage - beyondViewportPageCount)

    for (i in currentFirstPage - 1 downTo start) {
        if (list == null) list = mutableListOf()
        list.add(getAndMeasure(i))
    }

    pinnedPages.fastForEach { pageIndex ->
        if (pageIndex < start) {
            if (list == null) list = mutableListOf()
            list?.add(getAndMeasure(pageIndex))
        }
    }

    return list ?: emptyList()
}

private fun calculateNewCurrentPage(
    viewportSize: Int,
    visiblePagesInfo: List<MeasuredPage>,
    beforeContentPadding: Int,
    afterContentPadding: Int,
    itemSize: Int,
    snapPosition: SnapPosition,
    pageCount: Int,
): MeasuredPage? {
    return visiblePagesInfo.fastMaxBy {
        -abs(
            calculateDistanceToDesiredSnapPosition(
                mainAxisViewPortSize = viewportSize,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                itemSize = itemSize,
                itemOffset = it.offset,
                itemIndex = it.index,
                snapPosition = snapPosition,
                itemCount = pageCount,
            )
        )
    }
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
    layoutDirection: LayoutDirection,
    reverseLayout: Boolean,
    pageAvailableSize: Int,
    placeablesCache: MutableIntObjectMap<List<Placeable>>,
): MeasuredPage {
    val key = pagerItemProvider.getKey(index)
    val cachedPlaceables = placeablesCache[index]
    val placeable =
        if (cachedPlaceables != null) {
            cachedPlaceables
        } else {
            val measurables = compose(index)
            List(measurables.size) { i -> measurables[i].measure(childConstraints) }
                .also { placeablesCache[index] = it }
        }

    return MeasuredPage(
        index = index,
        placeables = placeable,
        visualOffset = visualPageOffset,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        layoutDirection = layoutDirection,
        reverseLayout = reverseLayout,
        size = pageAvailableSize,
        orientation = orientation,
        key = key,
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
    pageAvailableSize: Int,
): MutableList<MeasuredPage> {
    val pageSizeWithSpacing = (pageAvailableSize + spaceBetweenPages)
    val mainAxisLayoutSize = if (orientation == Orientation.Vertical) layoutHeight else layoutWidth
    val hasSpareSpace = finalMainAxisOffset < minOf(mainAxisLayoutSize, maxOffset)
    if (hasSpareSpace) {
        checkPrecondition(pagesScrollOffset == 0) {
            "non-zero pagesScrollOffset=$pagesScrollOffset"
        }
    }
    val positionedPages =
        ArrayList<MeasuredPage>(pages.size + extraPagesBefore.size + extraPagesAfter.size)

    if (hasSpareSpace) {
        requirePrecondition(extraPagesBefore.isEmpty() && extraPagesAfter.isEmpty()) {
            "No extra pages"
        }

        val pagesCount = pages.size
        fun Int.reverseAware() = if (!reverseLayout) this else pagesCount - this - 1

        val sizes = IntArray(pagesCount) { pageAvailableSize }
        val offsets = IntArray(pagesCount)

        val arrangement = spacedBy(spaceBetweenPages.toDp())
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
            val relativeOffset =
                if (reverseLayout) {
                    // inverse offset to align with scroll direction for positioning
                    mainAxisLayoutSize - absoluteOffset - page.size
                } else {
                    absoluteOffset
                }
            page.position(relativeOffset, layoutWidth, layoutHeight)
            positionedPages.add(page)
        }
    } else {
        var currentMainAxis = pagesScrollOffset
        extraPagesBefore.fastForEach {
            currentMainAxis -= pageSizeWithSpacing
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
        }

        currentMainAxis = pagesScrollOffset
        pages.fastForEach {
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
            currentMainAxis += pageSizeWithSpacing
        }

        extraPagesAfter.fastForEach {
            it.position(currentMainAxis, layoutWidth, layoutHeight)
            positionedPages.add(it)
            currentMainAxis += pageSizeWithSpacing
        }
    }
    return positionedPages
}

internal const val MinPageOffset = -0.5f
internal const val MaxPageOffset = 0.5f

private inline fun debugLog(generateMsg: () -> String) {
    if (PagerDebugConfig.MeasureLogic) {
        println("PagerMeasure: ${generateMsg()}")
    }
}
