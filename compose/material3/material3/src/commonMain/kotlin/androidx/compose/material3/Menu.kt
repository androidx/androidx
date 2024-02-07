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

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.tokens.MenuTokens
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.max
import kotlin.math.min

/**
 * Contains default values used for [DropdownMenuItem].
 */
object MenuDefaults {

    /**
     * Creates a [MenuItemColors] that represents the default text and icon colors used in a
     * [DropdownMenuItemContent].
     */
    @Composable
    fun itemColors() = MaterialTheme.colorScheme.defaultMenuItemColors

    /**
     * Creates a [MenuItemColors] that represents the default text and icon colors used in a
     * [DropdownMenuItemContent].
     *
     * @param textColor the text color of this [DropdownMenuItemContent] when enabled
     * @param leadingIconColor the leading icon color of this [DropdownMenuItemContent] when enabled
     * @param trailingIconColor the trailing icon color of this [DropdownMenuItemContent] when
     * enabled
     * @param disabledTextColor the text color of this [DropdownMenuItemContent] when not enabled
     * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItemContent] when
     * not enabled
     * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItemContent]
     * when not enabled
     */
    @Composable
    fun itemColors(
        textColor: Color = Color.Unspecified,
        leadingIconColor: Color = Color.Unspecified,
        trailingIconColor: Color = Color.Unspecified,
        disabledTextColor: Color = Color.Unspecified,
        disabledLeadingIconColor: Color = Color.Unspecified,
        disabledTrailingIconColor: Color = Color.Unspecified,
    ): MenuItemColors = MaterialTheme.colorScheme.defaultMenuItemColors.copy(
        textColor = textColor,
        leadingIconColor = leadingIconColor,
        trailingIconColor = trailingIconColor,
        disabledTextColor = disabledTextColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
    )

    internal val ColorScheme.defaultMenuItemColors: MenuItemColors
        get() {
            return defaultMenuItemColorsCached ?: MenuItemColors(
                textColor = fromToken(MenuTokens.ListItemLabelTextColor),
                leadingIconColor = fromToken(MenuTokens.ListItemLeadingIconColor),
                trailingIconColor = fromToken(MenuTokens.ListItemTrailingIconColor),
                disabledTextColor = fromToken(MenuTokens.ListItemDisabledLabelTextColor)
                .copy(alpha = MenuTokens.ListItemDisabledLabelTextOpacity),
                disabledLeadingIconColor = fromToken(MenuTokens.ListItemDisabledLeadingIconColor)
                .copy(alpha = MenuTokens.ListItemDisabledLeadingIconOpacity),
                disabledTrailingIconColor = fromToken(MenuTokens.ListItemDisabledTrailingIconColor)
                .copy(alpha = MenuTokens.ListItemDisabledTrailingIconOpacity),
            ).also {
                defaultMenuItemColorsCached = it
            }
        }

    /**
     * Default padding used for [DropdownMenuItem].
     */
    val DropdownMenuItemContentPadding = PaddingValues(
        horizontal = DropdownMenuItemHorizontalPadding,
        vertical = 0.dp
    )
}

/**
 * Represents the text and icon colors used in a menu item at different states.
 *
 * @constructor create an instance with arbitrary colors.
 * See [MenuDefaults.itemColors] for the default colors used in a [DropdownMenuItemContent].
 *
 * @param textColor the text color of this [DropdownMenuItemContent] when enabled
 * @param leadingIconColor the leading icon color of this [DropdownMenuItemContent] when enabled
 * @param trailingIconColor the trailing icon color of this [DropdownMenuItemContent] when
 * enabled
 * @param disabledTextColor the text color of this [DropdownMenuItemContent] when not enabled
 * @param disabledLeadingIconColor the leading icon color of this [DropdownMenuItemContent] when
 * not enabled
 * @param disabledTrailingIconColor the trailing icon color of this [DropdownMenuItemContent]
 * when not enabled
 */
