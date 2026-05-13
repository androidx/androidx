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

package androidx.pdf.models

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.pdf.annotation.models.KeyedPdfObject

/**
 * Represents a batch of [KeyedPdfObject] objects along with pagination information.
 *
 * @param objects The list of [KeyedPdfObject] objects in this batch.
 * @param currentBatchIndex The 0-based index of this batch.
 * @param totalBatchCount The total number of batches available for the request.
 */
@SuppressLint("BanParcelableUsage")
internal class PaginatedObjects(
    val objects: List<KeyedPdfObject>,
    val currentBatchIndex: Int,
    val totalBatchCount: Int,
) : Parcelable {

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(objects)
        parcel.writeInt(currentBatchIndex)
        parcel.writeInt(totalBatchCount)
    }

    companion object {

        @JvmField
        val CREATOR: Parcelable.Creator<PaginatedObjects> =
            object : Parcelable.Creator<PaginatedObjects> {
                override fun createFromParcel(parcel: Parcel): PaginatedObjects {
                    val objects = parcel.createTypedArrayList(KeyedPdfObject.CREATOR) ?: emptyList()
                    val currentBatchIndex = parcel.readInt()
                    val totalBatchCount = parcel.readInt()
                    return PaginatedObjects(objects, currentBatchIndex, totalBatchCount)
                }

                override fun newArray(size: Int): Array<out PaginatedObjects?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
