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

package androidx.pdf.view.layout

import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.Range
import kotlin.math.abs

/**
 * A [LayoutStrategy] that arranges pages in **rows of two pages**.
 *
 * @property pageCount The total number of pages in the document.
 * @property verticalPageSpacingPx The vertical distance in pixels between page rows.
 * @property horizontalPageSpacingPx The horizontal distance in pixels between the two pages in a
 *   row.
 * @param topPageMarginPx The top margin in pixels applied before the first page row.
 */
internal class TwoPageLayoutStrategy(
    val pageCount: Int,
    val verticalPageSpacingPx: Float,
    val horizontalPageSpacingPx: Float,
    topPageMarginPx: Float = 0f,
) : LayoutStrategy {
    override val pagesPerRow: Int = 2
    override val maxWidth: Float
        get() =
            if (pageCount == 1 || maxOddPageWidth == 0f) {
                maxEvenPageWidth
            } else {
                maxEvenPageWidth + horizontalPageSpacingPx + maxOddPageWidth
            }

    override val totalHeight: Float
        get() = computeTotalHeight()

    /** The index of the last page for which dimensions have been successfully set. */
    var lastKnownPage: Int = -1
        private set

    /** The index of the last page row for which dimensions have been set. */
    val lastKnownPageRow: Int
        get() = maxOf(0, lastKnownPage / pagesPerRow)

    /** The maximum width of an **even page** (left page) to ensure consistent alignment. */
    private var maxEvenPageWidth: Float = 0f

    /** The maximum width of an **odd page** (right page) to ensure consistent alignment. */
    private var maxOddPageWidth: Float = 0f

    /** The total number of page rows in this strategy. */
    private val rowCount: Int = Math.ceilDiv(pageCount, pagesPerRow)

    /** The width of the page row currently being laid out. */
    private var currentPageRowWidth = 0f

    init {
        require(pageCount > 0) { "Empty PDF!" }
        require(verticalPageSpacingPx >= 0) { "Invalid vertical spacing!" }
        require(horizontalPageSpacingPx >= 0) { "Invalid horizontal spacing!" }
    }

    /** The bottom position (y-coordinate) of each page row. */
    private val pageRowBottoms = FloatArray(rowCount) { 0f }

    /** The top position (y-coordinate) of each page row. */
    private val pageRowTops = FloatArray(rowCount) { 0f }.apply { this[0] = topPageMarginPx }

    /** Private constructor used by the Parcelable [CREATOR]. */
    private constructor(
        parcel: Parcel
    ) : this(
        pageCount = parcel.readInt(),
        verticalPageSpacingPx = parcel.readFloat(),
        horizontalPageSpacingPx = parcel.readFloat(),
    ) {
        lastKnownPage = parcel.readInt()
        maxEvenPageWidth = parcel.readFloat()
        maxOddPageWidth = parcel.readFloat()
        currentPageRowWidth = parcel.readFloat()

        parcel.readFloatArray(pageRowBottoms)
        parcel.readFloatArray(pageRowTops)
    }

    /**
     * Sets the dimensions for a given page and updates the internal layout model.
     *
     * @param pageNum The index of the page whose dimensions are being set.
     * @param pageDimension The dimension of [pageNum].
     */
    override fun setPagePositions(pageNum: Int, pageDimension: Dimension) {
        require(pageNum in 0 until pageCount) { "Page out of range" }
        require(pageDimension.y >= 0 && pageDimension.x >= 0) { "Negative size page" }
        if (pageNum.isEven()) {
            maxEvenPageWidth = maxOf(maxEvenPageWidth, pageDimension.x.toFloat())
        } else {
            maxOddPageWidth = maxOf(maxOddPageWidth, pageDimension.x.toFloat())
        }

        when {
            pageNum < lastKnownPage -> correctLayoutForOutOfOrderPage(pageNum, pageDimension.y)

            pageNum > lastKnownPage + 1 -> approximateLayoutForGap(pageNum, pageDimension.y)

            else -> layoutRowSequentially(pageNum, pageDimension.y)
        }
    }

    /**
     * Calculates the range of pages currently visible within the given viewport based on page rows.
     *
     * @param viewport The rectangle defining the visible area in document coordinates.
     * @param includePartial If `true`, partially visible page rows are included.
     * @return A [PagesInViewport] object containing the range of visible page indices.
     */
    override fun getVisiblePages(viewport: RectF, includePartial: Boolean): PagesInViewport {
        // If the viewport top is below the bottom of the last known page row,
        // indicate that layout is still in progress.
        if (viewport.top > pageRowBottoms[lastKnownPageRow]) {
            val startPage = lastKnownPageRow * pagesPerRow
            return PagesInViewport(
                Range(startPage, minOf(startPage + 1, pageCount - 1)),
                layoutInProgress = true,
            )
        }

        val startList = if (includePartial) pageRowBottoms else pageRowTops
        val endList = if (includePartial) pageRowTops else pageRowBottoms

        val rangeStartRow = abs(startList.binarySearch(viewport.top, 0, lastKnownPageRow + 1) + 1)
        val rangeEndRow =
            abs(endList.binarySearch(viewport.bottom, 0, lastKnownPageRow + 1) + 1) - 1

        if (rangeEndRow < rangeStartRow) {
            // No page row is visible. Find the page row closest to the viewport's center.
            val midPoint = (viewport.top + viewport.bottom) / 2
            val midResult = pageRowTops.binarySearch(midPoint, 0, lastKnownPageRow + 1)
            // Convert binary search result to the index of the closest row.
            val closestRow = maxOf(0, abs(midResult + 1) - 1)
            val startPage = closestRow * pagesPerRow
            // Return a range containing only the single closest page.
            return PagesInViewport(Range(startPage, minOf(startPage + 1, pageCount - 1)))
        }

        // Convert row indices to page indices.
        val startPage = rangeStartRow * pagesPerRow
        val endPage = minOf(rangeEndRow * pagesPerRow + 1, pageCount - 1)

        return PagesInViewport(Range(startPage, endPage))
    }

    /**
     * Calculates the [RectF] location of a given page in document coordinates.
     *
     * @param viewport The rectangle defining the visible area, used for horizontal centering.
     * @param pageNum The index of the page to locate.
     * @param pageDimension The dimension of [pageNum] in the document.
     * @return The [RectF] representing the page's boundaries in document coordinates.
     */
    override fun getPageLocation(viewport: RectF, pageNum: Int, pageDimension: Dimension): RectF {
        val top = pageRowTops[pageNum / pagesPerRow]
        val bottom = top + pageDimension.y

        // Center the two-page spread horizontally.
        val offset = maxOf(0f, (viewport.width() - maxWidth) / 2f)

        val left =
            if (pageNum.isEven()) {
                // Even page: Place on the left.
                offset + maxEvenPageWidth - pageDimension.x
            } else {
                // Odd page: Place on the right.
                offset + maxEvenPageWidth + horizontalPageSpacingPx
            }

        val right = left + pageDimension.x
        return RectF(left, top, right, bottom)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        // Write attributes
        parcel.writeInt(pageCount)
        parcel.writeFloat(verticalPageSpacingPx)
        parcel.writeFloat(horizontalPageSpacingPx)
        parcel.writeInt(lastKnownPage)
        parcel.writeFloat(maxEvenPageWidth)
        parcel.writeFloat(maxOddPageWidth)
        parcel.writeFloat(currentPageRowWidth)
        // Write arrays
        parcel.writeFloatArray(pageRowBottoms)
        parcel.writeFloatArray(pageRowTops)
    }

    /** Approximates the layout for any missing pages, then lays out the current page's row. */
    private fun approximateLayoutForGap(pageNum: Int, pageHeight: Int) {
        for (pageIndex in (lastKnownPage + 1) until pageNum) {
            // Approximate positions for missing page rows using current page's height
            layoutRowSequentially(pageIndex, pageHeight)
        }
        // After filling the gap, handle the current page sequentially.
        layoutRowSequentially(pageNum, pageHeight)
    }

    /**
     * Computes the total height of the document, estimating remaining height if pages are still
     * loading.
     */
    private fun computeTotalHeight(): Float {
        return when {
            lastKnownPage < 0 -> 0f
            lastKnownPage == pageCount - 1 ->
                pageRowBottoms[lastKnownPageRow] + verticalPageSpacingPx
            else -> {
                // Since all the pages are not yet loaded we need to make an estimate for now.
                val totalKnownHeight = pageRowBottoms[lastKnownPageRow]
                val lastKnownPageRowHeight =
                    pageRowBottoms[lastKnownPageRow] - pageRowTops[lastKnownPageRow]
                val estimatedRemainingHeight =
                    (lastKnownPageRowHeight + verticalPageSpacingPx) *
                        (rowCount - lastKnownPageRow - 1)
                totalKnownHeight + estimatedRemainingHeight
            }
        }
    }

    /** Corrects the layout when a page is loaded out of order. */
    private fun correctLayoutForOutOfOrderPage(pageNum: Int, pageHeight: Int) {
        val pageRowNum = pageNum / pagesPerRow
        val oldRowBottom = pageRowBottoms[pageRowNum]
        val newRowBottom = maxOf(pageRowBottoms[pageRowNum], pageRowTops[pageRowNum] + pageHeight)
        pageRowBottoms[pageRowNum] = newRowBottom

        val delta = newRowBottom - oldRowBottom
        // Only propagate if the row height actually changed.
        if (delta != 0f) {
            shiftSubsequentPageRows(pageRowNum + 1, delta)
        }
    }

    private fun layoutRowSequentially(pageNum: Int, pageHeight: Int) {
        val pageRowNum = pageNum / pagesPerRow
        // Row height is determined by the taller page.
        pageRowBottoms[pageRowNum] =
            maxOf(pageRowBottoms[pageRowNum], pageRowTops[pageRowNum] + pageHeight)
        // Set the top position for the next page row.
        if (pageRowNum + 1 < rowCount) {
            pageRowTops[pageRowNum + 1] = pageRowBottoms[pageRowNum] + verticalPageSpacingPx
        }
        lastKnownPage = pageNum
    }

    /** Helper to handle the "Ripple Effect" of when pages are loaded async. */
    private fun shiftSubsequentPageRows(fromPageRowIndex: Int, delta: Float) {
        for (pageRowIndex in fromPageRowIndex..lastKnownPageRow) {
            pageRowTops[pageRowIndex] += delta
            pageRowBottoms[pageRowIndex] += delta
        }
        if (lastKnownPageRow + 1 < rowCount) {
            pageRowTops[lastKnownPageRow + 1] += delta
        }
    }

    /** Returns true if the number is even. */
    private fun Int.isEven() = this % 2 == 0

    companion object CREATOR : Parcelable.Creator<TwoPageLayoutStrategy> {
        override fun createFromParcel(parcel: Parcel): TwoPageLayoutStrategy {
            return TwoPageLayoutStrategy(parcel)
        }

        override fun newArray(size: Int): Array<TwoPageLayoutStrategy?> {
            return arrayOfNulls(size)
        }
    }
}
