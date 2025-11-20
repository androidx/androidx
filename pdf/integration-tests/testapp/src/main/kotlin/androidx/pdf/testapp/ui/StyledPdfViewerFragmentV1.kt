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

package androidx.pdf.testapp.ui

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.core.os.OperationCanceledException
import androidx.pdf.testapp.R
import androidx.pdf.viewer.fragment.PdfStylingOptions
import androidx.pdf.viewer.fragment.PdfViewerFragmentV1

@SuppressLint("RestrictedApiAndroidX")
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class StyledPdfViewerFragmentV1 : PdfViewerFragmentV1 {

    constructor() : super()

    private constructor(pdfStylingOptions: PdfStylingOptions) : super(pdfStylingOptions)

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        when (error) {
            is OperationCanceledException ->
                (activity as? OpCancellationHandler)?.handleCancelOperation()
        }
    }

    companion object {
        fun newInstance(): StyledPdfViewerFragmentV1 {
            val stylingOptions = PdfStylingOptions(R.style.PdfViewCustomization)

            return StyledPdfViewerFragmentV1(stylingOptions)
        }
    }
}
