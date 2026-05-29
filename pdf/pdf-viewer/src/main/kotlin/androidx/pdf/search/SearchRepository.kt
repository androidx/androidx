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

import android.os.RemoteException
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.core.util.isEmpty
import androidx.core.util.isNotEmpty
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.ocr.OcrContextRepository
import androidx.pdf.ocr.OcrProvider
import androidx.pdf.ocr.search
import androidx.pdf.search.model.NoQuery
import androidx.pdf.search.model.QueryResults
import androidx.pdf.search.model.SearchResultState
import androidx.pdf.util.ExceptionUtils.isHandledRemoteException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * Repository responsible for searching over PDF documents.
 *
 * This repository handles the business logic for searching within PDF files, including:
 * - Initiating search operation using the [PdfDocument] interface, and selecting the initial search
 *   result based upon current visible page.
 * - Managing search results and providing navigation through them.
 *
 * The search results are exposed as a [StateFlow], allowing observers to react to changes in the
 * search results in a reactive manner.
 *
 * @param pdfDocument: Interface to interact with pdf document
 * @param ocrContextRepository: Optional repository for OCR-based search in images.
 * @param dispatcher: The [CoroutineDispatcher] to use for performing the search operation. Defaults
 *   to Dispatcher.IO.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SearchRepository(
    private val pdfDocument: PdfDocument,
    private var ocrContextRepository: OcrContextRepository? = null,
    // TODO(b/384001800) Remove dispatcher
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val ocrDispatcher = Dispatchers.Default.limitedParallelism(2)

    /** Sets the [OcrProvider] used for recognizing text in image-based PDF content. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun setOcrProvider(ocrProvider: OcrProvider?) {
        ocrContextRepository = ocrProvider?.let { OcrContextRepository(pdfDocument, it) }
    }

    private val _queryResults: MutableStateFlow<SearchResultState> = MutableStateFlow(NoQuery)

    /** Stream of search results for a given query. */
    public val queryResults: StateFlow<SearchResultState>
        get() = _queryResults.asStateFlow()

    private lateinit var cyclicIterator: CyclicSparseArrayIterator

    /**
     * Initiates search over pdf document
     *
     * @param query The search query string.
     * @param currentVisiblePage Provides current visible document page, which is required to search
     *   from specific page and to calculate initial QueryResultsIndex.
     * @param resultIndex (optional) The index of the selected result when restoring from a previous
     *   session. If not provided, the first matching result on the page will be selected by
     *   default.
     *
     * Results would be updated to [queryResults] in the coroutine collecting the flow.
     */
    public suspend fun produceSearchResults(
        query: String,
        currentVisiblePage: Int,
        resultIndex: Int = 0,
    ) {
        if (query.isBlank()) {
            clearSearchResults()
            return
        }

        val searchPageRange = IntRange(start = 0, endInclusive = pdfDocument.pageCount - 1)

        // search should be a background work, move execution on to provided [dispatcher]
        // to make [searchDocument] main-safe
        val searchResults =
            withContext(dispatcher) {
                try {
                    // Native Text Search.
                    val textResults =
                        pdfDocument.searchDocument(query = query, pageRange = searchPageRange)
                    // Image Text Search.
                    val ocrResults =
                        if (ocrContextRepository != null) fetchOcrResults(query, searchPageRange)
                        else SparseArray()

                    mergeResults(textResults, ocrResults)
                } catch (e: RemoteException) {
                    if (!e.isHandledRemoteException) throw e
                    // Gracefully recover from known remote failures (e.g., service crashes or
                    // IPC call rejection).
                    return@withContext null
                }
            }

        if (searchResults == null) {
            // An exception happened above because of service disconnection.
            // Reset search so that user may try again.
            _queryResults.update { NoQuery }
            return
        }
        val queryResults =
            if (searchResults.isNotEmpty()) {
                /*
                 When search results are available for a query, we initialize a cyclic iterator.
                 This iterator is used to traverse the results when `findPrev()` and `findNext()` are called.
                */
                cyclicIterator =
                    CyclicSparseArrayIterator(
                        searchData = searchResults,
                        visiblePage = currentVisiblePage,
                    )

                // Restores the current index if required, or selects the first index of the page.
                cyclicIterator.moveToIndex(index = resultIndex)

                QueryResults.Matched(
                    query = query,
                    pageRange = searchPageRange,
                    resultBounds = searchResults,
                    /* Set [queryResultsIndex] to cyclicIterator.current() which points to first result
                    on or nearest page to currentVisiblePage in forward direction. */
                    queryResultsIndex = cyclicIterator.current(),
                )
            } else {
                QueryResults.NoMatch(query = query, pageRange = searchPageRange)
            }

        _queryResults.update { queryResults }
    }

    /**
     * Iterate through searchResults in backward direction.
     *
     * Results would be updated to [queryResults] in the coroutine collecting the flow.
     *
     * Throws [NoSuchElementException] is search results are empty.
     */
    public suspend fun producePreviousResult() {
        val currentResult = queryResults.value

        if (currentResult !is QueryResults.Matched)
            throw NoSuchElementException("Iteration not possible over empty results")

        /*
         Create a shallow copy of the query result, updating only the `queryResultIndex`
         to point to the previous element in the `resultsBounds` of the current query result.
        */
        val prevResult =
            QueryResults.Matched(
                query = currentResult.query,
                resultBounds = currentResult.resultBounds,
                pageRange = currentResult.pageRange,
                queryResultsIndex = cyclicIterator.prev(),
            )

        _queryResults.update { prevResult }
    }

    /**
     * Iterate through searchResults in forward direction.
     *
     * Results would be updated to [queryResults] in the coroutine collecting the flow.
     *
     * Throws [NoSuchElementException] is search results are empty.
     */
    public suspend fun produceNextResult() {
        val currentResult = queryResults.value

        if (currentResult !is QueryResults.Matched)
            throw NoSuchElementException("Iteration not possible over empty results")

        /*
         Create a shallow copy of the query result, updating only the `queryResultIndex`
         to point to the next element in the `resultsBounds` of the current query result.
        */
        val nextResult =
            QueryResults.Matched(
                query = currentResult.query,
                resultBounds = currentResult.resultBounds,
                pageRange = currentResult.pageRange,
                queryResultsIndex = cyclicIterator.next(),
            )

        _queryResults.update { nextResult }
    }

    /**
     * Resets [queryResults] to initial state. This would be required to handle close/cancel action.
     */
    public fun clearSearchResults() {
        _queryResults.update { NoQuery }
    }

    private suspend fun fetchOcrResults(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> = coroutineScope {
        val ocrResults = SparseArray<List<PageMatchBounds>>()

        pageRange
            .map { pageNum ->
                async(ocrDispatcher) {
                    ensureActive()
                    val contexts = ocrContextRepository?.getOcrContexts(pageNum) ?: emptyList()
                    val matches =
                        contexts.flatMap { context ->
                            context.search(query).map { bounds ->
                                PageMatchBounds(bounds, textStartIndex = -1)
                            }
                        }

                    pageNum to matches
                }
            }
            .awaitAll()
            .forEach { (pageNum, ocrMatches) ->
                if (ocrMatches.isNotEmpty()) {
                    ocrResults.put(pageNum, ocrMatches)
                }
            }
        ocrResults
    }

    private fun mergeResults(
        textResults: SparseArray<List<PageMatchBounds>>,
        ocrResults: SparseArray<List<PageMatchBounds>>,
    ): SparseArray<List<PageMatchBounds>> {
        if (ocrResults.isEmpty()) {
            return textResults
        }

        val combinedResults = textResults.clone()

        // Merge OCR results into combinedResults and sort matches per page.
        for (i in 0 until ocrResults.size()) {
            val pageNum = ocrResults.keyAt(i)
            val ocrMatches = ocrResults.valueAt(i)
            val nativeMatches = combinedResults.get(pageNum) ?: emptyList()

            val mergedAndSorted =
                (nativeMatches + ocrMatches).sortedBy { it.bounds.firstOrNull()?.top }
            combinedResults.put(pageNum, mergedAndSorted)
        }

        return combinedResults
    }
}
