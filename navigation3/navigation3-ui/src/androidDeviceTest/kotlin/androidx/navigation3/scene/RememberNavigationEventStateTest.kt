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

package androidx.navigation3.scene

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigationevent.compose.NavigationEventState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class RememberNavigationEventStateTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testRememberNavigationEventState() {
        val backStack = mutableStateListOf(First, Second)
        lateinit var sceneState: SceneState<Any>
        lateinit var navigationEventState: NavigationEventState<SceneInfo<Any>>

        rule.setContent {
            val entries =
                rememberDecoratedNavEntries(
                    backStack,
                    emptyList(),
                    entryProvider {
                        entry<First> {}
                        entry<Second> {}
                        entry<Third> {}
                    },
                )
            sceneState =
                rememberSceneState(entries, listOf(SinglePaneSceneStrategy())) {
                    backStack.removeAt(backStack.lastIndex)
                }
            navigationEventState = rememberNavigationEventState(sceneState)
        }

        rule.runOnIdle {
            // Check that the NavigationEventState reflects the current scene from SceneState
            assertThat(navigationEventState.currentInfo.scene).isEqualTo(sceneState.currentScene)
            // Check that the NavigationEventState reflects the previous scenes from SceneState
            assertThat(navigationEventState.backInfo).hasSize(1)
            assertThat(navigationEventState.backInfo[0].scene)
                .isEqualTo(sceneState.previousScenes[0])
        }

        // Add a new entry to the backstack and verify it updates
        rule.runOnIdle { backStack.add(Third) }
        rule.waitForIdle()

        rule.runOnIdle {
            assertThat(navigationEventState.currentInfo.scene).isEqualTo(sceneState.currentScene)
            assertThat(navigationEventState.backInfo).hasSize(2)
            assertThat(navigationEventState.backInfo[0].scene)
                .isEqualTo(sceneState.previousScenes[0])
            assertThat(navigationEventState.backInfo[1].scene)
                .isEqualTo(sceneState.previousScenes[1])
        }
    }

    private object First

    private object Second

    private object Third
}
