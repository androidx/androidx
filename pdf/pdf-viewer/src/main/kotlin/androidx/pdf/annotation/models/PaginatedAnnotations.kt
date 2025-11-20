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
 * Represents a paginated batch of PDF annotations.
 *
 * @property annotations The list of [PdfAnnotationData] objects for the current page or batch.
 * @property currentBatchIndex The 0-based index of the current batch of annotations.
 * @property totalBatchCount The total number of batches available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("BanParcelableUsage")
public class PaginatedAnnotations(
    public val annotations: List<PdfAnnotationData>,
    public val currentBatchIndex: Int,
    public val totalBatchCount: Int,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(annotations)
        dest.writeInt(currentBatchIndex)
        dest.writeInt(totalBatchCount)
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<PaginatedAnnotations> =
            object : Parcelable.Creator<PaginatedAnnotations> {
                override fun createFromParcel(parcel: Parcel): PaginatedAnnotations {
                    val annotations =
                        parcel.createTypedArrayList(PdfAnnotationData.CREATOR) ?: emptyList()
                    val currentBatchIndex = parcel.readInt()
                    val totalBatchCount = parcel.readInt()
                    return PaginatedAnnotations(annotations, currentBatchIndex, totalBatchCount)
                }

                override fun newArray(size: Int): Array<out PaginatedAnnotations?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
