/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.remember
import androidx.compose.animation.ColorPropKey
import androidx.compose.animation.animate
import androidx.compose.animation.transition
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.Placeable
import androidx.compose.ui.layout.id
import androidx.compose.ui.layout.layoutId
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentColorAmbient
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.ProvideTextStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.background
import androidx.compose.foundation.contentColor
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.foundation.text.LastBaseline
import androidx.compose.material.TabConstants.defaultTabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.layout.ExperimentalSubcomposeLayoutApi
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlin.math.max

/**
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize
 * the indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it
 * should internally take care of sizing and positioning the indicator to match changes to
 * [selectedIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material.samples.FancyIndicator
 *
 * We can reuse [TabConstants.defaultTabIndicatorOffset] and just provide this indicator,
 * as we aren't changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the
 * appearance of the indicator as it animates between tabs, such as changing its color or size.
 * [indicator] is stacked on top of the entire TabRow, so you just need to provide a custom
 * transition that animates the offset of the indicator from the start of the TabRow. For
 * example, take the following example that uses a transition to animate the offset, width, and
 * color of the same FancyIndicator from before, also adding a physics based 'spring' effect to
 * the indicator in the direction of motion:
 *
 * @sample androidx.compose.material.samples.FancyAnimatedIndicator
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material.samples.FancyIndicatorContainerTabs
 *
 * @param T the type of the item provided that will map to a [Tab]
 * @param items the list containing the items used to build this TabRow
 * @param selectedIndex the index of the currently selected tab
 * @param modifier optional [Modifier] for this TabRow
 * @param backgroundColor The background color for the TabRow. Use [Color.Transparent] to have
 * no color.
 * @param contentColor The preferred content color provided by this TabRow to its children.
 * Defaults to either the matching `onFoo` color for [backgroundColor], or if [backgroundColor] is
 * not a color from the theme, this will keep the same value set above this TabRow.
 * @param scrollable if the tabs should be scrollable. If `false` the tabs will take up an equal
 * amount of the space given to TabRow. If `true` the tabs will take up only as much space as they
 * need, with any excess tabs positioned off screen and able to be scrolled to.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 * will be a [TabConstants.DefaultIndicator], using a [TabConstants.defaultTabIndicatorOffset]
 * modifier to animate its position. Note that this indicator will be forced to fill up the
 * entire TabRow, so you should use [TabConstants.defaultTabIndicatorOffset] or similar to
 * animate the actual drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the TabRow. This provides a layer of
 * separation between the TabRow and the content displayed underneath.
 * @param tab the [Tab] to be emitted for the given index and element of type [T] in [items]
 */
@Composable
fun <T> TabRow(
    items: List<T>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    scrollable: Boolean = false,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = { tabPositions ->
        TabConstants.DefaultIndicator(
            Modifier.defaultTabIndicatorOffset(tabPositions[selectedIndex])
        )
    },
    divider: @Composable () -> Unit = {
        TabConstants.DefaultDivider()
    },
    tab: @Composable (Int, T) -> Unit
) {
    Surface(modifier = modifier, color = backgroundColor, contentColor = contentColor) {
        // TODO: force scrollable for tabs that will be too small if they take up equal space?
        if (scrollable) {
            val tabs = @Composable {
                items.fastForEachIndexed { index, item ->
                    tab(index, item)
                }
            }
            ScrollableTabRow(selectedIndex, tabs, indicator, divider)
        } else {
            // TODO: b/150138067 remove modifier here and use the global LayoutWeight
            // modifier when it exists
            val tabs = @Composable { modifier: Modifier ->
                items.fastForEachIndexed { index, item ->
                    Box(modifier, gravity = ContentGravity.Center) {
                        tab(index, item)
                    }
                }
            }
            FixedTabRow(items.size, tabs, indicator, divider)
        }
    }
}

