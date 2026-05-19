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

package androidx.pdf.ocr

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.pdf.PdfPoint
import androidx.pdf.TestUtils.assertNotNullObjectByText
import androidx.pdf.TestUtils.assertNullObjectByText
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.annotation.models.PdfObject
import androidx.pdf.util.isImageSelectionAvailableInSdk
import androidx.pdf.view.FakePdfDocument
import androidx.pdf.view.PdfView
import androidx.pdf.view.PdfViewTestActivity
import androidx.pdf.view.scrollByY
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@LargeTest
class OcrProviderTest {

    private val imageBounds = RectF(0f, 0f, 500f, 500f)
    private val ocrWord = OcrText("Hello", listOf(Rect(0, 0, 100, 100)))

    @Before
    fun setup() {
        assumeTrue(isImageSelectionAvailableInSdk())

        val fakeResult = FakeOcrResult(words = listOf(ocrWord))
        val fakeOcrProvider = FakeOcrProvider(fakeResult)

        val fakePdfDocument =
            object : FakePdfDocument(pages = listOf(Point(500, 500), Point(500, 500))) {
                override suspend fun getTopPageObjectAtPosition(
                    pageNum: Int,
                    point: PointF,
                ): PdfObject? {
                    return if (pageNum == 0 && imageBounds.contains(point.x, point.y)) {
                        ImagePdfObject(
                            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888),
                            imageBounds,
                        )
                    } else null
                }
            }

        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        setOcrProvider(fakeOcrProvider)
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testLongPressOnOcrSelection_showsMenuOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val tapPoint = getTapPoint(PdfPoint(0, 25f, 25f))

            // Create a selection by long-pressing on the image word.
            onView(withId(PDF_VIEW_ID))
                .perform(performLongClickOnViewCoords(tapPoint.x, tapPoint.y))

            // Verify that the long press selected started action mode.
            assertNotNullObjectByText("Copy")
        }
    }

    @Test
    fun testLongPressOnImageNoWord_doesNotShowMenuOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val tapPoint = getTapPoint(PdfPoint(0, 200f, 200f))

            // Long-press on the image but away from the word.
            onView(withId(PDF_VIEW_ID))
                .perform(performLongClickOnViewCoords(tapPoint.x, tapPoint.y))

            // Verify that no selection menu is shown.
            assertNullObjectByText("Copy")
        }
    }

    @Test
    fun testDoubleTapAfterOcrSelection_stillShowsMenuOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val tapPoint = getTapPoint(PdfPoint(0, 25f, 25f))

            // Create a selection by long-pressing on the image word.
            onView(withId(PDF_VIEW_ID))
                .perform(performLongClickOnViewCoords(tapPoint.x, tapPoint.y))

            // Verify that the long press selected started action mode.
            assertNotNullObjectByText("Copy")

            // Double Tap to zoom in at the selection point.
            onView(withId(PDF_VIEW_ID))
                .perform(performDoubleClickOnViewCoords(tapPoint.x, tapPoint.y))

            // Verify that the contextual menu is still visible.
            assertNotNullObjectByText("Copy")
        }
    }

    @Test
    fun testScrollOutOfPageAfterOcrSelection_hidesMenuOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val tapPoint = getTapPoint(PdfPoint(0, 25f, 25f))

            // 1. Create a selection by long-pressing on the image word.
            onView(withId(PDF_VIEW_ID))
                .perform(performLongClickOnViewCoords(tapPoint.x, tapPoint.y))
            assertNotNullObjectByText("Copy")

            // 2. Scroll the view down by 600 pixels (page 0 is 500 high).
            // This should scroll the selection completely off-screen.
            onView(withId(PDF_VIEW_ID)).scrollByY(600)

            // 3. Verify that the contextual menu is hidden.
            assertNullObjectByText("Copy")
        }
    }

    private fun getTapPoint(pdfPoint: PdfPoint): PointF {
        var tapPoint: PointF? = null
        onView(withId(PDF_VIEW_ID)).check { view, _ ->
            val pdfView = view as PdfView
            tapPoint = pdfView.pdfToViewPoint(pdfPoint)
        }
        return requireNotNull(tapPoint) { "Failed to map PdfPoint to view coordinates" }
    }

    private fun performLongClickOnViewCoords(x: Float, y: Float): ViewAction {
        return GeneralClickAction(
            Tap.LONG,
            { view ->
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)

                val screenX = (screenPos[0] + x)
                val screenY = (screenPos[1] + y)

                floatArrayOf(screenX, screenY)
            },
            Press.FINGER,
            InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.BUTTON_PRIMARY,
        )
    }

    private fun performDoubleClickOnViewCoords(x: Float, y: Float): ViewAction {
        return GeneralClickAction(
            Tap.DOUBLE,
            { view ->
                val screenPos = IntArray(2)
                view.getLocationOnScreen(screenPos)

                val screenX = (screenPos[0] + x)
                val screenY = (screenPos[1] + y)

                floatArrayOf(screenX, screenY)
            },
            Press.FINGER,
            InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.BUTTON_PRIMARY,
        )
    }

    companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
