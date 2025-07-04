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

import android.graphics.Paint
import android.text.SpannableString
import android.text.TextPaint
import androidx.text.vertical.TextOrientationSpan.TextCombineUpright
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

@RunWith(JUnit4::class)
class LineBreakerTest {
    private val PREFIX = "PREFIX_PREFIX_PREFIX"
    private val SUFFIX = "SUFFIX_SUFFIX_SUFFIX"

    private val EM = 10f // make 1em = 10px
    private val HALF_EM = EM * 0.5f

    private val PAINT = TextPaint().apply { textSize = EM }

    private fun getVerticalAdvance(text: String): Float {
        PAINT.flags = PAINT.flags or Paint.VERTICAL_TEXT_FLAG
        return PAINT.measureText(text)
    }

    private fun getHorizontalAdvance(text: String, scaleFactor: Float = 1.0f): Float {
        PAINT.flags = PAINT.flags and Paint.VERTICAL_TEXT_FLAG.inv()
        PAINT.textSize = EM * scaleFactor
        try {
            return PAINT.measureText(text)
        } finally {
            PAINT.textSize = EM
        }
    }

    private fun getHorizontalLineHeight(text: String): Float {
        val fm = Paint.FontMetricsInt()
        PAINT.getFontMetricsInt(text, 0, text.length, 0, text.length, false, fm)
        return (fm.descent - fm.ascent).toFloat()
    }

