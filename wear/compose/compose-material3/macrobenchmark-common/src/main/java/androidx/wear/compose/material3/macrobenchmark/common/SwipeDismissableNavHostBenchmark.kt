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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.util.trace
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.waitForStable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import org.junit.Assert.assertNotNull

val SwipeDismissableNavHostBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val navController = rememberSwipeDismissableNavController()
                Box(
                    modifier = Modifier.semantics { contentDescription = CONTENT_DESCRIPTION_HOST }
                ) {
                    SwipeDismissableNavHost(
                        navController = navController,
                        startDestination = "off",
                    ) {
                        composable("off") {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Button(
                                    modifier =
                                        Modifier.semantics {
                                            contentDescription = CONTENT_DESCRIPTION
                                        },
                                    onClick = { navController.navigate("on") },
                                ) {
                                    Text("On")
                                }
                            }
                        }
                        composable("on") {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Button(onClick = { navController.navigate("off") }) { Text("Off") }
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val startX = 0
                val endX = device.displayWidth * 3 / 4
                val y = device.displayHeight / 2
                val navHost =
                    device.wait(
                        Until.findObject(By.desc(CONTENT_DESCRIPTION_HOST)),
                        FIND_OBJECT_TIMEOUT_MS,
                    )
                assertNotNull(navHost)
                repeat(5) {
                    trace(TRACE_NAVIGATE_TO) {
                        device
                            .wait(
                                Until.findObject(By.desc(CONTENT_DESCRIPTION)),
                                FIND_OBJECT_TIMEOUT_MS,
                            )
                            .click()
                    }
                    navHost.waitForStable(FIND_OBJECT_TIMEOUT_MS, requireStableScreenshot = false)
                    trace(TRACE_SWIPE_BACK) { device.swipe(startX, y, endX, y, 20) }
                    navHost.waitForStable(FIND_OBJECT_TIMEOUT_MS, requireStableScreenshot = false)
                }
            }
    }

internal const val CONTENT_DESCRIPTION_HOST = "host"
internal const val TRACE_NAVIGATE_TO = "navigate_to"
internal const val TRACE_SWIPE_BACK = "swipe_back"
