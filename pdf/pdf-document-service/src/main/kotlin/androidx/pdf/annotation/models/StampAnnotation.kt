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

package androidx.pdf.annotation.models

import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable

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
internal class StampAnnotation(pageNum: Int, val bounds: RectF, val pdfObjects: List<PdfObject>) :
    PdfAnnotation(pageNum) {

    internal constructor(
        parcel: Parcel
    ) : this(
        pageNum = parcel.readInt(),
        bounds = RectF().apply { readFromParcel(parcel) },
        pdfObjects =
            mutableListOf<PdfObject>().apply {
                val size = parcel.readInt()
                for (i in 0 until size) {
                    PdfObjectFactory.createFromParcel(parcel)?.let { add(it) }
                }
            },
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StampAnnotation) return false

        if (pageNum != other.pageNum) return false
        if (bounds != other.bounds) return false
        if (pdfObjects != other.pdfObjects) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + pdfObjects.hashCode()
        return result
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(TYPE)
        dest.writeInt(pageNum)
        bounds.writeToParcel(dest, flags)
        dest.writeInt(pdfObjects.size)
        for (pdfObject in pdfObjects) {
            pdfObject.writeToParcel(dest, flags)
        }
    }

    override fun describeContents(): Int = 0

    companion object {
        /** The type identifier for [StampAnnotation]. */
        internal const val TYPE: Int = 1

        /** [Parcelable.Creator] that instantiates [StampAnnotation] objects from a [Parcel]. */
        @JvmField
        val CREATOR: Parcelable.Creator<StampAnnotation> =
            object : Parcelable.Creator<StampAnnotation> {
                /**
                 * Creates a new instance of the Parcelable class, instantiating it from the given
                 * Parcel
                 */
                override fun createFromParcel(source: Parcel): StampAnnotation {
                    val type = source.readInt()
                    return StampAnnotation(source)
                }

                /** Creates a new array of the Parcelable class. */
                override fun newArray(size: Int): Array<StampAnnotation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
