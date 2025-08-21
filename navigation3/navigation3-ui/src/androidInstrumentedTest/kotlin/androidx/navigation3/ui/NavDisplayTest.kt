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

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.kruth.assertThat
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDisplayTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testContentShown() {
        composeTestRule.setContent {
            val backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                NavEntry(first) { Text(first) }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
    }

    @Test
    fun testContentChanged() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isFalse()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }

    @Test
    fun testDialog() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                sceneStrategy = DialogSceneStrategy(),
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            Button(onClick = { backStack += second }) { Text(first) }
                        }
                    second ->
                        NavEntry(second, metadata = DialogSceneStrategy.dialog()) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).performClick()

        composeTestRule.waitForIdle()
        // Both first and second should be showing if we are on a dialog.
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }

    @Test
    fun testOnBack() {
        lateinit var onBackDispatcher: OnBackPressedDispatcher
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            onBackDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(second) }

        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { onBackDispatcher.onBackPressed() }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
    }

    @Test
    fun testStateOfInactiveContentIsRestoredWhenWeGoBackToIt() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var backStack: NavBackStack
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    First ->
                        NavEntry(First, First.toString()) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                        }
                    Second -> NavEntry(Second, Second.toString()) {}
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            assertWithMessage("The number should be 1").that(numberOnScreen1.value).isEqualTo(1)
            backStack.add(Second)
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
        lateinit var numberOnScreen2: MutableState<Int>
        lateinit var backStack: NavBackStack
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    First ->
                        NavEntry(First, First.toString()) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                        }
                    Second ->
                        NavEntry(Second, Second.toString()) {
                            numberOnScreen2 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen2: ${numberOnScreen2.value}")
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

        composeTestRule.runOnIdle { backStack.add(Second) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2.value).isEqualTo(0)
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
        }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be 4").that(numberOnScreen2.value).isEqualTo(4)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen2: 4").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.removeAt(backStack.size - 1) }

        composeTestRule.runOnIdle {
            assertWithMessage("The number should be restored")
                .that(numberOnScreen1.value)
                .isEqualTo(2)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack.add(Second) }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2.value).isEqualTo(0)
        }

        assertThat(composeTestRule.onNodeWithText("numberOnScreen2: 0").isDisplayed()).isTrue()
    }

    @Test
    fun testIndividualSavedStateRegistries() {
        lateinit var mainRegistry: SavedStateRegistry
        lateinit var registry1: SavedStateRegistry
        lateinit var registry2: SavedStateRegistry
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            mainRegistry = LocalSavedStateRegistryOwner.current.savedStateRegistry
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                entryDecorators = listOf(rememberSavedStateNavEntryDecorator()),
            ) {
                when (it) {
                    first ->
                        NavEntry(first) {
                            registry1 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                        }
                    second ->
                        NavEntry(second) {
                            registry2 = LocalSavedStateRegistryOwner.current.savedStateRegistry
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Registries should be unique")
                .that(mainRegistry)
                .isNotEqualTo(registry1)
            backStack.add(second)
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Registries should be unique").that(registry2).isNotEqualTo(registry1)
        }
    }

    @Test
    fun swappingOutMultipleBackStacks() {
        lateinit var backStack1: MutableList<String>
        lateinit var backStack2: MutableList<String>
        lateinit var backStack3: MutableList<String>
        lateinit var state: MutableState<Int>

        composeTestRule.setContent {
            backStack1 = remember { mutableStateListOf(first) }
            backStack2 = remember { mutableStateListOf(second) }
            backStack3 = remember { mutableStateListOf(third) }
            state = remember { mutableStateOf(1) }
            val backStack =
                when (state.value) {
                    1 -> backStack1
                    2 -> backStack2
                    else -> backStack3
                }

            NavDisplay(
                backStack =
                    when (state.value) {
                        1 -> backStack1
                        2 -> backStack2
                        else -> backStack3
                    },
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                entryProvider =
                    entryProvider {
                        entry(first) { Text(first) }
                        entry(second) { Text(second) }
                        entry(third) { Text(third) }
                        entry(forth) { Text(forth) }
                    },
            )
        }

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { state.value = 2 }
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { state.value = 3 }
        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()

        composeTestRule.runOnIdle { backStack3.add(forth) }

        assertThat(backStack3).containsExactly(third, forth)
        assertThat(composeTestRule.onNodeWithText(forth).isDisplayed()).isTrue()
    }

    @Test
    fun swappingBackStackUsesDifferentHoistedStates() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var numberOnScreen2: MutableState<Int>

        lateinit var backStackState: MutableState<Int>
        lateinit var decoratorState: MutableState<Int>

        composeTestRule.setContent {
            val backStack1 = rememberNavBackStack(First)
            val backStack2 = rememberNavBackStack(Second)
            val decorator1 =
                listOf(rememberSceneSetupNavEntryDecorator(), rememberSavedStateNavEntryDecorator())
            val decorator2 =
                listOf(rememberSceneSetupNavEntryDecorator(), rememberSavedStateNavEntryDecorator())
            backStackState = remember { mutableStateOf(1) }
            decoratorState = remember { mutableStateOf(1) }

            val backStack =
                when (backStackState.value) {
                    1 -> backStack1
                    else -> backStack2
                }
            val decorators =
                when (decoratorState.value) {
                    1 -> decorator1
                    else -> decorator2
                }
            NavDisplay(
                backStack = backStack,
                entryDecorators = decorators,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                entryProvider =
                    entryProvider {
                        entry(First, First.toString()) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                            Text(first)
                        }
                        entry(Second, Second.toString()) {
                            numberOnScreen2 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen2: ${numberOnScreen2.value}")
                            Text(second)
                        }
                    },
            )
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
            numberOnScreen1.value++
            numberOnScreen1.value++
        }

        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText("numberOnScreen1: 2").assertIsDisplayed()

        backStackState.value = 2
        decoratorState.value = 2

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0").that(numberOnScreen2.value).isEqualTo(0)
            numberOnScreen2.value++
            numberOnScreen2.value++
            numberOnScreen2.value++
        }
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        composeTestRule.onNodeWithText("numberOnScreen2: 3").assertIsDisplayed()

        backStackState.value = 1
        decoratorState.value = 1

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText("numberOnScreen1: 2").assertIsDisplayed()

        backStackState.value = 2
        decoratorState.value = 2
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        composeTestRule.onNodeWithText("numberOnScreen2: 3").assertIsDisplayed()
    }

    @Test
    fun testInitEmptyBackstackThrows() {
        lateinit var backStack: MutableList<Any>
        val fail =
            assertFailsWith<IllegalArgumentException> {
                composeTestRule.setContent {
                    backStack = remember { mutableStateListOf() }
                    NavDisplay(
                        backStack = backStack,
                        onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
                    ) {
                        NavEntry(first) {}
                    }
                }
            }
        assertThat(fail.message).isEqualTo("NavDisplay backstack cannot be empty")
    }

    @Test
    fun testPopToEmptyBackstackThrows() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        val fail =
            assertFailsWith<IllegalArgumentException> {
                composeTestRule.runOnIdle { backStack.clear() }
                composeTestRule.waitForIdle()
            }
        assertThat(fail.message).isEqualTo("NavDisplay backstack cannot be empty")
    }

    @Test
    fun testPopAddInSameFrame() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()
        assertThat(backStack).containsExactly(first)

        composeTestRule.runOnIdle { backStack.add(second) }
        assertThat(backStack).containsExactly(first, second)

        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            backStack.removeAt(backStack.size - 1)
            backStack.add(second)
        }
        assertThat(backStack).containsExactly(first, second)

        assertThat(composeTestRule.onNodeWithText(second).isDisplayed()).isTrue()
    }

    @Test
    fun testPopAddInCenterInSameFrame() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(first, third, forth) }
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    third -> NavEntry(third) { Text(third) }
                    forth -> NavEntry(forth) { Text(forth) }
                    else -> error("Invalid key passed")
                }
            }
        }
        assertThat(backStack).containsExactly(first, third, forth)

        assertThat(composeTestRule.onNodeWithText(forth).isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            backStack.add(1, second)
            backStack.removeAt(backStack.size - 1)
        }
        assertThat(backStack).containsExactly(first, second, third)

        assertThat(composeTestRule.onNodeWithText(third).isDisplayed()).isTrue()
    }

    @Test
    fun testDuplicateKeyStateIsShared() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var backStack: NavBackStack
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) {
                when (it) {
                    First ->
                        NavEntry(First, First.toString()) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Text("numberOnScreen1: ${numberOnScreen1.value}")
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()

        assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(0)
        numberOnScreen1.value++
        numberOnScreen1.value++

        composeTestRule.onNodeWithText("numberOnScreen1: 2").assertIsDisplayed()

        backStack.add(First)
        composeTestRule.waitForIdle()

        assertWithMessage("Initial number should be 0").that(numberOnScreen1.value).isEqualTo(2)
        composeTestRule.onNodeWithText("numberOnScreen1: 2").assertIsDisplayed()

        backStack.removeLastOrNull()
        composeTestRule.waitForIdle()

        assertWithMessage("The number should be restored").that(numberOnScreen1.value).isEqualTo(2)
        composeTestRule.onNodeWithText("numberOnScreen1: 2").assertIsDisplayed()
    }

    @Test
    fun testDuplicateKeyStateNestedStateIsCorrect() {
        lateinit var numberOnScreen1: MutableState<Int>
        lateinit var nestedNumberOnScreen1: MutableState<Int>
        lateinit var backStack: NavBackStack
        lateinit var nestedBackStack: NavBackStack
        composeTestRule.setContent {
            backStack = rememberNavBackStack(First)
            nestedBackStack = rememberNavBackStack(First)
            NavDisplay(
                backStack = backStack,
                onBack = { repeat(it) { backStack.removeAt(backStack.lastIndex) } },
            ) { outerKey ->
                when (outerKey) {
                    First ->
                        NavEntry(First, First.toString()) {
                            numberOnScreen1 = rememberSaveable { mutableStateOf(0) }
                            Column {
                                Text("numberOnScreen1: ${numberOnScreen1.value}")
                                NavDisplay(
                                    backStack = nestedBackStack,
                                    onBack = {
                                        repeat(it) {
                                            nestedBackStack.removeAt(nestedBackStack.lastIndex)
                                        }
                                    },
                                ) { innerKey ->
                                    when (innerKey) {
                                        First ->
                                            NavEntry(First, First.toString()) {
                                                nestedNumberOnScreen1 = rememberSaveable {
                                                    mutableStateOf(0)
                                                }
                                                Text(
                                                    "nestedNumberOnScreen1: ${nestedNumberOnScreen1.value}"
                                                )
                                            }
                                        else -> error("Invalid key passed")
                                    }
                                }
                            }
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

        assertThat(composeTestRule.onNodeWithText("numberOnScreen1: 2").isDisplayed()).isTrue()

        composeTestRule.runOnIdle {
            assertWithMessage("Initial number should be 0")
                .that(nestedNumberOnScreen1.value)
                .isEqualTo(0)
            nestedNumberOnScreen1.value++
        }

        assertThat(composeTestRule.onNodeWithText("nestedNumberOnScreen1: 1").isDisplayed())
            .isTrue()
    }

    @Test
    fun testMultiPaneDisplay() {
        val leftText = "left screen"
        val rightText = "right screen"
        val singleText = "single screen"

        val backStack = mutableStateListOf("left", "right")
        composeTestRule.setContent {
            NavDisplay(sceneStrategy = TestTwoPaneSceneStrategy(), backStack = backStack) { key ->
                when (key) {
                    "left" -> NavEntry("left") { Text(leftText) }
                    "right" -> NavEntry("right") { Text(rightText) }
                    "single" -> NavEntry("single") { Text(singleText) }
                    else -> error("invalid key")
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(leftText).assertIsDisplayed()
        composeTestRule.onNodeWithText(rightText).assertIsDisplayed()

        backStack.clear()
        backStack.add("single")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(leftText).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(rightText).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(singleText).assertIsDisplayed()
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
private const val forth = "forth"

@Serializable object First : NavKey

@Serializable object Second : NavKey

class TestTwoPaneScene<T : Any>(
    override val key: Any,
    override val entries: List<NavEntry<T>>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val content: @Composable (() -> Unit) = {
        val left = entries.first()
        val right = entries.last()
        Row {
            Column { left.Content() }
            Column { right.Content() }
        }
    }
}

class TestTwoPaneSceneStrategy<T : Any> : SceneStrategy<T> {
    @Composable
    override fun calculateScene(entries: List<NavEntry<T>>, onBack: (Int) -> Unit): Scene<T>? {
        if (entries.size < 2) return null
        val lastTwoEntries = entries.takeLast(2)
        return TestTwoPaneScene(
            key = lastTwoEntries.first().contentKey,
            entries = entries.takeLast(2),
            previousEntries = listOf(),
        )
    }
}
