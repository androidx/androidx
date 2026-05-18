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

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.Range
import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.EditText
import androidx.autofill.HintConstants
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormEditInfo.Companion.createSetText
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.view.FormFillingEditText
import androidx.pdf.view.FormWidgetInteractionHandler
import androidx.pdf.view.PdfFormFillingState
import androidx.pdf.view.PdfView
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfAutofillHandlerTest {

    private lateinit var context: Context
    private lateinit var view: PdfView
    private lateinit var autofillManager: AutofillManager
    private lateinit var handler: PdfAutofillHandler

    @Before
    fun setup() {
        context = spy(RuntimeEnvironment.getApplication())
        autofillManager = mock<AutofillManager>()
        doReturn(autofillManager).whenever(context).getSystemService(AutofillManager::class.java)

        view = spy(PdfView(context))
        handler = PdfAutofillHandler(view, { point -> PointF(point.x, point.y) })
    }

    @Test
    fun onWidgetInteractionStarted_notifiesAutofillManager() {
        val text = "text"
        val widgetInfo = createTextField(text)
        val virtualId = getVirtualFormWidgetId(PAGE_INDEX, WIDGET_INDEX)

        handler.interactionListener.onWidgetInteractionStarted(virtualId, widgetInfo)

        verify(autofillManager).cancel()
        verify(autofillManager).notifyViewEntered(eq(view), eq(virtualId), any())
        verify(autofillManager).requestAutofill(eq(view), eq(virtualId), any())
    }

    @Test
    fun onWidgetValueChanged_notifiesAutofillManager() {
        val newText = "new text"
        val virtualId = getVirtualFormWidgetId(PAGE_INDEX, WIDGET_INDEX)
        val formEditInfo = createSetText(PAGE_INDEX, WIDGET_INDEX, newText)

        handler.interactionListener.onWidgetValueChanged(virtualId, formEditInfo)

        verify(autofillManager)
            .notifyValueChanged(eq(view), eq(virtualId), eq(AutofillValue.forText(newText)))
    }

    @Test
    fun onWidgetInteractionFinished_notifiesAutofillManager() {
        val virtualId = getVirtualFormWidgetId(PAGE_INDEX, WIDGET_INDEX)

        handler.interactionListener.onWidgetInteractionFinished(virtualId)

        verify(autofillManager).notifyViewExited(view, virtualId)
    }

    @Test
    fun onProvideVirtualStructure_populatesStructure_multipleWidgetsAndPages() {
        val text0 = "text0"
        val text1 = "text1"
        val emailHintText = "Email"
        val unknownHintText = "Unknown"

        val state =
            PdfFormFillingState(numPages = 2).apply {
                addPageFormWidgetInfos(
                    0,
                    listOf(createTextField(text = text0, index = 0), createCheckbox(index = 1)),
                )
                addPageFormWidgetInfos(1, listOf(createTextField(text = text1, index = 0)))
                addHintText(0, 0, emailHintText)
                addHintText(1, 0, unknownHintText)
            }

        val parentId = mock<AutofillId>()
        val children = List(2) { mock<ViewStructure>() }
        val structure =
            mock<ViewStructure> {
                on { autofillId } doReturn parentId
                on { addChildCount(1) } doReturn 0 doReturn 1
                on { newChild(0) } doReturn children[0]
                on { newChild(1) } doReturn children[1]
            }

        handler.onProvideVirtualStructure(structure, state, Range(0, 1))

        verify(structure, times(2)).addChildCount(1)
        children[0].verifyTextField(
            parentId = parentId,
            virtualId = getVirtualFormWidgetId(pageNumber = 0, widgetIndex = 0),
            text = text0,
            autofillHint = HintConstants.AUTOFILL_HINT_EMAIL_ADDRESS,
        )
        children[1].verifyTextField(
            parentId = parentId,
            virtualId = getVirtualFormWidgetId(pageNumber = 1, widgetIndex = 0),
            text = text1,
            hint = unknownHintText,
        )
    }

    @Test
    fun applyAutofillValues_updatesCurrentEdit() {
        val oldText = "old text"
        val newText = "new text"
        val widgetInfo = createTextField(oldText)
        val editText = EditText(context)
        val currentEdit = FormFillingEditText(editText, 12f, 0, widgetInfo)
        val virtualId = getVirtualFormWidgetId(PAGE_INDEX, WIDGET_INDEX)
        val values =
            SparseArray<AutofillValue>().apply { put(virtualId, AutofillValue.forText(newText)) }

        handler.applyAutofillValues(values, currentEdit, null)

        assertThat(editText.text.toString()).isEqualTo(newText)
    }

    @Test
    fun applyAutofillValues_notifiesInteractionHandler() = runTest {
        val newText = "new text"
        val virtualId = getVirtualFormWidgetId(PAGE_INDEX, WIDGET_INDEX)
        val interactionHandler = FormWidgetInteractionHandler(context, backgroundScope) {}
        val values =
            SparseArray<AutofillValue>().apply { put(virtualId, AutofillValue.forText(newText)) }

        val updates = mutableListOf<FormEditInfo>()
        val collectJob = launch {
            interactionHandler.formWidgetUpdates.collect { update -> updates.add(update) }
        }
        handler.applyAutofillValues(values, null, interactionHandler)

        runCurrent()

        assertThat(updates).hasSize(1)
        with(updates[0]) {
            assertThat(text).isEqualTo(newText)
            assertThat(pageNumber).isEqualTo(PAGE_INDEX)
            assertThat(widgetIndex).isEqualTo(WIDGET_INDEX)
            assertThat(type).isEqualTo(FormEditInfo.EDIT_TYPE_SET_TEXT)
        }
        collectJob.cancel()
    }

    private fun ViewStructure.verifyTextField(
        parentId: AutofillId,
        virtualId: Int,
        text: String,
        autofillHint: String? = null,
        hint: String? = null,
    ) {
        verify(this).setAutofillId(parentId, virtualId)
        verify(this).setAutofillType(View.AUTOFILL_TYPE_TEXT)
        verify(this).setAutofillValue(AutofillValue.forText(text))
        autofillHint?.let { verify(this).setAutofillHints(arrayOf(it)) }
        hint?.let { verify(this).hint = it }
    }

    private fun createTextField(
        text: String,
        index: Int = WIDGET_INDEX,
        rect: Rect = WIDGET_RECT,
    ): FormWidgetInfo {
        return FormWidgetInfo.createTextField(
            widgetIndex = index,
            widgetRect = rect,
            textValue = text,
            accessibilityLabel = "label",
            isReadOnly = false,
            isEditableText = true,
            isMultiLineText = false,
            maxLength = 100,
            fontSize = 12f,
        )
    }

    private fun createCheckbox(
        index: Int = WIDGET_INDEX,
        rect: Rect = WIDGET_RECT,
        textValue: String = "false",
    ): FormWidgetInfo {
        return FormWidgetInfo.createCheckbox(
            widgetIndex = index,
            widgetRect = rect,
            textValue = textValue,
            accessibilityLabel = "comboBox",
            isReadOnly = false,
        )
    }

    companion object {
        private const val PAGE_INDEX = 0
        private const val WIDGET_INDEX = 1
        private val WIDGET_RECT = Rect(0, 0, 50, 20)
    }
}
