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

package androidx.tv.material3

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design assist chip
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI
 *
 * @param onClick called when this chip is clicked
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param onLongClick callback to be called when the surface is long clicked (long-pressed)
 * @param leadingIcon optional icon at the start of the chip, preceding the [content] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape Defines the Chip's shape
 * @param colors Color to be used on background and content of the chip
 * @param scale Defines size of the chip relative to its original size
 * @param border Defines a border around the chip
 * @param glow Shadow to be shown behind the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states
 * @param content for this chip, ideally a Text composable
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun AssistChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: ClickableChipShape = AssistChipDefaults.shape(),
    colors: ClickableChipColors = AssistChipDefaults.colors(),
    scale: ClickableChipScale = AssistChipDefaults.scale(),
    border: ClickableChipBorder = AssistChipDefaults.border(),
    glow: ClickableChipGlow = AssistChipDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    ClickableChip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        onLongClick = onLongClick,
        label = content,
        labelTextStyle = MaterialTheme.typography.labelLarge,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight = AssistChipDefaults.ContainerHeight,
        paddingValues = chipPadding(
            hasAvatar = false,
            hasLeadingIcon = leadingIcon != null,
            hasTrailingIcon = trailingIcon != null
        ),
        interactionSource = interactionSource
    )
}

