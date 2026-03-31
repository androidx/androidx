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

import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.Range
import kotlin.math.abs
import kotlin.math.max

/**
 * A [LayoutStrategy] that arranges pages in a **single vertical column**.
 *
 * This strategy is used for the standard, single-page-per-row view of a PDF document, with pages
 * separated by [verticalPageSpacingPx].
 *
 * @property pageCount The total number of pages in the document.
 * @property verticalPageSpacingPx The vertical distance in pixels between pages.
 * @param topPageMarginPx The top margin in pixels applied before the first page.
 */
internal class SinglePageLayoutStrategy(
    val pageCount: Int,
    val verticalPageSpacingPx: Float,
    topPageMarginPx: Float = 0f,
) : LayoutStrategy {
    override val pagesPerRow: Int = 1
    override val maxWidth
        get() = _maxWidth

    override val totalHeight: Float
        get() = computeTotalHeight()

    /** The index of the last page for which dimensions have been successfully set. */
    var lastKnownPage: Int = -1
        private set

    /** The maximum width of a page row out of all pages whose dimensions are known. */
    private var _maxWidth: Float = 0f

    init {
        require(pageCount > 0) { "Empty PDF!" }
        require(verticalPageSpacingPx >= 0) { "Invalid vertical spacing!" }
    }

    /** The bottom position (y-coordinate) of each page known to this model */
    private val pageBottoms = FloatArray(pageCount) { 0f }

    /** The top position (y-coordinate) of each page known to this model */
    private val pageTops = FloatArray(pageCount) { 0f }.apply { this[0] = topPageMarginPx }

    /** Private constructor used by the Parcelable [CREATOR] to read the object's state. */
    private constructor(
        parcel: Parcel
    ) : this(pageCount = parcel.readInt(), verticalPageSpacingPx = parcel.readFloat()) {
        lastKnownPage = parcel.readInt()
        _maxWidth = parcel.readFloat()

        // Read the FloatArrays for pageBottoms and pageTops
        parcel.readFloatArray(pageBottoms)
        parcel.readFloatArray(pageTops)
    }

    /**
     * Sets the dimensions for a given page and updates the internal layout model.
     *
     * @param pageNum The index of the page whose dimensions are being set.
     * @param pageDimension of the [pageNum].
     */
    override fun setPagePositions(pageNum: Int, pageDimension: Dimension) {
        require(pageNum in 0 until pageCount) { "Page out of range" }
        require(pageDimension.y >= 0 && pageDimension.x >= 0) { "Negative size page" }
        _maxWidth = maxOf(_maxWidth, pageDimension.x.toFloat())

        when {
            pageNum < lastKnownPage -> correctLayoutForOutOfOrderPage(pageNum, pageDimension.y)

            pageNum > lastKnownPage + 1 -> approximateLayoutForGap(pageNum, pageDimension.y)

            else -> layoutPageSequentially(pageNum, pageDimension.y)
        }
    }

    /**
     * Calculates the range of pages currently visible within the given viewport.
     *
     * @param viewport The rectangle defining the visible area in document coordinates.
     * @param includePartial If `true`, pages even partially visible are included. If `false`, only
     *   fully visible pages are included.
     * @return A [PagesInViewport] object containing the range of visible page indices.
     */
    override fun getVisiblePages(viewport: RectF, includePartial: Boolean): PagesInViewport {
        val viewportBottom = viewport.bottom
        val viewportTop = viewport.top
        // If the top of the viewport exceeds the bottom of the last page whose dimensions are
        // known, return an empty range at the bottom of this model, and indicate that layout is
        // still in progress.
        val index = max(0, lastKnownPage)
        if (viewportTop > pageBottoms[index]) {
            return PagesInViewport(Range(index, index), layoutInProgress = true)
        }
        val startList = if (includePartial) pageBottoms else pageTops
        val endList = if (includePartial) pageTops else pageBottoms

        val rangeStart = abs(startList.binarySearch(viewportTop, 0, lastKnownPage + 1) + 1)
        val rangeEnd = abs(endList.binarySearch(viewportBottom, 0, lastKnownPage + 1) + 1) - 1

        if (rangeEnd < rangeStart) {
            // No page is entirely or partially visible. Find the page closest to the viewport's
            // center.
            val midPoint = (viewportTop + viewportBottom) / 2
            val midResult = pageTops.binarySearch(midPoint, 0, lastKnownPage + 1)
            val page = maxOf(abs(midResult + 1) - 1, 0)
            return PagesInViewport(Range(page, page))
        }

        return PagesInViewport(Range(rangeStart, rangeEnd))
    }

    /**
     * Calculates the [RectF] location of a given page in document coordinates, accounting for
     * horizontal alignment and scrolling relative to the viewport.
     *
     * @param viewport The rectangle defining the visible area. Used for horizontal centering or
     *   scrolling.
     * @param pageNum The index of the page to locate.
     * @param pageDimension The dimension of [pageNum] in the document.
     * @return The [RectF] representing the page's boundaries in document coordinates.
     */
    override fun getPageLocation(viewport: RectF, pageNum: Int, pageDimension: Dimension): RectF {
        var left = 0f
        var right: Float = _maxWidth
        val top = pageTops[pageNum]
        val bottom = top + pageDimension.y

        if (pageDimension.x < viewport.width()) {
            // Page is smaller than the view: center it horizontally.
            left = Math.max(left, viewport.left + (viewport.width() - pageDimension.x) / 2f)
        } else {
            // Page is larger than the view: manage horizontal scrolling.
            if (viewport.right > right) {
                // If the viewport has scrolled past the right edge of maxPageWidth, align page's
                // right edge to maxPageWidth.
                left = right - pageDimension.x
            } else if (viewport.left > left) {
                // Apply proportional horizontal scroll based on the ratio of max scroll available
                // for this page to the max scroll available for the layout's maxPageWidth.
                val maxScrollForLayout = right - viewport.width()
                if (maxScrollForLayout > 0) {
                    left = viewport.left * (right - pageDimension.x) / maxScrollForLayout
                }
            }
        }
        right = left + pageDimension.x

        return RectF(left, top, right, bottom)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Write attributes
        parcel.writeInt(pageCount)
        parcel.writeFloat(verticalPageSpacingPx)
        parcel.writeInt(lastKnownPage)
        parcel.writeFloat(_maxWidth)
        // Write arrays
        parcel.writeFloatArray(pageBottoms)
        parcel.writeFloatArray(pageTops)
    }

    /** Approximates the layout for any missing pages, then lays out the current page. */
    private fun approximateLayoutForGap(pageNum: Int, pageHeight: Int) {
        // Approximate the layout for the gap.
        for (pageIndex in lastKnownPage + 1 until pageNum) {
            layoutPageSequentially(pageIndex, pageHeight)
        }
        // After filling the gap, handle the current page sequentially.
        layoutPageSequentially(pageNum, pageHeight)
    }

    private fun computeTotalHeight(): Float {
        return if (lastKnownPage < 0) {
            0f
        } else if (lastKnownPage == pageCount - 1) {
            pageBottoms[lastKnownPage] + verticalPageSpacingPx
        } else {
            // Since all the pages are not yet loaded we need to make an estimate for now.
            val totalKnownHeight = pageBottoms[lastKnownPage]
            val lastKnownPageHeight = pageBottoms[lastKnownPage] - pageTops[lastKnownPage]
            val estimatedRemainingHeight =
                (lastKnownPageHeight + verticalPageSpacingPx) * (pageCount - lastKnownPage - 1)
            totalKnownHeight + estimatedRemainingHeight
        }
    }

    /** Corrects the layout when a page is loaded out of order. */
    private fun correctLayoutForOutOfOrderPage(pageNum: Int, pageHeight: Int) {
        val oldBottom = pageBottoms[pageNum]
        val newBottom = pageTops[pageNum] + pageHeight
        pageBottoms[pageNum] = newBottom

        val delta = newBottom - oldBottom
        // Only propagate if the height actually changed.
        if (delta != 0f) {
            shiftSubsequentPages(pageNum + 1, delta)
        }
    }

    private fun layoutPageSequentially(pageNum: Int, pageHeight: Int) {
        // Set the bottom position of current page.
        pageBottoms[pageNum] = pageTops[pageNum] + pageHeight
        // Set the top position for the next page.
        if (pageNum + 1 < pageCount) {
            pageTops[pageNum + 1] = pageBottoms[pageNum] + verticalPageSpacingPx
        }
        lastKnownPage = pageNum
    }

    /** Helper to handle the "Ripple Effect" of when pages are loaded async. */
    private fun shiftSubsequentPages(fromPageIndex: Int, delta: Float) {
        for (pageIndex in fromPageIndex..lastKnownPage) {
            pageTops[pageIndex] += delta
            pageBottoms[pageIndex] += delta
        }
        if (lastKnownPage + 1 < pageCount) {
            pageTops[lastKnownPage + 1] += delta
        }
    }

    companion object CREATOR : Parcelable.Creator<SinglePageLayoutStrategy> {
        override fun createFromParcel(parcel: Parcel): SinglePageLayoutStrategy {
            return SinglePageLayoutStrategy(parcel)
        }

        override fun newArray(size: Int): Array<SinglePageLayoutStrategy?> {
            return arrayOfNulls(size)
        }
    }
}
