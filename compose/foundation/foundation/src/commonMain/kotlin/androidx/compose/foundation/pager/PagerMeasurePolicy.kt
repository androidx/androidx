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
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberPagerMeasurePolicy(
    itemProviderLambda: () -> PagerLazyLayoutItemProvider,
    state: PagerState,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    beyondViewportPageCount: Int,
    pageSpacing: Dp,
    pageSize: PageSize,
    horizontalAlignment: Alignment.Horizontal?,
    verticalAlignment: Alignment.Vertical?,
    snapPosition: SnapPosition,
    coroutineScope: CoroutineScope,
    pageCount: () -> Int,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    state,
    contentPadding,
    reverseLayout,
    orientation,
    horizontalAlignment,
    verticalAlignment,
    pageSpacing,
    pageSize,
    snapPosition,
    pageCount,
    beyondViewportPageCount,
    coroutineScope
) {
    { containerConstraints ->
        state.measurementScopeInvalidator.attachToScope()
        val isVertical = orientation == Orientation.Vertical
        checkScrollableContainerConstraints(
            containerConstraints,
            if (isVertical) Orientation.Vertical else Orientation.Horizontal
        )

        // resolve content paddings
        val startPadding =
            if (isVertical) {
                contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateStartPadding(layoutDirection).roundToPx()
            }

        val endPadding =
            if (isVertical) {
                contentPadding.calculateRightPadding(layoutDirection).roundToPx()
            } else {
                // in horizontal configuration, padding is reversed by placeRelative
                contentPadding.calculateEndPadding(layoutDirection).roundToPx()
            }
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
        val totalVerticalPadding = topPadding + bottomPadding
        val totalHorizontalPadding = startPadding + endPadding
        val totalMainAxisPadding = if (isVertical) totalVerticalPadding else totalHorizontalPadding
        val beforeContentPadding = when {
            isVertical && !reverseLayout -> topPadding
            isVertical && reverseLayout -> bottomPadding
            !isVertical && !reverseLayout -> startPadding
            else -> endPadding // !isVertical && reverseLayout
        }
        val afterContentPadding = totalMainAxisPadding - beforeContentPadding
        val contentConstraints =
            containerConstraints.offset(-totalHorizontalPadding, -totalVerticalPadding)

        state.density = this

        val spaceBetweenPages = pageSpacing.roundToPx()

        // can be negative if the content padding is larger than the max size from constraints
        val mainAxisAvailableSize = if (isVertical) {
            containerConstraints.maxHeight - totalVerticalPadding
        } else {
            containerConstraints.maxWidth - totalHorizontalPadding
        }
        val visualItemOffset = if (!reverseLayout || mainAxisAvailableSize > 0) {
            IntOffset(startPadding, topPadding)
        } else {
            // When layout is reversed and paddings together take >100% of the available space,
            // layout size is coerced to 0 when positioning. To take that space into account,
            // we offset start padding by negative space between paddings.
            IntOffset(
                if (isVertical) startPadding else startPadding + mainAxisAvailableSize,
                if (isVertical) topPadding + mainAxisAvailableSize else topPadding
            )
        }

        val pageAvailableSize =
            with(pageSize) { calculateMainAxisPageSize(mainAxisAvailableSize, spaceBetweenPages) }

        state.premeasureConstraints = Constraints(
            maxWidth = if (orientation == Orientation.Vertical) {
                contentConstraints.maxWidth
            } else {
                pageAvailableSize
            },
            maxHeight = if (orientation != Orientation.Vertical) {
                contentConstraints.maxHeight
            } else {
                pageAvailableSize
            }
        )
        val itemProvider = itemProviderLambda()

        val currentPage: Int
        val currentPageOffset: Int

        Snapshot.withoutReadObservation {
            currentPage = state.matchScrollPositionWithKey(itemProvider, state.currentPage)
            currentPageOffset = snapPosition.currentPageOffset(
                mainAxisAvailableSize,
                pageAvailableSize,
                spaceBetweenPages,
                beforeContentPadding,
                afterContentPadding,
                state.currentPage,
                state.currentPageOffsetFraction,
                state.pageCount
            )
        }

        val pinnedPages = itemProvider.calculateLazyLayoutPinnedIndices(
            pinnedItemList = state.pinnedPages,
            beyondBoundsInfo = state.beyondBoundsInfo
        )

        val measureResult = Snapshot.withMutableSnapshot {
            measurePager(
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                constraints = contentConstraints,
                pageCount = pageCount(),
                spaceBetweenPages = spaceBetweenPages,
                mainAxisAvailableSize = mainAxisAvailableSize,
                visualPageOffset = visualItemOffset,
                pageAvailableSize = pageAvailableSize,
                beyondViewportPageCount = beyondViewportPageCount,
                orientation = orientation,
                currentPage = currentPage,
                currentPageOffset = currentPageOffset,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                pagerItemProvider = itemProvider,
                reverseLayout = reverseLayout,
                pinnedPages = pinnedPages,
                snapPosition = snapPosition,
                placementScopeInvalidator = state.placementScopeInvalidator,
                coroutineScope = coroutineScope,
                layout = { width, height, placement ->
                    layout(
                        containerConstraints.constrainWidth(width + totalHorizontalPadding),
                        containerConstraints.constrainHeight(height + totalVerticalPadding),
                        emptyMap(),
                        placement
                    )
                }
            )
        }
        state.applyMeasureResult(measureResult)
        measureResult
    }
}