@OptIn(ExperimentalSubcomposeLayoutApi::class)
@Composable
private fun FixedTabRow(
    tabCount: Int,
    tabs: @Composable (Modifier) -> Unit,
    indicatorContainer: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit
) {
    SubcomposeLayout<TabSlots>(Modifier.fillMaxWidth()) { constraints ->
        val tabsPlaceable = subcompose(TabSlots.Tabs) {
            Row {
                tabs(Modifier.weight(1f))
            }
        }.first().measure(constraints)

        val tabWidth = (tabsPlaceable.width / tabCount).toDp()
        val tabPositions = List(tabCount) { index ->
            TabPosition(tabWidth * index, tabWidth)
        }

        layout(tabsPlaceable.width, tabsPlaceable.height) {
            tabsPlaceable.place(0, 0)

            subcompose(TabSlots.Divider, divider).fastForEach {
                val placeable = it.measure(constraints)
                placeable.place(0, tabsPlaceable.height - placeable.height)
            }

            subcompose(TabSlots.Indicator) {
                indicatorContainer(tabPositions)
            }.fastForEach {
                it.measure(Constraints.fixed(tabsPlaceable.width, tabsPlaceable.height))
                    .place(0, 0)
            }
        }
    }
}

@OptIn(ExperimentalSubcomposeLayoutApi::class)
@Composable
private fun ScrollableTabRow(
    selectedIndex: Int,
    tabs: @Composable () -> Unit,
    indicatorContainer: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    val scrollableTabData = remember(scrollState) {
        ScrollableTabData(
            scrollState = scrollState,
            selectedTab = selectedIndex
        )
    }
    ScrollableRow(scrollState = scrollState, modifier = Modifier.fillMaxWidth()) {
        SubcomposeLayout<TabSlots> { constraints ->
            val minTabWidth = ScrollableTabRowMinimumTabWidth.toIntPx()
            val edgeOffset = ScrollableTabRowEdgeOffset.toIntPx()
            val tabConstraints = constraints.copy(minWidth = minTabWidth)

            val tabPlaceables = subcompose(TabSlots.Tabs, tabs)
                .fastMap { it.measure(tabConstraints) }

            var layoutWidth = edgeOffset * 2
            var layoutHeight = 0
            tabPlaceables.fastForEach {
                layoutWidth += it.width
                layoutHeight = maxOf(layoutHeight, it.height)
            }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                val tabPositions = mutableListOf<TabPosition>()
                var left = edgeOffset
                tabPlaceables.fastForEach {
                    it.place(left, 0)
                    tabPositions.add(TabPosition(left = left.toDp(), width = it.width.toDp()))
                    left += it.width
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(
                        constraints.copy(minWidth = layoutWidth, maxWidth = layoutWidth)
                    )
                    placeable.place(0, layoutHeight - placeable.height)
                }

                // The indicator container is measured to fill the entire space occupied by the tab
                // row, and then placed on top of the divider.
                subcompose(TabSlots.Indicator) {
                    indicatorContainer(tabPositions)
                }.fastForEach {
                    it.measure(Constraints.fixed(layoutWidth, layoutHeight))
                        .place(0, 0)
                }

                scrollableTabData.onLaidOut(
                    density = this@SubcomposeLayout,
                    edgeOffset = edgeOffset,
                    tabPositions = tabPositions,
                    selectedTab = selectedIndex
                )
            }
        }
    }
}

private enum class TabSlots {
    Tabs,
    Divider,
    Indicator
}

/**
 * Class holding onto state needed for [ScrollableTabRow]
 */
