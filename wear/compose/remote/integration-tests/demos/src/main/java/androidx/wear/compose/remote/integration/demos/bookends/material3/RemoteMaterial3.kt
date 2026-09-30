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

@file:Suppress("RestrictedApiAndroidX")

package androidx.wear.compose.remote.integration.demos.bookends.material3

import androidx.compose.remote.creation.compose.action.Action
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteCustomComponent
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.layout.RemoteRowScope
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.LocalTextConfiguration
import androidx.wear.compose.remote.integration.demos.bookends.BookendsSpec
import androidx.wear.compose.remote.integration.demos.bookends.BookendsSpec.toWire
import androidx.wear.compose.remote.material3.RemoteButton as RemoteMaterial3Button
import androidx.wear.compose.remote.material3.RemoteText as RemoteMaterial3Text
import androidx.wear.compose.remote.material3.RemoteTimeText as RemoteMaterial3TimeText

/*
 * The creation-side bookend: an API shaped like `androidx.wear.compose.remote.material3` (and
 * `androidx.wear.compose.material3`), but each component is emitted as a `Custom` component rather
 * than being drawn with remote primitives. The host renders them with the real Wear Compose
 * Material3 components (see the `player` package). Themes are not wired up; the host's default
 * Material3 theme applies.
 *
 * Custom components are leaves, so optional parts of a component (e.g. the secondary label and
 * icon of a button) are passed as primitive values rather than composable slots.
 */

/**
 * Curved time text, rendered as `androidx.wear.compose.material3.TimeText`.
 *
 * @param modifier The modifier for the time text.
 * @param time Optional fixed time to display, otherwise the current time.
 */
@RemoteComposable
@Composable
fun RemoteTimeText(modifier: RemoteModifier = RemoteModifier, time: RemoteString? = null) {
    if (LocalBookendsImplementation.current == BookendsImplementation.RemoteMaterial3) {
        RemoteMaterial3TimeText(
            modifier = modifier,
            time = time ?: RemoteTimeDefaults.defaultTimeString(),
        )
        return
    }
    RemoteCustomComponent(
        name = BookendsSpec.TIME_TEXT,
        modifier = modifier,
        properties = { time?.let { property(BookendsSpec.PROP_TEXT, it) } },
    )
}

/**
 * Button with a label, and optional secondary label and icon, rendered as
 * `androidx.wear.compose.material3.Button`.
 *
 * @param onClick The action performed when the button is clicked.
 * @param label The main label of the button.
 * @param modifier The modifier for the button.
 * @param secondaryLabel Optional secondary label, shown below the [label].
 * @param icon Optional icon, shown at the start of the button.
 * @param enabled Whether the button is enabled.
 */
@RemoteComposable
@Composable
fun RemoteButton(
    onClick: Action,
    label: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabel: RemoteString? = null,
    icon: RemoteImageBitmap? = null,
    enabled: RemoteBoolean = true.rb,
) {
    if (LocalBookendsImplementation.current == BookendsImplementation.RemoteMaterial3) {
        val secondaryLabelContent: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? =
            secondaryLabel?.let { text ->
                { RemoteText(text) }
            }
        // Note: IconPlugin sizes icons to ButtonDefaults.IconSize (26.dp), whereas
        // RemoteMaterial3's RemoteIcon defaults to RemoteIconDefaults.SmallIconSize (24.rdp) and
        // takes an ImageVector/RemoteImageVector rather than a RemoteImageBitmap.
        val iconContent: (@Composable () -> Unit)? = icon?.let { bitmap ->
            {
                RemoteImage(
                    remoteBitmap = bitmap,
                    contentDescription = null,
                    modifier = RemoteModifier.size(BookendsSpec.ICON_SIZE_DP.rdp),
                )
            }
        }
        // Note: ButtonPlugin applies fillMaxWidth() on the host (since RcPlayerCustom wraps custom
        // components in a Box that does not propagate min constraints), whereas RemoteMaterial3's
        // RemoteButton shrink-wraps its content unless fillMaxWidth() is applied. Apply
        // fillMaxWidth() here so both implementations produce the same button width.
        RemoteMaterial3Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            secondaryLabel = secondaryLabelContent,
            icon = iconContent,
            enabled = enabled,
            label = { RemoteText(label) },
        )
        return
    }
    // The click is dispatched from the native Material3 Button on the host, so no ripple or
    // semantics role is added here.
    RemoteCustomComponent(
        name = BookendsSpec.BUTTON,
        modifier = modifier.clickable(onClick, role = null),
        properties = {
            property(BookendsSpec.PROP_ENABLED, enabled)
            property(BookendsSpec.PROP_LABEL, label)
            secondaryLabel?.let { property(BookendsSpec.PROP_SECONDARY_LABEL, it) }
            icon?.let { property(BookendsSpec.PROP_ICON, it) }
        },
    )
}

/**
 * Text, rendered as `androidx.wear.compose.material3.Text`. The color and style default to the
 * host's `LocalContentColor` and `LocalTextStyle`.
 *
 * @param text The text to display.
 * @param modifier The modifier for the text.
 * @param color Optional text color, otherwise the host's content color.
 * @param textAlign Optional alignment of the text within its line, otherwise the alignment provided
 *   by the enclosing component.
 * @param maxLines The maximum number of lines, `Int.MAX_VALUE` uses the limit provided by the
 *   enclosing component.
 */
@RemoteComposable
@Composable
fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (LocalBookendsImplementation.current == BookendsImplementation.RemoteMaterial3) {
        val textConfiguration = LocalTextConfiguration.current
        RemoteMaterial3Text(
            text = text,
            modifier = modifier,
            color = color,
            textAlign = textAlign ?: textConfiguration.textAlign,
            maxLines = if (maxLines == Int.MAX_VALUE) textConfiguration.maxLines else maxLines,
        )
        return
    }
    RemoteCustomComponent(
        name = BookendsSpec.TEXT,
        modifier = modifier,
        properties = {
            property(BookendsSpec.PROP_TEXT, text)
            color?.let { property(BookendsSpec.PROP_COLOR, it) }
            textAlign?.let { property(BookendsSpec.PROP_TEXT_ALIGN, it.toWire()) }
            // Unset lets the host use the limit of the enclosing Material3 component.
            if (maxLines != Int.MAX_VALUE) property(BookendsSpec.PROP_MAX_LINES, maxLines)
        },
    )
}
