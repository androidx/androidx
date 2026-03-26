/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation3.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.kruth.assertThat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayLifecycleTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testNavigateForwardFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(backStack = backStack, onBack = { /* no-op */ }) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack += "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
            .inOrder()
    }

    @Test
    fun testNavigateBackFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A", "B")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(backStack = backStack, onBack = { /* no-op */ }) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack -= "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("B" to "ON_RESUME", "B" to "ON_PAUSE", "A" to "ON_RESUME")
            .inOrder()
    }

    @Test
    fun testTwoPaneForwardFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(TestTwoPaneSceneStrategy()),
            ) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack += "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly(
                "A" to "ON_RESUME",
                "A" to "ON_PAUSE",
                "A" to "ON_RESUME",
                "B" to "ON_RESUME",
            )
            .inOrder()
    }

    @Test
    fun testTwoPaneBackFiresLifecycleEventsInOrder() {
        val backStack = mutableStateListOf("A", "B")

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(TestTwoPaneSceneStrategy()),
            ) { key ->
                NavEntry(key) {
                    LifecycleResumeEffect(key1 = Unit) {
                        actualEvents += key to "ON_RESUME"
                        onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                    }
                }
            }
        }
        rule.runOnIdle { backStack -= "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly(
                "A" to "ON_RESUME",
                "B" to "ON_RESUME",
                "B" to "ON_PAUSE",
                "A" to "ON_PAUSE",
                "A" to "ON_RESUME",
            )
            .inOrder()
    }

    @Test
    fun testNavigateToDialogKeepsSinglePaneEntryAtStarted() {
        lateinit var backStack: SnapshotStateList<String>

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            backStack = remember { mutableStateListOf("A") }
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(remember { DialogSceneStrategy() }),
                onBack = { /* no-op */ },
            ) { key ->
                when (key) {
                    "A" ->
                        NavEntry("A") {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    else ->
                        NavEntry("B", metadata = DialogSceneStrategy.dialog()) {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                }
            }
        }
        rule.runOnIdle { backStack += "B" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
            .inOrder()
    }

    @Test
    fun testNavigateFromDialogToSinglePaneKeepsDialogAtStarted() {
        lateinit var backStack: SnapshotStateList<String>

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            backStack = remember { mutableStateListOf("A", "B") }
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(remember { DialogSceneStrategy() }),
                onBack = { /* no-op */ },
            ) { key ->
                when (key) {
                    "A" ->
                        NavEntry("A") {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    "B" ->
                        NavEntry("B", metadata = DialogSceneStrategy.dialog()) {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    else ->
                        NavEntry("C") {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                }
            }
        }

        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
            .inOrder()

        rule.runOnIdle { backStack += "C" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly(
                "A" to "ON_RESUME",
                "A" to "ON_PAUSE",
                "B" to "ON_RESUME",
                "B" to "ON_PAUSE",
                "C" to "ON_RESUME",
            )
            .inOrder()
    }

    @Test
    fun testNavigateToSecondDialogKeepsFirstDialogAtStarted() {
        lateinit var backStack: SnapshotStateList<String>

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            backStack = remember { mutableStateListOf("A", "B") }
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(remember { DialogSceneStrategy() }),
                onBack = { /* no-op */ },
            ) { key ->
                when (key) {
                    "A" ->
                        NavEntry("A") {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    "B" ->
                        NavEntry("B", metadata = DialogSceneStrategy.dialog()) {
                            println("B Lifecycle owner = ${LocalLifecycleOwner.current}")
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    else ->
                        NavEntry("C", metadata = DialogSceneStrategy.dialog()) {
                            println("C Lifecycle owner = ${LocalLifecycleOwner.current}")
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                }
            }
        }

        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
            .inOrder()

        rule.runOnIdle { backStack += "C" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly(
                "A" to "ON_RESUME",
                "A" to "ON_PAUSE",
                "B" to "ON_RESUME",
                "B" to "ON_PAUSE",
                "C" to "ON_RESUME",
            )
            .inOrder()
    }

    @Test
    fun testNavigateToSecondOverlayKeepsFirstOverlayAtStarted() {
        lateinit var backStack: SnapshotStateList<String>

        val actualEvents = mutableListOf<Pair<String, String>>()
        rule.setContent {
            backStack = remember { mutableStateListOf("A", "B") }
            NavDisplay(
                backStack = backStack,
                sceneStrategies = listOf(remember { MyCustomOverlaySceneStrategy() }),
                onBack = { /* no-op */ },
            ) { key ->
                when (key) {
                    "A" ->
                        NavEntry("A") {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    "B" ->
                        NavEntry("B", metadata = MyCustomOverlaySceneStrategy.overlay()) {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                    else ->
                        NavEntry("C", metadata = MyCustomOverlaySceneStrategy.overlay()) {
                            LifecycleResumeEffect(key1 = Unit) {
                                actualEvents += key to "ON_RESUME"
                                onPauseOrDispose { actualEvents += key to "ON_PAUSE" }
                            }
                        }
                }
            }
        }

        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly("A" to "ON_RESUME", "A" to "ON_PAUSE", "B" to "ON_RESUME")
            .inOrder()

        rule.runOnIdle { backStack += "C" }
        rule.waitForIdle()

        assertThat(actualEvents)
            .containsExactly(
                "A" to "ON_RESUME",
                "A" to "ON_PAUSE",
                "B" to "ON_RESUME",
                "B" to "ON_PAUSE",
                "C" to "ON_RESUME",
            )
            .inOrder()
    }
}

class MyCustomOverlayScene<T : Any>(
    override val key: Any,
    entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val content: @Composable (() -> Unit) = {
        Box(Modifier.height(250.dp).fillMaxWidth().background(Color.Blue)) { entry.Content() }
    }
}

internal class MyCustomOverlaySceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        if (lastEntry == null || lastEntry.metadata[OverlayKey] != true) return null
        return MyCustomOverlayScene(
            key = lastEntry.contentKey,
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
        )
    }

    companion object {

        object OverlayKey : NavMetadataKey<Boolean>

        fun overlay(): Map<String, Any> = metadata { put(OverlayKey, true) }
    }
}