    @Test
    fun `BreakLine all upright Japanese`() {
        val jpText = "吾輩は猫である。"
        val text = PREFIX + jpText + SUFFIX
        val jpStart = PREFIX.length
        val jpEnd = jpStart + jpText.length
        val breakText: (Float) -> LineBreaker.Result = { heightConstraint ->
            LineBreaker.breakTextIntoLines(
                text,
                jpStart,
                jpEnd,
                PAINT,
                heightConstraint,
                TextOrientation.MIXED,
            )
        }

        // The following test verifies the behavior of breaking "吾輩は猫である。"
        breakText(getVerticalAdvance(jpText)).also {
            // ----
            // 吾
            // 輩
            // は
            // 猫
            // で
            // あ
            // る
            // 。
            // ----
            assertThat(it.lineCount).isEqualTo(1)
            assertThat(it.width).isEqualTo(1 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 8)
        }

        breakText(getVerticalAdvance(jpText) * 0.9f).also {
            // ----
            // る吾
            // 。輩
            // 　は
            // 　猫
            // 　で
            // 　あ
            // ----
            assertThat(it.lineCount).isEqualTo(2)
            assertThat(it.width).isEqualTo(2 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 6)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 6)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 8)
        }

        breakText(getVerticalAdvance(jpText) * 0.4f).also {
            // ----
            // る猫吾
            // 。で輩
            // 　あは
            // ----
            assertThat(it.lineCount).isEqualTo(3)
            assertThat(it.width).isEqualTo(3 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 3)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 3)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 6)
            assertThat(it.getLineStart(2)).isEqualTo(jpStart + 6)
            assertThat(it.getLineEnd(2)).isEqualTo(jpStart + 8)
        }

        breakText(getVerticalAdvance(jpText) * 0.25f).also {
            // ----
            // るでは吾
            // 。あ猫輩
            // ----
            assertThat(it.lineCount).isEqualTo(4)
            assertThat(it.width).isEqualTo(4 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 2)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 2)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 4)
            assertThat(it.getLineStart(2)).isEqualTo(jpStart + 4)
            assertThat(it.getLineEnd(2)).isEqualTo(jpStart + 6)
            assertThat(it.getLineStart(3)).isEqualTo(jpStart + 6)
            assertThat(it.getLineEnd(3)).isEqualTo(jpStart + 8)
        }

        breakText(getVerticalAdvance(jpText) * 0.1f).also {
            // ----
            // 。るあで猫は輩吾
            // ----
            assertThat(it.lineCount).isEqualTo(8)
            assertThat(it.width).isEqualTo(8 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 1)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 1)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 2)
            assertThat(it.getLineStart(2)).isEqualTo(jpStart + 2)
            assertThat(it.getLineEnd(2)).isEqualTo(jpStart + 3)
            assertThat(it.getLineStart(3)).isEqualTo(jpStart + 3)
            assertThat(it.getLineEnd(3)).isEqualTo(jpStart + 4)
            assertThat(it.getLineStart(4)).isEqualTo(jpStart + 4)
            assertThat(it.getLineEnd(4)).isEqualTo(jpStart + 5)
            assertThat(it.getLineStart(5)).isEqualTo(jpStart + 5)
            assertThat(it.getLineEnd(5)).isEqualTo(jpStart + 6)
            assertThat(it.getLineStart(6)).isEqualTo(jpStart + 6)
            assertThat(it.getLineEnd(6)).isEqualTo(jpStart + 7)
            assertThat(it.getLineStart(7)).isEqualTo(jpStart + 7)
            assertThat(it.getLineEnd(7)).isEqualTo(jpStart + 8)
        }
    }

    @Test
    fun `BreakLine all rotate Latin`() {
        val enText = "Hello, world."
        val text = PREFIX + enText + SUFFIX
        val enStart = PREFIX.length
        val enEnd = enStart + enText.length
        val breakText: (Float) -> LineBreaker.Result = { heightConstraint ->
            LineBreaker.breakTextIntoLines(
                text,
                enStart,
                enEnd,
                PAINT,
                heightConstraint,
                TextOrientation.MIXED,
            )
        }

        // The following test verifies the behavior of breaking "Hello, World."
        // Each letter are rotated.
        breakText(getHorizontalAdvance(enText)).also {
            // |Hello, World.|
            // Note: the actual text is 90 degree rotated clockwise.
            assertThat(it.lineCount).isEqualTo(1)
            assertThat(it.width).isEqualTo(1 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 13)
        }

        breakText(getHorizontalAdvance(enText) * 0.9f).also {
            // |Hello,     |
            // |World.     |
            // Note: the actual text is 90 degree rotated clockwise.
            assertThat(it.lineCount).isEqualTo(2)
            assertThat(it.width).isEqualTo(2 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 7)
            assertThat(it.getLineStart(1)).isEqualTo(enStart + 7)
            assertThat(it.getLineEnd(1)).isEqualTo(enStart + 13)
        }

        breakText(1.0f).also {
            // |H|
            // |e|
            // |l|
            // |l|
            // |o|
            // |,|
            // | |  // TODO: The line end whitespace should not be counted line width.
            // |W|
            // |o|
            // |r|
            // |l|
            // |d|
            // |.|
            // Note: the actual text is 90 degree rotated clockwise.
            assertThat(it.lineCount).isEqualTo(13)
            assertThat(it.width).isEqualTo(13 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 1)
            assertThat(it.getLineStart(1)).isEqualTo(enStart + 1)
            assertThat(it.getLineEnd(1)).isEqualTo(enStart + 2)
            assertThat(it.getLineStart(2)).isEqualTo(enStart + 2)
            assertThat(it.getLineEnd(2)).isEqualTo(enStart + 3)
            assertThat(it.getLineStart(3)).isEqualTo(enStart + 3)
            assertThat(it.getLineEnd(3)).isEqualTo(enStart + 4)
            assertThat(it.getLineStart(4)).isEqualTo(enStart + 4)
            assertThat(it.getLineEnd(4)).isEqualTo(enStart + 5)
            assertThat(it.getLineStart(5)).isEqualTo(enStart + 5)
            assertThat(it.getLineEnd(5)).isEqualTo(enStart + 6)
            assertThat(it.getLineStart(6)).isEqualTo(enStart + 6)
            assertThat(it.getLineEnd(6)).isEqualTo(enStart + 7)
            assertThat(it.getLineStart(7)).isEqualTo(enStart + 7)
            assertThat(it.getLineEnd(7)).isEqualTo(enStart + 8)
            assertThat(it.getLineStart(8)).isEqualTo(enStart + 8)
            assertThat(it.getLineEnd(8)).isEqualTo(enStart + 9)
            assertThat(it.getLineStart(9)).isEqualTo(enStart + 9)
            assertThat(it.getLineEnd(9)).isEqualTo(enStart + 10)
            assertThat(it.getLineStart(10)).isEqualTo(enStart + 10)
            assertThat(it.getLineEnd(10)).isEqualTo(enStart + 11)
            assertThat(it.getLineStart(11)).isEqualTo(enStart + 11)
            assertThat(it.getLineEnd(11)).isEqualTo(enStart + 12)
            assertThat(it.getLineStart(12)).isEqualTo(enStart + 12)
            assertThat(it.getLineEnd(12)).isEqualTo(enStart + 13)
        }
    }

    @Test
    fun `BreakLine all upright Latin`() {
        val enText = "Hello, World."
        val text = PREFIX + enText + SUFFIX
        val enStart = PREFIX.length
        val enEnd = enStart + enText.length
        val breakText: (Float) -> LineBreaker.Result = { heightConstraint ->
            LineBreaker.breakTextIntoLines(
                text,
                enStart,
                enEnd,
                PAINT,
                heightConstraint,
                TextOrientation.UPRIGHT,
            )
        }

        breakText(getVerticalAdvance(enText)).also {
            // --
            // H
            // e
            // l
            // l
            // o
            // ,
            //
            // W
            // o
            // r
            // l
            // d
            // .
            // --
            assertThat(it.lineCount).isEqualTo(1)
            assertThat(it.width).isEqualTo(1 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 13)
        }

        breakText(getVerticalAdvance(enText) * 0.9f).also {
            // --
            // WH
            // oe
            // rl
            // ll
            // do
            // .,
            //
            //
            //
            //
            // --
            assertThat(it.lineCount).isEqualTo(2)
            assertThat(it.width).isEqualTo(2 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 7)
            assertThat(it.getLineStart(1)).isEqualTo(enStart + 7)
            assertThat(it.getLineEnd(1)).isEqualTo(enStart + 13)
        }

        breakText(1.0f).also {
            // -------------
            // .dlroW olleH
            // -------------
            assertThat(it.lineCount).isEqualTo(13)
            assertThat(it.width).isEqualTo(13 * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM)
            assertThat(it.getLineStart(0)).isEqualTo(enStart)
            assertThat(it.getLineEnd(0)).isEqualTo(enStart + 1)
            assertThat(it.getLineStart(1)).isEqualTo(enStart + 1)
            assertThat(it.getLineEnd(1)).isEqualTo(enStart + 2)
            assertThat(it.getLineStart(2)).isEqualTo(enStart + 2)
            assertThat(it.getLineEnd(2)).isEqualTo(enStart + 3)
            assertThat(it.getLineStart(3)).isEqualTo(enStart + 3)
            assertThat(it.getLineEnd(3)).isEqualTo(enStart + 4)
            assertThat(it.getLineStart(4)).isEqualTo(enStart + 4)
            assertThat(it.getLineEnd(4)).isEqualTo(enStart + 5)
            assertThat(it.getLineStart(5)).isEqualTo(enStart + 5)
            assertThat(it.getLineEnd(5)).isEqualTo(enStart + 6)
            assertThat(it.getLineStart(6)).isEqualTo(enStart + 6)
            assertThat(it.getLineEnd(6)).isEqualTo(enStart + 7)
            assertThat(it.getLineStart(7)).isEqualTo(enStart + 7)
            assertThat(it.getLineEnd(7)).isEqualTo(enStart + 8)
            assertThat(it.getLineStart(8)).isEqualTo(enStart + 8)
            assertThat(it.getLineEnd(8)).isEqualTo(enStart + 9)
            assertThat(it.getLineStart(9)).isEqualTo(enStart + 9)
            assertThat(it.getLineEnd(9)).isEqualTo(enStart + 10)
            assertThat(it.getLineStart(10)).isEqualTo(enStart + 10)
            assertThat(it.getLineEnd(10)).isEqualTo(enStart + 11)
            assertThat(it.getLineStart(11)).isEqualTo(enStart + 11)
            assertThat(it.getLineEnd(11)).isEqualTo(enStart + 12)
            assertThat(it.getLineStart(12)).isEqualTo(enStart + 12)
            assertThat(it.getLineEnd(12)).isEqualTo(enStart + 13)
        }
    }

    @Test
    fun `BreakLine TateChuYoko`() {
        val jpDateText = "2024年12月25日"
        val jpStart = PREFIX.length
        val jpEnd = jpStart + jpDateText.length
        val spanned =
            SpannableString(PREFIX + jpDateText + SUFFIX).apply {
                setSpan(TextCombineUpright(), jpStart, jpStart + 4, SPAN_FLAG) // 2024
                setSpan(TextCombineUpright(), jpStart + 5, jpStart + 7, SPAN_FLAG) // 12
                setSpan(TextCombineUpright(), jpStart + 8, jpStart + 10, SPAN_FLAG) // 25
            }
        val breakText: (Float) -> LineBreaker.Result = { heightConstraint ->
            LineBreaker.breakTextIntoLines(
                spanned,
                jpStart,
                jpEnd,
                PAINT,
                heightConstraint,
                TextOrientation.MIXED,
            )
        }

        val h2024 = getHorizontalLineHeight("2024")
        val hYear = getVerticalAdvance("年")
        val h12 = getHorizontalLineHeight("12")
        val hMonth = getVerticalAdvance("月")
        val h25 = getHorizontalLineHeight("25")
        val hDay = getVerticalAdvance("日")

        breakText(h2024 + hYear + h12 + hMonth + h25 + hDay).also {
            // ----
            // [2024]
            // 年
            // [12]
            // 月
            // [25]
            // 日
            // ----
            assertThat(it.lineCount).isEqualTo(1)
            assertThat(it.width).isEqualTo(1.1f * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM * 1.1f)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM * 1.1f)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 11)
        }

        breakText(h2024 + hYear + h12 + hMonth).also {
            // ----
            // [25]　[2024]
            // 日　　　年
            // 　　　　[12]
            // 　　　　月
            // ----
            assertThat(it.lineCount).isEqualTo(2)
            assertThat(it.width).isEqualTo(2 * 1.1f * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM * 1.1f)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM * 1.1f)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 8)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 8)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 11)
        }

        breakText(h2024 * 0.9f).also {
            // ----
            // 日[25]月[12]年[2024]
            // ----
            assertThat(it.lineCount).isEqualTo(6)
            assertThat(it.width).isEqualTo(6 * 1.1f * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM * 1.1f)
            assertThat(it.lineRightSide).isEqualTo(HALF_EM * 1.1f)
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 4)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 4)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 5)
            assertThat(it.getLineStart(2)).isEqualTo(jpStart + 5)
            assertThat(it.getLineEnd(2)).isEqualTo(jpStart + 7)
            assertThat(it.getLineStart(3)).isEqualTo(jpStart + 7)
            assertThat(it.getLineEnd(3)).isEqualTo(jpStart + 8)
            assertThat(it.getLineStart(4)).isEqualTo(jpStart + 8)
            assertThat(it.getLineEnd(4)).isEqualTo(jpStart + 10)
            assertThat(it.getLineStart(5)).isEqualTo(jpStart + 10)
            assertThat(it.getLineEnd(5)).isEqualTo(jpStart + 11)
        }
    }

    @Test
    fun `BreakLine Ruby`() {
        val jpText = "吾輩は猫である。"
        val jpStart = PREFIX.length
        val jpEnd = jpStart + jpText.length
        // Set "わがはい" ruby to "吾輩".
        val spanned =
            SpannableString(PREFIX + jpText + SUFFIX).apply {
                setSpan(RubySpan.Builder("わがはい").build(), jpStart, jpStart + 2, SPAN_FLAG) // 吾輩
            }
        val breakText: (Float) -> LineBreaker.Result = { heightConstraint ->
            LineBreaker.breakTextIntoLines(
                spanned,
                jpStart,
                jpEnd,
                PAINT,
                heightConstraint,
                TextOrientation.MIXED,
            )
        }

        breakText(getVerticalAdvance(jpText)).also {
            // ----
            // 吾
            // 輩
            // は
            // 猫
            // で
            // あ
            // る
            // 。
            // ----
            assertThat(it.lineCount).isEqualTo(1)
            assertThat(it.width).isEqualTo(1.5f * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(1 * EM) // 0.5em for right side, 0.5em for ruby
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 8)
        }

        breakText(getVerticalAdvance(jpText) * 0.1f).also {
            // ----
            // 。るあで猫は吾
            // 　　　　　　輩
            // ----
            assertThat(it.lineCount).isEqualTo(7)
            assertThat(it.width).isEqualTo(7 * 1.5f * EM)
            assertThat(it.lineLeftSide).isEqualTo(-HALF_EM)
            assertThat(it.lineRightSide).isEqualTo(1 * EM) // 0.5em for right side, 0.5em for ruby
            assertThat(it.getLineStart(0)).isEqualTo(jpStart)
            assertThat(it.getLineEnd(0)).isEqualTo(jpStart + 2)
            assertThat(it.getLineStart(1)).isEqualTo(jpStart + 2)
            assertThat(it.getLineEnd(1)).isEqualTo(jpStart + 3)
            assertThat(it.getLineStart(2)).isEqualTo(jpStart + 3)
            assertThat(it.getLineEnd(2)).isEqualTo(jpStart + 4)
            assertThat(it.getLineStart(3)).isEqualTo(jpStart + 4)
            assertThat(it.getLineEnd(3)).isEqualTo(jpStart + 5)
            assertThat(it.getLineStart(4)).isEqualTo(jpStart + 5)
            assertThat(it.getLineEnd(4)).isEqualTo(jpStart + 6)
            assertThat(it.getLineStart(5)).isEqualTo(jpStart + 6)
            assertThat(it.getLineEnd(5)).isEqualTo(jpStart + 7)
            assertThat(it.getLineStart(6)).isEqualTo(jpStart + 7)
            assertThat(it.getLineEnd(6)).isEqualTo(jpStart + 8)
        }
    }
}
