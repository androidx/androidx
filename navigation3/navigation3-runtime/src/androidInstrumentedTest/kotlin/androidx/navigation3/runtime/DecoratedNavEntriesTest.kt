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

package androidx.navigation3.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.kruth.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlin.test.fail
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DecoratedNavEntriesTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun decoratedEntriesRemembered() {
        var entries: List<NavEntry<Any>>? = null
        val state = mutableStateOf(2) // force into else branch

        val backStack = mutableStateListOf(1)
        val entryDecorators = listOf(createTestNavEntryDecorator<Int> {})
        val entryProvider: (key: Any) -> NavEntry<out Any> = { NavEntry(1) {} }
        composeTestRule.setContent {
            if (state.value == 1) {
                fail("Should not reach this branch")
            } else {
                entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            }
        }
        val previousEntries = entries
        state.value = 3 // force into else branch

        composeTestRule.waitForIdle()
        assertThat(previousEntries).isEqualTo(entries)
    }

    @Test
    fun decoratedEntriesRecreated() {
        var entries: List<NavEntry<Any>>? = null
        val state = mutableStateOf(1)

        val backStack = mutableStateListOf(1)
        val entryDecorators = listOf(createTestNavEntryDecorator<Int> {})
        val entryProvider: (key: Any) -> NavEntry<out Any> = { NavEntry(1) {} }
        composeTestRule.setContent {
            if (state.value == 1) {
                entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            } else {
                entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            }
        }
        val previousEntries = entries
        state.value = 2 // go into else branch

        composeTestRule.waitForIdle()
        assertThat(previousEntries).isNotEqualTo(entries)
    }

    @Test
    fun decorator_called() {
        var calledWrapContent = false

        val decorator = createTestNavEntryDecorator<Any> { entry -> calledWrapContent = true }

        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = listOf("something"),
                entryDecorators = listOf(decorator),
                entryProvider = { NavEntry("something") {} },
            ) { records ->
                records.last().Content()
            }
        }

        assertThat(calledWrapContent).isTrue()
    }

    @Test
    fun decorator_calledOnce() {
        var calledWrapContentCount = 0

        val decorator = createTestNavEntryDecorator<Any> { entry -> calledWrapContentCount++ }

        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = listOf("something"),
                entryDecorators = listOf(decorator, decorator),
                entryProvider = { NavEntry("something") {} },
            ) { records ->
                records.last().Content()
            }
        }

        composeTestRule.runOnIdle { assertThat(calledWrapContentCount).isEqualTo(1) }
    }

    @Test
    fun decorator_nestedCallOrder() {
        var callOrder = -1
        var outerEntryDecorator: Int = -1
        var innerEntryDecorator: Int = -1
        val innerDecorator =
            createTestNavEntryDecorator<Any> { entry ->
                innerEntryDecorator = ++callOrder
                entry.Content()
            }

        val outerDecorator =
            createTestNavEntryDecorator<Any> { entry ->
                outerEntryDecorator = ++callOrder
                entry.Content()
            }

        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = listOf("something"),
                entryDecorators = listOf(outerDecorator, innerDecorator),
                entryProvider = { NavEntry("something") {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }

        composeTestRule.waitForIdle()

        assertThat(outerEntryDecorator).isEqualTo(0)
        assertThat(innerEntryDecorator).isEqualTo(1)
    }

    @Test
    fun decorator_onPop_order() {
        var count = -1
        var outerPop = -1
        var innerPop = -1
        val innerDecorator =
            createTestNavEntryDecorator<Any>(onPop = { _ -> innerPop = ++count }) { entry ->
                entry.Content()
            }
        val outerDecorator =
            createTestNavEntryDecorator<Any>(onPop = { _ -> outerPop = ++count }) { entry ->
                entry.Content()
            }
        lateinit var backStack: SnapshotStateList<Int>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf(1, 2) }
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(outerDecorator, innerDecorator),
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1) {}
                        2 -> NavEntry(2) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }
        composeTestRule.runOnIdle { backStack.removeAt(1) }

        composeTestRule.waitForIdle()
        assertThat(innerPop).isEqualTo(0)
        assertThat(outerPop).isEqualTo(1)
    }

    @Test
    fun decorator_onPop_neverRenderedEntries() {
        val entriesOnPop = mutableListOf<String>()
        val entriesRendered = mutableListOf<String>()

        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { key -> entriesOnPop.add(key as String) }) {
                entry ->
                entry.Content()
            }
        lateinit var backStack: SnapshotStateList<String>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf("first", "second", "third") }
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = { key ->
                    when (key) {
                        "first" -> NavEntry("first", "first") { entriesRendered.add(it) }
                        "second" -> NavEntry("second", "second") { entriesRendered.add(it) }
                        "third" -> NavEntry("third", "third") { entriesRendered.add(it) }
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }

        assertThat(entriesRendered).containsExactly("third")
        assertThat(entriesOnPop).isEmpty()

        composeTestRule.runOnIdle {
            backStack.removeAt(2)
            backStack.removeAt(1)
        }

        composeTestRule.waitForIdle()
        assertThat(entriesRendered).containsExactly("third", "first")
        assertThat(entriesOnPop).containsExactly("third", "second").inOrder()
    }

    @Test
    fun decorator_onPop_newlyAddedDecorator() {
        val decoratorPopCallback = mutableListOf<String>()
        val decorator1 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator1") }
            ) { entry ->
                entry.Content()
            }
        val decorator2 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator2") }
            ) { entry ->
                entry.Content()
            }
        val backStack = mutableStateListOf(1, 2)
        val decorators = mutableStateListOf(decorator1)
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = decorators,
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1) {}
                        2 -> NavEntry(2) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(backStack).containsExactly(1, 2)
        assertThat(decoratorPopCallback).isEmpty()
        decorators.add(decorator2)

        composeTestRule.waitForIdle()

        assertThat(decoratorPopCallback).isEmpty()
        backStack.removeAt(backStack.lastIndex)

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1)
        assertThat(decoratorPopCallback).containsExactly("decorator2", "decorator1").inOrder()
    }

    @Test
    fun decorator_noOnPop_removedDecorator() {
        val decoratorPopCallback = mutableListOf<String>()
        val decorator1 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator1") }
            ) { entry ->
                entry.Content()
            }
        val decorator2 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator2") }
            ) { entry ->
                entry.Content()
            }
        val backStack = mutableStateListOf(1, 2)
        val decorators = mutableStateListOf(decorator1, decorator2)
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = decorators,
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1) {}
                        2 -> NavEntry(2) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(backStack).containsExactly(1, 2)
        assertThat(decoratorPopCallback).isEmpty()
        decorators.remove(decorator2)

        composeTestRule.waitForIdle()

        assertThat(decoratorPopCallback).isEmpty()
        backStack.removeAt(backStack.lastIndex)

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1)
        assertThat(decoratorPopCallback).containsExactly("decorator1").inOrder()
    }

    @Test
    fun decorator_noOnPop_atomicRemoveDecoratorAndPopEntry() {
        val decoratorPopCallback = mutableListOf<String>()
        val decorator1 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator1") }
            ) { entry ->
                entry.Content()
            }
        val decorator2 =
            createTestNavEntryDecorator<Any>(
                onPop = { key -> decoratorPopCallback.add("decorator2") }
            ) { entry ->
                entry.Content()
            }
        val backStack = mutableStateListOf(1, 2)
        val decorators = mutableStateListOf(decorator1, decorator2)
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = decorators,
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1) {}
                        2 -> NavEntry(2) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(backStack).containsExactly(1, 2)
        assertThat(decoratorPopCallback).isEmpty()

        decorators.remove(decorator2)
        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()

        assertThat(backStack).containsExactly(1)
        assertThat(decoratorPopCallback).containsExactly("decorator1").inOrder()
    }

    @Test
    fun decorator_wrapAddedEntry() {
        val wrappedEntries = mutableListOf<String>()
        val decorator =
            createTestNavEntryDecorator<String> {
                wrappedEntries.add(it.contentKey as String)
                it.Content()
            }
        val backStack = mutableStateListOf("first")
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = {
                    when (it) {
                        "first" -> NavEntry("first", "first") {}
                        "second" -> NavEntry("second", "second") {}
                        else -> error("Unknown key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly("first")
        assertThat(wrappedEntries).containsExactly("first")

        backStack.add("second")
        composeTestRule.waitForIdle()

        assertThat(backStack).containsExactly("first", "second")
        assertThat(wrappedEntries).containsExactly("first", "second")
    }

    @Test
    fun entry_reWrappedWithNewlyAddedDecorator() {
        var dec1Wrapped = 0
        var dec2Wrapped = 0
        val decorator1 =
            createTestNavEntryDecorator<Any> {
                dec1Wrapped++
                it.Content()
            }
        val decorator2 =
            createTestNavEntryDecorator<Any> {
                dec2Wrapped++
                it.Content()
            }
        val decorators = mutableStateListOf(decorator1)
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = listOf("something"),
                entryDecorators = decorators,
                entryProvider = { NavEntry("something") {} },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(dec1Wrapped).isEqualTo(1)
        assertThat(dec2Wrapped).isEqualTo(0)

        decorators.add(decorator2)
        composeTestRule.waitForIdle()

        assertThat(dec1Wrapped).isEqualTo(2)
        assertThat(dec2Wrapped).isEqualTo(1)
    }

    @Test
    fun entry_reWrappedWithRemovedDecorator() {
        var dec1Wrapped = 0
        var dec2Wrapped = 0
        val decorator1 =
            createTestNavEntryDecorator<Any> {
                dec1Wrapped++
                it.Content()
            }
        val decorator2 =
            createTestNavEntryDecorator<Any> {
                dec2Wrapped++
                it.Content()
            }
        val decorators = mutableStateListOf(decorator1, decorator2)
        composeTestRule.setContent {
            WithDecoratedNavEntries(
                backStack = listOf("something"),
                entryDecorators = decorators,
                entryProvider = { NavEntry("something") {} },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(dec1Wrapped).isEqualTo(1)
        assertThat(dec2Wrapped).isEqualTo(1)

        decorators.remove(decorator1)
        composeTestRule.waitForIdle()

        assertThat(dec1Wrapped).isEqualTo(1)
        assertThat(dec2Wrapped).isEqualTo(2)
    }

    @Test
    fun decoratorState_differentContentKeySeparatesState() {
        val stateMap = mutableMapOf<Int, String>()
        val decorator =
            createTestNavEntryDecorator<Any>(
                onPop = { contentKey -> stateMap.remove(contentKey) }
            ) { entry ->
                stateMap.put(entry.contentKey as Int, "state")
                entry.Content()
            }
        lateinit var backStack: SnapshotStateList<Any>
        composeTestRule.setContent {
            backStack = mutableStateListOf(DataClass(1))
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }
        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")

        backStack.add(DataClass(2))

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state", 2 to "state")
        backStack.removeLastOrNull()
        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")
    }

    @Test
    fun decoratorState_sameContentKeySharedState() {
        var popCalled = false
        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { popCalled = true }) { entry ->
                entry.Content()
            }
        val entry1 = NavEntry(1, contentKey = "sameKey") {}
        val entry2 = NavEntry(2, contentKey = "sameKey") {}
        lateinit var backStack: SnapshotStateList<Int>
        composeTestRule.setContent {
            backStack = mutableStateListOf(1, 2)
            WithDecoratedNavEntries(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = { key ->
                    when (key) {
                        1 -> entry1
                        2 -> entry2
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }
        assertThat(entry1.contentKey).isEqualTo(entry2.contentKey)
        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        composeTestRule.waitForIdle()
        assertThat(popCalled).isFalse()
    }

    @Test
    fun singleBackStack_immutableBackStack_navigate() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        lateinit var backStackState: MutableState<Any>
        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> mutableStateListOf(key1)
                    else -> mutableStateListOf(key1, key2)
                }
            val entryDecorators =
                listOf(
                    createTestNavEntryDecorator<Any>(
                        onPop = { contentKey -> stateMap.remove(contentKey) }
                    ) { entry ->
                        stateMap.put(entry.contentKey as Int, "state")
                        entry.Content()
                    }
                )
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = entryDecorators,
                    entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
                )
            entries.lastOrNull()?.Content()
        }
        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(1 to "state") }
        backStackState.value = 2 // navigate to 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state", 2 to "state")
    }

    @Test
    fun singleBackStack_immutableBackStack_pop() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        lateinit var backStackState: MutableState<Any>
        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> mutableStateListOf(key1, key2)
                    else -> mutableStateListOf(key1)
                }
            val entryDecorators =
                listOf(
                    createTestNavEntryDecorator<Any>(
                        onPop = { contentKey -> stateMap.remove(contentKey) }
                    ) { entry ->
                        stateMap.put(entry.contentKey as Int, "state")
                        entry.Content()
                    }
                )
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = entryDecorators,
                    entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
                )
            entries.lastOrNull()?.Content()
        }
        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }
        backStackState.value = 2 // pop 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")
    }

    @Test
    fun singleBackStack_immutableBackStackNavigateDupKey_sharedState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        lateinit var backStackState: MutableState<Any>
        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> mutableStateListOf(key1, key2)
                    else -> mutableStateListOf(key1, key2, key2)
                }
            val entryDecorators =
                listOf(
                    createTestNavEntryDecorator<Any>(
                        onPop = { contentKey -> stateMap.remove(contentKey) }
                    ) { entry ->
                        stateMap.put(entry.contentKey as Int, "state")
                        entry.Content()
                    }
                )
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = entryDecorators,
                    entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
                )
            entries.lastOrNull()?.Content()
        }
        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }
        backStackState.value = 2 // add 2 again

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")
    }

    @Test
    fun singleBackStack_immutableBackStackPopDupKey_statePreserved() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        lateinit var backStackState: MutableState<Any>
        var popCalled = false

        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> mutableStateListOf(key1, key2, key2)
                    else -> mutableStateListOf(key1, key2)
                }
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators =
                        listOf(
                            createTestNavEntryDecorator<Any>(
                                onPop = { contentKey ->
                                    stateMap.remove(contentKey)
                                    popCalled = true
                                }
                            ) { entry ->
                                stateMap.put(entry.contentKey as Int, "state")
                                entry.Content()
                            }
                        ),
                    entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
                )
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }

        backStackState.value = 2 // pop duplicate 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state") // state for 2 is preserved
        assertThat(popCalled).isFalse() // state for 2 is preserved
    }

    @Test
    fun singleBackStack_immutableBackStackPopAllDupKeys_stateCleared() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        lateinit var backStackState: MutableState<Any>

        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(1) }
            val decorator =
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> stateMap.remove(contentKey) }
                ) { entry ->
                    stateMap.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            val backStack =
                when (backStackState.value) {
                    1 -> mutableStateListOf(key1, key2, key2)
                    else -> mutableStateListOf(key1)
                }
            val entries =
                rememberDecoratedNavEntries(
                    backStack = backStack,
                    entryDecorators = listOf(decorator),
                    entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
                )
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }

        backStackState.value = 2 // pop all 2's

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")
    }

    @Test
    fun singleBackStack_hoistedStatesNavigateDupKey_sharedState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()

        val backStack = mutableStateListOf(key1, key2)
        val entryDecorators =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> stateMap.remove(contentKey) }
                ) { entry ->
                    stateMap.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )
        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }
        backStack.add(key2) // add 2 again

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")
    }

    @Test
    fun singleBackStack_hoistedStatesPopDupKey_statePreserved() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)
        var popCalled = false
        val stateMap = mutableMapOf<Int, String>()

        val backStack = mutableStateListOf(key1, key2, key2)
        val entryDecorators =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey ->
                        popCalled = true
                        stateMap.remove(contentKey)
                    }
                ) { entry ->
                    stateMap.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )
        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }

        backStack.removeLastOrNull() // pop duplicate 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state") // state for 2 is preserved
        assertThat(popCalled).isFalse()
    }

    @Test
    fun singleBackStack_hoistedStatesPopAllDupKeys_stateCleared() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()

        val backStack = mutableStateListOf(key1, key2, key2)
        val entryDecorators =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> stateMap.remove(contentKey) }
                ) { entry ->
                    stateMap.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )
        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            val entries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }

        // pop all 2's
        backStack.clear()
        backStack.add(key1)

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(key1)
        assertThat(stateMap).containsExactly(1 to "state")
    }

    @Test
    fun singleBackStack_swapDecorators_previousStatePreserved() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val backStack = mutableStateListOf(key1, key2)
        val decoratorState = mutableStateOf(1)

        val previousState = mutableMapOf<Int, String>()
        val previousDecorator =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> previousState.remove(contentKey) }
                ) { entry ->
                    previousState.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )

        val newState = mutableMapOf<Int, String>()
        val newDecorator =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> newState.remove(contentKey) }
                ) { entry ->
                    newState.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )
        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            val decorator = if (decoratorState.value == 1) previousDecorator else newDecorator
            val entries = rememberDecoratedNavEntries(backStack, decorator, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(previousState).containsExactly(2 to "state") }

        decoratorState.value = 2

        composeTestRule.waitForIdle()
        assertThat(previousState).containsExactly(2 to "state")
        assertThat(newState).containsExactly(2 to "state")
    }

    @Test
    fun singleBackStack_swapDecoratorsThenPop_stateCleared() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val backStack = mutableStateListOf(key1, key2)
        val decoratorState = mutableStateOf(1)

        val previousState = mutableMapOf<Int, String>()
        val previousDecorator =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> previousState.remove(contentKey) }
                ) { entry ->
                    previousState.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )

        val newState = mutableMapOf<Int, String>()
        val newDecorator =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> newState.remove(contentKey) }
                ) { entry ->
                    newState.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )
        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            val decorator = if (decoratorState.value == 1) previousDecorator else newDecorator
            val entries = rememberDecoratedNavEntries(backStack, decorator, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(previousState).containsExactly(2 to "state") }

        decoratorState.value = 2
        backStack.removeLastOrNull()

        composeTestRule.waitForIdle()
        assertThat(previousState).containsExactly(2 to "state")
        assertThat(newState).containsExactly(1 to "state")
    }

    @Test
    fun singleBackStack_swapDecoratorStateThenPop_stateCleared() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val backStack = mutableStateListOf(key1, key2)
        val decoratorStateState = mutableStateOf(1)
        val previousState = mutableMapOf<Int, String>()
        val newState = mutableMapOf<Int, String>()
        lateinit var currentState: MutableMap<Int, String>

        val entryDecorator =
            listOf(
                createTestNavEntryDecorator<Any>(
                    onPop = { contentKey -> currentState.remove(contentKey) }
                ) { entry ->
                    currentState.put(entry.contentKey as Int, "state")
                    entry.Content()
                }
            )

        val entryProvider: (key: Any) -> NavEntry<out Any> = entryProvider {
            entry<DataClass>({ it.arg }) {}
        }
        composeTestRule.setContent {
            currentState = if (decoratorStateState.value == 1) previousState else newState
            val entries = rememberDecoratedNavEntries(backStack, entryDecorator, entryProvider)
            entries.lastOrNull()?.Content()
        }

        composeTestRule.runOnIdle { assertThat(previousState).containsExactly(2 to "state") }

        decoratorStateState.value = 2
        backStack.removeLastOrNull()

        composeTestRule.waitForIdle()
        assertThat(previousState).containsExactly(2 to "state")
        assertThat(newState).containsExactly(1 to "state")
    }

    @Test
    fun multipleBackStack_swapDecoratedEntries_previousStatePreserved() {
        val backStack1 = mutableStateListOf(1)
        val backStack2 = mutableStateListOf(2)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(2) {} },
                )

            val entries = if (backStackState.value == 1) entries1 else entries2
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())

        // swap to second list of entries
        backStackState.value = 2

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(2.toString())
    }

    @Test
    fun multipleBackStack_swapDecoratedEntriesThenPop_stateCleared() {
        val backStack1 = mutableStateListOf(1, 2)
        val backStack2 = mutableStateListOf(3, 4)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(1) {}
                        entry(2) {}
                    },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(3) {}
                        entry(4) {}
                    },
                )

            val entries = if (backStackState.value == 1) entries1 else entries2
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(2.toString())

        // swap to second list of entries
        backStackState.value = 2

        composeTestRule.waitForIdle()
        assertThat(decoratorState2).containsExactly(4.toString())

        // pop last entry
        backStack2.removeLastOrNull()

        composeTestRule.waitForIdle()
        assertThat(decoratorState2).containsExactly(3.toString())

        // swap back to first list of entries
        backStackState.value = 1

        // pop last entry
        backStack1.removeLastOrNull()

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
    }

    @Test
    fun multipleBackStack_concatenateDecoratedEntries() {
        val backStack1 = mutableStateListOf(1)
        val backStack2 = mutableStateListOf(2)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(2) {} },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else if (backStackState.value == 2) {
                    entries2
                } else if (backStackState.value == 3) {
                    entries1 + entries2
                } else {
                    entries2 + entries1
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())

        // swap to second list of entries
        backStackState.value = 2

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(2.toString())

        // concat both lists
        backStackState.value = 3
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        // entry 2 is invoked again
        assertThat(decoratorState2).containsExactly(2.toString(), 2.toString())

        // concat in reverse order
        backStackState.value = 4
        composeTestRule.waitForIdle()
        // entry 1 is invoked again
        assertThat(decoratorState1).containsExactly(1.toString(), 1.toString())
        assertThat(decoratorState2).containsExactly(2.toString(), 2.toString())
    }

    @Test
    fun multipleBackStack_concatenateDecoratedEntries_navigateOnFirstStack() {
        val backStack1 = mutableStateListOf(1)
        val backStack2 = mutableStateListOf(2)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(1) {}
                        entry(3) {}
                    },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(2) {} },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries1 + entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())

        // concat both lists
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(2.toString())

        // navigate on first stack, new entry not rendered
        backStack1.add(3)
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        // last rendered entry is rendered again
        assertThat(decoratorState2).containsExactly(2.toString(), 2.toString())
    }

    @Test
    fun multipleBackStack_concatenateDecoratedEntries_navigateOnLastStack() {
        val backStack1 = mutableStateListOf(1)
        val backStack2 = mutableStateListOf(2)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(2) {}
                        entry(3) {}
                    },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries1 + entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())

        // concat both lists
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(2.toString())

        // navigate on second stack, new entry rendered
        backStack2.add(3)
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(2.toString(), 3.toString())
    }

    @Test
    fun multipleBackStack_concatenateDecoratedEntries_popOnFirstStack() {
        val backStack1 = mutableStateListOf(1, 2)
        val backStack2 = mutableStateListOf(3)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(1) {}
                        entry(2) {}
                    },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(3) {} },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries1 + entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(2.toString())

        // concat both lists
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(2.toString())
        assertThat(decoratorState2).containsExactly(3.toString())

        // pop on first stack, state cleared but the entry below it is not rendered
        backStack1.removeLastOrNull()
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).isEmpty()
        // last rendered entry on second stack is rendered again
        assertThat(decoratorState2).containsExactly(3.toString(), 3.toString())
    }

    @Test
    fun multipleBackStack_concatenateDecoratedEntries_popOnLastStack() {
        val backStack1 = mutableStateListOf(1)
        val backStack2 = mutableStateListOf(2, 3)
        val decoratorState1 = mutableListOf<Any>()
        val decoratorState2 = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    backStack1,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState1.remove(it) }) {
                            decoratorState1.add(it.contentKey)
                        }
                    ),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    backStack2,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState2.remove(it) }) {
                            decoratorState2.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(2) {}
                        entry(3) {}
                    },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries1 + entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())

        // concat both lists
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        assertThat(decoratorState2).containsExactly(3.toString())

        // pop on second stack, state cleared and the entry below it is rendered
        backStack2.removeLastOrNull()
        composeTestRule.waitForIdle()
        assertThat(decoratorState1).containsExactly(1.toString())
        // entry below is rendered
        assertThat(decoratorState2).containsExactly(2.toString())
    }

    @Test
    fun multipleBackStack_immutableBackStack_navigate() {
        val secondStackFirst = listOf(2)
        val secondStackSecond = listOf(2, 3)
        val decoratorState = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)
        val secondStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    mutableStateListOf(1),
                    listOf(),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    if (secondStackState.value == 1) secondStackFirst else secondStackSecond,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState.remove(it) }) {
                            decoratorState.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(2) {}
                        entry(3) {}
                    },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()

        // swap to second stack
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState).containsExactly(2.toString())

        // navigate on second stack with immutable backStack
        secondStackState.value = 2
        composeTestRule.waitForIdle()
        // entry below is rendered
        assertThat(decoratorState).containsExactly(2.toString(), 3.toString())
    }

    @Test
    fun multipleBackStack_immutableBackStack_pop() {
        val secondStackFirst = listOf(2, 3)
        val secondStackSecond = listOf(2)
        val decoratorState = mutableListOf<Any>()

        val backStackState = mutableStateOf(1)
        val secondStackState = mutableStateOf(1)

        composeTestRule.setContent {
            val entries1 =
                rememberDecoratedNavEntries(
                    mutableStateListOf(1),
                    listOf(),
                    entryProvider { entry(1) {} },
                )
            val entries2 =
                rememberDecoratedNavEntries(
                    if (secondStackState.value == 1) secondStackFirst else secondStackSecond,
                    listOf(
                        navEntryDecorator<Int>(onPop = { it -> decoratorState.remove(it) }) {
                            decoratorState.add(it.contentKey)
                        }
                    ),
                    entryProvider {
                        entry(2) {}
                        entry(3) {}
                    },
                )

            val entries =
                if (backStackState.value == 1) {
                    entries1
                } else {
                    entries2
                }
            entries.last().Content()
        }

        composeTestRule.waitForIdle()

        // swap to second stack
        backStackState.value = 2
        composeTestRule.waitForIdle()
        assertThat(decoratorState).containsExactly(3.toString())

        // navigate on second stack with immutable backStack
        secondStackState.value = 2
        composeTestRule.waitForIdle()
        // entry below is rendered
        assertThat(decoratorState).containsExactly(2.toString())
    }

    private data class DataClass(val arg: Int)
}

@Composable
private fun <T : Any> WithDecoratedNavEntries(
    backStack: List<T>,
    entryDecorators: List<@JvmSuppressWildcards NavEntryDecorator<*>> = listOf(),
    entryProvider: (key: T) -> NavEntry<out T>,
    content: @Composable (List<NavEntry<T>>) -> Unit,
) {
    val decoratedEntries = rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)
    content(decoratedEntries)
}
