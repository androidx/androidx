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
import androidx.ui.unit.Density
import androidx.ui.unit.IntPx
import androidx.ui.core.LayoutDirection
import androidx.ui.unit.ipx
import androidx.ui.unit.sp
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ImageAsset
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.integration.test.TextBenchmarkTestRule
import androidx.ui.integration.test.cartesian
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
            style = TextStyle(fontSize = 12.sp),
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
                    style = TextStyle(fontSize = 12.sp),
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
                val (textDelegate, canvas, layoutResult) = runWithTimingDisabled {
                    textDelegate(textGenerator).let {
                        val layoutResult = it.layout(Constraints(maxWidth = maxWidth))
                        val canvas = Canvas(
                            ImageAsset(
                                layoutResult.size.width.value,
                                layoutResult.size.height.value
                            )
                        )
                        Triple(it, canvas, layoutResult)
                    }
                }
                textDelegate.paint(canvas, layoutResult)
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
            val layoutResult = textDelegate.layout(Constraints(maxWidth = maxWidth))
            val canvas = Canvas(
                ImageAsset(layoutResult.size.width.value, layoutResult.size.height.value)
            )

            benchmarkRule.measureRepeated {
                textDelegate.paint(canvas, layoutResult)
            }
        }
    }
}

fun Float.toIntPx(): IntPx = ceil(this).roundToInt().ipx