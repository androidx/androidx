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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId

/**
 * The Navigation Suite wraps the provided content and places the adequate provided navigation
 * component on the screen according to the current [NavigationLayoutType].
 *
 * @param adaptiveInfo the current [WindowAdaptiveInfo]
 * @param modifier the [Modifier] to be applied to the navigation suite
 * @param layoutTypeProvider the current [NavigationLayoutTypeProvider]
 * @param navigationComponent the navigation component to be displayed, typically
 * [NavigationSuiteComponent]
 * @param containerColor the color used for the background of the navigation suite. Use
 * [Color.Transparent] to have no color
 * @param contentColor the preferred color for content inside the navigation suite. Defaults to
 * either the matching content color for [containerColor], or to the current [LocalContentColor] if
 * [containerColor] is not a color from the theme
 * @param content the content of your screen
 *
 * TODO: Remove "internal".
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun NavigationSuite(
    adaptiveInfo: WindowAdaptiveInfo,
    modifier: Modifier = Modifier,
    layoutTypeProvider: NavigationLayoutTypeProvider = NavigationSuiteDefaults.layoutTypeProvider,
    navigationComponent: @Composable () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit = {},
) {
    Surface(modifier = modifier, color = containerColor, contentColor = contentColor) {
        NavigationSuiteLayout(
            navigationLayoutType = layoutTypeProvider.calculateFromAdaptiveInfo(adaptiveInfo),
            navigationComponent = navigationComponent,
            content = content
        )
    }
}

/**
 * Layout for a [NavigationSuite]'s content.
 *
 * @param navigationLayoutType the current [NavigationLayoutType] of the [NavigationSuite]
 * @param navigationComponent the navigation component of the [NavigationSuite]
 * @param content the main body of the [NavigationSuite]
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun NavigationSuiteLayout(
    navigationLayoutType: NavigationLayoutType,
    navigationComponent: @Composable () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Layout(
        content = {
            Box(modifier = Modifier.layoutId("navigation")) { navigationComponent() }
            Box(modifier = Modifier.layoutId("content")) { content() }
        }
    ) { measurables, constraints ->
        val navigationPlaceable =
            measurables.first { it.layoutId == "navigation" }.measure(constraints)
        val layoutHeight = constraints.maxHeight
        val layoutWidth = constraints.maxWidth
        val contentPlaceable = measurables.first { it.layoutId == "content" }.measure(
            if (navigationLayoutType.orientation
                == NavigationSuiteFeature.Orientation.Horizontal
            ) {
                constraints.copy(
                    minHeight = layoutHeight - navigationPlaceable.height,
                    maxHeight = layoutHeight - navigationPlaceable.height
                )
            } else {
                constraints.copy(
                    minWidth = layoutWidth - navigationPlaceable.width,
                    maxWidth = layoutWidth - navigationPlaceable.width
                )
            }
        )

        layout(layoutWidth, layoutHeight) {
            when (navigationLayoutType.alignment) {
                // The navigation component can be vertical or horizontal.
                Alignment.TopStart -> {
                    // Place the navigation component at the start of the screen.
                    navigationPlaceable.placeRelative(0, 0)

                    if (navigationLayoutType.orientation
                        == NavigationSuiteFeature.Orientation.Horizontal
                    ) {
                        // Place content below the navigation component.
                        contentPlaceable.placeRelative(0, navigationPlaceable.height)
                    } else {
                        // Place content to the side of the navigation component.
                        contentPlaceable.placeRelative(navigationPlaceable.width, 0)
                    }
                }

                // The navigation component can only be vertical.
                Alignment.TopEnd -> {
                    navigationPlaceable.placeRelative(layoutWidth - navigationPlaceable.width, 0)
                    // Place content at the start of the screen.
                    contentPlaceable.placeRelative(0, 0)
                }

                // The navigation component can only be horizontal.
                Alignment.BottomStart -> {
                    // Place content above the navigation component.
                    contentPlaceable.placeRelative(0, 0)
                    // Place the navigation component at the bottom of the screen.
                    navigationPlaceable.placeRelative(0, layoutHeight - navigationPlaceable.height)
                }

                else -> {
                    // Do nothing if it's not a supported [Alignment].
                }
            }
        }
    }
}

/**
 * The default Material navigation component according to the current [NavigationLayoutType] to be
 * used with the Navigation Suite.
 *
 * For specifics about each navigation component, see [NavigationBar], [NavigationRail], and
 * [PermanentDrawerSheet].
 *
 * @param adaptiveInfo the current [WindowAdaptiveInfo] of the [NavigationSuite]
 * @param modifier the [Modifier] to be applied to the navigation component
 * @params colors [NavigationSuiteColors] that will be used to determine the container (background)
 * color of the navigation component and the preferred color for content inside the navigation
 * component
 * @param layoutTypeProvider the current [NavigationLayoutTypeProvider] of the [NavigationSuite]
 * @param content the content inside the current navigation component, typically
 * [navigationSuiteItem]s
 *
 * TODO: Remove "internal".
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun NavigationSuiteComponent(
    adaptiveInfo: WindowAdaptiveInfo,
    modifier: Modifier = Modifier,
    layoutTypeProvider: NavigationLayoutTypeProvider = NavigationSuiteDefaults.layoutTypeProvider,
    colors: NavigationSuiteColors = NavigationSuiteDefaults.colors(),
    content: NavigationSuiteComponentScope.() -> Unit
) {
    val scope by rememberStateOfItems(content)

    when (layoutTypeProvider.calculateFromAdaptiveInfo(adaptiveInfo)) {
        NavigationLayoutType.NavigationBar -> {
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

        NavigationLayoutType.NavigationRail -> {
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

        NavigationLayoutType.NavigationDrawer -> {
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

/**
 * A feature that describes characteristics of the navigation component of the [NavigationSuite].
 *
 * TODO: Remove "internal".
 */
