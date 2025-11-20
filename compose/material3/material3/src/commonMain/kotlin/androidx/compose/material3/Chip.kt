/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SuggestionChipDefaults.defaultElevatedSuggestionChipColors
import androidx.compose.material3.internal.animateElevation
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.AssistChipTokens
import androidx.compose.material3.tokens.FilterChipTokens
import androidx.compose.material3.tokens.InputChipTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.SuggestionChipTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastSumBy

/**
 * [Material Design assist chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png)
 *
 * This assist chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedAssistChip].
 *
 * Example of a flat AssistChip:
 *
 * @sample androidx.compose.material3.samples.AssistChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [AssistChipDefaults.assistChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [AssistChipDefaults.assistChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [AssistChipDefaults.assistChipBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: BorderStroke? = AssistChipDefaults.assistChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = AssistChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        minHeight = AssistChipDefaults.Height,
        paddingValues = AssistChipPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design assist chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/assist-chip.png)
 *
 * This assist chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedAssistChip].
 *
 * Example of a flat AssistChip:
 *
 * @sample androidx.compose.material3.samples.AssistChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [AssistChipDefaults.assistChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [AssistChipDefaults.assistChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [AssistChipDefaults.assistChipBorder].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this chip. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this chip in different states.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Maintained for binary compatibility. Use version with AssistChip that take a" +
        " BorderStroke instead",
    replaceWith =
        ReplaceWith(
            "AssistChip(onClick, label, modifier, enabled,leadingIcon," +
                " trailingIcon, shape, colors, elevation, border, interactionSource"
        ),
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun AssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.assistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    border: ChipBorder? = AssistChipDefaults.assistChipBorder(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = AssistChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border?.borderStroke(enabled)?.value,
        minHeight = AssistChipDefaults.Height,
        paddingValues = AssistChipPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated assist chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-assist-chip.png)
 *
 * This assist chip is applied with an elevated style. If you want a flat style, use the
 * [AssistChip].
 *
 * Example of an elevated AssistChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.ElevatedAssistChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [AssistChipDefaults.elevatedAssistChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [AssistChipDefaults.elevatedAssistChipElevation].
 * @param border the border to draw around the container of this chip
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun ElevatedAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.elevatedAssistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.elevatedAssistChipElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = AssistChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        elevation = elevation,
        colors = colors,
        minHeight = AssistChipDefaults.Height,
        paddingValues = AssistChipPadding,
        shape = shape,
        border = border,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated assist chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Assist chips represent smart or automated actions that can span multiple apps, such as opening a
 * calendar event from the home screen. Assist chips function as though the user asked an assistant
 * to complete the action. They should appear dynamically and contextually in a UI.
 *
 * ![Assist chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-assist-chip.png)
 *
 * This assist chip is applied with an elevated style. If you want a flat style, use the
 * [AssistChip].
 *
 * Example of an elevated AssistChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.ElevatedAssistChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [AssistChipDefaults.elevatedAssistChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [AssistChipDefaults.elevatedAssistChipElevation].
 * @param border the border to draw around the container of this chip
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this chip. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this chip in different states.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Maintained for binary compatibility. Use version with ElevatedAssistChip that take a" +
        " BorderStroke instead",
    replaceWith =
        ReplaceWith(
            "ElevatedAssistChip(onClick, label, modifier, enabled," +
                "leadingIcon, trailingIcon, shape, colors, elevation, border, interactionSource"
        ),
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun ElevatedAssistChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = AssistChipDefaults.shape,
    colors: ChipColors = AssistChipDefaults.elevatedAssistChipColors(),
    elevation: ChipElevation? = AssistChipDefaults.elevatedAssistChipElevation(),
    border: ChipBorder? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = AssistChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        elevation = elevation,
        colors = colors,
        minHeight = AssistChipDefaults.Height,
        paddingValues = AssistChipPadding,
        shape = shape,
        border = border?.borderStroke(enabled)?.value,
        interactionSource = interactionSource,
    )

/**
 * [Material Design filter chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes.
 *
 * ![Filter chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filter-chip.png)
 *
 * This filter chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedFilterChip].
 *
 * Tapping on a filter chip toggles its selection state. A selection state [leadingIcon] can be
 * provided (e.g. a checkmark) to be appended at the starting edge of the chip's label.
 *
 * Example of a flat FilterChip with a leading icon:
 *
 * @sample androidx.compose.material3.samples.FilterChipSample
 *
 * Example of a FilterChip with both a leading icon and a selected icon:
 *
 * @sample androidx.compose.material3.samples.FilterChipWithLeadingIconSample
 *
 * Example of a FilterChip with both a leading icon and a trailing icon:
 *
 * @sample androidx.compose.material3.samples.FilterChipWithTrailingIconSample
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text. When
 *   [selected] is true, this icon may visually indicate that the chip is selected (for example, via
 *   a checkmark icon).
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 *   in different states. See [FilterChipDefaults.filterChipColors].
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 *   different states. This controls the size of the shadow below the chip. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [FilterChipDefaults.filterChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [FilterChipDefaults.filterChipBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.filterChipElevation(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) =
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = FilterChipTokens.LabelTextFont.value,
        leadingIcon = leadingIcon,
        avatar = null,
        trailingIcon = trailingIcon,
        elevation = elevation,
        colors = colors,
        minHeight = FilterChipDefaults.Height,
        paddingValues = FilterChipPadding,
        shape = shape,
        border = border,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated filter chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Filter chips use tags or descriptive words to filter content. They can be a good alternative to
 * toggle buttons or checkboxes.
 *
 * ![Filter chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-filter-chip.png)
 *
 * This filter chip is applied with an elevated style. If you want a flat style, use the
 * [FilterChip].
 *
 * Tapping on a filter chip toggles its selection state. A selection state [leadingIcon] can be
 * provided (e.g. a checkmark) to be appended at the starting edge of the chip's label.
 *
 * Example of an elevated FilterChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.ElevatedFilterChipSample
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text. When
 *   [selected] is true, this icon may visually indicate that the chip is selected (for example, via
 *   a checkmark icon).
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [SelectableChipColors] that will be used to resolve the colors used for this chip
 *   in different states. See [FilterChipDefaults.elevatedFilterChipColors].
 * @param elevation [SelectableChipElevation] used to resolve the elevation for this chip in
 *   different states. This controls the size of the shadow below the chip. Additionally, when the
 *   container color is [ColorScheme.surface], this controls the amount of primary color applied as
 *   an overlay. See [FilterChipDefaults.filterChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [FilterChipDefaults.filterChipBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun ElevatedFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.elevatedFilterChipColors(),
    elevation: SelectableChipElevation? = FilterChipDefaults.elevatedFilterChipElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
) =
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = FilterChipTokens.LabelTextFont.value,
        leadingIcon = leadingIcon,
        avatar = null,
        trailingIcon = trailingIcon,
        elevation = elevation,
        colors = colors,
        minHeight = FilterChipDefaults.Height,
        paddingValues = FilterChipPadding,
        shape = shape,
        border = border,
        interactionSource = interactionSource,
    )

/**
 * [Material Design input chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Input chips represent discrete pieces of information entered by a user.
 *
 * ![Input chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/input-chip.png)
 *
 * An Input Chip can have a leading icon or an avatar at its start. In case both are provided, the
 * avatar will take precedence and will be displayed.
 *
 * Example of an InputChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.InputChipSample
 *
 * Example of an InputChip with an avatar and a trailing icon:
 *
 * @sample androidx.compose.material3.samples.InputChipWithAvatarSample
 *
 * Input chips should appear in a set and can be horizontally scrollable:
 *
 * @sample androidx.compose.material3.samples.ChipGroupSingleLineSample
 *
 * Alternatively, use [androidx.compose.foundation.layout.FlowRow] to wrap chips to a new line.
 *
 * @sample androidx.compose.material3.samples.ChipGroupReflowSample
 * @param selected whether this chip is selected or not
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingIcon optional icon at the start of the chip, preceding the [label] text
 * @param avatar optional avatar at the start of the chip, preceding the [label] text
 * @param trailingIcon optional icon at the end of the chip
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [InputChipDefaults.inputChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [InputChipDefaults.inputChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [InputChipDefaults.inputChipBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun InputChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    avatar: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = InputChipDefaults.shape,
    colors: SelectableChipColors = InputChipDefaults.inputChipColors(),
    elevation: SelectableChipElevation? = InputChipDefaults.inputChipElevation(),
    border: BorderStroke? = InputChipDefaults.inputChipBorder(enabled, selected),
    interactionSource: MutableInteractionSource? = null,
) {
    // If given, place the avatar in an InputChipTokens.AvatarShape shape before passing it into the
    // Chip function.
    var shapedAvatar: @Composable (() -> Unit)? = null
    if (avatar != null) {
        val avatarOpacity = if (enabled) 1f else InputChipTokens.DisabledAvatarOpacity
        val avatarShape = InputChipTokens.AvatarShape.value
        shapedAvatar =
            @Composable {
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            this.alpha = avatarOpacity
                            this.shape = avatarShape
                            this.clip = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    avatar()
                }
            }
    }
    SelectableChip(
        selected = selected,
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = InputChipTokens.LabelTextFont.value,
        leadingIcon = leadingIcon,
        avatar = shapedAvatar,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        minHeight = InputChipDefaults.Height,
        paddingValues =
            inputChipPadding(
                hasAvatar = shapedAvatar != null,
                hasLeadingIcon = leadingIcon != null,
                hasTrailingIcon = trailingIcon != null,
            ),
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design suggestion chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/suggestion-chip.png)
 *
 * This suggestion chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedSuggestionChip].
 *
 * Example of a flat SuggestionChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.SuggestionChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [SuggestionChipDefaults.suggestionChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [SuggestionChipDefaults.suggestionChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [SuggestionChipDefaults.suggestionChipBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: BorderStroke? = SuggestionChipDefaults.suggestionChipBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = SuggestionChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = icon,
        trailingIcon = null,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        minHeight = SuggestionChipDefaults.Height,
        paddingValues = SuggestionChipPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design suggestion chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/suggestion-chip.png)
 *
 * This suggestion chip is applied with a flat style. If you want an elevated style, use the
 * [ElevatedSuggestionChip].
 *
 * Example of a flat SuggestionChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.SuggestionChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 *   different states. See [SuggestionChipDefaults.suggestionChipColors].
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [SuggestionChipDefaults.suggestionChipElevation].
 * @param border the border to draw around the container of this chip. Pass `null` for no border.
 *   See [SuggestionChipDefaults.suggestionChipBorder].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this chip. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this chip in different states.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Maintained for binary compatibility. Use version with SuggestionChip that take a" +
        " BorderStroke instead",
    replaceWith =
        ReplaceWith(
            "SuggestionChip(onClick, label, modifier, enabled, icon," +
                " shape, colors, elevation, border, interactionSource"
        ),
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun SuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.suggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.suggestionChipElevation(),
    border: ChipBorder? = SuggestionChipDefaults.suggestionChipBorder(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = SuggestionChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = icon,
        trailingIcon = null,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border?.borderStroke(enabled)?.value,
        minHeight = SuggestionChipDefaults.Height,
        paddingValues = SuggestionChipPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated suggestion chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-suggestion-chip.png)
 *
 * This suggestion chip is applied with an elevated style. If you want a flat style, use the
 * [SuggestionChip].
 *
 * Example of an elevated SuggestionChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.ElevatedSuggestionChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [Surface] and [SuggestionChipDefaults.elevatedSuggestionChipElevation].
 * @param border the border to draw around the container of this chip different states. See
 *   [SuggestionChipDefaults.elevatedSuggestionChipColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip. You can use this to change the chip's appearance or
 *   preview the chip in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun ElevatedSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.elevatedSuggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.elevatedSuggestionChipElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = SuggestionChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = icon,
        trailingIcon = null,
        elevation = elevation,
        colors = colors,
        minHeight = SuggestionChipDefaults.Height,
        paddingValues = SuggestionChipPadding,
        shape = shape,
        border = border,
        interactionSource = interactionSource,
    )

/**
 * [Material Design elevated suggestion chip](https://m3.material.io/components/chips/overview)
 *
 * Chips help people enter information, make selections, filter content, or trigger actions. Chips
 * can show multiple interactive elements together in the same area, such as a list of selectable
 * movie times, or a series of email contacts.
 *
 * Suggestion chips help narrow a user's intent by presenting dynamically generated suggestions,
 * such as possible responses or search filters.
 *
 * ![Suggestion chip
 * image](https://developer.android.com/images/reference/androidx/compose/material3/elevated-suggestion-chip.png)
 *
 * This suggestion chip is applied with an elevated style. If you want a flat style, use the
 * [SuggestionChip].
 *
 * Example of an elevated SuggestionChip with a trailing icon:
 *
 * @sample androidx.compose.material3.samples.ElevatedSuggestionChipSample
 * @param onClick called when this chip is clicked
 * @param label text label for this chip
 * @param modifier the [Modifier] to be applied to this chip
 * @param enabled controls the enabled state of this chip. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param icon optional icon at the start of the chip, preceding the [label] text
 * @param shape defines the shape of this chip's container, border (when [border] is not null), and
 *   shadow (when using [elevation])
 * @param colors [ChipColors] that will be used to resolve the colors used for this chip in
 * @param elevation [ChipElevation] used to resolve the elevation for this chip in different states.
 *   This controls the size of the shadow below the chip. Additionally, when the container color is
 *   [ColorScheme.surface], this controls the amount of primary color applied as an overlay. See
 *   [Surface] and [SuggestionChipDefaults.elevatedSuggestionChipElevation].
 * @param border the border to draw around the container of this chip different states. See
 *   [SuggestionChipDefaults.elevatedSuggestionChipColors].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 *   for this chip. You can create and pass in your own `remember`ed instance to observe
 *   [Interaction]s and customize the appearance / behavior of this chip in different states.
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Maintained for binary compatibility. Use version with ElevatedSuggestionChip that take" +
        " a BorderStroke instead",
    replaceWith =
        ReplaceWith(
            "ElevatedSuggestionChip(onClick, label, modifier, enabled," +
                " icon, shape, colors, elevation, border, interactionSource"
        ),
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun ElevatedSuggestionChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = SuggestionChipDefaults.shape,
    colors: ChipColors = SuggestionChipDefaults.elevatedSuggestionChipColors(),
    elevation: ChipElevation? = SuggestionChipDefaults.elevatedSuggestionChipElevation(),
    border: ChipBorder? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) =
    Chip(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        label = label,
        labelTextStyle = SuggestionChipTokens.LabelTextFont.value,
        labelColor = colors.labelColor(enabled),
        leadingIcon = icon,
        trailingIcon = null,
        elevation = elevation,
        colors = colors,
        minHeight = SuggestionChipDefaults.Height,
        paddingValues = SuggestionChipPadding,
        shape = shape,
        border = border?.borderStroke(enabled)?.value,
        interactionSource = interactionSource,
    )

/** Contains the baseline values used by [AssistChip]. */
object AssistChipDefaults {
    /**
     * The height applied for an assist chip. Note that you can override it by applying
     * Modifier.height directly on a chip.
     */
    val Height = AssistChipTokens.ContainerHeight

