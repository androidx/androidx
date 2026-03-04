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

import android.graphics.Paint
import android.text.Layout
import android.text.SpannableString
import android.text.SpannedString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.max
import org.junit.Before
import org.junit.Test

class HorizontalRubySpanLayoutTest {
    private lateinit var paint: TextPaint
    private val text = SpannedString("Hello")
    private val rubyText = "World"

    @Before
    fun setup() {
        paint = TextPaint().apply { textSize = 20f }
    }

    @Test
    fun getSpanWidth_basic_calculation() {
        val layout = HorizontalRubySpanLayout(text, 0, text.length, rubyText, paint, 1.0f)
        val bodyWidth = ceil(paint.measureText(text, 0, text.length)).toInt()
        val rubyWidth = ceil(paint.measureText(rubyText, 0, rubyText.length)).toInt()
        assertThat(layout.spanWidth).isEqualTo(max(bodyWidth, rubyWidth))
    }

    @Test
    fun getSpanWidth_with_styled_text() {
        val spannable = SpannableString(text)
        spannable.setSpan(RelativeSizeSpan(2.0f), 0, text.length, 0)

        val layout = HorizontalRubySpanLayout(spannable, 0, text.length, rubyText, paint, 1.0f)
        val expectedWidth =
            ceil(Layout.getDesiredWidth(spannable, 0, spannable.length, paint)).toInt()

        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun fillFontMetrics_metrics_expansion() {
        val layout = HorizontalRubySpanLayout(text, 0, text.length, rubyText, paint, 1.0f)
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Integer.MAX_VALUE).build()
        val rubyLayout =
            StaticLayout.Builder.obtain(rubyText, 0, rubyText.length, paint, Integer.MAX_VALUE)
                .build()

        val bodyAscent = bodyLayout.getLineAscent(0)
        val bodyDescent = bodyLayout.getLineDescent(0)
        val rubyLineHeight = rubyLayout.getLineDescent(0) - rubyLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyAscent - rubyLineHeight)
        assertThat(fm.descent).isEqualTo(bodyDescent)
        assertThat(fm.top).isEqualTo(bodyAscent - rubyLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyDescent)
    }
}
