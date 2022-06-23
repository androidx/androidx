/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.compose.ui

import androidx.camera.integration.uiwidgets.compose.ui.navigation.ComposeCameraNavHost
import androidx.camera.integration.uiwidgets.compose.ui.navigation.ComposeCameraScreen
import androidx.camera.integration.uiwidgets.compose.ui.screen.components.ComposeCameraScreenTabRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun ComposeCameraApp() {
    MaterialTheme {
        val allScreens = ComposeCameraScreen.values().toList()
        val navController = rememberNavController()
        val backstackEntry = navController.currentBackStackEntryAsState()
        val currentScreen = ComposeCameraScreen.fromRoute(
            route = backstackEntry.value?.destination?.route,
            defaultRoute = ComposeCameraScreen.ImageCapture
        )

        Scaffold(
            topBar = {
                ComposeCameraScreenTabRow(
                    allScreens = allScreens,
                    onTabSelected = { screen ->
                        navController.navigate(screen.name)
                    },
                    currentScreen = currentScreen
                )
            }
        ) { innerPadding ->
            ComposeCameraNavHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
