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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.ShapeKeyTokens
import androidx.compose.material3.tokens.TypographyKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrain
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import kotlin.math.roundToInt

/**
 * Material Design expressive navigation bar.
 *
 * Expressive navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * The recommended configuration of the [ExpressiveNavigationBar] depends on the width size of the
 * screen it's being displayed at:
 * - In small screens, the [ExpressiveNavigationBar] should contain three to five
 * [ExpressiveNavigationBarItem]s, each representing a singular destination, and its [arrangement]
 * should be [NavigationBarArrangement.EqualWeight], so that the navigation items are equally
 * distributed on the bar.
 * - In medium screens, [ExpressiveNavigationBar] should contain three to six
 * [ExpressiveNavigationBarItem]s, each representing a singular destination, and its [arrangement]
 * should be [NavigationBarArrangement.Centered], so that the navigation items are distributed
 * grouped on the center of the bar.
 *
 * See [ExpressiveNavigationBarItem] for configuration specific to each item, and not the overall
 * [ExpressiveNavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this expressive navigation bar
 * @param containerColor the color used for the background of this expressive navigation bar. Use
 * [Color.Transparent] to have no color
 * @param contentColor the color for content inside this expressive navigation bar.
 * @param windowInsets a window insets of the expressive navigation bar
 * @param arrangement the [NavigationBarArrangement] of this expressive navigation bar
 * @param content the content of this expressive navigation bar, typically
 * [ExpressiveNavigationBarItem]s
 *
 * TODO: Remove "internal".
 */
@ExperimentalMaterial3Api
@Composable
internal fun ExpressiveNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ExpressiveNavigationBarDefaults.containerColor,
    contentColor: Color = ExpressiveNavigationBarDefaults.contentColor,
    windowInsets: WindowInsets = ExpressiveNavigationBarDefaults.windowInsets,
    arrangement: NavigationBarArrangement = ExpressiveNavigationBarDefaults.arrangement,
    content: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
    ) {
        Layout(
            modifier = modifier
                .windowInsetsPadding(windowInsets)
                .defaultMinSize(minHeight = NavigationBarHeight)
                .selectableGroup(),
            content = content,
            measurePolicy = when (arrangement) {
                NavigationBarArrangement.EqualWeight -> { EqualWeightContentMeasurePolicy() }
                NavigationBarArrangement.Centered -> { CenteredContentMeasurePolicy() }
                else -> { throw IllegalArgumentException("Invalid ItemsArrangement value.") }
            }
        )
    }
}

/**
 * Class that describes the different supported item arrangements of the [ExpressiveNavigationBar].
 *
 * TODO: Remove "internal".
 */
@JvmInline
internal value class NavigationBarArrangement private constructor(private val value: Int) {
    companion object {
        /*
         * The items are equally distributed on the Expressive Navigation Bar.
         *
         * This configuration is recommended for small width screens.
        */
        val EqualWeight = NavigationBarArrangement(0)

        /*
         * The items are centered on the Expressive Navigation Bar.
         *
         * This configuration is recommended for medium width screens.
         */
        val Centered = NavigationBarArrangement(1)
    }

    override fun toString() = when (this) {
        EqualWeight -> "EqualWeight"
        Centered -> "Centered"
        else -> "Unknown"
    }
}

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
 * TODO: Remove "internal".
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
        indicatorWidth = TopIconItemActiveIndicatorWidth,
        indicatorHorizontalPadding = indicatorHorizontalPadding,
        indicatorVerticalPadding = indicatorVerticalPadding,
        indicatorToLabelVerticalPadding = TopIconIndicatorToLabelPadding,
        startIconToLabelHorizontalPadding = StartIconToLabelPadding,
        topIconItemVerticalPadding = TopIconItemVerticalPadding,
        colors = colors,
        modifier = modifier,
        enabled = enabled,
        label = label,
        badge = badge,
        iconPosition = iconPosition,
        interactionSource = interactionSource,
    )
}

// TODO: Remove "internal".
/** Defaults used in [ExpressiveNavigationBar]. */
@ExperimentalMaterial3Api
internal object ExpressiveNavigationBarDefaults {
    /** Default container color for an expressive navigation bar. */
    // TODO: Replace with token.
    val containerColor: Color @Composable get() = ColorSchemeKeyTokens.SurfaceContainer.value

    /** Default content color for an expressive navigation bar. */
    // TODO: Replace with token.
    val contentColor: Color @Composable get() = ColorSchemeKeyTokens.OnSurfaceVariant.value

    /** Default arrangement for an expressive navigation bar. */
    val arrangement: NavigationBarArrangement get() = NavigationBarArrangement.EqualWeight

    /** Default window insets to be used and consumed by the expressive navigation bar. */
    val windowInsets: WindowInsets
        @Composable
        get() = WindowInsets.systemBarsForVisualComponents
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
}

// TODO: Remove "internal".
/** Defaults used in [ExpressiveNavigationBarItem]. */
@ExperimentalMaterial3Api
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

