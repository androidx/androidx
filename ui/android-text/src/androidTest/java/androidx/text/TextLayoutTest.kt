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

package androidx.text

import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
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
        sampleTypeface = Typeface.createFromAsset(
            instrumentation.context.assets, "sample_font.ttf")!!
    }

    @Test
    fun constructor_default_values() {
        val textLayout = TextLayout(charSequence = "", textPaint = TextPaint())
        val frameworkLayout = textLayout.layout

        assertThat(frameworkLayout.width, equalTo(0))
        assertThat(frameworkLayout.alignment, equalTo(Layout.Alignment.ALIGN_NORMAL))
        assertThat(frameworkLayout.getParagraphDirection(0), equalTo(Layout.DIR_LEFT_TO_RIGHT))
        assertThat(frameworkLayout.spacingMultiplier, equalTo(1.0f))
        assertThat(frameworkLayout.spacingAdd, equalTo(0.0f))
        // TODO(Migration/haoyuchang): Need public API to test includePadding, maxLines,
        // breakStrategy and hyphenFrequency.
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

        assertThat(frameworkLayout.width, equalTo(layoutWidth.toInt()))
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

        assertThat(frameworkLayout.width, lessThanOrEqualTo(layoutWidth.toInt()))
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
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2 * fontSize(line gap)
            assertThat(layout.getLineHeight(i), equalTo(textSize * 1.2f + lineSpacingExtra))
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
        assertThat(lastLine, greaterThanOrEqualTo(1))

        val actualHeight = layout.getLineHeight(lastLine)
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(actualHeight, equalTo(textSize * 1.2f))
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

        assertThat(layout.lineCount, equalTo(1))
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(layout.getLineHeight(0), equalTo(textSize * 1.2f))
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

        assertThat(layout.lineCount, equalTo(1))
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(layout.getLineHeight(0), equalTo(textSize * 1.2f))
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
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2 * fontSize(line gap)
            assertThat(layout.getLineHeight(i), equalTo(textSize * 1.2f * lineSpacingMultiplier))
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
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(layout.getLineHeight(lastLine), equalTo(textSize * 1.2f))
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

        assertThat(layout.lineCount, equalTo(1))
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(layout.getLineHeight(0), equalTo(textSize * 1.2f))
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

        assertThat(layout.lineCount, equalTo(1))
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(layout.getLineHeight(0), equalTo(textSize * 1.2f))
    }
}