private class ScrollableTabData(
    private val scrollState: ScrollState,
    private var selectedTab: Int
) {
    fun onLaidOut(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>,
        selectedTab: Int
    ) {
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab
            tabPositions.getOrNull(selectedTab)?.let {
                // Scrolls to the tab with [tabPosition], trying to place it in the center of the
                // screen or as close to the center as possible.
                val calculatedOffset = it.calculateTabOffset(density, edgeOffset, tabPositions)
                scrollState.smoothScrollTo(calculatedOffset)
            }
        }
    }

    /**
     * @return the offset required to horizontally center the tab inside this TabRow.
     * If the tab is at the start / end, and there is not enough space to fully centre the tab, this
     * will just clamp to the min / max position given the max width.
     */
    private fun TabPosition.calculateTabOffset(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>
    ): Float = with(density) {
        val totalTabRowWidth = tabPositions.last().right.toIntPx() + edgeOffset
        val visibleWidth = totalTabRowWidth - scrollState.maxValue.toInt()
        val tabOffset = left.toIntPx()
        val scrollerCenter = visibleWidth / 2
        val tabWidth = width.toIntPx()
        val centeredTabOffset = tabOffset - (scrollerCenter - tabWidth / 2)
        // How much space we have to scroll. If the visible width is <= to the total width, then
        // we have no space to scroll as everything is always visible.
        val availableSpace = (totalTabRowWidth - visibleWidth).coerceAtLeast(0)
        return centeredTabOffset.coerceIn(0, availableSpace).toFloat()
    }
}

/**
 * Contains default values used by tabs from the Material specification.
 */
object TabConstants {
    /**
     * Default [Divider], which will be positioned at the bottom of the [TabRow], underneath the
     * indicator.
     *
     * @param modifier modifier for the divider's layout
     * @param thickness thickness of the divider
     * @param color color of the divider
     */
    @Composable
    fun DefaultDivider(
        modifier: Modifier = Modifier,
        thickness: Dp = DefaultDividerThickness,
        color: Color = contentColor().copy(alpha = DefaultDividerOpacity)
    ) {
        Divider(modifier = modifier, thickness = thickness, color = color)
    }

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    fun DefaultIndicator(
        modifier: Modifier = Modifier,
        height: Dp = DefaultIndicatorHeight,
        color: Color = contentColor()
    ) {
        Box(modifier
            .fillMaxWidth()
            .preferredHeight(height)
            .background(color = color)
        )
    }

    /**
     * [Modifier] that takes up all the available width inside the [TabRow], and then animates
     * the offset of the indicator it is applied to, depending on the [currentTabPosition].
     *
     * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
     * calculate the offset of the indicator this modifier is applied to, as well as its width.
     */
    fun Modifier.defaultTabIndicatorOffset(
        currentTabPosition: TabPosition
    ): Modifier = composed {
        // TODO: should we animate the width of the indicator as it moves between tabs of different
        // sizes inside a scrollable tab row?
        val currentTabWidth = currentTabPosition.width
        val indicatorOffset = animate(
            target = currentTabPosition.left,
            animSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .preferredWidth(currentTabWidth)
    }

    /**
     * Default opacity for the color of [DefaultDivider]
     */
    const val DefaultDividerOpacity = 0.12f

    /**
     * Default thickness for [DefaultDivider]
     */
    val DefaultDividerThickness = 1.dp

    /**
     * Default height for [DefaultIndicator]
     */
    val DefaultIndicatorHeight = 2.dp
}

/**
 * Data class that contains information about a tab's position on screen, used for calculating
 * where to place the indicator that shows which tab is selected.
 *
 * @property left the left edge's x position from the start of the [TabRow]
 * @property right the right edge's x position from the start of the [TabRow]
 * @property width the width of this tab
 */
@Immutable
data class TabPosition internal constructor(val left: Dp, val width: Dp) {
    val right: Dp get() = left + width
}

/**
 * A Tab represents a single page of content using a text label and/or icon. It represents its
 * selected state by tinting the text label and/or image with [activeColor].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * This Tab has slots for [text] and/or [icon] - see the other Tab overload for a generic Tab
 * that is not opinionated about its content.
 *
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param modifier optional [Modifier] for this tab
 * @param activeColor the color for the content of this tab when selected
 * @param inactiveColor the color for the content of this tab when not selected
 */
@Composable
fun Tab(
    text: @Composable () -> Unit = emptyContent(),
    icon: @Composable () -> Unit = emptyContent(),
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = contentColor(),
    inactiveColor: Color = EmphasisAmbient.current.medium.applyEmphasis(activeColor)
) {
    val styledText = @Composable {
        val style = MaterialTheme.typography.button.copy(textAlign = TextAlign.Center)
        ProvideTextStyle(style, children = text)
    }
    Tab(selected, onSelected, modifier) {
        TabTransition(activeColor, inactiveColor, selected) {
            TabBaselineLayout(icon = icon, text = styledText)
        }
    }
}

/**
 * Generic [Tab] overload that is not opinionated about content / color. See the other overload
 * for a Tab that has specific slots for text and / or an icon, as well as providing the correct
 * colors for selected / unselected states.
 *
 * A custom tab using this API may look like:
 *
 * @sample androidx.compose.material.samples.FancyTab
 *
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param modifier optional [Modifier] for this tab
 * @param content the content of this tab
 */
@Composable
fun Tab(
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .selectable(selected = selected, onClick = onSelected)
            .fillMaxWidth(),
        gravity = ContentGravity.Center,
        children = content
    )
}

