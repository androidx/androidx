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

package androidx.pdf.search

import android.util.SparseArray
import androidx.pdf.content.PageMatchBounds
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CyclicSparseArrayIteratorTest {

    /**
     * Creates fake search results and combine them in [SparseArray].
     *
     * @param matches: page number where a match is found.
     */
    private fun createFakeSearchResults(vararg matches: Int): SparseArray<List<PageMatchBounds>> {
        val results: SparseArray<List<PageMatchBounds>> = SparseArray()
        matches.forEach { pageNum ->
            val newPageResult =
                results.get(pageNum, listOf()).toMutableList().also {
                    it.add(PageMatchBounds(bounds = listOf(), textStartIndex = 0))
                }
            results.append(pageNum, newPageResult)
        }
        return results
    }

    @Test(expected = IllegalArgumentException::class)
    fun test_initCyclicIterator_withEmptyResults() {
        val searchResults = SparseArray<List<PageMatchBounds>>()
        // Try init iterator with empty results; should throw IllegalArgumentException
        CyclicSparseArrayIterator(searchResults, visiblePage = 0)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_onVisiblePage() {
        val searchResults = createFakeSearchResults(0, 0, 0, 1, 2, 2, 3, 3, 3)
        val visiblePage = 1

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage)
        // fetch current item
        val currentItem = iterator.current()

        assertEquals(1, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_onNextPage() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 3, 3, 3)
        val visiblePage = 1

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage)
        // fetch current item
        val currentItem = iterator.current()

        assertEquals(2, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_onPreviousPages() {
        val searchResults = createFakeSearchResults(1, 1, 2, 2, 3, 3, 3)
        // select a page ahead of search results
        val visiblePage = 5

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage)
        // fetch current item
        val currentItem = iterator.current()
        // assert currentItem is first item after rollover
        assertEquals(1, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_afterMovingToSpecificIndex_inBounds() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage = 1)
        // try moving to index in bounds
        iterator.moveToIndex(2)
        // fetch current item
        val currentItem = iterator.current()

        assertEquals(2, currentItem.pageNum)
        assertEquals(2, currentItem.resultBoundsIndex)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun test_getCurrentItem_withSearchResults_afterMovingToSpecificIndex_outOfBounds() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage = 1)
        // try moving to an index greater than results available on page
        iterator.moveToIndex(3)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun test_getCurrentItem_withSearchResults_afterMovingToNegativeIndex() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage = 1)
        // try moving to an invalid index
        iterator.moveToIndex(-1)
    }

    @Test
    fun test_getNextItems_withSearchResults() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage = 1)
        // fetch next item
        var currentItem = iterator.next()
        assertEquals(2, currentItem.pageNum)
        assertEquals(1, currentItem.resultBoundsIndex)

        // fetch next item
        currentItem = iterator.next()
        assertEquals(2, currentItem.pageNum)
        assertEquals(2, currentItem.resultBoundsIndex)
        // roll over results
        repeat(4) { currentItem = iterator.next() }
        assertEquals(0, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getPrevItems_withSearchResults() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(searchResults, visiblePage = 1)
        // fetch prev item
        var currentItem = iterator.prev()
        assertEquals(0, currentItem.pageNum)
        assertEquals(2, currentItem.resultBoundsIndex)

        // fetch prev item
        currentItem = iterator.prev()
        assertEquals(0, currentItem.pageNum)
        assertEquals(1, currentItem.resultBoundsIndex)
        // roll over results
        repeat(3) { currentItem = iterator.prev() }
        assertEquals(3, currentItem.pageNum)
        assertEquals(1, currentItem.resultBoundsIndex)
    }
}
