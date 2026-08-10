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

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.text.toRemoteTypeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontVariation

/**
 * A remote composable for displaying the time and surrounding text, designed to curve along the top
 * of a circular screen. This is a remote version of the `TimeText` composable.
 *
 * `RemoteTimeText` is typically used at the top of the screen and is the remote equivalent of
 * `androidx.wear.compose.material3.TimeText`.
 *
 * @param modifier The [RemoteModifier] to be applied to the `RemoteTimeText`.
 * @param time The text to display as the time. Defaults to a formatted time string from the remote
 *   context.
 * @param leadingText Text to be displayed before the time, or null if not present.
 * @param trailingText Text to be displayed after the time, or null if not present.
 * @param separator The separator to be used between the leading/trailing text and the time.
 *   Defaults to "·".
 * @param color The color of the text. Defaults to the `onBackground` color from the current
 *   `RemoteMaterialTheme`.
 * @param style The [RemoteTextStyle] to be applied to the text. Defaults to
 *   [RemoteTimeTextDefaults.timeTextStyle].
 * @param fontFeatureSettings The font feature settings to be applied to the text. Defaults to
 *   [RemoteTimeTextDefaults.fontFeatureSettings].
 * @param fontVariationSettings The font variation settings to be applied to the text. Defaults to
 *   [RemoteTimeTextDefaults.fontVariationSettings].
 */
@RemoteComposable
@Composable
public fun RemoteTimeText(
    modifier: RemoteModifier = RemoteModifier,
    time: RemoteString = RemoteTimeDefaults.defaultTimeString(),
    fontSize: RemoteTextUnit? = null,
    fontFamily: RemoteFontFamily? = null,
    leadingText: RemoteString? = null,
    trailingText: RemoteString? = null,
    separator: RemoteString = "·".rs,
    color: RemoteColor? = null,
    style: RemoteTextStyle = RemoteTimeTextDefaults.timeTextStyle,
    fontFeatureSettings: String? = null,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    val text =
        buildTimeTextString(
            time = time,
            leadingText = leadingText ?: "".rs,
            trailingText = trailingText ?: "".rs,
            separator = separator,
        )
    val mergedStyle =
        style.merge(
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            fontVariationSettings = fontVariationSettings,
        )
    val resolvedFontSize = mergedStyle.fontSize ?: 14.rsp
    val resolvedColor = mergedStyle.color ?: RemoteMaterialTheme.colorScheme.onBackground

    RemoteBox(modifier.clearAndSetSemantics {}) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            drawTimeText(
                text = text,
                textColor = resolvedColor,
                fontSize = resolvedFontSize.toPx(),
                fontFamily = mergedStyle.fontFamily,
                fontVariationSettings = mergedStyle.combinedFontVariationSettings,
            )
        }
    }
}

@Composable
private fun buildTimeTextString(
    time: RemoteString,
    leadingText: RemoteString,
    trailingText: RemoteString,
    separator: RemoteString,
): RemoteString {
    val leadingWithSeparator = leadingText.isNotEmpty.select(leadingText + separator, "".rs)
    val trailingWithSeparator = trailingText.isNotEmpty.select(separator + trailingText, "".rs)
    return leadingWithSeparator + time + trailingWithSeparator
}

private fun RemoteDrawScope.drawTimeText(
    text: RemoteString,
    textColor: RemoteColor,
    fontSize: RemoteFloat,
    fontFamily: RemoteFontFamily?,
    fontVariationSettings: FontVariation.Settings?,
) {
    val width = width
    val height = height

    val textPaint = RemotePaint {
        textSize = fontSize
        typeface = fontFamily?.toRemoteTypeface() ?: RemoteTypeface.Default
        color = textColor
        this.fontVariationSettings = fontVariationSettings
    }

    drawTextOnCircle(
        text,
        width / 2f.rf,
        height / 2f.rf,
        width / 2f.rf - fontSize,
        270f.rf,
        0f.rf,
        textPaint,
    )
}

/** Contains the default values used by [RemoteTimeText]. */
public object RemoteTimeTextDefaults {
    /** Default font family used by [RemoteTimeText]. */
    public val fontFamily: RemoteFontFamily = RemoteFontFamily.Named("google:Roboto Flex")

    /** Default font feature settings used by [RemoteTimeText]. */
    public val fontFeatureSettings: String? = "tnum"

    /** Default font variation settings used by [RemoteTimeText]. */
    public val fontVariationSettings: FontVariation.Settings? = null

    /** Default text style used by [RemoteTimeText]. */
    public val timeTextStyle: RemoteTextStyle =
        RemoteTextStyle(
            fontSize = 14.rsp,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            fontVariationSettings = fontVariationSettings,
        )
}
