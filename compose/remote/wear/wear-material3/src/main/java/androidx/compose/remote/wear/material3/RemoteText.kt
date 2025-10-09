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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.wear.material3

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.material3.LocalTextStyle

/**
 * High level element that displays text and provides semantics / accessibility information.
 *
 * For ease of use, commonly used parameters from [TextStyle] are also present here. The order of
 * precedence is as follows:
 * - If a parameter is explicitly set here (i.e, it is _not_ `null` or [TextUnit.Unspecified]), then
 *   this parameter will always be used.
 * - If a parameter is _not_ set, (`null` or [TextUnit.Unspecified]), then the corresponding value
 *   from [style] will be used instead.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param color [Color] to apply to the text.
 * @param fontSize The size of glyphs to use when painting the text. See [TextStyle.fontSize].
 * @param fontStyle The typeface variant to use when drawing the letters (e.g., italic). See
 *   [TextStyle.fontStyle].
 * @param fontWeight The typeface thickness to use when painting the text (e.g., [FontWeight.Bold]).
 * @param fontFamily The font family to be used when rendering the text. See [TextStyle.fontFamily].
 * @param textAlign The alignment of the text within the lines of the paragraph. See
 *   [TextStyle.textAlign].
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text to span.
 * @param style Style configuration for the text such as color, font, line height etc.
 */
@SuppressLint("RestrictedApiAndroidX")
@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
    overflow: TextOverflow = LocalTextConfiguration.current.overflow,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.remote.creation.compose.layout.RemoteText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}
