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

package androidx.navigation3.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.metadata
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.DialogSceneStrategy.Companion.DialogKey
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable private object OverlaidNavKey : NavKey

@Serializable private object DialogNavKey : NavKey

@Composable
@Sampled
fun DialogSample() {
    val backStack = rememberNavBackStack(OverlaidNavKey)
    NavDisplay(
        backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(DialogSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<OverlaidNavKey> {
                    Box(
                        Modifier.fillMaxSize()
                            .background(Color(0.2f, 0.2f, 1.0f, 1.0f))
                            .border(10.dp, Color.Blue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            BasicText(
                                "Overlaid Entry",
                                Modifier.size(50.dp),
                                style = TextStyle(textAlign = TextAlign.Center),
                            )
                            Button(onClick = { backStack.add(DialogNavKey) }) {
                                Text("Open Dialog")
                            }
                        }
                    }
                }
                entry<DialogNavKey>(metadata = metadata { put(DialogKey, DialogProperties()) }) {
                    RedBox("Dialog")
                }
            },
    )
}
