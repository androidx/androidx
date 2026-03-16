/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.lifecycle.viewmodel.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RememberViewModelStoreOwnerTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rememberViewModelStoreOwner_whenComposableRemoved_clearsViewModel() {
        var currentVm: TestViewModel? = null
        var showStoreOwner by mutableStateOf(true)

        rule.setContent {
            if (showStoreOwner) {
                // We create a scoped owner for a specific UI sub-tree (like a Dialog).
                // When this conditional block is removed, the owner and its ViewModels
                // must be cleared to prevent memory leaks.
                val owner = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    currentVm = viewModel { TestViewModel() }
                }
            }
        }
        rule.waitForIdle()

        showStoreOwner = false
        rule.waitForIdle()

        assertThat(currentVm!!.isCleared).isTrue()
    }

    @Test
    fun rememberViewModelStoreOwner_whenAppBackgroundedAndRemoved_clearsViewModel() {
        var currentVm: TestViewModel? = null
        var showStoreOwner by mutableStateOf(true)

        rule.setContent {
            if (showStoreOwner) {
                val owner = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    currentVm = viewModel { TestViewModel() }
                }
            }
        }
        rule.waitForIdle()

        // Moving to CREATED simulates the app being in the background.
        // We ensure that UI state changes (removing the composable) still trigger
        // cleanup even if the Activity isn't currently visible/active.
        rule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        showStoreOwner = false
        rule.waitForIdle()

        assertThat(currentVm!!.isCleared).isTrue()
    }

    @Test
    fun rememberViewModelStoreOwner_whenSiblingRemoved_clearsIndependently() {
        var vmKeep: TestViewModel? = null
        var vmRemove: TestViewModel? = null
        var showSecond by mutableStateOf(true)

        rule.setContent {
            val owner1 = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner1) {
                vmKeep = viewModel(key = "vmKeep") { TestViewModel() }
            }

            if (showSecond) {
                val owner2 = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner2) {
                    vmRemove = viewModel(key = "vmRemove") { TestViewModel() }
                }
            }
        }
        rule.waitForIdle()

        showSecond = false
        rule.waitForIdle()

        // Verifies that owners are isolated. Clearing one does not affect the other,
        // even if they are defined within the same parent composition.
        assertThat(vmRemove!!.isCleared).isTrue()
        assertThat(vmKeep!!.isCleared).isFalse()
    }

    @Test
    fun rememberViewModelStoreOwner_whenConfigurationChanged_retainsViewModel() {
        var currentVm: TestViewModel? = null

        rule.setContent {
            val owner = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                currentVm = viewModel { TestViewModel() }
            }
        }
        rule.waitForIdle()
        val vm1 = currentVm

        // Rotation should destroy the Activity but retain the ViewModelStore.
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()

        val vm2 = currentVm
        assertThat(vm2).isSameInstanceAs(vm1)
        assertThat(vm2?.isCleared).isFalse()
    }

    @Test
    fun rememberViewModelStoreOwner_whenActivityDestroyed_clearsViewModel() {
        var currentVm: TestViewModel? = null

        rule.setContent {
            val owner = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                currentVm = viewModel { TestViewModel() }
            }
        }
        rule.waitForIdle()

        // Permanent destruction of the host (like a Back press) must clear all scoped ViewModels.
        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(currentVm!!.isCleared).isTrue()
    }

    @Test
    fun rememberViewModelStoreOwner_hoistedProvider_isolatesScopesButDefersCleanup() {
        var vmParent: TestViewModel? = null
        var vmChild: TestViewModel? = null

        var showParent by mutableStateOf(true)
        var showChild by mutableStateOf(true)

        rule.setContent {
            if (showParent) {
                // Using a shared provider allows multiple owners to exist within the same lifecycle
                // scope.
                val provider = rememberViewModelStoreProvider()

                val parentOwner =
                    rememberViewModelStoreOwner(provider, savedStateRegistryOwner = null)
                CompositionLocalProvider(LocalViewModelStoreOwner provides parentOwner) {
                    vmParent = viewModel(key = "parent") { TestViewModel() }
                }

                if (showChild) {
                    val childOwner =
                        rememberViewModelStoreOwner(provider, savedStateRegistryOwner = null)
                    CompositionLocalProvider(LocalViewModelStoreOwner provides childOwner) {
                        vmChild = viewModel(key = "child") { TestViewModel() }
                    }
                }
            }
        }
        rule.waitForIdle()

        assertThat(vmParent).isNotSameInstanceAs(vmChild)
        checkNotNull(vmParent)
        checkNotNull(vmChild)

        // With a hoisted provider, removing the child composable doesn't clear the child VM
        // immediately because the provider (the "bag" of stores) is still alive in the parent.
        showChild = false
        rule.waitForIdle()

        assertThat(vmParent.isCleared).isFalse()
        assertThat(vmChild.isCleared).isFalse()

        // Once the parent (and thus the provider) is removed, everything is cleared in a cascade.
        showParent = false
        rule.waitForIdle()

        assertThat(vmParent.isCleared).isTrue()
        assertThat(vmChild.isCleared).isTrue()
    }

    @Test
    fun rememberViewModelStoreOwner_withNullSavedStateRegistryOwner_doesNotSupportSavedState() {
        var isSavedStateOwner = true

        rule.setContent {
            // Explicitly opt out of SavedState support
            val owner = rememberViewModelStoreOwner(savedStateRegistryOwner = null)
            isSavedStateOwner = owner is SavedStateRegistryOwner
        }
        rule.waitForIdle()

        // The wrapper must fall back to a simple ViewModelStoreOwner
        assertThat(isSavedStateOwner).isFalse()
    }

    class TestViewModel : ViewModel() {
        var isCleared = false
            private set

        public override fun onCleared() {
            isCleared = true
        }
    }
}
