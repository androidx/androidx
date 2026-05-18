/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.autofill

import android.graphics.PointF
import android.os.RemoteException
import androidx.pdf.PdfDocument
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.view.PdfFormFillingState

/** Provider responsible for fetching and caching autofill hints asynchronously. */
internal class AutofillHintProvider(
    private val pdfDocument: PdfDocument,
    private val pdfFormFillingState: PdfFormFillingState,
    private val hintDetector: AutofillHintDetector = DefaultAutofillHintDetector(),
    private val onHintTextReady: (Pair<Int, Int>) -> Unit,
) {
    /**
     * Prefetches hints for text fields on the specified page asynchronously. Detected hints are
     * cached in [FormWidgetInfo].
     */
    suspend fun loadHintsForPage(pageNum: Int, allWidgets: List<FormWidgetInfo>) {
        allWidgets
            .asSequence()
            .filter {
                it.widgetType == FormWidgetInfo.WIDGET_TYPE_TEXTFIELD &&
                    pdfFormFillingState.getHintText(pageNum, it.widgetIndex) == null
            }
            .forEach { widget ->
                try {
                    val hintText = detectHintText(pageNum, widget, allWidgets)
                    if (hintText != null) {
                        pdfFormFillingState.addHintText(pageNum, widget.widgetIndex, hintText)
                        onHintTextReady(pageNum to widget.widgetIndex)
                    }
                } catch (e: RemoteException) {
                    // Fail silently as the form can be filled manually without autofill
                    // suggestions.
                }
            }
    }

    private suspend fun detectHintText(
        pageNum: Int,
        widget: FormWidgetInfo,
        allWidgets: List<FormWidgetInfo>,
    ): String? {
        val accessibilityLabel = widget.accessibilityLabel
        if (
            accessibilityLabel != null && hintDetector.detectHints(accessibilityLabel).isNotEmpty()
        ) {
            return accessibilityLabel
        }

        val leftText = searchLeftText(pageNum, widget, allWidgets)
        if (!leftText.isNullOrBlank()) {
            return leftText
        }

        val aboveText = searchAboveText(pageNum, widget)
        if (!aboveText.isNullOrBlank()) {
            return aboveText
        }

        return null
    }

    private suspend fun searchLeftText(
        pageNum: Int,
        widget: FormWidgetInfo,
        allWidgets: List<FormWidgetInfo>,
    ): String? {
        val widgetBounds = widget.widgetRect
        val widgetToLeft = getWidgetToLeft(widget, allWidgets)
        val startX = widgetToLeft?.widgetRect?.right?.toFloat() ?: HINT_SEARCH_MARGIN

        val selection =
            pdfDocument.getSelectionBounds(
                pageNum,
                PointF(startX, widgetBounds.top.toFloat()),
                PointF(widgetBounds.left - HINT_SEARCH_MARGIN, widgetBounds.bottom.toFloat()),
            ) ?: return null

        return selection.extractTextFromSelection()
    }

    private suspend fun searchAboveText(pageNum: Int, widget: FormWidgetInfo): String? {
        val widgetBounds = widget.widgetRect

        // Extracts text from a region above the widget matching its width and height
        val selection =
            pdfDocument.getSelectionBounds(
                pageNum,
                PointF(
                    widgetBounds.left.toFloat(),
                    widgetBounds.top.toFloat() - widgetBounds.height(),
                ),
                PointF(widgetBounds.right.toFloat(), widgetBounds.top.toFloat()),
            ) ?: return null

        return selection.extractTextFromSelection()
    }

    private fun getWidgetToLeft(
        targetWidget: FormWidgetInfo,
        allWidgets: List<FormWidgetInfo>,
    ): FormWidgetInfo? {
        val targetRect = targetWidget.widgetRect
        return allWidgets
            .filter { other ->
                val otherRect = other.widgetRect

                // Identifies the nearest widget to the left that vertically overlaps with the
                // target.
                other.widgetIndex != targetWidget.widgetIndex &&
                    otherRect.right < targetRect.left &&
                    otherRect.bottom > targetRect.top &&
                    otherRect.top < targetRect.bottom
            }
            .maxByOrNull { it.widgetRect.right }
    }

    private fun PageSelection.extractTextFromSelection(): String {
        return this.selectedContents
            .filterIsInstance<PdfPageTextContent>()
            .joinToString { it.text }
            .trim()
    }

    companion object {
        /**
         * The margin in PDF coordinates used to maintain a gap between the search area and the
         * widget or page boundaries. This prevents text bleeding during selection.
         */
        private const val HINT_SEARCH_MARGIN = 20f
    }
}