/**
 * Material Design filter chip
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes
 *
 * Tapping on a filter chip toggles its selection state. A selection state [leadingIcon] can be
 * provided (e.g. a checkmark) to be appended at the starting edge of the chip's label
 *
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param onLongClick callback to be called when the surface is long clicked (long-pressed)
 * @param leadingIcon optional icon at the start of the chip, preceding the [content] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape Defines the Chip's shape
 * @param colors Color to be used on background and content of the chip
 * @param scale Defines size of the chip relative to its original size
 * @param border Defines a border around the chip
 * @param glow Shadow to be shown behind the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states
 * @param content for this chip, ideally a Text composable
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: SelectableChipShape = FilterChipDefaults.shape(),
    colors: SelectableChipColors = FilterChipDefaults.colors(),
    scale: SelectableChipScale = FilterChipDefaults.scale(),
    border: SelectableChipBorder = FilterChipDefaults.border(),
    glow: SelectableChipGlow = FilterChipDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        onLongClick = onLongClick,
        label = content,
        labelTextStyle = MaterialTheme.typography.labelLarge,
        leadingIcon = leadingIcon,
        avatar = null,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight = FilterChipDefaults.ContainerHeight,
        paddingValues = chipPadding(
            hasAvatar = false,
            hasLeadingIcon = leadingIcon != null,
            hasTrailingIcon = trailingIcon != null
        ),
        interactionSource = interactionSource
    )
}

/**
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts
 *
 * Input chips represent discrete pieces of information entered by a user
 *
 * An Input Chip can have a leading icon or an avatar at its start. In case both are provided, the
 * avatar will take precedence and will be displayed
 *
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param onLongClick callback to be called when the surface is long clicked (long-pressed)
 * @param leadingIcon optional icon at the start of the chip, preceding the [content] text
 * @param avatar optional avatar at the start of the chip, preceding the [content] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape Defines the Chip's shape
 * @param colors Color to be used on background and content of the chip
 * @param scale Defines size of the chip relative to its original size
 * @param border Defines a border around the chip
 * @param glow Shadow to be shown behind the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states
 * @param content for this chip, ideally a Text composable
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: SelectableChipShape = InputChipDefaults.shape(hasAvatar = avatar != null),
    colors: SelectableChipColors = InputChipDefaults.colors(),
    scale: SelectableChipScale = InputChipDefaults.scale(),
    border: SelectableChipBorder = InputChipDefaults.border(hasAvatar = avatar != null),
    glow: SelectableChipGlow = InputChipDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        onLongClick = onLongClick,
        label = content,
        labelTextStyle = MaterialTheme.typography.labelLarge,
        leadingIcon = leadingIcon,
        avatar = avatar,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight = InputChipDefaults.ContainerHeight,
        paddingValues = chipPadding(
            hasAvatar = avatar != null,
            hasLeadingIcon = leadingIcon != null,
            hasTrailingIcon = trailingIcon != null
        ),
        interactionSource = interactionSource
    )
}

/**
 * Material Design suggestion chip
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters
 *
 * @param onClick called when this chip is clicked
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services
 * @param onLongClick callback to be called when the surface is long clicked (long-pressed)
 * @param shape Defines the Chip's shape
 * @param colors Color to be used on background and content of the chip
 * @param scale Defines size of the chip relative to its original size
 * @param border Defines a border around the chip
 * @param glow Shadow to be shown behind the chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this chip. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this chip in different states
 * @param content content for this chip, ideally a Text composable
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    shape: ClickableChipShape = SuggestionChipDefaults.shape(),
    colors: ClickableChipColors = SuggestionChipDefaults.colors(),
    scale: ClickableChipScale = SuggestionChipDefaults.scale(),
    border: ClickableChipBorder = SuggestionChipDefaults.border(),
    glow: ClickableChipGlow = SuggestionChipDefaults.glow(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    ClickableChip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        onLongClick = onLongClick,
        label = content,
        labelTextStyle = MaterialTheme.typography.labelLarge,
        leadingIcon = null,
        trailingIcon = null,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight = SuggestionChipDefaults.ContainerHeight,
        paddingValues = chipPadding(
            hasAvatar = false,
            hasLeadingIcon = false,
            hasTrailingIcon = false
        ),
        interactionSource = interactionSource
    )
}

@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
private fun ClickableChip(
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    shape: ClickableChipShape,
    colors: ClickableChipColors,
    scale: ClickableChipScale,
    border: ClickableChipBorder,
    glow: ClickableChipGlow,
    minHeight: Dp,
    paddingValues: PaddingValues,
    interactionSource: MutableInteractionSource
) {
    Surface(
        modifier = modifier.semantics { role = Role.Button },
        onClick = onClick,
        onLongClick = onLongClick,
        enabled = enabled,
        shape = shape.toClickableSurfaceShape(),
        colors = colors.toClickableSurfaceColors(),
        scale = scale.toClickableSurfaceScale(),
        border = border.toClickableSurfaceBorder(),
        glow = glow.toClickableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            leadingIcon = leadingIcon,
            avatar = null,
            trailingIcon = trailingIcon,
            minHeight = minHeight,
            paddingValues = paddingValues
        )
    }
}

@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
private fun SelectableChip(
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    shape: SelectableChipShape,
    colors: SelectableChipColors,
    scale: SelectableChipScale,
    border: SelectableChipBorder,
    glow: SelectableChipGlow,
    minHeight: Dp,
    paddingValues: PaddingValues,
    interactionSource: MutableInteractionSource
) {
    Surface(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        onLongClick = onLongClick,
        shape = shape.toToggleableSurfaceShape(),
        colors = colors.toToggleableSurfaceColors(),
        scale = scale.toToggleableSurfaceScale(),
        border = border.toToggleableSurfaceBorder(),
        glow = glow.toToggleableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            leadingIcon = leadingIcon,
            avatar = avatar,
            trailingIcon = trailingIcon,
            minHeight = minHeight,
            paddingValues = paddingValues
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ChipContent(
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    minHeight: Dp,
    paddingValues: PaddingValues
) {

    Row(
        Modifier
            .defaultMinSize(minHeight = minHeight)
            .padding(paddingValues),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.animation.AnimatedVisibility(visible = avatar != null) {
            Row {
                avatar?.invoke()
                Spacer(Modifier.width(HorizontalElementsPadding))
            }
        }
        androidx.compose.animation.AnimatedVisibility(visible = leadingIcon != null) {
            Row {
                leadingIcon?.invoke()
                Spacer(Modifier.width(HorizontalElementsPadding))
            }
        }
        CompositionLocalProvider(
            LocalTextStyle provides labelTextStyle,
            content = label
        )
        trailingIcon?.let { nnTrailingIcon ->
            Spacer(Modifier.width(HorizontalElementsPadding))
            nnTrailingIcon()
        }
    }
}

/**
 * Returns the [PaddingValues] for any TV chip component
 */
