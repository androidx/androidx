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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import androidx.pdf.PdfDocument
import androidx.pdf.PdfRect
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.models.FormWidgetInfo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val pageContent =
        PdfDocument.PdfPageContent(
            listOf(PdfPageTextContent(listOf(RectF(10f, 10f, 50f, 20f)), "SampleText")),
            emptyList(), // No images in this test case
        )

    private val pdfDocument =
        mock<PdfDocument> {
            on { getPageBitmapSource(any()) } doAnswer
                { invocation ->
                    FakeBitmapSource(invocation.getArgument(0))
                }
            onBlocking { getPageContent(pageNumber = 0) } doReturn pageContent
            onBlocking { getFormWidgetInfos(any()) } doReturn UPDATED_PAGE_WIDGET_INFOS
        }

    private val canvasSpy = spy(Canvas())

    private var invalidationCounter = 0
    private val invalidationTracker: () -> Unit = { invalidationCounter++ }

    private var pageTextReadyCounter = 0
    private val onPageTextReady: ((Int) -> Unit) = { _ -> pageTextReadyCounter++ }

    private lateinit var page: Page

    private val errorFlow = MutableSharedFlow<Throwable>()

    private fun createPage(): Page {
        return Page(
            pageNum = 0,
            pageSize = PAGE_SIZE,
            pdfDocument = pdfDocument,
            backgroundScope = testScope,
            maxBitmapSizePx = MAX_BITMAP_SIZE,
            onPageUpdate = invalidationTracker,
            onPageTextReady = onPageTextReady,
            errorFlow = errorFlow,
            isAccessibilityEnabled = true,
            formWidgetInfos =
                listOf(
                    FormWidgetInfo(
                        widgetIndex = 0,
                        widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                        widgetRect = Rect(10, 10, 20, 20),
                        textValue = "true",
                        accessibilityLabel = "radio",
                    ),
                    FormWidgetInfo(
                        widgetIndex = 0,
                        widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
                        widgetRect = Rect(10, 10, 20, 20),
                        textValue = "false",
                        accessibilityLabel = "radio",
                    ),
                ),
            pdfFormFillingConfig = PdfFormFillingConfig({ false }, Color.CYAN),
        )
    }

    @Before
    fun setup() {
        // Cancel any work from previous tests, and reset tracking variables
        testDispatcher.cancelChildren()
        invalidationCounter = 0
        pageTextReadyCounter = 0

        page = createPage()
    }

    @Test
    fun draw_withoutBitmap() {
        // Notably we don't call testDispatcher.scheduler.runCurrent(), so we start, but do not
        // finish, fetching a Bitmap
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        val locationInView = RectF(-60f, 125f, -60f + PAGE_SIZE.x, 125f + PAGE_SIZE.y)

        page.draw(canvasSpy, locationInView, listOf())

        verify(canvasSpy).drawRect(eq(locationInView), eq(BLANK_PAINT))
    }

    @Test
    fun draw_withBitmap() {
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        val locationInView = RectF(50f, -100f, 50f + PAGE_SIZE.x, -100f + PAGE_SIZE.y)

        page.draw(canvasSpy, locationInView, listOf())

        verify(canvasSpy)
            .drawBitmap(
                argThat { bitmap ->
                    bitmap.width == (PAGE_SIZE.x * 1.5).toInt() &&
                        bitmap.height == (PAGE_SIZE.y * 1.5).toInt()
                },
                isNull(),
                eq(locationInView),
                eq(BMP_PAINT),
            )
    }

    @Test
    fun draw_withHighlight() {
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        val leftEdgeInView = 650f
        val topEdgeInView = -320f
        val locationInView =
            RectF(
                leftEdgeInView,
                topEdgeInView,
                leftEdgeInView + PAGE_SIZE.x,
                topEdgeInView + PAGE_SIZE.y,
            )
        val highlight = Highlight(PdfRect(pageNum = 0, RectF(10F, 0F, 30F, 20F)), Color.YELLOW)

        page.draw(canvasSpy, locationInView, listOf(highlight))

        // We expect to draw the highlight at its location in page coordinates offset by the page's
        // location in View
        val expectedHighlightLoc =
            RectF().apply {
                set(
                    highlight.area.left,
                    highlight.area.top,
                    highlight.area.right,
                    highlight.area.bottom,
                )
                offset(leftEdgeInView, topEdgeInView)
            }
        verify(canvasSpy).drawRect(eq(expectedHighlightLoc), argThat { color == highlight.color })

        // Foot note: It's impossible to verify the behavior when drawing multiple highlights using
        // Mockito's Spy functionality, as it captures arguments by reference, and we re-use
        // Rect and Paint arguments to canvas.drawRect() to avoid allocations on the drawing path
    }

    @Test
    fun updateState_withAccessibilityEnabled_fetchesPageText() {
        page.isAccessibilityEnabled = true
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        assertThat(page.pageText).isEqualTo("SampleText")
        assertThat(pageTextReadyCounter).isEqualTo(1)
    }

    @Test
    fun setVisible_withAccessibilityDisabled_doesNotFetchPageText() {
        page.isAccessibilityEnabled = false
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()

        assertThat(page.pageText).isEqualTo(null)
        assertThat(pageTextReadyCounter).isEqualTo(0)
    }

    @Test
    fun updateState_doesNotFetchPageTextIfAlreadyFetched() {
        page.isAccessibilityEnabled = true
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        assertThat(page.pageText).isEqualTo("SampleText")
        assertThat(pageTextReadyCounter).isEqualTo(1)

        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        assertThat(page.pageText).isEqualTo("SampleText")
        assertThat(pageTextReadyCounter).isEqualTo(1)
    }

    @Test
    fun setPageInvisible_cancelsTextFetch() {
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        page.setInvisible()
        testDispatcher.scheduler.runCurrent()
        assertThat(page.pageText).isNull()
        assertThat(pageTextReadyCounter).isEqualTo(0)
    }

    @Test
    fun maybeUpdateFormWidgetInfos_updatesFormWidgetInfos() {
        page.maybeUpdateFormWidgetInfos(
            FormWidgetMetadataLoader(pdfDocument, PdfFormFillingState(10), mock())
        )
        testDispatcher.scheduler.runCurrent()
        assertThat(page.formWidgetInfos).isNotNull()
        assertThat(page.formWidgetInfos?.size).isEqualTo(2)
        assertThat(page.formWidgetInfos).isEqualTo(UPDATED_PAGE_WIDGET_INFOS)
    }
}

val PAGE_SIZE = Point(100, 150)
val FULL_PAGE_RECT = RectF(0f, 0f, PAGE_SIZE.x.toFloat(), PAGE_SIZE.y.toFloat())
val MAX_BITMAP_SIZE = Point(500, 500)
val UPDATED_PAGE_WIDGET_INFOS =
    listOf(
        FormWidgetInfo(
            widgetIndex = 0,
            widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            widgetRect = Rect(10, 10, 20, 20),
            textValue = "false",
            accessibilityLabel = "radio",
        ),
        FormWidgetInfo(
            widgetIndex = 0,
            widgetType = FormWidgetInfo.WIDGET_TYPE_RADIOBUTTON,
            widgetRect = Rect(10, 10, 20, 20),
            textValue = "true",
            accessibilityLabel = "radio",
        ),
    )
