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

import android.graphics.RectF
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.pdf.R
import androidx.pdf.view.PdfViewAccessibilityManager.Companion.FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET
import androidx.pdf.view.fastscroll.getDimensions
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewAccessibilityManagerTest {
    private lateinit var pdfView: PdfView
    private lateinit var activityScenario: ActivityScenario<PdfViewTestActivity>

    private val pdfDocument = FakePdfDocument.newInstance()

    @Before
    fun setupPdfView() {
        // Setup the test activity to host the PdfView
        PdfViewTestActivity.onCreateCallback = { activity ->
            val container =
                FrameLayout(activity).apply {
                    addView(
                        PdfView(activity).apply {
                            isFormFillingEnabled = true
                            this.pdfDocument = pdfDocument
                            id = PDF_VIEW_ID
                        },
                        ViewGroup.LayoutParams(100, 1000),
                    )
                }
            activity.setContentView(container)
        }

        activityScenario =
            ActivityScenario.launch(PdfViewTestActivity::class.java).onActivity { activity ->
                pdfView = activity.findViewById<PdfView>(PDF_VIEW_ID)
                requireNotNull(pdfView) { "PdfView must not be null." }
                pdfView.isAccessibilityEnabled = true
                pdfView.pdfDocument = pdfDocument
            }
    }

    @After
    fun closeActivityScenario() {
        activityScenario.close()

        // Reset the fast scroller visibility
        pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        pdfView.lastFastScrollerVisibility = false
    }

    @Test
    fun getVirtualViewAt_returnsCorrectPageLinkAndFormWidgets() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Round to an integer before using in any test case, i.e. matching the logic that makes
        // use of this value
        val topPageMargin = pdfView.context.getDimensions(R.dimen.top_page_margin).roundToInt()

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 2)

        // Test cases with content coordinates and expected page indices
        val testCases =
            listOf(
                Triple(25f, 25f, 0), // Maps to page 0
                Triple(25f, 250f, 1), // Maps to page 1
                Triple(0f, topPageMargin.toFloat(), 0), // Maps to the very start of the first page
                Triple(0f, 100f, 0), // Edge of the first page
                Triple(110f, 25f, -1), // Outside valid page bounds
                Triple(-10f, -10f, -1), // Outside viewport
                Triple(50f, 40f, 10), // Goto Link
                Triple(50f, 70f, 11), // External Link
                Triple(75f, 550f, FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET),
                Triple(75f, 475f, FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET + 1),
            )

        testCases.forEach { (x, y, expectedVirtualViewId) ->
            val adjustedX = PdfView.toViewCoord(x, pdfView.zoom, pdfView.scrollX)
            val adjustedY = PdfView.toViewCoord(y, pdfView.zoom, pdfView.scrollY)

            assertThat(pdfViewAccessibilityManager.getVirtualViewAt(adjustedX, adjustedY))
                .isEqualTo(expectedVirtualViewId)
        }
    }

    @Test
    fun getVisibleVirtualViews_returnsCorrectPagesLinksAndFormWidgets() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 4)
        Espresso.onView(withId(PDF_VIEW_ID))
            .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 5)

        // Enabling Fast Scroller Visibility
        pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        pdfView.lastFastScrollerVisibility = true

        val visiblePagesAndLinks = mutableListOf<Int>()
        pdfViewAccessibilityManager.getVisibleVirtualViews(visiblePagesAndLinks)
        assertThat(visiblePagesAndLinks)
            .isEqualTo(
                listOf(
                    0,
                    1,
                    2,
                    3,
                    4,
                    10,
                    11,
                    FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET,
                    FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET + 1,
                    1000002,
                    1000003,
                )
            )
    }

    @Test
    fun onPopulateNodeForVirtualView_setsCorrectContentDescriptionAndFocusability() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 4)
        Espresso.onView(withId(PDF_VIEW_ID))
            .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 5)

        val testCases =
            listOf(
                1 to "Page 2: Sample text for page 2",
                10 to "Go to page 5",
                11 to "Link: www.example.com",
                FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET to
                    "Form widget Type: Radio Button. Title: Radio. Current value: false. Click will toggle value.",
                FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET + 1 to
                    "Form widget Type: Multi select List Box. Title: ListBox. Value: Banana. Widget is read only and cannot be changed.",
            )
        testCases.forEach { (virtualViewId, expectedDescription) ->
            val node = mock(AccessibilityNodeInfoCompat::class.java)
            pdfViewAccessibilityManager.onPopulateNodeForVirtualView(virtualViewId, node)

            verify(node).contentDescription = expectedDescription
            verify(node).isFocusable = true
        }
    }

    @Test
    fun onPopulateNodeForVirtualView_setsCorrectBounds() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        val topPageMargin = pdfView.context.getDimensions(R.dimen.top_page_margin)
        val pageSpacing = pdfView.context.getDimensions(R.dimen.page_spacing)

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 0)

        val testCases =
            listOf(
                0 to RectF(0f, topPageMargin, 100f, 200f + topPageMargin),
                10 to RectF(25f, 30f + topPageMargin, 75f, 50f + topPageMargin),
                11 to RectF(25f, 60f + topPageMargin, 75f, 80f + topPageMargin),
                FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET to
                    RectF(50f, 500f + topPageMargin, 100f, 600f + topPageMargin),
                FORM_WIDGET_VIRTUAL_VIEW_ID_OFFSET + 1 to
                    RectF(
                        50f,
                        400f + topPageMargin + (pdfDocument.pages[0]?.y ?: 0) + pageSpacing,
                        100f,
                        550f + topPageMargin + (pdfDocument.pages[0]?.y ?: 0) + pageSpacing,
                    ),
            )

        testCases.forEach { (virtualViewId, boundsInParent) ->
            val node = mock(AccessibilityNodeInfoCompat::class.java)
            val expectedBounds =
                pdfViewAccessibilityManager.scalePageBounds(boundsInParent, pdfView.zoom)

            pdfViewAccessibilityManager.onPopulateNodeForVirtualView(virtualViewId, node)
            verify(node).let {
                pdfViewAccessibilityManager.setBoundsInScreenFromBoundsInParent(
                    node,
                    expectedBounds,
                )
            }
        }
    }

    @Test
    fun onPageTextReady_updatesAccessibilityNode() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 1)

        val node = mock(AccessibilityNodeInfoCompat::class.java)
        var virtualViewId = 0 // Page 1

        pdfViewAccessibilityManager.onPageTextReady(virtualViewId)

        // Verify content description is set as expected for Page 1
        pdfViewAccessibilityManager.onPopulateNodeForVirtualView(virtualViewId, node)
        verify(node).contentDescription = "Page 1: Sample text for page 1"

        // Verify default content description for non-visible page
        virtualViewId = 7 // Page 8
        pdfViewAccessibilityManager.onPopulateNodeForVirtualView(virtualViewId, node)
        verify(node).contentDescription = "Page 8" // Default value
    }

    @Test
    fun onPopulateNodeForVirtualView_fastScrollerThumb() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 0)

        // Enabling Fast Scroller Visibility
        pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        pdfView.lastFastScrollerVisibility = true

        val node = mock(AccessibilityNodeInfoCompat::class.java)
        val thumbVirtualId = PdfViewAccessibilityManager.FAST_SCROLLER_OFFSET + 1
        pdfViewAccessibilityManager.onPopulateNodeForVirtualView(thumbVirtualId, node)
        verify(node).contentDescription = "Scroll Bar"
        verify(node).isFocusable = true
    }

    @Test
    fun onPopulateNodeForVirtualView_fastScrollerPageIndicator() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 0)

        // Enabling Fast Scroller Visibility
        pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        pdfView.lastFastScrollerVisibility = true

        val node = mock(AccessibilityNodeInfoCompat::class.java)
        val pageIndicatorVirtualId = PdfViewAccessibilityManager.FAST_SCROLLER_OFFSET + 2
        pdfViewAccessibilityManager.onPopulateNodeForVirtualView(pageIndicatorVirtualId, node)
        verify(node).isFocusable = true
    }

    @Test
    fun getVisibleVirtualViews_fastScrollerElements() = runTest {
        val pdfViewAccessibilityManager =
            requireNotNull(pdfView.pdfViewAccessibilityManager) {
                "PdfViewAccessibilityManager must not be null."
            }

        // Wait until layout completes for the required pages
        pdfDocument.waitForLayout(untilPage = 4)
        Espresso.onView(withId(PDF_VIEW_ID))
            .checkPagesAreVisible(firstVisiblePage = 0, visiblePages = 5)

        // Enabling Fast Scroller Visibility
        pdfView.fastScrollVisibility = PdfView.FastScrollVisibility.ALWAYS_SHOW
        pdfView.lastFastScrollerVisibility = true

        val visibleViews = mutableListOf<Int>()
        pdfViewAccessibilityManager.getVisibleVirtualViews(visibleViews)

        assertThat(visibleViews).contains(PdfViewAccessibilityManager.FAST_SCROLLER_OFFSET + 1)
        assertThat(visibleViews).contains(PdfViewAccessibilityManager.FAST_SCROLLER_OFFSET + 2)
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
