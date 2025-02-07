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
import androidx.compose.foundation.Indication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.wear.compose.material3.tokens.FilledIconButtonTokens
import androidx.wear.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.wear.compose.material3.tokens.IconButtonTokens
import androidx.wear.compose.material3.tokens.OutlinedIconButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens

/**
 * Wear Material [IconButton] is a circular, icon-only button with transparent background and no
 * border. It offers a single slot to take icon or image content.
 *
 * Set the size of the [IconButton] with Modifier.[touchTargetAwareSize] to ensure that the
 * recommended minimum touch target size is available.
 *
 * The recommended [IconButton] sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a given [IconButtonDefaults]
 * size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [IconButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of an [IconButton]:
 *
 * @sample androidx.wear.compose.material3.samples.IconButtonSample
 *
 * Example of an [IconButton] with onLongClick:
 *
 * @sample androidx.wear.compose.material3.samples.IconButtonWithOnLongClickSample
 *
 * Example of an [IconButton] with shape animation of rounded corners on press:
 *
 * @sample androidx.wear.compose.material3.samples.IconButtonWithCornerAnimationSample
 *
 * Example of an [IconButton] with image content:
 *
 * @sample androidx.wear.compose.material3.samples.IconButtonWithImageSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shapes Defines the shape for this button. Defaults to a static shape based on
 *   [IconButtonDefaults.shape], but animated versions are available through
 *   [IconButtonDefaults.animatedShapes].
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 *   this button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
// TODO(b/261838497) Add Material3 samples and UX guidance links
@Composable
public fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    IconButtonImpl(
        onClick = onClick,
        modifier =
            modifier.minimumInteractiveComponentSize().size(IconButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shapes = shapes,
        border = { border },
        ripple = ripple(),
        content = provideScopeContent(colors.contentColor(enabled = enabled), content)
    )
}

/**
 * Wear Material [FilledIconButton] is a circular, icon-only button with a colored background and a
 * contrasting content color. It offers a single slot to take an icon or image.
 *
 * Set the size of the [FilledIconButton] with Modifier.[touchTargetAwareSize] to ensure that the
 * recommended minimum touch target size is available.
 *
 * The recommended [IconButton] sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a given [IconButton] size, or
 * refer to icon sizes [IconButtonDefaults.SmallIconSize], [IconButtonDefaults.DefaultIconSize],
 * [IconButtonDefaults.LargeIconSize] directly.
 *
 * [FilledIconButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of [FilledIconButton]:
 *
 * @sample androidx.wear.compose.material3.samples.FilledIconButtonSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shapes Defines the shape for this button. Defaults to a static shape based on
 *   [IconButtonDefaults.shape], but animated versions are available through
 *   [IconButtonDefaults.animatedShapes].
 * @param colors [IconButtonColors] that will be used to resolve the container and content color for
 *   this icon button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
// TODO(b/261838497) Add Material3 samples and UX guidance links
@Composable
public fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    IconButtonImpl(
        onClick = onClick,
        modifier =
            modifier.minimumInteractiveComponentSize().size(IconButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shapes = shapes,
        border = { border },
        ripple = ripple(),
        content = provideScopeContent(colors.contentColor(enabled = enabled), content)
    )
}

/**
 * Wear Material [FilledTonalIconButton] is a circular, icon-only button with a muted, colored
 * background and a contrasting icon color. It offers a single slot to take an icon or image.
 *
 * Set the size of the [FilledTonalIconButton] with Modifier.[touchTargetAwareSize] to ensure that
 * the recommended minimum touch target size is available.
 *
 * The recommended icon button sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a given [IconButtonDefaults]
 * size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [FilledTonalIconButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * Example of [FilledTonalIconButton]:
 *
 * @sample androidx.wear.compose.material3.samples.FilledTonalIconButtonSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shapes Defines the shape for this button. Defaults to a static shape based on
 *   [IconButtonDefaults.shape], but animated versions are available through
 *   [IconButtonDefaults.animatedShapes].
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 *   this button in different states.
 * @param border Optional [BorderStroke] for the icon button border.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
// TODO(b/261838497) Add Material3 samples and UX guidance links
@Composable
public fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    IconButtonImpl(
        onClick = onClick,
        modifier =
            modifier.minimumInteractiveComponentSize().size(IconButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shapes = shapes,
        border = { border },
        ripple = ripple(),
        content = provideScopeContent(colors.contentColor(enabled = enabled), content)
    )
}

/**
 * Wear Material [OutlinedIconButton] is a circular, icon-only button with a transparent background,
 * contrasting icon color and border. It offers a single slot to take an icon or image.
 *
 * Set the size of the [OutlinedIconButton] with Modifier.[touchTargetAwareSize] to ensure that the
 * recommended minimum touch target size is available.
 *
 * The recommended icon button sizes are [IconButtonDefaults.DefaultButtonSize],
 * [IconButtonDefaults.LargeButtonSize], [IconButtonDefaults.SmallButtonSize] and
 * [IconButtonDefaults.ExtraSmallButtonSize].
 *
 * Use [IconButtonDefaults.iconSizeFor] to determine the icon size for a given [IconButtonDefaults]
 * size, or refer to icon sizes [IconButtonDefaults.SmallIconSize],
 * [IconButtonDefaults.DefaultIconSize], [IconButtonDefaults.LargeButtonSize] directly.
 *
 * [OutlinedIconButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * An [OutlinedIconButton] has a transparent background and a thin border by default with content
 * taking the theme primary color.
 *
 * Example of [OutlinedIconButton]:
 *
 * @sample androidx.wear.compose.material3.samples.OutlinedIconButtonSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shapes Defines the shape for this button. Defaults to a static shape based on
 *   [IconButtonDefaults.shape], but animated versions are available through
 *   [IconButtonDefaults.animatedShapes].
 * @param colors [IconButtonColors] that will be used to resolve the background and icon color for
 *   this button in different states. See [IconButtonDefaults.outlinedIconButtonColors].
 * @param border Optional [BorderStroke] for the icon button
 *   border - [ButtonDefaults.outlinedButtonBorder] by default.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the icon button, expected to be icon or image.
 */
