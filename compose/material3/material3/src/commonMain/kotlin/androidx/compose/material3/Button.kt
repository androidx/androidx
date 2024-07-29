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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.tokens.ButtonSmallTokens
import androidx.compose.material3.tokens.ElevatedButtonTokens
import androidx.compose.material3.tokens.FilledButtonTokens
import androidx.compose.material3.tokens.FilledTonalButtonTokens
import androidx.compose.material3.tokens.OutlinedButtonTokens
import androidx.compose.material3.tokens.TextButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/buttons/overview" class="external"
 * target="_blank">Material Design button</a>.
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Filled button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-button.png)
 *
 * Filled buttons are high-emphasis buttons. Filled buttons have the most visual impact after the
 * [FloatingActionButton], and should be used for important, final actions that complete a flow,
 * like "Save", "Join now", or "Confirm".
 *
 * @sample androidx.compose.material3.samples.ButtonSample
 * @sample androidx.compose.material3.samples.ButtonWithIconSample
 *
 * Button that uses a square shape instead of the default round shape:
 *
 * @sample androidx.compose.material3.samples.SquareButtonSample
 *
 * Button that utilizes the small design content padding:
 *
 * @sample androidx.compose.material3.samples.SmallButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [ElevatedButton] for an [FilledTonalButton] with a shadow.
 * - See [TextButton] for a low-emphasis button with no border.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.buttonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. See
 *   [ButtonElevation.shadowElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val containerColor = colors.containerColor(enabled)
    val contentColor = colors.contentColor(enabled)
    val shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = shape,
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
                Modifier.defaultMinSize(
                        minWidth = ButtonDefaults.MinWidth,
                        minHeight = ButtonDefaults.MinHeight
                    )
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

/**
 * <a href="https://m3.material.io/components/buttons/overview" class="external"
 * target="_blank">Material Design elevated button</a>.
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Elevated button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-button.png)
 *
 * Elevated buttons are high-emphasis buttons that are essentially [FilledTonalButton]s with a
 * shadow. To prevent shadow creep, only use them when absolutely necessary, such as when the button
 * requires visual separation from patterned container.
 *
 * @sample androidx.compose.material3.samples.ElevatedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.elevatedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. See [ButtonDefaults.elevatedButtonElevation].
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun ElevatedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.elevatedShape,
    colors: ButtonColors = ButtonDefaults.elevatedButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.elevatedButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/buttons/overview" class="external"
 * target="_blank">Material Design filled tonal button</a>.
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Filled tonal button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-button.png)
 *
 * Filled tonal buttons are medium-emphasis buttons that is an alternative middle ground between
 * default [Button]s (filled) and [OutlinedButton]s. They can be used in contexts where
 * lower-priority button requires slightly more emphasis than an outline would give, such as "Next"
 * in an onboarding flow. Tonal buttons use the secondary color mapping.
 *
 * @sample androidx.compose.material3.samples.FilledTonalButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.filledTonalButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/buttons/overview" class="external"
 * target="_blank">Material Design outlined button</a>.
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Outlined button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-button.png)
 *
 * Outlined buttons are medium-emphasis buttons. They contain actions that are important, but are
 * not the primary action in an app. Outlined buttons pair well with [Button]s to indicate an
 * alternative, secondary action.
 *
 * @sample androidx.compose.material3.samples.OutlinedButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 * - See [TextButton] for a low-emphasis button with no border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation]).
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.outlinedButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay.
 * @param border the border to draw around the container of this button. Pass `null` for no border.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text, icon or image.
 */
@Composable
fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/buttons/overview" class="external"
 * target="_blank">Material Design text button</a>.
 *
 * Buttons help people initiate actions, from sending an email, to sharing a document, to liking a
 * post.
 *
 * ![Text button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/text-button.png)
 *
 * Text buttons are typically used for less-pronounced actions, including those located in dialogs
 * and cards. In cards, text buttons help maintain an emphasis on card content. Text buttons are
 * used for the lowest priority actions, especially when presenting multiple options.
 *
 * @sample androidx.compose.material3.samples.TextButtonSample
 *
 * Choose the best button for an action based on the amount of emphasis it needs. The more important
 * an action is, the higher emphasis its button should be.
 * - See [Button] for a high-emphasis button without a shadow, also known as a filled button.
 * - See [ElevatedButton] for a [FilledTonalButton] with a shadow.
 * - See [FilledTonalButton] for a middle ground between [OutlinedButton] and [Button].
 * - See [OutlinedButton] for a medium-emphasis button with a border.
 *
 * The default text style for internal [Text] components will be set to [Typography.labelLarge].
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this button's container, border (when [border] is not null),
 *   and shadow (when using [elevation])
 * @param colors [ButtonColors] that will be used to resolve the colors for this button in different
 *   states. See [ButtonDefaults.textButtonColors].
 * @param elevation [ButtonElevation] used to resolve the elevation for this button in different
 *   states. This controls the size of the shadow below the button. Additionally, when the container
 *   color is [ColorScheme.surface], this controls the amount of primary color applied as an
 *   overlay. A TextButton typically has no elevation, and the default value is `null`. See
 *   [ElevatedButton] for a button with elevation.
 * @param border the border to draw around the container of this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content The content displayed on the button, expected to be text.
 */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
) =
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )

