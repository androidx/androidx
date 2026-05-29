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

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.DeadObjectException
import android.os.RemoteException
import android.util.SparseArray
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.KeyedPdfObject
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.ocr.FakeOcrProvider
import androidx.pdf.ocr.FakeOcrResult
import androidx.pdf.ocr.OcrText
import androidx.pdf.search.model.NoQuery
import androidx.pdf.search.model.QueryResults
import androidx.pdf.view.FakePdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun produceSearchResults_onHandledRemoteException_updatesToNoQuery() = runTest {
        val remoteException =
            RemoteException("android.os.RemoteException: Method searchDocument is unimplemented.")
        val pdfDocument = FakePdfDocument(exceptionToThrow = remoteException)

        with(SearchRepository(pdfDocument)) {
            produceSearchResults(query = "test", currentVisiblePage = 0)
            assertTrue(queryResults.value is NoQuery)
        }
    }

    @Test
    fun produceSearchResults_onDeadObjectException_updatesToNoQuery() = runTest {
        val pdfDocument = FakePdfDocument(exceptionToThrow = DeadObjectException())

        with(SearchRepository(pdfDocument)) {
            produceSearchResults(query = "test", currentVisiblePage = 0)
            assertTrue(queryResults.value is NoQuery)
        }
    }

    @Test(expected = RemoteException::class)
    fun produceSearchResults_onUnhandledRemoteException_throws() = runTest {
        val pdfDocument = FakePdfDocument(exceptionToThrow = RemoteException())
        with(SearchRepository(pdfDocument)) {
            produceSearchResults(query = "test", currentVisiblePage = 0)
        }
    }

    @Test
    fun testSearchDocument_withOcrResults() = runTest {
        val pageNum = 0
        val query = "ocr"

        // Setup OCR Provider
        val ocrBounds = Rect(10, 10, 50, 50)
        val ocrText = OcrText(query, listOf(ocrBounds))
        val ocrResult = FakeOcrResult(words = listOf(ocrText))
        val ocrProvider = FakeOcrProvider(ocrResult)

        // Setup PDF Document with Image Object
        val imageBounds = RectF(0f, 0f, 50f, 50f)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(
                pages = listOf(Point(100, 100)),
                pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject)),
            )

        val searchRepository = SearchRepository(fakePdfDocument)
        searchRepository.setOcrProvider(ocrProvider)

        // Perform Search
        searchRepository.produceSearchResults(query, currentVisiblePage = pageNum)

        // Verify results
        val results = searchRepository.queryResults.value as QueryResults.Matched
        assertEquals(1, results.resultBounds.size())
        val matchBounds = results.resultBounds.get(pageNum)[0]

        assertEquals(1, matchBounds.bounds.size)
        assertEquals(5f, matchBounds.bounds[0].left)
        assertEquals(5f, matchBounds.bounds[0].top)
        assertEquals(25f, matchBounds.bounds[0].right)
        assertEquals(25f, matchBounds.bounds[0].bottom)
    }

    @Test
    fun testSearchDocument_mergesNativeAndOcrResults() = runTest {
        val pageNum = 0

        // 1. Setup Native Results
        // One result at top 20
        val nativeMatch = PageMatchBounds(listOf(RectF(0f, 20f, 100f, 30f)), textStartIndex = 0)
        val nativeResults =
            SparseArray<List<PageMatchBounds>>().apply { put(pageNum, listOf(nativeMatch)) }

        // 2. Setup OCR Results
        // One result at top 10 (should come before native)
        // One result at top 40 (should come after native)
        val ocrResult1 = OcrText("ocr1", listOf(Rect(0, 10, 100, 15)))
        val ocrResult2 = OcrText("ocr2", listOf(Rect(0, 40, 100, 45)))
        val ocrResult = FakeOcrResult(words = listOf(ocrResult1, ocrResult2))
        val ocrProvider = FakeOcrProvider(ocrResult)

        // 3. Setup PDF Document
        val imageBounds = RectF(0f, 0f, 100f, 100f)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(
                pages = listOf(Point(100, 100)),
                searchResults = nativeResults,
                pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject)),
            )

        val searchRepository = SearchRepository(fakePdfDocument)
        searchRepository.setOcrProvider(ocrProvider)

        // 4. Perform Search
        searchRepository.produceSearchResults("ocr", currentVisiblePage = pageNum)

        // 5. Verify merged and sorted results
        val results = searchRepository.queryResults.value as QueryResults.Matched
        val matches = results.resultBounds.get(pageNum)
        assertEquals(3, matches.size)

        // Sorted by top: 10, 20, 40
        assertEquals(10f, matches[0].bounds[0].top)
        assertEquals(20f, matches[1].bounds[0].top)
        assertEquals(40f, matches[2].bounds[0].top)
    }

    @Test
    fun testSearchDocument_noOcrProvider_onlyNativeResults() = runTest {
        val pageNum = 0
        val query = "test"

        // 1. Setup Native Results
        val nativeMatch = PageMatchBounds(listOf(RectF(0f, 20f, 100f, 30f)), textStartIndex = 0)
        val nativeResults =
            SparseArray<List<PageMatchBounds>>().apply { put(pageNum, listOf(nativeMatch)) }

        // 2. Setup PDF Document with Image Object (but no OCR provider)
        val imageBounds = RectF(0f, 0f, 100f, 100f)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(
                pages = listOf(Point(100, 100)),
                searchResults = nativeResults,
                pageObjectsPerPage = mapOf(pageNum to listOf(keyedObject)),
            )

        // No OCR provider set
        val searchRepository = SearchRepository(fakePdfDocument)

        // 3. Perform Search
        searchRepository.produceSearchResults(query, currentVisiblePage = pageNum)

        // 4. Verify only native results are returned
        val results = searchRepository.queryResults.value as QueryResults.Matched
        val matches = results.resultBounds.get(pageNum)
        assertEquals(1, matches.size)
        assertEquals(20f, matches[0].bounds[0].top)
    }

    @Test
    fun testSearchDocument_preservesNativeResultsWhenNoOcrMatchesOnPage() = runTest {
        val pageNum = 0
        val pageNum1 = 1

        // 1. Native results on page 0
        val nativeMatch = PageMatchBounds(listOf(RectF(0f, 20f, 100f, 30f)), textStartIndex = 0)
        val nativeResults =
            SparseArray<List<PageMatchBounds>>().apply { put(pageNum, listOf(nativeMatch)) }

        // 2. OCR results on page 1
        val ocrText = OcrText("ocr", listOf(Rect(0, 10, 100, 15)))
        val ocrResult = FakeOcrResult(words = listOf(ocrText))
        val ocrProvider = FakeOcrProvider(ocrResult)

        // 3. Setup PDF Document with image on page 1
        val imageBounds = RectF(0f, 0f, 100f, 100f)
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val imageObject = ImagePdfObject(bitmap, imageBounds)
        val keyedObject = KeyedPdfObject("image1", imageObject)

        val fakePdfDocument =
            FakePdfDocument(
                pages = listOf(Point(100, 100), Point(100, 100)),
                searchResults = nativeResults,
                pageObjectsPerPage = mapOf(pageNum1 to listOf(keyedObject)),
            )

        val searchRepository = SearchRepository(fakePdfDocument)
        searchRepository.setOcrProvider(ocrProvider)

        // 4. Perform Search
        searchRepository.produceSearchResults("ocr", currentVisiblePage = 0)

        // 5. Verify results
        val results = searchRepository.queryResults.value as QueryResults.Matched

        // Check Page 0 (Native only)
        val nativeMatches = results.resultBounds.get(pageNum)
        assertNotNull("Native results on page 0 should not be null", nativeMatches)
        assertEquals(1, nativeMatches.size)

        // Check Page 1 (OCR only)
        val ocrMatches = results.resultBounds.get(pageNum1)
        assertNotNull("OCR results on page 1 should not be null", ocrMatches)
        assertEquals(1, ocrMatches.size)
    }
}
