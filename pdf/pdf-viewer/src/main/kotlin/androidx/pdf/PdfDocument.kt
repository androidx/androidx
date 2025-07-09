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
import android.graphics.PointF
import android.graphics.Rect
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Size
import android.util.SparseArray
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageImageContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import java.io.Closeable
import java.io.IOException
import kotlin.jvm.Throws
import kotlinx.coroutines.CancellationException

/** Represents a PDF document and provides methods to interact with its content. */
public interface PdfDocument : Closeable {

    /** The URI of the document represented by this object */
    public val uri: Uri

    /** The total number of pages in the document. */
    public val pageCount: Int

    /** Indicates whether the document is linearized (optimized for fast web viewing). */
    public val isLinearized: Boolean

    /** The type of form present in the document. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY) public val formType: Int

    /**
     * Asynchronously retrieves information about the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PageInfo] object containing information about the page.
     */
    public suspend fun getPageInfo(pageNumber: Int): PageInfo

    /**
     * Asynchronously retrieves information about the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @param pageInfoFlags The flags for retrieving additional page information.
     * @return A [PageInfo] object containing information about the page.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getPageInfo(
        pageNumber: Int,
        pageInfoFlags: PageInfoFlags = PageInfoFlags.of(0),
    ): PageInfo

    /**
     * Asynchronously retrieves information about a range of pages.
     *
     * @param pageRange The range of page numbers (0-based, inclusive).
     * @return A list of [PageInfo] objects, one for each page in the range.
     */
    public suspend fun getPageInfos(pageRange: IntRange): List<PageInfo>

    /**
     * Asynchronously retrieves information about a range of pages.
     *
     * @param pageRange The range of page numbers (0-based, inclusive).
     * @param pageInfoFlags The flags for retrieving additional page information.
     * @return A list of [PageInfo] objects, one for each page in the range.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getPageInfos(
        pageRange: IntRange,
        pageInfoFlags: PageInfoFlags,
    ): List<PageInfo>

    /**
     * Asynchronously searches the document for the specified query within a range of pages.
     *
     * @param query The search query string.
     * @param pageRange The range of page numbers (0-based, inclusive) to search within.
     * @return A [SparseArray] mapping page numbers to lists of [PageMatchBounds] objects
     *   representing the search results on each page.
     */
    public suspend fun searchDocument(
        query: String,
        pageRange: IntRange,
    ): SparseArray<List<PageMatchBounds>>

    /**
     * Asynchronously retrieves the selection bounds (in PDF coordinates) for the specified text
     * selection.
     *
     * @param pageNumber The page on which text to be selected.
     * @param start The starting point of the text selection.
     * @param stop The ending point of the text selection.
     * @return A [PageSelection] object representing the selection bounds on the page.
     */
    public suspend fun getSelectionBounds(
        pageNumber: Int,
        start: PointF,
        stop: PointF,
    ): PageSelection?

    /**
     * Asynchronously retrieves the selection bounds (in PDF coordinates) for the complete text on
     * the page.
     *
     * @param pageNumber The page on which text to be selected.
     * @return A [PageSelection] object representing the selection bounds on the page.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getSelectAllSelectionBounds(pageNumber: Int): PageSelection?

    /**
     * Asynchronously retrieves the content (text and images) of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PdfPageContent] object representing the page's content.
     */
    public suspend fun getPageContent(pageNumber: Int): PdfPageContent?

    /**
     * Asynchronously retrieves the links (Go To and external) present on the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PdfPageLinks] object representing the page's links.
     */
    public suspend fun getPageLinks(pageNumber: Int): PdfPageLinks

    /**
     * Gets a [BitmapSource] for retrieving bitmap representations of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [BitmapSource] for the specified page, or null if the page number is invalid.
     */
    public fun getPageBitmapSource(pageNumber: Int): BitmapSource

