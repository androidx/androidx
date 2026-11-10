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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
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
import androidx.compose.remote.foundation.text.RemoteBasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.LocalTextConfiguration

/**
 * Displays Material Design text and provides accessibility semantics in a remote layout.
 *
 * This is the Wear Material 3 counterpart to [RemoteBasicText] and the remote equivalent of
 * [androidx.wear.compose.material3.Text], integrating with theme colors, typography, and
 * [LocalTextConfiguration] provided by surrounding Wear Material 3 components.
 *
 * Commonly used properties from [RemoteTextStyle] are also exposed directly as parameters for
 * convenience:
 * - When a styling parameter is non-`null`, it overrides the corresponding property in [style].
 * - When `null`, the value from [style] is used instead, falling back to a default value if also
 *   unset in [style].
 *
 * Example of styling [RemoteText] with [RemoteMaterialTheme.typography] and theme colors:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextSample
 * @param text text to display
 * @param modifier the [RemoteModifier] applied to this text layout
 * @param color text color. If `null`, uses [RemoteTextStyle.color] from [style], or the ambient
 *   content color if also `null` in [style]
 * @param fontSize glyph size. If `null`, uses [RemoteTextStyle.fontSize] from [style], or `12.sp`
 *   if also `null` in [style]
 * @param fontStyle typeface slope (such as [FontStyle.Italic]). If `null`, uses
 *   [RemoteTextStyle.fontStyle] from [style], or [FontStyle.Normal] if also `null` in [style]
 * @param fontWeight typeface thickness (such as [FontWeight.Bold]). If `null`, uses
 *   [RemoteTextStyle.fontWeight] from [style], or [FontWeight.Normal] if also `null` in [style]
 * @param fontFamily font family used to render the text. If `null`, uses
 *   [RemoteTextStyle.fontFamily] from [style], or the platform default typeface if also `null` in
 *   [style]
 * @param textAlign horizontal alignment of the text within its container. Defaults to
 *   [LocalTextConfiguration]'s alignment; if `null`, uses [RemoteTextStyle.textAlign] from [style],
 *   or [TextAlign.Start] if also unset in [style]
 * @param overflow how visual overflow is handled. Defaults to [LocalTextConfiguration]'s overflow
 * @param maxLines maximum number of visible lines (must be greater than zero). Defaults to
 *   [LocalTextConfiguration]'s `maxLines`
 * @param fontVariationSettings OpenType variable font axis settings. If `null`, uses
 *   [RemoteTextStyle.fontVariationSettings] from [style], or applies no custom variation settings
 *   if also `null` in [style]
 * @param style base [RemoteTextStyle] configuration for properties such as color, font, letter
 *   spacing, and line height. Defaults to the ambient text style provided by surrounding Wear
 *   Material 3 components
 */
@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: RemoteTextUnit? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign? = LocalTextConfiguration.current.textAlign,
    overflow: TextOverflow = LocalTextConfiguration.current.overflow,
    maxLines: Int = LocalTextConfiguration.current.maxLines,
    fontVariationSettings: FontVariation.Settings? = null,
    style: RemoteTextStyle = LocalRemoteTextStyle.current,
) {
    val color = color ?: style.color ?: LocalRemoteContentColor.current
    RemoteBasicText(
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

/**
 * CompositionLocal containing the preferred [RemoteTextStyle] that will be used by [RemoteText]
 * components by default. To set the value for this CompositionLocal, see [ProvideRemoteTextStyle]
 * which will merge any missing [RemoteTextStyle] properties with the existing [RemoteTextStyle] set
 * in this CompositionLocal.
 *
 * @see ProvideRemoteTextStyle
 */
public val LocalRemoteTextStyle: ProvidableCompositionLocal<RemoteTextStyle> =
    staticCompositionLocalOf {
        RemoteTextStyle()
    }
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get

/**
 * This function is used to set the current value of [LocalRemoteTextStyle], merging the given style
 * with the current style values for any missing attributes. Any [RemoteText] components included in
 * this component's [content] will be styled with this style unless styled explicitly.
 *
 * @see LocalRemoteTextStyle
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun ProvideRemoteTextStyle(
    value: RemoteTextStyle,
    content: @RemoteComposable @Composable () -> Unit,
) {
    val mergedStyle = LocalRemoteTextStyle.current.merge(value)
    CompositionLocalProvider(LocalRemoteTextStyle provides mergedStyle, content = content)
}
