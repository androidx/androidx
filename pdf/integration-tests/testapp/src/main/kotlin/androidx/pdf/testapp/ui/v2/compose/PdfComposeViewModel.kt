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

package androidx.pdf.testapp.ui.v2.compose

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.pdf.PdfDocument
import androidx.pdf.PdfLoader
import androidx.pdf.SandboxedPdfLoader
import java.util.concurrent.Executors
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** [ViewModel] to load [PdfDocument]s and maintain state on behalf of [PdfComposeFragment] */
@SuppressLint("RestrictedApiAndroidX")
internal class PdfComposeViewModel(
    private val pdfLoader: PdfLoader,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _loadedDocumentStateFlow = MutableStateFlow<PdfDocument?>(null)
    val loadedDocumentStateFlow: StateFlow<PdfDocument?> = _loadedDocumentStateFlow

    private val documentLoadJob: Job? = null

    var documentUri: Uri? = savedStateHandle[KEY_DOCUMENT_URI]
        set(value) {
            field = value
            savedStateHandle[KEY_DOCUMENT_URI] = value
            field?.let { loadDocument(it) }
        }

    private fun loadDocument(uri: Uri) {
        val prevJob = documentLoadJob

        viewModelScope.launch {
            prevJob?.cancelAndJoin()
            val document =
                try {
                    pdfLoader.openDocument(uri)
                } catch (ex: Exception) {
                    // TODO - handle password and I/O exceptions
                    null
                }
            _loadedDocumentStateFlow.update { document }
        }
    }

    companion object {
        private const val KEY_DOCUMENT_URI = "androidx.pdf.testapp.ui.v2.compose.KEY_DOCUMENT_URI"

        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    val application =
                        checkNotNull(extras[APPLICATION_KEY]) { "Application required" }
                    val savedStateHandle = extras.createSavedStateHandle()

                    val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
                    return (PdfComposeViewModel(
                        SandboxedPdfLoader(application, dispatcher),
                        savedStateHandle,
                    ))
                        as T
                }
            }
    }
}
