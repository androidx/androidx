/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive.navigationsuite

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationItemColors
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailItemDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.window.core.layout.WindowSizeClass

/** Possible values of [NavigationSuiteScaffoldState]. */
enum class NavigationSuiteScaffoldValue {
    /** The state of the navigation component of the scaffold when it's visible. */
    Visible,

    /** The state of the navigation component of the scaffold when it's hidden. */
    Hidden,
}

/**
 * A state object that can be hoisted to observe the navigation suite scaffold state. It allows for
 * setting its navigation component to be hidden or displayed.
 *
 * @see rememberNavigationSuiteScaffoldState to construct the default implementation.
 */
@Stable
interface NavigationSuiteScaffoldState {
    /** Whether the state is currently animating. */
    val isAnimating: Boolean

    /** Whether the navigation component is going to be shown or hidden. */
    val targetValue: NavigationSuiteScaffoldValue

    /** Whether the navigation component is currently shown or hidden. */
    val currentValue: NavigationSuiteScaffoldValue

    /** Hide the navigation component with animation and suspend until it fully expands. */
    suspend fun hide()

    /** Show the navigation component with animation and suspend until it fully expands. */
    suspend fun show()

    /**
     * Hide the navigation component with animation if it's shown, or collapse it otherwise, and
     * suspend until it fully expands.
     */
    suspend fun toggle()

    /**
     * Set the state without any animation and suspend until it's set.
     *
     * @param targetValue the value to set to
     */
    suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue)
}

/** Create and [remember] a [NavigationSuiteScaffoldState] */
@Composable
fun rememberNavigationSuiteScaffoldState(
    initialValue: NavigationSuiteScaffoldValue = NavigationSuiteScaffoldValue.Visible
): NavigationSuiteScaffoldState {
    return rememberSaveable(saver = NavigationSuiteScaffoldStateImpl.Saver()) {
        NavigationSuiteScaffoldStateImpl(initialValue = initialValue)
    }
}

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * The navigation component can be animated to be hidden or shown via a
 * [NavigationSuiteScaffoldState].
 *
 * The scaffold also supports an optional primary action composable, such as a floating action
 * button, which will be displayed according to the current [NavigationSuiteType].
 *
 * A simple usage example looks like this:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldSample
 *
 * An usage with custom layout choices looks like this:
 *
 * @sample androidx.compose.material3.adaptive.navigationsuite.samples.NavigationSuiteScaffoldCustomConfigSample
 * @param navigationItems the navigation items to be displayed, typically [NavigationSuiteItem]s
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param navigationSuiteType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param navigationItemVerticalArrangement the vertical arrangement of the items inside vertical
 *   navigation components (such as the types [NavigationSuiteType.WideNavigationRailCollapsed] and
 *   [NavigationSuiteType.WideNavigationRailExpanded]). It's recommended to use [Arrangement.Top],
 *   [Arrangement.Center], or [Arrangement.Bottom]. Defaults to [Arrangement.Top]
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as part of their header , and above horizontal navigation
 *   components.
 * @param primaryActionContentHorizontalAlignment The horizontal alignment of the primary action
 *   content, if present, when it's displayed along with a horizontal navigation component.
 * @param content the content of your screen
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@Composable
fun NavigationSuiteScaffold(
    navigationItems: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationSuiteType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(WindowAdaptiveInfoDefault),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    navigationItemVerticalArrangement: Arrangement.Vertical =
        NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit,
) {
    val scope =
        NavigationSuiteScaffoldWithPrimaryActionOverrideScope(
            navigationItems = navigationItems,
            modifier = modifier,
            navigationSuiteType = navigationSuiteType,
            navigationSuiteColors = navigationSuiteColors,
            containerColor = containerColor,
            contentColor = contentColor,
            state = state,
            navigationItemVerticalArrangement = navigationItemVerticalArrangement,
            primaryActionContent = primaryActionContent,
            primaryActionContentHorizontalAlignment = primaryActionContentHorizontalAlignment,
            content = content,
        )
    with(LocalNavigationSuiteScaffoldWithPrimaryActionOverride.current) {
        scope.NavigationSuiteScaffoldWithPrimaryAction()
    }
}

/**
 * This override provides the default behavior of the [NavigationSuiteScaffold] with primary action
 * content. This implementation is used when no override is specified.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
object DefaultNavigationSuiteScaffoldWithPrimaryActionOverride :
    NavigationSuiteScaffoldWithPrimaryActionOverride {
    @Composable
    override fun NavigationSuiteScaffoldWithPrimaryActionOverrideScope
        .NavigationSuiteScaffoldWithPrimaryAction() {
        Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationSuite(
                        navigationSuiteType = navigationSuiteType,
                        colors = navigationSuiteColors,
                        primaryActionContent = primaryActionContent,
                        verticalArrangement = navigationItemVerticalArrangement,
                        content = navigationItems,
                    )
                },
                navigationSuiteType = navigationSuiteType,
                state = state,
                primaryActionContent = primaryActionContent,
                primaryActionContentHorizontalAlignment = primaryActionContentHorizontalAlignment,
                content = {
                    Box(
                        Modifier.navigationSuiteScaffoldConsumeWindowInsets(
                            navigationSuiteType,
                            state,
                        )
                    ) {
                        content()
                    }
                },
            )
        }
    }
}

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * Note: It is recommended to use the [NavigationSuiteScaffold] function with the navigationItems
 * param that accepts [NavigationSuiteItem]s instead of this one.
 *
 * The navigation component can be animated to be hidden or shown via a
 * [NavigationSuiteScaffoldState].
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param content the content of your screen
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit = {},
) {
    with(LocalNavigationSuiteScaffoldOverride.current) {
        NavigationSuiteScaffoldOverrideScope(
                navigationSuiteItems = navigationSuiteItems,
                modifier = modifier,
                layoutType = layoutType,
                navigationSuiteColors = navigationSuiteColors,
                containerColor = containerColor,
                contentColor = contentColor,
                state = state,
                content = content,
            )
            .NavigationSuiteScaffold()
    }
}

/**
 * This override provides the default behavior of the [NavigationSuiteScaffold] component.
 *
 * [NavigationSuiteScaffoldOverride] used when no override is specified.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
object DefaultNavigationSuiteScaffoldOverride : NavigationSuiteScaffoldOverride {
    @Composable
    override fun NavigationSuiteScaffoldOverrideScope.NavigationSuiteScaffold() {
        Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
            NavigationSuiteScaffoldLayout(
                navigationSuite = {
                    NavigationSuite(
                        layoutType = layoutType,
                        colors = navigationSuiteColors,
                        content = navigationSuiteItems,
                    )
                },
                state = state,
                layoutType = layoutType,
                content = {
                    Box(
                        Modifier.consumeWindowInsets(
                            if (
                                state.currentValue == NavigationSuiteScaffoldValue.Hidden &&
                                    !state.isAnimating
                            ) {
                                NoWindowInsets
                            } else {
                                when (layoutType) {
                                    NavigationSuiteType.NavigationBar ->
                                        NavigationBarDefaults.windowInsets.only(
                                            WindowInsetsSides.Bottom
                                        )
                                    NavigationSuiteType.NavigationRail ->
                                        NavigationRailDefaults.windowInsets.only(
                                            WindowInsetsSides.Start
                                        )
                                    NavigationSuiteType.NavigationDrawer ->
                                        DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
                                    else -> NoWindowInsets
                                }
                            }
                        )
                    ) {
                        content()
                    }
                },
            )
        }
    }
}

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param content the content of your screen
 */
