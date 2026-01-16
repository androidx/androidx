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
@org.robolectric.annotation.Config(
    manifest = Config.NONE,
    sdk = [org.robolectric.annotation.Config.TARGET_SDK],
)
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
            FormWidgetInfo.createTextField(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                editableText,
                isMultiLineText = multiLineText,
                maxLength = maxLength,
                fontSize = fontSize,
            )

        assertEquals(widgetType, widgetInfo.widgetType)
        assertEquals(widgetIndex, widgetInfo.widgetIndex)
        assertEquals(widgetRect, widgetInfo.widgetRect)
        assertEquals(textValue, widgetInfo.textValue)
        assertEquals(accessibilityLabel, widgetInfo.accessibilityLabel)
        assertEquals(readOnly, widgetInfo.isReadOnly)
        assertEquals(editableText, widgetInfo.isEditableText)
        assertFalse(widgetInfo.isMultiSelect)
        assertEquals(multiLineText, widgetInfo.isMultiLineText)
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
            FormWidgetInfo.createListBox(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                false,
                isMultiSelect = false,
                listItems = listItems,
            )
        assertEquals(widgetType, widgetInfo.widgetType)
        assertEquals(widgetIndex, widgetInfo.widgetIndex)
        assertEquals(widgetRect, widgetInfo.widgetRect)
    }

    @Test
    fun formWidgetInfo_textField_writeToParcelAndCreateFromParcel_equals() {
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "Combo Box Label"
        val readOnly = true
        val editableText = true
        val multiLineText = false
        val maxLength = 20
        val fontSize = 14.0f

        val originalWidgetInfo =
            FormWidgetInfo.createTextField(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                editableText,
                multiLineText,
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
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "TextField Label"
        val readOnly = true
        val editableText = true
        val multiLineText = false
        val maxLength = 20
        val fontSize = 14.0f

        val originalWidgetInfo =
            FormWidgetInfo.createTextField(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                editableText,
                multiLineText,
                maxLength,
                fontSize,
            )

        val parcel = Parcel.obtain()
        originalWidgetInfo.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val createdWidgetInfo = FormWidgetInfo.CREATOR.createFromParcel(parcel)

        assertEquals(originalWidgetInfo, createdWidgetInfo)
        parcel.recycle()
    }

    @Test
    fun formWidgetInfo_combobox_writeToParcelAndCreateFromParcel_equals() {
        val widgetIndex = 1
        val widgetRect = Rect(10, 10, 110, 60)
        val textValue = "Initial Text"
        val accessibilityLabel = "TextField Label"
        val listItems = listOf(ListItem("Option 1", true), ListItem("Option 2", false))
        val readOnly = false
        val isEditableText = true
        val fontSize = 10f

        val originalWidgetInfo =
            FormWidgetInfo.createComboBox(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                readOnly,
                isEditableText,
                fontSize,
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
        val widgetIndex = 0
        val widgetRect = Rect(0, 0, 100, 50)
        val textValue = "Test Text"
        val accessibilityLabel = "Test Label"
        val listItems = listOf(ListItem("Option 1", true), ListItem("Option 2", false))

        val widgetInfo =
            FormWidgetInfo.createListBox(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                false,
                isMultiSelect = false,
                listItems = listItems,
            )

        assertEquals(widgetInfo, widgetInfo)
    }

    @Test
    fun formWidgetInfo_equals_sameValues_returnsTrue() {
        val widgetIndex = 0
        val widgetRect = Rect(0, 0, 100, 50)
        val textValue = "Test Text"
        val accessibilityLabel = "Test Label"
        val listItems = listOf(ListItem("Option 1", true), ListItem("Option 2", false))

        val widgetInfo1 =
            FormWidgetInfo.createListBox(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                false,
                isMultiSelect = false,
                listItems = listItems,
            )
        val widgetInfo2 =
            FormWidgetInfo.createListBox(
                widgetIndex,
                widgetRect,
                textValue,
                accessibilityLabel,
                false,
                isMultiSelect = false,
                listItems = listItems,
            )
        assertEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetType_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createRadioButton(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetIndex_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(1, Rect(0, 0, 100, 50), "Text", "Label", false)
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentWidgetRect_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(0, Rect(10, 10, 110, 60), "Text", "Label", false)
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentReadOnly_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
            )
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = true,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentTextValue_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text1", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text2", "Label", false)
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentAccessibilityLabel_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label1", false)
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label2", false)
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentEditableText_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createComboBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                fontSize = 10f,
                listItems = emptyList(),
            )
        val widgetInfo2 =
            FormWidgetInfo.createComboBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                false,
                isEditableText = true,
                fontSize = 10f,
                listItems = emptyList(),
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMultiSelect_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createListBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isMultiSelect = false,
                listItems = listOf(ListItem("Apple", false)),
            )
        val widgetInfo2 =
            FormWidgetInfo.createListBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isMultiSelect = true,
                listItems = listOf(ListItem("Apple", false)),
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMultiLineText_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = true,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )
        val widgetInfo2 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = true,
                isMultiLineText = true,
                maxLength = 10,
                fontSize = 12.0f,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentMaxLength_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )
        val widgetInfo2 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                isMultiLineText = false,
                maxLength = 20,
                fontSize = 12.0f,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentFontSize_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )
        val widgetInfo2 =
            FormWidgetInfo.createTextField(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 14.0f,
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentListItems_returnsFalse() {
        val widgetInfo1 =
            FormWidgetInfo.createComboBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                fontSize = 10.5f,
                listItems = listOf(ListItem("Option 1", true)),
            )
        val widgetInfo2 =
            FormWidgetInfo.createComboBox(
                0,
                Rect(0, 0, 100, 50),
                "Text",
                "Label",
                isReadOnly = false,
                isEditableText = false,
                fontSize = 10.5f,
                listItems = listOf(ListItem("Option 2", false)),
            )
        assertNotEquals(widgetInfo1, widgetInfo2)
    }

    @Test
    fun formWidgetInfo_equals_differentClass_returnsFalse() {
        val widgetInfo =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val other = "Not a FormWidgetInfo"
        assertNotEquals(widgetInfo, other)
    }

    @Test
    fun formWidgetInfo_equals_null_returnsFalse() {
        val widgetInfo =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        assertNotEquals(widgetInfo, null)
    }

    @Test
    fun formWidgetInfo_hashCode_equalObjects_equalHashCodes() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        assertEquals(widgetInfo1.hashCode(), widgetInfo2.hashCode())
    }

    @Test
    fun formWidgetInfo_hashCode_differentObjects_differentHashCodes() {
        val widgetInfo1 =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        val widgetInfo2 =
            FormWidgetInfo.createRadioButton(
                1,
                Rect(10, 10, 110, 60),
                "Other Text",
                "Other Label",
                isReadOnly = true,
            )
        assertNotEquals(widgetInfo1.hashCode(), widgetInfo2.hashCode())
    }

    @Test
    fun formWidgetInfo_describeContents_returnsZero() {
        val widgetInfo =
            FormWidgetInfo.createCheckbox(0, Rect(0, 0, 100, 50), "Text", "Label", false)
        assertEquals(0, widgetInfo.describeContents())
    }

    @Test
    fun formWidgetInfo_newArray_returnsArrayOfCorrectSize() {
        val size = 5
        val array = FormWidgetInfo.CREATOR.newArray(size)
        assertEquals(size, array.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_invalidMaxLength_throwsException() {
        FormWidgetInfo.createTextField(
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            isReadOnly = false,
            isEditableText = false,
            isMultiLineText = false,
            maxLength = -10,
            fontSize = 1f,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun formWidgetInfo_invalidFontSize_throwsException() {
        FormWidgetInfo.createTextField(
            0,
            Rect(0, 0, 100, 50),
            "Text",
            "Label",
            isReadOnly = false,
            isEditableText = false,
            isMultiLineText = false,
            maxLength = 10,
            fontSize = -1f,
        )
    }
}
