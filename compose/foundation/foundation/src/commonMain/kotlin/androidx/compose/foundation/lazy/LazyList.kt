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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.foundation.lazy.layout.lazyLayoutBeyondBoundsModifier
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.scrollingContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyList(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier,
    /** State controlling the scroll position */
    state: LazyListState,
    /** The inner padding to be added for the whole content(not for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** fling behavior to be used for flinging */
    flingBehavior: FlingBehavior,
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int = 0,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The content of the list */
    content: LazyListScope.() -> Unit
) {
    val itemProviderLambda = rememberLazyListItemProviderLambda(state, content)

    val semanticState = rememberLazyListSemanticState(state, isVertical)
    val coroutineScope = rememberCoroutineScope()
    val graphicsContext = LocalGraphicsContext.current
    val stickyHeadersEnabled = !LocalScrollCaptureInProgress.current

    val measurePolicy = rememberLazyListMeasurePolicy(
        itemProviderLambda,
        state,
        contentPadding,
        reverseLayout,
        isVertical,
        beyondBoundsItemCount,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement,
        coroutineScope,
        graphicsContext,
        stickyHeadersEnabled = stickyHeadersEnabled,
    )

    val orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal
    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .lazyLayoutSemantics(
                itemProviderLambda = itemProviderLambda,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled,
                reverseScrolling = reverseLayout,
            )
            .lazyLayoutBeyondBoundsModifier(
                state = rememberLazyListBeyondBoundsState(
                    state = state,
                    beyondBoundsItemCount = beyondBoundsItemCount
                ),
                beyondBoundsInfo = state.beyondBoundsInfo,
                reverseLayout = reverseLayout,
                layoutDirection = LocalLayoutDirection.current,
                orientation = orientation,
                enabled = userScrollEnabled
            )
            .then(state.itemAnimator.modifier)
            .scrollingContainer(
                state = state,
                orientation = orientation,
                enabled = userScrollEnabled,
                reverseScrolling = reverseLayout,
                flingBehavior = flingBehavior,
                interactionSource = state.internalInteractionSource
            ),
        prefetchState = state.prefetchState,
        measurePolicy = measurePolicy,
        itemProvider = itemProviderLambda
    )
}

@ExperimentalFoundationApi
@Composable
private fun rememberLazyListMeasurePolicy(
    /** Items provider of the list. */
    itemProviderLambda: () -> LazyListItemProvider,
    /** The state of the list. */
    state: LazyListState,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** Number of items to layout before and after the visible items */
    beyondBoundsItemCount: Int,
    /** The alignment to align items horizontally */
    horizontalAlignment: Alignment.Horizontal?,
    /** The alignment to align items vertically */
    verticalAlignment: Alignment.Vertical?,
    /** The horizontal arrangement for items */
    horizontalArrangement: Arrangement.Horizontal?,
    /** The vertical arrangement for items */
    verticalArrangement: Arrangement.Vertical?,
    /** Scope for animations */
    coroutineScope: CoroutineScope,
    /** Used for creating graphics layers */
    graphicsContext: GraphicsContext,
    stickyHeadersEnabled: Boolean,
) = remember<LazyLayoutMeasureScope.(Constraints) -> MeasureResult>(
    state,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalAlignment,
    verticalAlignment,
    horizontalArrangement,
    verticalArrangement,
    graphicsContext,
    stickyHeadersEnabled,
) {
    { containerConstraints ->
        state.measurementScopeInvalidator.attachToScope()
        // Tracks if the lookahead pass has occurred
        val hasLookaheadPassOccurred = state.hasLookaheadPassOccurred || isLookingAhead
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

        val itemProvider = itemProviderLambda()
        // this will update the scope used by the item composables
        itemProvider.itemScope.setMaxSize(
            width = contentConstraints.maxWidth,
            height = contentConstraints.maxHeight
        )

        val spaceBetweenItemsDp = if (isVertical) {
            requireNotNull(verticalArrangement) {
                "null verticalArrangement when isVertical == true"
            }.spacing
        } else {
            requireNotNull(horizontalArrangement) {
                "null horizontalAlignment when isVertical == false"
            }.spacing
        }
        val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

        val itemsCount = itemProvider.itemCount

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

        val measuredItemProvider = object : LazyListMeasuredItemProvider(
            contentConstraints,
            isVertical,
            itemProvider,
            this
        ) {
            override fun createItem(
                index: Int,
                key: Any,
                contentType: Any?,
                placeables: List<Placeable>,
                constraints: Constraints
            ): LazyListMeasuredItem {
                // we add spaceBetweenItems as an extra spacing for all items apart from the last one so
                // the lazy list measuring logic will take it into account.
                val spacing = if (index == itemsCount - 1) 0 else spaceBetweenItems
                return LazyListMeasuredItem(
                    index = index,
                    placeables = placeables,
                    isVertical = isVertical,
                    horizontalAlignment = horizontalAlignment,
                    verticalAlignment = verticalAlignment,
                    layoutDirection = layoutDirection,
                    reverseLayout = reverseLayout,
                    beforeContentPadding = beforeContentPadding,
                    afterContentPadding = afterContentPadding,
                    spacing = spacing,
                    visualOffset = visualItemOffset,
                    key = key,
                    contentType = contentType,
                    animator = state.itemAnimator,
                    constraints = constraints
                )
            }
        }

        val firstVisibleItemIndex: Int
        val firstVisibleScrollOffset: Int
        Snapshot.withoutReadObservation {
            firstVisibleItemIndex = state.updateScrollPositionIfTheFirstItemWasMoved(
                itemProvider, state.firstVisibleItemIndex
            )
            firstVisibleScrollOffset = state.firstVisibleItemScrollOffset
        }

        val pinnedItems = itemProvider.calculateLazyLayoutPinnedIndices(
            pinnedItemList = state.pinnedItems,
            beyondBoundsInfo = state.beyondBoundsInfo
        )

        val scrollToBeConsumed = if (isLookingAhead || !hasLookaheadPassOccurred) {
            state.scrollToBeConsumed
        } else {
            state.scrollDeltaBetweenPasses
        }

        @Suppress("PrimitiveInCollection")
        val headerIndexes = if (stickyHeadersEnabled) {
            itemProvider.headerIndexes
        } else {
            emptyList()
        }

        val measureResult = Snapshot.withMutableSnapshot {
            measureLazyList(
                itemsCount = itemsCount,
                measuredItemProvider = measuredItemProvider,
                mainAxisAvailableSize = mainAxisAvailableSize,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                spaceBetweenItems = spaceBetweenItems,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = firstVisibleScrollOffset,
                scrollToBeConsumed = scrollToBeConsumed,
                constraints = contentConstraints,
                isVertical = isVertical,
                headerIndexes = headerIndexes,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
                reverseLayout = reverseLayout,
                density = this,
                itemAnimator = state.itemAnimator,
                beyondBoundsItemCount = beyondBoundsItemCount,
                pinnedItems = pinnedItems,
                hasLookaheadPassOccurred = hasLookaheadPassOccurred,
                isLookingAhead = isLookingAhead,
                postLookaheadLayoutInfo = state.postLookaheadLayoutInfo,
                coroutineScope = coroutineScope,
                placementScopeInvalidator = state.placementScopeInvalidator,
                graphicsContext = graphicsContext,
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
        state.applyMeasureResult(measureResult, isLookingAhead)
        measureResult
    }
}
