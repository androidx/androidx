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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material3.Typography

/** Class holding typography definitions for [RemoteMaterialTheme]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteTypography(
    public val displayLarge: RemoteTextStyle,
    public val displayMedium: RemoteTextStyle,
    public val displaySmall: RemoteTextStyle,
    public val titleLarge: RemoteTextStyle,
    public val titleMedium: RemoteTextStyle,
    public val titleSmall: RemoteTextStyle,
    public val labelLarge: RemoteTextStyle,
    public val labelMedium: RemoteTextStyle,
    public val labelSmall: RemoteTextStyle,
    public val bodyLarge: RemoteTextStyle,
    public val bodyMedium: RemoteTextStyle,
    public val bodySmall: RemoteTextStyle,
    public val bodyExtraSmall: RemoteTextStyle,
    public val numeralExtraLarge: RemoteTextStyle,
    public val numeralLarge: RemoteTextStyle,
    public val numeralMedium: RemoteTextStyle,
    public val numeralSmall: RemoteTextStyle,
    public val numeralExtraSmall: RemoteTextStyle,
) {
    public constructor(
        defaultFontFamily: FontFamily = FontFamily.Default,
        displayLarge: RemoteTextStyle = RemoteTypographyTokens.DisplayLarge,
        displayMedium: RemoteTextStyle = RemoteTypographyTokens.DisplayMedium,
        displaySmall: RemoteTextStyle = RemoteTypographyTokens.DisplaySmall,
        titleLarge: RemoteTextStyle = RemoteTypographyTokens.TitleLarge,
        titleMedium: RemoteTextStyle = RemoteTypographyTokens.TitleMedium,
        titleSmall: RemoteTextStyle = RemoteTypographyTokens.TitleSmall,
        labelLarge: RemoteTextStyle = RemoteTypographyTokens.LabelLarge,
        labelMedium: RemoteTextStyle = RemoteTypographyTokens.LabelMedium,
        labelSmall: RemoteTextStyle = RemoteTypographyTokens.LabelSmall,
        bodyLarge: RemoteTextStyle = RemoteTypographyTokens.BodyLarge,
        bodyMedium: RemoteTextStyle = RemoteTypographyTokens.BodyMedium,
        bodySmall: RemoteTextStyle = RemoteTypographyTokens.BodySmall,
        bodyExtraSmall: RemoteTextStyle = RemoteTypographyTokens.BodyExtraSmall,
        numeralExtraLarge: RemoteTextStyle = RemoteTypographyTokens.NumeralExtraLarge,
        numeralLarge: RemoteTextStyle = RemoteTypographyTokens.NumeralLarge,
        numeralMedium: RemoteTextStyle = RemoteTypographyTokens.NumeralMedium,
        numeralSmall: RemoteTextStyle = RemoteTypographyTokens.NumeralSmall,
        numeralExtraSmall: RemoteTextStyle = RemoteTypographyTokens.NumeralExtraSmall,
    ) : this(
        displayLarge = displayLarge.withDefaultFontFamily(defaultFontFamily),
        displayMedium = displayMedium.withDefaultFontFamily(defaultFontFamily),
        displaySmall = displaySmall.withDefaultFontFamily(defaultFontFamily),
        titleLarge = titleLarge.withDefaultFontFamily(defaultFontFamily),
        titleMedium = titleMedium.withDefaultFontFamily(defaultFontFamily),
        titleSmall = titleSmall.withDefaultFontFamily(defaultFontFamily),
        labelLarge = labelLarge.withDefaultFontFamily(defaultFontFamily),
        labelMedium = labelMedium.withDefaultFontFamily(defaultFontFamily),
        labelSmall = labelSmall.withDefaultFontFamily(defaultFontFamily),
        bodyLarge = bodyLarge.withDefaultFontFamily(defaultFontFamily),
        bodyMedium = bodyMedium.withDefaultFontFamily(defaultFontFamily),
        bodySmall = bodySmall.withDefaultFontFamily(defaultFontFamily),
        bodyExtraSmall = bodyExtraSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraLarge = numeralExtraLarge.withDefaultFontFamily(defaultFontFamily),
        numeralLarge = numeralLarge.withDefaultFontFamily(defaultFontFamily),
        numeralMedium = numeralMedium.withDefaultFontFamily(defaultFontFamily),
        numeralSmall = numeralSmall.withDefaultFontFamily(defaultFontFamily),
        numeralExtraSmall = numeralExtraSmall.withDefaultFontFamily(defaultFontFamily),
    )

    public constructor(typography: Typography) : this() {
        RemoteTypography(
            displayLarge = RemoteTextStyle.fromTextStyle(typography.displayLarge),
            displayMedium = RemoteTextStyle.fromTextStyle(typography.displayMedium),
            displaySmall = RemoteTextStyle.fromTextStyle(typography.displaySmall),
            titleLarge = RemoteTextStyle.fromTextStyle(typography.titleLarge),
            titleMedium = RemoteTextStyle.fromTextStyle(typography.titleMedium),
            titleSmall = RemoteTextStyle.fromTextStyle(typography.titleSmall),
            labelLarge = RemoteTextStyle.fromTextStyle(typography.labelLarge),
            labelMedium = RemoteTextStyle.fromTextStyle(typography.labelMedium),
            labelSmall = RemoteTextStyle.fromTextStyle(typography.labelSmall),
            bodyLarge = RemoteTextStyle.fromTextStyle(typography.bodyLarge),
            bodyMedium = RemoteTextStyle.fromTextStyle(typography.bodyMedium),
            bodySmall = RemoteTextStyle.fromTextStyle(typography.bodySmall),
            bodyExtraSmall = RemoteTextStyle.fromTextStyle(typography.bodyExtraSmall),
            numeralExtraLarge = RemoteTextStyle.fromTextStyle(typography.numeralExtraLarge),
            numeralLarge = RemoteTextStyle.fromTextStyle(typography.numeralLarge),
            numeralMedium = RemoteTextStyle.fromTextStyle(typography.numeralMedium),
            numeralSmall = RemoteTextStyle.fromTextStyle(typography.numeralSmall),
            numeralExtraSmall = RemoteTextStyle.fromTextStyle(typography.numeralExtraSmall),
        )
    }

    public fun copy(
        displayLarge: RemoteTextStyle = this.displayLarge,
        displayMedium: RemoteTextStyle = this.displayMedium,
        displaySmall: RemoteTextStyle = this.displaySmall,
        titleLarge: RemoteTextStyle = this.titleLarge,
        titleMedium: RemoteTextStyle = this.titleMedium,
        titleSmall: RemoteTextStyle = this.titleSmall,
        labelLarge: RemoteTextStyle = this.labelLarge,
        labelMedium: RemoteTextStyle = this.labelMedium,
        labelSmall: RemoteTextStyle = this.labelSmall,
        bodyLarge: RemoteTextStyle = this.bodyLarge,
        bodyMedium: RemoteTextStyle = this.bodyMedium,
        bodySmall: RemoteTextStyle = this.bodySmall,
        bodyExtraSmall: RemoteTextStyle = this.bodyExtraSmall,
        numeralExtraLarge: RemoteTextStyle = this.numeralExtraLarge,
        numeralLarge: RemoteTextStyle = this.numeralLarge,
        numeralMedium: RemoteTextStyle = this.numeralMedium,
        numeralSmall: RemoteTextStyle = this.numeralSmall,
        numeralExtraSmall: RemoteTextStyle = this.numeralExtraSmall,
    ): RemoteTypography =
        RemoteTypography(
            displayLarge = displayLarge,
            displayMedium = displayMedium,
            displaySmall = displaySmall,
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            labelLarge = labelLarge,
            labelMedium = labelMedium,
            labelSmall = labelSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            bodyExtraSmall = bodyExtraSmall,
            numeralExtraLarge = numeralExtraLarge,
            numeralLarge = numeralLarge,
            numeralMedium = numeralMedium,
            numeralSmall = numeralSmall,
            numeralExtraSmall = numeralExtraSmall,
        )
}

/**
 * @return [this] if there is a [FontFamily] defined, otherwise copies [this] with [default] as the
 *   [FontFamily].
 */
private fun RemoteTextStyle.withDefaultFontFamily(default: FontFamily): RemoteTextStyle {
    return if (default == FontFamily.Default && fontFamily != null) this
    else copy(fontFamily = default)
}
