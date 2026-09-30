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
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 */

/** Groups the remote children of one composable lambda parameter under a slot id. */
@RemoteComposable
@Composable
private fun RemoteSlot(slotId: Int, content: @Composable @RemoteComposable () -> Unit) {
    RemoteCustomComponent(
        name = BookendsSpec.SLOT,
        properties = { property(BookendsSpec.PROP_SLOT_ID, slotId) },
        content = content,
    )
}

/**
 * Top level scaffold, rendered as `androidx.wear.compose.material3.AppScaffold`.
 *
 * @param modifier The modifier for the scaffold.
 * @param timeText The time text shown at the top of the screen, by default a [RemoteTimeText].
 * @param content The screen content, typically a [RemoteScreenScaffold].
 */
@RemoteComposable
@Composable
fun RemoteAppScaffold(
    modifier: RemoteModifier = RemoteModifier,
    timeText: @Composable @RemoteComposable () -> Unit = { RemoteTimeText() },
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCustomComponent(
        name = BookendsSpec.APP_SCAFFOLD,
        modifier = modifier.fillMaxSize(),
        content = {
            RemoteSlot(BookendsSpec.SLOT_TIME_TEXT, timeText)
            RemoteSlot(BookendsSpec.SLOT_CONTENT, content)
        },
    )
}

/**
 * Screen scaffold, rendered as `androidx.wear.compose.material3.ScreenScaffold`.
 *
 * Unlike the Material3 version there is no `scrollState` parameter: the host links the scaffold to
 * the [RemoteTransformingLazyColumn] in its [content], which drives the scroll indicator and the
 * time text scroll away.
 *
 * @param modifier The modifier for the scaffold.
 * @param timeText Optional time text overriding the one of the [RemoteAppScaffold].
 * @param scrollIndicator The scroll (position) indicator, by default a [RemoteScrollIndicator].
 * @param content The screen content, typically a [RemoteTransformingLazyColumn].
 */
@RemoteComposable
@Composable
fun RemoteScreenScaffold(
    modifier: RemoteModifier = RemoteModifier,
    timeText: (@Composable @RemoteComposable () -> Unit)? = null,
    scrollIndicator: (@Composable @RemoteComposable () -> Unit)? = { RemoteScrollIndicator() },
    content: @Composable @RemoteComposable () -> Unit,
) {
    RemoteCustomComponent(
        name = BookendsSpec.SCREEN_SCAFFOLD,
        modifier = modifier.fillMaxSize(),
        content = {
            timeText?.let { RemoteSlot(BookendsSpec.SLOT_TIME_TEXT, it) }
            scrollIndicator?.let { RemoteSlot(BookendsSpec.SLOT_SCROLL_INDICATOR, it) }
            RemoteSlot(BookendsSpec.SLOT_CONTENT, content)
        },
    )
}

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
 * Scroll (position) indicator for the enclosing [RemoteScreenScaffold]'s list, rendered as
 * `androidx.wear.compose.material3.ScrollIndicator`.
 *
 * @param modifier The modifier for the indicator.
 */
@RemoteComposable
@Composable
fun RemoteScrollIndicator(modifier: RemoteModifier = RemoteModifier) {
    RemoteCustomComponent(name = BookendsSpec.SCROLL_INDICATOR, modifier = modifier)
}

/** Receiver scope for the content of a [RemoteTransformingLazyColumn]. */
class RemoteTransformingLazyColumnScope internal constructor() {
    internal val items = mutableListOf<@Composable @RemoteComposable () -> Unit>()

    /** Adds a single item. */
    fun item(content: @Composable @RemoteComposable () -> Unit) {
        items.add(content)
    }

    /** Adds [count] items, with [itemContent] invoked for each index. */
    fun items(count: Int, itemContent: @Composable @RemoteComposable (index: Int) -> Unit) {
        repeat(count) { index -> items.add { itemContent(index) } }
    }
}

/**
 * Scrolling list, rendered as `androidx.wear.foundation.lazy.TransformingLazyColumn`.
 *
 * The host applies the Material3 transformation spec (morphing and scaling at the edges of the
 * screen) to each item, so there is no need to use `transformedHeight` or `SurfaceTransformation`
 * in the items. Items are recorded eagerly into the document; the host lays them out lazily.
 *
 * @param modifier The modifier for the list.
 * @param content The list content, declared with [RemoteTransformingLazyColumnScope.item] and
 *   [RemoteTransformingLazyColumnScope.items].
 */
@RemoteComposable
@Composable
fun RemoteTransformingLazyColumn(
    modifier: RemoteModifier = RemoteModifier,
    content: RemoteTransformingLazyColumnScope.() -> Unit,
) {
    val scope = RemoteTransformingLazyColumnScope().apply(content)
    RemoteCustomComponent(
        name = BookendsSpec.TRANSFORMING_LAZY_COLUMN,
        modifier = modifier.fillMaxSize(),
        content = {
            scope.items.forEach { item -> RemoteSlot(BookendsSpec.SLOT_CONTENT, item) }
        },
    )
}

