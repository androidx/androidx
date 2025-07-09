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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.material3.tokens.TypographyVariableFontsTokens
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@MediumTest
@RunWith(TestParameterInjector::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalTextApi::class)
class TypographyScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_PATH)

    @get:Rule val testName = TestName()

    enum class TextFont {
        ROBOTO,
        ROBOTO_FLEX,
    }

    @Test
    fun arc_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            CurvedFontText(
                text = "ArcLarge123",
                font = font,
                style = MaterialTheme.typography.arcLarge,
                variationSettings = TypographyVariableFontsTokens.ArcLargeVariationSettings,
            )
        }
    }

    @Test
    fun arc_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            CurvedFontText(
                text = "ArcMedium123",
                font = font,
                style = MaterialTheme.typography.arcMedium,
                variationSettings = TypographyVariableFontsTokens.ArcMediumVariationSettings,
            )
        }
    }

    @Test
    fun arc_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            CurvedFontText(
                text = "ArcSmall123",
                font = font,
                style = MaterialTheme.typography.arcSmall,
                variationSettings = TypographyVariableFontsTokens.ArcSmallVariationSettings,
            )
        }
    }

    @Test
    fun body_extra_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "BodyExtraSmall",
                font = font,
                style = MaterialTheme.typography.bodyExtraSmall,
                variationSettings = TypographyVariableFontsTokens.BodyExtraSmallVariationSettings,
            )
        }
    }

    @Test
    fun body_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "BodySmall",
                font = font,
                style = MaterialTheme.typography.bodySmall,
                variationSettings = TypographyVariableFontsTokens.BodySmallVariationSettings,
            )
        }
    }

    @Test
    fun body_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "BodyMedium",
                font = font,
                style = MaterialTheme.typography.bodyMedium,
                variationSettings = TypographyVariableFontsTokens.BodyMediumVariationSettings,
            )
        }
    }

    @Test
    fun body_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "BodyLarge",
                font = font,
                style = MaterialTheme.typography.bodyLarge,
                variationSettings = TypographyVariableFontsTokens.BodyLargeVariationSettings,
            )
        }
    }

    @Test
    fun display_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "DisplaySmall",
                font = font,
                style = MaterialTheme.typography.displaySmall,
                variationSettings = TypographyVariableFontsTokens.DisplaySmallVariationSettings,
            )
        }
    }

    @Test
    fun display_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "DisplayMedium",
                font = font,
                style = MaterialTheme.typography.displayMedium,
                variationSettings = TypographyVariableFontsTokens.DisplayMediumVariationSettings,
            )
        }
    }

    @Test
    fun display_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "DisplayLarge",
                font = font,
                style = MaterialTheme.typography.displayLarge,
                variationSettings = TypographyVariableFontsTokens.DisplayLargeVariationSettings,
            )
        }
    }

    @Test
    fun label_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "LabelSmall",
                font = font,
                style = MaterialTheme.typography.labelSmall,
                variationSettings = TypographyVariableFontsTokens.LabelSmallVariationSettings,
            )
        }
    }

    @Test
    fun label_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "LabelMedium",
                font = font,
                style = MaterialTheme.typography.labelMedium,
                variationSettings = TypographyVariableFontsTokens.LabelMediumVariationSettings,
            )
        }
    }

    @Test
    fun label_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "LabelLarge",
                font = font,
                style = MaterialTheme.typography.labelLarge,
                variationSettings = TypographyVariableFontsTokens.LabelLargeVariationSettings,
            )
        }
    }

    @Test
    fun title_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "TitleSmall",
                font = font,
                style = MaterialTheme.typography.titleSmall,
                variationSettings = TypographyVariableFontsTokens.TitleSmallVariationSettings,
            )
        }
    }

    @Test
    fun title_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "TitleMedium",
                font = font,
                style = MaterialTheme.typography.titleMedium,
                variationSettings = TypographyVariableFontsTokens.TitleMediumVariationSettings,
            )
        }
    }

    @Test
    fun title_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "TitleLarge",
                font = font,
                style = MaterialTheme.typography.titleLarge,
                variationSettings = TypographyVariableFontsTokens.TitleLargeVariationSettings,
            )
        }
    }

    @Test
    fun numeral_extra_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "NumeralExtraSmall",
                font = font,
                style = MaterialTheme.typography.numeralExtraSmall,
                variationSettings = TypographyVariableFontsTokens.NumeralExtraSmallVariationSettings,
            )
        }
    }

    @Test
    fun numeral_small_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "NumeralSmall123",
                font = font,
                style = MaterialTheme.typography.numeralSmall,
                variationSettings = TypographyVariableFontsTokens.NumeralSmallVariationSettings,
            )
        }
    }

    @Test
    fun numeral_medium_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "NumeralMedium123",
                font = font,
                style = MaterialTheme.typography.numeralMedium,
                variationSettings = TypographyVariableFontsTokens.NumeralMediumVariationSettings,
            )
        }
    }

    @Test
    fun numeral_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "NumeralLarge123",
                font = font,
                style = MaterialTheme.typography.numeralLarge,
                variationSettings = TypographyVariableFontsTokens.NumeralLargeVariationSettings,
            )
        }
    }

    @Test
    fun numeral_extra_large_font_test(@TestParameter font: TextFont) {
        verifyTypographyScreenshot {
            FontText(
                text = "NumeralExtraLarge123",
                font = font,
                style = MaterialTheme.typography.numeralExtraLarge,
                variationSettings = TypographyVariableFontsTokens.NumeralExtraLargeVariationSettings,
            )
        }
    }

    @Composable
    private fun FontText(
        text: String,
        font: TextFont,
        style: TextStyle,
        variationSettings: FontVariation.Settings,
    ) {
        ScreenConfiguration(screenSizeDp = SCREEN_SIZE_LARGE) {
            Box(
                modifier = Modifier.size(SCREEN_SIZE_LARGE.dp).testTag(TEST_TAG),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = text, style = getTextStyle(font, style, variationSettings))
            }
        }
    }

    @Composable
    private fun CurvedFontText(
        text: String,
        font: TextFont,
        style: CurvedTextStyle,
        variationSettings: FontVariation.Settings,
    ) {
        ScreenConfiguration(screenSizeDp = SCREEN_SIZE_LARGE) {
            CurvedLayout(Modifier.size(SCREEN_SIZE_LARGE.dp).testTag(TEST_TAG)) {
                curvedRow {
                    curvedText(
                        text = text,
                        style = getCurvedTextStyle(font, style, variationSettings),
                    )
                }
            }
        }
    }

    private fun getTextStyle(
        font: TextFont,
        style: TextStyle,
        variationSettings: FontVariation.Settings,
    ) =
        when (font) {
            TextFont.ROBOTO ->
                style.copy(
                    fontFamily =
                        createRobotoFontFamily(
                            fontWeight = style.fontWeight ?: FontWeight.Normal,
                            variationSettings = variationSettings,
                        )
                )
            TextFont.ROBOTO_FLEX ->
                style.copy(
                    fontFamily =
                        createRobotoFlexFontFamily(
                            fontWeight = style.fontWeight ?: FontWeight.Normal,
                            variationSettings = variationSettings,
                        )
                )
        }

    private fun getCurvedTextStyle(
        font: TextFont,
        style: CurvedTextStyle,
        variationSettings: FontVariation.Settings,
    ) =
        when (font) {
            TextFont.ROBOTO ->
                style.copy(
                    fontFamily =
                        createRobotoFontFamily(
                            fontWeight = style.fontWeight ?: FontWeight.Normal,
                            variationSettings = variationSettings,
                        )
                )
            TextFont.ROBOTO_FLEX ->
                style.copy(
                    fontFamily =
                        createRobotoFlexFontFamily(
                            fontWeight = style.fontWeight ?: FontWeight.Normal,
                            variationSettings = variationSettings,
                        )
                )
        }

    private fun createRobotoFontFamily(
        fontWeight: FontWeight,
        variationSettings: FontVariation.Settings,
    ) =
        Font(
                familyName = DeviceFontFamilyName(FontFamily.SansSerif.name),
                weight = fontWeight,
                variationSettings = variationSettings,
            )
            .toFontFamily()

    private fun createRobotoFlexFontFamily(
        fontWeight: FontWeight,
        variationSettings: FontVariation.Settings,
    ) =
        Font(
                resId = getFontId("robotoflex_variable"),
                weight = fontWeight,
                variationSettings = variationSettings,
            )
            .toFontFamily()

    private fun getFontId(fontFileName: String): Int {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.resources.getIdentifier(fontFileName, "font", context.packageName)
    }

    private fun verifyTypographyScreenshot(content: @Composable () -> Unit) {
        rule.verifyScreenshot(
            testName = testName,
            screenshotRule = screenshotRule,
            content = { content() },
        )
    }
}
