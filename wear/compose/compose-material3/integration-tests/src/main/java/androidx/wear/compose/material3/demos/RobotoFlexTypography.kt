/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.wear.compose.material3.demos

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.toFontFamily
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.tokens.TypographyVariableFontsTokens

@OptIn(ExperimentalTextApi::class)
fun createRobotoFlexTextStyle(style: TextStyle, variationSettings: FontVariation.Settings) =
    style.copy(
        fontFamily =
            Font(R.font.robotoflex_variable, variationSettings = variationSettings).toFontFamily()
    )

@OptIn(ExperimentalTextApi::class)
fun createRobotoFlexCurvedTextStyle(
    style: CurvedTextStyle,
    variationSettings: FontVariation.Settings
) =
    style.copy(
        fontFamily =
            Font(R.font.robotoflex_variable, variationSettings = variationSettings).toFontFamily()
    )

val DefaultTypography = Typography()

val RobotoFlexTypography =
    Typography(
        arcLarge =
            createRobotoFlexCurvedTextStyle(
                DefaultTypography.arcLarge,
                TypographyVariableFontsTokens.ArcLargeVariationSettings
            ),
        arcMedium =
            createRobotoFlexCurvedTextStyle(
                DefaultTypography.arcMedium,
                TypographyVariableFontsTokens.ArcMediumVariationSettings
            ),
        arcSmall =
            createRobotoFlexCurvedTextStyle(
                DefaultTypography.arcSmall,
                TypographyVariableFontsTokens.ArcSmallVariationSettings
            ),
        bodyExtraSmall =
            createRobotoFlexTextStyle(
                DefaultTypography.bodyExtraSmall,
                TypographyVariableFontsTokens.BodyExtraSmallVariationSettings
            ),
        bodySmall =
            createRobotoFlexTextStyle(
                DefaultTypography.bodySmall,
                TypographyVariableFontsTokens.BodySmallVariationSettings
            ),
        bodyMedium =
            createRobotoFlexTextStyle(
                DefaultTypography.bodyMedium,
                TypographyVariableFontsTokens.BodyMediumVariationSettings
            ),
        bodyLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.bodyLarge,
                TypographyVariableFontsTokens.BodyLargeVariationSettings
            ),
        displaySmall =
            createRobotoFlexTextStyle(
                DefaultTypography.displaySmall,
                TypographyVariableFontsTokens.DisplaySmallVariationSettings
            ),
        displayMedium =
            createRobotoFlexTextStyle(
                DefaultTypography.displayMedium,
                TypographyVariableFontsTokens.DisplayMediumVariationSettings
            ),
        displayLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.displayLarge,
                TypographyVariableFontsTokens.DisplayLargeVariationSettings
            ),
        labelSmall =
            createRobotoFlexTextStyle(
                DefaultTypography.labelSmall,
                TypographyVariableFontsTokens.LabelSmallVariationSettings
            ),
        labelMedium =
            createRobotoFlexTextStyle(
                DefaultTypography.labelMedium,
                TypographyVariableFontsTokens.LabelMediumVariationSettings
            ),
        labelLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.labelLarge,
                TypographyVariableFontsTokens.LabelLargeVariationSettings
            ),
        titleSmall =
            createRobotoFlexTextStyle(
                DefaultTypography.titleSmall,
                TypographyVariableFontsTokens.TitleSmallVariationSettings
            ),
        titleMedium =
            createRobotoFlexTextStyle(
                DefaultTypography.titleMedium,
                TypographyVariableFontsTokens.TitleMediumVariationSettings
            ),
        titleLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.titleLarge,
                TypographyVariableFontsTokens.TitleLargeVariationSettings
            ),
        numeralExtraSmall =
            createRobotoFlexTextStyle(
                DefaultTypography.numeralExtraSmall,
                TypographyVariableFontsTokens.NumeralExtraSmallVariationSettings
            ),
        numeralSmall =
            createRobotoFlexTextStyle(
                DefaultTypography.numeralSmall,
                TypographyVariableFontsTokens.NumeralSmallVariationSettings
            ),
        numeralMedium =
            createRobotoFlexTextStyle(
                DefaultTypography.numeralMedium,
                TypographyVariableFontsTokens.NumeralMediumVariationSettings
            ),
        numeralLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.numeralLarge,
                TypographyVariableFontsTokens.NumeralLargeVariationSettings
            ),
        numeralExtraLarge =
            createRobotoFlexTextStyle(
                DefaultTypography.numeralExtraLarge,
                TypographyVariableFontsTokens.NumeralExtraLargeVariationSettings
            ),
    )