private fun chipPadding(
    hasAvatar: Boolean,
    hasLeadingIcon: Boolean,
    hasTrailingIcon: Boolean
): PaddingValues {
    val start = if (hasAvatar) 4.dp else if (hasLeadingIcon) 12.dp else 16.dp
    val end = if (hasTrailingIcon) 12.dp else 16.dp
    val vertical = if (hasAvatar) 4.dp else 8.dp
    return PaddingValues(
        start = start,
        end = end,
        top = vertical,
        bottom = vertical
    )
}

/**
 * Contains the default values used by [AssistChip]
 */
@ExperimentalTvMaterial3Api
object AssistChipDefaults {
    /**
     * The height applied to an assist chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val ContainerHeight = 36.dp

    /**
     * The size of an Assist chip icon
     */
    val IconSize = 18.dp

    /**
     * The default [Shape] applied to an assist chip
     */
    val ContainerShape = RoundedCornerShape(8.dp)

    private const val DisabledBackgroundColorOpacity = 0.2f
    private const val DisabledContentColorOpacity = 0.8f

    /**
     * Creates a [ClickableChipShape] that represents the default container shapes used in an
     * [AssistChip]
     *
     * @param shape the shape used when the Chip is enabled, and has no other [Interaction]s
     * @param focusedShape the shape used when the Chip is enabled and focused
     * @param pressedShape the shape used when the Chip is enabled pressed
     * @param disabledShape the shape used when the Chip is not enabled
     * @param focusedDisabledShape the shape used when the Chip is not enabled and focused
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape
    ) = ClickableChipShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = disabledShape,
        focusedDisabledShape = focusedDisabledShape
    )

    /**
     * Creates a [ClickableChipColors] that represents the default container and content colors
     * used in an [AssistChip]
     *
     * @param containerColor the container color of this Chip when enabled
     * @param contentColor the content color of this Chip when enabled
     * @param focusedContainerColor the container color of this Chip when enabled and focused
     * @param focusedContentColor the content color of this Chip when enabled and focused
     * @param pressedContainerColor the container color of this Chip when enabled and pressed
     * @param pressedContentColor the content color of this Chip when enabled and pressed
     * @param disabledContainerColor the container color of this Chip when not enabled
     * @param disabledContentColor the content color of this Chip when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        pressedContentColor: Color = MaterialTheme.colorScheme.surface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundColorOpacity
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.border.copy(
            alpha = DisabledContentColorOpacity
        ),
    ) = ClickableChipColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [ClickableChipScale] that represents the default scaleFactors used in an
     * [AssistChip].
     * scaleFactors are used to modify the size of a composable in different [Interaction]
     * states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc
     *
     * @param scale the scaleFactor to be used for this Chip when enabled
     * @param focusedScale the scaleFactor to be used for this Chip when focused
     * @param pressedScale the scaleFactor to be used for this Chip when pressed
     * @param disabledScale the scaleFactor to be used for this Chip when disabled
     * @param focusedDisabledScale the scaleFactor to be used for this Chip when disabled and
     * focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) = ClickableChipScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = disabledScale,
        focusedDisabledScale = focusedDisabledScale
    )

    /**
     * Creates a [ClickableChipBorder] that represents the default [Border]s applied on an
     * AssistChip in different [Interaction] states.
     *
     * @param border the [Border] to be used for this Chip when enabled
     * @param focusedBorder the [Border] to be used for this Chip when focused
     * @param pressedBorder the [Border] to be used for this Chip when pressed
     * @param disabledBorder the [Border] to be used for this Chip when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Chip when disabled and
     * focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border
            ),
            shape = ContainerShape
        ),
        focusedBorder: Border = Border.None,
        pressedBorder: Border = focusedBorder,
        disabledBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = ContainerShape
        ),
        focusedDisabledBorder: Border = border
    ) = ClickableChipBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = disabledBorder,
        focusedDisabledBorder = focusedDisabledBorder
    )

    /**
     * Creates a [ClickableChipGlow] that represents the default [Glow]s used in an [AssistChip]
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = ClickableChipGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}

/**
 * Contains the default values used by [FilterChip].
 */
@ExperimentalTvMaterial3Api
object FilterChipDefaults {
    /**
     * The height applied to a filter chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val ContainerHeight = 36.dp

    /**
     * The size of a Filter chip icon
     */
    val IconSize = 18.dp

