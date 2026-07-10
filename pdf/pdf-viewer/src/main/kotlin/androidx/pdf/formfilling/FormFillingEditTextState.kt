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

package androidx.pdf.formfilling

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.pdf.models.FormWidgetInfo

/**
 * Stores the state of the [androidx.pdf.view.FormFillingEditText] if there is any present in the
 * [androidx.pdf.view.PdfView]
 */
@SuppressLint("BanParcelableUsage")
internal class FormFillingEditTextState(
    internal val currentText: String,
    internal val pageNumber: Int,
    internal val formWidgetInfo: FormWidgetInfo?,
) : Parcelable {
    private constructor(
        parcel: Parcel
    ) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        ParcelCompat.readParcelable(
            parcel,
            FormWidgetInfo::class.java.classLoader,
            FormWidgetInfo::class.java,
        ),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(currentText)
        dest.writeInt(pageNumber)
        dest.writeParcelable(formWidgetInfo, flags)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<FormFillingEditTextState> =
            object : Parcelable.Creator<FormFillingEditTextState> {
                override fun createFromParcel(parcel: Parcel): FormFillingEditTextState? {
                    return FormFillingEditTextState(parcel)
                }

                override fun newArray(size: Int): Array<out FormFillingEditTextState?>? {
                    return arrayOfNulls(size)
                }
            }
    }
}
