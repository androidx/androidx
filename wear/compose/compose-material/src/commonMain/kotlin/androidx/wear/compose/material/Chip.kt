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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
    ) {
        // TODO: Due to b/178201337 the paint() modifier on the box doesn't make a call to draw the
        //  box contents. As a result we need to have stacked boxes to enable us to paint the
        //  background
        val painterModifier =
            Modifier
                .paint(
                    painter = colors.background(enabled = enabled).value,
                    contentScale = ContentScale.Crop
                )
                .fillMaxSize()

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
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
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
 * For more information, see the
 * [Chips](https://developer.android.com/training/wearables/components/chips)
 * guide.
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
    contentPadding: PaddingValues = ChipDefaults.ContentPadding,
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
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconTintColor The icon tint color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun primaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.primary,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconTintColor = iconTintColor
        )
    }

    /**
     * Creates a [ChipColors] that represents the background and content colors for a primary [Chip]
     * with a linear gradient for a background. The gradient will be between startBackgroundColor
     * and endBackgroundColor. Gradient backgrounds are typically used for chips showing an on-going
     * activity, such as a music track that is playing.
     *
     * Gradient background chips should have a content color that contrasts with the background
     * gradient. If a chip is disabled then the color will have an alpha([ContentAlpha.disabled])
     * value applied.
     *
     * @param startBackgroundColor The background color used at the start of the gradient of this
     * [Chip] when enabled
     * @param endBackgroundColor The background color used at the end of the gradient of this [Chip]
     * when enabled
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconTintColor The icon tint color of this [Chip] when enabled, used for icon content
     * @param gradientDirection Whether the chips gradient should be start to end (indicated by
     * [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Composable
    public fun gradientBackgroundChipColors(
        startBackgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.325f)
            .compositeOver(MaterialTheme.colors.surface.copy(alpha = 0.75f)),
        endBackgroundColor: Color = MaterialTheme.colors.surface.copy(alpha = 0f)
            .compositeOver(MaterialTheme.colors.surface.copy(alpha = 0.75f)),
        contentColor: Color = contentColorFor(endBackgroundColor),
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor,
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
            iconTintColor = iconTintColor,
            disabledBackgroundPainter = BrushPainter(
                Brush.linearGradient(disabledBackgroundColors)
            ),
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor = secondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledIconTintColor = iconTintColor.copy(alpha = ContentAlpha.disabled),
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
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconTintColor The icon tint color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun secondaryChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = contentColorFor(backgroundColor),
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor
    ): ChipColors {
        return chipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconTintColor = iconTintColor
        )
    }

    /**
     * Creates a [ChipColors] that represents the default background (transparent) and content
     * colors for a child [Chip]. Child chips have a transparent background and use a default
     * content color of [Colors.onSurface].
     *
     * If a chip is disabled then the color will have an alpha([ContentAlpha.disabled]) value
     * applied.
     *
     * @param contentColor The content color of this [Chip] when enabled
     * @param secondaryContentColor The secondary content color of this [Chip] when enabled, used
     * for secondaryLabel content
     * @param iconTintColor The icon tint color of this [Chip] when enabled, used for icon content
     */
    @Composable
    public fun childChipColors(
        contentColor: Color = MaterialTheme.colors.onSurface,
        secondaryContentColor: Color = contentColor,
        iconTintColor: Color = contentColor
    ): ChipColors {
        return chipColors(
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            iconTintColor = iconTintColor
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
     * @param iconTintColor The icon tint color of this [Chip] when enabled, used for icon content
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
        iconTintColor: Color = contentColor,
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
            iconTintColor = iconTintColor,
            disabledBackgroundPainter = disabledBackgroundPainter,
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor = secondaryContentColor.copy(
                alpha = ContentAlpha.disabled
            ),
            disabledIconTintColor = iconTintColor.copy(alpha = ContentAlpha.disabled),
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
    internal val IconSpacing = 6.dp

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
    private val backgroundPainter: Painter,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val iconTintColor: Color,
    private val disabledBackgroundPainter: Painter,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledIconTintColor: Color,
) : ChipColors {

    constructor(
        backgroundColor: Color,
        contentColor: Color,
        secondaryContentColor: Color,
        iconTintColor: Color,
        disabledBackgroundColor: Color,
        disabledContentColor: Color,
        disabledSecondaryContentColor: Color,
        disabledIconTintColor: Color
    ) : this(
        ColorPainter(backgroundColor),
        contentColor,
        secondaryContentColor,
        iconTintColor,
        ColorPainter(disabledBackgroundColor),
        disabledContentColor,
        disabledSecondaryContentColor,
        disabledIconTintColor
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
    override fun iconTintColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) iconTintColor else disabledIconTintColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultChipColors

        if (backgroundPainter != other.backgroundPainter) return false
        if (contentColor != other.contentColor) return false
        if (secondaryContentColor != other.secondaryContentColor) return false
        if (iconTintColor != other.iconTintColor) return false
        if (disabledBackgroundPainter != other.disabledBackgroundPainter) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledIconTintColor != other.disabledIconTintColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundPainter.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + iconTintColor.hashCode()
        result = 31 * result + disabledBackgroundPainter.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledIconTintColor.hashCode()
        return result
    }
}
