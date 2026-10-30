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

@file:JvmName("RemoteTextKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.IntRange
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.toRemoteFontVariationSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Remote composable that displays text.
 *
 * Note that density-dependent values like [fontSize], [style#letterSpacing], and [style#lineHeight]
 * are converted to pixels using [RemoteDensity] from the environment where the [RemoteText] is
 * being *created*, not the remote environment where it will be displayed. This means these values
 * are fixed at creation time based on the local density.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextSample
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextStylingSample
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextFontFamilySample
 * @param text The text to be displayed.
 * @param modifier The [RemoteModifier] to be applied to this text.
 * @param color [RemoteColor] to apply to the text. If [color] is not specified, and it is not
 *   provided in [style], then [Color.Black] will be used.
 * @param fontSize The size of the font.
 * @param fontStyle The font style to be applied to the text.
 * @param fontWeight The font weight to be applied to the text.
 * @param fontFamily The font family to be applied to the text.
 * @param textAlign The alignment of the text within its container.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text.
 * @param fontFeatureSettings The advanced typography settings provided by font in CSS format (e.g.
 *   "smcp" or "tnum").
 * @param fontVariationSettings The font variation settings to be applied to the text.
 * @param style The [RemoteTextStyle] to be applied to the text.
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
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
    fontFeatureSettings: String? = null,
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
        textAlign = (textAlign ?: TextAlign.Unspecified).toRemoteTextAlign(),
        overflow = overflow.toRemoteTextOverflow(),
        maxLines = maxLines,
        style = style,
        fontFeatureSettings = fontFeatureSettings,
        fontVariationSettings = fontVariationSettings?.toRemoteFontVariationSettings(),
    )
}
