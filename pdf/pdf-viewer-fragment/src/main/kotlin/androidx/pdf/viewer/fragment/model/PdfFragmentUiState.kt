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

package androidx.pdf.viewer.fragment.model

import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocument

/**
 * A sealed interface representing the various UI states of the PdfViewerFragment.
 *
 * This sealed interface ensures that the UI states are mutually exclusive, meaning the fragment can
 * only be in one state at a time. Each possible state is defined as a separate object/class that
 * implements this interface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public sealed interface PdfFragmentUiState {
    /** Indicates that the PDF document is loading. */
    public object Loading : PdfFragmentUiState

    /**
     * Indicates that the PDF document has been loaded successfully.
     *
     * @property pdfDocument The loaded PDF document.
     */
    public class DocumentLoaded(public val pdfDocument: PdfDocument) : PdfFragmentUiState

    /**
     * Indicates that an error occurred while loading the PDF document.
     *
     * @property exception The exception that occurred.
     */
    public class DocumentError(public val exception: Exception) : PdfFragmentUiState

    /**
     * Indicates that the PDF document is password-protected and requires a password to be entered.
     *
     * @property passwordFailed Whether this is a retry attempt after an incorrect password was
     *   entered.
     */
    public class PasswordRequested(public val passwordFailed: Boolean) : PdfFragmentUiState
}
