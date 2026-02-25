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
import androidx.annotation.RestrictTo
import androidx.core.os.ParcelCompat
import androidx.pdf.PdfRect
import androidx.pdf.selection.model.ImageSelection

/**
 * Represents an image object within a PDF document.
 *
 * @property bitmap The [Bitmap] data of the image.
 * @property bounds The rectangular boundaries of its position and size on the PDF page.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ImagePdfObject(public val bitmap: Bitmap, public val bounds: RectF) : PdfObject {

    /** Flattens this object in to a Parcel. */
    public override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeFloat(bounds.left)
        dest.writeFloat(bounds.top)
        dest.writeFloat(bounds.right)
        dest.writeFloat(bounds.bottom)
        dest.writeParcelable(bitmap, flags)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ImagePdfObject> =
            object : Parcelable.Creator<ImagePdfObject> {
                override fun createFromParcel(parcel: Parcel): ImagePdfObject {
                    val left = parcel.readFloat()
                    val top = parcel.readFloat()
                    val right = parcel.readFloat()
                    val bottom = parcel.readFloat()
                    val bitmap: Bitmap? =
                        ParcelCompat.readParcelable(
                            parcel,
                            Bitmap::class.java.classLoader,
                            Bitmap::class.java,
                        )
                    if (bitmap != null)
                        return ImagePdfObject(bitmap, RectF(left, top, right, bottom))
                    throw IllegalArgumentException("bitmap cannot be null")
                }

                override fun newArray(size: Int): Array<ImagePdfObject?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

internal fun ImagePdfObject.toImageSelection(pageNum: Int) =
    ImageSelection(
        bitmap,
        PdfRect(pageNum, this.bounds.left, this.bounds.top, this.bounds.right, this.bounds.bottom),
    )
