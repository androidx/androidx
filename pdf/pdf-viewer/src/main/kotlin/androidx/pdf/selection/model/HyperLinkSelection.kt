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

import android.net.Uri
import android.os.Parcel
import android.text.TextUtils
import androidx.pdf.PdfRect
import androidx.pdf.selection.LinkSelection
import androidx.pdf.view.pdfRectFromParcel
import androidx.pdf.view.writeToParcel

/**
 * A [androidx.pdf.selection.Selection] for a hyperlink.
 *
 * This represents a text range that is also a hyperlink, containing both the link and the text that
 * is displayed.
 *
 * @property link The URL of the hyperlink.
 * @property linkText The text that is displayed as a hyperlink.
 * @property bounds The list of bounding boxes for the text.
 */
public class HyperLinkSelection(
    public val link: Uri,
    override val linkText: CharSequence,
    override val bounds: List<PdfRect>,
) : LinkSelection {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is HyperLinkSelection) return false

        if (other.link != this.link) return false
        if (other.linkText != this.linkText) return false
        if (other.bounds != this.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = link.hashCode()
        result = 31 * result + linkText.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }

    override fun toString(): String {
        return "HyperLinkSelection: link $link text $linkText bounds $bounds"
    }

    /** Writes a [HyperLinkSelection] to [dest]. */
    internal fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(link, flags)
        TextUtils.writeToParcel(linkText, dest, flags)
        dest.writeInt(bounds.size)
        for (bound in bounds) {
            bound.writeToParcel(dest)
        }
    }
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
