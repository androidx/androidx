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
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomAppBarState.Companion.Saver
import androidx.compose.material3.TopAppBarState.Companion.Saver
import androidx.compose.material3.internal.FloatProducer
import androidx.compose.material3.internal.ProvideContentColorTextStyle
import androidx.compose.material3.internal.rememberAccessibilityServiceState
import androidx.compose.material3.internal.systemBarsForVisualComponents
import androidx.compose.material3.tokens.AppBarLargeFlexibleTokens
import androidx.compose.material3.tokens.AppBarLargeTokens
import androidx.compose.material3.tokens.AppBarMediumFlexibleTokens
import androidx.compose.material3.tokens.AppBarMediumTokens
import androidx.compose.material3.tokens.AppBarSmallTokens
import androidx.compose.material3.tokens.AppBarTokens
import androidx.compose.material3.tokens.BottomAppBarTokens
import androidx.compose.material3.tokens.DockedToolbarTokens
import androidx.compose.material3.tokens.FabSecondaryContainerTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastSumBy
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * [Material Design small top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small TopAppBar has slots for a title, navigation icon, and actions.
 *
 * ![Small top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-top-app-bar.png)
 *
 * A simple top app bar looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBar A top app bar that uses a
 *
 * A simple sample with an [AppBarRow], that varies the number of actions shown.
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBarWithAdaptiveActions
 *
 *   [scrollBehavior] to customize its nested scrolling behavior when working in conjunction with a
 *   scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.PinnedTopAppBar
 * @sample androidx.compose.material3.samples.EnterAlwaysTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message = "Deprecated in favor of TopAppBar with expandedHeight parameter",
    level = DeprecationLevel.HIDDEN,
)
@ExperimentalMaterial3Api
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design small top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small TopAppBar has slots for a title, navigation icon, and actions.
 *
 * ![Small top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-top-app-bar.png)
 *
 * A simple top app bar looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBar A top app bar that uses a
 *   [scrollBehavior] to customize its nested scrolling behavior when working in conjunction with a
 *   scrolling content looks like:
 * @sample androidx.compose.material3.samples.PinnedTopAppBar
 * @sample androidx.compose.material3.samples.EnterAlwaysTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message = "Deprecated in favor of TopAppBar with contentPadding parameter",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
        contentPadding = TopAppBarDefaults.ContentPadding,
    )

/**
 * [Material Design small top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small TopAppBar has slots for a title, navigation icon, and actions.
 *
 * ![Small top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-top-app-bar.png)
 *
 * A simple top app bar looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBar A top app bar that uses a
 *   [scrollBehavior] to customize its nested scrolling behavior when working in conjunction with a
 *   scrolling content looks like:
 * @sample androidx.compose.material3.samples.PinnedTopAppBar
 * @sample androidx.compose.material3.samples.EnterAlwaysTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @param contentPadding the padding applied to the content of this TopAppBar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
) =
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = AppBarSmallTokens.TitleFont.value,
        subtitle = null,
        subtitleTextStyle = TextStyle.Default,
        titleHorizontalAlignment = Alignment.Start,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.TopAppBarExpandedHeight
            } else {
                expandedHeight
            },
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design center-aligned small top app
 * bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small top app bar has a header title that is horizontally aligned to the center.
 *
 * ![Center-aligned top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/center-aligned-top-app-bar.png)
 *
 * This CenterAlignedTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A center aligned top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with a scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message = "Deprecated in favor of CenterAlignedTopAppBar with expandedHeight parameter",
    level = DeprecationLevel.HIDDEN,
)
@ExperimentalMaterial3Api
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = TopAppBarDefaults.TopAppBarExpandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design center-aligned small top app
 * bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small top app bar has a header title that is horizontally aligned to the center.
 *
 * ![Center-aligned top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/center-aligned-top-app-bar.png)
 *
 * This CenterAlignedTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A center aligned top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with a scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message = "Deprecated in favor of CenterAlignedTopAppBar with contentPadding parameter",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = expandedHeight,
        contentPadding = TopAppBarDefaults.ContentPadding,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design center-aligned small top app
 * bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small top app bar has a header title that is horizontally aligned to the center.
 *
 * ![Center-aligned top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/center-aligned-top-app-bar.png)
 *
 * This CenterAlignedTopAppBar has slots for a title, navigation icon, and actions.
 *
 * A center aligned top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with a scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleCenterAlignedTopAppBar
 * @param title the title to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @param contentPadding the padding applied to the content of this TopAppBar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
) =
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = AppBarSmallTokens.TitleFont.value,
        subtitle = null,
        subtitleTextStyle = TextStyle.Default,
        titleHorizontalAlignment = Alignment.CenterHorizontally,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.TopAppBarExpandedHeight
            } else {
                expandedHeight
            },
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design small top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * This small TopAppBar has slots for a title, subtitle, navigation icon, and actions.
 *
 * ![Small top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small-top-app-bar.png)
 *
 * A top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with a scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.SimpleTopAppBarWithSubtitle
 * @sample androidx.compose.material3.samples.SimpleCenterAlignedTopAppBarWithSubtitle
 * @param title the title to be displayed in the top app bar
 * @param subtitle the subtitle to be displayed in the top app bar
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param titleHorizontalAlignment the horizontal alignment of the title and subtitle
 * @param expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app bar
 *   to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @param contentPadding the padding applied to the content of this TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
    contentPadding: PaddingValues = TopAppBarDefaults.ContentPadding,
) =
    SingleRowTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = AppBarSmallTokens.TitleFont.value,
        subtitle = subtitle,
        subtitleTextStyle = AppBarSmallTokens.SubtitleFont.value,
        titleHorizontalAlignment = titleHorizontalAlignment,
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.TopAppBarExpandedHeight
            } else {
                expandedHeight
            },
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design medium top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Medium top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/medium-top-app-bar.png)
 *
 * This MediumTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A medium top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message =
        "Deprecated in favor of MediumTopAppBar with collapsedHeight and expandedHeight " +
            "parameters",
    level = DeprecationLevel.HIDDEN,
)
@ExperimentalMaterial3Api
@Composable
fun MediumTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    MediumTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight,
        expandedHeight = TopAppBarDefaults.MediumAppBarExpandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design medium top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Medium top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/medium-top-app-bar.png)
 *
 * This MediumTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A medium top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedMediumTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param collapsedHeight this app bar height when collapsed by a provided [scrollBehavior]. This
 *   value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.MediumAppBarCollapsedHeight].
 * @param expandedHeight this app bar's maximum height. When a specified [scrollBehavior] causes the
 *   app bar to collapse or expand, this value will represent the maximum height that the app-bar
 *   will be allowed to expand. The expanded height is expected to be greater or equal to the
 *   [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise. Also,
 *   this value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.MediumAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @throws IllegalArgumentException if the provided [expandedHeight] is smaller than the
 *   [collapsedHeight]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediumTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    collapsedHeight: Dp = TopAppBarDefaults.MediumAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults.MediumAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TwoRowsTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = AppBarMediumTokens.TitleFont.value,
        smallTitleTextStyle = AppBarSmallTokens.TitleFont.value,
        titleBottomPadding = MediumTitleBottomPadding,
        smallTitle = title,
        subtitle = null,
        subtitleTextStyle = TextStyle.Default,
        smallSubtitle = null,
        smallSubtitleTextStyle = TextStyle.Default,
        titleHorizontalAlignment = Alignment.Start,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight =
            if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
                TopAppBarDefaults.MediumAppBarCollapsedHeight
            } else {
                collapsedHeight
            },
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.MediumAppBarExpandedHeight
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design medium flexible top app
 * bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Medium top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/medium-top-app-bar.png)
 *
 * This `MediumFlexibleTopAppBar` has slots for a title, subtitle, navigation icon, and actions. In
 * its default expanded state, the title and subtitle are displayed in a second row under the
 * navigation and actions.
 *
 * A medium flexible top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedMediumFlexibleTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param subtitle optional subtitle to be displayed in the top app bar. This subtitle will be used
 *   in the app bar's expanded and collapsed states
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param titleHorizontalAlignment the horizontal alignment of the title and subtitle
 * @param collapsedHeight this app bar height when collapsed by a provided [scrollBehavior]. This
 *   value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.MediumAppBarCollapsedHeight].
 * @param expandedHeight this app bar's maximum height. When a specified [scrollBehavior] causes the
 *   app bar to collapse or expand, this value will represent the maximum height that the app-bar
 *   will be allowed to expand. The expanded height is expected to be greater or equal to the
 *   [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise. Also,
 *   this value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight] or
 *   [TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @throws IllegalArgumentException if the provided [expandedHeight] is smaller than the
 *   [collapsedHeight]
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun MediumFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.MediumAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) {
            TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
        } else {
            TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight
        },
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TwoRowsTopAppBar(
        modifier = modifier,
        title = title,
        titleTextStyle = AppBarMediumFlexibleTokens.TitleFont.value,
        smallTitleTextStyle = AppBarSmallTokens.TitleFont.value,
        titleBottomPadding = MediumTitleBottomPadding,
        smallTitle = title,
        subtitle = subtitle ?: {},
        subtitleTextStyle = AppBarMediumFlexibleTokens.SubtitleFont.value,
        smallSubtitle = subtitle ?: {},
        smallSubtitleTextStyle = AppBarSmallTokens.SubtitleFont.value,
        titleHorizontalAlignment = titleHorizontalAlignment,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight =
            if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
                TopAppBarDefaults.MediumAppBarCollapsedHeight
            } else {
                collapsedHeight
            },
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                if (subtitle != null) {
                    TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
                } else {
                    TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight
                }
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design large top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Large top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/large-top-app-bar.png)
 *
 * This LargeTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A large top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@Deprecated(
    message =
        "Deprecated in favor of LargeTopAppBar with collapsedHeight and expandedHeight " +
            "parameters",
    level = DeprecationLevel.HIDDEN,
)
@ExperimentalMaterial3Api
@Composable
fun LargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    LargeTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight = TopAppBarDefaults.LargeAppBarCollapsedHeight,
        expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight,
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design large top app bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Large top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/large-top-app-bar.png)
 *
 * This LargeTopAppBar has slots for a title, navigation icon, and actions. In its default expanded
 * state, the title is displayed in a second row under the navigation and actions.
 *
 * A large top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedLargeTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param collapsedHeight this app bar height when collapsed by a provided [scrollBehavior]. This
 *   value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.LargeAppBarCollapsedHeight].
 * @param expandedHeight this app bar's maximum height. When a specified [scrollBehavior] causes the
 *   app bar to collapse or expand, this value will represent the maximum height that the app-bar
 *   will be allowed to expand. The expanded height is expected to be greater or equal to the
 *   [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise. Also,
 *   this value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.LargeAppBarExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @throws IllegalArgumentException if the provided [expandedHeight] is smaller to the
 *   [collapsedHeight]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp = TopAppBarDefaults.LargeAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TwoRowsTopAppBar(
        title = title,
        titleTextStyle = AppBarLargeTokens.TitleFont.value,
        smallTitleTextStyle = AppBarSmallTokens.TitleFont.value,
        titleBottomPadding = LargeTitleBottomPadding,
        smallTitle = title,
        modifier = modifier,
        subtitle = null,
        subtitleTextStyle = TextStyle.Default,
        smallSubtitle = null,
        smallSubtitleTextStyle = TextStyle.Default,
        titleHorizontalAlignment = Alignment.Start,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight =
            if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
                TopAppBarDefaults.LargeAppBarCollapsedHeight
            } else {
                collapsedHeight
            },
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                TopAppBarDefaults.LargeAppBarExpandedHeight
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * [Material Design large flexible top app
 * bar](https://m3.material.io/components/top-app-bar/overview)
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Large top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/large-top-app-bar.png)
 *
 * This `LargeFlexibleTopAppBar` has slots for a title, subtitle, navigation icon, and actions. In
 * its default expanded state, the title and subtitle are displayed in a second row under the
 * navigation and actions.
 *
 * A large flexible top app bar that uses a [scrollBehavior] to customize its nested scrolling
 * behavior when working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitUntilCollapsedCenterAlignedLargeFlexibleTopAppBar
 * @param title the title to be displayed in the top app bar. This title will be used in the app
 *   bar's expanded and collapsed states, although in its collapsed state it will be composed with a
 *   smaller sized [TextStyle]
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param subtitle an optional subtitle to be displayed in the top app bar. This subtitle will be
 *   used in the app bar's expanded and collapsed states.
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param titleHorizontalAlignment the horizontal alignment of the title and subtitle
 * @param collapsedHeight this app bar height when collapsed by a provided [scrollBehavior]. This
 *   value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.LargeAppBarCollapsedHeight].
 * @param expandedHeight this app bar's maximum height. When a specified [scrollBehavior] causes the
 *   app bar to collapse or expand, this value will represent the maximum height that the app-bar
 *   will be allowed to expand. The expanded height is expected to be greater or equal to the
 *   [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise. Also,
 *   this value must be specified and finite, otherwise it will be ignored and replaced with
 *   [TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight] or
 *   [TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight].
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @throws IllegalArgumentException if the provided [expandedHeight] is smaller to the
 *   [collapsedHeight]
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun LargeFlexibleTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.LargeAppBarCollapsedHeight,
    expandedHeight: Dp =
        if (subtitle != null) {
            TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
        } else {
            TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
        },
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) =
    TwoRowsTopAppBar(
        title = title,
        titleTextStyle = AppBarLargeFlexibleTokens.TitleFont.value,
        smallTitleTextStyle = AppBarSmallTokens.TitleFont.value,
        titleBottomPadding = LargeTitleBottomPadding,
        smallTitle = title,
        modifier = modifier,
        subtitle = subtitle ?: {},
        subtitleTextStyle = AppBarLargeFlexibleTokens.SubtitleFont.value,
        smallSubtitle = subtitle ?: {},
        smallSubtitleTextStyle = AppBarSmallTokens.SubtitleFont.value,
        titleHorizontalAlignment = titleHorizontalAlignment,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight =
            if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
                TopAppBarDefaults.LargeAppBarCollapsedHeight
            } else {
                collapsedHeight
            },
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                if (subtitle != null) {
                    TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
                } else {
                    TopAppBarDefaults.LargeFlexibleAppBarWithoutSubtitleExpandedHeight
                }
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )

/**
 * A basic two-rows Material Design top app bar.
 *
 * Top app bars display information and actions at the top of a screen.
 *
 * ![Two rows top app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/medium-top-app-bar.png)
 *
 * This two-rows top app bar has slots for titles and subtitles, navigation icon, and actions. In
 * its default expanded state, the expanded title and subtitle are displayed in a second row under
 * the navigation and actions.
 *
 * By default, the two-rows top app bar will apply the [MediumFlexibleTopAppBar] text styles to the
 * expanded and collapsed titles. You may override that by applying your own style to the
 * Composition passed into those collapsed and expanded title slots.
 *
 * A two-rows top app bar that uses a [scrollBehavior] to customize its nested scrolling behavior
 * when working in conjunction with scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.CustomTwoRowsTopAppBar
 * @param title a lambda for providing a title to be displayed in the top app bar in collapsed and
 *   expanded states. By default a small-app-bar [TextStyle] is applied to the Composition, and you
 *   may override it by wrapping your provided component with a composition local. Note that unlike
 *   the large or medium top app bars, the `TwoRowsTopAppBar` does not append bottom padding to the
 *   expanded title Composable by default. Padding should be applied directly to the provided
 *   expanded title, or to the [subtitle] that appears below it.
 * @param modifier the [Modifier] to be applied to this top app bar
 * @param subtitle a lambda for providing an optional subtitle to be displayed in the top app bar in
 *   collapsed and expanded states.
 * @param navigationIcon the navigation icon displayed at the start of the top app bar. This should
 *   typically be an [IconButton] or [IconToggleButton].
 * @param actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @param titleHorizontalAlignment the horizontal alignment of the title and subtitle
 * @param collapsedHeight the app bar's height in its collapsed state. Note that this value might be
 *   adjusted to support displaying larger fonts. In case the provided value is [Dp.Unspecified] or
 *   [Dp.Infinity], the height will default to [TopAppBarDefaults.MediumAppBarCollapsedHeight].
 * @param expandedHeight this app bar's height in its expanded state. When a specified
 *   [scrollBehavior] causes the app bar to collapse or expand, this value will represent the total
 *   height that the app-bar will expand to. The expanded height is expected to be greater or equal
 *   to the [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise.
 *   Note that this value might be adjusted to support displaying larger fonts. In case the provided
 *   value is [Dp.Unspecified] or [Dp.Infinity], the height will default to
 *   [TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight] when an [expandedSubtitle]
 *   is provided, or to [TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight] when
 *   it's not.
 * @param windowInsets a window insets that app bar will respect.
 * @param colors [TopAppBarColors] that will be used to resolve the colors used for this top app bar
 *   in different states. See [TopAppBarDefaults.topAppBarColors].
 * @param scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will be
 *   applied by this top app bar to set up its height and colors. A scroll behavior is designed to
 *   work in conjunction with a scrolled content to change the top app bar appearance as the content
 *   scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 * @throws IllegalArgumentException if the provided [expandedHeight] is smaller to the
 *   [collapsedHeight]
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun TwoRowsTopAppBar(
    title: @Composable (expanded: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable (expanded: Boolean) -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = Dp.Unspecified,
    expandedHeight: Dp = Dp.Unspecified,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TwoRowsTopAppBar(
        title = { title(true) },
        titleTextStyle = AppBarMediumFlexibleTokens.TitleFont.value,
        smallTitleTextStyle = AppBarSmallTokens.TitleFont.value,
        titleBottomPadding = 0.dp,
        smallTitle = { title(false) },
        modifier = modifier,
        subtitle = { subtitle?.invoke(true) },
        subtitleTextStyle = AppBarMediumFlexibleTokens.SubtitleFont.value,
        smallSubtitle = { subtitle?.invoke(false) },
        smallSubtitleTextStyle = AppBarSmallTokens.SubtitleFont.value,
        titleHorizontalAlignment = titleHorizontalAlignment,
        navigationIcon = navigationIcon,
        actions = actions,
        collapsedHeight =
            if (collapsedHeight == Dp.Unspecified || collapsedHeight == Dp.Infinity) {
                // Default to the medium app-bar collapsed height
                TopAppBarDefaults.MediumAppBarCollapsedHeight
            } else {
                collapsedHeight
            },
        expandedHeight =
            if (expandedHeight == Dp.Unspecified || expandedHeight == Dp.Infinity) {
                // Default to the medium app-bar expanded height
                if (subtitle != null) {
                    TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight
                } else {
                    TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight
                }
            } else {
                expandedHeight
            },
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}

/**
 * [Material Design bottom app bar](https://m3.material.io/components/bottom-app-bar/overview)
 *
 * A bottom app bar displays navigation and key actions at the bottom of small screens.
 *
 * ![Bottom app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * @sample androidx.compose.material3.samples.SimpleBottomAppBar
 *
 * It can optionally display a [FloatingActionButton] embedded at the end of the BottomAppBar.
 *
 * @sample androidx.compose.material3.samples.BottomAppBarWithFAB
 *
 * Also see [NavigationBar].
 *
 * @param actions the icon content of this BottomAppBar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param floatingActionButton optional floating action button at the end of this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param windowInsets a window insets that app bar will respect.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBar(
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable (() -> Unit)? = null,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
) =
    BottomAppBar(
        actions = actions,
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        scrollBehavior = null,
    )

/**
 * [Material Design bottom app bar](https://m3.material.io/components/bottom-app-bar/overview)
 *
 * A bottom app bar displays navigation and key actions at the bottom of small screens.
 *
 * ![Bottom app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * @sample androidx.compose.material3.samples.SimpleBottomAppBar
 *
 * It can optionally display a [FloatingActionButton] embedded at the end of the BottomAppBar.
 *
 * @sample androidx.compose.material3.samples.BottomAppBarWithFAB
 *
 * A bottom app bar that uses a [scrollBehavior] to customize its nested scrolling behavior when
 * working in conjunction with a scrolling content looks like:
 *
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBar
 *
 * Also see [NavigationBar].
 *
 * @param actions the icon content of this BottomAppBar. The default layout here is a [Row], so
 *   content inside will be placed horizontally.
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param floatingActionButton optional floating action button at the end of this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param windowInsets a window insets that app bar will respect.
 * @param scrollBehavior a [BottomAppBarScrollBehavior] which holds various offset values that will
 *   be applied by this bottom app bar to set up its height. A scroll behavior is designed to work
 *   in conjunction with a scrolled content to change the bottom app bar appearance as the content
 *   scrolls. Note that the bottom app bar will not react to scrolling in case a touch exploration
 *   service (e.g., TalkBack) is active. See [BottomAppBarScrollBehavior.nestedScrollConnection].
 */
