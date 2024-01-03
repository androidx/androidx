/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.graphics.Typeface
import android.text.BoringLayout
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.text.android.style.BaselineShiftSpan
import androidx.compose.ui.text.android.style.LineHeightStyleSpan
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextLayoutTest {
    lateinit var sampleTypeface: Typeface

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        sampleTypeface = ResourcesCompat.getFont(instrumentation.context, R.font.sample_font)!!
    }

    @Test
    fun constructor_default_values() {
        val textLayout = TextLayout(charSequence = "", textPaint = TextPaint(), width = 0f)
        val frameworkLayout = textLayout.layout

        assertThat(frameworkLayout.width).isEqualTo(0)
        assertThat(frameworkLayout.alignment).isEqualTo(Layout.Alignment.ALIGN_NORMAL)
        assertThat(frameworkLayout.getParagraphDirection(0)).isEqualTo(Layout.DIR_LEFT_TO_RIGHT)
        assertThat(frameworkLayout.spacingMultiplier).isEqualTo(1.0f)
        assertThat(frameworkLayout.spacingAdd).isEqualTo(0.0f)
    }

    @Test
    fun specifiedWidth_equalsTo_widthInFramework() {
        val layoutWidth = 100.0f
        val textLayout = TextLayout(
            charSequence = "",
            width = layoutWidth,
            textPaint = TextPaint()
        )
        val frameworkLayout = textLayout.layout

        assertThat(frameworkLayout.width).isEqualTo(layoutWidth.toInt())
    }

    @Test
    fun maxIntrinsicWidth_lessThan_specifiedWidth() {
        val text = "aaaa"
        val textSize = 20.0f
        val layoutWidth = textSize * (text.length - 1)

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val textLayout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint
        )
        val frameworkLayout = textLayout.layout

        assertThat(frameworkLayout.width).isAtMost(layoutWidth.toInt())
    }

    @Test
    fun lineSpacingExtra_whenMultipleLines_returnsSameAsGiven() {
        val text = "abcdefgh"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length / 4
        val lineSpacingExtra = 1.0f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingExtra = lineSpacingExtra,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false
        )

        for (i in 0 until layout.lineCount - 1) {
            // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
            assertThat(layout.getLineHeight(i)).isEqualTo(textSize + lineSpacingExtra)
        }
    }

    @Test
    fun lineSpacingExtra_whenMultipleLines_hasNoEffectOnLastLine() {
        val text = "abcdefgh"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length / 4
        val lineSpacingExtra = 1.0f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingExtra = lineSpacingExtra,
            // IncludePadding is false so that we can expected the last line's height to be
            // descend - ascend
            includePadding = false
        )

        val lastLine = layout.lineCount - 1
        assertThat(lastLine).isAtLeast(1)

        val actualHeight = layout.getLineHeight(lastLine)
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(actualHeight).isEqualTo(textSize)
    }

    @Test
    fun lineSpacingExtra_whenOneLine_hasNoEffects() {
        val text = "abc"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length
        val lineSpacingExtra = 1.0f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingExtra = lineSpacingExtra,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false

        )

        assertThat(layout.lineCount).isEqualTo(1)
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(layout.getLineHeight(0)).isEqualTo(textSize)
    }

    @Test
    fun lineSpacingExtra_whenOneLine_withTextRTL_hasNoEffects() {
        val text = "\u05D0\u05D0\u05D0"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length
        val lineSpacingExtra = 1.0f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingExtra = lineSpacingExtra,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false

        )

        assertThat(layout.lineCount).isEqualTo(1)
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(layout.getLineHeight(0)).isEqualTo(textSize)
    }

    @Test
    fun lineSpacingMultiplier_whenMultipleLines_returnsSameAsGiven() {
        val text = "abcdefgh"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length / 4
        val lineSpacingMultiplier = 1.5f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingMultiplier = lineSpacingMultiplier,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false
        )

        for (i in 0 until layout.lineCount - 1) {
            // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
            assertThat(layout.getLineHeight(i)).isEqualTo(textSize * lineSpacingMultiplier)
        }
    }

    @Test
    fun lineSpacingMultiplier_whenMultipleLines_hasNoEffectOnLastLine() {
        val text = "abcdefgh"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length / 4
        val lineSpacingMultiplier = 1.5f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingMultiplier = lineSpacingMultiplier,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false
        )

        val lastLine = layout.lineCount - 1
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(layout.getLineHeight(lastLine)).isEqualTo(textSize)
    }

    @Test
    fun lineSpacingMultiplier_whenOneLine_hasNoEffect() {
        val text = "abc"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length
        val lineSpacingMultiplier = 1.5f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingMultiplier = lineSpacingMultiplier,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false
        )

        assertThat(layout.lineCount).isEqualTo(1)
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(layout.getLineHeight(0)).isEqualTo(textSize)
    }

    @Test
    fun lineSpacingMultiplier_whenOneLine_withTextRTL_hasNoEffect() {
        val text = "\u05D0\u05D0\u05D0"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length
        val lineSpacingMultiplier = 1.5f

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        val layout = TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            lineSpacingMultiplier = lineSpacingMultiplier,
            // IncludePadding is false so that we can expected the 1st line's height to be
            // descend - ascend
            includePadding = false
        )

        assertThat(layout.lineCount).isEqualTo(1)
        // In the sample_font.ttf, the height of the line should be 1em, i.e. fontSize
        assertThat(layout.getLineHeight(0)).isEqualTo(textSize)
    }

    @Test
    fun createsBoringLayout_for_boringText() {
        assertThat(
            TextLayout(
                charSequence = "a",
                width = Float.MAX_VALUE,
                textPaint = TextPaint()
            ).layout
        ).isInstanceOf(BoringLayout::class.java)
    }

    @Test
    fun createsStaticLayout_for_rtl_text() {
        assertThat(
            TextLayout(
                charSequence = "\u05D0",
                width = Float.MAX_VALUE,
                textPaint = TextPaint()
            ).layout
        ).isInstanceOf(StaticLayout::class.java)
    }

    @Test
    fun createsStaticLayout_if_line_break_is_needed() {
        val text = "ab"
        val textSize = 20.0f
        val layoutWidth = textSize * text.length

        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = textSize

        assertThat(
            TextLayout(
                charSequence = text,
                width = layoutWidth / 2f,
                textPaint = textPaint
            ).layout
        ).isInstanceOf(StaticLayout::class.java)
    }

    @Test
    fun createsStaticLayout_if_text_has_baselineshift_spans() {
        val text = SpannableString("a").apply {
            // 0.5f is a random value
            setSpan(BaselineShiftSpan(0.5f), 0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }

        assertThat(
            TextLayout(
                charSequence = text,
                width = Float.MAX_VALUE,
                textPaint = TextPaint()
            ).layout
        ).isInstanceOf(StaticLayout::class.java)
    }

    @Test
    fun small_lineheight_prevents_clip_single_line() {
        val fontSize = 120f
        val lineHeight = 60f

        val layout = TextLayoutWithSmallLineHeight(
            text = "aA",
            fontSize = fontSize,
            lineHeight = lineHeight
        )

        val defaultFontMetrics = createTextPaint(fontSize).fontMetricsInt
        val expectedPadding = ((fontSize - lineHeight) / 2).toInt()

        assertThat(layout.topPadding).isEqualTo(expectedPadding)
        assertThat(layout.bottomPadding).isEqualTo(expectedPadding)
        assertThat(layout.height).isEqualTo(fontSize.toInt())
        assertThat(layout.getLineTop(0)).isEqualTo(0)
        assertThat(layout.getLineBottom(0)).isEqualTo(layout.height)
        assertThat(layout.getLineBaseline(0)).isEqualTo(-defaultFontMetrics.ascent.toFloat())
        assertThat(layout.getLineForVertical(0)).isEqualTo(0)
        assertThat(layout.getLineForVertical(layout.height)).isEqualTo(0)
    }

    @Test
    fun small_lineheight_prevents_clip_multi_line() {
        val fontSize = 120f
        val lineHeight = 60f
        val layout = TextLayoutWithSmallLineHeight(
            text = "aA\naA\naA",
            fontSize = fontSize,
            lineHeight = lineHeight
        )

        val defaultFontMetrics = createTextPaint(fontSize).fontMetricsInt
        val expectedPadding = ((fontSize - lineHeight) / 2).toInt()

        assertThat(layout.topPadding).isEqualTo(expectedPadding)
        assertThat(layout.bottomPadding).isEqualTo(expectedPadding)
        assertThat(layout.height).isEqualTo((3 * lineHeight + 2 * expectedPadding).toInt())
        assertThat(layout.getLineTop(0)).isEqualTo(0)
        assertThat(layout.getLineBaseline(0)).isEqualTo(-defaultFontMetrics.ascent)
        assertThat(layout.getLineForVertical(0)).isEqualTo(0)
        assertThat(layout.getLineBottom(2)).isEqualTo(layout.height)
        assertThat(layout.getLineForVertical(layout.height)).isEqualTo(2)
    }

    private fun TextLayoutWithSmallLineHeight(
        text: CharSequence,
        fontSize: Float,
        lineHeight: Float
    ): TextLayout {
        val textPaint = createTextPaint(fontSize)
        val spannable = SpannableString(text)
        spannable.setSpan(
            LineHeightStyleSpan(
                lineHeight = lineHeight,
                startIndex = 0,
                endIndex = text.length,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f
            ), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return TextLayout(
            charSequence = spannable,
            textPaint = textPaint,
            includePadding = false,
            width = Float.MAX_VALUE
        )
    }

    private fun createTextPaint(fontSize: Float): TextPaint {
        return TextPaint().apply {
            this.typeface = sampleTypeface
            this.textSize = fontSize
        }
    }
}
