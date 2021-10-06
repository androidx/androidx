/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.tokens.TopAppBarSmall
import androidx.compose.material3.tokens.TopAppBarSmallCentered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Material Design small top app bar.
 *
 * The top app bar displays information and actions relating to the current screen.
 *
 * This SmallTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A simple top app bar looks like:
 * @sample androidx.compose.material3.samples.SimpleSmallTopAppBar
 * A top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with a scrolling content looks like:
 * @sample androidx.compose.material3.samples.PinnedSmallTopAppBar
 * @sample androidx.compose.material3.samples.EnterAlwaysSmallTopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon The navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors a [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.smallTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun SmallTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = MaterialTheme.typography.fromToken(TopAppBarSmall.SmallHeadlineFont),
        centeredTitle = false,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * Material Design small top app bar with a header title that is horizontally aligned to the center.
 *
 * The top app bar displays information and actions relating to the current screen.
 *
 * This SmallCenteredTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A small centered top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with a scrolling content looks like:
 * @sample androidx.compose.material3.samples.SimpleCenteredTopAppBar
 *
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon The navigation icon displayed at the start of the top app bar. This should
 * typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 * [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param colors a [TopAppBarColors] that will be used to resolve the colors used for this top app
 * bar in different states. See [TopAppBarDefaults.smallCenteredTopAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 * applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 * work in conjunction with a scrolled content to change the top app bar appearance as the content
 * scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Composable
fun SmallCenteredTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.smallCenteredTopAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle =
        MaterialTheme.typography.fromToken(TopAppBarSmall.SmallHeadlineFont),
        centeredTitle = true,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors,
        scrollBehavior = scrollBehavior
    )
}

/**
 * A TopAppBarScrollBehavior defines how an app bar should behave when the content under it is
 * scrolled.
 *
 * @see [TopAppBarDefaults.pinnedScrollBehavior]
 */
@Stable
interface TopAppBarScrollBehavior {

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection

    /**
     * Returns the top app bar's current scroll fraction.
     *
     * A scrollFraction is a value between `0.0` to `1.0` that provides a percentage of the app
     * bar scroll position when the content is scrolled. `0.0` represents an expanded app bar,
     * while `1.0`
     * represents a collapsed one (e.g. the app bar is scrolled to its target offset). Note that
     * this value will be updated on scroll even if the [offset] is pinned to a specific
     * value (see [TopAppBarDefaults.pinnedScrollBehavior]). In this case a value of 1.0 represents
     * that the scroll value has exceeded the height of the pinned app bar, as if the app bar was
     * collapsing.
     */
    val scrollFraction: Float

    /**
     * The top app bar's offset limit in pixels, which represents the offset that a top app bar is
     * allowed to scroll when the scrollable content is scrolled.
     *
     * This limit is represented by a negative [Float], and used to coerce the [offset] value when
     * the scaffold's content is scrolled.
     */
    var offsetLimit: Float

    /**
     * The top app bar's current offset in pixels.
     *
     * The offset is usually between zero and the [offsetLimit].
     */
    var offset: Float

    /**
     * The current content offset that is updated when the nested scroll connection consumes scroll
     * events.
     *
     * A common behavior implementation would update this value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed.y` values.
     */
    var contentOffset: Float
}

/**
 * Represents the colors used by a top app bar in different states.
 *
 * Each app bar has their own default implementation available in [TopAppBarDefaults], such as
 * [TopAppBarDefaults.smallTopAppBarColors] for [SmallTopAppBar].
 */
@Stable
interface TopAppBarColors {
    /**
     * Represents the container color used for the top app bar, depending on whether the app bar is
     * scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun containerColor(scrollFraction: Float): State<Color>

    /**
     * Represents the color used for the top app bar's navigation icon depending on whether the app
     * bar is scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun navigationIconColor(scrollFraction: Float): State<Color>

    /**
     * Represents the color used for the top app bar's title depending on whether the app bar is
     * scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun titleColor(scrollFraction: Float): State<Color>

    /**
     * Represents the color used for the top app bar's action icons depending on whether the app bar
     * is scrolled, and the percentage of its area that is scrolled.
     *
     * @param scrollFraction the scroll percentage of the top app bar (0.0 when the app bar is
     * considered expanded to 1.0 when the app bar is scrolled to its target offset)
     */
    @Composable
    fun actionIconColor(scrollFraction: Float): State<Color>
}

/** Contains default values used for the top app bar implementations. */
object TopAppBarDefaults {

    /**
     * Creates a [TopAppBarColors] for small top app bars. The default implementation animates
     * between the provided colors according to the Material specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconColor the content color used for the navigation icon
     * @param titleColor the content color used for the title
     * @param actionIconsColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun smallTopAppBarColors(
        containerColor: Color =
            MaterialTheme.colorScheme.fromToken(TopAppBarSmall.SmallContainerColor),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmall.SmallOnScrollContainerElevation
        ),
        navigationIconColor: Color =
            MaterialTheme.colorScheme.fromToken(TopAppBarSmall.SmallLeadingIconColor),
        titleColor: Color = MaterialTheme.colorScheme.fromToken(TopAppBarSmall.SmallHeadlineColor),
        actionIconsColor: Color =
            MaterialTheme.colorScheme.fromToken(TopAppBarSmall.SmallTrailingIconColor)
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconColor,
            titleColor,
            actionIconsColor
        ) {
            AnimatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconColor,
                titleColor,
                actionIconsColor
            )
        }
    }

    /**
     * Creates a [TopAppBarColors] for small-centered top app bars. The default implementation
     * animates between the provided colors according to the Material specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconColor the content used color for the navigation icon
     * @param titleColor the content color used for the title
     * @param actionIconsColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun smallCenteredTopAppBarColors(
        containerColor: Color =
            MaterialTheme.colorScheme.fromToken(TopAppBarSmallCentered.SmallCenteredContainerColor),
        scrolledContainerColor: Color = MaterialTheme.colorScheme.applyTonalElevation(
            backgroundColor = containerColor,
            elevation = TopAppBarSmall.SmallOnScrollContainerElevation
        ),
        navigationIconColor: Color =
            MaterialTheme.colorScheme.fromToken(
                TopAppBarSmallCentered.SmallCenteredLeadingIconColor
            ),
        titleColor: Color =
            MaterialTheme.colorScheme.fromToken(
                TopAppBarSmallCentered.SmallCenteredHeadlineColor
            ),
        actionIconsColor: Color =
            MaterialTheme.colorScheme.fromToken(
                TopAppBarSmallCentered.SmallCenteredTrailingIconColor
            )
    ): TopAppBarColors {
        return remember(
            containerColor,
            scrolledContainerColor,
            navigationIconColor,
            titleColor,
            actionIconsColor
        ) {
            AnimatingTopAppBarColors(
                containerColor,
                scrolledContainerColor,
                navigationIconColor,
                titleColor,
                actionIconsColor
            )
        }
    }

    /**
     * Returns a pinned [TopAppBarScrollBehavior] that tracks nested-scroll callbacks and
     * updates its [TopAppBarScrollBehavior.contentOffset] accordingly.
     *
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     * pinned [TopAppBarScrollBehavior]
     */
    fun pinnedScrollBehavior(canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior =
        PinnedScrollBehavior(canScroll)

    /**
     * Returns a [TopAppBarScrollBehavior] that tracks nested-scroll callbacks and
     * updates its [TopAppBarScrollBehavior.offset] and [TopAppBarScrollBehavior.contentOffset]
     * accordingly.
     *
     * This scroll-connection updates the `offset` value immediately whenever the content is pulled
     * up or down. A top-bar that is set up with this [TopAppBarScrollBehavior] managed by this
     * [EnterAlwaysScrollBehavior] will immediately collapse when the scaffold's content is pulled
     * up, and will immediately appear when the content is pulled down.
     *
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     * [EnterAlwaysScrollBehavior]
     */
    fun enterAlwaysScrollBehavior(canScroll: () -> Boolean = { true }): TopAppBarScrollBehavior =
        EnterAlwaysScrollBehavior(canScroll)
}

/**
 * A single-row top app bar that is designed to be called by the Small and Small-Centered top app
 * bar composables.
 *
 * This SingleRowTopAppBar has slots for a title, navigation icon, and actions. When the
 * [centeredTitle] flag is true, the title will be horizontally aligned to the center of the top app
 * bar width.
 */
@Composable
private fun SingleRowTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    centeredTitle: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior?
) {
    // TODO(b/182393826): Check if there is a better place to set the offsetLimit.
    // Set a scroll offset limit to hide the entire app bar area when scrolling.
    val offsetLimit = with(LocalDensity.current) { -TopAppBarSmall.SmallContainerHeight.toPx() }
    SideEffect {
        if (scrollBehavior?.offsetLimit != offsetLimit) {
            scrollBehavior?.offsetLimit = offsetLimit
        }
    }

    // Obtain the container color from the TopAppBarColors.
    // This may potentially animate or interpolate a transition between the container-color and the
    // container's scrolled-color according to the app bar's scroll state.
    val scrollFraction = scrollBehavior?.scrollFraction ?: 0f
    val appBarContainerColor by colors.containerColor(scrollFraction)

    // Wrap the given actions in a Row.
    val actionsRow = @Composable {
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
    // Compose a Surface with a TopAppBarLayout content. The surface's background color will be
    // animated as specified above, and the height of the app bar will be determined by the current
    // scroll-state offset.
    Surface(modifier = modifier, color = appBarContainerColor) {
        val height = LocalDensity.current.run {
            TopAppBarSmall.SmallContainerHeight.toPx() + (scrollBehavior?.offset ?: 0f)
        }
        TopAppBarLayout(
            heightPx = height,
            navigationIconColor = colors.navigationIconColor(scrollFraction).value,
            titleColor = colors.titleColor(scrollFraction).value,
            actionIconsColor = colors.actionIconColor(scrollFraction).value,
            title = title,
            titleTextStyle = titleTextStyle,
            titleHorizontalArrangement =
            if (centeredTitle) Arrangement.Center else Arrangement.Start,
            navigationIcon = navigationIcon,
            actions = actionsRow,
        )
    }
}

/**
 * The base [Layout] for all top app bars. This function lays out a top app bar navigation icon
 * (leading icon), a title (header), and action icons (trailing icons). Note that the navigation and
 * the actions are optional.
 *
 * @param heightPx the total height this layout is capped to
 * @param navigationIconColor the color that will be applied via a [LocalContentColor] when
 * composing the navigation icon
 * @param titleColor the color that will be applied via a [LocalContentColor] when composing the
 * title
 * @param actionIconsColor the color that will be applied via a [LocalContentColor] when composing
 * the action icons
 * @param title the top app bar title (header)
 * @param titleTextStyle the title's text style
 * @param modifier a [Modifier]
 * @param titleAlpha the title's alpha
 * @param titleVerticalArrangement the title's vertical arrangement
 * @param titleHorizontalArrangement the title's horizontal arrangement
 * @param titleBottomPadding the title's bottom padding
 * @param navigationIcon a navigation icon [Composable]
 * @param actions actions [Composable]
 */
@Composable
private fun TopAppBarLayout(
    heightPx: Float,
    navigationIconColor: Color,
    titleColor: Color,
    actionIconsColor: Color,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    modifier: Modifier = Modifier,
    titleAlpha: Float = 1f,
    titleVerticalArrangement: Arrangement.Vertical = Arrangement.Center,
    titleHorizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    titleBottomPadding: Int = 0,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    Layout(
        {
            Box(Modifier.layoutId("navigationIcon").padding(start = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconColor,
                    content = navigationIcon
                )
            }
            Box(Modifier.layoutId("title").padding(horizontal = TopAppBarHorizontalPadding)) {
                ProvideTextStyle(value = titleTextStyle) {
                    CompositionLocalProvider(
                        LocalContentColor provides titleColor.copy(alpha = titleAlpha),
                        content = title
                    )
                }
            }
            Box(Modifier.layoutId("actionIcons").padding(end = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconsColor,
                    content = actions
                )
            }
        },
        modifier = modifier
    ) { measurables, constraints ->
        val navigationIconPlaceable =
            measurables.first { it.layoutId == "navigationIcon" }.measure(constraints)
        val actionIconsPlaceable =
            measurables.first { it.layoutId == "actionIcons" }.measure(constraints)

        val maxTitleWidth =
            constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width
        val titlePlaceable =
            measurables
                .first { it.layoutId == "title" }
                .measure(constraints.copy(maxWidth = maxTitleWidth))
        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        val layoutHeight = heightPx.roundToInt()

        layout(constraints.maxWidth, layoutHeight) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = (layoutHeight - navigationIconPlaceable.height) / 2
            )

            // Title
            titlePlaceable.placeRelative(
                x = when (titleHorizontalArrangement) {
                    Arrangement.Center -> (constraints.maxWidth - titlePlaceable.width) / 2
                    Arrangement.End ->
                        constraints.maxWidth - titlePlaceable.width - actionIconsPlaceable.width
                    // Arrangement.Start.
                    // An TopAppBarTitleInset will make sure the title is offset in case the
                    // navigation icon is missing.
                    else -> max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width)
                },
                y = when (titleVerticalArrangement) {
                    Arrangement.Center -> (layoutHeight - titlePlaceable.height) / 2
                    // Apply bottom padding from the title's baseline only when the Arrangement is
                    // "Bottom".
                    Arrangement.Bottom ->
                        if (titleBottomPadding == 0) layoutHeight - titlePlaceable.height
                        else layoutHeight - titlePlaceable.height - max(
                            0,
                            titleBottomPadding - titlePlaceable.height + titleBaseline
                        )
                    // Arrangement.Top
                    else -> 0
                }
            )

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = (layoutHeight - actionIconsPlaceable.height) / 2
            )
        }
    }
}

