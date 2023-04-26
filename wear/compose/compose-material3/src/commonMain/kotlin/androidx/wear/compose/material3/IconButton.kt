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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

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
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the icon button's shape. It is strongly recommended to use the default
 * as this shape is a key characteristic of the Wear Material3 design.
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 * this button in different states. See [IconButtonDefaults.outlinedIconButtonBorder].
 * @param border Optional [BorderStroke] for the icon button border.
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
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable BoxScope.() -> Unit,
) = IconButton(onClick, modifier, enabled, shape, colors, border, interactionSource, content)

/**
 * Contains the default values used by [IconButton].
 */
object IconButtonDefaults {
    /**
     * Recommended [Shape] for [IconButton].
     */
    val shape = CircleShape

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
     * @param containerColor The background color of this icon button when enabled
     * @param contentColor The color of this icon button when enabled
     */
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [FilledTonalIconButton]- by default,
     * a muted colored background with a contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled
     * @param contentColor The color of this icon button when enabled
     */
    @Composable
    fun filledTonalIconButtonColors(
        containerColor: Color = MaterialTheme.colorScheme.surface,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [OutlinedIconButton]- by default,
     * a transparent background with contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param contentColor The color of this icon button when enabled
     */
    @Composable
    fun outlinedIconButtonColors(
        contentColor: Color = MaterialTheme.colorScheme.primary,
    ): IconButtonColors {
        return iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        )
    }

    /**
     * Creates a [ButtonColors] with the colors for [IconButton] - by default,
     * a transparent background with a contrasting icon color.
     * If the icon button is disabled then the colors will default to
     * the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor the background color of this icon button when enabled
     * @param contentColor the color of this icon when enabled
     * @param disabledContainerColor the background color of this icon button when not enabled
     * @param disabledContentColor the color of this icon when not enabled
     */
    @Composable
    fun iconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onBackground,
        disabledContainerColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = DisabledBorderAndContainerAlpha
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = ContentAlpha.disabled
        )
    ): IconButtonColors = IconButtonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [BorderStroke], such as for an [OutlinedIconButton]
     *
     * @param borderColor The color to use for the border for this outline when enabled
     * @param disabledBorderColor The color to use for the border for this outline when
     * disabled
     * @param borderWidth The width to use for the border for this outline. It is strongly
     * recommended to use the default width as this outline is a key characteristic
     * of Wear Material3.
     */
    @Composable
    fun outlinedIconButtonBorder(
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

    /**
     * The recommended size of an icon when used inside an icon button with size
     * [SmallButtonSize] or [ExtraSmallButtonSize].
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val SmallIconSize = 24.dp

    /**
     * The default size of an icon when used inside an icon button of size DefaultButtonSize.
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val DefaultIconSize = 26.dp

    /**
     * The size of an icon when used inside an icon button with size [LargeButtonSize].
     * Use [iconSizeFor] to easily determine the icon size.
     */
    val LargeIconSize = 30.dp

    /**
     * The recommended background size of an extra small, compact button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val ExtraSmallButtonSize = 32.dp

    /**
     * The recommended size for a small button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val SmallButtonSize = 48.dp

    /**
     * The default size applied for buttons.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val DefaultButtonSize = 52.dp

    /**
     * The recommended size for a large button.
     * It is recommended to apply this size using Modifier.touchTargetAwareSize.
     */
    val LargeButtonSize = 60.dp
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * - See [IconButtonDefaults.filledIconButtonColors] and
 * [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonColors] for the default colors used in an
 * [OutlinedIconButton].
 */
@Immutable
class IconButtonColors internal constructor(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
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