@ExperimentalMaterial3Api
@Composable
fun BottomAppBar(
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable (() -> Unit)? = null,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    scrollBehavior: BottomAppBarScrollBehavior? = null,
) =
    BottomAppBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        windowInsets = windowInsets,
        contentPadding = contentPadding,
        scrollBehavior = scrollBehavior,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
        if (floatingActionButton != null) {
            Box(
                Modifier.fillMaxHeight()
                    .padding(top = FABVerticalPadding, end = FABHorizontalPadding),
                contentAlignment = Alignment.TopStart,
            ) {
                floatingActionButton()
            }
        }
    }

/**
 * [Material Design bottom app bar](https://m3.material.io/components/bottom-app-bar/overview)
 *
 * A bottom app bar displays navigation and key actions at the bottom of small screens.
 *
 * ![Bottom app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * If you are interested in displaying a [FloatingActionButton], consider using another overload.
 *
 * Also see [NavigationBar].
 *
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param windowInsets a window insets that app bar will respect.
 * @param content the content of this BottomAppBar. The default layout here is a [Row], so content
 *   inside will be placed horizontally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    content: @Composable RowScope.() -> Unit,
) =
    BottomAppBar(
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        scrollBehavior = null,
        content = content,
    )

/**
 * [Material Design bottom app bar](https://m3.material.io/components/bottom-app-bar/overview)
 *
 * A bottom app bar displays navigation and key actions at the bottom of small screens.
 *
 * ![Bottom app bar
 * image](https://developer.android.com/images/reference/androidx/compose/material3/bottom-app-bar.png)
 *
 * If you are interested in displaying a [FloatingActionButton], consider using another overload.
 *
 * Also see [NavigationBar].
 *
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param tonalElevation when [containerColor] is [ColorScheme.surface], a translucent primary color
 *   overlay is applied on top of the container. A higher tonal elevation value will result in a
 *   darker color in light theme and lighter color in dark theme. See also: [Surface].
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param windowInsets a window insets that app bar will respect.
 * @param scrollBehavior a [BottomAppBarScrollBehavior] which holds various offset values that will
 *   be applied by this bottom app bar to set up its height. A scroll behavior is designed to work
 *   in conjunction with a scrolled content to change the bottom app bar appearance as the content
 *   scrolls. Note that the bottom app bar will not react to scrolling in case a touch exploration
 *   service (e.g., TalkBack) is active. See [BottomAppBarScrollBehavior.nestedScrollConnection].
 * @param content the content of this BottomAppBar. The default layout here is a [Row], so content
 *   inside will be placed horizontally.
 */