/**
 * A [TopAppBarColors] implementation that animates the container color according to the top app
 * bar scroll state.
 *
 * This default implementation does not animate the leading, headline, or trailing colors.
 */
@Stable
private class AnimatingTopAppBarColors(
    private val containerColor: Color,
    private val scrolledContainerColor: Color,
    navigationIconColor: Color,
    titleColor: Color,
    actionIconColor: Color
) : TopAppBarColors {

    // In this TopAppBarColors implementation, the following colors never change their value as the
    // app bar scrolls.
    private val navigationIconColorState: State<Color> = mutableStateOf(navigationIconColor)
    private val titleColorState: State<Color> = mutableStateOf(titleColor)
    private val actionIconColorState: State<Color> = mutableStateOf(actionIconColor)

    @Composable
    override fun containerColor(scrollFraction: Float): State<Color> {
        return animateColorAsState(
            // Check if scrollFraction is slightly over zero to overcome float precision issues.
            targetValue = if (scrollFraction > 0.01f) {
                scrolledContainerColor
            } else {
                containerColor
            },
            animationSpec = tween(
                durationMillis = TopAppBarAnimationDurationMillis,
                easing = LinearOutSlowInEasing
            )
        )
    }

    @Composable
    override fun navigationIconColor(scrollFraction: Float): State<Color> = navigationIconColorState

    @Composable
    override fun titleColor(scrollFraction: Float): State<Color> = titleColorState

    @Composable
    override fun actionIconColor(scrollFraction: Float): State<Color> = actionIconColorState
}

