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

import android.graphics.Point
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewRenderingTest {
    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testPageRendering() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        setupPdfView(500, 1000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForRender(untilPage = 4)

            val requestedBitmaps = pdfDocument.bitmapRequests
            assertThat(requestedBitmaps.size).isEqualTo(5)
            for (i in 0..4) {
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width)
                    .isEqualTo(500)
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                    .isEqualTo(200)
            }

            close()
        }
    }

    @Test
    fun testPageRendering_renderNewPagesOnScroll() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        setupPdfView(500, 1000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForRender(untilPage = 4)
            assertThat(pdfDocument.bitmapRequests.size).isEqualTo(5)

            // Scroll until the viewport spans [1000, 2000] vertically and expect to render pages
            // [5, 9]
            Espresso.onView(withId(PDF_VIEW_ID)).scrollByY(1000)
            pdfDocument.waitForRender(untilPage = 9)
            val requestedBitmaps = pdfDocument.bitmapRequests
            assertThat(requestedBitmaps.size).isEqualTo(10)
            for (i in 0..9) {
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width)
                    .isEqualTo(500)
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                    .isEqualTo(200)
            }

            close()
        }
    }

    @Test
    fun testPageRendering_renderNewBitmapsOnZoom() = runTest {
        // Layout at 500x1000, and expect to render pages [0, 4] at 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        setupPdfView(500, 1000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForRender(untilPage = 4)
            assertThat(pdfDocument.bitmapRequests.size).isEqualTo(5)

            // Set zoom to 2f, and expect to render pages [0, 2] at 200x400
            // Reset render reach, as we expect to start rendering new Bitmaps for already-rendered
            // pages
            pdfDocument.clearBitmapRequests()
            Espresso.onView(withId(PDF_VIEW_ID)).zoomTo(2f)
            pdfDocument.waitForRender(untilPage = 2)
            val requestedBitmaps = pdfDocument.bitmapRequests
            assertThat(requestedBitmaps.size).isEqualTo(3)
            for (i in 0..2) {
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.width)
                    .isEqualTo(1000)
                assertThat((requestedBitmaps[i] as? FullBitmap)?.scaledPageSizePx?.height)
                    .isEqualTo(400)
            }

            close()
        }
    }
}

/** Create, measure, and layout a [PdfView] at the specified [width] and [height] */
private fun setupPdfView(width: Int, height: Int, fakePdfDocument: FakePdfDocument?) {
    PdfViewTestActivity.onCreateCallback = { activity ->
        with(activity) {
            container.addView(
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                },
                ViewGroup.LayoutParams(width, height),
            )
        }
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
