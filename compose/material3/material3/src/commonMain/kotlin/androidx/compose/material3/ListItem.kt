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

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.animateColor
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.heightOrZero
import androidx.compose.material3.internal.subtractConstraintSafely
import androidx.compose.material3.internal.widthOrZero
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import kotlin.jvm.JvmInline
import kotlin.math.max

/**
 * [Material Design list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of list item does not handle any user interaction. See other overloads for handling
 * general click actions, single-selection, or multi-selection.
 *
 * ![Lists
 * image](https://developer.android.com/images/reference/androidx/compose/material3/lists.png)
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text. For example:
 * - one-line item
 *
 * @sample androidx.compose.material3.samples.OneLineListItem
 * - two-line item
 *
 * @sample androidx.compose.material3.samples.TwoLineListItem
 * - three-line item with both overline and supporting content
 *
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithOverlineAndSupporting
 * - three-line item with extended supporting content
 *
 * @sample androidx.compose.material3.samples.ThreeLineListItemWithExtendedSupporting
 * @param headlineContent the headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param overlineContent the content displayed above the headline content
 * @param supportingContent the supporting content of the list item
 * @param leadingContent the leading content of the list item
 * @param trailingContent the trailing meta text, icon, switch or checkbox
 * @param colors [ListItemColors] that will be used to resolve the background and content color for
 *   this list item in different states. See [ListItemDefaults.colors]
 * @param tonalElevation the tonal elevation of this list item
 * @param shadowElevation the shadow elevation of this list item
 */
@Composable
fun ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    colors: ListItemColors = ListItemDefaults.colors(),
    tonalElevation: Dp = ListItemDefaults.Elevation,
    shadowElevation: Dp = ListItemDefaults.Elevation,
) {
    val decoratedHeadlineContent: @Composable () -> Unit = {
        ProvideTextStyleFromToken(
            colors.contentColor(enabled = true, selected = false, dragged = false),
            ListTokens.ItemLabelTextFont,
            headlineContent,
        )
    }
    val decoratedSupportingContent: @Composable (() -> Unit)? =
        supportingContent?.let {
            {
                ProvideTextStyleFromToken(
                    colors.supportingContentColor(
                        enabled = true,
                        selected = false,
                        dragged = false,
                    ),
                    ListTokens.ItemSupportingTextFont,
                    it,
                )
            }
        }
    val decoratedOverlineContent: @Composable (() -> Unit)? =
        overlineContent?.let {
            {
                ProvideTextStyleFromToken(
                    colors.overlineContentColor(enabled = true, selected = false, dragged = false),
                    ListTokens.ItemOverlineFont,
                    it,
                )
            }
        }
    val decoratedLeadingContent: @Composable (() -> Unit)? =
        leadingContent?.let {
            {
                Box(Modifier.padding(end = LeadingContentEndPadding)) {
                    CompositionLocalProvider(
                        LocalContentColor provides
                            colors.leadingContentColor(
                                enabled = true,
                                selected = false,
                                dragged = false,
                            ),
                        content = it,
                    )
                }
            }
        }
    val decoratedTrailingContent: @Composable (() -> Unit)? =
        trailingContent?.let {
            {
                Box(Modifier.padding(start = TrailingContentStartPadding)) {
                    ProvideTextStyleFromToken(
                        colors.trailingContentColor(
                            enabled = true,
                            selected = false,
                            dragged = false,
                        ),
                        ListTokens.ItemTrailingSupportingTextFont,
                        content = it,
                    )
                }
            }
        }

    Surface(
        modifier = Modifier.semantics(mergeDescendants = true) {}.then(modifier),
        shape = ListItemDefaults.shape,
        color = colors.containerColor(enabled = true, selected = false, dragged = false),
        contentColor = colors.contentColor(enabled = true, selected = false, dragged = false),
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
    ) {
        ListItemLayout(
            headline = decoratedHeadlineContent,
            overline = decoratedOverlineContent,
            supporting = decoratedSupportingContent,
            leading = decoratedLeadingContent,
            trailing = decoratedTrailingContent,
        )
    }
}