    /** The size of an assist chip icon. */
    val IconSize = AssistChipTokens.IconSize

    /**
     * Creates a [ChipColors] that represents the default container , label, and icon colors used in
     * a flat [AssistChip].
     */
    @Composable fun assistChipColors() = MaterialTheme.colorScheme.defaultAssistChipColors

    /**
     * Creates a [ChipColors] that represents the default container , label, and icon colors used in
     * a flat [AssistChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconContentColor the color of this chip's start icon when enabled
     * @param trailingIconContentColor the color of this chip's end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconContentColor the color of this chip's end icon when not enabled
     */
    @Composable
    fun assistChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        leadingIconContentColor: Color = Color.Unspecified,
        trailingIconContentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledLeadingIconContentColor: Color = Color.Unspecified,
        disabledTrailingIconContentColor: Color = Color.Unspecified,
    ): ChipColors =
        MaterialTheme.colorScheme.defaultAssistChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = leadingIconContentColor,
            trailingIconContentColor = trailingIconContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconContentColor = disabledLeadingIconContentColor,
            disabledTrailingIconContentColor = disabledTrailingIconContentColor,
        )

    internal val ColorScheme.defaultAssistChipColors: ChipColors
        get() {
            return defaultAssistChipColorsCached
                ?: ChipColors(
                        containerColor = Color.Transparent,
                        labelColor = fromToken(AssistChipTokens.LabelTextColor),
                        leadingIconContentColor = fromToken(AssistChipTokens.IconColor),
                        trailingIconContentColor = fromToken(AssistChipTokens.IconColor),
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor =
                            fromToken(AssistChipTokens.DisabledLabelTextColor)
                                .copy(alpha = AssistChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconContentColor =
                            fromToken(AssistChipTokens.DisabledIconColor)
                                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
                        disabledTrailingIconContentColor =
                            fromToken(AssistChipTokens.DisabledIconColor)
                                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
                    )
                    .also { defaultAssistChipColorsCached = it }
        }

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [AssistChip].
     *
     * @param elevation the elevation used when the [AssistChip] is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed.
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun assistChipElevation(
        elevation: Dp = AssistChipTokens.FlatContainerElevation,
        pressedElevation: Dp = elevation,
        focusedElevation: Dp = elevation,
        hoveredElevation: Dp = elevation,
        draggedElevation: Dp = AssistChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = elevation,
    ): ChipElevation =
        ChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [AssistChip].
     *
     * @param enabled whether the chip is enabled
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun assistChipBorder(
        enabled: Boolean,
        borderColor: Color = AssistChipTokens.FlatOutlineColor.value,
        disabledBorderColor: Color =
            AssistChipTokens.FlatDisabledOutlineColor.value.copy(
                alpha = AssistChipTokens.FlatDisabledOutlineOpacity
            ),
        borderWidth: Dp = AssistChipTokens.FlatOutlineWidth,
    ): BorderStroke = BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [AssistChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        "Maintained for binary compatibility. Use the assistChipBorder function that returns" +
            " BorderStroke instead",
        replaceWith =
            ReplaceWith(
                "assistChipBorder(enabled, borderColor," + " disabledBorderColor, borderWidth)"
            ),
        level = DeprecationLevel.WARNING,
    )
    @Composable
    fun assistChipBorder(
        borderColor: Color = AssistChipTokens.FlatOutlineColor.value,
        disabledBorderColor: Color =
            AssistChipTokens.FlatDisabledOutlineColor.value.copy(
                alpha = AssistChipTokens.FlatDisabledOutlineOpacity
            ),
        borderWidth: Dp = AssistChipTokens.FlatOutlineWidth,
    ): ChipBorder =
        ChipBorder(
            borderColor = borderColor,
            disabledBorderColor = disabledBorderColor,
            borderWidth = borderWidth,
        )

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [AssistChip].
     */
    @Composable
    fun elevatedAssistChipColors() = MaterialTheme.colorScheme.defaultElevatedAssistChipColors

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [AssistChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconContentColor the color of this chip's start icon when enabled
     * @param trailingIconContentColor the color of this chip's end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconContentColor the color of this chip's end icon when not enabled
     */
    @Composable
    fun elevatedAssistChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        leadingIconContentColor: Color = Color.Unspecified,
        trailingIconContentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledLeadingIconContentColor: Color = Color.Unspecified,
        disabledTrailingIconContentColor: Color = Color.Unspecified,
    ): ChipColors =
        MaterialTheme.colorScheme.defaultElevatedSuggestionChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = leadingIconContentColor,
            trailingIconContentColor = trailingIconContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconContentColor = disabledLeadingIconContentColor,
            disabledTrailingIconContentColor = disabledTrailingIconContentColor,
        )

    internal val ColorScheme.defaultElevatedAssistChipColors: ChipColors
        get() {
            return defaultElevatedAssistChipColorsCached
                ?: ChipColors(
                        containerColor = fromToken(AssistChipTokens.ElevatedContainerColor),
                        labelColor = fromToken(AssistChipTokens.LabelTextColor),
                        leadingIconContentColor = fromToken(AssistChipTokens.IconColor),
                        trailingIconContentColor = fromToken(AssistChipTokens.IconColor),
                        disabledContainerColor =
                            fromToken(AssistChipTokens.ElevatedDisabledContainerColor)
                                .copy(alpha = AssistChipTokens.ElevatedDisabledContainerOpacity),
                        disabledLabelColor =
                            fromToken(AssistChipTokens.DisabledLabelTextColor)
                                .copy(alpha = AssistChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconContentColor =
                            fromToken(AssistChipTokens.DisabledIconColor)
                                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
                        disabledTrailingIconContentColor =
                            fromToken(AssistChipTokens.DisabledIconColor)
                                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
                    )
                    .also { defaultElevatedAssistChipColorsCached = it }
        }

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [AssistChip].
     *
     * @param elevation the elevation used when the [AssistChip] is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed.
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedAssistChipElevation(
        elevation: Dp = AssistChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = AssistChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = AssistChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = AssistChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = AssistChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = AssistChipTokens.ElevatedDisabledContainerElevation,
    ): ChipElevation =
        ChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /** Default shape of an assist chip. */
    val shape: Shape
        @Composable get() = AssistChipTokens.ContainerShape.value
}

