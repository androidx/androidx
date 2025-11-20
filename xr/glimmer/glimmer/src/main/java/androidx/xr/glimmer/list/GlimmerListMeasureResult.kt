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
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize

internal class GlimmerListMeasureResult(
    /** The new first visible item. */
    val firstVisibleItem: GlimmerListMeasuredListItem?,
    /** The new value for [ListState.firstVisibleItemScrollOffset]. */
    val firstVisibleItemScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction. */
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass. */
    val consumedScroll: Float,
    /** MeasureResult defining the layout. */
    private val measureResult: MeasureResult,
    /** True when extra remeasure is required. */
    val remeasureNeeded: Boolean,
    /** Density of the last measure. */
    val density: Density,
    /** Constraints used to measure children. */
    val childConstraints: Constraints,
    // properties representing the info needed for [ListLayoutInfo]:
    /** see [ListLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<GlimmerListMeasuredListItem>,
    /** see [ListLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [ListLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [ListLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [ListLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [ListLayoutInfo.orientation] */
    override val orientation: Orientation,
    override val afterContentPadding: Int,
    override val mainAxisItemSpacing: Int,
) : ListLayoutInfo, MeasureResult by measureResult {

    val canScrollBackward
        get() = (firstVisibleItem?.index ?: 0) != 0 || firstVisibleItemScrollOffset != 0

    override val viewportSize: IntSize
        get() = IntSize(width, height)

    override val beforeContentPadding: Int
        get() = -viewportStartOffset
}
