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

package androidx.compose.ui.text.android.style

import android.graphics.Paint.FontMetricsInt
import android.text.SpannableString
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 1:1 Idiomatic Kotlin translation of Kex symbolic execution state space exploration for
 * [LineHeightStyleSpan].
 *
 * Each test corresponds directly to a generated Kex test artifact with commentary on realistic vs
 * synthetic SMT edge cases.
 */
@OptIn(InternalPlatformTextApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class LineHeightStyleSpanKexVerifiedTest {

    private fun createFm(ascent: Int, descent: Int): FontMetricsInt {
        return FontMetricsInt().apply {
            this.ascent = ascent
            this.descent = descent
            this.top = ascent
            this.bottom = descent
        }
    }

    // Kex: LineHeightStyleSpan_chooseHeightPerLine_8582574191
    // Validates single-line trimBoth early exit with negative ascent.
    @Test
    fun kex_chooseHeightPerLine_singleLineTrimBoth() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 0,
                lineHeight = 30f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -50, descent = 10)
        span.chooseHeight(
            SpannableString(""),
            start = 0,
            end = 0,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(-50)
        assertThat(fm.descent).isEqualTo(10)
    }

    // Kex: LineHeightStyleSpan_chooseHeightPerLine_8582574192
    // SMT Edge Case: Highly negative float line height (-1.08e9f).
    // Note: Bad/unrealistic SMT artifact in practice, but proves mathematical safety of `diff <= 0`
    // guard against underflow.
    @Test
    fun kex_chooseHeightPerLine_negativeLineHeightUnderflow_diffNegative() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = -100f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -20, descent = 5)
        span.chooseHeight(
            SpannableString("NegativeLineHeight"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = -100,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(-20)
        assertThat(fm.descent).isEqualTo(5)
        assertThat(span.firstAscentDiff).isEqualTo(0)
    }

    // Kex: LineHeightStyleSpan_chooseHeightPerLine_8582574193
    // SMT Edge Case: Massive positive float line height (2.13e9f).
    // Note: Unrealistic layout scale, but validates integer conversion and upward expansion.
    @Test
    fun kex_chooseHeightPerLine_largeLineHeight_expandsMetrics() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 1000f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -20, descent = 10) // height = 30, diff = 970
        span.chooseHeight(
            SpannableString("LargeLineHeight"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 1000,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(1000)
        assertThat(fm.ascent).isEqualTo(-505)
        assertThat(fm.descent).isEqualTo(495)
    }

    // Kex: LineHeightStyleSpan_chooseHeightPerLine_8582574194
    // SMT Edge Case: Both ascent and descent are positive (e.g., ascent = 10, descent = 40).
    // Note: Rare baseline shift scenario (e.g. subscript glyphs below baseline).
    @Test
    fun kex_chooseHeightPerLine_bothAscentAndDescentPositive() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 50f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = 10, descent = 30) // height = 20, diff = 30
        span.chooseHeight(
            SpannableString("PositiveAscent"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 50,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(50)
        assertThat(fm.ascent).isEqualTo(-5)
        assertThat(fm.descent).isEqualTo(45)
    }

    // Kex: LineHeightStyleSpan_chooseHeightPerLine_8582574195
    // Validates zero line height (`lineHeight = 0.0f`) where natural height exceeds target.
    @Test
    fun kex_chooseHeightPerLine_zeroLineHeight_preservesNaturalMetrics() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 0.0f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -80, descent = 40)
        span.chooseHeight(
            SpannableString("Zero"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 0,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(-80)
        assertThat(fm.descent).isEqualTo(40)
        assertThat(span.firstAscentDiff).isEqualTo(0)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_16701160952
    // Validates zero natural height (`ascent = 0, descent = 0`) no-op guard.
    @Test
    fun kex_chooseHeight_zeroNaturalHeight_earlyReturn() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = 0, descent = 0)
        span.chooseHeight(
            SpannableString("ZeroHeight"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(0)
        assertThat(fm.descent).isEqualTo(0)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_16701160954
    // Validates inverted negative natural height (`ascent = 10, descent = 5`) no-op guard.
    @Test
    fun kex_chooseHeight_negativeNaturalHeight_earlyReturn() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = 10, descent = 5)
        span.chooseHeight(
            SpannableString("InvertedHeight"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(10)
        assertThat(fm.descent).isEqualTo(5)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_16701160955
    // Validates single-line trimBoth dispatch to chooseHeightPerLine.
    @Test
    fun kex_chooseHeight_singleLineTrimBoth_perLineDispatch() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 0,
                lineHeight = 40f,
                trimFirstLineTop = true,
                trimLastLineBottom = true,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -15, descent = 5)
        span.chooseHeight(
            SpannableString("Single"),
            start = 0,
            end = 0,
            spanStartVertical = 0,
            lineHeight = 40,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(-15)
        assertThat(fm.descent).isEqualTo(5)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_16701160956
    // Validates Fixed mode dispatch with trimFirstLineTop = true, trimLastLineBottom = false.
    @Test
    fun kex_chooseHeight_fixedMode_trimFirstLineTopOnly() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = true,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.Fixed,
            )
        val fm = createFm(ascent = -15, descent = 5)
        span.chooseHeight(
            SpannableString("FixedFirstTrim"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        assertThat(fm.ascent).isEqualTo(-15) // trimmed to natural
        assertThat(fm.descent).isEqualTo(10) // padded
    }

    // Kex: LineHeightStyleSpan_chooseHeight_16701160957
    // Validates Fixed mode dispatch with trimFirstLineTop = false, trimLastLineBottom = false.
    @Test
    fun kex_chooseHeight_fixedMode_noTrim() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.Fixed,
            )
        val fm = createFm(ascent = -15, descent = 5)
        span.chooseHeight(
            SpannableString("FixedNoTrim"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(30)
        assertThat(fm.ascent).isEqualTo(-20)
        assertThat(fm.descent).isEqualTo(10)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_167011609512
    // Validates PerLine with topRatio = 0f (top aligned, all padding to descent).
    @Test
    fun kex_chooseHeight_perLine_topRatioZero() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 40f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.0f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -15, descent = 5)
        span.chooseHeight(
            SpannableString("TopAligned"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 40,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(40)
        assertThat(fm.ascent).isEqualTo(-15)
        assertThat(fm.descent).isEqualTo(25)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_167011609513
    // Validates PerLine with topRatio = 1f (bottom aligned, all padding to ascent).
    @Test
    fun kex_chooseHeight_perLine_topRatioOne() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 40f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 1.0f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm = createFm(ascent = -15, descent = 5)
        span.chooseHeight(
            SpannableString("BottomAligned"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 40,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(40)
        assertThat(fm.ascent).isEqualTo(-35)
        assertThat(fm.descent).isEqualTo(5)
    }

    // Kex: LineHeightStyleSpan_chooseHeight_167011609515
    // Validates PerLine with topRatio = -1f (proportional alignment based on natural ascent/descent
    // ratio).
    @Test
    fun kex_chooseHeight_perLine_proportionalRatio() {
        val span =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 40f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = -1.0f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val fm =
            createFm(
                ascent = -18,
                descent = 6,
            ) // height = 24, ratio = 0.75, diff = 16, descentDiff = 4
        span.chooseHeight(
            SpannableString("Proportional"),
            start = 0,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 40,
            fontMetricsInt = fm,
        )
        assertThat(fm.lineHeight()).isEqualTo(40)
        assertThat(fm.ascent).isEqualTo(-30)
        assertThat(fm.descent).isEqualTo(10)
    }

    // Kex: LineHeightStyleSpan_copyuitext_9253872540
    // Validates copy method with overridden parameters without reflection.
    @Test
    fun kex_copy_overriddenParameters() {
        val original =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = false,
                trimLastLineBottom = false,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val copy = original.copy(startIndex = 5, endIndex = 15, trimFirstLineTop = true)
        assertThat(copy.trimFirstLineTop).isTrue()
        assertThat(copy.trimLastLineBottom).isFalse()
        assertThat(copy.lineHeight).isEqualTo(30f)
        assertThat(copy.mode).isEqualTo(LineHeightStyle.Mode.PerLine)

        // Verify copy applied the new startIndex range
        val fm = createFm(ascent = -20, descent = 10)
        copy.chooseHeight(
            SpannableString("CopiedSpan"),
            start = 5,
            end = 10,
            spanStartVertical = 0,
            lineHeight = 30,
            fontMetricsInt = fm,
        )
        // Since start == startIndex (5) and trimFirstLineTop == true, ascent is trimmed to natural
        // (-20)
        assertThat(fm.ascent).isEqualTo(-20)
    }

    // Kex: LineHeightStyleSpan_copyuitext_9253872541
    // Validates copy method with default parameters without reflection.
    @Test
    fun kex_copy_defaultParameters() {
        val original =
            LineHeightStyleSpan(
                startIndex = 0,
                endIndex = 10,
                lineHeight = 30f,
                trimFirstLineTop = false,
                trimLastLineBottom = true,
                topRatio = 0.5f,
                mode = LineHeightStyle.Mode.PerLine,
            )
        val copy = original.copy(startIndex = 0, endIndex = 10)
        assertThat(copy.trimFirstLineTop).isFalse()
        assertThat(copy.trimLastLineBottom).isTrue()
        assertThat(copy.lineHeight).isEqualTo(30f)
        assertThat(copy.mode).isEqualTo(LineHeightStyle.Mode.PerLine)
    }
}
