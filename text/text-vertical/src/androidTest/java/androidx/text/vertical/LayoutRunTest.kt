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

package androidx.text.vertical

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.os.Build
import android.text.TextPaint
import androidx.test.filters.SdkSuppress
import androidx.text.vertical.ResolvedOrientation.Rotate
import androidx.text.vertical.ResolvedOrientation.TateChuYoko
import androidx.text.vertical.ResolvedOrientation.Upright
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.ceil
import kotlin.math.floor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
class LayoutRunTest {
    private val PREFIX = "PREFIX_PREFIX_PREFIX"
    private val SUFFIX = "SUFFIX_SUFFIX_SUFFIX"
    private val JAPANESE_TEXT = "あいうえお"
    private val LATIN_TEXT = "abcde"

    private val TEXT = PREFIX + LATIN_TEXT + JAPANESE_TEXT + SUFFIX
    private val LATIN_START = PREFIX.length
    private val LATIN_END = LATIN_START + LATIN_TEXT.length
    private val JAPANESE_START = LATIN_END
    private val JAPANESE_END = JAPANESE_START + JAPANESE_TEXT.length

    private val ONE_EM = 10f // make 1em = 10px
    private val HALF_EM = ONE_EM / 2

    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    private fun getVerticalAdvance(text: String): Float {
        val oldFlags = PAINT.flags
        PAINT.flags = PAINT.flags or Paint.VERTICAL_TEXT_FLAG
        try {
            return PAINT.measureText(text)
        } finally {
            PAINT.flags = oldFlags
        }
    }

    private fun getHorizontalAdvance(text: String): Float {
        val oldFlags = PAINT.flags
        PAINT.flags = PAINT.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        try {
            return PAINT.measureText(text)
        } finally {
            PAINT.flags = oldFlags
        }
    }

    private fun getHorizontalLineHeight(text: String): Float {
        val fm = FontMetricsInt()
        PAINT.getFontMetricsInt(text, 0, text.length, 0, text.length, false, fm)
        return (fm.descent - fm.ascent).toFloat()
    }

    private fun createLayoutRun(
        text: CharSequence,
        start: Int,
        end: Int,
        orientation: ResolvedOrientation,
    ) = createLayoutRun(text, start, end, PAINT, orientation)

