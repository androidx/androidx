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

import android.annotation.SuppressLint
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
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DecoratedNavEntryProviderTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun callDecorator() {
        var calledWrapContent = false

        val decorator = createTestNavEntryDecorator<Any> { entry -> calledWrapContent = true }

        composeTestRule.setContent {
            DecoratedNavEntryProvider(
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
    fun callDecoratorOnce() {
        var calledWrapContentCount = 0

        val decorator = createTestNavEntryDecorator<Any> { entry -> calledWrapContentCount++ }

        composeTestRule.setContent {
            DecoratedNavEntryProvider(
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
    fun nestedDecoratorsCallOrder() {
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
            DecoratedNavEntryProvider(
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
    fun decoratorsOnPop() {
        val poppedEntries = mutableListOf<Int>()
        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { key -> poppedEntries.add(key as Int) }) {
                entry ->
                entry.Content()
            }
        lateinit var backStack: SnapshotStateList<Int>
        composeTestRule.setContent {
            backStack = mutableStateListOf(1, 2)
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1, 1) {}
                        2 -> NavEntry(2, 2) {}
                        3 -> NavEntry(3, 3) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.let { it.Content() }
            }
        }
        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        composeTestRule.waitForIdle()
        assertThat(poppedEntries).containsExactly(2)

        backStack.add(3)

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1, 3).inOrder()

        backStack.removeAt(backStack.lastIndex)

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1)
        assertThat(poppedEntries).containsExactly(2, 3).inOrder()
    }

    @Test
    fun decoratorsOnPopImmutableBackStack() {
        val poppedEntries = mutableListOf<Int>()
        lateinit var backStackState: MutableState<Int>
        lateinit var backStack: List<Int>

        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { key -> poppedEntries.add(key as Int) }) {
                entry ->
                entry.Content()
            }
        composeTestRule.setContent {
            backStackState = remember { mutableStateOf(2) }
            backStack =
                when (backStackState.value) {
                    1 -> {
                        listOf(1)
                    }
                    2 -> {
                        listOf(1, 2)
                    }
                    else -> listOf(1, 3)
                }
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = { key ->
                    when (key) {
                        1 -> NavEntry(1, 1) {}
                        2 -> NavEntry(2, 2) {}
                        3 -> NavEntry(3, 3) {}
                        else -> error("Invalid Key")
                    }
                },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }
        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1, 2).inOrder()

        backStackState.value = 1 // pop 2

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1).inOrder()
        assertThat(poppedEntries).containsExactly(2)

        backStackState.value = 3 // add 3

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1, 3).inOrder()

        backStackState.value = 1 // pop 3

        composeTestRule.waitForIdle()
        assertThat(backStack).containsExactly(1).inOrder()
        assertThat(poppedEntries).containsExactly(2, 3).inOrder()
    }

    @Test
    fun decoratorsOnPopOrder() {
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
            backStack = mutableStateListOf(1, 2)
            DecoratedNavEntryProvider(
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
    fun decoratorsOnPopForNeverRenderedEntries() {
        val entriesOnPop = mutableListOf<String>()
        val entriesRendered = mutableListOf<String>()

        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { key -> entriesOnPop.add(key as String) }) {
                entry ->
                entry.Content()
            }
        lateinit var backStack: SnapshotStateList<String>
        composeTestRule.setContent {
            backStack = mutableStateListOf("first", "second", "third")
            DecoratedNavEntryProvider(
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
    fun onPopCalledForNewlyAddedDecorator() {
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
            DecoratedNavEntryProvider(
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
    fun onPopNotCalledForRemovedDecorator() {
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
            DecoratedNavEntryProvider(
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
    fun removedDecoratorStateNotPopped() {
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
            DecoratedNavEntryProvider(
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
    fun removedDecoratorStateNotPoppedForPoppedEntry() {
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
            DecoratedNavEntryProvider(
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
    fun entryReWrappedWithNewlyAddedDecorator() {
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
            DecoratedNavEntryProvider(
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
    fun entryReWrappedWithRemovedDecorator() {
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
            DecoratedNavEntryProvider(
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
    fun decoratorsWrapAddedEntry() {
        val wrappedEntries = mutableListOf<String>()
        val decorator =
            createTestNavEntryDecorator<String> {
                wrappedEntries.add(it.contentKey as String)
                it.Content()
            }
        val backStack = mutableStateListOf("first")
        composeTestRule.setContent {
            DecoratedNavEntryProvider(
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
    fun differingContentKeyStateClearedWhenPopped() {
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
            DecoratedNavEntryProvider(
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
    fun duplicateContentKeyStateNotClearedWhenPopped() {
        var state = 1
        val decorator =
            createTestNavEntryDecorator<Any>(onPop = { state = -1 }) { entry -> entry.Content() }
        lateinit var backStack: SnapshotStateList<Int>
        composeTestRule.setContent {
            backStack = mutableStateListOf(1, 2, 2)
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
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
        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        composeTestRule.waitForIdle()
        assertThat(state).isEqualTo(1)
    }

    @Test
    fun swappingBackStackPopsSharedDecoratorState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        val decorator =
            createTestNavEntryDecorator<Any>(
                onPop = { contentKey -> stateMap.remove(contentKey) }
            ) { entry ->
                stateMap.put(entry.contentKey as Int, "state")
                entry.Content()
            }
        lateinit var backStackState: MutableState<Any>

        composeTestRule.setContent {
            val backStack1 = mutableStateListOf(key1)
            val backStack2 = mutableStateListOf(key2)
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> backStack1
                    else -> backStack2
                }
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(1 to "state") }

        backStackState.value = 2 // popped 1

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")

        backStackState.value = 1 // popped 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")
    }

    @Test
    fun swappingBackStackPreservesSwappedDecoratorState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap1 = mutableMapOf<Int, String>()
        val stateMap2 = mutableMapOf<Int, String>()

        val backStack1 = mutableStateListOf(key1)
        val backStack2 = mutableStateListOf(key2)

        lateinit var backStackState: MutableState<Any>
        lateinit var currStateMap: MutableState<MutableMap<Int, String>>

        val decorator =
            createTestNavEntryDecorator<Any>(
                onPop = { contentKey -> currStateMap.value.remove(contentKey) }
            ) { entry ->
                currStateMap.value.put(entry.contentKey as Int, "state")
                entry.Content()
            }

        @SuppressLint("MutableCollectionMutableState")
        composeTestRule.setContent {
            currStateMap = remember { mutableStateOf(stateMap1) }
            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> backStack1
                    else -> backStack2
                }
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }

        composeTestRule.runOnIdle { assertThat(stateMap1).containsExactly(1 to "state") }

        backStackState.value = 2
        currStateMap.value = stateMap2

        composeTestRule.waitForIdle()
        assertThat(stateMap1).containsExactly(1 to "state")
        assertThat(stateMap2).containsExactly(2 to "state")

        backStackState.value = 1
        currStateMap.value = stateMap1

        composeTestRule.waitForIdle()
        assertThat(stateMap1).containsExactly(1 to "state")
        assertThat(stateMap2).containsExactly(2 to "state")
    }

    @Test
    fun popDuplicateWithImmutableBackStackPreservesState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        val decorator =
            createTestNavEntryDecorator<Any>(
                onPop = { contentKey -> stateMap.remove(contentKey) }
            ) { entry ->
                stateMap.put(entry.contentKey as Int, "state")
                entry.Content()
            }
        lateinit var backStackState: MutableState<Any>

        composeTestRule.setContent {
            val backStack1 = mutableStateListOf(key1, key2)
            val backStack2 = mutableStateListOf(key1, key2, key2)

            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> backStack1
                    else -> backStack2
                }
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }
        backStackState.value = 2 // add 2 again

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")

        backStackState.value = 1 // pop duplicate 2

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")
    }

    @Test
    fun popAllDuplicatesWithImmutableBackStackClearsState() {
        val key1 = DataClass(1)
        val key2 = DataClass(2)

        val stateMap = mutableMapOf<Int, String>()
        val decorator =
            createTestNavEntryDecorator<Any>(
                onPop = { contentKey -> stateMap.remove(contentKey) }
            ) { entry ->
                stateMap.put(entry.contentKey as Int, "state")
                entry.Content()
            }
        lateinit var backStackState: MutableState<Any>

        composeTestRule.setContent {
            val backStack1 = mutableStateListOf(key1, key2)
            val backStack2 = mutableStateListOf(key1, key2, key2)
            val backStack3 = mutableStateListOf(key1)

            backStackState = remember { mutableStateOf(1) }
            val backStack =
                when (backStackState.value) {
                    1 -> backStack1
                    2 -> backStack2
                    else -> backStack3
                }
            DecoratedNavEntryProvider(
                backStack = backStack,
                entryDecorators = listOf(decorator),
                entryProvider = entryProvider { entry<DataClass>({ it.arg }) {} },
            ) { entries ->
                entries.lastOrNull()?.Content()
            }
        }

        composeTestRule.runOnIdle { assertThat(stateMap).containsExactly(2 to "state") }
        backStackState.value = 2 // add 2 again

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(2 to "state")

        backStackState.value = 3 // pop both 2's

        composeTestRule.waitForIdle()
        assertThat(stateMap).containsExactly(1 to "state")
    }

    private data class DataClass(val arg: Int)
}
