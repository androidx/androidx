/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * [AlertDialog] is an opinionated, full screen dialog. This overload offers 5 slots for title,
 * negative button, positive button, optional icon and optional content.
 * The buttons are shown side-by-side below the icon, text and content.
 * [AlertDialog] is scrollable by default if the content is taller than the viewport.
 *
 * Example of an [AlertDialog] with an icon, title, body text and buttons:
 * @sample androidx.wear.compose.material.samples.AlertDialogWithButtons
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param negativeButton A slot for a [Button] indicating negative sentiment (e.g. No).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param positiveButton A slot for a [Button] indicating positive sentiment (e.g. Yes).
 * Clicking the button must remove the dialog from the composition hierarchy.
 * @param modifier Modifier to be applied to the dialog.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param titleColor [Color] representing the color for [title].
 * @param iconTintColor Icon [Color] that defaults to [contentColor],
 * unless specifically overridden.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for additional content, expected to be 2-3 lines of text.
 */
@Composable
public fun AlertDialog(
    title: @Composable () -> Unit,
    negativeButton: @Composable () -> Unit,
    positiveButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    titleColor: Color = contentColor,
    iconTintColor: Color = contentColor,
    contentPadding: PaddingValues = DialogDefaults.ButtonsContentPadding,
    content: @Composable (() -> Unit)? = null
) {
    DialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    ) {
        val weightsToCenterVertically = 0.5f

        // Use 50:50 weights to center the title + content between icon and buttons
        Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))

        if (icon != null) {
            DialogIconHeader(iconTintColor, content = icon)
        }

        DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, title)

        if (content != null) {
            DialogBody(content)
        }

        // Use 50:50 weights to center the title + content between icon and buttons
        Spacer(Modifier.fillMaxWidth().weight(weightsToCenterVertically))

        // Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            negativeButton()
            Spacer(modifier = Modifier.width(DialogDefaults.ButtonSpacing))
            positiveButton()
        }
    }
}

/**
 * [AlertDialog] is an opinionated, full screen dialog.
 * This overload offers 4 slots for title, optional icon, optional message text and
 * a content slot expected to be one or more vertically stacked [Chip]s or [ToggleChip]s.
 * [AlertDialog] is scrollable by default if the content is taller than the viewport.
 *
 * Example of an [AlertDialog] with an icon, title, message text and chips:
 * @sample androidx.wear.compose.material.samples.AlertDialogWithChips
 *
 * @param title A slot for displaying the title of the dialog,
 * expected to be one or two lines of text.
 * @param modifier Modifier to be applied to the dialog.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param message Optional slot for additional message content, expected to be 2-3 lines of text.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param backgroundColor [Color] representing the background color for the dialog.
 * @param contentColor [Color] representing the color for [content].
 * @param titleColor [Color] representing the color for [title].
 * @param iconTintColor Icon [Color] that defaults to [contentColor],
 * unless specifically overridden.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for one or more spaced [Chip]s, stacked vertically.
 */
