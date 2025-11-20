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
import android.graphics.Rect
import android.graphics.pdf.component.PdfAnnotation as AospPdfAnnotation
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
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.PdfLoadingStatus
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.adapter.PdfDocumentRendererFactory
import androidx.pdf.adapter.PdfDocumentRendererFactoryImpl
import androidx.pdf.annotation.PageAnnotationsProviderImpl
import androidx.pdf.annotation.converters.PdfAnnotationConvertersFactory
import androidx.pdf.annotation.models.AddEditResult
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.EditId
import androidx.pdf.annotation.models.ModifyEditResult
import androidx.pdf.annotation.models.PaginatedAnnotations
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.annotation.models.PdfAnnotationData
import androidx.pdf.annotation.processor.PageAnnotationsPaginator
import androidx.pdf.annotation.processor.PdfRendererAnnotationsProcessor
import androidx.pdf.models.Dimensions
import androidx.pdf.utils.readAnnotationsFromPfd

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfDocumentRemoteImpl(
    private val adapterFactory: PdfDocumentRendererFactory = PdfDocumentRendererFactoryImpl()
) : PdfDocumentRemote.Stub() {

    private lateinit var rendererAdapter: PdfDocumentRenderer
    private lateinit var annotationsProcessor: PdfRendererAnnotationsProcessor
    private var pageAnnotationsPaginator: PageAnnotationsPaginator? = null

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun openPdfDocument(pfd: ParcelFileDescriptor, password: String?): Int {
        try {
            rendererAdapter = adapterFactory.create(pfd, password)
            annotationsProcessor = PdfRendererAnnotationsProcessor(rendererAdapter)
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

    override fun getPageBitmap(pageNum: Int, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Create a bitmap with a white background. PdfRenderer doesn't
        // guarantee a specific background color by default.
        output.eraseColor(Color.WHITE)
        rendererAdapter.openPage(pageNum, useCache = true).renderPage(output)
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
    ): Bitmap {
        val output = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888)
        // Create a bitmap with a white background. PdfRenderer doesn't
        // guarantee a specific background color by default.
        output.eraseColor(Color.WHITE)

        // Latency optimization: Keep pages open to avoid re-initializing native objects
        // for subsequent rendering calls within the same user-visible portion.
        rendererAdapter
            .openPage(pageNum, useCache = true)
            .renderTile(output, offsetX, offsetY, pageWidth, pageHeight)
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
    override fun addAnnotations(pfd: ParcelFileDescriptor): AnnotationResult {
        val pdfAnnotationsData = readAnnotationsFromPfd(pfd)
        val (success, failures) = pdfAnnotationsData.partition { addPdfAnnotationToAosp(it) }
        return AnnotationResult(success, failures.map { it.annotation })
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    private fun addPdfAnnotationToAosp(pdfAnnotationData: PdfAnnotationData): Boolean {
        val annotation = pdfAnnotationData.annotation
        return rendererAdapter.withPage(annotation.pageNum) { page ->
            val converter = PdfAnnotationConvertersFactory.create<PdfAnnotation>(annotation)
            val aospAnnotation = converter.convert(annotation)
            try {
                page.addPageAnnotation(aospAnnotation)
                true
            } catch (e: IllegalStateException) {
                false
            }
        } == true
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun getPageAnnotations(pageNum: Int): List<PdfAnnotation>? {
        return rendererAdapter.withPage(pageNum) { page ->
            val aospAnnotations = page.getPageAnnotations()
            val pdfAnnotations = mutableListOf<PdfAnnotation>()
            for (aospAnnotation in aospAnnotations) {
                val unused = aospAnnotation.first // <-- AOSP ID
                try {
                    val converter =
                        PdfAnnotationConvertersFactory.create<AospPdfAnnotation>(
                            aospAnnotation.second
                        )
                    converter.convert(aospAnnotation.second, pageNum).let { pfdAnnotation ->
                        pdfAnnotations.add(pfdAnnotation)
                    }
                } catch (e: UnsupportedOperationException) {
                    // TODO: b/440966572 - Handle Unsupported Annotation like FreeTextAnnotation
                }
            }
            pdfAnnotations
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun applyEdits(annots: List<PdfAnnotationData>): AnnotationResult =
        annotationsProcessor.process(annots)

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun addEdit(annots: List<PdfAnnotationData>): AddEditResult {
        return annotationsProcessor.processAddEdits(annots)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun updateEdit(annots: List<PdfAnnotationData>): ModifyEditResult {
        return annotationsProcessor.processUpdateEdits(annots)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun removeEdit(editIds: List<EditId>): ModifyEditResult {
        return annotationsProcessor.processRemoveEdits(editIds)
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
    override fun getAllPageAnnotations(pageNum: Int): PaginatedAnnotations? {
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

    override fun isPdfLinearized(): Boolean {
        return rendererAdapter.isLinearized
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
}
