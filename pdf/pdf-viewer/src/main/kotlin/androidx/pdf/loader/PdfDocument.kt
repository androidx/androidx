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
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.util.Size
import android.util.SparseArray
import androidx.annotation.RestrictTo
import java.io.Closeable

@RestrictTo(RestrictTo.Scope.LIBRARY)
/** Represents a PDF document and provides methods to interact with its content. */
public interface PdfDocument : Closeable {

    /** The total number of pages in the document. */
    public val pageCount: Int

    /** Indicates whether the document is linearized (optimized for fast web viewing). */
    public val isLinearized: Boolean

    /** The type of form present in the document. */
    public val formType: Int

    /**
     * Asynchronously retrieves information about the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PageInfo] object containing information about the page.
     */
    public suspend fun getPageInfo(pageNumber: Int): PageInfo

    /**
     * Asynchronously retrieves information about a range of pages.
     *
     * @param pageRange The range of page numbers (0-based, inclusive).
     * @return A list of [PageInfo] objects, one for each page in the range.
     */
    public suspend fun getPageInfos(pageRange: IntRange): List<PageInfo>

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
        pageRange: IntRange
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
        stop: PointF
    ): PageSelection

    /**
     * Asynchronously retrieves the content (text and images) of the specified page.
     *
     * @param pageNumber The page number (0-based).
     * @return A [PdfPageContent] object representing the page's content.
     */
    public suspend fun getPageContent(pageNumber: Int): PdfPageContent

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
     * Represents information about a single page in the PDF document.
     *
     * @property pageNum The page number (0-based).
     * @property height The height of the page in points.
     * @property width The width of the page in points.
     */
    public class PageInfo(public val pageNum: Int, public val height: Int, public val width: Int)

    /** A source for retrieving bitmap representations of PDF pages. */
    public interface BitmapSource : Closeable {
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
        public val imageContents: List<PdfPageImageContent>
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
        public val externalLinks: List<PdfPageLinkContent>
    )

    /**
     * Represents a point within a specific page of a PDF document.
     *
     * @property pageNumber The page number (0-based) where the point is located.
     * @property pagePoint The coordinates (x, y) of the point relative to the page's origin.
     */
    public class PdfPoint(public val pageNumber: Int, public val pagePoint: PointF)
}
