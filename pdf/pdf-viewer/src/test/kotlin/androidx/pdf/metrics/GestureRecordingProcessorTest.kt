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
package androidx.pdf.metrics

import android.os.Build
import androidx.pdf.data.Range
import androidx.pdf.metrics.GestureRecordingProcessor.Companion.getNewlyVisiblePages
import androidx.pdf.metrics.GestureRecordingProcessor.Companion.loadingNewAssetsOnScroll
import androidx.pdf.widget.ZoomView.ZoomScroll
import androidx.test.filters.SmallTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

/** Unit tests for [GestureRecordingProcessor]. */
@SmallTest // TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
class GestureRecordingProcessorTest {
    private val mockCallback: EventCallback = mock()

    @Before
    fun setUp() {
        doNothing().`when`(mockCallback).onPageVisible(any())
        doNothing().`when`(mockCallback).onPageZoomed(any(), any())
    }

    @Test
    fun processNewPositionState_initialLoad_allVisiblePagesScrollLogged() {
        val initialPosition =
            ZoomScroll(/* zoom= */ 1.0f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ true)
        val visiblePages = Range(0, 1)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(
                visiblePages,
                prevVisiblePages,
                initialPosition,
                initialPosition.zoom,
                true
            )
        val zoomPositionState = PositionState(EventState.NO_EVENT, visiblePages)
        val gestureRecordingProcessor =
            GestureRecordingProcessor(mockCallback, initialPosition.zoom)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        for (pageNum in visiblePages) {
            verify(mockCallback).onPageVisible(pageNum)
        }
    }

    @Test
    fun processNewPositionState_secondLoad_newlyVisiblePagesScrollLogged() {
        val position =
            ZoomScroll(/* zoom= */ 0.5f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ true)
        val visiblePages = Range(0, 3)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(visiblePages, prevVisiblePages, position, position.zoom, true)
        val zoomPositionState = PositionState(EventState.NO_EVENT, visiblePages)
        val gestureRecordingProcessor = GestureRecordingProcessor(mockCallback, position.zoom)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        val newlyVisiblePages = Range(2, 3)
        for (pageNum in newlyVisiblePages) {
            verify(mockCallback).onPageVisible(pageNum)
        }
    }

    @Test
    fun processNewPositionState_unstableZoom_noPagesScrollLogged() {
        val position =
            ZoomScroll(/* zoom= */ 0.5f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ false)
        val visiblePages = Range(0, 3)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(visiblePages, prevVisiblePages, position, 1.0f, false)
        val zoomPositionState = PositionState(EventState.NO_EVENT, visiblePages)
        val gestureRecordingProcessor = GestureRecordingProcessor(mockCallback, 1.0f)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        verify(mockCallback, never()).onPageVisible(any())
    }

    @Test
    fun processNewPositionState_unstableScroll_stableZoom_newlyVisiblePagesScrollLogged() {
        val position =
            ZoomScroll(/* zoom= */ 0.5f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ false)
        val visiblePages = Range(0, 3)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(visiblePages, prevVisiblePages, position, position.zoom, false)
        val zoomPositionState = PositionState(EventState.NO_EVENT, visiblePages)
        val gestureRecordingProcessor = GestureRecordingProcessor(mockCallback, position.zoom)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        val newlyVisiblePages = Range(2, 3)
        for (pageNum in newlyVisiblePages) {
            verify(mockCallback).onPageVisible(pageNum)
        }
    }

    @Test
    fun processNewPositionState_stableZoomChanged_allVisiblePagesZoomLogged() {
        val position =
            ZoomScroll(/* zoom= */ 0.5f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ true)
        val visiblePages = Range(0, 3)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(visiblePages, prevVisiblePages, position, position.zoom, false)
        val zoomPositionState = PositionState(EventState.ZOOM_CHANGED, visiblePages)
        val gestureRecordingProcessor = GestureRecordingProcessor(mockCallback, position.zoom)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        for (pageNum in visiblePages) {
            verify(mockCallback).onPageZoomed(pageNum, position.zoom)
        }
    }

    @Test
    fun processNewPositionState_stableZoomNotChanged_noZoomLogging() {
        val position =
            ZoomScroll(/* zoom= */ 0.5f, /* scrollX= */ 0, /* scrollY= */ 0, /* stable= */ true)
        val visiblePages = Range(0, 3)
        val prevVisiblePages = Range(0, 1)
        val scrollPositionState =
            getScrollPositionState(visiblePages, prevVisiblePages, position, position.zoom, false)
        val zoomPositionState = PositionState(EventState.NO_EVENT, visiblePages)
        val gestureRecordingProcessor = GestureRecordingProcessor(mockCallback, position.zoom)

        gestureRecordingProcessor.processNewPositionState(scrollPositionState, zoomPositionState)

        verify(mockCallback, never()).onPageZoomed(any(), any())
    }

    companion object {
        fun getScrollPositionState(
            visiblePages: Range,
            prevVisiblePages: Range,
            initialPosition: ZoomScroll,
            stableZoom: Float,
            isInitialDocumentLoad: Boolean
        ): PositionState {
            val newlyVisiblePages =
                if (isInitialDocumentLoad) {
                    visiblePages
                } else getNewlyVisiblePages(visiblePages, prevVisiblePages)
            val loadingNewAssetsOnScroll =
                loadingNewAssetsOnScroll(newlyVisiblePages, initialPosition, stableZoom)
            return PositionState(
                if (loadingNewAssetsOnScroll) EventState.NEW_ASSETS_LOADED else EventState.NO_EVENT,
                newlyVisiblePages
            )
        }
    }
}
