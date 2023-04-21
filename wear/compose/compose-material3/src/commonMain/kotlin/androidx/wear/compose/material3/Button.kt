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

package androidx.wear.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Base level Wear Material3 [Button] that offers a single slot to take any content.
 * Used as the container for more opinionated [Button] components that take specific
 * content such as icons and labels.
 *
 * The [Button] is Stadium-shaped by default and has a max height designed to take no more than
 * two lines of text of [Typography.buttonMedium] style. The [Button] can have an icon or image
 * horizontally parallel to the two lines of text.
 *
 * [Button] takes the [ButtonDefaults.filledButtonColors] color scheme by default,
 * with colored background, contrasting content color and no border. This is a high-emphasis button
 * for the primary, most important or most common action on a screen.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors] and
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.filledButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this
 * button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.filledButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Chip(
        modifier = modifier.height(ButtonDefaults.Height),
        onClick = onClick,
        background = { colors.containerPainter(enabled = it) },
        border = { rememberUpdatedState(border) },
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        interactionSource = interactionSource,
        role = role,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.buttonMedium,
            content
        )
    )
}

/**
 * Base level Wear Material3 [FilledTonalButton] that offers a single slot to take any content.
 * Used as the container for more opinionated [FilledTonalButton] components that take specific
 * content such as icons and labels.
 *
 * The [FilledTonalButton] is Stadium-shaped by default and has a max height designed to take no
 * more than two lines of text of [Typography.buttonMedium] style. The [FilledTonalButton] can have
 * an icon or image horizontally parallel to the two lines of text.
 *
 * [FilledTonalButton] takes the [ButtonDefaults.filledTonalButtonColors] color scheme by default,
 * with muted background, contrasting content color and no border. This is a medium-emphasis button
 * for important actions that don't distract from other onscreen elements, such as final or
 * unblocking actions in a flow with less emphasis than [Button].
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors] and
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.filledTonalButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this
 * button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, role,
    content
)

/**
 * Base level Wear Material3 [OutlinedButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [OutlinedButton] components that take specific content such
 * as icons and labels.
 *
 * The [OutlinedButton] is Stadium-shaped by default and has a max height designed to take no more
 * than two lines of text of [Typography.buttonMedium] style. The [OutlinedButton] can have an
 * icon or image horizontally parallel to the two lines of text.
 *
 * [OutlinedButton] takes the [ButtonDefaults.outlinedButtonColors] color scheme by default,
 * with a transparent background and a thin border. This is a medium-emphasis button
 * for important, non-primary actions that need attention.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this
 * button in different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, role,
    content
)

/**
 * Base level Wear Material3 [ChildButton] that offers a single slot to take any content. Used as
 * the container for more opinionated [ChildButton] components that take specific content such
 * as icons and labels.
 *
 * The [ChildButton] is Stadium-shaped by default and has a max height designed to take no more than
 * two lines of text of [Typography.buttonMedium] style. The [ChildButton] can have an icon or image
 * horizontally parallel to the two lines of text.
 *
 * [ChildButton] takes the [ButtonDefaults.childButtonColors] color scheme by default,
 * with a transparent background and no border. This is a low-emphasis button for optional
 * or supplementary actions with the least amount of prominence.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors] and
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * Button can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.childButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the border for this
 * button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun ChildButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.childButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) = Button(
    onClick, modifier, enabled, shape, colors, border, contentPadding, interactionSource, role,
    content
)

/**
 * Wear Material3 [Button] that offers three slots and a specific layout for an
 * icon, label and secondaryLabel. The icon and secondaryLabel are optional.
 * The items are laid out with the icon, if provided, at the start of a row, with a column next
 * containing the two label slots.
 *
 * The [Button] is Stadium-shaped by default and has a max height designed to take no more than
 * two lines of text of [Typography.buttonMedium] style. If no secondary label is provided then
 * the label can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [Button] takes the [ButtonDefaults.filledButtonColors] color scheme by default,
 * with colored background, contrasting content color and no border. This is a high-emphasis button
 * for the primary, most important or most common action on a screen.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors] and
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * [Button] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned if there is an icon preset and
 * "start" or "center" aligned if not.
 * label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 * horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 * [ButtonDefaults.LargeIconSize]. In order to correctly render when the Button is not enabled,
 * the icon must set its alpha value to [LocalContentAlpha].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.buttonColors]. Defaults to
 * [ButtonDefaults.filledButtonColors]
 * @param border Optional [BorderStroke] that will be used to resolve the button border in
 * different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun Button(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.filledButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
) {
    androidx.wear.compose.materialcore.Chip(
        modifier = modifier.height(ButtonDefaults.Height),
        onClick = onClick,
        background = { colors.containerPainter(enabled = it) },
        border = { rememberUpdatedState(border) },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        contentPadding = contentPadding,
        label = provideScopeContent(
            colors.contentColor(enabled),
            MaterialTheme.typography.buttonMedium,
            label
        ),
        secondaryLabel = secondaryLabel?.let { provideScopeContent(
            colors.secondaryContentColor(enabled),
            MaterialTheme.typography.captionLarge,
            secondaryLabel
        ) },
        icon = icon?.let {
            provideScopeContent(colors.iconColor(enabled), icon)
        },
        defaultIconSpacing = ButtonDefaults.IconSpacing,
        role = role
    )
}

/**
 * Wear Material3 [FilledTonalButton] that offers three slots and a specific layout for an
 * icon, label and secondaryLabel. The icon and secondaryLabel are optional.
 * The items are laid out with the icon, if provided, at the start of a row, with a column next
 * containing the two label slots.
 *
 * The [FilledTonalButton] is Stadium-shaped by default and has a max height designed to take no
 * more than two lines of text of [Typography.buttonMedium] style. If no secondary label is provided
 * then the label can be two lines of text. The label and secondary label should be consistently
 * aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [FilledTonalButton] takes the [ButtonDefaults.filledTonalButtonColors] color scheme by default,
 * with muted background, contrasting content color and no border. This is a medium-emphasis button
 * for important actions that don't distract from other onscreen elements, such as final or
 * unblocking actions in a flow with less emphasis than [Button].
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors] and
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * [FilledTonalButton] can be enabled or disabled. A disabled button will not respond to
 * click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned if there is an icon preset and
 * "start" or "center" aligned if not.
 * label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 * horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 * [ButtonDefaults.LargeIconSize]. In order to correctly render when the Button is not enabled,
 * the icon must set its alpha value to [LocalContentAlpha].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.filledTonalButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in
 * different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun FilledTonalButton(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
) = Button(
    label,
    onClick,
    modifier,
    secondaryLabel,
    icon,
    enabled,
    shape,
    colors,
    border,
    contentPadding,
    interactionSource,
    role
)

/**
 * Wear Material3 [OutlinedButton] that offers three slots and a specific layout for an
 * icon, label and secondaryLabel. The icon and secondaryLabel are optional.
 * The items are laid out with the icon, if provided, at the start of a row, with a column next
 * containing the two label slots.
 *
 * The [OutlinedButton] is Stadium-shaped by default and has a max height designed to take no more
 * than two lines of text of [Typography.buttonMedium] style. If no secondary label is provided then
 * the label can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [OutlinedButton] takes the [ButtonDefaults.outlinedButtonColors] color scheme by default,
 * with a transparent background and a thin border. This is a medium-emphasis button
 * for important, non-primary actions that need attention.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [ChildButton] which defaults to [ButtonDefaults.childButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * [OutlinedButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned if there is an icon preset and
 * "start" or "center" aligned if not.
 * label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 * horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 * [ButtonDefaults.LargeIconSize]. In order to correctly render when the Button is not enabled,
 * the icon must set its alpha value to [LocalContentAlpha].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.outlinedButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in
 * different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun OutlinedButton(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
) = Button(
    label,
    onClick,
    modifier,
    secondaryLabel,
    icon,
    enabled,
    shape,
    colors,
    border,
    contentPadding,
    interactionSource,
    role
)

/**
 * Wear Material3 [ChildButton] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [ChildButton] is Stadium-shaped by default and has a max height designed to take no more than
 * two lines of text of [Typography.buttonMedium] style. If no secondary label is provided then
 * the label can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * [ChildButton] takes the [ButtonDefaults.childButtonColors] color scheme by default,
 * with a transparent background and no border. This is a low-emphasis button for optional
 * or supplementary actions with the least amount of prominence.
 *
 * Other recommended buttons with [ButtonColors] for different levels of emphasis are:
 * [Button] which defaults to [ButtonDefaults.filledButtonColors],
 * [FilledTonalButton] which defaults to [ButtonDefaults.filledTonalButtonColors],
 * [OutlinedButton] which defaults to [ButtonDefaults.outlinedButtonColors].
 * Buttons can also take an image background using [ButtonDefaults.imageBackgroundButtonColors].
 *
 * [Button] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the button
 * @param modifier Modifier to be applied to the button
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned if there is an icon preset and
 * "start" or "center" aligned if not.
 * label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the button's icon. The contents are expected to be a
 * horizontally and vertically aligned icon of size [ButtonDefaults.IconSize] or
 * [ButtonDefaults.LargeIconSize]. In order to correctly render when the Button is not enabled,
 * the icon must set its alpha value to [LocalContentAlpha].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material3 Theme
 * @param colors [ButtonColors] that will be used to resolve the background and content color for
 * this button in different states. See [ButtonDefaults.childButtonColors].
 * @param border Optional [BorderStroke] that will be used to resolve the button border in
 * different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@Composable
fun ChildButton(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.full,
    colors: ButtonColors = ButtonDefaults.childButtonColors(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
) = Button(
    label,
    onClick,
    modifier,
    secondaryLabel,
    icon,
    enabled,
    shape,
    colors,
    border,
    contentPadding,
    interactionSource,
    role
)

/**
 * Contains the default values used by [Button]
 */
