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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.wear.compose.material3.tokens.FilledIconButtonTokens
import androidx.wear.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.wear.compose.material3.tokens.IconButtonTokens
import androidx.wear.compose.material3.tokens.IconToggleButtonTokens
import androidx.wear.compose.material3.tokens.OutlinedIconButtonTokens

/**
 * Wear Material [IconButton] is a circular, icon-only button with transparent background and
 * no border. It offers a single slot to take icon or image content.
 *
 * Set the size of the [IconButton] with Modifier.[touchTargetAwareSize]
 * to ensure that the recommended minimum touch target size is available.
 *
 * The recommended [IconButton] sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a
 * given [IconButtonDefaults] size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [IconButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * Example of an [IconButton]:
 * @sample androidx.wear.compose.material3.samples.IconButtonSample
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the icon button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 design.
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 * this button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.shape,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.Button(
        onClick = onClick,
        modifier.minimumInteractiveComponentSize(),
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shape = shape,
        border = { rememberUpdatedState(border) },
        buttonSize = IconButtonDefaults.DefaultButtonSize,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled),
            content
        )
    )
}

/**
 * Wear Material [FilledIconButton] is a circular, icon-only button with a colored background
 * and a contrasting content color. It offers a single slot to take an icon or image.
 *
 * Set the size of the [FilledIconButton] with Modifier.[touchTargetAwareSize]
 * to ensure that the recommended minimum touch target size is available.
 *
 * The recommended [IconButton] sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a
 * given [IconButton] size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeIconSize] directly.
 *
 * [FilledIconButton] can be enabled or disabled. A disabled button will not respond to
 * click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * Example of [FilledIconButton]:
 * @sample androidx.wear.compose.material3.samples.FilledIconButtonSample
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the icon button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 design.
 * @param colors [IconButtonColors] that will be used to resolve the container and content color for
 * this icon button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.shape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = IconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content)

/**
 * Wear Material [FilledTonalIconButton] is a circular, icon-only button with a muted, colored
 * background and a contrasting icon color. It offers a single slot to take an icon or image.
 *
 * Set the size of the [FilledTonalIconButton] with Modifier.[touchTargetAwareSize]
 * to ensure that the recommended minimum touch target size is available.
 *
 * The recommended icon button sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a
 * given [IconButtonDefaults] size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [FilledTonalIconButton] can be enabled or disabled.
 * A disabled button will not respond to click events.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * Example of [FilledTonalIconButton]:
 * @sample androidx.wear.compose.material3.samples.FilledTonalIconButtonSample
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the icon button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 design.
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 * this button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.shape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = IconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content)

/**
 * Wear Material [OutlinedIconButton] is a circular, icon-only button with a transparent background,
 * contrasting icon color and border. It offers a single slot to take an icon or image.
 *
 * Set the size of the [OutlinedIconButton] with Modifier.[touchTargetAwareSize]
 * to ensure that the recommended minimum touch target size is available.
 *
 * The recommended icon button sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a
 * given [IconButtonDefaults] size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [OutlinedIconButton] can be enabled or disabled.
 * A disabled button will not respond to click events.
 *
 * An [OutlinedIconButton] has a transparent background and a thin border by default with
 * content taking the theme primary color.
 *
 * TODO(b/261838497) Add Material3 samples and UX guidance links
 *
 * Example of [OutlinedIconButton]:
 * @sample androidx.wear.compose.material3.samples.OutlinedIconButtonSample
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the icon button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 design.
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 * this button in different states. See [IconButtonDefaults.outlinedIconButtonColors].
 * @param border Optional [BorderStroke] for the icon button border -
 * [ButtonDefaults.outlinedButtonBorder] by default.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Button in different [Interaction]s.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.shape,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = IconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content)

/**
 * Wear Material [IconToggleButton] is a filled icon toggle button which switches between primary
 * colors and tonal colors depending on [checked] value, and offers a single slot for
 * icon or image.
 *
 * Set the size of the [IconToggleButton] with Modifier.[touchTargetAwareSize]
 * to ensure that the background padding will correctly reach the edge of the minimum touch target.
 * The recommended text button sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a
 * given [IconToggleButton] size, or refer to icon sizes
 * [IconButtonDefaults.SmallIconSize], [IconButtonDefaults.DefaultIconSize],
 * [IconButtonDefaults.LargeIconSize] directly.
 *
 * [IconToggleButton] can be enabled or disabled. A disabled button will not respond to
 * click events. When enabled, the checked and unchecked events are propagated by [onCheckedChange].
 *
 * A simple icon toggle button using the default colors
 * @sample androidx.wear.compose.material3.samples.IconToggleButtonSample
 *
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`,
 * this toggle button will not be clickable.
 * @param colors [ToggleButtonColors] that will be used to resolve the container and
 * content color for this toggle button.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this toggle button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this ToggleButton in different [Interaction]s.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material 3 Theme.
 * @param border Optional [BorderStroke] for the [IconToggleButton].
 * @param content The content to be drawn inside the toggle button.
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = IconButtonDefaults.shape,
    border: BorderStroke? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.minimumInteractiveComponentSize(),
        enabled = enabled,
        backgroundColor = { isEnabled, isChecked ->
            colors.containerColor(enabled = isEnabled, checked = isChecked)
        },
        border = { _, _ -> rememberUpdatedState(newValue = border) },
        toggleButtonSize = IconButtonDefaults.DefaultButtonSize,
        interactionSource = interactionSource,
        shape = shape,
        content = provideScopeContent(
            colors.contentColor(enabled = enabled, checked = checked),
            content
        )
    )
}

/**
 * Contains the default values used by [IconButton].
 */
