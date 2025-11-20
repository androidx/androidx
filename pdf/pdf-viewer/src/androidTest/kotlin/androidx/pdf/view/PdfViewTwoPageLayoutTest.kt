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

import android.content.pm.ActivityInfo
import android.graphics.Point
import android.graphics.RectF
import android.view.ViewGroup
import androidx.pdf.featureflag.PdfFeatureFlags
import androidx.pdf.view.layout.SinglePageLayoutStrategy
import androidx.pdf.view.layout.TwoPageLayoutStrategy
import androidx.test.core.app.ActivityScenario
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
class PdfViewTwoPageLayoutTest {
    @Before
    fun setUp() {
        PdfFeatureFlags.isLayoutStrategyEnabled = true
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testRerenderOnPagesPerRowChange_singleToTwoPage() = runTest {
        // With a 1200x1000 viewport, and pages of 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        val horizontalSpacing = 15f
        val verticalSpacing = 20f
        // Initial layout is single page
        setupPdfView(
            1200,
            1000,
            pdfDocument,
            PdfView.SINGLE_PAGE,
            horizontalSpacing,
            verticalSpacing,
        )

        lateinit var firstPageLocation: RectF
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Verify initial layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(SinglePageLayoutStrategy::class.java)
                assertThat(pdfView.contentWidth).isEqualTo(500f)
                pdfView.pageLayoutManager?.let {
                    firstPageLocation =
                        it.getPageLocation(0, pdfView.getVisibleAreaInContentCoords())
                }
            }

            // Change to two-page layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.pagesPerRow = PdfView.TWO_PAGE
            }

            // Verify new layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(TwoPageLayoutStrategy::class.java)
                val expectedMaxWidth = 500 + horizontalSpacing + 500
                assertThat(pdfView.contentWidth).isEqualTo(expectedMaxWidth)
                pdfView.pageLayoutManager?.let {
                    val newFirstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                    assertThat(newFirstPageLocation.top).isEqualTo(firstPageLocation.top)
                    assertThat(newFirstPageLocation.left)
                        .isEqualTo(firstPageLocation.right + horizontalSpacing)
                }
            }
            close()
        }
    }

    @Test
    fun testRerenderOnPagesPerRowChange_twoToSinglePage() = runTest {
        // With a 1200x1000 viewport, and pages of 500x200
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        val horizontalSpacing = 15f
        val verticalSpacing = 15f
        // Initial layout is two page
        setupPdfView(1200, 1000, pdfDocument, PdfView.TWO_PAGE, horizontalSpacing, verticalSpacing)

        lateinit var firstPageLocation: RectF
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Verify initial layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(TwoPageLayoutStrategy::class.java)
                val expectedMaxWidth = 500 + horizontalSpacing + 500
                assertThat(pdfView.pageLayoutManager?.maxContentWidth).isEqualTo(expectedMaxWidth)
                pdfView.pageLayoutManager?.let {
                    firstPageLocation =
                        it.getPageLocation(0, pdfView.getVisibleAreaInContentCoords())
                }
            }

            // Change to single-page layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.pagesPerRow = PdfView.SINGLE_PAGE
            }

            // Verify new layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(SinglePageLayoutStrategy::class.java)
                assertThat(pdfView.pageLayoutManager?.maxContentWidth).isEqualTo(500f)
                pdfView.pageLayoutManager?.let {
                    val newFirstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                    assertThat(newFirstPageLocation.top)
                        .isEqualTo(firstPageLocation.bottom + verticalSpacing)
                    assertThat(newFirstPageLocation.left).isEqualTo(firstPageLocation.left)
                }
            }
            close()
        }
    }

    @Test
    fun testRerenderOnHorizontalSpacingChange() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        val horizontalSpacing = 20f
        val verticalSpacing = 20f
        // Initial layout setup.
        setupPdfView(1200, 1000, pdfDocument, PdfView.TWO_PAGE, horizontalSpacing, verticalSpacing)

        lateinit var firstPageLocation: RectF
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Verify initial layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                val expectedMaxWidth = 500 + 20f + 500
                assertThat(pdfView.contentWidth).isEqualTo(expectedMaxWidth)
                pdfView.pageLayoutManager?.let {
                    firstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                }
            }

            // Change horizontal spacing
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.horizontalPageSpacing = 40f
            }

            // Verify new layout
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                val expectedMaxWidth = 500 + 40f + 500
                assertThat(pdfView.contentWidth).isEqualTo(expectedMaxWidth)
                pdfView.pageLayoutManager?.let {
                    val newFirstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                    assertThat(newFirstPageLocation.left).isEqualTo(firstPageLocation.left + 20f)
                }
            }
            close()
        }
    }

    @Test
    fun testRerenderOnVerticalSpacingChange() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(500, 200) })
        setupPdfView(1200, 1000, pdfDocument, PdfView.SINGLE_PAGE, 5f, 10f)

        lateinit var firstPageLocation: RectF
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Get initial page location
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.pageLayoutManager?.let {
                    firstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                }
            }

            // Change vertical spacing
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.verticalPageSpacing = 30f
            }

            // Verify new page location
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                pdfView.pageLayoutManager?.let {
                    val newFirstPageLocation =
                        it.getPageLocation(1, pdfView.getVisibleAreaInContentCoords())
                    assertThat(newFirstPageLocation.top).isEqualTo(firstPageLocation.top + 20f)
                }
            }
            close()
        }
    }

    @Test
    fun testOrientationChange_restoresState() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(600, 1000) })
        setupPdfView(1220, 1000, pdfDocument, PdfView.TWO_PAGE, 20f, 15f)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            // Verify initial layout and go to a page
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(TwoPageLayoutStrategy::class.java)

                // Go to page 4 (0-indexed).
                pdfView.scrollToPage(4)
            }

            // Verify if pdfView has scrolled to page 4.
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.firstVisiblePage).isEqualTo(4)
            }

            // Change orientation to landscape
            onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }

            // Verify layout and page number are restored
            onActivity {
                val pdfView = it.findViewById<PdfView>(PDF_VIEW_ID)
                assertThat(pdfView.pageLayoutManager?.layoutStrategy)
                    .isInstanceOf(TwoPageLayoutStrategy::class.java)

                // Check for page number restoration.
                assertThat(pdfView.firstVisiblePage).isEqualTo(4)
            }
            close()
        }
    }

    companion object {
        private const val PDF_VIEW_ID = 123456789

        /** Create, measure, and layout a [PdfView] at the specified [width] and [height] */
        private fun setupPdfView(
            width: Int,
            height: Int,
            fakePdfDocument: FakePdfDocument?,
            pagesPerRow: Int,
            horizontalPageSpacing: Float,
            verticalPageSpacing: Float,
        ) {
            PdfViewTestActivity.onCreateCallback = { activity ->
                with(activity) {
                    container.addView(
                        PdfView(activity).apply {
                            this.pagesPerRow = pagesPerRow
                            this.horizontalPageSpacing = horizontalPageSpacing
                            this.verticalPageSpacing = verticalPageSpacing
                            pdfDocument = fakePdfDocument
                            id = PDF_VIEW_ID
                        },
                        ViewGroup.LayoutParams(width, height),
                    )
                }
            }
        }
    }
}
