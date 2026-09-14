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

package androidx.xr.glimmer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Displays an urgent prompt requiring the user to acknowledge or take an action.
 *
 * Alert dialogs interrupt the user to communicate critical information, confirm decisions (such as
 * discarding changes or resetting preferences), or acknowledge important system status. The dialog
 * dims the background to focus attention on the prompt and displays actions directly below the
 * message.
 *
 * An alert dialog with confirm and dismiss actions:
 *
 * @sample androidx.xr.glimmer.samples.AlertDialogSample
 *
 * An alert dialog with only a confirm action:
 *
 * @sample androidx.xr.glimmer.samples.AlertDialogConfirmOnlySample
 * @param onDismissRequest called when the user tries to dismiss the dialog
 * @param confirmButton button used to confirm or accept the action. This will be the first button
 *   shown and takes focus upon the dialog being opened
 * @param text body text providing additional details or explanation for the dialog, placed below
 *   the [title]
 * @param modifier the [Modifier] to be applied to the dialog
 * @param dismissButton optional button used to dismiss or cancel the alert dialog
 * @param icon optional icon displayed at the top of the dialog, above the [title]
 * @param title optional title displaying the primary headline of the dialog, placed below the
 *   [icon] and above the [text]
 * @param shape the [Shape] of the dialog container
 * @param containerColor the background [Color] of the dialog container
 * @param contentColor the default [Color] for content inside the dialog container
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 */
@Composable
public fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    contentColor: Color = AlertDialogDefaults.contentColor(containerColor),
    contentPadding: PaddingValues = AlertDialogDefaults.contentPadding,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = AlertDialogProperties) {
        val typography = GlimmerTheme.typography
        val textStyle = typography.bodySmall.copy(textAlign = TextAlign.Start)
        val componentSpacingValues = GlimmerTheme.componentSpacingValues
        val innerPadding = componentSpacingValues.small
        val buttonSpacing = componentSpacingValues.large

        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(buttonSpacing, Alignment.CenterVertically),
        ) {
            Card(
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxWidth().focusProperties { canFocus = false },
            ) {
                Column(
                    modifier = Modifier.padding(innerPadding),
                    verticalArrangement =
                        Arrangement.spacedBy(TextVerticalSpacing, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (icon != null) {
                        val iconSize = GlimmerTheme.iconSizes.small
                        CompositionLocalProvider(LocalIconSize provides iconSize, content = icon)
                    }

                    if (title != null) {
                        val titleStyle = typography.bodyMedium.copy(textAlign = TextAlign.Center)
                        CompositionLocalProvider(
                            LocalTextStyle provides titleStyle,
                            content = title,
                        )
                    }

                    CompositionLocalProvider(LocalTextStyle provides textStyle, content = text)
                }
            }

            ButtonGroup {
                confirmButton()
                if (dismissButton != null) {
                    dismissButton()
                }
            }
        }
    }
}

/** Default values and styling for [AlertDialog]. */
public object AlertDialogDefaults {
    /** Default content padding applied to the dialog content. */
    public val contentPadding: PaddingValues
        @Composable get() = CardDefaults.contentPadding

    /** Default shape for the dialog container. */
    public val shape: Shape
        @Composable get() = CardDefaults.shape

    /** Default background color for the dialog container. */
    public val containerColor: Color
        @Composable get() = GlimmerTheme.colors.surface

    /**
     * Calculates the default content color for an [AlertDialog] based on the provided
     * [containerColor].
     *
     * @param containerColor the color of the alert dialog container
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(containerColor: Color = GlimmerTheme.colors.surface): Color {
        return calculateContentColor(containerColor)
    }
}

/** Spacing between icon, title, and body text */
private val TextVerticalSpacing = 3.dp

private val AlertDialogProperties =
    DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
        scrimAlpha = 1.0f,
    )
