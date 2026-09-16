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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class HorizontalEmphasisSpanLayoutTest {
    private lateinit var paint: TextPaint
    private val text = SpannedString("Hello")
    private val emphasisMark = "•"

    @Before
    fun setup() {
        paint = TextPaint().apply { textSize = 20f }
    }

    @Test
    fun getSpanWidth_basicCalculation() {
        val layout = HorizontalEmphasisSpanLayout(text, 0, text.length, emphasisMark, paint, 1.0f)
        val expectedWidth = ceil(paint.measureText(text, 0, text.length)).toInt()
        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun getSpanWidth_withStyledText() {
        val spannable = SpannableString(text)
        spannable.setSpan(RelativeSizeSpan(2.0f), 0, text.length, 0)

        val layout =
            HorizontalEmphasisSpanLayout(spannable, 0, text.length, emphasisMark, paint, 1.0f)
        val expectedWidth =
            ceil(Layout.getDesiredWidth(spannable, 0, spannable.length, paint)).toInt()

        assertThat(layout.spanWidth).isEqualTo(expectedWidth)
    }

    @Test
    fun fillFontMetrics_metricsExpansion() {
        val layout = HorizontalEmphasisSpanLayout(text, 0, text.length, emphasisMark, paint, 1.0f)
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, paint, Integer.MAX_VALUE).build()
        val emLayout =
            StaticLayout.Builder.obtain(
                    emphasisMark,
                    0,
                    emphasisMark.length,
                    paint,
                    Integer.MAX_VALUE,
                )
                .build()

        val bodyAscent = bodyLayout.getLineAscent(0)
        val bodyDescent = bodyLayout.getLineDescent(0)
        val emLineHeight = emLayout.getLineDescent(0) - emLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)
        assertThat(fm.ascent).isEqualTo(bodyAscent - emLineHeight)
        assertThat(fm.descent).isEqualTo(bodyDescent)
        assertThat(fm.top).isEqualTo(bodyAscent - emLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyDescent)
    }

    // A relative size of 1.0f cannot show a scaling defect, because scaled and unscaled
    // measurements are equal. These two tests use 0.5f.

    @Test
    fun constructor_doesNotChangeTheCallerTextSize() {
        val callerPaint = TextPaint().apply { textSize = 100f }

        HorizontalEmphasisSpanLayout(text, 0, text.length, emphasisMark, callerPaint, 0.5f)

        assertThat(callerPaint.textSize).isEqualTo(100f)
    }

    @Test
    fun fillFontMetrics_reservesSpaceForTheScaledMark() {
        val relSize = 0.5f
        val basePaint = TextPaint().apply { textSize = 100f }
        val layout =
            HorizontalEmphasisSpanLayout(
                text,
                0,
                text.length,
                emphasisMark,
                TextPaint(basePaint),
                relSize,
            )
        val bodyLayout =
            StaticLayout.Builder.obtain(text, 0, text.length, basePaint, Integer.MAX_VALUE).build()
        val scaledPaint = TextPaint(basePaint).apply { textSize = basePaint.textSize * relSize }
        val markLayout =
            StaticLayout.Builder.obtain(
                    emphasisMark,
                    0,
                    emphasisMark.length,
                    scaledPaint,
                    Integer.MAX_VALUE,
                )
                .build()
        val markLineHeight = markLayout.getLineDescent(0) - markLayout.getLineAscent(0)

        val fm = Paint.FontMetricsInt()
        layout.fillFontMetrics(fm)

        assertThat(fm.ascent).isEqualTo(bodyLayout.getLineAscent(0) - markLineHeight)
        assertThat(fm.descent).isEqualTo(bodyLayout.getLineDescent(0))
        assertThat(fm.top).isEqualTo(bodyLayout.getLineAscent(0) - markLineHeight)
        assertThat(fm.bottom).isEqualTo(bodyLayout.getLineDescent(0))
    }
}
