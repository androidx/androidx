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
import androidx.pdf.annotation.models.PdfAnnotation

internal const val TAG_INSERT = 1
internal const val TAG_UPDATE = 2
internal const val TAG_REMOVE = 3

/** Represents a single edit operation on a PDF document's annotations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DraftEditOperation : Parcelable {

    /** Returns the page number of the operation. */
    public fun getPage(): Int {
        return when (this) {
            is InsertDraftEditOperation -> this.annotation.pageNum
            is UpdateDraftEditOperation -> this.annotation.pageNum
            is RemoveDraftEditOperation -> this.pageNum
            else -> -1
        }
    }

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DraftEditOperation> =
            object : Parcelable.Creator<DraftEditOperation> {
                override fun createFromParcel(parcel: Parcel): DraftEditOperation {
                    return when (val tag = parcel.readInt()) {
                        TAG_INSERT -> InsertDraftEditOperation.createFromParcel(parcel)
                        TAG_UPDATE -> UpdateDraftEditOperation.createFromParcel(parcel)
                        TAG_REMOVE -> RemoveDraftEditOperation.createFromParcel(parcel)
                        else ->
                            throw IllegalArgumentException("Unknown DraftEditOperation tag: $tag")
                    }
                }

                override fun newArray(size: Int): Array<DraftEditOperation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/**
 * Represents an operation to insert a new annotation into the PDF document.
 *
 * @property annotation The [PdfAnnotation] object to be inserted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InsertDraftEditOperation(public val annotation: PdfAnnotation) : DraftEditOperation {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_INSERT)
        annotation.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<InsertDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): InsertDraftEditOperation {
            val annotation = PdfAnnotation.CREATOR.createFromParcel(parcel)!!
            return InsertDraftEditOperation(annotation)
        }

        override fun newArray(size: Int): Array<InsertDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Represents an operation to update an existing annotation in the PDF document.
 *
 * @property id The unique identifier of the annotation to be updated.
 * @property annotation The new [PdfAnnotation] data that will replace the existing annotation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpdateDraftEditOperation(public val id: String, public val annotation: PdfAnnotation) :
    DraftEditOperation {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_UPDATE)
        parcel.writeString(id)
        annotation.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<UpdateDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): UpdateDraftEditOperation {
            val id = parcel.readString() ?: ""
            val annotation = PdfAnnotation.CREATOR.createFromParcel(parcel)!!
            return UpdateDraftEditOperation(id, annotation)
        }

        override fun newArray(size: Int): Array<UpdateDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Represents an operation to remove an existing annotation from the PDF document.
 *
 * @property id The unique identifier of the annotation to be removed.
 * @property pageNum The page number where the annotation is located.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoveDraftEditOperation(public val id: String, public val pageNum: Int) :
    DraftEditOperation {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_REMOVE)
        parcel.writeString(id)
        parcel.writeInt(pageNum)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<RemoveDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): RemoveDraftEditOperation {
            val id = parcel.readString() ?: ""
            val pageNum = parcel.readInt()
            return RemoveDraftEditOperation(id, pageNum)
        }

        override fun newArray(size: Int): Array<RemoveDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}
