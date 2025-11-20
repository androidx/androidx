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

package androidx.pdf

import android.graphics.RectF

/**
 * Represents a rectangle in PDF coordinates, where [pageNum] indicates a PDF page, and [left],
 * [top], [right], and [bottom] indicate a rect in PDF points within the page, with the origin
 * existing at the top left corner of the page.
 */
public class PdfRect(
    public val pageNum: Int,
    public val left: Float,
    public val top: Float,
    public val right: Float,
    public val bottom: Float,
) {
    public constructor(
        pageNum: Int,
        pageRect: RectF,
    ) : this(pageNum, pageRect.left, pageRect.top, pageRect.right, pageRect.bottom)

    init {
        require(pageNum >= 0) { "Invalid negative page" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is PdfRect) return false

        if (other.pageNum != pageNum) return false
        if (other.left != left) return false
        if (other.top != top) return false
        if (other.right != right) return false
        if (other.bottom != bottom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pageNum.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        return result
    }

    override fun toString(): String {
        return "PdfRect: page $pageNum area ($left, $top, $right, $bottom)"
    }
}
