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
import android.graphics.PointF
import android.graphics.RectF
import android.util.SparseArray
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.pdf.PdfPoint
import androidx.pdf.R
import androidx.pdf.view.fastscroll.getDimensions
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewPaginationTest {
    var topPageMarginPx: Float = 0f
    var pageMarginPx: Float = 0f

    private var pdfView: PdfView? = null // This is initialized in the setup process

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testPageVisibility() = runTest {
        // Layout at 500x1000, and expect to see pages [0, 4] at 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        setupPdfView(width = 500, height = 1000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 4)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 5)
            close()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class) // Needed for advanceUntilIdle
    @Test
    fun testBgDimensionLoad() = runTest {
        val pageDims = List(5) { Point(50, 100) } + List(5) { Point(50, 200) }
        val pdfDocument = FakePdfDocument(pageDims)
        setupPdfView(width = 500, height = 1000, pdfDocument)
        pdfView?.backgroundScope = this

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 1)
            Espresso.onView(withId(PDF_VIEW_ID)).checkPagesAreVisible(firstVisiblePage = 0)

            // If all pages are loaded, the total height will be sum of the following
            //          100 * 5 + 200 * 5 = 1500
            //          topMargin + spacing-between-pages
            assertThat(pdfView?.contentHeight).isEqualTo(1500 + 10 * pageMarginPx + topPageMarginPx)
            close()
        }
        advanceUntilIdle()
    }

    @OptIn(ExperimentalCoroutinesApi::class) // Needed for advanceUntilIdle
    @Test
    fun testBgDimensionLoadWithMissingValues() = runTest {
        val pageDims =
            List(4) { Point(50, 100) } +
                listOf(null) + // 5th page will throw a cancellation exception
                List(5) { Point(50, 200) }
        val pdfDocument = FakePdfDocument(pageDims)
        pdfView?.backgroundScope = this
        setupPdfView(width = 500, height = 1000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 1)
            Espresso.onView(withId(PDF_VIEW_ID)).checkPagesAreVisible(firstVisiblePage = 0)

            // As 5th page throws cancellation exception, it is approximated by values of the 6th.
            // If all pages are loaded, the total height will be sum of the following
            //          100 * 4 + 200 * 6 = 1600
            //          topMargin + spacing-between-pages
            assertThat(pdfView?.contentHeight).isEqualTo(1600 + 10 * pageMarginPx + topPageMarginPx)
            close()
        }
        advanceUntilIdle()
    }

    @Test
    fun testPageVisibility_withoutPdfDocument() {
        setupPdfView(500, height = 1000, fakePdfDocument = null)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 0)
            close()
        }
    }

    @Test
    fun testPageVisibility_onSizeDecreased() = runTest {
        // Layout at 500x1000 initially, and expect to see pages [0, 3] at 500x300
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 300) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 3)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 4)

            // Reduce size to 100x200, which will update the zoom according to new width
            // i.e. 100/500 -> 0.2 clamped to min zoom = 0.25.
            // With 0.25% zoom, expect to see pages [0, 2] at 100x200(each page height = 300 * 0.25
            // = 75)
            onActivity { activity ->
                activity.findViewById<View>(PDF_VIEW_ID).apply {
                    measure(
                        MeasureSpec.makeMeasureSpec(100, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(200, MeasureSpec.EXACTLY),
                    )
                    layout(0, 0, 100, 200)
                }
            }

            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 2)
            close()
        }
    }

    @Test
    fun testPageVisibility_onScrollChanged() = runTest {
        // Layout at 1000x2000 initially, and expect to see pages [0, 3] at 1000x500
        val pdfDocument = FakePdfDocument(List(10) { Point(1000, 500) })
        setupPdfView(width = 1000, height = 2000, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 3)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 4)

            // Scroll until the viewport spans [500, 2500] vertically and expect to see pages [1, 4]
            // Add small buffer for page transition to handle float/int margin conversion.
            Espresso.onView(withId(PDF_VIEW_ID)).scrollByY(500 + ceil(topPageMarginPx).roundToInt())
            pdfDocument.waitForLayout(untilPage = 4)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 1, visiblePages = 4)

            close()
        }
    }

    @Test
    fun testPageVisibility_onZoomChanged() = runTest {
        // Layout at 100x550 initially, and expect to see pages [0, 5] at 100x80
        val pdfDocument = FakePdfDocument(List(10) { Point(100, 80) })
        setupPdfView(width = 100, height = 550, pdfDocument)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            pdfDocument.waitForLayout(untilPage = 5)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 6)

            // Set zoom to 2f and expect to see pages [0, 2]
            Espresso.onView(withId(PDF_VIEW_ID)).zoomTo(2f)
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 3)

            close()
        }
    }

    @Test
    fun testScrollToPage() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {

            // Scroll to page 9
            Espresso.onView(withId(PDF_VIEW_ID)).scrollToPage(9)
            pdfDocument.waitForLayout(untilPage = 9)

            // Expect to see only page 9
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 9, visiblePages = 1)
            close()
        }
    }

    @Test
    fun testScrollToPosition() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {

            // Scroll to the top of page 9
            Espresso.onView(withId(PDF_VIEW_ID)).scrollToPosition(PdfPoint(9, PointF(0F, 0F)))
            pdfDocument.waitForLayout(untilPage = 9)

            // Expect to see pages 8 and 9
            Espresso.onView(withId(PDF_VIEW_ID))
                .checkPagesAreVisible(firstVisiblePage = 8, visiblePages = 2)
            close()
        }
    }

    @Test
    fun testViewportListener_instantScroll() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val listener = PdfViewportListener()
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listener)
            }

            Espresso.onView(withId(PDF_VIEW_ID)).scrollToPage(9)
            pdfDocument.waitForLayout(untilPage = 9)

            assertThat(listener.firstVisiblePage).isEqualTo(9)
            checkEqualityWithTolerance(
                listener.firstPageLocation,
                RectF(0f, 0f, 500f, 1000f),
                tolerance = 0.5F,
            )

            assertThat(listener.zoomLevel).isEqualTo(1.0F)
            assertThat(listener.updates).isEqualTo(1)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listener)
            }
            close()
        }
    }

    @Test
    fun testViewportListener_smoothScroll() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val listener = PdfViewportListener()
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listener)
            }

            // Round up the floating-point top margin to an integer for precise pixel scrolling.
            val scrollOffsetForTopMargin = ceil(topPageMarginPx).roundToInt()

            // Scroll smoothly past page 0
            Espresso.onView(withId(PDF_VIEW_ID))
                .smoothScrollBy(totalPixels = 1000 + scrollOffsetForTopMargin, numSteps = 10)
            pdfDocument.waitForLayout(untilPage = 1)

            val expectedFirstPageLocation =
                RectF(
                    0f,
                    pageMarginPx + (topPageMarginPx - scrollOffsetForTopMargin),
                    500f,
                    1000 + pageMarginPx + (topPageMarginPx - scrollOffsetForTopMargin),
                )

            assertThat(listener.firstVisiblePage).isEqualTo(1)
            assertThat(listener.firstPageLocation).isEqualTo(expectedFirstPageLocation)
            assertThat(listener.zoomLevel).isEqualTo(1.0F)
            assertThat(listener.updates).isEqualTo(10)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listener)
            }
            close()
        }
    }

    @Test
    fun testViewportListener_instantZoom() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val listener = PdfViewportListener()
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listener)
            }

            val zoom = 5.0F
            Espresso.onView(withId(PDF_VIEW_ID)).zoomTo(zoom)

            assertThat(listener.firstVisiblePage).isEqualTo(0)
            assertThat(listener.firstPageLocation)
                .isEqualTo(
                    RectF(
                        0f,
                        (topPageMarginPx * zoom),
                        (500F * zoom),
                        ((1000F + topPageMarginPx) * zoom),
                    )
                )
            assertThat(listener.zoomLevel).isWithin(0.01F).of(zoom)
            assertThat(listener.updates).isEqualTo(1)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listener)
            }
            close()
        }
    }

    @Test
    fun testViewportListener_smoothZoom() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val listener = PdfViewportListener()
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listener)
            }

            val zoom = 5.0F
            Espresso.onView(withId(PDF_VIEW_ID)).smoothZoomTo(zoom, numSteps = 10)

            assertThat(listener.firstVisiblePage).isEqualTo(0)
            val expectedFirstPageLocation =
                RectF(0f, topPageMarginPx * zoom, (500F * zoom), (1000F + topPageMarginPx) * zoom)

            checkEqualityWithTolerance(
                listener.firstPageLocation,
                expectedFirstPageLocation,
                tolerance = 0.001f,
            )

            assertThat(listener.zoomLevel).isWithin(0.01F).of(zoom)
            assertThat(listener.updates).isEqualTo(10)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listener)
            }
            close()
        }
    }

    @Test
    fun testViewportListener_addRemove() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
        setupPdfView(width = 500, height = 1000, pdfDocument)
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            val listenerA = PdfViewportListener()
            val listenerB = PdfViewportListener()
            onActivity { activity ->
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listenerA)
                activity.findViewById<PdfView>(PDF_VIEW_ID).addOnViewportChangedListener(listenerB)
            }

            // Round up the floating-point top margin to an integer for precise pixel scrolling.
            val scrollOffsetForTopMargin = ceil(topPageMarginPx).roundToInt()

            // Scroll smoothly past page 0
            Espresso.onView(withId(PDF_VIEW_ID))
                .smoothScrollBy(totalPixels = 1000 + scrollOffsetForTopMargin, numSteps = 10)
            pdfDocument.waitForLayout(untilPage = 1)

            val expectedFirstPageLocation =
                RectF(
                    0f,
                    pageMarginPx + (topPageMarginPx - scrollOffsetForTopMargin),
                    500f,
                    1000 + pageMarginPx + (topPageMarginPx - scrollOffsetForTopMargin),
                )

            assertThat(listenerA.firstVisiblePage).isEqualTo(1)
            assertThat(listenerB.firstVisiblePage).isEqualTo(1)
            assertThat(listenerA.firstPageLocation).isEqualTo(expectedFirstPageLocation)
            assertThat(listenerB.firstPageLocation).isEqualTo(expectedFirstPageLocation)
            assertThat(listenerA.zoomLevel).isEqualTo(1.0F)
            assertThat(listenerB.zoomLevel).isEqualTo(1.0F)
            assertThat(listenerA.updates).isEqualTo(10)
            assertThat(listenerB.updates).isEqualTo(10)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listenerB)
            }

            // Scroll smoothly past page 1
            Espresso.onView(withId(PDF_VIEW_ID))
                .smoothScrollBy(totalPixels = 1000 + pageMarginPx.toInt(), numSteps = 10)
            pdfDocument.waitForLayout(untilPage = 1)

            assertThat(listenerA.firstVisiblePage).isEqualTo(2)
            assertThat(listenerA.firstPageLocation).isEqualTo(expectedFirstPageLocation)
            assertThat(listenerA.zoomLevel).isEqualTo(1.0F)
            assertThat(listenerA.updates).isEqualTo(20)
            // No updates for removed listener
            assertThat(listenerB.updates).isEqualTo(10)

            onActivity { activity ->
                activity
                    .findViewById<PdfView>(PDF_VIEW_ID)
                    .removeOnViewportChangedListener(listenerA)
            }
            close()
        }
    }

    @Test
    fun testCoordinateTranslation() =
        runTest(timeout = 30.seconds) {
            val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
            setupPdfView(width = 500, height = 1000, pdfDocument)
            with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
                pdfDocument.waitForLayout(untilPage = 1)
                val pdfToViewPoints =
                    mutableListOf(
                        PdfPoint(pageNum = 0, pagePoint = PointF(0F, 0F)) to
                            PointF(0F, 0F + topPageMarginPx),
                        PdfPoint(pageNum = 0, pagePoint = PointF(250F, 500F)) to
                            PointF(250F, 500F + topPageMarginPx),
                        PdfPoint(pageNum = 0, pagePoint = PointF(499F, 999F)) to
                            PointF(499F, 999F + topPageMarginPx),
                        PdfPoint(pageNum = 1, pagePoint = PointF(0F, 0F)) to
                            PointF(0F, 1000F + pageMarginPx + topPageMarginPx),
                        PdfPoint(pageNum = 1, pagePoint = PointF(250F, 500F)) to
                            PointF(250F, 1500F + pageMarginPx + topPageMarginPx),
                        PdfPoint(pageNum = 1, pagePoint = PointF(499F, 999F)) to
                            PointF(499F, 1999F + pageMarginPx + topPageMarginPx),
                    )
                onActivity { activity ->
                    val pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
                    for (pointPair in pdfToViewPoints) {
                        val viewPoint = pdfView.pdfToViewPoint(pointPair.first)
                        val pdfPoint = pdfView.viewToPdfPoint(pointPair.second)

                        assertThat(viewPoint).isEqualTo(pointPair.second)
                        assertThat(pdfPoint).isEqualTo(pointPair.first)
                    }
                }
            }
        }

    @Test
    fun testCoordinateTranslation_afterScroll() =
        runTest(timeout = 60.seconds) {
            val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
            setupPdfView(width = 500, height = 1000, pdfDocument)
            with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
                Espresso.onView(withId(PDF_VIEW_ID)).scrollToPage(3)
                pdfDocument.waitForLayout(untilPage = 3)

                val pdfToViewPoints =
                    mutableListOf(
                        PdfPoint(pageNum = 3, pagePoint = PointF(0F, 0F)) to PointF(0F, 0F),
                        PdfPoint(pageNum = 3, pagePoint = PointF(250F, 500F)) to PointF(250F, 500F),
                        PdfPoint(pageNum = 3, pagePoint = PointF(499F, 999F)) to PointF(499F, 999F),
                        PdfPoint(pageNum = 4, pagePoint = PointF(0F, 0F)) to
                            PointF(0F, 1000F + pageMarginPx),
                        PdfPoint(pageNum = 4, pagePoint = PointF(250F, 500F)) to
                            PointF(250F, 1500F + pageMarginPx),
                        PdfPoint(pageNum = 4, pagePoint = PointF(499F, 999F)) to
                            PointF(499F, 1999F + pageMarginPx),
                    )
                onActivity { activity ->
                    val pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
                    for (pointPair in pdfToViewPoints) {
                        val viewPoint = pdfView.pdfToViewPoint(pointPair.first)
                        val pdfPoint = pdfView.viewToPdfPoint(pointPair.second)

                        checkEqualityWithTolerance(viewPoint, pointPair.second, tolerance = 0.5F)
                        checkEqualityWithTolerance(
                            pdfPoint?.let { PointF(it.x, it.y) },
                            PointF(pointPair.first.x, pointPair.first.y),
                            tolerance = 0.5F,
                        )
                        assertThat(pdfPoint?.pageNum).isEqualTo(pointPair.first.pageNum)
                    }
                }
            }
        }

    @Test
    fun testCoordinateTranslation_afterZoom() =
        runTest(timeout = 60.seconds) {
            val pdfDocument = FakePdfDocument(List(10) { Point(500, 1000) })
            setupPdfView(width = 500, height = 1000, pdfDocument)
            with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
                val zoom = 2.0F
                Espresso.onView(withId(PDF_VIEW_ID)).zoomTo(zoom)

                val pdfToViewPoints =
                    mutableListOf(
                        PdfPoint(pageNum = 0, pagePoint = PointF(0F, 0F)) to
                            PointF(0F, (0F + topPageMarginPx) * zoom),
                        PdfPoint(pageNum = 0, pagePoint = PointF(250F, 500F)) to
                            PointF(250F * zoom, (500F + topPageMarginPx) * zoom),
                        PdfPoint(pageNum = 0, pagePoint = PointF(499F, 999F)) to
                            PointF(499F * zoom, (999F + topPageMarginPx) * zoom),
                        PdfPoint(pageNum = 1, pagePoint = PointF(0F, 0F)) to
                            PointF(0F, (1000F + pageMarginPx + topPageMarginPx) * zoom),
                        PdfPoint(pageNum = 1, pagePoint = PointF(250F, 500F)) to
                            PointF(250F * zoom, (1500F + pageMarginPx + topPageMarginPx) * zoom),
                        PdfPoint(pageNum = 1, pagePoint = PointF(499F, 999F)) to
                            PointF(499F * zoom, (1999F + pageMarginPx + topPageMarginPx) * zoom),
                    )

                onActivity { activity ->
                    val pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
                    for (pointPair in pdfToViewPoints) {
                        val viewPoint = pdfView.pdfToViewPoint(pointPair.first)
                        val pdfPoint = pdfView.viewToPdfPoint(pointPair.second)

                        assertThat(viewPoint).isEqualTo(pointPair.second)
                        assertThat(pdfPoint).isEqualTo(pointPair.first)
                    }
                }
            }
        }

    /** Create, measure, and layout a [PdfView] at the specified [width] and [height] */
    private fun setupPdfView(width: Int, height: Int, fakePdfDocument: FakePdfDocument?) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            pageMarginPx = activity.getDimensions(R.dimen.page_spacing)
            topPageMarginPx = activity.getDimensions(R.dimen.top_page_margin)
            val container = FrameLayout(activity)
            pdfView = PdfView(activity)
            container.addView(
                pdfView?.apply {
                    pdfDocument = fakePdfDocument
                    id = PDF_VIEW_ID
                },
                ViewGroup.LayoutParams(width, height),
            )
            activity.setContentView(container)
        }
    }
}

