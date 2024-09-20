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

package androidx.pdf.data

import androidx.annotation.RestrictTo

/** Represents the loading status of a PDF file. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class PdfLoadingStatus {
    SUCCESS, // The PDF was loaded successfully.
    WRONG_PASSWORD, // Incorrect password was provided for a password-protected PDF.
    PDF_ERROR, // Invalid or Corrupt pdf file was provided
    LOADING_ERROR // A general error occurred while trying to load the PDF
}
