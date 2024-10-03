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

package androidx.pdf.viewmodel

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.pdf.data.DisplayData
import androidx.pdf.util.TileBoard
import androidx.pdf.viewer.loader.PdfLoader
import androidx.pdf.viewer.loader.PdfLoaderCallbacks
import androidx.pdf.viewer.loader.WeakPdfLoaderCallbacks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class PdfLoaderViewModel : ViewModel() {
    private val _pdfLoaderStateFlow = MutableStateFlow<PdfLoader?>(null)
    public val pdfLoaderStateFlow: StateFlow<PdfLoader?> = _pdfLoaderStateFlow.asStateFlow()

    private var currentData: DisplayData? = null

    public fun updatePdfLoader(
        context: Context,
        newData: DisplayData,
        callbacks: PdfLoaderCallbacks,
        onDocumentChanged: () -> Unit
    ) {
        // Case 1: New file opened. Replace the existing loader with a new one
        if (newData != currentData) {
            currentData = newData
            onDocumentChanged()
            _pdfLoaderStateFlow.update { currentLoader ->
                // Clear the existing loader
                currentLoader?.cancelAll()
                currentLoader?.disconnect()
                val loader =
                    PdfLoader.create(context, newData, TileBoard.DEFAULT_RECYCLER, callbacks, false)
                loader
            }
        } else {
            // Case 2: Configuration change. Update callbacks and re-trigger document loading
            val loader = _pdfLoaderStateFlow.value
            loader?.callbacks = WeakPdfLoaderCallbacks.wrap(callbacks)
            loader?.reloadDocument() // Force re-triggering documentLoaded flow
        }
    }

    @VisibleForTesting
    internal fun updatePdfLoader(pdfLoader: PdfLoader) {
        _pdfLoaderStateFlow.value = pdfLoader
    }
}
