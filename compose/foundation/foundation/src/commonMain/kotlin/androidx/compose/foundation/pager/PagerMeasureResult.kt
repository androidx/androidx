/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalFoundationApi::class)
internal class PagerMeasureResult(
    override val visiblePagesInfo: List<PageInfo>,
    override val pagesCount: Int,
    override val pageSize: Int,
    override val pageSpacing: Int,
    override val afterContentPadding: Int,
    override val orientation: Orientation,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val reverseLayout: Boolean,
    val consumedScroll: Float,
    val firstVisiblePage: MeasuredPage?,
    override val closestPageToSnapPosition: PageInfo?,
    val firstVisiblePageOffset: Int,
    val canScrollForward: Boolean,
    measureResult: MeasureResult,
) : PagerLayoutInfo, MeasureResult by measureResult {
    override val viewportSize: IntSize
        get() = IntSize(width, height)
    override val beforeContentPadding: Int get() = -viewportStartOffset
}