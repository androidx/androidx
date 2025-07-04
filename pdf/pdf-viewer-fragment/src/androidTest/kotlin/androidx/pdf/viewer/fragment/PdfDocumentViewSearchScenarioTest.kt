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

package androidx.pdf.viewer.fragment

import android.net.Uri
import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.viewer.coroutines.toListDuring
import androidx.pdf.viewer.document.FakePdfDocument
import androidx.pdf.viewer.document.FakePdfLoader
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState
import androidx.pdf.viewer.fragment.model.SearchViewUiState
import androidx.pdf.viewer.rule.MainCoroutineRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfDocumentViewSearchScenarioTest {

    @ExperimentalCoroutinesApi @get:Rule internal var mainCoroutineRule = MainCoroutineRule()

    private val appContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private lateinit var pdfDocumentViewModel: PdfDocumentViewModel
    private val savedStateHandle = SavedStateHandle()
    private val documentUri = Uri.parse("content://test.pdf")

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

    fun setupViewModel(searchResults: SparseArray<List<PageMatchBounds>> = SparseArray()) {
        pdfDocumentViewModel =
            PdfDocumentViewModel(
                savedStateHandle,
                FakePdfLoader(FakePdfDocument(searchResults = searchResults)),
            )
    }

    @Test
    fun test_pdfDocumentViewMode_toggleSearch_documentNotLoaded() = runTest {
        setupViewModel()
        // try toggling search without document loaded
        pdfDocumentViewModel.updateSearchState(true)
        // collect search view state
        val searchViewUiState = pdfDocumentViewModel.searchViewUiState.toListDuring(1)
        // assert search view stays in inactive state
        assertEquals(1, searchViewUiState.size)
        assertTrue(searchViewUiState.first() is SearchViewUiState.Closed)
    }

    @Test
    fun test_pdfDocumentViewModel_isTextSearchActive_documentLoaded() = runTest {
        setupViewModel()
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)

        advanceUntilIdle()

        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentLoaded
        )

        // Assert initially the search view state is closed, until user have enabled it.
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Closed)

        // turn on search view toggle
        pdfDocumentViewModel.updateSearchState(true)

        assertEquals(true, savedStateHandle["textSearchState"])

        // assert state changed to [SearchViewUiState.Init]
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Init)

        // set toggle to false
        pdfDocumentViewModel.updateSearchState(false)

        // assert search state becomes [SearchViewUiState.Closed]
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Closed)
    }

    @Test
    fun test_pdfDocumentViewModel_onTextSearch_WithMatchingResults() = runTest {
        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)

        advanceUntilIdle()

        // turn on search
        pdfDocumentViewModel.apply {
            updateSearchState(true)
            searchDocument(SEARCH_QUERY, IntRange(0, 0))
        }

        advanceUntilIdle()

        with(pdfDocumentViewModel) {
            var collectedStates = searchViewUiState.take(2).toList()

            // Assert only 1 state transition occurred, i.e. from INIT -> ACTIVE
            assertTrue(collectedStates.first() is SearchViewUiState.Init)
            var currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(1, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform prev
            findPreviousMatch()
            // State transition: Active(1, 10) -> Active(10, 10)
            collectedStates = searchViewUiState.take(2).toList()

            assertEquals(2, collectedStates.size)
            currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(10, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform prev
            findPreviousMatch()
            // State transition: Active(10, 10) -> Active(9, 10)
            collectedStates = searchViewUiState.take(2).toList()

            currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(9, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform next
            repeat(3) { findNextMatch() }
            collectedStates = searchViewUiState.take(2).toList()

            currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(2, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // close search view and then re-open
            updateSearchState(false)
            assertTrue(searchViewUiState.value is SearchViewUiState.Closed)

            updateSearchState(true)
            assertTrue(searchViewUiState.value is SearchViewUiState.Init)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_onTextSearch_WithNoMatchingResults() = runTest {
        setupViewModel()
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()

        // turn on search
        pdfDocumentViewModel.apply {
            updateSearchState(true)
            // search for non-existent word
            searchDocument(SEARCH_QUERY + SEARCH_QUERY, visiblePageRange = IntRange(0, 2))
        }
        advanceUntilIdle()

        with(pdfDocumentViewModel) {
            var collectedStates = searchViewUiState.take(2).toList()

            // Assert only 1 state transition occurred, i.e. from INIT -> ACTIVE
            assertTrue(collectedStates.first() is SearchViewUiState.Init)
            val currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(0, currentState.totalMatches)
            assertEquals(0, currentState.currentMatch)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_onTextSearch_withAllPagesVisible() = runTest {
        val fakeResults = createFakeSearchResults(0, 0, 1, 2, 2)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()
        // turn on search
        pdfDocumentViewModel.apply {
            updateSearchState(true)
            // search for a word that exists on all 3 pages.
            searchDocument(query = "Proin", visiblePageRange = IntRange(0, 2))
        }
        advanceUntilIdle()

        with(pdfDocumentViewModel) {
            var collectedStates = searchViewUiState.take(2).toList()

            // Assert only 1 state transition occurred, i.e. from INIT -> ACTIVE
            assertTrue(collectedStates.first() is SearchViewUiState.Init)
            val currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(5, currentState.totalMatches)
            // assert result selected on page 1, i.e. the 3rd result
            assertEquals(3, currentState.currentMatch)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_onTextSearch_withNextResultSelected() = runTest {
        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()
        // turn on search
        pdfDocumentViewModel.apply {
            updateSearchState(true)
            searchDocument(query = "Proin", visiblePageRange = IntRange(5, 8))
        }
        advanceUntilIdle()

        with(pdfDocumentViewModel) {
            var collectedStates = searchViewUiState.take(2).toList()

            // Assert only 1 state transition occurred, i.e. from INIT -> ACTIVE
            assertTrue(collectedStates.first() is SearchViewUiState.Init)
            val currentState = collectedStates.last() as SearchViewUiState.Active
            assertEquals(10, currentState.totalMatches)
            // assert result selected on page 10, i.e. the 7th result
            assertEquals(7, currentState.currentMatch)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_noResultHighlighted_onSearchInit() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val pdfDocumentViewModel =
            PdfDocumentViewModel(savedStateHandle, SandboxedPdfLoader(appContext, testDispatcher))
        // Assert empty highlights when viewmodel is init
        var highlightData = pdfDocumentViewModel.highlightsFlow.value
        assertEquals(-1, highlightData.currentIndex)
        assertTrue(highlightData.highlightBounds.isEmpty())
        // Init search
        pdfDocumentViewModel.updateSearchState(isTextSearchActive = true)
        // Assert empty highlights when search is init
        highlightData = pdfDocumentViewModel.highlightsFlow.value
        assertEquals(-1, highlightData.currentIndex)
        assertTrue(highlightData.highlightBounds.isEmpty())
    }

    @Test
    fun test_pdfDocumentViewModel_noResultHighlighted_onNoResultMatch() = runTest {
        setupViewModel()
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()
        // turn on search
        pdfDocumentViewModel.apply {
            updateSearchState(true)
            searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(5, 8))
        }
        // Assert empty highlights when no results match
        val highlightData = pdfDocumentViewModel.highlightsFlow.value
        assertEquals(-1, highlightData.currentIndex)
        assertTrue(highlightData.highlightBounds.isEmpty())
    }

    @Test
    fun test_pdfDocumentViewModel_resultsHighlighted_onResultsMatch_withInitialPagesVisible() =
        runTest {
            val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
            setupViewModel(fakeResults)
            pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
            // wait for document to load
            advanceUntilIdle()
            // turn on search
            pdfDocumentViewModel.apply {
                updateSearchState(true)
                searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(0, 0))
            }
            // Assert highlights exist when there are matching results
            val highlights = pdfDocumentViewModel.highlightsFlow.take(2).toList()
            val updatedHighlightData = highlights.last()
            assertEquals(0, updatedHighlightData.currentIndex)
            val totalSearchMatches =
                (pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active)
                    .totalMatches
            assertEquals(totalSearchMatches, updatedHighlightData.highlightBounds.size)
        }

    @Test
    fun test_pdfDocumentViewModel_resultsHighlighted_onResultsMatch_withCenterPagesVisible() =
        runTest {
            val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
            setupViewModel(fakeResults)
            pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
            // wait for document to load
            advanceUntilIdle()
            // turn on search
            pdfDocumentViewModel.apply {
                updateSearchState(true)
                searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(5, 8))
            }
            // Assert highlights exist when there are matching results
            val highlights = pdfDocumentViewModel.highlightsFlow.take(2).toList()
            val updatedHighlightData = highlights.last()
            assertEquals(
                6,
                updatedHighlightData.currentIndex,
            ) // it will be a 7th match, so index will be 6(0-indexed).
            val totalSearchMatches =
                (pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active)
                    .totalMatches
            assertEquals(totalSearchMatches, updatedHighlightData.highlightBounds.size)
        }

    @Ignore("Need more investigation on why it's failing on post submits. b/386727907")
    @Test
    fun test_pdfDocumentViewModel_noSearchOnSameQuery() = runTest {
        var counter = 0
        val collectorJob = launch { pdfDocumentViewModel.searchViewUiState.collect { counter++ } }

        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()
        // turn on search
        pdfDocumentViewModel.updateSearchState(true)
        // assert search view Init state is collected
        assertEquals(1, counter)
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Init)

        // start search
        pdfDocumentViewModel.searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(0, 1))
        // wait for search operation to complete
        advanceUntilIdle()
        // assert new state emitted after search completion
        assertEquals(2, counter)

        // search with the same query again
        pdfDocumentViewModel.searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(0, 1))
        advanceUntilIdle()
        // Assert no new state is emitted
        assertEquals(2, counter)

        collectorJob.cancel()
    }

    @Test
    fun test_pdfDocumentViewModel_isTextSearchActive_turnedOff() = runTest {
        setupViewModel()
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // wait for document to load
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(true)
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Init)

        // Pass same doc uri, this will not re-trigger document load.
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // Assert search session is preserved
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Init)

        // Pass a different uri, this will load  a new document
        pdfDocumentViewModel.loadDocument(uri = Uri.parse("content://test1.pdf"), password = null)
        // Assert search is reset.
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Closed)
    }

    @Test
    fun test_pdfDocumentViewModel_searchRestoreFromState() = runTest {
        val queryInState = "test"
        val state =
            SavedStateHandle().apply {
                this["documentUri"] = documentUri
                this["textSearchState"] = true
                this["searchQuery"] = queryInState
                this["queryResultIndex"] = 2
                this["queryResultPageNum"] = 10
            }
        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        val pdfDocumentViewModel =
            PdfDocumentViewModel(state, FakePdfLoader(FakePdfDocument(searchResults = fakeResults)))

        val searchStates = pdfDocumentViewModel.searchViewUiState.take(3).toList()
        // assert initially search view is closed
        assertTrue(searchStates.first() is SearchViewUiState.Closed)

        // assert onRecreate it will first move to Init state
        assertTrue(searchStates[1] is SearchViewUiState.Init)

        // assert on complete of search operation, state is restored as per state.
        val endState = searchStates.last() as SearchViewUiState.Active
        assertEquals(queryInState, endState.query)
        assertEquals(9, endState.currentMatch)
        assertEquals(10, endState.totalMatches)
    }

    @Test
    fun test_pdfDocumentViewModel_stateCleared_onSettingSearchInactive() = runTest {
        val state =
            SavedStateHandle().apply {
                this["documentUri"] = documentUri
                this["textSearchState"] = true
                this["searchQuery"] = "test"
                this["queryResultIndex"] = 2
                this["queryResultPageNum"] = 10
            }

        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        val pdfDocumentViewModel =
            PdfDocumentViewModel(state, FakePdfLoader(FakePdfDocument(searchResults = fakeResults)))
        // wait for document load to complete after init.
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(false)
        // assert search params are cleared upon disabling search
        assertFalse(state.contains("searchQuery"))
        assertFalse(state.contains("queryResultIndex"))
        assertFalse(state.contains("queryResultPageNum"))
    }

    companion object {
        private const val SEARCH_QUERY = "ipsum"
    }
}