object IconButtonDefaults {
    /**
     * Recommended [Shape] for [IconButton].
     */
    val shape: Shape
        @Composable get() = IconButtonTokens.ContainerShape.value

    /**
     * Recommended icon size for a given icon button size.
     *
     * Ensures that the minimum recommended icon size is applied.
     *
     * Examples: for size [LargeButtonSize], returns [LargeIconSize],
     * for size [ExtraSmallButtonSize] returns [SmallIconSize].
     *
     * @param size The size of the icon button
     */
    fun iconSizeFor(size: Dp): Dp = max(SmallIconSize, size / 2f)

    /**
     * Creates a [ButtonColors] with the colors for [FilledIconButton] - by default,
     * a colored background with a contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = FilledIconButtonTokens.ContainerColor.value,
        contentColor: Color = FilledIconButtonTokens.ContentColor.value,
        disabledContainerColor: Color = FilledIconButtonTokens.DisabledContainerColor.value
            .toDisabledColor(disabledAlpha = FilledIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledIconButtonTokens.DisabledContentColor.value
            .toDisabledColor(disabledAlpha = FilledIconButtonTokens.DisabledContentOpacity)
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [FilledTonalIconButton]- by default,
     * a muted colored background with a contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    fun filledTonalIconButtonColors(
        containerColor: Color = FilledTonalIconButtonTokens.ContainerColor.value,
        contentColor: Color = FilledTonalIconButtonTokens.ContentColor.value,
        disabledContainerColor: Color = FilledTonalIconButtonTokens.DisabledContainerColor.value
            .toDisabledColor(disabledAlpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledTonalIconButtonTokens.DisabledContentColor.value
            .toDisabledColor(disabledAlpha = FilledTonalIconButtonTokens.DisabledContentOpacity)
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [OutlinedIconButton]- by default,
     * a transparent background with contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param contentColor The color of this icon button when enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    fun outlinedIconButtonColors(
        contentColor: Color = OutlinedIconButtonTokens.ContentColor.value,
        disabledContentColor: Color = OutlinedIconButtonTokens.DisabledContentColor.value
            .toDisabledColor(OutlinedIconButtonTokens.DisabledContentOpacity)
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [IconButton] - by default,
     * a transparent background with a contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    fun iconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = IconButtonTokens.ContentColor.value,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = IconButtonTokens.DisabledContentColor.value.toDisabledColor(
            disabledAlpha = IconButtonTokens.DisabledContentOpacity
        )
    ): IconButtonColors = IconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [ToggleButtonColors] for a [IconToggleButton]
     * - by default, a colored background with a contrasting content color.
     * If the button is disabled, then the colors will have an alpha
     * ([ContentAlpha.disabled]) value applied.
     *
     * @param checkedContainerColor The container color of this [IconToggleButton] when enabled
     * and checked
     * @param checkedContentColor The content color of this [IconToggleButton] when enabled and
     * checked
     * @param uncheckedContainerColor The container color of this [IconToggleButton] when enabled
     * and unchecked
     * @param uncheckedContentColor The content color of this [IconToggleButton] when enabled and
     * unchecked
     * @param disabledCheckedContainerColor The container color of this [IconToggleButton] when
     * checked and not enabled
     * @param disabledCheckedContentColor The content color of this [IconToggleButton] when checked
     * and not enabled
     * @param disabledUncheckedContainerColor The container color of this [IconToggleButton] when
     * unchecked and not enabled
     * @param disabledUncheckedContentColor The content color of this [IconToggleButton] when
     * unchecked and not enabled
     */
    @Composable
    fun iconToggleButtonColors(
        checkedContainerColor: Color = IconToggleButtonTokens.CheckedContainerColor.value,
        checkedContentColor: Color = IconToggleButtonTokens.CheckedContentColor.value,
        uncheckedContainerColor: Color = IconToggleButtonTokens.UncheckedContainerColor.value,
        uncheckedContentColor: Color = IconToggleButtonTokens.UncheckedContentColor.value,
        disabledCheckedContainerColor: Color = IconToggleButtonTokens.DisabledCheckedContainerColor
            .value.toDisabledColor(IconToggleButtonTokens.DisabledCheckedContainerOpacity),
        disabledCheckedContentColor: Color = IconToggleButtonTokens.DisabledCheckedContentColor
            .value.toDisabledColor(IconToggleButtonTokens.DisabledCheckedContentOpacity),
        disabledUncheckedContainerColor: Color = IconToggleButtonTokens
            .DisabledUncheckedContainerColor.value
            .toDisabledColor(IconToggleButtonTokens.DisabledUncheckedContainerOpacity),
        disabledUncheckedContentColor: Color = IconToggleButtonTokens.DisabledUncheckedContentColor
            .value.toDisabledColor(IconToggleButtonTokens.DisabledUncheckedContentOpacity),
    ): ToggleButtonColors {
        return ToggleButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
        )
    }

    /**
     * The recommended size of an icon when used inside an icon button with size
     * [SmallButtonSize] or [ExtraSmallButtonSize].
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val SmallIconSize = IconButtonTokens.IconSmallSize

    /**
     * The default size of an icon when used inside an icon button of size DefaultButtonSize.
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val DefaultIconSize = IconButtonTokens.IconDefaultSize

    /**
     * The size of an icon when used inside an icon button with size [LargeButtonSize].
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val LargeIconSize = IconButtonTokens.IconLargeSize

    /**
     * The recommended background size of an extra small, compact button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val ExtraSmallButtonSize = IconButtonTokens.ContainerExtraSmallSize

    /**
     * The recommended size for a small button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val SmallButtonSize = IconButtonTokens.ContainerSmallSize

    /**
     * The default size applied for buttons.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val DefaultButtonSize = IconButtonTokens.ContainerDefaultSize

    /**
     * The recommended size for a large button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val LargeButtonSize = IconButtonTokens.ContainerLargeSize
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * - See [IconButtonDefaults.filledIconButtonColors] and
 * [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonColors] for the default colors used in an
 * [OutlinedIconButton].
 *
 * @param containerColor the background color of this icon button when enabled.
 * @param contentColor the color of this icon when enabled.
 * @param disabledContainerColor the background color of this icon button when not enabled.
 * @param disabledContentColor the color of this icon when not enabled.
 */
@Immutable
class IconButtonColors constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Represents the container color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    internal fun containerColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) containerColor else disabledContainerColor)
    }

    /**
     * Represents the content color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    internal fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()

        return result
    }
}
