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
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.sp
import androidx.ui.test.Alphabet
import androidx.ui.test.RandomTextGenerator
import androidx.ui.test.TextType
import androidx.ui.test.cartesian
import androidx.ui.text.font.Font
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

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
            arrayOf(32, 128, 512),
            arrayOf(TextType.PlainText, TextType.StyledText),
            arrayOf(Alphabet.Latin, Alphabet.Cjk)
        )
    }

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    val textGenerator = RandomTextGenerator(alphabet = alphabet)

    // dummy object required to construct Paragraph
    private val resourceLoader = object : Font.ResourceLoader {
        override fun load(font: Font): Any {
            return false
        }
    }

    private fun paragraph(): Paragraph {
        val text = textGenerator.nextParagraph(textLength)
        val styles = if (textType == TextType.StyledText) {
            textGenerator.createStyles(text)
        } else {
            listOf()
        }
        return Paragraph(
            text = text,
            density = Density(density = 1f),
            style = TextStyle(fontSize = 12.sp),
            paragraphStyle = ParagraphStyle(),
            resourceLoader = resourceLoader,
            textStyles = styles,
            layoutDirection = LayoutDirection.Ltr
        )
    }

    @Test
    fun minIntrinsicWidth() {
        benchmarkRule.measureRepeated {
            val paragraph = runWithTimingDisabled {
                paragraph()
            }

            paragraph.minIntrinsicWidth
        }
    }

    @Test
    fun layout() {
        benchmarkRule.measureRepeated {
            val pair = runWithTimingDisabled {
                // measure an approximate max intrinsic width
                val paragraph = paragraph()
                paragraph.layout(ParagraphConstraints(Float.MAX_VALUE))
                // create a new paragraph and use a smaller width to get
                // some line breaking in the result
                Pair(paragraph(), paragraph.maxIntrinsicWidth / 4f)
            }

            pair.first.layout(ParagraphConstraints(pair.second))
        }
    }
}