@ExperimentalMaterial3Api
@Composable
fun BottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomAppBarDefaults.ContainerElevation,
    contentPadding: PaddingValues = BottomAppBarDefaults.ContentPadding,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    scrollBehavior: BottomAppBarScrollBehavior? = null,
    content: @Composable RowScope.() -> Unit,
) {
    BottomAppBarLayout(
        containerHeight = BottomAppBarTokens.ContainerHeight,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        content = content,
    )
}

// TODO missing image of the flexible bottom app bar.
/**
 * [Material Design flexible bottom app
 * bar](https://m3.material.io/components/bottom-app-bar/overview)
 *
 * A flexible bottom app bar displays navigation and key actions at the bottom of small screens.
 *
 * This variation of the Bottom app bar has a [horizontalArrangement] parameter for controlling the
 * way the content is arranged. Also, it allows more flexibility in controlling the bar's expanded
 * height with an [expandedHeight] value.
 *
 * If you are interested in displaying a [FloatingActionButton], consider using another overload
 * that takes a [FloatingActionButton] parameter.
 *
 * Also see [NavigationBar].
 *
 * A bottom app bar that specifies an [horizontalArrangement] and uses a [scrollBehavior] to
 * customize its nested scrolling behavior when working in conjunction with a scrolling content
 * looks like:
 *
 * @sample androidx.compose.material3.samples.BottomAppBarWithOverflow
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedAround
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedBetween
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBarSpacedEvenly
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBarFixed
 * @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBarFixedVibrant
 * @param modifier the [Modifier] to be applied to this BottomAppBar
 * @param containerColor the color used for the background of this BottomAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param contentColor the preferred color for content inside this BottomAppBar. Defaults to either
 *   the matching content color for [containerColor], or to the current [LocalContentColor] if
 *   [containerColor] is not a color from the theme.
 * @param contentPadding the padding applied to the content of this BottomAppBar
 * @param horizontalArrangement the horizontal arrangement of the content inside this BottomAppBar
 * @param expandedHeight the maximum height this bottom bar can reach when fully expanded. If a
 *   [scrollBehavior] is provided, the bar might collapse or expand based on scrolling. In that
 *   case, this value sets the upper limit for the bar's height during expansion. This [Dp] value
 *   must be specified, finite, and greater than zero; otherwise,
 *   [BottomAppBarDefaults.FlexibleBottomAppBarHeight] will be used as a default. In case the
 *   [scrollBehavior] is `null`, this value will simply be the fixed height of the bottom bar.
 * @param windowInsets a window insets that app bar will respect.
 * @param scrollBehavior a [BottomAppBarScrollBehavior] which holds various offset values that will
 *   be applied by this bottom app bar to set up its height. A scroll behavior is designed to work
 *   in conjunction with a scrolled content to change the bottom app bar appearance as the content
 *   scrolls. Note that the bottom app bar will not react to scrolling in case a touch exploration
 *   service (e.g., TalkBack) is active. See [BottomAppBarScrollBehavior.nestedScrollConnection].
 * @param content the content of this BottomAppBar. The default layout here is a [Row], so content
 *   inside will be placed horizontally.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FlexibleBottomAppBar(
    modifier: Modifier = Modifier,
    containerColor: Color = BottomAppBarDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: PaddingValues = BottomAppBarDefaults.FlexibleContentPadding,
    horizontalArrangement: Arrangement.Horizontal =
        BottomAppBarDefaults.FlexibleHorizontalArrangement,
    expandedHeight: Dp = BottomAppBarDefaults.FlexibleBottomAppBarHeight,
    windowInsets: WindowInsets = BottomAppBarDefaults.windowInsets,
    scrollBehavior: BottomAppBarScrollBehavior? = null,
    content: @Composable RowScope.() -> Unit,
) {
    BottomAppBarLayout(
        containerHeight =
            if (expandedHeight.isFinite && expandedHeight.isSpecified && expandedHeight > 0.dp) {
                expandedHeight
            } else {
                BottomAppBarDefaults.FlexibleBottomAppBarHeight
            },
        horizontalArrangement = horizontalArrangement,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = AppBarTokens.ContainerElevation,
        contentPadding = contentPadding,
        windowInsets = windowInsets,
        scrollBehavior = scrollBehavior,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomAppBarLayout(
    containerHeight: Dp,
    horizontalArrangement: Arrangement.Horizontal,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    tonalElevation: Dp,
    contentPadding: PaddingValues,
    windowInsets: WindowInsets,
    scrollBehavior: BottomAppBarScrollBehavior?,
    content: @Composable RowScope.() -> Unit,
) {
    val touchExplorationServiceEnabled by rememberTouchExplorationService()
    val activeScrollBehavior = if (!touchExplorationServiceEnabled) scrollBehavior else null
    // Set up support for resizing the bottom app bar when vertically dragging the bar itself.
    val appBarDragModifier =
        if (activeScrollBehavior != null && !activeScrollBehavior.isPinned) {
            Modifier.draggable(
                orientation = Orientation.Vertical,
                state =
                    rememberDraggableState { delta ->
                        activeScrollBehavior.state.heightOffset -= delta
                    },
                onDragStopped = { velocity ->
                    settleAppBarBottom(
                        activeScrollBehavior.state,
                        velocity,
                        activeScrollBehavior.flingAnimationSpec,
                        activeScrollBehavior.snapAnimationSpec,
                    )
                },
            )
        } else {
            Modifier
        }

    // Compose a Surface with a Row content.
    // The height of the app bar is determined by subtracting the bar's height offset from the
    // app bar's defined constant height value (i.e. the ContainerHeight token).
    Surface(
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        // TODO(b/209583788): Consider adding a shape parameter if updated design guidance allows
        shape = BottomAppBarTokens.ContainerShape.value,
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    // Sets the app bar's height offset to collapse the entire bar's height when
                    // content is scrolled.
                    activeScrollBehavior?.state?.heightOffsetLimit = -placeable.height.toFloat()

                    val height =
                        (placeable.height + (activeScrollBehavior?.state?.heightOffset ?: 0f))
                            .coerceAtLeast(0f)
                    layout(placeable.width, height.roundToInt()) { placeable.place(0, 0) }
                }
                .then(appBarDragModifier),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(windowInsets)
                .height(containerHeight)
                .padding(contentPadding),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

/**
 * A TopAppBarScrollBehavior defines how an app bar should behave when the content under it is
 * scrolled.
 *
 * @see [TopAppBarDefaults.pinnedScrollBehavior]
 * @see [TopAppBarDefaults.enterAlwaysScrollBehavior]
 * @see [TopAppBarDefaults.exitUntilCollapsedScrollBehavior]
 */
@ExperimentalMaterial3Api
@Stable
interface TopAppBarScrollBehavior {

    /**
     * A [TopAppBarState] that is attached to this behavior and is read and updated when scrolling
     * happens.
     */
    val state: TopAppBarState

    /**
     * Indicates whether the top app bar is pinned.
     *
     * A pinned app bar will stay fixed in place when content is scrolled and will not react to any
     * drag gestures.
     */
    val isPinned: Boolean

    /**
     * An optional [AnimationSpec] that defines how the top app bar snaps to either fully collapsed
     * or fully extended state when a fling or a drag scrolled it into an intermediate position.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * An optional [DecayAnimationSpec] that defined how to fling the top app bar when the user
     * flings the app bar itself, or the content below it.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>?

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection
}

/** Contains default values used for the top app bar implementations. */
object TopAppBarDefaults {

    /**
     * Creates a [TopAppBarColors] for small [TopAppBar]. The default implementation animates
     * between the provided colors according to the Material Design specification.
     */
    @Composable fun topAppBarColors() = MaterialTheme.colorScheme.defaultTopAppBarColors

    /**
     * Creates a [TopAppBarColors] for small [TopAppBar]. The default implementation animates
     * between the provided colors according to the Material Design specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @param subtitleContentColor the content color used for the subtitle
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Composable
    fun topAppBarColors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
        subtitleContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        MaterialTheme.colorScheme.defaultTopAppBarColors.copy(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor,
            subtitleContentColor,
        )

    /**
     * Creates a [TopAppBarColors] for small [TopAppBar]. The default implementation animates
     * between the provided colors according to the Material Design specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Deprecated(
        "Maintained for binary compatibility in favor of topAppBarColors with subtitleContentColor",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun topAppBarColors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = scrolledContainerColor,
            navigationIconContentColor = navigationIconContentColor,
            titleContentColor = titleContentColor,
            actionIconContentColor = actionIconContentColor,
            subtitleContentColor = titleContentColor,
        )

    internal val ColorScheme.defaultTopAppBarColors: TopAppBarColors
        get() {
            return defaultTopAppBarColorsCached
                ?: TopAppBarColors(
                        containerColor = fromToken(AppBarTokens.ContainerColor),
                        scrolledContainerColor = fromToken(AppBarTokens.OnScrollContainerColor),
                        navigationIconContentColor = fromToken(AppBarTokens.LeadingIconColor),
                        titleContentColor = fromToken(AppBarTokens.TitleColor),
                        actionIconContentColor = fromToken(AppBarTokens.TrailingIconColor),
                        subtitleContentColor = fromToken(AppBarTokens.SubtitleColor),
                    )
                    .also { defaultTopAppBarColorsCached = it }
        }

    /** Default padding used for [TopAppBar] content */
    val ContentPadding = PaddingValues(all = 0.dp)

    /** Default insets to be used and consumed by the top app bars */
    val windowInsets: WindowInsets
        @Composable
        get() =
            WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            )

    /**
     * Creates a [TopAppBarColors] for [CenterAlignedTopAppBar]s. The default implementation
     * animates between the provided colors according to the Material Design specification.
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith = ReplaceWith("topAppBarColors()"),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun centerAlignedTopAppBarColors() = MaterialTheme.colorScheme.defaultTopAppBarColors

    /**
     * Creates a [TopAppBarColors] for [CenterAlignedTopAppBar]s. The default implementation
     * animates between the provided colors according to the Material Design specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith =
            ReplaceWith(
                "topAppBarColors(containerColor, scrolledContainerColor, " +
                    "navigationIconContentColor, titleContentColor, actionIconContentColor)"
            ),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun centerAlignedTopAppBarColors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        MaterialTheme.colorScheme.defaultTopAppBarColors.copy(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor,
        )

    /**
     * Creates a [TopAppBarColors] for [MediumTopAppBar]s. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith = ReplaceWith("topAppBarColors()"),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun mediumTopAppBarColors() = MaterialTheme.colorScheme.defaultTopAppBarColors

    /**
     * Creates a [TopAppBarColors] for [MediumTopAppBar]s. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith =
            ReplaceWith(
                "topAppBarColors(containerColor, scrolledContainerColor, " +
                    "navigationIconContentColor, titleContentColor, actionIconContentColor)"
            ),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun mediumTopAppBarColors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        MaterialTheme.colorScheme.defaultTopAppBarColors.copy(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor,
        )

    /**
     * Creates a [TopAppBarColors] for [LargeTopAppBar]s. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith = ReplaceWith("topAppBarColors()"),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun largeTopAppBarColors() = MaterialTheme.colorScheme.defaultTopAppBarColors

    /**
     * Creates a [TopAppBarColors] for [LargeTopAppBar]s. The default implementation interpolates
     * between the provided colors as the top app bar scrolls according to the Material Design
     * specification.
     *
     * @param containerColor the container color
     * @param scrolledContainerColor the container color when content is scrolled behind it
     * @param navigationIconContentColor the content color used for the navigation icon
     * @param titleContentColor the content color used for the title
     * @param actionIconContentColor the content color used for actions
     * @return the resulting [TopAppBarColors] used for the top app bar
     */
    @Deprecated(
        "Use topAppBarColors instead",
        replaceWith =
            ReplaceWith(
                "topAppBarColors(containerColor, scrolledContainerColor, " +
                    "navigationIconContentColor, titleContentColor, actionIconContentColor)"
            ),
        DeprecationLevel.WARNING,
    )
    @Composable
    fun largeTopAppBarColors(
        containerColor: Color = Color.Unspecified,
        scrolledContainerColor: Color = Color.Unspecified,
        navigationIconContentColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors =
        MaterialTheme.colorScheme.defaultTopAppBarColors.copy(
            containerColor,
            scrolledContainerColor,
            navigationIconContentColor,
            titleContentColor,
            actionIconContentColor,
        )

    /**
     * Returns a pinned [TopAppBarScrollBehavior] that tracks nested-scroll callbacks and updates
     * its [TopAppBarState.contentOffset] accordingly.
     *
     * The returned [TopAppBarScrollBehavior] is remembered across compositions.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     *   state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     *   pinned [TopAppBarScrollBehavior]
     */
    @ExperimentalMaterial3Api
    @Composable
    fun pinnedScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(),
        canScroll: () -> Boolean = { true },
    ): TopAppBarScrollBehavior =
        remember(state, canScroll) { PinnedScrollBehavior(state = state, canScroll = canScroll) }

