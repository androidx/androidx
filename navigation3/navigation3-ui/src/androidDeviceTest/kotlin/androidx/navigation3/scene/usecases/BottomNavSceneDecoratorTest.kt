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

package androidx.navigation3.scene.usecases

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneDecoratorStrategy
import androidx.navigation3.scene.SceneDecoratorStrategyScope
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.usecases.BottomNavSceneDecorator.Companion.NAV_BAR
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

/** A [Scene] that displays a list and a detail [NavEntry] side-by-side in a 40/60 split. */
class BottomNavSceneDecorator<T : Any>(scene: Scene<T>) : Scene<T> {
    override val key: Any = scene.key
    override val entries: List<NavEntry<T>> = scene.entries
    override val previousEntries: List<NavEntry<T>> = scene.previousEntries
    override val content: @Composable (() -> Unit) = {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(0.9f)) { scene.content() }
            Row(modifier = Modifier.weight(0.1f)) {
                NavigationBar {
                    listOf(bottom_nav_home, bottom_nav_settings).forEach {
                        NavigationBarItem(selected = true, onClick = {}, icon = { Text(it) })
                    }
                }
            }
        }
    }

    companion object {
        internal const val NAV_BAR = "BottomNavScene-Bar"

        /**
         * Helper function to add metadata to a [Scene] indicating it should be displayed with
         * [BottomNavSceneDecorator]
         */
        fun showNavBar() = mapOf(NAV_BAR to true)
    }
}

/**
 * A [SceneStrategy] that returns a [BottomNavSceneDecorator] if there is a scene that wants to
 * display it
 */
class BottomNavSceneDecoratorStrategy<T : Any> : SceneDecoratorStrategy<T> {
    override fun SceneDecoratorStrategyScope<T>.decorateScene(scene: Scene<T>): Scene<T> {
        // If the scene provides metadata for a bottom nav display it.
        if (scene.metadata.containsKey(NAV_BAR)) {
            return BottomNavSceneDecorator(scene)
        }
        return scene
    }
}

@Composable
fun <T : Any> rememberBottomNavSceneStrategy(): BottomNavSceneDecoratorStrategy<T> {
    return remember { BottomNavSceneDecoratorStrategy() }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class BottomNavSceneDecoratorTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testContentShown() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = BottomNavSceneDecorator.showNavBar()) {
                            Text(first)
                        }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = BottomNavSceneDecorator.showNavBar()) { Text(it) }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()
    }

    @Test
    fun testContentShownOnNavigate() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first -> NavEntry(first) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = BottomNavSceneDecorator.showNavBar()) { Text(it) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()
    }

    @Test
    fun testContentRemovedOnPop() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first -> NavEntry(first) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = BottomNavSceneDecorator.showNavBar()) { Text(it) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isFalse()
    }

    @Test
    fun testWithThenDialog() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategy = DialogSceneStrategy(),
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = BottomNavSceneDecorator.showNavBar()) {
                            Text(first)
                        }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = DialogSceneStrategy.dialog()) { Text(it) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()
    }

    @Test
    fun testDialogThenWithBottomNav() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategy = DialogSceneStrategy(),
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = BottomNavSceneDecorator.showNavBar()) {
                            Text(first)
                        }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = DialogSceneStrategy.dialog()) { Text(it) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()
    }

    @Test
    fun testBottomNavInDialogNotShown() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategy = DialogSceneStrategy(),
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first -> NavEntry(first) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(
                            it,
                            metadata =
                                DialogSceneStrategy.dialog() + BottomNavSceneDecorator.showNavBar(),
                        ) {
                            Text(it)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        composeTestRule.waitForIdle()

        // this is show because we can't decorate overlays
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isFalse()
    }

    @Test
    fun testBottomNavInListDetail() {
        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategy = rememberListDetailSceneStrategy(),
                sceneDecoratorStrategies = listOf(rememberBottomNavSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(
                            it,
                            metadata =
                                ListDetailScene.detailPane() + BottomNavSceneDecorator.showNavBar(),
                        ) {
                            Text(it)
                        }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_home).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(bottom_nav_settings).isDisplayed()).isTrue()
    }
}

private const val first = "first"
private const val secondPrefix = "second"
private const val third = "third"

private const val bottom_nav_home = "Home"
private const val bottom_nav_settings = "Settings"
