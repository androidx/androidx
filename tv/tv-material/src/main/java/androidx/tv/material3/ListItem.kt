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

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Lists are continuous, vertical indexes of text or images.
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text.
 *
 * This ListItem handles click events, calling its [onClick] lambda. It also support selected state
 * which can be toggled using the [selected] param.
 *
 * @param selected defines whether this ListItem is selected or not
 * @param onClick called when this ListItem is clicked
 * @param headlineContent the [Composable] headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onLongClick called when this ListItem is long clicked (long-pressed).
 * @param leadingContent the [Composable] leading content of the list item
 * @param overlineContent the [Composable] content displayed above the headline content
 * @param supportingContent the [Composable] content displayed below the headline content
 * @param trailingContent the [Composable] trailing meta text, icon, switch or checkbox
 * @param tonalElevation the tonal elevation of this list item
 * @param shape [ListItemShape] defines the shape of ListItem's container in different interaction
 *   states. See [ListItemDefaults.shape].
 * @param colors [ListItemColors] defines the background and content colors used in the list item
 *   for different interaction states. See [ListItemDefaults.colors]
 * @param scale [ListItemScale] defines the size of the list item relative to its original size in
 *   different interaction states. See [ListItemDefaults.scale]
 * @param border [ListItemBorder] defines a border around the list item in different interaction
 *   states. See [ListItemDefaults.border]
 * @param glow [ListItemGlow] defines a shadow to be shown behind the list item for different
 *   interaction states. See [ListItemDefaults.glow]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun ListItem(
    selected: Boolean,
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = ListItemDefaults.TonalElevation,
    shape: ListItemShape = ListItemDefaults.shape(),
    colors: ListItemColors = ListItemDefaults.colors(),
    scale: ListItemScale = ListItemDefaults.scale(),
    border: ListItemBorder = ListItemDefaults.border(),
    glow: ListItemGlow = ListItemDefaults.glow(),
    interactionSource: MutableInteractionSource? = null
) {
    BaseListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = headlineContent,
        contentPadding = ListItemDefaults.ContentPadding,
        modifier = modifier,
        enabled = enabled,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        tonalElevation = tonalElevation,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight =
            listItemMinHeight(
                hasLeadingContent = leadingContent != null,
                hasSupportingContent = supportingContent != null,
                hasOverlineContent = overlineContent != null
            ),
        minIconSize = ListItemDefaults.IconSize,
        headlineTextStyle = MaterialTheme.typography.titleMedium,
        trailingTextStyle = MaterialTheme.typography.labelLarge,
        interactionSource = interactionSource
    )
}

