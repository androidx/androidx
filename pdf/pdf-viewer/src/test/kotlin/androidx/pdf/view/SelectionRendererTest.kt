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

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.SparseArray
import androidx.pdf.PdfPoint
import androidx.pdf.PdfRect
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.collections.List
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SelectionRendererTest {
    private lateinit var leftHandle: RecordingDrawable
    private lateinit var rightHandle: RecordingDrawable
    private lateinit var renderer: SelectionRenderer

    private val canvasSpy = spy(Canvas())

    @Before
    fun setup() {
        leftHandle = RecordingDrawable()
        rightHandle = RecordingDrawable()
        renderer =
            SelectionRenderer(
                ApplicationProvider.getApplicationContext(),
                { leftHandle },
                { rightHandle },
            )
    }

    @Test
    fun drawSelectionOnPage_rtl() {
        val textSelection =
            TextSelection(
                "This is the text that's selected",
                listOf(
                    PdfRect(pageNum = 0, RectF(150F, 150F, 190F, 160F)),
                    PdfRect(pageNum = 0, RectF(10F, 170F, 50F, 180F)),
                ),
            )
        val startBoundary =
            UiSelectionBoundary(PdfPoint(pageNum = 0, PointF(150F, 160F)), isRtl = true)
        val endBoundary =
            UiSelectionBoundary(PdfPoint(pageNum = 0, PointF(50F, 180F)), isRtl = true)
        val selection =
            SelectionModel(
                DocumentSelection(
                    SparseArray<List<Selection>>().apply { set(0, listOf(textSelection)) }
                ),
                startBoundary,
                endBoundary,
            )
        val locationInView = RectF(30f, 50f, 230f, 250f)
        val currentZoom = 2F

        renderer.drawSelectionOnPage(selection, pageNum = 0, canvasSpy, locationInView, currentZoom)

        // Handle's location in page, adjusted for page's location in View, adjusted in the way we
        // expect to position the "sharp point" of the handle, adjusted for the current zoom
        val startLeftAdjusted =
            startBoundary.location.x +
                locationInView.left +
                -0.25F * HANDLE_SIZE.x * 1 / currentZoom
        val startTopAdjusted =
            startBoundary.location.y + locationInView.top + -0.10F * HANDLE_SIZE.y * 1 / currentZoom
        assertThat(leftHandle.drawingBounds)
            .isEqualTo(
                Rect(
                    startLeftAdjusted.roundToInt(),
                    startTopAdjusted.roundToInt(),
                    (startLeftAdjusted + HANDLE_SIZE.x * 1 / currentZoom).roundToInt(),
                    (startTopAdjusted + HANDLE_SIZE.y * 1 / currentZoom).roundToInt(),
                )
            )

        // Handle's location in page, adjusted for page's location in View, adjusted in the way we
        // expect to position the "sharp point" of the handle, adjusted for the current zoom
        val endLeftAdjusted =
            endBoundary.location.x + locationInView.left + -0.75F * HANDLE_SIZE.x * 1 / currentZoom
        val endTopAdjusted =
            endBoundary.location.y + locationInView.top + -0.10F * HANDLE_SIZE.y * 1 / currentZoom
        assertThat(rightHandle.drawingBounds)
            .isEqualTo(
                Rect(
                    endLeftAdjusted.roundToInt(),
                    endTopAdjusted.roundToInt(),
                    (endLeftAdjusted + HANDLE_SIZE.x * 1 / currentZoom).roundToInt(),
                    (endTopAdjusted + HANDLE_SIZE.y * 1 / currentZoom).roundToInt(),
                )
            )

        for (bound in textSelection.bounds.map { RectF(it.left, it.top, it.right, it.bottom) }) {
            val expectedBounds =
                RectF(bound).apply { offset(locationInView.left, locationInView.top) }
            verify(canvasSpy).drawRect(eq(expectedBounds), eq(BOUNDS_PAINT))
        }
    }

    @Test
    fun drawSelectionOnPage_ltr() {
        val textSelection =
            TextSelection(
                "This is the text that's selected",
                listOf(
                    PdfRect(pageNum = 0, RectF(150F, 150F, 190F, 160F)),
                    PdfRect(pageNum = 0, RectF(10F, 170F, 50F, 180F)),
                ),
            )
        val startBoundary =
            UiSelectionBoundary(PdfPoint(pageNum = 0, PointF(150F, 160F)), isRtl = false)
        val endBoundary =
            UiSelectionBoundary(PdfPoint(pageNum = 0, PointF(50F, 180F)), isRtl = false)
        val selection =
            SelectionModel(
                DocumentSelection(
                    SparseArray<List<Selection>>().apply { set(0, listOf(textSelection)) }
                ),
                startBoundary,
                endBoundary,
            )
        val locationInView = RectF(30f, 50f, 230f, 250f)
        val currentZoom = 2F

        renderer.drawSelectionOnPage(selection, pageNum = 0, canvasSpy, locationInView, currentZoom)

        // Handle's location in page, adjusted for page's location in View, adjusted in the way we
        // expect to position the "sharp point" of the handle, adjusted for the current zoom
        val startLeftAdjusted =
            startBoundary.location.x +
                locationInView.left +
                -0.75F * HANDLE_SIZE.x * 1 / currentZoom
        val startTopAdjusted =
            startBoundary.location.y + locationInView.top + -0.10F * HANDLE_SIZE.y * 1 / currentZoom
        assertThat(rightHandle.drawingBounds)
            .isEqualTo(
                Rect(
                    startLeftAdjusted.roundToInt(),
                    startTopAdjusted.roundToInt(),
                    (startLeftAdjusted + HANDLE_SIZE.x * 1 / currentZoom).roundToInt(),
                    (startTopAdjusted + HANDLE_SIZE.y * 1 / currentZoom).roundToInt(),
                )
            )

        // Handle's location in page, adjusted for page's location in View, adjusted in the way we
        // expect to position the "sharp point" of the handle, adjusted for the current zoom
        val endLeftAdjusted =
            endBoundary.location.x + locationInView.left + -0.25F * HANDLE_SIZE.x * 1 / currentZoom
        val endTopAdjusted =
            endBoundary.location.y + locationInView.top + -0.10F * HANDLE_SIZE.y * 1 / currentZoom
        assertThat(leftHandle.drawingBounds)
            .isEqualTo(
                Rect(
                    endLeftAdjusted.roundToInt(),
                    endTopAdjusted.roundToInt(),
                    (endLeftAdjusted + HANDLE_SIZE.x * 1 / currentZoom).roundToInt(),
                    (endTopAdjusted + HANDLE_SIZE.y * 1 / currentZoom).roundToInt(),
                )
            )

        for (bound in textSelection.bounds.map { RectF(it.left, it.top, it.right, it.bottom) }) {
            val expectedBounds =
                RectF(bound).apply { offset(locationInView.left, locationInView.top) }
            verify(canvasSpy).drawRect(eq(expectedBounds), eq(BOUNDS_PAINT))
        }
    }
}

private val HANDLE_SIZE = Point(48, 48)

/** Simple fake [Drawable] that records its bounds at drawing time */
private class RecordingDrawable : Drawable() {
    var drawingBounds: Rect = Rect(-1, -1, -1, -1)

    override fun draw(canvas: Canvas) {
        copyBounds(drawingBounds)
    }

    override fun setAlpha(alpha: Int) {
        // No-op, fake
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // No-op, fake
    }

    override fun getIntrinsicWidth(): Int {
        return HANDLE_SIZE.x
    }

    override fun getIntrinsicHeight(): Int {
        return HANDLE_SIZE.y
    }

    // Deprecated, but must implement
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.OPAQUE
    }
}
