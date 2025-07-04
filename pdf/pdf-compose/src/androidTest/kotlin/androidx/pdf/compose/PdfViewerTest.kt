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

package androidx.pdf.compose

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.pdf.PdfPoint
import androidx.pdf.view.PdfView
import androidx.pdf.view.Selection
import androidx.pdf.view.TextSelection
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewerTest {
    @get:Rule val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun pdfViewerState_noDocument_defaults() {
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(state = pdfViewerState, pdfDocument = null)
        }

        assertThat(pdfViewerState.firstVisiblePage).isEqualTo(0)
        assertThat(pdfViewerState.visiblePagesCount).isEqualTo(0)
        assertThat(pdfViewerState.zoom).isEqualTo(PdfView.DEFAULT_INIT_ZOOM)
    }

    @Test
    fun pdfViewerState_defaults() {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 550) })

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 1100.toDp(context)),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Wait for fit to zoom, which means the initial pages have been laid out
        rule.waitUntil { pdfViewerState.zoom == 2.0F }
        assertThat(pdfViewerState.firstVisiblePage).isEqualTo(0)
        assertThat(pdfViewerState.visiblePagesCount).isEqualTo(1)
        // By default, PdfViewer will fit the content to its own width. Since we configured the View
        // to be exactly twice as wide as the Composable, we expect zoom to be 2.0
        assertThat(pdfViewerState.zoom).isEqualTo(2.0F)
    }

    @Test
    fun pdfViewerState_scrollUpAndDown() {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 550) })
        val pageZeroTopPositions = FloatArray(3)

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }
        // Wait for fit to zoom, which means the initial pages have been laid out
        rule.waitUntil { pdfViewerState.zoom == 2.0F }
        pageZeroTopPositions[0] = requireNotNull(pdfViewerState.getVisiblePageOffset(0)).y

        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput {
            swipeUp()
            // Click will stop a fling, if one starts, i.e. to make it easier to reason about where
            // we expect the page to be
            click()
        }
        pageZeroTopPositions[1] = requireNotNull(pdfViewerState.getVisiblePageOffset(0)).y

        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput {
            swipeDown()
            // Click will stop a fling, if one starts, i.e. to make it easier to reason about where
            // we expect the page to be
            click()
        }
        pageZeroTopPositions[2] = requireNotNull(pdfViewerState.getVisiblePageOffset(0)).y

        // Swipe up -> page 0 has a negative offset (i.e. above the top of the Composable)
        assertThat(pageZeroTopPositions[1]).isLessThan(pageZeroTopPositions[0])
        // Swipe down -> page 0 offset becomes more positive (i.e. moves towards the bottom of the
        // Composable)
        assertThat(pageZeroTopPositions[2]).isGreaterThan(pageZeroTopPositions[1])
    }

    @Test
    fun pdfViewerState_coordinateTranslation() {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        val topPageMarginPx = context.resources.getDimension(androidx.pdf.R.dimen.top_page_margin)
        val pageSpacingPx = context.resources.getDimension(androidx.pdf.R.dimen.page_spacing)

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }
        // Wait for fit to zoom, which means the initial pages have been laid out
        rule.waitUntil { pdfViewerState.zoom == 2.0F }

        // 2 x top margin to account for zoom
        val pageZeroTop = topPageMarginPx * 2
        val pageZeroTopLeft = PdfPoint(pageNum = 0, pagePoint = PointF(0F, 0F))
        val pageZeroTopLeftCompose = Offset(0F, pageZeroTop)
        // Cross check each page coordinate API against each other
        assertThat(pdfViewerState.visibleOffsetToPdfPoint(pageZeroTopLeftCompose))
            .isEqualTo(pageZeroTopLeft)
        assertThat(pdfViewerState.pdfPointToVisibleOffset(pageZeroTopLeft))
            .isEqualTo(pageZeroTopLeftCompose)
        assertThat(pdfViewerState.getVisiblePageOffset(0)).isEqualTo(pageZeroTopLeftCompose)

        // 2 x page height and spacing to account for zoom
        val pageOneTop = pageZeroTop + 450F + pageSpacingPx * 2
        val pageOneTopLeft = PdfPoint(pageNum = 1, pagePoint = PointF(0F, 0F))
        val pageOneTopLeftCompose = Offset(0F, pageOneTop)
        // Cross check each page coordinate API against each other
        assertThat(pdfViewerState.visibleOffsetToPdfPoint(pageOneTopLeftCompose))
            .isEqualTo(pageOneTopLeft)
        assertThat(pdfViewerState.pdfPointToVisibleOffset(pageOneTopLeft))
            .isEqualTo(pageOneTopLeftCompose)
        assertThat(pdfViewerState.getVisiblePageOffset(1)).isEqualTo(pageOneTopLeftCompose)
    }

    @Test
    fun pdfViewerState_scrollToPage() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        withContext(Dispatchers.Main) { pdfViewerState.scrollToPage(9) }
        rule.waitUntil { pdfViewerState.firstVisiblePage >= 8 }

        val visiblePageRange =
            pdfViewerState.firstVisiblePage until
                (pdfViewerState.firstVisiblePage + pdfViewerState.visiblePagesCount)
        assertThat(visiblePageRange.contains(9)).isTrue()
    }

    @Test
    fun pdfViewerState_scrollToPage_cancelDuringUserInteraction() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Perform a very fast swipe, i.e. a fling, so the UI is still setting when we attempt
        // scrollToPage, below
        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { swipeUp(durationMillis = 20) }
        var programmaticScrollCancelled = false
        withContext(Dispatchers.Main) {
            try {
                pdfViewerState.scrollToPage(9)
            } catch (ex: CancellationException) {
                programmaticScrollCancelled = true
            }
        }

        assertThat(programmaticScrollCancelled).isTrue()
    }

    @Test
    fun pdfViewerState_scrollToPosition() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Scroll to the top left corner of page 10
        withContext(Dispatchers.Main) {
            pdfViewerState.scrollToPosition(PdfPoint(pageNum = 9, pagePoint = PointF(0F, 0F)))
        }
        rule.waitUntil { pdfViewerState.firstVisiblePage >= 8 }

        val visiblePageRange =
            pdfViewerState.firstVisiblePage until
                (pdfViewerState.firstVisiblePage + pdfViewerState.visiblePagesCount)
        assertThat(visiblePageRange.contains(9)).isTrue()
    }

    @Test
    fun pdfViewerState_scrollToPosition_cancelDuringUserInteraction() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Perform a very fast swipe, i.e. a fling, so the UI is still setting when we attempt
        // scrollToPosition, below
        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { swipeUp(durationMillis = 20) }
        var programmaticScrollCancelled = false
        withContext(Dispatchers.Main) {
            try {
                pdfViewerState.scrollToPosition(PdfPoint(pageNum = 9, pagePoint = PointF(0F, 0F)))
            } catch (ex: CancellationException) {
                programmaticScrollCancelled = true
            }
        }

        assertThat(programmaticScrollCancelled).isTrue()
    }

    @Test
    fun pdfViewerState_zoomScroll_zoomOnly() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        withContext(Dispatchers.Main) { pdfViewerState.zoomScroll { zoomTo(10F) } }

        assertThat(pdfViewerState.zoom).isEqualTo(10F)
    }

    @Test
    fun pdfViewerState_zoomScroll_scrollOnly() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        val pageZeroInitOffset = pdfViewerState.getVisiblePageOffset(0)?.copy() ?: OFFSET_UNSET
        val scrolled = Array(2) { OFFSET_UNSET }
        val scrollDown = Offset(0F, 20F)
        val scrollUp = Offset(0F, -50F)
        var pageZeroMidOffset = OFFSET_UNSET
        withContext(Dispatchers.Main) {
            pdfViewerState.zoomScroll {
                // Scroll down 20 pixels
                scrolled[0] = scrollBy(scrollDown)
                pageZeroMidOffset = pdfViewerState.getVisiblePageOffset(0)?.copy() ?: OFFSET_UNSET
                // Attempt to scroll back up 50 pixels
                scrolled[1] = scrollBy(scrollUp)
            }
        }
        val pageZeroFinalOffset = pdfViewerState.getVisiblePageOffset(0)?.copy()

        // We should have scrolled down by the full amount
        assertThat(scrolled[0]).isEqualTo(scrollDown)
        // We should only have scrolled up by 20 pixels, since we only scrolled down 20 initially
        assertThat(scrolled[1]).isEqualTo(Offset(0F, -20F))
        // After scrolling down, we expect page zero to be 20 pixels below it's initial position
        assertThat(pageZeroInitOffset.y - pageZeroMidOffset.y).isEqualTo(scrollDown.y)
        // After scrolling down then up, we expect page 0 to be at the same position as it started
        assertThat(pageZeroFinalOffset).isEqualTo(pageZeroInitOffset)
    }

    @Test
    fun pdfViewerState_zoomScroll_zoomAndScroll() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        val pageZeroInitOffset = pdfViewerState.getVisiblePageOffset(0) ?: OFFSET_UNSET
        var scrolled = OFFSET_UNSET
        val scrollDown = Offset(0F, 20F)
        var pageZeroMidOffset = OFFSET_UNSET
        withContext(Dispatchers.Main) {
            pdfViewerState.zoomScroll {
                // Scroll down 20 pixels
                scrolled = scrollBy(scrollDown)
                pageZeroMidOffset = pdfViewerState.getVisiblePageOffset(0)?.copy() ?: OFFSET_UNSET
                zoomTo(10F)
            }
        }

        // We should have scrolled down by the full amount
        assertThat(scrolled).isEqualTo(scrollDown)
        // After scrolling down, we expect page zero to be 20 pixels below it's initial position
        assertThat(pageZeroInitOffset.y - pageZeroMidOffset.y).isEqualTo(scrollDown.y)
        // Zoom should be what we set it to
        assertThat(pdfViewerState.zoom).isEqualTo(10F)
    }

    @Test
    fun pdfViewerState_zoomScroll_cancelDuringUserInteraction() = runTest {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Perform a very fast swipe, i.e. a fling, so the UI is still setting when we attempt
        // zoomScroll, below
        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { swipeUp(durationMillis = 20) }
        var programmaticZoomScrollCancelled = false
        withContext(Dispatchers.Main) {
            try {
                pdfViewerState.zoomScroll {
                    scrollBy(Offset(0F, 10F))
                    zoomTo(10F)
                }
            } catch (ex: CancellationException) {
                programmaticZoomScrollCancelled = true
            }
        }

        assertThat(programmaticZoomScrollCancelled).isTrue()
    }

    @Test
    fun pdfViewerState_userGestureState_idleByDefault() {
        val pdfDocument = FakePdfDocument(List(10) { Point(425, 225) })
        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        assertThat(pdfViewerState.gestureState).isEqualTo(PdfViewerState.GESTURE_STATE_IDLE)
    }

    @Test
    fun pdfViewerState_observeSelection() {
        val pdfDocument =
            FakePdfDocument(List(10) { Point(425, 225) }, pageSelector = SIMPLE_SELECTOR)
        val selections = mutableListOf<Selection?>()

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            // Only record the selection state when that state changes. Don't log it on every
            // Composition
            LaunchedEffect(pdfViewerState.currentSelection) {
                selections.add(pdfViewerState.currentSelection)
            }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // PdfViewer will adjust zoom to fit the width of the content. Once this has happened we
        // know the initial pages have been laid out.
        rule.waitUntil { pdfViewerState.zoom == 2.0F }
        // Somewhere around the middle of page 0
        val longClickPosition =
            requireNotNull(pdfViewerState.pdfPointToVisibleOffset(PdfPoint(0, PointF(212F, 112F))))
        // b/418866416 - longClick() doesn't work w/ Android Views, so we send a down event without
        // an up event and just wait.
        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { down(longClickPosition) }
        rule.waitUntil { selections.size > 1 }

        assertThat(selections.size).isEqualTo(2)
        assertThat(selections[0]).isNull()
        // Just make sure we have a text selection, i.e. the one produced by SIMPLE_SELECTOR
        // The bounds of the selection, text, etc are all dictated by FakePdfDocument and not worth
        // asserting on here.
        assertThat(selections[1]).isInstanceOf(TextSelection::class.java)
        val textSelection = selections[1] as TextSelection
        assertThat(textSelection.text).isEqualTo(SIMPLE_SELECTOR_STATIC_TEXT)
    }

    @Test
    fun pdfViewerState_clearSelection() {
        val pdfDocument =
            FakePdfDocument(List(10) { Point(425, 225) }, pageSelector = SIMPLE_SELECTOR)
        val selections = mutableListOf<Selection?>()

        lateinit var pdfViewerState: PdfViewerState
        rule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            // Only record the selection state when that state changes. Don't log it on every
            // Composition
            LaunchedEffect(pdfViewerState.currentSelection) {
                selections.add(pdfViewerState.currentSelection)
            }
            PdfViewer(
                modifier =
                    Modifier.requiredSize(width = 850.toDp(context), height = 550.toDp(context))
                        .testTag(PDF_VIEW_TAG),
                state = pdfViewerState,
                pdfDocument = pdfDocument,
            )
        }

        // Step 1: select content to clear, see pdfViewerState_observeSelection RE how this works
        rule.waitUntil { pdfViewerState.zoom == 2.0F }
        val longClickPosition =
            requireNotNull(pdfViewerState.pdfPointToVisibleOffset(PdfPoint(0, PointF(212F, 112F))))
        rule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { down(longClickPosition) }
        rule.waitUntil { selections.size > 1 }

        // Step 2: Clear the selection
        pdfViewerState.clearSelection()
        rule.waitUntil { selections.size > 2 }

        assertThat(selections.size).isEqualTo(3)
        // Nothing selected -> something selected -> nothing selected
        assertThat(selections[0]).isNull()
        assertThat(selections[1]).isInstanceOf(TextSelection::class.java)
        assertThat(selections[2]).isNull()
    }

    @Test
    fun fastScrollConfig_uniformDefaults() {
        val resIdsConfig = FastScrollConfiguration.withDrawableAndDimensionIds()
        val resIdsAndDpConfig = FastScrollConfiguration.withDrawableIdsAndDp()

        assertThat(resIdsConfig.pageIndicatorMarginEnd(context))
            .isEqualTo(resIdsAndDpConfig.pageIndicatorMarginEnd(context))
        assertThat(resIdsConfig.verticalThumbMarginEnd(context))
            .isEqualTo(resIdsAndDpConfig.verticalThumbMarginEnd(context))
        val resIdsPageIndicatorBitmap =
            resIdsConfig.pageIndicatorBackgroundDrawable(context).toBitmap(width = 10, height = 10)
        val resIdsAndDpIndicatorBitmap =
            resIdsAndDpConfig
                .pageIndicatorBackgroundDrawable(context)
                .toBitmap(width = 10, height = 10)
        assertThat(resIdsPageIndicatorBitmap.sameAs(resIdsAndDpIndicatorBitmap)).isTrue()
        val resIdsThumbBitmap =
            resIdsConfig.verticalThumbDrawable(context).toBitmap(width = 10, height = 10)
        val resIdsAndDpThumbBitmap =
            resIdsAndDpConfig.verticalThumbDrawable(context).toBitmap(width = 10, height = 10)
        assertThat(resIdsThumbBitmap.sameAs(resIdsAndDpThumbBitmap)).isTrue()
    }
}

private val OFFSET_UNSET = Offset(Float.MIN_VALUE, Float.MIN_VALUE)
private const val PDF_VIEW_TAG = "PdfView"

fun Int.toDp(context: Context): Dp {
    return (this.toFloat() / context.resources.displayMetrics.density).dp
}
