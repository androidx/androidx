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
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.pdf.ExperimentalPdfApi
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPdfApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewBackgroundBitmapReleaseTest {

    @Before
    fun setup() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                val pdfView = PdfView(activity).apply { id = PDF_VIEW_ID }
                container.addView(pdfView, ViewGroup.LayoutParams(500, 1000))
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun releaseAll_releasesBitmapsWhenHidden_andFetchesThemAgainWhenShown() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).apply {
                    backgroundBitmapRelease = PdfView.BACKGROUND_BITMAP_RELEASE_ALL
                    pdfDocument = fakePdfDocument
                }
            }
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)

            hideWindow()
            fakePdfDocument.clearBitmapRequests()
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertThat(fakePdfDocument.bitmapRequests).isEmpty()

            moveToState(Lifecycle.State.RESUMED)

            // Bitmaps were released, so they are fetched again for the visible pages
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)
            close()
        }
    }

    @Test
    fun releaseNone_retainsBitmapsWhenHidden() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).pdfDocument = fakePdfDocument
            }
            fakePdfDocument.waitForRender(untilPage = 4)
            assertThat(fakePdfDocument.bitmapRequests.size).isEqualTo(5)

            hideWindow()
            fakePdfDocument.clearBitmapRequests()
            moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Bitmaps were retained, so none should be requested again
            assertThat(fakePdfDocument.bitmapRequests).isEmpty()
            close()
        }
    }

    /** Stops the activity, so that [PdfView]'s window is no longer visible. */
    private fun ActivityScenario<PdfViewTestActivity>.hideWindow() {
        moveToState(Lifecycle.State.CREATED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
