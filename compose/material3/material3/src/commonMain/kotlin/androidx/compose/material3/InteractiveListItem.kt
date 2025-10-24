/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.takeOrElse

/**
 * [Material Design standard list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [ListItem] handles click events, calling its [onClick] lambda to trigger an
 * action. See other overloads for handling single-selection, multi-selection, or no interaction
 * handling.
 *
 * @param onClick called when this list item is clicked.
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. See
 *   [InteractiveListItemDefaults.shapes].
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun ListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: InteractiveListItemShapes = InteractiveListItemDefaults.shapes(),
    colors: InteractiveListItemColors = InteractiveListItemDefaults.colors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = false,
        applySemantics = {},
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * [Material Design standard list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [ListItem] represents a single-selection item, analogous to a [RadioButton]. See
 * other overloads for handling general click actions, multi-selection, or no interaction handling.
 *
 * @param selected whether or not this list item is selected.
 * @param onClick called when this list item is clicked.
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. See
 *   [InteractiveListItemDefaults.shapes].
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun ListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: InteractiveListItemShapes = InteractiveListItemDefaults.shapes(),
    colors: InteractiveListItemColors = InteractiveListItemDefaults.colors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = selected,
        applySemantics = { this.selected = selected },
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * [Material Design standard list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [ListItem] represents a multi-selection (toggleable) item, analogous to a
 * [Checkbox]. See other overloads for handling general click actions, single-selection, or no
 * interaction handling.
 *
 * @param checked whether this list item is toggled on or off.
 * @param onCheckedChange called when this toggleable list item is clicked.
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. See
 *   [InteractiveListItemDefaults.shapes].
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun ListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: InteractiveListItemShapes = InteractiveListItemDefaults.shapes(),
    colors: InteractiveListItemColors = InteractiveListItemDefaults.colors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = checked,
        applySemantics = { toggleableState = ToggleableState(checked) },
        onClick = { onCheckedChange(!checked) },
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * [Material Design segmented list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [SegmentedListItem] handles click events, calling its [onClick] lambda to
 * trigger an action. See other overloads for handling single-selection, multi-selection, or no
 * interaction handling.
 *
 * @param onClick called when this list item is clicked.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. The base shape depends on the index of
 *   the item within the overall list. See [InteractiveListItemDefaults.segmentedShapes].
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun SegmentedListItem(
    onClick: () -> Unit,
    shapes: InteractiveListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: InteractiveListItemColors = InteractiveListItemDefaults.segmentedColors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = false,
        applySemantics = {},
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * [Material Design segmented list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [SegmentedListItem] represents a single-selection item, analogous to a
 * [RadioButton]. See other overloads for handling general click actions, multi-selection, or no
 * interaction handling.
 *
 * @param selected whether or not this list item is selected.
 * @param onClick called when this list item is clicked.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. The base shape depends on the index of
 *   the item within the overall list. See [InteractiveListItemDefaults.segmentedShapes].
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun SegmentedListItem(
    selected: Boolean,
    onClick: () -> Unit,
    shapes: InteractiveListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: InteractiveListItemColors = InteractiveListItemDefaults.segmentedColors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = selected,
        applySemantics = { this.selected = selected },
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * [Material Design segmented list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [SegmentedListItem] represents a multi-selection (toggleable) item, analogous to
 * a [Checkbox]. See other overloads for handling general click actions, single-selection, or no
 * interaction handling.
 *
 * @param checked whether this list item is toggled on or off.
 * @param onCheckedChange called when this toggleable list item is clicked.
 * @param shapes the [InteractiveListItemShapes] that this list item will use to morph between
 *   depending on the user's interaction with the list item. The base shape depends on the index of
 *   the item within the overall list. See [InteractiveListItemDefaults.segmentedShapes].
 * @param modifier the [Modifier] to be applied to this list item.
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param leadingContent the leading content of this list item, such as an icon or avatar.
 * @param trailingContent the trailing content of this list item, such as a checkbox, switch, or
 *   icon.
 * @param overlineContent the content displayed above the main content of the list item.
 * @param supportingContent the content displayed below the main content of the list item.
 * @param verticalAlignment the vertical alignment of children within the list item, after
 *   accounting for [contentPadding].
 * @param onLongClick called when this list item is long clicked (long-pressed).
 * @param onLongClickLabel semantic / accessibility label for the [onLongClick] action.
 * @param colors the [InteractiveListItemColors] that will be used to resolve the colors used for
 *   this list item in different states. See [InteractiveListItemDefaults.colors].
 * @param elevation the [InteractiveListItemElevation] used to resolve the elevation for this list
 *   item in different states. See [InteractiveListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline content.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
internal fun SegmentedListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: InteractiveListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = InteractiveListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: InteractiveListItemColors = InteractiveListItemDefaults.segmentedColors(),
    elevation: InteractiveListItemElevation = InteractiveListItemDefaults.elevation(),
    contentPadding: PaddingValues = InteractiveListItemDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    InteractiveListItem(
        modifier = modifier,
        content = content,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        verticalAlignment = verticalAlignment,
        enabled = enabled,
        selected = checked,
        applySemantics = { toggleableState = ToggleableState(checked) },
        onClick = { onCheckedChange(!checked) },
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        interactionSource = interactionSource,
        colors = colors,
        shapes = shapes,
        elevation = elevation,
        contentPadding = contentPadding,
    )
}

/**
 * Represents the colors of a list item in different states.
 *
 * @param containerColor the container color of the list item.
 * @param contentColor the content color of the list item.
 * @param leadingContentColor the color of the leading content of the list item.
 * @param trailingContentColor the color of the trailing content of the list item.
 * @param overlineContentColor the color of the overline content of the list item.
 * @param supportingContentColor the color of the supporting content of the list item.
 * @param disabledContainerColor the container color of the list item when disabled.
 * @param disabledContentColor the content color of the list item when disabled.
 * @param disabledLeadingContentColor the color of the leading content of the list item when
 *   disabled.
 * @param disabledTrailingContentColor the color of the trailing content of the list item when
 *   disabled.
 * @param disabledOverlineContentColor the color of the overline content of the list item when
 *   disabled.
 * @param disabledSupportingContentColor the color of the supporting content of the list item when
 *   disabled.
 * @param selectedContainerColor the container color of the list item when selected.
 * @param selectedContentColor the content color of the list item when selected.
 * @param selectedLeadingContentColor the color of the leading content of the list item when
 *   selected.
 * @param selectedTrailingContentColor the color of the trailing content of the list item when
 *   selected.
 * @param selectedOverlineContentColor the color of the overline content of the list item when
 *   selected.
 * @param selectedSupportingContentColor the color of the supporting content of the list item when
 *   selected.
 * @param draggedContainerColor the container color of the list item when dragged.
 * @param draggedContentColor the content color of the list item when dragged.
 * @param draggedLeadingContentColor the color of the leading content of the list item when dragged.
 * @param draggedTrailingContentColor the color of the trailing content of the list item when
 *   dragged.
 * @param draggedOverlineContentColor the color of the overline content of the list item when
 *   dragged.
 * @param draggedSupportingContentColor the color of the supporting content of the list item when
 *   dragged.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
internal class InteractiveListItemColors(
    // default
    val containerColor: Color,
    val contentColor: Color,
    val leadingContentColor: Color,
    val trailingContentColor: Color,
    val overlineContentColor: Color,
    val supportingContentColor: Color,
    // disabled
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val disabledLeadingContentColor: Color,
    val disabledTrailingContentColor: Color,
    val disabledOverlineContentColor: Color,
    val disabledSupportingContentColor: Color,
    // selected
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val selectedLeadingContentColor: Color,
    val selectedTrailingContentColor: Color,
    val selectedOverlineContentColor: Color,
    val selectedSupportingContentColor: Color,
    // dragged
    val draggedContainerColor: Color,
    val draggedContentColor: Color,
    val draggedLeadingContentColor: Color,
    val draggedTrailingContentColor: Color,
    val draggedOverlineContentColor: Color,
    val draggedSupportingContentColor: Color,
) {
    /**
     * Returns the container color of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun containerColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledContainerColor
            dragged -> draggedContainerColor
            selected -> selectedContainerColor
            else -> containerColor
        }

    /**
     * Returns the content color of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun contentColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledContentColor
            dragged -> draggedContentColor
            selected -> selectedContentColor
            else -> contentColor
        }

    /**
     * Returns the color of the leading content of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun leadingContentColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledLeadingContentColor
            dragged -> draggedLeadingContentColor
            selected -> selectedLeadingContentColor
            else -> leadingContentColor
        }

    /**
     * Returns the color of the trailing content of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun trailingContentColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledTrailingContentColor
            dragged -> draggedTrailingContentColor
            selected -> selectedTrailingContentColor
            else -> trailingContentColor
        }

    /**
     * Returns the color of the overline content of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun overlineContentColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledOverlineContentColor
            dragged -> draggedOverlineContentColor
            selected -> selectedOverlineContentColor
            else -> overlineContentColor
        }

    /**
     * Returns the color of the supporting content of the list item based on the current state.
     *
     * @param enabled whether the list item is enabled.
     * @param selected whether the list item is selected.
     * @param dragged whether the list item is dragged.
     */
    fun supportingContentColor(enabled: Boolean, selected: Boolean, dragged: Boolean): Color =
        when {
            !enabled -> disabledSupportingContentColor
            dragged -> draggedSupportingContentColor
            selected -> selectedSupportingContentColor
            else -> supportingContentColor
        }

    /**
     * Returns a copy of this [InteractiveListItemColors], optionally overriding some of the values.
     * This uses [Color.Unspecified] to mean “use the value from the source”.
     */
    fun copy(
        // default
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        leadingContentColor: Color = this.leadingContentColor,
        trailingContentColor: Color = this.trailingContentColor,
        overlineContentColor: Color = this.overlineContentColor,
        supportingContentColor: Color = this.supportingContentColor,
        // disabled
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
        disabledLeadingContentColor: Color = this.disabledLeadingContentColor,
        disabledTrailingContentColor: Color = this.disabledTrailingContentColor,
        disabledOverlineContentColor: Color = this.disabledOverlineContentColor,
        disabledSupportingContentColor: Color = this.disabledSupportingContentColor,
        // selected
        selectedContainerColor: Color = this.selectedContainerColor,
        selectedContentColor: Color = this.selectedContentColor,
        selectedLeadingContentColor: Color = this.selectedLeadingContentColor,
        selectedTrailingContentColor: Color = this.selectedTrailingContentColor,
        selectedOverlineContentColor: Color = this.selectedOverlineContentColor,
        selectedSupportingContentColor: Color = this.selectedSupportingContentColor,
        // dragged
        draggedContainerColor: Color = this.draggedContainerColor,
        draggedContentColor: Color = this.draggedContentColor,
        draggedLeadingContentColor: Color = this.draggedLeadingContentColor,
        draggedTrailingContentColor: Color = this.draggedTrailingContentColor,
        draggedOverlineContentColor: Color = this.draggedOverlineContentColor,
        draggedSupportingContentColor: Color = this.draggedSupportingContentColor,
    ): InteractiveListItemColors {
        return InteractiveListItemColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            contentColor = contentColor.takeOrElse { this.contentColor },
            leadingContentColor = leadingContentColor.takeOrElse { this.leadingContentColor },
            trailingContentColor = trailingContentColor.takeOrElse { this.trailingContentColor },
            overlineContentColor = overlineContentColor.takeOrElse { this.overlineContentColor },
            supportingContentColor =
                supportingContentColor.takeOrElse { this.supportingContentColor },
            disabledContainerColor =
                disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor = disabledContentColor.takeOrElse { this.disabledContentColor },
            disabledLeadingContentColor =
                disabledLeadingContentColor.takeOrElse { this.disabledLeadingContentColor },
            disabledTrailingContentColor =
                disabledTrailingContentColor.takeOrElse { this.disabledTrailingContentColor },
            disabledOverlineContentColor =
                disabledOverlineContentColor.takeOrElse { this.disabledOverlineContentColor },
            disabledSupportingContentColor =
                disabledSupportingContentColor.takeOrElse { this.disabledSupportingContentColor },
            selectedContainerColor =
                selectedContainerColor.takeOrElse { this.selectedContainerColor },
            selectedContentColor = selectedContentColor.takeOrElse { this.selectedContentColor },
            selectedLeadingContentColor =
                selectedLeadingContentColor.takeOrElse { this.selectedLeadingContentColor },
            selectedTrailingContentColor =
                selectedTrailingContentColor.takeOrElse { this.selectedTrailingContentColor },
            selectedOverlineContentColor =
                selectedOverlineContentColor.takeOrElse { this.selectedOverlineContentColor },
            selectedSupportingContentColor =
                selectedSupportingContentColor.takeOrElse { this.selectedSupportingContentColor },
            draggedContainerColor = draggedContainerColor.takeOrElse { this.draggedContainerColor },
            draggedContentColor = draggedContentColor.takeOrElse { this.draggedContentColor },
            draggedLeadingContentColor =
                draggedLeadingContentColor.takeOrElse { this.draggedLeadingContentColor },
            draggedTrailingContentColor =
                draggedTrailingContentColor.takeOrElse { this.draggedTrailingContentColor },
            draggedOverlineContentColor =
                draggedOverlineContentColor.takeOrElse { this.draggedOverlineContentColor },
            draggedSupportingContentColor =
                draggedSupportingContentColor.takeOrElse { this.draggedSupportingContentColor },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is InteractiveListItemColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (leadingContentColor != other.leadingContentColor) return false
        if (trailingContentColor != other.trailingContentColor) return false
        if (overlineContentColor != other.overlineContentColor) return false
        if (supportingContentColor != other.supportingContentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledLeadingContentColor != other.disabledLeadingContentColor) return false
        if (disabledTrailingContentColor != other.disabledTrailingContentColor) return false
        if (disabledOverlineContentColor != other.disabledOverlineContentColor) return false
        if (disabledSupportingContentColor != other.disabledSupportingContentColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedLeadingContentColor != other.selectedLeadingContentColor) return false
        if (selectedTrailingContentColor != other.selectedTrailingContentColor) return false
        if (selectedOverlineContentColor != other.selectedOverlineContentColor) return false
        if (selectedSupportingContentColor != other.selectedSupportingContentColor) return false
        if (draggedContainerColor != other.draggedContainerColor) return false
        if (draggedContentColor != other.draggedContentColor) return false
        if (draggedLeadingContentColor != other.draggedLeadingContentColor) return false
        if (draggedTrailingContentColor != other.draggedTrailingContentColor) return false
        if (draggedOverlineContentColor != other.draggedOverlineContentColor) return false
        if (draggedSupportingContentColor != other.draggedSupportingContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + leadingContentColor.hashCode()
        result = 31 * result + trailingContentColor.hashCode()
        result = 31 * result + overlineContentColor.hashCode()
        result = 31 * result + supportingContentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledLeadingContentColor.hashCode()
        result = 31 * result + disabledTrailingContentColor.hashCode()
        result = 31 * result + disabledOverlineContentColor.hashCode()
        result = 31 * result + disabledSupportingContentColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedLeadingContentColor.hashCode()
        result = 31 * result + selectedTrailingContentColor.hashCode()
        result = 31 * result + selectedOverlineContentColor.hashCode()
        result = 31 * result + selectedSupportingContentColor.hashCode()
        result = 31 * result + draggedContainerColor.hashCode()
        result = 31 * result + draggedContentColor.hashCode()
        result = 31 * result + draggedLeadingContentColor.hashCode()
        result = 31 * result + draggedTrailingContentColor.hashCode()
        result = 31 * result + draggedOverlineContentColor.hashCode()
        result = 31 * result + draggedSupportingContentColor.hashCode()
        return result
    }
}