/**
 * Implementation of [PdfView.OnViewportChangedListener] that captures the most recent values
 * received as well as the number of updates
 */
private class PdfViewportListener : PdfView.OnViewportChangedListener {
    var firstVisiblePage: Int = -1
        private set

    var firstPageLocation: RectF = RectF(-1f, -1f, -1f, -1f)
        private set

    var zoomLevel: Float = -1F
        private set

    var updates = 0
        private set

    override fun onViewportChanged(
        firstVisiblePage: Int,
        visiblePagesCount: Int,
        pageLocations: SparseArray<RectF>,
        zoomLevel: Float,
    ) {
        this.firstVisiblePage = firstVisiblePage
        this.firstPageLocation = RectF(pageLocations.get(firstVisiblePage))
        this.zoomLevel = zoomLevel
        updates++
    }
}

/**
 * Compares two RectF objects for equality within a given tolerance for each coordinate.
 *
 * @param expect The first RectF.
 * @param actual The second RectF.
 * @param tolerance The maximum allowed difference for each coordinate (left, top, right, bottom).
 */
fun checkEqualityWithTolerance(expect: RectF, actual: RectF, tolerance: Float) {
    val errorMessage = "Expected $expect; got $actual"
    assertWithMessage(errorMessage).that(abs(expect.left - actual.left)).isAtMost(tolerance)
    assertWithMessage(errorMessage).that(abs(expect.top - actual.top)).isAtMost(tolerance)
    assertWithMessage(errorMessage).that(abs(expect.right - actual.right)).isAtMost(tolerance)
    assertWithMessage(errorMessage).that(abs(expect.bottom - actual.bottom)).isAtMost(tolerance)
}

/**
 * Checks if two PointF objects are close enough, considering a tolerance for each coordinate.
 *
 * @param expect The first PointF.
 * @param actual The second PointF.
 * @param tolerance The maximum allowed difference for each coordinate (x, y).
 */
fun checkEqualityWithTolerance(expect: PointF?, actual: PointF?, tolerance: Float) {
    val errorMessage = "Expected $expect; got $actual"
    if (expect === actual) return
    assertWithMessage(errorMessage).that(expect).isNotNull()
    assertWithMessage(errorMessage).that(actual).isNotNull()
    // Truth.assertThat yields readable error messages, requireNotNull makes Kotlin happy
    assertWithMessage(errorMessage)
        .that(abs(requireNotNull(expect?.x) - requireNotNull(actual?.x)))
        .isAtMost(tolerance)
    assertWithMessage(errorMessage).that(abs(expect.y - actual.y)).isAtMost(tolerance)
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
