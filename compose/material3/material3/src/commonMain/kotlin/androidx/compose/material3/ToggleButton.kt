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

import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.material3.tokens.ButtonSmallTokens
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
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * TODO link to mio page when available.
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided our
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ButtonShapes] according to user interaction.
 *
 * TODO link to an image when available
 *
 * see [Button] for a static button that doesn't need to be toggled. see [IconToggleButton] for a
 * toggleable button where the content is specifically an [Icon].
 *
 * @sample androidx.compose.material3.samples.ToggleButtonSample
 * @sample androidx.compose.material3.samples.ToggleButtonWithIconSample
 *
 * For a [ToggleButton] that uses a round unchecked shape and morphs into a square checked shape:
 *
 * @sample androidx.compose.material3.samples.RoundToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ButtonShapes] that the toggle button will morph between depending on the
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
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ToggleButtonDefaults.shapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    // TODO Load the motionScheme tokens from the component tokens file
    // MotionSchemeKeyTokens.DefaultEffects is intentional here to prevent
    // any bounce in this component.
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
    val pressed by interactionSource.collectIsPressedAsState()
    val containerColor = colors.containerColor(enabled, checked)
    val contentColor = colors.contentColor(enabled, checked)
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp

    val buttonShape = shapeByInteraction(shapes, pressed, checked, defaultAnimationSpec)

    Surface(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = buttonShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = interactionSource
    ) {
        ProvideContentColorTextStyle(
            contentColor = contentColor,
            textStyle = MaterialTheme.typography.labelLarge
        ) {
            Row(
                Modifier.defaultMinSize(minHeight = ToggleButtonDefaults.MinHeight)
                    .then(
                        when (buttonShape) {
                            is ShapeWithOpticalCentering -> {
                                Modifier.opticalCentering(
                                    shape = buttonShape,
                                    basePadding = contentPadding
                                )
                            }
                            is CornerBasedShape -> {
                                Modifier.opticalCentering(
                                    shape = buttonShape,
                                    basePadding = contentPadding
                                )
                            }
                            else -> {
                                Modifier.padding(contentPadding)
                            }
                        }
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * TODO link to mio page when available.
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided our
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ButtonShapes] according to user interaction.
 *
 * TODO link to an image when available
 *
 * Elevated toggle buttons are high-emphasis Toggle buttons. To prevent shadow creep, only use them
 * when absolutely necessary, such as when the toggle button requires visual separation from
 * patterned container.
 *
 * see [ElevatedButton] for a static button that doesn't need to be toggled.
 *
 * @sample androidx.compose.material3.samples.ElevatedToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ButtonShapes] that the toggle button will morph between depending on the
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
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun ElevatedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ToggleButtonDefaults.shapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.elevatedToggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
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
        content = content
    )

/**
 * TODO link to mio page when available.
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided our
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ButtonShapes] according to user interaction.
 *
 * TODO link to an image when available
 *
 * tonal toggle buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [ToggleButton]s (filled) and [OutlinedToggleButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give. Tonal toggle
 * buttons use the secondary color mapping.
 *
 * see [FilledTonalButton] for a static button that doesn't need to be toggled. see
 * [FilledTonalIconToggleButton] for a toggleable button where the content is specifically an
 * [Icon].
 *
 * @sample androidx.compose.material3.samples.TonalToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ButtonShapes] that the toggle button will morph between depending on the
 *   user's interaction with the toggle button.
 * @param colors [ToggleButtonColors] that will be used to resolve the colors used for this toggle
 *   button in different states. See [ToggleButtonDefaults.tonalToggleButtonColors].
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
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun TonalToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ToggleButtonDefaults.shapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.tonalToggleButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
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
        content = content
    )

/**
 * TODO link to mio page when available.
 *
 * Toggle button is a toggleable button that switches between primary and tonal colors depending on
 * [checked]'s value. It also morphs between the three shapes provided in [shapes] depending on the
 * state of the interaction with the toggle button as long as the three shapes provided our
 * [CornerBasedShape]s. If a shape in [shapes] isn't a [CornerBasedShape], then toggle button will
 * toggle between the [ButtonShapes] according to user interaction.
 *
 * TODO link to an image when available
 *
 * Outlined toggle buttons are medium-emphasis buttons. They contain actions that are important, but
 * are not the primary action in an app. Outlined buttons pair well with [ToggleButton]s to indicate
 * an alternative, secondary action.
 *
 * see [OutlinedButton] for a static button that doesn't need to be toggled. see
 * [OutlinedIconToggleButton] for a toggleable button where the content is specifically an [Icon].
 *
 * @sample androidx.compose.material3.samples.OutlinedToggleButtonSample
 * @param checked whether the toggle button is toggled on or off.
 * @param onCheckedChange called when the toggle button is clicked.
 * @param modifier the [Modifier] to be applied to the toggle button.
 * @param enabled controls the enabled state of this toggle button. When `false`, this component
 *   will not respond to user input, and it will appear visually disabled and disabled to
 *   accessibility services.
 * @param shapes the [ButtonShapes] that the toggle button will morph between depending on the
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
 */
@Composable
@ExperimentalMaterial3ExpressiveApi
fun OutlinedToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shapes: ButtonShapes = ToggleButtonDefaults.shapes(),
    colors: ToggleButtonColors = ToggleButtonDefaults.outlinedToggleButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
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
        content = content
    )

