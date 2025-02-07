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
import androidx.pdf.content.PdfPageTextContent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancelChildren
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
            emptyList() // No images in this test case
        )

    private val pdfDocument =
        mock<PdfDocument> {
            on { getPageBitmapSource(any()) } doAnswer
                { invocation ->
                    FakeBitmapSource(invocation.getArgument(0))
                }
            onBlocking { getPageContent(pageNumber = 0) } doReturn pageContent
        }

    private val canvasSpy = spy(Canvas())

    private var invalidationCounter = 0
    private val invalidationTracker: () -> Unit = { invalidationCounter++ }

    private var pageTextReadyCounter = 0
    private val onPageTextReady: ((Int) -> Unit) = { _ -> pageTextReadyCounter++ }

    private lateinit var page: Page

    private fun createPage(isTouchExplorationEnabled: Boolean): Page {
        return Page(
            0,
            pageSize = PAGE_SIZE,
            pdfDocument,
            testScope,
            MAX_BITMAP_SIZE,
            isTouchExplorationEnabled = isTouchExplorationEnabled,
            invalidationTracker,
            onPageTextReady
        )
    }

    @Before
    fun setup() {
        // Cancel any work from previous tests, and reset tracking variables
        testDispatcher.cancelChildren()
        invalidationCounter = 0
        pageTextReadyCounter = 0

        page = createPage(isTouchExplorationEnabled = true)
    }

    @Test
    fun draw_withoutBitmap() {
        // Notably we don't call testDispatcher.scheduler.runCurrent(), so we start, but do not
        // finish, fetching a Bitmap
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        val locationInView = Rect(-60, 125, -60 + PAGE_SIZE.x, 125 + PAGE_SIZE.y)

        page.draw(canvasSpy, locationInView, listOf())

        verify(canvasSpy).drawRect(eq(locationInView), eq(BLANK_PAINT))
    }

    @Test
    fun draw_withBitmap() {
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        val locationInView = Rect(50, -100, 50 + PAGE_SIZE.x, -100 + PAGE_SIZE.y)

        page.draw(canvasSpy, locationInView, listOf())

        verify(canvasSpy)
            .drawBitmap(
                argThat { bitmap ->
                    bitmap.width == (PAGE_SIZE.x * 1.5).toInt() &&
                        bitmap.height == (PAGE_SIZE.y * 1.5).toInt()
                },
                isNull(),
                eq(locationInView),
                eq(BMP_PAINT)
            )
    }

    @Test
    fun draw_withHighlight() {
        page.setVisible(zoom = 1.5F, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        val leftEdgeInView = 650
        val topEdgeInView = -320
        val locationInView =
            Rect(
                leftEdgeInView,
                topEdgeInView,
                leftEdgeInView + PAGE_SIZE.x,
                topEdgeInView + PAGE_SIZE.y
            )
        val highlight = Highlight(PdfRect(pageNum = 0, RectF(10F, 0F, 30F, 20F)), Color.YELLOW)

        page.draw(canvasSpy, locationInView, listOf(highlight))

        // We expect to draw the highlight at its location in page coordinates offset by the page's
        // location in View
        val expectedHighlightLoc =
            RectF().apply {
                set(highlight.area.pageRect)
                offset(leftEdgeInView.toFloat(), topEdgeInView.toFloat())
            }
        verify(canvasSpy).drawRect(eq(expectedHighlightLoc), argThat { color == highlight.color })

        // Foot note: It's impossible to verify the behavior when drawing multiple highlights using
        // Mockito's Spy functionality, as it captures arguments by reference, and we re-use
        // Rect and Paint arguments to canvas.drawRect() to avoid allocations on the drawing path
    }

    @Test
    fun updateState_withTouchExplorationEnabled_fetchesPageText() {
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()
        assertThat(page.pageText).isEqualTo("SampleText")
        assertThat(pageTextReadyCounter).isEqualTo(1)
    }

    @Test
    fun setVisible_withTouchExplorationDisabled_doesNotFetchPageText() {
        page = createPage(isTouchExplorationEnabled = false)
        page.setVisible(zoom = 1.0f, FULL_PAGE_RECT)
        testDispatcher.scheduler.runCurrent()

        assertThat(page.pageText).isEqualTo(null)
        assertThat(pageTextReadyCounter).isEqualTo(0)
    }

    @Test
    fun updateState_doesNotFetchPageTextIfAlreadyFetched() {
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
}

val PAGE_SIZE = Point(100, 150)
val FULL_PAGE_RECT = Rect(0, 0, PAGE_SIZE.x, PAGE_SIZE.y)
val MAX_BITMAP_SIZE = Point(500, 500)