/** Contains the baseline values used by [FilterChip]. */
object FilterChipDefaults {
    /**
     * The height applied for a filter chip. Note that you can override it by applying
     * Modifier.height directly on a chip.
     */
    val Height = FilterChipTokens.ContainerHeight

    /** The size of a filter chip leading icon. */
    val IconSize = FilterChipTokens.IconSize

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in a flat [FilterChip].
     */
    @Composable fun filterChipColors() = MaterialTheme.colorScheme.defaultFilterChipColors

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in a flat [FilterChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconColor the color of this chip's start and end icons when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     *   selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun filterChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        selectedContainerColor: Color = Color.Unspecified,
        disabledSelectedContainerColor: Color = Color.Unspecified,
        selectedLabelColor: Color = Color.Unspecified,
        selectedLeadingIconColor: Color = Color.Unspecified,
        selectedTrailingIconColor: Color = Color.Unspecified,
    ): SelectableChipColors =
        MaterialTheme.colorScheme.defaultFilterChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconColor = iconColor,
            trailingIconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )

    internal val ColorScheme.defaultFilterChipColors: SelectableChipColors
        get() {
            return defaultFilterChipColorsCached
                ?: SelectableChipColors(
                        containerColor = Color.Transparent,
                        labelColor = fromToken(FilterChipTokens.UnselectedLabelTextColor),
                        leadingIconColor = fromToken(FilterChipTokens.UnselectedLeadingIconColor),
                        trailingIconColor = fromToken(FilterChipTokens.UnselectedTrailingIconColor),
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor =
                            fromToken(FilterChipTokens.DisabledLabelTextColor)
                                .copy(alpha = FilterChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(FilterChipTokens.DisabledLeadingIconColor)
                                .copy(alpha = FilterChipTokens.DisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(FilterChipTokens.DisabledTrailingIconColor)
                                .copy(alpha = FilterChipTokens.DisabledTrailingIconOpacity),
                        selectedContainerColor =
                            fromToken(FilterChipTokens.FlatSelectedContainerColor),
                        disabledSelectedContainerColor =
                            fromToken(FilterChipTokens.FlatDisabledSelectedContainerColor)
                                .copy(
                                    alpha = FilterChipTokens.FlatDisabledSelectedContainerOpacity
                                ),
                        selectedLabelColor = fromToken(FilterChipTokens.SelectedLabelTextColor),
                        selectedLeadingIconColor =
                            fromToken(FilterChipTokens.SelectedLeadingIconColor),
                        selectedTrailingIconColor =
                            fromToken(FilterChipTokens.SelectedTrailingIconColor),
                    )
                    .also { defaultFilterChipColorsCached = it }
        }

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for a flat [FilterChip].
     *
     * @param elevation the elevation used when the [FilterChip] is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun filterChipElevation(
        elevation: Dp = FilterChipTokens.FlatContainerElevation,
        pressedElevation: Dp = FilterChipTokens.FlatSelectedPressedContainerElevation,
        focusedElevation: Dp = FilterChipTokens.FlatSelectedFocusContainerElevation,
        hoveredElevation: Dp = FilterChipTokens.FlatSelectedHoverContainerElevation,
        draggedElevation: Dp = FilterChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = elevation,
    ): SelectableChipElevation =
        SelectableChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [BorderStroke] that represents the default border used in a flat [FilterChip].
     *
     * @param selected whether this chip is selected or not
     * @param enabled controls the enabled state of this chip. When `false`, this component will not
     *   respond to user input, and it will appear visually disabled and disabled to accessibility
     *   services.
     * @param borderColor the border color of this chip when enabled and not selected
     * @param selectedBorderColor the border color of this chip when enabled and selected
     * @param disabledBorderColor the border color of this chip when not enabled and not selected
     * @param disabledSelectedBorderColor the border color of this chip when not enabled but
     *   selected
     * @param borderWidth the border stroke width of this chip when not selected
     * @param selectedBorderWidth the border stroke width of this chip when selected
     */
    @Composable
    fun filterChipBorder(
        enabled: Boolean,
        selected: Boolean,
        borderColor: Color = FilterChipTokens.FlatUnselectedOutlineColor.value,
        selectedBorderColor: Color = Color.Transparent,
        disabledBorderColor: Color =
            FilterChipTokens.FlatDisabledUnselectedOutlineColor.value.copy(
                alpha = FilterChipTokens.FlatDisabledUnselectedOutlineOpacity
            ),
        disabledSelectedBorderColor: Color = Color.Transparent,
        borderWidth: Dp = FilterChipTokens.FlatUnselectedOutlineWidth,
        selectedBorderWidth: Dp = FilterChipTokens.FlatSelectedOutlineWidth,
    ): BorderStroke {
        val color =
            if (enabled) {
                if (selected) selectedBorderColor else borderColor
            } else {
                if (selected) disabledSelectedBorderColor else disabledBorderColor
            }
        return BorderStroke(if (selected) selectedBorderWidth else borderWidth, color)
    }

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in an elevated [FilterChip].
     */
    @Composable
    fun elevatedFilterChipColors() = MaterialTheme.colorScheme.defaultElevatedFilterChipColors

    /**
     * Creates a [SelectableChipColors] that represents the default container and content colors
     * used in an elevated [FilterChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconColor the color of this chip's start and end icons when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     *   selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun elevatedFilterChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        iconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        selectedContainerColor: Color = Color.Unspecified,
        disabledSelectedContainerColor: Color = Color.Unspecified,
        selectedLabelColor: Color = Color.Unspecified,
        selectedLeadingIconColor: Color = Color.Unspecified,
        selectedTrailingIconColor: Color = Color.Unspecified,
    ): SelectableChipColors =
        MaterialTheme.colorScheme.defaultElevatedFilterChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconColor = iconColor,
            trailingIconColor = iconColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )

    internal val ColorScheme.defaultElevatedFilterChipColors: SelectableChipColors
        get() {
            return defaultElevatedFilterChipColorsCached
                ?: SelectableChipColors(
                        containerColor =
                            fromToken(FilterChipTokens.ElevatedUnselectedContainerColor),
                        labelColor = fromToken(FilterChipTokens.UnselectedLabelTextColor),
                        leadingIconColor = fromToken(FilterChipTokens.UnselectedLeadingIconColor),
                        trailingIconColor = fromToken(FilterChipTokens.UnselectedTrailingIconColor),
                        disabledContainerColor =
                            fromToken(FilterChipTokens.ElevatedDisabledContainerColor)
                                .copy(alpha = FilterChipTokens.ElevatedDisabledContainerOpacity),
                        disabledLabelColor =
                            fromToken(FilterChipTokens.DisabledLabelTextColor)
                                .copy(alpha = FilterChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(FilterChipTokens.DisabledLeadingIconColor)
                                .copy(alpha = FilterChipTokens.DisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(FilterChipTokens.DisabledTrailingIconColor)
                                .copy(alpha = FilterChipTokens.DisabledLeadingIconOpacity),
                        selectedContainerColor =
                            fromToken(FilterChipTokens.ElevatedSelectedContainerColor),
                        disabledSelectedContainerColor =
                            fromToken(FilterChipTokens.ElevatedDisabledContainerColor)
                                .copy(alpha = FilterChipTokens.ElevatedDisabledContainerOpacity),
                        selectedLabelColor = fromToken(FilterChipTokens.SelectedLabelTextColor),
                        selectedLeadingIconColor =
                            fromToken(FilterChipTokens.SelectedLeadingIconColor),
                        selectedTrailingIconColor =
                            fromToken(FilterChipTokens.SelectedTrailingIconColor),
                    )
                    .also { defaultElevatedFilterChipColorsCached = it }
        }

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for an elevated [FilterChip].
     *
     * @param elevation the elevation used when the chip is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedFilterChipElevation(
        elevation: Dp = FilterChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = FilterChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = FilterChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = FilterChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = FilterChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = FilterChipTokens.ElevatedDisabledContainerElevation,
    ): SelectableChipElevation =
        SelectableChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /** Default shape of a filter chip. */
    val shape: Shape
        @Composable get() = FilterChipTokens.ContainerShape.value
}

/** Contains the baseline values used by an [InputChip]. */
object InputChipDefaults {
    /**
     * The height applied for an input chip. Note that you can override it by applying
     * Modifier.height directly on a chip.
     */
    val Height = InputChipTokens.ContainerHeight

    /** The size of an input chip icon. */
    val IconSize = InputChipTokens.LeadingIconSize

    /** The size of an input chip avatar. */
    val AvatarSize = InputChipTokens.AvatarSize

    /**
     * Creates a [SelectableChipColors] that represents the default container, label, and icon
     * colors used in an [InputChip].
     */
    @Composable fun inputChipColors() = MaterialTheme.colorScheme.defaultInputChipColors

    /**
     * Creates a [SelectableChipColors] that represents the default container, label, and icon
     * colors used in an [InputChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param leadingIconColor the color of this chip's start icon when enabled
     * @param trailingIconColor the color of this chip's start end icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledLeadingIconColor the color of this chip's start icon when not enabled
     * @param disabledTrailingIconColor the color of this chip's end icon when not enabled
     * @param selectedContainerColor the container color of this chip when selected
     * @param disabledSelectedContainerColor the container color of this chip when not enabled and
     *   selected
     * @param selectedLabelColor the label color of this chip when selected
     * @param selectedLeadingIconColor the color of this chip's start icon when selected
     * @param selectedTrailingIconColor the color of this chip's end icon when selected
     */
    @Composable
    fun inputChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        selectedContainerColor: Color = Color.Unspecified,
        disabledSelectedContainerColor: Color = Color.Unspecified,
        selectedLabelColor: Color = Color.Unspecified,
        selectedLeadingIconColor: Color = Color.Unspecified,
        selectedTrailingIconColor: Color = Color.Unspecified,
    ): SelectableChipColors =
        MaterialTheme.colorScheme.defaultInputChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            selectedLabelColor = selectedLabelColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )

    internal val ColorScheme.defaultInputChipColors: SelectableChipColors
        get() {
            return defaultInputChipColorsCached
                ?: SelectableChipColors(
                        containerColor = Color.Transparent,
                        labelColor = fromToken(InputChipTokens.UnselectedLabelTextColor),
                        leadingIconColor = fromToken(InputChipTokens.UnselectedLeadingIconColor),
                        trailingIconColor = fromToken(InputChipTokens.UnselectedTrailingIconColor),
                        disabledContainerColor = Color.Transparent,
                        disabledLabelColor =
                            fromToken(InputChipTokens.DisabledLabelTextColor)
                                .copy(alpha = InputChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(InputChipTokens.DisabledLeadingIconColor)
                                .copy(alpha = InputChipTokens.DisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(InputChipTokens.DisabledTrailingIconColor)
                                .copy(alpha = InputChipTokens.DisabledTrailingIconOpacity),
                        selectedContainerColor = fromToken(InputChipTokens.SelectedContainerColor),
                        disabledSelectedContainerColor =
                            fromToken(InputChipTokens.DisabledSelectedContainerColor)
                                .copy(alpha = InputChipTokens.DisabledSelectedContainerOpacity),
                        selectedLabelColor = fromToken(InputChipTokens.SelectedLabelTextColor),
                        selectedLeadingIconColor =
                            fromToken(InputChipTokens.SelectedLeadingIconColor),
                        selectedTrailingIconColor =
                            fromToken(InputChipTokens.SelectedTrailingIconColor),
                    )
                    .also { defaultInputChipColorsCached = it }
        }

    /**
     * Creates a [SelectableChipElevation] that will animate between the provided values according
     * to the Material specification for an [InputChip].
     *
     * @param elevation the elevation used when the [FilterChip] is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun inputChipElevation(
        elevation: Dp = InputChipTokens.ContainerElevation,
        pressedElevation: Dp = elevation,
        focusedElevation: Dp = elevation,
        hoveredElevation: Dp = elevation,
        draggedElevation: Dp = InputChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = elevation,
    ): SelectableChipElevation =
        SelectableChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [BorderStroke] that represents the default border used in an [InputChip].
     *
     * @param selected whether this chip is selected or not
     * @param enabled controls the enabled state of this chip. When `false`, this component will not
     *   respond to user input, and it will appear visually disabled and disabled to accessibility
     *   services.
     * @param borderColor the border color of this chip when enabled and not selected
     * @param selectedBorderColor the border color of this chip when enabled and selected
     * @param disabledBorderColor the border color of this chip when not enabled and not selected
     * @param disabledSelectedBorderColor the border color of this chip when not enabled but
     *   selected
     * @param borderWidth the border stroke width of this chip when not selected
     * @param selectedBorderWidth the border stroke width of this chip when selected
     */
    @Composable
    fun inputChipBorder(
        enabled: Boolean,
        selected: Boolean,
        borderColor: Color = InputChipTokens.UnselectedOutlineColor.value,
        selectedBorderColor: Color = Color.Transparent,
        disabledBorderColor: Color =
            InputChipTokens.DisabledUnselectedOutlineColor.value.copy(
                alpha = InputChipTokens.DisabledUnselectedOutlineOpacity
            ),
        disabledSelectedBorderColor: Color = Color.Transparent,
        borderWidth: Dp = InputChipTokens.UnselectedOutlineWidth,
        selectedBorderWidth: Dp = InputChipTokens.SelectedOutlineWidth,
    ): BorderStroke {
        val color =
            if (enabled) {
                if (selected) selectedBorderColor else borderColor
            } else {
                if (selected) disabledSelectedBorderColor else disabledBorderColor
            }
        return BorderStroke(if (selected) selectedBorderWidth else borderWidth, color)
    }

    /** Default shape of an input chip. */
    val shape: Shape
        @Composable get() = InputChipTokens.ContainerShape.value
}

/** Contains the baseline values used by [SuggestionChip]. */
object SuggestionChipDefaults {
    /**
     * The height applied for a suggestion chip. Note that you can override it by applying
     * Modifier.height directly on a chip.
     */
    val Height = SuggestionChipTokens.ContainerHeight

    /** The size of a suggestion chip icon. */
    val IconSize = SuggestionChipTokens.LeadingIconSize

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * a flat [SuggestionChip].
     */
    @Composable fun suggestionChipColors() = MaterialTheme.colorScheme.defaultSuggestionChipColors

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * a flat [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun suggestionChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        iconContentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledIconContentColor: Color = Color.Unspecified,
    ): ChipColors =
        MaterialTheme.colorScheme.defaultSuggestionChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = iconContentColor,
            trailingIconContentColor = Color.Unspecified,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconContentColor = disabledIconContentColor,
            disabledTrailingIconContentColor = Color.Unspecified,
        )

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for a flat [SuggestionChip].
     *
     * @param elevation the elevation used when the chip is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun suggestionChipElevation(
        elevation: Dp = SuggestionChipTokens.FlatContainerElevation,
        pressedElevation: Dp = elevation,
        focusedElevation: Dp = elevation,
        hoveredElevation: Dp = elevation,
        draggedElevation: Dp = SuggestionChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = elevation,
    ): ChipElevation =
        ChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /**
     * Creates a [BorderStroke] that represents the default border used in a flat [SuggestionChip].
     *
     * @param enabled whether the chip is enabled
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Composable
    fun suggestionChipBorder(
        enabled: Boolean,
        borderColor: Color = SuggestionChipTokens.FlatOutlineColor.value,
        disabledBorderColor: Color =
            SuggestionChipTokens.FlatDisabledOutlineColor.value.copy(
                alpha = SuggestionChipTokens.FlatDisabledOutlineOpacity
            ),
        borderWidth: Dp = SuggestionChipTokens.FlatOutlineWidth,
    ): BorderStroke = BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)

    /**
     * Creates a [ChipBorder] that represents the default border used in a flat [SuggestionChip].
     *
     * @param borderColor the border color of this chip when enabled
     * @param disabledBorderColor the border color of this chip when not enabled
     * @param borderWidth the border stroke width of this chip
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        "Maintained for binary compatibility. Use the suggestChipBorder functions instead",
        replaceWith =
            ReplaceWith(
                "suggestionChipBorder(enabled, borderColor," + " disabledBorderColor, borderWidth)"
            ),
        level = DeprecationLevel.WARNING,
    )
    @Composable
    fun suggestionChipBorder(
        borderColor: Color = SuggestionChipTokens.FlatOutlineColor.value,
        disabledBorderColor: Color =
            SuggestionChipTokens.FlatDisabledOutlineColor.value.copy(
                alpha = SuggestionChipTokens.FlatDisabledOutlineOpacity
            ),
        borderWidth: Dp = SuggestionChipTokens.FlatOutlineWidth,
    ): ChipBorder =
        ChipBorder(
            borderColor = borderColor,
            disabledBorderColor = disabledBorderColor,
            borderWidth = borderWidth,
        )

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [SuggestionChip].
     */
    @Composable
    fun elevatedSuggestionChipColors() =
        MaterialTheme.colorScheme.defaultElevatedSuggestionChipColors

    /**
     * Creates a [ChipColors] that represents the default container, label, and icon colors used in
     * an elevated [SuggestionChip].
     *
     * @param containerColor the container color of this chip when enabled
     * @param labelColor the label color of this chip when enabled
     * @param iconContentColor the color of this chip's icon when enabled
     * @param disabledContainerColor the container color of this chip when not enabled
     * @param disabledLabelColor the label color of this chip when not enabled
     * @param disabledIconContentColor the color of this chip's icon when not enabled
     */
    @Composable
    fun elevatedSuggestionChipColors(
        containerColor: Color = Color.Unspecified,
        labelColor: Color = Color.Unspecified,
        iconContentColor: Color = Color.Unspecified,
        disabledContainerColor: Color = Color.Unspecified,
        disabledLabelColor: Color = Color.Unspecified,
        disabledIconContentColor: Color = Color.Unspecified,
    ): ChipColors =
        MaterialTheme.colorScheme.defaultElevatedSuggestionChipColors.copy(
            containerColor = containerColor,
            labelColor = labelColor,
            leadingIconContentColor = iconContentColor,
            trailingIconContentColor = Color.Unspecified,
            disabledContainerColor = disabledContainerColor,
            disabledLabelColor = disabledLabelColor,
            disabledLeadingIconContentColor = disabledIconContentColor,
            disabledTrailingIconContentColor = Color.Unspecified,
        )

    internal val ColorScheme.defaultElevatedSuggestionChipColors: ChipColors
        get() {
            return defaultElevatedSuggestionChipColorsCached
                ?: ChipColors(
                        containerColor = fromToken(SuggestionChipTokens.ElevatedContainerColor),
                        labelColor = fromToken(SuggestionChipTokens.LabelTextColor),
                        leadingIconContentColor = fromToken(SuggestionChipTokens.LeadingIconColor),
                        trailingIconContentColor = Color.Unspecified,
                        disabledContainerColor =
                            fromToken(SuggestionChipTokens.ElevatedDisabledContainerColor)
                                .copy(alpha = AssistChipTokens.ElevatedDisabledContainerOpacity),
                        disabledLabelColor =
                            fromToken(SuggestionChipTokens.DisabledLabelTextColor)
                                .copy(alpha = SuggestionChipTokens.DisabledLabelTextOpacity),
                        disabledLeadingIconContentColor =
                            fromToken(AssistChipTokens.DisabledIconColor)
                                .copy(alpha = AssistChipTokens.DisabledIconOpacity),
                        disabledTrailingIconContentColor = Color.Unspecified,
                    )
                    .also { defaultElevatedSuggestionChipColorsCached = it }
        }

    /**
     * Creates a [ChipElevation] that will animate between the provided values according to the
     * Material specification for an elevated [SuggestionChip].
     *
     * @param elevation the elevation used when the chip is has no other [Interaction]s
     * @param pressedElevation the elevation used when the chip is pressed
     * @param focusedElevation the elevation used when the chip is focused
     * @param hoveredElevation the elevation used when the chip is hovered
     * @param draggedElevation the elevation used when the chip is dragged
     * @param disabledElevation the elevation used when the chip is not enabled
     */
    @Composable
    fun elevatedSuggestionChipElevation(
        elevation: Dp = SuggestionChipTokens.ElevatedContainerElevation,
        pressedElevation: Dp = SuggestionChipTokens.ElevatedPressedContainerElevation,
        focusedElevation: Dp = SuggestionChipTokens.ElevatedFocusContainerElevation,
        hoveredElevation: Dp = SuggestionChipTokens.ElevatedHoverContainerElevation,
        draggedElevation: Dp = SuggestionChipTokens.DraggedContainerElevation,
        disabledElevation: Dp = SuggestionChipTokens.ElevatedDisabledContainerElevation,
    ): ChipElevation =
        ChipElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            focusedElevation = focusedElevation,
            hoveredElevation = hoveredElevation,
            draggedElevation = draggedElevation,
            disabledElevation = disabledElevation,
        )

    /** Default shape of a suggestion chip. */
    val shape: Shape
        @Composable get() = SuggestionChipTokens.ContainerShape.value
}

