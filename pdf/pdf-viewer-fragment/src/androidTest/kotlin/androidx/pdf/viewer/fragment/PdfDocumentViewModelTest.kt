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

import android.graphics.Rect
import android.net.Uri
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.SavedStateHandle
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.viewer.coroutines.collectTill
import androidx.pdf.viewer.coroutines.toListDuring
import androidx.pdf.viewer.fragment.TestUtils.openFileAsUri
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35)
class PdfDocumentViewModelTest {

    private val appContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private lateinit var pdfDocumentViewModel: PdfDocumentViewModel
    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    fun setup() {
        val savedStateHandle = SavedStateHandle()

        pdfDocumentViewModel =
            PdfDocumentViewModel(savedStateHandle, SandboxedPdfLoader(appContext, dispatcher))
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentSuccess() = runTest {
        val documentUri = openFileAsUri(appContext, "sample.pdf")

        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)

        backgroundScope.launch {
            // Collect Ui states
            val uiStates = pdfDocumentViewModel.fragmentUiScreenState.toListDuring(200)
            // Since we've selected a error-free unprotected pdf,
            // ideally there should only 2 states transitions.
            assertTrue(uiStates.size == 2)
            // Assert the first state emitted was loading
            assertTrue(uiStates.first() is PdfFragmentUiState.Loading)
            // Assert the first state emitted was Document load
            assertTrue(uiStates.last() is PdfFragmentUiState.DocumentLoaded)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentSuccess_withStateSaved() = runTest {
        val documentUri = openFileAsUri(appContext, "sample.pdf")
        // Save document uri in savedStateHandle and inject in viewmodel
        val savedState = SavedStateHandle().also { it["documentUri"] = documentUri }
        // On init, pdfViewModel will try to load document again as documentUri != null
        val pdfViewModel =
            PdfDocumentViewModel(savedState, SandboxedPdfLoader(appContext, dispatcher))

        backgroundScope.launch {
            val uiStates = pdfViewModel.fragmentUiScreenState.toListDuring(200)

            // Assert there are only 2 state transitions
            assertTrue(uiStates.size == 2)

            // Assert the first state emitted was loading
            assertTrue(uiStates.first() is PdfFragmentUiState.Loading)
            // Assert the first state emitted was Document load
            assertTrue(uiStates.last() is PdfFragmentUiState.DocumentLoaded)
        }
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentNotTriggerForSameUri() = runTest {
        val documentUri = openFileAsUri(appContext, "sample.pdf")
        // Save document uri in savedStateHandle and inject in viewmodel
        val savedState = SavedStateHandle().also { it["documentUri"] = documentUri }
        // On init, pdfViewModel will try to load document again as documentUri != null
        val pdfViewModel =
            PdfDocumentViewModel(savedState, SandboxedPdfLoader(appContext, dispatcher))
        // Ignore the first 2 fragmentUiStates, as it's the first load
        pdfViewModel.fragmentUiScreenState.take(2).toList()
        // try loading the document again with same uri
        pdfViewModel.loadDocument(uri = documentUri, password = null)

        // Assert fragmentUiState never set to Loading
        assertTrue(pdfViewModel.fragmentUiScreenState.value !is PdfFragmentUiState.Loading)
    }

    @Test
    fun test_pdfDocumentViewModel_dismissPasswordDialogCheckOperationCanceledException() = runTest {
        val savedState = SavedStateHandle()

        val pdfViewModel =
            PdfDocumentViewModel(savedState, SandboxedPdfLoader(appContext, dispatcher))

        pdfViewModel.passwordDialogCancelled()

        // Assert fragmentUiState is set to DocumentError
        assertTrue(pdfViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentError)

        val state = pdfViewModel.fragmentUiScreenState.value as PdfFragmentUiState.DocumentError

        // Assert exception is OperationCanceledException
        assertTrue(state.exception is OperationCanceledException)
    }

    @Test
    fun test_pdfDocumentViewModel_toogleImmersiveModeInLoadingState() = runTest {
        val savedState = SavedStateHandle()
        // Not Providing document uri, so the state should be loading
        val pdfViewModel =
            PdfDocumentViewModel(savedState, SandboxedPdfLoader(appContext, dispatcher))

        // Assert fragmentUiState is set to Loading
        assertTrue(pdfViewModel.fragmentUiScreenState.value is PdfFragmentUiState.Loading)

        pdfViewModel.setImmersiveModeDesired(enterImmersive = true)

        // Assert immersive mode never set to true
        assertFalse(pdfViewModel.isImmersiveModeDesired)
    }

    @Test
    fun test_pdfDocumentViewModel_toogleImmersiveModeInDocumentErrorState() = runTest {
        val documentUri = openFileAsUri(appContext, CORRUPTED_PDF)

        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(
                mutableListOf<PdfFragmentUiState>()
            ) { state ->
                state is PdfFragmentUiState.DocumentError
            }
        }

        // load pdf document
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)

        // wait till collection is completed
        collectJob.join()

        // Assert fragmentUiState is set to DocumentError
        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentError
        )

        pdfDocumentViewModel.setImmersiveModeDesired(enterImmersive = true)

        // Assert immersive mode never set to true
        assertFalse(pdfDocumentViewModel.isImmersiveModeDesired)
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentFailure_corruptedPdf() = runTest {
        val documentUri = openFileAsUri(appContext, CORRUPTED_PDF)

        val uiStates = mutableListOf<PdfFragmentUiState>()
        // start collecting Ui states
        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(uiStates) { state ->
                state is PdfFragmentUiState.DocumentError
            }
        }
        // load pdf document
        pdfDocumentViewModel.loadDocument(documentUri, null)

        // wait till collection is completed
        collectJob.join()

        // Since we've selected a corrupted unprotected pdf,
        // ideally there should only 2 states transitions.
        assertTrue(uiStates.size == 2)
        // Assert the first state emitted was loading
        assertTrue(uiStates.first() is PdfFragmentUiState.Loading)
        // Assert the last state emitted was Document error
        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentError
        )
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentFailure_invalidUriPath() = runTest {
        val documentUri =
            Uri.parse("file:///data/data/com.example.app/invalid/path/to/nonexistent/file.pdf")

        val uiStates = mutableListOf<PdfFragmentUiState>()
        // start collecting Ui states
        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(uiStates) { state ->
                state is PdfFragmentUiState.DocumentError
            }
        }
        // load pdf document
        pdfDocumentViewModel.loadDocument(documentUri, null)

