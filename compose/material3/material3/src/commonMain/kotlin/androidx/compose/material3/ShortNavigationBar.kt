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
import androidx.compose.material3.tokens.NavigationBarHorizontalItemTokens
import androidx.compose.material3.tokens.NavigationBarTokens
import androidx.compose.material3.tokens.NavigationBarVerticalItemTokens
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
import kotlin.jvm.JvmInline
import kotlin.math.roundToInt

/**
 * Material Design short navigation bar.
 *
 * Short navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * The recommended configuration of the [ShortNavigationBar] depends on the width size of the screen
 * it's being displayed at:
 * - In small screens, the [ShortNavigationBar] should contain three to five
 *   [ShortNavigationBarItem]s, each representing a singular destination, and its [arrangement]
 *   should be [ShortNavigationBarArrangement.EqualWeight], so that the navigation items are equally
 *   distributed on the bar.
 * - In medium screens, [ShortNavigationBar] should contain three to six [ShortNavigationBarItem]s,
 *   each representing a singular destination, and its [arrangement] should be
 *   [ShortNavigationBarArrangement.Centered], so that the navigation items are distributed grouped
 *   on the center of the bar.
 *
 * A simple example of the first configuration looks like this:
 *
 * @sample androidx.compose.material3.samples.ShortNavigationBarSample
 *
 * And of the second configuration:
 *
 * @sample androidx.compose.material3.samples.ShortNavigationBarWithHorizontalItemsSample
 *
 * See [ShortNavigationBarItem] for configurations specific to each item, and not the overall
 * [ShortNavigationBar] component.
 *
 * @param modifier the [Modifier] to be applied to this navigation bar
 * @param containerColor the color used for the background of this navigation bar. Use
 *   [Color.Transparent] to have no color
 * @param contentColor the color for content inside this navigation bar.
 * @param windowInsets a window insets of the navigation bar
 * @param arrangement the [ShortNavigationBarArrangement] of this navigation bar
 * @param content the content of this navigation bar, typically [ShortNavigationBarItem]s
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ShortNavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = ShortNavigationBarDefaults.containerColor,
    contentColor: Color = ShortNavigationBarDefaults.contentColor,
    windowInsets: WindowInsets = ShortNavigationBarDefaults.windowInsets,
    arrangement: ShortNavigationBarArrangement = ShortNavigationBarDefaults.arrangement,
    content: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
    ) {
        Layout(
            modifier =
                modifier
                    .windowInsetsPadding(windowInsets)
                    .defaultMinSize(minHeight = NavigationBarTokens.ContainerHeight)
                    .selectableGroup(),
            content = content,
            measurePolicy =
                when (arrangement) {
                    ShortNavigationBarArrangement.EqualWeight -> {
                        EqualWeightContentMeasurePolicy()
                    }
                    ShortNavigationBarArrangement.Centered -> {
                        CenteredContentMeasurePolicy()
                    }
                    else -> {
                        throw IllegalArgumentException("Invalid ItemsArrangement value.")
                    }
                }
        )
    }
}

/** Class that describes the different supported item arrangements of the [ShortNavigationBar]. */
@JvmInline
value class ShortNavigationBarArrangement private constructor(private val value: Int) {
    companion object {
        /*
         * The items are equally distributed on the Short Navigation Bar.
         *
         * This configuration is recommended for small width screens.
         */
        val EqualWeight = ShortNavigationBarArrangement(0)

        /*
         * The items are centered on the Short Navigation Bar.
         *
         * This configuration is recommended for medium width screens.
         */
        val Centered = ShortNavigationBarArrangement(1)
    }

    override fun toString() =
        when (this) {
            EqualWeight -> "EqualWeight"
            Centered -> "Centered"
            else -> "Unknown"
        }
}

/**
 * Material Design short navigation bar item.
 *
 * Short navigation bars offer a persistent and convenient way to switch between primary
 * destinations in an app.
 *
 * It's recommend for navigation items to always have a text label. An [ShortNavigationBarItem]
 * always displays labels (if they exist) when selected and unselected.
 *
 * The [ShortNavigationBarItem] supports two different icon positions, top and start, which is
 * controlled by the [iconPosition] param:
 * - If the icon position is [NavigationItemIconPosition.Top] the icon will be displayed above the
 *   label. This configuration is recommended for short navigation bars used in small width screens,
 *   like a phone in portrait mode.
 * - If the icon position is [NavigationItemIconPosition.Start] the icon will be displayed to the
 *   start of the label. This configuration is recommended for short navigation bars used in medium
 *   width screens, like a phone in landscape mode.
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label text label for this item
 * @param modifier the [Modifier] to be applied to this item
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param badge optional badge to show on this item, typically a [Badge]
 * @param iconPosition the [NavigationItemIconPosition] for the icon
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. See [ShortNavigationBarItemDefaults.colors]
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun ShortNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: (@Composable () -> Unit)? = null,
    iconPosition: NavigationItemIconPosition = NavigationItemIconPosition.Top,
    colors: NavigationItemColors = ShortNavigationBarItemDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    val isIconPositionTop = iconPosition == NavigationItemIconPosition.Top
    val indicatorHorizontalPadding =
        if (isIconPositionTop) {
            TopIconIndicatorHorizontalPadding
        } else {
            StartIconIndicatorHorizontalPadding
        }
    val indicatorVerticalPadding =
        if (isIconPositionTop) {
            TopIconIndicatorVerticalPadding
        } else {
            StartIconIndicatorVerticalPadding
        }

    NavigationItem(
        selected = selected,
        onClick = onClick,
        icon = icon,
        labelTextStyle = NavigationBarTokens.LabelTextFont.value,
        indicatorShape = NavigationBarTokens.ItemActiveIndicatorShape.value,
        indicatorWidth = NavigationBarVerticalItemTokens.ActiveIndicatorWidth,
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

/** Defaults used in [ShortNavigationBar]. */
@ExperimentalMaterial3ExpressiveApi
object ShortNavigationBarDefaults {
    /** Default container color for a short navigation bar. */
    val containerColor: Color
        @Composable get() = NavigationBarTokens.ContainerColor.value