// TODO(b/261838497) Add Material3 samples and UX guidance links
@Composable
public fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shapes: IconButtonShapes = IconButtonDefaults.shapes(),
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    IconButtonImpl(
        onClick = onClick,
        modifier =
            modifier.minimumInteractiveComponentSize().size(IconButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shapes = shapes,
        border = { border },
        ripple = ripple(),
        content = provideScopeContent(colors.contentColor(enabled = enabled), content)
    )
}

@Composable
internal fun IconButtonImpl(
    onClick: () -> Unit,
    modifier: Modifier,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    enabled: Boolean,
    backgroundColor: @Composable (enabled: Boolean) -> Color,
    interactionSource: MutableInteractionSource?,
    shapes: IconButtonShapes,
    border: @Composable (enabled: Boolean) -> BorderStroke?,
    ripple: Indication,
    content: @Composable BoxScope.() -> Unit,
) {
    val (finalShape, finalInteractionSource) =
        animateButtonShape(
            defaultShape = shapes.shape,
            pressedShape = shapes.pressedShape,
            onPressAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>().faster(200f),
            onReleaseAnimationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            interactionSource = interactionSource
        )

    RoundButton(
        onClick,
        modifier,
        onLongClick,
        onLongClickLabel,
        enabled,
        backgroundColor,
        finalInteractionSource,
        finalShape,
        border,
        ripple,
        content
    )
}

/** Contains the default values used by [IconButton]. */
public object IconButtonDefaults {
    /** Recommended [Shape] for [IconButton]. */
    public val shape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerFull

    /** Recommended pressed [Shape] for [IconButton]. */
    public val pressedShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.small

    /** Recommended alpha to apply to an IconButton with Image content with disabled */
    public val DisabledImageOpacity: Float = DisabledContentAlpha

    /** Creates an [IconButtonShapes] with a static [shape]. */
    @Composable public fun shapes(): IconButtonShapes = MaterialTheme.shapes.defaultShapes

    /**
     * Creates an [IconButtonShapes] with a static [shape].
     *
     * @param shape The normal shape of the IconButton.
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
    ): IconButtonShapes = MaterialTheme.shapes.defaultShapes.copy(shape = shape)

    /**
     * Creates a [Shape] with a animation between two CornerBasedShapes.
     *
     * A simple icon button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.IconButtonWithCornerAnimationSample
     *
     * A simple icon toggle button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.IconToggleButtonSample
     */
    @Composable
    public fun animatedShapes(): IconButtonShapes = MaterialTheme.shapes.defaultAnimatedShapes

    /**
     * Creates a [Shape] with a animation between two CornerBasedShapes.
     *
     * A simple icon button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.IconButtonWithCornerAnimationSample
     *
     * A simple icon toggle button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.IconToggleButtonSample
     * @param shape The normal shape of the IconButton.
     * @param pressedShape The pressed shape of the IconButton.
     */
    @Composable
    public fun animatedShapes(
        shape: CornerBasedShape = IconButtonDefaults.shape,
        pressedShape: CornerBasedShape = IconButtonDefaults.pressedShape,
    ): IconButtonShapes =
        MaterialTheme.shapes.defaultAnimatedShapes.copy(shape = shape, pressedShape = pressedShape)

