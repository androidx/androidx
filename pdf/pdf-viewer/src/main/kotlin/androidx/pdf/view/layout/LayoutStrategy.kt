/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.view.layout

import android.graphics.Point
import android.graphics.RectF
import android.os.Parcelable

/**
 * An alias for [android.graphics.Point] used to semantically represent the width and height of a
 * PDF page.
 */
internal typealias Dimension = Point

/**
 * Defines a strategy for calculating page positions and determining visible areas within a PDF
 * viewer.
 *
 * This interface enables different layout modes (e.g., single-page vertical scroll, horizontal
 * carousel, or two-page spread/book view) by abstracting the coordinate system and visibility
 * calculations.
 */
internal interface LayoutStrategy : Parcelable {
    /**
     * The number of pages displayed side-by-side in a single row within this layout.
     *
     * This is typically 1 for single page layout or 2 for a book layout.
     */
    val pagesPerRow: Int

    /** The maximum width of a page row out of all page rows whose dimensions are known. */
    val maxWidth: Float

    /** The total height of PDF content with current layout strategy. */
    val totalHeight: Float

    /**
     * Calculates and stores the layout position for a specific page.
     *
     * This is typically called once for each page when its dimensions become known. The
     * implementation uses this information to build its internal map of page coordinates.
     *
     * @param pageNum The zero-based index of the page to position.
     * @param pageDimension The measured size (width and height) of the page.
     */
    fun setPagePositions(pageNum: Int, pageDimension: Dimension)

    /**
     * Returns the range of page numbers currently visible within the given viewport area.
     *
     * @param viewport The [RectF] representing the visible bounds in content coordinates (scrolled
     *   space).
     * @param includePartial If true, pages only partially within the viewport are included.
     * @return A [PagesInViewport] object containing the range of visible pages.
     */
    fun getVisiblePages(viewport: RectF, includePartial: Boolean): PagesInViewport

    /**
     * Returns the content coordinate location [RectF] for a specific page.
     *
     * This location is calculated relative to the entire content scroll area, *not* the screen.
     *
     * @param viewport The current visible viewport (used as context for certain layouts).
     * @param pageNum The zero-based index of the page to locate.
     * @param pageDimension The dimension of [pageNum] in the document.
     * @return A [RectF] defining the page's position (left, top, right, bottom) within the content
     *   area.
     */
    fun getPageLocation(viewport: RectF, pageNum: Int, pageDimension: Dimension): RectF
}
