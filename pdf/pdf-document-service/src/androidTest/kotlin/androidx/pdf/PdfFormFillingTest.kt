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

import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import androidx.pdf.PdfDocument.Companion.INCLUDE_FORM_WIDGET_INFO
import androidx.pdf.SandboxedPdfDocumentTest.Companion.withDocument
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM, codeName = "VanillaIceCream")
@RunWith(AndroidJUnit4::class)
class PdfFormFillingTest {

    @Test
    fun getPageInfo_textForm_assertWidgetInfos() = runTest {
        val formWidget0 =
            makeTextField(
                widgetIndex = 0,
                widgetRect = Rect(100, 170, 200, 200),
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Text Box",
                editableText = true,
                multiLineText = false,
                maxLength = -1,
                fontSize = 12.0f,
            )

        val formWidgetInfo1 =
            makeTextField(
                widgetIndex = 1,
                widgetRect = Rect(100, 70, 200, 100),
                readOnly = true,
                textValue = "",
                accessibilityLabel = "ReadOnly",
                editableText = false,
                multiLineText = false,
                maxLength = -1,
                fontSize = 0f,
            )

        val formWidgetInfo2 =
            makeTextField(
                widgetIndex = 2,
                widgetRect = Rect(100, 225, 200, 250),
                readOnly = false,
                textValue = "Elephant",
                accessibilityLabel = "CharLimit",
                editableText = true,
                multiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )

        val formWidgetInfo3 =
            makeTextField(
                widgetIndex = 3,
                widgetRect = Rect(100, 265, 200, 290),
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Password",
                editableText = true,
                multiLineText = false,
                maxLength = -1,
                fontSize = 12.0f,
            )

        val expectedWidgetInfos =
            listOf(formWidget0, formWidgetInfo1, formWidgetInfo2, formWidgetInfo3)
        verifyFormWidgetInfos(TEXT_FORM, 0, expectedWidgetInfoList = expectedWidgetInfos)
    }

    @Test
    fun getPageInfo_comboBox_assertWidgetInfos() = runTest {
        val readOnlyComboBox =
            makeComboBox(
                widgetIndex = 2,
                widgetRect = Rect(100, 70, 200, 100),
                textValue = "",
                accessibilityLabel = "Combo_ReadOnly",
                readOnly = true,
                editableText = false,
                fontSize = 0.0f,
                listItems = emptyList(),
            )

        val combo1Choices: List<ListItem> =
            listOf(
                ListItem(label = "Apple", selected = false),
                ListItem(label = "Banana", selected = true),
                ListItem(label = "Cherry", selected = false),
                ListItem(label = "Date", selected = false),
                ListItem(label = "Elderberry", selected = false),
                ListItem(label = "Fig", selected = false),
                ListItem(label = "Guava", selected = false),
                ListItem(label = "Honeydew", selected = false),
                ListItem(label = "Indian Fig", selected = false),
                ListItem(label = "Jackfruit", selected = false),
                ListItem(label = "Kiwi", selected = false),
                ListItem(label = "Lemon", selected = false),
                ListItem(label = "Mango", selected = false),
                ListItem(label = "Nectarine", selected = false),
                ListItem(label = "Orange", selected = false),
                ListItem(label = "Persimmon", selected = false),
                ListItem(label = "Quince", selected = false),
                ListItem(label = "Raspberry", selected = false),
                ListItem(label = "Strawberry", selected = false),
                ListItem(label = "Tamarind", selected = false),
                ListItem(label = "Ugli Fruit", selected = false),
                ListItem(label = "Voavanga", selected = false),
                ListItem(label = "Wolfberry", selected = false),
                ListItem(label = "Xigua", selected = false),
                ListItem(label = "Yangmei", selected = false),
                ListItem(label = "Zucchini", selected = false),
            )

        val comboBox1 =
            makeComboBox(
                widgetIndex = 1,
                widgetRect = Rect(100, 170, 200, 200),
                textValue = "Banana",
                accessibilityLabel = "Combo1",
                readOnly = false,
                editableText = false,
                fontSize = 0.0f,
                listItems = combo1Choices,
            )

        val editableChoices =
            listOf(
                ListItem(label = "Foo", selected = false),
                ListItem(label = "Bar", selected = false),
                ListItem(label = "Qux", selected = false),
            )

        val editableComboBox =
            makeComboBox(
                widgetIndex = 0,
                widgetRect = Rect(100, 220, 200, 250),
                textValue = "",
                accessibilityLabel = "Combo_Editable",
                readOnly = false,
                editableText = true,
                fontSize = 12.0f,
                listItems = editableChoices,
            )

        val expectedWidgetInfos = listOf(editableComboBox, comboBox1, readOnlyComboBox)
        verifyFormWidgetInfos(COMBO_BOX_FORM, 0, expectedWidgetInfoList = expectedWidgetInfos)
    }

    @Test
    fun getFormWidgetInfosOfType_checkbox_inClickForm() = runTest {
        val readOnlyCheckBox =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                widgetIndex = 0,
                widgetRect = Rect(135, 30, 155, 50),
                textValue = "true",
                accessibilityLabel = "readOnlyCheckbox",
                readOnly = true,
            )

        val checkBox =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                widgetIndex = 1,
                widgetRect = Rect(135, 70, 155, 90),
                textValue = "false",
                accessibilityLabel = "checkbox",
                readOnly = false,
            )

        verifyFormWidgetInfos(
            CLICK_FORM,
            0,
            intArrayOf(FormWidgetInfo.WIDGET_TYPE_CHECKBOX),
            listOf(readOnlyCheckBox, checkBox),
        )
    }

    @Test
    fun applyEdit_clickOnCheckBox() = runTest {
        val before =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                widgetIndex = 1,
                widgetRect = Rect(135, 70, 155, 90),
                textValue = "false",
                accessibilityLabel = "checkbox",
                readOnly = false,
            )

        val clickPoint = Point(145, 80)
        val editRec = FormEditRecord(0, before.widgetIndex, clickPoint = clickPoint)

        val after =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                widgetIndex = 1,
                widgetRect = Rect(135, 70, 155, 90),
                textValue = "true",
                accessibilityLabel = "checkbox",
                readOnly = false,
            )

        verifyApplyEdit(CLICK_FORM, 0, editRec, before, after)
    }

    @Test
    fun applyEdit_clickOnRadioButton() = runTest {
        val widgetArea = Rect(85, 230, 105, 250)
        val before =
            makeRadioButton(
                widgetIndex = 5,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "false",
                accessibilityLabel = "",
            )
        val clickPoint = Point(95, 240)
        val click = FormEditRecord(pageNumber = 0, widgetIndex = 5, clickPoint = clickPoint)
        val after =
            makeRadioButton(
                widgetIndex = 5,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "true",
                accessibilityLabel = "",
            )
        val expectedDirtyArea: List<Rect> = listOf(widgetArea)
        verifyApplyEdit(CLICK_FORM, 0, click, before, after, expectedDirtyArea)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnCombobox() = runTest {
        val comboboxArea = Rect(100, 220, 200, 250)
        val choicesBefore =
            listOf(
                ListItem(label = "Foo", selected = false),
                ListItem(label = "Bar", selected = false),
                ListItem(label = "Qux", selected = false),
            )
        val widgetBefore =
            makeComboBox(
                widgetIndex = 0,
                widgetRect = comboboxArea,
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Combo_Editable",
                editableText = true,
                fontSize = 12.0f,
                listItems = choicesBefore,
            )
        val selectBar =
            FormEditRecord(pageNumber = 0, widgetIndex = 0, selectedIndices = intArrayOf(1))
        val choicesAfter =
            listOf(
                ListItem(label = "Foo", selected = false),
                ListItem(label = "Bar", selected = true),
                ListItem(label = "Qux", selected = false),
            )
        val widgetAfter =
            makeComboBox(
                widgetIndex = 0,
                widgetRect = comboboxArea,
                readOnly = false,
                textValue = "Bar",
                accessibilityLabel = "Combo_Editable",
                editableText = true,
                fontSize = 12.0f,
                listItems = choicesAfter,
            )

        verifyApplyEdit(
            COMBO_BOX_FORM,
            0,
            selectBar,
            widgetBefore,
            widgetAfter,
            listOf(comboboxArea),
        )
    }

    @Test
    fun applyEdit_setTextOnComboBox() = runTest {
        val comboboxArea = Rect(100, 220, 200, 250)
        val choicesBefore =
            listOf(
                ListItem(label = "Foo", selected = false),
                ListItem(label = "Bar", selected = false),
                ListItem(label = "Qux", selected = false),
            )
        val widgetBefore =
            makeComboBox(
                widgetIndex = 0,
                widgetRect = comboboxArea,
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Combo_Editable",
                editableText = true,
                fontSize = 12.0f,
                listItems = choicesBefore,
            )
        val setText = FormEditRecord(pageNumber = 0, widgetIndex = 0, text = "Gecko tail")

        val widgetAfter =
            makeComboBox(
                widgetIndex = 0,
                widgetRect = comboboxArea,
                readOnly = false,
                textValue = "Gecko tail",
                accessibilityLabel = "Combo_Editable",
                editableText = true,
                fontSize = 12.0f,
                listItems = choicesBefore,
            )

        verifyApplyEdit(COMBO_BOX_FORM, 0, setText, widgetBefore, widgetAfter, listOf(comboboxArea))
    }

    @Test
    fun applyEdit_setChoiceSelectionOnListbox() = runTest {
        val widgetArea = Rect(100, 470, 200, 500)
        val choicesBefore =
            listOf(
                ListItem(label = "Alberta", selected = false),
                ListItem(label = "British Columbia", selected = false),
                ListItem(label = "Manitoba", selected = false),
                ListItem(label = "New Brunswick", selected = false),
                ListItem(label = "Newfoundland and Labrador", selected = false),
                ListItem(label = "Nova Scotia", selected = false),
                ListItem(label = "Ontario", selected = false),
                ListItem(label = "Prince Edward Island", selected = false),
                ListItem(label = "Quebec", selected = false),
                ListItem(label = "Saskatchewan", selected = true),
            )
        val widgetBefore =
            makeListbox(
                widgetIndex = 6,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Saskatchewan",
                accessibilityLabel = "Listbox_SingleSelectLastSelected",
                multiSelect = false,
                listItems = choicesBefore,
            )
        val clearSelection =
            FormEditRecord(pageNumber = 0, widgetIndex = 6, selectedIndices = intArrayOf(0))
        val choicesAfter =
            listOf(
                ListItem(label = "Alberta", selected = true),
                ListItem(label = "British Columbia", selected = false),
                ListItem(label = "Manitoba", selected = false),
                ListItem(label = "New Brunswick", selected = false),
                ListItem(label = "Newfoundland and Labrador", selected = false),
                ListItem(label = "Nova Scotia", selected = false),
                ListItem(label = "Ontario", selected = false),
                ListItem(label = "Prince Edward Island", selected = false),
                ListItem(label = "Quebec", selected = false),
                ListItem(label = "Saskatchewan", selected = false),
            )
        val widgetAfter =
            makeListbox(
                widgetIndex = 6,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Alberta",
                accessibilityLabel = "Listbox_SingleSelectLastSelected",
                multiSelect = false,
                listItems = choicesAfter,
            )

        verifyApplyEdit(
            LIST_BOX_FORM,
            0,
            clearSelection,
            widgetBefore,
            widgetAfter,
            listOf(widgetArea),
        )
    }

    @Test
    fun applyEdit_setTextInTextField() = runTest {
        val widgetArea = Rect(100, 170, 200, 200)
        val widgetBefore =
            makeTextField(
                widgetIndex = 0,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Text Box",
                editableText = true,
                multiLineText = false,
                maxLength = -1,
                fontSize = 12.0f,
            )

        val setText = FormEditRecord(pageNumber = 0, widgetIndex = 0, text = "Gecko tail")

        val widgetAfter =
            makeTextField(
                widgetIndex = 0,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Gecko tail",
                accessibilityLabel = "Text Box",
                editableText = true,
                multiLineText = false,
                maxLength = -1,
                fontSize = 12.0f,
            )
        val expectedDirtyArea: List<Rect> = listOf(widgetArea)
        verifyApplyEdit(TEXT_FORM, 0, setText, widgetBefore, widgetAfter, expectedDirtyArea)
    }

    @Test
    fun applyEdit_setMultipleChoiceSelectionOnListbox() = runTest {
        val widgetArea = Rect(100, 170, 200, 200)
        val choicesBefore =
            listOf(
                ListItem(label = "Apple", selected = false),
                ListItem(label = "Banana", selected = true),
                ListItem(label = "Cherry", selected = false),
                ListItem(label = "Date", selected = false),
                ListItem(label = "Elderberry", selected = false),
                ListItem(label = "Fig", selected = false),
                ListItem(label = "Guava", selected = false),
                ListItem(label = "Honeydew", selected = false),
                ListItem(label = "Indian Fig", selected = false),
                ListItem(label = "Jackfruit", selected = false),
                ListItem(label = "Kiwi", selected = false),
                ListItem(label = "Lemon", selected = false),
                ListItem(label = "Mango", selected = false),
                ListItem(label = "Nectarine", selected = false),
                ListItem(label = "Orange", selected = false),
                ListItem(label = "Persimmon", selected = false),
                ListItem(label = "Quince", selected = false),
                ListItem(label = "Raspberry", selected = false),
                ListItem(label = "Strawberry", selected = false),
                ListItem(label = "Tamarind", selected = false),
                ListItem(label = "Ugli Fruit", selected = false),
                ListItem(label = "Voavanga", selected = false),
                ListItem(label = "Wolfberry", selected = false),
                ListItem(label = "Xigua", selected = false),
                ListItem(label = "Yangmei", selected = false),
                ListItem(label = "Zucchini", selected = false),
            )
        val widgetBefore =
            makeListbox(
                widgetIndex = 1,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Banana",
                accessibilityLabel = "Listbox_MultiSelect",
                multiSelect = true,
                listItems = choicesBefore,
            )
        val selectMultiple =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, selectedIndices = intArrayOf(1, 2, 3))
        val choicesAfter =
            listOf(
                ListItem(label = "Apple", selected = false),
                ListItem(label = "Banana", selected = true),
                ListItem(label = "Cherry", selected = true),
                ListItem(label = "Date", selected = true),
                ListItem(label = "Elderberry", selected = false),
                ListItem(label = "Fig", selected = false),
                ListItem(label = "Guava", selected = false),
                ListItem(label = "Honeydew", selected = false),
                ListItem(label = "Indian Fig", selected = false),
                ListItem(label = "Jackfruit", selected = false),
                ListItem(label = "Kiwi", selected = false),
                ListItem(label = "Lemon", selected = false),
                ListItem(label = "Mango", selected = false),
                ListItem(label = "Nectarine", selected = false),
                ListItem(label = "Orange", selected = false),
                ListItem(label = "Persimmon", selected = false),
                ListItem(label = "Quince", selected = false),
                ListItem(label = "Raspberry", selected = false),
                ListItem(label = "Strawberry", selected = false),
                ListItem(label = "Tamarind", selected = false),
                ListItem(label = "Ugli Fruit", selected = false),
                ListItem(label = "Voavanga", selected = false),
                ListItem(label = "Wolfberry", selected = false),
                ListItem(label = "Xigua", selected = false),
                ListItem(label = "Yangmei", selected = false),
                ListItem(label = "Zucchini", selected = false),
            )
        val widgetAfter =
            makeListbox(
                widgetIndex = 1,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Banana",
                accessibilityLabel = "Listbox_MultiSelect",
                multiSelect = true,
                listItems = choicesAfter,
            )

        verifyApplyEdit(
            LIST_BOX_FORM,
            0,
            selectMultiple,
            widgetBefore,
            widgetAfter,
            listOf(widgetArea),
        )
    }

    @Test
    fun applyEdit_clearSelectionOnListbox() = runTest {
        val widgetArea = Rect(100, 470, 200, 500)
        val choicesBefore =
            listOf(
                ListItem(label = "Alberta", selected = false),
                ListItem(label = "British Columbia", selected = false),
                ListItem(label = "Manitoba", selected = false),
                ListItem(label = "New Brunswick", selected = false),
                ListItem(label = "Newfoundland and Labrador", selected = false),
                ListItem(label = "Nova Scotia", selected = false),
                ListItem(label = "Ontario", selected = false),
                ListItem(label = "Prince Edward Island", selected = false),
                ListItem(label = "Quebec", selected = false),
                ListItem(label = "Saskatchewan", selected = true),
            )
        val widgetBefore =
            makeListbox(
                widgetIndex = 6,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Saskatchewan",
                accessibilityLabel = "Listbox_SingleSelectLastSelected",
                multiSelect = false,
                listItems = choicesBefore,
            )
        val clearSelection =
            FormEditRecord(pageNumber = 0, widgetIndex = 6, selectedIndices = IntArray(0))
        val choicesAfter =
            listOf(
                ListItem(label = "Alberta", selected = false),
                ListItem(label = "British Columbia", selected = false),
                ListItem(label = "Manitoba", selected = false),
                ListItem(label = "New Brunswick", selected = false),
                ListItem(label = "Newfoundland and Labrador", selected = false),
                ListItem(label = "Nova Scotia", selected = false),
                ListItem(label = "Ontario", selected = false),
                ListItem(label = "Prince Edward Island", selected = false),
                ListItem(label = "Quebec", selected = false),
                ListItem(label = "Saskatchewan", selected = false),
            )
        val widgetAfter =
            makeListbox(
                widgetIndex = 6,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "",
                accessibilityLabel = "Listbox_SingleSelectLastSelected",
                multiSelect = false,
                listItems = choicesAfter,
            )

        verifyApplyEdit(
            LIST_BOX_FORM,
            0,
            clearSelection,
            widgetBefore,
            widgetAfter,
            listOf(widgetArea),
        )
    }

    @Test
    fun applyEdit_clearTextOnTextField() = runTest {
        val widgetArea = Rect(100, 225, 200, 250)
        val widgetBefore =
            makeTextField(
                widgetIndex = 2,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "Elephant",
                accessibilityLabel = "CharLimit",
                editableText = true,
                multiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )
        val clearText = FormEditRecord(pageNumber = 0, widgetIndex = 2, text = "")
        val widgetAfter =
            makeTextField(
                widgetIndex = 2,
                widgetRect = widgetArea,
                readOnly = false,
                textValue = "",
                accessibilityLabel = "CharLimit",
                editableText = true,
                multiLineText = false,
                maxLength = 10,
                fontSize = 12.0f,
            )

        verifyApplyEdit(TEXT_FORM, 0, clearText, widgetBefore, widgetAfter, listOf(widgetArea))
    }

    @Test
    fun applyEdit_clickOnReadOnlyCheckbox() = runTest {
        val clickOnROCheckbox =
            FormEditRecord(pageNumber = 0, widgetIndex = 0, clickPoint = Point(145, 40))

        verifyApplyEditThrowsException(CLICK_FORM, 0, clickOnROCheckbox)
    }

    @Test
    fun applyEdit_clickOnReadOnlyRadioButton() = runTest {
        val clickOnRORadioButton =
            FormEditRecord(pageNumber = 0, widgetIndex = 2, clickPoint = Point(95, 190))

        verifyApplyEditThrowsException(CLICK_FORM, 0, clickOnRORadioButton)
    }

    @Test
    fun applyEdit_setTextOnClickTypeWidget() = runTest {
        val setTextOnCheckbox = FormEditRecord(pageNumber = 0, widgetIndex = 1, text = "New text")

        verifyApplyEditThrowsException(CLICK_FORM, 0, setTextOnCheckbox)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnClickTypeWidget() = runTest {
        val setChoiceOnCB =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, selectedIndices = intArrayOf(1, 2, 3))

        verifyApplyEditThrowsException(CLICK_FORM, 0, setChoiceOnCB)
    }

    @Test
    fun applyEdit_clickOnInvalidPoint() = runTest {
        val clickOnNothing =
            FormEditRecord(pageNumber = 0, widgetIndex = 0, clickPoint = Point(0, 0))

        verifyApplyEditThrowsException(CLICK_FORM, 0, clickOnNothing)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnReadOnlyCombobox() = runTest {
        val setChoiceOnROCB =
            FormEditRecord(pageNumber = 0, widgetIndex = 2, selectedIndices = intArrayOf(1))

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, setChoiceOnROCB)
    }

    @Test
    fun applyEdit_setInvalidChoiceSelectionOnCombobox() = runTest {
        val setBadChoice =
            FormEditRecord(
                pageNumber = 0,
                widgetIndex = 1,
                selectedIndices = intArrayOf(100, 365, 1436),
            )

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, setBadChoice)
    }

    @Test
    fun applyEdit_setTextOnReadOnlyCombobox() = runTest {
        val setTextOnROCB = FormEditRecord(pageNumber = 0, widgetIndex = 2, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, setTextOnROCB)
    }

    @Test
    fun applyEdit_setTextOnUneditableCombobox() = runTest {
        val setTextOnUneditableCB =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, setTextOnUneditableCB)
    }

    @Test
    fun applyEdit_clickOnCombobox() = runTest {
        val clickOnCB =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, clickPoint = Point(150, 185))

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, clickOnCB)
    }

    // applyEdit edge cases - listbox
    @Test
    fun applyEdit_setMultipleChoiceSelectionOnSingleSelectionListbox() = runTest {
        val pickMultipleOnSingleChoice =
            FormEditRecord(pageNumber = 0, widgetIndex = 0, selectedIndices = intArrayOf(1, 2))

        verifyApplyEditThrowsException(LIST_BOX_FORM, 0, pickMultipleOnSingleChoice)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnReadOnlyListbox() = runTest {
        val setChoiceOnROLB =
            FormEditRecord(pageNumber = 0, widgetIndex = 2, selectedIndices = intArrayOf(1))

        verifyApplyEditThrowsException(LIST_BOX_FORM, 0, setChoiceOnROLB)
    }

    @Test
    fun applyEdit_clickOnListbox() = runTest {
        val clickOnLB =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, clickPoint = Point(150, 235))

        verifyApplyEditThrowsException(LIST_BOX_FORM, 0, clickOnLB)
    }

    @Test
    fun applyEdit_setTextOnListbox() = runTest {
        val setTextOnLB = FormEditRecord(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, 0, setTextOnLB)
    }

    @Test
    fun getFormWidgetInfo_assertNoWidgetsInNonFormPdf() = runTest {
        withDocument("sample.pdf") { document ->
            val widgetInfos = document.getFormWidgetInfos(0, intArrayOf())
            assertThat(widgetInfos).hasSize(0)
        }
    }

    @Test
    fun applyEdit_setTextOnReadOnlyTextField() = runTest {
        val setTextOnROTF = FormEditRecord(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(TEXT_FORM, 0, setTextOnROTF)
    }

    @Test
    fun applyEdit_clickOnTextField() = runTest {
        val clickOnTF =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, clickPoint = Point(150, 185))

        verifyApplyEditThrowsException(TEXT_FORM, 0, clickOnTF)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnTextField() = runTest {
        val setChoiceOnTF =
            FormEditRecord(pageNumber = 0, widgetIndex = 1, selectedIndices = intArrayOf(1, 2, 3))

        verifyApplyEditThrowsException(TEXT_FORM, 0, setChoiceOnTF)
    }

    companion object {
        private const val CLICK_FORM = "click_form.pdf"
        private const val TEXT_FORM = "text_form.pdf"
        private const val LIST_BOX_FORM = "listbox_form.pdf"
        private const val COMBO_BOX_FORM = "combobox_form.pdf"

        private suspend fun verifyFormWidgetInfos(
            fileName: String,
            pageNum: Int,
            expectedWidgetInfoList: List<FormWidgetInfo>,
        ) {
            withDocument(fileName) { document ->
                val actualFormWidgetInfos =
                    document.getPageInfo(
                        pageNum,
                        PdfDocument.PageInfoFlags.of(INCLUDE_FORM_WIDGET_INFO),
                    )
                assertThat(actualFormWidgetInfos.formWidgetInfos)
                    .hasSize(expectedWidgetInfoList.size)
                for (i in 0..expectedWidgetInfoList.size - 1) {
                    assertEquals(
                        actualFormWidgetInfos.formWidgetInfos!![i],
                        expectedWidgetInfoList[i],
                    )
                }
            }
        }

        private suspend fun verifyFormWidgetInfos(
            fileName: String,
            pageNum: Int,
            types: IntArray = intArrayOf(),
            expectedWidgetInfoList: List<FormWidgetInfo>,
        ) {
            withDocument(fileName) { document ->
                val actualFormWidgetInfos = document.getFormWidgetInfos(pageNum, types)
                assertThat(actualFormWidgetInfos).hasSize(expectedWidgetInfoList.size)
                for (i in 0..expectedWidgetInfoList.size - 1) {
                    assertEquals(actualFormWidgetInfos[i], expectedWidgetInfoList[i])
                }
            }
        }

        private suspend fun verifyApplyEdit(
            fileName: String,
            pageNum: Int,
            editRecord: FormEditRecord,
            before: FormWidgetInfo,
            after: FormWidgetInfo,
            expectedDirtyArea: List<Rect>? = null,
        ) {
            withDocument(fileName) { document ->
                val formWidgetInfos =
                    document.getFormWidgetInfos(pageNum, intArrayOf(before.widgetType))
                for (i in 0..formWidgetInfos.size - 1) {
                    if (formWidgetInfos[i].widgetIndex == before.widgetIndex) {
                        assertEquals(formWidgetInfos[i], before)
                    }
                }

                document.applyEdit(pageNum, editRecord)

                val actualFormWidgetInfos =
                    document.getFormWidgetInfos(pageNum, intArrayOf(before.widgetType))
                for (i in 0..actualFormWidgetInfos.size - 1) {
                    if (actualFormWidgetInfos[i].widgetIndex == after.widgetIndex) {
                        assertEquals(actualFormWidgetInfos[i], after)
                    }
                }
            }
        }

        private suspend fun verifyApplyEditThrowsException(
            fileName: String,
            pageNum: Int,
            editRecord: FormEditRecord,
        ) {
            withDocument(fileName) { document ->
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking { document.applyEdit(pageNum, editRecord) }
                }
            }
        }

        private fun makeComboBox(
            widgetIndex: Int,
            widgetRect: Rect,
            textValue: String,
            accessibilityLabel: String,
            readOnly: Boolean,
            editableText: Boolean,
            fontSize: Float,
            listItems: List<ListItem>,
        ): FormWidgetInfo {
            return FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_COMBOBOX,
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                readOnly = readOnly,
                editableText = editableText,
                fontSize = fontSize.takeIf { it > 0 },
                listItems = listItems,
            )
        }

        private fun makeRadioButton(
            widgetIndex: Int,
            widgetRect: Rect,
            readOnly: Boolean,
            textValue: String,
            accessibilityLabel: String,
        ): FormWidgetInfo {
            return FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                readOnly = readOnly,
            )
        }

        private fun makeListbox(
            widgetIndex: Int,
            widgetRect: Rect,
            readOnly: Boolean,
            textValue: String,
            accessibilityLabel: String,
            multiSelect: Boolean,
            listItems: List<ListItem>,
        ): FormWidgetInfo {
            return FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_LISTBOX,
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                readOnly = readOnly,
                multiSelect = multiSelect,
                listItems = listItems,
            )
        }

        private fun makeTextField(
            widgetIndex: Int,
            widgetRect: Rect,
            readOnly: Boolean,
            textValue: String,
            accessibilityLabel: String,
            editableText: Boolean,
            multiLineText: Boolean,
            maxLength: Int,
            fontSize: Float,
        ): FormWidgetInfo {
            return FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                readOnly = readOnly,
                editableText = editableText,
                multiLineText = multiLineText,
                maxLength = maxLength.takeIf { it >= 0 }, // Only include if > 0
                fontSize = fontSize.takeIf { it > 0 }, // Only include if > 0
            )
        }
    }
}
