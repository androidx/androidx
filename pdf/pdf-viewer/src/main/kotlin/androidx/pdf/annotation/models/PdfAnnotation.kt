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

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * Represents an annotation on a PDF page.
 *
 * This abstract class serves as the base for different types of PDF annotations. It handles common
 * properties and behaviors, such as parceling for inter-process communication.
 *
 * @param pageNum The page number (0-indexed) where this annotation is located.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class PdfAnnotation(public open val pageNum: Int) : Parcelable, PdfEdit() {

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        // Write the type of the annotation first so that it can be identified
        // when unparceling.
        when (this) {
            is StampAnnotation -> {
                dest.writeInt(STAMP_ANNOTATION_TYPE)
                writeStampAnnotationToParcel(dest, flags)
            }
            else -> {
                dest.writeInt(UNKNOWN_TYPE)
            }
        }
    }

    public companion object {
        /** Unknown annotation type. */
        internal const val UNKNOWN_TYPE: Int = -1

        /** Stamp annotation type representing [StampAnnotation] */
        internal const val STAMP_ANNOTATION_TYPE: Int = 1

        /** Parcelable creator for [PdfAnnotation]. */
        @JvmField
        public val CREATOR: Parcelable.Creator<PdfAnnotation> =
            object : Parcelable.Creator<PdfAnnotation> {
                override fun createFromParcel(parcel: Parcel): PdfAnnotation? {
                    val type = parcel.readInt()
                    return when (type) {
                        STAMP_ANNOTATION_TYPE -> StampAnnotation.CREATOR.createFromParcel(parcel)
                        else -> null
                    }
                }

                override fun newArray(size: Int): Array<PdfAnnotation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
