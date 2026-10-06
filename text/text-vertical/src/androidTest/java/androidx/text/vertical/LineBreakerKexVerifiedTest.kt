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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.text.vertical.TextOrientationSpan.CombineUpright
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests branches in `LineBreaker.kt` ([LineBreaker], [LineBreaker.Result], `WordBreaker`, and
 * `Context`) derived from Kex symbolic execution paths.
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class LineBreakerKexVerifiedTest {

    private val oneEm = 10f
    private val halfEm = oneEm * 0.5f
    private val paint =
        TextPaint().apply {
            textSize = oneEm
            typeface = Typeface.MONOSPACE
        }

    private fun getVerticalAdvance(text: String): Float = paint.measureTextVertical(text)

    private fun getHorizontalAdvance(text: String): Float = paint.measureText(text)

    private class RecordingCanvas : Canvas() {
        val drawnRanges = mutableListOf<Pair<Int, Int>>()

        override fun drawText(
            text: CharSequence,
            start: Int,
            end: Int,
            x: Float,
            y: Float,
            paint: Paint,
        ) {
            super.drawText(text, start, end, x, y, paint)
            drawnRanges.add(start to end)
        }
    }

    @Test
    fun breakTextIntoLines_emptyRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            LineBreaker.breakTextIntoLines("abc", 1, 1, paint, 100f, TextOrientation.Mixed)
        }
    }

    @Test
    fun breakTextIntoLines_multipleParagraphs_breaksAtNewlineAndDrawsEachLine() {
        val text = "a\nb"
        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                1000f,
                TextOrientation.Sideways,
            )

        assertThat(result.lineCount).isEqualTo(2)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(2)
        assertThat(result.getLineStart(1)).isEqualTo(2)
        assertThat(result.getLineEnd(1)).isEqualTo(3)
        assertThat(result.lineLeftSide).isEqualTo(-halfEm)
        assertThat(result.lineRightSide).isEqualTo(halfEm)
        assertThat(result.width).isEqualTo(2 * oneEm)

        val canvas = RecordingCanvas()
        result.draw(canvas, 100f, 20f, paint)
        assertThat(canvas.drawnRanges).containsExactly(0 to 2, 2 to 3).inOrder()
    }

    @Test
    fun processRun_firstWordOverflowsAfterExistingRun_breaksLineAndFitsWordOnNewLine() {
        // "あHello": "あ" is Upright, "Hello" is Rotate (a single word).
        val text = "あHello"
        val uprightHeight = getVerticalAdvance("あ")
        val rotateWordHeight = getHorizontalAdvance("Hello")
        // Constraint fits "あ" on line 0 and fits "Hello" on line 1 by itself, but cannot fit both
        // on line 0.
        val constraint = maxOf(uprightHeight, rotateWordHeight) + 1f

        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                constraint,
                TextOrientation.Mixed,
            )

        assertThat(result.lineCount).isEqualTo(2)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(1)
        assertThat(result.getLineStart(1)).isEqualTo(1)
        assertThat(result.getLineEnd(1)).isEqualTo(text.length)
    }

    @Test
    fun processRun_firstWordOverflowsAfterExistingRunAndExceedsConstraint_desperatelyBreaksGraphemes() {
        // "あabcd": "あ" is Upright, "abcd" is Rotate (a single word).
        val text = "あabcd"
        val uprightHeight = getVerticalAdvance("あ")
        val twoCharRotateHeight = getHorizontalAdvance("ab")
        // Constraint fits "あ" on line 0 and fits 2 rotated Latin chars per line, so "abcd" breaks
        // after "あ" and then desperately breaks into "ab" and "cd".
        val constraint = maxOf(uprightHeight, twoCharRotateHeight) + 0.1f

        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                constraint,
                TextOrientation.Mixed,
            )

        assertThat(result.lineCount).isEqualTo(3)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(1)
        assertThat(result.getLineStart(1)).isEqualTo(1)
        assertThat(result.getLineEnd(1)).isEqualTo(3)
        assertThat(result.getLineStart(2)).isEqualTo(3)
        assertThat(result.getLineEnd(2)).isEqualTo(5)
    }

    @Test
    fun processRun_surrogatePairGrapheme_neverSplitsInsideSurrogatePair() {
        // "𠮟あ" has a 2-char surrogate pair [0, 2) followed by "あ" [2, 3).
        val text = "𠮟あ"
        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                0f,
                TextOrientation.Upright,
            )

        assertThat(result.lineCount).isEqualTo(2)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(2)
        assertThat(result.getLineStart(1)).isEqualTo(2)
        assertThat(result.getLineEnd(1)).isEqualTo(3)
    }

    @Test
    fun processNonBreakableLayout_overflowsCurrentLineButFitsNewLine_keepsNewLineOpenForNextRun() {
        // "あい12う": "あい" is Upright [0, 2), "12" is TateChuYoko [2, 4), "う" is Upright [4, 5).
        val text =
            SpannableString("あい12う").apply {
                setSpan(CombineUpright(), 2, 4, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val hAi = getVerticalAdvance("あい")
        val fm = Paint.FontMetricsInt()
        paint.getFontMetricsIntCompat("12", 0, 2, 0, 2, false, fm)
        val h12 = (fm.descent - fm.ascent).toFloat()
        val hU = getVerticalAdvance("う")
        // Constraint cannot fit "あい" + "12" on line 0, but can fit "12" + "う" together on line 1.
        val constraint = maxOf(hAi, h12 + hU) + 0.1f

        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                constraint,
                TextOrientation.Mixed,
            )

        assertThat(result.lineCount).isEqualTo(2)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(2)
        assertThat(result.getLineStart(1)).isEqualTo(2)
        assertThat(result.getLineEnd(1)).isEqualTo(5)
    }

    @Test
    fun processNonBreakableLayout_rubyOverflowsCurrentLineAndExceedsConstraint_breaksBeforeAndAfterRuby() {
        // "あ漢字い" where "漢字" [1, 3) has a RubySpan.
        val rubySpan = RubySpan("かんじ")
        val text =
            SpannableString("あ漢字い").apply {
                setSpan(rubySpan, 1, 3, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            }
        val hA = getVerticalAdvance("あ")
        val hI = getVerticalAdvance("い")
        // Constraint fits a single upright character ("あ" or "い") on a line, while the 2-char
        // RubyLayoutRun for "漢字" exceeds the constraint.
        val constraint = maxOf(hA, hI)

        val result =
            LineBreaker.breakTextIntoLines(
                text,
                0,
                text.length,
                paint,
                constraint,
                TextOrientation.Upright,
            )

        assertThat(result.lineCount).isEqualTo(3)
        assertThat(result.getLineStart(0)).isEqualTo(0)
        assertThat(result.getLineEnd(0)).isEqualTo(1)
        assertThat(result.getLineStart(1)).isEqualTo(1)
        assertThat(result.getLineEnd(1)).isEqualTo(3)
        assertThat(result.getLineStart(2)).isEqualTo(3)
        assertThat(result.getLineEnd(2)).isEqualTo(4)
    }
}
