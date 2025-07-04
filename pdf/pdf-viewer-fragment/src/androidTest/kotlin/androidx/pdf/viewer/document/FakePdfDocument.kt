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

package androidx.pdf.viewer.document

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Size
import android.util.SparseArray
import androidx.annotation.OpenForTesting
import androidx.annotation.RequiresExtension
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import kotlin.random.Random

/**
 * Fake implementation of [PdfDocument], for testing.
 *
 * Provides an implementation of [getPageInfo] and [getPageInfos] that produces the dimensions
 * provided as [pages]. Provides an implementation of [getPageBitmapSource] that produces a random
 * solid RGB color bitmap for each page in [pages]. All other methods are fulfilled with no-op
 * implementations that return empty values.
 *
 * @param pages a list of [Point] defining the number of pages in the fake PDF and their dimensions
 * @param formType one of [PDF_FORM_TYPE_ACRO_FORM], [PDF_FORM_TYPE_XFA_FULL],
 *   [PDF_FORM_TYPE_XFA_FOREGROUND], or [PDF_FORM_TYPE_NONE] depending on the type of PDF form this
 *   fake PDF should represent
 * @param isLinearized true if this fake PDF is linearized
 */
@OpenForTesting
internal open class FakePdfDocument(
    /** A list of (x, y) page dimensions in content coordinates */
    private val pages: List<Point> = listOf(),
    override val formType: Int = PDF_FORM_TYPE_NONE,
    override val isLinearized: Boolean = false,
    private val searchResults: SparseArray<List<PageMatchBounds>> = SparseArray(),
    override val uri: Uri = Uri.parse("content://test.app/document.pdf"),
    private val pageLinks: List<PdfDocument.PdfPageLinks> = emptyList(),
) : PdfDocument {
    override val pageCount: Int = pages.size

    override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
        return FakeBitmapSource(pageNumber)
    }

    override suspend fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo> {
        return listOf()
    }

    override suspend fun getFormWidgetInfos(pageNum: Int, types: IntArray): List<FormWidgetInfo> {
        return listOf()
    }

    override suspend fun applyEdit(pageNum: Int, record: FormEditRecord): List<Rect> {
        return listOf()
    }

    override suspend fun write(destination: ParcelFileDescriptor) {
        return
    }

    override suspend fun getPageLinks(pageNumber: Int): PdfDocument.PdfPageLinks {
        return if (pageNumber < pageLinks.size) {
            pageLinks[pageNumber]
        } else {
            PdfDocument.PdfPageLinks(emptyList(), emptyList())
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getPageContent(pageNumber: Int): PdfDocument.PdfPageContent {
        // TODO(b/376136746) provide a useful implementation when it's needed for testing
        return PdfDocument.PdfPageContent(listOf(), listOf())
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
    override suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection {
        // TODO(b/376136631) provide a useful implementation when it's needed for testing
        return PageSelection(0, SelectionBoundary(0), SelectionBoundary(0), listOf())
    }

    override suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection? {
        return PageSelection(0, SelectionBoundary(0), SelectionBoundary(Int.MAX_VALUE), listOf())
    }

    override suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>> {
        return searchResults
    }

    override suspend fun getPageInfos(pageRange: IntRange): List<PdfDocument.PageInfo> {
        return pageRange.map { getPageInfo(it) }
    }

    override suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): List<PdfDocument.PageInfo> {
        return listOf()
    }

    override suspend fun getPageInfo(pageNumber: Int): PdfDocument.PageInfo {
        return getPageInfo(pageNumber, PdfDocument.PageInfoFlags.of(0))
    }

    override suspend fun getPageInfo(
        pageNumber: Int,
        pageInfoFlags: PdfDocument.PageInfoFlags,
    ): PdfDocument.PageInfo {
        val size = pages[pageNumber]
        return PdfDocument.PageInfo(pageNumber, size.y, size.x)
    }

    override fun close() {
        // No-op, fake
    }

    /**
     * A fake [PdfDocument.BitmapSource] that produces random RGB [Bitmap]s of the requested size
     */
    private inner class FakeBitmapSource(override val pageNumber: Int) : PdfDocument.BitmapSource {

        override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
            // Generate a solid random RGB bitmap at the requested size
            val size =
                if (tileRegion != null) Size(tileRegion.width(), tileRegion.height())
                else scaledPageSizePx
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bitmap.apply {
                val colorRng = Random(System.currentTimeMillis())
                eraseColor(
                    Color.argb(
                        255,
                        colorRng.nextInt(256),
                        colorRng.nextInt(256),
                        colorRng.nextInt(256),
                    )
                )
            }
            return bitmap
        }

        override fun close() {
            /* No-op, fake */
        }
    }
}

/** Represents the size and scale of a [Bitmap] requested from [PdfDocument.BitmapSource] */
internal sealed class SizeParams(val scaledPageSizePx: Size)

// Duplicated from PdfRenderer to avoid a hard dependency on SDK 35
/** Represents a PDF with no form fields */
internal const val PDF_FORM_TYPE_NONE = 0

/** Represents a PDF with form fields specified using the AcroForm spec */
internal const val PDF_FORM_TYPE_ACRO_FORM = 1

/** Represents a PDF with form fields specified using the entire XFA spec */
internal const val PDF_FORM_TYPE_XFA_FULL = 2

/** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
internal const val PDF_FORM_TYPE_XFA_FOREGROUND = 3
