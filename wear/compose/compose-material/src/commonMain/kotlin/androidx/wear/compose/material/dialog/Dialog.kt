/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.material.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.contentColorFor
import androidx.wear.compose.material.isRoundDevice
import kotlinx.coroutines.delay

/**
 * [Alert] lays out the content for an opinionated, alert screen.
 * This overload offers 5 slots for title, negative button, positive button, optional icon and
 * optional content. The buttons are shown side-by-side below the icon, text and content.
 * [Alert] is scrollable by default if the content is taller than the viewport.
 *
 * [Alert] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of an [Alert] with an icon, title, body text and buttons:
 * @sample androidx.wear.compose.material.samples.AlertWithButtons
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param negativeButton A slot for a [Button] indicating negative sentiment (e.g. No).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param positiveButton A slot for a [Button] indicating positive sentiment (e.g. Yes).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param titleColor [Color] representing the color for [title].
 * @param iconColor Icon [Color] that defaults to [contentColor],
 * unless specifically overridden.
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for additional content, expected to be 2-3 lines of text.
 */
@Composable
public fun Alert(
    title: @Composable ColumnScope.() -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: ScalingLazyListState = rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    titleColor: Color = contentColor,
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    DialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, title)
        }

        if (content != null) {
            item {
                DialogBody(contentColor, content)
            }
        }

        // Buttons
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                negativeButton()
                Spacer(modifier = Modifier.width(DialogDefaults.ButtonSpacing))
                positiveButton()
            }
        }
    }
}

/**
 * [Alert] lays out the content for an opinionated, alert screen.
 * This overload offers 5 slots for title, negative button, positive button, optional icon and
 * optional content. The buttons are shown side-by-side below the icon, text and content.
 * [Alert] is scrollable by default if the content is taller than the viewport.
 *
 * [Alert] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of an [Alert] with an icon, title, body text and buttons:
 * @sample androidx.wear.compose.material.samples.AlertWithButtons
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param negativeButton A slot for a [Button] indicating negative sentiment (e.g. No).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param positiveButton A slot for a [Button] indicating positive sentiment (e.g. Yes).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param titleColor [Color] representing the color for [title].
 * @param iconColor Icon [Color] that defaults to [contentColor],
 * unless specifically overridden.
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for additional content, expected to be 2-3 lines of text.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
        "A newer overload is available which uses ScalingLazyListState from " +
        "wear.compose.foundation.lazy package", level = DeprecationLevel.HIDDEN
)
@Composable
public fun Alert(
    title: @Composable ColumnScope.() -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    titleColor: Color = contentColor,
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    AlertWithMaterialSlc(
        title = title,
        negativeButton = negativeButton,
        positiveButton = positiveButton,
        modifier = modifier,
        icon = icon,
        scrollState = scrollState,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        titleColor = titleColor,
        iconColor = iconColor,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * @VisibleForTesting
 */
@Suppress("DEPRECATION")
@Deprecated("Used only for testing")
@Composable
internal fun AlertWithMaterialSlc(
    title: @Composable ColumnScope.() -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    titleColor: Color = contentColor,
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    MaterialDialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, title)
        }

        if (content != null) {
            item {
                DialogBody(contentColor, content)
            }
        }

        // Buttons
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                negativeButton()
                Spacer(modifier = Modifier.width(DialogDefaults.ButtonSpacing))
                positiveButton()
            }
        }
    }
}

/**
 * [Alert] lays out the content for an opinionated, alert screen.
 * This overload offers 4 slots for title, optional icon, optional message text and
 * a content slot expected to be one or more vertically stacked [Chip]s or [ToggleChip]s.
 * [Alert] is scrollable by default if the content is taller than the viewport.
 *
 * [Alert] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of an [Alert] with an icon, title, message text and chips:
 * @sample androidx.wear.compose.material.samples.AlertWithChips
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param modifier Modifier to be applied to the dialog.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param message Optional slot for additional message content, expected to be 2-3 lines of text.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param titleColor [Color] representing the color for [title].
 * @param messageColor [Color] representing the color for [message].
 * @param iconColor [Color] representing the color for [icon].
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for one or more spaced [Chip]s, stacked vertically.
 */
