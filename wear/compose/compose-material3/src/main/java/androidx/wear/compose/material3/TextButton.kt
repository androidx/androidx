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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.wear.compose.material3.tokens.FilledTextButtonTokens
import androidx.wear.compose.material3.tokens.FilledTonalTextButtonTokens
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.material3.tokens.OutlinedTextButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.material3.tokens.TextButtonTokens
import androidx.wear.compose.material3.tokens.TextToggleButtonTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * Wear Material [TextButton] is a circular, text-only button with transparent background and no
 * border. It offers a single slot for text.
 *
 * Set the size of the [TextButton] with [Modifier.touchTargetAwareSize] to ensure that the
 * recommended minimum touch target size is available. The recommended [TextButton] sizes are
 * [TextButtonDefaults.DefaultButtonSize], [TextButtonDefaults.LargeButtonSize] and
 * [TextButtonDefaults.SmallButtonSize]. The recommended text styles for each corresponding button
 * size are [TextButtonDefaults.defaultButtonTextStyle], [TextButtonDefaults.largeButtonTextStyle]
 * and [TextButtonDefaults.smallButtonTextStyle].
 *
 * The default [TextButton] has no border and a transparent background for low emphasis actions. For
 * actions that require high emphasis, set [colors] to [TextButtonDefaults.filledTextButtonColors].
 * For a medium-emphasis, outlined [TextButton], set [border] to
 * [ButtonDefaults.outlinedButtonBorder]. For a middle ground between outlined and filled, set
 * [colors] to [TextButtonDefaults.filledTonalTextButtonColors].
 *
 * [TextButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * Example of a [TextButton]:
 *
 * @sample androidx.wear.compose.material3.samples.TextButtonSample
 *
 * Example of a large, filled tonal [TextButton]:
 *
 * @sample androidx.wear.compose.material3.samples.LargeFilledTonalTextButtonSample
 *
 * Example of [TextButton] with onLongClick:
 *
 * @sample androidx.wear.compose.material3.samples.TextButtonWithOnLongClickSample
 *
 * Example of an [TextButton] with shape animation of rounded corners on press:
 *
 * @sample androidx.wear.compose.material3.samples.TextButtonWithCornerAnimationSample
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param onLongClick Called when this button is long clicked (long-pressed). When this callback is
 *   set, [onLongClickLabel] should be set as well.
 * @param onLongClickLabel Semantic / accessibility label for the [onLongClick] action.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the text button's shape. It is strongly recommended to use the default as
 *   this shape is a key characteristic of the Wear Material3 Theme.
 * @param colors [TextButtonColors] that will be used to resolve the background and content color
 *   for this button in different states.
 * @param border Optional [BorderStroke] that will be used to resolve the text button border in
 *   different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the text button, expected to be text or image.
 *
 * TODO(b/261838497) Add Material3 UX guidance links
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shape: Shape = TextButtonDefaults.shape,
    colors: TextButtonColors = TextButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    RoundButton(
        onClick = onClick,
        modifier.minimumInteractiveComponentSize().size(TextButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = interactionSource,
        shape = shape,
        border = { border },
        ripple = ripple(),
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled),
                TextButtonTokens.ContentFont.value,
                content
            )
    )
}

/**
 * Wear Material [TextToggleButton] is a filled text toggle button which switches between primary
 * colors and tonal colors depending on [checked] value, and offers a single slot for text.
 *
 * Set the size of the [TextToggleButton] with Modifier.[touchTargetAwareSize] to ensure that the
 * background padding will correctly reach the edge of the minimum touch target. The recommended
 * [TextToggleButton] sizes are [TextButtonDefaults.DefaultButtonSize],
 * [TextButtonDefaults.LargeButtonSize] and [TextButtonDefaults.SmallButtonSize]. The recommended
 * text styles for each corresponding button size are [TextButtonDefaults.defaultButtonTextStyle],
 * [TextButtonDefaults.largeButtonTextStyle] and [TextButtonDefaults.smallButtonTextStyle].
 *
 * [TextToggleButton] can be enabled or disabled. A disabled button will not respond to click
 * events. When enabled, the checked and unchecked events are propagated by [onCheckedChange].
 *
 * A simple text toggle button using the default colors:
 *
 * @sample androidx.wear.compose.material3.samples.TextToggleButtonSample
 *
 * Example of a large text toggle button:
 *
 * @sample androidx.wear.compose.material3.samples.LargeTextToggleButtonSample
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`, this toggle button
 *   will not be clickable.
 * @param colors [ToggleButtonColors] that will be used to resolve the container and content color
 *   for this toggle button.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 *   appearance or preview the toggle button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 *   default as this shape is a key characteristic of the Wear Material 3 Theme.
 * @param border Optional [BorderStroke] for the [TextToggleButton].
 * @param content The text to be drawn inside the toggle button.
 */