    /**
     * Returns the list of [FormWidgetInfo] on [pageNum]
     *
     * @property pageNum The page number (0-based).
     * @return A list of [FormWidgetInfo] objects representing the form widgets on the given page.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getFormWidgetInfos(pageNum: Int): List<FormWidgetInfo>

    /**
     * Returns the list of [FormWidgetInfo] on [pageNum], optionally filtered by widget type.
     *
     * @property pageNum The page number (0-based).
     * @property types The [FormWidgetInfo.WidgetType] of form widgets to return, or an empty array
     *   to return all widgets.
     * @return A list of [FormWidgetInfo] objects representing the form widgets of the specified
     *   types on the specified page.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getFormWidgetInfos(
        pageNum: Int,
        types: IntArray = intArrayOf(),
    ): List<FormWidgetInfo>

    /**
     * Applies the changes specified by [record] to the form, and returns a list of [Rect]
     * indicating regions of the PDF content that were affected by the mutation. It reflects the
     * regions of the PDF which need to be re-rendered to reflect the changes.
     *
     * It is recommended that UI classes maintain a list of [FormEditRecord] they've applied to the
     * document so they can be saved and restored across destructive events like low memory kills or
     * configuration changes.
     *
     * @property pageNum The page number (0-based).
     * @property record The [FormEditRecord] to apply to the form.
     * @return A list of [Rect] indicating regions of the PDF content that were affected by the
     *   mutation.
     * @throws IllegalArgumentException if the provided [record] cannot be applied to the widget
     *   indicated by the index, or if the index does not correspond to a widget on the page.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun applyEdit(pageNum: Int, record: FormEditRecord): List<Rect>

    /**
     * Writes the contents of this [PdfDocument] to [destination] and closes the
     * [ParcelFileDescriptor]
     *
     * @property destination The [ParcelFileDescriptor] to write to.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Throws(IOException::class)
    public suspend fun write(destination: ParcelFileDescriptor)

    /**
     * Represents information about a single page in the PDF document.
     *
     * @property pageNum The page number (0-based).
     * @property height The height of the page in points.
     * @property width The width of the page in points.
     */
    public class PageInfo
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public constructor(
        public val pageNum: Int,
        public val height: Int,
        public val width: Int,
        @get:RestrictTo(RestrictTo.Scope.LIBRARY)
        public val formWidgetInfos: List<FormWidgetInfo>? = null,
    ) {
        public constructor(
            pageNum: Int,
            height: Int,
            width: Int,
        ) : this(pageNum, height, width, formWidgetInfos = null)
    }

    /** A source for retrieving bitmap representations of PDF pages. */
    public interface BitmapSource : Closeable {
        public val pageNumber: Int

        /**
         * Asynchronously retrieves a bitmap representation of the page, optionally constrained to a
         * specific tile region.
         *
         * @param scaledPageSizePx The scaled page size in pixels, representing the page size in
         *   case of no zoom, and scaled page size in case of zooming.
         * @param tileRegion (Optional) The region of the page to include in the bitmap, in pixels
         *   within the `scaledPageSizePx`. This identifies the tile. If null, the entire page is
         *   included.
         * @return The bitmap representation of the page.
         */
        public suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect? = null): Bitmap
    }

    /**
     * Represents the combined text and image content within a single page of a PDF document.
     *
     * @property textContents A list of [PdfPageTextContent] objects representing the text elements
     *   on the page.
     * @property imageContents A list of ]PdfPageImageContent] objects representing the image
     *   elements on the page.
     */
    public class PdfPageContent(
        public val textContents: List<PdfPageTextContent>,
        public val imageContents: List<PdfPageImageContent>,
    )

    /**
     * Represents the links within a single page of a PDF document.
     *
     * @property gotoLinks A list of internal links (links to other pages within the same document)
     *   represented as [PdfPageGotoLinkContent] objects.
     * @property externalLinks A list of external links (links to web pages or other resources)
     *   represented as [PdfPageLinkContent] objects.
     */
    public class PdfPageLinks(
        public val gotoLinks: List<PdfPageGotoLinkContent>,
        public val externalLinks: List<PdfPageLinkContent>,
    )

    /**
     * A [CancellationException] indicating that a document has been closed.
     *
     * @property message: the detail message
     * @property cause: the cause of the exception, if available. This will be present if an
     *   exception occurred while executing operation that needs to be cancelled.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class DocumentClosedException(
        public override val message: String = "Document already closed",
        public override val cause: Throwable? = null,
    ) : CancellationException()

    /** Specifies the flags for loading pageInfo. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public class PageInfoFlags private constructor(public val value: Long) {
        public companion object {
            @JvmStatic public fun of(value: Long): PageInfoFlags = PageInfoFlags(value)
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        PDF_FORM_TYPE_NONE,
        PDF_FORM_TYPE_ACRO_FORM,
        PDF_FORM_TYPE_XFA_FULL,
        PDF_FORM_TYPE_XFA_FOREGROUND,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class FormType

    public companion object {
        /** Flag used with [getPageInfo] to include form widget metadata in the [PageInfo] */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public const val INCLUDE_FORM_WIDGET_INFO: Long = 1 shl 0

        /** Represents a PDF with no form fields */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val PDF_FORM_TYPE_NONE: Int = 0

        /** Represents a PDF with form fields specified using the AcroForm spec */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val PDF_FORM_TYPE_ACRO_FORM: Int = 1

        /** Represents a PDF with form fields specified using the entire XFA spec */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val PDF_FORM_TYPE_XFA_FULL: Int = 2

        /** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
        @RestrictTo(RestrictTo.Scope.LIBRARY) public const val PDF_FORM_TYPE_XFA_FOREGROUND: Int = 3
    }
}
