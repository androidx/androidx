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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.SegmentedMenuTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.collections.get
import kotlin.math.max
import kotlin.math.min

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * A [DropdownMenu] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [DropdownMenu] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [DropdownMenu] by itself will not take up any
 * space in a layout, as the menu is displayed in a separate window, on top of other content.
 *
 * The [content] of a [DropdownMenu] will typically be [DropdownMenuItem]s, as well as custom
 * content. Using [DropdownMenuItem]s will result in a menu that matches the Material specification
 * for menus. Also note that the [content] is placed inside a scrollable [Column], so using a
 * [androidx.compose.foundation.lazy.LazyColumn] as the root layout inside [content] is unsupported.
 *
 * [onDismissRequest] will be called when the menu should close - for example when there is a tap
 * outside the menu, or when the back key is pressed.
 *
 * [DropdownMenu] changes its positioning depending on the available space, always trying to be
 * fully visible. Depending on layout direction, first it will try to align its start to the start
 * of its parent, then its end to the end of its parent, and then to the edge of the window.
 * Vertically, it will try to align its top to the bottom of its parent, then its bottom to top of
 * its parent, and then to the edge of the window.
 *
 * An [offset] can be provided to adjust the positioning of the menu for cases when the layout
 * bounds of its parent do not coincide with its visual bounds.
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.MenuSample
 *
 * Example usage with a [ScrollState] to control the menu items scroll position:
 *
 * @sample androidx.compose.material3.samples.MenuWithScrollStateSample
 * @param expanded whether the menu is expanded or not
 * @param onDismissRequest called when the user requests to dismiss the menu, such as by tapping
 *   outside the menu's bounds
 * @param modifier [Modifier] to be applied to the menu's content
 * @param offset [DpOffset] from the original position of the menu. The offset respects the
 *   [androidx.compose.ui.unit.LayoutDirection], so the offset's x position will be added in LTR and
 *   subtracted in RTL.
 * @param scrollState a [ScrollState] to used by the menu's content for items vertical scrolling
 * @param properties [PopupProperties] for further customization of this popup's behavior
 * @param shape the shape of the menu
 * @param containerColor the container color of the menu
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param shadowElevation the elevation for the shadow below the menu
 * @param border the border to draw around the container of the menu. Pass `null` for no border.
 * @param content the content of this dropdown menu, typically a [DropdownMenuItem]
 */
@Composable
expect fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = DefaultMenuProperties,
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
)

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A [Popup] that provides the foundation for building a custom menu.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * This composable provides the [Popup] and layout behavior for a menu. This is useful for building
 * custom menus that require different content arrangements or styling than the default
 * [DropdownMenu].
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.GroupedMenuSample
 * @param expanded whether the menu is expanded or not.
 * @param onDismissRequest called when the user requests to dismiss the menu, such as by tapping
 *   outside the menu's bounds.
 * @param modifier [Modifier] to be applied to the menu's content.
 * @param offset [DpOffset] from the original position of the menu.
 * @param properties [PopupProperties] for further customization of this popup's behavior.
 * @param content the content of this dropdown menu.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
expect fun DropdownMenuPopup(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = DefaultMenuProperties,
    content: @Composable ColumnScope.() -> Unit,
)

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A composable for creating a visually distinct group within a [DropdownMenuPopup].
 *
 * This component adds additional styling to [content]. It's used to group related menu items.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.GroupedMenuSample
 * @param shapes the [MenuGroupShapes] of the menu group. The shapes provided should be determined
 *   by the number of groups in the menu as well as the group's position in the menu. There is a
 *   convenience function that can be used to easily determine the shape to be used at
 *   [MenuDefaults.groupShape]
 * @param modifier [Modifier] to be applied to this menu group.
 * @param containerColor the container color of the menu group. There are two predefined container
 *   colors at [MenuDefaults.groupStandardContainerColor] and
 *   [MenuDefaults.groupVibrantContainerColor] which you can use.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param shadowElevation the elevation for the shadow below the menu group.
 * @param border the border to draw around the container of the menu group.
 * @param contentPadding the padding applied to the content of this menu group.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu group.
 * @param content the content of this menu group, typically [DropdownMenuItem]s.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuGroup(
    shapes: MenuGroupShapes,
    modifier: Modifier = Modifier,
    containerColor: Color = MenuDefaults.groupStandardContainerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuGroupContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    var hasBeenHovered by remember { mutableStateOf(false) }
    if (hovered) {
        hasBeenHovered = true
    }

    val morphSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val groupShape = shapeByInteraction(shapes, hasBeenHovered, hovered, morphSpec)

    Surface(
        modifier = Modifier.hoverable(interactionSource = interactionSource),
        shape = groupShape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        Column(modifier = modifier.padding(contentPadding), content = content)
    }
}

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.MenuSample
 * @param text text of the menu item
 * @param onClick called when this menu item is clicked
 * @param modifier the [Modifier] to be applied to this menu item
 * @param leadingIcon optional leading icon to be displayed at the beginning of the item's text
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text. This
 *   trailing icon slot can also accept [Text] to indicate a keyboard shortcut.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [MenuItemColors] that will be used to resolve the colors used for this menu item in
 *   different states. See [MenuDefaults.itemColors].
 * @param contentPadding the padding applied to the content of this menu item
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item. You can use this to change the menu item's
 *   appearance or preview the menu item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