object ButtonDefaults {
    /**
     * Creates a [ButtonColors] with colored background and contrasting content color,
     * the defaults for high emphasis buttons like [Button], for the primary, most important
     * or most common action on a screen. If a button is disabled then the content will have
     * an alpha([ContentAlpha.disabled]) value applied and container/border colors will be
     * muted.
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     */
    @Composable
    fun filledButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        secondaryContentColor: Color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
        iconColor: Color = MaterialTheme.colorScheme.onPrimary
    ): ButtonColors {
        return buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor
        )
    }

    /**
     * Creates a [ButtonColors] with a muted background and contrasting content color,
     * the defaults for medium emphasis buttons like [FilledTonalButton].
     * Use [filledTonalButtonColors] for important actions that don't distract from
     * other onscreen elements, such as final or unblocking actions in a flow with less emphasis
     * than [filledButtonColors].
     *
     * If a button is disabled then the content will have an alpha([ContentAlpha.disabled])
     * value applied and container/border colors will be muted.
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     */
    @Composable
    fun filledTonalButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        secondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurface
    ): ButtonColors {
        return buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor
        )
    }

    /**
     * Creates a [ButtonColors] with a transparent background (typically paired with
     * [ButtonDefaults.outlinedButtonBorder]), the defaults for medium emphasis buttons
     * like [OutlinedButton], for important, non-primary actions that need attention.
     *
     * If a button is disabled then the content will have an alpha([ContentAlpha.disabled])
     * value applied and container/border colors will be muted.
     *
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     */
    @Composable
    fun outlinedButtonColors(
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        secondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurface
    ): ButtonColors {
        return buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor
        )
    }

    /**
     * Creates a [ButtonColors] with transparent background, the defaults for low emphasis
     * buttons like [ChildButton]. Use [childButtonColors] for optional or supplementary
     * actions with the least amount of prominence.
     *
     * If a button is disabled then the content will have an alpha([ContentAlpha.disabled])
     * value applied and container/border colors will be muted.
     *
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     */
    @Composable
    fun childButtonColors(
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        secondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        iconColor: Color = MaterialTheme.colorScheme.onSurface
    ): ButtonColors {
        return buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor
        )
    }

    /**
     * Creates a [ButtonColors] for a [Button] with an image background, typically with a scrim
     * over the image to ensure that the content is visible. Uses a default content color
     * of [ColorScheme.onSurface].
     *
     * @param backgroundImagePainter The [Painter] to use to draw the background of the [Button]
     * @param backgroundImageScrimBrush The [Brush] to use to paint a scrim over the background
     * image to ensure that any text drawn over the image is legible
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The secondary content color of this [Button] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Button] when enabled, used for icon content
     */
    @Composable
    fun imageBackgroundButtonColors(
        backgroundImagePainter: Painter,
        backgroundImageScrimBrush: Brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 1.0f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0f)
            )
        ),
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
    ): ButtonColors {
        val backgroundPainter =
            remember(backgroundImagePainter, backgroundImageScrimBrush) {
                androidx.wear.compose.materialcore.ImageWithScrimPainter(
                    imagePainter = backgroundImagePainter,
                    brush = backgroundImageScrimBrush
                )
            }

        val disabledContentAlpha = ContentAlpha.disabled
        val disabledBackgroundPainter =
            remember(backgroundImagePainter, backgroundImageScrimBrush, disabledContentAlpha) {
                androidx.wear.compose.materialcore.ImageWithScrimPainter(
                    imagePainter = backgroundImagePainter,
                    brush = backgroundImageScrimBrush,
                    alpha = disabledContentAlpha,
                )
            }
        return ButtonColors(
            containerPainter = backgroundPainter,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContainerPainter = disabledBackgroundPainter,
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor = secondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledIconColor = iconColor.copy(alpha = ContentAlpha.disabled),
        )
    }

    /**
     * Creates a [BorderStroke], such as for an [OutlinedButton]
     *
     * @param borderColor The color to use for the border for this outline when enabled
     * @param disabledBorderColor The color to use for the border for this outline when
     * disabled
     * @param borderWidth The width to use for the border for this outline. It is strongly
     * recommended to use the default width as this outline is a key characteristic
     * of Wear Material3.
     */
    @Composable
    fun outlinedButtonBorder(
        enabled: Boolean,
        borderColor: Color = MaterialTheme.colorScheme.outline,
        disabledBorderColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DisabledBorderAndContainerAlpha
        ),
        borderWidth: Dp = 1.dp
    ): BorderStroke {
        return remember {
            BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)
        }
    }

    private val ButtonHorizontalPadding = 14.dp
    private val ButtonVerticalPadding = 6.dp

    /**
     * The default content padding used by [Button]
     */
    val ContentPadding: PaddingValues = PaddingValues(
        horizontal = ButtonHorizontalPadding,
        vertical = ButtonVerticalPadding,
    )

    /**
     * The default size of the icon when used inside a [Button].
     */
    val IconSize: Dp = 24.dp

    /**
     * The size of the icon when used inside a Large "Avatar" [Button].
     */
    val LargeIconSize: Dp = 32.dp

    /**
     * Creates a [ButtonColors] that represents the default background and content colors used in
     * a [Button].
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The content color of this [Button] when enabled
     * @param iconColor The content color of this [Button] when enabled
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Button] when not enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    @Composable
    fun buttonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = contentColorFor(containerColor),
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DisabledBorderAndContainerAlpha
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = ContentAlpha.disabled
        ),
        disabledSecondaryContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = ContentAlpha.disabled
        ),
        disabledIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = ContentAlpha.disabled
        ),
    ): ButtonColors = ButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        secondaryContentColor = secondaryContentColor,
        iconColor = iconColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        disabledSecondaryContentColor = disabledSecondaryContentColor,
        disabledIconColor = disabledIconColor
    )

    /**
     * The default height applied for the [Button].
     * Note that you can override it by applying Modifier.heightIn directly on [Button].
     */
    internal val Height = 52.dp

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [Button].
     */
    internal val IconSpacing = 6.dp
}

