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

import androidx.animation.FastOutSlowInEasing
import androidx.animation.LinearEasing
import androidx.animation.transitionDefinition
import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.animation.ColorPropKey
import androidx.ui.animation.PxPropKey
import androidx.ui.animation.Transition
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.DensityAmbient
import androidx.ui.core.FirstBaseline
import androidx.ui.core.LastBaseline
import androidx.ui.core.Layout
import androidx.ui.core.LayoutTag
import androidx.ui.core.LayoutTagParentData
import androidx.ui.core.Modifier
import androidx.ui.core.ParentData
import androidx.ui.core.Placeable
import androidx.ui.core.Text
import androidx.ui.core.WithConstraints
import androidx.ui.core.tag
import androidx.ui.foundation.ColoredRect
import androidx.ui.foundation.HorizontalScroller
import androidx.ui.foundation.ScrollerPosition
import androidx.ui.foundation.SimpleImage
import androidx.ui.foundation.contentColor
import androidx.ui.foundation.selection.MutuallyExclusiveSetItem
import androidx.ui.graphics.Color
import androidx.ui.graphics.Image
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.material.TabRow.TabPosition
import androidx.ui.material.ripple.Ripple
import androidx.ui.material.surface.Surface
import androidx.ui.material.surface.primarySurface
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.IntPx
import androidx.ui.unit.Px
import androidx.ui.unit.dp
import androidx.ui.unit.max
import androidx.ui.unit.px
import androidx.ui.unit.sp
import androidx.ui.unit.toPx

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
 * @param color The background color for the TabRow. Use [Color.Transparent] to have no color.
 * @param contentColor The preferred content color provided by this TabRow to its children.
 * Defaults to either the matching `onFoo` color for [color], or if [color] is not a color from
 * the theme, this will keep the same value set above this TabRow.
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
    color: Color = MaterialTheme.colors().primarySurface,
    contentColor: Color = contentColorFor(color),
    scrollable: Boolean = false,
    indicatorContainer: @Composable() (tabPositions: List<TabPosition>) -> Unit = { tabPositions ->
        TabRow.IndicatorContainer(tabPositions, selectedIndex) {
            TabRow.Indicator()
        }
    },
    divider: @Composable() () -> Unit = {
        Divider(height = 1.dp, color = contentColor().copy(alpha = DividerOpacity))
    },
    tab: @Composable() (Int, T) -> Unit
) {
    Surface(color = color, contentColor = contentColor) {
        WithConstraints { constraints ->
            val width = constraints.maxWidth
            // TODO: force scrollable for tabs that will be too small if they take up equal space?
            if (scrollable) {
                val tabs = @Composable {
                    items.forEachIndexed { index, item ->
                        tab(index, item)
                    }
                }
                ScrollableTabRow(width, selectedIndex, tabs, indicatorContainer, divider)
            } else {
                val tabs = @Composable { modifier: Modifier ->
                    items.forEachIndexed { index, item ->
                        Container(modifier) {
                            tab(index, item)
                        }
                    }
                }
                FixedTabRow(width, items.size, tabs, indicatorContainer, divider)
            }
        }
    }
}

