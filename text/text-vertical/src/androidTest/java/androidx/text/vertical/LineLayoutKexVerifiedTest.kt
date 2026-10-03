/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.text.vertical

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `LineLayout.kt` derived from Kex symbolic execution paths.
 *
 * Covers [createLineLayout] and [LineLayout] initialization, side offset aggregation, width,
 * height, boundary accessors, and multi-run canvas drawing.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class LineLayoutKexVerifiedTest {

    private val paint = TextPaint().apply { textSize = 20f }
    private var canvasBitmap: Bitmap? = null

    @After
    fun recycleCanvasBitmap() {
        canvasBitmap?.recycle()
        canvasBitmap = null
    }

    // =========================================================================================
    // Target 1: createLineLayout & LineLayout with empty runs
    // =========================================================================================

    // Branch: start >= end in createLineLayout produces an empty runs list with zeroed metrics.
    @Test
    fun kex_createLineLayout_whenStartEqualsOrExceedsEnd_createsEmptyLineLayout() {
        val equalRangeLayout =
            createLineLayout("あいう", start = 2, end = 2, paint = paint, TextOrientation.Mixed)
        val reversedRangeLayout =
            createLineLayout("あいう", start = 3, end = 1, paint = paint, TextOrientation.Upright)

        assertThat(equalRangeLayout.runs).isEmpty()
        assertThat(equalRangeLayout.leftSide).isEqualTo(0f)
        assertThat(equalRangeLayout.rightSide).isEqualTo(0f)
        assertThat(equalRangeLayout.width).isEqualTo(0f)
        assertThat(equalRangeLayout.height).isEqualTo(0f)

        assertThat(reversedRangeLayout.runs).isEmpty()
        assertThat(reversedRangeLayout.width).isEqualTo(0f)
        assertThat(reversedRangeLayout.height).isEqualTo(0f)
    }

    // Branch: LineLayout.start and LineLayout.end throw NoSuchElementException when runs is empty.
    @Test
    fun kex_lineLayout_startAndEnd_whenRunsIsEmpty_throwNoSuchElementException() {
        val emptyLayout = LineLayout(emptyList())

        assertThrows(NoSuchElementException::class.java) { emptyLayout.start }
        assertThrows(NoSuchElementException::class.java) { emptyLayout.end }
    }

    // =========================================================================================
    // Target 2: LineLayout fold aggregation & draw
    // =========================================================================================

    // Branch: runs.fold aggregates min(leftSideOffset), max(rightSideOffset), and sum(height)
    // across multiple runs with asymmetric side offsets.
    @Test
    fun kex_lineLayout_init_whenMultipleRunsWithAsymmetricOffsets_foldsMinLeftMaxRightAndSumHeight() {
        val spanned =
            SpannableString("あ1234い").apply {
                setSpan(
                    EmphasisSpan(position = AnnotationPosition.After),
                    0,
                    1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    TextOrientationSpan.CombineUpright(),
                    1,
                    5,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                setSpan(
                    EmphasisSpan(position = AnnotationPosition.Before),
                    5,
                    6,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }

        val layout = createLineLayout(spanned, 0, spanned.length, paint, TextOrientation.Mixed)

        assertThat(layout.runs).hasSize(3)
        assertThat(layout.start).isEqualTo(0)
        assertThat(layout.end).isEqualTo(spanned.length)

        assertThat(layout.runs[0].leftSideOffset).isWithin(0.001f).of(-20f)
        assertThat(layout.runs[1].leftSideOffset).isWithin(0.001f).of(-11f)
        assertThat(layout.runs[2].rightSideOffset).isWithin(0.001f).of(20f)
        assertThat(layout.leftSide).isWithin(0.001f).of(-20f)
        assertThat(layout.rightSide).isWithin(0.001f).of(20f)
        assertThat(layout.width).isWithin(0.001f).of(40f)
        assertThat(layout.height)
            .isWithin(0.001f)
            .of(layout.runs[0].height + layout.runs[1].height + layout.runs[2].height)
    }

    // Branch: LineLayout.draw when runs is empty does not draw onto the canvas.
    @Test
    fun kex_lineLayout_draw_whenRunsIsEmpty_doesNotDraw() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        canvasBitmap = bitmap
        val canvas = Canvas(bitmap)
        val emptyLayout = LineLayout(emptyList())

        emptyLayout.draw(canvas, 25f, 0f, paint)

        assertThat(bitmap.getPixel(25, 10)).isEqualTo(Color.TRANSPARENT)
    }

    // Branch: LineLayout.draw iterates through runs and advances y by each run.height.
    @Test
    fun kex_lineLayout_draw_whenMultipleRuns_advancesVerticalOriginByRunHeight() {
        val layout = createLineLayout("abあcd", 0, 5, paint, TextOrientation.Mixed)
        assertThat(layout.runs).hasSize(3)

        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        canvasBitmap = bitmap
        val recordedTranslateYs = mutableListOf<Float>()
        val canvas =
            object : Canvas(bitmap) {
                override fun translate(dx: Float, dy: Float) {
                    recordedTranslateYs.add(dy)
                    super.translate(dx, dy)
                }
            }

        layout.draw(canvas, 50f, 10f, paint)

        // First RotateLayoutRun ("ab") translates to originY (10f); second RotateLayoutRun ("cd")
        // translates to originY + runs[0].height + runs[1].height.
        assertThat(recordedTranslateYs)
            .containsAtLeast(10f, 10f + layout.runs[0].height + layout.runs[1].height)
            .inOrder()
    }
}