    /**
     * Recommended icon size for a given icon button size.
     *
     * Ensures that the minimum recommended icon size is applied.
     *
     * Examples: for size [LargeButtonSize], returns [LargeIconSize], for size
     * [ExtraSmallButtonSize] returns [SmallIconSize].
     *
     * @param buttonSize The size of the icon button
     */
    public fun iconSizeFor(buttonSize: Dp): Dp =
        if (buttonSize >= LargeButtonSize) {
            LargeIconSize
        } else {
            max(SmallIconSize, buttonSize / 2f)
        }

    /**
     * Creates a [IconButtonColors] with the colors for [FilledIconButton] - by default, a colored
     * background with a contrasting icon color. If the icon button is disabled then the colors will
     * default to the MaterialTheme onSurface color with suitable alpha values applied.
     */
    @Composable
    public fun filledIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconButtonColors

    /**
     * Creates a [IconButtonColors] with the colors for [FilledIconButton] - by default, a colored
     * background with a contrasting icon color. If the icon button is disabled then the colors will
     * default to the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    public fun filledIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [IconButtonColors] as an alternative to the [filledTonalIconButtonColors], giving a
     * surface with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * Example of creating a [FilledIconButton] with [filledVariantIconButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledVariantIconButtonSample
     */
    @Composable
    public fun filledVariantIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledVariantIconButtonColors

    /**
     * Creates a [IconButtonColors] as an alternative to the [filledTonalIconButtonColors], giving a
     * surface with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * Example of creating a [FilledIconButton] with [filledVariantIconButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledVariantIconButtonSample
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    public fun filledVariantIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledVariantIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [IconButtonColors] with the colors for [FilledTonalIconButton]- by default, a muted
     * colored background with a contrasting icon color. If the icon button is disabled then the
     * colors will default to the MaterialTheme onSurface color with suitable alpha values applied.
     */
    @Composable
    public fun filledTonalIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconButtonColors

    /**
     * Creates a [IconButtonColors] with the colors for [FilledTonalIconButton]- by default, a muted
     * colored background with a contrasting icon color. If the icon button is disabled then the
     * colors will default to the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    public fun filledTonalIconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [IconButtonColors] with the colors for [OutlinedIconButton]- by default, a
     * transparent background with contrasting icon color. If the icon button is disabled then the
     * colors will default to the MaterialTheme onSurface color with suitable alpha values applied.
     */
    @Composable
    public fun outlinedIconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedIconButtonColors

    /**
     * Creates a [IconButtonColors] with the colors for [OutlinedIconButton]- by default, a
     * transparent background with contrasting icon color. If the icon button is disabled then the
     * colors will default to the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param contentColor The color of this icon button when enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    public fun outlinedIconButtonColors(
        contentColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedIconButtonColors.copy(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [IconButtonColors] with the colors for [IconButton] - by default, a transparent
     * background with a contrasting icon color. If the icon button is disabled then the colors will
     * default to the MaterialTheme onSurface color with suitable alpha values applied.
     */
    @Composable
    public fun iconButtonColors(): IconButtonColors =
        MaterialTheme.colorScheme.defaultIconButtonColors

