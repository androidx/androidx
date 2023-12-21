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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Base level Wear Material [Chip] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Chip] components that take specific content such
 * as icons and labels.
 *
 * The [Chip] is Stadium shaped and has a max height designed to take no more than two lines of text
 * of [Typography.button] style. The [Chip] can have an icon or image horizontally
 * parallel to the two lines of text.
 *
 * The [Chip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors].
 * @param modifier Modifier to be applied to the chip
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param role The type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional border parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun Chip(
    onClick: () -> Unit,
    colors: ChipColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) = Chip(
    onClick = onClick,
    colors = colors,
    border = ChipDefaults.chipBorder(),
    modifier = modifier,
    enabled = enabled,
    contentPadding = contentPadding,
    shape = shape,
    interactionSource = interactionSource,
    role = role,
    content = content)

/**
 * Base level Wear Material [Chip] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Chip] components that take specific content such
 * as icons and labels.
 *
 * The [Chip] is Stadium shaped and has a max height designed to take no more than two lines of text
 * of [Typography.button] style. The [Chip] can have an icon or image horizontally
 * parallel to the two lines of text.
 *
 * The [Chip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors].
 * @param border [ChipBorder] that will be used to resolve the border for this chip in different
 * states. See [ChipDefaults.chipBorder].
 * @param modifier Modifier to be applied to the chip
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param role The type of user interface element. Accessibility services might use this
 * to describe the element or do customizations
 */
@Composable
public fun Chip(
    onClick: () -> Unit,
    colors: ChipColors,
    border: ChipBorder,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = Role.Button,
    content: @Composable RowScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Chip(
        modifier = modifier.height(ChipDefaults.Height),
        onClick = onClick,
        background = { colors.background(enabled = it) },
        border = { border.borderStroke(enabled = it) },
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        interactionSource = interactionSource,
        role = role,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.button,
            content
        ),
    )
}

/**
 * Wear Material [Chip] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [Chip] is Stadium shaped and has a max height designed to take no more than two lines of text
 * of [Typography.button] style. If no secondary label is provided then the label
 * can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The [Chip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [Chip] with icon and a label only with longer text:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabel
 *
 * Example of a [Chip] with icon, label and secondary label:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabels
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.IconSize] or [ChipDefaults.LargeIconSize]. In
 * order to correctly render when the Chip is not enabled the icon must set its alpha value to
 * [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors]. Defaults to
 * [ChipDefaults.primaryChipColors]
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.

 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional shape parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun Chip(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
) = Chip(
    label,
    onClick,
    modifier,
    secondaryLabel,
    icon,
    colors,
    enabled,
    interactionSource,
    contentPadding,
    MaterialTheme.shapes.small
)

/**
 * Wear Material [Chip] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [Chip] is Stadium shaped and has a max height designed to take no more than two lines of text
 * of [Typography.button] style. If no secondary label is provided then the label
 * can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The [Chip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [Chip] with icon and a label only with longer text:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabel
 *
 * Example of a [Chip] with icon, label and secondary label:
 * @sample androidx.wear.compose.material.samples.ChipWithIconAndLabels
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.IconSize] or [ChipDefaults.LargeIconSize]. In
 * order to correctly render when the Chip is not enabled the icon must set its alpha value to
 * [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors]. Defaults to
 * [ChipDefaults.primaryChipColors]
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param border [ChipBorder] that will be used to resolve the chip border in different states.
 * See [ChipDefaults.chipBorder].
 */
@Composable
public fun Chip(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder = ChipDefaults.chipBorder()
) {
    androidx.wear.compose.materialcore.Chip(
        modifier = modifier.height(ChipDefaults.Height),
        label = provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.button,
            label
        ),
        onClick = onClick,
        background = { colors.background(enabled = it) },
        secondaryLabel = secondaryLabel?.let { provideScopeContent(
            colors.secondaryContentColor(enabled = enabled),
            textStyle = MaterialTheme.typography.caption2,
            secondaryLabel
        ) },
        icon = icon?.let {
            provideIcon(colors.iconColor(enabled = enabled), icon)
        },
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = { border.borderStroke(enabled = it) },
        defaultIconSpacing = ChipDefaults.IconSpacing,
        role = Role.Button
    )
}

