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
import androidx.annotation.RestrictTo

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
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class StampAnnotation(
    pageNum: Int,
    public val bounds: RectF,
    public val pdfObjects: List<PdfObject>,
) : PdfAnnotation(pageNum) {

    override fun describeContents(): Int = 0

    /** Flattens this object in to a Parcel. */
    public fun writeStampAnnotationToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pageNum)
        bounds.writeToParcel(dest, flags)
        dest.writeInt(pdfObjects.size)
        for (pdfObject in pdfObjects) {
            when (pdfObject) {
                is PathPdfObject -> {
                    dest.writeInt(PdfObject.TYPE_PATH_PDF_OBJECT)
                    pdfObject.writeToParcel(dest, flags)
                }
            }
        }
    }

    internal companion object {
        /** [Parcelable.Creator] that instantiates [StampAnnotation] objects from a [Parcel]. */
        @JvmField
        val CREATOR: Parcelable.Creator<StampAnnotation> =
            object : Parcelable.Creator<StampAnnotation> {
                /**
                 * Creates a new instance of the Parcelable class, instantiating it from the given
                 * Parcel
                 */
                override fun createFromParcel(source: Parcel): StampAnnotation {
                    val pageNum = source.readInt()
                    val bounds = RectF()
                    bounds.readFromParcel(source)
                    val size = source.readInt()
                    val objects = mutableListOf<PdfObject>()
                    for (i in 0 until size) {

                        // Fetching the type of the object without affecting parcel position
                        val objectType = source.readInt()

                        when (objectType) {
                            PdfObject.TYPE_PATH_PDF_OBJECT -> {
                                objects.add(PathPdfObject.CREATOR.createFromParcel(source))
                            }
                        // TODO: Add other pdf object types here
                        }
                    }
                    return StampAnnotation(pageNum, bounds, objects)
                }

                /** Creates a new array of the Parcelable class. */
                override fun newArray(size: Int): Array<StampAnnotation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
