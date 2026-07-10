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

package androidx.pdf.view.fastscroll

import android.graphics.Point
import android.view.ViewGroup.LayoutParams
import androidx.pdf.FAST_SCROLLER_BOTTOM
import androidx.pdf.FAST_SCROLLER_TOP
import androidx.pdf.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.pdf.assertScreenshot
import androidx.pdf.view.FakePdfDocument
import androidx.pdf.view.PdfView
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class FastScrollViewScubaTest {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Before
    fun setup() {
        setupFastScroller()
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testFastScrollerAtBottom() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onView(withId(PDF_VIEW_ID)).perform(swipeUp())
            assertScreenshot(PDF_VIEW_ID, screenshotRule, FAST_SCROLLER_BOTTOM)

            close()
        }
    }

    @Test
    fun testFastScrollerAtTop() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Swipe to the bottom first, as the fast scroller is hidden at the top initially.
            onView(withId(PDF_VIEW_ID)).perform(swipeUp())

            // Swipe back to the top
            onView(withId(PDF_VIEW_ID)).perform(swipeDown())
            assertScreenshot(PDF_VIEW_ID, screenshotRule, FAST_SCROLLER_TOP)

            close()
        }
    }

    private fun setupFastScroller() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                val pdfView =
                    PdfView(this).apply {
                        id = PDF_VIEW_ID
                        layoutParams = LayoutParams(250, 500)
                    }
                container.addView(pdfView)

                pdfView.pdfDocument = FakePdfDocument(List(2) { Point(250, 400) })
            }
        }
    }

    companion object {
        private const val PDF_VIEW_ID = 123456789
    }
}