    /**
     * Creates a [IconButtonColors] with the colors for [IconButton] - by default, a transparent
     * background with a contrasting icon color. If the icon button is disabled then the colors will
     * default to the MaterialTheme onSurface color with suitable alpha values applied.
     *
     * @param containerColor The background color of this icon button when enabled.
     * @param contentColor The color of this icon when enabled.
     * @param disabledContainerColor The background color of this icon button when not enabled.
     * @param disabledContentColor The color of this icon when not enabled.
     */
    @Composable
    public fun iconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Color.Unspecified
    ): IconButtonColors =
        MaterialTheme.colorScheme.defaultIconButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * The recommended size of an icon when used inside an icon button with size [SmallButtonSize]
     * or [ExtraSmallButtonSize]. Use [iconSizeFor] to easily determine the icon size.
     */
    public val SmallIconSize: Dp = IconButtonTokens.IconSmallSize

    /**
     * The default size of an icon when used inside an icon button of size DefaultButtonSize. Use
     * [iconSizeFor] to easily determine the icon size.
     */
    public val DefaultIconSize: Dp = IconButtonTokens.IconDefaultSize

    /**
     * The size of an icon when used inside an icon button with size [LargeButtonSize]. Use
     * [iconSizeFor] to easily determine the icon size.
     */
    public val LargeIconSize: Dp = IconButtonTokens.IconLargeSize

    /**
     * The recommended background size of an extra small, compact button. It is recommended to apply
     * this size using Modifier.touchTargetAwareSize.
     */
    public val ExtraSmallButtonSize: Dp = IconButtonTokens.ContainerExtraSmallSize

    /**
     * The recommended size for a small button. It is recommended to apply this size using
     * Modifier.touchTargetAwareSize.
     */
    public val SmallButtonSize: Dp = IconButtonTokens.ContainerSmallSize

    /**
     * The default size applied for buttons. It is recommended to apply this size using
     * Modifier.touchTargetAwareSize.
     */
    public val DefaultButtonSize: Dp = IconButtonTokens.ContainerDefaultSize

    /**
     * The recommended size for a large button. It is recommended to apply this size using
     * Modifier.touchTargetAwareSize.
     */
    public val LargeButtonSize: Dp = IconButtonTokens.ContainerLargeSize

    internal val Shapes.defaultShapes: IconButtonShapes
        @Composable
        get() {
            return defaultIconButtonShapesCached
                ?: IconButtonShapes(shape = shape).also { defaultIconButtonShapesCached = it }
        }

    internal val Shapes.defaultAnimatedShapes: IconButtonShapes
        @Composable
        get() {
            return defaultIconButtonAnimatedShapesCached
                ?: IconButtonShapes(shape = shape, pressedShape = pressedShape).also {
                    defaultIconButtonAnimatedShapesCached = it
                }
        }

    private val ColorScheme.defaultFilledIconButtonColors: IconButtonColors
        get() {
            return defaultFilledIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = fromToken(FilledIconButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledIconButtonTokens.ContentColor),
                        disabledContainerColor =
                            fromToken(FilledIconButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledIconButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledIconButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledIconButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledIconButtonColorsCached = it }
        }

    private val ColorScheme.defaultFilledVariantIconButtonColors: IconButtonColors
        get() {
            return defaultFilledVariantIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = fromToken(FilledIconButtonTokens.VariantContainerColor),
                        contentColor = fromToken(FilledIconButtonTokens.VariantContentColor),
                        disabledContainerColor =
                            fromToken(FilledIconButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledIconButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledIconButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledIconButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledVariantIconButtonColorsCached = it }
        }

    private val ColorScheme.defaultFilledTonalIconButtonColors: IconButtonColors
        get() {
            return defaultFilledTonalIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = fromToken(FilledTonalIconButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalIconButtonTokens.ContentColor),
                        disabledContainerColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        FilledTonalIconButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledTonalIconButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        FilledTonalIconButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledTonalIconButtonColorsCached = it }
        }

    private val ColorScheme.defaultOutlinedIconButtonColors: IconButtonColors
        get() {
            return defaultOutlinedIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedIconButtonTokens.ContentColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedIconButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = OutlinedIconButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultOutlinedIconButtonColorsCached = it }
        }

    private val ColorScheme.defaultIconButtonColors: IconButtonColors
        get() {
            return defaultIconButtonColorsCached
                ?: IconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(IconButtonTokens.ContentColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(IconButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = IconButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultIconButtonColorsCached = it }
        }
}

/**
 * Represents the container and content colors used in an icon button in different states.
 * - See [IconButtonDefaults.filledIconButtonColors] and
 *   [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 *   [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonColors] for the default colors used in an
 *   [OutlinedIconButton].
 *
 * @param containerColor the background color of this icon button when enabled.
 * @param contentColor the color of this icon when enabled.
 * @param disabledContainerColor the background color of this icon button when not enabled.
 * @param disabledContentColor the color of this icon when not enabled.
 */
@Immutable
public class IconButtonColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val disabledContainerColor: Color,
    public val disabledContentColor: Color,
) {
    /**
     * Returns a copy of this [IconButtonColors], optionally overriding some of the values.
     *
     * @param containerColor the background color of this icon button when enabled.
     * @param contentColor the color of this icon when enabled.
     * @param disabledContainerColor the background color of this icon button when not enabled.
     * @param disabledContentColor the color of this icon when not enabled.
     */
    public fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor
    ): IconButtonColors =
        IconButtonColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            disabledContainerColor =
                disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor = disabledContentColor.takeOrElse { this.disabledContentColor }
        )

    /**
     * Represents the container color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color {
        return if (enabled) containerColor else disabledContainerColor
    }

    /**
     * Represents the content color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color {
        return if (enabled) contentColor else disabledContentColor
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

/**
 * Represents the shapes used for [IconButton] in various states.
 *
 * If [pressedShape] is non null the shape will be animated on press.
 *
 * @param shape the shape of the icon button when enabled
 * @param pressedShape the shape of the icon button when pressed
 */
public class IconButtonShapes(
    public val shape: Shape,
    public val pressedShape: Shape? = null,
) {
    public fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
    ): IconButtonShapes =
        IconButtonShapes(
            shape = shape ?: this.shape,
            pressedShape = pressedShape ?: this.pressedShape
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()

        return result
    }
}
