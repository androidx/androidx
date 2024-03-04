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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.PrimaryNavigationTabTokens
import androidx.compose.material3.tokens.SecondaryNavigationTabTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design Fixed Primary tabs</a>
 *
 * Primary tabs are placed at the top of the content pane under a top app bar. They display the main
 * content destinations. Fixed tabs display all tabs in a set simultaneously. They are best for
 * switching between related content quickly, such as between transportation methods in a map. To
 * navigate between fixed tabs, tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A TabRow places its tabs evenly spaced along the entire row, with each tab taking up an
 * equal amount of space. See [PrimaryScrollableTabRow] for a tab row that does not enforce equal
 * size, and allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.PrimaryTextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material3.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize the
 * indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it should
 * internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material3.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator, as we aren't
 * changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the appearance
 * of the indicator as it animates between tabs, such as changing its color or size. [indicator] is
 * stacked on top of the entire TabRow, so you just need to provide a custom transition that
 * animates the offset of the indicator from the start of the TabRow. For example, take the
 * following example that uses a transition to animate the offset, width, and color of the same
 * FancyIndicator from before, also adding a physics based 'spring' effect to the indicator in the
 * direction of motion:
 *
 * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorContainerTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.PrimaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@ExperimentalMaterial3Api
@Composable
fun PrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.PrimaryIndicator(
            modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
            width = Dp.Unspecified,
        )
    },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    TabRowImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design Fixed Secondary tabs</a>
 *
 * Secondary tabs are used within a content area to further separate related content and establish
 * hierarchy. Fixed tabs display all tabs in a set simultaneously. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A Fixed TabRow places its tabs evenly spaced along the entire row, with each tab taking up
 * an equal amount of space. See [SecondaryScrollableTabRow] for a tab row that does not enforce
 * equal size, and allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.SecondaryTextTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@ExperimentalMaterial3Api
@Composable
fun SecondaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    TabRowImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design tabs</a>
 *
 * Material Design fixed tabs.
 *
 * For primary indicator tabs, use [PrimaryTabRow]. For secondary indicator tabs, use
 * [SecondaryTabRow].
 *
 * Fixed tabs display all tabs in a set simultaneously. They are best for switching between related
 * content quickly, such as between transportation methods in a map. To navigate between fixed tabs,
 * tap an individual tab, or swipe left or right in the content area.
 *
 * A TabRow contains a row of [Tab]s, and displays an indicator underneath the currently selected
 * tab. A TabRow places its tabs evenly spaced along the entire row, with each tab taking up an
 * equal amount of space. See [ScrollableTabRow] for a tab row that does not enforce equal size, and
 * allows scrolling to tabs that do not fit on screen.
 *
 * A simple example with text tabs looks like:
 *
 * @sample androidx.compose.material3.samples.TextTabs
 *
 * You can also provide your own custom tab, such as:
 *
 * @sample androidx.compose.material3.samples.FancyTabs
 *
 * Where the custom tab itself could look like:
 *
 * @sample androidx.compose.material3.samples.FancyTab
 *
 * As well as customizing the tab, you can also provide a custom [indicator], to customize the
 * indicator displayed for a tab. [indicator] will be placed to fill the entire TabRow, so it should
 * internally take care of sizing and positioning the indicator to match changes to
 * [selectedTabIndex].
 *
 * For example, given an indicator that draws a rounded rectangle near the edges of the [Tab]:
 *
 * @sample androidx.compose.material3.samples.FancyIndicator
 *
 * We can reuse [TabRowDefaults.tabIndicatorOffset] and just provide this indicator, as we aren't
 * changing how the size and position of the indicator changes between tabs:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorTabs
 *
 * You may also want to use a custom transition, to allow you to dynamically change the appearance
 * of the indicator as it animates between tabs, such as changing its color or size. [indicator] is
 * stacked on top of the entire TabRow, so you just need to provide a custom transition that
 * animates the offset of the indicator from the start of the TabRow. For example, take the
 * following example that uses a transition to animate the offset, width, and color of the same
 * FancyIndicator from before, also adding a physics based 'spring' effect to the indicator in the
 * direction of motion:
 *
 * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
 *
 * We can now just pass this indicator directly to TabRow:
 *
 * @sample androidx.compose.material3.samples.FancyIndicatorContainerTabs
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit =
        @Composable { tabPositions ->
            if (selectedTabIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
                )
            }
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    TabRowWithSubcomposeImpl(modifier, containerColor, contentColor, indicator, divider, tabs)
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design Scrollable Primary tabs</a>
 *
 * Primary tabs are placed at the top of the content pane under a top app bar. They display the main
 * content destinations. When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable
 * tabs can use longer text labels and a larger number of tabs. They are best used for browsing on
 * touch interfaces.
 *
 * A scrollable tab row contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A scrollable tab row places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [PrimaryTabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param scrollState the [ScrollState] of this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.PrimaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@ExperimentalMaterial3Api
@Composable
fun PrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.PrimaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
                width = Dp.Unspecified,
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    ScrollableTabRowImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        tabs = tabs,
        scrollState = scrollState,
    )
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design Scrollable Secondary tabs</a>
 *
 * Material Design scrollable tabs.
 *
 * Secondary tabs are used within a content area to further separate related content and establish
 * hierarchy. When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable tabs can use
 * longer text labels and a larger number of tabs. They are best used for browsing on touch
 * interfaces.
 *
 * A scrollable tab row contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A scrollable tab row places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [SecondaryTabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param scrollState the [ScrollState] of this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@ExperimentalMaterial3Api
@Composable
fun SecondaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    containerColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit =
        @Composable {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    ScrollableTabRowImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        tabs = tabs,
        scrollState = scrollState
    )
}