@Composable
public fun Alert(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    message: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: ScalingLazyListState = rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    titleColor: Color = contentColorFor(backgroundColor),
    messageColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColorFor(backgroundColor),
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: ScalingLazyListScope.() -> Unit
) {
    DialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, content = title)
        }

        if (message != null) {
            item {
                DialogBody(messageColor, message)
            }
        }

        content()
    }
}

/**
 * [Alert] lays out the content for an opinionated, alert screen.
 * This overload offers 4 slots for title, optional icon, optional message text and
 * a content slot expected to be one or more vertically stacked [Chip]s or [ToggleChip]s.
 * [Alert] is scrollable by default if the content is taller than the viewport.
 *
 * [Alert] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of an [Alert] with an icon, title, message text and chips:
 * @sample androidx.wear.compose.material.samples.AlertWithChips
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param modifier Modifier to be applied to the dialog.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param message Optional slot for additional message content, expected to be 2-3 lines of text.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param titleColor [Color] representing the color for [title].
 * @param messageColor [Color] representing the color for [message].
 * @param iconColor [Color] representing the color for [icon].
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for one or more spaced [Chip]s, stacked vertically.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
        "A newer overload is available which uses ScalingLazyListState and ScalingLazyListScope " +
        "from wear.compose.foundation.lazy package", level = DeprecationLevel.HIDDEN
)
@Composable
public fun Alert(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    message: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    titleColor: Color = contentColorFor(backgroundColor),
    messageColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColorFor(backgroundColor),
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: androidx.wear.compose.material.ScalingLazyListScope.() -> Unit
) {
    AlertWithMaterialSlc(
        title = title,
        modifier = modifier,
        icon = icon,
        message = message,
        scrollState = scrollState,
        backgroundColor = backgroundColor,
        titleColor = titleColor,
        messageColor = messageColor,
        iconColor = iconColor,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * @VisibleForTesting
 */
@Suppress("DEPRECATION")
@Deprecated("Used only for testing")
@Composable
internal fun AlertWithMaterialSlc(
    title: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    message: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    titleColor: Color = contentColorFor(backgroundColor),
    messageColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColorFor(backgroundColor),
    verticalArrangement: Arrangement.Vertical = DialogDefaults.AlertVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: androidx.wear.compose.material.ScalingLazyListScope.() -> Unit
) {
    MaterialDialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, content = title)
        }

        if (message != null) {
            item {
                DialogBody(messageColor, message)
            }
        }

        content()
    }
}

/**
 * [Confirmation] lays out the content for an opinionated confirmation screen that
 * displays a message to the user for [durationMillis]. It has a slot for an icon or image
 * (which could be animated).
 *
 * [Confirmation] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of a [Confirmation] with animation:
 * @sample androidx.wear.compose.material.samples.ConfirmationWithAnimation
 *
 * @param onTimeout Event invoked when the dialog has been shown for [durationMillis].
 * @param modifier Modifier to be applied to the dialog.
 * @param icon An optional slot for displaying an icon or image.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param durationMillis The number of milliseconds for which the dialog is displayed,
 * must be positive. Suggested values are [DialogDefaults.ShortDurationMillis],
 * [DialogDefaults.LongDurationMillis] or [DialogDefaults.IndefiniteDurationMillis].
 * @param backgroundColor [Color] representing the background color for this dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param iconColor Icon [Color] that defaults to the [contentColor],
 * unless specifically overridden.
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for the dialog title, expected to be one line of text.
 */
