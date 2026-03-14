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

import android.graphics.Typeface
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
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.GenericFontFamily

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
 */
@Suppress("RestrictedApiAndroidX")
@RemoteComposable
@Composable
public fun RemoteTimeText(
    modifier: RemoteModifier = RemoteModifier,
    time: RemoteString = RemoteTimeDefaults.defaultTimeString(),
    fontSize: RemoteTextUnit = 14.rsp,
    fontFamily: FontFamily? = null,
    leadingText: RemoteString? = null,
    trailingText: RemoteString? = null,
    separator: RemoteString = "·".rs,
    color: RemoteColor = RemoteMaterialTheme.colorScheme.onBackground,
) {
    val text =
        buildTimeTextString(
            time = time,
            leadingText = leadingText ?: "".rs,
            trailingText = trailingText ?: "".rs,
            separator = separator,
        )
    val fontSize = fontSize.toPx()

    RemoteBox(modifier.clearAndSetSemantics {}) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            drawTimeText(
                text = text,
                textColor = color,
                fontSize = fontSize,
                fontFamily = fontFamily,
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

@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawTimeText(
    text: RemoteString,
    textColor: RemoteColor,
    fontSize: RemoteFloat,
    fontFamily: FontFamily?,
) {
    val width = width
    val height = height

    val fontTypeface =
        when (fontFamily) {
            FontFamily.Default -> Typeface.DEFAULT
            FontFamily.SansSerif -> Typeface.SANS_SERIF
            FontFamily.Serif -> Typeface.SERIF
            FontFamily.Monospace -> Typeface.MONOSPACE
            else -> {
                if (fontFamily != null && (fontFamily is GenericFontFamily)) {
                    Typeface.create(fontFamily.name, Typeface.NORMAL)
                }
                null
            }
        }

    val textPaint = RemotePaint {
        textSize = fontSize
        typeface = fontTypeface
        color = textColor
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
