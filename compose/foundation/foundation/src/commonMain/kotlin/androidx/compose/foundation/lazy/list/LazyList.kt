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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.assertNotNestingScrollableContainers
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.OverScrollController
import androidx.compose.foundation.gestures.rememberOverScrollController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyMeasurePolicy
import androidx.compose.foundation.lazy.layout.rememberLazyLayoutPrefetchPolicy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.offset

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
    val overScrollController = rememberOverScrollController()

    val itemScope: Ref<LazyItemScopeImpl> = remember { Ref() }

    val stateOfItemsProvider = rememberStateOfItemsProvider(state, content, itemScope)

    val scope = rememberCoroutineScope()
    val placementAnimator = remember(state, isVertical) {
        LazyListItemPlacementAnimator(scope, isVertical)
    }
    state.placementAnimator = placementAnimator

    val measurePolicy = rememberLazyListMeasurePolicy(
        stateOfItemsProvider,
        itemScope,
        state,
        overScrollController,
        contentPadding,
        reverseLayout,
        isVertical,
        horizontalAlignment,
        verticalAlignment,
        horizontalArrangement,
        verticalArrangement,
        placementAnimator
    )

    state.prefetchPolicy = rememberLazyLayoutPrefetchPolicy()

    ScrollPositionUpdater(stateOfItemsProvider, state)

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .lazyListSemantics(
                stateOfItemsProvider = stateOfItemsProvider,
                state = state,
                coroutineScope = scope,
                isVertical = isVertical,
                reverseScrolling = reverseLayout,
                userScrollEnabled = userScrollEnabled
            )
            .clipScrollableContainer(isVertical)
            .scrollable(
                orientation = if (isVertical) Orientation.Vertical else Orientation.Horizontal,
                reverseDirection = run {
                    // A finger moves with the content, not with the viewport. Therefore,
                    // always reverse once to have "natural" gesture that goes reversed to layout
                    var reverseDirection = !reverseLayout
                    // But if rtl and horizontal, things move the other way around
                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                    if (isRtl && !isVertical) {
                        reverseDirection = !reverseDirection
                    }
                    reverseDirection
                },
                interactionSource = state.internalInteractionSource,
                flingBehavior = flingBehavior,
                state = state,
                overScrollController = overScrollController,
                enabled = userScrollEnabled
            ),
        prefetchPolicy = state.prefetchPolicy,
        measurePolicy = measurePolicy,
        itemsProvider = { stateOfItemsProvider.value }
    )
}

/** Extracted to minimize the recomposition scope */
@Composable
private fun ScrollPositionUpdater(
    stateOfItemsProvider: State<LazyListItemsProvider>,
    state: LazyListState
) {
    val itemsProvider = stateOfItemsProvider.value
    if (itemsProvider.itemsCount > 0) {
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)
    }
}