/**
 * Button with a label, and optional secondary label and icon, rendered as
 * `androidx.wear.compose.material3.Button`.
 *
 * @param onClick The action performed when the button is clicked.
 * @param modifier The modifier for the button.
 * @param secondaryLabel Optional secondary label, shown below the [label].
 * @param icon Optional icon, shown at the start of the button.
 * @param enabled Whether the button is enabled.
 * @param label The main label of the button.
 */
@RemoteComposable
@Composable
fun RemoteButton(
    onClick: Action,
    modifier: RemoteModifier = RemoteModifier,
    secondaryLabel: (@Composable @RemoteComposable RemoteRowScope.() -> Unit)? = null,
    icon: (@Composable @RemoteComposable () -> Unit)? = null,
    enabled: RemoteBoolean = true.rb,
    label: @Composable @RemoteComposable RemoteRowScope.() -> Unit,
) {
    if (LocalBookendsImplementation.current == BookendsImplementation.RemoteMaterial3) {
        // Note: ButtonPlugin applies fillMaxWidth() on the host (since RcPlayerCustom wraps custom
        // components in a Box that does not propagate min constraints), whereas RemoteMaterial3's
        // RemoteButton shrink-wraps its content unless fillMaxWidth() is applied. Apply
        // fillMaxWidth() here so both implementations produce the same button width.
        RemoteMaterial3Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            secondaryLabel = secondaryLabel,
            icon = icon,
            enabled = enabled,
            label = label,
        )
        return
    }
    val rowScope = remember { RemoteRowScope() }
    // The click is dispatched from the native Material3 Button on the host, so no ripple or
    // semantics role is added here.
    RemoteCustomComponent(
        name = BookendsSpec.BUTTON,
        modifier = modifier.clickable(onClick, role = null),
        content = {
            RemoteSlot(BookendsSpec.SLOT_LABEL) { rowScope.label() }
            secondaryLabel?.let { RemoteSlot(BookendsSpec.SLOT_SECONDARY_LABEL) { rowScope.it() } }
            icon?.let { RemoteSlot(BookendsSpec.SLOT_ICON, it) }
        },
        properties = { property(BookendsSpec.PROP_ENABLED, enabled) },
    )
}

/**
 * Icon displaying a [RemoteImageBitmap], rendered as `androidx.wear.compose.material3.Icon`.
 *
 * @param bitmap The bitmap to display.
 * @param contentDescription Optional accessibility description for the icon.
 * @param modifier The modifier for the icon.
 * @param tint Optional tint color for the icon, or `null` to draw the bitmap untinted.
 */
@RemoteComposable
@Composable
fun RemoteIcon(
    bitmap: RemoteImageBitmap,
    contentDescription: RemoteString?,
    modifier: RemoteModifier = RemoteModifier,
    tint: RemoteColor? = null,
) {
    if (LocalBookendsImplementation.current == BookendsImplementation.RemoteMaterial3) {
        // Note: IconPlugin sizes icons to ButtonDefaults.IconSize (26.dp), whereas
        // RemoteMaterial3's RemoteIcon defaults to RemoteIconDefaults.SmallIconSize (24.rdp) and
        // takes an ImageVector/RemoteImageVector rather than a RemoteImageBitmap.
        RemoteImage(
            remoteBitmap = bitmap,
            contentDescription = contentDescription,
            modifier =
                modifier
                    .semantics { this.contentDescription = contentDescription }
                    .size(BookendsSpec.ICON_SIZE_DP.rdp),
        )
        return
    }
    RemoteCustomComponent(
        name = BookendsSpec.ICON,
        modifier = modifier,
        properties = {
            property(BookendsSpec.PROP_BITMAP, bitmap)
            contentDescription?.let { property(BookendsSpec.PROP_CONTENT_DESCRIPTION, it) }
            tint?.let { property(BookendsSpec.PROP_TINT, it) }
        },
    )
}

/**
 * Text, rendered as `androidx.wear.compose.material3.Text`. The color and style default to the
 * host's `LocalContentColor` and `LocalTextStyle`, so it adapts to the component it is placed in
 * (e.g. the label of a [RemoteButton]).
 *
 * @param text The text to display.
 * @param modifier The modifier for the text.
 * @param color Optional text color, otherwise the host's content color.
 * @param textAlign Optional alignment of the text within its line, otherwise the alignment provided
 *   by the enclosing component.
 * @param maxLines The maximum number of lines, `Int.MAX_VALUE` uses the limit provided by the
 *   enclosing component (e.g. the label of a [RemoteButton]).
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
