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

package androidx.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextTest {

    @get:Rule
    val rule = createComposeRule()

    private val ExpectedTextStyle = TextStyle(
        color = Color.Blue,
        textAlign = TextAlign.End,
        fontSize = 32.sp,
        fontStyle = FontStyle.Italic,
        letterSpacing = 0.3.em
    )

    private val TestText = "TestText"

    @Test
    fun testDefaultIncludeFontPadding() {
        var localTextStyle: TextStyle? = null
        var display1TextStyle: TextStyle? = null
        rule.setContent {
            MaterialTheme {
                localTextStyle = LocalTextStyle.current
                display1TextStyle = LocalTypography.current.body1
            }
        }

        assertThat(
            localTextStyle?.platformStyle?.paragraphStyle?.includeFontPadding
        ).isEqualTo(false)
        assertThat(
            display1TextStyle?.platformStyle?.paragraphStyle?.includeFontPadding
        ).isEqualTo(false)
    }

    @Test
    fun inheritsThemeTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
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

        rule.runOnIdle {
            assertThat(textColor).isEqualTo(ExpectedTextStyle.color)
            assertThat(textAlign).isEqualTo(ExpectedTextStyle.textAlign)
            assertThat(fontSize).isEqualTo(ExpectedTextStyle.fontSize)
            assertThat(fontStyle).isEqualTo(ExpectedTextStyle.fontStyle)
            assertThat(letterSpacing).isEqualTo(ExpectedTextStyle.letterSpacing)
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
        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
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

        rule.runOnIdle {
            assertThat(textColor).isEqualTo(testStyle.color)
            assertThat(textAlign).isEqualTo(testStyle.textAlign)
            assertThat(fontSize).isEqualTo(testStyle.fontSize)
            assertThat(fontStyle).isEqualTo(testStyle.fontStyle)
            assertThat(letterSpacing).isEqualTo(testStyle.letterSpacing)
        }
    }

    @Test
    fun settingParametersExplicitly() {
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Green
        val expectedTextAlign = TextAlign.Center
        val expectedFontSize = 16.sp
        val expectedFontStyle = FontStyle.Normal
        val expectedLetterSpacing = 0.6.em

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
            // explicit parameters should override values from the style.
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    // Not really an expected use-case, but we should ensure the behavior here is consistent.
    @Test
    fun settingColorAndTextStyle() {
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val expectedColor = Color.Green
        val expectedTextAlign = TextAlign.Center
        val expectedFontSize = 16.sp
        val expectedFontStyle = FontStyle.Normal
        val expectedLetterSpacing = 0.6.em
        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
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
            // explicit parameters should override values from the style.
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    @Test
    fun testSemantics() {
        rule.setContent {
            ProvideTextStyle(ExpectedTextStyle) {
                Box(Modifier.background(Color.White)) {
                    Text(
                        TestText,
                        modifier = Modifier.testTag("text")
                    )
                }
            }
        }

        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag("text")
            .assertTextEquals(TestText)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(textLayoutResults) }
        assert(textLayoutResults.size == 1) { "TextLayoutResult is null" }
    }

    @Test
    fun semantics_hasColor_providedByParameter() {
        val expectedColor = Color(0.7f, 0.13f, 1.0f, 0.323f)
        rule.setContent {
            Text(
                "Test",
                color = expectedColor
            )
        }

        rule.onNodeWithText("Test").assert(SemanticsMatcher("") {
            val textLayoutResult = ArrayList<TextLayoutResult>()
            it.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(
                textLayoutResult
            )
            val color = textLayoutResult.first().layoutInput.style.color
            color == expectedColor
        })
    }
}