@Composable
public fun Confirmation(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: ScalingLazyListState = rememberScalingLazyListState(),
    durationMillis: Long = DialogDefaults.ShortDurationMillis,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.ConfirmationVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    require(durationMillis > 0) { "Duration must be a positive integer" }

    // Always refer to the latest inputs with which ConfirmationDialog was recomposed.
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(durationMillis) {
        delay(durationMillis)
        currentOnTimeout()
    }
    DialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(
                titleColor = contentColor,
                padding = DialogDefaults.TitleBottomPadding,
                content = content
            )
        }
    }
}

/**
 * [Confirmation] lays out the content for an opinionated confirmation screen that
 * displays a message to the user for [durationMillis]. It has a slot for an icon or image
 * (which could be animated).
 *
 * [Confirmation] can be used as a destination in a navigation graph
 * e.g. using SwipeDismissableNavHost. However, for a conventional fullscreen dialog,
 * displayed on top of other content, use [Dialog].
 *
 * Example of a [Confirmation] with animation:
 * @sample androidx.wear.compose.material.samples.ConfirmationWithAnimation
 *
 * @param onTimeout Event invoked when the dialog has been shown for [durationMillis].
 * @param modifier Modifier to be applied to the dialog.
 * @param icon An optional slot for displaying an icon or image.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param durationMillis The number of milliseconds for which the dialog is displayed,
 * must be positive. Suggested values are [DialogDefaults.ShortDurationMillis],
 * [DialogDefaults.LongDurationMillis] or [DialogDefaults.IndefiniteDurationMillis].
 * @param backgroundColor [Color] representing the background color for this dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param iconColor Icon [Color] that defaults to the [contentColor],
 * unless specifically overridden.
 * @param verticalArrangement The vertical arrangement of the dialog's children. This allows us
 * to add spacing between items and specify the arrangement of the items when we have not enough
 * of them to fill the whole minimum size.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for the dialog title, expected to be one line of text.
 */
@Suppress("DEPRECATION")
@Deprecated(
        "This overload is provided for backwards compatibility with Compose for Wear OS 1.1." +
        "A newer overload is available which uses ScalingLazyListState from " +
        "wear.compose.foundation.lazy package", level = DeprecationLevel.HIDDEN
)
@Composable
public fun Confirmation(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    durationMillis: Long = DialogDefaults.ShortDurationMillis,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.ConfirmationVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    ConfirmationWithMaterialSlc(
        onTimeout = onTimeout,
        modifier = modifier,
        icon = icon,
        scrollState = scrollState,
        durationMillis = durationMillis,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        iconColor = iconColor,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * @VisibleForTesting
 */
@Suppress("DEPRECATION")
@Deprecated("Used only for testing")
@Composable
internal fun ConfirmationWithMaterialSlc(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (ColumnScope.() -> Unit)? = null,
    scrollState: androidx.wear.compose.material.ScalingLazyListState =
        androidx.wear.compose.material.rememberScalingLazyListState(),
    durationMillis: Long = DialogDefaults.ShortDurationMillis,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconColor: Color = contentColor,
    verticalArrangement: Arrangement.Vertical = DialogDefaults.ConfirmationVerticalArrangement,
    contentPadding: PaddingValues = DialogDefaults.ContentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    require(durationMillis > 0) { "Duration must be a positive integer" }

    // Always refer to the latest inputs with which ConfirmationDialog was recomposed.
    val currentOnTimeout by rememberUpdatedState(onTimeout)

    LaunchedEffect(durationMillis) {
        delay(durationMillis)
        currentOnTimeout()
    }
    MaterialDialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
    ) {
        if (icon != null) {
            item {
                DialogIconHeader(iconColor, content = icon)
            }
        }

        item {
            DialogTitle(
                titleColor = contentColor,
                padding = DialogDefaults.TitleBottomPadding,
                content = content
            )
        }
    }
}

/**
 * Contains the default values used by [Alert] and [Confirmation].
 */
public object DialogDefaults {
    /**
     * Creates the recommended vertical arrangement for [Alert] dialog content.
     */
    public val AlertVerticalArrangement =
        Arrangement.spacedBy(4.dp, Alignment.CenterVertically)

    /**
     * Creates the recommended vertical arrangement for [Confirmation] dialog content.
     */
    public val ConfirmationVerticalArrangement =
        Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterVertically)

    /**
     * The padding to apply around the contents.
     */
    public val ContentPadding = PaddingValues(horizontal = 10.dp)

    /**
     * Short duration for showing [Confirmation].
     */
    public val ShortDurationMillis = 4000L

    /**
     * Long duration for showing [Confirmation].
     */
    public val LongDurationMillis = 10000L

    /**
     * Show [Confirmation] indefinitely (supports swipe-to-dismiss).
     */
    public val IndefiniteDurationMillis = Long.MAX_VALUE

    /**
     * Spacing between [Button]s.
     */
    internal val ButtonSpacing = 12.dp

    /**
     * Spacing below [Icon].
     */
    internal val IconSpacing = 4.dp

    /**
     * Padding around body content.
     */
    internal val BodyPadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 12.dp)
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 12.dp)

    /**
     * Padding around title text.
     */
    internal val TitlePadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 0.dp,
                    bottom = 8.dp
                )
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 8.dp)

    /**
     * Bottom padding for title text.
     */
    internal val TitleBottomPadding
        @Composable get() =
            PaddingValues(start = 0.dp, end = 0.dp, top = 0.dp, bottom = 16.dp)
}

