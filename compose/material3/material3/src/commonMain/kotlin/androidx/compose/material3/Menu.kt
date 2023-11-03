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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.tokens.MenuTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/**
 * Contains default values used for [DropdownMenuItem].
 */
object MenuDefaults {

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
        textColor: Color = MenuTokens.ListItemLabelTextColor.value,
        leadingIconColor: Color = MenuTokens.ListItemLeadingIconColor.value,
        trailingIconColor: Color = MenuTokens.ListItemTrailingIconColor.value,
        disabledTextColor: Color =
            MenuTokens.ListItemDisabledLabelTextColor.value
                .copy(alpha = MenuTokens.ListItemDisabledLabelTextOpacity),
        disabledLeadingIconColor: Color = MenuTokens.ListItemDisabledLeadingIconColor.value
            .copy(alpha = MenuTokens.ListItemDisabledLeadingIconOpacity),
        disabledTrailingIconColor: Color = MenuTokens.ListItemDisabledTrailingIconColor.value
            .copy(alpha = MenuTokens.ListItemDisabledTrailingIconOpacity),
    ): MenuItemColors = MenuItemColors(
        textColor = textColor,
        leadingIconColor = leadingIconColor,
        trailingIconColor = trailingIconColor,
        disabledTextColor = disabledTextColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
    )

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

@Composable
internal fun DropdownMenuContent(
    expandedState: MutableTransitionState<Boolean>,
    transformOriginState: MutableState<TransformOrigin>,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    // Menu open/close animation.
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
    Row(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = rememberRipple(true)
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