// TODO(b/201341237): Use token values for 0 elevation?
// TODO(b/201341237): Use token values for null border?
// TODO(b/201341237): Use token values for no color (transparent)?
/**
 * Contains the default values used by all 5 button types.
 *
 * Default values that apply to all buttons types are [MinWidth], [MinHeight], [IconSize], and
 * [IconSpacing].
 *
 * A default value that applies only to [Button], [ElevatedButton], [FilledTonalButton], and
 * [OutlinedButton] is [ContentPadding].
 *
 * Default values that apply only to [Button] are [buttonColors] and [buttonElevation]. Default
 * values that apply only to [ElevatedButton] are [elevatedButtonColors] and
 * [elevatedButtonElevation]. Default values that apply only to [FilledTonalButton] are
 * [filledTonalButtonColors] and [filledTonalButtonElevation]. A default value that applies only to
 * [OutlinedButton] is [outlinedButtonColors]. Default values that apply only to [TextButton] are
 * [TextButtonContentPadding] and [textButtonColors].
 */
object ButtonDefaults {

    // TODO use default content padding from tokens once available
    private val ButtonHorizontalPadding = 24.dp
    private val ButtonWithIconStartpadding = 16.dp
    private val SmallButtonStartPadding = ButtonSmallTokens.LeadingSpace
    private val SmallButtonEndPadding = ButtonSmallTokens.TrailingSpace
    private val ButtonVerticalPadding = 8.dp

