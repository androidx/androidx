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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.tokens.ElevationTokens
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.MenuTokens
import androidx.compose.material3.tokens.SegmentedMenuTokens
import androidx.compose.material3.tokens.ShapeTokens
import androidx.compose.material3.tokens.StandardMenuTokens
import androidx.compose.material3.tokens.VibrantMenuTokens
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Contains default values used for [DropdownMenu] and [DropdownMenuItem]. */
object MenuDefaults {
    /** The default tonal elevation for a menu. */
    val TonalElevation = ElevationTokens.Level0

    /** The default shadow elevation for a menu. */
    val ShadowElevation = MenuTokens.ContainerElevation

    /** The default leading icon size for a menu item. */
    val LeadingIconSize = SegmentedMenuTokens.ItemLeadingIconSize

    /** The default trailing icon size for a menu item. */
    val TrailingIconSize =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            24.dp
        } else {
            SegmentedMenuTokens.ItemTrailingIconSize
        }

    /** The default shape for a menu. */
    val shape
        @Composable get() = MenuTokens.ContainerShape.value

    /** The default container color for a menu. */
    val containerColor
        @Composable get() = MenuTokens.ContainerColor.value

    /**
     * The standard default container color for a group within a menu.
     *
     * Menus have two color options: standard (surface based) vibrant (tertiary based)
     *
     * These mappings provide options for lower or higher visual emphasis. Vibrant menus are more
     * prominent so should be used sparingly.
     */
    // TODO update with tokens when available
    @ExperimentalMaterial3ExpressiveApi
    val groupStandardContainerColor: Color
        @Composable get() = StandardMenuTokens.ContainerColor.value

    /**
     * The vibrant default container color for a group within a menu.
     *
     * Menus have two color options: standard (surface based) vibrant (tertiary based)
     *
     * These mappings provide options for lower or higher visual emphasis. Vibrant menus are more
     * prominent so should be used sparingly.
     */
    // TODO update with tokens when available
    @ExperimentalMaterial3ExpressiveApi
    val groupVibrantContainerColor: Color
        @Composable get() = VibrantMenuTokens.ContainerColor.value

    /** The default shape for the leading group of a menu. */
    @ExperimentalMaterial3ExpressiveApi
    val leadingGroupShape: Shape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = ShapeTokens.CornerValueLarge,
                topEnd = ShapeTokens.CornerValueLarge,
                bottomStart = ShapeTokens.CornerValueSmall,
                bottomEnd = ShapeTokens.CornerValueSmall,
            )

    /** The default shape for the middle group of a menu. */
    @ExperimentalMaterial3ExpressiveApi
    val middleGroupShape: Shape
        @Composable get() = SegmentedMenuTokens.GroupShape.value

    /** The default shape for the trailing group of a menu. */
    @ExperimentalMaterial3ExpressiveApi
    val trailingGroupShape: Shape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = ShapeTokens.CornerValueSmall,
                topEnd = ShapeTokens.CornerValueSmall,
                bottomStart = ShapeTokens.CornerValueLarge,
                bottomEnd = ShapeTokens.CornerValueLarge,
            )

    /** The default shape for the leading item of a menu or group. */
    @ExperimentalMaterial3ExpressiveApi
    val leadingItemShape: Shape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = ShapeTokens.CornerValueMedium,
                topEnd = ShapeTokens.CornerValueMedium,
                bottomStart = ShapeTokens.CornerValueExtraSmall,
                bottomEnd = ShapeTokens.CornerValueExtraSmall,
            )

    /** The default shape for the middle item of a menu or group. */
    @ExperimentalMaterial3ExpressiveApi
    val middleItemShape: Shape
        @Composable get() = SegmentedMenuTokens.ItemShape.value

    /** The default shape for the trailing item of a menu or group. */
    @ExperimentalMaterial3ExpressiveApi
    val trailingItemShape: Shape
        @Composable
        get() =
            RoundedCornerShape(
                topStart = ShapeTokens.CornerValueExtraSmall,
                topEnd = ShapeTokens.CornerValueExtraSmall,
                bottomStart = ShapeTokens.CornerValueMedium,
                bottomEnd = ShapeTokens.CornerValueMedium,
            )

    /** The default shape for a standalone item of a menu or group. */
    @ExperimentalMaterial3ExpressiveApi
    val standaloneItemShape: Shape
        @Composable get() = middleItemShape

    /** The selected shape for items of a group. */
    @ExperimentalMaterial3ExpressiveApi
    val selectedItemShape: Shape
        @Composable get() = SegmentedMenuTokens.ItemSelectedShape.value

    /** The default shape for a standalone group of a menu. */
    @ExperimentalMaterial3ExpressiveApi
    val standaloneGroupShape: Shape
        @Composable get() = SegmentedMenuTokens.ContainerShape.value

    /** The shape for a group of a menu that is no longer being hovered. */
    @ExperimentalMaterial3ExpressiveApi
    val inactiveGroupShape: Shape
        @Composable get() = SegmentedMenuTokens.InactiveContainerShape.value

    /** The default spacing between each menu group. Usually used in a [Spacer]'s height */
    @ExperimentalMaterial3ExpressiveApi val GroupSpacing: Dp = SegmentedMenuTokens.SegmentedGap

    /**
     * The default padding for a [HorizontalDivider] used in a menu group. Use this padding value in
     * a [HorizontalDivider]'s padding modifier.
     */
    @ExperimentalMaterial3ExpressiveApi
    val HorizontalDividerPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 2.dp)

    /** The default horizontal padding for a menu group label. Please see [MenuDefaults.Label]. */
    @ExperimentalMaterial3ExpressiveApi
    val DropdownMenuGroupLabelHorizontalPadding = PaddingValues(start = 12.dp, end = 4.dp)

    /**
     * A [MenuGroupShapes] constructor that the group in [index] should have when there are [count]
     * groups in the menu.
     *
     * @param index the index for this group in the menu.
     * @param count the count of groups in this menu.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun groupShape(index: Int, count: Int): MenuGroupShapes {
        if (count == 1) {
            return MaterialTheme.shapes.defaultMenuStandaloneGroupShapes
        }

        return when (index) {
            0 -> MaterialTheme.shapes.defaultMenuLeadingGroupShapes
            count - 1 -> MaterialTheme.shapes.defaultMenuTrailingGroupShapes
            else -> MaterialTheme.shapes.defaultMenuMiddleGroupShapes
        }
    }

    /**
     * A [MenuItemShapes] constructor that the item in [index] should have when there are [count]
     * items in the menu or group. If a [DropdownMenuGroup] is used, please pass the number of items
     * within the group as [count]. If no [DropdownMenuGroup] is used, please pass the number of
     * items in the entire menu as [count].
     *
     * @param index the index for this item in the menu or group.
     * @param count the count of items in this menu or group.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun itemShape(index: Int, count: Int): MenuItemShapes {
        if (count == 1) {
            return MaterialTheme.shapes.defaultMenuStandaloneItemShapes
        }

        return when (index) {
            0 -> MaterialTheme.shapes.defaultMenuLeadingItemShapes
            count - 1 -> MaterialTheme.shapes.defaultMenuTrailingItemShapes
            else -> MaterialTheme.shapes.defaultMenuMiddleItemShapes
        }
    }

    /**
     * Creates a [MenuItemColors] that represents the default text and icon colors used in a
     * [DropdownMenuItemContent].
     */
    @Composable fun itemColors(): MenuItemColors = MaterialTheme.colorScheme.defaultMenuItemColors

    /**
     * Creates a [MenuItemShapes] that represents the shapes used in a toggleable or selectable
     * [DropdownMenuItem], allowing for overrides.
     *
     * There is a convenience function that can be used to easily determine the shape to be used at
     * [MenuDefaults.itemShape].
     *
     * @param shape the shape when unselected. It uses [middleItemShape] as the default if null is
     *   provided.
     * @param selectedShape the shape when selected. It uses [selectedItemShape] as the default if
     *   null is provided.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun itemShapes(shape: Shape? = null, selectedShape: Shape? = null): MenuItemShapes =
        MaterialTheme.shapes.defaultMenuStandaloneItemShapes.copy(
            shape = shape,
            selectedShape = selectedShape,
        )

    /**
     * Creates a [MenuItemShapes] that represents the shapes used in a toggleable or selectable
     * [DropdownMenuItem].
     *
     * There is a convenience function that can be used to easily determine the shape to be used at
     * [MenuDefaults.itemShape].
     *
     * This [MenuItemShapes] has [MenuDefaults.standaloneItemShape] as the shape and
     * [MenuDefaults.selectedItemShape] as the selected shape.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun itemShapes(): MenuItemShapes = MaterialTheme.shapes.defaultMenuStandaloneItemShapes

    /**
     * Creates a [MenuGroupShapes] that represents the default shapes used in a [DropdownMenuGroup],
     * allowing for overrides.
     *
     * There is a convenience function that can be used to easily determine the shape to be used at
     * [MenuDefaults.groupShape].
     *
     * @param shape the default shape of the group. It uses [standaloneGroupShape] as the default if
     *   null is provided.
     * @param inactiveShape the shape when no longer being hovered. It uses [inactiveGroupShape] as
     *   the default if null is provided.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun groupShapes(shape: Shape? = null, inactiveShape: Shape? = null): MenuGroupShapes =
        MaterialTheme.shapes.defaultMenuStandaloneGroupShapes.copy(
            shape = shape,
            inactiveShape = inactiveShape,
        )

    /**
     * Creates a [MenuGroupShapes] that represents the default shapes used in a [DropdownMenuGroup].
     *
     * This [MenuGroupShapes] has [MenuDefaults.standaloneGroupShape] as the shape and
     * [MenuDefaults.inactiveGroupShape] as the inactive shape, the inactive shape is the shape of
     * the group after it is no longer being hovered.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun groupShapes(): MenuGroupShapes = MaterialTheme.shapes.defaultMenuStandaloneGroupShapes

    /**
     * The default label recommended to be used within a [DropdownMenuGroup].
     *
     * Labels can be used to categorize parts of the group or the entire group
     *
     * @param contentAlignment the alignment of the label's content.
     * @param padding the padding applied to the label's content.
     * @param content the content of the label.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun Label(
        contentAlignment: Alignment = Alignment.CenterStart,
        padding: PaddingValues = DropdownMenuGroupLabelHorizontalPadding,
        content: @Composable () -> Unit,
    ) {
        // TODO replace the typography with token when available
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Box(
                modifier =
                    Modifier.sizeIn(
                            minWidth = DropdownMenuItemDefaultMinWidth,
                            maxWidth = DropdownMenuItemDefaultMaxWidth,
                            minHeight = DropdownMenuGroupDefaultMinHeight,
                        )
                        .padding(padding),
                contentAlignment = contentAlignment,
            ) {
                content()
            }
        }
    }

    /**
     * The default horizontal padding for a menu group trailing label. Please see
     * [MenuDefaults.DropdownMenuItemTrailingLabel].
     */
    @ExperimentalMaterial3ExpressiveApi
    val DropdownMenuItemTrailingLabelHorizontalPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            PaddingValues(start = 0.dp, end = 6.dp)
        } else {
            PaddingValues(all = 0.dp)
        }

    /**
     * The default trailing label recommended to be used within a [DropdownMenuItem] which can be
     * passed to its trailingIcon param.
     *
     * @param padding the padding applied to the label's content.
     * @param content the content of the label.
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun DropdownMenuItemTrailingLabel(
        padding: PaddingValues = DropdownMenuItemTrailingLabelHorizontalPadding,
        content: @Composable () -> Unit,
    ) {
        // TODO replace the typography with token when available
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Box(modifier = Modifier.padding(padding)) { content() }
        }
    }

    /**
     * [Column] of a label and its supporting text. Used in a [DropdownMenuItem]'s text parameter
     * when a supporting text is desired.
     *
     * @param supportingText the supporting text of the label.
     * @param content the content of the label.
     */
    @Deprecated(
        "Removed in favor of the DropdownMenuItem APIs that have supportingText as a parameter.",
        level = DeprecationLevel.WARNING,
    )
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun LabelWithSupportingText(
        supportingText: @Composable () -> Unit,
        content: @Composable () -> Unit,
    ) {
        // TODO replace the typography with token when available
        Column {
            ProvideTextStyle(MaterialTheme.typography.labelLarge, content = content)
            ProvideTextStyle(MaterialTheme.typography.bodyMedium, content = supportingText)
        }
    }

    /**
     * Creates a [MenuItemColors] that represents the default text and icon colors used in a
     * [DropdownMenuItemContent].
     *
     * @param textColor the text color of this [DropdownMenuItemContent] when enabled
     * @param leadingIconColor the leading icon color of this [DropdownMenuItemContent] when enabled
     * @param trailingIconColor the trailing icon color of this [DropdownMenuItemContent] when
     *   enabled
     * @param disabledTextColor the text color of this [DropdownMenuItemContent] when not enabled
     * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItemContent] when
     *   not enabled
     * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItemContent]
     *   when not enabled
     */
    @Composable
    fun itemColors(
        textColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
    ): MenuItemColors =
        MaterialTheme.colorScheme.defaultMenuItemColors.copy(
            textColor = textColor,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledTextColor = disabledTextColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
        )

    /**
     * Creates a [MenuItemColors] that represents the default text, icon, and container colors used
     * in a standard color variant [DropdownMenuItem].
     *
     * @param textColor the text color of this [DropdownMenuItem] when enabled
     * @param containerColor the container color of this [DropdownMenuItem] when enabled and
     *   unselected
     * @param leadingIconColor the leading icon color of this [DropdownMenuItem] when enabled
     * @param trailingIconColor the trailing icon color of this [DropdownMenuItem] when enabled
     * @param disabledTextColor the text color of this [DropdownMenuItem] when not enabled
     * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItem] when not
     *   enabled
     * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItem] when not
     *   enabled
     * @param selectedContainerColor the container color of this [DropdownMenuItem] when enabled and
     *   selected
     * @param selectedTextColor the text color of this [DropdownMenuItem] when enabled and selected
     * @param selectedLeadingIconColor the leading icon color of this [DropdownMenuItem] when
     *   enabled and selected
     * @param selectedTrailingIconColor the trailing icon color of this [DropdownMenuItem] when
     *   enabled and selected
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun selectableItemColors(
        textColor: Color = Color.Unspecified,
        containerColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        selectedContainerColor: Color = Color.Unspecified,
        selectedTextColor: Color = Color.Unspecified,
        selectedLeadingIconColor: Color = Color.Unspecified,
        selectedTrailingIconColor: Color = Color.Unspecified,
    ): MenuItemColors =
        MaterialTheme.colorScheme.defaultMenuSelectableItemColors.copy(
            textColor = textColor,
            containerColor = containerColor,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledTextColor = disabledTextColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            selectedTextColor = selectedTextColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )

    /**
     * Creates a [MenuItemColors] that represents the default text, icon, and container colors used
     * in a vibrant color variant [DropdownMenuItem].
     *
     * @param textColor the text color of this [DropdownMenuItem] when enabled
     * @param containerColor the container color of this [DropdownMenuItem] when enabled and
     *   unselected
     * @param leadingIconColor the leading icon color of this [DropdownMenuItem] when enabled
     * @param trailingIconColor the trailing icon color of this [DropdownMenuItem] when enabled
     * @param disabledTextColor the text color of this [DropdownMenuItem] when not enabled
     * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItem] when not
     *   enabled
     * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItem] when not
     *   enabled
     * @param selectedContainerColor the container color of this [DropdownMenuItem] when enabled and
     *   selected
     * @param selectedTextColor the text color of this [DropdownMenuItem] when enabled and selected
     * @param selectedLeadingIconColor the leading icon color of this [DropdownMenuItem] when
     *   enabled and selected
     * @param selectedTrailingIconColor the trailing icon color of this [DropdownMenuItem] when
     *   enabled and selected
     */
    @ExperimentalMaterial3ExpressiveApi
    @Composable
    fun selectableItemVibrantColors(
        textColor: Color = Color.Unspecified,
        containerColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
        selectedContainerColor: Color = Color.Unspecified,
        selectedTextColor: Color = Color.Unspecified,
        selectedLeadingIconColor: Color = Color.Unspecified,
        selectedTrailingIconColor: Color = Color.Unspecified,
    ): MenuItemColors =
        MaterialTheme.colorScheme.defaultMenuSelectableItemVibrantColors.copy(
            textColor = textColor,
            containerColor = containerColor,
            leadingIconColor = leadingIconColor,
            trailingIconColor = trailingIconColor,
            disabledTextColor = disabledTextColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            selectedContainerColor = selectedContainerColor,
            selectedTextColor = selectedTextColor,
            selectedLeadingIconColor = selectedLeadingIconColor,
            selectedTrailingIconColor = selectedTrailingIconColor,
        )

    internal val ColorScheme.defaultMenuItemColors: MenuItemColors
        get() {
            return defaultMenuItemColorsCached
                ?: MenuItemColors(
                        textColor = fromToken(ListTokens.ItemLabelTextColor),
                        leadingIconColor = fromToken(ListTokens.ItemLeadingIconColor),
                        trailingIconColor = fromToken(ListTokens.ItemTrailingIconColor),
                        disabledTextColor =
                            fromToken(ListTokens.ItemDisabledLabelTextColor)
                                .copy(alpha = ListTokens.ItemDisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(ListTokens.ItemDisabledLeadingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(ListTokens.ItemDisabledTrailingIconColor)
                                .copy(alpha = ListTokens.ItemDisabledTrailingIconOpacity),
                    )
                    .also { defaultMenuItemColorsCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val ColorScheme.defaultMenuSelectableItemColors: MenuItemColors
        get() {
            return defaultMenuSelectableItemColorsCached
                ?: MenuItemColors(
                        textColor = fromToken(StandardMenuTokens.ItemLabelTextColor),
                        containerColor = fromToken(StandardMenuTokens.ContainerColor),
                        leadingIconColor = fromToken(StandardMenuTokens.ItemLeadingIconColor),
                        trailingIconColor = fromToken(StandardMenuTokens.ItemTrailingIconColor),
                        disabledTextColor =
                            fromToken(StandardMenuTokens.ItemDisabledLabelTextColor)
                                .copy(alpha = StandardMenuTokens.ItemDisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(StandardMenuTokens.ItemDisabledLeadingIconColor)
                                .copy(alpha = StandardMenuTokens.ItemDisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(StandardMenuTokens.ItemDisabledTrailingIconColor)
                                .copy(alpha = StandardMenuTokens.ItemDisabledTrailingIconOpacity),
                        disabledContainerColor = fromToken(StandardMenuTokens.ContainerColor),
                        selectedTextColor =
                            fromToken(StandardMenuTokens.ItemSelectedLabelTextColor),
                        selectedContainerColor =
                            fromToken(StandardMenuTokens.ItemSelectedContainerColor),
                        selectedLeadingIconColor =
                            fromToken(StandardMenuTokens.ItemSelectedLeadingIconColor),
                        selectedTrailingIconColor =
                            fromToken(StandardMenuTokens.ItemSelectedTrailingIconColor),
                    )
                    .also { defaultMenuSelectableItemColorsCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val ColorScheme.defaultMenuSelectableItemVibrantColors: MenuItemColors
        get() {
            return defaultMenuSelectableItemVibrantColorsCached
                ?: MenuItemColors(
                        textColor = fromToken(VibrantMenuTokens.ItemLabelTextColor),
                        containerColor = fromToken(VibrantMenuTokens.ContainerColor),
                        leadingIconColor = fromToken(VibrantMenuTokens.ItemLeadingIconColor),
                        trailingIconColor = fromToken(VibrantMenuTokens.ItemTrailingIconColor),
                        disabledTextColor =
                            fromToken(VibrantMenuTokens.ItemDisabledLabelTextColor)
                                .copy(alpha = VibrantMenuTokens.ItemDisabledLabelTextOpacity),
                        disabledLeadingIconColor =
                            fromToken(VibrantMenuTokens.ItemDisabledLeadingIconColor)
                                .copy(alpha = VibrantMenuTokens.ItemDisabledLeadingIconOpacity),
                        disabledTrailingIconColor =
                            fromToken(VibrantMenuTokens.ItemDisabledTrailingIconColor)
                                .copy(alpha = VibrantMenuTokens.ItemDisabledTrailingIconOpacity),
                        disabledContainerColor = fromToken(VibrantMenuTokens.ContainerColor),
                        selectedTextColor = fromToken(VibrantMenuTokens.ItemSelectedLabelTextColor),
                        selectedContainerColor =
                            fromToken(VibrantMenuTokens.ItemSelectedContainerColor),
                        selectedLeadingIconColor =
                            fromToken(VibrantMenuTokens.ItemSelectedLeadingIconColor),
                        selectedTrailingIconColor =
                            fromToken(VibrantMenuTokens.ItemSelectedTrailingIconColor),
                    )
                    .also { defaultMenuSelectableItemVibrantColorsCached = it }
        }

    /** Default padding used for [DropdownMenuItem]. */
    val DropdownMenuItemContentPadding =
        PaddingValues(horizontal = DropdownMenuItemHorizontalPadding, vertical = 0.dp)

    private val SelectableItemVerticalPadding = 12.dp

    /** Default padding used for [DropdownMenuItem] that are selectable. */
    val DropdownMenuSelectableItemContentPadding =
        if (shouldUsePrecisionPointerComponentSizing.value) {
            PaddingValues(
                start = 16.dp,
                end = 10.dp,
                top = SelectableItemVerticalPadding,
                bottom = SelectableItemVerticalPadding,
            )
        } else {
            PaddingValues(
                horizontal = DropdownMenuItemHorizontalPadding,
                vertical = SelectableItemVerticalPadding,
            )
        }

    /** Default padding used for [DropdownMenuGroup]. */
    val DropdownMenuGroupContentPadding =
        PaddingValues(horizontal = 0.dp, vertical = DropdownMenuGroupVerticalPadding)

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuStandaloneItemShapes: MenuItemShapes
        get() {
            return defaultMenuStandaloneItemShapesCached
                ?: MenuItemShapes(
                        shape = fromToken(SegmentedMenuTokens.ItemShape),
                        selectedShape = fromToken(SegmentedMenuTokens.ItemSelectedShape),
                    )
                    .also { defaultMenuStandaloneItemShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuLeadingItemShapes: MenuItemShapes
        get() {
            return defaultMenuLeadingItemShapesCached
                ?: MenuItemShapes(
                        shape =
                            RoundedCornerShape(
                                topStart = ShapeTokens.CornerValueMedium,
                                topEnd = ShapeTokens.CornerValueMedium,
                                bottomStart = ShapeTokens.CornerValueExtraSmall,
                                bottomEnd = ShapeTokens.CornerValueExtraSmall,
                            ),
                        selectedShape = fromToken(SegmentedMenuTokens.ItemSelectedShape),
                    )
                    .also { defaultMenuLeadingItemShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuMiddleItemShapes: MenuItemShapes
        get() {
            return defaultMenuMiddleItemShapesCached
                ?: MenuItemShapes(
                        shape = fromToken(SegmentedMenuTokens.ItemShape),
                        selectedShape = fromToken(SegmentedMenuTokens.ItemSelectedShape),
                    )
                    .also { defaultMenuMiddleItemShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuTrailingItemShapes: MenuItemShapes
        get() {
            return defaultMenuTrailingItemShapesCached
                ?: MenuItemShapes(
                        shape =
                            RoundedCornerShape(
                                topStart = ShapeTokens.CornerValueExtraSmall,
                                topEnd = ShapeTokens.CornerValueExtraSmall,
                                bottomStart = ShapeTokens.CornerValueMedium,
                                bottomEnd = ShapeTokens.CornerValueMedium,
                            ),
                        selectedShape = fromToken(SegmentedMenuTokens.ItemSelectedShape),
                    )
                    .also { defaultMenuTrailingItemShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuStandaloneGroupShapes: MenuGroupShapes
        get() {
            return defaultMenuStandaloneGroupShapesCached
                ?: MenuGroupShapes(
                        shape = fromToken(SegmentedMenuTokens.ContainerShape),
                        inactiveShape = fromToken(SegmentedMenuTokens.InactiveContainerShape),
                    )
                    .also { defaultMenuStandaloneGroupShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuLeadingGroupShapes: MenuGroupShapes
        get() {
            return defaultMenuLeadingGroupShapesCached
                ?: MenuGroupShapes(
                        shape =
                            RoundedCornerShape(
                                topStart = ShapeTokens.CornerValueLarge,
                                topEnd = ShapeTokens.CornerValueLarge,
                                bottomStart = ShapeTokens.CornerValueSmall,
                                bottomEnd = ShapeTokens.CornerValueSmall,
                            ),
                        inactiveShape = fromToken(SegmentedMenuTokens.InactiveContainerShape),
                    )
                    .also { defaultMenuLeadingGroupShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuMiddleGroupShapes: MenuGroupShapes
        get() {
            return defaultMenuMiddleGroupShapesCached
                ?: MenuGroupShapes(
                        shape = fromToken(SegmentedMenuTokens.GroupShape),
                        inactiveShape = fromToken(SegmentedMenuTokens.InactiveContainerShape),
                    )
                    .also { defaultMenuMiddleGroupShapesCached = it }
        }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    internal val Shapes.defaultMenuTrailingGroupShapes: MenuGroupShapes
        get() {
            return defaultMenuTrailingGroupShapesCached
                ?: MenuGroupShapes(
                        shape =
                            RoundedCornerShape(
                                topStart = ShapeTokens.CornerValueSmall,
                                topEnd = ShapeTokens.CornerValueSmall,
                                bottomStart = ShapeTokens.CornerValueLarge,
                                bottomEnd = ShapeTokens.CornerValueLarge,
                            ),
                        inactiveShape = fromToken(SegmentedMenuTokens.InactiveContainerShape),
                    )
                    .also { defaultMenuTrailingGroupShapesCached = it }
        }
}
