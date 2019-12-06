/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.filters.LargeTest
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.IntPx
import androidx.ui.core.LayoutDirection
import androidx.ui.core.ipx
import androidx.ui.core.sp
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.Image
import androidx.ui.test.RandomTextGenerator
import androidx.ui.test.TextBenchmarkTestRule
import androidx.ui.test.cartesian
import androidx.ui.text.font.Font
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.ceil
import kotlin.math.roundToInt

@LargeTest
@RunWith(Parameterized::class)
class TextDelegateBenchmark(
    private val textLength: Int,
    private val styleCount: Int
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(
            name = "length={0} styleCount={1}"
        )
        fun initParameters() =
            cartesian(
                arrayOf(32, 512),
                arrayOf(0, 32)
            )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkTestRule = TextBenchmarkTestRule()

    // dummy object required to construct Paragraph
    private val resourceLoader = object : Font.ResourceLoader {
        override fun load(font: Font): Any {
            return false
        }
    }

    private fun textDelegate(textGenerator: RandomTextGenerator): TextDelegate {
        val text = textGenerator.nextAnnotatedString(
            length = textLength,
            styleCount = styleCount,
            hasMetricAffectingStyle = true
        )

        return TextDelegate(
            text = text,
            density = Density(density = 1f),
            spanStyle = SpanStyle(fontSize = 12.sp),
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader
        )
    }

    @Test
    fun constructor() {
        textBenchmarkTestRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val text = runWithTimingDisabled {
                    textGenerator.nextAnnotatedString(
                        length = textLength,
                        styleCount = styleCount,
                        hasMetricAffectingStyle = true
                    )
                }

                TextDelegate(
                    text = text,
                    density = Density(density = 1f),
                    spanStyle = SpanStyle(fontSize = 12.sp),
                    layoutDirection = LayoutDirection.Ltr,
                    resourceLoader = resourceLoader
                )
            }
        }
    }

    /**
     * Measure the time taken by the first time layout of TextDelegate.
     */
    @Test
    fun first_layout() {
        textBenchmarkTestRule.generator { textGenerator ->
            val maxWidth = textDelegate(textGenerator).let {
                it.layout(Constraints())
                (it.maxIntrinsicWidth.value / 4f).toIntPx()
            }
            benchmarkRule.measureRepeated {
                val textDelegate = runWithTimingDisabled {
                    textDelegate(textGenerator)
                }
                textDelegate.layout(Constraints(maxWidth = maxWidth))
            }
        }
    }

    /**
     * Measure the time taken by re-layout text with different constraints.
     */
    @Test
    fun layout() {
        textBenchmarkTestRule.generator { textGenerator ->
            val width = textDelegate(textGenerator).let {
                it.layout(Constraints())
                (it.maxIntrinsicWidth.value / 4f).toIntPx()
            }
            val textDelegate = textDelegate(textGenerator)

            val offset = 20
            var sign = 1
            benchmarkRule.measureRepeated {
                val maxWidth = width.value + sign * offset
                sign *= -1
                textDelegate.layout(Constraints(maxWidth = maxWidth.ipx))
            }
        }
    }

    /**
     * Measure the time taken by the first time painting a TextDelegate on Canvas.
     */
    @Test
    fun first_paint() {
        textBenchmarkTestRule.generator { textGenerator ->
            val maxWidth = textDelegate(textGenerator).let {
                it.layout(Constraints())
                (it.maxIntrinsicWidth.value / 4f).toIntPx()
            }
            benchmarkRule.measureRepeated {
                val (textDelegate, canvas) = runWithTimingDisabled {
                    textDelegate(textGenerator).let {
                        it.layout(Constraints(maxWidth = maxWidth))
                        val canvas = Canvas(Image(it.width.value, it.height.value))
                        Pair(it, canvas)
                    }
                }
                textDelegate.paint(canvas)
            }
        }
    }

    /**
     * Measure the time taken by painting a TextDelegate on Canvas.
     */
    @Test
    fun paint() {
        textBenchmarkTestRule.generator { textGenerator ->
            val maxWidth = textDelegate(textGenerator).let {
                it.layout(Constraints())
                (it.maxIntrinsicWidth.value / 4f).toIntPx()
            }
            val textDelegate = textDelegate(textGenerator)
            textDelegate.layout(Constraints(maxWidth = maxWidth))
            val canvas = Canvas(
                Image(textDelegate.width.value, textDelegate.height.value)
            )

            benchmarkRule.measureRepeated {
                textDelegate.paint(canvas)
            }
        }
    }
}

fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx