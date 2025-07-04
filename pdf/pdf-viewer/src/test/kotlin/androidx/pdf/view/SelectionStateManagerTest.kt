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
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectionStateManagerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val errorFlow = MutableSharedFlow<Throwable>()

    // TODO(b/385407478) replace with FakePdfDocument when we're able to share it more broadly
    private val pdfDocument =
        mock<PdfDocument> {
            onBlocking { getSelectionBounds(any(), any(), any()) } doAnswer
                { invocation ->
                    val startPoint = invocation.getArgument<PointF>(1)
                    val endPoint = invocation.getArgument<PointF>(2)
                    pageSelectionFor(invocation.getArgument(0), startPoint, endPoint)
                }
        }

    /** It's simpler to set the selection manually for tests concerning the draggable handles */
    private val initialSelectionForDragging = getInitialSelectionForDragging()

    private lateinit var selectionStateManager: SelectionStateManager

    @Before
    fun setup() {
        selectionStateManager =
            SelectionStateManager(
                pdfDocument,
                testScope,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow,
                pageMetadataLoader = null,
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun maybeSelectWordAtPoint() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val uiSignals = mutableListOf<SelectionUiSignal>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.selectionUiSignalBus.toList(uiSignals)
        }

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        testDispatcher.scheduler.runCurrent()

        val selectionModel = selectionStateManager.selectionModel.value
        assertThat(selectionModel).isNotNull()
        assertThat(selectionModel?.documentSelection?.selection)
            .isInstanceOf(TextSelection::class.java)
        val selection =
            requireNotNull(selectionModel?.documentSelection?.selection as TextSelection)
        assertThat(selection.bounds)
            .isEqualTo(
                listOf(
                    PdfRect(
                        selectionPoint.pageNum,
                        RectF(
                            selectionPoint.x,
                            selectionPoint.y,
                            selectionPoint.x,
                            selectionPoint.y,
                        ),
                    )
                )
            )
        val selectionPointOnPage = PointF(selectionPoint.x, selectionPoint.y)
        assertThat(selection.text)
            .isEqualTo(
                "This is all the text between $selectionPointOnPage and $selectionPointOnPage"
            )

        assertThat(uiSignals.size).isEqualTo(4)
        // hide action mode
        assertThat(uiSignals[0]).isInstanceOf(SelectionUiSignal.ToggleActionMode::class.java)
        assertThat((uiSignals[0] as SelectionUiSignal.ToggleActionMode).show).isFalse()
        // play long press haptic feedback
        assertThat(uiSignals[1]).isInstanceOf(SelectionUiSignal.PlayHapticFeedback::class.java)
        assertThat((uiSignals[1] as SelectionUiSignal.PlayHapticFeedback).level)
            .isEqualTo(HapticFeedbackConstants.LONG_PRESS)
        // invalidate
        assertThat(uiSignals[2]).isInstanceOf(SelectionUiSignal.Invalidate::class.java)
        // show action mode
        assertThat(uiSignals[3]).isInstanceOf(SelectionUiSignal.ToggleActionMode::class.java)
        assertThat((uiSignals[3] as SelectionUiSignal.ToggleActionMode).show).isTrue()
    }

    @Test
    fun maybeSelectWordAtPoint_twice_lastSelectionWins() {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val selectionPoint2 = PdfPoint(pageNum = 10, PointF(250F, 193F))

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        selectionStateManager.maybeSelectWordAtPoint(selectionPoint2)
        testDispatcher.scheduler.runCurrent()

        val selectionModel = selectionStateManager.selectionModel.value
        assertThat(selectionModel).isNotNull()
        assertThat(selectionModel?.documentSelection?.selection)
            .isInstanceOf(TextSelection::class.java)
        val selection =
            requireNotNull(selectionModel?.documentSelection?.selection as TextSelection)
        assertThat(selection.bounds)
            .isEqualTo(
                listOf(
                    PdfRect(
                        selectionPoint2.pageNum,
                        RectF(
                            selectionPoint2.x,
                            selectionPoint2.y,
                            selectionPoint2.x,
                            selectionPoint2.y,
                        ),
                    )
                )
            )
        val selectionPointOnPage = PointF(selectionPoint2.x, selectionPoint2.y)
        assertThat(selection.text)
            .isEqualTo(
                "This is all the text between $selectionPointOnPage and $selectionPointOnPage"
            )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun clearSelection() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val uiSignals = mutableListOf<SelectionUiSignal>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.selectionUiSignalBus.toList(uiSignals)
        }

        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel).isNotNull()
        selectionStateManager.clearSelection()

        assertThat(selectionStateManager.selectionModel.value).isNull()
        // We only care about the final 2 signals that should occur as a result of cancellation
        // hide action mode
        assertThat(uiSignals[uiSignals.size - 2])
            .isInstanceOf(SelectionUiSignal.ToggleActionMode::class.java)
        assertThat((uiSignals[uiSignals.size - 2] as SelectionUiSignal.ToggleActionMode).show)
            .isFalse()
        // invalidate
        assertThat(uiSignals.last()).isInstanceOf(SelectionUiSignal.Invalidate::class.java)
    }

    @Test
    fun clearSelection_cancelsWork() {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))

        // Start a selection and don't finish it (i.e. no runCurrent)
        selectionStateManager.maybeSelectWordAtPoint(selectionPoint)
        assertThat(selectionStateManager.selectionModel.value).isNull()

        // Clear selection, flush the scheduler, and make sure selection remains null (i.e. the work
        // enqueued by our initial selection doesn't finish and supersede the cleared state)
        selectionStateManager.clearSelection()
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel.value).isNull()
    }

    @Test
    fun maybeDragHandle_actionDownOutsideHandle_returnFalse() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }

        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, PointF(0F, 0F)),
                    currentZoom = 2.0F,
                )
            )
            .isFalse()
    }

    @Test
    fun maybeDragHandle_actionDownInsideStartHandle_returnTrue() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // Chose a point inside the start handle touch target (below and behind the start position)
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        // "Grab" the start handle and make sure we handle the event
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()
    }

    @Test
    fun maybeDragHandle_actionDownInsideEndHandle_returnTrue() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // Chose a point inside the end handle touch target (below and ahead the end position)
        val insideEndHandle =
            PointF(
                    initialSelectionForDragging.endBoundary.location.x,
                    initialSelectionForDragging.endBoundary.location.y,
                )
                .apply { offset(HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        // "Grab" the end handle and make sure we handle the event
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideEndHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()
    }

    @Test
    fun maybeDragHandle_actionMove_updateSelection() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // "Grab" the start handle
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        // Drag the start handle by 5px in both x and y
        val newStartPosition =
            PointF(insideStartHandle).apply { offset(/* dx= */ 5F, /* dy= */ 5F) }
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, newStartPosition),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        // Make sure the selection is updated appropriately
        testDispatcher.scheduler.runCurrent()
        val selection = selectionStateManager.selectionModel.value?.documentSelection?.selection
        assertThat(selection).isInstanceOf(TextSelection::class.java)
        val expectedStartLoc =
            PointF(
                initialSelectionForDragging.endBoundary.location.x,
                initialSelectionForDragging.endBoundary.location.y,
            )
        val expectedEndLoc =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(/* dx= */ 5F, /* dy= */ 5F) }
        assertThat((selection as TextSelection).text)
            .isEqualTo("This is all the text between $expectedStartLoc and $expectedEndLoc")
    }

    @Test
    fun maybeDragHandle_actionMoveOutsidePage_returnTrue() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // "Grab" the start handle
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        // Drag the handle to a location outside any page (location = null), and make sure we still
        // "capture" the event
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_MOVE,
                    location = null,
                    currentZoom = 2.0F,
                )
            )
            .isTrue()
    }

    @Test
    fun maybeDragHandle_actionMoveWithoutActionDown_returnFalse() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // Chose a point inside the start handle touch target (below and behind the start position)
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        // Make sure we don't handle an ACTION_MOVE without an initial ACTION_DOWN, even when the
        // move event occurs within one of the drag handles
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isFalse()
    }

    @Test
    fun maybeDragHandle_actionUpWithoutActionDown_returnFalse() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // Chose a point inside the start handle touch target (below and behind the start position)
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        // Make sure we don't handle an ACTION_UP without an initial ACTION_DOWN, even when the
        // up event occurs within one of the drag handles
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_UP,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isFalse()
    }

    @Test
    fun maybeDragHandle_actionUp_returnTrueAndStopHandlingEvents() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // Chose a point inside the start handle touch target (below and behind the start position)
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        // "Grab" the start handle
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        // "Release" the start handle, and make sure we handle the event
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_UP,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        // Make sure we don't handle an ACTION_MOVE after releasing the gesture
        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isFalse()
    }

    @Test
    fun maybeDragHandle_actionMove_extendSelectionDownwards() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }
        // "Grab" the start handle
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        val value =
            selectionStateManager.maybeDragSelectionHandle(
                MotionEvent.ACTION_MOVE,
                PdfPoint(
                    pageNum = 2,
                    initialSelectionForDragging.endBoundary.location.x,
                    initialSelectionForDragging.endBoundary.location.y,
                ),
                currentZoom = 2.0F,
            )
        assertThat(value).isTrue()

        // Make sure the selection is updated appropriately
        testDispatcher.scheduler.runCurrent()
        val selection = selectionStateManager.selectionModel.value?.documentSelection?.selection

        assertThat(selection).isInstanceOf(TextSelection::class.java)
        val expectedStartLoc =
            PointF(
                initialSelectionForDragging.startBoundary.location.x,
                initialSelectionForDragging.startBoundary.location.y,
            )
        val expectedEndLoc =
            PointF(
                initialSelectionForDragging.endBoundary.location.x,
                initialSelectionForDragging.endBoundary.location.y,
            )

        val expectedText =
            "This is all the text between $expectedStartLoc and PointF(0.0, 0.0) This is all the text between PointF(0.0, 0.0) and $expectedEndLoc"

        assertThat((selection as TextSelection).text).isEqualTo(expectedText)
    }

    @Test
    fun maybeDragHandle_actionMove_extendSelectionUpwards() {
        selectionStateManager._selectionModel.update { getInitialSelectionForDragging(1) }
        // "Grab" the start handle
        val insideStartHandle =
            PointF(
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                )
                .apply { offset(-HANDLE_TOUCH_TARGET_PX / 4.0F, HANDLE_TOUCH_TARGET_PX / 4.0F) }

        assertThat(
                selectionStateManager.maybeDragSelectionHandle(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 1, insideStartHandle),
                    currentZoom = 2.0F,
                )
            )
            .isTrue()

        val value =
            selectionStateManager.maybeDragSelectionHandle(
                MotionEvent.ACTION_MOVE,
                PdfPoint(
                    pageNum = 0,
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                ),
                currentZoom = 2.0F,
            )
        assertThat(value).isTrue()

        // Make sure the selection is updated appropriately
        testDispatcher.scheduler.runCurrent()
        val selection = selectionStateManager.selectionModel.value?.documentSelection?.selection

        assertThat(selection).isInstanceOf(TextSelection::class.java)
        val expectedStartLoc =
            PointF(
                initialSelectionForDragging.startBoundary.location.x,
                initialSelectionForDragging.startBoundary.location.y,
            )
        val expectedEndLoc =
            PointF(
                initialSelectionForDragging.endBoundary.location.x,
                initialSelectionForDragging.endBoundary.location.y,
            )

        val expectedText =
            "This is all the text between $expectedStartLoc and PointF(0.0, 0.0) This is all the text between PointF(0.0, 0.0) and $expectedEndLoc"

        assertThat((selection as TextSelection).text).isEqualTo(expectedText)
    }

    private fun getInitialSelectionForDragging(pageNumber: Int = 0): SelectionModel {
        return SelectionModel(
            DocumentSelection(
                SparseArray<List<Selection>>().apply {
                    set(
                        pageNumber,
                        listOf(
                            TextSelection(
                                "This is the text that's selected",
                                listOf(
                                    PdfRect(pageNum = pageNumber, RectF(150F, 150F, 190F, 160F)),
                                    PdfRect(pageNum = pageNumber, RectF(10F, 170F, 50F, 180F)),
                                ),
                            )
                        ),
                    )
                }
            ),
            UiSelectionBoundary(PdfPoint(pageNum = pageNumber, PointF(150F, 160F)), isRtl = true),
            UiSelectionBoundary(PdfPoint(pageNum = pageNumber, PointF(50F, 180F)), isRtl = true),
        )
    }

    private fun pageSelectionFor(page: Int, start: PointF, end: PointF): PageSelection {
        return PageSelection(
            page,
            SelectionBoundary(point = Point(start.x.toInt(), start.y.toInt())),
            SelectionBoundary(point = Point(end.x.toInt(), end.y.toInt())),
            listOf(
                PdfPageTextContent(
                    listOf(
                        RectF(
                            minOf(start.x, end.x),
                            minOf(start.y, end.y),
                            maxOf(start.x, end.x),
                            maxOf(start.y, end.y),
                        )
                    ),
                    text = "This is all the text between $start and $end",
                )
            ),
        )
    }
}

private const val HANDLE_TOUCH_TARGET_PX = 48