/**
 * <a href="https://m3.material.io/components/tabs/overview" class="external"
 * target="_blank">Material Design tabs</a>
 *
 * Material Design scrollable tabs.
 *
 * For primary indicator tabs, use [PrimaryScrollableTabRow]. For secondary indicator tabs, use
 * [SecondaryScrollableTabRow].
 *
 * When a set of tabs cannot fit on screen, use scrollable tabs. Scrollable tabs can use longer text
 * labels and a larger number of tabs. They are best used for browsing on touch interfaces.
 *
 * A ScrollableTabRow contains a row of [Tab]s, and displays an indicator underneath the currently
 * selected tab. A ScrollableTabRow places its tabs offset from the starting edge, and allows
 * scrolling to tabs that are placed off screen. For a fixed tab row that does not allow scrolling,
 * and evenly places its tabs, see [TabRow].
 *
 * @param selectedTabIndex the index of the currently selected tab
 * @param modifier the [Modifier] to be applied to this tab row
 * @param containerColor the color used for the background of this tab row. Use [Color.Transparent]
 *   to have no color.
 * @param contentColor the preferred color for content inside this tab row. Defaults to either the
 *   matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param edgePadding the padding between the starting and ending edge of the scrollable tab row,
 *   and the tabs inside the row. This padding helps inform the user that this tab row can be
 *   scrolled, unlike a [TabRow].
 * @param indicator the indicator that represents which tab is currently selected. By default this
 *   will be a [TabRowDefaults.SecondaryIndicator], using a [TabRowDefaults.tabIndicatorOffset]
 *   modifier to animate its position. Note that this indicator will be forced to fill up the entire
 *   tab row, so you should use [TabRowDefaults.tabIndicatorOffset] or similar to animate the actual
 *   drawn indicator inside this space, and provide an offset from the start.
 * @param divider the divider displayed at the bottom of the tab row. This provides a layer of
 *   separation between the tab row and the content displayed underneath.
 * @param tabs the tabs inside this tab row. Typically this will be multiple [Tab]s. Each element
 *   inside this lambda will be measured and placed evenly across the row, each taking up equal
 *   space.
 */
@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit =
        @Composable { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
            )
        },
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit
) {
    ScrollableTabRowWithSubcomposeImpl(
        selectedTabIndex = selectedTabIndex,
        indicator = indicator,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        divider = divider,
        tabs = tabs,
        scrollState = rememberScrollState()
    )
}

/**
 * Scope for the composable used to render a Tab indicator, this can be used for more complex
 * indicators requiring layout information about the tabs like [TabRowDefaults.PrimaryIndicator] and
 * [TabRowDefaults.SecondaryIndicator]
 */
@ExperimentalMaterial3Api
interface TabIndicatorScope {

    /**
     * A layout modifier that provides tab positions, this can be used to animate and layout a
     * TabIndicator depending on size, position, and content size of each Tab.
     *
     * @sample androidx.compose.material3.samples.FancyAnimatedIndicatorWithModifier
     */
    fun Modifier.tabIndicatorLayout(
        measure:
            MeasureScope.(
                Measurable,
                Constraints,
                List<TabPosition>,
            ) -> MeasureResult
    ): Modifier