/**
 * Wear Material [OutlinedChip] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [OutlinedChip] is Stadium shaped and has a max height designed to take no more than two lines
 * of text of [Typography.button] style. If no secondary label is provided then the label
 * can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * the [OutlinedChip] has a transparent background, a thin border and contents which are
 * colored with the theme primary color. Colors can be obtained and customized using
 * [ChipDefaults.outlinedChipColors()].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [OutlinedChip] with icon and a label only with longer text:
 * @sample androidx.wear.compose.material.samples.OutlinedChipWithIconAndLabel
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.IconSize] or [ChipDefaults.LargeIconSize]. In
 * order to correctly render when the Chip is not enabled the icon must set its alpha value to
 * [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states.
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param border [ChipBorder] that will be used to resolve the chip border in different states.
 */
@Composable
public fun OutlinedChip(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.outlinedChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder = ChipDefaults.outlinedChipBorder()
) =
    Chip(
        label = label,
        onClick = onClick,
        modifier = modifier,
        secondaryLabel = secondaryLabel,
        icon = icon,
        colors = colors,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = border
    )

/**
 * A compact Wear Material Chip that offers two slots and a specific layout for an icon and label.
 * Both the icon and label are optional however it is expected that at least one will be provided.
 *
 * The [CompactChip] is Stadium shaped and has a max height designed to take no more than one line
 * of text of [Typography.caption1] style and/or one icon. The default max height is
 * [ChipDefaults.CompactChipHeight]. This includes a visible chip height of 32.dp and
 * 8.dp of padding above and below the chip in order to meet accessibility guidelines that
 * request a minimum of 48.dp height and width of tappable area.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The items are laid out as follows.
 *
 * 1. If a label is provided then the chip will be laid out with the optional icon at the start of a
 * row followed by the label with a default max height of [ChipDefaults.CompactChipHeight].
 *
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered with a
 * default height of [ChipDefaults.CompactChipHeight] and the default width of
 * [ChipDefaults.IconOnlyCompactChipWidth]
 *
 * If neither icon nor label is provided then the chip will displayed like an icon only chip but
 * with no contents and [ChipColors.background()] color.
 *
 * The [CompactChip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [CompactChip] with icon and single line of label text:
 * @sample androidx.wear.compose.material.samples.CompactChipWithIconAndLabel
 *
 * Example of a [CompactChip] with a label, note that the text is center aligned:
 * @sample androidx.wear.compose.material.samples.CompactChipWithLabel
 *
 * Example of a [CompactChip] with an icon only, note that the recommended icon size is 24x24 when
 * only an icon is displayed:
 * @sample androidx.wear.compose.material.samples.CompactChipWithIcon
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "center" aligned if not.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.SmallIconSize] when used with a label or
 * [ChipDefaults.IconSize] when used as the only content in the CompactChip. In order to correctly
 * render when the Chip is not enabled the icon must set its alpha value to [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors]. Defaults to
 * [ChipDefaults.primaryChipColors]
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 */
@Deprecated("This overload is provided for backwards compatibility with Compose for Wear OS 1.0." +
    "A newer overload is available with an additional shape parameter.",
    level = DeprecationLevel.HIDDEN)
@Composable
public fun CompactChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.CompactChipContentPadding,
) = CompactChip(
    onClick,
    modifier,
    label,
    icon,
    colors,
    enabled,
    interactionSource,
    contentPadding,
    MaterialTheme.shapes.small,
    ChipDefaults.chipBorder()
)