@Composable
public fun AlertDialog(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    message: @Composable (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    titleColor: Color = contentColor,
    iconTintColor: Color = contentColor,
    contentPadding: PaddingValues = DialogDefaults.ChipsContentPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    DialogImpl(
        modifier = modifier,
        scrollState = scrollState,
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    ) {
        if (icon != null) {
            DialogIconHeader(iconTintColor, content = icon)
        }

        DialogTitle(titleColor, padding = DialogDefaults.TitlePadding, content = title)

        if (message != null) {
            DialogBody(message)
        }

        content()
    }
}

/**
 * [ConfirmationDialog] is an opinionated dialog that displays a message to the user
 * for [durationMillis]. It has a slot for an icon or image (which could be animated).
 *
 * [ConfirmationDialog] can also be acknowledged via swipe-to-dismiss if the dialog is
 * displayed as a destination in a navigation graph e.g. using SwipeDismissableNavHost.
 *
 * Example of a [ConfirmationDialog] with animation:
 * @sample androidx.wear.compose.material.samples.ConfirmationDialogWithAnimation
 *
 * @param onTimeout Event invoked when the dialog has been shown for [durationMillis].
 * Must remove the [ConfirmationDialog] from the composition.
 * @param modifier Modifier to be applied to the dialog.
 * @param icon An optional slot for displaying an icon or image.
 * @param scrollState The scroll state for the dialog so that the scroll position can be displayed
 * e.g. by the [PositionIndicator] passed to [Scaffold].
 * @param durationMillis The number of milliseconds for which the dialog is displayed,
 * must be positive. Suggested values are [DialogDefaults.ShortDurationMillis],
 * [DialogDefaults.LongDurationMillis] or [DialogDefaults.IndefiniteDurationMillis].
 * @param backgroundColor [Color] representing the background color for this dialog.
 * @param contentColor [Color] representing the content color for this dialog.
 * @param iconTintColor Icon [Color] that defaults to the [contentColor],
 * unless specifically overridden.
 * @param contentPadding The padding to apply around the whole of the dialog's contents.
 * @param content A slot for the dialog title, expected to be one line of text.
 */
@Composable
public fun ConfirmationDialog(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    durationMillis: Long = DialogDefaults.ShortDurationMillis,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    iconTintColor: Color = contentColor,
    contentPadding: PaddingValues = DialogDefaults.ConfirmationContentPadding,
    content: @Composable () -> Unit
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
        contentPadding = contentPadding,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
    ) {
        if (icon != null) {
            DialogIconHeader(iconTintColor, content = icon)
        }

        DialogTitle(
            titleColor = contentColor,
            padding = DialogDefaults.TitleBottomPadding,
            content = content)
    }
}

/**
 * Contains the default values used by [AlertDialog] and [ConfirmationDialog].
 */
public object DialogDefaults {
    /**
     * Creates the recommended contentPadding for [AlertDialog] with chips, used to define
     * the spacing around content that may contain several chips.
     */
    public val ChipsContentPadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 70.dp)
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 20.dp, bottom = 64.dp)

    /**
     * Creates the recommended contentPadding for [AlertDialog] with buttons, used to define
     * the spacing around content for a dialog with icon, title, optional message text
     * and two buttons.
     */
    public val ButtonsContentPadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(start = 10.dp, end = 10.dp, top = 24.dp, bottom = 40.dp)
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 20.dp, bottom = 38.dp)

    /**
     * Creates the recommended contentPadding for [ConfirmationDialog],
     * used to define the spacing around content for a dialog with optional icon and title.
     */
    public val ConfirmationContentPadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(start = 14.dp, end = 14.dp, top = 24.dp, bottom = 24.dp)
            else
                PaddingValues(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 20.dp)

    /**
     * Short duration for showing [ConfirmationDialog].
     */
    val ShortDurationMillis = 4000L

    /**
     * Long duration for showing [ConfirmationDialog].
     */
    val LongDurationMillis = 10000L

    /**
     * Show [ConfirmationDialog] indefinitely (supports swipe-to-dismiss).
     */
    val IndefiniteDurationMillis = Long.MAX_VALUE

    /**
     * Spacing between [Button]s.
     */
    internal val ButtonSpacing = 12.dp

    /**
     * Spacing below [Icon].
     */
    internal val IconSpacing = 8.dp

    /**
     * Padding around body content.
     */
    internal val BodyPadding
        @Composable get() =
            if (isRoundDevice())
                PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp)
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 4.dp, bottom = 16.dp)

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
                    bottom = 12.dp
                )
            else
                PaddingValues(start = 5.dp, end = 5.dp, top = 0.dp, bottom = 12.dp)

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
    scrollState: ScrollState,
    backgroundColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(state = scrollState)
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * [DialogIconHeader] displays an icon at the top of the dialog
 * followed by the recommended spacing.
 *
 * @param iconTintColor [Color] in which to tint the icon.
 * @param content Slot for an icon.
 */
@Composable
private fun DialogIconHeader(
    iconTintColor: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides iconTintColor) {
        content()
        Spacer(Modifier.fillMaxWidth().height(DialogDefaults.IconSpacing))
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
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides titleColor,
        LocalTextStyle provides MaterialTheme.typography.title3
    ) {
        Column(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

/**
 * [DialogBody] displays the body content in a dialog with recommended padding.
 */
@Composable
private fun DialogBody(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.body2
    ) {
        Column(modifier = Modifier.padding(DialogDefaults.BodyPadding)) {
            content()
        }
    }
}