private val TabTintColor = ColorPropKey()

/**
 * [transition] defining how the tint color for a tab animates, when a new tab is selected. This
 * component uses [ContentColorAmbient] to provide an interpolated value between [activeColor]
 * and [inactiveColor] depending on the animation status.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    content: @Composable () -> Unit
) {
    val transitionDefinition = remember(activeColor, inactiveColor) {
        transitionDefinition<Boolean> {
            state(true) {
                this[TabTintColor] = activeColor
            }

            state(false) {
                this[TabTintColor] = inactiveColor
            }

            transition(toState = false, fromState = true) {
                TabTintColor using tween(
                    durationMillis = TabFadeInAnimationDuration,
                    delayMillis = TabFadeInAnimationDelay,
                    easing = LinearEasing
                )
            }

            transition(fromState = true, toState = false) {
                TabTintColor using tween(
                    durationMillis = TabFadeOutAnimationDuration,
                    easing = LinearEasing
                )
            }
        }
    }
    val state = transition(transitionDefinition, selected)
    Providers(ContentColorAmbient provides state[TabTintColor], children = content)
}

/**
 * A [Layout] that positions [text] and an optional [icon] with the correct baseline distances. This
 * Layout will either be [SmallTabHeight] or [LargeTabHeight] depending on its content, and then
 * place the text and/or icon inside with the correct baseline alignment.
 */