@Composable
private fun rememberLazyListMeasurePolicy(
    /** State containing the items provider of the list. */
    stateOfItemsProvider: State<LazyListItemsProvider>,
    /** Value holder for the item scope used to compose items. */
    itemScope: Ref<LazyItemScopeImpl>,
    /** The state of the list. */
    state: LazyListState,
    /** The overscroll controller. */
    overScrollController: OverScrollController,
    /** The inner padding to be added for the whole content(nor for each individual item) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the list */
    isVertical: Boolean,
    /** The alignment to align items horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal? = null,
    /** The alignment to align items vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical? = null,
    /** The horizontal arrangement for items. Required when isVertical is false */
    horizontalArrangement: Arrangement.Horizontal? = null,
    /** The vertical arrangement for items. Required when isVertical is true */
    verticalArrangement: Arrangement.Vertical? = null,
    /** Item placement animator. Should be notified with the measuring result */
    placementAnimator: LazyListItemPlacementAnimator
) = remember(
    state,
    overScrollController,
    contentPadding,
    reverseLayout,
    isVertical,
    horizontalAlignment,
    verticalAlignment,
    horizontalArrangement,
    verticalArrangement,
    placementAnimator
) {
    LazyMeasurePolicy { placeablesProvider, containerConstraints ->
        containerConstraints.assertNotNestingScrollableContainers(isVertical)

        // resolve content paddings
        val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
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

        val itemsProvider = stateOfItemsProvider.value
        state.updateScrollPositionIfTheFirstItemWasMoved(itemsProvider)

        // Update the state's cached Density
        state.density = this

        // this will update the scope object if the constrains have been changed
        itemScope.update(this, contentConstraints)

        val spaceBetweenItemsDp = if (isVertical) {
            requireNotNull(verticalArrangement).spacing
        } else {
            requireNotNull(horizontalArrangement).spacing
        }
        val spaceBetweenItems = spaceBetweenItemsDp.roundToPx()

        val itemsCount = itemsProvider.itemsCount

        val itemProvider = LazyMeasuredItemProvider(
            contentConstraints,
            isVertical,
            itemsProvider,
            placeablesProvider
        ) { index, key, placeables ->
            // we add spaceBetweenItems as an extra spacing for all items apart from the last one so
            // the lazy list measuring logic will take it into account.
            val spacing = if (index.value == itemsCount - 1) 0 else spaceBetweenItems
            LazyMeasuredItem(
                index = index.value,
                placeables = placeables,
                isVertical = isVertical,
                horizontalAlignment = horizontalAlignment,
                verticalAlignment = verticalAlignment,
                layoutDirection = layoutDirection,
                reverseLayout = reverseLayout,
                beforeContentPadding = beforeContentPadding,
                afterContentPadding = afterContentPadding,
                spacing = spacing,
                visualOffset = IntOffset(startPadding, topPadding),
                key = key,
                placementAnimator = placementAnimator
            )
        }
        state.premeasureConstraints = itemProvider.childConstraints

        // can be negative if the content padding is larger than the max size from constraints
        val mainAxisAvailableSize = if (isVertical) {
            containerConstraints.maxHeight - totalVerticalPadding
        } else {
            containerConstraints.maxWidth - totalHorizontalPadding
        }

        measureLazyList(
            itemsCount = itemsCount,
            itemProvider = itemProvider,
            mainAxisAvailableSize = mainAxisAvailableSize,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            firstVisibleItemIndex = state.firstVisibleItemIndexNonObservable,
            firstVisibleItemScrollOffset = state.firstVisibleItemScrollOffsetNonObservable,
            scrollToBeConsumed = state.scrollToBeConsumed,
            constraints = contentConstraints,
            isVertical = isVertical,
            headerIndexes = itemsProvider.headerIndexes,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            reverseLayout = reverseLayout,
            density = this,
            layoutDirection = layoutDirection,
            placementAnimator = placementAnimator,
            layout = { width, height, placement ->
                layout(
                    containerConstraints.constrainWidth(width + totalHorizontalPadding),
                    containerConstraints.constrainHeight(height + totalVerticalPadding),
                    emptyMap(),
                    placement
                )
            }
        ).also {
            state.applyMeasureResult(it)
            refreshOverScrollInfo(
                overScrollController,
                it,
                containerConstraints,
                totalHorizontalPadding,
                totalVerticalPadding
            )
        }.lazyLayoutMeasureResult
    }
}

private fun Ref<LazyItemScopeImpl>.update(density: Density, constraints: Constraints) {
    val value = value
    if (value == null || value.density != density || value.constraints != constraints) {
        this.value = LazyItemScopeImpl(density, constraints)
    }
}

private fun refreshOverScrollInfo(
    overScrollController: OverScrollController,
    result: LazyListMeasureResult,
    constraints: Constraints,
    totalHorizontalPadding: Int,
    totalVerticalPadding: Int
) {
    val canScrollForward = result.canScrollForward
    val canScrollBackward = (result.firstVisibleItem?.index ?: 0) != 0 ||
        result.firstVisibleItemScrollOffset != 0

    overScrollController.refreshContainerInfo(
        Size(
            constraints.constrainWidth(result.width + totalHorizontalPadding).toFloat(),
            constraints.constrainHeight(result.height + totalVerticalPadding).toFloat()
        ),
        canScrollForward || canScrollBackward
    )
}
