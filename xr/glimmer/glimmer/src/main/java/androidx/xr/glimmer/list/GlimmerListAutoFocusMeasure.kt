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

package androidx.xr.glimmer.list

import androidx.collection.IntList
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Density
import kotlin.math.abs

internal fun ListLayoutProperties.applyMeasureResult(
    state: ListState,
    itemsCount: Int,
    measuredItemProvider: GlimmerListMeasuredItemProvider,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    pinnedIndices: IntList,
    reverseLayout: Boolean,
    density: Density,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
): MeasureResult {
    return if (state.canScrollForward || state.canScrollBackward) {
        applyMeasureResultWithAutoFocus(
            state = state,
            itemsCount = itemsCount,
            measuredItemProvider = measuredItemProvider,
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
            pinnedIndices = pinnedIndices,
            reverseLayout = reverseLayout,
            density = density,
            layout = layout,
        )
    } else {
        // Non-scrollable
        val measureResult =
            measureGlimmerList(
                itemsCount = itemsCount,
                measuredItemProvider = measuredItemProvider,
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
                scrollToBeConsumed = state.scrollToBeConsumed,
                pinnedIndices = pinnedIndices,
                reverseLayout = reverseLayout,
                density = density,
                layout = layout,
            )
        state.autoFocusBehaviour.applyAutoFocusProperties(null)
        state.applyMeasureResult(
            result = measureResult,
            consumedScroll = 0f,
            accumulatedScroll = 0f,
        )
        measureResult
    }
}

private fun ListLayoutProperties.applyMeasureResultWithAutoFocus(
    state: ListState,
    itemsCount: Int,
    measuredItemProvider: GlimmerListMeasuredItemProvider,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    pinnedIndices: IntList,
    reverseLayout: Boolean,
    density: Density,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
): MeasureResult {
    // The user-dispatched scroll (ΔSu) is shared between the scroll of the content (ΔSc) and the
    // moving focus line (ΔSf). The proportion is not constant and depends on the state of the list.
    // This method calculates a proper share of `ΔSu = ΔSc + ΔSf`.
    val expectedContentScrollDelta =
        convertUserScrollDeltaToContentScrollDelta(
            properties = state.autoFocusBehaviour.properties,
            userScrollToBeConsumed = state.scrollToBeConsumed,
        )

    // Here's the original logic, with a modified input - the content scroll (ΔSc) is passed instead
    // of the user-dispatched scroll (ΔSu).
    val measureResult =
        measureGlimmerList(
            itemsCount = itemsCount,
            measuredItemProvider = measuredItemProvider,
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
            scrollToBeConsumed = expectedContentScrollDelta,
            pinnedIndices = pinnedIndices,
            reverseLayout = reverseLayout,
            density = density,
            layout = layout,
        )

    // Calculates new auto focus properties based on the latest list measure result.
    val autoFocusProperties =
        calculateAutoFocusProperties(layoutProperties = this, measureResult = measureResult)

    val consumedContentScrollDelta = measureResult.consumedScroll
    val unconsumedContentDelta = expectedContentScrollDelta - consumedContentScrollDelta

    val consumedScroll =
        when {
            // Reports that we consume all because we accumulated it for the next pass.
            abs(expectedContentScrollDelta) <= 0.5f -> state.scrollToBeConsumed
            // Reports that we consumed all, since we actually did — except for rounding errors.
            abs(unconsumedContentDelta) <= 0.5f -> state.scrollToBeConsumed
            // Content didn't consume all, so return a real consumpted part.
            else -> measureResult.consumedScroll
        }

    val accumulatedScroll =
        when {
            // We pretend that we consume all, but we will actually use it in the next pass.
            abs(expectedContentScrollDelta) <= 0.5f -> state.scrollToBeConsumed
            // We consume all, but let's accumulate errors after roundings.
            abs(unconsumedContentDelta) <= 0.5f -> unconsumedContentDelta
            // There was more scroll than we could even consume, so no accumulation remained.
            else -> 0f
        }

    // Save auto focus measure result for the next pass.
    state.autoFocusBehaviour.applyAutoFocusProperties(autoFocusProperties)

    state.applyMeasureResult(
        result = measureResult,
        consumedScroll = consumedScroll,
        accumulatedScroll = accumulatedScroll,
    )

    return measureResult
}

private fun convertUserScrollDeltaToContentScrollDelta(
    properties: GlimmerListAutoFocusProperties?,
    userScrollToBeConsumed: Float,
): Float {
    if (properties == null) {
        return userScrollToBeConsumed
    }

    // Forward scroll is negative, backward scroll is positive, so we need to invert it.
    val dSu = -userScrollToBeConsumed
    val prevSu = properties.userScroll
    val prevSc = properties.contentScroll

    val nextSu = prevSu + dSu
    val nextSc =
        AutoFocusScrollConverter.convertUserScrollToContentScroll(
            userScroll = nextSu,
            properties = properties,
        )

    val dSc = nextSc - prevSc

    // Restore the original sign.
    return -dSc
}

// TODO: b/431258694 - Support reverse scrolling.
private fun calculateAutoFocusProperties(
    layoutProperties: ListLayoutProperties,
    measureResult: GlimmerListMeasureResult,
): GlimmerListAutoFocusProperties? {
    if (!measureResult.canScrollForward && !measureResult.canScrollBackward) {
        return null
    }

    val viewportSize = layoutProperties.mainAxisAvailableSize.toFloat()
    val scrollThreshold = viewportSize * ProportionalThresholdFactor
    val contentLength =
        measureResult.visibleItemsAverageSize() * measureResult.totalItemsCount -
            measureResult.mainAxisItemSpacing.toFloat()

    val contentScroll = getTotalContentScrollDistance(measureResult).toFloat()

    val userScroll =
        AutoFocusScrollConverter.convertContentScrollToUserScroll(
            contentScroll = contentScroll,
            scrollThreshold = scrollThreshold,
            viewportSize = viewportSize,
            contentLength = contentLength,
        )

    val focusScroll = userScroll - contentScroll

    return GlimmerListAutoFocusProperties(
        userScroll = userScroll,
        focusScroll = focusScroll,
        contentScroll = contentScroll,
        contentLength = contentLength,
        scrollThreshold = scrollThreshold,
        layoutProperties = layoutProperties,
    )
}

/**
 * Returns a content scroll (Sc) value - the distance between the start of the content and list
 * viewport's top edge.
 */
private fun getTotalContentScrollDistance(measureResult: GlimmerListMeasureResult): Int {
    val firstVisibleItem = measureResult.visibleItemsInfo.first()
    val nonRenderedItemSizes =
        safeMultiply(measureResult.visibleItemsAverageSize(), firstVisibleItem.index)
    return nonRenderedItemSizes - firstVisibleItem.offset
}

/** Prevents integer overflow. */
private fun safeMultiply(a: Int, b: Int): Int {
    if (a == 0 || b == 0) return 0

    val result = a * b
    if (result / b == a) {
        return result
    }

    return if ((a > 0) == (b > 0)) Int.MAX_VALUE else Int.MIN_VALUE
}
