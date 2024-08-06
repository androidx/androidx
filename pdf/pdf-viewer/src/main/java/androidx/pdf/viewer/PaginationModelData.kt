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

package androidx.pdf.viewer

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.pdf.models.Dimensions
import kotlinx.parcelize.Parcelize

/** Parcelable representation of the data stored by [PaginationModel] */
@Parcelize
@SuppressLint("BanParcelableUsage")
internal data class PaginationModelData(
    /** The space between pages in pixels */
    val pageSpacingPx: Int,
    /** The maximum number of pages in the model, i.e. the number of pages in the PDF */
    val maxPages: Int,
    /** The dimensions of all pages the model has received dimensions for, in content coordinates */
    val pages: Array<Dimensions>,
    /**
     * The bottom position of each page, in content coordinates. Derived from [pages] and
     * [pageSpacingPx], but stored separately to avoid re-computation.
     */
    val pageStops: IntArray,
    /** The number of pages for which dimensions are known */
    val size: Int,
    /**
     * The estimated height of the next PDF page, based on known dimensions. Derived from [pages]
     * and [size], but stored separately to avoid re-computation.
     */
    val estimatedPageHeight: Float,
    /**
     * The cumulative height of all pages for which dimensions are known. Derived from [pages], but
     * stored separately to avoid re-computation.
     */
    val accumulatedPageSize: Int,
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PaginationModelData

        if (pageSpacingPx != other.pageSpacingPx) return false
        if (maxPages != other.maxPages) return false
        if (!pages.contentEquals(other.pages)) return false
        if (!pageStops.contentEquals(other.pageStops)) return false
        if (size != other.size) return false
        if (estimatedPageHeight != other.estimatedPageHeight) return false
        if (accumulatedPageSize != other.accumulatedPageSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageSpacingPx
        result = 31 * result + maxPages
        result = 31 * result + pages.contentHashCode()
        result = 31 * result + pageStops.contentHashCode()
        result = 31 * result + size
        result = 31 * result + estimatedPageHeight.hashCode()
        result = 31 * result + accumulatedPageSize
        return result
    }
}
