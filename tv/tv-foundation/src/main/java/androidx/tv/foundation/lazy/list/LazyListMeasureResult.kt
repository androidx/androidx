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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

/**
 * The result of the measure pass for lazy list layout.
 */
internal class LazyListMeasureResult(
    // properties defining the scroll position:
    /** The new first visible item.*/
    val firstVisibleItem: LazyListMeasuredItem?,
    /** The new value for [TvLazyListState.firstVisibleItemScrollOffset].*/
    val firstVisibleItemScrollOffset: Int,
    /** True if there is some space available to continue scrolling in the forward direction.*/
    val canScrollForward: Boolean,
    /** The amount of scroll consumed during the measure pass.*/
    val consumedScroll: Float,
    /** MeasureResult defining the layout.*/
    measureResult: MeasureResult,
    /** The amount of scroll-back that happened due to reaching the end of the list. */
    val scrollBackAmount: Float,
    // properties representing the info needed for LazyListLayoutInfo:
    /** see [TvLazyListLayoutInfo.visibleItemsInfo] */
    override val visibleItemsInfo: List<TvLazyListItemInfo>,
    /** see [TvLazyListLayoutInfo.viewportStartOffset] */
    override val viewportStartOffset: Int,
    /** see [TvLazyListLayoutInfo.viewportEndOffset] */
    override val viewportEndOffset: Int,
    /** see [TvLazyListLayoutInfo.totalItemsCount] */
    override val totalItemsCount: Int,
    /** see [TvLazyListLayoutInfo.reverseLayout] */
    override val reverseLayout: Boolean,
    /** see [TvLazyListLayoutInfo.orientation] */
    override val orientation: Orientation,
    /** see [TvLazyListLayoutInfo.afterContentPadding] */
    override val afterContentPadding: Int,
    /** see [TvLazyListLayoutInfo.mainAxisItemSpacing] */
    override val mainAxisItemSpacing: Int
) : TvLazyListLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset
}