@Deprecated(
    message = "Deprecated in favor of NavigationSuiteScaffold with state parameter",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    navigationSuiteColors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    containerColor: Color = NavigationSuiteScaffoldDefaults.containerColor,
    contentColor: Color = NavigationSuiteScaffoldDefaults.contentColor,
    content: @Composable () -> Unit = {},
) =
    NavigationSuiteScaffold(
        navigationSuiteItems = navigationSuiteItems,
        modifier = modifier,
        state = rememberNavigationSuiteScaffoldState(),
        layoutType = layoutType,
        navigationSuiteColors = navigationSuiteColors,
        containerColor = containerColor,
        contentColor = contentColor,
        content = content,
    )

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite], and the [primaryActionContent], if any, according to the current
 * [NavigationSuiteType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. An usage example of using a custom modal wide rail can be
 * found at androidx.compose.material3.demos.NavigationSuiteScaffoldCustomConfigDemo.
 *
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param navigationSuiteType the current [NavigationSuiteType]. Usually
 *   [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold layout
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as part of their header, and above horizontal navigation
 *   components.
 * @param primaryActionContentHorizontalAlignment The horizontal alignment of the primary action
 *   content, if present, when it's displayed along with a horizontal navigation component.
 * @param content the content of your screen
 */
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    navigationSuiteType: NavigationSuiteType,
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    primaryActionContent: @Composable (() -> Unit) = {},
    primaryActionContentHorizontalAlignment: Alignment.Horizontal =
        NavigationSuiteScaffoldDefaults.primaryActionContentAlignment,
    content: @Composable () -> Unit,
) {
    val animationProgress by
        animateFloatAsState(
            targetValue = if (state.currentValue == NavigationSuiteScaffoldValue.Hidden) 0f else 1f,
            animationSpec = AnimationSpec,
        )

    Layout({
        // Wrap the navigation suite and content composables each in a Box to not propagate the
        // parent's (Surface) min constraints to its children (see b/312664933).
        Box(Modifier.layoutId(NavigationSuiteLayoutIdTag)) { navigationSuite() }
        Box(Modifier.layoutId(PrimaryActionContentLayoutIdTag)) { primaryActionContent() }
        Box(Modifier.layoutId(ContentLayoutIdTag)) { content() }
    }) { measurables, constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        // Find the navigation suite composable through it's layoutId tag
        val navigationPlaceable =
            measurables
                .fastFirst { it.layoutId == NavigationSuiteLayoutIdTag }
                .measure(looseConstraints)
        val primaryActionContentPlaceable =
            measurables
                .fastFirst { it.layoutId == PrimaryActionContentLayoutIdTag }
                .measure(looseConstraints)
        val isNavigationBar = navigationSuiteType.isNavigationBar
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        // Find the content composable through it's layoutId tag.
        val contentPlaceable =
            measurables
                .fastFirst { it.layoutId == ContentLayoutIdTag }
                .measure(
                    if (isNavigationBar) {
                        constraints.copy(
                            minHeight =
                                layoutHeight -
                                    (navigationPlaceable.height * animationProgress).toInt(),
                            maxHeight =
                                layoutHeight -
                                    (navigationPlaceable.height * animationProgress).toInt(),
                        )
                    } else {
                        constraints.copy(
                            minWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt(),
                            maxWidth =
                                layoutWidth -
                                    (navigationPlaceable.width * animationProgress).toInt(),
                        )
                    }
                )

        layout(layoutWidth, layoutHeight) {
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceable.placeRelative(0, 0)
                // Place the navigation component at the bottom of the screen.
                navigationPlaceable.placeRelative(
                    0,
                    layoutHeight - (navigationPlaceable.height * animationProgress).toInt(),
                )
                // Place the primary action content above the navigation component.
                val positionX =
                    if (primaryActionContentHorizontalAlignment == Alignment.Start) {
                        PrimaryActionContentPadding.roundToPx()
                    } else if (
                        primaryActionContentHorizontalAlignment == Alignment.CenterHorizontally
                    ) {
                        (layoutWidth - primaryActionContentPlaceable.width) / 2
                    } else {
                        layoutWidth -
                            primaryActionContentPlaceable.width -
                            PrimaryActionContentPadding.roundToPx()
                    }
                primaryActionContentPlaceable.placeRelative(
                    positionX,
                    layoutHeight -
                        primaryActionContentPlaceable.height -
                        PrimaryActionContentPadding.roundToPx() -
                        (navigationPlaceable.height * animationProgress).toInt(),
                )
            } else {
                // Place the navigation component at the start of the screen.
                navigationPlaceable.placeRelative(
                    (0 - (navigationPlaceable.width * (1f - animationProgress))).toInt(),
                    0,
                )
                // Place content to the side of the navigation component.
                contentPlaceable.placeRelative(
                    (navigationPlaceable.width * animationProgress).toInt(),
                    0,
                )
            }
        }
    }
}

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * Note: It is recommended to use the [NavigationSuiteScaffoldLayout] function with the
 * navigationSuiteType param instead of this one.
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold].
 *
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold layout
 * @param content the content of your screen
 */
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    state: NavigationSuiteScaffoldState = rememberNavigationSuiteScaffoldState(),
    content: @Composable () -> Unit = {},
) {
    NavigationSuiteScaffoldLayout(
        navigationSuite = navigationSuite,
        navigationSuiteType = layoutType,
        state = state,
        content = content,
    )
}

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * The usage of this function is recommended when you need some customization that is not viable via
 * the use of [NavigationSuiteScaffold]. Example usage:
 *
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param content the content of your screen
 */
@Deprecated(
    message = "Deprecated in favor of NavigationSuiteScaffoldLayout with state parameter",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    content: @Composable () -> Unit = {},
) =
    NavigationSuiteScaffoldLayout(
        navigationSuite = navigationSuite,
        navigationSuiteType = layoutType,
        state = rememberNavigationSuiteScaffoldState(),
        content = content,
    )