@Composable
private fun FixedTabRow(
    width: IntPx,
    tabCount: Int,
    tabs: @Composable() (Modifier) -> Unit,
    indicatorContainer: @Composable() (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable() () -> Unit
) {
    val tabWidth = width / tabCount

    val tabPositions = remember(tabCount, tabWidth) {
        (0 until tabCount).map { index ->
            val left = (tabWidth * index)
            TabPosition(left, tabWidth)
        }
    }

    Stack(LayoutWidth.Fill) {
        Row(LayoutGravity.Center) {
            tabs(LayoutFlexible(1f))
        }
        Container(LayoutGravity.BottomCenter + LayoutWidth.Fill, children = divider)
        Container(LayoutGravity.Stretch) {
            indicatorContainer(tabPositions)
        }
    }
}

@Composable
private fun ScrollableTabRow(
    width: IntPx,
    selectedIndex: Int,
    tabs: @Composable() () -> Unit,
    indicatorContainer: @Composable() (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable() () -> Unit
) {
    val edgeOffset = with(DensityAmbient.current) { ScrollableTabRowEdgeOffset.toIntPx() }

    // TODO: unfortunate 1f lag as we need to first calculate tab positions before drawing the
    // indicator container
    var tabPositions by state { listOf<TabPosition>() }

    val scrollableTabData = ScrollableTabData(selectedIndex, tabPositions, width, edgeOffset)

    scrollableTabData.tabPositions = tabPositions
    scrollableTabData.selectedTab = selectedIndex
    scrollableTabData.visibleWidth = width

    val indicator = @Composable {
        // Delay indicator composition until we know tab positions
        if (tabPositions.isNotEmpty()) {
            indicatorContainer(tabPositions)
        }
    }

    HorizontalScroller(
        scrollerPosition = scrollableTabData.position,
        modifier = LayoutWidth.Fill
    ) {
        val tabTag = "tab"
        val indicatorTag = "indicator"
        val dividerTag = "divider"
        Layout(
            {
                ParentData(
                    object : LayoutTagParentData {
                        override val tag = tabTag
                    },
                    children = tabs
                )
                Container(LayoutTag(indicatorTag), children = indicator)
                Container(LayoutTag(dividerTag), children = divider)
            }
        ) { measurables, constraints ->
            val tabPlaceables = mutableListOf<Pair<Placeable, IntPx>>()
            val minTabWidth = ScrollableTabRowMinimumTabWidth.toIntPx()

            var layoutHeight = IntPx.Zero

            val tabConstraints = constraints.copy(minWidth = minTabWidth)

            val newTabPositions = mutableListOf<TabPosition>()

            val layoutWidth = measurables
                .filter { it.tag == tabTag }
                .fold(edgeOffset) { sum, measurable ->
                    val placeable = measurable.measure(tabConstraints)

                    if (placeable.height > layoutHeight) {
                        layoutHeight = placeable.height
                    }

                    // Position each tab at the end of the previous one
                    tabPlaceables.add(placeable to sum)
                    newTabPositions.add(TabPosition(left = sum, width = placeable.width))
                    sum + placeable.width
                } + edgeOffset

            if (tabPositions != newTabPositions) {
                tabPositions = newTabPositions
            }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                tabPlaceables.forEach { (placeable, left) ->
                    placeable.place(left, IntPx.Zero)
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                measurables.firstOrNull { it.tag == dividerTag }
                    ?.measure(constraints.copy(minWidth = layoutWidth, maxWidth = layoutWidth))
                    ?.run { place(IntPx.Zero, layoutHeight - height) }

                // The indicator container is measured to fill the entire space occupied by the tab
                // row, and then placed on top of the divider.
                measurables.firstOrNull { it.tag == indicatorTag }
                    ?.measure(Constraints.fixed(layoutWidth, layoutHeight))
                    ?.place(IntPx.Zero, IntPx.Zero)
            }
        }
    }
}

@Composable
private fun ScrollableTabData(
    initialSelectedTab: Int,
    tabPositions: List<TabPosition>,
    visibleWidth: IntPx,
    edgeOffset: IntPx
) = ScrollerPosition().let {
    remember(it) {
        ScrollableTabData(
            position = it,
            initialSelectedTab = initialSelectedTab,
            tabPositions = tabPositions,
            visibleWidth = visibleWidth,
            edgeOffset = edgeOffset
        )
    }
}

/**
 * Class holding onto state needed for [ScrollableTabRow]
 */
private class ScrollableTabData(
    val position: ScrollerPosition,
    initialSelectedTab: Int,
    var tabPositions: List<TabPosition>,
    var visibleWidth: IntPx,
    val edgeOffset: IntPx
) {
    var selectedTab: Int = initialSelectedTab
        set(value) {
            if (field != value && !isTabFullyVisible(value)) {
                val calculatedOffset = calculateTabOffset(value)
                position.smoothScrollTo(calculatedOffset.value)
            }
            field = value
        }

    private fun isTabFullyVisible(index: Int): Boolean {
        val tabPosition = tabPositions[index]
        val leftEdgeStart = position.value.px
        val leftEdgeVisible = leftEdgeStart <= tabPosition.left.toPx()
        val rightEdgeEnd = leftEdgeStart + visibleWidth
        val rightEdgeVisible = rightEdgeEnd >= tabPosition.right.toPx()
        return leftEdgeVisible && rightEdgeVisible
    }

    // TODO: this currently always centers the tab (if possible), should we support only scrolling
    // until the tab just becomes visible?
    private fun calculateTabOffset(index: Int): Px {
        val tabPosition = tabPositions[index]
        val tabOffset = tabPosition.left
        val scrollerCenter = visibleWidth / 2
        val tabWidth = tabPosition.width
        val centeredTabOffset = tabOffset - (scrollerCenter - tabWidth / 2)
        val totalTabRowWidth = tabPositions.last().right + edgeOffset
        return centeredTabOffset.coerceIn(IntPx.Zero, totalTabRowWidth - visibleWidth).toPx()
    }
}

object TabRow {
    private val IndicatorOffset = PxPropKey()

    /**
     * Data class that contains information about a tab's position on screen
     *
     * @property left the left edge's x position from the start of the [TabRow]
     * @property right the right edge's x position from the start of the [TabRow]
     * @property width the width of this tab
     */
    data class TabPosition internal constructor(val left: IntPx, val width: IntPx) {
        val right: IntPx get() = left + width
    }

    /**
     * Positions and animates the given [indicator] between tabs when [selectedIndex] changes.
     */
    @Composable
    fun IndicatorContainer(
        tabPositions: List<TabPosition>,
        selectedIndex: Int,
        indicator: @Composable() () -> Unit
    ) {
        // TODO: should we animate the width of the indicator as it moves between tabs of different
        // sizes inside a scrollable tab row?
        val currentTabWidth = with(DensityAmbient.current) {
            tabPositions[selectedIndex].width.toDp()
        }

        Container(expanded = true, alignment = Alignment.BottomStart) {
            IndicatorTransition(tabPositions, selectedIndex) { indicatorOffset ->
                val offset = with(DensityAmbient.current) { indicatorOffset.toDp() }
                Container(
                    modifier = LayoutPadding(start = offset),
                    width = currentTabWidth,
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
    fun Indicator(modifier: Modifier = Modifier.None, color: Color = contentColor()) {
        ColoredRect(color = color, height = IndicatorHeight, modifier = modifier)
    }

    /**
     * [Transition] that animates the indicator offset between a given list of [TabPosition]s.
     */
    @Composable
    internal fun IndicatorTransition(
        tabPositions: List<TabPosition>,
        selectedIndex: Int,
        children: @Composable() (indicatorOffset: Px) -> Unit
    ) {
        val transitionDefinition = remember(tabPositions) {
            transitionDefinition {
                // TODO: currently the first state set is the 'default' state, so we want to define the
                // state that is initially selected first, so we don't have any initial animations.
                // When this is supported by transitionDefinition, we should fix this to just set a
                // default or similar.
                state(selectedIndex) {
                    this[IndicatorOffset] = tabPositions[selectedIndex].left.toPx()
                }

                tabPositions.forEachIndexed { index, position ->
                    if (index != selectedIndex) {
                        state(index) {
                            this[IndicatorOffset] = position.left.toPx()
                        }
                    }
                }

                transition {
                    IndicatorOffset using tween {
                        duration = 250
                        easing = FastOutSlowInEasing
                    }
                }
            }
        }

        Transition(transitionDefinition, selectedIndex) { state ->
            children(state[IndicatorOffset])
        }
    }
}

/**
 * A Tab represents a single page of content using a text label and/or image. It represents its
 * selected state by tinting the text label and/or image with [ColorPalette.onPrimary].
 *
 * This should typically be used inside of a [TabRow], see the corresponding documentation for
 * example usage.
 *
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param activeColor the color for the content of this tab when selected
 * @param inactiveColor the color for the content of this tab when not selected
 */
@Composable
fun Tab(
    text: String? = null,
    icon: Image? = null,
    selected: Boolean,
    onSelected: () -> Unit,
    activeColor: Color = contentColor(),
    inactiveColor: Color = MaterialTheme.emphasisLevels().medium.emphasize(activeColor)
) {
    when {
        text != null && icon != null -> CombinedTab(
            text,
            icon,
            selected,
            onSelected,
            activeColor,
            inactiveColor
        )
        text != null -> TextTab(text, selected, onSelected, activeColor, inactiveColor)
        icon != null -> IconTab(icon, selected, onSelected, activeColor, inactiveColor)
        // Nothing provided here (?!), so let's just draw an empty tab that handles clicks
        else -> BaseTab(selected, onSelected, {})
    }
}

/**
 * A base Tab that displays some content inside of a clickable ripple.
 *
 * Also handles setting the correct semantic properties for accessibility purposes.
 *
 * @param selected whether this tab is selected or not, this is used to set the correct semantics
 * @param onSelected the callback to be invoked when this tab is selected
 * @param children the composable content to be displayed inside of this Tab
 */
@Composable
private fun BaseTab(selected: Boolean, onSelected: () -> Unit, children: @Composable() () -> Unit) {
    Ripple(bounded = true) {
        MutuallyExclusiveSetItem(selected = selected, onClick = onSelected, children = children)
    }
}

/**
 * A Tab that contains a text label, and represents its selected state by using [activeColor] and
 * [inactiveColor] for the color of the text label.
 *
 * @param text the text label displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param activeColor the color for the content of this tab when selected
 * @param inactiveColor the color for the content of this tab when not selected
 */
@Composable
private fun TextTab(
    text: String,
    selected: Boolean,
    onSelected: () -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    BaseTab(selected = selected, onSelected = onSelected) {
        Container(LayoutWidth.Fill + LayoutHeight(SmallTabHeight)) {
            TabTransition(activeColor, inactiveColor, selected) { tabTintColor ->
                TabTextBaselineLayout {
                    TabText(text, tabTintColor)
                }
            }
        }
    }
}

/**
 * A Tab that contains an icon, and represents its selected state by using [activeColor] and
 * [inactiveColor] for icon color.
 *
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param activeColor the color for the content of this tab when selected
 * @param inactiveColor the color for the content of this tab when not selected
 */
@Composable
private fun IconTab(
    icon: Image,
    selected: Boolean,
    onSelected: () -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    BaseTab(selected = selected, onSelected = onSelected) {
        Container(LayoutWidth.Fill + LayoutHeight(SmallTabHeight)) {
            TabTransition(activeColor, inactiveColor, selected) { tabTintColor ->
                TabIcon(icon, tabTintColor)
            }
        }
    }
}

/**
 * A Tab that contains a text label and an icon, and represents its selected state by using
 * [activeColor] and [inactiveColor] for text and icon color.
 *
 * @param text the text label displayed in this tab
 * @param icon the icon displayed in this tab
 * @param selected whether this tab is selected or not
 * @param onSelected the callback to be invoked when this tab is selected
 * @param activeColor the color for the content of this tab when selected
 * @param inactiveColor the color for the content of this tab when not selected
 */
@Composable
private fun CombinedTab(
    text: String,
    icon: Image,
    selected: Boolean,
    onSelected: () -> Unit,
    activeColor: Color,
    inactiveColor: Color
) {
    BaseTab(selected = selected, onSelected = onSelected) {
        Container(LayoutWidth.Fill + LayoutHeight(LargeTabHeight)) {
            TabTransition(activeColor, inactiveColor, selected) { tabTintColor ->
                TabTextBaselineLayout(
                    icon = { TabIcon(icon, tabTintColor) },
                    text = { TabText(text, tabTintColor) }
                )
            }
        }
    }
}

private val TabTintColor = ColorPropKey()

/**
 * [Transition] defining how the tint color for a tab animates, when a new tab is selected.
 */
@Composable
private fun TabTransition(
    activeColor: Color,
    inactiveColor: Color,
    selected: Boolean,
    children: @Composable() (color: Color) -> Unit
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
                TabTintColor using tween {
                    duration = TabFadeInAnimationDuration
                    delay = TabFadeInAnimationDelay
                    easing = LinearEasing
                }
            }

            transition(fromState = true, toState = false) {
                TabTintColor using tween {
                    duration = TabFadeOutAnimationDuration
                    easing = LinearEasing
                }
            }
        }
    }
    Transition(transitionDefinition, selected) { state ->
        children(state[TabTintColor])
    }
}

@Composable
private fun TabText(text: String, color: Color) {
    val buttonTextStyle = MaterialTheme.typography().button
    Text(
        text = text,
        style = buttonTextStyle.copy(color = color, textAlign = TextAlign.Center),
        maxLines = TextLabelMaxLines,
        modifier = LayoutPadding(start = HorizontalTextPadding, end = HorizontalTextPadding)
    )
}

/**
 * A [Layout] that positions [text] and an optional [icon] with the correct baseline distances. This
 * Layout will expand to fit the full height of the tab, and then place the text and icon positioned
 * correctly from the bottom edge of the tab.
 */
@Composable
private fun TabTextBaselineLayout(
    icon: @Composable() () -> Unit = emptyContent(),
    text: @Composable() () -> Unit
) {
    Layout(
        {
            Container(LayoutTag("text"), children = text)
            Container(LayoutTag("icon"), children = icon)
        }
    ) { measurables, constraints ->
        val textPlaceable = measurables.first { it.tag == "text" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs
            constraints.copy(minHeight = IntPx.Zero)
        )

        val firstBaseline =
            requireNotNull(textPlaceable[FirstBaseline]) { "No text baselines found" }
        val lastBaseline =
            requireNotNull(textPlaceable[LastBaseline]) { "No text baselines found" }

        val iconPlaceable = measurables.firstOrNull { it.tag == "icon" }?.measure(constraints)

        // Total offset from the bottom of this layout to the last text baseline
        val baselineOffset = if (firstBaseline == lastBaseline) {
            if (iconPlaceable == null) {
                SingleLineTextBaseline
            } else {
                SingleLineTextBaselineWithIcon
            }
        } else {
            if (iconPlaceable == null) {
                DoubleLineTextBaseline
            } else {
                DoubleLineTextBaselineWithIcon
            }
        }.toIntPx() + IndicatorHeight.toIntPx()

        val textHeight = textPlaceable.height

        // How much space there is between the bottom of the text layout's bounding box (not
        // baseline) and the bottom of this layout.
        val textOffsetBelow = baselineOffset - textHeight + lastBaseline

        if (iconPlaceable == null) {

            val containerWidth = textPlaceable.width
            val contentHeight = textPlaceable.height + textOffsetBelow
            val containerHeight = constraints.maxHeight
            layout(containerWidth, containerHeight) {
                val textPlaceableY = containerHeight - contentHeight
                textPlaceable.place(IntPx.Zero, textPlaceableY)
            }
        } else {
            // How much space there is between the top of the text layout's bounding box (not
            // baseline) and the top of the icon (essentially the top of this layout).
            val textOffsetAbove =
                iconPlaceable.height + IconDistanceFromBaseline.toIntPx() - firstBaseline

            val containerWidth = max(textPlaceable.width, iconPlaceable.width)
            val contentHeight = textOffsetAbove + textPlaceable.height + textOffsetBelow
            val containerHeight = constraints.maxHeight
            layout(containerWidth, containerHeight) {
                val iconPlaceableX = (containerWidth - iconPlaceable.width) / 2
                val iconPlaceableY = containerHeight - contentHeight
                iconPlaceable.place(iconPlaceableX, iconPlaceableY)

                val textPlaceableX = (containerWidth - textPlaceable.width) / 2
                val textPlaceableY = iconPlaceableY + textOffsetAbove
                textPlaceable.place(textPlaceableX, textPlaceableY)
            }
        }
    }
}

@Composable
private fun TabIcon(icon: Image, tint: Color) {
    Container(width = IconDiameter, height = IconDiameter) {
        SimpleImage(icon, tint)
    }
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
private const val TextLabelMaxLines = 2

// Tab transition specifications
private const val TabFadeInAnimationDuration = 150
private const val TabFadeInAnimationDelay = 100
private const val TabFadeOutAnimationDuration = 100

// The horizontal padding on the left and right of text
private val HorizontalTextPadding = 16.dp

private val IconDiameter = 24.dp

// Distance from the top of the indicator to the text baseline when there is one line of text
private val SingleLineTextBaseline = 18.sp
// Distance from the top of the indicator to the text baseline when there is one line of text and an
// icon
private val SingleLineTextBaselineWithIcon = 14.sp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
private val DoubleLineTextBaseline = 10.sp
// Distance from the top of the indicator to the last text baseline when there are two lines of text
// and an icon
private val DoubleLineTextBaselineWithIcon = 6.sp
// Distance from the first text baseline to the bottom of the icon in a combined tab
private val IconDistanceFromBaseline = 20.sp
