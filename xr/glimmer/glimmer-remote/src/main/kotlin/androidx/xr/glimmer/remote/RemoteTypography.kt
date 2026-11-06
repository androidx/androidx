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

package androidx.xr.glimmer.remote

import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.fromTextStyle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The Remote Compose Glimmer type scale includes a range of contrasting styles that support the
 * needs of your product and its content.
 *
 * @property titleLarge titleLarge is the largest title, and is typically reserved for emphasized
 *   text that is shorter in length.
 * @property titleMedium titleMedium is the second largest title, and is typically reserved for
 *   emphasized text that is shorter in length.
 * @property titleSmall titleSmall is the smallest title, and is typically reserved for emphasized
 *   text that is shorter in length.
 * @property bodyLarge bodyLarge is the largest body, and is typically used for long-form writing as
 *   it works well for small text sizes.
 * @property bodyMedium bodyMedium is the second largest body, and is typically used for long-form
 *   writing as it works well for small text sizes.
 * @property bodySmall bodySmall is the smallest body, and is typically used for long-form writing
 *   as it works well for small text sizes.
 * @property caption caption is the smallest text style, and should be used sparingly for use-cases
 *   where text is not essential to the user experience.
 */
@Immutable
public class RemoteTypography
private constructor(
    public val titleLarge: RemoteTextStyle,
    public val titleMedium: RemoteTextStyle,
    public val titleSmall: RemoteTextStyle,
    public val bodyLarge: RemoteTextStyle,
    public val bodyMedium: RemoteTextStyle,
    public val bodySmall: RemoteTextStyle,
    public val caption: RemoteTextStyle,
) {

    /**
     * Creates a Remote Compose Glimmer type scale.
     *
     * @param defaultFontFamily the default [RemoteFontFamily] to be used for [RemoteTextStyle]s
     *   provided in this constructor. This default will be used if the [RemoteFontFamily] on the
     *   [RemoteTextStyle] is `null`.
     * @param titleLarge titleLarge is the largest title, and is typically reserved for emphasized
     *   text that is shorter in length.
     * @param titleMedium titleMedium is the second largest title, and is typically reserved for
     *   emphasized text that is shorter in length.
     * @param titleSmall titleSmall is the smallest title, and is typically reserved for emphasized
     *   text that is shorter in length.
     * @param bodyLarge bodyLarge is the largest body, and is typically used for long-form writing
     *   as it works well for small text sizes.
     * @param bodyMedium bodyMedium is the second largest body, and is typically used for long-form
     *   writing as it works well for small text sizes.
     * @param bodySmall bodySmall is the smallest body, and is typically used for long-form writing
     *   as it works well for small text sizes.
     * @param caption caption is the smallest text style, and should be used sparingly for use-cases
     *   where it is not essential to the user experience.
     */
    public constructor(
        defaultFontFamily: RemoteFontFamily? = null,
        titleLarge: RemoteTextStyle = RemoteTypographyDefaults.TitleLarge,
        titleMedium: RemoteTextStyle = RemoteTypographyDefaults.TitleMedium,
        titleSmall: RemoteTextStyle = RemoteTypographyDefaults.TitleSmall,
        bodyLarge: RemoteTextStyle = RemoteTypographyDefaults.BodyLarge,
        bodyMedium: RemoteTextStyle = RemoteTypographyDefaults.BodyMedium,
        bodySmall: RemoteTextStyle = RemoteTypographyDefaults.BodySmall,
        caption: RemoteTextStyle = RemoteTypographyDefaults.Caption,
    ) : this(
        titleLarge = titleLarge.withDefaultFontFamily(defaultFontFamily),
        titleMedium = titleMedium.withDefaultFontFamily(defaultFontFamily),
        titleSmall = titleSmall.withDefaultFontFamily(defaultFontFamily),
        bodyLarge = bodyLarge.withDefaultFontFamily(defaultFontFamily),
        bodyMedium = bodyMedium.withDefaultFontFamily(defaultFontFamily),
        bodySmall = bodySmall.withDefaultFontFamily(defaultFontFamily),
        caption = caption.withDefaultFontFamily(defaultFontFamily),
    )

    /** Returns a copy of this RemoteTypography, optionally overriding some of the values. */
    public fun copy(
        titleLarge: RemoteTextStyle = this.titleLarge,
        titleMedium: RemoteTextStyle = this.titleMedium,
        titleSmall: RemoteTextStyle = this.titleSmall,
        bodyLarge: RemoteTextStyle = this.bodyLarge,
        bodyMedium: RemoteTextStyle = this.bodyMedium,
        bodySmall: RemoteTextStyle = this.bodySmall,
        caption: RemoteTextStyle = this.caption,
    ): RemoteTypography =
        RemoteTypography(
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
            caption = caption,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteTypography) return false

        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false
        if (caption != other.caption) return false

        return true
    }

    override fun hashCode(): Int {
        var result = titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        result = 31 * result + caption.hashCode()
        return result
    }

    override fun toString(): String {
        return "RemoteTypography(titleLarge=$titleLarge, titleMedium=$titleMedium, titleSmall=$titleSmall, bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, bodySmall=$bodySmall, caption=$caption)"
    }
}

/** Contains the default style values used by [RemoteTypography]. */
public object RemoteTypographyDefaults {

    /** Default [RemoteTextStyle] for [RemoteTypography.titleLarge]. */
    public val TitleLarge: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(750),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.titleMedium]. */
    public val TitleMedium: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(750),
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.titleSmall]. */
    public val TitleSmall: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(750),
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.bodyLarge]. */
    public val BodyLarge: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(520),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.bodyMedium]. */
    public val BodyMedium: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(520),
                fontSize = 24.sp,
                lineHeight = 36.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.bodySmall]. */
    public val BodySmall: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(520),
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )

    /** Default [RemoteTextStyle] for [RemoteTypography.caption]. */
    public val Caption: RemoteTextStyle =
        RemoteTextStyle.fromTextStyle(
            TextStyle(
                fontWeight = FontWeight(650),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = DefaultLetterSpacing,
            )
        )
}

private val DefaultLetterSpacing = 0.sp

/**
 * Uses [default] as the [RemoteFontFamily] for [this] [RemoteTextStyle] if no [RemoteFontFamily] is
 * set on the text style. If no [RemoteFontFamily] is set and [default] is null, the player default
 * font family will end up being used when the text is rendered.
 */
private fun RemoteTextStyle.withDefaultFontFamily(default: RemoteFontFamily?): RemoteTextStyle {
    return if (default == null || fontFamily != null) this else copy(fontFamily = default)
}
