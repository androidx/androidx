/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.ButtonLargeTokens
import androidx.compose.material3.tokens.ButtonMediumTokens
import androidx.compose.material3.tokens.ButtonSmallTokens
import androidx.compose.material3.tokens.ButtonXLargeTokens
import androidx.compose.material3.tokens.ButtonXSmallTokens
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.FilledTonalButtonTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TonalButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.jvm.JvmInline

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * Toggle button is a filled toggleable button that switches between primary and tonal colors
 * depending on [checked]'s value. It also morphs between the three shapes provided in [shapes]
 * depending on the state of the interaction with the toggle button as long as the three shapes
 * provided are [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle
 * button will toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * ![Filled toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-toggle-buttons.png)
 *
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [ToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. See
 *   [ToggleButtonElevation.shadowElevation]. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 *
 * [ToggleButton] with text content sample:
 *
 * @sample androidx.compose.material3.samples.ToggleButtonSample
 *
 * [ToggleButton] with icon and text sample:
 *
 * @sample androidx.compose.material3.samples.ToggleButtonWithIconSample
 *
 * [ToggleButton] uses the small button design as default. For a [ToggleButton] that uses the design
 * for extra small:
 *
 * @sample androidx.compose.material3.samples.XSmallToggleButtonWithIconSample
 *
 * For a [ToggleButton] that uses the design for medium:
 *
 * @sample androidx.compose.material3.samples.MediumToggleButtonWithIconSample
 *
 * For a [ToggleButton] that uses the design for large:
 *
 * @sample androidx.compose.material3.samples.LargeToggleButtonWithIconSample
 *
 * For a [ToggleButton] that uses the design for extra large:
 *
 * @sample androidx.compose.material3.samples.XLargeToggleButtonWithIconSample
 * @see [Button] for a static button that doesn't need to be toggled.
 * @see [IconToggleButton] for a toggleable button where the content is specifically an [Icon].
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = ToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = ToggleButtonSize.Small,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * This overload accepts an explicit [buttonSize] and optional [icon] to automatically configure
 * container size, shapes, content padding, icon sizes, icon spacing, and typography.
 *
 * There are multiple button size variants - providing a different [ToggleButtonSize] will affect
 * default values used inside this button, such as the corner shape and padding. Note that you can
 * still provide a size modifier such as [androidx.compose.foundation.layout.size] to change the
 * layout size of this button, [buttonSize] affects default values and values internal to the
 * button.
 *
 * @sample androidx.compose.material3.samples.ToggleButtonWithButtonSizeSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize the [ToggleButtonSize] of this toggle button, controlling its height, padding,
 *   and icon sizing.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param icon optional icon to be placed before the [content].
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [ToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 * @see [Button] for a static button that doesn't need to be toggled.
 * @see [IconToggleButton] for a toggleable button where the content is specifically an [Icon].
 */
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonSize.Small,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = ToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = ToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // TODO Load the motionScheme tokens from the component tokens file
    val defaultAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = colors.containerColor(enabled, checked)
    val contentColor = colors.contentColor(enabled, checked)
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp
    val buttonShape = shapeByInteraction(shapes, pressed, checked, defaultAnimationSpec)
    val animatedBorder = animateBorderStrokeAsState(border)

    val iconSize = ButtonDefaults.iconSizeFor(buttonSize.height)
    val iconSpacing = ButtonDefaults.iconSpacingFor(buttonSize.height)
    val textStyle = ButtonDefaults.textStyleFor(buttonSize.height)

    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = buttonShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = animatedBorder,
        interactionSource = interactionSource,
    ) {
        ProvideContentColorTextStyle(contentColor = contentColor, textStyle = textStyle) {
            Row(
                Modifier.defaultMinSize(minHeight = buttonSize.height).padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        Modifier.size(iconSize),
                        contentAlignment = Alignment.Center,
                        propagateMinConstraints = true,
                    ) {
                        icon()
                    }
                    Spacer(Modifier.width(iconSpacing))
                }
                content()
            }
        }
    }
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * ![Elevated toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-toggle-buttons.png)
 *
 * Elevated toggle buttons are high-emphasis Toggle buttons. To prevent shadow creep, only use them
 * when absolutely necessary, such as when the toggle button requires visual separation from
 * patterned container.
 *
 * @sample androidx.compose.material3.samples.ElevatedToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [ElevatedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [ElevatedToggleButtonDefaults.elevation].
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 * @see [ElevatedButton] for a static button that doesn't need to be toggled.
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
public fun ElevatedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ElevatedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = ElevatedToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ElevatedToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = ToggleButtonSize.Small,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * This overload accepts an explicit [buttonSize] and optional [icon] to automatically configure
 * container size, shapes, content padding, icon sizes, icon spacing, and typography.
 *
 * There are multiple button size variants - providing a different [ToggleButtonSize] will affect
 * default values used inside this button, such as the corner shape and padding. Note that you can
 * still provide a size modifier such as [androidx.compose.foundation.layout.size] to change the
 * layout size of this button, [buttonSize] affects default values and values internal to the
 * button.
 *
 * @sample androidx.compose.material3.samples.ToggleButtonWithButtonSizeSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize the [ToggleButtonSize] of this toggle button.
 * @param enabled controls the enabled state of this toggle button.
 * @param icon optional icon to be placed before the [content].
 * @param shapes the [ToggleButtonShapes] used for this toggle button.
 * @param colors [ToggleButtonColors] used for this toggle button. See
 *   [ElevatedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used for this button. See
 *   [ElevatedToggleButtonDefaults.elevation].
 * @param border the border to draw around the container.
 * @param contentPadding the spacing values to apply internally.
 * @param interactionSource an optional hoisted [MutableInteractionSource].
 * @param content The content displayed on the toggle button.
 */
@Composable
public fun ElevatedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonSize.Small,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = ElevatedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = ElevatedToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * ![Filled Tonal toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/tonal-toggle-buttons.png)
 *
 * tonal toggle buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [ToggleButton]s (filled) and [OutlinedToggleButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give. Tonal toggle
 * buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [FilledTonalToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay.
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 * @see [FilledTonalButton] for a static button that doesn't need to be toggled.
 * @see [FilledTonalIconToggleButton] for a toggleable button where the content is specifically an
 *   [Icon].
 *
 * @material3expressive
 */
@Deprecated(message = "Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@ExperimentalMaterial3ExpressiveApi
@Composable
public fun TonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
    colors: ToggleButtonColors = FilledTonalToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = FilledTonalToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = ToggleButtonSize.Small,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * ![Filled Tonal toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/tonal-toggle-buttons.png)
 *
 * filled tonal toggle buttons are medium-emphasis buttons that is an alternative middle ground
 * between default [ToggleButton]s (filled) and [OutlinedToggleButton]s. They can be used in
 * contexts where lower-priority button requires slightly more emphasis than an outline would give.
 * Tonal toggle buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [FilledTonalToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [FilledTonalToggleButtonDefaults.elevation].
 * @param border the border to draw around the container of this toggle button.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 * @see [FilledTonalButton] for a static button that doesn't need to be toggled.
 * @see [FilledTonalIconToggleButton] for a toggleable button where the content is specifically an
 *   [Icon].
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
public fun FilledTonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
    colors: ToggleButtonColors = FilledTonalToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = FilledTonalToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    FilledTonalToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = ToggleButtonSize.Small,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * This overload accepts an explicit [buttonSize] and optional [icon] to automatically configure
 * container size, shapes, content padding, icon sizes, icon spacing, and typography.
 *
 * There are multiple button size variants - providing a different [ToggleButtonSize] will affect
 * default values used inside this button, such as the corner shape and padding. Note that you can
 * still provide a size modifier such as [androidx.compose.foundation.layout.size] to change the
 * layout size of this button, [buttonSize] affects default values and values internal to the
 * button.
 *
 * @sample androidx.compose.material3.samples.ToggleButtonWithButtonSizeSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize the [ToggleButtonSize] of this toggle button.
 * @param enabled controls the enabled state of this toggle button.
 * @param icon optional icon to be placed before the [content].
 * @param shapes the [ToggleButtonShapes] used for this toggle button.
 * @param colors [ToggleButtonColors] used for this toggle button. See
 *   [FilledTonalToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used for this button. See
 *   [FilledTonalToggleButtonDefaults.elevation].
 * @param border the border to draw around the container.
 * @param contentPadding the spacing values to apply internally.
 * @param interactionSource an optional hoisted [MutableInteractionSource].
 * @param content The content displayed on the toggle button.
 */
@Composable
public fun FilledTonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonSize.Small,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = FilledTonalToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = FilledTonalToggleButtonDefaults.elevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided are
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ToggleButtonShapes] according to user interaction.
 *
 * ![Outlined toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-toggle-buttons.png)
 *
 * Outlined toggle buttons are medium-emphasis buttons. They contain actions that are important, but
 * are not the primary action in an app. Outlined buttons pair well with [ToggleButton]s to indicate
 * an alternative, secondary action.
 *
 * @sample androidx.compose.material3.samples.OutlinedToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ToggleButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [OutlinedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used to resolve the elevation for this button in
 *   different states. This controls the size of the shadow below the button. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay.
 * @param border the border to draw around the container of this toggle button. See
 *   [OutlinedToggleButtonDefaults.border].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content The content displayed on the toggle button, expected to be text, icon or image.
 * @see [OutlinedButton] for a static button that doesn't need to be toggled.
 * @see [OutlinedIconToggleButton] for a toggleable button where the content is specifically an
 *   [Icon].
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
public fun OutlinedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight),
    colors: ToggleButtonColors = OutlinedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = null,
    border: BorderStroke? = OutlinedToggleButtonDefaults.border(enabled, checked),
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(ToggleButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = ToggleButtonSize.Small,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * [Material Design toggle
 * button](https://m3.material.io/components/buttons/overview#f8ba981c-a363-4ccd-a332-ee1b0e124e5c)
 *
 * This overload accepts an explicit [buttonSize] and optional [icon] to automatically configure
 * container size, shapes, content padding, icon sizes, icon spacing, and typography.
 *
 * There are multiple button size variants - providing a different [ToggleButtonSize] will affect
 * default values used inside this button, such as the corner shape and padding. Note that you can
 * still provide a size modifier such as [androidx.compose.foundation.layout.size] to change the
 * layout size of this button, [buttonSize] affects default values and values internal to the
 * button.
 *
 * @sample androidx.compose.material3.samples.ToggleButtonWithButtonSizeSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param buttonSize the [ToggleButtonSize] of this toggle button.
 * @param enabled controls the enabled state of this toggle button.
 * @param icon optional icon to be placed before the [content].
 * @param shapes the [ToggleButtonShapes] used for this toggle button.
 * @param colors [ToggleButtonColors] used for this toggle button. See
 *   [OutlinedToggleButtonDefaults.colors].
 * @param elevation [ToggleButtonElevation] used for this button.
 * @param border the border to draw around the container. See [OutlinedToggleButtonDefaults.border].
 * @param contentPadding the spacing values to apply internally.
 * @param interactionSource an optional hoisted [MutableInteractionSource].
 * @param content The content displayed on the toggle button.
 */
@Composable
public fun OutlinedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: ToggleButtonSize = ToggleButtonSize.Small,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(buttonSize),
    colors: ToggleButtonColors = OutlinedToggleButtonDefaults.colors(),
    elevation: ToggleButtonElevation? = null,
    border: BorderStroke? = OutlinedToggleButtonDefaults.border(enabled, checked),
    contentPadding: PaddingValues =
        ToggleButtonDefaults.contentPaddingFor(buttonSize, hasStartIcon = icon != null),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        buttonSize = buttonSize,
        enabled = enabled,
        icon = icon,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * Represents the size of a [ToggleButton].
 *
 * Changing the toggle button size will affect default values used by a toggle button, such as
 * container height, shapes, padding, icon size, and icon spacing.
 */
@Immutable
@JvmInline
public value class ToggleButtonSize internal constructor(public val height: Dp) {
    public companion object {
        /** Extra Small toggle button size. */
        public val ExtraSmall: ToggleButtonSize =
            ToggleButtonSize(ButtonDefaults.ExtraSmallContainerHeight)

        /** Small toggle button size. */
        public val Small: ToggleButtonSize = ToggleButtonSize(ToggleButtonDefaults.MinHeight)

        /** Medium toggle button size. */
        public val Medium: ToggleButtonSize = ToggleButtonSize(ButtonDefaults.MediumContainerHeight)

        /** Large toggle button size. */
        public val Large: ToggleButtonSize = ToggleButtonSize(ButtonDefaults.LargeContainerHeight)

        /** Extra Large toggle button size. */
        public val ExtraLarge: ToggleButtonSize =
            ToggleButtonSize(ButtonDefaults.ExtraLargeContainerHeight)
    }
}

/** Contains the default values for all five toggle button types. */
public object ToggleButtonDefaults {
    /**
     * The default min height applied for all toggle button variants ([ToggleButton],
     * [ElevatedToggleButton], [FilledTonalToggleButton], [OutlinedToggleButton]).
     *
     * Override it by applying [Modifier.heightIn][androidx.compose.foundation.layout.heightIn]
     * directly on the toggle button composable.
     */
    public val MinHeight: Dp = ButtonSmallTokens.ContainerHeight

    private val ToggleButtonStartPadding = ButtonSmallTokens.LeadingSpace
    private val ToggleButtonEndPadding = ButtonSmallTokens.TrailingSpace
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside any toggle
     * button.
     */
    public val IconSpacing: Dp = ButtonSmallTokens.IconLabelSpace

    /**
     * The default size of the spacing between an icon and a text when they used inside any toggle
     * button.
     */
    public val IconSize: Dp = ButtonSmallTokens.IconSize

    private val ContentPadding: PaddingValues =
        PaddingValues(
            start = ToggleButtonStartPadding,
            top = ButtonVerticalPadding,
            end = ToggleButtonEndPadding,
            bottom = ButtonVerticalPadding,
        )

    /**
     * Recommended [PaddingValues] for a provided toggle button height.
     *
     * This padding is the same across all toggle button variants and should be used with
     * [ToggleButton], [ElevatedToggleButton], [FilledTonalToggleButton], and
     * [OutlinedToggleButton].
     *
     * The returned content padding is based on standard container height values and is not directly
     * interpolated from the provided [buttonHeight].
     *
     * @param buttonHeight the height of the toggle button
     * @param hasStartIcon whether the toggle button has a leading icon
     * @param hasEndIcon whether the toggle button has a trailing icon
     */
    public fun contentPaddingFor(
        buttonHeight: Dp,
        hasStartIcon: Boolean = false,
        hasEndIcon: Boolean = false,
    ): PaddingValues {
        val smallHeight = MinHeight
        val mediumHeight = ButtonDefaults.MediumContainerHeight
        val largeHeight = ButtonDefaults.LargeContainerHeight
        val xLargeHeight = ButtonDefaults.ExtraLargeContainerHeight
        return when {
            buttonHeight < smallHeight -> ExtraSmallContentPadding
            buttonHeight < mediumHeight -> getSmallContentPadding(hasStartIcon, hasEndIcon)
            buttonHeight < largeHeight -> getMediumContentPadding(hasStartIcon, hasEndIcon)
            buttonHeight < xLargeHeight -> getLargeContentPadding(hasStartIcon, hasEndIcon)
            else -> ExtraLargeContentPadding
        }
    }

    /**
     * Recommended [PaddingValues] for a provided [buttonSize].
     *
     * This padding is the same across all toggle button variants and should be used with
     * [ToggleButton], [ElevatedToggleButton], [FilledTonalToggleButton], and
     * [OutlinedToggleButton].
     *
     * @param buttonSize the [ToggleButtonSize] of the toggle button
     * @param hasStartIcon whether the toggle button has a leading icon
     * @param hasEndIcon whether the toggle button has a trailing icon
     */
    public fun contentPaddingFor(
        buttonSize: ToggleButtonSize,
        hasStartIcon: Boolean = false,
        hasEndIcon: Boolean = false,
    ): PaddingValues =
        contentPaddingFor(
            buttonHeight = buttonSize.height,
            hasStartIcon = hasStartIcon,
            hasEndIcon = hasEndIcon,
        )

    private val ExtraSmallContentPadding: PaddingValues
        get() = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)

    private val MediumContentPadding: PaddingValues
        get() =
            PaddingValues(
                start = MediumLeadingPadding,
                top = MediumVerticalPadding,
                end = MediumTrailingPadding,
                bottom = MediumVerticalPadding,
            )

    private fun getMediumContentPadding(hasLeadingIcon: Boolean, hasTrailingIcon: Boolean) =
        PaddingValues(
            start = if (hasLeadingIcon) IconMediumLeadingPadding else MediumLeadingPadding,
            top = MediumVerticalPadding,
            end = if (hasTrailingIcon) IconMediumTrailingPadding else MediumTrailingPadding,
            bottom = MediumVerticalPadding,
        )

    private val LargeContentPadding: PaddingValues
        get() =
            PaddingValues(
                start = LargeLeadingPadding,
                top = LargeVerticalPadding,
                end = LargeTrailingPadding,
                bottom = LargeVerticalPadding,
            )

    private fun getLargeContentPadding(hasLeadingIcon: Boolean, hasTrailingIcon: Boolean) =
        PaddingValues(
            start = if (hasLeadingIcon) IconLargeLeadingPadding else LargeLeadingPadding,
            top = LargeVerticalPadding,
            end = if (hasTrailingIcon) IconLargeTrailingPadding else LargeTrailingPadding,
            bottom = LargeVerticalPadding,
        )

    private val ExtraLargeContentPadding: PaddingValues
        get() =
            PaddingValues(
                start = ButtonXLargeTokens.LeadingSpace,
                end = ButtonXLargeTokens.TrailingSpace,
                top = 48.dp,
                bottom = 48.dp,
            )

    private fun getSmallContentPadding(hasStartIcon: Boolean, hasEndIcon: Boolean) =
        PaddingValues(
            start = if (hasStartIcon) iconSmallHorizontalPadding else ToggleButtonStartPadding,
            top = smallVerticalPadding,
            end = if (hasEndIcon) iconSmallHorizontalPadding else ToggleButtonEndPadding,
            bottom = smallVerticalPadding,
        )

    private val smallVerticalPadding
        get() = if (shouldUsePrecisionPointerComponentSizing.value) 8.dp else 10.dp

    private val iconSmallHorizontalPadding
        get() =
            if (shouldUsePrecisionPointerComponentSizing.value) 12.dp else ToggleButtonStartPadding

    private val MediumLeadingPadding = ButtonMediumTokens.LeadingSpace
    private val MediumTrailingPadding = ButtonMediumTokens.TrailingSpace
    private val MediumVerticalPadding = 16.dp
    private val IconMediumLeadingPadding = ButtonMediumTokens.LeadingSpace
    private val IconMediumTrailingPadding = ButtonMediumTokens.TrailingSpace
    private val LargeVerticalPadding = 32.dp
    private val LargeLeadingPadding = ButtonLargeTokens.LeadingSpace
    private val LargeTrailingPadding = ButtonLargeTokens.TrailingSpace
    private val IconLargeLeadingPadding = ButtonLargeTokens.LeadingSpace
    private val IconLargeTrailingPadding = ButtonLargeTokens.TrailingSpace

    /**
     * Creates a [ToggleButtonElevation] that will animate between the provided values according to
     * the Material specification for a [ToggleButton].
     *
     * @param defaultElevation the elevation used when the [ToggleButton] is enabled, and has no
     *   other [Interaction]s.
     * @param pressedElevation the elevation used when this [ToggleButton] is enabled and pressed.
     * @param focusedElevation the elevation used when the [ToggleButton] is enabled and focused.
     * @param hoveredElevation the elevation used when the [ToggleButton] is enabled and hovered.
     * @param disabledElevation the elevation used when the [ToggleButton] is not enabled.
     */
    @Composable
    public fun elevation(
        defaultElevation: Dp = FilledButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledButtonTokens.FocusedContainerElevation,
        hoveredElevation: Dp = FilledButtonTokens.HoveredContainerElevation,
        disabledElevation: Dp = FilledButtonTokens.DisabledContainerElevation,
    ): ToggleButtonElevation =
        ToggleButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [ToggleButtonShapes] that represents the default shape, pressedShape, and
     * checkedShape used in a [ToggleButton].
     */
    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun shapes(): ToggleButtonShapes = MaterialTheme.shapes.defaultToggleButtonShapes

    /**
     * Creates a [ToggleButtonShapes] that represents the default shape, pressedShape, and
     * checkedShape used in a [ToggleButton] and its variants.
     *
     * @param shape the unchecked shape for [ToggleButtonShapes]
     * @param pressedShape the unchecked shape for [ToggleButtonShapes]
     * @param checkedShape the unchecked shape for [ToggleButtonShapes]
     */
    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun shapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null,
    ): ToggleButtonShapes {
        val defaultShapes = MaterialTheme.shapes.defaultToggleButtonShapes
        return defaultShapes.copy(
            shape = shape ?: defaultShapes.shape,
            pressedShape = pressedShape ?: defaultShapes.pressedShape,
            checkedShape = checkedShape ?: defaultShapes.checkedShape,
        )
    }

    internal val Shapes.defaultToggleButtonShapes: ToggleButtonShapes
        get() {
            return defaultToggleButtonShapesCached
                ?: ToggleButtonShapes(
                        shape = fromToken(ButtonSmallTokens.ContainerShapeRound),
                        pressedShape = RoundedCornerShape(6.dp),
                        checkedShape = fromToken(ButtonSmallTokens.SelectedContainerShapeSquare),
                    )
                    .also { defaultToggleButtonShapesCached = it }
        }

    /** The default unchecked shape for [ToggleButton] */
    public val shape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** The default pressed shape for [ToggleButton] */
    public val pressedShape: Shape
        @Composable get() = RoundedCornerShape(6.dp)

    /** The default checked shape for [ToggleButton] */
    public val checkedShape: Shape
        @Composable get() = ButtonSmallTokens.SelectedContainerShapeSquare.value

    internal val extraSmallPressedShape: Shape
        @Composable get() = ButtonXSmallTokens.PressedContainerShape.value

    internal val mediumPressedShape: Shape
        @Composable get() = ButtonMediumTokens.PressedContainerShape.value

    internal val largePressedShape: Shape
        @Composable get() = ButtonLargeTokens.PressedContainerShape.value

    internal val extraLargePressedShape: Shape
        @Composable get() = ButtonXLargeTokens.PressedContainerShape.value

    internal val extraSmallCheckedShape: Shape
        @Composable get() = ButtonXSmallTokens.ContainerShapeSquare.value

    internal val mediumCheckedShape: Shape
        @Composable get() = ButtonMediumTokens.ContainerShapeSquare.value

    internal val largeCheckedShape: Shape
        @Composable get() = ButtonLargeTokens.ContainerShapeSquare.value

    internal val extraLargeCheckedShape: Shape
        @Composable get() = ButtonXLargeTokens.ContainerShapeSquare.value

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ToggleButton].
     */
    @Composable
    public fun colors(): ToggleButtonColors = MaterialTheme.colorScheme.defaultToggleButtonColors

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ToggleButton].
     *
     * @param containerColor the container color of this [ToggleButton] when enabled.
     * @param contentColor the content color of this [ToggleButton] when enabled.
     * @param disabledContainerColor the container color of this [ToggleButton] when not enabled.
     * @param disabledContentColor the content color of this [ToggleButton] when not enabled.
     * @param checkedContainerColor the container color of this [ToggleButton] when checked.
     * @param checkedContentColor the content color of this [ToggleButton] when checked.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    internal val ColorScheme.defaultToggleButtonColors: ToggleButtonColors
        get() {
            return defaultToggleButtonColorsCached
                ?: ToggleButtonColors(
                        containerColor = fromToken(FilledButtonTokens.UnselectedContainerColor),
                        contentColor =
                            fromToken(FilledButtonTokens.UnselectedPressedLabelTextColor),
                        disabledContainerColor =
                            fromToken(FilledButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = FilledButtonTokens.DisabledLabelTextOpacity),
                        checkedContainerColor =
                            fromToken(FilledButtonTokens.SelectedContainerColor),
                        checkedContentColor =
                            fromToken(FilledButtonTokens.SelectedPressedLabelTextColor),
                    )
                    .also { defaultToggleButtonColorsCached = it }
        }

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun elevatedToggleButtonColors(): ToggleButtonColors =
        ElevatedToggleButtonDefaults.colors()

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun elevatedToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        ElevatedToggleButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun tonalToggleButtonColors(): ToggleButtonColors =
        FilledTonalToggleButtonDefaults.colors()

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun tonalToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        FilledTonalToggleButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun filledTonalToggleButtonColors(): ToggleButtonColors =
        FilledTonalToggleButtonDefaults.colors()

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun filledTonalToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        FilledTonalToggleButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun outlinedToggleButtonColors(): ToggleButtonColors =
        OutlinedToggleButtonDefaults.colors()

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun outlinedToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        OutlinedToggleButtonDefaults.colors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    @Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
    @Composable
    public fun outlinedToggleButtonBorder(enabled: Boolean, checked: Boolean): BorderStroke? =
        OutlinedToggleButtonDefaults.border(enabled, checked)

    /**
     * Resolves the recommended [ToggleButtonShapes] for a given toggle button height.
     *
     * These shapes are the same across all toggle button variants and should be used with
     * [ToggleButton], [ElevatedToggleButton], [FilledTonalToggleButton], and
     * [OutlinedToggleButton].
     *
     * The input height is categorized into a shape bucket (such as extra small, small, medium,
     * large, or extra large) based on the closest standard button height.
     *
     * @param buttonHeight height of the button used to resolve the shape bucket
     */
    @Composable
    public fun shapesFor(buttonHeight: Dp): ToggleButtonShapes {
        val xSmallHeight = ButtonDefaults.ExtraSmallContainerHeight
        val smallHeight = ButtonDefaults.MinHeight
        val mediumHeight = ButtonDefaults.MediumContainerHeight
        val largeHeight = ButtonDefaults.LargeContainerHeight
        val xLargeHeight = ButtonDefaults.ExtraLargeContainerHeight
        return when {
            buttonHeight <= (xSmallHeight + smallHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = extraSmallPressedShape,
                    checkedShape = extraSmallCheckedShape,
                )
            buttonHeight <= (smallHeight + mediumHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes
            buttonHeight <= (mediumHeight + largeHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = mediumPressedShape,
                    checkedShape = mediumCheckedShape,
                )
            buttonHeight <= (largeHeight + xLargeHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = largePressedShape,
                    checkedShape = largeCheckedShape,
                )
            else ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = extraLargePressedShape,
                    checkedShape = extraLargeCheckedShape,
                )
        }
    }

    /**
     * Resolves the recommended [ToggleButtonShapes] for a given [buttonSize].
     *
     * These shapes are the same across all toggle button variants and should be used with
     * [ToggleButton], [ElevatedToggleButton], [FilledTonalToggleButton], and
     * [OutlinedToggleButton].
     *
     * @param buttonSize [ToggleButtonSize] used to resolve the shape bucket
     */
    @Composable
    public fun shapesFor(buttonSize: ToggleButtonSize): ToggleButtonShapes =
        shapesFor(buttonSize.height)
}

/** Contains default values used by [ElevatedToggleButton]. */
public object ElevatedToggleButtonDefaults {
    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ElevatedToggleButton].
     */
    @Composable
    public fun colors(): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultElevatedToggleButtonColors

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ElevatedToggleButton].
     *
     * @param containerColor the container color of this [ElevatedToggleButton] when enabled.
     * @param contentColor the content color of this [ElevatedToggleButton] when enabled.
     * @param disabledContainerColor the container color of this [ElevatedToggleButton] when not
     *   enabled.
     * @param disabledContentColor the content color of this [ElevatedToggleButton] when not
     *   enabled.
     * @param checkedContainerColor the container color of this [ElevatedToggleButton] when checked.
     * @param checkedContentColor the content color of this [ElevatedToggleButton] when checked.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultElevatedToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [ToggleButtonElevation] that will animate between the provided values according to
     * the Material specification for an [ElevatedToggleButton].
     *
     * @param defaultElevation the elevation used when the [ElevatedToggleButton] is enabled, and
     *   has no other [Interaction]s.
     * @param pressedElevation the elevation used when this [ElevatedToggleButton] is enabled and
     *   pressed.
     * @param focusedElevation the elevation used when the [ElevatedToggleButton] is enabled and
     *   focused.
     * @param hoveredElevation the elevation used when the [ElevatedToggleButton] is enabled and
     *   hovered.
     * @param disabledElevation the elevation used when the [ElevatedToggleButton] is not enabled.
     */
    @Composable
    public fun elevation(
        defaultElevation: Dp = ElevatedButtonTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedButtonTokens.FocusedContainerElevation,
        hoveredElevation: Dp = ElevatedButtonTokens.HoveredContainerElevation,
        disabledElevation: Dp = ElevatedButtonTokens.DisabledContainerElevation,
    ): ToggleButtonElevation =
        ToggleButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    internal val ColorScheme.defaultElevatedToggleButtonColors: ToggleButtonColors
        get() {
            return defaultElevatedToggleButtonColorsCached
                ?: ToggleButtonColors(
                        containerColor = fromToken(ElevatedButtonTokens.UnselectedContainerColor),
                        contentColor =
                            fromToken(ElevatedButtonTokens.UnselectedPressedLabelTextColor),
                        disabledContainerColor =
                            fromToken(ElevatedButtonTokens.DisabledContainerColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(ElevatedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledLabelTextOpacity),
                        checkedContainerColor =
                            fromToken(ElevatedButtonTokens.SelectedContainerColor),
                        checkedContentColor =
                            fromToken(ElevatedButtonTokens.SelectedPressedLabelTextColor),
                    )
                    .also { defaultElevatedToggleButtonColorsCached = it }
        }
}

/** Contains default values used by [FilledTonalToggleButton]. */
public object FilledTonalToggleButtonDefaults {
    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [FilledTonalToggleButton].
     */
    @Composable
    public fun colors(): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalToggleButtonColors

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [FilledTonalToggleButton].
     *
     * @param containerColor the container color of this [FilledTonalToggleButton] when enabled.
     * @param contentColor the content color of this [FilledTonalToggleButton] when enabled.
     * @param disabledContainerColor the container color of this [FilledTonalToggleButton] when not
     *   enabled.
     * @param disabledContentColor the content color of this [FilledTonalToggleButton] when not
     *   enabled.
     * @param checkedContainerColor the container color of this [FilledTonalToggleButton] when
     *   checked.
     * @param checkedContentColor the content color of this [FilledTonalToggleButton] when checked.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [ToggleButtonElevation] that will animate between the provided values according to
     * the Material specification for a [FilledTonalToggleButton].
     *
     * @param defaultElevation the elevation used when the [FilledTonalToggleButton] is enabled, and
     *   has no other [Interaction]s.
     * @param pressedElevation the elevation used when this [FilledTonalToggleButton] is enabled and
     *   pressed.
     * @param focusedElevation the elevation used when the [FilledTonalToggleButton] is enabled and
     *   focused.
     * @param hoveredElevation the elevation used when the [FilledTonalToggleButton] is enabled and
     *   hovered.
     * @param disabledElevation the elevation used when the [FilledTonalToggleButton] is not
     *   enabled.
     */
    @Composable
    public fun elevation(
        defaultElevation: Dp = FilledTonalButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledTonalButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledTonalButtonTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledTonalButtonTokens.HoverContainerElevation,
        disabledElevation: Dp = 0.dp,
    ): ToggleButtonElevation =
        ToggleButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    internal val ColorScheme.defaultFilledTonalToggleButtonColors: ToggleButtonColors
        get() {
            return defaultFilledTonalToggleButtonColorsCached
                ?: ToggleButtonColors(
                        containerColor = fromToken(TonalButtonTokens.UnselectedContainerColor),
                        contentColor = fromToken(TonalButtonTokens.UnselectedLabelTextColor),
                        disabledContainerColor =
                            fromToken(TonalButtonTokens.DisabledContainerColor)
                                .copy(alpha = TonalButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(TonalButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = TonalButtonTokens.DisabledLabelTextOpacity),
                        checkedContainerColor = fromToken(TonalButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(TonalButtonTokens.SelectedLabelTextColor),
                    )
                    .also { defaultFilledTonalToggleButtonColorsCached = it }
        }
}

/** Contains default values used by [OutlinedToggleButton]. */
public object OutlinedToggleButtonDefaults {
    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [OutlinedToggleButton].
     */
    @Composable
    public fun colors(): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedToggleButtonColors

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [OutlinedToggleButton].
     *
     * @param containerColor the container color of this [OutlinedToggleButton] when enabled.
     * @param contentColor the content color of this [OutlinedToggleButton] when enabled.
     * @param disabledContainerColor the container color of this [OutlinedToggleButton] when not
     *   enabled.
     * @param disabledContentColor the content color of this [OutlinedToggleButton] when not
     *   enabled.
     * @param checkedContainerColor the container color of this [OutlinedToggleButton] when checked.
     * @param checkedContentColor the content color of this [OutlinedToggleButton] when checked.
     */
    @Composable
    public fun colors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    internal val ColorScheme.defaultOutlinedToggleButtonColors: ToggleButtonColors
        get() {
            return defaultOutlinedToggleButtonColorsCached
                ?: ToggleButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedButtonTokens.UnselectedLabelTextColor),
                        disabledContainerColor =
                            fromToken(OutlinedButtonTokens.DisabledOutlineColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(OutlinedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledLabelTextOpacity),
                        checkedContainerColor =
                            fromToken(OutlinedButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(OutlinedButtonTokens.SelectedLabelTextColor),
                    )
                    .also { defaultOutlinedToggleButtonColorsCached = it }
        }

    /**
     * Resolves the default [BorderStroke] used in an [OutlinedToggleButton].
     *
     * @param enabled controls the enabled state of the button
     * @param checked controls the checked state of the button
     */
    @Composable
    public fun border(enabled: Boolean, checked: Boolean): BorderStroke? {
        return if (checked) {
            null
        } else {
            ButtonDefaults.outlinedButtonBorder(enabled)
        }
    }
}

/**
 * Represents the container and content colors used in a toggle button in different states.
 *
 * @param containerColor the container color of this [ToggleButton] when enabled.
 * @param contentColor the content color of this [ToggleButton] when enabled.
 * @param disabledContainerColor the container color of this [ToggleButton] when not enabled.
 * @param disabledContentColor the content color of this [ToggleButton] when not enabled.
 * @param checkedContainerColor the container color of this [ToggleButton] when checked.
 * @param checkedContentColor the content color of this [ToggleButton] when checked.
 * @constructor create an instance with arbitrary colors.
 * @see [ToggleButtonDefaults.colors] for the default colors used in a [ToggleButton].
 * @see [ElevatedToggleButtonDefaults.colors] for the default colors used in a
 *   [ElevatedToggleButton].
 * @see [FilledTonalToggleButtonDefaults.colors] for the default colors used in a
 *   [FilledTonalToggleButton].
 * @see [OutlinedToggleButtonDefaults.colors] for the default colors used in a
 *   [OutlinedToggleButton].
 */
@Immutable
public class ToggleButtonColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val disabledContainerColor: Color,
    public val disabledContentColor: Color,
    public val checkedContainerColor: Color,
    public val checkedContentColor: Color,
) {
    /**
     * Returns a copy of this ToggleButtonColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    public fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
    ): ToggleButtonColors =
        ToggleButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
            checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor.takeOrElse { this.checkedContentColor },
        )

    /**
     * Represents the container color for this toggle button, depending on [enabled] and [checked].
     *
     * @param enabled whether the toggle button is enabled
     * @param checked whether the toggle button is checked
     */
    @Stable
    internal fun containerColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> checkedContainerColor
            enabled && !checked -> containerColor
            else -> disabledContainerColor
        }
    }

    /**
     * Represents the content color for this toggle button, depending on [enabled] and [checked].
     *
     * @param enabled whether the toggle button is enabled
     * @param checked whether the toggle button is checked
     */
    @Stable
    internal fun contentColor(enabled: Boolean, checked: Boolean): Color {
        return when {
            enabled && checked -> checkedContentColor
            enabled && !checked -> contentColor
            else -> disabledContentColor
        }
    }

    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ToggleButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false

        return true
    }

    public override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()

        return result
    }
}