    /**
     * The default [Shape] applied to a filter chip
     */
    val ContainerShape = RoundedCornerShape(8.dp)

    private const val SelectedBackgroundColorOpacity = 0.4f
    private const val DisabledBackgroundColorOpacity = 0.2f
    private const val DisabledContentColorOpacity = 0.8f

    /**
     * Creates a [SelectableChipShape] that represents the default container shapes used in a
     * [FilterChip]
     *
     * @param shape the shape used when the Chip is enabled, and has no other [Interaction]s
     * @param focusedShape the shape used when the Chip is enabled and focused
     * @param pressedShape the shape used when the Chip is enabled and pressed
     * @param selectedShape the shape used when the Chip is enabled and selected
     * @param disabledShape the shape used when the Chip is not enabled
     * @param focusedSelectedShape the shape used when the Chip is enabled, focused and selected
     * @param focusedDisabledShape the shape used when the Chip is not enabled and focused
     * @param pressedSelectedShape the shape used when the Chip is enabled, pressed and selected
     * @param selectedDisabledShape the shape used when the Chip is not enabled and selected
     * @param focusedSelectedDisabledShape the shape used when the Chip is not enabled, focused
     * and selected
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        selectedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedSelectedShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape,
        pressedSelectedShape: Shape = shape,
        selectedDisabledShape: Shape = disabledShape,
        focusedSelectedDisabledShape: Shape = disabledShape
    ) = SelectableChipShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
        selectedDisabledShape = selectedDisabledShape,
        focusedSelectedDisabledShape = focusedSelectedDisabledShape
    )

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in a [FilterChip]
     *
     * @param containerColor the container color used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param contentColor the content color used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedContainerColor the container color used when the Chip is enabled and focused
     * @param focusedContentColor the content color used when the Chip is enabled and focused
     * @param pressedContainerColor the container color used when the Chip is enabled and pressed
     * @param pressedContentColor the content color used when the Chip is enabled and pressed
     * @param selectedContainerColor the container color used when the Chip is enabled and selected
     * @param selectedContentColor the content color used when the Chip is enabled and selected
     * @param disabledContainerColor the container color used when the Chip is not enabled
     * @param disabledContentColor the content color used when the Chip is not enabled
     * @param focusedSelectedContainerColor the container color used when the Chip is enabled,
     * focused and selected
     * @param focusedSelectedContentColor the content color used when the Chip is enabled,
     * focused and selected
     * @param pressedSelectedContainerColor the container color used when the Chip is enabled,
     * pressed and selected
     * @param pressedSelectedContentColor the content color used when the Chip is enabled,
     * pressed and selected
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        pressedContentColor: Color = MaterialTheme.colorScheme.surface,
        selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = SelectedBackgroundColorOpacity
        ),
        selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundColorOpacity
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.border.copy(
            alpha = DisabledContentColorOpacity
        ),
        focusedSelectedContainerColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedSelectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        pressedSelectedContainerColor: Color = MaterialTheme.colorScheme.secondary,
        pressedSelectedContentColor: Color = MaterialTheme.colorScheme.onSecondary,
    ) = SelectableChipColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

    /**
     * Creates a [SelectableChipScale] that represents the default scaleFactors used in a
     * [FilterChip]. scaleFactors are used to modify the size of a composable in different
     * [Interaction] states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc
     *
     * @param scale the scaleFactor used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedScale the scaleFactor used when the Chip is enabled and focused
     * @param pressedScale the scaleFactor used when the Chip is enabled and pressed
     * @param selectedScale the scaleFactor used when the Chip is enabled and selected
     * @param disabledScale the scaleFactor used when the Chip is not enabled
     * @param focusedSelectedScale the scaleFactor used when the Chip is enabled, focused and
     * selected
     * @param focusedDisabledScale the scaleFactor used when the Chip is not enabled and
     * focused
     * @param pressedSelectedScale the scaleFactor used when the Chip is enabled, pressed and
     * selected
     * @param selectedDisabledScale the scaleFactor used when the Chip is not enabled and
     * selected
     * @param focusedSelectedDisabledScale the scaleFactor used when the Chip is not enabled,
     * focused and selected
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) selectedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedSelectedScale: Float = focusedScale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) pressedSelectedScale: Float = scale,
        @FloatRange(from = 0.0) selectedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) focusedSelectedDisabledScale: Float = disabledScale
    ) = SelectableChipScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
        selectedDisabledScale = selectedDisabledScale,
        focusedSelectedDisabledScale = focusedSelectedDisabledScale
    )

    /**
     * Creates a [SelectableChipBorder] that represents the default [Border]s applied on a
     * [FilterChip] in different [Interaction] states
     *
     * @param border the [Border] used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedBorder the [Border] used when the Chip is enabled and focused
     * @param pressedBorder the [Border] used when the Chip is enabled and pressed
     * @param selectedBorder the [Border] used when the Chip is enabled and selected
     * @param disabledBorder the [Border] used when the Chip is not enabled
     * @param focusedSelectedBorder the [Border] used when the Chip is enabled, focused and
     * selected
     * @param focusedDisabledBorder the [Border] used when the Chip is not enabled and focused
     * @param pressedSelectedBorder the [Border] used when the Chip is enabled, pressed and
     * selected
     * @param selectedDisabledBorder the [Border] used when the Chip is not enabled and
     * selected
     * @param focusedSelectedDisabledBorder the [Border] used when the Chip is not enabled,
     * focused and selected
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border
            ),
            shape = ContainerShape
        ),
        focusedBorder: Border = Border.None,
        pressedBorder: Border = focusedBorder,
        selectedBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary
            ),
            shape = ContainerShape
        ),
        disabledBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = ContainerShape
        ),
        focusedSelectedBorder: Border = Border(
            border = BorderStroke(
                width = 1.1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = ContainerShape
        ),
        focusedDisabledBorder: Border = border,
        pressedSelectedBorder: Border = Border.None,
        selectedDisabledBorder: Border = Border.None,
        focusedSelectedDisabledBorder: Border = border
    ) = SelectableChipBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
        selectedDisabledBorder = selectedDisabledBorder,
        focusedSelectedDisabledBorder = focusedSelectedDisabledBorder,
    )

    /**
     * Creates a [SelectableChipGlow] that represents the default [Glow]s used in a [FilterChip]
     *
     * @param glow the [Glow] used when the Chip is enabled, and has no other [Interaction]s
     * @param focusedGlow the [Glow] used when the Chip is enabled and focused
     * @param pressedGlow the [Glow] used when the Chip is enabled and pressed
     * @param selectedGlow the [Glow] used when the Chip is enabled and selected
     * @param focusedSelectedGlow the [Glow] used when the Chip is enabled, focused and selected
     * @param pressedSelectedGlow the [Glow] used when the Chip is enabled, pressed and selected
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow,
        selectedGlow: Glow = glow,
        focusedSelectedGlow: Glow = focusedGlow,
        pressedSelectedGlow: Glow = glow
    ) = SelectableChipGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )
}

/**
 * Contains the default values used by [InputChip].
 */
@ExperimentalTvMaterial3Api
object InputChipDefaults {
    /**
     * The height applied for an input chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val ContainerHeight = 36.dp

    /**
     * The size of an Input chip icon
     */
    val IconSize = 18.dp

