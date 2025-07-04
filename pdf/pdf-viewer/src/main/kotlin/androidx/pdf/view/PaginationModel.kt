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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import android.util.Range
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import java.util.Collections
import kotlin.math.abs
import kotlin.math.max

/**
 * Stores the size and position of PDF pages. All dimensions and coordinates should be assumed to be
 * content coordinates and not View coordinates, unless symbol names and / or comments indicate
 * otherwise. Not thread safe; access only from the main thread.
 *
 * TODO(b/376135419) - Adapt implementation to work when page dimensions are not loaded sequentially
 */
@MainThread
@Suppress("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PaginationModel(
    val pageSpacingPx: Float,
    val numPages: Int,
    topPageMarginPx: Float = 0f,
) : Parcelable {

    init {
        require(numPages >= 0) { "Empty PDF!" }
        require(pageSpacingPx >= 0) { "Invalid spacing!" }
    }

    /** The estimated total height of this PDF */
    val totalEstimatedHeight: Float
        get() = computeEstimatedHeight()

    /** The maximum width of any page known to this model */
    var maxWidth: Float = 0f

    private var _reach: Int = -1

    /** The last page whose dimensions are known to this model, 0-indexed */
    val reach: Int
        get() = _reach

    /** The dimensions of all pages known to this model, as [Point] */
    private val pages = Array(numPages) { UNKNOWN_SIZE }

    /** The top position of each page known to this model */
    private val pagePositions = FloatArray(numPages) { -1f }.apply { this[0] = topPageMarginPx }

    /**
     * The estimated height of any page not known to this model, i.e. the average height of all
     * pages known to this model
     */
    private var averagePageHeight = 0

    /** The total height, excluding page spacing, of all pages known to this model. */
    private var accumulatedPageHeight = 0

    /**
     * The top position of each page known to this model, as a synthetic [List] to conveniently use
     * with APIs like [Collections.binarySearch]
     */
    private val pageTops: List<Float> =
        object : AbstractList<Float>() {
            override val size: Int
                get() = reach + 1

            override fun get(index: Int): Float {
                return pagePositions[index]
            }
        }

    /**
     * The bottom position of each page known to this model, as a synthetic [List] to conveniently
     * use with APIs like [Collections.binarySearch]
     */
    private val pageBottoms: List<Float> =
        object : AbstractList<Float>() {
            override val size: Int
                get() = reach + 1

            override fun get(index: Int): Float {
                return pagePositions[index] + pages[index].y
            }
        }

    constructor(parcel: Parcel) : this(parcel.readFloat(), parcel.readInt()) {
        _reach = parcel.readInt()
        averagePageHeight = parcel.readInt()
        accumulatedPageHeight = parcel.readInt()
        maxWidth = parcel.readFloat()
        parcel.readFloatArray(pagePositions)
        parcel.readTypedArray(pages, Point.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(pageSpacingPx)
        parcel.writeInt(numPages)
        parcel.writeInt(_reach)
        parcel.writeInt(averagePageHeight)
        parcel.writeInt(accumulatedPageHeight)
        parcel.writeFloat(maxWidth)
        parcel.writeFloatArray(pagePositions)
        parcel.writeTypedArray(pages, flags)
    }

    /** Adds [pageNum] to this model at [pageSize] */
    fun addPage(pageNum: Int, pageSize: Point) {
        require(pageNum in 0 until numPages) { "Page out of range" }
        require(pageSize.y >= 0 && pageSize.x >= 0) { "Negative size page" }
        // Edge case: missing pages. This model expects pages to be added sequentially
        for (i in maxOf(_reach, 0) until pageNum) {
            if (pages[i] == UNKNOWN_SIZE) {
                pages[i] = pageSize

                // Use prior values as an approximation.
                accumulatedPageHeight += pageSize.y
                if (i > 0) {
                    pagePositions[i] = pagePositions[i - 1] + pages[i - 1].y + pageSpacingPx
                }
            }
        }
        if (pageSize.x > maxWidth) {
            maxWidth = pageSize.x.toFloat()
        }
        pages[pageNum] = pageSize
        // Defensive: never set _reach to a smaller value, if pages are loaded out of order
        _reach = max(_reach, pageNum)
        accumulatedPageHeight += pageSize.y
        averagePageHeight = accumulatedPageHeight / maxOf(_reach + 1, 1)

        if (pageNum > 0) {
            pagePositions[pageNum] =
                pagePositions[pageNum - 1] + pages[pageNum - 1].y + pageSpacingPx
        }
    }

    /** Returns the size of [pageNum] in content coordinates */
    fun getPageSize(pageNum: Int): Point {
        require(pageNum in 0 until numPages) { "Page out of range" }
        return pages[pageNum]
    }

    /**
     * Returns a [Range] between the first and last pages that should be visible between
     * [viewportTop] and [viewportBottom], which are expected to be the top and bottom content
     * coordinates of the viewport.
     */
    fun getPagesInViewport(
        viewportTop: Float,
        viewportBottom: Float,
        includePartial: Boolean = true,
    ): PagesInViewport {
        // If the top of the viewport exceeds the bottom of the last page whose dimensions are
        // known, return an empty range at the bottom of this model, and indicate that layout is
        // still in progress.
        val index = max(0, reach)
        if (viewportTop > pageBottoms[index]) {
            return PagesInViewport(Range(index, index), layoutInProgress = true)
        }
        val startList = if (includePartial) pageBottoms else pageTops
        val endList = if (includePartial) pageTops else pageBottoms

        val rangeStart = abs(startList.binarySearch(viewportTop) + 1)
        val rangeEnd = abs(endList.binarySearch(viewportBottom) + 1) - 1

        if (rangeEnd < rangeStart) {
            // No page is entirely visible.
            val midPoint = (viewportTop + viewportBottom) / 2
            val midResult = pageTops.binarySearch(midPoint)
            val page = maxOf(abs(midResult + 1) - 1, 0)
            return PagesInViewport(Range(page, page))
        }

        return PagesInViewport(Range(rangeStart, rangeEnd))
    }

    /** Returns the location of the page in content coordinates */
    fun getPageLocation(pageNum: Int, viewport: RectF): RectF {
        val page = pages[pageNum]
        var left = 0f
        var right: Float = maxWidth
        val top = pagePositions[pageNum]
        val bottom = top + page.y
        // this page is smaller than the view's width, it may slide left or right.
        if (page.x < viewport.width()) {
            // page is smaller than the view: center
            left = Math.max(left, viewport.left + (viewport.width() - page.x) / 2f)
        } else {
            // page is larger than view: scroll proportionally
            if (viewport.right > right) {
                left = right - page.x
            } else if (viewport.left > left) {
                left = viewport.left * (right - page.x) / (right - viewport.width())
            }
        }
        right = left + page.x

        return RectF(left, top, right, bottom)
    }

    private fun computeEstimatedHeight(): Float {
        return if (_reach < 0) {
            0f
        } else if (_reach == numPages - 1) {
            val lastPageHeight = pages[_reach].y
            pagePositions[_reach] + lastPageHeight + (pageSpacingPx)
            // Otherwise, we have to guess
        } else {
            val totalKnownHeight = pagePositions[_reach] + pages[_reach].y
            val estimatedRemainingHeight =
                (averagePageHeight + pageSpacingPx) * (numPages - _reach - 1)
            totalKnownHeight + estimatedRemainingHeight
        }
    }

    companion object {
        /** Sentinel value for the size of a page unknown to this model */
        val UNKNOWN_SIZE = Point(-1, -1)

        @JvmField
        val CREATOR: Parcelable.Creator<PaginationModel> =
            object : Parcelable.Creator<PaginationModel> {
                override fun createFromParcel(parcel: Parcel): PaginationModel {
                    return PaginationModel(parcel)
                }

                override fun newArray(size: Int): Array<PaginationModel?> {
                    return arrayOfNulls(size)
                }
            }
    }

    override fun describeContents(): Int {
        return 0
    }
}

/**
 * Encapsulates our understanding of the currently-visible PDF pages, including whether this is a
 * best guess during layout or the accurate set of pages.
 */
internal data class PagesInViewport(
    /** The set of pages that are currently visible */
    val pages: Range<Int>,
    /**
     * True if we're actively laying out pages to reach the current scroll position, and [pages]
     * represents a best guess at the set of pages that are currently visible (i.e. instead of
     * precisely the pages that we know are visible).
     *
     * This will be false in most cases except those that involve jumping far ahead in the PDF (e.g.
     * fast scroll or programmatic changes in position)
     */
    val layoutInProgress: Boolean = false,
)
