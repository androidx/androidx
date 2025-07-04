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

import android.graphics.Rect
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FormWidgetInfoTest {

    @Test
    fun formWidgetInfo_textField_createInstance() {
        val widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD
        val widgetIndex = 0
        val widgetRect = Rect(0, 0, 100, 50)
        val textValue = "Test Text"
        val accessibilityLabel = "Test Label"
        val readOnly = false
        val editableText = false
        val multiLineText = true
        val maxLength = 10
        val fontSize = 12.0f

        val widgetInfo =
            FormWidgetInfo(
                widgetType,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                editableText,
                multiLineText = multiLineText,
                maxLength = maxLength,
                fontSize = fontSize,
            )

        assertEquals(widgetType, widgetInfo.widgetType)
        assertEquals(widgetIndex, widgetInfo.widgetIndex)
        assertEquals(widgetRect, widgetInfo.widgetRect)
        assertEquals(textValue, widgetInfo.textValue)
        assertEquals(accessibilityLabel, widgetInfo.accessibilityLabel)
        assertEquals(readOnly, widgetInfo.readOnly)
        assertEquals(editableText, widgetInfo.editableText)
        assertFalse(widgetInfo.multiSelect)
        assertEquals(multiLineText, widgetInfo.multiLineText)
        assertEquals(maxLength, widgetInfo.maxLength)
        assertEquals(fontSize, widgetInfo.fontSize, 0.0f)
        assertEquals(0, widgetInfo.listItems.size)
    }

    @Test
    fun formWidgetInfo_listbox_createInstance() {
        val widgetType = FormWidgetInfo.WIDGET_TYPE_LISTBOX
        val widgetIndex = 0
        val widgetRect = Rect(0, 0, 100, 50)
        val textValue = "Test Text"
        val accessibilityLabel = "Test Label"
        val listItems = listOf(ListItem("Option 1", true), ListItem("Option 2", false))

        val widgetInfo =
            FormWidgetInfo(
                widgetType,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                listItems = listItems,
            )
        assertEquals(widgetType, widgetInfo.widgetType)
        assertEquals(widgetIndex, widgetInfo.widgetIndex)
        assertEquals(widgetRect, widgetInfo.widgetRect)
    }

    @Test
    fun formWidgetInfo_textField_writeToParcelAndCreateFromParcel_equals() {
        val widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "Combo Box Label"
        val readOnly = true
        val editableText = true
        val maxLength = 20
        val fontSize = 14.0f

        val originalWidgetInfo =
            FormWidgetInfo(
                widgetType,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                editableText,
                maxLength = maxLength,
                fontSize = fontSize,
            )

        val parcel = Parcel.obtain()
        originalWidgetInfo.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdWidgetInfo = FormWidgetInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalWidgetInfo, createdWidgetInfo)
        parcel.recycle()
    }

    @Test
    fun formWidgetInfo_radioButton_writeToParcelAndCreateFromParcel_equals() {
        val widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "TextField Label"

        val originalWidgetInfo =
            FormWidgetInfo(widgetType, widgetIndex, widgetRect, textValue, accessibilityLabel)

        val parcel = Parcel.obtain()
        originalWidgetInfo.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdWidgetInfo = FormWidgetInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalWidgetInfo, createdWidgetInfo)
        parcel.recycle()
    }

    @Test
    fun formWidgetInfo_combobox_writeToParcelAndCreateFromParcel_equals() {
        val widgetType = FormWidgetInfo.WIDGET_TYPE_COMBOBOX
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "TextField Label"
        val listItems = listOf(ListItem("Option 1", true), ListItem("Option 2", false))

        val originalWidgetInfo =
            FormWidgetInfo(
                widgetType,
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                listItems = listItems,
            )

        val parcel = Parcel.obtain()
        originalWidgetInfo.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdWidgetInfo = FormWidgetInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalWidgetInfo, createdWidgetInfo)
        parcel.recycle()
    }

    @Test
    fun formWidgetInfo_equals_sameObject_returnsTrue() {
        val widgetInfo =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertEquals(widgetInfo, widgetInfo)
    }

    @Test
    fun formWidgetInfo_equals_sameValues_returnsTrue() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetType_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetIndex_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                1,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetRect_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(10, 10, 110, 60),
                "Text",
                "Label",
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentReadOnly_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                readOnly = false,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                readOnly = true,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentTextValue_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text1",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text2",
                "Label",
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentAccessibilityLabel_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label1",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label2",
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentEditableText_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_COMBOBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                editableText = false,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_COMBOBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                editableText = true,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMultiSelect_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_LISTBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                multiSelect = false,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_LISTBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                multiSelect = true,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMultiLineText_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                multiLineText = false,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                multiLineText = true,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMaxLength_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                maxLength = 10,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                maxLength = 20,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentFontSize_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                fontSize = 12.0f,
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                fontSize = 14.0f,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentListItems_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_COMBOBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                fontSize = 10.5f,
                listItems = listOf(ListItem("Option 1", true)),
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_COMBOBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                fontSize = 10.5f,
                listItems = listOf(ListItem("Option 2", false)),
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentClass_returnsFalse() {
        val widgetInfo =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val other = "Not a FormWidgetInfo"
        assertNotEquals(widgetInfo, other)
    }

    @Test
    fun formWidgetInfo_equals_null_returnsFalse() {
        val widgetInfo =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertNotEquals(widgetInfo, null)
    }

    @Test
    fun formWidgetInfo_hashCode_equalObjects_equalHashCodes() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertEquals(widgetInfo1.hashCode(), widgetInfo2.hashCode())
    }

    @Test
    fun formWidgetInfo_hashCode_differentObjects_differentHashCodes() {
        val widgetInfo1 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        val widgetInfo2 =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                1,
                Rect(10, 10, 110, 60),
                "Other Text",
                "Other Label",
                readOnly = true,
            )
        assertNotEquals(widgetInfo1.hashCode(), widgetInfo2.hashCode())
    }

    @Test
    fun formWidgetInfo_describeContents_returnsZero() {
        val widgetInfo =
            FormWidgetInfo(
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
            )
        assertEquals(0, widgetInfo.describeContents())
    }

    @Test
    fun formWidgetInfo_newArray_returnsArrayOfCorrectSize() {
        val size = 5
        val array = FormWidgetInfo.CREATOR.newArray(size)
        assertEquals(size, array.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_editableTextNotComboboxOrTextField_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            editableText = true,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_multiSelectNotListbox_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            multiSelect = true,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_multiLineTextNotTextField_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            multiLineText = true,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_maxLengthNotTextField_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            maxLength = 10,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_fontSizeNotComboboxOrTextField_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            fontSize = 10f,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_listItemsNotComboboxOrListbox_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            listItems = listOf(ListItem("Option 1", true)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_invalidMaxLength_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            maxLength = 0,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_invalidFontSize_throwsException() {
        FormWidgetInfo(
            FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            fontSize = 0f,
        )
    }
}
