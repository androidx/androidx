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

package androidx.pdf.util

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.os.Parcel
import android.text.TextUtils
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.annotation.content.ImagePdfObject
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.selection.Selection
import androidx.pdf.selection.model.GoToLinkSelection
import androidx.pdf.selection.model.HyperLinkSelection
import androidx.pdf.selection.model.ImageSelection
import androidx.pdf.selection.model.TextSelection
import kotlin.math.roundToInt

/**
 * Writes a [PdfRect] to [dest].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun PdfRect.writeToParcel(dest: Parcel) {
    dest.writeInt(pageNum)
    dest.writeFloat(left)
    dest.writeFloat(top)
    dest.writeFloat(right)
    dest.writeFloat(bottom)
}

/**
 * Writes a [PdfPoint] to [dest].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun PdfPoint.writeToParcel(dest: Parcel) {
    dest.writeInt(pageNum)
    dest.writeFloat(x)
    dest.writeFloat(y)
}

/** Maps a [PdfPoint] to a [Point] within the local coordinate space of an image. */
internal fun PdfPoint.toImagePoint(imageRect: RectF, bitmapSize: Point): Point {
    val imageWidth = imageRect.right - imageRect.left
    val imageHeight = imageRect.bottom - imageRect.top
    require(imageWidth > 0 && imageHeight > 0) {
        "Invalid image dimensions: $imageWidth x $imageHeight"
    }
    val relativeX = (x - imageRect.left) / imageWidth
    val relativeY = (y - imageRect.top) / imageHeight
    return Point((relativeX * bitmapSize.x).roundToInt(), (relativeY * bitmapSize.y).roundToInt())
}

/**
 * Returns a list of [Selection]s as exposed in the [androidx.pdf.view.PdfView] API from a
 * [PageSelection] as produced by the [androidx.pdf.PdfDocument] API
 */
internal fun PageSelection.toViewSelection(): List<Selection> {
    val selections = mutableListOf<Selection>()
    selectedContents.forEach { pdfPageContent ->
        when (pdfPageContent) {
            is PdfPageTextContent -> {
                val bounds = pdfPageContent.bounds.map { PdfRect(this.page, it) }
                selections.add(TextSelection(pdfPageContent.text, bounds))
            }
        }
    }
    return selections
}

internal fun ImagePdfObject.toImageSelection(pageNum: Int): ImageSelection =
    ImageSelection(
        bitmap,
        PdfRect(pageNum, this.bounds.left, this.bounds.top, this.bounds.right, this.bounds.bottom),
    )

/** The intrinsic dimensions of this object's bitmap in pixels. */
internal val ImagePdfObject.bitmapSize: Point
    get() = Point(bitmap.width, bitmap.height)

/**
 * Reads an [ImageSelection] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun imageSelectionFromParcel(source: Parcel): ImageSelection {
    val bounds = mutableListOf<PdfRect>()
    val boundsSize = source.readInt()
    for (i in 0 until boundsSize) {
        val pageNum = source.readInt()
        val left = source.readFloat()
        val top = source.readFloat()
        val right = source.readFloat()
        val bottom = source.readFloat()
        bounds.add(PdfRect(pageNum, left, top, right, bottom))
    }

    // Return an ImageSelection with a placeholder bitmap.
    // The SelectionStateManager is responsible for re-fetching the actual bitmap.
    return ImageSelection(PLACEHOLDER_BITMAP, bounds.first()).apply { isPlaceholder = true }
}

/**
 * Reads a [TextSelection] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun textSelectionFromParcel(source: Parcel): TextSelection {
    val text = requireNotNull(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source))
    val boundsSize = source.readInt()
    val bounds = mutableListOf<PdfRect>()
    for (i in 0 until boundsSize) {
        bounds.add(pdfRectFromParcel(source))
    }
    return TextSelection(text, bounds.toList())
}

/**
 * Reads a [GoToLinkSelection] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun goToLinkSelectionFromParcel(source: Parcel): GoToLinkSelection {
    val destination =
        GoToLinkSelection.Destination(
            source.readInt(),
            source.readFloat(),
            source.readFloat(),
            source.readFloat(),
        )
    val text = requireNotNull(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source))
    val boundsSize = source.readInt()
    val bounds = mutableListOf<PdfRect>()
    for (i in 0 until boundsSize) {
        bounds.add(pdfRectFromParcel(source))
    }
    return GoToLinkSelection(destination, text, bounds.toList())
}

/**
 * Reads a [HyperLinkSelection] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun hyperLinkSelectionFromParcel(source: Parcel): HyperLinkSelection {
    val link = requireNotNull(Uri.CREATOR.createFromParcel(source))
    val text = requireNotNull(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source))
    val boundsSize = source.readInt()
    val bounds = mutableListOf<PdfRect>()
    for (i in 0 until boundsSize) {
        bounds.add(pdfRectFromParcel(source))
    }
    return HyperLinkSelection(link, text, bounds.toList())
}

/**
 * Reads a [PdfPoint] from [source].
 *
 * Not part of the public API because public APIs cannot be [android.os.Parcelable]
 */
internal fun pdfPointFromParcel(source: Parcel): PdfPoint {
    val pageNum = source.readInt()
    val x = source.readFloat()
    val y = source.readFloat()
    return PdfPoint(pageNum, x, y)
}

internal fun pdfRectFromParcel(source: Parcel): PdfRect {
    val pageNum = source.readInt()
    val left = source.readFloat()
    val top = source.readFloat()
    val right = source.readFloat()
    val bottom = source.readFloat()
    return PdfRect(pageNum, left, top, right, bottom)
}

/** Tiny placeholder bitmap used when restoring ImageSelection from a parcel. */
internal val PLACEHOLDER_BITMAP: Bitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8) }
