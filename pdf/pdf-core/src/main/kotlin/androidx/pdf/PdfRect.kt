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

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.os.Parcel
import androidx.annotation.RestrictTo

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

/** Maps a [PdfRect] to a [RectF]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun PdfRect.toRectF(): RectF = RectF(left, top, right, bottom)

/** Calculates the center point of a [PdfRect]. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val PdfRect.centerPoint: PdfPoint
    get() {
        val x = (left + right) / 2
        val y = (top + bottom) / 2
        return PdfPoint(pageNum, x, y)
    }

/** The vertically centered point on the left edge of this rectangle. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val PdfRect.leftCenter: PdfPoint
    get() = PdfPoint(pageNum, left, (top + bottom) / 2)

/** The vertically centered point on the right edge of this rectangle. */
@get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public val PdfRect.rightCenter: PdfPoint
    get() = PdfPoint(pageNum, right, (top + bottom) / 2)

/**
 * Maps a [Rect] from bitmap coordinates to a [PdfRect] in PDF coordinates.
 *
 * @param pageNum The page number in the PDF.
 * @param imageRect The bounding box of the image within the PDF page, in PDF coordinates. This
 *   represents the area of the PDF page that the bitmap covers.
 * @param bitmapSize The size of the bitmap in pixels from which this [Rect] was taken.
 * @return A [PdfRect] representing the same area as this [Rect], but in PDF coordinates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Rect.toPdfRect(pageNum: Int, imageRect: RectF, bitmapSize: Point): PdfRect {
    val imageWidth = imageRect.right - imageRect.left
    val imageHeight = imageRect.bottom - imageRect.top
    require(bitmapSize.x > 0 && bitmapSize.y > 0) {
        "Invalid bitmap size: ${bitmapSize.x} x ${bitmapSize.y}"
    }
    return PdfRect(
        pageNum,
        imageRect.left + (left.toFloat() / bitmapSize.x) * imageWidth,
        imageRect.top + (top.toFloat() / bitmapSize.y) * imageHeight,
        imageRect.left + (right.toFloat() / bitmapSize.x) * imageWidth,
        imageRect.top + (bottom.toFloat() / bitmapSize.y) * imageHeight,
    )
}

internal fun PdfRect.writeToParcel(dest: Parcel) {
    dest.writeInt(pageNum)
    dest.writeFloat(left)
    dest.writeFloat(top)
    dest.writeFloat(right)
    dest.writeFloat(bottom)
}

internal fun pdfRectFromParcel(source: Parcel): PdfRect {
    val pageNum = source.readInt()
    val left = source.readFloat()
    val top = source.readFloat()
    val right = source.readFloat()
    val bottom = source.readFloat()
    return PdfRect(pageNum, left, top, right, bottom)
}
