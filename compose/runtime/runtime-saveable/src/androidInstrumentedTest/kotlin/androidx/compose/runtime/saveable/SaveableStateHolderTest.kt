/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.runtime.saveable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeLayoutState
import androidx.compose.ui.layout.SubcomposeSlotReusePolicy
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("RememberReturnType")
@MediumTest
@RunWith(AndroidJUnit4::class)
class SaveableStateHolderTest {

    @get:Rule val rule = createAndroidComposeRule<Activity>()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun childStateProviderWorksInAnotherThread() {
        val owners = mutableSetOf<SavedStateRegistryOwner>()

        val state = SubcomposeLayoutState(SubcomposeSlotReusePolicy(1))
        val content =
            @Composable {
                Box {
                    val holder = rememberSaveableStateHolder()
                    holder.SaveableStateProvider(key = Screens.Screen1) {
                        val localOwner = LocalSavedStateRegistryOwner.current
                        LaunchedEffect(localOwner) { owners += localOwner }
                    }
                }
            }

        rule.setContent {
            SubcomposeLayout(state) {
                subcompose("for-reuse", content)
                layout(10, 10) {}
            }
        }

        val precomposition = rule.runOnIdle { state.createPausedPrecomposition(Unit, content) }

        // Create a precomposition (without executing it) on the UI thread
        while (!precomposition.isComplete) {
            // Run composables outside of the main thread.
            precomposition.resume { false }
        }

        // Apply the precomposition on the main thread to execute effects like LaunchedEffect
        rule.runOnIdle { precomposition.apply() }

        // If no IllegalStateException (like "addObserver must be called on the main thread") is
        // thrown, it means SaveableStateRegistryWrapper works correctly even when part of the state
        // was built off the main thread.
    }

    @Test
    fun stateIsRestoredWhenGoBackToScreen1() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen1 = -1
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    numberOnScreen1 = remember { increment++ }
                    restorableNumberOnScreen1 = rememberSaveable { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(0)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            numberOnScreen1 = -1
            restorableNumberOnScreen1 = -1
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(2)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }

    @Test
    fun simpleRestoreOnlyOneScreen() {
        var increment = 0
        var number = -1
        var restorableNumber = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(Screens.Screen1) {
                number = remember { increment++ }
                restorableNumber = rememberSaveable { increment++ }
            }
        }

