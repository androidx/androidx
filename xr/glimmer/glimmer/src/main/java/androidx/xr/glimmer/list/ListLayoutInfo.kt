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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastSumBy

/** Information about the layout of the [VerticalList]. */
public interface ListLayoutInfo {
    /** The list of [LazyListItemInfo] representing all the currently visible items. */
    public val visibleItemsInfo: List<LazyListItemInfo>

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Usually it is 0, but it can be negative if non-zero
     * [beforeContentPadding] was applied as the content displayed in the content padding area is
     * still visible.
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    public val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the list layout minus [beforeContentPadding].
     *
     * You can use it to understand what items from [visibleItemsInfo] are fully visible.
     */
    public val viewportEndOffset: Int

    /** The total count of items passed to [VerticalList]. */
    public val totalItemsCount: Int

    /**
     * The size of the viewport in pixels. It is the list layout size including all the content
     * paddings.
     */
    public val viewportSize: IntSize

    /** The orientation of the list. */
    public val orientation: Orientation
    /** True if the direction of scrolling and layout is reversed. */
    @get:Suppress("GetterSetterNames") public val reverseLayout: Boolean

    /**
     * The content padding in pixels applied before the first item in the direction of scrolling.
     */
    public val beforeContentPadding: Int

    /** The content padding in pixels applied after the last item in the direction of scrolling. */
    public val afterContentPadding: Int

    public val mainAxisItemSpacing: Int
}

internal fun ListLayoutInfo.visibleItemsAverageSize(): Int {
    val visibleItems = visibleItemsInfo
    val itemsSum = visibleItems.fastSumBy { it.size }
    return itemsSum / visibleItems.size + mainAxisItemSpacing
}

internal val ListLayoutInfo.singleAxisViewportSize: Int
    get() =
        if (orientation == Orientation.Vertical) {
            viewportSize.height
        } else {
            viewportSize.width
        }
