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
import android.util.Size
import android.util.SparseArray
import androidx.annotation.IntDef
import androidx.annotation.LongDef
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.KeyedPdfAnnotation
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageImageContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormWidgetInfo
import java.io.Closeable
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellationException

/** Represents a PDF document and provides methods to interact with its content. */
public interface PdfDocument : Closeable {

    /** The URI of the document represented by this object */
    public val uri: Uri

    /** The total number of pages in the document. */
    public val pageCount: Int

    /** Indicates whether the document is linearized (optimized for fast web viewing). */
    @Deprecated(
        "Deprecated, Use linearizationStatus instead",
        replaceWith = ReplaceWith("linearizationStatus"),
    )
    public val isLinearized: Boolean

    /** Indicates the linearization status of the document. */
    @get:LinearizationStatus public val linearizationStatus: Int

    /**
     * The render params used to determine the contents that will be rendered on the bitmap.
     *
     * @see RenderParams
     * @see BitmapSource.getBitmap
     */
    public val renderParams: RenderParams

    /**
     * The type of form present in the document.
     *
     * @see [FormType] for the supported types.
     */
    @FormType public val formType: Int

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
     * @param pageInfoFlags A bitmask for retrieving additional page information. Does not include
     *   any additional information by default.
     * @return A [PageInfo] object containing information about the page.
     */
    public suspend fun getPageInfo(
        pageNumber: Int,
        @PageInfoFlags pageInfoFlags: Long = PAGE_INFO_EXCLUDE_FORM_WIDGETS,
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
     * @param pageInfoFlags A bitmask for retrieving additional page information. Does not include
     *   any additional information by default.
     * @return A list of [PageInfo] objects, one for each page in the range.
     */
    public suspend fun getPageInfos(
        pageRange: IntRange,
        @PageInfoFlags pageInfoFlags: Long = PAGE_INFO_EXCLUDE_FORM_WIDGETS,
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
     * Retrieves a list of all annotations for the specified page.
     *
     * @param pageNum The page number (0-indexed) from which to retrieve edits.
     * @return A list of [KeyedPdfAnnotation] objects on the page. Returns an empty list if there
     *   are no edits on the page.
     * @throws IllegalArgumentException if the page number is invalid.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public suspend fun getAnnotationsForPage(pageNum: Int): List<KeyedPdfAnnotation>

    /**
     * Gets a [BitmapSource] for retrieving bitmap representations of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [BitmapSource] for the specified page, or null if the page number is invalid.
     */
    public fun getPageBitmapSource(pageNumber: Int): BitmapSource

    /**
     * Returns the list of [FormWidgetInfo] on [pageNum], optionally filtered by widget type.
     *
     * @param pageNum The page number (0-based).
     * @param types Bitmask to determine the types of form widgets to include in the result.
     *   Includes all types of form widgets by default.
     * @return A list of [FormWidgetInfo] objects representing the form widgets of the specified
     *   types on the specified page.
     */
    public suspend fun getFormWidgetInfos(
        pageNum: Int,
        @FormWidgetTypeFlags types: Long = FORM_WIDGET_INCLUDE_ALL_TYPES,
    ): List<FormWidgetInfo>

    /**
     * Returns the topmost page object at a specific point on the page.
     *
     * @param pageNum The page number (0-based).
     * @param point The point on the page to query.
     * @return The topmost [PdfObject] at the specified point or returns null if no page object is
     *   present.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public suspend fun getTopPageObjectAtPosition(pageNum: Int, point: PointF): PdfObject?

    /**
     * Listener interface for receiving notifications when some regions of the PDF content are
     * invalidated.
     */
    public interface OnPdfContentInvalidatedListener {
        /**
         * Invoked when some regions of the PDF content are invalidated, and need to be re-rendered.
         * (example scenario - when a form field is edited in the PDF.)
         *
         * @param pageNumber The page number (0-index based) on which the content was invalidated.
         * @param dirtyAreas A list of [Rect] indicating regions of the PDF content that were
         *   invalidated and need to be re-rendered in order to sync UI to the latest state of the
         *   document.
         */
        public fun onPdfContentInvalidated(pageNumber: Int, dirtyAreas: List<Rect>)
    }

    /**
     * Adds a listener to receive notifications when some regions of the PDF content are
     * invalidated.
     *
     * @param executor The executor on which the listener's methods will be called.
     * @param listener The listener to add.
     * @see [OnPdfContentInvalidatedListener]
     */
    public fun addOnPdfContentInvalidatedListener(
        executor: Executor,
        listener: OnPdfContentInvalidatedListener,
    )

    /**
     * Removes the listener from the list of listeners which are notified when some regions of the
     * PDF content are invalidated.
     *
     * @param listener The listener to remove.
     */
    public fun removeOnPdfContentInvalidatedListener(listener: OnPdfContentInvalidatedListener)

    /**
     * Represents information about a single page in the PDF document.
     *
     * @property pageNum The page number (0-based).
     * @property height The height of the page in points.
     * @property width The width of the page in points.
     * @property formWidgetInfos (Optional) A list of [FormWidgetInfo] objects representing the form
     *   widgets present on the given [pageNum]. This property is only populated if
     *   [PdfDocument.PAGE_INFO_INCLUDE_FORM_WIDGET] is set in the 'pageInfoFlags' passed to
     *   [PdfDocument.getPageInfo]. It will be empty if FormWidgetInfo is not requested or if there
     *   are no form widgets present on the page.
     */
    public class PageInfo(
        public val pageNum: Int,
        public val height: Int,
        public val width: Int,
        public val formWidgetInfos: List<FormWidgetInfo> = emptyList(),
    )

    /** A source for retrieving bitmap representations of PDF pages. */
    public interface BitmapSource : Closeable {
        public val pageNumber: Int

        /**
         * Asynchronously retrieves a bitmap representation of the page, optionally constrained to a
         * specific tile region. [renderParams] is used to determine what contents are rendered on
         * the bitmap.
         *
         * @param scaledPageSizePx The scaled page size in pixels, representing the page size in
         *   case of no zoom, and scaled page size in case of zooming.
         * @param tileRegion (Optional) The region of the page to include in the bitmap, in pixels
         *   within the `scaledPageSizePx`. This identifies the tile. If null, the entire page is
         *   included.
         * @return The bitmap representation of the page.
         * @see renderParams
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

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        PDF_FORM_TYPE_NONE,
        PDF_FORM_TYPE_ACRO_FORM,
        PDF_FORM_TYPE_XFA_FULL,
        PDF_FORM_TYPE_XFA_FOREGROUND,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class FormType

    @LongDef(flag = true, value = [PAGE_INFO_EXCLUDE_FORM_WIDGETS, PAGE_INFO_INCLUDE_FORM_WIDGET])
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class PageInfoFlags

    @LongDef(
        flag = true,
        value =
            [
                FORM_WIDGET_INCLUDE_ALL_TYPES,
                FORM_WIDGET_INCLUDE_PUSHBUTTON_TYPE,
                FORM_WIDGET_INCLUDE_CHECKBOX_TYPE,
                FORM_WIDGET_INCLUDE_RADIOBUTTON_TYPE,
                FORM_WIDGET_INCLUDE_COMBOBOX_TYPE,
                FORM_WIDGET_INCLUDE_LISTBOX_TYPE,
                FORM_WIDGET_INCLUDE_TEXTFIELD_TYPE,
                FORM_WIDGET_INCLUDE_SIGNATURE_TYPE,
            ],
    )
    @Retention(AnnotationRetention.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class FormWidgetTypeFlags

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        LINEARIZATION_STATUS_NOT_LINEARIZED,
        LINEARIZATION_STATUS_LINEARIZED,
        LINEARIZATION_STATUS_UNKNOWN,
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public annotation class LinearizationStatus

    public companion object {
        /** Represents a PDF with no form fields */
        public const val PDF_FORM_TYPE_NONE: Int = 0

        /** Represents a PDF with form fields specified using the AcroForm spec */
        public const val PDF_FORM_TYPE_ACRO_FORM: Int = 1

        /** Represents a PDF with form fields specified using the entire XFA spec */
        public const val PDF_FORM_TYPE_XFA_FULL: Int = 2

        /** Represents a PDF with form fields specified using the XFAF subset of the XFA spec */
        public const val PDF_FORM_TYPE_XFA_FOREGROUND: Int = 3

        /**
         * Flag used with [getPageInfo] to exclude any additional information in the returned
         * [PageInfo]
         */
        public const val PAGE_INFO_EXCLUDE_FORM_WIDGETS: Long = 0L
        /** Flag used with [getPageInfo] to include form widget metadata in the [PageInfo] */
        public const val PAGE_INFO_INCLUDE_FORM_WIDGET: Long = 1 shl 0

        /** Flag to include all types of form widgets in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_ALL_TYPES: Long = -1
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_UNKNOWN] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_UNKNOWN_TYPE: Long = 1 shl 0
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_PUSHBUTTON_TYPE: Long = 1 shl 1
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_CHECKBOX] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_CHECKBOX_TYPE: Long = 1 shl 2
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_RADIOBUTTON_TYPE: Long = 1 shl 3
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_COMBOBOX] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_COMBOBOX_TYPE: Long = 1 shl 4
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_LISTBOX] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_LISTBOX_TYPE: Long = 1 shl 5
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_TEXTFIELD] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_TEXTFIELD_TYPE: Long = 1 shl 6
        /** Flag to include [FormWidgetInfo.WIDGET_TYPE_SIGNATURE] in [getFormWidgetInfos] */
        public const val FORM_WIDGET_INCLUDE_SIGNATURE_TYPE: Long = 1 shl 7

        /** Indicates that the document is not linearized */
        public const val LINEARIZATION_STATUS_NOT_LINEARIZED: Int = 0
        /** Indicates that the document is linearized (optimized for fast web viewing) */
        public const val LINEARIZATION_STATUS_LINEARIZED: Int = 1
        /** Indicates that the linearization status of the document could not be determined */
        public const val LINEARIZATION_STATUS_UNKNOWN: Int = 2
    }
}