@Composable
private fun Chip(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    shape: Shape,
    colors: ChipColors,
    elevation: ChipElevation?,
    border: BorderStroke?,
    minHeight: Dp,
    paddingValues: PaddingValues,
    interactionSource: MutableInteractionSource?,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled),
        shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
        border = border,
        interactionSource = interactionSource,
    ) {
        ChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            labelColor = labelColor,
            leadingIcon = leadingIcon,
            avatar = null,
            trailingIcon = trailingIcon,
            leadingIconColor = colors.leadingIconContentColor(enabled),
            trailingIconColor = colors.trailingIconContentColor(enabled),
            minHeight = minHeight,
            paddingValues = paddingValues,
        )
    }
}

@Composable
private fun SelectableChip(
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    shape: Shape,
    colors: SelectableChipColors,
    elevation: SelectableChipElevation?,
    border: BorderStroke?,
    minHeight: Dp,
    paddingValues: PaddingValues,
    interactionSource: MutableInteractionSource?,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = shape,
        color = colors.containerColor(enabled, selected),
        shadowElevation = elevation?.shadowElevation(enabled, interactionSource)?.value ?: 0.dp,
        border = border,
        interactionSource = interactionSource,
    ) {
        // Selectable chips are animating the leading and trailing icons when they change from
        // `null` to non-`null` values.
        AnimatingChipContent(
            label = label,
            labelTextStyle = labelTextStyle,
            leadingIcon = leadingIcon,
            avatar = avatar,
            labelColor = colors.labelColor(enabled, selected),
            trailingIcon = trailingIcon,
            leadingIconColor = colors.leadingIconContentColor(enabled, selected),
            trailingIconColor = colors.trailingIconContentColor(enabled, selected),
            minHeight = minHeight,
            paddingValues = paddingValues,
        )
    }
}

