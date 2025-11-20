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

import android.os.Parcel
import android.text.TextUtils
import androidx.pdf.PdfRect
import androidx.pdf.selection.Selection
import androidx.pdf.view.pdfRectFromParcel
import androidx.pdf.view.writeToParcel

/**
 * Represents text content that has been selected.
 *
 * @property text The selected text.
 * @property bounds The bounding rectangles of the selected text.
 */
public class TextSelection(public val text: CharSequence, override val bounds: List<PdfRect>) :
    Selection {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextSelection) return false

        if (other.text != this.text) return false
        if (other.bounds != this.bounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + bounds.hashCode()
        return result
    }

    override fun toString(): String {
        return "TextSelection: text $text bounds $bounds"
    }

    /** Writes a [TextSelection] to [dest]. */
    internal fun writeToParcel(dest: Parcel, flags: Int) {
        TextUtils.writeToParcel(text, dest, flags)
        dest.writeInt(bounds.size)
        for (bound in bounds) {
            bound.writeToParcel(dest)
        }
    }
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
