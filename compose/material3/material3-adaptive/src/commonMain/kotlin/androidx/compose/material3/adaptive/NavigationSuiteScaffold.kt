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

package androidx.compose.material3.adaptive

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass.Companion.Compact
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Companion.Expanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

/**
 * The Navigation Suite Scaffold wraps the provided content and places the adequate provided
 * navigation component on the screen according to the current [NavigationSuiteType].
 *
 * Example default usage:
 * @sample androidx.compose.material3.adaptive.samples.NavigationSuiteScaffoldSample
 * Example custom configuration usage:
 * @sample androidx.compose.material3.adaptive.samples.NavigationSuiteScaffoldCustomConfigSample
 *
 * @param navigationSuiteItems the navigation items to be displayed
 * @param modifier the [Modifier] to be applied to the navigation suite scaffold
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 * [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param containerColor the color used for the background of the navigation suite scaffold. Use
 * [Color.Transparent] to have no color
 * @param contentColor the preferred color for content inside the navigation suite scaffold.
 * Defaults to either the matching content color for [containerColor], or to the current
 * [LocalContentColor] if [containerColor] is not a color from the theme
 * @param content the content of your screen
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        NavigationSuiteScaffoldLayout(
            navigationSuite = {
                NavigationSuite(layoutType = layoutType, content = navigationSuiteItems)
            },
            layoutType = layoutType,
            content = content
        )
    }
}

/**
 * Layout for a [NavigationSuiteScaffold]'s content. This function wraps the [content] and places
 * the [navigationSuite] component according to the given [layoutType].
 *
 * @param navigationSuite the navigation component to be displayed, typically [NavigationSuite]
 * @param layoutType the current [NavigationSuiteType]. Defaults to
 * [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param content the content of your screen
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun NavigationSuiteScaffoldLayout(
    navigationSuite: @Composable () -> Unit,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    content: @Composable () -> Unit = {}
) {
    Layout(
        contents = listOf(navigationSuite, content)
    ) { (navigationMeasurables, contentMeasurables), constraints ->
        val navigationPlaceables = navigationMeasurables.fastMap { it.measure(constraints) }
        val isNavigationBar = layoutType == NavigationSuiteType.NavigationBar
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        val contentPlaceables = contentMeasurables.fastMap { it.measure(
            if (isNavigationBar) {
                constraints.copy(
                    minHeight = layoutHeight - (navigationPlaceables.fastMaxOfOrNull { it.height }
                        ?: 0),
                    maxHeight = layoutHeight - (navigationPlaceables.fastMaxOfOrNull { it.height }
                        ?: 0)
                )
            } else {
                constraints.copy(
                    minWidth = layoutWidth - (navigationPlaceables.fastMaxOfOrNull { it.width }
                        ?: 0),
                    maxWidth = layoutWidth - (navigationPlaceables.fastMaxOfOrNull { it.width }
                        ?: 0)
                )
            }
        ) }

        layout(layoutWidth, layoutHeight) {
            if (isNavigationBar) {
                // Place content above the navigation component.
                contentPlaceables.fastForEach {
                    it.placeRelative(0, 0)
                }
                // Place the navigation component at the bottom of the screen.
                navigationPlaceables.fastForEach {
                    it.placeRelative(
                        0,
                        layoutHeight - (navigationPlaceables.fastMaxOfOrNull { it.height } ?: 0))
                }
            } else {
                // Place the navigation component at the start of the screen.
                navigationPlaceables.fastForEach {
                    it.placeRelative(0, 0)
                }
                // Place content to the side of the navigation component.
                contentPlaceables.fastForEach {
                    it.placeRelative((navigationPlaceables.fastMaxOfOrNull { it.width } ?: 0), 0)
                }
            }
        }
    }
}

/**
 * The default Material navigation component according to the current [NavigationSuiteType] to be
 * used with the [NavigationSuiteScaffold].
 *
 * For specifics about each navigation component, see [NavigationBar], [NavigationRail], and
 * [PermanentDrawerSheet].
 *
 * @param modifier the [Modifier] to be applied to the navigation component
 * @param layoutType the current [NavigationSuiteType] of the [NavigationSuiteScaffold]. Defaults to
 * [NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo]
 * @param colors [NavigationSuiteColors] that will be used to determine the container (background)
 * color of the navigation component and the preferred color for content inside the navigation
 * component
 * @param content the content inside the current navigation component, typically
 * [NavigationSuiteScope.item]s
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun NavigationSuite(
    modifier: Modifier = Modifier,
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(WindowAdaptiveInfoDefault),
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: NavigationSuiteScope.() -> Unit
) {
    val scope by rememberStateOfItems(content)

    when (layoutType) {
        NavigationSuiteType.NavigationBar -> {
            NavigationBar(
                modifier = modifier,
                containerColor = colors.navigationBarContainerColor,
                contentColor = colors.navigationBarContentColor
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
                        colors = it.colors?.navigationBarItemColors
                            ?: NavigationBarItemDefaults.colors(),
                        interactionSource = it.interactionSource
                    )
                }
            }
        }

        NavigationSuiteType.NavigationRail -> {
            NavigationRail(
                modifier = modifier,
                containerColor = colors.navigationRailContainerColor,
                contentColor = colors.navigationRailContentColor
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
                        colors = it.colors?.navigationRailItemColors
                            ?: NavigationRailItemDefaults.colors(),
                        interactionSource = it.interactionSource
                    )
                }
            }
        }

        NavigationSuiteType.NavigationDrawer -> {
            PermanentDrawerSheet(
                modifier = modifier,
                drawerContainerColor = colors.navigationDrawerContainerColor,
                drawerContentColor = colors.navigationDrawerContentColor
            ) {
                scope.itemList.forEach {
                    NavigationDrawerItem(
                        modifier = it.modifier,
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        badge = it.badge,
                        label = { it.label?.invoke() ?: Text("") },
                        colors = it.colors?.navigationDrawerItemColors
                            ?: NavigationDrawerItemDefaults.colors(),
                        interactionSource = it.interactionSource
                    )
                }
            }
        }
    }
}

/** The scope associated with the [NavigationSuiteScope]. */
@ExperimentalMaterial3AdaptiveApi
interface NavigationSuiteScope {

