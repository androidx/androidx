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

package androidx.pdf.viewer

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.pdf.find.FindInFileView
import androidx.pdf.models.GotoLinkDestination
import androidx.pdf.models.PageSelection
import androidx.pdf.util.ObservableValue
import androidx.pdf.util.ZoomUtils
import androidx.pdf.widget.ZoomView
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SingleTapHandlerTest {

    private val mockContext: Context = ApplicationProvider.getApplicationContext()
    private val mockAnnotationButton: FloatingActionButton = mock()
    private val mockPaginatedView: PaginatedView = mock()
    private val mockFindInFileView: FindInFileView = mock()
    private val mockZoomView: ZoomView = mock()
    private val mockSelectionModel: PdfSelectionModel = mock()
    private val mockPaginationModel: PaginationModel = mock()
    private val mockLayoutHandler: LayoutHandler = mock()
    private val mockImmersiveModeRequester: ImmersiveModeRequester = mock()
    private val mockSelection: ObservableValue<PageSelection> =
        mock<ObservableValue<PageSelection>>()
    private val mockPageSelection: PageSelection = mock()
    private val mockMotionEvent: MotionEvent = mock()
    private val mockPageMosaicView: PageMosaicView = mock()
    private val mockLinkDestination: GotoLinkDestination = mock()

    private lateinit var singleTapHandler: SingleTapHandler

    @Before
    fun setup() {
        singleTapHandler =
            SingleTapHandler(
                mockContext,
                mockAnnotationButton,
                mockPaginatedView,
                mockFindInFileView,
                mockZoomView,
                mockSelectionModel,
                mockPaginationModel,
                mockLayoutHandler,
                mockImmersiveModeRequester,
            )
        whenever(mockSelectionModel.selection()).thenReturn(mockSelection)
        whenever(mockSelectionModel.selection().get()).thenReturn(mockPageSelection)
    }

    @Test
    fun noImmersiveModeActionWhenAnnotationResolvableFalse() {
        singleTapHandler.setAnnotationIntentResolvable(false)
        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)
        verifyNoInteractions(mockImmersiveModeRequester)
    }

    @Test
    fun immersiveModeChangeRequestWhenAnnotationResolvableTrueAndFiFAndFabGone() {
        singleTapHandler.setAnnotationIntentResolvable(true)
        whenever(mockAnnotationButton.visibility).thenReturn(View.GONE)
        whenever(mockFindInFileView.visibility).thenReturn(View.GONE)

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockImmersiveModeRequester).requestImmersiveModeChange(false)
    }

    @Test
    fun immersiveModeChangeRequestWhenAnnotationResolvableTrueAndFabVisible() {
        singleTapHandler.setAnnotationIntentResolvable(true)
        whenever(mockAnnotationButton.visibility).thenReturn(View.VISIBLE)
        whenever(mockFindInFileView.visibility).thenReturn(View.GONE)

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockImmersiveModeRequester).requestImmersiveModeChange(true)
    }

    @Test
    fun immersiveModeChangeRequestWhenAnnotationResolvableTrueAndFiFVisible() {
        singleTapHandler.setAnnotationIntentResolvable(true)
        whenever(mockAnnotationButton.visibility).thenReturn(View.GONE)
        whenever(mockFindInFileView.visibility).thenReturn(View.VISIBLE)

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockImmersiveModeRequester).requestImmersiveModeChange(true)
    }

    @Test
    fun immersiveModeChangeRequestWhenAnnotationResolvableTrueAndFabAndFiFVisible() {
        singleTapHandler.setAnnotationIntentResolvable(true)
        whenever(mockAnnotationButton.visibility).thenReturn(View.VISIBLE)
        whenever(mockFindInFileView.visibility).thenReturn(View.VISIBLE)

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockImmersiveModeRequester).requestImmersiveModeChange(true)
    }

    @Test
    fun handleSelectionIsCalledOnSingleTapWhenHadSelectionFalse() {
        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)
        verify(mockSelectionModel).setSelection(null)
    }

    @Test
    fun handleSelectionIsCalledOnSingleTapWhenHadSelectionTrue() {
        whenever(mockSelection.get()).thenReturn(null)
        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)
        verify(mockSelectionModel, never()).setSelection(null)
    }

    @Test
    fun findInFileViewHandleSingleTapEventIsCalled() {
        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockFindInFileView).handleSingleTapEvent()
    }

    @Test
    fun handleExternalLinkIsCalledWithCorrectPointAndLinkUrlNull() {
        whenever(mockMotionEvent.x).thenReturn(100f)
        whenever(mockMotionEvent.y).thenReturn(200f)
        val point = Point(mockMotionEvent.x.toInt(), mockMotionEvent.y.toInt())

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)

        verify(mockPageMosaicView).getLinkUrl(point)
    }

    @Test
    fun handleExternalLinkIsCalledWithCorrectPointAndLinkUrlNotNull() {
        whenever(mockMotionEvent.x).thenReturn(100f)
        whenever(mockMotionEvent.y).thenReturn(200f)
        whenever(mockPageMosaicView.getLinkUrl(any())).thenReturn("SampleLinkUrl")

        val point = Point(mockMotionEvent.x.toInt(), mockMotionEvent.y.toInt())

        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)
        verify(mockPageMosaicView).getLinkUrl(point)
    }

    @Test
    fun goToPageDestIsCalledWithGotoDestNotNull() {
        whenever(mockMotionEvent.x).thenReturn(100f)
        whenever(mockMotionEvent.y).thenReturn(200f)
        val point = Point(mockMotionEvent.x.toInt(), mockMotionEvent.y.toInt())
        whenever(mockPageMosaicView.getGotoDestination(point)).thenReturn(mockLinkDestination)
        singleTapHandler.handleSingleTapConfirmedEventOnPage(mockMotionEvent, mockPageMosaicView)
        verify(mockPageMosaicView).getGotoDestination(point)
    }

    @Test
    fun goToPageDestWithDestinationPageNumberGreaterThanPaginationModelSize() {
        val pageNumber = 2
        val paginationModelSize = 1

        whenever(mockLinkDestination.pageNumber).thenReturn(pageNumber)
        whenever(mockPaginationModel.size).thenReturn(paginationModelSize)

        singleTapHandler.gotoPageDest(mockLinkDestination)
        verify(mockLayoutHandler).layoutPages(mockLinkDestination.pageNumber + 1)
        verify(mockLayoutHandler).add(any())
        verify(mockLinkDestination, never()).yCoordinate
    }

    @Test
    fun gotoPageDestWithValidYCoordinate() {
        val pageNumber = 1
        val paginationModelSize = 2
        val viewportWidth = 300
        val viewportHeight = 600
        val yCoordinate = 150f
        val mockPageRect = Rect(0, 0, viewportWidth, 600)

        whenever(mockLinkDestination.pageNumber).thenReturn(pageNumber)
        whenever(mockLinkDestination.yCoordinate).thenReturn(yCoordinate)
        whenever(mockPaginationModel.size).thenReturn(paginationModelSize)
        whenever(mockPaginationModel.getPageLocation(pageNumber, mockPaginatedView.viewArea))
            .thenReturn(mockPageRect)
        whenever(mockZoomView.viewportWidth).thenReturn(viewportWidth)
        whenever(mockZoomView.viewportHeight).thenReturn(viewportHeight)

        singleTapHandler.gotoPageDest(mockLinkDestination)

        val zoom =
            ZoomUtils.calculateZoomToFit(
                mockZoomView.viewportWidth.toFloat(),
                mockZoomView.viewportHeight.toFloat(),
                mockPageRect.width().toFloat(),
                1F,
            )

        // Verify the zoom level and centering
        verify(mockZoomView).setZoom(zoom)
        verify(mockZoomView)
            .centerAt(
                mockPageRect.centerX().toFloat(),
                mockPaginationModel.getLookAtY(pageNumber, yCoordinate.toInt()).toFloat(),
            )
    }

    @Test
    fun gotoPageDestWithNullYCoordinate() {
        val pageNumber = 1
        val paginationModelSize = 2
        val mockPageRect = Rect(0, 0, 300, 600)

        whenever(mockLinkDestination.pageNumber).thenReturn(pageNumber)
        whenever(mockLinkDestination.yCoordinate).thenReturn(null)
        whenever(mockPaginationModel.size).thenReturn(paginationModelSize)
        whenever(mockPaginationModel.getPageLocation(pageNumber, mockPaginatedView.viewArea))
            .thenReturn(mockPageRect)

        singleTapHandler.gotoPageDest(mockLinkDestination)
        verify(mockPaginationModel)
            .getPageLocation(mockLinkDestination.pageNumber, mockPaginatedView.viewArea)
        verify(mockZoomView).setZoom(any())
    }

    @Test
    fun gotoPageDestWithPageNumberOutOfBounds() {
        val pageNumber = 2
        val paginationModelSize = 1

        whenever(mockLinkDestination.pageNumber).thenReturn(pageNumber)
        whenever(mockPaginationModel.size).thenReturn(paginationModelSize)

        singleTapHandler.gotoPageDest(mockLinkDestination)

        verify(mockLayoutHandler).layoutPages(pageNumber + 1)
        verify(mockLayoutHandler).add(any())
    }

    @Test
    fun correctZoomLevelCalculated() {
        val pageNumber = 1
        val paginationModelSize = 2
        val mockPageRect = Rect(0, 0, 400, 800) // Different dimensions

        whenever(mockLinkDestination.pageNumber).thenReturn(pageNumber)
        whenever(mockPaginationModel.size).thenReturn(paginationModelSize)
        whenever(mockPaginationModel.getPageLocation(pageNumber, mockPaginatedView.viewArea))
            .thenReturn(mockPageRect)

        singleTapHandler.gotoPageDest(mockLinkDestination)

        val expectedZoom =
            ZoomUtils.calculateZoomToFit(
                mockZoomView.viewportWidth.toFloat(),
                mockZoomView.viewportHeight.toFloat(),
                mockPageRect.width().toFloat(),
                mockPageRect.height().toFloat(),
            )

        verify(mockZoomView).setZoom(expectedZoom)
    }
}
