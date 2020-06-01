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
import androidx.ui.unit.PxPosition
import androidx.ui.unit.sp
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

/**
 * The benchmark for methods of [Paragraph].
 */
@LargeTest
@RunWith(Parameterized::class)
class ParagraphMethodBenchmark(private val textType: TextType, private val textLength: Int) {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    val textBenchmarkRule = TextBenchmarkTestRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "type={0} length={1}")
        fun initParameters() = cartesian(
            arrayOf(TextType.PlainText, TextType.StyledText),
            arrayOf(512)
        )
    }

    // dummy object required to construct Paragraph
    private val resourceLoader = object : Font.ResourceLoader {
        override fun load(font: Font): Any {
            return false
        }
    }

    private fun paragraphIntrinsics(
        textGenerator: RandomTextGenerator,
        textLength: Int
    ): ParagraphIntrinsics {
        val text = textGenerator.nextParagraph(textLength)
        val spanStyles = if (textType == TextType.StyledText) {
            textGenerator.createStyles(text)
        } else {
            listOf()
        }
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

    private fun paragraph(
        textGenerator: RandomTextGenerator,
        textLength: Int = this.textLength,
        preferredLineCount: Int = 4
    ): Paragraph {
        val paragraphIntrinsics = paragraphIntrinsics(textGenerator, textLength)
        return Paragraph(
            paragraphIntrinsics = paragraphIntrinsics,
            constraints = ParagraphConstraints(
                width = paragraphIntrinsics.maxIntrinsicWidth / preferredLineCount
            )
        )
    }

    @Test
    fun getPathForRange() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            benchmarkRule.measureRepeated {
                paragraph.getPathForRange(0, textLength / 2)
            }
        }
    }

    @Test
    fun getCursorRect() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            benchmarkRule.measureRepeated {
                paragraph.getCursorRect(textLength / 2)
            }
        }
    }

    @Test
    fun getLineLeft() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            val line = paragraph.lineCount / 2
            benchmarkRule.measureRepeated {
                paragraph.getLineLeft(line)
            }
        }
    }

    @Test
    fun getLineRight() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            val line = paragraph.lineCount / 2
            benchmarkRule.measureRepeated {
                paragraph.getLineRight(line)
            }
        }
    }

    @Test
    fun getLineWidth() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            val line = paragraph.lineCount / 2
            benchmarkRule.measureRepeated {
                paragraph.getLineWidth(line / 2)
            }
        }
    }

    @Test
    fun getHorizontalPosition() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            benchmarkRule.measureRepeated {
                paragraph.getHorizontalPosition(textLength / 2, true)
            }
        }
    }

    @Test
    fun getOffsetForPosition() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            val centerPosition = PxPosition((paragraph.width / 2), (paragraph.height / 2))
            benchmarkRule.measureRepeated {
                paragraph.getOffsetForPosition(centerPosition)
            }
        }
    }

    @Test
    fun getBoundingBox() {
        textBenchmarkRule.generator { generator ->
            val paragraph = paragraph(generator)
            benchmarkRule.measureRepeated {
                paragraph.getBoundingBox(textLength / 2)
            }
        }
    }
}