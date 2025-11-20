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
import android.graphics.Color
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.pdf.R
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.view.fastscroll.getDimensions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Factory for [EditText]s to accept form filling input. */
internal class FormFillingTextInputFactory(private val context: Context) {

    val editTextBoundaryWidth: Int =
        context.getDimensions(R.dimen.form_widget_edit_text_boundary_width).roundToInt()

    /** Returns an [EditText] configured to accept input for [formWidgetInfo] */
    fun makeEditText(
        pageNum: Int,
        formWidgetInfo: FormWidgetInfo,
        startingText: String? = null,
    ): FormFillingEditText {
        require(formWidgetInfo.widgetType == FormWidgetInfo.WIDGET_TYPE_TEXTFIELD)
        return EditText(context).withFormWidget(pageNum, formWidgetInfo, startingText)
    }

    private fun EditText.withFormWidget(
        pageNum: Int,
        formWidget: FormWidgetInfo,
        startingText: String?,
    ): FormFillingEditText {
        setBackgroundResource(R.drawable.form_edit_text_background)
        setTextColor(Color.BLACK)

        setPadding(editTextBoundaryWidth, editTextBoundaryWidth, editTextBoundaryWidth, 0)

        layoutParams =
            ViewGroup.LayoutParams(
                formWidget.widgetRect.width() + 2 * editTextBoundaryWidth,
                formWidget.widgetRect.height() + 2 * editTextBoundaryWidth,
            )
        this.applyLengthFilter(formWidget)
        gravity = Gravity.CENTER_VERTICAL
        inputType = configureInputType(formWidget.multiLineText)
        configureText(startingText, formWidget, this)
        imeOptions = DEFAULT_IME_OPTIONS
        return FormFillingEditText(this, textSize, pageNum, formWidget)
    }

    private fun EditText.applyLengthFilter(formWidget: FormWidgetInfo) {
        filters =
            if (formWidget.maxLength > 0) {
                arrayOf(InputFilter.LengthFilter(formWidget.maxLength))
            } else {
                arrayOf()
            }
    }

    private fun configureText(
        startingText: String?,
        formWidget: FormWidgetInfo,
        editText: EditText,
    ) {
        val currentText = startingText ?: (formWidget.textValue ?: "")

        val maxFontSize =
            max(MINIMUM_GENERAL_TEXT_FONT_SIZE_PX, formWidget.widgetRect.height().toFloat())
        val fontSize =
            if (abs(formWidget.fontSize) < AUTO_SIZE_THRESHOLD) {
                autoFontResize(maxFontSize, formWidget.multiLineText)
            } else {
                min(formWidget.fontSize, maxFontSize)
            }

        editText.apply {
            setText(currentText)
            setSelection(currentText.length)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
        }
    }

    private fun autoFontResize(maxFontSize: Float, multilineText: Boolean): Float {
        return if (!multilineText) {
            maxFontSize
        } else {
            min(maxFontSize, MULTILINE_TEXT_AUTOSIZE_MAX_FONT_SIZE_PX)
        }
    }

    private fun configureInputType(multiLineText: Boolean): Int {
        var inputTypeSettings = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        if (multiLineText) {
            inputTypeSettings =
                inputTypeSettings or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
        }
        return inputTypeSettings
    }
}

/**
 * Composes an [EditText] that's configured specifically for form filling text input together with
 * the page number the widget belongs to and the widget's own metadata.
 */
internal class FormFillingEditText(
    val editText: EditText,
    val fontSize: Float,
    val pageNum: Int,
    val formWidget: FormWidgetInfo,
)

/**
 * A specified font size of 0.0F indicates the font should be auto-sized to match the widget. We
 * compare the specified font size to a small epsilon of 1/100th of a point.
 */
internal const val AUTO_SIZE_THRESHOLD = 0.01F

/**
 * Rough minimum usable text size in px.
 *
 * Below this size some fonts may encounter issues with spacing, cursor overlap, etc.
 */
internal const val MINIMUM_GENERAL_TEXT_FONT_SIZE_PX = 6F

/** Rough maximum usable text size in px for multiline edit text widgets. */
internal const val MULTILINE_TEXT_AUTOSIZE_MAX_FONT_SIZE_PX = 16F

/**
 * Ime options control what type of soft input method is displayed when this view receives focus and
 * how it behaves.
 *
 * These options create a normal soft keyboard whose return button will be a Done button when this
 * view is showing single line text.
 */
internal const val DEFAULT_IME_OPTIONS = EditorInfo.IME_ACTION_DONE
