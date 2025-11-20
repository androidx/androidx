/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("DEPRECATION") // Suppress for WindowWidthSizeClass

package androidx.compose.material3.demos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ModalWideNavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@Suppress("DEPRECATION") // WindowWidthSizeClass is deprecated
fun NavigationSuiteScaffoldCustomConfigDemo() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    // Custom configuration that shows a wide navigation rail in small/medium width screens, an
    // expanded wide navigation rail in expanded width screens, and a short navigation bar in small
    // height screens.
    val navSuiteType =
        with(currentWindowAdaptiveInfo()) {
            if (
                windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.COMPACT ||
                    windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.MEDIUM
            ) {
                NavigationSuiteType.WideNavigationRailCollapsed
            } else if (windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT) {
                NavigationSuiteType.ShortNavigationBarMedium
            } else if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
                NavigationSuiteType.WideNavigationRailExpanded
            } else {
                NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfo())
            }
        }
    val state = rememberNavigationSuiteScaffoldState()
    val scope = rememberCoroutineScope()
    val railState = rememberWideNavigationRailState()
    val railExpanded = railState.currentValue == WideNavigationRailValue.Expanded
    val isWideNavRailCollapsedType = navSuiteType == NavigationSuiteType.WideNavigationRailCollapsed
    val animateFAB =
        if (
            navSuiteType == NavigationSuiteType.ShortNavigationBarMedium ||
                navSuiteType == NavigationSuiteType.NavigationBar
        ) {
            Modifier.animateFloatingActionButton(
                visible = state.currentValue == NavigationSuiteScaffoldValue.Visible,
                alignment = Alignment.BottomEnd,
            )
        } else {
            Modifier
        }
    val fab =
        @Composable {
            val startPadding =
                if (navSuiteType == NavigationSuiteType.ShortNavigationBarMedium) {
                    0.dp
                } else {
                    24.dp
                }
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(start = startPadding).then(animateFAB),
                onClick = { /* onClick function for FAB. */ },
                expanded =
                    if (isWideNavRailCollapsedType) railExpanded
                    else navSuiteType == NavigationSuiteType.WideNavigationRailExpanded,
                icon = { Icon(Icons.Filled.Add, "FAB") },
                text = { Text("Add new") },
            )
        }
    val menuButton =
        @Composable {
            IconButton(
                modifier =
                    Modifier.padding(start = 24.dp, bottom = 8.dp).semantics {
                        stateDescription = if (railExpanded) "Expanded" else "Collapsed"
                    },
                onClick = { scope.launch { railState.toggle() } },
            ) {
                if (railExpanded) {
                    Icon(Icons.AutoMirrored.Filled.MenuOpen, "Collapse rail")
                } else {
                    Icon(Icons.Filled.Menu, "Expand rail")
                }
            }
        }

    MaterialExpressiveTheme {
        Surface {
            // Use NavigationSuiteScaffoldLayout so that we can customize the NavigationSuite.
            NavigationSuiteScaffoldLayout(
                navigationSuiteType = navSuiteType,
                state = state,
                primaryActionContent = fab,
                navigationSuite = {
                    // Pass in a custom modal rail to substitute the default collapsed wide nav
                    // rail.
                    if (isWideNavRailCollapsedType) {
                        ModalWideNavigationRail(
                            state = railState,
                            header = {
                                Column {
                                    menuButton()
                                    Spacer(Modifier.padding(vertical = 8.dp))
                                    fab()
                                }
                            },
                            expandedHeaderTopPadding = 64.dp,
                        ) {
                            navItems.forEachIndexed { index, navItem ->
                                WideNavigationRailItem(
                                    icon = {
                                        Icon(
                                            if (selectedItem == index) Icons.Filled.Favorite
                                            else Icons.Outlined.FavoriteBorder,
                                            contentDescription = null,
                                        )
                                    },
                                    label = { Text(navItem) },
                                    selected = selectedItem == index,
                                    onClick = { selectedItem = index },
                                    railExpanded = railExpanded,
                                )
                            }
                        }
                    } else {
                        NavigationSuite(
                            navigationSuiteType = navSuiteType,
                            primaryActionContent = fab,
                        ) {
                            navItems.forEachIndexed { index, navItem ->
                                NavigationSuiteItem(
                                    navigationSuiteType = navSuiteType,
                                    icon = {
                                        Icon(
                                            if (selectedItem == index) Icons.Filled.Favorite
                                            else Icons.Outlined.FavoriteBorder,
                                            contentDescription = null,
                                        )
                                    },
                                    label = { Text(navItem) },
                                    selected = selectedItem == index,
                                    onClick = { selectedItem = index },
                                )
                            }
                        }
                    }
                },
            ) {
                // Screen content.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text =
                            "Current NavigationSuiteType: $navSuiteType\n" +
                                "Visibility: ${state.currentValue}",
                        textAlign = TextAlign.Center,
                    )
                    Button(onClick = { scope.launch { state.toggle() } }) {
                        Text("Hide/show navigation component")
                    }
                }
            }
        }
    }
}