@Immutable
class MenuItemColors(
    val textColor: Color,
    val leadingIconColor: Color,
    val trailingIconColor: Color,
    val disabledTextColor: Color,
    val disabledLeadingIconColor: Color,
    val disabledTrailingIconColor: Color,
) {

    /**
     * Returns a copy of this MenuItemColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        textColor: Color = this.textColor,
        leadingIconColor: Color = this.leadingIconColor,
        trailingIconColor: Color = this.trailingIconColor,
        disabledTextColor: Color = this.disabledTextColor,
        disabledLeadingIconColor: Color = this.disabledLeadingIconColor,
        disabledTrailingIconColor: Color = this.disabledTrailingIconColor,
    ) = MenuItemColors(
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
     */
    @Composable
    internal fun textColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) textColor else disabledTextColor)
    }

    /**
     * Represents the leading icon color for a menu item, depending on its [enabled] state.
     *
     * @param enabled whether the menu item is enabled
     */
    @Composable
    internal fun leadingIconColor(enabled: Boolean): Color =
        if (enabled) leadingIconColor else disabledLeadingIconColor

    /**
     * Represents the trailing icon color for a menu item, depending on its [enabled] state.
     *
     * @param enabled whether the menu item is enabled
     */
    @Composable
    internal fun trailingIconColor(enabled: Boolean): Color =
        if (enabled) trailingIconColor else disabledTrailingIconColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is MenuItemColors) return false

        if (textColor != other.textColor) return false
        if (leadingIconColor != other.leadingIconColor) return false
        if (trailingIconColor != other.trailingIconColor) return false
        if (disabledTextColor != other.disabledTextColor) return false
        if (disabledLeadingIconColor != other.disabledLeadingIconColor) return false
        if (disabledTrailingIconColor != other.disabledTrailingIconColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = textColor.hashCode()
        result = 31 * result + leadingIconColor.hashCode()
        result = 31 * result + trailingIconColor.hashCode()
        result = 31 * result + disabledTextColor.hashCode()
        result = 31 * result + disabledLeadingIconColor.hashCode()
        result = 31 * result + disabledTrailingIconColor.hashCode()
        return result
    }
}

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external" target="_blank">Material Design dropdown menu</a>.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * A [DropdownMenu] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [DropdownMenu] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [DropdownMenu] by itself will not take up any
 * space in a layout, as the menu is displayed in a separate window, on top of other content.
 *
 * The [content] of a [DropdownMenu] will typically be [DropdownMenuItem]s, as well as custom
 * content. Using [DropdownMenuItem]s will result in a menu that matches the Material
 * specification for menus. Also note that the [content] is placed inside a scrollable [Column],
 * so using a [LazyColumn] as the root layout inside [content] is unsupported.
 *
 * [onDismissRequest] will be called when the menu should close - for example when there is a
 * tap outside the menu, or when the back key is pressed.
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
 * @sample androidx.compose.material3.samples.MenuSample
 *
 * Example usage with a [ScrollState] to control the menu items scroll position:
 * @sample androidx.compose.material3.samples.MenuWithScrollStateSample
 *
 * @param expanded whether the menu is expanded or not
 * @param onDismissRequest called when the user requests to dismiss the menu, such as by tapping
 * outside the menu's bounds
 * @param modifier [Modifier] to be applied to the menu's content
 * @param offset [DpOffset] from the original position of the menu. The offset respects the
 * [LayoutDirection], so the offset's x position will be added in LTR and subtracted in RTL.
 * @param scrollState a [ScrollState] to used by the menu's content for items vertical scrolling
 * @param properties [PopupProperties] for further customization of this popup's behavior
 * @param content the content of this dropdown menu, typically a [DropdownMenuItem]
 */
