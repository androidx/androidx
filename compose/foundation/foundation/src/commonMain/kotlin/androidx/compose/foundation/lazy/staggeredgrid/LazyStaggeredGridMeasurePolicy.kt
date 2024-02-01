/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.staggeredgrid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.checkScrollableContainerConstraints
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScope
import androidx.compose.foundation.lazy.layout.calculateLazyLayoutPinnedIndices
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberStaggeredGridMeasurePolicy(
    state: LazyStaggeredGridState,
    itemProviderLambda: () -> LazyStaggeredGridItemProvider,
    contentPadding: PaddingValues,
    reverseLayout: Boolean,
    orientation: Orientation,
    mainAxisSpacing: Dp,
    crossAxisSpacing: Dp,
    coroutineScope: CoroutineScope,
    slots: LazyGridStaggeredGridSlotsProvider
): LazyLayoutMeasureScope.(Constraints) -> LazyStaggeredGridMeasureResult = remember(
    state,
    itemProviderLambda,
    contentPadding,
    reverseLayout,
    orientation,
    mainAxisSpacing,
    crossAxisSpacing,
    slots
) {
    { constraints ->
        checkScrollableContainerConstraints(
            constraints,
            orientation
        )
        val resolvedSlots = slots.invoke(density = this, constraints = constraints)
        val isVertical = orientation == Orientation.Vertical
        val itemProvider = itemProviderLambda()

        // setup measure
        val beforeContentPadding = contentPadding.beforePadding(
            orientation, reverseLayout, layoutDirection
        ).roundToPx()
        val afterContentPadding = contentPadding.afterPadding(
            orientation, reverseLayout, layoutDirection
        ).roundToPx()
        val startContentPadding = contentPadding.startPadding(
            orientation, layoutDirection
        ).roundToPx()

        val maxMainAxisSize = if (isVertical) constraints.maxHeight else constraints.maxWidth
        val mainAxisAvailableSize = maxMainAxisSize - beforeContentPadding - afterContentPadding
        val contentOffset = if (isVertical) {
            IntOffset(startContentPadding, beforeContentPadding)
        } else {
            IntOffset(beforeContentPadding, startContentPadding)
        }

        val horizontalPadding = contentPadding.run {
            calculateStartPadding(layoutDirection) + calculateEndPadding(layoutDirection)
        }.roundToPx()
        val verticalPadding = contentPadding.run {
            calculateTopPadding() + calculateBottomPadding()
        }.roundToPx()

        val pinnedItems = itemProvider.calculateLazyLayoutPinnedIndices(
            state.pinnedItems,
            state.beyondBoundsInfo
        )

        measureStaggeredGrid(
            state = state,
            pinnedItems = pinnedItems,
            itemProvider = itemProvider,
            resolvedSlots = resolvedSlots,
            constraints = constraints.copy(
                minWidth = constraints.constrainWidth(horizontalPadding),
                minHeight = constraints.constrainHeight(verticalPadding)
            ),
            mainAxisSpacing = mainAxisSpacing.roundToPx(),
            contentOffset = contentOffset,
            mainAxisAvailableSize = mainAxisAvailableSize,
            isVertical = isVertical,
            reverseLayout = reverseLayout,
            beforeContentPadding = beforeContentPadding,
            afterContentPadding = afterContentPadding,
            coroutineScope = coroutineScope
        ).also {
            state.applyMeasureResult(it)
        }
    }
}

private fun PaddingValues.startPadding(
    orientation: Orientation,
    layoutDirection: LayoutDirection
): Dp =
    when (orientation) {
        Orientation.Vertical -> calculateStartPadding(layoutDirection)
        Orientation.Horizontal -> calculateTopPadding()
    }

private fun PaddingValues.beforePadding(
    orientation: Orientation,
    reverseLayout: Boolean,
    layoutDirection: LayoutDirection
): Dp =
    when (orientation) {
        Orientation.Vertical ->
            if (reverseLayout) calculateBottomPadding() else calculateTopPadding()
        Orientation.Horizontal ->
            if (reverseLayout) {
                calculateEndPadding(layoutDirection)
            } else {
                calculateStartPadding(layoutDirection)
            }
    }

private fun PaddingValues.afterPadding(
    orientation: Orientation,
    reverseLayout: Boolean,
    layoutDirection: LayoutDirection
): Dp =
    when (orientation) {
        Orientation.Vertical ->
            if (reverseLayout) calculateTopPadding() else calculateBottomPadding()
        Orientation.Horizontal ->
            if (reverseLayout) {
                calculateStartPadding(layoutDirection)
            } else {
                calculateEndPadding(layoutDirection)
            }
    }
