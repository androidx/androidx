/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.pdf;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.pdf.content.PdfPageGotoLinkContent;
import android.graphics.pdf.content.PdfPageImageContent;
import android.graphics.pdf.content.PdfPageLinkContent;
import android.graphics.pdf.content.PdfPageTextContent;
import android.graphics.pdf.models.PageMatchBounds;
import android.graphics.pdf.models.FormEditRecord;
import android.graphics.pdf.models.FormWidgetInfo;
import android.graphics.pdf.models.selection.PageSelection;
import android.graphics.pdf.models.selection.SelectionBoundary;
import android.os.ParcelFileDescriptor;
import androidx.pdf.models.Dimensions;
import androidx.pdf.annotation.models.PdfAnnotation;
import androidx.pdf.annotation.models.AnnotationResult;

/** Remote interface for interacting with a PDF document */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface PdfDocumentRemote {
    /**
     * Opens a PDF document from the given ParcelFileDescriptor.
     *
     * @param pfd The ParcelFileDescriptor containing the PDF data.
     * @param password The password to unlock the PDF, or null if not required.
     * @return A status code indicating the result of the operation (see {@link PdfStatus}).
     */
    int openPdfDocument(in ParcelFileDescriptor pfd, String password);

    /**
     * Gets the total number of pages in the document.
     *
     * @return The number of pages.
     */
    int numPages();


    /**
     * Gets the dimensions (width and height) of the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A Dimensions object containing the width and height of the page.
     */
    Dimensions getPageDimensions(int pageNum);

    /**
     * Renders the specified page of the PDF document into a Bitmap.
     *
     * @param pageNum The zero-based page number to render.
     * @param width The desired width of the resulting Bitmap.
     * @param height The desired height of the resulting Bitmap.
     * @return A Bitmap representation of the specified page, or null if an error occurs.
     */
    Bitmap getPageBitmap(int pageNum, int width, int height);

    /**
     * Renders a tile of the specified page into a Bitmap.
     *
     * <p>This method is useful for rendering large PDF pages in chunks to avoid excessive
     * memory consumption.</p>
     *
     * @param pageNum The zero-based page number to render.
     * @param tileWidth The width of the tile to render.
     * @param tileHeight The height of the tile to render.
     * @param pageWidth The width of the whole PDF page.
     * @param pageHeight The height of the whole PDF page.
     * @param offsetX The horizontal offset of the tile within the page.
     * @param offsetY The vertical offset of the tile within the page.
     * @return A Bitmap representation of the specified tile, or null if an error occurs.
     */
    Bitmap getTileBitmap(int pageNum, int tilewidth, int tileHeight, int pageWidth, int pageHeight, int offsetX, int offsetY);

    /**
     * Gets the text content of the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A list of {@link PdfPageTextContent} on the page.
     */
    List<PdfPageTextContent> getPageText(int pageNum);

    /**
     * Searches the specified page for the given query and returns the matching regions.
     *
     * @param pageNum The page number (0-based).
     * @param query The search query string.
     * @return A list of {@link PageMatchBounds} objects representing the matching regions.
     */
    List<PageMatchBounds> searchPageText(int pageNum, String query);

    /**
     * Selects text on the specified page based on the given selection boundaries.
     *
     * @param pageNum The page number (0-based).
     * @param start The starting boundary of the selection.
     * @param stop The ending boundary of the selection.
     * @return The {@link PageSelection} result.
     */
    PageSelection selectPageText(int pageNum, in SelectionBoundary start, in SelectionBoundary stop);

    /**
     * Gets a list of external links present on the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A list of all external link {@link PdfPageLinkContent} objects on the page.
     */
    List<PdfPageLinkContent> getPageExternalLinks(int pageNum);

    /**
     * Gets a list of "Go To" links and bookmarks present on the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A list of all "GoTo" link {@link PdfPageGotoLinkContent} objects on the page.
     */
    List<PdfPageGotoLinkContent> getPageGotoLinks(int pageNum);

    /**
     * Gets the image content of the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A list of {@link PdfPageImageContent} on the page.
     */
    List<PdfPageImageContent> getPageImageContent(int pageNum);

    /**
     * Checks if the PDF is linearized (optimized for fast web viewing).
     *
     * @return True if the PDF is linearized, false otherwise.
     */
    boolean isPdfLinearized();

    /**
     * Gets the type of form present in the document.
     *
     * @return The form type.
     */
    int getFormType();

    /**
     * Releases resources associated with the specified page.
     *
     * @param pageNum The page number (0-based) to release.
     */
    void releasePage(int pageNum);

    /**
     * Closes the currently open PDF document and releases associated resources.
     *
     * <p>This method should be called when the client is finished working with the
     * PDF document to ensure proper cleanup.</p>
     */
    void closePdfDocument();

    /**
     * Gets the form widgets data of the specified page.
     *
     * @param pageNum The page number (0-based).
     * @return A list of {@link FormWidgetInfo} on the page.
     */
    List<FormWidgetInfo> getFormWidgetInfos(int pageNum);

    /**
     * Gets the form widgets data of the specified page and widget types.
     *
     * @param pageNum The page number (0-based).
     * @param types The widget types to retrieve.
     * @return A list of {@link FormWidgetInfo} on the page.
     */
    List<FormWidgetInfo> getFormWidgetInfosOfType(int pageNum, in int[] types);

    /**
    * Applies a form widget edit to the PDF on the given page.
    *
    * @param pageNum The page number (0-based).
    * @param editRecord The edit to apply.
    * @return Rectangular areas of the page bitmap that have been invalidated by this action.
    */
    List<Rect> applyEdit(int pageNum, in FormEditRecord editRecord);

    /**
    * Writes the contents of the PdfDocument to the destination and closes the ParcelFileDescriptor.
    *
    * @param destination The ParcelFileDescriptor to write to.
    * @param removePasswordProtection Whether to remove password protection from the document.
    */
    void write(in ParcelFileDescriptor destination, boolean removePasswordProtection);


    /**
    * Adds the annotations present in the given file to the document.
    *
    * @param pfd The file descriptor for the file from which annotations will be added.
    *            The file should contain annotations intended for the currently opened PDF.
    * @return A {@link AnnotationResult} object indicating the success or failure of the operation.
    */
    AnnotationResult addAnnotations(in ParcelFileDescriptor pfd);

    /**
    * Retrieves all annotations present on the specified page.
    *
    * @param pageNum The 0-based index of the page from which to retrieve annotations.
    * @return A list of {@link PdfAnnotation} objects representing all annotations on the page.
    *         Returns an empty list if there are no annotations or if the page number is invalid.
    */
    List<PdfAnnotation> getPageAnnotations(int pageNum);


}