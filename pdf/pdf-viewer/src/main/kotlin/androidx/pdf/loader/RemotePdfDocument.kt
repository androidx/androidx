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
package androidx.pdf.loader

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.util.Size
import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.pdf.loader.PdfDocument.BitmapSource
import androidx.pdf.loader.PdfDocument.PageInfo
import androidx.pdf.loader.PdfDocument.PdfPageContent
import androidx.pdf.loader.PdfDocument.PdfPageLinks
import androidx.pdf.models.PdfDocumentProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Represents a remote PDF document accessed through a connection to a [PdfDocumentProvider]
 * service. Provides methods to interact with the remote PDF content, including retrieving page
 * information, searching for text, and obtaining bitmap representations of pages.
 *
 * @param connection The AbstractPdfConnection used to communicate with the remote service.
 * @param dispatcher The dispatcher to use for coroutine operations.
 * @param pfd The ParcelFileDescriptor associated with the PDF document.
 * @param password (Optional) The password to unlock the PDF document if it is protected. Default
 *   value is null.
 * @param pageCount The total number of pages in the document.
 * @param isLinearized Indicates whether the document is linearized (optimized for web viewing).
 * @param formType The type of form present in the document (if any).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class RemotePdfDocument(
    private val connection: PdfServiceConnection,
    private val dispatcher: CoroutineDispatcher,
    private val pfd: ParcelFileDescriptor,
    private val password: String? = null,
    override val pageCount: Int,
    override val isLinearized: Boolean,
    override val formType: Int,
) : PdfDocument {

    /**
     * Helper function to perform remote operations on the PDF document.
     *
     * @param operation The lambda function to execute on the remote PdfDocumentProvider.
     * @return The result of the operation.
     * @throws RemoteException If there's an error communicating with the remote service.
     */
    private suspend fun <T> performRemoteOperation(operation: (PdfDocumentProvider) -> T): T =
        withContext(dispatcher) { operation(connection.pdfDocumentProvider) }

    /**
     * Retrieves information about the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PageInfo] object containing information about the page.
     */
    override suspend fun getPageInfo(pageNumber: Int): PageInfo {
        return performRemoteOperation { service ->
            val pageDimension = service.getPageDimensions(pageNumber)
            PageInfo(pageNumber, pageDimension.height, pageDimension.width)
        }
    }

    /**
     * Retrieves information about a range of pages.
     *
     * @param pageRange The range of page numbers (0-based).
     * @return A list of [PageInfo] objects, one for each page in the range.
     */
    override suspend fun getPageInfos(pageRange: IntRange): List<PageInfo> {
        return pageRange.map { getPageInfo(it) }
    }

    /**
     * Searches the document for the specified query within a range of pages.
     *
     * @param query The search query string.
     * @param pageRange The range of page numbers (0-based, inclusive) to search within.
     * @return A [SparseArray] mapping page numbers to lists of [PageMatchBounds] objects
     *   representing the search results on each page.
     */
    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange
    ): SparseArray<List<PageMatchBounds>> {
        return pageRange.map { searchDocument(query, it) }.toSparseArray()
    }

    /**
     * Searches the document for the specified query within a page.
     *
     * @param query The search query string.
     * @param pageNumber The page number (0-based).
     * @return A list of [PageMatchBounds] objects representing the search results on each page.
     */
    private suspend fun searchDocument(query: String, pageNumber: Int): List<PageMatchBounds> {
        return performRemoteOperation { service -> service.searchPageText(pageNumber, query) }
    }

    /**
     * Retrieves the selection bounds (in PDF coordinates) for the specified text selection.
     *
     * @param pageNumber The page on which text to be selected (0-based).
     * @param start The starting point of the text selection.
     * @param stop The ending point of the text selection.
     * @return A SparseArray mapping page numbers to [PageSelection] objects representing the
     *   selection bounds on each page.
     */
    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection {
        return performRemoteOperation { service ->
            val startBoundary = SelectionBoundary(Point(start.x.toInt(), start.y.toInt()))
            val stopBoundary = SelectionBoundary(Point(stop.x.toInt(), stop.y.toInt()))
            service.selectPageText(pageNumber, startBoundary, stopBoundary)
        }
    }

    /**
     * Retrieves the content (text and images) of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PdfPageContent] object representing the page's content.
     */
    override suspend fun getPageContent(pageNumber: Int): PdfPageContent {
        return performRemoteOperation { service ->
            PdfPageContent(service.getPageText(pageNumber), service.getPageImageContent(pageNumber))
        }
    }

    /**
     * Retrieves the links (Go To and external) present on the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PdfPageLinks] object representing the page's links.
     */
    override suspend fun getPageLinks(pageNumber: Int): PdfPageLinks {
        return performRemoteOperation { service ->
            PdfPageLinks(
                service.getPageGotoLinks(pageNumber),
                service.getPageExternalLinks(pageNumber)
            )
        }
    }

    /**
     * Gets a [BitmapSource] for retrieving bitmap representations of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [BitmapSource] for the specified page.
     */
    override fun getPageBitmapSource(pageNumber: Int): BitmapSource = PageBitmapSource(pageNumber)

    /**
     * Converts a list of items into a SparseArray, using the item's index as the key.
     *
     * @return A SparseArray containing the items from the list.
     */
    private fun <T> List<T>.toSparseArray(): SparseArray<T> {
        val sparseArray = SparseArray<T>()
        for ((index, item) in this.withIndex()) {
            sparseArray.put(index, item)
        }
        return sparseArray
    }

    override fun close() {
        connection.disconnect()
        pfd.close()
    }

    /**
     * Represents a source for retrieving bitmap representations of a specific page in the remote
     * PDF document.
     *
     * @param pageNumber The 0-based page number.
     */
    internal inner class PageBitmapSource(private val pageNumber: Int) : BitmapSource {
        /**
         * Retrieves a bitmap representation of the page.
         *
         * @param scaledPageSizePx The desired size of the bitmap in pixels.
         * @param tileRegion The optional region of the page to render (null for the entire page).
         * @return The bitmap of the specified page or region.
         */
        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            return performRemoteOperation { service ->
                if (tileRegion == null) {
                    service.getPageBitmap(
                        pageNumber,
                        scaledPageSizePx.width,
                        scaledPageSizePx.height
                    )
                } else {
                    val offsetX = tileRegion.left
                    val offsetY = tileRegion.top
                    service.getTileBitmap(
                        pageNumber,
                        tileRegion.width(),
                        tileRegion.height(),
                        scaledPageSizePx.width,
                        scaledPageSizePx.height,
                        offsetX,
                        offsetY
                    )
                }
            }
        }

        override fun close() {
            connection.pdfDocumentProvider.releasePage(pageNumber)
        }
    }
}
