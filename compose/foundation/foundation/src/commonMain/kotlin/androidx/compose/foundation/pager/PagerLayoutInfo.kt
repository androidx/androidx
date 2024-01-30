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
import androidx.compose.ui.unit.IntSize

/**
 * Contains useful information about the currently displayed layout state of a [Pager]. This
 * information is available after the first measure pass.
 *
 * Use [PagerState.layoutInfo] to retrieve this
 */
@ExperimentalFoundationApi
sealed interface PagerLayoutInfo {
    /**
     * A list of all pages that are currently visible in the [Pager]
     */
    val visiblePagesInfo: List<PageInfo>

    /**
     * The size of the Pages in this [Pager] provided by the [PageSize] API in the Pager definition.
     */
    val pageSize: Int

    /**
     * The spacing provided in the [Pager] creation.
     */
    val pageSpacing: Int

    /**
     * The start offset of the layout's viewport in pixels. You can think of it as a minimum offset
     * which would be visible. Usually it is 0, but it can be negative if non-zero
     * beforeContentPadding was applied as the content displayed in the content padding area is
     * still visible.
     *
     * You can use it to understand what items from [visiblePagesInfo] are fully visible.
     */
    val viewportStartOffset: Int

    /**
     * The end offset of the layout's viewport in pixels. You can think of it as a maximum offset
     * which would be visible. It is the size of the lazy list layout minus [beforeContentPadding].
     *
     * You can use it to understand what items from [visiblePagesInfo] are fully visible.
     */
    val viewportEndOffset: Int

    /**
     * The content padding in pixels applied before the first page in the direction of scrolling.
     * For example it is a top content padding for [VerticalPager] with reverseLayout set to false.
     */
    val beforeContentPadding: Int

    /**
     * The content padding in pixels applied after the last page in the direction of scrolling.
     * For example it is a bottom content padding for [VerticalPager] with reverseLayout set to
     * false.
     */
    val afterContentPadding: Int

    /**
     * The size of the viewport in pixels. It is the [Pager] layout size including all the
     * content paddings.
     */
    val viewportSize: IntSize

    /**
     * The [Pager] orientation.
     */
    val orientation: Orientation

    /**
     * True if the direction of scrolling and layout is reversed.
     */
    @Suppress("GetterSetterNames")
    @get:Suppress("GetterSetterNames")
    val reverseLayout: Boolean

    /**
     * Pages to compose and layout before and after the list of visible pages. This does not include
     * the pages automatically composed and laid out by the pre-fetcher in the direction of the
     * scroll during scroll events.
     */
    val beyondBoundsPageCount: Int
}

@ExperimentalFoundationApi
internal val PagerLayoutInfo.mainAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width
