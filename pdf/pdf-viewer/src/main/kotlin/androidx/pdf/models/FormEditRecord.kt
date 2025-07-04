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

package androidx.pdf.models

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.core.os.ParcelCompat
import java.util.Objects

/**
 * Record of a form filling operation that has been executed on a single form field in a PDF.
 * Contains the minimum amount of data required to replicate the action on the form.
 *
 * @see <a
 *   href="https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/PDF32000_2008.pdf">PDF
 *   32000-1:2008</a>
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FormEditRecord
private constructor(
    /** Represents the page number on which the edit occurred */
    public val pageNumber: Int,
    /** Represents the index of the widget that was edited. */
    public val widgetIndex: Int,
    @EditType public val type: Int,
    public val clickPoint: Point? = null,
    public val selectedIndices: IntArray? = null,
    public val text: String? = null,
) : Parcelable {
    init {
        require(pageNumber >= 0) { "pageNumber should be greater than or equal to 0" }
        require(widgetIndex >= 0) { "widgetIndex should be greater than or equal to 0" }
    }

    /** Construct a FormEditRecord of type [EDIT_TYPE_SET_INDICES] */
    public constructor(
        @IntRange(from = 0) pageNumber: Int,
        @IntRange(from = 0) widgetIndex: Int,
        selectedIndices: IntArray,
    ) : this(pageNumber, widgetIndex, EDIT_TYPE_SET_INDICES, selectedIndices = selectedIndices)

    /** Construct a FormEditRecord of type [EDIT_TYPE_SET_TEXT] */
    public constructor(
        @IntRange(from = 0) pageNumber: Int,
        @IntRange(from = 0) widgetIndex: Int,
        text: String,
    ) : this(pageNumber, widgetIndex, EDIT_TYPE_SET_TEXT, text = text)

    /** Construct a FormEditRecord of type [EDIT_TYPE_CLICK] */
    public constructor(
        @IntRange(from = 0) pageNumber: Int,
        @IntRange(from = 0) widgetIndex: Int,
        clickPoint: Point,
    ) : this(pageNumber, widgetIndex, EDIT_TYPE_CLICK, clickPoint = clickPoint)

    private constructor(
        parcel: Parcel
    ) : this(
        pageNumber = parcel.readInt(),
        widgetIndex = parcel.readInt(),
        type = parcel.readInt(),
        clickPoint =
            ParcelCompat.readParcelable(parcel, Point::class.java.classLoader, Point::class.java),
        selectedIndices = parcel.createIntArray(),
        text = parcel.readString(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pageNumber)
        dest.writeInt(widgetIndex)
        dest.writeInt(type)
        dest.writeParcelable(clickPoint, flags)
        dest.writeIntArray(selectedIndices)
        dest.writeString(text)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FormEditRecord) return false

        return pageNumber == other.pageNumber &&
            widgetIndex == other.widgetIndex &&
            type == other.type &&
            Objects.equals(clickPoint, other.clickPoint) &&
            selectedIndices.contentEquals(other.selectedIndices) &&
            Objects.equals(text, other.text)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            pageNumber,
            widgetIndex,
            type,
            clickPoint,
            selectedIndices.contentHashCode(),
            text,
        )
    }

    /** Form edit operation type */
    @IntDef(EDIT_TYPE_CLICK, EDIT_TYPE_SET_INDICES, EDIT_TYPE_SET_TEXT)
    @Retention(AnnotationRetention.SOURCE)
    public annotation class EditType

    public companion object {
        /** Indicates a click on a clickable form widget */
        public const val EDIT_TYPE_CLICK: Int = 0
        /** Represents setting indices on a combobox or listbox form widget */
        public const val EDIT_TYPE_SET_INDICES: Int = 1
        /** Represents setting text on a text field or editable combobox form widget */
        public const val EDIT_TYPE_SET_TEXT: Int = 2

        @JvmField
        public val CREATOR: Parcelable.Creator<FormEditRecord> =
            object : Parcelable.Creator<FormEditRecord> {
                override fun createFromParcel(parcel: Parcel): FormEditRecord? {
                    return FormEditRecord(parcel)
                }

                override fun newArray(size: Int): Array<FormEditRecord?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
