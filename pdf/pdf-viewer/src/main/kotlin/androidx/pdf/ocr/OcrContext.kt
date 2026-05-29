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

package androidx.pdf.ocr

import android.graphics.RectF
import androidx.annotation.RestrictTo
import androidx.pdf.Dimension
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.toImagePoint
import androidx.pdf.toPdfRect
import androidx.pdf.toRectF

/** Holds OCR results and the spatial data needed to map touches back to PDF pixels. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class OcrContext(
    val ocrResult: OcrResult,
    val pageNum: Int,
    val imageRect: RectF,
    val bitmapSize: Dimension,
)

/** Searches for occurrences of the [query] and returns their bounding boxes in PDF coordinates. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun OcrContext.search(query: String): List<List<RectF>> {
    return ocrResult.getSearchBounds(query).map { match ->
        match.map { ocrRect -> ocrRect.toPdfRect(pageNum, imageRect, bitmapSize).toRectF() }
    }
}

/** Gets text between two [PdfPoint]s on the page. */
internal fun OcrContext.getText(startPoint: PdfPoint, endPoint: PdfPoint): OcrText {
    val startImagePoint = startPoint.toImagePoint(imageRect, bitmapSize)
    val endImagePoint = endPoint.toImagePoint(imageRect, bitmapSize)
    return ocrResult.getText(startImagePoint, endImagePoint)
}

/** Returns the word and its bounding boxes at the specified coordinate. */
internal fun OcrContext.getWordAt(point: PdfPoint): OcrText? {
    if (!imageRect.contains(point.x, point.y)) return null
    val imagePoint = point.toImagePoint(imageRect, bitmapSize)
    return ocrResult.getWordAt(imagePoint)
}

/** Maps [OcrText] bounds from image coordinates to PDF coordinates. */
internal fun OcrContext.mapOcrTextToPdfBounds(ocrText: OcrText): List<PdfRect> {
    return ocrText.bounds.map { it.toPdfRect(pageNum, imageRect, bitmapSize) }
}
