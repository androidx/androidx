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

package androidx.compose.material3.adaptive.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.NavigationSuite
import androidx.compose.material3.adaptive.NavigationSuiteAlignment
import androidx.compose.material3.adaptive.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.NavigationSuiteType
import androidx.compose.material3.adaptive.calculateWindowAdaptiveInfo
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun NavigationSuiteScaffoldSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    val navSuiteType =
        NavigationSuiteDefaults.calculateFromAdaptiveInfo(calculateWindowAdaptiveInfo())

    NavigationSuiteScaffold(
        navigationSuite = {
            NavigationSuite {
                navItems.forEachIndexed { index, navItem ->
                    item(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = navItem) },
                        label = { Text(navItem) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Preview
@Sampled
@Composable
fun NavigationSuiteScaffoldCustomConfigSample() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val navItems = listOf("Songs", "Artists", "Playlists")
    val adaptiveInfo = calculateWindowAdaptiveInfo()
    val customNavSuiteType = with(adaptiveInfo) {
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
            NavigationSuiteType.NavigationDrawer
        } else if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            NavigationSuiteType.NavigationRail
        } else {
            NavigationSuiteDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
        }
    }

    // Custom configuration that shows nav rail on end of screen in small screens, and navigation
    // drawer in large screens.
    NavigationSuiteScaffold(
        navigationSuite = {
            NavigationSuite(
                layoutType = customNavSuiteType,
                modifier = if (customNavSuiteType == NavigationSuiteType.NavigationRail) {
                    Modifier.alignment(NavigationSuiteAlignment.EndVertical)
                } else {
                    Modifier
                }
            ) {
                navItems.forEachIndexed { index, navItem ->
                    item(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = navItem) },
                        label = { Text(navItem) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
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
