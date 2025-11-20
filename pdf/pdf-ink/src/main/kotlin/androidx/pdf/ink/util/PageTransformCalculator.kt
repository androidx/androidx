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

package androidx.pdf.ink.util

import android.graphics.Matrix
import android.graphics.RectF
import android.util.SparseArray

/** Calculates transformation matrices for PDF pages based on viewport information. */
internal class PageTransformCalculator {

    /**
     * Calculates the transformation matrices for the visible pages.
     *
     * @param firstVisiblePage The index of the first visible page.
     * @param visiblePagesCount The number of visible pages.
     * @param pageLocations A sparse array mapping page numbers to their locations in the view.
     * @param zoomLevel The current zoom level of the PDF view.
     * @return A [Map] of transformation matrices for the visible pages.
     */
    fun calculate(
        firstVisiblePage: Int,
        visiblePagesCount: Int,
        pageLocations: SparseArray<RectF>,
        zoomLevel: Float,
    ): Map<Int, Matrix> {
        val pageMatrices = mutableMapOf<Int, Matrix>()
        val lastVisiblePage = firstVisiblePage + visiblePagesCount

        for (currentPage in firstVisiblePage until lastVisiblePage) {
            val currentPageLocation: RectF? = pageLocations.get(currentPage)
            currentPageLocation?.let { location ->
                val transformationMatrix =
                    Matrix().apply {
                        postScale(zoomLevel, zoomLevel)
                        postTranslate(location.left, location.top)
                    }
                pageMatrices.put(currentPage, transformationMatrix)
            }
        }
        return pageMatrices.toMap()
    }
}
