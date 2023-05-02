/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.tokens.Elevation

/**
 * Material Design filled button for TV.
 *
 * Filled buttons are for high emphasis (important, final actions that complete a flow).
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for high emphasis (important, final actions that complete a flow).
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param scale Defines size of the Button relative to its original size.
 * @param glow Shadow to be shown behind the Button.
 * @param shape Defines the Button's shape.
 * @param colors Color to be used for background and content of the Button
 * @param tonalElevation tonal elevation used to apply a color shift to the button to give the it
 * higher emphasis
 * @param border Defines a border around the Button.
 * @param contentPadding the spacing values to apply internally between the container and the
 * content
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param content the content of the button
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: ButtonScale = ButtonDefaults.scale(),
    glow: ButtonGlow = ButtonDefaults.glow(),
    shape: ButtonShape = ButtonDefaults.shape(),
    colors: ButtonColors = ButtonDefaults.colors(),
    tonalElevation: Dp = Elevation.Level0,
    border: ButtonBorder = ButtonDefaults.border(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    ButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        scale = scale,
        glow = glow,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Material Design outlined button for TV.
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app. Outlined buttons pair well with [Button]s to indicate an
 * alternative, secondary action.
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 *
 * - See [Button] for high emphasis (important, final actions that complete a flow).
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param scale Defines size of the Button relative to its original size.
 * @param glow Shadow to be shown behind the Button.
 * @param shape Defines the Button's shape.
 * @param colors Color to be used for background and content of the Button
 * @param tonalElevation tonal elevation used to apply a color shift to the button to give the it
 * higher emphasis
 * @param border Defines a border around the Button.
 * @param contentPadding the spacing values to apply internally between the container and the
 * content
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param content the content of the button
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: ButtonScale = OutlinedButtonDefaults.scale(),
    glow: ButtonGlow = OutlinedButtonDefaults.glow(),
    shape: ButtonShape = OutlinedButtonDefaults.shape(),
    colors: ButtonColors = OutlinedButtonDefaults.colors(),
    tonalElevation: Dp = Elevation.Level0,
    border: ButtonBorder = OutlinedButtonDefaults.border(),
    contentPadding: PaddingValues = OutlinedButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    ButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        scale = scale,
        glow = glow,
        shape = shape,
        colors = colors,
        tonalElevation = tonalElevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@ExperimentalTvMaterial3Api
@Composable
private fun ButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    scale: ButtonScale,
    glow: ButtonGlow,
    shape: ButtonShape,
    colors: ButtonColors,
    tonalElevation: Dp,
    border: ButtonBorder,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.semantics { role = Role.Button },
        onClick = onClick,
        enabled = enabled,
        scale = scale.toClickableSurfaceScale(),
        glow = glow.toClickableSurfaceGlow(),
        shape = shape.toClickableSurfaceShape(),
        colors = colors.toClickableSurfaceColors(),
        tonalElevation = tonalElevation,
        border = border.toClickableSurfaceBorder(),
        interactionSource = interactionSource
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            Row(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = BaseButtonDefaults.MinWidth,
                        minHeight = BaseButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}
