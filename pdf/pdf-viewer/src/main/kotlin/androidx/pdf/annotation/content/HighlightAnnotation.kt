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
 * Represents a Highlight Annotation in a PDF document.
 *
 * @property pageNum The page number (0-indexed) where this annotation is located.
 * @property bounds The list of bounding [RectF] of the annotation denoting areas on the page which
 *   are highlighted.
 * @property color The color of the highlight.
 */
public class HighlightAnnotation(
    pageNum: Int,
    public val bounds: List<RectF>,
    public val color: Int,
) : PdfAnnotation(pageNum) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HighlightAnnotation) return false
        return pageNum == other.pageNum && bounds == other.bounds && color == other.color
    }

    override fun hashCode(): Int {
        var result = pageNum
        result = 31 * result + bounds.hashCode()
        result = 31 * result + color
        return result
    }
}
