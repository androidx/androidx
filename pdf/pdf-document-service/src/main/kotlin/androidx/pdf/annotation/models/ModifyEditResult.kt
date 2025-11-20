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
 * Represents the result of an Edit and Delete PdfEdit operation.
 *
 * @property success A list of editIds representing aospIds that are successfully processed
 * @property failures A list of editIds representing aospIds that failed to be processed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
public class ModifyEditResult(
    public override val success: List<EditId>,
    public override val failures: List<EditId>,
) : Parcelable, PdfEditResult<EditId, EditId> {

    /** Default implementation for [Parcelable.describeContents], returning 0. */
    override fun describeContents(): Int = 0

    /** Flattens this object in to a Parcel. */
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(success.size)
        success.forEach { it.writeToParcel(dest, flags) }
        dest.writeInt(failures.size)
        failures.forEach { it.writeToParcel(dest, flags) }
    }

    /** Companion object for creating [ModifyEditResult] instances from a [Parcel]. */
    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<ModifyEditResult> =
            object : Parcelable.Creator<ModifyEditResult> {
                /**
                 * Creates an [ModifyEditResult] instance from a [Parcel].
                 *
                 * @param parcel The parcel to read the object's data from.
                 * @return A new instance of [ModifyEditResult], or null if creation fails.
                 */
                override fun createFromParcel(parcel: Parcel): ModifyEditResult? {
                    val successSize = parcel.readInt()
                    val success = mutableListOf<EditId>()
                    for (i in 0 until successSize) {
                        EditId.CREATOR.createFromParcel(parcel)?.let { editId ->
                            success.add(editId)
                        }
                    }
                    val failuresSize = parcel.readInt()
                    val failures = mutableListOf<EditId>()
                    for (i in 0 until failuresSize) {
                        EditId.CREATOR.createFromParcel(parcel)?.let { editId ->
                            failures.add(editId)
                        }
                    }
                    return ModifyEditResult(success, failures)
                }

                /**
                 * Creates a new array of [ModifyEditResult].
                 *
                 * @param size The size of the array.
                 * @return An array of [ModifyEditResult] of the specified size, with all elements
                 *   initialized to null.
                 */
                override fun newArray(size: Int): Array<ModifyEditResult?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