    /** Default content color for a short navigation bar. */
    val contentColor: Color
        @Composable get() = contentColorFor(containerColor)

    /** Default arrangement for a short navigation bar. */
    val arrangement: ShortNavigationBarArrangement
        get() = ShortNavigationBarArrangement.EqualWeight

    /** Default window insets to be used and consumed by the short navigation bar. */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
}

/** Defaults used in [ShortNavigationBarItem]. */
@ExperimentalMaterial3ExpressiveApi
object ShortNavigationBarItemDefaults {
    /**
     * Creates a [NavigationItemColors] with the provided colors according to the Material
     * specification.
     */
    @Composable fun colors() = MaterialTheme.colorScheme.defaultShortNavigationBarItemColors

    internal val ColorScheme.defaultShortNavigationBarItemColors: NavigationItemColors
        get() {
            return defaultShortNavigationBarItemColorsCached
                ?: NavigationItemColors(
                        selectedIconColor = fromToken(NavigationBarTokens.ItemActiveIconColor),
                        selectedTextColor = fromToken(NavigationBarTokens.ItemActiveLabelTextColor),
                        selectedIndicatorColor =
                            fromToken(NavigationBarTokens.ItemActiveIndicatorColor),
                        unselectedIconColor = fromToken(NavigationBarTokens.ItemInactiveIconColor),
                        unselectedTextColor =
                            fromToken(NavigationBarTokens.ItemInactiveLabelTextColor),
                        disabledIconColor =
                            fromToken(NavigationBarTokens.ItemInactiveIconColor)
                                .copy(alpha = DisabledAlpha),
                        disabledTextColor =
                            fromToken(NavigationBarTokens.ItemInactiveLabelTextColor)
                                .copy(alpha = DisabledAlpha),
                    )
                    .also { defaultShortNavigationBarItemColorsCached = it }
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
            return layout(width, itemHeight) {}
        }

        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables =
                measurables.fastMap {
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
            itemsPlaceables =
                measurables.fastMap {
                    it.measure(
                        constraints.constrain(
                            Constraints.fixed(width = itemWidth, height = itemHeight)
                        )
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
            return layout(width, itemHeight) {}
        }

        var barHorizontalPadding = 0
        val itemsPlaceables: List<Placeable>
        if (!constraints.hasBoundedWidth) {
            // If width constraint is not bounded, let item containers widths be as big as they are.
            // This may lead to a different items arrangement than the expected.
            itemsPlaceables =
                measurables.fastMap {
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
            itemsPlaceables =
                measurables.fastMap {
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

/*@VisibleForTesting*/
internal val TopIconItemVerticalPadding = NavigationBarVerticalItemTokens.ContainerBetweenSpace
/*@VisibleForTesting*/
internal val TopIconIndicatorVerticalPadding =
    (NavigationBarVerticalItemTokens.ActiveIndicatorHeight -
        NavigationBarVerticalItemTokens.IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorHorizontalPadding =
    (NavigationBarVerticalItemTokens.ActiveIndicatorWidth -
        NavigationBarVerticalItemTokens.IconSize) / 2
/*@VisibleForTesting*/
internal val StartIconIndicatorVerticalPadding =
    (NavigationBarHorizontalItemTokens.ActiveIndicatorHeight -
        NavigationBarHorizontalItemTokens.IconSize) / 2
/*@VisibleForTesting*/
internal val TopIconIndicatorToLabelPadding: Dp = 4.dp
/*@VisibleForTesting*/
internal val StartIconIndicatorHorizontalPadding =
    NavigationBarHorizontalItemTokens.ActiveIndicatorLeadingSpace
/*@VisibleForTesting*/
internal val StartIconToLabelPadding = NavigationBarTokens.ItemActiveIndicatorIconLabelSpace
