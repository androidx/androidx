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

package androidx.pdf.view

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Handles user interaction with different types of form widgets in a PDF document and assembles a
 * [FormEditInfo] based on the interaction. The class also handles responsibilities like creating
 * drop-down menus or creating text fields for user input.
 */
internal class FormWidgetInteractionHandler(
    private val context: Context,
    private val backgroundScope: CoroutineScope,
    private val placeTextInputInLayout: (FormFillingEditText?) -> Unit,
) {
    private val formFillingTextInputFactory = FormFillingTextInputFactory(context)

    internal val formWidgetUpdates: SharedFlow<FormEditInfo>
        get() = _formWidgetUpdates

    private val _formWidgetUpdates = MutableSharedFlow<FormEditInfo>()

    /** Entry point to handle interaction with the formWidget. */
    fun handleInteraction(touchPoint: PdfPoint, formWidgetInfo: FormWidgetInfo) {
        if (formWidgetInfo.isReadOnly) return

        val pageNum = touchPoint.pageNum
        // switch case to delegate to the appropriate handler
        when (formWidgetInfo.widgetType) {
            FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
            FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON -> {
                handleInteractionWithClickTypeWidget(touchPoint, formWidgetInfo)
            }

            FormWidgetInfo.WIDGET_TYPE_TEXTFIELD -> {
                handleInteractionWithTextWidget(pageNum, formWidgetInfo)
            }

            FormWidgetInfo.WIDGET_TYPE_COMBOBOX -> {
                handleInteractionWithComboBox(pageNum, formWidgetInfo)
            }

            FormWidgetInfo.WIDGET_TYPE_LISTBOX -> {
                handleInteractionWithChoiceSelectionWidget(pageNum, formWidgetInfo)
            }
        }
    }

    /** Implements logic to take user input in a click-type widget. */
    private fun handleInteractionWithClickTypeWidget(
        clickPoint: PdfPoint,
        formWidgetInfo: FormWidgetInfo,
    ) {
        val formEditInfo =
            FormEditInfo.createClick(
                formWidgetInfo.widgetIndex,
                clickPoint =
                    PdfPoint(
                        clickPoint.pageNum,
                        formWidgetInfo.widgetRect.centerX().toFloat(),
                        formWidgetInfo.widgetRect.centerY().toFloat(),
                    ),
            )
        relayFormEditInfo(formEditInfo)
    }

    /** Implements logic to take user input in a text field. Once done assembles a FormEditInfo */
    fun handleInteractionWithTextWidget(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
        startingText: String? = null,
    ) {
        val formFillingEditText = configureEditText(pageNum, formWidgetInfo, startingText)
        placeTextInputInLayout.invoke(formFillingEditText)
    }

    private fun configureEditText(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
        startingText: String? = null,
    ): FormFillingEditText {
        val formFillingEditText =
            formFillingTextInputFactory.makeEditText(pageNum, formWidgetInfo, startingText) {
                currentText ->
                createAndRelayEditTextInfo(pageNum, formWidgetInfo.widgetIndex, currentText)
            }
        formFillingEditText.let {
            val editText = it.editText
            editText.setOnEditorActionListener { _, actionId: Int, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    finishTextEditing(formFillingEditText)
                }
                false
            }
        }
        return formFillingEditText
    }

    private fun hideKeyboard(editText: EditText) {
        val imm: InputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    fun finishTextEditing(formFillingEditText: FormFillingEditText) {
        formFillingEditText.editText.clearFocus()
        hideKeyboard(formFillingEditText.editText)
        placeTextInputInLayout(null)
    }

    fun createAndRelayEditTextInfo(pageNum: Int, widgetIndex: Int, text: String) {
        FormEditInfo.createSetText(pageNum, widgetIndex, text).also { relayFormEditInfo(it) }
    }

    private fun handleInteractionWithComboBox(pageNum: Int, formWidgetInfo: FormWidgetInfo) {
        showSingleChoiceSelectMenu(
            pageNum,
            formWidgetInfo,
            showCustomOption = formWidgetInfo.isEditableText,
        )
    }

    /**
     * Creates a drop-down menu with a list of options. Once option is selected by the user,
     * assembles a FormEditInfo
     */
    private fun handleInteractionWithChoiceSelectionWidget(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
    ) {
        if (formWidgetInfo.isMultiSelect) {
            showMultiChoiceSelectMenu(pageNum, formWidgetInfo)
        } else {
            showSingleChoiceSelectMenu(pageNum, formWidgetInfo)
        }
    }

    private fun showSingleChoiceSelectMenu(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
        showCustomOption: Boolean = false,
    ) {
        val listItems = formWidgetInfo.listItems
        val labels =
            if (showCustomOption) {
                listOf(context.getString(R.string.combobox_custom_option)) +
                    listItems.map { it.label }
            } else {
                listItems.map { it.label }
            }

        val initialSelection = listItems.indexOfFirst { it.isSelected }
        // Offset by 1 if "Custom" is at index 0; otherwise use the raw index.
        var selectedIndex = if (showCustomOption) initialSelection + 1 else initialSelection

        MaterialAlertDialogBuilder(context)
            .setSingleChoiceItems(labels.toTypedArray(), selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(context.getString(R.string.confirm_selection)) { dialog, _ ->
                if (showCustomOption && selectedIndex == 0) {
                    // User selected the "Custom" option
                    handleInteractionWithTextWidget(
                        pageNum,
                        formWidgetInfo,
                        startingText = formWidgetInfo.textValue,
                    )
                } else {
                    // Calculate the actual index in the original listItems
                    val actualIndex = if (showCustomOption) selectedIndex - 1 else selectedIndex
                    if (actualIndex >= 0) {
                        handleSelectedItem(pageNum, formWidgetInfo, listOf(actualIndex))
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showMultiChoiceSelectMenu(pageNum: Int, formWidgetInfo: FormWidgetInfo) {
        val selectedItems =
            BooleanArray(formWidgetInfo.listItems.size) { i ->
                formWidgetInfo.listItems[i].isSelected
            }
        val listItemValues: List<String> = formWidgetInfo.listItems.map { it.label }

        MaterialAlertDialogBuilder(context)
            .setMultiChoiceItems(listItemValues.toTypedArray(), selectedItems) {
                dialog,
                which,
                isChecked ->
                selectedItems[which] = isChecked
            }
            .setPositiveButton(context.getString(R.string.confirm_selection)) { dialog, which ->
                handleSelectedItem(
                    pageNum,
                    formWidgetInfo,
                    selectedItems.indices.filter { selectedItems[it] },
                )
                dialog.dismiss()
            }
            .show()
    }

    private fun handleSelectedItem(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
        selectedItemIndices: List<Int>,
    ) {
        val formEditInfo =
            FormEditInfo.createSetIndices(
                pageNum,
                formWidgetInfo.widgetIndex,
                selectedIndices = selectedItemIndices.toIntArray(),
            )

        relayFormEditInfo(formEditInfo)
    }

    private fun relayFormEditInfo(formEditInfo: FormEditInfo) {
        backgroundScope.launch { _formWidgetUpdates.emit(formEditInfo) }
    }
}