/**
 * A [TopAppBarScrollBehavior] that only adjusts its content offset, without adjusting any
 * properties that affect the height of the top app bar.
 *
 * @param canScroll a callback that can be used to determine whether scroll events are to be
 * handled by this [PinnedScrollBehavior]
 */
private class PinnedScrollBehavior(val canScroll: () -> Boolean = { true }) :
    TopAppBarScrollBehavior {
    override var offsetLimit = -Float.MAX_VALUE
    override val scrollFraction: Float
        get() = if (offsetLimit != 0f) {
            1 - ((offsetLimit - contentOffset).coerceIn(
                minimumValue = offsetLimit,
                maximumValue = 0f
            ) / offsetLimit)
        } else {
            0f
        }
    override var offset = 0f
    override var contentOffset by mutableStateOf(0f)
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                if (consumed.y == 0f && available.y > 0f) {
                    // Reset the total offset to zero when scrolling all the way down. This will
                    // eliminate some float precision inaccuracies.
                    contentOffset = 0f
                } else {
                    contentOffset += consumed.y
                }
                return Offset.Zero
            }
        }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the height of the top app bar.
 *
 * A top-bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when the
 * scaffold's content is pulled up, and will immediately appear when the content is pulled down.
 *
 * @param canScroll a callback that can be used to determine whether scroll events are to be
 * handled by this [EnterAlwaysScrollBehavior]
 */