@Composable
fun TextToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: TextToggleButtonColors = TextButtonDefaults.textToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = TextButtonDefaults.shape,
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
        border = { _, _ -> border },
        toggleButtonSize = TextButtonDefaults.DefaultButtonSize,
        interactionSource = interactionSource,
        shape = shape,
        ripple = ripple(),
        content =
            provideScopeContent(
                colors.contentColor(enabled = enabled, checked = checked),
                TextToggleButtonTokens.ContentFont.value,
                content
            )
    )
}

/** Contains the default values used by [TextButton]. */
object TextButtonDefaults {
    /** Recommended [Shape] for [TextButton]. */
    val shape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerFull

    /** Recommended pressed [Shape] for [TextButton]. */
    val pressedShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.small

    /**
     * Creates a [Shape] with a animation between two CornerBasedShapes.
     *
     * @param interactionSource the interaction source applied to the Button.
     * @param shape The normal shape of the IconButton.
     * @param pressedShape The pressed shape of the IconButton.
     */
    @Composable
    fun animatedShape(
        interactionSource: InteractionSource,
        shape: CornerBasedShape = TextButtonDefaults.shape,
        pressedShape: CornerBasedShape = TextButtonDefaults.pressedShape,
    ) = animatedPressedButtonShape(interactionSource, shape, pressedShape)

    /**
     * Creates a [TextButtonColors] with the colors for a filled [TextButton]- by default, a colored
     * background with a contrasting content color. If the text button is disabled then the colors
     * will default to [ColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    fun filledTextButtonColors() = MaterialTheme.colorScheme.defaultFilledTextButtonColors

    /**
     * Creates a [TextButtonColors] with the colors for a filled [TextButton]- by default, a colored
     * background with a contrasting content color. If the text button is disabled then the colors
     * will default to [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of [TextButton] with [filledTextButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledTextButtonSample
     * @param containerColor The background color of this text button when enabled
     * @param contentColor The content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    fun filledTextButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): TextButtonColors =
        MaterialTheme.colorScheme.defaultFilledTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [TextButtonColors] as an alternative to the [filledTonal TextButtonColors], giving
     * a surface with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * Example of creating a [TextButton] with [filledVariantTextButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledVariantTextButtonSample
     */
    @Composable
    fun filledVariantTextButtonColors() =
        MaterialTheme.colorScheme.defaultFilledVariantTextButtonColors

    /**
     * Creates a [TextButtonColors] as an alternative to the [filledTonal TextButtonColors], giving
     * a surface with more chroma to indicate selected or highlighted states that are not primary
     * calls-to-action. If the icon button is disabled then the colors will default to the
     * MaterialTheme onSurface color with suitable alpha values applied.
     *
     * Example of creating a [TextButton] with [filledVariantTextButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledVariantTextButtonSample
     * @param containerColor The background color of this text button when enabled
     * @param contentColor The content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    fun filledVariantTextButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): TextButtonColors =
        MaterialTheme.colorScheme.defaultFilledVariantTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [TextButtonColors] with the colors for a filled, tonal [TextButton]- by default, a
     * muted colored background with a contrasting content color. If the text button is disabled
     * then the colors will default to [ColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    fun filledTonalTextButtonColors() = MaterialTheme.colorScheme.defaultFilledTonalTextButtonColors

    /**
     * Creates a [TextButtonColors] with the colors for a filled, tonal [TextButton]- by default, a
     * muted colored background with a contrasting content color. If the text button is disabled
     * then the colors will default to [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of [TextButton] with [filledTonalTextButtonColors]:
     *
     * @sample androidx.wear.compose.material3.samples.FilledTonalTextButtonSample
     * @param containerColor The background color of this text button when enabled
     * @param contentColor The content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    fun filledTonalTextButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): TextButtonColors =
        MaterialTheme.colorScheme.defaultFilledTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [TextButtonColors] with the colors for an outlined [TextButton]- by default, a
     * transparent background with contrasting content color. If the button is disabled, then the
     * colors will default to [ColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    fun outlinedTextButtonColors() = MaterialTheme.colorScheme.defaultOutlinedTextButtonColors

    /**
     * Creates a [TextButtonColors] with the colors for an outlined [TextButton]- by default, a
     * transparent background with contrasting content color. If the button is disabled, then the
     * colors will default to [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * Example of [TextButton] with [outlinedTextButtonColors] and
     * [ButtonDefaults.outlinedButtonBorder]:
     *
     * @sample androidx.wear.compose.material3.samples.OutlinedTextButtonSample
     * @param contentColor The content color of this text button when enabled
     * @param disabledContentColor The content color of this text button when not enabled
     */
    @Composable
    fun outlinedTextButtonColors(
        contentColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): TextButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedTextButtonColors.copy(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContentColor
        )

