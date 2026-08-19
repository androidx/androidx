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

import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.util.SparseArray
import androidx.lifecycle.SavedStateHandle
import androidx.pdf.PdfFeature
import androidx.pdf.PdfPoint
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private fun createFakeSearchResults(
        vararg matches: Int,
        bounds: List<RectF> = listOf(RectF(10f, 10f, 50f, 20f)),
    ): SparseArray<List<PageMatchBounds>> {
        val results: SparseArray<List<PageMatchBounds>> = SparseArray()
        matches.forEach { pageNum ->
            val newPageResult =
                results.get(pageNum, listOf()).toMutableList().also {
                    it.add(PageMatchBounds(bounds = bounds, textStartIndex = 0))
                }
            results.append(pageNum, newPageResult)
        }
        return results
    }

    fun setupViewModel(
        searchResults: SparseArray<List<PageMatchBounds>> = SparseArray(),
        pageCount: Int = 15,
    ) {
        pdfDocumentViewModel =
            PdfDocumentViewModel(
                savedStateHandle,
                FakePdfLoader(
                    FakePdfDocument(
                        pages = List(pageCount) { Point(600, 800) },
                        searchResults = searchResults,
                        supportedFeatures = setOf(PdfFeature.SEARCH),
                    )
                ),
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
            searchDocument(SEARCH_QUERY, IntRange(0, 0), debounce = Duration.ZERO)
        }
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        with(pdfDocumentViewModel) {
            var currentState = searchViewUiState.value as SearchViewUiState.Active
            assertEquals(1, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform prev
            findPreviousMatch()
            advanceUntilIdle()
            currentState = searchViewUiState.value as SearchViewUiState.Active
            assertEquals(10, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform prev
            findPreviousMatch()
            advanceUntilIdle()
            currentState = searchViewUiState.value as SearchViewUiState.Active
            assertEquals(9, currentState.currentMatch)
            assertEquals(10, currentState.totalMatches)

            // Perform next
            repeat(3) { findNextMatch() }
            advanceUntilIdle()
            currentState = searchViewUiState.value as SearchViewUiState.Active
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
            searchDocument(
                SEARCH_QUERY + SEARCH_QUERY,
                visiblePageRange = IntRange(0, 2),
                debounce = Duration.ZERO,
            )
        }
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        with(pdfDocumentViewModel) {
            val currentState = searchViewUiState.value as SearchViewUiState.Active
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
            searchDocument(
                query = "Proin",
                visiblePageRange = IntRange(0, 2),
                debounce = Duration.ZERO,
            )
        }
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        with(pdfDocumentViewModel) {
            val currentState = searchViewUiState.value as SearchViewUiState.Active
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
            searchDocument(
                query = "Proin",
                visiblePageRange = IntRange(5, 8),
                debounce = Duration.ZERO,
            )
        }
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        with(pdfDocumentViewModel) {
            val currentState = searchViewUiState.value as SearchViewUiState.Active
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
            searchDocument(
                query = SEARCH_QUERY,
                visiblePageRange = IntRange(5, 8),
                debounce = Duration.ZERO,
            )
        }
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
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
                searchDocument(
                    query = SEARCH_QUERY,
                    visiblePageRange = IntRange(0, 0),
                    debounce = Duration.ZERO,
                )
            }
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            }
            // Assert highlights exist when there are matching results
            val updatedHighlightData = pdfDocumentViewModel.highlightsFlow.value
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
                searchDocument(
                    query = SEARCH_QUERY,
                    visiblePageRange = IntRange(5, 8),
                    debounce = Duration.ZERO,
                )
            }
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            }
            // Assert highlights exist when there are matching results
            val updatedHighlightData = pdfDocumentViewModel.highlightsFlow.value
            assertEquals(
                6,
                updatedHighlightData.currentIndex,
            ) // it will be a 7th match, so index will be 6(0-indexed).
            val totalSearchMatches =
                (pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active)
                    .totalMatches
            assertEquals(totalSearchMatches, updatedHighlightData.highlightBounds.size)
        }

    @Test
    fun test_pdfDocumentViewModel_noSearchOnSameQuery() = runTest {
        val fakeResults = createFakeSearchResults(0, 1, 2, 2, 5, 5, 10, 10, 10, 10)
        setupViewModel(fakeResults)

        val searchUiStates = mutableListOf<SearchViewUiState>()
        val collectedJob =
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                pdfDocumentViewModel.searchViewUiState.collect { searchUiStates.add(it) }
            }

        // Assert initially closed state is collected
        assertEquals(1, searchUiStates.size)
        assertTrue(searchUiStates.first() is SearchViewUiState.Closed)

        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        // Wait for document to load
        pdfDocumentViewModel.fragmentUiScreenState.first { it is PdfFragmentUiState.DocumentLoaded }

        // turn on search
        pdfDocumentViewModel.updateSearchState(true)
        // assert search view Init state is collected
        assertTrue(searchUiStates[1] is SearchViewUiState.Init)
        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Init)

        // start search
        pdfDocumentViewModel.searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(0, 1))
        // wait for search to complete
        val activeState =
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            } as SearchViewUiState.Active

        // assert state after search completion
        assertEquals(SEARCH_QUERY, activeState.query)
        assertEquals(1, activeState.currentMatch)
        assertEquals(10, activeState.totalMatches)
        val stateCount = searchUiStates.size

        // search with the same query again
        pdfDocumentViewModel.searchDocument(query = SEARCH_QUERY, visiblePageRange = IntRange(0, 1))
        advanceUntilIdle()
        // Assert no new state is emitted
        assertEquals(stateCount, searchUiStates.size)

        collectedJob.cancel()
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
            PdfDocumentViewModel(
                state,
                FakePdfLoader(
                    FakePdfDocument(
                        pages = List(15) { Point(600, 800) },
                        searchResults = fakeResults,
                        supportedFeatures = setOf(PdfFeature.SEARCH),
                    )
                ),
            )

        // assert on complete of search operation, state is restored as per state.
        val endState =
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            } as SearchViewUiState.Active
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
            PdfDocumentViewModel(
                state,
                FakePdfLoader(
                    FakePdfDocument(
                        pages = List(15) { Point(600, 800) },
                        searchResults = fakeResults,
                        supportedFeatures = setOf(PdfFeature.SEARCH),
                    )
                ),
            )
        // wait for document load to complete after init.
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(false)
        // assert search params are cleared upon disabling search
        assertFalse(state.contains("searchQuery"))
        assertFalse(state.contains("queryResultIndex"))
        assertFalse(state.contains("queryResultPageNum"))
    }

    @Test
    fun test_pdfDocumentViewModel_progressiveSearch_emitsIntermediateStatesWithIsSearching() =
        runTest {
            val fakeResults = createFakeSearchResults(0, 2, 5)
            setupViewModel(fakeResults)
            pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
            advanceUntilIdle()

            val collectedStates = mutableListOf<SearchViewUiState>()
            val job =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    pdfDocumentViewModel.searchViewUiState.collect { collectedStates.add(it) }
                }

            pdfDocumentViewModel.updateSearchState(true)
            pdfDocumentViewModel.searchDocument(
                query = SEARCH_QUERY,
                visiblePageRange = IntRange(0, 0),
                debounce = Duration.ZERO,
            )
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            }

            val activeStates = collectedStates.filterIsInstance<SearchViewUiState.Active>()
            assertTrue(activeStates.isNotEmpty())

            // Final state must indicate search completion
            val finalState = activeStates.last()
            assertFalse(finalState.isSearching)
            assertEquals(3, finalState.totalMatches)

            job.cancel()
        }

    @Test
    fun test_pdfDocumentViewModel_searchDebounce_typingCancelsPendingSearch() = runTest {
        val fakeResults = createFakeSearchResults(0, 1)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(true)

        val emittedStates = mutableListOf<SearchViewUiState>()
        val job =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                pdfDocumentViewModel.searchViewUiState.collect { emittedStates.add(it) }
            }

        // Type first query with 300ms debounce
        pdfDocumentViewModel.searchDocument(
            query = "a",
            visiblePageRange = IntRange(0, 0),
            debounce = 300.milliseconds,
        )

        // Type second query before debounce expires
        pdfDocumentViewModel.searchDocument(
            query = "ab",
            visiblePageRange = IntRange(0, 0),
            debounce = 300.milliseconds,
        )
        advanceUntilIdle()

        // Assert that we NEVER actually started searching for "a"
        val searchedForA =
            emittedStates.any {
                it is SearchViewUiState.Active && it.query == "a" && it.isSearching
            }
        assertFalse(searchedForA)

        val activeState = pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active
        assertEquals("ab", activeState.query)

        job.cancel()
    }

    @Test
    fun test_pdfDocumentViewModel_emptyQuery_clearsSearchImmediately() = runTest {
        val fakeResults = createFakeSearchResults(0, 1)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(true)
        pdfDocumentViewModel.searchDocument(
            query = "test",
            visiblePageRange = IntRange(0, 0),
            debounce = Duration.ZERO,
        )
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        assertTrue(pdfDocumentViewModel.searchViewUiState.value is SearchViewUiState.Active)
        val activeState = pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active

        // Assert we have a valid set of highlights after search completes
        val activeHighlights = pdfDocumentViewModel.highlightsFlow.value
        assertEquals(0, activeHighlights.currentIndex)
        assertEquals(activeState.totalMatches, activeHighlights.highlightBounds.size)

        // Empty query should clear results immediately
        pdfDocumentViewModel.searchDocument(
            query = "",
            visiblePageRange = IntRange(0, 0),
            debounce = Duration.ZERO,
        )
        advanceUntilIdle()

        val highlights = pdfDocumentViewModel.highlightsFlow.value
        assertEquals(-1, highlights.currentIndex)
        assertTrue(highlights.highlightBounds.isEmpty())
    }

    @Test
    fun test_pdfDocumentViewModel_whitespaceQuery_triggersSearch() = runTest {
        val fakeResults = createFakeSearchResults(0, 1)
        setupViewModel(fakeResults)
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        advanceUntilIdle()

        pdfDocumentViewModel.updateSearchState(true)
        pdfDocumentViewModel.searchDocument(
            query = "   ",
            visiblePageRange = IntRange(0, 0),
            debounce = Duration.ZERO,
        )
        pdfDocumentViewModel.searchViewUiState.first {
            it is SearchViewUiState.Active && !it.isSearching
        }

        val activeState = pdfDocumentViewModel.searchViewUiState.value as SearchViewUiState.Active
        assertEquals("   ", activeState.query)
    }

    @Test
    fun test_pdfDocumentViewModel_searchScrollPositionFlow_emitsOnlyOnInitialMatchAndNavigation() =
        runTest {
            val fakeResults = createFakeSearchResults(0, 2, 5)
            setupViewModel(fakeResults)
            pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
            advanceUntilIdle()

            val scrollPositions = mutableListOf<PdfPoint>()
            val job =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    pdfDocumentViewModel.searchScrollPositionFlow.collect {
                        scrollPositions.add(it)
                    }
                }

            pdfDocumentViewModel.updateSearchState(true)
            // 1. Initial search finds matches on page 0, page 2, page 5.
            pdfDocumentViewModel.searchDocument(
                query = SEARCH_QUERY,
                visiblePageRange = IntRange(0, 0),
                debounce = Duration.ZERO,
            )
            pdfDocumentViewModel.searchViewUiState.first {
                it is SearchViewUiState.Active && !it.isSearching
            }

            // Should emit scroll target for page 0 (initial match) exactly once, even though
            // progressive search streamed 3 pages
            assertEquals(1, scrollPositions.size)
            assertEquals(0, scrollPositions[0].pageNum)

            // 2. User clicks Next -> moves to page 2 -> should emit 2nd scroll event
            pdfDocumentViewModel.findNextMatch()
            advanceUntilIdle()
            assertEquals(2, scrollPositions.size)
            assertEquals(2, scrollPositions[1].pageNum)

            // 3. User clicks Next -> moves to page 5 -> should emit 3rd scroll event
            pdfDocumentViewModel.findNextMatch()
            advanceUntilIdle()
            assertEquals(3, scrollPositions.size)
            assertEquals(5, scrollPositions[2].pageNum)

            // 4. User clicks Prev -> moves back to page 2 -> should emit 4th scroll event
            pdfDocumentViewModel.findPreviousMatch()
            advanceUntilIdle()
            assertEquals(4, scrollPositions.size)
            assertEquals(2, scrollPositions[3].pageNum)

            job.cancel()
        }

    companion object {
        private const val SEARCH_QUERY = "ipsum"
    }
}