private class EnterAlwaysScrollBehavior(val canScroll: () -> Boolean = { true }) :
    TopAppBarScrollBehavior {
    override val scrollFraction: Float
        get() = if (offsetLimit != 0f) {
            1 - ((offsetLimit - contentOffset).coerceIn(
                minimumValue = offsetLimit,
                maximumValue = 0f
            ) / offsetLimit)
        } else {
            0f
        }
    override var offsetLimit by mutableStateOf(-Float.MAX_VALUE)
    override var offset by mutableStateOf(0f)
    override var contentOffset by mutableStateOf(0f)
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!canScroll()) return Offset.Zero
                val newOffset = (offset + available.y)
                val coerced = newOffset.coerceIn(minimumValue = offsetLimit, maximumValue = 0f)
                return if (newOffset == coerced) {
                    // Nothing coerced, meaning we're in the middle of top-bar collapse or
                    // expand.
                    offset = coerced
                    available
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                contentOffset += consumed.y
                if (offset == 0f || offset == offsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {
                        // Reset the total offset to zero when scrolling all the way down.
                        // This will eliminate some float precision inaccuracies.
                        contentOffset = 0f
                    }
                }
                offset = (offset + consumed.y).coerceIn(
                    minimumValue = offsetLimit,
                    maximumValue = 0f
                )
                return Offset.Zero
            }
        }
}

private val TopAppBarHorizontalPadding = 4.dp

// A title inset when the App-Bar is a Medium or Large one. Also used to size a spacer when the
// navigation icon is missing.
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding

private const val TopAppBarAnimationDurationMillis = 500