/**
 * [Material Design standard list item](https://m3.material.io/components/lists/overview)
 *
 * Lists are continuous, vertical indexes of text or images.
 *
 * This overload of [ListItem] handles click events, calling its [onClick] lambda to trigger an
 * action. See other overloads for handling single-selection, multi-selection, or no interaction
 * handling.
 *
 * @sample androidx.compose.material3.samples.ClickableListItemSample
 * @sample androidx.compose.material3.samples.ClickableListItemWithClickableChildSample
 * @sample androidx.compose.material3.samples.ListItemWithModeChangeOnLongClickSample
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
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. See [ListItemDefaults.shapes].
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.colors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ListItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    colors: ListItemColors = ListItemDefaults.colors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
 * @sample androidx.compose.material3.samples.SingleSelectionListItemSample
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
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. See [ListItemDefaults.shapes].
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.colors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ListItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    colors: ListItemColors = ListItemDefaults.colors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
        applySemantics = {
            this.selected = selected
            role = Role.RadioButton
        },
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
 * @sample androidx.compose.material3.samples.MultiSelectionListItemSample
 * @sample androidx.compose.material3.samples.ListItemWithModeChangeOnLongClickSample
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
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. See [ListItemDefaults.shapes].
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.colors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    shapes: ListItemShapes = ListItemDefaults.shapes(),
    colors: ListItemColors = ListItemDefaults.colors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
        applySemantics = {
            toggleableState = ToggleableState(checked)
            role = Role.Checkbox
        },
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
 * @sample androidx.compose.material3.samples.SegmentedListItemWithExpansionSample
 * @param onClick called when this list item is clicked.
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. The base shape depends on the index of the item within
 *   the overall list. See [ListItemDefaults.segmentedShapes].
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
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.segmentedColors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SegmentedListItem(
    onClick: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
 * @sample androidx.compose.material3.samples.SingleSelectionSegmentedListItemSample
 * @param selected whether or not this list item is selected.
 * @param onClick called when this list item is clicked.
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. The base shape depends on the index of the item within
 *   the overall list. See [ListItemDefaults.segmentedShapes].
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
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.segmentedColors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SegmentedListItem(
    selected: Boolean,
    onClick: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
        applySemantics = {
            this.selected = selected
            role = Role.RadioButton
        },
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
 * @sample androidx.compose.material3.samples.MultiSelectionSegmentedListItemSample
 * @param checked whether this list item is toggled on or off.
 * @param onCheckedChange called when this toggleable list item is clicked.
 * @param shapes the [ListItemShapes] that this list item will use to morph between depending on the
 *   user's interaction with the list item. The base shape depends on the index of the item within
 *   the overall list. See [ListItemDefaults.segmentedShapes].
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
 * @param colors the [ListItemColors] that will be used to resolve the colors used for this list
 *   item in different states. See [ListItemDefaults.segmentedColors].
 * @param elevation the [ListItemElevation] used to resolve the elevation for this list item in
 *   different states. See [ListItemDefaults.elevation].
 * @param contentPadding the padding to be applied to the content of this list item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the main content of this list item. Also known as the headline or label.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun SegmentedListItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    verticalAlignment: Alignment.Vertical = ListItemDefaults.verticalAlignment(),
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    colors: ListItemColors = ListItemDefaults.segmentedColors(),
    elevation: ListItemElevation = ListItemDefaults.elevation(),
    contentPadding: PaddingValues = ListItemDefaults.ContentPadding,
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
        applySemantics = {
            toggleableState = ToggleableState(checked)
            role = Role.Checkbox
        },
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

@Composable
private fun ListItemLayout(
    leading: @Composable (() -> Unit)?,
    trailing: @Composable (() -> Unit)?,
    headline: @Composable () -> Unit,
    overline: @Composable (() -> Unit)?,
    supporting: @Composable (() -> Unit)?,
) {
    val measurePolicy = remember { ListItemMeasurePolicy() }
    Layout(
        contents =
            listOf(headline, overline ?: {}, supporting ?: {}, leading ?: {}, trailing ?: {}),
        measurePolicy = measurePolicy,
    )
}

private class ListItemMeasurePolicy : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables
        var currentTotalWidth = 0

        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val startPadding = ListItemStartPadding
        val endPadding = ListItemEndPadding
        val horizontalPadding = (startPadding + endPadding).roundToPx()

        // ListItem layout has a cycle in its dependencies which we use
        // intrinsic measurements to break:
        // 1. Intrinsic leading/trailing width
        // 2. Intrinsic supporting height
        // 3. Intrinsic vertical padding
        // 4. Actual leading/trailing measurement
        // 5. Actual supporting measurement
        // 6. Actual vertical padding
        val intrinsicLeadingWidth =
            leadingMeasurable.firstOrNull()?.minIntrinsicWidth(constraints.maxHeight) ?: 0
        val intrinsicTrailingWidth =
            trailingMeasurable.firstOrNull()?.minIntrinsicWidth(constraints.maxHeight) ?: 0
        val intrinsicSupportingWidthConstraint =
            looseConstraints.maxWidth.subtractConstraintSafely(
                intrinsicLeadingWidth + intrinsicTrailingWidth + horizontalPadding
            )
        val intrinsicSupportingHeight =
            supportingMeasurable
                .firstOrNull()
                ?.minIntrinsicHeight(intrinsicSupportingWidthConstraint) ?: 0
        val intrinsicIsSupportingMultiline =
            isSupportingMultilineHeuristic(intrinsicSupportingHeight)
        val intrinsicListItemType =
            ListItemType(
                hasOverline = overlineMeasurable.firstOrNull() != null,
                hasSupporting = supportingMeasurable.firstOrNull() != null,
                isSupportingMultiline = intrinsicIsSupportingMultiline,
            )
        val intrinsicVerticalPadding = (verticalPadding(intrinsicListItemType) * 2).roundToPx()

        val paddedLooseConstraints =
            looseConstraints.offset(
                horizontal = -horizontalPadding,
                vertical = -intrinsicVerticalPadding,
            )

        val leadingPlaceable = leadingMeasurable.firstOrNull()?.measure(paddedLooseConstraints)
        currentTotalWidth += leadingPlaceable.widthOrZero

        val trailingPlaceable =
            trailingMeasurable
                .firstOrNull()
                ?.measure(paddedLooseConstraints.offset(horizontal = -currentTotalWidth))
        currentTotalWidth += trailingPlaceable.widthOrZero

        var currentTotalHeight = 0

        val headlinePlaceable =
            headlineMeasurable
                .firstOrNull()
                ?.measure(paddedLooseConstraints.offset(horizontal = -currentTotalWidth))
        currentTotalHeight += headlinePlaceable.heightOrZero

        val supportingPlaceable =
            supportingMeasurable
                .firstOrNull()
                ?.measure(
                    paddedLooseConstraints.offset(
                        horizontal = -currentTotalWidth,
                        vertical = -currentTotalHeight,
                    )
                )
        currentTotalHeight += supportingPlaceable.heightOrZero
        val isSupportingMultiline =
            supportingPlaceable != null &&
                (supportingPlaceable[FirstBaseline] != supportingPlaceable[LastBaseline])

        val overlinePlaceable =
            overlineMeasurable
                .firstOrNull()
                ?.measure(
                    paddedLooseConstraints.offset(
                        horizontal = -currentTotalWidth,
                        vertical = -currentTotalHeight,
                    )
                )

        val listItemType =
            ListItemType(
                hasOverline = overlinePlaceable != null,
                hasSupporting = supportingPlaceable != null,
                isSupportingMultiline = isSupportingMultiline,
            )
        val topPadding = verticalPadding(listItemType)
        val verticalPadding = topPadding * 2

        val width =
            calculateWidth(
                leadingWidth = leadingPlaceable.widthOrZero,
                trailingWidth = trailingPlaceable.widthOrZero,
                headlineWidth = headlinePlaceable.widthOrZero,
                overlineWidth = overlinePlaceable.widthOrZero,
                supportingWidth = supportingPlaceable.widthOrZero,
                horizontalPadding = horizontalPadding,
                constraints = constraints,
            )
        val height =
            calculateHeight(
                leadingHeight = leadingPlaceable.heightOrZero,
                trailingHeight = trailingPlaceable.heightOrZero,
                headlineHeight = headlinePlaceable.heightOrZero,
                overlineHeight = overlinePlaceable.heightOrZero,
                supportingHeight = supportingPlaceable.heightOrZero,
                listItemType = listItemType,
                verticalPadding = verticalPadding.roundToPx(),
                constraints = constraints,
            )

        return place(
            width = width,
            height = height,
            leadingPlaceable = leadingPlaceable,
            trailingPlaceable = trailingPlaceable,
            headlinePlaceable = headlinePlaceable,
            overlinePlaceable = overlinePlaceable,
            supportingPlaceable = supportingPlaceable,
            isThreeLine = listItemType == ListItemType.ThreeLine,
            startPadding = startPadding.roundToPx(),
            endPadding = endPadding.roundToPx(),
            topPadding = topPadding.roundToPx(),
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

    private fun IntrinsicMeasureScope.calculateIntrinsicWidth(
        measurables: List<List<IntrinsicMeasurable>>,
        height: Int,
        intrinsicMeasure: IntrinsicMeasurable.(height: Int) -> Int,
    ): Int {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables
        return calculateWidth(
            leadingWidth = leadingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            trailingWidth = trailingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            headlineWidth = headlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            overlineWidth = overlineMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            supportingWidth = supportingMeasurable.firstOrNull()?.intrinsicMeasure(height) ?: 0,
            horizontalPadding = (ListItemStartPadding + ListItemEndPadding).roundToPx(),
            constraints = Constraints(),
        )
    }

    private fun IntrinsicMeasureScope.calculateIntrinsicHeight(
        measurables: List<List<IntrinsicMeasurable>>,
        width: Int,
        intrinsicMeasure: IntrinsicMeasurable.(width: Int) -> Int,
    ): Int {
        val (
            headlineMeasurable,
            overlineMeasurable,
            supportingMeasurable,
            leadingMeasurable,
            trailingMeasurable) =
            measurables

        var remainingWidth =
            width.subtractConstraintSafely((ListItemStartPadding + ListItemEndPadding).roundToPx())
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
        val headlineHeight = headlineMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val supportingHeight =
            supportingMeasurable.firstOrNull()?.intrinsicMeasure(remainingWidth) ?: 0
        val isSupportingMultiline = isSupportingMultilineHeuristic(supportingHeight)
        val listItemType =
            ListItemType(
                hasOverline = overlineHeight > 0,
                hasSupporting = supportingHeight > 0,
                isSupportingMultiline = isSupportingMultiline,
            )

        return calculateHeight(
            leadingHeight = leadingHeight,
            trailingHeight = trailingHeight,
            headlineHeight = headlineHeight,
            overlineHeight = overlineHeight,
            supportingHeight = supportingHeight,
            listItemType = listItemType,
            verticalPadding = (verticalPadding(listItemType) * 2).roundToPx(),
            constraints = Constraints(),
        )
    }
}

private fun IntrinsicMeasureScope.calculateWidth(
    leadingWidth: Int,
    trailingWidth: Int,
    headlineWidth: Int,
    overlineWidth: Int,
    supportingWidth: Int,
    horizontalPadding: Int,
    constraints: Constraints,
): Int {
    if (constraints.hasBoundedWidth) {
        return constraints.maxWidth
    }
    // Fallback behavior if width constraints are infinite
    val mainContentWidth = maxOf(headlineWidth, overlineWidth, supportingWidth)
    return horizontalPadding + leadingWidth + mainContentWidth + trailingWidth
}

private fun IntrinsicMeasureScope.calculateHeight(
    leadingHeight: Int,
    trailingHeight: Int,
    headlineHeight: Int,
    overlineHeight: Int,
    supportingHeight: Int,
    listItemType: ListItemType,
    verticalPadding: Int,
    constraints: Constraints,
): Int {
    val defaultMinHeight =
        when (listItemType) {
            ListItemType.OneLine -> ListTokens.ItemOneLineContainerHeight
            ListItemType.TwoLine -> ListTokens.ItemTwoLineContainerHeight
            else /* ListItemType.ThreeLine */ -> ListTokens.ItemThreeLineContainerHeight
        }
    val minHeight = max(constraints.minHeight, defaultMinHeight.roundToPx())

    val mainContentHeight = headlineHeight + overlineHeight + supportingHeight

    return max(minHeight, verticalPadding + maxOf(leadingHeight, mainContentHeight, trailingHeight))
        .coerceAtMost(constraints.maxHeight)
}