/**
 * Represents the container and content colors used in buttons
 * in different states.
 */
@Immutable
class ButtonColors internal constructor(
    private val containerPainter: Painter,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val iconColor: Color,
    private val disabledContainerPainter: Painter,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledIconColor: Color,
) {
    /**
     * Creates a [ButtonColors] where all of the values are explicitly defined.
     *
     * @param containerColor The background color of this [Button] when enabled
     * @param contentColor The content color of this [Button] when enabled
     * @param secondaryContentColor The content color of this [Button] when enabled
     * @param iconColor The content color of this [Button] when enabled
     * @param disabledContainerColor The background color of this [Button] when not enabled
     * @param disabledContentColor The content color of this [Button] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Button] when not enabled
     * @param disabledIconColor The content color of this [Button] when not enabled
     */
    internal constructor(
        containerColor: Color,
        contentColor: Color,
        secondaryContentColor: Color,
        iconColor: Color,
        disabledContainerColor: Color,
        disabledContentColor: Color,
        disabledSecondaryContentColor: Color,
        disabledIconColor: Color,
    ) : this(
        ColorPainter(containerColor),
        contentColor,
        secondaryContentColor,
        iconColor,
        ColorPainter(disabledContainerColor),
        disabledContentColor,
        disabledSecondaryContentColor,
        disabledIconColor,
    )

    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    internal fun containerPainter(enabled: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) containerPainter else disabledContainerPainter
        )
    }

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    internal fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) contentColor else disabledContentColor
        )
    }

    /**
     * Represents the secondary content color for this button, depending on [enabled].
     *
     * @param enabled Whether the button is enabled
     */
    @Composable
    internal fun secondaryContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) secondaryContentColor else disabledSecondaryContentColor
        )
    }

    /**
     * Represents the icon color for this button, depending on [enabled].
     *
     * @param enabled Whether the button is enabled
     */
    @Composable
    internal fun iconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) iconColor else disabledIconColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonColors) return false

        if (containerPainter != other.containerPainter) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconColor != other.iconColor) return false
        if (disabledContainerPainter != other.disabledContainerPainter) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconColor != other.disabledIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerPainter.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledContainerPainter.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        return result
    }
}