/**
 * The shapes that will be used in toggle buttons. Toggle button will morph between these three
 * shapes depending on the interaction of the toggle button, assuming all of the shapes are
 * [CornerBasedShape]s.
 *
 * @property shape is the unchecked shape.
 * @property pressedShape is the pressed shape.
 * @property checkedShape is the checked shape.
 */
@Immutable
public class ToggleButtonShapes(
    public val shape: Shape,
    public val pressedShape: Shape,
    public val checkedShape: Shape,
) {
    /**
     * Returns a copy of this [ToggleButtonShapes] with optionally overridden shapes.
     *
     * Passing `null` for any shape parameter retains the current value from this instance.
     *
     * @param shape unchecked shape, or null to keep the current unchecked shape
     * @param pressedShape pressed shape, or null to keep the current pressed shape
     * @param checkedShape checked shape, or null to keep the current checked shape
     */
    public fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
        checkedShape: Shape? = this.checkedShape,
    ): ToggleButtonShapes =
        ToggleButtonShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
            checkedShape = checkedShape.takeOrElse { this.checkedShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ToggleButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false
        if (checkedShape != other.checkedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + checkedShape.hashCode()

        return result
    }
}

/**
 * Represents the elevation used in a toggle button in different states.
 * - See [ToggleButtonDefaults.elevation] for the default elevation used in a [ToggleButton].
 * - See [ElevatedToggleButtonDefaults.elevation] for the default elevation used in an
 *   [ElevatedToggleButton].
 * - See [FilledTonalToggleButtonDefaults.elevation] for the default elevation used in a
 *   [FilledTonalToggleButton].
 */
