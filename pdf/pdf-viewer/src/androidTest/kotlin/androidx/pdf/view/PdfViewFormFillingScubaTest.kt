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

package androidx.pdf.view

import android.graphics.Point
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.pdf.assertScreenshot
import androidx.pdf.models.FormEditInfo
import androidx.pdf.models.FormWidgetInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class PdfViewFormFillingScubaTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testFormWidgetClick_withPdfViewPadding_spawnsEditTextWithCorrectBounds() = runTest {
        runFormWidgetClickScreenshotTest(
            paddingRect = Rect(0, 40, 0, 0),
            goldenScreenshotId = FORM_TEXT_WIDGET_CORRECT_POSITION_WITH_PADDING,
        )
    }

    @Test
    fun testFormWidgetClick_withoutPdfViewPadding_spawnsEditTextWithCorrectBounds() = runTest {
        runFormWidgetClickScreenshotTest(
            paddingRect = Rect(0, 0, 0, 0),
            goldenScreenshotId = FORM_TEXT_WIDGET_CORRECT_POSITION_WITHOUT_PADDING,
        )
    }

    private suspend fun runFormWidgetClickScreenshotTest(
        paddingRect: Rect,
        goldenScreenshotId: String,
    ) {
        val widgetRect = Rect(60, 60, 200, 200)
        val currentTextValue = "Hello"
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(2) { Point(800, 1000) },
                formType = PdfDocument.PDF_FORM_TYPE_ACRO_FORM,
                pageFormWidgetInfos =
                    mapOf(
                        0 to
                            listOf(
                                FormWidgetInfo.createTextField(
                                    widgetIndex = 0,
                                    widgetRect = widgetRect,
                                    textValue = currentTextValue,
                                    accessibilityLabel = currentTextValue,
                                    isReadOnly = false,
                                    isMultiLineText = false,
                                    fontSize = 10.0f,
                                    isEditableText = true,
                                    maxLength = 100,
                                )
                            )
                    ),
            )

        var pdfView: PdfView? = null
        setupPdfView(
            fakePdfDocument = fakePdfDocument,
            enableFormFilling = true,
            paddingRect = paddingRect,
        )

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)
            fakePdfDocument.waitForLayout(untilPage = 0)
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                pdfView = view as PdfView
                pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_HIDE
            }
            val widgetCenterInViewCoordinates =
                pdfView?.pdfToViewPoint(
                    PdfPoint(0, widgetRect.centerX().toFloat(), widgetRect.centerY().toFloat())
                )
            requireNotNull(widgetCenterInViewCoordinates)
            Espresso.onView(withId(PDF_VIEW_ID))
                .perform(
                    performSingleTapOnCoords(
                        widgetCenterInViewCoordinates.x,
                        widgetCenterInViewCoordinates.y,
                    )
                )
            val editTextMatcher: (View) -> Boolean = { view ->
                view is EditText && view.text.toString() == currentTextValue && view.isShown
            }
            val childAddedIdlingResource = ChildViewAddedIdlingResource(pdfView, editTextMatcher)
            try {
                IdlingRegistry.getInstance().register(childAddedIdlingResource)
                Espresso.onView(withText(currentTextValue))
                    .check(ViewAssertions.matches(isDisplayed()))
                assertScreenshot(PDF_VIEW_ID, screenshotRule, goldenScreenshotId)
            } finally {
                IdlingRegistry.getInstance().unregister(childAddedIdlingResource)
            }
            close()
        }
    }

    private fun setupPdfView(
        width: Int = DEFAULT_WIDTH,
        height: Int = DEFAULT_HEIGHT,
        fakePdfDocument: FakePdfDocument?,
        enableFormFilling: Boolean = false,
        paddingRect: Rect = Rect(0, 0, 0, 0),
    ) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        setPadding(
                            paddingRect.left,
                            paddingRect.top,
                            paddingRect.right,
                            paddingRect.bottom,
                        )
                        isFormFillingEnabled = enableFormFilling
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        addOnFormWidgetInfoUpdatedListener(
                            object : PdfView.OnFormWidgetInfoUpdatedListener {
                                override fun onFormWidgetInfoUpdated(formEditInfo: FormEditInfo) {}
                            }
                        )
                    },
                    ViewGroup.LayoutParams(width, height),
                )
            }
        }
    }

    companion object {
        private const val PDF_VIEW_ID = 123456789
        private const val DEFAULT_WIDTH = 200
        private const val DEFAULT_HEIGHT = 400
        private const val FORM_TEXT_WIDGET_CORRECT_POSITION_WITH_PADDING =
            "form_edit_text_correct_position_with_padding"
        private const val FORM_TEXT_WIDGET_CORRECT_POSITION_WITHOUT_PADDING =
            "form_edit_text_correct_position_without_padding"
    }
}
