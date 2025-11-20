/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.selection.model

import android.os.Parcel
import android.text.TextUtils
import androidx.pdf.PdfRect
import androidx.pdf.selection.LinkSelection
import androidx.pdf.view.pdfRectFromParcel
import androidx.pdf.view.writeToParcel

/**
 * A [androidx.pdf.selection.Selection] for a goto link.
 *
 * This represents a text range that is also a goto link, containing both the link destination and
 * the text that is displayed.
 *
 * @property destination The destination of the goto link.
 * @property linkText The text that is displayed as a goto link.
 * @property bounds The list of bounding boxes for the text.
 */
public class GoToLinkSelection(
    public val destination: Destination,
    override val linkText: CharSequence,
    override val bounds: List<PdfRect>,
) : LinkSelection {

    /**
     * Creates a new instance of GoToLinkSelection.Destination using the page number, x coordinate,
     * and y coordinate of the destination where goto link is directing, and the zoom factor of the
     * page when goto link takes to the destination
     *
     * Note: Here (0,0) represents top-left corner of the page.
     *
     * @param pageNumber: Page number of the goto link Destination
     * @param xCoordinate: X coordinate of the goto link Destination in points (1/ 72")
     * @param yCoordinate: Y coordinate of the goto link Destination in points (1/ 72")
     * @param zoom: Zoom factor of the page when goto link takes to the destination
     */
    public class Destination(
        public val pageNumber: Int,
        public val xCoordinate: Float,
        public val yCoordinate: Float,
        public val zoom: Float,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is GoToLinkSelection) return false

        if (other.destination.pageNumber != this.destination.pageNumber) return false
        if (other.destination.xCoordinate != this.destination.xCoordinate) return false
        if (other.destination.yCoordinate != this.destination.yCoordinate) return false
        if (other.destination.zoom != this.destination.zoom) return false
        if (other.linkText != this.linkText) return false
        if (other.bounds != this.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = destination.pageNumber.hashCode()
        result = 31 * result + destination.xCoordinate.hashCode()
        result = 31 * result + destination.yCoordinate.hashCode()
        result = 31 * result + destination.zoom.hashCode()
        result = 31 * result + linkText.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }

    override fun toString(): String {
        return "GoToLinkSelection: destination $destination text $linkText bounds $bounds"
    }

    /** Writes a [GoToLinkSelection] to [dest]. */
    internal fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(destination.pageNumber)
        dest.writeFloat(destination.xCoordinate)
        dest.writeFloat(destination.yCoordinate)
        dest.writeFloat(destination.zoom)
        TextUtils.writeToParcel(linkText, dest, flags)
        dest.writeInt(bounds.size)
        for (bound in bounds) {
            bound.writeToParcel(dest)
        }
    }
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
