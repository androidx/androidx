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

import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope

/** The result of the measure pass of the [TransformingLazyColumn]. */
internal class TransformingLazyColumnMeasureResult(
    /** MeasureResult defining the layout. */
    measureResult: MeasureResult,
    /** The index of the item that should be considered as an anchor during scrolling. */
    val anchorItemIndex: Int,
    /** The offset of the anchor item from the top of screen. */
    val anchorItemScrollOffset: Int,
    /** Last known height for the anchor item or negative number if it hasn't been measured. */
    val lastMeasuredItemHeight: Int,
    /** Scope for animations. */
    val coroutineScope: CoroutineScope,
    /** Layout information for the visible items. */
    override val visibleItems: List<TransformingLazyColumnVisibleItemInfo>,
    /** see [TransformingLazyColumnLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** The spacing between items in the direction of scrolling. */
    val itemSpacing: Int,
    /** Density of the last measure. */
    val density: Density,
    /** True if there is some space available to continue scrolling in the forward direction. */
    var canScrollForward: Boolean,
    /** True if there is some space available to continue scrolling in the backward direction. */
    var canScrollBackward: Boolean,
) : TransformingLazyColumnLayoutInfo, MeasureResult by measureResult {
    /** see [TransformingLazyColumnLayoutInfo.viewportSize] */
    override val viewportSize: IntSize
        get() = IntSize(width = width, height = height)
}
