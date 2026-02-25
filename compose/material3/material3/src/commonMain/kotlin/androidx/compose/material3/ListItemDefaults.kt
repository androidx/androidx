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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.ReorderListTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/** Contains the default values used by list items. */
object ListItemDefaults {
    /** The default padding applied to all content within a list item. */
    val ContentPadding: PaddingValues =
        PaddingValues(
            start = InteractiveListStartPadding,
            end = InteractiveListEndPadding,
            top = InteractiveListTopPadding,
            bottom = InteractiveListBottomPadding,
        )

    /** The default elevation of a list item */
    val Elevation: Dp = ListTokens.ItemContainerElevation

    /** The default shape of a list item */
    val shape: Shape
        @Composable @ReadOnlyComposable get() = ListTokens.ItemContainerShape.value

    /** The container color of a list item */
    val containerColor: Color
        @Composable @ReadOnlyComposable get() = ListTokens.ItemContainerColor.value

    /** The content color of a list item */
    val contentColor: Color
        @Composable @ReadOnlyComposable get() = ListTokens.ItemLabelTextColor.value

    /**
     * Creates a [ListItemColors] that represents the default colors for a [ListItem] in different
     * states.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultListItemColors

    /**
     * Creates a [ListItemColors] that represents the default colors for a [ListItem] in different
     * states.
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
    ): ListItemColors {
        return MaterialTheme.colorScheme.defaultListItemColors.copy(
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

    internal val ColorScheme.defaultListItemColors: ListItemColors
        get() {
            return defaultListItemColorsCached
                ?: ListItemColors(
                        // default
                        containerColor = fromToken(ListTokens.ItemContainerColor),
                        contentColor = fromToken(ListTokens.ItemLabelTextColor),
                        leadingContentColor = fromToken(ListTokens.ItemLeadingIconColor),
                        trailingContentColor = fromToken(ListTokens.ItemTrailingIconColor),
                        overlineContentColor = fromToken(ListTokens.ItemOverlineColor),
                        supportingContentColor = fromToken(ListTokens.ItemSupportingTextColor),
                        // selected
                        selectedContainerColor = fromToken(ListTokens.ItemSelectedContainerColor),
                        selectedContentColor = fromToken(ListTokens.ItemSelectedLabelTextColor),
                        selectedLeadingContentColor =
                            fromToken(ListTokens.ItemSelectedLeadingIconColor),
                        selectedTrailingContentColor =
                            fromToken(ListTokens.ItemSelectedTrailingIconColor),
                        selectedOverlineContentColor =
                            fromToken(ListTokens.ItemSelectedOverlineColor),
                        selectedSupportingContentColor =
                            fromToken(ListTokens.ItemSelectedSupportingTextColor),
                        // disabled
                        disabledContainerColor = fromToken(ListTokens.ItemContainerColor),
                        disabledContentColor =
                            fromToken(ListTokens.ItemDisabledLabelTextColor)
                                .copy(alpha = ListTokens.ItemDisabledLabelTextOpacity),
                        disabledLeadingContentColor =
                            fromToken(ListTokens.ItemDisabledLeadingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledLeadingIconOpacity),
                        disabledTrailingContentColor =
                            fromToken(ListTokens.ItemDisabledTrailingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledTrailingIconOpacity),
                        disabledOverlineContentColor =
                            fromToken(ListTokens.ItemDisabledOverlineColor)
                                .copy(alpha = ListTokens.ItemDisabledOverlineOpacity),
                        disabledSupportingContentColor =
                            fromToken(ListTokens.ItemDisabledSupportingTextColor)
                                .copy(alpha = ListTokens.ItemDisabledSupportingTextOpacity),
                        // dragged
                        draggedContainerColor = fromToken(ReorderListTokens.ItemContainerColor),
                        draggedContentColor = fromToken(ReorderListTokens.ItemLabelTextColor),
                        draggedLeadingContentColor =
                            fromToken(ReorderListTokens.ItemLeadingIconColor),
                        draggedTrailingContentColor =
                            fromToken(ReorderListTokens.ItemTrailingIconColor),
                        draggedOverlineContentColor =
                            fromToken(ReorderListTokens.ItemOverlineColor),
                        draggedSupportingContentColor =
                            fromToken(ReorderListTokens.ItemSupportingTextColor),
                    )
                    .also { defaultListItemColorsCached = it }
        }

    /**
     * Creates a [ListItemColors] that represents the default colors for a [SegmentedListItem] in
     * different states.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun segmentedColors(): ListItemColors = MaterialTheme.colorScheme.defaultSegmentedListItemColors

    /**
     * Creates a [ListItemColors] that represents the default colors for a [SegmentedListItem] in
     * different states.
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
    @ExperimentalMaterial3ExpressiveApi
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
    ): ListItemColors {
        return MaterialTheme.colorScheme.defaultSegmentedListItemColors.copy(
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

    internal val ColorScheme.defaultSegmentedListItemColors: ListItemColors
        get() {
            return defaultSegmentedListItemColorsCached
                ?: ListItemColors(
                        // default
                        containerColor = fromToken(ListTokens.ItemSegmentedContainerColor),
                        contentColor = fromToken(ListTokens.ItemLabelTextColor),
                        leadingContentColor = fromToken(ListTokens.ItemLeadingIconColor),
                        trailingContentColor = fromToken(ListTokens.ItemTrailingIconColor),
                        overlineContentColor = fromToken(ListTokens.ItemOverlineColor),
                        supportingContentColor = fromToken(ListTokens.ItemSupportingTextColor),
                        // selected
                        selectedContainerColor = fromToken(ListTokens.ItemSelectedContainerColor),
                        selectedContentColor = fromToken(ListTokens.ItemSelectedLabelTextColor),
                        selectedLeadingContentColor =
                            fromToken(ListTokens.ItemSelectedLeadingIconColor),
                        selectedTrailingContentColor =
                            fromToken(ListTokens.ItemSelectedTrailingIconColor),
                        selectedOverlineContentColor =
                            fromToken(ListTokens.ItemSelectedOverlineColor),
                        selectedSupportingContentColor =
                            fromToken(ListTokens.ItemSelectedSupportingTextColor),
                        // disabled
                        disabledContainerColor = fromToken(ListTokens.ItemSegmentedContainerColor),
                        disabledContentColor =
                            fromToken(ListTokens.ItemDisabledLabelTextColor)
                                .copy(alpha = ListTokens.ItemDisabledLabelTextOpacity),
                        disabledLeadingContentColor =
                            fromToken(ListTokens.ItemDisabledLeadingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledLeadingIconOpacity),
                        disabledTrailingContentColor =
                            fromToken(ListTokens.ItemDisabledTrailingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledTrailingIconOpacity),
                        disabledOverlineContentColor =
                            fromToken(ListTokens.ItemDisabledOverlineColor)
                                .copy(alpha = ListTokens.ItemDisabledOverlineOpacity),
                        disabledSupportingContentColor =
                            fromToken(ListTokens.ItemDisabledSupportingTextColor)
                                .copy(alpha = ListTokens.ItemDisabledSupportingTextOpacity),
                        // dragged
                        draggedContainerColor = fromToken(ReorderListTokens.ItemContainerColor),
                        draggedContentColor = fromToken(ReorderListTokens.ItemLabelTextColor),
                        draggedLeadingContentColor =
                            fromToken(ReorderListTokens.ItemLeadingIconColor),
                        draggedTrailingContentColor =
                            fromToken(ReorderListTokens.ItemTrailingIconColor),
                        draggedOverlineContentColor =
                            fromToken(ReorderListTokens.ItemOverlineColor),
                        draggedSupportingContentColor =
                            fromToken(ReorderListTokens.ItemSupportingTextColor),
                    )
                    .also { defaultSegmentedListItemColorsCached = it }
        }

    /**
     * Creates a [ListItemShapes] that represents the default shapes for a [ListItem] in different
     * states.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun shapes(): ListItemShapes = MaterialTheme.shapes.defaultListItemShapes

    /**
     * Creates a [ListItemShapes] that represents the default shapes for a [ListItem] in different
     * states.
     *
     * @param shape the default shape of the list item.
     * @param selectedShape the shape of the list item when selected.
     * @param pressedShape the shape of the list item when pressed.
     * @param focusedShape the shape of the list item when focused.
     * @param hoveredShape the shape of the list item when hovered.
     * @param draggedShape the shape of the list item when dragged.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun shapes(
        shape: Shape? = null,
        selectedShape: Shape? = null,
        pressedShape: Shape? = null,
        focusedShape: Shape? = null,
        hoveredShape: Shape? = null,
        draggedShape: Shape? = null,
    ): ListItemShapes =
        MaterialTheme.shapes.defaultListItemShapes.copy(
            shape = shape,
            selectedShape = selectedShape,
            pressedShape = pressedShape,
            focusedShape = focusedShape,
            hoveredShape = hoveredShape,
            draggedShape = draggedShape,
        )

    /**
     * Constructor for [ListItemShapes] to be used by a [SegmentedListItem] which has an [index] in
     * a list that has a total of [count] items.
     *
     * @param index the index for this list item in the overall list.
     * @param count the total count of list items in the overall list.
     * @param defaultShapes the default [ListItemShapes] that should be used for standalone items or
     *   items in the middle of the list.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun segmentedShapes(
        index: Int,
        count: Int,
        defaultShapes: ListItemShapes = shapes(),
    ): ListItemShapes {
        val overrideShape = ListTokens.ContainerShape.value
        return remember(index, count, defaultShapes, overrideShape) {
            when {
                count == 1 -> defaultShapes

                index == 0 -> {
                    val defaultBaseShape = defaultShapes.shape
                    if (defaultBaseShape is CornerBasedShape && overrideShape is CornerBasedShape) {
                        defaultShapes.copy(
                            shape =
                                defaultBaseShape.copy(
                                    topStart = overrideShape.topStart,
                                    topEnd = overrideShape.topEnd,
                                )
                        )
                    } else {
                        defaultShapes
                    }
                }

                index == count - 1 -> {
                    val defaultBaseShape = defaultShapes.shape
                    if (defaultBaseShape is CornerBasedShape && overrideShape is CornerBasedShape) {
                        defaultShapes.copy(
                            shape =
                                defaultBaseShape.copy(
                                    bottomStart = overrideShape.bottomStart,
                                    bottomEnd = overrideShape.bottomEnd,
                                )
                        )
                    } else {
                        defaultShapes
                    }
                }

                else -> defaultShapes
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultListItemShapes: ListItemShapes
        get() {
            return defaultListItemShapesCached
                ?: ListItemShapes(
                        shape = fromToken(ListTokens.ItemContainerExpressiveShape),
                        selectedShape = fromToken(ListTokens.ItemSelectedContainerExpressiveShape),
                        pressedShape = fromToken(ListTokens.ItemPressedContainerExpressiveShape),
                        focusedShape = fromToken(ListTokens.ItemFocusedContainerExpressiveShape),
                        hoveredShape = fromToken(ListTokens.ItemHoveredContainerExpressiveShape),
                        draggedShape = fromToken(ReorderListTokens.ItemShape),
                    )
                    .also { defaultListItemShapesCached = it }
        }

    /**
     * Creates a [ListItemElevation] that represents the elevation for a [ListItem] in different
     * states.
     *
     * @param elevation the default elevation of the list item.
     * @param draggedElevation the elevation of the list item when dragged.
     */
    @ExperimentalMaterial3ExpressiveApi
    fun elevation(
        elevation: Dp = ListTokens.ItemContainerElevation,
        draggedElevation: Dp = ListTokens.ItemDraggedContainerElevation,
    ): ListItemElevation =
        ListItemElevation(elevation = elevation, draggedElevation = draggedElevation)

