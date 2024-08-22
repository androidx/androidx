/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.ui.unit.IntSize

/**
 * Scroll progress of an item in a [LazyColumn] before any modifications to the item's height are
 * applied (using [LazyColumnItemScope.transformedHeight] modifier).
 */
sealed interface LazyColumnItemScrollProgress {
    /**
     * The offset as a fraction of the top of the item relative to the list container. Is within
     * (0, 1) when item is inside the screen and could be negative if the top of the item is off the
     * screen. Value is calculated from the top of the container. This value is calculated before
     * any height modifications are applied (using [LazyColumnItemScope.transformedHeight]
     * modifier).
     */
    val topOffsetFraction: Float

    /**
     * The offset as a fraction of the bottom of the item relative to the list container. Is within
     * (0, 1) when item is inside the screen and could exceed 1 when the bottom of item is off the
     * screen. Value is calculated from the top of the container. This value is calculated before
     * any height modifications are applied (using [LazyColumnItemScope.transformedHeight]
     * modifier).
     */
    val bottomOffsetFraction: Float
}

/** Represents an item that is visible in the [LazyColumn] component. */
sealed interface LazyColumnVisibleItemInfo {
    /** The index of the item in the underlying data source. */
    val index: Int

    /** The offset of the item from the start of the visible area. */
    val offset: Int

    /** The height of the item after applying any height changes. */
    val height: Int

    /** The scroll progress of the item, indicating its position within the visible area. */
    val scrollProgress: LazyColumnItemScrollProgress
}

/** Holds the layout information for a [LazyColumn]. */
sealed interface LazyColumnLayoutInfo {

    /** A list of [LazyColumnVisibleItemInfo] objects representing the visible items in the list. */
    val visibleItems: List<LazyColumnVisibleItemInfo>

    /** The total count of items passed to [LazyColumn]. */
    val totalItemsCount: Int

    /** The size of the viewport in pixels. */
    val viewportSize: IntSize
}
