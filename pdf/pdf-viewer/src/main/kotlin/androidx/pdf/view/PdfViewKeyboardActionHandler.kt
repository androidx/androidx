/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.view

import android.graphics.PointF
import androidx.pdf.PdfPoint
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.util.ClipboardUtils
import androidx.pdf.util.ZoomUtils
import kotlin.math.roundToInt

/**
 * Performs actions in response to keyboard shortcuts detected by [PdfViewExternalInputManager]
 *
 * @param pdfView The view on which the actions are to be taken.
 */
internal class PdfViewKeyboardActionHandler(pdfView: PdfView) :
    PdfViewExternalInputHandler(pdfView) {

    override val horizontalScrollFactor = HORIZONTAL_SCROLL_FACTOR
    override val verticalScrollFactor = VERTICAL_SCROLL_FACTOR

    private val pivotX: Float
        get() = (pdfView.left + pdfView.right) / 2f

    private val pivotY: Float
        get() = pdfView.top.toFloat()

    fun copySelection() {
        val text = (pdfView.currentSelection as? TextSelection)?.text ?: return
        ClipboardUtils.copyToClipboard(pdfView.context, text.toString())
        pdfView.clearCurrentSelection()
    }

    fun scrollLeftOrScrollToPreviousPage(): Boolean {
        val fitToWidthZoom =
            ZoomUtils.calculateZoomToFit(
                pdfView.viewportWidth.toFloat(),
                pdfView.viewportHeight.toFloat(),
                pdfView.contentWidth,
                1f,
            )
        if (pdfView.zoom <= fitToWidthZoom) {
            val previousPage = findPreviousPage() ?: return false
            pdfView.scrollToPosition(PdfPoint(previousPage, PointF(0f, 0f)), ScrollAlignment.TOP)
        } else {
            scrollLeft()
        }
        return true
    }

    fun scrollRightOrScrollToNextPage() {
        val fitToWidthZoom =
            ZoomUtils.calculateZoomToFit(
                pdfView.viewportWidth.toFloat(),
                pdfView.viewportHeight.toFloat(),
                pdfView.contentWidth,
                1f,
            )
        if (pdfView.zoom <= fitToWidthZoom) {
            pdfView.scrollToPosition(
                PdfPoint(pdfView.firstVisiblePage + 1, PointF(0f, 0f)),
                ScrollAlignment.TOP,
            )
        } else {
            scrollRight()
        }
    }

    fun zoomIn() {
        zoomIn(pivotX, pivotY)
    }

    fun zoomOut() {
        zoomOut(pivotX, pivotY)
    }

    fun zoomFitToWidth() {
        applyZoom(pdfView.getFitToWidthZoom(), pivotX, pivotY)
    }

    private fun findPreviousPage(): Int? {
        val firstPageRect =
            pdfView.pageLayoutManager?.getPageLocation(
                pdfView.firstVisiblePage,
                pdfView.getVisibleAreaInContentCoords(),
            ) ?: return null

        val scrollYForPageTop = (firstPageRect.top * pdfView.zoom).roundToInt()

        // If the current scrollY is greater than the ideal top position, it means the top of
        // the page is partially scrolled off-screen.
        val isScrolledPastTop = pdfView.scrollY > scrollYForPageTop

        val previousPage =
            if (isScrolledPastTop) {
                // If scrolled past the top, the target is the top of the current page.
                pdfView.firstVisiblePage
            } else {
                // Otherwise, the target is the previous page.
                pdfView.firstVisiblePage - 1
            }

        return if (previousPage >= 0) previousPage else null
    }

    private companion object {
        const val HORIZONTAL_SCROLL_FACTOR = 20
        const val VERTICAL_SCROLL_FACTOR = 20
    }
}
