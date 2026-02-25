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
@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.material3

import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
@RemoteComposable
@Composable
public fun RemoteTimeText(
    modifier: RemoteModifier = RemoteModifier,
    time: RemoteString = RemoteTimeTextDefaults.defaultTimeString(),
    fontSize: RemoteTextUnit = 14.rsp,
    fontFamily: FontFamily? = null,
    leadingText: RemoteString? = null,
    trailingText: RemoteString? = null,
    separator: RemoteString = RemoteString("·"),
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

private fun RemoteDrawScope.drawTimeText(
    text: RemoteString,
    textColor: RemoteColor,
    fontSize: RemoteFloat,
    fontFamily: FontFamily?,
) {
    val width = remoteWidth
    val height = remoteHeight

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

    val textPaint =
        RemotePaint().apply {
            textSize = fontSize.floatId
            typeface = fontTypeface
            remoteColor = textColor
        }

    drawTextOnCircle(
        text,
        width / 2f,
        height / 2f,
        width / 2f - fontSize,
        270f.rf,
        0f.rf,
        DrawTextOnCircle.Alignment.CENTER,
        DrawTextOnCircle.Placement.OUTSIDE,
        textPaint,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTimeTextDefaults {

    /**
     * Returns a [RemoteBoolean] indicating whether the time should be displayed in 24-hour format.
     * Currently captured at recording time.
     */
    @Composable
    public fun is24HourFormat(): RemoteBoolean =
        RemoteBoolean(DateFormat.is24HourFormat(LocalContext.current))

    @Composable
    public fun defaultTimeString(is24HourFormat: RemoteBoolean = is24HourFormat()): RemoteString {
        val mins =
            (RemoteFloat(FLOAT_TIME_IN_MIN) % 60f).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val hours24String: RemoteString =
            RemoteFloat(FLOAT_TIME_IN_HR).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val currentHour = RemoteFloat(FLOAT_TIME_IN_HR)
        val hour12: RemoteFloat =
            ((currentHour % 12f).eq(0.rf)).select(RemoteFloat(12f), currentHour % 12f)
        val hours12String: RemoteString = hour12.toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val amPm: RemoteString = (currentHour.lt(12.rf)).select(" AM".rs, " PM".rs)

        val time24 = hours24String + ":" + mins
        val time12 = hours12String + ":" + mins + amPm
        return is24HourFormat.select(time24, time12)
    }
}
