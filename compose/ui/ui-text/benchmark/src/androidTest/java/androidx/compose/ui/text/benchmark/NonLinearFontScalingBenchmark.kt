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

package androidx.compose.ui.text.benchmark

import android.content.Context
import android.util.TypedValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests font rendering performance when using the platform APIs to calculate pixel values for font
 * sizes under non-linear font scaling.
 */
@LargeTest
@RunWith(Parameterized::class)
class NonLinearFontScalingBenchmark(
    private val textLength: Int,
    fontSizeSp: Int,
    private val isLineHeightSp: Boolean
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} fontSize={1} isLineHeightSp={2}")
        fun initParameters(): List<Array<Any?>> =
            cartesian(
                arrayOf(512),
                // font size
                arrayOf(8, 30),
                // isLineHeightSp. This helps us verify that the calculation to keep line heights
                // proportional doesn't affect performance too much. (see b/273326061)
                arrayOf(false, true)
            )
    }

    @get:Rule val benchmarkRule = BenchmarkRule()

    @get:Rule val textBenchmarkRule = TextBenchmarkTestRule(Alphabet.Latin)

    private lateinit var instrumentationContext: Context

    // Width initialized in setup().
    private var width: Float = 0f
    private val fontSize = fontSizeSp.sp

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        width =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                textBenchmarkRule.widthDp,
                instrumentationContext.resources.displayMetrics
            )
    }

    private fun text(textGenerator: RandomTextGenerator): String {
        return textGenerator.nextParagraph(textLength) + "\n"
    }

    private fun paragraph(text: String, width: Float, density: Density): Paragraph {
        return Paragraph(
            paragraphIntrinsics = paragraphIntrinsics(text, density),
            constraints = Constraints(maxWidth = ceil(width).toInt()),
            overflow = TextOverflow.Clip
        )
    }

    private fun paragraphIntrinsics(text: String, density: Density): ParagraphIntrinsics {
        assertThat(fontSize.isSp).isTrue()

        @Suppress("DEPRECATION")
        val style =
            if (isLineHeightSp) {
                TextStyle(
                    fontSize = fontSize,
                    lineHeight = fontSize * 2,
                    lineHeightStyle = LineHeightStyle.Default,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            } else {
                TextStyle(
                    fontSize = fontSize,
                    lineHeight = 2.em,
                    lineHeightStyle = LineHeightStyle.Default,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            }

        return ParagraphIntrinsics(
            text = text,
            density = density,
            style = style,
            fontFamilyResolver = createFontFamilyResolver(instrumentationContext)
        )
    }

    @Test
    fun nonLinearfontScaling1x_construct() {
        val density =
            Density(instrumentationContext.resources.displayMetrics.density, fontScale = 1f)

        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val text = runWithTimingDisabled {
                    // create a new paragraph and use a smaller width to get
                    // some line breaking in the result
                    text(textGenerator)
                }

                paragraph(text = text, width = width, density = density)
            }
        }
    }

    @Test
    fun nonLinearfontScaling2x_construct() {
        val density =
            Density(instrumentationContext.resources.displayMetrics.density, fontScale = 2f)

        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val text = runWithTimingDisabled {
                    // create a new paragraph and use a smaller width to get
                    // some line breaking in the result
                    text(textGenerator)
                }

                paragraph(text = text, width = width, density = density)
            }
        }
    }
}
