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
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
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

    @Test
    fun test_initCyclicIterator_startsEmpty() {
        // Iterator can now be initialized empty
        val iterator = CyclicSparseArrayIterator(visiblePage = 0)
        // Calling current() when empty returns -1 for pageNum
        assertEquals(-1, iterator.current().pageNum)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_onVisiblePage() {
        val searchResults = createFakeSearchResults(0, 0, 0, 1, 2, 2, 3, 3, 3)
        val visiblePage = 1

        val iterator = CyclicSparseArrayIterator(visiblePage)
        populateIterator(iterator, searchResults, visiblePage)

        // fetch current item
        val currentItem = iterator.current()

        assertEquals(1, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_onNextPage() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 3, 3, 3)
        val visiblePage = 1

        val iterator = CyclicSparseArrayIterator(visiblePage)
        populateIterator(iterator, searchResults, visiblePage)

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

        val iterator = CyclicSparseArrayIterator(visiblePage)
        populateIterator(iterator, searchResults, visiblePage)

        // fetch current item
        val currentItem = iterator.current()
        // assert currentItem is first item after rollover
        assertEquals(1, currentItem.pageNum)
        assertEquals(0, currentItem.resultBoundsIndex)
    }

    @Test
    fun test_getCurrentItem_withSearchResults_afterMovingToSpecificIndex_inBounds() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(visiblePage = 1)
        populateIterator(iterator, searchResults, 1)

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

        val iterator = CyclicSparseArrayIterator(visiblePage = 1)
        populateIterator(iterator, searchResults, 1)
        // try moving to an index greater than results available on page
        iterator.moveToIndex(3)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun test_getCurrentItem_withSearchResults_afterMovingToNegativeIndex() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(visiblePage = 1)
        populateIterator(iterator, searchResults, 1)
        // try moving to an invalid index
        iterator.moveToIndex(-1)
    }

    @Test
    fun test_getNextItems_withSearchResults() {
        val searchResults = createFakeSearchResults(0, 0, 0, 2, 2, 2, 3, 3, 3)

        val iterator = CyclicSparseArrayIterator(visiblePage = 1)
        populateIterator(iterator, searchResults, 1)
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

        val iterator = CyclicSparseArrayIterator(visiblePage = 1)
        populateIterator(iterator, searchResults, 1)
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

    @Test
    fun test_addMatches_appendsNewPage_preservesCurrentSelection() {
        val initialResults = createFakeSearchResults(2, 2)
        val iterator = CyclicSparseArrayIterator(visiblePage = 2)
        populateIterator(iterator, initialResults, 2)

        assertEquals(2, iterator.current().pageNum)
        assertEquals(0, iterator.current().resultBoundsIndex)

        // Append page 4 results
        val updatedResults = createFakeSearchResults(4, 4)
        populateIterator(iterator, updatedResults, 2)

        // Current item should remain unchanged
        assertEquals(2, iterator.current().pageNum)
        assertEquals(0, iterator.current().resultBoundsIndex)

        // Next should navigate through updated page results
        var nextItem = iterator.next()
        assertEquals(2, nextItem.pageNum)
        assertEquals(1, nextItem.resultBoundsIndex)

        nextItem = iterator.next()
        assertEquals(4, nextItem.pageNum)
        assertEquals(0, nextItem.resultBoundsIndex)
    }

    @Test
    fun test_addMatches_prependsNewPage_preservesCurrentSelection() {
        val initialResults = createFakeSearchResults(5, 5)
        val iterator = CyclicSparseArrayIterator(visiblePage = 5)
        populateIterator(iterator, initialResults, 5)
        iterator.moveToIndex(1)

        assertEquals(5, iterator.current().pageNum)
        assertEquals(1, iterator.current().resultBoundsIndex)

        // Prepend page 1 and append page 10
        val updatedResults = createFakeSearchResults(1, 10)
        populateIterator(iterator, updatedResults, 5)

        // Current item should still point to page 5 index 1
        assertEquals(5, iterator.current().pageNum)
        assertEquals(1, iterator.current().resultBoundsIndex)

        // Previous should go to page 5 index 0, then page 1
        var prevItem = iterator.prev()
        assertEquals(5, prevItem.pageNum)
        assertEquals(0, prevItem.resultBoundsIndex)

        prevItem = iterator.prev()
        assertEquals(1, prevItem.pageNum)
        assertEquals(0, prevItem.resultBoundsIndex)
    }

    private fun populateIterator(
        iterator: CyclicSparseArrayIterator,
        searchResults: SparseArray<List<PageMatchBounds>>,
        visiblePage: Int = 1,
    ) {
        if (searchResults.size() == 0) return
        val maxPageNum = searchResults.keyAt(searchResults.size() - 1)
        val totalPages = maxOf(visiblePage + 1, maxPageNum + 1)
        val clampedStart = visiblePage.coerceIn(0, totalPages - 1)
        val pageSequence = (clampedStart until totalPages) + (0 until clampedStart)

        for (pageNum in pageSequence) {
            val matches = searchResults.get(pageNum)
            if (matches != null && matches.isNotEmpty()) {
                iterator.addMatches(pageNum, matches)
            }
        }
    }
}