    private class MockCanvas(val drawTextCallback: (CharSequence, Int, Int, Paint) -> Unit) :
        Canvas() {
        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            drawTextCallback(text, start, end, paint)
        }
    }

    @Test
    fun layoutRun_UprightSingleStyleLatin() {
        createLayoutRun(TEXT, LATIN_START, LATIN_END, Upright).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getVerticalAdvance(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(LATIN_START)
                    assertThat(end).isEqualTo(LATIN_END)
                    assertThat(paint.hasVerticalTextFlag()).isTrue()
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_UprightSingleStyleJapanese() {
        createLayoutRun(TEXT, JAPANESE_START, JAPANESE_END, Upright).run {
            assertThat(start).isEqualTo(JAPANESE_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getVerticalAdvance(JAPANESE_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(JAPANESE_START)
                    assertThat(end).isEqualTo(JAPANESE_END)
                    assertThat(paint.hasVerticalTextFlag()).isTrue()
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_RotateSingleStyleLatin() {
        createLayoutRun(TEXT, LATIN_START, LATIN_END, Rotate).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getHorizontalAdvance(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(LATIN_START)
                    assertThat(end).isEqualTo(LATIN_END)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_RotateSingleStyleJapanese() {
        createLayoutRun(TEXT, JAPANESE_START, JAPANESE_END, Rotate).run {
            assertThat(start).isEqualTo(JAPANESE_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getVerticalAdvance(JAPANESE_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(JAPANESE_START)
                    assertThat(end).isEqualTo(JAPANESE_END)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_TateChuYokoSingleStyleLatinCanFitInto1em() {
        createLayoutRun(TEXT, LATIN_START, LATIN_START + 1, TateChuYoko).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_START + 1)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getHorizontalLineHeight(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(LATIN_START)
                    assertThat(end).isEqualTo(LATIN_START + 1)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                    assertThat(paint.textScaleX).isEqualTo(1f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_TateChuYokoSingleStyleJapaneseCanFitInto1em() {
        createLayoutRun(TEXT, JAPANESE_START, JAPANESE_START + 1, TateChuYoko).run {
            assertThat(start).isEqualTo(JAPANESE_START)
            assertThat(end).isEqualTo(JAPANESE_START + 1)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height).isEqualTo(getHorizontalLineHeight(JAPANESE_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM) // rightSide is half of 1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(JAPANESE_START)
                    assertThat(end).isEqualTo(JAPANESE_START + 1)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                    assertThat(paint.textScaleX).isEqualTo(1f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_TateChuYokoSingleStyleLatinStretch() {
        createLayoutRun(TEXT, LATIN_START, LATIN_START + 4, TateChuYoko).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_START + 4)
            assertThat(width).isEqualTo(ONE_EM * 1.1f) // allocate 1.1em for stretched text
            assertThat(height).isEqualTo(getHorizontalLineHeight(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM * 1.1f) // leftSide is half of 1.1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM * 1.1f) // rightSide is half of 1.1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(LATIN_START)
                    assertThat(end).isEqualTo(LATIN_START + 4)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                    assertThat(paint.textScaleX).isNotEqualTo(1f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_TateChuYokoSingleStyleJapaneseStretch() {
        createLayoutRun(TEXT, JAPANESE_START, JAPANESE_START + 4, TateChuYoko).run {
            assertThat(start).isEqualTo(JAPANESE_START)
            assertThat(end).isEqualTo(JAPANESE_START + 4)
            assertThat(width).isEqualTo(ONE_EM * 1.1f) // allocate 1.1em for stretched text
            assertThat(height).isEqualTo(getHorizontalLineHeight(JAPANESE_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM * 1.1f) // leftSide is half of 1.1em
            assertThat(rightSideOffset).isEqualTo(HALF_EM * 1.1f) // rightSide is half of 1.1em

            draw(
                MockCanvas { text, start, end, paint ->
                    assertThat(text).isEqualTo(TEXT)
                    assertThat(start).isEqualTo(JAPANESE_START)
                    assertThat(end).isEqualTo(JAPANESE_START + 4)
                    assertThat(paint.hasVerticalTextFlag()).isFalse()
                    assertThat(paint.textScaleX).isNotEqualTo(1f)
                },
                0f,
                0f,
                PAINT,
            )
        }
    }

    @Test
    fun layoutRun_UprightPlainString_honorsBasePaintBgColor() {
        val bgPaint = bgTextPaint()
        val run = createLayoutRun(JAPANESE_TEXT, 0, JAPANESE_TEXT.length, bgPaint, Upright)
        assertRunDrawsBackground(run, bgPaint)
    }

    @Test
    fun layoutRun_RotatePlainString_honorsBasePaintBgColor() {
        val bgPaint = bgTextPaint()
        val run = createLayoutRun(LATIN_TEXT, 0, LATIN_TEXT.length, bgPaint, Rotate)
        assertRunDrawsBackground(run, bgPaint)
    }

    @Test
    fun rubyLayoutRun_plainStringRubyAnnotation_honorsBasePaintBgColor() {
        val bgPaint = bgTextPaint()
        val run = RubyLayoutRun("漢字", 0, 2, TextOrientation.Mixed, bgPaint, RubySpan("かな"))
        val bitmap =
            Bitmap.createBitmap(160, ceil(run.height).toInt() + 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }

        run.draw(canvas, ORIGIN_X, ORIGIN_Y, bgPaint)

        // Check specifically inside the ruby annotation column to the right of the 1em body column.
        val rubyLeft = ceil(ORIGIN_X + bgPaint.textSize * 0.5f).toInt() + 1
        val rubyRight = floor(ORIGIN_X + run.rightSideOffset).toInt()
        val top = ORIGIN_Y.toInt()
        val bottom = ceil(ORIGIN_Y + run.height).toInt()
        assertWithMessage(
                "Expected Color.YELLOW pixel in [%s, %s) x [%s, %s)",
                rubyLeft,
                rubyRight,
                top,
                bottom,
            )
            .that(bitmap.hasPixelWithColor(Color.YELLOW, rubyLeft, top, rubyRight, bottom))
            .isTrue()
    }

    private fun assertRunDrawsBackground(run: LayoutRun, bgPaint: TextPaint) {
        val bitmap =
            Bitmap.createBitmap(120, ceil(run.height).toInt() + 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }

        run.draw(canvas, ORIGIN_X, ORIGIN_Y, bgPaint)

        val left = floor(ORIGIN_X + run.leftSideOffset).toInt()
        val top = ORIGIN_Y.toInt()
        val right = ceil(ORIGIN_X + run.rightSideOffset).toInt()
        val bottom = ceil(ORIGIN_Y + run.height).toInt()
        assertWithMessage(
                "Expected Color.YELLOW pixel in [%s, %s) x [%s, %s)",
                left,
                right,
                top,
                bottom,
            )
            .that(bitmap.hasPixelWithColor(Color.YELLOW, left, top, right, bottom))
            .isTrue()
    }
}

/** Large enough that glyph strokes cannot cover every pixel of the background rectangle. */
private const val BG_TEXT_SIZE = 40f
private const val ORIGIN_X = 60f
private const val ORIGIN_Y = 10f

private fun bgTextPaint(): TextPaint =
    TextPaint().apply {
        textSize = BG_TEXT_SIZE
        color = Color.BLACK
        bgColor = Color.YELLOW
    }

private fun Paint.hasVerticalTextFlag() =
    (flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG

/** Returns true when any pixel in `[left, right) x [top, bottom)` equals [expectedColor]. */
private fun Bitmap.hasPixelWithColor(
    expectedColor: Int,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): Boolean {
    require(left in 0 until right && right <= width) {
        "Invalid horizontal range [$left, $right) for bitmap width $width"
    }
    require(top in 0 until bottom && bottom <= height) {
        "Invalid vertical range [$top, $bottom) for bitmap height $height"
    }
    for (y in top until bottom) {
        for (x in left until right) {
            if (getPixel(x, y) == expectedColor) {
                return true
            }
        }
    }
    return false
}
