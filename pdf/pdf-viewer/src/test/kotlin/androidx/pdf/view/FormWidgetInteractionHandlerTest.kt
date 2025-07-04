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

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.DeadObjectException
import androidx.core.graphics.toRect
import androidx.pdf.PdfDocument
import androidx.pdf.PdfPoint
import androidx.pdf.exceptions.RequestFailedException
import androidx.pdf.models.FormEditRecord
import androidx.pdf.models.FormWidgetInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FormWidgetInteractionHandlerTest {
    private val pdfDocument =
        mock<PdfDocument> {
            on { pageCount } doReturn TOTAL_PAGES
            onBlocking { getPageInfo(any(), any()) } doAnswer
                { invocationOnMock ->
                    PdfDocument.PageInfo(
                        pageNum = invocationOnMock.getArgument(0),
                        height = PAGE_HEIGHT,
                        width = PAGE_WIDTH,
                    )
                }
        }
    private val errorFlow = MutableSharedFlow<Throwable>()
    private lateinit var handler: FormWidgetInteractionHandler
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope: TestScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val applicationContext = ApplicationProvider.getApplicationContext<Context>()
        handler =
            FormWidgetInteractionHandler(applicationContext, pdfDocument, testScope, errorFlow)
    }

    @Test
    fun handleInteraction_readOnlyWidget_doesNothing() = runTest {
        val touchPoint = PdfPoint(0, PointF(10f, 20f))
        val formWidgetInfo =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_TEXTFIELD,
                widgetIndex = 0,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Hello",
                accessibilityLabel = "accessible",
                readOnly = true, // Read-only widget
            )

        handler.handleInteraction(touchPoint, formWidgetInfo)
        // Assert that applyEdit was never called for a read-only widget
        verify(pdfDocument, never()).applyEdit(any(), any())
    }

    @Test
    fun handleInteractionWithClickTypeWidget_successfulApplyEdit() = runTest {
        val invalidatedRectValues = mutableListOf<Pair<Int, List<RectF>>>()
        backgroundScope.launch(testDispatcher) {
            handler.invalidatedAreas.toList(invalidatedRectValues)
        }

        val pageNum = 1
        val pdfCoordinates = PointF(10f, 20f)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_CHECKBOX,
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Hello",
                accessibilityLabel = "accessible",
                readOnly = false,
            )
        val invalidatedRects = listOf(Rect(0, 0, 100, 100))
        val expectedEditRecord =
            FormEditRecord(
                pageNumber = pageNum,
                widgetIndex = widgetIndex,
                clickPoint = Point(pdfCoordinates.x.toInt(), pdfCoordinates.y.toInt()),
            )
        `when`(pdfDocument.applyEdit(pageNum, expectedEditRecord)).thenReturn(invalidatedRects)

        handler.handleInteractionWithClickTypeWidget(pageNum, pdfCoordinates, formWidgetInfo)
        verify(pdfDocument).applyEdit(pageNum, expectedEditRecord)
        assertThat(invalidatedRectValues.size).isEqualTo(1)
        assertThat(invalidatedRectValues[0].first).isEqualTo(pageNum)
        assertThat(invalidatedRectValues[0].second.map { it.toRect() }).isEqualTo(invalidatedRects)
    }

    @Test
    fun handleInteraction_radioButtonWidget_applyEdit() = runTest {
        val invalidatedAreas = mutableListOf<Pair<Int, List<RectF>>>()
        backgroundScope.launch(testDispatcher) { handler.invalidatedAreas.toList(invalidatedAreas) }

        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Radio",
                accessibilityLabel = "accessible",
                readOnly = false,
            )
        val expectedEditRecord =
            FormEditRecord(
                pageNumber = pageNum,
                widgetIndex = widgetIndex,
                clickPoint = Point(pdfCoordinates.x.toInt(), pdfCoordinates.y.toInt()),
            )
        val invalidatedRects = listOf(Rect(0, 0, 100, 100))

        `when`(pdfDocument.applyEdit(pageNum, expectedEditRecord)).thenReturn(invalidatedRects)
        handler.handleInteractionWithClickTypeWidget(pageNum, pdfCoordinates, formWidgetInfo)
        verify(pdfDocument).applyEdit(pageNum, expectedEditRecord)
        assertThat(invalidatedAreas.size).isEqualTo(1)
        assertThat(invalidatedAreas[0].first).isEqualTo(pageNum)
        assertThat(invalidatedAreas[0].second.map { it.toRect() }).isEqualTo(invalidatedRects)
    }

    @Test
    fun handleInteraction_pushButtonWidget_callsApplyEdit() = runTest {
        val invalidatedAreas = mutableListOf<Pair<Int, List<RectF>>>()
        backgroundScope.launch(testDispatcher) { handler.invalidatedAreas.toList(invalidatedAreas) }

        val pageNum = 0
        val pdfCoordinates = PointF(10f, 20f)
        val touchPoint = PdfPoint(pageNum, pdfCoordinates)
        val widgetIndex = 0
        val formWidgetInfo =
            FormWidgetInfo(
                widgetType = FormWidgetInfo.WIDGET_TYPE_PUSHBUTTON,
                widgetIndex = widgetIndex,
                widgetRect = Rect(10, 10, 20, 20),
                textValue = "Push",
                accessibilityLabel = "accessible",
                readOnly = false,
            )
        val expectedEditRecord =
            FormEditRecord(
                pageNumber = pageNum,
                widgetIndex = widgetIndex,
                clickPoint = Point(pdfCoordinates.x.toInt(), pdfCoordinates.y.toInt()),
            )
        val invalidatedRects = listOf(Rect(0, 0, 100, 100))

        `when`(pdfDocument.applyEdit(pageNum, expectedEditRecord)).thenReturn(invalidatedRects)

        handler.handleInteraction(touchPoint, formWidgetInfo)

        verify(pdfDocument).applyEdit(pageNum, expectedEditRecord)
        assertThat(invalidatedAreas.size).isEqualTo(1)
        assertThat(invalidatedAreas[0].first).isEqualTo(pageNum)
        assertThat(invalidatedAreas[0].second.map { it.toRect() }).isEqualTo(invalidatedRects)
    }

    @Test
    fun applyEditRecord_emitsInvalidatedAreasOnSuccess() = runTest {
        val invalidatedAreas = mutableListOf<Pair<Int, List<RectF>>>()
        backgroundScope.launch(testDispatcher) { handler.invalidatedAreas.toList(invalidatedAreas) }
        val pageNum = 1
        val widgetIndex = 0
        val formEditRecord =
            FormEditRecord(pageNumber = pageNum, widgetIndex = widgetIndex, text = "New Text")
        val invalidatedRects = listOf(Rect(0, 0, 50, 50))

        `when`(pdfDocument.applyEdit(pageNum, formEditRecord)).thenReturn(invalidatedRects)

        handler.applyEditRecord(pageNum, formEditRecord)

        assertThat(invalidatedAreas.size).isEqualTo(1)
        assertThat(invalidatedAreas[0].first).isEqualTo(pageNum)
        assertThat(invalidatedAreas[0].second.map { it.toRect() }).isEqualTo(invalidatedRects)
    }

    @Test
    fun applyEditRecord_emitsRequestFailedExceptionForDeadObjectException() = runTest {
        val pageNum = 1
        val widgetIndex = 0
        val formEditRecord =
            FormEditRecord(pageNumber = pageNum, widgetIndex = widgetIndex, text = "New Text")

        `when`(pdfDocument.applyEdit(pageNum, formEditRecord)).thenAnswer {
            throw DeadObjectException()
        }

        val errors = mutableListOf<Throwable>()
        backgroundScope.launch(testDispatcher) { errorFlow.toList(errors) }

        handler.applyEditRecord(pageNum, formEditRecord)

        assertEquals(1, errors.size)
        val error = errors[0]
        assert(error is RequestFailedException)
        val requestFailedException = error as RequestFailedException
        assertEquals(pageNum..pageNum, requestFailedException.requestMetadata.pageRange)
        assert(requestFailedException.throwable is DeadObjectException)
    }

    @Test
    fun applyEditRecord_emitsRequestFailedExceptionForIllegalArgumentException() = runTest {
        val pageNum = 1
        val widgetIndex = 0
        val formEditRecord =
            FormEditRecord(pageNumber = pageNum, widgetIndex = widgetIndex, text = "New Text")

        `when`(pdfDocument.applyEdit(pageNum, formEditRecord)).thenAnswer {
            throw IllegalArgumentException()
        }

        val errors = mutableListOf<Throwable>()
        backgroundScope.launch(testDispatcher) { errorFlow.toList(errors) }

        handler.applyEditRecord(pageNum, formEditRecord)

        assertEquals(1, errors.size)
        val error = errors[0]
        assert(error is RequestFailedException)
        val requestFailedException = error as RequestFailedException
        assertEquals(pageNum..pageNum, requestFailedException.requestMetadata.pageRange)
        assert(requestFailedException.throwable is IllegalArgumentException)
    }
}

private const val TOTAL_PAGES = 100
private const val PAGE_WIDTH = 100
private const val PAGE_HEIGHT = 200
