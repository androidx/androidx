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

package androidx.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.DeadObjectException
import android.os.ParcelFileDescriptor
import android.util.Size
import android.util.SparseArray
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.annotation.WorkerThread
import androidx.pdf.PdfDocument.BitmapSource
import androidx.pdf.PdfDocument.Companion.INCLUDE_FORM_WIDGET_INFO
import androidx.pdf.PdfDocument.DocumentClosedException
import androidx.pdf.PdfDocument.PdfPageContent
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.service.connect.PdfServiceConnection
import androidx.pdf.utils.toAndroidClass
import androidx.pdf.utils.toContentClass
import java.util.concurrent.TimeoutException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A concrete implementation of the [PdfDocument] interface that interacts with a PDF document
 * through a sandboxed Android service.
 *
 * This class is created when a successful connection is established to the [PdfDocumentServiceImpl]
 * and the PDF document is loaded using the [PdfRenderer]. It provides a remote interface for
 * clients to interact with the document, enabling operations like rendering pages, extracting text,
 * searching for content, and accessing metadata.
 *
 * The communication with the service is handled through a [PdfServiceConnection], ensuring that the
 * PDF rendering operations are performed in a separate process for security and stability.
 *
 * @param uri The URI of the PDF document.
 * @param fileDescriptor The [ParcelFileDescriptor] associated with the document.
 * @param connection The [PdfServiceConnection] used to interact with the service.
 * @param coroutineContext The [CoroutineContext] used for asynchronous operations, particularly for
 *   I/O-bound tasks such as interacting with the PDF service. It is recommended to use a dispatcher
 *   appropriate for blocking I/O operations, such as `Dispatchers.IO`.
 * @param pageCount The total number of pages in the document.
 * @param isLinearized Indicates whether the document is linearized.
 * @param formType The type of form present in the document.
 * @constructor Creates a new [SandboxedPdfDocument] instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SandboxedPdfDocument(
    override val uri: Uri,
    public val connection: PdfServiceConnection,
    private val password: String?,
    private val fileDescriptor: ParcelFileDescriptor,
    private val coroutineContext: CoroutineContext,
    override val pageCount: Int,
    override val isLinearized: Boolean,
    override val formType: Int,
) : PdfDocument {

    /** The [CoroutineScope] we use to close [BitmapSource]s asynchronously */
    private val closeScope = CoroutineScope(coroutineContext + SupervisorJob())

    /**
     * Indicates whether this [androidx.pdf.SandboxedPdfDocument] is closed explicitly by calling
     * [close].
     *
     * Once closed, any further operations on the document are invalid.
     */
    private var isDocumentClosedExplicitly = false

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return getPageInfo(pageNumber, PdfDocument.PageInfoFlags.of(0))
    }

    override suspend fun getPageInfo(
        pageNumber: Int,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): PdfDocument.PageInfo {
        return withDocument { document ->
            // TODO(b/407777410): Update the logic so that callers can refetch the information in
            // case
            //  default value is returned
            val dimensions = document.getPageDimensions(pageNumber)

            // Check if the INCLUDE_FORM_WIDGET_INFO flag is set
            val formWidgetInfo =
                if (pageInfoFlags.value and INCLUDE_FORM_WIDGET_INFO != 0L) {
                    document.getFormWidgetInfos(pageNumber).map { it.toContentClass() }
                } else {
                    null
                }

            if (dimensions == null || dimensions.height <= 0 || dimensions.width <= 0) {
                PdfDocument.PageInfo(pageNumber, DEFAULT_PAGE, DEFAULT_PAGE, formWidgetInfo)
            } else {
                PdfDocument.PageInfo(
                    pageNumber,
                    dimensions.height,
                    dimensions.width,
                    formWidgetInfo,
                )
            }
        }
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(pageNumber = it) }
    }

    override suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(pageNumber = it, pageInfoFlags = pageInfoFlags) }
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> {
        return withDocument { document ->
            SparseArray<List<PageMatchBounds>>(pageRange.last + 1).apply {
                pageRange.forEach { pageNum ->
                    (document.searchPageText(pageNum, query) ?: listOf())
                        .takeIf { it.isNotEmpty() }
                        ?.let { put(pageNum, it.map { result -> result.toContentClass() }) }
                }
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection? {
        return withDocument { document ->
            val startBoundary =
                SelectionBoundary(point = Point(start.x.toInt(), start.y.toInt())).toAndroidClass()
            val stopBoundary =
                SelectionBoundary(point = Point(stop.x.toInt(), stop.y.toInt())).toAndroidClass()
            document.selectPageText(pageNumber, startBoundary, stopBoundary)?.toContentClass()
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection? {
        return withDocument { document ->
            document
                .selectPageText(
                    pageNumber,
                    android.graphics.pdf.models.selection.SelectionBoundary(0),
                    android.graphics.pdf.models.selection.SelectionBoundary(Int.MAX_VALUE),
                )
                ?.toContentClass()
        }
    }

    override suspend fun getPageContent(pageNumber: Int): PdfPageContent {
        return withDocument { document ->
            val textContents =
                document.getPageText(pageNumber)?.map { it.toContentClass() } ?: listOf()
            val imageContents =
                document.getPageImageContent(pageNumber)?.map { it.toContentClass() } ?: listOf()
            PdfPageContent(textContents, imageContents)
        }
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        return withDocument { document ->
            val gotoLinks =
                document.getPageGotoLinks(pageNumber)?.map { it.toContentClass() } ?: listOf()
            val externalLinks =
                document.getPageExternalLinks(pageNumber)?.map { it.toContentClass() } ?: listOf()
            PdfDocument.PdfPageLinks(gotoLinks, externalLinks)
        }
    }

    override fun getPageBitmapSource(pageNumber: Int): BitmapSource = PageBitmapSource(pageNumber)

    override suspend fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo> {
        return getFormWidgetInfos(pageNum, intArrayOf())
    }

    override suspend fun getFormWidgetInfos(pageNum: Int, types: IntArray): List<FormWidgetInfo> {
        return withDocument { document ->
            document.getFormWidgetInfosOfType(pageNum, types).map { it.toContentClass() }
        }
    }

    override suspend fun applyEdit(pageNum: Int, record: FormEditRecord): List<Rect> {
        return withDocument { document -> document.applyEdit(pageNum, record.toAndroidClass()) }
    }

    override suspend fun write(destination: ParcelFileDescriptor) {
        return withDocument { document ->
            document.write(destination, /* removePasswordProtection= */ false)
        }
    }

    @WorkerThread
    override fun close() {
        isDocumentClosedExplicitly = true

        connection.disconnect()

        // TODO(b/377920470): Remove this when PdfRenderer closes the file descriptor
        fileDescriptor.close()
    }

    /**
     * Represents a source for retrieving bitmap representations of a specific page in the remote
     * PDF document.
     *
     * @param pageNumber The 0-based page number.
     */
    internal inner class PageBitmapSource(override val pageNumber: Int) : BitmapSource {
        /**
         * Retrieves a bitmap representation of the page.
         *
         * @param scaledPageSizePx The desired size of the bitmap in pixels.
         * @param tileRegion The optional region of the page to render (null for the entire page).
         * @return The bitmap of the specified page or region.
         */
        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            return withDocument { document ->
                if (tileRegion == null) {
                    document.getPageBitmap(
                        pageNumber,
                        scaledPageSizePx.width,
                        scaledPageSizePx.height,
                    ) ?: getDefaultBitmap(scaledPageSizePx.width, scaledPageSizePx.height)
                } else {
                    val offsetX = tileRegion.left
                    val offsetY = tileRegion.top
                    document.getTileBitmap(
                        pageNumber,
                        tileRegion.width(),
                        tileRegion.height(),
                        scaledPageSizePx.width,
                        scaledPageSizePx.height,
                        offsetX,
                        offsetY,
                    ) ?: getDefaultBitmap(tileRegion.width(), tileRegion.height())
                }
            }
        }

        private fun getDefaultBitmap(width: Int, height: Int): Bitmap {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            output.eraseColor(Color.WHITE)
            return output
        }

        @WorkerThread
        override fun close() {
            if (connection.isConnected) {
                // We can't block the main thread with this IPC
                closeScope.launch { withDocument { it.releasePage(pageNumber) } }
            }

            // TODO(b/397324529): Enqueue releasePage requests and execute when connection is
            //  re-established
        }
    }

    private suspend fun <T> withDocument(block: (PdfDocumentRemote) -> T): T {
        var trial = 1
        while (true) {
            try {
                return withDocumentWithoutRetry(block)
            } catch (e: Exception) {
                // We retry a max of 3 times if it is one of retry-able exceptions.
                if (
                    trial > MAX_RETRIES || ((e !is DeadObjectException) && (e !is TimeoutException))
                ) {
                    throw e
                }
                trial++
                // We sleep for some duration to give the service some time to recover.
                // The loop then retries it.
                delay(MIN_RETRY_DURATION * trial)
            }
        }
    }

    private suspend fun <T> withDocumentWithoutRetry(block: (PdfDocumentRemote) -> T): T {
        // If document is already closed, cancel all the pending operations on this document
        if (isDocumentClosedExplicitly) throw DocumentClosedException()

        // Create a new job in parent's context. Since with document can be called from any scope,
        // we need a handle to check coroutines actively working with document. Linking to parent's
        // job helps in cancellation.
        val taskJob =
            Job(parent = coroutineContext[Job]).also { job ->
                connection.pendingJobs.add(job)

                // clean up on completion
                job.invokeOnCompletion { connection.pendingJobs.remove(job) }
            }

        return withContext(coroutineContext + taskJob) {
            // Binder object will be null if the service is disconnected. Let's try reconnecting
            // explicitly
            if (connection.documentBinder == null) {
                connection.connect(uri)
            }

            // The documentBinder may be null if the service disconnects immediately after a
            // reconnection attempt. This is a rare, but recoverable, condition that subsequent
            // retries(triggered from `withDocument()`) should resolve.
            val binder =
                connection.documentBinder
                    ?: throw DeadObjectException("connection.documentBinder is still null")

            if (connection.needsToReopenDocument) {
                try {
                    binder.openPdfDocument(fileDescriptor, password)
                } catch (e: Exception) {
                    // Since `connection.connect(uri)` is suspending in nature, a explicit
                    // document.close() could be triggered independently while current block is
                    // waiting to be resumed.
                    // Ensure cancelling any work on this document, if it's closed.
                    throw if (isDocumentClosedExplicitly) DocumentClosedException(cause = e) else e
                }

                connection.needsToReopenDocument = false
            }

            val result = block(binder)

            // Manually completing taskJob because a Job created using Job() does not complete on
            // its own. Unlike coroutines launched with launch or async, a standalone Job() remains
            // active indefinitely unless explicitly completed or canceled.
            taskJob.complete()

            return@withContext result
        }
    }

    private companion object {
        private const val DEFAULT_PAGE = 400
        // Max number of retries to make on exceptions like DeadObjectExceptions on service.
        private const val MAX_RETRIES = 3
        // Min retry duration in milliseconds to start with.
        private const val MIN_RETRY_DURATION = 400L

        /**
         * Converts a list of items into a SparseArray, using the item's index as the key.
         *
         * @return A [SparseArray] containing the items from the list.
         * @throws IllegalArgumentException if the size of the list does not match the specified
         *   range.
         */
        private fun <T> List<T>.toSparseArray(range: IntRange): SparseArray<T> {
            require(this.size == (range.last - range.first + 1))

            val sparseArray = SparseArray<T>()
            val iterator = this.iterator()
            for (index: Int in range) {
                val item = iterator.next()
                sparseArray.put(index, item)
            }
            return sparseArray
        }
    }
}