    /**
     * Returns a [TopAppBarScrollBehavior]. A top app bar that is set up with this
     * [TopAppBarScrollBehavior] will immediately collapse when the content is pulled up, and will
     * immediately appear when the content is pulled down.
     *
     * The returned [TopAppBarScrollBehavior] is remembered across compositions.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     *   state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     *   [EnterAlwaysScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps
     *   to either fully collapsed or fully extended state when a fling or a drag scrolled it into
     *   an intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top
     *   app bar when the user flings the app bar itself, or the content below it
     */
    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun enterAlwaysScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(),
        canScroll: () -> Boolean = { true },
        // TODO Load the motionScheme tokens from the component tokens file
        snapAnimationSpec: AnimationSpec<Float>? = MotionSchemeKeyTokens.DefaultEffects.value(),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): TopAppBarScrollBehavior {
        return enterAlwaysScrollBehavior(
            state = state,
            canScroll = canScroll,
            snapAnimationSpec = snapAnimationSpec,
            flingAnimationSpec = flingAnimationSpec,
            reverseLayout = false,
        )
    }

    /**
     * Returns a [TopAppBarScrollBehavior]. A top app bar that is set up with this
     * [TopAppBarScrollBehavior] will immediately collapse when the content is pulled up, and will
     * immediately appear when the content is pulled down.
     *
     * The returned [TopAppBarScrollBehavior] is remembered across compositions.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     *   state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     *   [EnterAlwaysScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps
     *   to either fully collapsed or fully extended state when a fling or a drag scrolled it into
     *   an intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top
     *   app bar when the user flings the app bar itself, or the content below it
     * @param reverseLayout indicates that this behavior is applied to a scrollable content that has
     *   a reversed direction of scrolling and layout
     */
    @ExperimentalMaterial3Api
    @Composable
    fun enterAlwaysScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(),
        canScroll: () -> Boolean = { true },
        // TODO Load the motionScheme tokens from the component tokens file
        snapAnimationSpec: AnimationSpec<Float>? = MotionSchemeKeyTokens.DefaultEffects.value(),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
        reverseLayout: Boolean = false,
    ): TopAppBarScrollBehavior =
        remember(state, canScroll, snapAnimationSpec, flingAnimationSpec, reverseLayout) {
            EnterAlwaysScrollBehavior(
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec,
                canScroll = canScroll,
                reverseLayout = reverseLayout,
            )
        }

    /**
     * Returns a [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and
     * height of the top app bar.
     *
     * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse
     * when the nested content is pulled up, and will expand back the collapsed area when the
     * content is pulled all the way down.
     *
     * The returned [TopAppBarScrollBehavior] is remembered across compositions.
     *
     * @param state the state object to be used to control or observe the top app bar's scroll
     *   state. See [rememberTopAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     *   [ExitUntilCollapsedScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps
     *   to either fully collapsed or fully extended state when a fling or a drag scrolled it into
     *   an intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top
     *   app bar when the user flings the app bar itself, or the content below it
     */
    @ExperimentalMaterial3Api
    @Composable
    fun exitUntilCollapsedScrollBehavior(
        state: TopAppBarState = rememberTopAppBarState(),
        canScroll: () -> Boolean = { true },
        // TODO Load the motionScheme tokens from the component tokens file
        snapAnimationSpec: AnimationSpec<Float>? = MotionSchemeKeyTokens.DefaultEffects.value(),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): TopAppBarScrollBehavior =
        remember(state, canScroll, snapAnimationSpec, flingAnimationSpec) {
            ExitUntilCollapsedScrollBehavior(
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec,
                canScroll = canScroll,
            )
        }

    /** The default expanded height of a [TopAppBar] and the [CenterAlignedTopAppBar]. */
    val TopAppBarExpandedHeight: Dp = AppBarSmallTokens.ContainerHeight

    /** The default height of a [MediumTopAppBar] when collapsed by a [TopAppBarScrollBehavior]. */
    val MediumAppBarCollapsedHeight: Dp = AppBarSmallTokens.ContainerHeight

    /** The default expanded height of a [MediumTopAppBar]. */
    val MediumAppBarExpandedHeight: Dp = AppBarMediumTokens.ContainerHeight

    /** The default expanded height of a [MediumFlexibleTopAppBar] without subtitle. */
    val MediumFlexibleAppBarWithoutSubtitleExpandedHeight: Dp =
        AppBarMediumFlexibleTokens.ContainerHeight

    /** The default expanded height of a [MediumFlexibleTopAppBar] with subtitle. */
    val MediumFlexibleAppBarWithSubtitleExpandedHeight: Dp =
        AppBarMediumFlexibleTokens.LargeContainerHeight

    /** The default height of a [LargeTopAppBar] when collapsed by a [TopAppBarScrollBehavior]. */
    val LargeAppBarCollapsedHeight: Dp = AppBarSmallTokens.ContainerHeight

    /** The default expanded height of a [LargeTopAppBar]. */
    val LargeAppBarExpandedHeight: Dp = AppBarLargeTokens.ContainerHeight

    /** The default expanded height of a [LargeFlexibleTopAppBar] without subtitle. */
    val LargeFlexibleAppBarWithoutSubtitleExpandedHeight: Dp =
        AppBarLargeFlexibleTokens.ContainerHeight

    /** The default expanded height of a [LargeFlexibleTopAppBar] with subtitle. */
    val LargeFlexibleAppBarWithSubtitleExpandedHeight: Dp =
        AppBarLargeFlexibleTokens.LargeContainerHeight
}

/**
 * Creates a [TopAppBarState] that is remembered across compositions.
 *
 * @param initialHeightOffsetLimit the initial value for [TopAppBarState.heightOffsetLimit], which
 *   represents the pixel limit that a top app bar is allowed to collapse when the scrollable
 *   content is scrolled
 * @param initialHeightOffset the initial value for [TopAppBarState.heightOffset]. The initial
 *   offset height offset should be between zero and [initialHeightOffsetLimit].
 * @param initialContentOffset the initial value for [TopAppBarState.contentOffset]
 */
@Composable
fun rememberTopAppBarState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
): TopAppBarState {
    return rememberSaveable(saver = TopAppBarState.Saver) {
        TopAppBarState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
    }
}

/**
 * A state object that can be hoisted to control and observe the top app bar state. The state is
 * read and updated by a [TopAppBarScrollBehavior] implementation.
 *
 * In most cases, this state will be created via [rememberTopAppBarState].
 *
 * @param initialHeightOffsetLimit the initial value for [TopAppBarState.heightOffsetLimit]
 * @param initialHeightOffset the initial value for [TopAppBarState.heightOffset]
 * @param initialContentOffset the initial value for [TopAppBarState.contentOffset]
 */
@Stable
class TopAppBarState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
) {

    /**
     * The top app bar's height offset limit in pixels, which represents the limit that a top app
     * bar is allowed to collapse to.
     *
     * Use this limit to coerce the [heightOffset] value when it's updated.
     */
    var heightOffsetLimit = initialHeightOffsetLimit

    /**
     * The top app bar's current height offset in pixels. This height offset is applied to the fixed
     * height of the app bar to control the displayed height when content is being scrolled.
     *
     * Updates to the [heightOffset] value are coerced between zero and [heightOffsetLimit].
     */
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    /**
     * The total offset of the content scrolled under the top app bar.
     *
     * The content offset is used to compute the [overlappedFraction], which can later be read by an
     * implementation.
     *
     * This value is updated by a [TopAppBarScrollBehavior] whenever a nested scroll connection
     * consumes scroll events. A common implementation would update the value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed.y` values.
     */
    var contentOffset by mutableFloatStateOf(initialContentOffset)

    /**
     * A value that represents the collapsed height percentage of the app bar.
     *
     * A `0.0` represents a fully expanded bar, and `1.0` represents a fully collapsed bar (computed
     * as [heightOffset] / [heightOffsetLimit]).
     */
    val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }

    /**
     * A value that represents the percentage of the app bar area that is overlapping with the
     * content scrolled behind it.
     *
     * A `0.0` indicates that the app bar does not overlap any content, while `1.0` indicates that
     * the entire visible app bar area overlaps the scrolled content.
     */
    val overlappedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                1 -
                    ((heightOffsetLimit - contentOffset).coerceIn(
                        minimumValue = heightOffsetLimit,
                        maximumValue = 0f,
                    ) / heightOffsetLimit)
            } else {
                0f
            }

    companion object {
        /** The default [Saver] implementation for [TopAppBarState]. */
        val Saver: Saver<TopAppBarState, *> =
            listSaver(
                save = { listOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset) },
                restore = {
                    TopAppBarState(
                        initialHeightOffsetLimit = it[0],
                        initialHeightOffset = it[1],
                        initialContentOffset = it[2],
                    )
                },
            )
    }

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
}

/**
 * Represents the colors used by a top app bar in different states. This implementation animates the
 * container color according to the top app bar scroll state. It does not animate the leading,
 * headline, or trailing colors.
 *
 * @param containerColor the color used for the background of this TopAppBar. Use
 *   [Color.Transparent] to have no color.
 * @param scrolledContainerColor the container color when content is scrolled behind it
 * @param navigationIconContentColor the content color used for the navigation icon
 * @param titleContentColor the content color used for the title
 * @param actionIconContentColor the content color used for actions
 * @param subtitleContentColor the content color used for a subtitle
 * @constructor create an instance with arbitrary colors, see [TopAppBarColors] for a factory method
 *   using the default material3 spec
 */
