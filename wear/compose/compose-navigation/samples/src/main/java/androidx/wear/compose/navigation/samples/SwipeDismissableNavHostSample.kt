/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.navigation.samples

import androidx.annotation.Sampled
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

@Sampled
@Composable
fun SimpleNavHost() {
    // Example of using a NavHost where each destination in the NavGraph has a unique name.
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "off"
    ) {
        composable("off") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Button(onClick = { navController.navigate("on") }) { Text("On") }
            }
        }
        composable("on") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Button(onClick = { navController.navigate("off") }) { Text("Off") }
            }
        }
    }
}

@Sampled
@Composable
fun NavHostWithNamedArgument() {
    // Example of using a NavHost where we pass an argument to a destination in the NavGraph.
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "list"
    ) {
        composable("list") {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                item {
                    ListHeader {
                        Text("List Screen")
                    }
                }
                items(5) { index ->
                    CompactChip(
                        modifier = Modifier.padding(vertical = 4.dp),
                        onClick = { navController.navigate("detail/$index") },
                        label = {
                            Text(
                                text = "Item $index",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize()
                            )
                        })
                }
            }
        }
        composable(
            route = "detail/{Id}",
            arguments = listOf(
                navArgument("Id") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getInt("Id") ?: 0
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ListHeader {
                    Text("Details Screen")
                }
                Text("Item $itemId")
            }
        }
    }
}
