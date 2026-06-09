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

/**
 * This sealed interface represents specific types of PDF objects, such as [PathPdfObject]. It is
 * also [Parcelable] to allow for seamless communication between different android components.
 */
internal sealed interface PdfObject : Parcelable {

    abstract override fun writeToParcel(dest: Parcel, flags: Int)

    /** Companion object holding constants related to [PdfObject] types. */
    companion object {

        /** Parcelable creator for [PdfObject]. */
        @JvmField
        val CREATOR: Parcelable.Creator<PdfObject> =
            object : Parcelable.Creator<PdfObject> {
                override fun createFromParcel(parcel: Parcel): PdfObject? {
                    return PdfObjectFactory.createFromParcel(parcel)
                }

                override fun newArray(size: Int): Array<PdfObject?> {
                    return arrayOfNulls(size)
                }
            }
    }

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0
}
