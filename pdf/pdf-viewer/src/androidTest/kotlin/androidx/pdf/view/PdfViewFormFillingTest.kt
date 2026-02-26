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

import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.models.ListItem
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressImeActionButton
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewFormFillingTest {

    private lateinit var formEditInfos: MutableList<FormEditInfo>

    @Before
    fun setUp() {
        formEditInfos = mutableListOf()
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    private fun setupPdfView(
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        fakePdfDocument: FakePdfDocument?,
        enableFormFilling: Boolean = false,
    ) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        isFormFillingEnabled = enableFormFilling
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        addOnFormWidgetInfoUpdatedListener(
                            object : PdfView.OnFormWidgetInfoUpdatedListener {
                                override fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo) {
                                    formEditInfos.add(formEditInfo)
                                }
                            }
                        )
                    },
                    ViewGroup.LayoutParams(width, height),
                )
            }
        }
    }

    @Test
    fun testInteractionWithClickTypeFormWidget() = runTest {
        assumeFalse(
            "Test fails on cuttlefish b/466080956",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
        val formWidgetRect = Rect(10, 10, 100, 100)
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(10) { Point(DEFAULT_WIDTH, DEFAULT_HEIGHT) },
                formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
                pageFormWidgetInfos =
                    mapOf(
                        0 to
                            listOf(
                                FormWidgetInfo.createRadioButton(
                                    widgetIndex = 0,
                                    widgetRect = formWidgetRect,
                                    textValue = "TextField",
                                    accessibilityLabel = "TextField",
                                    isReadOnly = false,
                                )
                            )
                    ),
            )
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = true)

        var pdfClickPoint: PdfPoint? = null
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            val viewClickPoint = PointF(25f, 25f)
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                pdfClickPoint = requireNotNull(pdfView.viewToPdfPoint(viewClickPoint))
            }

            Espresso.onView(withId(PDF_VIEW_ID))
                .perform(performSingleTapOnCoords(viewClickPoint.x, viewClickPoint.y))

            close()
        }

        pdfClickPoint = requireNotNull(pdfClickPoint)
        // Confirm that fakePdfDocument.applyEdit is called.
        assertThat(formEditInfos).hasSize(1)
        assertThat(formEditInfos[0])
            .isEqualTo(
                FormEditInfo.createClick(
                    widgetIndex = 0,
                    clickPoint =
                        PdfPoint(
                            pdfClickPoint.pageNum,
                            formWidgetRect.centerX().toFloat(),
                            formWidgetRect.centerY().toFloat(),
                        ),
                )
            )
    }

    @Test
    fun testInteractionWithComboBox_hasCustomTextInputByDefault() = runTest {
        val fakePdfDocument = getFakePdfDocumentInstance(getSampleComboBoxWidget(true))
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = true)

        val currentTextValue = fakePdfDocument.getFormWidgetInfos(0)[0].textValue
        val textToAppend = " World"
        val finalText = currentTextValue + textToAppend

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            var pdfView: PdfView? = null

            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                pdfView = view as PdfView
            }
            // Tap on the form-widget
            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(25f, 25f))

            val expectedChoices =
                listOf("Custom") +
                    fakePdfDocument.getFormWidgetInfos(0)[0].listItems.map { it.label }
            // Confirm that the expected choices are displayed.
            checkDialogBoxOptionsAndConfirmButton(expectedChoices)
            // Select the "Custom" option to enter custom text.
            Espresso.onView(withText(expectedChoices[0])).inRoot(isDialog()).perform(click())
            // Confirm the selection.
            Espresso.onView(withText(R.string.confirm_selection))
                .inRoot(isDialog())
                .perform(click())
            // EditText will be added as a child view to PdfView, create a matcher for the child
            // view.
            val editTextMatcher: (View) -> Boolean = { view ->
                view is EditText && view.text.toString() == currentTextValue && view.isShown
            }

            val childAddedIdlingResource = ChildViewAddedIdlingResource(pdfView!!, editTextMatcher)
            try {
                IdlingRegistry.getInstance().register(childAddedIdlingResource)
                Espresso.onView(withText(currentTextValue))
                    .check(ViewAssertions.matches(isDisplayed()))
                Espresso.onView(withText(currentTextValue)).perform(typeText(textToAppend))
                Espresso.onView(withText(finalText)).check(ViewAssertions.matches(isDisplayed()))
                Espresso.onView(withText(finalText)).perform(pressImeActionButton())
            } finally {
                IdlingRegistry.getInstance().unregister(childAddedIdlingResource)
            }
            close()
        }
        assertThat(formEditInfos).hasSize(textToAppend.length)
        for (i in 0..<formEditInfos.size) {
            assertThat(formEditInfos[i])
                .isEqualTo(
                    FormEditInfo.createSetText(
                        0,
                        0,
                        currentTextValue + textToAppend.substring(0, i + 1),
                    )
                )
        }
    }

    @Test
    fun testInteractionWhenClickedOnSingleChoiceTypeFormWidget() = runTest {
        val fakePdfDocument = getFakePdfDocumentInstance(getChoiceTypeFormWidgets(false))
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = true)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)

            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
            }

            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(25f, 25f))

            val choices = fakePdfDocument.getFormWidgetInfos(0)[0].listItems.map { it.label }
            // Check if a MaterialDialogBox is displayed with the
            // correct options and the positive option "Confirm"
            checkDialogBoxOptionsAndConfirmButton(choices)

            // Select the first option from the list. (Second option was selected by default)
            Espresso.onView(withText(choices[0])).inRoot(isDialog()).perform(click())

            // Perform a click on the point where the text "Confirm" is present to confirm selection
            Espresso.onView(withText(R.string.confirm_selection))
                .inRoot(isDialog())
                .perform(click())

            close()
        }
        // Confirm that fakePdfDocument.applyEdit is called.
        assertThat(formEditInfos).hasSize(1)
        assertThat(formEditInfos[0])
            .isEqualTo(
                FormEditInfo.createSetIndices(
                    pageNumber = 0,
                    widgetIndex = 0,
                    selectedIndices = IntArray(1) { 0 },
                )
            )
    }

    @Test
    fun testInteractionWhenClickedOnMultipleChoiceTypeFormWidget() = runTest {
        val fakePdfDocument = getFakePdfDocumentInstance(getChoiceTypeFormWidgets(true))
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = true)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)

            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
            }

            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(25f, 25f))

            val choices = fakePdfDocument.getFormWidgetInfos(0)[0].listItems.map { it.label }
            // Check if a MaterialDialogBox is displayed with the
            // correct options and the positive option "Confirm"
            checkDialogBoxOptionsAndConfirmButton(choices)

            // Select the first option from the list. (Second option was selected by default)
            Espresso.onView(withText(choices[0])).inRoot(isDialog()).perform(click())
            // De-select the second option from the list.
            Espresso.onView(withText(choices[1])).inRoot(isDialog()).perform(click())
            // Select the third option from the list.
            Espresso.onView(withText(choices[2])).inRoot(isDialog()).perform(click())

            // Confirm the selection
            Espresso.onView(withText(R.string.confirm_selection))
                .inRoot(isDialog())
                .perform(click())

            close()
        }
        // Confirm that fakePdfDocument.applyEdit is called.
        assertThat(formEditInfos).hasSize(1)
        assertThat(formEditInfos[0])
            .isEqualTo(
                FormEditInfo.createSetIndices(
                    pageNumber = 0,
                    widgetIndex = 0,
                    selectedIndices = intArrayOf(0, 2),
                )
            )
    }

    @SdkSuppress(maxSdkVersion = 35)
    @Test
    fun testInteractionWithTextTypeFormWidget_actionDoneAppliesEdit() = runTest {
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(2) { Point(500, 500) },
                formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
                pageFormWidgetInfos =
                    mapOf(
                        0 to
                            listOf(
                                FormWidgetInfo.createTextField(
                                    widgetIndex = 0,
                                    widgetRect = Rect(10, 10, 200, 200),
                                    textValue = "Hello",
                                    accessibilityLabel = "Hello",
                                    isReadOnly = false,
                                    isMultiLineText = false,
                                    fontSize = 10.0f,
                                    isEditableText = true,
                                    maxLength = 100,
                                )
                            )
                    ),
            )
        val textValue = fakePdfDocument.getFormWidgetInfos(0)[0].textValue
        val textToAppend = " World"
        val finalText = textValue + textToAppend
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = true)
        var pdfView: PdfView? = null
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                pdfView = view as PdfView
            }
            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(50f, 50f))
            val editTextMatcher: (View) -> Boolean = { view ->
                view is EditText && view.text.toString() == textValue && view.isShown
            }
            val childAddedIdlingResource = ChildViewAddedIdlingResource(pdfView!!, editTextMatcher)
            try {
                IdlingRegistry.getInstance().register(childAddedIdlingResource)
                Espresso.onView(withText(textValue)).check(ViewAssertions.matches(isDisplayed()))
                Espresso.onView(withText(textValue)).perform(typeText(textToAppend))
                Espresso.onView(withText(finalText)).check(ViewAssertions.matches(isDisplayed()))
                Espresso.onView(withText(finalText)).perform(pressImeActionButton())
            } finally {
                IdlingRegistry.getInstance().unregister(childAddedIdlingResource)
            }
            close()
        }
        // We get callbacks when the edit text gets updated.
        assertThat(formEditInfos).hasSize(textToAppend.length)
        for (i in 0..<formEditInfos.size) {
            assertThat(formEditInfos[i])
                .isEqualTo(
                    FormEditInfo.createSetText(0, 0, textValue + textToAppend.substring(0, i + 1))
                )
        }
    }

    @Test
    fun testReloadFormMetadataWhenFormFillingFlagChanged() = runTest {
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(2) { Point(100, 200) },
                formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
                pageFormWidgetInfos =
                    mapOf(
                        0 to
                            listOf(
                                FormWidgetInfo.createRadioButton(
                                    widgetIndex = 0,
                                    widgetRect = Rect(10, 10, 100, 100),
                                    textValue = "TextField",
                                    accessibilityLabel = "TextField",
                                    isReadOnly = false,
                                )
                            ),
                        1 to
                            listOf(
                                FormWidgetInfo.createRadioButton(
                                    widgetIndex = 0,
                                    widgetRect = Rect(10, 10, 100, 100),
                                    textValue = "true",
                                    accessibilityLabel = "Radio",
                                    isReadOnly = false,
                                )
                            ),
                    ),
            )
        setupPdfView(fakePdfDocument = fakePdfDocument, enableFormFilling = false)
        var pdfView: PdfView? = null

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                pdfView = view as PdfView
            }
            fakePdfDocument.clearFormWidgetRequests()
            pdfView?.isFormFillingEnabled = true
            fakePdfDocument.waitForFormDataFetch(1)
            close()
        }
        assertThat(fakePdfDocument.formWidgetRequests).hasSize(2)
    }

    private fun getFakePdfDocumentInstance(formWidgetInfos: List<FormWidgetInfo>): FakePdfDocument {
        return FakePdfDocument(
            pages = List(10) { Point(DEFAULT_WIDTH, DEFAULT_HEIGHT) },
            formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
            pageFormWidgetInfos = mapOf(0 to formWidgetInfos),
        )
    }

    private fun getSampleComboBoxWidget(customInput: Boolean): List<FormWidgetInfo> {
        val choicesAllUnselected =
            listOf(ListItem("Apple", false), ListItem("Banana", false), ListItem("Cherry", false))
        val widgetWithCustomInput =
            FormWidgetInfo.createComboBox(
                widgetIndex = 0,
                widgetRect = Rect(10, 10, 100, 100),
                textValue = "Android",
                accessibilityLabel = "Fruit",
                isReadOnly = false,
                isEditableText = true,
                fontSize = 10.0f,
                listItems = choicesAllUnselected,
            )

        val singleChoiceSelected =
            listOf(ListItem("Apple", false), ListItem("Banana", true), ListItem("Cherry", false))

        val widgetWithSelection =
            FormWidgetInfo.createComboBox(
                widgetIndex = 0,
                widgetRect = Rect(10, 10, 100, 100),
                textValue = "Banana",
                accessibilityLabel = "Fruit",
                isReadOnly = false,
                isEditableText = true,
                fontSize = 10.0f,
                listItems = singleChoiceSelected,
            )
        return if (customInput) {
            listOf(widgetWithCustomInput)
        } else {
            listOf(widgetWithSelection)
        }
    }

    private fun getChoiceTypeFormWidgets(multiselect: Boolean): List<FormWidgetInfo> {
        val choices =
            listOf(ListItem("Apple", false), ListItem("Banana", true), ListItem("Cherry", false))
        return listOf(
            FormWidgetInfo.createListBox(
                widgetIndex = 0,
                widgetRect = Rect(10, 10, 100, 100),
                textValue = "Banana",
                accessibilityLabel = "ListBox",
                isReadOnly = false,
                listItems = choices,
                isMultiSelect = multiselect,
            )
        )
    }

    private fun checkDialogBoxOptionsAndConfirmButton(choices: List<String>) {
        for (choice in choices) {
            Espresso.onView(withText(choice))
                .inRoot(isDialog())
                .check(ViewAssertions.matches(isDisplayed()))
        }
        Espresso.onView(withText(R.string.confirm_selection))
            .inRoot(isDialog())
            .check(ViewAssertions.matches(isDisplayed()))
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
private const val DEFAULT_WIDTH = 200
private const val DEFAULT_HEIGHT = 400