/** Contains the default values for all five toggle button types. */
@ExperimentalMaterial3ExpressiveApi
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

    /** The default content padding used by all toglge buttons. */
    val ContentPadding =
        PaddingValues(
            start = ToggleButtonStartPadding,
            top = ButtonVerticalPadding,
            end = ToggleButtonEndPadding,
            bottom = ButtonVerticalPadding
        )

    /**
     * Creates a [ButtonShapes] that represents the default shape, pressedShape, and checkedShape
     * used in a [ToggleButton].
     */
    @Composable fun shapes() = MaterialTheme.shapes.defaultShapes

    /**
     * Creates a [ButtonShapes] that represents the default shape, pressedShape, and checkedShape
     * used in a [ToggleButton] and its variants.
     *
     * @param shape the unchecked shape for [ButtonShapes]
     * @param pressedShape the unchecked shape for [ButtonShapes]
     * @param checkedShape the unchecked shape for [ButtonShapes]
     */
    @Composable
    fun shapes(
        shape: Shape? = null,
        pressedShape: Shape? = null,
        checkedShape: Shape? = null
    ): ButtonShapes =
        MaterialTheme.shapes.defaultShapes.copy(
            shape = shape,
            pressedShape = pressedShape,
            checkedShape = checkedShape
        )

    internal val Shapes.defaultShapes: ButtonShapes
        @Composable
        get() {
            return defaultToggleButtonShapesCached
                ?: ButtonShapes(
                        shape = shape,
                        pressedShape = pressedShape,
                        checkedShape = checkedShape
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
        @Composable get() = ButtonSmallTokens.ContainerShapeSquare.value

    /** The default pressed shape for [ToggleButton] */
    val pressedShape: Shape
        @Composable get() = RoundedCornerShape(6.dp)

    /** The default checked shape for [ToggleButton] */
    val checkedShape: Shape
        @Composable get() = ButtonSmallTokens.SelectedContainerShapeRound.value

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
        checkedContentColor: Color = Color.Unspecified
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor
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
                            fromToken(FilledButtonTokens.SelectedPressedLabelTextColor)
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
        checkedContentColor: Color = Color.Unspecified
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultElevatedToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor
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
                            fromToken(ElevatedButtonTokens.SelectedPressedLabelTextColor)
                    )
                    .also { defaultElevatedToggleButtonColorsCached = it }
        }

    /**
     * Creates a [ToggleButtonColors] that represents the default container and content colors used
     * in a [TonalToggleButton].
     */
    @Composable
    fun tonalToggleButtonColors() = MaterialTheme.colorScheme.defaultTonalToggleButtonColors

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
    @Composable
    fun tonalToggleButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultTonalToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor
        )

    internal val ColorScheme.defaultTonalToggleButtonColors: ToggleButtonColors
        get() {
            return defaultTonalToggleButtonColorsCached
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
                        checkedContentColor = fromToken(TonalButtonTokens.SelectedLabelTextColor)
                    )
                    .also { defaultTonalToggleButtonColorsCached = it }
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
        checkedContentColor: Color = Color.Unspecified
    ): ToggleButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedToggleButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor
        )

    internal val ColorScheme.defaultOutlinedToggleButtonColors: ToggleButtonColors
        get() {
            return defaultOutlinedToggleButtonColorsCached
                ?: ToggleButtonColors(
                        containerColor =
                            fromToken(OutlinedButtonTokens.UnselectedPressedOutlineColor),
                        contentColor = fromToken(OutlinedButtonTokens.UnselectedLabelTextColor),
                        disabledContainerColor =
                            fromToken(OutlinedButtonTokens.DisabledOutlineColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(OutlinedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledLabelTextOpacity),
                        checkedContainerColor =
                            fromToken(OutlinedButtonTokens.SelectedContainerColor),
                        checkedContentColor = fromToken(OutlinedButtonTokens.SelectedLabelTextColor)
                    )
                    .also { defaultOutlinedToggleButtonColorsCached = it }
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
 * - See [ToggleButtonDefaults.toggleButtonColors] for the default colors used in a [ToggleButton].
 * - See [ToggleButtonDefaults.elevatedToggleButtonColors] for the default colors used in a
 *   [ElevatedToggleButton].
 * - See [ToggleButtonDefaults.tonalToggleButtonColors] for the default colors used in a
 *   [TonalToggleButton].
 * - See [ToggleButtonDefaults.outlinedToggleButtonColors] for the default colors used in a
 *   [OutlinedToggleButton].
 */
@Immutable
class ToggleButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val checkedContainerColor: Color,
    val checkedContentColor: Color
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
        checkedContentColor: Color = this.checkedContentColor
    ) =
        ToggleButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
            checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor.takeOrElse { this.checkedContentColor }
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
class ButtonShapes(val shape: Shape, val pressedShape: Shape, val checkedShape: Shape) {
    /** Returns a copy of this ButtonShapes, optionally overriding some of the values. */
    fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
        checkedShape: Shape? = this.checkedShape
    ) =
        ButtonShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
            checkedShape = checkedShape.takeOrElse { this.checkedShape }
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonShapes) return false

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

internal val ButtonShapes.hasRoundedCornerShapes: Boolean
    get() =
        shape is RoundedCornerShape &&
            pressedShape is RoundedCornerShape &&
            checkedShape is RoundedCornerShape

@Composable
private fun shapeByInteraction(
    shapes: ButtonShapes,
    pressed: Boolean,
    checked: Boolean,
    animationSpec: FiniteAnimationSpec<Float>
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape
        } else if (checked) {
            shapes.checkedShape
        } else shapes.shape

    if (shapes.hasRoundedCornerShapes)
        return key(shapes) {
            rememberAnimatedShape(
                shape as RoundedCornerShape,
                animationSpec,
            )
        }

    return shape
}