    /** The vertical space between different [SegmentedListItem]s. */
    @ExperimentalMaterial3ExpressiveApi val SegmentedGap: Dp = ListTokens.SegmentedGap

    /**
     * Returns the default vertical alignment of children content within a [ListItem]. This is
     * equivalent to [Alignment.CenterVertically] for shorter items and [Alignment.Top] for taller
     * items.
     */
    @ExperimentalMaterial3ExpressiveApi
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

    /**
     * Creates a [ListItemColors] that represents the default container and content colors used in a
     * [ListItem].
     *
     * @param containerColor the container color of this list item when enabled.
     * @param headlineColor the headline text content color of this list item when enabled.
     * @param leadingIconColor the color of this list item's leading content when enabled.
     * @param overlineColor the overline text color of this list item
     * @param supportingColor the supporting text color of this list item
     * @param trailingIconColor the color of this list item's trailing content when enabled.
     * @param disabledHeadlineColor the content color of this list item when not enabled.
     * @param disabledLeadingIconColor the color of this list item's leading content when not
     *   enabled.
     * @param disabledTrailingIconColor the color of this list item's trailing content when not
     *   enabled.
     */
    @Composable
    fun colors(
        containerColor: Color = Color.Unspecified,
        headlineColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        overlineColor: Color = Color.Unspecified,
        supportingColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledHeadlineColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
    ): ListItemColors =
        MaterialTheme.colorScheme.defaultListItemColors.copy(
            containerColor = containerColor,
            contentColor = headlineColor,
            leadingContentColor = leadingIconColor,
            overlineContentColor = overlineColor,
            supportingContentColor = supportingColor,
            trailingContentColor = trailingIconColor,
            disabledContentColor = disabledHeadlineColor,
            disabledLeadingContentColor = disabledLeadingIconColor,
            disabledTrailingContentColor = disabledTrailingIconColor,
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
@Immutable
class ListItemColors(
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
    constructor(
        containerColor: Color,
        headlineColor: Color,
        leadingIconColor: Color,
        overlineColor: Color,
        supportingTextColor: Color,
        trailingIconColor: Color,
        disabledHeadlineColor: Color,
        disabledLeadingIconColor: Color,
        disabledTrailingIconColor: Color,
    ) : this(
        // default
        containerColor = containerColor,
        contentColor = headlineColor,
        leadingContentColor = leadingIconColor,
        trailingContentColor = trailingIconColor,
        overlineContentColor = overlineColor,
        supportingContentColor = supportingTextColor,
        // disabled
        disabledContainerColor = Color.Unspecified,
        disabledContentColor = disabledHeadlineColor,
        disabledLeadingContentColor = disabledLeadingIconColor,
        disabledTrailingContentColor = disabledTrailingIconColor,
        disabledOverlineContentColor = Color.Unspecified,
        disabledSupportingContentColor = Color.Unspecified,
        // selected
        selectedContainerColor = Color.Unspecified,
        selectedContentColor = Color.Unspecified,
        selectedLeadingContentColor = Color.Unspecified,
        selectedTrailingContentColor = Color.Unspecified,
        selectedOverlineContentColor = Color.Unspecified,
        selectedSupportingContentColor = Color.Unspecified,
        // dragged
        draggedContainerColor = Color.Unspecified,
        draggedContentColor = Color.Unspecified,
        draggedLeadingContentColor = Color.Unspecified,
        draggedTrailingContentColor = Color.Unspecified,
        draggedOverlineContentColor = Color.Unspecified,
        draggedSupportingContentColor = Color.Unspecified,
    )

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
     * Returns a copy of this [ListItemColors], optionally overriding some of the values. This uses
     * [Color.Unspecified] to mean “use the value from the source”.
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
    ): ListItemColors {
        return ListItemColors(
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
        if (other == null || other !is ListItemColors) return false

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

    @Deprecated("Renamed to contentColor")
    val headlineColor: Color
        get() = contentColor

    @Deprecated("Renamed to leadingContentColor")
    val leadingIconColor: Color
        get() = leadingContentColor

    @Deprecated("Renamed to overlineContentColor")
    val overlineColor: Color
        get() = overlineContentColor

    @Deprecated("Renamed to supportingContentColor")
    val supportingTextColor: Color
        get() = supportingContentColor

    @Deprecated("Renamed to trailingContentColor")
    val trailingIconColor: Color
        get() = trailingContentColor

    @Deprecated("Renamed to disabledContentColor")
    val disabledHeadlineColor: Color
        get() = disabledContentColor

    @Deprecated("Renamed to disabledLeadingContentColor")
    val disabledLeadingIconColor: Color
        get() = disabledLeadingContentColor

    @Deprecated("Renamed to disabledTrailingContentColor")
    val disabledTrailingIconColor: Color
        get() = disabledTrailingContentColor

    @Deprecated("Use overload with parameters for selected and dragged colors")
    @Suppress("DEPRECATION")
    fun copy(
        containerColor: Color = this.containerColor,
        headlineColor: Color = this.headlineColor,
        leadingIconColor: Color = this.leadingIconColor,
        overlineColor: Color = this.overlineColor,
        supportingTextColor: Color = this.supportingTextColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledHeadlineColor: Color = this.disabledHeadlineColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
    ) =
        ListItemColors(
            containerColor = containerColor.takeOrElse { this.containerColor },
            headlineColor = headlineColor.takeOrElse { this.headlineColor },
            leadingIconColor = leadingIconColor.takeOrElse { this.leadingIconColor },
            overlineColor = overlineColor.takeOrElse { this.overlineColor },
            supportingTextColor = supportingTextColor.takeOrElse { this.supportingTextColor },
            trailingIconColor = trailingIconColor.takeOrElse { this.trailingIconColor },
            disabledHeadlineColor = disabledHeadlineColor.takeOrElse { this.disabledHeadlineColor },
            disabledLeadingIconColor =
                disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
            disabledTrailingIconColor =
                disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
        )
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
class ListItemShapes(
    val shape: Shape,
    val selectedShape: Shape,
    val pressedShape: Shape,
    val focusedShape: Shape,
    val hoveredShape: Shape,
    val draggedShape: Shape,
) {
    /** Returns a copy of this [ListItemShapes], optionally overriding some of the values. */
    fun copy(
        shape: Shape? = this.shape,
        selectedShape: Shape? = this.selectedShape,
        pressedShape: Shape? = this.pressedShape,
        focusedShape: Shape? = this.focusedShape,
        hoveredShape: Shape? = this.hoveredShape,
        draggedShape: Shape? = this.draggedShape,
    ): ListItemShapes =
        ListItemShapes(
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
        if (other == null || other !is ListItemShapes) return false

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
private val ListItemShapes.hasRoundedCornerShapes: Boolean
    get() =
        shape is RoundedCornerShape &&
            selectedShape is RoundedCornerShape &&
            pressedShape is RoundedCornerShape &&
            focusedShape is RoundedCornerShape &&
            hoveredShape is RoundedCornerShape &&
            draggedShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ListItemShapes.hasCornerBasedShapes: Boolean
    get() =
        shape is CornerBasedShape &&
            selectedShape is CornerBasedShape &&
            pressedShape is CornerBasedShape &&
            focusedShape is CornerBasedShape &&
            hoveredShape is CornerBasedShape &&
            draggedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ListItemShapes.shapeForInteraction(
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
    } else if (hasCornerBasedShapes) {
        return key(this) { rememberAnimatedShape(shape as CornerBasedShape, animationSpec) }
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
class ListItemElevation(val elevation: Dp, val draggedElevation: Dp) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ListItemElevation) return false

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
