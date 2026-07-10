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

package androidx.pdf.annotation.content

import android.graphics.RectF

/**
 * Represents a stamp annotation in a PDF document.
 *
 * Stamp annotations are used to add different types of [PdfObject]. This class encapsulates the
 * properties of a stamp annotation, such as its page number, bounding box, and the list of PDF
 * objects that constitute the stamp's appearance.
 *
 * @property pageNum The page number (0-indexed) where this annotation is located.
 * @property bounds The bounding [RectF] of the annotation on the page, in PDF coordinates.
 * @property pdfObjects A list of [PdfObject] instances that define the visual appearance of the
 *   stamp.
 */
public class StampAnnotation(
    pageNum: Int,
    public val bounds: RectF,
    public val pdfObjects: List<PdfObject>,
) : PdfAnnotation(pageNum) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StampAnnotation) return false
        return pageNum == other.pageNum && bounds == other.bounds && pdfObjects == other.pdfObjects
    }

    override fun hashCode(): Int {
        var result = pageNum
        result = 31 * result + bounds.hashCode()
        result = 31 * result + pdfObjects.hashCode()
        return result
    }
}
