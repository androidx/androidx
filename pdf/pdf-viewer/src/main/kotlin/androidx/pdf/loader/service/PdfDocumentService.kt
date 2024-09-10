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

package androidx.pdf.loader.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRendererPreV
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.annotation.NonNull
import androidx.annotation.RestrictTo
import androidx.pdf.data.PdfLoadingStatus
import androidx.pdf.models.Dimensions
import androidx.pdf.models.PdfDocumentProvider
import androidx.pdf.service.PdfPageWrapper
import androidx.pdf.service.PdfRendererWrapper
import androidx.pdf.service.PdfRendererWrapperFactory
import java.io.IOException

/** Service providing access to the PdfRenderer API. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
internal class PdfDocumentService : Service() {

    @NonNull
    override fun onBind(intent: Intent): IBinder {
        return PdfDocumentRemoteImpl()
    }

    private class PdfDocumentRemoteImpl : PdfDocumentProvider.Stub() {
        private var pdfRendererWrapper: PdfRendererWrapper? = null

        override fun openPdfDocument(pfd: ParcelFileDescriptor, password: String?): Int {
            return try {
                pdfRendererWrapper = PdfRendererWrapperFactory.create(pfd, password)
                PdfLoadingStatus.SUCCESS.ordinal
            } catch (e: SecurityException) {
                PdfLoadingStatus.WRONG_PASSWORD.ordinal
            } catch (e: IOException) {
                PdfLoadingStatus.PDF_ERROR.ordinal
            } catch (e: IllegalArgumentException) {
                PdfLoadingStatus.PDF_ERROR.ordinal
            } catch (e: Exception) {
                PdfLoadingStatus.LOADING_ERROR.ordinal
            }
        }

        override fun numPages(): Int {
            return getPageWrapper().pageCount
        }

        override fun getPageBitmap(pageNum: Int, width: Int, height: Int): Bitmap {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // Create a bitmap with a white background. PdfRenderer doesn't
            // guarantee a specific background color by default.
            output.eraseColor(Color.WHITE)

            // Latency optimization: Keep pages open to avoid re-initializing native objects
            // for subsequent rendering calls within the same user-visible portion.
            getPageWrapper().openPage(pageNum, useCache = true).renderPage(output)
            return output
        }

        override fun getTileBitmap(
            pageNum: Int,
            tileWidth: Int,
            tileHeight: Int,
            pageWidth: Int,
            pageHeight: Int,
            offsetX: Int,
            offsetY: Int
        ): Bitmap {
            val output = Bitmap.createBitmap(tileWidth, tileHeight, Bitmap.Config.ARGB_8888)
            // Create a bitmap with a white background. PdfRenderer doesn't
            // guarantee a specific background color by default.
            output.eraseColor(Color.WHITE)

            // Latency optimization: Keep pages open to avoid re-initializing native objects
            // for subsequent rendering calls within the same user-visible portion.
            getPageWrapper()
                .openPage(pageNum, useCache = true)
                .renderTile(output, offsetX, offsetY, pageWidth, pageHeight)
            return output
        }

        override fun isPdfLinearized(): Boolean {
            return getPageWrapper().documentLinearizationType ==
                PdfRendererPreV.DOCUMENT_LINEARIZED_TYPE_LINEARIZED
        }

        override fun getFormType(): Int {
            return getPageWrapper().documentFormType
        }

        override fun getPageDimensions(pageNum: Int): Dimensions {
            return executeOnPage(pageNum) { Dimensions(it.width, it.height) }
        }

        @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
        override fun getPageText(pageNum: Int): List<PdfPageTextContent> {
            return executeOnPage(pageNum) { it.getPageTextContents() }
        }

        override fun searchPageText(pageNum: Int, query: String): List<PageMatchBounds> {
            return executeOnPage(pageNum) { it.searchPageText(query) }
        }

        override fun selectPageText(
            pageNum: Int,
            start: SelectionBoundary,
            stop: SelectionBoundary
        ): PageSelection? {
            return executeOnPage(pageNum) { it.selectPageText(start, stop) }
        }

        @SuppressLint("ObsoleteSdkInt") // TODO: Remove after sdk extension 13 release
        override fun getPageExternalLinks(pageNum: Int): List<PdfPageLinkContent> {
            return executeOnPage(pageNum) { it.getPageLinks() }
        }

        override fun getPageGotoLinks(pageNum: Int): List<PdfPageGotoLinkContent> {
            return executeOnPage(pageNum) { it.getPageGotoLinks() }
        }

        override fun getPageImageContent(pageNum: Int): List<PdfPageImageContent> {
            return executeOnPage(pageNum) { it.getPageImageContents() }
        }

        private fun getPageWrapper(): PdfRendererWrapper {
            return pdfRendererWrapper
                ?: throw IllegalStateException("Not initialized, call openPdfDocument first")
        }

        private fun <T> executeOnPage(pageNum: Int, block: (PdfPageWrapper) -> T): T {
            val pageWrapper = getPageWrapper()
            var pdfPage: PdfPageWrapper? = null
            return try {
                pdfPage = pageWrapper.openPage(pageNum, useCache = false)
                block(pdfPage)
            } finally {
                pageWrapper.releasePage(pdfPage, pageNum)
            }
        }

        override fun releasePage(pageNum: Int) {
            getPageWrapper().releasePage(null, pageNum)
        }

        override fun closePdfDocument() {
            pdfRendererWrapper?.close()
            pdfRendererWrapper = null
        }
    }
}