/**
 * A compact Wear Material Chip that offers two slots and a specific layout for an icon and label.
 * Both the icon and label are optional however it is expected that at least one will be provided.
 *
 * The [CompactChip] is Stadium shaped and has a max height designed to take no more than one line
 * of text of [Typography.caption1] style and/or one icon. The default max height is
 * [ChipDefaults.CompactChipHeight]. This includes a visible chip height of 32.dp and
 * 8.dp of padding above and below the chip in order to meet accessibility guidelines that
 * request a minimum of 48.dp height and width of tappable area.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The items are laid out as follows.
 *
 * 1. If a label is provided then the chip will be laid out with the optional icon at the start of a
 * row followed by the label with a default max height of [ChipDefaults.CompactChipHeight].
 *
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered with a
 * default height of [ChipDefaults.CompactChipHeight] and the default width of
 * [ChipDefaults.IconOnlyCompactChipWidth]
 *
 * If neither icon nor label is provided then the chip will displayed like an icon only chip but
 * with no contents and [ChipColors.background()] color.
 *
 * The [CompactChip] can have different styles with configurable content colors, background colors
 * including gradients, these are provided by [ChipColors] implementations.
 *
 * The recommended set of [ChipColors] styles can be obtained from [ChipDefaults], e.g.
 * [ChipDefaults.primaryChipColors] to get a color scheme for a primary [Chip] which by default
 * will have a solid background of [Colors.primary] and content color of
 * [Colors.onPrimary].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [CompactChip] with icon and single line of label text:
 * @sample androidx.wear.compose.material.samples.CompactChipWithIconAndLabel
 *
 * Example of a [CompactChip] with a label, note that the text is center aligned:
 * @sample androidx.wear.compose.material.samples.CompactChipWithLabel
 *
 * Example of a [CompactChip] with an icon only, note that the recommended icon size is 24x24 when
 * only an icon is displayed:
 * @sample androidx.wear.compose.material.samples.CompactChipWithIcon
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "center" aligned if not.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.SmallIconSize] when used with a label or
 * [ChipDefaults.IconSize] when used as the only content in the CompactChip. In order to correctly
 * render when the Chip is not enabled the icon must set its alpha value to [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.chipColors]. Defaults to
 * [ChipDefaults.primaryChipColors]
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param border [ChipBorder] that will be used to resolve the border for this chip in different
 * states. See [ChipDefaults.chipBorder].
 */
@Composable
public fun CompactChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.CompactChipContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder = ChipDefaults.chipBorder()
) {
    androidx.wear.compose.materialcore.CompactChip(
        modifier = modifier.height(ChipDefaults.CompactChipHeight),
        onClick = onClick,
        background = { colors.background(enabled = it) },
        label = label?.let { provideScopeContent(
            colors.contentColor(enabled = enabled),
            MaterialTheme.typography.caption1,
            label
        ) },
        icon = icon?.let { provideIcon(
            colors.iconColor(enabled = enabled),
            icon
        ) },
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = { border.borderStroke(enabled = it) },
        defaultIconOnlyCompactChipWidth = ChipDefaults.IconOnlyCompactChipWidth,
        defaultCompactChipTapTargetPadding = ChipDefaults.CompactChipTapTargetPadding,
        defaultIconSpacing = ChipDefaults.IconSpacing,
        role = Role.Button,
    )
}

/**
 * A compact Outlined Wear Material Chip that offers two slots and a specific layout for an icon and
 * label. Both the icon and label are optional however it is expected that at least one will be
 * provided.
 *
 * The [CompactChip] is Stadium shaped and has a max height designed to take no more than one line
 * of text of [Typography.caption1] style and/or one icon. The default max height is
 * [ChipDefaults.CompactChipHeight]. This includes a visible chip height of 32.dp and
 * 8.dp of padding above and below the chip in order to meet accessibility guidelines that
 * request a minimum of 48.dp height and width of tappable area.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The items are laid out as follows.
 *
 * 1. If a label is provided then the chip will be laid out with the optional icon at the start of a
 * row followed by the label with a default max height of [ChipDefaults.CompactChipHeight].
 *
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered with a
 * default height of [ChipDefaults.CompactChipHeight] and the default width of
 * [ChipDefaults.IconOnlyCompactChipWidth]
 *
 * If neither icon nor label is provided then the chip will displayed like an icon only chip but
 * with no contents and [ChipColors.background()] color.
 *
 * the [OutlinedCompactChip] has a transparent background, a thin border and contents which are
 * colored with the theme primary color. Colors can be obtained and customized using
 * [ChipDefaults.outlinedChipColors()].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [OutlinedCompactChip] with icon and single line of label text:
 * @sample androidx.wear.compose.material.samples.OutlinedCompactChipWithIconAndLabel
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "center" aligned if not.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.SmallIconSize] when used with a label or
 * [ChipDefaults.IconSize] when used as the only content in the CompactChip. In order to correctly
 * render when the Chip is not enabled the icon must set its alpha value to [LocalContentAlpha].
 * @param colors [ChipColors] that will be used to resolve the background and content color for
 * this chip in different states. See [ChipDefaults.outlinedChipColors]. Defaults to
 * [ChipDefaults.primaryChipColors]
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme
 * @param border [ChipBorder] that will be used to resolve the border for this chip in different
 * states. See [ChipDefaults.outlinedChipBorder].
 */
