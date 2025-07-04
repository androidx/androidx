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
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.FormWidgetInfo
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.ParcelFileDescriptor
import androidx.annotation.RestrictTo
import androidx.pdf.PdfDocumentRemote
import androidx.pdf.PdfLoadingStatus
import androidx.pdf.adapter.PdfDocumentRenderer
import androidx.pdf.adapter.PdfDocumentRendererFactory
import androidx.pdf.adapter.PdfDocumentRendererFactoryImpl
import androidx.pdf.adapter.PdfPage
import androidx.pdf.annotation.models.AnnotationResult
import androidx.pdf.annotation.models.PdfAnnotation
import androidx.pdf.models.Dimensions

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfDocumentRemoteImpl(
    private val adapterFactory: PdfDocumentRendererFactory = PdfDocumentRendererFactoryImpl()
) : PdfDocumentRemote.Stub() {

    private lateinit var rendererAdapter: PdfDocumentRenderer

    override fun openPdfDocument(pfd: ParcelFileDescriptor, password: String?): Int {
        try {
            rendererAdapter = adapterFactory.create(pfd, password)
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
        return withPage(pageNum) { page -> Dimensions(page.width, page.height) }
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
        return withPage(pageNum) { page -> page.getPageTextContents() }
    }

    override fun searchPageText(pageNum: Int, query: String): List<PageMatchBounds>? {
        return withPage(pageNum) { page -> page.searchPageText(query) }
    }

    override fun selectPageText(
        pageNum: Int,
        start: SelectionBoundary,
        stop: SelectionBoundary,
    ): PageSelection? {
        return withPage(pageNum) { page -> page.selectPageText(start, stop) }
    }

    override fun getPageExternalLinks(pageNum: Int): List<PdfPageLinkContent>? {
        return withPage(pageNum) { page -> page.getPageLinks() }
    }

    override fun getPageGotoLinks(pageNum: Int): List<PdfPageGotoLinkContent>? {
        return withPage(pageNum) { page -> page.getPageGotoLinks() }
    }

    override fun getPageImageContent(pageNum: Int): List<PdfPageImageContent>? {
        return withPage(pageNum) { page -> page.getPageImageContents() }
    }

    override fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo>? {
        return withPage(pageNum) { page -> page.getFormWidgetInfos() }
    }

    override fun getFormWidgetInfosOfType(pageNum: Int, types: IntArray): List<FormWidgetInfo>? {
        return withPage(pageNum) { page -> page.getFormWidgetInfos(types) }
    }

    override fun applyEdit(pageNum: Int, editRecord: FormEditRecord): List<Rect>? {
        return withPage(pageNum) { page -> page.applyEdit(editRecord) }
    }

    override fun write(destination: ParcelFileDescriptor, removePasswordProtection: Boolean) {
        try {
            rendererAdapter.write(destination, removePasswordProtection)
        } catch (exception: Exception) {
            throw RuntimeException(exception)
        }
    }

    override fun addAnnotations(pfd: ParcelFileDescriptor): AnnotationResult? {
        // TODO: addAnnotations to be implemented in subsequent CL
        return AnnotationResult(listOf(), listOf())
    }

    override fun getPageAnnotations(pageNum: Int): List<PdfAnnotation> {
        // TODO:  getPageAnnotations to be implemented in subsequent CL
        return mutableListOf()
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

    private fun <T> withPage(pageNum: Int, block: (PdfPage) -> T): T? {
        var page: PdfPage? = null
        var results: T?

        try {
            page = rendererAdapter.openPage(pageNum, useCache = false)
            results = block(page)
        } finally {
            rendererAdapter.releasePage(page, pageNum)
        }

        return results
    }
}
