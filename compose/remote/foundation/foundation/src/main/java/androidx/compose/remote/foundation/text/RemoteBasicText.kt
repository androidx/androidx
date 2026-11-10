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
package androidx.compose.remote.foundation.text

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.toRemoteFontVariationSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Displays text in a remote layout.
 *
 * This is the remote equivalent of Compose UI's [androidx.compose.foundation.text.BasicText]: a
 * foundational building block that renders text from explicit parameters and [style] without
 * reading Material theme colors or typography. For themed text that integrates with Material color
 * schemes and typography, use a higher-level component such as
 * [androidx.wear.compose.remote.material3.RemoteText].
 *
 * Commonly used properties from [RemoteTextStyle] are also exposed directly as parameters for
 * convenience:
 * - When a styling parameter is non-`null`, it overrides the corresponding property in [style].
 * - When `null`, the value from [style] is used instead, falling back to a default value if also
 *   unset in [style].
 *
 * Example of a basic [RemoteBasicText]:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextSample
 *
 * Example of [RemoteBasicText] with bold and italic styling:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextStylingSample
 *
 * Example of [RemoteBasicText] with a monospace [RemoteFontFamily]:
 *
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextFontFamilySample
 * @param text text to display
 * @param modifier the [RemoteModifier] applied to this text layout
 * @param color text color. If `null`, uses [RemoteTextStyle.color] from [style], or
 *   [androidx.compose.ui.graphics.Color.Black] if also `null` in [style]
 * @param fontSize glyph size. If `null`, uses [RemoteTextStyle.fontSize] from [style], or `12.sp`
 *   if also `null` in [style]
 * @param fontStyle typeface slope (such as [FontStyle.Italic]). If `null`, uses
 *   [RemoteTextStyle.fontStyle] from [style], or [FontStyle.Normal] if also `null` in [style]
 * @param fontWeight typeface thickness (such as [FontWeight.Bold]). If `null`, uses
 *   [RemoteTextStyle.fontWeight] from [style], or [FontWeight.Normal] if also `null` in [style]
 * @param fontFamily font family used to render the text. If `null`, uses
 *   [RemoteTextStyle.fontFamily] from [style], or the platform default typeface if also `null` in
 *   [style]
 * @param textAlign horizontal alignment of the text within its container. If `null`, uses
 *   [RemoteTextStyle.textAlign] from [style], or [TextAlign.Start] if also unset in [style]
 * @param overflow how visual overflow is handled. Defaults to [TextOverflow.Clip]
 * @param maxLines maximum number of visible lines. Defaults to [Int.MAX_VALUE]
 * @param fontVariationSettings OpenType variable font axis settings. If `null`, uses
 *   [RemoteTextStyle.fontVariationSettings] from [style], or applies no custom variation settings
 *   if also `null` in [style]
 * @param style base [RemoteTextStyle] configuration for properties such as color, font, letter
 *   spacing, and line height. Defaults to [RemoteTextStyle.Default]
 */
@Composable
@RemoteComposable
public fun RemoteBasicText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: RemoteTextUnit? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    fontVariationSettings: FontVariation.Settings? = null,
    style: RemoteTextStyle = RemoteTextStyle.Default,
) {
    RemoteText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle?.toRemoteFontStyle(),
        fontWeight = fontWeight?.toRemoteFontWeight(),
        fontFamily = fontFamily,
        textAlign = textAlign?.toRemoteTextAlign(),
        overflow = overflow.toRemoteTextOverflow(),
        maxLines = maxLines,
        fontVariationSettings = fontVariationSettings?.toRemote(),
        style = style,
    )
}

private fun FontStyle.toRemoteFontStyle(): RemoteFontStyle =
    when (this) {
        FontStyle.Italic -> RemoteFontStyle.Italic
        else -> RemoteFontStyle.Normal
    }

private fun FontWeight.toRemoteFontWeight(): RemoteFontWeight = RemoteFontWeight(weight)

private fun TextAlign.toRemoteTextAlign(): RemoteTextAlign =
    when (this) {
        TextAlign.Left -> RemoteTextAlign.Left
        TextAlign.Right -> RemoteTextAlign.Right
        TextAlign.Center -> RemoteTextAlign.Center
        TextAlign.Justify -> RemoteTextAlign.Justify
        TextAlign.Start -> RemoteTextAlign.Start
        TextAlign.End -> RemoteTextAlign.End
        else -> RemoteTextAlign.Unspecified
    }

private fun TextOverflow.toRemoteTextOverflow(): RemoteTextOverflow =
    when (this) {
        TextOverflow.Clip -> RemoteTextOverflow.Clip
        TextOverflow.Visible -> RemoteTextOverflow.Visible
        TextOverflow.Ellipsis -> RemoteTextOverflow.Ellipsis
        TextOverflow.StartEllipsis -> RemoteTextOverflow.StartEllipsis
        TextOverflow.MiddleEllipsis -> RemoteTextOverflow.MiddleEllipsis
        else -> RemoteTextOverflow.Clip
    }

@Suppress("RestrictedApiAndroidX")
private fun FontVariation.Settings.toRemote(): RemoteFontVariation.Settings =
    toRemoteFontVariationSettings()
