/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.lifecycle.viewmodel

import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_KEY
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ViewModelStoreOwnerFactoryTest {

    // A custom key to test that base CreationExtras are preserved during the merge.
    private val TEST_EXTRA_KEY = object : CreationExtras.Key<String> {}

    @Test
    fun viewModelStoreOwner_whenCreated_thenWiresPropertiesAndMergesExtras() {
        val store = ViewModelStore()
        val defaultArgs = savedState { putString("test_arg", "value") }
        val baseExtras = MutableCreationExtras().apply { set(TEST_EXTRA_KEY, "test_value") }
        val factory = object : ViewModelProvider.Factory {}

        val owner =
            ViewModelStoreOwner(
                viewModelStore = store,
                defaultArgs = defaultArgs,
                defaultCreationExtras = baseExtras,
                defaultFactory = factory,
            )

        // Verify standard interface wiring.
        assertThat(owner.viewModelStore).isSameInstanceAs(store)
        assertThat(owner).isInstanceOf(HasDefaultViewModelProviderFactory::class.java)

        val ownerWithFactory = owner as HasDefaultViewModelProviderFactory
        assertThat(ownerWithFactory.defaultViewModelProviderFactory).isSameInstanceAs(factory)

        // Verify the creation extras were merged correctly, injecting the owner and args
        // without dropping the originally provided extras.
        val generatedExtras = ownerWithFactory.defaultViewModelCreationExtras
        assertThat(generatedExtras[TEST_EXTRA_KEY]).isEqualTo("test_value")
        assertThat(generatedExtras[DEFAULT_ARGS_KEY]!!.read { contentDeepEquals(defaultArgs) })
            .isTrue()
        assertThat(generatedExtras[VIEW_MODEL_STORE_OWNER_KEY]).isSameInstanceAs(owner)
    }

    @Test
    fun viewModelStoreOwner_whenCreatedWithSavedStateRegistry_thenWiresPropertiesAndEnablesSavedStateHandles() {
        // Set up a concrete SavedStateRegistry environment.
        val lifecycleOwner = FakeSavedStateRegistryOwner()
        lifecycleOwner.controller.performAttach()
        lifecycleOwner.controller.performRestore(null)

        val store = ViewModelStore()
        val defaultArgs = savedState()
        val factory = object : ViewModelProvider.Factory {}

        val owner =
            ViewModelStoreOwner(
                viewModelStore = store,
                savedStateRegistry = lifecycleOwner.savedStateRegistry,
                lifecycle = lifecycleOwner.lifecycle,
                defaultArgs = defaultArgs,
                defaultFactory = factory,
            )
        require(owner is SavedStateRegistryOwner)

        // Verify extended interface wiring
        assertThat(owner).isInstanceOf(SavedStateRegistryOwner::class.java)
        assertThat(owner.viewModelStore).isSameInstanceAs(store)
        assertThat(owner.lifecycle).isSameInstanceAs(lifecycleOwner.lifecycle)

        val savedStateOwner = owner as SavedStateRegistryOwner
        assertThat(savedStateOwner.savedStateRegistry)
            .isSameInstanceAs(lifecycleOwner.savedStateRegistry)

        // Verify creation extras specific to SavedState functionality
        val ownerWithFactory = owner as HasDefaultViewModelProviderFactory
        val generatedExtras = ownerWithFactory.defaultViewModelCreationExtras
        assertThat(generatedExtras[SAVED_STATE_REGISTRY_OWNER_KEY]).isSameInstanceAs(owner)
        assertThat(generatedExtras[VIEW_MODEL_STORE_OWNER_KEY]).isSameInstanceAs(owner)

        // Verify the side effect in the init block:
        // enableSavedStateHandles() should have registered the SAVED_STATE_KEY provider.
        assertThat(lifecycleOwner.savedStateRegistry.getSavedStateProvider(SAVED_STATE_KEY))
            .isNotNull()
    }

    @Test
    @IgnoreAndroidHostTarget
    fun viewModelStoreOwner_whenBaseExtrasHaveDefaultArgs_mergesWithProvidedDefaultArgs() {
        val store = ViewModelStore()

        // Provide an initial set of arguments to simulate existing state from an upstream caller.
        val existingArgs = savedState { putString("existing_key", "existing_value") }
        val baseExtras = MutableCreationExtras().apply { this[DEFAULT_ARGS_KEY] = existingArgs }

        val factoryArgs = savedState { putString("factory_key", "factory_value") }
        val factory = object : ViewModelProvider.Factory {}

        val owner =
            ViewModelStoreOwner(
                viewModelStore = store,
                defaultArgs = factoryArgs,
                defaultCreationExtras = baseExtras,
                defaultFactory = factory,
            )

        val ownerWithFactory = owner as HasDefaultViewModelProviderFactory
        val generatedExtras = ownerWithFactory.defaultViewModelCreationExtras
        val mergedArgs = generatedExtras[DEFAULT_ARGS_KEY]

        assertThat(mergedArgs).isNotNull()
        assertThat(mergedArgs?.read { getString("existing_key") }).isEqualTo("existing_value")
        assertThat(mergedArgs?.read { getString("factory_key") }).isEqualTo("factory_value")
    }

    private class FakeSavedStateRegistryOwner : SavedStateRegistryOwner {
        val controller = SavedStateRegistryController.create(this)

        override val savedStateRegistry
            get() = controller.savedStateRegistry

        override val lifecycle = LifecycleRegistry.createUnsafe(owner = this)
    }
}
