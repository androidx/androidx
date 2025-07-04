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
                FormWidgetInfo(
                    widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                    widgetIndex = 0,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                ),
                FormWidgetInfo(
                    widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                    widgetIndex = 1,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                ),
            ),
        )
        mPdfFormFillingState.addPageFormWidgetInfos(
            1,
            listOf(
                FormWidgetInfo(
                    widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                    widgetIndex = 0,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                ),
                FormWidgetInfo(
                    widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                    widgetIndex = 1,
                    widgetRect = Rect(50, 500, 100, 600),
                    textValue = "false",
                    accessibilityLabel = "Radio",
                ),
            ),
        )

        val parcel = Parcel.obtain()
        mPdfFormFillingState.writeToParcel(parcel, 0)

        parcel.setDataPosition(0)

        val newPdfFormFillingState = PdfFormFillingState.CREATOR.createFromParcel(parcel)

        assertThat(newPdfFormFillingState.numPages).isEqualTo(mPdfFormFillingState.numPages)
        for (i in 0 until NUM_PAGES) {
            assertThat(newPdfFormFillingState.getPageFormWidgetInfos(i))
                .isEqualTo(mPdfFormFillingState.getPageFormWidgetInfos(i))
        }
    }
}