private fun MeasureScope.place(
    width: Int,
    height: Int,
    leadingPlaceable: Placeable?,
    trailingPlaceable: Placeable?,
    headlinePlaceable: Placeable?,
    overlinePlaceable: Placeable?,
    supportingPlaceable: Placeable?,
    isThreeLine: Boolean,
    startPadding: Int,
    endPadding: Int,
    topPadding: Int,
): MeasureResult {
    return layout(width, height) {
        leadingPlaceable?.let {
            it.placeRelative(
                x = startPadding,
                y = if (isThreeLine) topPadding else CenterVertically.align(it.height, height),
            )
        }

        val mainContentX = startPadding + leadingPlaceable.widthOrZero
        val mainContentY =
            if (isThreeLine) {
                topPadding
            } else {
                val totalHeight =
                    headlinePlaceable.heightOrZero +
                        overlinePlaceable.heightOrZero +
                        supportingPlaceable.heightOrZero
                CenterVertically.align(totalHeight, height)
            }
        var currentY = mainContentY

        overlinePlaceable?.placeRelative(mainContentX, currentY)
        currentY += overlinePlaceable.heightOrZero

        headlinePlaceable?.placeRelative(mainContentX, currentY)
        currentY += headlinePlaceable.heightOrZero

        supportingPlaceable?.placeRelative(mainContentX, currentY)

        trailingPlaceable?.let {
            it.placeRelative(
                x = width - endPadding - it.width,
                y = if (isThreeLine) topPadding else CenterVertically.align(it.height, height),
            )
        }
    }
}

