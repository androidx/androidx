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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.TestTwoPaneScene
import androidx.navigation3.ui.TestTwoPaneSceneStrategy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
internal class SceneStateTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

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
                rememberSceneState(entries, listOf(DialogSceneStrategy())) {
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

    @Suppress("DEPRECATION")
    @Test
    fun testSceneStrategyThenFirstStrategy() {
        val sceneStrategy = TestTwoPaneSceneStrategy<String>() then (SinglePaneSceneStrategy())
        val entries = listOf(NavEntry(key = "first") {}, NavEntry(key = "second") {})
        var currentScene: Scene<String>? = null
        rule.setContent {
            val sceneState = rememberSceneState(entries, listOf(sceneStrategy)) {}
            currentScene = sceneState.currentScene
        }

        rule.waitForIdle()
        assertThat(currentScene).isInstanceOf<TestTwoPaneScene<String>>()
    }

    @Suppress("DEPRECATION")
    @Test
    fun testSceneStrategyThenChainedStrategy() {
        val sceneStrategy = TestTwoPaneSceneStrategy<String>() then (SinglePaneSceneStrategy())
        val entries = listOf(NavEntry(key = "first") {})
        var currentScene: Scene<String>? = null
        rule.setContent {
            val sceneState = rememberSceneState(entries, listOf(sceneStrategy)) {}
            currentScene = sceneState.currentScene
        }

        rule.waitForIdle()
        assertThat(currentScene).isInstanceOf<SinglePaneScene<String>>()
    }

    @Test
    fun testSceneStateDoesNotRecalculateOnUnrelatedRecomposition() {
        val strategy = CountingSceneStrategy<Any>()
        val backStack = mutableStateListOf(First)
        val sceneStates = mutableSetOf<SceneState<Any>>()

        // Unrelated state we'll use to trigger recomposition.
        var tick by mutableStateOf(0)

        rule.setContent {
            val entries =
                rememberDecoratedNavEntries<Any>(
                    backStack,
                    entryDecorators = emptyList(),
                    entryProvider { entry<First> { Text("First") } },
                )

            // Read tick to participate in recomposition without changing inputs.
            @Suppress("UnusedVariable", "unused") val unused = tick

            sceneStates += rememberSceneState(entries, listOf(strategy), onBack = {})
        }

        // First composition should call calculate once.
        assertThat(strategy.calculateSceneInvocations).isEqualTo(1)

        // Trigger recomposition.
        rule.runOnIdle { tick++ }
        rule.runOnIdle { tick++ }
        rule.runOnIdle { tick++ }

        // After recomposition, still only one calculation.
        rule.runOnIdle { assertThat(strategy.calculateSceneInvocations).isEqualTo(1) }

        // Sanity check.
        rule.runOnIdle {
            assertThat(sceneStates.first().currentScene).isInstanceOf<SinglePaneScene<Any>>()
        }
        rule.runOnIdle { assertThat(sceneStates.size).isEqualTo(1) }
    }

    @Test
    fun testSceneStateDoesNotRecalculateOnOnBackChange() {
        val strategy = CountingSceneStrategy<Any>()

        // Unrelated state we'll use to trigger recomposition.
        var tick by mutableStateOf(0)

        rule.setContent {
            val entries =
                rememberDecoratedNavEntries<Any>(
                    listOf(First),
                    entryDecorators = emptyList(),
                    entryProvider { entry<First> { Text("First") } },
                )

            // This creates a new lambda instance on every recomposition
            // that captures the current 'tick'.
            val unstableOnBack = {
                @Suppress("UNUSED_VARIABLE") val unused = tick
            }

            rememberSceneState(entries, listOf(strategy), onBack = unstableOnBack)
        }

        // First composition should call calculate once.
        rule.runOnIdle { assertThat(strategy.calculateSceneInvocations).isEqualTo(1) }

        // Trigger recomposition, which creates a new 'unstableOnBack' lambda.
        rule.runOnIdle { tick++ }
        rule.runOnIdle { tick++ }

        // After recomposition, calculation should NOT have run again.
        rule.runOnIdle { assertThat(strategy.calculateSceneInvocations).isEqualTo(1) }
    }

    @Test
    fun testSceneStateRecalculatesOnEntriesChange() {
        val strategy = CountingSceneStrategy<Any>()
        val backStack = mutableStateListOf<Any>(First)

        rule.setContent {
            val entries =
                rememberDecoratedNavEntries(
                    backStack,
                    entryDecorators = emptyList(),
                    entryProvider {
                        entry<First> { Text("First") }
                        entry<Second> { Text("Second") }
                    },
                )
            rememberSceneState(entries, listOf(strategy), onBack = {})
        }

        // First composition should call calculate once.
        rule.runOnIdle { assertThat(strategy.calculateSceneInvocations).isEqualTo(1) }

        // Trigger recomposition by changing entries.
        rule.runOnIdle { backStack += Second }

        // After recomposition with new entries, calculation should run again.
        rule.runOnIdle { assertThat(strategy.calculateSceneInvocations).isGreaterThan(1) }
    }

    @Test
    fun testSceneStateRecalculatesOnStrategyChange() {
        val initialStrategy = CountingSceneStrategy<Any>()
        val newStrategy = CountingSceneStrategy<Any>()
        var strategy: SceneStrategy<Any> by mutableStateOf(initialStrategy)

        rule.setContent {
            val entries =
                rememberDecoratedNavEntries<Any>(
                    listOf(First),
                    entryDecorators = emptyList(),
                    entryProvider { entry<First> { Text("First") } },
                )
            rememberSceneState(entries, listOf(strategy), onBack = {})
        }

        // First composition should call calculate once on the initial strategy.
        rule.runOnIdle { assertThat(initialStrategy.calculateSceneInvocations).isEqualTo(1) }
        rule.runOnIdle { assertThat(newStrategy.calculateSceneInvocations).isEqualTo(0) }

        // Trigger recomposition by changing the strategy instance.
        rule.runOnIdle { strategy = newStrategy }

        // The new strategy should now be used, incrementing its count.
        rule.runOnIdle { assertThat(initialStrategy.calculateSceneInvocations).isEqualTo(1) }
        rule.runOnIdle { assertThat(newStrategy.calculateSceneInvocations).isEqualTo(1) }
    }
}

private object First

private object Second

private object Third

/** Minimal strategy that counts calls to [calculateSceneWithSinglePaneFallback]. */
private class CountingSceneStrategy<T : Any>() : SceneStrategy<T> {

    private val base = SinglePaneSceneStrategy<T>()

    var calculateSceneInvocations = 0
        private set

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        calculateSceneInvocations++
        with(base) {
            return calculateScene(entries)
        }
    }
}
