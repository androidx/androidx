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

import android.graphics.Point
import android.graphics.PointF
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewZoomStateTest {

    private lateinit var activityScenario: ActivityScenario<PdfViewTestActivity>

    @Before
    fun setup() {
        activityScenario = ActivityScenario.launch(PdfViewTestActivity::class.java)
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
        activityScenario.close()
    }

    private fun setupPdfView(fakePdfDocument: FakePdfDocument?) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container = FrameLayout(activity)
            val pdfView =
                PdfView(activity).apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                    minZoom = 0.5f
                    maxZoom = 5.0f
                }
            container.addView(pdfView, ViewGroup.LayoutParams(PAGE_WIDTH, PAGE_HEIGHT * 2))
            activity.setContentView(container)
        }
    }

    @Test
    fun testInitialZoom_fitWidth() = runTest {
        val fakePdfDocument = FakePdfDocument(List(20) { Point(PAGE_WIDTH, PAGE_HEIGHT) })

        setupPdfView(fakePdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForLayout(untilPage = 3)
            onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                assertThat(pdfView.isInitialZoomDone).isTrue()
                assertThat(pdfView.zoom).isWithin(0.01f).of(1.0f)
            }
        }
    }

    @Test
    fun testGetDefaultZoom_fitWidth() = runTest {
        val fakePdfDocument = FakePdfDocument(List(20) { Point(PAGE_WIDTH, PAGE_HEIGHT) })

        setupPdfView(fakePdfDocument)
        activityScenario.recreate()

        activityScenario.onActivity { activity ->
            val pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
            pdfView.zoom = 2.0f
            val expectedZoom = 1.0f
            val actualZoom = pdfView.getDefaultZoom()
            assertThat(actualZoom).isWithin(0.01f).of(expectedZoom)
        }
    }

    @Test
    fun testRestoreUserZoomAndScrollPosition() = runTest {
        val fakePdfDocument = FakePdfDocument(List(20) { Point(PAGE_WIDTH, PAGE_HEIGHT) })
        val savedZoom = 2.5f
        val savedScrollPosition = PointF(100f, PAGE_HEIGHT * 1f / savedZoom)

        setupPdfView(fakePdfDocument)
        activityScenario.recreate()

        activityScenario.onActivity { activity ->
            val pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
            pdfView.zoom = savedZoom
            pdfView.scrollTo(
                (savedScrollPosition.x * savedZoom - pdfView.viewportWidth / 2f).toInt(),
                (savedScrollPosition.y * savedZoom - pdfView.viewportHeight / 2f).toInt()
            )
            pdfView.isInitialZoomDone = true
        }

        activityScenario.recreate()

        onView(withId(PDF_VIEW_ID)).check { view, _ ->
            view as PdfView
            assertThat(view.zoom).isWithin(0.01f).of(savedZoom)
            val expectedScrollX =
                (savedScrollPosition.x * savedZoom - view.viewportWidth / 2f).toInt()
            val expectedScrollY =
                (savedScrollPosition.y * savedZoom - view.viewportHeight / 2f).toInt()
            assertThat(view.scrollX).isEqualTo(expectedScrollX)
            assertThat(view.scrollY).isEqualTo(expectedScrollY)
        }
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
private const val PAGE_WIDTH = 500
private const val PAGE_HEIGHT = 800

/** The height of the viewport, minus padding */
val PdfView.viewportHeight: Int
    get() = bottom - top - paddingBottom - paddingTop

/** The width of the viewport, minus padding */
val PdfView.viewportWidth: Int
    get() = right - left - paddingRight - paddingLeft
