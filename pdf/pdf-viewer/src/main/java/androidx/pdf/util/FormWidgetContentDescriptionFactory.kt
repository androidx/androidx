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

package androidx.pdf.util

import android.content.Context
import androidx.pdf.R
import androidx.pdf.models.FormWidgetInfo

/** Utility class for Form Widgets. */
internal class FormWidgetContentDescriptionFactory {
    companion object {
        private const val SPACE = " "

        /** Get accessibility content description for a form widget. */
        fun getContentDescription(formWidgetInfo: FormWidgetInfo, context: Context): String {
            val builder: StringBuilder = StringBuilder(context.getString(R.string.form_widget))
            builder.append(SPACE)

            val typeName = getWidgetNameFromWidgetType(formWidgetInfo.widgetType, context)
            val typeString =
                if (formWidgetInfo.multiSelect) {
                    context.getString(R.string.form_multiselect_type, typeName)
                } else {
                    context.getString(R.string.form_type, typeName)
                }
            builder.append(typeString)
            builder.append(SPACE)

            val accessibilityLabel =
                if (!formWidgetInfo.accessibilityLabel.isNullOrEmpty()) {
                    formWidgetInfo.accessibilityLabel
                } else {
                    context.getString(R.string.form_unknown)
                }
            builder.append(context.getString(R.string.form_title, accessibilityLabel))
            builder.append(SPACE)

            if (formWidgetInfo.readOnly) {
                builder.append(context.getString(R.string.form_value, getTextValue(formWidgetInfo)))
                builder.append(SPACE)
                builder.append(context.getString(R.string.form_read_only))
            } else {
                builder.append(
                    context.getString(R.string.form_current_value, getTextValue(formWidgetInfo))
                )
                builder.append(SPACE)
                val clickActionInstruction =
                    when (formWidgetInfo.widgetType) {
                        FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                        FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON,
                        FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON ->
                            context.getString(R.string.form_click_toggle_instruction)

                        FormWidgetInfo.WIDGET_TYPE_LISTBOX,
                        FormWidgetInfo.WIDGET_TYPE_COMBOBOX ->
                            context.getString(R.string.form_click_opens_choices_menu_instruction)

                        FormWidgetInfo.WIDGET_TYPE_TEXTFIELD ->
                            context.getString(R.string.form_click_enters_text_edit_mode_instruction)
                        else -> ""
                    }
                builder.append(clickActionInstruction)
            }

            return builder.toString()
        }

        private fun getWidgetNameFromWidgetType(widgetType: Int, context: Context): String {
            return when (widgetType) {
                FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON ->
                    context.getString(R.string.form_widget_type_push_button)
                FormWidgetInfo.WIDGET_TYPE_CHECKBOX ->
                    context.getString(R.string.form_widget_type_checkbox)
                FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON ->
                    context.getString(R.string.form_widget_type_radio_button)
                FormWidgetInfo.WIDGET_TYPE_COMBOBOX ->
                    context.getString(R.string.form_widget_type_combo_box)
                FormWidgetInfo.WIDGET_TYPE_LISTBOX ->
                    context.getString(R.string.form_widget_type_list_box)
                FormWidgetInfo.WIDGET_TYPE_TEXTFIELD ->
                    context.getString(R.string.form_widget_type_text_field)
                FormWidgetInfo.WIDGET_TYPE_SIGNATURE ->
                    context.getString(R.string.form_widget_type_signature)
                else -> context.getString(R.string.form_unknown)
            }
        }

        private fun getTextValue(formWidgetInfo: FormWidgetInfo): String {
            return formWidgetInfo.textValue ?: ""
        }
    }
}