    /**
     * This function sets the parameters of the default Material navigation item to be used with the
     * Navigation Suite Scaffold. The item is called in [NavigationSuite], according to the
     * current [NavigationSuiteType].
     *
     * For specifics about each item component, see [NavigationBarItem], [NavigationRailItem], and
     * [NavigationDrawerItem].
     *
     * @param selected whether this item is selected
     * @param onClick called when this item is clicked
     * @param icon icon for this item, typically an [Icon]
     * @param modifier the [Modifier] to be applied to this item
     * @param enabled controls the enabled state of this item. When `false`, this component will not
     * respond to user input, and it will appear visually disabled and disabled to accessibility
     * services. Note: as of now, for [NavigationDrawerItem], this is always `true`.
     * @param label the text label for this item
     * @param alwaysShowLabel whether to always show the label for this item. If `false`, the label will
     * only be shown when this item is selected. Note: for [NavigationDrawerItem] this is always `true`
     * @param badge optional badge to show on this item
     * @param colors [NavigationSuiteItemColors] that will be used to resolve the colors used for this
     * item in different states.
     * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
     * for this item. You can create and pass in your own `remember`ed instance to observe
     * [Interaction]s and customize the appearance / behavior of this item in different states
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
        interactionSource: MutableInteractionSource = MutableInteractionSource()
    )
}

/**
 * Class that describes the different navigation suite types of the [NavigationSuiteScaffold].
 *
 * The [NavigationSuiteType] informs the [NavigationSuite] of what navigation component to expect.
 */
@JvmInline
@ExperimentalMaterial3AdaptiveApi
value class NavigationSuiteType private constructor(private val description: String) {
    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationBar]
         * that will be displayed at the bottom of the screen.
         *
         * @see NavigationBar
         */
        val NavigationBar = NavigationSuiteType(description = "NavigationBar")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a [NavigationRail]
         * that will be displayed at the start of the screen.
         *
         * @see NavigationRail
         */
        val NavigationRail = NavigationSuiteType(description = "NavigationRail")

        /**
         * A navigation suite type that instructs the [NavigationSuite] to expect a
         * [PermanentDrawerSheet] that will be displayed at the start of the screen.
         *
         * @see PermanentDrawerSheet
         */
        val NavigationDrawer = NavigationSuiteType(description = "NavigationDrawer")
    }
}

/** Contains the default values used by the [NavigationSuiteScaffold]. */
@ExperimentalMaterial3AdaptiveApi
object NavigationSuiteScaffoldDefaults {
    /**
     * Returns the expected [NavigationSuiteType] according to the provided [WindowAdaptiveInfo].
     * Usually used with the [NavigationSuiteScaffold] and related APIs.
     *
     * @param adaptiveInfo the provided [WindowAdaptiveInfo]
     * @see NavigationSuiteScaffold
     */
    fun calculateFromAdaptiveInfo(adaptiveInfo: WindowAdaptiveInfo): NavigationSuiteType {
        return with(adaptiveInfo) {
            if (posture.isTabletop || windowSizeClass.heightSizeClass == Compact) {
                NavigationSuiteType.NavigationBar
            } else if (windowSizeClass.widthSizeClass == Expanded) {
                NavigationSuiteType.NavigationRail
            } else {
                NavigationSuiteType.NavigationBar
            }
        }
    }
}

