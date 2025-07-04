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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navigation
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.ActivityDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

val WearNavDemos =
    DemoCategory(
        "Navigation",
        listOf(
            ActivityDemo("Simple NavHost", SimpleNavHostActivity::class),
            ActivityDemo("Named Argument", NavHostWithNamedArgumentActivity::class),
            ActivityDemo("Deep Link", DeepLinkActivity::class),
        ),
    )

/**
 * This demo demonstrates how a deep link from an external source can create a synthetic back stack
 * for a logical "up" navigation experience.
 *
 * The navigation graph has a 3-level structure: "home" -> "list" -> "detail". The "list" and
 * "detail" screens are defined within a nested navigation graph.
 *
 * By deep linking directly to the "detail" screen, the NavHost automatically adds the start
 * destination of the parent graph ("list") and the root graph ("home") to the back stack first.
 *
 * To test this behavior:
 * 1. Open this demo in the Wear OS demo app.
 * 2. Close the app.
 * 3. Execute the following ADB command:
 * ```
 * adb shell am start -W -a android.intent.action.VIEW \
 *   -d "app://androidx.wear.compose.navigation.demos/detail/123" \
 *   androidx.wear.compose.integration.demos
 * ```
 *
 * The app will open directly on the "Details Screen". When you swipe back, you will navigate to the
 * "List Screen", and swiping back again will take you to the "Home Screen", confirming the
 * synthetic back stack was created correctly.
 */
@Composable
fun NavHostWithDeepLink() {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = "home") {
        composable("home") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ListHeader { Text("Home Screen") }
                CompactChip(
                    onClick = {
                        // To enter a nested graph, navigate to its route.
                        navController.navigate("listGraph")
                    },
                    label = { Text("Browse list") },
                )
            }
        }

        navigation(startDestination = "list", route = "listGraph") {
            composable("list") {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    item { ListHeader { Text("List Screen") } }
                    items(5) { index ->
                        CompactChip(
                            modifier = Modifier.padding(vertical = 4.dp),
                            onClick = {
                                // For in-app navigation, it is best practice to use routes,
                                // not deep link URIs.
                                navController.navigate("detail/$index")
                            },
                            label = {
                                Text(
                                    text = "Item $index",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                        )
                    }
                }
            }

            composable(
                route = "detail/{$DEEP_LINK_ID_ARG}",
                arguments = listOf(navArgument(DEEP_LINK_ID_ARG) { type = NavType.IntType }),
                // The deepLink is now primarily for external entry.
                deepLinks =
                    listOf(navDeepLink { uriPattern = "$DEEP_LINK_URI/detail/{$DEEP_LINK_ID_ARG}" }),
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt(DEEP_LINK_ID_ARG) ?: 0
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    ListHeader { Text("Details Screen") }
                    Text("Item from deep link: $itemId")
                }
            }
        }
    }
}

private const val DEEP_LINK_URI = "app://androidx.wear.compose.navigation.demos"
private const val DEEP_LINK_ID_ARG = "Id"