@Composable
private fun ProvideTextStyleFromToken(
    color: Color,
    textToken: TypographyKeyTokens,
    content: @Composable () -> Unit,
) =
    ProvideContentColorTextStyle(
        contentColor = color,
        textStyle = textToken.value,
        content = content,
    )

/** Helper class to define list item type. Used for padding and sizing definition. */
@JvmInline
private value class ListItemType private constructor(private val lines: Int) :
    Comparable<ListItemType> {

    override operator fun compareTo(other: ListItemType) = lines.compareTo(other.lines)

    companion object {
        /** One line list item */
        val OneLine = ListItemType(1)

        /** Two line list item */
        val TwoLine = ListItemType(2)

        /** Three line list item */
        val ThreeLine = ListItemType(3)

        internal operator fun invoke(
            hasOverline: Boolean,
            hasSupporting: Boolean,
            isSupportingMultiline: Boolean,
        ): ListItemType {
            return when {
                (hasOverline && hasSupporting) || isSupportingMultiline -> ThreeLine
                hasOverline || hasSupporting -> TwoLine
                else -> OneLine
            }
        }
    }
}

// Container related defaults
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting internal val ListItemVerticalPadding = 8.dp

@VisibleForTesting internal val ListItemThreeLineVerticalPadding = 12.dp

