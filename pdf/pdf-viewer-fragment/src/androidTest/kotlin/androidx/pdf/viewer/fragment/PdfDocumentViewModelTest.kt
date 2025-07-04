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
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.SavedStateHandle
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.viewer.coroutines.collectTill
import androidx.pdf.viewer.coroutines.toListDuring
import androidx.pdf.viewer.fragment.TestUtils.openFileAsUri
import androidx.pdf.viewer.fragment.model.PdfFragmentUiState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
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

    companion object {
        private const val CORRUPTED_PDF = "corrupted.pdf"
    }
}
