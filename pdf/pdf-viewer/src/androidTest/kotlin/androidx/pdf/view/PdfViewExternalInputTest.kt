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

import android.content.ClipboardManager
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.pdf.PdfPoint
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.util.ZoomUtils
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.EspressoKey
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewExternalInputTest {

    @Before
    fun setUp() {
        val textContents =
            FAKE_PAGE_TEXT.map { text ->
                PdfPageTextContent(listOf(RectF(0f, 0f, 2000f, 4000f)), text)
            }
        val fakePdfDocument =
            FakePdfDocument(pages = List(10) { Point(2000, 4000) }, textContents = textContents)
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        isFocusable = true
                        isFocusableInTouchMode = true
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
    fun testDpadDown_scrollsDown() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the top to have space to scroll down.
                    pdfView.scrollTo(pdfView.scrollX, 0)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_DOWN))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, KEYBOARD_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(expectedScrollDelta)

        // Verify that the view did not scroll horizontally.
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testDpadLeft_zoomMoreThanFitToWidth_scrollsLeft() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var fitToWidthZoom: Float
        val initialScrollX = 500
        var finalZoomGreaterThanFitToWidth = false

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        // Request focus to receive key events.
                        requestFocus()
                        // find fit to width zoom
                        fitToWidthZoom =
                            ZoomUtils.calculateZoomToFit(
                                viewportWidth.toFloat(),
                                viewportHeight.toFloat(),
                                contentWidth,
                                1f,
                            )
                        // Zoom in to ensure the content is larger than the view, making it
                        // scrollable.
                        zoom = fitToWidthZoom * 2f
                        if (zoom > fitToWidthZoom) finalZoomGreaterThanFitToWidth = true
                        // Scroll right initially so there is space to scroll left.
                        scrollTo(initialScrollX, pdfView.scrollY)
                    }
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_LEFT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Verify that the view scrolled horizontally
        if (finalZoomGreaterThanFitToWidth) assertThat(scrollAfter.x).isLessThan(scrollBefore.x)

        // Verify that the view did not scroll vertically.
        assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testDpadLeft_zoomLessOrEqualToFitToWidth_AtTopOfPage_scrollToPreviousPage() {
        var fitToWidthZoom: Float
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var isFinalZoomLessOrEqualToFitToWidth = false
        var scrollForPreviousPageTop = -1

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        // Request focus to receive key events.
                        requestFocus()
                        // find fit to width zoom
                        fitToWidthZoom =
                            ZoomUtils.calculateZoomToFit(
                                viewportWidth.toFloat(),
                                viewportHeight.toFloat(),
                                contentWidth,
                                1f,
                            )
                        zoom = fitToWidthZoom - 0.1f
                        if (zoom <= fitToWidthZoom) isFinalZoomLessOrEqualToFitToWidth = true

                        scrollToPosition(PdfPoint(1, 0f, 0f))
                    }
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_LEFT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val pageRect =
                        pdfView.pageLayoutManager?.getPageLocation(
                            0,
                            pdfView.getVisibleAreaInContentCoords(),
                        )
                    if (pageRect != null) {
                        // Calculate y to align top of page with top of viewport.
                        scrollForPreviousPageTop = (pageRect.top * pdfView.zoom).roundToInt()
                    }
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        if (isFinalZoomLessOrEqualToFitToWidth) {
            assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
            assertThat(scrollAfter.y).isEqualTo(scrollForPreviousPageTop)
        }
    }

    @Test
    fun testDpadLeft_zoomLessOrEqualToFitToWidth_beyondTopOfPage_scrollToPreviousPage() {
        var fitToWidthZoom: Float
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var isFinalZoomLessOrEqualToFitToWidth = false
        var scrollForPreviousPageTop = -1

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        // Request focus to receive key events.
                        requestFocus()
                        // find fit to width zoom
                        fitToWidthZoom =
                            ZoomUtils.calculateZoomToFit(
                                viewportWidth.toFloat(),
                                viewportHeight.toFloat(),
                                contentWidth,
                                1f,
                            )
                        zoom = fitToWidthZoom - 0.1f
                        if (zoom <= fitToWidthZoom) isFinalZoomLessOrEqualToFitToWidth = true
                        scrollToPosition(PdfPoint(1, 0f, 10f), ScrollAlignment.TOP)
                    }
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_LEFT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val pageRect =
                        pdfView.pageLayoutManager?.getPageLocation(
                            1,
                            pdfView.getVisibleAreaInContentCoords(),
                        )
                    if (pageRect != null) {
                        // Calculate y to align top of page with top of viewport.
                        scrollForPreviousPageTop = (pageRect.top * pdfView.zoom).roundToInt()
                    }
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        if (isFinalZoomLessOrEqualToFitToWidth) {
            assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
            assertThat(scrollAfter.y).isEqualTo(scrollForPreviousPageTop)
        }
    }

    @Test
    fun testDpadRight_zoomMoreThanFitToWidth_scrollsRight() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var fitToWidthZoom: Float
        var finalZoomGreaterThanFitToWidth = false

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        // Request focus to receive key events.
                        requestFocus()
                        // find fit to width zoom
                        fitToWidthZoom =
                            ZoomUtils.calculateZoomToFit(
                                viewportWidth.toFloat(),
                                viewportHeight.toFloat(),
                                contentWidth,
                                1f,
                            )
                        // Zoom in to ensure the content is larger than the view, making it
                        // scrollable.
                        zoom = fitToWidthZoom * 2f
                        if (zoom > fitToWidthZoom) finalZoomGreaterThanFitToWidth = true
                        // Scroll left initially so there is space to scroll left.
                        scrollTo(0, pdfView.scrollY)
                    }
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Verify that the view scrolled horizontally.
        if (finalZoomGreaterThanFitToWidth) assertThat(scrollAfter.x).isGreaterThan(scrollBefore.x)

        // Verify that the view did not scroll vertically.
        assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testDpadRight_zoomLessOrEqualToFitToWidth_scrollToNextPage() {
        var fitToWidthZoom: Float
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var isFinalZoomLessOrEqualToFitToWidth = false
        var scrollForPreviousPageTop = -1

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        // Request focus to receive key events.
                        requestFocus()
                        // find fit to width zoom
                        fitToWidthZoom =
                            ZoomUtils.calculateZoomToFit(
                                viewportWidth.toFloat(),
                                viewportHeight.toFloat(),
                                contentWidth,
                                1f,
                            )
                        zoom = fitToWidthZoom - 0.1f
                        if (zoom <= fitToWidthZoom) isFinalZoomLessOrEqualToFitToWidth = true
                    }
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val pageRect =
                        pdfView.pageLayoutManager?.getPageLocation(
                            1,
                            pdfView.getVisibleAreaInContentCoords(),
                        )
                    if (pageRect != null) {
                        // Calculate y to align top of page with top of viewport.
                        scrollForPreviousPageTop = (pageRect.top * pdfView.zoom).roundToInt()
                    }
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        if (isFinalZoomLessOrEqualToFitToWidth) {
            assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
            assertThat(scrollAfter.y).isEqualTo(scrollForPreviousPageTop)
        }
    }

    @Test
    fun testDpadUp_scrollsUp() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0
        val initialScrollY = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Request focus to receive key events.
                    pdfView.post { pdfView.requestFocus() }
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll down initially so there is space to scroll up.
                    pdfView.scrollTo(pdfView.scrollX, initialScrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                .perform(ViewActions.pressKey(KeyEvent.KEYCODE_DPAD_UP))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, KEYBOARD_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(-expectedScrollDelta)

        // Verify that the view did not scroll horizontally.
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testMouseScrollDown_scrollsDown() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the top to have space to scroll down.
                    pdfView.scrollTo(pdfView.scrollX, 0)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                // Negative vscroll for scroll down
                .scrollMouseWheel(-1.0f, 0f)
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, MOUSE_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(expectedScrollDelta)
        // Verify that the view did not scroll horizontally.
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testMouseScrollLeft_scrollsLeft() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0
        val initialScrollX = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll right initially so there is space to scroll left.
                    pdfView.scrollTo(initialScrollX, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                // Negative hscroll for scroll left
                .scrollMouseWheel(0f, -1.0f)
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, MOUSE_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(-expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testMouseScrollRight_scrollsRight() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportWidth = 0

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Ensure we are at the far left to have space to scroll right.
                    pdfView.scrollTo(0, pdfView.scrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportWidth = pdfView.viewportWidth
                }
                // Positive hscroll for scroll right
                .scrollMouseWheel(0f, 1.0f)
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportWidth, MOUSE_HORIZONTAL_SCROLL_FACTOR)

        // Verify that the view scrolled horizontally by the correct amount.
        assertThat(scrollAfter.x - scrollBefore.x).isEqualTo(expectedScrollDelta)

        // Verify that the view did not scroll vertically.
        assertThat(scrollAfter.y).isEqualTo(scrollBefore.y)
    }

    @Test
    fun testMouseScrollUp_scrollsUp() {
        var scrollBefore = Point(Int.MAX_VALUE, Int.MAX_VALUE)
        var scrollAfter = Point(Int.MIN_VALUE, Int.MIN_VALUE)
        var viewportHeight = 0
        val initialScrollY = 500

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Zoom in to ensure the content is larger than the view, making it scrollable.
                    pdfView.zoom = 2.0f
                    // Scroll down initially so there is space to scroll up.
                    pdfView.scrollTo(pdfView.scrollX, initialScrollY)
                    scrollBefore = Point(pdfView.scrollX, pdfView.scrollY)
                    viewportHeight = pdfView.viewportHeight
                }
                // Positive vscroll for scroll up
                .scrollMouseWheel(1.0f, 0f)
                .check { view, _ ->
                    val pdfView = view as PdfView
                    scrollAfter = Point(pdfView.scrollX, pdfView.scrollY)
                }
            close()
        }

        // Calculate the expected scroll distance based on the helper utility.
        val expectedScrollDelta =
            ExternalInputUtils.calculateScroll(viewportHeight, MOUSE_VERTICAL_SCROLL_FACTOR)

        // Verify that the view scrolled vertically by the correct amount.
        assertThat(scrollAfter.y - scrollBefore.y).isEqualTo(-expectedScrollDelta)
        // Verify that the view did not scroll horizontally.
        assertThat(scrollAfter.x).isEqualTo(scrollBefore.x)
    }

    @Test
    fun testCtrlZero_ifZoomAboveBaseline_zoomsToDefault() {
        var defaultZoom = 0f
        var zoomAfter = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    defaultZoom = pdfView.getFitToWidthZoom()

                    pdfView.zoom = pdfView.maxZoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_0)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                }
            close()
        }
        assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(defaultZoom)
    }

    @Test
    fun testCtrlZero_ifZoomBelowBaseline_zoomsToBaseline() {
        var defaultZoom = 0f
        var zoomAfter = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    defaultZoom = pdfView.getFitToWidthZoom()

                    pdfView.zoom = pdfView.minZoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_0)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                }
            close()
        }
        assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(defaultZoom)
    }

    @Test
    fun testCtrlNumpadZero_ifZoomAboveBaseline_zoomsToBaseline() {
        var defaultZoom = 0f
        var zoomAfter = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    defaultZoom = pdfView.getFitToWidthZoom()

                    pdfView.zoom = pdfView.maxZoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_0)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                }
            close()
        }
        assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(defaultZoom)
    }

    @Test
    fun testCtrlNumpadZero_ifZoomBelowBaseline_zoomsToBaseline() {
        var defaultZoom = 0f
        var zoomAfter = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    defaultZoom = pdfView.getFitToWidthZoom()

                    pdfView.zoom = pdfView.minZoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_0)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                }
            close()
        }
        assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(defaultZoom)
    }

    @Test
    fun testCtrlEquals_zoomsIn() {
        var zoomBefore = 0f
        var zoomAfter = 0f
        var maxZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    zoomBefore = pdfView.zoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_EQUALS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                    maxZoom = pdfView.maxZoom
                }
            close()
        }

        // zoomAfter should be greater than zoomBefore only if zoomBefore is smaller than maxZoom
        if (zoomBefore < maxZoom) {
            assertThat(zoomAfter).isGreaterThan(zoomBefore)
        } else {
            assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(maxZoom)
        }
    }

    @Test
    fun testCtrlPlus_zoomsIn() {
        var zoomBefore = 0f
        var zoomAfter = 0f
        var maxZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    zoomBefore = pdfView.zoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_PLUS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                    maxZoom = pdfView.maxZoom
                }
            close()
        }

        // zoomAfter should be greater than zoomBefore only if zoomBefore is smaller than maxZoom
        if (zoomBefore < maxZoom) {
            assertThat(zoomAfter).isGreaterThan(zoomBefore)
        } else {
            assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(maxZoom)
        }
    }

    @Test
    fun testCtrlMinus_zoomsOut() {
        var zoomBefore = 0f
        var zoomAfter = 0f
        var minZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    zoomBefore = pdfView.zoom
                }
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_MINUS)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                    minZoom = pdfView.minZoom
                }
            close()
        }

        // zoomAfter should be smaller than zoomBefore only if zoomBefore is greater than minZoom
        if (zoomBefore > minZoom) {
            assertThat(zoomBefore).isGreaterThan(zoomAfter)
        } else {
            assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(minZoom)
        }
    }

    @Test
    fun testCtrlPlusMouseScrollDown_zoomsIn() {
        var zoomBefore = 0f
        var zoomAfter = 0f
        var maxZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    zoomBefore = pdfView.zoom
                }
                .scrollMouseWheel(-1.0f, 0f, KeyEvent.META_CTRL_ON)
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                    maxZoom = pdfView.maxZoom
                }
            close()
        }

        // zoomAfter should be greater than zoomBefore only if zoomBefore is smaller than maxZoom
        if (zoomBefore < maxZoom) {
            assertThat(zoomAfter).isGreaterThan(zoomBefore)
        } else {
            assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(maxZoom)
        }
    }

    @Test
    fun testCtrlPlusMouseScrollUp_zoomsOut() {
        var zoomBefore = 0f
        var zoomAfter = 0f
        var minZoom = 0f

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }

                    zoomBefore = pdfView.zoom
                }
                .scrollMouseWheel(1.0f, 0f, KeyEvent.META_CTRL_ON)
                .check { view, _ ->
                    val pdfView = view as PdfView

                    zoomAfter = pdfView.zoom
                    minZoom = pdfView.minZoom
                }
            close()
        }

        // zoomAfter should be smaller than zoomBefore only if zoomBefore is greater than minZoom
        if (zoomBefore > minZoom) {
            assertThat(zoomBefore).isGreaterThan(zoomAfter)
        } else {
            assertThat(zoomAfter).isWithin(ZOOM_DIFFERENCE_TOLERANCE).of(minZoom)
        }
    }

    @Test
    fun testCtrlC_copiesSelectionToClipboardAndClearsSelection() {
        val expectedSelectedText = FAKE_PAGE_TEXT[0]

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.post { pdfView.requestFocus() }
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
                .check { view, _ ->
                    val pdfView = view as PdfView
                    // Verify that the long press selected the correct word.
                    val selectedText =
                        (pdfView.currentSelection as? TextSelection)?.text?.toString()
                    assertThat(selectedText).isEqualTo(expectedSelectedText)
                }
                // Perform the Ctrl+C key press.
                .perform(
                    ViewActions.pressKey(
                        EspressoKey.Builder()
                            .withKeyCode(KeyEvent.KEYCODE_C)
                            .withCtrlPressed(true)
                            .build()
                    )
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val clipboard = pdfView.context.getSystemService(ClipboardManager::class.java)

                    // Verify the clipboard content.
                    val clip = clipboard.primaryClip
                    assertThat(clip).isNotNull()
                    assertThat(clip!!.itemCount).isGreaterThan(0)
                    val clipboardText = clip.getItemAt(0).text
                    assertThat(clipboardText).isEqualTo(expectedSelectedText)

                    // Verify that the selection was cleared after copying.
                    assertThat(pdfView.currentSelection).isNull()
                }
            close()
        }
    }

    @Test
    fun mouseDrag_selectsContentBetweenStartAndEnd_samePage() {
        var start: PointF? = PointF(-1f, -1f)
        var end: PointF? = PointF(-1f, -1f)

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        isAutoScrollingEnabled = false
                        start = pdfToViewPoint(PdfPoint(0, 200f, 400f))
                        end = pdfToViewPoint(PdfPoint(0, 300f, 400f))
                        assertThat(start).isNotNull()
                        assertThat(end).isNotNull()
                    }
                }
                .drag(
                    start!!.x,
                    start!!.y,
                    end!!.x,
                    end!!.y,
                    MotionEvent.BUTTON_PRIMARY,
                    0,
                    InputDevice.SOURCE_MOUSE,
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val selection = pdfView.currentSelection
                    assertThat(selection).isNotNull()
                    val selectedPages = selection?.bounds?.map { it.pageNum }?.toSet()
                    assertThat(selectedPages).isNotNull()
                    assertThat(selectedPages).contains(0)
                }
            close()
        }
    }

    @Test
    fun mouseDrag_selectsContentBetweenStartAndEnd_differentPages() {
        var start: PointF? = null
        var end: PointF? = null

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        isAutoScrollingEnabled = false
                        // zoom out to get both the pages within the viewport
                        zoom = 0.1f
                        // Start point on page 0
                        start = pdfToViewPoint(PdfPoint(0, 1000f, 1000f))
                        // End point on page 1
                        end = pdfToViewPoint(PdfPoint(1, 1000f, 400f))
                        assertThat(start).isNotNull()
                        assertThat(end).isNotNull()
                    }
                }
                .drag(
                    start!!.x,
                    start!!.y,
                    end!!.x,
                    end!!.y,
                    MotionEvent.BUTTON_PRIMARY,
                    0,
                    InputDevice.SOURCE_MOUSE,
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val selection = pdfView.currentSelection
                    assertThat(selection).isNotNull()
                    val selectedPages = selection?.bounds?.map { it.pageNum }?.toSet()
                    assertThat(selectedPages).isNotNull()
                    assertThat(selectedPages).contains(0)
                    assertThat(selectedPages).contains(1)
                }
            close()
        }
    }

    @Test
    fun mouseDrag_selectsContentInReverse_differentPages() {
        assumeFalse(
            "Test fails on cuttlefish b/465537830",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
        var start: PointF? = null
        var end: PointF? = null

        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    with(pdfView) {
                        isAutoScrollingEnabled = false
                        // zoom out to get both the pages within the viewport
                        zoom = 0.1f
                        // Start point on page 1
                        start = pdfToViewPoint(PdfPoint(1, 1000f, 400f))
                        // End point on page 0
                        end = pdfToViewPoint(PdfPoint(0, 1000f, 1000f))
                        assertThat(start).isNotNull()
                        assertThat(end).isNotNull()
                    }
                }
                .drag(
                    start!!.x,
                    start!!.y,
                    end!!.x,
                    end!!.y,
                    MotionEvent.BUTTON_PRIMARY,
                    0,
                    InputDevice.SOURCE_MOUSE,
                )
                .check { view, _ ->
                    val pdfView = view as PdfView
                    val selection = pdfView.currentSelection
                    assertThat(selection).isNotNull()
                    val selectedPages = selection?.bounds?.map { it.pageNum }?.toSet()
                    assertThat(selectedPages).isNotNull()
                    assertThat(selectedPages).contains(0)
                    assertThat(selectedPages).contains(1)
                }
            close()
        }
    }

    private companion object {
        /** Arbitrary fixed ID for PdfView */
        private const val PDF_VIEW_ID = 123456789
        private const val ZOOM_DIFFERENCE_TOLERANCE = 0.001f
        const val KEYBOARD_VERTICAL_SCROLL_FACTOR = 20
        const val KEYBOARD_HORIZONTAL_SCROLL_FACTOR = 20
        const val MOUSE_VERTICAL_SCROLL_FACTOR = 14
        const val MOUSE_HORIZONTAL_SCROLL_FACTOR = 14
        // A map of page numbers to their text content for the fake document.
        val FAKE_PAGE_TEXT = (0..9).map { "FAKE_PAGE_TEXT $it" }
    }
}
