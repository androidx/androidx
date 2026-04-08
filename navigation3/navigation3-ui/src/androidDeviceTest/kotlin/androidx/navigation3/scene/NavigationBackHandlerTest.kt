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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavigationBackHandlerTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    private val input = DirectNavigationEventInput()
    private val owner =
        TestNavigationEventDispatcherOwner().apply { navigationEventDispatcher.addInput(input) }

    @Test
    fun testNavigationBackHandlerSinglePop() {
        var backCount = 0
        val backStack = mutableStateListOf(First, Second)

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                val entries =
                    rememberDecoratedNavEntries(
                        backStack,
                        emptyList(),
                        entryProvider {
                            entry<First> {}
                            entry<Second> {}
                        },
                    )
                val sceneState =
                    rememberSceneState(entries, listOf(SinglePaneSceneStrategy())) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                NavigationBackHandler(sceneState) {
                    backCount++
                    backStack.removeAt(backStack.lastIndex)
                }
            }
        }

        rule.runOnIdle { input.backCompleted() }

        rule.runOnIdle {
            assertThat(backCount).isEqualTo(1)
            assertThat(backStack).containsExactly(First)
        }
    }

    @Test
    fun testNavigationBackHandlerMultiPop() {
        var backCount = 0
        val backStack = mutableStateListOf(First, Second, Third)

        // A scene strategy that pops two entries.
        val multiPopStrategy = SceneStrategy { entries ->
            SinglePaneScene(
                key = entries.last().contentKey,
                entry = entries.last(),
                previousEntries = entries.dropLast(2),
            )
        }

        rule.setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
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
                val sceneState =
                    rememberSceneState(
                        entries,
                        listOf(multiPopStrategy, SinglePaneSceneStrategy()),
                    ) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                NavigationBackHandler(sceneState) {
                    backCount++
                    backStack.removeAt(backStack.lastIndex)
                }
            }
        }

        rule.runOnIdle { input.backCompleted() }

        rule.runOnIdle {
            assertThat(backCount).isEqualTo(2)
            assertThat(backStack).containsExactly(First)
        }
    }

    private object First

    private object Second

    private object Third
}
