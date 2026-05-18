/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.autofill

import android.graphics.PointF
import android.graphics.Rect
import android.util.Range
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import androidx.core.graphics.toRectF
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.view.FormFillingEditText
import androidx.pdf.view.FormWidgetInteractionHandler
import androidx.pdf.view.PdfFormFillingState
import kotlin.math.roundToInt

/**
 * Delegate responsible for handling Android Autofill requests and interactions for PDF forms.
 *
 * It bridges the gap between the Android Autofill system and the internal PDF state, allowing the
 * system to provide and apply autofill suggestions for form fields, while also notifying the system
 * of user-initiated focus and value changes.
 */
internal class PdfAutofillHandler(
    private val view: View,
    private val pdfToViewPoint: (PdfPoint) -> PointF?,
    private val hintDetector: AutofillHintDetector = DefaultAutofillHintDetector(),
) {

    private val autofillManager: AutofillManager? by lazy {
        view.context.getSystemService(AutofillManager::class.java)
    }

    val interactionListener: FormWidgetInteractionListener =
        object : FormWidgetInteractionListener {
            override fun onWidgetInteractionStarted(virtualId: Int, widgetInfo: FormWidgetInfo) {
                val pageNum = getPageNumber(virtualId)
                val rect = PdfRect(pageNum, widgetInfo.widgetRect.toRectF()).toScreenRect()
                autofillManager?.apply {
                    cancel()
                    notifyViewEntered(view, virtualId, rect)
                    requestAutofill(view, virtualId, rect)
                }
            }

            override fun onWidgetValueChanged(virtualId: Int, formEditInfo: FormEditInfo) {
                autofillManager?.notifyValueChanged(
                    view,
                    virtualId,
                    AutofillValue.forText(formEditInfo.text),
                )
            }

            override fun onWidgetInteractionFinished(virtualId: Int) {
                autofillManager?.notifyViewExited(view, virtualId)
            }
        }

    /**
     * Populates a [android.view.ViewStructure] with the virtual hierarchy of the PDF form fields.
     *
     * @param structure The structure to populate.
     * @param state The current state of form filling in the PDF.
     */
    fun onProvideVirtualStructure(
        structure: ViewStructure,
        state: PdfFormFillingState,
        visiblePages: Range<Int>,
    ) {
        val parentId = structure.autofillId ?: return

        for (pageNum in visiblePages.lower..visiblePages.upper) {
            val widgets = state.getPageFormWidgetInfos(pageNum)

            for (widget in widgets) {
                if (widget.widgetType != FormWidgetInfo.WIDGET_TYPE_TEXTFIELD) continue
                val virtualId = getVirtualFormWidgetId(pageNum, widget.widgetIndex)

                val index = structure.addChildCount(1)
                val child = structure.newChild(index).apply { setAutofillId(parentId, virtualId) }
                populateWidgetStructure(child, widget, state, pageNum)
            }
        }
    }

    private fun populateWidgetStructure(
        structure: ViewStructure,
        widget: FormWidgetInfo,
        state: PdfFormFillingState,
        pageNum: Int,
    ) {
        val hintText = state.getHintText(pageNum, widget.widgetIndex)

        structure.apply {
            if (hintText != null) {
                val autofillHints = hintDetector.detectHints(hintText)
                if (autofillHints.isNotEmpty()) {
                    setAutofillHints(autofillHints.toTypedArray())
                } else {
                    hint = hintText
                }
            }

            setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES)
            setAutofillType(View.AUTOFILL_TYPE_TEXT)
            widget.textValue?.let { setAutofillValue(AutofillValue.forText(it)) }

            val rect = PdfRect(pageNum, widget.widgetRect.toRectF()).toViewRect()
            if (!rect.isEmpty) {
                setDimens(rect.left, rect.top, 0, 0, rect.width(), rect.height())
            }
        }
    }

    /**
     * Handles autofill requests for the PDF by applying values provided by the Autofill service.
     *
     * @param values The values provided by the Autofill service.
     * @param currentEdit The currently active edit field, if any.
     * @param interactionHandler The handler for form widget interactions.
     */
    fun applyAutofillValues(
        values: SparseArray<AutofillValue>,
        currentEdit: FormFillingEditText?,
        interactionHandler: FormWidgetInteractionHandler?,
    ) {
        for (i in 0 until values.size()) {
            val virtualId = values.keyAt(i)
            val autofillValue = values.valueAt(i)

            if (autofillValue == null || !autofillValue.isText) continue
            val text = autofillValue.textValue.toString()

            val pageNum = getPageNumber(virtualId)
            val widgetIndex = getWidgetIndex(virtualId)

            if (
                currentEdit != null &&
                    (currentEdit.pageNum == pageNum) &&
                    (currentEdit.formWidget.widgetIndex == widgetIndex)
            ) {
                currentEdit.editText.setText(text)
                currentEdit.editText.setSelection(text.length)
            } else {
                interactionHandler?.createAndRelayEditTextInfo(
                    pageNum = pageNum,
                    widgetIndex = widgetIndex,
                    text = text,
                )
            }
        }
    }

    private fun PdfRect.toScreenRect(): Rect {
        return toViewRect().apply {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            offset(location[0], location[1])
        }
    }

    private fun PdfRect.toViewRect(): Rect {
        val topLeft = pdfToViewPoint(PdfPoint(pageNum, left, top)) ?: return Rect()
        val bottomRight = pdfToViewPoint(PdfPoint(pageNum, right, bottom)) ?: return Rect()

        return Rect(
            topLeft.x.roundToInt(),
            topLeft.y.roundToInt(),
            bottomRight.x.roundToInt(),
            bottomRight.y.roundToInt(),
        )
    }
}