/**
 * Chip content.
 *
 * Use the [AnimatingChipContent] if you wish to animate the leading and trailing icons when they
 * change from `null` to non-`null` values. Otherwise, use this version for better performance.
 */
@Composable
private fun ChipContent(
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    minHeight: Dp,
    paddingValues: PaddingValues,
) {
    CompositionLocalProvider(
        LocalContentColor provides labelColor,
        LocalTextStyle provides labelTextStyle,
    ) {
        Layout(
            modifier = Modifier.defaultMinSize(minHeight = minHeight).padding(paddingValues),
            content = {
                if (avatar != null || leadingIcon != null) {
                    Box(
                        modifier = Modifier.layoutId(LeadingIconLayoutId),
                        contentAlignment = Alignment.Center,
                        content = {
                            val leadingContent =
                                leadingContent(avatar, leadingIcon, leadingIconColor)
                            if (leadingContent != null) {
                                leadingContent()
                            }
                        },
                    )
                }
                Row(
                    modifier =
                        Modifier.layoutId(LabelLayoutId).padding(HorizontalElementsPadding, 0.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    content = { label() },
                )
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.layoutId(TrailingIconLayoutId),
                        contentAlignment = Alignment.Center,
                        content = {
                            val trailingContent = trailingContent(trailingIcon, trailingIconColor)
                            if (trailingContent != null) {
                                trailingContent()
                            }
                        },
                    )
                }
            },
            measurePolicy = remember { ChipLayoutMeasurePolicy() },
        )
    }
}

