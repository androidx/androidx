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
import androidx.annotation.FloatRange
import androidx.annotation.IntDef
import androidx.annotation.IntRange
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
public class FormWidgetInfo
private constructor(
    /** The [WidgetType] of this widget */
    @WidgetType public val widgetType: Int,
    /** The index of this widget among all form widgets on the page */
    public val widgetIndex: Int,
    /** The bounds of this widget in PDF coordinates */
    public val widgetRect: Rect,
    /**
     * The text value of this widget, if present. Comes from the "V" value in the annotation
     * dictionary.
     *
     * See PDF spec 1.7 Table 8.69
     */
    public val textValue: String?,
    /**
     * The accessibility label for this widget, if present. Comes from the "TU" or "T" value in the
     * annotation dictionary.
     *
     * See PDF spec 1.7 Table 8.69
     */
    public val accessibilityLabel: String?,
    /** True if this widget is read-only and accepts changes */
    public val isReadOnly: Boolean = false,
    /**
     * True if this widget has editable text. Only applicable to [WIDGET_TYPE_COMBOBOX] or
     * [WIDGET_TYPE_TEXTFIELD]. Defaults to 'false' for other widget types.
     */
    public val isEditableText: Boolean = false,
    /**
     * True if this widget supports selecting multiple [ListItem]s. Only applicable to
     * [WIDGET_TYPE_LISTBOX]. Defaults to 'false' for other widget types.
     */
    public val isMultiSelect: Boolean = false,
    /**
     * True if this widget supports multi-line text input. Only applicable to
     * [WIDGET_TYPE_TEXTFIELD]. Defaults to 'false' for other widget types.
     */
    public val isMultiLineText: Boolean = false,
    /**
     * The configured maximum text length for a form widget of type [WIDGET_TYPE_TEXTFIELD]), or -1
     * for widgets this does not apply to.
     */
    public val maxLength: Int = -1,
    /**
     * The configured font size for a form widget that accepts text input ([WIDGET_TYPE_COMBOBOX] or
     * [WIDGET_TYPE_TEXTFIELD]), or 0 for widgets this does not apply to.
     */
    public val fontSize: Float = 0.0f,
    /**
     * The set of choice options in a form widget of type [WIDGET_TYPE_COMBOBOX] or
     * [WIDGET_TYPE_LISTBOX], or an empty list for other widget types.
     */
    public val listItems: List<ListItem> = emptyList(),
) : Parcelable {

    init {
        require(widgetIndex >= 0) { "widgetIndex must be non-negative" }
        require(fontSize >= 0f) { "fontSize must be non-negative" }
    }

    private constructor(
        parcel: Parcel
    ) : this(
        widgetType = parcel.readInt(),
        widgetIndex = parcel.readInt(),
        widgetRect =
            ParcelCompat.readParcelable(parcel, Rect::class.java.classLoader, Rect::class.java)!!,
        textValue = parcel.readString(),
        accessibilityLabel = parcel.readString(),
        isReadOnly = parcel.readBoolean(),
        isEditableText = parcel.readBoolean(),
        isMultiSelect = parcel.readBoolean(),
        isMultiLineText = parcel.readBoolean(),
        maxLength = parcel.readInt(),
        fontSize = parcel.readFloat(),
        listItems = parcel.createTypedArrayList(ListItem.CREATOR)?.toList() ?: emptyList(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(widgetType)
        dest.writeInt(widgetIndex)
        dest.writeParcelable(widgetRect, flags)
        dest.writeString(textValue)
        dest.writeString(accessibilityLabel)
        dest.writeBoolean(isReadOnly)
        dest.writeBoolean(isEditableText)
        dest.writeBoolean(isMultiSelect)
        dest.writeBoolean(isMultiLineText)
        dest.writeInt(maxLength)
        dest.writeFloat(fontSize)
        dest.writeTypedList(listItems)
    }

    override fun hashCode(): Int {
        return Objects.hash(
            widgetType,
            widgetIndex,
            widgetRect,
            isReadOnly,
            textValue,
            accessibilityLabel,
            isEditableText,
            isMultiSelect,
            isMultiLineText,
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
            isReadOnly == other.isReadOnly &&
            Objects.equals(textValue, other.textValue) &&
            Objects.equals(accessibilityLabel, other.accessibilityLabel) &&
            isEditableText == other.isEditableText &&
            isMultiSelect == other.isMultiSelect &&
            isMultiLineText == other.isMultiLineText &&
            maxLength == other.maxLength &&
            fontSize == other.fontSize &&
            listItems == other.listItems
    }

    /** Represents the type of a form widget */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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

        /** Factory method for creating form widget of type [WIDGET_TYPE_PUSHBUTTON]. */
        @JvmStatic
        public fun createPushButton(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_PUSHBUTTON,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
            )

        /** Factory method for creating form widget of type [WIDGET_TYPE_CHECKBOX]. */
        @JvmStatic
        public fun createCheckbox(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_CHECKBOX,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
            )

        /** Factory method for creating form widget of type [WIDGET_TYPE_RADIOBUTTON]. */
        @JvmStatic
        public fun createRadioButton(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_RADIOBUTTON,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
            )

        /** Factory method for creating form widget of type [WIDGET_TYPE_SIGNATURE]. */
        @JvmStatic
        public fun createSignature(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_SIGNATURE,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
            )

        /** Factory method for creating form widget of type [WIDGET_TYPE_COMBOBOX]. */
        @JvmStatic
        public fun createComboBox(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
            isEditableText: Boolean,
            @FloatRange(from = 0.0) fontSize: Float,
            listItems: List<ListItem>,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_COMBOBOX,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
                isEditableText,
                fontSize = fontSize,
                listItems = listItems,
            )

        /** Factory method for creating form widget of type [WIDGET_TYPE_TEXTFIELD]. */
        @JvmStatic
        public fun createTextField(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
            isEditableText: Boolean,
            isMultiLineText: Boolean,
            @IntRange(from = 0) maxLength: Int,
            @FloatRange(from = 0.0) fontSize: Float,
        ): FormWidgetInfo {
            require(maxLength >= 0)
            require(fontSize >= 0)

            return FormWidgetInfo(
                WIDGET_TYPE_TEXTFIELD,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
                isEditableText = isEditableText,
                isMultiLineText = isMultiLineText,
                maxLength = maxLength,
                fontSize = fontSize,
            )
        }

        @JvmStatic
        public fun createListBox(
            @IntRange(from = 0) widgetIndex: Int,
            widgetRect: Rect,
            textValue: String?,
            accessibilityLabel: String?,
            isReadOnly: Boolean,
            isMultiSelect: Boolean,
            listItems: List<ListItem>,
        ): FormWidgetInfo =
            FormWidgetInfo(
                WIDGET_TYPE_LISTBOX,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                isReadOnly,
                isMultiSelect = isMultiSelect,
                listItems = listItems,
            )

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
