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

import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import androidx.pdf.FakePdfDocument
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormWidgetInfo
import androidx.pdf.view.PdfFormFillingState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class AutofillHintProviderTest {

    private lateinit var provider: AutofillHintProvider

    private val pageNum = 0
    private val widgetIndex = 0

    @Test
    fun loadHintsForPage_validAccessibilityLabel_returnsValidHint() = runTest {
        val accessibilityLabel = "Email Address"
        val widget = createTextField(accessibilityLabel = accessibilityLabel)
        val allWidgets = listOf(widget)
        val pdfDocument = FakePdfDocument(pages = listOf(Point(500, 500)))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(accessibilityLabel)
    }

    @Test
    fun loadHintsForPage_textToLeftDetected_returnsHintText() = runTest {
        val hintText = "First Name"
        val widget = createTextField()
        val allWidgets = listOf(widget)

        val textContent =
            PdfPageTextContent(bounds = listOf(RectF(10f, 110f, 80f, 140f)), text = hintText)
        val pdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(textContent))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(hintText)
    }

    @Test
    fun loadHintsForPage_textAboveDetected_returnsHintText() = runTest {
        val hintText = "Last Name"
        val widget = createTextField()
        val allWidgets = listOf(widget)

        val textContent =
            PdfPageTextContent(bounds = listOf(RectF(100f, 40f, 200f, 60f)), text = hintText)
        val pdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(textContent))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(hintText)
    }

    @Test
    fun loadHintsForPage_nonTextField_ignoredForHint() = runTest {
        val accessibilityLabel = "Email Address"
        val widget =
            FormWidgetInfo.createCheckbox(
                widgetIndex = widgetIndex,
                widgetRect = Rect(100, 100, 200, 150),
                textValue = "false",
                accessibilityLabel = accessibilityLabel,
                isReadOnly = false,
            )
        val allWidgets = listOf(widget)
        val pdfDocument = FakePdfDocument(pages = listOf(Point(500, 500)))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isNull()
    }

    @Test
    fun loadHintsForPage_hintAlreadyExists_skipsDetection() = runTest {
        val accessibilityLabel = "Email Address"
        val existingHint = "Existing Hint"
        val widget = createTextField(accessibilityLabel = accessibilityLabel)
        val allWidgets = listOf(widget)

        val formFillingState = PdfFormFillingState(numPages = 1)
        formFillingState.addHintText(pageNum, widgetIndex, existingHint)

        val pdfDocument = FakePdfDocument(pages = listOf(Point(500, 500)))
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(existingHint)
    }

    @Test
    fun loadHintsForPage_invalidAccessibilityLabel_fallsBackToText() = runTest {
        val hintText = "First Name"
        val widget = createTextField(accessibilityLabel = "Text1")
        val allWidgets = listOf(widget)

        val textContent =
            PdfPageTextContent(bounds = listOf(RectF(10f, 110f, 80f, 140f)), text = hintText)
        val pdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(textContent))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(hintText)
    }

    @Test
    fun loadHintsForPage_widgetToLeft_detectsTextBetweenWidgets() = runTest {
        val hintText = "First Name"
        val widgetToLeft = createTextField(index = 0, rect = Rect(10, 100, 50, 150))
        val targetWidget = createTextField(index = 1, rect = Rect(100, 100, 200, 150))
        val allWidgets = listOf(widgetToLeft, targetWidget)

        val textContent =
            PdfPageTextContent(bounds = listOf(RectF(60f, 110f, 90f, 140f)), text = hintText)
        val pdfDocument =
            FakePdfDocument(pages = listOf(Point(500, 500)), textContents = listOf(textContent))
        val formFillingState = PdfFormFillingState(numPages = 1)
        provider = AutofillHintProvider(pdfDocument, formFillingState) {}

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(formFillingState.getHintText(pageNum, 1)).isEqualTo(hintText)
    }

    @Test
    fun loadHintsForPage_hintDetected_invokesCallback() = runTest {
        val accessibilityLabel = "Email Address"
        val widget = createTextField(accessibilityLabel = accessibilityLabel)
        val allWidgets = listOf(widget)
        val pdfDocument = FakePdfDocument(pages = listOf(Point(500, 500)))
        val formFillingState = PdfFormFillingState(numPages = 1)
        val emissions = mutableListOf<Pair<Int, Int>>()
        provider = AutofillHintProvider(pdfDocument, formFillingState) { emissions.add(it) }

        provider.loadHintsForPage(pageNum, allWidgets)

        assertThat(emissions).containsExactly(pageNum to widgetIndex)
    }

    private fun createTextField(
        index: Int = widgetIndex,
        rect: Rect = Rect(100, 100, 200, 150),
        accessibilityLabel: String? = null,
    ): FormWidgetInfo {
        return FormWidgetInfo.createTextField(
            widgetIndex = index,
            widgetRect = rect,
            textValue = null,
            accessibilityLabel = accessibilityLabel,
            isReadOnly = false,
            isEditableText = true,
            isMultiLineText = false,
            maxLength = 100,
            fontSize = 12f,
        )
    }
}
