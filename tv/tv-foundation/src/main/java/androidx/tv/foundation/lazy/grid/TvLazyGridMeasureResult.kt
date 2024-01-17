/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

/**
 * The result of the measure pass for lazy list layout.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TvLazyGridMeasureResult(
    // properties defining the scroll position:
    /** The new first visible line of items.*/
    val firstVisibleLine: LazyGridMeasuredLine?,
    /** The new value for [TvLazyGridState.firstVisibleItemScrollOffset].*/
    val firstVisibleLineScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    val consumedScroll: Float,
    /** MeasureResult defining the layout.*/
    measureResult: MeasureResult,
    // properties representing the info needed for LazyListLayoutInfo:
    /** see [TvLazyGridLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<TvLazyGridItemInfo>,
    /** see [TvLazyGridLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [TvLazyGridLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [TvLazyGridLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [TvLazyGridLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [TvLazyGridLayoutInfo.orientation] */
    override val orientation: Orientation,
    /** see [TvLazyGridLayoutInfo.afterContentPadding] */
    override val afterContentPadding: Int,
    /** see [TvLazyGridLayoutInfo.mainAxisItemSpacing] */
    override val mainAxisItemSpacing: Int
) : TvLazyGridLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset
}
