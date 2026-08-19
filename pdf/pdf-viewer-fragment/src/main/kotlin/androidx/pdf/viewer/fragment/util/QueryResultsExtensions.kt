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

package androidx.pdf.viewer.fragment.util

import android.graphics.PointF
import androidx.pdf.PdfPoint
import androidx.pdf.search.model.QueryResults
import androidx.pdf.viewer.fragment.model.HighlightBound

/**
 * Calculate current index and total number of matches in a SparseArray.
 *
 * @return A pair where first value is current index and last value is total matches.
 */
internal fun QueryResults.Matched.fetchCounterData(): Pair<Int, Int> =
    Pair(
        resultBounds.getFlattenedIndex(
            queryResultsIndex.pageNum,
            queryResultsIndex.resultBoundsIndex,
        ),
        resultBounds.countTotalElements(),
    )

/**
 * Maps [QueryResults.Matched] state to [androidx.pdf.viewer.fragment.model.HighlightData].
 *
 * This would be used to add highlights on PdfView.
 */
internal fun QueryResults.Matched.toHighlightsData(): List<HighlightBound> {
    val totalCount = resultBounds.countTotalElements()
    val highlightBounds = ArrayList<HighlightBound>(totalCount)

    for (pageIndex in 0 until resultBounds.size()) {
        val pageNum = resultBounds.keyAt(pageIndex)
        val pageMatchBounds = resultBounds.valueAt(pageIndex)
        for (matchIndex in pageMatchBounds.indices) {
            highlightBounds.add(HighlightBound(pageNum, pageMatchBounds[matchIndex]))
        }
    }

    return highlightBounds
}

/**
 * Resolves the top-left [PdfPoint] location of the currently selected match in
 * [QueryResults.Matched].
 *
 * @return The [PdfPoint] of the current match, or `null` if no bounds exist.
 */
internal val QueryResults.Matched.currentMatchLocation: PdfPoint?
    get() {
        val pageMatches = resultBounds.get(queryResultsIndex.pageNum) ?: return null
        val matchBounds = pageMatches.getOrNull(queryResultsIndex.resultBoundsIndex) ?: return null
        val firstRect = matchBounds.bounds.firstOrNull() ?: return null
        return PdfPoint(queryResultsIndex.pageNum, PointF(firstRect.left, firstRect.top))
    }