@Stable
class TopAppBarColors(
    val containerColor: Color,
    val scrolledContainerColor: Color,
    val navigationIconContentColor: Color,
    val titleContentColor: Color,
    val actionIconContentColor: Color,
    val subtitleContentColor: Color,
) {
    @Deprecated(
        "Use the TopAppBarColors constructor with subtitleContentColor",
        replaceWith =
            ReplaceWith(
                "TopAppBarColors(containerColor, scrolledContainerColor," +
                    "navigationIconContentColor, titleContentColor, actionIconContentColor, " +
                    "subtitleContentColor)"
            ),
        DeprecationLevel.WARNING,
    )
    constructor(
        containerColor: Color,
        scrolledContainerColor: Color,
        navigationIconContentColor: Color,
        titleContentColor: Color,
        actionIconContentColor: Color,
    ) : this(
        containerColor = containerColor,
        scrolledContainerColor = scrolledContainerColor,
        navigationIconContentColor = navigationIconContentColor,
        titleContentColor = titleContentColor,
        actionIconContentColor = actionIconContentColor,
        subtitleContentColor = titleContentColor,
    )

    /**
     * Returns a copy of this TopAppBarColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        scrolledContainerColor: Color = this.scrolledContainerColor,
        navigationIconContentColor: Color = this.navigationIconContentColor,
        titleContentColor: Color = this.titleContentColor,
        actionIconContentColor: Color = this.actionIconContentColor,
        subtitleContentColor: Color = this.subtitleContentColor,
    ) =
        TopAppBarColors(
            containerColor.takeOrElse { this.containerColor },
            scrolledContainerColor.takeOrElse { this.scrolledContainerColor },
            navigationIconContentColor.takeOrElse { this.navigationIconContentColor },
            titleContentColor.takeOrElse { this.titleContentColor },
            actionIconContentColor.takeOrElse { this.actionIconContentColor },
            subtitleContentColor.takeOrElse { this.subtitleContentColor },
        )

    /**
     * Represents the container color used for the top app bar.
     *
     * A [colorTransitionFraction] provides a percentage value that can be used to generate a color.
     * Usually, an app bar implementation will pass in a [colorTransitionFraction] read from the
     * [TopAppBarState.collapsedFraction] or the [TopAppBarState.overlappedFraction].
     *
     * @param colorTransitionFraction a `0.0` to `1.0` value that represents a color transition
     *   percentage
     */
    @Stable
    internal fun containerColor(colorTransitionFraction: Float): Color {
        return lerp(
            containerColor,
            scrolledContainerColor,
            FastOutLinearInEasing.transform(colorTransitionFraction),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is TopAppBarColors) return false

        if (containerColor != other.containerColor) return false
        if (scrolledContainerColor != other.scrolledContainerColor) return false
        if (navigationIconContentColor != other.navigationIconContentColor) return false
        if (titleContentColor != other.titleContentColor) return false
        if (actionIconContentColor != other.actionIconContentColor) return false
        if (subtitleContentColor != other.subtitleContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + scrolledContainerColor.hashCode()
        result = 31 * result + navigationIconContentColor.hashCode()
        result = 31 * result + titleContentColor.hashCode()
        result = 31 * result + actionIconContentColor.hashCode()
        result = 31 * result + subtitleContentColor.hashCode()

        return result
    }
}

/**
 * A BottomAppBarScrollBehavior defines how a bottom app bar should behave when the content under it
 * is scrolled.
 *
 * @see [BottomAppBarDefaults.exitAlwaysScrollBehavior]
 */
@ExperimentalMaterial3Api
@Stable
interface BottomAppBarScrollBehavior {

    /**
     * A [BottomAppBarState] that is attached to this behavior and is read and updated when
     * scrolling happens.
     */
    val state: BottomAppBarState

    /**
     * Indicates whether the bottom app bar is pinned.
     *
     * A pinned app bar will stay fixed in place when content is scrolled and will not react to any
     * drag gestures.
     */
    val isPinned: Boolean

    /**
     * An optional [AnimationSpec] that defines how the bottom app bar snaps to either fully
     * collapsed or fully extended state when a fling or a drag scrolled it into an intermediate
     * position.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * An optional [DecayAnimationSpec] that defined how to fling the bottom app bar when the user
     * flings the app bar itself, or the content below it.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>?

    /**
     * A [NestedScrollConnection] that should be attached to a [Modifier.nestedScroll] in order to
     * keep track of the scroll events.
     */
    val nestedScrollConnection: NestedScrollConnection
}

/** Contains default values used for the bottom app bar implementations. */
object BottomAppBarDefaults {

    /** Default color used for [BottomAppBar] container */
    val containerColor: Color
        @Composable get() = BottomAppBarTokens.ContainerColor.value

    /** Default elevation used for [BottomAppBar] */
    val ContainerElevation: Dp = 0.dp

    /**
     * Default padding used for [BottomAppBar] when content are default size (24dp) icons in
     * [IconButton] that meet the minimum touch target (48.dp).
     */
    val ContentPadding =
        PaddingValues(
            start = BottomAppBarHorizontalPadding,
            top = BottomAppBarVerticalPadding,
            end = BottomAppBarHorizontalPadding,
        )

    /** Default insets that will be used and consumed by [BottomAppBar]. */
    val windowInsets: WindowInsets
        @Composable
        get() {
            return WindowInsets.systemBarsForVisualComponents.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
        }

    /** The color of a [BottomAppBar]'s [FloatingActionButton] */
    val bottomAppBarFabColor: Color
        @Composable get() = FabSecondaryContainerTokens.ContainerColor.value

    @ExperimentalMaterial3ExpressiveApi
    /** Default padding used for [FlexibleBottomAppBar]. */
    val FlexibleContentPadding =
        PaddingValues(
            start = DockedToolbarTokens.ContainerLeadingSpace,
            end = DockedToolbarTokens.ContainerTrailingSpace,
        )

    /**
     * Default height of a flexible [FlexibleBottomAppBar]. The height here represents the height of
     * the bottom app bar in its expanded state.
     */
    @ExperimentalMaterial3ExpressiveApi
    val FlexibleBottomAppBarHeight = DockedToolbarTokens.ContainerHeight

    /** A default [Arrangement] that will be used to space a [FlexibleBottomAppBar]'s content. */
    @ExperimentalMaterial3ExpressiveApi
    val FlexibleHorizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween

    /**
     * An [Arrangement] that will be used to space [FlexibleBottomAppBar]'s with a fixed spacing.
     */
    @ExperimentalMaterial3ExpressiveApi
    val FlexibleFixedHorizontalArrangement: Arrangement.Horizontal =
        Arrangement.spacedBy(DockedToolbarTokens.ContainerMaxSpacing, Alignment.CenterHorizontally)

    // TODO: note that this scroll behavior may impact assistive technologies making the component
    //  inaccessible. See @sample androidx.compose.material3.samples.ExitAlwaysBottomAppBar on how
    //  to disable scrolling when touch exploration is enabled.
    /**
     * Returns a [BottomAppBarScrollBehavior]. A bottom app bar that is set up with this
     * [BottomAppBarScrollBehavior] will immediately collapse when the content is pulled up, and
     * will immediately appear when the content is pulled down.
     *
     * The returned [BottomAppBarScrollBehavior] is remembered across compositions.
     *
     * @param state the state object to be used to control or observe the bottom app bar's scroll
     *   state. See [rememberBottomAppBarState] for a state that is remembered across compositions.
     * @param canScroll a callback used to determine whether scroll events are to be handled by this
     *   [ExitAlwaysScrollBehavior]
     * @param snapAnimationSpec an optional [AnimationSpec] that defines how the bottom app bar
     *   snaps to either fully collapsed or fully extended state when a fling or a drag scrolled it
     *   into an intermediate position
     * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the
     *   bottom app bar when the user flings the app bar itself, or the content below it
     */
    @ExperimentalMaterial3Api
    @Composable
    fun exitAlwaysScrollBehavior(
        state: BottomAppBarState = rememberBottomAppBarState(),
        canScroll: () -> Boolean = { true },
        // TODO Load the motionScheme tokens from the component tokens file
        snapAnimationSpec: AnimationSpec<Float>? = MotionSchemeKeyTokens.FastSpatial.value(),
        flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
    ): BottomAppBarScrollBehavior =
        remember(state, canScroll, snapAnimationSpec, flingAnimationSpec) {
            ExitAlwaysScrollBehavior(
                state = state,
                snapAnimationSpec = snapAnimationSpec,
                flingAnimationSpec = flingAnimationSpec,
                canScroll = canScroll,
            )
        }
}

/**
 * Creates a [BottomAppBarState] that is remembered across compositions.
 *
 * @param initialHeightOffsetLimit the initial value for [BottomAppBarState.heightOffsetLimit],
 *   which represents the pixel limit that a bottom app bar is allowed to collapse when the
 *   scrollable content is scrolled
 * @param initialHeightOffset the initial value for [BottomAppBarState.heightOffset]. The initial
 *   offset height offset should be between zero and [initialHeightOffsetLimit].
 * @param initialContentOffset the initial value for [BottomAppBarState.contentOffset]
 */
@ExperimentalMaterial3Api
@Composable
fun rememberBottomAppBarState(
    initialHeightOffsetLimit: Float = -Float.MAX_VALUE,
    initialHeightOffset: Float = 0f,
    initialContentOffset: Float = 0f,
): BottomAppBarState {
    return rememberSaveable(saver = BottomAppBarState.Saver) {
        BottomAppBarState(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)
    }
}

/**
 * A state object that can be hoisted to control and observe the bottom app bar state. The state is
 * read and updated by a [BottomAppBarScrollBehavior] implementation.
 *
 * In most cases, this state will be created via [rememberBottomAppBarState].
 */
@ExperimentalMaterial3Api
interface BottomAppBarState {

    /**
     * The bottom app bar's height offset limit in pixels, which represents the limit that a bottom
     * app bar is allowed to collapse to.
     *
     * Use this limit to coerce the [heightOffset] value when it's updated.
     */
    var heightOffsetLimit: Float

    /**
     * The bottom app bar's current height offset in pixels. This height offset is applied to the
     * fixed height of the app bar to control the displayed height when content is being scrolled.
     *
     * Updates to the [heightOffset] value are coerced between zero and [heightOffsetLimit].
     */
    var heightOffset: Float

    /**
     * The total offset of the content scrolled under the bottom app bar.
     *
     * This value is updated by a [BottomAppBarScrollBehavior] whenever a nested scroll connection
     * consumes scroll events. A common implementation would update the value to be the sum of all
     * [NestedScrollConnection.onPostScroll] `consumed.y` values.
     */
    var contentOffset: Float

    /**
     * A value that represents the collapsed height percentage of the app bar.
     *
     * A `0.0` represents a fully expanded bar, and `1.0` represents a fully collapsed bar (computed
     * as [heightOffset] / [heightOffsetLimit]).
     */
    val collapsedFraction: Float

    companion object {
        /** The default [Saver] implementation for [BottomAppBarState]. */
        val Saver: Saver<BottomAppBarState, *> =
            listSaver(
                save = { listOf(it.heightOffsetLimit, it.heightOffset, it.contentOffset) },
                restore = {
                    BottomAppBarState(
                        initialHeightOffsetLimit = it[0],
                        initialHeightOffset = it[1],
                        initialContentOffset = it[2],
                    )
                },
            )
    }
}

/**
 * Creates a [BottomAppBarState].
 *
 * @param initialHeightOffsetLimit the initial value for [BottomAppBarState.heightOffsetLimit],
 *   which represents the pixel limit that a bottom app bar is allowed to collapse when the
 *   scrollable content is scrolled
 * @param initialHeightOffset the initial value for [BottomAppBarState.heightOffset]. The initial
 *   offset height offset should be between zero and [initialHeightOffsetLimit].
 * @param initialContentOffset the initial value for [BottomAppBarState.contentOffset]
 */
@ExperimentalMaterial3Api
fun BottomAppBarState(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
): BottomAppBarState =
    BottomAppBarStateImpl(initialHeightOffsetLimit, initialHeightOffset, initialContentOffset)

@OptIn(ExperimentalMaterial3Api::class)
@Stable
private class BottomAppBarStateImpl(
    initialHeightOffsetLimit: Float,
    initialHeightOffset: Float,
    initialContentOffset: Float,
) : BottomAppBarState {

    override var heightOffsetLimit by mutableFloatStateOf(initialHeightOffsetLimit)

    override var heightOffset: Float
        get() = _heightOffset.floatValue
        set(newOffset) {
            _heightOffset.floatValue =
                newOffset.coerceIn(minimumValue = heightOffsetLimit, maximumValue = 0f)
        }

    override var contentOffset by mutableFloatStateOf(initialContentOffset)

    override val collapsedFraction: Float
        get() =
            if (heightOffsetLimit != 0f) {
                heightOffset / heightOffsetLimit
            } else {
                0f
            }

    private var _heightOffset = mutableFloatStateOf(initialHeightOffset)
}

/**
 * A [BottomAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a
 * bottom app bar.
 *
 * A bottom app bar that is set up with this [BottomAppBarScrollBehavior] will immediately collapse
 * when the nested content is pulled up, and will immediately appear when the content is pulled
 * down.
 *
 * @param state a [BottomAppBarState]
 * @param snapAnimationSpec an optional [AnimationSpec] that defines how the bottom app bar snaps to
 *   either fully collapsed or fully extended state when a fling or a drag scrolled it into an
 *   intermediate position
 * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the bottom
 *   app bar when the user flings the app bar itself, or the content below it
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 *   [ExitAlwaysScrollBehavior]
 */
@OptIn(ExperimentalMaterial3Api::class)
private class ExitAlwaysScrollBehavior(
    override val state: BottomAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : BottomAppBarScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                state.heightOffset = state.heightOffset + consumed.y
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (
                    available.y > 0f &&
                        (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit)
                ) {
                    // Reset the total content offset to zero when scrolling all the way down.
                    // This will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed +
                    settleAppBarBottom(state, available.y, flingAnimationSpec, snapAnimationSpec)
            }
        }
}

