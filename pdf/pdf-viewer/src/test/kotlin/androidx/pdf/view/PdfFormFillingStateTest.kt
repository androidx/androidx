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

import android.graphics.Rect
import android.os.Parcel
import androidx.pdf.models.FormWidgetInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfFormFillingStateTest {
    private val NUM_PAGES = 10

    private lateinit var mPdfFormFillingState: PdfFormFillingState

    @Before
    fun setup() {
        mPdfFormFillingState = PdfFormFillingState(NUM_PAGES)
    }

    @Test
    fun testParcelable() {
        mPdfFormFillingState.addPageFormWidgetInfos(
            0,
            listOf(
                FormWidgetInfo.createRadioButton(
                    widgetIndex = 0,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                    isReadOnly = false,
                ),
                FormWidgetInfo.createRadioButton(
                    widgetIndex = 1,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                    isReadOnly = false,
                ),
            ),
        )
        mPdfFormFillingState.addPageFormWidgetInfos(
            1,
            listOf(
                FormWidgetInfo.createTextField(
                    widgetIndex = 0,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                    isReadOnly = false,
                    isEditableText = true,
                    isMultiLineText = false,
                    maxLength = 10,
                    fontSize = 10f,
                ),
                FormWidgetInfo.createCheckbox(
                    widgetIndex = 1,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                    isReadOnly = false,
                ),
            ),
        )

        mPdfFormFillingState.addHintText(pageNum = 0, widgetIndex = 0, text = "Name")
        mPdfFormFillingState.addHintText(pageNum = 1, widgetIndex = 0, text = "Email")

        val parcel = Parcel.obtain()
        mPdfFormFillingState.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val newPdfFormFillingState = PdfFormFillingState.CREATOR.createFromParcel(parcel)

        assertThat(newPdfFormFillingState.numPages).isEqualTo(mPdfFormFillingState.numPages)
        for (i in 0 until NUM_PAGES) {
            assertThat(newPdfFormFillingState.getPageFormWidgetInfos(i))
                .isEqualTo(mPdfFormFillingState.getPageFormWidgetInfos(i))
        }
        assertThat(newPdfFormFillingState.getHintText(0, 0)).isEqualTo("Name")
        assertThat(newPdfFormFillingState.getHintText(1, 0)).isEqualTo("Email")
        assertThat(newPdfFormFillingState.getHintText(2, 0)).isNull()
    }

    @Test
    fun test_addAndGetHintText() {
        val pageNum = 0
        val widgetIndex = 5
        val hintText = "First Name"

        mPdfFormFillingState.addHintText(pageNum, widgetIndex, hintText)

        assertThat(mPdfFormFillingState.getHintText(pageNum, widgetIndex)).isEqualTo(hintText)
    }

    @Test
    fun test_hasHintsForPage() {
        assertThat(mPdfFormFillingState.hasHintsForPage(0)).isFalse()

        mPdfFormFillingState.addHintText(0, 0, "Hint")

        assertThat(mPdfFormFillingState.hasHintsForPage(0)).isTrue()
        assertThat(mPdfFormFillingState.hasHintsForPage(1)).isFalse()
    }
}
