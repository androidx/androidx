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

package androidx.pdf.viewer.fragment.view

import android.graphics.PointF
import androidx.annotation.ColorInt
import androidx.pdf.PdfPoint
import androidx.pdf.view.Highlight
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.model.HighlightData

/** A view manager class for [PdfView]. */
internal class PdfViewManager(
    private val pdfView: PdfView,
    @ColorInt private val selectedHighlightColor: Int,
    @ColorInt private val highlightColor: Int,
) {

    fun setHighlights(highlightData: HighlightData) {
        val highlights = mutableListOf<Highlight>()

        highlightData.highlightBounds.mapIndexed { index, highlightBound ->
            val highlightColor =
                if (highlightData.currentIndex == index) selectedHighlightColor else highlightColor

            highlights.addAll(highlightBound.toHighlight(highlightColor))
        }

        pdfView.setHighlights(highlights)
    }

    fun scrollToCurrentSearchResult(highlightData: HighlightData) {
        with(highlightData) {
            // if not invalid-index, scroll to selected highlight
            if (currentIndex != -1) {
                val selectedHighlight = highlightBounds[currentIndex]
                /* select starting bound of the match, as this is a reference point where user
                should be scrolled to. */
                val selectedBounds = selectedHighlight.pageMatchBounds.bounds.first()
                /**
                 * There could be a potential race condition between [PdfView.onLayout] and
                 * [PdfView.scrollToPosition] in scenarios such as fragment(and view) recreation.
                 *
                 * To ensure that the view is properly laid out before attempting to scroll, we post
                 * the scroll action to the message queue. This will allow the layout pass to
                 * complete before the scroll position is applied.
                 */
                pdfView.post {
                    pdfView.scrollToPosition(
                        position =
                            PdfPoint(
                                selectedHighlight.pageNum,
                                PointF(selectedBounds.left, selectedBounds.top),
                            )
                    )
                }
            }
        }
    }
}