@Composable
public fun OutlinedCompactChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable RowScope.() -> Unit)? = null,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    colors: ChipColors = ChipDefaults.outlinedChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.CompactChipContentPadding,
    shape: Shape = MaterialTheme.shapes.small,
    border: ChipBorder = ChipDefaults.outlinedChipBorder()
) =
    CompactChip(
        onClick = onClick,
        modifier = modifier,
        label = label,
        icon = icon,
        colors = colors,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        border = border
    )

/**
 * Represents the background and content colors used in a chip in different states.
 *
 * See [ChipDefaults.primaryChipColors] for the default colors used in a primary styled [Chip].
 * See [ChipDefaults.secondaryChipColors] for the default colors used in a secondary styled [Chip].
 */
@Stable
public interface ChipColors {
    /**
     * Represents the background treatment for this chip, depending on [enabled]. Backgrounds can
     * be solid, transparent or have a gradient applied.
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable
    public fun background(enabled: Boolean): State<Painter>

    /**
     * Represents the content color for this chip, depending on [enabled].
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable
    public fun contentColor(enabled: Boolean): State<Color>

    /**
     * Represents the secondary content color for this chip, depending on [enabled].
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable
    public fun secondaryContentColor(enabled: Boolean): State<Color>

    /**
     * Represents the icon color for this chip, depending on [enabled].
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable
    public fun iconColor(enabled: Boolean): State<Color>
}

/**
 * Represents the border stroke used in a [Chip] in different states.
 */
@Stable
public interface ChipBorder {
    @Composable
    /**
     * Represents the border stroke for this chip, depending on [enabled] or null if no border
     *
     * @param enabled Whether the chip is enabled
     */
    public fun borderStroke(enabled: Boolean): State<BorderStroke?>
}

/**
 * Contains the default values used by [Chip]
 */
public object ChipDefaults {

    /**
     * Creates a [ChipColors] that represents the default background and content colors for a
     * primary [Chip]. Primary chips have a colored background with a contrasting content color. If
     * a chip is disabled then the colors will have an alpha([ContentAlpha.disabled]) value applied.
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun primaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor
    ): ChipColors {
        // For light background colors, the default disabled content colors do not provide
        // sufficient contrast. Instead, we default to using background for disabled content.
        // See b/254025377
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledContentColor = MaterialTheme.colors.background,
            disabledSecondaryContentColor = MaterialTheme.colors.background,
            disabledIconColor = MaterialTheme.colors.background,
        )
    }

    /**
     * Creates a [ChipColors] that represents the background and content colors for a primary [Chip]
     * with a linear gradient for a background. The gradient will be between startBackgroundColor
     * and endBackgroundColor. Gradient backgrounds are typically used for chips showing an on-going
     * activity, such as a music track that is playing.
     *
     * Gradient background chips should have a content color that contrasts with the background
     * gradient. If a chip is disabled then the colors will have an alpha([ContentAlpha.disabled])
     * value applied.
     *
     * @param startBackgroundColor The background color used at the start of the gradient of this
     * [Chip] when enabled
     * @param endBackgroundColor The background color used at the end of the gradient of this [Chip]
     * when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     * @param gradientDirection Whether the chips gradient should be start to end (indicated by
     * [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Composable
    public fun gradientBackgroundChipColors(
        startBackgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.5f)
            .compositeOver(MaterialTheme.colors.surface),
        endBackgroundColor: Color = MaterialTheme.colors.surface.copy(alpha = 0f)
            .compositeOver(MaterialTheme.colors.surface),
        contentColor: Color = contentColorFor(endBackgroundColor),
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
    ): ChipColors {
        val backgroundColors: List<Color>
        val disabledBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            backgroundColors = listOf(
                startBackgroundColor,
                endBackgroundColor
            )
            disabledBackgroundColors = listOf(
                startBackgroundColor.copy(alpha = ContentAlpha.disabled),
                endBackgroundColor.copy(alpha = ContentAlpha.disabled)
            )
        } else {
            backgroundColors = listOf(
                endBackgroundColor,
                startBackgroundColor
            )
            disabledBackgroundColors = listOf(
                endBackgroundColor.copy(alpha = ContentAlpha.disabled),
                startBackgroundColor.copy(alpha = ContentAlpha.disabled),
            )
        }
        return DefaultChipColors(
            backgroundPainter = BrushPainter(Brush.linearGradient(backgroundColors)),
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledBackgroundColors)
            ),
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor = secondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledIconColor = iconColor.copy(alpha = ContentAlpha.disabled),
        )
    }

    /**
     * Creates a [ChipColors] that represents the default background and content colors for a
     * secondary [Chip]. Secondary chips have a muted background with a contrasting content color.
     * If a chip is disabled then the colors will have an alpha([ContentAlpha.disabled]) value
     * applied.
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun secondaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor
        )
    }

    /**
     * Creates a [ChipColors] that represents the default background (transparent) and content
     * colors for a child [Chip]. Child chips have a transparent background and use a default
     * content color of [Colors.onSurface].
     *
     * If a chip is disabled then the colors will have an alpha([ContentAlpha.disabled]) value
     * applied.
     *
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun childChipColors(
        contentColor: Color = MaterialTheme.colors.onSurface,
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor
    ): ChipColors {
        return chipColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledBackgroundColor = Color.Transparent
        )
    }

    /**
     * Creates a [ChipColors] for an image background [Chip]. Image background chips have an image
     * as the background of the chip typically with a scrim over the image to ensure that the
     * content is visible, and use a default content color of [Colors.onBackground].
     *
     * @param backgroundImagePainter The [Painter] to use to draw the background of the [Chip]
     * @param backgroundImageScrimBrush The [Brush] to use to paint a scrim over the background
     * image to ensure that any text drawn over the image is legible
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun imageBackgroundChipColors(
        backgroundImagePainter: Painter,
        backgroundImageScrimBrush: Brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colors.surface.copy(alpha = 1.0f),
                MaterialTheme.colors.surface.copy(alpha = 0f)
            )
        ),
        contentColor: Color = MaterialTheme.colors.onBackground,
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
    ): ChipColors {
        val backgroundPainter =
            remember(backgroundImagePainter, backgroundImageScrimBrush) {
                ImageWithScrimPainter(
                    imagePainter = backgroundImagePainter,
                    brush = backgroundImageScrimBrush
                )
            }

        val disabledContentAlpha = ContentAlpha.disabled
        val disabledBackgroundPainter =
            remember(backgroundImagePainter, backgroundImageScrimBrush, disabledContentAlpha) {
                ImageWithScrimPainter(
                    imagePainter = backgroundImagePainter,
                    brush = backgroundImageScrimBrush,
                    alpha = disabledContentAlpha,
                )
            }
        return DefaultChipColors(
            backgroundPainter = backgroundPainter,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledBackgroundPainter = disabledBackgroundPainter,
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor = secondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledIconColor = iconColor.copy(alpha = ContentAlpha.disabled),
        )
    }

    /**
     * Creates a [ChipColors] for an outline [Chip]. Outline chips have a transparent background
     * with a thin border.
     *
     * If a chip is disabled then the colors will have an alpha([ContentAlpha.disabled]) value
     * applied.
     *
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconColor The icon color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun outlinedChipColors(
        contentColor: Color = MaterialTheme.colors.primary,
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
    ): ChipColors {
        return chipColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconColor = iconColor,
            disabledBackgroundColor = Color.Transparent,
        )
    }

    /**
     * Creates a default (no border) [ChipBorder] for a [Chip]
     *
     * @param borderStroke The border for this [Chip] when enabled
     * @param disabledBorderStroke The border to use for this [Chip] when disabled
     */
    @Composable
    public fun chipBorder(
        borderStroke: BorderStroke? = null,
        disabledBorderStroke: BorderStroke? = null
    ): ChipBorder {
        return DefaultChipBorder(
            borderStroke = borderStroke,
            disabledBorderStroke = disabledBorderStroke
        )
    }