@VisibleForTesting internal val ListItemStartPadding = 16.dp

@VisibleForTesting internal val ListItemEndPadding = 16.dp

// Icon related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting internal val LeadingContentEndPadding = 16.dp

// Trailing related defaults.
// TODO: Make sure these values stay up to date until replaced with tokens.
@VisibleForTesting internal val TrailingContentStartPadding = 16.dp

// In the actual layout phase, we can query supporting baselines,
// but for an intrinsic measurement pass, we have to estimate.
private fun Density.isSupportingMultilineHeuristic(estimatedSupportingHeight: Int): Boolean =
    estimatedSupportingHeight > 30.sp.roundToPx()

private fun verticalPadding(listItemType: ListItemType): Dp =
    when (listItemType) {
        ListItemType.ThreeLine -> ListItemThreeLineVerticalPadding
        else -> ListItemVerticalPadding
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
    colors: ListItemColors,
    shapes: ListItemShapes,
    elevation: ListItemElevation,
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

    val leadingTextStyle = ListTokens.ItemLeadingAvatarLabelFont
    val trailingTextStyle = ListTokens.ItemTrailingSupportingTextFont
    val overlineTextStyle = ListTokens.ItemOverlineFont
    val supportingTextStyle = ListTokens.ItemSupportingTextFont
    val contentTextStyle = ListTokens.ItemLabelTextFont

    val targetElevation = if (dragged.value) elevation.draggedElevation else elevation.elevation
    val shadowElevation = animateDpAsState(targetElevation, elevationAnimationSpec)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        InteractiveListItemLayout(
            modifier =
                modifier
                    .semantics(mergeDescendants = true, properties = applySemantics)
                    .defaultMinSize(minHeight = ListTokens.ItemOneLineContainerHeight)
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

internal val InteractiveListStartPadding = ListTokens.ItemLeadingSpace
internal val InteractiveListEndPadding = ListTokens.ItemTrailingSpace
internal val InteractiveListTopPadding = ListTokens.ItemTopSpace
internal val InteractiveListBottomPadding = ListTokens.ItemBottomSpace
internal val InteractiveListInternalSpacing = ListTokens.ItemBetweenSpace

/**
 * How tall a list item (excluding padding) needs to be before internal content is top-aligned
 * instead of center-aligned.
 */
internal val InteractiveListVerticalAlignmentBreakpoint =
    (ListTokens.ItemThreeLineContainerHeight + ListTokens.ItemTwoLineContainerHeight) / 2 -
        InteractiveListTopPadding -
        InteractiveListBottomPadding
