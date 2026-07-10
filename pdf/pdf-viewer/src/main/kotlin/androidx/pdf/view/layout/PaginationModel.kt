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

package androidx.pdf.view.layout

import android.os.Parcel
import android.os.Parcelable
import android.util.Range
import androidx.annotation.MainThread
import androidx.pdf.Dimension
import kotlin.math.max

/**
 * A data model for storing the dimensions of PDF pages.
 *
 * This class is not thread safe and should only be accessed from the main thread.
 */
@MainThread
@Suppress("BanParcelableUsage")
internal class PaginationModel(val numPages: Int) : Parcelable {

    init {
        require(numPages >= 0) { "Empty PDF!" }
    }

    private var _reach: Int = -1

    /** The last page whose dimensions are known to this model, 0-indexed */
    val reach: Int
        get() = _reach

    /** The dimensions of all pages known to this model */
    private val pages = Array(numPages) { UNKNOWN_SIZE }

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        _reach = parcel.readInt()
        parcel.readTypedArray(pages, Dimension.CREATOR)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(numPages)
        parcel.writeInt(_reach)
        parcel.writeTypedArray(pages, flags)
    }

    /** Adds [pageNum] to this model at [pageSize] */
    fun addPage(pageNum: Int, pageSize: Dimension) {
        require(pageNum in 0 until numPages) { "Page out of range" }
        require(pageSize.y >= 0 && pageSize.x >= 0) { "Negative size page" }
        // Edge case: missing pages.
        for (i in maxOf(_reach, 0) until pageNum) {
            if (pages[i] == UNKNOWN_SIZE) {
                pages[i] = pageSize
            }
        }
        pages[pageNum] = pageSize
        // Defensive: never set _reach to a smaller value, if pages are loaded out of order
        _reach = max(_reach, pageNum)
    }

    /** Returns the size of [pageNum] in content coordinates */
    fun getPageSize(pageNum: Int): Dimension {
        require(pageNum in 0 until numPages) { "Page out of range" }
        return pages[pageNum]
    }

    companion object {
        /** Sentinel value for the size of a page unknown to this model */
        val UNKNOWN_SIZE = Dimension(-1, -1)

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