    /**
     * A Modifier that follows the default offset and animation
     *
     * @param selectedTabIndex the index of the current selected tab
     * @param matchContentSize this modifier can also animate the width of the indicator \ to match
     *   the content size of the tab.
     */
    fun Modifier.tabIndicatorOffset(
        selectedTabIndex: Int,
        matchContentSize: Boolean = false
    ): Modifier
}

internal interface TabPositionsHolder {

    fun setTabPositions(positions: List<TabPosition>)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabRowImpl(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    indicator: @Composable TabIndicatorScope.() -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor
    ) {
        // TODO Load the motionScheme tokens from the component tokens file
        val tabIndicatorAnimationSpec: FiniteAnimationSpec<Dp> =
            MotionSchemeKeyTokens.DefaultSpatial.value()
        val scope = remember {
            object : TabIndicatorScope, TabPositionsHolder {

                val tabPositions = mutableStateOf<(List<TabPosition>)>(listOf())

                override fun Modifier.tabIndicatorLayout(
                    measure:
                        MeasureScope.(
                            Measurable,
                            Constraints,
                            List<TabPosition>,
                        ) -> MeasureResult
                ): Modifier =
                    this.layout { measurable: Measurable, constraints: Constraints ->
                        measure(
                            measurable,
                            constraints,
                            tabPositions.value,
                        )
                    }

                override fun Modifier.tabIndicatorOffset(
                    selectedTabIndex: Int,
                    matchContentSize: Boolean
                ): Modifier =
                    this.then(
                        TabIndicatorModifier(
                            tabPositions,
                            selectedTabIndex,
                            matchContentSize,
                            tabIndicatorAnimationSpec
                        )
                    )

                override fun setTabPositions(positions: List<TabPosition>) {
                    tabPositions.value = positions
                }
            }
        }

        Layout(
            modifier = Modifier.fillMaxWidth(),
            contents =
                listOf(
                    tabs,
                    divider,
                    { scope.indicator() },
                )
        ) { (tabMeasurables, dividerMeasurables, indicatorMeasurables), constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabCount = tabMeasurables.size
            var tabWidth = 0
            if (tabCount > 0) {
                tabWidth = (tabRowWidth / tabCount)
            }
            val tabRowHeight =
                tabMeasurables.fastFold(initial = 0) { max, curr ->
                    maxOf(curr.maxIntrinsicHeight(tabWidth), max)
                }

            scope.setTabPositions(
                List(tabCount) { index ->
                    var contentWidth =
                        minOf(tabMeasurables[index].maxIntrinsicWidth(tabRowHeight), tabWidth)
                            .toDp()
                    contentWidth -= HorizontalTextPadding * 2
                    // Enforce minimum touch target of 24.dp
                    val indicatorWidth = maxOf(contentWidth, 24.dp)

                    TabPosition(tabWidth.toDp() * index, tabWidth.toDp(), indicatorWidth)
                }
            )

            val tabPlaceables =
                tabMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = tabRowHeight,
                            maxHeight = tabRowHeight,
                        )
                    )
                }

            val dividerPlaceables =
                dividerMeasurables.fastMap { it.measure(constraints.copy(minHeight = 0)) }

            val indicatorPlaceables =
                indicatorMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = 0,
                            maxHeight = tabRowHeight
                        )
                    )
                }

            layout(tabRowWidth, tabRowHeight) {
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }

                dividerPlaceables.fastForEach { placeable ->
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                indicatorPlaceables.fastForEach { it.placeRelative(0, tabRowHeight - it.height) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollableTabRowImpl(
    selectedTabIndex: Int,
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    edgePadding: Dp,
    scrollState: ScrollState,
    indicator: @Composable TabIndicatorScope.() -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentSize(align = Alignment.CenterStart)
                .horizontalScroll(scrollState)
                .selectableGroup()
                .clipToBounds(),
        color = containerColor,
        contentColor = contentColor
    ) {
        val coroutineScope = rememberCoroutineScope()
        // TODO Load the motionScheme tokens from the component tokens file
        val scrollAnimationSpec: FiniteAnimationSpec<Float> =
            MotionSchemeKeyTokens.DefaultSpatial.value()
        val tabIndicatorAnimationSpec: FiniteAnimationSpec<Dp> =
            MotionSchemeKeyTokens.DefaultSpatial.value()
        val scrollableTabData =
            remember(scrollState, coroutineScope) {
                ScrollableTabData(
                    scrollState = scrollState,
                    coroutineScope = coroutineScope,
                    animationSpec = scrollAnimationSpec
                )
            }

        val scope = remember {
            object : TabIndicatorScope, TabPositionsHolder {

                val tabPositions = mutableStateOf<(List<TabPosition>)>(listOf())

                override fun Modifier.tabIndicatorLayout(
                    measure:
                        MeasureScope.(
                            Measurable,
                            Constraints,
                            List<TabPosition>,
                        ) -> MeasureResult
                ): Modifier =
                    this.layout { measurable: Measurable, constraints: Constraints ->
                        measure(
                            measurable,
                            constraints,
                            tabPositions.value,
                        )
                    }

                override fun Modifier.tabIndicatorOffset(
                    selectedTabIndex: Int,
                    matchContentSize: Boolean
                ): Modifier =
                    this.then(
                        TabIndicatorModifier(
                            tabPositions,
                            selectedTabIndex,
                            matchContentSize,
                            tabIndicatorAnimationSpec
                        )
                    )

                override fun setTabPositions(positions: List<TabPosition>) {
                    tabPositions.value = positions
                }
            }
        }

        Layout(
            contents =
                listOf(
                    tabs,
                    divider,
                    { scope.indicator() },
                )
        ) { (tabMeasurables, dividerMeasurables, indicatorMeasurables), constraints ->
            val padding = edgePadding.roundToPx()
            val tabCount = tabMeasurables.size
            val minTabWidth = ScrollableTabRowMinimumTabWidth.roundToPx()
            val layoutHeight =
                tabMeasurables.fastFold(initial = 0) { curr, measurable ->
                    maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
                }
            var layoutWidth = padding * 2
            val tabConstraints =
                constraints.copy(
                    minWidth = minTabWidth,
                    minHeight = layoutHeight,
                    maxHeight = layoutHeight,
                )

            var left = edgePadding
            val tabPlaceables = tabMeasurables.fastMap { it.measure(tabConstraints) }

            val positions =
                List(tabCount) { index ->
                    val tabWidth =
                        maxOf(ScrollableTabRowMinimumTabWidth, tabPlaceables[index].width.toDp())
                    layoutWidth += tabWidth.roundToPx()
                    // Enforce minimum touch target of 24.dp
                    val contentWidth = maxOf(tabWidth - (HorizontalTextPadding * 2), 24.dp)
                    val tabPosition =
                        TabPosition(left = left, width = tabWidth, contentWidth = contentWidth)
                    left += tabWidth
                    tabPosition
                }
            scope.setTabPositions(positions)

            val dividerPlaceables =
                dividerMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minHeight = 0,
                            minWidth = layoutWidth,
                            maxWidth = layoutWidth
                        )
                    )
                }

            val indicatorPlaceables =
                indicatorMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = 0,
                            maxWidth = positions[selectedTabIndex].width.roundToPx(),
                            minHeight = 0,
                            maxHeight = layoutHeight
                        )
                    )
                }

            layout(layoutWidth, layoutHeight) {
                left = edgePadding
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(left.roundToPx(), 0)
                    left += positions[index].width
                }

                dividerPlaceables.fastForEach { placeable ->
                    placeable.placeRelative(0, layoutHeight - placeable.height)
                }

                indicatorPlaceables.fastForEach {
                    val relativeOffset =
                        max(0, (positions[selectedTabIndex].width.roundToPx() - it.width) / 2)
                    it.placeRelative(relativeOffset, layoutHeight - it.height)
                }

                scrollableTabData.onLaidOut(
                    density = this@Layout,
                    edgeOffset = padding,
                    tabPositions = positions,
                    selectedTab = selectedTabIndex
                )
            }
        }
    }
}