private class EqualWeightContentMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val width = constraints.maxWidth
        var itemHeight = constraints.minHeight
        val itemsCount = measurables.size
        // If there are no items, bar will be empty.
        if (itemsCount < 1) {
            return layout(width, itemHeight) { }
        }

        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables = measurables.fastMap {
                it.measure(constraints.constrain(Constraints.fixedHeight(height = itemHeight)))
            }
        } else {
            val itemWidth = width / itemsCount
            measurables.fastForEach {
                val measurableHeight = it.maxIntrinsicHeight(itemWidth)
                if (itemHeight < measurableHeight) {
                    itemHeight = measurableHeight.coerceAtMost(constraints.maxHeight)
                }
            }

            // Make sure the item containers have the same width and height.
            itemsPlaceables = measurables.fastMap {
                it.measure(
                    constraints.constrain(Constraints.fixed(width = itemWidth, height = itemHeight))
                )
            }
        }

        return layout(width, itemHeight) {
            var x = 0
            val y = 0
            itemsPlaceables.fastForEach { item ->
                item.placeRelative(x, y)
                x += item.width
            }
        }
    }
}

private class CenteredContentMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val width = constraints.maxWidth
        var itemHeight = constraints.minHeight
        val itemsCount = measurables.size
        // If there are no items, bar will be empty.
        if (itemsCount < 1) {
            return layout(width, itemHeight) { }
        }

        var barHorizontalPadding = 0
        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables = measurables.fastMap {
                it.measure(constraints.constrain(Constraints.fixedHeight(height = itemHeight)))
            }
        } else {
            val itemMaxWidth = width / itemsCount
            barHorizontalPadding = calculateCenteredContentHorizontalPadding(itemsCount, width)
            val itemMinWidth = (width - (barHorizontalPadding * 2)) / itemsCount

            // Make sure the item containers will have the same height.
            measurables.fastForEach {
                val measurableHeight = it.maxIntrinsicHeight(itemMinWidth)
                if (itemHeight < measurableHeight) {
                    itemHeight = measurableHeight.coerceAtMost(constraints.maxHeight)
                }
            }
            itemsPlaceables = measurables.fastMap {
                var currentItemWidth = itemMinWidth
                val measurableWidth = it.maxIntrinsicWidth(constraints.minHeight)
                if (currentItemWidth < measurableWidth) {
                    // Let an item container be bigger in width if needed, but limit it to
                    // itemMaxWidth.
                    currentItemWidth = measurableWidth.coerceAtMost(itemMaxWidth)
                    // Update horizontal padding so that items remain centered.
                    barHorizontalPadding -= (currentItemWidth - itemMinWidth) / 2
                }

                it.measure(
                    constraints.constrain(
                        Constraints.fixed(width = currentItemWidth, height = itemHeight)
                    )
                )
            }
        }

        return layout(width, itemHeight) {
            var x = barHorizontalPadding
            val y = 0
            itemsPlaceables.fastForEach { item ->
                item.placeRelative(x, y)
                x += item.width
            }
        }
    }
}

/*
 * For the navigation bar with start icon items, the horizontal padding of the bar depends on the
 * number of items:
 * - 3 items: the items should occupy 60% of the bar's width, so the horizontal padding should be
 *   20% of it on each side.
 * - 4 items: the items should occupy 70% of the bar's width, so the horizontal padding should be
 *   15% of it on each side.
 * - 5 items: the items should occupy 80% of the bar's width, so the horizontal padding should be
 *   10% of it on each side.
 * - 6 items: the items should occupy 90% of the bar's width, so the horizontal padding should be
 *   5% of it on each side.
 */
private fun calculateCenteredContentHorizontalPadding(itemsCount: Int, barWidth: Int): Int {
    if (itemsCount > 6) return 0
    // Formula to calculate the padding percentage based on the number of items and bar width.
    val paddingPercentage = ((100 - 10 * (itemsCount + 3)) / 2f) / 100
    return (paddingPercentage * barWidth).roundToInt()
}

/* TODO: Replace below values with tokens. */
private val IconSize = 24.0.dp
private val TopIconItemActiveIndicatorWidth = 56.dp
private val TopIconItemActiveIndicatorHeight = 32.dp
private val StartIconItemActiveIndicatorHeight = 40.dp
private val LabelTextFont = TypographyKeyTokens.LabelMedium
private val ActiveIndicatorShape = ShapeKeyTokens.CornerFull
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveIconColor = ColorSchemeKeyTokens.Secondary
// TODO: Update to OnSecondaryContainer once value matches Secondary.
private val ActiveLabelTextColor = ColorSchemeKeyTokens.Secondary
private val ActiveIndicatorColor = ColorSchemeKeyTokens.SecondaryContainer
private val InactiveIconColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val InactiveLabelTextColor = ColorSchemeKeyTokens.OnSurfaceVariant
private val NavigationBarHeight = 64.dp

/*@VisibleForTesting*/
internal val TopIconItemVerticalPadding = 6.dp
/*@VisibleForTesting*/
internal val TopIconIndicatorVerticalPadding = (TopIconItemActiveIndicatorHeight - IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorHorizontalPadding = (TopIconItemActiveIndicatorWidth - IconSize) / 2
/*@VisibleForTesting*/
internal val StartIconIndicatorVerticalPadding = (StartIconItemActiveIndicatorHeight - IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorToLabelPadding: Dp = 4.dp
/*@VisibleForTesting*/
internal val StartIconIndicatorHorizontalPadding = 16.dp /* TODO: Replace with token. */
/*@VisibleForTesting*/
internal val StartIconToLabelPadding = 4.dp /* TODO: Replace with token. */