/**
 * Lists are continuous, vertical indexes of text or images.
 *
 * [DenseListItem] is a smaller/denser version of the Material [ListItem].
 *
 * This component can be used to achieve the list item templates existing in the spec. One-line list
 * items have a singular line of headline content. Two-line list items additionally have either
 * supporting or overline content. Three-line list items have either both supporting and overline
 * content, or extended (two-line) supporting text.
 *
 * This ListItem handles click events, calling its [onClick] lambda. It also support selected state
 * which can be toggled using the [selected] param.
 *
 * @param selected defines whether this ListItem is selected or not
 * @param onClick called when this ListItem is clicked
 * @param headlineContent the [Composable] headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onLongClick called when this ListItem is long clicked (long-pressed).
 * @param leadingContent the [Composable] leading content of the list item
 * @param overlineContent the [Composable] content displayed above the headline content
 * @param supportingContent the [Composable] content displayed below the headline content
 * @param trailingContent the [Composable] trailing meta text, icon, switch or checkbox
 * @param tonalElevation the tonal elevation of this list item
 * @param shape [ListItemShape] defines the shape of ListItem's container in different interaction
 *   states. See [ListItemDefaults.shape].
 * @param colors [ListItemColors] defines the background and content colors used in the list item
 *   for different interaction states. See [ListItemDefaults.colors]
 * @param scale [ListItemScale] defines the size of the list item relative to its original size in
 *   different interaction states. See [ListItemDefaults.scale]
 * @param border [ListItemBorder] defines a border around the list item in different interaction
 *   states. See [ListItemDefaults.border]
 * @param glow [ListItemGlow] defines a shadow to be shown behind the list item for different
 *   interaction states. See [ListItemDefaults.glow]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun DenseListItem(
    selected: Boolean,
    onClick: () -> Unit,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    tonalElevation: Dp = ListItemDefaults.TonalElevation,
    shape: ListItemShape = ListItemDefaults.shape(),
    colors: ListItemColors = ListItemDefaults.colors(),
    scale: ListItemScale = ListItemDefaults.scale(),
    border: ListItemBorder = ListItemDefaults.border(),
    glow: ListItemGlow = ListItemDefaults.glow(),
    interactionSource: MutableInteractionSource? = null
) {
    BaseListItem(
        selected = selected,
        onClick = onClick,
        headlineContent = headlineContent,
        contentPadding = ListItemDefaults.ContentPaddingDense,
        modifier = modifier,
        enabled = enabled,
        onLongClick = onLongClick,
        leadingContent = leadingContent,
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        trailingContent = trailingContent,
        tonalElevation = tonalElevation,
        shape = shape,
        colors = colors,
        scale = scale,
        border = border,
        glow = glow,
        minHeight =
            listItemMinHeight(
                hasLeadingContent = leadingContent != null,
                hasSupportingContent = supportingContent != null,
                hasOverlineContent = overlineContent != null,
                dense = true
            ),
        minIconSize = ListItemDefaults.IconSizeDense,
        headlineTextStyle = MaterialTheme.typography.titleSmall,
        trailingTextStyle = MaterialTheme.typography.labelSmall,
        interactionSource = interactionSource
    )
}

/**
 * Base composable for [ListItem]
 *
 * @param selected defines whether this ListItem is selected or not
 * @param onClick called when this ListItem is clicked
 * @param contentPadding [PaddingValues] defines the inner padding applied to the ListItem's content
 * @param headlineContent the [Composable] headline content of the list item
 * @param modifier [Modifier] to be applied to the list item
 * @param enabled controls the enabled state of this list item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param onLongClick called when this ListItem is long clicked (long-pressed).
 * @param leadingContent the [Composable] leading content of the list item
 * @param overlineContent the [Composable] content displayed above the headline content
 * @param supportingContent the [Composable] content displayed below the headline content
 * @param trailingContent the [Composable] trailing meta text, icon, switch or checkbox
 * @param tonalElevation the tonal elevation of this list item
 * @param shape [ListItemShape] defines the shape of ListItem's container in different interaction
 *   states.
 * @param colors [ListItemColors] defines the background and content colors used in the list item
 *   for different interaction states.
 * @param scale [ListItemScale] defines the size of the list item relative to its original size in
 *   different interaction states.
 * @param border [ListItemBorder] defines a border around the list item in different interaction
 *   states.
 * @param glow [ListItemGlow] defines a shadow to be shown behind the list item for different
 *   interaction states.
 * @param minHeight defines the minimum height [Dp] for the ListItem.
 * @param minIconSize defines the minimum icon size [Dp] to be used in leading content.
 * @param headlineTextStyle defines the [TextStyle] for the headline content.
 * @param trailingTextStyle defines the [TextStyle] for the trailing content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this list item. You can use this to change the list item's
 *   appearance or preview the list item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
private fun BaseListItem(
    selected: Boolean,
    onClick: () -> Unit,
    contentPadding: PaddingValues,
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)?,
    overlineContent: (@Composable () -> Unit)?,
    supportingContent: (@Composable () -> Unit)?,
    leadingContent: (@Composable BoxScope.() -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
    tonalElevation: Dp,
    shape: ListItemShape,
    colors: ListItemColors,
    scale: ListItemScale,
    border: ListItemBorder,
    glow: ListItemGlow,
    minHeight: Dp,
    minIconSize: Dp,
    headlineTextStyle: TextStyle,
    trailingTextStyle: TextStyle,
    interactionSource: MutableInteractionSource?
) {
    val semanticModifier =
        Modifier.semantics(mergeDescendants = true) { this.selected = selected }.then(modifier)

    Surface(
        selected = selected,
        onClick = onClick,
        modifier = semanticModifier,
        enabled = enabled,
        onLongClick = onLongClick,
        tonalElevation = tonalElevation,
        shape = shape.toSelectableSurfaceShape(),
        colors = colors.toSelectableSurfaceColors(),
        scale = scale.toSelectableSurfaceScale(),
        border = border.toSelectableSurfaceBorder(),
        glow = glow.toSelectableSurfaceGlow(),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier.defaultMinSize(minHeight = minHeight).padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let {
                CompositionLocalProvider(
                    LocalContentColor provides ListItemDefaults.LeadingContentColor
                ) {
                    Box(
                        modifier =
                            Modifier.defaultMinSize(
                                minWidth = minIconSize,
                                minHeight = minIconSize
                            ),
                        contentAlignment = Alignment.Center,
                        content = it
                    )
                }
                Spacer(modifier = Modifier.padding(end = ListItemDefaults.LeadingContentEndPadding))
            }

            Box(Modifier.weight(1f).align(Alignment.CenterVertically)) {
                Column {
                    overlineContent?.let {
                        CompositionLocalProvider(
                            LocalContentColor provides
                                LocalContentColor.current.copy(
                                    alpha = ListItemDefaults.OverlineContentOpacity
                                )
                        ) {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.labelSmall,
                                content = it
                            )
                        }
                    }

                    ProvideTextStyle(value = headlineTextStyle, content = headlineContent)

                    supportingContent?.let {
                        CompositionLocalProvider(
                            LocalContentColor provides
                                LocalContentColor.current.copy(
                                    alpha = ListItemDefaults.SupportingContentOpacity
                                )
                        ) {
                            ProvideTextStyle(
                                value = MaterialTheme.typography.bodySmall,
                                content = it
                            )
                        }
                    }
                }
            }

            trailingContent?.let {
                Box(
                    modifier =
                        Modifier.padding(start = ListItemDefaults.TrailingContentStartPadding)
                ) {
                    CompositionLocalProvider(LocalContentColor provides LocalContentColor.current) {
                        ProvideTextStyle(value = trailingTextStyle, content = it)
                    }
                }
            }
        }
    }
}

/**
 * Calculates the minimum container height for [ListItem] based on whether the list items are
 * One-Line, Two-Line or Three-Line list items.
 *
 * @param hasLeadingContent if the leading supporting visual of the list item exists.
 * @param hasSupportingContent if the supporting text composable of the list item exists.
 * @param hasOverlineContent if the text composable displayed above the headline text exists.
 * @param dense whether the current list item is dense or not.
 * @return The minimum container height for the given list item (to be used with
 *   [Modifier.defaultMinSize]).
 */