    /**
     * Creates a [ChipBorder] for an [OutlinedChip]
     *
     * @param borderColor The color to use for the border for this [OutlinedChip] when enabled
     * @param disabledBorderColor The color to use for the border for this [OutlinedChip] when
     * disabled
     * @param borderWidth The width to use for the border for this [OutlinedChip]
     */
    @Composable
    public fun outlinedChipBorder(
        borderColor: Color = MaterialTheme.colors.primaryVariant.copy(alpha = 0.6f),
        disabledBorderColor: Color = borderColor.copy(alpha = ContentAlpha.disabled),
        borderWidth: Dp = 1.dp
    ): ChipBorder {
        return DefaultChipBorder(
            borderStroke = BorderStroke(borderWidth, borderColor),
            disabledBorderStroke = BorderStroke(borderWidth, disabledBorderColor)
        )
    }

    private val ChipHorizontalPadding = 14.dp
    private val ChipVerticalPadding = 6.dp

    /**
     * The default content padding used by [Chip]
     */
    public val ContentPadding: PaddingValues = PaddingValues(
        start = ChipHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipHorizontalPadding,
        bottom = ChipVerticalPadding
    )

    private val CompactChipHorizontalPadding = 12.dp
    private val CompactChipVerticalPadding = 0.dp

    /**
     * The default content padding used by [CompactChip]
     */
    public val CompactChipContentPadding: PaddingValues = PaddingValues(
        start = CompactChipHorizontalPadding,
        top = CompactChipVerticalPadding,
        end = CompactChipHorizontalPadding,
        bottom = CompactChipVerticalPadding
    )

    /**
     * The default height applied for the [Chip].
     * Note that you can override it by applying Modifier.heightIn directly on [Chip].
     */
    internal val Height = 52.dp

    /**
     * The height applied for the [CompactChip]. This includes a visible chip height of 32.dp and
     * 8.dp of padding above and below the chip in order to meet accessibility guidelines that
     * request a minimum of 48.dp height and width of tappable area.
     *
     * Note that you can override it by adjusting Modifier.height and Modifier.padding directly on
     * [CompactChip].
     */
    internal val CompactChipHeight = 48.dp

    /**
     * The default padding to be provided around a [CompactChip] in order to ensure that its
     * tappable area meets minimum UX guidance.
     */
    public val CompactChipTapTargetPadding: PaddingValues = PaddingValues(
        top = 8.dp,
        bottom = 8.dp
    )

    /**
     * The default width applied for the [CompactChip] when it has no label provided.
     * Note that you can override it by applying Modifier.width directly on [CompactChip].
     */
    internal val IconOnlyCompactChipWidth = 52.dp

    /**
     * The default size of the icon when used inside a [Chip].
     */
    public val IconSize: Dp = 24.dp

    /**
     * The size of the icon when used inside a Large "Avatar" [Chip].
     */
    public val LargeIconSize: Dp = 32.dp

    /**
     * The size of the icon when used inside a "Compact" [Chip].
     */
    public val SmallIconSize: Dp = 20.dp

