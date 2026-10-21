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

package androidx.compose.ui.text.benchmark

import android.content.Context
import android.util.TypedValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.roundToInt
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Microbenchmark for [TextLayoutResult.getBoundingBoxesForRange] and
 * [TextLayoutResult.fillPathForRange].
 */
@LargeTest
@RunWith(Parameterized::class)
class TextLayoutResultRangeBenchmark(
    private val textLength: Int,
    private val rangeTextKind: RangeTextKind,
) {
    enum class RangeTextKind {
        PlainText,
        StyledText,
        BiDiText,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} kind={1}")
        fun initParameters(): List<Array<Any?>> =
            cartesian(
                arrayOf(32, 512),
                arrayOf(
                    RangeTextKind.PlainText,
                    RangeTextKind.StyledText,
                    RangeTextKind.BiDiText,
                ),
            )
    }

    @get:Rule val benchmarkRule = BenchmarkRule()

    @get:Rule val textBenchmarkRule = TextBenchmarkTestRule()

    private lateinit var instrumentationContext: Context
    private var width: Int = 0
    private val fontSize = textBenchmarkRule.fontSizeSp.sp

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        width =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    textBenchmarkRule.widthDp,
                    instrumentationContext.resources.displayMetrics,
                )
                .roundToInt()
    }

    private fun createAnnotatedString(textGenerator: RandomTextGenerator): AnnotatedString {
        return when (rangeTextKind) {
            RangeTextKind.PlainText -> {
                AnnotatedString(text = textGenerator.nextParagraph(textLength))
            }
            RangeTextKind.StyledText -> {
                val text = textGenerator.nextParagraph(textLength)
                AnnotatedString(text = text, spanStyles = textGenerator.createStyles(text))
            }
            RangeTextKind.BiDiText -> {
                val sb = StringBuilder(textLength)
                val ltrWord = "compose "
                val rtlWord = "\u05E9\u05DC\u05D5\u05DD\u05E2\u05D5\u05DC\u05DD "
                var toggle = false
                while (sb.length < textLength) {
                    sb.append(if (toggle) rtlWord else ltrWord)
                    toggle = !toggle
                }
                AnnotatedString(text = sb.substring(0, textLength))
            }
        }
    }

    private fun measureTextLayoutResult(textGenerator: RandomTextGenerator): TextLayoutResult {
        val textMeasurer =
            TextMeasurer(
                defaultFontFamilyResolver = createFontFamilyResolver(instrumentationContext),
                defaultLocaleList = LocaleList("en"),
                defaultDensity = Density(instrumentationContext),
                defaultLayoutDirection = LayoutDirection.Ltr,
                cacheSize = 0,
            )
        return textMeasurer.measure(
            text = createAnnotatedString(textGenerator),
            style = TextStyle(fontSize = fontSize),
            constraints = Constraints.fixedWidth(width),
        )
    }

    @Test
    fun getBoundingBoxesForRange() {
        textBenchmarkRule.generator { generator ->
            val layoutResult = measureTextLayoutResult(generator)
            val start = textLength / 4
            val end = (textLength * 3) / 4
            benchmarkRule.measureRepeated { layoutResult.getBoundingBoxesForRange(start, end) }
        }
    }

    @Test
    fun fillPathForRange_merged_rounded() {
        textBenchmarkRule.generator { generator ->
            val layoutResult = measureTextLayoutResult(generator)
            val start = textLength / 4
            val end = (textLength * 3) / 4
            val targetPath = Path()
            val radius = CornerRadius(8f)
            benchmarkRule.measureRepeated {
                layoutResult.fillPathForRange(
                    start = start,
                    end = end,
                    target = targetPath,
                    cornerRadius = radius,
                    mergeLines = true,
                )
            }
        }
    }

    @Test
    fun fillPathForRange_merged_sharp() {
        textBenchmarkRule.generator { generator ->
            val layoutResult = measureTextLayoutResult(generator)
            val start = textLength / 4
            val end = (textLength * 3) / 4
            val targetPath = Path()
            benchmarkRule.measureRepeated {
                layoutResult.fillPathForRange(
                    start = start,
                    end = end,
                    target = targetPath,
                    cornerRadius = CornerRadius.Zero,
                    mergeLines = true,
                )
            }
        }
    }

    @Test
    fun fillPathForRange_unmerged_rounded() {
        textBenchmarkRule.generator { generator ->
            val layoutResult = measureTextLayoutResult(generator)
            val start = textLength / 4
            val end = (textLength * 3) / 4
            val targetPath = Path()
            val radius = CornerRadius(8f)
            benchmarkRule.measureRepeated {
                layoutResult.fillPathForRange(
                    start = start,
                    end = end,
                    target = targetPath,
                    cornerRadius = radius,
                    mergeLines = false,
                )
            }
        }
    }

    @Test
    fun getPathForRange() {
        textBenchmarkRule.generator { generator ->
            val layoutResult = measureTextLayoutResult(generator)
            val start = textLength / 4
            val end = (textLength * 3) / 4
            benchmarkRule.measureRepeated { layoutResult.getPathForRange(start, end) }
        }
    }
}
