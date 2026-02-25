/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import android.view.ViewGroup
import androidx.pdf.PdfPoint
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
@LargeTest
class BitmapUpdatedListenerTest {
    private lateinit var pdfView: PdfView

    @Before
    fun setup() {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                pdfView = PdfView(activity).apply { id = PDF_VIEW_ID }

                container.addView(pdfView, ViewGroup.LayoutParams(500, 1000))
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testOnBitmapFetched_onBitmapUpdated() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })
        val listener = TestBitmapUpdatedListener()

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).apply {
                    setOnBitmapUpdatedListener(listener)
                    pdfDocument = fakePdfDocument
                }
            }

            fakePdfDocument.waitForRender(4)

            // Assert bitmap fetched for [0,4] with each page size - 500 x 200
            assertThat(listener.fetchPageUpdates).isEqualTo(5)
            // Assert no call made to clear pages
            assertThat(listener.clearPageUpdates).isEqualTo(0)
            close()
        }
    }

    @Test
    fun testOnPageScroll_firesOnPageCleared() = runTest {
        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })
        val listener = TestBitmapUpdatedListener()

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).apply {
                    setOnBitmapUpdatedListener(listener)
                    pdfDocument = fakePdfDocument
                }
            }

            fakePdfDocument.waitForRender(4)

            // Assert bitmap fetched for [0,4] with each page size - 500 x 200
            assertThat(listener.fetchPageUpdates).isEqualTo(5)

            // Scroll to the top of page 10
            Espresso.onView(withId(PDF_VIEW_ID)).scrollToPosition(PdfPoint(9, PointF(0F, 0F)))
            fakePdfDocument.waitForRender(9)

            // Assert clear bitmaps called for pages that are gone off the viewport
            assertThat(listener.clearPageUpdates).isGreaterThan(0)

            close()
        }
    }

    @Test
    fun testListenerUpdate_swapListener() = runTest {
        val listener1 = TestBitmapUpdatedListener()
        val listener2 = TestBitmapUpdatedListener()

        val fakePdfDocument = FakePdfDocument(pages = List(10) { Point(500, 200) })

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            var pdfView: PdfView? = null
            onActivity { activity ->
                pdfView = activity.findViewById(PDF_VIEW_ID)
                // Set listener 1
                pdfView?.setOnBitmapUpdatedListener(listener1)
                pdfView?.pdfDocument = fakePdfDocument
            }

            fakePdfDocument.waitForRender(0)
            // Assert updates are dispatched to the listener 1
            assertThat(listener1.fetchPageUpdates).isGreaterThan(0)

            onActivity {
                // Swap the listener
                pdfView?.setOnBitmapUpdatedListener(listener2)
            }

            // Scroll to the top of page 10
            Espresso.onView(withId(PDF_VIEW_ID)).scrollToPosition(PdfPoint(9, PointF(0F, 0F)))

            fakePdfDocument.waitForRender(9)
            // Assert updates are dispatched to the new listener
            assertThat(listener2.fetchPageUpdates).isGreaterThan(0)
        }
    }

    class TestBitmapUpdatedListener : PdfView.OnBitmapUpdatedListener {

        var fetchPageUpdates = 0
        var clearPageUpdates = 0

        override fun onBitmapFetched(pageNum: Int) {
            fetchPageUpdates++
        }

        override fun onBitmapCleared(pageNum: Int) {
            clearPageUpdates++
        }
    }

    companion object {
        const val PDF_VIEW_ID = 123456789
    }
}