@Composable
private fun TabBaselineLayout(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit
) {
    Layout(
        {
            Box(
                Modifier.layoutId("text"),
                paddingStart = HorizontalTextPadding,
                paddingEnd = HorizontalTextPadding,
                children = text
            )
            Box(Modifier.layoutId("icon"), children = icon)
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables.first { it.id == "text" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs
            constraints.copy(minHeight = 0)
        )

        val iconPlaceable = measurables.first { it.id == "icon" }.measure(constraints)

        val hasTextPlaceable =
            textPlaceable.width != 0 && textPlaceable.height != 0

        val hasIconPlaceable =
            iconPlaceable.width != 0 && iconPlaceable.height != 0

        val tabWidth = max(textPlaceable.width, iconPlaceable.width)

        val tabHeight =
            (if (hasTextPlaceable && hasIconPlaceable) LargeTabHeight else SmallTabHeight).toIntPx()

        val firstBaseline = textPlaceable[FirstBaseline]
        val lastBaseline = textPlaceable[LastBaseline]

        layout(tabWidth, tabHeight) {
            when {
                hasTextPlaceable && hasIconPlaceable -> placeTextAndIcon(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    iconPlaceable = iconPlaceable,
                    tabWidth = tabWidth,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline,
                    lastBaseline = lastBaseline
                )
                hasTextPlaceable -> placeText(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    tabHeight = tabHeight,
                    firstBaseline = firstBaseline,
                    lastBaseline = lastBaseline
                )
                hasIconPlaceable -> placeIcon(iconPlaceable, tabHeight)
                else -> {}
            }
        }
    }
}

/**
 * Places the provided [iconPlaceable] in the vertical center of the provided [tabHeight].
 */
private fun Placeable.PlacementScope.placeIcon(
    iconPlaceable: Placeable,
    tabHeight: Int
) {
    val iconY = (tabHeight - iconPlaceable.height) / 2
    iconPlaceable.place(0, iconY)
}

/**
 * Places the provided [textPlaceable] offset from the bottom of the tab using the correct
 * baseline offset.
 */
private fun Placeable.PlacementScope.placeText(
    density: Density,
    textPlaceable: Placeable,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int
) {
    val baselineOffset = if (firstBaseline == lastBaseline) {
        SingleLineTextBaseline
    } else {
        DoubleLineTextBaseline
    }

    // Total offset between the last text baseline and the bottom of the Tab layout
    val totalOffset = with(density) {
        baselineOffset.toIntPx() + TabConstants.DefaultIndicatorHeight.toIntPx()
    }

    val textPlaceableY = tabHeight - lastBaseline - totalOffset
    textPlaceable.place(0, textPlaceableY)
}

/**
 * Places the provided [textPlaceable] offset from the bottom of the tab using the correct
 * baseline offset, with the provided [iconPlaceable] placed above the text using the correct
 * baseline offset.
 */
private fun Placeable.PlacementScope.placeTextAndIcon(
    density: Density,
    textPlaceable: Placeable,
    iconPlaceable: Placeable,
    tabWidth: Int,
    tabHeight: Int,
    firstBaseline: Int,
    lastBaseline: Int
) {
    val baselineOffset = if (firstBaseline == lastBaseline) {
        SingleLineTextBaselineWithIcon
    } else {
        DoubleLineTextBaselineWithIcon
    }

    // Total offset between the last text baseline and the bottom of the Tab layout
    val textOffset = with(density) {
        baselineOffset.toIntPx() + TabConstants.DefaultIndicatorHeight.toIntPx()
    }

    // How much space there is between the top of the icon (essentially the top of this layout)
    // and the top of the text layout's bounding box (not baseline)
    val iconOffset = with(density) {
        iconPlaceable.height + IconDistanceFromBaseline.toIntPx() - firstBaseline
    }

    val textPlaceableX = (tabWidth - textPlaceable.width) / 2
    val textPlaceableY = tabHeight - lastBaseline - textOffset
    textPlaceable.place(textPlaceableX, textPlaceableY)

    val iconPlaceableX = (tabWidth - iconPlaceable.width) / 2
    val iconPlaceableY = textPlaceableY - iconOffset
    iconPlaceable.place(iconPlaceableX, iconPlaceableY)
}

// TabRow specifications
// How far from the start and end of a scrollable TabRow should the first Tab be displayed
private val ScrollableTabRowEdgeOffset = 52.dp
private val ScrollableTabRowMinimumTabWidth = 90.dp

// Tab specifications
private val SmallTabHeight = 48.dp
private val LargeTabHeight = 72.dp

// Tab transition specifications
private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100

// The horizontal padding on the left and right of text
private val HorizontalTextPadding = 16.dp

// Distance from the top of the indicator to the text baseline when there is one line of text
private val SingleLineTextBaseline = 18.dp
// Distance from the top of the indicator to the text baseline when there is one line of text and an
// icon
private val SingleLineTextBaselineWithIcon = 14.dp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
private val DoubleLineTextBaseline = 10.dp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
// and an icon
private val DoubleLineTextBaselineWithIcon = 6.dp
// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp
