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

package androidx.navigation3.scene

import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class SceneStateTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun testSceneStateChanges() {
        lateinit var backStack: MutableList<Any>
        lateinit var sceneState: SceneState<Any>

        rule.setContent {
            backStack = remember { mutableStateListOf(First) }
            val entries =
                rememberDecoratedNavEntries(
                    backStack,
                    emptyList(),
                    entryProvider {
                        entry<First> { Text("First") }
                        entry<Second>(metadata = DialogSceneStrategy.dialog()) { Text("Second") }
                        entry<Third>(metadata = DialogSceneStrategy.dialog()) { Text("Third") }
                    },
                )
            sceneState =
                rememberSceneState(entries, DialogSceneStrategy()) {
                    backStack.removeAt(backStack.lastIndex)
                }
        }

        assertThat(sceneState.currentScene).isInstanceOf<SinglePaneScene<Any>>()
        assertThat(sceneState.previousScenes).isEmpty()
        assertThat(sceneState.overlayScenes).isEmpty()

        rule.runOnIdle { backStack.add(Second) }

        rule.waitForIdle()

        assertThat(sceneState.currentScene).isInstanceOf<SinglePaneScene<Any>>()
        assertThat(sceneState.previousScenes).hasSize(1)
        assertThat(sceneState.overlayScenes).hasSize(1)

        rule.runOnIdle { backStack.add(Third) }

        assertThat(sceneState.currentScene).isInstanceOf<SinglePaneScene<Any>>()
        assertThat(sceneState.previousScenes).hasSize(1)
        assertThat(sceneState.overlayScenes).hasSize(1)
    }
}

object First

object Second

object Third
