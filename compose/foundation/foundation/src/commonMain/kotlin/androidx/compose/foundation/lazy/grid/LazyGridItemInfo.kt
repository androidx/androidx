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

package androidx.compose.foundation.lazy.grid

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Contains useful information about an individual item in lazy grids like [LazyVerticalGrid].
 *
 * @see LazyGridLayoutInfo
 */
public sealed interface LazyGridItemInfo {
    /** The index of the item in the grid. */
    public val index: Int

    /** The key of the item which was passed to the item() or items() function. */
    public val key: Any

    /**
     * The offset of the item in pixels. It is relative to the top start of the lazy grid container.
     */
    public val offset: IntOffset

    /**
     * The row occupied by the top start point of the item. If this is unknown, for example while
     * this item is animating to exit the viewport and is still visible, the value will be
     * [UnknownRow].
     */
    public val row: Int

    /**
     * The column occupied by the top start point of the item. If this is unknown, for example while
     * this item is animating to exit the viewport and is still visible, the value will be
     * [UnknownColumn].
     */
    public val column: Int

    /**
     * The pixel size of the item. Note that if you emit multiple layouts in the composable slot for
     * the item then this size will be calculated as the max of their sizes.
     */
    public val size: IntSize

    /** The content type of the item which was passed to the item() or items() function. */
    public val contentType: Any?

    /**
     * The horizontal span of the item if it's in a [LazyVerticalGrid] or the vertical span if the
     * item is in a [LazyHorizontalGrid].
     *
     * Note, [LazyGridLayoutInfo.maxSpan] can be used to get the maximum number of spans in a line,
     * e.g., to check if the item is filling the whole line.
     */
    public val span: Int

    public companion object {
        /**
         * Possible value for [row], when they are unknown. This can happen when the item is visible
         * while animating to exit the viewport.
         */
        public const val UnknownRow: Int = -1
        /**
         * Possible value for [column], when they are unknown. This can happen when the item is
         * visible while animating to exit the viewport.
         */
        public const val UnknownColumn: Int = -1
    }
}

internal fun LazyGridItemInfo.lineIndex(orientation: Orientation): Int =
    if (orientation == Orientation.Vertical) {
        row
    } else {
        column
    }
