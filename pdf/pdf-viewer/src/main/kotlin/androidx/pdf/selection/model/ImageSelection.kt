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

package androidx.pdf.selection.model

import android.graphics.Bitmap
import android.os.Parcel
import androidx.pdf.PdfRect
import androidx.pdf.selection.Selection
import androidx.pdf.view.PdfView

/**
 * Represents a specific image object selected from a PDF document.
 *
 * The [ImageSelection] is delivered to the application if [PdfView.isImageSelectionEnabled] is
 * enabled, via the [PdfView.OnSelectionChangedListener]. It can then be used to implement features
 * like image sharing or copying. The [bounds] can be passed to [PdfView.setHighlights] to visually
 * highlight the selected image, while the [bitmap] can be used for copying or sharing.
 *
 * ##### Memory Management
 * As this class holds a [Bitmap], it implements [AutoCloseable]. It is **mandatory** to call
 * [close] when the object is no longer needed to recycle the bitmap and avoid memory leaks. Using a
 * `try-with-resources` statement (or the `use` extension function in Kotlin) is recommended for
 * safe handling.
 *
 * @property bitmap This object captures the entire [Bitmap] of image located at the top of the
 *   Z-axis (the visually topmost image) at the interaction point. This is the raw image data and
 *   not a "flattened" screenshot; the [bitmap] contains only the data for that specific image
 *   object and excludes overlapping text, vector graphics, or background content.
 * @property bounds A list containing a single [PdfRect] that defines the page number and the
 *   bounding box of the image in PDF points. These bounds represent the image's layout position and
 *   scale on the page.
 */
public class ImageSelection internal constructor(public val bitmap: Bitmap, imageBounds: PdfRect) :
    Selection, AutoCloseable {

    public override val bounds: List<PdfRect> = listOf(imageBounds)

    /**
     * Internal flag to indicate if this selection was restored from a parcel without its bitmap.
     */
    internal var isPlaceholder: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ImageSelection) return false

        // Bitmaps are compared by reference, as comparing pixel by pixel is expensive.
        if (other.bitmap != this.bitmap) return false
        if (other.bounds != this.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }

    override fun close() {
        if (!bitmap.isRecycled && !isPlaceholder) {
            bitmap.recycle()
        }
    }

    /** Writes a [ImageSelection] to [dest]. */
    internal fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(bounds.size)
        for (bound in bounds) {
            dest.writeInt(bound.pageNum)
            dest.writeFloat(bound.left)
            dest.writeFloat(bound.top)
            dest.writeFloat(bound.right)
            dest.writeFloat(bound.bottom)
        }
        // We do not write the bitmap to the parcel due to its large size.
        // It will be re-fetched on restoration.
    }

    internal companion object {
        /** Tiny placeholder bitmap used when restoring ImageSelection from a parcel. */
        val PLACEHOLDER_BITMAP: Bitmap by lazy { Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8) }
    }
}

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
    return ImageSelection(ImageSelection.PLACEHOLDER_BITMAP, bounds.first()).apply {
        isPlaceholder = true
    }
}