/**
 * Settles the app bar by flinging, in case the given velocity is greater than zero, and snapping
 * after the fling settles.
 */
@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBarBottom(
    state: BottomAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = velocity).animateDecay(
            flingAnimationSpec
        ) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset
            state.heightOffset = initialHeightOffset + delta
            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
        }
    }
    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec,
            ) {
                state.heightOffset = value
            }
        }
    }

    return Velocity(0f, remainingVelocity)
}

// Padding minus IconButton's min touch target expansion
private val BottomAppBarHorizontalPadding = 16.dp - 12.dp
internal val BottomAppBarVerticalPadding = 16.dp - 12.dp

// Padding minus content padding
private val FABHorizontalPadding = 16.dp - BottomAppBarHorizontalPadding
private val FABVerticalPadding = 12.dp - BottomAppBarVerticalPadding

/**
 * A single-row top app bar that is designed to be called by the small and center aligned top app
 * bar composables.
 *
 * This SingleRowTopAppBar has slots for a title, subtitle, navigation icon, and actions.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
)
@Composable
private fun SingleRowTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    subtitle: (@Composable () -> Unit)?,
    subtitleTextStyle: TextStyle,
    titleHorizontalAlignment: Alignment.Horizontal,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    expandedHeight: Dp,
    contentPadding: PaddingValues,
    windowInsets: WindowInsets,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val scope =
        SingleRowTopAppBarOverrideScope(
            modifier = modifier,
            title = title,
            titleTextStyle = titleTextStyle,
            subtitle = subtitle,
            subtitleTextStyle = subtitleTextStyle,
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = expandedHeight,
            contentPadding = contentPadding,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    with(LocalSingleRowTopAppBarOverride.current) { scope.SingleRowTopAppBar() }
}

/**
 * Provides the default behavior of the [SingleRowTopAppBar] component. This implementation is used
 * when no override is specified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
object DefaultSingleRowTopAppBarOverride : SingleRowTopAppBarOverride {
    @Composable
    override fun SingleRowTopAppBarOverrideScope.SingleRowTopAppBar() {
        require(expandedHeight.isSpecified && expandedHeight.isFinite) {
            "The expandedHeight is expected to be specified and finite"
        }

        // Obtain the container color from the TopAppBarColors using the `overlapFraction`. This
        // ensures that the colors will adjust whether the app bar behavior is pinned or scrolled.
        // This may potentially animate or interpolate a transition between the container-color and
        // the container's scrolled-color according to the app bar's scroll state.
        val targetColor by
            remember(colors, scrollBehavior) {
                derivedStateOf {
                    val overlappingFraction = scrollBehavior?.state?.overlappedFraction ?: 0f
                    colors.containerColor(if (overlappingFraction > 0.01f) 1f else 0f)
                }
            }

        val appBarContainerColor =
            animateColorAsState(
                targetColor,
                // TODO Load the motionScheme tokens from the component tokens file
                animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
            )

        // Wrap the given actions in a Row.
        val actionsRow =
            @Composable {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }

        // Set up support for resizing the top app bar when vertically dragging the bar itself.
        val appBarDragModifier =
            if (scrollBehavior != null && !scrollBehavior.isPinned) {
                Modifier.draggable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberDraggableState { delta ->
                            scrollBehavior.state.heightOffset += delta
                        },
                    onDragStopped = { velocity ->
                        settleAppBar(
                            scrollBehavior.state,
                            velocity,
                            scrollBehavior.flingAnimationSpec,
                            scrollBehavior.snapAnimationSpec,
                        )
                    },
                )
            } else {
                Modifier
            }

        // Compose a Surface with a TopAppBarLayout content.
        // The surface's background color is animated as specified above.
        // The height of the app bar is determined by subtracting the bar's height offset from the
        // app bar's defined constant height value (i.e. the ContainerHeight token).
        Box(
            modifier =
                modifier
                    .then(appBarDragModifier)
                    .drawBehind {
                        val color = appBarContainerColor.value
                        if (color != Color.Unspecified) {
                            drawRect(color = color)
                        }
                    }
                    .semantics { isTraversalGroup = true }
                    .pointerInput(Unit) {}
        ) {
            TopAppBarLayout(
                modifier =
                    Modifier.windowInsetsPadding(windowInsets)
                        // clip after padding so we don't show the title over the inset area
                        .clipToBounds()
                        .adjustHeightOffsetLimit(scrollBehavior),
                scrolledOffset = { scrollBehavior?.state?.heightOffset ?: 0f },
                navigationIconContentColor = colors.navigationIconContentColor,
                titleContentColor = colors.titleContentColor,
                actionIconContentColor = colors.actionIconContentColor,
                subtitleContentColor = colors.subtitleContentColor,
                title = title,
                titleTextStyle = titleTextStyle,
                subtitle = subtitle,
                subtitleTextStyle = subtitleTextStyle,
                titleAlpha = { 1f },
                titleVerticalArrangement = Arrangement.Center,
                titleHorizontalAlignment = titleHorizontalAlignment,
                titleBottomPadding = 0,
                hideTitleSemantics = false,
                navigationIcon = navigationIcon,
                actions = actionsRow,
                height = expandedHeight,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * Interface that allows libraries to override the behavior of single-row `TopAppBar` components.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalSingleRowTopAppBarOverride] in the Compose hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
interface SingleRowTopAppBarOverride {
    /** Behavior function that is called by the [SingleRowTopAppBar] component. */
    @Composable fun SingleRowTopAppBarOverrideScope.SingleRowTopAppBar()
}

/**
 * Parameters available to [SingleRowTopAppBar].
 *
 * @property modifier the [Modifier] to be applied to this top app bar
 * @property title the title to be displayed in the top app bar typically be an [IconButton] or
 *   [IconToggleButton].
 * @property titleTextStyle the title's text style
 * @property subtitle a lambda for providing an optional subtitle to be displayed in the top app bar
 *   in collapsed and expanded states.
 * @property subtitleTextStyle the subtitle's text style
 * @property titleHorizontalAlignment the title's horizontal alignment
 * @property navigationIcon the navigation icon displayed at the start of the top app bar. This
 *   should typically be an [IconButton] or [IconToggleButton].
 * @property actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @property expandedHeight this app bar's height. When a specified [scrollBehavior] causes the app
 *   bar to collapse or expand, this value will represent the maximum height that the bar will be
 *   allowed to expand. This value must be specified and finite, otherwise it will be ignored and
 *   replaced with [TopAppBarDefaults.TopAppBarExpandedHeight].
 * @property windowInsets a window insets that app bar will respect.
 * @property colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 *   bar in different states. See [TopAppBarDefaults.topAppBarColors].
 * @property scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will
 *   be applied by this top app bar to set up its height and colors. A scroll behavior is designed
 *   to work in conjunction with a scrolled content to change the top app bar appearance as the
 *   content scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
class SingleRowTopAppBarOverrideScope
internal constructor(
    val modifier: Modifier,
    val title: @Composable () -> Unit,
    val titleTextStyle: TextStyle,
    val subtitle: (@Composable () -> Unit)?,
    val subtitleTextStyle: TextStyle,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val navigationIcon: @Composable () -> Unit,
    val actions: @Composable RowScope.() -> Unit,
    val expandedHeight: Dp,
    val contentPadding: PaddingValues,
    val windowInsets: WindowInsets,
    val colors: TopAppBarColors,
    val scrollBehavior: TopAppBarScrollBehavior?,
)

/** CompositionLocal containing the currently-selected [SingleRowTopAppBarOverride]. */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
val LocalSingleRowTopAppBarOverride: ProvidableCompositionLocal<SingleRowTopAppBarOverride> =
    compositionLocalOf {
        DefaultSingleRowTopAppBarOverride
    }

