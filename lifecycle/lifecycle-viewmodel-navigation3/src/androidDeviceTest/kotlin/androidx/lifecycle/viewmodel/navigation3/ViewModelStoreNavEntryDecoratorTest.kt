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

package androidx.lifecycle.viewmodel.navigation3

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ViewModelStoreNavEntryDecoratorTest {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @Test
    fun testViewModelProvided() {
        lateinit var savedStateWrapper: NavEntryDecorator<String>
        lateinit var viewModelWrapper: NavEntryDecorator<String>
        lateinit var viewModel1: MyViewModel
        lateinit var viewModel2: MyViewModel
        val entry1Arg = "entry1 Arg"
        val entry2Arg = "entry2 Arg"
        val entry1 =
            NavEntry("key1") {
                viewModel1 = viewModel<MyViewModel>()
                viewModel1.myArg = entry1Arg
            }
        val entry2 =
            NavEntry("key2") {
                viewModel2 = viewModel<MyViewModel>()
                viewModel2.myArg = entry2Arg
            }
        composeTestRule.setContent {
            savedStateWrapper = rememberSaveableStateHolderNavEntryDecorator()
            viewModelWrapper = rememberViewModelStoreNavEntryDecorator()

            rememberDecoratedNavEntries(
                    listOf(entry1, entry2),
                    listOf(savedStateWrapper, viewModelWrapper),
                )
                .forEach { it.Content() }
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Incorrect arg for entry 1")
                .that(viewModel1.myArg)
                .isEqualTo(entry1Arg)
            assertWithMessage("Incorrect arg for entry 2")
                .that(viewModel2.myArg)
                .isEqualTo(entry2Arg)
        }
    }

    @Test
    fun testViewModelNoSavedStateNavEntryDecorator() {
        lateinit var viewModelWrapper: NavEntryDecorator<String>
        lateinit var viewModel1: MyViewModel
        val entry1Arg = "entry1 Arg"
        val entry1 =
            NavEntry("key1") {
                viewModel1 = viewModel<MyViewModel>()
                viewModel1.myArg = entry1Arg
            }
        try {
            composeTestRule.setContent {
                viewModelWrapper = rememberViewModelStoreNavEntryDecorator()

                rememberDecoratedNavEntries(listOf(entry1), listOf(viewModelWrapper)).forEach {
                    it.Content()
                }
            }
        } catch (e: Exception) {
            with(assertThat(e.message)) {
                // Assert the static parts of the new error message
                contains("Failed to enable `SavedStateHandle` for `")
                contains("`. The `Lifecycle.State` must be `INITIALIZED` or `CREATED`, but was `")
                contains(
                    "`. You must call `enableSavedStateHandles()` before the `Lifecycle.State` moves to `STARTED`."
                )
            }
        }
    }

    @Test
    fun testViewModelSaved() {
        lateinit var backStack: MutableList<Any>
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf("Home") }
            NavDisplay(
                backStack = backStack,
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    "Home" -> {
                        NavEntry(key) { viewModel<HomeViewModel>() }
                    }
                    "AnotherScreen" -> {
                        NavEntry(key) { viewModel<HomeViewModel>() }
                    }
                    else -> error("Unknown key: $key")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertWithMessage("Only one viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(1)

        composeTestRule.runOnIdle { backStack.add("AnotherScreen") }

        composeTestRule.waitForIdle()
        assertWithMessage("Another viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(2)

        composeTestRule.runOnIdle { backStack.removeAt(backStack.lastIndex) }

        composeTestRule.waitForIdle()
        assertWithMessage("We should be reusing the old ViewModel since we popped")
            .that(globalViewModelCount)
            .isEqualTo(2)

        composeTestRule.runOnIdle { backStack.add("AnotherScreen") }

        composeTestRule.waitForIdle()
        assertWithMessage("Another viewModel should be created")
            .that(globalViewModelCount)
            .isEqualTo(3)
    }

    @Test
    fun testViewModelSavedStateHandle() {
        lateinit var backStack: MutableList<Any>
        lateinit var viewModel: SavedStateViewModel
        composeTestRule.setContent {
            backStack = remember { mutableStateListOf("Home") }
            NavDisplay(
                backStack = backStack,
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                onBack = { backStack.removeAt(backStack.lastIndex) },
            ) { key ->
                when (key) {
                    "Home" -> {
                        NavEntry(key) {
                            viewModel =
                                viewModel<SavedStateViewModel> {
                                    val handle = createSavedStateHandle()
                                    SavedStateViewModel(handle)
                                }
                        }
                    }
                    else -> error("Unknown key: $key")
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(viewModel.savedStateHandle).isNotNull()
    }

    @Test
    fun testViewModelRemovedOnPop() {
        val viewModels = mutableMapOf<Int, MyViewModel>()

        fun createNaveEntry(key: Int) = NavEntry(key) { viewModels[key] = viewModel<MyViewModel>() }

        val entry1 = createNaveEntry(1)
        val entry2 = createNaveEntry(2)
        val entry3 = createNaveEntry(3)
        val backStack = mutableStateListOf(entry1, entry2, entry3)

        composeTestRule.setContent {
            val decorated =
                rememberDecoratedNavEntries(
                    entries = backStack,
                    entryDecorators =
                        listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                        ),
                )

            decorated.forEach { entry -> entry.Content() }
        }

        assertThat(viewModels.mapValues { (_, viewModel) -> viewModel.isCleared })
            .isEqualTo(mapOf(1 to false, 2 to false, 3 to false))

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()
        assertThat(viewModels.mapValues { (_, viewModel) -> viewModel.isCleared })
            .isEqualTo(mapOf(1 to false, 2 to false, 3 to true))

        backStack.removeAt(backStack.lastIndex)
        composeTestRule.waitForIdle()
        assertThat(viewModels.mapValues { (_, viewModel) -> viewModel.isCleared })
            .isEqualTo(mapOf(1 to false, 2 to true, 3 to true))
    }
}

class MyViewModel : ViewModel() {
    var myArg = "default"
    var isCleared = false
        private set

    override fun onCleared() {
        isCleared = true
    }
}

var globalViewModelCount = 0

class HomeViewModel : ViewModel() {
    init {
        globalViewModelCount++
    }
}

class SavedStateViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()