/**
 * Similar to [ChipContent], but animating the leading and trailing icons when they change from
 * `null` to non-`null` values.
 */
@Composable
private fun AnimatingChipContent(
    label: @Composable () -> Unit,
    labelTextStyle: TextStyle,
    labelColor: Color,
    leadingIcon: @Composable (() -> Unit)?,
    avatar: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
    trailingIconColor: Color,
    minHeight: Dp,
    paddingValues: PaddingValues,
) {
    CompositionLocalProvider(
        LocalContentColor provides labelColor,
        LocalTextStyle provides labelTextStyle,
    ) {
        // TODO Load the motionScheme tokens from the component tokens file
        val fadeInSpec = MotionSchemeKeyTokens.SlowEffects.value<Float>()
        val fadeOutSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
        val expandSpec = MotionSchemeKeyTokens.FastSpatial.value<IntSize>()
        val shrinkSpec = MotionSchemeKeyTokens.DefaultEffects.value<IntSize>()
        Layout(
            modifier = Modifier.defaultMinSize(minHeight = minHeight).padding(paddingValues),
            content = {
                // Animate the leading content visibility.
                AnimatedVisibility(
                    modifier = Modifier.layoutId(LeadingIconLayoutId),
                    visible = avatar != null || leadingIcon != null,
                    enter =
                        expandHorizontally(
                            animationSpec = expandSpec,
                            expandFrom = Alignment.Start,
                        ) + fadeIn(animationSpec = fadeInSpec),
                    exit =
                        shrinkHorizontally(
                            animationSpec = shrinkSpec,
                            shrinkTowards = Alignment.Start,
                        ) + fadeOut(animationSpec = fadeOutSpec),
                ) {
                    // Retain the leading content. This will ensure that the AnimatedVisibility will
                    // work correctly when the content lambda changes to null. The retained
                    // content gets disposed once the AnimatedVisibility finishes exiting.
                    val leadingContentRetainedState =
                        rememberRetainedState(
                            targetValue = leadingContent(avatar, leadingIcon, leadingIconColor)
                        )

                    Box(
                        contentAlignment = Alignment.Center,
                        content = {
                            // Read from retained state
                            leadingContentRetainedState.value?.invoke()
                        },
                    )
                }
                Row(
                    modifier =
                        Modifier.layoutId(LabelLayoutId)
                            .padding(horizontal = HorizontalElementsPadding),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    content = { label() },
                )

                // Animate the trailing content visibility.
                AnimatedVisibility(
                    modifier = Modifier.layoutId(TrailingIconLayoutId),
                    visible = trailingIcon != null,
                    enter =
                        expandHorizontally(animationSpec = expandSpec, expandFrom = Alignment.End) +
                            fadeIn(animationSpec = fadeInSpec),
                    exit =
                        shrinkHorizontally(
                            animationSpec = shrinkSpec,
                            shrinkTowards = Alignment.End,
                        ) + fadeOut(animationSpec = fadeOutSpec),
                ) {
                    // Retain the trailing content. This will ensure that the AnimatedVisibility
                    // will work correctly when the content lambda changes to null. The retained
                    // content gets disposed once the AnimatedVisibility finishes exiting.
                    val trailingContentRetainedState =
                        rememberRetainedState(
                            targetValue = trailingContent(trailingIcon, trailingIconColor)
                        )

                    Box(
                        contentAlignment = Alignment.Center,
                        content = {
                            // Read from retained state
                            trailingContentRetainedState.value?.invoke()
                        },
                    )
                }
            },
            measurePolicy = remember { ChipLayoutMeasurePolicy() },
        )
    }
}

/**
 * Returns the actual leading content lambda based on priority (avatar > leadingIcon) and applied
 * with the given content color.
 */
@Composable
private fun leadingContent(
    avatar: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    leadingIconColor: Color,
): @Composable (() -> Unit)? =
    when {
        avatar != null -> avatar // An avatar takes precedence
        leadingIcon != null -> {
            @Composable {
                CompositionLocalProvider(
                    LocalContentColor provides leadingIconColor,
                    content = leadingIcon,
                )
            }
        }
        else -> null // Neither exists
    }

