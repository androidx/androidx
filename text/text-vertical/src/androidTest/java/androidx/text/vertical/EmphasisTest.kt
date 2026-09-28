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
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class EmphasisTest {
    private val ONE_EM = 10f // make 1em = 10px
    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    /**
     * Checks the base-text draws of an [UprightLayoutRun]. API 36 and higher draw the base text in
     * one `drawText` call. Lower API levels draw one cluster in each call. Each cluster in these
     * tests is one code point.
     */
    private fun assertBaseTextDraws(draws: List<String>, expected: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            assertThat(draws).containsExactly(expected)
        } else {
            val clusters = expected.codePoints().toArray().map { String(Character.toChars(it)) }
            assertThat(draws).containsExactlyElementsIn(clusters).inOrder()
        }
    }

    private class StyleTextBuilder(val result: SpannableStringBuilder = SpannableStringBuilder()) {
        fun <R : Any> withSpan(span: Any, block: StyleTextBuilder.() -> R): R {
            val index = result.length
            val r = block(this)
            result.setSpan(span, index, result.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            return r
        }

        fun <R : Any> withEmphasis(
            style: EmphasisStyle = EmphasisSpan.DEFAULT_EMPHASIS_STYLE,
            filled: Boolean = EmphasisSpan.DEFAULT_EMPHASIS_FILL,
            scale: Float = EmphasisSpan.DEFAULT_SCALE,
            block: StyleTextBuilder.() -> R,
        ) = withSpan(EmphasisSpan(style, filled, scale = scale), block)

        fun text(text: CharSequence) {
            result.append(text)
        }
    }

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

        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            super.drawText(text, x, y, paint)
            drawTextCallback(text, 0, text.length, paint)
        }
    }

    @Test
    fun emphasis_UprightDefaultCase() {
        val text = StyleTextBuilder().apply { withEmphasis { text("あいうえお") } }.result
        val originalTextSize = PAINT.textSize
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "\u2022") { // The default filled DOT is mapped to U+2022.
                        // The default scale is 0.5.
                        assertThat(paint.textSize).isEqualTo(originalTextSize / 2)
                        emphasisCallCount++
                    } else {
                        assertThat(paint.textSize).isEqualTo(originalTextSize)
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertBaseTextDraws(baseDraws, "あいうえお")
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun emphasis_UprightCustomizedCase() {
        val text =
            StyleTextBuilder()
                .apply {
                    withEmphasis(style = EmphasisStyle.Triangle, filled = false, scale = 0.7f) {
                        text("あいうえお")
                    }
                }
                .result
        val originalTextSize = PAINT.textSize
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "\u25B3") { // Unfilled triangle is mapped to U+25B3
                        assertThat(paint.textSize).isEqualTo(originalTextSize * 0.7f)
                        emphasisCallCount++
                    } else {
                        assertThat(paint.textSize).isEqualTo(originalTextSize)
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertBaseTextDraws(baseDraws, "あいうえお")
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun emphasis_UprightSkippingCase() {
        val text = StyleTextBuilder().apply { withEmphasis { text("あいうえお。") } }.result
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, _ ->
                    val substr = text.substring(start, end)
                    if (substr == "\u2022") {
                        emphasisCallCount++
                    } else {
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertBaseTextDraws(baseDraws, "あいうえお。")
            // U+3002(。) is a punctuation that should not draw emphasis.
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun emphasis_UprightSurrogatePair() {
        val surrogatePairText = "\uD840\uDC0B\uD83C\uDF4B"
        // The text contains two surrogate pair letters: U+2000B and U+1F34B
        val text = StyleTextBuilder().apply { withEmphasis { text(surrogatePairText) } }.result
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, _ ->
                    val substr = text.substring(start, end)
                    if (substr == "\u2022") {
                        emphasisCallCount++
                    } else {
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertBaseTextDraws(baseDraws, surrogatePairText)
            // Single emphasis should be drawn for surrogate pairs.
            assertThat(emphasisCallCount).isEqualTo(2)
        }
    }

    @Test
    fun emphasis_TateChuYokoDefaultCase() {
        val text = StyleTextBuilder().apply { withEmphasis { text("12") } }.result
        val originalTextSize = PAINT.textSize
        TateChuYokoLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "\u2022") { // The default filled DOT is mapped to U+2022.
                        // The default scale is 0.5.
                        assertThat(paint.textSize).isEqualTo(originalTextSize / 2)
                        emphasisCallCount++
                    } else {
                        assertThat(paint.textSize).isEqualTo(originalTextSize)
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            // `TateChuYokoLayoutRun` draws the base text in one `drawText` call on all API levels.
            assertThat(baseDraws).containsExactly("12")
            // single dot is drawn for single TateChuYoko block
            assertThat(emphasisCallCount).isEqualTo(1)
        }
    }

    @Test
    fun emphasis_TateChuYokoCustomizedCase() {
        val text =
            StyleTextBuilder()
                .apply {
                    withEmphasis(style = EmphasisStyle.Triangle, filled = false, scale = 0.7f) {
                        text("12")
                    }
                }
                .result
        val originalTextSize = PAINT.textSize
        TateChuYokoLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            val baseDraws = mutableListOf<String>()
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "\u25B3") { // Unfilled triangle is mapped to U+25B3
                        assertThat(paint.textSize).isEqualTo(originalTextSize * 0.7f)
                        emphasisCallCount++
                    } else {
                        assertThat(paint.textSize).isEqualTo(originalTextSize)
                        baseDraws += substr
                    }
                },
                0f,
                0f,
                PAINT,
            )
            // `TateChuYokoLayoutRun` draws the base text in one `drawText` call on all API levels.
            assertThat(baseDraws).containsExactly("12")
            assertThat(emphasisCallCount).isEqualTo(1)
        }
    }
}
