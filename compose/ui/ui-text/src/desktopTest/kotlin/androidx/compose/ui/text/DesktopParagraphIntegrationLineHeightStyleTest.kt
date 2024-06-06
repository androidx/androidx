/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.google.common.truth.FloatSubject
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.jetbrains.skia.FontMetrics
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


@RunWith(JUnit4::class)
class DesktopParagraphIntegrationLineHeightStyleTest {
    private val fontFamilyResolver = createFontFamilyResolver()
    private val fontFamilyMeasureFont =
        FontFamily(
            Font(
                "font/sample_font.ttf",
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )
    private val defaultDensity = Density(density = 1f)
    private val fontSize = 10.sp
    private val lineHeight = 20.sp
    private val fontSizeInPx = with(defaultDensity) { fontSize.toPx() }
    private val lineHeightInPx = with(defaultDensity) { lineHeight.toPx() }

    /* single line even */

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun singleLine_even_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun singleLine_even_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun singleLine_even_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_even_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    /* single line top */

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun singleLine_top_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun singleLine_top_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(defaultFontMetrics.descent)
        }
    }

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun singleLine_top_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_top_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    /* single line bottom */

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun singleLine_bottom_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun singleLine_bottom_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent - diff)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(defaultFontMetrics.descent)
        }
    }

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun singleLine_bottom_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_bottom_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    /* single line proportional */

    @Test
    fun singleLine_proportional_trim_None() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_LastLineBottom() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_FirstLineTop() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    @Test
    fun singleLine_proportional_trim_Both() {
        val paragraph = singleLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(defaultFontMetrics.lineHeight())
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)
        }
    }

    /* multi line even */

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun multiLine_even_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            for (line in 0 until lineCount) {
                assertThat(getLineHeight(line)).isWithinMetricToleranceOf(lineHeightInPx)
                assertThat(getLineAscent(line)).isWithinMetricToleranceOf(expectedAscent)
                assertThat(getLineDescent(line)).isWithinMetricToleranceOf(expectedDescent)
            }

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun multiLine_even_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun multiLine_even_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Center is not supported") // TODO: Support non-proportional alignment
    fun multiLine_even_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Center
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = (lineHeightInPx - defaultFontMetrics.lineHeight()) / 2
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    /* multi line top */

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun multiLine_top_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun multiLine_top_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun multiLine_top_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Top is not supported") // TODO: Support non-proportional alignment
    fun multiLine_top_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Top
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent
        val expectedDescent = defaultFontMetrics.descent + diff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    /* multi line bottom */

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun multiLine_bottom_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun multiLine_bottom_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun multiLine_bottom_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    @Ignore("Alignment.Bottom is not supported") // TODO: Support non-proportional alignment
    fun multiLine_bottom_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Bottom
        )

        val defaultFontMetrics = defaultFontMetrics()
        val diff = lineHeightInPx - defaultFontMetrics.lineHeight()
        val expectedAscent = defaultFontMetrics.ascent - diff
        val expectedDescent = defaultFontMetrics.descent

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - diff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    /* multi line proportional */

    @Test
    fun multiLine_proportional_trim_None() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.None,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_LastLineBottom() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.LastLineBottom,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_FirstLineTop() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.FirstLineTop,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    fun multiLine_proportional_trim_Both() {
        val paragraph = multiLineParagraph(
            lineHeightTrim = Trim.Both,
            lineHeightAlignment = Alignment.Proportional
        )

        val defaultFontMetrics = defaultFontMetrics()
        val descentDiff = proportionalDescentDiff(defaultFontMetrics)
        val ascentDiff = defaultFontMetrics.lineHeight() - descentDiff
        val expectedAscent = defaultFontMetrics.ascent - ascentDiff
        val expectedDescent = defaultFontMetrics.descent + descentDiff

        with(paragraph) {
            assertThat(getLineHeight(0)).isWithinMetricToleranceOf(lineHeightInPx - ascentDiff)
            assertThat(getLineAscent(0)).isWithinMetricToleranceOf(defaultFontMetrics.ascent)
            assertThat(getLineDescent(0)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(1)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineAscent(1)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(1)).isWithinMetricToleranceOf(expectedDescent)

            assertThat(getLineHeight(2)).isWithinMetricToleranceOf(lineHeightInPx - descentDiff)
            assertThat(getLineAscent(2)).isWithinMetricToleranceOf(expectedAscent)
            assertThat(getLineDescent(2)).isWithinMetricToleranceOf(defaultFontMetrics.descent)

            assertThat(getLineBaseline(1) - getLineBaseline(0)).isWithinMetricToleranceOf(lineHeightInPx)
            assertThat(getLineBaseline(2) - getLineBaseline(1)).isWithinMetricToleranceOf(lineHeightInPx)
        }
    }

    @Test
    fun lastLineEmptyTextHasSameLineHeightAsNonEmptyText() {
        assertEmptyLineMetrics("", "a")
        assertEmptyLineMetrics("\n", "a\na")
        assertEmptyLineMetrics("a\n", "a\na")
        assertEmptyLineMetrics("\na", "a\na")
        assertEmptyLineMetrics("\na\na", "a\na\na")
        assertEmptyLineMetrics("a\na\n", "a\na\na")
    }

    private fun assertEmptyLineMetrics(textWithEmptyLine: String, textWithoutEmptyLine: String) {
        val textStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                trim = Trim.None,
                alignment = Alignment.Proportional
            ),
        )

        val paragraphWithEmptyLastLine = simpleParagraph(
            text = textWithEmptyLine,
            style = textStyle
        ) as SkiaParagraph

        val otherParagraph = simpleParagraph(
            text = textWithoutEmptyLine,
            style = textStyle
        ) as SkiaParagraph

        with(paragraphWithEmptyLastLine) {
            for (line in 0 until lineCount) {
                assertThat(height).isWithinMetricToleranceOf(otherParagraph.height)
                assertThat(getLineTop(line)).isWithinMetricToleranceOf(otherParagraph.getLineTop(line))
                assertThat(getLineBottom(line)).isWithinMetricToleranceOf(otherParagraph.getLineBottom(line))
                assertThat(getLineHeight(line)).isWithinMetricToleranceOf(otherParagraph.getLineHeight(line))
                assertThat(getLineAscent(line)).isWithinMetricToleranceOf(otherParagraph.getLineAscent(line))
                assertThat(getLineDescent(line)).isWithinMetricToleranceOf(otherParagraph.getLineDescent(line))
                assertThat(getLineBaseline(line)).isWithinMetricToleranceOf(otherParagraph.getLineBaseline(line))
            }
        }
    }

    private fun singleLineParagraph(
        lineHeightTrim: Trim,
        lineHeightAlignment: Alignment,
        text: String = "AAA"
    ): SkiaParagraph {
        val textStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                trim = lineHeightTrim,
                alignment = lineHeightAlignment
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            style = textStyle,
            width = text.length * fontSizeInPx
        ) as SkiaParagraph

        assertThat(paragraph.lineCount).isEqualTo(1)

        return paragraph
    }

    private fun multiLineParagraph(
        lineHeightTrim: Trim,
        lineHeightAlignment: Alignment,
    ): SkiaParagraph {
        val lineCount = 3
        val word = "AAA"
        val text = "AAA".repeat(lineCount)

        val textStyle = TextStyle(
            lineHeightStyle = LineHeightStyle(
                trim = lineHeightTrim,
                alignment = lineHeightAlignment
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            style = textStyle,
            width = word.length * fontSizeInPx
        ) as SkiaParagraph

        assertThat(paragraph.lineCount).isEqualTo(lineCount)

        return paragraph
    }

    private fun simpleParagraph(
        text: String = "",
        style: TextStyle? = null,
        maxLines: Int = Int.MAX_VALUE,
        ellipsis: Boolean = false,
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        width: Float = Float.MAX_VALUE
    ): Paragraph {
        return Paragraph(
            text = text,
            spanStyles = spanStyles,
            style = TextStyle(
                fontFamily = fontFamilyMeasureFont,
                fontSize = fontSize,
                lineHeight = lineHeight,
            ).merge(style),
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = Constraints(maxWidth = width.ceilToInt()),
            density = defaultDensity,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    private fun defaultFontMetrics(): FontMetricsInt {
        val defaultFont = (simpleParagraph() as SkiaParagraph).defaultFont
        return FontMetricsInt(defaultFont.metrics)
    }

    private fun proportionalDescentDiff(fontMetrics: FontMetricsInt): Int {
        val ascent = abs(fontMetrics.ascent.toFloat())
        val ascentRatio = ascent / fontMetrics.lineHeight()
        return ceil(fontMetrics.lineHeight() * (1f - ascentRatio)).toInt()
    }
}

private data class FontMetricsInt(
    var ascent: Int,
    var descent: Int
) {
    constructor(fontMetrics: FontMetrics) : this(
        ascent = fontMetrics.ascent.roundToInt(),
        descent = fontMetrics.descent.roundToInt(),
    )

    fun lineHeight(): Int = descent - ascent
}

private const val metricsComparisonTolerance = 0.001f

fun FloatSubject.isWithinMetricToleranceOf(value: Float) =
    this.isWithin(metricsComparisonTolerance).of(value)

fun FloatSubject.isWithinMetricToleranceOf(value: Int) =
    this.isWithin(metricsComparisonTolerance).of(value.toFloat())