    /**
     * The size of an Input chip avatar
     */
    val AvatarSize = 28.dp

    /**
     * The default [Shape] applied to an input chip
     */
    val ContainerShape = RoundedCornerShape(8.dp)

    /**
     * The default [Shape] applied to an input chip with avatar
     */
    val ContainerShapeWithAvatar = RoundedCornerShape(36.dp)

    private const val SelectedBackgroundColorOpacity = 0.4f
    private const val DisabledBackgroundColorOpacity = 0.2f
    private const val DisabledContentColorOpacity = 0.8f

    /**
     * Creates a [SelectableChipShape] that represents the default container shapes used in an
     * [InputChip]
     *
     * @param hasAvatar changes the default shape based on whether the avatar composable is not
     * null in the Chip
     * @param shape the shape used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedShape the shape used when the Chip is enabled and focused
     * @param pressedShape the shape used when the Chip is enabled and pressed
     * @param selectedShape the shape used when the Chip is enabled and selected
     * @param disabledShape the shape used when the Chip is not enabled
     * @param focusedSelectedShape the shape used when the Chip is enabled, focused and selected
     * @param focusedDisabledShape the shape used when the Chip is not enabled and focused
     * @param pressedSelectedShape the shape used when the Chip is enabled, pressed and selected
     * @param selectedDisabledShape the shape used when the Chip is not enabled and selected
     * @param focusedSelectedDisabledShape the shape used when the Chip is not enabled, focused
     * and selected
     */
    fun shape(
        hasAvatar: Boolean,
        shape: Shape = if (hasAvatar) ContainerShapeWithAvatar else ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        selectedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedSelectedShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape,
        pressedSelectedShape: Shape = shape,
        selectedDisabledShape: Shape = disabledShape,
        focusedSelectedDisabledShape: Shape = disabledShape
    ) = SelectableChipShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
        selectedDisabledShape = selectedDisabledShape,
        focusedSelectedDisabledShape = focusedSelectedDisabledShape
    )

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in an [InputChip]
     *
     * @param containerColor the container color used when the Chip is enabled, and has no
     * other [Interaction]s
     * @param contentColor the content color used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedContainerColor the container color used when the Chip is enabled and focused
     * @param focusedContentColor the content color used when the Chip is enabled and focused
     * @param pressedContainerColor the container color used when the Chip is enabled and pressed
     * @param pressedContentColor the content color used when the Chip is enabled and pressed
     * @param selectedContainerColor the container color used when the Chip is enabled and selected
     * @param selectedContentColor the content color used when the Chip is enabled and selected
     * @param disabledContainerColor the container color used when the Chip is not enabled
     * @param disabledContentColor the content color used when the Chip is not enabled
     * @param focusedSelectedContainerColor the container color used when the Chip is enabled,
     * focused and selected
     * @param focusedSelectedContentColor the content color used when the Chip is enabled,
     * focused and selected
     * @param pressedSelectedContainerColor the container color used when the Chip is enabled,
     * pressed and selected
     * @param pressedSelectedContentColor the content color used when the Chip is enabled, pressed
     * and selected
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        pressedContentColor: Color = MaterialTheme.colorScheme.surface,
        selectedContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(
            alpha = SelectedBackgroundColorOpacity
        ),
        selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundColorOpacity
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.border.copy(
            alpha = DisabledContentColorOpacity
        ),
        focusedSelectedContainerColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedSelectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        pressedSelectedContainerColor: Color = MaterialTheme.colorScheme.secondary,
        pressedSelectedContentColor: Color = MaterialTheme.colorScheme.onSecondary,
    ) = SelectableChipColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor,
    )

    /**
     * Creates a [SelectableChipScale] that represents the default scaleFactors used in an
     * [InputChip]. scaleFactors are used to modify the size of a composable in different
     * [Interaction] states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc
     *
     * @param scale the scaleFactor used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedScale the scaleFactor used when the Chip is enabled and focused
     * @param pressedScale the scaleFactor used when the Chip is enabled and pressed
     * @param selectedScale the scaleFactor used when the Chip is enabled and selected
     * @param disabledScale the scaleFactor used when the Chip is not enabled
     * @param focusedSelectedScale the scaleFactor used when the Chip is enabled, focused and
     * selected
     * @param focusedDisabledScale the scaleFactor used when the Chip is not enabled and
     * focused
     * @param pressedSelectedScale the scaleFactor used when the Chip is enabled, pressed and
     * selected
     * @param selectedDisabledScale the scaleFactor used when the Chip is not enabled and
     * selected
     * @param focusedSelectedDisabledScale the scaleFactor used when the Chip is not enabled,
     * focused and selected
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) selectedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedSelectedScale: Float = focusedScale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) pressedSelectedScale: Float = scale,
        @FloatRange(from = 0.0) selectedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) focusedSelectedDisabledScale: Float = disabledScale
    ) = SelectableChipScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
        selectedDisabledScale = selectedDisabledScale,
        focusedSelectedDisabledScale = focusedSelectedDisabledScale
    )

    /**
     * Creates a [SelectableChipBorder] that represents the default [Border]s applied on an
     * [InputChip] in different [Interaction] states
     *
     * @param hasAvatar changes the default border shape based on whether the avatar composable is
     * not null in the Chip
     * @param border the [Border] used when the Chip is enabled, and has no other
     * [Interaction]s
     * @param focusedBorder the [Border] used when the Chip is enabled and focused
     * @param pressedBorder the [Border] used when the Chip is enabled and pressed
     * @param selectedBorder the [Border] used when the Chip is enabled and selected
     * @param disabledBorder the [Border] used when the Chip is not enabled
     * @param focusedSelectedBorder the [Border] used when the Chip is enabled, focused and
     * selected
     * @param focusedDisabledBorder the [Border] used when the Chip is not enabled and focused
     * @param pressedSelectedBorder the [Border] used when the Chip is enabled, pressed and
     * selected
     * @param selectedDisabledBorder the [Border] used when the Chip is not enabled and
     * selected
     * @param focusedSelectedDisabledBorder the [Border] used when the Chip is not enabled,
     * focused and selected
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        hasAvatar: Boolean,
        border: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border
            ),
            shape = if (hasAvatar) ContainerShapeWithAvatar else ContainerShape
        ),
        focusedBorder: Border = Border.None,
        pressedBorder: Border = focusedBorder,
        selectedBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.secondary
            ),
            shape = if (hasAvatar) ContainerShapeWithAvatar else ContainerShape
        ),
        disabledBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = if (hasAvatar) ContainerShapeWithAvatar else ContainerShape
        ),
        focusedSelectedBorder: Border = Border(
            border = BorderStroke(
                width = 1.1.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = if (hasAvatar) ContainerShapeWithAvatar else ContainerShape
        ),
        focusedDisabledBorder: Border = border,
        pressedSelectedBorder: Border = Border.None,
        selectedDisabledBorder: Border = Border.None,
        focusedSelectedDisabledBorder: Border = border
    ) = SelectableChipBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
        selectedDisabledBorder = selectedDisabledBorder,
        focusedSelectedDisabledBorder = focusedSelectedDisabledBorder,
    )

    /**
     * Creates a [SelectableChipGlow] that represents the default [Glow]s used in an [InputChip]
     *
     * @param glow the [Glow] used when the Chip is enabled, and has no other [Interaction]s
     * @param focusedGlow the [Glow] used when the Chip is enabled and focused
     * @param pressedGlow the [Glow] used when the Chip is enabled and pressed
     * @param selectedGlow the [Glow] used when the Chip is enabled and selected
     * @param focusedSelectedGlow the [Glow] used when the Chip is enabled, focused and selected
     * @param pressedSelectedGlow the [Glow] used when the Chip is enabled, pressed and selected
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow,
        selectedGlow: Glow = glow,
        focusedSelectedGlow: Glow = focusedGlow,
        pressedSelectedGlow: Glow = glow
    ) = SelectableChipGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )
}

/**
 * Contains the default values used by [SuggestionChip].
 */
