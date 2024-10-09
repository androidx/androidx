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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.wear.compose.material3.tokens.FilledTextButtonTokens
import androidx.wear.compose.material3.tokens.FilledTonalTextButtonTokens
import androidx.wear.compose.material3.tokens.OutlinedTextButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.material3.tokens.TextButtonTokens

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
 * @param shapes Defines the shape for this button. Defaults to a static shape based on
 *   [TextButtonDefaults.shape], but animated versions are available through
 *   [TextButtonDefaults.animatedShapes].
 * @param colors [TextButtonColors] that will be used to resolve the background and content color
 *   for this button in different states.
 * @param border Optional [BorderStroke] that will be used to resolve the text button border in
 *   different states. See [ButtonDefaults.outlinedButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the text button, expected to be text or image.
 */
// TODO(b/261838497) Add Material3 UX guidance links
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    enabled: Boolean = true,
    shapes: TextButtonShapes = TextButtonDefaults.shapes(),
    colors: TextButtonColors = TextButtonDefaults.textButtonColors(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val (finalShape, finalInteractionSource) =
        animateButtonShape(
            defaultShape = shapes.shape,
            pressedShape = shapes.pressed,
            onPressAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            onReleaseAnimationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
            interactionSource = interactionSource
        )

    RoundButton(
        onClick = onClick,
        modifier.minimumInteractiveComponentSize().size(TextButtonDefaults.DefaultButtonSize),
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        enabled = enabled,
        backgroundColor = { colors.containerColor(enabled = it) },
        interactionSource = finalInteractionSource,
        shape = finalShape,
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

/** Contains the default values used by [TextButton]. */
object TextButtonDefaults {
    /** Recommended [Shape] for [TextButton]. */
    val shape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerFull

    /** Recommended pressed [Shape] for [TextButton]. */
    val pressedShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.small

    /**
     * Creates a [TextButtonShapes] with a static [shape].
     *
     * @param shape The normal shape of the TextButton.
     */
    @Composable
    fun shapes(
        shape: Shape = TextButtonDefaults.shape,
    ): TextButtonShapes = TextButtonShapes(shape = shape)

    /**
     * Creates a [Shape] with a animation between two CornerBasedShapes.
     *
     * A simple text button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.TextButtonWithCornerAnimationSample
     * @param shape The normal shape of the TextButton.
     * @param pressedShape The pressed shape of the TextButton.
     */
    @Composable
    fun animatedShapes(
        shape: CornerBasedShape = TextButtonDefaults.shape,
        pressedShape: CornerBasedShape = TextButtonDefaults.pressedShape,
    ): TextButtonShapes =
        TextButtonShapes(
            shape = shape,
            pressed = pressedShape,
        )

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
     * calls-to-action. If the text button is disabled then the colors will default to the
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
     * calls-to-action. If the text button is disabled then the colors will default to the
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
 * Represents the shapes used for [TextButton] in various states.
 *
 * If [pressed] is non null the shape will be animated on press.
 *
 * @param shape the shape of the text button when enabled
 * @param pressed the shape of the text button when pressed
 */
class TextButtonShapes(
    val shape: Shape,
    val pressed: Shape? = null,
) {
    fun copy(
        default: Shape = this.shape,
        pressed: Shape? = this.pressed,
    ) = TextButtonShapes(default, pressed)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextButtonShapes) return false

        if (shape != other.shape) return false
        if (pressed != other.pressed) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressed.hashCode()

        return result
    }
}
