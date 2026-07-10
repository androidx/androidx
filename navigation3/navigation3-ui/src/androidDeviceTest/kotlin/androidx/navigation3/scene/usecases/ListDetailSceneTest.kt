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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.scene.usecases.ListDetailScene.Companion.DETAIL_KEY
import androidx.navigation3.scene.usecases.ListDetailScene.Companion.LIST_KEY
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

/** A [Scene] that displays a list and a detail [NavEntry] side-by-side in a 40/60 split. */
class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    val listEntry: NavEntry<T>,
    val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)
    override val content: @Composable (() -> Unit) = {
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.4f)) { listEntry.Content() }
            Column(modifier = Modifier.weight(0.6f)) {
                AnimatedContent(
                    targetState = detailEntry,
                    contentKey = { entry -> entry.contentKey },
                    transitionSpec = {
                        // Slide new content up, keeping the old content in place underneath
                        slideIntoContainer(
                            towards = SlideDirection.Up,
                            animationSpec = tween(1000),
                        ) togetherWith ExitTransition.KeepUntilTransitionsFinished
                    },
                ) { entry ->
                    entry.Content()
                }
            }
        }
    }

    companion object {
        internal const val LIST_KEY = "ListDetailScene-List"
        internal const val DETAIL_KEY = "ListDetailScene-Detail"

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed as a list
         * in the [ListDetailScene].
         */
        fun listPane() = mapOf(LIST_KEY to true)

        /**
         * Helper function to add metadata to a [NavEntry] indicating it can be displayed as a list
         * in the [ListDetailScene].
         */
        fun detailPane() = mapOf(DETAIL_KEY to true)
    }
}

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): ListDetailSceneStrategy<T> {
    // This should really be calculated from the WindowSizeClass, but for testing purposes
    // we'll say the device is always wide enough
    val isWidthWideEnough = true

    return remember { ListDetailSceneStrategy(isWidthWideEnough = isWidthWideEnough) }
}

/**
 * A [SceneStrategy] that returns a [ListDetailScene] if the window is wide enough and the last two
 * back stack entries are list and detail.
 */
class ListDetailSceneStrategy<T : Any>(val isWidthWideEnough: Boolean) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {

        if (!isWidthWideEnough) {
            return null
        }

        val detailEntry =
            entries.lastOrNull()?.takeIf { it.metadata.containsKey(DETAIL_KEY) } ?: return null
        val listEntry = entries.findLast { it.metadata.containsKey(LIST_KEY) } ?: return null

        // We use the list's contentKey to uniquely identify the scene.
        // This allows the detail panes to be animated in and out by the scene, rather than
        // having NavDisplay animate the whole scene out when the selected detail item changes.
        val sceneKey = listEntry.contentKey

        return ListDetailScene(
            key = sceneKey,
            previousEntries = entries.dropLast(1),
            listEntry = listEntry,
            detailEntry = detailEntry,
        )
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class ListDetailSceneTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testContentShown() {
        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = ListDetailScene.detailPane()) { Text(it) }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testNavigateToSecondDetail() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = ListDetailScene.detailPane()) { key -> Text(key) }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add("$secondPrefix:1") }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testReplaceDetail() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = ListDetailScene.detailPane()) { key -> Text(key) }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle {
            backStack.removeAll { it.startsWith(secondPrefix) }
            backStack.add("$secondPrefix:1")
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:1").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testNavigateToFullScreen() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(first, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = ListDetailScene.detailPane()) { key -> Text(key) }
                    it == third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add(third) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()
    }

    @Test
    fun testOnBackFromFullScreen() {
        lateinit var backStack: MutableList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, "$secondPrefix:0") }
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeAt(backStack.lastIndex) },
                sceneStrategies = listOf(rememberListDetailSceneStrategy()),
            ) {
                when {
                    it == first ->
                        NavEntry(it, metadata = ListDetailScene.listPane()) { Text(first) }
                    it.startsWith(secondPrefix) ->
                        NavEntry(it, metadata = ListDetailScene.detailPane()) { key -> Text(key) }
                    it == third -> NavEntry(it) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add(third) }

        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.removeLastOrNull() }
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText("$secondPrefix:0").isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }
}

private const val first = "first"
private const val secondPrefix = "second"
private const val third = "third"
