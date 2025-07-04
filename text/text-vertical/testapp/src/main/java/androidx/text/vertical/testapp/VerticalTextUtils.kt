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

package androidx.text.vertical.testapp

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.text.vertical.EmphasisSpan
import androidx.text.vertical.EmphasisStyle
import androidx.text.vertical.FontShearSpan
import androidx.text.vertical.FontShearSpan.Companion.DEFAULT_FONT_SHEAR
import androidx.text.vertical.RubySpan
import androidx.text.vertical.TextOrientationSpan

const val SPAN_FLAG = Spanned.SPAN_INCLUSIVE_EXCLUSIVE

class VerticalTextBuilder {
    @Composable
    fun Sideways(text: CharSequence) = withSpan(TextOrientationSpan.Sideways(), { this.text(text) })

    @Composable
    fun Upright(text: CharSequence) = withSpan(TextOrientationSpan.Upright(), { this.text(text) })

    @Composable
    fun TateChuYoko(text: CharSequence) =
        withSpan(TextOrientationSpan.TextCombineUpright(), { this.text(text) })

    @Composable
    fun <R : Any> ruby(ruby: CharSequence, block: @Composable VerticalTextBuilder.() -> R): R =
        withSpan(RubySpan.Builder(ruby).build(), block)

    fun text(text: CharSequence, rubyMap: Map<String, String> = emptyMap()) {
        val textStartOffset = result.length
        result.append(text)

        rubyMap.forEach { key, ruby ->
            var searchOffset = textStartOffset
            var found = result.indexOf(key, searchOffset)
            while (found != -1) {
                result.setSpan(RubySpan.Builder(ruby).build(), found, found + key.length, SPAN_FLAG)
                searchOffset = found + key.length
                found = result.indexOf(key, searchOffset)
            }
        }
    }

    @Composable
    private fun <R : Any> withSpan(span: Any, block: @Composable VerticalTextBuilder.() -> R): R {
        val index = result.length
        val r = block(this)
        result.setSpan(span, index, result.length, SPAN_FLAG)
        return r
    }

    private class TextStyleSpan(
        private val fontSize: TextUnit = TextUnit.Unspecified,
        private val textColor: Color = Color.Unspecified,
        private val backgroundColor: Color = Color.Unspecified,
        private val density: Density,
    ) : MetricAffectingSpan() {

        override fun updateMeasureState(textPaint: TextPaint) {
            if (fontSize.isSpecified) {
                if (fontSize.isSp) {
                    textPaint.textSize = fontSize.value * density.fontScale * density.density
                } else {
                    textPaint.textSize *= fontSize.value
                }
            }
            if (textColor.isSpecified) {
                textPaint.color = textColor.toArgb()
            }
            if (backgroundColor.isSpecified) {
                textPaint.bgColor = backgroundColor.toArgb()
            }
        }

        override fun updateDrawState(tp: TextPaint) = updateMeasureState(tp)
    }

    @Composable
    fun <R : Any> withStyle(
        fontSize: TextUnit = TextUnit.Unspecified,
        textColor: Color = Color.Unspecified,
        backgroundColor: Color = Color.Unspecified,
        block: @Composable VerticalTextBuilder.() -> R,
    ): R =
        withSpan(TextStyleSpan(fontSize, textColor, backgroundColor, LocalDensity.current), block)

    @Composable
    fun <R : Any> withFontShear(
        fontShear: Float = DEFAULT_FONT_SHEAR,
        block: @Composable VerticalTextBuilder.() -> R,
    ): R = withSpan(FontShearSpan(fontShear), block)

    @Composable
    fun <R : Any> withEmphasis(
        style: Int = EmphasisStyle.DOT,
        filled: Boolean = true,
        scale: Float = 0.5f,
        block: @Composable VerticalTextBuilder.() -> R,
    ): R = withSpan(EmphasisSpan(style, filled, scale), block)

    var result = SpannableStringBuilder()
}

@Composable
fun buildVerticalText(builder: @Composable VerticalTextBuilder.() -> Unit): SpannableStringBuilder =
    VerticalTextBuilder().apply { this.builder() }.result
