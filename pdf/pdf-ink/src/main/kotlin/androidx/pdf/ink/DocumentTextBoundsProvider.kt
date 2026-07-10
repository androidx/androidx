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

package androidx.pdf.ink

import android.graphics.PointF
import android.graphics.RectF
import androidx.pdf.PdfDocument
import androidx.pdf.PdfFeature
import androidx.pdf.annotation.TextBoundsProvider
import androidx.pdf.content.PdfPageTextContent

/**
 * An implementation of [TextBoundsProvider] that retrieves text boundaries from a [PdfDocument].
 */
internal class DocumentTextBoundsProvider(private val pdfDocument: PdfDocument) :
    TextBoundsProvider {
    override suspend fun getTextBoundsBetweenPoints(
        pageNum: Int,
        start: PointF,
        end: PointF,
    ): List<RectF> {
        // selection boundary api is not available below sdk-ext 13
        if (!pdfDocument.isFeatureSupported(PdfFeature.TEXT_SELECTION)) return listOf()

        return pdfDocument
            .getSelectionBounds(pageNum, start, end)
            ?.selectedContents
            ?.filterIsInstance<PdfPageTextContent>()
            ?.flatMap { it.bounds } ?: emptyList()
    }
}
