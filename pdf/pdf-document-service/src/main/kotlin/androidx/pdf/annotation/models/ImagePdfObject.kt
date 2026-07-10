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

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat

/**
 * Represents an image object within a PDF document.
 *
 * @property bitmap The [Bitmap] data of the image.
 * @property bounds The rectangular boundaries of its position and size on the PDF page.
 */
internal class ImagePdfObject(val bitmap: Bitmap, val bounds: RectF) : PdfObject {

    internal constructor(
        parcel: Parcel
    ) : this(
        bounds =
            RectF(parcel.readFloat(), parcel.readFloat(), parcel.readFloat(), parcel.readFloat()),
        bitmap =
            ParcelCompat.readParcelable(parcel, Bitmap::class.java.classLoader, Bitmap::class.java)
                ?: throw IllegalArgumentException("bitmap cannot be null"),
    )

    init {
        require(bitmap.width > 0 && bitmap.height > 0) {
            "Invalid bitmap dimensions: ${bitmap.width} width ${bitmap.height} height"
        }
        require(bounds.width() > 0 && bounds.height() > 0) { "Invalid image bounds: $bounds" }
    }

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(TYPE)
        dest.writeFloat(bounds.left)
        dest.writeFloat(bounds.top)
        dest.writeFloat(bounds.right)
        dest.writeFloat(bounds.bottom)
        dest.writeParcelable(bitmap, flags)
    }

    override fun describeContents(): Int = 0

    companion object {
        /** Constant representing an image PDF object type. */
        internal const val TYPE: Int = 2

        @JvmField
        val CREATOR: Parcelable.Creator<ImagePdfObject> =
            object : Parcelable.Creator<ImagePdfObject> {
                override fun createFromParcel(parcel: Parcel): ImagePdfObject {
                    val type = parcel.readInt()
                    return ImagePdfObject(parcel)
                }

                override fun newArray(size: Int): Array<ImagePdfObject?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