        rule.runOnIdle {
            assertThat(number).isEqualTo(0)
            assertThat(restorableNumber).isEqualTo(1)
            number = -1
            restorableNumber = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(number).isEqualTo(2)
            assertThat(restorableNumber).isEqualTo(1)
        }
    }

    @Test
    fun switchToScreen2AndRestore() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen2 = -1
        var restorableNumberOnScreen2 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen2) {
                    numberOnScreen2 = remember { increment++ }
                    restorableNumberOnScreen2 = rememberSaveable { increment++ }
                }
            }
        }

        rule.runOnIdle { screen = Screens.Screen2 }

        // wait for the screen switch to apply
        rule.runOnIdle {
            assertThat(numberOnScreen2).isEqualTo(0)
            assertThat(restorableNumberOnScreen2).isEqualTo(1)
            numberOnScreen2 = -1
            restorableNumberOnScreen2 = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(numberOnScreen2).isEqualTo(2)
            assertThat(restorableNumberOnScreen2).isEqualTo(1)
        }
    }

    @Test
    fun stateOfScreen1IsSavedAndRestoredWhileWeAreOnScreen2() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var numberOnScreen1 = -1
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    numberOnScreen1 = remember { increment++ }
                    restorableNumberOnScreen1 = rememberSaveable { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(0)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
            screen = Screens.Screen2
        }

        // wait for the screen switch to apply
        rule.runOnIdle {
            numberOnScreen1 = -1
            restorableNumberOnScreen1 = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        // switch back to screen1
        rule.runOnIdle { screen = Screens.Screen1 }

        rule.runOnIdle {
            assertThat(numberOnScreen1).isEqualTo(2)
            assertThat(restorableNumberOnScreen1).isEqualTo(1)
        }
    }

    @Test
    fun weCanSkipSavingForCurrentScreen() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableStateHolder: SaveableStateHolder? = null
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            restorableStateHolder = holder
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    restorableNumberOnScreen1 = rememberSaveable { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            restorableNumberOnScreen1 = -1
            restorableStateHolder!!.removeState(Screens.Screen1)
            screen = Screens.Screen2
        }

        rule.runOnIdle {
            // switch back to screen1
            screen = Screens.Screen1
        }

        rule.runOnIdle { assertThat(restorableNumberOnScreen1).isEqualTo(1) }
    }

    @Test
    fun weCanRemoveAlreadySavedState() {
        var increment = 0
        var screen by mutableStateOf(Screens.Screen1)
        var restorableStateHolder: SaveableStateHolder? = null
        var restorableNumberOnScreen1 = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            restorableStateHolder = holder
            holder.SaveableStateProvider(screen) {
                if (screen == Screens.Screen1) {
                    restorableNumberOnScreen1 = rememberSaveable { increment++ }
                } else {
                    // screen 2
                    remember { 100 }
                }
            }
        }

        rule.runOnIdle {
            assertThat(restorableNumberOnScreen1).isEqualTo(0)
            restorableNumberOnScreen1 = -1
            screen = Screens.Screen2
        }

        rule.runOnIdle {
            // switch back to screen1
            restorableStateHolder!!.removeState(Screens.Screen1)
            screen = Screens.Screen1
        }

        rule.runOnIdle { assertThat(restorableNumberOnScreen1).isEqualTo(1) }
    }

    @Test
    fun restoringStateOfThePreviousPageAfterCreatingBundle() {
        var showFirstPage by mutableStateOf(true)
        var firstPageState: MutableState<Int>? = null

        rule.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(showFirstPage) {
                if (showFirstPage) {
                    firstPageState = rememberSaveable { mutableStateOf(0) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(firstPageState!!.value).isEqualTo(0)
            // change the value, so we can assert this change will be restored
            firstPageState!!.value = 1
            firstPageState = null
            showFirstPage = false
        }

        rule.runOnIdle {
            rule.activity.doFakeSave()
            showFirstPage = true
        }

        rule.runOnIdle { assertThat(firstPageState!!.value).isEqualTo(1) }
    }

    @Test
    fun saveNothingWhenNoRememberSaveableIsUsedInternally() {
        var showFirstPage by mutableStateOf(true)
        val registry = SaveableStateRegistry(null) { true }

        rule.setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                val holder = rememberSaveableStateHolder()
                holder.SaveableStateProvider(showFirstPage) {}
            }
        }

        rule.runOnIdle { showFirstPage = false }

        rule.runOnIdle {
            val savedData = registry.performSave()
            assertThat(savedData).isEqualTo(emptyMap<String, List<Any?>>())
        }
    }

    @Test
    fun childSavedStateRegistryRestores() {
        var increment = 0
        var number = -1
        var restorableNumber = -1
        restorationTester.setContent {
            val holder = rememberSaveableStateHolder()
            holder.SaveableStateProvider(Screens.Screen1) {
                number = remember { increment++ }

                val owner = LocalSavedStateRegistryOwner.current
                restorableNumber = remember {
                    owner.savedStateRegistry.consumeRestoredStateForKey("key1")?.read {
                        getIntOrNull("key2") ?: -50
                    } ?: increment++
                }
                SideEffect {
                    val valueSnapshot = restorableNumber
                    owner.savedStateRegistry.registerSavedStateProvider("key1") {
                        savedState { putInt("key2", valueSnapshot) }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(number).isEqualTo(0)
            assertThat(restorableNumber).isEqualTo(1)
            number = -1
            restorableNumber = -1
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(number).isEqualTo(2)
            assertThat(restorableNumber).isEqualTo(1)
        }
    }

    @Test
    fun childLocalCompositionsAreSet() {
        var key by mutableStateOf(1)
        val rootRegistry = SaveableStateRegistry(restoredValues = null) { true }

        val localRegistries = mutableListOf<SaveableStateRegistry>()
        val localOwners = mutableListOf<SavedStateRegistryOwner>()
        rule.setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides rootRegistry) {
                val holder = rememberSaveableStateHolder()
                holder.SaveableStateProvider(key) {
                    localRegistries += LocalSaveableStateRegistry.current!!
                    localOwners += LocalSavedStateRegistryOwner.current
                }
            }
        }

        rule.runOnIdle { key = 2 }
        rule.runOnIdle { key = 3 }
        rule.runOnIdle { key = 4 }
        rule.runOnIdle { key = 5 }

        rule.runOnIdle {
            assertThat(localRegistries.size).isEqualTo(5)
            assertThat(localRegistries).doesNotContain(rootRegistry)
            assertThat(localRegistries).containsExactlyElementsIn(localOwners)
            assertThat(localRegistries).containsNoDuplicates()
        }
    }

    class Activity : ComponentActivity() {
        fun doFakeSave() {
            onSaveInstanceState(Bundle())
        }
    }
}

enum class Screens {
    Screen1,
    Screen2,
}
