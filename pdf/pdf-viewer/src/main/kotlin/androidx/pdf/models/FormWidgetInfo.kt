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
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.core.os.ParcelCompat
import java.util.Objects

/**
 * Information about a form widget of a PDF document.
 *
 * @see <a
 *   href="https://opensource.adobe.com/dc-acrobat-sdk-docs/pdfstandards/PDF32000_2008.pdf">PDF
 *   32000-1:2008</a>
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FormWidgetInfo(
    @WidgetType public val widgetType: Int,
    public val widgetIndex: Int,
    public val widgetRect: Rect,
    public val textValue: String?,
    public val accessibilityLabel: String?,
    public val readOnly: Boolean = false,
    public val editableText: Boolean = false,
    public val multiSelect: Boolean = false,
    public val multiLineText: Boolean = false,
    maxLength: Int? = null,
    fontSize: Float? = null,
    listItems: List<ListItem>? = null,
) : Parcelable {
    init {
        if (editableText) {
            require(widgetType == WIDGET_TYPE_COMBOBOX || widgetType == WIDGET_TYPE_TEXTFIELD) {
                "Editable text is only supported on combo-boxes and text fields"
            }
        }

        if (multiSelect) {
            require(widgetType == WIDGET_TYPE_LISTBOX) {
                "Multi-select is only supported on text fields"
            }
        }

        if (multiLineText) {
            require(widgetType == WIDGET_TYPE_TEXTFIELD) {
                "Multiline text is only supported on text fields"
            }
        }
    }

    public val maxLength: Int =
        maxLength?.also {
            require(it > 0) { "Invalid max Length" }
            require(widgetType == WIDGET_TYPE_TEXTFIELD) {
                "Max length is only supported on text fields"
            }
        } ?: Int.MIN_VALUE

    public val fontSize: Float =
        fontSize?.also {
            require(it > 0) { "Invalid font size" }
            require(widgetType == WIDGET_TYPE_COMBOBOX || widgetType == WIDGET_TYPE_TEXTFIELD) {
                "Font size is only supported on combo-boxes and text fields"
            }
        } ?: Float.MIN_VALUE

    public val listItems: List<ListItem> =
        listItems?.also {
            require(widgetType == WIDGET_TYPE_COMBOBOX || widgetType == WIDGET_TYPE_LISTBOX) {
                "Choice options are only supported on combo-boxes and list boxes"
            }
        } ?: emptyList()

    private constructor(
        parcel: Parcel
    ) : this(
        widgetType = parcel.readInt(),
        widgetIndex = parcel.readInt(),
        widgetRect =
            ParcelCompat.readParcelable(parcel, Rect::class.java.classLoader, Rect::class.java)!!,
        textValue = parcel.readString(),
        accessibilityLabel = parcel.readString(),
        readOnly = parcel.readBoolean(),
        editableText = parcel.readBoolean(),
        multiSelect = parcel.readBoolean(),
        multiLineText = parcel.readBoolean(),
        maxLength = parcel.readInt().takeIf { it != Int.MIN_VALUE },
        fontSize = parcel.readFloat().takeIf { it != Float.MIN_VALUE },
        listItems =
            parcel.createTypedArrayList(ListItem.CREATOR).takeIf { it?.isNotEmpty() == true },
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(widgetType)
        dest.writeInt(widgetIndex)
        dest.writeParcelable(widgetRect, flags)
        dest.writeString(textValue)
        dest.writeString(accessibilityLabel)
        dest.writeBoolean(readOnly)
        dest.writeBoolean(editableText)
        dest.writeBoolean(multiSelect)
        dest.writeBoolean(multiLineText)
        dest.writeInt(maxLength)
        dest.writeFloat(fontSize)
        dest.writeTypedList(listItems)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            widgetType,
            widgetIndex,
            widgetRect,
            readOnly,
            textValue,
            accessibilityLabel,
            editableText,
            multiSelect,
            multiLineText,
            maxLength,
            fontSize,
            listItems,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FormWidgetInfo) return false

        return widgetType == other.widgetType &&
            widgetIndex == other.widgetIndex &&
            Objects.equals(widgetRect, other.widgetRect) &&
            readOnly == other.readOnly &&
            Objects.equals(textValue, other.textValue) &&
            Objects.equals(accessibilityLabel, other.accessibilityLabel) &&
            editableText == other.editableText &&
            multiSelect == other.multiSelect &&
            multiLineText == other.multiLineText &&
            maxLength == other.maxLength &&
            fontSize == other.fontSize &&
            listItems == other.listItems
    }

    /** Represents the type of a form widget */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        WIDGET_TYPE_UNKNOWN,
        WIDGET_TYPE_PUSHBUTTON,
        WIDGET_TYPE_CHECKBOX,
        WIDGET_TYPE_RADIOBUTTON,
        WIDGET_TYPE_COMBOBOX,
        WIDGET_TYPE_LISTBOX,
        WIDGET_TYPE_TEXTFIELD,
        WIDGET_TYPE_SIGNATURE,
    )
    public annotation class WidgetType

    public companion object {
        /** Represents a form widget type that is unknown */
        public const val WIDGET_TYPE_UNKNOWN: Int = 0
        /** Represents a push button type form widget */
        public const val WIDGET_TYPE_PUSHBUTTON: Int = 1
        /** Represents a checkbox type form widget */
        public const val WIDGET_TYPE_CHECKBOX: Int = 2
        /** Represents a radio button type form widget */
        public const val WIDGET_TYPE_RADIOBUTTON: Int = 3
        /** Represents a combobox type form widget */
        public const val WIDGET_TYPE_COMBOBOX: Int = 4
        /** Represents a listbox type form widget */
        public const val WIDGET_TYPE_LISTBOX: Int = 5
        /** Represents a text field type form widget */
        public const val WIDGET_TYPE_TEXTFIELD: Int = 6
        /** Represents a signature type form widget */
        public const val WIDGET_TYPE_SIGNATURE: Int = 7

        @JvmField
        public val CREATOR: Parcelable.Creator<FormWidgetInfo> =
            object : Parcelable.Creator<FormWidgetInfo> {
                override fun createFromParcel(parcel: Parcel): FormWidgetInfo? {
                    return FormWidgetInfo(parcel)
                }

                override fun newArray(size: Int): Array<FormWidgetInfo?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
