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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
public fun TextButton(
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
            pressedShape = shapes.pressedShape,
            onPressAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>().faster(200f),
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
public object TextButtonDefaults {
    /** Recommended [Shape] for [TextButton]. */
    public val shape: RoundedCornerShape
        @Composable get() = ShapeTokens.CornerFull

    /** Recommended pressed [Shape] for [TextButton]. */
    public val pressedShape: CornerBasedShape
        @Composable get() = MaterialTheme.shapes.small

    /** Creates a [TextButtonShapes] with a static [shape]. */
    @Composable public fun shapes(): TextButtonShapes = MaterialTheme.shapes.defaultShapes

    /**
     * Creates a [TextButtonShapes] with a static [shape].
     *
     * @param shape The normal shape of the TextButton.
     */
    @Composable
    public fun shapes(
        shape: Shape? = null,
    ): TextButtonShapes = MaterialTheme.shapes.defaultShapes.copy(shape = shape)

    /**
     * Creates a [Shape] with a animation between two CornerBasedShapes.
     *
     * A simple text button using the default colors, animated when pressed.
     *
     * @sample androidx.wear.compose.material3.samples.TextButtonWithCornerAnimationSample
     */
    @Composable
    public fun animatedShapes(): TextButtonShapes = MaterialTheme.shapes.defaultAnimatedShapes

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
    public fun animatedShapes(
        shape: CornerBasedShape? = null,
        pressedShape: CornerBasedShape? = null,
    ): TextButtonShapes =
        MaterialTheme.shapes.defaultAnimatedShapes.copy(shape = shape, pressedShape = pressedShape)

    /**
     * Creates a [TextButtonColors] with the colors for a filled [TextButton]- by default, a colored
     * background with a contrasting content color. If the text button is disabled then the colors
     * will default to [ColorScheme.onSurface] with suitable alpha values applied.
     */
    @Composable
    public fun filledTextButtonColors(): TextButtonColors =
        MaterialTheme.colorScheme.defaultFilledTextButtonColors

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
    public fun filledTextButtonColors(
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
    public fun filledVariantTextButtonColors(): TextButtonColors =
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
    public fun filledVariantTextButtonColors(
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
    public fun filledTonalTextButtonColors(): TextButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalTextButtonColors

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
    public fun filledTonalTextButtonColors(
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
    public fun outlinedTextButtonColors(): TextButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedTextButtonColors

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
    public fun outlinedTextButtonColors(
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
    @Composable
    public fun textButtonColors(): TextButtonColors =
        MaterialTheme.colorScheme.defaultTextButtonColors

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
    public fun textButtonColors(
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
    public val SmallButtonSize: Dp = TextButtonTokens.ContainerSmallSize

    /**
     * The default size applied for buttons. It is recommended to apply this size using
     * [Modifier.touchTargetAwareSize].
     */
    public val DefaultButtonSize: Dp = TextButtonTokens.ContainerDefaultSize

    /**
     * The recommended size for a large button. It is recommended to apply this size using
     * [Modifier.touchTargetAwareSize].
     */
    public val LargeButtonSize: Dp = TextButtonTokens.ContainerLargeSize

    /** The recommended text style for a small button. */
    public val smallButtonTextStyle: TextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelMedium

    /** The default text style applied for buttons. */
    public val defaultButtonTextStyle: TextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelMedium

    /** The recommended text style for a large button. */
    public val largeButtonTextStyle: TextStyle
        @ReadOnlyComposable @Composable get() = MaterialTheme.typography.labelLarge

    internal val Shapes.defaultShapes: TextButtonShapes
        @Composable
        get() {
            return defaultTextButtonShapesCached
                ?: TextButtonShapes(shape = TextButtonDefaults.shape).also {
                    defaultTextButtonShapesCached = it
                }
        }

    internal val Shapes.defaultAnimatedShapes: TextButtonShapes
        @Composable
        get() {
            return defaultTextButtonAnimatedShapesCached
                ?: TextButtonShapes(
                        shape = TextButtonDefaults.shape,
                        pressedShape = TextButtonDefaults.pressedShape
                    )
                    .also { defaultTextButtonAnimatedShapesCached = it }
        }

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
public class TextButtonColors(
    public val containerColor: Color,
    public val contentColor: Color,
    public val disabledContainerColor: Color,
    public val disabledContentColor: Color,
) {

    /**
     * Returns a copy of this TextButtonColors optionally overriding some of the values.
     *
     * @param containerColor the background color of this text button when enabled.
     * @param contentColor the content color of this text button when enabled.
     * @param disabledContainerColor the background color of this text button when not enabled.
     * @param disabledContentColor the content color of this text button when not enabled.
     */
    public fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor
    ): TextButtonColors =
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
 * If [pressedShape] is non null the shape will be animated on press.
 *
 * @param shape the shape of the text button when enabled
 * @param pressedShape the shape of the text button when pressed
 */
public class TextButtonShapes(
    public val shape: Shape,
    public val pressedShape: Shape? = null,
) {
    public fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
    ): TextButtonShapes =
        TextButtonShapes(
            shape = shape ?: this.shape,
            pressedShape = pressedShape ?: this.pressedShape
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TextButtonShapes) return false

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
