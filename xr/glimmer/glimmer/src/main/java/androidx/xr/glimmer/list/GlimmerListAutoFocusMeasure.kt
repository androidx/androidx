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
    val measureResult =
        if (state.autoFocusState.isAutoFocusEnabled) {
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
            applyMeasureResultWithoutAutoFocus(
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
        }

    // Calculates new autofocus properties based on the latest list measure result.
    val autoFocusProperties =
        calculateAutoFocusProperties(layoutProperties = this, measureResult = measureResult)

    // Keep the autofocus state updated, even if it is disabled. If the user switches back to
    // non-direct input, they will have the correct numbers for calculating the focus position.
    state.autoFocusState.applyAutoFocusProperties(autoFocusProperties)

    return measureResult
}

/** Applies user scroll as-is without any autofocus adjustments (ΔSu == ΔSc). */
private fun ListLayoutProperties.applyMeasureResultWithoutAutoFocus(
    state: ListState,
    itemsCount: Int,
    measuredItemProvider: GlimmerListMeasuredItemProvider,
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    pinnedIndices: IntList,
    reverseLayout: Boolean,
    density: Density,
    layout: (Int, Int, Placeable.PlacementScope.() -> Unit) -> MeasureResult,
): GlimmerListMeasureResult {
    val incomingScroll = state.incomingScroll
    val scrollToBeConsumed = incomingScroll + state.carryOverScroll

    val measureResult =
        measureGlimmerList(
            itemsCount = itemsCount,
            measuredItemProvider = measuredItemProvider,
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
            scrollToBeConsumed = scrollToBeConsumed,
            pinnedIndices = pinnedIndices,
            reverseLayout = reverseLayout,
            density = density,
            layout = layout,
        )

    val unconsumedScroll = scrollToBeConsumed - measureResult.consumedScroll
    val consumedScroll =
        when {
            // Reports that we consume all because we carry it over to the next pass.
            abs(scrollToBeConsumed) <= 0.5f -> incomingScroll
            // Reports that we consumed all, since we actually did — except for rounding errors.
            abs(unconsumedScroll) <= 0.5f -> incomingScroll
            // Content didn't consume all, so return a real consumed part.
            else -> measureResult.consumedScroll
        }

    val scrollToCarryOver =
        when {
            // We pretend that we consume all, but we will actually use it in the next pass.
            abs(scrollToBeConsumed) <= 0.5f -> scrollToBeConsumed
            // We consume all, but let's save errors after roundings for successor passes.
            abs(unconsumedScroll) <= 0.5f -> unconsumedScroll
            // There was more scroll than we could even consume, so no need to carry over.
            else -> 0f
        }

    state.applyMeasureResult(
        result = measureResult,
        consumedScroll = consumedScroll,
        scrollToCarryOver = scrollToCarryOver,
    )

    return measureResult
}

/** Splits user-dispatched scroll between focus and content scroll (ΔSu = ΔSc + ΔSf). */
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
): GlimmerListMeasureResult {
    val incomingScroll = state.incomingScroll
    val scrollToBeConsumed = incomingScroll + state.carryOverScroll

    // The user-dispatched scroll (ΔSu) is shared between the scroll of the content (ΔSc)
    // and the moving focus line (ΔSf). The proportion is not constant and depends on the
    // state of the list. This method calculates a proper share of `ΔSu = ΔSc + ΔSf`.
    val expectedContentScrollDelta =
        convertUserScrollDeltaToContentScrollDelta(
            properties = state.autoFocusState.properties,
            userScrollToBeConsumed = scrollToBeConsumed,
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

    val prevAutoFocusProperties = state.autoFocusState.properties
    val expectedFocusScrollDelta = scrollToBeConsumed - expectedContentScrollDelta

    val consumedContentScrollDelta = measureResult.consumedScroll
    val consumedFocusScrollDelta =
        calculateConsumedFocusDelta(consumedContentScrollDelta, prevAutoFocusProperties)

    val unconsumedContentDelta = expectedContentScrollDelta - consumedContentScrollDelta
    val unconsumedFocusDelta = expectedFocusScrollDelta - consumedFocusScrollDelta

    val consumedScroll =
        when {
            // Reports that we consume all because we carry it over to the next pass.
            abs(expectedContentScrollDelta) <= 0.5f -> incomingScroll
            // Reports that we consumed all, since we actually did — except for rounding errors.
            abs(unconsumedContentDelta) <= 0.5f -> incomingScroll
            // Content didn't consume all, so return a real consumed part.
            else -> consumedContentScrollDelta + consumedFocusScrollDelta
        }

    val scrollToCarryOver =
        when {
            // We pretend that we consume all, but we will actually use it in the next pass.
            abs(expectedContentScrollDelta) <= 0.5f -> scrollToBeConsumed
            // We consume all, but let's save errors after roundings for successor passes.
            abs(unconsumedContentDelta) <= 0.5f -> unconsumedContentDelta + unconsumedFocusDelta
            // There was more scroll than we could even consume, so no need to carry over.
            else -> 0f
        }

    state.applyMeasureResult(
        result = measureResult,
        consumedScroll = consumedScroll,
        scrollToCarryOver = scrollToCarryOver,
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

/** Uses the previous measure results to correctly calculate how much focus scroll was consumed. */
private fun calculateConsumedFocusDelta(
    consumedContentDelta: Float,
    prevAutoFocusProperties: GlimmerListAutoFocusProperties?,
): Float {
    // If there is no previous measurements, assume that focus consumed everything.
    if (prevAutoFocusProperties == null) {
        return consumedContentDelta
    }
    // Forward scroll is negative, backward scroll is positive, so we need to invert it.
    val dSc = -consumedContentDelta
    val nextSc = prevAutoFocusProperties.contentScroll + dSc
    val nextSu =
        AutoFocusScrollConverter.convertContentScrollToUserScroll(
            // Uses the new real position of the content that contains rounding errors.
            contentScroll = nextSc,
            // Uses the previous measurement results that contains the _old_ estimation error.
            properties = prevAutoFocusProperties,
        )
    val nextSf = nextSu - nextSc
    val dSf = nextSf - prevAutoFocusProperties.focusScroll
    // Restore the original sign.
    return -dSf
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
