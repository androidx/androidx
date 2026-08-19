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

import androidx.annotation.ColorInt
import androidx.pdf.Highlight
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.model.HighlightData

internal class PdfHighlightManager(
    private val pdfView: PdfView,
    @ColorInt private val selectedHighlightColor: Int,
    @ColorInt private val highlightColor: Int,
) {

    fun setHighlights(highlightData: HighlightData) {
        val bounds = highlightData.highlightBounds
        val highlights = ArrayList<Highlight>(bounds.size)
        val selectedIndex = highlightData.currentIndex
        for (i in bounds.indices) {
            val color = if (i == selectedIndex) selectedHighlightColor else highlightColor

            highlights.addAll(bounds[i].toHighlight(color))
        }

        pdfView.setHighlights(highlights)
    }
}
