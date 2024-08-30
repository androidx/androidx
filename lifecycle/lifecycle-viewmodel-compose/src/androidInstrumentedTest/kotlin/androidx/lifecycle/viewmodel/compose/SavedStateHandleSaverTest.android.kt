/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SavedStateHandleSaverTest {

    @get:Rule
    public val activityTestRuleScenario: ActivityScenarioRule<AppCompatActivity> =
        ActivityScenarioRule(AppCompatActivity::class.java)

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun simpleRestore() {
        var array: IntArray? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))

        activityTestRuleScenario.scenario.onActivity {
            array!![0] = 1
            // we null it to ensure recomposition happened
            array = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                array = viewModel.savedStateHandle.saveable<IntArray>("key") { intArrayOf(0) }
            }
        }

        assertThat(array).isEqualTo(intArrayOf(1))
    }

    private class CustomStateHolder(
        initialValue: Int
    ) {
        var value by mutableStateOf(initialValue)

        companion object {
            val Saver: Saver<CustomStateHolder, *> = Saver(
                save = { it.value },
                restore = { CustomStateHolder(it) }
            )
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun customStateHolder_simpleRestore() {
        var stateHolder: CustomStateHolder? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                stateHolder = viewModel.savedStateHandle.saveable(
                    key = "key",
                    saver = CustomStateHolder.Saver
                ) {
                    CustomStateHolder(0)
                }
            }
        }

        assertThat(stateHolder?.value).isEqualTo(0)

        activityTestRuleScenario.scenario.onActivity {
            stateHolder!!.value = 1
            // we null it to ensure recomposition happened
            stateHolder = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                stateHolder = viewModel.savedStateHandle.saveable(
                    key = "key",
                    saver = CustomStateHolder.Saver
                ) {
                    CustomStateHolder(0)
                }
            }
        }

        assertThat(stateHolder?.value).isEqualTo(1)
    }

    private data class CustomState(
        val value: Int
    ) {
        companion object {
            val Saver: Saver<CustomState, *> = Saver(
                save = { it.value },
                restore = { CustomState(it) }
            )
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun mutableState_simpleRestore() {
        var state: MutableState<CustomState>? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                state = viewModel.savedStateHandle.saveable(
                    key = "key",
                    stateSaver = CustomState.Saver
                ) {
                    mutableStateOf(CustomState(0))
                }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(0))

        activityTestRuleScenario.scenario.onActivity {
            state!!.value = CustomState(1)
            // we null it to ensure recomposition happened
            state = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                state = viewModel.savedStateHandle.saveable(
                    key = "key",
                    stateSaver = CustomState.Saver
                ) {
                    mutableStateOf(CustomState(0))
                }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(1))
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun mutableState_restoreReferentialEqualityPolicy() {
        var state: MutableState<CustomState>? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                state = viewModel.savedStateHandle.saveable(
                    key = "key",
                    stateSaver = CustomState.Saver
                ) {
                    mutableStateOf(CustomState(0), referentialEqualityPolicy())
                }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(0))

        activityTestRuleScenario.scenario.onActivity {
            state!!.value = CustomState(1)
            // we null it to ensure recomposition happened
            state = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                state = viewModel.savedStateHandle.saveable(
                    key = "key",
                    stateSaver = CustomState.Saver
                ) {
                    mutableStateOf(CustomState(0), referentialEqualityPolicy())
                }
            }
        }

        assertThat(state?.value).isEqualTo(CustomState(1))
        assertThat((state as SnapshotMutableState).policy)
            .isEqualTo(referentialEqualityPolicy<CustomState>())
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun delegate_simpleRestore() {
        var savedStateHandle: SavedStateHandle? = null
        var array: IntArray? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                savedStateHandle = viewModel.savedStateHandle
                val arrayProperty: IntArray by viewModel.savedStateHandle.saveable<IntArray> {
                    intArrayOf(0)
                }
                array = arrayProperty
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("arrayProperty"))

        activityTestRuleScenario.scenario.onActivity {
            array!![0] = 1
            // we null both to ensure recomposition happened
            array = null
            savedStateHandle = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                savedStateHandle = viewModel.savedStateHandle
                val arrayProperty: IntArray by viewModel.savedStateHandle.saveable<IntArray> {
                    intArrayOf(0)
                }
                array = arrayProperty
            }
        }

        assertThat(array).isEqualTo(intArrayOf(1))
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("arrayProperty"))
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun mutableState_delegate_simpleRestore() {
        var savedStateHandle: SavedStateHandle? = null
        var getCount: (() -> Int)? = null
        var setCount: ((Int) -> Unit)? = null
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                savedStateHandle = viewModel.savedStateHandle
                var count by viewModel.savedStateHandle.saveable {
                    mutableStateOf(0)
                }
                getCount = { count }
                setCount = { count = it }
            }
        }

        assertThat(getCount!!()).isEqualTo(0)
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))

        activityTestRuleScenario.scenario.onActivity {
            setCount!!(1)
            // we null all to ensure recomposition happened
            getCount = null
            setCount = null
            savedStateHandle = null
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                savedStateHandle = viewModel.savedStateHandle
                var count by viewModel.savedStateHandle.saveable {
                    mutableStateOf(0)
                }
                getCount = { count }
                setCount = { count = it }
            }
        }

        assertThat(getCount!!()).isEqualTo(1)
        assertThat(savedStateHandle?.keys()).isEqualTo(setOf("count"))
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun noConflictKeys_delegate_simpleRestore() {
        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                val firstClass = FirstClass(viewModel.savedStateHandle)
                val secondClass = SecondClass(viewModel.savedStateHandle)
                assertThat(firstClass.savedProperty).isEqualTo("One")
                assertThat(secondClass.savedProperty).isEqualTo("Two")
            }
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val viewModel = viewModel<SavingTestViewModel>(activity)
                val firstClass = FirstClass(viewModel.savedStateHandle)
                val secondClass = SecondClass(viewModel.savedStateHandle)
                assertThat(firstClass.savedProperty).isEqualTo("One")
                assertThat(secondClass.savedProperty).isEqualTo("Two")
            }
        }
    }

    @OptIn(SavedStateHandleSaveableApi::class)
    @Test
    fun conflictKeys_local_delegate_simpleRestore() {

        fun firstFunction(handle: SavedStateHandle): String {
            val localProperty by handle.saveable { mutableStateOf("One") }
            return localProperty
        }

        fun secondFunction(handle: SavedStateHandle): String {
            val localProperty by handle.saveable { mutableStateOf("Two") }
            return localProperty
        }

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val savedStateHandle = viewModel<SavingTestViewModel>(activity).savedStateHandle
                firstFunction(savedStateHandle)
                secondFunction(savedStateHandle)
            }
        }

        activityTestRuleScenario.scenario.recreate()

        activityTestRuleScenario.scenario.onActivity { activity ->
            activity.setContent {
                val savedStateHandle = viewModel<SavingTestViewModel>(activity).savedStateHandle
                // TODO(b/331695354): Fix local property saveable delegate key conflict
                assertThat(firstFunction(savedStateHandle)).isEqualTo("Two")
                assertThat(secondFunction(savedStateHandle)).isEqualTo("Two")
            }
        }
    }
}

class SavingTestViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()

class FirstClass(savedStateHandle: SavedStateHandle) {
    @OptIn(SavedStateHandleSaveableApi::class)
    val savedProperty by savedStateHandle.saveable { mutableStateOf("One") }
}

class SecondClass(savedStateHandle: SavedStateHandle) {
    @OptIn(SavedStateHandleSaveableApi::class)
    val savedProperty by savedStateHandle.saveable { mutableStateOf("Two") }
}