private fun listItemMinHeight(
    hasLeadingContent: Boolean,
    hasSupportingContent: Boolean,
    hasOverlineContent: Boolean,
    dense: Boolean = false
): Dp {
    return when {
        hasSupportingContent && hasOverlineContent -> {
            if (dense) ListItemDefaults.MinDenseContainerHeightThreeLine
            else ListItemDefaults.MinContainerHeightThreeLine
        }
        hasSupportingContent || hasOverlineContent -> {
            if (dense) ListItemDefaults.MinDenseContainerHeightTwoLine
            else ListItemDefaults.MinContainerHeightTwoLine
        }
        hasLeadingContent -> {
            if (dense) ListItemDefaults.MinDenseContainerHeightLeadingContent
            else ListItemDefaults.MinContainerHeightLeadingContent
        }
        else -> {
            if (dense) ListItemDefaults.MinDenseContainerHeight
            else ListItemDefaults.MinContainerHeight
        }
    }
}

internal fun ListItemShape.toSelectableSurfaceShape() =
    SelectableSurfaceShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape,
        selectedDisabledShape = disabledShape,
        focusedSelectedDisabledShape = focusedDisabledShape
    )

internal fun ListItemColors.toSelectableSurfaceColors() =
    SelectableSurfaceColors(
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
        pressedSelectedContentColor = pressedSelectedContentColor
    )

internal fun ListItemScale.toSelectableSurfaceScale() =
    SelectableSurfaceScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale,
        selectedDisabledScale = disabledScale,
        focusedSelectedDisabledScale = focusedDisabledScale
    )

internal fun ListItemBorder.toSelectableSurfaceBorder() =
    SelectableSurfaceBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder,
        selectedDisabledBorder = disabledBorder,
        focusedSelectedDisabledBorder = focusedDisabledBorder
    )

internal fun ListItemGlow.toSelectableSurfaceGlow() =
    SelectableSurfaceGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )
