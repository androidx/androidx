/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file    except in compliance with the License.
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

import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo

/**
 * An interface for rendering PDF documents.
 *
 * This interface provides a consistent API for interacting with PDF rendering engines, abstracting
 * away the differences between [android.graphics.pdf.PdfRenderer] and
 * [android.graphics.pdf.PdfRendererPreV] based on the Android OS version.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PdfDocumentRenderer : AutoCloseable {
    /**
     * Indicates whether the PDF document is linearized.
     *
     * A linearized PDF allows for faster initial display of the first page, as it optimizes the
     * file structure for progressive loading.
     */
    public val isLinearized: Boolean

    /** The total number of pages in the PDF document. */
    public val pageCount: Int

    /**
     * The type of form present in the PDF document. This value corresponds to the
     * `PdfDocument.FormType` constants.
     */
    public val formType: Int

    /**
     * Opens a page for rendering.
     *
     * @param pageNum The zero-based page number to open.
     * @param useCache Whether to use a cached instance of the page if available.
     * @return A [PdfPage] representing the opened page.
     * @see releasePage
     */
    public fun openPage(pageNum: Int, useCache: Boolean): PdfPage

    /**
     * Closes a page and releases its resources.
     *
     * This method should be called after finished using a page obtained from [openPage] to ensure
     * proper resource management. It also removes and clears any cached instance associated with
     * the page.
     *
     * @param page The [PdfPage] to close, or null if the page was not opened.
     * @param pageNum The zero-based page number of the page to close.
     */
    public fun releasePage(page: PdfPage?, pageNum: Int)

    /**
     * Writes the contents of the [androidx.pdf.PdfDocument] to the destination and closes the
     * [ParcelFileDescriptor]
     *
     * @param destination The [ParcelFileDescriptor] to write to.
     * @param removePasswordProtection Whether to remove password protection from the document.
     */
    public fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean)

    public fun <T> withPage(pageNum: Int, block: (PdfPage) -> T): T? {
        var page: PdfPage? = null
        var results: T?

        try {
            page = this.openPage(pageNum, useCache = false)
            results = block(page)
        } finally {
            this.releasePage(page, pageNum)
        }

        return results
    }
}
