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

package androidx.wear.compose.integration.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.edgeSwipeToDismiss
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent(parent = null) {
            MaterialTheme {
                val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(
                    navController = navController,
                    state = rememberSwipeDismissableNavHostState(swipeToDismissBoxState),
                    startDestination = START
                ) {
                    composable(START) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(text = "Screen 1", color = MaterialTheme.colors.onSurface)
                            CompactChip(
                                onClick = { navController.navigate(SCREEN2) },
                                label = { Text("Next screen") },
                            )
                            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            CompactChip(
                                onClick = { navController.navigate(EDGE_SWIPE_SCREEN) },
                                label = { Text("Screen with edge swipe") },
                            )
                        }
                    }
                    composable(SCREEN2) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(text = "Screen 2", color = MaterialTheme.colors.onSurface)
                            CompactChip(
                                onClick = { navController.navigate(SCREEN3) },
                                label = { Text("Click for next screen") },
                            )
                        }
                    }
                    composable(SCREEN3) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(text = "Screen 3", color = MaterialTheme.colors.onSurface)
                            Text(
                                text = "Swipe right to go back",
                                color = MaterialTheme.colors.onSurface,
                            )
                        }
                    }
                    composable(EDGE_SWIPE_SCREEN) {
                        val horizontalScrollState = rememberScrollState(0)
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                modifier = Modifier.align(Alignment.Center)
                                    .edgeSwipeToDismiss(swipeToDismissBoxState)
                                    .horizontalScroll(horizontalScrollState),
                                text = "This text can be scrolled horizontally - " +
                                    "to dismiss, swipe " +
                                    "right from the left edge of the screen" +
                                    " (called Edge Swiping)",
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val START = "start"
private const val SCREEN2 = "screen2"
private const val SCREEN3 = "screen3"
private const val EDGE_SWIPE_SCREEN = "edge_swipe_screen"