        // wait till collection is completed
        collectJob.join()

        // Since we've selected a invalid Uri Path,
        // ideally there should only 2 states transitions.
        assertTrue(uiStates.size == 2)
        // Assert the first state emitted was loading
        assertTrue(uiStates.first() is PdfFragmentUiState.Loading)
        // Assert the last state emitted was Document error
        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentError
        )
    }

    @Test
    fun test_pdfDocumentViewModel_loadDocumentFailure_invalidUriScheme() = runTest {
        val documentUri = Uri.parse("xyz://path/to/sample.pdf")

        val uiStates = mutableListOf<PdfFragmentUiState>()
        // start collecting Ui states
        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(uiStates) { state ->
                state is PdfFragmentUiState.DocumentError
            }
        }
        // load pdf document
        pdfDocumentViewModel.loadDocument(documentUri, null)

        // wait till collection is completed
        collectJob.join()

        // Since we've selected a invalid Uri Scheme,
        // ideally there should only 2 states transitions.
        assertTrue(uiStates.size == 2)
        // Assert the first state emitted was loading
        assertTrue(uiStates.first() is PdfFragmentUiState.Loading)
        // Assert the last state emitted was Document error
        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentError
        )
    }

    @Test
    fun testApplyFormEdit_withEditableDocument_appliesTheEdit() = runTest {
        // 1. Arrange: Load a real document with form fields.
        val documentUri = openFileAsUri(appContext, "click_form.pdf")
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)

        // Wait until the document is loaded.
        val uiStates = mutableListOf<PdfFragmentUiState>()
        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(uiStates) { state ->
                state is PdfFragmentUiState.DocumentLoaded
            }
        }
        collectJob.join()
        val loadedState = pdfDocumentViewModel.fragmentUiScreenState.value
        assertTrue(loadedState is PdfFragmentUiState.DocumentLoaded)
        val document = (loadedState as PdfFragmentUiState.DocumentLoaded).pdfDocument

        // Use a latch to wait for the async edit to complete.
        val latch = CountDownLatch(1)
        var editApplied = false
        var editedPageNum: Int? = null
        var dirtyAreasOnEdit: List<Rect>? = null
        val listener =
            object : PdfDocument.OnPdfContentInvalidatedListener {
                override fun onPdfContentInvalidated(pageNumber: Int, dirtyAreas: List<Rect>) {
                    editApplied = true
                    editedPageNum = pageNumber
                    dirtyAreasOnEdit = dirtyAreas
                    latch.countDown()
                }
            }
        document.addOnPdfContentInvalidatedListener({ command -> command.run() }, listener)

        // Bounds in content coordinates of the widget on which the edit is applied
        val widgetArea = Rect(135, 70, 155, 90)
        val formEditInfo =
            FormEditInfo.createClick(widgetIndex = 1, clickPoint = PdfPoint(0, 145f, 80f))

        // 2. Act: Call the method on the ViewModel.
        pdfDocumentViewModel.applyFormEdit(formEditInfo)

        val wasApplied = latch.await(5, TimeUnit.SECONDS)
        document.removeOnPdfContentInvalidatedListener(listener)

        assertTrue(wasApplied)
        assertTrue(editApplied)
        assertTrue(editedPageNum == 0)
        assertTrue(dirtyAreasOnEdit != null)
        assertTrue(fullyContains(listOf(widgetArea), dirtyAreasOnEdit!!))
    }

    @Test
    fun test_formFilling_stateRestoration() = runTest {
        // Assemble a list of edits which were applied to the document.
        // This replicates a scenario where process death occurs after edits were applied.
        val formEditInfos = ArrayList<FormEditInfo>()
        // CheckBox at index 1 is un-checked, becomes checked post this edit, i.e. textValue = true
        val clickOnCheckBox =
            FormEditInfo.createClick(widgetIndex = 1, clickPoint = PdfPoint(0, 145f, 80f))
        // Radio button at widgetIndex 5 is selected, widgetIndex 7 (derived from metadata) which
        // was initially selected will become unselected.
        val clickOnRadioButton =
            FormEditInfo.createClick(widgetIndex = 5, clickPoint = PdfPoint(0, 95f, 240f))
        formEditInfos.add(clickOnCheckBox)
        formEditInfos.add(clickOnRadioButton)

        val documentUri = openFileAsUri(appContext, "click_form.pdf")
        val localSavedStateHandle =
            SavedStateHandle().also {
                it["documentUri"] = documentUri
                it["formEditInfos"] = formEditInfos
            }

        val pdfDocumentViewModel =
            PdfDocumentViewModel(localSavedStateHandle, SandboxedPdfLoader(appContext, dispatcher))

        val collectJob = launch {
            pdfDocumentViewModel.fragmentUiScreenState.collectTill(mutableListOf()) { state ->
                state is PdfFragmentUiState.DocumentLoaded
            }
        }
        pdfDocumentViewModel.loadDocument(uri = documentUri, password = null)
        collectJob.join()

        assertTrue(
            pdfDocumentViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentLoaded
        )
        val loadedState = pdfDocumentViewModel.fragmentUiScreenState.value
        assertTrue(loadedState is PdfFragmentUiState.DocumentLoaded)
        val document = (loadedState as PdfFragmentUiState.DocumentLoaded).pdfDocument
        val formWidgetInfos = document.getFormWidgetInfos(0)
        val expectedFormWidgetInfoIndex1 =
            FormWidgetInfo.createCheckbox(
                widgetIndex = 1,
                widgetRect = Rect(135, 70, 155, 90),
                textValue = "true",
                accessibilityLabel = "checkbox",
                isReadOnly = false,
            )
        val expectedFormWidgetInfoIndex5 =
            FormWidgetInfo.createRadioButton(
                widgetIndex = 5,
                widgetRect = Rect(85, 230, 105, 250),
                textValue = "true",
                accessibilityLabel = "",
                isReadOnly = false,
            )
        val expectedFormWidgetInfoIndex7 =
            FormWidgetInfo.createRadioButton(
                widgetIndex = 7,
                widgetRect = Rect(185, 230, 205, 250),
                textValue = "false",
                accessibilityLabel = "",
                isReadOnly = false,
            )

        for (widget: FormWidgetInfo in formWidgetInfos) {
            when (widget.widgetIndex) {
                1 -> assertTrue(widget == expectedFormWidgetInfoIndex1)
                5 -> assertTrue(widget == expectedFormWidgetInfoIndex5)
                7 -> assertTrue(widget == expectedFormWidgetInfoIndex7)
            }
        }
    }

    @Test
    fun test_pdfDocumentViewModel_forceLoadDocument_reloadsSuccessfully() = runTest {
        val documentUri = openFileAsUri(appContext, "sample.pdf")
        val savedState = SavedStateHandle()
        val testViewModel =
            TestPdfDocumentViewModel(savedState, SandboxedPdfLoader(appContext, dispatcher))

        testViewModel.loadDocument(uri = documentUri, password = null)

        testViewModel.fragmentUiScreenState.first { it is PdfFragmentUiState.DocumentLoaded }
        assertTrue(testViewModel.fragmentUiScreenState.value is PdfFragmentUiState.DocumentLoaded)

        val uiStates = mutableListOf<PdfFragmentUiState>()
        val reloadJob = launch {
            testViewModel.fragmentUiScreenState.collectTill(uiStates) { state ->
                state is PdfFragmentUiState.DocumentLoaded &&
                    uiStates.any { it is PdfFragmentUiState.Loading }
            }
        }

        // force load document without current state
        testViewModel.forceLoadDocument()
        reloadJob.join()

        // Should contain Loading followed by DocumentLoaded
        assertTrue(uiStates.any { it is PdfFragmentUiState.Loading })
        assertTrue(uiStates.last() is PdfFragmentUiState.DocumentLoaded)
        // Assert states get reset
        assertFalse(testViewModel.isTextSearchActiveFromState)
        assertFalse(testViewModel.isImmersiveModeDesired)
        assertFalse(savedState.contains("formEditInfos"))
    }

    private fun fullyContains(innerRects: List<Rect>, outerRects: List<Rect>): Boolean {
        return innerRects.all { inner -> outerRects.any { outer -> outer.contains(inner) } }
    }

    /** A test-only subclass to expose protected methods for verification. */
    private class TestPdfDocumentViewModel(
        savedStateHandle: SavedStateHandle,
        pdfLoader: SandboxedPdfLoader,
    ) : PdfDocumentViewModel(savedStateHandle, pdfLoader) {
        public override fun forceLoadDocument() {
            super.forceLoadDocument()
        }
    }

    companion object {
        private const val CORRUPTED_PDF = "corrupted.pdf"
    }
}
