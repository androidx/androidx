/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.HorizontalPagerScaffoldSample
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy

class HorizontalPagerNavDisplaySampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppScaffold {
                    HorizontalPagerNavDisplaySample { finish() }
                }
            }
        }
    }
}

private object PagerHome

private object PagerScreen

@Composable
fun HorizontalPagerNavDisplaySample(onExit: () -> Unit = {}) {
    val backStack = remember { mutableStateListOf<Any>(PagerHome) }

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<PagerHome> {
                    ScreenScaffold {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("Home Screen")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { backStack.add(PagerScreen) }) {
                                Text("Open Pager")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onExit) {
                                Text("Exit")
                            }
                        }
                    }
                }
                entry<PagerScreen> {
                    HorizontalPagerScaffoldSample(navigateBack = { backStack.removeLastOrNull() })
                }
            },
    )
}
