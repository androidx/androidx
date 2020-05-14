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
import androidx.ui.unit.Density
import androidx.ui.unit.sp
import androidx.ui.graphics.Canvas
import androidx.ui.graphics.ImageAsset
import androidx.ui.integration.test.Alphabet
import androidx.ui.integration.test.RandomTextGenerator
import androidx.ui.integration.test.TextBenchmarkTestRule
import androidx.ui.integration.test.TextType
import androidx.ui.integration.test.cartesian
import androidx.ui.text.font.Font
import androidx.ui.text.style.TextDirectionAlgorithm
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.math.roundToInt

@LargeTest
@RunWith(Parameterized::class)
class ParagraphBenchmark(
    private val textLength: Int,
    private val textType: TextType,
    alphabet: Alphabet
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "length={0} type={1} alphabet={2}")
        fun initParameters(): List<Array<Any>> = cartesian(
            arrayOf(32, 512),
            arrayOf(TextType.PlainText, TextType.StyledText),
            arrayOf(Alphabet.Latin, Alphabet.Cjk)
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule(alphabet)

    // dummy object required to construct Paragraph
    private val resourceLoader = object : Font.ResourceLoader {
        override fun load(font: Font): Any {
            return false
        }
    }

    private fun text(textGenerator: RandomTextGenerator): AnnotatedString {
        val text = textGenerator.nextParagraph(textLength)
        val spanStyles = if (textType == TextType.StyledText) {
            textGenerator.createStyles(text)
        } else {
            listOf()
        }
        return AnnotatedString(text = text, spanStyles = spanStyles)
    }

    private fun paragraph(
        text: String,
        spanStyles: List<AnnotatedString.Range<SpanStyle>>,
        constraints: ParagraphConstraints
    ): Paragraph {
        return Paragraph(
            paragraphIntrinsics = paragraphIntrinsics(text, spanStyles),
            constraints = constraints
        )
    }

    private fun paragraphIntrinsics(
        textGenerator: RandomTextGenerator
    ): ParagraphIntrinsics {
        val annotatedString = text(textGenerator)
        return paragraphIntrinsics(
            text = annotatedString.text,
            spanStyles = annotatedString.spanStyles
        )
    }

    private fun paragraphIntrinsics(
        text: String,
        spanStyles: List<AnnotatedString.Range<SpanStyle>>
    ): ParagraphIntrinsics {
        return ParagraphIntrinsics(
            text = text,
            density = Density(density = 1f),
            style = TextStyle(
                fontSize = 12.sp,
                textDirectionAlgorithm = TextDirectionAlgorithm.ContentOrLtr
            ),
            resourceLoader = resourceLoader,
            spanStyles = spanStyles
        )
    }

    @Test
    fun minIntrinsicWidth() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val intrinsics = runWithTimingDisabled {
                    paragraphIntrinsics(textGenerator)
                }

                intrinsics.minIntrinsicWidth
            }
        }
    }

    @Test
    fun maxIntrinsicWidth() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val intrinsics = runWithTimingDisabled {
                    paragraphIntrinsics(textGenerator)
                }

                intrinsics.maxIntrinsicWidth
            }
        }
    }

    @Test
    fun construct() {
        textBenchmarkRule.generator { textGenerator ->
            benchmarkRule.measureRepeated {
                val textAndWidth = runWithTimingDisabled {
                    val intrinsics = paragraphIntrinsics(textGenerator)
                    // create a new paragraph and use a smaller width to get
                    // some line breaking in the result
                    Pair(
                        text(textGenerator),
                        intrinsics.maxIntrinsicWidth / 4f
                    )
                }

                paragraph(
                    text = textAndWidth.first.text,
                    spanStyles = textAndWidth.first.spanStyles,
                    constraints = ParagraphConstraints(textAndWidth.second)
                )
            }
        }
    }

    /**
     * The time taken to paint the [Paragraph] on [Canvas] for the first time.
     */
    @Test
    fun first_paint() {
        textBenchmarkRule.generator { textGenerator ->
            val width = paragraphIntrinsics(textGenerator).maxIntrinsicWidth / 4f
            benchmarkRule.measureRepeated {
                val (paragraph, canvas) = runWithTimingDisabled {
                    val (text, style) = text(textGenerator)
                    // create a new paragraph and use a smaller width to get
                    // some line breaking in the result
                    val paragraph = paragraph(text, style, ParagraphConstraints(width))
                    val canvas = Canvas(
                        ImageAsset(paragraph.width.roundToInt(), paragraph.height.roundToInt())
                    )
                    Pair(paragraph, canvas)
                }
                paragraph.paint(canvas)
            }
        }
    }

    /**
     * The time taken to repaint the [Paragraph] on [Canvas].
     */
    @Test
    fun paint() {
        textBenchmarkRule.generator { textGenerator ->
            val width = paragraphIntrinsics(textGenerator).maxIntrinsicWidth / 4f
            val (text, style) = text(textGenerator)
            // create a new paragraph and use a smaller width to get
            // some line breaking in the result
            val paragraph = paragraph(text, style, ParagraphConstraints(width))
            val canvas = Canvas(
                ImageAsset(paragraph.width.roundToInt(), paragraph.height.roundToInt())
            )
            // Paint for the first time, so that we only benchmark repaint.
            paragraph.paint(canvas)
            benchmarkRule.measureRepeated {
                paragraph.paint(canvas)
            }
        }
    }
}