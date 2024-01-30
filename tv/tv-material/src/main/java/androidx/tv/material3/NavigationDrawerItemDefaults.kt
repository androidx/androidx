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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.tv.material3.tokens.Elevation

/**
 * Contains the default values used by selectable [NavigationDrawerItem]
 */
@ExperimentalTvMaterial3Api // TODO (b/263353219): Remove this before launching beta
object NavigationDrawerItemDefaults {
    /**
     * The default Icon size used by [NavigationDrawerItem]
     */
    val IconSize = 24.dp

    /**
     * The size of the [NavigationDrawerItem] when the drawer is collapsed
     */
    val CollapsedDrawerItemWidth = 56.dp

    /**
     * The size of the [NavigationDrawerItem] when the drawer is expanded
     */
    val ExpandedDrawerItemWidth = 256.dp

    /**
     * The default content padding [PaddingValues] used by [NavigationDrawerItem] when the drawer
     * is expanded
     */

    val ContainerHeightOneLine = 56.dp
    val ContainerHeightTwoLine = 64.dp

    /**
     * The default elevation used by [NavigationDrawerItem]
     */
    val NavigationDrawerItemElevation = Elevation.Level0

    /**
     * Animation enter default for inner content
     */
    val ContentAnimationEnter = fadeIn() + slideIn { IntOffset(-it.width, 0) }

    /**
     * Animation exit default for inner content
     */
    val ContentAnimationExit = fadeOut() + slideOut { IntOffset(0, 0) }

