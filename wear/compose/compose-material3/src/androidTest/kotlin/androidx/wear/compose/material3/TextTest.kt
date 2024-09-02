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

package androidx.wear.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalArgumentException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class TextTest {
    @get:Rule val rule = createComposeRule()

    private val ExpectedTextStyle =
        TextStyle(
            color = Color.Red,
            fontSize = 32.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontFamily = FontFamily.Default,
            letterSpacing = 1.sp,
            textDecoration = TextDecoration.Underline,
            textAlign = TextAlign.End,
            lineHeight = 10.sp,
        )

    private val TestText = "TestText"

    @Test
    fun testDefaultIncludeFontPadding() {
        var localTextStyle: TextStyle? = null
        var displayMediumTextStyle: TextStyle? = null
        rule.setContent {
            MaterialTheme {
                localTextStyle = LocalTextStyle.current
                displayMediumTextStyle = LocalTypography.current.displayMedium
            }
        }

        assertThat(localTextStyle?.platformStyle?.paragraphStyle?.includeFontPadding)
            .isEqualTo(false)

        assertThat(displayMediumTextStyle?.platformStyle?.paragraphStyle?.includeFontPadding)
            .isEqualTo(false)
    }

    @Test
    fun validateGreaterMinLinesResultsGreaterSize() {
        var size1 = 0
        var size2 = 0

        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                Column(Modifier.background(Color.White)) {
                    Text(
                        "Lorem ipsum",
                        minLines = 1,
                        maxLines = 3,
                        onTextLayout = { size1 = it.size.height }
                    )

                    Text(
                        "Lorem ipsum",
                        minLines = 2,
                        maxLines = 3,
                        onTextLayout = { size2 = it.size.height }
                    )
                }
            }
        }

        rule.runOnIdle { assertThat(size2).isGreaterThan(size1) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMinLinesGreaterThanZero() {
        rule.setContent { Text(TestText, minLines = 0) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMaxLinesGreaterThanMinLines() {
        rule.setContent { Text(TestText, minLines = 2, maxLines = 1) }
    }

    @Test
    fun colorParameterOverridesStyleColor() {
        var textColor: Color? = null
        val expectedColor = Color.Red

        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                ProvideTextStyle(ExpectedTextStyle) {
                    Box(Modifier.background(Color.White)) {
                        Text(
                            TestText,
                            color = expectedColor,
                            onTextLayout = { textColor = it.layoutInput.style.color }
                        )
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(textColor).isEqualTo(expectedColor) }
    }

    @Test
    fun styleColorOverridesLocalContentColor() {
        var textColor: Color? = null

        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                ProvideTextStyle(ExpectedTextStyle) {
                    Box(Modifier.background(Color.White)) {
                        Text(TestText, onTextLayout = { textColor = it.layoutInput.style.color })
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(textColor).isEqualTo(ExpectedTextStyle.color) }
    }

    @Test
    fun usesLocalContentColorAsFallback() {
        var textColor: Color? = null

        rule.setContent {
            CompositionLocalProvider(LocalContentColor provides Color.Blue) {
                Box(Modifier.background(Color.White)) {
                    Text(TestText, onTextLayout = { textColor = it.layoutInput.style.color })
                }
            }
        }

        rule.runOnIdle { assertThat(textColor).isEqualTo(Color.Blue) }
    }

    @Test
    fun inheritsTextStyle() {
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var fontWeight: FontWeight? = null
        var fontFamily: FontFamily? = null
        var letterSpacing: TextUnit? = null
        var textDecoration: TextDecoration? = null
        var textAlign: TextAlign? = null

        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
                    Text(
                        TestText,
                        onTextLayout = {
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            fontWeight = it.layoutInput.style.fontWeight
                            fontFamily = it.layoutInput.style.fontFamily
                            letterSpacing = it.layoutInput.style.letterSpacing
                            textDecoration = it.layoutInput.style.textDecoration
                            textAlign = it.layoutInput.style.textAlign
                        }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(fontSize).isEqualTo(ExpectedTextStyle.fontSize)
            assertThat(fontStyle).isEqualTo(ExpectedTextStyle.fontStyle)
            assertThat(fontWeight).isEqualTo(ExpectedTextStyle.fontWeight)
            assertThat(fontFamily).isEqualTo(ExpectedTextStyle.fontFamily)
            assertThat(letterSpacing).isEqualTo(ExpectedTextStyle.letterSpacing)
            assertThat(textDecoration).isEqualTo(ExpectedTextStyle.textDecoration)
            assertThat(textAlign).isEqualTo(ExpectedTextStyle.textAlign)
        }
    }

    @Test
    fun setsParametersExplicitly() {
        // Test to ensure that when parameter is set explicitly, then this parameter will be used
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Red
        val expectedTextAlign = TextAlign.End
        val expectedFontSize = 32.sp
        val expectedFontStyle = FontStyle.Italic
        val expectedLetterSpacing = 1.em

        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
                    Text(
                        TestText,
                        color = expectedColor,
                        textAlign = expectedTextAlign,
                        fontSize = expectedFontSize,
                        fontStyle = expectedFontStyle,
                        letterSpacing = expectedLetterSpacing,
                        onTextLayout = {
                            textAlign = it.layoutInput.style.textAlign
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            letterSpacing = it.layoutInput.style.letterSpacing
                        }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    @Test
    fun verify_max_text_lines_limits_number_of_lines() {
        val maxLines = 2
        val text =
            "Long text that will cover multiple lines of text, and needs to be more " +
                "than 2 lines so that we can make sure that our max lines override is " +
                "working otherwise it would not be a good test."
        var result: TextLayoutResult? = null
        rule.setContent {
            CompositionLocalProvider(
                LocalTextConfiguration provides
                    TextConfiguration(
                        maxLines = maxLines,
                        textAlign = TextConfigurationDefaults.TextAlign,
                        overflow = TextConfigurationDefaults.Overflow,
                    )
            ) {
                Text(
                    text = text,
                    onTextLayout = { textLayoutResult -> result = textLayoutResult },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertThat(result!!.lineCount).isEqualTo(maxLines)
    }

    @Test
    fun verify_text_without_maxlines_does_not_limit_number_of_lines() {
        val text =
            "Long text that will cover multiple lines of text, and needs to be more " +
                "than 2 lines so that we can make sure that our max lines override is " +
                "working otherwise it would not be a good test."
        var result: TextLayoutResult? = null
        rule.setContent {
            Text(
                text = text,
                onTextLayout = { textLayoutResult -> result = textLayoutResult },
                modifier = Modifier.testTag(TEST_TAG).width(200.dp)
            )
        }

        assertThat(result!!.lineCount).isAtLeast(3)
    }

    @Test
    fun verify_overflow_defaults_to_clip() {
        val text =
            "Long text that will cover multiple lines of text, and needs to be more " +
                "than 2 lines so that we can make sure that our max lines override is " +
                "working otherwise it would not be a good test."
        var result: TextLayoutResult? = null
        rule.setContent {
            Text(
                text = text,
                maxLines = 1,
                onTextLayout = { textLayoutResult -> result = textLayoutResult },
                modifier = Modifier.testTag(TEST_TAG)
            )
        }

        assertThat(result!!.isLineEllipsized(0)).isFalse()
    }

    @Test
    fun verify_overflow_is_overridable() {
        val text =
            "Long text that will cover multiple lines of text, and needs to be more " +
                "than 2 lines so that we can make sure that our max lines override is " +
                "working otherwise it would not be a good test."
        var result: TextLayoutResult? = null
        rule.setContent {
            CompositionLocalProvider(
                LocalTextConfiguration provides
                    TextConfiguration(
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextConfigurationDefaults.TextAlign,
                        maxLines = TextConfigurationDefaults.MaxLines,
                    )
            ) {
                Text(
                    text = text,
                    maxLines = 1,
                    onTextLayout = { textLayoutResult -> result = textLayoutResult },
                    modifier = Modifier.testTag(TEST_TAG)
                )
            }
        }

        assertThat(result!!.isLineEllipsized(0)).isTrue()
    }
}