@Stable
public class ToggleButtonElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a toggle button, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the toggle button to give it higher
     * emphasis.
     *
     * @param enabled whether the toggle button is enabled
     * @param interactionSource the [InteractionSource] for this toggle button
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource,
    ): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target =
            if (!enabled) {
                disabledElevation
            } else {
                when (interaction) {
                    is PressInteraction.Press -> pressedElevation
                    is HoverInteraction.Enter -> hoveredElevation
                    is FocusInteraction.Focus -> focusedElevation
                    else -> defaultElevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state
                    animatable.snapTo(target)
                } else {
                    val lastInteraction =
                        when (animatable.targetValue) {
                            pressedElevation -> PressInteraction.Press(Offset.Zero)
                            hoveredElevation -> HoverInteraction.Enter()
                            focusedElevation -> FocusInteraction.Focus()
                            else -> null
                        }
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target,
                    )
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ToggleButtonElevation) return false

        if (defaultElevation != other.defaultElevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultElevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

internal val ToggleButtonShapes.hasRoundedCornerShapes: Boolean
    get() =
        shape is RoundedCornerShape &&
            pressedShape is RoundedCornerShape &&
            checkedShape is RoundedCornerShape

internal val ToggleButtonShapes.hasCornerBasedShapes: Boolean
    get() =
        shape is CornerBasedShape &&
            pressedShape is CornerBasedShape &&
            checkedShape is CornerBasedShape

@Composable
private fun shapeByInteraction(
    shapes: ToggleButtonShapes,
    pressed: Boolean,
    checked: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape
        } else if (checked) {
            shapes.checkedShape
        } else {
            shapes.shape
        }

    if (shapes.hasRoundedCornerShapes)
        return key(shapes) { rememberAnimatedShape(shape as RoundedCornerShape, animationSpec) }
    else if (shapes.hasCornerBasedShapes)
        return key(shapes) { rememberAnimatedShape(shape as CornerBasedShape, animationSpec) }

    return shape
}

@Composable
private fun animateBorderStrokeAsState(border: BorderStroke?): BorderStroke? {
    val targetWidth = border?.width ?: 0.dp
    val animatedWidth by
        animateDpAsState(
            targetValue = targetWidth,
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
        )

    val targetColor = (border?.brush as? SolidColor)?.value ?: Color.Transparent

    val animatedColor by
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
        )

    if (animatedWidth <= 0.dp) {
        return null
    }

    val brush = border?.brush
    return remember(animatedWidth, animatedColor, brush) {
        if (brush is SolidColor || brush == null) {
            BorderStroke(animatedWidth, SolidColor(animatedColor))
        } else {
            BorderStroke(animatedWidth, brush)
        }
    }
}
