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

package androidx.pdf.ink

import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.pdf.EditablePdfDocument
import androidx.pdf.PdfDocument
import androidx.pdf.PdfLoader
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.annotation.PdfAnnotationsEditor
import androidx.pdf.annotation.history.AnnotationRecordsHistoryManager
import androidx.pdf.annotation.manager.PdfAnnotationsManager
import androidx.pdf.annotation.models.AnnotationsDisplayState
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfEdits
import androidx.pdf.annotation.models.VisiblePdfAnnotations
import androidx.pdf.ink.model.ApplyEditsState
import androidx.pdf.ink.state.AnnotationDrawingMode
import androidx.pdf.ink.state.PdfEditMode
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_ANNOTATIONS
import androidx.pdf.ink.util.InkDefaults
import androidx.pdf.ink.view.tool.AnnotationToolInfo
import androidx.pdf.ink.view.tool.Eraser
import androidx.pdf.ink.view.tool.Highlighter
import androidx.pdf.ink.view.tool.Pen
import androidx.pdf.viewer.fragment.PdfDocumentViewModel
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState
import java.util.BitSet
import java.util.concurrent.Executors
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class EditableDocumentViewModel(private val state: SavedStateHandle, loader: PdfLoader) :
    PdfDocumentViewModel(state, loader) {
    private var recordsHistoryManager: AnnotationRecordsHistoryManager? = null
    private var annotationsEditor: PdfAnnotationsEditor? = null
    private var annotationsManager: PdfAnnotationsManager? = null
    private var historyCollectionJob: Job? = null
    private val bitmapAvailabilityMap = BitSet()

    private val _annotationDisplayStateFlow = MutableStateFlow(AnnotationsDisplayState.EMPTY)

    internal val annotationsDisplayStateFlow: StateFlow<AnnotationsDisplayState> =
        _annotationDisplayStateFlow.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    internal val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    internal val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    internal val pdfEditModeFlow: StateFlow<PdfEditMode> =
        state.getStateFlow(EDIT_MODE_ENABLED_KEY, PdfEditMode.Disabled)

    internal var pdfEditMode: PdfEditMode
        get() = state[EDIT_MODE_ENABLED_KEY] ?: PdfEditMode.Disabled
        set(value) {
            if (pdfEditMode == value) return
            // Cannot switch journeys in the same session
            if (pdfEditMode is PdfEditMode.Enabled && value is PdfEditMode.Enabled) return

            state[EDIT_MODE_ENABLED_KEY] = value
            if (value !is PdfEditMode.Enabled) {
                // Discard any draft changes when exiting edit mode
                discardUnsavedChanges()
                forceLoadDocument()
            }
        }

    internal var areAnnotationsVisible: Boolean
        get() = state[ANNOTATION_VISIBLE_KEY] ?: true
        set(value) {
            state[ANNOTATION_VISIBLE_KEY] = value
        }

    internal var areAnnotationsVisibleFlow: StateFlow<Boolean> =
        state.getStateFlow(ANNOTATION_VISIBLE_KEY, true)

    private val _applyEditsStatus = MutableStateFlow<ApplyEditsState>(ApplyEditsState.Ready)
    internal val applyEditsStatus: StateFlow<ApplyEditsState> = _applyEditsStatus.asStateFlow()

    // TODO: b/441634479 Refactor to extract the document from `DocumentLoaded` UI state.
    internal var editablePdfDocument: EditablePdfDocument? = null

    private val _drawingMode =
        MutableStateFlow<AnnotationDrawingMode>(
            AnnotationDrawingMode.PenMode(
                InkDefaults.DEFAULT_BRUSH_SIZE,
                InkDefaults.DEFAULT_INK_COLOR,
            )
        )
    internal val drawingMode: StateFlow<AnnotationDrawingMode> = _drawingMode.asStateFlow()

    private val _isPdfViewGestureActive = MutableStateFlow(false)

    internal var isPdfViewGestureActive: Boolean
        get() = _isPdfViewGestureActive.value
        set(value) {
            _isPdfViewGestureActive.value = value
        }

    internal var visiblePageRange: IntRange = 0..0

    /** Reactive state that combines multiple flows to determine if interaction is enabled. */
    internal val isAnnotationInteractionEnabled: StateFlow<Boolean> =
        combine(
                pdfEditModeFlow,
                areAnnotationsVisibleFlow,
                _applyEditsStatus,
                _isPdfViewGestureActive,
            ) { pdfEditMode, isVisible, status, isGestureActive ->
                (pdfEditMode is PdfEditMode.Enabled &&
                    pdfEditMode.journey == EDITING_JOURNEY_ANNOTATIONS) &&
                    isVisible &&
                    status != ApplyEditsState.InProgress &&
                    !isGestureActive
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var didApplyEdits: Boolean = false

    init {
        viewModelScope.launch {
            _fragmentUiScreenState.collect { state ->
                if (state is PdfFragmentUiState.DocumentLoaded) {
                    maybeInitialiseForDocument(state.pdfDocument)
                }
            }
        }
    }

    public override fun forceLoadDocument() {
        if (didApplyEdits || formEditInfos.isNotEmpty()) {
            resetState()
            super.forceLoadDocument()
        }
    }

    @VisibleForTesting
    public override fun resetState() {
        super.resetState()
        pdfEditMode = PdfEditMode.Disabled
        editablePdfDocument = null
        _annotationDisplayStateFlow.value = AnnotationsDisplayState.EMPTY
        didApplyEdits = false
        bitmapAvailabilityMap.clear()
    }

    internal fun maybeInitialiseForDocument(document: PdfDocument) {
        if (document is EditablePdfDocument) {
            val documentUri = document.uri
            setupManagersAndHandlers(documentUri, document)
        } else {
            editablePdfDocument = null
        }
    }

    // Annotation Operations
    internal fun addDraftAnnotation(annotation: PdfAnnotation) {
        withEditor { editor ->
            editor.addDraftAnnotation(annotation)
            refreshVisibleAnnotations(visiblePageRange)
        }
    }

    internal fun undo() {
        withEditor { editor ->
            editor.undo()
            refreshVisibleAnnotations(visiblePageRange)
        }
    }

    internal fun redo() {
        withEditor { editor ->
            editor.redo()
            refreshVisibleAnnotations(visiblePageRange)
        }
    }

    internal fun removeAnnotation(annotationKey: String) {
        withEditor { editor ->
            editor.removeAnnotation(annotationKey)
            refreshVisibleAnnotations(visiblePageRange)
        }
    }

    // Data Loading & Saving

    /** Updates the transformation matrices for rendering annotations. */
    internal fun updateTransformationMatrices(transformationMatrices: Map<Int, Matrix>) {
        if (editablePdfDocument != null) {
            _annotationDisplayStateFlow.update {
                it.copy(transformationMatrices = transformationMatrices)
            }
        }
    }

    /**
     * Fetches annotations from the [PdfAnnotationsManager] for the defined page range.
     *
     * @param startPage The starting page number (inclusive).
     * @param endPage The ending page number (inclusive).
     */
    internal fun fetchAnnotationsForPageRange(startPage: Int, endPage: Int) {
        if (editablePdfDocument == null) return

        viewModelScope.launch { refreshVisibleAnnotations(startPage..endPage) }
    }

    internal fun applyDraftEdits() {
        val document = editablePdfDocument
        val localAnnotationsManager = annotationsManager

        if (document == null || localAnnotationsManager == null) {
            _applyEditsStatus.value =
                ApplyEditsState.Failure(IllegalStateException("Document not available"))
            return
        }

        _applyEditsStatus.value = ApplyEditsState.InProgress
        if (hasUnsavedChanges()) didApplyEdits = true

        viewModelScope.launch {
            try {
                val editsDraft = localAnnotationsManager.getAnnotationModifications()
                document.applyEdits(editsDraft)
                val handle = document.createWriteHandle()

                recordsHistoryManager?.clear()
                annotationsManager?.discardChanges()
                _applyEditsStatus.value = ApplyEditsState.Success(handle)
            } catch (e: Exception) {
                _applyEditsStatus.value = ApplyEditsState.Failure(e)
            }
        }
    }

    internal fun resetApplyEditsStatus() {
        _applyEditsStatus.value = ApplyEditsState.Ready
    }

    /**
     * Checks for unsaved changes by verifying if there are any edits in the history.
     *
     * @return `true` if unsaved changes exist, `false` if the document is not loaded or there are
     *   no changes.
     */
    internal fun hasUnsavedChanges(): Boolean =
        editablePdfDocument != null &&
            ((recordsHistoryManager?.canUndo?.value ?: false) || formEditInfos.isNotEmpty())

    /** Discards all uncommitted edits, reverting the document to its last saved state. */
    private fun discardUnsavedChanges() {
        withEditor { editor ->
            editor.clear()
            refreshVisibleAnnotations(visiblePageRange)
        }
    }

    /**
     * Shared logic to re-fetch annotations for the screen and update the UI flow. Consolidating
     * this reduces code duplication and allocation errors.
     */
    private suspend fun refreshVisibleAnnotations(range: IntRange) {
        val manager = annotationsManager ?: return

        // Defensive check to avoid stale obsolete get invocations
        if (visiblePageRange != range) return

        val pageAnnotations =
            range
                .associateWith { pageNum ->
                    // Display Annotation only for pages whose bitmap is available
                    if (bitmapAvailabilityMap.get(pageNum)) {
                        manager.getAnnotations(pageNum)
                    } else {
                        listOf()
                    }
                }
                .filterValues { it.isNotEmpty() }

        // This check ensures that the flow is not updated with stale data
        if (visiblePageRange != range) return

        _annotationDisplayStateFlow.update {
            it.copy(visiblePageAnnotations = VisiblePdfAnnotations(pageAnnotations))
        }
    }

    private fun setupManagersAndHandlers(
        documentUri: Uri?,
        document: EditablePdfDocument?,
        initialMatrices: Map<Int, Matrix> = emptyMap(),
    ) {
        // Cleanup previous flows to prevent memory leaks
        historyCollectionJob?.cancel()

        if (documentUri != null && document != null) {
            state[LOADED_DOCUMENT_URI_KEY] = documentUri
            editablePdfDocument = document

            val manager = PdfAnnotationsManager.create(document)
            val history = AnnotationRecordsHistoryManager()
            val editor = PdfAnnotationsEditor(history, manager)

            recordsHistoryManager = history
            annotationsEditor = editor
            annotationsManager = manager

            // Collect history states in a tracked job
            historyCollectionJob =
                viewModelScope.launch {
                    launch { history.canUndo.collect { _canUndo.value = it } }
                    launch { history.canRedo.collect { _canRedo.value = it } }
                }

            viewModelScope.launch {
                val visiblePdfAnnotations =
                    VisiblePdfAnnotations(
                        pageAnnotations =
                            visiblePageRange
                                .associateWith { pageNum -> manager.getAnnotations(pageNum) }
                                .filterValues { it.isNotEmpty() }
                    )
                _annotationDisplayStateFlow.value =
                    AnnotationsDisplayState(
                        edits = PdfEdits(editsByPage = emptyMap()),
                        transformationMatrices = initialMatrices,
                        visiblePageAnnotations = visiblePdfAnnotations,
                    )
            }
        } else {
            editablePdfDocument = null
            recordsHistoryManager = null
            annotationsEditor = null
            annotationsManager = null
        }
    }

    internal fun setCurrentToolInfo(toolInfo: AnnotationToolInfo) {
        val pdfDocument = editablePdfDocument
        when (toolInfo) {
            is Pen ->
                _drawingMode.value =
                    AnnotationDrawingMode.PenMode(toolInfo.brushSize, toolInfo.color)
            is Highlighter -> {
                if (toolInfo.color != null && pdfDocument != null) {
                    _drawingMode.value =
                        AnnotationDrawingMode.HighlighterMode(
                            toolInfo.brushSize,
                            toolInfo.color,
                            pdfDocument,
                        )
                } else {
                    // TODO: Add support for emoji highlighter
                }
            }
            is Eraser -> _drawingMode.value = AnnotationDrawingMode.EraserMode
        }
    }

    internal var initialFormFillingEnabledState: Boolean?
        get() = state[INITIAL_FORM_FILLING_STATE_KEY]
        set(value) {
            state[INITIAL_FORM_FILLING_STATE_KEY] = value
        }

    private fun withEditor(block: suspend (PdfAnnotationsEditor) -> Unit) {
        viewModelScope.launch {
            if (annotationsEditor == null) {
                setupManagersAndHandlers(
                    documentUri = state.get<Uri>(LOADED_DOCUMENT_URI_KEY),
                    document = editablePdfDocument,
                )
            }
            annotationsEditor?.let { block(it) }
        }
    }

    internal fun onBitmapFetched(pageNum: Int) {
        bitmapAvailabilityMap.set(pageNum)
        if (pageNum in visiblePageRange) {
            viewModelScope.launch { refreshVisibleAnnotations(visiblePageRange) }
        }
    }

    internal fun onBitmapCleared(pageNum: Int) {
        bitmapAvailabilityMap.clear(pageNum)
        if (pageNum in visiblePageRange) {
            viewModelScope.launch { refreshVisibleAnnotations(visiblePageRange) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal companion object {
        const val LOADED_DOCUMENT_URI_KEY = "loadedDocumentUri"
        private const val EDIT_MODE_ENABLED_KEY = "isEditModeEnabled"

        private const val ANNOTATION_VISIBLE_KEY = "isAnnotationVisible"
        private const val INITIAL_FORM_FILLING_STATE_KEY = "initialFormFillingState"

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
                    return (EditableDocumentViewModel(
                        savedStateHandle,
                        SandboxedPdfLoader(application, dispatcher),
                    ))
                        as T
                }
            }
    }
}
