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

package androidx.tv.integration.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val pageColor = Color.Black

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun App() {
    val navController = rememberNavController()
    val initialSelectedTab = Navigation.StandardNavigationDrawer

    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .background(pageColor)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TopNavigation(
                initialSelectedTab = initialSelectedTab,
                updateSelectedTab = {
                    if (it.toRouteValue() != navController.currentDestination?.route) {
                        navController.navigate(it.toRouteValue()) {
                            popUpTo(initialSelectedTab.toRouteValue()) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )

            NavHost(
                navController = navController,
                startDestination = initialSelectedTab.toRouteValue()
            ) {
                Navigation.values().forEach { routeNavigation ->
                    composable(routeNavigation.toRouteValue()) {
                        routeNavigation.action()
                    }
                }
            }
        }
    }
}
