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
import androidx.compose.remote.foundation.text.RemoteBasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy

/**
 * Displays Material Design text and provides accessibility semantics in a remote layout.
 *
 * This is the Wear Material 3 counterpart to [RemoteBasicText] and the remote equivalent of
 * [androidx.wear.compose.material3.Text], integrating with theme colors, typography, and ambient
 * text configuration via [LocalRemoteTextConfiguration] provided by surrounding Wear Material 3
 * components.
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
 * @param fontStyle typeface slope (such as [RemoteFontStyle.Italic]). If `null`, uses
 *   [RemoteTextStyle.fontStyle] from [style], or [RemoteFontStyle.Normal] if also `null` in [style]
 * @param fontWeight typeface thickness (such as [RemoteFontWeight.Bold]). If `null`, uses
 *   [RemoteTextStyle.fontWeight] from [style], or [RemoteFontWeight.Normal] if also `null` in
 *   [style]
 * @param fontFamily font family used to render the text. If `null`, uses
 *   [RemoteTextStyle.fontFamily] from [style], or the platform default typeface if also `null` in
 *   [style]
 * @param textAlign horizontal alignment of the text within its container. Defaults to the ambient
 *   text configuration's alignment; if `null`, uses [RemoteTextStyle.textAlign] from [style], or
 *   [RemoteTextAlign.Start] if also unset in [style]
 * @param overflow how visual overflow is handled. Defaults to the ambient text configuration's
 *   overflow
 * @param maxLines maximum number of visible lines (must be greater than zero). Defaults to the
 *   ambient text configuration's `maxLines`
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
    fontStyle: RemoteFontStyle? = null,
    fontWeight: RemoteFontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: RemoteTextAlign? = LocalRemoteTextConfiguration.current.textAlign,
    overflow: RemoteTextOverflow = LocalRemoteTextConfiguration.current.overflow,
    maxLines: Int = LocalRemoteTextConfiguration.current.maxLines,
    fontVariationSettings: RemoteFontVariation.Settings? = null,
    style: RemoteTextStyle = LocalRemoteTextStyle.current,
) {
    val color = color ?: style.color ?: LocalRemoteContentColor.current
    RemoteBasicText(
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
        fontVariationSettings = fontVariationSettings,
        style = style,
    )
}

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

/**
 * CompositionLocal containing the preferred [RemoteTextConfiguration] that will be used by
 * [RemoteText] components by default consisting of text alignment, overflow specification and max
 * lines. Material3 components related to text such as [RemoteButton], [RemoteCheckboxButton],
 * [RemoteSwitchButton], [RemoteRadioButton] use [LocalRemoteTextConfiguration] to set values with
 * which to style child text components.
 */
public val LocalRemoteTextConfiguration: ProvidableCompositionLocal<RemoteTextConfiguration> =
    compositionLocalOf(structuralEqualityPolicy()) {
        RemoteTextConfiguration(
            RemoteTextConfigurationDefaults.TextAlign,
            RemoteTextConfigurationDefaults.Overflow,
            RemoteTextConfigurationDefaults.MaxLines,
        )
    }

/**
 * Class representing aspects of [RemoteText] that can be configured with
 * [LocalRemoteTextConfiguration].
 *
 * @param textAlign The alignment of the text within the lines of the paragraph.
 * @param overflow How visual overflow should be handled.
 * @param maxLines The maximum number of lines for the text to span, wrapping if necessary.
 */
@Immutable
public class RemoteTextConfiguration(
    public val textAlign: RemoteTextAlign?,
    public val overflow: RemoteTextOverflow,
    public val maxLines: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is RemoteTextConfiguration) return false

        if (textAlign != other.textAlign) return false
        if (overflow != other.overflow) return false
        if (maxLines != other.maxLines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textAlign?.hashCode() ?: 0
        result = 31 * result + overflow.hashCode()
        result = 31 * result + maxLines
        return result
    }
}

/** Default values for [RemoteTextConfiguration]. */
public object RemoteTextConfigurationDefaults {
    /** Default text alignment for [RemoteText]. */
    public val TextAlign: RemoteTextAlign? = null

    /** Default visual text overflow for [RemoteText]. */
    public val Overflow: RemoteTextOverflow = RemoteTextOverflow.Clip

    /** Default max lines for [RemoteText]. */
    public val MaxLines: Int = Int.MAX_VALUE
}