internal data class TabIndicatorModifier(
    val tabPositionsState: State<List<TabPosition>>,
    val selectedTabIndex: Int,
    val followContentSize: Boolean,
    val animationSpec: FiniteAnimationSpec<Dp>
) : ModifierNodeElement<TabIndicatorOffsetNode>() {

    override fun create(): TabIndicatorOffsetNode {
        return TabIndicatorOffsetNode(
            tabPositionsState = tabPositionsState,
            selectedTabIndex = selectedTabIndex,
            followContentSize = followContentSize,
            animationSpec = animationSpec
        )
    }

    override fun update(node: TabIndicatorOffsetNode) {
        node.tabPositionsState = tabPositionsState
        node.selectedTabIndex = selectedTabIndex
        node.followContentSize = followContentSize
        node.animationSpec = animationSpec
    }

    override fun InspectorInfo.inspectableProperties() {
        // Show nothing in the inspector.
    }
}

internal class TabIndicatorOffsetNode(
    var tabPositionsState: State<List<TabPosition>>,
    var selectedTabIndex: Int,
    var followContentSize: Boolean,
    var animationSpec: FiniteAnimationSpec<Dp>
) : Modifier.Node(), LayoutModifierNode {

    private var offsetAnimatable: Animatable<Dp, AnimationVector1D>? = null
    private var widthAnimatable: Animatable<Dp, AnimationVector1D>? = null
    private var initialOffset: Dp? = null
    private var initialWidth: Dp? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        if (tabPositionsState.value.isEmpty()) {
            return layout(0, 0) {}
        }

        val currentTabWidth =
            if (followContentSize) {
                tabPositionsState.value[selectedTabIndex].contentWidth
            } else {
                tabPositionsState.value[selectedTabIndex].width
            }

        if (initialWidth != null) {
            val widthAnim =
                widthAnimatable
                    ?: Animatable(initialWidth!!, Dp.VectorConverter).also { widthAnimatable = it }

            if (currentTabWidth != widthAnim.targetValue) {
                coroutineScope.launch { widthAnim.animateTo(currentTabWidth, animationSpec) }
            }
        } else {
            initialWidth = currentTabWidth
        }

        val indicatorOffset = tabPositionsState.value[selectedTabIndex].left

        if (initialOffset != null) {
            val offsetAnim =
                offsetAnimatable
                    ?: Animatable(initialOffset!!, Dp.VectorConverter).also {
                        offsetAnimatable = it
                    }

            if (indicatorOffset != offsetAnim.targetValue) {
                coroutineScope.launch { offsetAnim.animateTo(indicatorOffset, animationSpec) }
            }
        } else {
            initialOffset = indicatorOffset
        }

        val offset = offsetAnimatable?.value ?: indicatorOffset

        val width = widthAnimatable?.value ?: currentTabWidth

        val placeable =
            measurable.measure(
                constraints.copy(minWidth = width.roundToPx(), maxWidth = width.roundToPx())
            )

        return layout(placeable.width, placeable.height) { placeable.place(offset.roundToPx(), 0) }
    }
}

