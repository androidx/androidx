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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The Glimmer type scale includes a range of contrasting styles that support the needs of your
 * product and its content.
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
 */
@Immutable
public class Typography
private constructor(
    public val titleLarge: TextStyle,
    public val titleMedium: TextStyle,
    public val titleSmall: TextStyle,
    public val bodyLarge: TextStyle,
    public val bodyMedium: TextStyle,
    public val bodySmall: TextStyle,
) {

    /**
     * Creates a Glimmer type scale.
     *
     * @param defaultFontFamily the default [FontFamily] to be used for [TextStyle]s provided in
     *   this constructor. This default will be used if the [FontFamily] on the [TextStyle] is
     *   `null`.
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
     */
    public constructor(
        defaultFontFamily: FontFamily? = null,
        titleLarge: TextStyle = TitleLarge,
        titleMedium: TextStyle = TitleMedium,
        titleSmall: TextStyle = TitleSmall,
        bodyLarge: TextStyle = BodyLarge,
        bodyMedium: TextStyle = BodyMedium,
        bodySmall: TextStyle = BodySmall,
    ) : this(
        titleLarge = titleLarge.withDefaultFontFamily(defaultFontFamily),
        titleMedium = titleMedium.withDefaultFontFamily(defaultFontFamily),
        titleSmall = titleSmall.withDefaultFontFamily(defaultFontFamily),
        bodyLarge = bodyLarge.withDefaultFontFamily(defaultFontFamily),
        bodyMedium = bodyMedium.withDefaultFontFamily(defaultFontFamily),
        bodySmall = bodySmall.withDefaultFontFamily(defaultFontFamily),
    )

    /** Returns a copy of this Typography, optionally overriding some of the values. */
    public fun copy(
        titleLarge: TextStyle = this.titleLarge,
        titleMedium: TextStyle = this.titleMedium,
        titleSmall: TextStyle = this.titleSmall,
        bodyLarge: TextStyle = this.bodyLarge,
        bodyMedium: TextStyle = this.bodyMedium,
        bodySmall: TextStyle = this.bodySmall,
    ): Typography =
        Typography(
            titleLarge = titleLarge,
            titleMedium = titleMedium,
            titleSmall = titleSmall,
            bodyLarge = bodyLarge,
            bodyMedium = bodyMedium,
            bodySmall = bodySmall,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Typography) return false

        if (titleLarge != other.titleLarge) return false
        if (titleMedium != other.titleMedium) return false
        if (titleSmall != other.titleSmall) return false
        if (bodyLarge != other.bodyLarge) return false
        if (bodyMedium != other.bodyMedium) return false
        if (bodySmall != other.bodySmall) return false

        return true
    }

    override fun hashCode(): Int {
        var result = titleLarge.hashCode()
        result = 31 * result + titleMedium.hashCode()
        result = 31 * result + titleSmall.hashCode()
        result = 31 * result + bodyLarge.hashCode()
        result = 31 * result + bodyMedium.hashCode()
        result = 31 * result + bodySmall.hashCode()
        return result
    }

    override fun toString(): String {
        return "Typography(titleLarge=$titleLarge, titleMedium=$titleMedium, titleSmall=$titleSmall, bodyLarge=$bodyLarge, bodyMedium=$bodyMedium, bodySmall=$bodySmall)"
    }
}

private val DefaultLineHeightStyle =
    LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.FirstLineTop,
    )

private val TitleLarge =
    TextStyle.Default.copy(
        fontWeight = FontWeight(750),
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.05.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

private val TitleMedium =
    TextStyle.Default.copy(
        fontWeight = FontWeight(750),
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.0525.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

private val TitleSmall =
    TextStyle.Default.copy(
        fontWeight = FontWeight(750),
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.0525.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

private val BodyLarge =
    TextStyle.Default.copy(
        fontWeight = FontWeight.W600,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.05.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

private val BodyMedium =
    TextStyle.Default.copy(
        fontWeight = FontWeight.W600,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.0425.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

private val BodySmall =
    TextStyle.Default.copy(
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.0525.em,
        lineHeightStyle = DefaultLineHeightStyle,
    )

/**
 * Uses [default] as the [FontFamily] for [this] [TextStyle] if no [FontFamily] is set on the text
 * style. If no [FontFamily] is set and [default] is null, the platform default text style will end
 * up being used when the text is rendered.
 */
private fun TextStyle.withDefaultFontFamily(default: FontFamily?): TextStyle {
    return if (default == null || fontFamily != null) this else copy(fontFamily = default)
}
