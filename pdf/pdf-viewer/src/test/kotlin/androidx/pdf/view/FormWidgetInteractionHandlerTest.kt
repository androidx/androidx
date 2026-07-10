/*
 * Copyright 2024 The Android Open Source Project
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

import android.graphics.PointF
import android.graphics.Rect
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.ComponentActivity
import androidx.pdf.PdfPoint
import androidx.pdf.autofill.FakeFormWidgetInteractionListener
import androidx.pdf.autofill.getVirtualFormWidgetId
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class FormWidgetInteractionHandlerTest {
    private lateinit var handler: FormWidgetInteractionHandler
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope: TestScope = TestScope(testDispatcher)

    private var capturedEditText: FormFillingEditText? = null
    private val testInteractionListener = FakeFormWidgetInteractionListener()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        handler =
            FormWidgetInteractionHandler(activity, testScope) { formEditText ->
                if (capturedEditText != null) {
                    activity
                        .findViewById<ViewGroup>(android.R.id.content)
                        .removeView(capturedEditText!!.editText)
                }
                capturedEditText = formEditText
                if (formEditText != null) {
                    activity
                        .findViewById<ViewGroup>(android.R.id.content)
                        .addView(formEditText.editText)
                }
            }
        handler.interactionListener = testInteractionListener
        capturedEditText = null
    }

    @Test
    fun handleInteraction_checkBoxWidget() = runTest {
        val invalidatedRectValues = mutableListOf<FormEditInfo>()
        backgroundScope.launch(testDispatcher) {
            handler.formWidgetUpdates.toList(invalidatedRectValues)
        }

        val pageNum = 1
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createCheckbox(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Hello",
                accessibilityLabel = "accessible",
                isReadOnly = false,
            )

        val expectedEditRecord =
            FormEditInfo.createClick(
                widgetIndex = widgetIndex,
                clickPoint =
                    PdfPoint(
                        pageNum,
                        formWidgetInfo.widgetRect.centerX().toFloat(),
                        formWidgetInfo.widgetRect.centerY().toFloat(),
                    ),
            )

        handler.handleInteraction(touchPoint, formWidgetInfo)
        assertThat(invalidatedRectValues.size).isEqualTo(1)
        assertThat(invalidatedRectValues[0]).isEqualTo(expectedEditRecord)
    }

    @Test
    fun handleInteraction_radioButtonWidget() = runTest {
        val formEditInfos = mutableListOf<FormEditInfo>()
        backgroundScope.launch(testDispatcher) { handler.formWidgetUpdates.toList(formEditInfos) }

        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)

        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createRadioButton(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Radio",
                accessibilityLabel = "accessible",
                isReadOnly = false,
            )
        val expectedEditRecord =
            FormEditInfo.createClick(
                widgetIndex = widgetIndex,
                clickPoint =
                    PdfPoint(
                        pageNum,
                        formWidgetInfo.widgetRect.centerX().toFloat(),
                        formWidgetInfo.widgetRect.centerY().toFloat(),
                    ),
            )

        handler.handleInteraction(touchPoint, formWidgetInfo)
        assertThat(formEditInfos.size).isEqualTo(1)
        assertThat(formEditInfos[0]).isEqualTo(expectedEditRecord)
    }

    @Test
    fun handleInteraction_pushButtonWidget() = runTest {
        val formEditInfos = mutableListOf<FormEditInfo>()
        backgroundScope.launch(testDispatcher) { handler.formWidgetUpdates.toList(formEditInfos) }

        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createPushButton(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Push",
                accessibilityLabel = "accessible",
                isReadOnly = false,
            )
        val expectedEditRecord =
            FormEditInfo.createClick(
                widgetIndex = widgetIndex,
                clickPoint =
                    PdfPoint(
                        pageNum,
                        formWidgetInfo.widgetRect.centerX().toFloat(),
                        formWidgetInfo.widgetRect.centerY().toFloat(),
                    ),
            )

        handler.handleInteraction(touchPoint, formWidgetInfo)

        assertThat(formEditInfos.size).isEqualTo(1)
        assertThat(formEditInfos[0]).isEqualTo(expectedEditRecord)
    }

    @Test
    fun handleInteraction_textFieldWidget_notifiesInteractionStarted() = runTest {
        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createTextField(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Push",
                accessibilityLabel = "accessible",
                isReadOnly = false,
                isEditableText = true,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 10f,
            )
        handler.handleInteraction(touchPoint, formWidgetInfo)
        assertThat(capturedEditText).isNotNull()

        ShadowLooper.idleMainLooper()

        assertThat(testInteractionListener.startEvent).isNotNull()
        assertThat(testInteractionListener.startEvent!!.id)
            .isEqualTo(getVirtualFormWidgetId(pageNum, widgetIndex))
    }

    @Test
    fun handleInteraction_textFieldWidget_notifiesValueChanged() = runTest {
        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createTextField(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Original",
                accessibilityLabel = "accessible",
                isReadOnly = false,
                isEditableText = true,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 10f,
            )
        handler.handleInteraction(touchPoint, formWidgetInfo)

        val newText = "New Value"
        capturedEditText?.editText?.setText(newText)
        val formEditInfo = FormEditInfo.createSetText(pageNum, widgetIndex, newText)
        ShadowLooper.idleMainLooper()

        assertThat(testInteractionListener.changeEvent).isNotNull()
        assertThat(testInteractionListener.changeEvent!!.id)
            .isEqualTo(getVirtualFormWidgetId(pageNum, widgetIndex))
        assertThat(testInteractionListener.changeEvent!!.formEditInfo).isEqualTo(formEditInfo)
    }

    @Test
    fun handleInteraction_textFieldWidget_notifiesFinishedOnActionDone() = runTest {
        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo.createTextField(
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Original",
                accessibilityLabel = "accessible",
                isReadOnly = false,
                isEditableText = true,
                isMultiLineText = false,
                maxLength = 10,
                fontSize = 10f,
            )
        handler.handleInteraction(touchPoint, formWidgetInfo)
        ShadowLooper.idleMainLooper()

        capturedEditText?.editText?.onEditorAction(EditorInfo.IME_ACTION_DONE)

        assertThat(testInteractionListener.finishEvent).isNotNull()
        assertThat(testInteractionListener.finishEvent!!.id)
            .isEqualTo(getVirtualFormWidgetId(pageNum, widgetIndex))
    }
}
