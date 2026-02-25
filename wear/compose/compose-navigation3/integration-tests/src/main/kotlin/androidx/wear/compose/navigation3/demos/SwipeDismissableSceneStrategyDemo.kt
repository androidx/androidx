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

package androidx.wear.compose.navigation3.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import kotlinx.serialization.Serializable

@Composable
fun SwipeDismissableSceneStrategyDemo(hasBackstack: Boolean, onCheckedChange: () -> Unit) {
    val backStack = rememberNavBackStack(Start)
    val transformingLazyColumnState = rememberTransformingLazyColumnState()

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Start> {
                    TransformingLazyColumn(
                        state = transformingLazyColumnState,
                        modifier =
                            Modifier.then(
                                    if (!hasBackstack) Modifier.background(Color.DarkGray)
                                    else Modifier
                                )
                                .padding(horizontal = 10.dp, vertical = 15.dp),
                    ) {
                        item {
                            ListHeader {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Screen 1", color = MaterialTheme.colors.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Navigation3",
                                        color = MaterialTheme.colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.caption1,
                                    )
                                }
                            }
                        }
                        item {
                            SwitchButton(
                                checked = !hasBackstack,
                                onCheckedChange = { onCheckedChange() },
                            ) {
                                Text("Empty backstack mode")
                            }
                        }
                        item {
                            CompactChip(
                                onClick = { backStack.navigate(Screen2, hasBackstack) },
                                label = { Text("Next screen") },
                            )
                        }
                        item {
                            CompactChip(
                                onClick = { backStack.navigate(PagerScaffoldScreen, hasBackstack) },
                                label = { Text("Screen with PagerScaffold") },
                            )
                        }
                        item {
                            CompactChip(
                                onClick = { backStack.navigate(S2RStandardScreen, hasBackstack) },
                                label = { Text("S2R - Standard") },
                            )
                        }
                        item {
                            CompactChip(
                                onClick = {
                                    backStack.navigate(S2RDualDirectionScreen, hasBackstack)
                                },
                                label = { Text("S2R - Dual Direction") },
                            )
                        }
                    }
                }
                entry<Screen2> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(text = "Screen 2", color = MaterialTheme.colors.onSurface)
                        Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                        CompactChip(
                            onClick = { backStack.navigate(Screen3, hasBackstack) },
                            label = { Text("Click for next screen") },
                        )
                        Spacer(modifier = Modifier.fillMaxWidth().height(4.dp))
                        CompactChip(
                            onClick = { backStack.removeLastOrNull() },
                            label = { Text("Go Back") },
                        )
                    }
                }
                entry<Screen3> {
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
                            onClick = { backStack.removeLastOrNull() },
                            label = { Text("Go Back") },
                        )
                    }
                }
                entry<PagerScaffoldScreen> {
                    AppScaffold {
                        val pagerState = rememberPagerState(pageCount = { 10 })

                        HorizontalPagerScaffold(pagerState = pagerState) {
                            HorizontalPager(
                                state = pagerState,
                                flingBehavior =
                                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(
                                        state = pagerState
                                    ),
                            ) { page ->
                                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                    ScreenScaffold {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("Page $page")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                entry<S2RStandardScreen> { SwipeToRevealSingleButtonWithAnchoring() }

                entry<S2RDualDirectionScreen> { SwipeToRevealBothDirectionsNonAnchoring() }
            },
    )
}

@Serializable object Start : NavKey

@Serializable object Screen2 : NavKey

@Serializable object Screen3 : NavKey

@Serializable object PagerScaffoldScreen : NavKey

@Serializable object S2RStandardScreen : NavKey

@Serializable object S2RDualDirectionScreen : NavKey

private fun NavBackStack<NavKey>.navigate(key: NavKey, hasBackStack: Boolean) {
    if (!hasBackStack) removeAll { it != Start }
    add(key)
}