/**
 * A two-rows top app bar that is designed to be called by the Large and Medium top app bar
 * composables.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3ComponentOverrideApi::class,
)
@Composable
private fun TwoRowsTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    titleBottomPadding: Dp,
    smallTitle: @Composable () -> Unit,
    smallTitleTextStyle: TextStyle,
    subtitle: (@Composable () -> Unit)?,
    subtitleTextStyle: TextStyle,
    smallSubtitle: (@Composable () -> Unit)?,
    smallSubtitleTextStyle: TextStyle,
    titleHorizontalAlignment: Alignment.Horizontal,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    collapsedHeight: Dp,
    expandedHeight: Dp,
    windowInsets: WindowInsets,
    colors: TopAppBarColors,
    scrollBehavior: TopAppBarScrollBehavior?,
) {
    val scope =
        TwoRowsTopAppBarOverrideScope(
            modifier = modifier,
            title = title,
            titleTextStyle = titleTextStyle,
            titleBottomPadding = titleBottomPadding,
            smallTitle = smallTitle,
            smallTitleTextStyle = smallTitleTextStyle,
            subtitle = subtitle,
            subtitleTextStyle = subtitleTextStyle,
            smallSubtitle = smallSubtitle,
            smallSubtitleTextStyle = smallSubtitleTextStyle,
            titleHorizontalAlignment = titleHorizontalAlignment,
            navigationIcon = navigationIcon,
            actions = actions,
            collapsedHeight = collapsedHeight,
            expandedHeight = expandedHeight,
            windowInsets = windowInsets,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    with(LocalTwoRowsTopAppBarOverride.current) { scope.TwoRowsTopAppBar() }
}

/**
 * Provides the default behavior of the [TwoRowsTopAppBar] component. This implementation is used
 * when no override is specified.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
object DefaultTwoRowsTopAppBarOverride : TwoRowsTopAppBarOverride {
    @Composable
    override fun TwoRowsTopAppBarOverrideScope.TwoRowsTopAppBar() {
        require(collapsedHeight.isSpecified && collapsedHeight.isFinite) {
            "The collapsedHeight is expected to be specified and finite"
        }
        require(expandedHeight.isSpecified && expandedHeight.isFinite) {
            "The expandedHeight is expected to be specified and finite"
        }
        require(expandedHeight >= collapsedHeight) {
            "The expandedHeight is expected to be greater or equal to the collapsedHeight"
        }
        val titleBottomPaddingPx = with(LocalDensity.current) { titleBottomPadding.roundToPx() }

        // Obtain the container Color from the TopAppBarColors using the `collapsedFraction`, as the
        // bottom part of this TwoRowsTopAppBar changes color at the same rate the app bar expands
        // or collapse.
        // This will potentially animate or interpolate a transition between the container color and
        // the container's scrolled color according to the app bar's scroll state.
        val colorTransitionFraction = { scrollBehavior?.state?.collapsedFraction ?: 0f }
        val appBarContainerColor = { colors.containerColor(colorTransitionFraction()) }

        // Wrap the given actions in a Row.
        val actionsRow =
            @Composable {
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        val topTitleAlpha = { TopTitleAlphaEasing.transform(colorTransitionFraction()) }
        val bottomTitleAlpha = { 1f - colorTransitionFraction() }
        // Hide the top row title semantics when its alpha value goes below 0.5 threshold.
        // Hide the bottom row title semantics when the top title semantics are active.
        val hideTopRowSemantics by
            remember(colorTransitionFraction) {
                derivedStateOf { colorTransitionFraction() < 0.5f }
            }
        val hideBottomRowSemantics = !hideTopRowSemantics

        // Set up support for resizing the top app bar when vertically dragging the bar itself.
        val appBarDragModifier =
            if (scrollBehavior != null && !scrollBehavior.isPinned) {
                Modifier.draggable(
                    orientation = Orientation.Vertical,
                    state =
                        rememberDraggableState { delta ->
                            scrollBehavior.state.heightOffset += delta
                        },
                    onDragStopped = { velocity ->
                        settleAppBar(
                            scrollBehavior.state,
                            velocity,
                            scrollBehavior.flingAnimationSpec,
                            scrollBehavior.snapAnimationSpec,
                        )
                    },
                )
            } else {
                Modifier
            }

        Box(
            modifier =
                modifier
                    .then(appBarDragModifier)
                    .drawBehind { drawRect(color = appBarContainerColor()) }
                    .semantics { isTraversalGroup = true }
                    .pointerInput(Unit) {}
        ) {
            Column {
                TopAppBarLayout(
                    modifier =
                        Modifier.windowInsetsPadding(windowInsets)
                            // clip after padding so we don't show the title over the inset area
                            .clipToBounds(),
                    scrolledOffset = { 0f },
                    navigationIconContentColor = colors.navigationIconContentColor,
                    titleContentColor = colors.titleContentColor,
                    actionIconContentColor = colors.actionIconContentColor,
                    subtitleContentColor = colors.subtitleContentColor,
                    title = smallTitle,
                    titleTextStyle = smallTitleTextStyle,
                    subtitle = smallSubtitle,
                    subtitleTextStyle = smallSubtitleTextStyle,
                    titleAlpha = topTitleAlpha,
                    titleVerticalArrangement = Arrangement.Center,
                    titleHorizontalAlignment = titleHorizontalAlignment,
                    titleBottomPadding = 0,
                    hideTitleSemantics = hideTopRowSemantics,
                    navigationIcon = navigationIcon,
                    actions = actionsRow,
                    height = collapsedHeight,
                    contentPadding = TopAppBarDefaults.ContentPadding,
                )
                TopAppBarLayout(
                    modifier =
                        Modifier
                            // only apply the horizontal sides of the window insets padding, since
                            // the top padding will always be applied by the layout above
                            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal))
                            .clipToBounds()
                            .adjustHeightOffsetLimit(scrollBehavior),
                    scrolledOffset = { scrollBehavior?.state?.heightOffset ?: 0f },
                    navigationIconContentColor = colors.navigationIconContentColor,
                    titleContentColor = colors.titleContentColor,
                    actionIconContentColor = colors.actionIconContentColor,
                    subtitleContentColor = colors.subtitleContentColor,
                    title = title,
                    titleTextStyle = titleTextStyle,
                    subtitle = subtitle,
                    subtitleTextStyle = subtitleTextStyle,
                    titleAlpha = bottomTitleAlpha,
                    titleVerticalArrangement = Arrangement.Bottom,
                    titleHorizontalAlignment = titleHorizontalAlignment,
                    titleBottomPadding = titleBottomPaddingPx,
                    hideTitleSemantics = hideBottomRowSemantics,
                    navigationIcon = {},
                    actions = {},
                    height = expandedHeight - collapsedHeight,
                    contentPadding = TopAppBarDefaults.ContentPadding,
                )
            }
        }
    }
}

/**
 * Interface that allows libraries to override the behavior of two-row `TopAppBar` components.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalTwoRowsTopAppBarOverride] in the Compose hierarchy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
interface TwoRowsTopAppBarOverride {
    /** Behavior function that is called by the [TwoRowsTopAppBar] component. */
    @Composable fun TwoRowsTopAppBarOverrideScope.TwoRowsTopAppBar()
}

/**
 * Parameters available to [TwoRowsTopAppBar].
 *
 * @property title a lambda for providing a title to be displayed in the top app bar in collapsed
 *   and expanded states. By default a small-app-bar [TextStyle] is applied to the Composition, and
 *   you may override it by wrapping your provided component with a composition local. Note that
 *   unlike the large or medium top app bars, the `TwoRowsTopAppBar` does not append bottom padding
 *   to the expanded title Composable by default. Padding should be applied directly to the provided
 *   expanded title, or to the [subtitle] that appears below it.
 * @property modifier the [Modifier] to be applied to this top app bar
 * @property subtitle a lambda for providing an optional subtitle to be displayed in the top app bar
 *   in collapsed and expanded states.
 * @property navigationIcon the navigation icon displayed at the start of the top app bar. This
 *   should typically be an [IconButton] or [IconToggleButton].
 * @property actions the actions displayed at the end of the top app bar. This should typically be
 *   [IconButton]s. The default layout here is a [Row], so icons inside will be placed horizontally.
 * @property titleHorizontalAlignment the horizontal alignment of the title and subtitle
 * @property collapsedHeight the app bar's height in its collapsed state. Note that this value might
 *   be adjusted to support displaying larger fonts. In case the provided value is [Dp.Unspecified]
 *   or [Dp.Infinity], the height will default to [TopAppBarDefaults.MediumAppBarCollapsedHeight].
 * @property expandedHeight this app bar's height in its expanded state. When a specified
 *   [scrollBehavior] causes the app bar to collapse or expand, this value will represent the total
 *   height that the app-bar will expand to. The expanded height is expected to be greater or equal
 *   to the [collapsedHeight], and the function will throw an [IllegalArgumentException] otherwise.
 *   Note that this value might be adjusted to support displaying larger fonts. In case the provided
 *   value is [Dp.Unspecified] or [Dp.Infinity], the height will default to
 *   [TopAppBarDefaults.MediumFlexibleAppBarWithSubtitleExpandedHeight] when an [expandedSubtitle]
 *   is provided, or to [TopAppBarDefaults.MediumFlexibleAppBarWithoutSubtitleExpandedHeight] when
 *   it's not.
 * @property windowInsets a window insets that app bar will respect.
 * @property colors [TopAppBarColors] that will be used to resolve the colors used for this top app
 *   bar in different states. See [TopAppBarDefaults.topAppBarColors].
 * @property scrollBehavior a [TopAppBarScrollBehavior] which holds various offset values that will
 *   be applied by this top app bar to set up its height and colors. A scroll behavior is designed
 *   to work in conjunction with a scrolled content to change the top app bar appearance as the
 *   content scrolls. See [TopAppBarScrollBehavior.nestedScrollConnection].
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
class TwoRowsTopAppBarOverrideScope
internal constructor(
    val modifier: Modifier,
    val title: @Composable () -> Unit,
    val titleTextStyle: TextStyle,
    val titleBottomPadding: Dp,
    val smallTitle: @Composable () -> Unit,
    val smallTitleTextStyle: TextStyle,
    val subtitle: (@Composable () -> Unit)?,
    val subtitleTextStyle: TextStyle,
    val smallSubtitle: (@Composable () -> Unit)?,
    val smallSubtitleTextStyle: TextStyle,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val navigationIcon: @Composable () -> Unit,
    val actions: @Composable RowScope.() -> Unit,
    val collapsedHeight: Dp,
    val expandedHeight: Dp,
    val windowInsets: WindowInsets,
    val colors: TopAppBarColors,
    val scrollBehavior: TopAppBarScrollBehavior?,
)

/** CompositionLocal containing the currently-selected [TwoRowsTopAppBarOverride]. */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3ComponentOverrideApi
val LocalTwoRowsTopAppBarOverride: ProvidableCompositionLocal<TwoRowsTopAppBarOverride> =
    compositionLocalOf {
        DefaultTwoRowsTopAppBarOverride
    }

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.adjustHeightOffsetLimit(scrollBehavior: TopAppBarScrollBehavior?) =
    scrollBehavior?.state?.let {
        onSizeChanged { size ->
            val offset = size.height.toFloat() - it.heightOffset
            it.heightOffsetLimit = -offset
        }
    } ?: this