expect fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
)

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * @param onClick called when this menu item is clicked
 * @param text text of the menu item.
 * @param shape [Shape] of this menu item. The shapes provided should be determined by the number of
 *   items in the group or menu as well as the item's position in the menu. Please use
 *   [MenuDefaults.leadingItemShape] for the first item in a list, [MenuDefaults.middleItemShape]
 *   for the middle items in a list, and [MenuDefaults.trailingItemShape] for the last item in a
 *   list.
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) =
    DropdownMenuItem(
        onClick = onClick,
        text = text,
        shape = shape,
        modifier = modifier,
        supportingText = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * @param onClick called when this menu item is clicked
 * @param text text of the menu item.
 * @param shape [Shape] of this menu item. The shapes provided should be determined by the number of
 *   items in the group or menu as well as the item's position in the menu. Please use
 *   [MenuDefaults.leadingItemShape] for the first item in a list, [MenuDefaults.middleItemShape]
 *   for the middle items in a list, and [MenuDefaults.trailingItemShape] for the last item in a
 *   list.
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 * @param supportingText optional supporting text of the menu item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuSelectableItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    DropdownMenuItemContent(
        text = text,
        selected = false,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.Button },
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        selectedLeadingIcon = null,
        enabled = enabled,
        colors = colors,
        shapes = MenuDefaults.itemShapes(shape = shape),
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A menu item that changes its styling depending on the [checked] state.
 *
 * This composable is suitable for menu items that represent an on/off setting, behaving like a
 * checkbox or switch within the menu.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.GroupedMenuSample
 * @param checked whether this menu item is currently checked.
 * @param onCheckedChange called when this menu item is clicked, with the new checked state.
 * @param text text of the menu item.
 * @param shapes [MenuItemShapes] that will be used to resolve the shapes for this menu item. The
 *   shape of this item is determined by the value of [checked]. The shapes provided should be
 *   determined by the number of items in the group or menu as well as the item's position in the
 *   menu. There is a convenience function that can be used to easily determine the shape to be used
 *   at [MenuDefaults.itemShape]
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param checkedLeadingIcon optional leading icon to be displayed when the item is checked.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item. There
 *   are two predefined [MenuItemColors] at [MenuDefaults.selectableItemColors] and
 *   [MenuDefaults.selectableItemVibrantColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: @Composable () -> Unit,
    shapes: MenuItemShapes,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    checkedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.selectableItemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuSelectableItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) =
    DropdownMenuItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        text = text,
        shapes = shapes,
        modifier = modifier,
        supportingText = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        checkedLeadingIcon = checkedLeadingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A menu item that changes its styling depending on the [checked] state.
 *
 * This composable is suitable for menu items that represent an on/off setting, behaving like a
 * checkbox or switch within the menu.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.GroupedMenuSample
 * @param checked whether this menu item is currently checked.
 * @param onCheckedChange called when this menu item is clicked, with the new checked state.
 * @param text text of the menu item.
 * @param shapes [MenuItemShapes] that will be used to resolve the shapes for this menu item. The
 *   shape of this item is determined by the value of [checked]. The shapes provided should be
 *   determined by the number of items in the group or menu as well as the item's position in the
 *   menu. There is a convenience function that can be used to easily determine the shape to be used
 *   at [MenuDefaults.itemShape]
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param checkedLeadingIcon optional leading icon to be displayed when the item is checked.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item. There
 *   are two predefined [MenuItemColors] at [MenuDefaults.selectableItemColors] and
 *   [MenuDefaults.selectableItemVibrantColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 * @param supportingText optional supporting text of the menu item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: @Composable () -> Unit,
    shapes: MenuItemShapes,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    checkedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.selectableItemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuSelectableItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    DropdownMenuItemContent(
        text = text,
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier.semantics { role = Role.Checkbox },
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        selectedLeadingIcon = checkedLeadingIcon,
        enabled = enabled,
        colors = colors,
        shapes = shapes,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A menu item that changes its styling depending on the [selected] state.
 *
 * This composable is suitable for menu items that represent an on/off setting, behaving like a
 * radio button within the menu.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 * @param selected whether this menu item is currently selected.
 * @param onClick called when this menu item is clicked.
 * @param text text of the menu item.
 * @param shapes [MenuItemShapes] that will be used to resolve the shapes for this menu item. The
 *   shape of this item is determined by the value of [selected]. The shapes provided should be
 *   determined by the number of items in the group or menu as well as the item's position in the
 *   menu. There is a convenience function that can be used to easily determine the shape to be used
 *   at [MenuDefaults.itemShape]
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param checkedLeadingIcon optional leading icon to be displayed when the item is checked.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item. There
 *   are two predefined [MenuItemColors] at [MenuDefaults.selectableItemColors] and
 *   [MenuDefaults.selectableItemVibrantColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 */
@Deprecated("Maintained for binary compatibility.", level = DeprecationLevel.HIDDEN)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    shapes: MenuItemShapes,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    checkedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.selectableItemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuSelectableItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) =
    DropdownMenuItem(
        selected = selected,
        onClick = onClick,
        text = text,
        shapes = shapes,
        modifier = modifier,
        supportingText = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        selectedLeadingIcon = checkedLeadingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * A menu item that changes its styling depending on the [selected] state.
 *
 * This composable is suitable for menu items that represent an on/off setting, behaving like a
 * radio button within the menu.
 *
 * ![Dropdown menu
 * image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu-selectable-items.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 * @param selected whether this menu item is currently selected.
 * @param onClick called when this menu item is clicked.
 * @param text text of the menu item.
 * @param shapes [MenuItemShapes] that will be used to resolve the shapes for this menu item. The
 *   shape of this item is determined by the value of [selected]. The shapes provided should be
 *   determined by the number of items in the group or menu as well as the item's position in the
 *   menu. There is a convenience function that can be used to easily determine the shape to be used
 *   at [MenuDefaults.itemShape]
 * @param modifier the [Modifier] to be applied to this menu item.
 * @param leadingIcon optional leading icon to be displayed when the item is unchecked.
 * @param selectedLeadingIcon optional leading icon to be displayed when the item is selected.
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input.
 * @param colors [MenuItemColors] that will be used to resolve the colors for this menu item. There
 *   are two predefined [MenuItemColors] at [MenuDefaults.selectableItemColors] and
 *   [MenuDefaults.selectableItemVibrantColors] which you can use or modify.
 * @param contentPadding the padding applied to the content of this menu item.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item.
 * @param supportingText optional supporting text of the menu item.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun DropdownMenuItem(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    shapes: MenuItemShapes,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    selectedLeadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.selectableItemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuSelectableItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
    supportingText: @Composable (() -> Unit)? = null,
) {
    DropdownMenuItemContent(
        text = text,
        selected = selected,
        onClick = onClick,
        modifier = modifier.semantics { role = Role.RadioButton },
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        selectedLeadingIcon = selectedLeadingIcon,
        enabled = enabled,
        colors = colors,
        shapes = shapes,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}

// TODO: Consider to move into public [MenuDefaults]
internal expect val DefaultMenuProperties: PopupProperties

/**
 * Represents the text and icon colors used in a menu item at different states.
 *
 * @param textColor the text color of this [DropdownMenuItemContent] when enabled
 * @param leadingIconColor the leading icon color of this [DropdownMenuItemContent] when enabled
 * @param trailingIconColor the trailing icon color of this [DropdownMenuItemContent] when enabled
 * @param disabledTextColor the text color of this [DropdownMenuItemContent] when not enabled
 * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItemContent] when not
 *   enabled
 * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItemContent] when
 *   not enabled
 * @param containerColor the container color of this menu item when enabled and unselected
 * @param disabledContainerColor the container color of this menu item when not enabled
 * @param selectedTextColor the text color of this menu item when enabled and selected
 * @param selectedContainerColor the container color of this menu item when enabled and selected
 * @param selectedLeadingIconColor the leading icon color of this menu item when enabled and
 *   selected
 * @param selectedTrailingIconColor the trailing icon color of this menu item when enabled and
 *   selected
 * @constructor create an instance with arbitrary colors. See [MenuDefaults.itemColors] for the
 *   default colors used in a [DropdownMenuItemContent].
 */
@Immutable
class MenuItemColors
@ExperimentalMaterial3ExpressiveApi
constructor(
    val textColor: Color,
    val leadingIconColor: Color,
    val trailingIconColor: Color,
    val disabledTextColor: Color,
    val disabledLeadingIconColor: Color,
    val disabledTrailingIconColor: Color,
    containerColor: Color,
    disabledContainerColor: Color,
    selectedTextColor: Color,
    selectedLeadingIconColor: Color,
    selectedTrailingIconColor: Color,
    selectedContainerColor: Color,
) {

    /** The container color of this menu item when enabled and unselected. */
    @ExperimentalMaterial3ExpressiveApi val containerColor: Color = containerColor

    /** The container color of this menu item when not enabled */
    @ExperimentalMaterial3ExpressiveApi val disabledContainerColor = disabledContainerColor

    /** The container color of this menu item when enabled and selected. */
    @ExperimentalMaterial3ExpressiveApi val selectedContainerColor: Color = selectedContainerColor

    /** The text color of this menu item when enabled and selected. */
    @ExperimentalMaterial3ExpressiveApi val selectedTextColor: Color = selectedTextColor

    /** The leading icon color of this menu item when enabled and selected. */
    @ExperimentalMaterial3ExpressiveApi
    val selectedLeadingIconColor: Color = selectedLeadingIconColor

    /** The trailing icon color of this menu item when enabled and selected. */
    @ExperimentalMaterial3ExpressiveApi
    val selectedTrailingIconColor: Color = selectedTrailingIconColor

    /**
     * Creates an instance with colors for a standard menu item.
     *
     * This constructor is used for [DropdownMenuItem].
     *
     * @param textColor the text color of this menu item when enabled
     * @param leadingIconColor the leading icon color of this menu item when enabled
     * @param trailingIconColor the trailing icon color of this menu item when enabled
     * @param disabledTextColor the text color of this menu item when not enabled
     * @param disabledLeadingIconColor the leading icon color of this menu item when not enabled
     * @param disabledTrailingIconColor the trailing icon color of this menu item when not enabled
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    constructor(
        textColor: Color,
        leadingIconColor: Color,
        trailingIconColor: Color,
        disabledTextColor: Color,
        disabledLeadingIconColor: Color,
        disabledTrailingIconColor: Color,
    ) : this(
        textColor = textColor,
        leadingIconColor = leadingIconColor,
        trailingIconColor = trailingIconColor,
        disabledTextColor = disabledTextColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        containerColor = Color.Unspecified,
        disabledContainerColor = Color.Unspecified,
        selectedTextColor = Color.Unspecified,
        selectedLeadingIconColor = Color.Unspecified,
        selectedTrailingIconColor = Color.Unspecified,
        selectedContainerColor = Color.Unspecified,
    )

    /**
     * Returns a copy of this MenuItemColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    @ExperimentalMaterial3ExpressiveApi
    fun copy(
        textColor: Color = this.textColor,
        containerColor: Color = this.containerColor,
        leadingIconColor: Color = this.leadingIconColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledTextColor: Color = this.disabledTextColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
        selectedTextColor: Color = this.selectedTextColor,
        selectedContainerColor: Color = this.selectedContainerColor,
        selectedLeadingIconColor: Color = this.selectedLeadingIconColor,
        selectedTrailingIconColor: Color = this.selectedTrailingIconColor,
    ) =
        MenuItemColors(
            textColor.takeOrElse { this.textColor },
            leadingIconColor.takeOrElse { this.leadingIconColor },
            trailingIconColor.takeOrElse { this.trailingIconColor },
            disabledTextColor.takeOrElse { this.disabledTextColor },
            disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
            disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
            containerColor.takeOrElse { this.containerColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            selectedTextColor.takeOrElse { this.selectedTextColor },
            selectedLeadingIconColor.takeOrElse { this.selectedLeadingIconColor },
            selectedTrailingIconColor.takeOrElse { this.selectedTrailingIconColor },
            selectedContainerColor.takeOrElse { this.selectedContainerColor },
        )

    /**
     * Returns a copy of this MenuItemColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        textColor: Color = this.textColor,
        leadingIconColor: Color = this.leadingIconColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledTextColor: Color = this.disabledTextColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
    ) =
        MenuItemColors(
            textColor.takeOrElse { this.textColor },
            leadingIconColor.takeOrElse { this.leadingIconColor },
            trailingIconColor.takeOrElse { this.trailingIconColor },
            disabledTextColor.takeOrElse { this.disabledTextColor },
            disabledLeadingIconColor.takeOrElse { this.disabledLeadingIconColor },
            disabledTrailingIconColor.takeOrElse { this.disabledTrailingIconColor },
        )

    /**
     * Represents the text color for a menu item, depending on its [enabled] state.
     *
     * @param enabled whether the menu item is enabled
     * @param selected whether the menu item is selected.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Stable
    internal fun textColor(enabled: Boolean, selected: Boolean = false): Color {
        return if (enabled) {
            if (selected) {
                selectedTextColor
            } else {
                textColor
            }
        } else {
            disabledTextColor
        }
    }

    /**
     * Represents the leading icon color for a menu item, depending on its [enabled] state.
     *
     * @param enabled whether the menu item is enabled
     * @param selected whether the menu item is selected.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Stable
    internal fun leadingIconColor(enabled: Boolean, selected: Boolean = false): Color {
        return if (enabled) {
            if (selected) {
                selectedLeadingIconColor
            } else {
                leadingIconColor
            }
        } else {
            disabledLeadingIconColor
        }
    }

    /**
     * Represents the trailing icon color for a menu item, depending on its [enabled] state.
     *
     * @param enabled whether the menu item is enabled
     * @param selected whether the menu item is selected.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Stable
    internal fun trailingIconColor(enabled: Boolean, selected: Boolean = false): Color {
        return if (enabled) {
            if (selected) {
                selectedTrailingIconColor
            } else {
                trailingIconColor
            }
        } else {
            disabledTrailingIconColor
        }
    }

    /**
     * Represents the container color for a menu item, depending on its [enabled] and [selected]
     * state.
     *
     * @param enabled whether the menu item is enabled.
     * @param selected whether the menu item is selected.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Stable
    internal fun containerColor(enabled: Boolean, selected: Boolean = false): Color {
        return if (enabled) {
            if (selected) {
                selectedContainerColor
            } else {
                containerColor
            }
        } else {
            disabledContainerColor
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MenuItemColors) return false

        if (textColor != other.textColor) return false
        if (containerColor != other.containerColor) return false
        if (leadingIconColor != other.leadingIconColor) return false
        if (trailingIconColor != other.trailingIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedTextColor != other.selectedTextColor) return false
        if (selectedLeadingIconColor != other.selectedLeadingIconColor) return false
        if (selectedTrailingIconColor != other.selectedTrailingIconColor) return false

        return true
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun hashCode(): Int {
        var result = textColor.hashCode()
        result = 31 * result + containerColor.hashCode()
        result = 31 * result + leadingIconColor.hashCode()
        result = 31 * result + trailingIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + selectedContainerColor.hashCode()
        result = 31 * result + selectedTextColor.hashCode()
        result = 31 * result + selectedLeadingIconColor.hashCode()
        result = 31 * result + selectedTrailingIconColor.hashCode()
        return result
    }
}

/**
 * Represents the shapes used for a [DropdownMenuItem] in its various states.
 *
 * @param shape the [Shape] to use when the item is unselected.
 * @param selectedShape the [Shape] to use when the item is selected.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
class MenuItemShapes(val shape: Shape, val selectedShape: Shape) {
    /** Returns a copy of this MenuItemShapes, optionally overriding some of the values. */
    fun copy(shape: Shape? = this.shape, selectedShape: Shape? = this.selectedShape) =
        MenuItemShapes(
            shape = shape.takeOrElse { this.shape },
            selectedShape = selectedShape.takeOrElse { this.selectedShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MenuItemShapes) return false

        return shape == other.shape && selectedShape == other.selectedShape
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + selectedShape.hashCode()

        return result
    }
}

/**
 * Represents the shapes used for a [DropdownMenuGroup].
 *
 * @param shape the default [Shape] to use for the group.
 * @param inactiveShape the [Shape] to use when the group has stop being hovered.
 */
@ExperimentalMaterial3ExpressiveApi
@Immutable
class MenuGroupShapes(val shape: Shape, val inactiveShape: Shape) {
    /** Returns a copy of this MenuGroupShapes, optionally overriding some of the values. */
    fun copy(shape: Shape? = this.shape, inactiveShape: Shape? = this.inactiveShape) =
        MenuGroupShapes(
            shape = shape.takeOrElse { this.shape },
            inactiveShape = inactiveShape.takeOrElse { this.inactiveShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MenuGroupShapes) return false

        return shape == other.shape && inactiveShape == other.inactiveShape
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + inactiveShape.hashCode()

        return result
    }
}

@Composable
internal fun DropdownMenuContent(
    modifier: Modifier,
    expandedState: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    scrollState: ScrollState,
    shape: Shape,
    containerColor: Color,
    tonalElevation: Dp,
    shadowElevation: Dp,
    border: BorderStroke?,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Menu open/close animation.
    @Suppress("DEPRECATION") val transition = updateTransition(expandedState, "DropDownMenu")
    // TODO Load the motionScheme tokens from the component tokens file
    val scaleAnimationSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val alphaAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
    val scale by
        transition.animateFloat(transitionSpec = { scaleAnimationSpec }) { expanded ->
            if (expanded) ExpandedScaleTarget else ClosedScaleTarget
        }

    val alpha by
        transition.animateFloat(transitionSpec = { alphaAnimationSpec }) { expanded ->
            if (expanded) ExpandedAlphaTarget else ClosedAlphaTarget
        }

    val isInspecting = LocalInspectionMode.current
    Surface(
        modifier =
            Modifier.graphicsLayer {
                scaleX =
                    if (!isInspecting) scale
                    else if (expandedState.targetState) ExpandedScaleTarget else ClosedScaleTarget
                scaleY =
                    if (!isInspecting) scale
                    else if (expandedState.targetState) ExpandedScaleTarget else ClosedScaleTarget
                this.alpha =
                    if (!isInspecting) alpha
                    else if (expandedState.targetState) ExpandedAlphaTarget else ClosedAlphaTarget
                transformOrigin = transformOriginState.value
            },
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
    ) {
        Column(
            modifier =
                modifier
                    .padding(vertical = DropdownMenuVerticalPadding)
                    .width(IntrinsicSize.Max)
                    .verticalScroll(scrollState),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DropdownMenuItemContent(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier,
    supportingText: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    selectedLeadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    enabled: Boolean,
    colors: MenuItemColors,
    shapes: MenuItemShapes,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    // TODO Load the motionScheme tokens from the component tokens file
    val expandAndShrinkSpec = MotionSchemeKeyTokens.FastSpatial.value<IntSize>()
    val fadeInAndOutSpec = MotionSchemeKeyTokens.FastEffects.value<Float>()
    val morphSpec = MotionSchemeKeyTokens.FastSpatial.value<Float>()
    val colorAnimationSpec = MotionSchemeKeyTokens.FastEffects.value<Color>()

    val containerColor = colors.containerColor(enabled = enabled, selected = selected)
    val animatedContainerColor by
        animateColorAsState(targetValue = containerColor, animationSpec = colorAnimationSpec)
    val itemShape = shapeByInteraction(shapes, selected, morphSpec)

    val hasLeadingIcon = leadingIcon != null || selectedLeadingIcon != null
    val hasTrailingIcon = trailingIcon != null

    Surface(
        selected = selected,
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    if (supportingText != null) {
                        DropdownMenuSelectableItemWithSupportTexPadding
                    } else {
                        DropdownMenuSelectableItemPadding
                    }
                ),
        enabled = enabled,
        shape = itemShape,
        color = animatedContainerColor,
        interactionSource = interactionSource,
    ) {
        // TODO replace with token
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Layout(
                modifier =
                    Modifier.sizeIn(
                            minWidth = DropdownMenuItemDefaultMinWidth,
                            maxWidth = DropdownMenuItemDefaultMaxWidth,
                            minHeight = SegmentedMenuTokens.Item,
                        )
                        .padding(contentPadding),
                content = {
                    if (hasLeadingIcon) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.leadingIconColor(enabled, selected)
                        ) {
                            Box(
                                modifier = Modifier.layoutId(LeadingIconLayoutId),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selectedLeadingIcon != null) {
                                    if (leadingIcon == null) {
                                        AnimatedVisibility(
                                            visible = selected,
                                            // Defines the animation when the icon enters the
                                            // composition.
                                            // It expands horizontally and fades in.
                                            enter =
                                                expandHorizontally(
                                                    animationSpec = expandAndShrinkSpec
                                                ) + fadeIn(animationSpec = fadeInAndOutSpec),
                                            // Defines the animation when the icon exits the
                                            // composition.
                                            // It shrinks horizontally and fades out.
                                            exit =
                                                shrinkHorizontally(
                                                    animationSpec = expandAndShrinkSpec
                                                ) + fadeOut(animationSpec = fadeInAndOutSpec),
                                        ) {
                                            WrappedLeadingIcon { selectedLeadingIcon() }
                                        }
                                    } else if (selected) {
                                        WrappedLeadingIcon { selectedLeadingIcon() }
                                    } else {
                                        WrappedLeadingIcon { leadingIcon() }
                                    }
                                } else {
                                    WrappedLeadingIcon { leadingIcon!!.invoke() }
                                }
                            }
                        }
                    }

                    CompositionLocalProvider(
                        LocalContentColor provides colors.textColor(enabled, selected)
                    ) {
                        Box(
                            Modifier.layoutId(TextLayoutId)
                                .padding(
                                    end =
                                        if (hasTrailingIcon) {
                                            DropdownMenuIconTextPadding
                                        } else {
                                            0.dp
                                        }
                                ),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (supportingText != null) {
                                LabelWithSupportingText(
                                    supportingText = supportingText,
                                    modifier = Modifier.layoutId(TextLayoutId),
                                    content = text,
                                )
                            } else {
                                text()
                            }
                        }
                    }

                    if (hasTrailingIcon) {
                        CompositionLocalProvider(
                            LocalContentColor provides colors.trailingIconColor(enabled, selected)
                        ) {
                            Box(
                                Modifier.layoutId(TrailingIconLayoutId)
                                    .defaultMinSize(
                                        minWidth = SegmentedMenuTokens.ItemTrailingIconSize
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                trailingIcon()
                            }
                        }
                    }

                    // for measurement for trailing icon if provided
                    if (hasLeadingIcon) {
                        Box(modifier = Modifier.layoutId(GhostLeadingIconLayoutId)) {
                            WrappedLeadingIcon {
                                if (leadingIcon != null) {
                                    leadingIcon()
                                } else {
                                    selectedLeadingIcon!!.invoke()
                                }
                            }
                        }
                    }
                },
                measurePolicy =
                    DropdownMenuItemMeasurePolicy(
                        leadingIcon != null || selectedLeadingIcon != null,
                        trailingIcon != null,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val MenuItemShapes.hasRoundedCornerShapes: Boolean
    get() = shape is RoundedCornerShape && selectedShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val MenuGroupShapes.hasRoundedCornerShapes: Boolean
    get() = shape is RoundedCornerShape && inactiveShape is RoundedCornerShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val MenuItemShapes.hasCornerBasedShapes: Boolean
    get() = shape is CornerBasedShape && selectedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val MenuGroupShapes.hasCornerBasedShapes: Boolean
    get() = shape is CornerBasedShape && inactiveShape is CornerBasedShape

@Composable
internal fun DropdownMenuItemContent(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    enabled: Boolean,
    colors: MenuItemColors,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
) {
    Row(
        modifier =
            modifier
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    interactionSource = interactionSource,
                    indication = ripple(true),
                )
                .fillMaxWidth()
                // Preferred min and max width used during the intrinsic measurement.
                .sizeIn(
                    minWidth = DropdownMenuItemDefaultMinWidth,
                    maxWidth = DropdownMenuItemDefaultMaxWidth,
                    minHeight = MenuListItemContainerHeight,
                )
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TODO(b/271818892): Align menu list item style with general list item style.
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.leadingIconColor(enabled)
                ) {
                    Box(Modifier.defaultMinSize(minWidth = ListTokens.ItemLeadingIconSize)) {
                        leadingIcon()
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides colors.textColor(enabled)) {
                Box(
                    Modifier.weight(1f)
                        .padding(
                            start =
                                if (leadingIcon != null) {
                                    DropdownMenuItemHorizontalPadding
                                } else {
                                    0.dp
                                },
                            end =
                                if (trailingIcon != null) {
                                    DropdownMenuItemHorizontalPadding
                                } else {
                                    0.dp
                                },
                        )
                ) {
                    text()
                }
            }
            if (trailingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.trailingIconColor(enabled)
                ) {
                    Box(Modifier.defaultMinSize(minWidth = ListTokens.ItemTrailingIconSize)) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

internal fun calculateTransformOrigin(anchorBounds: IntRect, menuBounds: IntRect): TransformOrigin {
    val pivotX =
        when {
            menuBounds.left >= anchorBounds.right -> 0f
            menuBounds.right <= anchorBounds.left -> 1f
            menuBounds.width == 0 -> 0f
            else -> {
                val intersectionCenter =
                    (max(anchorBounds.left, menuBounds.left) +
                        min(anchorBounds.right, menuBounds.right)) / 2
                (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
            }
        }
    val pivotY =
        when {
            menuBounds.top >= anchorBounds.bottom -> 0f
            menuBounds.bottom <= anchorBounds.top -> 1f
            menuBounds.height == 0 -> 0f
            else -> {
                val intersectionCenter =
                    (max(anchorBounds.top, menuBounds.top) +
                        min(anchorBounds.bottom, menuBounds.bottom)) / 2
                (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
            }
        }
    return TransformOrigin(pivotX, pivotY)
}

/**
 * [Column] of a label and its supporting text. Used in a [DropdownMenuItem]'s text parameter when a
 * supporting text is desired.
 *
 * @param supportingText the supporting text of the label.
 * @param content the content of the label.
 */
@Composable
private fun LabelWithSupportingText(
    supportingText: @Composable () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // TODO replace the typography with token when available
    Column(modifier = modifier) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge, content = content)
        ProvideTextStyle(MaterialTheme.typography.bodyMedium, content = supportingText)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun shapeByInteraction(
    shapes: MenuItemShapes,
    selected: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        if (selected) {
            shapes.selectedShape
        } else {
            shapes.shape
        }

    if (shapes.hasRoundedCornerShapes)
        return key(shapes.shape, shapes.selectedShape) {
            rememberAnimatedShape(shape as RoundedCornerShape, animationSpec)
        }
    else if (shapes.hasCornerBasedShapes)
        return key(shapes.shape, shapes.selectedShape) {
            rememberAnimatedShape(shape as CornerBasedShape, animationSpec)
        }

    return shape
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun shapeByInteraction(
    shapes: MenuGroupShapes,
    hasBeenHovered: Boolean,
    hovered: Boolean,
    animationSpec: FiniteAnimationSpec<Float>,
): Shape {
    val shape =
        if (hasBeenHovered && !hovered) {
            shapes.inactiveShape
        } else {
            shapes.shape
        }

    if (shapes.hasRoundedCornerShapes)
        return key(shapes.shape, shapes.inactiveShape) {
            rememberAnimatedShape(shape as RoundedCornerShape, animationSpec)
        }
    else if (shapes.hasCornerBasedShapes)
        return key(shapes.shape, shapes.inactiveShape) {
            rememberAnimatedShape(shape as CornerBasedShape, animationSpec)
        }

    return shape
}

@Composable
private fun WrappedLeadingIcon(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier =
            Modifier.defaultMinSize(minWidth = SegmentedMenuTokens.ItemLeadingIconSize)
                .padding(end = DropdownMenuIconTextPadding),
        content = content,
    )
}

/**
 * A [MeasurePolicy] for [DropdownMenuItemContent] that handles the layout and alignment of the
 * leading icon, text, and trailing icon.
 *
 * This policy correctly accounts for the space needed by icons, even when the leading icon is
 * animating in or out.
 */
private class DropdownMenuItemMeasurePolicy(
    val hasLeadingIcon: Boolean,
    val hasTrailingIcon: Boolean,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        return if (!hasLeadingIcon && !hasTrailingIcon) {
            JustTextMeasureResult(measurables, constraints)
        } else if (!hasTrailingIcon) {
            NoTrailingIconMeasureResult(measurables, constraints)
        } else if (!hasLeadingIcon) {
            NoLeadingIconMeasureResult(measurables, constraints)
        } else {
            DefaultMeasureResult(measurables, constraints)
        }
    }

    fun MeasureScope.JustTextMeasureResult(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val mainContentPlaceable =
            measurables
                .fastFirst { it.layoutId == TextLayoutId }
                .measure(constraints.copy(minWidth = 0))

        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                // If unbounded, the total width is the sum of the measured static parts.
                mainContentPlaceable.width
            }
        val height = maxOf(constraints.minHeight, mainContentPlaceable.height)

        return layout(width, height) {
            mainContentPlaceable.placeRelative(
                x = 0,
                y =
                    Alignment.CenterVertically.align(
                        size = mainContentPlaceable.height,
                        space = height,
                    ),
            )
        }
    }

    fun MeasureScope.NoLeadingIconMeasureResult(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val trailingPlaceable =
            measurables
                .fastFirst { it.layoutId == TrailingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))

        val mainContentConstraints =
            if (constraints.hasBoundedWidth) {
                val mainContentMaxWidth =
                    (constraints.maxWidth - trailingPlaceable.width).coerceAtLeast(0)
                Constraints.fixedWidth(mainContentMaxWidth)
            } else {
                // If width is unbounded, let the main content measure itself freely.
                constraints.copy(minWidth = 0)
            }

        val mainPlaceable =
            measurables.fastFirst { it.layoutId == TextLayoutId }.measure(mainContentConstraints)

        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                // If unbounded, the total width is the sum of the measured static parts.
                trailingPlaceable.width + mainPlaceable.width
            }

        val height =
            maxOf(constraints.minHeight, max(trailingPlaceable.height, mainPlaceable.height))

        return layout(width, height) {
            mainPlaceable.placeRelative(
                x = 0,
                y = Alignment.CenterVertically.align(size = mainPlaceable.height, space = height),
            )

            trailingPlaceable.placeRelative(
                x = width - trailingPlaceable.width,
                y =
                    Alignment.CenterVertically.align(
                        size = trailingPlaceable.height,
                        space = height,
                    ),
            )
        }
    }

    fun MeasureScope.NoTrailingIconMeasureResult(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val leadingPlaceable =
            measurables
                .fastFirst { it.layoutId == LeadingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))
        val ghostPlaceable =
            measurables
                .fastFirst { it.layoutId == GhostLeadingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))

        val mainContentConstraints =
            if (constraints.hasBoundedWidth) {
                val mainContentMaxWidth =
                    (constraints.maxWidth - ghostPlaceable.width).coerceAtLeast(0)
                Constraints.fixedWidth(mainContentMaxWidth)
            } else {
                // If width is unbounded, let the main content measure itself freely.
                constraints.copy(minWidth = 0)
            }
        val mainPlaceable =
            measurables.fastFirst { it.layoutId == TextLayoutId }.measure(mainContentConstraints)

        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                // If unbounded, the total width is the sum of the measured static parts.
                ghostPlaceable.width + mainPlaceable.width
            }
        val height =
            maxOf(constraints.minHeight, max(leadingPlaceable.height, mainPlaceable.height))
        return layout(width, height) {
            leadingPlaceable.placeRelative(
                x = 0,
                y = Alignment.CenterVertically.align(size = leadingPlaceable.height, space = height),
            )

            mainPlaceable.placeRelative(
                x = leadingPlaceable.width,
                y = Alignment.CenterVertically.align(size = mainPlaceable.height, space = height),
            )
        }
    }

    fun MeasureScope.DefaultMeasureResult(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val leadingPlaceable =
            measurables
                .fastFirst { it.layoutId == LeadingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))
        val trailingPlaceable =
            measurables
                .fastFirst { it.layoutId == TrailingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))
        val ghostPlaceable =
            measurables
                .fastFirst { it.layoutId == GhostLeadingIconLayoutId }
                .measure(constraints.copy(minWidth = 0))

        val mainContentConstraints =
            if (constraints.hasBoundedWidth) {
                val mainContentMaxWidth =
                    (constraints.maxWidth - ghostPlaceable.width - trailingPlaceable.width)
                        .coerceAtLeast(0)
                Constraints.fixedWidth(mainContentMaxWidth)
            } else {
                // If width is unbounded, let the main content measure itself freely.
                constraints.copy(minWidth = 0)
            }
        val mainPlaceable =
            measurables.fastFirst { it.layoutId == TextLayoutId }.measure(mainContentConstraints)

        val width =
            if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                // If unbounded, the total width is the sum of the measured static parts.
                ghostPlaceable.width + mainPlaceable.width + trailingPlaceable.width
            }
        val height =
            maxOf(
                constraints.minHeight,
                maxOf(leadingPlaceable.height, mainPlaceable.height, trailingPlaceable.height),
            )
        return layout(width, height) {
            leadingPlaceable.placeRelative(
                x = 0,
                y = Alignment.CenterVertically.align(size = leadingPlaceable.height, space = height),
            )

            mainPlaceable.placeRelative(
                x = leadingPlaceable.width,
                y = Alignment.CenterVertically.align(size = mainPlaceable.height, space = height),
            )

            trailingPlaceable.placeRelative(
                x = width - trailingPlaceable.width,
                y =
                    Alignment.CenterVertically.align(
                        size = trailingPlaceable.height,
                        space = height,
                    ),
            )
        }
    }
}

// Size defaults.
internal val MenuVerticalMargin = 48.dp
internal val MenuHorizontalMargin = 8.dp
private val MenuListItemContainerHeight = 48.dp
internal val DropdownMenuItemHorizontalPadding = 12.dp
internal val DropdownMenuGroupVerticalPadding = 2.dp

private val DropdownMenuSelectableItemPadding = PaddingValues(horizontal = 4.dp)
private val DropdownMenuSelectableItemWithSupportTexPadding =
    PaddingValues(horizontal = 4.dp, vertical = 2.dp)
private val DropdownMenuIconTextPadding = 8.dp
internal val DropdownMenuVerticalPadding = 8.dp
internal val DropdownMenuItemDefaultMinWidth = 112.dp
internal val DropdownMenuItemDefaultMaxWidth = 280.dp

internal val DropdownMenuGroupDefaultMinHeight = 32.dp

private const val LeadingIconLayoutId = "leadingIcon"
private const val TextLayoutId = "text"
private const val TrailingIconLayoutId = "trailingIcon"
private const val GhostLeadingIconLayoutId = "ghostLeadingIcon"

// Menu open/close animation.
internal const val ExpandedScaleTarget = 1f
internal const val ClosedScaleTarget = 0.8f
internal const val ExpandedAlphaTarget = 1f
internal const val ClosedAlphaTarget = 0f
