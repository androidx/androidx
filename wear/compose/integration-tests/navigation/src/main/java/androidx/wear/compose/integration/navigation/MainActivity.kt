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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.compose.navigation.rememberSwipeDismissableNavHostState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent(parent = null) {
            MaterialTheme {
                val transformingLazyColumnState = rememberTransformingLazyColumnState()
                val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
                val navController = rememberSwipeDismissableNavController()
                SwipeDismissableNavHost(
                    navController = navController,
                    state = rememberSwipeDismissableNavHostState(swipeToDismissBoxState),
                    startDestination = START
                ) {
                    composable(START) {
                        TransformingLazyColumn(
                            state = transformingLazyColumnState,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            item {
                                ListHeader {
                                    Text(text = "Screen 1", color = MaterialTheme.colors.onSurface)
                                }
                            }
                            item {
                                CompactChip(
                                    onClick = { navController.navigate(SCREEN2) },
                                    label = { Text("Next screen") },
                                )
                            }
                            item {
                                CompactChip(
                                    onClick = { navController.navigate(EDGE_SWIPE_SCREEN) },
                                    label = { Text("Screen with edge swipe") },
                                )
                            }
                            item {
                                CompactChip(
                                    onClick = { navController.navigate(PAGER_SCAFFOLD_SCREEN) },
                                    label = { Text("Screen with PagerScaffold") },
                                )
                            }
                            item {
                                CompactChip(
                                    onClick = { navController.navigate(S2R_STANDARD_SCREEN) },
                                    label = { Text("S2R - Standard") },
                                )
                            }
                            item {
                                CompactChip(
                                    onClick = { navController.navigate(S2R_DUAL_DIRECTION_SCREEN) },
                                    label = { Text("S2R - Dual Direction") },
                                )
                            }
                        }
                    }
                    composable(SCREEN2) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(text = "Screen 2", color = MaterialTheme.colors.onSurface)
                            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            CompactChip(
                                onClick = { navController.navigate(SCREEN3) },
                                label = { Text("Click for next screen") },
                            )
                            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            CompactChip(
                                onClick = { navController.popBackStack() },
                                label = { Text("Go Back") },
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
                            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            Text(
                                text = "Swipe right to go back",
                                color = MaterialTheme.colors.onSurface,
                            )
                            Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                            CompactChip(
                                onClick = { navController.popBackStack() },
                                label = { Text("Go Back") },
                            )
                        }
                    }
                    composable(EDGE_SWIPE_SCREEN) {
                        val horizontalScrollState = rememberScrollState(0)
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text(
                                modifier =
                                    Modifier.align(Alignment.Center)
                                        .edgeSwipeToDismiss(swipeToDismissBoxState)
                                        .horizontalScroll(horizontalScrollState),
                                text =
                                    "This text can be scrolled horizontally - " +
                                        "to dismiss, swipe " +
                                        "right from the left edge of the screen" +
                                        " (called Edge Swiping)",
                            )
                        }
                    }
                    composable(PAGER_SCAFFOLD_SCREEN) {
                        AppScaffold {
                            val pagerState = rememberPagerState(pageCount = { 10 })

                            HorizontalPagerScaffold(pagerState = pagerState) { page ->
                                ScreenScaffold {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Page $page")
                                    }
                                }
                            }
                        }
                    }

                    composable(S2R_STANDARD_SCREEN) { SwipeToRevealSingleButtonWithAnchoring() }

                    composable(S2R_DUAL_DIRECTION_SCREEN) {
                        SwipeToRevealBothDirectionsNonAnchoring()
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
private const val PAGER_SCAFFOLD_SCREEN = "pager_scaffold_screen"
private const val S2R_STANDARD_SCREEN = "s2r_standard_screen"
private const val S2R_DUAL_DIRECTION_SCREEN = "s2r_dual_direction_screen"
