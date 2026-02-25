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

package androidx.pdf.selection

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.util.SparseArray
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.pdf.FakePdfDocument
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.pdf.annotation.models.ImagePdfObject
import androidx.pdf.content.PageSelection
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.content.SelectionBoundary
import androidx.pdf.selection.model.ImageSelection
import androidx.pdf.selection.model.TextSelection
import androidx.pdf.utils.isRequiredSdkExtensionAvailable
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNull
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
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
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
    private val fakePdfDocument = FakePdfDocument()

    /** It's simpler to set the selection manually for tests concerning the draggable handles */
    private val initialSelectionForDragging = getInitialSelectionForDragging()

    private lateinit var selectionStateManager: SelectionStateManager
    private lateinit var selectionStateManagerWithFakeDoc: SelectionStateManager

    @Before
    fun setup() {
        selectionStateManager =
            SelectionStateManager(
                pdfDocument,
                testScope,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow,
                pageLayoutManager = null,
                pageManager = null,
            )
        selectionStateManagerWithFakeDoc =
            SelectionStateManager(
                fakePdfDocument,
                testScope,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow,
                pageLayoutManager = null,
                pageManager = null,
            )
    }

    @Test
    fun maybeSelectImageAtPoint_imageSelectionDisabled() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        val pageNumber = 0
        val selectionPoint = PointF(100F, 200F)
        val selectionPdfPoint = PdfPoint(pageNumber, selectionPoint)

        selectionStateManagerWithFakeDoc.maybeSelectContentAtPoint(selectionPdfPoint)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val actualSelection =
            selectionStateManagerWithFakeDoc._selectionModel.value?.documentSelection?.selection
        assertNull(actualSelection)
    }

    @Test
    fun maybeSelectImageAtPoint_imagePresent() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        val pageNumber = 0
        val selectionPoint = PointF(100F, 200F)
        val selectionPdfPoint = PdfPoint(pageNumber, selectionPoint)

        val expectedImagePdfObject = FakePdfDocument.getSampleImagePdfObject()
        val expectedBounds = expectedImagePdfObject.bounds
        val expectedBitmap = expectedImagePdfObject.bitmap

        selectionStateManagerWithFakeDoc.isImageSelectionEnabled = true
        selectionStateManagerWithFakeDoc.maybeSelectContentAtPoint(selectionPdfPoint)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val actualSelection =
            selectionStateManagerWithFakeDoc._selectionModel.value?.documentSelection?.selection

        assertThat(actualSelection is ImageSelection).isTrue()
        (actualSelection as ImageSelection).let { imageSelection ->
            assertThat(imageSelection.bitmap).isEqualTo(expectedBitmap)
            assertThat(imageSelection.bounds.size).isEqualTo(1)
            assertThat(imageSelection.bounds[0].left).isEqualTo(expectedBounds.left)
            assertThat(imageSelection.bounds[0].top).isEqualTo(expectedBounds.top)
            assertThat(imageSelection.bounds[0].right).isEqualTo(expectedBounds.right)
            assertThat(imageSelection.bounds[0].bottom).isEqualTo(expectedBounds.bottom)
        }
    }

    @Test
    fun maybeSelectImageAtPoint_imageNotPresent() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        val pageNumber = -1
        val selectionPoint = PointF(100F, 200F)
        val selectionPdfPoint = PdfPoint(pageNumber, selectionPoint)

        selectionStateManagerWithFakeDoc.isImageSelectionEnabled = true
        selectionStateManagerWithFakeDoc.maybeSelectContentAtPoint(selectionPdfPoint)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val actualSelection =
            selectionStateManagerWithFakeDoc._selectionModel.value?.documentSelection?.selection
        assertNull(actualSelection)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun maybeSelectImageAtPoint_imageAndTextBothPresent() = runTest {
        if (!isRequiredSdkExtensionAvailable(19)) return@runTest

        val pageNumber = 10
        val selectionPoint = PointF(150F, 265F)
        val selectionPdfPoint = PdfPoint(pageNumber, selectionPoint)

        selectionStateManagerWithFakeDoc.isImageSelectionEnabled = true
        // check if text is selected if no image present at selectionPoint
        selectionStateManagerWithFakeDoc.maybeSelectContentAtPoint(selectionPdfPoint)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        testDispatcher.scheduler.runCurrent()

        val selectionModel = selectionStateManagerWithFakeDoc.selectionModel.value
        assertThat(selectionModel).isNotNull()

        // check selection is a text selection
        assertThat(selectionModel?.documentSelection?.selection is TextSelection).isTrue()
        (selectionModel?.documentSelection?.selection as TextSelection).let { selection ->
            assertThat(selection.bounds.size).isEqualTo(1)
            assertThat(selection.bounds[0].left).isEqualTo(selectionPoint.x)
            assertThat(selection.bounds[0].top).isEqualTo(selectionPoint.y)
            assertThat(selection.bounds[0].right).isEqualTo(selectionPoint.x)
            assertThat(selection.bounds[0].bottom).isEqualTo(selectionPoint.y)
            assertThat(selection.text)
                .isEqualTo("This is all the text between $selectionPoint and $selectionPoint")
        }

        // recheck if image is selected, when image present and text both present on selectionPoint
        val expectedBounds = RectF(0f, 100f, 0f, 100f)
        val expectedBitmap = mock<Bitmap>()
        val expectedImagePdfObject = ImagePdfObject(expectedBitmap, expectedBounds)

        // mock image present at this point
        whenever(pdfDocument.getTopPageObjectAtPosition(pageNumber, selectionPoint))
            .thenReturn(expectedImagePdfObject)

        selectionStateManager.maybeSelectContentAtPoint(selectionPdfPoint)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val actualSelection =
            selectionStateManager._selectionModel.value?.documentSelection?.selection

        // check selection is an image selection
        assertThat(actualSelection is ImageSelection).isTrue()
        (actualSelection as ImageSelection).let { imageSelection ->
            assertThat(imageSelection.bitmap).isEqualTo(expectedBitmap)
            assertThat(imageSelection.bounds.size).isEqualTo(1)
            assertThat(imageSelection.bounds[0].left).isEqualTo(expectedBounds.left)
            assertThat(imageSelection.bounds[0].top).isEqualTo(expectedBounds.top)
            assertThat(imageSelection.bounds[0].right).isEqualTo(expectedBounds.right)
            assertThat(imageSelection.bounds[0].bottom).isEqualTo(expectedBounds.bottom)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun maybeSelectWordAtPoint() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val uiSignals = mutableListOf<SelectionUiSignal>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.selectionUiSignalBus.toList(uiSignals)
        }

        selectionStateManager.maybeSelectContentAtPoint(selectionPoint)
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

        selectionStateManager.maybeSelectContentAtPoint(selectionPoint)
        selectionStateManager.maybeSelectContentAtPoint(selectionPoint2)
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
    fun clearCurrentSelection() = runTest {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))
        val uiSignals = mutableListOf<SelectionUiSignal>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            selectionStateManager.selectionUiSignalBus.toList(uiSignals)
        }

        selectionStateManager.maybeSelectContentAtPoint(selectionPoint)
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel).isNotNull()
        selectionStateManager.clearCurrentSelection()

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
    fun clearCurrentSelection_cancelsWork() {
        val selectionPoint = PdfPoint(pageNum = 10, PointF(150F, 265F))

        // Start a selection and don't finish it (i.e. no runCurrent)
        selectionStateManager.maybeSelectContentAtPoint(selectionPoint)
        assertThat(selectionStateManager.selectionModel.value).isNull()

        // Clear selection, flush the scheduler, and make sure selection remains null (i.e. the work
        // enqueued by our initial selection doesn't finish and supersede the cleared state)
        selectionStateManager.clearCurrentSelection()
        testDispatcher.scheduler.runCurrent()
        assertThat(selectionStateManager.selectionModel.value).isNull()
    }

    @Test
    fun maybeDragHandle_actionDownOutsideHandle_returnFalse() {
        selectionStateManager._selectionModel.update { initialSelectionForDragging }

        assertThat(
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, PointF(0F, 0F)),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideEndHandle),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        // Drag the start handle by 5px in both x and y
        val newStartPosition =
            PointF(insideStartHandle).apply { offset(/* dx= */ 5F, /* dy= */ 5F) }
        assertThat(
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, newStartPosition),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        // Drag the handle to a location outside any page (location = null), and make sure we still
        // "capture" the event
        assertThat(
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_MOVE,
                    location = null,
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_UP,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        // "Release" the start handle, and make sure we handle the event
        assertThat(
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_UP,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        // Make sure we don't handle an ACTION_MOVE after releasing the gesture
        assertThat(
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_MOVE,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 0, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        val value =
            selectionStateManager.maybeDragSelection(
                MotionEvent.ACTION_MOVE,
                PdfPoint(
                    pageNum = 2,
                    initialSelectionForDragging.endBoundary.location.x,
                    initialSelectionForDragging.endBoundary.location.y,
                ),
                currentZoom = 2.0F,
                false,
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
                selectionStateManager.maybeDragSelection(
                    MotionEvent.ACTION_DOWN,
                    PdfPoint(pageNum = 1, insideStartHandle),
                    currentZoom = 2.0F,
                    false,
                )
            )
            .isTrue()

        val value =
            selectionStateManager.maybeDragSelection(
                MotionEvent.ACTION_MOVE,
                PdfPoint(
                    pageNum = 0,
                    initialSelectionForDragging.startBoundary.location.x,
                    initialSelectionForDragging.startBoundary.location.y,
                ),
                currentZoom = 2.0F,
                false,
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
    fun processInitialSelection_withTextSelection_isSetCorrectly() = runTest {
        // Use a non-image selection as the initial state
        val initialTextSelection = getInitialSelectionForDragging()
        val manager =
            SelectionStateManager(
                pdfDocument,
                testScope,
                initialSelection = initialTextSelection,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow = errorFlow,
                pageLayoutManager = null,
                pageManager = null,
            )

        // Verify the initial selection is set directly
        assertThat(manager.selectionModel.value).isEqualTo(initialTextSelection)
    }

    @Test
    fun processInitialSelection_withPlaceholderImage_returnsNullAndStartsRefetch() = runTest {
        // Create a placeholder ImageSelection
        val placeholderBounds = PdfRect(0, RectF(10f, 10f, 90f, 90f))
        val topLeft = PdfPoint(0, 10f, 10f)
        val bottomRight = PdfPoint(0, 90f, 90f)
        val placeHolderBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        val placeholderImageSelection =
            ImageSelection(bitmap = placeHolderBitmap, imageBounds = placeholderBounds).apply {
                isPlaceholder = true
            }
        val initialImageSelection =
            SelectionModel(
                DocumentSelection(
                    SparseArray<List<Selection>>().apply {
                        set(0, listOf(placeholderImageSelection))
                    }
                ),
                UiSelectionBoundary(topLeft, false),
                UiSelectionBoundary(bottomRight, false),
            )

        val manager =
            SelectionStateManager(
                fakePdfDocument, // Use FakePdfDocument which can find an image
                testScope,
                initialSelection = initialImageSelection,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow = errorFlow,
                pageLayoutManager = null,
                pageManager = null,
            )
        manager.isImageSelectionEnabled = true

        // Verify the initial state is null, as placeholders are filtered out
        assertNull(manager.selectionModel.value)

        // Advance the coroutine to allow the background re-fetch to complete
        testDispatcher.scheduler.runCurrent()

        // Verify the final selection is the actual image from the document
        val finalSelection = manager.selectionModel.value?.documentSelection?.selection
        assertThat(finalSelection).isInstanceOf(ImageSelection::class.java)
        val finalImageSelection = finalSelection as ImageSelection

        // Check that the placeholder has been replaced with the actual bitmap
        assertThat(finalImageSelection.isPlaceholder).isFalse()
        assertThat(finalImageSelection.bitmap).isNotNull()
        assertThat(finalImageSelection.bounds.first().pageNum).isEqualTo(placeholderBounds.pageNum)
    }

    @Test
    fun processInitialSelection_withNullInitialSelection_remainsNull() = runTest {
        // Pass null as the initial selection
        val manager =
            SelectionStateManager(
                pdfDocument,
                testScope,
                initialSelection = null,
                handleTouchTargetSizePx = HANDLE_TOUCH_TARGET_PX,
                errorFlow = errorFlow,
                pageLayoutManager = null,
                pageManager = null,
            )

        // Verify the selection model remains null
        assertNull(manager.selectionModel.value)
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
