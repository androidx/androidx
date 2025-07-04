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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.text.TextPaint
import androidx.text.vertical.ResolvedOrientation.Rotate
import androidx.text.vertical.ResolvedOrientation.TateChuYoko
import androidx.text.vertical.ResolvedOrientation.Upright
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextRunsTest {
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
        PAINT.flags = PAINT.flags or Paint.VERTICAL_TEXT_FLAG
        return PAINT.measureText(text)
    }

    private fun getHorizontalAdvance(text: String): Float {
        PAINT.flags = PAINT.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        return PAINT.measureText(text)
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
    fun `Upright SingleStyle Latin`() {
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
    fun `Upright SingleStyle Japanese`() {
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
    fun `Rotate SingleStyle Latin`() {
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
    fun `Rotate SingleStyle Japanese`() {
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
    fun `TateChuYoko SingleStyle Latin Can fit into 1em`() {
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
    fun `TateChuYoko SingleStyle Japanese Can fit into 1em`() {
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
    fun `TateChuYoko SingleStyle Latin Stretch`() {
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
    fun `TateChuYoko SingleStyle Japanese Stretch`() {
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
}

private fun Paint.hasVerticalTextFlag() =
    (flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG
