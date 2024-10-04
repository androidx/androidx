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
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.tokens.TypographyVariableFontsTokens

@OptIn(ExperimentalTextApi::class)
fun createRobotoFlexTextStyle(variationSettings: FontVariation.Settings) =
    TextStyle(
        fontFamily =
            Font(R.font.robotoflex_variable, variationSettings = variationSettings).toFontFamily()
    )

val RobotoFlexTypography =
    Typography(
        arcLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.ArcLargeVariationSettings),
        arcMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.ArcMediumVariationSettings),
        arcSmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.ArcSmallVariationSettings),
        bodyExtraSmall =
            createRobotoFlexTextStyle(
                TypographyVariableFontsTokens.BodyExtraSmallVariationSettings
            ),
        bodySmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.BodySmallVariationSettings),
        bodyMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.BodyMediumVariationSettings),
        bodyLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.BodyLargeVariationSettings),
        displaySmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.DisplaySmallVariationSettings),
        displayMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.DisplayMediumVariationSettings),
        displayLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.DisplayLargeVariationSettings),
        labelSmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.LabelSmallVariationSettings),
        labelMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.LabelMediumVariationSettings),
        labelLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.LabelLargeVariationSettings),
        titleSmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.TitleSmallVariationSettings),
        titleMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.TitleMediumVariationSettings),
        titleLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.TitleLargeVariationSettings),
        numeralExtraSmall =
            createRobotoFlexTextStyle(
                TypographyVariableFontsTokens.NumeralExtraSmallVariationSettings
            ),
        numeralSmall =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.NumeralSmallVariationSettings),
        numeralMedium =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.NumeralMediumVariationSettings),
        numeralLarge =
            createRobotoFlexTextStyle(TypographyVariableFontsTokens.NumeralLargeVariationSettings),
        numeralExtraLarge =
            createRobotoFlexTextStyle(
                TypographyVariableFontsTokens.NumeralExtraLargeVariationSettings
            ),
    )
