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

package androidx.lifecycle.viewmodel

import androidx.kruth.assertThat
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.defaultViewModelProviderFactory
import androidx.lifecycle.get
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

internal class ViewModelStoreProviderTest {

    @Test
    fun getViewModelStore_whenCalledFirstTime_createsNewStore() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)

        val store = provider.getOrCreate("key")

        assertThat(store).isNotNull()
    }

    @Test
    fun getViewModelStore_whenCalledTwiceWithSameKey_returnsSameInstance() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "feature_A"

        val store1 = provider.getOrCreate(key)
        val store2 = provider.getOrCreate(key)

        // Ensures state is correctly shared across multiple call sites requesting the same scope.
        assertThat(store2).isSameInstanceAs(store1)
    }

    @Test
    fun getViewModelStore_whenCalledWithDifferentKeys_returnsDifferentInstances() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)

        val store1 = provider.getOrCreate("feature_A")
        val store2 = provider.getOrCreate("feature_B")

        // Different keys dictate strict scope isolation.
        assertThat(store2).isNotSameInstanceAs(store1)
    }

    @Test
    fun removeState_whenCountIsZero_clearsStoreImmediately() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "feature_A"

        val store1 = provider.getOrCreate(key)
        val vm1 = createViewModel(store1)

        // Because no tokens were acquired (ref count is 0), the store is not protected
        // and must be cleared synchronously.
        provider.clearKey(key)

        val store2 = provider.getOrCreate(key)
        val vm2 = createViewModel(store2)

        assertThat(vm2).isNotSameInstanceAs(vm1)
    }

    @Test
    fun removeState_whenCountIsPositive_defersCleanupUntilReleased() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "shared_feature"

        val token = provider.acquireToken(key)

        val store1 = provider.getOrCreate(key)
        val vm1 = createViewModel(store1)

        // The active token (ref count = 1) defers the cleanup to protect the ViewModels
        // from being cleared while still in use by the consumer.
        provider.clearKey(key)

        val store2 = provider.getOrCreate(key)
        val vm2 = createViewModel(store2)
        assertThat(vm2).isSameInstanceAs(vm1)

        // Releasing the final token triggers the deferred cleanup.
        token.close()

        val store3 = provider.getOrCreate(key)
        val vm3 = createViewModel(store3)
        assertThat(vm3).isNotSameInstanceAs(vm1)
    }

    @Test
    fun getViewModelStoreOwner_whenCalled_returnsWrapperDelegatingToParentDefaults() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)

        val wrapper = provider.getOrCreateOwner("key")

        // The child scope must propagate the parent's default factory and extras so that
        // dependency injection and customized ViewModel instantiation continue to work natively.
        assertThat(wrapper).isInstanceOf<HasDefaultViewModelProviderFactory>()

        val wrapperAsDefault = wrapper as HasDefaultViewModelProviderFactory
        assertThat(wrapperAsDefault.defaultViewModelProviderFactory)
            .isEqualTo(owner.defaultViewModelProviderFactory)
    }

    @Test
    fun getViewModelStoreOwner_whenCalledTwice_returnsNewWrapperButSameStore() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "key"

        val wrapper1 = provider.getOrCreateOwner(key)
        val wrapper2 = provider.getOrCreateOwner(key)

        // Owner wrappers are lightweight allocation objects and are intentionally not cached.
        assertThat(wrapper2).isNotSameInstanceAs(wrapper1)
        assertThat(wrapper2.viewModelStore).isSameInstanceAs(wrapper1.viewModelStore)
    }

    @Test
    fun acquireKey_withMultipleTokens_requiresAllToBeReleasedBeforeCleanup() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "key"

        val token1 = provider.acquireToken(key)
        val token2 = provider.acquireToken(key)

        val vm1 = createViewModel(provider.getOrCreate(key))
        provider.clearKey(key)

        // Releasing token1 drops the ref count to 1. The store must survive until count is 0.
        token1.close()
        val vm2 = createViewModel(provider.getOrCreate(key))
        assertThat(vm2).isSameInstanceAs(vm1)

        token2.close()
        val vm3 = createViewModel(provider.getOrCreate(key))
        assertThat(vm3).isNotSameInstanceAs(vm1)
    }

    @Test
    fun configurationChange_whenProviderRecreated_preservesStore() {
        val owner = TestViewModelStoreOwner()

        val provider1 = ViewModelStoreProvider(owner)
        val key = "config_change_test"
        val vm1 = createViewModel(provider1.getOrCreate(key))

        // A configuration change destroys the Provider instance, but the parent Owner
        // keeps its internal StateHolder alive, preserving the child stores.
        val provider2 = ViewModelStoreProvider(owner)
        val vm2 = createViewModel(provider2.getOrCreate(key))

        assertThat(vm2).isSameInstanceAs(vm1)
    }

    @Test
    fun ownerDestroy_whenParentDies_clearsChildStores() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val key = "child"

        val store = provider.getOrCreate(key)
        val vm = createViewModel(store)

        // Permanent destruction of the parent clears the root store, which must cascade
        // through the StateHolder and clear all managed child stores to prevent memory leaks.
        owner.viewModelStore.clear()

        val newOwner = TestViewModelStoreOwner()
        val newProvider = ViewModelStoreProvider(newOwner)
        val newVm = createViewModel(newProvider.getOrCreate(key))

        assertThat(newVm).isNotSameInstanceAs(vm)
    }

    @Test
    fun constructor_whenOwnerIsNull_createsRootProvider() {
        val provider = ViewModelStoreProvider(owner = null)
        val key = "root_test"

        val store = provider.getOrCreate(key)
        assertThat(store).isNotNull()
    }

    @Test
    fun rootProvider_managesLifecycleIndependently() {
        // A null owner implies a Root Provider that manages its own lifecycle manually,
        // requiring explicit cleanup since there is no parent to trigger it via configuration
        // changes.
        val provider = ViewModelStoreProvider(owner = null)
        val key = "root_feature"

        val token = provider.acquireToken(key)

        val store1 = provider.getOrCreate(key)
        val vm1 = createViewModel(store1)

        token.close()
        provider.clearAllKeys()

        val store2 = provider.getOrCreate(key)
        val vm2 = createViewModel(store2)

        assertThat(vm2).isNotSameInstanceAs(vm1)
    }

    @Test
    fun getViewModelStoreOwner_withNullSavedStateOwner_doesNotImplementSavedStateRegistryOwner() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)

        val wrapper = provider.getOrCreateOwner(key = "key", savedStateRegistryOwner = null)

        // Strict interface compliance prevents ViewModels from attempting to read a null registry.
        assertThat(wrapper).isNotInstanceOf<SavedStateRegistryOwner>()
    }

    @Test
    fun getViewModelStoreOwner_withSavedStateOwner_implementsSavedStateRegistryOwner() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val savedStateOwner = TestSavedStateRegistryOwner()

        val wrapper =
            provider.getOrCreateOwner(key = "key", savedStateRegistryOwner = savedStateOwner)

        assertThat(wrapper).isInstanceOf<SavedStateRegistryOwner>()
    }

    @Test
    fun getViewModelStoreOwner_withSavedStateOwner_delegatesLifecycleAndRegistry() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val savedStateOwner = TestSavedStateRegistryOwner()

        val wrapper =
            provider.getOrCreateOwner(key = "key", savedStateRegistryOwner = savedStateOwner)
        val wrapperAsSavedStateOwner = wrapper as SavedStateRegistryOwner

        // The wrapper must perfectly delegate state resolution to the provided parent owner
        // to ensure SavedStateHandles survive configuration changes correctly.
        assertThat(wrapperAsSavedStateOwner.lifecycle).isSameInstanceAs(savedStateOwner.lifecycle)
        assertThat(wrapperAsSavedStateOwner.savedStateRegistry)
            .isSameInstanceAs(savedStateOwner.savedStateRegistry)
    }

    @Test
    fun getViewModelStoreOwner_withSavedStateOwner_upgradesDefaultFactoryToSavedStateViewModelFactory() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val savedStateOwner = TestSavedStateRegistryOwner()

        val wrapper =
            provider.getOrCreateOwner(
                key = "key",
                savedStateRegistryOwner = savedStateOwner,
                defaultFactory = SavedStateViewModelFactory(),
            )
        val wrapperAsDefaults = wrapper as HasDefaultViewModelProviderFactory

        // Providing a SavedStateRegistryOwner implicitly requires upgrading the factory
        // so that ViewModels requesting a SavedStateHandle can be correctly instantiated.
        assertThat(wrapperAsDefaults.defaultViewModelProviderFactory)
            .isInstanceOf<SavedStateViewModelFactory>()
    }

    @Test
    @IgnoreAndroidHostTarget
    fun getViewModelStoreOwner_withSavedStateOwner_injectsSavedStateCreationExtras() {
        val owner = TestViewModelStoreOwner()
        val provider = ViewModelStoreProvider(owner)
        val savedStateOwner = TestSavedStateRegistryOwner()

        val wrapper =
            provider.getOrCreateOwner(key = "key", savedStateRegistryOwner = savedStateOwner)
        val wrapperAsDefaults = wrapper as HasDefaultViewModelProviderFactory
        val extras = wrapperAsDefaults.defaultViewModelCreationExtras

        // The wrapper must inject itself into the extras to satisfy the CreationExtras requirements
        // of SavedStateViewModelFactory.
        assertThat(extras[SAVED_STATE_REGISTRY_OWNER_KEY]).isSameInstanceAs(wrapper)
        assertThat(extras[VIEW_MODEL_STORE_OWNER_KEY]).isSameInstanceAs(wrapper)
    }

    private fun createViewModel(store: ViewModelStore): TestViewModel {
        return ViewModelProvider.create(store, TestViewModel.FACTORY).get<TestViewModel>()
    }

    private class TestViewModel : ViewModel() {
        companion object {
            val FACTORY = viewModelFactory { initializer { TestViewModel() } }
        }
    }

    private class TestViewModelStoreOwner :
        ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        override val viewModelStore: ViewModelStore = ViewModelStore()
    }

    private class TestSavedStateRegistryOwner(
        initialState: Lifecycle.State = Lifecycle.State.INITIALIZED
    ) : SavedStateRegistryOwner {

        @OptIn(ExperimentalCoroutinesApi::class)
        @Suppress("VisibleForTests")
        val lifecycleRegistry =
            TestLifecycleOwner(
                initialState = Lifecycle.State.INITIALIZED,
                coroutineDispatcher = UnconfinedTestDispatcher(),
            )

        val controller = SavedStateRegistryController.create(owner = this).apply { performAttach() }

        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry.lifecycle

        init {
            lifecycleRegistry.currentState = initialState
        }
    }
}