/**
 * The default Material navigation component according to the current [NavigationSuiteType] to be
 * used with the [NavigationSuiteScaffold].
 *
 * For specifics about each navigation component, see [ShortNavigationBar], [WideNavigationRail],
 * [NavigationRail], and [PermanentDrawerSheet].
 *
 * @param navigationSuiteType the [NavigationSuiteType] of the associated [NavigationSuiteScaffold].
 *   Usually [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param modifier the [Modifier] to be applied to the navigation component
 * @param colors [NavigationSuiteColors] that will be used to determine the container (background)
 *   color of the navigation component and the preferred color for content inside the navigation
 *   component
 * @param verticalArrangement the vertical arrangement of the items inside vertical navigation
 *   components, such as the wide navigation rail. It's recommended to use [Arrangement.Top],
 *   [Arrangement.Center], or [Arrangement.Bottom].
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as their header, and above horizontal navigation
 *   components.
 * @param content the content inside the current navigation component, typically
 *   [NavigationSuiteItem]s
 */
@Composable
fun NavigationSuite(
    navigationSuiteType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    verticalArrangement: Arrangement.Vertical = NavigationSuiteDefaults.verticalArrangement,
    primaryActionContent: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    val movableContent = remember(content) { movableContentOf(content) }
    when (navigationSuiteType) {
        NavigationSuiteType.ShortNavigationBarCompact -> {
            ShortNavigationBar(
                modifier = modifier,
                containerColor = colors.shortNavigationBarContainerColor,
                contentColor = colors.shortNavigationBarContentColor,
                content = movableContent,
            )
        }
        NavigationSuiteType.ShortNavigationBarMedium -> {
            ShortNavigationBar(
                modifier = modifier,
                containerColor = colors.shortNavigationBarContainerColor,
                contentColor = colors.shortNavigationBarContentColor,
                content = movableContent,
            )
        }
        NavigationSuiteType.WideNavigationRailCollapsed -> {
            WideNavigationRail(
                modifier = modifier,
                header = primaryActionContent,
                arrangement = verticalArrangement,
                colors = colors.wideNavigationRailColors,
                content = movableContent,
            )
        }
        NavigationSuiteType.WideNavigationRailExpanded -> {
            WideNavigationRail(
                modifier = modifier,
                header = primaryActionContent,
                state =
                    rememberWideNavigationRailState(
                        initialValue = WideNavigationRailValue.Expanded
                    ),
                arrangement = verticalArrangement,
                colors = colors.wideNavigationRailColors,
                content = movableContent,
            )
        }
        // Note: This function does not support providing a NavigationBar for the
        // NavigationSuiteType.NavigationBar type instead provides a ShortNavigationBar with a
        // taller height so that it is visually the same.
        // It's advised to to use NavigationSuiteType.ShortNavigationBarVerticalItems instead.
        NavigationSuiteType.NavigationBar -> {
            ShortNavigationBar(
                modifier = modifier.heightIn(min = TallNavigationBarHeight),
                containerColor = colors.navigationBarContainerColor,
                contentColor = colors.navigationBarContentColor,
                content = movableContent,
            )
        }
        // It's advised to to use NavigationSuiteType.WideNavigationRail instead of
        // NavigationSuiteType.NavigationRail.
        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                header = { primaryActionContent() },
                containerColor = colors.navigationRailContainerColor,
                contentColor = colors.navigationRailContentColor,
            ) {
                if (
                    verticalArrangement == Arrangement.Center ||
                        verticalArrangement == Arrangement.Bottom
                ) {
                    Spacer(Modifier.weight(1f))
                }
                movableContent()
                if (verticalArrangement == Arrangement.Center) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        // It's advised to to use NavigationSuiteType.WideNavigationRail instead of
        // NavigationSuiteType.NavigationDrawer.
        NavigationSuiteType.NavigationDrawer -> {
            PermanentDrawerSheet(
                modifier = modifier,
                drawerContainerColor = colors.navigationDrawerContainerColor,
                drawerContentColor = colors.navigationDrawerContentColor,
            ) {
                primaryActionContent()
                if (
                    verticalArrangement == Arrangement.Center ||
                        verticalArrangement == Arrangement.Bottom
                ) {
                    Spacer(Modifier.weight(1f))
                }
                movableContent()
                if (verticalArrangement == Arrangement.Center) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * The default Material navigation component according to the current [NavigationSuiteType] to be
 * used with the [NavigationSuiteScaffold].
 *
 * Note: It is recommended to use the [NavigationSuite] function with the navigationSuiteType param
 * and that accepts [NavigationSuiteItem]s instead of this one.
 *
 * For specifics about each navigation component, see [NavigationBar], [NavigationRail], and
 * [PermanentDrawerSheet].
 *
 * @param modifier the [Modifier] to be applied to the navigation component
 * @param layoutType the current [NavigationSuiteType] of the [NavigationSuiteScaffold]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param colors [NavigationSuiteColors] that will be used to determine the container (background)
 *   color of the navigation component and the preferred color for content inside the navigation
 *   component
 * @param content the content inside the current navigation component, typically
 *   [NavigationSuiteScope.item]s
 */
@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: NavigationSuiteScope.() -> Unit,
) {
    val scope by rememberStateOfItems(content)
    // Define defaultItemColors here since we can't set NavigationSuiteDefaults.itemColors() as a
    // default for the colors param of the NavigationSuiteScope.item non-composable function.
    val defaultItemColors = NavigationSuiteDefaults.itemColors()

    when (layoutType) {
        NavigationSuiteType.NavigationBar -> {
            NavigationBar(
                modifier = modifier,
                containerColor = colors.navigationBarContainerColor,
                contentColor = colors.navigationBarContentColor,
            ) {
                scope.itemList.forEach {
                    NavigationBarItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                        enabled = it.enabled,
                        label = it.label,
                        alwaysShowLabel = it.alwaysShowLabel,
                        colors =
                            it.colors?.navigationBarItemColors
                                ?: defaultItemColors.navigationBarItemColors,
                        interactionSource = it.interactionSource,
                    )
                }
            }
        }
        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = colors.navigationRailContainerColor,
                contentColor = colors.navigationRailContentColor,
            ) {
                scope.itemList.forEach {
                    NavigationRailItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = { NavigationItemIcon(icon = it.icon, badge = it.badge) },
                        enabled = it.enabled,
                        label = it.label,
                        alwaysShowLabel = it.alwaysShowLabel,
                        colors =
                            it.colors?.navigationRailItemColors
                                ?: defaultItemColors.navigationRailItemColors,
                        interactionSource = it.interactionSource,
                    )
                }
            }
        }
        NavigationSuiteType.NavigationDrawer -> {
            PermanentDrawerSheet(
                modifier = modifier,
                drawerContainerColor = colors.navigationDrawerContainerColor,
                drawerContentColor = colors.navigationDrawerContentColor,
            ) {
                scope.itemList.forEach {
                    NavigationDrawerItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        badge = it.badge,
                        label = { it.label?.invoke() ?: Text("") },
                        colors =
                            it.colors?.navigationDrawerItemColors
                                ?: defaultItemColors.navigationDrawerItemColors,
                        interactionSource = it.interactionSource,
                    )
                }
            }
        }
        NavigationSuiteType.None -> {
            /* Do nothing. */
        }
        else -> {
            NavigationSuite(
                navigationSuiteType = layoutType,
                modifier = modifier,
                colors = colors,
            ) {
                scope.itemList.forEach {
                    NavigationSuiteItem(
                        isNavigationSuite = true,
                        navigationSuiteType = layoutType,
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        badge = it.badge,
                        enabled = it.enabled,
                        label = it.label,
                        navigationSuiteItemColors =
                            it.colors ?: NavigationSuiteDefaults.itemColors(),
                        navigationItemColors = null,
                        interactionSource = it.interactionSource,
                    )
                }
            }
        }
    }
}

