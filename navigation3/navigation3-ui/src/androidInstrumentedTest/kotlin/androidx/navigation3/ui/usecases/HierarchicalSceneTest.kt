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

package androidx.navigation3.ui.usecases

import android.window.BackEvent
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.Scene
import androidx.navigation3.ui.SceneStrategy
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

private class HierarchicalScene<T : Any>(
    private val navEntries: List<NavEntry<T>?>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val key: Any = navEntries.filterNotNull().first().contentKey
    override val entries: List<NavEntry<T>> = navEntries.filterNotNull()

    override val content: @Composable () -> Unit = {
        Row {
            navEntries.forEach { navEntry ->
                Box(Modifier.weight(1f)) {
                    if (navEntry != null) {
                        key(navEntry.contentKey) { navEntry.Content() }
                    }
                }
            }
        }
    }
}

private class HierarchicalSceneStrategy<T : Any>(private val columns: Int) : SceneStrategy<T> {
    @Composable
    override fun calculateScene(
        entries: List<NavEntry<T>>,
        onBack: (count: Int) -> Unit,
    ): Scene<T> {
        val includedEntries = entries.takeLast(columns)
        return remember(columns, includedEntries) {
            HierarchicalScene(
                List(columns, includedEntries::getOrNull),
                previousEntries =
                    if (entries.size > columns) {
                        entries.dropLast(1)
                    } else {
                        emptyList()
                    },
            )
        }
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class HierarchicalSceneTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testContentShown() {
        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testContentChanged() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add(third) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()
    }

    @Test
    fun testOnBack() {
        lateinit var onBackDispatcher: OnBackPressedDispatcher
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            onBackDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add(third) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { onBackDispatcher.onBackPressed() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testChangingNumberOfColumns() {
        lateinit var onBackDispatcher: OnBackPressedDispatcher
        lateinit var backStack: MutableList<Any>
        var columns by mutableStateOf(2)

        composeTestRule.setContent {
            onBackDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember(columns) { HierarchicalSceneStrategy(columns) },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { backStack.add(third) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { columns = 1 }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { onBackDispatcher.onBackPressed() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()

        composeTestRule.runOnIdle { columns = 3 }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }

    @Test
    fun testStateOfInactiveContentIsRestoredWhenWeGoBackToIt() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first ->
                        NavEntry(first) { numberOnScreen1 = rememberSaveable { mutableStateOf(0) } }
                    second -> NavEntry(second) {}
                    third -> NavEntry(third) {}
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            assertWithMessage("The number should be 1").that(numberOnScreen1.value).isEqualTo(1)
            backStack.add(third)
        }

        composeTestRule.runOnIdle { backStack.removeAt(backStack.size - 1) }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(1)
        }
    }

    @Test
    fun testStateIsRemovedOnPop() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var numberOnScreen3: MutableState<Int>
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, second) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                        }
                    second -> NavEntry(second) {}
                    third ->
                        NavEntry(third) {
                            numberOnScreen3 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen3: ${numberOnScreen3.value}")
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 2").that(numberOnScreen1.value).isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(third) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen3.value).isEqualTo(0)
            numberOnScreen3.value++
            numberOnScreen3.value++
            numberOnScreen3.value++
            numberOnScreen3.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 4").that(numberOnScreen3.value).isEqualTo(4)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen3: 4").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.removeAt(backStack.size - 1) }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(third) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen3.value).isEqualTo(0)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen3: 0").isDisplayed()).isTrue()
    }

    @Test
    fun testPredictiveBackShowsCorrectEntries() {
        lateinit var backPressedDispatcher: OnBackPressedDispatcher
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backPressedDispatcher =
                LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = remember { HierarchicalSceneStrategy(2) },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            backPressedDispatcher.dispatchOnBackStarted(
                BackEventCompat(0.1F, 0.1F, 0.1F, BackEvent.EDGE_LEFT)
            )
            backPressedDispatcher.dispatchOnBackProgressed(
                BackEventCompat(0.1F, 0.1F, 0.5F, BackEvent.EDGE_LEFT)
            )
        }

        composeTestRule.waitForIdle()

        // While predictive back is happening, all 3 entries should be shown
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backPressedDispatcher.onBackPressed() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isFalse()
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
