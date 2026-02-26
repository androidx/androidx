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

package androidx.compose.remote.creation.compose.text

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.asRemoteTextUnit
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

/**
 * A remote-aware text style that mirrors [androidx.compose.ui.text.TextStyle] but uses remote types
 * where applicable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteTextStyle(
    public val color: RemoteColor? = null,
    public val fontSize: RemoteTextUnit? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
    public val fontFamily: FontFamily? = null,
    public val letterSpacing: RemoteTextUnit? = null,
    public val background: RemoteColor? = null,
    public val textAlign: TextAlign? = null,
    public val lineHeight: RemoteTextUnit? = null,
    public val textDecoration: TextDecoration? = null,
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun merge(other: RemoteTextStyle?): RemoteTextStyle {
        if (other == null) return this
        return RemoteTextStyle(
            color = other.color ?: this.color,
            fontSize = other.fontSize ?: this.fontSize,
            fontWeight = other.fontWeight ?: this.fontWeight,
            fontStyle = other.fontStyle ?: this.fontStyle,
            fontFamily = other.fontFamily ?: this.fontFamily,
            letterSpacing = other.letterSpacing ?: this.letterSpacing,
            background = other.background ?: this.background,
            textAlign = other.textAlign ?: this.textAlign,
            lineHeight = other.lineHeight ?: this.lineHeight,
            textDecoration = other.textDecoration ?: this.textDecoration,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun merge(
        color: RemoteColor? = null,
        fontSize: RemoteTextUnit? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        fontFamily: FontFamily? = null,
        letterSpacing: RemoteTextUnit? = null,
        background: RemoteColor? = null,
        textAlign: TextAlign? = null,
        lineHeight: RemoteTextUnit? = null,
        textDecoration: TextDecoration? = null,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color ?: this.color,
            fontSize = fontSize ?: this.fontSize,
            fontWeight = fontWeight ?: this.fontWeight,
            fontStyle = fontStyle ?: this.fontStyle,
            fontFamily = fontFamily ?: this.fontFamily,
            letterSpacing = letterSpacing ?: this.letterSpacing,
            background = background ?: this.background,
            textAlign = textAlign ?: this.textAlign,
            lineHeight = lineHeight ?: this.lineHeight,
            textDecoration = textDecoration ?: this.textDecoration,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun copy(
        color: RemoteColor? = this.color,
        fontSize: RemoteTextUnit? = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        fontFamily: FontFamily? = this.fontFamily,
        letterSpacing: RemoteTextUnit? = this.letterSpacing,
        background: RemoteColor? = this.background,
        textAlign: TextAlign? = this.textAlign,
        lineHeight: RemoteTextUnit? = this.lineHeight,
        textDecoration: TextDecoration? = this.textDecoration,
    ): RemoteTextStyle {
        return RemoteTextStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            background = background,
            textAlign = textAlign,
            lineHeight = lineHeight,
            textDecoration = textDecoration,
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        @SuppressLint("RestrictedApiAndroidX")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromTextStyle(style: TextStyle): RemoteTextStyle {
            // Maps unspecified color into null as it's not supported in remote compose.
            val color = if (style.color == Color.Unspecified) null else style.color.rc
            return RemoteTextStyle(
                color = color,
                fontSize = style.fontSize.asRemoteTextUnit(),
                fontWeight = style.fontWeight,
                fontStyle = style.fontStyle,
                fontFamily = style.fontFamily,
                letterSpacing = style.letterSpacing.asRemoteTextUnit(),
                background = style.background.rc,
                textAlign = style.textAlign,
                lineHeight = style.lineHeight.asRemoteTextUnit(),
                textDecoration = style.textDecoration,
            )
        }

        public val Default: RemoteTextStyle = RemoteTextStyle()
    }
}