/**
 * The default Material navigation item component according to the current [NavigationSuiteType] to
 * be used with the [NavigationSuite] that accepts this function.
 *
 * For specifics about each navigation component, see [ShortNavigationBarItem],
 * [WideNavigationRailItem], [NavigationRailItem], and [NavigationDrawerItem].
 *
 * @param selected whether this item is selected
 * @param onClick called when this item is clicked
 * @param icon icon for this item, typically an [Icon]
 * @param label the text label for this item
 * @param modifier the [Modifier] to be applied to this item
 * @param navigationSuiteType the current [NavigationSuiteType] of the associated [NavigationSuite].
 *   Defaults to [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param enabled controls the enabled state of this item. When `false`, this component will not
 *   respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services. Note: as of now, for [NavigationDrawerItem], this is always `true`.
 * @param badge optional badge to show on this item
 * @param colors [NavigationItemColors] that will be used to resolve the colors used for this item
 *   in different states. If null, a default Material colors for each specific item will be used.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
 *   preview the item in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 */
@Composable
fun NavigationSuiteItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    navigationSuiteType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(WindowAdaptiveInfoDefault),
    enabled: Boolean = true,
    badge: @Composable (() -> Unit)? = null,
    colors: NavigationItemColors? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    NavigationSuiteItem(
        isNavigationSuite = false,
        navigationSuiteType = navigationSuiteType,
        selected = selected,
        onClick = onClick,
        icon = icon,
        label = label,
        modifier = modifier,
        enabled = enabled,
        badge = badge,
        navigationItemColors = colors,
        navigationSuiteItemColors = null,
        interactionSource = interactionSource,
    )
}

@Composable
private fun NavigationSuiteItem(
    isNavigationSuite: Boolean,
    navigationSuiteType: NavigationSuiteType,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable (() -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
    badge: @Composable (() -> Unit)?,
    navigationItemColors: NavigationItemColors?,
    navigationSuiteItemColors: NavigationSuiteItemColors?,
    interactionSource: MutableInteractionSource?,
) {
    when (navigationSuiteType) {
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium -> {
            val iconPosition =
                if (navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact) {
                    NavigationItemIconPosition.Top
                } else {
                    NavigationItemIconPosition.Start
                }
            ShortNavigationBarItem(
                selected = selected,
                onClick = onClick,
                icon = { NavigationItemIcon(icon = icon, badge = badge) },
                label = label,
                modifier = modifier,
                enabled = enabled,
                iconPosition = iconPosition,
                colors = navigationItemColors ?: ShortNavigationBarItemDefaults.colors(),
                interactionSource = interactionSource,
            )
        }
        NavigationSuiteType.WideNavigationRailCollapsed,
        NavigationSuiteType.WideNavigationRailExpanded -> {
            WideNavigationRailItem(
                railExpanded =
                    navigationSuiteType == NavigationSuiteType.WideNavigationRailExpanded,
                selected = selected,
                onClick = onClick,
                icon = { NavigationItemIcon(icon = icon, badge = badge) },
                label = label,
                modifier = modifier,
                enabled = enabled,
                colors = navigationItemColors ?: WideNavigationRailItemDefaults.colors(),
                interactionSource = interactionSource,
            )
        }
        // Note: This function does not support providing a NavigationBarItem for the
        // NavigationSuiteType.NavigationBar type due to the NavigationBarItem being limited to
        // RowScope. Instead we provide ShortNavigationBarItem with a top padding so that it is
        // visually the same.
        // It's advised to to use NavigationSuiteType.ShortNavigationBarVerticalItems instead.
        NavigationSuiteType.NavigationBar -> {
            val defaultColors =
                navigationSuiteItemColors?.navigationBarItemColors
                    ?: NavigationBarItemDefaults.colors()
            val actualColors =
                if ((!isNavigationSuite && navigationItemColors == null) || isNavigationSuite) {
                    ShortNavigationBarItemDefaults.colors(
                        selectedIconColor = defaultColors.selectedIconColor,
                        selectedTextColor = defaultColors.selectedTextColor,
                        selectedIndicatorColor = defaultColors.selectedIndicatorColor,
                        unselectedIconColor = defaultColors.unselectedIconColor,
                        unselectedTextColor = defaultColors.unselectedTextColor,
                        disabledIconColor = defaultColors.disabledIconColor,
                        disabledTextColor = defaultColors.disabledTextColor,
                    )
                } else {
                    navigationItemColors!!
                }

            ShortNavigationBarItem(
                selected = selected,
                onClick = onClick,
                icon = { NavigationItemIcon(icon = icon, badge = badge) },
                label = label,
                modifier = modifier.padding(top = 8.dp),
                enabled = enabled,
                colors = actualColors,
                interactionSource = interactionSource,
            )
        }
        // It's advised to to use NavigationSuiteType.WideNavigationRail instead of
        // NavigationSuiteType.NavigationRail.
        NavigationSuiteType.NavigationRail -> {
            val actualColors =
                if (isNavigationSuite) {
                    navigationSuiteItemColors?.navigationRailItemColors
                        ?: NavigationRailItemDefaults.colors()
                } else {
                    if (navigationItemColors != null) {
                        NavigationRailItemDefaults.colors(
                            selectedIconColor = navigationItemColors.selectedIconColor,
                            selectedTextColor = navigationItemColors.selectedTextColor,
                            indicatorColor = navigationItemColors.selectedIndicatorColor,
                            unselectedIconColor = navigationItemColors.unselectedIconColor,
                            unselectedTextColor = navigationItemColors.unselectedTextColor,
                            disabledIconColor = navigationItemColors.disabledIconColor,
                            disabledTextColor = navigationItemColors.disabledTextColor,
                        )
                    } else {
                        NavigationSuiteDefaults.itemColors().navigationRailItemColors
                    }
                }
            NavigationRailItem(
                selected = selected,
                onClick = onClick,
                icon = { NavigationItemIcon(icon = icon, badge = badge) },
                label = label,
                modifier = modifier,
                enabled = enabled,
                colors = actualColors,
                interactionSource = interactionSource,
            )
        }
        // It's advised to to use NavigationSuiteType.WideNavigationRail instead of
        // NavigationSuiteType.NavigationDrawer.
        NavigationSuiteType.NavigationDrawer -> {
            val actualColors =
                if (isNavigationSuite) {
                    navigationSuiteItemColors?.navigationDrawerItemColors
                        ?: NavigationDrawerItemDefaults.colors()
                } else {
                    if (navigationItemColors != null) {
                        NavigationDrawerItemDefaults.colors(
                            selectedIconColor = navigationItemColors.selectedIconColor,
                            selectedTextColor = navigationItemColors.selectedTextColor,
                            unselectedIconColor = navigationItemColors.unselectedIconColor,
                            unselectedTextColor = navigationItemColors.unselectedTextColor,
                            selectedContainerColor = navigationItemColors.selectedIndicatorColor,
                        )
                    } else {
                        NavigationSuiteDefaults.itemColors().navigationDrawerItemColors
                    }
                }

            NavigationDrawerItem(
                modifier = modifier,
                selected = selected,
                onClick = onClick,
                icon = icon,
                badge = badge,
                label = { label?.invoke() ?: Text("") },
                colors = actualColors,
                interactionSource = interactionSource,
            )
        }
    }
}

/** The scope associated with the [NavigationSuiteScope]. */
sealed interface NavigationSuiteScope {

    /**
     * This function sets the parameters of the default Material navigation item to be used with the
     * Navigation Suite Scaffold. The item is called in [NavigationSuite], according to the current
     * [NavigationSuiteType].
     *
     * For specifics about each item component, see [NavigationBarItem], [NavigationRailItem], and
     * [NavigationDrawerItem].
     *
     * @param selected whether this item is selected
     * @param onClick called when this item is clicked
     * @param icon icon for this item, typically an [Icon]
     * @param modifier the [Modifier] to be applied to this item
     * @param enabled controls the enabled state of this item. When `false`, this component will not
     *   respond to user input, and it will appear visually disabled and disabled to accessibility
     *   services. Note: as of now, for [NavigationDrawerItem], this is always `true`.
     * @param label the text label for this item
     * @param alwaysShowLabel whether to always show the label for this item. If `false`, the label
     *   will only be shown when this item is selected. Note: for [NavigationDrawerItem] this is
     *   always `true`
     * @param badge optional badge to show on this item
     * @param colors [NavigationSuiteItemColors] that will be used to resolve the colors used for
     *   this item in different states. If null, [NavigationSuiteDefaults.itemColors] will be used.
     * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
     *   emitting [Interaction]s for this item. You can use this to change the item's appearance or
     *   preview the item in different states. Note that if `null` is provided, interactions will
     *   still happen internally.
     */
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null,
        alwaysShowLabel: Boolean = true,
        badge: (@Composable () -> Unit)? = null,
        colors: NavigationSuiteItemColors? = null,
        interactionSource: MutableInteractionSource? = null,
    )
}