/**
 * Represents the shapes of a list item in different states.
 *
 * @param shape the default shape of the list item.
 * @param selectedShape the shape of the list item when selected.
 * @param pressedShape the shape of the list item when pressed.
 * @param focusedShape the shape of the list item when focused.
 * @param hoveredShape the shape of the list item when hovered.
 * @param draggedShape the shape of the list item when dragged.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
internal class InteractiveListItemShapes(
    val shape: Shape,
    val selectedShape: Shape,
    val pressedShape: Shape,
    val focusedShape: Shape,
    val hoveredShape: Shape,
    val draggedShape: Shape,
) {
    /**
     * Returns a copy of this [InteractiveListItemShapes], optionally overriding some of the values.
     */
    fun copy(
        shape: Shape? = this.shape,
        selectedShape: Shape? = this.selectedShape,
        pressedShape: Shape? = this.pressedShape,
        focusedShape: Shape? = this.focusedShape,
        hoveredShape: Shape? = this.hoveredShape,
        draggedShape: Shape? = this.draggedShape,
    ): InteractiveListItemShapes =
        InteractiveListItemShapes(
            shape = shape.takeOrElse { this.shape },
            selectedShape = selectedShape.takeOrElse { this.selectedShape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
            focusedShape = focusedShape.takeOrElse { this.focusedShape },
            hoveredShape = hoveredShape.takeOrElse { this.hoveredShape },
            draggedShape = draggedShape.takeOrElse { this.draggedShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is InteractiveListItemShapes) return false

        if (shape != other.shape) return false
        if (selectedShape != other.selectedShape) return false
        if (pressedShape != other.pressedShape) return false
        if (focusedShape != other.focusedShape) return false
        if (hoveredShape != other.hoveredShape) return false
        if (draggedShape != other.draggedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + selectedShape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + focusedShape.hashCode()
        result = 31 * result + hoveredShape.hashCode()
        result = 31 * result + draggedShape.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val InteractiveListItemShapes.hasRoundedCornerShapes: Boolean
    get() =
        shape is RoundedCornerShape &&
            selectedShape is RoundedCornerShape &&
            pressedShape is RoundedCornerShape &&
            focusedShape is RoundedCornerShape &&
            hoveredShape is RoundedCornerShape &&
            draggedShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InteractiveListItemShapes.shapeForInteraction(
    selected: Boolean,
    pressed: Boolean,
    focused: Boolean,
    hovered: Boolean,
    dragged: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        when {
            pressed -> pressedShape
            dragged -> draggedShape
            selected -> selectedShape
            focused -> focusedShape
            hovered -> hoveredShape
            else -> shape
        }

    if (hasRoundedCornerShapes) {
        return key(this) { rememberAnimatedShape(shape as RoundedCornerShape, animationSpec) }
    }

    return shape
}

/**
 * Represents the elevation of a list item in different states.
 *
 * @param elevation the default elevation of the list item.
 * @param draggedElevation the elevation of the list item when dragged.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
internal class InteractiveListItemElevation(val elevation: Dp, val draggedElevation: Dp) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is InteractiveListItemElevation) return false

        if (elevation != other.elevation) return false
        if (draggedElevation != other.draggedElevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = elevation.hashCode()
        result = 31 * result + draggedElevation.hashCode()
        return result
    }
}

/** Contains the default values for interactive [ListItem]s. */
@ExperimentalMaterial3ExpressiveApi
@Immutable
internal object InteractiveListItemDefaults {
    /** The default padding applied to all content within a list item. */
    val ContentPadding: PaddingValues =
        PaddingValues(
            start = InteractiveListStartPadding,
            end = InteractiveListEndPadding,
            top = InteractiveListTopPadding,
            bottom = InteractiveListBottomPadding,
        )

    /**
     * Creates an [InteractiveListItemColors] that represents the default colors for an interactive
     * [ListItem] in different states.
     */
    @Composable
    fun colors(): InteractiveListItemColors {
        return MaterialTheme.colorScheme.defaultInteractiveListItemColors
    }

    /**
     * Creates an [InteractiveListItemColors] that represents the default colors for an interactive
     * [ListItem] in different states.
     *
     * @param containerColor the container color of the list item.
     * @param contentColor the content color of the list item.
     * @param leadingContentColor the leading content color of the list item.
     * @param trailingContentColor the trailing content color of the list item.
     * @param overlineContentColor the overline content color of the list item.
     * @param supportingContentColor the supporting content color of the list item.
     * @param disabledContainerColor the container color of the list item when disabled.
     * @param disabledContentColor the content color of the list item when disabled.
     * @param disabledLeadingContentColor the leading content color of the list item when disabled.
     * @param disabledTrailingContentColor the trailing content color of the list item when
     *   disabled.
     * @param disabledOverlineContentColor the overline content color of the list item when
     *   disabled.
     * @param disabledSupportingContentColor the supporting content color of the list item when
     *   disabled.
     * @param selectedContainerColor the container color of the list item when selected.
     * @param selectedContentColor the content color of the list item when selected.
     * @param selectedLeadingContentColor the leading content color of the list item when selected.
     * @param selectedTrailingContentColor the trailing content color of the list item when
     *   selected.
     * @param selectedOverlineContentColor the overline content color of the list item when
     *   selected.
     * @param selectedSupportingContentColor the supporting content color of the list item when
     *   selected.
     * @param draggedContainerColor the container color of the list item when dragged.
     * @param draggedContentColor the content color of the list item when dragged.
     * @param draggedLeadingContentColor the leading content color of the list item when dragged.
     * @param draggedTrailingContentColor the trailing content color of the list item when dragged.
     * @param draggedOverlineContentColor the overline content color of the list item when dragged.
     * @param draggedSupportingContentColor the supporting content color of the list item when
     *   dragged.
     */
    @Composable
    fun colors(
        // default
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        leadingContentColor: Color = Color.Unspecified,
        trailingContentColor: Color = Color.Unspecified,
        overlineContentColor: Color = Color.Unspecified,
        supportingContentColor: Color = Color.Unspecified,
        // disabled
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledLeadingContentColor: Color = Color.Unspecified,
        disabledTrailingContentColor: Color = Color.Unspecified,
        disabledOverlineContentColor: Color = Color.Unspecified,
        disabledSupportingContentColor: Color = Color.Unspecified,
        // selected
        selectedContainerColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
        selectedLeadingContentColor: Color = Color.Unspecified,
        selectedTrailingContentColor: Color = Color.Unspecified,
        selectedOverlineContentColor: Color = Color.Unspecified,
        selectedSupportingContentColor: Color = Color.Unspecified,
        // dragged
        draggedContainerColor: Color = Color.Unspecified,
        draggedContentColor: Color = Color.Unspecified,
        draggedLeadingContentColor: Color = Color.Unspecified,
        draggedTrailingContentColor: Color = Color.Unspecified,
        draggedOverlineContentColor: Color = Color.Unspecified,
        draggedSupportingContentColor: Color = Color.Unspecified,
    ): InteractiveListItemColors {
        return MaterialTheme.colorScheme.defaultInteractiveListItemColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            leadingContentColor = leadingContentColor,
            trailingContentColor = trailingContentColor,
            overlineContentColor = overlineContentColor,
            supportingContentColor = supportingContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledLeadingContentColor = disabledLeadingContentColor,
            disabledTrailingContentColor = disabledTrailingContentColor,
            disabledOverlineContentColor = disabledOverlineContentColor,
            disabledSupportingContentColor = disabledSupportingContentColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedLeadingContentColor = selectedLeadingContentColor,
            selectedTrailingContentColor = selectedTrailingContentColor,
            selectedOverlineContentColor = selectedOverlineContentColor,
            selectedSupportingContentColor = selectedSupportingContentColor,
            draggedContainerColor = draggedContainerColor,
            draggedContentColor = draggedContentColor,
            draggedLeadingContentColor = draggedLeadingContentColor,
            draggedTrailingContentColor = draggedTrailingContentColor,
            draggedOverlineContentColor = draggedOverlineContentColor,
            draggedSupportingContentColor = draggedSupportingContentColor,
        )
    }

    // TODO: load tokens from component file
    internal val ColorScheme.defaultInteractiveListItemColors: InteractiveListItemColors
        get() {
            return defaultInteractiveListItemColorsCached
                ?: InteractiveListItemColors(
                        // default
                        containerColor = fromToken(ColorSchemeKeyTokens.SurfaceBright),
                        contentColor = fromToken(ColorSchemeKeyTokens.OnSurface),
                        leadingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        trailingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        overlineContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        supportingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        // selected
                        selectedContainerColor = fromToken(ColorSchemeKeyTokens.SecondaryContainer),
                        selectedContentColor = fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedLeadingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedTrailingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedOverlineContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedSupportingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        // disabled
                        disabledContainerColor = fromToken(ColorSchemeKeyTokens.SurfaceBright),
                        disabledContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledLeadingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledTrailingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledOverlineContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledSupportingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        // dragged
                        draggedContainerColor = fromToken(ColorSchemeKeyTokens.TertiaryContainer),
                        draggedContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedLeadingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedTrailingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedOverlineContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedSupportingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                    )
                    .also { defaultInteractiveListItemColorsCached = it }
        }

    /**
     * Creates an [InteractiveListItemColors] that represents the default colors for an interactive
     * [SegmentedListItem] in different states.
     */
    @Composable
    fun segmentedColors(): InteractiveListItemColors =
        MaterialTheme.colorScheme.defaultSegmentedInteractiveListItemColors

    /**
     * Creates an [InteractiveListItemColors] that represents the default colors for an interactive
     * [SegmentedListItem] in different states.
     *
     * @param containerColor the container color of the list item.
     * @param contentColor the content color of the list item.
     * @param leadingContentColor the leading content color of the list item.
     * @param trailingContentColor the trailing content color of the list item.
     * @param overlineContentColor the overline content color of the list item.
     * @param supportingContentColor the supporting content color of the list item.
     * @param disabledContainerColor the container color of the list item when disabled.
     * @param disabledContentColor the content color of the list item when disabled.
     * @param disabledLeadingContentColor the leading content color of the list item when disabled.
     * @param disabledTrailingContentColor the trailing content color of the list item when
     *   disabled.
     * @param disabledOverlineContentColor the overline content color of the list item when
     *   disabled.
     * @param disabledSupportingContentColor the supporting content color of the list item when
     *   disabled.
     * @param selectedContainerColor the container color of the list item when selected.
     * @param selectedContentColor the content color of the list item when selected.
     * @param selectedLeadingContentColor the leading content color of the list item when selected.
     * @param selectedTrailingContentColor the trailing content color of the list item when
     *   selected.
     * @param selectedOverlineContentColor the overline content color of the list item when
     *   selected.
     * @param selectedSupportingContentColor the supporting content color of the list item when
     *   selected.
     * @param draggedContainerColor the container color of the list item when dragged.
     * @param draggedContentColor the content color of the list item when dragged.
     * @param draggedLeadingContentColor the leading content color of the list item when dragged.
     * @param draggedTrailingContentColor the trailing content color of the list item when dragged.
     * @param draggedOverlineContentColor the overline content color of the list item when dragged.
     * @param draggedSupportingContentColor the supporting content color of the list item when
     *   dragged.
     */
    @Composable
    fun segmentedColors(
        // default
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified,
        leadingContentColor: Color = Color.Unspecified,
        trailingContentColor: Color = Color.Unspecified,
        overlineContentColor: Color = Color.Unspecified,
        supportingContentColor: Color = Color.Unspecified,
        // disabled
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color = Color.Unspecified,
        disabledLeadingContentColor: Color = Color.Unspecified,
        disabledTrailingContentColor: Color = Color.Unspecified,
        disabledOverlineContentColor: Color = Color.Unspecified,
        disabledSupportingContentColor: Color = Color.Unspecified,
        // selected
        selectedContainerColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
        selectedLeadingContentColor: Color = Color.Unspecified,
        selectedTrailingContentColor: Color = Color.Unspecified,
        selectedOverlineContentColor: Color = Color.Unspecified,
        selectedSupportingContentColor: Color = Color.Unspecified,
        // dragged
        draggedContainerColor: Color = Color.Unspecified,
        draggedContentColor: Color = Color.Unspecified,
        draggedLeadingContentColor: Color = Color.Unspecified,
        draggedTrailingContentColor: Color = Color.Unspecified,
        draggedOverlineContentColor: Color = Color.Unspecified,
        draggedSupportingContentColor: Color = Color.Unspecified,
    ): InteractiveListItemColors {
        return MaterialTheme.colorScheme.defaultSegmentedInteractiveListItemColors.copy(
            containerColor = containerColor,
            contentColor = contentColor,
            leadingContentColor = leadingContentColor,
            trailingContentColor = trailingContentColor,
            overlineContentColor = overlineContentColor,
            supportingContentColor = supportingContentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            disabledLeadingContentColor = disabledLeadingContentColor,
            disabledTrailingContentColor = disabledTrailingContentColor,
            disabledOverlineContentColor = disabledOverlineContentColor,
            disabledSupportingContentColor = disabledSupportingContentColor,
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedLeadingContentColor = selectedLeadingContentColor,
            selectedTrailingContentColor = selectedTrailingContentColor,
            selectedOverlineContentColor = selectedOverlineContentColor,
            selectedSupportingContentColor = selectedSupportingContentColor,
            draggedContainerColor = draggedContainerColor,
            draggedContentColor = draggedContentColor,
            draggedLeadingContentColor = draggedLeadingContentColor,
            draggedTrailingContentColor = draggedTrailingContentColor,
            draggedOverlineContentColor = draggedOverlineContentColor,
            draggedSupportingContentColor = draggedSupportingContentColor,
        )
    }

    // TODO: load tokens from component file
    internal val ColorScheme.defaultSegmentedInteractiveListItemColors: InteractiveListItemColors
        get() {
            return defaultSegmentedInteractiveListItemColorsCached
                ?: InteractiveListItemColors(
                        // default
                        containerColor = fromToken(ColorSchemeKeyTokens.Surface),
                        contentColor = fromToken(ColorSchemeKeyTokens.OnSurface),
                        leadingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        trailingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        overlineContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        supportingContentColor = fromToken(ColorSchemeKeyTokens.OnSurfaceVariant),
                        // selected
                        selectedContainerColor = fromToken(ColorSchemeKeyTokens.SecondaryContainer),
                        selectedContentColor = fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedLeadingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedTrailingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedOverlineContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        selectedSupportingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSecondaryContainer),
                        // disabled
                        disabledContainerColor = fromToken(ColorSchemeKeyTokens.Surface),
                        disabledContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledLeadingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledTrailingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledOverlineContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        disabledSupportingContentColor =
                            fromToken(ColorSchemeKeyTokens.OnSurface)
                                .copy(alpha = InteractiveListDisabledOpacity),
                        // dragged
                        draggedContainerColor = fromToken(ColorSchemeKeyTokens.TertiaryContainer),
                        draggedContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedLeadingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedTrailingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedOverlineContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                        draggedSupportingContentColor = fromToken(ColorSchemeKeyTokens.Tertiary),
                    )
                    .also { defaultSegmentedInteractiveListItemColorsCached = it }
        }

    /**
     * Creates an [InteractiveListItemShapes] that represents the default shapes for an interactive
     * [ListItem] in different states.
     */
    @Composable
    fun shapes(): InteractiveListItemShapes = MaterialTheme.shapes.defaultInteractiveListItemShapes

    /**
     * Creates an [InteractiveListItemShapes] that represents the default shapes for an interactive
     * [ListItem] in different states.
     *
     * @param shape the default shape of the list item.
     * @param selectedShape the shape of the list item when selected.
     * @param pressedShape the shape of the list item when pressed.
     * @param focusedShape the shape of the list item when focused.
     * @param hoveredShape the shape of the list item when hovered.
     * @param draggedShape the shape of the list item when dragged.
     */
    @Composable
    fun shapes(
        shape: Shape? = null,
        selectedShape: Shape? = null,
        pressedShape: Shape? = null,
        focusedShape: Shape? = null,
        hoveredShape: Shape? = null,
        draggedShape: Shape? = null,
    ): InteractiveListItemShapes =
        MaterialTheme.shapes.defaultInteractiveListItemShapes.copy(
            shape = shape,
            selectedShape = selectedShape,
            pressedShape = pressedShape,
            focusedShape = focusedShape,
            hoveredShape = hoveredShape,
            draggedShape = draggedShape,
        )

    /**
     * Constructor for [InteractiveListItemShapes] to be used by a [SegmentedListItem] which has an
     * [index] in a list that has a total of [count] items.
     *
     * @param index the index for this list item in the overall list.
     * @param count the total count of list items in the overall list.
     * @param defaultShapes the default [InteractiveListItemShapes] that should be used for
     *   standalone items or items in the middle of the list.
     */
    @Composable
    fun segmentedShapes(
        index: Int,
        count: Int,
        defaultShapes: InteractiveListItemShapes = shapes(),
    ): InteractiveListItemShapes {
        return remember(index, count, defaultShapes) {
            when {
                count == 1 -> defaultShapes

                index == 0 -> {
                    val defaultBaseShape = defaultShapes.shape
                    if (defaultBaseShape is CornerBasedShape) {
                        defaultShapes.copy(
                            shape = defaultBaseShape.bottom(topSize = ShapeDefaults.CornerLarge)
                        )
                    } else {
                        defaultShapes
                    }
                }

                index == count - 1 -> {
                    val defaultBaseShape = defaultShapes.shape
                    if (defaultBaseShape is CornerBasedShape) {
                        defaultShapes.copy(
                            shape = defaultBaseShape.top(bottomSize = ShapeDefaults.CornerLarge)
                        )
                    } else {
                        defaultShapes
                    }
                }

                else -> defaultShapes
            }
        }
    }

    // TODO: load tokens from component file
    internal val Shapes.defaultInteractiveListItemShapes: InteractiveListItemShapes
        get() {
            return defaultInteractiveListItemShapesCached
                ?: InteractiveListItemShapes(
                        shape = fromToken(ShapeKeyTokens.CornerExtraSmall),
                        selectedShape = fromToken(ShapeKeyTokens.CornerLarge),
                        pressedShape = fromToken(ShapeKeyTokens.CornerLarge),
                        focusedShape = fromToken(ShapeKeyTokens.CornerLarge),
                        hoveredShape = fromToken(ShapeKeyTokens.CornerLarge),
                        draggedShape = fromToken(ShapeKeyTokens.CornerLarge),
                    )
                    .also { defaultInteractiveListItemShapesCached = it }
        }

    /**
     * Creates an [InteractiveListItemElevation] that represents the elevation for an interactive
     * [ListItem] in different states.
     *
     * @param elevation the default elevation of the list item.
     * @param draggedElevation the elevation of the list item when dragged.
     */
    // TODO: load tokens from component file
    fun elevation(
        elevation: Dp = ElevationTokens.Level0,
        draggedElevation: Dp = ElevationTokens.Level4,
    ): InteractiveListItemElevation =
        InteractiveListItemElevation(elevation = elevation, draggedElevation = draggedElevation)

    // TODO: replace with token
    /** The vertical space between different [SegmentedListItem]s. */
    val SegmentedGap: Dp = 2.dp

    /**
     * Returns the default vertical alignment of children content within a [ListItem]. This is
     * equivalent to [Alignment.CenterVertically] for shorter items and [Alignment.Top] for taller
     * items.
     */
    @Composable
    fun verticalAlignment(): Alignment.Vertical {
        val density = LocalDensity.current
        return remember(density) {
            Alignment.Vertical { size, space ->
                val breakpoint =
                    with(density) { InteractiveListVerticalAlignmentBreakpoint.roundToPx() }
                val baseAlignment =
                    if (space < breakpoint) {
                        Alignment.CenterVertically
                    } else {
                        Alignment.Top
                    }
                baseAlignment.align(size, space)
            }
        }
    }
}

@Composable
private fun LeadingDecorator(
    startPadding: Dp,
    color: Color,
    textStyle: TypographyKeyTokens,
    content: (@Composable () -> Unit)?,
) {
    if (content != null) {
        Box(Modifier.padding(end = InteractiveListInternalSpacing)) {
            val horizontalPadding = startPadding + InteractiveListInternalSpacing
            // Padding contributes to content's touch target, so we can reduce enforcement value
            val mics =
                (LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                        horizontalPadding)
                    .coerceAtLeast(0.dp)
            ProvideContentColorTextStyle(
                contentColor = color,
                textStyle = textStyle.value,
                LocalMinimumInteractiveComponentSize provides mics,
                content = content,
            )
        }
    }
}

@Composable
private fun TrailingDecorator(
    endPadding: Dp,
    color: Color,
    textStyle: TypographyKeyTokens,
    content: (@Composable () -> Unit)?,
) {
    if (content != null) {
        Box(Modifier.padding(start = InteractiveListInternalSpacing)) {
            val horizontalPadding = endPadding + InteractiveListInternalSpacing
            // Padding contributes to content's touch target, so we can reduce enforcement value
            val mics =
                (LocalMinimumInteractiveComponentSize.current.takeOrElse { 0.dp } -
                        horizontalPadding)
                    .coerceAtLeast(0.dp)
            ProvideContentColorTextStyle(
                contentColor = color,
                textStyle = textStyle.value,
                LocalMinimumInteractiveComponentSize provides mics,
                content = content,
            )
        }
    }
}

@Composable
private fun OverlineDecorator(
    color: Color,
    textStyle: TypographyKeyTokens,
    content: (@Composable () -> Unit)?,
) {
    if (content != null) {
        Box {
            ProvideContentColorTextStyle(
                contentColor = color,
                textStyle = textStyle.value,
                content = content,
            )
        }
    }
}

@Composable
private fun SupportingDecorator(
    color: Color,
    textStyle: TypographyKeyTokens,
    content: (@Composable () -> Unit)?,
) {
    if (content != null) {
        Box {
            ProvideContentColorTextStyle(
                contentColor = color,
                textStyle = textStyle.value,
                content = content,
            )
        }
    }
}

@Composable
private fun ContentDecorator(
    color: Color,
    textStyle: TypographyKeyTokens,
    content: @Composable () -> Unit,
) {
    Box {
        ProvideContentColorTextStyle(
            contentColor = color,
            textStyle = textStyle.value,
            content = content,
        )
    }
}

/**
 * Equivalent to [collectIsPressedAsState], [collectIsFocusedAsState], etc. but only uses one
 * [LaunchedEffect]. The [MutableState] parameters, if provided, will be set to the corresponding
 * state value.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InteractionSource.CollectInteractionsAsState(
    pressedState: MutableState<Boolean>? = null,
    focusedState: MutableState<Boolean>? = null,
    hoveredState: MutableState<Boolean>? = null,
    draggedState: MutableState<Boolean>? = null,
) {
    LaunchedEffect(this) {
        val pressInteractions = pressedState?.let { mutableListOf<PressInteraction.Press>() }
        val focusInteractions = focusedState?.let { mutableListOf<FocusInteraction.Focus>() }
        val hoverInteractions = hoveredState?.let { mutableListOf<HoverInteraction.Enter>() }
        val dragInteractions = draggedState?.let { mutableListOf<DragInteraction.Start>() }

        interactions.collect { interaction ->
            when (interaction) {
                // press
                is PressInteraction.Press -> pressInteractions?.add(interaction)
                is PressInteraction.Release -> pressInteractions?.remove(interaction.press)
                is PressInteraction.Cancel -> pressInteractions?.remove(interaction.press)
                // focus
                is FocusInteraction.Focus -> focusInteractions?.add(interaction)
                is FocusInteraction.Unfocus -> focusInteractions?.remove(interaction.focus)
                // hover
                is HoverInteraction.Enter -> hoverInteractions?.add(interaction)
                is HoverInteraction.Exit -> hoverInteractions?.remove(interaction.enter)
                // drag
                is DragInteraction.Start -> dragInteractions?.add(interaction)
                is DragInteraction.Stop -> dragInteractions?.remove(interaction.start)
                is DragInteraction.Cancel -> dragInteractions?.remove(interaction.start)
            }
            if (pressedState != null && pressInteractions != null) {
                pressedState.value = pressInteractions.isNotEmpty()
            }
            if (focusedState != null && focusInteractions != null) {
                focusedState.value = focusInteractions.isNotEmpty()
            }
            if (hoveredState != null && hoverInteractions != null) {
                hoveredState.value = hoverInteractions.isNotEmpty()
            }
            if (draggedState != null && dragInteractions != null) {
                draggedState.value = dragInteractions.isNotEmpty()
            }
        }
    }
}

private data class InteractiveListColorState(
    val enabled: Boolean,
    val selected: Boolean,
    val dragged: Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("ComposableLambdaParameterPosition")
private fun InteractiveListItem(
    modifier: Modifier,
    content: @Composable () -> Unit,
    leadingContent: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    overlineContent: @Composable (() -> Unit)?,
    supportingContent: @Composable (() -> Unit)?,
    verticalAlignment: Alignment.Vertical,
    enabled: Boolean,
    selected: Boolean,
    applySemantics: SemanticsPropertyReceiver.() -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    interactionSource: MutableInteractionSource?,
    colors: InteractiveListItemColors,
    shapes: InteractiveListItemShapes,
    elevation: InteractiveListItemElevation,
    contentPadding: PaddingValues,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val pressed = remember { mutableStateOf(false) }
    val focused = remember { mutableStateOf(false) }
    val hovered = remember { mutableStateOf(false) }
    val dragged = remember { mutableStateOf(false) }

    interactionSource.CollectInteractionsAsState(
        pressedState = pressed,
        focusedState = focused,
        hoveredState = hovered,
        draggedState = dragged,
    )

    // TODO: Load the motionScheme tokens from the component tokens file
    val colorAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Color>()
    val shapeAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val elevationAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Dp>()

    val shape =
        shapes.shapeForInteraction(
            selected = selected,
            pressed = pressed.value,
            focused = focused.value,
            hovered = hovered.value,
            dragged = dragged.value,
            animationSpec = shapeAnimationSpec,
        )

    val colorState = InteractiveListColorState(enabled, selected, dragged.value)
    val transition = updateTransition(colorState, "ListColor")

    val containerColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.containerColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }
    val contentColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.contentColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }
    val leadingColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.leadingContentColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }
    val trailingColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.trailingContentColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }
    val overlineColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.overlineContentColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }
    val supportingColor by
        transition.animateColor(transitionSpec = { colorAnimationSpec }) { state ->
            colors.supportingContentColor(
                enabled = state.enabled,
                selected = state.selected,
                dragged = state.dragged,
            )
        }

    // TODO: load tokens from component tokens file
    val leadingTextStyle = TypographyKeyTokens.TitleMedium
    val trailingTextStyle = TypographyKeyTokens.LabelSmall
    val overlineTextStyle = TypographyKeyTokens.LabelMedium
    val supportingTextStyle = TypographyKeyTokens.BodyMedium
    val contentTextStyle = TypographyKeyTokens.BodyLarge

    val targetElevation = if (dragged.value) elevation.draggedElevation else elevation.elevation
    val shadowElevation = animateDpAsState(targetElevation, elevationAnimationSpec)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        InteractiveListItemLayout(
            modifier =
                modifier
                    .semantics(mergeDescendants = true, properties = applySemantics)
                    .minimumInteractiveComponentSize()
                    .zIndexLambda { if (shadowElevation.value > 0.dp) 1f else 0f }
                    .graphicsLayer {
                        this.shadowElevation = with(density) { shadowElevation.value.toPx() }
                        this.shape = shape
                        clip = false
                    }
                    .background(color = containerColor, shape = shape)
                    .clip(shape)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        enabled = enabled,
                        onLongClick = onLongClick,
                        onLongClickLabel = onLongClickLabel,
                        onClick = onClick,
                    )
                    .padding(contentPadding),
            verticalAlignment = verticalAlignment,
            leading = {
                LeadingDecorator(
                    startPadding = contentPadding.calculateStartPadding(layoutDirection),
                    color = leadingColor,
                    textStyle = leadingTextStyle,
                    content = leadingContent,
                )
            },
            trailing = {
                TrailingDecorator(
                    endPadding = contentPadding.calculateEndPadding(layoutDirection),
                    color = trailingColor,
                    textStyle = trailingTextStyle,
                    content = trailingContent,
                )
            },
            overline = {
                OverlineDecorator(
                    color = overlineColor,
                    textStyle = overlineTextStyle,
                    content = overlineContent,
                )
            },
            supporting = {
                SupportingDecorator(
                    color = supportingColor,
                    textStyle = supportingTextStyle,
                    content = supportingContent,
                )
            },
            content = {
                ContentDecorator(
                    color = contentColor,
                    textStyle = contentTextStyle,
                    content = content,
                )
            },
        )
    }
}

@Composable
private fun InteractiveListItemLayout(
    modifier: Modifier,
    verticalAlignment: Alignment.Vertical,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    overline: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val measurePolicy =
        remember(verticalAlignment) {
            InteractiveListItemMeasurePolicy(verticalAlignment = verticalAlignment)
        }
    Layout(
        modifier = modifier,
        contents = listOf(leading, trailing, overline, supporting, content),
        measurePolicy = measurePolicy,
    )
}

private class InteractiveListItemMeasurePolicy(val verticalAlignment: Alignment.Vertical) :
    MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            contentMeasurable,
        ) = measurables

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        var constraintOffsetX = 0
        var constraintOffsetY = 0

        val leadingPlaceable = leadingMeasurable.firstOrNull()?.measure(looseConstraints)
        constraintOffsetX += leadingPlaceable.widthOrZero

        val trailingPlaceable =
            trailingMeasurable
                .firstOrNull()
                ?.measure(looseConstraints.offset(horizontal = -constraintOffsetX))
        constraintOffsetX += trailingPlaceable.widthOrZero

        val overlinePlaceable =
            overlineMeasurable
                .firstOrNull()
                ?.measure(looseConstraints.offset(horizontal = -constraintOffsetX))
        constraintOffsetY += overlinePlaceable.heightOrZero

        val contentPlaceable =
            contentMeasurable
                .firstOrNull()
                ?.measure(
                    looseConstraints.offset(
                        horizontal = -constraintOffsetX,
                        vertical = -constraintOffsetY,
                    )
                )
        constraintOffsetY += contentPlaceable.heightOrZero

        val supportingPlaceable =
            supportingMeasurable
                .firstOrNull()
                ?.measure(
                    looseConstraints.offset(
                        horizontal = -constraintOffsetX,
                        vertical = -constraintOffsetY,
                    )
                )

        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                overlineWidth = overlinePlaceable.widthOrZero,
                supportingWidth = supportingPlaceable.widthOrZero,
                contentWidth = contentPlaceable.widthOrZero,
                constraints = constraints,
            )
        val height =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                overlineHeight = overlinePlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                contentHeight = contentPlaceable.heightOrZero,
                constraints = constraints,
            )

        return place(
            width = width,
            height = height,
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            contentPlaceable = contentPlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::maxIntrinsicHeight)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::maxIntrinsicWidth)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
    ): Int = calculateIntrinsicHeight(measurables, width, IntrinsicMeasurable::minIntrinsicHeight)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
    ): Int = calculateIntrinsicWidth(measurables, height, IntrinsicMeasurable::minIntrinsicWidth)

    private fun calculateIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
        intrinsicMeasure: IntrinsicMeasurable.(height: Int) -> Int,
    ): Int {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            contentMeasurable,
        ) = measurables

        return calculateWidth(
            leadingWidth = leadingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            trailingWidth = trailingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            overlineWidth = overlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            supportingWidth = supportingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            contentWidth = contentMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            constraints = Constraints(),
        )
    }

    private fun calculateIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
        intrinsicMeasure: IntrinsicMeasurable.(width: Int) -> Int,
    ): Int {
        val (
            leadingMeasurable,
            trailingMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            contentMeasurable,
        ) = measurables

        var remainingWidth = width

        val leadingHeight =
            leadingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val trailingHeight =
            trailingMeasurable.firstOrNull()?.let {
                val height = it.intrinsicMeasure(remainingWidth)
                remainingWidth =
                    remainingWidth.subtractConstraintSafely(
                        it.maxIntrinsicWidth(Constraints.Infinity)
                    )
                height
            } ?: 0
        val overlineHeight = overlineMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val supportingHeight =
            supportingMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val contentHeight = contentMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            overlineHeight = overlineHeight,
            supportingHeight = supportingHeight,
            contentHeight = contentHeight,
            constraints = Constraints(),
        )
    }

    private fun MeasureScope.place(
        width: Int,
        height: Int,
        leadingPlaceable: Placeable?,
        trailingPlaceable: Placeable?,
        contentPlaceable: Placeable?,
        overlinePlaceable: Placeable?,
        supportingPlaceable: Placeable?,
    ): MeasureResult {
        return layout(width, height) {
            leadingPlaceable?.placeRelative(
                x = 0,
                y = verticalAlignment.align(leadingPlaceable.height, height),
            )

            val mainContentX = leadingPlaceable.widthOrZero
            val mainContentTotalHeight =
                contentPlaceable.heightOrZero +
                    overlinePlaceable.heightOrZero +
                    supportingPlaceable.heightOrZero
            val mainContentY = verticalAlignment.align(mainContentTotalHeight, height)
            var currY = mainContentY

            overlinePlaceable?.placeRelative(mainContentX, currY)
            currY += overlinePlaceable.heightOrZero

            contentPlaceable?.placeRelative(mainContentX, currY)
            currY += contentPlaceable.heightOrZero

            supportingPlaceable?.placeRelative(mainContentX, currY)

            trailingPlaceable?.placeRelative(
                x = width - trailingPlaceable.width,
                y = verticalAlignment.align(trailingPlaceable.height, height),
            )
        }
    }

    private fun calculateWidth(
        leadingWidth: Int,
        trailingWidth: Int,
        overlineWidth: Int,
        supportingWidth: Int,
        contentWidth: Int,
        constraints: Constraints,
    ): Int {
        if (constraints.hasBoundedWidth) {
            return constraints.maxWidth
        }
        // Fallback behavior if width constraints are infinite
        val mainContentWidth = maxOf(contentWidth, overlineWidth, supportingWidth)
        return leadingWidth + mainContentWidth + trailingWidth
    }

    private fun calculateHeight(
        leadingHeight: Int,
        trailingHeight: Int,
        overlineHeight: Int,
        supportingHeight: Int,
        contentHeight: Int,
        constraints: Constraints,
    ): Int {
        val mainContentHeight = contentHeight + overlineHeight + supportingHeight

        return constraints.constrainHeight(maxOf(leadingHeight, mainContentHeight, trailingHeight))
    }
}

private fun Modifier.zIndexLambda(zIndex: FloatProducer): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0, zIndex = zIndex()) }
    }

// TODO: replace with tokens
internal val InteractiveListStartPadding = 16.dp
internal val InteractiveListEndPadding = 16.dp
internal val InteractiveListTopPadding = 12.dp
internal val InteractiveListBottomPadding = 12.dp
internal val InteractiveListInternalSpacing = 12.dp
internal val InteractiveListDisabledOpacity = 0.38f
/**
 * How tall a list item needs to be before internal content is top-aligned instead of
 * center-aligned.
 */
internal val InteractiveListVerticalAlignmentBreakpoint =
    80.dp - InteractiveListTopPadding - InteractiveListBottomPadding
