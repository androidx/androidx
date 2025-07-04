/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file    except in compliance with the License.
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

package androidx.pdf.adapter

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.RenderParams
import android.graphics.pdf.content.PdfPageGotoLinkContent
import android.graphics.pdf.content.PdfPageImageContent
import android.graphics.pdf.content.PdfPageLinkContent
import android.graphics.pdf.content.PdfPageTextContent
import android.graphics.pdf.models.FormEditRecord
import android.graphics.pdf.models.FormWidgetInfo
import android.graphics.pdf.models.PageMatchBounds
import android.graphics.pdf.models.selection.PageSelection
import android.graphics.pdf.models.selection.SelectionBoundary
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
/**
 * An interface for interacting with individual pages of a PDF document.
 *
 * This interface provides a consistent API for interacting with PDF page rendering engines,
 * abstracting away the differences between [android.graphics.pdf.PdfRenderer.Page] and
 * [android.graphics.pdf.PdfRendererPreV.Page] based on the Android OS version.
 */
public interface PdfPage : AutoCloseable {
    /** The height of the page in pixels. */
    public val height: Int

    /** The width of the page in pixels. */
    public val width: Int

    /**
     * Renders the entire page onto the provided [Bitmap].
     *
     * @param bitmap The [Bitmap] to render the page onto.
     */
    public fun renderPage(bitmap: Bitmap)

    /**
     * Renders a specific tile of the page onto the provided [Bitmap].
     *
     * This allows for rendering only a portion of the page, which can be useful for implementing
     * zooming functionality.
     *
     * @param bitmap The [Bitmap] to render the tile onto.
     * @param left The left coordinate of the tile in pixels.
     * @param top The top coordinate of the tile in pixels.
     * @param scaledPageWidth The scaled width of the page in pixels.
     * @param scaledPageHeight The scaled height of the page in pixels.
     */
    public fun renderTile(
        bitmap: Bitmap,
        left: Int,
        top: Int,
        scaledPageWidth: Int,
        scaledPageHeight: Int,
    )

    /**
     * Retrieves the text contents of the page.
     *
     * @return A list of [PdfPageTextContent] objects representing the text elements on the page.
     */
    public fun getPageTextContents(): List<PdfPageTextContent>

    /**
     * Retrieves the image contents of the page.
     *
     * @return A list of [PdfPageImageContent] objects representing the images on the page.
     */
    public fun getPageImageContents(): List<PdfPageImageContent>

    /**
     * Retrieves information about all form widgets on the page, returns an empty list if there are
     * no form widgets on the page.
     *
     * @return A list of [FormWidgetInfo] objects representing the form widgets on the page.
     */
    public fun getFormWidgetInfos(): List<FormWidgetInfo>

    /**
     * Retrieves a list of [FormWidgetInfo] objects for all the form widgets of the given types.
     *
     * @param types an array of the types of form widgets to retrieve
     * @return A list of [FormWidgetInfo] objects representing the form widgets of the given types.
     */
    public fun getFormWidgetInfos(types: IntArray): List<FormWidgetInfo>

    /**
     * Selects text on the page based on the given boundaries.
     *
     * @param start The starting boundary of the selection.
     * @param stop The ending boundary of the selection.
     * @return A [PageSelection] object representing the selected text, or null if no text is
     *   selected.
     */
    public fun selectPageText(start: SelectionBoundary, stop: SelectionBoundary): PageSelection?

    /**
     * Searches for the given query within the page's text content.
     *
     * @param query The text to search for.
     * @return A list of [PageMatchBounds] objects representing the locations of the matches.
     */
    public fun searchPageText(query: String): List<PageMatchBounds>

    /**
     * Retrieves the external links present on the page.
     *
     * @return A list of [PdfPageLinkContent] objects representing the links.
     */
    public fun getPageLinks(): List<PdfPageLinkContent>

    /**
     * Retrieves the "Go to" links present on the page.
     *
     * These are links that navigate to a specific location within the document.
     *
     * @return A list of [PdfPageGotoLinkContent] objects representing the "Go to" links.
     */
    public fun getPageGotoLinks(): List<PdfPageGotoLinkContent>

    /**
     * Returns the [RenderParams] used for rendering the page.
     *
     * By default, this includes rendering flags for highlighting and text annotations.
     *
     * @return The [RenderParams] for this page.
     */
    public fun getRenderParams(): RenderParams

    /**
     * Applies a [FormEditRecord] to the given PDF.
     *
     * @param editRecord The [FormEditRecord] to apply to the PDF.
     * @return Rectangular areas of the page bitmap that have been invalidated by this action.
     */
    public fun applyEdit(editRecord: FormEditRecord): List<Rect>
}
