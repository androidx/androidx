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

package androidx.pdf.service

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.FormWidgetInfo
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.pdf.DraftEditOperation
import androidx.pdf.DraftEditResult
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.PdfLoadingStatus
import androidx.pdf.RenderParams
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.adapter.PdfDocumentRendererFactory
import androidx.pdf.adapter.PdfDocumentRendererFactoryImpl
import androidx.pdf.annotation.PageAnnotationsProviderImpl
import androidx.pdf.annotation.models.PaginatedAnnotations
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.annotation.processor.PageAnnotationsPaginator
import androidx.pdf.annotation.processor.PdfRendererAnnotationsProcessor
import androidx.pdf.models.Dimensions
import androidx.pdf.utils.toPdfObject

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfDocumentRemoteImpl(
    private val adapterFactory: PdfDocumentRendererFactory = PdfDocumentRendererFactoryImpl()
) : PdfDocumentRemote.Stub() {

    private lateinit var rendererAdapter: PdfDocumentRenderer
    private lateinit var rendererAnnotationsProcessor: PdfRendererAnnotationsProcessor
    private var pageAnnotationsPaginator: PageAnnotationsPaginator? = null

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun openPdfDocument(pfd: ParcelFileDescriptor, password: String?): Int {
        try {
            rendererAdapter = adapterFactory.create(pfd, password)
            rendererAnnotationsProcessor = PdfRendererAnnotationsProcessor(rendererAdapter)
            return PdfLoadingStatus.SUCCESS.ordinal
        } catch (exception: SecurityException) {
            return PdfLoadingStatus.WRONG_PASSWORD.ordinal
        } catch (exception: IllegalArgumentException) {
            return PdfLoadingStatus.PDF_ERROR.ordinal
        } catch (exception: Exception) {
            return PdfLoadingStatus.UNKNOWN.ordinal
        }
    }

    override fun numPages(): Int {
        return rendererAdapter.pageCount
    }

    override fun getPageDimensions(pageNum: Int): Dimensions? {
        return rendererAdapter.withPage(pageNum) { page -> Dimensions(page.width, page.height) }
    }

    override fun getPageBitmap(
        pageNum: Int,
        width: Int,
        height: Int,
        renderParams: RenderParams,
    ): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Create a bitmap with a white background. PdfRenderer doesn't
        // guarantee a specific background color by default.
        output.eraseColor(Color.WHITE)
        // TODO (b/464133165): Update renderPage to use renderParams
        rendererAdapter.openPage(pageNum, useCache = true).renderPage(output, renderParams)
        return output
    }

    override fun getTileBitmap(
        pageNum: Int,
        tileWidth: Int,
        tileHeight: Int,
        pageWidth: Int,
        pageHeight: Int,
        offsetX: Int,
        offsetY: Int,
        renderParams: RenderParams,
    ): Bitmap {
        val output = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888)
        // Create a bitmap with a white background. PdfRenderer doesn't
        // guarantee a specific background color by default.
        output.eraseColor(Color.WHITE)

        // Latency optimization: Keep pages open to avoid re-initializing native objects
        // for subsequent rendering calls within the same user-visible portion.
        // TODO (b/464133165): Update renderTile to use renderParams
        rendererAdapter
            .openPage(pageNum, useCache = true)
            .renderTile(output, offsetX, offsetY, pageWidth, pageHeight, renderParams)
        return output
    }

    override fun getPageText(pageNum: Int): List<PdfPageTextContent>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getPageTextContents() }
    }

    override fun searchPageText(pageNum: Int, query: String): List<PageMatchBounds>? {
        return rendererAdapter.withPage(pageNum) { page -> page.searchPageText(query) }
    }

    override fun selectPageText(
        pageNum: Int,
        start: SelectionBoundary,
        stop: SelectionBoundary,
    ): PageSelection? {
        return rendererAdapter.withPage(pageNum) { page -> page.selectPageText(start, stop) }
    }

    override fun getPageExternalLinks(pageNum: Int): List<PdfPageLinkContent>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getPageLinks() }
    }

    override fun getPageGotoLinks(pageNum: Int): List<PdfPageGotoLinkContent>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getPageGotoLinks() }
    }

    override fun getPageImageContent(pageNum: Int): List<PdfPageImageContent>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getPageImageContents() }
    }

    override fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getFormWidgetInfos() }
    }

    override fun getFormWidgetInfosOfType(pageNum: Int, types: IntArray): List<FormWidgetInfo>? {
        return rendererAdapter.withPage(pageNum) { page -> page.getFormWidgetInfos(types) }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 19)
    override fun getTopPageObjectAtPosition(
        pageNum: Int,
        point: PointF,
        types: IntArray,
    ): PdfObject? {
        return rendererAdapter.withPage(pageNum) { page ->
            val topObjectResult = page.getTopPageObjectAtPosition(point, types)
            topObjectResult?.let {
                val convertedObject: PdfObject? = topObjectResult.second.toPdfObject()
                return@withPage convertedObject
            }
        }
    }

    override fun applyEdit(pageNum: Int, editRecord: FormEditRecord): List<Rect>? {
        return rendererAdapter.withPage(pageNum) { page -> page.applyEdit(editRecord) }
    }

    override fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean) {
        try {
            rendererAdapter.write(destination, removePasswordProtection)
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun getPageAnnotations(pageNum: Int): PaginatedAnnotations? {
        if (pageAnnotationsPaginator == null || pageAnnotationsPaginator!!.pageNum != pageNum) {
            pageAnnotationsPaginator =
                PageAnnotationsPaginator(
                    pageNum,
                    annotationsProvider =
                        PageAnnotationsProviderImpl(documentRenderer = rendererAdapter),
                )
        }

        return pageAnnotationsPaginator!!.getPageAnnotations()
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun getBatchedPageAnnotations(pageNum: Int, batchIndex: Int): PaginatedAnnotations? {
        if (pageAnnotationsPaginator == null || pageAnnotationsPaginator!!.pageNum != pageNum) {
            pageAnnotationsPaginator =
                PageAnnotationsPaginator(
                    pageNum,
                    annotationsProvider =
                        PageAnnotationsProviderImpl(documentRenderer = rendererAdapter),
                )
        }

        return pageAnnotationsPaginator!!.getPageAnnotations(batchIndex)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun applyDraftEdits(operations: List<DraftEditOperation>): DraftEditResult {
        return rendererAnnotationsProcessor.process(operations)
    }

    override fun getLinearizationStatus(): Int {
        return rendererAdapter.linearizationStatus
    }

    override fun getFormType(): Int {
        return rendererAdapter.formType
    }

    override fun releasePage(pageNum: Int) {
        rendererAdapter.releasePage(null, pageNum)
    }

    override fun closePdfDocument() {
        rendererAdapter.close()
    }

    override fun getInterfaceVersion(): Int = VERSION
}
