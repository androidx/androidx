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

package androidx.pdf.annotation

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.pdf.annotation.models.PdfAnnotation

/**
 * Associates a [PdfAnnotation] with a unique key.
 *
 * @param key The unique string identifier for the annotation.
 * @param annotation The [PdfAnnotation] object.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class KeyedPdfAnnotation(public val key: String, public val annotation: PdfAnnotation) :
    Parcelable {
    override fun equals(other: Any?): Boolean {
        return (other is KeyedPdfAnnotation) && other.key == key && other.annotation == annotation
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + annotation.hashCode()
        return result
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(key)
        annotation.writeToParcel(p0, p1)
    }

    public companion object {

        @JvmField
        public val CREATOR: Parcelable.Creator<KeyedPdfAnnotation> =
            object : Parcelable.Creator<KeyedPdfAnnotation> {
                override fun createFromParcel(parcel: Parcel): KeyedPdfAnnotation {
                    val key =
                        parcel.readString()
                            ?: throw IllegalStateException("Parcel should contain a key!")
                    val annotation = PdfAnnotation.CREATOR.createFromParcel(parcel)
                    return KeyedPdfAnnotation(key, annotation)
                }

                override fun newArray(size: Int): Array<out KeyedPdfAnnotation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
