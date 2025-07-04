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
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class EmphasisTest {
    private val ONE_EM = 10f // make 1em = 10px
    private val PAINT = TextPaint().apply { textSize = ONE_EM }

    private class StyleTextBuilder(val result: SpannableStringBuilder = SpannableStringBuilder()) {
        fun <R : Any> withSpan(span: Any, block: StyleTextBuilder.() -> R): R {
            val index = result.length
            val r = block(this)
            result.setSpan(span, index, result.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            return r
        }

        fun <R : Any> withEmphasis(
            style: Int = EmphasisSpan.DEFAULT_EMPHASIS_STYLE,
            filled: Boolean = EmphasisSpan.DEFAULT_EMPHASIS_FILL,
            scale: Float = EmphasisSpan.DEFAULT_SCALE,
            block: StyleTextBuilder.() -> R,
        ) = withSpan(EmphasisSpan(style, filled, scale), block)

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
    fun `EmphasisTest Upright Default Case`() {
        val text = StyleTextBuilder().apply { withEmphasis { text("あいうえお") } }.result
        val originalTextSize = PAINT.textSize
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "あいうえお") {
                        // pass
                    } else if (substr == "\u2022") { // The default filled DOT is mapped to U+2022.
                        // The default scale is 0.5.
                        assertThat(paint.textSize).isEqualTo(originalTextSize / 2)
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun `EmphasisTest Upright Customized Case`() {
        val text =
            StyleTextBuilder()
                .apply {
                    withEmphasis(style = EmphasisStyle.TRIANGLE, filled = false, scale = 0.7f) {
                        text("あいうえお")
                    }
                }
                .result
        val originalTextSize = PAINT.textSize
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "あいうえお") {
                        // pass
                    } else if (substr == "\u25B3") { // Unfilled triangle is mapped to U+25B3
                        assertThat(paint.textSize).isEqualTo(originalTextSize * 0.7f)
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun `EmphasisTest Upright Skipping Case`() {
        val text = StyleTextBuilder().apply { withEmphasis { text("あいうえお。") } }.result
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "あいうえお。") {
                        // pass
                    } else if (substr == "\u2022") {
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            // U+3002(。) is a punctuation that should not draw emphasis.
            assertThat(emphasisCallCount).isEqualTo(5)
        }
    }

    @Test
    fun `EmphasisTest Upright SurrogatePair`() {
        val surrogatePairText = "\uD840\uDC0B\uD83C\uDF4B"
        // The text contains two surrogate pair letters: U+2000B and U+1F34B
        val text = StyleTextBuilder().apply { withEmphasis { text(surrogatePairText) } }.result
        UprightLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == surrogatePairText) {
                        // pass
                    } else if (substr == "\u2022") {
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            // Single emphasis should be drawn for surrogate pairs.
            assertThat(emphasisCallCount).isEqualTo(2)
        }
    }

    @Test
    fun `EmphasisTest TateChuYoko Default Case`() {
        val text = StyleTextBuilder().apply { withEmphasis { text("12") } }.result
        val originalTextSize = PAINT.textSize
        TateChuYokoLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "12") {
                        // pass
                    } else if (substr == "\u2022") { // The default filled DOT is mapped to U+2022.
                        // The default scale is 0.5.
                        assertThat(paint.textSize).isEqualTo(originalTextSize / 2)
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            // single dot is drawn for single TateChuYoko block
            assertThat(emphasisCallCount).isEqualTo(1)
        }
    }

    @Test
    fun `EmphasisTest TateChuYoko Customized Case`() {
        val text =
            StyleTextBuilder()
                .apply {
                    withEmphasis(style = EmphasisStyle.TRIANGLE, filled = false, scale = 0.7f) {
                        text("12")
                    }
                }
                .result
        val originalTextSize = PAINT.textSize
        TateChuYokoLayoutRun(text, 0, text.length, PAINT).also {
            var emphasisCallCount = 0
            it.draw(
                MockCanvas { text, start, end, paint ->
                    val substr = text.substring(start, end)
                    if (substr == "12") {
                        // pass
                    } else if (substr == "\u25B3") { // Unfilled triangle is mapped to U+25B3
                        assertThat(paint.textSize).isEqualTo(originalTextSize * 0.7f)
                        emphasisCallCount++
                    } else {
                        throw RuntimeException("Unexpected string: $substr arrived")
                    }
                },
                0f,
                0f,
                PAINT,
            )
            assertThat(emphasisCallCount).isEqualTo(1)
        }
    }
}
