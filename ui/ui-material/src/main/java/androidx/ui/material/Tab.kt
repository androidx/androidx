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

package androidx.ui.material

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.compose.Providers
import androidx.compose.emptyContent
import androidx.compose.getValue
import androidx.compose.remember
import androidx.compose.setValue
import androidx.compose.state
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.DpPropKey
import androidx.ui.animation.transition
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.Placeable
import androidx.ui.core.WithConstraints
import androidx.ui.core.id
import androidx.ui.core.layoutId
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
import androidx.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredWidth
import androidx.ui.material.TabRow.IndicatorTransition
import androidx.ui.material.TabRow.TabPosition
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.foundation.text.LastBaseline
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import androidx.ui.util.fastFirstOrNull
import androidx.ui.util.fastForEach
import androidx.ui.util.fastForEachIndexed
import kotlin.math.max

/**
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.ui.material.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.ui.material.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.ui.material.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicatorContainer], to customize
 * the indicator displayed for a tab. [indicatorContainer] is responsible for positioning an
 * indicator and for animating its position when [selectedIndex] changes.
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.ui.material.samples.FancyIndicator
 *
 * We can reuse [TabRow.IndicatorContainer] and just provide this indicator, as we aren't changing
 * the transition:
 *
 * @sample androidx.ui.material.samples.FancyIndicatorTabs
 *
 * You may also want to provide a custom transition, to allow you to dynamically change the
 * appearance of the indicator as it animates between tabs, such as changing its color or size.
 * [indicatorContainer] is stacked on top of the entire TabRow, so you just need to provide a custom
 * container that animates the offset of the indicator from the start of the TabRow and place your
 * custom indicator inside of it. For example, take the following custom container that animates
 * position of the indicator, the color of the indicator, and also adds a physics based 'spring'
 * effect to the indicator in the direction of motion:
 *
 * @sample androidx.ui.material.samples.FancyIndicatorContainer
 *
 * This container will fill up the entire width of the TabRow, and when a new tab is selected,
 * the transition will be called with a new value for [selectedIndex], which will animate the
 * indicator to the position of the new tab.
 *
 * We can use this custom container similarly to before:
 *
 * @sample androidx.ui.material.samples.FancyIndicatorContainerTabs
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
 * @param indicatorContainer the container responsible for positioning and animating the position of
 * the indicator between tabs. By default this will be [TabRow.IndicatorContainer], which animates a
 * [TabRow.Indicator] between tabs.
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
    indicatorContainer: @Composable (tabPositions: List<TabPosition>) -> Unit = { tabPositions ->
        TabRow.IndicatorContainer(tabPositions, selectedIndex) {
            TabRow.Indicator()
        }
    },
    divider: @Composable () -> Unit = {
        Divider(thickness = 1.dp, color = contentColor().copy(alpha = DividerOpacity))
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
            ScrollableTabRow(selectedIndex, tabs, indicatorContainer, divider)
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
            FixedTabRow(items.size, tabs, indicatorContainer, divider)
        }
    }
}

@Composable
private fun FixedTabRow(
    tabCount: Int,
    tabs: @Composable (Modifier) -> Unit,
    indicatorContainer: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit
) {
    Stack(Modifier.fillMaxWidth()) {
        Row {
            tabs(Modifier.weight(1f))
        }
        Box(Modifier.gravity(Alignment.BottomCenter).fillMaxWidth(), children = divider)
        WithConstraints(Modifier.matchParentSize()) {
            val width = constraints.maxWidth
            val tabWidth = width / tabCount
            val density = DensityAmbient.current

            val tabPositions = remember(tabCount, tabWidth) {
                with(density) {
                    (0 until tabCount).map { index ->
                        val left = (tabWidth * index)
                        TabPosition(left.toDp(), tabWidth.toDp())
                    }
                }
            }
            indicatorContainer(tabPositions)
        }
    }
}

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

    // TODO: unfortunate 1f lag as we need to first calculate tab positions before drawing the
    // indicator container
    var tabPositions by state { listOf<TabPosition>() }

    val indicator = @Composable {
        // Delay indicator composition until we know tab positions
        if (tabPositions.isNotEmpty()) {
            indicatorContainer(tabPositions)
        }
    }

    val indicatorTag = "indicator"
    val dividerTag = "divider"
    ScrollableRow(modifier = Modifier.fillMaxWidth(), scrollState = scrollState) {
        Layout(
            children = {
                tabs()
                Box(Modifier.layoutId(indicatorTag), children = indicator)
                Box(Modifier.layoutId(dividerTag), children = divider)
            }
        ) { measurables, constraints ->
            val tabPlaceables = mutableListOf<Pair<Placeable, Int>>()
            val minTabWidth = ScrollableTabRowMinimumTabWidth.toIntPx()
            val edgeOffset = ScrollableTabRowEdgeOffset.toIntPx()

            var layoutHeight = 0

            val tabConstraints = constraints.copy(minWidth = minTabWidth)

            val newTabPositions = mutableListOf<TabPosition>()

            val layoutWidth = measurables
                // to avoid wrapping each tab with the Box only to pass the LayoutTag
                .filter { it.id == null }
                .fold(edgeOffset) { sum, measurable ->
                    val placeable = measurable.measure(tabConstraints)

                    if (placeable.height > layoutHeight) {
                        layoutHeight = placeable.height
                    }

                    // Position each tab at the end of the previous one
                    tabPlaceables.add(placeable to sum)
                    newTabPositions.add(
                        TabPosition(
                            left = sum.toDp(),
                            width = placeable.width.toDp()
                        )
                    )
                    sum + placeable.width
                } + edgeOffset

            if (tabPositions != newTabPositions) {
                tabPositions = newTabPositions
            }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                tabPlaceables.fastForEach { (placeable, left) ->
                    placeable.place(left, 0)
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                measurables.fastFirstOrNull { it.id == dividerTag }
                    ?.measure(constraints.copy(minWidth = layoutWidth, maxWidth = layoutWidth))
                    ?.run { place(0, layoutHeight - height) }

                // The indicator container is measured to fill the entire space occupied by the tab
                // row, and then placed on top of the divider.
                measurables.fastFirstOrNull { it.id == indicatorTag }
                    ?.measure(Constraints.fixed(layoutWidth, layoutHeight))
                    ?.place(0, 0)

                scrollableTabData.onLaidOut(
                    density = this@Layout,
                    edgeOffset = edgeOffset,
                    tabPositions = tabPositions,
                    selectedTab = selectedIndex
                )
            }
        }
    }
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

object TabRow {
    private val IndicatorOffset = DpPropKey()

    /**
     * Data class that contains information about a tab's position on screen
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
     * Positions and animates the given [indicator] between tabs when [selectedIndex] changes.
     */
    @Composable
    fun IndicatorContainer(
        tabPositions: List<TabPosition>,
        selectedIndex: Int,
        indicator: @Composable () -> Unit
    ) {
        // TODO: should we animate the width of the indicator as it moves between tabs of different
        // sizes inside a scrollable tab row?
        val currentTabWidth = tabPositions[selectedIndex].width

        Box(Modifier.fillMaxSize(), gravity = ContentGravity.BottomStart) {
            IndicatorTransition(tabPositions, selectedIndex) { indicatorOffset ->
                Box(
                    Modifier.padding(start = indicatorOffset).preferredWidth(currentTabWidth),
                    children = indicator
                )
            }
        }
    }

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param color color of the indicator
     */
    @Composable
    fun Indicator(modifier: Modifier = Modifier, color: Color = contentColor()) {
        Box(modifier.fillMaxWidth().preferredHeight(IndicatorHeight).background(color = color))
    }

    /**
     * [transition] that animates the indicator offset between a given list of [TabPosition]s.
     */
    @Composable
    internal fun IndicatorTransition(
        tabPositions: List<TabPosition>,
        selectedIndex: Int,
        indicator: @Composable (indicatorOffset: Dp) -> Unit
    ) {
        val transitionDefinition = remember(tabPositions) {
            transitionDefinition {
                // TODO: currently the first state set is the 'default' state, so we want to define the
                // state that is initially selected first, so we don't have any initial animations.
                // When this is supported by transitionDefinition, we should fix this to just set a
                // default or similar.
                state(selectedIndex) {
                    this[IndicatorOffset] = tabPositions[selectedIndex].left
                }

                tabPositions.forEachIndexed { index, position ->
                    if (index != selectedIndex) {
                        state(index) {
                            this[IndicatorOffset] = position.left
                        }
                    }
                }

                transition {
                    IndicatorOffset using tween(
                        durationMillis = 250,
                        easing = FastOutSlowInEasing
                    )
                }
            }
        }

        val state = transition(transitionDefinition, selectedIndex)
        indicator(state[IndicatorOffset])
    }
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
 * @sample androidx.ui.material.samples.FancyTab
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
        transitionDefinition {
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
                    firstBaseline = requireNotNull(firstBaseline) { "No text baselines found" },
                    lastBaseline = requireNotNull(lastBaseline) { "No text baselines found" }
                )
                hasTextPlaceable -> placeText(
                    density = this@Layout,
                    textPlaceable = textPlaceable,
                    tabHeight = tabHeight,
                    firstBaseline = requireNotNull(firstBaseline) { "No text baselines found" },
                    lastBaseline = requireNotNull(lastBaseline) { "No text baselines found" }
                )
                hasIconPlaceable -> placeIcon(iconPlaceable, tabHeight)
                else -> {
                }
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
    val totalOffset = with(density) { baselineOffset.toIntPx() + IndicatorHeight.toIntPx() }

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
    val textOffset = with(density) { baselineOffset.toIntPx() + IndicatorHeight.toIntPx() }

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
private val IndicatorHeight = 2.dp
private const val DividerOpacity = 0.12f
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
