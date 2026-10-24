/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.material3.integration.testapp.components

import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.xr.compose.platform.LocalSpatialCapabilities

/** Scope for declaring the items of a [NavigationSuiteScaffold]. */
internal interface NavigationSuiteScope {
    /** Adds a navigation item to the navigation suite. */
    fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        label: @Composable (() -> Unit)? = null,
    )
}

/**
 * A navigation suite scaffold that places the appropriate navigation component for
 * [navigationSuiteType] alongside [content].
 *
 * Unlike [androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold], the
 * navigation components are provided by the wrappers in this package ([NavigationBar],
 * [ShortNavigationBar], [NavigationRail], and [WideNavigationRail]), so their Material XR
 * equivalents are used when spatial UI is enabled.
 */
@Composable
internal fun NavigationSuiteScaffold(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    navigationSuiteType: NavigationSuiteType,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (LocalSpatialCapabilities.current.isSpatialUiEnabled) {
        NavigationSuiteLayout(navigationSuiteItems, navigationSuiteType, content)
    } else {
        // When not spatialized set a surface with the background color so that it is not
        // transparent.
        Surface(
            modifier = modifier,
            color = NavigationSuiteScaffoldDefaults.containerColor,
        ) {
            NavigationSuiteLayout(navigationSuiteItems, navigationSuiteType, content)
        }
    }
}

@Composable
internal fun NavigationSuiteLayout(
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    navigationSuiteType: NavigationSuiteType,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            NavigationSuite(
                navigationSuiteType = navigationSuiteType,
                navigationSuiteItems = navigationSuiteItems,
            )
        },
        navigationSuiteType = navigationSuiteType,
        content = content,
    )
}

/**
 * The navigation component for [navigationSuiteType], populated with the items declared in
 * [navigationSuiteItems].
 */
@Composable
internal fun NavigationSuite(
    navigationSuiteType: NavigationSuiteType,
    navigationSuiteItems: NavigationSuiteScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = NavigationSuiteScopeImpl().apply(navigationSuiteItems).items
    when (navigationSuiteType) {
        NavigationSuiteType.NavigationBar ->
            NavigationBar(modifier = modifier) {
                items.forEach {
                    NavigationBarItem(
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        modifier = it.modifier,
                        enabled = it.enabled,
                        label = it.label,
                    )
                }
            }
        NavigationSuiteType.ShortNavigationBarCompact,
        NavigationSuiteType.ShortNavigationBarMedium -> {
            val iconPosition =
                if (navigationSuiteType == NavigationSuiteType.ShortNavigationBarCompact) {
                    NavigationItemIconPosition.Top
                } else {
                    NavigationItemIconPosition.Start
                }
            ShortNavigationBar(modifier = modifier) {
                items.forEach {
                    ShortNavigationBarItem(
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        label = it.label,
                        modifier = it.modifier,
                        enabled = it.enabled,
                        iconPosition = iconPosition,
                    )
                }
            }
        }
        NavigationSuiteType.NavigationRail ->
            NavigationRail(modifier = modifier) {
                items.forEach {
                    NavigationRailItem(
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        modifier = it.modifier,
                        enabled = it.enabled,
                        label = it.label,
                    )
                }
            }
        NavigationSuiteType.WideNavigationRailCollapsed,
        NavigationSuiteType.WideNavigationRailExpanded -> {
            val expanded = navigationSuiteType == NavigationSuiteType.WideNavigationRailExpanded
            // Reset the rail state whenever the collapsed/expanded type changes.
            key(expanded) {
                WideNavigationRail(
                    modifier = modifier,
                    state =
                        rememberWideNavigationRailState(
                            initialValue =
                                if (expanded) {
                                    WideNavigationRailValue.Expanded
                                } else {
                                    WideNavigationRailValue.Collapsed
                                }
                        ),
                ) {
                    items.forEach {
                        WideNavigationRailItem(
                            selected = it.selected,
                            onClick = it.onClick,
                            icon = it.icon,
                            label = it.label,
                            railExpanded = expanded,
                            modifier = it.modifier,
                            enabled = it.enabled,
                        )
                    }
                }
            }
        }
        // There is no Material XR equivalent of the navigation drawer, so always use the Material 3
        // component.
        NavigationSuiteType.NavigationDrawer ->
            PermanentDrawerSheet(modifier = modifier) {
                items.forEach {
                    NavigationDrawerItem(
                        selected = it.selected,
                        onClick = it.onClick,
                        icon = it.icon,
                        modifier = it.modifier,
                        label = { it.label?.invoke() ?: Text("") },
                    )
                }
            }
        else -> {
            /* NavigationSuiteType.None: do nothing. */
        }
    }
}

private class NavigationSuiteItemData(
    val selected: Boolean,
    val onClick: () -> Unit,
    val icon: @Composable () -> Unit,
    val modifier: Modifier,
    val enabled: Boolean,
    val label: @Composable (() -> Unit)?,
)

private class NavigationSuiteScopeImpl : NavigationSuiteScope {
    val items = mutableListOf<NavigationSuiteItemData>()

    override fun item(
        selected: Boolean,
        onClick: () -> Unit,
        icon: @Composable () -> Unit,
        modifier: Modifier,
        enabled: Boolean,
        label: @Composable (() -> Unit)?,
    ) {
        items += NavigationSuiteItemData(selected, onClick, icon, modifier, enabled, label)
    }
}
