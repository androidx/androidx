/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose.demos

import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.samples.Dashboard
import androidx.navigation.compose.samples.Dialog
import androidx.navigation.compose.samples.DialogContent
import androidx.navigation.compose.samples.Profile
import androidx.navigation.compose.samples.Scrollable

@Composable
fun BottomBarNavDemo() {
    val navController = rememberNavController()

    val items =
        listOf(
            stringResource(R.string.profile) to Profile,
            stringResource(R.string.dashboard) to Dashboard(),
            stringResource(R.string.scrollable) to Scrollable
        )

    Scaffold(
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry = navController.currentBackStackEntryAsState().value
                val currentDestination = navBackStackEntry?.destination
                items.forEach { (name, destination) ->
                    BottomNavigationItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                        label = { Text(name) },
                        selected =
                            currentDestination?.hierarchy?.any {
                                it.hasRoute(destination::class)
                            } == true,
                        onClick = {
                            navController.navigate(destination) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, Profile, Modifier.padding(innerPadding)) {
            composable<Profile> { Profile(navController) }
            composable<Dashboard> { Dashboard(navController) }
            composable<Scrollable> { Scrollable(navController) }
            dialog<Dialog> { DialogContent(navController) }
        }
    }
}