/**
 * Class that describes the different navigation suite types of the [NavigationSuiteScaffold].
 *
 * The [NavigationSuiteType] informs the [NavigationSuite] of what navigation component to expect.
 */
@JvmInline
value class NavigationSuiteType private constructor(private val description: String) {
    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a
         * [ShortNavigationBar] with vertical [ShortNavigationBarItem]s that will be displayed at
         * the bottom of the screen.
         *
         * @see [ShortNavigationBar]
         */
        val ShortNavigationBarCompact =
            NavigationSuiteType(description = "ShortNavigationBarCompact")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a
         * [ShortNavigationBar] with horizontal [ShortNavigationBarItem]s that will be displayed at
         * the bottom of the screen.
         *
         * @see [ShortNavigationBar]
         */
        val ShortNavigationBarMedium = NavigationSuiteType(description = "ShortNavigationBarMedium")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a collapsed
         * [WideNavigationRail] that will be displayed at the start of the screen.
         *
         * @see [WideNavigationRail]
         */
        val WideNavigationRailCollapsed =
            NavigationSuiteType(description = "WideNavigationRailCollapsed")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect an expanded
         * [WideNavigationRail] that will be displayed at the start of the screen.
         *
         * @see [WideNavigationRail]
         */
        val WideNavigationRailExpanded =
            NavigationSuiteType(description = "WideNavigationRailExpanded")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationBar]
         * that will be displayed at the bottom of the screen.
         *
         * Note: It's recommended to use [ShortNavigationBarCompact] instead of this layout type.
         *
         * @see [NavigationBar]
         */
        val NavigationBar = NavigationSuiteType(description = "NavigationBar")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationRail]
         * that will be displayed at the start of the screen.
         *
         * Note: It's recommended to use [WideNavigationRailCollapsed] instead of this layout type.
         *
         * @see [NavigationRail]
         */
        val NavigationRail = NavigationSuiteType(description = "NavigationRail")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a
         * [PermanentDrawerSheet] that will be displayed at the start of the screen.
         *
         * Note: It's recommended to use [WideNavigationRailExpanded] instead of this layout type.
         *
         * @see [PermanentDrawerSheet]
         */
        val NavigationDrawer = NavigationSuiteType(description = "NavigationDrawer")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to not display any
         * navigation components on the screen.
         *
         * Note: It's recommended to use [NavigationSuiteScaffoldState] instead of this layout type
         * and set the visibility of the navigation component to hidden.
         */
        val None = NavigationSuiteType(description = "None")
    }
}

