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
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * Remote composable that displays basic text.
 *
 * @param text The text to be displayed.
 * @param modifier The [RemoteModifier] to be applied to this text.
 * @param color [RemoteColor] to apply to the text. If [color] is not specified, and it is not
 *   provided in [style], then [androidx.compose.ui.graphics.Color.Black] will be used.
 * @param fontSize The size of the font.
 * @param fontStyle The font style to be applied to the text.
 * @param fontWeight The font weight to be applied to the text.
 * @param fontFamily The font family to be applied to the text.
 * @param textAlign The alignment of the text within its container.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text.
 * @param style The [RemoteTextStyle] to be applied to the text.
 * @param fontVariationSettings The font variation settings to be applied to the text.
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextSample
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextStylingSample
 * @sample androidx.compose.remote.foundation.samples.RemoteBasicTextFontFamilySample
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
    style: RemoteTextStyle = RemoteTextStyle.Default,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    RemoteText(
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
        fontVariationSettings = fontVariationSettings,
    )
}
