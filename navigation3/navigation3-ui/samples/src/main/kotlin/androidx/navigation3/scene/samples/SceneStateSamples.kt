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

package androidx.navigation3.scene.samples

import androidx.annotation.Sampled
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Suppress("unused")
@Sampled
@Composable
fun SceneStateSample() {
    val backStack = rememberSaveable { mutableStateListOf("a", "b") }
    val entries =
        rememberDecoratedNavEntries(backStack) { key -> NavEntry(key) { Text("Key = $key") } }
    val sceneState =
        rememberSceneState(
            entries,
            listOf(SinglePaneSceneStrategy()),
            onBack = { backStack.removeLastOrNull() },
        )
    val currentScene = sceneState.currentScene
    val navigationEventState =
        rememberNavigationEventState(
            currentInfo = SceneInfo(currentScene),
            backInfo = sceneState.previousScenes.map { SceneInfo(it) },
        )
    NavigationBackHandler(
        navigationEventState,
        isBackEnabled = currentScene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            // Remove entries from the back stack until we've removed all popped entries
            repeat(entries.size - currentScene.previousEntries.size) {
                backStack.removeLastOrNull()
            }
        },
    )
    NavDisplay(sceneState, navigationEventState)
}
