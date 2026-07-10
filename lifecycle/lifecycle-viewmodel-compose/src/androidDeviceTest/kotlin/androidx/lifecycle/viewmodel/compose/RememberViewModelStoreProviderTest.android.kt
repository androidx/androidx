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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.savedstate.read
import androidx.savedstate.savedState
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RememberViewModelStoreProviderTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private class TestViewModel : ViewModel() {
        var isCleared = false
            private set

        override fun onCleared() {
            super.onCleared()
            isCleared = true
        }
    }

    @Test
    fun rememberViewModelStoreProvider_whenParentProvided_createsLinkedProvider() {
        var capturedProvider: ViewModelStoreProvider? = null

        rule.setContent {
            // Linking a provider to a parent (like the Activity) allows the
            // provider to participate in a larger, managed lifecycle.
            val activityOwner = LocalViewModelStoreOwner.current!!
            capturedProvider = rememberViewModelStoreProvider(parent = activityOwner)
        }
        rule.waitForIdle()

        assertThat(capturedProvider).isNotNull()
    }

    @Test
    fun rememberViewModelStoreProvider_whenComposableRecomposed_returnsSameInstance() {
        var provider1: ViewModelStoreProvider? = null
        var provider2: ViewModelStoreProvider? = null
        var triggerRecompose by mutableStateOf(0)

        rule.setContent {
            // We use SideEffect to capture the instance across recompositions.
            // The provider must be stable; if it recreated on every recompose,
            // we would lose all stored ViewModels and their state.
            SideEffect { triggerRecompose.toString() }

            val p = rememberViewModelStoreProvider()
            if (triggerRecompose == 0) provider1 = p else provider2 = p
        }
        rule.waitForIdle()

        triggerRecompose++
        rule.waitForIdle()

        assertThat(provider2).isSameInstanceAs(provider1)
    }

    @Test
    fun rememberViewModelStoreProvider_whenParentNull_createsRootProvider() {
        var capturedProvider: ViewModelStoreProvider? = null

        rule.setContent {
            // Passing null creates a standalone root. This is useful for features
            // that need to manage their own lifecycle entirely independent of
            // the hosting Activity or Fragment.
            capturedProvider = rememberViewModelStoreProvider(parent = null)
        }
        rule.waitForIdle()

        assertThat(capturedProvider).isNotNull()
        val store = capturedProvider?.getOrCreate("test_key")
        assertThat(store).isNotNull()
    }

    @Test
    fun rememberViewModelStoreProvider_whenLeavingComposition_disposesProvider() {
        var provider: ViewModelStoreProvider? = null
        var showComposable by mutableStateOf(true)

        rule.setContent {
            if (showComposable) {
                provider = rememberViewModelStoreProvider()
            }
        }
        rule.waitForIdle()
        val originalProvider = provider!!

        // When a composable is removed from the tree, its 'remembered' state
        // is discarded. This ensures that memory is reclaimed once the
        // feature is no longer active in the UI.
        showComposable = false
        rule.waitForIdle()

        showComposable = true
        rule.waitForIdle()

        assertThat(provider).isNotSameInstanceAs(originalProvider)
    }

    @Test
    fun rememberViewModelStoreProvider_withDefaultArgs_propagatesToProvider() {
        var provider: ViewModelStoreProvider? = null
        val expectedArgs = savedState { putString("key", "value") }

        rule.setContent { provider = rememberViewModelStoreProvider(defaultArgs = expectedArgs) }
        rule.waitForIdle()

        assertThat(provider).isNotNull()
        val owner = provider!!.getOrCreateOwner("test_key") as HasDefaultViewModelProviderFactory
        val actualArgs = owner.defaultViewModelCreationExtras[DEFAULT_ARGS_KEY]
        assertThat(actualArgs!!.read { contentDeepEquals(expectedArgs) }).isTrue()
    }

    @Test
    fun rememberViewModelStoreProvider_onDispose_doesNotAffectSiblingStores() {
        lateinit var providerA: ViewModelStoreProvider
        lateinit var providerB: ViewModelStoreProvider
        var showA by mutableStateOf(true)

        rule.setContent {
            if (showA) {
                providerA = rememberViewModelStoreProvider()
            }
            // Sibling provider that stays in the composition
            providerB = rememberViewModelStoreProvider()
        }
        rule.waitForIdle()

        val storeABeforeDisposal = providerA.getOrCreate("test_key")
        val storeBBeforeDisposal = providerB.getOrCreate("test_key")

        // We remove providerA from the tree. Its 'onDispose' will run.
        // Because it was keyed by its unique composition hash, it should
        // only clean up its own separated store, leaving the parent and
        // sibling stores untouched.
        showA = false
        rule.waitForIdle()

        val storeAAfterDisposal = providerA.getOrCreate("test_key")
        val storeBAfterDisposal = providerB.getOrCreate("test_key")

        // Verify that ProviderA's store has been destroyed.
        assertThat(storeAAfterDisposal).isNotSameInstanceAs(storeABeforeDisposal)

        // Verify that providerB's store survived providerA's destruction.
        assertThat(storeBAfterDisposal).isSameInstanceAs(storeBBeforeDisposal)
    }

    @Test
    fun rememberViewModelStoreProvider_withSameKey_whenOneDisposed_preservesState() {
        lateinit var providerA: ViewModelStoreProvider
        lateinit var providerB: ViewModelStoreProvider
        var showA by mutableStateOf(true)
        val sharedProviderKey = "shared_provider_key"
        val sharedChildKey = "shared_child_key"

        rule.setContent {
            if (showA) {
                providerA = rememberViewModelStoreProvider(key = sharedProviderKey)
            }
            // Sibling provider that shares the same parent and provider key
            providerB = rememberViewModelStoreProvider(key = sharedProviderKey)
        }
        rule.waitForIdle()

        val storeABeforeDisposal = providerA.getOrCreate(sharedChildKey)
        val storeBBeforeDisposal = providerB.getOrCreate(sharedChildKey)

        // Since they share the same parent and the same provider key, they must share the same
        // store.
        assertThat(storeBBeforeDisposal).isSameInstanceAs(storeABeforeDisposal)

        // We remove providerA from the tree. Its 'onDispose' will run and call 'clearAllKeys()'.
        // However, providerB is still in the composition and it holds a provider-level token.
        // The shared state must survive as long as at least one provider is active.
        showA = false
        rule.waitForIdle()

        val storeBAfterDisposal = providerB.getOrCreate(sharedChildKey)

        // The shared store must survive because Provider B is still active.
        assertThat(storeBAfterDisposal).isSameInstanceAs(storeABeforeDisposal)
    }

    @Test
    fun rememberViewModelStoreProvider_withUnstableParent_isRemembered() {
        var initialViewModel: TestViewModel? = null
        var count by mutableIntStateOf(0)

        // A stable parent instance returning unstable default parameters.
        // The getters return new instances on every access. This simulates the
        // unstable inputs that previously caused `remember()` to invalidate
        // and incorrectly clear the `ViewModelStore` upon recomposition.
        val unstableParent =
            object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
                override val viewModelStore = ViewModelStore()

                override val defaultViewModelProviderFactory
                    get() = SavedStateViewModelFactory()

                override val defaultViewModelCreationExtras
                    get() = CreationExtras()
            }

        rule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides unstableParent) {
                BasicText("Count: $count")

                val storeProvider = rememberViewModelStoreProvider()
                val storeOwner = rememberViewModelStoreOwner(storeProvider)

                CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
                    val viewModel = viewModel<TestViewModel> { TestViewModel() }

                    if (initialViewModel == null) {
                        initialViewModel = viewModel
                    }
                }
            }
        }

        rule.waitForIdle()

        count++
        rule.waitForIdle()

        assertThat(initialViewModel?.isCleared).isFalse()
    }
}
