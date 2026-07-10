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

import android.graphics.PointF

/**
 * Represents a point in PDF coordinates, where [pageNum] indicates a 0-indexed PDF page, and ([x],
 * [y]) indicates a point in PDF points within the page, with the origin existing at the top left
 * corner of the page.
 */
public class PdfPoint(public val pageNum: Int, public val x: Float, public val y: Float) {

    public constructor(pageNum: Int, pagePoint: PointF) : this(pageNum, pagePoint.x, pagePoint.y)

    init {
        require(pageNum >= 0) { "Invalid negative page" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is PdfPoint) return false

        if (other.pageNum != pageNum) return false
        if (other.x != x) return false
        if (other.y != y) return false
        return true
    }

    override fun hashCode(): Int {
        var result = pageNum.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        return result
    }

    override fun toString(): String {
        return "PdfPoint: page $pageNum pagePoint ($x, $y)"
    }
}
