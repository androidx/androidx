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

package androidx.ui.foundation

import androidx.test.filters.MediumTest
import androidx.ui.graphics.Color
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.TextUnit
import androidx.ui.unit.em
import androidx.ui.unit.sp
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class TextTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val ExpectedTextStyle = TextStyle(
        color = Color.Blue,
        textAlign = TextAlign.End,
        fontSize = 32.sp,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.3.em
    )

    private val TestText = "TestText"

    @Test
    fun inheritsCurrentTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(
                        TestText,
                        onTextLayout = {
                            textColor = it.layoutInput.style.color
                            textAlign = it.layoutInput.style.textAlign
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            letterSpacing = it.layoutInput.style.letterSpacing
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(textColor).isEqualTo(ExpectedTextStyle.color)
            Truth.assertThat(textAlign).isEqualTo(ExpectedTextStyle.textAlign)
            Truth.assertThat(fontSize).isEqualTo(ExpectedTextStyle.fontSize)
            Truth.assertThat(fontStyle).isEqualTo(ExpectedTextStyle.fontStyle)
            Truth.assertThat(letterSpacing).isEqualTo(ExpectedTextStyle.letterSpacing)
        }
    }

    @Test
    fun settingCustomTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val testStyle = TextStyle(
            color = Color.Green,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            fontStyle = FontStyle.Normal,
            letterSpacing = 0.6.em
        )
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(
                        TestText,
                        style = testStyle,
                        onTextLayout = {
                            textColor = it.layoutInput.style.color
                            textAlign = it.layoutInput.style.textAlign
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            letterSpacing = it.layoutInput.style.letterSpacing
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            Truth.assertThat(textColor).isEqualTo(testStyle.color)
            Truth.assertThat(textAlign).isEqualTo(testStyle.textAlign)
            Truth.assertThat(fontSize).isEqualTo(testStyle.fontSize)
            Truth.assertThat(fontStyle).isEqualTo(testStyle.fontStyle)
            Truth.assertThat(letterSpacing).isEqualTo(testStyle.letterSpacing)
        }
    }

    @Test
    fun settingParametersExplicitly() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Green
        val expectedTextAlign = TextAlign.Center
        val expectedFontSize = 16.sp
        val expectedFontStyle = FontStyle.Normal
        val expectedLetterSpacing = 0.6.em

        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    Text(
                        TestText,
                        color = expectedColor,
                        textAlign = expectedTextAlign,
                        fontSize = expectedFontSize,
                        fontStyle = expectedFontStyle,
                        letterSpacing = expectedLetterSpacing,
                        onTextLayout = {
                            textColor = it.layoutInput.style.color
                            textAlign = it.layoutInput.style.textAlign
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            letterSpacing = it.layoutInput.style.letterSpacing
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            // explicit parameters should override values from the style.
            Truth.assertThat(textColor).isEqualTo(expectedColor)
            Truth.assertThat(textAlign).isEqualTo(expectedTextAlign)
            Truth.assertThat(fontSize).isEqualTo(expectedFontSize)
            Truth.assertThat(fontStyle).isEqualTo(expectedFontStyle)
            Truth.assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    // Not really an expected use-case, but we should ensure the behavior here is consistent.
    @Test
    fun settingColorAndTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Green
        val expectedTextAlign = TextAlign.Center
        val expectedFontSize = 16.sp
        val expectedFontStyle = FontStyle.Normal
        val expectedLetterSpacing = 0.6.em
        composeTestRule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(backgroundColor = Color.White) {
                    // Set both color and style
                    Text(
                        TestText,
                        color = expectedColor,
                        textAlign = expectedTextAlign,
                        fontSize = expectedFontSize,
                        fontStyle = expectedFontStyle,
                        letterSpacing = expectedLetterSpacing,
                        style = ExpectedTextStyle,
                        onTextLayout = {
                            textColor = it.layoutInput.style.color
                            textAlign = it.layoutInput.style.textAlign
                            fontSize = it.layoutInput.style.fontSize
                            fontStyle = it.layoutInput.style.fontStyle
                            letterSpacing = it.layoutInput.style.letterSpacing
                        }
                    )
                }
            }
        }

        runOnIdleCompose {
            // explicit parameters should override values from the style.
            Truth.assertThat(textColor).isEqualTo(expectedColor)
            Truth.assertThat(textAlign).isEqualTo(expectedTextAlign)
            Truth.assertThat(fontSize).isEqualTo(expectedFontSize)
            Truth.assertThat(fontStyle).isEqualTo(expectedFontStyle)
            Truth.assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }
}