    /**
     * The default size of the spacing between an icon and a text when they are used inside a
     * [Chip].
     */
    internal val IconSpacing = 6.dp

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * a [Chip].
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The content color of this [Chip] when enabled
     * @param iconColor The content color of this [Chip] when enabled
     * @param disabledBackgroundColor The background color of this [Chip] when not enabled
     * @param disabledContentColor The content color of this [Chip] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Chip] when not enabled
     * @param disabledIconColor The content color of this [Chip] when not enabled
     */
    @Composable
    public fun chipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconColor: Color = contentColor,
        disabledBackgroundColor: Color = backgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledContentColor: Color = contentColor.copy(alpha = ContentAlpha.disabled),
        disabledSecondaryContentColor: Color =
            secondaryContentColor.copy(alpha = ContentAlpha.disabled),
        disabledIconColor: Color = iconColor.copy(alpha = ContentAlpha.disabled),
    ): ChipColors = DefaultChipColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        secondaryContentColor = secondaryContentColor,
        iconColor = iconColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        disabledSecondaryContentColor = disabledSecondaryContentColor,
        disabledIconColor = disabledIconColor
    )

    /**
     * Creates a [ChipColors] where all of the values are explicitly defined.
     *
     * @param backgroundPainter The background painter of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The content color of this [Chip] when enabled
     * @param iconColor The content color of this [Chip] when enabled
     * @param disabledBackgroundPainter The background painter of this [Chip] when not enabled
     * @param disabledContentColor The content color of this [Chip] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Chip] when not enabled
     * @param disabledIconColor The content color of this [Chip] when not enabled
     */
    @ExperimentalWearMaterialApi
    public fun chipColors(
        backgroundPainter: Painter,
        contentColor: Color,
        secondaryContentColor: Color,
        iconColor: Color,
        disabledBackgroundPainter: Painter,
        disabledContentColor: Color,
        disabledSecondaryContentColor: Color,
        disabledIconColor: Color,
    ): ChipColors = DefaultChipColors(
        backgroundPainter = backgroundPainter,
        contentColor = contentColor,
        secondaryContentColor = secondaryContentColor,
        iconColor = iconColor,
        disabledBackgroundPainter = disabledBackgroundPainter,
        disabledContentColor = disabledContentColor,
        disabledSecondaryContentColor = disabledSecondaryContentColor,
        disabledIconColor = disabledIconColor
    )
}

/**
 * Default [ChipColors] implementation.
 */
@Immutable
internal class DefaultChipColors(
    private val backgroundPainter: Painter,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val iconColor: Color,
    private val disabledBackgroundPainter: Painter,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledIconColor: Color
) : ChipColors {

    constructor(
        backgroundColor: Color,
        contentColor: Color,
        secondaryContentColor: Color,
        iconColor: Color,
        disabledBackgroundColor: Color,
        disabledContentColor: Color,
        disabledSecondaryContentColor: Color,
        disabledIconColor: Color
    ) : this(
        ColorPainter(backgroundColor),
        contentColor,
        secondaryContentColor,
        iconColor,
        ColorPainter(disabledBackgroundColor),
        disabledContentColor,
        disabledSecondaryContentColor,
        disabledIconColor
    )

    @Composable
    override fun background(enabled: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) backgroundPainter else disabledBackgroundPainter
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) contentColor else disabledContentColor
        )
    }

    @Composable
    override fun secondaryContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) secondaryContentColor else disabledSecondaryContentColor
        )
    }

    @Composable
    override fun iconColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) iconColor else disabledIconColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultChipColors

        if (backgroundPainter != other.backgroundPainter) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconColor != other.iconColor) return false
        if (disabledBackgroundPainter != other.disabledBackgroundPainter) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconColor != other.disabledIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundPainter.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconColor.hashCode()
        result = 31 * result + disabledBackgroundPainter.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconColor.hashCode()
        return result
    }
}

/**
 * Default [ChipBorder] implementation.
 */
@Immutable
private class DefaultChipBorder(
    private val borderStroke: BorderStroke? = null,
    private val disabledBorderStroke: BorderStroke? = null
) : ChipBorder {
    @Composable
    override fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(if (enabled) borderStroke else disabledBorderStroke)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultChipBorder

        if (borderStroke != other.borderStroke) return false
        if (disabledBorderStroke != other.disabledBorderStroke) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderStroke.hashCode()
        result = 31 * result + disabledBorderStroke.hashCode()
        return result
    }
}
