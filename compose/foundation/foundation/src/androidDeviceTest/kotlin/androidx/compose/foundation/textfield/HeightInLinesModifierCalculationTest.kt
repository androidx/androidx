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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.EmptyTextReplacement
import androidx.compose.foundation.text.ceilToIntPx
import androidx.compose.foundation.text.computeSizeForDefaultText
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class HeightInLinesModifierCalculationTest(private val config: TestConfig) {
    data class TestConfig(
        val text: String,
        val lineLimits: TextFieldLineLimits.MultiLine,
        val lineHeight: TextUnit?,
        val alignment: LineHeightStyle.Alignment?,
        val trim: LineHeightStyle.Trim?,
        val mode: LineHeightStyle.Mode?,
    ) {
        override fun toString(): String {
            return "text=$text, limits=$lineLimits, lineHeight=$lineHeight, align=$alignment, trim=$trim, mode=$mode"
        }
    }

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun heightInLinesCalculation() {
        var measuredHeight = -1
        var expectedHeight = -1

        rule.setContent {
            val density = LocalDensity.current
            val fontFamilyResolver = LocalFontFamilyResolver.current

            val textStyle =
                if (config.lineHeight == null) {
                    TextStyle(fontSize = 24.sp)
                } else {
                    TextStyle(
                        fontSize = 24.sp,
                        lineHeight = config.lineHeight,
                        lineHeightStyle =
                            LineHeightStyle(
                                alignment = config.alignment!!,
                                trim = config.trim!!,
                                mode = config.mode!!,
                            ),
                    )
                }

            Layout(
                content = {
                    var text by remember { mutableStateOf(config.text) }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = textStyle,
                        singleLine = false,
                        minLines = config.lineLimits.minHeightInLines,
                        maxLines = config.lineLimits.maxHeightInLines,
                    )
                }
            ) { measurables, constraints ->
                val measurable = measurables[0]
                val placeable = measurable.measure(constraints)
                measuredHeight = placeable.height

                val firstLineHeight =
                    computeSizeForDefaultText(
                            style = textStyle,
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                        )
                        .height

                val firstTwoLinesHeight =
                    computeSizeForDefaultText(
                            style = textStyle,
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                            lines = 2,
                        )
                        .height

                val computedLineHeight = firstTwoLinesHeight - firstLineHeight

                val minLines = config.lineLimits.minHeightInLines

                val maxLines = config.lineLimits.maxHeightInLines

                val minLinesHeight =
                    if (minLines == 1) {
                        -1
                    } else {
                        firstLineHeight + computedLineHeight * (minLines - 1)
                    }

                val maxLinesHeight =
                    if (maxLines == Int.MAX_VALUE) {
                        -1
                    } else {
                        firstLineHeight + computedLineHeight * (maxLines - 1)
                    }

                val textToMeasure = config.text.ifEmpty { EmptyTextReplacement }
                val rawHeight =
                    Paragraph(
                            text = textToMeasure,
                            style = textStyle,
                            maxLines = 1000,
                            density = density,
                            fontFamilyResolver = fontFamilyResolver,
                            constraints = Constraints(),
                        )
                        .height
                        .ceilToIntPx()

                expectedHeight =
                    rawHeight.coerceIn(
                        if (minLinesHeight == -1) 0 else minLinesHeight,
                        if (maxLinesHeight == -1) Int.MAX_VALUE else maxLinesHeight,
                    )

                layout(0, 0) {}
            }
        }

        assertThat(measuredHeight).isEqualTo(expectedHeight)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "config={0}")
        fun params(): Iterable<Array<Any?>> {
            val texts = listOf("", "text")
            // Multiline() and SingleLine don't apply this modifier
            val lineLimitsList =
                listOf(TextFieldLineLimits.MultiLine(2, 5), TextFieldLineLimits.MultiLine(3, 3))
            val lineHeights = listOf(TextUnit.Unspecified, 32.sp)
            val alignments =
                listOf(
                    LineHeightStyle.Alignment.Top,
                    LineHeightStyle.Alignment.Center,
                    LineHeightStyle.Alignment.Proportional,
                    LineHeightStyle.Alignment.Bottom,
                )
            val trims =
                listOf(
                    LineHeightStyle.Trim.None,
                    LineHeightStyle.Trim.Both,
                    LineHeightStyle.Trim.FirstLineTop,
                    LineHeightStyle.Trim.LastLineBottom,
                )
            val modes =
                listOf(
                    LineHeightStyle.Mode.Fixed,
                    LineHeightStyle.Mode.Minimum,
                    LineHeightStyle.Mode.Tight,
                )

            val params = mutableListOf<Array<Any?>>()
            for (text in texts) {
                for (lineLimits in lineLimitsList) {
                    for (lineHeight in lineHeights) {
                        if (lineHeight == TextUnit.Unspecified) {
                            params.add(
                                arrayOf(TestConfig(text, lineLimits, null, null, null, null))
                            )
                        } else {
                            for (alignment in alignments) {
                                for (trim in trims) {
                                    for (mode in modes) {
                                        params.add(
                                            arrayOf(
                                                TestConfig(
                                                    text,
                                                    lineLimits,
                                                    lineHeight,
                                                    alignment,
                                                    trim,
                                                    mode,
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return params
        }
    }
}