@Composable
private fun TabRowWithSubcomposeImpl(
    modifier: Modifier,
    containerColor: Color,
    contentColor: Color,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit,
    divider: @Composable () -> Unit,
    tabs: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.selectableGroup(),
        color = containerColor,
        contentColor = contentColor
    ) {
        SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
            val tabRowWidth = constraints.maxWidth
            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)
            val tabCount = tabMeasurables.size
            var tabWidth = 0
            if (tabCount > 0) {
                tabWidth = (tabRowWidth / tabCount)
            }
            val tabRowHeight =
                tabMeasurables.fastFold(initial = 0) { max, curr ->
                    maxOf(curr.maxIntrinsicHeight(tabWidth), max)
                }

            val tabPlaceables =
                tabMeasurables.fastMap {
                    it.measure(
                        constraints.copy(
                            minWidth = tabWidth,
                            maxWidth = tabWidth,
                            minHeight = tabRowHeight,
                            maxHeight = tabRowHeight,
                        )
                    )
                }

            val tabPositions =
                List(tabCount) { index ->
                    var contentWidth =
                        minOf(tabMeasurables[index].maxIntrinsicWidth(tabRowHeight), tabWidth)
                            .toDp()
                    contentWidth -= HorizontalTextPadding * 2
                    // Enforce minimum touch target of 24.dp
                    val indicatorWidth = maxOf(contentWidth, 24.dp)
                    TabPosition(tabWidth.toDp() * index, tabWidth.toDp(), indicatorWidth)
                }

            layout(tabRowWidth, tabRowHeight) {
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(index * tabWidth, 0)
                }

                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable = it.measure(constraints.copy(minHeight = 0))
                    placeable.placeRelative(0, tabRowHeight - placeable.height)
                }

                subcompose(TabSlots.Indicator) { indicator(tabPositions) }
                    .fastForEach {
                        it.measure(Constraints.fixed(tabRowWidth, tabRowHeight)).placeRelative(0, 0)
                    }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollableTabRowWithSubcomposeImpl(
    selectedTabIndex: Int,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    divider: @Composable () -> Unit = @Composable { HorizontalDivider() },
    tabs: @Composable () -> Unit,
    scrollState: ScrollState,
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        val coroutineScope = rememberCoroutineScope()
        // TODO Load the motionScheme tokens from the component tokens file
        val scrollAnimationSpec: FiniteAnimationSpec<Float> =
            MotionSchemeKeyTokens.DefaultSpatial.value()
        val scrollableTabData =
            remember(scrollState, coroutineScope) {
                ScrollableTabData(
                    scrollState = scrollState,
                    coroutineScope = coroutineScope,
                    animationSpec = scrollAnimationSpec
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

            val tabMeasurables = subcompose(TabSlots.Tabs, tabs)

            val layoutHeight =
                tabMeasurables.fastFold(initial = 0) { curr, measurable ->
                    maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
                }

            val tabConstraints =
                constraints.copy(
                    minWidth = minTabWidth,
                    minHeight = layoutHeight,
                    maxHeight = layoutHeight,
                )

            val tabPlaceables = mutableListOf<Placeable>()
            val tabContentWidths = mutableListOf<Dp>()
            tabMeasurables.fastForEach {
                val placeable = it.measure(tabConstraints)
                var contentWidth =
                    minOf(it.maxIntrinsicWidth(placeable.height), placeable.width).toDp()
                contentWidth -= HorizontalTextPadding * 2
                tabPlaceables.add(placeable)
                tabContentWidths.add(contentWidth)
            }

            val layoutWidth =
                tabPlaceables.fastFold(initial = padding * 2) { curr, measurable ->
                    curr + measurable.width
                }

            // Position the children.
            layout(layoutWidth, layoutHeight) {
                // Place the tabs
                val tabPositions = mutableListOf<TabPosition>()
                var left = padding
                tabPlaceables.fastForEachIndexed { index, placeable ->
                    placeable.placeRelative(left, 0)
                    tabPositions.add(
                        TabPosition(
                            left = left.toDp(),
                            width = placeable.width.toDp(),
                            contentWidth = tabContentWidths[index]
                        )
                    )
                    left += placeable.width
                }

                // The divider is measured with its own height, and width equal to the total width
                // of the tab row, and then placed on top of the tabs.
                subcompose(TabSlots.Divider, divider).fastForEach {
                    val placeable =
                        it.measure(
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
                subcompose(TabSlots.Indicator) { indicator(tabPositions) }
                    .fastForEach {
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
 * Data class that contains information about a tab's position on screen, used for calculating where
 * to place the indicator that shows which tab is selected.
 *
 * @property left the left edge's x position from the start of the [TabRow]
 * @property right the right edge's x position from the start of the [TabRow]
 * @property width the width of this tab
 * @property contentWidth the content width of this tab. Should be a minimum of 24.dp
 */
@Immutable
class TabPosition internal constructor(val left: Dp, val width: Dp, val contentWidth: Dp) {

    val right: Dp
        get() = left + width

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabPosition) return false

        if (left != other.left) return false
        if (width != other.width) return false
        if (contentWidth != other.contentWidth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + contentWidth.hashCode()
        return result
    }

    override fun toString(): String {
        return "TabPosition(left=$left, right=$right, width=$width, contentWidth=$contentWidth)"
    }
}

/** Contains default implementations and values used for TabRow. */
object TabRowDefaults {
    /** The default padding from the starting edge before a tab in a [ScrollableTabRow]. */
    val ScrollableTabRowEdgeStartPadding = 52.dp

    /** Default container color of a tab row. */
    @Deprecated(
        message = "Use TabRowDefaults.primaryContainerColor instead",
        replaceWith = ReplaceWith("primaryContainerColor")
    )
    val containerColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ContainerColor.value

    /** Default container color of a [PrimaryTabRow]. */
    val primaryContainerColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ContainerColor.value

    /** Default container color of a [SecondaryTabRow]. */
    val secondaryContainerColor: Color
        @Composable get() = SecondaryNavigationTabTokens.ContainerColor.value

    /** Default content color of a tab row. */
    @Deprecated(
        message = "Use TabRowDefaults.primaryContentColor instead",
        replaceWith = ReplaceWith("primaryContentColor")
    )
    val contentColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ActiveLabelTextColor.value

    /** Default content color of a [PrimaryTabRow]. */
    val primaryContentColor: Color
        @Composable get() = PrimaryNavigationTabTokens.ActiveLabelTextColor.value

    /** Default content color of a [SecondaryTabRow]. */
    val secondaryContentColor: Color
        @Composable get() = SecondaryNavigationTabTokens.ActiveLabelTextColor.value

    /**
     * Default indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    @Deprecated(
        message = "Use SecondaryIndicator instead.",
        replaceWith = ReplaceWith("SecondaryIndicator(modifier, height, color)")
    )
    fun Indicator(
        modifier: Modifier = Modifier,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color =
            MaterialTheme.colorScheme.fromToken(PrimaryNavigationTabTokens.ActiveIndicatorColor)
    ) {
        Box(modifier.fillMaxWidth().height(height).background(color = color))
    }

    /**
     * Primary indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param width width of the indicator
     * @param height height of the indicator
     * @param color color of the indicator
     * @param shape shape of the indicator
     */
    @Composable
    fun PrimaryIndicator(
        modifier: Modifier = Modifier,
        width: Dp = 24.dp,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color = PrimaryNavigationTabTokens.ActiveIndicatorColor.value,
        shape: Shape = PrimaryNavigationTabTokens.ActiveIndicatorShape
    ) {
        Spacer(
            modifier
                .requiredHeight(height)
                .requiredWidth(width)
                .background(color = color, shape = shape)
        )
    }

    /**
     * Secondary indicator, which will be positioned at the bottom of the [TabRow], on top of the
     * divider.
     *
     * @param modifier modifier for the indicator's layout
     * @param height height of the indicator
     * @param color color of the indicator
     */
    @Composable
    fun SecondaryIndicator(
        modifier: Modifier = Modifier,
        height: Dp = PrimaryNavigationTabTokens.ActiveIndicatorHeight,
        color: Color = PrimaryNavigationTabTokens.ActiveIndicatorColor.value
    ) {
        Box(modifier.fillMaxWidth().height(height).background(color = color))
    }

    /**
     * [Modifier] that takes up all the available width inside the [TabRow], and then animates the
     * offset of the indicator it is applied to, depending on the [currentTabPosition].
     *
     * @param currentTabPosition [TabPosition] of the currently selected tab. This is used to
     *   calculate the offset of the indicator this modifier is applied to, as well as its width.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun Modifier.tabIndicatorOffset(currentTabPosition: TabPosition): Modifier =
        composed(
            inspectorInfo =
                debugInspectorInfo {
                    name = "tabIndicatorOffset"
                    value = currentTabPosition
                }
        ) {
            // TODO Load the motionScheme tokens from the component tokens file
            val currentTabWidth by
                animateDpAsState(
                    targetValue = currentTabPosition.width,
                    animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value()
                )
            val indicatorOffset by
                animateDpAsState(
                    targetValue = currentTabPosition.left,
                    animationSpec = MotionSchemeKeyTokens.DefaultSpatial.value()
                )
            fillMaxWidth()
                .wrapContentSize(Alignment.BottomStart)
                .offset { IntOffset(x = indicatorOffset.roundToPx(), y = 0) }
                .width(currentTabWidth)
        }
}

private enum class TabSlots {
    Tabs,
    Divider,
    Indicator
}

/** Class holding onto state needed for [ScrollableTabRow] */
private class ScrollableTabData(
    private val scrollState: ScrollState,
    private val coroutineScope: CoroutineScope,
    private val animationSpec: FiniteAnimationSpec<Float>
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
                        scrollState.animateScrollTo(calculatedOffset, animationSpec = animationSpec)
                    }
                }
            }
        }
    }

    /**
     * @return the offset required to horizontally center the tab inside this TabRow. If the tab is
     *   at the start / end, and there is not enough space to fully centre the tab, this will just
     *   clamp to the min / max position given the max width.
     */
    private fun TabPosition.calculateTabOffset(
        density: Density,
        edgeOffset: Int,
        tabPositions: List<TabPosition>
    ): Int =
        with(density) {
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