@Composable
expect fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
)

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external" target="_blank">Material Design dropdown menu</a> item.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * Example usage:
 * @sample androidx.compose.material3.samples.MenuSample
 *
 * @param text text of the menu item
 * @param onClick called when this menu item is clicked
 * @param modifier the [Modifier] to be applied to this menu item
 * @param leadingIcon optional leading icon to be displayed at the beginning of the item's text
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text. This
 * trailing icon slot can also accept [Text] to indicate a keyboard shortcut.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [MenuItemColors] that will be used to resolve the colors used for this menu item in
 * different states. See [MenuDefaults.itemColors].
 * @param contentPadding the padding applied to the content of this menu item
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this menu item. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this menu item in different states.
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
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
)

@Suppress("ModifierParameter")
@Composable
internal fun DropdownMenuContent(
    expandedState: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Menu open/close animation.
    @Suppress("DEPRECATION")
    val transition = updateTransition(expandedState, "DropDownMenu")

    val scale by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(
                    durationMillis = InTransitionDuration,
                    easing = LinearOutSlowInEasing
                )
            } else {
                // Expanded to dismissed.
                tween(
                    durationMillis = 1,
                    delayMillis = OutTransitionDuration - 1
                )
            }
        }
    ) { expanded ->
        if (expanded) 1f else 0.8f
    }

    val alpha by transition.animateFloat(
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = 30)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = OutTransitionDuration)
            }
        }
    ) { expanded ->
        if (expanded) 1f else 0f
    }

    Surface(
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            transformOrigin = transformOriginState.value
        },
        shape = MenuTokens.ContainerShape.value,
        color = MaterialTheme.colorScheme.fromToken(MenuTokens.ContainerColor),
        tonalElevation = MenuTokens.ContainerElevation,
        shadowElevation = MenuTokens.ContainerElevation
    ) {
        Column(
            modifier = modifier
                .padding(vertical = DropdownMenuVerticalPadding)
                .width(IntrinsicSize.Max)
                .verticalScroll(scrollState),
            content = content
        )
    }
}

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
    interactionSource: MutableInteractionSource
) {
    @Suppress("DEPRECATION_ERROR")
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = androidx.compose.material.ripple.rememberRipple(true)
            )
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .sizeIn(
                minWidth = DropdownMenuItemDefaultMinWidth,
                maxWidth = DropdownMenuItemDefaultMaxWidth,
                minHeight = MenuTokens.ListItemContainerHeight
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProvideTextStyle(MaterialTheme.typography.fromToken(MenuTokens.ListItemLabelTextFont)) {
            if (leadingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.leadingIconColor(enabled),
                ) {
                    Box(Modifier.defaultMinSize(minWidth = MenuTokens.ListItemLeadingIconSize)) {
                        leadingIcon()
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides colors.textColor(enabled).value) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(
                            start = if (leadingIcon != null) {
                                DropdownMenuItemHorizontalPadding
                            } else {
                                0.dp
                            },
                            end = if (trailingIcon != null) {
                                DropdownMenuItemHorizontalPadding
                            } else {
                                0.dp
                            }
                        )
                ) {
                    text()
                }
            }
            if (trailingIcon != null) {
                CompositionLocalProvider(
                    LocalContentColor provides colors.trailingIconColor(enabled)
                ) {
                    Box(Modifier.defaultMinSize(minWidth = MenuTokens.ListItemTrailingIconSize)) {
                        trailingIcon()
                    }
                }
            }
        }
    }
}

internal fun calculateTransformOrigin(
    anchorBounds: IntRect,
    menuBounds: IntRect
): TransformOrigin {
    val pivotX = when {
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
    val pivotY = when {
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

// Size defaults.
internal val MenuVerticalMargin = 48.dp
private val DropdownMenuItemHorizontalPadding = 12.dp
internal val DropdownMenuVerticalPadding = 8.dp
private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMaxWidth = 280.dp

// Menu open/close animation.
internal const val InTransitionDuration = 120
internal const val OutTransitionDuration = 75