@ExperimentalMaterial3AdaptiveApi
internal interface NavigationSuiteFeature {
    /**
     * Represents the orientation of the navigation component of the [NavigationSuite].
     */
    enum class Orientation {
        /**
         * The navigation component of the [NavigationSuite] is horizontal, such as the
         * Navigation Bar.
         */
        Horizontal,

        /**
         * The navigation component of the [NavigationSuite] is vertical, such as the
         * Navigation Rail or Navigation Drawer.
         */
        Vertical
    }
}

/**
 * Class that describes the different navigation layout types of the [NavigationSuite].
 *
 * The [NavigationLayoutType] informs the [NavigationSuite] of what navigation component to expect
 * and how to properly place it on the screen in relation to the [NavigationSuite]'s content.
 *
 * @param description the description of the [NavigationLayoutType]
 * @param alignment the [Alignment] of the [NavigationLayoutType] that helps inform how the
 * navigation component will be positioned on the screen. The current supported alignments are:
 * [Alignment.TopStart], [Alignment.BottomStart], and [Alignment.TopEnd]
 * @param orientation the [NavigationSuiteFeature.Orientation] of the [NavigationLayoutType] that
 * helps inform how the navigation component will be positioned on the screen in relation to the
 * content
 *
 * TODO: Make class open instead of internal.
 */
@ExperimentalMaterial3AdaptiveApi
internal class NavigationLayoutType constructor(
    private val description: String,
    // TODO: Make this an internal open val.
    internal val alignment: Alignment,
    // TODO: Make this an internal open val.
    internal val orientation: NavigationSuiteFeature.Orientation
) {
    override fun toString(): String {
        return description
    }

    companion object {
        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a [NavigationBar]
         * and properly place it on the screen.
         *
         * @see NavigationBar
         */
        @JvmField
        val NavigationBar =
            NavigationLayoutType(
                description = "NavigationBar",
                alignment = Alignment.BottomStart,
                orientation = NavigationSuiteFeature.Orientation.Horizontal,
            )

        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a
         * [NavigationRail] and properly place it on the screen.
         *
         * @see NavigationRail
         */
        @JvmField
        val NavigationRail =
            NavigationLayoutType(
                description = "NavigationRail",
                alignment = Alignment.TopStart,
                orientation = NavigationSuiteFeature.Orientation.Vertical,
            )

        /**
         * A navigation layout type that instructs the [NavigationSuite] to expect a
         * [PermanentDrawerSheet] and properly place it on the screen.
         *
         * @see PermanentDrawerSheet
         */
        @JvmField
        val NavigationDrawer =
            NavigationLayoutType(
                description = "NavigationDrawer",
                alignment = Alignment.TopStart,
                orientation = NavigationSuiteFeature.Orientation.Vertical,
            )
    }
}

/**
 * The scope associated with the [NavigationSuiteComponent].
 *
 * TODO: Remove "internal".
 */
internal interface NavigationSuiteComponentScope {
    /**
     * This function sets the parameters of the default Material navigation item to be used with the
     * Navigation Suite. The item is called in [NavigationSuiteComponent], according to the current
     * [NavigationLayoutType].
     */
    fun item(
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
    )
}

