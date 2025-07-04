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
import android.text.SpannableString
import android.text.TextPaint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

class RubyTest {
    private val PREFIX = "PREFIX_PREFIX_PREFIX"
    private val SUFFIX = "SUFFIX_SUFFIX_SUFFIX"
    private val LATIN_TEXT = "abcde"
    private val RUBY_TEXT = "ABCDE"

    private val TEXT = PREFIX + LATIN_TEXT + SUFFIX
    private val LATIN_START = PREFIX.length
    private val LATIN_END = LATIN_START + LATIN_TEXT.length

    private val ONE_EM = 10f // make 1em = 10px
    private val HALF_EM = ONE_EM / 2

    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    private fun getVerticalAdvance(text: String, scaleFactor: Float = 1.0f): Float {
        PAINT.flags = PAINT.flags or Paint.VERTICAL_TEXT_FLAG
        PAINT.textSize = ONE_EM * scaleFactor
        try {
            return PAINT.measureText(text)
        } finally {
            PAINT.textSize = ONE_EM
        }
    }

    private fun getHorizontalAdvance(text: String, scaleFactor: Float = 1.0f): Float {
        PAINT.flags = PAINT.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        PAINT.textSize = ONE_EM * scaleFactor
        try {
            return PAINT.measureText(text)
        } finally {
            PAINT.textSize = ONE_EM
        }
    }

    private class MockCanvas() : Canvas() {
        data class DrawTextRunCall(
            val text: CharSequence,
            val start: Int,
            val end: Int,
            val paint: Paint,
        )

        val invocations = mutableListOf<DrawTextRunCall>()

        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            invocations.add(DrawTextRunCall(text, start, end, Paint(paint)))
        }
    }

    @Test
    fun `Ruby Builder build and get - default`() {
        RubySpan.Builder(RUBY_TEXT).build().run {
            assertThat(text).isEqualTo(RUBY_TEXT)
            assertThat(orientation).isEqualTo(TextOrientation.MIXED)
            assertThat(textScale).isEqualTo(0.5f)
        }
    }

    @Test
    fun `Ruby Builder build and get - customize`() {
        RubySpan.Builder(RUBY_TEXT)
            .setOrientation(TextOrientation.UPRIGHT)
            .setTextScale(0.3f)
            .build()
            .run {
                assertThat(text).isEqualTo(RUBY_TEXT)
                assertThat(orientation).isEqualTo(TextOrientation.UPRIGHT)
                assertThat(textScale).isEqualTo(0.3f)
            }
    }

    @Test
    fun `RubyLayout create - Ruby is shorter than base text`() {
        val rubySpan = RubySpan.Builder(RUBY_TEXT).build()
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.MIXED, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // Since the ruby is shorter than base text, the base text is height for the run.
            assertThat(height).isEqualTo(getHorizontalAdvance(LATIN_TEXT))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
        }
    }

    @Test
    fun `RubyLayout create - Ruby is longer than base text`() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan = RubySpan.Builder(LONG_RUBY_TEXT).build()
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.MIXED, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // Since the ruby is longer than base text, the ruby text is height for the run.
            assertThat(height).isEqualTo(getHorizontalAdvance(LONG_RUBY_TEXT, 0.5f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(LONG_RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
        }
    }

    @Test
    fun `RubyLayout create - Ruby upright orientation`() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan =
            RubySpan.Builder(LONG_RUBY_TEXT).setOrientation(TextOrientation.UPRIGHT).build()
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.MIXED, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.5f) // 1em for base text, 0.5em for ruby.
            // The ruby text is layout with UPRIGHT orientation. Therefore, the vertical advance
            // is used for the height.
            assertThat(height).isEqualTo(getVerticalAdvance(LONG_RUBY_TEXT, 0.5f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            assertThat(rightSideOffset)
                .isEqualTo(ONE_EM) // right half of 1em + 0.5em for ruby width.

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(LONG_RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isTrue()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.5f)
            }
        }
    }

    @Test
    fun `RubyLayout create - Ruby scale`() {
        val LONG_RUBY_TEXT = RUBY_TEXT.repeat(10)
        val rubySpan = RubySpan.Builder(LONG_RUBY_TEXT).setTextScale(0.3f).build()
        RubyLayoutRun(TEXT, LATIN_START, LATIN_END, TextOrientation.MIXED, PAINT, rubySpan).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(LATIN_END)
            assertThat(width).isEqualTo(ONE_EM * 1.3f) // 1em for base text, 0.5em for ruby.
            // The ruby text is layout with UPRIGHT orientation. Therefore, the vertical advance
            // is used for the height.
            assertThat(height).isEqualTo(getHorizontalAdvance(LONG_RUBY_TEXT, 0.3f /* scale */))
            assertThat(leftSideOffset).isEqualTo(-HALF_EM) // leftSide is half of 1em
            // right half of 1em + 0.5em for ruby width.
            assertThat(rightSideOffset).isEqualTo(HALF_EM + 0.3f * ONE_EM)

            val mock = MockCanvas()
            draw(mock, 0f, 0f, PAINT)
            assertThat(mock.invocations.size).isEqualTo(2)
            val bodyIndex = if (mock.invocations[0].text == TEXT) 0 else 1
            val rubyIndex = if (bodyIndex == 0) 1 else 0

            mock.invocations[bodyIndex].run {
                assertThat(start).isEqualTo(LATIN_START)
                assertThat(end).isEqualTo(LATIN_END)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize)
            }
            mock.invocations[rubyIndex].run {
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(LONG_RUBY_TEXT.length)
                assertThat(paint.hasVerticalTextFlag()).isFalse()
                assertThat(paint.textSize).isEqualTo(PAINT.textSize * 0.3f)
            }
        }
    }
}

private fun Paint.hasVerticalTextFlag() =
    (flags and Paint.VERTICAL_TEXT_FLAG) == Paint.VERTICAL_TEXT_FLAG
