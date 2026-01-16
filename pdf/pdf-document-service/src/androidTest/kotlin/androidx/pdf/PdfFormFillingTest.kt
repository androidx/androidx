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

import android.graphics.Rect
import android.os.Build
import androidx.pdf.SandboxedPdfDocumentTest.Companion.withDocument
import androidx.pdf.SandboxedPdfDocumentTest.Companion.withEditableDocument
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotEquals
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
                ListItem(label = "Apple", isSelected = false),
                ListItem(label = "Banana", isSelected = true),
                ListItem(label = "Cherry", isSelected = false),
                ListItem(label = "Date", isSelected = false),
                ListItem(label = "Elderberry", isSelected = false),
                ListItem(label = "Fig", isSelected = false),
                ListItem(label = "Guava", isSelected = false),
                ListItem(label = "Honeydew", isSelected = false),
                ListItem(label = "Indian Fig", isSelected = false),
                ListItem(label = "Jackfruit", isSelected = false),
                ListItem(label = "Kiwi", isSelected = false),
                ListItem(label = "Lemon", isSelected = false),
                ListItem(label = "Mango", isSelected = false),
                ListItem(label = "Nectarine", isSelected = false),
                ListItem(label = "Orange", isSelected = false),
                ListItem(label = "Persimmon", isSelected = false),
                ListItem(label = "Quince", isSelected = false),
                ListItem(label = "Raspberry", isSelected = false),
                ListItem(label = "Strawberry", isSelected = false),
                ListItem(label = "Tamarind", isSelected = false),
                ListItem(label = "Ugli Fruit", isSelected = false),
                ListItem(label = "Voavanga", isSelected = false),
                ListItem(label = "Wolfberry", isSelected = false),
                ListItem(label = "Xigua", isSelected = false),
                ListItem(label = "Yangmei", isSelected = false),
                ListItem(label = "Zucchini", isSelected = false),
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
                ListItem(label = "Foo", isSelected = false),
                ListItem(label = "Bar", isSelected = false),
                ListItem(label = "Qux", isSelected = false),
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
            FormWidgetInfo.createCheckbox(
                widgetIndex = 0,
                widgetRect = Rect(135, 30, 155, 50),
                textValue = "true",
                accessibilityLabel = "readOnlyCheckbox",
                isReadOnly = true,
            )

        val checkBox =
            FormWidgetInfo.createCheckbox(
                widgetIndex = 1,
                widgetRect = Rect(135, 70, 155, 90),
                textValue = "false",
                accessibilityLabel = "checkbox",
                isReadOnly = false,
            )

        verifyFormWidgetInfos(
            CLICK_FORM,
            0,
            PdfDocument.FORM_WIDGET_INCLUDE_CHECKBOX_TYPE,
            listOf(readOnlyCheckBox, checkBox),
        )
    }

    @Test
    fun applyEdit_clickOnCheckBox() = runTest {
        val widgetArea = Rect(135, 70, 155, 90)
        val before =
            FormWidgetInfo.createCheckbox(
                widgetIndex = 1,
                widgetRect = widgetArea,
                textValue = "false",
                accessibilityLabel = "checkbox",
                isReadOnly = false,
            )

        val clickPoint = PdfPoint(pageNum = 0, x = 145f, y = 80f)
        val editRec = FormEditInfo.createClick(before.widgetIndex, clickPoint = clickPoint)

        val after =
            FormWidgetInfo.createCheckbox(
                widgetIndex = 1,
                widgetRect = widgetArea,
                textValue = "true",
                accessibilityLabel = "checkbox",
                isReadOnly = false,
            )

        val expectedDirtyArea: List<Rect> = listOf(widgetArea)
        verifyApplyEdit(CLICK_FORM, 0, editRec, before, after, expectedDirtyArea)
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
        val clickPoint = PdfPoint(pageNum = 0, x = 95f, y = 240f)
        val click = FormEditInfo.createClick(widgetIndex = 5, clickPoint = clickPoint)
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
                ListItem(label = "Foo", isSelected = false),
                ListItem(label = "Bar", isSelected = false),
                ListItem(label = "Qux", isSelected = false),
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
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 0,
                selectedIndices = intArrayOf(1),
            )
        val choicesAfter =
            listOf(
                ListItem(label = "Foo", isSelected = false),
                ListItem(label = "Bar", isSelected = true),
                ListItem(label = "Qux", isSelected = false),
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
                ListItem(label = "Foo", isSelected = false),
                ListItem(label = "Bar", isSelected = false),
                ListItem(label = "Qux", isSelected = false),
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
        val setText =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 0, text = "Gecko tail")

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
                ListItem(label = "Alberta", isSelected = false),
                ListItem(label = "British Columbia", isSelected = false),
                ListItem(label = "Manitoba", isSelected = false),
                ListItem(label = "New Brunswick", isSelected = false),
                ListItem(label = "Newfoundland and Labrador", isSelected = false),
                ListItem(label = "Nova Scotia", isSelected = false),
                ListItem(label = "Ontario", isSelected = false),
                ListItem(label = "Prince Edward Island", isSelected = false),
                ListItem(label = "Quebec", isSelected = false),
                ListItem(label = "Saskatchewan", isSelected = true),
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
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 6,
                selectedIndices = intArrayOf(0),
            )
        val choicesAfter =
            listOf(
                ListItem(label = "Alberta", isSelected = true),
                ListItem(label = "British Columbia", isSelected = false),
                ListItem(label = "Manitoba", isSelected = false),
                ListItem(label = "New Brunswick", isSelected = false),
                ListItem(label = "Newfoundland and Labrador", isSelected = false),
                ListItem(label = "Nova Scotia", isSelected = false),
                ListItem(label = "Ontario", isSelected = false),
                ListItem(label = "Prince Edward Island", isSelected = false),
                ListItem(label = "Quebec", isSelected = false),
                ListItem(label = "Saskatchewan", isSelected = false),
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

        val setText =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 0, text = "Gecko tail")

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
                ListItem(label = "Apple", isSelected = false),
                ListItem(label = "Banana", isSelected = true),
                ListItem(label = "Cherry", isSelected = false),
                ListItem(label = "Date", isSelected = false),
                ListItem(label = "Elderberry", isSelected = false),
                ListItem(label = "Fig", isSelected = false),
                ListItem(label = "Guava", isSelected = false),
                ListItem(label = "Honeydew", isSelected = false),
                ListItem(label = "Indian Fig", isSelected = false),
                ListItem(label = "Jackfruit", isSelected = false),
                ListItem(label = "Kiwi", isSelected = false),
                ListItem(label = "Lemon", isSelected = false),
                ListItem(label = "Mango", isSelected = false),
                ListItem(label = "Nectarine", isSelected = false),
                ListItem(label = "Orange", isSelected = false),
                ListItem(label = "Persimmon", isSelected = false),
                ListItem(label = "Quince", isSelected = false),
                ListItem(label = "Raspberry", isSelected = false),
                ListItem(label = "Strawberry", isSelected = false),
                ListItem(label = "Tamarind", isSelected = false),
                ListItem(label = "Ugli Fruit", isSelected = false),
                ListItem(label = "Voavanga", isSelected = false),
                ListItem(label = "Wolfberry", isSelected = false),
                ListItem(label = "Xigua", isSelected = false),
                ListItem(label = "Yangmei", isSelected = false),
                ListItem(label = "Zucchini", isSelected = false),
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
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 1,
                selectedIndices = intArrayOf(1, 2, 3),
            )
        val choicesAfter =
            listOf(
                ListItem(label = "Apple", isSelected = false),
                ListItem(label = "Banana", isSelected = true),
                ListItem(label = "Cherry", isSelected = true),
                ListItem(label = "Date", isSelected = true),
                ListItem(label = "Elderberry", isSelected = false),
                ListItem(label = "Fig", isSelected = false),
                ListItem(label = "Guava", isSelected = false),
                ListItem(label = "Honeydew", isSelected = false),
                ListItem(label = "Indian Fig", isSelected = false),
                ListItem(label = "Jackfruit", isSelected = false),
                ListItem(label = "Kiwi", isSelected = false),
                ListItem(label = "Lemon", isSelected = false),
                ListItem(label = "Mango", isSelected = false),
                ListItem(label = "Nectarine", isSelected = false),
                ListItem(label = "Orange", isSelected = false),
                ListItem(label = "Persimmon", isSelected = false),
                ListItem(label = "Quince", isSelected = false),
                ListItem(label = "Raspberry", isSelected = false),
                ListItem(label = "Strawberry", isSelected = false),
                ListItem(label = "Tamarind", isSelected = false),
                ListItem(label = "Ugli Fruit", isSelected = false),
                ListItem(label = "Voavanga", isSelected = false),
                ListItem(label = "Wolfberry", isSelected = false),
                ListItem(label = "Xigua", isSelected = false),
                ListItem(label = "Yangmei", isSelected = false),
                ListItem(label = "Zucchini", isSelected = false),
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
                ListItem(label = "Alberta", isSelected = false),
                ListItem(label = "British Columbia", isSelected = false),
                ListItem(label = "Manitoba", isSelected = false),
                ListItem(label = "New Brunswick", isSelected = false),
                ListItem(label = "Newfoundland and Labrador", isSelected = false),
                ListItem(label = "Nova Scotia", isSelected = false),
                ListItem(label = "Ontario", isSelected = false),
                ListItem(label = "Prince Edward Island", isSelected = false),
                ListItem(label = "Quebec", isSelected = false),
                ListItem(label = "Saskatchewan", isSelected = true),
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
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 6,
                selectedIndices = IntArray(0),
            )
        val choicesAfter =
            listOf(
                ListItem(label = "Alberta", isSelected = false),
                ListItem(label = "British Columbia", isSelected = false),
                ListItem(label = "Manitoba", isSelected = false),
                ListItem(label = "New Brunswick", isSelected = false),
                ListItem(label = "Newfoundland and Labrador", isSelected = false),
                ListItem(label = "Nova Scotia", isSelected = false),
                ListItem(label = "Ontario", isSelected = false),
                ListItem(label = "Prince Edward Island", isSelected = false),
                ListItem(label = "Quebec", isSelected = false),
                ListItem(label = "Saskatchewan", isSelected = false),
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
        val clearText = FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 2, text = "")
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
            FormEditInfo.createClick(
                widgetIndex = 0,
                clickPoint = PdfPoint(pageNum = 0, x = 145f, y = 40f),
            )

        verifyApplyEditThrowsException(CLICK_FORM, clickOnROCheckbox)
    }

    @Test
    fun applyEdit_clickOnReadOnlyRadioButton() = runTest {
        val clickOnRORadioButton =
            FormEditInfo.createClick(
                widgetIndex = 2,
                clickPoint = PdfPoint(pageNum = 0, x = 95f, y = 190f),
            )

        verifyApplyEditThrowsException(CLICK_FORM, clickOnRORadioButton)
    }

    @Test
    fun applyEdit_setTextOnClickTypeWidget() = runTest {
        val setTextOnCheckbox =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 1, text = "New text")

        verifyApplyEditThrowsException(CLICK_FORM, setTextOnCheckbox)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnClickTypeWidget() = runTest {
        val setChoiceOnCB =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 1,
                selectedIndices = intArrayOf(1, 2, 3),
            )

        verifyApplyEditThrowsException(CLICK_FORM, setChoiceOnCB)
    }

    @Test
    fun applyEdit_clickOnInvalidPoint() = runTest {
        val clickOnNothing =
            FormEditInfo.createClick(
                widgetIndex = 0,
                clickPoint = PdfPoint(pageNum = 0, x = 0f, y = 0f),
            )

        verifyApplyEditThrowsException(CLICK_FORM, clickOnNothing)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnReadOnlyCombobox() = runTest {
        val setChoiceOnROCB =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 2,
                selectedIndices = intArrayOf(1),
            )

        verifyApplyEditThrowsException(COMBO_BOX_FORM, setChoiceOnROCB)
    }

    @Test
    fun applyEdit_setInvalidChoiceSelectionOnCombobox() = runTest {
        val setBadChoice =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 1,
                selectedIndices = intArrayOf(100, 365, 1436),
            )

        verifyApplyEditThrowsException(COMBO_BOX_FORM, setBadChoice)
    }

    @Test
    fun applyEdit_setTextOnReadOnlyCombobox() = runTest {
        val setTextOnROCB =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 2, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, setTextOnROCB)
    }

    @Test
    fun applyEdit_setTextOnUneditableCombobox() = runTest {
        val setTextOnUneditableCB =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, setTextOnUneditableCB)
    }

    @Test
    fun applyEdit_clickOnCombobox() = runTest {
        val clickOnCB =
            FormEditInfo.createClick(
                widgetIndex = 1,
                clickPoint = PdfPoint(pageNum = 0, x = 150f, y = 185f),
            )

        verifyApplyEditThrowsException(COMBO_BOX_FORM, clickOnCB)
    }

    // applyEdit edge cases - listbox
    @Test
    fun applyEdit_setMultipleChoiceSelectionOnSingleSelectionListbox() = runTest {
        val pickMultipleOnSingleChoice =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 0,
                selectedIndices = intArrayOf(1, 2),
            )

        verifyApplyEditThrowsException(LIST_BOX_FORM, pickMultipleOnSingleChoice)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnReadOnlyListbox() = runTest {
        val setChoiceOnROLB =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 2,
                selectedIndices = intArrayOf(1),
            )

        verifyApplyEditThrowsException(LIST_BOX_FORM, setChoiceOnROLB)
    }

    @Test
    fun applyEdit_clickOnListbox() = runTest {
        val clickOnLB =
            FormEditInfo.createClick(
                widgetIndex = 1,
                clickPoint = PdfPoint(pageNum = 0, x = 150f, y = 235f),
            )

        verifyApplyEditThrowsException(LIST_BOX_FORM, clickOnLB)
    }

    @Test
    fun applyEdit_setTextOnListbox() = runTest {
        val setTextOnLB =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(COMBO_BOX_FORM, setTextOnLB)
    }

    @Test
    fun getFormWidgetInfo_assertNoWidgetsInNonFormPdf() = runTest {
        withDocument("sample.pdf") { document ->
            val widgetInfos = document.getFormWidgetInfos(0)
            assertThat(widgetInfos).hasSize(0)
        }
    }

    @Test
    fun applyEdit_setTextOnReadOnlyTextField() = runTest {
        val setTextOnROTF =
            FormEditInfo.createSetText(pageNumber = 0, widgetIndex = 1, text = "new text")

        verifyApplyEditThrowsException(TEXT_FORM, setTextOnROTF)
    }

    @Test
    fun applyEdit_clickOnTextField() = runTest {
        val clickOnTF =
            FormEditInfo.createClick(
                widgetIndex = 1,
                clickPoint = PdfPoint(pageNum = 0, x = 150f, y = 185f),
            )

        verifyApplyEditThrowsException(TEXT_FORM, clickOnTF)
    }

    @Test
    fun applyEdit_setChoiceSelectionOnTextField() = runTest {
        val setChoiceOnTF =
            FormEditInfo.createSetIndices(
                pageNumber = 0,
                widgetIndex = 1,
                selectedIndices = intArrayOf(1, 2, 3),
            )

        verifyApplyEditThrowsException(TEXT_FORM, setChoiceOnTF)
    }

    @Test
    fun pdfContentInvalidatedListener_calledOnCorrectExecutorThread() = runTest {
        withEditableDocument(CLICK_FORM) { document ->
            var listenerThread: Thread? = null
            val listenerLatch = CountDownLatch(1)

            val callingThread: Thread = Thread.currentThread()
            val customThreadName = "CustomThread"

            val customExecutor =
                Executors.newSingleThreadExecutor { command -> Thread(command, customThreadName) }

            document.addOnPdfContentInvalidatedListener(
                customExecutor,
                object : PdfDocument.OnPdfContentInvalidatedListener {
                    override fun onPdfContentInvalidated(pageNumber: Int, dirtyAreas: List<Rect>) {
                        listenerThread = Thread.currentThread()
                        listenerLatch.countDown()
                    }
                },
            )
            val clickPoint = PdfPoint(pageNum = 0, x = 145f, y = 80f)
            val editRec = FormEditInfo.createClick(1, clickPoint = clickPoint)

            document.applyEdit(editRec)

            assertTrue(listenerLatch.await(5, TimeUnit.SECONDS))
            assertNotEquals(callingThread, listenerThread)
            assertEquals(listenerThread?.name, customThreadName)
            customExecutor.shutdown()
        }
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
                    document.getPageInfo(pageNum, PdfDocument.PAGE_INFO_INCLUDE_FORM_WIDGET)
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
            types: Long,
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
            editRecord: FormEditInfo,
            before: FormWidgetInfo,
            after: FormWidgetInfo,
            expectedDirtyArea: List<Rect>,
        ) {
            withEditableDocument(fileName) { document ->
                document.addOnPdfContentInvalidatedListener(
                    { command -> command.run() },
                    object : PdfDocument.OnPdfContentInvalidatedListener {
                        override fun onPdfContentInvalidated(
                            pageNumber: Int,
                            dirtyAreas: List<Rect>,
                        ) {
                            assertThat(fullyContains(expectedDirtyArea, dirtyAreas)).isTrue()
                        }
                    },
                )
                val formWidgetInfos =
                    document.getFormWidgetInfos(pageNum, (1 shl before.widgetType).toLong())
                for (i in 0..formWidgetInfos.size - 1) {
                    if (formWidgetInfos[i].widgetIndex == before.widgetIndex) {
                        assertEquals(formWidgetInfos[i], before)
                    }
                }

                document.applyEdit(editRecord)

                val actualFormWidgetInfos =
                    document.getFormWidgetInfos(pageNum, (1 shl before.widgetType).toLong())
                for (i in 0..actualFormWidgetInfos.size - 1) {
                    if (actualFormWidgetInfos[i].widgetIndex == after.widgetIndex) {
                        assertEquals(actualFormWidgetInfos[i], after)
                    }
                }
            }
        }

        /**
         * Returns true if every rect in [innerRects] is contained by at least one rect in
         * [outerRects].
         */
        private fun fullyContains(innerRects: List<Rect>, outerRects: List<Rect>): Boolean {
            return innerRects.all { inner -> outerRects.any { outer -> outer.contains(inner) } }
        }

        private suspend fun verifyApplyEditThrowsException(
            fileName: String,
            editRecord: FormEditInfo,
        ) {
            withDocument(fileName) { document ->
                document as EditablePdfDocument
                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking { document.applyEdit(editRecord) }
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
            return FormWidgetInfo.createComboBox(
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                isReadOnly = readOnly,
                isEditableText = editableText,
                fontSize = fontSize.takeIf { it > 0 } ?: 0f,
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
            return FormWidgetInfo.createRadioButton(
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                isReadOnly = readOnly,
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
            return FormWidgetInfo.createListBox(
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                isReadOnly = readOnly,
                isMultiSelect = multiSelect,
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
            return FormWidgetInfo.createTextField(
                widgetIndex = widgetIndex,
                widgetRect = widgetRect,
                textValue = textValue,
                accessibilityLabel = accessibilityLabel,
                isReadOnly = readOnly,
                isEditableText = editableText,
                isMultiLineText = multiLineText,
                maxLength = maxLength.takeIf { it >= 0 } ?: 0, // Only include if > 0
                fontSize = fontSize.takeIf { it > 0 } ?: 0f, // Only include if > 0
            )
        }
    }
}
