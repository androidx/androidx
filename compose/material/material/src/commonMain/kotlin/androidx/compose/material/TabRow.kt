/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://material.io/components/tabs#fixed-tabs" class="external" target="_blank">Material Design fixed tabs</a>.
 *
 * Fixed tabs display all tabs in a set simultaneously. They are best for switching between related
 * content quickly, such as between transportation methods in a map. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * ![Fixed tabs image](https://developer.android.com/images/reference/androidx/compose/material/fixed-tabs.png)
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A TabRow places its tabs evenly spaced along the entire row, with each tab
 * taking up an equal amount of space. See [ScrollableTabRow] for a tab row that does not enforce
 * equal size, and allows scrolling to tabs that do not fit on screen.
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
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator,
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
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier optional [Modifier] for this TabRow
 * @param backgroundColor The background color for the TabRow. Use [Color.Transparent] to have
 * no color.
 * @param contentColor The preferred content color provided by this TabRow to its children.
 * Defaults to either the matching content color for [backgroundColor], or if [backgroundColor] is
 * not a color from the theme, this will keep the same value set above this TabRow.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 * will be a [TabRowDefaults.Indicator], using a [TabRowDefaults.tabIndicatorOffset]
 * modifier to animate its position. Note that this indicator will be forced to fill up the
 * entire TabRow, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to
 * animate the actual drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the TabRow. This provides a layer of
 * separation between the TabRow and the content displayed underneath.
 * @param tabs the tabs inside this TabRow. Typically this will be multiple [Tab]s. Each element
 * inside this lambda will be measured and placed evenly across the TabRow, each taking up equal
 * space.
 */
@Composable
@UiComposable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    indicator: @Composable @UiComposable
        (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
        TabRowDefaults.Indicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
        )
    },
    divider: @Composable @UiComposable () -> Unit =
        @Composable {
            TabRowDefaults.Divider()
        },
    tabs: @Composable @UiComposable () -> Unit
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = backgroundColor,
        contentColor = contentColor
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)
            val tabCount = tabMeasurables.size
            val tabWidth = (tabRowWidth / tabCount)
            val tabPlaceables = tabMeasurables.fastMap {
                it.measure(constraints.copy(minWidth = tabWidth, maxWidth = tabWidth))
            }

            val tabRowHeight = tabPlaceables.fastMaxBy { it.height }?.height ?: 0

            val tabPositions = List(tabCount) { index ->
                TabPosition(tabWidth.toDp() * index, tabWidth.toDp())
            }

            layout(tabRowWidth, tabRowHeight) {
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }

                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(constraints.copy(minHeight = 0))
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                subcompose(TabSlots.Indicator) {
                    indicator(tabPositions)
                }.fastForEach {
                    it.measure(Constraints.fixed(tabRowWidth, tabRowHeight)).placeRelative(0, 0)
                }
            }
        }
    }
}

/**
 * <a href="https://material.io/components/tabs#scrollable-tabs" class="external" target="_blank">Material Design scrollable tabs</a>.
 *
 * When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable tabs can use longer text
 * labels and a larger number of tabs. They are best used for browsing on touch interfaces.
 *
 * ![Scrollable tabs image](https://developer.android.com/images/reference/androidx/compose/material/scrollable-tabs.png)
 *
 * A ScrollableTabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A ScrollableTabRow places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow
 * scrolling, and evenly places its tabs, see [TabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier optional [Modifier] for this ScrollableTabRow
 * @param backgroundColor The background color for the ScrollableTabRow. Use [Color.Transparent] to
 * have no color.
 * @param contentColor The preferred content color provided by this ScrollableTabRow to its
 * children. Defaults to either the matching content color for [backgroundColor], or if
 * [backgroundColor] is not a color from the theme, this will keep the same value set above this
 * ScrollableTabRow.
 * @param edgePadding the padding between the starting and ending edge of ScrollableTabRow, and
 * the tabs inside the ScrollableTabRow. This padding helps inform the user that this tab row can
 * be scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 * will be a [TabRowDefaults.Indicator], using a [TabRowDefaults.tabIndicatorOffset]
 * modifier to animate its position. Note that this indicator will be forced to fill up the
 * entire ScrollableTabRow, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to
 * animate the actual drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the ScrollableTabRow. This provides a layer
 * of separation between the ScrollableTabRow and the content displayed underneath.
 * @param tabs the tabs inside this ScrollableTabRow. Typically this will be multiple [Tab]s. Each
 * element inside this lambda will be measured and placed evenly across the TabRow, each taking
 * up equal space.
 */
