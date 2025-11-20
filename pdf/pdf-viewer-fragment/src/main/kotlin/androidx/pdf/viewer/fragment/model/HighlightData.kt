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

package androidx.pdf.viewer.fragment.model

import androidx.annotation.ColorInt
import androidx.pdf.PdfRect
import androidx.pdf.content.PageMatchBounds
import androidx.pdf.view.Highlight

/**
 * A model class representing all the highlights to be added on PdfView.
 *
 * Additionally it contains an index, which points at current selected highlight.
 *
 * @property currentIndex index of a highlight from [highlightBounds].
 * @property highlightBounds bounds for all the highlights to be added.
 */
internal data class HighlightData(val currentIndex: Int, val highlightBounds: List<HighlightBound>)

/**
 * A model class containing information for a single highlight to be added on PdfView.
 *
 * @property pageNum page number of the document where highlight should be added.
 * @property pageMatchBounds actual bounds of the rect(s) that needs to be highlighted.
 */
internal data class HighlightBound(val pageNum: Int, val pageMatchBounds: PageMatchBounds) {

    fun toHighlight(@ColorInt highlightColor: Int): List<Highlight> =
        pageMatchBounds.bounds.map { bound -> Highlight(PdfRect(pageNum, bound), highlightColor) }
}
