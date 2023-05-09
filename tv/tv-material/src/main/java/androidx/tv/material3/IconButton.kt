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

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Material Design standard icon button for TV.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * [content] should typically be an [Icon]. If using a custom icon, note that the typical size for
 * the internal icon is 24 x 24 dp.
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
 * @param border Defines a border around the Button.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param content the content of the button, typically an [Icon]
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: ButtonScale = IconButtonDefaults.scale(),
    glow: ButtonGlow = IconButtonDefaults.glow(),
    shape: ButtonShape = IconButtonDefaults.shape(),
    colors: ButtonColors = IconButtonDefaults.colors(),
    border: ButtonBorder = IconButtonDefaults.border(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .semantics { role = Role.Button }
            .size(IconButtonDefaults.MediumButtonSize),
        onClick = onClick,
        enabled = enabled,
        shape = shape.toClickableSurfaceShape(),
        colors = colors.toClickableSurfaceColors(),
        scale = scale.toClickableSurfaceScale(),
        border = border.toClickableSurfaceBorder(),
        glow = glow.toClickableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * Material Design standard icon button for TV.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * [content] should typically be an [Icon]. If using a custom icon, note that the typical size for
 * the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param scale Defines size of the Button relative to its original size
 * @param glow Shadow to be shown behind the Button.
 * @param shape Defines the Button's shape.
 * @param colors Color to be used for background and content of the Button
 * @param border Defines a border around the Button.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param content the content of the button, typically an [Icon]
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: ButtonScale = OutlinedIconButtonDefaults.scale(),
    glow: ButtonGlow = OutlinedIconButtonDefaults.glow(),
    shape: ButtonShape = OutlinedIconButtonDefaults.shape(),
    colors: ButtonColors = OutlinedIconButtonDefaults.colors(),
    border: ButtonBorder = OutlinedIconButtonDefaults.border(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .semantics { role = Role.Button }
            .size(OutlinedIconButtonDefaults.MediumButtonSize),
        onClick = onClick,
        enabled = enabled,
        shape = shape.toClickableSurfaceShape(),
        colors = colors.toClickableSurfaceColors(),
        scale = scale.toClickableSurfaceScale(),
        border = border.toClickableSurfaceBorder(),
        glow = glow.toClickableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
