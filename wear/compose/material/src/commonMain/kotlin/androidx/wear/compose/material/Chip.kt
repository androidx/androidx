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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
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
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(ChipDefaults.Height)
            .clip(shape = shape)
            .background(color = Color.Transparent, shape = shape)
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .paint(
                    painter = colors.background(enabled = enabled).value,
                )

        val contentBoxModifier = Modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = role,
                indication = rememberRipple(),
                interactionSource = interactionSource,
            )
            .fillMaxSize()
            .padding(contentPadding)

        Box(
            modifier = painterModifier
        ) { }
        Box(
            modifier = contentBoxModifier
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colors.contentColor(enabled = enabled).value,
                LocalTextStyle provides MaterialTheme.typography.button,
                LocalContentAlpha provides colors.contentColor(enabled = enabled).value.alpha,
                content = content
            )
        }
    }
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
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.IconSize] or [ChipDefaults.LargeIconSize].
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
@Composable
public fun Chip(
    label: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.contentPadding(icon != null),
) {
    Chip(
        onClick = onClick,
        colors = colors,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.wrapContentSize(align = Alignment.Center)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.iconTintColor(enabled).value,
                        LocalContentAlpha provides
                            colors.iconTintColor(enabled = enabled).value.alpha,
                        content = icon
                    )
                }
                Spacer(modifier = Modifier.size(ChipDefaults.IconSpacing))
            }
            Column {
                CompositionLocalProvider(
                    LocalContentColor provides colors.contentColor(enabled).value,
                    LocalTextStyle provides MaterialTheme.typography.button,
                    LocalContentAlpha provides colors.contentColor(enabled = enabled).value.alpha,
                    content = label
                )
                if (secondaryLabel != null) {
                    CompositionLocalProvider(
                        LocalContentColor provides colors.secondaryContentColor(enabled).value,
                        LocalTextStyle provides MaterialTheme.typography.caption2,
                        LocalContentAlpha provides
                            colors.secondaryContentColor(enabled = enabled).value.alpha,
                        content = secondaryLabel
                    )
                }
            }
        }
    }
}

/**
 * A compact Wear Material Chip that offers two slots and a specific layout for an icon and label.
 * Both the icon and label are optional however it is expected that at least one will be provided.
 *
 * The [CompactChip] is Stadium shaped and has a max height designed to take no more than one line
 * of text of [Typography.button] style and/or one 24x24 icon. The default max height is
 * [ChipDefaults.CompactChipHeight].
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
 * @param onClick Will be called when the user clicks the chip
 * @param modifier Modifier to be applied to the chip
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if not.
 * @param icon A slot for providing the chip's icon. The contents are expected to be a horizontally
 * and vertically aligned icon of size [ChipDefaults.IconSize] or [ChipDefaults.LargeIconSize].
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
@Composable
public fun CompactChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    colors: ChipColors = ChipDefaults.primaryChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ChipDefaults.contentPadding(icon != null),
) {
    if (label != null) {
        Chip(
            label = label,
            onClick = onClick,
            modifier = modifier.height(ChipDefaults.CompactChipHeight),
            icon = icon,
            colors = colors,
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = contentPadding
        )
    } else {
        Chip(
            onClick = onClick,
            modifier = modifier
                .height(ChipDefaults.CompactChipHeight)
                .width(ChipDefaults.IconOnlyCompactChipWidth),
            colors = colors,
            enabled = enabled,
            interactionSource = interactionSource,
            contentPadding = contentPadding
        ) {
            if (icon != null) {
                icon()
            }
        }
    }
}

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
     * Represents the icon tint color for this chip, depending on [enabled].
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable
    public fun iconTintColor(enabled: Boolean): State<Color>
}

/**
 * Contains the default values used by [Chip]
 */
public object ChipDefaults {

    /**
     * Creates a [ChipColors] that represents the default background and content colors for a
     * primary [Chip]. Primary chips have a colored background with a contrasting content color. If
     * a chip is disabled then the color will have an alpha([ContentAlpha.disabled]) value applied.
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     */
    @Composable
    public fun primaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ChipColors] that represents the default background and content colors for a
     * secondary [Chip]. Secondary chips have a muted background with a contrasting content color.
     * If a chip is disabled then the color will have an alpha([ContentAlpha.disabled]) value
     * applied.
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     */
    @Composable
    public fun secondaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = contentColorFor(backgroundColor)
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor
        )
    }

    private val ChipHorizontalPadding = 16.dp
    private val ChipWithIconHorizontalPadding = 12.dp
    private val ChipVerticalPadding = 6.dp

    /**
     * Creates a [PaddingValues] for a [Chip] based on whether the chip has a icon. For chips with
     * icons a smaller start and end padding are applied to compensate for the space that the icon
     * consumes.
     *
     * @param hasIcon Whether the [Chip] has an icon.
     */
    public fun contentPadding(
        hasIcon: Boolean
    ): PaddingValues {
        return if (hasIcon) ContentWithIconPadding else ContentPadding
    }

    /**
     * The default content padding used by [Chip]
     */
    public val ContentPadding: PaddingValues = PaddingValues(
        start = ChipHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipHorizontalPadding,
        bottom = ChipVerticalPadding
    )

    /**
     * The content padding used by a [Chip] which includes an icon
     */
    public val ContentWithIconPadding: PaddingValues = PaddingValues(
        start = ChipWithIconHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipWithIconHorizontalPadding,
        bottom = ChipVerticalPadding
    )

    /**
     * The default height applied for the [Chip].
     * Note that you can override it by applying Modifier.heightIn directly on [Chip].
     */
    internal val Height = 52.dp

    /**
     * The height applied for the [CompactChip].
     * Note that you can override it by applying Modifier.height directly on [CompactChip].
     */
    internal val CompactChipHeight = 32.dp

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
    internal val IconSpacing = 8.dp

    /**
     * Creates a [ChipColors] that represents the default background and content colors used in
     * a [Chip].
     *
     * @param backgroundColor The background color of this [Chip] when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The content color of this [Chip] when enabled
     * @param iconTintColor The content color of this [Chip] when enabled
     * @param disabledBackgroundColor The background color of this [Chip] when not enabled
     * @param disabledContentColor The content color of this [Chip] when not enabled
     * @param disabledSecondaryContentColor The content color of this [Chip] when not enabled
     * @param disabledIconTintColor The content color of this [Chip] when not enabled
     */
    @Composable
    public fun chipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor,
        disabledBackgroundColor: Color = backgroundColor.copy(alpha = ContentAlpha.disabled),
        disabledContentColor: Color = contentColor.copy(alpha = ContentAlpha.disabled),
        disabledSecondaryContentColor: Color =
            secondaryContentColor.copy(alpha = ContentAlpha.disabled),
        disabledIconTintColor: Color = iconTintColor.copy(alpha = ContentAlpha.disabled),
    ): ChipColors = DefaultChipColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        secondaryContentColor = secondaryContentColor,
        iconTintColor = iconTintColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor,
        disabledSecondaryContentColor = disabledSecondaryContentColor,
        disabledIconTintColor = disabledIconTintColor,
    )
}

/**
 * Default [ChipColors] implementation.
 */
@Immutable
private class DefaultChipColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val iconTintColor: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledIconTintColor: Color,
) : ChipColors {
    @Composable
    override fun background(enabled: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) ColorPainter(backgroundColor) else ColorPainter(disabledBackgroundColor)
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
    override fun iconTintColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) iconTintColor else disabledIconTintColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultChipColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconTintColor != other.iconTintColor) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconTintColor != other.disabledIconTintColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconTintColor.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconTintColor.hashCode()
        return result
    }
}
