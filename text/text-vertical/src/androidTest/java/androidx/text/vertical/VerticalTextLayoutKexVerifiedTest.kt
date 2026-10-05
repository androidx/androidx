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
import android.graphics.Paint
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `VerticalTextLayout.kt` derived from Kex symbolic execution paths.
 *
 * Covers [VerticalTextLayout] constructor precondition branches, default arguments,
 * [VerticalTextLayout.width], [VerticalTextLayout.lineCount], [VerticalTextLayout.draw], and
 * [VerticalTextLayout.isVerticalTextSupported].
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class VerticalTextLayoutKexVerifiedTest {

    private val paint = TextPaint().apply { textSize = 10f }
    private var canvasBitmap: Bitmap? = null

    @After
    fun recycleCanvasBitmap() {
        canvasBitmap?.recycle()
        canvasBitmap = null
    }

    // =========================================================================================
    // Target 1: VerticalTextLayout constructor precondition branches
    // =========================================================================================

    // Branch: start > end fails require(start <= end && end <= text.length && height >= 0).
    @Test
    fun kex_constructor_whenStartGreaterThanEnd_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VerticalTextLayout(text = "あいう", start = 2, end = 1, paint = paint, height = 100f)
        }
    }

    // Branch: start <= end && end > text.length fails require(start <= end && end <= text.length
    // && height >= 0).
    @Test
    fun kex_constructor_whenEndGreaterThanTextLength_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VerticalTextLayout(text = "あいう", start = 0, end = 4, paint = paint, height = 100f)
        }
    }

    // Branch: start <= end && end <= text.length && height < 0f fails require(start <= end &&
    // end <= text.length && height >= 0).
    @Test
    fun kex_constructor_whenHeightIsNegative_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VerticalTextLayout(
                text = "あいう",
                start = 0,
                end = 3,
                paint = paint,
                height = -Float.MIN_VALUE,
            )
        }
    }

    // Branch: height is Float.NaN (height >= 0 evaluates to false under IEEE-754) and fails
    // require.
    @Test
    fun kex_constructor_whenHeightIsNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            VerticalTextLayout(text = "あいう", start = 0, end = 3, paint = paint, height = Float.NaN)
        }
    }

    // =========================================================================================
    // Target 2: VerticalTextLayout default arguments, properties, draw, and capability query
    // =========================================================================================

    // Branch: Empty range (start == end, including default empty text) passes the constructor
    // require check at boundary start == end and currently throws IllegalArgumentException inside
    // LineBreaker.finish() because no runs are produced (not a VerticalTextLayout contract).
    @Test
    fun kex_constructor_whenRangeIsEmpty_currentlyThrowsFromLineBreaker() {
        val defaultEmptyError =
            assertThrows(IllegalArgumentException::class.java) { VerticalTextLayout() }
        val emptySliceError =
            assertThrows(IllegalArgumentException::class.java) {
                VerticalTextLayout(
                    text = "あいう",
                    start = 3,
                    end = 3,
                    paint = paint,
                    height = 0f,
                    orientation = TextOrientation.Upright,
                )
            }

        assertThat(defaultEmptyError).hasMessageThat().isEqualTo("Cannot break with empty runs.")
        assertThat(emptySliceError).hasMessageThat().isEqualTo("Cannot break with empty runs.")
    }

    // Branch: Default optional arguments (start = 0, end = text.length, paint = TextPaint(),
    // height = 0f, orientation = TextOrientation.Mixed) satisfy require at boundary height == 0f
    // and populate layout properties, draw, and capability query.
    @Test
    fun kex_constructor_withDefaultOptionalArguments_createsLayoutAndDraws() {
        val layout = VerticalTextLayout(text = "あ")
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        canvasBitmap = bitmap
        var drawCount = 0
        val canvas =
            object : Canvas(bitmap) {
                override fun drawText(
                    text: CharSequence,
                    start: Int,
                    end: Int,
                    x: Float,
                    y: Float,
                    paint: Paint,
                ) {
                    drawCount++
                    super.drawText(text, start, end, x, y, paint)
                }
            }

        layout.draw(canvas, 25f, 0f)

        assertThat(layout.text.toString()).isEqualTo("あ")
        assertThat(layout.start).isEqualTo(0)
        assertThat(layout.end).isEqualTo(1)
        assertThat(layout.paint).isNotSameInstanceAs(paint)
        assertThat(layout.paint.textSize).isEqualTo(TextPaint().textSize)
        assertThat(layout.height).isEqualTo(0f)
        assertThat(layout.orientation).isEqualTo(TextOrientation.Mixed)
        assertThat(layout.width).isGreaterThan(0f)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isVerticalTextSupported()).isTrue()
        assertThat(drawCount).isGreaterThan(0)
    }
}