/**
 * Common Wear Material dialog implementation that offers a single content slot,
 * fills the screen and is scrollable by default if the content is taller than the viewport.
 */
@Composable
private fun DialogImpl(
    modifier: Modifier = Modifier,
    scrollState: ScalingLazyListState,
    verticalArrangement: Arrangement.Vertical,
    backgroundColor: Color,
    contentPadding: PaddingValues,
    content: ScalingLazyListScope.() -> Unit
) {
    ScalingLazyColumn(
        state = scrollState,
        autoCentering = null,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        content = content
    )
}

/**
 * Common Wear Material dialog implementation that offers a single content slot,
 * fills the screen and is scrollable by default if the content is taller than the viewport.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Use [DialogImpl] targeting " +
        "androidx.wear.compose.foundation.lazy ScalingLazyColumn instead"
)
@Composable
private fun MaterialDialogImpl(
    modifier: Modifier = Modifier,
    scrollState: androidx.wear.compose.material.ScalingLazyListState,
    verticalArrangement: Arrangement.Vertical,
    backgroundColor: Color,
    contentPadding: PaddingValues,
    content: androidx.wear.compose.material.ScalingLazyListScope.() -> Unit
) {
    androidx.wear.compose.material.ScalingLazyColumn(
        state = scrollState,
        autoCentering = null,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        content = content
    )
}

/**
 * [DialogIconHeader] displays an icon at the top of the dialog
 * followed by the recommended spacing.
 *
 * @param iconColor [Color] in which to tint the icon.
 * @param content Slot for an icon.
 */
@Composable
private fun DialogIconHeader(
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides iconColor) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .height(DialogDefaults.IconSpacing)
            )
        }
    }
}

/**
 * [DialogTitle] displays the title content in a dialog with the recommended padding.
 *
 * @param titleColor [Color] in which the title is displayed.
 * @param padding The padding around the title content.
 */
@Composable
private fun DialogTitle(
    titleColor: Color,
    padding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides titleColor,
        LocalTextStyle provides MaterialTheme.typography.title3
    ) {
        Column(
            modifier = Modifier.padding(padding),
            content = content,
        )
    }
}

/**
 * [DialogBody] displays the body content in a dialog with recommended padding.
 */
@Composable
private fun DialogBody(
    bodyColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides bodyColor,
        LocalTextStyle provides MaterialTheme.typography.body2
    ) {
        Column(
            modifier = Modifier.padding(DialogDefaults.BodyPadding),
            content = content
        )
    }
}