    /**
     * Default border used by [NavigationDrawerItem]
     */
    val DefaultBorder
        @ReadOnlyComposable
        @Composable get() = Border(
            border = BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.border
            ),
        )

    /**
     * The default container color used by [NavigationDrawerItem]'s trailing badge
     */
    val TrailingBadgeContainerColor
        @ReadOnlyComposable
        @Composable get() = MaterialTheme.colorScheme.tertiary

    /**
     * The default text style used by [NavigationDrawerItem]'s trailing badge
     */
    val TrailingBadgeTextStyle
        @ReadOnlyComposable
        @Composable get() = MaterialTheme.typography.labelSmall

    /**
     * The default content color used by [NavigationDrawerItem]'s trailing badge
     */
    val TrailingBadgeContentColor
        @ReadOnlyComposable
        @Composable get() = MaterialTheme.colorScheme.onTertiary

    /**
     * Creates a trailing badge for [NavigationDrawerItem]
     */
    @Composable
    fun TrailingBadge(
        text: String,
        containerColor: Color = TrailingBadgeContainerColor,
        contentColor: Color = TrailingBadgeContentColor
    ) {
        Box(
            modifier = Modifier
                .background(containerColor, RoundedCornerShape(50))
                .padding(10.dp, 2.dp)
        ) {
            ProvideTextStyle(value = TrailingBadgeTextStyle) {
                Text(
                    text = text,
                    color = contentColor,
                )
            }
        }
    }

    /**
     * Creates a [NavigationDrawerItemShape] that represents the default container shapes
     * used in a selectable [NavigationDrawerItem]
     *
     * @param shape the default shape used when the [NavigationDrawerItem] is enabled
     * @param focusedShape the shape used when the [NavigationDrawerItem] is enabled and focused
     * @param pressedShape the shape used when the [NavigationDrawerItem] is enabled and pressed
     * @param selectedShape the shape used when the [NavigationDrawerItem] is enabled and selected
     * @param disabledShape the shape used when the [NavigationDrawerItem] is not enabled
     * @param focusedSelectedShape the shape used when the [NavigationDrawerItem] is enabled,
     * focused and selected
     * @param focusedDisabledShape the shape used when the [NavigationDrawerItem] is not enabled
     * and focused
     * @param pressedSelectedShape the shape used when the [NavigationDrawerItem] is enabled,
     * pressed and selected
     */
    fun shape(
        shape: Shape = RoundedCornerShape(50),
        focusedShape: Shape = shape,
        pressedShape: Shape = shape,
        selectedShape: Shape = shape,
        disabledShape: Shape = shape,
        focusedSelectedShape: Shape = shape,
        focusedDisabledShape: Shape = disabledShape,
        pressedSelectedShape: Shape = shape
    ) = NavigationDrawerItemShape(
        shape = shape,
        focusedShape = focusedShape,
        pressedShape = pressedShape,
        selectedShape = selectedShape,
        disabledShape = disabledShape,
        focusedSelectedShape = focusedSelectedShape,
        focusedDisabledShape = focusedDisabledShape,
        pressedSelectedShape = pressedSelectedShape
    )

    /**
     * Creates a [NavigationDrawerItemColors] that represents the default container &
     * content colors used in a selectable [NavigationDrawerItem]
     *
     * @param containerColor the default container color used when the [NavigationDrawerItem] is
     * enabled
     * @param contentColor the default content color used when the [NavigationDrawerItem] is enabled
     * @param inactiveContentColor the content color used when none of the navigation items have
     * focus
     * @param focusedContainerColor the container color used when the [NavigationDrawerItem] is
     * enabled and focused
     * @param focusedContentColor the content color used when the [NavigationDrawerItem] is enabled
     * and focused
     * @param pressedContainerColor the container color used when the [NavigationDrawerItem] is
     * enabled and pressed
     * @param pressedContentColor the content color used when the [NavigationDrawerItem] is enabled
     * and pressed
     * @param selectedContainerColor the container color used when the [NavigationDrawerItem] is
     * enabled and selected
     * @param selectedContentColor the content color used when the [NavigationDrawerItem] is
     * enabled and selected
     * @param disabledContainerColor the container color used when the [NavigationDrawerItem] is
     * not enabled
     * @param disabledContentColor the content color used when the [NavigationDrawerItem] is not
     * enabled
     * @param disabledInactiveContentColor the content color used when none of the navigation items
     * have focus and this item is disabled
     * @param focusedSelectedContainerColor the container color used when the
     * NavigationDrawerItem is enabled, focused and selected
     * @param focusedSelectedContentColor the content color used when the [NavigationDrawerItem]
     * is enabled, focused and selected
     * @param pressedSelectedContainerColor the container color used when the
     * [NavigationDrawerItem] is enabled, pressed and selected
     * @param pressedSelectedContentColor the content color used when the [NavigationDrawerItem] is
     * enabled, pressed and selected
     */
    @ReadOnlyComposable
    @Composable
    fun colors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        inactiveContentColor: Color = contentColor.copy(alpha = 0.4f),
        focusedContainerColor: Color = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor: Color = contentColorFor(focusedContainerColor),
        pressedContainerColor: Color = focusedContainerColor,
        pressedContentColor: Color = contentColorFor(focusedContainerColor),
        selectedContainerColor: Color =
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
        selectedContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledInactiveContentColor: Color = disabledContentColor.copy(alpha = 0.4f),
        focusedSelectedContainerColor: Color = focusedContainerColor,
        focusedSelectedContentColor: Color = focusedContentColor,
        pressedSelectedContainerColor: Color = pressedContainerColor,
        pressedSelectedContentColor: Color = pressedContentColor
    ) = NavigationDrawerItemColors(
        containerColor = containerColor,
        contentColor = contentColor,
        inactiveContentColor = inactiveContentColor,
        focusedContainerColor = focusedContainerColor,
        focusedContentColor = focusedContentColor,
        pressedContainerColor = pressedContainerColor,
        pressedContentColor = pressedContentColor,
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
        disabledInactiveContentColor = disabledInactiveContentColor,
        focusedSelectedContainerColor = focusedSelectedContainerColor,
        focusedSelectedContentColor = focusedSelectedContentColor,
        pressedSelectedContainerColor = pressedSelectedContainerColor,
        pressedSelectedContentColor = pressedSelectedContentColor
    )

    /**
     * Creates a [NavigationDrawerItemScale] that represents the default scales used in a
     * selectable [NavigationDrawerItem]
     *
     * scales are used to modify the size of a composable in different [Interaction] states
     * e.g. `1f` (original) in default state, `1.2f` (scaled up) in focused state, `0.8f` (scaled
     * down) in pressed state, etc.
     *
     * @param scale the scale used when the [NavigationDrawerItem] is enabled
     * @param focusedScale the scale used when the [NavigationDrawerItem] is enabled and focused
     * @param pressedScale the scale used when the [NavigationDrawerItem] is enabled and pressed
     * @param selectedScale the scale used when the [NavigationDrawerItem] is enabled and selected
     * @param disabledScale the scale used when the [NavigationDrawerItem] is not enabled
     * @param focusedSelectedScale the scale used when the [NavigationDrawerItem] is enabled,
     * focused and selected
     * @param focusedDisabledScale the scale used when the [NavigationDrawerItem] is not enabled and
     * focused
     * @param pressedSelectedScale the scale used when the [NavigationDrawerItem] is enabled,
     * pressed and selected
     */
    fun scale(
        @FloatRange(from = 0.0) scale: Float = 1f,
        @FloatRange(from = 0.0) focusedScale: Float = 1.05f,
        @FloatRange(from = 0.0) pressedScale: Float = scale,
        @FloatRange(from = 0.0) selectedScale: Float = scale,
        @FloatRange(from = 0.0) disabledScale: Float = scale,
        @FloatRange(from = 0.0) focusedSelectedScale: Float = focusedScale,
        @FloatRange(from = 0.0) focusedDisabledScale: Float = disabledScale,
        @FloatRange(from = 0.0) pressedSelectedScale: Float = scale
    ) = NavigationDrawerItemScale(
        scale = scale,
        focusedScale = focusedScale,
        pressedScale = pressedScale,
        selectedScale = selectedScale,
        disabledScale = disabledScale,
        focusedSelectedScale = focusedSelectedScale,
        focusedDisabledScale = focusedDisabledScale,
        pressedSelectedScale = pressedSelectedScale
    )

    /**
     * Creates a [NavigationDrawerItemBorder] that represents the default [Border]s
     * applied on a selectable [NavigationDrawerItem] in different [Interaction] states
     *
     * @param border the default [Border] used when the [NavigationDrawerItem] is enabled
     * @param focusedBorder the [Border] used when the [NavigationDrawerItem] is enabled and focused
     * @param pressedBorder the [Border] used when the [NavigationDrawerItem] is enabled and pressed
     * @param selectedBorder the [Border] used when the [NavigationDrawerItem] is enabled and
     * selected
     * @param disabledBorder the [Border] used when the [NavigationDrawerItem] is not enabled
     * @param focusedSelectedBorder the [Border] used when the [NavigationDrawerItem] is enabled,
     * focused and selected
     * @param focusedDisabledBorder the [Border] used when the [NavigationDrawerItem] is not
     * enabled and focused
     * @param pressedSelectedBorder the [Border] used when the [NavigationDrawerItem] is enabled,
     * pressed and selected
     */
    @ReadOnlyComposable
    @Composable
    fun border(
        border: Border = Border.None,
        focusedBorder: Border = border,
        pressedBorder: Border = focusedBorder,
        selectedBorder: Border = border,
        disabledBorder: Border = border,
        focusedSelectedBorder: Border = focusedBorder,
        focusedDisabledBorder: Border = DefaultBorder,
        pressedSelectedBorder: Border = border
    ) = NavigationDrawerItemBorder(
        border = border,
        focusedBorder = focusedBorder,
        pressedBorder = pressedBorder,
        selectedBorder = selectedBorder,
        disabledBorder = disabledBorder,
        focusedSelectedBorder = focusedSelectedBorder,
        focusedDisabledBorder = focusedDisabledBorder,
        pressedSelectedBorder = pressedSelectedBorder
    )

    /**
     * Creates a [NavigationDrawerItemGlow] that represents the default [Glow]s used in a
     * selectable [NavigationDrawerItem]
     *
     * @param glow the [Glow] used when the [NavigationDrawerItem] is enabled, and has no other
     * [Interaction]s
     * @param focusedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and focused
     * @param pressedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and pressed
     * @param selectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled and selected
     * @param focusedSelectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled,
     * focused and selected
     * @param pressedSelectedGlow the [Glow] used when the [NavigationDrawerItem] is enabled,
     * pressed and selected
     */
    fun glow(
        glow: Glow = Glow.None,
        focusedGlow: Glow = glow,
        pressedGlow: Glow = glow,
        selectedGlow: Glow = glow,
        focusedSelectedGlow: Glow = focusedGlow,
        pressedSelectedGlow: Glow = glow
    ) = NavigationDrawerItemGlow(
        glow = glow,
        focusedGlow = focusedGlow,
        pressedGlow = pressedGlow,
        selectedGlow = selectedGlow,
        focusedSelectedGlow = focusedSelectedGlow,
        pressedSelectedGlow = pressedSelectedGlow
    )
}