/** Contains the default values used by the [NavigationSuiteScaffold]. */
object NavigationSuiteScaffoldDefaults {
    /**
     * Returns the recommended [NavigationSuiteType] according to the provided [WindowAdaptiveInfo],
     * following the Material specifications. Usually used with the [NavigationSuiteScaffold] and
     * related APIs.
     *
     * @param adaptiveInfo the provided [WindowAdaptiveInfo]
     * @see NavigationSuiteScaffold
     */
    fun navigationSuiteType(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType {
        return with(adaptiveInfo) {
            if (windowSizeClass.minWidth == WindowSizeClass.WidthSizeClasses.Compact) {
                NavigationSuiteType.ShortNavigationBarCompact
            } else if (
                windowPosture.isTabletop ||
                    windowSizeClass.minHeight == WindowSizeClass.HeightSizeClasses.Compact
            ) {
                NavigationSuiteType.ShortNavigationBarMedium
            } else {
                NavigationSuiteType.WideNavigationRailCollapsed
            }
        }
    }

    /**
     * Returns the standard [NavigationSuiteType] according to the provided [WindowAdaptiveInfo].
     * Usually used with the [NavigationSuiteScaffold] and related APIs.
     *
     * Note: It's recommended to use [navigationSuiteType] instead of this function, as that one
     * offers extended and preferred types.
     *
     * @param adaptiveInfo the provided [WindowAdaptiveInfo]
     * @see NavigationSuiteScaffold
     * @see navigationSuiteType
     */
    @Suppress("DEPRECATION") // WindowWidthSizeClass deprecated
    fun calculateFromAdaptiveInfo(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType {
        return with(adaptiveInfo) {
            if (
                windowPosture.isTabletop ||
                    windowSizeClass.minHeight == WindowSizeClass.HeightSizeClasses.Compact ||
                    windowSizeClass.minWidth == WindowSizeClass.WidthSizeClasses.Compact
            ) {
                NavigationSuiteType.NavigationBar
            } else {
                NavigationSuiteType.NavigationRail
            }
        }
    }

    /** Default container color for a navigation suite scaffold. */
    val containerColor: Color
        @Composable get() = MaterialTheme.colorScheme.background

    /** Default content color for a navigation suite scaffold. */
    val contentColor: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    /** Default primary action content alignment for a navigation suite scaffold. */
    val primaryActionContentAlignment = Alignment.End
}

/** Contains the default values used by the [NavigationSuite]. */
object NavigationSuiteDefaults {
    /** Default items vertical arrangement for a navigation suite. */
    val verticalArrangement = Arrangement.Top

    /**
     * Creates a [NavigationSuiteColors] with the provided colors for the container color, according
     * to the Material specification.
     *
     * Use [Color.Transparent] for the navigation*ContainerColor to have no color. The
     * navigation*ContentColor will default to either the matching content color for
     * navigation*ContainerColor, or to the current [LocalContentColor] if navigation*ContainerColor
     * is not a color from the theme.
     *
     * @param shortNavigationBarContainerColor the container color for the [ShortNavigationBar]
     * @param shortNavigationBarContentColor the content color for the [ShortNavigationBar]
     * @param wideNavigationRailColors the [WideNavigationRailColors] for the [WideNavigationRail]
     * @param navigationBarContainerColor the default container color for the [NavigationBar]
     * @param navigationBarContentColor the default content color for the [NavigationBar]
     * @param navigationRailContainerColor the default container color for the [NavigationRail]
     * @param navigationRailContentColor the default content color for the [NavigationRail]
     * @param navigationDrawerContainerColor the default container color for the
     *   [PermanentDrawerSheet]
     * @param navigationDrawerContentColor the default content color for the [PermanentDrawerSheet]
     */
    @Composable
    fun colors(
        shortNavigationBarContentColor: Color = ShortNavigationBarDefaults.contentColor,
        shortNavigationBarContainerColor: Color = ShortNavigationBarDefaults.containerColor,
        wideNavigationRailColors: WideNavigationRailColors = WideNavigationRailDefaults.colors(),
        navigationBarContainerColor: Color = NavigationBarDefaults.containerColor,
        navigationBarContentColor: Color = contentColorFor(navigationBarContainerColor),
        navigationRailContainerColor: Color = NavigationRailDefaults.ContainerColor,
        navigationRailContentColor: Color = contentColorFor(navigationRailContainerColor),
        navigationDrawerContainerColor: Color =
            @Suppress("DEPRECATION") DrawerDefaults.containerColor,
        navigationDrawerContentColor: Color = contentColorFor(navigationDrawerContainerColor),
    ): NavigationSuiteColors =
        NavigationSuiteColors(
            navigationDrawerContentColor = navigationDrawerContentColor,
            shortNavigationBarContentColor = shortNavigationBarContentColor,
            shortNavigationBarContainerColor = shortNavigationBarContainerColor,
            wideNavigationRailColors = wideNavigationRailColors,
            navigationBarContainerColor = navigationBarContainerColor,
            navigationBarContentColor = navigationBarContentColor,
            navigationRailContainerColor = navigationRailContainerColor,
            navigationRailContentColor = navigationRailContentColor,
            navigationDrawerContainerColor = navigationDrawerContainerColor,
        )

    /**
     * Creates a [NavigationSuiteColors] with the provided colors for the container color, according
     * to the Material specification.
     *
     * Use [Color.Transparent] for the navigation*ContainerColor to have no color. The
     * navigation*ContentColor will default to either the matching content color for
     * navigation*ContainerColor, or to the current [LocalContentColor] if navigation*ContainerColor
     * is not a color from the theme.
     *
     * @param navigationBarContainerColor the default container color for the [NavigationBar]
     * @param navigationBarContentColor the default content color for the [NavigationBar]
     * @param navigationRailContainerColor the default container color for the [NavigationRail]
     * @param navigationRailContentColor the default content color for the [NavigationRail]
     * @param navigationDrawerContainerColor the default container color for the
     *   [PermanentDrawerSheet]
     * @param navigationDrawerContentColor the default content color for the [PermanentDrawerSheet]
     */
    @Deprecated(
        message =
            "Deprecated in favor of colors with shortNavigationBar*Color and " +
                "wideNavigationRailColors parameters",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun colors(
        navigationBarContainerColor: Color = NavigationBarDefaults.containerColor,
        navigationBarContentColor: Color = contentColorFor(navigationBarContainerColor),
        navigationRailContainerColor: Color = NavigationRailDefaults.ContainerColor,
        navigationRailContentColor: Color = contentColorFor(navigationRailContainerColor),
        navigationDrawerContainerColor: Color =
            @Suppress("DEPRECATION") DrawerDefaults.containerColor,
        navigationDrawerContentColor: Color = contentColorFor(navigationDrawerContainerColor),
    ): NavigationSuiteColors =
        NavigationSuiteColors(
            shortNavigationBarContainerColor = ShortNavigationBarDefaults.containerColor,
            shortNavigationBarContentColor = ShortNavigationBarDefaults.contentColor,
            wideNavigationRailColors = WideNavigationRailDefaults.colors(),
            navigationBarContainerColor = navigationBarContainerColor,
            navigationBarContentColor = navigationBarContentColor,
            navigationRailContainerColor = navigationRailContainerColor,
            navigationRailContentColor = navigationRailContentColor,
            navigationDrawerContainerColor = navigationDrawerContainerColor,
            navigationDrawerContentColor = navigationDrawerContentColor,
        )

    /**
     * Creates a [NavigationSuiteItemColors] with the provided colors for a
     * [NavigationSuiteScope.item].
     *
     * For specifics about each navigation item colors see [NavigationBarItemColors],
     * [NavigationRailItemColors], and [NavigationDrawerItemColors].
     *
     * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
     *   [NavigationBarItem] of the [NavigationSuiteScope.item]
     * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
     *   [NavigationRailItem] of the [NavigationSuiteScope.item]
     * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
     *   [NavigationDrawerItem] of the [NavigationSuiteScope.item]
     */
    @Composable
    fun itemColors(
        navigationBarItemColors: NavigationBarItemColors = NavigationBarItemDefaults.colors(),
        navigationRailItemColors: NavigationRailItemColors = NavigationRailItemDefaults.colors(),
        navigationDrawerItemColors: NavigationDrawerItemColors =
            NavigationDrawerItemDefaults.colors(),
    ): NavigationSuiteItemColors =
        NavigationSuiteItemColors(
            navigationBarItemColors = navigationBarItemColors,
            navigationRailItemColors = navigationRailItemColors,
            navigationDrawerItemColors = navigationDrawerItemColors,
        )
}

/**
 * Represents the colors of a [NavigationSuite].
 *
 * For specifics about each navigation component colors see [NavigationBarDefaults],
 * [NavigationRailDefaults], and [DrawerDefaults].
 *
 * @param shortNavigationBarContainerColor the container color for the [ShortNavigationBar] of the
 *   [NavigationSuite]
 * @param shortNavigationBarContentColor the content color for the [ShortNavigationBar] of the
 *   [NavigationSuite]
 * @param wideNavigationRailColors the [WideNavigationRailColors] for the [WideNavigationRail] of
 *   the [NavigationSuite]
 * @param navigationBarContainerColor the container color for the [NavigationBar] of the
 *   [NavigationSuite]
 * @param navigationBarContentColor the content color for the [NavigationBar] of the
 *   [NavigationSuite]
 * @param navigationRailContainerColor the container color for the [NavigationRail] of the
 *   [NavigationSuite]
 * @param navigationRailContentColor the content color for the [NavigationRail] of the
 *   [NavigationSuite]
 * @param navigationDrawerContainerColor the container color for the [PermanentDrawerSheet] of the
 *   [NavigationSuite]
 * @param navigationDrawerContentColor the content color for the [PermanentDrawerSheet] of the
 *   [NavigationSuite]
 */
class NavigationSuiteColors
internal constructor(
    val shortNavigationBarContainerColor: Color,
    val shortNavigationBarContentColor: Color,
    val wideNavigationRailColors: WideNavigationRailColors,
    val navigationBarContainerColor: Color,
    val navigationBarContentColor: Color,
    val navigationRailContainerColor: Color,
    val navigationRailContentColor: Color,
    val navigationDrawerContainerColor: Color,
    val navigationDrawerContentColor: Color,
)

/**
 * Represents the colors of a [NavigationSuiteScope.item].
 *
 * For specifics about each navigation item colors see [NavigationBarItemColors],
 * [NavigationRailItemColors], and [NavigationDrawerItemColors].
 *
 * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
 *   [NavigationBarItem] of the [NavigationSuiteScope.item]
 * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
 *   [NavigationRailItem] of the [NavigationSuiteScope.item]
 * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
 *   [NavigationDrawerItem] of the [NavigationSuiteScope.item]
 */
class NavigationSuiteItemColors(
    val navigationBarItemColors: NavigationBarItemColors,
    val navigationRailItemColors: NavigationRailItemColors,
    val navigationDrawerItemColors: NavigationDrawerItemColors,
)

internal val WindowAdaptiveInfoDefault
    @Composable get() = currentWindowAdaptiveInfo()

internal val NavigationSuiteScaffoldValue.isVisible
    get() = this == NavigationSuiteScaffoldValue.Visible

internal class NavigationSuiteScaffoldStateImpl(var initialValue: NavigationSuiteScaffoldValue) :
    NavigationSuiteScaffoldState {
    private val internalValue: Float = if (initialValue.isVisible) Visible else Hidden
    private val internalState = Animatable(internalValue, Float.VectorConverter)
    private val _currentVal = derivedStateOf {
        if (internalState.value == Visible) {
            NavigationSuiteScaffoldValue.Visible
        } else {
            NavigationSuiteScaffoldValue.Hidden
        }
    }

    override val isAnimating: Boolean
        get() = internalState.isRunning

    override val targetValue: NavigationSuiteScaffoldValue
        get() =
            if (internalState.targetValue == Visible) {
                NavigationSuiteScaffoldValue.Visible
            } else {
                NavigationSuiteScaffoldValue.Hidden
            }

    override val currentValue: NavigationSuiteScaffoldValue
        get() = _currentVal.value

    override suspend fun hide() {
        internalState.animateTo(targetValue = Hidden, animationSpec = AnimationSpec)
    }

    override suspend fun show() {
        internalState.animateTo(targetValue = Visible, animationSpec = AnimationSpec)
    }

    override suspend fun toggle() {
        internalState.animateTo(
            targetValue = if (targetValue.isVisible) Hidden else Visible,
            animationSpec = AnimationSpec,
        )
    }

    override suspend fun snapTo(targetValue: NavigationSuiteScaffoldValue) {
        val target = if (targetValue.isVisible) Visible else Hidden
        internalState.snapTo(target)
    }

    companion object {
        private const val Hidden = 0f
        private const val Visible = 1f

        /** The default [Saver] implementation for [NavigationSuiteScaffoldState]. */
        fun Saver() =
            Saver<NavigationSuiteScaffoldState, NavigationSuiteScaffoldValue>(
                save = { it.targetValue },
                restore = { NavigationSuiteScaffoldStateImpl(it) },
            )
    }
}

@Composable
private fun Modifier.navigationSuiteScaffoldConsumeWindowInsets(
    navigationSuiteType: NavigationSuiteType,
    state: NavigationSuiteScaffoldState,
): Modifier =
    consumeWindowInsets(
        if (state.currentValue == NavigationSuiteScaffoldValue.Hidden && !state.isAnimating) {
            NoWindowInsets
        } else {
            when (navigationSuiteType) {
                NavigationSuiteType.ShortNavigationBarCompact,
                NavigationSuiteType.ShortNavigationBarMedium ->
                    ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.WideNavigationRailCollapsed,
                NavigationSuiteType.WideNavigationRailExpanded ->
                    WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationBar ->
                    NavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)
                NavigationSuiteType.NavigationRail ->
                    NavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)
                NavigationSuiteType.NavigationDrawer ->
                    DrawerDefaults.windowInsets.only(WindowInsetsSides.Start)
                else -> NoWindowInsets
            }
        }
    )

private val NavigationSuiteType.isNavigationBar
    get() =
        this == NavigationSuiteType.ShortNavigationBarCompact ||
            this == NavigationSuiteType.ShortNavigationBarMedium ||
            this == NavigationSuiteType.NavigationBar

private interface NavigationSuiteItemProvider {
    val itemsCount: Int
    val itemList: MutableVector<NavigationSuiteItem>
}

private class NavigationSuiteItem(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val modifier: Modifier,
    val enabled: Boolean,
    val label: @Composable (() -> Unit)?,
    val alwaysShowLabel: Boolean,
    val badge: (@Composable () -> Unit)?,
    val colors: NavigationSuiteItemColors?,
    val interactionSource: MutableInteractionSource?,
)

private class NavigationSuiteScopeImpl : NavigationSuiteScope, NavigationSuiteItemProvider {