@ExperimentalTvMaterial3Api
object SuggestionChipDefaults {
    /**
     * The height applied to a suggestion chip.
     * Note that you can override it by applying Modifier.height directly on a chip.
     */
    val ContainerHeight = 36.dp

    /**
     * The default [Shape] applied to a suggestion chip
     */
    val ContainerShape = RoundedCornerShape(8.dp)

    private const val DisabledBackgroundColorOpacity = 0.2f
    private const val DisabledContentColorOpacity = 0.8f

    /**
     * Creates a [ClickableChipShape] that represents the default container shapes used in a
     * [SuggestionChip]
     *
     * @param shape the shape used when the Chip is enabled, and has no other [Interaction]s
     * @param focusedShape the shape used when the Chip is enabled and focused
     * @param pressedShape the shape used when the Chip is enabled pressed
     * @param disabledShape the shape used when the Chip is not enabled
     * @param focusedDisabledShape the shape used when the Chip is not enabled and focused
     */
    fun shape(
        shape: Shape = ContainerShape,
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape
    ) = ClickableChipShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        disabledShape = disabledShape,
        focusedDisabledShape = focusedDisabledShape
    )

    /**
     * Creates a [ClickableChipColors] that represents the default container and content colors
     * used in a [SuggestionChip]
     *
     * @param containerColor the container color of this Chip when enabled
     * @param contentColor the content color of this Chip when enabled
     * @param focusedContainerColor the container color of this Chip when enabled and focused
     * @param focusedContentColor the content color of this Chip when enabled and focused
     * @param pressedContainerColor the container color of this Chip when enabled and pressed
     * @param pressedContentColor the content color of this Chip when enabled and pressed
     * @param disabledContainerColor the container color of this Chip when not enabled
     * @param disabledContentColor the content color of this Chip when not enabled
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedContainerColor: Color = MaterialTheme.colorScheme.onSurface,
        focusedContentColor: Color = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        pressedContentColor: Color = MaterialTheme.colorScheme.surface,
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = DisabledBackgroundColorOpacity
        ),
        disabledContentColor: Color = MaterialTheme.colorScheme.border.copy(
            alpha = DisabledContentColorOpacity
        )
    ) = ClickableChipColors(
        containerColor = containerColor,
        contentColor = contentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    /**
     * Creates a [ClickableChipScale] that represents the default scaleFactors used in a
     * [SuggestionChip].
     * scaleFactors are used to modify the size of a composable in different [Interaction]
     * states e.g. 1f (original) in default state, 1.2f (scaled up) in focused state,
     * 0.8f (scaled down) in pressed state, etc
     *
     * @param scale the scaleFactor to be used for this Chip when enabled
     * @param focusedScale the scaleFactor to be used for this Chip when focused
     * @param pressedScale the scaleFactor to be used for this Chip when pressed
     * @param disabledScale the scaleFactor to be used for this Chip when disabled
     * @param focusedDisabledScale the scaleFactor to be used for this Chip when disabled and
     * focused
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.1f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale
    ) = ClickableChipScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        disabledScale = disabledScale,
        focusedDisabledScale = focusedDisabledScale
    )

    /**
     * Creates a [ClickableChipBorder] that represents the default [Border]s applied on a
     * [SuggestionChip] in different [Interaction] states
     *
     * @param border the [Border] to be used for this Chip when enabled
     * @param focusedBorder the [Border] to be used for this Chip when focused
     * @param pressedBorder the [Border] to be used for this Chip when pressed
     * @param disabledBorder the [Border] to be used for this Chip when disabled
     * @param focusedDisabledBorder the [Border] to be used for this Chip when disabled and
     * focused
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.border
            ),
            shape = ContainerShape
        ),
        focusedBorder: Border = Border.None,
        pressedBorder: Border = focusedBorder,
        disabledBorder: Border = Border(
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = ContainerShape
        ),
        focusedDisabledBorder: Border = border
    ) = ClickableChipBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        disabledBorder = disabledBorder,
        focusedDisabledBorder = focusedDisabledBorder
    )

    /**
     * Creates a [ClickableChipGlow] that represents the default [Glow]s used in a [SuggestionChip]
     *
     * @param glow the Glow behind this Button when enabled
     * @param focusedGlow the Glow behind this Button when focused
     * @param pressedGlow the Glow behind this Button when pressed
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow
    ) = ClickableChipGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow
    )
}

/**
 * The padding between the elements in the chip
 */
internal val HorizontalElementsPadding = 8.dp
