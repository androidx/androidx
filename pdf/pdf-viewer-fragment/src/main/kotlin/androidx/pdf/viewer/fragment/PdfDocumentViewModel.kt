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
import androidx.annotation.RestrictTo
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.pdf.PdfDocument
import androidx.pdf.PdfLoader
import androidx.pdf.PdfPasswordException
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.search.SearchRepository
import androidx.pdf.search.model.NoQuery
import androidx.pdf.search.model.QueryResults
import androidx.pdf.search.model.SearchResultState
import androidx.pdf.viewer.fragment.model.HighlightData
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState
import androidx.pdf.viewer.fragment.model.SearchViewUiState
import androidx.pdf.viewer.fragment.util.fetchCounterData
import androidx.pdf.viewer.fragment.util.getCenter
import androidx.pdf.viewer.fragment.util.toHighlightsData
import java.util.concurrent.Executors
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A ViewModel class responsible for managing the loading and state of a PDF document.
 *
 * This ViewModel uses a [PdfLoader] to asynchronously open a PDF document from a given Uri. The
 * loading result, which can be either a success with a [PdfDocument] or a failure with an
 * exception, is exposed through the `pdfDocumentStateFlow`.
 *
 * The `loadDocument` function initiates the loading process within the `viewModelScope`, ensuring
 * that the operation is properly managed and not cancelled by configuration changes.
 *
 * @property loader The [PdfLoader] used to open the PDF document.
 * @constructor Creates a new [PdfDocumentViewModel] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfDocumentViewModel(
    private val state: SavedStateHandle,
    private val loader: PdfLoader,
) : ViewModel() {

    /** A Coroutine [Job] that manages the PDF loading task. */
    private var documentLoadJob: Job? = null

    /**
     * Parent [Job] for search query and result collectors. All children jobs will be cancelled upon
     * disabling [PdfViewerFragment.isTextSearchActive].
     */
    private val searchCollector = SupervisorJob(viewModelScope.coroutineContext[Job])

    /**
     * Parent [Job] for search operations triggered on [SearchRepository]. All children jobs will
     * cancelled upon updating search query.
     */
    private var searchJob: Job = SupervisorJob(viewModelScope.coroutineContext[Job])

    private val _fragmentUiScreenState =
        MutableStateFlow<PdfFragmentUiState>(PdfFragmentUiState.Loading)

    /**
     * Represents the UI state of the fragment.
     *
     * Exposes the UI state as a StateFlow to enable reactive consumption and ensure that consumers
     * always receive the latest state.
     */
    internal val fragmentUiScreenState: StateFlow<PdfFragmentUiState>
        get() = _fragmentUiScreenState.asStateFlow()

    private val _searchViewUiState = MutableStateFlow<SearchViewUiState>(SearchViewUiState.Closed)

    /** Stream of UI states of the PdfSearchView. */
    internal val searchViewUiState: StateFlow<SearchViewUiState>
        get() = _searchViewUiState.asStateFlow()

    internal val immersiveModeFlow: StateFlow<Boolean>
        get() = state.getStateFlow(IMMERSIVE_MODE_STATE_KEY, false)

    private val _highlightsFlow = MutableStateFlow<HighlightData>(EMPTY_HIGHLIGHTS)

    /** Stream of highlights to be added on PdfView. Also includes scroll to page data. */
    internal val highlightsFlow: StateFlow<HighlightData>
        get() = _highlightsFlow.asStateFlow()

    /**
     * Indicates whether the user is entering their password for the first time or making a repeated
     * attempt.
     *
     * This state is used to determine the appropriate error message to display in the password
     * dialog.
     */
    private var passwordFailed = false

    /** DocumentUri as set in [state] */
    val documentUriFromState: Uri?
        get() = state[DOCUMENT_URI_KEY]

    /** isTextSearchActive as set in [state] */
    val isTextSearchActiveFromState: Boolean
        get() = state[TEXT_SEARCH_STATE_KEY] ?: false

    /** isImmersiveModeFromState as set in [state] */
    val isImmersiveModeDesired: Boolean
        get() = state[IMMERSIVE_MODE_STATE_KEY] ?: false

    /** Holds business logic for search feature. */
    private lateinit var searchRepository: SearchRepository

    init {
        /**
         * Open PDF if documentUri was previously set in state. This will be required in events like
         * process death
         */
        state.get<Uri>(DOCUMENT_URI_KEY)?.let { uri ->
            documentLoadJob = viewModelScope.launch { openDocument(uri) }
            /*
            Trigger restoring search view once document is loaded.
            This is required as [SearchRepository] depends on [PdfDocument] which is created in
            [PdfFragmentUiState.DocumentLoaded] state.
            */
            documentLoadJob?.invokeOnCompletion { maybeRestoreSearchState() }
            documentLoadJob?.invokeOnCompletion { maybeRestoreImmersiveModeState() }
        }
    }

    private fun maybeRestoreImmersiveModeState() {
        setImmersiveModeDesired(enterImmersive = isImmersiveModeDesired)
    }

    private fun maybeRestoreSearchState() {
        // Return early if search is disabled, as there's no result to restore.
        if (!isTextSearchActiveFromState) return

        // Restore search session from last state saved
        updateSearchState(isTextSearchActive = isTextSearchActiveFromState)
        val query = state.get<String>(SEARCH_QUERY_KEY)
        val pageNum = state.get<Int>(QUERY_RESULT_PAGE_NUM_KEY) ?: 0
        val resultIndex = state.get<Int>(QUERY_RESULT_INDEX_KEY) ?: 0
        query?.let {
            viewModelScope.launch(searchJob) {
                searchRepository.produceSearchResults(
                    query = query,
                    currentVisiblePage = pageNum,
                    resultIndex = resultIndex,
                )
            }
        }
    }

    /**
     * Initiates the loading of a PDF document from the provided Uri.
     *
     * This function uses the provided [PdfLoader] to asynchronously open the PDF document. The
     * loading result is then posted to the `pdfDocumentStateFlow` as a [Result] object, indicating
     * either success with a [PdfDocument] or failure with an exception.
     *
     * The loading operation is executed within the `viewModelScope` to ensure that it continues
     * even if a configuration change occurs.
     *
     * @param uri The Uri of the PDF document to load.
     * @param password The optional password to use if the document is encrypted.
     */
    fun loadDocument(uri: Uri?, password: String?) {
        uri?.let {
            /*
            Triggers the document loading process only under the following conditions:
            1. **New Document URI:** The URI of the document to be loaded is different
            `from the URI of the previously loaded document.
            2. **Previous Load Failure or No Previous Load:** This is required when a
             reload of document is required like document loading failed previous time or opened
             using an incorrect password.
             */
            if (
                (uri != state[DOCUMENT_URI_KEY] ||
                    fragmentUiScreenState.value !is PdfFragmentUiState.DocumentLoaded)
            ) {
                state[DOCUMENT_URI_KEY] = uri
                // Ensure we don't schedule duplicate loading by canceling previous one.
                if (documentLoadJob?.isActive == true) documentLoadJob?.cancel()

                // Loading a new document should not persist a search session from previous
                // document.
                updateSearchState(isTextSearchActive = false)
                setImmersiveModeDesired(enterImmersive = true)

                documentLoadJob = viewModelScope.launch { openDocument(uri, password) }
            }
        }
    }

    /**
     * Called when the user toggles the search view's active state
     * [PdfViewerFragment.isTextSearchActive].
     *
     * This function updates the search state in the [SavedStateHandle] and performs actions related
     * to enabling/disabling the search view.
     */
    internal fun updateSearchState(isTextSearchActive: Boolean) {
        /**
         * [SearchRepository] is initialized only after a document is successfully loaded. If user
         * triggers search before document is loaded, it will be a No-Op.
         */
        if (fragmentUiScreenState.value !is PdfFragmentUiState.DocumentLoaded) return

        state[TEXT_SEARCH_STATE_KEY] = isTextSearchActive

        if (isTextSearchActive) {
            _searchViewUiState.update { SearchViewUiState.Init }
            collectSearchResults()
        } else {
            searchJob.children.forEach { it.cancel() }
            searchCollector.children.forEach { it.cancel() }
            searchRepository.clearSearchResults()

            _searchViewUiState.update { SearchViewUiState.Closed }
            _highlightsFlow.update { EMPTY_HIGHLIGHTS }
            // Remove search params set in state on disabling search.
            state.apply {
                remove<String>(SEARCH_QUERY_KEY)
                remove<Int>(QUERY_RESULT_PAGE_NUM_KEY)
                remove<Int>(QUERY_RESULT_INDEX_KEY)
            }
        }
    }

    private fun collectSearchResults() {
        viewModelScope.launch(searchCollector) {
            searchRepository.queryResults.collect { queryResults ->
                handleQueryResults(queryResults)
            }
        }
    }

    private fun handleQueryResults(queryResults: SearchResultState) {
        when (queryResults) {
            is NoQuery -> {
                _searchViewUiState.update { SearchViewUiState.Init }
                _highlightsFlow.update { EMPTY_HIGHLIGHTS }
            }
            is QueryResults.NoMatch -> {
                state[SEARCH_QUERY_KEY] = queryResults.query

                _searchViewUiState.update {
                    SearchViewUiState.Active(
                        query = queryResults.query,
                        currentMatch = 0,
                        totalMatches = 0,
                    )
                }
                _highlightsFlow.update { EMPTY_HIGHLIGHTS }
            }
            is QueryResults.Matched -> {
                with(queryResults) {
                    state[SEARCH_QUERY_KEY] = query
                    state[QUERY_RESULT_PAGE_NUM_KEY] = queryResultsIndex.pageNum
                    state[QUERY_RESULT_INDEX_KEY] = queryResultsIndex.resultBoundsIndex
                }

                val (currentIndex, totalMatches) = queryResults.fetchCounterData()
                _searchViewUiState.update {
                    SearchViewUiState.Active(
                        query = queryResults.query,
                        // The UI displays the search result counter starting from 1,
                        // so we add 1 to the current index.
                        currentMatch = if (totalMatches > 0) currentIndex + 1 else 0,
                        totalMatches = totalMatches,
                    )
                }
                _highlightsFlow.update {
                    HighlightData(
                        currentIndex = currentIndex,
                        highlightBounds = queryResults.toHighlightsData(),
                    )
                }
            }
        }
    }

    /**
     * Handles user interaction related to enabling the immersive mode.
     *
     * This function ensures that the immersive mode is properly applied and ready for user input
     * when triggered.
     */
    fun setImmersiveModeDesired(enterImmersive: Boolean) {
        /**
         * Immersive mode state should be updated only after document is loaded. else it will be a
         * No-Op.
         */
        if (fragmentUiScreenState.value !is PdfFragmentUiState.DocumentLoaded) return
        state[IMMERSIVE_MODE_STATE_KEY] = enterImmersive
    }

    /**
     * Toggles the immersive mode state.
     *
     * This function ensures that the immersive mode is properly applied and ready for user input
     * when triggered.
     */
    fun toggleImmersiveModeState() {
        /**
         * Immersive mode state should be updated only after document is loaded. else it will be a
         * No-Op.
         */
        if (fragmentUiScreenState.value !is PdfFragmentUiState.DocumentLoaded) return
        state[IMMERSIVE_MODE_STATE_KEY] = !isImmersiveModeDesired
    }

    private suspend fun openDocument(uri: Uri, password: String? = null) {
        /**
         * PdfDocument, if ever created, will be stored in DocumentLoaded state. This state could be
         * transitioned to other only if a new uri is submitted.
         */
        releaseDocument()

        /** Move to [PdfFragmentUiState.Loading] state before we begin load operation. */
        _fragmentUiScreenState.update { PdfFragmentUiState.Loading }

        try {

            // Try opening pdf with provided params
            val document = loader.openDocument(uri, password)

            searchRepository = SearchRepository(document)

            /** Successful load, move to [PdfFragmentUiState.DocumentLoaded] state. */
            _fragmentUiScreenState.update { PdfFragmentUiState.DocumentLoaded(document) }
            setImmersiveModeDesired(enterImmersive = false)

            /** Resets the [passwordFailed] state after a document is successfully loaded. */
            passwordFailed = false
        } catch (passwordException: PdfPasswordException) {
            /** Move to [PdfFragmentUiState.PasswordRequested] for password protected pdf. */
            _fragmentUiScreenState.update { PdfFragmentUiState.PasswordRequested(passwordFailed) }

            /** Enable [passwordFailed] for subsequent password attempts. */
            passwordFailed = true
        } catch (exception: Exception) {
            /** Generic exception handling, move to [PdfFragmentUiState.DocumentError] state. */
            _fragmentUiScreenState.update { PdfFragmentUiState.DocumentError(exception) }

            /** Resets the [passwordFailed] state after a document failed to load. */
            passwordFailed = false
        }
    }

    /** Intent triggered when user submits a search query. */
    fun searchDocument(query: String, visiblePageRange: IntRange) {
        /**
         * Cannot start searching document before it's loaded, i.e. fragment is moved to
         * [PdfFragmentUiState.DocumentLoaded] state.
         */
        if (fragmentUiScreenState.value !is PdfFragmentUiState.DocumentLoaded) return

        val queryResults = searchRepository.queryResults.value
        // Return early if the query is unchanged from the previous search to avoid redundant
        // operations.
        if (queryResults is QueryResults && queryResults.query == query) return

        // Cancel any on-going search operation(s) as the results will not be valid anymore.
        searchJob.children.forEach { it.cancel() }

        viewModelScope.launch(searchJob) {
            searchRepository.produceSearchResults(
                query = query,
                currentVisiblePage = visiblePageRange.getCenter(),
            )
        }
    }

    /** Intent triggered when user clicks prev button. */
    fun findPreviousMatch() {
        viewModelScope.launch(searchJob) { searchRepository.producePreviousResult() }
    }

    /** Intent triggered when user clicks next button. */
    fun findNextMatch() {
        viewModelScope.launch(searchJob) { searchRepository.produceNextResult() }
    }

    private fun IntRange.getCenterPage(): Int {
        val size = endInclusive - first + 1
        return first + size / 2
    }

    fun passwordDialogCancelled() {
        /** Resets the [passwordFailed] state after a password dialog is cancelled. */
        passwordFailed = false
        _fragmentUiScreenState.update {
            PdfFragmentUiState.DocumentError(
                OperationCanceledException("Password cancelled. Cannot open PDF.")
            )
        }
    }

    /**
     * Closes the currently loaded PDF document, if one exists. This is important to release
     * resources and prevent leaks.
     */
    private fun releaseDocument() {
        (_fragmentUiScreenState.value as? PdfFragmentUiState.DocumentLoaded)?.pdfDocument?.close()
    }

    override fun onCleared() {
        super.onCleared()
        releaseDocument()
    }

    @Suppress("UNCHECKED_CAST")
    companion object {

        private const val DOCUMENT_URI_KEY = "documentUri"
        private const val TEXT_SEARCH_STATE_KEY = "textSearchState"
        private const val IMMERSIVE_MODE_STATE_KEY = "immersiveModeState"
        private const val SEARCH_QUERY_KEY = "searchQuery"
        private const val QUERY_RESULT_INDEX_KEY = "queryResultIndex"
        private const val QUERY_RESULT_PAGE_NUM_KEY = "queryResultPageNum"
        private val EMPTY_HIGHLIGHTS = HighlightData(currentIndex = -1, highlightBounds = listOf())

        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    // Get the Application object from extras
                    val application = checkNotNull(extras[APPLICATION_KEY])
                    // Create a SavedStateHandle for this ViewModel from extras
                    val savedStateHandle = extras.createSavedStateHandle()

                    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
                    return (PdfDocumentViewModel(
                        savedStateHandle,
                        SandboxedPdfLoader(application, dispatcher),
                    ))
                        as T
                }
            }
    }
}
