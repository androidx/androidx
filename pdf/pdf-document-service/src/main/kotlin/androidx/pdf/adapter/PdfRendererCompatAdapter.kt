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

package androidx.pdf.adapter

import android.annotation.SuppressLint
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * A [PdfRendererCompatAdapter] implementation that uses the [PdfRenderer] class for rendering. This
 * adapter is intended for use on A. API levels < 31 (Android S).
 *
 * @param pfd The [ParcelFileDescriptor] representing the PDF document to render.
 * @constructor Creates a new [PdfRendererCompatAdapter] instance.
 */
@SuppressLint("ObsoleteSdkInt")
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PdfRendererCompatAdapter(pfd: ParcelFileDescriptor) : PdfDocumentRenderer {

    private val pageCache: PdfPageCache = PdfPageCache()
    private val pdfRenderer = PdfRenderer(pfd)

    override val linearizationStatus: Int
        get() =
            throw UnsupportedOperationException("Operation supported above S + SDK extension >= 13")

    override val pageCount: Int
        get() = pdfRenderer.pageCount

    override val formType: Int
        get() =
            throw UnsupportedOperationException("Operation supported above S + SDK extension >= 13")

    override fun openPage(pageNum: Int, useCache: Boolean): PdfPage {
        return pageCache.getOrUpdate(pageNum, useCache) {
            PdfPageCompatAdapter(pdfRenderer.openPage(pageNum))
        }
    }

    override fun releasePage(page: PdfPage?, pageNum: Int) {
        val removedPage = pageCache.remove(pageNum)
        if (removedPage == null) {
            page?.close()
        } else {
            removedPage.close()
            if (page != removedPage) page?.close()
        }
    }

    override fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean) {
        throw UnsupportedOperationException("Operation supported above S")
    }

    override fun close() {
        pageCache.clearAll()
        pdfRenderer.close()
    }
}
