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

package androidx.pdf

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo

/** Represents the result of a batch of draft edit operations on a PDF document. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public sealed class DraftEditResult : Parcelable {

    override fun describeContents(): Int = 0

    /**
     * Represents a successful execution of all draft edit operations in the batch.
     *
     * @property ids A list of unique identifiers corresponding to the successfully applied edits.
     *   The order of IDs matches the order of operations in the request.
     */
    public data class Success(val ids: List<String>) : DraftEditResult() {

        internal constructor(
            parcel: Parcel
        ) : this(ids = parcel.createStringArrayList() ?: emptyList())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(TAG_SUCCESS)
            parcel.writeStringList(ids)
        }
    }

    /**
     * Represents a failure that occurred during the execution of the draft edit operations.
     *
     * @property failedBatchIndex The index of the operation in the batch that caused the failure.
     *   Operations before this index were successfully applied.
     * @property appliedIds A list of unique identifiers for the operations that were successfully
     *   applied before the failure occurred.
     * @property errorMessage A descriptive message explaining the reason for the failure.
     */
    public data class Failure(
        val failedBatchIndex: Int,
        val appliedIds: List<String>,
        val errorMessage: String,
    ) : DraftEditResult() {

        internal constructor(
            parcel: Parcel
        ) : this(
            failedBatchIndex = parcel.readInt(),
            appliedIds = parcel.createStringArrayList() ?: emptyList(),
            errorMessage = parcel.readString() ?: "",
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(TAG_FAILURE)
            parcel.writeInt(failedBatchIndex)
            parcel.writeStringList(appliedIds)
            parcel.writeString(errorMessage)
        }
    }

    public companion object {
        private const val TAG_SUCCESS = 0
        private const val TAG_FAILURE = 1

        @JvmField
        public val CREATOR: Parcelable.Creator<DraftEditResult> =
            object : Parcelable.Creator<DraftEditResult> {
                override fun createFromParcel(parcel: Parcel): DraftEditResult {
                    return when (val tag = parcel.readInt()) {
                        TAG_SUCCESS -> Success(parcel)
                        TAG_FAILURE -> Failure(parcel)
                        else -> throw IllegalArgumentException("Unknown DraftEditResult tag: $tag")
                    }
                }

                override fun newArray(size: Int): Array<DraftEditResult?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
