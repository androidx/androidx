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
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.operations.DrawTextOnCircle
import androidx.compose.remote.core.operations.TextFromFloat
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteCanvasDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.runtime.Composable

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
    leadingText: RemoteString? = null,
    trailingText: RemoteString? = null,
    separator: RemoteString = RemoteString("·"),
    color: RemoteColor = RemoteMaterialTheme.colorScheme.onBackground,
) {
    val text =
        buildTimeTextString(
            time = time,
            leadingText = leadingText ?: RemoteString(""),
            trailingText = trailingText ?: RemoteString(""),
            separator = separator,
        )
    RemoteBox(modifier.clearAndSetSemantics {}) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) { drawTimeText(text, color) }
    }
}

@Composable
private fun buildTimeTextString(
    time: RemoteString,
    leadingText: RemoteString,
    trailingText: RemoteString,
    separator: RemoteString,
): RemoteString {
    val leadingWithSeparator =
        leadingText.isNotEmpty.select(leadingText + separator, RemoteString(""))
    val trailingWithSeparator =
        trailingText.isNotEmpty.select(separator + trailingText, RemoteString(""))
    return leadingWithSeparator + time + trailingWithSeparator
}

private fun RemoteCanvasDrawScope.drawTimeText(text: RemoteString, color: RemoteColor) {
    val width = remote.component.width
    val height = remote.component.height

    val fontSize = 30f
    val textPaint =
        RemotePaint().apply {
            textSize = fontSize
            typeface = Typeface.DEFAULT
            remoteColor = color
        }

    canvas.drawTextOnCircle(
        text,
        width / 2f,
        height / 2f,
        width / 2f - fontSize,
        270f,
        0f,
        DrawTextOnCircle.Alignment.CENTER,
        DrawTextOnCircle.Placement.INSIDE,
        textPaint,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RemoteTimeTextDefaults {

    @Composable
    public fun defaultTimeString(): RemoteString {
        val mins =
            (RemoteFloat(FLOAT_TIME_IN_MIN) % 60f).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        val hours = RemoteFloat(FLOAT_TIME_IN_HR).toRemoteString(2, 0, TextFromFloat.PAD_PRE_ZERO)
        return hours + RemoteString(":") + mins
    }
}
