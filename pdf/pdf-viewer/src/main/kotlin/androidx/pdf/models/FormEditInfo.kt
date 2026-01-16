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
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.pdf.PdfPoint
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
public class FormEditInfo
private constructor(
    /** Represents the page number on which the edit occurred */
    public val pageNumber: Int,
    /** Represents the index of the widget that was edited. */
    public val widgetIndex: Int,
    @EditType public val type: Int,
    public val clickPoint: PdfPoint? = null,
    public val text: String? = null,
    private val selectedIndices: IntArray? = null,
) : Parcelable {
    init {
        require(pageNumber >= 0) { "pageNumber should be greater than or equal to 0" }
        require(widgetIndex >= 0) { "widgetIndex should be greater than or equal to 0" }
    }

    private constructor(
        parcel: Parcel
    ) : this(
        pageNumber = parcel.readInt(),
        widgetIndex = parcel.readInt(),
        type = parcel.readInt(),
        selectedIndices = parcel.createIntArray(),
        text = parcel.readString(),
        clickPoint = readPdfPointFromParcel(parcel),
    )

    /**
     * Returns the count of the selected items.
     *
     * @see 'selectedIndices' in [FormEditInfo.createSetIndices].
     */
    public val selectedIndexCount: Int
        get() = selectedIndices?.size ?: 0

    /**
     * Returns the index of the selected item in the list [FormWidgetInfo.listItems] at the given
     * [index] in the list of selected indices.
     *
     * @param index The position of the selected index to retrieve, from 0 to [selectedIndexCount]
     *     - 1.
     *
     * @return The index of the selected item from the list [FormWidgetInfo.listItems], returns -1
     *   in case there is no selection or the index is invalid.
     */
    public fun getSelectedIndexAt(index: Int): Int {
        return if (selectedIndices == null || index !in 0..<selectedIndexCount) {
            -1
        } else {
            selectedIndices[index]
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(pageNumber)
        dest.writeInt(widgetIndex)
        dest.writeInt(type)
        dest.writeIntArray(selectedIndices)
        dest.writeString(text)
        if (clickPoint == null) {
            dest.writeInt(-1)
        } else {
            dest.writeInt(clickPoint.pageNum)
            dest.writeFloat(clickPoint.x)
            dest.writeFloat(clickPoint.y)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FormEditInfo) return false

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
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class EditType

    public companion object {
        /** Indicates a click on a clickable form widget */
        public const val EDIT_TYPE_CLICK: Int = 0
        /** Represents setting indices on a combobox or listbox form widget */
        public const val EDIT_TYPE_SET_INDICES: Int = 1
        /** Represents setting text on a text field or editable combobox form widget */
        public const val EDIT_TYPE_SET_TEXT: Int = 2

        /**
         * Create a FormEditInfo object of type [EDIT_TYPE_CLICK]
         *
         * @param widgetIndex The index of the widget that was edited.
         * @param clickPoint The point on the [pageNumber] in PDF coordinates where the click
         *   occurred. Note: The origin exists at the top left corner of the page.
         * @see [androidx.pdf.view.PdfView.viewToPdfPoint]
         */
        @JvmStatic
        public fun createClick(
            @IntRange(from = 0) widgetIndex: Int,
            clickPoint: PdfPoint,
        ): FormEditInfo {
            return FormEditInfo(
                clickPoint.pageNum,
                widgetIndex,
                EDIT_TYPE_CLICK,
                clickPoint = clickPoint,
            )
        }

        /**
         * Create a FormEditInfo object of type [EDIT_TYPE_SET_INDICES]
         *
         * @param pageNumber The page number on which the edit occurred.
         * @param widgetIndex The index of the widget that was edited.
         * @param selectedIndices The indices of the selected items from [FormWidgetInfo.listItems]
         */
        @JvmStatic
        public fun createSetIndices(
            @IntRange(from = 0) pageNumber: Int,
            @IntRange(from = 0) widgetIndex: Int,
            selectedIndices: IntArray,
        ): FormEditInfo {
            return FormEditInfo(
                pageNumber,
                widgetIndex,
                EDIT_TYPE_SET_INDICES,
                selectedIndices = selectedIndices,
            )
        }

        /**
         * Create a FormEditInfo object of type [EDIT_TYPE_SET_TEXT]
         *
         * @param pageNumber The page number on which the edit occurred.
         * @param widgetIndex The index of the widget that was edited.
         * @param text The text to set on the widget.
         */
        @JvmStatic
        public fun createSetText(
            @IntRange(from = 0) pageNumber: Int,
            @IntRange(from = 0) widgetIndex: Int,
            text: String,
        ): FormEditInfo {
            return FormEditInfo(pageNumber, widgetIndex, EDIT_TYPE_SET_TEXT, text = text)
        }

        private fun readPdfPointFromParcel(parcel: Parcel): PdfPoint? {
            val pageNumber = parcel.readInt()
            return if (pageNumber == -1) {
                null
            } else {
                PdfPoint(pageNumber, parcel.readFloat(), parcel.readFloat())
            }
        }

        @JvmField
        public val CREATOR: Parcelable.Creator<FormEditInfo> =
            object : Parcelable.Creator<FormEditInfo> {
                override fun createFromParcel(parcel: Parcel): FormEditInfo {
                    return FormEditInfo(parcel)
                }

                override fun newArray(size: Int): Array<FormEditInfo?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
