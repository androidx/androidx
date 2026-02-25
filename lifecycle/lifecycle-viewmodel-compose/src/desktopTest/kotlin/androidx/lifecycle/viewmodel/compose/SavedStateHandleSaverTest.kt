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

package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.kruth.assertThat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals

@OptIn(
    ExperimentalTestApi::class,
    ExperimentalComposeUiApi::class,
    SavedStateHandleSaveableApi::class,
)
class SavedStateHandleSaverTest {

    @Test
    fun simpleRestore() = runComposeUiTestOnUiThread {
        var array: IntArray? = null

        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertContentEquals(intArrayOf(0), array)

        val savedState = window1.saveState()
        window1.dispose()

        array!![0] = 1
        // we null it to ensure recomposition happened
        array = null

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertContentEquals(intArrayOf(1), array)
        window2.dispose()
    }

    private class CustomStateHolder(initialValue: Int) {
        var value by mutableStateOf(initialValue)

        companion object {
            val Saver: Saver<CustomStateHolder, *> =
                Saver(save = { it.value }, restore = { CustomStateHolder(it) })
        }
    }

    @Test
    fun customStateHolder_simpleRestore() = runComposeUiTestOnUiThread {
        var stateHolder: CustomStateHolder? = null
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel = viewModel<SavingTestViewModel>(
                    factory = SavingTestViewModel.factory()
                )
                stateHolder = viewModel.savedStateHandle.saveable(
                    key = "key",
                    saver = CustomStateHolder.Saver,
                ) {
                    CustomStateHolder(0)
                }
            }
        }

        assertThat(stateHolder?.value).isEqualTo(0)

        stateHolder!!.value = 1
        // we null it to ensure recomposition happened
        stateHolder = null

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                stateHolder =
                    viewModel.savedStateHandle.saveable(
                        key = "key",
                        saver = CustomStateHolder.Saver,
                    ) {
                        CustomStateHolder(0)
                    }
            }
        }

        assertThat(stateHolder?.value).isEqualTo(1)
        window2.dispose()
    }

    private data class CustomState(val value: Int) {
        companion object {
            val Saver: Saver<CustomState, *> =
                Saver(save = { it.value }, restore = { CustomState(it) })
        }
    }

    @Test
    fun mutableState_simpleRestore() = runComposeUiTestOnUiThread {
        var state: MutableState<CustomState>? = null

        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel = viewModel<SavingTestViewModel>(
                    factory = SavingTestViewModel.factory()
                )
                state =
                    viewModel.savedStateHandle.saveable(
                        key = "key",
                        stateSaver = CustomState.Saver,
                    ) {
                        mutableStateOf(CustomState(0))
                    }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(0))

        state!!.value = CustomState(1)
        // we null it to ensure recomposition happened
        state = null

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                state =
                    viewModel.savedStateHandle.saveable(
                        key = "key",
                        stateSaver = CustomState.Saver,
                    ) {
                        mutableStateOf(CustomState(0))
                    }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(1))
        window2.dispose()
    }

    @Test
    fun mutableState_restoreReferentialEqualityPolicy() = runComposeUiTestOnUiThread {
        var state: MutableState<CustomState>? = null
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                state =
                    viewModel.savedStateHandle.saveable(
                        key = "key",
                        stateSaver = CustomState.Saver,
                    ) {
                        mutableStateOf(CustomState(0), referentialEqualityPolicy())
                    }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(0))

        state!!.value = CustomState(1)
        // we null it to ensure recomposition happened
        state = null


        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                state =
                    viewModel.savedStateHandle.saveable(
                        key = "key",
                        stateSaver = CustomState.Saver,
                    ) {
                        mutableStateOf(CustomState(0), referentialEqualityPolicy())
                    }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(1))
        assertThat((state as SnapshotMutableState).policy)
            .isEqualTo(referentialEqualityPolicy<CustomState>())
        window2.dispose()
    }

    @Test
    fun delegate_simpleRestore() = runComposeUiTestOnUiThread {
        var savedStateHandle: SavedStateHandle? = null
        var array: IntArray? = null
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                val arrayProperty: IntArray by
                viewModel.savedStateHandle.saveable<IntArray> { intArrayOf(0) }
                array = arrayProperty
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("arrayProperty"))

        array!![0] = 1
        // we null both to ensure recomposition happened
        array = null
        savedStateHandle = null

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                val arrayProperty: IntArray by
                viewModel.savedStateHandle.saveable<IntArray> { intArrayOf(0) }
                array = arrayProperty
            }
        }

        assertThat(array).isEqualTo(intArrayOf(1))
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("arrayProperty"))
        window2.dispose()
    }

    @Test
    fun mutableState_delegate_simpleRestore() = runComposeUiTestOnUiThread {
        var savedStateHandle: SavedStateHandle? = null
        var getCount: (() -> Int)? = null
        var setCount: ((Int) -> Unit)? = null
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                var count by viewModel.savedStateHandle.saveable { mutableStateOf(0) }
                getCount = { count }
                setCount = { count = it }
            }
        }

        assertThat(getCount!!()).isEqualTo(0)
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))

        setCount!!(1)
        // we null all to ensure recomposition happened
        getCount = null
        setCount = null
        savedStateHandle = null

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                var count by viewModel.savedStateHandle.saveable { mutableStateOf(0) }
                getCount = { count }
                setCount = { count = it }
            }
        }

        assertThat(getCount!!()).isEqualTo(1)
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))
        window2.dispose()
    }

    @Test
    fun nullableMutableState_delegate_simpleRestore() = runComposeUiTestOnUiThread {
        var savedStateHandle: SavedStateHandle? = null
        var getOptionalCount: (() -> Int?)? = null
        var setOptionalCount: ((Int?) -> Unit)? = null
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                var count: Int? by viewModel.savedStateHandle.saveable { mutableStateOf(null) }
                getOptionalCount = { count }
                setOptionalCount = { count = it }
            }
        }

        assertThat(getOptionalCount!!()).isNull()
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))

        setOptionalCount!!(1)
        // we null all to ensure recomposition happened
        getOptionalCount = null
        setOptionalCount = null
        savedStateHandle = null

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                savedStateHandle = viewModel.savedStateHandle
                var count: Int? by viewModel.savedStateHandle.saveable { mutableStateOf(null) }
                getOptionalCount = { count }
                setOptionalCount = { count = it }
            }
        }

        assertThat(getOptionalCount!!()).isEqualTo(1)
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))
        window2.dispose()
    }

    @Test
    fun noConflictKeys_delegate_simpleRestore() = runComposeUiTestOnUiThread {
        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                val firstClass = FirstClass(viewModel.savedStateHandle)
                val secondClass = SecondClass(viewModel.savedStateHandle)
                assertThat(firstClass.savedProperty).isEqualTo("One")
                assertThat(secondClass.savedProperty).isEqualTo("Two")
            }
        }

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val viewModel =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory())
                val firstClass = FirstClass(viewModel.savedStateHandle)
                val secondClass = SecondClass(viewModel.savedStateHandle)
                assertThat(firstClass.savedProperty).isEqualTo("One")
                assertThat(secondClass.savedProperty).isEqualTo("Two")
            }
        }
        window2.dispose()
    }

    @Test
    fun conflictKeys_local_delegate_simpleRestore() = runComposeUiTestOnUiThread {
        fun firstFunction(handle: SavedStateHandle): String {
            val localProperty by handle.saveable { mutableStateOf("One") }
            return localProperty
        }

        fun secondFunction(handle: SavedStateHandle): String {
            val localProperty by handle.saveable { mutableStateOf("Two") }
            return localProperty
        }

        val window1 = ComposeWindow().apply {
            isVisible = true
            setContent {
                val savedStateHandle =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory()).savedStateHandle
                firstFunction(savedStateHandle)
                secondFunction(savedStateHandle)
            }
        }

        val savedState = window1.saveState()
        window1.dispose()

        val window2 = ComposeWindow(savedState = savedState).apply {
            isVisible = true
            setContent {
                val savedStateHandle =
                    viewModel<SavingTestViewModel>(factory = SavingTestViewModel.factory()).savedStateHandle
                // TODO(b/331695354): Fix local property saveable delegate key conflict
                assertThat(firstFunction(savedStateHandle)).isEqualTo("Two")
                assertThat(secondFunction(savedStateHandle)).isEqualTo("Two")
            }
        }
        window2.dispose()
    }
}

class SavingTestViewModel(val savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val savedStateHandle = createSavedStateHandle()
                SavingTestViewModel(savedStateHandle)
            }
        }
    }
}

class FirstClass(savedStateHandle: SavedStateHandle) {
    @OptIn(SavedStateHandleSaveableApi::class)
    val savedProperty by savedStateHandle.saveable { mutableStateOf("One") }
}

class SecondClass(savedStateHandle: SavedStateHandle) {
    @OptIn(SavedStateHandleSaveableApi::class)
    val savedProperty by savedStateHandle.saveable { mutableStateOf("Two") }
}

@OptIn(ExperimentalTestApi::class)
private fun runComposeUiTestOnUiThread(block: ComposeUiTest.() -> Unit) {
    runComposeUiTest {
        runOnUiThread { block() }
    }
}