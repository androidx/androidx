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

package androidx.pdf.actions

import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.pdf.PdfPoint
import androidx.pdf.view.PdfView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.PrecisionDescriber
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.Tapper
import com.google.common.truth.Truth

/** [GeneralClickAction] that supports clicking on PDF coordinates, as [PdfPoint] */
fun clickOnPdfPoint(
    pdfPoint: PdfPoint,
    tapper: Tapper = Tap.SINGLE,
    precisionDescriber: PrecisionDescriber = Press.FINGER,
    inputSource: Int = InputDevice.SOURCE_UNKNOWN,
    buttonState: Int = MotionEvent.BUTTON_PRIMARY,
): ViewAction =
    GeneralClickAction(
        tapper,
        PdfCoordinatesProvider(pdfPoint),
        precisionDescriber,
        inputSource,
        buttonState,
    )

/**
 * [CoordinatesProvider] implementation that allows tests to specify a location in PDF coordinates
 * that's converted to View coordinates at runtime.
 */
private class PdfCoordinatesProvider(private val pdfPoint: PdfPoint) : CoordinatesProvider {
    override fun calculateCoordinates(view: View): FloatArray {
        // Truth makes nice, readable exceptions. Require makes smart casts happy.
        Truth.assertThat(view).isInstanceOf(PdfView::class.java)
        require(view is PdfView)
        val viewPoint = view.pdfToViewPoint(pdfPoint)

        // Truth makes nice, readable exceptions. Check makes smart casts happy.
        Truth.assertThat(viewPoint).isNotNull()
        checkNotNull(viewPoint)

        return floatArrayOf(viewPoint.x, viewPoint.y)
    }
}
