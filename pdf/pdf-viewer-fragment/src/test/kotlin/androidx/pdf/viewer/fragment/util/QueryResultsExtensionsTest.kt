/*
 * Copyright 2026 The Android Open Source Project
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
import android.graphics.RectF
import android.util.SparseArray
import androidx.pdf.PdfPoint
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.search.model.QueryResults
import androidx.pdf.search.model.QueryResultsIndex
import androidx.pdf.viewer.fragment.model.HighlightBound
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class QueryResultsExtensionsTest {

    private fun createMatch(
        left: Float = 10f,
        top: Float = 20f,
        right: Float = 30f,
        bottom: Float = 40f,
        textStartIndex: Int = 0,
    ): PageMatchBounds =
        PageMatchBounds(
            bounds = listOf(RectF(left, top, right, bottom)),
            textStartIndex = textStartIndex,
        )

    @Test
    fun fetchCounterData_returnsCurrentFlattenedIndexAndTotalMatches() {
        val resultBounds =
            SparseArray<List<PageMatchBounds>>().apply {
                put(1, listOf(createMatch(textStartIndex = 0), createMatch(textStartIndex = 5)))
                put(3, listOf(createMatch(textStartIndex = 10)))
            }

        // Match on page 1, index 0 -> counter: (0, 3)
        val matched1 =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 1, resultBoundsIndex = 0),
            )
        assertThat(matched1.fetchCounterData()).isEqualTo(Pair(0, 3))

        // Match on page 1, index 1 -> counter: (1, 3)
        val matched2 =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 1, resultBoundsIndex = 1),
            )
        assertThat(matched2.fetchCounterData()).isEqualTo(Pair(1, 3))

        // Match on page 3, index 0 -> counter: (2, 3)
        val matched3 =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 3, resultBoundsIndex = 0),
            )
        assertThat(matched3.fetchCounterData()).isEqualTo(Pair(2, 3))
    }

    @Test
    fun toHighlightsData_convertsSparseArrayToFlattenedHighlightBoundsList() {
        val match1 = createMatch(left = 10f, top = 20f, textStartIndex = 0)
        val match2 = createMatch(left = 30f, top = 40f, textStartIndex = 5)
        val match3 = createMatch(left = 50f, top = 60f, textStartIndex = 10)

        val resultBounds =
            SparseArray<List<PageMatchBounds>>().apply {
                put(0, listOf(match1, match2))
                put(2, listOf(match3))
            }

        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..3,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 0, resultBoundsIndex = 0),
            )

        val highlights = matched.toHighlightsData()
        assertThat(highlights)
            .containsExactly(
                HighlightBound(pageNum = 0, pageMatchBounds = match1),
                HighlightBound(pageNum = 0, pageMatchBounds = match2),
                HighlightBound(pageNum = 2, pageMatchBounds = match3),
            )
            .inOrder()
    }

    @Test
    fun toHighlightsData_emptyResults_returnsEmptyList() {
        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..3,
                resultBounds = SparseArray(),
                queryResultsIndex = QueryResultsIndex(pageNum = 0, resultBoundsIndex = 0),
            )

        assertThat(matched.toHighlightsData()).isEmpty()
    }

    @Test
    fun currentMatchLocation_validSelection_returnsPdfPointWithTopLeftCoordinates() {
        val match = createMatch(left = 15f, top = 25f, right = 50f, bottom = 60f)
        val resultBounds = SparseArray<List<PageMatchBounds>>().apply { put(2, listOf(match)) }

        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 2, resultBoundsIndex = 0),
            )

        val location = matched.currentMatchLocation
        assertThat(location).isEqualTo(PdfPoint(2, PointF(15f, 25f)))
    }

    @Test
    fun currentMatchLocation_pageNotFound_returnsNull() {
        val resultBounds =
            SparseArray<List<PageMatchBounds>>().apply { put(1, listOf(createMatch())) }

        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 3, resultBoundsIndex = 0),
            )

        assertThat(matched.currentMatchLocation).isNull()
    }

    @Test
    fun currentMatchLocation_indexOutOfBoundsOnPage_returnsNull() {
        val resultBounds =
            SparseArray<List<PageMatchBounds>>().apply { put(1, listOf(createMatch())) }

        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 1, resultBoundsIndex = 5),
            )

        assertThat(matched.currentMatchLocation).isNull()
    }

    @Test
    fun currentMatchLocation_emptyBoundsInPageMatch_returnsNull() {
        val emptyBoundsMatch = PageMatchBounds(bounds = emptyList(), textStartIndex = 0)
        val resultBounds =
            SparseArray<List<PageMatchBounds>>().apply { put(1, listOf(emptyBoundsMatch)) }

        val matched =
            QueryResults.Matched(
                query = "test",
                pageRange = 0..5,
                resultBounds = resultBounds,
                queryResultsIndex = QueryResultsIndex(pageNum = 1, resultBoundsIndex = 0),
            )

        assertThat(matched.currentMatchLocation).isNull()
    }
}
