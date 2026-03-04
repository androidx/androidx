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
import android.graphics.pdf.PdfRenderer
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
import androidx.pdf.PdfDocument.DocumentClosedException
import androidx.pdf.PdfDocument.PdfPageContent
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.processor.BatchPdfAnnotationsProcessor
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.service.PdfDocumentServiceImpl
import androidx.pdf.service.connect.PdfServiceConnection
import androidx.pdf.utils.toAndroidClass
import androidx.pdf.utils.toContentClass
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
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
 * @param linearizationStatus Indicates the linearization status of the document.
 * @param formType The type of form present in the document.
 * @param isLinearized Indicates whether the document is linearized.
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
    override val linearizationStatus: Int,
    override val formType: Int,
    override val renderParams: RenderParams,
    @Deprecated(
        "Deprecated, Use linearizationStatus instead",
        replaceWith = ReplaceWith("linearizationStatus"),
    )
    override val isLinearized: Boolean,
) : EditablePdfDocument() {

    private val refCount = AtomicInteger(1)

    /** The [CoroutineScope] we use to close [BitmapSource]s asynchronously */
    private val closeScope = CoroutineScope(coroutineContext + SupervisorJob())

    private val onPdfContentInvalidatedListeners:
        CopyOnWriteArrayList<PdfContentInvalidationEntry> =
        CopyOnWriteArrayList()

    private val onEditsAppliedListenerEntries: MutableList<OnEditsAppliedListenerEntry> =
        Collections.synchronizedList(mutableListOf())

    private val batchPdfAnnotationsProcessor =
        BatchPdfAnnotationsProcessor(requireNotNull(connection.documentBinder))

    /**
     * Indicates whether this [androidx.pdf.SandboxedPdfDocument] is closed explicitly by calling
     * [close].
     *
     * Once closed, any further operations on the document are invalid.
     */
    private var isDocumentClosedExplicitly = false

    @Suppress("WrongConstant")
    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return getPageInfo(pageNumber, PdfDocument.PAGE_INFO_EXCLUDE_FORM_WIDGETS)
    }

    override suspend fun getPageInfo(pageNumber: Int, pageInfoFlags: Long): PdfDocument.PageInfo {
        return withDocument { document ->
            // TODO(b/407777410): Update the logic so that callers can refetch the information in
            // case
            //  default value is returned
            val dimensions = document.getPageDimensions(pageNumber)

            // Check if the INCLUDE_FORM_WIDGET_INFO flag is set
            val formWidgetInfo =
                if (pageInfoFlags and PdfDocument.PAGE_INFO_INCLUDE_FORM_WIDGET != 0L) {
                    document.getFormWidgetInfos(pageNumber).map { it.toContentClass() }
                } else {
                    emptyList()
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
        pageInfoFlags: Long,
    ): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(pageNumber = it, pageInfoFlags = pageInfoFlags) }
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> = coroutineScope {
        return@coroutineScope withDocument { document ->
            SparseArray<List<PageMatchBounds>>(pageRange.last + 1).apply {
                pageRange.forEach { pageNum ->
                    // Check for cancellation at the start of new page search
                    ensureActive()
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

    override suspend fun getFormWidgetInfos(pageNum: Int, types: Long): List<FormWidgetInfo> {
        return withDocument { document ->
            document.getFormWidgetInfosOfType(pageNum, getFormWidgetTypesArray(types)).map {
                it.toContentClass()
            }
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    override suspend fun getTopPageObjectAtPosition(pageNum: Int, point: PointF): PdfObject? {
        return withDocument { document ->
            document.getTopPageObjectAtPosition(pageNum, point, intArrayOf())
        }
    }

    override fun addOnPdfContentInvalidatedListener(
        executor: Executor,
        listener: PdfDocument.OnPdfContentInvalidatedListener,
    ) {
        onPdfContentInvalidatedListeners.add(PdfContentInvalidationEntry(executor, listener))
    }

    override fun removeOnPdfContentInvalidatedListener(
        listener: PdfDocument.OnPdfContentInvalidatedListener
    ) {
        for (pdfContentInvalidationEntry in onPdfContentInvalidatedListeners) {
            if (pdfContentInvalidationEntry.listener == listener) {
                onPdfContentInvalidatedListeners.remove(pdfContentInvalidationEntry)
                break
            }
        }
    }

    override suspend fun applyEdit(record: FormEditInfo) {
        val dirtyAreas = withDocument { document ->
            document.applyEdit(record.pageNumber, record.toAndroidClass())
        }
        onPdfContentInvalidatedListeners.forEach { (executor, listener) ->
            executor.execute { listener.onPdfContentInvalidated(record.pageNumber, dirtyAreas) }
        }
    }

    override suspend fun applyEdits(editsDraft: EditsDraft): List<String> {
        return batchPdfAnnotationsProcessor.process(editsDraft) { appliedBatchEdits ->
            appliedBatchEdits.forEach { appliedEdit ->
                onEditsAppliedListenerEntries.forEach { entry ->
                    entry.executor.execute {
                        entry.listener.onEditApplied(appliedEdit.pageNum, appliedEdit.editId)
                    }
                }
            }
        }
    }

    /**
     * Generates a handle for writing the document. This handle should be closed after use.
     *
     * @return A [PdfWriteHandle] for the document.
     */
    override fun createWriteHandle(): PdfWriteHandle {
        refCount.incrementAndGet()
        return PdfWriteHandleImpl(this)
    }

    override suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> =
        getKeyedAnnotationsForPage(pageNum)

    override fun addOnEditsAppliedListener(executor: Executor, listener: OnEditsAppliedListener) {
        onEditsAppliedListenerEntries.add(OnEditsAppliedListenerEntry(executor, listener))
    }

    override fun removeOnEditsAppliedListener(listener: OnEditsAppliedListener) {
        for (onEditsAppliedListener in onEditsAppliedListenerEntries) {
            if (onEditsAppliedListener.listener == listener) {
                onEditsAppliedListenerEntries.remove(onEditsAppliedListener)
                break
            }
        }
    }

    @WorkerThread
    override fun close() {
        if (refCount.decrementAndGet() > 0) return

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
         * @param renderParams The render params used to render contents on the bitmap.
         * @return The bitmap of the specified page or region.
         */
        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            return withDocument { document ->
                if (tileRegion == null) {
                    document.getPageBitmap(
                        pageNumber,
                        scaledPageSizePx.width,
                        scaledPageSizePx.height,
                        renderParams,
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
                        renderParams,
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

    internal suspend fun <T> withDocument(block: (PdfDocumentRemote) -> T): T {
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

    private suspend fun getKeyedAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation> {
        val firstBatch = withDocument { it.getPageAnnotations(pageNum) } ?: return emptyList()
        if (firstBatch.totalBatchCount <= 1) {
            return firstBatch.annotations
        }

        return coroutineScope {
            val firstAnnotations = firstBatch.annotations
            val deferredRemainingBatches =
                (1 until firstBatch.totalBatchCount).map { batchIndex ->
                    async {
                        withDocument { remote ->
                            remote.getBatchedPageAnnotations(pageNum, batchIndex).annotations
                        }
                    }
                }

            val remainingAnnotations = deferredRemainingBatches.awaitAll().flatten()
            firstAnnotations + remainingAnnotations
        }
    }

    private fun getFormWidgetTypesArray(types: Long): IntArray {
        if (types == PdfDocument.FORM_WIDGET_INCLUDE_ALL_TYPES) return intArrayOf()

        return buildList {
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_TEXTFIELD_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_TEXTFIELD)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_PUSHBUTTON_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_RADIOBUTTON_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_CHECKBOX_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_CHECKBOX)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_COMBOBOX_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_COMBOBOX)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_LISTBOX_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_LISTBOX)
                if (types and PdfDocument.FORM_WIDGET_INCLUDE_SIGNATURE_TYPE != 0L)
                    add(FormWidgetInfo.WIDGET_TYPE_SIGNATURE)
            }
            .toIntArray()
    }

    private data class PdfContentInvalidationEntry(
        val executor: Executor,
        val listener: PdfDocument.OnPdfContentInvalidatedListener,
    )

    private data class OnEditsAppliedListenerEntry(
        val executor: Executor,
        val listener: OnEditsAppliedListener,
    )

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
