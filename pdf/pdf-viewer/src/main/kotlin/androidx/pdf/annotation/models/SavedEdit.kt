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

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/**
 * Represents a savedEdit, containing its [EditId] and [PdfAnnotation].
 *
 * @param editId The [EditId] is unique identifier for the edit.
 * @param annotation The [PdfAnnotation] that was saved.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class SavedEdit(public val editId: EditId, public val annotation: PdfAnnotation) :
    Parcelable {
    override fun describeContents(): Int = 0

    /** Flatten this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        editId.writeToParcel(dest, flags)
        annotation.writeToParcel(dest, flags)
    }

    public companion object {

        /** Creator for generating instances of [SavedEdit] from a [Parcel]. */
        @JvmField
        public val CREATOR: Parcelable.Creator<SavedEdit> =
            object : Parcelable.Creator<SavedEdit> {
                override fun createFromParcel(parcel: Parcel): SavedEdit {
                    val editId = EditId.CREATOR.createFromParcel(parcel)
                    val annotation = PdfAnnotation.CREATOR.createFromParcel(parcel)
                    return SavedEdit(editId, annotation)
                }

                override fun newArray(size: Int): Array<out SavedEdit?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