/** Contains the default values used by the [NavigationSuite]. */
@ExperimentalMaterial3AdaptiveApi
object NavigationSuiteDefaults {
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
     * [PermanentDrawerSheet]
     * @param navigationDrawerContentColor the default content color for the [PermanentDrawerSheet]
     */
    @Composable
    fun colors(
        navigationBarContainerColor: Color = NavigationBarDefaults.containerColor,
        navigationBarContentColor: Color = contentColorFor(navigationBarContainerColor),
        navigationRailContainerColor: Color = NavigationRailDefaults.ContainerColor,
        navigationRailContentColor: Color = contentColorFor(navigationRailContainerColor),
        navigationDrawerContainerColor: Color = DrawerDefaults.containerColor,
        navigationDrawerContentColor: Color = contentColorFor(navigationDrawerContainerColor),
    ): NavigationSuiteColors =
        NavigationSuiteColors(
            navigationBarContainerColor = navigationBarContainerColor,
            navigationBarContentColor = navigationBarContentColor,
            navigationRailContainerColor = navigationRailContainerColor,
            navigationRailContentColor = navigationRailContentColor,
            navigationDrawerContainerColor = navigationDrawerContainerColor,
            navigationDrawerContentColor = navigationDrawerContentColor
        )
}

/**
 * Represents the colors of a [NavigationSuite].
 *
 * For specifics about each navigation component colors see [NavigationBarDefaults],
 * [NavigationRailDefaults], and [DrawerDefaults].
 *
 * @param navigationBarContainerColor the container color for the [NavigationBar] of the
 * [NavigationSuite]
 * @param navigationBarContentColor the content color for the [NavigationBar] of the
 * [NavigationSuite]
 * @param navigationRailContainerColor the container color for the [NavigationRail] of the
 * [NavigationSuite]
 * @param navigationRailContentColor the content color for the [NavigationRail] of the
 * [NavigationSuite]
 * @param navigationDrawerContainerColor the container color for the [PermanentDrawerSheet] of the
 * [NavigationSuite]
 * @param navigationDrawerContentColor the content color for the [PermanentDrawerSheet] of the
 * [NavigationSuite]
 */
@ExperimentalMaterial3AdaptiveApi
class NavigationSuiteColors
internal constructor(
    val navigationBarContainerColor: Color,
    val navigationBarContentColor: Color,
    val navigationRailContainerColor: Color,
    val navigationRailContentColor: Color,
    val navigationDrawerContainerColor: Color,
    val navigationDrawerContentColor: Color
)

/**
 * Represents the colors of a [NavigationSuiteScope.item].
 *
 * For specifics about each navigation item colors see [NavigationBarItemColors],
 * [NavigationRailItemColors], and [NavigationDrawerItemColors].
 *
 * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
 * [NavigationBarItem] of the [NavigationSuiteScope.item]
 * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
 * [NavigationRailItem] of the [NavigationSuiteScope.item]
 * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
 * [NavigationDrawerItem] of the [NavigationSuiteScope.item]
 */
@ExperimentalMaterial3AdaptiveApi
class NavigationSuiteItemColors
internal constructor(
    val navigationBarItemColors: NavigationBarItemColors,
    val navigationRailItemColors: NavigationRailItemColors,
    val navigationDrawerItemColors: NavigationDrawerItemColors,
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal expect val WindowAdaptiveInfoDefault: WindowAdaptiveInfo
    @Composable
    get

private interface NavigationSuiteItemProvider {
    val itemsCount: Int
    val itemList: MutableVector<NavigationSuiteItem>
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    val interactionSource: MutableInteractionSource
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class NavigationSuiteScopeImpl : NavigationSuiteScope,
    NavigationSuiteItemProvider {

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
        interactionSource: MutableInteractionSource
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
                interactionSource = interactionSource
            )
        )
    }

    override val itemList: MutableVector<NavigationSuiteItem> = mutableVectorOf()

    override val itemsCount: Int
        get() = itemList.size
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun rememberStateOfItems(
    content: NavigationSuiteScope.() -> Unit
): State<NavigationSuiteItemProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { NavigationSuiteScopeImpl().apply(latestContent.value) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationItemIcon(
    icon: @Composable () -> Unit,
    badge: (@Composable () -> Unit)? = null,
) {
    if (badge != null) {
        BadgedBox(badge = { badge.invoke() }) {
            icon()
        }
    } else {
        icon()
    }
}
