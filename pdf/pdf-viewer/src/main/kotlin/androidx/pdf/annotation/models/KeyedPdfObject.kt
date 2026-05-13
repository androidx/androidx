/*
 * Copyright 2026 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * Associates a [PdfObject] with a unique key.
 *
 * @param key The unique string identifier for the object.
 * @param pdfObject The [PdfObject] object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class KeyedPdfObject(public val key: String, public val pdfObject: PdfObject) : Parcelable {
    override fun equals(other: Any?): Boolean {
        return (other is KeyedPdfObject) && other.key == key && other.pdfObject == pdfObject
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + pdfObject.hashCode()
        return result
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(key)
        pdfObject.writeToParcel(parcel, flags)
    }

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<KeyedPdfObject> =
            object : Parcelable.Creator<KeyedPdfObject> {
                override fun createFromParcel(parcel: Parcel): KeyedPdfObject {
                    val key =
                        parcel.readString()
                            ?: throw IllegalStateException("Parcel should contain a key!")
                    val pdfObject =
                        PdfObject.CREATOR.createFromParcel(parcel)
                            ?: throw IllegalStateException("Parcel should contain a PdfObject!")
                    return KeyedPdfObject(key, pdfObject)
                }

                override fun newArray(size: Int): Array<out KeyedPdfObject?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
