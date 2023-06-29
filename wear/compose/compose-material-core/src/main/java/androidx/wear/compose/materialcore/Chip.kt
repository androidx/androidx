
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
package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp

/**
 * Base level [Chip] that offers a single slot to take any content.
 *
 * Is used as the container for more opinionated [Chip] components that take specific content such
 * as icons and labels.
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param background Resolves the background for this chip in different states.
 * @param border Resolves the border for this chip in different states.
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Chip(
    onClick: () -> Unit,
    background: @Composable (enabled: Boolean) -> State<Painter>,
    border: @Composable (enabled: Boolean) -> State<BorderStroke?>?,
    modifier: Modifier,
    enabled: Boolean,
    contentPadding: PaddingValues,
    shape: Shape,
    interactionSource: MutableInteractionSource,
    role: Role?,
    content: @Composable RowScope.() -> Unit,
) {
    val borderStroke = border(enabled)?.value
    Row(
        modifier = modifier
            .then(
                if (borderStroke != null) Modifier.border(
                    border = borderStroke,
                    shape = shape
                ) else Modifier
            )
            .clip(shape = shape)
            .width(intrinsicSize = IntrinsicSize.Max)
            .paint(
                painter = background(enabled).value,
                contentScale = ContentScale.Crop
            )
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = role,
                indication = rememberRipple(),
                interactionSource = interactionSource,
            )
            .padding(contentPadding),
        content = content
    )
}

/**
 * Wear Material [Chip] that offers three slots and a specific layout for an icon, label and
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out with the icon,
 * if provided, at the start of a row, with a column next containing the two label slots.
 *
 * The [Chip] has a max height designed to take no more than two lines of text
 * If no secondary label is provided then the label
 * can be two lines of text. The label and secondary label should be consistently aligned.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The [Chip] can have different styles with configurable content colors and painted background
 * including gradients.
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the chip
 * @param background Resolves the background for this chip in different states.
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon.
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not
 * be clickable
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Chip. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param border Resolves the chip border in different states.
 * @param defaultIconSpacing Spacing between icon and label, if both are provided.
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun Chip(
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    background: @Composable (enabled: Boolean) -> State<Painter>,
    modifier: Modifier,
    secondaryLabel: (@Composable RowScope.() -> Unit)?,
    icon: (@Composable BoxScope.() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    contentPadding: PaddingValues,
    shape: Shape,
    border: @Composable (enabled: Boolean) -> State<BorderStroke?>?,
    defaultIconSpacing: Dp,
    role: Role?,
) {
    Chip(
        modifier = modifier,
        onClick = onClick,
        background = background,
        border = border,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        interactionSource = interactionSource,
        role = role
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Fill the container height but not its width as chips have fixed size height but we
            // want them to be able to fit their content
            modifier = Modifier.fillMaxHeight()
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.wrapContentSize(align = Alignment.Center),
                    content = icon
                )
                Spacer(modifier = Modifier.size(defaultIconSpacing))
            }
            Column {
                Row(content = label)
                secondaryLabel?.let {
                    Row(content = secondaryLabel)
                }
            }
        }
    }
}

/**
 * A compact Wear Material Chip that offers two slots and a specific layout for an icon and label.
 * Both the icon and label are optional however it is expected that at least one will be provided.
 *
 * The [CompactChip] has a max height designed to take no more than one line
 * of text and/or one icon. This includes a visible chip height of 32.dp and
 * 8.dp of padding above and below the chip in order to meet accessibility guidelines that
 * request a minimum of 48.dp height and width of tappable area.
 *
 * If a icon is provided then the labels should be "start" aligned, e.g. left aligned in ltr so that
 * the text starts next to the icon.
 *
 * The items are laid out as follows.
 *
 * 1. If a label is provided then the chip will be laid out with the optional icon at the start of a
 * row followed by the label.
 *
 * 2. If only an icon is provided it will be laid out vertically and horizontally centered
 * and the default width of [defaultIconOnlyCompactChipWidth]
 *
 * If neither icon nor label is provided then the chip will displayed like an icon only chip but
 * with no contents and [background] color.
 *
 * The [CompactChip] can have different styles with configurable content colors and backgrounds
 * including gradients.
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
 *
 * @param onClick Will be called when the user clicks the chip
 * @param background Resolves the background for this chip in different states.
 * @param modifier Modifier to be applied to the chip
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "center" aligned if not.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon.
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
 * @param border Resolves the border for this chip in different states.
 * @param defaultIconOnlyCompactChipWidth The default width of the compact chip if there is no label
 * @param defaultCompactChipTapTargetPadding Default padding to increase the tap target
 * @param defaultIconSpacing Spacing between icon and label, if both are provided
 * @param role Role semantics that accessibility services can use to provide more
 * context to users.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun CompactChip(
    onClick: () -> Unit,
    background: @Composable (enabled: Boolean) -> State<Painter>,
    modifier: Modifier,
    label: (@Composable RowScope.() -> Unit)?,
    icon: (@Composable BoxScope.() -> Unit)?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    contentPadding: PaddingValues,
    shape: Shape,
    border: @Composable (enabled: Boolean) -> State<BorderStroke?>?,
    defaultIconOnlyCompactChipWidth: Dp,
    defaultCompactChipTapTargetPadding: PaddingValues,
    defaultIconSpacing: Dp,
    role: Role?,
) {
    if (label != null) {
        Chip(
            modifier = modifier
                .padding(defaultCompactChipTapTargetPadding),
            label = label,
            onClick = onClick,
            background = background,
            secondaryLabel = null,
            icon = icon,
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = contentPadding,
            shape = shape,
            border = border,
            defaultIconSpacing = defaultIconSpacing,
            role = role
        )
    } else {
        // Icon only compact chips have their own layout with a specific width and center aligned
        // content. We use the base simple single slot Chip under the covers.
        Chip(
            modifier = modifier
                .width(defaultIconOnlyCompactChipWidth)
                .padding(defaultCompactChipTapTargetPadding),
            onClick = onClick,
            background = background,
            border = border,
            enabled = enabled,
            contentPadding = contentPadding,
            shape = shape,
            interactionSource = interactionSource,
            role = role,
        ) {
            // Use a box to fill and center align the icon into the single slot of the Chip
            Box(modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(align = Alignment.Center)) {
                if (icon != null) {
                    icon()
                }
            }
        }
    }
}
