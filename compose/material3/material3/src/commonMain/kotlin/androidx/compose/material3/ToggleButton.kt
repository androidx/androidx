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
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.ButtonLargeTokens
import androidx.compose.material3.tokens.ButtonMediumTokens
import androidx.compose.material3.tokens.ButtonSmallTokens
import androidx.compose.material3.tokens.ButtonXLargeTokens
import androidx.compose.material3.tokens.ButtonXSmallTokens
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TonalButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
 *   button in different states. See [ToggleButtonDefaults.toggleButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. See
 *   [ButtonElevation.shadowElevation]. Additionally, when the container color is
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
 *
 * For a [ToggleButton] that goes from a square shape to round shape:
 *
 * @sample androidx.compose.material3.samples.SquareToRoundToggleButtonSample
 * @see [Button] for a static button that doesn't need to be toggled.
 * @see [IconToggleButton] for a toggleable button where the content is specifically an [Icon].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
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
    val animatedBorder = animateBorderStrokeAsState(checked, border)

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
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = MaterialTheme.typography.labelLarge,
        ) {
            Row(
                Modifier.defaultMinSize(minHeight = ToggleButtonDefaults.MinHeight)
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
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
 *   button in different states. See [ToggleButtonDefaults.elevatedToggleButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ElevatedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.elevatedToggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

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
 *   button in different states. See [ToggleButtonDefaults.filledTonalToggleButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
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
@Deprecated(
    message = "Use FilledTonalToggleButton instead.",
    replaceWith =
        ReplaceWith(
            "FilledTonalToggleButton(checked, onCheckedChange, modifier, enabled, shapes, " +
                "colors, elevation, border, contentPadding, interactionSource, content)",
            "androidx.compose.material3.FilledTonalToggleButton",
        ),
    level = DeprecationLevel.WARNING,
)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun TonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.filledTonalToggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    FilledTonalToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

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
 *   button in different states. See [ToggleButtonDefaults.filledTonalToggleButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FilledTonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.filledTonalToggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

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
 *   button in different states. See [ToggleButtonDefaults.outlinedToggleButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this toggle button.
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
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OutlinedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ToggleButtonShapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight),
    colors: ToggleButtonColors = ToggleButtonDefaults.outlinedToggleButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) =
    ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )

/** Contains the default values for all five toggle button types. */
object ToggleButtonDefaults {
    /**
     * The default min height applied for all toggle buttons. Note that you can override it by
     * applying Modifier.heightIn directly on the toggle button composable.
     */
    val MinHeight = ButtonSmallTokens.ContainerHeight

    private val ToggleButtonStartPadding = ButtonSmallTokens.LeadingSpace
    private val ToggleButtonEndPadding = ButtonSmallTokens.TrailingSpace
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default size of the spacing between an icon and a text when they used inside any toggle
     * button.
     */
    val IconSpacing = ButtonSmallTokens.IconLabelSpace

    /**
     * The default size of the spacing between an icon and a text when they used inside any toggle
     * button.
     */
    val IconSize = ButtonSmallTokens.IconSize

    /** The default content padding used by all toggle buttons. */
    val ContentPadding =
        PaddingValues(
            start = ToggleButtonStartPadding,
            top = ButtonVerticalPadding,
            end = ToggleButtonEndPadding,
            bottom = ButtonVerticalPadding,
        )

    /**
     * Creates a [ToggleButtonShapes] that represents the default shape, pressedShape, and
     * checkedShape used in a [ToggleButton].
     */
    @Deprecated(
        "Maintained for binary compatibility.",
        replaceWith = ReplaceWith("ToggleButtonShapes()"),
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun shapes() = MaterialTheme.shapes.defaultToggleButtonShapes

    /**
     * Creates a [ToggleButtonShapes] that represents the default shape, pressedShape, and
     * checkedShape used in a [ToggleButton] and its variants.
     *
     * @param shape the unchecked shape for [ToggleButtonShapes]
     * @param pressedShape the unchecked shape for [ToggleButtonShapes]
     * @param checkedShape the unchecked shape for [ToggleButtonShapes]
     */
    @Deprecated(
        "Maintained for binary compatibility.",
        replaceWith = ReplaceWith("ToggleButtonShapes(shape, pressedShape, checkedShape)"),
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun shapes(
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

    /** A round shape that can be used for all [ToggleButton]s and its variants */
    val roundShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** A square shape that can be used for all [ToggleButton]s and its variants */
    val squareShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeSquare.value

    /** The default unchecked shape for [ToggleButton] */
    val shape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** The default pressed shape for [ToggleButton] */
    val pressedShape: Shape
        @Composable get() = RoundedCornerShape(6.dp)

    /** The default checked shape for [ToggleButton] */
    val checkedShape: Shape
        @Composable get() = ButtonSmallTokens.SelectedContainerShapeSquare.value

    /** The default square shape for a extra small toggle button */
    val extraSmallSquareShape: Shape
        @Composable get() = ButtonXSmallTokens.ContainerShapeSquare.value

    /** The default square shape for a medium toggle button */
    val mediumSquareShape: Shape
        @Composable get() = ButtonMediumTokens.ContainerShapeSquare.value

    /** The default square shape for a large toggle button */
    val largeSquareShape: Shape
        @Composable get() = ButtonLargeTokens.ContainerShapeSquare.value

    /** The default square shape for a extra large toggle button */
    val extraLargeSquareShape: Shape
        @Composable get() = ButtonXLargeTokens.ContainerShapeSquare.value

    /** The default pressed shape for a extra small toggle button */
    val extraSmallPressedShape: Shape
        @Composable get() = ButtonXSmallTokens.PressedContainerShape.value

    /** The default pressed shape for a medium toggle button */
    val mediumPressedShape: Shape
        @Composable get() = ButtonMediumTokens.PressedContainerShape.value

    /** The default pressed shape for a large toggle button */
    val largePressedShape: Shape
        @Composable get() = ButtonLargeTokens.PressedContainerShape.value

    /** The default pressed shape for a extra large toggle button */
    val extraLargePressedShape: Shape
        @Composable get() = ButtonXLargeTokens.PressedContainerShape.value

    /** The default checked square shape for a extra small toggle button */
    val extraSmallCheckedSquareShape: Shape
        @Composable get() = ButtonXSmallTokens.ContainerShapeSquare.value

    /** The default checked square shape for a medium toggle button */
    val mediumCheckedSquareShape: Shape
        @Composable get() = ButtonMediumTokens.ContainerShapeSquare.value

    /** The default checked square shape for a large toggle button */
    val largeCheckedSquareShape: Shape
        @Composable get() = ButtonLargeTokens.ContainerShapeSquare.value

    /** The default checked square shape for a extra large toggle button */
    val extraLargeCheckedSquareShape: Shape
        @Composable get() = ButtonXLargeTokens.ContainerShapeSquare.value

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ToggleButton].
     */
    @Composable fun toggleButtonColors() = MaterialTheme.colorScheme.defaultToggleButtonColors

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
    fun toggleButtonColors(
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

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [ElevatedToggleButton].
     */
    @Composable
    fun elevatedToggleButtonColors() = MaterialTheme.colorScheme.defaultElevatedToggleButtonColors

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
    fun elevatedToggleButtonColors(
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

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [TonalToggleButton].
     */
    @Deprecated(
        message = "Maintained for binary compatibility.",
        replaceWith = ReplaceWith("filledTonalToggleButtonColors()"),
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun tonalToggleButtonColors() = filledTonalToggleButtonColors()

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [FilledTonalToggleButton].
     */
    @Composable
    fun filledTonalToggleButtonColors() =
        MaterialTheme.colorScheme.defaultFilledTonalToggleButtonColors

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [TonalToggleButton].
     *
     * @param containerColor the container color of this [TonalToggleButton] when enabled.
     * @param contentColor the content color of this [TonalToggleButton] when enabled.
     * @param disabledContainerColor the container color of this [TonalToggleButton] when not
     *   enabled.
     * @param disabledContentColor the content color of this [TonalToggleButton] when not enabled.
     * @param checkedContainerColor the container color of this [TonalToggleButton] when checked.
     * @param checkedContentColor the content color of this [TonalToggleButton] when checked.
     */
    @Deprecated(
        message = "Maintained for binary compatibility.",
        replaceWith =
            ReplaceWith(
                "filledTonalToggleButtonColors(containerColor, contentColor, " +
                    "disabledContainerColor, disabledContentColor, checkedContainerColor, " +
                    "checkedContentColor)"
            ),
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun tonalToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
    ): ToggleButtonColors =
        filledTonalToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

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
    fun filledTonalToggleButtonColors(
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

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [OutlinedToggleButton].
     */
    @Composable
    fun outlinedToggleButtonColors() = MaterialTheme.colorScheme.defaultOutlinedToggleButtonColors

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
    fun outlinedToggleButtonColors(
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
     * Resolves the recommended [ToggleButtonShapes] for a given toggle button height.
     *
     * The input height is categorized into a shape bucket (such as extra small, small, medium,
     * large, or extra large) based on the closest standard button height.
     *
     * @param buttonHeight height of the button used to resolve the shape bucket
     */
    @Composable
    fun shapesFor(buttonHeight: Dp): ToggleButtonShapes {
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
                    checkedShape = extraSmallCheckedSquareShape,
                )
            buttonHeight <= (smallHeight + mediumHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes
            buttonHeight <= (mediumHeight + largeHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = mediumPressedShape,
                    checkedShape = mediumCheckedSquareShape,
                )
            buttonHeight <= (largeHeight + xLargeHeight) / 2 ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = largePressedShape,
                    checkedShape = largeCheckedSquareShape,
                )
            else ->
                MaterialTheme.shapes.defaultToggleButtonShapes.copy(
                    shape = shape,
                    pressedShape = extraLargePressedShape,
                    checkedShape = extraLargeCheckedSquareShape,
                )
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
 * @see [ToggleButtonDefaults.toggleButtonColors] for the default colors used in a [ToggleButton].
 * @see [ToggleButtonDefaults.elevatedToggleButtonColors] for the default colors used in a
 *   [ElevatedToggleButton].
 * @see [ToggleButtonDefaults.filledTonalToggleButtonColors] for the default colors used in a
 *   [FilledTonalToggleButton].
 * @see [ToggleButtonDefaults.outlinedToggleButtonColors] for the default colors used in a
 *   [OutlinedToggleButton].
 */
@Immutable
class ToggleButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
) {
    /**
     * Returns a copy of this ToggleButtonColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
    ) =
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

    override fun equals(other: Any?): Boolean {
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

    override fun hashCode(): Int {
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
class ToggleButtonShapes(val shape: Shape, val pressedShape: Shape, val checkedShape: Shape) {
    /**
     * Returns a copy of this [ToggleButtonShapes] with optionally overridden shapes.
     *
     * Passing `null` for any shape parameter retains the current value from this instance.
     *
     * @param shape unchecked shape, or null to keep the current unchecked shape
     * @param pressedShape pressed shape, or null to keep the current pressed shape
     * @param checkedShape checked shape, or null to keep the current checked shape
     */
    fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
        checkedShape: Shape? = this.checkedShape,
    ) =
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ToggleButtonShapes.hasRoundedCornerShapes: Boolean
    get() =
        shape is RoundedCornerShape &&
            pressedShape is RoundedCornerShape &&
            checkedShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val ToggleButtonShapes.hasCornerBasedShapes: Boolean
    get() =
        shape is CornerBasedShape &&
            pressedShape is CornerBasedShape &&
            checkedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
private fun animateBorderStrokeAsState(checked: Boolean, border: BorderStroke?): BorderStroke? {
    if (border == null) return null

    val targetWidth = if (checked) 0.dp else border.width
    val animatedWidth by
        animateDpAsState(
            targetValue = targetWidth,
            animationSpec = MotionSchemeKeyTokens.FastSpatial.value(),
        )

    val targetColor =
        if (checked) {
            Color.Transparent
        } else {
            (border.brush as? SolidColor)?.value ?: Color.Transparent
        }

    val animatedColor by
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
        )

    if (checked && animatedWidth <= 0.dp) {
        return null
    }

    return remember(animatedWidth, animatedColor, border.brush) {
        if (border.brush is SolidColor) {
            BorderStroke(animatedWidth, SolidColor(animatedColor))
        } else {
            BorderStroke(animatedWidth, border.brush)
        }
    }
}