    override fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        label: @Composable (() -> Unit)?,
        alwaysShowLabel: Boolean,
        badge: (@Composable () -> Unit)?,
        colors: NavigationSuiteItemColors?,
        interactionSource: MutableInteractionSource?,
    ) {
        itemList.add(
            NavigationSuiteItem(
                selected = selected,
                onClick = onClick,
                icon = icon,
                modifier = modifier,
                enabled = enabled,
                label = label,
                alwaysShowLabel = alwaysShowLabel,
                badge = badge,
                colors = colors,
                interactionSource = interactionSource,
            )
        )
    }

    override val itemList: MutableVector<NavigationSuiteItem> = mutableVectorOf()

    override val itemsCount: Int
        get() = itemList.size
}

@Composable
private fun rememberStateOfItems(
    content: NavigationSuiteScope.() -> Unit
): State<NavigationSuiteItemProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember { derivedStateOf { NavigationSuiteScopeImpl().apply(latestContent.value) } }
}

@Composable
private fun NavigationItemIcon(
    icon: @Composable () -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    if (badge != null) {
        BadgedBox(badge = { badge.invoke() }) { icon() }
    } else {
        icon()
    }
}

private const val SpringDefaultSpatialDamping = 0.9f
private const val SpringDefaultSpatialStiffness = 700.0f
private const val NavigationSuiteLayoutIdTag = "navigationSuite"
private const val PrimaryActionContentLayoutIdTag = "primaryActionContent"
private const val ContentLayoutIdTag = "content"

