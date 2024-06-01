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

package androidx.wear.compose.materialcore

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
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
    fun supports_testtag_on_Text() {
        rule.setContent {
            TextWithDefaults(
                text = AnnotatedString(TestText),
                modifier = Modifier.testTag(TEST_TAG),
                style = ExpectedTextStyle
            )
        }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun validateGreaterMinLinesResultsGreaterSize() {
        var size1 = 0
        var size2 = 0

        rule.setContent {
            Column(Modifier.background(Color.White)) {
                TextWithDefaults(
                    AnnotatedString(TestText),
                    minLines = 1,
                    maxLines = 3,
                    onTextLayout = { size1 = it.size.height },
                    style = ExpectedTextStyle
                )

                TextWithDefaults(
                    AnnotatedString(TestText),
                    minLines = 2,
                    maxLines = 3,
                    onTextLayout = { size2 = it.size.height },
                    style = ExpectedTextStyle
                )
            }
        }

        rule.runOnIdle { Truth.assertThat(size2).isGreaterThan(size1) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMinLinesGreaterThanZero() {
        rule.setContent {
            TextWithDefaults(
                AnnotatedString(TestText),
                minLines = 0,
                maxLines = 1,
                style = ExpectedTextStyle
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun validateMaxLinesGreaterThanMinLines() {
        rule.setContent {
            TextWithDefaults(
                AnnotatedString(TestText),
                minLines = 2,
                maxLines = 1,
                style = ExpectedTextStyle
            )
        }
    }

    @Test
    fun colorParameterOverridesStyleColor() {
        verifyTextColor(Color.Red, ExpectedTextStyle, Color.Red)
    }

    @Test
    fun styleColorOverridesUnspecifiedColor() {
        verifyTextColor(Color.Unspecified, ExpectedTextStyle, ExpectedTextStyle.color)
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
            TextWithDefaults(
                AnnotatedString(TestText),
                onTextLayout = {
                    fontSize = it.layoutInput.style.fontSize
                    fontStyle = it.layoutInput.style.fontStyle
                    fontWeight = it.layoutInput.style.fontWeight
                    fontFamily = it.layoutInput.style.fontFamily
                    letterSpacing = it.layoutInput.style.letterSpacing
                    textDecoration = it.layoutInput.style.textDecoration
                    textAlign = it.layoutInput.style.textAlign
                },
                style = ExpectedTextStyle
            )
        }

        rule.runOnIdle {
            Truth.assertThat(fontSize).isEqualTo(ExpectedTextStyle.fontSize)
            Truth.assertThat(fontStyle).isEqualTo(ExpectedTextStyle.fontStyle)
            Truth.assertThat(fontWeight).isEqualTo(ExpectedTextStyle.fontWeight)
            Truth.assertThat(fontFamily).isEqualTo(ExpectedTextStyle.fontFamily)
            Truth.assertThat(letterSpacing).isEqualTo(ExpectedTextStyle.letterSpacing)
            Truth.assertThat(textDecoration).isEqualTo(ExpectedTextStyle.textDecoration)
            Truth.assertThat(textAlign).isEqualTo(ExpectedTextStyle.textAlign)
        }
    }

    @Test
    fun setsParametersExplicitly() {
        // Test to ensure that when parameter is set explicitly, then this parameter will be used
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Blue
        val expectedTextAlign = TextAlign.End
        val expectedFontSize = 32.sp
        val expectedFontStyle = FontStyle.Italic
        val expectedLetterSpacing = 1.em

        rule.setContent {
            TextWithDefaults(
                AnnotatedString(TestText),
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
                },
                style = ExpectedTextStyle
            )
        }

        rule.runOnIdle {
            Truth.assertThat(textAlign).isEqualTo(expectedTextAlign)
            Truth.assertThat(fontSize).isEqualTo(expectedFontSize)
            Truth.assertThat(fontStyle).isEqualTo(expectedFontStyle)
            Truth.assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    private fun verifyTextColor(color: Color, style: TextStyle, expectedColor: Color) {
        var textColor: Color? = null
        rule.setContent {
            TextWithDefaults(
                AnnotatedString(TestText),
                color = color,
                modifier = Modifier.testTag(TEST_TAG),
                onTextLayout = { textColor = it.layoutInput.style.color },
                style = style
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor, 0.1f)

        rule.runOnIdle { Truth.assertThat(textColor).isEqualTo(expectedColor) }
    }
}

@Composable
internal fun TextWithDefaults(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style
    )
}
