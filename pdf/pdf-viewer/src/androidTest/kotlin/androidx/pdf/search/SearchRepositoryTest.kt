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

package androidx.pdf.search

import android.util.SparseArray
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.search.model.NoQuery
import androidx.pdf.search.model.QueryResults
import androidx.pdf.view.FakePdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchRepositoryTest {

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
    fun testSearchDocument_resultsOnCurrentVisiblePage() = runTest {
        val fakeResults = createFakeSearchResults(1, 5, 5, 10)
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 5)

            var results = queryResults.value as QueryResults.Matched
            // Assert results exists on 3 pages
            assertEquals(3, results.resultBounds.size())
            assertEquals(5, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)

            // fetch next result
            produceNextResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to next result on same page
            assertEquals(5, results.queryResultsIndex.pageNum)
            assertEquals(1, results.queryResultsIndex.resultBoundsIndex)

            // fetch next result
            produceNextResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to next result on next page
            // in forward direction
            assertEquals(10, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)

            // fetch next result
            produceNextResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to next result cyclically
            assertEquals(1, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)

            // fetch previous result
            producePreviousResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to previous result cyclically
            assertEquals(10, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)
        }
    }

    @Test
    fun testSearchDocument_allResultsAfterCurrentVisiblePage() = runTest {
        val fakeResults = createFakeSearchResults(1, 5, 5, 10)
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 7)

            var results = queryResults.value as QueryResults.Matched
            // Assert results exists on 3 pages
            assertEquals(3, results.resultBounds.size())
            assertEquals(10, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)

            // fetch next result
            produceNextResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to next result cyclically
            assertEquals(1, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)
        }
    }

    @Test
    fun testSearchDocument_allResultsBeforeCurrentVisiblePage() = runTest {
        val fakeResults = createFakeSearchResults(1, 5, 5, 10)
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 11)

            var results = queryResults.value as QueryResults.Matched
            // Assert results exists on 3 pages
            assertEquals(3, results.resultBounds.size())
            // Assert selectedSearchResult point to next result cyclically
            assertEquals(1, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)

            // fetch next result
            produceNextResult()
            results = queryResults.value as QueryResults.Matched
            // Assert selectedSearchResult point to next result on next page
            assertEquals(5, results.queryResultsIndex.pageNum)
            assertEquals(0, results.queryResultsIndex.resultBoundsIndex)
        }
    }

    @Test
    fun testSearchDocument_noMatchingResults() = runTest {
        val fakeResults = createFakeSearchResults()
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 11)

            val results = queryResults.value

            // Assert no results returned
            assertTrue(results is QueryResults.NoMatch)
            assertEquals("test", (results as QueryResults.NoMatch).query)
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun testFindPrevOperation_noMatchingResults() = runTest {
        val fakeResults = createFakeSearchResults()
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 11)

            val results = queryResults.value
            assertTrue(results is QueryResults.NoMatch)

            // fetch previous result, should throw [NoSuchElementException]
            producePreviousResult()
        }
    }

    @Test(expected = NoSuchElementException::class)
    fun testFindNextOperation_noMatchingResults() = runTest {
        val fakeResults = createFakeSearchResults()
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 10)

            val results = queryResults.value
            assertTrue(results is QueryResults.NoMatch)

            // fetch next result, should throw [NoSuchElementException]
            produceNextResult()
        }
    }

    @Test
    fun testClearRepository() = runTest {
        val fakeResults = createFakeSearchResults(1, 5, 5, 10)
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            // search document
            produceSearchResults(query = "test", currentVisiblePage = 11)

            val results = queryResults.value as QueryResults.Matched
            assertEquals(3, results.resultBounds.size())

            // clear results
            clearSearchResults()

            // assert results are cleared
            assertTrue(queryResults.value is NoQuery)
        }
    }

    @Test
    fun test_searchDocument_withRestoreToSelectedIndex() = runTest {
        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        val fakePdfDocument = FakePdfDocument(searchResults = fakeResults)

        with(SearchRepository(fakePdfDocument)) {
            produceSearchResults(query = "test", currentVisiblePage = 10, resultIndex = 2)

            val results = queryResults.value as QueryResults.Matched
            assertEquals(5, results.resultBounds.size())
            assertEquals(10, results.queryResultsIndex.pageNum)
            assertEquals(2, results.queryResultsIndex.resultBoundsIndex)
        }
    }
}