/**
 * This function sets the parameters of the default Material navigation item to be used with the
 * Navigation Suite. The item is called in [NavigationSuiteComponent], according to the current
 * [NavigationLayoutType].
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
 *
 * TODO: Remove "internal".
 */
internal fun NavigationSuiteComponentScope.navigationSuiteItem(
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
) {
    item(
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = modifier,
        enabled = enabled,
        label = label,
        badge = badge,
        alwaysShowLabel = alwaysShowLabel,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * A [NavigationLayoutType] provider associated with the [NavigationSuite] and the
 * [NavigationSuiteComponent].
 *
 * TODO: Remove "internal".
 */
internal fun interface NavigationLayoutTypeProvider {

    /**
     * Returns the expected [NavigationLayoutType] according to the provided
     * [WindowAdaptiveInfo].
     *
     * @param adaptiveInfo the provided [WindowAdaptiveInfo]
     */
    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    fun calculateFromAdaptiveInfo(adaptiveInfo: WindowAdaptiveInfo): NavigationLayoutType
}

/**
 * Contains the default values used by the [NavigationSuite].
 *
 * TODO: Remove "internal".
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal object NavigationSuiteDefaults {

    /** The default implementation of the [NavigationLayoutTypeProvider]. */
    val layoutTypeProvider = NavigationLayoutTypeProvider { adaptiveInfo ->
        with(adaptiveInfo) {
            if (posture.isTabletop || windowSizeClass.heightSizeClass == Compact) {
                NavigationLayoutType.NavigationBar
            } else if (windowSizeClass.widthSizeClass == Expanded) {
                NavigationLayoutType.NavigationRail
            } else {
                NavigationLayoutType.NavigationBar
            }
        }
    }

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
 * Represents the colors of a [NavigationSuiteComponent].
 *
 * For specifics about each navigation component colors see [NavigationBarDefaults],
 * [NavigationRailDefaults], and [DrawerDefaults].
 *
 * @param navigationBarContainerColor the container color for the [NavigationBar] of the
 * [NavigationSuiteComponent]
 * @param navigationBarContentColor the content color for the [NavigationBar] of the
 * [NavigationSuiteComponent]
 * @param navigationRailContainerColor the container color for the [NavigationRail] of the
 * [NavigationSuiteComponent]
 * @param navigationRailContentColor the content color for the [NavigationRail] of the
 * [NavigationSuiteComponent]
 * @param navigationDrawerContainerColor the container color for the [PermanentDrawerSheet] of the
 * [NavigationSuiteComponent]
 * @param navigationDrawerContentColor the content color for the [PermanentDrawerSheet] of the
 * [NavigationSuiteComponent]
 *
 * TODO: Remove "internal".
 */
internal class NavigationSuiteColors
internal constructor(
    val navigationBarContainerColor: Color,
    val navigationBarContentColor: Color,
    val navigationRailContainerColor: Color,
    val navigationRailContentColor: Color,
    val navigationDrawerContainerColor: Color,
    val navigationDrawerContentColor: Color
)

/**
 * Represents the colors of a [navigationSuiteItem].
 *
 * For specifics about each navigation item colors see [NavigationBarItemColors],
 * [NavigationRailItemColors], and [NavigationDrawerItemColors].
 *
 * @param navigationBarItemColors the [NavigationBarItemColors] associated with the
 * [NavigationBarItem] of the [navigationSuiteItem]
 * @param navigationRailItemColors the [NavigationRailItemColors] associated with the
 * [NavigationRailItem] of the [navigationSuiteItem]
 * @param navigationDrawerItemColors the [NavigationDrawerItemColors] associated with the
 * [NavigationDrawerItem] of the [navigationSuiteItem]
 *
 * TODO: Remove "internal".
 */
internal class NavigationSuiteItemColors
internal constructor(
    val navigationBarItemColors: NavigationBarItemColors,
    val navigationRailItemColors: NavigationRailItemColors,
    val navigationDrawerItemColors: NavigationDrawerItemColors,
)

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
    val interactionSource: MutableInteractionSource
)

private class NavigationSuiteComponentScopeImpl : NavigationSuiteComponentScope,
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

@Composable
private fun rememberStateOfItems(
    content: NavigationSuiteComponentScope.() -> Unit
): State<NavigationSuiteItemProvider> {
    val latestContent = rememberUpdatedState(content)
    return remember {
        derivedStateOf { NavigationSuiteComponentScopeImpl().apply(latestContent.value) }
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
