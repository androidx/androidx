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
@file:Suppress("BanConcurrentHashMap")

package androidx.pdf.adapter

import android.graphics.pdf.LoadParams
import android.graphics.pdf.PdfRendererPreV
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo

/**
 * A [PdfDocumentRenderer] implementation that uses the [PdfRendererPreV] class for rendering.
 *
 * This adapter provides a consistent interface for interacting with [PdfRendererPreV], allowing it
 * to be used interchangeably with other PDF rendering implementations.
 *
 * @param pfd The [ParcelFileDescriptor] representing the PDF document to render.
 * @param password The password to use for decrypting the PDF, or null if no password is required.
 * @constructor Creates a new [PdfDocumentRendererPreVAdapter] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
internal class PdfDocumentRendererPreVAdapter(pfd: ParcelFileDescriptor, password: String?) :
    PdfDocumentRenderer {
    private val pdfRendererPreV =
        PdfRendererPreV(pfd, /* params= */ LoadParams.Builder().setPassword(password).build())
    private val pageCache: PdfPageCache = PdfPageCache()

    override val isLinearized: Boolean
        get() =
            pdfRendererPreV.documentLinearizationType ==
                PdfRendererPreV.DOCUMENT_LINEARIZED_TYPE_LINEARIZED

    override val pageCount: Int
        get() = pdfRendererPreV.pageCount

    override val formType: Int
        get() = pdfRendererPreV.pdfFormType

    /** Caller should use [releasePage] to close the page resource reliably after usage. */
    override fun openPage(pageNum: Int, useCache: Boolean): PdfPage {
        return pageCache.getOrUpdate(pageNum, useCache) {
            PdfPagePreVAdapter(pdfRendererPreV.openPage(pageNum))
        }
    }

    /** Closes the page. Also removes and clears the cached instance, if held. */
    override fun releasePage(page: PdfPage?, pageNum: Int) {
        val removedPage = pageCache.remove(pageNum)
        if (removedPage == null) {
            page?.close()
        } else {
            removedPage.close()
            if (page != removedPage) {
                page?.close()
            }
        }
    }

    override fun close() {
        pageCache.clearAll()
        pdfRendererPreV.close()
    }

    override fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean) {
        pdfRendererPreV.write(destination, removePasswordProtection)
    }
}
