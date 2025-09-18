/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation3.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventState.InProgress
import androidx.navigationevent.compose.NavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class NavDisplayInfoTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun testPredictiveBackSwipePopulatesNavDisplayInfoCorrectly() {
        val dispatcherOwner = TestNavigationEventDispatcherOwner()
        val dispatcher = dispatcherOwner.navigationEventDispatcher
        val input = DirectNavigationEventInput()
        dispatcher.addInput(input)

        val backStack = mutableListOf(KEY_1, KEY_2, KEY_3, KEY_4, KEY_5)

        // This scene strategy simulates a pop of 2 entries.
        val sceneStrategy = TestSceneStrategy { current, entries, _ ->
            TestScene(current!!.contentKey, entries, entries.dropLast(2))
        }

        rule.setContent {
            NavigationEventDispatcherOwner(parent = dispatcherOwner) {
                NavDisplay(
                    backStack = backStack,
                    sceneStrategy = sceneStrategy,
                    onBack = { backStack.removeAt(backStack.lastIndex) },
                    entryProvider = { key -> NavEntry(key) {} },
                )
            }
        }

        // Simulate a predictive back gesture in progress.
        input.backProgressed(NavigationEvent(progress = 0.5F))

        // Wait for the UI to recompose and update the state.
        rule.waitForIdle()

        // Get an immutable copy of the back stack at this point in the test.
        val currentBackStack = backStack.toList()

        // Assert the state correctly reflects the in-progress gesture.
        rule.runOnIdle {
            @Suppress("UNCHECKED_CAST")
            val currentState = dispatcher.state.value as InProgress<NavDisplayInfo>

            // The `currentInfo` should reflect the current back stack.
            assertThat(currentState.currentInfo.visibleEntries)
                .containsExactlyElementsIn(currentBackStack)

            // The `previousInfo` should reflect the back stack after a pop,
            // as calculated by the SceneStrategy.
            assertThat(currentState.backInfo.lastOrNull()?.visibleEntries)
                .containsExactlyElementsIn(currentBackStack.dropLast(2))
        }
    }
}

private const val KEY_1 = "KEY_1"
private const val KEY_2 = "KEY_2"
private const val KEY_3 = "KEY_3"
private const val KEY_4 = "KEY_4"
private const val KEY_5 = "KEY_5"

private data class TestScene<T : Any>(
    override val key: Any = Any(),
    override val entries: List<NavEntry<T>> = emptyList(),
    override val previousEntries: List<NavEntry<T>> = emptyList(),
    override val content: @Composable (() -> Unit) = {},
) : Scene<T>

private class TestSceneStrategy<T : Any>(
    private val calculateScene: (NavEntry<T>?, List<NavEntry<T>>, (Int) -> Unit) -> Scene<T>
) : SceneStrategy<T> {

    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>, onBack: (Int) -> Unit): Scene<T>? =
        calculateScene(entries.lastOrNull(), entries, onBack)
}
