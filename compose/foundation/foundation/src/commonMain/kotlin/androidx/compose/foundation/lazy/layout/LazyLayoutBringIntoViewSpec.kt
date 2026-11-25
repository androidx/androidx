/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

internal interface LazyLayoutViewportInsets {
    val beforeContentPadding: Int
    val afterContentPadding: Int
    val stickingItemsCombinedSize: Int
        get() = 0
}

/**
 * Creates and remembers a [LazyLayoutBringIntoViewSpec] wrapping [LocalBringIntoViewSpec].
 *
 * Accounts for main-axis content padding and sticky item insets from [layoutInfo] when calculating
 * bring-into-view scroll distance.
 *
 * @param reverseLayout whether the layout is reversed
 * @param isVertical whether the layout scrolls vertically
 * @param layoutInfo returns the current main-axis content padding and sticky item insets
 */
@Composable
internal fun rememberLazyLayoutBringIntoViewSpec(
    reverseLayout: Boolean,
    isVertical: Boolean,
    layoutInfo: () -> LazyLayoutViewportInsets,
): BringIntoViewSpec {
    val currentBringIntoViewSpec = LocalBringIntoViewSpec.current
    val isLayoutAxisReversed =
        reverseLayout xor (!isVertical && LocalLayoutDirection.current == LayoutDirection.Rtl)
    return remember(layoutInfo, isLayoutAxisReversed, currentBringIntoViewSpec) {
        LazyLayoutBringIntoViewSpec(
            isLayoutAxisReversed = isLayoutAxisReversed,
            delegate = currentBringIntoViewSpec,
            layoutInfo = layoutInfo,
        )
    }
}

internal class LazyLayoutBringIntoViewSpec(
    private val isLayoutAxisReversed: Boolean,
    private val delegate: BringIntoViewSpec,
    private val layoutInfo: () -> LazyLayoutViewportInsets,
) : BringIntoViewSpec by delegate {
    @OptIn(ExperimentalFoundationApi::class)
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val info = layoutInfo()
        if (!ComposeFoundationFlags.isLazyLayoutBringIntoViewContentPaddingEnabled) {
            val nonStickyItemsOffset = info.stickingItemsCombinedSize.toFloat()
            val validOffset = if (!isLayoutAxisReversed) offset - nonStickyItemsOffset else offset
            return delegate.calculateScrollDistance(
                offset = validOffset,
                size = size,
                containerSize = containerSize - nonStickyItemsOffset,
            )
        }
        val beforeInset = maxOf(info.beforeContentPadding, info.stickingItemsCombinedSize, 0)
        val afterInset = info.afterContentPadding.coerceAtLeast(0)
        val leadingInset = (if (isLayoutAxisReversed) afterInset else beforeInset).toFloat()
        val trailingInset = (if (isLayoutAxisReversed) beforeInset else afterInset).toFloat()
        val totalInset = leadingInset + trailingInset
        val activeStart: Float
        val activeContainerSize: Float
        if (containerSize > totalInset) {
            activeStart = leadingInset
            activeContainerSize = containerSize - totalInset
        } else {
            activeStart = 0f
            activeContainerSize = containerSize
        }
        return delegate.calculateScrollDistance(
            offset = offset - activeStart,
            size = size,
            containerSize = activeContainerSize,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyLayoutBringIntoViewSpec) return false
        return layoutInfo === other.layoutInfo &&
            isLayoutAxisReversed == other.isLayoutAxisReversed &&
            delegate == other.delegate
    }

    override fun hashCode(): Int {
        var result = layoutInfo.hashCode()
        result = 31 * result + isLayoutAxisReversed.hashCode()
        result = 31 * result + delegate.hashCode()
        return result
    }
}