/**
 * The base [Layout] for all top app bars. This function lays out a top app bar navigation icon
 * (leading icon), a title (header), and action icons (trailing icons). Note that the navigation and
 * the actions are optional.
 *
 * @param modifier a [Modifier]
 * @param scrolledOffset a [FloatProducer] that provides the app bar offset in pixels (note that
 *   when the app bar is scrolled, the lambda will output negative values)
 * @param navigationIconContentColor the content color that will be applied via a
 *   [LocalContentColor] when composing the navigation icon
 * @param titleContentColor the color that will be applied via a [LocalContentColor] when composing
 *   the title
 * @param subtitleContentColor the color that will be applied via a [LocalContentColor] when
 *   composing the subtitle
 * @param actionIconContentColor the content color that will be applied via a [LocalContentColor]
 *   when composing the action icons
 * @param title the top app bar title (header)
 * @param titleTextStyle the title's text style
 * @param titleAlpha the title's alpha
 * @param titleVerticalArrangement the title's vertical arrangement
 * @param titleHorizontalAlignment the title's horizontal alignment
 * @param titleBottomPadding the title's bottom padding
 * @param hideTitleSemantics hides the title node from the semantic tree. Apply this boolean when
 *   this layout is part of a [TwoRowsTopAppBar] to hide the title's semantics from accessibility
 *   services. This is needed to avoid having multiple titles visible to accessibility services at
 *   the same time, when animating between collapsed / expanded states.
 * @param navigationIcon a navigation icon [Composable]
 * @param actions actions [Composable]
 * @param height this app bar's requested height
 * @param contentPadding the padding applied to the content of this TopAppBar.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopAppBarLayout(
    modifier: Modifier,
    scrolledOffset: FloatProducer,
    navigationIconContentColor: Color,
    titleContentColor: Color,
    subtitleContentColor: Color,
    actionIconContentColor: Color,
    title: @Composable () -> Unit,
    titleTextStyle: TextStyle,
    subtitle: (@Composable () -> Unit)?,
    subtitleTextStyle: TextStyle,
    titleAlpha: () -> Float,
    titleVerticalArrangement: Arrangement.Vertical,
    titleHorizontalAlignment: Alignment.Horizontal,
    titleBottomPadding: Int,
    hideTitleSemantics: Boolean,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    height: Dp,
    contentPadding: PaddingValues,
) {
    Layout(
        {
            Box(Modifier.layoutId("navigationIcon").padding(start = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides navigationIconContentColor,
                    content = navigationIcon,
                )
            }
            if (subtitle != null) {
                Column(
                    modifier =
                        Modifier.layoutId("title")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .then(
                                if (hideTitleSemantics) Modifier.clearAndSetSemantics {}
                                else Modifier
                            )
                            .graphicsLayer { alpha = titleAlpha() },
                    horizontalAlignment = titleHorizontalAlignment,
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = titleTextStyle,
                        content = title,
                    )
                    ProvideContentColorTextStyle(
                        contentColor = subtitleContentColor,
                        textStyle = subtitleTextStyle,
                        content = subtitle,
                    )
                }
            } else { // TODO(b/352770398): Workaround to maintain compatibility
                Box(
                    modifier =
                        Modifier.layoutId("title")
                            .padding(horizontal = TopAppBarHorizontalPadding)
                            .then(
                                if (hideTitleSemantics) Modifier.clearAndSetSemantics {}
                                else Modifier
                            )
                            .graphicsLayer { alpha = titleAlpha() }
                ) {
                    ProvideContentColorTextStyle(
                        contentColor = titleContentColor,
                        textStyle = titleTextStyle,
                        content = title,
                    )
                }
            }
            Box(Modifier.layoutId("actionIcons").padding(end = TopAppBarHorizontalPadding)) {
                CompositionLocalProvider(
                    LocalContentColor provides actionIconContentColor,
                    content = actions,
                )
            }
        },
        modifier = modifier,
        measurePolicy =
            remember(
                scrolledOffset,
                titleVerticalArrangement,
                titleHorizontalAlignment,
                titleBottomPadding,
                height,
            ) {
                TopAppBarMeasurePolicy(
                    scrolledOffset,
                    titleVerticalArrangement,
                    titleHorizontalAlignment,
                    titleBottomPadding,
                    height,
                    contentPadding,
                )
            },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class TopAppBarMeasurePolicy(
    val scrolledOffset: FloatProducer,
    val titleVerticalArrangement: Arrangement.Vertical,
    val titleHorizontalAlignment: Alignment.Horizontal,
    val titleBottomPadding: Int,
    val height: Dp,
    val contentPadding: PaddingValues,
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val navigationIconPlaceable =
            measurables
                .fastFirst { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0))
        val actionIconsPlaceable =
            measurables
                .fastFirst { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0))
        val startPaddingPx = contentPadding.calculateStartPadding(layoutDirection)
        val endPaddingPx = contentPadding.calculateEndPadding(layoutDirection)
        val maxTitleWidth =
            if (constraints.maxWidth == Constraints.Infinity) {
                constraints.maxWidth
            } else {
                (constraints.maxWidth -
                        navigationIconPlaceable.width -
                        actionIconsPlaceable.width -
                        startPaddingPx.roundToPx() -
                        endPaddingPx.roundToPx())
                    .coerceAtLeast(0)
            }
        val titlePlaceable =
            measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = maxTitleWidth))

        // Locate the title's baseline.
        val titleBaseline =
            if (titlePlaceable[LastBaseline] != AlignmentLine.Unspecified) {
                titlePlaceable[LastBaseline]
            } else {
                0
            }

        // Subtract the scrolledOffset from the maxHeight. The scrolledOffset is expected to be
        // equal or smaller than zero.
        val scrolledOffsetValue = scrolledOffset()
        val heightOffset = if (scrolledOffsetValue.isNaN()) 0 else scrolledOffsetValue.roundToInt()
        val topPaddingPx = contentPadding.calculateTopPadding().roundToPx()
        val bottomPaddingPx = contentPadding.calculateBottomPadding().roundToPx()
        val maxLayoutHeight =
            max(height.roundToPx(), titlePlaceable.height) + topPaddingPx + bottomPaddingPx
        val layoutHeight =
            if (constraints.maxHeight == Constraints.Infinity) {
                maxLayoutHeight
            } else {
                (maxLayoutHeight + heightOffset).coerceAtLeast(0)
            }

        return placeTopAppBar(
            constraints,
            layoutHeight,
            maxLayoutHeight,
            navigationIconPlaceable,
            titlePlaceable,
            actionIconsPlaceable,
            titleBaseline,
            contentPadding,
        )
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.minIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.minIntrinsicHeight(width) } ?: 0,
        )
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ) = measurables.fastSumBy { it.maxIntrinsicWidth(height) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        return max(
            height.roundToPx(),
            measurables.fastMaxOfOrNull { it.maxIntrinsicHeight(width) } ?: 0,
        )
    }

    private fun MeasureScope.placeTopAppBar(
        constraints: Constraints,
        layoutHeight: Int,
        maxLayoutHeight: Int,
        navigationIconPlaceable: Placeable,
        titlePlaceable: Placeable,
        actionIconsPlaceable: Placeable,
        titleBaseline: Int,
        contentPadding: PaddingValues,
    ): MeasureResult {
        val topPadding = contentPadding.calculateTopPadding().roundToPx()
        val bottomPadding = contentPadding.calculateBottomPadding().roundToPx()
        val startPadding = contentPadding.calculateStartPadding(layoutDirection).roundToPx()
        val endPadding = contentPadding.calculateEndPadding(layoutDirection).roundToPx()
        val contentHeight = layoutHeight + topPadding - bottomPadding
        return layout(constraints.maxWidth, layoutHeight) {
            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = startPadding,
                y = (contentHeight - navigationIconPlaceable.height) / 2,
            )

            titlePlaceable.let {
                val start = max(TopAppBarTitleInset.roundToPx(), navigationIconPlaceable.width)
                val end = actionIconsPlaceable.width

                // Align using the maxWidth. We will adjust the position later according to the
                // start and end. This is done to ensure that a center alignment is still maintained
                // when the start and end have different widths. Note that the title is centered
                // relative to the entire app bar width, and not just centered between the
                // navigation icon and the actions.
                var titleX =
                    titleHorizontalAlignment.align(
                        size = titlePlaceable.width,
                        space = constraints.maxWidth,
                        // Using Ltr as we call placeRelative later on.
                        layoutDirection = LayoutDirection.Ltr,
                    )
                // Reposition the title based on the start and the end (i.e. the navigation and
                // action widths).
                if (titleX < start) {
                    titleX += startPadding + (start - titleX)
                } else if (titleX + titlePlaceable.width > constraints.maxWidth - end) {
                    titleX +=
                        startPadding +
                            ((constraints.maxWidth - end) - (titleX + titlePlaceable.width))
                }

                // The titleVerticalArrangement is always one of Center or Bottom.
                val titleY =
                    when (titleVerticalArrangement) {
                        Arrangement.Center -> (contentHeight - titlePlaceable.height) / 2
                        // Apply bottom padding from the title's baseline only when the Arrangement
                        // is "Bottom".
                        Arrangement.Bottom ->
                            if (titleBottomPadding == 0) {
                                contentHeight - titlePlaceable.height
                            } else {
                                // Calculate the actual padding from the bottom of the title, taking
                                // into account its baseline.
                                val paddingFromBottom =
                                    titleBottomPadding - (titlePlaceable.height - titleBaseline)
                                // Adjust the bottom padding to a smaller number if there is no room
                                // to fit the title.
                                val heightWithPadding = paddingFromBottom + titlePlaceable.height
                                val adjustedBottomPadding =
                                    if (heightWithPadding > maxLayoutHeight) {
                                        paddingFromBottom - (heightWithPadding - maxLayoutHeight)
                                    } else {
                                        paddingFromBottom
                                    }

                                contentHeight -
                                    titlePlaceable.height -
                                    max(0, adjustedBottomPadding)
                            }
                        // Arrangement.Top
                        else -> 0
                    }

                it.placeRelative(titleX, titleY)
            }

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width - endPadding,
                y = (contentHeight - actionIconsPlaceable.height) / 2,
            )
        }
    }
}

/**
 * Returns a [TopAppBarScrollBehavior] that only adjusts its content offset, without adjusting any
 * properties that affect the height of a top app bar.
 *
 * @param state a [TopAppBarState]
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 *   [PinnedScrollBehavior]
 */
@OptIn(ExperimentalMaterial3Api::class)
private class PinnedScrollBehavior(
    override val state: TopAppBarState,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = true
    override val snapAnimationSpec: AnimationSpec<Float>? = null
    override val flingAnimationSpec: DecayAnimationSpec<Float>? = null
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f) {
                    // Reset the total content offset to zero when scrolling all the way down.
                    // This will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
                return super.onPostFling(consumed, available)
            }
        }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will immediately appear when the content is pulled down.
 *
 * @param state a [TopAppBarState]
 * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps to
 *   either fully collapsed or fully extended state when a fling or a drag scrolled it into an
 *   intermediate position
 * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top app
 *   bar when the user flings the app bar itself, or the content below it
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 *   [EnterAlwaysScrollBehavior]
 * @param reverseLayout indicates that this behavior is applied to a scrollable content that has a
 *   reversed direction of scrolling and layout
 */
@OptIn(ExperimentalMaterial3Api::class)
private class EnterAlwaysScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
    val reverseLayout: Boolean = false,
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!canScroll()) return Offset.Zero
                val prevHeightOffset = state.heightOffset
                state.heightOffset += available.y
                // The state's heightOffset is coerce in a minimum value of heightOffsetLimit and a
                // maximum value 0f, so we check if its value was actually changed after the
                // available.y was added to it in order to tell if the top app bar is currently
                // collapsing or expanding.
                // Note that when the content was set with a revered layout, we always return a
                // zero offset.
                return if (!reverseLayout && prevHeightOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                if (!reverseLayout) state.heightOffset += consumed.y
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (
                    available.y > 0f &&
                        (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit)
                ) {
                    // Reset the total content offset to zero when scrolling all the way down.
                    // This will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed +
                    settleAppBar(state, available.y, flingAnimationSpec, snapAnimationSpec)
            }
        }
}

/**
 * A [TopAppBarScrollBehavior] that adjusts its properties to affect the colors and height of a top
 * app bar.
 *
 * A top app bar that is set up with this [TopAppBarScrollBehavior] will immediately collapse when
 * the nested content is pulled up, and will expand back the collapsed area when the content is
 * pulled all the way down.
 *
 * @param state a [TopAppBarState]
 * @param snapAnimationSpec an optional [AnimationSpec] that defines how the top app bar snaps to
 *   either fully collapsed or fully extended state when a fling or a drag scrolled it into an
 *   intermediate position
 * @param flingAnimationSpec an optional [DecayAnimationSpec] that defined how to fling the top app
 *   bar when the user flings the app bar itself, or the content below it
 * @param canScroll a callback used to determine whether scroll events are to be handled by this
 *   [ExitUntilCollapsedScrollBehavior]
 */
@OptIn(ExperimentalMaterial3Api::class)
private class ExitUntilCollapsedScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Don't intercept if scrolling down.
                if (!canScroll() || available.y > 0f) return Offset.Zero

                val prevHeightOffset = state.heightOffset
                state.heightOffset = state.heightOffset + available.y
                return if (prevHeightOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y

                if (available.y < 0f || consumed.y < 0f) {
                    // When scrolling up, just update the state's height offset.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset = state.heightOffset + consumed.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }

                if (available.y > 0f) {
                    // Adjust the height offset in case the consumed delta Y is less than what was
                    // recorded as available delta Y in the pre-scroll.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset = state.heightOffset + available.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0) {
                    // Reset the total content offset to zero when scrolling all the way down. This
                    // will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed +
                    settleAppBar(state, available.y, flingAnimationSpec, snapAnimationSpec)
            }
        }
}

/**
 * Settles the app bar by flinging, in case the given velocity is greater than zero, and snapping
 * after the fling settles.
 */
@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?,
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = velocity).animateDecay(
            flingAnimationSpec
        ) {
            val delta = value - lastValue
            val initialHeightOffset = state.heightOffset
            state.heightOffset = initialHeightOffset + delta
            val consumed = abs(initialHeightOffset - state.heightOffset)
            lastValue = value
            remainingVelocity = this.velocity
            // avoid rounding errors and stop if anything is unconsumed
            if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
        }
    }
    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 && state.heightOffset > state.heightOffsetLimit) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec,
            ) {
                state.heightOffset = value
            }
        }
    }

    return Velocity(0f, remainingVelocity)
}

/** Returns the current accessibility touch exploration service [State]. */
@Composable
private fun rememberTouchExplorationService() =
    rememberAccessibilityServiceState(
        listenToTouchExplorationState = true,
        listenToSwitchAccessState = false,
        listenToVoiceAccessState = false,
    )

// An easing function used to compute the alpha value that is applied to the top title part of a
// Medium or Large app bar.
/*@VisibleForTesting*/
internal val TopTitleAlphaEasing = CubicBezierEasing(.8f, 0f, .8f, .15f)

private val MediumTitleBottomPadding = 24.dp
private val LargeTitleBottomPadding = 28.dp
private val TopAppBarHorizontalPadding = 4.dp

// A title inset when the App-Bar is a Medium or Large one. Also used to size a spacer when the
// navigation icon is missing.
private val TopAppBarTitleInset = 16.dp - TopAppBarHorizontalPadding
