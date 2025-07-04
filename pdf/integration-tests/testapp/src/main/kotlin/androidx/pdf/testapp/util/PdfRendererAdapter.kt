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

package androidx.pdf.testapp.util

import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRendererPreV
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ext.SdkExtensions

class PdfRendererAdapter(parcelFileDescriptor: ParcelFileDescriptor) {

    private var pdfRenderer: PdfRenderer? = null
    private var pdfRendererPreV: PdfRendererPreV? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            pdfRenderer = PdfRenderer(parcelFileDescriptor)
        } else {
            if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
                pdfRendererPreV = PdfRendererPreV(parcelFileDescriptor)
            }
        }
    }

    fun openPage(pageNum: Int): PdfPageAdapter {
        return when {
            pdfRenderer != null -> PdfPageAdapter(pdfRenderer!!, pageNum)
            pdfRendererPreV != null -> PdfPageAdapter(pdfRendererPreV!!, pageNum)
            else -> throw UnsupportedOperationException("PDF renderer not initialized")
        }
    }

    fun close() {
        pdfRenderer?.close()
        pdfRenderer = null

        if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= 13) {
            pdfRendererPreV?.close()
        }
        pdfRendererPreV = null
    }
}
