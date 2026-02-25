/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.ink.fragment

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.SavedStateHandle
import androidx.pdf.FakeEditablePdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.annotation.models.AnnotationsDisplayState
import androidx.pdf.annotation.models.PathPdfObject
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.StampAnnotation
import androidx.pdf.coroutines.collectTill
import androidx.pdf.ink.EditableDocumentViewModel
import androidx.pdf.ink.model.ApplyEditsState
import androidx.pdf.ink.state.AnnotationDrawingMode
import androidx.pdf.ink.state.PdfEditMode
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_ANNOTATIONS
import androidx.pdf.ink.state.PdfEditMode.Companion.EDITING_JOURNEY_FORM_FILLING
import androidx.pdf.ink.util.InkDefaults
import androidx.pdf.ink.view.tool.Eraser
import androidx.pdf.ink.view.tool.Highlighter
import androidx.pdf.ink.view.tool.Pen
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@ExperimentalCoroutinesApi
class EditableDocumentViewModelTest {

    private lateinit var annotationsViewModel: EditableDocumentViewModel
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var fakeDocument: FakeEditablePdfDocument

    private val appContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val dispatcher = UnconfinedTestDispatcher()

    private val defaultDocumentUri = Uri.parse("content://test/document.pdf")

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)

        fakeDocument =
            FakeEditablePdfDocument(
                uri = defaultDocumentUri,
                pages = listOf(android.graphics.Point(100, 200), android.graphics.Point(100, 200)),
            )

        savedStateHandle = SavedStateHandle()
        annotationsViewModel =
            EditableDocumentViewModel(savedStateHandle, SandboxedPdfLoader(appContext, dispatcher))

        annotationsViewModel.maybeInitialiseForDocument(fakeDocument)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun statePersistence_restoresEditMode_afterRecreation() = runTest {
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled()

        val newViewModel =
            EditableDocumentViewModel(savedStateHandle, SandboxedPdfLoader(appContext, dispatcher))

        assertThat(newViewModel.pdfEditMode is PdfEditMode.Enabled).isTrue()
        assertThat(newViewModel.pdfEditModeFlow.first() is PdfEditMode.Enabled).isTrue()
        assertThat((newViewModel.pdfEditModeFlow.first() as PdfEditMode.Enabled).journey)
            .isEqualTo(EDITING_JOURNEY_ANNOTATIONS)
    }

    @Test
    fun editMode_setJourneyFormFilling() = runTest {
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Disabled).isTrue()
        annotationsViewModel.pdfEditMode =
            PdfEditMode.Enabled(journey = EDITING_JOURNEY_ANNOTATIONS)

        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Enabled).isTrue()
        assertThat(annotationsViewModel.pdfEditModeFlow.first() is PdfEditMode.Enabled).isTrue()
        assertThat((annotationsViewModel.pdfEditModeFlow.first() as PdfEditMode.Enabled).journey)
            .isEqualTo(EDITING_JOURNEY_ANNOTATIONS)
    }

    @Test
    fun statePersistence_restoresAnnotationVisibility_afterRecreation() = runTest {
        annotationsViewModel.areAnnotationsVisible = false

        val newViewModel =
            EditableDocumentViewModel(savedStateHandle, SandboxedPdfLoader(appContext, dispatcher))

        assertThat(newViewModel.areAnnotationsVisible).isFalse()
        assertThat(newViewModel.areAnnotationsVisibleFlow.first()).isFalse()
    }

    @Test
    fun resetState_clearsAnnotationStateAndDisablesEditMode() = runTest {
        val annotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(annotation)
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled()

        annotationsViewModel.resetState()

        assertThat(annotationsViewModel.pdfEditModeFlow.first() is PdfEditMode.Enabled).isFalse()
        val annotationsDisplayState = annotationsViewModel.annotationsDisplayStateFlow.first()
        assertThat(annotationsDisplayState).isEqualTo(AnnotationsDisplayState.EMPTY)
        assertThat(annotationsViewModel.applyEditsStatus.value).isEqualTo(ApplyEditsState.Ready)
    }

    @Test
    fun maybeInitialiseForDocument_resetsState_whenDocumentUriChanges() = runTest {
        val initialAnnotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(initialAnnotation)

        val initialDocUri = Uri.parse("content://test/test1.pdf")
        savedStateHandle[EditableDocumentViewModel.LOADED_DOCUMENT_URI_KEY] = initialDocUri
        annotationsViewModel.onBitmapFetched(0)
        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations
            )
            .isNotEmpty()

        val newDocUri = Uri.parse("content://test/test2.pdf")
        val newFakeDoc = FakeEditablePdfDocument(uri = newDocUri)

        annotationsViewModel.maybeInitialiseForDocument(newFakeDoc)

        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations
            )
            .isEmpty()
        assertThat(savedStateHandle.get<Uri>(EditableDocumentViewModel.LOADED_DOCUMENT_URI_KEY))
            .isEqualTo(newDocUri)
    }

    @Test
    fun cannotSwitchEditingJourney_withoutDisablingEditModeFirst() = runTest {
        // Check Editing Journey is disabled
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Disabled).isTrue()

        // Switch editing journey to annotations
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled(EDITING_JOURNEY_ANNOTATIONS)
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Enabled).isTrue()
        var editMode = annotationsViewModel.pdfEditMode as PdfEditMode.Enabled
        assertThat(editMode.journey).isEqualTo(EDITING_JOURNEY_ANNOTATIONS)

        // Try switching to form-filling
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled(EDITING_JOURNEY_FORM_FILLING)
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Enabled).isTrue()
        editMode = annotationsViewModel.pdfEditMode as PdfEditMode.Enabled
        // editMode should not change
        assertThat(editMode.journey).isEqualTo(EDITING_JOURNEY_ANNOTATIONS)

        // Disable and then try switching
        annotationsViewModel.pdfEditMode = PdfEditMode.Disabled
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Disabled).isTrue()
        // Switch to form-filling now
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled(EDITING_JOURNEY_FORM_FILLING)
        assertThat(annotationsViewModel.pdfEditMode is PdfEditMode.Enabled).isTrue()
        editMode = annotationsViewModel.pdfEditMode as PdfEditMode.Enabled
        // editMode should successfully change to form-filling
        assertThat(editMode.journey).isEqualTo(EDITING_JOURNEY_FORM_FILLING)
    }

    // --- Annotation Editing Tests ---

    @Test
    fun addDraftAnnotation_updatesDraftState_forSingleAnnotation() = runTest {
        val annotation = createAnnotation(pageNum = 0)

        annotationsViewModel.addDraftAnnotation(annotation)
        annotationsViewModel.onBitmapFetched(0)

        val firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]

        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)
        assertThat(firstPageEdits!!.first().annotation).isEqualTo(annotation)
    }

    @Test
    fun addAnnotations_updatesDraftState_forMultipleDraftAnnotationOnSamePage() = runTest {
        val annotation1 = createAnnotation(pageNum = 0, bounds = RectF(0f, 0f, 50f, 50f))
        val annotation2 = createAnnotation(pageNum = 0, bounds = RectF(50f, 50f, 100f, 100f))

        annotationsViewModel.addDraftAnnotation(annotation1)
        annotationsViewModel.addDraftAnnotation(annotation2)
        annotationsViewModel.onBitmapFetched(0)

        val firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]

        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(2)

        val annotationsInState = firstPageEdits!!.map { it.annotation }
        assertThat(annotationsInState).containsExactly(annotation1, annotation2)
    }

    @Test
    fun undo_removesLastAddedAnnotation_andUpdatesState() = runTest {
        val annotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(annotation)

        assertThat(annotationsViewModel.canUndo.first()).isTrue()

        annotationsViewModel.undo()

        val page0Edits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(page0Edits).isNull() // Should be removed/null
        assertThat(annotationsViewModel.canUndo.first()).isFalse()
        assertThat(annotationsViewModel.canRedo.first()).isTrue()
    }

    @Test
    fun redo_restoresLastUndoneAnnotation() = runTest {
        val annotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(annotation)
        annotationsViewModel.undo()
        annotationsViewModel.redo()
        annotationsViewModel.onBitmapFetched(0)

        val firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(firstPageEdits).hasSize(1)
        assertThat(annotationsViewModel.canUndo.first()).isTrue()
        assertThat(annotationsViewModel.canRedo.first()).isFalse()
    }

    @Test
    fun removeAnnotation_removesNewlyDrawnAnnotation() = runTest {
        val annotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(annotation)
        annotationsViewModel.addDraftAnnotation(annotation)
        annotationsViewModel.onBitmapFetched(0)

        // The annotations should be added and pageAnnotations List size should be 2.
        var firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(2)

        annotationsViewModel.removeAnnotation(firstPageEdits!!.first().key)
        annotationsViewModel.onBitmapFetched(0)

        // The annotation should be removed and pageAnnotations List size should now be 1.
        firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)
    }

    @Test
    fun removeAnnotation_removesExistingAnnotation() = runTest {
        val existingAnnotation = createAnnotation(pageNum = 0)
        val newUri = Uri.parse("content://test/new_doc.pdf")
        val documentWithAnnotation =
            FakeEditablePdfDocument(
                uri = newUri,
                initialEdits = listOf(existingAnnotation, existingAnnotation),
            )

        annotationsViewModel.maybeInitialiseForDocument(documentWithAnnotation)

        annotationsViewModel.fetchAnnotationsForPageRange(0, 0)
        annotationsViewModel.onBitmapFetched(0)

        // The annotations should be present and pageAnnotations List size should be 2.
        var firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(2)

        annotationsViewModel.removeAnnotation(firstPageEdits!!.first().key)

        // The annotation should be removed and pageAnnotations List size should now be 1.
        firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]
        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)
    }

    // --- Visible Page Range & Refresh Tests ---

    @Test
    fun fetchAnnotationsForPageRange_updatesStateWithExistingAnnotations() = runTest {
        val existingAnnotation = createAnnotation(pageNum = 0)
        val newUri = Uri.parse("content://test/new_doc.pdf")
        val documentWithAnnotation =
            FakeEditablePdfDocument(uri = newUri, initialEdits = listOf(existingAnnotation))

        annotationsViewModel.maybeInitialiseForDocument(documentWithAnnotation)

        annotationsViewModel.fetchAnnotationsForPageRange(0, 0)
        annotationsViewModel.onBitmapFetched(0)

        val firstPageEdits =
            annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                .pageAnnotations[0]

        assertThat(firstPageEdits).isNotNull()
        assertThat(firstPageEdits).hasSize(1)
        assertThat(firstPageEdits!!.first().annotation).isEqualTo(existingAnnotation)
    }

    @Test
    fun updateVisiblePageRange_usedByUndoRedo_toRefeshCorrectPages() = runTest {
        val annotationPage1 = createAnnotation(pageNum = 1)
        annotationsViewModel.visiblePageRange = 1..1
        annotationsViewModel.addDraftAnnotation(annotationPage1)
        annotationsViewModel.onBitmapFetched(1)

        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations[1]
            )
            .hasSize(1)

        annotationsViewModel.undo()

        val state = annotationsViewModel.annotationsDisplayStateFlow.value
        assertThat(state.visiblePageAnnotations.pageAnnotations[1]).isNull()
    }

    // --- Display State Tests ---

    @Test
    fun updateTransformationMatrices_updatesFlow() = runTest {
        val matrixPage0 = Matrix().apply { setScale(1f, 1f) }
        val matrixPage1 = Matrix().apply { setTranslate(10f, 10f) }
        val newMatrices = mapOf(0 to matrixPage0, 1 to matrixPage1)

        annotationsViewModel.updateTransformationMatrices(newMatrices)
        val emittedState = annotationsViewModel.annotationsDisplayStateFlow.first()

        assertThat(emittedState.transformationMatrices.size).isEqualTo(2)
        assertThat(emittedState.transformationMatrices[0]).isEqualTo(matrixPage0)
        assertThat(emittedState.transformationMatrices[1]).isEqualTo(matrixPage1)
    }

    // --- Interaction State Tests ---

    @Test
    fun initialAreAnnotationsEnabled_isTrue() = runTest {
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.first()).isFalse()
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled()

        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.first()).isTrue()
    }

    @Test
    fun setAnnotationVisibility_updatesIsAnnotationInteractionEnabled() = runTest {
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled()

        annotationsViewModel.areAnnotationsVisible = false
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.first()).isFalse()

        annotationsViewModel.areAnnotationsVisible = true
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.first()).isTrue()
    }

    @Test
    fun isPdfViewGestureActive_updatesIsAnnotationInteractionEnabled() = runTest {
        annotationsViewModel.pdfEditMode = PdfEditMode.Enabled()

        annotationsViewModel.areAnnotationsVisible = true
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.value).isTrue()

        // Disable interaction by activating gesture
        annotationsViewModel.isPdfViewGestureActive = true
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.value).isFalse()

        // Re-enable interaction by deactivating gesture
        annotationsViewModel.isPdfViewGestureActive = false
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.value).isTrue()
    }

    @Test
    fun updateDisplayState_onBitmapUpdated() = runTest {
        val annotationPage1 = createAnnotation(pageNum = 1)
        val annotationPage2 = createAnnotation(pageNum = 2)
        annotationsViewModel.visiblePageRange = 1..2
        annotationsViewModel.addDraftAnnotation(annotationPage1)
        annotationsViewModel.addDraftAnnotation(annotationPage2)
        annotationsViewModel.onBitmapFetched(1)

        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations[1]
            )
            .hasSize(1)
        annotationsViewModel.onBitmapFetched(2)
        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations
                    .size
            )
            .isEqualTo(2)

        annotationsViewModel.onBitmapCleared(1)
        assertThat(
                annotationsViewModel.annotationsDisplayStateFlow.value.visiblePageAnnotations
                    .pageAnnotations
                    .size
            )
            .isEqualTo(1)

        annotationsViewModel.onBitmapCleared(2)
        val state = annotationsViewModel.annotationsDisplayStateFlow.value
        assertThat(state.visiblePageAnnotations.pageAnnotations[1]).isNull()
    }

    // --- Apply Edits Tests ---

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun applyDraftEdits_updatesStatusFlow_onSuccess() = runTest {
        val annotation = createAnnotation(pageNum = 0)
        annotationsViewModel.addDraftAnnotation(annotation)

        val applyStates = mutableListOf<ApplyEditsState>()
        val collectJob =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                annotationsViewModel.applyEditsStatus.collectTill(applyStates) { state ->
                    state is ApplyEditsState.Success
                }
            }
        testScheduler.advanceUntilIdle()

        annotationsViewModel.applyDraftEdits()

        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.value).isFalse()

        collectJob.join()

        assertThat(applyStates).isNotEmpty()
        assertThat(applyStates.first()).isEqualTo(ApplyEditsState.Ready)
        assertThat(applyStates).contains(ApplyEditsState.InProgress)
        assertThat(applyStates.last()).isInstanceOf(ApplyEditsState.Success::class.java)

        assertThat(annotationsViewModel.hasUnsavedChanges()).isFalse()
        assertThat(annotationsViewModel.canUndo.value).isFalse()

        val savedAnnotations = fakeDocument.getAnnotationsForPage(0)
        assertThat(savedAnnotations).hasSize(1)
        assertThat(savedAnnotations.first().annotation).isEqualTo(annotation)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun applyDraftEdits_updatesStatusFlow_onFailure_whenDocumentIsNull() = runTest {
        annotationsViewModel.resetState() // Forces document null

        val applyStates = mutableListOf<ApplyEditsState>()
        val collectJob =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                annotationsViewModel.applyEditsStatus.collectTill(applyStates) { state ->
                    state is ApplyEditsState.Failure
                }
            }

        testScheduler.advanceUntilIdle()

        annotationsViewModel.applyDraftEdits()
        assertThat(annotationsViewModel.isAnnotationInteractionEnabled.value).isFalse()
        collectJob.join()

        assertThat(applyStates).isNotEmpty()
        assertThat(applyStates.last()).isInstanceOf(ApplyEditsState.Failure::class.java)
        // Note: The message string comes from your ViewModel implementation
        assertThat((applyStates.last() as ApplyEditsState.Failure).error.message)
            .contains("Document not available")
    }

    // --- Tool Selection Tests ---

    @Test
    fun setCurrentToolInfo_updatesDrawingMode_whenPenSelected() = runTest {
        val penTool = Pen(brushSize = 2.0f, color = Color.RED)
        annotationsViewModel.setCurrentToolInfo(penTool)

        val drawingMode = annotationsViewModel.drawingMode.first()
        assertThat(drawingMode).isInstanceOf(AnnotationDrawingMode.PenMode::class.java)
        assertThat((drawingMode as AnnotationDrawingMode.PenMode).size).isEqualTo(2.0f)
        assertThat(drawingMode.color).isEqualTo(Color.RED)
    }

    @Test
    fun setCurrentToolInfo_updatesDrawingMode_whenHighlighterSelected() = runTest {
        val highlighterTool = Highlighter(brushSize = 10.0f, color = Color.YELLOW, emoji = null)
        annotationsViewModel.setCurrentToolInfo(highlighterTool)

        val drawingMode = annotationsViewModel.drawingMode.first()
        assertThat(drawingMode).isInstanceOf(AnnotationDrawingMode.HighlighterMode::class.java)
        assertThat((drawingMode as AnnotationDrawingMode.HighlighterMode).size).isEqualTo(10.0f)
        val colorWithHighlighterAlpha =
            ColorUtils.setAlphaComponent(Color.YELLOW, InkDefaults.HIGHLIGHTER_ALPHA)
        assertThat(drawingMode.color).isEqualTo(colorWithHighlighterAlpha)
    }

    @Test
    fun setCurrentToolInfo_updatesDrawingMode_whenEraserSelected() = runTest {
        annotationsViewModel.setCurrentToolInfo(Eraser)
        val drawingMode = annotationsViewModel.drawingMode.first()
        assertThat(drawingMode).isEqualTo(AnnotationDrawingMode.EraserMode)
    }

    // --- Helpers ---

    fun createAnnotation(
        pageNum: Int = 0,
        bounds: RectF = RectF(0f, 0f, 100f, 100f),
    ): PdfAnnotation {
        return StampAnnotation(
            pageNum = pageNum,
            bounds = bounds,
            pdfObjects =
                listOf(PathPdfObject(brushColor = 0, brushWidth = 0f, inputs = emptyList())),
        )
    }
}