private val TallNavigationBarHeight = 80.dp
private val PrimaryActionContentPadding = 16.dp
private val NoWindowInsets = WindowInsets(0, 0, 0, 0)
private val AnimationSpec: SpringSpec<Float> =
    spring(dampingRatio = SpringDefaultSpatialDamping, stiffness = SpringDefaultSpatialStiffness)

/**
 * Interface that allows libraries to override the behavior of the [NavigationSuiteScaffold]
 * component.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalNavigationSuiteScaffoldOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
interface NavigationSuiteScaffoldOverride {
    /** Behavior function that is called by the [NavigationSuiteScaffold] component. */
    @Composable fun NavigationSuiteScaffoldOverrideScope.NavigationSuiteScaffold()
}

/**
 * Parameters available to [NavigationSuiteScaffold].
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param content the content of your screen
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
class NavigationSuiteScaffoldOverrideScope
internal constructor(
    val navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    val modifier: Modifier = Modifier,
    val layoutType: NavigationSuiteType,
    val navigationSuiteColors: NavigationSuiteColors,
    val containerColor: Color,
    val contentColor: Color,
    val state: NavigationSuiteScaffoldState,
    val content: @Composable () -> Unit = {},
)

/** CompositionLocal containing the currently-selected [NavigationSuiteScaffoldOverride]. */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
val LocalNavigationSuiteScaffoldOverride:
    ProvidableCompositionLocal<NavigationSuiteScaffoldOverride> =
    compositionLocalOf {
        DefaultNavigationSuiteScaffoldOverride
    }

/**
 * Interface that allows libraries to override the behavior of the [NavigationSuiteScaffold]
 * component. This is the version where a primary action content is present.
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [NavigationSuiteScaffoldWithPrimaryActionOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
interface NavigationSuiteScaffoldWithPrimaryActionOverride {
    @Composable
    fun NavigationSuiteScaffoldWithPrimaryActionOverrideScope
        .NavigationSuiteScaffoldWithPrimaryAction()
}

/**
 * Parameters available to [NavigationSuiteScaffold] that includes a primary action.
 *
 * @param navigationItems the navigation items to be displayed, typically [NavigationSuiteItem]s
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param navigationSuiteType the current [NavigationSuiteType]. Defaults to
 *   [NavigationSuiteScaffoldDefaults.navigationSuiteType]
 * @param navigationSuiteColors [NavigationSuiteColors] that will be used to determine the container
 *   (background) color of the navigation component and the preferred color for content inside the
 *   navigation component
 * @param containerColor the color used for the background of the navigation suite scaffold,
 *   including the passed [content] composable. Use [Color.Transparent] to have no color
 * @param contentColor the preferred color to be used for typography and iconography within the
 *   passed in [content] lambda inside the navigation suite scaffold.
 * @param state the [NavigationSuiteScaffoldState] of this navigation suite scaffold
 * @param navigationItemVerticalArrangement the vertical arrangement of the items inside vertical
 *   navigation components (such as the types [NavigationSuiteType.WideNavigationRailCollapsed] and
 *   [NavigationSuiteType.WideNavigationRailExpanded]). It's recommended to use [Arrangement.Top],
 *   [Arrangement.Center], or [Arrangement.Bottom]. Defaults to [Arrangement.Top]
 * @param primaryActionContent The optional primary action content of the navigation suite scaffold,
 *   if any. Typically a [androidx.compose.material3.FloatingActionButton]. It'll be displayed
 *   inside vertical navigation components as part of their header , and above horizontal navigation
 *   components.
 * @param primaryActionContentHorizontalAlignment The horizontal alignment of the primary action
 *   content, if present, when it's displayed along with a horizontal navigation component.
 * @param content the content of your screen
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
class NavigationSuiteScaffoldWithPrimaryActionOverrideScope
internal constructor(
    val navigationItems: @Composable () -> Unit,
    val modifier: Modifier = Modifier,
    val navigationSuiteType: NavigationSuiteType,
    val navigationSuiteColors: NavigationSuiteColors,
    val containerColor: Color,
    val contentColor: Color,
    val state: NavigationSuiteScaffoldState,
    val navigationItemVerticalArrangement: Arrangement.Vertical,
    val primaryActionContent: @Composable (() -> Unit),
    val primaryActionContentHorizontalAlignment: Alignment.Horizontal,
    val content: @Composable () -> Unit,
)

/**
 * CompositionLocal containing the currently-selected
 * [NavigationSuiteScaffoldWithPrimaryActionOverride].
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
val LocalNavigationSuiteScaffoldWithPrimaryActionOverride:
    ProvidableCompositionLocal<NavigationSuiteScaffoldWithPrimaryActionOverride> =
    compositionLocalOf {
        DefaultNavigationSuiteScaffoldWithPrimaryActionOverride
    }
