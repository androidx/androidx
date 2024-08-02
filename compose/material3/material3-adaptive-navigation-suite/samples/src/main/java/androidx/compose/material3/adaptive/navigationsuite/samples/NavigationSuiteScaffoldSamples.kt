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

package androidx.compose.material3.adaptive.navigationsuite.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldLayout
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun NavigationSuiteScaffoldSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            navItems.forEachIndexed { index, navItem ->
                item(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = navItem) },
                    label = { Text(navItem) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    ) {
        // Screen content.
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Current NavigationSuiteType: $navSuiteType"
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun NavigationSuiteScaffoldCustomConfigSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    val adaptiveInfo = currentWindowAdaptiveInfo()
    // Custom configuration that shows a navigation drawer in large screens.
    val customNavSuiteType = with(adaptiveInfo) {
        if (windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
            NavigationSuiteType.NavigationDrawer
        } else {
            NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }
    }

    NavigationSuiteScaffold(
        layoutType = customNavSuiteType,
        navigationSuiteItems = {
            navItems.forEachIndexed { index, navItem ->
                item(
                    icon = { Icon(Icons.Filled.Favorite, contentDescription = navItem) },
                    label = { Text(navItem) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    ) {
        // Screen content.
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Current custom NavigationSuiteType: $customNavSuiteType"
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun NavigationSuiteScaffoldCustomNavigationRail() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    val navSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())

    NavigationSuiteScaffoldLayout(
        navigationSuite = {
            // Custom Navigation Rail with centered items.
            if (navSuiteType == NavigationSuiteType.NavigationRail) {
                NavigationRail {
                    // Adding Spacers before and after the item so they are pushed towards the
                    // center of the NavigationRail.
                    Spacer(Modifier.weight(1f))
                    navItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index }
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
            } else {
                NavigationSuite {
                    navItems.forEachIndexed { index, item ->
                        item(
                            icon = { Icon(Icons.Filled.Favorite, contentDescription = item) },
                            label = { Text(item) },
                            selected = selectedItem == index,
                            onClick = { selectedItem = index }
                        )
                    }
                }
            }
        }
    ) {
        // Screen content.
        Text(
            modifier = Modifier.padding(16.dp),
            text = "Current NavigationSuiteType: $navSuiteType"
        )
    }
}