/** Returns the trailing content lambda applied with the given content color. */
@Composable
private fun trailingContent(
    trailingIcon: @Composable (() -> Unit)?,
    trailingIconColor: Color,
): @Composable (() -> Unit)? =
    if (trailingIcon != null) {
        @Composable {
            CompositionLocalProvider(
                LocalContentColor provides trailingIconColor,
                content = trailingIcon,
            )
        }
    } else {
        null
    }

/**
 * Remembers the last non-null value emitted by the [targetValue]. When [targetValue] becomes null,
 * this function continues to return the last non-null value, allowing content to gracefully animate
 * out.
 */
@Composable
private fun <T> rememberRetainedState(targetValue: T?): State<T?> {
    val retainedState = remember { mutableStateOf(targetValue) }
    if (targetValue != null) {
        retainedState.value = targetValue
    }
    return retainedState
}

private class ChipLayoutMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val leadingIconPlaceable: Placeable? =
            measurables
                .fastFirstOrNull { it.layoutId == LeadingIconLayoutId }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val leadingIconWidth = leadingIconPlaceable.widthOrZero
        val leadingIconHeight = leadingIconPlaceable.heightOrZero

        val trailingIconPlaceable: Placeable? =
            measurables
                .fastFirstOrNull { it.layoutId == TrailingIconLayoutId }
                ?.measure(constraints.copy(minWidth = 0, minHeight = 0))
        val trailingIconWidth = trailingIconPlaceable.widthOrZero
        val trailingIconHeight = trailingIconPlaceable.heightOrZero

        val labelPlaceable =
            measurables
                .fastFirst { it.layoutId == LabelLayoutId }
                .measure(constraints.offset(horizontal = -(leadingIconWidth + trailingIconWidth)))

        val width = leadingIconWidth + labelPlaceable.width + trailingIconWidth
        val height = maxOf(leadingIconHeight, labelPlaceable.height, trailingIconHeight)

        return layout(width, height) {
            leadingIconPlaceable?.placeRelative(
                0,
                Alignment.CenterVertically.align(leadingIconHeight, height),
            )
            labelPlaceable.placeRelative(leadingIconWidth, 0)
            trailingIconPlaceable?.placeRelative(
                leadingIconWidth + labelPlaceable.width,
                Alignment.CenterVertically.align(trailingIconHeight, height),
            )
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.fastSumBy { it.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = measurables.fastSumBy { it.maxIntrinsicWidth(height) }
}

/**
 * Represents the elevation used in a selectable chip in different states.
 *
 * Note that this default implementation does not take into consideration the `selectable` state
 * passed into its [shadowElevation]. If you wish to apply that state, use a different
 * [SelectableChipElevation].
 *
 * @param elevation the elevation used when the chip is enabled.
 * @param pressedElevation the elevation used when the chip is pressed.
 * @param focusedElevation the elevation used when the chip is focused
 * @param hoveredElevation the elevation used when the chip is hovered
 * @param draggedElevation the elevation used when the chip is dragged
 * @param disabledElevation the elevation used when the chip is not enabled
 */
@Immutable
class ChipElevation(
    val elevation: Dp,
    val pressedElevation: Dp,
    val focusedElevation: Dp,
    val hoveredElevation: Dp,
    val draggedElevation: Dp,
    val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a chip, depending on its [enabled] state and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the chip to give it higher emphasis.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
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
        var lastInteraction by remember { mutableStateOf<Interaction?>(null) }
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
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
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
                    is DragInteraction.Start -> draggedElevation
                    else -> elevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state
                    animatable.snapTo(target)
                } else {
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target,
                    )
                }
                lastInteraction = interaction
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipElevation) return false

        if (elevation != other.elevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Represents the elevation used in a selectable chip in different states.
 *
 * @param elevation the elevation used when the chip is enabled.
 * @param pressedElevation the elevation used when the chip is pressed.
 * @param focusedElevation the elevation used when the chip is focused
 * @param hoveredElevation the elevation used when the chip is hovered.
 * @param draggedElevation the elevation used when the chip is dragged
 * @param disabledElevation the elevation used when the chip is not enabled
 */
@Immutable
class SelectableChipElevation(
    val elevation: Dp,
    val pressedElevation: Dp,
    val focusedElevation: Dp,
    val hoveredElevation: Dp,
    val draggedElevation: Dp,
    val disabledElevation: Dp,
) {
    /**
     * Represents the shadow elevation used in a chip, depending on [enabled] and
     * [interactionSource].
     *
     * Shadow elevation is used to apply a shadow around the surface to give it higher emphasis.
     *
     * @param enabled whether the chip is enabled
     * @param interactionSource the [InteractionSource] for this chip
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
        var lastInteraction by remember { mutableStateOf<Interaction?>(null) }
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
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
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
                    is DragInteraction.Start -> draggedElevation
                    else -> elevation
                }
            }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            if (animatable.targetValue != target) {
                if (!enabled) {
                    // No transition when moving to a disabled state
                    animatable.snapTo(target)
                } else {
                    animatable.animateElevation(
                        from = lastInteraction,
                        to = interaction,
                        target = target,
                    )
                }
                lastInteraction = interaction
            }
        }

        return animatable.asState()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SelectableChipElevation) return false

        if (elevation != other.elevation) return false
        if (pressedElevation != other.pressedElevation) return false
        if (focusedElevation != other.focusedElevation) return false
        if (hoveredElevation != other.hoveredElevation) return false
        if (disabledElevation != other.disabledElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elevation.hashCode()
        result = 31 * result + pressedElevation.hashCode()
        result = 31 * result + focusedElevation.hashCode()
        result = 31 * result + hoveredElevation.hashCode()
        result = 31 * result + disabledElevation.hashCode()
        return result
    }
}

/**
 * Represents the container and content colors used in a clickable chip in different states.
 *
 * @param containerColor the container color of this chip when enabled
 * @param labelColor the label color of this chip when enabled
 * @param leadingIconContentColor the color of this chip's start icon when enabled
 * @param trailingIconContentColor the color of this chip's end icon when enabled
 * @param disabledContainerColor the container color of this chip when not enabled
 * @param disabledLabelColor the label color of this chip when not enabled
 * @param disabledLeadingIconContentColor the color of this chip's start icon when not enabled
 * @param disabledTrailingIconContentColor the color of this chip's end icon when not enabled
 * @constructor create an instance with arbitrary colors, see [AssistChipDefaults],
 *   [InputChipDefaults], and [SuggestionChipDefaults] for the default colors used in the various
 *   Chip configurations.
 */
@Immutable
class ChipColors
constructor(
    val containerColor: Color,
    val labelColor: Color,
    val leadingIconContentColor: Color,
    val trailingIconContentColor: Color,
    val disabledContainerColor: Color,
    val disabledLabelColor: Color,
    val disabledLeadingIconContentColor: Color,
    val disabledTrailingIconContentColor: Color,
    // TODO(b/113855296): Support other states: hover, focus, drag
) {
    /**
     * Returns a copy of this ChipColors, optionally overriding some of the values. This uses the
     * Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        labelColor: Color = this.labelColor,
        leadingIconContentColor: Color = this.leadingIconContentColor,
        trailingIconContentColor: Color = this.trailingIconContentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledLabelColor: Color = this.disabledLabelColor,
        disabledLeadingIconContentColor: Color = this.disabledLeadingIconContentColor,
        disabledTrailingIconContentColor: Color = this.disabledTrailingIconContentColor,
    ) =
        ChipColors(
            containerColor.takeOrElse { this.containerColor },
            labelColor.takeOrElse { this.labelColor },
            leadingIconContentColor.takeOrElse { this.leadingIconContentColor },
            trailingIconContentColor.takeOrElse { this.trailingIconContentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledLabelColor.takeOrElse { this.disabledLabelColor },
            disabledLeadingIconContentColor.takeOrElse { this.disabledLeadingIconContentColor },
            disabledTrailingIconContentColor.takeOrElse { this.disabledTrailingIconContentColor },
        )

    /**
     * Represents the container color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the label color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Stable
    internal fun labelColor(enabled: Boolean): Color =
        if (enabled) labelColor else disabledLabelColor

    /**
     * Represents the leading icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Stable
    internal fun leadingIconContentColor(enabled: Boolean): Color =
        if (enabled) leadingIconContentColor else disabledLeadingIconContentColor

    /**
     * Represents the trailing icon's content color for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Stable
    internal fun trailingIconContentColor(enabled: Boolean): Color =
        if (enabled) trailingIconContentColor else disabledTrailingIconContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipColors) return false

        if (containerColor != other.containerColor) return false
        if (labelColor != other.labelColor) return false
        if (leadingIconContentColor != other.leadingIconContentColor) return false
        if (trailingIconContentColor != other.trailingIconContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (disabledLeadingIconContentColor != other.disabledLeadingIconContentColor) return false
        if (disabledTrailingIconContentColor != other.disabledTrailingIconContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + leadingIconContentColor.hashCode()
        result = 31 * result + trailingIconContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + disabledLeadingIconContentColor.hashCode()
        result = 31 * result + disabledTrailingIconContentColor.hashCode()

        return result
    }
}

internal val ColorScheme.defaultSuggestionChipColors: ChipColors
    get() {
        return defaultSuggestionChipColorsCached
            ?: ChipColors(
                    containerColor = Color.Transparent,
                    labelColor = fromToken(SuggestionChipTokens.LabelTextColor),
                    leadingIconContentColor = fromToken(SuggestionChipTokens.LeadingIconColor),
                    trailingIconContentColor = Color.Unspecified,
                    disabledContainerColor = Color.Transparent,
                    disabledLabelColor =
                        fromToken(SuggestionChipTokens.DisabledLabelTextColor)
                            .copy(alpha = SuggestionChipTokens.DisabledLabelTextOpacity),
                    disabledLeadingIconContentColor =
                        fromToken(SuggestionChipTokens.DisabledLeadingIconColor)
                            .copy(alpha = SuggestionChipTokens.DisabledLeadingIconOpacity),
                    disabledTrailingIconContentColor = Color.Unspecified,
                )
                .also { defaultSuggestionChipColorsCached = it }
    }

/**
 * Represents the container and content colors used in a selectable chip in different states.
 *
 * See [FilterChipDefaults.filterChipColors] and [FilterChipDefaults.elevatedFilterChipColors] for
 * the default colors used in [FilterChip].
 */
@Immutable
class SelectableChipColors
constructor(
    private val containerColor: Color,
    private val labelColor: Color,
    private val leadingIconColor: Color,
    private val trailingIconColor: Color,
    private val disabledContainerColor: Color,
    private val disabledLabelColor: Color,
    private val disabledLeadingIconColor: Color,
    private val disabledTrailingIconColor: Color,
    private val selectedContainerColor: Color,
    private val disabledSelectedContainerColor: Color,
    private val selectedLabelColor: Color,
    private val selectedLeadingIconColor: Color,
    private val selectedTrailingIconColor: Color,
    // TODO(b/113855296): Support other states: hover, focus, drag
) {
    /**
     * Returns a copy of this SelectableChipColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        labelColor: Color = this.labelColor,
        leadingIconColor: Color = this.leadingIconColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledLabelColor: Color = this.disabledLabelColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
        selectedContainerColor: Color = this.selectedContainerColor,
        disabledSelectedContainerColor: Color = this.disabledSelectedContainerColor,
        selectedLabelColor: Color = this.selectedLabelColor,
        selectedLeadingIconColor: Color = this.selectedLeadingIconColor,
        selectedTrailingIconColor: Color = this.selectedTrailingIconColor,
    ) =
        SelectableChipColors(
            containerColor.takeOrElse { this.containerColor },
            labelColor.takeOrElse { this.labelColor },
            leadingIconColor.takeOrElse { this.leadingIconColor },
            trailingIconColor.takeOrElse { this.trailingIconColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledLabelColor.takeOrElse { this.disabledLabelColor },
            disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
            disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
            selectedContainerColor.takeOrElse { this.selectedContainerColor },
            disabledSelectedContainerColor.takeOrElse { this.disabledSelectedContainerColor },
            selectedLabelColor.takeOrElse { this.selectedLabelColor },
            selectedLeadingIconColor.takeOrElse { this.selectedLeadingIconColor },
            selectedTrailingIconColor.takeOrElse { this.selectedTrailingIconColor },
        )

    /**
     * Represents the container color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Stable
    internal fun containerColor(enabled: Boolean, selected: Boolean): Color {
        return when {
            !enabled -> if (selected) disabledSelectedContainerColor else disabledContainerColor
            !selected -> containerColor
            else -> selectedContainerColor
        }
    }

    /**
     * Represents the label color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Stable
    internal fun labelColor(enabled: Boolean, selected: Boolean): Color {
        return when {
            !enabled -> disabledLabelColor
            !selected -> labelColor
            else -> selectedLabelColor
        }
    }

    /**
     * Represents the leading icon color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Stable
    internal fun leadingIconContentColor(enabled: Boolean, selected: Boolean): Color {
        return when {
            !enabled -> disabledLeadingIconColor
            !selected -> leadingIconColor
            else -> selectedLeadingIconColor
        }
    }

    /**
     * Represents the trailing icon color for this chip, depending on [enabled] and [selected].
     *
     * @param enabled whether the chip is enabled
     * @param selected whether the chip is selected
     */
    @Stable
    internal fun trailingIconContentColor(enabled: Boolean, selected: Boolean): Color {
        return when {
            !enabled -> disabledTrailingIconColor
            !selected -> trailingIconColor
            else -> selectedTrailingIconColor
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is SelectableChipColors) return false

        if (containerColor != other.containerColor) return false
        if (labelColor != other.labelColor) return false
        if (leadingIconColor != other.leadingIconColor) return false
        if (trailingIconColor != other.trailingIconColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledLabelColor != other.disabledLabelColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (selectedLabelColor != other.selectedLabelColor) return false
        if (selectedLeadingIconColor != other.selectedLeadingIconColor) return false
        if (selectedTrailingIconColor != other.selectedTrailingIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + labelColor.hashCode()
        result = 31 * result + leadingIconColor.hashCode()
        result = 31 * result + trailingIconColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledLabelColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + selectedLabelColor.hashCode()
        result = 31 * result + selectedLeadingIconColor.hashCode()
        result = 31 * result + selectedTrailingIconColor.hashCode()

        return result
    }
}

/** Represents the border stroke used in a chip in different states. */
@Deprecated(
    "Maintained for binary compatibility. Use the chipBorder functions instead",
    level = DeprecationLevel.WARNING,
)
@Immutable
class ChipBorder
internal constructor(
    private val borderColor: Color,
    private val disabledBorderColor: Color,
    private val borderWidth: Dp,
) {
    /**
     * Represents the [BorderStroke] for this chip, depending on [enabled].
     *
     * @param enabled whether the chip is enabled
     */
    @Composable
    internal fun borderStroke(enabled: Boolean): State<BorderStroke?> {
        return rememberUpdatedState(
            BorderStroke(borderWidth, if (enabled) borderColor else disabledBorderColor)
        )
    }

    @Suppress("DEPRECATION")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ChipBorder) return false

        if (borderColor != other.borderColor) return false
        if (disabledBorderColor != other.disabledBorderColor) return false
        if (borderWidth != other.borderWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = borderColor.hashCode()
        result = 31 * result + disabledBorderColor.hashCode()
        result = 31 * result + borderWidth.hashCode()

        return result
    }
}

/** Returns the [PaddingValues] for the input chip. */
private fun inputChipPadding(
    hasAvatar: Boolean = false,
    hasLeadingIcon: Boolean = false,
    hasTrailingIcon: Boolean = false,
): PaddingValues {
    val start = if (hasAvatar || !hasLeadingIcon) 4.dp else 8.dp
    val end = if (hasTrailingIcon) 8.dp else 4.dp
    return PaddingValues(start = start, end = end)
}

/** The padding between the elements in the chip. */
private val HorizontalElementsPadding = 8.dp

/** Returns the [PaddingValues] for the assist chip. */
private val AssistChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)

/** [PaddingValues] for the filter chip. */
private val FilterChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)

/** Returns the [PaddingValues] for the suggestion chip. */
private val SuggestionChipPadding = PaddingValues(horizontal = HorizontalElementsPadding)

private const val LeadingIconLayoutId = "leadingIcon"
private const val LabelLayoutId = "label"
private const val TrailingIconLayoutId = "trailingIcon"