@Composable
@UiComposable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowPadding,
    indicator: @Composable @UiComposable
        (tabPositions: List<TabPosition>) -> Unit = @Composable { tabPositions ->
        TabRowDefaults.Indicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
        )
    },
    divider: @Composable @UiComposable () -> Unit =
        @Composable {
            TabRowDefaults.Divider()
        },
    tabs: @Composable @UiComposable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = backgroundColor,
        contentColor = contentColor
    ) {
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val scrollableTabData = remember(scrollState, coroutineScope) {
            ScrollableTabData(
                scrollState = scrollState,
                coroutineScope = coroutineScope
            )
        }
        SubcomposeLayout(
            Modifier.fillMaxWidth()
                .wrapContentSize(align = Alignment.CenterStart)
                .horizontalScroll(scrollState)
                .selectableGroup()
                .clipToBounds()
        ) { constraints ->
            val minTabWidth = ScrollableTabRowMinimumTabWidth.roundToPx()
            val padding = edgePadding.roundToPx()
            val tabConstraints = constraints.copy(minWidth = minTabWidth)

            val tabPlaceables = subcompose(TabSlots.Tabs, tabs)
                .fastMap { it.measure(tabConstraints) }

            var layoutWidth = padding * 2
            var layoutHeight = 0
            tabPlaceables.fastForEach {
                layoutWidth += it.width
                layoutHeight = maxOf(layoutHeight, it.height)
            }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                val tabPositions = mutableListOf<TabPosition>()
                var left = padding
                tabPlaceables.fastForEach {
                    it.placeRelative(left, 0)
                    tabPositions.add(TabPosition(left = left.toDp(), width = it.width.toDp()))
                    left += it.width
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(
                        constraints.copy(
                            minHeight = 0,
                            minWidth = layoutWidth,
                            maxWidth = layoutWidth
                        )
                    )
                    placeable.placeRelative(0, layoutHeight - placeable.height)
                }

                // The indicator container is measured to fill the entire space occupied by the tab
                // row, and then placed on top of the divider.
                subcompose(TabSlots.Indicator) {
                    indicator(tabPositions)
                }.fastForEach {
                    it.measure(Constraints.fixed(layoutWidth, layoutHeight)).placeRelative(0, 0)
                }

                scrollableTabData.onLaidOut(
                    density = this@SubcomposeLayout,
                    edgeOffset = padding,
                    tabPositions = tabPositions,
                    selectedTab = selectedTabIndex
                )
            }
        }
    }
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
class TabPosition internal constructor(val left: Dp, val width: Dp) {
    val right: Dp get() = left + width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabPosition) return false

        if (left != other.left) return false
        if (width != other.width) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + width.hashCode()
        return result
    }

    override fun toString(): String {
        return "TabPosition(left=$left, right=$right, width=$width)"
    }
}

/**
 * Contains default implementations and values used for TabRow.
 */
object TabRowDefaults {
    /**
     * Default [Divider], which will be positioned at the bottom of the [TabRow], underneath the
     * indicator.
     *
     * @param modifier modifier for the divider's layout
     * @param thickness thickness of the divider
     * @param color color of the divider
     */
    @Composable
    fun Divider(
        modifier: Modifier = Modifier,
        thickness: Dp = DividerThickness,
        color: Color = LocalContentColor.current.copy(alpha = DividerOpacity)
    ) {
        androidx.compose.material.Divider(modifier = modifier, thickness = thickness, color = color)
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
    fun Indicator(
        modifier: Modifier = Modifier,
        height: Dp = IndicatorHeight,
        color: Color = LocalContentColor.current
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
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
    fun Modifier.tabIndicatorOffset(
        currentTabPosition: TabPosition
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "tabIndicatorOffset"
            value = currentTabPosition
        }
    ) {
        val currentTabWidth by animateDpAsState(
            targetValue = currentTabPosition.width,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        val indicatorOffset by animateDpAsState(
            targetValue = currentTabPosition.left,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
            .width(currentTabWidth)
    }

    /**
     * Default opacity for the color of [Divider]
     */
    const val DividerOpacity = 0.12f

    /**
     * Default thickness for [Divider]
     */
    val DividerThickness = 1.dp

    /**
     * Default height for [Indicator]
     */
    val IndicatorHeight = 2.dp

    /**
     * The default padding from the starting edge before a tab in a [ScrollableTabRow].
     */
    val ScrollableTabRowPadding = 52.dp
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
    private val coroutineScope: CoroutineScope
) {
    private var selectedTab: Int? = null

    fun onLaidOut(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>,
        selectedTab: Int
    ) {
        // Animate if the new tab is different from the old tab, or this is called for the first
        // time (i.e selectedTab is `null`).
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab
            tabPositions.getOrNull(selectedTab)?.let {
                // Scrolls to the tab with [tabPosition], trying to place it in the center of the
                // screen or as close to the center as possible.
                val calculatedOffset = it.calculateTabOffset(density, edgeOffset, tabPositions)
                if (scrollState.value != calculatedOffset) {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(
                            calculatedOffset,
                            animationSpec = ScrollableTabRowScrollSpec
                        )
                    }
                }
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
    ): Int = with(density) {
        val totalTabRowWidth = tabPositions.last().right.roundToPx() + edgeOffset
        val visibleWidth = totalTabRowWidth - scrollState.maxValue
        val tabOffset = left.roundToPx()
        val scrollerCenter = visibleWidth / 2
        val tabWidth = width.roundToPx()
        val centeredTabOffset = tabOffset - (scrollerCenter - tabWidth / 2)
        // How much space we have to scroll. If the visible width is <= to the total width, then
        // we have no space to scroll as everything is always visible.
        val availableSpace = (totalTabRowWidth - visibleWidth).coerceAtLeast(0)
        return centeredTabOffset.coerceIn(0, availableSpace)
    }
}

private val ScrollableTabRowMinimumTabWidth = 90.dp

/**
 * [AnimationSpec] used when scrolling to a tab that is not fully visible.
 */
private val ScrollableTabRowScrollSpec: AnimationSpec<Float> = tween(
    durationMillis = 250,
    easing = FastOutSlowInEasing
)
