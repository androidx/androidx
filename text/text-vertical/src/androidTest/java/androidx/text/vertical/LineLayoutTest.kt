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
import android.graphics.Paint.FontMetricsInt
import android.text.SpannableString
import android.text.TextPaint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val SPAN_FLAG = SpannableString.SPAN_INCLUSIVE_EXCLUSIVE

@RunWith(JUnit4::class)
class LineLayoutRunTest {
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

    @Test
    fun `LineLayout plain text with Mixed`() {
        createLineLayout(TEXT, LATIN_START, JAPANESE_END, PAINT, TextOrientation.MIXED).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height)
                .isEqualTo(getHorizontalAdvance(LATIN_TEXT) + getVerticalAdvance(JAPANESE_TEXT))
            assertThat(leftSide).isEqualTo(-HALF_EM)
            assertThat(rightSide).isEqualTo(HALF_EM)
            assertThat(runs.size).isEqualTo(2)
            assertThat(runs[0].start).isEqualTo(LATIN_START)
            assertThat(runs[0].end).isEqualTo(LATIN_END)
            assertThat(runs[0]).isInstanceOf(RotateLayoutRun::class.java)
            assertThat(runs[1].start).isEqualTo(JAPANESE_START)
            assertThat(runs[1].end).isEqualTo(JAPANESE_END)
            assertThat(runs[1]).isInstanceOf(UprightLayoutRun::class.java)
        }
    }

    @Test
    fun `LineLayout plain text with Upright`() {
        createLineLayout(TEXT, LATIN_START, JAPANESE_END, PAINT, TextOrientation.UPRIGHT).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height)
                .isEqualTo(getVerticalAdvance(LATIN_TEXT) + getVerticalAdvance(JAPANESE_TEXT))
            assertThat(leftSide).isEqualTo(-HALF_EM)
            assertThat(rightSide).isEqualTo(HALF_EM)
            assertThat(runs.size).isEqualTo(1)
            assertThat(runs[0].start).isEqualTo(LATIN_START)
            assertThat(runs[0].end).isEqualTo(JAPANESE_END)
            assertThat(runs[0]).isInstanceOf(UprightLayoutRun::class.java)
        }
    }

    @Test
    fun `LineLayout plain text with Sideways`() {
        createLineLayout(TEXT, LATIN_START, JAPANESE_END, PAINT, TextOrientation.SIDEWAYS).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height)
                .isEqualTo(getHorizontalAdvance(LATIN_TEXT) + getHorizontalAdvance(JAPANESE_TEXT))
            assertThat(leftSide).isEqualTo(-HALF_EM)
            assertThat(rightSide).isEqualTo(HALF_EM)
            assertThat(runs.size).isEqualTo(1)
            assertThat(runs[0].start).isEqualTo(LATIN_START)
            assertThat(runs[0].end).isEqualTo(JAPANESE_END)
            assertThat(runs[0]).isInstanceOf(RotateLayoutRun::class.java)
        }
    }

    @Test
    fun `LineLayout span override text with Sideways`() {
        val spanned =
            SpannableString(TEXT).apply {
                setSpan(TextOrientationSpan.Sideways(), JAPANESE_START, JAPANESE_END, SPAN_FLAG)
            }
        createLineLayout(spanned, LATIN_START, JAPANESE_END, PAINT, TextOrientation.MIXED).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            assertThat(width).isEqualTo(ONE_EM) // width is 1em.
            assertThat(height)
                .isEqualTo(getHorizontalAdvance(LATIN_TEXT) + getHorizontalAdvance(JAPANESE_TEXT))
            assertThat(leftSide).isEqualTo(-HALF_EM)
            assertThat(rightSide).isEqualTo(HALF_EM)
            assertThat(runs.size).isEqualTo(1)
            assertThat(runs[0].start).isEqualTo(LATIN_START)
            assertThat(runs[0].end).isEqualTo(JAPANESE_END)
            assertThat(runs[0]).isInstanceOf(RotateLayoutRun::class.java)
        }
    }

    @Test
    fun `LineLayout span override text with TateChuYoko`() {
        val spanned =
            SpannableString(TEXT).apply {
                setSpan(TextOrientationSpan.TextCombineUpright(), LATIN_START, LATIN_END, SPAN_FLAG)
            }
        createLineLayout(spanned, LATIN_START, JAPANESE_END, PAINT, TextOrientation.MIXED).run {
            assertThat(start).isEqualTo(LATIN_START)
            assertThat(end).isEqualTo(JAPANESE_END)
            // Overall line width is extended to 1.1em because of the long TateChuYoko span.
            assertThat(width).isEqualTo(ONE_EM * 1.1f) // width is 1em.
            assertThat(height)
                .isEqualTo(getHorizontalLineHeight(LATIN_TEXT) + getVerticalAdvance(JAPANESE_TEXT))

            // Overall line sides are extended to 1.1em because of the long TateChuYoko span.
            assertThat(leftSide).isEqualTo(-HALF_EM * 1.1f)
            assertThat(rightSide).isEqualTo(HALF_EM * 1.1f)
            assertThat(runs.size).isEqualTo(2)
            assertThat(runs[0].start).isEqualTo(LATIN_START)
            assertThat(runs[0].end).isEqualTo(LATIN_END)
            assertThat(runs[0]).isInstanceOf(TateChuYokoLayoutRun::class.java)
            assertThat(runs[1].start).isEqualTo(JAPANESE_START)
            assertThat(runs[1].end).isEqualTo(JAPANESE_END)
            assertThat(runs[1]).isInstanceOf(UprightLayoutRun::class.java)
        }
    }
}
