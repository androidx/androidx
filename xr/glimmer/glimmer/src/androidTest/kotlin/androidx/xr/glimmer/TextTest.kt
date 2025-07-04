/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextTest {

    @get:Rule val rule = createComposeRule()

    private val expectedTextStyle =
        TextStyle(
            color = Color.Blue,
            textAlign = TextAlign.End,
            fontSize = 32.sp,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.3.em,
        )

    private val testString = "TestText"
    private val testAnnotatedString = AnnotatedString(testString)

    @Test
    fun defaultTextStyle() {
        var localTextStyle: TextStyle? = null
        var bodySmallTextStyle: TextStyle? = null
        rule.setGlimmerThemeContent {
            localTextStyle = LocalTextStyle.current
            bodySmallTextStyle = GlimmerTheme.typography.bodySmall
        }
        assertThat(localTextStyle).isEqualTo(bodySmallTextStyle)
    }

    @Test
    fun usesLocalTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testString,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
        }

        rule.runOnIdle {
            assertThat(textColor).isEqualTo(expectedTextStyle.color)
            assertThat(textAlign).isEqualTo(expectedTextStyle.textAlign)
            assertThat(fontSize).isEqualTo(expectedTextStyle.fontSize)
            assertThat(fontStyle).isEqualTo(expectedTextStyle.fontStyle)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle.letterSpacing)
        }
    }

    @Test
    fun usesLocalTextStyle_annotatedString() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testAnnotatedString,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
        }

        rule.runOnIdle {
            assertThat(textColor).isEqualTo(expectedTextStyle.color)
            assertThat(textAlign).isEqualTo(expectedTextStyle.textAlign)
            assertThat(fontSize).isEqualTo(expectedTextStyle.fontSize)
            assertThat(fontStyle).isEqualTo(expectedTextStyle.fontStyle)
            assertThat(letterSpacing).isEqualTo(expectedTextStyle.letterSpacing)
        }
    }

    @Test
    fun usesContentColor() {
        // The way we set text color when using content color (using color producer) is not
        // applied to onTextLayout, so we test it by drawing a blue background behind the text, with
        // expected blue content color - so when we take a screenshot it should be fully blue.
        rule.setGlimmerThemeContent {
            Box(Modifier.fillMaxSize().background(Color.Blue).padding(20.dp)) {
                Text(
                    testString,
                    modifier =
                        Modifier.surface(
                                color = Color.Blue,
                                contentColor = Color.Blue,
                                border = null,
                            )
                            .testTag("test"),
                )
            }
        }

        rule.onNodeWithTag("test").captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun usesContentColor_annotatedString() {
        // The way we set text color when using content color (using color producer) is not
        // applied to onTextLayout, so we test it by drawing a blue background behind the text, with
        // expected blue content color - so when we take a screenshot it should be fully blue.
        rule.setGlimmerThemeContent {
            Box(Modifier.fillMaxSize().background(Color.Blue).padding(20.dp)) {
                Text(
                    testAnnotatedString,
                    modifier =
                        Modifier.surface(
                                color = Color.Blue,
                                contentColor = Color.Blue,
                                border = null,
                            )
                            .testTag("test"),
                )
            }
        }

        rule.onNodeWithTag("test").captureToImage().assertPixels { Color.Blue }
    }

    @Test
    fun settingCustomTextStyle_overridesLocalTextStyle() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val testStyle =
            TextStyle(
                color = Color.Green,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontStyle = FontStyle.Normal,
                letterSpacing = 0.6.em,
            )
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testString,
                    style = testStyle,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
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
    fun settingCustomTextStyle_overridesLocalTextStyle_annotatedString() {
        var textColor: Color? = null
        var textAlign: TextAlign? = null
        var fontSize: TextUnit? = null
        var fontStyle: FontStyle? = null
        var letterSpacing: TextUnit? = null
        val testStyle =
            TextStyle(
                color = Color.Green,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontStyle = FontStyle.Normal,
                letterSpacing = 0.6.em,
            )
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testAnnotatedString,
                    style = testStyle,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
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
    fun settingParametersExplicitly_overridesLocalTextStyle() {
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

        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testString,
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
                    },
                )
            }
        }

        rule.runOnIdle {
            // explicit parameters should override values from the style.
            assertThat(textColor).isEqualTo(expectedColor)
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    @Test
    fun settingParametersExplicitly_overridesLocalTextStyle_annotatedString() {
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

        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testAnnotatedString,
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
                    },
                )
            }
        }

        rule.runOnIdle {
            // explicit parameters should override values from the style.
            assertThat(textColor).isEqualTo(expectedColor)
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    // Not really an expected use-case, but we should ensure the behavior here is consistent.
    @Test
    fun settingColorAndTextStyle_colorOverridesStyle() {
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
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testString,
                    color = expectedColor,
                    textAlign = expectedTextAlign,
                    fontSize = expectedFontSize,
                    fontStyle = expectedFontStyle,
                    letterSpacing = expectedLetterSpacing,
                    style = expectedTextStyle,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
        }

        rule.runOnIdle {
            // explicit parameters should override values from the style.
            assertThat(textColor).isEqualTo(expectedColor)
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    // Not really an expected use-case, but we should ensure the behavior here is consistent.
    @Test
    fun settingColorAndTextStyle_colorOverridesStyle_annotatedString() {
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
        rule.setGlimmerThemeContent {
            CompositionLocalProvider(LocalTextStyle provides expectedTextStyle) {
                Text(
                    testAnnotatedString,
                    color = expectedColor,
                    textAlign = expectedTextAlign,
                    fontSize = expectedFontSize,
                    fontStyle = expectedFontStyle,
                    letterSpacing = expectedLetterSpacing,
                    style = expectedTextStyle,
                    onTextLayout = {
                        textColor = it.layoutInput.style.color
                        textAlign = it.layoutInput.style.textAlign
                        fontSize = it.layoutInput.style.fontSize
                        fontStyle = it.layoutInput.style.fontStyle
                        letterSpacing = it.layoutInput.style.letterSpacing
                    },
                )
            }
        }

        rule.runOnIdle {
            // explicit parameters should override values from the style.
            assertThat(textColor).isEqualTo(expectedColor)
            assertThat(textAlign).isEqualTo(expectedTextAlign)
            assertThat(fontSize).isEqualTo(expectedFontSize)
            assertThat(fontStyle).isEqualTo(expectedFontStyle)
            assertThat(letterSpacing).isEqualTo(expectedLetterSpacing)
        }
    }

    @Test
    fun testSemantics() {
        rule.setGlimmerThemeContent { Text(testString, modifier = Modifier.testTag("text")) }

        val textLayoutResults = getTextLayoutResults("text")
        assert(textLayoutResults != null) { "TextLayoutResult is null" }
    }

    @Test
    fun testSemantics_annotatedString() {
        rule.setGlimmerThemeContent {
            Text(testAnnotatedString, modifier = Modifier.testTag("text"))
        }

        val textLayoutResults = getTextLayoutResults("text")
        assert(textLayoutResults != null) { "TextLayoutResult is null" }
    }

    @Test
    fun testContentColorChangeVisibleInSemantics() {
        var switchColor by mutableStateOf(false)
        rule.setGlimmerThemeContent {
            val contentColor =
                if (switchColor) {
                    GlimmerTheme.colors.primary
                } else {
                    GlimmerTheme.colors.secondary
                }
            Text(
                testString,
                modifier = Modifier.surface(contentColor = contentColor).testTag("text"),
            )
        }

        val textLayoutResults = getTextLayoutResults("text")
        switchColor = true
        rule.waitForIdle()
        val textLayoutResults2 = getTextLayoutResults("text")

        assertThat(textLayoutResults2?.layoutInput?.style?.color).isNotNull()
        assertThat(textLayoutResults2?.layoutInput?.style?.color)
            .isNotEqualTo(textLayoutResults?.layoutInput?.style?.color)
    }

    @Test
    fun testContentColorChangeVisibleInSemantics_annotatedString() {
        var switchColor by mutableStateOf(false)
        rule.setGlimmerThemeContent {
            val contentColor =
                if (switchColor) {
                    GlimmerTheme.colors.primary
                } else {
                    GlimmerTheme.colors.secondary
                }
            Text(
                testAnnotatedString,
                modifier = Modifier.surface(contentColor = contentColor).testTag("text"),
            )
        }

        val textLayoutResults = getTextLayoutResults("text")
        switchColor = true
        rule.waitForIdle()
        val textLayoutResults2 = getTextLayoutResults("text")

        assertThat(textLayoutResults2?.layoutInput?.style?.color).isNotNull()
        assertThat(textLayoutResults2?.layoutInput?.style?.color)
            .isNotEqualTo(textLayoutResults?.layoutInput?.style?.color)
    }

    @Test
    fun semantics_hasColor_providedByParameter() {
        val expectedColor = Color(0.7f, 0.13f, 1.0f, 0.323f)
        rule.setContent {
            Text(testString, color = expectedColor, modifier = Modifier.testTag("text"))
        }

        rule
            .onNodeWithTag("text")
            .assert(
                SemanticsMatcher("") {
                    val textLayoutResult = ArrayList<TextLayoutResult>()
                    it.config
                        .getOrNull(SemanticsActions.GetTextLayoutResult)
                        ?.action
                        ?.invoke(textLayoutResult)
                    val color = textLayoutResult.first().layoutInput.style.color
                    color == expectedColor
                }
            )
    }

    @Test
    fun semantics_hasColor_providedByParameter_annotatedString() {
        val expectedColor = Color(0.7f, 0.13f, 1.0f, 0.323f)
        rule.setContent {
            Text(testAnnotatedString, color = expectedColor, modifier = Modifier.testTag("text"))
        }

        rule
            .onNodeWithTag("text")
            .assert(
                SemanticsMatcher("") {
                    val textLayoutResult = ArrayList<TextLayoutResult>()
                    it.config
                        .getOrNull(SemanticsActions.GetTextLayoutResult)
                        ?.action
                        ?.invoke(textLayoutResult)
                    val color = textLayoutResult.first().layoutInput.style.color
                    color == expectedColor
                }
            )
    }

    @Test
    fun testAutoSize_changesTextSize() {
        rule.setGlimmerThemeContent {
            Box(Modifier.size(200.dp)) {
                Text(
                    text = "a b c d e",
                    modifier = Modifier.testTag("TEXT"),
                    autoSize = TextAutoSize.StepBased(),
                )
            }
        }

        val bounds = rule.onNodeWithTag("TEXT").getBoundsInRoot()

        // Text tries to fill entire size of parent
        // (Not exact due to discrete allowed text sizes)
        bounds.height.assertIsEqualTo(200.dp, tolerance = 20.dp)
        bounds.width.assertIsEqualTo(200.dp, tolerance = 20.dp)
    }

    @Test
    fun testAutoSize_changesTextSize_annotatedString() {
        rule.setGlimmerThemeContent {
            Box(Modifier.size(200.dp)) {
                Text(
                    text = AnnotatedString("a b c d e"),
                    modifier = Modifier.testTag("TEXT"),
                    autoSize = TextAutoSize.StepBased(),
                )
            }
        }

        val bounds = rule.onNodeWithTag("TEXT").getBoundsInRoot()

        // Text tries to fill entire size of parent
        // (Not exact due to discrete allowed text sizes)
        bounds.height.assertIsEqualTo(200.dp, tolerance = 20.dp)
        bounds.width.assertIsEqualTo(200.dp, tolerance = 20.dp)
    }

    private fun getTextLayoutResults(tag: String): TextLayoutResult? {
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(textLayoutResults)
        }
        return textLayoutResults.firstOrNull()
    }
}