    /**
     * The default content padding used by [Button], [ElevatedButton], [FilledTonalButton], and
     * [OutlinedButton] buttons.
     * - See [TextButtonContentPadding] or [TextButtonWithIconContentPadding] for content padding
     *   used by [TextButton].
     * - See [ButtonWithIconContentPadding] for content padding used by [Button] that contains
     *   [Icon].
     */
    val ContentPadding =
        PaddingValues(
            start = ButtonHorizontalPadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    /** The default content padding used by [Button] that contains an [Icon]. */
    val ButtonWithIconContentPadding =
        PaddingValues(
            start = ButtonWithIconStartpadding,
            top = ButtonVerticalPadding,
            end = ButtonHorizontalPadding,
            bottom = ButtonVerticalPadding
        )

    /** The default content padding used for small [Button] */
    @ExperimentalMaterial3ExpressiveApi
    val SmallButtonContentPadding
        get() =
            PaddingValues(
                start = SmallButtonStartPadding,
                top = ButtonVerticalPadding,
                end = SmallButtonEndPadding,
                bottom = ButtonVerticalPadding
            )

    private val TextButtonHorizontalPadding = 12.dp

    /**
     * The default content padding used by [TextButton].
     * - See [TextButtonWithIconContentPadding] for content padding used by [TextButton] that
     *   contains [Icon].
     */
    val TextButtonContentPadding =
        PaddingValues(
            start = TextButtonHorizontalPadding,
            top = ContentPadding.calculateTopPadding(),
            end = TextButtonHorizontalPadding,
            bottom = ContentPadding.calculateBottomPadding()
        )

    private val TextButtonWithIconHorizontalEndPadding = 16.dp

    /** The default content padding used by [TextButton] that contains an [Icon]. */
    val TextButtonWithIconContentPadding =
        PaddingValues(
            start = TextButtonHorizontalPadding,
            top = ContentPadding.calculateTopPadding(),
            end = TextButtonWithIconHorizontalEndPadding,
            bottom = ContentPadding.calculateBottomPadding()
        )

    /**
     * The default min width applied for all buttons. Note that you can override it by applying
     * Modifier.widthIn directly on the button composable.
     */
    val MinWidth = 58.dp

    /**
     * The default min height applied for all buttons. Note that you can override it by applying
     * Modifier.heightIn directly on the button composable.
     */
    val MinHeight = ButtonSmallTokens.ContainerHeight

    /** The default size of the icon when used inside any button. */
    val IconSize = FilledButtonTokens.IconSize

    /**
     * The default size of the spacing between an icon and a text when they used inside any button.
     */
    val IconSpacing = 8.dp

    /** Square shape for any button. */
    @ExperimentalMaterial3ExpressiveApi
    val squareShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeSquare.value

    /** Default shape for a button. */
    val shape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for an elevated button. */
    val elevatedShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for a filled tonal button. */
    val filledTonalShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for an outlined button. */
    val outlinedShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /** Default shape for a text button. */
    val textShape: Shape
        @Composable get() = ButtonSmallTokens.ContainerShapeRound.value

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button].
     */
    @Composable fun buttonColors() = MaterialTheme.colorScheme.defaultButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [Button].
     *
     * @param containerColor the container color of this [Button] when enabled.
     * @param contentColor the content color of this [Button] when enabled.
     * @param disabledContainerColor the container color of this [Button] when not enabled.
     * @param disabledContentColor the content color of this [Button] when not enabled.
     */
    @Composable
    fun buttonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultButtonColors: ButtonColors
        get() {
            return defaultButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(FilledButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = FilledButtonTokens.DisabledLabelTextOpacity)
                    )
                    .also { defaultButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [ElevatedButton].
     */
    @Composable fun elevatedButtonColors() = MaterialTheme.colorScheme.defaultElevatedButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [ElevatedButton].
     *
     * @param containerColor the container color of this [ElevatedButton] when enabled
     * @param contentColor the content color of this [ElevatedButton] when enabled
     * @param disabledContainerColor the container color of this [ElevatedButton] when not enabled
     * @param disabledContentColor the content color of this [ElevatedButton] when not enabled
     */
    @Composable
    fun elevatedButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultElevatedButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultElevatedButtonColors: ButtonColors
        get() {
            return defaultElevatedButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(ElevatedButtonTokens.ContainerColor),
                        contentColor = fromToken(ElevatedButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(ElevatedButtonTokens.DisabledContainerColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(ElevatedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = ElevatedButtonTokens.DisabledLabelTextOpacity)
                    )
                    .also { defaultElevatedButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [FilledTonalButton].
     */
    @Composable
    fun filledTonalButtonColors() = MaterialTheme.colorScheme.defaultFilledTonalButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [FilledTonalButton].
     *
     * @param containerColor the container color of this [FilledTonalButton] when enabled
     * @param contentColor the content color of this [FilledTonalButton] when enabled
     * @param disabledContainerColor the container color of this [FilledTonalButton] when not
     *   enabled
     * @param disabledContentColor the content color of this [FilledTonalButton] when not enabled
     */
    @Composable
    fun filledTonalButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultFilledTonalButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultFilledTonalButtonColors: ButtonColors
        get() {
            return defaultFilledTonalButtonColorsCached
                ?: ButtonColors(
                        containerColor = fromToken(FilledTonalButtonTokens.ContainerColor),
                        contentColor = fromToken(FilledTonalButtonTokens.LabelTextColor),
                        disabledContainerColor =
                            fromToken(FilledTonalButtonTokens.DisabledContainerColor)
                                .copy(alpha = FilledTonalButtonTokens.DisabledContainerOpacity),
                        disabledContentColor =
                            fromToken(FilledTonalButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = FilledTonalButtonTokens.DisabledLabelTextOpacity)
                    )
                    .also { defaultFilledTonalButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [OutlinedButton].
     */
    @Composable fun outlinedButtonColors() = MaterialTheme.colorScheme.defaultOutlinedButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in an
     * [OutlinedButton].
     *
     * @param containerColor the container color of this [OutlinedButton] when enabled
     * @param contentColor the content color of this [OutlinedButton] when enabled
     * @param disabledContainerColor the container color of this [OutlinedButton] when not enabled
     * @param disabledContentColor the content color of this [OutlinedButton] when not enabled
     */
    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultOutlinedButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultOutlinedButtonColors: ButtonColors
        get() {
            return defaultOutlinedButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(OutlinedButtonTokens.LabelTextColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(OutlinedButtonTokens.DisabledLabelTextColor)
                                .copy(alpha = OutlinedButtonTokens.DisabledLabelTextOpacity)
                    )
                    .also { defaultOutlinedButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [TextButton].
     */
    @Composable fun textButtonColors() = MaterialTheme.colorScheme.defaultTextButtonColors

    /**
     * Creates a [ButtonColors] that represents the default container and content colors used in a
     * [TextButton].
     *
     * @param containerColor the container color of this [TextButton] when enabled
     * @param contentColor the content color of this [TextButton] when enabled
     * @param disabledContainerColor the container color of this [TextButton] when not enabled
     * @param disabledContentColor the content color of this [TextButton] when not enabled
     */
    @Composable
    fun textButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
    ): ButtonColors =
        MaterialTheme.colorScheme.defaultTextButtonColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        )

    internal val ColorScheme.defaultTextButtonColors: ButtonColors
        get() {
            return defaultTextButtonColorsCached
                ?: ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = fromToken(TextButtonTokens.LabelColor),
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor =
                            fromToken(TextButtonTokens.DisabledLabelColor)
                                .copy(alpha = TextButtonTokens.DisabledLabelOpacity)
                    )
                    .also { defaultTextButtonColorsCached = it }
        }

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [Button].
     *
     * @param defaultElevation the elevation used when the [Button] is enabled, and has no other
     *   [Interaction]s.
     * @param pressedElevation the elevation used when this [Button] is enabled and pressed.
     * @param focusedElevation the elevation used when the [Button] is enabled and focused.
     * @param hoveredElevation the elevation used when the [Button] is enabled and hovered.
     * @param disabledElevation the elevation used when the [Button] is not enabled.
     */
    @Composable
    fun buttonElevation(
        defaultElevation: Dp = FilledButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledButtonTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledButtonTokens.HoverContainerElevation,
        disabledElevation: Dp = FilledButtonTokens.DisabledContainerElevation,
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [ElevatedButton].
     *
     * @param defaultElevation the elevation used when the [ElevatedButton] is enabled, and has no
     *   other [Interaction]s.
     * @param pressedElevation the elevation used when this [ElevatedButton] is enabled and pressed.
     * @param focusedElevation the elevation used when the [ElevatedButton] is enabled and focused.
     * @param hoveredElevation the elevation used when the [ElevatedButton] is enabled and hovered.
     * @param disabledElevation the elevation used when the [ElevatedButton] is not enabled.
     */
    @Composable
    fun elevatedButtonElevation(
        defaultElevation: Dp = ElevatedButtonTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedButtonTokens.FocusedContainerElevation,
        hoveredElevation: Dp = ElevatedButtonTokens.HoveredContainerElevation,
        disabledElevation: Dp = ElevatedButtonTokens.DisabledContainerElevation
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation
        )

    /**
     * Creates a [ButtonElevation] that will animate between the provided values according to the
     * Material specification for a [FilledTonalButton].
     *
     * @param defaultElevation the elevation used when the [FilledTonalButton] is enabled, and has
     *   no other [Interaction]s.
     * @param pressedElevation the elevation used when this [FilledTonalButton] is enabled and
     *   pressed.
     * @param focusedElevation the elevation used when the [FilledTonalButton] is enabled and
     *   focused.
     * @param hoveredElevation the elevation used when the [FilledTonalButton] is enabled and
     *   hovered.
     * @param disabledElevation the elevation used when the [FilledTonalButton] is not enabled.
     */
    @Composable
    fun filledTonalButtonElevation(
        defaultElevation: Dp = FilledTonalButtonTokens.ContainerElevation,
        pressedElevation: Dp = FilledTonalButtonTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledTonalButtonTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledTonalButtonTokens.HoverContainerElevation,
        disabledElevation: Dp = 0.dp
    ): ButtonElevation =
        ButtonElevation(
            defaultElevation = defaultElevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            disabledElevation = disabledElevation
        )

    /** The default [BorderStroke] used by [OutlinedButton]. */
    val outlinedButtonBorder: BorderStroke
        @Composable
        @Deprecated(
            message =
                "Please use the version that takes an `enabled` param to get the " +
                    "`BorderStroke` with the correct opacity",
            replaceWith = ReplaceWith("outlinedButtonBorder(enabled)")
        )
        get() =
            BorderStroke(
                width = ButtonSmallTokens.OutlinedOutlineWidth,
                color = OutlinedButtonTokens.OutlineColor.value,
            )

    /**
     * The default [BorderStroke] used by [OutlinedButton].
     *
     * @param enabled whether the button is enabled
     */
    @Composable
    fun outlinedButtonBorder(enabled: Boolean = true): BorderStroke =
        BorderStroke(
            width = ButtonSmallTokens.OutlinedOutlineWidth,
            color =
                if (enabled) {
                    OutlinedButtonTokens.OutlineColor.value
                } else {
                    OutlinedButtonTokens.OutlineColor.value.copy(
                        alpha = OutlinedButtonTokens.DisabledContainerOpacity
                    )
                }
        )
}

/**
 * Represents the elevation for a button in different states.
 * - See [ButtonDefaults.buttonElevation] for the default elevation used in a [Button].
 * - See [ButtonDefaults.elevatedButtonElevation] for the default elevation used in a
 *   [ElevatedButton].
 */
@Stable
class ButtonElevation
internal constructor(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a button, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the button to give it higher emphasis.
     *
     * @param enabled whether the button is enabled
     * @param interactionSource the [InteractionSource] for this button
     */
    @Composable
    internal fun shadowElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
    ): State<Dp> {
        return animateElevation(enabled = enabled, interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(
        enabled: Boolean,
        interactionSource: InteractionSource
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
                        target = target
                    )
                }
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonElevation) return false

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

/**
 * Represents the container and content colors used in a button in different states.
 *
 * @param containerColor the container color of this [Button] when enabled.
 * @param contentColor the content color of this [Button] when enabled.
 * @param disabledContainerColor the container color of this [Button] when not enabled.
 * @param disabledContentColor the content color of this [Button] when not enabled.
 *     @constructor create an instance with arbitrary colors.
 * - See [ButtonDefaults.buttonColors] for the default colors used in a [Button].
 * - See [ButtonDefaults.elevatedButtonColors] for the default colors used in a [ElevatedButton].
 * - See [ButtonDefaults.textButtonColors] for the default colors used in a [TextButton].
 */
@Immutable
class ButtonColors
constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    /**
     * Returns a copy of this ButtonColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor
    ) =
        ButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
        )

    /**
     * Represents the container color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the content color for this button, depending on [enabled].
     *
     * @param enabled whether the button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color =
        if (enabled) contentColor else disabledContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ButtonColors) return false

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