    /**
     * Creates a [TextButtonColors] for a text button - by default, a transparent background with
     * contrasting content color. If the button is disabled then the colors default to
     * [ColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable fun textButtonColors() = MaterialTheme.colorScheme.defaultTextButtonColors

    /**
     * Creates a [TextButtonColors] for a text button - by default, a transparent background with
     * contrasting content color. If the button is disabled then the colors default to
     * [ColorScheme.onSurface] with suitable alpha values applied.
     *
     * @param containerColor the background color of this text button when enabled
     * @param contentColor the content color of this text button when enabled
     * @param disabledContainerColor the background color of this text button when not enabled
     * @param disabledContentColor the content color of this text button when not enabled
     */
    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Color.Unspecified,
    ): TextButtonColors =
        MaterialTheme.colorScheme.defaultTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [TextToggleButtonColors] for a [TextToggleButton]
     * - by default, a colored background with a contrasting content color. If the button is
     *   disabled, then the colors will have an alpha ([DisabledContainerAlpha] or
     *   [DisabledContentAlpha]) value applied.
     */
    @Composable
    fun textToggleButtonColors() = MaterialTheme.colorScheme.defaultTextToggleButtonColors

    /**
     * Creates a [TextToggleButtonColors] for a [TextToggleButton]
     * - by default, a colored background with a contrasting content color. If the button is
     *   disabled, then the colors will have an alpha ([DisabledContainerAlpha] or
     *   [DisabledContentAlpha]) value applied.
     *
     * @param checkedContainerColor the container color of this [TextToggleButton] when enabled and
     *   checked
     * @param checkedContentColor the content color of this [TextToggleButton] when enabled and
     *   checked
     * @param uncheckedContainerColor the container color of this [TextToggleButton] when enabled
     *   and unchecked
     * @param uncheckedContentColor the content color of this [TextToggleButton] when enabled and
     *   unchecked
     * @param disabledCheckedContainerColor the container color of this [TextToggleButton] when
     *   checked and not enabled
     * @param disabledCheckedContentColor the content color of this [TextToggleButton] when checked
     *   and not enabled
     * @param disabledUncheckedContainerColor the container color of this [TextToggleButton] when
     *   unchecked and not enabled
     * @param disabledUncheckedContentColor the content color of this [TextToggleButton] when
     *   unchecked and not enabled
     */
    @Composable
    fun textToggleButtonColors(
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
        uncheckedContainerColor: Color = Color.Unspecified,
        uncheckedContentColor: Color = Color.Unspecified,
        disabledCheckedContainerColor: Color = Color.Unspecified,
        disabledCheckedContentColor: Color = Color.Unspecified,
        disabledUncheckedContainerColor: Color = Color.Unspecified,
        disabledUncheckedContentColor: Color = Color.Unspecified,
    ): TextToggleButtonColors =
        MaterialTheme.colorScheme.defaultTextToggleButtonColors.copy(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
        )

    /**
     * The recommended size for a small button. It is recommended to apply this size using
     * [Modifier.touchTargetAwareSize].
     */
    val SmallButtonSize = TextButtonTokens.ContainerSmallSize

    /**
     * The default size applied for buttons. It is recommended to apply this size using
     * [Modifier.touchTargetAwareSize].
     */
    val DefaultButtonSize = TextButtonTokens.ContainerDefaultSize

    /**
     * The recommended size for a large button. It is recommended to apply this size using
     * [Modifier.touchTargetAwareSize].
     */
    val LargeButtonSize = TextButtonTokens.ContainerLargeSize

    /** The recommended text style for a small button. */
    val smallButtonTextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelMedium

    /** The default text style applied for buttons. */
    val defaultButtonTextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelMedium

    /** The recommended text style for a large button. */
    val largeButtonTextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelLarge

    private val ColorScheme.defaultFilledTextButtonColors: TextButtonColors
        get() {
            return defaultFilledTextButtonColorsCached
                ?: TextButtonColors(
                        containerColor = fromToken(FilledTextButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTextButtonTokens.ContentColor),
                        disabledContainerColor =
                            fromToken(FilledTextButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTextButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledTextButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTextButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledTextButtonColorsCached = it }
        }

    private val ColorScheme.defaultFilledVariantTextButtonColors: TextButtonColors
        get() {
            return defaultFilledVariantTextButtonColorsCached
                ?: TextButtonColors(
                        containerColor = fromToken(FilledTextButtonTokens.VariantContainerColor),
                        contentColor = fromToken(FilledTextButtonTokens.VariantContentColor),
                        disabledContainerColor =
                            fromToken(FilledTextButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTextButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledTextButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = FilledTextButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledVariantTextButtonColorsCached = it }
        }

    private val ColorScheme.defaultFilledTonalTextButtonColors: TextButtonColors
        get() {
            return defaultFilledTonalTextButtonColorsCached
                ?: TextButtonColors(
                        containerColor = fromToken(FilledTonalTextButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalTextButtonTokens.ContentColor),
                        disabledContainerColor =
                            fromToken(FilledTonalTextButtonTokens.DisabledContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        FilledTonalTextButtonTokens.DisabledContainerOpacity
                                ),
                        disabledContentColor =
                            fromToken(FilledTonalTextButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        FilledTonalTextButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultFilledTonalTextButtonColorsCached = it }
        }

    private val ColorScheme.defaultOutlinedTextButtonColors: TextButtonColors
        get() {
            return defaultOutlinedTextButtonColorsCached
                ?: TextButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedTextButtonTokens.ContentColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedTextButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = OutlinedTextButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultOutlinedTextButtonColorsCached = it }
        }

    private val ColorScheme.defaultTextButtonColors: TextButtonColors
        get() {
            return defaultTextButtonColorsCached
                ?: TextButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(TextButtonTokens.ContentColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(TextButtonTokens.DisabledContentColor)
                                .toDisabledColor(
                                    disabledAlpha = TextButtonTokens.DisabledContentOpacity
                                )
                    )
                    .also { defaultTextButtonColorsCached = it }
        }

    private val ColorScheme.defaultTextToggleButtonColors: TextToggleButtonColors
        get() {
            return defaultTextToggleButtonColorsCached
                ?: TextToggleButtonColors(
                        checkedContainerColor =
                            fromToken(TextToggleButtonTokens.CheckedContainerColor),
                        checkedContentColor = fromToken(TextToggleButtonTokens.CheckedContentColor),
                        uncheckedContainerColor =
                            fromToken(TextToggleButtonTokens.UncheckedContainerColor),
                        uncheckedContentColor =
                            fromToken(TextToggleButtonTokens.UncheckedContentColor),
                        disabledCheckedContainerColor =
                            fromToken(TextToggleButtonTokens.DisabledCheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        TextToggleButtonTokens.DisabledCheckedContainerOpacity
                                ),
                        disabledCheckedContentColor =
                            fromToken(TextToggleButtonTokens.DisabledCheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        TextToggleButtonTokens.DisabledCheckedContentOpacity
                                ),
                        disabledUncheckedContainerColor =
                            fromToken(TextToggleButtonTokens.DisabledUncheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        TextToggleButtonTokens.DisabledUncheckedContainerOpacity
                                ),
                        disabledUncheckedContentColor =
                            fromToken(TextToggleButtonTokens.DisabledUncheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        TextToggleButtonTokens.DisabledUncheckedContentOpacity
                                ),
                    )
                    .also { defaultTextToggleButtonColorsCached = it }
        }
}

/**
 * Represents the container and content colors used in a text button in different states.
 *
 * See [TextButtonDefaults.filledTextButtonColors],
 * [TextButtonDefaults.filledTonalTextButtonColors], [TextButtonDefaults.textButtonColors] and
 * [TextButtonDefaults.outlinedTextButtonColors] for [TextButtonColors] with different levels of
 * emphasis.
 *
 * @param containerColor the background color of this text button when enabled.
 * @param contentColor the content color of this text button when enabled.
 * @param disabledContainerColor the background color of this text button when not enabled.
 * @param disabledContentColor the content color of this text button when not enabled.
 */
@Immutable
class TextButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {

    internal fun copy(
        containerColor: Color,
        contentColor: Color,
        disabledContainerColor: Color,
        disabledContentColor: Color
    ) =
        TextButtonColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            disabledContainerColor =
                disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor = disabledContentColor.takeOrElse { this.disabledContentColor }
        )

    /**
     * Represents the container color for this text button, depending on [enabled].
     *
     * @param enabled whether the text button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color {
        return if (enabled) containerColor else disabledContainerColor
    }

    /**
     * Represents the content color for this text button, depending on [enabled].
     *
     * @param enabled whether the text button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color {
        return if (enabled) contentColor else disabledContentColor
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextButtonColors) return false

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
 * Represents the different container and content colors used for [TextToggleButton] in various
 * states, that are checked, unchecked, enabled and disabled.
 *
 * @param checkedContainerColor Container or background color when the toggle button is checked
 * @param checkedContentColor Color of the content (text or icon) when the toggle button is checked
 * @param uncheckedContainerColor Container or background color when the toggle button is unchecked
 * @param uncheckedContentColor Color of the content (text or icon) when the toggle button is
 *   unchecked
 * @param disabledCheckedContainerColor Container or background color when the toggle button is
 *   disabled and checked
 * @param disabledCheckedContentColor Color of content (text or icon) when the toggle button is
 *   disabled and checked
 * @param disabledUncheckedContainerColor Container or background color when the toggle button is
 *   disabled and unchecked
 * @param disabledUncheckedContentColor Color of the content (text or icon) when the toggle button
 *   is disabled and unchecked
 */
@Immutable
class TextToggleButtonColors(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
) {
    internal fun copy(
        checkedContainerColor: Color,
        checkedContentColor: Color,
        uncheckedContainerColor: Color,
        uncheckedContentColor: Color,
        disabledCheckedContainerColor: Color,
        disabledCheckedContentColor: Color,
        disabledUncheckedContainerColor: Color,
        disabledUncheckedContentColor: Color,
    ): TextToggleButtonColors =
        TextToggleButtonColors(
            checkedContainerColor = checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor = checkedContentColor.takeOrElse { this.checkedContentColor },
            uncheckedContainerColor =
                uncheckedContainerColor.takeOrElse { this.uncheckedContainerColor },
            uncheckedContentColor = uncheckedContentColor.takeOrElse { this.uncheckedContentColor },
            disabledCheckedContainerColor =
                disabledCheckedContainerColor.takeOrElse { this.disabledCheckedContainerColor },
            disabledCheckedContentColor =
                disabledCheckedContentColor.takeOrElse { this.disabledCheckedContentColor },
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor.takeOrElse { this.disabledUncheckedContainerColor },
            disabledUncheckedContentColor =
                disabledUncheckedContentColor.takeOrElse { this.disabledUncheckedContentColor },
        )

    /**
     * Determines the container color based on whether the toggle button is [enabled] and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    internal fun containerColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedContainerColor,
            uncheckedColor = uncheckedContainerColor,
            disabledCheckedColor = disabledCheckedContainerColor,
            disabledUncheckedColor = disabledUncheckedContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Determines the content color based on whether the toggle button is [enabled] and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    internal fun contentColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedContentColor,
            uncheckedColor = uncheckedContentColor,
            disabledCheckedColor = disabledCheckedContentColor,
            disabledUncheckedColor = disabledUncheckedContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as TextToggleButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        return result
    }
}

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)
