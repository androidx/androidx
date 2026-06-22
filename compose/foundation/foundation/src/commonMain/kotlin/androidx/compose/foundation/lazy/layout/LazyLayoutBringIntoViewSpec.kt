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

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Creates and remembers a [StickyHeaderBringIntoViewSpec] that wraps
 * `LocalBringIntoViewSpec.current` and takes the main axis sticky header size into account when
 * calculating scroll distance.
 *
 * @param stickyItemsCombinedSizeLambda A lambda that returns the combined size of all sticky items
 *   that are currently sticking to the start of the layout.
 * @param reverseLayout Whether the layout is reversed.
 */
@Composable
internal fun rememberLazyLayoutBringIntoViewSpec(
    reverseLayout: Boolean,
    isVertical: Boolean,
    stickyItemsCombinedSizeLambda: () -> Int,
): BringIntoViewSpec {
    val currentBringIntoViewSpec = LocalBringIntoViewSpec.current
    val layoutDirection = LocalLayoutDirection.current
    return remember(
        stickyItemsCombinedSizeLambda,
        reverseLayout,
        layoutDirection,
        currentBringIntoViewSpec,
        isVertical,
    ) {
        StickyHeaderBringIntoViewSpec(
            stickyItemsCombinedSizeLambda,
            reverseLayout,
            layoutDirection,
            isVertical,
            currentBringIntoViewSpec,
        )
    }
}

private class StickyHeaderBringIntoViewSpec(
    private val stickyItemsCombinedSizeLambda: () -> Int,
    private val reverseLayout: Boolean,
    private val layoutDirection: LayoutDirection,
    private val isVertical: Boolean,
    private val bringIntoViewSpec: BringIntoViewSpec,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val nonStickyItemsOffset = stickyItemsCombinedSizeLambda().toFloat()
        // If we are horizontal with a (!reverse && Ltr) or (reverse && Rtl) then we must offset
        // because reversing Rtl is equivalent to regular Ltr.
        // If we are vertical, we must offset if we are not reversed.
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val applyStickyOffset = reverseLayout == (!isVertical && isRtl)
        val validOffset = if (applyStickyOffset) offset - nonStickyItemsOffset else offset
        return bringIntoViewSpec.calculateScrollDistance(
            offset = validOffset,
            size = size,
            containerSize = containerSize - nonStickyItemsOffset,
        )
    }
}
