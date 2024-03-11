/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material Design expressive navigation bar item.
 *
 * Expressive navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * It's recommend for navigation items to always have a text label. An [ExpressiveNavigationBarItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * The [ExpressiveNavigationBarItem] supports two different icon positions, top and start, which is
 * controlled by the [iconPosition] param:
 * - If the icon position is [NavigationItemIconPosition.Top] the icon will be displayed above the
 * label. This configuration is recommended for expressive navigation bars used in small width
 * screens, like a phone in portrait mode.
 * - If the icon position is [NavigationItemIconPosition.Start] the icon will be displayed to the
 * start of the label. This configuration is recommended for expressive navigation bars used in
 * medium width screens, like a phone in landscape mode.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param label text label for this item
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this
 * item in different states. See [ExpressiveNavigationBarItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this item. You can use this to change the item's appearance
 * or preview the item in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 *
 * TODO: Remove internal.
 */
@ExperimentalMaterial3Api
@Composable
internal fun ExpressiveNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    iconPosition: NavigationItemIconPosition = NavigationItemIconPosition.Top,
    colors: NavigationItemColors = ExpressiveNavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val isIconPositionTop = iconPosition == NavigationItemIconPosition.Top
    val indicatorHorizontalPadding = if (isIconPositionTop) {
        TopIconIndicatorHorizontalPadding
    } else {
        StartIconIndicatorHorizontalPadding
    }
    val indicatorVerticalPadding = if (isIconPositionTop) {
        TopIconIndicatorVerticalPadding
    } else {
        StartIconIndicatorVerticalPadding
    }

    NavigationItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        labelTextStyle = MaterialTheme.typography.fromToken(LabelTextFont),
        indicatorShape = ActiveIndicatorShape.value,
        indicatorWidth = VerticalItemActiveIndicatorWidth,
        indicatorHorizontalPadding = indicatorHorizontalPadding,
        indicatorVerticalPadding = indicatorVerticalPadding,
        indicatorToLabelVerticalPadding = TopIconIndicatorToLabelPadding,
        startIconToLabelHorizontalPadding = StartIconToLabelPadding,
        colors = colors,
        modifier = modifier,
        enabled = enabled,
        label = label,
        badge = badge,
        iconPosition = iconPosition,
        interactionSource = interactionSource,
    )
}

/**
 * Defaults used in [ExpressiveNavigationBarItem].
 *
 * TODO: Remove internal.
 */
internal object ExpressiveNavigationBarItemDefaults {
    /**
     * Creates a [NavigationItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable
    fun colors() = MaterialTheme.colorScheme.defaultExpressiveNavigationBarItemColors

    internal val ColorScheme.defaultExpressiveNavigationBarItemColors: NavigationItemColors
        get() {
            return defaultExpressiveNavigationBarItemColorsCached ?: NavigationItemColors(
                selectedIconColor = fromToken(ActiveIconColor),
                selectedTextColor = fromToken(ActiveLabelTextColor),
                selectedIndicatorColor = fromToken(ActiveIndicatorColor),
                unselectedIconColor = fromToken(InactiveIconColor),
                unselectedTextColor = fromToken(InactiveLabelTextColor),
                disabledIconColor = fromToken(InactiveIconColor).copy(alpha = DisabledAlpha),
                disabledTextColor = fromToken(InactiveLabelTextColor).copy(alpha = DisabledAlpha),
            ).also {
                defaultExpressiveNavigationBarItemColorsCached = it
            }
        }
}

/* TODO: Replace below values with tokens. */
private val IconSize = 24.0.dp
private val VerticalItemActiveIndicatorWidth = 56.dp
private val VerticalItemActiveIndicatorHeight = 32.dp
private val HorizontalItemActiveIndicatorHeight = 40.dp
private val LabelTextFont = TypographyKeyTokens.LabelMedium
private val ActiveIndicatorShape = ShapeKeyTokens.CornerFull
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveIconColor = ColorSchemeKeyTokens.Secondary
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveLabelTextColor = ColorSchemeKeyTokens.Secondary
private val ActiveIndicatorColor = ColorSchemeKeyTokens.SecondaryContainer
private val InactiveIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val InactiveLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant

/*@VisibleForTesting*/
internal val TopIconIndicatorVerticalPadding = (VerticalItemActiveIndicatorHeight - IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorHorizontalPadding = (VerticalItemActiveIndicatorWidth - IconSize) / 2
/*@VisibleForTesting*/
internal val StartIconIndicatorVerticalPadding =
    (HorizontalItemActiveIndicatorHeight - IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorToLabelPadding: Dp = 4.dp
/*@VisibleForTesting*/
internal val StartIconIndicatorHorizontalPadding = 16.dp /* TODO: Replace with token. */
/*@VisibleForTesting*/
internal val StartIconToLabelPadding = 4.dp /* TODO: Replace with token. */
